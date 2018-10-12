package com.cai.service;

import java.io.IOException;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.domain.Event;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.XMLParser;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;
import com.cai.http.model.ScanPayQueryReqData;
import com.cai.http.model.ScanPayQueryResData;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * 平台API接入
 * 
 * @author run
 *
 */
public class PtAPIServiceImpl extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(PtAPIServiceImpl.class);

	private static PtAPIServiceImpl instance = null;

	/**
	 * 订单号锁
	 */
	private final Cache<String, Lock> orderLockMap;

	private PtAPIServiceImpl() {
		orderLockMap = CacheBuilder.newBuilder().concurrencyLevel(8).expireAfterWrite(10, TimeUnit.MINUTES).build();
	}

	public static PtAPIServiceImpl getInstance() {
		if (null == instance) {
			instance = new PtAPIServiceImpl();
		}
		return instance;
	}

	public Lock getOrCreateOrderLock(String centerOrderID) {
		Lock lock = orderLockMap.getIfPresent(centerOrderID);
		if (lock == null) {
			synchronized (this) {
				lock = new ReentrantLock();
				orderLockMap.put(centerOrderID, lock);
			}
		}
		return lock;
	}

	// 微信接口
	/**
	 * 通过code获取access_token
	 * 
	 * @param code
	 * @return
	 * 
	 */
	public JSONObject wxGetAccessTokenByCode(String code, int game_id) {
		// http请求方式: GET
		// https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5000);
		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/oauth2/access_token?appid=").append(sysParamModel.getStr1()).append("&&secret=")
				.append(sysParamModel.getStr2()).append("&code=").append(code).append("&grant_type=authorization_code");
		// System.out.println(buf.toString());
		try {
			String result = HttpClientUtils.get(buf.toString());
			// System.out.println("result=" + result);
			if (result == null)
				return null;
			JSONObject jsonObject = JSON.parseObject(result);
			return jsonObject;

		} catch (Exception e) {
			logger.error("error", e);
		}

		return null;
	}

	/**
	 * 刷新或续期access_token使用
	 * 
	 * @return
	 */
	public JSONObject wxFlushToken(int game_id, String refresh_token) {

		// https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=APPID&grant_type=refresh_token&refresh_token=REFRESH_TOKEN
		if (game_id == 0) {
			game_id = 6;
		}

		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5000);
		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=").append(sysParamModel.getStr1()).append("&grant_type=refresh_token")
				.append("&refresh_token=").append(refresh_token);
		// System.out.println(buf.toString());
		try {
			String result = HttpClientUtils.get(buf.toString());
			// System.out.println("result=" + result);
			if (result == null)
				return null;
			JSONObject jsonObject = JSON.parseObject(result);
			return jsonObject;

		} catch (Exception e) {
			logger.error("error", e);
		}

		return null;
	}

	/**
	 * 检验授权凭证（access_token）是否有效
	 * 
	 * @return
	 */
	public JSONObject wxCheckToken(int game_id, String access_token, String openid) {

		// https://api.weixin.qq.com/sns/auth?access_token=ACCESS_TOKEN&openid=OPENID
		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/auth?access_token=").append(access_token).append("&openid=").append(openid);
		// System.out.println(buf.toString());
		try {
			String result = HttpClientUtils.get(buf.toString());
			// System.out.println("result=" + result);
			if (result == null)
				return null;
			JSONObject jsonObject = JSON.parseObject(result);
			return jsonObject;

		} catch (Exception e) {
			logger.error("error", e);
		}

		return null;
	}

	/**
	 * 获取用户个人信息（UnionID机制）
	 * 
	 * @return
	 */
	public JSONObject wxUserinfo(int game_id, String access_token, String openid) {

		// https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
		game_id = game_id == 0 ? 1 : game_id;

		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/userinfo?access_token=").append(access_token).append("&&openid=").append(openid);
		// System.out.println(buf.toString());
		try {
			String result = HttpClientUtils.get(buf.toString());
			// System.out.println("result=" + result);
			if (result == null)
				return null;
			JSONObject jsonObject = JSON.parseObject(result);
			return jsonObject;

		} catch (Exception e) {
			logger.error("error", e);
		}

		return null;
	}

	/**
	 * 支付查询api
	 * 
	 * @param reqData
	 * @return
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public ScanPayQueryResData scanPayQueryReq(ScanPayQueryReqData reqData) {
		try {
			String res = HttpClientUtils.postParameters("https://api.mch.weixin.qq.com/pay/orderquery", XMLParser.toXML(reqData));
			logger.warn("ScanPayQueryResData get response:" + res);
			if (StringUtils.isEmpty(res)) {
				return null;
			}
			ScanPayQueryResData scanPayQueryResData = XMLParser.getObjectFromXML(res, ScanPayQueryResData.class);
			return scanPayQueryResData;
		} catch (Exception e) {
			logger.error("支付查询api异常", e);
		}
		return null;
	}

	@Override
	protected void startService() {
		// TODO Auto-generated method stub

	}

	@Override
	public MonitorEvent montior() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onEvent(Event<SortedMap<String, String>> event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionCreate(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sessionFree(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dbUpdate(int _userID) {
		// TODO Auto-generated method stub

	}

}
