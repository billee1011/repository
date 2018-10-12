package com.lingyu.common.entity;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "announce")
public class Announce {

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	private int id;
	
	@Column(name = "content")
	private String content;
	
	@Column(name = "interval_gap")
	private int interval;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "begin_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date beginTime;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "end_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date endTime;
	
	@Column(name = "area_ids")
	private String areaIds; //
	
	@Column(name = "pid")
	private String pid;
	
	@Column(name="user_id")
	private int userId;
	
	@Column(name="is_exists")
	private boolean exists = true;
	
	@Column(name="pf")
	private String pf;
	
	
	private transient Set<Integer> areaIdSet = new HashSet<Integer>();
	
	private transient long lastPublishMillis = -1L;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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
	
	public String getAreaIds() {
		return areaIds;
	}

	public void setAreaIds(String areaIds) {
		this.areaIds = areaIds;
	}
	
	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}
	
	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}
	
	public void serialize(){
		StringBuilder sb = new StringBuilder();
		int index = 0;
		for(Integer areaId : areaIdSet){
			if(index > 0){
				sb.append(",");
			}
			sb.append(areaId);
			index++;
		}
		areaIds = sb.toString();
	}
	
	public Set<Integer> getAreaIdSet() {
		return areaIdSet;
	}

	public void setAreaIdSet(Set<Integer> areaIdSet) {
		this.areaIdSet = areaIdSet;
	}
	
	public long getLastPublishMillis() {
		return lastPublishMillis;
	}

	public void setLastPublishMillis(long lastPublishMillis) {
		this.lastPublishMillis = lastPublishMillis;
	}
	
	public boolean isExists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}
	
	public String getPf() {
		return pf;
	}

	public void setPf(String pf) {
		this.pf = pf;
	}

	public void derialize(){
		String[] ss = StringUtils.split(areaIds, ",");
		if(ArrayUtils.isNotEmpty(ss)){
			for(String s : ss){
				areaIdSet.add(Integer.parseInt(s));
			}
		}
	}
}
