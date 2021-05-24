package com.lmarket.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
//import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.common.id.SnowFlakeGenerateIdWorker;
import com.common.to.SeckillOrderTo;
import com.common.to.SkuReductionTo;
import com.common.utils.R;
import com.common.vo.MemberResponseVo;
import com.lmarket.seckill.feign.CouponFeignService;
import com.lmarket.seckill.feign.ProductFeignService;
import com.lmarket.seckill.interceptor.LoginUserInterceptor;
import com.lmarket.seckill.service.SeckillService;
import com.lmarket.seckill.to.SeckillSkuRedisTo;
import com.lmarket.seckill.vo.SeckillSessionsWithSkus;
import com.lmarket.seckill.vo.SeckillSkuVo;
import com.lmarket.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RedissonClient redissonClient;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";

    private final String SECKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:"; //后面跟商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //1、扫描最近3天需要参与秒杀的活动
        R lates3DaySession = couponFeignService.getLates3DaySession();
        if (lates3DaySession.getCode() == 0) {
            //上架秒杀商品
            List<SeckillSessionsWithSkus> data = lates3DaySession.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            //缓存到redis
            //1、缓存活动信息
            saveSessionInfos(data);
            //2、缓存活动关联的商品信息
            saveSessionSkuInfos(data);
        }
    }

    public List<SeckillSkuRedisTo> BlockExceptionHandler(BlockException e){
        log.error("getCurrentSeckillSkus被限流了。。。");
        return null;
    }

    @SentinelResource(value = "getCurrentSeckillSkus", blockHandler = "BlockExceptionHandler")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {

        try(Entry entry = SphU.entry("seckillSkus")){
            //1、确定当前时间属于那个秒杀场次
            long time = new Date().getTime();
            Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
            for (String key : keys) {
                //"seckill:sessions:1619568000000_1619575200000"
                String[] s = key.replace(SESSION_CACHE_PREFIX, "").split("_");
                Long start = Long.parseLong(s[0]);
                Long end = Long.parseLong(s[1]);
                if (time >= start && time <= end) {
                    //2、获取该秒杀场次需要的所有商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
                    List<String> list = hashOps.multiGet(range);
                    if (list != null) {
                        List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                            SeckillSkuRedisTo redis = JSON.parseObject(item.toString(), SeckillSkuRedisTo.class);
//                        redis.setRandomCode(null); 当前秒杀开始了，则会需要随机码
                            return redis;
                        }).collect(Collectors.toList());
                        return collect;
                    }
                    break;
                }
            }
        }catch (BlockException e){
            log.error("资源被限流， {}", e.getMessage());
        }
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {

        //1、找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                //1_43
                if (Pattern.matches(regx, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

                    //判断随机码是否返回：当前时间在秒杀时间段才返回随机码
                    long currenTime = new Date().getTime();
                    if (currenTime >= skuRedisTo.getStartTime() && currenTime <= skuRedisTo.getEndTime()) {

                    } else {
                        skuRedisTo.setRandomCode(null); //不在秒杀时间段内，则把随机码置空
                    }

                    return skuRedisTo;
                }

            }
        }

        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        long s1 = System.currentTimeMillis();
        MemberResponseVo responseVo = LoginUserInterceptor.localUser.get();

        //获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)) {
            return null;
        } else {
            SeckillSkuRedisTo redis = JSON.parseObject(json, SeckillSkuRedisTo.class);
            //1、校验时间的合法性
            long time = new Date().getTime();

            long ttl = redis.getEndTime() - time;

            if (time >= redis.getStartTime() && time <= redis.getEndTime()) {
                //2、校验随机码和商品id
                String randomCode = redis.getRandomCode();
                String skuId = redis.getPromotionSessionId() + "_" + redis.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //3、验证秒杀期间购物的数量是否合理
                    if (num <= redis.getSeckillCount()) {
                        //4、验证用户是否已经购买过该秒杀商品 （保证幂等性），如果秒杀成功，就去redis里面占位：userId_sessionId_skuId
                        String redisKey = responseVo.getMemberId() + "_" + skuId;
                        //占位，并设置过期时间
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {
                            //占位成功
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode); //获取信号量

                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功
                                //快速下单
                                SnowFlakeGenerateIdWorker timeId = new SnowFlakeGenerateIdWorker(); //用雪花算法生成订单号
//                                String timeId = IdWorker.getTimeId(); //创建一个订单号
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(String.valueOf(timeId));
                                orderTo.setMemberId(responseVo.getMemberId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(redis.getPromotionSessionId());
                                orderTo.setSkuId(redis.getSkuId());
                                orderTo.setSeckillPrice(redis.getSeckillPrice());
                                orderTo.setSkuDefaultImg(redis.getSkuInfo().getSkuDefaultImg());


                                //发送MQ消息
                                log.info("商品秒杀成功。。。。。");
                                rabbitTemplate.convertAndSend("order-event-exchange",
                                        "order.seckill.order",
                                        orderTo);
                                long s2 = System.currentTimeMillis();
                                log.info("秒杀下单耗时。。。。。"+(s2-s1));
                                return String.valueOf(timeId);
                            } else {
                                return null;
                            }
                        } else {
                            //已购买秒杀商品
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        if(sessions != null){
            sessions.stream().forEach(session -> {
                long startTime = session.getStartTime().getTime();
                long endTime = session.getEndTime().getTime();
                String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

                //缓存到活动信息
                Boolean hasKey = redisTemplate.hasKey(key);
                //保证幂等性
                if (!hasKey) {
                    List<String> collect = session.getRelationEntityList().stream().map(item ->
                            item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString())
                            .collect(Collectors.toList());
                    redisTemplate.opsForList().leftPushAll(key, collect);
                }


            });
        }

    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        if(sessions != null){
            sessions.stream().forEach(session -> {
                //准备hash操作
                BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SECKILL_CACHE_PREFIX);
                session.getRelationEntityList().stream().forEach(seckillSkuVo -> {
                    //生成随机码
                    String token = UUID.randomUUID().toString().replace("-", "");

                    if (!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString())) { //保证幂等性
                        //缓存商品信息
                        SeckillSkuRedisTo skuRedisTo = new SeckillSkuRedisTo();
                        //1、sku的基本信息
                        R skuInfo = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                        if (skuInfo.getCode() == 0) {
                            SkuInfoVo info = skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            skuRedisTo.setSkuInfo(info);
                        }

                        //2、sku的秒杀信息
                        BeanUtils.copyProperties(seckillSkuVo, skuRedisTo);
                        //3、设置当前商品的秒杀时间信息
                        skuRedisTo.setStartTime(session.getStartTime().getTime());
                        skuRedisTo.setEndTime(session.getEndTime().getTime());

                        //4、设置随机码
                        skuRedisTo.setRandomCode(token);

                        Boolean hasKey = redisTemplate.hasKey(SKU_STOCK_SEMAPHORE);

                        //5、使用库存作为分布式的信号量(作用是限流)
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        //商品可以秒杀的数量作为信号量
                        semaphore.trySetPermits(seckillSkuVo.getSeckillCount());

                        String s = JSON.toJSONString(skuRedisTo);
                        ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), s);
                    }
                });
            });
        }

    }
}
