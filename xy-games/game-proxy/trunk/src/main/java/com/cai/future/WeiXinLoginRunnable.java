/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.future;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.AccountConstant;
import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EPtType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.EWxHeadimgurlType;
import com.cai.common.define.LoginType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountRecommendModel;
import com.cai.common.domain.AccountWeixinModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.EmojiFilter;
import com.cai.common.util.IpUtil;
import com.cai.common.util.ModelToRedisUtil;
import com.cai.common.util.MyStringUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.UUIDUtils;
import com.cai.common.util.WxUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamDict;
import com.cai.handler.LoginHandler;
import com.cai.module.LoginModule;
import com.cai.module.LoginMsgSender;
import com.cai.module.RoomModule;
import com.cai.service.FoundationService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.RecommendService;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.ip.IPSeeker;
import com.google.common.base.Strings;
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
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsAccountWeixinModelResponse;

/**
 * 
 *
 * @author DIY date: 2017年12月16日 下午3:13:49 <br/>
 */
public class WeiXinLoginRunnable implements Runnable {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private C2SSession session;

	private LoginRequest request;

	private int game_index_tmp;

	private String client_flag_tmp;
	private String client_version_tmp;

	private long createTime;

	/**
	 * @param session
	 * @param request
	 * @param game_index_tmp
	 * @param client_flag_tmp
	 * @param client_version_tmp
	 */
	public WeiXinLoginRunnable(C2SSession session, LoginRequest request, int game_index_tmp, String client_flag_tmp, String client_version_tmp) {
		this.session = session;
		this.request = request;
		this.game_index_tmp = game_index_tmp;
		this.client_flag_tmp = client_flag_tmp;
		this.client_version_tmp = client_version_tmp;

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
		if (pass > 6000) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录超时,请重试").build());
			logger.error("微信登录超时大于3秒........." + pass);
			return;
		}

		PerformanceTimer timer = new PerformanceTimer();
		try {
			SessionUtil.setAttr(session, AttributeKeyConstans.LOGIN_TYPE, LoginType.WX);

			int type = request.getType();

			JSONObject jsonObject = null;
			String wx_code = request.getWxCode();
			if (type == LoginHandler.WX_NEW_LOGIN || type == LoginHandler.WX_LITTLE_GAME_LOGIN) {
				String accessToken = request.getAccessTokenJson();
				if (Strings.isNullOrEmpty(accessToken)) {
					session.send(MessageResponse.getMsgAllResponse("登录失败,accessToken无效").build());
					return;
				}
				jsonObject = JSON.parseObject(request.getAccessTokenJson());
			} else {
				jsonObject = PtAPIServiceImpl.getInstance().wxGetAccessTokenByCodeChannel(wx_code, game_index_tmp, request.getChannelId());
			}
			if (jsonObject == null) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败Error1!").build());
				LoginMsgSender.sendLoginFailedRsp(session, 1, 1);
				return;
			}

			Integer errCode = 0;
			if (jsonObject.containsKey("errcode")) {
				errCode = (Integer) jsonObject.get("errcode");
			}

			if (errCode != 0) {
				logger.error("登录失败wx_code==" + wx_code + "errCode==" + errCode + "msg" + jsonObject);
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,code无效").build());
				LoginMsgSender.sendLoginFailedRsp(session, 1, 1);
				return;
			}

			String access_token = jsonObject.getString("access_token");
			int expires_in = jsonObject.getInteger("expires_in");
			String refresh_token = jsonObject.getString("refresh_token");
			String openid = jsonObject.getString("openid");
			String scope = jsonObject.getString("scope");
			String unionid = jsonObject.getString("unionid");

			// 用户详情
			if (type == LoginHandler.WX_NEW_LOGIN || type == LoginHandler.WX_LITTLE_GAME_LOGIN) {

				String accessUserJson = request.getAccessUserJson();
				if (Strings.isNullOrEmpty(accessUserJson)) {
					session.send(MessageResponse.getMsgAllResponse("登录失败,userToken无效").build());
					return;
				}
				jsonObject = JSON.parseObject(accessUserJson);
			} else {
				jsonObject = PtAPIServiceImpl.getInstance().wxUserinfo(game_index_tmp, access_token, openid);
			}

			if (jsonObject == null) {
				logger.error("登录失败wx_code==" + wx_code + "errCode==" + errCode);
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败Error2!").build());
				LoginMsgSender.sendLoginFailedRsp(session, 1, 1);
				return;
			}

			if (jsonObject.containsKey("errcode")) {
				errCode = (Integer) jsonObject.get("errcode");
			}
			if (errCode != 0) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,token无效").build());
				LoginMsgSender.sendLoginFailedRsp(session, 1, 1);
				return;
			}

			String thirdToken = UUIDUtils.getUUID32();
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
			unionid = jsonObject.getString("unionid");// 全平台唯一id

			// 微信小游戏，没有unionid
			if (type == LoginHandler.WX_LITTLE_GAME_LOGIN) {
				unionid = String.format("MGAME_%s", openid);
			}
			String accounName = EPtType.WX.getId() + "_" + unionid;

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			Account account = centerRMIServer.getAndCreateAccount(EPtType.WX.getId(), accounName, session.getClientIP(), client_flag_tmp,
					client_version_tmp, SystemConfig.proxy_index);

			if (account == null) {
				PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录失败,Error3").build());
				LoginMsgSender.sendLoginFailedRsp(session, 1, 1);
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

			// 俱乐部微信头像/昵称刷新问题
			if (null != accountWeixinModel) {
				if (!Objects.equals(headimgurl, accountWeixinModel.getHeadimgurl()) || !Objects.equals(nickname, accountWeixinModel.getNickname())) {
					LoginModule.syncUpdateClubMemberInfo(accountModel.getAccount_id(), headimgurl, nickname);
				}
			}

			accountWeixinModel.setAccess_token(access_token);
			accountWeixinModel.setRefresh_token(refresh_token);
			accountWeixinModel.setOpenid(openid);
			accountWeixinModel.setScope(scope);
			accountWeixinModel.setUnionid(unionid);
			accountWeixinModel.setNickname(nickname);
			accountWeixinModel.setSex(sex);
			accountWeixinModel.setProvince("");
			accountWeixinModel.setCity("");
			accountWeixinModel.setCountry("");
			accountWeixinModel.setHeadimgurl(headimgurl);
			accountWeixinModel.setPrivilege("");
			accountWeixinModel.setLast_flush_time(new Date());
			accountWeixinModel.setSelf_token(LoginModule.encodeToken(accountWeixinModel.getAccount_id()));
			accountWeixinModel.setLast_false_self_token(new Date());
			// String ip = session.getClientIP();
			String ip = request.getClientIp();
			String ip_addr = IPSeeker.getInstance().getAddress(ip);

			// ===========客户端动态参数===============
			SysParamModel sysParamModel2000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_index_tmp).get(2000);
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
												.getSysParamModelDictionaryByGameId(game_index_tmp).get(2004);
										int addGold = sysParamModel2004.getVal2();
										AccountRecommendModel accountRecommendModel = new AccountRecommendModel();
										accountRecommendModel.setAccount_id(uid);
										accountRecommendModel.setTarget_account_id(account.getAccount_id());
										accountRecommendModel.setCreate_time(new Date());
										accountRecommendModel.setGold_num(addGold);
										accountRecommendModel.setTarget_name(accountWeixinModel.getNickname());
										String icon = WxUtil.changHeadimgurl(accountWeixinModel.getHeadimgurl(), EWxHeadimgurlType.S132);
										accountRecommendModel.setUpdate_time(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
										accountRecommendModel.setTarget_icon(icon);
										boolean flag = centerRMIServer.addAccountRecommendModel(accountRecommendModel);
										if (flag) {
											// 给好友加金币
											// centerRMIServer.addAccountGold(uid,
											// addGold, false,
											// "分享好友下载,好友account_id:"
											// +
											// account.getAccount_id(),
											// EGoldOperateType.FRIEND_DOWN);
											// 给自己加金币,登录成功后的地方给
											RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
											redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
											RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
											rsAccountResponseBuilder.setAccountId(account.getAccount_id());
											RsAccountParamModelResponse.Builder rsAccountParamModelResponse = RsAccountParamModelResponse
													.newBuilder();
											rsAccountParamModelResponse.setAccountId(account.getAccount_id());
											rsAccountParamModelResponse.setType(EAccountParamType.ADD_RECOMMEND_GOLD.getId());
											rsAccountParamModelResponse.setData1(System.currentTimeMillis());
											rsAccountParamModelResponse.setVal1(1);// 有推荐获豆资格
											rsAccountParamModelResponse.setNeedDb(true);
											rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse);
											redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
											RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
													ERedisTopicType.topicCenter);
											// 开启邀请新用户送红包活动
											RecommendService.getInstance().invite(uid, account.getAccount_id(), headimgurl);
										}
										try {
											FoundationService.getInstance().sendInviteFriendActivityMissionProcess(account.getAccount_id(), uid);
										} catch (Exception e) {
											logger.error("邀请下载任务通知失败", e);
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
			LoginModule.loginReset(account, session);
			accountModel.setLast_login_time(new Date());
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
			rsAccountModelResponseBuilder.setLoginTimes(accountModel.getLogin_times());
			rsAccountModelResponseBuilder.setLastClientFlag(accountModel.getLast_client_flag());
			rsAccountModelResponseBuilder.setClientVersion(accountModel.getClient_version());
			rsAccountModelResponseBuilder.setClientIp(accountModel.getClient_ip());
			rsAccountModelResponseBuilder.setClientIp2(accountModel.getClient_ip2());
			rsAccountModelResponseBuilder.setRecommendId(accountModel.getRecommend_id());
			rsAccountModelResponseBuilder.setLoginReward(accountModel.getLogin_reward());
			rsAccountModelResponseBuilder.setChannelId(accountModel.getLast_channel());
			rsAccountModelResponseBuilder.setThirdToken(thirdToken);
			rsAccountModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountModelResponse(rsAccountModelResponseBuilder);
			//
			RsAccountWeixinModelResponse.Builder rsAccountWeixinModelResponseBuilder = ModelToRedisUtil
					.getRsAccountWeixinModelResponse(accountWeixinModel);
			rsAccountWeixinModelResponseBuilder.setNeedDb(true);
			rsAccountResponseBuilder.setRsAccountWeixinModelResponse(rsAccountWeixinModelResponseBuilder);
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);
			// ==========================

			session.setAccountLoginTime(System.currentTimeMillis());

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
			loginResponse.setLoginType(LoginType.WX);
			loginResponse.setThirdToken(thirdToken);
			loginResponse.setErrorCode(0);
			if (RoomModule.checkHasRoom(account, session)) {
				loginResponse.setRoomStatus(1); // 有房间
			}

			LoginModule.assignCityCode(request.getClientIp(), loginResponse);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.LOING);
			responseBuilder.setExtension(Protocol.loginResponse, loginResponse.build());
			PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

			MongoDBServiceImpl.getInstance().player_log(account.getAccount_id(), ELogType.login,
					"微信登录:account_id:" + account.getAccount_id() + ",IPAddr:" + ip_addr, 1L, timer.get(), session.getClientIP());
		} catch (Exception e) {
			PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("登录异常Error-E201").build());
			logger.error("error" + e);
			throw e;
		}
	}

}
