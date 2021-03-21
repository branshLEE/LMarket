package com.lmarket.product;
//
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lmarket.product.entity.BrandEntity;
import com.lmarket.product.service.BrandService;
import com.lmarket.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
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

}
