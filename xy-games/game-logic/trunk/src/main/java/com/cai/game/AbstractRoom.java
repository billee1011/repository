package com.cai.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;

import com.cai.ai.RobotPlayer;
import com.cai.coin.CoinPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RMICmd;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.S2SCmd;
import com.cai.common.define.ECardCategory;
import com.cai.common.define.EGameType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMsgIdType;
import com.cai.common.define.ERoomStatus;
import com.cai.common.define.ESysLogLevelType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.define.ETriggerType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.LogicRoomInfo;
import com.cai.common.domain.Player;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.json.MatchBaseScoreJsonModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.rmi.vo.ClubExclusiveRMIVo;
import com.cai.common.type.PlayerRoomStatus;
import com.cai.common.util.DescParams;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.RuntimeOpt;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.core.SystemConfig;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.GameGroupRuleDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AutoReadyRunnable;
import com.cai.future.runnable.CommonRunnable;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.match.MatchPlayer;
import com.cai.redis.service.RedisService;
import com.cai.service.MatchTableService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.SessionServiceImpl;
import com.cai.util.ClubMsgSender;
import com.cai.util.MatchRoomUtils;
import com.cai.util.RedisRoomUtil;
import com.cai.util.SystemRoomUtil;
import com.cai.util.TestCardUti;
import com.google.common.collect.Maps;
import com.xianyi.framework.core.concurrent.disruptor.TaskDispatcher;
import com.xianyi.framework.core.concurrent.selfDriver.AutoDriverQueue;

import protobuf.clazz.Common.ChatMsgReq;
import protobuf.clazz.Common.ChatMsgRsp;
import protobuf.clazz.Common.CommonILI;
import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.s2s.S2SProto.ProxyRoomUpdateProto;
import protobuf.clazz.s2s.S2SProto.S2STransmitProto;

public abstract class AbstractRoom extends Room {

	private static final String GAME_START_FLAG = "game_start_flag";

	private static final String GAME_END_FLAG = "game_end_flag";

	/**
	 */
	private static final long serialVersionUID = 1L;

	public GameRoomRecord _gameRoomRecord;
	public BrandLogModel _recordRoomRecord;

	protected int _player_open_less[]; // 允许三人场

	public int _game_status;

	public boolean istrustee[]; // 托管状态

	/**
	 * 这个值 不能在这里改--这个参数测试用 通过后台改牌 改单个桌子的
	 */
	public boolean BACK_DEBUG_CARDS_MODE = false;

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;
	/**
	 * 后台操作临时数组
	 */
	public int debug_my_cards[];

	public float game_cell; // 游戏底分
	public int _user_times[];// 用户倍数
	public int _cur_banker = GameConstants.INVALID_SEAT; // 当前庄

	public int continue_banker_count = 1; // 连庄次数
	public int _all_card_len = 0;
	public int _repertory_card[];

	protected int playerNumber;

	public boolean _player_status[]; // 用户状态

	// ai用来区分操作回合
	public int[] aiFlag = new int[16];

	public Player _apply_seat_players[];// 申请位置的用户
	public Player _bu_score_players[]; // 补分用户申请

	// 玩家初始位置
	private Map<Long, Integer> accountid_to_idnex;
	public int matchId; // 比赛场配置ID
	public int id; // 比赛场次数唯一ID

	public MatchBaseScoreJsonModel matchBase;

	// 这个房间需要扣的豆，根据配置算出
	public int config_cost_dou;

	// 最小开桌人数 [默认两人]
	private int minPlayerCount = 2;

	// 自动准备调度器id
	private int autoReadyTimerId;

	public RoomRedisModel roomRedisModel = null;

	private final Map<Integer, ScheduledFuture<?>> scheduleMap = Maps.newConcurrentMap();

	// 调度器id
	private final static AtomicInteger AUTO_SCHEDULE_ID = new AtomicInteger(0);

	// 房间内任务执行器
	private static final Executor excutor = TaskDispatcher.newDispatcher(RuntimeOpt.availableProcessors() << 2,
			"room-task-dispatcher", 262144, RuntimeOpt.availableProcessors());

	// private static final Executor excutor = Executors.newFixedThreadPool(10);
	// 房间内任务队列
	private final AutoDriverQueue taskQueue = AutoDriverQueue.newQueue(excutor);

	/**
	 * 是否防作弊场
	 */
	protected boolean isFraud = false;

	// private static final Runnable EMPTY_TASK = () -> {
	// };

	public AbstractRoom(RoomType roomType, int maxNumber) {
		super(roomType, maxNumber);
		// WalkerGeek 初始化最少开局人数

		_player_ready = new int[maxNumber];
		_player_open_less = new int[maxNumber];
		for (int i = 0; i < maxNumber; i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;

		}

		_player_status = new boolean[maxNumber];
		game_cell = 1;
		_apply_seat_players = new Player[maxNumber * maxNumber];
		_bu_score_players = new Player[maxNumber * maxNumber];
		_user_times = new int[maxNumber];
		for (int i = 0; i < maxNumber; i++) {
			_player_status[i] = false;// 默认为true 能中途进的游戏才有false的可能
			_user_times[i] = 1;
		}
	}

	public Player[] get_bu_score_players() {
		return _bu_score_players;
	}

	public int get_bu_score_count() {
		int count = 0;
		for (Player player : _bu_score_players) {
			if (player == null)
				continue;
			count++;
		}
		return count;

	}

	public boolean isUseAi(int seat_index, int aiIndex) {
		return aiFlag[seat_index] == aiIndex;
	}

	/**
	 * 会否托管
	 */
	public boolean checkTrutess(int seat_index) {
		if (istrustee == null) {
			return false;
		}
		int lenght = istrustee.length;
		if (seat_index < 0 || seat_index >= lenght) {
			return false;
		}
		return istrustee[seat_index];
	}

	/**
	 * 获取倍数
	 */
	public int getTimes(int seat_index) {
		int times = 1;
		if (matchBase != null) {
			times = matchBase.getTimes();
			if (_user_times != null) {
				if (seat_index < _user_times.length) {
					int tempTimes = _user_times[seat_index];
					if (tempTimes > times) {
						times = tempTimes;
					}
				}
			}
		}
		return times;
	}

	/**
	 * 获取结算系数
	 */
	public int getSettleBase(int seat_index) {
		int base = 1;
		int baseScore = 1;
		int times = getTimes(seat_index);
		if (matchBase != null) {
			base = matchBase.getBase();
			baseScore = matchBase.getBaseScore();
		}
		int value = base * baseScore * times;
		return value;
	}

	/**
	 * 获取金币场结算系数
	 */
	public int getSettleBaseCoin(int seat_index) {
		int baseScore = 1;
		if (matchBase != null) {
			baseScore = matchBase.getBaseScore();
		}
		return baseScore;
	}

	/**
	 * 获取用户刺激场的倍数，用于计算输赢金币
	 *
	 * @param account_id
	 * @return
	 */
	public int getCoinExciteMultiple(long account_id) {
		Player player = getPlayer(account_id);
		if (null != player && player instanceof RobotPlayer) {
			return ((RobotPlayer) player).getExciteMultiple();
		}
		return 1;
	}

	public final Player get_bu_score_player(long account_id) {

		for (Player player : get_bu_score_players()) {
			if (player != null) {
				if (player.getAccount_id() == account_id) {
					return player;
				}
			}
		}
		return null;
	}

	public final boolean delete_bu_score_player(long account_id) {
		for (Player player : get_bu_score_players()) {
			if (player == null)
				continue;
			if (player.getAccount_id() == account_id) {
				player = null;
			}
		}

		return true;
	}

	public Player[] get_apply_players() {
		return _apply_seat_players;
	}

	public int get_apply_players_count() {
		int count = 0;
		for (Player player : _apply_seat_players) {
			if (player == null)
				continue;
			count++;
		}
		return count;

	}

	public final Player get_apply_player(long account_id) {

		for (Player player : get_apply_players()) {
			if (player != null) {
				if (player.getAccount_id() == account_id) {
					return player;
				}
			}
		}

		return null;
	}

	public final boolean delete_apply_player(long account_id) {

		for (Player player : get_apply_players()) {
			if (player == null)
				continue;
			if (player.getAccount_id() == account_id) {
				player = null;
			}
		}
		return true;
	}

	public Player[] get_apply_index_players(int seat_index, int maxcount) {
		Player[] apply_seat_player = new Player[maxcount];

		int count = 0;
		for (Player player : _apply_seat_players) {
			if (player == null)
				continue;
			if (player.get_apply_index() != seat_index)
				continue;
			apply_seat_player[count++] = player;
		}
		return apply_seat_player;

	}

	public void delete_apply_index_players(int seat_index) {
		int count = this.get_apply_index_players_count(seat_index);
		if (count == 0)
			return;
		Player[] apply_seat_palyer = new Player[count];
		apply_seat_palyer = get_apply_index_players(seat_index, count);
		for (int i = 0; i < count; i++) {
			if (apply_seat_palyer[i] == null)
				continue;
			if (apply_seat_palyer[i].get_apply_index() != seat_index)
				continue;
			delete_apply_player(apply_seat_palyer[i].getAccount_id());
		}
		return;
	}

	public int get_apply_index_players_count(int seat_index) {

		int count = 0;
		for (Player player : _apply_seat_players) {
			if (player == null)
				continue;
			if (player.get_apply_index() != seat_index)
				continue;
			count++;
		}
		return count;
	}

	public int get_apply_seat_count(int seat_index) {
		int count = 0;
		return count;
	}

	/**
	 * @param _seat_index
	 * @param _reason
	 */
	@Override
	public final boolean handler_game_finish(int _seat_index, int _reason) {
		log_warn(GAME_END_FLAG);// 游戏结束
		List<Player> trutessList = new ArrayList<>();

		for (Player player : get_players()) {
			if (player != null) {
				player.setRound(player.getRound() + 1);
				boolean isTrutess = checkTrutess(player.get_seat_index());
				player.setIsTrusteeOver(isTrutess);
				if (isTrutess) {
					trutessList.add(player);
				}
			}
		}
		// long now = System.currentTimeMillis();
		// for (Player player : get_players()) {
		// if (player != null) {
		// if (now - player.getLocation_time() > 1000 * 60 * 30) {// 大于30分钟清理掉
		// player.locationInfor = null;
		// }
		// }
		// }

		boolean onFinish = on_room_game_finish(_seat_index, _reason);

		if (clubInfo.matchId > 0) {
			try {
				if (_cur_round < _game_round) {

					cancelShedule(autoReadyTimerId);

					SheduleArgs arg = SheduleArgs.newArgs();
					schedule(arg, 5 * 1000L);
					autoReadyTimerId = arg.getTimerId();
				}
				// 小局分数统计
				ClubMsgSender.gameRoundSnapshotNotify(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		MatchRoomUtils.onRoomFinish(this);
		SystemRoomUtil.onRoomFinish(this, trutessList);

		return onFinish;
	}

	/**
	 * 游戏结束，清理玩家---这里只清理牌桌内的玩家
	 */
	public void cleanPlayers() {
		for (Player player : get_players()) {
			if (player == null)
				continue;
			if (player.getRoom_id() == getRoom_id() || player.getRoom_id() == 0) {
				if (!is_match() && !isClubMatch()) {
					PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
				}
			} else {
				MongoDBServiceImpl.getInstance().server_error_log(getRoom_id(), ELogType.roomIdError,
						"房间结束清理的时候，房间号对不上", player.getAccount_id(), player.toString(), getGame_id());
			}
		}
		observers().clear();
	}

	public abstract boolean on_room_game_finish(int _seat_index, int _reason);

	@Override
	public final boolean handler_game_start() {
		log_warn(GAME_START_FLAG);// 记录房间局数开始时间

		isStart = true;

		// 玩家随机位置
		if (_cur_round == 0 && getRuleValue(GameConstants.GAME_RULE_RANDOM_SEAT) == 1) {
			shuffle_players();
		}

		if (_cur_round == 0 && _game_type_index == GameConstants.GAME_TYPE_PHZ_YONG_ZHOU) {
			shuffle_players();
		}

		List<Player> allPlayers = getAllPlayers();
		allPlayers.forEach(p -> {
			p.setCurRoom(this);
		});
		boolean result = on_handler_game_start();
		GRR.setRoom(this);
		ClubMsgSender.roomStatusUpdate(ERoomStatus.START, this);

		// 推送房间数据[GAME-TODO]
		if (_cur_round == 1) {
			ClubMsgSender.syncAppointClubRoomStatusTOClubServer(this.getRoom_id(), clubInfo.clubId);
		}

		MatchRoomUtils.onRoundStart(this);

		if (_game_type_index == EGameType.DTPH.getId()) {// 大厅碰胡才有这个需求
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

		return result;
	}

	private final void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < this.get_players().length; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		// 重新排下位置
		for (int i = 0; i < this.get_players().length; i++) {
			if (i < pl.size()) {
				get_players()[i] = pl.get(i);
				get_players()[i].set_seat_index(i);
				if (getCreate_player() != null
						&& get_players()[i].getAccount_id() == getCreate_player().getAccount_id()) {
					// _cur_banker = i;
					getCreate_player().set_seat_index(i);
				}
			} else {
				get_players()[i] = null;
			}
		}

		if (_cur_round == 0 && getRuleValue(GameConstants.GAME_RULE_RANDOM_SEAT) == 1) {
			int rand = (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
			this._cur_banker = rand % this.getTablePlayerNumber();
		}

		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(_game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);
		this.send_response_to_room(roomResponse2);

		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this);
	}

	public void initRoomRule(int uniqId, int matchId, DescParams params) {
		this.id = uniqId;
		this.matchId = matchId;
		this._game_rule_index = params._game_rule_index;
		this.gameRuleIndexEx = params.game_rules;
		params.getMap().forEach((key, value) -> {
			this.ruleMap.put(key, value);
		});
	}

	/**
	 * 设置出牌时间 单位:秒 默认15秒
	 */
	public void setPlayOutTime(int outCardTime) {
		if (outCardTime > 0) {
			this.setPlay_card_time(outCardTime);
		}
	}

	/**
	 * 创建房间
	 *
	 * @param matchId
	 * @param id
	 * @return
	 */
	public boolean handler_create_room(List<MatchPlayer> players, int type, int maxNumber, int id, int matchId,
			MatchBaseScoreJsonModel matchBase) {
		this.matchBase = matchBase;
		initRobotRule(this.ruleMap);
		this.setCreate_type(type);
		this.setCreate_player(players.get(0));
		this.id = id;
		this.matchId = matchId;
		this.isStart = true;

		roomRedisModel = new RoomRedisModel();
		roomRedisModel.setGameRuleDes(this.get_game_des());
		roomRedisModel.setRoomStatus(this._game_status);
		roomRedisModel.setPlayer_max(maxNumber);
		roomRedisModel.setGame_round(this._game_round);
		roomRedisModel.setStart(true);
		roomRedisModel.setCreate_time(create_time * 1000);
		roomRedisModel.setGame_rule_index(_game_rule_index);
		roomRedisModel.setLogic_index(SystemConfig.logic_index);
		roomRedisModel.setCreateType(GameConstants.CREATE_ROOM_MATCH);
		roomRedisModel.setGame_type_index(_game_type_index);
		roomRedisModel.setGame_id(getGame_id());
		// 写入redis
		RedisService redisService = SpringService.getBean(RedisService.class);
		// redisService.hSet(RedisConstant.ROOM, getRoom_id() + "",
		// roomRedisModel);
		for (int i = 0; i < players.size(); i++) {
			get_players()[i] = players.get(i);
			players.get(i).set_seat_index(i);
			players.get(i).setRoom_id(this.getRoom_id());
			roomRedisModel.getPlayersIdSet().add(players.get(i).getAccount_id());
			roomRedisModel.getNames().add(players.get(i).getNick_name());
			roomRedisModel.setCur_player_num(players.size());

			_player_result.game_score[i] = players.get(i).getCurScore();

			if (!players.get(i).isRobot() && !players.get(i).isLeave() && players.get(i).isEnter()) {
				onPlayerEnterUpdateRedis(players.get(i).getAccount_id());
			}

		}

		redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		send_response_to_room(roomResponse);
		handler_game_start();

		for (int i = 0; i < maxNumber; i++) {
			// _player_ready[i] = 1;
		}

		MatchPlayer matchPlayer = null;
		for (int i = 0; i < players.size(); i++) {
			matchPlayer = players.get(i);
			handler_request_trustee(i, matchPlayer.isIsTrusteeOver(), 0);
		}

		return true;
	}

	/**
	 * 创建金币场房间
	 */
	public boolean handler_create_coin_room(List<CoinPlayer> players, int type, int maxNumber,
			MatchBaseScoreJsonModel matchBase) {
		this.matchBase = matchBase;
		initRobotRule(this.ruleMap);
		this.setCreate_type(type);
		this.setCreate_player(players.get(0));
		this.isStart = true;

		// 金币产配牌----- 很冒险！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
		TestCardUti.DebugEntry entry = TestCardUti.cardArray(_game_type_index);
		if (null != entry) {
			BACK_DEBUG_CARDS_MODE = true;
			debug_my_cards = entry.getArrays();
		}

		roomRedisModel = new RoomRedisModel();
		roomRedisModel.setGameRuleDes(this.get_game_des());
		roomRedisModel.setRoomStatus(this._game_status);
		roomRedisModel.setPlayer_max(maxNumber);
		roomRedisModel.setGame_round(this._game_round);
		roomRedisModel.setStart(true);
		roomRedisModel.setCreate_time(create_time * 1000);
		roomRedisModel.setGame_rule_index(_game_rule_index);
		roomRedisModel.setLogic_index(SystemConfig.logic_index);
		roomRedisModel.setCreateType(type);
		roomRedisModel.setGame_type_index(_game_type_index);
		roomRedisModel.setGame_id(getGame_id());
		// 写入redis
		RedisService redisService = SpringService.getBean(RedisService.class);
		// redisService.hSet(RedisConstant.ROOM, getRoom_id() + "",
		// roomRedisModel);
		CoinPlayer coinPlayer = null;
		for (int i = 0; i < players.size(); i++) {
			coinPlayer = players.get(i);
			get_players()[i] = coinPlayer;
			coinPlayer.set_seat_index(i);
			coinPlayer.setRoom_id(this.getRoom_id());
			roomRedisModel.getPlayersIdSet().add(coinPlayer.getAccount_id());
			roomRedisModel.getNames().add(coinPlayer.getNick_name());
			roomRedisModel.setCur_player_num(players.size());

			if (!coinPlayer.isRobot()) {
				onPlayerEnterUpdateRedis(coinPlayer.getAccount_id());
			}

		}

		redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		send_response_to_room(roomResponse);
		handler_game_start();

		return true;
	}

	/**
	 * 创建俱乐部比赛房间
	 */
	public boolean handler_create_club_match_room(List<Player> players, int type, int maxNumber) {
		this.setCreate_type(type);
		this.isStart = true;

		roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "",
				RoomRedisModel.class);
		roomRedisModel.setGameRuleDes(this.get_game_des());
		roomRedisModel.setRoomStatus(this._game_status);
		roomRedisModel.setPlayer_max(maxNumber);
		roomRedisModel.setGame_round(this._game_round);
		roomRedisModel.setStart(true);
		roomRedisModel.setCreate_time(create_time * 1000);
		roomRedisModel.setGame_rule_index(_game_rule_index);
		roomRedisModel.setLogic_index(SystemConfig.logic_index);
		roomRedisModel.setCreateType(type);
		roomRedisModel.setGame_type_index(_game_type_index);
		roomRedisModel.setGame_id(getGame_id());
		// 写入redis
		RedisService redisService = SpringService.getBean(RedisService.class);
		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			get_players()[i] = player;
			player.set_seat_index(i);
			player.setRoom_id(this.getRoom_id());

			try {
				_player_ready[i] = 1;
			} catch (Exception e) {
				e.printStackTrace();
			}

			roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
			roomRedisModel.getNames().add(player.getNick_name());
			roomRedisModel.setCur_player_num(players.size());
		}

		redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		// send_response_to_room(roomResponse);
		send_club_match_response_to_room(roomResponse, false);
		handler_game_start();

		return true;
	}

	private boolean send_club_match_response_to_room(RoomResponse.Builder roomResponse, boolean exceptObserver) {
		try {
			roomResponse.setRoomId(super.getRoom_id());// 日志用的
			roomResponse.setAppId(getGame_id());

			Player player = null;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				int roomId = SystemRoomUtil.getRoomId(player.getAccount_id());
				if (roomId > 0 && roomId != super.getRoom_id()) { // 玩家已经在其他房间里了
					continue;
				}
				PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
			}

			if (!exceptObserver) {
				observers().sendAll(roomResponse);
			}
		} catch (Exception e) {
			logger.error("send_club_match_response_to_room error", e);
		}

		return true;
	}

	/**
	 * 创建房间
	 *
	 * @return
	 */
	@Override
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		this.setCreate_type(type);
		this.setCreate_player(player);
		roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "",
				RoomRedisModel.class);
		// 代理开房
		if (type == GameConstants.CREATE_ROOM_PROXY) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
					TimeUnit.MINUTES);

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);

			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.setGame_round(this._game_round);
			roomRedisModel.setCreate_account_id(player.getAccount_id());
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			// 发送进入房间
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_CREATE_RROXY_ROOM_SUCCESS);
			load_room_info_data(roomResponse);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			PlayerServiceImpl.getInstance().send(player, responseBuilder.build());
			return true;
		}

		// 机器人开房
		if (type == GameConstants.CREATE_ROOM_ROBOT) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
					TimeUnit.MINUTES);

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(RoomComonUtil.getMaxNumber(this, this.getDescParams()));
			roomRedisModel.setGame_round(this._game_round);
			roomRedisModel.setCreate_account_id(player.getAccount_id());
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			return true;
		}

		if (club_id > 0) {
			RedisService redisService = SpringService.getBean(RedisService.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.setGame_round(this._game_round);
			roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
			roomRedisModel.getNames().add(player.getNick_name());
			// roomRedisModel.setCreate_account_id(player.getAccount_id());
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);
		}
		GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
				TimeUnit.MINUTES);

		// c成功
		get_players()[0] = player;
		player.set_seat_index(0);
		_cur_banker = player.get_seat_index();

		//
		onPlayerEnterUpdateRedis(player.getAccount_id());

		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		roomResponse.setIsGoldRoom(is_sys());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		return send_response_to_player(player.get_seat_index(), roomResponse);
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {

		int limitCount = 20;
		if (SystemConfig.gameDebug == 1) {
			limitCount = 5;
		}
		// 限制围观者数量，未来加到配置表控制
		if (player.getAccount_id() != getRoom_owner_account_id() && observers().count() >= limitCount) {
			this.send_error_notify(player, 1, "当前游戏围观位置已满,下次赶早!");
			return false;
		}
		observers().enter(player);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		observers().send(player, roomResponse);

		return true;
	}

	/**
	 * 是否能进入牌桌 ，默认中途不可进需要修改的table自己继承
	 *
	 * @param player
	 * @return
	 */
	protected boolean canEnter(Player player) {
		return !isStart || is_sys();
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {
		player.setStatus(PlayerRoomStatus.NORMAL);

		if (getRuleValue(GameConstants.GAME_RULE_IP) > 0) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;

				// logger.error("tarplayer
				// ip=="+tarplayer.getAccount_ip()+"player
				// ip=="+player.getAccount_ip());
				if (player.getAccount_id() == tarplayer.getAccount_id())
					continue;
				if ((!is_sys()) && StringUtils.isNotEmpty(tarplayer.getAccount_ip())
						&& StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					player.setRoom_id(0);// 把房间信息清除--
					send_error_notify(player, 1, "不允许相同ip进入");
					return false;
				}
			}
		}

		if (matchId > 0) {
			return false;
		}

		if (!canEnter(player)) {
			player.setRoom_id(0);// 把房间信息清除--
			if (GameConstants.CREATE_ROOM_NEW_COIN == getCreate_type()) {
				send_error_notify(player, 1, "房间不存在！");
			} else {
				send_error_notify(player, 1, "游戏中途不可进");
			}

			return false;
		}

		int seat_index = GameConstants.INVALID_SEAT;

		// if (playerNumber == 0) {// 未开始 才分配位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				get_players()[i] = player;
				seat_index = i;
				break;
			}
		}
		// }

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT
				&& player.get_seat_index() < getTablePlayerNumber()) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer != null && tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			if (GameConstants.CREATE_ROOM_NEW_COIN == getCreate_type()) {
				send_error_notify(player, 1, "房间不存在");
			} else {
				send_error_notify(player, 1, "游戏已经开始");
			}

			return false;
		}

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setIsGoldRoom(is_sys());
		roomResponse.setAppId(this.getGame_id());

		// WalkerGeek 新人加入清空之前少人的确认
		clear_open_less();

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);
		godViewObservers().sendAll(roomResponse);
		// 同步数据

		// ========同步到中心========
		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		roomRedisModel.getNames().add(player.getNick_name());
		int cur_player_num = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				cur_player_num++;
			}
		}
		roomRedisModel.setCur_player_num(cur_player_num);
		roomRedisModel.setGame_round(this._game_round);
		// 写入redis
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		onPlayerEnterUpdateRedis(player.getAccount_id());

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);

		if (player.getAccount_id() == getRoom_owner_account_id()) {
			this.getCreate_player().set_seat_index(player.get_seat_index());
		}

		return true;
	}

	/**
	 * 清空玩家少人模式状态 WalkerGeek 进入房间时候需要清空少人模式状态的游戏拷贝这个方法在子游戏table实现
	 */
	@Override
	public void clear_open_less() {

		// if (ruleMap.get(GameConstants.GAME_RULE_CAN_LESS) != null) {
		// for (int i = 0; i < _player_open_less.length; i++) {
		// _player_open_less[i] = 0;
		// }
		// }

	}

	@Override
	public final boolean refresh_room_redis_data(int type, boolean notifyRedis) {

		SysParamModel sysParamModel2232 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(6)
				.get(3000);
		if (sysParamModel2232 != null && sysParamModel2232.getVal1() > 0) {// 感觉下面的代码没啥意义，加个开关
			if (type == GameConstants.PROXY_ROOM_UPDATE) {
				int cur_player_num = 0;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (this.get_players()[i] != null) {
						cur_player_num++;
					}
				}

				// 写入redis
				if (roomRedisModel != null) {
					RedisService redisService = SpringService.getBean(RedisService.class);
					roomRedisModel.setGameRuleDes(this.get_game_des());
					roomRedisModel.setRoomStatus(this._game_status);
					roomRedisModel.setPlayer_max(RoomComonUtil.getMaxNumber(this, getDescParams()));
					roomRedisModel.setCur_player_num(cur_player_num);
					roomRedisModel.setGame_round(this._game_round);
					roomRedisModel.setCur_round(this._cur_round);
					// roomRedisModel.setCreate_time(System.currentTimeMillis());
					redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

				}

			}

		}

		if (notifyRedis) {
			if (getCreate_type() == GameConstants.CREATE_ROOM_PROXY&&  clubInfo.clubId<=0) {
				SessionServiceImpl.getInstance().sendGate(1, PBUtil
						.toS2SRequet(S2SCmd.S_G_S,
								S2STransmitProto.newBuilder().setAccountId(getRoom_owner_account_id())
										.setRequest(PBUtil.toS2SResponse(S2SCmd.PROXY_ROOM_STATUS,
												ProxyRoomUpdateProto.newBuilder()
														.setAccountId(getRoom_owner_account_id()).setChangeType(type))))
						.build());
			}
		}

		return true;
	}

	// 初始化玩家的房间Id
	public boolean onPlayerEnterUpdateRedis(long accountId) {
		return RedisRoomUtil.joinRoom(accountId, getRoom_id());
	}

	@Override
	public final Player get_player(long account_id) {
		Player player = null;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player != null && player.getAccount_id() == account_id) {
				return player;
			}
		}

		return null;
	}

	/**
	 * 加载房间里的玩家信息
	 *
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * @param rplayer
	 * @return
	 */
	public final RoomPlayerResponse.Builder newPlayerBaseBuilder(Player rplayer) {
		int seatIndex;
		if (null == rplayer || (seatIndex = rplayer.get_seat_index()) < 0 || seatIndex >= get_players().length) {
			return null;
		}

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(rplayer.getAccount_id());
		room_player.setIp(rplayer.getAccount_ip());
		if (isFraud) {
			room_player.setHeadImgUrl("***");
			room_player.setUserName("***");
		} else {
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setUserName(rplayer.getNick_name());
		}

		room_player.setIp(rplayer.getAccount_ip());
		room_player.setSeatIndex(rplayer.get_seat_index());
		room_player.setOnline(rplayer.isOnline() ? 1 : 0);
		room_player.setIpAddr(rplayer.getAccount_ip_addr());
		room_player.setSex(rplayer.getSex());
		room_player.setScore(_player_result.game_score[seatIndex] * rplayer.getMyTimes());
		room_player.setReady(_player_ready[seatIndex]);
		room_player.setPao(_player_result.pao[seatIndex] < 0 ? 0 : _player_result.pao[seatIndex]);
		room_player.setQiang(_player_result.qiang[seatIndex]);
		room_player.setOpenThree(_player_open_less[seatIndex] == 0 ? false : true);
		room_player.setMoney(rplayer.getMoney());
		room_player.setGold(rplayer.getGold());
		room_player.setGvoiceStatus(rplayer.getGvoiceStatus());
		room_player.setStatus(rplayer.getStatus());
		room_player.setHasPiao(_player_result.haspiao[seatIndex]);
		if (rplayer.locationInfor != null) {
			room_player.setLocationInfor(rplayer.locationInfor);
		}
		return room_player;
	}

	/**
	 * 加载房间里的玩家信息--定位
	 *
	 * @param roomResponse
	 */
	public void load_player_info_data_location(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public final void getLocationTip() {
		RoomUtil.getLocationTip(this);
	}

	/***
	 * 强制解散
	 */
	@Override
	public final boolean force_account(final String tips) {
		try {
			int number = getTablePlayerNumber() <= 0 ? GameConstants.GAME_PLAYER : getTablePlayerNumber();
			if (this._cur_round == 0) {

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				Player player = null;

				huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

				for (int i = 0; i < number; i++) {
					if (i >= this.get_players().length) {
						break;
					}
					player = this.get_players()[i];
					if (player != null && !is_match()) {
						send_error_notify(i, ESysMsgType.INCLUDE_ERROR.getId(), tips);
					}
				}

			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_SYSTEM);
				Player player = null;
				for (int i = 0; i < number; i++) {
					if (i >= this.get_players().length) {
						break;
					}
					player = this.get_players()[i];
					if (player != null && !is_match() && !isClubMatch()) {
						send_error_notify(i, ESysMsgType.NONE.getId(), tips);
					}
				}
			}
		} catch (Exception e) {
			logger.error("解散房间报错" + this.getRoom_id(), e);
			// 删除房间--游戏 逻辑报错
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		return false;
	}

	protected final boolean huan_dou(int result) {
		if (cost_dou == 0)
			return false;

		boolean huan = false;
		if (result == GameConstants.Game_End_NORMAL || result == GameConstants.Game_End_DRAW
				|| result == GameConstants.Game_End_ROUND_OVER) {
			return false;
		} else if (result == GameConstants.Game_End_RELEASE_NO_BEGIN) {
			// 还没开始
			huan = true;
		} else if (result == GameConstants.Game_End_RELEASE_RESULT || result == GameConstants.Game_End_RELEASE_PLAY
				|| result == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| result == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			// 开始了 没打完
			if ((this._cur_round <= 1) && (GRR != null)) {
				huan = true;
			}
		} else if (result == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (this._cur_round <= 1) {
				huan = true;
			}
		} else {
			return false;
		}

		if (this.clubInfo.matchId > 0) {
			huan = false;
		}

		if (huan) {

			StringBuilder buf = new StringBuilder();
			if (clubInfo.clubId > 0) {
				buf.append("开局失败" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id())
						.append(",game_type_index:" + _game_type_index).append(",game_round:" + _game_round)
						.append(",房主:" + this.getRoom_owner_account_id())
						.append(",豆+:" + cost_dou + ",亲友圈:" + clubInfo.clubId);
			} else {
				buf.append("开局失败" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id())
						.append(",game_type_index:" + _game_type_index).append(",game_round:" + _game_round)
						.append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
			}

			// 如果是扣了专属豆，还专属豆
			if (/* clubInfo.clubId > 0 && */ clubInfo.exclusive) {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);

				ClubExclusiveRMIVo vo = ClubExclusiveRMIVo
						.newVo(getRoom_owner_account_id(), getGame_id(), cost_dou, EGoldOperateType.FAILED_ROOM)
						.setGameTypeIndex(getGameTypeIndex()).setClubId(clubInfo.clubId);

				vo.setDesc(buf.toString());
				AddGoldResultModel addresult = centerRMIServer.rmiInvoke(RMICmd.CLUB_EXCLUSIVE_REPAY, vo);
				if (null != addresult && addresult.isSuccess()) {
					Object attament = addresult.getAttament();
					if (null != attament && attament instanceof CommonILI) {
						ClubMsgSender.sendExclusiveGoldUpdate(getRoom_owner_account_id(),
								Arrays.asList((CommonILI) attament));
					}
				}
			} else {
				// 把豆还给玩家
				AddGoldResultModel addresult = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
						cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
				if (addresult.isSuccess() == false) {
					logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
				}
			}
			cost_dou = 0;
		}

		return true;
	}

	/*
	 */
	@Override
	public final boolean handler_requst_open_less(Player player, boolean openThree) {
		if (GameConstants.GS_MJ_FREE != _game_status) {
			return false;
		}

		// 已经开局 返回
		if (this._cur_round != 0) {
			return false;
		}

		if (_player_ready[player.get_seat_index()] == 0) {
			return false;
		}

		int less = openThree ? 1 : 0;
		_player_open_less[player.get_seat_index()] = less;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(i, roomResponse2);
		}

		int count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				count++;
			}
		}
		// 牌桌不是3人
		if (count != (getTablePlayerNumber() - 1)) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}

		if (openLess == readys && readys == getTablePlayerNumber() - 1) {
			playerNumber = readys;
			handler_game_start();

			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

		return false;
	}

	public boolean checkDedu() {
		return false;
	}

	/**
	 * 允许少人模式扩展
	 */
	@Override
	public boolean handler_requst_open_less(Player player, int playerNum) {
		if (GameConstants.GS_MJ_FREE != _game_status) {
			return false;
		}

		// 不允许一个人开启游戏，兼容客户端bug
		if (playerNum == 1) {
			return false;
		}

		// 判断规则是否存在
		if (getRuleValue(GameConstants.GAME_RULE_CAN_LESS) == 0) {
			return false;
		}
		// 已经开局 返回
		if (this._cur_round != 0) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}
		// 变更少人模式数组
		int less = playerNum < getTablePlayerNumber() ? 1 : 0;
		if ((openLess + 1) == playerNum) {

			this.changePlayer();
			for (int j = 0; j < this.get_players().length; j++) {
				if (this.get_players()[j] != null) {
					_player_open_less[j] = less;
				} else {
					_player_open_less[j] = 0;
				}
			}

			playerNumber = playerNum;
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		} else if (playerNum == GameConstants.INVALID_SEAT) {// 取消勾选少人模式
			less = 0;
			playerNumber = playerNum;
			_player_open_less[player.get_seat_index()] = less;
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		} else {
			_player_open_less[player.get_seat_index()] = less;
		}
		// 通知客户端
		this.refresh_less_player();

		if ((openLess + 1) == readys && readys == playerNum) {
			this.changePlayer();
			playerNumber = playerNum;
			this.refresh_less_player();
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());

			handler_game_start();
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

		return false;
	}

	/**
	 * 重新分配座位
	 *
	 * @return
	 */
	@Override
	public boolean changePlayer() {
		Player[] players = new Player[this.get_players().length];
		// 变更准备状态
		int[] players_ready = new int[_player_ready.length];
		int count = 0;
		for (int i = 0; i < this.get_players().length; i++) {
			Player player = this.get_players()[i];
			if (player == null) {
				continue;
			}
			player.set_seat_index(count);
			players[count] = player;
			// 准备状态
			players_ready[count] = _player_ready[i];

			count++;
		}

		_player_ready = players_ready;
		this.set_players(players);
		// 后面抽出来再放到抽象层 -GAME-TODO
		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this);
		return true;
	}

	/**
	 * 刷新玩家少人模式信息
	 *
	 * @return
	 */
	public boolean refresh_less_player() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(i, roomResponse2);
		}
		return true;
	}

	/**
	 * WalkerGeek 玩家离开房间初始化少人模式参数 同时更新redis缓存
	 *
	 * @return
	 */
	public boolean init_less_param() {
		for (int i = 0; i < _player_open_less.length; i++) {
			_player_open_less[i] = 0;
		}

		// 减少redis的更新频率
		if (playerNumber <= 0) {
			return true;
		}
		playerNumber = 0;
		// this.changePlayer();
		this.refresh_less_player();
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		return true;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
			if (this.get_players()[get_seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			_player_ready[get_seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(get_seat_index, roomResponse2);
			}
			return false;
		} else {
			return handler_player_ready(get_seat_index, is_cancel);
		}
	}

	/***
	 * 加载房间的玩法 状态信息
	 *
	 * @param roomResponse
	 */
	public final void load_room_info_data(RoomResponse.Builder roomResponse) {
		RoomInfo.Builder room_info = getRoomInfo();
		roomResponse.setRoomId(getRoom_id());
		roomResponse.setAppId(getGame_id());
		roomResponse.setRoomInfo(room_info);
	}

	/**
	 * @return
	 */
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = encodeRoomBase();
		return room_info;
	}

	public final RoomInfo.Builder encodeRoomBase() {
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setMatchId(this.matchId);
		room_info.setMatchIndexId(id);
		room_info.setCreateName(this.getRoom_owner_name());
		room_info.setClubId(club_id);
		room_info.setCreateType(getCreate_type());
		if (clubInfo.clubId > 0) {
			room_info.setRuleId(clubInfo.ruleId);
			room_info.setClubRoomIndex(clubInfo.index);
			room_info.setClubMatchId(clubInfo.matchId);
		}
		room_info.setGameMaxPlayer(this.getTablePlayerNumber());
		if (commonGameRuleProtos != null) {
			room_info.setNewRules(commonGameRuleProtos);
		}

		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				room_info.addGameRuleIndexEx(ruleEx[i]);
			}

		}
		if (this.clubInfo.clubName != null) {
			room_info.setClubName(clubInfo.clubName);
		} else {
			room_info.setClubName("");
		}
		return room_info;
	}

	public void send_trustee_info() {
		if (istrustee == null) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(i);
			roomResponse.setIstrustee(istrustee[i]);
			this.send_response_to_room(roomResponse);
		}
	}

	/**
	 * 发送协议给指定位置玩家
	 *
	 * @param seat_index
	 * @param roomResponse
	 * @return
	 */
	public final boolean send_response_to_player(int seat_index, RoomResponse.Builder roomResponse) {
		if (seat_index < 0 || seat_index >= this.get_players().length) {
			return false;
		}

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		roomResponse.setAppId(getGame_id());
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public final boolean send_response_to_god_player(Player player, RoomResponse.Builder roomResponse) {
		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		roomResponse.setAppId(getGame_id());

		this.godViewObservers().send(player, roomResponse);
		return true;
	}

	public final boolean send_response_to_all_god(RoomResponse.Builder roomResponse) {
		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		roomResponse.setAppId(getGame_id());

		if (godViewObservers().count() > 0) {
			godViewObservers().sendAll(roomResponse);
		}
		return true;
	}

	/**
	 * @param roomResponse
	 * @param exceptObserver
	 *            排除围观者
	 * @return
	 */
	public boolean send_response_to_room(RoomResponse.Builder roomResponse, boolean exceptObserver) {
		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		roomResponse.setAppId(getGame_id());

		Player player = null;
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;

			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		if (!exceptObserver) {
			observers().sendAll(roomResponse);
		}
		return true;
	}

	/**
	 * 游戏大结算，记录数据。如果不超过1局会还豆
	 *
	 * @param game_end
	 */
	protected void record_game_round(GameEndResponse.Builder game_end, int real_reason) {

		if (GRR != null && !is_sys()) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			String stl = String.valueOf(id);
			game_end.setBrandIdStr(stl);
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();
			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());
			// 记录 to mangodb
			MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
					null, null, gzipByte, this.getRoom_id() + "",
					_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(), getRoom_owner_account_id());

		}
		if ((cost_dou > 0) && (this._cur_round == 1)) {
			huan_dou(game_end.getEndType());
		}
	}

	protected boolean checkHuanDou(int end_type) {
		return true;
	}

	@Override
	public final void runnable_create_time_out() {
		// 已经开始
		if (this._game_status != GameConstants.GS_MJ_FREE) {
			return;
		}

		// 把豆还给创建的人
		this.huan_dou(GameConstants.Game_End_RELEASE_SYSTEM);

		force_account("游戏等待超时解散");
	}

	public String get_game_des() {
		DescParams params = GameDescUtil.params.get();

		putDescParam(params);
		params._game_rule_index = _game_rule_index;
		params.setRuleMap(ruleMap);
		params._game_type_index = _game_type_index;
		if (gameRuleIndexEx != null) {
			params.game_rules = gameRuleIndexEx;
		}
		params.groupConfig = GameGroupRuleDict.getInstance().get(_game_type_index);

		return GameDescUtil.getGameDesc(params);
	}

	public final void log_error(String error) {
		logger.error("房间[" + this.getRoom_id() + " ,游戏Id:" + _game_type_index + "]" + "局数=" + _cur_round + error);
	}

	public final void log_warn(String error) {

		logger.warn("房间[" + this.getRoom_id() + " ,游戏Id:" + _game_type_index + "]" + "局数=" + _cur_round + error);

	}

	public void log_info(String info) {

		// logger.info("房间[" + this.getRoom_id() + "]" + info);

	}

	public final void log_player_error(int seat_index, String error) {
		logger.error("房间[" + this.getRoom_id() + " ,游戏Id:" + _game_type_index + "]" + " 玩家[" + seat_index + "]" + "局数="
				+ _cur_round + error);
	}

	public final DescParams getDescParams() {
		DescParams params = GameDescUtil.params.get();

		putDescParam(params);
		params._game_rule_index = _game_rule_index;
		params.setRuleMap(ruleMap);
		params._game_type_index = _game_type_index;
		if (gameRuleIndexEx != null) {
			params.game_rules = gameRuleIndexEx;
		}
		return params;
	}

	protected final void record_game_room() {
		// 第一局开始
		if (this._cur_round == 0) {

			_gameRoomRecord = new GameRoomRecord(RoomComonUtil.getMaxNumber(getDescParams()));
			this.set_record_id(BrandIdDict.getInstance().getId());

			_gameRoomRecord.set_record_id(this.get_record_id());
			_gameRoomRecord.setRoom_id(this.getRoom_id());
			_gameRoomRecord.setRoom_owner_account_id(this.getRoom_owner_account_id());
			_gameRoomRecord.setCreate_time(this.getCreate_time());
			_gameRoomRecord.setRoom_owner_name(this.getRoom_owner_name());
			_gameRoomRecord.set_player(_player_result);
			_gameRoomRecord.setPlayers(this.get_players());

			_gameRoomRecord.setGame_id(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index));

			String name = "";
			if (StringUtils.isNotEmpty(groupName)) {
				name = groupName;
			} else if (club_id > 0) {
				name = clubInfo.clubName;
			} else {
				name = getCreate_player().getNick_name();
			}

			Date createDate = new Date();

			this.setStartGameTime(createDate.getTime() / 1000L);

			_gameRoomRecord.setStart_time((int) (this.getStartGameTime()));

			_recordRoomRecord = MongoDBServiceImpl.getInstance().parentBrand(_gameRoomRecord.getGame_id(),
					this.get_record_id(), "", _gameRoomRecord.to_json(), (long) this._game_round,
					(long) this._game_type_index, this.getRoom_id() + "", getRoom_owner_account_id(), clubInfo,
					cost_dou, name, matchId, is_sys(), createDate, getCreate_type());

			_recordRoomRecord.setGroupID(groupID);

			_recordRoomRecord.setCreateType(getCreate_type());

			if (getCreate_player() != null) {
				_gameRoomRecord.setCreate_player(getCreate_player());
			}

		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;

			if (_player_status[i] == true)
				continue;
			// 兼容中途进来的玩家
			_player_status[i] = true;

			if (!is_sys()) {

				MongoDBServiceImpl.getInstance().accountBrand(_gameRoomRecord.getGame_id(),
						this.get_players()[i].getAccount_id(), this.get_record_id(), getRoom_owner_account_id(),
						club_id, getCreate_type(), clubInfo);
				continue;
			}

		}
	}

	public final int getCreateAccountSeatIndex() {
		if (getCreate_player() == null) {
			return 0;
		}
		for (int i = 0; i < get_players().length; i++) {
			if (get_players()[i] != null && get_players()[i].get_seat_index() == getCreate_player().get_seat_index()) {
				return get_players()[i].get_seat_index();
			}
		}
		return 0;
	}

	/*
	 */
	@Override
	public final boolean handler_request_goods(int get_seat_index, RoomRequest room_rq) {
		long targetID = room_rq.getTargetAccountId();
		int goodsID = room_rq.getGoodsID();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GOODS);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setGoodID(goodsID);
		roomResponse.setTargetID(targetID);
		this.send_response_to_room(roomResponse);

		// if (isCoinRoom()) {
		// return false;
		// }
		//
		// // 刷新玩家金币
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);
		return false;
	}

	public final boolean send_response_to_room(RoomResponse.Builder roomResponse) {
		return this.send_response_to_room(roomResponse, false);
	}

	public boolean send_response_to_other(int seat_index, RoomResponse.Builder roomResponse) {
		return this.send_response_to_other(seat_index, roomResponse, false);
	}

	/**
	 * @param roomResponse
	 * @param exceptObserver
	 *            排除围观者
	 * @return
	 */
	public boolean send_response_to_other(int seat_index, RoomResponse.Builder roomResponse, boolean exceptObserver) {
		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		roomResponse.setAppId(getGame_id());
		Player player = null;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;
			if (i == seat_index)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		if (!exceptObserver) {
			observers().sendAll(roomResponse);
		}

		if (godViewObservers().count() > 0) {
			godViewObservers().sendAll(roomResponse);
		}
		return true;
	}

	public final boolean send_sys_response_to_player(int seat_index, String msg) {
		if (seat_index < 0 || seat_index >= this.get_players().length) {
			return false;
		}

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(ESysMsgType.NONE.getId());
		msgBuilder.setMsg(msg);
		msgBuilder.setErrorId(EMsgIdType.ROOM_ERROR.getId());
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());

		return true;
	}

	/**
	 * 发送错误信息
	 *
	 * @param seat_index
	 * @param type
	 * @param msg
	 * @return
	 */
	public final boolean send_error_notify(int seat_index, int type, String msg) {
		if (seat_index < 0 || seat_index >= this.get_players().length) {
			return false;
		}
		if (this.get_players()[seat_index] == null)
			return false;
		return send_error_notify(this.get_players()[seat_index], type, msg);

	}

	@Override
	public final boolean send_error_notify(Player player, int type, String msg) {
		if (player == null) {
			return false;
		}

		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());
		return false;
	}

	@Override
	public final boolean handler_requst_location(Player player, LocationInfor locationInfor) {
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_player_info_data(roomResponse);
			if (_game_type_index == GameConstants.GAME_TYPE_SANDAHA) { // 三打哈问题解决方案
				this.load_room_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.send_response_to_player(i, roomResponse);
		}
		return true;
	}

	@Override
	public final boolean handler_requst_location_new(Player player, LocationInfor locationInfor) {
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_player_info_data_location(roomResponse);
			if (_game_type_index == GameConstants.GAME_TYPE_SANDAHA) { // 三打哈问题解决方案
				this.load_room_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS_LOCATION);
			this.send_response_to_player(i, roomResponse);
		}
		return true;
	}

	/**
	 * @param game_type_index
	 * @param game_rule_index
	 * @param game_round
	 */
	public boolean init_table(int game_type_index, int game_rule_index, int game_round) {
		this._game_type_index = game_type_index;
		this._game_rule_index = game_rule_index;
		this._game_round = game_round;
		if (kou_dou()) {
			on_init_table(game_type_index, game_rule_index, game_round);
			return true;
		}
		return false;

	}

	protected final boolean kou_dou() {
		return RoomUtil.kou_dou(this);
	}

	/**
	 * @param game_type_index
	 * @param game_rule_index
	 * @param game_round
	 */
	public abstract void on_init_table(int game_type_index, int game_rule_index, int game_round);

	// 玩家进入房间
	@Override
	public boolean handler_reconnect_room(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setIsGoldRoom(is_sys());
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);

		// 该玩家是观战者
		if (observers().exist(player.getAccount_id())) {
			observers().send(player, roomResponse);
		} else {
			send_response_to_player(player.get_seat_index(), roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
			send_response_to_other(player.get_seat_index(), roomResponse);
			godViewObservers().sendAll(roomResponse);
		}

		return true;
	}

	@Override
	public final boolean handler_audio_chat(Player player, com.google.protobuf.ByteString chat, int l,
			float audio_len) {
		return RoomUtil.handler_audio_chat(this, player, chat, l, audio_len);
	}

	@Override
	public boolean handler_online(int seat_index, boolean is_online) {
		if (seat_index < 0 || seat_index >= this.getTablePlayerNumber() || seat_index >= this.get_players().length)
			return false;
		Player player = this.get_players()[seat_index];
		if (player == null)
			return false;
		player.setOnline(is_online);
		handler_player_offline(player);
		return true;
	}

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);
		return true;
	}

	@Override
	public final boolean handler_emjoy_chat(Player player, int id) {

		RoomUtil.handler_emjoy_chat(this, player, id);

		return true;
	}

	@Override
	public final boolean process_flush_time() {
		setLast_flush_time(System.currentTimeMillis());

		return true;
	}

	@Override
	public boolean handler_requst_chat(int seat_index, RoomRequest room_rq, int type) {
		ChatMsgReq req = PBUtil.toObject(room_rq, ChatMsgReq.class);
		ChatMsgRsp.Builder builder = ChatMsgRsp.newBuilder();
		builder.setChatMsg(req.getChatMsg());

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ROOM_CHAT);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(builder));
		RoomUtil.send_response_to_room(this, roomResponse);
		return true;
	}

	@Override
	public boolean handler_requst_xia_ba(Player player, int pao, int ziBa, int duanmen) {
		return false;
	}

	@Override
	public boolean handler_requst_liang_zhang(int seat_index, int operate_code, int operate_card,
			List<Integer> linag_cards, int liang_cards_count) {
		return false;
	}

	@Override
	public boolean handler_requst_chu_zi(int seat_index, List<Integer> canCards, int type) {
		return false;
	}

	public int getMinPlayerCount() {
		return minPlayerCount;
	}

	public void setMinPlayerCount(int minPlayerCount) {
		this.minPlayerCount = minPlayerCount;
	}

	/**
	 * PS:有修改才同步 '' 同步最小开局人数到俱乐部服
	 */
	public void syncMinPlayerCountToClub() {
		ClubMsgSender.roomStatusUpdate(ERoomStatus.TABLE_MIN_PLAYER_COUNT, this);
	}

	public void syncMinPlayerCountToClub(int minPlayerCount) {
		this.minPlayerCount = minPlayerCount;
		ClubMsgSender.roomStatusUpdate(ERoomStatus.TABLE_MIN_PLAYER_COUNT, this);
	}

	/**
	 * 执行调度器
	 *
	 * @param args
	 */
	public void executeSchedule(SheduleArgs args) {
		scheduleMap.remove(args.getTimerId());
		timerCallBack(args);

		if (args.getTimerId() == autoReadyTimerId) {
			Runnable autoReady = new AutoReadyRunnable(getRoom_id());
			autoReady.run();
		}
	}

	/**
	 * 调度回调
	 */
	protected void timerCallBack(SheduleArgs args) {
	}

	/**
	 * @param timerId
	 * @param args
	 * @param delay
	 * @param replaceIfExsit
	 * @return
	 */
	public ScheduledFuture<?> schedule(int timerId, SheduleArgs args, long delay, boolean replaceIfExsit) {
		ScheduledFuture<?> future = GameSchedule.put(new CommonRunnable(getRoom_id(), args.setTimerId(timerId)), delay,
				TimeUnit.MILLISECONDS);
		ScheduledFuture<?> oldFuture = scheduleMap.put(timerId, future);
		if (replaceIfExsit && null != oldFuture) {
			oldFuture.cancel(false);
		}
		return future;
	}

	/**
	 * 调度器
	 *
	 * @param timerId
	 *            调度器id
	 * @param args
	 *            参数
	 * @param delay
	 *            延迟(单位ms)
	 */
	public ScheduledFuture<?> schedule(int timerId, SheduleArgs args, long delay) {
		return schedule(timerId, args, delay, true);
	}

	/**
	 * 调度器,自动生成调度器id
	 *
	 * @param args
	 * @param delay
	 */
	public ScheduledFuture<?> schedule(SheduleArgs args, long delay) {
		return schedule(AUTO_SCHEDULE_ID.decrementAndGet(), args, delay);
	}

	/**
	 * 取消调度
	 *
	 * @param timerId
	 */
	public void cancelShedule(int timerId) {
		ScheduledFuture<?> future = scheduleMap.remove(timerId);
		if (null != future) {
			future.cancel(false);
		}
	}

	/**
	 * @param timerId
	 * @param delay
	 * @return
	 */
	public ScheduledFuture<?> animationSchedule(int timerId, long delay) {
		final Runnable task = () -> {
			runInRoomLoop(() -> {
				animation_timer(timerId);
			});
		};

		ScheduledFuture<?> future = GameSchedule.put(task, delay, TimeUnit.MILLISECONDS);
		scheduleMap.put(timerId, future);
		return future;
	}

	/**
	 * 是否包含调度器
	 *
	 * @param timerId
	 * @return
	 */
	public boolean hasShedule(int timerId) {
		return scheduleMap.containsKey(timerId);
	}

	public LogicRoomInfo getLogicRoomInfo(boolean isWeb) {
		if (null == roomRedisModel)
			return null;

		LogicRoomInfo roomInfo = new LogicRoomInfo();
		roomInfo.set_game_type_index(roomRedisModel.getGame_type_index());
		roomInfo.setCurNum(roomRedisModel.getCur_player_num());
		roomInfo.setFull(roomRedisModel.getPlayer_max() == roomRedisModel.getCur_player_num() ? true : false);

		if (isWeb) {// web调用才需要传这些信息
			roomInfo.setRoomStatus(isStart ? 1 : 0);
			roomInfo.setCreateID(getRoom_owner_account_id());
			roomInfo.setCurRound(this._cur_round);
			roomInfo.setGameDesc(this.get_game_des());
			if (club_id > 0) {
				roomInfo.setClubId(club_id);
				roomInfo.setClubName(clubInfo.clubName);
			}
		}

		int[] playerIds = new int[roomRedisModel.getPlayersIdSet().size()];
		int i = 0;
		for (Long id : roomRedisModel.getPlayersIdSet()) {
			playerIds[i++] = (int) id.longValue();
		}
		roomInfo.setPlayerIDs(playerIds);
		return roomInfo;
	}

	public void sendTrusteeResp(int seatIndex) {
		if (is_match() || isCoinRoom()) {
			try {
				boolean istrustee = checkTrutess(seatIndex);
				if (istrustee) {
					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
					roomResponse.setOperatePlayer(seatIndex);
					roomResponse.setIstrustee(true);
					send_response_to_room(roomResponse);
				}
			} catch (Exception e) {
				logger.error("sendTrusteeMsg->error !!", e);
			}
		}

	}

	public final void runInRoomLoop(final Runnable task) {

		// 特殊处理，防止死循环
		int taskCount = taskQueue.addTask(task);

		if (taskCount > 200 && taskCount < 230) { // 加上限防止死循环搞死mongo
			String msg = String.format("房间id:%d,gameId:%d,gameTypeIndex:%d,任务数量:%d,线程:%s", getRoom_id(), getGame_id(),
					getGameTypeIndex(), taskCount, Thread.currentThread().getName());
			MongoDBServiceImpl.getInstance().systemLog_queue(ELogType.roomAutoDiverQueue, msg, null, null,
					ESysLogLevelType.NONE);

			if (taskCount == 201) {
				ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
				HashMap<String, String> msgMap = new HashMap<>();
				msgMap.put("mobile", "13670139534");
				msgMap.put("content", msg);
				centerRMIServer.rmiInvoke(RMICmd.SEND_MSG, msgMap);
			}

		}

		if (taskCount > 50) {
			logger.error("房间id:{},gameId:{},gameTypeIndex:{},任务数量:{},线程:{}", getRoom_id(), getGame_id(),
					getGameTypeIndex(), taskCount, Thread.currentThread().getName());
		}
	}

	@Override
	public boolean runnable_remove_middle_cards_general(int seat_index) {
		return true;
	}

	@Override
	public boolean handler_god_view_observer_enter(Player player) {
		godViewObservers().enter(player);
		player.setStatus(PlayerRoomStatus.GOD_OBSERVER);

		if (isStart && null != GRR) {
			GRR.setRoom(this);
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		roomResponse.setPlayerEnterType(PlayerRoomStatus.GOD_OBSERVER);
		godViewObservers().send(player, roomResponse);
		if (is_match()) {
			MatchTableService.getInstance().matchObserver(id, this, player.getAccount_id(), true);
		}

		return true;
	}

	/**
	 * 上帝视角围观者离开房间
	 */
	@Override
	public final boolean handler_god_view_observer_exit(Player player) {
		if (godViewObservers().exist(player.getAccount_id())) {
			player.setStatus(PlayerRoomStatus.INVALID);
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			godViewObservers().send(player, quit_roomResponse);
			godViewObservers().exit(player.getAccount_id());
			if (is_match()) {
				MatchTableService.getInstance().matchObserver(id, this, player.getAccount_id(), false);
			}
			return true;
		}
		return true;
	}

	/**
	 * 发给所有围观者
	 *
	 * @param roomResponse
	 */
	public void sendAllGodView(RoomResponse.Builder roomResponse) {
		godViewObservers().sendAll(roomResponse);
	}

	public boolean isClubMatch() {
		return clubInfo.matchId > 0;
	}

	public boolean isClubRoom() {
		return clubInfo.clubId > 0;
	}

	@Override
	public void handler_player_auto_ready(int seat_index) {
		if (_player_ready == null) {
			return;
		}
		int num = _player_ready.length;
		if (seat_index < 0 || seat_index >= num) {
			return;
		}
		if (_player_ready[seat_index] != 0) {
			return;
		}
		// handler_player_ready(seat_index, false);
	}

	public boolean isFraud() {
		return isFraud;
	}

	public void setFraud(boolean fraud) {
		isFraud = fraud;
	}

	/**
	 * 上报牌型类型
	 *
	 * @param triggerType
	 *            触发类型 {@see com.cai.common.define.ETriggerType }
	 * @param accountId
	 *            触发玩家id
	 * @param cardTypeValue
	 *            牌型值，对应GameConstants_*下的牌型值
	 */
	public final void triggerTypeEvent(ETriggerType triggerType, long accountId, long cardTypeValue) {
		if (id <= 0) {
			return;
		}
		// 支持金币场,比赛
		Player player = getPlayer(accountId);
		if (null != player && player instanceof RobotPlayer) {
			((RobotPlayer) player).triggerEvent(triggerType, cardTypeValue, 1);
		}
	}

	/**
	 * 上报余牌型类型
	 *
	 * @param triggerType
	 *            触发类型 {@see com.cai.common.define.ETriggerType }
	 * @param accountId
	 *            触发玩家id
	 * @param value
	 *            值(余牌类型:余牌数量,牌型:1)
	 */
	public final void triggerSurplusEvent(ETriggerType triggerType, long accountId, int value) {
		if (id <= 0) {
			return;
		}
		// 支持金币场,比赛
		Player player = getPlayer(accountId);
		if (null != player && player instanceof RobotPlayer) {
			((RobotPlayer) player).triggerEvent(triggerType, ECardCategory.SURPLUS.cardTypeValue(), value);
		}
	}

	/**
	 * 上报起手牌或者结束牌
	 *
	 * @param triggerType
	 * @param accountId
	 * @param cardArray
	 */
	public final void triggerCardEvent(ETriggerType triggerType, long accountId, int[] cardArray) {
		if (id <= 0 || triggerType == ETriggerType.ING) {
			return;
		}
		// 支持金币场,比赛
		Player player = getPlayer(accountId);
		if (null != player && player instanceof RobotPlayer) {
			int[] cardArray_ = new int[cardArray.length];
			System.arraycopy(cardArray, 0, cardArray_, 0, cardArray.length);
			((RobotPlayer) player).triggerCardEvent(triggerType, cardArray_);
		}
	}

	/**
	 * 上报事件截止
	 *
	 * @param triggerType
	 * @param accountId
	 */
	public final void triggerEventOver(ETriggerType triggerType, long accountId) {
		if (id <= 0) {
			return;
		}
		// 支持金币场,比赛
		Player player = getPlayer(accountId);
		if (null != player && player instanceof RobotPlayer) {

			((RobotPlayer) player).triggerEventOver(triggerType);
		}
	}

}
