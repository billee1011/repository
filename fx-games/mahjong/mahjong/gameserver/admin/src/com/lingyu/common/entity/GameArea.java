package com.lingyu.common.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.format.annotation.DateTimeFormat;

import com.lingyu.admin.vo.GameAreaVo;
import com.lingyu.admin.vo.SimpleGameAreaVo;
import com.lingyu.common.constant.SystemConstant;
import com.lingyu.common.constant.TimeConstant;

@Entity
@Table(name = "game_area")
public class GameArea implements IRpcOwner, Comparable<GameArea> {
	@Id
	@Column(name = "world_id", unique = true, nullable = false)
	private int worldId;

	@Column(name = "area_id")
	private int areaId;

	@Column(name = "world_name")
	private String worldName;

	@Column(name = "area_name")
	private String areaName;

	@Column(name = "area_type")
	private int type;

	@Column(name = "external_ip")
	private String externalIp;

	@Column(name = "tcp_port")
	private int tcpPort;

	// 对内ip
	@Column(name = "ip")
	private String ip;

	// 对内端口
	@Column(name = "port")
	private int port;

	@Column(name = "pid")
	private String pid;

	@Column(name = "status")
	private int status;

	@Column(name = "follower_id")
	private int followerId;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "add_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date addTime; // 开始时间
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "combine_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date combineTime = TimeConstant.DATE_LONG_AGO; // 合服时间
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "restart_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date restartTime = TimeConstant.DATE_LONG_AGO; // 起服时间

	private transient GameAreaVo gameAreaVo;

	private transient Set<GameArea> childAreas = new HashSet<GameArea>();

	public int getWorldId() {
		return worldId;
	}

	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	public String getWorldName() {
		return worldName;
	}

	public void setWorldName(String worldName) {
		this.worldName = worldName;
	}

	@Override
	public boolean isValid() {
		return status != SystemConstant.SERVER_STATUS_STOPED;
	}

	@Override
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

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

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public String getExternalIp() {
		return externalIp;
	}

	public void setExternalIp(String externalIp) {
		this.externalIp = externalIp;
	}

	public int getTcpPort() {
		return tcpPort;
	}

	public void setTcpPort(int tcpPort) {
		this.tcpPort = tcpPort;
	}

	@Override
	public int getFollowerId() {
		return followerId;
	}

	public void setFollowerId(int followerId) {
		this.followerId = followerId;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}

	public GameAreaVo toGameAreaVo() {
		GameAreaVo ret = new GameAreaVo();
		ret.setWorldId(getWorldId());
		ret.setWorldName(getWorldName());
		ret.setAreaId(getAreaId());
		ret.setAreaName(getAreaName());
		// ret.setType(getType());
		ret.setExternalIp(getExternalIp());
		ret.setTcpPort(getTcpPort());
		ret.setIp(getIp());
		ret.setPort(getPort());
		// ret.setPlatformName(platformName);
		ret.setStatus(status);
		ret.setValid(isValid());
		ret.setFollowerId(getFollowerId());
		ret.setAddTime(getAddTime());
		return ret;
	}

	public SimpleGameAreaVo toSimpleGameAreaVo() {
		SimpleGameAreaVo ret = new SimpleGameAreaVo();
		ret.setId(areaId);
		ret.setName(areaName);
		ret.setValid(isValid());
		return ret;
	}

	public GameAreaVo getGameAreaVo() {
		return gameAreaVo;
	}

	public void setGameAreaVo(GameAreaVo gameAreaVo) {
		this.gameAreaVo = gameAreaVo;
	}

	public Set<GameArea> getChildAreas() {
		return childAreas;
	}

	public void setChildAreas(Set<GameArea> childAreas) {
		this.childAreas = childAreas;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}


	public Date getCombineTime() {
		return combineTime;
	}

	public void setCombineTime(Date combineTime) {
		this.combineTime = combineTime;
	}

	public Date getRestartTime() {
		return restartTime;
	}

	public void setRestartTime(Date restartTime) {
		this.restartTime = restartTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + worldId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GameArea other = (GameArea) obj;
		if (worldId != other.worldId)
			return false;
		return true;
	}

	public SimpleGameArea toSimpleGameArea(String platformName) {
		SimpleGameArea ret = new SimpleGameArea();
		ret.setPid(pid);
		ret.setPlatformName(platformName);
		ret.setId(areaId);
		ret.setName(areaName);
		ret.setWorldId(worldId);
		ret.setWorldName(worldName);
		ret.setStatus(status);
		return ret;
	}
	
	public Object[] toAreaVO() {
		return new Object[] { areaId, areaName, worldId, worldName };
	}

	public int compareTo(GameArea obj) {
		if(addTime.getTime()>obj.getAddTime().getTime()){
			return 1;
		}else if(addTime.getTime()==obj.getAddTime().getTime()){
			if(worldId>obj.getWorldId()){
				return 1;
			}else{
				return -1;
			}
		}
		else{
			return -1;
		}
	}
}
