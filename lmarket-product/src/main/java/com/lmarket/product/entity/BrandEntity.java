package com.lmarket.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.common.valid.AddGroup;
import com.common.valid.ListValue;
import com.common.valid.UpdataGroup;
import com.common.valid.UpdateStatusGroup;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.*;

/**
 * Ʒ
 * 
 * @author branshlee
 * @email branshLEE@gmail.com
 * @date 2021-02-22 13:31:35
 */
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Ʒ
	 */
	@Null(message = "新增不能指定品牌id", groups = {AddGroup.class})
	@NotNull(message = "修改必须指定品牌id", groups = {UpdataGroup.class})
	@TableId
	private Long brandId;
	/**
	 * Ʒ
	 */
	@NotBlank(message = "品牌名必须提交", groups = {UpdataGroup.class, AddGroup.class})
	private String name;
	/**
	 * Ʒ
	 */
	@NotEmpty(groups = {AddGroup.class})
	@URL(message = "logo必须是一个合法的url地址", groups = {AddGroup.class, UpdataGroup.class})
	private String logo;
	/**
	 * 
	 */
	private String descript;
	/**
	 * 
	 */

	@NotNull(groups = {AddGroup.class, UpdateStatusGroup.class})
	@ListValue(values={0,1}, groups = {AddGroup.class, UpdateStatusGroup.class})
	private Integer showStatus;
	/**
	 * 
	 */
	@NotEmpty(groups = {AddGroup.class})
	@Pattern(regexp = "^[a-zA-Z]$", message = "检索首字母必须是一个字母", groups = {AddGroup.class, UpdataGroup.class})
	private String firstLetter;
	/**
	 * 
	 */
	@NotNull(groups = {AddGroup.class})
	@Min(value = 0, message = "排序必须大于等于0", groups = {AddGroup.class, UpdataGroup.class})
	private Integer sort;

}
