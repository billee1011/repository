package com.lingyu.noark.data.entity;

import java.util.Date;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Json;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;
import com.lingyu.noark.data.annotation.Temporal.TemporalType;

@Entity
@Table(name = "buy_log_{addTime#yyyyMMdd}")
public class BuyLog {
	@Id
	@Column(name = "id", comment = "道具Id")
	private long id;

	@IsRoleId
	@Column(name = "role_id", nullable = true, comment = "角色Id",order=1)
	private long roleId;

	@Column(name = "template_id", nullable = true, comment = "模板Id")
	private int templateId;

	@Column(name = "name", length = 36, comment = "道具名称",order=2)
	private String name;

	@Column(name = "bind", nullable = false, comment = "是否绑定")
	private boolean bind;

	@Json()
	@Column(name = "attribute", comment = "道具属性")
	private Attribute attribute;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "add_time", nullable = true, comment = "创建时间")
	private Date addTime;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public long getRoleId() {
		return roleId;
	}

	public void setRoleId(long roleId) {
		this.roleId = roleId;
	}

	public int getTemplateId() {
		return templateId;
	}

	public void setTemplateId(int templateId) {
		this.templateId = templateId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isBind() {
		return bind;
	}

	public void setBind(boolean bind) {
		this.bind = bind;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}
}