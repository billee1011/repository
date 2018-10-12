package com.lingyu.msg.http;

import java.util.Date;

public class VersionNotic_C2S_Msg{
	private int type; // 公告类型 1=版本公告 2=官方公告
	private String content;
	private String version; // 版本
	private Date time;
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
}
