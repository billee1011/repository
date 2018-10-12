package com.lingyu.common.entity;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import com.lingyu.common.constant.TimeConstant;

@Entity
@Table(name = "redeem_record")
public class RedeemRecord {
	/** 申请中 */
	public static final int STATUS_APPLYING = 0;
	/** 同意 */
	public static final int STATUS_ACCEPTED = 1;
	/** 拒绝 */
	public static final int STATUS_REJECTED = 2;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	private int id;

	@Column(name = "admin_id")
	private int adminId;

	@Column(name = "admin_name")
	// 防止补完删除管理员号的情况
	private String adminName;

	@Column(name = "ip")
	// ip地址
	private String ip;

	@Column(name = "coin")
	private long coin;

	@Column(name = "diamond")
	private int diamond;

	@Column(name = "all_area")
	private boolean allArea;

	@Column(name = "areas")
	private String areas;

	@Column(name = "area_id_list")
	private String areaIdList;

	@Column(name = "is_all")
	private boolean isAll;

	@Column(name = "players")
	private String players;

	@Column(name = "items")
	private String items;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "add_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date addTime;

	/**
	 * 申请状态
	 */
	@Column(name = "status")
	private int status = STATUS_APPLYING;

	/**
	 * 审核时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "check_time")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date checkTime = TimeConstant.DATE_LONG_AGO;

	/** 审核人ID */
	@Column(name = "check_admin_id")
	private int checkAdminId;

	/** 审核人名字 */
	@Column(name = "check_admin_name")
	private String checkAdminName;

	/** 补偿message */
	@Column(name = "redeem_msg")
	private String redeemMsg;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getAdminId() {
		return adminId;
	}

	public void setAdminId(int adminId) {
		this.adminId = adminId;
	}

	public String getAdminName() {
		return adminName;
	}

	public void setAdminName(String adminName) {
		this.adminName = adminName;
	}

	public long getCoin() {
		return coin;
	}

	public void setCoin(long coin) {
		this.coin = coin;
	}

	public int getDiamond() {
		return diamond;
	}

	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}

	public String getItems() {
		return items;
	}

	public void setItems(String items) {
		this.items = items;
	}

	public boolean isAll() {
		return isAll;
	}

	public void setAll(boolean isAll) {
		this.isAll = isAll;
	}

	public String getPlayers() {
		return players;
	}

	public void setPlayers(String players) {
		this.players = players;
	}

	public String getAreas() {
		return areas;
	}

	public void setAreas(String areas) {
		this.areas = areas;
	}

	public boolean isAllArea() {
		return allArea;
	}

	public void setAllArea(boolean allArea) {
		this.allArea = allArea;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Date getCheckTime() {
		return checkTime;
	}

	public void setCheckTime(Date checkTime) {
		this.checkTime = checkTime;
	}

	public int getCheckAdminId() {
		return checkAdminId;
	}

	public void setCheckAdminId(int checkAdminId) {
		this.checkAdminId = checkAdminId;
	}

	public String getCheckAdminName() {
		return checkAdminName;
	}

	public void setCheckAdminName(String checkAdminName) {
		this.checkAdminName = checkAdminName;
	}

	public String getAreaIdList() {
		return areaIdList;
	}

	public void setAreaIdList(String areaIdList) {
		this.areaIdList = areaIdList;
	}

	public String getRedeemMsg() {
		return redeemMsg;
	}

	public void setRedeemMsg(String redeemMsg) {
		this.redeemMsg = redeemMsg;
	}

}
