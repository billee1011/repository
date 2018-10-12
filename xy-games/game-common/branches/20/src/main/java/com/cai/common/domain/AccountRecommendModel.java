package com.cai.common.domain;

import java.io.Serializable;
import java.util.Date;

public class AccountRecommendModel implements Serializable {

	private int id;
	/**
	 * 玩家id
	 */
	private long account_id;
	/**
	 * 目标玩家
	 */
	private long target_account_id;
	/**
	 * 创建时间
	 */
	private Date create_time;

	/**
	 * 获得的金币数
	 */
	private int gold_num;
	
	//扩展
	/**
	 * 目标账号名字
	 */
	private String target_name;
	
	/**
	 * 目标账号icon
	 */
	private String target_icon;
	
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getAccount_id() {
		return account_id;
	}

	public void setAccount_id(long account_id) {
		this.account_id = account_id;
	}

	public long getTarget_account_id() {
		return target_account_id;
	}

	public void setTarget_account_id(long target_account_id) {
		this.target_account_id = target_account_id;
	}

	public Date getCreate_time() {
		return create_time;
	}

	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}

	public int getGold_num() {
		return gold_num;
	}

	public void setGold_num(int gold_num) {
		this.gold_num = gold_num;
	}

	public String getTarget_name() {
		return target_name;
	}

	public void setTarget_name(String target_name) {
		this.target_name = target_name;
	}

	public String getTarget_icon() {
		return target_icon;
	}

	public void setTarget_icon(String target_icon) {
		this.target_icon = target_icon;
	}
	
	

}
