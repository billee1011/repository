package com.cai.http.action;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EBonusPointsType;
import com.cai.common.define.EDiamondOperateType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ChannelModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.sdk.DiamondLogModel;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.ConcurrentSet;
import com.cai.common.util.HttpClientUtils;
import com.cai.common.util.MD5;
import com.cai.common.util.Signature;
import com.cai.common.util.SpringService;
import com.cai.common.util.XMLParser;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.http.model.PayNotifyData;
import com.cai.http.model.ResponseData;
import com.cai.http.security.SignUtil;
import com.cai.service.BonusPointsService;
import com.cai.service.MongoDBService;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.ZZPromoterService;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Controller
@RequestMapping("/weixin")
public class WeixinController {

	private static Logger logger = Logger.getLogger(WeixinController.class);

	private static int failTimes;

	private String failResponseData(String msg) {
		ResponseData responseData = new ResponseData();
		responseData.setReturn_msg(msg);
		responseData.setReturn_code("FAIL");
		String toResponse = XMLParser.toXML(responseData);
		return toResponse;
	}

	private void updateAddCardLog(String remark, Query query, String notityXml, int orderStatus, String transactionID) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Update update = new Update();
		update.set("centerOrderID", transactionID);
		update.set("orderSoures", notityXml);
		update.set("orderStatus", 0);
		update.set("remark", remark);
		if (orderStatus == 0) {
			Date gameOrderTime = new Date();
			update.set("finishDate", gameOrderTime);
		}
		mongoDBService.getMongoTemplate().updateFirst(query, update, AddCardLog.class);
	}

	private void updateDiamondLogModel(String remark, Query query, String notityXml, int orderStatus, String transactionID) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Update update = new Update();
		update.set("centerOrderID", transactionID);
		update.set("orderSoures", notityXml);
		update.set("orderStatus", 0);
		update.set("remark", remark);
		if (orderStatus == 0) {
			Date gameOrderTime = new Date();
			update.set("finishDate", gameOrderTime);
		}
		mongoDBService.getMongoTemplate().updateFirst(query, update, DiamondLogModel.class);
	}

	/**
	 * 处理微信充值钻石
	 * 
	 * @param payNotifyData
	 * @param response
	 * @param model
	 * @param notityXml
	 */
	private void payCallDiamond(PayNotifyData payNotifyData, HttpServletResponse response, Model model, String notityXml) {

		ResponseData responseData = new ResponseData();// 返回给微信的

		String gameOrderID = payNotifyData.getOut_trade_no();
		logger.info("微信回调订单号：" + gameOrderID);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Lock lock = PtAPIServiceImpl.getInstance().getOrCreateOrderLock(gameOrderID);
		lock.lock();
		try {
			if (payNotifyData.getReturn_code().equalsIgnoreCase("SUCCESS")) {// 说明支付是成功的

				logger.warn("payCallDiamond收到微信回调成功通知:" + payNotifyData.toString());

				responseData.setReturn_code("SUCCESS");

				// 看是否处理过了
				MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
				Query query = new Query();
				query.addCriteria(Criteria.where("orderID").is(gameOrderID));

				DiamondLogModel diamondCardLog = mongoDBService.getMongoTemplate().findOne(query, DiamondLogModel.class);
				if (diamondCardLog == null) {
					logger.error("payCallDiamond重大bug微信发来的订单号  在我这里找不到记录 transaction_id= " + payNotifyData.getTransaction_id()
							+ "中心订单号centerOrederID=" + payNotifyData.getOut_trade_no());
					response.getWriter().write(failResponseData("找不到临时订单"));
					return;
				}

				String appid, mch_id, desc, notify_url, key;
				ChannelModel channelModel = ChannelModelDict.getInstance().getChannelModel(diamondCardLog.getChannelId());
				if (null == channelModel) {
					logger.error(String.format("payCallDiamond重大bug channelId:%d 查出来为空,transaction_id:%s,centerOrederID:%s",
							diamondCardLog.getChannelId(), payNotifyData.getTransaction_id(), payNotifyData.getOut_trade_no()));
					responseData.setReturn_msg("FAIL");
					response.getWriter().write(XMLParser.toXML(responseData));
					return;
				}

				appid = channelModel
						.getChannelAppId(); /*
											 * sysParamModel5000.getStr1();
											 */
				mch_id = channelModel
						.getChannelAppCode(); /*
												 * sysParamModel5001 .getStr1();
												 */
				desc = channelModel
						.getChannelPayDesc(); /*
												 * sysParamModel5002 .getStr2();
												 */
				notify_url = channelModel.getChannelPayCBUrl(); /*
																 * sysParamModel5002.
																 * getStr1( );
																 */ // 支付通知地址--给微信回调的
				key = channelModel
						.getChannelPaySecret(); /*
												 * sysParamModel5001 .getStr2();
												 */

				boolean isSign = Signature.checkSignature(payNotifyData.toMap(), key);
				if (!isSign) {
					responseData.setReturn_msg("签名失败");
					responseData.setReturn_code("FAIL");
					String toResponse = XMLParser.toXML(responseData);
					try {
						response.getWriter().write(toResponse);
					} catch (IOException e) {
						e.printStackTrace();
					}
					logger.error("签名失败总次数=" + failTimes++ + " :签名失败 微信订单=" + payNotifyData.getTransaction_id() + "本地订单="
							+ payNotifyData.getOut_trade_no());
					return;
				}

				logger.debug("微信支付 签名成功");

				if ((!diamondCardLog.getOrderID().equals(gameOrderID))) {
					logger.error("重大bug查出来不一样 gameOrderID=" + gameOrderID + " diamondCardLog.getOrderID()=" + diamondCardLog.getOrderID());
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));
					return;
				}

				if (diamondCardLog.getOrderStatus() != 1) {// 不是临时的 如果是2
															// 我这边自己会处理
					// 所有告诉微信 我成功
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));
					return;
				}

				if (Integer.parseInt(payNotifyData.getTotal_fee()) != diamondCardLog.getRmb()) {
					logger.error("重大bug!!!rmb 跟 微信传过来的不一样" + "notityXml=" + notityXml + "本地中心订单号=" + payNotifyData.getOut_trade_no());
					response.getWriter().write(failResponseData("rmb不一致"));
					return;
				}

				if (!payNotifyData.getMch_id()
						.equals(/* sysParamModel5001.getStr1() */mch_id)) {
					logger.error("重大bug!!!商户号 跟跟本地不一样" + "notityXml=" + notityXml + "本地中心订单号=" + payNotifyData.getOut_trade_no());
					response.getWriter().write(failResponseData("商户不一致"));
					return;
				}
				// 到这里 一定是 成功的 --后续的异常 自己处理
				responseData.setReturn_msg("OK");
				response.getWriter().write(XMLParser.toXML(responseData));

				SdkDiamondShopModel shop = SdkDiamondShopDict.getInstance().getSdkDiamondShopMap().get(diamondCardLog.getShopId());
				if (shop == null) {
					logger.error("payCallDiamond需要手动处理商品不在列表中！！！gameOrderId=" + gameOrderID + "shopID==" + diamondCardLog.getShopId());
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));// 通知微信成功只能手动处理

					updateDiamondLogModel("找不到商品", query, notityXml, 2, payNotifyData.getTransaction_id());
					return;
				}

				AddGoldResultModel t = null;
				try {

					t = centerRMIServer.addAccountDiamond(diamondCardLog.getAccountId(), shop.getDiamond() + shop.getSend_diamond(), true,
							"安卓充值钻石商品:" + diamondCardLog.getShopId(), EDiamondOperateType.RECHARGE_ANZHUO.getId());// 调用游戏充值
					if (t == null || !t.isSuccess()) {
						logger.error("diamondCardLog游戏服务器返回充值失败！！？gameOrderID=" + gameOrderID);
						return;
					}

				} catch (Exception e) {
					logger.error("RMI处理充值异常" + "notityXml=" + notityXml + "本地中心订单号=" + payNotifyData.getOut_trade_no());
					updateDiamondLogModel("payCallRMI异常", query, notityXml, 2, payNotifyData.getTransaction_id());
					return;
				}

				// 更新微信订单
				updateDiamondLogModel("成功", query, notityXml, 0, payNotifyData.getTransaction_id());

				logger.info("微信回调订单成功：" + gameOrderID);

			} else {
				response.getWriter().write(failResponseData("签名失败"));
				logger.info("[微信支付失败]：" + payNotifyData.getReturn_msg());
			}

		} catch (Exception e) {
			logger.error("回调处理异常", e);
		} finally {
			lock.unlock();
		}

	}

	// 微信回调接口
	@RequestMapping("/paycall")
	public void payCall(HttpServletRequest request, HttpServletResponse response, Model model) {
		// 验证微信充值请求 签名===
		String inputLine;
		String notityXml = "";
		// 取出回调中的数据
		try {
			while ((inputLine = request.getReader().readLine()) != null) {
				notityXml += inputLine;
			}
			request.getReader().close();
		} catch (IOException e1) {
			logger.error("读取微信回调字节流出错");
			return;
		}

		logger.debug("[微信支付][回调]接收到的报文：" + notityXml);

		ResponseData responseData = new ResponseData();// 返回给微信的
		if (StringUtils.isEmpty(notityXml)) {
			responseData.setReturn_msg("签名失败");
			responseData.setReturn_code("FAIL");
			String toResponse = XMLParser.toXML(responseData);
			try {
				response.getWriter().write(toResponse);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		PayNotifyData payNotifyData = XMLParser.getObjectFromXML(notityXml, PayNotifyData.class);

		if (payNotifyData.getAttach() != null && payNotifyData.getAttach().equals(DiamondLogModel.DIAMONDATTACH)) {
			payCallDiamond(payNotifyData, response, model, notityXml);
			return;
		}

		String gameOrderID = payNotifyData.getOut_trade_no();
		logger.info("微信回调订单号：" + gameOrderID);
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Lock lock = PtAPIServiceImpl.getInstance().getOrCreateOrderLock(gameOrderID);
		lock.lock();
		try {
			if (payNotifyData.getReturn_code().equalsIgnoreCase("SUCCESS")) {// 说明支付是成功的

				logger.warn("收到微信回调成功通知:" + payNotifyData.toString());

				responseData.setReturn_code("SUCCESS");

				// 看是否处理过了
				MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
				Query query = new Query();
				query.addCriteria(Criteria.where("orderID").is(gameOrderID));

				AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
				if (addCardLog == null) {
					logger.error("重大bug微信发来的订单号  在我这里找不到记录 transaction_id= " + payNotifyData.getTransaction_id() + "中心订单号centerOrederID="
							+ payNotifyData.getOut_trade_no());
					response.getWriter().write(failResponseData("找不到临时订单"));
					return;
				}
				if (addCardLog.getGameId() == 0) {
					logger.error("记录的gameId=0 " + payNotifyData.getTransaction_id() + "中心订单号centerOrederID=" + payNotifyData.getOut_trade_no());
					responseData.setReturn_code("SUCCESS");
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));// 通知微信成功只能手动处理
					updateAddCardLog("addCardLog.getGameId()==0", query, notityXml, 2, payNotifyData.getTransaction_id());
					return;
				}

				SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(addCardLog.getGameId()).get(5000);// 微信登录相关,str1=appid,str2=应用密钥
				SysParamModel sysParamModel5001 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(addCardLog.getGameId()).get(5001);// 微信支付str1=商户号str2=key
				SysParamModel sysParamModel5002 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(addCardLog.getGameId()).get(5002);// 微信支付回调地址str1

				SysParamModel sysParamModel5007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(5007);// 开关

				String appid, mch_id, desc, notify_url, key;

				if (null != sysParamModel5007 && sysParamModel5007.getVal1().intValue() == 1) {

					ChannelModel channelModel = ChannelModelDict.getInstance().getChannelModel(addCardLog.getChannelId());
					if (null == channelModel) {
						logger.error(String.format("重大bug channelId:%d 查出来为空,transaction_id:%s,centerOrederID:%s", addCardLog.getChannelId(),
								payNotifyData.getTransaction_id(), payNotifyData.getOut_trade_no()));
						responseData.setReturn_msg("FAIL");
						response.getWriter().write(XMLParser.toXML(responseData));
						return;
					}

					appid = channelModel
							.getChannelAppId(); /*
												 * sysParamModel5000.getStr1();
												 */
					mch_id = channelModel.getChannelAppCode(); /*
																 * sysParamModel5001
																 * .getStr1();
																 */
					desc = channelModel.getChannelPayDesc(); /*
																 * sysParamModel5002
																 * .getStr2();
																 */
					notify_url = channelModel
							.getChannelPayCBUrl(); /*
													 * sysParamModel5002.
													 * getStr1( );
													 */ // 支付通知地址--给微信回调的
					key = channelModel.getChannelPaySecret(); /*
																 * sysParamModel5001
																 * .getStr2();
																 */

				} else {
					appid = sysParamModel5000.getStr1();
					mch_id = sysParamModel5001.getStr1();
					key = sysParamModel5001.getStr2();
				}

				boolean isSign = Signature.checkSignature(payNotifyData.toMap(), key);
				if (!isSign) {
					responseData.setReturn_msg("签名失败");
					responseData.setReturn_code("FAIL");
					String toResponse = XMLParser.toXML(responseData);
					try {
						response.getWriter().write(toResponse);
					} catch (IOException e) {
						e.printStackTrace();
					}
					logger.error("签名失败总次数=" + failTimes++ + " :签名失败 微信订单=" + payNotifyData.getTransaction_id() + "本地订单="
							+ payNotifyData.getOut_trade_no());
					return;
				}

				logger.debug("微信支付 签名成功");

				if ((!addCardLog.getOrderID().equals(gameOrderID))) {
					logger.error("重大bug查出来不一样 gameOrderID=" + gameOrderID + " addCardLog.getOrderID()=" + addCardLog.getOrderID());
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));
					return;
				}

				if (addCardLog.getOrderStatus() != 1) {// 不是临时的 如果是2 我这边自己会处理
					// 所有告诉微信 我成功
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));
					return;
				}

				if (Integer.parseInt(payNotifyData.getTotal_fee()) != addCardLog.getRmb()) {
					logger.error("重大bug!!!rmb 跟 微信传过来的不一样" + "notityXml=" + notityXml + "本地中心订单号=" + payNotifyData.getOut_trade_no());
					response.getWriter().write(failResponseData("rmb不一致"));
					return;
				}

				if (!payNotifyData.getMch_id()
						.equals(/* sysParamModel5001.getStr1() */mch_id)) {
					logger.error("重大bug!!!商户号 跟跟本地不一样" + "notityXml=" + notityXml + "本地中心订单号=" + payNotifyData.getOut_trade_no());
					response.getWriter().write(failResponseData("商户不一致"));
					return;
				}
				// 到这里 一定是 成功的 --后续的异常 自己处理
				responseData.setReturn_msg("OK");
				response.getWriter().write(XMLParser.toXML(responseData));

				ShopModel shop = ShopDict.getInstance().getShopModel(addCardLog.getShopId());
				if (shop == null) {
					logger.error("需要手动处理商品不在列表中！！！gameOrderId=" + gameOrderID + "shopID==" + addCardLog.getShopId());
					responseData.setReturn_msg("OK");
					response.getWriter().write(XMLParser.toXML(responseData));// 通知微信成功只能手动处理

					updateAddCardLog("找不到商品", query, notityXml, 2, payNotifyData.getTransaction_id());
					return;
				}

				AddGoldResultModel t = null;
				try {

					t = centerRMIServer.addAccountGold(addCardLog.getAccountId(), shop.getGold() + shop.getSend_gold(), true,
							"游戏内充值商品:" + addCardLog.getShopId(), EGoldOperateType.SHOP_PAY);// 调用游戏充值
					if (t == null || !t.isSuccess()) {
						logger.error("游戏服务器返回充值失败！！？gameOrderID=" + gameOrderID);
						return;
					}
					AccountModel accountModel = centerRMIServer.getAccountModel(addCardLog.getAccountId());
					if (accountModel.getIs_agent() > 0) {
						BonusPointsService.getInstance().rechargeSendBonusPoints(addCardLog.getAccountId(), shop.getPrice(),
								EBonusPointsType.RECHARGE_SEND_BP);
					}
				} catch (Exception e) {
					logger.error("RMI处理充值异常" + "notityXml=" + notityXml + "本地中心订单号=" + payNotifyData.getOut_trade_no());
					updateAddCardLog("payCallRMI异常", query, notityXml, 2, payNotifyData.getTransaction_id());
					return;
				}

				// 更新微信订单
				updateAddCardLog("成功", query, notityXml, 0, payNotifyData.getTransaction_id());
				PublicService publicService = SpringService.getBean(PublicService.class);
				publicService.getPublicDAO().insertAddCard(addCardLog);
				logger.info("微信回调订单成功：" + gameOrderID);
				try {
					Map<String, String> map = new HashMap<String, String>();
					map.put("accountId", addCardLog.getAccountId() + "");
					map.put("money", shop.getPrice() + "");
					centerRMIServer.rmiInvoke(RMICmd.RECHARGE_TASK, map);
				} catch (Exception e) {
					logger.error("调用充值任务失败", e);
				}
				try {
					AccountZZPromoterModel am = ZZPromoterService.getInstance().getAccountZZPromoterModel(addCardLog.getAccountId());
					if (am != null && am.getAccount_id() > 0) {
						ZZPromoterService.getInstance().recharge(1, shop.getPrice(), am.getAccount_id(), addCardLog.getAccountId(),
								addCardLog.getOrderID());
					}
				} catch (Exception e) {
					logger.error("麻将协会推广用户充值返利失败", e);
				}
			} else {
				response.getWriter().write(failResponseData("签名失败"));
				logger.info("[微信支付失败]：" + payNotifyData.getReturn_msg());
			}

		} catch (Exception e) {
			logger.error("回调处理异常", e);
		} finally {
			lock.unlock();
		}

	}

	// 易接审核
	final ConcurrentSet<String> esdkOrder = new ConcurrentSet<>();

	// 易接支付回调接口
	@RequestMapping("/esdk/paycall")
	public void esdkPayCall(HttpServletRequest request, HttpServletResponse response) {

		Map<String, String> paramMap = Maps.newHashMap();
		Enumeration<String> es = request.getParameterNames();

		List<String> keySort = Lists.newArrayList();
		while (es.hasMoreElements()) {
			String k = es.nextElement();
			paramMap.put(k, request.getParameter(k));
			keySort.add(k);
		}

		logger.error("esdk充值回调参数:" + paramMap);

		String tcd = paramMap.get("tcd");
		if (esdkOrder.contains(tcd)) {
			esdkResp(response, "SUCCESS");
			logger.error("重复补单，回复成功！tcd:" + tcd);
			return;
		}

		StringBuilder sb = new StringBuilder();
		Collections.sort(keySort);
		keySort.forEach((k) -> {
			if ("sign".equals(k)) {
				return;
			}
			sb.append(k).append("=").append(paramMap.get(k)).append("&");
		});
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		String shareKey = "RTALJVNKRGQPOJRMIGAMP239NXESIDJM";
		if (!Objects.equal(MD5.MD5Encode(sb.toString() + shareKey), paramMap.get("sign"))) {
			logger.error("签名错误！ self:" + MD5.MD5Encode(sb.toString() + shareKey) + " other:" + paramMap.get("sign"));
			esdkResp(response, "FAILED");
			return;
		}
		// 签名校验

		JSONObject cbiJson = JSON.parseObject(paramMap.get("cbi"));

		ShopModel shop = ShopDict.getInstance().getShopModel(cbiJson.getIntValue("shopId"));
		if (shop == null) {
			logger.error("esdkPayCall 找不到商品id:" + paramMap.get("cbi"));
			esdkResp(response, "FAILED");
			return;
		}

		AddGoldResultModel t = null;
		try {

			t = SpringService.getBean(ICenterRMIServer.class).addAccountGold(cbiJson.getLongValue("accountId"), shop.getGold() + shop.getSend_gold(),
					true, "游戏内充值商品:" + cbiJson.getIntValue("shopId"), EGoldOperateType.SHOP_PAY);// 调用游戏充值
			if (t == null || !t.isSuccess()) {
				logger.error("游戏服务器返回充值失败！！？tcd=" + tcd);
				return;
			}
			esdkOrder.add(tcd);
		} catch (Exception e) {
			logger.error("RMI处理充值异常" + "cbi=" + paramMap.get("cbi") + "tcd=" + tcd);
			return;
		}

		esdkResp(response, "SUCCESS");
	}

	@RequestMapping("/pay/call")
	public void oppoPay(HttpServletRequest request, HttpServletResponse response) {
		try {
			logger.error("收到oppo信息.........");
			SysParamModel sysParamModel2267 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2267);
			String path = "http://sync.imooffice.cn:81/cb/oppo/24F29B91AABE66BA/sync.html";
			if (sysParamModel2267 != null && StringUtils.isBlank(sysParamModel2267.getStr1())) {
				path = sysParamModel2267.getStr1();
			}
			HttpClientUtils.postParameters(path, SignUtil.getParametersHashMap(request));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void esdkResp(HttpServletResponse response, String result) {
		try {
			response.setHeader("content-type", "text/html;charset=UTF-8");
			OutputStream outputStream = response.getOutputStream();
			outputStream.write(result.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Map<String, String> m = Maps.newHashMap();
		m.put("accountId", "30587");
		m.put("shopId", "57");
		System.out.println(JSON.toJSON(m));
		System.out.println(System.currentTimeMillis());

		Map<String, String> signMap = Maps.newHashMap();
		signMap.put("app", "{24F29B91-AABE66BA}");
		signMap.put("ct", "1527566168127");
		signMap.put("pt", "1527566168127");
		signMap.put("ssid", "123456");
		signMap.put("tcd", "4");
		signMap.put("ver", "1");
		signMap.put("cbi", JSON.toJSON(m).toString());
		signMap.put("fee", "100");
		signMap.put("sdk", "1");
		signMap.put("st", "1");
		signMap.put("uid", "1234");

		List<String> keySort = new ArrayList<>(signMap.keySet());
		StringBuilder sb = new StringBuilder();
		Collections.sort(keySort);
		keySort.forEach((k) -> {
			if ("sign".equals(k)) {
				return;
			}
			sb.append(k).append("=").append(signMap.get(k)).append("&");
		});
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		String s = sb.toString() + "RTALJVNKRGQPOJRMIGAMP239NXESIDJM";
		System.out.println(s);
		System.out.println(MD5.MD5Encode(s));

	}
}
