package com.cai.handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ProxyGoldLogModel;
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
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;

import javolution.util.FastMap;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AddGoldCardHistoryResponse;
import protobuf.clazz.Protocol.GoldCardAllResponse;
import protobuf.clazz.Protocol.GoldCardRequest;
import protobuf.clazz.Protocol.PayBuyResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.StoreAllResponse;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 房卡相关的
 * 
 * @author run
 *
 */
public class GoldCardHandler extends ClientHandler<GoldCardRequest> {

	/**
	 * 修改密码
	 */
	private static final int MODIFY_PASSWD = 1;

	/**
	 * 赠送他人房卡
	 */
	private static final int GIVE_OTHER_CARD = 2;

	/**
	 * 转卡日志
	 */
	private static final int GIVE_CARD_LOG = 3;

	/**
	 * 代理商城
	 */
	private static final int STORE_UI = 4;

	/**
	 * 购买商品
	 */
	private static final int BUY_GOLD = 5;

	/**
	 * 支付成功 客户端返回订单号
	 */
	private static final int PAY_BACK = 6;

	@Override
	public void onRequest() throws Exception {

		if (!request.hasType())
			return;

		int type = request.getType();

		// 操作频率控制
		if (!session.isCanRequest("GoldCardHandler_" + type, 300L)) {
			return;
		}

		if (session.getAccount() == null)
			return;

		Account account = session.getAccount();

		AccountModel accountModel = account.getAccountModel();
		if (accountModel.getIs_agent() != 1) {
			send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
			return;
		}

		if (type == MODIFY_PASSWD) {

			if (!request.hasNewPasswd())
				return;

			String _old_pw = request.getOldPasswd();
			String _new_pw = request.getNewPasswd();

			if (accountModel.getPassword() != null && !"".equals(accountModel.getPassword().trim())) {
				if (_old_pw == null || "".equals(_old_pw)
						|| !DigestUtils.md5Hex(_old_pw).equals(accountModel.getPassword())) {
					send(MessageResponse.getMsgAllResponse("旧密码不正确!").build());
					return;
				}
			}
			if (_new_pw == null || "".equals(_new_pw.trim())) {
				send(MessageResponse.getMsgAllResponse("新密码不能为空!").build());
				return;
			}

			if (_new_pw.length() < 6 || _new_pw.length() > 10) {
				send(MessageResponse.getMsgAllResponse("新密码长度必须大于等6位小于等于10位!").build());
				return;
			}

			// 重置密码
			accountModel.setPassword(DigestUtils.md5Hex(_new_pw));

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountModelResponseBuilder.setPassword(accountModel.getPassword());
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicCenter);

			// send(MessageResponse.getMsgAllResponse("修改成功!").build());

			// 返回
			GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
			goldCardAllResponseBuilder.setType(3);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.GOLD_CARD);
			responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
			send(responseBuilder.build());

		}

		else if (type == GIVE_OTHER_CARD) {

			if (!request.hasNum())
				return;

			int num = request.getNum();
			if (num < 0 || num > 99999999) {
				send(MessageResponse.getMsgAllResponse("数量必须大于0小于99999999").build());
				return;
			}

			if (!request.hasOldPasswd()) {
				send(MessageResponse.getMsgAllResponse("密码不能为空").build());
				return;
			}

			if (accountModel.getPassword() == null || "".equals(accountModel.getPassword().trim())) {
				send(MessageResponse.getMsgAllResponse("请先修改密码").build());
				return;
			}

			if (!DigestUtils.md5Hex(request.getOldPasswd()).equals(accountModel.getPassword())) {
				send(MessageResponse.getMsgAllResponse("密码不正确").build());
				return;
			}

			if (!request.hasTargetAccountId()) {
				return;
			}

			if (request.getTargetAccountId() == account.getAccount_id()) {
				send(MessageResponse.getMsgAllResponse("不能赠送给自己").build());
				return;
			}

			// 判断对方是否存在
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account targetAccount = centerRMIServer.getAccount(request.getTargetAccountId());
			if (targetAccount == null) {
				send(MessageResponse.getMsgAllResponse("对方ID不存在").build());
				return;
			}

			// 判断数量是否足够
			if (account.getAccountModel().getGold() < num) {
				send(MessageResponse.getMsgAllResponse("房卡数量不足!").build());
				return;
			}

			if (!request.hasVerify())
				return;

			// 是否确认
			boolean verify = request.getVerify();
			if (!verify) {

				GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
				goldCardAllResponseBuilder.setType(1);
				goldCardAllResponseBuilder.setTargetAccountId(targetAccount.getAccount_id());
				goldCardAllResponseBuilder.setTargetNickName(targetAccount.getNickName());

				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.GOLD_CARD);
				responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
				send(responseBuilder.build());

			} else {
				AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account.getAccount_id(), -num,false, "游戏内转卡,转给account_id:" + targetAccount.getAccount_id(),EGoldOperateType.PROXY_GIVE);
				if (!addGoldResultModel.isSuccess()) {
					send(MessageResponse.getMsgAllResponse("房卡数量不足!").build());
					return;
				}

				// 日志
				String nick_name = "--";
				if (targetAccount.getAccountWeixinModel() != null) {
					String name = targetAccount.getAccountWeixinModel().getNickname();
					if (name != null && !"".equals(nick_name)) {
						nick_name = name;
					}
				}
				int target_proxy_account = targetAccount.getAccountModel().getIs_agent();
				
				MongoDBServiceImpl.getInstance().proxyGoldLog(account.getAccount_id(), targetAccount.getAccount_id(),nick_name, num, account.getLast_login_ip(), 0,target_proxy_account);

				centerRMIServer.addAccountGold(targetAccount.getAccount_id(), num, false,"游戏内转卡,接收account_id:" + account.getAccount_id(),EGoldOperateType.PROXY_GIVE);
				// send(MessageResponse.getMsgAllResponse("转卡成功!").build());

				GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
				goldCardAllResponseBuilder.setType(4);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.GOLD_CARD);
				responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
				send(responseBuilder.build());

				return;
			}

		}

		else if (type == GIVE_CARD_LOG) {

			if (!request.hasCurPage())
				return;

			Long target_account_id = null;
			if (request.hasTargetAccountId()) {
				target_account_id = request.getTargetAccountId();
			}

			// 当前页
			int cur_page = request.getCurPage();
			int totalSize = MongoDBServiceImpl.getInstance().getProxyGoldLogModelCount(account.getAccount_id(),
					target_account_id);
			Page page = new Page(cur_page, 6, totalSize);
			List<ProxyGoldLogModel> proxyGoldLogModelList = MongoDBServiceImpl.getInstance()
					.getProxyGoldLogModelList(page, account.getAccount_id(), target_account_id);

			List<AddGoldCardHistoryResponse> addGoldCardHistoryResponseList = Lists.newArrayList();
			for (ProxyGoldLogModel m : proxyGoldLogModelList) {
				addGoldCardHistoryResponseList.add(MessageResponse.getAddGoldCardHistoryResponse(m).build());
			}

			GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
			goldCardAllResponseBuilder.setType(2);
			goldCardAllResponseBuilder.addAllAddGoldCardHistoryResponseList(addGoldCardHistoryResponseList);
			goldCardAllResponseBuilder.setCurPage(page.getRealPage());
			goldCardAllResponseBuilder.setPageSize(page.getPageSize());
			goldCardAllResponseBuilder.setTotalPage(page.getTotalPage());
			goldCardAllResponseBuilder.setTotalSize(page.getTotalSize());
			if (target_account_id != null) {
				goldCardAllResponseBuilder.setTargetAccountId(target_account_id);
			}

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.GOLD_CARD);
			responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
			send(responseBuilder.build());

		}

		else if (type == STORE_UI) {
			SysParamModel sysParamModel1015 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
					.get(1015);

			if (sysParamModel1015.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("代理商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(),
					ShopModel.AGENT_SHOP);
			for (ShopModel shopModel : shopModelList) {
				StoreGoodResponse.Builder storeGoodResponseBuilder = MessageResponse.getStoreGoodResponse(shopModel);
				storeGoodResponseList.add(storeGoodResponseBuilder.build());
			}

			// 代理商成列表
			GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
			goldCardAllResponseBuilder.setType(5);
			goldCardAllResponseBuilder.addAllStoreGoodResponseList(storeGoodResponseList);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.GOLD_CARD);
			responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
			send(responseBuilder.build());
		}

		else if (type == BUY_GOLD) {
			int shopID = request.getGoodsId();
		
			SysParamModel sysParamModel1015 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
					.get(1015);

			if (sysParamModel1015.getVal1() == 0) {
				send(MessageResponse.getMsgAllResponse("代理商城暂未开放!").build());
				return;
			}

			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(),
					ShopModel.AGENT_SHOP);
		    ShopModel findModel = null;
			for(ShopModel shop:shopModelList) {
				if(shop.getId()==shopID) {
					findModel=shop;break;
				}
			}
			ShopModel shopModel=findModel;
			if (null == shopModel) {
				logger.error("商品 获取失败!!!" + shopID);
				send(MessageResponse.getMsgAllResponse("商品失效").build());
				return;
			}

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			String gameOrderID = centerRMIServer.getGameOrderID();

			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {

					if(session==null) {
						logger.error("session 为null!!!");
						return;
					}
					PtAPIServiceImpl ptAPIServiceImpl = PtAPIServiceImpl.getInstance();
					String prepayID = ptAPIServiceImpl.getPrepayId(gameOrderID, shopModel.getName(),
							shopModel.getPrice(), account.getLast_login_ip(), shopID);
					if (StringUtils.isEmpty(prepayID)) {
						logger.error("prepayId 获取失败!!!");
						PlayerServiceImpl.getInstance().sendAccountMsg(session,MessageResponse.getMsgAllResponse("prepayId获取失败!").build());
						return;
					}

					GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
					goldCardAllResponseBuilder.setType(6);
					goldCardAllResponseBuilder
							.setPayBuyResponse(ptAPIServiceImpl.getPayBuyResponse(prepayID, gameOrderID));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.GOLD_CARD);
					responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
					
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

					// 插入临时订单
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(),
							account.getAccountModel().getIs_agent(), ESellType.GAME_PAY_CARD.getId(), shopModel.getId(),
							shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(), 0,
							ESellType.GAME_PAY_CARD.getName(), ESellType.GAME_PAY_CARD.getName(), 0 + "",
							ESellType.GAME_PAY_CARD.getName(), "");

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
