package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.id.ServerObject;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;

/**
 * 货币流
 * @author wangning
 * @date 2017年2月20日 下午3:23:04
 */
@Entity
@Table(name = "money_flow_log")
public class MoneyFlowLog extends ServerObject {
	/** 平台ID */
	@Column(name = "pid", length = 64, comment = "平台")
	private String pid;
	
	/** 区id */
	@Column(name = "area_id", nullable = false, defaultValue = "0", comment = "区id")
	private int areaId;
	
	@Column(name = "world_id", nullable = false, defaultValue = "0", comment = "world_id")
	private int worldId;
	
	@Column(name = "user_id", length = 1024, comment = "平台")
	private String userId;

	/** 角色id */
	@IsRoleId
	@Column(name = "role_id")
	private long roleId;

	/** 货币类型 如：1:铜币 2:钻石 3:绑定钻石 */
	@Column(name = "currency_type", nullable = false, defaultValue = "0", comment = "货币类型")
	private int currencyType;

	/** 增加或者消耗（0 消耗 1增加） */
	@Column(name = "use_type", nullable = false, defaultValue = "0", comment = "增加或者消耗")
	private int useType;

	/** 变化前数量 */
	@Column(name = "before_value", nullable = false, defaultValue = "0", comment = "变化前数量")
	private long beforeValue;

	/** 变更数量 */
	@Column(name = "value", nullable = false, defaultValue = "0", comment = "变更数量")
	private long value;

	/** 变化后数量 */
	@Column(name = "after_value", nullable = false, defaultValue = "0", comment = "变化后数量")
	private long afterValue;

	/** 操作类型 */
	@Column(name = "operate_type", nullable = false, defaultValue = "0", comment = "操作类型")
	private int operateType;

	@Temporal
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime;

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public int getAreaId() {
		return areaId;
	}

	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}

	public int getWorldId() {
		return worldId;
	}

	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public int getCurrencyType() {
		return currencyType;
	}

	public void setCurrencyType(int currencyType) {
		this.currencyType = currencyType;
	}

	public int getUseType() {
		return useType;
	}

	public void setUseType(int useType) {
		this.useType = useType;
	}

	public long getBeforeValue() {
		return beforeValue;
	}

	public void setBeforeValue(long beforeValue) {
		this.beforeValue = beforeValue;
	}

	public long getValue() {
		return value;
	}

	public void setValue(long value) {
		this.value = value;
	}

	public long getAfterValue() {
		return afterValue;
	}

	public void setAfterValue(long afterValue) {
		this.afterValue = afterValue;
	}

	public int getOperateType() {
		return operateType;
	}

	public void setOperateType(int operateType) {
		this.operateType = operateType;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}
}
