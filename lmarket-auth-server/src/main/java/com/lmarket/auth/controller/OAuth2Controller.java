package com.lmarket.auth.controller;

import com.alibaba.fastjson.TypeReference;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipaySystemOauthTokenRequest;
import com.alipay.api.request.AlipayUserInfoShareRequest;
import com.alipay.api.response.AlipaySystemOauthTokenResponse;
import com.alipay.api.response.AlipayUserInfoShareResponse;
import com.common.utils.R;
import com.lmarket.auth.feign.MemberFeignService;
import com.lmarket.auth.vo.MemberResponseVo;
import com.lmarket.auth.vo.Oauth2UserVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * 处理社交登录请求
 */

@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    MemberFeignService memberFeignService;

    @Value("${alipay.appId}")
    private String appId;

    @Value("${alipay.redirect_uri}")
    private String redirect_uri;

    @Value("${alipay.APP_PRIVATE_KEY}")
    private String APP_PRIVATE_KEY;

    @Value("${alipay.ALIPAY_PUBLIC_KEY}")
    private String ALIPAY_PUBLIC_KEY;

    @RequestMapping("alipayLogin")
    public String alipayLogin(){
        String redirect_uri1 = "";
        try {
            //url加密
            redirect_uri1 = URLEncoder.encode(redirect_uri, "utf-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }
        String url = "https://openauth.alipay.com/oauth2/publicAppAuthorize.htm?app_id="+appId+"&scope=auth_user&redirect_uri="+redirect_uri1;

        return "redirect:"+url;
    }

    @RequestMapping("oauth2.0/alipay/redirect")
    public String alipayRedirect(@RequestParam("app_id") String appId, @RequestParam("auth_code") String code, HttpSession session) throws AlipayApiException {
        System.out.println("啦啦啦啦。。。。");

        //通过auth_code获取第三方账号信息
        Oauth2UserVo vo = alipayUserInfo(appId, code);

        //第三方账号登录
        R oauth2Login = memberFeignService.oauth2Login(vo);

        if(oauth2Login.getCode() == 0){
            log.info("登录成功！");
//            System.out.println("哈哈哈哈哈哈哈哈哈   "+vo.getNick_name());
            session.setAttribute("loginUser", vo.getNick_name());
            return "redirect:http://lmarket.com";
        }else{
            return "redirect:http://auth.lmarket.com/login.html";
        }

    }

    private Oauth2UserVo alipayUserInfo(String appId, String code) throws AlipayApiException {

        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
                appId, APP_PRIVATE_KEY, "json", "GBK", ALIPAY_PUBLIC_KEY, "RSA2");
        AlipaySystemOauthTokenRequest request = new AlipaySystemOauthTokenRequest();
        request.setCode(code);
        request.setGrantType("authorization_code");
        AlipayUserInfoShareRequest infoShareRequest = new AlipayUserInfoShareRequest();
        AlipaySystemOauthTokenResponse oauthTokenResponse = alipayClient.execute(request);

        //获取令牌access_token
        String accessToken = oauthTokenResponse.getAccessToken();
        //获取到用户信息
        AlipayUserInfoShareResponse infoShareResponse = alipayClient.execute(infoShareRequest, accessToken);
        System.out.println(infoShareResponse.getBody());

        Oauth2UserVo oauth2UserVo = new Oauth2UserVo();

        oauth2UserVo.setUser_id(infoShareResponse.getUserId());
        oauth2UserVo.setAccess_token(accessToken);

        //获取的用户昵称，用于页面session
        oauth2UserVo.setNick_name(infoShareResponse.getNickName());

        return oauth2UserVo;
    }
}
