package com.lingyu.common.entity;

import java.util.Date;

import com.lingyu.common.constant.TimeConstant;
import com.lingyu.noark.data.annotation.Column;
import com.lingyu.noark.data.annotation.Entity;
import com.lingyu.noark.data.annotation.Id;
import com.lingyu.noark.data.annotation.Table;
import com.lingyu.noark.data.annotation.Temporal;
/**
 * 版本公告 or 官方公告
 * @author wangning
 * @date 2017年3月3日 下午9:36:51
 */
@Entity
@Table(name = "version_notice")
public class VersionNotice{
	
	@Id
	@Column(name = "id", nullable = false, defaultValue = "0", comment = "公告类型 1=版本 2=官方")
	private int id;
	
	@Column(name = "content", length = 1024, comment = "内容")
	private String content;
	
	@Column(name = "version", length = 64, comment = "版本号")
	private String version;
	
	@Temporal
	@Column(name = "add_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "创建时间")
	private Date addTime = TimeConstant.DATE_LONG_AGO;
	
	@Temporal
	@Column(name = "modify_time", nullable = false, defaultValue = "2000-01-01 00:00:00", comment = "修改时间")
	private Date modifyTime = TimeConstant.DATE_LONG_AGO;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
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
}
