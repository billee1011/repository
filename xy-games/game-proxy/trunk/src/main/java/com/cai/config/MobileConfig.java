/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.config;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月12日 下午4:27:04 <br/>
 */
public class MobileConfig {

	private static MobileConfig cfg = new MobileConfig();

	/**
	 * 是否开启手机验证码功能，防止临时需要关闭
	 */
	private volatile boolean openIdentifyCode = true;

	/**
	 * 登陆校验码存活时间
	 */
	private volatile int loginCodeAalive;

	public static MobileConfig get() {
		return cfg;
	}

	public boolean isOpenIdentifyCode() {
		return openIdentifyCode;
	}

	public MobileConfig setOpenIdentifyCode(boolean openIdentifyCode) {
		this.openIdentifyCode = openIdentifyCode;
		return this;
	}

	public int getLoginCodeAalive() {
		return loginCodeAalive;
	}

	public MobileConfig setLoginCodeAalive(int loginCodeAalive) {
		this.loginCodeAalive = loginCodeAalive;
		return this;
	}

	@Override
	public String toString() {
		return "MobileConfig [openIdentifyCode=" + openIdentifyCode + ", loginCodeAalive=" + loginCodeAalive + "]";
	}

}
