package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.id.ServerObject;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;

@Table(name = "agent")
@Entity(fetch = FeatchType.START, delayInsert = false)
public class Agent extends ServerObject {
	@Column(name = "role_id")
	private long roleId;

	@Column(name = "type", nullable = false, comment = "代理级别")
	private int type; // 代理类型，0：admin最高级代理，1：一级代理，2：二级代理

	@Temporal
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime = TimeConstant.DATE_LONG_AGO;

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public Date getAddTime() {
		return addTime;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}
}