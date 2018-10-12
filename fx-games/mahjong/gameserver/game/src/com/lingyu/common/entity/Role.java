package com.lingyu.common.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Entity.FeatchType;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Json;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;

/**
 * 角色表
 * @author wangning
 * @date 2016年12月6日 下午4:33:55
 */
@Table(name = "role")
@Entity(fetch = FeatchType.START)
public class Role {
	
	@Id
	@IsRoleId
	@Column(name = "id")
	private long id;
	
	@Column(name = "pid", length = 20, nullable = false, comment = "平台 ios or android")
	private String pid;
	
	@Column(name = "user_id", length = 128, nullable = false, comment = "用户账号")
	private String userId;
	
	@Column(name = "name", length = 256, nullable = false, comment = "角色名")
	private String name;
	
	/** 1=男 2=女*/
	@Column(name = "gender", nullable = false, defaultValue = "0", comment = "性別")
	private int gender;
	
	@Column(name = "province", length = 128, comment = "省份")
	private String province;
	
	@Column(name = "city", length = 64, comment = "城市")
	private String city;
	
	@Column(name = "country", length = 64, comment = "country")
	private String country;
	
	@Column(name = "headimgurl", length = 64, comment = "头像名")
	private String headimgurl;
	
	// 钻石
	@Column(name = "diamond", nullable = false, defaultValue = "0", comment = "钻石")
	private long diamond;
	
	@Column(name = "state", nullable = false, defaultValue = "0", comment = "状态")
	private int state;
	
	@Column(name = "ip", length = 30, comment = "ip")
	private String ip;
	
	@Column(name = "total_login_days", defaultValue = "0", comment = "累计登陆天数")
	private int totalLoginDays;
	
	@Column(name = "online_millis", nullable = false, defaultValue = "0", comment = "角色在线时长总和")
	private long onlineMillis;
	
	@Temporal
	@Column(name = "last_login_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "最后登录时间")
	private Date lastLoginTime;
	
	@Temporal
	@Column(name = "last_logout_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "最后登出时间")
	private Date lastLogoutTime;
	
	@Json
	@Column(name = "log_ids", length = 65535, defaultValue = "{}", comment = "所有战绩的集合")
	private List<Long> logIds = new ArrayList<>();
	
	@Temporal
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime = TimeConstant.DATE_LONG_AGO;

	@Temporal
	@Column(name = "modify_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "修改时间")
	private Date modifyTime = TimeConstant.DATE_LONG_AGO;
	
	// 角色房间号 缓存
	private int roomNum;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}
	
	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getHeadimgurl() {
		return headimgurl;
	}

	public void setHeadimgurl(String headimgurl) {
		this.headimgurl = headimgurl;
	}

	public long getDiamond() {
		return diamond;
	}

	public void setDiamond(long diamond) {
		this.diamond = diamond;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getTotalLoginDays() {
		return totalLoginDays;
	}

	public void setTotalLoginDays(int totalLoginDays) {
		this.totalLoginDays = totalLoginDays;
	}

	public long getOnlineMillis() {
		return onlineMillis;
	}

	public void setOnlineMillis(long onlineMillis) {
		this.onlineMillis = onlineMillis;
	}

	public Date getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public Date getLastLogoutTime() {
		return lastLogoutTime;
	}

	public void setLastLogoutTime(Date lastLogoutTime) {
		this.lastLogoutTime = lastLogoutTime;
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

	public int getRoomNum() {
		return roomNum;
	}

	public void setRoomNum(int roomNum) {
		this.roomNum = roomNum;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	public List<Long> getLogIds() {
		return logIds;
	}

	public void setLogIds(List<Long> logIds) {
		this.logIds = logIds;
	}
}