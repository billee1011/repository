package com.cai.common.domain;

import java.util.Date;

/**
 * 大厅app升级包管理
 * @author Administrator
 *
 */
public class AppItem {
	
	private int id;
	private int gameSeq;//单个游戏升级序列号
	private int appId;//子游戏id
	private String appName;//子游戏名称
	private String packagepath;//整包名
	private String versions;//包版本信息
	private String iconUrl;//图标url
	private int t_status;//游戏状态，0未发布，1发布
	private int orders;//游戏的展示顺序
	private int flag;//游戏标志，new,hot,活动，比赛
	private int packagesize;//整包大小
	private String downUrl;//差异包相对路径
	private int size;//差异包大小
	private Date operate_at;
	private String packageDownPath;//包下载路径
	private int game_type;//游戏类型
	
	
	
	public int getGame_type() {
		return game_type;
	}
	public void setGame_type(int game_type) {
		this.game_type = game_type;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getGameSeq() {
		return gameSeq;
	}
	public void setGameSeq(int gameSeq) {
		this.gameSeq = gameSeq;
	}
	public int getAppId() {
		return appId;
	}
	public void setAppId(int appId) {
		this.appId = appId;
	}
	public String getPackagepath() {
		return packagepath;
	}
	public void setPackagepath(String packagepath) {
		this.packagepath = packagepath;
	}
	public String getIconUrl() {
		return iconUrl;
	}
	public void setIconUrl(String iconUrl) {
		this.iconUrl = iconUrl;
	}
	public String getVersions() {
		return versions;
	}
	public void setVersions(String versions) {
		this.versions = versions;
	}
	public int getT_status() {
		return t_status;
	}
	public void setT_status(int t_status) {
		this.t_status = t_status;
	}
	public Date getOperate_at() {
		return operate_at;
	}
	public void setOperate_at(Date operate_at) {
		this.operate_at = operate_at;
	}
	public int getOrders() {
		return orders;
	}
	public void setOrders(int orders) {
		this.orders = orders;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public int getPackagesize() {
		return packagesize;
	}
	public void setPackagesize(int packagesize) {
		this.packagesize = packagesize;
	}
	public String getDownUrl() {
		return downUrl;
	}
	public void setDownUrl(String downUrl) {
		this.downUrl = downUrl;
	}
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getPackageDownPath() {
		return packageDownPath;
	}
	public void setPackageDownPath(String packageDownPath) {
		this.packageDownPath = packageDownPath;
	}
	
	
}
