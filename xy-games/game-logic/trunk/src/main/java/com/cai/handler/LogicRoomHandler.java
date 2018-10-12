package com.cai.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.ai.Gamer;
import com.cai.clubmatch.ClubMatchPlayer;
import com.cai.common.config.ExclusiveGoldCfg;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EGameType;
import com.cai.common.define.ELogType;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.EServerType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AccountMatchRedis;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.type.MatchType;
import com.cai.common.util.Bits;
import com.cai.common.util.BullFightUtil;
import com.cai.common.util.Pair;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.core.Global;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.Session;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.abz.PUKEType;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.TTZTable;
import com.cai.game.btz.handler.tb.TBTable;
import com.cai.game.bullfight.newyy.YynOxTable;
import com.cai.game.chdphz.CHDPHZTable;
import com.cai.game.czbg.CZBGTable;
import com.cai.game.czwxox.CZWXOXTable;
import com.cai.game.dbd.DBDType;
import com.cai.game.dbn.DBNTable;
import com.cai.game.ddz.handler.henanddz.DDZ_HENAN_Table;
import com.cai.game.ddz.handler.jdddz.DDZ_JD_Table;
import com.cai.game.dzd.DZDTable;
import com.cai.game.eightox.EIGHTOXTable;
import com.cai.game.fkn.FKNTable;
import com.cai.game.fkpsh.FKPSHTable;
import com.cai.game.fls.FLSTable;
import com.cai.game.gdy.GDYType;
import com.cai.game.gxzp.GXZPTable;
import com.cai.game.gzp.GZPTable;
import com.cai.game.hbzp.HBPHZTable;
import com.cai.game.hh.HHTable;
import com.cai.game.hh.HHType;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hongershi.HongErShiTable;
import com.cai.game.hongershi.hy.HongErShiTable_HY;
import com.cai.game.hongershi.pj.HongErShiTable_PJ;
import com.cai.game.huaihuaox.HUAIHUAOXTable;
import com.cai.game.jdb.JDBTable;
import com.cai.game.jxklox.JXKLOXTable;
import com.cai.game.klox.KLOXTable;
import com.cai.game.laopai.LPType;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.MJType;
import com.cai.game.nn.NNTable;
import com.cai.game.paijiu.PJType;
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.PDKType;
import com.cai.game.pdk.handler.fifteenpdk.PDK_FIFTEEN_Table;
import com.cai.game.pdk.handler.jdpdk.PDK_JD_Table;
import com.cai.game.pdk.handler.laizipdk.PDK_LZ_Table;
import com.cai.game.pdk.handler.ll_fifteenpdk.LL_PDK_FIFTEEN_Table;
import com.cai.game.pdk.handler.ll_jdpdk.LL_PDK_JD_Table;
import com.cai.game.pdk.handler.srpdk.PDK_SR_Table;
import com.cai.game.phu.PHTable;
import com.cai.game.phz.PHZType;
import com.cai.game.pshox.PSHOXTable;
import com.cai.game.qjqf.QJQFTable;
import com.cai.game.schcp.SCHCPTable;
import com.cai.game.schcp.SCHCPType;
import com.cai.game.schcpdss.SCHCPDSSTable;
import com.cai.game.schcpdss.SCHCPDSSType;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.schcpdz.SCHCPDZType;
import com.cai.game.scphz.SCPHZType;
import com.cai.game.sdh.SDHType;
import com.cai.game.sg.SGTable;
import com.cai.game.shidianban.SDBTable;
import com.cai.game.shisanzhang.SSZType;
import com.cai.game.tdz.TDZType;
import com.cai.game.universal.bullfight.BullFightTable;
import com.cai.game.universal.creazybullfight.CreazyBullFightTable;
import com.cai.game.universal.doubanniu.DouBanNiuTable;
import com.cai.game.wmq.WMQTable;
import com.cai.game.wsk.WSKType;
import com.cai.game.xykl.XYKLTable;
import com.cai.game.yyox.YYOXTable;
import com.cai.game.yyqf.YYQFTable;
import com.cai.game.zjh.ZJHType;
import com.cai.match.MatchPlayer;
import com.cai.net.core.ClientHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RoomMapingService;
import com.cai.service.SessionServiceImpl;
import com.cai.util.ClubMsgSender;
import com.cai.util.MessageResponse;
import com.cai.util.RedisRoomUtil;
import com.cai.util.SystemRoomUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Ints;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.LogicRoomAccountItemRequest;
import protobuf.clazz.Protocol.LogicRoomRequest;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;

/**
 * 房间
 *
 * @author run
 */
public class LogicRoomHandler extends ClientHandler<LogicRoomRequest> {

	protected static final Logger logger = LoggerFactory.getLogger(LogicRoomHandler.class);
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
	 * 上帝视角
	 */
	private static final int BE_GOD_VIEW_OBSERVER = 59;

	/**
	 * 代理房间创建
	 */
	private static final int PROXY_ROOM_CREATE = 51;
	/**
	 * 加入金币场
	 */
	private static final int JOIN_GOLD_ROOM = 53;

	private static final int UPDATE_MONEY_AND_GOLD = 5;// 刷新玩家的金币 豆子信息

	/**
	 * 用于俱乐部创建房间后直接观望而不是坐下，备用
	 */
	private static final int CLUB_PROXY_ROOM_CREATE_AND_OBSERVER = 66;

	/**
	 * 单个玩家创建房间调用频率限制
	 */
	private static final LoadingCache<Long, Boolean> createCache = CacheBuilder.newBuilder().maximumSize(2000000L)
			.expireAfterWrite(2, TimeUnit.SECONDS).build(new CacheLoader<Long, Boolean>() {
				@Override
				public Boolean load(Long key) throws Exception {
					return null;
				}
			});
	/**
	 * 单个玩家加入房间调用频率限制
	 */
	private static final LoadingCache<Long, Boolean> joinCache = CacheBuilder.newBuilder().maximumSize(2000000L).expireAfterWrite(2, TimeUnit.SECONDS)
			.build(new CacheLoader<Long, Boolean>() {
				@Override
				public Boolean load(Long key) throws Exception {
					return null;
				}
			});

	public static boolean isBlockVisit(long accountId, boolean isCreate) {
		Boolean value = (isCreate ? createCache : joinCache).getIfPresent(accountId);
		if (null == value) {
			(isCreate ? createCache : joinCache).put(accountId, Boolean.TRUE);
			return false;
		}
		return true;
	}

	@Override
	public void execute() throws Exception {
		final int roomId = request.getRoomId();
		if (roomId > 0) {
			AbstractRoom room = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
			if (null != room) {
				room.runInRoomLoop(() -> {
					try {
						doExecute();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} else {
				logger.error("request logicroom type="+request.getType());
				doExecuteBack();
			}
		} else {//基本上是协议4
			logger.error("request logic room type="+request.getType());
			doExecuteBack();
		}
	}
	
	
	
	private void doExecuteBack() {
		Global.getRoomPoolBACK().execute(new Runnable() {
			
			@Override
			public void run() {
				try{
					doExecute();
				}catch(Exception e) {
					e.printStackTrace();
				}
			
			}
		});
		
	}

	@Override
	public void onRequest() throws Exception {

		LogicRoomRequest r = request;

		int type = r.getType();

		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();

		// 请求id
		logicRoomAccountItemRequest.getAccountId();

		int room_id = r.getRoomId();

		
		if (type == CRATE_ROOM) {

			handler_player_create_room(r, GameConstants.CREATE_ROOM_NORMAL, room_id, session);

		} else if (type == JOIN_ROOM || type == BE_OBSERVER || type == BE_GOD_VIEW_OBSERVER) {
			handler_join_room(r, room_id, session);
		} else if (type == RESET_CONNECT) {// 重连返回10

			AbstractRoom table = PlayerServiceImpl.getInstance().getRoomMap().get(r.getRoomId());

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
				RedisRoomUtil.clearRoom(r.getLogicRoomAccountItemRequest().getAccountId(), r.getRoomId());
				/////////////
				RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class)
						.hGet(RedisConstant.ROOM, r.getRoomId() + "", RoomRedisModel.class);

				if (roomRedisModel != null && roomRedisModel.getPlayersIdSet() != null) {

					roomRedisModel.getPlayersIdSet().remove(r.getLogicRoomAccountItemRequest().getAccountId());
					roomRedisModel.getNames().remove(r.getLogicRoomAccountItemRequest().getNickName());
				}
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, r.getRoomId() + "", roomRedisModel);

				logger.error("table == null" + r.getRoomId() + ",玩家id=" + r.getLogicRoomAccountItemRequest().getAccountId());

				return;
			}

			// ===========是否在这个房间里============
			boolean flag = false;
			for (Player p : table.get_players()) {
				if (p == null)
					continue;
				if (p.getAccount_id() == r.getLogicRoomAccountItemRequest().getAccountId()) {
					if (table.is_match()) {
						MatchPlayer player = (MatchPlayer) p;
						if (player.isLeave()) {
							break;
						}
					}
					flag = true;
					break;
				}
			}
			if (!flag) {

				long accountId = r.getLogicRoomAccountItemRequest().getAccountId();
				// 数据不一致时修复
				// 返回消息
				logger.error("数据不一致时修复" + table.getRoom_id() + "玩家id=" + r.getLogicRoomAccountItemRequest().getAccountId());

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(3);// 3重连返回,没有有效房间
				Response.Builder responseBuilder = Response.newBuilder();
				responseBuilder.setResponseType(ResponseType.ROOM);
				responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
				send(responseBuilder.build());

				// ========同步到中心========
				RedisRoomUtil.clearRoom(r.getLogicRoomAccountItemRequest().getAccountId(), r.getRoomId());

				/////////////
				RoomRedisModel roomRedisModel = table.roomRedisModel;
				roomRedisModel.getPlayersIdSet().remove(r.getLogicRoomAccountItemRequest().getAccountId());
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, r.getRoomId() + "", roomRedisModel);

				// 修复
				if (table.clubInfo.clubId > 0) {
					Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(accountId);
					if (null == player) {
						player = new Gamer();
						player.setAccount_id(accountId);
					}
					ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_EXIT, table, player);
				}

				return;

			}
			// ======================

			handler_player_reconnect_room(r);

			// System.out.println("重连的房间间:"+ room_id);
			// System.out.println("重连接的玩家信息:" + logicRoomAccountItemRequest);

		} else if (type == OFFLINE) {
			long account_id = request.getAccountId();
			// System.out.println("收到下线消息 "+account_id);
			Player player = PlayerServiceImpl.getInstance().getPlayerMap().get(account_id);
//			if (player == null) {//感觉这段代码没啥必要性
//				int roomId = SystemRoomUtil.getRoomId(account_id);
//				if (roomId <= 0) {
//					return;
//				}
//				Room table = PlayerServiceImpl.getInstance().getRoomMap().get(roomId);
//				if (table == null) {
//					return;
//				}
//				player = table.getPlayer(account_id);
//			}

			if (player == null)
				return;

			if (player.getChannel() != session.getChannel()) {
				logger.error("玩家下线 但是下线的 channel 跟当前的 channel 不一致");
				return;
			}
			// TODO 下线后的操作

			room_id = player.getRoom_id();
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
			if (table == null) {
				return;
			}
			player.setOnline(false);
			// 刷新时间
			table.process_flush_time();

			table.handler_player_offline(player);

		} else if (type == PROXY_ROOM_CREATE) {
			// 创建代理房间
			handler_player_create_room(r, GameConstants.CREATE_ROOM_PROXY, 0, session);
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
			AbstractRoom suitableRoom = null;
			int logicIndex = -1;
			Map<Integer, AbstractRoom> roomMap = PlayerServiceImpl.getInstance().goldRooms;
			for (Map.Entry<Integer, AbstractRoom> entry : roomMap.entrySet()) {
				// 找金币场 且未满 且玩法相同 的房间
				if (entry.getValue().is_sys() && entry.getValue().getPlayerCount() < RoomComonUtil
						.getMaxNumber(entry.getValue(), entry.getValue().getDescParams()) && entry.getValue().getGameTypeIndex() == game_type_index
						&& entry.getValue().getGameRuleIndex() == game_rule_index) {
					if (suitableRoom != null) {
						suitableRoom = entry.getValue().getPlayerCount() > suitableRoom.getPlayerCount() ? entry.getValue() : suitableRoom;
					} else {
						suitableRoom = entry.getValue();
					}
				}
			}

			RoomRedisModel roomRedisModel = null;
			if (suitableRoom != null) {
				room_id = suitableRoom.getRoom_id();
				roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, room_id + "", RoomRedisModel.class);
				if (roomRedisModel == null) {
					suitableRoom = null;
					logger.error("逻辑服有房间，redis清理掉了。。。" + room_id);
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
				roomRedisModel = new RoomRedisModel();
				roomRedisModel.setRoom_id(room_id);
				roomRedisModel.setLogic_index(SystemConfig.logic_index);// TODO
				// 临时
				roomRedisModel.getPlayersIdSet().add(accountId);

				roomRedisModel.setCreate_time(System.currentTimeMillis());
				roomRedisModel.setGame_round(1);
				roomRedisModel.setMoneyRoom(true);
				roomRedisModel.setGame_rule_index(get_game_rule_index_in_gold(game_type_index));
				roomRedisModel.setGame_type_index(game_type_index);

				roomRedisModel.setGame_id(r.getGameId());
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

				// ========同步到中心========
				RedisRoomUtil.clearRoom(r.getLogicRoomAccountItemRequest().getAccountId(), r.getRoomId());

				logicIndex = SystemConfig.logic_index;
				handler_player_create_room(r, GameConstants.CREATE_ROOM_SYS, room_id, session);
			} else {// 加入房间
				long accountId = (long) session.getAccountID();
				accountId = logicRoomAccountItemRequest.getAccountId();
				roomRedisModel = suitableRoom.roomRedisModel;
				roomRedisModel.getPlayersIdSet().add(accountId);
				// 写入redis
				SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, room_id + "", roomRedisModel);

				// ========同步到中心========
				RedisRoomUtil.clearRoom(r.getLogicRoomAccountItemRequest().getAccountId(), r.getRoomId());

				logicIndex = roomRedisModel.getLogic_index();
				handler_join_room(r, room_id, session);
			}

			// ===== 金币场 ======
			// GoldRoomMsgReq.Builder builder = GoldRoomMsgReq.newBuilder();
			// builder.setAccountId(logicRoomAccountItemRequest.getAccountId());
			// builder.setLogicIndex(logicIndex);
			// session.channel.writeAndFlush(PBUtil.toS2SRequet(S2SCmd.GOLD_ROOM_MSG,
			// builder));

		} else if (type == UPDATE_MONEY_AND_GOLD) {// 刷新 房间里的玩家的 金币 豆子信息
			long accountid = r.getLogicRoomAccountItemRequest().getAccountId();
			Room table = PlayerServiceImpl.getInstance().getRoomMap().get(r.getRoomId());

			if (table == null)// 不在房间里面 不需要通知
				return;

			if (table.getCreate_type() == GameConstants.CREATE_ROOM_NEW_COIN) {
				return;
			}

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

		} else if (type == CLUB_PROXY_ROOM_CREATE_AND_OBSERVER) {
			handler_player_create_room(r, GameConstants.CREATE_ROOM_PROXY, 0, session);
			handler_join_room(r.toBuilder().setType(56).build(), room_id, session);
		}

	}

	// 金币场 默认麻将类型
	private static int change_game_type_index_in_gold(int game_type_index) {
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
		case GameConstants.GAME_TYPE_NEW_AN_YANG:
		case GameConstants.GAME_TYPE_NEW_LIN_ZHOU:
		case GameConstants.GAME_TYPE_NEW_HE_NAN:
		case GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN:
		case GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG:
		case GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA:
		case GameConstants.GAME_TYPE_NEW_ZHOU_KOU:
		case GameConstants.GAME_TYPE_NEW_LUO_HE:
			ret = GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG;
			break;
		case GameConstants.GAME_TYPE_FLS_LX:// 福禄寿
		case GameConstants.GAME_TYPE_FLS_LX_TWENTY:
		case GameConstants.GAME_TYPE_FLS_LX_CG:// 福禄寿
		case GameConstants.GAME_TYPE_FLS_LX_THREE:// 福禄寿
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
		case GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ:
			ret = GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ;
			break;
		case GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH:
			ret = GameConstants.GAME_TYPE_HONG_ZHONG_MJ_TH;
			break;
		case GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON:
			ret = GameConstants.GAME_TYPE_MJ_YUYANG_HONGZHON;
			break;
		case GameConstants.GAME_TYPE_LHQ_HD:
			ret = GameConstants.GAME_TYPE_LHQ_HD;
		default:
			break;
		}
		return ret;
	}

	// 金币场 默认玩法
	private static int get_game_rule_index_in_gold(int game_type_index) {
		if (game_type_index == GameConstants.GAME_TYPE_HENAN_HZ || game_type_index == GameConstants.GAME_TYPE_NEW_HN_HONG_ZHONG) {
			return 4629;// 4625=0x1211
		} else if (game_type_index == GameConstants.GAME_TYPE_HZ) {
			return 1046;
		} else if (game_type_index == GameConstants.GAME_TYPE_XTHH) {
			return 50;// 底注1 一赖到底 飘赖有奖
		} else if (game_type_index == GameConstants.GAME_TYPE_LHQ_HD) {
			return 153880595;
		}
		return 0;// 自摸胡
	}

	/**
	 * @param r
	 * @param type 创建房间的类型 0 普通,1：代理
	 * @return
	 */
	public static boolean handler_player_create_room(LogicRoomRequest r, int type, int room_id, Session session) {

		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();

		// if (isBlockVisit(logicRoomAccountItemRequest.getAccountId(), true)) {
		// logger.error("handler_player_create_room 玩家:{} 访问频率过高！",
		// logicRoomAccountItemRequest.getAccountId());
		// return false;
		// }

		if (room_id == 0)
			room_id = r.getRoomId();

		int game_type_index = room_rq.getGameTypeIndex();
		int game_rule_index = room_rq.getGameRuleIndex();
		int game_round = room_rq.getGameRound();
		List<Integer> gameRuleindexEx = room_rq.getGameRuleIndexExList();
		// value = 0; 客户端默认不发
		if (type == GameConstants.CREATE_ROOM_SYS) {// 金币场
			game_type_index = change_game_type_index_in_gold(game_type_index);
			game_rule_index = get_game_rule_index_in_gold(game_type_index);
			game_round = 1;
		} else if (room_rq.getClubId() > 0 || room_rq.getIsNewRule()) {
			game_rule_index = Room.getNewRuleIndex(room_rq.getNewRules());
		}

		// 测试牌局
		AbstractRoom table = createRoom(game_type_index, game_rule_index);
		table.setCreate_type(type);
		table.setRoom_id(room_id);
		table.setCreate_time(System.currentTimeMillis() / 1000L);
		table.setRoom_owner_account_id(logicRoomAccountItemRequest.getAccountId());
		table.setRoom_owner_name(logicRoomAccountItemRequest.getNickName());
		// 扣俱乐部拥有者
		if (logicRoomAccountItemRequest.getClubOwner() > 0) {
			table.setRoom_owner_account_id(logicRoomAccountItemRequest.getClubOwner());
			table.setRoom_owner_name(r.getClubOwnerAccount().getNickName());
		}

		table.club_id = room_rq.getClubId();
		if (table.club_id > 0) {
			table.clubInfo.clubId = table.club_id;
			table.clubInfo.ruleId = room_rq.getRuleId();
			table.clubInfo.clubName = room_rq.getClubName();
			// 俱乐部成员一定大于0
			if (room_rq.getClubMemberSize() > 0) {
				table.clubInfo.clubMemberSize = room_rq.getClubMemberSize();
			}
			if (logicRoomAccountItemRequest.hasJoinId()) {
				Pair<Integer, Integer> pv = Bits.getLH(logicRoomAccountItemRequest.getJoinId());
				table.clubInfo.index = pv.getFirst().intValue();
			}
			LogicRoomAccountItemRequest clubOwnerAccount = r.getClubOwnerAccount();
			table.setRoom_owner_name(clubOwnerAccount.getNickName());
		}

		// 初始化player,同时放入缓存
		Player player = null;
		if (type == GameConstants.CREATE_ROOM_NORMAL || type == GameConstants.CREATE_ROOM_SYS /*
		 * || table.club_id
		 * > 0
		 */) {
			player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, room_id, session);
		} else {
			player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, 0, session);// 代理开房传入的房间号为0
		}

		boolean flag = true;
		//
		// 新的玩法数据结构
		if (table.club_id > 0 || room_rq.getIsNewRule()) {
			table.initNewRule(room_rq.getNewRules());
			flag = table.init_table(game_type_index, table._game_rule_index, game_round);
		} else {

			if (gameRuleindexEx != null && gameRuleindexEx.size() > 0) {
				table.setGameRuleIndexEx(Ints.toArray(gameRuleindexEx));
			}
			table.getGameRuleIndexEx()[0] = game_rule_index;

			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_CI, room_rq.getBaseScoreCi());
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_GANG, room_rq.getBaseScoreGan());
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE, room_rq.getBaseScore());
			table.putRule(GameConstants.GAME_RULE_MAX_TIMES, room_rq.getMaxTimes());

			flag = table.init_table(game_type_index, game_rule_index, game_round);
		}

		if (flag == false) {
			MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
			e.setType(type);
			if (ExclusiveGoldCfg.get().isMustCostExclusive(table.getGame_id())) {
				e.setMsg(ExclusiveGoldCfg.get().lackExclusiveTip(table.getGame_id()));
			} else {
				e.setMsg("开局失败，豆不够");
			}
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
			PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

			logger.error("开局失败，豆不够 房间Id{} 俱乐部Id{} 创建人Id{}", room_id, table.club_id, logicRoomAccountItemRequest.getAccountId());
			SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM, String.valueOf(room_id).getBytes());
			return flag;
		}

		if ((type != GameConstants.CREATE_ROOM_PROXY && type != GameConstants.CREATE_ROOM_ROBOT) && !table
				.onPlayerEnterUpdateRedis(player.getAccount_id())) {
			send_error_notify(player, 1, "已在其他房间中");
			SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM, String.valueOf(room_id).getBytes());
			return false;
		}

		// boolean start =
		PlayerServiceImpl.getInstance().getRoomMap().put(room_id, table);
		if (type == GameConstants.CREATE_ROOM_SYS) {
			PlayerServiceImpl.getInstance().goldRooms.put(room_id, table);
		}

		boolean ret = table.handler_create_room(player, type, RoomComonUtil.getMaxNumber(table.getDescParams()));
		if (!ret) {
			logger.error("table.handler_create_room error,roomid:{},type:{},player:{}", room_id, type, player.getAccount_id());
		}
		if (table.club_id > 0 && r.hasClubOwnerAccount()) {
			Player clubOwner = PlayerServiceImpl.getInstance().getClubCreatePlayer(r.getClubOwnerAccount(), session);
			table.setCreate_player(clubOwner);
		}

		if (table.club_id > 0) {
			table.setCreate_type(GameConstants.CREATE_ROOM_CLUB);
		}
		// 俱乐部房间,通知俱乐部服.在此处处理,不需要在每个子游戏的单独处理,偷了个懒
		if (ret && (table.club_id > 0 && logicRoomAccountItemRequest.getJoinId() >= 0)) {
			player.setClubJoinId(logicRoomAccountItemRequest.getJoinId());
			ClubMsgSender.roomStatusUpdate(ERoomStatus.CREATE, table);
			if (GameConstants.CREATE_ROOM_NORMAL == type) { //
				ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_ENTER, table, player);
			}
		}
		return ret;
	}

	public static boolean handler_join_room(LogicRoomRequest r, int room_id, Session session) {
		Room table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
		if (table == null) {
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
			msgBuilder.setType(ESysMsgType.NONE.getId());
			msgBuilder.setMsg("房间不存在");
			responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
			session.send(responseBuilder.build());
			logger.error("handler_join_room房间不存在" + room_id + r);

			// 兼容创建房间失败###############################
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.ROOM, Integer.toString(room_id), RoomRedisModel.class);
			if (null != roomRedisModel) {
				if (roomRedisModel.getLogic_index() == SystemConfig.logic_index) {
					logger.warn("######################### delete room :{}", room_id);
					SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM, String.valueOf(room_id).getBytes());
				}
			}

			return false;
		}
		int roomId = SystemRoomUtil.getRoomId(r.getLogicRoomAccountItemRequest().getAccountId());

		// 频率
		if (r.getType() == JOIN_ROOM && isBlockVisit(r.getLogicRoomAccountItemRequest().getAccountId(), false)) {
			logger.error("handler_join_room  玩家:{} 访问频率过高！", r.getLogicRoomAccountItemRequest().getAccountId());
			return false;
		}

		String tip = null;
		if (roomId > 0) {
			tip = "您已经在房间中";
		} else {
			AccountMatchRedis accountMatchRedis = SpringService.getBean(RedisService.class)
					.hGet(RedisConstant.MATCH_ROOM_ACCOUNT, r.getLogicRoomAccountItemRequest().getAccountId() + "", AccountMatchRedis.class);
			if (accountMatchRedis != null && accountMatchRedis.isStart()) {
				tip = "您已经报名比赛了";
			}
		}

		if (tip != null) {
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.MSG);
			MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
			msgBuilder.setType(ESysMsgType.NONE.getId());
			msgBuilder.setMsg(tip);
			responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
			session.send(responseBuilder.build());
			logger.error("handler_join_room您已经在房间中room:{},accountid:{},msg:{}", room_id, r.getLogicRoomAccountItemRequest().getAccountId(), r);
			return false;
		}

		boolean ret = false;
		ReentrantLock lock = table.getRoomLock();
		try {
			lock.lock();
			ret = handler_player_enter_room(r, room_id, session);
		} finally {
			lock.unlock();
		}
		return ret;
	}

	public static boolean createRoomByBobot(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round, String nickName,
			String groupId, String groupName, int isInner) {
		boolean flag = true;
		try {
			AbstractRoom table = createRoom(game_type_index, game_rule_index);
			table.groupID = groupId;
			table.groupName = groupName;
			table.isInner = isInner;
			table.setRoom_id(roomID);
			table.setCreate_time(System.currentTimeMillis() / 1000L);
			table.setRoom_owner_account_id(accountID);
			table.setRoom_owner_name(nickName);
			table.setGameRuleIndexEx(new int[] { game_rule_index });
			flag = table.init_table(game_type_index, game_rule_index, game_round);
			if (flag == false) {
				return flag;
			}

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

	public static boolean createRoomByBobotExtend(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round,
			String nickName, String groupId, String groupName, int isInner, int exRule, int fanshu, int baseScore, int gangScore, int ciScore,
			int WcTimes) {
		boolean flag = true;
		try {
			AbstractRoom table = createRoom(game_type_index, game_rule_index);
			table.groupID = groupId;
			table.groupName = groupName;
			table.isInner = isInner;
			table.setRoom_id(roomID);
			table.setCreate_time(System.currentTimeMillis() / 1000L);
			table.setRoom_owner_account_id(accountID);
			table.setRoom_owner_name(nickName);
			table.setGameRuleIndexEx(new int[] { game_rule_index, exRule });

			table.putRule(GameConstants.GAME_RULE_BASE_SCORE, baseScore);
			table.putRule(GameConstants.GAME_RULE_MAX_TIMES, fanshu);
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_CI, ciScore);
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_GANG, gangScore);
			if (WcTimes > 0) {
				// 望城跑胡子30胡息以上见1加(1~10)
				table.putRule(7, WcTimes);
			}

			flag = table.init_table(game_type_index, game_rule_index, game_round);

			if (flag == false) {
				return flag;
			}

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

	public static boolean createRoomByBobotExtend(long accountID, int roomID, int game_type_index, int game_rule_index, int game_round,
			String nickName, String groupId, String groupName, int isInner, int exRule, int fanshu, int baseScore, int gangScore, int ciScore) {
		boolean flag = true;
		try {
			AbstractRoom table = createRoom(game_type_index, game_rule_index);
			table.groupID = groupId;
			table.groupName = groupName;
			table.isInner = isInner;
			table.setRoom_id(roomID);
			table.setCreate_time(System.currentTimeMillis() / 1000L);
			table.setRoom_owner_account_id(accountID);
			table.setRoom_owner_name(nickName);
			table.setGameRuleIndexEx(new int[] { game_rule_index, exRule });

			table.putRule(GameConstants.GAME_RULE_BASE_SCORE, baseScore);
			table.putRule(GameConstants.GAME_RULE_MAX_TIMES, fanshu);
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_CI, ciScore);
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_GANG, gangScore);
			// if(WcTimes>0){
			// //望城跑胡子30胡息以上见1加(1~10)
			// table.putRule(7, WcTimes);
			// }

			flag = table.init_table(game_type_index, game_rule_index, game_round);

			if (flag == false) {
				return flag;
			}

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

	public static boolean createRoomByBobotExtend(long accountID, int roomID, int game_type_index, int game_round, String nickName, String groupId,
			String groupName, int isInner, Map<Integer, Integer> map) {
		boolean flag = true;
		try {
			int game_rule_index = Room.game_rule_index(map);
			AbstractRoom table = createRoom(game_type_index, game_rule_index);
			table.groupID = groupId;
			table.groupName = groupName;
			table.isInner = isInner;
			table.setRoom_id(roomID);
			table.setCreate_time(System.currentTimeMillis() / 1000L);
			table.setRoom_owner_account_id(accountID);
			table.setRoom_owner_name(nickName);
			table.initRobotRule(map);
			flag = table.init_table(game_type_index, game_rule_index, game_round);
			if (flag == false) {
				return flag;
			}

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

	public static AbstractRoom createRoom(int game_type_index, int game_rule_index) {

		MJType mjType = MJType.getType(game_type_index);
		// TODO 这里先这么写。后面考虑要不要加大类型，或者根据game_type区间来判断子类型
		if (mjType != null) {
			return mjType.createTable();
		}

		HHType hhType = HHType.getType(game_type_index);
		if (hhType != null) {
			return hhType.createTable();
		}

		PHZType phzType = PHZType.getType(game_type_index);
		if (phzType != null) {
			return phzType.createTable();
		}
		LPType lpType = LPType.getType(game_type_index);
		if (lpType != null) {
			return lpType.createTable();
		}
		SSZType sszType = SSZType.getType(game_type_index);
		if (sszType != null) {
			return sszType.createTable();
		}
		SDHType sdhType = SDHType.getType(game_type_index);
		if (sdhType != null) {
			return sdhType.createTable();
		}
		TDZType tdzType = TDZType.getType(game_type_index);
		if (tdzType != null) {
			return tdzType.createTable();
		}
		WSKType wskType = WSKType.getType(game_type_index);
		if (wskType != null) {
			return wskType.createTable();
		}
		GDYType gdyType = GDYType.getType(game_type_index);
		if (gdyType != null) {
			return gdyType.createTable();
		}
		DBDType dbdType = DBDType.getType(game_type_index);
		if (dbdType != null) {
			return dbdType.createTable();
		}
		PJType pjType = PJType.getType(game_type_index);
		if (pjType != null) {
			return pjType.createTable();
		}
		ZJHType zjhType = ZJHType.getType(game_type_index);
		if (zjhType != null) {
			return zjhType.createTable();
		}
		PUKEType pukeType = PUKEType.getType(game_type_index);
		if (pukeType != null) {
			return pukeType.createTable();
		}
		PDKType pdkType = PDKType.getType(game_type_index);
		if (pdkType != null) {
			return pdkType.createTable();
		}
		SCHCPDZType schcpdzType = SCHCPDZType.getType(game_type_index);
		if (schcpdzType != null) {
			return schcpdzType.createTable();
		}
		SCHCPType schcpType = SCHCPType.getType(game_type_index);
		if (schcpType != null) {
			return schcpType.createTable();
		}
		SCPHZType schphzType = SCPHZType.getType(game_type_index);
		if (schphzType != null) {
			return schphzType.createTable();
		}
		SCHCPDSSType schcpdssType = SCHCPDSSType.getType(game_type_index);
		if (schcpdssType != null) {
			return schcpdssType.createTable();
		}
		AbstractRoom table = null;

		// 新创房机制
		final RoomMapingService service = RoomMapingService.getInstance();
		table = service.createRoom(game_type_index);
		if (null != table) {
			return table;
		}

		if (game_type_index == GameConstants.GAME_TYPE_FLS_LX || game_type_index == GameConstants.GAME_TYPE_FLS_LX_TWENTY
				|| game_type_index == GameConstants.GAME_TYPE_FLS_LX_DP || game_type_index == GameConstants.GAME_TYPE_FLS_LX_THREE
				|| game_type_index == GameConstants.GAME_TYPE_FLS_LX_TWO) {
			table = new FLSTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HH_YX || game_type_index == GameConstants.GAME_TYPE_PHZ_YX
				|| game_type_index == GameConstants.GAME_TYPE_FPHZ_YX || game_type_index == GameConstants.GAME_TYPE_PHZ_XT
				|| game_type_index == GameConstants.GAME_TYPE_LHQ_HD || game_type_index == GameConstants.GAME_TYPE_LHQ_HY
				|| game_type_index == GameConstants.GAME_TYPE_LHQ_QD || game_type_index == GameConstants.GAME_TYPE_THK_HY
				|| game_type_index == GameConstants.GAME_TYPE_HGW_HH || game_type_index == GameConstants.GAME_TYPE_468_HONG_GUAI_WAN) {
			table = new HHTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PHZ_CHD || game_type_index == GameConstants.GAME_TYPE_HH_CHD
				|| game_type_index == GameConstants.GAME_TYPE_DHD_CHD || game_type_index == GameConstants.GAME_TYPE_LBA_CHD
				|| game_type_index == GameConstants.GAME_TYPE_PHZ_CHD_DT || game_type_index == GameConstants.GAME_TYPE_HH_CHD_DT
				|| game_type_index == GameConstants.GAME_TYPE_DHD_CHD_DT || game_type_index == GameConstants.GAME_TYPE_LBA_CHD_DT) {

			table = new CHDPHZTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PHU_YX) {

			table = new PHTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_WMQ_AX || game_type_index == GameConstants.GAME_TYPE_WMQ_AX_S) {
			table = new WMQTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PHZ_GUILIN_ZP || game_type_index == GameConstants.GAME_TYPE_PHZ_BAYI_ZP) {
			table = new GXZPTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_SEVER_OX || game_type_index == GameConstants.GAME_TYPE_SZOX
				|| game_type_index == GameConstants.GAME_TYPE_LZOX || game_type_index == GameConstants.GAME_TYPE_ZYQOX
				|| game_type_index == GameConstants.GAME_TYPE_MSZOX || game_type_index == GameConstants.GAME_TYPE_MFZOX
				|| game_type_index == GameConstants.GAME_TYPE_TBOX || game_type_index == GameConstants.GAME_TYPE_SEVER_OX_LX
				|| game_type_index == GameConstants.GAME_TYPE_SZOX_LX || game_type_index == GameConstants.GAME_TYPE_LZOX_LX
				|| game_type_index == GameConstants.GAME_TYPE_ZYQOX_LX || game_type_index == GameConstants.GAME_TYPE_MSZOX_LX
				|| game_type_index == GameConstants.GAME_TYPE_MFZOX_LX || game_type_index == GameConstants.GAME_TYPE_TBOX_LX
				|| game_type_index == GameConstants.GAME_TYPE_JDOX_YY || game_type_index == GameConstants.GAME_TYPE_KSZOX) {
			table = new NNTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_DBN) {
			table = new DBNTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_FKN) {
			table = new FKNTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_KLJDOX || game_type_index == GameConstants.GAME_TYPE_KLFKOX
				|| game_type_index == GameConstants.GAME_TYPE_KLTBOX || game_type_index == GameConstants.GAME_TYPE_KLDGOX) {
			table = new KLOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_KLJX_NNSZ || game_type_index == GameConstants.GAME_TYPE_KLJX_ZYQZ
				|| game_type_index == GameConstants.GAME_TYPE_KLJX_SIX_MPQZ || game_type_index == GameConstants.GAME_TYPE_KLJX_DGOX
				|| game_type_index == GameConstants.GAME_TYPE_KLJX_EIGHT_MPQZ) {
			table = new JXKLOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_CZWXOX) {
			table = new CZWXOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PSH_OX) {
			table = new PSHOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PSH_FK_OX) {
			table = new FKPSHTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_EIGHT_OX || BullFightUtil.isEightOX(game_type_index)) { // 八人牛牛玩法
			table = new EIGHTOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_OX_YY) {
			table = new YYOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_SG_JD || game_type_index == GameConstants.GAME_TYPE_SG_BJH
				|| game_type_index == GameConstants.GAME_TYPE_SG_SW) {
			table = new SGTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HJK) {
			table = new HJKTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_BTZ_YY) {
			table = new BTZTable(game_rule_index);
		} else if (game_type_index == GameConstants.GAME_TYPE_TTZ) {
			table = new TTZTable(game_rule_index);
		} else if (game_type_index == GameConstants.GAME_TYPE_PDK_FP || game_type_index == GameConstants.GAME_TYPE_PDK_JD
				|| game_type_index == GameConstants.GAME_TYPE_PDK_LZ || game_type_index == GameConstants.GAME_TYPE_PDK_SW
				|| game_type_index == GameConstants.GAME_TYPE_PDK_JD_LL || game_type_index == GameConstants.GAME_TYPE_PDK_SW_LL) {
			if (game_type_index == GameConstants.GAME_TYPE_PDK_JD) {
				table = new PDK_JD_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_SW) {
				table = new PDK_FIFTEEN_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_LZ) {
				table = new PDK_LZ_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_FP) {
				table = new PDK_SR_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_JD_LL) {
				table = new LL_PDK_JD_Table();
			} else if (game_type_index == GameConstants.GAME_TYPE_PDK_SW_LL) {
				table = new LL_PDK_FIFTEEN_Table();
			} else {
				table = new PDKTable();
			}
		} else if (game_type_index == GameConstants.GAME_TYPE_DDZ_JD) {
			table = new DDZ_JD_Table();
		} else if (game_type_index == GameConstants.GAME_TYPE_DDZ_HENAN) {
			table = new DDZ_HENAN_Table();
		} else if (game_type_index == GameConstants.GAME_TYPE_XYKL) {
			table = new XYKLTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_QJQF) {
			table = new QJQFTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_QF_YY) {
			table = new YYQFTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_BTZ_TB || (GameConstants.GAME_TYPE_BTZ_TB_BEGIN <= game_type_index
				&& GameConstants.GAME_TYPE_BTZ_TB_END >= game_type_index)) { // 推饼(跟扳砣子一模一样)
			table = new TBTable(game_rule_index);
		} else if (game_type_index == GameConstants.GAME_TYPE_DZD) { // 打炸弹
			table = new DZDTable(game_rule_index);
		} else if (game_type_index == GameConstants.GAME_TYPE_JDB || game_type_index == GameConstants.GAME_TYPE_DZH) {
			table = new JDBTable();

		} else if (game_type_index == GameConstants.GAME_TYPE_SHI_DIAN_BAN) {
			table = new SDBTable();

		} else if (game_type_index == GameConstants.GAME_TYPE_GZP) {
			table = new GZPTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_CP_DZHUI) {
			table = new SCHCPTable();
		} else if (BullFightUtil.isUniversalBullFight(game_type_index)) {
			table = new BullFightTable();
		} else if (BullFightUtil.isTypeFengKuang(game_type_index)) {
			table = new CreazyBullFightTable();
		} else if (BullFightUtil.isTypeDouBan(game_type_index)) {
			table = new DouBanNiuTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_CZBG) {
			table = new CZBGTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HONG_ER_SHI) {
			table = new HongErShiTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_PK_HES_PJ) {
			table = new HongErShiTable_PJ();
		} else if (game_type_index == GameConstants.GAME_TYPE_PK_HES_HY) {
			table = new HongErShiTable_HY();
		} else if (game_type_index == GameConstants.GAME_TYPE_DN_YI_YANG) {
			table = new YynOxTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HB_DYZP) {
			table = new HBPHZTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_HUAIHUA_OX) {
			table = new HUAIHUAOXTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_QIONG_LAI_HONG || game_type_index == GameConstants.GAME_TYPE_CP_DSS_QL
				|| game_type_index == GameConstants.GAME_TYPE_CP_DSS_DY) {
			table = new SCHCPDSSTable();
		} else if (game_type_index == GameConstants.GAME_TYPE_CP_DZHUA) {
			table = new SCHCPDZTable();
		} else {
			table = new MJTable();
		}

		return table;
	}

	private static boolean handler_player_enter_room(LogicRoomRequest r, int room_id, Session session) {
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();
		RoomRequest room_rq = r.getRoomRequest();

		// int room_id = r.getRoomId();
		AbstractRoom table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			return false;
		}

		// 刷新时间
		table.process_flush_time();

		Player player = null;
		// 初始化player

		// 观战玩家对象不加入公共缓存
		if (r.getType() == BE_OBSERVER || BE_GOD_VIEW_OBSERVER == r.getType()) {// 并且在移除房间对象的时候就不需要处理观战者的player缓存了，因为不共享，删除了会导致正在游戏的player玩家缓存失效
			player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, 0, session);
			player.setRoom_id(room_id);
		} else {
			player = PlayerServiceImpl.getInstance().createPlayer(logicRoomAccountItemRequest, room_id, session);
		}

		// 防止createPlayer对象的roomId没有被正确赋值
		if (player.getAccount_id() == table.getCreate_player().getAccount_id()) {
			table.getCreate_player().setRoom_id(room_id);
		}
		// 重入判断
		if (table.get_player(logicRoomAccountItemRequest.getAccountId()) != null) {
			send_error_notify(player, 2, "您已经加入房间");// 这里不能把roomId至为0
			return false;
		}

		boolean flag = false;
		int game_type_index = table._game_type_index;
		if (r.getType() == BE_OBSERVER && !(game_type_index == 9009 || game_type_index == 9008 || game_type_index == 9071) && !BullFightUtil
				.isTypeDouBan(game_type_index) && !BullFightUtil.isTypeFengKuang(game_type_index)) {
			flag = table.handler_enter_room_observer(player);
		} else if (r.getType() == BE_GOD_VIEW_OBSERVER) {
			flag = table.handler_god_view_observer_enter(player);
		} else {
			flag = table.handler_enter_room(player);
			if (flag) {
				// 俱乐部房间,通知俱乐部服.在此处处理,不需要在每个子游戏的单独处理
				if (table.club_id > 0) {
					player.setClubJoinId(table.clubInfo.index << 16 & 0xffff0000);
					ClubMsgSender.playerStatusUpdate(ERoomStatus.PLAYER_ENTER, table, player);
				}
			} else {
				logger.warn("handler_enter_room error!,roomid:{},playerid:{}", table.getRoom_id(), player.getAccount_id());
			}
		}

		return flag;
	}

	private boolean handler_player_reconnect_room(LogicRoomRequest r) {
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getLogicRoomAccountItemRequest();

		int room_id = r.getRoomId();
		AbstractRoom table = PlayerServiceImpl.getInstance().getRoomMap().get(room_id);

		if (table == null) {
			logger.error("本地房间刚好不存在了", room_id + "accountID" + r.getAccountId());
			return false;
		}

		// 刷新时间
		table.process_flush_time();

		long accountId = logicRoomAccountItemRequest.getAccountId();
		Player player = table.get_player(accountId);
		if ((player == null) && (player = table.observers().getPlayer(accountId)) == null) {
			logger.error("本地房间刚好不存在了", room_id + "accountID" + r.getAccountId() + "accountId=" + accountId);
			return false;
		}
		// 重定位转发服的相关位置
		player.setChannel(session.getChannel());
		player.setProxy_index(logicRoomAccountItemRequest.getProxyIndex());
		player.setProxy_session_id(logicRoomAccountItemRequest.getProxySessionId());
		player.setAccount_ip(logicRoomAccountItemRequest.getAccountIp());
		player.setOnline(true);
		if (player.getRoom_id() != room_id) {
			logger.error("玩家对象roomId不一致" + room_id + "player.getRoom_id()=" + player.toString() + "accountID" + player.getAccount_id());
		}
		player.setRoom_id(room_id);
		player.setMatchConnectStatus(MatchType.C_STATUS_START);

		return table.handler_reconnect_room(player);

	}

	public static boolean send_error_notify(Player player, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;

	}

	public static boolean handler_create_club_match_room(LogicRoomRequest r) {
		LogicRoomAccountItemRequest logicRoomAccountItemRequest = r.getClubOwnerAccount();
		RoomRequest room_rq = r.getRoomRequest();

		int room_id = r.getRoomId();

		int game_type_index = room_rq.getGameTypeIndex();
		int game_rule_index = room_rq.getGameRuleIndex();
		int game_round = room_rq.getGameRound();
		List<Integer> gameRuleindexEx = room_rq.getGameRuleIndexExList();
		// value = 0; 客户端默认不发
		if (room_rq.getClubId() > 0 || room_rq.getIsNewRule()) {
			game_rule_index = Room.getNewRuleIndex(room_rq.getNewRules());
		}

		// 测试牌局
		AbstractRoom table = createRoom(game_type_index, game_rule_index);
		table.enableRobot();
		table.setCreate_type(GameConstants.CREATE_ROOM_CLUB);
		table.setRoom_id(room_id);

		table.setCreate_time(System.currentTimeMillis() / 1000L);
		table.setRoom_owner_account_id(logicRoomAccountItemRequest.getAccountId());
		table.setRoom_owner_name(logicRoomAccountItemRequest.getNickName());

		table.club_id = room_rq.getClubId();
		table.clubInfo.clubId = table.club_id;
		table.clubInfo.ruleId = room_rq.getRuleId();
		table.clubInfo.clubName = room_rq.getClubName();
		table.clubInfo.matchId = r.getClubMatchId();
		// 俱乐部成员一定大于0
		if (room_rq.getClubMemberSize() > 0) {
			table.clubInfo.clubMemberSize = room_rq.getClubMemberSize();
		}

		LogicRoomAccountItemRequest clubOwnerAccount = r.getClubOwnerAccount();
		table.setRoom_owner_name(clubOwnerAccount.getNickName());

		// 初始化player,同时放入缓存
		List<Player> players = new ArrayList<>();
		Session proxy = null;
		for (LogicRoomAccountItemRequest request : r.getClubMatchPlayersList()) {
			proxy = SessionServiceImpl.getInstance().getSession(EServerType.PROXY, request.getProxyIndex());
			// if (proxy == null) {
			// SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM,
			// String.valueOf(room_id).getBytes());
			// return false;
			// }

			if (SystemRoomUtil.getRoomId(request.getAccountId()) <= 0) {
				if (!table.onPlayerEnterUpdateRedis(request.getAccountId())) {
					SpringService.getBean(RedisService.class).hDel(RedisConstant.ROOM, String.valueOf(room_id).getBytes());
					return false;
				}
			}

			ClubMatchPlayer player = PlayerServiceImpl.getInstance().createClubMatchPlayer(request, room_id, proxy);
			players.add(player);
		}

		// 新的玩法数据结构
		boolean initResult = true;
		if (table.club_id > 0 || room_rq.getIsNewRule()) {
			table.initNewRule(room_rq.getNewRules());
			initResult = table.init_table(game_type_index, table._game_rule_index, game_round);
		} else {

			if (gameRuleindexEx != null && gameRuleindexEx.size() > 0) {
				table.setGameRuleIndexEx(Ints.toArray(gameRuleindexEx));
			}
			table.getGameRuleIndexEx()[0] = game_rule_index;

			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_CI, room_rq.getBaseScoreCi());
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE_GANG, room_rq.getBaseScoreGan());
			table.putRule(GameConstants.GAME_RULE_BASE_SCORE, room_rq.getBaseScore());
			table.putRule(GameConstants.GAME_RULE_MAX_TIMES, room_rq.getMaxTimes());

			initResult = table.init_table(game_type_index, game_rule_index, game_round);
		}
		if (!initResult) {
			logger.error("table.init_table false,roomid:{},type:{},clubId:{},matchId:{}", room_id, GameConstants.CREATE_ROOM_CLUB,
					room_rq.getClubId(), r.getClubMatchId());
			return false;
		}

		PlayerServiceImpl.getInstance().getRoomMap().put(room_id, table);

		//是否防做弊场
		SysParamModel paramModel = SysParamServerDict.getInstance().getSysParam(EGameType.DT.getId(), 2247);
		if (null != paramModel && paramModel.getVal1() == 1) {
			table.setFraud(true);
			logger.error("room:{} is fraud!!", room_id);
		}

		boolean ret = table
				.handler_create_club_match_room(players, GameConstants.CREATE_ROOM_CLUB, RoomComonUtil.getMaxNumber(table.getDescParams()));
		if (!ret) {
			logger.error("table.handler_create_club_match_room error,roomid:{},type:{},clubId:{},matchId:{}", room_id, GameConstants.CREATE_ROOM_CLUB,
					room_rq.getClubId(), r.getClubMatchId());
		}
		if (table.club_id > 0 && r.hasClubOwnerAccount()) {
			Player clubOwner = PlayerServiceImpl.getInstance().getClubCreatePlayer(logicRoomAccountItemRequest, proxy);
			table.setCreate_player(clubOwner);
		}

		return ret;
	}
}
