package com.lmarket.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.common.constant.AuthServerConstant;
import com.common.exception.BizCodeEnume;
import com.common.utils.R;
import com.common.vo.MemberResponseVo;
import com.lmarket.auth.feign.MemberFeignService;
import com.lmarket.auth.feign.ThirdPartFeignService;
import com.lmarket.auth.vo.UserLoginVo;
import com.lmarket.auth.vo.UserRegistVo;
import lombok.AllArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

    @Autowired
    MemberFeignService memberFeignService;

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
            return "redirect:http://auth.lmarket.market/reg.html";
        }


        //1、校验验证码
        String code = vo.getCode();

        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if(!StringUtils.isEmpty(s)){
            if(code.equals(s.split("_")[0])){
                //删除验证码 令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //验证码通过 注册，调用远程服务进行注册
                R regist = memberFeignService.regist(vo);
                if(regist.getCode() == 0){
                    //成功

                    return "redirect:http://auth.lmarket.market/login.html";
                }else{
                    //失败
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", regist.getData(new TypeReference<String>(){}));
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.lmarket.market/reg.html";
                }

            }else{
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                //校验出错，转发到注册页面
                return "redirect:http://auth.lmarket.market/reg.html";
            }
        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", errors);
            //校验出错，转发到注册页面
            return "redirect:http://auth.lmarket.market/reg.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){

        //远程登录
        R login = memberFeignService.login(vo);
        if(login.getCode() == 0){
            //成功放到session中
            MemberResponseVo data = login.getData("data", new TypeReference<MemberResponseVo>() {
            });
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "redirect:http://lmarket.market";
        }else{
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.lmarket.market/login.html";
        }

    }


}
