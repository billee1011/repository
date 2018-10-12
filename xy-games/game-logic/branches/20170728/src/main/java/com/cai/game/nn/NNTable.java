/**
 * 
 */
package com.cai.game.nn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMsgIdType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.define.SysGameTypeEnum;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.FvMask;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AddJettonRunnable;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.OpenCardRunnable;
import com.cai.future.runnable.ReadyRunnable;
import com.cai.future.runnable.RobotBankerRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.nn.handler.NNHandler;
import com.cai.game.nn.handler.NNHandlerAddJetton;
import com.cai.game.nn.handler.NNHandlerCallBanker;
import com.cai.game.nn.handler.NNHandlerDispatchCard;
import com.cai.game.nn.handler.NNHandlerFinish;
import com.cai.game.nn.handler.NNHandlerOpenCard;
import com.cai.game.nn.handler.lzox.NNHandlerAddJetton_lzOX;
import com.cai.game.nn.handler.lzox.NNHandlerOpenCard_lzOX;
import com.cai.game.nn.handler.mfzox.NNHandlerAddJetton_mfzOX;
import com.cai.game.nn.handler.mfzox.NNHandlerCallBanker_mfzOX;
import com.cai.game.nn.handler.mfzox.NNHandlerOpenCard_mfzOX;
import com.cai.game.nn.handler.mszox.NNHandlerAddJetton_mszOX;
import com.cai.game.nn.handler.mszox.NNHandlerCallBanker_mszOX;
import com.cai.game.nn.handler.mszox.NNHandlerOpenCard_mszOX;
import com.cai.game.nn.handler.serverox.NNHandlerAddJetton_serverOX;
import com.cai.game.nn.handler.serverox.NNHandlerOpenCard_serverOX;
import com.cai.game.nn.handler.szox.NNHandlerAddJetton_szOX;
import com.cai.game.nn.handler.szox.NNHandlerOpenCard_szOX;
import com.cai.game.nn.handler.tbox.NNHandlerOpenCard_tbOX;
import com.cai.game.nn.handler.zyqox.NNHandlerAddJetton_zyqOX;
import com.cai.game.nn.handler.zyqox.NNHandlerCallBanker_zyqOX;
import com.cai.game.nn.handler.zyqox.NNHandlerOpenCard_zyqOX;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.AddJetton;
import protobuf.clazz.Protocol.CallBanker;
import protobuf.clazz.Protocol.CallBankerInfo;
import protobuf.clazz.Protocol.CardType;
import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStart;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.OpenCard;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.Timer_OX;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;

///////////////////////////////////////////////////////////////////////////////////////////////
public class NNTable extends Room {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(NNTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	/**
	 * 这个值 不能在这里改--这个参数测试用 通过后台改牌 改单个桌子的
	 */
	public boolean BACK_DEBUG_CARDS_MODE = false;
	/**
	 * 后台操作临时数组
	 */
	public int debug_my_cards[];

	public int _cur_round; // 局数
	public int _player_ready[]; // 准备
	public int _player_open_less[]; // 允许三人场
	public float _di_fen; // 底分

	public int _game_status;

	public boolean istrustee[]; // 托管状态

	public PlayerResult _player_result;
	public PlayerStatus _playerStatus[];

	public int _repertory_card[];

	GameRoomRecord _gameRoomRecord;
	BrandLogModel _recordRoomRecord;

	public int _call_banker[]; // 用户是否叫庄
	public int _add_Jetton[]; // 用户下注
	public boolean _open_card[]; // 用户摊牌
	public int _cur_call_banker; // 叫庄用户
	public int _cur_banker; // 当前庄
	public int _next_banker; // 下一轮的庄家
	public boolean _player_status[]; // 用户状态
	public int _player_count; // 用户数量
	public int _win_player_oxtb; // 通比牛牛赢家
	public boolean _win_player_ox[]; // 牛牛赢家
	public boolean _ping_Player_ox[]; // 牛牛平
	public int _jetton_info_sever_ox[][]; // 下注数据
	public int _jetton_info_cur[][]; // 当前下注数据
	public int _call_banker_info[]; // 叫庄信息
	public int _banker_times; // 庄家倍数
	public int _banker_max_times; // 庄家最大倍数
	public int _can_tuizhu_player[]; // 可以推注用户
	public GameRoundRecord GRR;
	public int _all_card_len = 0;
	public NNGameLogic _logic = null;
	public int _operate_start_time; // 操作开始时间
	public int _cur_operate_time; // 可操作时间
	public int _cur_game_timer; // 当前游戏定时器
	public int _card_type_ox[]; // 牌类型
	public int game_cell; // 游戏底分

	/**
	 * 当前牌桌的庄家
	 */
	public int _banker_select = GameConstants.INVALID_SEAT;

	/**
	 * 当前桌子内玩家数量
	 */
	private int playerNumber;

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;
	private ScheduledFuture _game_scheduled;

	public NNHandler _handler;

	public NNHandlerDispatchCard _handler_dispath_card;
	public NNHandlerCallBanker _handler_Call_banker;
	public NNHandlerAddJetton _handler_add_jetton;
	public NNHandlerOpenCard _handler_open_card;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public NNHandlerFinish _handler_finish; // 结束

	public NNTable() {
		super(RoomType.OX, GameConstants.GAME_PLAYER_SIX);

		_logic = new NNGameLogic();
		// 玩家
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			get_players()[i] = null;

		}
		_game_status = GameConstants.GS_OX_FREE;
		// 游戏变量
		_player_ready = new int[GameConstants.GAME_PLAYER_OX];
		_player_open_less = new int[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_call_banker = new int[GameConstants.GAME_PLAYER_OX]; // 用户是否叫庄
		_add_Jetton = new int[GameConstants.GAME_PLAYER_OX]; // 用户下注
		_open_card = new boolean[GameConstants.GAME_PLAYER_OX]; // 用户摊牌
		_cur_call_banker = 0;
		_player_status = new boolean[GameConstants.GAME_PLAYER_OX];
		_ping_Player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_win_player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_call_banker_info = new int[5];
		_can_tuizhu_player = new int[GameConstants.GAME_PLAYER_OX];
		_card_type_ox = new int[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_can_tuizhu_player[i] = 0;
			_card_type_ox[i] = -1;
		}
		_banker_max_times = 1;
		game_cell = 1;
	}

	public void init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;

		_game_round = game_round;
		_cur_round = 0;
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_jetton_info_sever_ox = new int[3][2];

		_jetton_info_sever_ox[0][0] = 1;
		_jetton_info_sever_ox[0][1] = 2;
		_jetton_info_sever_ox[1][0] = 2;
		_jetton_info_sever_ox[1][1] = 4;
		_jetton_info_sever_ox[2][0] = 4;
		_jetton_info_sever_ox[2][1] = 8;
		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_OX][5];
		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
		}
		_win_player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_ping_Player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_card_type_ox = new int[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_win_player_ox[i] = false;
			_ping_Player_ox[i] = false;
			_card_type_ox[i] = -1;
		}
		game_cell = 1;
		_banker_times = 1;
		_banker_max_times = 1;

		boolean kd = this.kou_dou();
		if (kd == false) {
			return;
		}

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), GameConstants.GAME_PLAYER_OX);

		if (is_mj_type(GameConstants.GAME_TYPE_TBOX) || is_mj_type(GameConstants.GAME_TYPE_TBOX_LX)) {
			_handler_open_card = new NNHandlerOpenCard_tbOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_SEVER_OX) || is_mj_type(GameConstants.GAME_TYPE_SEVER_OX_LX)) {
			_handler_add_jetton = new NNHandlerAddJetton_serverOX();
			_handler_open_card = new NNHandlerOpenCard_serverOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_SZOX) || is_mj_type(GameConstants.GAME_TYPE_SZOX_LX)) {
			_handler_add_jetton = new NNHandlerAddJetton_szOX();
			_handler_open_card = new NNHandlerOpenCard_szOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_LZOX) || is_mj_type(GameConstants.GAME_TYPE_LZOX_LX)) {
			_handler_add_jetton = new NNHandlerAddJetton_lzOX();
			_handler_open_card = new NNHandlerOpenCard_lzOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_ZYQOX) || is_mj_type(GameConstants.GAME_TYPE_ZYQOX_LX)) {
			_handler_add_jetton = new NNHandlerAddJetton_zyqOX();
			_handler_open_card = new NNHandlerOpenCard_zyqOX();
			_handler_Call_banker = new NNHandlerCallBanker_zyqOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MSZOX) || is_mj_type(GameConstants.GAME_TYPE_MSZOX_LX)) {
			_handler_add_jetton = new NNHandlerAddJetton_mszOX();
			_handler_open_card = new NNHandlerOpenCard_mszOX();
			_handler_Call_banker = new NNHandlerCallBanker_mszOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MFZOX) || is_mj_type(GameConstants.GAME_TYPE_MFZOX_LX)) {
			_handler_add_jetton = new NNHandlerAddJetton_mfzOX();
			_handler_open_card = new NNHandlerOpenCard_mfzOX();
			_handler_Call_banker = new NNHandlerCallBanker_mfzOX();
		}
		_handler_finish = new NNHandlerFinish();
		_call_banker = new int[GameConstants.GAME_PLAYER_OX]; // 用户是否叫庄
		_add_Jetton = new int[GameConstants.GAME_PLAYER_OX]; // 用户下注
		_open_card = new boolean[GameConstants.GAME_PLAYER_OX]; // 用户摊牌
		_player_status = new boolean[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}
		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_OX][5];
		_cur_call_banker = 0;
		_cur_banker = 0;
		_banker_times = 1;

	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////
	public boolean reset_init_data() {

		this.log_error("_game_rule_index" + this._game_rule_index);

		record_game_room();

		GRR = new GameRoundRecord(GameConstants.GAME_PLAYER_OX, GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[GameConstants.GAME_PLAYER_OX];
		istrustee = new boolean[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.OX_MAX_CARD_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_playerStatus[i].reset();
		}
		_call_banker = new int[GameConstants.GAME_PLAYER_OX]; // 用户是否叫庄
		_add_Jetton = new int[GameConstants.GAME_PLAYER_OX]; // 用户下注
		_open_card = new boolean[GameConstants.GAME_PLAYER_OX]; // 用户摊牌
		_player_status = new boolean[GameConstants.GAME_PLAYER_OX];
		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_OX][5];
		_card_type_ox = new int[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_win_player_ox[i] = false;
			_ping_Player_ox[i] = false;
			_card_type_ox[i] = -1;
		}
		_win_player_oxtb = -1;
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());

		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		GRR._video_recode.setBankerPlayer(this._banker_select);
		_banker_max_times = 1;
		game_cell = 1;
		return true;
	}

	public void getLocationTip() {
		RoomUtil.getLocationTip(this);
	}

	// 游戏开始
	public boolean handler_game_start() {

		_game_status = GameConstants.GS_OX_FREE;

		reset_init_data();

		GRR._banker_player = _banker_select;
		if (has_rule(GameConstants.GAME_RULE_MAX_ONE_TIMES))
			_banker_max_times = 1;
		if (has_rule(GameConstants.GAME_RULE_MAX_TWO_TIMES))
			_banker_max_times = 2;
		if (has_rule(GameConstants.GAME_RULE_MAX_THREE_TIMES))
			_banker_max_times = 3;
		if (has_rule(GameConstants.GAME_RULE_MAX_FOUR_TIMES))
			_banker_max_times = 4;
		//

		_repertory_card = new int[GameConstants.CARD_COUNT_OX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_OX);

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (is_mj_type(GameConstants.GAME_TYPE_TBOX) || is_mj_type(GameConstants.GAME_TYPE_TBOX_LX)) {
			return game_start_TBOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_SEVER_OX) || is_mj_type(GameConstants.GAME_TYPE_SEVER_OX_LX)) {
			return game_start_ServerOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_SZOX) || is_mj_type(GameConstants.GAME_TYPE_SZOX_LX)) {
			return game_start_SZOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_LZOX) || is_mj_type(GameConstants.GAME_TYPE_LZOX_LX)) {
			return game_start_LZOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_ZYQOX) || is_mj_type(GameConstants.GAME_TYPE_ZYQOX_LX)) {
			return call_banker_ZYQOX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MSZOX) || is_mj_type(GameConstants.GAME_TYPE_MSZOX_LX)) {
			return call_banker_MSZOX();
		}

		if (is_mj_type(GameConstants.GAME_TYPE_MFZOX) || is_mj_type(GameConstants.GAME_TYPE_MFZOX_LX)) {
			return call_banker_MFZOX();
		}

		return false;
	}

	public boolean call_banker_MFZOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_CALL_BANKER;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					if (k == i) {
						for (int j = 0; j < 4; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					} else {
						for (int j = 0; j < 4; j++) {
							cards.addItem(GameConstants.BLACK_CARD);

						}
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);
			this.load_room_info_data(roomResponse);
			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
			this.send_response_to_player(i, roomResponse);
		}

		// 围观
		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}

				} else {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.BLACK_CARD);

					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);
			this.load_room_info_data(roomResponse);
			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}
		roomResponse_ox.setCallBankerInfo(call_banker_info);
		SendCard.Builder send_card = SendCard.newBuilder();
		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] != true) {
				for (int j = 0; j < 4; j++) {
					cards.addItem(GameConstants.INVALID_CARD);

				}

			} else {
				for (int j = 0; j < 4; j++) {
					cards.addItem(GRR._cards_data[k][j]);

				}
			}
			// 回放数据
			this.GRR._video_recode.addHandCards(k, cards);
			send_card.addSendCard(k, cards);
		}

		roomResponse_ox.setSendCard(send_card);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, this._game_status);

		return true;
	}

	public boolean call_banker_MSZOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_CALL_BANKER;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		// 发送数据&围观
		{
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < 3; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {

					for (int j = 0; j < 3; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);
			this.load_room_info_data(roomResponse);
			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
			RoomUtil.send_response_to_room(this, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}
		roomResponse_ox.setCallBankerInfo(call_banker_info);
		SendCard.Builder send_card = SendCard.newBuilder();
		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] != true) {
				for (int j = 0; j < 3; j++) {
					cards.addItem(GameConstants.INVALID_CARD);

				}

			} else {

				for (int j = 0; j < 3; j++) {
					cards.addItem(GRR._cards_data[k][j]);

				}

			}
			// 回放数据
			this.GRR._video_recode.addHandCards(k, cards);
			send_card.addSendCard(k, cards);
		}

		roomResponse_ox.setSendCard(send_card);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, this._game_status);

		return true;
	}

	public boolean call_banker_ZYQOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_CALL_BANKER;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		// 发送数据
		{
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
			RoomUtil.send_response_to_room(this, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();
		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}
		roomResponse_ox.setCallBankerInfo(call_banker_info);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, this._game_status);

		return true;
	}

	public void game_start_ZYQOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态

		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		for (int j = _call_banker_info.length - 1; j >= 0; j--) {
			int chairID[] = new int[GameConstants.GAME_PLAYER_OX];
			int chair_count = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (this._player_status[i] == false)
					continue;
				if ((this._player_status[i] == true) && (_call_banker_info[j] == this._call_banker[i])) {

					chairID[chair_count++] = i;
				}

			}
			if (chair_count > 0) {
				int temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % chair_count);
				this._cur_banker = chairID[temp];
				_banker_times = _call_banker[this._cur_banker];
				if (_banker_times == 0)
					_banker_times = 1;
				break;
			}
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if (i != this._cur_banker) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[0][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[1][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[2][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
							}
						}

						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][2] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][2]);
						}
					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}
			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}

		//// 观战数据//////////
		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if ((i != this._cur_banker) && (this._player_status[i] == true)) {
					if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[0][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[0][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[1][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[1][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[2][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[2][j];
						}
					}

					if (_can_tuizhu_player[i] > 0) {
						_jetton_info_cur[i][2] = _can_tuizhu_player[i];
						cards.addItem(_jetton_info_cur[i][2]);
					}
				}

				game_start.addJettonCell(i, cards);
			}
			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if ((k != this._cur_banker) && (this._player_status[k] == true)) {
				if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[0][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[1][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[2][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
					}
				}

				if (_can_tuizhu_player[k] > 0) {
					_jetton_info_cur[k][2] = _can_tuizhu_player[k];
					cards.addItem(_jetton_info_cur[k][2]);
				}
			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, this._game_status);
	}

	public void add_call_banker(int seat_index) {
		{
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			// 回放数据
			CallBanker.Builder call_banker = CallBanker.newBuilder();
			call_banker.setSeatIndex(seat_index);
			call_banker.setCallBanker(_call_banker[seat_index]);
			roomResponse_ox.setCallBanker(call_banker);
			roomResponse.setType(MsgConstants.RESPONSE_SELECT_BANKER);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			RoomUtil.send_response_to_room(this, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		// 回放数据
		CallBanker.Builder call_banker = CallBanker.newBuilder();
		call_banker.setSeatIndex(seat_index);
		call_banker.setCallBanker(_call_banker[seat_index]);
		roomResponse_ox.setCallBanker(call_banker);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_SELECT_BANKER);
		this.GRR.add_room_response(roomResponse);
	}

	public boolean game_start_LZOX() {
		this._cur_banker = _next_banker;
		// 游戏开始
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if (i != this._cur_banker) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[0][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[1][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[2][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
							}
						}

						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][2] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][2]);
						}
					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}

		// 观战数据
		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if ((i != this._cur_banker) && (this._player_status[i] == true)) {
					if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[0][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[0][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[1][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[2][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[2][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[2][j];
						}
					}

					if (_can_tuizhu_player[i] > 0) {
						_jetton_info_cur[i][2] = _can_tuizhu_player[i];
						cards.addItem(_jetton_info_cur[i][2]);
					}
				}

				game_start.addJettonCell(i, cards);
			}

			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if ((k != this._cur_banker) && (this._player_status[k] == true)) {
				if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[0][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[1][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[2][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
					}
				}

				if (_can_tuizhu_player[k] > 0) {
					_jetton_info_cur[k][2] = _can_tuizhu_player[k];
					cards.addItem(_jetton_info_cur[k][2]);
				}
			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, this._game_status);

		return true;
	}

	public boolean game_start_SZOX() {
		// 游戏开始
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态
		if (_cur_round == 1) {
			int chairID[] = new int[GameConstants.GAME_PLAYER_OX];
			int chair_count = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (this._player_status[i] == true) {

					chairID[chair_count++] = i;
				}

			}
			if (chair_count > 0) {
				int temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % chair_count);
				this._cur_banker = chairID[temp];

			}
		} else
			this._cur_banker = this._next_banker;
		this._next_banker = this._cur_banker;
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);
			if (i != this._cur_banker) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[0][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[1][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[2][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
							}
						}

						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][2] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][2]);
						}

					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}

		// 围观
		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if ((i != this._cur_banker) && (this._player_status[i] == true)) {
					if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[0][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[0][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[1][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[1][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[2][j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[2][j];
						}
					}

					if (_can_tuizhu_player[i] > 0) {
						_jetton_info_cur[i][2] = _can_tuizhu_player[i];
						cards.addItem(_jetton_info_cur[i][2]);
					}

				}

				game_start.addJettonCell(i, cards);
			}

			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			RoomUtil.send_response_to_observer(this, roomResponse);
			// this.send_response_to_player(i, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if ((k != this._cur_banker) && (this._player_status[k] == true)) {
				if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[0][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[1][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[2][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
					}
				}

				if (_can_tuizhu_player[k] > 0) {
					_jetton_info_cur[k][2] = _can_tuizhu_player[k];
					cards.addItem(_jetton_info_cur[k][2]);
				}
			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, this._game_status);

		return true;
	}

	private boolean game_start_ServerOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);
			if (i != this._cur_banker) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[0][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[1][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
							}
						}
						if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
							for (int j = 0; j < 2; j++) {
								cards.addItem(_jetton_info_sever_ox[2][j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
							}
						}

						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][2] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][2]);
						}

					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}

		// 围观
		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			this.load_room_info_data(roomResponse);
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if ((k != this._cur_banker) && (this._player_status[k] == true)) {
					if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[0][j]);
							_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[1][j]);
							_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
						}
					}
					if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
						for (int j = 0; j < 2; j++) {
							cards.addItem(_jetton_info_sever_ox[2][j]);
							_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
						}
					}

					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][2] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][2]);
					}

				}

				game_start.addJettonCell(k, cards);
			}
			roomResponse_ox.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if ((k != this._cur_banker) && (this._player_status[k] == true)) {
				if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[0][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[1][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[1][j];
					}
				}
				if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
					for (int j = 0; j < 2; j++) {
						cards.addItem(_jetton_info_sever_ox[2][j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[2][j];
					}
				}

				if (_can_tuizhu_player[k] > 0) {
					_jetton_info_cur[k][2] = _can_tuizhu_player[k];
					cards.addItem(_jetton_info_cur[k][2]);
				}
			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, this._game_status);

		return true;
	}

	public void add_jetton_ox(int seat_index) {
		{
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			// 回放数据
			AddJetton.Builder add_jetton = AddJetton.newBuilder();
			add_jetton.setSeatIndex(seat_index);
			add_jetton.setJettonScore(_add_Jetton[seat_index]);
			roomResponse_ox.setAddJetton(add_jetton);
			roomResponse.setType(MsgConstants.RESPONSE_ADD_JETTON);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			RoomUtil.send_response_to_room(this, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		// 回放数据
		AddJetton.Builder add_jetton = AddJetton.newBuilder();
		add_jetton.setSeatIndex(seat_index);
		add_jetton.setJettonScore(_add_Jetton[seat_index]);
		roomResponse_ox.setAddJetton(add_jetton);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_JETTON);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		this.GRR.add_room_response(roomResponse);

	}

	public void send_card_date_ox() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_OPEN_CARD;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					if (k == i) {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					} else {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GameConstants.BLACK_CARD);

						}
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
			this.send_response_to_player(i, roomResponse);
		}

		{
			// 给围观者发牌背
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				// if (this.get_players()[i] == null) {
				// continue;
				// }
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[i] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
					send_card.addSendCard(i, cards);
				} else {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
					send_card.addSendCard(i, cards);
				}
			}
			roomResponse_ox.setSendCard(send_card);
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] == true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GRR._cards_data[k][j]);

					}

				} else {

					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}
				}

				send_card.addSendCard(k, cards);
			}
			roomResponse_ox.setSendCard(send_card);
		}
		roomResponse.setRoomResponseOx(roomResponse_ox);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return;
	}

	private boolean game_start_TBOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_OPEN_CARD;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
			this.game_cell = 1;
		}
		if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
			this.game_cell = 2;
		}
		if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
			this.game_cell = 4;
		}
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(-1);
			roomResponse_ox.setGameStart(game_start);
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					if (k == i) {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					} else {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GameConstants.BLACK_CARD);

						}
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);
			this.load_room_info_data(roomResponse);
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);

		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(-1);
			roomResponse_ox.setGameStart(game_start);
			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);
			this.load_room_info_data(roomResponse);
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(this._cur_operate_time);
			roomResponse_ox.setDisplayTime(timer);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			// this.send_response_to_player(i, roomResponse);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(-1);
		roomResponse_ox.setGameStart(game_start);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;

			SendCard.Builder send_card = SendCard.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] == true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GRR._cards_data[k][j]);

					}

				} else {

					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}
				}

				send_card.addSendCard(k, cards);
			}
			roomResponse_ox.setSendCard(send_card);
		}
		roomResponse.setRoomResponseOx(roomResponse_ox);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return true;
	}

	public void open_card_ox(int seat_index) {
		// 发送数据
		int times = 1;
		if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
			times = _logic.get_times_two(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
			times = _logic.get_times_one(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);

		{
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			// 回放数据
			this.GRR._video_recode.addHandCards(cards);
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				cards.addItem(GRR._cards_data[seat_index][j]);
			}
			SendCard.Builder sendCard = SendCard.newBuilder();
			sendCard.addSendCard(cards);
			roomResponse_ox.setSendCard(sendCard);
			CardType.Builder card_type = CardType.newBuilder();
			card_type.setCardType(this._card_type_ox[seat_index]);
			card_type.setTime(times);
			roomResponse_ox.setCardType(card_type);
			OpenCard.Builder open_card = OpenCard.newBuilder();
			open_card.setSeatIndex(seat_index);
			open_card.setOpen(true);
			roomResponse_ox.setOpenCard(open_card);
			roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			RoomUtil.send_response_to_room(this, roomResponse);
		}
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
			cards.addItem(GRR._cards_data[seat_index][j]);
		}
		SendCard.Builder sendCard = SendCard.newBuilder();
		sendCard.addSendCard(cards);
		roomResponse_ox.setSendCard(sendCard);
		OpenCard.Builder open_card = OpenCard.newBuilder();
		open_card.setSeatIndex(seat_index);
		open_card.setOpen(true);
		roomResponse_ox.setOpenCard(open_card);
		CardType.Builder card_type = CardType.newBuilder();
		card_type.setCardType(this._card_type_ox[seat_index]);
		roomResponse_ox.setCardType(card_type);
		roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		this.GRR.add_room_response(roomResponse);
		////////////////////////////////////////////////// 回放
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);

	}

	public void process_tbox_calulate_end() {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] == true) {

				_win_player_oxtb = i;
				break;
			}

		}
		for (int i = _win_player_oxtb + 1; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this._player_status[i] != true)
				continue;
			boolean first_ox = _logic.get_ox_card(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			boolean next_ox = _logic.get_ox_card(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			boolean action = _logic.compare_card(GRR._cards_data[_win_player_oxtb], GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, first_ox,
					next_ox, _game_rule_index);

			if (action == false) {
				_win_player_oxtb = i;
			}

		}
	}

	public void process_ox_calulate_end() {

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (i == this._cur_banker)
				continue;
			if (this._player_status[i] != true)
				continue;
			boolean first_ox = _logic.get_ox_card(GRR._cards_data[this._cur_banker], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			boolean next_ox = _logic.get_ox_card(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			boolean action = _logic.compare_card(GRR._cards_data[this._cur_banker], GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, first_ox,
					next_ox, _game_rule_index);
			// 获取点数
			int next_type = _logic.get_card_type(GRR._cards_data[this._cur_banker], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			int first_type = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			if (has_rule(GameConstants.GAME_RULE_EQUAL_PING) && (next_type == first_type)) {
				// 排序大小
				_ping_Player_ox[i] = true;
				continue;

			}
			// 获取点数
			if (action == true) {
				_win_player_ox[i] = true;
			}
		}
	}

	private void countCardType(int card_type, int seat_index) {
		try {
			int wFanShu = 0;
			if (card_type == GameConstants.OX_FIVE_KING) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.oxwuhuaox, "", 0, 0l, this.getRoom_id());
			}

			if (card_type == GameConstants.OX_BOOM) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.oxboomox, "", 0, 0l, this.getRoom_id());
			}

			if (card_type == GameConstants.OX_WUXIAONIU) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.oxwuxiaoox, "", 0, 0l, this.getRoom_id());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void process_chi_calulate_score() {

		GRR._win_order[_win_player_oxtb] = 1;
		int calculate_score = 0;

		if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
			calculate_score = _logic.get_times_two(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
			calculate_score = _logic.get_times_one(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);

		float lChiHuScore = calculate_score * this.game_cell;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] == false)
				continue;
			int card_type = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			countCardType(card_type, i);
		}
		////////////////////////////////////////////////////// 自摸 算分
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_ping_Player_ox[i] == true)
				continue;
			if (_player_status[i] == false)
				continue;
			if (i == _win_player_oxtb) {
				continue;
			}

			// 胡牌分
			GRR._game_score[i] -= lChiHuScore;
			GRR._game_score[_win_player_oxtb] += lChiHuScore;

		}

	}

	public void process_chi_calulate_score_ox() {

		int calculate_score = 0;
		float lChiHuScore = calculate_score;
		_win_player_oxtb = this._cur_banker;
		////////////////////////////////////////////////////// 自摸 算分
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] == false)
				continue;
			int card_type = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
			countCardType(card_type, i);
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_ping_Player_ox[i] == true)
				continue;
			if (_player_status[i] == false)
				continue;
			if (i == this._cur_banker) {
				continue;
			}
			if (_win_player_ox[i] == true) {
				if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
					calculate_score = _logic.get_times_two(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
				if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
					calculate_score = _logic.get_times_one(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
				lChiHuScore = calculate_score * _banker_times * _add_Jetton[i];
			} else {

				if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
					calculate_score = _logic.get_times_two(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
				if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
					calculate_score = _logic.get_times_one(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
				lChiHuScore = -calculate_score * _banker_times * _add_Jetton[i];
			}

			// 胡牌分

			GRR._game_score[i] -= lChiHuScore;
			GRR._game_score[this._cur_banker] += lChiHuScore;
			this.log_error("用户：" + this.get_players()[i].getNick_name() + "分数 " + GRR._game_score[i]);
			this.log_error("用户：" + this.get_players()[this._cur_banker].getNick_name() + "分数 " + GRR._game_score[this._cur_banker]);
		}
		if (has_rule(GameConstants.GAME_RULE_PlAYER_TUI_ZHU)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (_player_status[i] == false)
					continue;
				if (i == this._cur_banker)
					continue;
				if (this._can_tuizhu_player[i] == 0) {
					if (GRR._game_score[i] > 0) {
						int temp = 0;
						if ((int) GRR._game_score[i] + this._add_Jetton[i] > this._jetton_info_cur[i][0] * 10)
							temp = this._jetton_info_cur[i][0] * 10;
						else
							temp = (int) GRR._game_score[i] + this._add_Jetton[i];
						this._can_tuizhu_player[i] = temp;
						this.log_error("用户：" + this.get_players()[i].getNick_name() + "分数 " + GRR._game_score[i]);
						this.log_error("推注：" + this._can_tuizhu_player[i]);
					}
				}
			}

		}

	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > GameConstants.GAME_PLAYER_OX)
			return false;
		return istrustee[seat_index];
	}

	/**
	 * 开始游戏 逻辑
	 */
	/*
	 * public void game_start_real() { int playerCount = getPlayerCount();
	 * this.GRR._banker_player = this._current_player = this._banker_select; //
	 * 游戏开始 this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
	 * GameStartResponse.Builder gameStartResponse =
	 * GameStartResponse.newBuilder();
	 * gameStartResponse.setBankerPlayer(this.GRR._banker_player);
	 * gameStartResponse.setCurrentPlayer(this._current_player);
	 * gameStartResponse.setLeftCardCount(this.GRR._left_card_count);
	 * 
	 * int hand_cards[][] = new int[playerCount][GameConstants.MAX_FLS_COUNT];
	 * // 发送数据 for (int i = 0; i < playerCount; i++) { int hand_card_count =
	 * this._logic.switch_to_cards_data(this.GRR._cards_index[i],
	 * hand_cards[i]); gameStartResponse.addCardsCount(hand_card_count); }
	 * 
	 * // 发送数据 for (int i = 0; i < playerCount; i++) {
	 * Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
	 * 
	 * // 只发自己的牌 gameStartResponse.clearCardData(); for (int j = 0; j <
	 * GameConstants.MAX_FLS_COUNT; j++) {
	 * gameStartResponse.addCardData(hand_cards[i][j]); }
	 * 
	 * // 回放数据 this.GRR._video_recode.addHandCards(cards);
	 * 
	 * RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	 * this.load_room_info_data(roomResponse);
	 * this.load_common_status(roomResponse);
	 * 
	 * if (this._cur_round == 1) { // shuffle_players();
	 * this.load_player_info_data(roomResponse); }
	 * roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
	 * roomResponse.setGameStart(gameStartResponse);
	 * roomResponse.setCurrentPlayer( this._current_player ==
	 * GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * roomResponse.setGameStatus(this._game_status);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * this.send_response_to_player(i, roomResponse); }
	 * ////////////////////////////////////////////////// 回放
	 * RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	 * roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
	 * this.load_room_info_data(roomResponse);
	 * this.load_common_status(roomResponse);
	 * this.load_player_info_data(roomResponse); for (int i = 0; i <
	 * playerCount; i++) { Int32ArrayResponse.Builder cards =
	 * Int32ArrayResponse.newBuilder();
	 * 
	 * for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
	 * cards.addItem(hand_cards[i][j]); } gameStartResponse.addCardsData(cards);
	 * }
	 * 
	 * roomResponse.setGameStart(gameStartResponse);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * this.GRR.add_room_response(roomResponse);
	 * 
	 * // 检测听牌 for (int i = 0; i < playerCount; i++) {
	 * this._playerStatus[i]._hu_card_count =
	 * this.get_fls_ting_card(this._playerStatus[i]._hu_cards,
	 * this.GRR._cards_index[i], this.GRR._weave_items[i],
	 * this.GRR._weave_count[i]); if (this._playerStatus[i]._hu_card_count > 0)
	 * { this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count,
	 * this._playerStatus[i]._hu_cards); } }
	 * 
	 * this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0); }
	 */

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		_logic.random_card_data(repertory_card, mj_cards);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * 5 + j];
			}
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
		// int cards[] = new int[] {0x01,0x11,0x18,0x13,0x02};
		// int cards1[] = new int[] {0x0b,0x0c,0x2c,0x09,0x3d};
		// int cards2[] = new int[] {0x2C,0x11,0x33,0x2a,0x18};
		// int cards3[] = new int[] {0x16,0x2D,0x34,0x2C,0x38};
		// int cards4[] = new int[] {0x2C,0x11,0x33,0x2a,0x18};
		// int cards5[] = new int[] {0x2C,0x11,0x33,0x2a,0x18};
		////// int cards2[] = new int[]
		// {0x12,0x12,0x11,0x13,0x13,0x03,0x03,0x02,0x07,0x0a,0x1a,0x04,0x05,0x06,0x14,
		////// 0x06,0x02,0x17,0x1a,0x19};
		////// //In cards[] = new int[]{011,011,011};
		////// // int cards2[] = new int[] { 0x12, 0x12, 0x12, 0x12, 0x01, 0x02,
		// 0x04, 0x03, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17,
		////// // 0x17, 0x15, 0x19, 0x17,0x17,0x13 };
		////// // int cards3[] = new int[] { 0x13, 0x13, 0x14, 0x14, 0x01, 0x02,
		// 0x04, 0x03, 0x05, 0x06, 0x15, 0x15, 0x16, 0x17,
		////// // 0x17, 0x15, 0x19, 0x18,0x18,0x16 };
		//////
		////// for (int i = 0; i < GameConstants.GAME_PLAYER_HH; i++) {
		////// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		////// GRR._cards_data[i][j] = 0;
		////// }
		////// }
		//////// int send_count = (GameConstants.MAX_HH_COUNT );
		////////
		////// _repertory_card[_all_card_len - GRR._left_card_count] = 0x13;
		// for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
		// GRR._cards_data[0][j] = 0;
		// GRR._cards_data[1][j] = 0;
		// GRR._cards_data[2][j] = 0;
		// GRR._cards_data[3][j] = 0;
		// GRR._cards_data[4][j] = 0;
		// GRR._cards_data[5][j] = 0;
		// }
		//////////////// for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
		////////////////
		//////////////// GRR._cards_index[0][_logic.switch_to_card_index(cards[j])]
		// += 1;
		//////////////// }
		// for(int j = 0; j< cards.length;j++)
		// {
		// GRR._cards_data[0][j] =cards[j] ;
		// }
		// for(int j = 0; j<cards1.length;j++)
		// {
		// GRR._cards_data[1][j] =cards1[j] ;
		// }
		// for(int j = 0; j<cards2.length;j++)
		// {
		// GRR._cards_data[2][j] =cards2[j] ;
		// }
		// for(int j = 0; j<cards3.length;j++)
		// {
		// GRR._cards_data[3][j] =cards3[j] ;
		// }
		// for(int j = 0; j<cards4.length;j++)
		// {
		// GRR._cards_data[4][j] =cards4[j] ;
		// }
		// for(int j = 0; j<cards5.length;j++)
		// {
		// GRR._cards_data[5][j] =cards5[j] ;
		// }

		// for (int i = 0; i < GameConstants.GAME_PLAYER_HH; i++) {
		// int send_count = (GameConstants.MAX_HH_COUNT );
		//
		// for (int j = 0; j < send_count; j++) {
		// GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
		// }
		// }

//		/***************
//		 * 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 //
//		 ***********************/
//		int[] realyCards = new int[] { 0x39, 0x34, 0x31, 0x04, 0x2A, 0x07, 0x1b, 0x35, 0x3D, 0x3C, 0x0C, 0x01, 0x37, 0x11, 0x19, 0x16, 0x0D, 0x18,
//				0x33, 0x28, 0x14, 0x05, 0x2C, 0x1A, 0x25, 0x17, 0x1C, 0x23, 0x2B, 0x2D };
//		testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > GameConstants.OX_MAX_CARD_COUNT) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps);
					debug_my_cards = null;
				}
			}

		}
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		int count = realyCards.length / GameConstants.OX_MAX_CARD_COUNT;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		if (count > 6)
			count = 6;
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i++) {
				for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == count)
				break;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

		// 发牌
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	// 游戏结束
	public boolean handler_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return this.handler_game_finish_tbox(seat_index, reason);

		// return false;
	}

	public boolean handler_game_finish_tbox(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER_OX;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_READY_TIMER, GameConstants.HJK_READY_TIME_SECONDS, true);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		Timer_OX.Builder timer = Timer_OX.newBuilder();
		timer.setDisplayTime(this._cur_operate_time);
		roomResponse_ox.setDisplayTime(timer);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);

			// 特别显示的牌

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++)
				game_end.addGameScore(GRR._game_score[i]);

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
				if (this._game_scheduled != null)
					this.kill_timer();
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN)
				real_reason = reason;
			else
				real_reason = GameConstants.Game_End_RELEASE_PLAY;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);
		RoomUtil.send_response_to_room(this, roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER_OX; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	/**
	 * 创建房间
	 * 
	 * @return
	 */
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		this.setCreate_type(type);
		this.setCreate_player(player);
		// 代理开房
		if (type == GameConstants.CREATE_ROOM_PROXY) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);

			// 写入redis

			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "",
					RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.setGame_round(this._game_round);
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
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "",
					RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(RoomComonUtil.getMaxNumber(this));
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			return true;
		}

		// c成功
		get_players()[0] = player;
		player.set_seat_index(0);
		_cur_banker = player.get_seat_index();

		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		return send_response_to_player(player.get_seat_index(), roomResponse);
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {

		// if (_cur_round == 0 &&
		// !has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
		// for (Player tarplayer : get_players()) {
		// if (tarplayer == null)
		// continue;
		//
		// // logger.error("tarplayer
		// // ip=="+tarplayer.getAccount_ip()+"player
		// // ip=="+player.getAccount_ip());
		// if (player.getAccount_id() == tarplayer.getAccount_id())
		// continue;
		// if (StringUtils.isNotEmpty(tarplayer.getAccount_ip()) &&
		// StringUtils.isNotEmpty(player.getAccount_ip())
		// && player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
		// player.setRoom_id(0);// 把房间信息清除--
		// send_error_notify(player, 1, "不允许相同ip进入");
		// return false;
		// }
		// }
		// }

		int seat_index = GameConstants.INVALID_SEAT;
		/**
		 * 1) 勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，但只有房主能坐下（有空位时），此时普通用户进入时界面上只有“观战”按键。
		 * 2)不勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，且均能坐下（有空位时）。
		 * 3)游戏开始前进入桌子观战的用户，不管是否勾选此选项，只要有空位可随时坐下
		 */
		if (player.getAccount_id() != this.getRoom_owner_account_id() && has_rule(GameConstants.GAME_RULE_START_FORBID_JOIN)) {

			boolean flag = false;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (this._player_status[i] == true)
					flag = true;
			}
			if (flag == true) {
				send_error_notify(player, 2, "该房间已经禁止其它玩家在游戏中进入");
				return false;
			}

		}
		// if (has_rule(GameConstants.GAME_RULE_AA_PAY)) {
		// if (kou_dou_aa(player, seat_index, false) == false)
		// return false;
		// }
		if (playerNumber == 0) {// 未开始 才分配位置
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (get_players()[i] == null) {
					get_players()[i] = player;
					seat_index = i;
					break;
				}
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			send_error_notify(player, 1, "游戏已经开始");
			return false;
		}

		// 从观察者列表移出
		if (!observers().sit(player.getAccount_id())) {
			logger.error(String.format("玩家[%s]必须先成为观察者才能坐下!", player));
			// return false;
		}

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);

		// 同步数据

		// ========同步到中心========
		RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "", RoomRedisModel.class);
		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		// 写入redis
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(player.getAccount_id());
		rsAccountResponseBuilder.setRoomId(getRoom_id());
		//
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		return true;
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {
		// 限制围观者数量，未来加到配置表控制
		if (observers().count() >= 20) {
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

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			observers().send(player, quit_roomResponse);
			observers().exit(player.getAccount_id());
			return true;
		}
		return false;
	}

	// 玩家进入房间
	@Override
	public boolean handler_reconnect_room(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
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
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cai.common.domain.Room#handler_requst_open_less(com.cai.common.domain
	 * .Player, boolean)
	 */
	@Override
	public boolean handler_requst_open_less(Player player, boolean openThree) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(i, roomResponse2);
		}

		int count = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] != null) {
				count++;
			}
		}
		// 牌桌不是3人
		if (count != (GameConstants.GAME_PLAYER_OX - 1)) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}

		if (openLess == readys && readys == GameConstants.GAME_PLAYER_OX - 1) {
			playerNumber = readys;
			handler_game_start();

			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

		return false;
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

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) {//
			if (this.get_players()[seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}
		if (this.get_players()[seat_index] == null) {
			return false;
		}

		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		_player_ready[seat_index] = 1;

		log_error(seat_index + " ready");
		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}
		_player_count = 0;
		int _cur_count = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}
		log_error(seat_index + " ready" + _player_count + "cur_count" + _cur_count);
		if ((_player_count >= 2) && (_player_count == _cur_count))
			handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}

		return true;
		// handler_player_ready(seat_index);

	}

	@Override
	public boolean handler_observer_be_in_room(Player player) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && player != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_observer_be_in_room(this, player);

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				// this.send_response_to_player(seat_index, roomResponse);
				observers().send(player, roomResponse);
			}
		}

		return true;
	}

	/**
	 * //用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card) {
		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {

		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card, luoCode);
		}

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		if (this._handler != null) {
			this._handler.handler_call_banker(this, seat_index, call_banker);
		}

		return true;
	}

	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {

		if (this._handler != null) {
			this._handler.handler_add_jetton(this, seat_index, jetton);
		}

		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {

		if (this._handler != null) {
			this._handler.handler_open_cards(this, seat_index, open_flag);
		}

		return true;
	}

	/**
	 * 释放
	 */
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	public boolean process_flush_time() {
		long old_flush = super.getLast_flush_time();
		setLast_flush_time(System.currentTimeMillis());
		long new_flush = super.getLast_flush_time();

		// 线程安全，先不要开放，加好锁后再处理
		// //大于10分钟通知redis
		// if(new_flush - old_flush > 1000L*60*10){
		// RoomRedisModel roomRedisModel =
		// SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
		// super.getRoom_id()+"", RoomRedisModel.class);
		// if(roomRedisModel!=null){
		// roomRedisModel.setLast_flush_time(System.currentTimeMillis());
		// //写入redis
		// SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM,
		// super.getRoom_id()+"", roomRedisModel);
		// }
		// }

		return true;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	public void countChiHuTimes(int _seat_index, boolean isZimo) {

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
	}

	@Override
	public Player get_player(long account_id) {
		Player player = null;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			player = this.get_players()[i];
			if (player != null && player.getAccount_id() == account_id) {
				return player;
			}
		}

		return null;
	}

	@Override
	public boolean handler_audio_chat(Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		return RoomUtil.handler_audio_chat(this, player, chat, l, audio_len);
	}

	@Override
	public boolean handler_emjoy_chat(Player player, int id) {
		RoomUtil.handler_emjoy_chat(this, player, id);
		return true;
	}

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
	 * 
	 * Release_Room_Type_SEND = 1, //发起解散 Release_Room_Type_AGREE, //同意
	 * Release_Room_Type_DONT_AGREE, //不同意 Release_Room_Type_CANCEL,
	 * //还没开始,房主解散房间 Release_Room_Type_QUIT //还没开始,普通玩家退出房间
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {

		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 150;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}
		if (DEBUG_CARDS_MODE)
			delay = 10;
		int playerNumber = getTablePlayerNumber();
		if (playerNumber == 0) {
			playerNumber = GameConstants.GAME_PLAYER_OX;
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}
			if (this._game_scheduled != null) {
				this.kill_timer();
			}
			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER_HH; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < playerNumber; i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == playerNumber) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < playerNumber; j++) {
					player = this.get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < playerNumber; i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}
			// //大于一半就解散
			// int join_game_count = 0;
			// for(int i = 0; i< playerNumber; i++)
			// {
			// if(this._player_status[i] == true)
			// join_game_count++;
			// }
			// int jei_san_count = 0;
			// for(int i = 0; i< playerNumber; i++)
			// {
			// if(this._player_status[i] == true)
			// {
			// if (_gameRoomRecord.release_players[i] == 1) {
			// jei_san_count++;// 有一个不同意
			// }
			//
			// }
			// }
			// if(jei_san_count<=join_game_count/2)
			// {
			// return false;
			// }

			for (int j = 0; j < playerNumber; j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			this.set_timer(this._cur_game_timer, this._cur_operate_time, false);
			if (this._cur_operate_time > 0) {
				Timer_OX.Builder timer = Timer_OX.newBuilder();
				timer.setDisplayTime(_cur_operate_time);
				roomResponse_ox.setDisplayTime(timer);
			}
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			// 大于一半就解散
			// int join_game_count = 0;
			// for(int i = 0; i< playerNumber; i++)
			// {
			// if(this._player_status[i] == true)
			// join_game_count++;
			// }
			// int jei_san_count = 0;
			// for(int i = 0; i< playerNumber; i++)
			// {
			// if(this._player_status[i] == true)
			// {
			// if (_gameRoomRecord.release_players[i] == 1) {
			// jei_san_count++;//
			// }
			//
			// }
			// }
			// if(jei_san_count<join_game_count/2)
			// {
			// return false;
			// }
			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < playerNumber; j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}

			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < playerNumber; i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出

			if (this._player_status[seat_index] == true) {
				if (GameConstants.GS_MJ_FREE != _game_status) {
					// 游戏已经开始
					return false;
				}
				send_error_notify(seat_index, 2, "您已经开始游戏了,不能退出游戏");
				return false;
			}
			// if (has_rule(GameConstants.GAME_RULE_AA_PAY)) {
			// this.huan_dou_aa(seat_index);
			// }
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());

			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);
			int _cur_count = 0;
			int player_count = 0;
			boolean flag = false;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					_cur_count += 1;
				}
				if (this._player_status[i] == true)
					flag = true;
				if (_player_ready[i] == 0) {

				}
				if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
					player_count += 1;
				}
			}
			log_error(seat_index + " ready" + _player_count + "cur_count" + _cur_count);
			if ((player_count >= 2) && (player_count == _cur_count) && (flag == false))
				handler_game_start();

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {

		return true;
	}

	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			roomResponse.setFlashTime(250);
			roomResponse.setStandTime(1500);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(1500);

		}

		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			roomResponse.setFlashTime(250);
			roomResponse.setStandTime(250);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(400);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}
			this.send_response_to_other(seat_index, roomResponse);// 别人是背着的
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			}

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	// 添加牌到牌队
	private boolean operate_add_discard(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// 出牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 胡息 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param hu_xi_kind_type
	 * @param hu_xi_count
	 * @param time
	 * @param to_player
	 * @return
	 */
	// public boolean operate_update_hu_xi(int seat_index, int
	// hu_xi_kind_type,int hu_xi_count, int time, int to_player) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_UPDATE_HU_XI);
	// roomResponse.setHuXiType(hu_xi_kind_type);
	// roomResponse.setHuXiCount(hu_xi_count);
	// roomResponse.setEffectTime(time);
	//
	// if (to_player == GameConstants.INVALID_SEAT) {
	// GRR.add_room_response(roomResponse);
	// this.send_response_to_room(roomResponse);
	// } else {
	// this.send_response_to_player(to_player, roomResponse);
	// }
	//
	// return true;
	// }

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			// GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
					| GameConstants.WIK_EQS;
			this.log_error(
					"weave.kind" + curPlayerStatus._action_weaves[i].weave_kind + "center_card" + curPlayerStatus._action_weaves[i].center_card);
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {
							this.log_error("lou_qi_weave.kind" + curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);

							chi_group.addLouWeaveKind(curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);
						}
					}
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		// GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 发牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param to_player
	 * @return
	 */
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(2500);
			roomResponse.setInsertTime(150);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);// 给别人 牌数据
			}
			this.send_response_to_other(seat_index, roomResponse);
			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}
			GRR.add_room_response(roomResponse);
			return this.send_response_to_player(seat_index, roomResponse);

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
				GRR.add_room_response(roomResponse);
				return this.send_response_to_player(seat_index, roomResponse);
			}
		}

		return false;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				if ((weaveitems[j].weave_kind == GameConstants.WIK_TI_LONG || weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG)
						&& weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_player_xiang_gong_flag(int seat_index, boolean is_xiang_gong) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_XIANGGONG);
		roomResponse.setProvidePlayer(seat_index);
		roomResponse.setIsXiangGong(is_xiang_gong);
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 删除牌堆的牌 (吃碰杆的那张牌 从废弃牌堆上移除)
	 * 
	 * @param seat_index
	 * @param discard_index
	 * @return
	 */

	public boolean operate_remove_discard(int seat_index, int discard_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setDiscardIndex(discard_index);

		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 牌局中分数结算
	 * 
	 * @param seat_index
	 * @param score
	 * @return
	 */
	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean handler_player_offline(Player player) {
		// 旁观者下线后直接移出
		if (observers().exist(player.getAccount_id())) {
			this.handler_exit_room_observer(player);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setGameStatus(_game_status);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse);

			send_response_to_other(player.get_seat_index(), roomResponse);
		}

		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER_OX;
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					winner = j;
				}
			}
			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (winner != -1) {
				_player_result.win_order[winner] = win_idx;
				// win_idx++;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			player_result.addPlayersId(i);
			// }
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(this._cur_banker);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {

			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * //处理首牌
	 * 
	 * @param seat_index
	 * @param delay_time
	 * @return
	 */
	public boolean exe_chuli_first_card(int seat_index, int type, int delay_time) {

		// if (delay_time > 0) {
		// GameSchedule.put(new ChulifirstCardRunnable(this.getRoom_id(),
		// seat_index, type, false), delay_time,
		// TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		// } else {
		// // 发牌
		// this._handler = this._handler_chuli_firstcards;
		// this._handler_chuli_firstcards.reset_status(seat_index, type);
		// this._handler.exe(this);
		// }

		return true;
	}

	/***
	 * 加载房间的玩法 状态信息
	 * 
	 * @param roomResponse
	 */
	public void load_room_info_data(RoomResponse.Builder roomResponse) {
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._banker_select);

		roomResponse.setRoomInfo(room_info);
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i] < 0 ? 0 : _player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * 发给指定玩家
	 * 
	 * @param seat_index
	 * @param roomResponse
	 * @return
	 */
	public boolean send_response_to_player(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public boolean send_sys_response_to_player(int seat_index, String msg) {
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
	 * 发给房间所有人
	 * 
	 * @param roomResponse
	 * @return
	 */
	public boolean send_response_to_room(RoomResponse.Builder roomResponse) {
		return send_response_to_room(roomResponse, false);
	}

	/**
	 * 
	 * @param roomResponse
	 * @param exceptObserver
	 *            排除围观者
	 * @return
	 */
	public boolean send_response_to_room(RoomResponse.Builder roomResponse, boolean exceptObserver) {
		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		if (!exceptObserver) {
			observers().sendAll(roomResponse);
		}

		return true;
	}

	public boolean send_error_notify(int seat_index, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());

		return false;

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

	/**
	 * 发给其他玩家
	 * 
	 * @param seat_index
	 * @param roomResponse
	 * @return
	 */
	public boolean send_response_to_other(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
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
		// 也给观战者发送
		observers().sendAll(roomResponse);
		return true;
	}

	private void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtianhu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhyidianhong, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhonghu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhongfantian, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhallhei, "", 0, 0l, this.getRoom_id());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void record_game_room() {
		// 第一局开始
		_gameRoomRecord = new GameRoomRecord(RoomComonUtil.getMaxNumber(this));
		if (this._cur_round == 0)
			this.set_record_id(BrandIdDict.getInstance().getId());

		_gameRoomRecord.set_record_id(this.get_record_id());
		_gameRoomRecord.setRoom_id(this.getRoom_id());
		_gameRoomRecord.setRoom_owner_account_id(this.getRoom_owner_account_id());
		_gameRoomRecord.setCreate_time(this.getCreate_time());
		_gameRoomRecord.setRoom_owner_name(this.getRoom_owner_name());
		_gameRoomRecord.set_player(_player_result);
		_gameRoomRecord.setPlayers(this.get_players());

		_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

		// 设置战绩游戏ID

		_gameRoomRecord.setGame_id(SysGameTypeEnum.getGameIDByTypeIndex(_game_type_index));

		if (this._cur_round == 0) {
			_recordRoomRecord = MongoDBServiceImpl.getInstance().parentBrand(_gameRoomRecord.getGame_id(), this.get_record_id(), "",
					_gameRoomRecord.to_json(), (long) this._game_round, (long) this._game_type_index, this.getRoom_id() + "",
					getRoom_owner_account_id());
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (!((this.get_players()[i] != null) || (this._player_status[i] == true)))
				continue;
			MongoDBServiceImpl.getInstance().accountBrand(_gameRoomRecord.getGame_id(), this.get_players()[i].getAccount_id(), this.get_record_id(),
					getRoom_owner_account_id());
		}

	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	private void record_game_round(GameEndResponse.Builder game_end) {
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setMsg(_gameRoomRecord.to_json());
			MongoDBServiceImpl.getInstance().updateParenBrand(_recordRoomRecord);
		}

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			String stl = String.valueOf(id);
			game_end.setBrandIdStr(stl);
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			// 记录 to mangodb
			MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "", null, null, gzipByte,
					this.getRoom_id() + "", _recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(), getRoom_owner_account_id());

		}

		if ((cost_dou > 0) && (this._cur_round == 1)) {
			// 不是正常结束的
			if ((game_end.getEndType() != GameConstants.Game_End_NORMAL) && (game_end.getEndType() != GameConstants.Game_End_RELEASE_PLAY)) {
				// 还豆
				StringBuilder buf = new StringBuilder();
				buf.append("开局失败[" + game_end.getEndType() + "]" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id())
						.append(",game_type_index:" + _game_type_index).append(",game_round:" + _game_round)
						.append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
				// 把豆还给玩家
				AddGoldResultModel result = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(), cost_dou, false, buf.toString(),
						EGoldOperateType.FAILED_ROOM);
				if (result.isSuccess() == false) {
					logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
				}

			}
		}

	}

	public void log_error(String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + error);

	}

	public void log_player_error(int seat_index, String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + " 玩家[" + seat_index + "]" + error);

	}

	public String get_game_des() {
		return GameDescUtil.getGameDesc(_game_type_index, _game_rule_index);
	}

	public boolean is_zhuang_xian() {
		if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI) || is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			return false;
		}
		return true;
	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {
		return card;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {
		this._handler = this._handler_finish;
		this._handler_finish.exe(this);
		return true;
	}

	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (card_count > 0) {
			if (card_data[0] == 0)
				log_error(" 加入到牌堆" + seat_index + ':' + card_count + ':' + card_data[0]);

		}
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

		return true;

	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_card;
			this._handler_dispath_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d) {
		// // 是否有抢杠胡
		// this._handler = this._handler_gang;
		// this._handler_gang.reset_status(seat_index, provide_player,
		// center_card, action, type, self, d,depatch);
		// this._handler.exe(this);

		return true;
	}

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_out_card(int seat_index, int card, int type) {
		// 出牌
		// this._handler = this._handler_out_card_operate;
		// this._handler_out_card_operate.reset_status(seat_index, card, type);
		// this._handler.exe(this);

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		// 出牌
		// _last_player =provider;
		// this._handler = this._handler_chi_peng;
		// this._handler_chi_peng.reset_status(seat_index, _out_card_player,
		// action, card, type);
		// this._handler.exe(this);

		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
				TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;

			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
		if (send_client == true) {
			this.operate_add_discard(seat_index, card_count, card_data);
		}
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards(int seat_index) {

	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	public void runnable_create_time_out() {
		// 已经开始
		if (this._game_status != GameConstants.GS_MJ_FREE) {
			return;
		}

		// 把豆还给创建的人
		this.huan_dou(GameConstants.Game_End_RELEASE_SYSTEM);

		process_release_room();
	}

	/***
	 * 强制解散
	 */
	public boolean force_account() {
		if (this._cur_round == 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			Player player = null;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏已被系统解散");
			}
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_SYSTEM);
		}

		return false;
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i].set_seat_index(i);
		}
	}

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

	}

	public boolean handler_requst_location(Player player, LocationInfor locationInfor) {
		// LocationUtil.LantitudeLongitudeDist(lon1, lat1, lon2, lat2);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.send_response_to_player(i, roomResponse);
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee && SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			istrustee[get_seat_index] = isTrustee;
		}
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		// // 发牌
		// this._handler = this._handler_chuli_firstcards;
		// this._handler_chuli_firstcards.reset_status(_seat_index, _type);
		// this._handler.exe(this);
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		// if (playerNumber == 0) {
		// playerNumber = GameConstants.GAME_PLAYER_HH;
		// }
		return GameConstants.GAME_PLAYER_OX;
	}

	public boolean refresh_room_redis_data(int type, boolean notifyRedis) {
		if (type == GameConstants.PROXY_ROOM_UPDATE) {
			int cur_player_num = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (this.get_players()[i] != null) {
					cur_player_num++;
				}
			}

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "",
					RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(GameConstants.GAME_PLAYER_OX);
			roomRedisModel.setCur_player_num(cur_player_num);
			roomRedisModel.setGame_round(this._game_round);
			// roomRedisModel.setCreate_time(System.currentTimeMillis());
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		} else if (type == GameConstants.PROXY_ROOM_RELEASE) {

		}
		if (notifyRedis) {
			// 通知redis消息队列
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
			RsCmdResponse.Builder rsCmdResponseBuilder = RsCmdResponse.newBuilder();
			rsCmdResponseBuilder.setType(3);
			rsCmdResponseBuilder.setAccountId(this.getRoom_owner_account_id());
			redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicProxy);

		}
		return true;
	}

	private boolean kou_dou_aa(Player cur_player, int seat_index, boolean create_room) {
		int game_id = 0;
		int game_type_index = this._game_type_index;
		int index = 0;
		game_id = SysGameTypeEnum.getGameIDByTypeIndex(game_type_index);
		index = SysGameTypeEnum.getGameGoldTypeIndex(game_type_index);
		this.setGame_id(game_id);
		// 判断房卡
		SysParamModel sysParamModel1007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1007);
		SysParamModel sysParamModel1008 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1008);
		SysParamModel sysParamModel1009 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1009);
		int check_gold = 0;
		boolean create_result = true;

		if (_game_round == sysParamModel1007.getVal1()) {
			check_gold = sysParamModel1007.getVal3();
		} else if (_game_round == sysParamModel1008.getVal1()) {
			check_gold = sysParamModel1008.getVal3();
		} else if (_game_round == sysParamModel1009.getVal1()) {
			check_gold = sysParamModel1009.getVal3();
		}

		// 注意游戏ID不一样
		if (check_gold == 0) {
			create_result = false;
		} else {
			// 是否免费的
			if (create_room == true) {
				SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);

				if (sysParamModel != null && sysParamModel.getVal2() == 1) {
					// 收费
					StringBuilder buf = new StringBuilder();
					buf.append("创建房间:" + this.getRoom_id()).append("game_id:" + this.getGame_id()).append(",game_type_index:" + game_type_index)
							.append(",game_round:" + _game_round);
					AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(this.getRoom_owner_account_id(), check_gold, false,
							buf.toString());
					if (result.isSuccess() == false) {
						create_result = false;
					} else {
						// 扣豆成功
						cost_dou = check_gold;
					}
				}
			}
			if (create_result == false) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
				Player player = null;
				for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
					this.send_response_to_player(i, roomResponse);

					player = this.get_players()[i];
					if (player == null)
						continue;
					if (i == 0) {
						send_error_notify(i, 1, "闲逸豆不足,游戏解散");
					} else {
						send_error_notify(i, 1, "创建人闲逸豆不足,游戏解散");
					}

				}

				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
				return false;
			}

		}
		if (create_room == false) {
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);

			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				// 收费
				StringBuilder buf = new StringBuilder();
				// buf.append("创建房间:" + this.getRoom_id()).append("game_id:" +
				// this.getGame_id())
				// .append(",game_type_index:" +
				// game_type_index).append(",game_round:" + _game_round);
				AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(cur_player.getAccount_id(), check_gold, false, buf.toString());
				if (result.isSuccess() == false) {
					create_result = false;
				} else {
					// 扣豆成功
					cost_dou = check_gold;
				}
			}
			if (create_result == false) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
				send_error_notify(cur_player, 1, "您的闲逸豆不够，不能参与游戏");

				// 删除房间

				return false;
			}
		}

		return true;
	}

	private boolean kou_dou() {
		return RoomUtil.kou_dou(this);
		// int game_type_index = this._game_type_index;
		// //收费索引
		// this.game_index =
		// SysGameTypeEnum.getGameGoldTypeIndex(game_type_index);
		// int game_id=SysGameTypeEnum.getGameIDByTypeIndex(game_type_index);
		// this.setGame_id(SysGameTypeEnum.getGameIDByTypeIndex(game_type_index));
		// if (is_sys()) {
		// return true;
		// }
		// // 判断房卡
		// // 判断房卡
		// SysParamModel sysParamModel1007 =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
		// .get(1007);
		// SysParamModel sysParamModel1008 =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
		// .get(1008);
		// SysParamModel sysParamModel1009 =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
		// .get(1009);
		// int check_gold = 0;
		// boolean create_result = true;
		//
		// if (_game_round == sysParamModel1007.getVal1()) {
		// check_gold = sysParamModel1007.getVal3();
		// } else if (_game_round == sysParamModel1008.getVal1()) {
		// check_gold = sysParamModel1008.getVal3();
		// } else if (_game_round == sysParamModel1009.getVal1()) {
		// check_gold = sysParamModel1009.getVal3();
		// }
		// // 注意游戏ID不一样
		// if (check_gold == 0) {
		// create_result = false;
		// } else {
		// // 是否免费的
		// SysParamModel sysParamModel =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
		// .get(this.game_index );
		//
		// if (sysParamModel != null && sysParamModel.getVal2() == 1) {
		// // 收费
		// StringBuilder buf = new StringBuilder();
		// buf.append("创建房间:" + this.getRoom_id()).append("game_id:" +
		// this.getGame_id())
		// .append(",game_type_index:" + game_type_index).append(",game_round:"
		// + _game_round);
		// AddGoldResultModel result =
		// PlayerServiceImpl.getInstance().subGold(this.getRoom_owner_account_id(),
		// check_gold, false, buf.toString());
		// if (result.isSuccess() == false) {
		// create_result = false;
		// } else {
		// // 扣豆成功
		// cost_dou = check_gold;
		// }
		// }
		//
		// }
		// if (create_result == false) {
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		// Player player = null;
		// for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
		// this.send_response_to_player(i, roomResponse);
		//
		// player = this.get_players()[i];
		// if (player == null)
		// continue;
		// if (i == 0) {
		// send_error_notify(i, 1, "闲逸豆不足,游戏解散");
		// } else {
		// send_error_notify(i, 1, "创建人闲逸豆不足,游戏解散");
		// }
		//
		// }
		//
		// // 删除房间
		// PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		// return false;
		// }
		//
		// return true;
	}

	private boolean huan_dou(int result) {
		if (cost_dou == 0)
			return false;

		boolean huan = false;
		if (result == GameConstants.Game_End_NORMAL || result == GameConstants.Game_End_DRAW || result == GameConstants.Game_End_ROUND_OVER) {
			return false;
		} else if (result == GameConstants.Game_End_RELEASE_NO_BEGIN) {
			// 还没开始
			huan = true;
		} else if (result == GameConstants.Game_End_RELEASE_RESULT || result == GameConstants.Game_End_RELEASE_PLAY
				|| result == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || result == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

		if (huan) {

			// 不是正常结束的
			// if((game_end.getEndType()!=GameConstants.Game_End_NORMAL) &&
			// (game_end.getEndType()!=GameConstants.Game_End_DRAW)){
			// 还豆
			StringBuilder buf = new StringBuilder();
			buf.append("开局失败" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id()).append(",game_type_index:" + _game_type_index)
					.append(",game_round:" + _game_round).append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
			// 把豆还给玩家
			AddGoldResultModel addresult = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(), cost_dou, false, buf.toString(),
					EGoldOperateType.FAILED_ROOM);
			if (addresult.isSuccess() == false) {
				logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
			}

		}

		return true;
	}

	public boolean huan_dou_aa(int seat_index) {
		if (this._player_status[seat_index] == false) {
			StringBuilder buf = new StringBuilder();
			buf.append("开局失败" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id()).append(",game_type_index:" + _game_type_index)
					.append(",game_round:" + _game_round).append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
			// 把豆还给玩家
			AddGoldResultModel addresult = PlayerServiceImpl.getInstance().addGold(this.get_players()[seat_index].getAccount_id(), cost_dou, false,
					buf.toString(), EGoldOperateType.FAILED_ROOM);
			if (addresult.isSuccess() == false) {
				logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
			}

		}
		return true;
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_goods(int,
	 * protobuf.clazz.Protocol.RoomRequest)
	 */
	@Override
	public boolean handler_request_goods(int get_seat_index, RoomRequest room_rq) {
		long targetID = room_rq.getTargetAccountId();
		int goodsID = room_rq.getGoodsID();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GOODS);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setGoodID(goodsID);
		roomResponse.setTargetID(targetID);
		this.send_response_to_room(roomResponse);
		return false;
	}

	@Override
	public int getGameTypeIndex() {
		return _game_type_index;
	}

	@Override
	public void clear_score_in_gold_room() {

	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean set_timer(int timer_type, int time, boolean makeDBtimer) {
		// if (!is_sys())
		// return false;

		SysParamModel sysParamModel3008 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeEnum.getGameIDByTypeIndex(_game_type_index)).get(3008);
		if (sysParamModel3008.getVal5() == 0)
			return true;
		if (makeDBtimer == false) {
			_cur_game_timer = timer_type;
			if (timer_type == GameConstants.HJK_READY_TIMER) {
				_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
				_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
				_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			}
			return true;
		}
		_cur_game_timer = timer_type;
		if (timer_type == GameConstants.HJK_READY_TIMER) {
			_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), sysParamModel3008.getVal1(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal1();
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), sysParamModel3008.getVal2(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal2();
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), sysParamModel3008.getVal3(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal3();
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), sysParamModel3008.getVal4(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal4();
		}
		return true;

	}

	public boolean kill_timer() {
		// if (!is_sys())
		// return false;
		_game_scheduled.cancel(false);
		_game_scheduled = null;

		return false;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	public boolean open_card_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._open_card[i] == false) {

				this._handler.handler_open_cards(this, i, true);

			}
		}
		return false;
	}

	public boolean robot_banker_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._call_banker[i] == -1) {

				this._handler.handler_call_banker(this, i, 0);

			}
		}
		return false;
	}

	public boolean ready_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++)
			if (this.get_players()[i] != null) {
				if (this._player_ready[i] == 0) {
					handler_player_ready(i, false);

				}
			}
		return false;
	}

	public boolean add_jetton_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._add_Jetton[i] == 0) {

				this._handler.handler_add_jetton(this, i, 0);

			}
		}
		return false;
	}

	@Override
	public void init_other_param(Object... objects) {
		// WalkerGeek Auto-generated method stub

	}

	/**
	 * @param seat_index
	 * @param call_banker
	 * @param qiang_banker
	 * @return
	 */
	@Override
	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
		return true;
	}
}
