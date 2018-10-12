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
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.ClubExclusiveGoldLogModel;
import com.cai.common.domain.GameLogModel;
import com.cai.common.domain.HallRecommendModel;
import com.cai.common.domain.Page;
import com.cai.common.domain.ProxyGoldLogModel;
import com.cai.common.domain.ShopModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.sdk.SdkDiamondShopModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.Global;
import com.cai.dictionary.SdkDiamondShopDict;
import com.cai.dictionary.ShopDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.WeiXinPayDiamondRunnable;
import com.cai.future.WeiXinPayRunnable;
import com.cai.service.FoundationService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RecommendService;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AddGoldCardHistoryResponse;
import protobuf.clazz.Protocol.GoldCardAllResponse;
import protobuf.clazz.Protocol.GoldCardRequest;
import protobuf.clazz.Protocol.GoldCardTransResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.StoreGoodResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 *
 * @author
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

	/**
	 * 填写代理推荐人id
	 */
	private static final int RECOMMEND_AGENT_ID = 8;

	// 闲逸豆交易流水
	private static final int GOLDCARD_TRANS = 9;

	// 专属豆交易流水
	private static final int CLUB_EXCLUSIVE_TRANS = 11;
	
	
	/**
	 * 购买钻石商品
	 */
	private static final int BUY_DIAMOND = 12;

	private static final String CLUB_TEXT1 = "亲友圈";
	private static final String CLUB_TEXT2 = "俱乐部";

	private static final String OPEN_ROOM_NORMAL = "创建房间扣除";
	private static final String OPEN_ROOM_CLUB = "亲友圈开房扣除";

	private static final String FAILED_ROOM_NORMAL = "创建房间退还";
	private static final String FAILED_ROOM_CLUB = "亲友圈开房退还";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(GoldCardRequest request, Request topRequest, C2SSession session) throws Exception {
		if (!request.hasType())
			return;

		int type = request.getType();

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
				if (_old_pw == null || "".equals(_old_pw) || !DigestUtils.md5Hex(_old_pw).equals(accountModel.getPassword())) {
					session.send(MessageResponse.getMsgAllResponse("旧密码不正确!").build());
					return;
				}
			}
			if (_new_pw == null || "".equals(_new_pw.trim())) {
				session.send(MessageResponse.getMsgAllResponse("新密码不能为空!").build());
				return;
			}

			if (_new_pw.length() < 6 || _new_pw.length() > 15) {
				session.send(MessageResponse.getMsgAllResponse("新密码长度必须大于等6位小于等于15位!").build());
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
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

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

			if (accountModel.getIs_agent() < 1 && accountModel.getProxy_level() < 1) {
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
			SysParamModel sysParamModel1109 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(6).get(1109);
			if (sysParamModel1109 != null && sysParamModel1109.getVal3() == 0) {// 是否控制代理之间互相转卡，0禁止，1不禁止
				boolean isSpecail = isSpecialAccount(account.getAccount_id(), request.getTargetAccountId(), sysParamModel1109);
				// try {
				// if (StringUtils.isNotEmpty(sysParamModel1109.getStr1())) {
				// String[] specailIds =
				// StringUtils.split(sysParamModel1109.getStr1(), "#");
				// for (String specail : specailIds) {
				// if (Long.parseLong(specail) == account.getAccount_id()) {
				// isSpecail = true;
				// break;
				// }
				// }
				// }
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				if (!isSpecail && account.getAccountModel().getProxy_level() > 0 && targetAccount.getAccountModel().getProxy_level() > 0) {
					session.send(MessageResponse.getMsgAllResponse("代理之间不能相互转卡").build());
					return;
				}
				if (!isSpecail && account.getAccountModel().getIs_agent() > 0 && targetAccount.getAccountModel().getIs_agent() > 0) {
					session.send(MessageResponse.getMsgAllResponse("代理之间不能相互转卡").build());
					return;
				}
			}

			if (account.getGame_id() == GameConstants.GAME_ID_FLS_LX) {
				if (targetAccount.getAccountModel().getProxy_level() > 0) {
					if (targetAccount.getAccountModel().getUp_proxy() != account.getAccount_id()) {
						session.send(MessageResponse.getMsgAllResponse("不能转卡给非自己的下级代理").build());
						return;
					}
				}

				if (sysParamModel1109 != null && sysParamModel1109.getVal2() == 0) {// 控制非3级代理是否可以转给普通玩家
					if (account.getAccountModel().getProxy_level() >= 0 && account.getAccountModel().getProxy_level() <= 2
							&& targetAccount.getAccountModel().getUp_proxy() == 0) {
						session.send(MessageResponse.getMsgAllResponse("不能转卡给普通玩家").build());
						return;
					}
				}
			}

			// 判断数量是否足够
			if (account.getAccountModel().getGold() < num) {
				session.send(MessageResponse.getMsgAllResponse(SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足!")).build());
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
				AddGoldResultModel addGoldResultModel = centerRMIServer.addAccountGold(account.getAccount_id(), -num, false,
						"游戏内转卡,转给account_id:" + targetAccount.getAccount_id(), EGoldOperateType.PROXY_GIVE);
				if (!addGoldResultModel.isSuccess()) {
					session.send(MessageResponse.getMsgAllResponse(SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足!")).build());
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

				MongoDBServiceImpl.getInstance().proxyGoldLog(account.getAccount_id(), targetAccount.getAccount_id(), nick_name, num,
						account.getAccountModel().getClient_ip(), 0, target_proxy_account, account.getAccountModel().getGold());

				centerRMIServer.addAccountGold(targetAccount.getAccount_id(), num, false, "游戏内转卡,接收account_id:" + account.getAccount_id(),
						EGoldOperateType.PROXY_GIVE);
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

			if (!GbCdCtrl.canHandleMust(session, Opt.GIVE_CARD_LOG))
				return;

			Global.getLogicService().execute(new Runnable() {

				@Override
				public void run() {
					if (accountModel.getIs_agent() < 1) {
						session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
						logger.error("你不是代理不能操作!" + accountModel.getAccount_id());
						return;
					}

					if (!request.hasCurPage()) {
						logger.error("客户端未传当前页" + accountModel.getAccount_id());
						return;
					}

					Long target_account_id = null;
					if (request.hasTargetAccountId()) {
						target_account_id = request.getTargetAccountId();
					}

					// 当前页
					int cur_page = request.getCurPage();
					int totalSize = MongoDBServiceImpl.getInstance().getProxyGoldLogModelCount(account.getAccount_id(), target_account_id);

					int size = 3;

					SysParamModel param = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
					if (param.getVal2() > 0 && param.getVal2() < 20) {
						size = param.getVal2();
					}

					Page page = new Page(cur_page, size, totalSize);
					List<ProxyGoldLogModel> proxyGoldLogModelList = MongoDBServiceImpl.getInstance().getProxyGoldLogModelList(page,
							account.getAccount_id(), target_account_id);

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
			SysParamModel sysParamModel1015 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(1015);

			if (sysParamModel1015.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("代理商城暂未开放!").build());
				return;
			}

			// 商品列表
			List<StoreGoodResponse> storeGoodResponseList = Lists.newArrayList();
			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.AGENT_SHOP);
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
			if (!GbCdCtrl.canHandle(session, Opt.BUY_GOLD))
				return;

			if (accountModel.getIs_agent() < 1) {
				session.send(MessageResponse.getMsgAllResponse("你不是代理不能操作!").build());
				return;
			}
			int shopID = request.getGoodsId();

			SysParamModel sysParamModel1015 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(1015);

			if (sysParamModel1015.getVal1() == 0) {
				session.send(MessageResponse.getMsgAllResponse("代理商城暂未开放!").build());
				return;
			}

			List<ShopModel> shopModelList = ShopDict.getInstance().getShopModelByGameIdAndShopType(account.getGame_id(), ShopModel.AGENT_SHOP);
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

			Global.getWxPayService().execute(new WeiXinPayRunnable(gameOrderID, shopModel, shopID, session, account));

			SysParamModel systemParam = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(10000);
			if (systemParam == null || systemParam.getVal1() == 1) {// 默认开启，掉单严重
				Global.schedule(new Runnable() {// 不管冲没冲，去调用一次查询微信
					@Override
					public void run() {
						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						centerRMIServer.payCenterCall(gameOrderID, 0);
					}
				}, 20);
			}
		}
		
		
		else if (type == BUY_DIAMOND) {
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

		else if (type == PAY_BACK) {
			String gameOrderID = request.getGameOrderId();
			if (StringUtils.isEmpty(gameOrderID)) {
				logger.error("客户端 发送的  gameOrderID 是空的!!!");
				return;
			}
			logger.warn("收到客户端PAY_BACK协议");
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			centerRMIServer.payCenterCall(gameOrderID, 0);
		}

		else if (type == UPDATE_RECOMMEND_ID) {

			if (!GbCdCtrl.canHandle(session, Opt.UPDATE_RECOMMEND_ID))
				return;

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
			if (account.getGame_id() == EGameType.MJ.getId()) {
				if (targetAccount.getAccountModel().getCreate_time().getTime() >= account.getAccountModel().getCreate_time().getTime()) {
					logger.error("推荐人的生成时间早于被推荐人" + recommend_id);
					session.send(MessageResponse.getMsgAllResponse("推荐人无效").build());
					return;
				}
			}

			if (account.getGame_id() == EGameType.AY.getId()) {
				if (targetAccount.getAccountModel().getCreate_time().getTime() >= account.getAccountModel().getCreate_time().getTime()) {
					logger.error("不是当天填写" + recommend_id);
					session.send(MessageResponse.getMsgAllResponse("推荐人无效").build());
					return;
				}
			}

			if (account.getGame_id() == EGameType.AY.getId()) {
				boolean flag = DateUtils.isSameDay(new Date(), account.getAccountModel().getCreate_time());
				if (!flag) {
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
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

			// send(MessageResponse.getMsgAllResponse("修改成功!").build());

			// 返回
			GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
			goldCardAllResponseBuilder.setType(7);
			goldCardAllResponseBuilder.setTargetAccountId(recommend_id);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.GOLD_CARD);
			responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
			session.send(responseBuilder.build());
			int recommendCount = 300;
			SysParamModel sysParamModel2224 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2224);
			// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2224);
			if (sysParamModel2224 != null && sysParamModel2224.getVal3() > 0) {
				recommendCount = sysParamModel2224.getVal3();
			}
			if (targetAccount.getRecommendRelativeModel().get() > recommendCount) {
				// 填写的推荐人数超过上线,取消提现
				if (targetAccount.getAccountModel().getIs_rebate() == 1) {
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(targetAccount.getAccount_id());
					rsAccountModelResponse.setIsRebate(0);
					centerRMIServer.ossModifyAccountModel(rsAccountModelResponse.build());
				}
			}
			// 添加到预处理map
			// 活动相关
			SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2000);
			if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
				if (targetAccount != null) {
					// 活动相关
					// SysParamModel sysParamModel2004 =
					// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
					// int addGold = sysParamModel2004.getVal2();
					AccountRecommendModel accountRecommendModel = new AccountRecommendModel();
					accountRecommendModel.setAccount_id(targetAccount.getAccount_id());
					accountRecommendModel.setTarget_account_id(account.getAccount_id());
					accountRecommendModel.setCreate_time(new Date());
					accountRecommendModel.setGold_num(0);
					accountRecommendModel.setTarget_name(account.getNickName());
					accountRecommendModel.setTarget_icon(account.getIcon());
					accountRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
					boolean flag = centerRMIServer.addAccountRecommendModel(accountRecommendModel);
					if (flag) {
						// 给好友/推荐人加金币
						// centerRMIServer.addAccountGold(targetAccount.getAccount_id(),
						// addGold, false,
						// "分享好友下载,好友account_id:" + account.getAccount_id(),
						// EGoldOperateType.FRIEND_DOWN);
						//
						RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
						rsAccountParamModelResponse.setAccountId(account.getAccount_id());
						rsAccountParamModelResponse.setType(EAccountParamType.ADD_RECOMMEND_GOLD.getId());
						rsAccountParamModelResponse.setData1(System.currentTimeMillis());
						rsAccountParamModelResponse.setVal1(1);// 有推荐获豆资格
						rsAccountParamModelResponse.setNeedDb(true);
						rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
						// 给自己加金币,登录成功后的地方给
						// 开启邀请新用户送红包活动
						RecommendService.getInstance().invite(recommend_id, account.getAccount_id(),
								account.getAccountWeixinModel() != null ? account.getAccountWeixinModel().getHeadimgurl() : "");
					}
				}
			}
			try {
				FoundationService.getInstance().sendInviteFriendActivityMissionProcess(account.getAccount_id(), recommend_id);
			} catch (Exception e) {
				logger.error("邀请下载任务通知失败", e);
			}
			// addRecommendPlayerIncome(account, recommend_id);
		} else if (type == RECOMMEND_AGENT_ID) {
			// if(!GbCdCtrl.canHandle(session, Opt.RECOMMEND_AGENT_ID)) return;

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
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account accountI = centerRMIServer.getAccount(account.getAccount_id());
			if (accountI.getHallRecommendModel().getAccount_id() != 0L || accountI.getHallRecommendModel().getTarget_account_id() > 0) {
				session.send(MessageResponse.getMsgAllResponse("你已经设置过了推荐人").build());
				logger.error("你已经设置了推荐人!" + account.getAccount_id());
				return;
			}
			SysParamModel sysParamModel2224 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2224);
			if (sysParamModel2224 == null || sysParamModel2224.getVal2() == 0) {
				if (accountI.getAccountModel().getIs_agent() > 0) {
					logger.error("代理不能设置推荐人" + account.getAccount_id());
					session.send(MessageResponse.getMsgAllResponse("代理不能设置推荐人").build());
					return;
				}
			}
			// if (accountI.getHallRecommendModel().getRecommend_level() == 1) {
			// logger.error("你已经是一级推广员" + account.getAccount_id());
			// session.send(MessageResponse.getMsgAllResponse("你已经是一级推广员，不能设置推荐人").build());
			// return;
			// }

			// 验证是否有这个玩家
			Account targetAccount = centerRMIServer.getAccount(recommend_id);
			if (targetAccount == null) {
				logger.error("对方ID不存在" + recommend_id);
				session.send(MessageResponse.getMsgAllResponse("对方ID不存在").build());
				return;
			}
			SysParamModel sysParamModel2251 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6).get(2251);
			if (sysParamModel2251 == null || sysParamModel2251.getVal4() != 1) {
				if (targetAccount.getHallRecommendModel().getRecommend_level() == 0) {
					logger.error("被推荐人不是推广员" + recommend_id);
					session.send(MessageResponse.getMsgAllResponse("被推荐人不是推广员").build());
					return;
				}
			} else {
				// 只要是代理的身份就可以了
				if (targetAccount.getAccountModel().getIs_agent() == 0) {
					logger.error("推荐人不是推广员也不是代理" + recommend_id);
					session.send(MessageResponse.getMsgAllResponse("推荐人必须是代理身份").build());
					return;
				}
			}

			if (sysParamModel2224 != null && sysParamModel2224.getVal1() == 1) {
				if (targetAccount.getAccountModel().getCreate_time().getTime() >= account.getAccountModel().getCreate_time().getTime()) {
					logger.error("推荐人的生成时间早于被推荐人" + recommend_id);
					session.send(MessageResponse.getMsgAllResponse("推荐人无效").build());
					return;
				}
			}
			// if (accountI.getHallRecommendModel().getRecommend_level() > 0) {
			// if (accountI.getHallRecommendModel().getRecommend_level() <
			// targetAccount.getHallRecommendModel().getRecommend_level()) {
			// logger.error("推荐人无效-等级低" + recommend_id);
			// session.send(MessageResponse.getMsgAllResponse("推荐人无效-等级低").build());
			// return;
			// }
			// }
			int recommendCount = 300;
			if (sysParamModel2224 != null && sysParamModel2224.getVal3() > 0) {
				recommendCount = sysParamModel2224.getVal3();
			}
			if (targetAccount.getRecommendRelativeModel().get() > recommendCount) {
				// 填写的推荐人数超过上线,取消提现
				if (targetAccount.getAccountModel().getIs_rebate() == 1) {
					RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
					rsAccountModelResponse.setAccountId(targetAccount.getAccount_id());
					rsAccountModelResponse.setIsRebate(0);
					centerRMIServer.ossModifyAccountModel(rsAccountModelResponse.build());
				}
			}
			GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
			goldCardAllResponseBuilder.setType(8);
			goldCardAllResponseBuilder.setTargetAccountId(recommend_id);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.GOLD_CARD);
			responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
			session.send(responseBuilder.build());
			// ========同步到中心========
			HallRecommendModel nowHallRecommendModel = new HallRecommendModel();// 现在的推荐关系
			nowHallRecommendModel.setAccount_id(recommend_id);
			nowHallRecommendModel.setTarget_name(account.getNickName());
			nowHallRecommendModel.setTarget_icon(account.getIcon());
			nowHallRecommendModel.setRecommend_level(0);
			nowHallRecommendModel.setCreate_time(new Date());
			nowHallRecommendModel.setTarget_account_id(account.getAccount_id());
			nowHallRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
			nowHallRecommendModel.setProxy_level(accountModel.getIs_agent());
			boolean success = centerRMIServer.addHallRecommendModel(nowHallRecommendModel);
			if (success) {
				SysParamModel sysParamModel2004 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
				int addGold = 50;
				if (sysParamModel2004 != null) {
					addGold = sysParamModel2004.getVal4();
				}
				centerRMIServer.addAccountGold(account.getAccount_id(), addGold, false, "填写推广员推荐人送豆，推广员account_id:" + targetAccount.getAccount_id(),
						EGoldOperateType.PADDING_RECOMMEND_ID);
				try {
					addRecommendPreReceive(account, recommend_id);
				} catch (Exception e) {
					logger.info(account.getAccount_id() + "addRecommendPreReceive error recommend_id=" + recommend_id);
				}
				// RedisResponse.Builder redisResponseBuilder =
				// RedisResponse.newBuilder();
				// redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				// RsAccountResponse.Builder rsAccountResponseBuilder =
				// RsAccountResponse.newBuilder();
				// rsAccountResponseBuilder.setAccountId(account.getAccount_id());
				// RsAccountParamModelResponse.Builder
				// rsAccountParamModelResponse =
				// RsAccountParamModelResponse.newBuilder();
				// rsAccountParamModelResponse.setAccountId(account.getAccount_id());
				// rsAccountParamModelResponse.setType(EAccountParamType.ADD_HALL_GOLD.getId());
				// rsAccountParamModelResponse.setVal1(1);//大厅填写推广员获豆获得资格
				// rsAccountParamModelResponse.setNeedDb(true);
				// rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
				// redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
				// ERedisTopicType.topicCenter);
			} else {
				session.send(MessageResponse.getMsgAllResponse("设置推广员失败").build());
			}

		} else if (type == GOLDCARD_TRANS) {
			// 闲逸豆交易流水
			if (!GbCdCtrl.canHandleMust(session, Opt.GOLDCARD_TRANS)) {
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.GOLD_CARD);
				GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
				goldCardAllResponseBuilder.setType(10);
				responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
				session.send(responseBuilder.build());
				return;
			}

			Global.getLogicService().execute(new Runnable() {

				@Override
				public void run() {
					// 分页
					int realPage = request.getCurPage();
					Page page = new Page();
					page.setPageSize(10);
					page.setRealPage(realPage);
					List<GameLogModel> gameLogList = MongoDBServiceImpl.getInstance().goldcardTransLog(account.getAccount_id(), ELogType.addGold,
							page);

					GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
					goldCardAllResponseBuilder.setType(10);
					EGoldOperateType eGoldOperateType = null;
					for (GameLogModel v : gameLogList) {
						GoldCardTransResponse.Builder goldCardTransBuilder = GoldCardTransResponse.newBuilder();
						goldCardTransBuilder.setId(v.getId());
						goldCardTransBuilder.setCurrentGold((int) v.getCurrGold());
						goldCardTransBuilder.setTime((int) (v.getCreate_time().getTime() / 1000));
						goldCardTransBuilder.setTransGold(v.getV1().intValue());
						eGoldOperateType = EGoldOperateType.getOperateTypeById(v.getV2().intValue(), goldCardTransBuilder.getTransGold());
						if (eGoldOperateType == EGoldOperateType.OPEN_ROOM) {
							if (v.getMsg().contains(CLUB_TEXT1) || v.getMsg().contains(CLUB_TEXT2)) {
								// 俱乐部开房
								goldCardTransBuilder.setTitle(OPEN_ROOM_CLUB);
							} else {
								goldCardTransBuilder.setTitle(OPEN_ROOM_NORMAL);
							}
						} else if (eGoldOperateType == EGoldOperateType.FAILED_ROOM) {
							// 开房失败
							if (v.getMsg().contains(CLUB_TEXT1) || v.getMsg().contains(CLUB_TEXT2)) {
								// 俱乐部开房
								goldCardTransBuilder.setTitle(FAILED_ROOM_CLUB);
							} else {
								goldCardTransBuilder.setTitle(FAILED_ROOM_NORMAL);
							}
						} else {
							goldCardTransBuilder.setTitle(eGoldOperateType.getTransDesc());
						}
						goldCardAllResponseBuilder.addGoldCardTransList(goldCardTransBuilder.build());
					}
					goldCardAllResponseBuilder.setCurPage(request.getCurPage());
					goldCardAllResponseBuilder.setPageSize(page.getPageSize());
					goldCardAllResponseBuilder.setTotalPage(page.getTotalPage());
					goldCardAllResponseBuilder.setTotalSize(page.getTotalSize());
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.GOLD_CARD);
					responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
					session.send(responseBuilder.build());
				}
			});

		} else if (type == CLUB_EXCLUSIVE_TRANS) {
			// 专属豆交易流水
			if (!GbCdCtrl.canHandleMust(session, Opt.CLUB_EXCLUSIVE)) {
				Response.Builder responseBuilder = Response.newBuilder();
				GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
				goldCardAllResponseBuilder.setType(CLUB_EXCLUSIVE_TRANS);
				responseBuilder.setResponseType(ResponseType.GOLD_CARD);
				responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
				session.send(responseBuilder.build());
				return;
			}

			final SysGameTypeDict sysGameTypeDict = SysGameTypeDict.getInstance();

			Global.getLogicService().execute(new Runnable() {

				@Override
				public void run() {
					// 分页
					int realPage = request.getCurPage();
					Page page = new Page();
					page.setPageSize(10);
					page.setRealPage(realPage);
					List<ClubExclusiveGoldLogModel> gameLogList = MongoDBServiceImpl.getInstance().clubExclusiveTransLog(account.getAccount_id(),
							page);

					GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
					goldCardAllResponseBuilder.setType(CLUB_EXCLUSIVE_TRANS);
					EGoldOperateType eGoldOperateType = null;
					for (ClubExclusiveGoldLogModel v : gameLogList) {
						GoldCardTransResponse.Builder goldCardTransBuilder = GoldCardTransResponse.newBuilder();
						goldCardTransBuilder.setId(v.get_id());
						goldCardTransBuilder.setCurrentGold((int) v.getV2());
						goldCardTransBuilder.setTime((int) (v.getCreate_time().getTime() / 1000));

						// 兼容老的专属豆日志处理方式，8月30上线后，删除这种处理方式
						if (v.getOperateType() == EGoldOperateType.OSS_DESC_EXCLUSIVE_GOLD.getId()) {
							// 后台扣除已经为负，直接赋值
							goldCardTransBuilder.setTransGold((int) v.getV3());
						} else {
							if (v.getMsg().indexOf("|减少[") >= 0) {
								goldCardTransBuilder.setTransGold((int) (-v.getV3()));
							} else {
								goldCardTransBuilder.setTransGold((int) (v.getV3()));
							}
						}
						// 做兼容处理，8月30日上线后，下个版本更新，采用这种方式处理
						// 客户端显示正负关系，需要特殊处理
						// goldCardTransBuilder.setTransGold((int)(v.getV2()-v.getV1()));

						goldCardTransBuilder.setGameId(v.getGameTypeIndex());
						goldCardTransBuilder.setAppId(v.getAppId());
						// 显示APP名字
						if (v.getGameTypeIndex() > 0) {
							goldCardTransBuilder.setAppName(sysGameTypeDict.getMJname(v.getGameTypeIndex()));
						} else if (v.getGameTypeIndex() == 0) {
							// 显示APP名字
							goldCardTransBuilder.setAppName(sysGameTypeDict.getAppNameByAppId(v.getAppId()));
						}
						eGoldOperateType = EGoldOperateType.getOperateTypeById(v.getOperateType(), goldCardTransBuilder.getTransGold());
						if (eGoldOperateType == EGoldOperateType.OPEN_ROOM) {
							if (v.getMsg().contains(CLUB_TEXT1) || v.getMsg().contains(CLUB_TEXT2)) {
								// 俱乐部开房
								goldCardTransBuilder.setTitle(OPEN_ROOM_CLUB);
							} else {
								goldCardTransBuilder.setTitle(OPEN_ROOM_NORMAL);
							}
						} else if (eGoldOperateType == EGoldOperateType.FAILED_ROOM) {
							// 开房失败
							if (v.getMsg().contains(CLUB_TEXT1) || v.getMsg().contains(CLUB_TEXT2)) {
								// 俱乐部开房
								goldCardTransBuilder.setTitle(FAILED_ROOM_CLUB);
							} else {
								goldCardTransBuilder.setTitle(FAILED_ROOM_NORMAL);
							}
						} else {
							goldCardTransBuilder.setTitle(eGoldOperateType.getTransDesc());
						}
						goldCardAllResponseBuilder.addGoldCardTransList(goldCardTransBuilder.build());
					}
					goldCardAllResponseBuilder.setCurPage(request.getCurPage());
					goldCardAllResponseBuilder.setPageSize(page.getPageSize());
					goldCardAllResponseBuilder.setTotalPage(page.getTotalPage());
					goldCardAllResponseBuilder.setTotalSize(page.getTotalSize());
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.GOLD_CARD);
					responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
					session.send(responseBuilder.build());
				}
			});

		}
	}

	public boolean isSpecialAccount(long accountId, long targetAccountId, SysParamModel sysParamModel1109) {
		try {
			if (StringUtils.isNotEmpty(sysParamModel1109.getStr1())) {
				String[] specailIds = StringUtils.split(sysParamModel1109.getStr1(), "#");
				for (String specail : specailIds) {
					if (Long.parseLong(specail) == accountId) {
						return true;
					}
				}
				String[] specailIds2 = StringUtils.split(sysParamModel1109.getStr2(), "#");
				for (String specail : specailIds2) {
					if (Long.parseLong(specail) == targetAccountId) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// 预返利，获取返利资格
	public void addRecommendPreReceive(Account account, long recommend_id) {
		SysParamModel sysParamModel5000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(5000);
		long gameId = sysParamModel5000.getVal1();
		if (gameId != EGameType.JS.getId()) {// 江苏棋牌才有推荐返利
			return;
		}
		AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account,
				EAccountParamType.RECOMMEND_PLAYER_RECEIVE);
		// val1=1获得返利资格，2表示已经返利
		if (accountParamModel.getVal1() != null && accountParamModel.getVal1() == 2) {
			return;
		}
		accountParamModel.setVal1(1);
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
		rsAccountParamModelResponse.setAccountId(account.getAccount_id());
		rsAccountParamModelResponse.setType(EAccountParamType.RECOMMEND_PLAYER_RECEIVE.getId());
		rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
		rsAccountParamModelResponse.setNeedDb(true);
		rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

	}

	/**
	 * 填写推荐人后，自己也获得奖励
	 * 
	 * @param account
	 */
	// private void recommondAward(final Account account) {
	// // 是否领取分享下载金币
	// SysParamModel sysParamModel2000 =
	// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2000);
	// if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
	// AccountModel accountModel = account.getAccountModel();
	// if (accountModel != null && accountModel.getRecommend_id() != 0) {
	// AccountParamModel accountParamModel =
	// PublicServiceImpl.getInstance().getAccountParamModel(account,
	// EAccountParamType.DRAW_SHARE_DOWN);
	// if (accountParamModel.getVal1() == null || accountParamModel.getVal1() ==
	// 0) {
	// accountParamModel.setVal1(1);
	//
	// // ========同步到中心========
	// RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
	// redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
	// //
	// RsAccountResponse.Builder rsAccountResponseBuilder =
	// RsAccountResponse.newBuilder();
	// rsAccountResponseBuilder.setAccountId(account.getAccount_id());
	// //
	// RsAccountParamModelResponse.Builder rsAccountParamModelResponse =
	// RsAccountParamModelResponse.newBuilder();
	// rsAccountParamModelResponse.setAccountId(account.getAccount_id());
	// rsAccountParamModelResponse.setType(EAccountParamType.DRAW_SHARE_DOWN.getId());
	// rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
	// rsAccountParamModelResponse.setNeedDb(true);
	// rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
	// //
	// redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
	// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
	// ERedisTopicType.topicCenter);
	//
	// // 活动相关
	// SysParamModel sysParamModel2004 =
	// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(account.getGame_id()).get(2004);
	// int addGold = sysParamModel2004.getVal3();
	// SpringService.getBean(ICenterRMIServer.class).addAccountGold(account.getAccount_id(),
	// addGold, false,
	// "通过分享下载,分享人account_id:" + accountModel.getRecommend_id(),
	// EGoldOperateType.SHARE_DOWN);
	// }
	// }
	// }
	// }
}
