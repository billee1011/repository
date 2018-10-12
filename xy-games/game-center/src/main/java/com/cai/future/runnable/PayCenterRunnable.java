package com.cai.future.runnable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.cai.common.constant.RMICmd;
import com.cai.common.define.EBonusPointsType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ChannelModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.common.util.XMLParser;
import com.cai.dictionary.ChannelModelDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.http.model.ScanPayQueryReqData;
import com.cai.http.model.ScanPayQueryResData;
import com.cai.service.BonusPointsService;
import com.cai.service.MongoDBService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.ZZPromoterService;
import com.cai.util.StoreNoticeUtil;

/**
 * 用于主动去微信获取支付订单
 */
public class PayCenterRunnable implements Runnable {

	private static Logger logger = Logger.getLogger(PayCenterRunnable.class);

	private static int fail_Times = 0;

	private static final String MSG_SHOP = "找不到商品";
	private static final String MSG_RMI_ERROR = "RMI处理充值异常";
	private static final String MSG_SUCCESS = "回调成功";

	/**
	 * 本地订单号
	 */
	private String gameOrderID;

	/**
	 * 充值类型
	 */
	private int rechargeType;

	public PayCenterRunnable(String gameOrderID, int rechargeType) {
		this.gameOrderID = gameOrderID;
		this.rechargeType = rechargeType;
	}

	@Override
	public void run() {
		try {
			logger.info(gameOrderID + " 订单开始落地");
			AddCardLog addCardLog = getAddCardLog();
			if (addCardLog == null || addCardLog.getOrderStatus() == 0) {
				return;
			}

			// 再去微信拉订单
			ScanPayQueryResData scanPayQueryResData = getScanPayQueryResData(addCardLog);
			if (scanPayQueryResData == null) {
				logger.info(gameOrderID + " 订单校验失败，落地失败");
				return;
			}

			fail_Times++;
			logger.error("PayCenterRunnable 执行微信查询次数" + fail_Times);
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			// 到此为止 成功
			Lock lock = PtAPIServiceImpl.getInstance().getOrCreateOrderLock(gameOrderID);
			lock.lock();
			try {
				addCardLog = getAddCardLog();// 再查询一遍虽然有点效率问题 这次是加锁的 防止
												// 微信拉订单的过程中 又有通知了
				if (addCardLog == null || addCardLog.getOrderStatus() == 0) {
					logger.error("订单又成功了...");
					return;
				}

				// 看是否处理过了
				ShopModel shop = ShopDict.getInstance().getShopModel(addCardLog.getShopId());
				if (shop == null) {
					logger.error("需要手动处理 -- 商品不在列表中！！！gameOrderId=" + gameOrderID + " shopID==" + addCardLog.getShopId());
					updateStatusAddCardLog(scanPayQueryResData, MSG_SHOP + addCardLog.getShopId(), 2);
					return;
				}

				AddGoldResultModel t = null;
				try {
					EGoldOperateType operateType = EGoldOperateType.SHOP_PAY;
					if (rechargeType == 1) // 金币场充值
					{
						operateType = EGoldOperateType.COIN_PAY;
					}
					t = centerRMIServer.addAccountGold(addCardLog.getAccountId(), shop.getGold() + shop.getSend_gold(), true,
							"游戏内充值商品:" + addCardLog.getShopId(), operateType);// 调用游戏充值
					if (t == null || !t.isSuccess()) {
						logger.error("游戏服务器返回充值失败！！？gameOrderID=" + gameOrderID);
						return;
					}
					AccountModel model = centerRMIServer.getAccountModel(addCardLog.getAccountId());
					if (model.getIs_agent() > 0) {
						BonusPointsService.getInstance().rechargeSendBonusPoints(addCardLog.getAccountId(), shop.getPrice(),
								EBonusPointsType.RECHARGE_SEND_BP);
					}
					StoreNoticeUtil.storeNotice(addCardLog);
				} catch (Exception e) {
					logger.error("RMI处理充值异常" + "本地订单号=" + gameOrderID);
					updateStatusAddCardLog(scanPayQueryResData, MSG_RMI_ERROR, 2);
					return;
				}
				updateStatusAddCardLog(scanPayQueryResData, MSG_SUCCESS, 0);
				PublicService publicService = SpringService.getBean(PublicService.class);
				publicService.getPublicDAO().insertAddCard(addCardLog);
				logger.info(gameOrderID + " 订单落地成功，更新订单状态成功");
				PlayerServiceImpl.getInstance().rechargeForCoin(rechargeType, gameOrderID);
				// 添加充值任务
				try {
					Map<String, String> map = new HashMap<String, String>();
					map.put("accountId", addCardLog.getAccountId() + "");
					map.put("money", shop.getPrice() + "");
					map.put("rechargeType", "0");
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
			} catch (Exception e) {
				logger.error("代理服调用payCenterCall异常", e);
			} finally {
				lock.unlock();
			}

		} catch (Exception e) {
			logger.error("通知订单job失败", e);
		}
	}

	private AddCardLog getAddCardLog() {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("orderID").is(gameOrderID));

		AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
		if (addCardLog == null) {
			logger.error("安卓 PayCenterRunnable 订单居然找不到了!!!" + gameOrderID);
			return null;
		}
		if ((!addCardLog.getOrderID().equals(gameOrderID))) {
			logger.error("重大bug  查出来不一样 gameOrderID=" + gameOrderID + " addCardLog.getOrderID()=" + addCardLog.getOrderID());
			return null;
		}
		return addCardLog;
	}

	private void updateStatusAddCardLog(ScanPayQueryResData scanPayQueryResData, String remark, int orderStatus) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Update update = new Update();
		Query query = new Query();
		query.addCriteria(Criteria.where("orderID").is(gameOrderID));

		update.set("centerOrderID", scanPayQueryResData.getTransaction_id());
		update.set("orderSoures", scanPayQueryResData.toString());
		update.set("orderStatus", orderStatus);
		update.set("remark", remark);
		if (orderStatus == 0) {
			Date finishDate = new Date();
			update.set("finishDate", finishDate);
		}
		mongoDBService.getMongoTemplate().updateFirst(query, update, AddCardLog.class);
	}

	private ScanPayQueryResData getScanPayQueryResData(AddCardLog addCardLog) {
		PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(addCardLog.getGameId()).get(5000);// 微信登录相关,str1=appid,str2=应用密钥
		SysParamModel sysParamModel5001 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(addCardLog.getGameId()).get(5001);// 微信支付str1=商户号str2=key

		SysParamModel sysParamModel5007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(5007);// 开关

		String appid, mch_id, desc, notify_url, key;
		if (null != sysParamModel5007 && sysParamModel5007.getVal1().intValue() == 1) {

			ChannelModel channelModel = ChannelModelDict.getInstance().getChannelModel(addCardLog.getChannelId());
			if (null == channelModel) {
				logger.error("channelModel is null=" + gameOrderID);
				return null;
			}

			appid = channelModel
					.getChannelAppId(); /* sysParamModel5000.getStr1(); */
			mch_id = channelModel
					.getChannelAppCode(); /* sysParamModel5001.getStr1(); */
			desc = channelModel
					.getChannelPayDesc(); /* sysParamModel5002.getStr2(); */
			notify_url = channelModel
					.getChannelPayCBUrl(); /* sysParamModel5002.getStr1(); */ // 支付通知地址--给微信回调的
			key = channelModel
					.getChannelPaySecret(); /* sysParamModel5001.getStr2(); */

		} else {
			appid = sysParamModel5000.getStr1();
			mch_id = sysParamModel5001.getStr1();
			key = sysParamModel5001.getStr2();
		}

		ScanPayQueryReqData scanPayQueryReq = new ScanPayQueryReqData(appid, mch_id, key, XMLParser.getRandomStringByLength(32), gameOrderID);

		ScanPayQueryResData scanPayQueryResData = ptAPIServiceImpl.scanPayQueryReq(scanPayQueryReq);

		if (scanPayQueryResData == null) {
			logger.error("scanPayQueryResData 拉取失败orderID=" + gameOrderID);
			return null;
		}
		logger.warn("PayCenterRunnable!!主动微信拉取!scanPayQueryResData=" + scanPayQueryResData.toString());
		if (!scanPayQueryResData.getReturn_code().equalsIgnoreCase("SUCCESS")) {// 通信标识
			logger.error("PayCenterRunnable主动拉取失败orderID=" + gameOrderID);
			return null;
		}

		if (!scanPayQueryResData.getTrade_state().equalsIgnoreCase("SUCCESS")) {// 支付是成功的
			logger.error("scanPayQueryResData 微信返回是失败的 啥情况!!!orderID=" + gameOrderID);
			return null;
		}

		if (!scanPayQueryResData.getMch_id().equals(mch_id)) {
			logger.error("重大bug!!!商户号 跟跟本地不一样" + "本地中心订单号=" + scanPayQueryResData.getOut_trade_no());
			return null;
		}

		if (Integer.parseInt(scanPayQueryResData.getTotal_fee()) != addCardLog.getRmb()) {
			logger.error("重大bug!!!rmb 跟 微信传过来的不一样" + "本地中心订单号=" + scanPayQueryResData.getOut_trade_no());
			return null;
		}
		return scanPayQueryResData;
	}

}
