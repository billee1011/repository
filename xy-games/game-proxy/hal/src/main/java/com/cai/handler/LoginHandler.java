package com.cai.handler;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPropertyType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.LoginNoticeModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.IpUtil;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RegexUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.WxUtil;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.IPGroupDict;
import com.cai.dictionary.LoginNoticeDict;
import com.cai.dictionary.SysParamDict;
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

	public static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);

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

	/**
	 * 客户端ip上传
	 */
	private static final int CLIENT_IP = 4;
	
	
	/**
	 * 客户端上传登录ip
	 */
	private static final int CLIENT_IP_LOGIN = 5;

	@Override
	public void onRequest() throws Exception {

		int type = request.getType();
		
		// 操作频率控制
		if (!session.isCanRequest("LoginHandler_" + type, 500L)) {
			logger.error("!session.isCanRequest()!");
			return;
		}

		int game_index = 1;
		if (request.hasGameIndex()) {
			game_index = request.getGameIndex();
		}

		// 现在只有1,2
		if (EGameType.getEGameType(game_index)==null) {
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

			if (!request.hasWxCode()) {
				logger.error("!request.hasWxCode()!");
				return;
			}

			///////////////////////////////////////////////////////////////////////////////////////////
			Global.getWxService().execute(new Runnable() {

				@Override
				public void run() {
					try {
						String wx_code = request.getWxCode();
						JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxGetAccessTokenByCode(wx_code,
								game_index_tmp);
						if (jsonObject == null) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("登录失败Error1!").build());
							loginError(1,1);
							return;
						}

						Integer errCode = 0;
						if (jsonObject.containsKey("errcode")) {
							errCode = (Integer) jsonObject.get("errcode");
						}

						if (errCode != 0) {
							logger.error("登录失败wx_code==" + wx_code + "errCode==" + errCode + "msg" + jsonObject);
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("登录失败,code无效").build());
							loginError(1,1);
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
							logger.error("登录失败wx_code==" + wx_code + "errCode==" + errCode);
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("登录失败Error2!").build());
							loginError(1,1);
							return;
						}

						if (jsonObject.containsKey("errcode")) {
							errCode = (Integer) jsonObject.get("errcode");
						}
						if (errCode != 0) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("登录失败,token无效").build());
							loginError(1,1);
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
						Account account = centerRMIServer.getAndCreateAccount(EPtType.WX.getId(), accounName,
								session.getClientIP(), client_flag_tmp, client_version_tmp);

						if (account == null) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("登录失败,Error3").build());
							loginError(1,1);
							return;
						}

						AccountModel accountModel = account.getAccountModel();

						// 封号检测
						if (accountModel.getBanned() == 1) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
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
						accountWeixinModel.setCity("");
						accountWeixinModel.setCountry(country);
						accountWeixinModel.setHeadimgurl(headimgurl);
						accountWeixinModel.setPrivilege(privilege);
						accountWeixinModel.setLast_flush_time(new Date());
						accountWeixinModel.setSelf_token(
								accountWeixinModel.getAccount_id() + "-" + RandomUtil.getRandomString(20));
						accountWeixinModel.setLast_false_self_token(new Date());

						String ip = session.getClientIP();
						String ip_addr = IPSeeker.getInstance().getAddress(ip);

						// ===========客户端动态参数===============
						SysParamModel sysParamModel2000 = SysParamDict.getInstance()
								.getSysParamModelDictionaryByGameId(game_index_tmp).get(2000);
						if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
							if (request.hasStrParam()) {
								try {
									// 首次登录
									// if(accountModel.getLogin_times()==0){
									String strParam = request.getStrParam();
									if (strParam != null && !"".equals(strParam.trim())) {
										// 转成json
										JSONObject jobj = JSONObject.parseObject(strParam);
										Long uid = jobj.getLong("uid");
										// 查看账号是否存在
										if (uid != accountModel.getAccount_id()) {
											Account targetAccount = centerRMIServer.getAccount(uid);
											if (targetAccount != null) {
												if (accountModel.getRecommend_id() == 0L) {
													accountModel.setRecommend_id(uid);
													// 活动相关
													SysParamModel sysParamModel2004 = SysParamDict.getInstance()
															.getSysParamModelDictionaryByGameId(game_index_tmp)
															.get(2004);
													int addGold = sysParamModel2004.getVal2();
													AccountRecommendModel accountRecommendModel = new AccountRecommendModel();
													accountRecommendModel.setAccount_id(uid);
													accountRecommendModel.setTarget_account_id(account.getAccount_id());
													accountRecommendModel.setCreate_time(new Date());
													accountRecommendModel.setGold_num(addGold);
													accountRecommendModel
															.setTarget_name(accountWeixinModel.getNickname());
													String icon = WxUtil.changHeadimgurl(
															accountWeixinModel.getHeadimgurl(), EWxHeadimgurlType.S46);
													accountRecommendModel.setTarget_icon(icon);
													boolean flag = centerRMIServer
															.addAccountRecommendModel(accountRecommendModel);
													if (flag) {
														// 给好友加金币
														centerRMIServer.addAccountGold(uid, addGold, false,
																"分享好友下载,好友account_id:" + account.getAccount_id(),
																EGoldOperateType.FRIEND_DOWN);
														// 给自己加金币,登录成功后的地方给
													}
												}
											}
										}
									}
									// }
								} catch (Exception e) {
									logger.error("error", e);
								}
							}
						}
						// ===================================

						// 放到缓存中
						account.setCacheCreateTime(System.currentTimeMillis());
						account.setLastProxyIndex(SystemConfig.proxy_index);
						account.setGame_id(game_index_tmp);
						reset_login(account);
						accountModel.setLast_login_time(new Date());
						accountModel.setLogin_times(accountModel.getLogin_times() + 1);
						accountModel.setLast_client_flag(client_flag_tmp);
						accountModel.setClient_version(client_version_tmp);
						accountModel.setClient_ip(ip);
						// 如果不是白名单的直接赋值
						if (!IpUtil.isWhiteIp(ip)) {
							accountModel.setClient_ip2(ip);
						} else {
							if (accountModel.getClient_ip2() == null || "".equals(accountModel.getClient_ip2())) {
								accountModel.setClient_ip2(RandomUtil.randomIp());
							}
						}

						// ========同步到中心========
						RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
						redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
						//
						RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
						rsAccountResponseBuilder.setAccountId(account.getAccount_id());
						rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
						rsAccountResponseBuilder.setGameId(game_index_tmp);
						//
						RsAccountModelResponse.Builder rsAccountModelResponseBuilder = RsAccountModelResponse
								.newBuilder();
						rsAccountModelResponseBuilder.setAccountId(account.getAccount_id());
						rsAccountModelResponseBuilder.setLastLoginTime(accountModel.getLast_login_time().getTime());
						rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
						rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
						rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
						rsAccountModelResponseBuilder.setClientIp(accountModel.getClient_ip());
						rsAccountModelResponseBuilder.setClientIp2(accountModel.getClient_ip2());
						rsAccountModelResponseBuilder.setRecommendId(accountModel.getRecommend_id());
						rsAccountModelResponseBuilder.setLoginReward(accountModel.getLogin_reward());
						rsAccountModelResponseBuilder.setNeedDb(true);
						rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
						//
						RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil
								.getRsAccountWeixinModelResponse(accountWeixinModel);
						rsAccountWeixinModelResponseBuilder.setNeedDb(true);
						rsAccountResponseBuilder.setRsAccountWeixinModelResponse(rsAccountWeixinModelResponseBuilder);
						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
								ERedisTopicType.topicAll);
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

						LoginItemResponse.Builder loginItemResponBuilder = MessageResponse.getLoginItemResponse(account,
								1);
						// 属性列表
						List<AccountPropertyResponse> list = MessageResponse
								.getSysAccountPropertyResponseList(game_index_tmp, account);
						loginItemResponBuilder.addAllAccountProperty(list);

						wxResponse.setLoginItemResponse(loginItemResponBuilder);
						loginResponse.setWxLoginItemResponse(wxResponse);
						loginResponse.setErrorCode(0);

						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.LOING);
						responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
						PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

						MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
								"微信登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 1L, timer.get(),
								session.getClientIP());

					} catch (Exception e) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session,
								MessageResponse.getMsgAllResponse("登录异常Error-E201").build());
						logger.error("error"+e);
						throw e;
					}
				}
			});

			///////////////////////////////////////////////////////////////////////////////////////////

		}

		else if (type == PT_CACHE_WX_LOGIN) {

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
					this.sendSelfToketLoginFail(selfToken);
					return;
				}

				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				Account account = centerRMIServer.getAccount(account_id);

				if (account == null || !account.getAccountModel().getPt().equals(EPtType.WX.getId())) {
					this.sendSelfToketLoginFail(selfToken);
					logger.error("account == null ||this.sendSelfToketLoginFail(selfToken)");
					return;
				}

				AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
				if (accountWeixinModel == null) {
					this.sendSelfToketLoginFail(selfToken);
					logger.error("发现账号异常,没有微信记录:account_id:" + account.getAccount_id());
					return;
				} else {
					if (accountWeixinModel.getLast_false_self_token() == null
							|| accountWeixinModel.getSelf_token() == null) {
						this.sendSelfToketLoginFail(selfToken);
						logger.error(
								"accountWeixinModel.getLast_false_self_token() == null ||this.sendSelfToketLoginFail(selfToken)");
						return;
					}

					// long k1 =
					// accountWeixinModel.getLast_false_self_token().getTime() +
					// 1000L * 60 * 60 * 24 * 30;//30天
					long k1 = accountWeixinModel.getLast_false_self_token().getTime() + 1000L * 60 * 60 * 24 * 20;// 20天
					if (!selfToken.equals(accountWeixinModel.getSelf_token()) || k1 < System.currentTimeMillis()) {
						this.sendSelfToketLoginFail(selfToken);
						logger.error(" k1 < System.currentTimeMillis()||this.sendSelfToketLoginFail(selfToken)");

						return;
					}

					AccountModel accountModel = account.getAccountModel();

					// 封号检测
					if (accountModel.getBanned() == 1) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session,
								MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
						return;
					}

					String ip = session.getClientIP();
					String ip_addr = IPSeeker.getInstance().getAddress(ip);

					// 放到缓存中
					account.setCacheCreateTime(System.currentTimeMillis());
					account.setLastProxyIndex(SystemConfig.proxy_index);
					account.setGame_id(game_index);
					reset_login(account);
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
					rsAccountModelResponseBuilder.setNeedDb(true);
					rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
					//
					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
							ERedisTopicType.topicAll);
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
					List<AccountPropertyResponse> list = MessageResponse.getSysAccountPropertyResponseList(game_index,
							account);
					loginItemResponBuilder.addAllAccountProperty(list);

					wxResponse.setLoginItemResponse(loginItemResponBuilder);
					loginResponse.setWxLoginItemResponse(wxResponse);
					loginResponse.setErrorCode(0);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.LOING);
					responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
					send(responseBuilder.build());

					MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
							"微信缓存登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 2L, timer.get(),
							session.getClientIP());

				}

			} catch (Exception e) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session,
						MessageResponse.getMsgAllResponse("登录异常Error-E202").build());
				throw e;
			}

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

			String accounName = EPtType.SELF.getId() + "_" + imei;

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAndCreateAccount(EPtType.SELF.getId(), accounName,
					session.getClientIP(), client_flag, client_version);
			AccountModel accountModel = account.getAccountModel();

			// 封号检测
			if (accountModel.getBanned() == 1) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session,
						MessageResponse.getMsgAllResponse("账号被封，请联系客服！").build());
				return;
			}

			String ip = session.getClientIP();
			String ip_addr = IPSeeker.getInstance().getAddress(ip);

			// 放到缓存中
			account.setCacheCreateTime(System.currentTimeMillis());
			account.setLastProxyIndex(SystemConfig.proxy_index);
			account.setGame_id(game_index);
			reset_login(account);
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
			// rsAccountModelResponseBuilder.setRecommendId(accountModel.getRecommend_id());
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicAll);
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

			MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
					"快速登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 3L, timer.get(),
					session.getClientIP());
			// 测试
			// send(MessageResponse.getMsgAllResponse("这是一个提示消息提示!").build());

		}

		else if (type == LOGIN_SUCCESS) {
			Account account = session.getAccount();
			if (account == null) {
				logger.error("login_success account == null");
				return;
			}
//			addRecommendPlayerIncome(account);//推广员推荐玩家，上级返现
			AccountModel accountModel = account.getAccountModel();
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			// 最后的公告
			sendLastNotice(account);
			if(account.getGame_id()==0){
				account.setGame_id(1);
			}
			// 是否领取分享下载金币
			SysParamModel sysParamModel2000 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(account.getGame_id()).get(2000);
			if (sysParamModel2000 != null && sysParamModel2000.getVal1() == 1) {
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
						centerRMIServer.addAccountGold(account.getAccount_id(), addGold, false,
								"通过分享下载,分享人account_id:" + accountModel.getRecommend_id(), EGoldOperateType.SHARE_DOWN);
					}
				}
			}

			// ==================登录公告===========================
			// 是否是重连 0=正常 >0重连
			int reconnet = 1;
			if (request.hasReconnect()) {
				reconnet = request.getReconnect();
			}

			if (reconnet == 0) {

				Date now = MyDateUtil.getNow();
				LoginNoticeModel loginNoticeModel = LoginNoticeDict.getInstance().getLoginNoticeDict()
						.get(account.getGame_id());
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

						LoginNoticeResponse.Builder LoginNoticeResponseBuilder = MessageResponse
								.getLoginNoticeResponse(loginNoticeModel);
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
						RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse
								.newBuilder();
						rsAccountParamModelResponse.setAccountId(account.getAccount_id());
						rsAccountParamModelResponse.setType(EAccountParamType.LAST_LOGIN_NOTICE.getId());
						rsAccountParamModelResponse.setData1(accountParamModel.getDate1().getTime());
						rsAccountParamModelResponse.setNeedDb(true);
						rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
						//
						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
								ERedisTopicType.topicCenter);
					}

				}

			}
			// =======================================================

			IPGroupDict.getInstance().updateIpToPlayer(session);
			// ==================================================

			////////////////////////////////////////////////////////////////////////
			// 调用其它线程池
			Global.getWxService().execute(new Runnable() {
				@Override
				public void run() {
					AccountWeixinModel accountWeixinModel = account.getAccountWeixinModel();
					if (accountWeixinModel != null) {
						// 是否要刷新微信登录缓存,30分钟刷新一次
						Date last_flush_time = accountWeixinModel.getLast_flush_time();
						if (last_flush_time == null
								|| last_flush_time.getTime() + 1000L * 60 * 30 < MyDateUtil.getNow().getTime()) {

							// 旧值
							String old_nickname = accountWeixinModel.getNickname();
							String old_headimgurl = accountWeixinModel.getHeadimgurl();

							JSONObject jsonObject = PtAPIServiceImpl.getInstance().wxFlushToken(account.getGame_id(),
									accountWeixinModel.getRefresh_token());
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

									jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(account.getGame_id(),
											access_token, openid);
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
											RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse
													.newBuilder();
											rsAccountResponseBuilder.setAccountId(account.getAccount_id());
											//
											RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil
													.getRsAccountWeixinModelResponse(accountWeixinModel);
											rsAccountWeixinModelResponseBuilder.setNeedDb(true);
											rsAccountResponseBuilder.setRsAccountWeixinModelResponse(
													rsAccountWeixinModelResponseBuilder);
											//
											redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
											RedisServiceImpl.getInstance().convertAndSendRsResponse(
													redisResponseBuilder.build(), ERedisTopicType.topicAll);
											// ==========================

											AccountPropertyListResponse.Builder accountPropertyListResponseBuilder = AccountPropertyListResponse
													.newBuilder();
											// 刷新玩家头像和昵称
											if (accountWeixinModel.getNickname() != null) {
												if (!accountWeixinModel.getNickname().equals(old_nickname)) {
													AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
															.getAccountPropertyResponse(
																	EPropertyType.ACCOUNT_NICK_NAME.getId(), null, null,
																	null, null, null, accountWeixinModel.getNickname(),
																	null, null);
													accountPropertyListResponseBuilder
															.addAccountProperty(accountPropertyResponseBuilder);
												}
											}
											if (accountWeixinModel.getHeadimgurl() != null) {
												if (!accountWeixinModel.getHeadimgurl().equals(old_headimgurl)) {
													AccountPropertyResponse.Builder accountPropertyResponseBuilder = MessageResponse
															.getAccountPropertyResponse(
																	EPropertyType.ACCOUNT_HEAR_IMG.getId(), null, null,
																	null, null, null,
																	accountWeixinModel.getHeadimgurl(), null, null);
													accountPropertyListResponseBuilder
															.addAccountProperty(accountPropertyResponseBuilder);
												}
											}
											if (accountPropertyListResponseBuilder.getAccountPropertyBuilderList()
													.size() > 0) {
												Response.Builder responseBuilder = Response.newBuilder();
												responseBuilder.setResponseType(ResponseType.PROPERTY);
												responseBuilder.setExtension(Protocol.accountPropertyListResponse,
														accountPropertyListResponseBuilder.build());
												PlayerServiceImpl.getInstance().sendAccountMsg(session,
														responseBuilder.build());
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

		else if (type == CLIENT_IP) {

			Account account = session.getAccount();
			if (account == null) {
				logger.error("CLIENT_IP account == null");

				return;
			}

			if (!request.hasClientIp()) {
				logger.error("CLIENT_IP !request.hasClientIp()");

				return;
			}

			String ip = request.getClientIp();
			// 验证
			boolean flag = RegexUtil.isboolIP(ip);
			if (!flag) {
				logger.error("CLIENT_IP RegexUtil.isboolIP(ip)");
				return;
			}

			LoginResponse.Builder loginResponsebuilder = LoginResponse.newBuilder();
			loginResponsebuilder.setType(5);
			loginResponsebuilder.setClientIp(ip);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.LOING);
			responseBuilder.setExtension(Protocol.loginResponse, loginResponsebuilder.build());
			send(responseBuilder.build());

			// 放到缓存

			AccountModel accountModel = account.getAccountModel();
			accountModel.setClient_ip2(ip);

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
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicAll);

			// 日志
			String ip_addr = IPSeeker.getInstance().getAddress(accountModel.getClient_ip2());
			MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.clientNotifyIp,
					"客户端传ip:" + accountModel.getClient_ip2() + ",地区:" + ip_addr, null, null,
					accountModel.getClient_ip());
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

			MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.clientNotifyLoginIp,
					"客户端传ip:" + ip ,null, null,
					null);
		}
		
		else {
			logger.error("未知的类型LoginHandler" + request.toString());
		}
		

	}
//	private void addRecommendPlayerIncome(Account account){
//		try{
//			AccountParamModel accountParamModel = PublicServiceImpl.getInstance().getAccountParamModel(account,
//					EAccountParamType.UP_RECOMMEND_PLAYER_INCOME);
//			if(accountParamModel.getVal1() !=null&&accountParamModel.getVal1()==1){
//				return ;
//			}
//			accountParamModel.setVal1(1);
//			// ========同步到中心========
//			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
//			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
//			//
//			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
//			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
//			//
//			RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse
//					.newBuilder();
//			rsAccountParamModelResponse.setAccountId(account.getAccount_id());
//			rsAccountParamModelResponse.setType(EAccountParamType.UP_RECOMMEND_PLAYER_INCOME.getId());
//			rsAccountParamModelResponse.setVal1(accountParamModel.getVal1());
//			rsAccountParamModelResponse.setNeedDb(true);
//			rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
//			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
//			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
//					ERedisTopicType.topicCenter);
//			// 活动相关
//			AccountModel accountModel = account.getAccountModel();
//			if(accountModel.getRecommend_id()==0){
//				return;//无推荐人
//			}
//			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
//			Account recommendAccount = centerRMIServer.getAccount(accountModel.getRecommend_id());
//			int level = recommendAccount.getAccountModel().getRecommend_level();
//			if(recommendAccount== null||level==0){
//				return;//推荐人不存在或推荐人不是推广员
//			}
//			SysParamModel sysParamModel6001 = SysParamDict.getInstance()
//					.getSysParamModelDictionaryByGameId(account.getGame_id()).get(6001);
//			if (sysParamModel6001 == null){
//				return;
//			}
//			if(level == 1){
//				logger.info(recommendAccount.getAccount_id()+" 一级推广员推荐新玩家返利:"+sysParamModel6001.getVal1()/10.0);
//				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal1()/10.0,0l, "推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
//				return;
//			}else if(level == 2){
//				logger.info(recommendAccount.getAccount_id()+" 二级推广员推荐新玩家返利:"+sysParamModel6001.getVal2()/10.0);
//				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal2()/10.0, 0l,"推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
//				if(recommendAccount.getAccountModel().getRecommend_id()!=0){
//					Account recommendAccountUp = centerRMIServer.getAccount(recommendAccount.getAccountModel().getRecommend_id());
//					if(recommendAccountUp.getAccountModel().getRecommend_level()==1){
//						logger.info(recommendAccount.getAccount_id()+" 二级推广员推荐新玩家返利:"+sysParamModel6001.getVal4()/10.0);
//						centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(), sysParamModel6001.getVal4()/10.0,2l, "下级推广员推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
//						return;
//					}
//				}
//				
//			}else if(level == 3){
//				logger.info(recommendAccount.getAccount_id()+" 三级推广员推荐新玩家返利:"+sysParamModel6001.getVal3()/10.0);
//				centerRMIServer.doRecommendIncome(recommendAccount.getAccount_id(), sysParamModel6001.getVal3()/10.0,0l, "推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
//				if(recommendAccount.getAccountModel().getRecommend_id()!=0){
//					Account recommendAccountUp = centerRMIServer.getAccount(recommendAccount.getAccountModel().getRecommend_id());
//					if(recommendAccountUp.getAccountModel().getRecommend_level()==2){
//						logger.info(recommendAccount.getAccount_id()+" 三级推广员推荐新玩家返利:"+sysParamModel6001.getVal5()/10.0);
//						centerRMIServer.doRecommendIncome(recommendAccountUp.getAccount_id(), sysParamModel6001.getVal5()/10.0, 3l,"下级推广员推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
//						if(recommendAccountUp.getAccountModel().getRecommend_id()!=0){
//							Account recommendAccountUpUp = centerRMIServer.getAccount(recommendAccountUp.getAccountModel().getRecommend_id());
//							if(recommendAccountUpUp.getAccountModel().getRecommend_level()==1){
//								logger.info(recommendAccount.getAccount_id()+" 三级推广员推荐新玩家返利:"+sysParamModel6001.getVal4()/10.0);
//								centerRMIServer.doRecommendIncome(recommendAccountUpUp.getAccount_id(), sysParamModel6001.getVal4()/10.0,3l, "下级推广员推荐新玩家", EGoldOperateType.RECOMMEND_PLAYER);
//							}
//						}
//					}
//				}
//			}
//		}catch(Exception e){
//		}
//	}
		

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
	
	
	private void loginError(int type,int errorCode) {
		LoginResponse.Builder loginResponse = LoginResponse.newBuilder();
		loginResponse.setType(type);
		loginResponse.setErrorCode(errorCode);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.LOING);
		responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
		PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
		logger.info("微信登录:失败type="+type + errorCode);
	}
	
	private void reset_login(Account account) {
		try {
			Date lastLogin = account.getAccountModel().getLast_login_time();
			Date now = new Date();
			if(lastLogin!=null) {
				 Date lastZero = MyDateUtil.getZeroDate(lastLogin);
				 Date nowZero = MyDateUtil.getZeroDate(now);
				 int between = MyDateUtil.daysBetween(lastZero, nowZero);
				 if(between>1) {
					 	account.getAccountModel().setLogin_reward(0);
					 
						RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
						redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
						//
						RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
						rsAccountResponseBuilder.setAccountId(account.getAccount_id());

						RsAccountModelResponse.Builder rsAccountModelResponse = RsAccountModelResponse.newBuilder();
						rsAccountModelResponse.setAccountId(account.getAccount_id());
						rsAccountModelResponse.setNeedDb(true);
						rsAccountModelResponse.setLoginReward(0);
						rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponse);

						redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
								ERedisTopicType.topicAll);
				 }
			}
		}catch(Exception e) {
			logger.error("重置异常accountID="+account.getAccount_id());
		}
		
	}

}
