package com.lmarket.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.common.exception.BizCodeEnume;
import com.common.exception.NoStockException;
import com.common.utils.R;
import com.common.vo.MemberResponseVo;
import com.lmarket.order.constant.OrderConstant;
import com.lmarket.order.dao.OrderItemDao;
import com.lmarket.order.entity.OrderItemEntity;
import com.lmarket.order.enume.OrderStatusEnum;
import com.lmarket.order.feign.CartFeignService;
import com.lmarket.order.feign.MemberFeignService;
import com.lmarket.order.feign.ProductFeignService;
import com.lmarket.order.feign.WmsFeignService;
import com.lmarket.order.interceptor.LoginUserInterceptor;
import com.lmarket.order.service.OrderItemService;
import com.lmarket.order.to.OrderCreateTo;
import com.lmarket.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.PageUtils;
import com.common.utils.Query;

import com.lmarket.order.dao.OrderDao;
import com.lmarket.order.entity.OrderEntity;
import com.lmarket.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    WmsFeignService wmsFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ThreadPoolExecutor executor;

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResponseVo memberResponseVo = LoginUserInterceptor.localUser.get();

        //异步之前先获取请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1、远程查询所有的收货地址列表
            RequestContextHolder.setRequestAttributes(requestAttributes); //共享请求头

            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getMemberId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> getCartItemsFuture = CompletableFuture.runAsync(() -> {
            //2、远程查询购物车所有选中的购物项
            RequestContextHolder.setRequestAttributes(requestAttributes); //共享请求头

            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);

        }, executor).thenRunAsync(()->{
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if(data != null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);


        //3、查询用户积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);

        //4、其他数据自动计算

        //TODO 5、防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(getAddressFuture, getCartItemsFuture).get();

        return confirmVo;
    }

    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        MemberResponseVo memberResponseVo = LoginUserInterceptor.localUser.get();
        responseVo.setCode(0);


        //1、验证令牌[令牌的对比和删除两个操作需要在一个原子操作里面]
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end"; //0-令牌校验失败，1-删除成功
        String orderToken = vo.getOrderToken();

        //原子验证、删除令牌
        Long res = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        if(res == 0L){
            //令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        }else{
            //去创建订单，验令牌、价格，锁库存
            //1、创建订单、订单项等信息
            OrderCreateTo order = createOrder();
            //2、验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if(Math.abs(payAmount.subtract(payPrice).doubleValue())<0.01){
                //金额对比
                //3、保存订单
                saveOrder(order);
                //4、库存锁定 只要有异常，则回滚订单数据:[订单号、所有订单项信息（skuId, skuName, num）]
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItem().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);
                //远程锁库存
                R r = wmsFeignService.orderLockStock(wareSkuLockVo);
                if(r.getCode() == 0){
                    //锁库存成功
                    responseVo.setOrder(order.getOrder());

                    //TODO 远程扣减积分
                    return responseVo;
                }else{
                    //锁库存失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);

//                    responseVo.setCode(3);
//                    return responseVo;
                }

            }else{
                responseVo.setCode(2);
                return responseVo;
            }

        }

//        String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
//        if(orderToken != null && orderToken.equals(redisToken)){
//            //令牌验证通过
//            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());
//
//        }else{
//            return null;
//        }
    }

    /**
     * 保存订单数据
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);

        List<OrderItemEntity> orderItem = order.getOrderItem();
        orderItemService.saveBatch(orderItem);
    }

    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1、生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity orderEntity = buildOrder(orderSn);

        //2、获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);

        //3、计算价格、积分相关信息
        checkPrice(orderEntity, itemEntities);
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItem(itemEntities);

        return  orderCreateTo;
    }

    private void checkPrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.00");
        BigDecimal couponAmout = new BigDecimal("0.00");
        BigDecimal integrationAmount = new BigDecimal("0.00");
        BigDecimal promotionAmount = new BigDecimal("0.00");

        BigDecimal gift = new BigDecimal("0.00");
        BigDecimal growth = new BigDecimal("0.00");

        //订单的总额：通过叠加每一个订单项的总额信息
        for (OrderItemEntity itemEntity : itemEntities) {
            couponAmout = couponAmout.add(itemEntity.getCouponAmount());
            integrationAmount = integrationAmount.add(itemEntity.getIntegrationAmount());
            promotionAmount = promotionAmount.add(itemEntity.getPromotionAmount());
            total  = total.add(itemEntity.getRealAmount());
            gift = gift.add(new BigDecimal(itemEntity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(itemEntity.getGiftGrowth().toString()));
        }
        //1、订单价格相关
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        //设置应付总额
        orderEntity.setCouponAmount(couponAmout);
        orderEntity.setIntegrationAmount(integrationAmount);
        orderEntity.setPromotionAmount(promotionAmount);
        orderEntity.setTotalAmount(total);

        //设置积分等信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        orderEntity.setDeleteStatus(0); //0-未删除

    }

    /**
     * 构建一个订单
     * @param orderSn
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVo responseVo = LoginUserInterceptor.localUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(responseVo.getMemberId());

        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        //获取收货地址信息
        R fare = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //设置运费信息
        orderEntity.setFreightAmount(fareResp.getFare());
        //设置收货信息
        orderEntity.setReceiverProvince(fareResp.getAddress().getProvince()); //省份/直辖市
        orderEntity.setReceiverCity(fareResp.getAddress().getCity()); //城市
        orderEntity.setReceiverRegion(fareResp.getAddress().getRegion()); //区
        orderEntity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress()); //详细地址
        orderEntity.setReceiverPostCode(fareResp.getAddress().getPostCode()); //邮编
        orderEntity.setReceiverName(fareResp.getAddress().getName()); //收货人名字
        orderEntity.setBillReceiverPhone(fareResp.getAddress().getPhone()); //收货电话

        //设置订单的相关状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode()); //订单状态
        orderEntity.setAutoConfirmDay(7); //自动确认收货时间


        return orderEntity;
    }

    /**
     * 构建所有的订单项数据
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确认每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if(currentUserCartItems != null && currentUserCartItems.size() > 0){
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }

        return null;
    }

    /**
     * 构建一个订单项
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        //1、订单信息：订单号 v
        //2、商品的spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setCategoryId(data.getCatelogId());

        //3、商品的sku信息 v
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImages());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttr);
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        //TODO 4、优惠信息
        //5、积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        //6、订单项的价格信息
        //优惠减免的价格
        orderItemEntity.setPromotionAmount(new BigDecimal("0.00"));
        orderItemEntity.setCouponAmount(new BigDecimal("0.00"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0.00"));

        //当前订单项的实际金额 总额减去优惠
        BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = orign.subtract(orderItemEntity.getPromotionAmount()).subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

}