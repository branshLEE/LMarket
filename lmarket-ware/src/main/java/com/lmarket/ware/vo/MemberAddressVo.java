package com.lmarket.ware.vo;

import lombok.Data;

@Data
public class MemberAddressVo {
    private Long id;
    /**
     * member_id
     */
    private Long memberId;
    /**
     *
     */
    private String name;
    /**
     *
     */
    private String phone;
    /**
     *
     */
    private String postCode;
    /**
     * ʡ
     */
    private String province;
    /**
     *
     */
    private String city;
    /**
     *
     */
    private String region;
    /**
     *
     */
    private String detailAddress;
    /**
     * ʡ
     */
    private String areacode;
    /**
     *
     */
    private Integer defaultStatus;
}
