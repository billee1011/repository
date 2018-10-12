package com.cai.common.domain;

import java.util.Date;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;

@Document(collection = "add_card_log")
@CompoundIndexes({ @CompoundIndex(name = "index_accountId", def = "{'accountId': 0}"),
		@CompoundIndex(name = "index_nickname", def = "{'nickname': 0}"),
		@CompoundIndex(name = "index_shopId", def = "{'shopId': 0}"),
		@CompoundIndex(name = "index_sellType", def = "{'sellType': 0}"),
		@CompoundIndex(name = "index_cashAccountName", def = "{'cashAccountName': 0}"),
		@CompoundIndex(name = "index_ossName", def = "{'ossName': 0}"),
		@CompoundIndex(name = "index_accountType", def = "{'accountType': 0}"),
		@CompoundIndex(name = "index_sellType", def = "{'sellType': 0}") })
// 增加房卡记录表
public class AddCardLog {
	/**
	 * 订单流水号
	 */
	private String orderID;
	/**
	 * 玩家帐号ID
	 */
	@Indexed
	private Long accountId;
	/**
	 * 微信昵称
	 */
	private String nickname;
	/**
	 * 帐号类型(1--代理 0-普通)
	 */
	private int accountType;

	/**
	 * 1购买 2赠送 3测试 4其它
	 */
	private Integer sellType;

	/**
	 * 商品ID >0 商品ID -1非购买
	 */
	@Indexed
	private Integer shopId;

	/**
	 * 购买的房卡数
	 */
	private Integer cardNum;

	/**
	 * 购买赠送的房卡数
	 */
	private Integer sendNum;
	/**
	 * 花费的rmb
	 */
	private Integer rmb;
	/**
	 * 收款帐号ID
	 */
	private Integer cashAccountID;

	/**
	 * 收款帐号名
	 */
	@Indexed
	private String cashAccountName;
	/**
	 * 备注
	 */
	private String remark;
	/**
	 * 订单创建时间
	 */
	private Date create_time;

	/**
	 * 后台管理员ID
	 */
	private String ossID;

	/**
	 * 后台管理员名字
	 */
	private String ossName;

	/**
	 * 第三方平台订单号
	 */
	@Indexed
	private String centerOrderID;

	/**
	 * 订单状态  -针对本游戏客户端充值才有 0-成功 1-临时 2-异常
	 */
	private int orderStatus;
	/**
	 * 订单真正完成时间  ---针对本游戏客户端充值才有
	 */
	private Date finishDate;
	
	private String orderSoures;

	public String getCenterOrderID() {
		return centerOrderID;
	}

	public void setCenterOrderID(String centerOrderID) {
		this.centerOrderID = centerOrderID;
	}

	public String getOrderID() {
		return orderID;
	}

	public void setOrderID(String orderID) {
		this.orderID = orderID;
	}

	public int getAccountType() {
		return accountType;
	}

	public void setAccountType(int accountType) {
		this.accountType = accountType;
	}

	public Long getAccountId() {
		return accountId;
	}

	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	public Integer getSellType() {
		return sellType;
	}

	public void setSellType(Integer sellType) {
		this.sellType = sellType;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public Integer getShopId() {
		return shopId;
	}

	public void setShopId(Integer shopId) {
		this.shopId = shopId;
	}

	public Integer getCardNum() {
		return cardNum;
	}

	public void setCardNum(Integer cardNum) {
		this.cardNum = cardNum;
	}

	public Integer getSendNum() {
		return sendNum;
	}

	public void setSendNum(Integer sendNum) {
		this.sendNum = sendNum;
	}

	public Integer getRmb() {
		return rmb;
	}

	public void setRmb(Integer rmb) {
		this.rmb = rmb;
	}

	public Integer getCashAccountID() {
		return cashAccountID;
	}

	public void setCashAccountID(Integer cashAccountID) {
		this.cashAccountID = cashAccountID;
	}

	public String getCashAccountName() {
		return cashAccountName;
	}

	public void setCashAccountName(String accountName) {
		this.cashAccountName = accountName;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Date getCreate_time() {
		return create_time;
	}

	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}

	public String getOssID() {
		return ossID;
	}

	public Date getFinishDate() {
		return finishDate;
	}

	public void setFinishDate(Date finishDate) {
		this.finishDate = finishDate;
	}

	public int getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(int orderStatus) {
		this.orderStatus = orderStatus;
	}

	public void setOssID(String ossID) {
		this.ossID = ossID;
	}

	public String getOssName() {
		return ossName;
	}

	public void setOssName(String ossName) {
		this.ossName = ossName;
	}
	
	public String getOrderSoures() {
		return orderSoures;
	}


	public void setOrderSource(String notityXml) {
		this.orderSoures=notityXml;
		
	}
}
