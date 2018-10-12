package com.lingyu.common.config;

import org.dom4j.Element;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.util.TimeUtil;

public class _37PlatformConfig extends PlatformConfig {
	/** 防沉迷 */
	public static final String KEY_ADULT = "adult";
	/** 平台vip */
	public static final String KEY_PTVIP = "ptVip";
	/** 是否是微端 */
	public static final String KEY_CLIENT = "client";
	/** 是否是广告用户  1是，0 不是*/
	public static final String KEY_FROM_AD = "from_ad";
	public static final String PF = "pf";
	/** 第三方参数keys */
	public static final String[] THIRD_PARTY_KEYS = {KEY_ADULT, KEY_PTVIP, KEY_CLIENT ,KEY_FROM_AD,PF};
	public final static String URL_USER_INFO = "/getuser?sessionid=";
	/** 微端的值 */
	public static final String VALUE_CLIENT = "2";
	/**
	 * 37平台key值
	 */
	public static final String KEY = "7964f8dddeb95fc5";
	
	public static final String GAME_KEY="sanda";
	
	

	/**
	 * 时间差过长
	 */
	public static final long TimeToLong = 3 * TimeUtil.MINUTE;
	private String key;
	private String phoneurl;
	private String chatUrl="?game_key={0}&server_id={1}&time={2}&login_account={3}&actor={4}&actor_id={5}&content={6}&ip={7}&sign={8}";

	public final static Object[][] PFS_37 = {
			// 37
			{ "37", 1, "37" }, };

	@Override
	public void parseFrom(Element element) throws ServiceException {
		super.parseFrom(element);
		key = element.attributeValue("key");
		phoneurl = element.attributeValue("phoneurl");
		chatUrl = element.attributeValue("chatUrl")+chatUrl;
	}

	public String getKey() {
		return key;
	}

	public String getPhoneurl() {
		return phoneurl;
	}

	public String getChatUrl() {
		return chatUrl;
	}

}
