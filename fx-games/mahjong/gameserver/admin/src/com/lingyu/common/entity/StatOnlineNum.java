package com.lingyu.common.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.lingyu.admin.vo.PlayerNumVO;

@Entity
@Table(name = "stat_online_num")
public class StatOnlineNum {
	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	@Column(name = "pid")
	private String pid;
	@Column(name = "area_id")
	private int areaId;
	@Column(name = "num")
	private int num;
	@Column(name = "add_time")
	private Date addTime;
	@Transient
	private int worldId;

	public int getWorldId() {
		return worldId;
	}

	public void setWorldId(int worldId) {
		this.worldId = worldId;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public int getNum() {
		return num;
	}

	public void setNum(int num) {
		this.num = num;
	}

	public int getId() {
		return this.id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAreaId() {
		return areaId;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public void setAreaId(int areaId) {
		this.areaId = areaId;
	}

	public PlayerNumVO toVO() {
		PlayerNumVO ret = new PlayerNumVO();
		ret.setNum(num);
		ret.setAddTime(addTime);
		return ret;
	}
}
