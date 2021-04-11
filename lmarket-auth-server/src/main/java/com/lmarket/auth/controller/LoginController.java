package com.lmarket.auth.controller;

import com.common.constant.AuthServerConstant;
import com.common.exception.BizCodeEnume;
import com.common.utils.R;
import com.lmarket.auth.feign.ThirdPartFeignService;
import com.lmarket.auth.vo.UserRegistVo;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Autowired
    ThirdPartFeignService thirdPartFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone") String phone){

        //TODO 1、接口防刷

        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if(!StringUtils.isEmpty(redisCode)){
            long l = Long.parseLong(redisCode.split("_")[1]);
            if(System.currentTimeMillis() - l < 60000){
                //60秒内不再发验证码
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(), BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2、验证码的再次校验：存到redis key-phone, value-code
        String code = UUID.randomUUID().toString().substring(0, 6)+"_"+System.currentTimeMillis();

        //redis缓存验证码，防止同一个手机号在60秒内再次发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone, code, 10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone, code.split("_")[0]);
        return R.ok();
    }

    /**
     * RedirectAttributes redirectAttributes：模拟重定向携带数据
     * @param vo
     * @param result
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){
        if(result.hasErrors()){

            /**
             * .map(fieldError -> {
             *                 String field = fieldError.getField();
             *                 String defaultMessage = fieldError.getDefaultMessage();
             *                 errors.put(field, defaultMessage);
             *                 return
             *             })
             */

            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(
                    FieldError::getField, FieldError::getDefaultMessage, (newValue,oldValue)->newValue));
//            model.addAttribute("errors", errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            //校验出错，转发到注册页面
            return "redirect:http://auth.lmarket.com/reg.html";
        }

        //注册，调用远程服务进行注册

        //注册成功回到首页，回到登录页
        return "redirect:/login.html";
    }
}
