package com.lingyu.noark.data.entity;

import java.io.Serializable;

public class Attribute implements Serializable, Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9088653136311859150L;
	private int id;
	private String name = "123456";
	private long item = 123456789L;

	public Attribute() {

	}

	public Attribute(int id) {
		this.id = id;
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

	public long getItem() {
		return item;
	}

	public void setItem(long item) {
		this.item = item;
	}

	@Override
	public Attribute clone() throws CloneNotSupportedException {
		Attribute attribute = new Attribute();
		attribute.setId(id);
		attribute.setItem(item);
		attribute.setName(name);
		return attribute;
	}
}
