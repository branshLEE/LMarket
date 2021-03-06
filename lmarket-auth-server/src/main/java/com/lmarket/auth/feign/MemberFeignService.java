package com.lmarket.auth.feign;

import com.common.utils.R;
import com.lmarket.auth.vo.Oauth2UserVo;
import com.lmarket.auth.vo.UserLoginVo;
import com.lmarket.auth.vo.UserRegistVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("lmarket-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    R oauth2Login(@RequestBody Oauth2UserVo vo);
}
