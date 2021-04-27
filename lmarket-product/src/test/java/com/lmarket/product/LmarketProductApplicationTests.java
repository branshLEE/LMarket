package com.lmarket.product;
//
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lmarket.product.dao.AttrGroupDao;
import com.lmarket.product.dao.SkuSaleAttrValueDao;
import com.lmarket.product.entity.BrandEntity;
import com.lmarket.product.service.BrandService;
import com.lmarket.product.service.CategoryService;
import com.lmarket.product.vo.SkuItemSaleAttrVo;
import com.lmarket.product.vo.SkuItemVo;
import com.lmarket.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@Slf4j
@SpringBootTest
class LmarketProductApplicationTests {

	@Autowired
	BrandService brandService;
//
	@Autowired
	CategoryService categoryService;

	@Autowired
	StringRedisTemplate stringRedisTemplate;

	@Autowired
	RedissonClient redissonClient;

	@Autowired
	AttrGroupDao attrGroupDao;

	@Autowired
	SkuSaleAttrValueDao skuSaleAttrValueDao;

	@Test
	public void test(){
//		List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(22L, 225L);
//		System.out.println(attrGroupWithAttrsBySpuId);

		List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(22L);
		System.out.println(saleAttrsBySpuId);
	}

	@Test
	public void redisson(){
		System.out.println(redissonClient);
	}

	@Test
	public void testStringRedisTemplate(){
		ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

		//保存
		ops.set("hello", "world_"+ UUID.randomUUID().toString());

		//查询
		String hello = ops.get("hello");
		System.out.println("之前保存的数据是："+hello);
	}

	@Test
	public void testFindPath(){
		Long[] catelogPath = categoryService.findCatelogPath(225L);
		log.info("完整路径：{}", Arrays.asList(catelogPath));
	}

	@Test
	void contextLoads() {

//		BrandEntity brandEntity = new BrandEntity();
//		brandEntity.setBrandId(1L);
//		brandEntity.setDescript("华为品牌");
////		//brandEntity.setDescript("测试");
////		brandEntity.setName("华为");
////		brandService.save(brandEntity);
////		System.out.println("保存成功！");
//		brandService.updateById(brandEntity);
//		System.out.println("更新成功！");

		List<BrandEntity> list=brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id",1L));
		list.forEach((item)->{
			System.out.println(item);
		});
	}
//
//	@Test
//	public void test11(){
//		Date date = new Date();
//		long time = date.getTime();
//		System.out.println("ooooo.."+date+"oooooooooooo..."+time);
//
//		Date date1 = new Date(1619575200000L);
//		Date date2 = new Date(1619575200000L);
//
//		System.out.println("date1..."+date1.toString()+"  date2..."+date2.toString());
//
//		LocalDateTime start = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
//		String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//		System.out.println("start........"+format);
//	}

}
