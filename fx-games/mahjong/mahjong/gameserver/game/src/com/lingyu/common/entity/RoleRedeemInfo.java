package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;
/**
 * 角色补偿记录表  <br/>
 * 这个类打死不能跨服啊 不能跨服啊<br/>
 * 跨服会把我的记录给盖掉的  会多发补偿的<br/> 
 *<br/>
 *                            切记 切记  慎之 慎之<br/>
 * @author Wang Shuguang
 *
 */
@Table(name = "role_redeem_info")
@Entity(fetch = FeatchType.START, delayInsert = false)
public class RoleRedeemInfo {
	@Id
	@IsRoleId
	@Column(name = "role_id")
	private long roleId;
	
	@Temporal
	@Column(name = "redeem_time", nullable = false, defaultValue = "2000-01-01 00:00:00")
	private Date redeemTime = TimeConstant.DATE_LONG_AGO;

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public Date getRedeemTime() {
		return redeemTime;
	}

	public void setRedeemTime(Date redeemTime) {
		this.redeemTime = redeemTime;
	}
}
