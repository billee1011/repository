/**
 * Copyright@2015-2016 Hunan Qisheng Network Technology Co. Ltd.[SHEN-ZHEN]
 */
package com.cai.handler;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.cai.common.constant.AttributeKeyConstans;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MessageRev;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2CCmd;
import com.cai.common.constant.SysParamEnum;
import com.cai.common.define.EGameType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountGroupModel;
import com.cai.common.domain.AccountMatchRedis;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.BrandChildLogModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.CoinPlayerMatchRedis;
import com.cai.common.domain.CoinPlayerRedis;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Page;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PrxoyPlayerRoomModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysGameType;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.ClubAndAccountVo;
import com.cai.common.util.PBUtil;
import com.cai.common.util.SessionUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.core.GbCdCtrl;
import com.cai.core.GbCdCtrl.Opt;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.module.RoomModule;
import com.cai.redis.service.RedisService;
import com.cai.service.ClientServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.MessageResponse;
import com.cai.util.RoomUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.xianyi.framework.core.transport.ICmd;
import com.xianyi.framework.core.transport.netty.session.C2SSession;
import com.xianyi.framework.handler.IClientHandler;

import org.springframework.util.StringUtils;
import protobuf.clazz.ClubMsgProto;
import protobuf.clazz.ClubMsgProto.ClubTableNeedPassportResponse;
import protobuf.clazz.Common;
import protobuf.clazz.Common.CommonGameRuleProto;
import protobuf.clazz.Common.ProxyRoomAppIdsProto;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.PlayerResultFLSResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.ProxyRoomItemResponse;
import protobuf.clazz.Protocol.ProxyRoomViewResposne;
import protobuf.clazz.Protocol.Request;
import protobuf.clazz.Protocol.Request.RequestType;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.c2s.C2SProto.MessageReceiveRsp;
import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

/**
 * @author
 */
@ICmd(code = RequestType.ROOM_VALUE, exName = "roomRequest")
public class RoomHandler extends IClientHandler<RoomRequest> {

	/**
	 * 创建房间
	 */
	public static final int CREATE_ROOM = 1;

	/**
	 * 加入房间
	 */
	public static final int JOIN_ROOM = 2;

	/**
	 * 重连
	 */
	public static final int RESET_CONNECT = 3;

	/**
	 * 请求牌局记录
	 */
	public static final int REQUEST_GAME_ROOM_RECORD = 4;

	/**
	 * 小局
	 */
	public static final int REQUEST_GAME_ROUND_RECORD = 5;

	/**
	 * 小局回放
	 */
	public static final int ROUND_RECORD_VIDEO = 6;

	/**
	 * 牌局父ID -- 根据牌局父ID查询 小局记局表
	 */
	public static final int PARENT_ROUND_RECORD_VIDEO = 7;

	/**
	 * 代理房间列表,主界面
	 */
	public static final int PROXY_ROOM_MAIN_VIEW = 50;

	/**
	 * 代理房间创建
	 */
	public static final int PROXY_ROOM_CREATE = 51;

	/**
	 * 代理开房记录
	 */
	public static final int PROXY_ROOM_RECORD = 52;

	/**
	 * 加入金币场
	 */
	private static final int JOIN_GOLD_ROOM = 53;

	/**
	 * 观战者
	 */
	private static final int BE_OBSERVER = 56;

	/**
	 * 获得代理创建的app
	 */
	private static final int PROXY_ROOM_APP_LIST = 57;

	/**
	 * 随机码播放
	 */
	private static final int ROUND_RECORD_VIDEO_BY_NUM = 58;
	/**
	 * 上帝视角
	 */
	private static final int BE_GOD_VIEW_OBSERVER = 59;

	@Override
	protected void execute(RoomRequest request, Request topRequest, C2SSession session) throws Exception {

		int type = request.getType();

		if (session.getAccount() == null) {
			session.shutdownGracefully();
			return;
		}

		Account account = session.getAccount();

		int game_id_temp = account.getGame_id();

		if (request.getAppId() > 0 || game_id_temp == EGameType.JS.getId()) {
			game_id_temp = request.getAppId();
		}
		// 玩法
		// int game_rule_index = request.getGameRuleIndex();
		final int game_id = game_id_temp;

		if (type == CREATE_ROOM) {

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					if (account.getNextEnterRoomTime() >= System.currentTimeMillis()) {
						session.send(MessageResponse.getMsgAllResponse("操作过于频繁").build());
						return;
					}

					SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
					if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.MSG);
						MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
						msgBuilder.setType(ESysMsgType.NONE.getId());
						msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
						responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
						session.send(responseBuilder.build());
						return;
					}

					if (!request.hasGameRound())
						return;
					if (!request.hasGameTypeIndex())
						return;

					account.resetNextEnterRoomTime();

					// 麻将类型
					int game_type_index = request.getGameTypeIndex();

					// 玩法
					int game_rule_index = request.getGameRuleIndex();
					List<Integer> gameRuleindexEx = request.getGameRuleIndexExList();
					/// 局数
					int game_round = request.getGameRound();
					// if(game_id != GameConstants.GAME_ID_OX){
					// if (!(game_round == 4 || game_round == 8 || game_round ==
					// 16)) {
					// return;
					// }
					// }

					// TODO 从redis查看是否有进入其它房间

					int roomId = RoomUtil.getRoomId(account.getAccount_id());
					if (roomId != 0) {
						// 验证一下
						session.send(MessageResponse.getMsgAllResponse("已进入其它房间:" + roomId).build());
						return;
					}

					// 开放判断
					SysParamModel sysParamModel = null;
					try {
						sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
								.get(SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index));
					} catch (Exception e) {
						session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
						logger.error("即将开放,敬请期待" + game_id + "index=" + game_type_index);
						return;
					}
					if (sysParamModel != null && sysParamModel.getVal1() != 1) {
						session.send(MessageResponse
								.getMsgAllResponse(Strings.isNullOrEmpty(sysParamModel.getStr2()) ? "即将开放,敬请期待!" : sysParamModel.getStr2()).build());
						return;
					}

					int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(game_type_index);
					if (roundGoldArray == null) {
						session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
						logger.error("roundGoldArray is null" + game_type_index);
						return;
					}

					SysParamModel findParam = null;
					for (int index : roundGoldArray) {
						SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);// 扑克类30
						if (tempParam == null) {
							logger.error("不存在的参数game_id" + game_id + "index=" + index);
							continue;
						}
						if (tempParam.getVal1() == game_round) {
							findParam = tempParam;
							break;
						}
					}

					if (findParam == null) {
						session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
						logger.error("findParam is null" + game_type_index + "game_id=" + game_id);
						return;
					}

					// 判断房卡是否免费
					if (sysParamModel != null && sysParamModel.getVal2() == 1) {

						long gold = account.getAccountModel().getGold();
						if (gold < findParam.getVal2()) {
							session.send(MessageResponse.getMsgAllResponse(SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足")).build());
							return;
						}
					}

					CoinPlayerMatchRedis redis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.COIN_PLAYER_MATCH_INFO, account.getAccount_id() + "", CoinPlayerMatchRedis.class);
					if (redis != null) {
						session.send(MessageResponse.getMsgAllResponse("金币场游戏正在匹配中").build());
						return;
					}

					CoinPlayerRedis coinRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.COIN_PLAYER_INFO, account.getAccount_id() + "", CoinPlayerRedis.class);
					if (coinRedis != null) {
						session.send(MessageResponse.getMsgAllResponse("有未完成的金币场游戏").build());
						return;
					}

					AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.MATCH_ROOM_ACCOUNT, account.getAccount_id() + "", AccountMatchRedis.class);
					if (accountMatchRedis != null && accountMatchRedis.isStart()) {
						session.send(MessageResponse.getMsgAllResponse("已经报名比赛了").build());
						return;
					}

					int logicSvrId = ClientServiceImpl.getInstance().allotLogicIdFromCenter(game_id);
					if (logicSvrId <= 0) {
						session.send(MessageResponse.getMsgAllResponse("服务器正在临时维护中，请稍等...").build());
						return;
					}

					// TODO 创建房间号,写入redis 记录是哪个逻辑计算服的
					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					int room_id = centerRMIServer.randomRoomId(1, logicSvrId);// 随机房间号
					if (room_id == -1) {
						session.send(MessageResponse.getMsgAllResponse("创建房间失败!").build());
						return;
					}
					if (room_id == -2) {
						session.send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build());
						return;
					}

					SessionUtil.setLogicSvrId(session, logicSvrId, room_id);
					// redis房间记录
					RoomRedisModel roomRedisModel = new RoomRedisModel();
					roomRedisModel.setRoom_id(room_id);
					roomRedisModel.setLogic_index(logicSvrId);
					roomRedisModel.getPlayersIdSet().add(session.getAccountID());
					roomRedisModel.getNames().add(account.getNickName());
					if (account.getAccountModel().getClient_ip() != null) {
						roomRedisModel.getIpSet().add(account.getAccountModel().getClient_ip());
					}

					if (gameRuleindexEx != null) {
						int[] ruleEx = new int[gameRuleindexEx.size()];
						for (int i = 0; i < gameRuleindexEx.size(); i++) {
							ruleEx[i] = gameRuleindexEx.get(i);
						}
						roomRedisModel.setGameRuleIndexEx(ruleEx);
					}

					roomRedisModel.setCreate_time(System.currentTimeMillis());
					roomRedisModel.setGame_round(game_round);
					roomRedisModel.setGame_rule_index(game_rule_index);
					roomRedisModel.setGame_type_index(game_type_index);
					roomRedisModel.setGame_id(game_id);
					SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

					// =======================

					// 通知逻辑服
					Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
					LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
					logicRoomRequestBuilder.setType(1);
					logicRoomRequestBuilder.setRoomRequest(request);
					logicRoomRequestBuilder.setRoomId(room_id);
					logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
					requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());

					boolean flag = ClientServiceImpl.getInstance().sendMsg(logicSvrId, requestBuider.build());

					if (!flag) {
						session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
						return;
					}

				}
			});

		} else if (type == JOIN_ROOM || BE_OBSERVER == type || type == BE_GOD_VIEW_OBSERVER) {

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					if (!request.hasRoomId()) {
						session.send(MessageResponse.getMsgAllResponse("游戏号不存在，请重新输入").build());
						return;
					}

					if (account.getNextEnterRoomTime() >= System.currentTimeMillis()) {
						session.send(MessageResponse.getMsgAllResponse("操作过于频繁").build());
						// 告诉客户端已经收到消息[辅助解决客户端输入房间号加入房间失败的bug]
						sendRoomMsgRev(session, request.getRoomId());
						return;
					}
					account.resetEnterRoomTime();

					// 告诉客户端已经收到消息[辅助解决客户端输入房间号加入房间失败的bug]
					sendRoomMsgRev(session, request.getRoomId());

					// 1如果他以前有房间的
					int source_room_id = RoomUtil.getRoomId(account.getAccount_id());
					if (source_room_id != 0) {
						RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class)
								.hGet(RedisConstant.ROOM, source_room_id + "", RoomRedisModel.class);
						if (roomRedisModel != null) {
							int loginc_index = roomRedisModel.getLogic_index();
							if (loginc_index <= 0) {
								logger.error("玩家[{}]请求加入房间 ，但房间对应的处理逻辑服不存在,{}", loginc_index);
								return;
							}
							SessionUtil.setLogicSvrId(session, loginc_index, source_room_id);
							Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
							LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
							logicRoomRequestBuilder.setType(3);
							logicRoomRequestBuilder.setRoomRequest(request);
							logicRoomRequestBuilder.setRoomId(source_room_id);
							logicRoomRequestBuilder.setAccountId(account.getAccount_id());
							logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
							requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
							boolean flag = ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
							if (!flag) {
								session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
							}
							return;
						}
					}

					CoinPlayerMatchRedis redis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.COIN_PLAYER_MATCH_INFO, account.getAccount_id() + "", CoinPlayerMatchRedis.class);
					if (redis != null) {
						session.send(MessageResponse.getMsgAllResponse("金币场游戏正在匹配中").build());
						return;
					}

					CoinPlayerRedis coinRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.COIN_PLAYER_INFO, account.getAccount_id() + "", CoinPlayerRedis.class);
					if (coinRedis != null) {
						session.send(MessageResponse.getMsgAllResponse("有未完成的金币场游戏").build());
						return;
					}

					AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.MATCH_ROOM_ACCOUNT, account.getAccount_id() + "", AccountMatchRedis.class);
					if (accountMatchRedis != null && accountMatchRedis.isStart()) {
						session.send(MessageResponse.getMsgAllResponse("已经报名比赛了").build());
						return;
					}

					int room_id = request.getRoomId();

					// TODO 从redis取出,判断出是哪个逻辑计算服的
					RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);
					if (roomRedisModel == null) {
						session.send(MessageResponse.getMsgAllResponse("房间不存在，请重新输入").build());
						logger.error("客户端输入的房间号，redis不存在" + room_id + "account.getAccount_id()=" + account.getAccount_id());
						return;
					}

					int gameId = SysGameTypeDict.getInstance().getGameIDByTypeIndex(roomRedisModel.getGame_type_index());
					if (gameId != request.getAppId() && request.hasAppId()) {
						if ((request.hasAppId() && request.getAppId() != roomRedisModel.getGame_id())) {
							int in_room_way = request.getInRoomWay();
							logger.error("appId==" + request.getAppId() + "but gameID==" + roomRedisModel.getGame_id() + "account.getAccount_id()="
									+ account.getAccount_id() + "==room_id==" + room_id + "==type=" + type + "==way==" + in_room_way);
							session.send(MessageResponse.getMsgAllResponse("房间不存在，请重新输入").build());
							return;
						}
					}

					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					RsSystemStopReadyStatusResponse rsSystemStopReadyStatusResponse = centerRMIServer.systemStopReadyStatus();
					if (rsSystemStopReadyStatusResponse.getSystemStopReady()) {
						session.send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能进入房间,请等待服务器停机维护完成再登录!").build());
						return;
					}

					if (roomRedisModel.getPlayersIdSet().contains(account.getAccount_id())) {
						// session.send(MessageResponse.getMsgAllResponse("已经在房间里了!").build());

						logger.error("出bug了。。。" + roomRedisModel.getPlayersIdSet() + "account.getAccount_id()==" + account.getAccount_id());
						/////////////
						// roomRedisModel.getPlayersIdSet().remove(account.getAccount_id());//
						// 容错下
						// SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM,
						// room_id + "", roomRedisModel);
						//
						// RedisResponse.Builder redisResponseBuilder =
						// RedisResponse.newBuilder();
						// redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
						// //
						// RsAccountResponse.Builder rsAccountResponseBuilder =
						// RsAccountResponse.newBuilder();
						// rsAccountResponseBuilder.setAccountId(account.getAccount_id());
						// rsAccountResponseBuilder.setRoomId(0);
						// //
						// redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
						// RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
						// ERedisTopicType.topicAll);

						int loginc_index = roomRedisModel.getLogic_index();
						if (loginc_index <= 0) {
							logger.error("玩家[{}]发起进入房间操作，但房间对用的处理逻辑服ID不合理,logicIndex:{}", loginc_index);
							return;
						}
						Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
						LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
						logicRoomRequestBuilder.setType(type);
						logicRoomRequestBuilder.setRoomRequest(request);
						logicRoomRequestBuilder.setRoomId(room_id);
						logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
						requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
						ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
						// 标注当前连接/玩家是在哪个逻辑服上的
						SessionUtil.setLogicSvrId(session, loginc_index, room_id);

						return;
					}

					if (org.apache.commons.lang.StringUtils.isNotEmpty(roomRedisModel.getGroupID()) && roomRedisModel.getIsInner() == 1) {

						if (account.getAccountGroupModelMap().get(roomRedisModel.getGroupID()) == null) {
							session.send(MessageResponse.getMsgAllResponse("抱歉，非此群成员不能进入房间!").build());
							return;
						}
					}

					if (roomRedisModel.getClub_id() > 0) {

						Map<String, AccountGroupModel> map = account.getAccountGroupModelMap();
						ClubAndAccountVo vo = ClubAndAccountVo.newVO(account.getAccount_id(), roomRedisModel.getClub_id())
								.setAccountGroupIds(map.keySet()).setRuleId(roomRedisModel.getRule_id());
						vo.setTableIndex(roomRedisModel.getTable_index());
						vo = centerRMIServer.rmiInvoke(RMICmd.CLUB_AND_ACCOUNT, vo);

						if (!(vo.isInClub() || vo.isClubGroup())) {
							session.send(MessageResponse.getMsgAllResponse("您不是亲友圈成员，无法加入!").build());
							return;
						}

						// @see EclubIdentity
						if (vo.getIdentity() == -1) {
							session.send(MessageResponse.getMsgAllResponse("您已经被管理设置为暂停娱乐，无法加入!").build());
							return;
						}

						if (!vo.isTireEnough()) {
							session.send(MessageResponse.getMsgAllResponse("疲劳值太低了,无法进行游戏,请联系管理员进行处理!").build());
							return;
						}

						if (vo.getBanPlayerName() != null) {
							session.send(MessageResponse.getMsgAllResponse(String.format("您暂时无法与 %s 玩家同桌进行游戏，如有疑问请联系亲友圈管理员!", vo.getBanPlayerName())).build());
							return;
						}

						if (!vo.isCanEnterTable()) {
							session.send(MessageResponse.getMsgAllResponse("您需要等待"+ vo.getEnterBanTime() +"秒后，才能重新进入牌桌!").build());
							return;
						}

						if (!vo.isClubWelfareEnough()) {
							session.send(MessageResponse.getMsgAllResponse("您的福卡不足"+ vo.getLimitWelfare() +"，无法进入牌桌!").build());
							return;
						}

						if (SysParamDict.getInstance().isObserverGameTypeIndex(roomRedisModel.getGame_type_index())) {
							// 观战类游戏
							if (type == BE_OBSERVER) {
								if (vo.getTablePassport() > 0) {
									// 需要输入密码
									ClubTableNeedPassportResponse.Builder rspBuilder = ClubTableNeedPassportResponse.newBuilder();
									rspBuilder.setClubId(roomRedisModel.getClub_id());
									rspBuilder.setRuleId(roomRedisModel.getRule_id());
									rspBuilder.setTableIndex(roomRedisModel.getTable_index());
									session.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_TABLE_NEED_PASSPORT_RSP, rspBuilder));
									return;
								}
							} else if (type == JOIN_ROOM) {
								if (vo.getLeftGameLimitRound() > -1) {
									if (vo.getLeftGameLimitRound() == 0) {
										session.send(MessageResponse.getMsgAllResponse("今日您在本包间牌局数已用完，无法继续游戏").build());
										return;
									}
								}
							}
						} else {
							if (vo.getTablePassport() > 0) {
								// 需要输入密码
								ClubTableNeedPassportResponse.Builder rspBuilder = ClubTableNeedPassportResponse.newBuilder();
								rspBuilder.setClubId(roomRedisModel.getClub_id());
								rspBuilder.setRuleId(roomRedisModel.getRule_id());
								rspBuilder.setTableIndex(roomRedisModel.getTable_index());
								session.send(PBUtil.toS2CCommonRsp(S2CCmd.CLUB_TABLE_NEED_PASSPORT_RSP, rspBuilder));
								return;
							}

							if (vo.getLeftGameLimitRound() > -1) {
								if (vo.getLeftGameLimitRound() == 0) {
									session.send(MessageResponse.getMsgAllResponse("今日您在本包间牌局数已用完，无法继续游戏").build());
									return;
								}
							}
						}
					}

					if (roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SEVER_OX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SZOX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_LZOX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_ZYQOX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MSZOX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MFZOX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_TBOX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_DBN
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SEVER_OX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SZOX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_LZOX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_ZYQOX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MSZOX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MFZOX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_TBOX_LX
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_JDOX_YY
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SG_JD
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SG_BJH
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SG_SW) {
						if (roomRedisModel.getPlayersIdSet().size() >= GameConstants.GAME_PLAYER_OX) {
							if (JOIN_ROOM == type) { // 坐下判断人数，观战忽略
								session.send(MessageResponse.getMsgAllResponse("房间人数已满!").build());
								return;
							}
						}

					} else if (roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_HJK
							|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_FKN) {
						if (roomRedisModel.getPlayersIdSet().size() >= GameConstants.GAME_PLAYER_HJK) {
							if (JOIN_ROOM == type) { // 坐下判断人数，观战忽略
								session.send(MessageResponse.getMsgAllResponse("房间人数已满!").build());
								return;
							}
						}
					}

					// TODO 选择逻辑计算服
					int loginc_index = roomRedisModel.getLogic_index();
					if (loginc_index <= 0) {
						logger.error("玩家[{}]发起进入房间操作，但房间对用的处理逻辑服ID不合理,logicIndex:{}", loginc_index);
						return;
					}
					Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
					LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
					logicRoomRequestBuilder.setType(type);
					logicRoomRequestBuilder.setRoomRequest(request);
					logicRoomRequestBuilder.setRoomId(room_id);
					logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
					requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
					ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
					// 标注当前连接/玩家是在哪个逻辑服上的
					SessionUtil.setLogicSvrId(session, loginc_index, room_id);
					// =======================

					// TODO 数据统计-从哪里进入房间的
					if (request.hasInRoomWay()) {
						int in_room_way = request.getInRoomWay();
						// MongoDBServiceImpl.getInstance().systemLog(ELogType.inRoomWay,
						// null, (long) in_room_way, null,
						// ESysLogLevelType.NONE);
					}

				}
			});

		} else if (type == RESET_CONNECT) {

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					// 判断是否上次在牌桌上
					RoomRedisModel roomRedisModel = null;

					int room_id = RoomModule.getRoomId(account.getAccount_id());
					if (room_id != 0) {
						roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);
					}

					if (roomRedisModel == null) {
						RoomModule.clearRoom(account.getAccount_id(), room_id);
						// =======================

						// 返回消息
						RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
						roomResponse.setType(3);
						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.ROOM);
						responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
						session.send(responseBuilder.build());
					} else {
						int loginc_index = roomRedisModel.getLogic_index();
						if (loginc_index <= 0) {
							logger.error("玩家[{}]重连，但房间处理的逻辑服id不合理,logic_index:{}", loginc_index);
							return;
						}
						SessionUtil.setLogicSvrId(session, loginc_index, room_id);
						Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
						LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
						logicRoomRequestBuilder.setType(3);
						logicRoomRequestBuilder.setRoomRequest(request);
						logicRoomRequestBuilder.setRoomId(room_id);
						logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
						requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
						boolean flag = ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
						if (!flag) {
							session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
							return;
						}
					}
				}

			});

		} else if (type == REQUEST_GAME_ROOM_RECORD) {

			if (!GbCdCtrl.canHandleMust(session, Opt.REQUEST_GAME_ROOM_RECORD))
				return;

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long target_account_id = account.getAccount_id();
					if (request.hasTargetAccountId()) {
						// if (account.getAccountModel().getIs_agent() != 1 &&
						// account.getAccountModel().getIs_inner() != 1) {
						// session.send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
						// return;
						// }
						target_account_id = request.getTargetAccountId();
					}

					List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance()
							.getParentBrandListByAccountId(target_account_id, game_id, 0, 0);
					int l = room_record.size();
					GameRoomRecord grr = null;
					RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
					game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);
					game_room_record.setAppId(request.getAppId());
					for (int k = 0; k < l; k++) {
						boolean error_check = false;
						BrandLogModel brandLogModel = room_record.get(k);
						grr = GameRoomRecord.to_Object(brandLogModel.getMsg());//
						if (grr == null)
							continue;
						int length = grr.getPlayers().length;
						for (int i = 0; i < length; i++) {
							if (i >= grr.getPlayers().length)
								continue;
							if (grr.getPlayers()[i] == null) {
								// error_check = true;
								continue;
							}
						}
						if (error_check)
							continue;

						PlayerResult _player_result = grr.get_player();

						PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

						PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();

						Player create_player = grr.getCreate_player();
						if (create_player != null) {
							RoomPlayerResponse.Builder room_player = setPlayerInfo(_player_result, player_result, create_player, length);
							player_result.setCreatePlayer(room_player);
						}
						recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
						if (brandLogModel.getRandomNum() != null) {
							player_result.setRandomNum(brandLogModel.getRandomNum());
						}

						game_room_record.addGameRoomRecords(player_result);
					}

					// 返回消息
					game_room_record.setPageType(0);// 不分页--兼容老版本
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == PROXY_ROOM_RECORD) {

			if (!GbCdCtrl.canHandleMust(session, Opt.PROXY_ROOM_RECORD))
				return;

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long create_account_id = account.getAccount_id();
					int l = 0;
					List<BrandLogModel> room_record;
					Page page = null;
					if (request.hasCurPage()) {
						int cur_page = request.getCurPage();
						if (cur_page <= 0)
							cur_page = 1;
						int totalSize = MongoDBServiceImpl.getInstance().getProxyParentBrandListByAccountIdCountNew(create_account_id, game_id);

						int f = totalSize % 10;
						int totalPage = 0;
						if (f == 0)
							totalPage = totalSize / 10;
						else
							totalPage = totalSize / 10 + 1;
						if (cur_page > totalPage) {
							cur_page = totalPage;
						}

						page = new Page(cur_page, 10, totalSize);
						room_record = MongoDBServiceImpl.getInstance().getProxyParentBrandListByAccountIdNew(page, create_account_id, game_id);
						l = room_record.size();
					} else {
						room_record = MongoDBServiceImpl.getInstance().getProxyParentBrandListByAccountId(create_account_id, game_id, 0, 0);
						l = room_record.size();
					}

					GameRoomRecord grr = null;
					RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
					game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);

					for (int k = 0; k < l; k++) {
						boolean error_check = false;
						grr = GameRoomRecord.to_Object(room_record.get(k).getMsg());//
						if (grr == null)
							continue;
						int length = grr.getPlayers().length;
						for (int i = 0; i < length; i++) {
							if (i >= grr.getPlayers().length)
								continue;
							if (grr.getPlayers()[i] == null) {
								// error_check = true;
								continue;
							}
						}
						if (error_check)
							continue;

						PlayerResult _player_result = grr.get_player();

						PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

						PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();

						Player create_player = grr.getCreate_player();
						if (create_player != null) {
							RoomPlayerResponse.Builder room_player = setPlayerInfo(_player_result, player_result, create_player, length);
							player_result.setCreatePlayer(room_player);
						}
						recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
						game_room_record.addGameRoomRecords(player_result);
					}

					if (page != null) {
						game_room_record.setCurPage(page.getRealPage());
						game_room_record.setPageSize(page.getPageSize());
						game_room_record.setTotalPage(page.getTotalPage());
						game_room_record.setTotalSize(page.getTotalSize());
					}
					game_room_record.setPageType(1);// 分页--兼容老版本
					// 返回消息
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					game_room_record.setAppId(request.getAppId());
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

				}

			});

		} else if (type == REQUEST_GAME_ROUND_RECORD) {

			if (!GbCdCtrl.canHandleMust(session, Opt.REQUEST_GAME_ROUND_RECORD))
				return;

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long record_id = request.getRecordId();
					String recordStr = request.getRecordIdStr();
					if (!StringUtils.isEmpty(recordStr)) {
						record_id = Long.parseLong(recordStr);
					}

					List<BrandChildLogModel> round_records = MongoDBServiceImpl.getInstance().getChildVideoBrandList(record_id, game_id);
					int l = round_records.size();
					RoomResponse.Builder game_round_record = RoomResponse.newBuilder();
					game_round_record.setType(MsgConstants.RESPONSE_GAME_ROUND_RECORD_LIST);
					for (int f = 0; f < l; f++) {
						byte[] gzipByte = ZipUtil.unGZip(round_records.get(f).getVideo_record());
						try {
							GameEndResponse game_end = Protocol.GameEndResponse.parseFrom(gzipByte);
							// 不要回放数据

							GameEndResponse.Builder gameEndResponseBuilder = game_end.toBuilder();
							if (!game_end.hasBrandIdStr() && game_end.hasBrandId()) {
								gameEndResponseBuilder.setBrandIdStr(String.valueOf(game_end.getBrandId()));
							}
							gameEndResponseBuilder.clearRecord();
							game_end = gameEndResponseBuilder.build();
							game_round_record.addGameRoundRecords(game_end);

						} catch (Exception e) {
							logger.error("error", e);
						}
					}
					// 返回消息

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, game_round_record.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == ROUND_RECORD_VIDEO) {

			if (!GbCdCtrl.canHandle(session, Opt.ROUND_RECORD_VIDEO))
				return;

			if (!request.hasBrandId())
				return;
			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long brand_id = request.getBrandId(); // 1612141055370040001L;
					String brandStr = request.getBrandIdStr();
					if (!StringUtils.isEmpty(brandStr)) {
						brand_id = Long.parseLong(brandStr);
					}
					BrandChildLogModel brandLogModel = MongoDBServiceImpl.getInstance().getChildBrandVideoByBrandId(brand_id, game_id);
					if (brandLogModel == null) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("记录不存在").build());
						return;
					}
					RoomResponse.Builder roomResponseBuilder = RoomResponse.newBuilder();
					roomResponseBuilder.setType(MsgConstants.RESPONSE_GAME_ROUND_RECORD);
					byte[] gzipByte = ZipUtil.unGZip(brandLogModel.getVideo_record());
					try {
						GameEndResponse game_end = Protocol.GameEndResponse.parseFrom(gzipByte);
						roomResponseBuilder.setGameRoundRecord(game_end);
						roomResponseBuilder.setAppId(brandLogModel.getGame_id());
					} catch (Exception e) {
						logger.error("error", e);
					}
					// 返回消息
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, roomResponseBuilder.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == PARENT_ROUND_RECORD_VIDEO) {

			if (!GbCdCtrl.canHandle(session, Opt.PARENT_ROUND_RECORD_VIDEO))
				return;

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {
				@Override
				public void run() {
					long target_brand_parent_id = 0l;
					if (request.hasTargetBrandParentId()) {
						// if (account.getAccountModel().getIs_inner() != 1) {
						// session.send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
						// return;
						// }
						target_brand_parent_id = request.getTargetBrandParentId();
					}
					if (request.hasBrandIdStr()) {
						target_brand_parent_id = Long.parseLong(request.getBrandIdStr());
					}
					if (target_brand_parent_id <= 0) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("牌局父Id错误").build());
						return;
					}
					BrandLogModel room_record = MongoDBServiceImpl.getInstance().getParentBrandByParentId(target_brand_parent_id, game_id);
					GameRoomRecord grr = null;
					RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
					game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);
					if (room_record != null) {
						boolean error_check = false;
						grr = GameRoomRecord.to_Object(room_record.getMsg());//
						int length = grr.getPlayers().length;
						// int length = gamecount;
						for (int i = 0; i < length; i++) {
							if (i >= grr.getPlayers().length)
								continue;
							if (grr.getPlayers()[i] == null) {
								// error_check = true;
								continue;
							}
						}
						if (error_check) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("牌局Id错误").build());
							return;
						}

						PlayerResult _player_result = grr.get_player();

						PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();
						PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();
						recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
						if (room_record.getRandomNum() != null) {
							player_result.setRandomNum(room_record.getRandomNum());
						}
						game_room_record.addGameRoomRecords(player_result);
					}

					// 返回消息
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == ROUND_RECORD_VIDEO_BY_NUM) {

			if (!GbCdCtrl.canHandle(session, Opt.ROUND_RECORD_VIDEO_BY_NUM))
				return;

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {
				@Override
				public void run() {
					long target_brand_parent_id = 0l;
					if (request.hasTargetBrandParentId()) {
						// if (account.getAccountModel().getIs_inner() != 1) {
						// session.send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
						// return;
						// }
						target_brand_parent_id = request.getTargetBrandParentId();
					}
					if (request.hasBrandIdStr()) {
						target_brand_parent_id = Long.parseLong(request.getBrandIdStr());
					}
					if (target_brand_parent_id <= 0) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("回放码Id错误").build());
						return;
					}

					BrandLogModel room_record = null;
					if (account.getAccountModel().getIs_inner() == 1 && String.valueOf(target_brand_parent_id).length() == 19) { //父牌局Id
						room_record = MongoDBServiceImpl.getInstance().getParentBrandByParentId(target_brand_parent_id);
					} else if (account.getAccountModel().getIs_inner() == 1 && target_brand_parent_id <= 9999999) {
						room_record = MongoDBServiceImpl.getInstance().getParentBrandByRoomID((int) target_brand_parent_id);
					} else {
						room_record = MongoDBServiceImpl.getInstance().getParentBrandByRandomNum((int) target_brand_parent_id);
					}

					GameRoomRecord grr = null;
					RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
					game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);
					if (room_record != null) {
						boolean error_check = false;
						grr = GameRoomRecord.to_Object(room_record.getMsg());//
						int length = grr.getPlayers().length;
						// int length = gamecount;
						for (int i = 0; i < length; i++) {
							if (i >= grr.getPlayers().length)
								continue;
							if (grr.getPlayers()[i] == null) {
								// error_check = true;
								continue;
							}
						}
						if (error_check) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("牌局Id错误").build());
							return;
						}

						PlayerResult _player_result = grr.get_player();
						PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();
						PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();
						recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
						player_result.setAppId(room_record.getGame_id());
						if (room_record.isRealKouDou()) {
							player_result.setCostGold(room_record.getGold_count());
						}

						player_result.setRandomNum(room_record.getRandomNum());
						game_room_record.addGameRoomRecords(player_result);
					}

					// 返回消息
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == MsgConstants.REQUST_LOCATION || type == MsgConstants.REQUST_LOCATION_NEW) {

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					LocationInfor locationInfo = request.getLocationInfor();
					SessionUtil.setAttr(session, AttributeKeyConstans.ACCOUNT_LOCATION, locationInfo);

					// 跟客户端对过，定位的时候会带上正在进行的房间id
					int room_id = request.getRoomId(); // RoomUtil.getRoomId(account.getAccount_id());
					// 消息再次封装
					if (room_id > 0) {
						int logicIndex = SessionUtil.getLogicSvrId(session, room_id);
						if (logicIndex <= 0) {
							// 防止在房间外请求，加个保险
							RoomRedisModel redisModule = RoomModule.getRoomRedisModelIfExsit(room_id);
							if (null == redisModule) {
								return;
							}
							logicIndex = redisModule.getLogic_index();
							if (logicIndex <= 0) {
								logger.error("玩家[{}]请求查看 LBS，但逻辑服索引id不合理。logicIndex:{}", account, logicIndex);
								return;
							}
							SessionUtil.setLogicSvrId(session, logicIndex, room_id);
						}

						// 消息再次封装
						Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(), session).build();
						ClientServiceImpl.getInstance().sendMsg(logicIndex, logicRequest);
					}
				}
			});

		} else if (type == PROXY_ROOM_MAIN_VIEW) {

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					AccountRedis accountRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", AccountRedis.class);
					
					SysParamModel sysParamModel = null;
					sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(SysParamEnum.ID_1107.getId());

					int count = 50;
					if (sysParamModel != null) {
						if (account.getAccountModel().getIs_agent() < 1) {
							count = sysParamModel.getVal2();
						} else {
							count = sysParamModel.getVal1();
						}
					}
					
					if (accountRedis == null) {
						ProxyRoomViewResposne.Builder proxyRoomViewResposneBuilder = ProxyRoomViewResposne.newBuilder();
						List<ProxyRoomItemResponse> proxyRoomItemResponseList = Lists.newArrayList();
						proxyRoomViewResposneBuilder.addAllProxyRoomItemResponseList(proxyRoomItemResponseList);
						proxyRoomViewResposneBuilder.setCanMaxRoom(count);// TODO 临时

						RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
						roomResponse.setType(MsgConstants.RESPONSE_MY_ROOMS);
						roomResponse.setAppId(request.getAppId());
						roomResponse.setProxyRoomViewResposne(proxyRoomViewResposneBuilder);

						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.ROOM);
						responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

						session.send(responseBuilder.build());
						return;
					}

					Map<Integer, PrxoyPlayerRoomModel> proxyRoomMap = accountRedis.getProxRoomMap();
					if (null == proxyRoomMap) {
						logger.error("AccountRedis is null!", account.getAccount_id());
						return;
					}
					List<PrxoyPlayerRoomModel> list = Lists.newArrayList(proxyRoomMap.values());
					// 排序一下
					Collections.sort(list, new Comparator<PrxoyPlayerRoomModel>() {
						public int compare(PrxoyPlayerRoomModel p1, PrxoyPlayerRoomModel p2) {
							return ((Long) p2.getCreate_time()).compareTo((Long) p1.getCreate_time());// id
							// 从大到小
						}
					});

					RedisService redisService = SpringService.getBean(RedisService.class);
					List<ProxyRoomItemResponse> proxyRoomItemResponseList = Lists.newArrayList();

					final int appId = request.getAppId();

					for (PrxoyPlayerRoomModel model : list) {

						if (appId != 0) {
							if (model.getApp_id() != appId && (appId != EGameType.DT.getId() && appId != EGameType.JS.getId()
									&& appId != EGameType.DTPH.getId())) {
								continue;
							}
						}

						RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, model.getRoom_id() + "", RoomRedisModel.class);
						if (null == roomRedisModel) {
							logger.warn("玩家:{}-->房间:{}不存在!", account, model.getRoom_id());
							continue;
						}

						if (roomRedisModel != null) {
							ProxyRoomItemResponse.Builder proxyRoomItemResponseBuilder = MessageResponse
									.getProxyRoomItemResponse(model, roomRedisModel);
							proxyRoomItemResponseList.add(proxyRoomItemResponseBuilder.build());
							SessionUtil.setLogicSvrId(session, roomRedisModel.getLogic_index(), roomRedisModel.getRoom_id());
						}
					}

				

					ProxyRoomViewResposne.Builder proxyRoomViewResposneBuilder = ProxyRoomViewResposne.newBuilder();
					proxyRoomViewResposneBuilder.addAllProxyRoomItemResponseList(proxyRoomItemResponseList);
					proxyRoomViewResposneBuilder.setCanMaxRoom(count);// TODO 临时

					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					roomResponse.setType(MsgConstants.RESPONSE_MY_ROOMS);
					roomResponse.setAppId(request.getAppId());
					roomResponse.setProxyRoomViewResposne(proxyRoomViewResposneBuilder);

					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

					session.send(responseBuilder.build());
				}

			});

		} else if (type == PROXY_ROOM_CREATE) {
			// if(account.getAccountModel().getIs_agent()!=1){
			// session.send(MessageResponse.getMsgAllResponse("非代理没有权限").build());
			// return;
			// }
			if (!GbCdCtrl.canHandle(session, Opt.PROXY_ROOM_CREATE))
				return;

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
							.get(SysParamEnum.ID_1000.getId());
					if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.MSG);
						MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
						msgBuilder.setType(ESysMsgType.NONE.getId());
						msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
						responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
						session.send(responseBuilder.build());
						return;
					}

					SysParamModel sysParamModel = null;
					sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(SysParamEnum.ID_1107.getId());

					int count = 0;
					if (sysParamModel != null) {
						if (account.getAccountModel().getIs_agent() < 1) {
							count = sysParamModel.getVal2();
						} else {
							count = sysParamModel.getVal1();
						}
					}

					ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
					AccountRedis accountRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", AccountRedis.class);
					if (null == accountRedis) {
						accountRedis = new AccountRedis();
						accountRedis.setAccount_id(account.getAccount_id());
//						SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", accountRedis);
					}
					Map<Integer, PrxoyPlayerRoomModel> proxyRoomMap = accountRedis.getProxRoomMap();

					List<PrxoyPlayerRoomModel> list = Lists.newArrayList(proxyRoomMap.values());

					int create_count = request.getProxyCreateRoomCount();
					if (create_count <= 0) {
						create_count = 1;
					}

					if (list != null && list.size() + create_count > count) {
						session.send(MessageResponse.getMsgAllResponse("最多同时只能创建" + count + "个房间").build());
						return;
					}

					// if (RoomUtil.getRoomId(account.getAccount_id()) != 0) {
					// // 验证一下
					// session.send(MessageResponse.getMsgAllResponse("您已经在房间中").build());
					// return;
					// }

					if (!request.hasGameRound())
						return;
					// if (!request.hasGameRuleIndex())
					// return;
					if (!request.hasGameTypeIndex())
						return;

					// 麻将类型
					int game_type_index = request.getGameTypeIndex();
					// 玩法
					int game_rule_index = request.getGameRuleIndex();
					/// 局数
					int game_round = request.getGameRound();
					// 封顶倍数
					int max_times = request.getMaxTimes();
					// 底分
					int base_score = request.getBaseScore();

					// 新规则兼容旧规则
					if (request.getIsNewRule()) {
						if (GameGroupRuleDict.getInstance().getBySubId(game_type_index) == null) {
							session.send(MessageResponse.getMsgAllResponse("该游戏暂未开放").build());
							return;
						}

						for (CommonGameRuleProto rule : request.getNewRules().getRulesList()) {
							if (rule.getRuleId() < 32) {
								game_rule_index = game_rule_index | (1 << rule.getRuleId());
							}
						}
					}

					// if(game_id != GameConstants.GAME_ID_OX){
					// if (!(game_round == 4 || game_round == 8 || game_round ==
					// 16)) {
					// return;
					// }
					// }

					// 开放判断
					try {
						sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
								.get(SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index));
					} catch (Exception e) {
						session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
						logger.error("SysParamModel 获取失败" + game_type_index);
						return;
					}

					if (sysParamModel != null && sysParamModel.getVal1() != 1) {
						session.send(MessageResponse
								.getMsgAllResponse(Strings.isNullOrEmpty(sysParamModel.getStr2()) ? "即将开放,敬请期待!" : sysParamModel.getStr2()).build());
						logger.error("即将开放,敬请期待" + game_type_index);
						return;
					}

					// 判断房卡是否免费
					if (sysParamModel != null && sysParamModel.getVal2() == 1) {
						int[] roundGoldArray = SysGameTypeDict.getInstance().getGoldIndexByTypeIndex(game_type_index);
						if (roundGoldArray == null) {
							session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
							logger.error("roundGoldArray null " + game_type_index);
							return;
						}

						SysParamModel findParam = null;
						for (int index : roundGoldArray) {
							SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);// 扑克类30
							if (tempParam == null) {
								logger.error("不存在的参数game_id" + game_id + "index=" + index);
								continue;
							}
							if (tempParam.getVal1() == game_round) {
								findParam = tempParam;
								break;
							}
						}

						if (findParam == null) {
							session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
							logger.error("findParam null " + game_type_index + "game_id=" + game_id + "game_round=" + game_round);
							return;
						}

						long gold = account.getAccountModel().getGold();
						long needCost = findParam.getVal2() * create_count;
						if (gold < needCost) {

							do {
								boolean ret = false;
								//判断专属豆
								ClubMsgProto.ClubExclusiveGoldProto exclusiveGoldProto = centerRMIServer
										.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_GOLD_INFO, account.getAccount_id());
								for (Common.CommonILI exclusivePB : exclusiveGoldProto.getExclusiveList()) {
									if (exclusivePB.getK() == game_id && exclusivePB.getV1() >= needCost) {
										ret = true;
										break;
									}
								}
								if (ret) {
									break;
								}
								session.send(MessageResponse
										.getMsgAllResponse(-1, SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足,是否充值?"),
												ESysMsgType._ERROR).build());
								return;
							} while (false);
						}
					}
					// TODO 创建房间号,写入redis 记录是哪个逻辑计算服的
					int logicSvrId = ClientServiceImpl.getInstance().allotLogicIdFromCenter(game_id);
					if (logicSvrId <= 0) {
						session.send(MessageResponse.getMsgAllResponse("服务器正在维护中...").build());
						return;
					}

					for (int i = 0; i < create_count; i++) {
						int room_id = centerRMIServer.randomRoomId(1, logicSvrId);// 随机房间号
						if (room_id == -1) {
							session.send(MessageResponse.getMsgAllResponse("创建房间失败!").build());
							return;
						}
						if (room_id == -2) {
							session.send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build());
							return;
						}
						long create_time = System.currentTimeMillis();

						// 代理创建房间，还没进房间，不需要标示逻辑服
						SessionUtil.setLogicSvrId(session, logicSvrId, room_id);

						// redis房间记录 代理开房间，人不需要进去
						RoomRedisModel roomRedisModel = new RoomRedisModel();
						roomRedisModel.setRoom_id(room_id);
						roomRedisModel.setLogic_index(logicSvrId);// TODO 临时
						// roomRedisModel.getPlayersIdSet().add(session.getAccountID());
						if (account.getAccountModel().getClient_ip() != null) {
							roomRedisModel.getIpSet().add(account.getAccountModel().getClient_ip());
						}
						roomRedisModel.setCreate_time(System.currentTimeMillis());
						roomRedisModel.setGame_round(game_round);
						roomRedisModel.setGame_rule_index(game_rule_index);
						roomRedisModel.setGame_type_index(game_type_index);
						roomRedisModel.setGame_id(game_id);
						roomRedisModel.setProxy_room(true);
						roomRedisModel.setCreate_account_id(account.getAccount_id());
						roomRedisModel.setBase_score(base_score);
						roomRedisModel.setMax_times(max_times);
						SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

						PrxoyPlayerRoomModel prxoyPlayerRoomModel = new PrxoyPlayerRoomModel();
						prxoyPlayerRoomModel.setCreate_account_id(account.getAccount_id());
						prxoyPlayerRoomModel.setRoom_id(room_id);
						prxoyPlayerRoomModel.setApp_id(game_id);
						prxoyPlayerRoomModel.setCreate_time(create_time);

						accountRedis.getProxRoomMap().put(prxoyPlayerRoomModel.getRoom_id(), prxoyPlayerRoomModel);
						SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", accountRedis);

						// 通知逻辑服
						Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
						LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
						logicRoomRequestBuilder.setType(51);
						logicRoomRequestBuilder.setRoomRequest(request);
						logicRoomRequestBuilder.setRoomId(room_id);
						logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
						requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
						boolean flag = ClientServiceImpl.getInstance().sendMsg(logicSvrId, requestBuider.build());

						if (!flag) {
							session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
							return;
						}
					}

				}

			});

		} else if (type == JOIN_GOLD_ROOM) {
			if (!GbCdCtrl.canHandle(session, Opt.JOIN_GOLD_ROOM))
				return;

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					if (check_is_stop_service(account, session))
						return;

					SysGameType gameType = SysGameTypeDict.getInstance().getSysGameType(request.getGameTypeIndex());
					if (gameType == null) {
						session.send(MessageResponse.getMsgAllResponse("金币场未开放").build());
						return;
					}
					// 金币场配置
					SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameType.getGameID()).get(5006);
					if (sysParamModel == null) {
						session.send(MessageResponse.getMsgAllResponse("金币场未开放").build());
						logger.error("sysParamModel == null where gameId = " + gameType.getGameID());
						return;
					}

					// 判断金币场是否开放
					int open = sysParamModel.getVal1().intValue();
					if (open != 1) {
						session.send(MessageResponse.getMsgAllResponse("金币场未开放").build());
						return;
					}
					// 判断金币是否足够
					long gold = account.getAccountModel().getMoney();
					long entrygold = sysParamModel.getVal4().longValue();
					if (gold < entrygold) {
						session.send(MessageResponse.getMsgAllResponse("金币必须大于" + entrygold + "才能进入金币场").build());
						return;
					}

					// 如果他以前有房间的
					if (check_is_had_room(account, request, session))
						return;
					if (!request.hasGameTypeIndex())
						return;
					// 麻将类型
					int game_type_index = request.getGameTypeIndex();
					// 玩法
					// int game_rule_index =
					// get_game_rule_index(game_type_index);
					// 开放判断
					sysParamModel = get_sysparm_model(game_type_index, gameType.getGameID());
					if (sysParamModel != null && sysParamModel.getVal1() != 1) {
						session.send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
						return;
					}

					// 通知逻辑服
					Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
					LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
					logicRoomRequestBuilder.setType(JOIN_GOLD_ROOM);
					logicRoomRequestBuilder.setRoomRequest(request);
					logicRoomRequestBuilder.setRoomId(0);
					logicRoomRequestBuilder.setGameId(gameType.getGameID());
					logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
					requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
					// GAME-TODO 之前没有房间，默认推倒第一个逻辑服>>> 要改
					int historyLogicServerId = 1;
					if (SystemConfig.gameDebug == 1) {
						historyLogicServerId = SystemConfig.proxy_index;
					}
					boolean flag = ClientServiceImpl.getInstance().sendMsg(historyLogicServerId, requestBuider.build());
					SessionUtil.setLogicSvrId(session, historyLogicServerId, -1);
					if (!flag) {
						session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
						return;
					}
				}

			});

		} else if (PROXY_ROOM_APP_LIST == request.getType()) {

			if (!GbCdCtrl.canHandle(session, Opt.PROXY_ROOM_APP_LIST))
				return;

			Global.getRoomExtraService().execute(new Runnable() {

				@Override
				public void run() {
					HashSet<Integer> appIds = new HashSet<>();
					AccountRedis accountRedis = SpringService.getBean(RedisService.class)
							.hGet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "", AccountRedis.class);
					if (accountRedis == null) {
						session.send(PBUtil.toS2CCommonRsp(S2CCmd.PROXY_ROOM_APP_LIST, ProxyRoomAppIdsProto.newBuilder().addAllAppIds(appIds)));
						return;
					}

					Map<Integer, PrxoyPlayerRoomModel> proxyRoomMap = accountRedis.getProxRoomMap();
					if (null == proxyRoomMap) {
						logger.error("AccountRedis is null!", account.getAccount_id());
						return;
					}

				
					proxyRoomMap.forEach((roomId, roomModel) -> {
						appIds.add(roomModel.getApp_id());
					});

					session.send(PBUtil.toS2CCommonRsp(S2CCmd.PROXY_ROOM_APP_LIST, ProxyRoomAppIdsProto.newBuilder().addAllAppIds(appIds)));

				}

			});

		} else if (type == PARENT_ROUND_RECORD_VIDEO) {

			if (!GbCdCtrl.canHandle(session, Opt.PARENT_ROUND_RECORD_VIDEO))
				return;

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long target_brand_parent_id = 0l;
					if (request.hasTargetBrandParentId()) {
						// if (account.getAccountModel().getIs_inner() != 1) {
						// session.send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
						// return;
						// }
						target_brand_parent_id = request.getTargetBrandParentId();
					}
					if (request.hasBrandIdStr()) {
						target_brand_parent_id = Long.parseLong(request.getBrandIdStr());
					}
					if (target_brand_parent_id <= 0) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("牌局父Id错误").build());
						return;
					}
					BrandLogModel room_record = MongoDBServiceImpl.getInstance().getParentBrandByParentId(target_brand_parent_id, game_id);
					GameRoomRecord grr = null;
					RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
					game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);
					if (room_record != null) {
						boolean error_check = false;
						grr = GameRoomRecord.to_Object(room_record.getMsg());//
						int length = grr.getPlayers().length;
						// int length = gamecount;
						for (int i = 0; i < length; i++) {
							if (i >= grr.getPlayers().length)
								continue;
							if (grr.getPlayers()[i] == null) {
								// error_check = true;
								continue;
							}
						}
						if (error_check) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session, MessageResponse.getMsgAllResponse("牌局Id错误").build());
							return;
						}

						PlayerResult _player_result = grr.get_player();
						PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();
						PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();
						recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);
						game_room_record.addGameRoomRecords(player_result);
					}

					// 返回消息
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// session.send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		}else if (type == MsgConstants.REQUST_PLAYER_BE_IN_ROOM){//不要影响牌桌
			Global.getRoomExtraService().execute(new Runnable() {
				
				@Override
				public void run() {
					int roomId = 0;
					int logicIndex = -1;
					if (request.hasRoomId() && request.getRoomId() != -1) {
						logicIndex = SessionUtil.getLogicSvrId(session, request.getRoomId());
						roomId = request.getRoomId();
						if (logicIndex <= 0) {
							logger.error("12房间不存在"+request.getRoomId());
							RoomRedisModel roomRedisModel = RoomModule.getRoomRedisModelIfExsit(request.getRoomId());
							if (null != roomRedisModel) {
								SessionUtil.setLogicSvrId(session, roomRedisModel.getLogic_index(), request.getRoomId());
								logicIndex = roomRedisModel.getLogic_index();
							}
						}
					} else {
						RoomRedisModel roomRedisModel = RoomModule.getRoomRedisModelIfExsit(account, session);
						if (null != roomRedisModel) {
							roomId = roomRedisModel.getRoom_id();
							logicIndex = roomRedisModel.getLogic_index();
							SessionUtil.setLogicSvrId(session, logicIndex, roomId);
						} else {
							logicIndex = SessionUtil.getLastAccessLogicSvrId(session);
						}
						logger.warn("客户端请求12房间协议 房间号=" + roomId);
					}

					if (logicIndex <= 0) {
						if (roomId == 0 && request.getRoomId() > 0) {
							force_exit(session);
							logger.error("12玩家:{}房间不存在，强制提出!", account);
						}
						return;
					}

					LocationInfor locationInfo = SessionUtil.getAttr(session, AttributeKeyConstans.ACCOUNT_LOCATION);
					if (null != locationInfo) {
						request.toBuilder().setLocationInfor(locationInfo);
					}
					// 消息再次封装
					Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(), session).build();
					ClientServiceImpl.getInstance().sendMsg(logicIndex, logicRequest);
					
				}
			});
		} else {
			int roomId = 0;
			int logicIndex = -1;
			if (request.hasRoomId() && request.getRoomId() != -1) {
				logicIndex = SessionUtil.getLogicSvrId(session, request.getRoomId());
				roomId = request.getRoomId();
				if (logicIndex <= 0) {
					logger.error("logicIndex玩家:{}房间{}不存在,协议号{}", account, request.getRoomId(), type);
					RoomRedisModel roomRedisModel = RoomModule.getRoomRedisModelIfExsit(request.getRoomId());
					if (null != roomRedisModel) {
						SessionUtil.setLogicSvrId(session, roomRedisModel.getLogic_index(), request.getRoomId());
						logicIndex = roomRedisModel.getLogic_index();
					}
				}
			} else {
				RoomRedisModel roomRedisModel = RoomModule.getRoomRedisModelIfExsit(account, session);
				if (null != roomRedisModel) {
					roomId = roomRedisModel.getRoom_id();
					logicIndex = roomRedisModel.getLogic_index();
					SessionUtil.setLogicSvrId(session, logicIndex, roomId);
				} else {
					logicIndex = SessionUtil.getLastAccessLogicSvrId(session);
				}
				logger.warn("玩家:[{}],客户端请求房间协议，但没有带房间号信息@@@!协议号" + type + "房间号=" + roomId, account);
			}

			if (logicIndex <= 0) {
				logger.error("玩家:{}房间请求，但没有连接指定逻辑服[{}]，type:{},roomid:{},reqroomid:{}!!", account, logicIndex, type, roomId, request.getRoomId());
				if (roomId == 0 && request.getRoomId() > 0) {
					force_exit(session);
					logger.error("玩家:{}房间不存在，强制提出!", account);
				}
				return;
			}

			LocationInfor locationInfo = SessionUtil.getAttr(session, AttributeKeyConstans.ACCOUNT_LOCATION);
			if (null != locationInfo) {
				request.toBuilder().setLocationInfor(locationInfo);
			}
			// 消息再次封装
			Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(), session).build();
			ClientServiceImpl.getInstance().sendMsg(logicIndex, logicRequest);
		}
	}

	public static void force_exit(C2SSession session) {
		RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
		quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, quit_roomResponse.build());
		session.send(responseBuilder.build());
	}

	/**
	 * @param grr
	 * @param _player_result
	 * @param player_result
	 */
	public static void setRoomInfor(GameRoomRecord grr, PlayerResult _player_result, PlayerResultResponse.Builder player_result) {
		player_result.setRoomId(grr.getRoom_id());
		player_result.setRoomOwnerAccountId(grr.getRoom_owner_account_id());
		player_result.setRoomOwnerName(grr.getRoom_owner_name());

		if (grr.getStart_time() > 0) {
			player_result.setCreateTime(grr.getStart_time());
		} else {
			player_result.setCreateTime(grr.getCreate_time());
		}

		player_result.setRecordId(grr.get_record_id());
		player_result.setRecordIdStr(grr.get_record_id() + "");
		player_result.setGameRound(_player_result.game_round);
		player_result.setGameRuleDes(_player_result.game_rule_des);
		player_result.setGameRuleIndex(_player_result.game_rule_index);
		player_result.setGameTypeIndex(_player_result.game_type_index);
	}

	/**
	 * @param _player_result
	 * @param player_result
	 * @param player
	 * @return
	 */
	public static RoomPlayerResponse.Builder setPlayerInfo(PlayerResult _player_result, PlayerResultResponse.Builder player_result, Player player,
			int length) {
		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(player.getAccount_id());
		room_player.setHeadImgUrl(player.getAccount_icon());
		room_player.setIp(player.getAccount_ip());
		room_player.setUserName(player.getNick_name());
		room_player.setSeatIndex(player.get_seat_index());
		room_player.setOnline(player.isOnline() ? 1 : 0);
		// TODO: 河南商丘麻将，添加nao（暗杠锁死）字段；pao qiang 字段，用来查牌时显示下的跑分，呛分
		int seat_index = player.get_seat_index();
		if (seat_index >= 0 && seat_index <= length - 1 && _player_result != null) {
			if (_player_result.nao != null)
				room_player.setNao(_player_result.nao[seat_index]);
			if (_player_result.pao != null)
				room_player.setPao(_player_result.pao[seat_index]);
			if (_player_result.qiang != null)
				room_player.setQiang(_player_result.qiang[seat_index]);
		}
		return room_player;
	}

	/**
	 * @param grr
	 * @param _player_result
	 * @param player_result
	 * @param playerResultFLSResponse
	 */
	public static void recorde_common(GameRoomRecord grr, PlayerResult _player_result, PlayerResultResponse.Builder player_result,
			PlayerResultFLSResponse.Builder playerResultFLSResponse, int game_count) {
		setRoomInfor(grr, _player_result, player_result);
		for (int i = 0; i < game_count; i++) {

			if (i >= grr.getPlayers().length)
				continue;

			if (grr.getPlayers()[i] == null) {
				continue;
			}
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			// if((_player_result.game_type_index==MJGameConstants.GAME_TYPE_ZZ)||(_player_result.game_type_index==MJGameConstants.GAME_TYPE_HZ)||
			// (_player_result.game_type_index==MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			// }else
			// if(_player_result.game_type_index==MJGameConstants.GAME_TYPE_CS
			// ||
			// (_player_result.game_type_index==MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);
			// }

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);

			if (_player_result.men_qing != null) {
				player_result.addMenQingCount(_player_result.men_qing[i]);
			}
			if (_player_result.gun_gun != null) {
				player_result.addGunGunCount(_player_result.gun_gun[i]);
			}
			if (_player_result.peng_peng_hu != null) {
				player_result.addPengPengHuCount(_player_result.peng_peng_hu[i]);
			}
			if (_player_result.gang_shang_hua != null) {
				player_result.addGangShangHuaCount(_player_result.gang_shang_hua[i]);
			}
			if (_player_result.hai_di != null) {
				player_result.addHaiDiCount(_player_result.hai_di[i]);
			}

			if (_player_result.hei != null) {
				playerResultFLSResponse.addHei(_player_result.hei[i]);
			}
			if (_player_result.hong != null) {
				playerResultFLSResponse.addHong(_player_result.hong[i]);
			}
			if (_player_result.ku != null) {
				playerResultFLSResponse.addKu(_player_result.ku[i]);
			}
			if (_player_result.ka != null) {
				playerResultFLSResponse.addKa(_player_result.ka[i]);
			}
			if (_player_result.qing != null) {
				playerResultFLSResponse.addQing(_player_result.qing[i]);
			}
			if (_player_result.shidui != null) {
				playerResultFLSResponse.addShidui(_player_result.shidui[i]);
			}
			if (_player_result.tai != null) {
				playerResultFLSResponse.addTai(_player_result.tai[i]);
			}

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			int xl = _player_result.lost_fan_shu.length;
			int yl = _player_result.lost_fan_shu[0].length;

			if (i < xl) {
				for (int j = 0; j < game_count; j++) {
					if (j < yl) {
						lfs.addItem(_player_result.lost_fan_shu[i][j]);
					}
				}
			}

			player_result.addLostFanShu(lfs);

			Player rplayer = grr.getPlayers()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = setPlayerInfo(_player_result, player_result, rplayer, game_count);
			player_result.addPlayers(room_player);

			player_result.addPlayersId(grr.getPlayers()[i].getAccount_id());
			player_result.addPlayersName(grr.getPlayers()[i].getNick_name());
		}
		player_result.setFlsResponse(playerResultFLSResponse);
	}

	/*
	 * 检查是否停服状态
	 */
	public boolean check_is_stop_service(Account account, C2SSession session) {
		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
			msgBuilder.setType(ESysMsgType.NONE.getId());
			msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
			responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
			session.send(responseBuilder.build());
			return true;
		}
		return false;
	}

	/*
	 * 检查是否已经有了房间了
	 */
	public boolean check_is_had_room(Account account, RoomRequest request, C2SSession session) {
		int source_room_id = RoomUtil.getRoomId(account.getAccount_id());
		if (source_room_id != 0) {
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.ROOM, source_room_id + "", RoomRedisModel.class);
			if (roomRedisModel != null) {
				int loginc_index = roomRedisModel.getLogic_index();
				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
				logicRoomRequestBuilder.setType(3);
				logicRoomRequestBuilder.setRoomRequest(request);
				logicRoomRequestBuilder.setRoomId(source_room_id);
				logicRoomRequestBuilder.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());

				if (loginc_index <= 0) {
					logger.error("玩家:{} 发起房间请求，但没有连接指定逻辑服[{}]!!", account, loginc_index);
					return false;
				}
				SessionUtil.setLogicSvrId(session, loginc_index, request.getRoomId());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(loginc_index, requestBuider.build());
				if (!flag) {
					session.send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
				}
				return true;
			}
		}
		return false;
	}

	public SysParamModel get_sysparm_model(int game_type_index, int game_id) {
		SysParamModel sysParamModel = null;
		try {
			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index));
		} catch (Exception e) {
			logger.error("SysParamModel 获取失败" + game_type_index);
		}
		return sysParamModel;
	}

	public int get_game_rule_index(int game_type_index) {
		return 0;
	}

	public static void joinRoom() {

	}

	private static void sendRoomMsgRev(final C2SSession session, int roomid) {
		MessageReceiveRsp.Builder builder = MessageReceiveRsp.newBuilder();
		builder.setVar1(roomid);
		builder.setType(MessageRev.ROOM);
		session.send(PBUtil.toS2CCommonRsp(S2CCmd.MESSAGE_RECEIVE, builder));
	}
}
