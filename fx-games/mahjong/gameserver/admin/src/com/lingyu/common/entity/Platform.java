package com.lingyu.common.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.format.annotation.DateTimeFormat;

import com.lingyu.admin.vo.PlatformVo;

@Entity
@Table(name = "platform")
public class Platform {
	@Id
	@Column(name = "id", unique = true, nullable = false)
	private String id;

	/**
	 * 用户名
	 */
	@Column(name = "name")
	private String name;
	/**
	 * 描述
	 */
	@Column(name = "description")
	private String description;
	/**
	 * 创建时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "add_time")
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date addTime;
	/**
	 * 上次修改时间
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "modify_time")
	@DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
	private Date modifyTime;
	
	/**
	 * 汇率
	 */
	@Column(name = "exchange_rate")
	private float exchangeRate;
	
	/**
	 * 域名
	 */
	@Column(name = "domain")
	private String domain;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public float getExchangeRate() {
		return exchangeRate;
	}

	public void setExchangeRate(float exchangeRate) {
		this.exchangeRate = exchangeRate;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	public PlatformVo toVO(){
		PlatformVo ret=new PlatformVo();
		ret.setId(id);
		ret.setName(name);
		return ret;
	}
}
