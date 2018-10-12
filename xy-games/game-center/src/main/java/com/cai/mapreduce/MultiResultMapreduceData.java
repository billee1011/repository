package com.cai.mapreduce;

/**
 * 多个结果集的Mapreduce返回
 * 需要将value JSON化取值
 * @author chansonyan
 * 2018年6月19日
 */
public class MultiResultMapreduceData {
	
	private int id;
	private String value;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
}
