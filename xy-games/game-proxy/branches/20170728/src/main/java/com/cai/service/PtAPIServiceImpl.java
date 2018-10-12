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
import com.cai.domain.UnifiedOrderReqData;
import com.cai.domain.UnifiedOrderResponseData;
import com.xianyi.framework.server.AbstractService;

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
		game_id=game_id==0?1:game_id;
		
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
	
	/**
	 * @param code
	 * @return
	 * 
	 */
	public String getbaiduPosition(int game_id, double x_pos, double y_pos) {
		game_id=game_id==0?1:game_id;
		
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5005);
		StringBuilder buf = new StringBuilder();
		buf.append("http://api.map.baidu.com/geocoder/v2/?callback=renderReverse&location=").append(x_pos).append(",")
				.append(y_pos).append("&output=json&pois=0&coordtype=wgs84ll&ak=").append(sysParamModel.getStr1());

		// System.out.println(buf.toString());
		try {
			String result = HttpClientUtils.get(buf.toString());
			// System.out.println("result=" + result);
			int begin = result.indexOf("(");
			int end = result.lastIndexOf(")");
			result = result.substring(begin + 1, end);
			JSONObject json = JSON.parseObject(result);
			int status = json.getInteger("status");
			if (status != 0) {
				logger.error("定位获取失败status==", status);
				return "";
			}

			json = json.getJSONObject("result");
			String addr = json.getString("formatted_address");
//			String subaddr = json.getString("sematic_description");
			if(addr.length()>10) {
				addr = addr.substring(0, addr.length()-5);
			}if(addr.length()>15) {
				addr = addr.substring(0, addr.length()-7);
			}
			return addr;
		} catch (Exception e) {
			logger.error("error", e,"x_pos="+x_pos+"y_pos"+y_pos);
		}
		return "";

	}

	/**
	 * 刷新或续期access_token使用
	 * 
	 * @return
	 */
	public JSONObject wxFlushToken(int game_id, String refresh_token) {

		// https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=APPID&grant_type=refresh_token&refresh_token=REFRESH_TOKEN
		game_id=game_id==0?1:game_id;
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(5000);
		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=").append(sysParamModel.getStr1())
				.append("&grant_type=refresh_token").append("&refresh_token=").append(refresh_token);
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
		game_id=game_id==0?1:game_id;
		// https://api.weixin.qq.com/sns/auth?access_token=ACCESS_TOKEN&openid=OPENID
		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/auth?access_token=").append(access_token).append("&openid=")
				.append(openid);
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
		game_id=game_id==0?1:game_id;
		StringBuilder buf = new StringBuilder();
		buf.append("https://api.weixin.qq.com/sns/userinfo?access_token=").append(access_token).append("&&openid=")
				.append(openid);
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
	 * 统一下单
	 * 
	 * @param gameOrderID
	 *            -内部订单Id
	 * @param total_fee
	 *            总金额 --分为单位
	 * @param create_ip
	 *            玩家Ip
	 * @param productID
	 *            商品ID
	 * @return
	 */
	public String getPrepayId(String gameOrderID, String body, int total_fee, String create_ip, int productID,int gameID) {
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID).get(5000);// 微信登录相关,str1=appid,str2=应用密钥
		SysParamModel sysParamModel5001 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID).get(5001);// 微信支付str1=商户号str2=key
		SysParamModel sysParamModel5002 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID).get(5002);// 微信支付回调地址str1
		UnifiedOrderReqData reqData = new UnifiedOrderReqData.UnifiedOrderReqDataBuilder(sysParamModel5000.getStr1(),
				sysParamModel5001.getStr1(), sysParamModel5002.getStr2(), gameOrderID, total_fee, create_ip, sysParamModel5002.getStr1(),
				"APP", sysParamModel5001.getStr2()).setProduct_id(productID + "").setDevice_info("WEB").build();
		try {
			logger.debug("UnifiedOrderReqData get request:" + reqData.toString());
			String res = HttpClientUtils.postParameters("https://api.mch.weixin.qq.com/pay/unifiedorder",
					XMLParser.toXML(reqData));
			logger.debug("UnifiedOrder get response:" + res);
			UnifiedOrderResponseData responseData = XMLParser.getObjectFromXML(res, UnifiedOrderResponseData.class);
			logger.debug("统一下单收到返回" + responseData.toString());
			String return_code = responseData.getReturn_code();
			String return_msg = responseData.getReturn_msg();
			if (return_code != null && return_msg != null && return_code.equalsIgnoreCase("SUCCESS")
					&& return_msg.equalsIgnoreCase("OK")) {
				return responseData.getPrepay_id();
			} else {
				logger.error("统一下单失败请求参数" + reqData.toString());
				logger.error("统一下单返回值" + "return_code=" + return_code + "return_msg=" + return_msg);
			}

		} catch (Exception e) {
			logger.error("统一下单异常", e);
		}
		return "";
	}
	
	
	public PayBuyResponse getPayBuyResponse(String prepayID,String gameOrderID,int gameID) {
		
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID).get(5000);// 微信登录相关,str1=appid,str2=应用密钥
		SysParamModel sysParamModel5001 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameID).get(5001);// 微信支付str1=商户号str2=key
		// 签名生成 临时订单
		Map<String, Object> signMap = new HashMap<String, Object>();
		signMap.put("appid", sysParamModel5000.getStr1());
		signMap.put("partnerid", sysParamModel5001.getStr1());
		signMap.put("prepayid", prepayID);
		signMap.put("package", "Sign=WXPay");
		signMap.put("noncestr", XMLParser.getRandomStringByLength(32));
		signMap.put("timestamp", XMLParser.getTimeStamp());
		signMap.put("sign", Signature.getSign(signMap, sysParamModel5001.getStr2()));
		logger.info("发送给微信的订单参数" + JSONObject.toJSONString(signMap));
		
		PayBuyResponse.Builder payBuyResponseBuidler = PayBuyResponse.newBuilder();
		payBuyResponseBuidler.setAppid((String) signMap.get("appid"));
		payBuyResponseBuidler.setPartnerid((String) signMap.get("partnerid"));
		payBuyResponseBuidler.setPrepayid((String) signMap.get("prepayid"));
		payBuyResponseBuidler.setTimestamp((String) signMap.get("timestamp"));
		payBuyResponseBuidler.setNoncestr((String) signMap.get("noncestr"));
		payBuyResponseBuidler.setPackage((String) signMap.get("package"));
		payBuyResponseBuidler.setSign((String) signMap.get("sign"));
		payBuyResponseBuidler.setGameOrderID(gameOrderID);
		return payBuyResponseBuidler.build();
	}


	/**
	 * 客户端 充值 生成临时订单
	 * 
	 * @param orderID订单流水号
	 * @param accountId
	 * @param nickname
	 * @param accountType
	 * @param sellType
	 * @param shopId
	 * @param cardNum
	 * @param sendNum
	 * @param rmb
	 * @param cashAccountID
	 * @param cashAccountName
	 * @param remark
	 * @param ossID
	 * @param ossName
	 */
	public void addCardLog(String orderID, long accountId, String nickname, int accountType, int sellType, int shopId,
			int cardNum, int sendNum, int rmb, int cashAccountID, String cashAccountName, String remark, String ossID,
			String ossName, String centerOrderID,int gameID,String shopName) {
		try {
			AddCardLog addcardlog = new AddCardLog();
			addcardlog.setAccountId(accountId);
			addcardlog.setOrderID(orderID);
			addcardlog.setAccountType(accountType);
			addcardlog.setCardNum(cardNum);
			addcardlog.setCashAccountID(cashAccountID);
			addcardlog.setCashAccountName(cashAccountName);
			addcardlog.setCreate_time(new Date());
			addcardlog.setNickname(nickname);
			addcardlog.setRemark(remark);
			addcardlog.setRmb(rmb);
			addcardlog.setSellType(sellType);
			addcardlog.setSendNum(sendNum);
			addcardlog.setShopId(shopId);
			addcardlog.setOssID(ossID);
			addcardlog.setOssName(ossName);
			addcardlog.setCenterOrderID(centerOrderID);
			addcardlog.setGameId(gameID);
			addcardlog.setShopName(shopName);
			addcardlog.setOrderStatus(1);
			SpringService.getBean(MongoDBService.class).getMongoTemplate().insert(addcardlog);
		} catch (Exception e) {
			logger.error("addcardLog插入日志异常" + e);
		}
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
