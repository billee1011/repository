package com.cai.service;

import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.EGameType;
import com.cai.common.domain.Event;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.HttpClientUtils;
import com.cai.core.MonitorEvent;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;

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
	 * @param code
	 * @return
	 * 
	 */
	public String getbaiduPosition(int game_id, double x_pos, double y_pos) {
//		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5005);
//		if (sysParamModel == null) {
//			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5005);
//		}
//
//		StringBuilder buf = new StringBuilder();
//		buf.append("http://api.map.baidu.com/geocoder/v2/?callback=renderReverse&location=").append(x_pos).append(",").append(y_pos)
//				.append("&output=json&pois=0&coordtype=wgs84ll&ak=").append(sysParamModel.getStr1());
//
//		// System.out.println(buf.toString());
//		try {
//			String result = HttpClientUtils.get(buf.toString());
//			// System.out.println("result=" + result);
//			int begin = result.indexOf("(");
//			int end = result.lastIndexOf(")");
//			result = result.substring(begin + 1, end);
//			JSONObject json = JSON.parseObject(result);
//			int status = json.getInteger("status");
//			if (status != 0) {
//				logger.error("定位获取失败status==", status);
//				return "";
//			}
//
//			json = json.getJSONObject("result");
//			String addr = json.getString("formatted_address");
//			// String subaddr = json.getString("sematic_description");
//			if (addr.length() > 10) {
//				addr = addr.substring(0, addr.length() - 5);
//			}
//			if (addr.length() > 15) {
//				addr = addr.substring(0, addr.length() - 7);
//			}
//			return addr;
//		} catch (Exception e) {
//			logger.error("error"+"x_pos=" + x_pos + "y_pos" + y_pos, e);
//		}
		return "";

	}

	/**
	 * @param code
	 * @return
	 * 
	 */
	public String getTengXunPosition(int game_id, double x_pos, double y_pos) {
		
//		try {
//			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5005);
//			if (sysParamModel == null) {
//				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5005);
//			}
//			StringBuilder buf = new StringBuilder();
//			buf.append("http://restapi.amap.com/v3/geocode/regeo?location=").append(y_pos).append(",").append(x_pos).append("&key=")
//					.append(sysParamModel.getStr2());
//			//
//			if (sysParamModel.getStr2().isEmpty())
//				return "";
//			String result = HttpClientUtils.get(buf.toString());
//			// System.out.println("result=" + result);
//			JSONObject json = JSON.parseObject(result);
//			JSONObject code = json.getJSONObject("regeocode");
//			String addr = code.getString("formatted_address");
//			if (addr.length() > 10) {
//				addr = addr.substring(0, addr.length() - 5);
//			}
//			if (addr.length() > 15) {
//				addr = addr.substring(0, addr.length() - 7);
//			}
//			return addr;
//		} catch (Exception e) {
//			logger.error("error"+"x_pos=" + x_pos + "y_pos" + y_pos, e);
//		}
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
		
		try {
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5000);
			if (null == sysParamModel) {
				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(5000);
			}
			StringBuilder buf = new StringBuilder();
			buf.append("https://api.weixin.qq.com/sns/oauth2/access_token?appid=").append(sysParamModel.getStr1()).append("&&secret=")
					.append(sysParamModel.getStr2()).append("&code=").append(code).append("&grant_type=authorization_code");
			// System.out.println(buf.toString());
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
