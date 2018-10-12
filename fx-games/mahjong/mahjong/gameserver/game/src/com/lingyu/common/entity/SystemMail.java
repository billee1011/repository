package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.GeneratedValue;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.Table;

@Entity
@Table(name = "system_mail")
public class SystemMail {

	@Id
	@GeneratedValue
	@Column(name = "id")
	private int id;
	@Column(name = "title")
	private String title;
	@Column(name = "content")
	private String content;
	@Column(name = "attachment", length = 65535, comment = "附件")
	private String attachment;
	@Column(name = "coin")
	private int coin;
	@Column(name = "diamond")
	private int diamond;
	@Column(name = "add_time")
	private Date addTime;
	@Column(name = "modify_time")
	private Date modifyTime;
	@Column(name = "currency_opt_type", nullable = false, defaultValue = "1091")//OperateType.MAIL_GET
	private int currencyOptType;
	@Column(name = "serial_id", nullable = false, defaultValue = "0", comment = "序列号")
	private long serialId;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}

	public int getCoin() {
		return coin;
	}

	public void setCoin(int coin) {
		this.coin = coin;
	}

	public int getDiamond() {
		return diamond;
	}

	public void setDiamond(int diamond) {
		this.diamond = diamond;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}

	public int getCurrencyOptType() {
		return currencyOptType;
	}

	public void setCurrencyOptType(int currencyOptType) {
		this.currencyOptType = currencyOptType;
	}

	public long getSerialId() {
		return serialId;
	}

	public void setSerialId(long serialId) {
		this.serialId = serialId;
	}
}