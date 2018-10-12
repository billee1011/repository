package com.lingyu.common.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import com.lingyu.common.core.ServiceException;
import com.lingyu.common.util.XMLUtil;

public class TencentPlatformConfig extends PlatformConfig {

	// 参数key
	public static final String APPID = "appid";
	public static final String OPENID = "openid";
	public static final String OPENKEY = "openkey";

	public static final String SIG = "sig";
	public static final String PF = "pf";
	public static final String PFKEY = "pfkey";
	public static final String SEQID = "seqid";
	public static final String IOPENID = "iopenid";
	public static final String INVKEY = "invkey";
	public static final String ITIME = "itime";
	public static final String TOKEY_TYPE = "tokentype";
	public static final String APP_CONTRACT_ID = "appContractId";
	public static final String VIA = "via";

	public static final String MICRO_CLIENT_PID = "microClientPid";
	private final static String TGW_URL = "tgwUrl";
	private final static String URL_3366 = "url3366";
	// 登入传入的额外参数
	public static final String[] EXTRA_PARAMS = { OPENKEY, PF, PFKEY, SEQID, IOPENID, INVKEY, ITIME, APP_CONTRACT_ID, VIA };

	/** 蓝钻会员 */
	public static final String BLUE_VIP = "blue_vip";
	/** qq会员 */
	public static final String MEMBER_VIP = "member_vip";

	/** 联盟区前缀 */
	public static final String PF_UNION_PREFIX = "union";
	public static final String PF_SPLIT_STAR = "*";

	/**
	 * fopenids 必须 string 需要获取数据的openid列表，中间以_隔开，每次最多100个。
	 * 注意如果使用GET方式传100个openid是超过2048字节限制的，所以如果需要传100个openid请使用POST方法。
	 */
	public static final String FRIENDS_OPENIDS = "fopenids";

	/** not login */
	public static final int CODE_NOT_LOGIN = 1002;
	/** 返回码成功 */
	public static final int CODE_SUCCESS = 0;
	/** 存在感 */
	public static final int EXISTS = 1;
	/** 2015：用户已经参加过这个任务了 */
	public static final int CODE_HAS_JOINED = 2015;

	// 蓝钻信息
	/** 是否为蓝钻用户（0：不是； 1：是） */
	public static final int IS_BLUE_VIP_BIT = 16;
	/** 是否是年费蓝钻用户（0：不是； 1：是） */
	public static final int IS_YEAR_BLUE_VIP_BIT = 17;
	/** 是否豪华版蓝钻（0：不是； 1：是） */
	public static final int IS_HIGH_BLUE_VIP_BIT = 18;

	// 黄钻信息

	// 黄钻信息
	/** 一无所有的屌丝 */
	public static final int POOR_GUY = 0;
	/** 是否为黄钻用户（0：不是； 1：是） */
	public static final int IS_YELLOW_VIP_BIT = 16;
	/** 是否为年费黄钻用户（0：不是； 1：是） */
	public static final int IS_YEAR_YELLOW_VIP_BIT = 17;
	/** 是否为豪华版黄钻用户（0：不是； 1：是） */
	public static final int IS_YELLOW_HIGH_VIP_BIT = 18;
	/**
	 * 用户的付费类型: {@value}<BR>
	 * <li>0：非预付费用户（先开通业务后付费，一般指通过手机开通黄钻的用户）</li>
	 * <li>1：预付费用户（先付费后开通业务，一般指通过Q币Q点、财付通或网银付费开通黄钻的用户）</li>
	 */
	public static final int YELLO_VIP_PAY_WAY = 19;
	/** 获取level使用的mask */
	public static final int LEVEL_MASK = 0xFFFF;

	/**
	 * token类型-礼包赠送类型
	 */
	public static final int TOKEY_TYPE_DONATE = 1;

	/**
	 * 开平api版本
	 */
	public static final String OPEN_API_VERSION = "v3";

	/** qq会员--无 */
	public static final int QQ_MEMBER_NONE = 0;
	/** qq会员--普通 */
	public static final int QQ_MEMBER_NORMAL = 1;
	/** qq会员--年费 */
	public static final int QQ_MEMBER_YEAR = 2;
	/** qq会员--超级会员 */
	public static final int QQ_MEMBER_SUPER = 3;
	/** qq会员--年费超级会员 */
	public static final int QQ_MEMBER_SUPER_YEAR = 4;

	/** 单位:秒 */
	public static final int CONFIRM_DELAY_INERVAL = 10;
	/** 可以使用保存桌面的渠道类型 */
	public static String[] SAVE_DESK_PFS = { "website", "qzone" };
	/** 可以使用下载微端的渠道类型 */
	public static String[] CAN_DOWNLOAD_MICRO_CLIENT = { "website", "qqgame", "union" };
	/** 可以可以领取微端的渠道类型 */
	public static String[] RECEIVE_MICRO_CLIENT = { "website", "qqgame", "union" };
	/************************ QQ权限 ***********************/
	/** 黄钻 */
	public static final int PRIVILEGE_TYPE_YELLOW = 1;
	/** 蓝钻 */
	public static final int PRIVILEGE_TYPE_BLUE = 2;
	/** TGP */
	public static final int PRIVILEGE_TYPE_TGP = 3;
	/** qq会员 */
	public static final int PRIVILEGE_TYPE_QQ_MEMBER = 4;

	/** 豪华版 */
	public static final int PRIVILEGE_SPECIAL_TYPE_HIGH = 1;
	/** 是豪华 */
	public static final int IS_PRIVILEGE_SPECIAL_TYPE_HIGH = 1;
	/** 不是豪华 */
	public static final int ISNOT_PRIVILEGE_SPECIAL_TYPE_HIGH = 0;
	/** 年费版 */
	public static final int PRIVILEGE_SPECIAL_TYPE_YEAR = 2;
	/** 是年费 */
	public static final int IS_PRIVILEGE_SPECIAL_TYPE_YEAR = 1;
	/** 不是年费 */
	public static final int ISNOT_PRIVILEGE_SPECIAL_TYPE_YEAR = 0;

	/** 渠道礼包-每日礼包 */
	public static final int CHANNEL_AWARD_TYPE_DAY = 0;
	/** 渠道礼包-累登礼包 */
	public static final int CHANNEL_AWARD_TYPE_CONTINUE = 1;

	/** 渠道礼包-每日礼包模板id */
	public static final int CHANNEL_DAY_TEMPLATE_ID = 0;
	/************************ QQ权限 ***********************/

	public final static String APP_ID = "appId";
	private final static String APP_KEY = "appKey";
	private final static String APP_NAME = "appName";
	private final static String RES_URL = "resUrl";
	// private final static String LOG_UP_URL = "logUpUrl";
	private final static String LOG_UP_ENABLE = "logUpEnable";
	private final static String REPORT_ENABLE = "reportEnable";

	/** 蛋疼的游戏联盟pf分隔符 */
	public final static String PF_SPLIT = "-";
	public final static String PF_STAR_SPLIT = "*";
	/**
	 * 腾讯平台pf对应的int值 {{pf_report,"qzone"},1}. {{pf_report,"pengyou"},2}.
	 * {{pf_report,"tapp"},3}. {{pf_report,"qqgame"},10}.
	 * {{pf_report,"3366"},11}. {{pf_report,"website"},12}.
	 * {{pf_report,"union"},17}.
	 */
	public final static Object[][] PFS = {
			// QQ空间
			{ "qzone", 1, "QQ空间" },
			// 腾讯朋友
			{ "pengyou", 2, "腾讯朋友" },
			// 腾讯微博
			{ "tapp", 3, "腾讯微博" },
			// 腾讯QPlus
			{ "qplus", 4, "腾讯QPlus" },
			// qqgame (10,'QQ游戏')
			{ "qqgame", 10, "QQ游戏" },
			// 3366 (11,'3366')
			{ "3366", 11, "3366" },
			// website (12,'游戏官网')
			{ "website", 12, "游戏官网" },
			// (16,'游戏人生')
			{ "igame", 16, "游戏人生" },
			// union (17,'腾讯游戏联盟')
			{ "union", 17, "腾讯游戏联盟" },
			// box (23,'腾讯游戏盒子')
			{ "box", 23, "腾讯游戏盒子" }, 
			//空间应用中心
			{ "myapp_pc", 24, "空间应用中心" }, 
			//QQ浏览器
			{ "open_pc_game_browser", 25, "QQ浏览器" },
			// 心悦平台
			{ "xinyue", 888, "心悦" },
			/** 官网微端 */
			{ "MicroClient_1", 100, "官网微端" },
			/** 大厅微端 */
			{ "MicroClient_2", 101, "大厅微端" }, 
			/** 妙聚 */
			{ "miaoju", 102, "妙聚" },
			
	};

	/** 心悦渠道pf值 */
	public static final String PF_XINYUE = "xinyue";

	private String appId;
	private String appKey;
	private String appName;
	private String tgwUrl;
	private String url3366;
	/** 是否需要账号激活 */
	private boolean activate;

	private String qqBrowserUrl;
	private String resUrl;
	// private String logUpUrl; //数据上报地址
	private boolean logUpEnable; // 数据上报是否开启
	private String analysisUrl;
	private String compassUrl;
	private String unionUrl;
	private boolean reportEnable;
	private Map<String, Integer> pf2value = new HashMap<>();

	@Override
	public void parseFrom(Element element) throws ServiceException {
		super.parseFrom(element);
		this.setAppId(element.attributeValue(APP_ID));
		this.setAppKey(element.attributeValue(APP_KEY));
		this.setAppName(element.attributeValue(APP_NAME));
		this.setResUrl(element.attributeValue(RES_URL));
		this.setTgwUrl(element.attributeValue(TGW_URL));
		this.setUrl3366(element.attributeValue(URL_3366));
		this.setActivate(XMLUtil.attributeValueBoolean(element, "activate"));
		// this.setLogUpUrl(element.attributeValue(LOG_UP_URL));
		boolean logUpEnable = XMLUtil.attributeValueBoolean(element, LOG_UP_ENABLE, false);
		this.setLogUpEnable(logUpEnable);
		boolean reportEnable = XMLUtil.attributeValueBoolean(element, REPORT_ENABLE, false);
		this.setReportEnable(reportEnable);
		this.setAnalysisUrl(XMLUtil.attributeValueString(element, "analysisUrl") + "/");
		this.setCompassUrl(XMLUtil.attributeValueString(element, "compassUrl") + "/");
		for (Object[] objs : PFS) {
			pf2value.put((String) objs[0], (Integer) objs[1]);
		}
		this.setUnionUrl(XMLUtil.attributeValueString(element, "unionUrl") + "/");
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getResUrl() {
		return resUrl;
	}

	public void setResUrl(String resUrl) {
		this.resUrl = resUrl;
	}

	public String getAnalysisUrl() {
		return analysisUrl;
	}

	public void setAnalysisUrl(String analysisUrl) {
		this.analysisUrl = analysisUrl;
	}

	public String getCompassUrl() {
		return compassUrl;
	}

	public void setCompassUrl(String compassUrl) {
		this.compassUrl = compassUrl;
	}

	// public String getLogUpUrl()
	// {
	// return logUpUrl;
	// }
	// public void setLogUpUrl(String logUpUrl)
	// {
	// this.logUpUrl = logUpUrl;
	// }
	public boolean isLogUpEnable() {
		return logUpEnable;
	}

	public void setLogUpEnable(boolean logUpEnable) {
		this.logUpEnable = logUpEnable;
	}

	public String getUnionUrl() {
		return unionUrl;
	}

	public void setUnionUrl(String unionUrl) {
		this.unionUrl = unionUrl;
	}

	public boolean isReportEnable() {
		return reportEnable;
	}

	public void setReportEnable(boolean reportEnable) {
		this.reportEnable = reportEnable;
	}

	public String getTgwUrl() {
		return tgwUrl;
	}

	public void setTgwUrl(String tgwUrl) {
		this.tgwUrl = tgwUrl;
	}

	public boolean isActivate() {
		return activate;
	}

	public void setActivate(boolean activate) {
		this.activate = activate;
	}

	public String getUrl3366() {
		return url3366;
	}

	public void setUrl3366(String url3366) {
		this.url3366 = url3366;
	}

	public String getQqBrowserUrl() {
		return qqBrowserUrl;
	}

	public void setQqBrowserUrl(String qqBrowserUrl) {
		this.qqBrowserUrl = qqBrowserUrl;
	}

	public int getPFValue(String pf) {
		if (pf.indexOf("union") >= 0) {
			pf = StringUtils.substringBetween(pf, PF_STAR_SPLIT, PF_SPLIT);
		}
		Integer pfValue = pf2value.get(pf);
		if (pfValue != null) {
			return pfValue;
		}
		return 12; // 20; //阿拉丁
		// 我们和开平是两个完全不同的部门，其他游戏上报这个的时候，从没有遇到过这个问题
		// 阿拉丁
		// 2014/9/18 17:41:16
		// 你如果实在无法区分，上报20吧。
	}
}
