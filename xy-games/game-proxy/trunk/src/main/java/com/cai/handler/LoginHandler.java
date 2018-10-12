/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 *
 */
package com.cai.handler;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.LongFunction;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.AccountConstant;
import com.cai.common.constant.ActivityMissionTypeEnum;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.LoginType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.IpUtil;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RegexUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.TimeUtil;
import com.cai.common.util.UUIDUtils;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.IPGroupDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.future.WechatGameLoginRunnable;
import com.cai.future.WeiXinLoginRunnable;
import com.cai.module.LoginModule;
import com.cai.module.RoomModule;
import com.cai.service.C2SSessionService;
import com.cai.service.FoundationService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.PublicServiceImpl;
import com.cai.service.RecommendService;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.ip.IPSeeker;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AccountPropertyListResponse;
import protobuf.clazz.Protocol.AccountPropertyResponse;
import protobuf.clazz.Protocol.FastLogingItemResponse;
import protobuf.clazz.Protocol.GoldCardAllResponse;
import protobuf.clazz.Protocol.LoginItemResponse;
import protobuf.clazz.Protocol.LoginNoticeResponse;
import protobuf.clazz.Protocol.LoginRequest;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.OtherSystemResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
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
 *
 * @author
 */
@ICmd(code = RequestType.LOING_VALUE, exName = "loginRequest")
public class LoginHandler extends IClientHandler<LoginRequest> {

	/**
	 * 微信登录
	 */
	public static final int WX_LOGIN = 1;

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

	/**
	 * 客户端ip上传
	 */
	private static final int CLIENT_IP = 4;

	/**
	 * 客户端上传登录ip
	 */
	private static final int CLIENT_IP_LOGIN = 5;

	/**
	 * 微信新登录，客户端会把数据需要的数据上报
	 */
	public static final int WX_NEW_LOGIN = 6;

	/**
	 * 微信小游戏登陆，
	 */
	public static final int WX_LITTLE_GAME_LOGIN = 7;

	@Override
	protected void execute(LoginRequest request, Request topRequest, C2SSession session) throws Exception {
		int type = request.getType();

		int game_index = 1;
		if (request.hasGameIndex()) {
			game_index = request.getGameIndex();
		}

		// 现在只有1,2
		if (EGameType.getEGameType(game_index) == null) {
			logger.error("game_index not exist!");
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

		PerformanceTimer timer = new PerformanceTimer();
		Date now = new Date();
		// 微信登录
		if (type == WX_LOGIN || type == WX_NEW_LOGIN) {

			// if (!request.hasWxCode()) {
			// logger.error("!request.hasWxCode()!");
			// return;
			// }

			///////////////////////////////////////////////////////////////////////////////////////////
			Global.getWxLoginService().execute(new WeiXinLoginRunnable(session, request, game_index_tmp, client_version_tmp, client_version_tmp));

			///////////////////////////////////////////////////////////////////////////////////////////

		} else if (type == WX_LITTLE_GAME_LOGIN) {
			Global.getWxLoginService().execute(new WechatGameLoginRunnable(session, request, game_index_tmp, client_version_tmp, client_version_tmp));
		}

		else if (type == PT_CACHE_WX_LOGIN) {

			Global.getPtLoginService().execute(new Runnable() {

				@Override
				public void run() {
					try {

						String selfToken = request.getSelfToken();

						if (StringUtils.isEmpty(selfToken)) {
							logger.error("客户端请求登陆平台转码登陆，但没有传selfToken!!!!");
							return;
						}

						Pair<Long, String> pair = LoginModule.decodeToken(selfToken);

						long account_id = pair.getFirst().longValue();

						if (account_id == 0L) {
							sendSelfToketLoginFail(selfToken, session, 1);
							return;
						}

						SessionUtil.setAttr(session, AttributeKeyConstans.LOGIN_TYPE, LoginType.WX);

						// 检测一下是否有缓存[如果有缓存，直接用，不重新拉取] GAME-TODO
						LongFunction<Account> accountFunc = (id) -> {
							C2SSession oldSession = C2SSessionService.getInstance().getSession(id);
							if (null != oldSession && null != oldSession.getAccount()) {
								return oldSession.getAccount();
							}
							ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
							return centerRMIServer.getAccount(id);
						};

						Account account = accountFunc.apply(account_id);

						if (account == null || !account.getAccountModel().getPt().equals(EPtType.WX.getId())) {
							sendSelfToketLoginFail(selfToken, session, 2);
							logger.error("account == null ||this.sendSelfToketLoginFail(selfToken)");
							return;
						}

						AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
						if (accountWeixinModel == null) {
							sendSelfToketLoginFail(selfToken, session, 3);
							logger.error("发现账号异常,没有微信记录:account_id:" + account.getAccount_id());
							return;
						} else {
							if (accountWeixinModel.getLast_false_self_token() == null || accountWeixinModel.getSelf_token() == null) {
								sendSelfToketLoginFail(selfToken, session, 4);
								logger.error("accountWeixinModel.getLast_false_self_token() == null ||this.sendSelfToketLoginFail(selfToken)");
								return;
							}

							long k1 = accountWeixinModel.getLast_false_self_token().getTime() + 1000L * 60 * 60 * 24 * 20;// 20天
							if (!selfToken.equals(accountWeixinModel.getSelf_token()) || k1 < System.currentTimeMillis()) {
								sendSelfToketLoginFail(selfToken, session, 5);
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
							if (StringUtils.isNotEmpty(request.getClientIp())) {
								boolean flag = RegexUtil.isboolIP(request.getClientIp());
								if (flag) {
									ip = request.getClientIp();
								}
							}
							String ip_addr = IPSeeker.getInstance().getAddress(ip);

							// 放到缓存中
							account.setCacheCreateTime(System.currentTimeMillis());
							account.setLastProxyIndex(SystemConfig.proxy_index);
							account.setGame_id(game_index_tmp);

							if (null == accountModel.getLast_login_time() || !DateUtils.isSameDay(now, accountModel.getLast_login_time())) {
								//重置当天在线时长
								accountModel.setToday_online(0);
							}
							LoginModule.loginReset(account, session);
							accountModel.setLast_login_time(now);
							accountModel.setLogin_times(accountModel.getLogin_times() + 1);
							accountModel.setLast_client_flag(client_flag_tmp);
							accountModel.setClient_version(client_version_tmp);
							accountModel.setClient_ip(ip);
							accountModel.setLast_channel(request.getChannelId());
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
							
							String thirdToken = UUIDUtils.getUUID32();
							
							// ========同步到中心========
							RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
							redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
							//
							RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
							rsAccountResponseBuilder.setAccountId(account.getAccount_id());
							rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
							rsAccountResponseBuilder.setGameId(game_index_tmp);
							//
							RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
							rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
							rsAccountModelResponseBuilder.setLastLoginTime(accountModel.getLast_login_time().getTime());
							rsAccountModelResponseBuilder.setTodayOnline(accountModel.getToday_online());
							rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
							rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
							rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
							rsAccountModelResponseBuilder.setClientIp(accountModel.getClient_ip());
							rsAccountModelResponseBuilder.setClientIp2(accountModel.getClient_ip2());
							rsAccountModelResponseBuilder.setLoginReward(accountModel.getLogin_reward());
							rsAccountModelResponseBuilder.setChannelId(accountModel.getLast_channel());
							rsAccountModelResponseBuilder.setThirdToken(thirdToken);
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
							loginResponse.setLoginType(LoginType.WX);
							loginResponse.setThirdToken(thirdToken);
							
							WxLoginItemResponse.Builder wxResponse = WxLoginItemResponse.newBuilder();
							wxResponse.setSelfToken(selfToken);

							LoginItemResponse.Builder loginItemResponBuilder = MessageResponse.getLoginItemResponse(account, 1);
							// 属性列表
							List<AccountPropertyResponse> list = MessageResponse.getSysAccountPropertyResponseList(game_index_tmp, account);
							loginItemResponBuilder.addAllAccountProperty(list);

							wxResponse.setLoginItemResponse(loginItemResponBuilder);
							loginResponse.setWxLoginItemResponse(wxResponse);
							loginResponse.setErrorCode(0);
							if (RoomModule.checkHasRoom(account, session)) {
								loginResponse.setRoomStatus(1); // 有房间
							}

							// 城市码相关
							LoginModule.assignCityCode(request.getClientIp(), loginResponse);

							Response.Builder responseBuilder = Response.newBuilder();
							responseBuilder.setResponseType(ResponseType.LOING);
							responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
							session.send(responseBuilder.build());

							
							MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
									"微信缓存登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 2L, timer.get(), session.getClientIP());

						}

					} catch (Exception e) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录异常Error-E202").build());
						throw e;
					}
				}
			});

		}

		// 快速登录
		else if (type == FAST_LOGIN) {

			if (!request.hasImei()) {
				logger.error("!request.hasImei()");
				return;
			}
			String imei = request.getImei();

			if (imei == null || "".equals(imei.trim())) {
				logger.error(".equals(imei.trim()");
				return;
			}

			SessionUtil.setAttr(session, AttributeKeyConstans.LOGIN_TYPE, LoginType.SELF);
			String accounName = EPtType.SELF.getId() + "_" + imei;

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAndCreateAccount(EPtType.SELF.getId(), accounName, session.getClientIP(), client_flag,
					client_version, SystemConfig.proxy_index);
			AccountModel accountModel = account.getAccountModel();

			// 封号检测
			if (accountModel.getBanned() == 1) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
				return;
			}

			String ip = session.getClientIP();
			if (StringUtils.isNotEmpty(request.getClientIp())) {
				boolean flag = RegexUtil.isboolIP(request.getClientIp());
				if (flag) {
					ip = request.getClientIp();
				}
			}
			String ip_addr = IPSeeker.getInstance().getAddress(ip);

			// 放到缓存中
			account.setCacheCreateTime(System.currentTimeMillis());
			account.setLastProxyIndex(SystemConfig.proxy_index);
			account.setGame_id(game_index);

			if (null == accountModel.getLast_login_time() || !DateUtils.isSameDay(now, accountModel.getLast_login_time())) {
				//重置当天在线时长
				accountModel.setToday_online(0);
			}
			
			LoginModule.loginReset(account, session);
			
			
			accountModel.setLast_login_time(now);
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

			String thirdToken = UUIDUtils.getUUID32();
			
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
			rsAccountModelResponseBuilder.setTodayOnline(accountModel.getToday_online());
			rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
			rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
			rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
			rsAccountModelResponseBuilder.setClientIp(accountModel.getClient_ip());
			rsAccountModelResponseBuilder.setClientIp2(accountModel.getClient_ip2());
			rsAccountModelResponseBuilder.setLoginReward(accountModel.getLogin_reward());
			// rsAccountModelResponseBuilder.setRecommendId(accountModel.getRecommend_id());
			rsAccountModelResponseBuilder.setThirdToken(thirdToken);
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
			if (RoomModule.checkHasRoom(account, session)) {
				loginResponse.setRoomStatus(1); // 有房间
			}

			loginResponse.setLoginType(LoginType.SELF);
			loginResponse.setThirdToken(thirdToken);
			// 城市码相关
			LoginModule.assignCityCode(request.getClientIp(), loginResponse);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.LOING);
			responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
			session.send(responseBuilder.build());


			MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
					"快速登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 3L, timer.get(), session.getClientIP());
		}

		else if (type == LOGIN_SUCCESS) {
			Account account = session.getAccount();
			if (account == null) {
				logger.error("login_success account == null");
				return;
			}
			// 通过分享链接下载app，实行推广员返利
			// 最后的公告
			sendLastNotice(account, session);
			if (account.getGame_id() == 0) {
				account.setGame_id(1);
			}

			// ==================登录公告===========================
			// 是否是重连 0=正常 >0重连
			int reconnet = 1;
			if (request.hasReconnect()) {
				reconnet = request.getReconnect();
			}

			addAccountLoginMission(account);

			if (reconnet == 0) {

				LoginNoticeModel loginNoticeModel = LoginNoticeDict.getInstance().getLoginNoticeDict().get(account.getGame_id());
				if (loginNoticeModel != null && loginNoticeModel.getOpen() == 1) {
					boolean flag = false;
					AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account,
							EAccountParamType.LAST_LOGIN_NOTICE);
					if (accountParamModel.getDate1() != null) {
						int hour = MyDateUtil.hourBetween(accountParamModel.getDate1(), now);
						if (hour > loginNoticeModel.getInterval_hour() - 1) {
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
						session.send(responseBuilder.build());

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
			// 同步app版本信息给大厅
			// sendAppItemList();
			// =======================================================

			IPGroupDict.getInstance().updateIpToPlayer(session);
			// ==================================================

			////////////////////////////////////////////////////////////////////////
			// 调用其它线程池
			Global.getWeiXinFlushService().execute(new Runnable() {
				@Override
				public void run() {
					AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
					if (accountWeixinModel != null) {
						// 是否要刷新微信登录缓存,60分钟刷新一次
						Date last_flush_time = accountWeixinModel.getLast_flush_time();
						if (last_flush_time == null || last_flush_time.getTime() + 1000L * 60 * 600 < MyDateUtil.getNow().getTime()) {

							// 旧值
							String old_nickname = accountWeixinModel.getNickname();
							String old_headimgurl = accountWeixinModel.getHeadimgurl();

							JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxFlushToken(account.getGame_id(),
									accountWeixinModel.getRefresh_token(),account.getLastChannelId());
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

									jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(account.getGame_id(), access_token, openid);
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
											nickname = MyStringUtil.substringByLength(nickname, AccountConstant.NICK_NAME_LEN);

											String sex = jsonObject.getString("sex");
											String province = jsonObject.getString("province");
											String city = jsonObject.getString("city");
											String country = jsonObject.getString("country");
											String headimgurl = jsonObject.getString("headimgurl");
											String privilege = jsonObject.getString("privilege");
											String unionid = jsonObject.getString("unionid");// 全平台唯一id

 											// 俱乐部微信头像/昵称刷新问题
											if (!Objects.equals(headimgurl, old_headimgurl) || !Objects.equals(nickname, old_nickname)) {
												LoginModule.syncUpdateClubMemberInfo(account.getAccount_id(), headimgurl, nickname);
											}

											accountWeixinModel.setAccess_token(access_token);
											accountWeixinModel.setRefresh_token(refresh_token);
											accountWeixinModel.setOpenid(openid);
											accountWeixinModel.setScope(scope);
											accountWeixinModel.setUnionid(unionid);
											accountWeixinModel.setNickname(nickname);
											accountWeixinModel.setSex(sex);
											accountWeixinModel.setProvince(province);
											accountWeixinModel.setCity("");
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
											RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil
													.getRsAccountWeixinModelResponse(accountWeixinModel);
											rsAccountWeixinModelResponseBuilder.setNeedDb(true);
											rsAccountResponseBuilder.setRsAccountWeixinModelResponse(rsAccountWeixinModelResponseBuilder);
											//
											redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
											RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
													ERedisTopicType.topicAll);
											// ==========================

											AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse
													.newBuilder();
											// 刷新玩家头像和昵称
											if (accountWeixinModel.getNickname() != null) {
												if (!accountWeixinModel.getNickname().equals(old_nickname)) {
													AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
															.getAccountPropertyResponse(EPropertyType.ACCOUNT_NICK_NAME.getId(), null, null, null,
																	null, null, accountWeixinModel.getNickname(), null, null);
													accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
												}
											}
											if (accountWeixinModel.getHeadimgurl() != null) {
												if (!accountWeixinModel.getHeadimgurl().equals(old_headimgurl)) {
													AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
															.getAccountPropertyResponse(EPropertyType.ACCOUNT_HEAR_IMG.getId(), null, null, null,
																	null, null, accountWeixinModel.getHeadimgurl(), null, null);
													accountPropertyListResponseBuilder.addAccountProperty(accountPropertyResponseBuilder);
												}
											}
											if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList().size() > 0) {
												Response.Builder responseBuilder = Response.newBuilder();
												responseBuilder.setResponseType(ResponseType.PROPERTY);
												responseBuilder.setExtension(Protocol.accountPropertyListResponse,
														accountPropertyListResponseBuilder.build());
												PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
											}
										}
									}

								}
							}

						}
					}
				}
			});
			try {
				if (account.getHallRecommendModel().getAccount_id() == 0 && account.getHallRecommendModel().getTarget_account_id() == 0) {
					if (account.getAccountWeixinModel() != null) {
						ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
						int res = centerRMIServer.setRecommender(account.getAccountWeixinModel().getUnionid());
						if (res == 0) {
							long recommend_id = centerRMIServer.getRecommendId(account.getAccount_id());
							if (recommend_id > 0) {
								GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
								goldCardAllResponseBuilder.setType(8);
								goldCardAllResponseBuilder.setTargetAccountId(recommend_id);
								Response.Builder responseBuilder = Response.newBuilder();
								responseBuilder.setResponseType(ResponseType.GOLD_CARD);
								responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
								session.send(responseBuilder.build());
							}
						}
					}
				}
				// RecommendService.getInstance().hallSendGold(account.getAccount_id());
				boolean sendOk = RecommendService.getInstance().SendGold(account.getAccount_id());
				if (sendOk) {
					GoldCardAllResponse.Builder goldCardAllResponseBuilder = GoldCardAllResponse.newBuilder();
					goldCardAllResponseBuilder.setType(9);
					goldCardAllResponseBuilder.setGetPrize(1);
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.GOLD_CARD);
					responseBuilder.setExtension(Protocol.goldCardAllResponse, goldCardAllResponseBuilder.build());
					session.send(responseBuilder.build());
				}
			} catch (Exception e) {
				// 送豆返利
				logger.error("error RecommendService.getInstance().SendGold " + account.getAccount_id());
			}
			////////////////////////////////////////////////////////////////

		}

		else if (type == CLIENT_IP) {
			if (!request.hasClientIp()) {
				logger.error("CLIENT_IP !request.hasClientIp()");

				return;
			}

			String ip = request.getClientIp();
			// 验证
			boolean flag = RegexUtil.isboolIP(ip);
			if (!flag) {
				logger.error("CLIENT_IP RegexUtil.isboolIP(ip),{},accont:{}", ip);
				return;
			}

			Account account = session.getAccount();
			SessionUtil.setAttr(session, AttributeKeyConstans.IP_ACCOUNT, ip);
			if (account == null) {
				logger.error("CLIENT_IP account == null");
				return;
			}

			LoginResponse.Builder loginResponsebuilder = LoginResponse.newBuilder();
			loginResponsebuilder.setType(5);
			loginResponsebuilder.setClientIp(ip);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.LOING);
			responseBuilder.setExtension(Protocol.loginResponse, loginResponsebuilder.build());
			session.send(responseBuilder.build());

			// 放到缓存

			AccountModel accountModel = account.getAccountModel();
			accountModel.setClient_ip2(ip);
			accountModel.setClient_ip(ip);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			//
			RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse.newBuilder();
			rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountModelResponseBuilder.setClientIp2(accountModel.getClient_ip2());
			rsAccountModelResponseBuilder.setClientIp(ip);
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);

			// 日志
			String ip_addr = IPSeeker.getInstance().getAddress(accountModel.getClient_ip2());
			MongoDBServiceImpl.getInstance().player_log_clientIP(account.getAccount_id(), ELogType.clientNotifyIpNEW,
					"客户端传ip:" + accountModel.getClient_ip2() + ",地区:" + ip_addr, null, null, accountModel.getClient_ip());
		}

		else if (type == CLIENT_IP_LOGIN) {
			Account account = session.getAccount();
			if (account == null) {
				logger.error("CLIENT_IP_LOGIN account == null");
				return;
			}

			if (!request.hasClientIp()) {
				logger.error("CLIENT_IP_LOGIN !request.hasClientIp()");
				return;
			}

			String ip = request.getClientIp();

			MongoDBServiceImpl.getInstance().player_log_notifyIP(account.getAccount_id(), ELogType.clientNotifyLoginIpNEW, "客户端传ip:" + ip, null, null,
					null);
		}

		else {
			logger.error("未知的类型LoginHandler" + request.toString());
		}
	}

	private void addAccountLoginMission(Account account) {
		// 用户登录（包括老用户）
		FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.LOGIN, 1, 1);
		// 新用户注册登录
		Date create_time = account.getAccountModel().getCreate_time();
		if (create_time != null) {
			boolean isSameDay = TimeUtil.isOverOneDay(create_time);
			if (isSameDay) {
				FoundationService.getInstance().sendActivityMissionProcess(account.getAccount_id(), ActivityMissionTypeEnum.NEW_PLAYER_FIRSTLOGIN, 1,
						1);
			}
		}
	}

	/**
	 * 登录后发送最后的公告信息
	 * 
	 * @param account
	 */
	private void sendLastNotice(Account account, C2SSession session) {
	}

	/**
	 * 平台转码登录失败返回
	 * 
	 * @param selfToken
	 */
	private void sendSelfToketLoginFail(String selfToken, C2SSession session, int step) {
		LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
		loginResponse.setType(3);
		loginResponse.setErrorCode(step);
		Integer loginType = SessionUtil.getAttr(session, AttributeKeyConstans.LOGIN_TYPE);
		if (null != loginType) {
			loginResponse.setLoginType(loginType.intValue());
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.LOING);
		responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
		session.send(responseBuilder.build());
		logger.info("平台转码的微信登录失败:selfToken:" + selfToken + "\t" + step);
	}
}
