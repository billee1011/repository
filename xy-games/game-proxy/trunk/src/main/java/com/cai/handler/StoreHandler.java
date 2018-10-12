/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import static protobuf.clazz.Protocol.storeAllResponse;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.ItemExchangeModel;
import com.cai.common.domain.MoneyShopModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WelfareExchangeModel;
import com.cai.common.domain.WelfareGoodsTypeModel;
import com.cai.common.domain.sdk.DiamondLogModel;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.StoreOpeType;
import com.cai.common.type.StoreType;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.Global;
import com.cai.dictionary.ItemExchangeDict;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.dictionary.WelfareExchangeDict;
import com.cai.dictionary.WelfareGoodsTypeDict;
import com.cai.future.WeiXinPayDiamondRunnable;
import com.cai.service.MongoDBService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.StoreAllResponse;
import protobuf.clazz.Protocol.StoreDiamondResponse;
import protobuf.clazz.Protocol.StoreExchangeItemResponse;
import protobuf.clazz.Protocol.StoreGoodMoneyResponse;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.clazz.Protocol.StoreRequest;
import protobuf.clazz.Protocol.StoreWelfareCardExchangeResponse;
import protobuf.clazz.Protocol.StoreWelfareGoodsType;

/**
 *
 * @author
 */
@ICmd(code = RequestType.STORE_VALUE, exName = "storeRequest")
public class StoreHandler extends IClientHandler<StoreRequest> {

	@Override
	protected void execute(StoreRequest request, Request topRequest, C2SSession session) throws Exception {
		if (!request.hasType())
			return;
		int type = request.getType();

		Account account = session.getAccount();
		if (account == null)
			return;

		final int game_id = account.getGame_id();

		if (type == StoreOpeType.QUERY_FUNCTION_STATU) {

			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1013);
			if (sysParamModel == null) {
				logger.error("获取不到相关的系统参数");
			}

			// 提示
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.QUERY_FUNCTION_STATU);
			storeAllResponseBuilder.setMsg(sysParamModel.getStr1());
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());

		}

		else if (type == StoreOpeType.STORE_UI) {

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.NORMAL_SHOP);
			for (ShopModel shopModel : shopModelList) {
				Date beginShowTime = shopModel.getBegin_show_time();
				Date endShowTime = shopModel.getEnd_show_time();
				Date now = new Date();
				if (beginShowTime != null && endShowTime != null) {
					if (now.before(beginShowTime) || now.after(endShowTime))
						continue;
				}
				StoreGoodResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreGoodResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 城商链接
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_UI);
			storeAllResponseBuilder.addAllStoreGoodResponseList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());
		}

		else if (type == StoreOpeType.STORE_UI_MONEY) {
			// 商品列表
			List<StoreGoodMoneyResponse> storeGoodResponseList = Lists.newArrayList();
			List<MoneyShopModel> shopModelList = MoneyShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id());
			for (MoneyShopModel shopModel : shopModelList) {
				Date beginShowTime = shopModel.getBegin_show_time();
				Date endShowTime = shopModel.getEnd_show_time();
				Date now = new Date();
				if (beginShowTime != null && endShowTime != null) {
					if (now.before(beginShowTime) || now.after(endShowTime))
						continue;
				}
				StoreGoodMoneyResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreMoneyResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 城商链接
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_UI_MONEY);
			storeAllResponseBuilder.addAllMoneyList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());

		}

		else if (type == StoreOpeType.STORE_UI_MONEY_BUY) {
			int shopID = request.getGoodsId();

			List<MoneyShopModel> shopModelList = MoneyShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id());
			MoneyShopModel findModel = null;
			for (MoneyShopModel shop : shopModelList) {
				if (shop.getId() == shopID) {
					findModel = shop;
					break;
				}
			}

			if (findModel == null) {
				logger.error("商品 获取失败!!!" + shopID);
				session.send(MessageResponse.getMsgAllResponse("商品失效").build());
				return;
			}
			Date beginTime = findModel.getBegin_time();
			Date endTime = findModel.getEnd_time();
			Date now = new Date();
			if (beginTime != null && endTime != null) {
				if (now.before(beginTime) || now.after(endTime)) {
					session.send(MessageResponse.getMsgAllResponse("商品未上架").build());
					return;
				}
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			AddGoldResultModel result = centerRMIServer.addAccountGold(account.getAccount_id(), -findModel.getPrice(), false, "兑换金币,shopID:" + shopID,
					EGoldOperateType.MONEY_DUIHUAN);
			if (result.isSuccess()) {
				centerRMIServer.addAccountMoney(account.getAccount_id(), findModel.getMoney() + findModel.getSendMoney(), false,
						"兑换金币,shopID:" + shopID, EMoneyOperateType.DUIHUAN_PROP);
			}
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_UI_MONEY_BUY);
			storeAllResponseBuilder.setIsSuccess(result.isSuccess());
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());

			// 如果在房间里面兑换金币 需要通知 逻辑服更新个人信息
			PlayerServiceImpl.getInstance().notifyLogicToUpdateAccountInfo(session, 5, -findModel.getPrice(),
					findModel.getMoney() + findModel.getSendMoney());

			MongoDBServiceImpl.getInstance().storeBuyLog(account.getAccount_id(), StoreType.COIN, shopID, 1, findModel.getName(),
					findModel.getGameType(), 1);
		}

		else if (type == StoreOpeType.STORE_UI_IOS) {

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<AppShopModel> shopModelList = ShopDict.getInstance().getAppShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.NORMAL_SHOP);
			for (AppShopModel shopModel : shopModelList) {
				StoreGoodResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreGoodResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 城商链接
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_UI_IOS);
			storeAllResponseBuilder.addAllStoreGoodResponseList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());
		}

		else if (type == StoreOpeType.BUY_GOODS) {

			if (!GbCdCtrl.canHandle(session, Opt.BUY_GOODS))
				return;

			int shopID = request.getGoodsId();

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.NORMAL_SHOP);
			ShopModel findModel = null;
			for (ShopModel shop : shopModelList) {
				if (shop.getId() == shopID) {
					findModel = shop;
					break;
				}
			}

			ShopModel shopModel = findModel;
			if (null == shopModel) {
				logger.error("商品 获取失败!!!" + shopID);
				session.send(MessageResponse.getMsgAllResponse("商品失效").build());
				return;
			}
			if (shopModel.isAgentShop()) {
				logger.error("普通商城 出现 代理商品!!!" + shopID);
				session.send(MessageResponse.getMsgAllResponse("商品不存在").build());
				return;
			}

			Date beginTime = shopModel.getBegin_time();
			Date endTime = shopModel.getEnd_time();
			Date now = new Date();
			if (beginTime != null && endTime != null) {
				if (now.before(beginTime) || now.after(endTime)) {
					session.send(MessageResponse.getMsgAllResponse("商品未上架").build());
					return;
				}
			}

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			String gameOrderID = centerRMIServer.getGameOrderID();

			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {
					PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
					String prepayID = ptAPIServiceImpl.getPrepayId(gameOrderID, shopModel.getName(), shopModel.getPrice(),
							account.getAccountModel().getClient_ip(), shopID, account.getGame_id(),account.getLastChannelId());
					logger.info("prepayId==" + prepayID);
					if (StringUtils.isEmpty(prepayID)) {
						logger.error("prepayId 获取失败!!!");
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("prepayId获取失败!").build());
						return;
					}

					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(3);
					storeAllResponseBuilder.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse(prepayID, gameOrderID, account.getGame_id(),account.getLastChannelId()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());

					int uiType = 0;
					int subUiType = 0;
					int opId = 0;
					if(request.hasUiType()){
						uiType = request.getUiType();
						subUiType = request.getSubUiType();
						opId = request.getOpId();
					}
					// 插入临时订单
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(), account.getAccountModel().getIs_agent(),
							ESellType.GAME_PAY_CARD.getId(), shopModel.getId(), shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(),
							0, ESellType.GAME_PAY_CARD.getName(), ESellType.GAME_PAY_CARD.getName(), 0 + "", ESellType.GAME_PAY_CARD.getName(), "",
							game_id, shopModel.getName(),uiType,subUiType,opId,account.getLastChannelId());
					
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());


					MongoDBServiceImpl.getInstance().storeBuyLog(account.getAccount_id(), StoreType.GOLD, shopID, 1, shopModel.getName(),
							shopModel.getGame_type(), shopModel.getShop_type());
				}
			});
		}
		
		else if (type == StoreOpeType.BUY_DIAMOND) {
			if (!GbCdCtrl.canHandle(session, Opt.BUY_GOLD))
				return;

		
			int shopID = request.getGoodsId();

			SdkDiamondShopModel sdkDiamondShopModel = 	SdkDiamondShopDict.getInstance().getSdkDiamondShopMap().get(shopID);
			
			if (null == sdkDiamondShopModel) {
				logger.error("商品 获取失败!!!" + shopID);
				session.send(MessageResponse.getMsgAllResponse("商品失效").build());
				return;
			}

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			String gameOrderID = centerRMIServer.getDiamondUniqueID();

			Global.getWxPayService().execute(new WeiXinPayDiamondRunnable(gameOrderID, sdkDiamondShopModel, shopID, session, account));

			SysParamModel systemParam = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(10000);
			if (systemParam == null || systemParam.getVal1() == 1) {// 默认开启，掉单严重
				Global.schedule(new Runnable() {// 不管冲没冲，去调用一次查询微信
					@Override
					public void run() {
						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						centerRMIServer.payCenterCall(gameOrderID, 2);
					}
				}, 20);
			}
		}

		else if (type == StoreOpeType.BUY_GOODS_IOS) {

			int shopID = request.getGoodsId();

			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {

					SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1014);

					if (sysParamModel1014.getVal1() == 0) {
						session.send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
						return;
					}

					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					String gameOrderID = centerRMIServer.getGameOrderID();

					List<AppShopModel> shopModelList = ShopDict.getInstance().getAppShopModelByGameIdAndShopType(account.getGame_id(),
							ShopModel.NORMAL_SHOP);
					AppShopModel findModel = null;
					for (AppShopModel shop : shopModelList) {
						if (shop.getShop_id() == shopID) {
							findModel = shop;
							break;
						}
					}

					AppShopModel shopModel = findModel;
					if (null == shopModel) {
						logger.error("商品 获取失败!!!" + shopID);
						session.send(MessageResponse.getMsgAllResponse("商品失效").build());
						return;
					}
					if (shopModel.isAgentShop()) {
						logger.error("普通商城 出现 代理商品!!!" + shopID);
						session.send(MessageResponse.getMsgAllResponse("商品不存在").build());
						return;
					}

					PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(StoreOpeType.BUY_GOODS_IOS);
					storeAllResponseBuilder.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse("", gameOrderID, account.getGame_id(),account.getLastChannelId()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());

					// 插入临时订单
					int uiType = 0;
					int subUiType = 0;
					int opId = 0;
					if(request.hasUiType()){
						uiType = request.getUiType();
						subUiType = request.getSubUiType();
						opId = request.getOpId();
					}
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(), account.getAccountModel().getIs_agent(),
							ESellType.IOS_PAY_CARD.getId(), shopModel.getShop_id(), shopModel.getGold(), shopModel.getSend_gold(),
							shopModel.getPrice(), 0, ESellType.IOS_PAY_CARD.getName(), ESellType.IOS_PAY_CARD.getName(), 0 + "",
							ESellType.IOS_PAY_CARD.getName(), "", game_id, shopModel.getName(),uiType,subUiType,opId,account.getLastChannelId());
					
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

					MongoDBServiceImpl.getInstance().storeBuyLog(account.getAccount_id(), StoreType.GOLD, shopID, 1, shopModel.getName(),
							shopModel.getGame_type(), shopModel.getShop_type());
				}
			});
		}
		
		
		
		else if (type == StoreOpeType.BUY_DIAMOND_IOS) {

			int shopID = request.getGoodsId();

			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {


					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					String gameOrderID = centerRMIServer.getDiamondUniqueID();
					
					SdkDiamondShopModel shopModel = 	SdkDiamondShopDict.getInstance().getSdkDiamondShopMap().get(shopID);
					
					if (null == shopModel) {
						logger.error("商品 获取失败!!!" + shopID);
						session.send(MessageResponse.getMsgAllResponse("商品失效").build());
						return;
					}
					
					PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(StoreOpeType.BUY_DIAMOND_IOS);
					storeAllResponseBuilder.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse("", gameOrderID, account.getGame_id(),account.getLastChannelId()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());

					// 插入临时订单
					int uiType = 0;
					int subUiType = 0;
					int opId = 0;
					if(request.hasUiType()){
						uiType = request.getUiType();
						subUiType = request.getSubUiType();
						opId = request.getOpId();
					}
					
					ptAPIServiceImpl.addDiamondLogModel(gameOrderID, account.getAccount_id(), account.getNickName(), account.getAccountModel().getIs_agent(),
							ESellType.DIAMOND_PAY_IOS.getId(), shopModel.getId(), shopModel.getDiamond(), shopModel.getSend_diamond(), shopModel.getPrice(), 0,
							ESellType.DIAMOND_PAY_IOS.getName(), ESellType.DIAMOND_PAY_IOS.getName(), 0 + "", ESellType.DIAMOND_PAY_IOS.getName(), "",
							account.getGame_id(), shopModel.getName(),0,0,0,0);
					
					
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

				}
			});
		}


		else if (type == StoreOpeType.PAY_BACK) {
			String gameOrderID = request.getGameOrderId();
			if (StringUtils.isEmpty(gameOrderID)) {
				logger.error("客户端 发送的  gameOrderID 是空的!!!" + gameOrderID);
				return;
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.payCenterCall(gameOrderID, request.getRechargeType());
		}
		
		
		else if (type == StoreOpeType.PAY_IOS_DIAMOND) {
			if (!GbCdCtrl.canHandle(session, Opt.PAY_IOS))
				return;
			Global.getAppStoreService().execute(new Runnable() {
				@Override
				public void run() {
					String gameOrderID = request.getGameOrderId();
					String receipt = request.getReceipt();
					int rechargeType = request.getRechargeType();
					if (StringUtils.isEmpty(gameOrderID) || StringUtils.isEmpty(receipt)) {
						logger.error("客户端 发送的  PAY_IOS_DIAMOND gameOrderID or receipt是空的!!!" + receipt + " gameOrderID=" + gameOrderID + " accountID="
								+ account.getAccount_id());
						return;
					}
					boolean isSuccess = false;
					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					String transactionId = "";
					if (request.hasTransactionId()) {
						transactionId = request.getTransactionId();
						logger.warn("transactionId" + transactionId + " gameOrderID=" + gameOrderID + " accountID=" + account.getAccount_id());
						isSuccess = centerRMIServer.payCenterCallIOSByTransactionId(account.getAccount_id(), gameOrderID, receipt, transactionId,
								rechargeType);
					} else {
						isSuccess = centerRMIServer.payCenterCallIOS(account.getAccount_id(), gameOrderID, receipt, rechargeType);
					}

					MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
					Query query = new Query();
					query.addCriteria(Criteria.where("orderID").is(gameOrderID));

					DiamondLogModel addCardLog = mongoDBService.getMongoTemplate().findOne(query, DiamondLogModel.class);
					if (addCardLog == null || addCardLog.getOrderStatus() != 0) {
						isSuccess = false;
					} else {
						isSuccess = true;
					}

					if (StringUtils.isNotEmpty(transactionId)) {
						Query appquery = new Query();
						appquery.addCriteria(Criteria.where("centerOrderID").is(transactionId));
						addCardLog = mongoDBService.getMongoTemplate().findOne(query, DiamondLogModel.class);
						if (addCardLog == null || addCardLog.getOrderStatus() != 0) {
							isSuccess = false;
						} else {
							isSuccess = true;
						}
					}

					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(StoreOpeType.PAY_IOS_DIAMOND);
					storeAllResponseBuilder.setIsSuccess(isSuccess);
					storeAllResponseBuilder.setReceipt(receipt);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}
			});
		}

		else if (type == StoreOpeType.PAY_IOS) {
			if (!GbCdCtrl.canHandle(session, Opt.PAY_IOS))
				return;
			Global.getAppStoreService().execute(new Runnable() {
				@Override
				public void run() {
					String gameOrderID = request.getGameOrderId();
					String receipt = request.getReceipt();
					int rechargeType = request.getRechargeType();
					if (StringUtils.isEmpty(gameOrderID) || StringUtils.isEmpty(receipt)) {
						logger.error("客户端 发送的  gameOrderID or receipt是空的!!!" + receipt + " gameOrderID=" + gameOrderID + " accountID="
								+ account.getAccount_id());
						return;
					}
					boolean isSuccess = false;
					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					String transactionId = "";
					if (request.hasTransactionId()) {
						transactionId = request.getTransactionId();
						logger.warn("transactionId" + transactionId + " gameOrderID=" + gameOrderID + " accountID=" + account.getAccount_id());
						isSuccess = centerRMIServer.payCenterCallIOSByTransactionId(account.getAccount_id(), gameOrderID, receipt, transactionId,
								rechargeType);
					} else {
						isSuccess = centerRMIServer.payCenterCallIOS(account.getAccount_id(), gameOrderID, receipt, rechargeType);
					}

					MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
					Query query = new Query();
					query.addCriteria(Criteria.where("orderID").is(gameOrderID));

					AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
					if (addCardLog == null || addCardLog.getOrderStatus() != 0) {
						isSuccess = false;
					} else {
						isSuccess = true;
					}

					if (StringUtils.isNotEmpty(transactionId)) {
						Query appquery = new Query();
						appquery.addCriteria(Criteria.where("centerOrderID").is(transactionId));
						addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
						if (addCardLog == null || addCardLog.getOrderStatus() != 0) {
							isSuccess = false;
						} else {
							isSuccess = true;
						}
					}

					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(StoreOpeType.PAY_IOS);
					storeAllResponseBuilder.setIsSuccess(isSuccess);
					storeAllResponseBuilder.setReceipt(receipt);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}
			});
		} else if (type == StoreOpeType.STORE_ITEM_EXCHANGE) { // 实物兑换商品列表
			List<StoreExchangeItemResponse> storeExchangeItemList = Lists.newArrayList();
			Map<Integer, ItemExchangeModel> modelMap = ItemExchangeDict.getInstance().getItemExchangeMap();
			for (ItemExchangeModel model : modelMap.values()) {
				Date beginShowTime = model.getBeginShowTime();
				Date endShowTime = model.getEndShowTime();
				Date now = new Date();
				if (beginShowTime != null && endShowTime != null) {
					if (now.before(beginShowTime) || now.after(endShowTime))
						continue;
				}
				StoreExchangeItemResponse.Builder storeExchangeItemResponseBuilder = MessageResponse.getStoreExchangeItemResponse(model);
				storeExchangeItemList.add(storeExchangeItemResponseBuilder.build());
			}

			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_ITEM_EXCHANGE);
			storeAllResponseBuilder.addAllItemList(storeExchangeItemList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());
		} else if (type == StoreOpeType.STORE_WELFARE_EXCHANGE) { // 福卡兑换商品列表
			List<StoreWelfareCardExchangeResponse> storeWelfareExchangeItemList = Lists.newArrayList();
			Map<Integer, WelfareExchangeModel> modelMap = WelfareExchangeDict.getInstance().getWelfareExchangeMap();
			for (WelfareExchangeModel model : modelMap.values()) {
//				Date beginShowTime = model.getBeginShowTime();
//				Date endShowTime = model.getEndShowTime();
//				Date now = new Date();
//				if (beginShowTime != null && endShowTime != null) {
//					if (now.before(beginShowTime) || now.after(endShowTime))
//						continue;
//				}
				// 商品类型下架
				if(!WelfareGoodsTypeDict.getInstance().isGoodsTypeOnSale(model.getGoods_type())) {
					continue;
				}
				//商品下架
				if (model.getOnline() == 0) {
					continue;
				}
				StoreWelfareCardExchangeResponse.Builder storeWelfareExchangeItemResponseBuilder = MessageResponse
						.getStoreWelfareExchangeResponse(model);
				storeWelfareExchangeItemList.add(storeWelfareExchangeItemResponseBuilder.build());
			}

			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_WELFARE_EXCHANGE);
			storeAllResponseBuilder.addAllWelfareItemList(storeWelfareExchangeItemList);

			Map<Integer, WelfareGoodsTypeModel> welfareGoodsTypeMap = WelfareGoodsTypeDict.getInstance().getWelfareGoodsTypeMap();
			for (WelfareGoodsTypeModel model : welfareGoodsTypeMap.values()) {
				if (model.getOnline() == 1) {
					StoreWelfareGoodsType.Builder goodsTypeBuilder = StoreWelfareGoodsType.newBuilder();
					goodsTypeBuilder.setGoodsType(model.getId());
					goodsTypeBuilder.setGoodsTypeDesc(model.getType_desc());
					goodsTypeBuilder.setOrder(model.getDis_order());
					storeAllResponseBuilder.addWelfareGoodsType(goodsTypeBuilder);
				}
			}

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());
		} else if (type == StoreOpeType.STORE_DIAMOND) { 
			// 钻石商城列表
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.STORE_DIAMOND);
			Map<Integer, SdkDiamondShopModel> modelMap = SdkDiamondShopDict.getInstance().getSdkDiamondShopMap();
			for (SdkDiamondShopModel model : modelMap.values()) {
				StoreDiamondResponse.Builder builder = StoreDiamondResponse.newBuilder();
				builder.setDiamond(model.getDiamond());
				builder.setIcon(model.getIcon());
				builder.setId(model.getId());
				builder.setName(model.getName());
				builder.setPrice(model.getPrice());
				builder.setSendDiamond(model.getSend_diamond());
				builder.setSendTag(model.getRemark());
				builder.setType(model.getPlatform());
				storeAllResponseBuilder.addStoreDiamondList(builder);
			}

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());
			session.send(responseBuilder.build());
		}
	}
}
