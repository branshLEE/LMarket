package com.lmarket.auth.vo;

import lombok.Data;

@Data
public class Oauth2UserVo {
    private String access_token;
    private String user_id;
    private String avatar;
    private String province;
    private String city;
    private String nick_name;
    private String gender;
}
