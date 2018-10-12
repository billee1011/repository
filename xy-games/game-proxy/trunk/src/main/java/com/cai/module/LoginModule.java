/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.module;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.S2SCmd;
import com.cai.common.constant.Symbol;
import com.cai.common.define.EAccountParamType;
import com.cai.common.define.EGameType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountModel;
import com.cai.common.domain.AccountParamModel;
import com.cai.common.domain.AppItem;
import com.cai.common.domain.GameRecommendIndexModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.BaiduLBSJsonModel;
import com.cai.common.util.IPAndCityCache;
import com.cai.common.util.MyDateUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.AppItemDict;
import com.cai.dictionary.GameRecommendDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.redis.service.RedisService;
import com.cai.service.C2SSessionService;
import com.cai.service.ClientServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.base.Strings;
import com.xianyi.framework.core.transport.netty.session.C2SSession;

import javolution.util.FastMap;
import protobuf.clazz.ClubMsgProto.ClubAccountProto;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.LoginResponse;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.SubAppItemResponse;
import protobuf.clazz.Protocol.UpdateSubAppItemResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountModelResponse;
import protobuf.redis.ProtoRedis.RsAccountParamModelResponse;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 
 *
 * @author wu_hc date: 2017年8月9日 上午11:00:28 <br/>
 */
public final class LoginModule {

	private static Logger logger = LoggerFactory.getLogger(LoginModule.class);

	final static String ipReg = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
	private static final Pattern ip_pattern = Pattern.compile(ipReg);

	/**
	 * 登陆成功尝试进房间
	 */
	public static volatile boolean tryEnterRoomWhenLoginSuccess = false;

	/**
	 * 
	 * @param account
	 * @param session
	 */
	public static void loginReset(Account account, C2SSession session) {
		try {
			C2SSessionService.getInstance().online(session, account);

			Date lastLogin = account.getAccountModel().getLast_login_time();
			Date now = new Date();
			if (lastLogin != null) {
				Date lastZero = MyDateUtil.getZeroDate(lastLogin);
				Date nowZero = MyDateUtil.getZeroDate(now);
				int between = MyDateUtil.daysBetween(lastZero, nowZero);
				if (between > 1) {
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
					AccountParamModel param = account.getAccountParamModelMap().get(EAccountParamType.TAKE_SHARE_DOUBLE_REWARD.getId());
					if (param != null && param.getVal1() > 0) {
						param.setVal1(0);
						RsAccountParamModelResponse.Builder rsAccountParamModelResponse_share = RsAccountParamModelResponse.newBuilder();
						rsAccountParamModelResponse_share.setAccountId(account.getAccount_id());
						rsAccountParamModelResponse_share.setType(EAccountParamType.TAKE_SHARE_DOUBLE_REWARD.getId());
						rsAccountParamModelResponse_share.setVal1(0);
						rsAccountParamModelResponse_share.setData1(System.currentTimeMillis());
						rsAccountParamModelResponse_share.setNeedDb(true);
						rsAccountResponseBuilder.addRsAccountParamModelResponseList(rsAccountParamModelResponse_share);
					}

					redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
					RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicCenter);
				}
			}
		} catch (Exception e) {
			logger.error("重置异常accountID=" + account.getAccount_id());
		}

	}

	/**
	 * 同步登陆状态到各个服
	 * 
	 * @param account
	 * @param gameIndex
	 */
	public static void syncLoginStatusr(final Account account, int gameIndex) {

		AccountModel accountModel = account.getAccountModel();

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(account.getAccount_id());
		rsAccountResponseBuilder.setLastProxyIndex(SystemConfig.proxy_index);
		rsAccountResponseBuilder.setGameId(gameIndex);
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
	}

	/**
	 * ip对应的城市编码
	 * 
	 * @param ip
	 */
	public static void assignCityCode(String ip, LoginResponse.Builder loginResponse) {
		if (StringUtils.isNotEmpty(ip) && ip_pattern.matcher(ip).matches()) {
			Integer cityCode = IPAndCityCache.getCityCodeIfExsit(ip);
			if (null != cityCode) {
				loginResponse.setCityCode(cityCode.intValue());
			} else {
				BaiduLBSJsonModel lbsModel = PtAPIServiceImpl.getInstance().getLBSModelFromIP(EGameType.DT.getId(), ip);
				if (null != lbsModel && lbsModel.getStatus() == 0) {
					int icityCode = lbsModel.getContent().getAddress_detail().getCity_code();
					loginResponse.setCityCode(icityCode);
					IPAndCityCache.append(ip, icityCode);
				}
			}
		}
	}

	/**
	 * 有app更新，通知客户端更新
	 * 
	 * @param updateApps
	 */
	public static void syncUpdateAppItems(final List<AppItem> updateApps) {

		long autoIncreVersion = AppItemDict.getInstance().getAutoIncreVersion();
		UpdateSubAppItemResponse.Builder builder = LoginModule.newAppItemsBuilder(updateApps);
		builder.setAutoIncreVersion((int) autoIncreVersion);
		final Collection<C2SSession> onlineAccounts = C2SSessionService.getInstance().getAllOnlieSession();
		onlineAccounts.forEach((session) -> {
			session.send(PBUtil.toS2CCommonRsp(S2CCmd.APPITEM_UPDATE, builder));
		});
	}

	/**
	 * 
	 * @param appItems
	 * @return
	 */
	public static UpdateSubAppItemResponse.Builder newAppItemsBuilder(final List<AppItem> appItems) {
		UpdateSubAppItemResponse.Builder builder = UpdateSubAppItemResponse.newBuilder();
		long autoIncreVersion = AppItemDict.getInstance().getAutoIncreVersion();
		builder.setAutoIncreVersion((int) autoIncreVersion);
		Map<Integer, GameRecommendIndexModel> recommendDict = GameRecommendDict.getInstance().getGameRecommendDict();

		appItems.forEach((appItem) -> {
			GameRecommendIndexModel recommendIdx = recommendDict.get(appItem.getAppId());

			SubAppItemResponse.Builder subAppItemResponse = SubAppItemResponse.newBuilder();
			subAppItemResponse.setAppId(appItem.getAppId());
			subAppItemResponse.setFlag(appItem.getFlag());
			subAppItemResponse.setOrder(null == recommendIdx ? appItem.getOrders() : recommendIdx.getRecommend_index());
			subAppItemResponse.setPackagepath(appItem.getPackagepath());
			subAppItemResponse.setPackagesize(appItem.getPackagesize());
			subAppItemResponse.setStatus(appItem.getT_status());
			subAppItemResponse.setVersion(appItem.getVersions());
			subAppItemResponse.setAppName(appItem.getAppName());
			subAppItemResponse.setGameType(appItem.getGame_type());

			if (StringUtils.isNotEmpty(appItem.getCity())) {
				List<Integer> cityCode = com.cai.common.util.StringUtil.toIntList(appItem.getCity(), Symbol.COMMA);
				subAppItemResponse.addAllCityCodes(cityCode);
			}

			if (StringUtils.isNotEmpty(appItem.getNot_fit_city())) {
				List<Integer> cityCode = com.cai.common.util.StringUtil.toIntList(appItem.getNot_fit_city(), Symbol.COMMA);
				subAppItemResponse.addAllFilterCityCodes(cityCode);
			}

			subAppItemResponse.setShowIndex(appItem.getShow_index() == 1 ? true : false);

			// ########审核需要！！！！！！
			FastMap<Integer, SysParamModel> params = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6);
			SysParamModel model2300 = params.get(2300);
			if (null != model2300 && model2300.getVal1() == 1) {
				subAppItemResponse.setFilterCity(appItem.getNot_fit_city());
				subAppItemResponse.setIconUrl(appItem.getIconUrl());
				subAppItemResponse.setCity(null == appItem.getCity() ? "0" : appItem.getCity());
				subAppItemResponse.setDownUrl(appItem.getPackageDownPath());
			}

			subAppItemResponse.setShowFlag(appItem.getShow_index());
			subAppItemResponse.setPlaceholder(appItem.getPlaceholder());
			builder.addItems(subAppItemResponse.build());
		});

		return builder;
	}

	/**
	 * 生成自定义token
	 * 
	 * @param accountId
	 * @return
	 */
	public static final String encodeToken(long accountId) {
		return String.join("-", Long.toString(accountId), UUID.randomUUID().toString().replaceAll("-", ""));
	}

	/**
	 * 
	 * 
	 */
	public static final Pair<Long, String> decodeToken(String selfToken) {
		String[] tokenArr = selfToken.split("-");
		try {
			return Pair.of(Long.parseLong(tokenArr[0]), tokenArr[1]);
		} catch (Exception e) {
			return Pair.of(0L, "-");
		}
	}

	/**
	 * 登陆后的流程
	 * 
	 * @param session
	 */
	public static boolean enterRoomIfExsit(final C2SSession session) {
		Account account = session.getAccount();

		// 判断是否上次在牌桌上
		RoomRedisModel roomRedisModel = null;

		int room_id = RoomModule.getRoomId(account.getAccount_id());
		if (room_id != 0) {
			roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);
		}

		if (roomRedisModel == null) {
			RoomModule.clearRoom(account.getAccount_id(), room_id);

			// 返回消息
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(3);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			session.send(responseBuilder.build());
			return false;
		} else {
			int loginc_index = roomRedisModel.getLogic_index();
			if (loginc_index <= 0) {
				logger.error("玩家[{}]重连，但房间处理的逻辑服id不合理,logic_index:{}", loginc_index);
				return false;
			}
			SessionUtil.setLogicSvrId(session, loginc_index, room_id);
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(3);
			logicRoomRequestBuilder.setRoomRequest(RoomRequest.newBuilder().setType(3).setAppId(roomRedisModel.getGame_id()).setRoomId(room_id));
			logicRoomRequestBuilder.setRoomId(room_id);
			logicRoomRequestBuilder.setAccountId(session.getAccount().getAccount_id());
			logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
			return true;
		}
	}

	/**
	 * 同步头像变化信息
	 * 
	 * @param headImg
	 * @param nickName
	 */
	public static void syncUpdateClubMemberInfo(long accountId, String headImg, String nickName) {
		ClubAccountProto.Builder builder = ClubAccountProto.newBuilder();
		builder.setAccountId(accountId);
		if (!Strings.isNullOrEmpty(headImg)) {
			builder.setAvatar(headImg);
		}
		if (!Strings.isNullOrEmpty(nickName)) {
			builder.setNickname(nickName);
		}
		ClientServiceImpl.getInstance().sendClub(PBUtil.toS2SRequet(S2SCmd.CLUB_SYNC_MEMBER_INFO, builder).build());
	}

	public static void main(String[] args) {
		System.out.println(encodeToken(555555555));
	}
}
