package com.lmarket.member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 
 * 
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 23:40:43
 */
@Data
@TableName("ums_integration_change_history")
public class IntegrationChangeHistoryEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * member_id
	 */
	private Long memberId;
	/**
	 * create_time
	 */
	private Date createTime;
	/**
	 * 
	 */
	private Integer changeCount;
	/**
	 * 
	 */
	private String note;
	/**
	 * 
	 */
	private Integer sourceTyoe;

}
