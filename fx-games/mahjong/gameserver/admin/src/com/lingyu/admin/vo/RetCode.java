package com.lingyu.admin.vo;

public enum RetCode {
	SUCCESS(1, "成功"),
	ACCOUNT_ACTIVED(100, "账号重复激活"),
	
	ASYN_SUCCESS(1001, "异步操作，详情看日志记录"),
	;
	private final int code;
	private final String desc;
	RetCode(int code, String desc){
		this.code = code;
		this.desc = desc;
	}
	public int getCode() {
		return code;
	}
	public String getDesc() {
		return desc;
	}
}
