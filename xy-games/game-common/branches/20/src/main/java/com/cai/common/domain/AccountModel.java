package com.cai.common.domain;

import java.util.Date;

/**
 * 账号
 * @author run
 *
 */
public class AccountModel extends DBBaseModel{

	/**
	 * 账号id
	 */
	private long account_id; 
	
	/**
	 * 平台标识, WX=微信，SELF=自有
	 */
	private String pt;
	/**
	 * 账号名
	 */
	private String account_name; 
	/**
	 * 密码
	 */
	private String password; 
	/**
	 * 登录次数
	 */
	private int login_times; 
	/**
	 * 注册时间
	 */
	private Date create_time; 
	/**
	 * 最后登录时间
	 */
	private Date last_login_time; 
	/**
	 * 手机号
	 */
	private String mobile_phone; 
	/**
	 * 历史充值总额
	 */
	private long history_pay_gold;
	/**
	 * 金币数量
	 */
	private long gold; 
	
	/**
	 * 铜钱数量
	 */
	private long money;
	
	/**
	 * 最后登录的ip,服务端获取的
	 */
	private String client_ip;
	
	/**
	 * 最后登录ip,客户端传的
	 */
	private String client_ip2;
	
	/**
	 * 1-封禁 0-不封禁
	 */
	private int banned;
	
	/**
	 * 是否代理(1-是 0-否)
	 */
	private int is_agent;
	
	/**
	 * 今日在线时长(秒)
	 */
	private int today_online;
	
	/**
	 * 历史在线时长(秒)
	 */
	private int history_online;
	
	/**
	 * 最后登录的设备
	 */
	private String last_client_flag;
	
	/**
	 * 客户端最后登录版本
	 */
	private String client_version;
	
	/**
	 * 推荐人账号id
	 */
	private long recommend_id;
	
	public long getAccount_id() {
		return account_id;
	}
	public void setAccount_id(long account_id) {
		this.account_id = account_id;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getAccount_name() {
		return account_name;
	}
	public void setAccount_name(String account_name) {
		this.account_name = account_name;
	}
	public int getLogin_times() {
		return login_times;
	}
	public void setLogin_times(int login_times) {
		this.login_times = login_times;
	}
	public Date getCreate_time() {
		return create_time;
	}
	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}
	public Date getLast_login_time() {
		return last_login_time;
	}
	public void setLast_login_time(Date last_login_time) {
		this.last_login_time = last_login_time;
	}
	public String getMobile_phone() {
		return mobile_phone;
	}
	public void setMobile_phone(String mobile_phone) {
		this.mobile_phone = mobile_phone;
	}
	public long getHistory_pay_gold() {
		return history_pay_gold;
	}
	public void setHistory_pay_gold(long history_pay_gold) {
		this.history_pay_gold = history_pay_gold;
	}
	public long getGold() {
		return gold;
	}
	public void setGold(long gold) {
		this.gold = gold;
	}
	public String getClient_ip() {
		return client_ip;
	}
	public void setClient_ip(String client_ip) {
		this.client_ip = client_ip;
	}
	public String getPt() {
		return pt;
	}
	public void setPt(String pt) {
		this.pt = pt;
	}
	public int getBanned() {
		return banned;
	}
	public void setBanned(int banned) {
		this.banned = banned;
	}
	public int getIs_agent() {
		return is_agent;
	}
	public void setIs_agent(int is_agent) {
		this.is_agent = is_agent;
	}
	public int getToday_online() {
		return today_online;
	}
	public void setToday_online(int today_online) {
		this.today_online = today_online;
	}
	public int getHistory_online() {
		return history_online;
	}
	public void setHistory_online(int history_online) {
		this.history_online = history_online;
	}
	public String getLast_client_flag() {
		return last_client_flag;
	}
	public void setLast_client_flag(String last_client_flag) {
		this.last_client_flag = last_client_flag;
	}
	public String getClient_version() {
		return client_version;
	}
	public void setClient_version(String client_version) {
		this.client_version = client_version;
	}
	public long getRecommend_id() {
		return recommend_id;
	}
	public void setRecommend_id(long recommend_id) {
		this.recommend_id = recommend_id;
	}
	public long getMoney() {
		return money;
	}
	public void setMoney(long money) {
		this.money = money;
	}
	public String getClient_ip2() {
		return client_ip2;
	}
	public void setClient_ip2(String client_ip2) {
		this.client_ip2 = client_ip2;
	} 
	

	
	
	
}
