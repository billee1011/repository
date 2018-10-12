package com.lingyu.admin.vo;

public class SimpleGameAreaVo {
	private int id;
	private String name;
	private int success;   
	private boolean valid;

	public SimpleGameAreaVo() {
	}

	public SimpleGameAreaVo(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSuccess() {
		return success;
	}

	public void setSuccess(int success) {
		this.success = success;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

}
