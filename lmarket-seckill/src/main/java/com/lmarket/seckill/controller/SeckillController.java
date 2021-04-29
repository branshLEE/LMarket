package com.lmarket.seckill.controller;

import com.common.utils.R;
import com.lmarket.seckill.service.SeckillService;
import com.lmarket.seckill.to.SeckillSkuRedisTo;
import com.lmarket.seckill.vo.SeckillSkuVo;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    SeckillService seckillService;

    /**
     * 返回当前时间可以参与的商品秒杀信息
     * @return
     */
    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(vos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){
        SeckillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setData(to);
    }

    @GetMapping("/kill")
    public String seckKill(@RequestParam("killId") String killId,
                           @RequestParam("key") String key,
                           @RequestParam("num") Integer num,
                           Model model){
        //1、先判断是否登录
        String orderSn = seckillService.kill(killId, key, num);
        model.addAttribute("seckillOrderSn", orderSn);

        return "success";
    }
}
