/**
 * 
 */
package com.cai.game.hjk;

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
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.hjk.handler.HJKHandler;
import com.cai.game.hjk.handler.HJKHandlerAddJetton;
import com.cai.game.hjk.handler.HJKHandlerCallBanker;
import com.cai.game.hjk.handler.HJKHandlerDispatchCard;
import com.cai.game.hjk.handler.HJKHandlerFinish;
import com.cai.game.hjk.handler.HJKHandlerOpenCard;
import com.cai.game.hjk.handler.HJKHandlerQuest;
import com.cai.game.hjk.handler.hjk.HJKHandlerAddJetton_hjk;
import com.cai.game.hjk.handler.hjk.HJKHandlerCallBanker_hjk;
import com.cai.game.hjk.handler.hjk.HJKHandlerDispatchCard_hjk;
import com.cai.game.hjk.handler.hjk.HJKHandlerOpenCard_hjk;
import com.cai.game.hjk.handler.hjk.HJKHandlerQuest_hjk;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol.AddJetton_HJK;
import protobuf.clazz.Protocol.ButtonPop_HJK;
import protobuf.clazz.Protocol.CallBankerInfo_HJK;
import protobuf.clazz.Protocol.CallBanker_HJK;
import protobuf.clazz.Protocol.CardValue_HJK;
import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStart_HJK;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.OpenCard_HJK;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_HJK;
import protobuf.clazz.Protocol.SendCard_HJK;
import protobuf.clazz.Protocol.TouXiang_HJK;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.YaoCard_HJK;

///////////////////////////////////////////////////////////////////////////////////////////////
public class HJKTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(HJKTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

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
	public boolean _win_player[]; // 赢家
	public boolean _ping_Player[]; // 平
	public int _jetton_info_sever_ox[][]; // 下注数据
	public int _jetton_info_cur[][]; // 当前下注数据
	public int _call_banker_info[]; // 叫庄信息
	public int _banker_times; // 庄家倍数
	public int _banker_max_times; // 庄家最大倍数
	public int _can_tuizhu_player[]; // 可以推注用户
	public int _game_times[]; // 游戏分数
	public float _temp_score[]; // 临时分数启记录
	public int _Pop_button; // 弹按钮
	public boolean _yao_button; // 要按钮
	public boolean _pass_button; // 过按钮
	public boolean _all_open_card; // 全开
	public boolean _make_pass_button[]; // 使用过pass
	public int _no_tou_xiang_player[]; // 投降用户
	public int _card_type[]; // 牌类型
	public HJKGameLogic _logic = null;
	public int _time; // 游戏是否翻倍
	public int _wu_xiao_long; // 上家五小龙用户
	public int _is_tou_xiang_count; // 判断投降用户数
	public int _own_room_seat; // 房主的位置

	public int _last_banker = GameConstants.INVALID_SEAT; // 上局庄家

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public HJKHandler _handler;

	public HJKHandlerDispatchCard _handler_dispath_card;
	public HJKHandlerCallBanker _handler_Call_banker;
	public HJKHandlerAddJetton _handler_add_jetton;
	public HJKHandlerOpenCard _handler_open_card;
	public HJKHandlerQuest _handler_quest;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public HJKHandlerFinish _handler_finish; // 结束

	public HJKTable() {
		super(RoomType.HJK, GameConstants.GAME_PLAYER_FIVE);

		_logic = new HJKGameLogic();
		// 玩家
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			get_players()[i] = null;

		}
		_game_status = GameConstants.GS_OX_FREE;
		// 游戏变量
		_player_ready = new int[GameConstants.GAME_PLAYER_HJK];
		_player_open_less = new int[GameConstants.GAME_PLAYER_HJK];
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}

		_last_banker = GameConstants.INVALID_SEAT;
		_call_banker = new int[GameConstants.GAME_PLAYER_HJK]; // 用户是否叫庄
		_add_Jetton = new int[GameConstants.GAME_PLAYER_HJK]; // 用户下注
		_open_card = new boolean[GameConstants.GAME_PLAYER_HJK]; // 用户摊牌
		_cur_call_banker = 0;
		_player_status = new boolean[GameConstants.GAME_PLAYER_HJK];
		_ping_Player_ox = new boolean[GameConstants.GAME_PLAYER_HJK];
		_win_player_ox = new boolean[GameConstants.GAME_PLAYER_HJK];
		_call_banker_info = new int[5];
		_can_tuizhu_player = new int[GameConstants.GAME_PLAYER_HJK];
		_game_times = new int[GameConstants.GAME_PLAYER_HJK];
		_ping_Player = new boolean[GameConstants.GAME_PLAYER_HJK];
		_win_player = new boolean[GameConstants.GAME_PLAYER_HJK];
		_temp_score = new float[GameConstants.GAME_PLAYER_HJK];
		_wu_xiao_long = -1;
		_own_room_seat = -1;
		_is_tou_xiang_count = 0;
		_Pop_button = -1;
		_yao_button = false;
		_pass_button = false;
		_all_open_card = false;
		_card_type = new int[GameConstants.GAME_PLAYER_HJK];
		_make_pass_button = new boolean[GameConstants.GAME_PLAYER_HJK];
		_no_tou_xiang_player = new int[GameConstants.GAME_PLAYER_HJK];
		_is_tou_xiang_count = 0;
		_time = 1;
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_can_tuizhu_player[i] = 0;
			_game_times[i] = 0;
			_win_player[i] = false;
			_ping_Player[i] = false;
			_temp_score[i] = 0;
			_make_pass_button[i] = false;
			_no_tou_xiang_player[i] = 0;
			_card_type[i] = 0;
		}
		_banker_max_times = 1;
		this.setMinPlayerCount(3);
	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;

		_jetton_info_sever_ox = new int[2][4];

		_jetton_info_sever_ox[0][0] = 1;
		_jetton_info_sever_ox[1][0] = 1;
		_jetton_info_sever_ox[0][1] = 2;
		_jetton_info_sever_ox[1][1] = 2;
		_jetton_info_sever_ox[0][2] = 3;
		_jetton_info_sever_ox[1][2] = 3;
		_jetton_info_sever_ox[0][3] = 5;
		_jetton_info_sever_ox[1][3] = 5;
		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_HJK][5];

		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
			for (int j = 0; j < 5; j++)
				_jetton_info_cur[i][j] = 0;
		}
		_last_banker = GameConstants.INVALID_SEAT;
		_win_player_ox = new boolean[GameConstants.GAME_PLAYER_HJK];
		_ping_Player_ox = new boolean[GameConstants.GAME_PLAYER_HJK];
		_win_player = new boolean[GameConstants.GAME_PLAYER_HJK];
		_ping_Player = new boolean[GameConstants.GAME_PLAYER_HJK];
		_temp_score = new float[GameConstants.GAME_PLAYER_HJK];
		_wu_xiao_long = -1;
		_is_tou_xiang_count = 0;
		_Pop_button = -1;
		_yao_button = false;
		_pass_button = false;
		_all_open_card = false;
		_own_room_seat = -1;
		_time = 1;
		_make_pass_button = new boolean[GameConstants.GAME_PLAYER_HJK];
		_card_type = new int[GameConstants.GAME_PLAYER_HJK];
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_win_player_ox[i] = false;
			_ping_Player_ox[i] = false;
			_win_player[i] = false;
			_ping_Player[i] = false;
			_temp_score[i] = 0;
			_make_pass_button[i] = false;
			_no_tou_xiang_player[i] = 0;
			_card_type[i] = 0;

		}
		_wu_xiao_long = -1;
		_banker_times = 1;
		_banker_max_times = 1;

		_Pop_button = -1;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), GameConstants.GAME_PLAYER_HJK);

		_handler_finish = new HJKHandlerFinish();
		_call_banker = new int[GameConstants.GAME_PLAYER_HJK]; // 用户是否叫庄
		_add_Jetton = new int[GameConstants.GAME_PLAYER_HJK]; // 用户下注
		_open_card = new boolean[GameConstants.GAME_PLAYER_HJK]; // 用户摊牌
		_player_status = new boolean[GameConstants.GAME_PLAYER_HJK];
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_game_times[i] = 0;
		}
		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_HJK][5];

		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < 5; j++)
				_jetton_info_cur[i][j] = 0;
		}
		_cur_call_banker = 0;
		_cur_banker = 0;
		_banker_times = 1;
		if (this.is_mj_type(GameConstants.GAME_TYPE_HJK)) {
			_handler_Call_banker = new HJKHandlerCallBanker_hjk();
			_handler_add_jetton = new HJKHandlerAddJetton_hjk();
			_handler_open_card = new HJKHandlerOpenCard_hjk();
			_handler_dispath_card = new HJKHandlerDispatchCard_hjk();
			_handler_quest = new HJKHandlerQuest_hjk();
		}
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

		record_game_room();

		GRR = new GameRoundRecord(GameConstants.GAME_PLAYER_HJK, GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[GameConstants.GAME_PLAYER_HJK];
		istrustee = new boolean[GameConstants.GAME_PLAYER_HJK];
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_COUNT_HJK + 5);
		}
		_yao_button = false;
		_pass_button = false;
		_all_open_card = false;
		_card_type = new int[GameConstants.GAME_PLAYER_HJK];
		_cur_round++;
		_time = 1;
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_playerStatus[i].reset();
		}
		_call_banker = new int[GameConstants.GAME_PLAYER_HJK]; // 用户是否叫庄
		_add_Jetton = new int[GameConstants.GAME_PLAYER_HJK]; // 用户下注
		_open_card = new boolean[GameConstants.GAME_PLAYER_HJK]; // 用户摊牌
		_player_status = new boolean[GameConstants.GAME_PLAYER_HJK];
		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_HJK][5];

		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
			for (int j = 0; j < 5; j++)
				_jetton_info_cur[i][j] = 0;
		}
		_make_pass_button = new boolean[GameConstants.GAME_PLAYER_HJK];
		_no_tou_xiang_player = new int[GameConstants.GAME_PLAYER_HJK];
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_win_player_ox[i] = false;
			_ping_Player_ox[i] = false;
			_make_pass_button[i] = false;
			_no_tou_xiang_player[i] = 0;
			_card_type[i] = 0;
		}
		_win_player_oxtb = -1;

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
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
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

		GRR._video_recode.setBankerPlayer(this._cur_banker);
		_banker_max_times = 1;

		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_OX_FREE;

		reset_init_data();

		GRR._banker_player = _cur_banker;
		if (has_rule(GameConstants.GAME_RULE_MAX_ONE_TIMES))
			_banker_max_times = 1;
		if (has_rule(GameConstants.GAME_RULE_MAX_TWO_TIMES))
			_banker_max_times = 2;
		if (has_rule(GameConstants.GAME_RULE_MAX_THREE_TIMES))
			_banker_max_times = 3;
		if (has_rule(GameConstants.GAME_RULE_MAX_FOUR_TIMES))
			_banker_max_times = 4;
		//
		if (is_mj_type(GameConstants.GAME_TYPE_HJK)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_HJK];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HJK);
		}
		//
		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
		if (is_mj_type(GameConstants.GAME_TYPE_HJK)) {
			return select_mode_HJK();
		}

		return false;
	}

	public boolean select_mode_HJK() {
		this._game_status = GameConstants.GS_HJK_CALL_BANKER;// 设置状态
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_EVERYONE_ROBOT_BANKER)) {

			call_banker_hjk();

		}
		if (has_rule(GameConstants.GAME_RULE_WUXILONG_HUAN_BANKER)) {
			game_start_hjk();
		}
		if (has_rule(GameConstants.GAME_RULE_DING_BNAKER)) {
			game_start_hjk();
		}
		if (has_rule(GameConstants.GAME_RULE_FIRST_ROBOT_BANKER)) {
			if (this._cur_round == 1) {
				call_banker_hjk();
			} else {
				game_start_hjk();
			}
		}
		if (has_rule(GameConstants.GAME_RULE_LUN_BNAKER)) {
			game_start_hjk();
		}
		return true;
	}

	public boolean call_banker_hjk() {
		// 游戏开始

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this._player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();

			this.load_room_info_data(roomResponse);
			CallBankerInfo_HJK.Builder call_banker_info = CallBankerInfo_HJK.newBuilder();
			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse_hjk.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		CallBankerInfo_HJK.Builder call_banker_info = CallBankerInfo_HJK.newBuilder();
		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}
		roomResponse_hjk.setCallBankerInfo(call_banker_info);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, this._game_status);

		return true;
	}

	public void game_start_hjk() {
		// 游戏开始
		this._game_status = GameConstants.GS_HJK_ADD_JETTON;// 设置状态
		if (has_rule(GameConstants.GAME_RULE_EVERYONE_ROBOT_BANKER)) {
			for (int j = this._banker_max_times; j >= 0; j--) {
				int chairID[] = new int[GameConstants.GAME_PLAYER_HJK];
				int chair_count = 0;
				for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
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
					if (_banker_times <= 0)
						_banker_times = 1;
					break;
				}
			}
		}
		if (has_rule(GameConstants.GAME_RULE_WUXILONG_HUAN_BANKER)) {
			if (this._wu_xiao_long != -1) {
				_cur_banker = this._wu_xiao_long;
			} else {
				if (this._cur_round == 1 && this._own_room_seat != -1)
					_cur_banker = this._own_room_seat;
				else if (this._cur_round == 1) {
					this._banker_max_times = 0;
					for (int j = this._banker_max_times; j >= 0; j--) {
						int chairID[] = new int[GameConstants.GAME_PLAYER_HJK];
						int chair_count = 0;
						for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
							if (this._player_status[i] == false)
								continue;
							if (this._player_status[i] == true) {

								chairID[chair_count++] = i;
							}

						}
						if (chair_count > 0) {
							int temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % chair_count);
							this._cur_banker = chairID[temp];
							_banker_times = _call_banker[this._cur_banker];
							if (_banker_times <= 0)
								_banker_times = 1;
							break;
						}
					}
				}
			}
			_banker_times = 1;
		}
		if (has_rule(GameConstants.GAME_RULE_DING_BNAKER)) {
			_cur_banker = 0;
			_banker_times = 1;
		}
		if (has_rule(GameConstants.GAME_RULE_FIRST_ROBOT_BANKER)) {
			if (this._cur_round == 1) {
				for (int j = this._banker_max_times; j >= 0; j--) {
					int chairID[] = new int[GameConstants.GAME_PLAYER_HJK];
					int chair_count = 0;
					for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
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
						if (_banker_times <= 0)
							_banker_times = 1;
						break;
					}
				}
			} else {
				int chairID[] = new int[GameConstants.GAME_PLAYER_HJK];
				int chair_count = 0;

				if (this._wu_xiao_long != -1)
					this._cur_banker = this._wu_xiao_long;
				_banker_times = 1;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_LUN_BNAKER)) {
			if (this._cur_round == 1 && this._own_room_seat != -1)
				_cur_banker = this._own_room_seat;
			else if (this._cur_round == 1) {
				this._banker_max_times = 0;
				for (int j = this._banker_max_times; j >= 0; j--) {
					int chairID[] = new int[GameConstants.GAME_PLAYER_HJK];
					int chair_count = 0;
					for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
						if (this._player_status[i] == false)
							continue;
						if (this._player_status[i] == true) {

							chairID[chair_count++] = i;
						}

					}
					if (chair_count > 0) {
						int temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % chair_count);
						this._cur_banker = chairID[temp];
						_banker_times = _call_banker[this._cur_banker];
						if (_banker_times <= 0)
							_banker_times = 1;
						break;
					}
				}
			}
		}
		this._wu_xiao_long = -1;
		_can_tuizhu_player[this._cur_banker] = 0;
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();

			this.load_room_info_data(roomResponse);

			GameStart_HJK.Builder game_start = GameStart_HJK.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if (i != this._cur_banker) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_HJK; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true) && (i == k)) {
						for (int j = 0; j < 3; j++) {
							cards.addItem(_jetton_info_sever_ox[0][j]);
							_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
						}
						if (_can_tuizhu_player[k] > 0 && _last_banker == this._cur_banker) {
							_jetton_info_cur[k][3] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][3]);
						}
					}
					game_start.addJettonCell(k, cards);
				}
			}
			roomResponse_hjk.setGameStart(game_start);
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart_HJK.Builder game_start = GameStart_HJK.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_HJK; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if ((k != this._cur_banker) && (this._player_status[k] == true)) {
				for (int j = 0; j < 3; j++) {
					cards.addItem(_jetton_info_sever_ox[0][j]);
					_jetton_info_cur[k][j] = _jetton_info_sever_ox[0][j];
				}
				if (_can_tuizhu_player[k] > 0 && _last_banker == this._cur_banker) {
					_jetton_info_cur[k][3] = _can_tuizhu_player[k];
					cards.addItem(_jetton_info_cur[k][3]);
				}
			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse_hjk.setGameStart(game_start);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, this._game_status);

	}

	public void add_call_banker(int seat_index) {
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			// if(this._player_status[i] != true)
			// continue;
			if (this.get_players()[i] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			// 回放数据
			CallBanker_HJK.Builder call_banker = CallBanker_HJK.newBuilder();
			call_banker.setSeatIndex(seat_index);
			call_banker.setCallBanker(_call_banker[seat_index]);
			roomResponse_hjk.setCallBanker(call_banker);
			roomResponse.setType(MsgConstants.RESPONSE_SELECT_BANKER);
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		// 回放数据
		CallBanker_HJK.Builder call_banker = CallBanker_HJK.newBuilder();
		call_banker.setSeatIndex(seat_index);
		call_banker.setCallBanker(_call_banker[seat_index]);
		roomResponse_hjk.setCallBanker(call_banker);
		roomResponse.setType(MsgConstants.RESPONSE_SELECT_BANKER);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
	}

	public boolean is_open_card(int seat_index) {
		int sum[] = new int[3];
		this._card_type[seat_index] = _logic.get_card_sum(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index], sum);
		if (this._logic.is_black_jack(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]) == true) {
			this._card_type[seat_index] = GameConstants.HJK_CARD_TYPE_HJK;
			return true;
		}
		if (this._logic.is_aa_card(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]) == true) {
			this._card_type[seat_index] = GameConstants.HJK_CARD_TYPE_AA;
			return true;
		}
		if (this._logic.is_seven_card(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]) == true) {
			this._card_type[seat_index] = GameConstants.HJK_CARD_TYPE_SEVEN;
			return true;
		}
		if (this._logic.is_wu_dragon(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]) == true) {
			this._card_type[seat_index] = GameConstants.HJK_CARD_TYPE_WUXIAOLONG;
			if (this._wu_xiao_long == -1)
				this._wu_xiao_long = seat_index;
			return true;
		}
		if ((seat_index == this._cur_banker) && (is_pass_button(seat_index))) {
			return true;

		}
		if ((seat_index != this._cur_banker) && this.GRR._card_count[seat_index] == 5 && (is_pass_button(seat_index))) {
			return true;
		}

		return false;
	}

	public boolean is_pass_button(int seat_index) {
		int sum[] = new int[3];
		if (_logic.is_burst(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]) == true) {
			this._card_type[seat_index] = GameConstants.HJK_CARD_TYPE_BRUST;
			return true;
		}
		int temp = _logic.get_card_sum(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index], sum);
		if (temp == 21) {
			boolean flag = true;
			for (int j = 2; j >= 0; j--) {
				if (sum[j] != 0 && sum[j] < temp) {
					flag = false;
				}
			}
			if (flag == true) {
				this._card_type[seat_index] = 21;
				return true;
			}

		}
		if (GRR._card_count[seat_index] == GameConstants.MAX_COUNT_HJK) {
			this._card_type[seat_index] = temp;
			return true;
		}

		return false;
	}

	public void caluate_score(int seat_index, boolean b_time) {
		int times[] = new int[1];
		if (this._logic.deduce_winer(GRR._cards_data[_cur_banker], GRR._card_count[_cur_banker], GRR._cards_data[seat_index],
				GRR._card_count[seat_index], times) == 1) {
			if (seat_index != _cur_banker && GRR._card_count[seat_index] == 5) {
				times[0] = 2;
			}
			_game_times[seat_index] = times[0];

			if (b_time == true)
				GRR._game_score[seat_index] = -times[0] * _add_Jetton[seat_index] * _time;
			else
				GRR._game_score[seat_index] = -times[0] * _add_Jetton[seat_index];

			this._temp_score[seat_index] = GRR._game_score[seat_index];
			this._temp_score[_cur_banker] += -this._temp_score[seat_index];
			GRR._game_score[_cur_banker] += -GRR._game_score[seat_index];

		} else if (_logic.deduce_winer(GRR._cards_data[_cur_banker], GRR._card_count[_cur_banker], GRR._cards_data[seat_index],
				GRR._card_count[seat_index], times) == 2) {
			if (seat_index != _cur_banker && GRR._card_count[seat_index] == 5) {
				times[0] = 2;
			}
			_game_times[seat_index] = times[0];

			_win_player[seat_index] = true;
			if (b_time == true)
				GRR._game_score[seat_index] = times[0] * _add_Jetton[seat_index] * _time;
			else
				GRR._game_score[seat_index] = times[0] * _add_Jetton[seat_index];
			this._temp_score[seat_index] = GRR._game_score[seat_index];
			this._temp_score[_cur_banker] += -this._temp_score[seat_index];
			GRR._game_score[_cur_banker] += -GRR._game_score[seat_index];

		} else {
			_ping_Player[seat_index] = true;
			this.GRR._game_score[seat_index] = 0;
			this.GRR._game_score[_cur_banker] += this.GRR._game_score[seat_index];
		}

		_player_result.game_score[seat_index] += this._temp_score[seat_index];
		_player_result.game_score[_cur_banker] -= this._temp_score[seat_index];
		if (has_rule(GameConstants.GAME_RULE_NO_LEI_ZHU))
			return;

		if (!(has_rule(GameConstants.GAME_RULE_LUN_BNAKER) == true || has_rule(GameConstants.GAME_RULE_EVERYONE_ROBOT_BANKER) == true)) {

			if (GRR._game_score[seat_index] > 0) {
				int temp = (int) GRR._game_score[seat_index] + this._add_Jetton[seat_index];
				if (has_rule(GameConstants.GAME_RULE_MAX_TWENTY_FOUR)) {
					if (temp > 24)
						temp = 24;
				}
				if (has_rule(GameConstants.GAME_RULE_MAX_FOURTY_EIGHT)) {
					if (temp > 48)
						temp = 48;
				}
				if (temp <= 3)
					this._can_tuizhu_player[seat_index] = 0;
				else
					this._can_tuizhu_player[seat_index] = temp;
			} else if (GRR._game_score[seat_index] < 0) {
				this._can_tuizhu_player[seat_index] = 0;
			}

		}

	}

	public void add_jetton_hjk(int seat_index) {
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			// 回放数据
			AddJetton_HJK.Builder add_jetton = AddJetton_HJK.newBuilder();
			add_jetton.setSeatIndex(seat_index);
			add_jetton.setJettonScore(_add_Jetton[seat_index]);
			roomResponse_hjk.setAddJetton(add_jetton);
			roomResponse.setType(MsgConstants.RESPONSE_ADD_JETTON);
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		// 回放数据
		AddJetton_HJK.Builder add_jetton = AddJetton_HJK.newBuilder();
		add_jetton.setSeatIndex(seat_index);
		add_jetton.setJettonScore(_add_Jetton[seat_index]);
		roomResponse_hjk.setAddJetton(add_jetton);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_JETTON);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);

	}

	public void send_card_date_hjk() {

		// 游戏开始
		this._game_status = GameConstants.GS_HJK_YAO_CARD;// 设置状态

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			SendCard_HJK.Builder send_card = SendCard_HJK.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_HJK; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GRR._card_count[k]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					if (k == i) {
						for (int j = 0; j < GRR._card_count[k]; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}

						if (k == i)
							for (int j = 0; j < GRR._card_value_count[k]; j++) {
								card_value.addItem(GRR._card_value[k][j]);
							}

					} else {
						for (int j = 0; j < GRR._card_count[k]; j++) {
							if (j < GameConstants.FRIST_DISPATCH_COUNT)
								cards.addItem(GameConstants.BLACK_CARD);
							else
								cards.addItem(GRR._cards_data[k][j]);

						}
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
				send_card.addCardValue(k, card_value);
				send_card.addOpenCard(this._open_card[k]);
				send_card.addScore(this._temp_score[k]);
			}
			roomResponse_hjk.setSendCard(send_card);

			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this._player_status[i] != true)
				continue;
			SendCard_HJK.Builder send_card = SendCard_HJK.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_HJK; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GRR._card_count[k]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				}
				if (this._player_status[k] == true) {
					for (int j = 0; j < GRR._card_count[k]; j++) {
						cards.addItem(GRR._cards_data[k][j]);

					}

					for (int j = 0; j < GRR._card_value_count[k]; j++) {
						card_value.addItem(GRR._card_value[k][j]);
					}

				}

				send_card.addSendCard(k, cards);
				send_card.addOpenCard(this._open_card[k]);
				send_card.addCardValue(k, card_value);
				send_card.addScore(this._temp_score[k]);
			}
			roomResponse_hjk.setSendCard(send_card);
		}
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_dispath_card;
		_handler_dispath_card.reset_status(this._game_status);

		return;
	}

	public void open_all_card_hjk(boolean open_card[]) {

		// 游戏开始
		this._game_status = GameConstants.GS_HJK_YAO_CARD;// 设置状态

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			SendCard_HJK.Builder send_card = SendCard_HJK.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_HJK; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GRR._card_count[k]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					if (this._open_card[k] == true) {
						for (int j = 0; j < GRR._card_count[k]; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					}

				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
				send_card.addOpenCard(open_card[k]);
				send_card.addCardType(this._card_type[k]);
				send_card.addScore(this._temp_score[k]);

			}
			roomResponse_hjk.setSendCard(send_card);

			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_OPEN_ALL_CARD);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_OPEN_ALL_CARD);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this._player_status[i] != true)
				continue;
			SendCard_HJK.Builder send_card = SendCard_HJK.newBuilder();
			for (int k = 0; k < GameConstants.GAME_PLAYER_HJK; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < GRR._card_count[k]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);

					}

				} else {
					if (this._open_card[k] == true) {
						for (int j = 0; j < GRR._card_count[k]; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}

					}

				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
				send_card.addOpenCard(open_card[k]);
				send_card.addCardType(this._card_type[k]);
				send_card.addScore(this._temp_score[k]);

			}

			roomResponse_hjk.setSendCard(send_card);
		}
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_dispath_card;
		_handler_dispath_card.reset_status(this._game_status);

		return;
	}

	public void yao_card_date_hjk(int seat_index, int send_card_data) {
		// 游戏开始 this._game_status = GameConstants.GS_HJK_YAO_CARD;// 设置状态

		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {

			if (this.get_players()[i] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			CardValue_HJK.Builder card_value_hjk = CardValue_HJK.newBuilder();
			YaoCard_HJK.Builder yao_card = YaoCard_HJK.newBuilder();
			yao_card.setSeatIndex(seat_index);
			yao_card.setSendCard(send_card_data);
			roomResponse_hjk.setYaoCard(yao_card);
			if (seat_index == i || seat_index == this._cur_banker) {
				Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
				if (seat_index != this._cur_banker)
					for (int j = 0; j < GRR._card_value_count[i]; j++) {
						card_value.addItem(GRR._card_value[i][j]);
					}
				else
					for (int j = 0; j < GRR._card_value_count[seat_index]; j++) {
						card_value.addItem(GRR._card_value[seat_index][j]);
					}
				card_value_hjk.addCardValue(card_value);
				roomResponse_hjk.setCardValue(card_value_hjk);
			}
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this._player_status[i] != true)
				continue;

			CardValue_HJK.Builder card_value_hjk = CardValue_HJK.newBuilder();
			YaoCard_HJK.Builder yao_card = YaoCard_HJK.newBuilder();
			yao_card.setSeatIndex(seat_index);
			yao_card.setSendCard(send_card_data);
			roomResponse_hjk.setYaoCard(yao_card);
			if (seat_index == i || seat_index == this._cur_banker) {
				Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
				if (seat_index != this._cur_banker)
					for (int j = 0; j < GRR._card_value_count[i]; j++) {
						card_value.addItem(GRR._card_value[i][j]);
					}
				else
					for (int j = 0; j < GRR._card_value_count[seat_index]; j++) {
						card_value.addItem(GRR._card_value[seat_index][j]);
					}
				card_value_hjk.addCardValue(card_value);
				roomResponse_hjk.setCardValue(card_value_hjk);
			}

		}

		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_dispath_card;
		_handler_dispath_card.reset_status(this._game_status);

		return;
	}

	public void operate_card_date_hjk() {
		// 游戏开始
		this._game_status = GameConstants.GS_HJK_YAO_CARD;// 设置状态
		int seat_index[] = new int[GameConstants.GAME_PLAYER_HJK];
		Arrays.fill(seat_index, -1);
		ButtonPop_HJK.Builder button_pop = ButtonPop_HJK.newBuilder();
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			PlayerStatus tempPlayerStatus = _playerStatus[i];
			if (tempPlayerStatus._action_count > 0)
				button_pop.addSeatIndex(i);
		}
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			if (this._player_status[i] == false)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();

			PlayerStatus curPlayerStatus = _playerStatus[i];
			button_pop.clearActions();
			for (int j = 0; j < curPlayerStatus._action_count; j++) {
				button_pop.addActions(curPlayerStatus._action[j]);

			}

			roomResponse_hjk.setButtonPop(button_pop);
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_OPERATE_RESULT);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_OPERATE_RESULT);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this._player_status[i] != true)
				continue;
			button_pop.clearActions();
			PlayerStatus curPlayerStatus = _playerStatus[i];
			for (int j = 0; j < curPlayerStatus._action_count; j++) {
				button_pop.addActions(curPlayerStatus._action[j]);

			}
			roomResponse_hjk.setButtonPop(button_pop);

		}
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_dispath_card;
		_handler_dispath_card.reset_status(this._game_status);

		return;
	}

	public void operate_tou_xiang_operate(int seat_index, int is_tou_xiang) {
		// 游戏开始
		this._game_status = GameConstants.GS_HJK_YAO_CARD;// 设置状态
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			if (this._player_status[i] == false)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			TouXiang_HJK.Builder tou_xiang = TouXiang_HJK.newBuilder();

			tou_xiang.setSeatIndex(seat_index);
			tou_xiang.setIsTouXiang(is_tou_xiang);
			roomResponse_hjk.setTouXiang(tou_xiang);
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			roomResponse.setType(MsgConstants.RESPONSE_TOU_XIANG_OPERATE);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_TOU_XIANG_OPERATE);
		// 发送数据
		TouXiang_HJK.Builder tou_xiang = TouXiang_HJK.newBuilder();

		tou_xiang.setSeatIndex(seat_index);
		tou_xiang.setIsTouXiang(is_tou_xiang);
		roomResponse_hjk.setTouXiang(tou_xiang);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_dispath_card;
		_handler_dispath_card.reset_status(this._game_status);

		return;
	}

	public void open_card_hjk(int seat_index, boolean open_banker_card) {
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
			// 回放数据
			this.GRR._video_recode.addHandCards(cards);
			for (int j = 0; j < GRR._card_count[seat_index]; j++) {
				cards.addItem(GRR._cards_data[seat_index][j]);
			}
			SendCard_HJK.Builder sendCard = SendCard_HJK.newBuilder();
			sendCard.addSendCard(cards);
			roomResponse_hjk.setSendCard(sendCard);
			OpenCard_HJK.Builder open_card = OpenCard_HJK.newBuilder();
			open_card.setSeatIndex(seat_index);
			open_card.setOpen(true);

			open_card.setCardType(this._card_type[seat_index]);
			if (open_banker_card == true) {
				if (this._card_type[seat_index] == 0) {
					int sum[] = new int[3];
					this._card_type[seat_index] = _logic.get_card_sum(GRR._cards_data[seat_index], GRR._card_count[seat_index], sum);
				}
				open_card.setBankerScore(this._temp_score[this._cur_banker]);
				open_card.setIdleScore(this._temp_score[seat_index]);
			} else {
				open_card.setBankerScore(-1);
				open_card.setIdleScore(-1);
				CardValue_HJK.Builder card_value_hjk = CardValue_HJK.newBuilder();
				if (seat_index == this._cur_banker) {
					Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_value_count[this._cur_banker]; j++) {
						card_value.addItem(GRR._card_value[this._cur_banker][j]);
					}
					card_value_hjk.addCardValue(card_value);
					roomResponse_hjk.setCardValue(card_value_hjk);
				}
			}

			roomResponse_hjk.setOpenCard(open_card);
			this.load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);
			roomResponse.setRoomResponseHjk(roomResponse_hjk);
			this.send_response_to_player(i, roomResponse);
		}
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		for (int j = 0; j < GameConstants.MAX_COUNT_HJK; j++) {
			cards.addItem(GRR._cards_data[seat_index][j]);
		}
		SendCard_HJK.Builder sendCard = SendCard_HJK.newBuilder();
		sendCard.addSendCard(cards);
		roomResponse_hjk.setSendCard(sendCard);
		OpenCard_HJK.Builder open_card = OpenCard_HJK.newBuilder();
		open_card.setSeatIndex(seat_index);
		open_card.setOpen(true);
		open_card.setCardType(this._card_type[seat_index]);
		if (open_banker_card == true) {
			if (this._card_type[seat_index] == 0) {
				int sum[] = new int[3];
				this._card_type[seat_index] = _logic.get_card_sum(GRR._cards_data[seat_index], GRR._card_count[seat_index], sum);
			}
			open_card.setBankerScore(this._temp_score[this._cur_banker]);
			open_card.setIdleScore(this._temp_score[seat_index]);
		} else {
			open_card.setBankerScore(-1);
			open_card.setIdleScore(-1);
			CardValue_HJK.Builder card_value_hjk = CardValue_HJK.newBuilder();
			if (seat_index == this._cur_banker) {
				Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_value_count[this._cur_banker]; j++) {
					card_value.addItem(GRR._card_value[this._cur_banker][j]);
				}
				card_value_hjk.addCardValue(card_value);
				roomResponse_hjk.setCardValue(card_value_hjk);
			}
		}
		roomResponse_hjk.setOpenCard(open_card);
		this.load_player_info_data(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		this.GRR.add_room_response(roomResponse);
		////////////////////////////////////////////////// 回放
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);

	}

	private void countCardType(int card_type, int seat_index) {
		try {
			if (card_type == GameConstants.HJK_CARD_TYPE_AA) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hjkaa, "", 0, 0l, this.getRoom_id());
			}

			if (card_type == GameConstants.HJK_CARD_TYPE_HJK) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hjkhjk, "", 0, 0l, this.getRoom_id());
			}

			if (card_type == GameConstants.HJK_CARD_TYPE_SEVEN) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hjk777, "", 0, 0l, this.getRoom_id());
			}

			if (card_type == GameConstants.HJK_CARD_TYPE_WUXIAOLONG) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hjkwuxiaolong, "", 0, 0l, this.getRoom_id());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > GameConstants.GAME_PLAYER_HJK)
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

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			for (int j = 0; j < GameConstants.FRIST_DISPATCH_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * GameConstants.FRIST_DISPATCH_COUNT + j];
			}
			GRR._card_count[i] += GameConstants.FRIST_DISPATCH_COUNT;
			GRR._left_card_count -= GameConstants.FRIST_DISPATCH_COUNT;
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
		// int cards[] = new int[] {0x02,0x12};
		// int cards1[] = new int[] {0x01,0x0c};
		// int cards2[] = new int[] {0x21,0x22};
		// int cards3[] = new int[] {0x16,0x2D};
		// int cards4[] = new int[] {0x2C,0x13};
		//// int cards5[] = new int[] {0x2C,0x11,0x33,0x2a,0x18};
		//////// int cards2[] = new int[]
		// {0x12,0x12,0x11,0x13,0x13,0x03,0x03,0x02,0x07,0x0a,0x1a,0x04,0x05,0x06,0x14,
		//////// 0x06,0x02,0x17,0x1a,0x19};
		//////// //int cards[] = new int[]{011,011,011};
		//////// // int cards2[] = new int[] { 0x12, 0x12, 0x12, 0x12, 0x01,
		// 0x02, 0x04, 0x03, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17,
		//////// // 0x17, 0x15, 0x19, 0x17,0x17,0x13 };
		//////// // int cards3[] = new int[] { 0x13, 0x13, 0x14, 0x14, 0x01,
		// 0x02, 0x04, 0x03, 0x05, 0x06, 0x15, 0x15, 0x16, 0x17,
		//////// // 0x17, 0x15, 0x19, 0x18,0x18,0x16 };
		////////
		//////// for (int i = 0; i < GameConstants.GAME_PLAYER_HH; i++) {
		//////// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		//////// GRR._cards_data[i][j] = 0;
		//////// }
		//////// }
		////////// int send_count = (GameConstants.MAX_HH_COUNT );
		//////////
		//////// _repertory_card[_all_card_len - GRR._left_card_count] = 0x13;
		// for (int j = 0; j < GameConstants.FRIST_DISPATCH_COUNT; j++) {
		// GRR._cards_data[0][j] = 0;
		// GRR._cards_data[1][j] = 0;
		// GRR._cards_data[2][j] = 0;
		// GRR._cards_data[3][j] = 0;
		// GRR._cards_data[4][j] = 0;
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

		// /*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可
		// ***********************/
		// int[] realyCards = new int[]
		// {0x23,0x05,0x06,0x09,0x27,0x02,0x11,0x21,0x36,0x04,
		// 0x17,0x29,0x1b,0x35,0x15,0x22,0x18,0x21,0x36,0x04,
		// 5, 17, 58, 33, 10, 54, 19, 4, 37, 51, 39, 237, 43, 52, 9, 23, 3, 18,
		// 20, 2, 53, 40};
		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > GameConstants.FRIST_DISPATCH_COUNT) {
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
		int count = GameConstants.GAME_PLAYER_HJK;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < GRR._card_count[i]; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;
			for (; i < count; i++) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == count)
				break;
		}
		k = 0;
		while (k < realyCards.length) {
			_repertory_card[k] = realyCards[k];
			k++;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			for (int j = 0; j < GameConstants.FRIST_DISPATCH_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {

			for (int j = 0; j < GameConstants.FRIST_DISPATCH_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HJK)) {
			return this.handler_game_finish_hjk(seat_index, reason);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return false;
	}

	public int lun_banker(int cur_banker) {
		int next_banker = -1;
		for (int i = 1; i < GameConstants.GAME_PLAYER_HJK; i++) {
			next_banker = (cur_banker + i) % GameConstants.GAME_PLAYER_HJK;
			if (_player_status[next_banker] == false) {
				continue;
			}
			if (next_banker == this._cur_banker)
				continue;
			break;
		}
		return next_banker;
	}

	public boolean handler_game_finish_hjk(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER_HJK;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
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
			int cards[] = new int[10];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len - GRR._left_card_count + 10; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

			// 特别显示的牌

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
				Int32ArrayResponse.Builder cards_type = Int32ArrayResponse.newBuilder();
				if (this._player_status[i] == true)
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_type.addItem(GRR._cards_data[i][j]);
					}
				game_end.addCardsData(i, cards_type);
				game_end.addJettonScore(this._add_Jetton[i]);
				game_end.addCardType(this._card_type[i]);
				game_end.addGameScore(GRR._game_score[i]);
			}

		}
		this._last_banker = this._cur_banker;
		if (has_rule(GameConstants.GAME_RULE_LUN_BNAKER)) {

			this._cur_banker = lun_banker(this._cur_banker);
		}
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if ((_cur_round >= _game_round)) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
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
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER_HJK; j++) {
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
	 * 是否能进入牌桌 ，默认中途不可进需要修改的table自己继承
	 * 
	 * @param player
	 * @return
	 */
	protected boolean canEnter(Player player) {
		if (has_rule(GameConstants.GAME_RULE_MID_JOIN) == false) {
			boolean flag = false;
			for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
				if (this._player_status[i] == true)
					flag = true;
			}
			if (flag == true) {
				send_error_notify(player, 2, "该房间已经禁止其它玩家在游戏中进入");
				return false;
			}

		}

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

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
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
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			// if (_player_ready[i] == 0) {
			// this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE,true);
			//// return false;
			// }
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}

		if ((_player_count >= GameConstants.MIN_PLAYER_HJK_COUNT) && (_player_count == _cur_count))
			handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
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
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
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
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		if (this._handler != null) {
			this._handler.handler_operate_button(this, seat_index, operate_code);
		}

		return true;
	}

	public void quest_operate_button(int seat_index, int operate_code) {
		this._handler = this._handler_quest;
		if (this._handler != null) {
			this._handler.handler_operate_button(this, seat_index, operate_code);
		}

	}

	public void dispatch_operate_button(int seat_index, int operate_code) {
		this._handler = this._handler_dispath_card;
		if (this._handler != null) {
			this._handler.handler_operate_button(this, seat_index, operate_code);
		}

	}

	public void opencard_operate_button(int seat_index, int operate_code) {
		this._handler = this._handler_open_card;
		if (this._handler != null) {
			this._handler.handler_operate_button(this, seat_index, operate_code);
		}

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
			for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	public void countChiHuTimes(int _seat_index, boolean isZimo) {

		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {

			if (this._card_type[i] == GameConstants.HJK_CARD_TYPE_WUXIAOLONG)
				_player_result.hjk_wu_xiao_long[i] += 1;
			if (this._card_type[i] == GameConstants.HJK_CARD_TYPE_AA)
				_player_result.hjk_aa[i] += 1;
			if (this._card_type[i] == GameConstants.HJK_CARD_TYPE_SEVEN)
				_player_result.hjk_seven[i] += 1;
			if (this._card_type[i] == GameConstants.HJK_CARD_TYPE_HJK)
				_player_result.hjk_hjk[i] += 1;
		}

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
			playerNumber = GameConstants.GAME_PLAYER_HJK;
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
			if (get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id())
				this._own_room_seat = -1;
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
			for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					_cur_count += 1;
				}
				if (_player_ready[i] == 0) {
					// this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE,true);
					// return false;
				}
				if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
					player_count += 1;
				}
			}

			if ((player_count >= GameConstants.MIN_PLAYER_HJK_COUNT) && (_player_count == _cur_count))
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

			for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
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
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {

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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

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
			count = GameConstants.GAME_PLAYER_HJK;
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

			player_result.addSevenCount(_player_result.hjk_seven[i]);
			player_result.addAACount(_player_result.hjk_aa[i]);
			player_result.addWuXiaoLongCount(_player_result.hjk_wu_xiao_long[i]);
			player_result.addHjkCount(_player_result.hjk_hjk[i]);

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

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
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
			roomResponse.addPlayers(room_player);
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
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
		return GameConstants.GAME_PLAYER_HJK;
	}

	private boolean kou_dou_aa(Player cur_player, int seat_index, boolean create_room) {
		int game_id = 0;
		int game_type_index = this._game_type_index;
		int index = 0;
		game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		index = SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index);
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
				for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
					this.send_response_to_player(i, roomResponse);

					player = this.get_players()[i];
					if (player == null)
						continue;
					if (i == 0) {
						send_error_notify(i, 1, SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足,游戏解散"));
					} else {
						send_error_notify(i, 1, SysParamServerDict.getInstance().replaceGoldTipsWord("创建人闲逸豆不足,游戏解散"));
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
				send_error_notify(cur_player, 1, SysParamServerDict.getInstance().replaceGoldTipsWord("您的闲逸豆不够，不能参与游戏"));

				// 删除房间

				return false;
			}
		}

		return true;
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
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

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	public boolean open_card_timer() {
		if (this._open_card[this._cur_banker] == false) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false) {
					continue;
				}
				if (this._open_card[i] == true) {
					this.open_card_hjk(i, true);
				}
			}

		} else {
			this.open_all_card_hjk(this._open_card);

			this.countChiHuTimes(this._cur_banker, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;

			GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), this._cur_banker, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			return true;
		}
		this.operate_card_date_hjk();
		return false;
	}

	public boolean robot_banker_timer() {
		return false;
	}

	public boolean ready_timer() {
		return false;
	}

	public boolean add_jetton_timer() {
		return false;
	}

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
