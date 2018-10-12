package com.cai.game.wsk.xiangtanxiaozao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dmz.DmzRsp.PaiFenData;
import protobuf.clazz.tcdg.TcdgRsp.Opreate_RequestWsk_tcdg;
import protobuf.clazz.xtxz.xtxzRsp.BiaoTaiResponse_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.CallBankerResponse_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.GameStart_Wsk_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.LiangPai_Result_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.Opreate_RequestWsk_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.OutCardDataWsk_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.PukeGameEndWsk_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.RefreshCardData_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.RefreshMingPai_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.RefreshScore_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.TableResponse_xtxz;


public class Table_XTXZ extends AbstractRoom {
	private static final long serialVersionUID = -2419887548488809773L;

	protected static Logger logger = Logger.getLogger(Table_XTXZ.class);

	// suitInvalid = -1,--无效牌值
	// suitPass = 0, --过
	// suitSingle = 1, --单张
	// suitDouble = 2, --对子
	// suitThree = 3, --三张
	// suitTriAndSingle = 4, --三带一
	// suitTriAndTwo = 5, --三带二
	// suitStraight = 6, --顺子
	// suitDoubleStraight = 7, --双顺
	// suitPlane = 8, --飞机带翅膀
	// suitPlaneLost = 9, --飞机缺翅膀
	// suitFourAndSingle = 10, --四带一
	// suitFourAndTwo = 11, --四带二
	// suitFourAndThree = 12, --四带三
	// suitFlush = 13,--同花顺
	// suitRuanBomb = 14, --带赖子炸弹
	// suitBomb = 15, -- 炸弹
	// suitLaiziBomb = 16, --四个赖子炸弹
	// suitWangBomb = 17, --王炸
	// suitTriStraight = 18, --三顺
	// suitFourStraight = 19, --四顺(444455556666)
	// suitZa510K = 20,--假510K
	// suitZheng510K = 21,--真510K

	/**
	 * 当前正在进行某个操作的玩家
	 */
	public int _current_player = GameConstants.INVALID_SEAT;
	/**
	 * 上一个已经进行操作的玩家（包括过牌和正常出牌）
	 */
	public int _prev_player = GameConstants.INVALID_SEAT;
	/**
	 * 记录牌桌上当前的出牌玩家，用来处理断线重连和一些特殊情况，全局的更方便一些。
	 */
	public int _out_card_player = GameConstants.INVALID_SEAT;

	/**
	 * 所有玩家，当前打出的牌数据。
	 */
	public int _cur_out_card_data[][] = new int[getTablePlayerNumber()][get_hand_card_count_max()];;
	/**
	 * 所有玩家，当前打出的牌张数。
	 */
	public int _cur_out_card_count[] = new int[getTablePlayerNumber()];

	/**
	 * 本轮，上一个出牌人出的经过转换的数据。这样方便直接获取出的牌是什么类型的。
	 */
	public int _turn_out_card_data[] = new int[get_hand_card_count_max()];
	/**
	 * 本轮，上一个出牌人出的原始牌数据。
	 */
	public int _turn_real_card_data[] = new int[get_hand_card_count_max()];
	/**
	 * 本轮，上一个出牌人出的牌数量。
	 */
	public int _turn_out_card_count = 0;
	/**
	 * 本轮，上一个出牌人出的牌类型。
	 */
	public int _turn_out_card_type = 0;

	// 小结算面板里的显示数据
	/**
	 * 每个玩家抓的分
	 */
	public int _get_score[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家的输赢分
	 */
	public int _win_score[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家的彩头分
	 */
	public int _xi_qian_score[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家的花牌分
	 */
	public int _magic_card_score[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家的罚分
	 */
	public int _punish_score[] = new int[getTablePlayerNumber()];

	// 大结算面板里的显示数据
	/**
	 * 每个玩家所有小局中的最高抓分
	 */
	public int _max_get_score[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的独牌次数
	 */
	public int _solo_times[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的一游次数
	 */
	public int _first_finish_times[] = new int[getTablePlayerNumber()];
	public int _second_finish_times[] = new int[getTablePlayerNumber()];
	public int _thirdly_finish_times[] = new int[getTablePlayerNumber()];
	public int _fourthly_finish_times[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的六喜次数
	 */
	public int _five_boom_times[] = new int[getTablePlayerNumber()];
	public int _tmp_five_boom_times[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的六喜次数
	 */
	public int _six_boom_times[] = new int[getTablePlayerNumber()];
	public int _tmp_six_boom_times[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的七喜次数
	 */
	public int _seven_boom_times[] = new int[getTablePlayerNumber()];
	public int _tmp_seven_boom_times[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的八喜次数
	 */
	public int _eight_boom_times[] = new int[getTablePlayerNumber()];
	public int _tmp_eight_boom_times[] = new int[getTablePlayerNumber()];
	
	/**
	 * 每个玩家所有大局中的喜次数
	 */
	public int _xi_boom_times[] = new int[getTablePlayerNumber()];
	/**
	 * 每个玩家所有小局中的飘分输赢
	 */
	public int _piao_score[] = new int[getTablePlayerNumber()];

	/**
	 * 牌桌上，第一至第四个出完牌的人是谁
	 */
	public int _chuwan_shunxu[] = new int[getTablePlayerNumber()];
	/**
	 * 牌桌上，所有的五十K的牌
	 */
	public int _pai_score_card[] = new int[] { 0x05, 0x05, 0x15, 0x15, 0x25, 0x25, 0x35, 0x35, 0x0A, 0x0A, 0x1A, 0x1A, 0x2A, 0x2A, 0x3A, 0x3A, 0x0D,
			0x0D, 0x1D, 0x1D, 0x2D, 0x2D, 0x3D, 0x3D };
	/**
	 * 牌桌上，五十K的牌，一共有24张，每小局需要重置
	 */
	public int _pai_score_count = _pai_score_card.length;
	/**
	 * 牌桌上，五十K的牌，一共有200分，每小局需要重置
	 */
	public int _pai_score = 200;

	// 已经打出的分值牌
	public int _out_pai_score_card[] = new int[24];
	public int _out_pai_score_count = 0;
	public int _out_pai_score = 0;
	//总的牌分
	public int _all_pai_score = 200;

	/**
	 * 牌桌上，找朋友玩法时，所有人的队友
	 */
	public int _friend_seat[] = new int[getTablePlayerNumber()];

	public ScheduledFuture<?> _trustee_schedule[];
	public ScheduledFuture<?> _game_scheduled;

	public GameLogic_XTXZ _logic = new GameLogic_XTXZ();

	protected long _request_release_time;

	protected ScheduledFuture<?> _release_scheduled;
	protected ScheduledFuture<?> _table_scheduled;

	public AbstractHandler_XTXZ _handler;
	public HandlerOutCardOperate_XTXZ _handler_out_card_operate;
	public HandlerCallBnaker_XTXZ _handler_call_banker;
	public HandlerFinish_XTXZ _handler_finish;
	public HandlerPiao_XTXZ _handler_piao;
	public HandlerBiaoTai_XTXZ _handler_biao;

	/**
	 * 牌桌上，本轮打牌过程中，积累的总分值
	 */
	public int _turn_have_score = 0;
	/**
	 * 是否已经操作独牌。0还没操作，1已经点了操作(不管操作是‘独牌’还是‘不独’)
	 */
	public int _is_call_banker[] = new int[getTablePlayerNumber()];
	/**
	 * 是否已经操作独牌。0还没操作，1已经点了操作(不管操作是‘独牌’还是‘不独’)
	 */
	public int _is_biao_tai[] = new int[getTablePlayerNumber()];
	
	/**
	 * 是否是‘独牌’模式，也就是1打3，牌局中不计算抓分
	 */
	public boolean _is_yi_da_san = false;
	/**
	 * 牌桌上手牌排序的原则
	 */
	public int _sort_type[] = new int[getTablePlayerNumber()];
	/**
	 * 牌桌上，自动叫庄的那张牌
	 */
	public int _jiao_pai_card = GameConstants.INVALID_CARD;
	/**
	 * 牌桌上，和叫庄的那张牌对应的牌，是否已经出现，没出现的时候是-1，出现了之后=_jiao_pai_card
	 */
	public int _out_card_ming_ji = GameConstants.INVALID_CARD;;

	protected static final int GAME_OPREATE_TYPE_LIANG_PAI = 1; // 亮牌
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 2; // 叫庄
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER_NO = 3; // 不叫庄
	protected static final int GAME_OPREATE_TYPE_SORT_CARD = 4; // 理牌
	protected static final int GAME_OPREATE_TYPE_WSK_SORT = 5; // 510k排序
	protected static final int GAME_OPREATE_TYPE_PASS = 6; //表态过
	protected static final int GAME_OPREATE_TYPE_XI_HONG = 7; //表态细红牌
	protected static final int GAME_OPREATE_TYPE_DA_HONG = 8; //表态大红牌
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG = 9; //投降

	/**
	 * 打出的硬五十K的数量，用于计算喜分
	 */
	public int _ying_wsk_count[] = new int[getTablePlayerNumber()];
	/**
	 * 打出的大小王牌的数量，用于计算喜分
	 */
	public int _out_magic_count[] = new int[getTablePlayerNumber()];
	/**
	 * 打出的花牌的数量，用于计算喜分
	 */
	public int _out_flower_count[] = new int[getTablePlayerNumber()];
	/**
	 * 玩家起手抓的花牌数量
	 */
	public int _qishou_flower_count[] = new int[getTablePlayerNumber()];
	/**
	 * 本轮炸队友的次数
	 */
	public int _cur_round_false_bomb[] = new int[getTablePlayerNumber()];
	/**
	 * 标记是否是先炸队友
	 */
	public boolean _is_first_false_bomb[] = new boolean[getTablePlayerNumber()];

	//public boolean hasRuleLiuZhaYiFen = false;
	public boolean hasRuleBoomMustOut = false;
	public boolean hasRuleDisplayCount = false;
	public boolean hasRuleWuZhaSuanXi = false;
	public boolean hasRuleSanDaiEr = false;
	public boolean hasRuleShangWang = false;
	
	public int have_tou_qiang;//0没有投降，1有投降
	public int shuang_wang_seat;
	
	public boolean after_out_finish;
	public boolean can_last_out_finish[];
	
	public int jiang_fa_socre[];
	

	public int[] player_sort_card = new int[getTablePlayerNumber()];

	public Table_XTXZ() {
		super(RoomType.MJ, GameConstants.GAME_PLAYER);

		_game_type_index = GameConstants.GAME_TYPE_PK_TONG_CHENG;
		_game_status = GameConstants.GS_MJ_FREE;

		_logic.ruleMap = ruleMap;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}

		_player_ready = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;
		_game_rule_index = game_rule_index;

		_player_result = new PlayerResult(getRoom_owner_account_id(), getRoom_id(), _game_type_index, _game_rule_index, _game_round, get_game_des(),
				getTablePlayerNumber());

		_handler_out_card_operate = new HandlerOutCardOperate_XTXZ();
		_handler_call_banker = new HandlerCallBnaker_XTXZ();
		_handler_finish = new HandlerFinish_XTXZ();
		_handler_piao = new HandlerPiao_XTXZ();
		_handler_biao = new HandlerBiaoTai_XTXZ();

		_max_get_score = new int[getTablePlayerNumber()];
		_solo_times = new int[getTablePlayerNumber()];
		_first_finish_times = new int[getTablePlayerNumber()];
		_second_finish_times = new int[getTablePlayerNumber()];
		_thirdly_finish_times = new int[getTablePlayerNumber()];
		_fourthly_finish_times = new int[getTablePlayerNumber()];
		_five_boom_times = new int[getTablePlayerNumber()];
		_six_boom_times = new int[getTablePlayerNumber()];
		_seven_boom_times = new int[getTablePlayerNumber()];
		_eight_boom_times = new int[getTablePlayerNumber()];

		hasRuleBoomMustOut = has_rule(GameConstants.GAME_RULE_PK_TC_BOOM_MUST_OUT);
		hasRuleDisplayCount = has_rule(GameConstants.GAME_RULE_WSK_SHOW_CARD_COUNT_XTXZ);
		hasRuleSanDaiEr = has_rule(GameConstants.GAME_RULE_WSK_CAN_3_TAKE_2_XTXZ);
		hasRuleWuZhaSuanXi = has_rule(GameConstants.GAME_RULE_WSK_WU_XI_XTXZ);
		hasRuleShangWang = has_rule(GameConstants.GAME_RULE_WSK_TAKE_KING_2_XTXZ);	
				
		_logic.has_san_dai_er = hasRuleSanDaiEr;
		_logic.has_shuang_wang = hasRuleShangWang;
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		handler_request_trustee(_seat_index, true, 0);
	}

	public int get_hand_card_count_max() {
		return GameConstants.WSK_MAX_COUNT - 1;
	}

	protected void initBanker() {
		_cur_banker = 0;
		// _cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}

	public boolean reset_init_data() {
		if (_cur_round == 0) {
			initBanker();
			record_game_room();
		}

		_cur_round_false_bomb = new int[getTablePlayerNumber()];
		_qishou_flower_count = new int[getTablePlayerNumber()];

		_pai_score_card = new int[] { 0x05, 0x05, 0x15, 0x15, 0x25, 0x25, 0x35, 0x35, 0x0A, 0x0A, 0x1A, 0x1A, 0x2A, 0x2A, 0x3A, 0x3A, 0x0D, 0x0D,
				0x1D, 0x1D, 0x2D, 0x2D, 0x3D, 0x3D };
		_pai_score_count = _pai_score_card.length;
		_pai_score = 200;

		_out_card_ming_ji = GameConstants.INVALID_CARD;

		_cur_out_card_data = new int[getTablePlayerNumber()][get_hand_card_count_max()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}
		_cur_out_card_count = new int[getTablePlayerNumber()];
		Arrays.fill(_cur_out_card_count, 0);

		_turn_out_card_count = 0;
		_turn_out_card_data = new int[get_hand_card_count_max()];
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		_turn_out_card_type = 0;

		_get_score = new int[getTablePlayerNumber()];
		_win_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_magic_card_score = new int[getTablePlayerNumber()];
		_punish_score = new int[getTablePlayerNumber()];
		_piao_score = new int[getTablePlayerNumber()];

		_chuwan_shunxu = new int[getTablePlayerNumber()];
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);

		_friend_seat = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = i;
		}
		_is_call_banker = new int[getTablePlayerNumber()];
		_is_biao_tai = new int[getTablePlayerNumber()];
		_turn_have_score = 0;
		_is_yi_da_san = false;
		have_tou_qiang = 0;
		shuang_wang_seat = -1;
		can_last_out_finish = new boolean[getTablePlayerNumber()];
		jiang_fa_socre = new int[getTablePlayerNumber()];

		_sort_type = new int[getTablePlayerNumber()];
		Arrays.fill(_sort_type, GameConstants.WSK_ST_ORDER);
		_jiao_pai_card = GameConstants.INVALID_CARD;

		_turn_real_card_data = new int[get_hand_card_count_max()];

		_run_player_id = 0;

		_ying_wsk_count = new int[getTablePlayerNumber()];
		_out_magic_count = new int[getTablePlayerNumber()];
		_out_flower_count = new int[getTablePlayerNumber()];
		_tmp_five_boom_times = new int[getTablePlayerNumber()];
		_tmp_six_boom_times = new int[getTablePlayerNumber()];
		_tmp_seven_boom_times = new int[getTablePlayerNumber()];
		_tmp_eight_boom_times = new int[getTablePlayerNumber()];

		_out_pai_score_card = new int[24];
		_out_pai_score_count = 0;
		_out_pai_score = 0;
		_all_pai_score = 200;

		_is_first_false_bomb = new boolean[getTablePlayerNumber()];

		player_sort_card = new int[getTablePlayerNumber()];

		GRR = new GameRoundRecord(getTablePlayerNumber(), GameConstants.MAX_WEAVE_LAOPAI, GameConstants.WSK_MAX_COUNT,
				GameConstants.MAX_INDEX_LAOPAI);

		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_end_reason = GameConstants.INVALID_VALUE;
		istrustee = new boolean[getTablePlayerNumber()];

		_playerStatus = new PlayerStatus[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus();
			_player_result.pao[i] = -1;
		}

		_cur_round++;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}

		GRR._room_info.setRoomId(getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(getRoom_owner_account_id());

		if (_cur_round == 0) {
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
				if (rplayer.locationInfor != null) {
					room_player.setLocationInfor(rplayer.locationInfor);
				}
				GRR._video_recode.addPlayers(room_player);
			}
		}

		GRR._video_recode.setBankerPlayer(_cur_banker);

		return true;
	}
	
	public void InitPama(){
		have_tou_qiang = 0;
		shuang_wang_seat = -1;
		after_out_finish = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			can_last_out_finish[i] = false;
			_is_biao_tai[i] = -1;
			jiang_fa_socre[i] = 0;
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();
		
		InitPama();
		
		progress_banker_select();

		GRR._banker_player = _current_player = _cur_banker;

		_game_status = GameConstants.GS_TC_WSK_CALLBANKER;

		init_shuffle();

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	protected boolean on_game_start() {
		
		//if (hasRulePiao) {
		//	set_handler(_handler_piao);
		//	_handler_piao.exe(this);
		//	return true;
		//}
		return on_game_start_real();
	}

	protected boolean on_game_start_real() {
		for (int play_index = 0; play_index < getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_GAME_START);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setRoomInfo(getRoomInfo());

			// 发送数据
			GameStart_Wsk_xtxz.Builder gamestart = GameStart_Wsk_xtxz.newBuilder();
			gamestart.setRoomInfo(getRoomInfo());
			load_player_info_data_game_start(gamestart);

			gamestart.setCurBanker(GameConstants.INVALID_SEAT);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if(this.hasRuleDisplayCount){
					gamestart.addCardCount(GRR._card_count[i]);
				}else{
					gamestart.addCardCount(0);
				}
				

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				@SuppressWarnings("unused")
				Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				gamestart.addCardsData(cards_card);
			}
			gamestart.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			// 自己才有牌数据
			send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_GAME_START);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_Wsk_xtxz.Builder gamestart = GameStart_Wsk_xtxz.newBuilder();
		gamestart.setRoomInfo(getRoomInfo());
		load_player_info_data_game_start(gamestart);

		gamestart.setCurBanker(GameConstants.INVALID_SEAT);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);

			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			@SuppressWarnings("unused")
			Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}

			gamestart.addCardsData(cards_card);
		}

		gamestart.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		GRR.add_room_response(roomResponse);
		have_tou_qiang = 0;
		shuang_wang_seat = get_shuang_wang_seat();
		if(this.has_rule(GameConstants.GAME_RULE_WSK_TAKE_KING_2_XTXZ) && shuang_wang_seat != -1){
			_game_status = GameConstants.GS_XTXZ_WSK_CALLBANKER;
			set_handler(_handler_call_banker);
			have_tou_qiang = 1;
			_current_player = shuang_wang_seat;
			_is_yi_da_san = true;
			roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_CALL_BANKER_RESULT);
			CallBankerResponse_xtxz.Builder callbanker_result = CallBankerResponse_xtxz.newBuilder();
			callbanker_result.setBankerPlayer(GRR._banker_player);
			callbanker_result.setOpreateAction(-1);
			callbanker_result.setCallPlayer(-1);
			callbanker_result.setCurrentPlayer(shuang_wang_seat);
			callbanker_result.setDisplayTime(10);
			callbanker_result.setRoomInfo(getRoomInfo());
			callbanker_result.setHaveTouQiang(have_tou_qiang);
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
			send_response_to_room(roomResponse);
		}
		
		//可包牌
		else if(this.has_rule(GameConstants.GAME_RULE_WSK_CAN_BAO_XTXZ)){
			_game_status = GameConstants.GS_XTXZ_WSK_CALLBANKER;
			set_handler(_handler_call_banker);
			roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_CALL_BANKER_RESULT);
			CallBankerResponse_xtxz.Builder callbanker_result = CallBankerResponse_xtxz.newBuilder();
			callbanker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
			callbanker_result.setOpreateAction(-1);
			callbanker_result.setCallPlayer(-1);
			callbanker_result.setCurrentPlayer(_current_player);
			callbanker_result.setDisplayTime(10);
			callbanker_result.setRoomInfo(getRoomInfo());
			callbanker_result.setHaveTouQiang(have_tou_qiang);
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
			send_response_to_room(roomResponse);
		}
		//叫牌
		else{
			_game_status = GameConstants.GS_XTXZ_WSK_LIANG_PAI;
			GRR._banker_player = _current_player;
			//发这条的目的是确定一下庄家的位置
			roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_CALL_BANKER_RESULT);
			CallBankerResponse_xtxz.Builder callbanker_result = CallBankerResponse_xtxz.newBuilder();
			callbanker_result.setBankerPlayer(GRR._banker_player);
			callbanker_result.setOpreateAction(-1);
			callbanker_result.setCallPlayer(-1);
			callbanker_result.setCurrentPlayer(-1);
			callbanker_result.setDisplayTime(10);
			callbanker_result.setRoomInfo(getRoomInfo());
			callbanker_result.setHaveTouQiang(have_tou_qiang);
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
			send_response_to_room(roomResponse);
			
			auto_liang_pai(_current_player);
		}


		refresh_pai_score(GameConstants.INVALID_SEAT);

		refresh_user_get_score(GameConstants.INVALID_SEAT);

		return true;
	}
	
	public int get_shuang_wang_seat(){
		for(int i = 0;i < getTablePlayerNumber();i++){
			int king_count = 0;
			for(int j = 0;j < GRR._card_count[i];j++){
				if(GRR._cards_data[i][j] == 0x4f){
					king_count++;
				}
			}
			if(king_count == 2){
				return i;
			}
		}
		return -1;
	}

	public void process_boom_score() {
		if (GRR != null && !hasRuleBoomMustOut) {
			int pCount = getTablePlayerNumber();
			for (int i = 0; i < pCount; i++) {
				_five_boom_times[i] += _tmp_five_boom_times[i];
				_six_boom_times[i] += _tmp_six_boom_times[i];
				_seven_boom_times[i] += _tmp_seven_boom_times[i];
				_eight_boom_times[i] += _tmp_eight_boom_times[i];

				int[] boomCount = _logic.get_boom_type_count(GRR._cards_data[i], GRR._card_count[i]);

				/*if (hasRuleLiuZhaYiFen) {
					_six_boom_times[i] += boomCount[2];
					_seven_boom_times[i] += boomCount[1];
					_eight_boom_times[i] += boomCount[0];
					_xi_qian_score[i] += (4 * boomCount[0] + 2 * boomCount[1] + 1 * boomCount[2]) * (pCount - 1);
					for (int j = 0; j < pCount; j++) {
						if (i == j)
							continue;
						_xi_qian_score[j] -= 4 * boomCount[0] + 2 * boomCount[1] + 1 * boomCount[2];
					}
				} else */{
					_seven_boom_times[i] += boomCount[1];
					_eight_boom_times[i] += boomCount[0];
					_xi_qian_score[i] += (2 * boomCount[0] + 1 * boomCount[1]) * (pCount - 1);
					for (int j = 0; j < pCount; j++) {
						if (i == j)
							continue;
						_xi_qian_score[j] -= 2 * boomCount[0] + 1 * boomCount[1];
					}
				}
			}
		}
	}

	public void load_player_info_data_game_start(GameStart_Wsk_xtxz.Builder roomResponse) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	protected void init_shuffle() {
		if(has_rule(GameConstants.GAME_RULE_WSK_NO_KING_XTXZ)){
			_repertory_card = new int[Constants_XTXZ.CARD_DATA_WSK_NO_KING.length];
			shuffle(_repertory_card, Constants_XTXZ.CARD_DATA_WSK_NO_KING);
		}else if(has_rule(GameConstants.GAME_RULE_WSK_TAKE_KING_2_XTXZ)){
			_repertory_card = new int[Constants_XTXZ.CARD_DATA_WSK_KING_2.length];
			shuffle(_repertory_card, Constants_XTXZ.CARD_DATA_WSK_KING_2);
		}else if(has_rule(GameConstants.GAME_RULE_WSK_TAKE_KING_4_XTXZ)){
			_repertory_card = new int[Constants_XTXZ.CARD_DATA_WSK_KING_4.length];
			shuffle(_repertory_card, Constants_XTXZ.CARD_DATA_WSK_KING_4);
		}
	};

	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = getTablePlayerNumber();

		int max_hand_count = get_hand_card_count_max();

		int dispatch_count = 0;

		if (has_rule(GameConstants.GAME_RULE_WSK_NO_KING_XTXZ)) {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = repertory_card[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		} else if (has_rule(GameConstants.GAME_RULE_WSK_TAKE_KING_4_XTXZ)) {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = repertory_card[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		} else if (has_rule(GameConstants.GAME_RULE_WSK_TAKE_KING_2_XTXZ)) {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				if (i != GRR._banker_player && i != (GRR._banker_player + 1) % getTablePlayerNumber())
					tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = repertory_card[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		} else {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = repertory_card[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		BACK_DEBUG_CARDS_MODE = true;
		debug_my_cards = new int[] { 
				79, 79, 78, 78, 8, 26, 22, 9, 05, 11, 58, 55, 61, 52, 53, 44, 51, 59, 12, 52, 04, 21, 42, 41, 58, 28, 21, 
				02, 19, 24, 50, 13, 53, 60, 18, 29, 05, 36, 49, 13, 33, 9, 28, 37, 38, 43, 36, 38, 54, 57, 20, 39, 19, 61,
				12, 18, 03, 37, 51, 01, 17, 35, 56, 41, 50, 26, 57, 8, 45, 59, 35, 03, 24, 42, 02, 22, 11, 60, 07, 25, 55,
				06, 33, 10, 39, 40, 10, 27, 07, 25, 29, 01, 06, 54, 49, 27, 43, 04, 23, 44, 17, 20, 23, 45, 34, 40, 56, 34};

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
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

	public void testRealyCard(int[] realyCards) {
		int count = getTablePlayerNumber();

		int max_hand_count = get_hand_card_count_max();

		int dispatch_count = 0;

		if (has_rule(GameConstants.GAME_RULE_PK_TC_ZERO_MAGIC)) {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = realyCards[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		} else if (has_rule(GameConstants.GAME_RULE_PK_TC_ONE_MAGIC)) {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				if (i != GRR._banker_player)
					tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = realyCards[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		} else if (has_rule(GameConstants.GAME_RULE_PK_TC_TWO_MAGIC)) {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				if (i != GRR._banker_player && i != (GRR._banker_player + 1) % getTablePlayerNumber())
					tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = realyCards[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		} else {
			for (int i = 0; i < count; i++) {
				int tmp_count = max_hand_count;
				tmp_count--;

				for (int j = 0; j < tmp_count; j++) {
					GRR._cards_data[i][j] = realyCards[dispatch_count + j];
				}

				dispatch_count += tmp_count;

				GRR._card_count[i] = tmp_count;

				_logic.sort_card_list_before_card_change(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
			}
		}

		for (int i = 0; i < count; i++) {
			_qishou_flower_count[i] = _logic.get_flower_count(GRR._cards_data[i], GRR._card_count[i]);
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(realyCards));

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	public void testSameCard(int[] cards) {
		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	/**
	 * 游戏正常结束的时候，计算喜钱分
	 */
	public void process_xi_qian() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_ying_wsk_count[i] >= 4) {
				int basic_xi_score = (_ying_wsk_count[i] - 3) * 2;

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (j == i) {
						_xi_qian_score[j] += basic_xi_score * (getTablePlayerNumber() - 1);
					} else {
						_xi_qian_score[j] -= basic_xi_score;
					}
				}
			}

			if (_out_magic_count[i] + _out_flower_count[i] >= 4) {
				int basic_xi_score = 1;
				if (_out_magic_count[i] == 3 && _out_flower_count[i] == 1)
					basic_xi_score = 1;
				if (_out_magic_count[i] == 3 && _out_flower_count[i] == 2)
					basic_xi_score = 2;
				if (_out_magic_count[i] == 4 && _out_flower_count[i] == 0)
					basic_xi_score = 2;
				if (_out_magic_count[i] == 4 && _out_flower_count[i] == 1)
					basic_xi_score = 3;
				if (_out_magic_count[i] == 4 && _out_flower_count[i] == 2)
					basic_xi_score = 4;

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (j == i) {
						_xi_qian_score[j] += basic_xi_score * (getTablePlayerNumber() - 1);
					} else {
						_xi_qian_score[j] -= basic_xi_score;
					}
				}
			}
		}
	}

	/**
	 * 牌局正常结算之后，计算花牌分
	 */
	public void process_flower_score() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int basic_flower_score = 0;
			if (_qishou_flower_count[i] == 1)
				basic_flower_score = 1;
			else if (_qishou_flower_count[i] == 2)
				basic_flower_score = 2;

			if (basic_flower_score > 0) {
				if (_win_score[i] > 0) {
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (j == i)
							continue;

						if (_win_score[j] < 0) {
							_magic_card_score[i] += basic_flower_score;
							_magic_card_score[j] -= basic_flower_score;
						}
					}
				} else {
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (j == i)
							continue;

						if (_win_score[j] > 0) {
							_magic_card_score[i] -= basic_flower_score;
							_magic_card_score[j] += basic_flower_score;
						}
					}
				}
			}
		}

	}

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		if (_chuwan_shunxu != null && _chuwan_shunxu[0] != GameConstants.INVALID_SEAT)
			_cur_banker = _chuwan_shunxu[0];

		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (_cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				load_player_info_data(roomResponse2);
				send_response_to_player(i, roomResponse2);
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_xtxz.Builder game_end_wsk = PukeGameEndWsk_xtxz.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			//cal_score_wsk_xtxz(_win_score);
			//process_xi_qian();
			//process_flower_score();
			//process_boom_score();

			if(_chuwan_shunxu[0] != GameConstants.INVALID_SEAT){
				_first_finish_times[_chuwan_shunxu[0]]++;
			}
			if(_chuwan_shunxu[1] != GameConstants.INVALID_SEAT){
				_second_finish_times[_chuwan_shunxu[1]]++;
			}
			if(_chuwan_shunxu[2] != GameConstants.INVALID_SEAT){
				_thirdly_finish_times[_chuwan_shunxu[2]]++;
			}
			if(_chuwan_shunxu[3] != GameConstants.INVALID_SEAT){
				_fourthly_finish_times[_chuwan_shunxu[3]]++;
			}
			

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (_get_score[j] > _max_get_score[j])
					_max_get_score[j] = _get_score[j];
			}
		}

		load_player_info_data_game_end(game_end_wsk);

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);

		if (GRR != null) {
			
			cal_score_wsk_xtxz(_win_score);
			
			game_end_wsk.setBankerPlayer(GRR._banker_player);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addCardCount(GRR._card_count[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_wsk.addCardsData(i, cards_card);

			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			
			if(this._is_yi_da_san){
				game_end_wsk.setBaoPlayer(GRR._banker_player);
				game_end_wsk.setBanPlayer(-1);
			}
			else{
				game_end_wsk.setBaoPlayer(-1);
				game_end_wsk.setBanPlayer(this._friend_seat[GRR._banker_player]);
			}
		}

		int[] win_order = new int[getTablePlayerNumber()];
		Arrays.fill(win_order, GameConstants.INVALID_SEAT);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (_chuwan_shunxu[j] == i) {
					win_order[i] = j;
					break;
				}
			}
		}

		if (reason != GameConstants.Game_End_NORMAL) {
			// 重置数据，防止非第1小局的时候，游戏没开始就解散了
			Arrays.fill(_get_score, 0);
			Arrays.fill(_win_score, 0);
			Arrays.fill(_xi_qian_score, 0);
			Arrays.fill(_magic_card_score, 0);
			Arrays.fill(_punish_score, 0);
			Arrays.fill(_player_result.pao, 0);
			Arrays.fill(_piao_score, 0);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += _win_score[i];
			get_players()[i].setGame_score((long) _player_result.game_score[i]);

			game_end.addGameScore(_win_score[i]);

			game_end_wsk.addWinOrder(win_order[i]);//出完牌的顺序
			game_end_wsk.addJiangFaScore(jiang_fa_socre[i]);//奖罚分
			game_end_wsk.addZhuaScore(_get_score[i]); // 抓分
			game_end_wsk.addXiScore(_xi_qian_score[i]); // 喜分
			game_end_wsk.addHongPai(_is_biao_tai[i]); // 表态
			game_end_wsk.addDangJuScore(_win_score[i]);
			
			//game_end_wsk.addYingFenScore(_win_score[i]); // 赢分
			//game_end_wsk.addXianQianScore(_xi_qian_score[i]); // 彩头
			//game_end_wsk.addChengFaScore(_punish_score[i]); // 罚分

			game_end_wsk.addEndScore(_win_score[i]); //当局积分
			
			game_end_wsk.addAllEndScore((int) _player_result.game_score[i]); // 总积分
		}

		
		// 重置数据，防止非第1小局的时候，游戏没开始就解散了
		Arrays.fill(_get_score, 0);
		Arrays.fill(_win_score, 0);
		Arrays.fill(_xi_qian_score, 0);
		Arrays.fill(_magic_card_score, 0);
		Arrays.fill(_punish_score, 0);

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk.addEndScoreZhua(_max_get_score[i]); // 最高抓分
					game_end_wsk.addYiYaoTimes(_first_finish_times[i]); // 一游次数
					game_end_wsk.addErYaoTimes(_second_finish_times[i]); // 二游次数
					game_end_wsk.addSanYaoTimes(_thirdly_finish_times[i]); // 三游次数
					game_end_wsk.addSiYaoTimes(_fourthly_finish_times[i]); // 四游次数
					game_end_wsk.addQiFenTimes(_xi_boom_times[i]); // 喜分次数

					game_end_wsk.addAllEndScore((int) _player_result.game_score[i]); // 总积分
				}

				game_end.setPlayerResult(process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addEndScoreZhua(_max_get_score[i]); // 最高抓分
				game_end_wsk.addYiYaoTimes(_first_finish_times[i]); // 一游次数
				game_end_wsk.addErYaoTimes(_second_finish_times[i]); // 二游次数
				game_end_wsk.addSanYaoTimes(_thirdly_finish_times[i]); // 三游次数
				game_end_wsk.addSiYaoTimes(_fourthly_finish_times[i]); // 四游次数
				game_end_wsk.addQiFenTimes(_xi_boom_times[i]); // 喜分次数

				game_end_wsk.addAllEndScore((int) _player_result.game_score[i]); // 总积分
			}

			game_end.setPlayerResult(process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}

		game_end_wsk.setReason(real_reason);

		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		Arrays.fill(_get_score, 0);

		return true;
	}
	
	public int cal_score_wsk_xtxz(int win_score[]) {
		if(this._is_yi_da_san){
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				win_score[i] = this.jiang_fa_socre[i] + this._xi_qian_score[i];
			}
		}else{
			//计算加倍
			int banker_team_times = 0;
			int other_team_times = 0;
			int banker_team_get_socre = 0;
			int other_team_get_socre = 0;
			int temp_get_socre[] = new int[getTablePlayerNumber()];
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				temp_get_socre[i] = this._get_score[i];
			}
			if(this.has_rule(GameConstants.GAME_RULE_WSK_CAN_RED_XTXZ)){
				banker_team_times = _is_biao_tai[GRR._banker_player] >_is_biao_tai[this._friend_seat[GRR._banker_player]] ? _is_biao_tai[GRR._banker_player] : _is_biao_tai[this._friend_seat[GRR._banker_player]] ;
				for(int i = 0;i < this.getTablePlayerNumber();i++){
					if(i == GRR._banker_player || i == this._friend_seat[GRR._banker_player]){
						continue;
					}
					other_team_times = other_team_times > _is_biao_tai[i] ? other_team_times : _is_biao_tai[i];
				}
			}

			for(int i = 0;i < this.getTablePlayerNumber();i++){
				if(i == GRR._banker_player || i == this._friend_seat[GRR._banker_player]){
					temp_get_socre[i] = temp_get_socre[i] + (banker_team_times + other_team_times)*temp_get_socre[i];
					banker_team_get_socre += temp_get_socre[i];
				}else{
					temp_get_socre[i] = temp_get_socre[i] + (banker_team_times + other_team_times)*temp_get_socre[i];
					other_team_get_socre += temp_get_socre[i];
				}
			}
			int cha_score = banker_team_get_socre - other_team_get_socre;
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				if(i == GRR._banker_player || i == this._friend_seat[GRR._banker_player]){
					win_score[i] = this.jiang_fa_socre[i]*2 + this._xi_qian_score[i] + cha_score;
				}
				else{
					win_score[i] = this.jiang_fa_socre[i]*2 + this._xi_qian_score[i] - cha_score;
				}
				
			}

		}

		return 0;
	}

	/**
	 * 游戏结束时，计算所有完抓分对应的赢分
	 * 
	 * @param win_score
	 * @return
	 */
	public int cal_score_wsk(int win_score[]) {
		int score = (int) game_cell;

		int shang_you_score = 0;
		int xia_you_score = 0;
		int er_you_score = 0;

		int times = 1;

		if (!_is_yi_da_san) {
			int you_num = 0;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_chuwan_shunxu[i] != GameConstants.INVALID_SEAT) {
					you_num++;
				}
			}

			if (you_num == 1) {
				// 一游=200
				times = 4;

				score *= times;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
						win_score[i] += score;
					} else {
						win_score[i] -= score;
					}
				}
			} else if (you_num == 2) {
				if (_friend_seat[_chuwan_shunxu[0]] == _chuwan_shunxu[1]) {
					// 打出一二游 一二游同队

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i != _chuwan_shunxu[0] && i != _friend_seat[_chuwan_shunxu[0]]) {
							xia_you_score += _get_score[i];
						}
					}

					if (xia_you_score == 0) {
						// 对方未拿分
						times = 4;
					} else {
						// 对方有拿分
						times = 2;
					}

					score *= times;

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
							win_score[i] += score;
						} else {
							win_score[i] -= score;
						}
					}
				} else {
					// 打出一二游 一二游不同队

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
							xia_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1]) {
							er_you_score += _get_score[i];
						}
					}

					int score_fence = 100;
					if (_chuwan_shunxu[1] == GRR._banker_player || _friend_seat[_chuwan_shunxu[1]] == GRR._banker_player)
						score_fence = 105;

					int score_fence_two = 105;
					if (_chuwan_shunxu[1] == GRR._banker_player || _friend_seat[_chuwan_shunxu[1]] == GRR._banker_player)
						score_fence_two = 100;

					if (er_you_score == 200) {
						// 二游=200
						times = 2;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (er_you_score >= score_fence && shang_you_score > 0) {
						// N<=二游<200 & 一游队伍>0
						times = 1;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (shang_you_score == 200) {
						// 一游队伍=200
						times = 2;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (er_you_score > 0 && er_you_score < score_fence && shang_you_score >= score_fence_two) {
						// 0<二游<N_1 & 上游队伍>=N_2
						times = 1;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					}
				}
			} else {
				// 打出一二游后继续打出三四游
				int score_fence = 100;
				if (_chuwan_shunxu[1] == GRR._banker_player || _friend_seat[_chuwan_shunxu[1]] == GRR._banker_player)
					score_fence = 105;

				if (_friend_seat[_chuwan_shunxu[0]] == _chuwan_shunxu[2]) {
					// 一三游同队

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
							xia_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1]) {
							er_you_score += _get_score[i];
						}
					}

					if (er_you_score == 0) {
						// 二游未拿分
						times = 2;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (er_you_score > 0 && er_you_score < score_fence) {
						// 0<二游<N
						times = 1;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (er_you_score >= score_fence && er_you_score < 200) {
						// N<=二游<200
						times = 1;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					}
				} else {
					// 一四游同队

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
							xia_you_score += _get_score[i];
						}
					}

					if (shang_you_score == 0) {
						// 一四游=0
						times = 2;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (shang_you_score > 0 && shang_you_score < score_fence) {
						// 0<一四游<N
						times = 1;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (shang_you_score >= score_fence && shang_you_score < 200) {
						// N<=一四游<200
						times = 1;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					} else if (shang_you_score == 200) {
						// 一四游=200
						times = 2;

						score *= times;

						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								win_score[i] += score;
							} else {
								win_score[i] -= score;
							}
						}
					}
				}
			}
		} else {
			times = 4;
			score *= times;

			if (_chuwan_shunxu[0] == GRR._banker_player) {
				win_score[GRR._banker_player] += 3 * score;
			} else {
				win_score[GRR._banker_player] -= 3 * score;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player) {
					continue;
				}

				if (_chuwan_shunxu[0] == GRR._banker_player) {
					win_score[i] -= score;
				} else {
					win_score[i] += score;
				}
			}
		}

		/*if (hasRulePiao) {
			List<Integer> lostIndex = new ArrayList<>();
			List<Integer> winIndex = new ArrayList<>();
			int pCount = getTablePlayerNumber();
			for (int i = 0; i < pCount; i++) {
				if (win_score[i] > 0)
					winIndex.add(i);
				else if (win_score[i] < 0)
					lostIndex.add(i);
			}
			int winCount = winIndex.size();
			int lostCount = lostIndex.size();
			if (_is_yi_da_san) {
				int winnerPiao = 0;
				int loserPiao = 0;
				if (winCount == 1) {
					winnerPiao = winCount > 0 ? _player_result.pao[winIndex.get(0)] : 0;
					loserPiao = lostCount >= 3
							? _player_result.pao[lostIndex.get(0)] + _player_result.pao[lostIndex.get(1)] + _player_result.pao[lostIndex.get(2)]
							: 0;
				} else if (winCount == 3) {
					winnerPiao = winCount >= 3
							? _player_result.pao[winIndex.get(0)] + _player_result.pao[winIndex.get(1)] + _player_result.pao[winIndex.get(2)]
							: 0;
					loserPiao = lostCount > 0 ? _player_result.pao[lostIndex.get(0)] : 0;
				}

				for (int i = 0; i < pCount; i++) {
					if (winIndex.contains(i)) {
						int tmpScore = _player_result.pao[i] * lostCount + loserPiao;
						_piao_score[i] += tmpScore;
					} else {
						int tmpScore = _player_result.pao[i] * winCount + winnerPiao;
						_piao_score[i] -= tmpScore;
					}
				}
			} else {
				int winnerPiao = winCount >= 2 ? _player_result.pao[winIndex.get(0)] + _player_result.pao[winIndex.get(1)] : 0;
				int loserPiao = lostCount >= 2 ? _player_result.pao[lostIndex.get(0)] + _player_result.pao[lostIndex.get(1)] : 0;

				for (int i = 0; i < pCount; i++) {
					if (winIndex.contains(i)) {
						int tmpScore = _player_result.pao[i] * lostCount + loserPiao;
						_piao_score[i] += tmpScore;
					} else {
						int tmpScore = _player_result.pao[i] * winCount + winnerPiao;
						_piao_score[i] -= tmpScore;
					}
				}
			}
		}*/

		return times;
	}

	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (_game_status == GameConstants.GS_MJ_PAO) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (seat_index == i) {
						continue;
					}
				}
			}
		}
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return on_handler_game_finish(seat_index, reason);
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {//
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (_cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				load_player_info_data(roomResponse2);
				send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		if (_cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			load_player_info_data(roomResponse2);
			send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			// send_play_data(seat_index);

			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);

		}
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;
		if (GameConstants.GS_MJ_FREE != _game_status) {
			return handler_player_ready(seat_index, false);
		}
		return true;
	}

	public void fixBugTemp(int seat_index) {
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
	}

	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		return true;
	}

	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		if (_handler_out_card_operate != null) {
			_handler = _handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

			_handler_out_card_operate.reset_status(get_seat_index, out_cards, card_count, b_out_card);
			_handler.exe(this);
		}
		return true;
	}

	public void refresh_ming_pai(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshMingPai_xtxz.Builder refresh_ming_pai = RefreshMingPai_xtxz.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_REFRESH_MING_PAI);

		refresh_ming_pai.setCardData(_jiao_pai_card);
		if (_out_card_ming_ji == GameConstants.INVALID_CARD) {
			refresh_ming_pai.setSeatIndex(GameConstants.INVALID_SEAT);
		} else {
			refresh_ming_pai.setSeatIndex(_friend_seat[GRR._banker_player]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_ming_pai));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_xtxz.Builder roomResponse) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndWsk_xtxz.Builder roomResponse) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
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
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, boolean is_deal) {
		for (int j = 0; j < count; j++) {
			cards_data[j] &= 0xFF;
		}

		for (int index = 0; index < getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_xtxz.Builder outcarddata = OutCardDataWsk_xtxz.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(_turn_out_card_count);
			for (int i = 0; i < _turn_out_card_count; i++) {
				outcarddata.addPrCardsData(_turn_real_card_data[i]);
				outcarddata.addPrCardsChangeData(_turn_out_card_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(_current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_turn_out_card_type);
			outcarddata.setIsFirstOut(false);

			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);
			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (is_deal) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			if (_current_player == index && _current_player != GameConstants.INVALID_SEAT) {
				int can_out_card_data[] = new int[get_hand_card_count_max()];
				int can_out_card_count = _logic.search_can_out_cards(GRR._cards_data[_current_player], GRR._card_count[_current_player],
						_turn_out_card_data, _turn_out_card_count, can_out_card_data);

				for (int i = 0; i < can_out_card_count; i++) {
					outcarddata.addUserCanOutData(can_out_card_data[i]);
				}
				outcarddata.setUserCanOutCount(can_out_card_count);
			}

			if (_out_card_ming_ji != GameConstants.INVALID_CARD && GRR._card_count[index] == 0) {
				outcarddata.setFriendSeatIndex(_friend_seat[index]);
			} else {
				outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int cardCount = GRR._card_count[i];
				if (!hasRuleDisplayCount)
					cardCount = 0;

				if (_out_card_ming_ji != GameConstants.INVALID_CARD && GRR._card_count[i] == 0) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (i == index) {
						int tmpI = _friend_seat[i];
						for (int j = 0; j < GRR._card_count[tmpI]; j++) {
							cards_card.addItem(GRR._cards_data[tmpI][j]);
						}
					}
					outcarddata.addHandCardsData(cards_card);

					outcarddata.addHandCardCount(cardCount);

					outcarddata.addWinOrder(_chuwan_shunxu[i]);
				} else {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (i == index) {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					}
					outcarddata.addHandCardsData(cards_card);

					outcarddata.addHandCardCount(cardCount);

					outcarddata.addWinOrder(_chuwan_shunxu[i]);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			send_response_to_player(index, roomResponse);

		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataWsk_xtxz.Builder outcarddata = OutCardDataWsk_xtxz.newBuilder();
		// Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);

		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
		}
		// 上一出牌数据
		outcarddata.setPrCardsCount(_turn_out_card_count);
		for (int i = 0; i < _turn_out_card_count; i++) {
			outcarddata.addPrCardsData(_turn_real_card_data[i]);
			outcarddata.addPrCardsChangeData(_turn_out_card_data[i]);
		}
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setCurPlayer(_current_player);
		outcarddata.setDisplayTime(10);
		outcarddata.setPrOutCardType(_turn_out_card_type);
		outcarddata.setIsFirstOut(false);

		if (_turn_out_card_count == 0) {
			outcarddata.setIsCurrentFirstOut(1);
		} else {
			outcarddata.setIsCurrentFirstOut(0);
		}
		if (_current_player != GameConstants.INVALID_SEAT) {
			if (GRR._card_count[_current_player] == 0) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			int can_out_card_data[] = new int[get_hand_card_count_max()];
			int can_out_card_count = _logic.search_can_out_cards(GRR._cards_data[_current_player], GRR._card_count[_current_player],
					_turn_out_card_data, _turn_out_card_count, can_out_card_data);

			for (int i = 0; i < can_out_card_count; i++) {
				outcarddata.addUserCanOutData(can_out_card_data[i]);
			}
			outcarddata.setUserCanOutCount(can_out_card_count);
		} else {
			outcarddata.setIsHaveNotCard(0);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			outcarddata.addHandCardsData(cards_card);
			outcarddata.addHandCardCount(GRR._card_count[i]);
			outcarddata.addWinOrder(_chuwan_shunxu[i]);
		}

		outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		return true;
	}

	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		load_player_info_data(roomResponse);
		load_room_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

		return true;
	}

	public PlayerResultResponse.Builder process_player_result(int result) {
		huan_dou(result);

		// 大赢家
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// if(is_mj_type(MJGameConstants.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
			// }
		}

		player_result.setRoomId(getRoom_id());
		player_result.setRoomOwnerAccountId(getRoom_owner_account_id());
		player_result.setRoomOwnerName(getRoom_owner_name());
		player_result.setCreateTime(getCreate_time());
		player_result.setRecordId(get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(getCreate_player().getAccount_icon());
		room_player.setIp(getCreate_player().getAccount_ip());
		room_player.setUserName(getCreate_player().getNick_name());
		room_player.setSeatIndex(getCreate_player().get_seat_index());
		room_player.setOnline(getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(getCreate_player().getAccount_ip_addr());
		room_player.setSex(getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;
		}

		if (is_sys()) {
			Random random = new Random(); //
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//
		}
	}

	public boolean exe_finish(int reason) {
		return true;
	}

	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;
	}

	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
	}

	public void runnable_remove_out_cards(int seat_index, int type) {
	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
	}

	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > getTablePlayerNumber() || !is_sys())
			return false;
		return istrustee[seat_index];
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}
		if (!is_sys())// 麻将普通场 不需要托管
			return false;

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return true;

	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// getTablePlayerNumber()
		for (int i = 0; i < scores.length; i++) {
			if (get_players()[i] == null) {
				continue;
			}
			score = (int) (scores[i] * beilv);
			// 逻辑处理
			get_players()[i].setMoney(get_players()[i].getMoney() + score);
			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(get_players()[i].getAccount_id(), score, false, buf.toString(),
					EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
		// playerNumber = 0;
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		load_player_info_data(roomResponse);
		send_response_to_player(seat_index, roomResponse);
		// send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		load_player_info_data(roomResponse);
		send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		_kick_schedule = null;

		if (!is_sys())
			return false;

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			if (getPlayerCount() != getTablePlayerNumber() || _player_ready == null) {
				return false;
			}

			// 检查是否所有人都未准备
			int not_ready_count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] != null && _player_ready[i] == 0) {// 未准备的玩家
					not_ready_count++;
				}
			}
			if (not_ready_count == getTablePlayerNumber())// 所有人都未准备 不用踢
				return false;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player rPlayer = get_players()[i];
				if (rPlayer != null && _player_ready[i] == 0) {// 未准备的玩家
					send_error_notify(i, 2, "您长时间未准备,被踢出房间!");

					RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
					quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
					send_response_to_player(i, quit_roomResponse);

					get_players()[i] = null;
					_player_ready[i] = 0;
					// _player_open_less[i] = 0;
					PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), rPlayer.getAccount_id());
				}
			}
			//
			if (getPlayerCount() == 0) {// 释放房间
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
			} else {
				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				load_player_info_data(refreshroomResponse);
				send_response_to_room(refreshroomResponse);
			}
			return true;
		}
		return false;
	}

	public void refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_xtxz.Builder refresh_user_getscore = RefreshScore_xtxz.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addXianQianScore(_xi_qian_score[i]);
		}
		if(_is_yi_da_san){
			refresh_user_getscore.setTableScore(-1);
		}else{
			refresh_user_getscore.setTableScore(_turn_have_score);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}
	}

	public boolean open_card_timer() {
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

	public void set_handler(AbstractHandler_XTXZ handler) {
		_handler = handler;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_XTXZ_OPERATE) {
			Opreate_RequestWsk_xtxz req = PBUtil.toObject(room_rq, Opreate_RequestWsk_xtxz.class);
			return handler_requst_opreate(seat_index, req.getOpreateType());
		}

		return true;
	}

	public void deal_liang_pai(int seat_index, int card_data) {
		if (_game_status != GameConstants.GS_TC_WSK_LIANG_PAI || seat_index != GRR._banker_player) {
			return;
		}
		// 不能叫大小王
		if (_logic.get_card_color(card_data) == 0x40) {
			send_error_notify(seat_index, 2, "请选择正确的牌型!");
			return;
		}
		int other_seat_index = GameConstants.INVALID_SEAT;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			for (int j = 0; j < GRR._card_count[i]; j++) {
				if (card_data == GRR._cards_data[i][j]) {
					other_seat_index = i;
					break;
				}
			}
			if (other_seat_index != GameConstants.INVALID_SEAT) {
				break;
			}
		}

		if (other_seat_index == GameConstants.INVALID_SEAT) {
			send_error_notify(seat_index, 2, "请选择正确的牌");
			return;
		}
		_jiao_pai_card = card_data;

		// 保存搭档信息
		_friend_seat[seat_index] = other_seat_index;
		_friend_seat[other_seat_index] = seat_index;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i != seat_index && i != other_seat_index) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (j != seat_index && j != other_seat_index && i != j) {
						_friend_seat[i] = j;
					}
				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_xtxz.Builder liang_pai_result = LiangPai_Result_xtxz.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(card_data);
		liang_pai_result.addSeatIndex(seat_index);
		liang_pai_result.addSeatIndex(other_seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		send_response_to_room(roomResponse);

		GRR.add_room_response(roomResponse);

		_game_status = GameConstants.GS_TC_WSK_PLAY;
		operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);

	}

	public void auto_liang_pai(int seat_index) {
		if (_game_status != GameConstants.GS_XTXZ_WSK_LIANG_PAI || seat_index != GRR._banker_player) {
			return;
		}

		// 选取所有的不重复的手牌
		int single_card_count = 0;
		int[] single_cards_data = new int[GameConstants.WSK_MAX_COUNT];
		int tmp_card = GRR._cards_data[GRR._banker_player][0];
		int repeated = 0;

		for (int j = 1; j < GRR._card_count[GRR._banker_player]; j++) {
			if (tmp_card == GRR._cards_data[GRR._banker_player][j]) {
				repeated++;
			} else {
				if (repeated == 0) {
					single_cards_data[single_card_count++] = tmp_card;
				}
				tmp_card = GRR._cards_data[GRR._banker_player][j];
				repeated = 0;
			}
		}

		if (repeated == 0) {
			single_cards_data[single_card_count++] = GRR._cards_data[GRR._banker_player][GRR._card_count[GRR._banker_player] - 1];
		}

		//if (has_rule(GameConstants.GAME_RULE_PK_TC_ONE_MAGIC) && single_card_count == 1 && single_cards_data[0] == Constants_XTXZ.FLOWER_CARD) {
		//	send_error_notify(seat_index, 2, "1花牌玩法时，庄手里只有花牌单");
		//	return;
		//}

		boolean exist_none_magic = false;
		int none_magic_count = 0;
		for (int i = 0; i < single_card_count; i++) {
			if (single_cards_data[i] != Constants_XTXZ.FLOWER_CARD /*&& single_cards_data[i] != Constants_XTXZ.CARD_BIG_MAGIC
					&& single_cards_data[i] != Constants_XTXZ.CARD_SMALL_MAGIC*/) {
				exist_none_magic = true;
				none_magic_count++;
			}
		}

		if (single_card_count == 0) {
			send_error_notify(seat_index, 2, "自动亮牌时，玩家手上没单牌！");
			return;
		}

		if (exist_none_magic) {
			single_card_count = none_magic_count;
		}

		int other_seat_index = GameConstants.INVALID_SEAT;

		int random_index = RandomUtil.getRandomNumber(single_card_count);
		int card_data = single_cards_data[random_index];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			for (int j = 0; j < GRR._card_count[i]; j++) {
				if (card_data == GRR._cards_data[i][j]) {
					other_seat_index = i;
					break;
				}
			}
			if (other_seat_index != GameConstants.INVALID_SEAT) {
				break;
			}
		}

		int loop_count = 0;
		while (other_seat_index == GameConstants.INVALID_SEAT && loop_count < 10) {
			loop_count++;

			random_index = RandomUtil.getRandomNumber(single_card_count);
			card_data = single_cards_data[random_index];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (card_data == GRR._cards_data[i][j]) {
						other_seat_index = i;
						break;
					}
				}
				if (other_seat_index != GameConstants.INVALID_SEAT) {
					break;
				}
			}
		}

		if (other_seat_index == GameConstants.INVALID_SEAT) {
			throw new RuntimeException("自动亮牌时，获取随机单牌数据出错！");
		}

		_jiao_pai_card = card_data;

		// 保存搭档信息
		_friend_seat[seat_index] = other_seat_index;
		_friend_seat[other_seat_index] = seat_index;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i != seat_index && i != other_seat_index) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (j != seat_index && j != other_seat_index && i != j) {
						_friend_seat[i] = j;
					}
				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_xtxz.Builder liang_pai_result = LiangPai_Result_xtxz.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(card_data);
		liang_pai_result.addSeatIndex(seat_index);
		liang_pai_result.addSeatIndex(other_seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		send_response_to_room(roomResponse);

		GRR.add_room_response(roomResponse);

		
		//表态
		if(this.has_rule(GameConstants.GAME_RULE_WSK_CAN_RED_XTXZ)){
			_game_status = GameConstants.GS_XTXZ_WSK_BIAOTAI;
			set_handler(_handler_biao);
			roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_BIAO_TAI);
			BiaoTaiResponse_xtxz.Builder callbanker_result = BiaoTaiResponse_xtxz.newBuilder();
			callbanker_result.setOpreateAction(-1);
			callbanker_result.setCallPlayer(-1);
			callbanker_result.setCurrentPlayer(GRR._banker_player);
			callbanker_result.setDisplayTime(10);
			callbanker_result.setRoomInfo(getRoomInfo());
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
			send_response_to_room(roomResponse);
		}else{
			set_handler(_handler_out_card_operate);
			_game_status = GameConstants.GS_XTXZ_WSK_PLAY;
			operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);
		}

	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_LIANG_PAI: {
			//deal_liang_pai(seat_index, card_data);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER_NO: {
			_handler_call_banker.handler_call_banker(this, seat_index, 0);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER: {
			_handler_call_banker.handler_call_banker(this, seat_index, 1);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG:{
			_handler_call_banker.handler_call_banker(this, seat_index, 2);
		}
		case GAME_OPREATE_TYPE_SORT_CARD: {
			deal_sort_card(seat_index,GameConstants.WSK_ST_ORDER);
			return true;
		}
		case GAME_OPREATE_TYPE_WSK_SORT:{
			deal_sort_card(seat_index,GameConstants.WSK_ST_COUNT);
			//deal_sort_card_custom(seat_index, list);
			return true;
		}
		case GAME_OPREATE_TYPE_PASS:{
			_handler_biao.handler_bai_tai(this,seat_index,0);
			return true;
		}
		case GAME_OPREATE_TYPE_XI_HONG:{
			_handler_biao.handler_bai_tai(this,seat_index,1);
			return true;
		}
		case GAME_OPREATE_TYPE_DA_HONG:{
			_handler_biao.handler_bai_tai(this,seat_index,2);
			return true;
		}

		//case GAME_OPREATE_TYPE_SORT_CARD_CUSTOM: {
		//	deal_sort_card_custom(seat_index, list);
		//	return true;
		//}
		//case GAME_OPREATE_TYPE_BIAO_TAI:{
		//	_handler_biao.handler_bai_tai(this,seat_index,card_data);
		//}
		}
		return true;
	}

	public void deal_sort_card(int seat_index,int type) {
		if (GRR == null)
			return;

		if (GRR._card_count[seat_index] == 0)
			return;

		_sort_type[seat_index] = type;
		/*if (_sort_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_sort_type[seat_index] = GameConstants.WSK_ST_ORDER;
		} else if (_sort_type[seat_index] == GameConstants.WSK_ST_ORDER) {
			_sort_type[seat_index] = GameConstants.WSK_ST_COUNT;
		}*/

		//_logic.sort_card_list_before_card_change(GRR._cards_data[seat_index], GRR._card_count[seat_index], _sort_type[seat_index]);
		_logic.sort_card_list_before_card_change_xtxz(GRR._cards_data[seat_index], GRR._card_count[seat_index], _sort_type[seat_index]);
		refresh_card(seat_index);
	}

	public void deal_sort_card_custom(int seat_index, List<Integer> list) {
		if (GRR == null)
			return;

		int count = list.size();

		if (count == 0) {
			boolean hasCustomer = false;
			for (int j = 0; j < GRR._card_count[seat_index]; j++) {
				if (GRR._cards_data[seat_index][j] > Constants_XTXZ.SPECIAL_CARD_TYPE) {
					hasCustomer = true;
					break;
				}
			}
			if (hasCustomer) {
				// 如果已经有手动理过的牌，根据上一次的理牌类型还原手牌显示
				player_sort_card[seat_index] = 0;

				for (int j = 0; j < GRR._card_count[seat_index]; j++) {
					if (GRR._cards_data[seat_index][j] > Constants_XTXZ.SPECIAL_CARD_TYPE) {
						GRR._cards_data[seat_index][j] = GRR._cards_data[seat_index][j] & 0xFF;
					}
				}

				_logic.sort_card_list_before_card_change(GRR._cards_data[seat_index], GRR._card_count[seat_index], _sort_type[seat_index]);

				refresh_card(seat_index);
			} else {
				send_error_notify(seat_index, 2, "您当前还没有手动理过牌，不需要还原！");
				return;
			}
		} else {
			int cards[] = new int[count];
			for (int i = 0; i < count; i++) {
				cards[i] = list.get(i);
			}

			int flag = (++player_sort_card[seat_index] & 0xF) << 8;

			boolean flag_lost = true;

			for (int i = 0; i < list.size(); i++) {
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
					if (GRR._cards_data[seat_index][j] == cards[i]) {
						if (GRR._cards_data[seat_index][j] > Constants_XTXZ.SPECIAL_CARD_TYPE) {
							GRR._cards_data[seat_index][j] = GRR._cards_data[seat_index][j] & 0xFF;
						} else {
							flag_lost = false;
							GRR._cards_data[seat_index][j] += flag;
						}

						break;
					}
				}
			}

			if (flag_lost) {
				int tmpSortCount = 0;
				int tmpFlag = 0;
				flag = 0;
				for (int i = GRR._card_count[seat_index] - 1; i >= 0; i--) {
					if (GRR._cards_data[seat_index][i] > Constants_XTXZ.SPECIAL_CARD_TYPE) {
						int newTmpFlag = GRR._cards_data[seat_index][i] - (GRR._cards_data[seat_index][i] & 0xFF);
						if (newTmpFlag != tmpFlag) {
							tmpFlag = newTmpFlag;
							flag = (++tmpSortCount & 0xF) << 8;
						}
						GRR._cards_data[seat_index][i] = (GRR._cards_data[seat_index][i] & 0xFF) + flag;
					}
				}
				player_sort_card[seat_index] = tmpSortCount;
			}

			_logic.sort_card_list_before_card_change(GRR._cards_data[seat_index], GRR._card_count[seat_index], _sort_type[seat_index]);

			refresh_card(seat_index);
		}
	}

	public void refresh_card(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_REFRESH_CARD);
		// 发送数据
		RefreshCardData_xtxz.Builder refresh_card = RefreshCardData_xtxz.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (to_player == i) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
			}
			refresh_card.addHandCardsData(cards_card);
			refresh_card.addHandCardCount(GRR._card_count[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据
		send_response_to_player(to_player, roomResponse);
	}

	public void refresh_pai_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_PAI_SCORE);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		PaiFenData.Builder pai_score_data = PaiFenData.newBuilder();

		int count = 0;
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

		for (int i = 0; i < _out_pai_score_count; i++) {
			if (_logic.get_card_logic_value(_out_pai_score_card[i]) == Constants_XTXZ.CARD_THIRTEEN) {
				cards.addItem(_out_pai_score_card[i]);
				count++;
			}
		}

		for (int i = 0; i < _out_pai_score_count; i++) {
			if (_logic.get_card_logic_value(_out_pai_score_card[i]) == Constants_XTXZ.CARD_TEN) {
				cards.addItem(_out_pai_score_card[i]);
				count++;
			}
		}

		for (int i = 0; i < _out_pai_score_count; i++) {
			if (_logic.get_card_logic_value(_out_pai_score_card[i]) == Constants_XTXZ.CARD_FIVE) {
				cards.addItem(_out_pai_score_card[i]);
				count++;
			}
		}

		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);

		pai_score_data.setYuScore(_all_pai_score - _out_pai_score);

		roomResponse.setCommResponse(PBUtil.toByteString(pai_score_data));

		if (to_player == GameConstants.INVALID_SEAT) {
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		if (is_sys()) {
			return true;
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 60;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
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

			// 取消之前的调度
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJgetTablePlayerNumber(); i++) {
			// Player pl = get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == getTablePlayerNumber()) {
				if (GRR == null) {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			send_response_to_room(roomResponse);

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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + get_players()[seat_index].getNick_name() + "]不同意解散");

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
				send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
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
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
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
			get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), player.getAccount_id());
			}

			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			if (player.getAccount_id() == getRoom_owner_account_id()) {
				getCreate_player().set_seat_index(GameConstants.INVALID_SEAT);
			}
			// 通知代理
			refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());

		}
			break;
		}

		return true;

	}

	/**
	 * 播放接风音效
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean operate_catch_action(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(1);
		roomResponse.addEffectsIndex(100001);

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

}
