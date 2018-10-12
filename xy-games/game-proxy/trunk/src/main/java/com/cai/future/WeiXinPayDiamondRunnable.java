/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future;

import static protobuf.clazz.Protocol.storeAllResponse;

/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */


import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.sdk.DiamondLogModel;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.type.StoreOpeType;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GoldCardAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.StoreAllResponse;
import protobuf.clazz.Protocol.Response.ResponseType;

/**
 * 
 *
 * @author DIY date: 2017年12月16日 下午3:32:11 <br/>
 */
public class WeiXinPayDiamondRunnable implements Runnable {
	
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private String gameOrderID;

	private SdkDiamondShopModel shopModel;

	private int shopID;

	private C2SSession session;

	private Account account;

	private long createTime;

	/**
	 * @param gameOrderID
	 * @param shopModel
	 * @param shopID
	 * @param session
	 * @param account
	 */
	public WeiXinPayDiamondRunnable(String gameOrderID, SdkDiamondShopModel shopModel, int shopID, C2SSession session, Account account) {
		this.gameOrderID = gameOrderID;
		this.shopModel = shopModel;
		this.shopID = shopID;
		this.session = session;
		this.account = account;

		this.createTime = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		long now = System.currentTimeMillis();
		long pass = now - createTime;
		if (pass > 5000) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("支付请求超时,请重试").build());
			logger.error("支付请求间隔超时大于5秒........." + pass);
			return;
		}
		try {
			PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
			String prepayID = ptAPIServiceImpl.getPrepayId(gameOrderID, shopModel.getName(), shopModel.getPrice(),
					account.getAccountModel().getClient_ip(), shopID, account.getGame_id(), account.getLastChannelId(),DiamondLogModel.DIAMONDATTACH);
			if (StringUtils.isEmpty(prepayID)) {
				logger.error("prepayId 获取失败!!!" + account.getGame_id());
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("prepayId获取失败!").build());
				return;
			}

			StoreAllResponse.Builder storeAllResponseBuilder = StoreAllResponse.newBuilder();
			storeAllResponseBuilder.setType(StoreOpeType.BUY_DIAMOND);
			storeAllResponseBuilder.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse(prepayID, gameOrderID, account.getGame_id(),account.getLastChannelId()));

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.Store);
			responseBuilder.setExtension(storeAllResponse, storeAllResponseBuilder.build());

			// 插入临时订单
//			ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(), account.getAccountModel().getIs_agent(),
//					ESellType.GAME_PAY_CARD.getId(), shopModel.getId(), shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(), 0,
//					ESellType.GAME_PAY_CARD.getName(), ESellType.GAME_PAY_CARD.getName(), 0 + "", ESellType.GAME_PAY_CARD.getName(), "",
//					account.getGame_id(), shopModel.getName());
			
			
			ptAPIServiceImpl.addDiamondLogModel(gameOrderID, account.getAccount_id(), account.getNickName(), account.getAccountModel().getIs_agent(),
					ESellType.DIAMOND_PAY_AZ.getId(), shopModel.getId(), shopModel.getDiamond(), shopModel.getSend_diamond(), shopModel.getPrice(), 0,
					ESellType.DIAMOND_PAY_AZ.getName(), ESellType.DIAMOND_PAY_AZ.getName(), 0 + "", ESellType.DIAMOND_PAY_AZ.getName(), "",
					account.getGame_id(), shopModel.getName(),0,0,0,0);
			
			PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("#### 微信支付出现异常~~~~~~~exception:{}", e);
		}

	}

}
