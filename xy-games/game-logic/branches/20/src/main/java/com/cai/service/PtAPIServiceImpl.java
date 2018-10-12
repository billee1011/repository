package com.cai.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.Event;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.Signature;
import com.cai.common.util.SpringService;
import com.cai.common.util.XMLParser;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.PayBuyResponse;

/**
 * 平台API接入
 * 
 * @author run
 *
 */
public class PtAPIServiceImpl extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(PtAPIServiceImpl.class);

	private static PtAPIServiceImpl instance = null;

	private PtAPIServiceImpl() {
	}

	public static PtAPIServiceImpl getInstance() {
		if (null == instance) {
			instance = new PtAPIServiceImpl();
		}
		return instance;
	}

	/**
	 * 
	 */
	/**
	 * 通过code获取access_token
	 * 
	 * @param code
	 * @return
	 * 
	 */
	public String getbaiduPosition(int game_id, double x_pos, double y_pos) {
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5000);
		StringBuilder buf = new StringBuilder();
		buf.append("http://api.map.baidu.com/geocoder/v2/?callback=renderReverse&location=").append(x_pos).append(",")
				.append(y_pos).append("&output=json&pois=0&ak=").append("2ou77gmZe77fCQKG2I4Ep99NnnZjKUC8");

		// System.out.println(buf.toString());
		try {
			String result = HttpClientUtils.get(buf.toString());
			// System.out.println("result=" + result);
			int begin = result.indexOf("(");
			int end = result.indexOf(")");
			result = result.substring(begin + 1, end);
			JSONObject json = JSON.parseObject(result);
			int status = json.getInteger("status");
			if (status != 0) {
				logger.error("定位获取失败status==", status);
				return "";
			}

			json = json.getJSONObject("result");
			String addr = json.getString("formatted_address");
			String subaddr = json.getString("sematic_description");
			return addr + subaddr;
		} catch (Exception e) {
			logger.error("error", e);
		}
		return "";

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
		buf.append("https://api.weixin.qq.com/sns/oauth2/access_token?appid=").append(sysParamModel.getStr1())
				.append("&&secret=").append(sysParamModel.getStr2()).append("&code=").append(code)
				.append("&grant_type=authorization_code");
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
