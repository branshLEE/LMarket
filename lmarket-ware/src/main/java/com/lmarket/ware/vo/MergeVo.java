package com.lmarket.ware.vo;

import lombok.Data;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

@Data
public class MergeVo {

    private Long purchaseId;
    private List<Long> items;
}
