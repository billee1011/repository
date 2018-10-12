package com.cai.handler;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AddCardLog;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AppShopModel;
import com.cai.common.domain.MoneyShopModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.dictionary.MoneyShopDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.net.core.ClientHandler;
import com.cai.service.ClientServiceImpl;
import com.cai.service.MongoDBService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.StoreAllResponse;
import protobuf.clazz.Protocol.StoreGoodMoneyResponse;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.clazz.Protocol.StoreRequest;
import protobuf.clazz.Protocol.Request.RequestType;

/**
 * 商城
 * 
 * @author run
 *
 */
public class TestStoreHandler extends ClientHandler<StoreRequest> {

	/**
	 * 查询功能状态
	 */
	private static final int QUERY_FUNCTION_STATU = 1;

	/**
	 * 商城界面
	 */
	private static final int STORE_UI = 2;

	/**
	 * 商城界面
	 */
	private static final int STORE_UI_IOS = 7;

	/**
	 * 购买
	 */
	private static final int BUY_GOODS = 3;

	/**
	 * 支付成功 客户端返回订单号
	 */
	private static final int PAY_BACK = 4;

	/**
	 * 苹果支付
	 */
	private static final int PAY_IOS = 5;

	/**
	 * ios购买
	 */
	private static final int BUY_GOODS_IOS = 6;

	/**
	 * 金币商城界面
	 */
	private static final int STORE_UI_MONEY = 8;
	
	/**
	 * 金币商城购买
	 */
	private static final int STORE_UI_MONEY_BUY = 9;

	@Override
	public void onRequest() throws Exception {

		if (!request.hasType())
			return;
		int type = request.getType();

		// 操作频率控制
		if (!session.isCanRequest("LoginHandler_" + type, 300L)) {
			return;
		}

		Account account = session.getAccount();
		if (account == null)
			return;

		final int game_id = account.getGame_id();

		if (type == QUERY_FUNCTION_STATU) {

			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(1013);
			if (sysParamModel == null) {
				logger.error("获取不到相关的系统参数");
			}

			// 提示
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(1);
			storeAllResponseBuilder.setMsg(sysParamModel.getStr1());
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());
			send(responseBuilder.build());

		}

		else if (type == STORE_UI) {

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(),
					ShopModel.NORMAL_SHOP);
			for (ShopModel shopModel : shopModelList) {
				StoreGoodResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreGoodResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 城商链接
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(2);
			storeAllResponseBuilder.addAllStoreGoodResponseList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());
			send(responseBuilder.build());
		}

		else if (type == STORE_UI_MONEY) {
			// 商品列表
			List<StoreGoodMoneyResponse> storeGoodResponseList = Lists.newArrayList();
			List<MoneyShopModel> shopModelList = MoneyShopDict.getInstance()
					.getShopModelByGameIdAndShopType(account.getGame_id());
			for (MoneyShopModel shopModel : shopModelList) {
				StoreGoodMoneyResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreMoneyResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 城商链接
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(STORE_UI_MONEY);
			storeAllResponseBuilder.addAllMoneyList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());
			send(responseBuilder.build());

		}
		
		
		else if(type ==  STORE_UI_MONEY_BUY) {
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
				send(MessageResponse.getMsgAllResponse("商品失效").build());
				return;
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			AddGoldResultModel result = centerRMIServer.addAccountGold(account.getAccount_id(),  -findModel.getPrice(), false,
					"兑换金币,shopID:" + shopID, EGoldOperateType.MONEY_DUIHUAN);
			if(result.isSuccess()) {
				centerRMIServer.addAccountMoney(account.getAccount_id(), findModel.getMoney()+findModel.getSendMoney(), false, "兑换金币,shopID:" + shopID, EMoneyOperateType.DUIHUAN_PROP);
			}
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(STORE_UI_MONEY_BUY);
			storeAllResponseBuilder.setIsSuccess(result.isSuccess());
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());
			send(responseBuilder.build());
			
			
			//如果在房间里面兑换金币    需要通知 逻辑服更新个人信息
			if(account.getRoom_id() != 0  && result.isSuccess()){
				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
				logicRoomRequestBuilder.setType(5);
				logicRoomRequestBuilder.setRoomId(account.getRoom_id());
				logicRoomRequestBuilder.setAddGold(-findModel.getPrice());
				logicRoomRequestBuilder.setAddMoney(findModel.getMoney()+findModel.getSendMoney());
				logicRoomRequestBuilder
						.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
				if (!flag) {
					logger.error("在房间里面兑换金币   通知逻辑服失败!!!");
					return;
				}
			}
		}

		else if (type == STORE_UI_IOS) {

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<AppShopModel> shopModelList = ShopDict.getInstance()
					.getAppShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.NORMAL_SHOP);
			for (AppShopModel shopModel : shopModelList) {
				StoreGoodResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreGoodResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 城商链接
			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(STORE_UI_IOS);
			storeAllResponseBuilder.addAllStoreGoodResponseList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());
			send(responseBuilder.build());
		}

		else if (type == BUY_GOODS) {
			int shopID = request.getGoodsId();

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(),
					ShopModel.NORMAL_SHOP);
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
				send(MessageResponse.getMsgAllResponse("商品失效").build());
				return;
			}
			if (shopModel.isAgentShop()) {
				logger.error("普通商城 出现 代理商品!!!" + shopID);
				send(MessageResponse.getMsgAllResponse("商品不存在").build());
				return;
			}

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			String gameOrderID = centerRMIServer.getGameOrderID();

			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {
					if (session == null) {
						logger.error("session 为null!!!");
						return;
					}
					PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
					String prepayID = ptAPIServiceImpl.getPrepayId(gameOrderID, shopModel.getName(),
							shopModel.getPrice(), account.getAccountModel().getClient_ip(), shopID,
							account.getGame_id());
					if (StringUtils.isEmpty(prepayID)) {
						logger.error("prepayId 获取失败!!!");
						PlayerServiceImpl.getInstance().sendAccountMsg(session,
								MessageResponse.getMsgAllResponse("prepayId获取失败!").build());
						return;
					}

					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(3);
					storeAllResponseBuilder.setPayBuyResponse(
							ptAPIServiceImpl.getPayBuyResponse(prepayID, gameOrderID, account.getGame_id()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());

					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

					// 插入临时订单
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(),
							account.getAccountModel().getIs_agent(), ESellType.GAME_PAY_CARD.getId(), shopModel.getId(),
							shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(), 0,
							ESellType.GAME_PAY_CARD.getName(), ESellType.GAME_PAY_CARD.getName(), 0 + "",
							ESellType.GAME_PAY_CARD.getName(), "", game_id, shopModel.getName());

				}
			});

		}

		else if (type == BUY_GOODS_IOS) {

			int shopID = request.getGoodsId();
			
			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {
					if (session == null) {
						logger.error("session 为null!!!");
						return;
					}
					
					SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
							.get(1014);

					if (sysParamModel1014.getVal1() == 0) {
						send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
						return;
					}

					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					String gameOrderID = centerRMIServer.getGameOrderID();

					List<AppShopModel> shopModelList = ShopDict.getInstance()
							.getAppShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.NORMAL_SHOP);
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
						send(MessageResponse.getMsgAllResponse("商品失效").build());
						return;
					}
					if (shopModel.isAgentShop()) {
						logger.error("普通商城 出现 代理商品!!!" + shopID);
						send(MessageResponse.getMsgAllResponse("商品不存在").build());
						return;
					}

					PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(BUY_GOODS_IOS);
					storeAllResponseBuilder
							.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse("", gameOrderID, account.getGame_id()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());

					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

					// 插入临时订单
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(),
							account.getAccountModel().getIs_agent(), ESellType.GAME_PAY_CARD.getId(), shopModel.getShop_id(),
							shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(), 0,
							ESellType.GAME_PAY_CARD.getName(), ESellType.GAME_PAY_CARD.getName(), 0 + "",
							ESellType.GAME_PAY_CARD.getName(), "", game_id, shopModel.getName());
				}
				
			});
		}

		else if (type == PAY_BACK) {
			String gameOrderID = request.getGameOrderId();
			if (StringUtils.isEmpty(gameOrderID)) {
				logger.error("客户端 发送的  gameOrderID 是空的!!!"+gameOrderID);
				return;
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.payCenterCall(gameOrderID);
		}

		else if (type == PAY_IOS) {
			Global.getWxPayService().execute(new Runnable() {
				@Override
				public void run() {
					String gameOrderID = request.getGameOrderId();
					String receipt = request.getReceipt();
					if (StringUtils.isEmpty(gameOrderID) || StringUtils.isEmpty(receipt)) {
						logger.error("客户端 发送的  gameOrderID or receipt是空的!!!"+receipt+" gameOrderID="+gameOrderID+" accountID="+account.getAccount_id());
						return;
					}
					
					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					boolean isSuccess = centerRMIServer.payCenterCallIOS(account.getAccount_id(),gameOrderID, receipt);

					MongoDBService mongoDBService = SpringService.getBean(MongoDBService.class);
					Query query = new Query();
					query.addCriteria(Criteria.where("orderID").is(gameOrderID));

					AddCardLog addCardLog = mongoDBService.getMongoTemplate().findOne(query, AddCardLog.class);
					if (addCardLog == null || addCardLog.getOrderStatus() != 0) {
						isSuccess = false;
					} else {
						isSuccess = true;
					}

					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(PAY_IOS);
					storeAllResponseBuilder.setIsSuccess(isSuccess);
					storeAllResponseBuilder.setReceipt(receipt);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());
//					send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}
				
			});
		
		}

	}

}
