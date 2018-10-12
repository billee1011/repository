package com.lingyu.msg.http;

public class MaintainServer_S2C_Msg extends HttpMsg{
	private int retCode;
	private int status;

	public int getRetCode() {
		return retCode;
	}

	public void setRetCode(int retCode) {
		this.retCode = retCode;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}
