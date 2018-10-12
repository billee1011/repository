package com.lingyu.noark.data.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.IsRoleId;
import com.lingyu.noark.data.annotation.Json;
import com.lingyu.noark.data.annotation.Json.JsonStyle;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;
import com.lingyu.noark.data.annotation.Temporal.TemporalType;

@Entity
@Table(name = "item2", comment = "道具表")
public class Item extends ServerObject implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4233664897575554325L;

	@IsRoleId
	@Column(name = "role_id", nullable = true, comment = "角色Id")
	private long roleId;

	@Column(name = "template_id", nullable = true, comment = "模板Id")
	private int templateId;

	@Column(name = "name", length = 36, comment = "道具名称")
	private String name;

	@Column(name = "bind", nullable = false, comment = "是否绑定", defaultValue = "true")
	private boolean bind;

	@Json()
	@Column(name = "attribute", comment = "道具属性", defaultValue = "{}")
	private Attribute attribute;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "add_time", nullable = true, comment = "创建时间", defaultValue = "2012-11-11 00:00:00")
	private Date addTime;

	@Column(name = "money", nullable = false, precision = 10, scale = 1, defaultValue = "5.26")
	private float money;

	@Json(style = JsonStyle.WriteClassName)
	@Column(name = "money1", nullable = false, precision = 10, scale = 2, defaultValue = "5.2600001")
	private double money1;

	@Column(name = "name1", length = 128, comment = "道具名称", defaultValue = "jqka")
	private String name1;
	@Column(name = "name2", length = 65535, comment = "道具名称")
	private String name2;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "long_time", comment = "一个Long类型的时间测试")
	private long time = System.currentTimeMillis();

	@Column(name = "intx", defaultValue = "0")
	private AtomicInteger intx;

	public AtomicInteger getIntx() {
		return intx;
	}

	public void setIntx(AtomicInteger intx) {
		this.intx = intx;
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

	public Date getAddTime() {
		return addTime;
	}

	public void setAddTime(Date addTime) {
		this.addTime = addTime;
	}

	public Attribute getAttribute() {
		return attribute;
	}

	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	public boolean isBind() {
		return bind;
	}

	public void setBind(boolean bind) {
		this.bind = bind;
	}

	@Override
	public Item clone() throws CloneNotSupportedException {
		Item ix = new Item();
		ix.setId(this.getId());
		ix.setName(this.getName());
		ix.setTemplateId(this.getTemplateId());
		ix.setRoleId(this.getRoleId());
		ix.setBind(this.isBind());
		ix.setAddTime(this.getAddTime());
		ix.setAttribute(attribute.clone());
		return ix;
	}

	@Override
	public String toString() {
		return "Item[id=" + id + "]";
	}

	public float getMoney() {
		return money;
	}

	public void setMoney(float money) {
		this.money = money;
	}

	public double getMoney1() {
		return money1;
	}

	public void setMoney1(double money1) {
		this.money1 = money1;
	}

	public String getName1() {
		return name1;
	}

	public void setName1(String name1) {
		this.name1 = name1;
	}

	public String getName2() {
		return name2;
	}

	public void setName2(String name2) {
		this.name2 = name2;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}
}