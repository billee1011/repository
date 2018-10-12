package com.cai.game.universal.doubanniu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.BullFightUtil;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AddJettonRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.BankerOperateRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.LiangCardRunnable;
import com.cai.future.runnable.OpenCardRunnable;
import com.cai.future.runnable.ReadyRunnable;
import com.cai.future.runnable.RobotBankerRunnable;
import com.cai.future.runnable.TrusteeRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.universal.doubanniu.handler.DouBanNiuHandler;
import com.cai.game.universal.doubanniu.handler.DouBanNiuHandlerFinish;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.Timer_OX;
import protobuf.clazz.dbn.dbnRsp.Animation_Dbn;
import protobuf.clazz.dbn.dbnRsp.BankerResult;
import protobuf.clazz.dbn.dbnRsp.ButtonOperate;
import protobuf.clazz.dbn.dbnRsp.FirstSeat;
import protobuf.clazz.dbn.dbnRsp.GameStartDbn;
import protobuf.clazz.dbn.dbnRsp.Jetton_result;
import protobuf.clazz.dbn.dbnRsp.LiangCard_Dbn;
import protobuf.clazz.dbn.dbnRsp.OpenCard_Dbn;
import protobuf.clazz.dbn.dbnRsp.Opreate_dbn_Request;
import protobuf.clazz.dbn.dbnRsp.PukeGameEndDbn;
import protobuf.clazz.dbn.dbnRsp.RoomInfoDbn;
import protobuf.clazz.dbn.dbnRsp.RoomPlayerResponseDbn;
import protobuf.clazz.dbn.dbnRsp.SendCard_Dbn;
import protobuf.clazz.dbn.dbnRsp.TableResponseDbn;

public class DouBanNiuTable extends AbstractRoom {
	private static final long serialVersionUID = 7060061356475703643L;
	private static final int ID_TIMER_ANIMATION_CANCEl_BANKER = 1;
	private static final int ID_TIMER_ANIMATION_QIE_DALAY = 2;

	/**
	 * 通用斗板牛的子游戏ID索引，统一用4096，方便SysParamServer参数获取
	 */
	private static final int UNIVERSAL_DBN_GAME_ID = 4096;
	/**
	 * 通用斗板牛的ID索引，统一用100，方便SysParamServer参数获取
	 */
	private static final int UNIVERSAL_DBN_ID = 100;

	private static Logger logger = Logger.getLogger(DouBanNiuTable.class);

	public static boolean DEBUG_CARDS_MODE = false;

	public int _jetton_round;
	public int _cur_jetton_round;
	public int _di_fen; // 底分
	public int _jetton_current; // 当前
	public int _jetton_max; // 下注最高分
	public int _user_jetton_score[];
	public int _jetton_total_score;
	public boolean _b_round_opreate[];
	public boolean isGiveup[]; // 玩家是否放弃
	public boolean isLookCard[];// 玩家是否看牌
	public boolean isLose[]; // 玩家是否输了
	public int _user_gen_score[];

	public int _is_opreate_look[];// 是否可以操作看牌
	public int _is_opreate_give_up[];// 是否可以操作放弃
	public int _is_opreate_compare[];// 是否可以操作比牌
	public int _is_opreate_gen[];// 是否可以操作跟注
	public int _is_opreate_add[];// 是否可以操作加注

	public int is_game_start; // 游戏是否开始

	public ScheduledFuture _trustee_schedule[];// 托管定时器
	public ScheduledFuture _liang_pai_schedule;
	private ScheduledFuture _game_scheduled;

	public DouBanNiuGameLogic _logic = null;

	//
	public int _current_player = GameConstants.INVALID_SEAT;
	public int _prev_palyer = GameConstants.INVALID_SEAT;
	public int _prev_seat = GameConstants.INVALID_VALUE;
	public int _pre_opreate_type;

	public int _last_banker; // 上一轮的庄家
	public int _banker_up_score; // 上庄分数
	public int _banker_bu_score; // 补庄分数
	public int _jetton_min_score; // 最小下注分数
	public int _owner_account_id; // 房主是否上庄
	public int _cur_banker_count; // 当前庄家做庄次数
	public int _cur_banker_score; // 当前庄家做庄分数
	public int _down_min_banker_score; // 下庄分数
	public boolean _is_bu_banker[]; // 用户补庄
	public int _banker_count; // 庄家数量
	public int _banker_operate; // 庄家操作
	public int _first_player; // 先家操作用户
	public int _first_seat; // 切牌后的位置
	public int _is_first; // 是否有先家
	public int _idi_qie_card_time; // 切牌时间
	public int _idi_jetton_score_time; // 下注时间
	public int _idi_open_card_time; // 开牌时间
	public int _idi_change_banker_time; // 切换庄家
	public int _idi_ready_card_time; // 等待时间
	public int _qie_card; // 切的牌
	public int _jetton_score[]; // 下注按钮
	public int _jetton_count; // 下注按钮个数
	public int _jetton_player[]; // 用户下注
	public boolean _open_card_player[]; // 用户开牌
	public boolean _liang_card_player[]; // 用户亮牌
	public int _player_times[]; // 用户倍数
	public int _ox_value[]; // 牛值
	public int _tong_sha_count[]; // 通杀次数
	public int _tong_pei_count[]; // 通赔次数
	public int _niu_niu_count[]; // 牛牛次数
	public int _no_niu_count[]; // 无牛次数
	public int _victory_count[]; // 胜利的次数
	public boolean is_calculate; // 是否结算
	public int _banker_ya_score; // 庄家压的分数
	public int _liang_card_sort[]; // 这牌顺序
	public int _end_score[]; // 每局结束分数

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public DouBanNiuHandler _handler;
	public int _operate_start_time; // 操作开始时间
	public int _cur_operate_time; // 可操作时间
	public int _cur_game_timer; // 当前游戏定时器
	public int _trustee_type[];// 托管的内容
	public boolean _wait_cancel_trustee[];// 取消托管
	public int _max_round;
	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public DouBanNiuHandlerFinish _handler_finish; // 结束

	public DouBanNiuTable() {
		super(RoomType.HH, 6);

		_logic = new DouBanNiuGameLogic();
		_jetton_round = 0;
		_max_round = 0;
		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		_user_jetton_score = new int[getTablePlayerNumber()];
		_b_round_opreate = new boolean[getTablePlayerNumber()];
		isGiveup = new boolean[getTablePlayerNumber()];
		isLookCard = new boolean[getTablePlayerNumber()];
		_user_gen_score = new int[getTablePlayerNumber()];
		isLose = new boolean[getTablePlayerNumber()];
		_is_opreate_look = new int[getTablePlayerNumber()];
		_is_opreate_give_up = new int[getTablePlayerNumber()];
		_is_opreate_compare = new int[getTablePlayerNumber()];
		_is_opreate_gen = new int[getTablePlayerNumber()];
		_is_opreate_add = new int[getTablePlayerNumber()];
		Arrays.fill(_is_opreate_look, 0);
		Arrays.fill(_is_opreate_give_up, 0);
		Arrays.fill(_is_opreate_compare, 0);
		Arrays.fill(_is_opreate_gen, 0);
		Arrays.fill(_is_opreate_add, 0);
		_jetton_total_score = 0;

		is_calculate = true;
		_cur_banker = 0;
		_last_banker = 0;
		_banker_up_score = 0;
		_banker_bu_score = 0;
		_jetton_min_score = 0;
		_owner_account_id = -1;
		_cur_banker_score = 0;
		_down_min_banker_score = 10;
		_first_player = -1;
		_first_seat = -1;
		_banker_count = 0;
		_idi_qie_card_time = 5; // 切牌时间
		_idi_jetton_score_time = 5; // 下注时间
		_idi_open_card_time = 20; // 开牌时间
		_idi_change_banker_time = 5; // 庄家操作
		_idi_ready_card_time = 10; // 等待时间
		_banker_operate = 0;
		_is_first = -1;
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_jetton_count = 0;
		_banker_ya_score = 0;
		_is_bu_banker = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[4];
		_jetton_player = new int[getTablePlayerNumber()]; // 用户下注
		_open_card_player = new boolean[getTablePlayerNumber()]; // 用户开牌
		_liang_card_player = new boolean[getTablePlayerNumber()]; // 用户亮牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_is_bu_banker, false);
		Arrays.fill(_jetton_score, 0);
		Arrays.fill(_jetton_player, 0);
		Arrays.fill(_open_card_player, false);
		Arrays.fill(_liang_card_player, false);
		_tong_sha_count = new int[this.getTablePlayerNumber()]; // 通杀次数
		Arrays.fill(_tong_sha_count, 0);
		_tong_pei_count = new int[this.getTablePlayerNumber()]; // 通赔次数
		Arrays.fill(_tong_pei_count, 0);
		_niu_niu_count = new int[this.getTablePlayerNumber()]; // 牛牛次数
		Arrays.fill(_niu_niu_count, 0);
		_no_niu_count = new int[this.getTablePlayerNumber()]; // 无牛次数
		Arrays.fill(_no_niu_count, 0);
		_victory_count = new int[this.getTablePlayerNumber()]; // 胜利的次数
		Arrays.fill(_victory_count, 0);
		_trustee_type = new int[this.getTablePlayerNumber()];
		Arrays.fill(_trustee_type, 0);
		_wait_cancel_trustee = new boolean[this.getTablePlayerNumber()];// 取消托管
		Arrays.fill(_wait_cancel_trustee, false);
		istrustee = new boolean[this.getTablePlayerNumber()]; //
		Arrays.fill(istrustee, false);
		_liang_card_sort = new int[this.getTablePlayerNumber()];
		Arrays.fill(_liang_card_sort, 0);
		_end_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_end_score, 0);

		_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER_OX];
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_max_round = 0;
		_game_round = game_round;
		_max_round = game_round;
		if (has_rule(GameConstants.GAME_RULE_FOUR_PlAYER))
			_game_round = 10;
		else if (has_rule(GameConstants.GAME_RULE_FIVE_PLAYER))
			_game_round = 20;
		else if (has_rule(GameConstants.GAME_RULE_SIX_PLAYER))
			_game_round = 30;

		_cur_round = 0;
		is_game_start = 0;
		_cur_banker = 0;
		_last_banker = 0;
		_banker_up_score = 0;
		_banker_bu_score = 0;
		_jetton_min_score = 0;
		_owner_account_id = -1;
		_cur_banker_count = 0;
		_cur_banker_score = 0;
		_banker_count = 0;
		_down_min_banker_score = 10;
		_banker_operate = 0;
		_first_player = -1;
		_first_seat = -1;
		_is_first = -1;
		_idi_qie_card_time = 5; // 切牌时间
		_idi_jetton_score_time = 5; // 下注时间
		_idi_open_card_time = 20; // 开牌时间
		_idi_change_banker_time = 5; // 庄家操作
		_idi_ready_card_time = 10; // 等待时间
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_jetton_count = 0;
		_banker_ya_score = 0;
		_is_bu_banker = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[4];
		_jetton_player = new int[getTablePlayerNumber()]; // 用户下注
		_open_card_player = new boolean[getTablePlayerNumber()]; // 用户开牌
		_liang_card_player = new boolean[getTablePlayerNumber()]; // 用户亮牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_is_bu_banker, false);
		Arrays.fill(_jetton_score, 0);
		Arrays.fill(_jetton_player, 0);
		Arrays.fill(_open_card_player, false);
		Arrays.fill(_liang_card_player, false);
		_tong_sha_count = new int[this.getTablePlayerNumber()]; // 通杀次数
		Arrays.fill(_tong_sha_count, 0);
		_tong_pei_count = new int[this.getTablePlayerNumber()]; // 通赔次数
		Arrays.fill(_tong_pei_count, 0);
		_niu_niu_count = new int[this.getTablePlayerNumber()]; // 牛牛次数
		Arrays.fill(_niu_niu_count, 0);
		_no_niu_count = new int[this.getTablePlayerNumber()]; // 无牛次数
		Arrays.fill(_no_niu_count, 0);
		_victory_count = new int[this.getTablePlayerNumber()]; // 胜利的次数
		Arrays.fill(_victory_count, 0);
		_trustee_type = new int[GameConstants.GAME_PLAYER_OX];
		Arrays.fill(_trustee_type, 0);
		_wait_cancel_trustee = new boolean[GameConstants.GAME_PLAYER_OX];// 取消托管
		Arrays.fill(_wait_cancel_trustee, false);
		istrustee = new boolean[this.getTablePlayerNumber()]; //
		Arrays.fill(istrustee, false);
		_liang_card_sort = new int[this.getTablePlayerNumber()];
		Arrays.fill(_liang_card_sort, 0);
		_end_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_end_score, 0);
		is_calculate = true;
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER_OX];
		}
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_handler_finish = new DouBanNiuHandlerFinish();
		this.setMinPlayerCount(this.getTablePlayerNumber());

	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	public boolean has_trustee(int cbRule, int seat_index) {
		return FvMask.has_any(this._trustee_type[seat_index], FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////
	@Override
	public boolean reset_init_data() {

		if (_cur_round == 0) {
			record_game_room();
		}

		this._handler = null;
		SysParamModel sysParamModel4 = SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(UNIVERSAL_DBN_ID);

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.OX_MAX_CARD_COUNT,
				GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_banker_operate = 0;
		if (sysParamModel4 != null) {
			_idi_ready_card_time = sysParamModel4.getVal1(); // 等待时间
			_idi_qie_card_time = sysParamModel4.getVal2(); // 切牌时间
			_idi_jetton_score_time = sysParamModel4.getVal3(); // 下注时间
			_idi_open_card_time = sysParamModel4.getVal4(); // 开牌时间
		} else {
			_idi_ready_card_time = 10; // 等待时间
			_idi_qie_card_time = 5; // 切牌时间
			_idi_jetton_score_time = 5; // 下注时间
			_idi_open_card_time = 20; // 开牌时间
			_idi_change_banker_time = 5; // 庄家操作
		}

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.OX_MAX_CARD_COUNT,
				GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_banker_operate = 0;
		_idi_qie_card_time = 5; // 切牌时间
		_idi_jetton_score_time = 5; // 下注时间
		_idi_open_card_time = 20; // 开牌时间
		_idi_ready_card_time = 10; // 等待时间
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_first_player = -1;
		_first_seat = -1;
		_jetton_player = new int[getTablePlayerNumber()]; // 用户下注
		_open_card_player = new boolean[getTablePlayerNumber()]; // 用户开牌
		_liang_card_player = new boolean[getTablePlayerNumber()]; // 用户亮牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_jetton_player, 0);
		Arrays.fill(_open_card_player, false);
		Arrays.fill(_liang_card_player, false);
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
		}
		_cur_round++;
		_liang_card_sort = new int[this.getTablePlayerNumber()];
		Arrays.fill(_liang_card_sort, 0);
		_end_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_end_score, 0);

		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_max_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());

		Player rplayer;
		is_calculate = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:" + this._game_status);

		reset_init_data();

		_cur_jetton_round = 0;
		//
		if (BullFightUtil.isTypeDouBan(_game_type_index)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_OX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_OX);
		}

		Arrays.fill(_user_jetton_score, 0);
		Arrays.fill(_b_round_opreate, false);
		Arrays.fill(isGiveup, false);
		Arrays.fill(isLookCard, false);
		Arrays.fill(isLose, false);

		Arrays.fill(_is_opreate_look, 0);
		Arrays.fill(_is_opreate_give_up, 0);
		Arrays.fill(_is_opreate_compare, 0);
		Arrays.fill(_is_opreate_gen, 0);
		Arrays.fill(_is_opreate_add, 0);
		_jetton_total_score = 0;

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (this.has_rule(GameConstants.GAME_RULE_FOUR_PlAYER)) {
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_ONE)) {
				_banker_up_score = 40;
				_banker_bu_score = 80;
				_jetton_min_score = 5;
				_down_min_banker_score = 10;
			}
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_TWO)) {
				_banker_up_score = 80;
				_banker_bu_score = 160;
				_jetton_min_score = 10;
				_down_min_banker_score = 20;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_FIVE_PLAYER)) {
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_ONE)) {
				_banker_up_score = 50;
				_banker_bu_score = 100;
				_jetton_min_score = 5;
				_down_min_banker_score = 15;
			}
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_TWO)) {
				_banker_up_score = 100;
				_banker_bu_score = 200;
				_jetton_min_score = 10;
				_down_min_banker_score = 30;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_SIX_PLAYER)) {
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_ONE)) {
				_banker_up_score = 60;
				_banker_bu_score = 120;
				_jetton_min_score = 5;
				_down_min_banker_score = 20;
			}
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_TWO)) {
				_banker_up_score = 120;
				_banker_bu_score = 240;
				_jetton_min_score = 10;
				_down_min_banker_score = 40;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
			_is_first = 0;
		}
		_cur_banker_count++;
		if (_cur_round == 1) {
			if (this._owner_account_id != -1) {
				_cur_banker = this._owner_account_id;
				_cur_banker_count = 1;
			} else {
				int rand = (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
				_cur_banker = rand % this.getTablePlayerNumber();
				_cur_banker_count = 1;

			}
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_ONE)) {
				this._jetton_score[0] = 10;
				this._jetton_score[1] = 5;
				this._jetton_count = 2;
			}
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_TWO)) {
				this._jetton_score[0] = 20;
				this._jetton_score[1] = 10;
				this._jetton_count = 2;
			}
			_banker_count = 1;
			_cur_banker_score = _banker_up_score;
			GRR._game_score[_cur_banker] = -_cur_banker_score;
			_banker_ya_score = _banker_up_score;
			_player_result.game_score[_cur_banker] += GRR._game_score[_cur_banker];
			if (has_rule(GameConstants.GAME_RULE_THREE_JU_DOWN_BANKER)) {
				this._max_round = 3;
			}
			if (has_rule(GameConstants.GAME_RULE_FOUR_JU_DOWN_BANKER)) {
				this._max_round = 4;
			}
		}
		_last_banker = this._cur_banker;
		return game_start_dbn();
	}

	public boolean banker_info(int opr_type, int seat_index, boolean is_grr) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_UPDATA_BANKER);
		BankerResult.Builder banker_result = BankerResult.newBuilder();
		banker_result.setOprType(opr_type);
		banker_result.setCurBankerCount(_banker_count);
		if ((opr_type & GameConstants.BANKER_BU) == 1)
			banker_result.setBuBankerScore(this._banker_bu_score);
		banker_result.setTableDiFen(this._cur_banker_score);
		banker_result.setAllBankerCount(this.getTablePlayerNumber());
		banker_result.setBankerCurRound(this._cur_banker_count);
		banker_result.setCurBanker(_cur_banker);
		roomResponse.setCommResponse(PBUtil.toByteString(banker_result));

		if (is_grr == false) {
			this.send_response_to_player(seat_index, roomResponse);

		} else {
			GRR.add_room_response(roomResponse);

			this.send_response_to_room(roomResponse);
		}

		return true;
	}

	// 斗板牛开始
	public boolean game_start_dbn() {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		// 刷新手牌
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			if (this.get_players()[index] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_DBN_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			GameStartDbn.Builder gamestart = GameStartDbn.newBuilder();
			RoomInfoDbn.Builder room_info = getRoomInfoDbn();
			gamestart.setRoomInfo(room_info);
			this.load_player_info_data_game_start(gamestart);
			gamestart.setTableDiFen(_cur_banker_score);

			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
			GRR.add_room_response(roomResponse);

			this.send_response_to_player(index, roomResponse);
		}
		banker_info(0, _cur_banker, true);
		if (has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
			this._first_player = (_cur_banker + 1) % this.getTablePlayerNumber();
			_game_status = GameConstants.GS_OX_QIE_CARD;// 设置状态
			if (_is_first == 0) {
				_banker_operate = 0;
				_banker_operate = GameConstants.QIE_CARD;
				update_button(3, _first_player, this._idi_qie_card_time, true);
				if (this._game_scheduled != null)
					this.kill_timer();
				this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, this._idi_qie_card_time, true);
				if (_trustee_schedule[this._first_player] != null)
					this.set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, _idi_qie_card_time, this._first_player, true);
			}
		} else if (has_rule(GameConstants.GAME_RULE_DA_XIAO_END)) {
			_game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态
			this._current_player = -1;
			update_button(2, this._current_player, this._idi_jetton_score_time, true);
			if (this._game_scheduled != null)
				this.kill_timer();
			this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			this.set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, -1, true);

		}
		return true;
	}

	public boolean update_button(int opr_type, int seat_index, int display_time, boolean is_grr) {
		if (is_grr == false) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DBN_UPDATA_BUTTON);
			ButtonOperate.Builder button_operate = ButtonOperate.newBuilder();
			button_operate.setOprType(opr_type);
			button_operate.setDisplayTime(display_time);
			button_operate.setOperateIndex(seat_index);
			if (opr_type == 3) {
				button_operate.setOperateIndex(_first_player);
				if (_first_player == seat_index)
					button_operate.setBankerOperate(this._banker_operate);
			} else if (opr_type == 2) {
				if (has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
					button_operate.setOperateIndex(this._current_player);
					if (seat_index == this._current_player)
						for (int i = this._jetton_count - 1; i >= 0; i--)
							button_operate.addJettonScore(this._jetton_score[i]);
					button_operate.setBankerOperate(this._banker_operate);
				} else if (has_rule(GameConstants.GAME_RULE_DA_XIAO_END)) {
					button_operate.setOperateIndex(seat_index);
					for (int i = this._jetton_count - 1; i >= 0; i--)
						button_operate.addJettonScore(this._jetton_score[i]);
					button_operate.setBankerOperate(this._banker_operate);
				}

			} else
				button_operate.setBankerOperate(this._banker_operate);

			roomResponse.setCommResponse(PBUtil.toByteString(button_operate));
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_UPDATA_BUTTON);
		ButtonOperate.Builder button_operate = ButtonOperate.newBuilder();
		button_operate.setOperateIndex(seat_index);
		button_operate.setOprType(opr_type);
		button_operate.setDisplayTime(display_time);
		roomResponse.setCommResponse(PBUtil.toByteString(button_operate));
		if (seat_index != -1)
			this.send_response_to_other(seat_index, roomResponse);
		if (opr_type == 2) {
			for (int i = this._jetton_count - 1; i >= 0; i--)
				button_operate.addJettonScore(this._jetton_score[i]);
		}

		button_operate.setBankerOperate(this._banker_operate);

		roomResponse.setCommResponse(PBUtil.toByteString(button_operate));
		if (GRR != null)
			GRR.add_room_response(roomResponse);
		if (seat_index != -1)
			this.send_response_to_player(seat_index, roomResponse);
		else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				if (i == this._cur_banker)
					continue;
				if (opr_type == 2) {
					button_operate.clearJettonScore();
					for (int j = this._jetton_count - 1; j >= 0; j--)
						button_operate.addJettonScore(this._jetton_score[j]);
				}
				button_operate.setOperateIndex(i);

				roomResponse.setCommResponse(PBUtil.toByteString(button_operate));
				this.send_response_to_player(i, roomResponse);

			}
		}
		return true;
	}

	// 切牌
	public boolean result_qie_card(int seat_index) {
		if (_is_first != 0) {
			log_error("切牌不对  _is_first" + "  " + _is_first);
			return true;
		}
		if ((_banker_operate & GameConstants.QIE_CARD) == 0) {
			log_error("不是切牌模式this._banker_operate  " + this._banker_operate);
			return true;
		}

		if (_first_player != seat_index) {
			log_error("不是当前用户操作 _first_player " + this._first_player + "  " + seat_index);
			return true;
		}
		_is_first = 2;
		_qie_card = this._repertory_card[_all_card_len - GRR._left_card_count];
		_first_seat = (_cur_banker + _logic.get_real_card_value(_qie_card) - 1) % this.getTablePlayerNumber();
		if (_first_seat == _cur_banker) {
			_first_seat = (_cur_banker + 1) % this.getTablePlayerNumber();
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_QIE_CARD);
		FirstSeat.Builder first_seat = FirstSeat.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			first_seat.setCard(_qie_card);
			first_seat.setFirstSeatIdex(_first_seat);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(first_seat));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		this._current_player = _first_seat;
		_game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态
		GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_QIE_DALAY), 2700, TimeUnit.MILLISECONDS);
		return true;
	}

	public boolean result_jetton(int seat_index, int sub_index) {
		if (has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
			if (this._current_player != seat_index) {
				log_error("不是当前用户操作" + this._current_player + "  " + seat_index);
				return true;
			}
		}

		if (this._jetton_player[seat_index] != 0) {
			log_error("您已经下注了" + this._current_player);
			return true;
		}
		if (sub_index < 0 || sub_index >= this._jetton_count) {
			log_error("下注下标不对" + this._jetton_count);
			return true;
		}
		_jetton_player[seat_index] = this._jetton_score[this._jetton_count - sub_index - 1];
		send_jetton_to_room(seat_index);
		boolean flag = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == _cur_banker)
				continue;
			if (_jetton_player[i] == 0) {
				flag = true;
				break;
			}
		}
		if (flag == true && has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
			this._current_player = (this._current_player + 1) % this.getTablePlayerNumber();
			if (this._current_player == _cur_banker)
				this._current_player = (this._current_player + 1) % this.getTablePlayerNumber();

			update_button(2, this._current_player, this._idi_jetton_score_time, true);
			if (this._game_scheduled != null)
				this.kill_timer();
			this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			if (_trustee_schedule[this._current_player] != null)
				this.set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, this._current_player, true);
		} else if (flag == false) {
			if (this._game_scheduled != null)
				this.kill_timer();
			this.set_timer(GameConstants.HJK_OPEN_CARD_TIMER, _idi_open_card_time, true);
			this.set_trustee_timer(GameConstants.HJK_OPEN_CARD_TIMER, _idi_open_card_time, -1, true);
			send_card();
		}

		return true;
	}

	public void send_jetton_to_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_ADD_JETTON);
		Jetton_result.Builder jetton_result = Jetton_result.newBuilder();
		jetton_result.setJettonSeat(seat_index);
		jetton_result.setJettonScore(this._jetton_player[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(jetton_result));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
	}

	public boolean send_card() {
		_game_status = GameConstants.GS_OX_OPEN_CARD;// 设置状态
		_banker_operate = 0;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_SEND_CARD);
		SendCard_Dbn.Builder send_card = SendCard_Dbn.newBuilder();
		send_card.setDisplayTime(this._idi_open_card_time);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_card.clearSendCard();
			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (k == i) {
					for (int j = 0; j < 5; j++) {
						cards.addItem(GRR._cards_data[k][j]);

					}
				} else {
					for (int j = 0; j < 5; j++) {
						cards.addItem(GameConstants.BLACK_CARD);

					}
				}

				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);

			}
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			this.send_response_to_player(i, roomResponse);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_card.clearSendCard();
			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < 5; j++) {
					cards.addItem(GRR._cards_data[k][j]);

				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);

			}
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			GRR.add_room_response(roomResponse);
		}
		return true;
	}

	public boolean open_card(int seat_index) {

		if (this._open_card_player[seat_index] != false) {
			log_error("您已经开牌了" + seat_index);
			return true;
		}
		this._open_card_player[seat_index] = true;
		if (has_rule(GameConstants.GAME_RULE_JETTON_ONE_TIEMS))
			this._player_times[seat_index] = _logic.get_times_two(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		if (has_rule(GameConstants.GAME_RULE_JETTON_TWO_TIEMS))
			this._player_times[seat_index] = _logic.get_times_one(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		_logic.get_ox_card(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		this._ox_value[seat_index] = _logic.get_card_type(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		send_open_to_room(seat_index);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			if (_open_card_player[i] == false)
				return true;

		}

		if (has_rule(GameConstants.GAME_RULE_DA_XIAO_END)) {

			liang_pai_sort();
			this._prev_seat = 0;

		}
		this._current_player = this._cur_banker;
		liang_pai(this._current_player, 0);
		return true;
	}

	public void liang_pai_sort() {
		int index_id[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(index_id, 0);
		int user_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			index_id[user_count++] = i;
		}
		int banker_seat = -1;
		for (int i = 0; i < user_count - 1; i++) {
			for (int j = i + 1; j < user_count; j++) {
				boolean first_ox = _logic.get_ox_card(GRR._cards_data[index_id[i]], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
				boolean next_ox = _logic.get_ox_card(GRR._cards_data[index_id[j]], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
				boolean action = _logic.compare_card(GRR._cards_data[index_id[i]], GRR._cards_data[index_id[j]], GameConstants.OX_MAX_CARD_COUNT,
						first_ox, next_ox, _game_rule_index);
				if (action == false) {
					int temp = index_id[i];
					index_id[i] = index_id[j];
					index_id[j] = temp;
				}
			}
			if (index_id[i] == this._cur_banker)
				banker_seat = i;
		}
		if (index_id[user_count - 1] == this._cur_banker)
			banker_seat = user_count - 1;
		int liang_count = 0;
		for (int i = user_count - 1; i > banker_seat; i--) {
			this._liang_card_sort[liang_count++] = index_id[i];
		}
		for (int i = 0; i < banker_seat; i++) {
			this._liang_card_sort[liang_count++] = index_id[i];
		}
		this._liang_card_sort[liang_count++] = _cur_banker;
	}

	public boolean liang_pai(int seat_index, int opr_type) {
		if (_liang_pai_schedule != null) {
			_liang_pai_schedule.cancel(false);
			_liang_pai_schedule = null;
		}
		_liang_pai_schedule = GameSchedule.put(new LiangCardRunnable(getRoom_id(), opr_type), 1500, TimeUnit.MILLISECONDS);
		return true;
	}

	public boolean liang_pai_result(int opr_type) {
		if (this._liang_card_player[this._current_player] != false) {
			log_error("您已经开牌了" + this._current_player);
			return true;
		}
		this._liang_card_player[this._current_player] = true;
		int bankerScore = 0;
		int idleScore = 0;
		boolean first_ox = _logic.get_ox_card(GRR._cards_data[_cur_banker], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		boolean next_ox = _logic.get_ox_card(GRR._cards_data[this._current_player], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index);
		boolean action = _logic.compare_card(GRR._cards_data[_cur_banker], GRR._cards_data[this._current_player], GameConstants.OX_MAX_CARD_COUNT,
				first_ox, next_ox, _game_rule_index);
		if (action == true) {
			idleScore -= this._player_times[this._cur_banker] * this._jetton_player[this._current_player];
			bankerScore -= idleScore;
		} else {
			idleScore = this._player_times[this._current_player] * this._jetton_player[this._current_player];
			bankerScore -= idleScore;
		}
		if (this._cur_banker_score <= idleScore || is_calculate == false) {
			idleScore = this._cur_banker_score;
			if (this._cur_banker_score == 0)
				this._ox_value[this._current_player] = 100;
			this._cur_banker_score = 0;
			is_calculate = false;
			bankerScore = 0;
		}
		this._cur_banker_score += bankerScore;
		GRR._game_score[this._current_player] = idleScore;
		_player_result.game_score[this._current_player] += GRR._game_score[this._current_player];
		_end_score[this._current_player] = idleScore;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_LIANG_CARD);
		LiangCard_Dbn.Builder liang_card = LiangCard_Dbn.newBuilder();

		liang_card.setOprType(opr_type);
		liang_card.setSeatIndex(this._current_player);
		liang_card.setScore(idleScore);
		liang_card.setTableDiFen(this._cur_banker_score);
		liang_card.setTimes(this._player_times[this._current_player]);
		liang_card.setOxValue(this._ox_value[this._current_player]);

		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < 5; j++) {
			liang_card.addCards(GRR._cards_data[this._current_player][j]);

		}
		this.load_player_info_data_liang_pai(liang_card, this._current_player);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_card));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		if (has_rule(GameConstants.GAME_RULE_DA_XIAO_END)) {

			if (this._liang_card_sort[this._prev_seat] != _cur_banker)
				_liang_pai_schedule = GameSchedule.put(new LiangCardRunnable(getRoom_id(), 1), 1500, TimeUnit.MILLISECONDS);
			else {
				calculate_jetton();
				all_game_end_record();
				banker_operate();

			}
			this._current_player = this._liang_card_sort[this._prev_seat];
			this._prev_seat = this._prev_seat + 1;
		}
		if (has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
			if (this._current_player == this._cur_banker) {
				this._current_player = this._first_seat;
				if (_liang_pai_schedule != null) {
					_liang_pai_schedule.cancel(false);
					_liang_pai_schedule = null;
				}
				_liang_pai_schedule = GameSchedule.put(new LiangCardRunnable(getRoom_id(), 1), 1500, TimeUnit.MILLISECONDS);
				return true;
			}
			this._current_player = (this._current_player + 1) % this.getTablePlayerNumber();
			if (this._current_player == _cur_banker) {
				this._current_player = (this._current_player + 1) % this.getTablePlayerNumber();
			}
			if (this._current_player != _first_seat) {
				if (_liang_pai_schedule != null) {
					_liang_pai_schedule.cancel(false);
					_liang_pai_schedule = null;
				}
				_liang_pai_schedule = GameSchedule.put(new LiangCardRunnable(getRoom_id(), 1), 1500, TimeUnit.MILLISECONDS);

			} else {
				calculate_jetton();
				all_game_end_record();
				banker_operate();

			}

		}

		return true;

	}

	public boolean set_trustee_timer(int timer_type, int time, int seat_index, boolean makeDBtimer) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER_OX];
		}
		if (timer_type == GameConstants.HJK_READY_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_BANKER_OPERATE) {
			time = 2;
		}
		if (makeDBtimer == false)
			time = 0;
		if (seat_index == -1) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (istrustee[i] == true) {
					if (_trustee_schedule[i] != null) {

						_trustee_schedule[i].cancel(false);
						_trustee_schedule[i] = null;
						_trustee_schedule[i] = GameSchedule.put(new TrusteeRunnable(getRoom_id(), timer_type, i), time, TimeUnit.SECONDS);
						_operate_start_time = (int) (System.currentTimeMillis() / 1000L);

					} else {
						_trustee_schedule[i] = GameSchedule.put(new TrusteeRunnable(getRoom_id(), timer_type, i), time, TimeUnit.SECONDS);
						_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
					}
				} else {
					if (_trustee_schedule[i] != null) {
						_trustee_schedule[i].cancel(false);
						_trustee_schedule[i] = null;
					}

				}
			}
		} else {
			if (istrustee[seat_index] == true) {
				if (_trustee_schedule[seat_index] != null) {

					_trustee_schedule[seat_index].cancel(false);
					_trustee_schedule[seat_index] = null;
					_trustee_schedule[seat_index] = GameSchedule.put(new TrusteeRunnable(getRoom_id(), timer_type, seat_index), time,
							TimeUnit.SECONDS);
					_operate_start_time = (int) (System.currentTimeMillis() / 1000L);

				} else {
					_trustee_schedule[seat_index] = GameSchedule.put(new TrusteeRunnable(getRoom_id(), timer_type, seat_index), time,
							TimeUnit.SECONDS);
					_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				}
			} else {
				if (_trustee_schedule[seat_index] != null) {
					_trustee_schedule[seat_index].cancel(false);
					_trustee_schedule[seat_index] = null;
				}

			}
		}

		return true;
	}

	public boolean set_timer(int timer_type, int time, boolean makeDBtimer) {
		// if (!is_sys())
		// return false;
		_cur_game_timer = timer_type;
		SysParamModel sysParamModel4 = SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(UNIVERSAL_DBN_GAME_ID)
				.get(UNIVERSAL_DBN_ID);

		if (sysParamModel4 != null && sysParamModel4.getVal5() == 0)
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
			_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), sysParamModel4.getVal1(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel4.getVal1();
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), sysParamModel4.getVal2(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel4.getVal2();
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), sysParamModel4.getVal3(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel4.getVal3();
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), sysParamModel4.getVal4(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel4.getVal4();
		} else if (timer_type == GameConstants.HJK_BANKER_OPERATE) {
			_game_scheduled = GameSchedule.put(new BankerOperateRunnable(getRoom_id()), sysParamModel4.getVal1() / 2, TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel4.getVal1() / 2;
		}
		return true;

	}

	public void all_game_end_record() {
		int user_count = 0;
		int lose_count = 0;
		int win_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			user_count++;
			if (this._ox_value[i] >= 10) {
				_niu_niu_count[i]++; // 牛牛次数
			} else if (this._ox_value[i] == 0) {
				_no_niu_count[i]++;
			}
			if (GRR._game_score[i] > 0) {
				_victory_count[i]++;
				if (i != _cur_banker)
					win_count++;
			} else if (GRR._game_score[i] < 0) {
				if (i != _cur_banker)
					lose_count++;
			}

		}
		if (user_count == win_count + 1) {
			_tong_pei_count[_cur_banker]++;
			animation(1);

		}
		if (user_count == lose_count + 1) {

			_tong_sha_count[_cur_banker]++;
			animation(0);

		}

	}

	public boolean animation(int opr_type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_ANIMATION);
		Animation_Dbn.Builder animation_dbn = Animation_Dbn.newBuilder();
		animation_dbn.setOprType(opr_type);
		roomResponse.setCommResponse(PBUtil.toByteString(animation_dbn));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	public void calculate_jetton() {
		int max_score = 0;
		if (this._cur_banker_score <= _down_min_banker_score) {
			return;
		}

		max_score = _cur_banker_score / 2;

		if (has_rule(GameConstants.GAME_RULE_1000_TIAO_BANKER) || has_rule(GameConstants.GAME_RULE_2000_TIAO_BANKER)) {
			if (has_rule(GameConstants.GAME_RULE_JETTON_SCORE_ONE)) {
				if (max_score > 60)
					max_score = 60;
			}
			if (has_rule(GameConstants.GAME_RULE_JETTON_SCORE_TWO)) {
				max_score = _cur_banker_score / 2;
				if (max_score < 140 / 2 && max_score > 120 / 2) {
					max_score = 60;
				}
			}
			if (has_rule(GameConstants.GAME_RULE_JETTON_SCORE_THREE)) {
				max_score = _cur_banker_score / 3;
				if (max_score < 210 / 2 && max_score > 180 / 2) {
					max_score = 60;
				}
			}
		}
		this._jetton_count = 0;
		do {

			if (max_score % 10 == 0)
				this._jetton_score[this._jetton_count++] = max_score;
			else if (max_score % 5 == 0)
				this._jetton_score[this._jetton_count++] = max_score;
			else if (max_score % 10 > 5)
				this._jetton_score[this._jetton_count++] = ((max_score / 10) + 1) * 10;
			else if (max_score % 10 < 5)
				this._jetton_score[this._jetton_count++] = (max_score / 10) * 10;
			if (this._jetton_count == 4)
				if (this._jetton_score[this._jetton_count - 1] > 5) {
					this._jetton_score[this._jetton_count - 1] = 5;
				}
			max_score = this._jetton_score[this._jetton_count - 1];
			max_score /= 2;
			if (max_score < 5)
				break;

		} while (this._jetton_count < 4);
	}

	public boolean banker_operate() {
		_banker_operate = 0;
		if (has_rule(GameConstants.GAME_RULE_1000_TIAO_BANKER)) {
			if (_cur_banker_score > 1000) {
				GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
				return true;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_2000_TIAO_BANKER)) {
			if (_cur_banker_score > 2000) {
				GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
				return true;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_THREE_JU_DOWN_BANKER)) {

			if (_cur_banker_score <= _down_min_banker_score && _cur_banker_count <= 3) {
				_banker_operate |= GameConstants.BANKER_CANCEL;
				if (has_rule(GameConstants.GAME_RULE_BU_BANKER) && this._is_bu_banker[_cur_banker] == false) {
					_banker_operate |= GameConstants.BANKER_BU;
				} else {
					GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
					return true;
				}
			} else if (_cur_banker_score <= _down_min_banker_score && _cur_banker_count > 3) {
				GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
				return true;
			}
			if (_cur_banker_score > _down_min_banker_score && _cur_banker_count >= 3) {
				_banker_operate |= GameConstants.BANKER_CANCEL;
				if (has_rule(GameConstants.GAME_RULE_LIAN_BANKER)) {
					_banker_operate |= GameConstants.BANKER_CONTINUE;
				} else {
					GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
					return true;
				}
			}

		}
		if (has_rule(GameConstants.GAME_RULE_FOUR_JU_DOWN_BANKER)) {
			if (_cur_banker_score <= _down_min_banker_score && _cur_banker_count <= 4) {
				_banker_operate |= GameConstants.BANKER_CANCEL;
				if (has_rule(GameConstants.GAME_RULE_BU_BANKER) && this._is_bu_banker[_cur_banker] == false) {
					_banker_operate |= GameConstants.BANKER_BU;
				} else {
					GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
					return true;
				}
			} else if (_cur_banker_score <= _down_min_banker_score && _cur_banker_count > 4) {
				GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
			}
			if (_cur_banker_score > _down_min_banker_score && _cur_banker_count >= 4) {
				_banker_operate |= GameConstants.BANKER_CANCEL;
				if (has_rule(GameConstants.GAME_RULE_LIAN_BANKER)) {
					_banker_operate |= GameConstants.BANKER_CONTINUE;
				} else {
					GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_CANCEl_BANKER), 2500, TimeUnit.MILLISECONDS);
					return true;
				}
			}
		}
		if (_banker_operate > GameConstants.BANKER_CANCEL) {
			if (this._game_scheduled != null)
				this.kill_timer();
			this.set_timer(GameConstants.HJK_BANKER_OPERATE, _idi_change_banker_time, true);
			if (_trustee_schedule[_cur_banker] != null)
				this.set_trustee_timer(GameConstants.HJK_BANKER_OPERATE, _idi_change_banker_time, _cur_banker, true);
			return update_button(1, this._cur_banker, this._idi_ready_card_time, true);
		}
		if (_banker_operate <= GameConstants.BANKER_CANCEL) {

			this.handler_game_finish(this._cur_banker, GameConstants.Game_End_NORMAL);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				update_button(4, i, this._idi_ready_card_time, true);
			}
		}
		return true;
	}

	public boolean add_bu_banker() {
		if (GRR == null)
			return false;
		if (this._is_bu_banker[_cur_banker] == true) {
			this.log_error("您已经补庄过了，不能再补庄" + _cur_banker);
		}
		if (!has_rule(GameConstants.GAME_RULE_BU_BANKER)) {
			this.log_error("该房间没有选择补庄操作" + _cur_banker);
		}
		_banker_operate = 0;
		GRR._game_score[_cur_banker] = -this._banker_bu_score;
		this._banker_ya_score += _banker_bu_score;
		_player_result.game_score[_cur_banker] += GRR._game_score[_cur_banker];
		this._cur_banker_score += this._banker_bu_score;
		this._is_bu_banker[_cur_banker] = true;
		if (has_rule(GameConstants.GAME_RULE_THREE_JU_DOWN_BANKER)) {
			if (this._cur_banker_count >= 3)
				this._max_round++;
		}
		if (has_rule(GameConstants.GAME_RULE_FOUR_JU_DOWN_BANKER)) {
			if (this._cur_banker_count >= 4)
				this._max_round++;
		}
		if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_ONE)) {
			this._jetton_score[0] = 20;
			this._jetton_score[1] = 10;
			this._jetton_count = 2;
		}
		if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_TWO)) {
			this._jetton_score[0] = 40;
			this._jetton_score[1] = 20;
			this._jetton_count = 2;
		}

		banker_info(GameConstants.BANKER_BU, _cur_banker, true);

		this.handler_game_finish(this._cur_banker, GameConstants.Game_End_NORMAL);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			update_button(4, i, this._idi_ready_card_time, true);
		}
		return true;
	}

	public boolean cancel_banker() {
		_banker_operate = 0;
		GRR._game_score[_cur_banker] = this._cur_banker_score;
		_player_result.game_score[_cur_banker] += GRR._game_score[_cur_banker];
		if (this._banker_count < this.getTablePlayerNumber()) {
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_ONE)) {
				this._jetton_score[0] = 10;
				this._jetton_score[1] = 5;
				this._jetton_count = 2;
			}
			if (has_rule(GameConstants.GAME_RULE_BANKER_SCORE_TWO)) {
				this._jetton_score[0] = 20;
				this._jetton_score[1] = 10;
				this._jetton_count = 2;
			}
			_cur_banker = (_cur_banker + 1) % this.getTablePlayerNumber();
			if (has_rule(GameConstants.GAME_RULE_THREE_JU_DOWN_BANKER)) {

				this._max_round = 3;
			}
			if (has_rule(GameConstants.GAME_RULE_FOUR_JU_DOWN_BANKER)) {
				this._max_round = 4;
			}
			this._banker_count++;
			_cur_banker_count = 0;
			_cur_banker_score = _banker_up_score;
			GRR._game_score[_cur_banker] = -_cur_banker_score;
			_player_result.game_score[_cur_banker] += GRR._game_score[_cur_banker];
			banker_info(0, _cur_banker, true);

		} else {
			_max_round = _cur_banker_count;
		}

		this.handler_game_finish(this._cur_banker, GameConstants.Game_End_NORMAL);
		if (_max_round != _cur_banker_count)
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				update_button(4, i, this._idi_ready_card_time, true);
			}
		return true;
	}

	public boolean send_open_to_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_OPEN_CARD);
		OpenCard_Dbn.Builder open_card = OpenCard_Dbn.newBuilder();
		open_card.setOpenCard(_open_card_player[seat_index]);
		open_card.setSeatIndex(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));
		this.send_response_to_other(seat_index, roomResponse);
		for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
			open_card.addCards(this.GRR._cards_data[seat_index][i]);
		}
		open_card.setOxValue(this._ox_value[seat_index]);
		open_card.setTimes(this._player_times[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public int get_hand_card_count_max() {
		return GameConstants.ZJH_MAX_COUNT;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int card_cards[]) {

		_all_card_len = repertory_card.length;
		GRR._left_card_count = 0;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * 5 + j];
			}
			GRR._left_card_count += 5;
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	public void kill_timer() {
		_game_scheduled.cancel(false);
		_game_scheduled = null;
	}

	private void test_cards() {

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		// int[] realyCards = new int[] {
		// 0x11,0x02,0x07,0x05,0x05,
		// 0x01,0x12,0x13,0x14,0x15,
		// 0x31,0x22,0x23,0x24,0x25,
		// 0x21,0x32,0x33,0x34,0x35
		// };
		// testRealyCard(realyCards);

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

	public void load_player_info_data_liang_pai(LiangCard_Dbn.Builder roomResponse, int seat_index) {
		Player rplayer;
		rplayer = this.get_players()[seat_index];
		if (rplayer == null)
			return;
		RoomPlayerResponseDbn.Builder room_player = RoomPlayerResponseDbn.newBuilder();
		room_player.setAccountId(rplayer.getAccount_id());
		room_player.setHeadImgUrl(rplayer.getAccount_icon());
		room_player.setIp(rplayer.getAccount_ip());
		room_player.setUserName(rplayer.getNick_name());
		room_player.setSeatIndex(rplayer.get_seat_index());
		room_player.setOnline(rplayer.isOnline() ? 1 : 0);
		room_player.setIpAddr(rplayer.getAccount_ip_addr());
		room_player.setSex(rplayer.getSex());
		room_player.setScore(_player_result.game_score[seat_index]);
		room_player.setReady(_player_ready[seat_index]);
		room_player.setMoney(rplayer.getMoney());
		room_player.setGold(rplayer.getGold());
		if (rplayer.locationInfor != null) {
			room_player.setLocationInfor(rplayer.locationInfor);
		}
		roomResponse.addPlayers(room_player);
	}

	public void load_player_info_data_game_start(GameStartDbn.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDbn.Builder room_player = RoomPlayerResponseDbn.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndDbn.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDbn.Builder room_player = RoomPlayerResponseDbn.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponseDbn.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDbn.Builder room_player = RoomPlayerResponseDbn.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._wait_cancel_trustee[i] == true) {
				this._wait_cancel_trustee[i] = false;
				handler_request_trustee(i, false, 0);
			}
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		ret = this.handler_game_finish_dbn(seat_index, reason);

		return ret;
	}

	public boolean handler_game_finish_dbn(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}
		if (this._game_scheduled != null)
			this.kill_timer();
		if (_cur_banker_count < _max_round) {
			this.set_timer(GameConstants.HJK_READY_TIMER, this._idi_ready_card_time, true);
			this.set_trustee_timer(GameConstants.HJK_READY_TIMER, _idi_ready_card_time, -1, true);
		}

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);

		// 最高得分
		// this.operate_player_data();

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBN_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndDbn.Builder game_end_dbn = PukeGameEndDbn.newBuilder();
		RoomInfoDbn.Builder room_info = getRoomInfoDbn();
		game_end_dbn.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {
		}
		this.load_player_info_data_game_end(game_end_dbn);
		game_end_dbn.setGameRound(this.getTablePlayerNumber());
		game_end_dbn.setCurRound(_banker_count);

		if (GRR != null) {
			int banker_score = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == this._last_banker) {
					continue;
				}
				banker_score += _end_score[i];
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == this._last_banker)
					game_end.addGameScore(-banker_score);
				else
					game_end.addGameScore(_end_score[i]);
				game_end_dbn.addCardCount(GRR._card_count[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_dbn.addCardsData(cards_card);
			}
		}

		boolean end = false;
		// if(reason == GameConstants.Game_End_NORMAL)
		// {
		// if(_cur_banker_count >= _max_round )
		// {
		// int banker_round = 0;
		// if(has_rule(GameConstants.GAME_RULE_FOUR_PlAYER))
		// banker_round = 4;
		// if(has_rule(GameConstants.GAME_RULE_FIVE_PLAYER))
		// banker_round = 5;
		// if(has_rule(GameConstants.GAME_RULE_SIX_PLAYER))
		// banker_round = 6;
		// if(_banker_count < banker_round)
		// {
		// _max_round ++;
		// this.log_info("swe warn banker_round =
		// "+banker_round+"_cur_banker_count =
		// "+_cur_banker_count +"_banker_count = "+_banker_count);
		// }
		// }
		// }
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_banker_count >= _max_round) {// 局数到了
				end = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_dbn.addEndScore((int) _player_result.game_score[i]);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_dbn.addEndScore((int) _player_result.game_score[i]);
			}
			game_end.setPlayerResult(this.process_player_result(reason));

			real_reason = GameConstants.Game_End_RELEASE_PLAY;// 刘局
			end = true;

		}
		game_end_dbn.setWinner(seat_index);
		game_end_dbn.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_max_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_dbn));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
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
	 * @return
	 */
	@Override
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = encodeRoomBase();

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	/**
	 * @return
	 */
	public RoomInfoDbn.Builder getRoomInfoDbn() {
		RoomInfoDbn.Builder room_info = RoomInfoDbn.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_max_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setCreateName(this.getRoom_owner_name());
		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	/**
	 * 创建房间
	 * 
	 * @return
	 */
	@Override
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		super.handler_create_room(player, type, maxNumber);
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			_owner_account_id = player.get_seat_index();
		}
		return true;
	}

	@Override
	public boolean handler_enter_room(Player player) {
		if (super.handler_enter_room(player)) {
			if (player.getAccount_id() == getRoom_owner_account_id()) {
				_owner_account_id = player.get_seat_index();
			}
			return true;
		}
		return false;
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
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			_owner_account_id = player.get_seat_index();
		}
		//
		observers().send(player, roomResponse);

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
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		_player_ready[seat_index] = 1;
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		if (is_cancel) {
			_player_ready[seat_index] = 0;
		}

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if (this.istrustee[seat_index]) {
			this.istrustee[seat_index] = false;
			if (this._trustee_schedule[seat_index] != null) {
				this._trustee_schedule[seat_index].cancel(false);
				this._trustee_schedule[seat_index] = null;
			}
			this._trustee_type[seat_index] = 0;
		}
		this.log_error("gme_status:" + this._game_status + " seat_index:" + seat_index);
		if ((GameConstants.GS_MJ_FREE != _game_status) && this.get_players()[seat_index] != null) {
			banker_info(0, seat_index, false);
		}
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DBN_RECONNECT_DATA);

			TableResponseDbn.Builder tableResponse_dbn = TableResponseDbn.newBuilder();
			load_player_info_data_reconnect(tableResponse_dbn);
			RoomInfoDbn.Builder room_info = getRoomInfoDbn();
			tableResponse_dbn.setRoomInfo(room_info);

			tableResponse_dbn.setBankerPlayer(_cur_banker);
			tableResponse_dbn.setCurrentPlayer(_current_player);
			tableResponse_dbn.setSceneStatus(_game_status);
			tableResponse_dbn.setFirstSeatIdex(_first_seat);
			tableResponse_dbn.setTableDiFen(this._cur_banker_score);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				tableResponse_dbn.addJettonScore(this._jetton_player[i]);
				tableResponse_dbn.addOpenCard(this._open_card_player[i]);
				tableResponse_dbn.addLiangCard(this._liang_card_player[i]);
				if (this._liang_card_player[i] == true) {
					tableResponse_dbn.addOxValue(this._ox_value[i]);
					tableResponse_dbn.addPlayerTimes(this._player_times[i]);
				} else {
					tableResponse_dbn.addOxValue(this._ox_value[i]);
					tableResponse_dbn.addPlayerTimes(this._player_times[i]);
				}
			}
			tableResponse_dbn.setTableDiFen(this._cur_banker_score);
			tableResponse_dbn.setOperateQieCard(this._is_first);

			if (_game_status == GameConstants.GS_OX_OPEN_CARD) {
				int card_count = 5;
				for (int k = 0; k < this.getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if (k == seat_index || this._liang_card_player[k] == true) {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					} else {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.BLACK_CARD);

						}
					}

					tableResponse_dbn.addCardsData(k, cards);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_dbn));
			send_response_to_player(seat_index, roomResponse);
			if (_game_status == GameConstants.GS_OX_QIE_CARD) {
				if (_is_first == 0) {
					_banker_operate = 0;
					_banker_operate = GameConstants.QIE_CARD;
					update_button(3, seat_index, this._idi_qie_card_time, false);
				}
			}
			if (_game_status == GameConstants.GS_OX_ADD_JETTON) {
				if (has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
					if (this._cur_banker != seat_index && this._jetton_player[seat_index] == 0 && this._current_player == seat_index) {

						update_button(2, seat_index, this._idi_jetton_score_time, false);

					}

				} else if (has_rule(GameConstants.GAME_RULE_DA_XIAO_END)) {
					if (this._jetton_player[seat_index] == 0 && seat_index != _cur_banker) {
						update_button(2, seat_index, this._idi_jetton_score_time, false);
					}
				}
			}
			if (_game_status == GameConstants.GS_OX_OPEN_CARD) {
				if (_banker_operate > 0 && seat_index == this._cur_banker)
					update_button(1, seat_index, this._idi_open_card_time, false);
			}
			if (_game_status == GameConstants.GS_MJ_WAIT) {
				update_button(4, seat_index, this._idi_ready_card_time, false);
			}
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
		if (this._cur_round > 0)
			return handler_player_ready(seat_index, false);
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

		// if (this._handler != null) {
		// this._handler.handler_player_out_card(this, seat_index, card);
		// }

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
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

		// if (this._handler != null) {
		// this._handler.handler_operate_card(this, seat_index, operate_code,
		// operate_card, luoCode);
		// }

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
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_DBN_OPERATE) {

			Opreate_dbn_Request req = PBUtil.toObject(room_rq, Opreate_dbn_Request.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getAddJettonScore());
		}
		return true;
	}

	// 1:切牌 2：下注 3：开牌 4：再挺一局 5：补庄6：下庄
	public boolean handler_requst_opreate(int seat_index, int operate_type, int jetton_score) {
		if (operate_type < 1 || operate_type > 6) {
			log_error("命令不在范围内" + seat_index);
		}
		switch (operate_type) {
		case 1: {
			return result_qie_card(seat_index);
		}
		case 2: {
			return result_jetton(seat_index, jetton_score);
		}
		case 3: {
			return open_card(seat_index);
		}
		case 4: {
			if (this._game_scheduled != null)
				this.kill_timer();
			_banker_operate = 0;
			if (has_rule(GameConstants.GAME_RULE_THREE_JU_DOWN_BANKER)) {
				if (this._cur_banker_count >= 3)
					this._max_round++;
			}
			if (has_rule(GameConstants.GAME_RULE_FOUR_JU_DOWN_BANKER)) {
				if (this._cur_banker_count >= 4)
					this._max_round++;
			}

			this.handler_game_finish(this._cur_banker, GameConstants.Game_End_NORMAL);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				update_button(4, i, this._idi_ready_card_time, true);
			}
			break;
		}
		case 5: {
			if (this._game_scheduled != null)
				this.kill_timer();
			return add_bu_banker();

		}
		case 6: {
			if (this._game_scheduled != null)
				this.kill_timer();
			return cancel_banker();
		}

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
		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

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
			playerNumber = this.getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._trustee_schedule[i] != null) {
					this._trustee_schedule[i].cancel(false);
					this._trustee_schedule[i] = null;
				}
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

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null) {
					_gameRoomRecord.release_players[i] = 0;
				}

			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null) {
					if (_gameRoomRecord.release_players[i] == 1) {// 都同意了
						count++;
					}
				}

			}
			if (count == this.getPlayerCount()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[i] == null) {
					continue;
				}
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			this._player_result.game_score[_cur_banker] += this._cur_banker_score;
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

			this.set_timer(this._cur_game_timer, this._cur_operate_time, false);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.istrustee[i] == true)
					this.set_trustee_timer(_cur_game_timer, _cur_operate_time, i, true);
			}

			if (this._cur_operate_time > 0) {
				Timer_OX.Builder timer = Timer_OX.newBuilder();
				timer.setDisplayTime(_cur_operate_time);
			}

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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
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

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}
			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
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

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);// 29
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		return false;
	}

	/**
	 * 效果
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

	@Override
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
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @return
	 */
	public boolean operate_player_cards() {

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

	@Override
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

	public boolean send_play_data(int seat_index) {
		return true;

	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
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
		//
		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();
		//
		// for (int i = 0; i < count; i++) {
		// player_result.addGameScore(_player_result.game_score[i]);
		// player_result.addWinOrder(_player_result.win_order[i]);
		// player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
		// player_result.addMingTangCount(_player_result.ming_tang_count[i]);
		// player_result.addYingXiCount(_player_result.ying_xi_count[i]);
		//
		// player_result.addPlayersId(i);
		// // }
		// }

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_max_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);
		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			room_player.setMoney(rplayer.getMoney());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public boolean send_response_to_special(int seat_index, int to_player, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;

		player = this.get_players()[to_player];
		if (player == null)
			return true;
		if (to_player == seat_index)
			return true;
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

		PlayerServiceImpl.getInstance().send(this.get_players()[to_player], responseBuilder.build());

		return true;
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {

		this._handler = this._handler_finish;
		// this._handler_finish.exe(this);
		return true;
	}

	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

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
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d,
			int delay) {
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}

		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(this.getRoom_id(), seat_index, provide_player, center_card, action, type, depatch, self, d), delay,
					TimeUnit.MILLISECONDS);
		} else {

		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {

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

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_out_card(int seat_index, int card, int type) {

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
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
	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;
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
		if (_playerStatus == null || istrustee == null)
			return false;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);

		if ((!(_game_status == GameConstants.GS_MJ_WAIT || _game_status == GameConstants.GS_MJ_FREE)) && isTrustee == false) {
			send_error_notify(get_seat_index, 2, "托管将在本轮结束后取消!");
			_wait_cancel_trustee[get_seat_index] = true;
			return false;
		}

		istrustee[get_seat_index] = isTrustee;
		_trustee_type[get_seat_index] = Trustee_type;
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
		if (istrustee[get_seat_index] == true)
			this.set_trustee_timer(this._cur_game_timer, GameConstants.OX_TRUESTEE_OPERATE_TIME, get_seat_index, false);
		else {
			if (_trustee_schedule[get_seat_index] != null) {
				_trustee_schedule[get_seat_index].cancel(false);
				_trustee_schedule[get_seat_index] = null;
			}
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	@Override
	public int getTablePlayerNumber() {
		if (BullFightUtil.isTypeDouBan(_game_type_index)) {
			if (has_rule(GameConstants.GAME_RULE_FOUR_PlAYER))
				return 4;
			if (has_rule(GameConstants.GAME_RULE_FIVE_PLAYER))
				return 5;
			if (has_rule(GameConstants.GAME_RULE_SIX_PLAYER))
				return 6;
		}
		return GameConstants.GAME_PLAYER_OX;
	}

	@Override
	public boolean exe_finish(int reason) {
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(this.get_players()[i].getAccount_id(), score, false,
					buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
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

	@Override
	public boolean liang_card_timer(int seat_index) {
		liang_pai_result(seat_index);
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		if (this._is_first == 0) {
			handler_requst_opreate(this._first_player, 1, 0);
		}
		return false;
	}

	@Override
	public boolean animation_timer(int timer_id) {
		switch (timer_id) {
		case ID_TIMER_ANIMATION_QIE_DALAY: {
			update_button(2, this._current_player, this._idi_jetton_score_time, true);
			if (this._game_scheduled != null)
				this.kill_timer();
			this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			if (_trustee_schedule[this._first_seat] != null)
				this.set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, this._first_seat, true);
			return true;
		}
		case ID_TIMER_ANIMATION_CANCEl_BANKER: {
			cancel_banker();
			return true;
		}
		}

		return false;
	}

	@Override
	public boolean ready_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++)
			if (this.get_players()[i] != null) {
				if (this._player_ready[i] == 0) {
					handler_player_ready(i, false);

				}
			}
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		if (has_rule(GameConstants.GAME_RULE_SHUN_XU_END)) {
			if (this._current_player != this._cur_banker)
				this.result_jetton(this._current_player, 0);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == _cur_banker)
					continue;
				this.result_jetton(i, 0);
			}
		}

		return false;
	}

	@Override
	public boolean open_card_timer() {
		// TODO Auto-generated method stub
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._open_card_player[i] == false) {
				this.open_card(i);
			}
		}
		return false;
	}

	@Override
	public boolean Banker_Operate_timer() {
		if (this._banker_operate != 0) {
			handler_requst_opreate(this._cur_banker, 6, 0);
		}
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {

		if (operate_id == GameConstants.HJK_READY_TIMER) {
			handler_player_ready(seat_index, false);
		} else if (operate_id == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			robot_banker_timer();
		} else if (operate_id == GameConstants.HJK_ADD_JETTON_TIMER) {
			add_jetton_timer();

		} else if (operate_id == GameConstants.HJK_OPEN_CARD_TIMER) {
			if (this._open_card_player[seat_index] == false) {
				this.open_card(seat_index);
			}
		} else if (operate_id == GameConstants.HJK_BANKER_OPERATE) {
			if (has_trustee(GameConstants.GAME_RULE_TG_ON_BU, seat_index)) {
				if ((this._banker_operate & GameConstants.BANKER_BU) != 0) {
					if (this._cur_banker == seat_index)
						handler_requst_opreate(this._cur_banker, 5, 0);
				}

			}
			if (has_trustee(GameConstants.GAME_RULE_TG_ON_CONTINUE, seat_index)) {
				if ((this._banker_operate & GameConstants.BANKER_CONTINUE) != 0) {
					if (this._cur_banker == seat_index)
						handler_requst_opreate(this._cur_banker, 4, 0);
				}

			}
			if ((this._banker_operate & GameConstants.BANKER_CANCEL) != 0) {
				if (this._cur_banker == seat_index)
					handler_requst_opreate(this._cur_banker, 6, 0);
			}
		}

		return true;
	}
}
