/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;

import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.ClientUpModel;
import com.cai.common.domain.ContinueLoginModel;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.SpringService;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.dictionary.ActivityDict;
import com.cai.dictionary.ContinueLoginDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ActivityDetailResponse;
import protobuf.clazz.Protocol.ActivityRequest;
import protobuf.clazz.Protocol.ActivityResponse;
import protobuf.clazz.Protocol.LoginNoticeResponse;
import protobuf.clazz.Protocol.OtherSystemResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RewardResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 *
 * @author xwy
 */
@ICmd(code = RequestType.ACTIVITY_VALUE, exName = "activityRequest")
public class ActivityHandler extends IClientHandler<ActivityRequest> {

	/**
	 * 1请求活动信息()
	 */
	private static final int ACTIVITY_DETAIL = 1;

	/**
	 * 2=领取登录奖励（activityID）
	 */
	private static final int GET_LOGIN_REWARD = 2;

	private static final int CLIENT_UP = 5;// 客户端上报

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.xianyi.framework.cmd.IClientHandler#execute(com.google.protobuf.
	 * GeneratedMessage, com.cai.domain.Session)
	 */
	@Override
	protected void execute(ActivityRequest request, Request topRequest, C2SSession session) throws Exception {
		if (!request.hasType())
			return;

		int type = request.getType();

		if (session.getAccount() == null)
			return;

		Account account = session.getAccount();

		AccountModel accountModel = account.getAccountModel();

		if (type == ACTIVITY_DETAIL) {

			ActivityDetailResponse.Builder activityDetailResponse = ActivityDetailResponse.newBuilder();
			activityDetailResponse.setActivityID(1);
			activityDetailResponse.setActivityType(1);
			activityDetailResponse.addAllContinueLoginResponse(ContinueLoginDict.getInstance().getContinueLoginResponseList(account.getGame_id()));
			int day = accountModel.getLogin_reward();

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			AccountParamModel accountParamModel = centerRMIServer.getAccountParamModelLoginReward(account.getAccount_id(),
					EAccountParamType.IS_REWARD_LOGIN);
			AccountParamModel accountParamMode_share = centerRMIServer.getAccountParamModelLoginReward(account.getAccount_id(),
					EAccountParamType.TAKE_SHARE_DOUBLE_REWARD);
			int shareReward = 0;
			if (day >= 7) {
				day = 1;
			} else {
				day += 1;
				shareReward = accountParamMode_share.getVal1();
			}
			activityDetailResponse.setLoginCanReward(day);// 连续登录可以领取的奖励(第几天的)
			activityDetailResponse.setRewardDay(accountModel.getLogin_reward());// 已经领了第几天
			activityDetailResponse.setLoginIsReward(accountParamModel.getVal1() == null ? 0 : accountParamModel.getVal1());//
			// 连续登录当天是否领取奖励
			activityDetailResponse.setShareReward(shareReward);
			// 代理商成列表
			ActivityResponse.Builder activityResponse = ActivityResponse.newBuilder();
			activityResponse.setType(ACTIVITY_DETAIL);
			activityResponse.addActivityDetails(activityDetailResponse);
			List<ActivityDetailResponse> activitylist = ActivityDict.getInstance().getActivityDetailResponse(account.getGame_id());
			for (ActivityDetailResponse res : activitylist) {
				activityResponse.addActivityDetails(res);
			}

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ACTIVITY);
			responseBuilder.setExtension(Protocol.activityResponse, activityResponse.build());
			session.send(responseBuilder.build());

			LoginNoticeModel loginNoticeModel = LoginNoticeDict.getInstance().getLoginNoticeDict().get(account.getGame_id());
			if (loginNoticeModel != null && loginNoticeModel.getOpen() == 1) {
				LoginNoticeResponse.Builder LoginNoticeResponseBuilder = MessageResponse.getLoginNoticeResponse(loginNoticeModel);
				OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
				otherSystemResponseBuilder.setType(4);
				otherSystemResponseBuilder.setLoginNoticeResponse(LoginNoticeResponseBuilder);
				Response.Builder builder = Response.newBuilder();
				builder.setResponseType(ResponseType.OTHER_SYS);
				builder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
				session.send(builder.build());
			}

		} else if (type == CLIENT_UP) {
			
			if (!GbCdCtrl.canHandleMust(session, Opt.CLIENT_UP))
				return;

			
			int upType = request.getUpType();
			String upText = request.getUpText();

			ClientUpModel upModel = new ClientUpModel();
			upModel.setPlayerId(accountModel.getAccount_id());
			upModel.setCreate_time(new Date());
			upModel.setUpString(upText);
			upModel.setUpType(upType);
			MongoDBServiceImpl.getInstance().insert_Model(upModel);

		} else if (type == GET_LOGIN_REWARD) {
			int day = accountModel.getLogin_reward();
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			AccountParamModel accountParamModel = centerRMIServer.getAccountParamModelLoginReward(account.getAccount_id(),
					EAccountParamType.IS_REWARD_LOGIN);

			if (accountParamModel.getVal1() != null && accountParamModel.getVal1() == 1) {
				session.send(MessageResponse.getMsgAllResponse("奖励已经领取!").build());
				return;
			}
			AccountParamModel accountParamMode_share = centerRMIServer.getAccountParamModelLoginReward(account.getAccount_id(),
					EAccountParamType.TAKE_SHARE_DOUBLE_REWARD);
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			int shareReward = 0;
			if (day >= 7) {
				day = 1;
				if (request.getShareType() != 1) {
					RsAccountParamModelResponse.Builder rsAccountParamModelResponse_share = RsAccountParamModelResponse.newBuilder();
					rsAccountParamModelResponse_share.setAccountId(account.getAccount_id());
					rsAccountParamModelResponse_share.setType(EAccountParamType.TAKE_SHARE_DOUBLE_REWARD.getId());
					rsAccountParamModelResponse_share.setVal1(0);
					rsAccountParamModelResponse_share.setData1(System.currentTimeMillis());
					rsAccountParamModelResponse_share.setNeedDb(true);
					rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse_share);
				}
			} else {
				shareReward = accountParamMode_share.getVal1();
				day += 1;
			}

			if (shareReward == 1 && request.getShareType() == 1) {
				session.send(MessageResponse.getMsgAllResponse("已经领取过双倍奖励啦，不能再领取啦!").build());
				return;
			}

			ContinueLoginModel loginModel = ContinueLoginDict.getInstance().getContinueLoginModel(account.getGame_id(), day);

			if (loginModel == null) {
				logger.error("loginModel is null" + day);
				ActivityResponse.Builder activityResponse = ActivityResponse.newBuilder();
				activityResponse.setType(GET_LOGIN_REWARD);
				activityResponse.setIsSuccess(false);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ACTIVITY);
				responseBuilder.setExtension(Protocol.activityResponse, activityResponse.build());
				session.send(responseBuilder.build());
				return;
			}

			Date now = new Date();
			boolean flag = false;
			if (accountParamModel.getDate1() != null) {
				boolean isSameday = DateUtils.isSameDay(accountParamModel.getDate1(), now);
				if (!isSameday) {
					flag = true;
				}
			} else {
				flag = true;
			}

			if (!flag) {
				logger.error("flag is false" + accountParamModel.getDate1());
				return;

			}

			accountParamModel.setVal1(1);
			accountParamModel.setDate1(now);
			accountModel.setLogin_reward(day);
			for (RewardResponse response : loginModel.getRewardList()) {
				if (response.getNumber() <= 0)
					continue;
				int num = request.getShareType() == 1 ? response.getNumber() * 2 : response.getNumber();
				if (response.getType() == 1) {
					centerRMIServer.addAccountGold(account.getAccount_id(), num, false, "每日登录领取闲逸豆:" + response.getNumber(),
							EGoldOperateType.LOGIN_ACTIVITY);
				} else {
					centerRMIServer.addAccountMoney(account.getAccount_id(), num, false, "每日登录领取金币" + day, EMoneyOperateType.LOGIN_ACTIVITY);
				}
			}

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//

			rsAccountResponseBuilder.setAccountId(account.getAccount_id());

			RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
			rsAccountModelResponse.setLoginReward(1);
			rsAccountModelResponse.setAccountId(account.getAccount_id());
			rsAccountModelResponse.setNeedDb(true);
			rsAccountModelResponse.setLoginReward(accountModel.getLogin_reward());
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponse);

			//
			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
			rsAccountParamModelResponse.setType(EAccountParamType.IS_REWARD_LOGIN.getId());
			rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
			rsAccountParamModelResponse.setData1(System.currentTimeMillis());
			rsAccountParamModelResponse.setNeedDb(true);
			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
			if (request.getShareType() == 1) {
				RsAccountParamModelResponse.Builder rsAccountParamModelResponse_share = RsAccountParamModelResponse.newBuilder();
				rsAccountParamModelResponse_share.setAccountId(account.getAccount_id());
				rsAccountParamModelResponse_share.setType(EAccountParamType.TAKE_SHARE_DOUBLE_REWARD.getId());
				rsAccountParamModelResponse_share.setVal1(1);
				rsAccountParamModelResponse_share.setData1(System.currentTimeMillis());
				rsAccountParamModelResponse_share.setNeedDb(true);
				rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse_share);
				shareReward = 1;
			}
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

			ActivityResponse.Builder activityResponse = ActivityResponse.newBuilder();
			activityResponse.setType(GET_LOGIN_REWARD);
			activityResponse.setIsSuccess(true);
			activityResponse.setRewardDay(accountModel.getLogin_reward());
			activityResponse.setShareType(request.getShareType());
			activityResponse.setShareReward(shareReward);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ACTIVITY);
			responseBuilder.setExtension(Protocol.activityResponse, activityResponse.build());
			session.send(responseBuilder.build());
		}
	}

}
