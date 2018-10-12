package com.cai.game.bullfight.newyy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddJettonRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OpenCardRunnable;
import com.cai.future.runnable.ReadyRunnable;
import com.cai.future.runnable.RobotBankerRunnable;
import com.cai.future.runnable.TrusteeRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.zjh.handler.ZJHHandler;
import com.cai.game.zjh.handler.ZJHHandlerFinish;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.Timer_OX;
import protobuf.clazz.yiyangox.yiyangoxRsp.ButtonOperateYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.GameStartYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.JettonResultYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.OpenCardYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.Opreate_YiYangOx_Request;
import protobuf.clazz.yiyangox.yiyangoxRsp.PlayerResultYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.PukeGameEndYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.RoomPlayerResponseYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.SelectdBankerResultYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.SelectdBankerYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.SendCardYiYangOx;
import protobuf.clazz.yiyangox.yiyangoxRsp.TableResponseYiYangOx;

public class YynOxTable extends AbstractRoom {
	private static final long serialVersionUID = 7060061356475703643L;

	private static final int ID_TIMER_ANIMATION_START = 1;
	private static final int ID_TIMER_ANIMATION_ROBOT = 2;

	private static Logger logger = Logger.getLogger(YynOxTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
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
	public int _tui_zhu_times; // 推注倍数
	public int _can_tuizhu_player[]; // 可以推注用户

	public int is_game_start; // 游戏是否开始

	public YynGameLogic _logic = null;

	public int _current_player = GameConstants.INVALID_SEAT;
	public int _prev_palyer = GameConstants.INVALID_SEAT;
	public int _pre_opreate_type;

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
	public int _is_first; // 是否有先家
	public int _idi_call_banker_time; // 切牌时间
	public int _idi_jetton_score_time; // 下注时间
	public int _idi_open_card_time; // 开牌时间
	public int _idi_ready_card_time; // 等待时间
	public int _qie_card; // 切的牌
	public int _jetton_score[][]; // 下注按钮
	public int _jetton_count[]; // 下注按钮个数
	public int _jetton_count_max; // 最大下注个数

	public int _jetton_player[]; // 用户下注
	public boolean _open_card_player[]; // 用户开牌
	public int _player_times[]; // 用户倍数
	public int _ox_value[]; // 牛值
	public boolean _player_status[]; // 游戏状态
	public int _call_banker_info[]; // 叫庄信息
	public int _banker_times; // 庄家倍数
	public int _banker_max_times;// 最大抢庄倍数
	public int _player_call_banker[]; // 用户是否叫庄
	public int _owner_seat; // 庄家位置
	public String _str_ox_value[]; // 牛几
	public int _operate_start_time; // 操作开始时间
	public int _cur_operate_time; // 可操作时间
	public int _cur_game_timer; // 当前游戏定时器
	public int _next_banker;
	public int _game_rule_index_long;
	public int _trustee_type[];// 托管的内容
	public boolean _wait_cancel_trustee[];// 取消托管

	private long _request_release_time;
	@SuppressWarnings("rawtypes")
	private ScheduledFuture _release_scheduled;
	@SuppressWarnings("rawtypes")
	private ScheduledFuture _game_scheduled;
	@SuppressWarnings("rawtypes")
	public ScheduledFuture _trustee_schedule[];

	public ZJHHandler _handler;

	public ZJHHandlerFinish _handler_finish; // 结束

	public YynOxTable() {
		super(RoomType.OX, 8);

		_logic = new YynGameLogic();
		_jetton_round = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}

		_game_status = GameConstants.GS_MJ_FREE;

		_player_ready = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		_next_banker = -1;
		istrustee = new boolean[getTablePlayerNumber()];
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
		_can_tuizhu_player = new int[getTablePlayerNumber()]; // 可以推注用户
		_trustee_type = new int[getTablePlayerNumber()];
		_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		_wait_cancel_trustee = new boolean[getTablePlayerNumber()];// 取消托管
		Arrays.fill(_trustee_type, 0);
		Arrays.fill(_wait_cancel_trustee, false);
		Arrays.fill(_is_opreate_look, 0);
		Arrays.fill(_is_opreate_give_up, 0);
		Arrays.fill(_is_opreate_compare, 0);
		Arrays.fill(_is_opreate_gen, 0);
		Arrays.fill(_is_opreate_add, 0);
		playerNumber = 0;
		_jetton_total_score = 0;
		_tui_zhu_times = 0; // 推注倍数

		_cur_banker = 0;
		_banker_up_score = 0;
		_banker_bu_score = 0;
		_jetton_min_score = 0;
		_owner_account_id = -1;
		_cur_banker_score = 0;
		_down_min_banker_score = 10;
		_first_player = -1;
		_banker_count = 0;
		_jetton_count_max = 0;
		_idi_call_banker_time = 5; // 切牌时间
		_idi_jetton_score_time = 5; // 下注时间
		_idi_open_card_time = 20; // 开牌时间
		_idi_ready_card_time = 10; // 等待时间
		_banker_operate = 0;
		_is_first = -1;
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_jetton_count = new int[getTablePlayerNumber()];
		_is_bu_banker = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[getTablePlayerNumber()][10];
		_jetton_player = new int[getTablePlayerNumber()]; // 用户下注
		_open_card_player = new boolean[getTablePlayerNumber()]; // 用户开牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];

		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_is_bu_banker, false);
		Arrays.fill(_jetton_player, 0);
		Arrays.fill(_open_card_player, false);

		_player_status = new boolean[getTablePlayerNumber()];
		Arrays.fill(_player_status, false);
		_call_banker_info = new int[getTablePlayerNumber()]; // 叫庄信息
		Arrays.fill(_call_banker_info, 0);
		_player_call_banker = new int[getTablePlayerNumber()];
		Arrays.fill(_player_call_banker, -1);
		_banker_times = 1; // 庄家倍数
		_banker_max_times = 1;
		_str_ox_value = new String[getTablePlayerNumber()]; // 牛几
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_str_ox_value[i] = "";
			_jetton_score[i] = new int[10];
		}
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		}

		_owner_seat = -1;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_cur_banker = 0;
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		is_game_start = 0;
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
		_is_first = -1;
		_idi_call_banker_time = 5; // 切牌时间
		_idi_jetton_score_time = 10; // 下注时间
		_idi_open_card_time = 20; // 开牌时间
		_idi_ready_card_time = 10; // 等待时间
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		// 玩家
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		_owner_seat = -1;
		istrustee = new boolean[getTablePlayerNumber()];
		_jetton_count = new int[getTablePlayerNumber()];
		_is_bu_banker = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[getTablePlayerNumber()][10];
		_jetton_player = new int[getTablePlayerNumber()]; // 用户下注
		_open_card_player = new boolean[getTablePlayerNumber()]; // 用户开牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		_trustee_type = new int[getTablePlayerNumber()];
		_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		_wait_cancel_trustee = new boolean[getTablePlayerNumber()];// 取消托管
		Arrays.fill(_trustee_type, 0);
		Arrays.fill(_wait_cancel_trustee, false);
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_is_bu_banker, false);
		Arrays.fill(_jetton_player, 0);
		Arrays.fill(_open_card_player, false);
		_player_status = new boolean[getTablePlayerNumber()];
		Arrays.fill(_player_status, false);
		_call_banker_info = new int[getTablePlayerNumber()]; // 叫庄信息
		Arrays.fill(_call_banker_info, 0);
		_player_call_banker = new int[getTablePlayerNumber()];
		Arrays.fill(_player_call_banker, -1);
		_can_tuizhu_player = new int[getTablePlayerNumber()]; // 可以推注用户
		Arrays.fill(_can_tuizhu_player, 0);
		_banker_times = 1; // 庄家倍数
		_banker_max_times = 1;
		_str_ox_value = new String[getTablePlayerNumber()]; // 牛几
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_str_ox_value[i] = "";
			_jetton_score[i] = new int[10];
		}
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		}
		_player_result = new PlayerResult(getRoom_owner_account_id(), getRoom_id(), _game_type_index, _game_rule_index, _game_round, get_game_des(),
				getTablePlayerNumber());
		_handler_finish = new ZJHHandlerFinish();
		setMinPlayerCount(2); // 郴州五小牛最小开局人数为2

	}

	public boolean has_trustee(int cbRule, int seat_index) {
		return FvMask.has_any(_trustee_type[seat_index], FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
	}

	@Override
	public boolean reset_init_data() {
		record_game_room();

		_handler = null;
		SysParamModel sysParamModel3 = SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(9);

		GRR = new GameRoundRecord(getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.OX_MAX_CARD_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_banker_operate = 0;
		if (sysParamModel3 != null) {
			_idi_ready_card_time = sysParamModel3.getVal1(); // 等待时间
			_idi_call_banker_time = sysParamModel3.getVal2(); // 切牌时间
			_idi_jetton_score_time = sysParamModel3.getVal3(); // 下注时间
			_idi_open_card_time = sysParamModel3.getVal4(); // 开牌时间
		} else {
			_idi_ready_card_time = 5; // 等待时间
			_idi_call_banker_time = 6; // 切牌时间
			_idi_jetton_score_time = 10; // 下注时间
			_idi_open_card_time = 20; // 开牌时间
		}

		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_jetton_player = new int[getTablePlayerNumber()]; // 用户下注
		_open_card_player = new boolean[getTablePlayerNumber()]; // 用户开牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_jetton_player, 0);
		Arrays.fill(_open_card_player, false);

		_player_status = new boolean[getTablePlayerNumber()];
		Arrays.fill(_player_status, false);
		_call_banker_info = new int[getTablePlayerNumber()]; // 叫庄信息
		Arrays.fill(_call_banker_info, 0);
		_player_call_banker = new int[getTablePlayerNumber()];
		Arrays.fill(_player_call_banker, -1);
		_banker_times = 1; // 庄家倍数
		_playerStatus = new PlayerStatus[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
		}
		_str_ox_value = new String[getTablePlayerNumber()]; // 牛几
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_str_ox_value[i] = "";
		}
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		}
		_cur_round++;

		// 牌局回放
		GRR._room_info.setRoomId(getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(getRoom_owner_account_id());
		GRR._room_info.setNewRules(commonGameRuleProtos);

		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
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

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_OX_GAME_PLAY;

		reset_init_data();

		_cur_jetton_round = 0;

		_repertory_card = new int[GameConstants.CARD_COUNT_NO_HUA_PAI_OX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_YI_YANG);

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

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

	
		if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_ONE_TO_FIVE) == 1) {
			_jetton_count_max = 5;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Arrays.fill(_jetton_score[i], 0);
				_jetton_count[i] = _jetton_count_max;
				for (int j = 0; j < _jetton_count[i]; j++) {
					_jetton_score[i][j] = 1 + j;
				}
			}
		}
		if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_TWO_TO_TEN) == 1) {
			_jetton_count_max = 5;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Arrays.fill(_jetton_score[i], 0);
				_jetton_count[i] = _jetton_count_max;
				for (int j = 0; j < _jetton_count[i]; j++) {
					_jetton_score[i][j] = (1 + j) * 2;
				}
			}
		}
		if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_FIVE_TO_TEN) == 1) {
			_jetton_count_max = 6;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Arrays.fill(_jetton_score[i], 0);
				_jetton_count[i] = _jetton_count_max;
				for (int j = 0; j < _jetton_count[i]; j++) {
					_jetton_score[i][j] = 5 + j;
				}
			}
		}
		if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_ONE_TO_THREE) == 1) {
			_jetton_count_max = 3;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Arrays.fill(_jetton_score[i], 0);
				_jetton_count[i] = _jetton_count_max;
				for (int j = 0; j < _jetton_count[i]; j++) {
					_jetton_score[i][j] = 1 + j;
				}
			}
		}
		if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_TWO_TO_FIVE) == 1) {
			_jetton_count_max = 4;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Arrays.fill(_jetton_score[i], 0);
				_jetton_count[i] = _jetton_count_max;
				for (int j = 0; j < _jetton_count[i]; j++) {
					_jetton_score[i][j] = 2 + j;
				}
			}
		}
		if (getRuleValue(GameConstants.GAME_RULE_GU_DING_YA_FEN) == 1) {
			// 获取固定压分的分值
			_jetton_count_max = 1;
			int gu_ding_fen = getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_GU_DING);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Arrays.fill(_jetton_score[i], 0);
				_jetton_count[i] = _jetton_count_max;
				for (int j = 0; j < _jetton_count[i]; j++) {
					_jetton_score[i][j] = gu_ding_fen;
				}
			}
		}

		_tui_zhu_times = 0;

		if (getRuleValue(GameConstants.GAME_RULE_BEI_SHU_QIANG_ZHUANG) == 1) {
			if (getRuleValue(GameConstants.GAME_RULE_MAX_ONE_TIMES) == 1)
				_banker_max_times = 1;
			else if (getRuleValue(GameConstants.GAME_RULE_MAX_TWO_TIMES) == 1)
				_banker_max_times = 2;
			else if (getRuleValue(GameConstants.GAME_RULE_MAX_THREE_TIMES) == 1)
				_banker_max_times = 3;
			else if (getRuleValue(GameConstants.GAME_RULE_MAX_FOUR_TIMES) == 1)
				_banker_max_times = 4;
			else
				_banker_max_times = 1;
		} else {
			_banker_max_times = 1;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker_info[i] = i;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_can_tuizhu_player[i] > 0) {
				_jetton_score[i][_jetton_count[i]++] = _can_tuizhu_player[i];
			}
		}

		return game_start_yiyangox();
	}

	public boolean game_start_yiyangox() {
		_game_status = GameConstants.GS_OX_GAME_PLAY; // 设置状态

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		for (int index = 0; index < getTablePlayerNumber(); index++) {
			if (get_players()[index] == null) {
				continue;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

			load_common_status(roomResponse);

			roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_GAME_START);
			roomResponse.setGameStatus(_game_status);

			GameStartYiYangOx.Builder gamestart = GameStartYiYangOx.newBuilder();

			RoomInfo.Builder room_info = getRoomInfo();

			gamestart.setRoomInfo(room_info);

			load_player_info_data_game_start(gamestart);

			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			send_response_to_player(index, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_GAME_START);
		roomResponse.setGameStatus(_game_status);

		GameStartYiYangOx.Builder gamestart = GameStartYiYangOx.newBuilder();

		RoomInfo.Builder room_info = getRoomInfo();

		gamestart.setRoomInfo(room_info);

		load_player_info_data_game_start(gamestart);

		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		GRR.add_room_response(roomResponse);

		observers().sendAll(roomResponse);

		GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_START), 0, TimeUnit.MILLISECONDS);

		return true;
	}

	public boolean update_button(int opr_type, int seat_index, int display_time, boolean is_grr) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_UPDATE_BTN);

		ButtonOperateYiYangOx.Builder button_operate = ButtonOperateYiYangOx.newBuilder();
		button_operate.setOpreateType(opr_type);
		button_operate.setDisplayTime(display_time);
		button_operate.clearButton();

		if (opr_type == 0) {
			for (int j = 0; j <= _banker_max_times; j++) {
				_call_banker_info[j] = j;

				button_operate.addButton(_call_banker_info[j]);
			}
		}

		if (opr_type == 1 && seat_index != _cur_banker) {
			for (int j = 0; j < _jetton_count[seat_index]; j++) {
				button_operate.addButton(_jetton_score[seat_index][j]);
			}

			if (_jetton_count[seat_index] != _jetton_count_max)
				button_operate.setIsHaveTuiZhu(true);
			else
				button_operate.setIsHaveTuiZhu(false);
		}

		if (opr_type == 2) {
			for (int j = 0; j < 2; j++) {
				button_operate.addButton(j);
			}

			int temp_cards[] = new int[GameConstants.OX_MAX_CARD_COUNT];
			for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
				temp_cards[i] = GRR._cards_data[seat_index][i];
			}
		}

		roomResponse.setCommResponse(PBUtil.toByteString(button_operate));

		send_response_to_player(seat_index, roomResponse);

		if (is_grr == true)
			GRR.add_room_response(roomResponse);

		button_operate.clearButton();
		button_operate.clearChoosecardsIndex();

		roomResponse.setCommResponse(PBUtil.toByteString(button_operate));

		observers().sendAll(roomResponse);

		return true;
	}

	public boolean send_call_banker_to_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_ROBOT_BANKER);

		SelectdBankerYiYangOx.Builder robot_banker = SelectdBankerYiYangOx.newBuilder();
		robot_banker.setBankerSeat(seat_index);
		robot_banker.setBankerScore(_player_call_banker[seat_index]);

		roomResponse.setCommResponse(PBUtil.toByteString(robot_banker));

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean set_banker_to_room() {
		_next_banker = _cur_banker;
		_can_tuizhu_player[_cur_banker] = 0;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_ROBOT_BANKER_RESULT);

		SelectdBankerResultYiYangOx.Builder robot_banker = SelectdBankerResultYiYangOx.newBuilder();
		robot_banker.setBankerSeat(_cur_banker);
		robot_banker.setBankerScore(_banker_times);

		roomResponse.setCommResponse(PBUtil.toByteString(robot_banker));

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean call_banker(int seat_index, int sub_index) {
		if (_game_status != GameConstants.GS_OX_CALL_BANKER)
			return true;

		if (sub_index < 0 || sub_index > _banker_max_times) {
			log_error("下注下标不对" + _jetton_count[seat_index]);
			return true;
		}

		if (_player_call_banker[seat_index] != -1) {
			log_error("您已经叫庄了" + seat_index);
			return true;
		}

		_player_call_banker[seat_index] = _call_banker_info[sub_index];

		send_call_banker_to_room(seat_index);

		boolean flag = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (_player_call_banker[i] == -1) {
				flag = true;
				break;
			}
		}

		if (flag == false) {
			for (int j = _banker_max_times; j >= 0; j--) {
				int chairID[] = new int[getTablePlayerNumber()];
				int chair_count = 0;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_status[i] == false)
						continue;

					if ((_player_status[i] == true) && (_call_banker_info[j] == _player_call_banker[i])) {
						chairID[chair_count++] = i;
					}
				}

				if (chair_count > 0) {
					int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
					int temp = rand % chair_count;

					_cur_banker = chairID[temp];
					_banker_times = _player_call_banker[_cur_banker];

					if (_banker_times == 0)
						_banker_times = 1;

					break;
				}
			}

			set_banker_to_room();

			_game_status = GameConstants.GS_OX_ADD_JETTON;

			int user_count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_status[i] == true)
					user_count++;
			}

			GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_ROBOT), 100 * user_count + 2000, TimeUnit.MILLISECONDS);
		}

		return true;
	}

	public boolean result_jetton(int seat_index, int sub_index) {
		if (_game_status != GameConstants.GS_OX_ADD_JETTON)
			return true;

		if (sub_index < 0 || sub_index >= _jetton_count[seat_index]) {
			log_error("下注下标不对" + _jetton_count);
			return true;
		}

		if (_jetton_player[seat_index] != 0) {
			log_error("您已经下注了" + _current_player);
			return true;
		}

		if (sub_index == _jetton_count_max) {
			_can_tuizhu_player[seat_index] = -1;
		} else {
			_can_tuizhu_player[seat_index] = 0;
		}

		_jetton_player[seat_index] = _jetton_score[seat_index][sub_index];

		send_jetton_to_room(seat_index);

		boolean flag = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (i == _cur_banker)
				continue;

			if (_jetton_player[i] == 0) {
				flag = true;
				return true;
			}
		}

		if (flag == false) {
			send_card(5, 1, false);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_status[i] == false)
					continue;

				update_button(2, i, _idi_open_card_time, true);
			}

			if (_game_scheduled != null)
				kill_timer();

			set_timer(GameConstants.HJK_OPEN_CARD_TIMER, _idi_open_card_time, true);
			set_trustee_timer(GameConstants.HJK_OPEN_CARD_TIMER, _idi_open_card_time, true);
		}

		_game_status = GameConstants.GS_OX_OPEN_CARD;

		return true;
	}

	public void send_jetton_to_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_JETTON);

		JettonResultYiYangOx.Builder jetton_result = JettonResultYiYangOx.newBuilder();
		jetton_result.setJettonSeat(seat_index);
		jetton_result.setJettonScore(_jetton_player[seat_index]);

		roomResponse.setCommResponse(PBUtil.toByteString(jetton_result));

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);
	}

	public boolean send_card(int card_count, int opr_type, boolean is_display) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_SEND_CARD);

		SendCardYiYangOx.Builder send_card = SendCardYiYangOx.newBuilder();
		send_card.setDisplayTime(_idi_open_card_time);
		send_card.setOpreateType(opr_type);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null)
				continue;

			send_card.clearSendCard();

			for (int k = 0; k < getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] == false) {
					for (int j = 0; j < card_count; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else if (k == i || (is_display == true)) {
					for (int j = 0; j < card_count; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}

					if (card_count == GameConstants.OX_MAX_CARD_COUNT) {
						_player_times[i] = _logic.get_times_one(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, ruleMap);
						_ox_value[i] = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, ruleMap);

						int temp_cards_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
						for (int index = 0; index < GameConstants.OX_MAX_CARD_COUNT; index++) {
							temp_cards_data[index] = GRR._cards_data[k][index];
						}

						_logic.get_ox_card(temp_cards_data, GameConstants.OX_MAX_CARD_COUNT, ruleMap);

						send_card.clearSortCard();
						for (int index = 0; index < GameConstants.OX_MAX_CARD_COUNT; index++) {
							send_card.addSortCard(temp_cards_data[index]);
						}

						send_card.setOxValue(_ox_value[i]);
						send_card.setTimes(_player_times[i]);
					}
				} else {
					for (int j = 0; j < card_count; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}

				send_card.addSendCard(k, cards);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));

			send_response_to_player(i, roomResponse);
		}

		send_card.clearSendCard();

		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (_player_status[k] == false) {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			}

			send_card.addSendCard(k, cards);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		GRR.add_room_response(roomResponse);

		send_card.clearSendCard();

		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (_player_status[k] == false) {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			}

			send_card.addSendCard(k, cards);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		GRR.add_room_response(roomResponse);

		send_card.clearSendCard();

		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (_player_status[k] == false) {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GameConstants.BLACK_CARD);
				}
			}

			send_card.addSendCard(k, cards);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		observers().sendAll(roomResponse);

		return true;
	}

	public boolean open_card(int seat_index) {
		if (_game_status != GameConstants.GS_OX_OPEN_CARD)
			return true;

		if (seat_index < 0 || seat_index >= getTablePlayerNumber()) {
			send_error_notify(seat_index, 2, "您的位置不对哦！");
			return true;
		}

		if (GRR == null) {
			return true;
		}

		if (_open_card_player[seat_index] != false) {
			log_error("您已经开牌了" + seat_index);
			return true;
		}

		_logic.get_ox_card(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, ruleMap);

		send_open_to_room(seat_index);

		_open_card_player[seat_index] = true;

		boolean flag = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (_open_card_player[i] == false) {
				flag = true;
				return true;
			}
		}

		if (flag == false) {
			process_ox_calulate_end();
			all_game_end_record();

			int delay = 0;
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), _cur_banker, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
		}

		return true;
	}

	public void all_game_end_record() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += GRR._game_score[i];
		}
	}

	public int process_tbox_calulate_end() {
		int win_player_oxtb = -1;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == true) {
				win_player_oxtb = i;
				break;
			}
		}

		for (int i = win_player_oxtb + 1; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] != true)
				continue;

			boolean first_ox = _logic.get_ox_card(GRR._cards_data[win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, ruleMap);
			boolean next_ox = _logic.get_ox_card(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, ruleMap);

			int action = _logic.compare_card(GRR._cards_data[win_player_oxtb], GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, first_ox, next_ox,
					ruleMap);
			if (action == -1) {
				win_player_oxtb = i;
			}
		}

		return win_player_oxtb;
	}

	public void process_ox_calulate_end() {
		if (getRuleValue(GameConstants.GAME_RULE_PAI_DA_DANG_ZHUANG) == 1) {
			int _win_player_oxtb = process_tbox_calulate_end();
			_next_banker = _win_player_oxtb;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == _cur_banker)
				continue;

			if (_player_status[i] != true)
				continue;

			boolean first_ox = _logic.get_ox_card(GRR._cards_data[_cur_banker], GameConstants.OX_MAX_CARD_COUNT, ruleMap);
			boolean next_ox = _logic.get_ox_card(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, ruleMap);
			int action = _logic.compare_card(GRR._cards_data[_cur_banker], GRR._cards_data[i],

					GameConstants.OX_MAX_CARD_COUNT, first_ox, next_ox, ruleMap);

			int calculate_score = 0;
			int lChiHuScore = 0;

			if (action == 1) {
				calculate_score = _logic.get_times_one(GRR._cards_data[_cur_banker], GameConstants.OX_MAX_CARD_COUNT, ruleMap);

				lChiHuScore = calculate_score * _jetton_player[i] * _banker_times;
			} else if (action == -1) {
				calculate_score = _logic.get_times_one(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, ruleMap);

				lChiHuScore = -calculate_score * _jetton_player[i] * _banker_times;
			}

			GRR._game_score[i] -= lChiHuScore;
			GRR._game_score[_cur_banker] += lChiHuScore;
		}

		if (_tui_zhu_times > 0) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_status[i] == false)
					continue;

				if (i == _cur_banker)
					continue;

				if (_can_tuizhu_player[i] == 0) {
					if (GRR._game_score[i] > 0) {
						int temp = 0;

						if ((int) GRR._game_score[i] + _jetton_player[i] > _jetton_score[i][0] * _tui_zhu_times)
							temp = _jetton_score[i][0] * _tui_zhu_times;
						else
							temp = (int) GRR._game_score[i] + _jetton_player[i];

						if (_jetton_score[i][_jetton_count[i] - 1] >= temp)
							_can_tuizhu_player[i] = 0;
						else
							_can_tuizhu_player[i] = temp;
					}
				}
			}
		}
	}

	public boolean send_open_to_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_OPEN_CARD);

		OpenCardYiYangOx.Builder open_card = OpenCardYiYangOx.newBuilder();
		open_card.setOpenCard(_open_card_player[seat_index]);
		open_card.setSeatIndex(seat_index);

		for (int i = 0; i < GameConstants.OX_MAX_CARD_COUNT; i++) {
			open_card.addCards(GRR._cards_data[seat_index][i]);
		}
		open_card.setOxValue(_ox_value[seat_index]);
		open_card.setTimes(_player_times[seat_index]);

		roomResponse.clearCommResponse();
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public void kill_timer() {
		_game_scheduled.cancel(false);
		_game_scheduled = null;
	}

	public boolean set_timer(int timer_type, int time, boolean makeDBtimer) {
		_cur_game_timer = timer_type;

		SysParamModel sysParamModel3 = SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(9);
		if (sysParamModel3 == null)
			return true;

		if (sysParamModel3 != null && sysParamModel3.getVal5() == 0)
			return true;

		if (makeDBtimer == false) {
			_cur_game_timer = timer_type;

			if (timer_type == GameConstants.HJK_READY_TIMER) {
				_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
				_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
				_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			}

			return true;
		}

		_cur_game_timer = timer_type;

		if (timer_type == GameConstants.HJK_READY_TIMER) {
			_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), sysParamModel3.getVal1(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3.getVal1();
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), sysParamModel3.getVal2(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3.getVal2();
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), sysParamModel3.getVal3(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3.getVal3();
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), sysParamModel3.getVal4(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3.getVal4();
		}

		return true;
	}

	public boolean set_trustee_timer(int timer_type, int time, boolean makeDBtimer) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		}

		if (timer_type == GameConstants.HJK_READY_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			time = 2;
		}

		if (makeDBtimer == false)
			time = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

		return true;
	}

	private void shuffle(int repertory_card[], int card_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * 5 + j];
			}
			GRR._left_card_count += 5;
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {
		if (DEBUG_CARDS_MODE) {
			int[] realyCards = new int[] { 0x21, 0x22, 0x23, 0x24, 0x25, 0x06, 0x16, 0x26, 0x13, 0x19 };
			testRealyCard(realyCards);
			DEBUG_CARDS_MODE = true;
		}

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

	public RoomPlayerResponseYiYangOx.Builder load_player_info_data(int seat_index) {
		Player rplayer = get_players()[seat_index];
		if (rplayer == null)
			return null;

		RoomPlayerResponseYiYangOx.Builder room_player = RoomPlayerResponseYiYangOx.newBuilder();
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

		return room_player;
	}

	public void load_player_info_data_game_start(GameStartYiYangOx.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;

			RoomPlayerResponseYiYangOx.Builder room_player = RoomPlayerResponseYiYangOx.newBuilder();
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

	public void load_player_info_data_game_end(PukeGameEndYiYangOx.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;

			RoomPlayerResponseYiYangOx.Builder room_player = RoomPlayerResponseYiYangOx.newBuilder();
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

	public void load_player_info_data_reconnect(TableResponseYiYangOx.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;

			RoomPlayerResponseYiYangOx.Builder room_player = RoomPlayerResponseYiYangOx.newBuilder();
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

	public void testRealyCard(int[] realyCards) {
		int count = realyCards.length / GameConstants.OX_MAX_CARD_COUNT;

		if (count > getTablePlayerNumber())
			count = getTablePlayerNumber();

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
		DEBUG_CARDS_MODE = false; // 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");
	}

	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}

		DEBUG_CARDS_MODE = false; // 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		_game_status = GameConstants.GS_MJ_WAIT;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_wait_cancel_trustee[i] == true) {
				_wait_cancel_trustee[i] = false;
				handler_request_trustee(i, false, 0);
			}
		}

		ret = handler_game_finish_yiyangox(seat_index, reason);

		return ret;
	}

	public boolean handler_game_finish_yiyangox(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}

		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		for (int i = 0; i < count; i++) {
			if (_cur_round > 0) {
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				load_player_info_data(roomResponse2);
				send_response_to_player(i, roomResponse2);
			}
		}

		if (_game_scheduled != null)
			kill_timer();

		if (_cur_round < _game_round) {
			set_timer(GameConstants.HJK_READY_TIMER, _idi_ready_card_time, true);
			set_trustee_timer(GameConstants.HJK_READY_TIMER, _idi_ready_card_time, true);
		}

		int end_score[] = new int[getTablePlayerNumber()];
		Arrays.fill(end_score, 0);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_GAME_END);

		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		PukeGameEndYiYangOx.Builder game_end_yiyang_ox = PukeGameEndYiYangOx.newBuilder();

		RoomInfo.Builder room_info = getRoomInfo();
		game_end_yiyang_ox.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());
		game_end.setCountPickNiao(getTablePlayerNumber());// 玩法人数

		if (reason == GameConstants.Game_End_NORMAL) {
		}

		load_player_info_data_game_end(game_end_yiyang_ox);

		game_end_yiyang_ox.setGameRound(_game_round);
		game_end_yiyang_ox.setCurRound(_cur_round);
		game_end_yiyang_ox.setDisplayTime(_idi_ready_card_time);

		if (GRR != null) {
			operate_player_data();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(GRR._game_score[i]);
				game_end_yiyang_ox.addEndScore((int) GRR._game_score[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}

				game_end_yiyang_ox.addCardsData(cards_card);
				game_end_yiyang_ox.addStrOxValue(_str_ox_value[i]);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) { // 局数到了
				end = true;
				game_end_yiyang_ox.setPlayerResult(process_player_result_yiyangox(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end_yiyang_ox.setPlayerResult(process_player_result_yiyangox(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
		}

		game_end_yiyang_ox.setWinner(seat_index);
		game_end_yiyang_ox.setReason(real_reason);

		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_yiyang_ox));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}
		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end, reason);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;

				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	@Override
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = encodeRoomBase();

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);

		return room_info;
	}

	@Override
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		super.handler_create_room(player, type, maxNumber);

		if (player.getAccount_id() == getRoom_owner_account_id()) {
			_owner_account_id = player.get_seat_index();
			_owner_seat = player.get_seat_index();
		}

		return true;
	}

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏

			observers().send(player, quit_roomResponse);

			observers().exit(player.getAccount_id());

			return true;
		}

		return false;
	}

	@Override
	public boolean handler_enter_room(Player player) {
		if (getRuleValue(GameConstants.GAME_RULE_IP) > 0) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;

				if (player.getAccount_id() == tarplayer.getAccount_id())
					continue;

				if ((!is_sys()) && StringUtils.isNotEmpty(tarplayer.getAccount_ip()) && StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					send_error_notify(player, 1, "不允许相同ip进入");

					return false;
				}
			}
		}

		int seat_index = GameConstants.INVALID_SEAT;

		if (player.getAccount_id() != getRoom_owner_account_id() && getRuleValue(GameConstants.GAME_RULE_START_FORBID_JOIN) == 0) {
			boolean flag = false;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_status[i] == true)
					flag = true;
			}

			if (flag == true) {
				send_error_notify(player, 2, "该房间已经禁止其它玩家在游戏中进入");

				return false;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				get_players()[i] = player;
				seat_index = i;
				break;
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0); // 把房间信息清除--
			send_error_notify(player, 1, "游戏已经开始");
			return false;
		}

		if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id())
			_owner_seat = seat_index;

		if (!observers().sit(player.getAccount_id())) {
			logger.error(String.format("玩家[%s]必须先成为观察者才能坐下!", player));
		}

		player.set_seat_index(seat_index);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);

		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		roomRedisModel.getNames().add(player.getNick_name());

		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		onPlayerEnterUpdateRedis(player.getAccount_id());
		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);

		if (getRuleValue(GameConstants.GAME_RULE_CONTORL_START) == 1) {
			if (is_game_start == 1)
				is_game_start = 0;

			control_game_start();
		}

		return true;
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {
		int limitCount = 20;

		if (SystemConfig.gameDebug == 1) {
			limitCount = 5;
		}

		if (player.getAccount_id() != getRoom_owner_account_id() && observers().count() >= limitCount) {
			send_error_notify(player, 1, "当前游戏围观位置已满,下次赶早!");
			return false;
		}

		observers().enter(player);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		observers().send(player, roomResponse);

		return true;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {
			if (get_players()[get_seat_index] == null) {
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

			if (_cur_round > 0) {
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家

				load_player_info_data(roomResponse2);
				send_response_to_player(get_seat_index, roomResponse2);
			}

			return false;
		} else {
			return handler_player_ready(get_seat_index, is_cancel);
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (get_players()[seat_index] == null) {
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
		if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIsCancelReady(is_cancel);

		send_response_to_room(roomResponse);

		if (_cur_round > 0) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家

			load_player_info_data(roomResponse2);
			send_response_to_player(seat_index, roomResponse2);
		}

		is_game_start();

		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return false;
	}

	public void is_game_start() {
		int _player_count = 0;
		int _cur_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}

			if ((get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}

		int sit_max = 2;
		if ((_player_count >= sit_max) && (_player_count == _cur_count)) {
			if (_cur_round == 0 && getRuleValue(GameConstants.GAME_RULE_CONTORL_START) == 1) {
				is_game_start = 1;
				control_game_start();
			} else {
				handler_game_start();
			}
		} else if ((_cur_round == 0) && getRuleValue(GameConstants.GAME_RULE_CONTORL_START) == 1) {
			is_game_start = 0;
			control_game_start();
		}
	}

	@Override
	public boolean handler_ox_game_start(int room_id, long account_id) {
		if (is_game_start == 2)
			return true;

		is_game_start = 2;

		control_game_start();

		handler_game_start();

		boolean nt = true;

		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	public boolean control_game_start() {
		if (getRuleValue(GameConstants.GAME_RULE_CONTORL_START) == 0)
			return false;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ROOM_OWNER_START);
		roomResponse.setEffectType(is_game_start);
		roomResponse.setPaoDes(getRoom_owner_name());

		if (is_game_start != 2) {
			send_response_to_room(roomResponse);
		}

		Player player = observers().getPlayer(getRoom_owner_account_id());
		if (player == null) {
			player = get_player(getRoom_owner_account_id());

			if (player != null) {
				send_response_to_player(player.get_seat_index(), roomResponse);
			}
		} else {
			observers().send(player, roomResponse);
		}

		return true;
	}

	public boolean table_response_observer(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_RECONNECT_DATA);

		TableResponseYiYangOx.Builder tableResponse_yiyang_ox = TableResponseYiYangOx.newBuilder();

		load_player_info_data_reconnect(tableResponse_yiyang_ox);

		int display_time = _cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - _operate_start_time);

		RoomInfo.Builder room_info = getRoomInfo();
		tableResponse_yiyang_ox.setRoomInfo(room_info);
		tableResponse_yiyang_ox.setBankerPlayer(_cur_banker);
		tableResponse_yiyang_ox.setDisplayTime(display_time);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			tableResponse_yiyang_ox.addCallButton(_player_call_banker[i]);
			tableResponse_yiyang_ox.addJettonScore(_jetton_player[i]);
			tableResponse_yiyang_ox.addOpenCard(_open_card_player[i]);

			if (_open_card_player[i] == true) {
				tableResponse_yiyang_ox.addOxValue(_ox_value[i]);
				tableResponse_yiyang_ox.addTimes(_player_times[i]);
			} else {
				tableResponse_yiyang_ox.addOxValue(-1);
				tableResponse_yiyang_ox.addTimes(0);
			}
		}
		tableResponse_yiyang_ox.setSceneStatus(_game_status);

		if (_game_status == GameConstants.GS_OX_CALL_BANKER) {
			tableResponse_yiyang_ox.setBankerPlayer(-1);
			int card_count = 4;

			if (getRuleValue(GameConstants.GAME_RULE_KSZQZ_OX) == 1) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if (_player_status[k] == false) {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.INVALID_CARD);
						}
					} else {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}

					tableResponse_yiyang_ox.addCardsData(k, cards);
				}
			}
		}

		if (_game_status == GameConstants.GS_OX_ADD_JETTON) {
			int card_count = 4;

			if (getRuleValue(GameConstants.GAME_RULE_KSZQZ_OX) == 1) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if (_player_status[k] == false) {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.INVALID_CARD);
						}
					} else {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}

					tableResponse_yiyang_ox.addCardsData(k, cards);
				}
			}
		}

		if (_game_status == GameConstants.GS_OX_OPEN_CARD) {
			int card_count = 5;

			for (int k = 0; k < getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] == false) {
					for (int j = 0; j < card_count; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else if (_open_card_player[k] == true) {
					for (int j = 0; j < card_count; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}

					tableResponse_yiyang_ox.setCurOxValue(-1);
					tableResponse_yiyang_ox.setCurTimes(0);
					tableResponse_yiyang_ox.clearSortCard();
				} else {
					for (int j = 0; j < card_count; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}

				tableResponse_yiyang_ox.addCardsData(k, cards);
			}
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_yiyang_ox));

		observers().send(player, roomResponse);

		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(Player player) {
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			control_game_start();
		}

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && player != null) {
			table_response_observer(player);
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

				observers().send(player, roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		// if (istrustee[seat_index]) {
		// istrustee[seat_index] = false;
		//
		// if (_trustee_schedule[seat_index] != null) {
		// _trustee_schedule[seat_index].cancel(false);
		// _trustee_schedule[seat_index] = null;
		// }
		//
		// _trustee_type[seat_index] = 0;
		// }

		if (is_game_start == 1) {
			control_game_start();
		}

		if (istrustee[seat_index]) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIstrustee(istrustee[seat_index]);
			send_response_to_room(roomResponse);
		}

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_YI_YANG_OX_RECONNECT_DATA);

			TableResponseYiYangOx.Builder tableResponse_yiyang_ox = TableResponseYiYangOx.newBuilder();

			load_player_info_data_reconnect(tableResponse_yiyang_ox);

			int display_time = _cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - _operate_start_time);

			RoomInfo.Builder room_info = getRoomInfo();
			tableResponse_yiyang_ox.setRoomInfo(room_info);
			tableResponse_yiyang_ox.setBankerPlayer(_cur_banker);
			tableResponse_yiyang_ox.setDisplayTime(display_time);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				tableResponse_yiyang_ox.addCallButton(_player_call_banker[i]);
				tableResponse_yiyang_ox.addJettonScore(_jetton_player[i]);
				tableResponse_yiyang_ox.addOpenCard(_open_card_player[i]);

				if (_open_card_player[i] == true) {
					tableResponse_yiyang_ox.addOxValue(_ox_value[i]);
					tableResponse_yiyang_ox.addTimes(_player_times[i]);
				} else {
					tableResponse_yiyang_ox.addOxValue(-1);
					tableResponse_yiyang_ox.addTimes(0);
				}
			}

			tableResponse_yiyang_ox.setSceneStatus(_game_status);

			if (_game_status == GameConstants.GS_OX_CALL_BANKER) {
				tableResponse_yiyang_ox.setBankerPlayer(-1);
				int card_count = 4;

				if (getRuleValue(GameConstants.GAME_RULE_KSZQZ_OX) == 1) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

						if (_player_status[k] == false) {
							for (int j = 0; j < card_count; j++) {
								cards.addItem(GameConstants.INVALID_CARD);
							}
						} else if (k == seat_index) {
							for (int j = 0; j < card_count; j++) {
								cards.addItem(GRR._cards_data[k][j]);
							}
						} else {
							for (int j = 0; j < card_count; j++) {
								cards.addItem(GameConstants.BLACK_CARD);
							}
						}

						tableResponse_yiyang_ox.addCardsData(k, cards);
					}
				}
			}

			if (_game_status == GameConstants.GS_OX_ADD_JETTON) {
				int card_count = 4;

				if (getRuleValue(GameConstants.GAME_RULE_KSZQZ_OX) == 1) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

						if (_player_status[k] == false) {
							for (int j = 0; j < card_count; j++) {
								cards.addItem(GameConstants.INVALID_CARD);
							}
						} else if (k == seat_index) {
							for (int j = 0; j < card_count; j++) {
								cards.addItem(GRR._cards_data[k][j]);
							}
						} else {
							for (int j = 0; j < card_count; j++) {
								cards.addItem(GameConstants.BLACK_CARD);
							}
						}

						tableResponse_yiyang_ox.addCardsData(k, cards);
					}
				}
			}

			if (_game_status == GameConstants.GS_OX_OPEN_CARD) {
				int card_count = 5;

				for (int k = 0; k < getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if (_player_status[k] == false) {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.INVALID_CARD);
						}
					} else if (k == seat_index || _open_card_player[k] == true) {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}

						if (k == seat_index) {
							tableResponse_yiyang_ox.setCurOxValue(_ox_value[seat_index]);
							tableResponse_yiyang_ox.setCurTimes(_player_times[seat_index]);
						}

						int temp_cards_data[] = new int[GameConstants.OX_MAX_CARD_COUNT];
						for (int index = 0; index < GameConstants.OX_MAX_CARD_COUNT; index++) {
							temp_cards_data[index] = GRR._cards_data[seat_index][index];
						}

						_logic.get_ox_card(temp_cards_data, GameConstants.OX_MAX_CARD_COUNT, ruleMap);

						tableResponse_yiyang_ox.clearSortCard();

						for (int index = 0; index < GameConstants.OX_MAX_CARD_COUNT; index++) {
							tableResponse_yiyang_ox.addSortCard(temp_cards_data[index]);
						}
					} else {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}

					tableResponse_yiyang_ox.addCardsData(k, cards);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_yiyang_ox));

			send_response_to_player(seat_index, roomResponse);

			if (_game_status == GameConstants.GS_OX_CALL_BANKER && _player_status[seat_index] == true) {
				tableResponse_yiyang_ox.setBankerPlayer(-1);

				if (_player_call_banker[seat_index] == -1) {
					update_button(0, seat_index, _idi_call_banker_time, false);
				}
			}

			if (_game_status == GameConstants.GS_OX_ADD_JETTON && _player_status[seat_index] == true) {
				if (_jetton_player[seat_index] == 0) {
					update_button(1, seat_index, _idi_jetton_score_time, false);
				}
			}

			if (_game_status == GameConstants.GS_OX_OPEN_CARD && _player_status[seat_index] == true) {
				if (_open_card_player[seat_index] == false) {
					update_button(2, seat_index, _idi_open_card_time, false);
				}
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

				send_response_to_player(seat_index, roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		return true;
	}

	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		return true;
	}

	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		return true;
	}

	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_YI_YANG_OX_OPERATE) {
			Opreate_YiYangOx_Request req = PBUtil.toObject(room_rq, Opreate_YiYangOx_Request.class);

			if (req == null) {
				log_error("req = null");

				return false;
			}

			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getAddJettonScore());
		}

		return true;
	}

	// 1：抢庄，2：下注，3：开牌
	public boolean handler_requst_opreate(int seat_index, int operate_type, int sub_index) {
		if (operate_type < 1 || operate_type > 3) {
			log_error("命令不在范围内" + seat_index);
		}

		switch (operate_type) {
		case 1: {
			return call_banker(seat_index, sub_index);
		}
		case 2: {
			return result_jetton(seat_index, sub_index);
		}
		case 3: {
			return open_card(seat_index);
		}
		}

		return true;
	}

	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

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
			playerNumber = getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			if (_game_scheduled != null) {
				kill_timer();
			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_request_release_time = System.currentTimeMillis() + delay * 1000;

			if (GRR == null) {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT),
						delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT),
						delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			int count = 0;
			for (int i = 0; i < playerNumber; i++) {
				if (_gameRoomRecord.release_players[i] == 1) {
					count++;
				}
			}

			if (count == playerNumber) {
				if (GRR == null) {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < playerNumber; j++) {
					player = get_players()[j];
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

			send_response_to_room(roomResponse);

			return false;
		}
		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

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

			send_response_to_room(roomResponse);

			for (int i = 0; i < playerNumber; i++) {
				if (get_players()[i] == null)
					continue;

				if (_gameRoomRecord.release_players[i] != 1) {
					return false;
				}
			}

			for (int j = 0; j < playerNumber; j++) {
				player = get_players()[j];
				if (player == null)
					continue;

				send_error_notify(j, 1, "游戏解散成功!");
			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_release_scheduled = null;

			if (GRR == null) {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}
		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}

			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			set_timer(_cur_game_timer, _cur_operate_time, false);
			set_trustee_timer(_cur_game_timer, _cur_operate_time, false);

			if (_cur_operate_time > 0) {
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

			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_release_scheduled = null;

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < playerNumber; j++) {
				player = get_players()[j];
				if (player == null)
					continue;

				send_error_notify(j, 1, "游戏解散失败!玩家[" + get_players()[seat_index].getNick_name() + "]不同意解散");
			}

			return false;
		}
		case GameConstants.Release_Room_Type_CANCEL: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT); // 直接拉出游戏
				send_response_to_room(roomResponse);

				for (int i = 0; i < playerNumber; i++) {
					Player p = get_players()[i];
					if (p == null)
						continue;

					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}
				}

				huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);

				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
			} else {
				return false;
			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: { // 玩家未开始游戏 退出
			if (_player_status[seat_index] == true) {
				if (GameConstants.GS_MJ_FREE != _game_status) {
					return false;
				}

				send_error_notify(seat_index, 2, "您已经开始游戏了,不能退出游戏");

				return false;
			}

			if (get_players()[seat_index] != null) {
				if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id())
					_owner_seat = -1;

			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏

			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");

			get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), player.getAccount_id());

			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

			load_player_info_data(refreshroomResponse);
			send_response_to_other(seat_index, refreshroomResponse);

			is_game_start();

			refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_room(roomResponse);

			for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) {
				Player p = get_players()[i];
				if (p == null)
					continue;

				send_error_notify(i, 2, "游戏已被创建者解散");
			}

			huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);

			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
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

	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return send_response_to_room(roomResponse);
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

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS); // 刷新玩家
		load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

		return true;
	}

	private PlayerResultYiYangOx.Builder process_player_result_yiyangox(int reason) {
		PlayerResultYiYangOx.Builder player_result = PlayerResultYiYangOx.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player_result.addPlayerScore((int) _player_result.game_score[i]);
		}

		return player_result;
	}

	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null)
				continue;

			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];

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

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;
	}

	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}

		if (GRR == null)
			return;

		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;

			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
	}

	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
	}

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

		if (istrustee[get_seat_index] == true) {
			set_trustee_timer(_cur_game_timer, GameConstants.OX_TRUESTEE_OPERATE_TIME, false);
		} else {
			if (_trustee_schedule[get_seat_index] != null) {
				_trustee_schedule[get_seat_index].cancel(false);
				_trustee_schedule[get_seat_index] = null;
			}
		}

		send_response_to_room(roomResponse);

		return false;
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
		if (getRuleValue(GameConstants.GAME_RULE_GAME_RULE_PLAYER_FIVE) == 1) {
			return 5;
		}
		if (getRuleValue(GameConstants.GAME_RULE_GAME_RULE_PLAYER_SEVEN) == 1) {
			return 7;
		}
		return 5;
	}

	@Override
	public boolean exe_finish(int reason) {
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();
		float[] scores = _player_result.getGame_score();

		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());

			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(get_players()[i].getAccount_id(), score, false, buf.toString(),
					EMoneyOperateType.ROOM_COST);

			if (addGoldResultModel.isSuccess() == false) {
				logger.error("玩家:" + get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}

			scores[i] = 0;
		}
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS); // 刷新玩家
		load_player_info_data(roomResponse);
		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	@Override
	public boolean robot_banker_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (_player_call_banker[i] == -1) {
				call_banker(i, 0);
			}
		}

		return false;
	}

	@Override
	public boolean ready_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] != null) {
				if (_player_ready[i] == 0) {
					handler_player_ready(i, false);
				}
			}
		}

		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (i == _cur_banker)
				continue;

			if (_jetton_player[i] == 0) {
				result_jetton(i, 0);
			}
		}

		return false;
	}

	@Override
	public boolean open_card_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (_open_card_player[i] == false) {
				open_card(i);
			}
		}

		return false;
	}

	@Override
	public boolean animation_timer(int timer_id) {
		switch (timer_id) {
		case ID_TIMER_ANIMATION_START: {
			if (getRuleValue(GameConstants.GAME_RULE_BA_WANG_ZHUANG) == 1) {
				if (_cur_round == 1) {
					int chairID[] = new int[getTablePlayerNumber()];
					int chair_count = 0;
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (_player_status[i] == true) {
							chairID[chair_count++] = i;
						}
					}
					if (chair_count > 0) {
						int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
						int temp = rand % chair_count;
						_cur_banker = chairID[temp];
					}
				}

				_player_call_banker[_cur_banker] = 1;

				set_banker_to_room();

				_game_status = GameConstants.GS_OX_ADD_JETTON;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_status[i] == false)
						continue;

					update_button(1, i, _idi_jetton_score_time, true);
				}

				if (_game_scheduled != null)
					kill_timer();

				set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
				set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			} else if (getRuleValue(GameConstants.GAME_RULE_PAI_DA_DANG_ZHUANG) == 1) {
				if (_cur_round == 1) {
					int chairID[] = new int[getTablePlayerNumber()];
					int chair_count = 0;
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (_player_status[i] == true) {
							chairID[chair_count++] = i;
						}
					}
					if (chair_count > 0) {
						int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
						int temp = rand % chair_count;
						_cur_banker = chairID[temp];
					}
				} else {
					_cur_banker = _next_banker;
				}

				_player_call_banker[_cur_banker] = 1;

				set_banker_to_room();

				_game_status = GameConstants.GS_OX_ADD_JETTON;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_status[i] == false)
						continue;

					update_button(1, i, _idi_jetton_score_time, true);
				}

				if (_game_scheduled != null)
					kill_timer();

				set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
				set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			} else if (getRuleValue(GameConstants.GAME_RULE_LLSZ_OX) == 1) {
				if (_cur_round == 1) {
					boolean flag = false;
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (_player_status[i] == true) {
							if (get_players()[i].getAccount_id() == getCreate_player().getAccount_id()) {
								_cur_banker = i;
								flag = true;
								break;
							}
						}
					}
					if (flag == false) {
						int chairID[] = new int[getTablePlayerNumber()];
						int chair_count = 0;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (_player_status[i] == true) {
								chairID[chair_count++] = i;
							}
						}
						if (chair_count > 0) {
							int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
							int temp = rand % chair_count;
							_cur_banker = chairID[temp];
						}
					}
				} else {
					int next_banker = _next_banker;
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						next_banker = (_next_banker + i) % getTablePlayerNumber();

						if (_next_banker == next_banker)
							continue;

						if (_player_status[next_banker] == true)
							break;
					}
					_cur_banker = next_banker;
				}

				_player_call_banker[_cur_banker] = 1;

				set_banker_to_room();

				_game_status = GameConstants.GS_OX_ADD_JETTON;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_status[i] == false)
						continue;
					update_button(1, i, _idi_jetton_score_time, true);
				}

				if (_game_scheduled != null)
					kill_timer();

				set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
				set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			} else if (getRuleValue(GameConstants.GAME_RULE_KSZQZ_OX) == 1) {
				send_card(4, 0, false);

				_game_status = GameConstants.GS_OX_CALL_BANKER;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_status[i] == false)
						continue;
					update_button(0, i, _idi_call_banker_time, true);
				}

				if (_game_scheduled != null)
					kill_timer();

				set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, _idi_call_banker_time, true);
				set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, _idi_call_banker_time, true);
			} else if (getRuleValue(GameConstants.GAME_RULE_BU_KAN_PAI_QIANG_ZHUANG) == 1) {
				_game_status = GameConstants.GS_OX_CALL_BANKER;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_status[i] == false)
						continue;
					update_button(0, i, _idi_call_banker_time, true);
				}

				if (_game_scheduled != null)
					kill_timer();

				set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, _idi_call_banker_time, true);
				set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, _idi_call_banker_time, true);
			}
			return true;
		}
		case ID_TIMER_ANIMATION_ROBOT: {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_status[i] == false)
					continue;

				update_button(1, i, _idi_jetton_score_time, true);
			}

			if (_game_scheduled != null)
				kill_timer();

			set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);
			set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, _idi_jetton_score_time, true);

			return true;
		}
		}

		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		if (operate_id == GameConstants.HJK_READY_TIMER) {
			handler_player_ready(seat_index, false);
		} else if (operate_id == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			if (_player_status[seat_index] == false)
				return true;

			if (has_trustee(GameConstants.GAME_RULE_TG_NO_BNAKER, seat_index)) {
				call_banker(seat_index, 0);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_ONE_BEI, seat_index)) {
				call_banker(seat_index, 1);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_TWO_BEI, seat_index)) {
				call_banker(seat_index, 2);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_THREE_BEI, seat_index)) {
				call_banker(seat_index, 3);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_BEI, seat_index)) {
				call_banker(seat_index, 4);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_IS_ROBOT_BANKER, seat_index)) {
				call_banker(seat_index, 1);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_NO_BNAKER, seat_index)) {
				call_banker(seat_index, 0);
			}
		} else if (operate_id == GameConstants.HJK_ADD_JETTON_TIMER) {
			if (_player_status[seat_index] == false)
				return true;

			if (_cur_banker == seat_index)
				return true;

			if (_player_status[seat_index] == false)
				return true;

			if (has_trustee(GameConstants.GAME_RULE_TG_IS_TUI_ZHU, seat_index) && _can_tuizhu_player[seat_index] > 0) {
				result_jetton(seat_index, _jetton_count[seat_index] - 1);
			} else if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_ONE_TO_FIVE) == 1) {
				if (has_trustee(GameConstants.GAME_RULE_TG_ONE_FEN, seat_index)) {
					result_jetton(seat_index, 0);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
					result_jetton(seat_index, 1);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_THREE_FEN, seat_index)) {
					result_jetton(seat_index, 2);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
					result_jetton(seat_index, 3);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FIVE_FEN, seat_index)) {
					result_jetton(seat_index, 4);
				}
			} else if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_TWO_TO_TEN) == 1) {
				if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
					result_jetton(seat_index, 0);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
					result_jetton(seat_index, 1);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_SIX_FEN, seat_index)) {
					result_jetton(seat_index, 2);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_EIGHT_FEN, seat_index)) {
					result_jetton(seat_index, 3);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_TEN_FEN, seat_index)) {
					result_jetton(seat_index, 4);
				}
			} else if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_FIVE_TO_TEN) == 1) {
				if (has_trustee(GameConstants.GAME_RULE_TG_FIVE_FEN, seat_index)) {
					result_jetton(seat_index, 0);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_SIX_FEN, seat_index)) {
					result_jetton(seat_index, 1);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_SEVEN_FEN, seat_index)) {
					result_jetton(seat_index, 2);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_EIGHT_FEN, seat_index)) {
					result_jetton(seat_index, 3);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_NINE_FEN, seat_index)) {
					result_jetton(seat_index, 4);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_TEN_FEN, seat_index)) {
					result_jetton(seat_index, 5);
				}
			} else if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_ONE_TO_THREE) == 1) {
				if (has_trustee(GameConstants.GAME_RULE_TG_ONE_FEN, seat_index)) {
					result_jetton(seat_index, 0);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
					result_jetton(seat_index, 1);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_THREE_FEN, seat_index)) {
					result_jetton(seat_index, 2);
				}
			} else if (getRuleValue(GameConstants.GAME_RULE_XIA_ZHU_TWO_TO_FIVE) == 1) {
				if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
					result_jetton(seat_index, 0);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_THREE_FEN, seat_index)) {
					result_jetton(seat_index, 1);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
					result_jetton(seat_index, 2);
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FIVE_FEN, seat_index)) {
					result_jetton(seat_index, 3);
				}
			} else if (getRuleValue(GameConstants.GAME_RULE_GU_DING_YA_FEN) == 1) {
				result_jetton(seat_index, 0);
			}
		} else if (operate_id == GameConstants.HJK_OPEN_CARD_TIMER) {
			if (_player_status[seat_index] == false)
				return true;

			open_card(seat_index);
		}

		return false;
	}
}
