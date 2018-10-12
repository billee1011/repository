package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.id.ServerObject;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;

@Entity
@Table(name = "agent_log")
public class AgentLog extends ServerObject {

	@Column(name = "role_id")
	private long roleId;

	@Column(name = "to_role_id")
	private long toRoleId;

	@Column(name = "to_role_last_diamond", nullable = false, defaultValue = "0", comment = "钻石")
	private long toRoleLastDiamond;

	@Column(name = "role_lat_diamond", nullable = false, defaultValue = "0", comment = "钻石")
	private long roleLastDiamond;

	// 赠送的钻石
	@Column(name = "diamond", nullable = false, defaultValue = "0", comment = "钻石")
	private long diamond;

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

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public long getToRoleId() {
		return toRoleId;
	}

	public void setToRoleId(long toRoleId) {
		this.toRoleId = toRoleId;
	}

	public long getToRoleLastDiamond() {
		return toRoleLastDiamond;
	}

	public void setToRoleLastDiamond(long toRoleLastDiamond) {
		this.toRoleLastDiamond = toRoleLastDiamond;
	}

	public long getRoleLastDiamond() {
		return roleLastDiamond;
	}

	public void setRoleLastDiamond(long roleLastDiamond) {
		this.roleLastDiamond = roleLastDiamond;
	}

	public long getDiamond() {
		return diamond;
	}

	public void setDiamond(long diamond) {
		this.diamond = diamond;
	}
}