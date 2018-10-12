package com.cai.common.define;

public enum ESellType {// 1购买 2赠送 3测试 4其它

	BUY_CARD(1, "后台购买"), 
	SEND_CARD(2, "后台赠送"),
	TEST_CARD(3, "测试"),
	CENTER_PAY_CARD(4, "公众号充值"),
	OTHER_CARD(5,"其它"),
	GAME_PAY_CARD(6, "游戏内充值");

	private int id;

	private String name; // 中文注释

	private ESellType(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
