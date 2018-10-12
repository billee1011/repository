package com.cai.handler;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.net.core.ClientHandler;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.ip.IPSeeker;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.FastLogingItemResponse;
import protobuf.clazz.Protocol.LoginItemResponse;
import protobuf.clazz.Protocol.LoginNoticeResponse;
import protobuf.clazz.Protocol.LoginRequest;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.OtherSystemResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.WxLoginItemResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsAccountWeixinModelResponse;

/**
 * 登录
 * 
 * @author run
 *
 */
public class LoginHandler extends ClientHandler<LoginRequest> {

	/**
	 * 微信登录
	 */
	private static final int WX_LOGIN = 1;

	/**
	 * 平台转码的微信登录
	 */
	private static final int PT_CACHE_WX_LOGIN = 2;

	/**
	 * 快速登录
	 */
	private static final int FAST_LOGIN = 3;

	/**
	 * 登录成功之后处理
	 */
	private static final int LOGIN_SUCCESS = 100;

	@Override
	public void onRequest() throws Exception {

		int type = request.getType();

		// 操作频率控制
		if (!session.isCanRequest("LoginHandler_" + type, 500L)) {
			return;
		}

		int game_index = 1;
		if (request.hasGameIndex()) {
			game_index = request.getGameIndex();
		}

		// 现在只有1,2
		if (!(game_index == 1 || game_index == 2)) {
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
		final int game_index_tmp = game_index;
		final String client_flag_tmp = client_flag;
		final String client_version_tmp = client_version;

		// //临时
		// if(StringUtils.isNotEmpty(client_flag) && !"0".equals(client_flag)){
		// if(StringUtils.isNotEmpty(client_version)){
		//
		// if("android".equals(client_flag) &&
		// client_version.compareTo("1.0.30")<0){
		// PlayerServiceImpl.getInstance().sendAccountMsg(session,
		// MessageResponse.getMsgAllResponse("当前游戏版本过低，请退出后台再进入，按提示更新!").build());
		// }else if("ios".equals(client_flag) &&
		// client_version.compareTo("1.0.11")<0){
		// PlayerServiceImpl.getInstance().sendAccountMsg(session,
		// MessageResponse.getMsgAllResponse("当前游戏版本过低，请退出后台再进入，按提示更新!").build());
		// }
		// }
		// }

		PerformanceTimer timer = new PerformanceTimer();

		// 微信登录
		if (type == WX_LOGIN) {

			if (!request.hasWxCode())
				return;

			///////////////////////////////////////////////////////////////////////////////////////////
			Global.getWxService().execute(new Runnable() {

				@Override
				public void run() {
					try {
						String wx_code = request.getWxCode();
						JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxGetAccessTokenByCode(wx_code, game_index_tmp);
						if (jsonObject == null) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败Error1!").build());
							return;
						}

						Integer errCode = 0;
						if (jsonObject.containsKey("errcode")) {
							errCode = (Integer) jsonObject.get("errcode");
						}

						if (errCode != 0) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,code无效").build());
							return;
						}

						String access_token = jsonObject.getString("access_token");
						int expires_in = jsonObject.getInteger("expires_in");
						String refresh_token = jsonObject.getString("refresh_token");
						String openid = jsonObject.getString("openid");
						String scope = jsonObject.getString("scope");
						String unionid = jsonObject.getString("unionid");

						// 用户详情
						jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(game_index_tmp, access_token, openid);
						if (jsonObject == null) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败Error2!").build());
							return;
						}

						if (jsonObject.containsKey("errcode")) {
							errCode = (Integer) jsonObject.get("errcode");
						}
						if (errCode != 0) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,token无效").build());
							return;
						}

						openid = jsonObject.getString("openid");
						String nickname = jsonObject.getString("nickname");
						// nickname转码，过滤mysql识别不了的
						nickname = EmojiFilter.filterEmoji(nickname);
						// 长度控制
						nickname = MyStringUtil.substringByLength(nickname, 12);

						String sex = jsonObject.getString("sex");
						String province = jsonObject.getString("province");
						String city = jsonObject.getString("city");
						String country = jsonObject.getString("country");
						String headimgurl = jsonObject.getString("headimgurl");
						String privilege = jsonObject.getString("privilege");
						unionid = jsonObject.getString("unionid");// 全平台唯一id

						String accounName = EPtType.WX.getId() + "_" + unionid;

						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						Account account = centerRMIServer.getAndCreateAccount(EPtType.WX.getId(), accounName, session.getClientIP(), client_flag_tmp, client_version_tmp);

						if (account == null) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,Error3").build());
							return;
						}

						AccountModel accountModel = account.getAccountModel();

						// 封号检测
						if (accountModel.getBanned() == 1) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
							return;
						}

						// 微信相关的
						AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
						accountWeixinModel.setAccess_token(access_token);
						accountWeixinModel.setRefresh_token(refresh_token);
						accountWeixinModel.setOpenid(openid);
						accountWeixinModel.setScope(scope);
						accountWeixinModel.setUnionid(unionid);
						accountWeixinModel.setNickname(nickname);
						accountWeixinModel.setSex(sex);
						accountWeixinModel.setProvince(province);
						accountWeixinModel.setCity(city);
						accountWeixinModel.setCountry(country);
						accountWeixinModel.setHeadimgurl(headimgurl);
						accountWeixinModel.setPrivilege(privilege);
						accountWeixinModel.setLast_flush_time(new Date());
						accountWeixinModel.setSelf_token(accountWeixinModel.getAccount_id() + "-" + RandomUtil.getRandomString(20));
						accountWeixinModel.setLast_false_self_token(new Date());

						// 放到缓存中
						account.setCacheCreateTime(System.currentTimeMillis());
						account.setLastProxyIndex(SystemConfig.proxy_index);
						account.setGame_id(game_index_tmp);
						accountModel.setLast_login_time(new Date());
						accountModel.setLogin_times(accountModel.getLogin_times() + 1);
						accountModel.setLast_client_flag(client_flag_tmp);
						accountModel.setClient_version(client_version_tmp);
						account.setLast_login_ip(session.getClientIP());
						account.setIp_addr(IPSeeker.getInstance().getAddress(account.getLast_login_ip()));

						// ========同步到中心========
						RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
						redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
						//
						RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
						rsAccountResponseBuilder.setAccountId(account.getAccount_id());
						rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
						rsAccountResponseBuilder.setGameId(game_index_tmp);
						rsAccountResponseBuilder.setLastLoginIp(session.getClientIP());
						rsAccountResponseBuilder.setIpAddr(account.getIp_addr());
						//
						RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
						rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
						rsAccountModelResponseBuilder.setLastLoginTime(accountModel.getLast_login_time().getTime());
						rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
						rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
						rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
						rsAccountModelResponseBuilder.setNeedDb(true);
						rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
						//
						RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil.getRsAccountWeixinModelResponse(accountWeixinModel);
						rsAccountWeixinModelResponseBuilder.setNeedDb(true);
						rsAccountResponseBuilder.setRsAccountWeixinModelResponse(rsAccountWeixinModelResponseBuilder);
						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
						// ==========================

						// 重写session
						session.setAccountID(account.getAccountModel().getAccount_id());
						session.setAccount_name(account.getAccountModel().getAccount_name());
						session.setAccountLoginTime(System.currentTimeMillis());
						session.setAccount(account);

						// 登录之后的session
						SessionServiceImpl.getInstance().fireSessionCreate(session);

						// 返回消息
						LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
						loginResponse.setType(1);
						WxLoginItemResponse.Builder wxResponse = WxLoginItemResponse.newBuilder();
						wxResponse.setSelfToken(accountWeixinModel.getSelf_token());

						LoginItemResponse.Builder loginItemResponBuilder = MessageResponse.getLoginItemResponse(account, 1);
						// 属性列表
						List<AccountPropertyResponse> list = MessageResponse.getSysAccountPropertyResponseList(game_index_tmp, account);
						loginItemResponBuilder.addAllAccountProperty(list);

						wxResponse.setLoginItemResponse(loginItemResponBuilder);
						loginResponse.setWxLoginItemResponse(wxResponse);
						loginResponse.setErrorCode(0);

						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.LOING);
						responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
						PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
						;

						MongoDBServiceImpl.getInstance().log(account.getAccount_id(), ELogType.login, "微信登录:account_id:" + account.getAccount_id() + ",IPAddr:" + account.getIp_addr(), 1L, timer.get(), session.getClientIP());

					} catch (Exception e) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录异常Error-E201").build());
						throw e;
					}
				}
			});

			///////////////////////////////////////////////////////////////////////////////////////////

		}

		else if (type == PT_CACHE_WX_LOGIN) {

			try {
				if (!request.hasSelfToken())
					return;

				String selfToken = request.getSelfToken();
				long account_id = 0L;
				try {
					String[] tokens = selfToken.split("-");
					account_id = Long.parseLong(tokens[0]);
				} catch (Exception e) {
					logger.error("error", e);
				}

				if (account_id == 0L) {
					this.sendSelfToketLoginFail(selfToken);
					return;
				}

				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				Account account = centerRMIServer.getAccount(account_id);

				if (account == null || !account.getAccountModel().getPt().equals(EPtType.WX.getId())) {
					this.sendSelfToketLoginFail(selfToken);
					return;
				}

				AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
				if (accountWeixinModel == null) {
					this.sendSelfToketLoginFail(selfToken);
					logger.error("发现账号异常,没有微信记录:account_id:" + account.getAccount_id());
					return;
				} else {
					if (accountWeixinModel.getLast_false_self_token() == null || accountWeixinModel.getSelf_token() == null) {
						this.sendSelfToketLoginFail(selfToken);
						return;
					}

					// long k1 =
					// accountWeixinModel.getLast_false_self_token().getTime() +
					// 1000L * 60 * 60 * 24 * 30;//30天
					long k1 = accountWeixinModel.getLast_false_self_token().getTime() + 1000L * 60 * 60 * 24 * 20;// 20天
					if (!selfToken.equals(accountWeixinModel.getSelf_token()) || k1 < System.currentTimeMillis()) {
						this.sendSelfToketLoginFail(selfToken);
						return;
					}

					AccountModel accountModel = account.getAccountModel();

					// 封号检测
					if (accountModel.getBanned() == 1) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
						return;
					}

					// 放到缓存中
					account.setCacheCreateTime(System.currentTimeMillis());
					account.setLastProxyIndex(SystemConfig.proxy_index);
					account.setGame_id(game_index);
					accountModel.setLast_login_time(new Date());
					accountModel.setLogin_times(accountModel.getLogin_times() + 1);
					accountModel.setLast_client_flag(client_flag);
					accountModel.setClient_version(client_version);
					account.setLast_login_ip(session.getClientIP());
					account.setIp_addr(IPSeeker.getInstance().getAddress(account.getLast_login_ip()));

					// ========同步到中心========
					RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
					redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
					//
					RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
					rsAccountResponseBuilder.setAccountId(account.getAccount_id());
					rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
					rsAccountResponseBuilder.setGameId(game_index);
					rsAccountResponseBuilder.setLastLoginIp(session.getClientIP());
					rsAccountResponseBuilder.setIpAddr(account.getIp_addr());
					//
					RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
					rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
					rsAccountModelResponseBuilder.setLastLoginTime(accountModel.getLast_login_time().getTime());
					rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
					rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
					rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
					rsAccountModelResponseBuilder.setNeedDb(true);
					rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
					//
					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
					// ==========================

					// 重写session
					session.setAccountID(account.getAccountModel().getAccount_id());
					session.setAccount_name(account.getAccountModel().getAccount_name());
					session.setAccountLoginTime(System.currentTimeMillis());
					session.setAccount(account);

					// 登录之后的session
					SessionServiceImpl.getInstance().fireSessionCreate(session);

					// 返回消息
					LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
					loginResponse.setType(3);
					WxLoginItemResponse.Builder wxResponse = WxLoginItemResponse.newBuilder();
					wxResponse.setSelfToken(selfToken);

					LoginItemResponse.Builder loginItemResponBuilder = MessageResponse.getLoginItemResponse(account, 1);
					// 属性列表
					List<AccountPropertyResponse> list = MessageResponse.getSysAccountPropertyResponseList(game_index, account);
					loginItemResponBuilder.addAllAccountProperty(list);

					wxResponse.setLoginItemResponse(loginItemResponBuilder);
					loginResponse.setWxLoginItemResponse(wxResponse);
					loginResponse.setErrorCode(0);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.LOING);
					responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
					send(responseBuilder.build());

					MongoDBServiceImpl.getInstance().log(account.getAccount_id(), ELogType.login, "微信缓存登录:account_id:" + account.getAccount_id() + ",IPAddr:" + account.getIp_addr(), 2L, timer.get(), session.getClientIP());

				}

			} catch (Exception e) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录异常Error-E202").build());
				throw e;
			}

		}

		// 快速登录
		else if (type == FAST_LOGIN) {

			if (!request.hasImei())
				return;
			String imei = request.getImei();

			if (imei == null || "".equals(imei.trim())) {
				return;
			}

			String accounName = EPtType.SELF.getId() + "_" + imei;

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAndCreateAccount(EPtType.SELF.getId(), accounName, session.getClientIP(), client_flag, client_version);
			AccountModel accountModel = account.getAccountModel();

			// 封号检测
			if (accountModel.getBanned() == 1) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
				return;
			}

			// 放到缓存中
			account.setCacheCreateTime(System.currentTimeMillis());
			account.setLastProxyIndex(SystemConfig.proxy_index);
			account.setGame_id(game_index);
			accountModel.setLast_login_time(new Date());
			accountModel.setLogin_times(accountModel.getLogin_times() + 1);
			accountModel.setLast_client_flag(client_flag);
			accountModel.setClient_version(client_version);
			account.setLast_login_ip(session.getClientIP());
			account.setIp_addr(IPSeeker.getInstance().getAddress(account.getLast_login_ip()));

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
			rsAccountResponseBuilder.setGameId(game_index);
			rsAccountResponseBuilder.setLastLoginIp(session.getClientIP());
			rsAccountResponseBuilder.setIpAddr(account.getIp_addr());
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountModelResponseBuilder.setLastLoginTime(accountModel.getLast_login_time().getTime());
			rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
			rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
			rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
			// ==========================

			// 重写session
			session.setAccountID(account.getAccountModel().getAccount_id());
			session.setAccount_name(account.getAccountModel().getAccount_name());
			session.setAccountLoginTime(System.currentTimeMillis());
			session.setAccount(account);

			// 登录之后的session
			SessionServiceImpl.getInstance().fireSessionCreate(session);

			// 返回消息
			LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
			loginResponse.setType(2);
			FastLogingItemResponse.Builder fastLogingItemResponse = FastLogingItemResponse.newBuilder();
			fastLogingItemResponse.setImei(imei);
			LoginItemResponse.Builder loginItemResponBuilder = MessageResponse.getLoginItemResponse(account, 1);
			// 属性列表
			List<AccountPropertyResponse> list = MessageResponse.getSysAccountPropertyResponseList(game_index, account);
			loginItemResponBuilder.addAllAccountProperty(list);

			fastLogingItemResponse.setLoginItemResponse(loginItemResponBuilder);
			loginResponse.setFastLogingItemResponse(fastLogingItemResponse);
			loginResponse.setErrorCode(0);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.LOING);
			responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
			send(responseBuilder.build());

			MongoDBServiceImpl.getInstance().log(account.getAccount_id(), ELogType.login, "快速登录:account_id:" + account.getAccount_id() + ",IPAddr:" + account.getIp_addr(), 3L, timer.get(), session.getClientIP());
			// 测试
			// send(MessageResponse.getMsgAllResponse("这是一个提示消息提示!").build());

		}

		else if (type == LOGIN_SUCCESS) {
			Account account = session.getAccount();
			if (account == null)
				return;

			// 最后的公告
			sendLastNotice(account);

			// ==================登录公告===========================
			// 是否是重连 0=正常  >0重连
			int reconnet = 1;
			if (request.hasReconnect()) {
				reconnet = request.getReconnect();
			}

			if (reconnet == 0) {

				Date now = MyDateUtil.getNow();
				LoginNoticeModel loginNoticeModel = LoginNoticeDict.getInstance().getLoginNoticeDict().get(account.getGame_id());
				if (loginNoticeModel != null && loginNoticeModel.getOpen() == 1) {
					boolean flag = false;
					AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account, EAccountParamType.LAST_LOGIN_NOTICE);
					if (accountParamModel.getDate1() != null) {
						int hour = MyDateUtil.hourBetween(accountParamModel.getDate1(), now);
						if (hour > loginNoticeModel.getInterval_hour()-1) {
							flag = true;
						}

					} else {
						flag = true;
					}

					if (flag) {
						accountParamModel.setDate1(now);

						LoginNoticeResponse.Builder LoginNoticeResponseBuilder = MessageResponse.getLoginNoticeResponse(loginNoticeModel);
						OtherSystemResponse.Builder otherSystemResponseBuilder = OtherSystemResponse.newBuilder();
						otherSystemResponseBuilder.setType(4);
						otherSystemResponseBuilder.setLoginNoticeResponse(LoginNoticeResponseBuilder);
						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.OTHER_SYS);
						responseBuilder.setExtension(Protocol.otherSystemResponse, otherSystemResponseBuilder.build());
						send(responseBuilder.build());
						
						// ========同步到中心========
						RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
						redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
						//
						RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
						rsAccountResponseBuilder.setAccountId(account.getAccount_id());
						//
						RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse.newBuilder();
						rsAccountParamModelResponse.setAccountId(account.getAccount_id());
						rsAccountParamModelResponse.setType(EAccountParamType.LAST_LOGIN_NOTICE.getId());
						rsAccountParamModelResponse.setData1(accountParamModel.getDate1().getTime());
						rsAccountParamModelResponse.setNeedDb(true);
						rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
						//
						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
					}

				}

			}
			//=======================================================
			

			////////////////////////////////////////////////////////////////////////
			// 调用其它线程池
			Global.getWxService().execute(new Runnable() {
				@Override
				public void run() {
					AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
					if (accountWeixinModel != null) {
						// 是否要刷新微信登录缓存,30分钟刷新一次
						Date last_flush_time = accountWeixinModel.getLast_flush_time();
						if (last_flush_time == null || last_flush_time.getTime() + 1000L * 60 * 30 < MyDateUtil.getNow().getTime()) {

							// 旧值
							String old_nickname = accountWeixinModel.getNickname();
							String old_headimgurl = accountWeixinModel.getHeadimgurl();

							JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxFlushToken(1, accountWeixinModel.getRefresh_token());
							if (jsonObject != null) {
								Integer errCode = 0;
								if (jsonObject.containsKey("errcode")) {
									errCode = (Integer) jsonObject.get("errcode");
								}
								if (errCode == 0) {
									String access_token = jsonObject.getString("access_token");
									String openid = jsonObject.getString("openid");
									String refresh_token = jsonObject.getString("refresh_token");
									String scope = jsonObject.getString("scope");

									jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(1, access_token, openid);
									if (jsonObject != null) {
										if (jsonObject.containsKey("errcode")) {
											errCode = (Integer) jsonObject.get("errcode");
										}
										if (errCode == 0) {
											openid = jsonObject.getString("openid");
											String nickname = jsonObject.getString("nickname");
											// nickname转码，过滤mysql识别不了的
											nickname = EmojiFilter.filterEmoji(nickname);
											// 长度控制
											nickname = MyStringUtil.substringByLength(nickname, 12);

											String sex = jsonObject.getString("sex");
											String province = jsonObject.getString("province");
											String city = jsonObject.getString("city");
											String country = jsonObject.getString("country");
											String headimgurl = jsonObject.getString("headimgurl");
											String privilege = jsonObject.getString("privilege");
											String unionid = jsonObject.getString("unionid");// 全平台唯一id

											accountWeixinModel.setAccess_token(access_token);
											accountWeixinModel.setRefresh_token(refresh_token);
											accountWeixinModel.setOpenid(openid);
											accountWeixinModel.setScope(scope);
											accountWeixinModel.setUnionid(unionid);
											accountWeixinModel.setNickname(nickname);
											accountWeixinModel.setSex(sex);
											accountWeixinModel.setProvince(province);
											accountWeixinModel.setCity(city);
											accountWeixinModel.setCountry(country);
											accountWeixinModel.setHeadimgurl(headimgurl);
											accountWeixinModel.setPrivilege(privilege);
											accountWeixinModel.setLast_flush_time(new Date());
											// accountWeixinModel.setSelf_token(accountWeixinModel.getAccount_id()
											// + "-" +
											// RandomUtil.getRandomString(20));
											// accountWeixinModel.setLast_false_self_token(new
											// Date());
											accountWeixinModel.setNeedDB(true);

											// ========同步到中心========
											RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
											redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
											//
											RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
											rsAccountResponseBuilder.setAccountId(account.getAccount_id());
											//
											RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil.getRsAccountWeixinModelResponse(accountWeixinModel);
											rsAccountWeixinModelResponseBuilder.setNeedDb(true);
											rsAccountResponseBuilder.setRsAccountWeixinModelResponse(rsAccountWeixinModelResponseBuilder);
											//
											redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
											RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
											// ==========================

											AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse.newBuilder();
											// 刷新玩家头像和昵称
											if (accountWeixinModel.getNickname() != null) {
												if (!accountWeixinModel.getNickname().equals(old_nickname)) {
													AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.ACCOUNT_NICK_NAME.getId(), null, null, null, accountWeixinModel.getNickname(), null, null);
													accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
												}
											}
											if (accountWeixinModel.getHeadimgurl() != null) {
												if (!accountWeixinModel.getHeadimgurl().equals(old_headimgurl)) {
													AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse.getAccountPropertyResponse(EPropertyType.ACCOUNT_HEAR_IMG.getId(), null, null, null, accountWeixinModel.getHeadimgurl(), null, null);
													accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
												}
											}
											if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList().size() > 0) {
												Response.Builder responseBuilder = Response.newBuilder();
												responseBuilder.setResponseType(ResponseType.PROPERTY);
												responseBuilder.setExtension(Protocol.accountPropertyListResponse, accountPropertyListResponseBuilder.build());
												PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
												;
											}
										}
									}

								}
							}

						}
					}
				}
			});

			////////////////////////////////////////////////////////////////

		}

	}

	/**
	 * 登录后发送最后的公告信息
	 * 
	 * @param account
	 */
	private void sendLastNotice(Account account) {
		Map<Integer, Response> lastNoticeCache = PublicServiceImpl.getInstance().getLastNoticeCache();
		Response response = lastNoticeCache.get(account.getGame_id());
		if (response != null) {
			send(response);
			return;
		}

		response = lastNoticeCache.get(0);
		if (response != null) {
			send(response);
		}
	}

	/**
	 * 平台转码登录失败返回
	 * 
	 * @param selfToken
	 */
	private void sendSelfToketLoginFail(String selfToken) {
		LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
		loginResponse.setType(3);
		loginResponse.setErrorCode(1);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.LOING);
		responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
		send(responseBuilder.build());
		logger.info("平台转码的微信登录失败:selfToken:" + selfToken);
	}

}
