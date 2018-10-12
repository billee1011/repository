/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.tasks;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.define.ELogType;
import com.cai.common.define.EPlatType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.AppItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.IpUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.define.LoginRspType;
import com.cai.dictionary.AppItemDict;
import com.cai.module.LoginModule;
import com.cai.module.LoginMsgSender;
import com.cai.module.RoomModule;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.ip.IPSeeker;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.LoginItemResponse;
import protobuf.clazz.Protocol.LoginRequest;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.SubAppItemResponse;
import protobuf.clazz.Protocol.WxLoginItemResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 
 * wx平台转码登陆
 * 
 * @author wu_hc date: 2017年8月9日 上午10:44:48 <br/>
 */
public class WXCacheLoginTask implements Runnable {

	private static Logger logger = LoggerFactory.getLogger(WXCacheLoginTask.class);

	private final LoginRequest request;
	private final C2SSession session;

	/**
	 * @param request
	 * @param session
	 */
	public WXCacheLoginTask(LoginRequest request, C2SSession session) {
		this.request = request;
		this.session = session;
	}

	@Override
	public void run() {
		try {
			if (!request.hasSelfToken()) {
				logger.error("!request.hasSelfToken()");
				return;
			}

			String selfToken = request.getSelfToken();
			long account_id = 0L;
			try {
				String[] tokens = selfToken.split("-");
				account_id = Long.parseLong(tokens[0]);
			} catch (Exception e) {
				logger.error("error", e);
			}

			if (account_id == 0L) {
				LoginMsgSender.sendLoginFailedRsp(session, LoginRspType.WX_CACHE, 0);
				return;
			}

			PerformanceTimer timer = new PerformanceTimer();

			int game_index = request.hasGameIndex() ? request.getGameIndex() : 1;
			String client_flag = request.hasClientFlag() ? EPlatType.platType(request.getClientFlag()).getDesc() : EPlatType.UNKOWN.getDesc();
			String client_version = request.hasClientVersion() ? request.getClientVersion() : "";

			// 检测一下是否有缓存
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAccount(account_id);

			// 1
			if (null == account) {
				logger.error("微信平台登陆,数据库查无此人并创建失败!{}", account_id);
				return;
			}

			// 2
			if (!account.getAccountModel().getPt().equals(EPtType.WX.getId())) {
				LoginMsgSender.sendLoginFailedRsp(session, LoginRspType.WX_CACHE, 0);
				logger.error("微信平台登陆,玩家不是微信玩家!{}", account_id);
				return;
			}

			AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
			if (accountWeixinModel == null) {
				LoginMsgSender.sendLoginFailedRsp(session, LoginRspType.WX_CACHE, 0);
				logger.error("发现账号异常,没有微信记录:account_id:" + account.getAccount_id());
				return;
			} else {
				if (accountWeixinModel.getLast_false_self_token() == null || accountWeixinModel.getSelf_token() == null) {
					LoginMsgSender.sendLoginFailedRsp(session, LoginRspType.WX_CACHE, 0);
					logger.error("accountWeixinModel.getLast_false_self_token() == null ||this.sendSelfToketLoginFail(selfToken)");
					return;
				}

				long k1 = accountWeixinModel.getLast_false_self_token().getTime() + 1000L * 60 * 60 * 24 * 20;// 20天
				if (!selfToken.equals(accountWeixinModel.getSelf_token()) || k1 < System.currentTimeMillis()) {
					LoginMsgSender.sendLoginFailedRsp(session, LoginRspType.WX_CACHE, 0);
					logger.error(" reqToken:{},cacheToken:{},k1:{},now:{}", selfToken, accountWeixinModel.getSelf_token(), k1,
							System.currentTimeMillis());

					return;
				}

				AccountModel accountModel = account.getAccountModel();

				// 封号检测
				if (accountModel.getBanned() == 1) {
					PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
					return;
				}

				String ip = session.getClientIP();
				String ip_addr = IPSeeker.getInstance().getAddress(ip);

				// 放到缓存中
				account.setCacheCreateTime(System.currentTimeMillis());
				account.setLastProxyIndex(SystemConfig.proxy_index);
				account.setGame_id(game_index);
//				LoginModule.loginReset(account, session);

				accountModel.setLast_login_time(new Date());
				accountModel.setLogin_times(accountModel.getLogin_times() + 1);
				accountModel.setLast_client_flag(client_flag);
				accountModel.setClient_version(client_version);
				accountModel.setClient_ip(ip);
				// 如果不是白名单的直接赋值
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
//				rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
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
				rsAccountModelResponseBuilder.setNeedDb(true);
				rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
				// ==========================

				// 重写session
				session.setAccountLoginTime(System.currentTimeMillis());
				session.setAccount(account);

				// 返回消息
				LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
				loginResponse.setType(3);
				try {
					List<AppItem> list0 = AppItemDict.getInstance().getAppItemList();
					if (list0 == null) {
						centerRMIServer.reLoadAppItemDictionary();
						AppItemDict.getInstance().load();
						list0 = AppItemDict.getInstance().getAppItemList();
					}
					for (AppItem appItem : list0) {
						SubAppItemResponse.Builder subAppItemResponse = SubAppItemResponse.newBuilder();
						subAppItemResponse.setAppId(appItem.getAppId());
						subAppItemResponse.setFlag(appItem.getFlag());
						subAppItemResponse.setIconUrl(appItem.getIconUrl());
						subAppItemResponse.setOrder(appItem.getOrders());
						subAppItemResponse.setPackagepath(appItem.getPackagepath());
						subAppItemResponse.setPackagesize(appItem.getPackagesize());
						subAppItemResponse.setStatus(appItem.getT_status());
						subAppItemResponse.setVersion(appItem.getVersions());
						subAppItemResponse.setDownUrl(appItem.getPackageDownPath());
						subAppItemResponse.setAppName(appItem.getAppName());
						subAppItemResponse.setGameType(appItem.getGame_type());
						subAppItemResponse.setPlaceholder(appItem.getPlaceholder());
						loginResponse.addAppList(subAppItemResponse);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				WxLoginItemResponse.Builder wxResponse = WxLoginItemResponse.newBuilder();
				wxResponse.setSelfToken(selfToken);

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
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.LOING);
				responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
				session.send(responseBuilder.build());

				LoginModule.loginReset(account, session);
				
				MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
						"微信缓存登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 2L, timer.get(), session.getClientIP());

			}

		} catch (Exception e) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录异常Error-E202").build());
			throw e;
		}
	}
}
