package com.lingyu.msg.http;

import java.util.List;

public class Redeem_C2S_Msg implements ISerialaIdable{
	private String mailTitle;
	private String mailContent;
	private int selectRoleType;
	private List<NewRedeemRoleDTO> redeemRoles;
	private int money;
	private int bindDiamond;
	private int diamond;
	private List<RedeemItemDTO> redeemItems;

	private int serialId;

	public String getMailTitle() {
		return mailTitle;
	}

	public void setMailTitle(String mailTitle) {
		this.mailTitle = mailTitle;
	}

	public String getMailContent() {
		return mailContent;
	}

	public void setMailContent(String mailContent) {
		this.mailContent = mailContent;
	}

	public int getSelectRoleType() {
		return selectRoleType;
	}

	public void setSelectRoleType(int selectRoleType) {
		this.selectRoleType = selectRoleType;
	}

	public List<NewRedeemRoleDTO> getRedeemRoles() {
		return redeemRoles;
	}

	public void setRedeemRoles(List<NewRedeemRoleDTO> redeemRoles) {
		this.redeemRoles = redeemRoles;
	}

	public int getMoney() {
		return money;
	}

	public void setMoney(int money) {
		this.money = money;
	}

	public int getDiamond() {
		return diamond;
	}

	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}

	public List<RedeemItemDTO> getRedeemItems() {
		return redeemItems;
	}

	public void setRedeemItems(List<RedeemItemDTO> redeemItems) {
		this.redeemItems = redeemItems;
	}

	public int getBindDiamond() {
		return bindDiamond;
	}

	public void setBindDiamond(int bindDiamond) {
		this.bindDiamond = bindDiamond;
	}

	public int getSerialId() {
		return serialId;
	}

	public void setSerialId(int serialId) {
		this.serialId = serialId;
	}

}
