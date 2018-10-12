package com.cai.common.domain;

//组合子项
public class WeaveItem {
	/**
	 * 组合类型--吃碰杆
	 */
	public int weave_kind;
	/**
	 * 中心扑克
	 */
	public int center_card;
	/**
	 * 公开标志
	 */
	public int public_card;
	/**
	 * 供应用户
	 */
	public int provide_player;

	public WeaveItem(){
		
	}

	public int getWeave_kind() {
		return weave_kind;
	}

	public void setWeave_kind(int weave_kind) {
		this.weave_kind = weave_kind;
	}

	public int getCenter_card() {
		return center_card;
	}

	public void setCenter_card(int center_card) {
		this.center_card = center_card;
	}

	public int getPublic_card() {
		return public_card;
	}

	public void setPublic_card(int public_card) {
		this.public_card = public_card;
	}

	public int getProvide_player() {
		return provide_player;
	}

	public void setProvide_player(int provide_player) {
		this.provide_player = provide_player;
	}
}
