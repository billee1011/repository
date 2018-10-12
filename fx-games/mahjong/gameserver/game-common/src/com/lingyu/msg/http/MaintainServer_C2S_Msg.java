package com.lingyu.msg.http;

public class MaintainServer_C2S_Msg {

	private int status;// 0 正常状态 1维护状态
	private String reason;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

}
