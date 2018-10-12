/**
 * 
 */
package com.cai.future.runnable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.RMICmd;
import com.cai.common.define.EBonusPointsType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.AppStoreApp;
import com.cai.common.domain.zhuzhou.AccountZZPromoterModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.dictionary.ShopDict;
import com.cai.future.GameSchedule;
import com.cai.service.BonusPointsService;
import com.cai.service.MongoDBService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicService;
import com.cai.service.ZZPromoterService;
import com.cai.util.StoreNoticeUtil;

/**
 * 用于ios 闲逸豆充值
 */
public class PayIosRunnable implements Runnable {

	private static Logger logger = Logger.getLogger(PayIosRunnable.class);

	private static final String MSG_SHOP = "找不到商品";
	private static final String MSG_RMI_ERROR = "RMI处理充值异常";
	private static final String MSG_SUCCESS = "回调成功";

	/**
	 * 本地订单号
	 */
	private String gameOrderID;

	/**
	 * 票据
	 */
	private String receiptData;

	private long accountID;

	private long createTime;

	public String transactionIdOld;

	/**
	 * 充值类型
	 */
	private int rechargeType;

	public PayIosRunnable(long accountID, String gameOrderID, String receipt, long createTime, int rechargeType) {
		this.accountID = accountID;
		this.gameOrderID = gameOrderID;
		this.receiptData = receipt;
		this.createTime = createTime;
		this.rechargeType = rechargeType;
	}
	
	public PayIosRunnable(long accountID, String gameOrderID, String receipt, long createTime, int rechargeType,String transactionIdOld) {
		this.accountID = accountID;
		this.gameOrderID = gameOrderID;
		this.receiptData = receipt;
		this.createTime = createTime;
		this.rechargeType = rechargeType;
		this.transactionIdOld = transactionIdOld;
	}

	@Override
	public void run() {
		try {
			// 验证订单
			JSONObject jsonR = AppStoreApp.verifyAppStoreReceipts(receiptData);

			logger.info(accountID + "IOS订单验证返回结果：" + jsonR + "gameOrderID=" + gameOrderID + "客户端传transactionIdOld==" + transactionIdOld);

			if (jsonR == null || jsonR.getIntValue("status") != 0) {// 验证失败
				logger.info("订单验证返回结果：verfiy fail" + jsonR);
				if (System.currentTimeMillis() - createTime < 1000 * 120) {// 2分钟之内的失败
																			// 重新请求下验证
					GameSchedule.put(new PayIosRunnable(accountID, gameOrderID, receiptData, createTime, rechargeType), 5, TimeUnit.SECONDS);
				}
				return;
			}

			// 获取订单相关信息
			JSONObject receipt = jsonR.getJSONObject("receipt");
			String transaction_id = receipt.getString("transaction_id");
			String product_id = receipt.getString("product_id");

			if (!StringUtils.isEmpty(transactionIdOld)) {
				if (!transactionIdOld.equals(transaction_id)) {
					logger.error("transactionIdOld对不上？transactionIdOld=" + transactionIdOld + "transaction_id==" + transaction_id + " product_id="
							+ product_id + "rec" + "");
					return;
				}
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			// 到此为止 成功
			Lock lock = PtAPIServiceImpl.getInstance().getOrCreateOrderLock(gameOrderID);
			lock.lock();
			try {
				AddCardLog addCardLog = getAddCardLog();// 再查询一遍虽然有点效率问题 这次是加锁的
														// 防止
				// 微信拉订单的过程中 又有通知了
				if (addCardLog == null || addCardLog.getOrderStatus() == 0) {
					logger.error(accountID + "addCardLog==" + addCardLog + "receiptData=" + "");
					return;
				}

				if (Integer.parseInt(product_id) != addCardLog.getShopId().intValue()) {
					logger.error("商品ID对不上？gameOrderID=" + gameOrderID + "product_id=" + product_id + "rec" + "");
					return;
				}

				if ((!addCardLog.getOrderID().equals(gameOrderID))) {
					logger.error("重大bug查出来不一样 gameOrderID=" + gameOrderID + " addCardLog.getOrderID()=" + addCardLog.getOrderID() + "rec" + "");
					return;
				}

				// 看是否处理过了
				AppShopModel shop = ShopDict.getInstance().getAppShopModel(addCardLog.getShopId());
				if (shop == null) {
					logger.error("需要手动处理 -- 商品不在列表中！！！gameOrderId=" + gameOrderID + " shopID==" + addCardLog.getShopId() + "rec" + "");
					updateStatusAddCardLog(transaction_id, receiptData, MSG_SHOP + addCardLog.getShopId(), 2);
					return;
				}

				boolean isProcess = getCardLogIsProcessByCenterOrderID(transaction_id, accountID);
				if (isProcess)
					return;

				AddGoldResultModel t = null;
				try {
					EGoldOperateType operateType = EGoldOperateType.SHOP_PAY;
					if (rechargeType == 1) // 金币场充值
					{
						operateType = EGoldOperateType.COIN_PAY;
					}
					t = centerRMIServer.addAccountGold(addCardLog.getAccountId(), shop.getGold() + shop.getSend_gold(), true,
							"游戏内充值商品(appStore):" + addCardLog.getShopId(), operateType);// 调用游戏充值
					if (t == null || !t.isSuccess()) {
						logger.error("游戏服务器返回充值失败！！？gameOrderID=" + gameOrderID + "rec" + "");
						return;
					}
					AccountModel model = centerRMIServer.getAccountModel(addCardLog.getAccountId());
					if (model.getIs_agent() > 0) {
						BonusPointsService.getInstance().rechargeSendBonusPoints(addCardLog.getAccountId(), shop.getPrice(),
								EBonusPointsType.RECHARGE_SEND_BP);
					}
					StoreNoticeUtil.storeNotice(addCardLog);
				} catch (Exception e) {
					logger.error("RMI处理充值异常" + "本地订单号=" + gameOrderID + "rec" + receiptData);
					updateStatusAddCardLog(transaction_id, receiptData, MSG_RMI_ERROR, 2);
					return;
				}
				updateStatusAddCardLog(transaction_id, receiptData, MSG_SUCCESS, 0);
				PublicService publicService = SpringService.getBean(PublicService.class);
				publicService.getPublicDAO().insertAddCard(addCardLog);
				logger.info(gameOrderID + " 订单落地成功，更新订单状态成功");
				PlayerServiceImpl.getInstance().rechargeForCoin(rechargeType, gameOrderID);
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
						ZZPromoterService.getInstance().recharge(2, shop.getPrice(), am.getAccount_id(), addCardLog.getAccountId(),
								addCardLog.getOrderID());
					}
				} catch (Exception e) {
					logger.error("麻将协会推广用户充值返利失败", e);
				}
			} catch (Exception e) {
				logger.error("代理服调用payIOSCall异常" + "rec" + receiptData, e);
			} finally {
				lock.unlock();
			}

		} catch (Exception e) {
			logger.error(accountID + "通知订单job失败" + "rec" + receiptData, e);
		}
	}

	private AddCardLog getAddCardLog() {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("orderID").is(gameOrderID));

		AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
		if (addCardLog == null) {
			logger.error("PayCenterRunnable ios订单居然找不到了!!!" + gameOrderID + "rec" + "");
			return null;
		}
		if ((!addCardLog.getOrderID().equals(gameOrderID))) {
			logger.error("重大bug  查出来不一样 gameOrderID=" + gameOrderID + " addCardLog.getOrderID()=" + addCardLog.getOrderID() + "rec" + "");
			return null;
		}
		return addCardLog;
	}

	private boolean getCardLogIsProcessByCenterOrderID(String centerOrderID, long accountID) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Query query = new Query();
		query.addCriteria(Criteria.where("centerOrderID").is(centerOrderID));

		AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
		if (addCardLog != null) {
			if (addCardLog.getAccountId().longValue() != accountID) {
				logger.error("重大bug查出来不一样 accountID=" + accountID + " addCardLog.getAccountId()=" + addCardLog.getAccountId() + "rec" + "");
				return true;
			}

			if ((!addCardLog.getOrderID().equals(gameOrderID))) {
				logger.error("根据centerOrderID查出来的 本地订单号不一致 不处理=" + gameOrderID + " addCardLog.getOrderID()=" + addCardLog.getOrderID() + "accountID="
						+ accountID + "rec" + "");
				return true;
			}

			if (addCardLog.getOrderStatus() == 0)
				return true;
		}
		return false;
	}

	private void updateStatusAddCardLog(String transactionID, String orderSoures, String remark, int orderStatus) {
		MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
		Update update = new Update();
		Query query = new Query();
		query.addCriteria(Criteria.where("orderID").is(gameOrderID));

		update.set("centerOrderID", transactionID);
		update.set("orderSoures", orderSoures);
		update.set("orderStatus", orderStatus);
		update.set("remark", remark);
		if (orderStatus == 0) {
			Date finishDate = new Date();
			update.set("finishDate", finishDate);
		}
		mongoDBService.getMongoTemplate().updateFirst(query, update, AddCardLog.class);
	}

}
