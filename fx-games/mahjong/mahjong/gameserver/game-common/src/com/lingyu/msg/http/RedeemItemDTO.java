package com.lingyu.msg.http;

public class RedeemItemDTO {
	private String itemId;
	private String itemName;
	private int count;
	private int bind;
	private String extendShuxing;
	public String getItemId() {
		return itemId;
	}
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public int getBind() {
		return bind;
	}
	public void setBind(int bind) {
		this.bind = bind;
	}
	public String getExtendShuxing() {
		return extendShuxing;
	}
	public void setExtendShuxing(String extendShuxing) {
		this.extendShuxing = extendShuxing;
	}
	
}
