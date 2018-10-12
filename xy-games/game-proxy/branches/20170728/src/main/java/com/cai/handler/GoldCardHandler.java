/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.Date;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESellType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.Session;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AddGoldCardHistoryResponse;
import protobuf.clazz.Protocol.GoldCardAllResponse;
import protobuf.clazz.Protocol.GoldCardRequest;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;

/**
 *
 * @author wu_hc
 */
@ICmd(code = RequestType.GOLD_CARD_VALUE, exName = "goldCardRequest")
public class GoldCardHandler extends IClientHandler<GoldCardRequest> {

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

	/**
	 * 更新推荐代理人id
	 */
	private static final int UPDATE_RECOMMEND_ID = 7;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(GoldCardRequest request, Request topRequest, Session session) throws Exception {
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

		if (type == MODIFY_PASSWD) {

			if (accountModel.getIs_agent() < 1) {
				session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
				return;
			}

			if (!request.hasNewPasswd())
				return;

			String _old_pw = request.getOldPasswd();
			String _new_pw = request.getNewPasswd();

			if (accountModel.getPassword() != null && !"".equals(accountModel.getPassword().trim())) {
				if (_old_pw == null || "".equals(_old_pw)
						|| !DigestUtils.md5Hex(_old_pw).equals(accountModel.getPassword())) {
					session.send(MessageResponse.getMsgAllResponse("旧密码不正确!").build());
					return;
				}
			}
			if (_new_pw == null || "".equals(_new_pw.trim())) {
				session.send(MessageResponse.getMsgAllResponse("新密码不能为空!").build());
				return;
			}

			if (_new_pw.length() < 6 || _new_pw.length() > 10) {
				session.send(MessageResponse.getMsgAllResponse("新密码长度必须大于等6位小于等于10位!").build());
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
			session.send(responseBuilder.build());

		}

		else if (type == GIVE_OTHER_CARD) {

			if (accountModel.getIs_agent() < 1) {
				session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
				return;
			}

			if (!request.hasNum())
				return;

			int num = request.getNum();
			if (num < 0 || num > 99999999) {
				session.send(MessageResponse.getMsgAllResponse("数量必须大于0小于99999999").build());
				return;
			}

			if (!request.hasOldPasswd()) {
				session.send(MessageResponse.getMsgAllResponse("密码不能为空").build());
				return;
			}

			if (accountModel.getPassword() == null || "".equals(accountModel.getPassword().trim())) {
				session.send(MessageResponse.getMsgAllResponse("请先修改密码").build());
				return;
			}

			if (!DigestUtils.md5Hex(request.getOldPasswd()).equals(accountModel.getPassword())) {
				session.send(MessageResponse.getMsgAllResponse("密码不正确").build());
				return;
			}

			if (!request.hasTargetAccountId()) {
				return;
			}

			if (request.getTargetAccountId() == account.getAccount_id()) {
				session.send(MessageResponse.getMsgAllResponse("不能赠送给自己").build());
				return;
			}

			// 判断对方是否存在
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account targetAccount = centerRMIServer.getAccount(request.getTargetAccountId());
			if (targetAccount == null) {
				session.send(MessageResponse.getMsgAllResponse("对方ID不存在").build());
				return;
			}

			if (account.getGame_id() == GameConstants.GAME_ID_FLS_LX) {
				if (targetAccount.getAccountModel().getProxy_level() > 0) {
					if (targetAccount.getAccountModel().getUp_proxy() != account.getAccount_id()) {
						session.send(MessageResponse.getMsgAllResponse("不能转卡给非自己的下级代理").build());
						return;
					}
				}

				SysParamModel sysParamModel1109 = SysParamDict.getInstance()
						.getSysParamModelDictionaryByGameId(account.getGame_id()).get(1109);
				if (sysParamModel1109 != null && sysParamModel1109.getVal2() == 0) {// 控制非3级代理是否可以转给普通玩家
					if (account.getAccountModel().getProxy_level() >= 0
							&& account.getAccountModel().getProxy_level() <= 2
							&& targetAccount.getAccountModel().getUp_proxy() == 0) {
						session.send(MessageResponse.getMsgAllResponse("不能转卡给普通玩家").build());
						return;
					}
				}
			}

			// 判断数量是否足够
			if (account.getAccountModel().getGold() < num) {
				session.send(MessageResponse.getMsgAllResponse("房卡数量不足!").build());
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
				session.send(responseBuilder.build());

			} else {
				AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account.getAccount_id(), -num,
						false, "游戏内转卡,转给account_id:" + targetAccount.getAccount_id(), EGoldOperateType.PROXY_GIVE);
				if (!addGoldResultModel.isSuccess()) {
					session.send(MessageResponse.getMsgAllResponse("房卡数量不足!").build());
					return;
				}

				// 日志
				String nick_name = "--";
				if (targetAccount.getAccountWeixinModel() != null) {
					String name = targetAccount.getAccountWeixinModel().getNickname();
					if (StringUtils.isNotEmpty(name))
						nick_name = name;
				} else {
					// 会不会存在这种情况????
					nick_name = targetAccount.getAccount_name();
				}
				int target_proxy_account = targetAccount.getAccountModel().getProxy_level();

				MongoDBServiceImpl.getInstance().proxyGoldLog(account.getAccount_id(), targetAccount.getAccount_id(),
						nick_name, num, account.getAccountModel().getClient_ip(), 0, target_proxy_account,
						account.getAccountModel().getGold());

				centerRMIServer.addAccountGold(targetAccount.getAccount_id(), num, false,
						"游戏内转卡,接收account_id:" + account.getAccount_id(), EGoldOperateType.PROXY_GIVE);
				// send(MessageResponse.getMsgAllResponse("转卡成功!").build());

				GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
				goldCardAllResponseBuilder.setType(4);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.GOLD_CARD);
				responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
				session.send(responseBuilder.build());

				return;
			}

		}

		else if (type == GIVE_CARD_LOG) {

			Global.getWxPayService().execute(new Runnable() {

				@Override
				public void run() {
					if (accountModel.getIs_agent() < 1) {
						session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
						return;
					}

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
					// send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		}

		else if (type == STORE_UI) {

			if (accountModel.getIs_agent() < 1) {
				session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
				return;
			}
			SysParamModel sysParamModel1015 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(account.getGame_id()).get(1015);

			if (sysParamModel1015.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("代理商城暂未开放!").build());
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
			session.send(responseBuilder.build());
		}

		else if (type == BUY_GOLD) {

			if (accountModel.getIs_agent() < 1) {
				session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
				return;
			}
			int shopID = request.getGoodsId();

			SysParamModel sysParamModel1015 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(account.getGame_id()).get(1015);

			if (sysParamModel1015.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("代理商城暂未开放!").build());
				return;
			}

			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(),
					ShopModel.AGENT_SHOP);
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

					GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
					goldCardAllResponseBuilder.setType(6);
					goldCardAllResponseBuilder.setPayBuyResponse(
							ptAPIServiceImpl.getPayBuyResponse(prepayID, gameOrderID, account.getGame_id()));

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.GOLD_CARD);
					responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());

					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

					// 插入临时订单
					ptAPIServiceImpl.addCardLog(gameOrderID, account.getAccount_id(), account.getNickName(),
							account.getAccountModel().getIs_agent(), ESellType.GAME_PAY_CARD.getId(), shopModel.getId(),
							shopModel.getGold(), shopModel.getSend_gold(), shopModel.getPrice(), 0,
							ESellType.GAME_PAY_CARD.getName(), ESellType.GAME_PAY_CARD.getName(), 0 + "",
							ESellType.GAME_PAY_CARD.getName(), "", account.getGame_id(), shopModel.getName());

				}
			});

		}

		else if (type == PAY_BACK) {
			String gameOrderID = request.getGameOrderId();
			if (StringUtils.isEmpty(gameOrderID)) {
				logger.error("客户端 发送的  gameOrderID 是空的!!!");
				return;
			}
			logger.warn("收到客户端PAY_BACK协议");
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.payCenterCall(gameOrderID);
		}

		else if (type == UPDATE_RECOMMEND_ID) {

			if (!request.hasTargetAccountId()) {
				logger.error("推荐人ID未定义!" + account.getAccount_id());
				return;
			}
			long recommend_id = request.getTargetAccountId();

			if (recommend_id == account.getAccount_id()) {
				session.send(MessageResponse.getMsgAllResponse("不能设置自己").build());
				logger.error("不能设置自己!" + account.getAccount_id());
				return;
			}

			if (account.getAccountModel().getRecommend_id() != 0L) {
				session.send(MessageResponse.getMsgAllResponse("你已经设置了推荐人").build());
				logger.error("你已经设置了推荐人!" + account.getAccount_id());
				return;
			}

			// 验证是否有这个玩家
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account targetAccount = centerRMIServer.getAccount(recommend_id);
			if (targetAccount == null) {
				logger.error("对方ID不存在" + recommend_id);
				session.send(MessageResponse.getMsgAllResponse("对方ID不存在").build());
				return;
			}

			if (targetAccount.getAccountModel().getCreate_time().getTime() >= account.getAccountModel().getCreate_time()
					.getTime()) {
				logger.error("不是当天填写" + recommend_id);
				session.send(MessageResponse.getMsgAllResponse("推荐人无效").build());
				return;
			}
			
			
			if(account.getGame_id()==EGameType.AY.getId()) {
				boolean flag= DateUtils.isSameDay(new Date(), account.getAccountModel().getCreate_time());
				if(!flag) {
					logger.error("推荐人无效" + recommend_id);
					session.send(MessageResponse.getMsgAllResponse("推荐人无效").build());
					return;
				}
			}

			// 重置推荐代理人id
			accountModel.setRecommend_id(recommend_id);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountModelResponseBuilder.setRecommendId(recommend_id);
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicCenter);

			// send(MessageResponse.getMsgAllResponse("修改成功!").build());

			// 返回
			GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
			goldCardAllResponseBuilder.setType(7);
			goldCardAllResponseBuilder.setTargetAccountId(recommend_id);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.GOLD_CARD);
			responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
			session.send(responseBuilder.build());

			// 活动相关
			SysParamModel sysParamModel2000 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(account.getGame_id()).get(2000);
			if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
				if (targetAccount != null) {
					// 活动相关
					SysParamModel sysParamModel2004 = SysParamDict.getInstance()
							.getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
					int addGold = sysParamModel2004.getVal2();
					AccountRecommendModel accountRecommendModel = new AccountRecommendModel();
					accountRecommendModel.setAccount_id(targetAccount.getAccount_id());
					accountRecommendModel.setTarget_account_id(account.getAccount_id());
					accountRecommendModel.setCreate_time(new Date());
					accountRecommendModel.setGold_num(addGold);
					accountRecommendModel.setTarget_name(account.getNickName());
					accountRecommendModel.setTarget_icon(account.getIcon());
					accountRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
					boolean flag = centerRMIServer.addAccountRecommendModel(accountRecommendModel);
					if (flag) {
						// 给好友/推荐人加金币
						centerRMIServer.addAccountGold(targetAccount.getAccount_id(), addGold, false,
								"分享好友下载,好友account_id:" + account.getAccount_id(), EGoldOperateType.FRIEND_DOWN);
						// 给自己加金币,登录成功后的地方给
					}

				}
			}
			addRecommendPlayerIncome(account, recommend_id);
		}
	}

	private void addRecommendPlayerIncome(Account account, long recommend_id) {
		try {
			AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account,
					EAccountParamType.UP_RECOMMEND_PLAYER_INCOME);
			if (accountParamModel.getVal1() != null && accountParamModel.getVal1() == 1) {
				return;
			}

			// 活动相关
			// AccountModel accountModel = account.getAccountModel();
			if (recommend_id == 0) {
				return;// 无推荐人
			}
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account recommendAccount = centerRMIServer.getAccount(recommend_id);
			int level = recommendAccount.getAccountModel().getRecommend_level();
			if (recommendAccount == null || level == 0) {
				return;// 推荐人不存在或推荐人不是推广员
			}
			SysParamModel sysParamModel6001 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(account.getGame_id()).get(6001);
			if (sysParamModel6001 == null) {
				return;
			}
			if (level == 1) {
				logger.info(recommendAccount.getAccount_id() + " 一级推广员推荐新玩家返利:" + sysParamModel6001.getVal1() / 10.0);
				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal1() / 10.0,
						0l, "推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
				return;
			} else if (level == 2) {
				logger.info(recommendAccount.getAccount_id() + " 二级推广员推荐新玩家返利:" + sysParamModel6001.getVal2() / 10.0);
				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal2() / 10.0,
						0l, "推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
				if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
					Account recommendAccountUp = centerRMIServer
							.getAccount(recommendAccount.getAccountModel().getRecommend_id());
					if (recommendAccountUp.getAccountModel().getRecommend_level() == 1) {
						logger.info(recommendAccount.getAccount_id() + " 二级推广员推荐新玩家返利:"
								+ sysParamModel6001.getVal4() / 10.0);
						centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(),
								sysParamModel6001.getVal4() / 10.0, 2l, "下级推广员推荐新玩家",
								EGoldOperateType.RECOMMEND_PLAYER);
						return;
					}
				}

			} else if (level == 3) {
				logger.info(recommendAccount.getAccount_id() + " 三级推广员推荐新玩家返利:" + sysParamModel6001.getVal3() / 10.0);
				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal3() / 10.0,
						0l, "推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
				if (recommendAccount.getAccountModel().getRecommend_id() != 0) {
					Account recommendAccountUp = centerRMIServer
							.getAccount(recommendAccount.getAccountModel().getRecommend_id());
					if (recommendAccountUp.getAccountModel().getRecommend_level() == 2) {
						logger.info(recommendAccount.getAccount_id() + " 三级推广员推荐新玩家返利:"
								+ sysParamModel6001.getVal5() / 10.0);
						centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(),
								sysParamModel6001.getVal5() / 10.0, 3l, "下级推广员推荐新玩家",
								EGoldOperateType.RECOMMEND_PLAYER);
						if (recommendAccountUp.getAccountModel().getRecommend_id() != 0) {
							Account recommendAccountUpUp = centerRMIServer
									.getAccount(recommendAccountUp.getAccountModel().getRecommend_id());
							if (recommendAccountUpUp.getAccountModel().getRecommend_level() == 1) {
								logger.info(recommendAccount.getAccount_id() + " 三级推广员推荐新玩家返利:"
										+ sysParamModel6001.getVal4() / 10.0);
								centerRMIServer.doRecommendIncome(recommendAccountUpUp.getAccount_id(),
										sysParamModel6001.getVal4() / 10.0, 3l, "下级推广员推荐新玩家",
										EGoldOperateType.RECOMMEND_PLAYER);
							}
						}
					}
				}
			}
			accountParamModel.setVal1(1);
			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse.setType(EAccountParamType.UP_RECOMMEND_PLAYER_INCOME.getId());
			rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
			rsAccountParamModelResponse.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicCenter);
		} catch (Exception e) {
		}
	}

	/**
	 * 填写推荐人后，自己也获得奖励
	 * 
	 * @param account
	 */
	private void recommondAward(final Account account) {
		// 是否领取分享下载金币
		SysParamModel sysParamModel2000 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(account.getGame_id()).get(2000);
		if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
			AccountModel accountModel = account.getAccountModel();
			if (accountModel != null && accountModel.getRecommend_id() != 0) {
				AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account,
						EAccountParamType.DRAW_SHARE_DOWN);
				if (accountParamModel.getVal1() == null || accountParamModel.getVal1() == 0) {
					accountParamModel.setVal1(1);

					// ========同步到中心========
					RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
					redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
					//
					RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
					rsAccountResponseBuilder.setAccountId(account.getAccount_id());
					//
					RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse
							.newBuilder();
					rsAccountParamModelResponse.setAccountId(account.getAccount_id());
					rsAccountParamModelResponse.setType(EAccountParamType.DRAW_SHARE_DOWN.getId());
					rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
					rsAccountParamModelResponse.setNeedDb(true);
					rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
					//
					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
							ERedisTopicType.topicCenter);

					// 活动相关
					SysParamModel sysParamModel2004 = SysParamDict.getInstance()
							.getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
					int addGold = sysParamModel2004.getVal3();
					SpringService.getBean(ICenterRMIServer.class).addAccountGold(account.getAccount_id(), addGold,
							false, "通过分享下载,分享人account_id:" + accountModel.getRecommend_id(),
							EGoldOperateType.SHARE_DOWN);
				}
			}
		}
	}
}
