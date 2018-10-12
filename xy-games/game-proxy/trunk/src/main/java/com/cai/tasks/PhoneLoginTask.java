/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cai.common.define.EGameType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.LoginType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.IpUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.module.LoginModule;
import com.cai.module.RoomModule;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.LoginItemResponse;
import protobuf.clazz.Protocol.LoginRequest;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.WxLoginItemResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 
 * 
 *
 * @author wu_hc date: 2017年12月4日 下午2:52:19 <br/>
 */
public final class PhoneLoginTask implements Runnable {

	private final C2SSession session;
	private final LoginRequest request;
	private final long accountId;

	public PhoneLoginTask(C2SSession session, LoginRequest loginReq, long accountId) {
		this.session = session;
		this.request = loginReq;
		this.accountId = accountId;
	}

	@Override
	public void run() {
		// 检测一下是否有缓存
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		Account account = centerRMIServer.getAccount(accountId);

		if (account == null) {
			session.send(MessageResponse.getMsgAllResponse("不存在帐号!").build());
			return;
		}

		int game_index = 1;
		if (request.hasGameIndex()) {
			game_index = request.getGameIndex();
		}

		// 现在只有1,2
		if (EGameType.getEGameType(game_index) == null) {
			return;
		}

		String client_flag = "0";
		if (request.hasClientFlag()) {
			if (request.getClientFlag() == 1) {
				client_flag = "android";
			} else if (request.getClientFlag() == 2) {
				client_flag = "ios";
			} else if (request.getClientFlag() == 3) {
				client_flag = "pc";
			}
		}

		String client_version = "";
		if (request.hasClientVersion()) {
			if (client_version.length() < 100) {
				client_version = request.getClientVersion();
			}
		}

		AccountModel accountModel = account.getAccountModel();

		// 封号检测
		if (accountModel.getBanned() == 1) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
			return;
		}

		String ip = session.getClientIP();

		// 放到缓存中
		account.setCacheCreateTime(System.currentTimeMillis());
		account.setLastProxyIndex(SystemConfig.proxy_index);
		account.setGame_id(game_index);
		
		accountModel.setLast_login_time(new Date());
		accountModel.setLogin_times(accountModel.getLogin_times() + 1);
		accountModel.setLast_client_flag(client_flag);
		accountModel.setClient_version(client_version);
		accountModel.setClient_ip(ip);
		accountModel.setLast_channel(request.getChannelId());
		if (!IpUtil.isWhiteIp(ip)) {
			accountModel.setClient_ip2(ip);
		} else {
			if (accountModel.getClient_ip2() == null || "".equals(accountModel.getClient_ip2())) {
				accountModel.setClient_ip2(RandomUtil.randomIp());
			}
		}
		if (StringUtils.isNotEmpty(request.getClientIp())) {
			accountModel.setClient_ip2(request.getClientIp());
		}

		// ========同步到中心========
		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
		rsAccountResponseBuilder.setGameId(game_index);
		//
		RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
		rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
		rsAccountModelResponseBuilder.setLastLoginTime(accountModel.getLast_login_time().getTime());
		rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
		rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
		rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
		rsAccountModelResponseBuilder.setClientIp(accountModel.getClient_ip());
		rsAccountModelResponseBuilder.setClientIp2(accountModel.getClient_ip2());
		rsAccountModelResponseBuilder.setLoginReward(accountModel.getLogin_reward());
		rsAccountModelResponseBuilder.setChannelId(accountModel.getLast_channel());
		rsAccountModelResponseBuilder.setNeedDb(true);
		rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
		//
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
		// ==========================

		// 重写session
		session.setAccountLoginTime(System.currentTimeMillis());

		// 返回消息
		LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
		loginResponse.setType(3);
		loginResponse.setLoginType(LoginType.MOBILE);
		WxLoginItemResponse.Builder wxResponse = WxLoginItemResponse.newBuilder();

		LoginItemResponse.Builder loginItemResponBuilder = MessageResponse.getLoginItemResponse(account, 1);
		// 属性列表
		List<AccountPropertyResponse> list = MessageResponse.getSysAccountPropertyResponseList(game_index, account);
		loginItemResponBuilder.addAllAccountProperty(list);

		wxResponse.setLoginItemResponse(loginItemResponBuilder);
		loginResponse.setWxLoginItemResponse(wxResponse);
		loginResponse.setErrorCode(0);
		if (RoomModule.checkHasRoom(account, session)) {
			loginResponse.setRoomStatus(1); // 有房间
		}

		// 城市码相关
		LoginModule.assignCityCode(ip, loginResponse);

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.LOING);
		responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
		session.send(responseBuilder.build());
		
		LoginModule.loginReset(account, session);
	}
}
