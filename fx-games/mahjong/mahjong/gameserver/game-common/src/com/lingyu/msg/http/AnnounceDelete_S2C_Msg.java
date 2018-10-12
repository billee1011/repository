package com.lingyu.msg.http;

public class AnnounceDelete_S2C_Msg extends HttpMsg{
	private int retCode;
	private int announceId;
	
	public int getRetCode() {
		return retCode;
	}
	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public int getAnnounceId() {
		return announceId;
	}

	public void setAnnounceId(int announceId) {
		this.announceId = announceId;
	}
}
