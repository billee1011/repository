package com.cai.common.domain;

//组合子项
public class WeaveItem {
	/**
	 * 组合类型--吃碰杆
	 */
	public int weave_kind;
	/**
	 * 中心扑克--吃 碰 杆 啥牌
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
	/**
	 * 胡息
	 */
	public int hu_xi;
	/**
	 * 落起组合数量
	 */
	public int lou_qi_count;
	/**
	 * 落起组合数量
	 */
	public int lou_qi_weave[][];
	
	/**
	 * 组合牌
	 */
	public int weave_card[];
	
	public  WeaveItem(){
		lou_qi_count = 0;
		lou_qi_weave = new int[50][2];
		weave_card = new int[4];
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
	public int getHu_xi() {
		return hu_xi;
	}

	public void setHu_xi(int hu_xi) {
		this.hu_xi = hu_xi;
	}
	public int getLou_qi_weave() {
		return lou_qi_count;
	}

	public void setLou_qi_weave(int lou_qi_count) {
		this.lou_qi_count = lou_qi_count;
	}
	
	
}
