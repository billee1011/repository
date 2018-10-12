package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.id.ServerObject;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;
/**
 * 公告 
 */
@Entity(fetch=FeatchType.START)
@Table(name = "announce")
public class Announce extends ServerObject{
	@Column(name="announce_id")
	private int announceId;
	
	@Column(name = "content")
	private String content;
	
	@Column(name = "interval_gap")
	private int interval;
	@Temporal
	@Column(name = "begin_time")
	private Date beginTime;
	@Temporal
	@Column(name = "end_time")
	private Date endTime;
	
	public int getAnnounceId() {
		return announceId;
	}
	public void setAnnounceId(int announceId) {
		this.announceId = announceId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getInterval() {
		return interval;
	}
	public void setInterval(int interval) {
		this.interval = interval;
	}
	public Date getBeginTime() {
		return beginTime;
	}
	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
}
