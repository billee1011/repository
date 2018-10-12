package com.cai.handler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.game.btz.BTZTable;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.handler.jdddz.DDZ_JD_Table;
import com.cai.game.fls.FLSTable;
import com.cai.game.hh.HHTable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.MJType;
import com.cai.game.nn.NNTable;
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.handler.fifteenpdk.PDK_FIFTEEN_Table;
import com.cai.game.pdk.handler.jdpdk.PDK_JD_Table;
import com.cai.game.pdk.handler.laizipdk.PDK_LZ_Table;
import com.cai.game.pdk.handler.srpdk.PDK_SR_Table;
import com.cai.game.zjh.ZJHTable;
import com.cai.game.btz.BTZTable;
import com.cai.game.ddz.DDZTable;
import com.cai.net.core.ClientHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.MessageResponse;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.s2s.S2SProto.PlayerStatus;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;

/**
 * 房间
 * 
 * @author run
 *
 */
public class LogicRoomHandler extends ClientHandler<LogicRoomRequest> {

	private static Logger logger = Logger.getLogger(LogicRoomHandler.class);

	/**
	 * 创建房间
	 */
	private static final int CRATE_ROOM = 1;

	/**
	 * 加入房间
	 */
	private static final int JOIN_ROOM = 2;

	/**
	 * 重连
	 */
	private static final int RESET_CONNECT = 3;

	/**
	 * 下线
	 */
	private static final int OFFLINE = 4;

	/**
	 * 观战者
	 */
	private static final int BE_OBSERVER = 56;
	/**
	 * 代理房间创建
	 */
	private static final int PROXY_ROOM_CREATE = 51;
	/**
	 * 加入金币场
	 */
	private static final int JOIN_GOLD_ROOM = 53;

	private static final int UPDATE_MONEY_AND_GOLD = 5;// 刷新玩家的金币 豆子信息

	@Override
	public void onRequest() throws Exception {

		LogicRoomRequest r = request;
		// System.out.println("=====");

		int type = r.getType();

		// #########server handler test
		// if (type != -1) {
		// PlayerStatus.Builder b = PlayerStatus.newBuilder();
		// b.setAccountId(23235623);
		// b.setRoomId(-1352356);
		// b.setStatus(1);
		// session.channel.writeAndFlush(PBUtil.toS2SRequet(S2SCmd.TEST_CMD,
		// b).build());
		// return;
		// }
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();

		int room_id = r.getRoomId();

		if (type == CRATE_ROOM) {

			handler_player_create_room(r, GameConstants.CREATE_ROOM_NORMAL, room_id);

		} else if (type == JOIN_ROOM || type == BE_OBSERVER) {
			handler_join_room(r, room_id);
		}

		else if (type == RESET_CONNECT) {

			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(r.getRoomId());

			// 判断房间是否在内存中
			if (table == null) {
				// 删除
				PlayerServiceImpl.getInstance().delRoomId(r.getRoomId());

				// 返回消息
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(3);// 3重连返回,没有有效房间
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ROOM);
				responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
				send(responseBuilder.build());

				// ========同步到中心======== 把中心的房间删除 add 5.10
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(r.getLogicRoomAccountItemRequest().getAccountId());
				rsAccountResponseBuilder.setRoomId(0);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

				/////////////
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, r.getRoomId() + "",
						RoomRedisModel.class);

				if (roomRedisModel != null && roomRedisModel.getPlayersIdSet() != null) {
					roomRedisModel.getPlayersIdSet().remove(r.getLogicRoomAccountItemRequest().getAccountId());
				}
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, r.getRoomId() + "", roomRedisModel);

				return;
			}

			// ===========是否在这个房间里============
			boolean flag = false;
			for (Player p : table.get_players()) {
				if (p == null)
					continue;
				if (p.getAccount_id() == r.getLogicRoomAccountItemRequest().getAccountId()) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				// 数据不一致时修复
				// 返回消息
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(3);// 3重连返回,没有有效房间
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ROOM);
				responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
				send(responseBuilder.build());

				// ========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(r.getLogicRoomAccountItemRequest().getAccountId());
				rsAccountResponseBuilder.setRoomId(0);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

				/////////////
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, r.getRoomId() + "",
						RoomRedisModel.class);
				roomRedisModel.getPlayersIdSet().remove(r.getLogicRoomAccountItemRequest().getAccountId());
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, r.getRoomId() + "", roomRedisModel);

				return;

			}
			// ======================

			ReentrantLock lock = table.getRoomLock();
			try {
				lock.lock();
				handler_player_reconnect_room(r);
			} finally {
				lock.unlock();
			}

			// System.out.println("重连的房间间:"+ room_id);
			// System.out.println("重连接的玩家信息:" + logicRoomAccountItemRequest);

		}

		else if (type == OFFLINE) {
			long account_id = request.getAccountId();
			// System.out.println("收到下线消息 "+account_id);
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(account_id);
			if (player == null)
				return;

			player.setOnline(false);
			// TODO 下线后的操作

			room_id = player.getRoom_id();
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if (table == null) {
				return;
			}

			ReentrantLock lock = table.getRoomLock();
			try {
				lock.lock();
				// 刷新时间
				table.process_flush_time();

				table.handler_player_offline(player);
			} finally {
				lock.unlock();
			}

		}

		else if (type == PROXY_ROOM_CREATE) {
			// 创建代理房间
			handler_player_create_room(r, GameConstants.CREATE_ROOM_PROXY, 0);
		} else if (type == MsgConstants.REQUST_PROXY_RELEASE_ROOM) {
			// 代理解散房间
			long account_id = request.getAccountId();
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(account_id);
			if (player == null)
				return;

			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if (table == null) {
				return;
			}

			ReentrantLock lock = table.getRoomLock();
			try {
				lock.lock();

				table.handler_release_room(player, GameConstants.Release_Room_Type_PROXY);
			} finally {
				lock.unlock();
			}
		} else if (type == JOIN_GOLD_ROOM) {
			int game_type_index = change_game_type_index_in_gold(room_rq.getGameTypeIndex());
			int game_rule_index = get_game_rule_index_in_gold(game_type_index);
			// 找合适的房间
			int max_player_num = 4;
			Room suitableRoom = null;
			Map<Integer, Room> roomMap = PlayerServiceImpl.getInstance().getRoomMap();
			for (Map.Entry<Integer, Room> entry : roomMap.entrySet()) {
				// 找金币场 且未满 且玩法相同 的房间
				if (entry.getValue().is_sys() && entry.getValue().getPlayerCount() < max_player_num
						&& entry.getValue().getGameTypeIndex() == game_type_index && entry.getValue().getGameRuleIndex() == game_rule_index) {
					if (suitableRoom != null) {
						suitableRoom = entry.getValue().getPlayerCount() > suitableRoom.getPlayerCount() ? entry.getValue() : suitableRoom;
					} else {
						suitableRoom = entry.getValue();
					}
				}
			}
			// 没找到
			if (suitableRoom == null) {
				// 需要创建新金币场房间
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				room_id = PlayerServiceImpl.getInstance().getUnionRoomID();// 随机房间号
				if (room_id == -1) {
					send(MessageResponse.getMsgAllResponse(-1, "创建房间失败!").build());
					return;
				}
				if (room_id == -2) {
					send(MessageResponse.getMsgAllResponse(-2, "服务器进入停服倒计时,不能创建房间,请等待服务器停机维护完成再登录!").build());
					return;
				}
				long accountId = (long) session.getAccountID();
				accountId = logicRoomAccountItemRequest.getAccountId();
				// redis房间记录
				RoomRedisModel roomRedisModel = new RoomRedisModel();
				roomRedisModel.setRoom_id(room_id);
				roomRedisModel.setLogic_index(1);// TODO 临时
				roomRedisModel.getPlayersIdSet().add(accountId);

				roomRedisModel.setCreate_time(System.currentTimeMillis());
				roomRedisModel.setGame_round(1);
				roomRedisModel.setMoneyRoom(true);
				roomRedisModel.setGame_rule_index(get_game_rule_index_in_gold(game_type_index));
				roomRedisModel.setGame_type_index(game_type_index);

				roomRedisModel.setGame_id(r.getGameId());
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

				// ========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(accountId);
				rsAccountResponseBuilder.setRoomId(room_id);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

				handler_player_create_room(r, GameConstants.CREATE_ROOM_SYS, room_id);
			} else {// 加入房间
				room_id = suitableRoom.getRoom_id();
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "",
						RoomRedisModel.class);
				long accountId = (long) session.getAccountID();
				accountId = logicRoomAccountItemRequest.getAccountId();
				roomRedisModel.getPlayersIdSet().add(accountId);
				// 写入redis
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

				// ========同步到中心========
				RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
				redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
				//
				RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
				rsAccountResponseBuilder.setAccountId(accountId);
				rsAccountResponseBuilder.setRoomId(room_id);
				//
				redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
				RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

				handler_join_room(r, room_id);
			}
		} else if (type == UPDATE_MONEY_AND_GOLD) {// 刷新 房间里的玩家的 金币 豆子信息
			long accountid = r.getLogicRoomAccountItemRequest().getAccountId();
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(r.getRoomId());

			if (table == null)// 不在房间里面 不需要通知
				return;

			ReentrantLock lock = table.getRoomLock();
			try {
				lock.lock();
				Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountid);
				if (player != null) {
					if (r.hasAddGold()) {
						player.setGold(player.getGold() + r.getAddGold());
					}
					if (r.hasAddMoney()) {
						player.setMoney(player.getMoney() + r.getAddMoney());
					}
					table.handler_refresh_all_player_data();// 通知给客户端
				}
			} finally {
				lock.unlock();
			}

		}

	}

	// 金币场 默认麻将类型
	private int change_game_type_index_in_gold(int game_type_index) {
		int ret = 0;
		switch (game_type_index) {
		case GameConstants.GAME_TYPE_ZZ:// 湖南麻将
		case GameConstants.GAME_TYPE_CS:
		case GameConstants.GAME_TYPE_HZ:
		case GameConstants.GAME_TYPE_SHUANGGUI:
		case GameConstants.GAME_TYPE_ZHUZHOU:
			ret = GameConstants.GAME_TYPE_HZ;
			break;
		case GameConstants.GAME_TYPE_XTHH:// 湖北麻将
			ret = GameConstants.GAME_TYPE_XTHH;
			break;
		case GameConstants.GAME_TYPE_HENAN_AY:// 河南麻将
		case GameConstants.GAME_TYPE_HENAN_LZ:
		case GameConstants.GAME_TYPE_HENAN:
		case GameConstants.GAME_TYPE_HENAN_HZ:
		case GameConstants.GAME_TYPE_HENAN_XY:
		case GameConstants.GAME_TYPE_HENAN_SMX:
		case GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN:
		case GameConstants.GAME_TYPE_HENAN_ZHOU_KOU:
		case GameConstants.GAME_TYPE_HENAN_LH:
			ret = GameConstants.GAME_TYPE_HENAN_HZ;
			break;
		case GameConstants.GAME_TYPE_FLS_LX:// 福禄寿
		case GameConstants.GAME_TYPE_FLS_LX_TWENTY:
		case GameConstants.GAME_TYPE_FLS_LX_CG:// 福禄寿
			ret = GameConstants.GAME_TYPE_FLS_LX;
			break;
		case GameConstants.GAME_TYPE_SXG:// 衡阳
			ret = GameConstants.GAME_TYPE_SXG;
			break;
		case GameConstants.GAME_ID_HH_YX:// 攸县红黑胡
			ret = GameConstants.GAME_ID_HH_YX;
			break;
		case GameConstants.GAME_ID_OX:// 牛牛
			ret = GameConstants.GAME_ID_OX;
			break;
		case GameConstants.GAME_ID_HJK:// 21点
			ret = GameConstants.GAME_ID_HJK;
			break;
		case GameConstants.GAME_ID_PDK:// 跑得快
			ret = GameConstants.GAME_ID_PDK;
			break;
		case GameConstants.GAME_ID_DDZ:
			ret = GameConstants.GAME_ID_DDZ;
			break;
		default:
			break;
		}
		return ret;
	}

	// 金币场 默认玩法
	private int get_game_rule_index_in_gold(int game_type_index) {
		if (game_type_index == GameConstants.GAME_TYPE_HENAN_HZ) {
			return 4629;// 4625=0x1211
		} else if (game_type_index == GameConstants.GAME_TYPE_HZ) {
			return 1046;
		} else if (game_type_index == GameConstants.GAME_TYPE_XTHH) {
			return 50;// 底注1 一赖到底 飘赖有奖
		}
		return 0;// 自摸胡
	}

	/**
	 * 
	 * @param r
	 * @param type
	 *            创建房间的类型 0 普通,1：代理
	 * @return
	 */
	private boolean handler_player_create_room(LogicRoomRequest r, int type, int room_id) {
		// TODO ...

		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();

		if (room_id == 0)
			room_id = r.getRoomId();

		int game_type_index = room_rq.getGameTypeIndex();
		int game_rule_index = room_rq.getGameRuleIndex();
		int game_round = room_rq.getGameRound();

		List<Integer> gameRuleindexEx = room_rq.getGameRuleIndexExList();

		if (type == GameConstants.CREATE_ROOM_SYS) {// 金币场
			game_type_index = change_game_type_index_in_gold(game_type_index);
			game_rule_index = get_game_rule_index_in_gold(game_type_index);
			game_round = 1;
		}

		// 测试牌局
		Room table = createRoom(game_type_index);

		if (gameRuleindexEx != null) {
			int[] ruleEx = new int[gameRuleindexEx.size()];
			for (int i = 0; i < gameRuleindexEx.size(); i++) {
				ruleEx[i] = gameRuleindexEx.get(i);
			}
			table.setGameRuleIndexEx(ruleEx);
		}
		table.setRoom_id(room_id);
		table.setCreate_time(System.currentTimeMillis() / 1000L);
		table.setRoom_owner_account_id(logicRoomAccountItemRequest.getAccountId());
		table.setRoom_owner_name(logicRoomAccountItemRequest.getNickName());
		// WalkerGeek 洛阳杠次底分赋值
		if (game_type_index == GameConstants.GAME_TYPE_HENAN_LYGC) {
			table.init_other_param(room_rq.getBaseScoreGan(), room_rq.getBaseScoreCi(), room_rq.getBaseScore());
		}

		table.set_is_sys(type == GameConstants.CREATE_ROOM_SYS);
		table.base_score = room_rq.getBaseScore();
		table.max_times = room_rq.getMaxTimes();
		table.init_table(game_type_index, game_rule_index, game_round);

		// boolean start =

		PlayerServiceImpl.getInstance().getRoomMap().put(room_id, table);

		// 初始化player,同时放入缓存

		Player player = null;
		if (type == GameConstants.CREATE_ROOM_NORMAL || type == GameConstants.CREATE_ROOM_SYS) {
			player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, room_id, session);
		} else {
			player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, 0, session);// 代理开房传入的房间号为0
		}

		table.handler_create_room(player, type, RoomComonUtil.getMaxNumber(table));

		return true;

	}

	private boolean handler_join_room(LogicRoomRequest r, int room_id) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (table == null) {
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
			msgBuilder.setType(ESysMsgType.NONE.getId());
			msgBuilder.setMsg("房间不存在");
			responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
			send(responseBuilder.build());
			return false;
		}

		ReentrantLock lock = table.getRoomLock();
		try {
			lock.lock();
			handler_player_enter_room(r, room_id);
		} finally {
			lock.unlock();
		}
		return true;
	}

	public static boolean createRoomByBobot(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round, String nickName,
			String groupId, String groupName, int isInner) {
		boolean flag = true;
		try {
			Room table = createRoom(game_type_index);
			table.groupID = groupId;
			table.groupName = groupName;
			table.isInner = isInner;
			table.setRoom_id(roomID);
			table.setCreate_time(System.currentTimeMillis() / 1000L);
			table.setRoom_owner_account_id(accountID);
			table.setRoom_owner_name(nickName);

			table.init_table(game_type_index, game_rule_index, game_round);
			// boolean start =

			PlayerServiceImpl.getInstance().getRoomMap().put(table.getRoom_id(), table);

			// 初始化player,同时放入缓存

			Player player = null;
			player = PlayerServiceImpl.getInstance().createPlayer(accountID);// 代理开房传入的房间号为0

			// table.handler_create_room(player,
			// GameConstants.CREATE_ROOM_ROBOT, getMaxNumber(game_type_index));

			GameSchedule.put(new CreateTimeOutRunnable(roomID), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);

			table.setCreate_type(GameConstants.CREATE_ROOM_ROBOT);
			table.setCreate_player(player);
		} catch (Exception e) {
			flag = false;
			logger.error("创建房间失败", e);
		}
		return flag;
	}

	private static Room createRoom(int game_type_index) {

		MJType mjType = MJType.getType(game_type_index);
		// TODO 这里先这么写。后面考虑要不要加大类型，或者根据game_type区间来判断子类型
		if (mjType != null) {
			return mjType.createTable();
		}

		Room table = null;
		if (game_type_index == GameConstants.GAME_TYPE_FLS_LX || game_type_index == GameConstants.GAME_TYPE_FLS_LX_TWENTY
				|| game_type_index == GameConstants.GAME_TYPE_FLS_LX_DP) {
			table = new FLSTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HH_YX || game_type_index == GameConstants.GAME_TYPE_PHZ_YX
				|| game_type_index == GameConstants.GAME_TYPE_FPHZ_YX || game_type_index == GameConstants.GAME_TYPE_PHZ_CHD
				|| game_type_index == GameConstants.GAME_TYPE_PHZ_XT || game_type_index == GameConstants.GAME_TYPE_LHQ_HD
				|| game_type_index == GameConstants.GAME_TYPE_THK_HY|| game_type_index == GameConstants.GAME_TYPE_WMQ_AX) {
			table = new HHTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_SEVER_OX || game_type_index == GameConstants.GAME_TYPE_SZOX
				|| game_type_index == GameConstants.GAME_TYPE_LZOX || game_type_index == GameConstants.GAME_TYPE_ZYQOX
				|| game_type_index == GameConstants.GAME_TYPE_MSZOX || game_type_index == GameConstants.GAME_TYPE_MFZOX
				|| game_type_index == GameConstants.GAME_TYPE_TBOX || game_type_index == GameConstants.GAME_TYPE_SEVER_OX_LX
				|| game_type_index == GameConstants.GAME_TYPE_SZOX_LX || game_type_index == GameConstants.GAME_TYPE_LZOX_LX
				|| game_type_index == GameConstants.GAME_TYPE_ZYQOX_LX || game_type_index == GameConstants.GAME_TYPE_MSZOX_LX
				|| game_type_index == GameConstants.GAME_TYPE_MFZOX_LX || game_type_index == GameConstants.GAME_TYPE_TBOX_LX) {
			table = new NNTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HJK) {
			table = new HJKTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HJK) {
			table = new BTZTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PDK_FP || game_type_index == GameConstants.GAME_TYPE_PDK_JD
				|| game_type_index == GameConstants.GAME_TYPE_PDK_LZ || game_type_index == GameConstants.GAME_TYPE_PDK_SW) {
			if (game_type_index == GameConstants.GAME_TYPE_PDK_JD) {
				table = new PDK_JD_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_SW) {
				table = new PDK_FIFTEEN_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_LZ) {
				table = new PDK_LZ_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_FP) {
				table = new PDK_SR_Table();
			} else {
				table = new PDKTable();
			}
		} else if (game_type_index == GameConstants.GAME_TYPE_DDZ_JD) {
			table = new DDZ_JD_Table();
		} else if (game_type_index == GameConstants.GAME_TYPE_ZJH_JD) {
			table = new ZJHTable();
		} else {
			table = new MJTable();
		}
		return table;
	}

	private boolean handler_player_enter_room(LogicRoomRequest r, int room_id) {
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();

		// int room_id = r.getRoomId();
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		// 刷新时间
		table.process_flush_time();

		// 初始化player
		Player player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, room_id, session);

		// 重入判断
		if (table.get_player(logicRoomAccountItemRequest.getAccountId()) != null) {
			send_error_notify(player, 2, "您已经加入房间");
			return false;
		}

		boolean flag = false;
		if (r.getType() == BE_OBSERVER) {
			table.handler_enter_room_observer(player);
		} else {
			flag = table.handler_enter_room(player);
		}
		if (flag) {

		}

		return false;

	}

	private boolean handler_player_reconnect_room(LogicRoomRequest r) {
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();

		int room_id = r.getRoomId();
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		// 刷新时间
		table.process_flush_time();

		long accountId = logicRoomAccountItemRequest.getAccountId();
		Player player = table.get_player(accountId);
		if ((player == null) && (player = table.observers().getPlayer(accountId)) == null) {
			return false;
		}
		// 重定位转发服的相关位置
		player.setChannel(session.getChannel());
		player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
		player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
		player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
		player.setOnline(true);
		return table.handler_reconnect_room(player);

	}

	public boolean send_error_notify(Player player, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;

	}
}
