package com.cai.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.Signature;
import com.cai.common.util.SpringService;
import com.cai.common.util.XMLParser;
import com.cai.core.Global;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.net.core.ClientHandler;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.PayBuyResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.StoreAllResponse;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.clazz.Protocol.StoreRequest;

/**
 * 商城
 * 
 * @author run
 *
 */
public class StoreHandler extends ClientHandler<StoreRequest> {

	public static final Logger logger = LoggerFactory.getLogger(StoreHandler.class);
	/**
	 * 查询功能状态
	 */
	private static final int QUERY_FUNCTION_STATU = 1;

	/**
	 * 商城界面
	 */
	private static final int STORE_UI = 2;

	/**
	 * 购买
	 */
	private static final int BUY_GOODS = 3;

	/**
	 * 支付成功 客户端返回订单号
	 */
	private static final int PAY_BACK = 4;

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

			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1013);
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

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.NORMAL_SHOP);
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

		else if (type == BUY_GOODS) {
			int shopID = request.getGoodsId();

			SysParamModel sysParamModel1014 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1014);

			if (sysParamModel1014.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("商城暂未开放!").build());
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
					String prepayID = ptAPIServiceImpl.getPrepayId(gameOrderID, shopModel.getName(), shopModel.getPrice(), account.getAccountModel().getClient_ip(), shopID,account.getGame_id());
					if (StringUtils.isEmpty(prepayID)) {
						logger.error("prepayId 获取失败!!!");
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("prepayId获取失败!").build());
						return;
					}

					StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
					storeAllResponseBuilder.setType(3);
					storeAllResponseBuilder.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse(prepayID, gameOrderID,account.getGame_id()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.Store);
					responseBuilder.setExtension(Protocol.storeAllResponse, storeAllResponseBuilder.build());

					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

					// 插入临时订单
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(), account.getAccountModel().getIs_agent(), ESellType.GAME_PAY_CARD.getId(), shopModel.getId(), shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(), 0, ESellType.GAME_PAY_CARD.getName(),
							ESellType.GAME_PAY_CARD.getName(), 0 + "", ESellType.GAME_PAY_CARD.getName(), "",game_id,shopModel.getName());

				}
			});

		}

		else if (type == PAY_BACK) {
			String gameOrderID = request.getGameOrderId();
			if (StringUtils.isEmpty(gameOrderID)) {
				logger.error("客户端 发送的  gameOrderID 是空的!!!");
				return;
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.payCenterCall(gameOrderID);
		}

	}

}
