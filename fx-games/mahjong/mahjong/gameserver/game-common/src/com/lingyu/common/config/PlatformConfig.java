package com.lingyu.common.config;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Element;
import com.lingyu.common.core.ServiceException;
public abstract class PlatformConfig
{
	private static final Logger logger = LogManager.getLogger(PlatformConfig.class);
	public final static String ATTR_NAME = "name";
	public final static String ATTR_ID = "id";
	public final static String ATTR_EXCHANGE_RATE = "exchangeRate";
	public final static String HOST_NAME = "hostName";
	public final static String CLIENT_URL = "clientUrl";
	public final static String PAY_URL = "payUrl";
	public final static String HOME_URL = "homeUrl";
	public final static String APP_ID = "appId";
	public final static String LOGIN_URI = "loginUri";
	public final static String FORUM_URL = "forumUrl"; // 论坛url
	public final static String TWITTER_URL = "twitterUrl"; // 官方微博url
	public final static String KEY = "key"; // 平台密钥
	
	//平台网址
	protected String hostName;
	/**
	 * 平台ID
	 */
	protected String id;
	/**平台key*/
	private String key;
	/**
	 * 平台名字
	 */
	protected String name;
	/**
	 * 游戏兑换率: 为平台币/游戏币
	 */
	protected float exchangeRate;
	/** 客户端CDN地址 */
	protected String clientUrl;
	/** 购买地址 */
	protected String payUrl;
	/** 主页地址 */
	protected String homeUrl;
	/** APPID */
	protected String appId;

	protected String loginUri;

	protected String forumUrl;

	protected String twitterUrl;
	protected static String globalCDN;

	protected Map<String, String> pf2microclient = new HashMap<>();
	protected Map<String, String> pf2logo = new HashMap<>();
	protected Map<String, String> pf2bg = new HashMap<>();
	
	protected Map<String, String> areaClientUrls = new HashMap<>();
	public void parseFrom(Element element) throws ServiceException
	{
		id = element.attributeValue(ATTR_ID);
		hostName= element.attributeValue(HOST_NAME);
		name = element.attributeValue(ATTR_NAME);
		clientUrl = element.attributeValue(CLIENT_URL);
		payUrl = element.attributeValue(PAY_URL);
		homeUrl = element.attributeValue(HOME_URL);
		appId = element.attributeValue(APP_ID);
		loginUri = element.attributeValue(LOGIN_URI);
		forumUrl = element.attributeValue(FORUM_URL);
		twitterUrl = element.attributeValue(TWITTER_URL);
		key = element.attributeValue(KEY);
		String exchangeRateStr = element.attributeValue(ATTR_EXCHANGE_RATE);
		if (exchangeRateStr != null)
		{
			exchangeRate = Float.parseFloat(exchangeRateStr);
		}
		else
		{
			logger.warn("no {}, {}", ATTR_EXCHANGE_RATE, element.toString());
		}
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public float getExchangeRate()
	{
		return exchangeRate;
	}
	public void setExchangeRate(float exchangeRate)
	{
		this.exchangeRate = exchangeRate;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getClientUrl(int areaId) {
		String ret = areaClientUrls.get(String.valueOf(areaId));
		if(StringUtils.isEmpty(ret)){
			ret = clientUrl;
		}
		if(StringUtils.isEmpty(ret)){
			ret=globalCDN;
		}
		return ret;
	}

	public static void setGlobalCDN(String globalCDN) {
		PlatformConfig.globalCDN = globalCDN;
	}
	public void setClientUrl(String clientUrl) {
		this.clientUrl = clientUrl;
	}

	public String getPayUrl() {
		return payUrl;
	}

	public void setPayUrl(String payUrl) {
		this.payUrl = payUrl;
	}

	public String getHomeUrl() {
		return homeUrl;
	}

	public void setHomeUrl(String homeUrl) {
		this.homeUrl = homeUrl;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getLoginUri() {
		return loginUri;
	}

	public String getForumUrl() {
		return forumUrl;
	}

	public void setForumUrl(String forumUrl) {
		this.forumUrl = forumUrl;
	}

	public String getTwitterUrl() {
		return twitterUrl;
	}

	public void setTwitterUrl(String twitterUrl) {
		this.twitterUrl = twitterUrl;
	}

	public void putMircoClient(String pf, String url) {
		pf2microclient.put(pf, url);
	}

	public void setPf2microclient(Map<String, String> pf2microclient) {
		this.pf2microclient = pf2microclient;
	}

	public String getMicroClient(String pf) {
		// union-10055-1*union-10029-2
		return pf2microclient.get(pfFilter(pf));
	}
	
	public void setPf2logo(Map<String, String> pf2logo) {
		this.pf2logo = pf2logo;
	}
	
	public String getLogoUrl(String pf) {
		// union-10055-1*union-10029-2
		return pf2logo.get(pfFilter(pf));
	}
	public void putLogoUrl(String pf, String url) {
		pf2logo.put(pf, url);
	}
	
	public void setPf2bg(Map<String, String> pf2bg) {
		this.pf2bg = pf2bg;
	}
	
	public String getBgUrl(String pf) {
		// union-10055-1*union-10029-2
		return pf2bg.get(pfFilter(pf));
	}
	public void putBgUrl(String pf, String url) {
		pf2bg.put(pf, url);
	}
	
	
	public Map<String, String> getAreaClientUrls() {
		return areaClientUrls;
	}

	public void setAreaClientUrls(Map<String, String> areaClientUrls) {
		this.areaClientUrls = areaClientUrls;
	}

//	public String getclientUrl() {
//		return clientUrl;
//	}
	
	
	public String pfFilter(String pf){
		if (pf.indexOf("union") >= 0) {
			pf = StringUtils.substringBetween(pf, "*", "-");
		}
		return pf;
	}
	
	
}
