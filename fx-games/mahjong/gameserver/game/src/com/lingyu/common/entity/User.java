package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.common.id.ServerObject;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;

/**
 * 用戶表
 * @author wangning
 * @date 2016年12月6日 下午4:34:03
 */
@Table(name = "user")
@Entity(fetch = FeatchType.START, delayInsert = false)
public class User extends ServerObject{
	
	@Column(name = "pid", nullable = false, length = 20, comment = "平台 ios or android")
	private String pid;
	
	@Column(name = "user_id", nullable = false, length = 128, comment = "用户账号")
	private String userId;
	
	@Column(name = "access_token", length = 128, comment = "微信的token")
	private String accessToken;
	
	@Column(name = "refresh_token", length = 128, comment = "微信的刷新token(续期accessToken用)")
	private String refreshToken;
	
	@Column(name = "maching_id", nullable = false, length = 64, comment = "设备码")
	private String machingId;
	
	@Column(name = "type", nullable = false, defaultValue = "0", comment = "玩家类型")
	private int type;
	
	@Temporal
	@Column(name = "token_end_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "token的过期时间")
	private Date tokenEndTime = TimeConstant.DATE_LONG_AGO;
	
	@Temporal
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime = TimeConstant.DATE_LONG_AGO;

	@Temporal
	@Column(name = "modify_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "修改时间")
	private Date modifyTime = TimeConstant.DATE_LONG_AGO;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Date getTokenEndTime() {
		return tokenEndTime;
	}

	public void setTokenEndTime(Date tokenEndTime) {
		this.tokenEndTime = tokenEndTime;
	}

	public String getMachingId() {
		return machingId;
	}

	public void setMachingId(String machingId) {
		this.machingId = machingId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}