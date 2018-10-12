package com.lingyu.common.entity;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import com.alibaba.fastjson.JSON;
import com.lingyu.admin.vo.UserVo;

@Entity
@Table(name = "user")
public class User {
	/**
	 * 主键id
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	private int id;
	/**
	 * 用户名
	 */
	@Column(name = "name")
	private String name;

	/** 昵称 */
	@Column(name = "nick_name")
	private String nickName;

	/**
	 * 密码
	 */
	@Column(name = "password")
	private String password;
	/**
	 * 邮件
	 */
	@Column(name = "email")
	private String email;

	/** 上次登录的区 */
	@Column(name = "last_area_id")
	private int lastAreaId;
	/** 上次平台选择 */
	@Column(name = "last_pid")
	private String lastPid;

	@Column(name = "role_id")
	private int roleId;

	@Column(name = "privileges")
	private String privileges;

	private transient Map<Integer, Integer> privilegeMap = new HashMap<>();
	// 记录连续登录错误的次数
	@Column(name = "login_failed")
	private int loginFailed = 0;
	/**
	 * 创建时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "add_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date addTime;
	/**
	 * 上次修改时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "modify_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date modifyTime;
	
	@Column(name = "last_login_ip")
	private String lastLoginIp;

	private transient List<String> platformIdList = new ArrayList<>();

	public Integer getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getRoleId() {
		return roleId;
	}

	public void setRoleId(int roleId) {
		this.roleId = roleId;
	}

	public boolean isAuthorize(int code) {
		return privilegeMap.containsKey(code);
	}

	public void serialize() {
		privileges = JSON.toJSONString(privilegeMap.values());
	}

	public void deserialize() {
		List<Integer> list = JSON.parseArray(privileges, Integer.class);
		for (int code : list) {
			privilegeMap.put(code, code);
		}
	}

	public Collection<Integer> getPrivilegeList() {
		return privilegeMap.values();
	}

	public void setPrivilegeList(Collection<Integer> privilegeList) {
		privilegeMap.clear();
		for (int code : privilegeList) {
			privilegeMap.put(code, code);
		}

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

	public int getLastAreaId() {
		return lastAreaId;
	}

	public void setLastAreaId(int lastAreaId) {
		this.lastAreaId = lastAreaId;
	}

	public String getLastPid() {
		return lastPid;
	}

	public int getLoginFailed() {
		return loginFailed;
	}

	public void setLoginFailed(int loginFailed) {
		this.loginFailed = loginFailed;
	}

	public void setLastPid(String lastPid) {
		this.lastPid = lastPid;
	}

	public List<String> getPlatformIdList() {
		return platformIdList;
	}

	public void setPlatformIdList(List<String> platformIdList) {
		this.platformIdList = platformIdList;
	}
	
	public String getLastLoginIp() {
		return lastLoginIp;
	}

	public void setLastLoginIp(String lastLoginIp) {
		this.lastLoginIp = lastLoginIp;
	}

	public UserVo toUserVo(String lastAreaName, String roleName){
		UserVo ret = new UserVo();
		ret.setId(id);
		ret.setName(name);
		ret.setNickName(nickName);
		ret.setAddTime(addTime);
		ret.setEmail(email);
		ret.setLastAreaName(lastAreaName);
		ret.setRoleName(roleName);
		ret.setPlatformIdList(platformIdList);
		return ret;
	}
}
