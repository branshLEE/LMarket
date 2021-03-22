package com.lmarket.product.web;

import com.lmarket.product.entity.CategoryEntity;
import com.lmarket.product.service.CategoryService;
import com.lmarket.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){

        //TODO 1、查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();

        //视图解析器进行拼串
        //classpath:/templates/ +返回值+ .html
        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    //index/catelog.json
    @ResponseBody
    @GetMapping("/index/catelog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson(){

        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        //1、获取一把锁，只要锁的名字一样，就是同一把锁
        RLock myLock = redisson.getLock("myLock");

        //2、加锁
        //myLock.lock(); //每隔10秒自动续期，直到任务执行结束
        myLock.lock(30, TimeUnit.SECONDS); //30秒后自动解锁，自动解锁时间一定要大于业务执行时间(因为这个lock()不会自动续期)
        try {

            System.out.println("加锁成功，执行业务"+Thread.currentThread().getId());
            Thread.sleep(30000);
        }catch (Exception e){

        }finally {
            //解锁
            System.out.println("释放锁"+Thread.currentThread().getId());
            myLock.unlock();
        }

        return "hello";
    }
}
