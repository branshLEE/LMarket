package com.lmarket.order.web;

import com.lmarket.order.service.OrderService;
import com.lmarket.order.vo.OrderConfirmVo;
import com.lmarket.order.vo.OrderSubmitVo;
import com.lmarket.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
public class OrderWebController {

    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", confirmVo);

        //展示订单确认的数据
        return "confirm";
    }

    /**
     * 提交订单（下单）
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){

        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
        if(responseVo.getCode() == 0){
            //成功下单，去支付页面
            model.addAttribute("SubmitOrderResponse", responseVo);
            return "pay";
        }else{
            String msg = "下单失败";
            switch (responseVo.getCode()){
                case 1: msg+="订单信息过期，请刷新再次提交"; break;
                case 2: msg+="订单商品价格发生变化，请确认后再次提交"; break;
                case 3: msg+="库存锁定失败，商品库存不足"; break;
            }
            redirectAttributes.addFlashAttribute("msg", msg);
            return "redirect:http://order.lmarket.com/toTrade";
        }
        //下单成功，来到支付选择页
        //下单失败，回到订单确认页重新确认订单信息
    }
}
