package com.cai.handler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.define.SysGameTypeEnum;
import com.cai.common.domain.Account;
import com.cai.common.domain.AccountRedis;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Page;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PrxoyPlayerRoomModel;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.core.Global;
import com.cai.dictionary.SysParamDict;
import com.cai.net.core.ClientHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.ClientServiceImpl;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.LocationInfor.Builder;
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
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsSystemStopReadyStatusResponse;

/**
 * 房间处理
 * 
 * @author run
 *
 */
public class TestRoomHandler extends ClientHandler<RoomRequest> {

	private static Logger logger = Logger.getLogger(RoomHandler.class);

	/**
	 * 创建房间
	 */
	private static final int CREATE_ROOM = 1;

	/**
	 * 加入房间
	 */
	private static final int JOIN_ROOM = 2;

	/**
	 * 重连
	 */
	private static final int RESET_CONNECT = 3;

	/**
	 * 请求牌局记录
	 */
	private static final int REQUEST_GAME_ROOM_RECORD = 4;

	/**
	 * 小局
	 */
	private static final int REQUEST_GAME_ROUND_RECORD = 5;

	/**
	 * 小局回放
	 */
	private static final int ROUND_RECORD_VIDEO = 6;

	/**
	 * 牌局父ID -- 根据牌局父ID查询 小局记局表
	 */
	private static final int PARENT_ROUND_RECORD_VIDEO = 7;

	/**
	 * 代理房间列表,主界面
	 */
	private static final int PROXY_ROOM_MAIN_VIEW = 50;

	/**
	 * 代理房间创建
	 */
	private static final int PROXY_ROOM_CREATE = 51;

	/**
	 * 代理开房记录
	 */
	private static final int PROXY_ROOM_RECORD = 52;

	/**
	 * 加入金币场
	 */
	private static final int JOIN_GOLD_ROOM = 53;

	/**
	 * 使用道具
	 */
	// private static final int REQUST_GOODS = 21;
	@Override
	public void onRequest() throws Exception {

		int type = request.getType();
		// 操作频率控制
		if (!session.isCanRequest("RoomHandler_" + type, 300L)) {
			return;
		}

		if (session.getAccount() == null)
			return;

		Account account = session.getAccount();

		int game_id_temp = account.getGame_id();
		int game_count = 0;

		if (request.getAppId() > 0) {
			game_id_temp = request.getAppId();
		}
		int game_type_index = request.getGameTypeIndex();
		// 玩法
		int game_rule_index = request.getGameRuleIndex();
		game_count = RoomComonUtil.getMaxNumber(game_type_index,game_rule_index);
		final int game_id = game_id_temp;

		final int gamecount = game_count;
		if (type == CREATE_ROOM) {

			SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
					.get(1000);
			if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.MSG);
				MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
				msgBuilder.setType(ESysMsgType.NONE.getId());
				msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
				responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
				send(responseBuilder.build());
				return;
			}

			if (!request.hasGameRound())
				return;
			if (!request.hasGameTypeIndex())
				return;

			// 麻将类型
		    game_type_index = request.getGameTypeIndex();
			// 玩法
			game_rule_index = request.getGameRuleIndex();
			
			List<Integer> gameRuleindexEx = request.getGameRuleIndexExList();
			/// 局数
			int game_round = request.getGameRound();
			// if(game_id != GameConstants.GAME_ID_OX){
			// if (!(game_round == 4 || game_round == 8 || game_round == 16)) {
			// return;
			// }
			// }

			// TODO 从redis查看是否有进入其它房间

			if (account.getRoom_id() != 0) {
				// 验证一下
				send(MessageResponse.getMsgAllResponse("调试:已进入其它房间:" + account.getRoom_id()).build());
				return;
			}

			// 开放判断
			SysParamModel sysParamModel = null;
			try {
				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
						.get(SysGameTypeEnum.getGameGoldTypeIndex(game_type_index));
			} catch (Exception e) {
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				logger.error("即将开放,敬请期待" + game_id + "index=" + game_type_index);
				return;
			}
			if (sysParamModel != null && sysParamModel.getVal1() != 1) {
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				return;
			}
			int[] roundGoldArray = SysGameTypeEnum.getGoldIndexByTypeIndex(game_type_index);
			if (roundGoldArray == null) {
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				logger.error("roundGoldArray is null" + game_type_index);
				return;
			}

			SysParamModel findParam = null;
			for (int index : roundGoldArray) {
				SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
						.get(index);// 扑克类30
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
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				logger.error("findParam is null" + game_type_index+"game_id="+game_id);
				return;
			}
			// 判断房卡是否免费
			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
			

				long gold = account.getAccountModel().getGold();
				if (gold < findParam.getVal2()) {
					send(MessageResponse.getMsgAllResponse("房卡不足").build());
					return;
				}
			}

			// TODO 创建房间号,写入redis 记录是哪个逻辑计算服的
			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			int room_id = centerRMIServer.randomRoomId(1);// 随机房间号
			if (room_id == -1) {
				send(MessageResponse.getMsgAllResponse("创建房间失败!").build());
				return;
			}
			if (room_id == -2) {
				send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build());
				return;
			}

			// redis房间记录
			RoomRedisModel roomRedisModel = new RoomRedisModel();
			roomRedisModel.setRoom_id(room_id);
			roomRedisModel.setLogic_index(1);// TODO 临时
			roomRedisModel.getPlayersIdSet().add(session.getAccountID());
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

			// 玩家最后的房间号记录
			account.setRoom_id(room_id);

			// ========同步到中心========
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
			//
			RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
			rsAccountResponseBuilder.setAccountId(account.getAccount_id());
			rsAccountResponseBuilder.setRoomId(account.getRoom_id());
			//
			redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicAll);
			// =======================

			// 通知逻辑服
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(1);
			logicRoomRequestBuilder.setRoomRequest(request);
			logicRoomRequestBuilder.setRoomId(room_id);
			logicRoomRequestBuilder
					.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());

			if (!flag) {
				send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
				return;
			}

		}

		else if (type == JOIN_ROOM) {

			if (!request.hasRoomId())
				return;
			int room_id = request.getRoomId();

			// 如果他以前有房间的
			int source_room_id = account.getRoom_id();
			if (source_room_id != 0) {
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
						source_room_id + "", RoomRedisModel.class);
				if (roomRedisModel != null) {
					int loginc_index = roomRedisModel.getLogic_index();
					Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
					LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
					logicRoomRequestBuilder.setType(3);
					logicRoomRequestBuilder.setRoomRequest(request);
					logicRoomRequestBuilder.setRoomId(source_room_id);
					logicRoomRequestBuilder
							.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
					requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
					boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
					if (!flag) {
						send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
					}
					return;
				}
			}

			// TODO 从redis取出,判断出是哪个逻辑计算服的
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					room_id + "", RoomRedisModel.class);
			if (roomRedisModel == null || (request.hasAppId() && request.getAppId() != roomRedisModel.getGame_id())) {
				send(MessageResponse.getMsgAllResponse("房间不存在!").build());
				return;
			}

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			RsSystemStopReadyStatusResponse rsSystemStopReadyStatusResponse = centerRMIServer.systemStopReadyStatus();
			if (rsSystemStopReadyStatusResponse.getSystemStopReady()) {
				send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能进入房间,请等待服务器停机维护完成再登录!").build());
				return;
			}

			if (roomRedisModel.getPlayersIdSet().contains(account.getAccount_id())) {
				send(MessageResponse.getMsgAllResponse("已经在房间里了!").build());

				logger.error("出bug了。。。" + roomRedisModel.getPlayersIdSet() + "account.getAccount_id()=="
						+ account.getAccount_id());
				/////////////
				roomRedisModel.getPlayersIdSet().remove(account.getAccount_id());// 容错下
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account.getAccount_id());
				rsAccountResponseBuilder.setRoomId(0);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
						ERedisTopicType.topicAll);
				return;
			}

			if (org.apache.commons.lang.StringUtils.isNotEmpty(roomRedisModel.getGroupID())
					&& roomRedisModel.getIsInner() == 1) {

				if (account.getAccountGroupModelMap().get(roomRedisModel.getGroupID()) == null) {
					send(MessageResponse.getMsgAllResponse("抱歉，非此群成员不能进入房间!").build());
					return;
				}
			}

			if (roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SEVER_OX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SZOX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_LZOX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_ZYQOX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MSZOX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MFZOX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_TBOX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_HJK
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SEVER_OX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_SZOX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_LZOX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_ZYQOX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MSZOX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_MFZOX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_TBOX_LX
					|| roomRedisModel.getGame_type_index() == GameConstants.GAME_TYPE_BTZ_YY) {
				if (roomRedisModel.getPlayersIdSet().size() >= GameConstants.GAME_PLAYER_OX) {
					send(MessageResponse.getMsgAllResponse("房间人数已满!").build());
					return;
				}

			} else {
				if (roomRedisModel.getPlayersIdSet().size() >= 4) {
					send(MessageResponse.getMsgAllResponse("房间人数已满!").build());
					return;
				}
			}


			// TODO 选择逻辑计算服
			int loginc_index = roomRedisModel.getLogic_index();
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(2);
			logicRoomRequestBuilder.setRoomRequest(request);
			logicRoomRequestBuilder.setRoomId(room_id);
			logicRoomRequestBuilder
					.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			ClientServiceImpl.getInstance().sendMsg(requestBuider.build());

			// =======================

			// TODO 数据统计-从哪里进入房间的
			if (request.hasInRoomWay()) {
				int in_room_way = request.getInRoomWay();
				MongoDBServiceImpl.getInstance().systemLog(ELogType.inRoomWay, null, (long) in_room_way, null,
						ESysLogLevelType.NONE);
			}

		}

		else if (type == RESET_CONNECT) {
			// 判断是否上次在牌桌上
			RoomRedisModel roomRedisModel = null;
			int room_id = account.getRoom_id();
			if (room_id != 0) {
				roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "",
						RoomRedisModel.class);
			}

			if (roomRedisModel == null) {

				account.setRoom_id(0);

				// ========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(account.getAccount_id());
				rsAccountResponseBuilder.setRoomId(account.getRoom_id());
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
						ERedisTopicType.topicAll);
				// =======================

				// 返回消息
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(3);
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ROOM);
				responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
				send(responseBuilder.build());
			} else {

				int loginc_index = roomRedisModel.getLogic_index();
				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
				logicRoomRequestBuilder.setType(3);
				logicRoomRequestBuilder.setRoomRequest(request);
				logicRoomRequestBuilder.setRoomId(room_id);
				logicRoomRequestBuilder
						.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
				if (!flag) {
					send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
					return;
				}
			}

		} else if (type == REQUEST_GAME_ROOM_RECORD) {
			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long target_account_id = account.getAccount_id();
					if (request.hasTargetAccountId()) {
						// if (account.getAccountModel().getIs_agent() != 1 &&
						// account.getAccountModel().getIs_inner() != 1) {
						// send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
						// return;
						// }
						target_account_id = request.getTargetAccountId();
					}

					List<BrandLogModel> room_record = MongoDBServiceImpl.getInstance()
							.getParentBrandListByAccountId(target_account_id, game_id);
					int l = room_record.size();
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
							RoomPlayerResponse.Builder room_player = setPlayerInfo(player_result, create_player);
							player_result.setCreatePlayer(room_player);
						}
						recorde_common(grr, _player_result, player_result, playerResultFLSResponse, length);

						game_room_record.addGameRoomRecords(player_result);
					}

					// 返回消息
					game_room_record.setPageType(0);// 不分页--兼容老版本
					Response.Builder responseBuilder = Response.newBuilder();
					responseBuilder.setResponseType(ResponseType.ROOM);
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == PROXY_ROOM_RECORD)

		{
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
						int totalSize = MongoDBServiceImpl.getInstance()
								.getProxyParentBrandListByAccountIdCountNew(create_account_id, game_id);

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
						room_record = MongoDBServiceImpl.getInstance().getProxyParentBrandListByAccountIdNew(page,
								create_account_id, game_id);
						l = room_record.size();
					} else {
						room_record = MongoDBServiceImpl.getInstance()
								.getProxyParentBrandListByAccountId(create_account_id, game_id);
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
							RoomPlayerResponse.Builder room_player = setPlayerInfo(player_result, create_player);
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
					responseBuilder.setExtension(Protocol.roomResponse, game_room_record.build());
					// send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());

				}

			});

		} else if (type == REQUEST_GAME_ROUND_RECORD) {

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long record_id = request.getRecordId();
					String recordStr = request.getRecordIdStr();
					if (!StringUtils.isEmpty(recordStr)) {
						record_id = Long.parseLong(recordStr);
					}

					List<BrandLogModel> round_records = MongoDBServiceImpl.getInstance().getChildBrandList(record_id,
							game_id);
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
					// send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == ROUND_RECORD_VIDEO)

		{
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
					BrandLogModel brandLogModel = MongoDBServiceImpl.getInstance().getChildBrandByBrandId(brand_id,
							game_id);
					if (brandLogModel == null) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session,
								MessageResponse.getMsgAllResponse("记录不存在").build());
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
					// send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == PARENT_ROUND_RECORD_VIDEO) {

			Global.getService(Global.SERVER_LOGIC).execute(new Runnable() {

				@Override
				public void run() {
					long target_brand_parent_id = 0l;
					if (request.hasTargetBrandParentId()) {
						// if (account.getAccountModel().getIs_inner() != 1) {
						// send(MessageResponse.getMsgAllResponse("你不是代理没有权限查看其他玩家牌局记录").build());
						// return;
						// }
						target_brand_parent_id = request.getTargetBrandParentId();
					}
					if (request.hasBrandIdStr()) {
						target_brand_parent_id = Long.parseLong(request.getBrandIdStr());
					}
					if (target_brand_parent_id <= 0) {
						PlayerServiceImpl.getInstance().sendAccountMsg(session,
								MessageResponse.getMsgAllResponse("牌局父Id错误").build());
						return;
					}
					BrandLogModel room_record = MongoDBServiceImpl.getInstance()
							.getParentBrandByParentId(target_brand_parent_id, game_id);
					GameRoomRecord grr = null;
					RoomResponse.Builder game_room_record = RoomResponse.newBuilder();
					game_room_record.setType(MsgConstants.RESPONSE_GAME_ROOM_RECORD_LIST);
					if (room_record != null) {
						boolean error_check = false;
						grr = GameRoomRecord.to_Object(room_record.getMsg());//
						int length = grr.getPlayers().length;
						for (int i = 0; i < length; i++) {
							if (grr.getPlayers()[i] == null) {
								// error_check = true;
								continue;
							}
						}
						if (error_check) {
							PlayerServiceImpl.getInstance().sendAccountMsg(session,
									MessageResponse.getMsgAllResponse("牌局Id错误").build());
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
					// send(responseBuilder.build());
					PlayerServiceImpl.getInstance().sendAccountMsg(session, responseBuilder.build());
				}

			});

		} else if (type == MsgConstants.REQUST_LOCATION) {
			int room_id = account.getRoom_id();
			// 消息再次封装
			if (room_id > 0) {
				// 消息再次封装
				Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(), session).build();
				ClientServiceImpl.getInstance().sendMsg(logicRequest);
			} else {

				Global.getService("").execute(new Runnable() {

					@Override
					public void run() {

						LocationInfor reqlocationInfor = topRequest.getExtension(Protocol.roomRequest)
								.getLocationInfor();
						if (reqlocationInfor.getPosX() == 0 || reqlocationInfor.getPosY() == 0) {
							return;
						}

						String position = PtAPIServiceImpl.getInstance().getbaiduPosition(game_id,
								reqlocationInfor.getPosX(), reqlocationInfor.getPosY());

						Builder locationInfor = LocationInfor.newBuilder();
						locationInfor.setAddress(position);
						locationInfor.setPosX(reqlocationInfor.getPosX());
						locationInfor.setPosY(reqlocationInfor.getPosY());
						locationInfor.setTargetAccountId(reqlocationInfor.getTargetAccountId());

						Response.Builder responseBuilder = Response.newBuilder();
						responseBuilder.setResponseType(ResponseType.ROOM);

						RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
						roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
						RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
						room_player.setLocationInfor(locationInfor);
						roomResponse.addPlayers(room_player);
						responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
						send(responseBuilder.build());

					}
				});
			}

		}

		else if (type == PROXY_ROOM_MAIN_VIEW) {

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

			AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS,
					account.getAccount_id() + "", AccountRedis.class);
			Map<Integer, PrxoyPlayerRoomModel> proxyRoomMap = accountRedis.getProxRoomMap();
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
			for (PrxoyPlayerRoomModel model : list) {
				RoomRedisModel roomRedisModel = redisService.hGet(RedisConstant.ROOM, model.getRoom_id() + "",
						RoomRedisModel.class);
				if (roomRedisModel != null) {
					ProxyRoomItemResponse.Builder proxyRoomItemResponseBuilder = MessageResponse
							.getProxyRoomItemResponse(model, roomRedisModel);
					proxyRoomItemResponseList.add(proxyRoomItemResponseBuilder.build());
				}
			}

			ProxyRoomViewResposne.Builder proxyRoomViewResposneBuilder = ProxyRoomViewResposne.newBuilder();
			proxyRoomViewResposneBuilder.addAllProxyRoomItemResponseList(proxyRoomItemResponseList);
			proxyRoomViewResposneBuilder.setCanMaxRoom(10);// TODO 临时

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_MY_ROOMS);
			roomResponse.setProxyRoomViewResposne(proxyRoomViewResposneBuilder);

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			send(responseBuilder.build());
		}

		else if (type == PROXY_ROOM_CREATE) {
			// if(account.getAccountModel().getIs_agent()!=1){
			// send(MessageResponse.getMsgAllResponse("非代理没有权限").build());
			// return;
			// }
			SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
					.get(1000);
			if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.MSG);
				MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
				msgBuilder.setType(ESysMsgType.NONE.getId());
				msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
				responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
				send(responseBuilder.build());
				return;
			}

			SysParamModel sysParamModel = null;
			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1107);

			int count = 0;
			if (sysParamModel != null) {
				if (account.getAccountModel().getIs_agent() < 1) {
					count = sysParamModel.getVal2();
				} else {
					count = sysParamModel.getVal1();
				}
			}

			ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
			AccountRedis accountRedis = SpringService.getBean(RedisService.class).hGet(RedisConstant.ACCOUNT_REDIS,
					account.getAccount_id() + "", AccountRedis.class);

			Map<Integer, PrxoyPlayerRoomModel> proxyRoomMap = accountRedis.getProxRoomMap();
			List<PrxoyPlayerRoomModel> list = Lists.newArrayList(proxyRoomMap.values());
			if (list != null && list.size() >= count) {
				send(MessageResponse.getMsgAllResponse("最多同时只能创建" + count + "个房间").build());
				return;
			}

			if (account.getRoom_id() != 0) {
				// 验证一下
				send(MessageResponse.getMsgAllResponse("您已经在房间中").build());
				return;
			}

			if (!request.hasGameRound())
				return;
			if (!request.hasGameRuleIndex())
				return;
			if (!request.hasGameTypeIndex())
				return;

			// 麻将类型
			 game_type_index = request.getGameTypeIndex();
			// 玩法
			game_rule_index = request.getGameRuleIndex();
			/// 局数
			int game_round = request.getGameRound();

			// if(game_id != GameConstants.GAME_ID_OX){
			// if (!(game_round == 4 || game_round == 8 || game_round == 16)) {
			// return;
			// }
			// }

			// 开放判断
			try {
				sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
						.get(SysGameTypeEnum.getGameGoldTypeIndex(game_type_index));
			} catch (Exception e) {
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				logger.error("SysParamModel 获取失败" + game_type_index);
				return;
			}

			if (sysParamModel != null && sysParamModel.getVal1() != 1) {
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				logger.error("即将开放,敬请期待" + game_type_index);
				return;
			}

			// 判断房卡是否免费
			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				int[] roundGoldArray = SysGameTypeEnum.getGoldIndexByTypeIndex(game_type_index);
				if (roundGoldArray == null) {
					send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
					logger.error("roundGoldArray null " + game_type_index);
					return;
				}

				SysParamModel findParam = null;
				for (int index : roundGoldArray) {
					SysParamModel tempParam = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
							.get(index);// 扑克类30
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
					send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
					logger.error("findParam null " + game_type_index+"game_id="+game_id);
					return;
				}

				long gold = account.getAccountModel().getGold();
				if (gold < findParam.getVal2()) {
					send(MessageResponse.getMsgAllResponse("房卡不足").build());
					return;
				}
			}
			// TODO 创建房间号,写入redis 记录是哪个逻辑计算服的

			int room_id = centerRMIServer.randomRoomId(1);// 随机房间号
			if (room_id == -1) {
				send(MessageResponse.getMsgAllResponse("创建房间失败!").build());
				return;
			}
			if (room_id == -2) {
				send(MessageResponse.getMsgAllResponse("服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build());
				return;
			}
			long create_time = System.currentTimeMillis();
			// redis房间记录 代理开房间，人不需要进去
			RoomRedisModel roomRedisModel = new RoomRedisModel();
			roomRedisModel.setRoom_id(room_id);
			roomRedisModel.setLogic_index(1);// TODO 临时
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
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

			PrxoyPlayerRoomModel prxoyPlayerRoomModel = new PrxoyPlayerRoomModel();
			prxoyPlayerRoomModel.setCreate_account_id(account.getAccount_id());
			prxoyPlayerRoomModel.setRoom_id(room_id);
			prxoyPlayerRoomModel.setCreate_time(create_time);

			accountRedis.getProxRoomMap().put(prxoyPlayerRoomModel.getRoom_id(), prxoyPlayerRoomModel);
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ACCOUNT_REDIS, account.getAccount_id() + "",
					accountRedis);

			// 通知逻辑服
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(51);
			logicRoomRequestBuilder.setRoomRequest(request);
			logicRoomRequestBuilder.setRoomId(room_id);
			logicRoomRequestBuilder
					.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());

			if (!flag) {
				send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
				return;
			}
		} else if (type == JOIN_GOLD_ROOM) {
			if (check_is_stop_service(account))
				return;

			// 金币场配置
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(5006);
			// 判断金币场是否开放
			int open = sysParamModel.getVal1().intValue();
			if (open != 1) {
				send(MessageResponse.getMsgAllResponse("金币场未开放").build());
				return;
			}
			// 判断金币是否足够
			long gold = account.getAccountModel().getMoney();
			long entrygold = sysParamModel.getVal4().longValue();
			if (gold < entrygold) {
				send(MessageResponse.getMsgAllResponse("金币必须大于" + entrygold + "才能进入金币场").build());
				return;
			}

			// 如果他以前有房间的
			if (check_is_had_room(account))
				return;
			if (!request.hasGameTypeIndex())
				return;
			// 麻将类型
			game_type_index = request.getGameTypeIndex();
			// 玩法
			// int game_rule_index = get_game_rule_index(game_type_index);
			// 开放判断
			sysParamModel = get_sysparm_model(game_type_index, game_id);
			if (sysParamModel != null && sysParamModel.getVal1() != 1) {
				send(MessageResponse.getMsgAllResponse("即将开放,敬请期待!").build());
				return;
			}

			// 通知逻辑服
			Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
			LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
			logicRoomRequestBuilder.setType(JOIN_GOLD_ROOM);
			logicRoomRequestBuilder.setRoomRequest(request);
			logicRoomRequestBuilder.setRoomId(0);
			logicRoomRequestBuilder.setGameId(game_id);
			logicRoomRequestBuilder
					.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
			requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
			boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());

			if (!flag) {
				send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
				return;
			}
			/*
			 * RoomRedisModel suitableRRM =
			 * get_suitable_gold_room(game_type_index, game_rule_index);
			 * if(suitableRRM == null){//没有找到合适的房间
			 * create_gold_room(account,game_type_index,game_rule_index,game_id)
			 * ; }else{ join_gold_room(suitableRRM); }
			 */
			// }else if(type == REQUST_GOODS){

		} else {
			// 消息再次封装
			Request logicRequest = MessageResponse.getLogicRequest(topRequest.toBuilder(), session).build();
			ClientServiceImpl.getInstance().sendMsg(logicRequest);
		}

	}

	/**
	 * @param grr
	 * @param _player_result
	 * @param player_result
	 */
	public void setRoomInfor(GameRoomRecord grr, PlayerResult _player_result,
			PlayerResultResponse.Builder player_result) {
		player_result.setRoomId(grr.getRoom_id());
		player_result.setRoomOwnerAccountId(grr.getRoom_owner_account_id());
		player_result.setRoomOwnerName(grr.getRoom_owner_name());
		player_result.setCreateTime(grr.getCreate_time());
		player_result.setRecordId(grr.get_record_id());
		player_result.setRecordIdStr(grr.get_record_id() + "");
		player_result.setGameRound(_player_result.game_round);
		player_result.setGameRuleDes(_player_result.game_rule_des);
		player_result.setGameRuleIndex(_player_result.game_rule_index);
		player_result.setGameTypeIndex(_player_result.game_type_index);
	}

	/**
	 * @param player_result
	 * @param create_player
	 */
	public RoomPlayerResponse.Builder setPlayerInfo(PlayerResultResponse.Builder player_result, Player player) {
		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(player.getAccount_id());
		room_player.setHeadImgUrl(player.getAccount_icon());
		room_player.setIp(player.getAccount_ip());
		room_player.setUserName(player.getNick_name());
		room_player.setSeatIndex(player.get_seat_index());
		room_player.setOnline(player.isOnline() ? 1 : 0);
		return room_player;
	}

	/**
	 * @param grr
	 * @param _player_result
	 * @param player_result
	 * @param playerResultFLSResponse
	 */
	private void recorde_common(GameRoomRecord grr, PlayerResult _player_result,
			PlayerResultResponse.Builder player_result, PlayerResultFLSResponse.Builder playerResultFLSResponse,
			int game_count) {
		setRoomInfor(grr, _player_result, player_result);
		for (int i = 0; i < game_count; i++) {
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
			int xl=_player_result.lost_fan_shu.length;
			int yl=_player_result.lost_fan_shu[0].length;
			
			if(i<xl) {
				for (int j = 0; j < game_count; j++) {
					if(j<yl) {
						lfs.addItem(_player_result.lost_fan_shu[i][j]);
					}
				}
			}
			

			player_result.addLostFanShu(lfs);

			Player rplayer = grr.getPlayers()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = setPlayerInfo(player_result, rplayer);
			player_result.addPlayers(room_player);

			player_result.addPlayersId(grr.getPlayers()[i].getAccount_id());
			player_result.addPlayersName(grr.getPlayers()[i].getNick_name());
		}
		player_result.setFlsResponse(playerResultFLSResponse);
	}

	/*
	 * 检查是否停服状态
	 */
	public boolean check_is_stop_service(Account account) {
		SysParamModel sysParamModel1000 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(1000);
		if (sysParamModel1000.getVal3() == 0 && account.getAccountModel().getIs_inner() == 0) {

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
			msgBuilder.setType(ESysMsgType.NONE.getId());
			msgBuilder.setMsg("当前停服维护中,请稍后再进入游戏");
			responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
			send(responseBuilder.build());
			return true;
		}
		return false;
	}

	/*
	 * 检查是否已经有了房间了
	 */
	public boolean check_is_had_room(Account account) {
		int source_room_id = account.getRoom_id();
		if (source_room_id != 0) {
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					source_room_id + "", RoomRedisModel.class);
			if (roomRedisModel != null) {
				int loginc_index = roomRedisModel.getLogic_index();
				Request.Builder requestBuider = MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
				LogicRoomRequest.Builder logicRoomRequestBuilder = LogicRoomRequest.newBuilder();
				logicRoomRequestBuilder.setType(3);
				logicRoomRequestBuilder.setRoomRequest(request);
				logicRoomRequestBuilder.setRoomId(source_room_id);
				logicRoomRequestBuilder
						.setLogicRoomAccountItemRequest(MessageResponse.getLogicRoomAccountItemRequest(session));
				requestBuider.setExtension(Protocol.logicRoomRequest, logicRoomRequestBuilder.build());
				boolean flag = ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
				if (!flag) {
					send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build());
				}
				return true;
			}
		}
		return false;
	}

	/*
	 * 找到合适的金币场房间
	 */
	public RoomRedisModel get_suitable_gold_room(int game_type_index, int game_rule_index) {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		List<RoomRedisModel> rrmList = centerRMIServer.getAllRoomRedisModelList();
		RoomRedisModel suitableRRM = null;
		for (RoomRedisModel rrm : rrmList) {
			// 找到一个没满的房间 且人最多的
			if (rrm.getGame_type_index() == game_type_index && rrm.getGame_rule_index() == game_rule_index
					&& rrm.getCur_player_num() < rrm.getPlayer_max()) {
				if (suitableRRM != null) {
					if (rrm.getCur_player_num() > suitableRRM.getCur_player_num())
						suitableRRM = rrm;
				} else {
					suitableRRM = rrm;
				}
			}
		}
		return suitableRRM;
	}

	public SysParamModel get_sysparm_model(int game_type_index, int game_id) {
		SysParamModel sysParamModel = null;
		try {
			sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(SysGameTypeEnum.getGameGoldTypeIndex(game_type_index));
		} catch (Exception e) {
			logger.error("SysParamModel 获取失败" + game_type_index);
		}
		return sysParamModel;
	}

	public int get_game_rule_index(int game_type_index) {
		return 0;
	}
	/*
	 * public boolean create_gold_room(Account account, int game_type_index, int
	 * game_rule_index, int game_id){ ICenterRMIServer centerRMIServer =
	 * SpringService.getBean(ICenterRMIServer.class); int room_id =
	 * centerRMIServer.randomRoomId(1);// 随机房间号 if (room_id == -1) {
	 * send(MessageResponse.getMsgAllResponse("创建房间失败!").build()); return false;
	 * } if (room_id == -2) { send(MessageResponse.getMsgAllResponse(
	 * "服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build()); return false; } //
	 * redis房间记录 RoomRedisModel roomRedisModel = new RoomRedisModel();
	 * roomRedisModel.setRoom_id(room_id); roomRedisModel.setLogic_index(1);//
	 * TODO 临时 roomRedisModel.getPlayersIdSet().add(session.getAccountID()); if
	 * (account.getAccountModel().getClient_ip() != null) {
	 * roomRedisModel.getIpSet().add(account.getAccountModel().getClient_ip());
	 * } roomRedisModel.setCreate_time(System.currentTimeMillis());
	 * roomRedisModel.setGame_round(1);
	 * roomRedisModel.setGame_rule_index(game_rule_index);
	 * roomRedisModel.setGame_type_index(game_type_index);
	 * roomRedisModel.setGame_id(game_id);
	 * SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM,
	 * room_id + "", roomRedisModel);
	 * 
	 * // 玩家最后的房间号记录 account.setRoom_id(room_id);
	 * 
	 * // ========同步到中心======== RedisResponse.Builder redisResponseBuilder =
	 * RedisResponse.newBuilder();
	 * redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP); //
	 * RsAccountResponse.Builder rsAccountResponseBuilder =
	 * RsAccountResponse.newBuilder();
	 * rsAccountResponseBuilder.setAccountId(account.getAccount_id());
	 * rsAccountResponseBuilder.setRoomId(account.getRoom_id()); //
	 * redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
	 * RedisServiceImpl.getInstance().convertAndSendRsResponse(
	 * redisResponseBuilder.build(), ERedisTopicType.topicAll); //
	 * =======================
	 * 
	 * // 通知逻辑服 Request.Builder requestBuider =
	 * MessageResponse.getLogicRequest(RequestType.LOGIC_ROOM, session);
	 * LogicRoomRequest.Builder logicRoomRequestBuilder =
	 * LogicRoomRequest.newBuilder(); logicRoomRequestBuilder.setType(1);
	 * logicRoomRequestBuilder.setRoomRequest(request);
	 * logicRoomRequestBuilder.setRoomId(room_id); logicRoomRequestBuilder
	 * .setLogicRoomAccountItemRequest(MessageResponse.
	 * getLogicRoomAccountItemRequest(session));
	 * requestBuider.setExtension(Protocol.logicRoomRequest,
	 * logicRoomRequestBuilder.build()); boolean flag =
	 * ClientServiceImpl.getInstance().sendMsg(requestBuider.build());
	 * 
	 * if (!flag) {
	 * send(MessageResponse.getMsgAllResponse("逻辑服务器链接失败").build()); return
	 * false; } return true; } public boolean join_gold_room(RoomRedisModel
	 * rrm){ return true; }
	 */
}
