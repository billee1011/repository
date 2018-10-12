package com.lingyu.admin.core;

public class Code {
	private String id;
	private String message="OK";

	public Code(String code) {
		this.id = code;
		// this.message = SessionUtil.getI18nMessage(code);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
