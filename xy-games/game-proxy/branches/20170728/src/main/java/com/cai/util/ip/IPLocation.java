package com.cai.util.ip;

/**
 * 用来封装ip相关信息，目前只有两个字段，ip所在的国家和地区
 * @author run
 * @date 2013-5-27
 */
public class IPLocation {
	public String country;
	public String area;

	public IPLocation() {
		country = area = "";
	}

	public IPLocation getCopy() {
		IPLocation ret = new IPLocation();
		ret.country = country;
		ret.area = area;
		return ret;
	}
}
