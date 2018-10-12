package com.cai.http.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 验证支付回调之后 需要返回给微信的-- 商户处理后同步返回给微信参数
 */
@XStreamAlias("xml")
public class ResponseData {
	//SUCCESS/FAIL  SUCCESS表示商户接收通知成功并校验成功
	private String return_code;
	//返回信息，如非空，为错误原因：签名失败  参数格式校验错误
	private String return_msg;

	public String getReturn_code() {
		return return_code;
	}

	public void setReturn_code(String return_code) {
		this.return_code = return_code;
	}

	public String getReturn_msg() {
		return return_msg;
	}

	public void setReturn_msg(String return_msg) {
		this.return_msg = return_msg;
	}
}
