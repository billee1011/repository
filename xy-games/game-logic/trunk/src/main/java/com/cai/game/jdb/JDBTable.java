/**
 * 
 */
package com.cai.game.jdb;

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
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.LiangCardRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

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
import protobuf.clazz.dbn.dbnRsp.FirstSeat;
import protobuf.clazz.jdb.jdbRsp.ButtonOperateJdb;
import protobuf.clazz.jdb.jdbRsp.GameStartJdb;
import protobuf.clazz.jdb.jdbRsp.JettonResultJdb;
import protobuf.clazz.jdb.jdbRsp.LiangCardJdb;
import protobuf.clazz.jdb.jdbRsp.OpenCardJdb;
import protobuf.clazz.jdb.jdbRsp.Opreate_Jdb_Request;
import protobuf.clazz.jdb.jdbRsp.PauseGameJdb;
import protobuf.clazz.jdb.jdbRsp.PlayerListJdb;
import protobuf.clazz.jdb.jdbRsp.PlayerResultJdb;
import protobuf.clazz.jdb.jdbRsp.PopupMessage;
import protobuf.clazz.jdb.jdbRsp.PukeGameEndJdb;
import protobuf.clazz.jdb.jdbRsp.RecordList;
import protobuf.clazz.jdb.jdbRsp.ReturnDataJdb;
import protobuf.clazz.jdb.jdbRsp.RoomInfoJdb;
import protobuf.clazz.jdb.jdbRsp.RoomPlayInfo;
import protobuf.clazz.jdb.jdbRsp.RoomPlayerResponseJdb;
import protobuf.clazz.jdb.jdbRsp.SelectdBankerJdb;
import protobuf.clazz.jdb.jdbRsp.SelectdBankerResultJdb;
import protobuf.clazz.jdb.jdbRsp.SendCardJdb;
import protobuf.clazz.jdb.jdbRsp.TableResponseJdb;

//public static class RecordItem {
//	public int  cur_round ;          //局数
//	public int cards_data[][] = new int[8][2] ; //扑克列表
//	public int jetton_player[][] = new int[8][8]; //用户下注
//	public int  end_score [][] = new int[][];//结算分数
//	repeated int32 area_status = 12; //区域输赢
//}
///////////////////////////////////////////////////////////////////////////////////////////////
public class JDBTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;
	private static final int ID_TIMER_ANIMATION_START = 1;
	private static final int ID_TIMER_ANIMATION_ROBOT = 2;
	private static final int ID_TIMER_ANIMATION_QIE_DALAY = 3;
	private static final int ID_TIMER_JETTON_ONE = 4;
	private static final int ID_TIMER_JETTON_TWO = 5;
	private static final int ID_TIMER_JETTON_THREE = 6;
	private static final int ID_TIMER_LIANG_PAI = 7;
	private static final int ID_TIMER_GO_TO = 8;

	private static Logger logger = Logger.getLogger(JDBTable.class);

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

	public int is_game_start; // 游戏是否开始

	public ScheduledFuture _trustee_schedule[];// 托管定时器
	public ScheduledFuture _liang_schedule[]; // 亮牌

	public static JDBGameLogic _logic = new JDBGameLogic();

	//
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
	public int _idi_jetton_time_one; // 下注时间一
	public int _idi_jetton_time_two; // 下注时间二
	public int _idi_jetton_time_three; // 下注时间三
	public int _idi_open_card_time; // 开牌时间
	public int _idi_ready_card_time; // 等待时间
	public int _qie_card; // 切的牌
	public int _jetton_score[]; // 下注按钮
	public int _jetton_count; // 下注按钮个数
	public long _jetton_player[][]; // 用户下注
	public int _open_card_player[]; // 用户开牌
	public boolean _liang_card_player[]; // 用户亮牌
	public int _player_times[]; // 用户倍数
	public int _ox_value[]; // 牛值
	public boolean _player_status[]; // 游戏状态
	public int _call_banker_info[]; // 叫庄信息
	public int _banker_times; // 庄家倍数
	public int _player_call_banker[]; // 用户是否叫庄
	public int _liang_card_sort[]; // 亮牌的顺序
	public int _liang_card_count; // 亮牌用户的数量
	public int _tong_sha_count[]; // 通杀次数
	public int _tong_pei_count[]; // 通赔次数
	public int _niu_niu_count[]; // 牛牛次数
	public int _no_niu_count[]; // 无牛次数
	public int _victory_count[]; // 胜利的次数
	public int _owner_seat; // 庄家位置
	public String _str_ox_value[]; // 牛几
	public int _operate_start_time; // 操作开始时间
	public int _cur_operate_time; // 可操作时间
	public int _cur_game_timer; // 当前游戏定时器
	public long _min_carry_momey;// 最低携带量
	public long _max_carry_momey;// 最大携带量
	public long _init_momey; // 初始分
	public long _enter_gold; // 入场扣豆
	public long _bu_fen_glod; // 每次补分扣豆
	public long _add_banker_glod;// 添庄扣豆
	public long _min_bu_money; // 最低补分
	public long _max_bu_money; // 最高补分
	public int _start_game_player; // 游戏开始最少人数
	public int _game_interval; // 游戏时间
	public int _first_seat;
	public int _jetton_seat[]; // 下注的方位
	public long _jetton_area[]; // 下注分数
	public long _jetton_player_all[]; // 单个用户下注和
	public long _max_player_jetton[]; // 每个用户最多可以 下注
	public long _hou_shou_score; // 庄家后手分
	public int _area_status[]; // 区域输赢状态
	public int _add_banker_times[]; // 添庄次数
	public int _next_banker; // 下一局的庄家
	public int _operate_type[]; // 操作类型
	public int _operate_button[]; // 操作按钮
	public String _operate_str[]; // 操作描述
	public boolean _restart_room_own; // 房主重新开始
	public int _is_pass_jetton[]; // 放弃下注 0:没有按钮， 1：有按钮， 2：已经点了
	public long _max_jetton_score; // 最大下注分数
	public long _start_timer; // 开始 时间
	public boolean _is_pause_game; // 是否暂时游戏
	public boolean _is_pause_cancel_banker; // 停止游戏后下庄
	public int _last_banker;
	public int _left_card_count; // 剩余张数
	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;
	private ScheduledFuture _game_scheduled;
	public List<GameRecordInfo> record_info_list;
	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public JDBTable() {
		super(RoomType.SG, GameConstants.JDB_SIT_MAX);

		// /_logic = new JDBGameLogic();
		_jetton_round = 0;
		_first_seat = GameConstants.INVALID_SEAT;
		_next_banker = GameConstants.INVALID_SEAT;
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
		record_info_list = new ArrayList<GameRecordInfo>();
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
		playerNumber = 0;
		_jetton_total_score = 0;

		_cur_banker = 0;
		_banker_up_score = 0;
		_banker_bu_score = 0;
		_jetton_min_score = 0;
		_owner_account_id = -1;
		_cur_banker_score = 0;
		_down_min_banker_score = 10;
		_first_player = -1;
		_banker_count = 0;
		_idi_call_banker_time = 10; // 切牌时间
		_idi_jetton_score_time = 5; // 下注时间
		_idi_jetton_time_one = 15; // 下注时间一
		_idi_jetton_time_two = 10; // 下注时间二
		_idi_jetton_time_three = 10; // 下注时间三
		_idi_open_card_time = 20; // 开牌时间
		_idi_ready_card_time = 10; // 等待时间
		_banker_operate = 0;
		_is_first = -1;
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_jetton_count = 3;
		_is_bu_banker = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[4];
		_jetton_player = new long[getTablePlayerNumber()][getTablePlayerNumber()]; // 用户下注
		_open_card_player = new int[getTablePlayerNumber()]; // 用户开牌
		_liang_card_player = new boolean[getTablePlayerNumber()]; // 用户亮牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		_start_timer = 0;
		_is_pause_game = false;
		_is_pause_cancel_banker = false;

		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_is_bu_banker, false);
		Arrays.fill(_jetton_score, 0);
		Arrays.fill(_open_card_player, -1);
		Arrays.fill(_liang_card_player, false);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				_jetton_player[i][j] = 0;
			}
		}

		_player_status = new boolean[getTablePlayerNumber()];
		Arrays.fill(_player_status, false);
		_call_banker_info = new int[getTablePlayerNumber()]; // 叫庄信息
		Arrays.fill(_call_banker_info, 0);
		_player_call_banker = new int[getTablePlayerNumber()];
		Arrays.fill(_player_call_banker, -1);
		_banker_times = 1; // 庄家倍数
		_liang_card_sort = new int[getTablePlayerNumber()]; // 亮牌的顺序
		Arrays.fill(_liang_card_sort, -1);
		_liang_card_count = 0; // 亮牌用户的数量
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
		_str_ox_value = new String[this.getTablePlayerNumber()]; // 牛几
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_str_ox_value[i] = "";
		}
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[this.getTablePlayerNumber()];
		}
		if (_liang_schedule == null) {
			_liang_schedule = new ScheduledFuture[this.getTablePlayerNumber()];
		}
		_min_carry_momey = 600;// 最低携带量
		_max_carry_momey = 4000;// 最大携带量
		_init_momey = 200; // 初始分
		_enter_gold = 20; // 入场扣豆
		_bu_fen_glod = 10; // 每次补分扣豆
		_add_banker_glod = 8;// 添庄扣豆
		_min_bu_money = 200; // 最低补分
		_max_bu_money = 1000; // 最高补分
		_start_game_player = 2; // 游戏开始最少人数
		_game_interval = 30; // 游戏时间
		_owner_seat = -1;
		_hou_shou_score = 0; // 庄家后首分
		_restart_room_own = false;
		_is_pause_cancel_banker = false;
		this._max_jetton_score = 0;
		_jetton_area = new long[this.getTablePlayerNumber()];
		Arrays.fill(_jetton_area, 0);
		_jetton_player_all = new long[this.getTablePlayerNumber()];
		Arrays.fill(_jetton_player_all, 0);
		_max_player_jetton = new long[this.getTablePlayerNumber()]; // 每个用户最多可以
																	// 下注
		Arrays.fill(_max_player_jetton, 0);
		_area_status = new int[getTablePlayerNumber()];
		Arrays.fill(_area_status, -1);
		_add_banker_times = new int[this.getTablePlayerNumber()];
		Arrays.fill(_add_banker_times, 0);
		_operate_type = new int[this.getTablePlayerNumber()]; // 操作类型
		Arrays.fill(_operate_type, 0);
		_operate_button = new int[this.getTablePlayerNumber()];
		Arrays.fill(_operate_button, 0);
		_is_pass_jetton = new int[this.getTablePlayerNumber()];
		Arrays.fill(_is_pass_jetton, 0);
		_operate_str = new String[this.getTablePlayerNumber()];

	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_cur_banker = -1;
		_last_banker = -1;
		_left_card_count = 54;
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_game_round = 100;
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
		_idi_call_banker_time = 10; // 切牌时间
		_idi_jetton_score_time = 10; // 下注时间
		_idi_open_card_time = 20; // 开牌时间
		_idi_ready_card_time = 10; // 等待时间
		_qie_card = 0; // 切的牌
		_all_card_len = 0; // 总牌数
		_jetton_count = 3;
		_is_bu_banker = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[4];
		_jetton_player = new long[getTablePlayerNumber()][getTablePlayerNumber()]; // 用户下注
		_open_card_player = new int[getTablePlayerNumber()]; // 用户开牌
		_liang_card_player = new boolean[getTablePlayerNumber()]; // 用户亮牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		record_info_list = new ArrayList<GameRecordInfo>();
		_owner_seat = -1;
		_hou_shou_score = 0; // 庄家后首分
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_is_bu_banker, false);
		Arrays.fill(_jetton_score, 0);
		Arrays.fill(_open_card_player, -1);
		Arrays.fill(_liang_card_player, false);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				_jetton_player[i][j] = 0;
			}
		}

		_player_status = new boolean[getTablePlayerNumber()];
		Arrays.fill(_player_status, false);
		_call_banker_info = new int[getTablePlayerNumber()]; // 叫庄信息
		Arrays.fill(_call_banker_info, 0);
		_player_call_banker = new int[getTablePlayerNumber()];
		Arrays.fill(_player_call_banker, -1);
		_banker_times = 1; // 庄家倍数
		_liang_card_sort = new int[getTablePlayerNumber()]; // 亮牌的顺序
		Arrays.fill(_liang_card_sort, -1);
		_liang_card_count = 0; // 亮牌用户的数量
		_tong_sha_count = new int[this.getTablePlayerNumber()]; // 通杀次数
		Arrays.fill(_tong_sha_count, 0);
		_tong_pei_count = new int[this.getTablePlayerNumber()]; // 通赔次数
		Arrays.fill(_tong_pei_count, 0);
		_niu_niu_count = new int[this.getTablePlayerNumber()]; // 牛牛次数
		Arrays.fill(_niu_niu_count, 0);
		_no_niu_count = new int[this.getTablePlayerNumber()]; // 牛牛次数
		Arrays.fill(_no_niu_count, 0);
		_victory_count = new int[this.getTablePlayerNumber()]; // 胜利的次数
		Arrays.fill(_victory_count, 0);
		_str_ox_value = new String[this.getTablePlayerNumber()]; // 牛几
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_str_ox_value[i] = "";
		}
		_add_banker_times = new int[this.getTablePlayerNumber()];
		Arrays.fill(_add_banker_times, 0);
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[getTablePlayerNumber()];
		}
		if (_liang_schedule == null) {
			_liang_schedule = new ScheduledFuture[getTablePlayerNumber()];
		}
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());

		_min_carry_momey = 600;// 最低携带量
		_max_carry_momey = 4000;// 最大携带量
		_init_momey = 200; // 初始分
		_enter_gold = 20; // 入场扣豆
		_bu_fen_glod = 10; // 每次补分扣豆
		_add_banker_glod = 8;// 添庄扣豆
		_min_bu_money = 200; // 最低补分
		_max_bu_money = 1000; // 最高补分
		_start_game_player = 2; // 游戏开始最少人数
		_game_interval = 30; // 游戏时间
		_first_seat = GameConstants.INVALID_SEAT;
		_jetton_seat = new int[getTablePlayerNumber() / 2];
		this._max_jetton_score = 0;
		_is_pause_cancel_banker = false;
		_jetton_area = new long[this.getTablePlayerNumber()];
		Arrays.fill(_jetton_area, 0);
		_max_player_jetton = new long[this.getTablePlayerNumber()]; // 每个用户最多可以
																	// 下注
		Arrays.fill(_max_player_jetton, 0);
		_jetton_player_all = new long[this.getTablePlayerNumber()];
		Arrays.fill(_jetton_player_all, 0);
		for (int i = 0; i < this.getTablePlayerNumber() / 2; i++) {
			_jetton_seat[i] = i * 2;
		}
		_area_status = new int[getTablePlayerNumber()];
		Arrays.fill(_area_status, -1);
		_add_banker_times = new int[this.getTablePlayerNumber()];
		Arrays.fill(_add_banker_times, 0);
		_start_game_player = this.getRuleValue(GameConstants.GAME_RULE_JDB_PEOPEL);
		_game_interval = this.getRuleValue(GameConstants.GAME_RULE_JDB_MIN);
		_operate_type = new int[this.getTablePlayerNumber()]; // 操作类型
		Arrays.fill(_operate_type, 0);
		_operate_button = new int[this.getTablePlayerNumber()];
		Arrays.fill(_operate_button, 0);
		_is_pass_jetton = new int[this.getTablePlayerNumber()];
		Arrays.fill(_is_pass_jetton, 0);
		_operate_str = new String[this.getTablePlayerNumber()];
		_restart_room_own = false;
		if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_ONE) == 1) {
			_min_carry_momey = 600;// 最低携带量
			_max_carry_momey = 4000;// 最大携带量
			_init_momey = 200; // 初始分
			_enter_gold = 20; // 入场扣豆
			_bu_fen_glod = 10; // 每次补分扣豆
			_add_banker_glod = 8;// 添庄扣豆
			_min_bu_money = 200; // 最低补分
			_max_bu_money = 1000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_TWO) == 1) {
			_min_carry_momey = 1200;// 最低携带量
			_max_carry_momey = 8000;// 最大携带量
			_init_momey = 400; // 初始分
			_enter_gold = 40; // 入场扣豆
			_bu_fen_glod = 20; // 每次补分扣豆
			_add_banker_glod = 16;// 添庄扣豆
			_min_bu_money = 400; // 最低补分
			_max_bu_money = 2000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_THREE) == 1) {
			_min_carry_momey = 3000;// 最低携带量
			_max_carry_momey = 20000;// 最大携带量
			_init_momey = 1000; // 初始分
			_enter_gold = 100; // 入场扣豆
			_bu_fen_glod = 50; // 每次补分扣豆
			_add_banker_glod = 40;// 添庄扣豆
			_min_bu_money = 1000; // 最低补分
			_max_bu_money = 5000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_FOUR) == 1) {
			_min_carry_momey = 6000;// 最低携带量
			_max_carry_momey = 40000;// 最大携带量
			_init_momey = 2000; // 初始分
			_enter_gold = 200; // 入场扣豆
			_bu_fen_glod = 100; // 每次补分扣豆
			_add_banker_glod = 80;// 添庄扣豆
			_min_bu_money = 2000; // 最低补分
			_max_bu_money = 10000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_FIVE) == 1) {
			_min_carry_momey = 12000;// 最低携带量
			_max_carry_momey = 80000;// 最大携带量
			_init_momey = 4000; // 初始分
			_enter_gold = 400; // 入场扣豆
			_bu_fen_glod = 200; // 每次补分扣豆
			_add_banker_glod = 160;// 添庄扣豆
			_min_bu_money = 4000; // 最低补分
			_max_bu_money = 10000; // 最高补分
		}
		if (this.getRuleValue(GameConstants.GAME_RULE_DZH) == 1) {
			_min_carry_momey = 200;// 最低携带量
			_max_carry_momey = 100000;// 最大携带量
			_init_momey = 8000; // 初始分
			_enter_gold = 200; // 入场扣豆
			_bu_fen_glod = 100; // 每次补分扣豆
			_add_banker_glod = 80;// 添庄扣豆
			_min_bu_money = 2000; // 最低补分
			_max_bu_money = 10000; // 最高补分
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

		SysParamModel sysParamModel7 = SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(7);

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.JDB_CARD_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_banker_operate = 0;
		if (sysParamModel7 != null) {
			_idi_ready_card_time = sysParamModel7.getVal1(); // 等待时间
			_idi_call_banker_time = sysParamModel7.getVal2(); // 切牌时间
			_idi_jetton_score_time = sysParamModel7.getVal3(); // 下注时间
			_idi_open_card_time = sysParamModel7.getVal4(); // 开牌时间
		}
		// else
		// {
		// _idi_ready_card_time = 5; //等待时间
		// _idi_call_banker_time = 10; //切牌时间
		// _idi_jetton_score_time = 10; //下注时间
		// _idi_open_card_time = 20; //开牌时间
		// }

		_qie_card = 0; // 切的牌
		_jetton_player = new long[getTablePlayerNumber()][getTablePlayerNumber()]; // 用户下注
		_open_card_player = new int[getTablePlayerNumber()]; // 用户开牌
		_liang_card_player = new boolean[getTablePlayerNumber()]; // 用户亮牌
		_player_times = new int[getTablePlayerNumber()];
		_ox_value = new int[getTablePlayerNumber()];
		_is_pause_cancel_banker = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				_jetton_player[i][j] = 0;
			}
		}
		Arrays.fill(_ox_value, -1);
		Arrays.fill(_player_times, 0);
		Arrays.fill(_open_card_player, -1);
		Arrays.fill(_liang_card_player, false);

		_player_status = new boolean[getTablePlayerNumber()];
		Arrays.fill(_player_status, false);
		_call_banker_info = new int[getTablePlayerNumber()]; // 叫庄信息
		Arrays.fill(_call_banker_info, 0);
		_player_call_banker = new int[getTablePlayerNumber()];
		Arrays.fill(_player_call_banker, -1);
		_banker_times = 1; // 庄家倍数
		_liang_card_sort = new int[getTablePlayerNumber()]; // 亮牌的顺序
		Arrays.fill(_liang_card_sort, -1);
		_liang_card_count = 0; // 亮牌用户的数量
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
		}
		_str_ox_value = new String[this.getTablePlayerNumber()]; // 牛几
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_str_ox_value[i] = "";
		}
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[this.getTablePlayerNumber()];
		}
		if (_liang_schedule == null) {
			_liang_schedule = new ScheduledFuture[this.getTablePlayerNumber()];
		}
		_cur_round++;

		istrustee = new boolean[4];
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
		this._max_jetton_score = 0;
		_jetton_area = new long[this.getTablePlayerNumber()];
		Arrays.fill(_jetton_area, 0);
		_max_player_jetton = new long[this.getTablePlayerNumber()]; // 每个用户最多可以
																	// 下注
		Arrays.fill(_max_player_jetton, 0);
		_jetton_player_all = new long[this.getTablePlayerNumber()];
		Arrays.fill(_jetton_player_all, 0);
		_first_seat = GameConstants.INVALID_SEAT;
		_jetton_seat = new int[getTablePlayerNumber() / 2];
		for (int i = 0; i < this.getTablePlayerNumber() / 2; i++) {
			_jetton_seat[i] = i * 2;
		}
		_area_status = new int[getTablePlayerNumber()];
		Arrays.fill(_area_status, -1);
		_is_pass_jetton = new int[this.getTablePlayerNumber()];
		Arrays.fill(_is_pass_jetton, 0);
		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_JDB_GAME_PLAY;

		reset_init_data();
		if (_cur_round == 1) {
			_start_timer = System.currentTimeMillis() / 1000;
		}
		_cur_jetton_round = 0;
		//
		if (this._all_card_len - this._left_card_count < 9) {
			_repertory_card = new int[GameConstants.JDB_MAX_CARD_COUNT];
		}
		shuffle(_repertory_card, GameConstants.CARD_DATA_JDB);

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

		for (int j = 0; j <= this._banker_times; j++)
			this._call_banker_info[j] = j;
		this._jetton_count = 3;
		for (int j = 0; j < this._jetton_count; j++)
			this._jetton_score[j] = j + 1;
		_start_game_player = this.getRuleValue(GameConstants.GAME_RULE_JDB_PEOPEL);
		_game_interval = this.getRuleValue(GameConstants.GAME_RULE_JDB_MIN);
		if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_ONE) == 1) {
			_min_carry_momey = 600;// 最低携带量
			_max_carry_momey = 4000;// 最大携带量
			_init_momey = 200; // 初始分
			_enter_gold = 20; // 入场扣豆
			_bu_fen_glod = 10; // 每次补分扣豆
			_add_banker_glod = 8;// 添庄扣豆
			_min_bu_money = 200; // 最低补分
			_max_bu_money = 1000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_TWO) == 1) {
			_min_carry_momey = 400;// 最低携带量
			_max_carry_momey = 8000;// 最大携带量
			_init_momey = 400; // 初始分
			_enter_gold = 40; // 入场扣豆
			_bu_fen_glod = 20; // 每次补分扣豆
			_add_banker_glod = 16;// 添庄扣豆
			_min_bu_money = 400; // 最低补分
			_max_bu_money = 2000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_THREE) == 1) {
			_min_carry_momey = 3000;// 最低携带量
			_max_carry_momey = 20000;// 最大携带量
			_init_momey = 1000; // 初始分
			_enter_gold = 100; // 入场扣豆
			_bu_fen_glod = 50; // 每次补分扣豆
			_add_banker_glod = 40;// 添庄扣豆
			_min_bu_money = 1000; // 最低补分
			_max_bu_money = 5000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_FOUR) == 1) {
			_min_carry_momey = 6000;// 最低携带量
			_max_carry_momey = 40000;// 最大携带量
			_init_momey = 2000; // 初始分
			_enter_gold = 200; // 入场扣豆
			_bu_fen_glod = 100; // 每次补分扣豆
			_add_banker_glod = 80;// 添庄扣豆
			_min_bu_money = 2000; // 最低补分
			_max_bu_money = 10000; // 最高补分
		} else if (this.getRuleValue(GameConstants.GAME_RULE_JDB_GRADE_FIVE) == 1) {
			_min_carry_momey = 12000;// 最低携带量
			_max_carry_momey = 80000;// 最大携带量
			_init_momey = 4000; // 初始分
			_enter_gold = 400; // 入场扣豆
			_bu_fen_glod = 200; // 每次补分扣豆
			_add_banker_glod = 160;// 添庄扣豆
			_min_bu_money = 4000; // 最低补分
			_max_bu_money = 10000; // 最高补分
		}
		if (this.getRuleValue(GameConstants.GAME_RULE_DZH) == 1) {
			_min_carry_momey = 200;// 最低携带量
			_max_carry_momey = 100000;// 最大携带量
			_init_momey = 8000; // 初始分
			_enter_gold = 200; // 入场扣豆
			_bu_fen_glod = 100; // 每次补分扣豆
			_add_banker_glod = 80;// 添庄扣豆
			_min_bu_money = 2000; // 最低补分
			_max_bu_money = 10000; // 最高补分
		}
		return game_start_jdb();
	}

	// 斗板牛开始
	public boolean game_start_jdb() {

		_game_status = GameConstants.GS_JDB_GAME_PLAY;// 设置状态
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}
		// 刷新手牌

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_JDB_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		GameStartJdb.Builder gamestart = GameStartJdb.newBuilder();
		RoomInfoJdb.Builder room_info = getRoomInfoJdb();
		gamestart.setRoomInfo(room_info);
		this.load_player_info_data_game_start(gamestart);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		this.send_response_to_room(roomResponse);
		//
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// this.load_common_status(roomResponse);
		// roomResponse.setType(MsgConstants.RESPONSE_JDB_GAME_START);
		// roomResponse.setGameStatus(this._game_status);
		// // 发送数据
		// GameStartJdb.Builder gamestart = GameStartJdb.newBuilder();
		// RoomInfoJdb.Builder room_info= getRoomInfoJdb();
		// gamestart.setRoomInfo(room_info);
		// this.load_player_info_data_game_start(gamestart);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		GRR.add_room_response(roomResponse);
		GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_START), 2500, TimeUnit.MILLISECONDS);

		return true;
	}

	public boolean update_button(int opr_type, int seat_index, int display_time, boolean is_grr) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_UPDATE_BTN);
		ButtonOperateJdb.Builder button_operate = ButtonOperateJdb.newBuilder();
		button_operate.setOpreateType(opr_type);
		button_operate.setDisplayTime(display_time);

		button_operate.clearButton();
		if (opr_type == 0 && seat_index % 2 == 0) {
			for (int j = 0; j <= this._banker_times; j++) {
				this._call_banker_info[j] = j;
				button_operate.addButton(this._call_banker_info[j]);
			}
		}
		if ((opr_type == 1 || opr_type == 2 || opr_type == 3) && seat_index != _cur_banker) {
			button_operate.setMaxTableJettonScore(this._max_jetton_score);
			if (this._is_pass_jetton[seat_index] == 1)
				button_operate.setIsShowpass(this._is_pass_jetton[seat_index]);
			if (_max_player_jetton[seat_index] < this._init_momey / 40 || this._is_pass_jetton[seat_index] == 2) {
				button_operate.setMinJettonScore(0);
				button_operate.setMaxJettonScore(0);
			} else {
				int min_score = (int) this._init_momey / 40;

				button_operate.setMinJettonScore(min_score);
				button_operate.setMaxJettonScore((int) _max_player_jetton[seat_index]);
			}
			for (int j = 0; j < _jetton_count; j++) {
				this._jetton_score[j] = j + 1;
				button_operate.addButton(this._jetton_score[j]);
			}
		}
		if (opr_type == 4) {

			for (int j = 0; j < 2 && seat_index % 2 == 0; j++) {
				button_operate.addButton(j);
			}
			int temp_cards[] = new int[GameConstants.JDB_CARD_COUNT];
			for (int i = 0; i < GameConstants.JDB_CARD_COUNT; i++) {
				temp_cards[i] = GRR._cards_data[seat_index][i];
			}

		}
		roomResponse.setCommResponse(PBUtil.toByteString(button_operate));

		this.send_response_to_player(seat_index, roomResponse);
		if (is_grr == true)
			GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean send_call_banker_to_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_ROBOT_BANKER);
		SelectdBankerJdb.Builder robot_banker = SelectdBankerJdb.newBuilder();
		robot_banker.setBankerSeat(seat_index);
		robot_banker.setBankerScore(this._player_call_banker[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(robot_banker));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean set_banker_to_room(int operate_times) {
		Player player = this.get_players()[_cur_banker];
		if (player == null) {
			log_error("庄家用户不对" + _cur_banker);
			return true;
		}
		if (operate_times == 1) {
			this._hou_shou_score = (long) _player_result.game_score[_cur_banker] - this._init_momey;
			_player_result.game_score[_cur_banker] = this._init_momey;
			player.setGame_score((int) _player_result.game_score[_cur_banker]);
		} else if (operate_times == 2) {
			this._hou_shou_score -= this._init_momey * operate_times;
			_player_result.game_score[_cur_banker] += this._init_momey * operate_times;
		} else if (operate_times == 4) {
			this._next_banker = this._cur_banker;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_JDB_ROBOT_BANKER_RESULT);
			SelectdBankerResultJdb.Builder robot_banker = SelectdBankerResultJdb.newBuilder();
			robot_banker.setBankerSeat(-1);
			robot_banker.setHouShouScore(this._hou_shou_score);
			this.load_player_info_data_call_banker(robot_banker);
			roomResponse.setCommResponse(PBUtil.toByteString(robot_banker));
			if (GRR != null)
				GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
			// if(this._player_ready[_cur_banker] == 0)
			// {
			// this.go_to(_cur_banker);
			// }
			return true;
		} else if (operate_times == 5) {
			this._next_banker = this._cur_banker;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_JDB_ROBOT_BANKER_RESULT);
			SelectdBankerResultJdb.Builder robot_banker = SelectdBankerResultJdb.newBuilder();
			robot_banker.setBankerSeat(this._cur_banker);
			robot_banker.setHouShouScore(this._hou_shou_score);
			this.load_player_info_data_call_banker(robot_banker);
			roomResponse.setCommResponse(PBUtil.toByteString(robot_banker));
			if (GRR != null)
				GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		}
		this._next_banker = this._cur_banker;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_ROBOT_BANKER_RESULT);
		SelectdBankerResultJdb.Builder robot_banker = SelectdBankerResultJdb.newBuilder();
		robot_banker.setBankerSeat(_cur_banker);
		robot_banker.setHouShouScore(this._hou_shou_score);
		this.load_player_info_data_call_banker(robot_banker);
		roomResponse.setCommResponse(PBUtil.toByteString(robot_banker));
		if (GRR != null)
			GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		if (this._player_ready[_cur_banker] == 0) {
			this.go_to(_cur_banker);
		}
		return true;
	}

	public void calculate_player_jetton_score() {
		int count = 0;
		this._max_jetton_score = (long) this._player_result.game_score[this._cur_banker];
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (this._player_status[i] == false) {
				continue;
			}
			if (this._cur_banker == i)
				continue;
			count++;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (this._player_status[i] == false) {
				continue;
			}
			if (this._cur_banker == i)
				continue;
			int score = (int) _player_result.game_score[_cur_banker] / count / 5;
			this._max_player_jetton[i] = (long) score * 5;
			if (this._max_player_jetton[i] > (long) _player_result.game_score[i]) {
				this._max_player_jetton[i] = (long) _player_result.game_score[i];
			}

		}
	}

	public boolean call_banker(int seat_index, int sub_index) {
		if (sub_index < 0 || sub_index > this._banker_times) {
			log_error("下注下标不对" + this._jetton_count);
			return true;
		}
		if (this._player_call_banker[seat_index] != -1) {
			log_error("您已经叫庄了" + seat_index);
			return true;
		}
		this._player_call_banker[seat_index] = this._call_banker_info[sub_index];
		send_call_banker_to_room(seat_index);
		boolean flag = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (this._player_status[i] == false)
				continue;
			if (this._player_call_banker[i] == -1) {
				flag = true;
				break;
			}
		}
		if (flag == false) {

			for (int j = _banker_times; j >= 0; j--) {
				int chairID[] = new int[this.getTablePlayerNumber()];
				int chair_count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					if ((this._player_status[i] == true) && (_call_banker_info[j] == this._player_call_banker[i])) {
						if (i % 2 == 0)
							chairID[chair_count++] = i;
					}

				}
				if (chair_count > 0) {
					int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
					int temp = rand % chair_count;
					_cur_banker = chairID[temp];
					this._last_banker = _cur_banker;
					_banker_times = _player_call_banker[_cur_banker];
					if (_banker_times == 0)
						_banker_times = 1;
					break;
				}
			}
			set_banker_to_room(1);
			calculate_player_jetton_score();
			result_qie_card();
			// this._game_status = GameConstants.GS_OX_ADD_JETTON;
			// int user_count = 0;
			// for(int i = 0; i<this.getTablePlayerNumber();i++)
			// {
			// if(this._player_status[i] ==true)
			// user_count ++;
			// }
			// GameSchedule.put(new
			// AnimationRunnable(getRoom_id(),ID_TIMER_ANIMATION_ROBOT),
			// 100*user_count+2000, TimeUnit.MILLISECONDS);
			//
		}
		return true;
	}

	public boolean result_jetton(int seat_index, long jetton_score, int area, int opt_type) {

		if (seat_index == this._cur_banker) {
			log_error("下注用户不对" + seat_index);
			return true;
		}
		// if(area == this._cur_banker)
		// {
		// log_error("下注区域不对"+area) ;
		// return true;
		// }
		if (jetton_score % 5 != 0) {
			this.send_error_notify(seat_index, 2, "请下5的倍数");
			return true;
		}
		if (this._player_result.game_score[seat_index] - this.caculate_max_jetton(seat_index) - jetton_score < 0) {
			log_error("已经超过您下注的最大分了，不能下注！");
		}
		if (opt_type != GameConstants.CMD_PASS_JETTON) {
			if (jetton_score > this._max_player_jetton[seat_index]) {
				jetton_score = this._max_player_jetton[seat_index];
				if (jetton_score == 0) {
					this.send_error_notify(seat_index, 1, "时间已到，您当前下注没有成功");
				}
				String str = "";
				str += "当前最高可下注" + jetton_score + "分，您的超出下注分数将返还";

			}
			if (this._max_jetton_score == 0) {
				log_error("没有可以下注的分数了" + area);
				return true;
			}
			_jetton_player[seat_index][area] += jetton_score;
			_jetton_area[area] += jetton_score;
			this._max_player_jetton[seat_index] -= jetton_score;
			this._max_jetton_score -= jetton_score;
			_jetton_player_all[seat_index] += jetton_score;
			send_jetton_to_room(seat_index, area, jetton_score);
		} else {
			this._is_pass_jetton[seat_index] = 2;
		}

		if (this.is_can_jetton() == false) {
			this.kill_timer();
			this.open_card_operate();
			return true;
		}
		if (GameConstants.GS_JDB_ADD_JETTON_ONE == this._game_status) {
			// if(this._max_player_jetton[seat_index] == 0)
			// return true;
			if (this._is_pass_jetton[seat_index] == 0 && this._max_player_jetton[seat_index] != 0) {
				this._is_pass_jetton[seat_index] = 1;
			}

			boolean flag = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				if (this._cur_banker == i)
					continue;
				if (this._is_pass_jetton[i] == 2)
					continue;
				if (this._max_player_jetton[i] != 0)
					flag = true;
			}
			if (flag == true) {
				this.update_button(1, seat_index, this._idi_jetton_time_one, true);
				return true;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
				if (this._player_status[i] == false)
					continue;
				if (this._cur_banker == i)
					continue;
				if (this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
					continue;
				if (this._is_pass_jetton[i] == 2)
					continue;
				if (this._max_player_jetton[i] == 0) {
					this._is_pass_jetton[i] = 1;
					if (this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score)
						this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					else
						this._max_player_jetton[i] = this._max_jetton_score;
					flag = true;
				} else {
					this._max_player_jetton[i] = 0;
				}
			}
			if (flag == false) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					if (this._cur_banker == i)
						continue;
					if (i % 2 == 0) {
						this._is_pass_jetton[i] = 2;
						this._max_player_jetton[i] = 0;
						continue;
					}
					if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
						continue;
					if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score) {
						this._is_pass_jetton[i] = 1;
						this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					} else if (i % 2 == 1) {
						this._is_pass_jetton[i] = 1;
						this._max_player_jetton[i] = this._max_jetton_score;
					} else
						this._max_player_jetton[i] = 0;
					flag = true;

				}
				if (flag == false) {
					this.open_card_operate();
					return true;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					update_button(3, i, this._idi_jetton_time_three, true);
				}
				this._game_status = GameConstants.GS_JDB_ADD_JETTON_THREE;
				set_timer(ID_TIMER_JETTON_THREE, this._idi_jetton_time_three);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				update_button(2, i, this._idi_jetton_time_two, true);
			}
			this._game_status = GameConstants.GS_JDB_ADD_JETTON_TWO;
			set_timer(ID_TIMER_JETTON_TWO, this._idi_jetton_time_two);
		} else if (GameConstants.GS_JDB_ADD_JETTON_TWO == this._game_status) {
			boolean flag = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
				if (this._player_status[i] == false)
					continue;
				if (i == this._cur_banker)
					continue;
				if (this._is_pass_jetton[i] != 1)
					continue;
				if (this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
					continue;
				if (this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score) {
					this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					flag = true;
				} else
					this._max_player_jetton[i] = this._max_jetton_score;
				flag = true;
				this.update_button(2, i, this._idi_jetton_time_two, true);
			}
			if (flag == false) {

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					if (this._cur_banker == i)
						continue;
					if (i % 2 == 0) {
						this._is_pass_jetton[i] = 0;
						this._max_player_jetton[i] = 0;
						continue;
					}
					if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
						continue;
					if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score) {
						this._is_pass_jetton[i] = 1;
						this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					} else if (i % 2 == 1) {
						this._is_pass_jetton[i] = 1;
						this._max_player_jetton[i] = this._max_jetton_score;
					} else
						this._max_player_jetton[i] = 0;
					flag = true;

				}
				if (flag == false) {
					this.open_card_operate();
					return true;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					update_button(3, i, this._idi_jetton_time_three, true);
				}
				this._game_status = GameConstants.GS_JDB_ADD_JETTON_THREE;
				set_timer(ID_TIMER_JETTON_THREE, this._idi_jetton_time_three);

				return true;
			}
		} else if (GameConstants.GS_JDB_ADD_JETTON_THREE == this._game_status) {
			boolean flag = false;
			for (int i = 1; i < this.getTablePlayerNumber(); i += 2) {
				if (this._player_status[i] == false)
					continue;
				if (this._is_pass_jetton[i] == 2)
					continue;
				if (this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score) {
					this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					flag = true;
				} else {
					this._max_player_jetton[i] = this._max_jetton_score;
					flag = true;
				}
				this.update_button(3, i, _idi_jetton_score_time, true);
			}
			if (flag == false) {
				this.open_card_operate();
				return true;
			}
		}

		// boolean flag = false;
		// for(int i = 0; i<this.getTablePlayerNumber();i++)
		// {
		// if(this._player_status[i] == false)
		// continue;
		// if(i == _cur_banker)
		// continue;
		// }
		// if(flag == false)
		// {
		//
		// }

		return true;
	}

	public boolean open_card_operate() {
		send_card(2, 1);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			update_button(4, i, this._idi_open_card_time, true);
		}
		this._game_status = GameConstants.GS_JDB_OPEN_CARD;
		this.set_timer(ID_TIMER_LIANG_PAI, this._idi_open_card_time);
		return true;
	}

	// 切牌
	public boolean result_qie_card() {

		_qie_card = this._repertory_card[_all_card_len - _left_card_count];
		_left_card_count++;
		_first_seat = (_cur_banker + _logic.get_real_card_value(_qie_card) - 1) % 4;
		if (_first_seat == _cur_banker) {
			_first_seat = (_cur_banker + 1) % 4;
			_first_seat = 2 * _first_seat;
		} else {
			_first_seat = 2 * _first_seat;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_QIE_CARD);
		FirstSeat.Builder first_seat = FirstSeat.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			first_seat.setCard(_qie_card);
			first_seat.setFirstSeatIdex(_first_seat);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(first_seat));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		this._current_player = _first_seat;
		_game_status = GameConstants.GS_JDB_QIE_CARD;// 设置状态
		GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_QIE_DALAY), 2000, TimeUnit.MILLISECONDS);
		return true;
	}

	public void send_jetton_to_room(int seat_index, int area, long jetton_score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_JETTON);
		JettonResultJdb.Builder jetton_result = JettonResultJdb.newBuilder();
		jetton_result.setJettonSeat(seat_index);
		jetton_result.setJettonScore(jetton_score);
		jetton_result.setJettonArea(area);
		jetton_result.setPlayerMaxJetton(this._max_player_jetton[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(jetton_result));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
	}

	public boolean send_card(int card_count, int opr_type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_SEND_CARD);
		SendCardJdb.Builder send_card = SendCardJdb.newBuilder();
		send_card.setDisplayTime(this._idi_open_card_time);
		send_card.setOpreateType(opr_type);
		for (int k = 0; k < this.getTablePlayerNumber(); k += 2) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (card_count < GameConstants.JDB_CARD_COUNT) {
				cards.addItem(GRR._cards_data[k][0]);
				cards.addItem(GameConstants.BLACK_CARD);

			} else {
				for (int j = 0; j < card_count; j++) {
					cards.addItem(GRR._cards_data[k][j]);

				}
			}
			send_card.addSendCard(cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));
		// for(int i = 0 ; i < this.getTablePlayerNumber();i++)
		// {
		// if(this._player_status[i] == false)
		// continue;
		// // 回放数据
		//
		// this.send_response_to_player(i,roomResponse);
		// }
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}

	public boolean open_card(int seat_index, int is_open_card) {
		//
		// if (has_rule(GameConstants.GAME_RULE_FKN_JB))
		// this._player_times[seat_index] =
		// _logic.get_times_two(GRR._cards_data[seat_index],
		// GameConstants.JDB_CARD_COUNT, _game_rule_index);
		// if (has_rule(GameConstants.GAME_RULE_JD_FKN))
		// this._player_times[seat_index] =
		// _logic.get_times_one(GRR._cards_data[seat_index],
		// GameConstants.JDB_CARD_COUNT, _game_rule_index);

		this._ox_value[seat_index] = _logic.get_card_type(GRR._cards_data[seat_index], GameConstants.JDB_CARD_COUNT, _game_rule_index);
		this._str_ox_value[seat_index] = _logic.get_card_ox_value(this._ox_value[seat_index]);

		if (this._open_card_player[seat_index] != -1) {
			log_error("您已经开牌了" + seat_index);
			return true;
		}
		this._open_card_player[seat_index] = is_open_card;
		send_open_to_room(seat_index, is_open_card);
		boolean flag = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (this._player_status[i] == false)
				continue;

			if (_open_card_player[i] == -1) {
				flag = true;
				return true;
			}

		}
		if (flag == false) {
			this.kill_timer();
			int loop = 0;
			int i = this._first_seat;
			while (loop < 4) {
				loop++;

				int time = 1;

				_liang_schedule[i] = GameSchedule.put(new LiangCardRunnable(getRoom_id(), i), time * (loop + 1), TimeUnit.SECONDS);
				i = (i + 2) % this.getTablePlayerNumber();
			}
		}

		return true;
	}

	public void process_ox_calulate_end() {

		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (i == _cur_banker)
				continue;
			int action_value = _logic.compare_card(GRR._cards_data[_cur_banker], GRR._cards_data[i], GameConstants.JDB_CARD_COUNT, _game_rule_index);
			// 获取点数
			long calculate_score = 0;
			long lChiHuScore = 0;
			// 获取点数
			this._area_status[i] = action_value;
			if (action_value == GameConstants.JDB_CALCULATE_WIN) {

				calculate_score = 1;
				lChiHuScore = calculate_score * this._jetton_area[i];
				this._area_status[i] = GameConstants.JDB_CALCULATE_LOST;

			} else if (action_value == GameConstants.JDB_CALCULATE_LOST) {

				calculate_score = -1;

				lChiHuScore = calculate_score * _jetton_area[i];
				this._area_status[i] = GameConstants.JDB_CALCULATE_WIN;
			}
			// 胡牌分
			for (int j = 0; j < this.getTablePlayerNumber(); j++)
				GRR._game_score[j] -= calculate_score * this._jetton_player[j][i];
			GRR._game_score[_cur_banker] += lChiHuScore;

		}
		all_game_end_record();

	}

	public void all_game_end_record() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			_player_result.game_score[i] += GRR._game_score[i];
			Player player = this.get_players()[i];
			if (player != null) {
				player.setGame_score((long) _player_result.game_score[i]);
				this.add_number_to_manager_list(player);
			}
		}
	}

	public boolean liang_pai(int seat_index) {
		if (this._liang_card_player[seat_index] != false) {
			log_error("您已经亮牌了" + seat_index);
			return true;
		}
		if (_liang_schedule[seat_index] != null) {
			_liang_schedule[seat_index].cancel(false);
			_liang_schedule[seat_index] = null;
		}
		this._liang_card_player[seat_index] = true;
		liang_pai_result(seat_index);
		return true;
	}

	public boolean liang_pai_result(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_LIANG_PAI);
		LiangCardJdb.Builder liang_card = LiangCardJdb.newBuilder();
		liang_card.setSeatIndex(seat_index);
		this._ox_value[seat_index] = _logic.get_card_type(GRR._cards_data[seat_index], GameConstants.JDB_CARD_COUNT, _game_rule_index);
		for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
			liang_card.addCards(GRR._cards_data[seat_index][j]);
		}
		liang_card.setJdbValue(this._ox_value[seat_index]);
		liang_card.setTimes(this._player_times[seat_index]);

		roomResponse.setCommResponse(PBUtil.toByteString(liang_card));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		if (seat_index == (this._first_seat + this.getTablePlayerNumber() - 2) % this.getTablePlayerNumber()) {
			process_ox_calulate_end();
			int delay = 2;

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), _cur_banker, GameConstants.Game_End_NORMAL), delay * 500, TimeUnit.MILLISECONDS);

		}

		return true;

	}

	public boolean send_open_to_room(int seat_index, int is_open_card) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_OPEN_CARD);
		OpenCardJdb.Builder open_card = OpenCardJdb.newBuilder();
		open_card.setOpenCard(_open_card_player[seat_index]);
		open_card.setSeatIndex(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));
		this.send_response_to_other(seat_index, roomResponse);
		if (is_open_card == 1)
			for (int i = 0; i < GameConstants.JDB_CARD_COUNT; i++) {
				open_card.addCards(this.GRR._cards_data[seat_index][i]);
			}
		else {

			open_card.addCards(this.GRR._cards_data[seat_index][0]);
			open_card.addCards(GameConstants.BLACK_CARD);

		}
		open_card.setJdbValue(this._ox_value[seat_index]);
		open_card.setTimes(this._player_times[seat_index]);
		roomResponse.clearCommResponse();
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public int get_hand_card_count_max() {
		return GameConstants.ZJH_MAX_COUNT;
	}

	public void kill_timer() {
		if (_game_scheduled != null) {
			_game_scheduled.cancel(false);
			_game_scheduled = null;
		}

	}

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000, TimeUnit.MILLISECONDS);
		_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
		this._cur_operate_time = time;

		return true;

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

		if (_all_card_len - _left_card_count < 9) {
			int xi_pai_count = 0;
			int rand = (int) RandomUtil.generateRandomNumber(3, 6);

			while (xi_pai_count < 6 && xi_pai_count < rand) {
				if (xi_pai_count == 0)
					_logic.random_card_data(repertory_card, card_cards);
				else
					_logic.random_card_data(repertory_card, repertory_card);

				xi_pai_count++;
			}

			_left_card_count = 0;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[_left_card_count++];
			}
			// _left_card_count +=GameConstants.JDB_CARD_COUNT;
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {

		// /********
		// * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		// **************************/
		// int[] realyCards = new int[] {
		// 0x11,0x02,0x05,0x01,0x03,0x11,0x15,0x23,0x25
		// };
		// testRealyCard(realyCards);

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > GameConstants.JDB_CARD_COUNT) {
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

	public void send_room_player_info(Player player, boolean is_observer) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_ROOM_PLAY_INFO);
		RoomPlayInfo.Builder room_player_info = RoomPlayInfo.newBuilder();
		RoomInfoJdb.Builder room_info = getRoomInfoJdb();
		room_player_info.setRoomInfo(room_info);
		this.load_player_info_data_room_player_info(player.getAccount_id(), room_player_info);
		roomResponse.setCommResponse(PBUtil.toByteString(room_player_info));

		if (is_observer) {
			observers().send(player, roomResponse);
			return;
		}
		if (GRR != null)
			GRR.add_room_response(roomResponse);
		this.send_response_to_player(player.get_seat_index(), roomResponse);
	}

	public void load_player_info_data_room_player_info(long account_id, RoomPlayInfo.Builder roomResponse) {
		Player rplayer;

		rplayer = room_players().getPlayer(account_id);
		if (rplayer == null)
			return;
		RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
		room_player.setAccountId(rplayer.getAccount_id());
		room_player.setHeadImgUrl(rplayer.getAccount_icon());
		room_player.setIp(rplayer.getAccount_ip());
		room_player.setUserName(rplayer.getNick_name());
		room_player.setSeatIndex(rplayer.get_seat_index());
		room_player.setOnline(rplayer.isOnline() ? 1 : 0);
		room_player.setIpAddr(rplayer.getAccount_ip_addr());
		room_player.setSex(rplayer.getSex());
		room_player.setScore(rplayer.getGame_score());
		room_player.setReady(0);
		room_player.setMoney(rplayer.getMoney());
		room_player.setGold(rplayer.getGold());
		room_player.setCarryMoney(rplayer.getCarry_score());
		room_player.setApplySeatIndex(rplayer.get_apply_index());
		room_player.setBuMoney(rplayer.getBu_score());
		if (rplayer.locationInfor != null) {
			room_player.setLocationInfor(rplayer.locationInfor);
		}
		roomResponse.addPlayers(room_player);

	}

	public RoomPlayerResponseJdb.Builder load_player_info_data(int seat_index) {
		Player rplayer = this.get_players()[seat_index];
		if (rplayer == null)
			return null;
		RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
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
		room_player.setCarryMoney(rplayer.getCarry_score());
		room_player.setApplySeatIndex(rplayer.get_apply_index());
		room_player.setBuMoney(rplayer.getBu_score());
		if (rplayer.locationInfor != null) {
			room_player.setLocationInfor(rplayer.locationInfor);
		}
		return room_player;
	}

	public void load_player_info_data_call_banker(SelectdBankerResultJdb.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
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
			room_player.setCarryMoney(rplayer.getCarry_score());
			room_player.setApplySeatIndex(rplayer.get_apply_index());
			room_player.setBuMoney(rplayer.getBu_score());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_start(GameStartJdb.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
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
			room_player.setCarryMoney(rplayer.getCarry_score());
			room_player.setApplySeatIndex(rplayer.get_apply_index());
			room_player.setBuMoney(rplayer.getBu_score());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_player_list(PlayerListJdb.Builder roomResponse, int operate_type, boolean is_roomown) {
		int i = -1;
		if (operate_type == 1)
			for (Player rplayer : apply_seat_players().room_infoCollection()) {
				i++;
				if (rplayer == null)
					continue;
				RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
				room_player.setAccountId(rplayer.getAccount_id());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
				room_player.setIp(rplayer.getAccount_ip());
				room_player.setUserName(rplayer.getNick_name());
				room_player.setSeatIndex(rplayer.get_seat_index());
				room_player.setOnline(rplayer.isOnline() ? 1 : 0);
				room_player.setIpAddr(rplayer.getAccount_ip_addr());
				room_player.setSex(rplayer.getSex());
				room_player.setScore(rplayer.getGame_score());
				room_player.setReady(0);
				room_player.setMoney(rplayer.getMoney());
				room_player.setGold(rplayer.getGold());
				room_player.setCarryMoney(rplayer.getCarry_score());
				room_player.setApplySeatIndex(rplayer.get_apply_index());
				room_player.setBuMoney(rplayer.getBu_score());
				if (rplayer.locationInfor != null) {
					room_player.setLocationInfor(rplayer.locationInfor);
				}
				roomResponse.addPlayers(room_player);
			}
		else if (operate_type == 2)
			for (Player rplayer : apply_score_players().room_infoCollection()) {
				i++;
				if (rplayer == null)
					continue;
				RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
				room_player.setAccountId(rplayer.getAccount_id());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
				room_player.setIp(rplayer.getAccount_ip());
				room_player.setUserName(rplayer.getNick_name());
				room_player.setSeatIndex(rplayer.get_seat_index());
				room_player.setOnline(rplayer.isOnline() ? 1 : 0);
				room_player.setIpAddr(rplayer.getAccount_ip_addr());
				room_player.setSex(rplayer.getSex());
				room_player.setScore(rplayer.getGame_score());
				room_player.setReady(0);
				room_player.setMoney(rplayer.getMoney());
				room_player.setGold(rplayer.getGold());
				room_player.setCarryMoney(rplayer.getCarry_score());
				room_player.setApplySeatIndex(rplayer.get_apply_index());
				room_player.setBuMoney(rplayer.getBu_score());
				if (rplayer.locationInfor != null) {
					room_player.setLocationInfor(rplayer.locationInfor);
				}
				roomResponse.addPlayers(room_player);
			}
		else if (operate_type == 3) {
			for (Player rplayer : room_players().room_infoCollection()) {
				i++;
				if (rplayer == null)
					continue;
				if (rplayer.getAccount_id() == this.getRoom_owner_account_id() && is_roomown == false)
					continue;
				if (rplayer.get_seat_index() < 0 && is_roomown == false)
					continue;
				RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
				room_player.setAccountId(rplayer.getAccount_id());
				room_player.setHeadImgUrl(rplayer.getAccount_icon());
				room_player.setIp(rplayer.getAccount_ip());
				room_player.setUserName(rplayer.getNick_name());
				room_player.setSeatIndex(rplayer.get_seat_index());
				room_player.setOnline(rplayer.isOnline() ? 1 : 0);
				room_player.setIpAddr(rplayer.getAccount_ip_addr());
				room_player.setSex(rplayer.getSex());
				room_player.setScore(rplayer.getGame_score());
				room_player.setReady(0);
				room_player.setMoney(rplayer.getMoney());
				room_player.setGold(rplayer.getGold());
				room_player.setCarryMoney(rplayer.getCarry_score());
				room_player.setApplySeatIndex(rplayer.get_apply_index());
				room_player.setBuMoney(rplayer.getBu_score());
				if (rplayer.locationInfor != null) {
					room_player.setLocationInfor(rplayer.locationInfor);
				}
				roomResponse.addPlayers(room_player);
			}
		}
	}

	public long load_player_info_data_result_list(PlayerResultJdb.Builder roomResponse) {
		int i = -1;
		long max_score = 0;
		for (Player rplayer : room_players().room_infoCollection()) {
			i++;
			if (rplayer == null)
				continue;
			if (rplayer.getGame_score() - rplayer.getCarry_score() > max_score)
				max_score = rplayer.getGame_score() - rplayer.getCarry_score();
			RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(rplayer.getGame_score());
			room_player.setReady(0);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setCarryMoney(rplayer.getCarry_score());
			room_player.setApplySeatIndex(rplayer.get_apply_index());
			room_player.setBuMoney(rplayer.getBu_score());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
		return max_score;
	}

	public void load_player_info_data_game_end(PukeGameEndJdb.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
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
			room_player.setApplySeatIndex(rplayer.get_apply_index());
			room_player.setCarryMoney(rplayer.getCarry_score());
			room_player.setBuMoney(rplayer.getBu_score());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_get_record(RecordList.Builder roomResponse, GameRecordInfo record_info) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = record_info.player[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(rplayer.getGame_score());
			room_player.setReady(1);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setApplySeatIndex(rplayer.get_apply_index());
			room_player.setCarryMoney(rplayer.getCarry_score());
			room_player.setBuMoney(rplayer.getBu_score());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponseJdb.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseJdb.Builder room_player = RoomPlayerResponseJdb.newBuilder();
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
			room_player.setApplySeatIndex(rplayer.get_apply_index());
			room_player.setCarryMoney(rplayer.getCarry_score());
			room_player.setBuMoney(rplayer.getBu_score());
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
		int count = realyCards.length / GameConstants.JDB_CARD_COUNT;

		count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		count = this.getTablePlayerNumber();
		int k = 0;
		this._left_card_count = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i += 2) {
				for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
					GRR._cards_data[i][j] = realyCards[this._left_card_count++];
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
			for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
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

		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		_game_status = GameConstants.GS_MJ_WAIT;
		ret = this.handler_game_finish_JDB(seat_index, reason);

		return ret;
	}

	public void clear_data() {
		Arrays.fill(this._player_call_banker, -1);
	}

	public boolean handler_game_finish_JDB(int seat_index, int reason) {
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
		// if(_cur_round < _game_round)
		// this.set_timer(GameConstants.HJK_READY_TIMER,
		// GameConstants.HJK_READY_TIME_SECONDS, true);
		// GameSchedule.put(new
		// AnimationRunnable(getRoom_id(),this.ID_TIMER_GO_TO),
		// this._idi_ready_card_time*1000, TimeUnit.MILLISECONDS);

		// set_timer(ID_TIMER_GO_TO, this._idi_ready_card_time);
		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		clear_data();
		// 最高得分

		// this._game_status = GameConstants.GS_OX_GAME_END;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndJdb.Builder game_end_Jdb = PukeGameEndJdb.newBuilder();
		RoomInfoJdb.Builder room_info = getRoomInfoJdb();
		game_end_Jdb.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {
		}
		this.load_player_info_data_game_end(game_end_Jdb);
		game_end_Jdb.setGameRound(_game_round);
		game_end_Jdb.setCurRound(_cur_round);
		if (_cur_banker != -1)
			game_end_Jdb.setAddBankerTimes(1 - this._add_banker_times[_cur_banker]);
		game_end_Jdb.setCurBanker(this._cur_banker);
		if (GRR != null) {
			this.operate_player_data();
			GameRecordInfo record_info = new GameRecordInfo(this.getTablePlayerNumber(), GameConstants.JDB_MAX_CARD_COUNT,
					GameConstants.JDB_MAX_AREA);
			record_info.cur_round = this._cur_round;
			record_info.banker_seat = this._cur_banker;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(GRR._game_score[i]);

				game_end_Jdb.addEndScore((int) GRR._game_score[i]);
				record_info.end_score[i] = (int) GRR._game_score[i];
				record_info.player[i] = this.get_players()[i];
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i % 2 == 0) {
					for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
						record_info.cards_data[i][j] = GRR._cards_data[i][j];
					}
				}

				game_end_Jdb.addCardsData(cards_card);
				Int32ArrayResponse.Builder jetton_player = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					jetton_player.addItem((int) this._jetton_player[i][j]);
					record_info.jetton_player[i][j] = (int) this._jetton_player[i][j];
				}
				game_end_Jdb.addJettonPlayer(jetton_player);
				game_end_Jdb.addAreaStatus(this._area_status[i]);
				record_info.area_status[i] = this._area_status[i];
				int operate_player = GameConstants.JDB_OPERATE_NULL;
				if (i == _cur_banker && this.is_mj_type(GameConstants.GAME_TYPE_DZH) == false) {
					operate_player |= GameConstants.JDB_GO_TO;
					operate_player |= GameConstants.JDB_ADD_BANKER;
					operate_player |= GameConstants.JDB_CACEL_BANKER;
				} else {
					operate_player |= GameConstants.JDB_STAND_UP;
					operate_player |= GameConstants.JDB_GO_TO;
				}
				game_end_Jdb.addOperateButton(operate_player);
			}
			this.record_info_list.add(record_info);
		}

		boolean end = false;
		long end_time = System.currentTimeMillis() / 1000;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (end_time - this._start_timer >= this._game_interval * 60) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				// game_end_Jdb.setPlayerResult(this.process_player_result_Jdb(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			// game_end_Jdb.setPlayerResult(this.process_player_result_Jdb(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;

		}
		// if(end== false)
		// {
		// set_timer(ID_TIMER_GO_TO, this._idi_ready_card_time);
		// }
		game_end_Jdb.setWinner(seat_index);
		game_end_Jdb.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}
		if (end == true) {

			Player banker_player = new Player();
			if (this._cur_banker != -1)
				banker_player = this.get_players()[this._cur_banker];
			if (banker_player != null && this._hou_shou_score != 0) {
				banker_player.setGame_score(banker_player.getGame_score() + this._hou_shou_score);
				this.add_number_to_manager_list(banker_player);
			}
			PlayerResultJdb.Builder player_result = PlayerResultJdb.newBuilder();
			long max_score = load_player_info_data_result_list(player_result);
			this.log_info("max_info = " + max_score);
			player_result.setDayingjia(max_score);
			player_result.setStartTime(this._start_timer);
			player_result.setEndTime(end_time);
			for (Player player : room_players().room_infoCollection()) {
				if (player == null)
					continue;
				player_result.setOwnScore(player.getGame_score());
				player_result.setTotalRecord(player.getGame_score() - player.getCarry_score());
				game_end_Jdb.setPlayerResult(player_result);
				game_end.setCommResponse(PBUtil.toByteString(game_end_Jdb));
				roomResponse.setGameEnd(game_end);
				room_players().send(player, roomResponse);
			}

		} else {
			game_end.setCommResponse(PBUtil.toByteString(game_end_Jdb));
			roomResponse.setGameEnd(game_end);
			this.send_response_to_room(roomResponse, true);
		}

		record_game_round(game_end, reason);

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
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = encodeRoomBase();

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	/**
	 * @return
	 */
	public RoomInfoJdb.Builder getRoomInfoJdb() {
		RoomInfoJdb.Builder room_info = RoomInfoJdb.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setCreateName(this.getRoom_owner_name());
		room_info.setMinCarryMomey(this._min_carry_momey);
		room_info.setMaxCarryMomey(this._max_carry_momey);
		room_info.setInitMomey(this._init_momey);
		room_info.setEnterGold(this._enter_gold);
		room_info.setPuFenGlod(this._bu_fen_glod);
		room_info.setAddBankerGlod(this._add_banker_glod);
		room_info.setMinBuMoney(this._min_bu_money);
		room_info.setMaxBuMoney(this._max_bu_money);
		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		long interval = 0;
		if (_cur_round < 1)
			interval = this._game_interval * 60;
		else
			interval = this._game_interval * 60 - (System.currentTimeMillis() / 1000 - this._start_timer);
		room_info.setLeftTime(interval);
		return room_info;
	}

	/**
	 * 创建房间
	 * 
	 * @return
	 */
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		this.setCreate_type(type);
		this.setCreate_player(player);
		roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "", RoomRedisModel.class);
		if (type == GameConstants.CREATE_ROOM_PROXY) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);
			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);

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
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(RoomComonUtil.getMaxNumber(this, getDescParams()));
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			return true;
		}

		if (club_id > 0) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);
			RedisService redisService = SpringService.getBean(RedisService.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
			roomRedisModel.getNames().add(player.getNick_name());
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);
		}
		if (type == GameConstants.CREATE_ROOM_NORMAL) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);
		}

		// c成功
		get_players()[0] = player;
		player.set_seat_index(0);
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			_owner_account_id = player.get_seat_index();
			_owner_seat = player.get_seat_index();
		}

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

	public boolean handler_enter_room(Player player) {
		if (super.handler_enter_room(player)) {
			if (player.getAccount_id() == getRoom_owner_account_id()) {
				_owner_account_id = player.get_seat_index();
			}
			return true;
		}
		return false;
	}

	public void send_message_creator(RoomResponse.Builder roomResponse) {
		if (observers().exist(this.getCreate_player().getAccount_id()) == true) {
			observers().send(this.getCreate_player(), roomResponse);
		} else {
			Player player = this.get_player(this.getRoom_owner_account_id());
			if (player == null)
				return;
			this.send_response_to_player(player.get_seat_index(), roomResponse);
		}

	}

	public void send_message_player(long account_id, int seat_index, RoomResponse.Builder roomResponse) {

		if (observers().exist(account_id) == true) {
			observers().send(observers().getPlayer(account_id), roomResponse);
		} else {

			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void update_apply_list() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		PlayerListJdb.Builder player_list = PlayerListJdb.newBuilder();
		this.load_player_info_data_player_list(player_list, 1, true);
		roomResponse.setCommResponse(PBUtil.toByteString(player_list));
		roomResponse.setType(MsgConstants.RESPONSE_JDB_APPLY_LIST);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		this.load_room_info_data(roomResponse);
		send_message_creator(roomResponse);
	}

	public void update_apply_bu_money_list() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		PlayerListJdb.Builder player_list = PlayerListJdb.newBuilder();
		this.load_player_info_data_player_list(player_list, 2, true);
		roomResponse.setCommResponse(PBUtil.toByteString(player_list));
		roomResponse.setType(MsgConstants.RESPONSE_JDB_APPLY_BU_SCORE_LIST);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		this.load_room_info_data(roomResponse);
		send_message_creator(roomResponse);
	}

	public boolean update_manager_list(boolean is_roomown) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		PlayerListJdb.Builder player_list = PlayerListJdb.newBuilder();
		this.load_player_info_data_player_list(player_list, 3, is_roomown);
		roomResponse.setCommResponse(PBUtil.toByteString(player_list));
		roomResponse.setType(MsgConstants.RESPONSE_JDB_ROOM_MANAGER);
		if (is_roomown == true)
			roomResponse.setType(MsgConstants.RESPONSE_JDB_USER_NUMBER_LIST);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		this.load_room_info_data(roomResponse);
		send_message_creator(roomResponse);
		return true;
	}

	public void add_number_to_manager_list(Player player) {
		if (room_players().exist(player.getAccount_id())) {
			room_players().exit(player.getAccount_id());
		}
		room_players().enter(player);
	}

	public boolean carry_money(long account_id, long carry_money) {
		Player player = observers().getPlayer(account_id);
		if (player == null) {
			return true;
		}
		if (carry_money < this._min_carry_momey || carry_money > this._max_carry_momey) {
			String str;
			str = "您携带的分数，不在[" + _min_carry_momey + "," + _max_carry_momey + "]!";
			this.send_error_notify(player, 1, str);
			return false;
		}
		if (player.getCarry_score() != -1) {
			this.send_error_notify(player, 1, "您不能再携带分数了,请使用补分操作");
			return false;
		}
		player.setCarry_score(carry_money);
		player.setGame_score(carry_money);
		add_number_to_manager_list(player);
		return true;
	}

	public boolean carry_bu_money_apply(long account_id, long money) {
		Player player = room_players().getPlayer(account_id);
		if (player == null) {
			player = this.get_player(account_id);
			if (player == null)
				return true;
		}
		if (money < this._min_bu_money || money > this._max_bu_money) {
			String str;
			str = "您携带的分数，不在[" + _min_carry_momey + "," + _max_carry_momey + "]!";
			this.send_error_notify(player, 1, str);
			return false;
		}

		if (apply_score_players().exist(account_id)) {
			this.send_error_notify(player, 1, "您的补分申请还在审核中，请勿重复申请！");
			return false;
		} else {
			if (this.kou_dou_aa(player, this._bu_fen_glod, 2)) {
				player.setBu_score(money);
				apply_score_players().enter(player);
			} else {
				String des = " ";
				des = "补分需要消耗" + this._bu_fen_glod + SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆您身上闲逸豆不足，是否充值？");
				this.send_error_notify(player, 5, des);
				return false;
			}
		}

		update_apply_bu_money_list();
		add_number_to_manager_list(player);
		return true;
	}

	public int max_player_game() {
		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Player player = get_players()[i];
			if (player == null)
				continue;
			count++;
		}
		return count;
	}

	public boolean apply_enter_room(long account_id, int seat_index) {
		Player player = observers().getPlayer(account_id);
		if (player == null) {
			return true;
		}
		if (max_player_game() >= this._start_game_player) {
			this.send_error_notify(player, 2, "人员已满，不能再申请!");
			return true;
		}
		if (seat_index < 0 || seat_index >= GameConstants.JDB_APPLY_COUNT) {
			this.send_error_notify(player, 2, "该位置申请位置不对，请选择正确位置申请入座!");
			return true;
		}
		if (apply_seat_players().apply_same_seat_count(seat_index) >= GameConstants.JDB_APPLY_COUNT) {
			this.send_error_notify(player, 2, "该位置申请人数过多，请选择其他位置申请入座!");
			return false;
		}
		if (apply_seat_players().exist(account_id)) {
			this.send_error_notify(player, 2, "您的入座申请已提交，请等待房主审核！");
			return false;
		}
		if (seat_index == 0 && this.is_mj_type(GameConstants.GAME_TYPE_DZH) && player.getCarry_score() < this._init_momey) {
			String des = " ";
			des = "您申请的是庄家位，您的分数低于" + _init_momey + "请先补分！";
			this.send_error_notify(player, 1, des);
			return false;
		}

		long interval_time = System.currentTimeMillis() / 1000 - player.getLeave_timer();
		if (interval_time < 20) {
			String des = "";
			des += "您的入座申请未通过，请" + (20 - interval_time) + "秒后再进行申请！";
			this.send_error_notify(player, 2, des);
			return false;

		}
		if (player.getJoin_game() == false && this.kou_dou_aa(player, this._enter_gold, 1) == false) {
			String des = " ";
			des = "入场需要消耗" + this._enter_gold + SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆您身上闲逸豆不足，是否充值？");
			this.send_error_notify(player, 5, des);
			return false;
		}
		player.set_apply_index(seat_index);
		apply_seat_players().enter(player);
		if (player.getAccount_id() == this.getRoom_owner_account_id()) {
			this.sure_seat(true, true, player.getAccount_id());
			return true;
		}

		update_apply_list();
		add_number_to_manager_list(player);
		this.send_error_notify(player, 2, "请耐心等待，房主在确认！");
		return true;
	}

	public boolean handler_ox_game_start(int room_id, long account_id) {
		if (is_game_start == 2)
			return true;
		is_game_start = 2;
		control_game_start();

		handler_game_start();
		boolean nt = true;
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return true;
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {

		int limitCount = 20;
		if (SystemConfig.gameDebug == 1) {
			limitCount = 15;
		}
		if (player == null)
			return false;
		// 限制围观者数量，未来加到配置表控制
		if (player.getAccount_id() != getRoom_owner_account_id() && observers().count() >= limitCount) {
			this.send_error_notify(player, 1, "当前游戏围观位置已满,下次赶早!");
			return false;
		}
		if (room_players().exist(player.getAccount_id())) {
			player = room_players().getPlayer(player.getAccount_id());
			player.set_seat_index(GameConstants.INVALID_SEAT);
		} else {
			player.setCarry_score(-1);
			player.set_seat_index(GameConstants.INVALID_SEAT);
		}
		player.set_apply_index(GameConstants.INVALID_SEAT);
		observers().enter(player);
		add_number_to_manager_list(player);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		observers().send(player, roomResponse);
		send_room_player_info(player, true);
		int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index);
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			if (this.is_game_start == 1) {
				control_game_start();
				return true;
			}
			int _cur_count = 0;
			int player_count = 0;
			boolean flag = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			if ((player_count >= GameConstants.JDB_SIT_MIN) && (player_count == _cur_count)) {
				if (this._cur_round == 0) {
					this.is_game_start = 1;
					control_game_start();
				}
			} else {
				control_game_start();
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
			is_game_start();
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
		is_game_start();
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	public void is_game_start() {
		int _player_count = 0;
		int _cur_count = 0;
		int mine_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
				if (i % 2 == 0)
					mine_count++;
			}
		}
		if (this.get_players()[0] == null && this.is_mj_type(GameConstants.GAME_TYPE_DZH)) {
			return;
		}
		if ((this._cur_round == 0 || this._restart_room_own == true) && (_player_count >= GameConstants.JDB_SIT_MIN) && (_player_count == _cur_count)
				&& mine_count >= 2) {
			this.is_game_start = 1;
			control_game_start();
		} else if ((this._cur_round == 0 || this._restart_room_own == true) && (_player_count < GameConstants.JDB_SIT_MIN || mine_count < 2)) {
			this.is_game_start = 0;
			control_game_start();
		} else if ((_player_count >= GameConstants.JDB_SIT_MIN) && (_player_count == _cur_count) && mine_count >= 2 && this.is_game_start == 2) {
			this.handler_game_start();
		}
	}

	public boolean handler_observer_be_in_room(Player player) {
		this.update_apply_list();
		this.update_apply_bu_money_list();
		send_room_player_info(player, true);
		if (player.getAccount_id() == this.getRoom_owner_account_id()) {
			this.update_apply_list();
			this.update_apply_bu_money_list();
			if (this.is_game_start == 1) {
				control_game_start();
			}
		}
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && player != null) {
			// this.send_play_data(seat_index);

			// if (this._handler != null)
			// this._handler.handler_observer_be_in_room(this, player);

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

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		Player player_owner = this.get_players()[seat_index];
		if (player_owner == null) {
			return false;
		}
		if (player_owner.getAccount_id() == this.getRoom_owner_account_id()) {
			this.update_apply_list();
			this.update_apply_bu_money_list();

			if (this.is_game_start == 1) {
				control_game_start();
			}
		}
		send_room_player_info(player_owner, false);
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_JDB_RECONNECT_DATA);

			TableResponseJdb.Builder tableResponse_Jdb = TableResponseJdb.newBuilder();
			load_player_info_data_reconnect(tableResponse_Jdb);
			RoomInfoJdb.Builder room_info = getRoomInfoJdb();
			tableResponse_Jdb.setRoomInfo(room_info);

			tableResponse_Jdb.setBankerPlayer(_cur_banker);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				tableResponse_Jdb.addOpenCard(this._open_card_player[i]);
				tableResponse_Jdb.addLiangCard(this._liang_card_player[i]);
				Int32ArrayResponse.Builder jetton_score = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					jetton_score.addItem((int) this._jetton_player[i][j]);
				}
				tableResponse_Jdb.addJettonScore(jetton_score);
				if (this._liang_card_player[i] == true) {
					tableResponse_Jdb.addJdbValue(this._ox_value[i]);
				} else {
					tableResponse_Jdb.addJdbValue(this._ox_value[i]);

				}
				tableResponse_Jdb.addAreaStatus(this._area_status[i]);
				tableResponse_Jdb.addOperateButton(this._operate_button[i]);
				tableResponse_Jdb.addPlayerStatus(this._player_status[i]);
				tableResponse_Jdb.addCallBanker(this._player_call_banker[i]);
			}
			tableResponse_Jdb.setSceneStatus(_game_status);
			tableResponse_Jdb.setQieCard(this._qie_card);
			tableResponse_Jdb.setHouShouScore(this._hou_shou_score);
			if (this._game_status > GameConstants.GS_JDB_QIE_CARD) {
				for (int k = 0; k < this.getTablePlayerNumber(); k += 2) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					int card_count = 1;
					if (this._liang_card_player[k] == true)
						card_count = 2;

					if (card_count < GameConstants.JDB_CARD_COUNT) {
						cards.addItem(GRR._cards_data[k][0]);
						cards.addItem(GameConstants.BLACK_CARD);

					} else {
						for (int j = 0; j < card_count; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					}
					tableResponse_Jdb.addCardsData(cards);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_Jdb));
			this.send_response_to_player(seat_index, roomResponse);
			this.update_apply_list();
			this.update_apply_bu_money_list();
			if (_game_status == GameConstants.GS_JDB_CALL_BANKER) {

				tableResponse_Jdb.setBankerPlayer(-1);
				if (this._player_call_banker[seat_index] == -1 && this._player_status[seat_index] == true) {
					update_button(0, seat_index, this._idi_call_banker_time, false);
				}

			}
			if (_game_status == GameConstants.GS_JDB_ADD_JETTON_ONE) {
				for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
					if (this._player_status[i] == false)
						continue;
					update_button(1, i, this._idi_jetton_score_time, true);
				}
			}
			if (_game_status == GameConstants.GS_JDB_ADD_JETTON_TWO) {
				for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
					if (this._player_status[i] == false)
						continue;
					update_button(2, i, this._idi_jetton_score_time, true);
				}
			}
			if (_game_status == GameConstants.GS_JDB_ADD_JETTON_TWO) {
				for (int i = 1; i < this.getTablePlayerNumber(); i += 2) {
					if (this._player_status[i] == false)
						continue;
					update_button(3, i, this._idi_jetton_score_time, true);
				}
			}
			if (_game_status == GameConstants.GS_OX_OPEN_CARD) {

				if (this._player_status[seat_index] == true && this._open_card_player[seat_index] == -1 && seat_index / 2 == 0) {
					update_button(4, seat_index, this._idi_open_card_time, false);
				}

			}
		}

		Player player = get_players()[seat_index];
		if (player != null && this._operate_type[seat_index] != 0)
			this.popup_message(player.getAccount_id(), player.get_seat_index(), -1, this._operate_type[seat_index], this._operate_button[seat_index],
					this._operate_str[seat_index]);
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
		if (this._cur_round > 0 && this._operate_type[seat_index] == 0)
			return go_to(seat_index);
		return true;

	}

	public boolean popup_message(long accound_id, int seat_index, int display_time, int operate_type, int operate_button, String des) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_POPUP_MESSAGE);
		PopupMessage.Builder popup_message = PopupMessage.newBuilder();
		popup_message.setDisplaytime(display_time);
		popup_message.setOperateButton(operate_button);
		popup_message.setOperateType(operate_type);
		popup_message.setDes(des);
		roomResponse.setCommResponse(PBUtil.toByteString(popup_message));
		this.send_message_player(accound_id, seat_index, roomResponse);
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
		if (type == MsgConstants.REQUST_JDB_OPERATE) {

			Opreate_Jdb_Request req = PBUtil.toObject(room_rq, Opreate_Jdb_Request.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req);
		}
		return true;
	}

	// 1:r抢庄 2：下注 3：开牌 4：忽略 和 确认申请用户 5：携带分数 6:申请坐下
	public boolean handler_requst_opreate(int seat_index, Opreate_Jdb_Request req) {
		if (req.getOpreateType() < 1 || req.getOpreateType() > GameConstants.CMD_MAX_INDEX) {
			log_error("命令不在范围内" + seat_index);
		}
		switch (req.getOpreateType()) {
		case GameConstants.CMD_CALL_BANKER: {
			return call_banker(seat_index, req.getSubBtn());
		}
		case GameConstants.CMD_PASS_JETTON:
		case GameConstants.CMD_ADD_JETTON: {
			return result_jetton(seat_index, req.getJettonScore(), req.getAddJettonArea(), req.getOpreateType());
		}
		case GameConstants.CMD_OPEN_CARD: {
			if (this.get_players()[seat_index] == null) {
				return false;
			}
			if (this._open_card_player[seat_index] == -1) {
				if (this.kou_dou_aa(this.get_players()[seat_index], 20, 4)) {
					int is_open_card = 0;
					if (req.getIsCreatorOperate() == true)
						is_open_card = 1;
					return open_card(seat_index, is_open_card);
				}
			} else {
				return false;
			}
		}
		case GameConstants.CMD_CREATOR_OPERATE: {
			return sure_seat(req.getIsCreatorOperate(), true, req.getApplyAccountId());
		}
		case GameConstants.CMD_CARRY_MONEY: {
			return carry_money(req.getApplyAccountId(), req.getMomey());
		}
		case GameConstants.CMD_APPLY_SEAT: {
			return apply_enter_room(req.getApplyAccountId(), req.getSeatIndex());
		}
		case GameConstants.CMD_APPLY_BU_SCORE: {
			return carry_bu_money_apply(req.getApplyAccountId(), req.getMomey());
		}
		case GameConstants.CMD_CREAROR_BU_SCORE: {
			return sure_bu_money(req.getIsCreatorOperate(), req.getApplyAccountId());
		}
		case GameConstants.CMD_GO_TO: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";
			if (this._is_pause_game == true) {
				this.send_error_notify(seat_index, 0, "游戏已经暂停，请等待房主开始...........");
				return true;
			}

			return go_to(seat_index);
		}

		case GameConstants.CMD_STAND_UP: {

			return stand_up(seat_index, req.getApplyAccountId(), req.getIsCreatorOperate());
		}
		case GameConstants.CMD_ADD_BANKER: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";
			return add_banker(seat_index);
		}
		case GameConstants.CMD_END_ADD_SCORE: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";
			if (req.getOperateButton() == GameConstants.JDB_POP_CANCEL)
				return add_stand_up(seat_index);
			else {
				this.log_info("CMD_END_ADD_SCORE 操作命令错误" + req.getOperateButton());
				return true;
			}
		}
		case GameConstants.CMD_END_ADD_BANKER: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";

			if (req.getOperateButton() == GameConstants.JDB_POP_CANCEL) {
				cancel_banker(seat_index);
				return change_banker(seat_index);
			} else {
				this.log_info("CMD_END_ADD_BANKER 操作命令错误" + req.getOperateButton());
				return true;
			}
		}
		case GameConstants.CMD_END_NEXT_BANKER: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";
			this._add_banker_times[seat_index] = 0;
			this._left_card_count = GameConstants.JDB_MAX_CARD_COUNT;
			if (req.getOperateButton() == GameConstants.JDB_POP_SURE)
				return change_banker(seat_index);
			else {
				this.log_info("CMD_END_NEXT_BANKER 操作命令错误" + req.getOperateButton());
				return true;
			}
		}
		case GameConstants.CMD_END_MADE_BANKER: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";
			this._add_banker_times[seat_index] = 0;
			this._left_card_count = GameConstants.JDB_MAX_CARD_COUNT;
			if (req.getOperateButton() == GameConstants.JDB_POP_CANCEL) {
				return change_banker(seat_index);
			} else if (req.getOperateButton() == GameConstants.JDB_POP_BANKER) {
				this._next_banker = seat_index;

				go_to(seat_index);
				return true;
			} else {
				this.log_info("CMD_END_MADE_BANKER 操作命令错误" + req.getOperateButton());
				return true;
			}
		}
		case GameConstants.CMD_GET_NUMBER_LIST: {

			return this.update_manager_list(false);
		}
		case GameConstants.CMD_USER_GET_NUMBER_LIST: {
			return this.update_manager_list(true);
		}
		case GameConstants.CMD_GO_OUT_OPERATE: {
			return go_out_operate(seat_index, req.getApplyAccountId());
		}
		case GameConstants.CMD_CANCEL_BANKER: {
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";
			this._add_banker_times[seat_index] = 0;
			this._left_card_count = GameConstants.JDB_MAX_CARD_COUNT;
			if (seat_index != this._cur_banker) {
				return true;
			}
			if (this._is_pause_game) {
				this._is_pause_cancel_banker = true;
				return true;
			}
			cancel_banker(seat_index);
			change_banker(seat_index);
			this.go_to(seat_index);
			return true;

		}
		case GameConstants.CMD_RECORD_INFO: {

			return get_record_info(req.getApplyAccountId(), req.getApplyRound());
		}
		case GameConstants.CMD_ALL_AGREE_SEAT_OFF: {
			return sure_seat_off(req.getIsCreatorOperate(), req.getApplyAccountId());
		}
		case GameConstants.CMD_ALL_AGREE_ADD_SCORE: {
			return sure_add_score(req.getIsCreatorOperate(), true, req.getApplyAccountId());
		}
		case GameConstants.CMD_PAUSE_GAME: {
			if (req.getApplyAccountId() != this.getRoom_owner_account_id()) {
				return true;
			}
			this._is_pause_game = req.getIsCreatorOperate();
			return pause_game(req.getIsCreatorOperate());
		}
		}
		return true;
	}

	public void cancel_banker(int seat_index) {
		if (this._hou_shou_score > 0 && this._cur_banker == seat_index) {
			_player_result.game_score[_cur_banker] = _player_result.game_score[_cur_banker] + (float) this._hou_shou_score;
			this._hou_shou_score = 0;
			set_banker_to_room(4);
			// this._cur_banker = GameConstants.INVALID_SEAT;
		}
		return;
	}

	public boolean pause_game(boolean is_pause_game) {
		if (is_pause_game == true) {
			String des = "游戏将本局结束时暂停，请等待房主继续开始游戏...........";
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_JDB_PAUSE_GAME);
			PauseGameJdb.Builder pause_game = PauseGameJdb.newBuilder();
			pause_game.setOptType(is_pause_game);
			pause_game.setDes(des);
			for (Player player : room_players().room_infoCollection()) {
				if (player == null)
					continue;
				roomResponse.setCommResponse(PBUtil.toByteString(pause_game));
				room_players().send(player, roomResponse);
			}

		} else if (is_pause_game == false) {
			String des = "";
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_JDB_PAUSE_GAME);
			PauseGameJdb.Builder pause_game = PauseGameJdb.newBuilder();
			pause_game.setOptType(is_pause_game);
			pause_game.setDes(des);
			for (Player player : room_players().room_infoCollection()) {
				if (player == null)
					continue;
				roomResponse.setCommResponse(PBUtil.toByteString(pause_game));
				room_players().send(player, roomResponse);
			}
			if (this._is_pause_cancel_banker == true) {
				this._is_pause_cancel_banker = false;
				cancel_banker(this._cur_banker);
				change_banker(this._cur_banker);
				this.go_to(this._cur_banker);
			}
			this.ready_timer();
		}

		return true;
	}

	public boolean get_record_info(long account_id, int apply_round) {
		GameRecordInfo record_info = new GameRecordInfo(this.getTablePlayerNumber(), GameConstants.JDB_MAX_CARD_COUNT, GameConstants.JDB_MAX_AREA);
		Player player = this.room_players().getPlayer(account_id);
		if (player == null) {
			return false;
		}
		if (apply_round - 1 > this.record_info_list.size() || apply_round < 1) {
			this.send_error_notify(player, 1, "当前无战绩数据，请稍后查看");
			return false;
		}
		if (apply_round >= this.record_info_list.size())
			apply_round = this.record_info_list.size();
		if (apply_round == 0) {
			this.send_error_notify(player, 1, "当前无战绩数据，请稍后查看");
			return false;
		}
		record_info = this.record_info_list.get(apply_round - 1);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_RECORD_INFO);
		RecordList.Builder record_item = RecordList.newBuilder();
		load_player_info_data_get_record(record_item, record_info);
		record_item.setCurRound(record_info.cur_round);
		record_item.setBankerSeat(record_info.banker_seat);

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			record_item.addEndScore(record_info.end_score[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (i % 2 == 0) {
				for (int j = 0; j < GameConstants.JDB_CARD_COUNT; j++) {
					cards_card.addItem(record_info.cards_data[i][j]);
				}
			}

			record_item.addCardsData(cards_card);
			Int32ArrayResponse.Builder jetton_player = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				jetton_player.addItem(record_info.jetton_player[i][j]);
			}
			record_item.addJettonPlayer(jetton_player);
			record_item.addAreaStatus(record_info.area_status[i]);

		}
		roomResponse.setCommResponse(PBUtil.toByteString(record_item));
		room_players().send(player, roomResponse);
		return true;
	}

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			// PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			observers().send(player, quit_roomResponse);
			observers().exit(player.getAccount_id());
			return true;
		}
		return false;
	}

	public void return_data(Player player, int opt_type, boolean is_success, String str) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_JDB_RETRUN_DATA);
		ReturnDataJdb.Builder return_data = ReturnDataJdb.newBuilder();
		return_data.setOptType(opt_type);
		return_data.setIsSuccess(is_success);
		return_data.setDes(str);
		roomResponse.setCommResponse(PBUtil.toByteString(return_data));
		if (player != null)
			room_players().send(player, roomResponse);
	}

	public boolean go_out_operate(int seat_index, long account_id) {
		// if(account_id == this.getRoom_owner_account_id())
		// {
		// this.send_error_notify(this.getCreate_player(), 1, "房主，您不能踢除自己哦!");
		// return true;
		// }
		Player player = observers().getPlayer(account_id);
		if (player != null) {
			this.sure_seat(false, false, player.getAccount_id());
			this.sure_add_score(false, false, player.getAccount_id());

			handler_exit_room_observer(player);
			player.set_apply_index(GameConstants.LEAVE_SEAT);
			this.add_number_to_manager_list(player);
			return_data(this.getCreate_player(), GameConstants.CMD_GO_OUT_OPERATE, true, "");
			return true;
		}

		if ((GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status) && get_player(account_id) != null) {
			player = get_player(account_id);
			if (player == null) {
				return_data(this.getCreate_player(), GameConstants.CMD_GO_OUT_OPERATE, false, "");
				return true;
			}
			if (this.handler_release_room(player, GameConstants.Release_Room_Type_QUIT) == false) {
				return_data(this.getCreate_player(), GameConstants.CMD_GO_OUT_OPERATE, false, "");
				return true;
			}
			this.sure_add_score(false, false, player.getAccount_id());
			this._operate_button[seat_index] = 0;
			this._operate_type[seat_index] = 0;
			this._operate_str[seat_index] = "";

			if (this._last_banker != -1 && (seat_index == this._cur_banker)) {
				cancel_banker(seat_index);
				change_banker(seat_index);
			} else if (this._last_banker == -1 && seat_index == this._next_banker) {
				change_banker(seat_index);
			}
			if (player.get_seat_index() == this._last_banker && this._last_banker != -1) {
				this._player_result.game_score[this._last_banker] += this._hou_shou_score;
				player.setGame_score((long) this._player_result.game_score[this._last_banker]);
				this._restart_room_own = true;
			}

			player.set_apply_index(GameConstants.LEAVE_SEAT);
			this.add_number_to_manager_list(player);
			this._player_result.game_score[seat_index] = 0;
			return_data(this.getCreate_player(), GameConstants.CMD_GO_OUT_OPERATE, true, "");
			int player_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
				if (this.get_players()[i] == null)
					continue;
				player_count++;
			}
			if (player_count < 2) {
				this._restart_room_own = true;
			}
		}

		return true;
	}

	public boolean change_banker(int seat_index) {
		this._last_banker = -1;
		String des = "当前您可坐庄，请选择是否当庄";
		this._next_banker = seat_index;
		int loop = 0;
		this._player_call_banker[seat_index] = 0;
		this._add_banker_times[seat_index] = 0;
		while (loop < 4) {
			this._next_banker = (this._next_banker + 2) % this.getTablePlayerNumber();

			loop++;
			Player player = this.get_players()[this._next_banker];
			if (player == null) {
				continue;
			}
			if (this._next_banker == this._cur_banker) {
				int player_count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
					if (this.get_players()[i] == null)
						continue;
					player_count++;
				}
				if (player_count < 2) {
					this._restart_room_own = true;
					return true;
				}
				for (int j = _banker_times; j >= 0; j--) {
					int chairID[] = new int[this.getTablePlayerNumber()];
					int chair_count = 0;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this.get_players()[i] == null)
							continue;
						if ((i != this._cur_banker) && (_call_banker_info[j] == this._player_call_banker[i])) {
							if (i % 2 == 0)
								chairID[chair_count++] = i;
						}

					}
					if (chair_count > 0) {
						int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
						int temp = rand % chair_count;
						this._next_banker = chairID[temp];
						_banker_times = _player_call_banker[_cur_banker];
						if (_banker_times == 0)
							_banker_times = 1;
						break;
					}
				}
				if (this._player_ready[seat_index] == 0) {
					go_to(seat_index);
				}
				return true;
			}
			if (this._player_result.game_score[this._next_banker] > this._init_momey && this._player_call_banker[this._next_banker] != 0) {
				if (this._player_ready[this._next_banker] == 1)
					this.handler_player_ready(this._next_banker, true);
				if (this._player_ready[seat_index] == 0 && this._operate_button[seat_index] == 0) {
					go_to(seat_index);
				}

				this._operate_str[this._next_banker] = des;
				this._operate_button[this._next_banker] = 0;
				this._operate_type[this._next_banker] = GameConstants.CMD_END_ADD_BANKER;
				this._operate_button[this._next_banker] |= GameConstants.JDB_POP_BANKER;
				this._operate_button[this._next_banker] |= GameConstants.JDB_POP_CANCEL;
				this.popup_message(player.getAccount_id(), player.get_seat_index(), -1, this._operate_type[this._next_banker],
						this._operate_button[this._next_banker], this._operate_str[this._next_banker]);
				return true;
			}
		}

		return true;
	}

	public boolean add_banker(int seat_index) {
		if (_cur_banker == -1) {
			log_info("add_banker cur_banker = " + _cur_banker);
			return true;
		}
		if (this._add_banker_times[_cur_banker] > 0) {
			return true;
		}
		if (this._hou_shou_score < this._init_momey * 2) {
			String des = "您的台面分数不足最低下注分数,是否进行补分？";
			this._operate_str[seat_index] = des;
			this._operate_type[seat_index] = GameConstants.CMD_END_ADD_SCORE;
			this._operate_button[seat_index] |= GameConstants.JDB_POP_ADD_SCORE;
			this._operate_button[seat_index] |= GameConstants.JDB_POP_CANCEL;

			this.popup_message(this.get_players()[_cur_banker].getAccount_id(), this.get_players()[_cur_banker].get_seat_index(), -1,
					this._operate_type[seat_index], this._operate_button[seat_index], this._operate_str[seat_index]);
			return true;
		}
		if (seat_index != _cur_banker) {
			return true;
		}
		Player player = this.get_players()[seat_index];
		if (player == null) {
			return true;
		}
		if (player.getJoin_game() == false && this.kou_dou_aa(player, this._add_banker_glod, 3) == false) {
			String des = " ";
			des = "添庄需要消耗" + this._add_banker_glod + SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆您身上闲逸豆不足，是否充值？");
			this.send_error_notify(player, 5, des);
			return false;
		}
		this._add_banker_times[_cur_banker]++;
		set_banker_to_room(2);
		return true;
	}

	public boolean add_stand_up(int seat_index) {
		Player player = this.get_players()[seat_index];
		if (player == null) {
			return true;
		}

		if (this._player_status[seat_index] == true) {
			if (!(GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status)) {
				// 游戏已经开始
				send_error_notify(seat_index, 2, "您正在游戏 ,不能退出，请游戏结束时离开!");
				return false;
			}
		}

		if (this._last_banker != -1 && seat_index == this._cur_banker) {
			cancel_banker(seat_index);
			change_banker(seat_index);
		} else if (this._last_banker == -1 && seat_index == this._next_banker) {
			change_banker(seat_index);
		}
		this._player_status[seat_index] = false;
		this.handler_player_ready(seat_index, true);
		player.setGame_score((long) this._player_result.game_score[seat_index]);
		player.set_seat_index(GameConstants.INVALID_SEAT);
		this.add_number_to_manager_list(player);
		if (this.observers().exist(player.getAccount_id())) {
			this.observers().exit(player.getAccount_id());
			this.observers().enter(player);
		} else {
			this.observers().enter(player);
		}
		this._player_result.game_score[seat_index] = 0;
		this.get_players()[seat_index] = null;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		room_players().send(player, roomResponse);
		send_response_to_other(seat_index, roomResponse);
		int player_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (this.get_players()[i] == null)
				continue;
			player_count++;
		}
		if (player_count < 2) {
			this._restart_room_own = true;
		}

		return true;
	}

	public boolean stand_up(int seat_index, long account_id, boolean is_kick) {
		if (seat_index != -1) {
			if (is_kick == true) {
				if (!(GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status)) {
					// 游戏已经开始
					send_error_notify(seat_index, 2, "您暂时不能踢人 ，请当局游戏结束后再操作!");
					return false;
				}
			} else {
				this._operate_button[seat_index] = 0;
				this._operate_type[seat_index] = 0;
				this._operate_str[seat_index] = "";
				return add_stand_up(seat_index);
			}
		}
		if (seat_index != -1) {
			if (this._last_banker != -1 && seat_index == this._cur_banker) {
				cancel_banker(seat_index);
				change_banker(seat_index);
			} else if (this._last_banker == -1 && seat_index == this._next_banker) {
				change_banker(seat_index);
			}
		}

		Player player = this.get_player(account_id);
		if (player == null) {
			return true;
		}
		seat_index = player.get_seat_index();
		player = this.get_players()[seat_index];
		if (player == null) {
			return true;
		}
		this._player_status[seat_index] = false;
		this.handler_player_ready(seat_index, true);
		player.setGame_score((long) this._player_result.game_score[seat_index]);
		player.set_seat_index(GameConstants.INVALID_SEAT);
		this.add_number_to_manager_list(player);
		if (this.observers().exist(player.getAccount_id())) {
			this.observers().exit(player.getAccount_id());
			this.observers().enter(player);
		} else {
			this.observers().enter(player);
		}
		this._player_result.game_score[seat_index] = 0;
		this.get_players()[seat_index] = null;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		room_players().send(player, roomResponse);
		send_response_to_other(seat_index, roomResponse);
		this.return_data(player, GameConstants.CMD_STAND_UP, true, "您已被房主踢出游戏桌");
		this.return_data(this.getCreate_player(), GameConstants.CMD_STAND_UP, true, "");
		int player_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			if (this.get_players()[i] == null)
				continue;
			player_count++;
		}
		if (player_count < 2) {
			this._restart_room_own = true;
		}
		return true;
	}

	public boolean go_to(int seat_index) {
		Player player = this.get_players()[seat_index];
		if (player == null) {
			return true;
		}
		if (seat_index != _cur_banker || this.is_mj_type(GameConstants.GAME_TYPE_DZH)) {
			if (this._player_result.game_score[seat_index] < this._init_momey / 40) {
				String des = "您的台面分数不足最低下注分数,是否进行补分？";
				this._operate_str[seat_index] = des;
				this._operate_type[seat_index] = GameConstants.CMD_END_ADD_SCORE;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_ADD_SCORE;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_CANCEL;
				this.popup_message(player.getAccount_id(), player.get_seat_index(), -1, this._operate_type[seat_index],
						this._operate_button[seat_index], this._operate_str[seat_index]);
				return true;
			} else {
				this.handler_player_ready(seat_index, false);
			}
		} else {
			if (this._player_result.game_score[seat_index] < this._init_momey / 40 * min_count_player()
					&& this._hou_shou_score >= this._init_momey * 2 && this._add_banker_times[seat_index] == 0) {
				String des = "您的台面分已清零，是否添庄继续坐庄？";
				this._operate_str[seat_index] = des;
				this._operate_type[seat_index] = GameConstants.CMD_END_ADD_BANKER;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_ADD_BANKER;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_CANCEL;
				this.popup_message(player.getAccount_id(), player.get_seat_index(), -1, this._operate_type[seat_index],
						this._operate_button[seat_index], this._operate_str[seat_index]);
			} else if (this._player_result.game_score[seat_index] < this._init_momey / 40 * min_count_player()
					&& this._hou_shou_score < this._init_momey * 2 && this._add_banker_times[seat_index] == 0) {
				String des = "您的台面分数不足最低下注分数,是否进行补分？";
				this._operate_str[seat_index] = des;
				this._operate_type[seat_index] = GameConstants.CMD_END_ADD_SCORE;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_ADD_SCORE;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_CANCEL;
				this.popup_message(player.getAccount_id(), player.get_seat_index(), -1, this._operate_type[seat_index],
						this._operate_button[seat_index], this._operate_str[seat_index]);
			} else if (this._add_banker_times[seat_index] == 1) {

				if (this._player_result.game_score[seat_index] + this._hou_shou_score < this._init_momey / 40 * min_count_player()) {
					cancel_banker(seat_index);
					String des = "您的台面分数不足最低下注分数,是否进行补分？";
					this._operate_str[seat_index] = des;
					this._operate_type[seat_index] = GameConstants.CMD_END_ADD_SCORE;
					this._operate_button[seat_index] |= GameConstants.JDB_POP_ADD_SCORE;
					this._operate_button[seat_index] |= GameConstants.JDB_POP_CANCEL;
					this.popup_message(player.getAccount_id(), player.get_seat_index(), -1, this._operate_type[seat_index],
							this._operate_button[seat_index], this._operate_str[seat_index]);
					this.send_error_notify(seat_index, 0, "由于您的台面分已清零并且无法继续添庄，现将下移庄位");

					change_banker(seat_index);
					// this.handler_player_ready(seat_index, false);
				} else if (this._player_result.game_score[seat_index] >= this._init_momey / 40 * min_count_player()) {
					this.handler_player_ready(seat_index, false);
				} else if (this._player_result.game_score[seat_index] + this._hou_shou_score >= this._init_momey / 40 * min_count_player()) {
					cancel_banker(seat_index);
					change_banker(seat_index);
				}

			} else {
				this.handler_player_ready(seat_index, false);
			}
		}

		return true;
	}

	public int min_count_player() {
		int count = 3;
		return count;

	}

	public boolean sure_bu_money(boolean is_bu, long apply_account_id) {
		Player player = this.apply_score_players().getPlayer(apply_account_id);
		if (player == null) {
			return false;
		}
		if (is_bu == false) {
			this.send_error_notify(player, 1, "您的补分申请未通过，请稍后再申请！");
			apply_score_players().exit(apply_account_id);
			this.huan_dou_aa(player, (int) this._bu_fen_glod, 2);
		} else {

			Player player_seat = this.get_player(apply_account_id);
			Player player_room = this.room_players().getPlayer(apply_account_id);
			if (player_seat != null) {
				if (this.is_mj_type(GameConstants.GAME_TYPE_JDB) && player_seat.get_seat_index() == this._last_banker) {
					long carry_money = player_seat.getCarry_score();
					player_seat.setCarry_score(carry_money + player.getBu_score());
					this._hou_shou_score += player.getBu_score();
					this.set_banker_to_room(5);
					if (this._player_ready[player_seat.get_seat_index()] == 0) {

						this.go_to(player_seat.get_seat_index());
					}
				} else {
					long carry_money = player_seat.getCarry_score();
					player_seat.setCarry_score(carry_money + player.getBu_score());
					this._player_result.game_score[player_seat.get_seat_index()] = this._player_result.game_score[player_seat.get_seat_index()]
							+ player.getBu_score();
					if (this._player_ready[player_seat.get_seat_index()] == 0) {

						this.go_to(player_seat.get_seat_index());
					}
				}
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				roomResponse.setGameStatus(_game_status);
				roomResponse.setAppId(this.getGame_id());

				this.load_room_info_data(roomResponse);
				this.load_player_info_data(roomResponse);
				room_players().send(player, roomResponse);
				send_response_to_other(player_seat.get_seat_index(), roomResponse);

			} else if (player_room != null) {
				player_room.setCarry_score(player_room.getCarry_score() + player.getBu_score());
				player_room.setGame_score(player_room.getGame_score() + player.getBu_score());
				player_room.setBu_score(0);
			}

			apply_score_players().exit(apply_account_id);
		}
		this.update_apply_bu_money_list();
		return true;
	}

	public boolean sure_seat(boolean is_seat, boolean is_display, long apply_account_id) {
		Player player = apply_seat_players().getPlayer(apply_account_id);
		if (player == null) {
			return false;
		}
		Player room_player = room_players().getPlayer(apply_account_id);
		if (room_player == null)
			return false;
		if (is_seat == false) {

			room_player.setLeave_timer(System.currentTimeMillis() / 1000);
			if (is_display == true)
				this.send_error_notify(room_player, 1, "您的入座申请未通过，请20秒后再进行申请！");
			apply_seat_players().exit(apply_account_id);
			if (player.getJoin_game() == false) {
				this.huan_dou_aa(player, (int) this._enter_gold, 1);
			}
		} else {
			int seat_index = GameConstants.INVALID_SEAT;
			if (get_players()[player.get_apply_index()] != null) {
				player = null;
				return false;
			}

			seat_index = player.get_apply_index();
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

			if (!onPlayerEnterUpdateRedis(player.getAccount_id())) {
				send_error_notify(player, 1, "已在其他房间中");
				return false;
			}
			if (player.getAccount_id() == this.getRoom_owner_account_id()) {
				this.getCreate_player().set_seat_index(seat_index);
			}
			player.setJoin_game(true);
			player.set_seat_index(seat_index);
			_player_result.game_score[seat_index] = room_player.getGame_score();
			get_players()[player.get_apply_index()] = room_player;
			this.room_players().enter(player);
			apply_seat_players().exit(apply_account_id);
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
			if (this.max_player_game() == this._start_game_player) {
				apply_same_all_delete();
			} else
				apply_same_seat_delete(seat_index);
			// 同步数据
			// 同步数据

			// ========同步到中心========
			roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
			roomRedisModel.getNames().add(player.getNick_name());
			// 写入redis
			SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
			if (player.getGame_score() < this._init_momey / 40 * min_count_player()) {
				String des = "您的台面分数不足最低下注分数,是否进行补分？";
				this._operate_str[seat_index] = des;
				this._operate_type[seat_index] = GameConstants.CMD_END_ADD_SCORE;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_ADD_SCORE;
				this._operate_button[seat_index] |= GameConstants.JDB_POP_CANCEL;

				this.popup_message(this.get_players()[seat_index].getAccount_id(), this.get_players()[seat_index].get_seat_index(), -1,
						this._operate_type[seat_index], this._operate_button[seat_index], this._operate_str[seat_index]);

			} else
				this.handler_player_ready(seat_index, false);
		}
		update_apply_list();
		return true;
	}

	public void apply_same_all_delete() {
		for (Player player : apply_seat_players().room_infoCollection()) {
			if (player == null)
				continue;

			this.send_error_notify(player, 2, "游戏已经满员了！ ");
			apply_seat_players().exit(player.getAccount_id());

		}
	}

	public void apply_same_seat_delete(int seat_index) {
		for (Player player : apply_seat_players().room_infoCollection()) {
			if (player == null)
				continue;
			if (player.get_apply_index() == seat_index) {
				this.send_error_notify(player, 2, "您的位置已经有人坐下了，请选择其它位置");
				apply_seat_players().exit(player.getAccount_id());
			}
		}
	}

	public boolean sure_seat_off(boolean is_seat, long apply_account_id) {
		for (Player rplayer : apply_seat_players().room_infoCollection()) {
			Player player = apply_seat_players().getPlayer(rplayer.getAccount_id());
			if (player == null) {
				continue;
			}
			Player room_player = room_players().getPlayer(rplayer.getAccount_id());
			if (room_player == null)
				continue;
			if (is_seat == false) {

				room_player.setLeave_timer(System.currentTimeMillis() / 1000);
				this.send_error_notify(room_player, 1, "您的入座申请未通过，请20秒后再进行申请！");
				apply_seat_players().exit(rplayer.getAccount_id());
			}
		}
		update_apply_list();
		return true;
	}

	public boolean sure_add_score(boolean is_bu, boolean is_display, long apply_account_id) {
		for (Player rplayer : apply_score_players().room_infoCollection()) {
			Player player = this.apply_score_players().getPlayer(rplayer.getAccount_id());
			if (player == null) {
				continue;
			}
			if (is_bu == false) {
				if (is_display == true)
					this.send_error_notify(player, 1, "您的补分申请未通过，请稍后再申请！");
				this.apply_score_players().exit(rplayer.getAccount_id());
				this.huan_dou_aa(player, (int) this._bu_fen_glod, 2);
			} else {
				Player player_seat = this.get_player(rplayer.getAccount_id());
				Player player_room = this.room_players().getPlayer(rplayer.getAccount_id());
				if (player_seat != null) {
					if (this.is_mj_type(GameConstants.GAME_TYPE_JDB) && player_seat.get_seat_index() == this._last_banker) {
						long carry_money = player_seat.getCarry_score();
						player_seat.setCarry_score(carry_money + player.getBu_score());
						this._hou_shou_score += player.getBu_score();
						if (this._player_ready[player_seat.get_seat_index()] == 0) {

							this.go_to(player_seat.get_seat_index());
						}
					} else {
						long carry_money = player_seat.getCarry_score();
						player_seat.setCarry_score(carry_money + player.getBu_score());
						this._player_result.game_score[player_seat.get_seat_index()] = this._player_result.game_score[player_seat.get_seat_index()]
								+ player.getBu_score();
						if (this._player_ready[player_seat.get_seat_index()] == 0) {
							this.go_to(player_seat.get_seat_index());
						}
					}

				} else if (player_room != null) {
					player_room.setCarry_score(player_room.getCarry_score() + player.getBu_score());
					player_room.setGame_score(player_room.getGame_score() + player.getBu_score());
					player_room.setBu_score(0);
				}

				apply_score_players().exit(rplayer.getAccount_id());
			}
		}
		this.update_apply_bu_money_list();
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
				if (_liang_schedule[i] != null) {
					_liang_schedule[i].cancel(false);
					_liang_schedule[i] = null;
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

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			if (player != null)
				_gameRoomRecord.release_players[seat_index] = 1;// 同意
			else
				_gameRoomRecord.request_player_seat = -2;

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

			for (int j = 0; j < playerNumber; j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
			for (Player player_observer : this.observers().observerCollection()) {
				if (player_observer == null)
					continue;
				send_error_notify(player_observer, 1, "游戏解散成功!");
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
			this.set_timer(this._cur_game_timer, this._cur_operate_time);
			if (this._cur_operate_time > 0) {
				Timer_OX.Builder timer = Timer_OX.newBuilder();
				timer.setDisplayTime(_cur_operate_time);
			}
			int sort_count = 0;
			if (this._game_status == GameConstants.GS_JDB_OPEN_CARD) {
				int loop = 0;
				int i = this._first_seat;
				while (loop < 4) {
					loop++;

					int time = 1;
					if (this._liang_card_player[i] == false)
						_liang_schedule[i] = GameSchedule.put(new LiangCardRunnable(getRoom_id(), i), time * (loop + 1), TimeUnit.SECONDS);
					i = (i + 2) % this.getTablePlayerNumber();
				}
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
				for (Player player_observer : this.observers().observerCollection()) {
					if (player_observer == null)
						continue;
					send_error_notify(player_observer, 2, "游戏已解散");
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
				if (!(GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status)) {
					// 游戏已经开始
					send_error_notify(seat_index, 2, "您正在游戏 ,不能退出，请游戏结束时离开!");
					return false;
				}

			}
			if (get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id())
				this._owner_seat = -1;
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());

			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);
			is_game_start();
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
			for (Player player_observer : this.observers().observerCollection()) {
				if (player_observer == null)
					continue;
				send_error_notify(player_observer, 2, "游戏已被创建者解散");
			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	public boolean control_game_start() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ROOM_OWNER_START);
		roomResponse.setEffectType(this.is_game_start);
		roomResponse.setPaoDes(getRoom_owner_name());
		if (this.is_game_start != 2)
			this.send_response_to_room(roomResponse);
		Player player = observers().getPlayer(getRoom_owner_account_id());
		if (player == null) {
			player = this.get_player(getRoom_owner_account_id());
			if (player != null)
				this.send_response_to_player(player.get_seat_index(), roomResponse);
		} else {
			observers().send(player, roomResponse);
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

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);

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

		// this._handler = this._handler_finish;
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

		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		return GameConstants.JDB_SIT_MAX;
	}

	public boolean huan_dou_aa(Player player, int cost_dou, int type) {
		// 是否免费的int game_id = 0;
		int game_type_index = this._game_type_index;
		int index = 0;
		int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		String des = "";
		if (type == 1) {
			des = "游戏中入坐还豆";
		}
		if (type == 2) {
			des = "游戏中补分还豆";
		}
		if (type == 3) {
			des = "游戏中添加还豆";
		}
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);
		if (sysParamModel != null && sysParamModel.getVal2() != 1) {
			return true;
		}

		StringBuilder buf = new StringBuilder();
		buf.append(des + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id()).append(",game_type_index:" + _game_type_index)
				.append(",game_round:" + _game_round).append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
		// 把豆还给玩家
		AddGoldResultModel addresult = PlayerServiceImpl.getInstance().addGold(player.getAccount_id(), cost_dou, false, buf.toString(),
				EGoldOperateType.FAILED_ROOM);
		if (addresult.isSuccess() == false) {
			logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败" + des);
		}

		return true;
	}

	private boolean kou_dou_aa(Player cur_player, long cost_dou, int type) {
		int game_id = 0;
		int game_type_index = this._game_type_index;
		int index = SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index);
		game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		int check_gold = (int) cost_dou;
		// 注意游戏ID不一样
		String des = "";
		if (type == 1) {
			des = "游戏中入坐扣豆";
		}
		if (type == 2) {
			des = "游戏中补分扣豆";
		}
		if (type == 3) {
			des = "游戏中添加扣豆";
		}
		if (type == 4) {
			des = "游戏中搓牌扣豆";
		}

		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);
		if (sysParamModel != null && sysParamModel.getVal2() == 1) {
			// 收费
			StringBuilder buf = new StringBuilder();
			buf.append(des + this.getRoom_id()).append("game_id:" + this.getGame_id()).append(",game_type_index:" + game_type_index)
					.append(",game_round:" + _game_round);
			AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(cur_player.getAccount_id(), check_gold, false, buf.toString());
			if (result.isSuccess() == false) {
				return false;
			} else {
				// 扣豆成功
				cost_dou = check_gold;
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

	public boolean robot_banker_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._player_call_banker[i] == -1) {

				this.call_banker(i, 0);

			}
		}
		return false;
	}

	public boolean ready_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++)
			if (this.get_players()[i] != null) {
				if (this._player_ready[i] == 0 && this._operate_button[i] == 0) {
					go_to(i);

				}
			}
		return false;
	}

	public boolean add_jetton_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (i == _cur_banker)
				continue;
			// if (this._jetton_player[i] == 0) {
			//
			// this.result_jetton( i, 0);
			//
			// }
		}
		return false;
	}

	@Override
	public boolean open_card_timer() {
		// TODO Auto-generated method stub

		return false;
	}

	public long caculate_max_jetton(int seat_index) {
		long max_jetton_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
			max_jetton_score += this._jetton_player[seat_index][i];
		}
		return max_jetton_score;
	}

	public boolean is_can_jetton() {
		long max_jetton = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._is_pass_jetton[i] == 2)
				continue;
			max_jetton += this._max_player_jetton[i];
		}
		if (max_jetton + this._max_jetton_score > 0)
			return true;
		else
			return false;
	}

	public boolean animation_timer(int timer_id) {
		switch (timer_id) {
		case ID_TIMER_ANIMATION_START: {
			// this._cur_banker = this._next_banker;
			if (is_mj_type(GameConstants.GAME_TYPE_JDB)) {
				if (this._cur_round == 1 || this._restart_room_own == true) {
					if (_owner_seat != -1)
						_cur_banker = _owner_seat;
					else {
						this._game_status = GameConstants.GS_JDB_CALL_BANKER;
						this.set_timer(ID_TIMER_ANIMATION_ROBOT, this._idi_call_banker_time);
						if (this._restart_room_own == true && this._hou_shou_score != 0)
							this.cancel_banker(this._cur_banker);
						this._restart_room_own = false;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (this._player_status[i] == false)
								continue;

							update_button(0, i, this._idi_call_banker_time, true);
						}
					}
					return true;
				}

				if (this._next_banker == -1) {
					for (int j = _banker_times; j >= 0; j--) {
						int chairID[] = new int[this.getTablePlayerNumber()];
						int chair_count = 0;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (this._player_status[i] == false)
								continue;
							if ((this._player_status[i] == true) && (_call_banker_info[j] == this._player_call_banker[i])) {
								if (i % 2 == 0)
									chairID[chair_count++] = i;
							}

						}
						if (chair_count > 0) {
							int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
							int temp = rand % chair_count;
							_cur_banker = chairID[temp];
							this._last_banker = _cur_banker;
							_banker_times = _player_call_banker[_cur_banker];
							if (_banker_times == 0)
								_banker_times = 1;
							break;
						}
					}
					set_banker_to_room(1);
					calculate_player_jetton_score();
				} else if (this._last_banker != this._next_banker) {
					this._cur_banker = this._next_banker;
					set_banker_to_room(1);
				} else {
					this._cur_banker = this._next_banker;
					set_banker_to_room(3);
				}
				this._last_banker = this._cur_banker;
				result_qie_card();

			} else if (is_mj_type(GameConstants.GAME_TYPE_DZH)) {
				_cur_banker = 0;
				set_banker_to_room(3);
				_cur_banker = this._next_banker;
				this._last_banker = _cur_banker;
				calculate_player_jetton_score();
				result_qie_card();

			}

			return true;
		}
		case ID_TIMER_ANIMATION_ROBOT: {

			for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
				if (this._player_status[i] == false)
					continue;
				if (this._player_call_banker[i] != -1)
					continue;
				this.call_banker(i, 0);
			}

			return true;
		}
		case ID_TIMER_ANIMATION_QIE_DALAY: {

			this.send_card(1, 0);
			this._last_banker = this._cur_banker;
			calculate_player_jetton_score();
			this._max_jetton_score = (long) this._player_result.game_score[this._cur_banker];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				update_button(1, i, this._idi_jetton_time_one, true);
			}
			this._game_status = GameConstants.GS_JDB_ADD_JETTON_ONE;
			set_timer(ID_TIMER_JETTON_ONE, this._idi_jetton_time_one);
			return true;
		}
		case ID_TIMER_JETTON_ONE: {
			if (this._max_jetton_score == 0) {
				this.open_card_operate();
				return true;
			}
			boolean flag = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i += 2) {
				if (this._player_status[i] == false)
					continue;
				if (this._cur_banker == i)
					continue;
				if (this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
					continue;
				if (this._max_player_jetton[i] == 0) {
					this._is_pass_jetton[i] = 1;
					if (this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score)
						this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					else
						this._max_player_jetton[i] = this._max_jetton_score;
					flag = true;
				} else {
					this._max_player_jetton[i] = 0;
				}
			}
			if (flag == false) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					if (this._cur_banker == i)
						continue;
					if (i % 2 == 0) {
						this._is_pass_jetton[i] = 0;
						this._max_player_jetton[i] = 0;
						continue;
					}
					if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
						continue;
					if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score)
						this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
					else if (i % 2 == 1)
						this._max_player_jetton[i] = this._max_jetton_score;
					else
						this._max_player_jetton[i] = 0;
					flag = true;

				}
				if (flag == false) {
					this.open_card_operate();
					return true;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_status[i] == false)
						continue;
					update_button(3, i, this._idi_jetton_time_three, true);
				}
				this._game_status = GameConstants.GS_JDB_ADD_JETTON_THREE;
				set_timer(ID_TIMER_JETTON_THREE, this._idi_jetton_time_three);
				return true;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				update_button(2, i, this._idi_jetton_time_two, true);
			}
			this._game_status = GameConstants.GS_JDB_ADD_JETTON_TWO;
			set_timer(ID_TIMER_JETTON_TWO, this._idi_jetton_time_two);
			return true;
		}
		case ID_TIMER_JETTON_TWO: {
			if (this._max_jetton_score == 0) {
				this.open_card_operate();
				return true;
			}
			boolean flag = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				if (this._cur_banker == i)
					continue;
				if (i % 2 == 0) {
					this._is_pass_jetton[i] = 0;
					this._max_player_jetton[i] = 0;
					continue;
				}
				if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) == 0)
					continue;
				if (i % 2 == 1 && this._player_result.game_score[i] - this.caculate_max_jetton(i) < this._max_jetton_score) {
					this._max_player_jetton[i] = (long) this._player_result.game_score[i] - this.caculate_max_jetton(i);
				} else if (i % 2 == 1)
					this._max_player_jetton[i] = this._max_jetton_score;

				flag = true;

			}
			if (flag == false) {
				this.open_card_operate();
				return true;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				update_button(3, i, this._idi_jetton_time_three, true);

			}
			this._game_status = GameConstants.GS_JDB_ADD_JETTON_THREE;
			set_timer(ID_TIMER_JETTON_THREE, this._idi_jetton_time_three);
			return true;
		}
		case ID_TIMER_JETTON_THREE: {

			this.kill_timer();
			this.open_card_operate();
			return true;
		}
		case ID_TIMER_LIANG_PAI: {

			int loop = 0;
			int i = this._first_seat;
			while (loop < 4) {
				loop++;

				int time = 1;

				_liang_schedule[i] = GameSchedule.put(new LiangCardRunnable(getRoom_id(), i), time * (loop + 1), TimeUnit.SECONDS);
				i = (i + 2) % this.getTablePlayerNumber();
			}

			return true;
		}
		case ID_TIMER_GO_TO: {
			this.ready_timer();
			return true;
		}
		}

		return false;
	}

	public boolean liang_card_timer(int seat_index) {
		liang_pai(seat_index);
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
