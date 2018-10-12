package com.lingyu.common.entity;

import java.util.Date;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.lingyu.common.util.TimeUtil;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.Table;

@Entity
@Table(name = "server_info")
public class ServerInfo {

	@Id
	@Column(name = "id")
	private int id;
	@Column(name = "name")
	private String name;
	@Column(name = "status")
	private int status;// 状态
	@Column(name = "times")
	private int times;// 重启次数
	@Column(name = "cq_time")
	private Date openTime;// 重启时间
	@Column(name = "start_time")
	private Date startTime;// 服务器开区时间
	@Column(name = "maintain_time")
	private Date maintainTime;// 维护时间
	@Column(name = "combine_time")
	private Date combineTime;// 和服时间

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getTimes() {
		return times;
	}

	public void setTimes(int times) {
		this.times = times;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Date getOpenTime() {
		return openTime;
	}

	public void setOpenTime(Date openTime) {
		this.openTime = openTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getMaintainTime() {
		return maintainTime;
	}

	public void setMaintainTime(Date maintainTime) {
		this.maintainTime = maintainTime;
	}

	public Date getCombineTime() {
		return combineTime;
	}

	public void setCombineTime(Date combineTime) {
		this.combineTime = combineTime;
	}

	public void updateTimes(int times) {
		this.times = times;

	}

	public int getOpenedDays() {
		return TimeUtil.subDateToDay(this.getStartTime(), new Date());
	}

	public int getOpenedMonths() {
		return TimeUtil.subDateToMonth(this.getStartTime(), new Date());
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}