package com.lingyu.common.io;

public class GameMsg {

	private int id;
	private int type;
	private int length;
	private Object[] content;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public Object[] getContent() {
		return content;
	}

	public void setContent(Object[] content) {
		this.content = content;
	}
}
