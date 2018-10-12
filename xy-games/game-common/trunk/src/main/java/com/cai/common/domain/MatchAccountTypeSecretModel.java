package com.cai.common.domain;

public class MatchAccountTypeSecretModel extends DBBaseModel {

	/**
	 */
	private static final long serialVersionUID = 1L;
	
	private String secret_code;
	private int account_type;
	private int state;
	
	public String getSecret_code() {
		return secret_code;
	}
	public void setSecret_code(String secret_code) {
		this.secret_code = secret_code;
	}
	public int getAccount_type() {
		return account_type;
	}
	public void setAccount_type(int account_type) {
		this.account_type = account_type;
	}
	public int getState() {
		return state;
	}
	public void setState(int state) {
		this.state = state;
	}
}
