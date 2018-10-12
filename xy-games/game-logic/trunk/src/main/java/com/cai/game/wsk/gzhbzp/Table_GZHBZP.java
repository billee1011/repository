package com.cai.game.wsk.gzhbzp;

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
import protobuf.clazz.gzhbzp.gzhbzpRsp.CallBankerOpreate_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.CallBankerResponse_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.GameStart_Wsk_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.LiangPai_Result_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.Opreate_RequestWsk_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.OutCardDataWsk_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.PukeGameEndWsk_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.RefreshCardData_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.RefreshMingPai_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.RefreshScore_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.TableResponse_gzhbzp;

public class Table_GZHBZP extends AbstractRoom {
	private static final long serialVersionUID = -2419887548488809773L;

	protected static Logger logger = Logger.getLogger(Table_GZHBZP.class);

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

	/**
	 * 牌桌上，找朋友玩法时，所有人的队友
	 */
	public int _friend_seat[] = new int[getTablePlayerNumber()];

	public ScheduledFuture<?> _trustee_schedule[];
	public ScheduledFuture<?> _game_scheduled;

	public GameLogic_GZHBZP _logic = new GameLogic_GZHBZP();

	protected long _request_release_time;

	protected ScheduledFuture<?> _release_scheduled;
	protected ScheduledFuture<?> _table_scheduled;

	public AbstractHandler_GZHBZP _handler;
	public HandlerOutCardOperate_GZHBZP _handler_out_card_operate;
	public HandlerCallBnaker_GZHBZP _handler_call_banker;
	public HandlerFinish_GZHBZP _handler_finish;
	public HandlerPiao_GZHBZP _handler_piao;

	/**
	 * 牌桌上，本轮打牌过程中，积累的总分值
	 */
	public int _turn_have_score = 0;
	/**
	 * 是否已经操作独牌。0还没操作，1已经点了操作(不管操作是‘独牌’还是‘不独’)
	 */
	public int _is_call_banker[] = new int[getTablePlayerNumber()];
	/**
	 * 是否是‘独牌’模式，也就是1打3，牌局中不计算抓分
	 */
	public boolean _is_yi_da_san = false;
	public boolean _is_yi_da_yi = false;
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
	protected static final int GAME_OPREATE_TYPE_CHECK_SCORE = 5; // 查分
	protected static final int GAME_OPREATE_TYPE_SORT_CARD_CUSTOM = 6; // 自由理牌

	public int _zhua_pai[] = new int[getTablePlayerNumber()]; // 抓牌
	public int _award_dou[] = new int[getTablePlayerNumber()]; // 豆
	public int _award_plane[] = new int[getTablePlayerNumber()]; // 滚筒
	public int _end_type[] = new int[getTablePlayerNumber()]; // 类型
	public int _round_score[] = new int[getTablePlayerNumber()]; // 牌局分
	public int _award_score[] = new int[getTablePlayerNumber()]; // 奖分输赢
	public int _gun_long_count[] = new int[getTablePlayerNumber()]; // 滚龙次数
	public int _all_single_count[] = new int[getTablePlayerNumber()];// 十三烂次数
	public int _zhua_pai_count = 0; // 抓牌
	public int _call_banker_opreate[] = new int[getTablePlayerNumber()]; // 叫庄操作
	public int _all_round_score[] = new int[this.getTablePlayerNumber()];// 牌局输赢

	public int[] player_sort_card = new int[getTablePlayerNumber()];

	public Table_GZHBZP() {
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

		_handler_out_card_operate = new HandlerOutCardOperate_GZHBZP();
		_handler_call_banker = new HandlerCallBnaker_GZHBZP();
		_handler_finish = new HandlerFinish_GZHBZP();
		_handler_piao = new HandlerPiao_GZHBZP();

		_zhua_pai = new int[getTablePlayerNumber()]; // 抓牌
		_award_dou = new int[getTablePlayerNumber()]; // 豆
		_award_plane = new int[getTablePlayerNumber()]; // 滚筒
		_end_type = new int[getTablePlayerNumber()]; // 类型
		_round_score = new int[getTablePlayerNumber()]; // 牌局分
		_award_score = new int[getTablePlayerNumber()]; // 奖分输赢
		_gun_long_count = new int[getTablePlayerNumber()]; // 滚龙次数
		_all_single_count = new int[getTablePlayerNumber()];// 十三烂次数
		_all_round_score = new int[this.getTablePlayerNumber()];// 牌局输赢
		_zhua_pai_count = 0;

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
		return GameConstants.BZP_GZH_MAX_COUNT;
	}

	protected void initBanker() {
		_cur_banker = 0;
		// _cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}

	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {
			initBanker();
			record_game_room();
		}
		_prev_player = GameConstants.INVALID_SEAT;
		_zhua_pai = new int[getTablePlayerNumber()]; // 抓牌
		_award_dou = new int[getTablePlayerNumber()]; // 豆
		_award_plane = new int[getTablePlayerNumber()]; // 滚筒
		_end_type = new int[getTablePlayerNumber()]; // 类型
		_round_score = new int[getTablePlayerNumber()]; // 牌局分
		_zhua_pai_count = 0;
		Arrays.fill(_call_banker_opreate, -1);
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

		_chuwan_shunxu = new int[getTablePlayerNumber()];
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);

		_friend_seat = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = i;
		}
		_is_call_banker = new int[getTablePlayerNumber()];

		_turn_have_score = 0;
		_is_yi_da_san = false;
		_is_yi_da_yi = false;

		_sort_type = new int[getTablePlayerNumber()];
		Arrays.fill(_sort_type, GameConstants.WSK_ST_ORDER);
		_jiao_pai_card = GameConstants.INVALID_CARD;

		_turn_real_card_data = new int[get_hand_card_count_max()];

		_run_player_id = 0;

		_out_pai_score_card = new int[24];
		_out_pai_score_count = 0;
		_out_pai_score = 0;

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
		if (commonGameRuleProtos != null)
			GRR._room_info.setNewRules(commonGameRuleProtos);

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

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		GRR._banker_player = -1;

		_game_status = GameConstants.GS_GZH_BZP_CALLBANKER;

		_repertory_card = new int[GameConstants.BZP_GZH_MAX_CARD_COUNT];
		shuffle(_repertory_card, GameConstants.CARD_DATA_BZP_GZH);

	 if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
		test_cards();

		getLocationTip();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		progress_banker_select();

		return on_game_start();
	}

	protected boolean on_game_start() {
		return on_game_start_real();
	}

	protected boolean on_game_start_real() {
		for (int play_index = 0; play_index < getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_GAME_START);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setRoomInfo(getRoomInfo());

			// 发送数据
			GameStart_Wsk_gzhbzp.Builder gamestart = GameStart_Wsk_gzhbzp.newBuilder();
			gamestart.setRoomInfo(getRoomInfo());
			load_player_info_data_game_start(gamestart);

			gamestart.setCurBanker(GameConstants.INVALID_SEAT);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				gamestart.addCardCount(GRR._card_count[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				@SuppressWarnings("unused")
				Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
						if (GRR._cards_data[i][j] == 0x37) {
							this._cur_banker = i;
							_current_player = i;
						}
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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_GAME_START);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_Wsk_gzhbzp.Builder gamestart = GameStart_Wsk_gzhbzp.newBuilder();
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
		if (this.get_specail_end() == true)
			return true;
		roomResponse.clear();

		set_handler(_handler_call_banker);

		roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_CALL_BANKER_RESULT);

		CallBankerResponse_gzhbzp.Builder callbanker_result = CallBankerResponse_gzhbzp.newBuilder();
		callbanker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
		callbanker_result.setOpreateAction(-1);
		callbanker_result.setCallPlayer(-1);

		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));

		send_response_to_room(roomResponse);
		//
		// refresh_pai_score(GameConstants.INVALID_SEAT);
		//
		// refresh_user_get_score(GameConstants.INVALID_SEAT);
		for (int i = 0; i < this.getTablePlayerNumber(); i++)
			update_button(i, true);
		refresh_user_get_score(GameConstants.INVALID_SEAT);
		return true;
	}

	public void update_button(int seat_index, boolean is_grr) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_BANKER_BUTTON);
		CallBankerOpreate_gzhbzp.Builder call_button = CallBankerOpreate_gzhbzp.newBuilder();
		call_button.setDisplayTime(10);
		call_button.setCurPlayer(this._current_player);
		if(this._game_status != GameConstants.GS_GZH_BZP_LIANG_PAI)
		{
			if (seat_index == this._current_player) {
				if (GRR._banker_player == -1) {
					call_button.addButton(0);
					call_button.addButton(1);
				} else {
					call_button.addButton(2);
					call_button.addButton(3);
				}
			}
		}
		else{
			call_button.addButton(4);//叫牌
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			call_button.addAllOpreateAction(this._call_banker_opreate[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(call_button));
		if (is_grr == true)
			GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

	}

	public boolean get_specail_end() {
		int max_index = this._current_player;
		int card_type = _logic.get_card_type(GRR._cards_data[this._current_player], GRR._card_count[this._current_player]);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._current_player == i)
				continue;
			int next_type = _logic.get_card_type(GRR._cards_data[i], GRR._card_count[i]);
			if (card_type < next_type) {
				card_type = next_type;
				max_index = i;
			}

		}
		if (card_type >= GameConstants.BZP_GZH_GUN_LONG) {
			if ((card_type & GameConstants.BZP_GZH_GUN_LONG) != 0)
				this._call_banker_opreate[max_index] = GameConstants.BZP_GZH_GUN_LONG_TYPE;
			else if ((card_type & GameConstants.BZP_GZH_ALL_SINGLE) != 0)
				this._call_banker_opreate[max_index] = GameConstants.BZP_GZH_ALL_SINGLE_TYPE;
			caculate_score();
			int delay = 3000;
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), max_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.MILLISECONDS);
			// 显示出牌
			// for(int i = 0; i< this.getTablePlayerNumber();i++){
			// this._current_player = -1;
			// int next_type =
			// _logic.get_card_type(GRR._cards_data[i],GRR._card_count[i]);
			// if(next_type == -1)
			// next_type = 0;
			// operate_out_card(i, GRR._card_count[i], GRR._cards_data[i],
			// next_type,
			// GameConstants.INVALID_SEAT, false);
			// }

			return true;
		}
		return false;
	}

	public void load_player_info_data_game_start(GameStart_Wsk_gzhbzp.Builder roomResponse) {
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

	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}
		int index = 0;
		int count = this.getTablePlayerNumber();
		for (int j = 0; j < this.get_hand_card_count_max(); j++) {
			for (int i = 0; i < count; i++) {
				GRR._cards_data[i][j] = repertory_card[index++];

			}

		}
		for (int i = 0; i < count; i++) {
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.sort_out_card_list(GRR._cards_data[i], GRR._card_count[i]);
		}
		if (count == 2) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[2][j] = repertory_card[2 * this.get_hand_card_count_max() + j];
			}
		}
		GRR._left_card_count = 0;
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		// int cards[] = new int[] { 0x0d, 0x1d, 0x2d, 0x3D, 0x0D, 0x1D, 0x2D,
		// 0x4E, 0x4F, 0x06, 0x16, 0x26, 0x36, 0x07, 0x07, 0x07 };
		// int cards[] = new int[] { 0x02, 0x12, 0x22, 0x32, 0x02, 0x12, 0x22,
		// 0x01, 0x4F, 0x07, 0x17, 0x27, 0x37, 0x07, 0x17, 0x27, 0x01, 0x09,
		// 0x0a, 0x1a, 0x2a, 0x3a, 0x0a, 0x1a, 0x2a, 0x01, 0x19, };

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

//		 BACK_DEBUG_CARDS_MODE = true;
//		 debug_my_cards = new int[] {0x01,0x11,0x21,0x31,
//				 0x02,0x12,0x22,0x3d,
//				 0x03,0x13,0x23,0x3c,
//				 0x04,0x14,0x24,0x3b,
//				 0x05,0x15,0x25,0x3a,
//				 0x06,0x16,0x26,0x39,
//				 0x07,0x17,0x27,0x38,
//				 0x08,0x18,0x28,0x37,
//				 0x09,0x19,0x29,0x36,
//				 0x0a,0x1a,0x2a,0x35,
//				 0x0b,0x1b,0x2b,0x34,
//				 0x0c,0x1c,0x2c,0x33,
//				 0x0d,0x1d,0x2d,0x32,
//		
//		
//		
//		 };

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > GameConstants.BZP_GZH_MAX_COUNT) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				}
			}

		}
		BACK_DEBUG_CARDS_MODE = true;

	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_cur_banker = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == this.getTablePlayerNumber())
				break;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_index[i][j] = 0;
			}

		}
		this._repertory_card = cards;
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[k++];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	/**
	 * 游戏正常结束的时候，计算喜钱分
	 */
	public void process_xi_qian() {

	}

	/**
	 * 牌局正常结算之后，计算花牌分
	 */
	public void process_flower_score() {

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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_gzhbzp.Builder game_end_gzhbzp = PukeGameEndWsk_gzhbzp.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_gzhbzp.setRoomInfo(getRoomInfo());

		// 计算分数

		load_player_info_data_game_end(game_end_gzhbzp);

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);

		if (GRR != null) {
			game_end_gzhbzp.setBankerPlayer(GRR._banker_player);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_gzhbzp.addCardCount(GRR._card_count[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_gzhbzp.addCardsData(i, cards_card);
				game_end_gzhbzp.addRoundScore(this._round_score[i]);
				game_end_gzhbzp.addZhuaPai(this._zhua_pai[i]);
				game_end_gzhbzp.addAwardDou(this._award_dou[i]);
				game_end_gzhbzp.addAwardPlane(this._award_plane[i]);
				game_end_gzhbzp.addEndType(this._call_banker_opreate[i]);
				game_end_gzhbzp.addEndScore((int) this.GRR._game_score[i]);
				game_end_gzhbzp.addUserCardtype(-1);
				game_end.addGameScore(GRR._game_score[i]);
				int order = 0;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (this._chuwan_shunxu[j] == i)
						break;
					order++;
				}
				game_end_gzhbzp.addWinOrder(order);

				if (this._is_yi_da_yi == true || this._is_yi_da_san == true)
					game_end_gzhbzp.addTeamNumber(0);
				else {
					if (this.GRR._banker_player > 0 && (i == this.GRR._banker_player || i == this._friend_seat[GRR._banker_player]))
						game_end_gzhbzp.addTeamNumber(1);
					else
						game_end_gzhbzp.addTeamNumber(2);
				}

			}
			caculate_end();
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);

		}

		int end_score_type = 0;

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

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;

				for (int i = 0; i < getTablePlayerNumber(); i++) {

					game_end_gzhbzp.addAllEndScore((int) _player_result.game_score[i]); // 总积分
					game_end_gzhbzp.addAwardScore(this._award_score[i]);
					game_end_gzhbzp.addGunLongCount(this._gun_long_count[i]);
					game_end_gzhbzp.addAllSingleCount(this._all_single_count[i]);
					game_end_gzhbzp.addAllRoundScore(this._all_round_score[i]);
				}

				game_end.setPlayerResult(process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;

			for (int i = 0; i < getTablePlayerNumber(); i++) {

				game_end_gzhbzp.addAllEndScore((int) _player_result.game_score[i]); // 总积分
				game_end_gzhbzp.addAwardScore(this._award_score[i]);
				game_end_gzhbzp.addGunLongCount(this._gun_long_count[i]);
				game_end_gzhbzp.addAllSingleCount(this._all_single_count[i]);
				game_end_gzhbzp.addAllRoundScore(this._all_round_score[i]);
			}

			game_end.setPlayerResult(process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}

		game_end_gzhbzp.setReason(real_reason);

		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_gzhbzp));
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

		return true;
	}

	public void caculate_end() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += GRR._game_score[i];
			if (this._call_banker_opreate[i] == GameConstants.BZP_GZH_GUN_LONG_TYPE)
				this._gun_long_count[i]++;
			if (this._call_banker_opreate[i] == GameConstants.BZP_GZH_ALL_SINGLE_TYPE)
				this._all_single_count[i]++;
			this._award_score[i] += this._award_dou[i] + this._award_plane[i];
			this._all_round_score[i] += this._round_score[i];
		}
	}

	/**
	 * 游戏结束时，计算所有完抓分对应的赢分
	 * 
	 * @param win_score
	 * @return
	 */
	public int cal_score_wsk(int win_score[]) {
		int times = 0;
		return times;
	}

	@Override
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
		if (_player_ready[seat_index] == 1)
			return false;
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

	@Override
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
		RefreshMingPai_gzhbzp.Builder refresh_ming_pai = RefreshMingPai_gzhbzp.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_REFRESH_MING_PAI);

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

	public void load_player_info_data_reconnect(TableResponse_gzhbzp.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndWsk_gzhbzp.Builder roomResponse) {
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
			OutCardDataWsk_gzhbzp.Builder outcarddata = OutCardDataWsk_gzhbzp.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_OUT_CARD);// 201
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
			outcarddata.setCardType(_logic.switch_s_to_c(type));
			outcarddata.setCurPlayer(_current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_logic.switch_s_to_c(_turn_out_card_type));
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
					if (this._is_yi_da_yi == true || this._is_yi_da_yi == true)
						outcarddata.addWinOrder(-1);
					else	
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
					if (this._is_yi_da_yi == true || this._is_yi_da_yi == true)
						outcarddata.addWinOrder(-1);
					else	
						outcarddata.addWinOrder(_chuwan_shunxu[i]);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			send_response_to_player(index, roomResponse);

		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataWsk_gzhbzp.Builder outcarddata = OutCardDataWsk_gzhbzp.newBuilder();
		// Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_OUT_CARD);// 201
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
		outcarddata.setCardType(_logic.switch_s_to_c(type));
		outcarddata.setCurPlayer(_current_player);
		outcarddata.setDisplayTime(10);
		outcarddata.setPrOutCardType(_logic.switch_s_to_c(_turn_out_card_type));
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
			if (this._is_yi_da_yi == true || this._is_yi_da_yi == true)
				outcarddata.addWinOrder(-1);
			else	
				outcarddata.addWinOrder(_chuwan_shunxu[i]);
		}

		outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		return true;
	}

	@Override
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

	@Override
	public boolean exe_finish(int reason) {
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
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;
	}

	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
	}

	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
	}

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
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
		RefreshScore_gzhbzp.Builder refresh_user_getscore = RefreshScore_gzhbzp.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(this._zhua_pai[i]);
		}
		refresh_user_getscore.setTableScore(this._zhua_pai_count);

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}
	}

	@Override
	public boolean open_card_timer() {
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		return false;
	}

	@Override
	public boolean ready_timer() {
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		return false;
	}

	public void set_handler(AbstractHandler_GZHBZP handler) {
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
		if (type == MsgConstants.REQUST_GZH_BZP_OPERATE) {
			Opreate_RequestWsk_gzhbzp req = PBUtil.toObject(room_rq, Opreate_RequestWsk_gzhbzp.class);
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getCallOpreate(), req.getCardData(), req.getSortCardList());
		}

		return true;
	}

	public int get_award(int card_type, int card_data) {
		if ((card_type & GameConstants.BZP_GZH_BOMB) != 0) {
			if (this.getRuleValue(GameConstants.GAME_RULE_BZP_GZH_DOU_ZERO) == 1)
				return 0;
			if (this.getRuleValue(GameConstants.GAME_RULE_BZP_GZH_DOU_ONE) == 1) {
				if (_logic.get_card_value(card_data) == 2) {
					return 2;
				} else
					return 1;
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_BZP_GZH_DOU_TWO) == 1) {
				if (_logic.get_card_value(card_data) == 2) {
					return 4;
				} else
					return 2;
			}
		}
		return 0;
	}

	public int get_gun_tong(int card_type) {
		if ((card_type & GameConstants.BZP_GZH_PLANE) != 0)
			return 3;
		return 0;
	}

	public void deal_liang_pai(int seat_index) {
		if (_game_status != GameConstants.GS_TC_WSK_LIANG_PAI || seat_index != GRR._banker_player) {
			return;
		}
		int other_seat_index = this.GRR._banker_player;
		while (other_seat_index == this.GRR._banker_player)
			other_seat_index = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber();

		int index = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % GameConstants.BZP_GZH_MAX_COUNT;

		if (other_seat_index == GameConstants.INVALID_SEAT) {
			send_error_notify(seat_index, 2, "请选择正确的牌");
			return;
		}
		_jiao_pai_card = GRR._cards_data[other_seat_index][index];

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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_gzhbzp.Builder liang_pai_result = LiangPai_Result_gzhbzp.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(_jiao_pai_card);
		liang_pai_result.addSeatIndex(seat_index);
		liang_pai_result.addSeatIndex(other_seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		send_response_to_room(roomResponse);

		GRR.add_room_response(roomResponse);

		_game_status = GameConstants.GS_TC_WSK_PLAY;
		set_handler(_handler_out_card_operate);
		operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);

	}
	public void deal_liang_pai(int seat_index , int card) {
		if (_game_status != GameConstants.GS_TC_WSK_LIANG_PAI || seat_index != GRR._banker_player) {
			return;
		}
		if(!_logic.is_valid_card(card)){
			log_info("牌值不对 ： "+card);
			return ;
		}
		int other_seat_index = GameConstants.INVALID_SEAT;
		for(int i = 0; i< this.getTablePlayerNumber();i++)
		{
			if(i == GRR._banker_player)
				continue;
			for(int j = 0; j< GRR._card_count[i];j++){
				if(GRR._cards_data[i][j] == card)
				{
					other_seat_index = i;
					break;
				}
			}
			if(other_seat_index != -1)
			{
				break;
			}
		}

		if (other_seat_index == GameConstants.INVALID_SEAT) {
			send_error_notify(seat_index, 2, "请选择正确的牌");
			return;
		}
		_jiao_pai_card = card;

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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_gzhbzp.Builder liang_pai_result = LiangPai_Result_gzhbzp.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(_jiao_pai_card);
		liang_pai_result.addSeatIndex(seat_index);
		liang_pai_result.addSeatIndex(other_seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		send_response_to_room(roomResponse);

		GRR.add_room_response(roomResponse);

		_game_status = GameConstants.GS_TC_WSK_PLAY;
		set_handler(_handler_out_card_operate);
		operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);

	}

	public void auto_liang_pai(int seat_index) {
		if (_game_status != GameConstants.GS_TC_WSK_LIANG_PAI || seat_index != GRR._banker_player) {
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

		if (has_rule(GameConstants.GAME_RULE_PK_TC_ONE_MAGIC) && single_card_count == 1 && single_cards_data[0] == Constants_GZHBZP.FLOWER_CARD) {
			send_error_notify(seat_index, 2, "1花牌玩法时，庄手里只有花牌单");
			return;
		}

		boolean exist_none_magic = false;
		int none_magic_count = 0;
		for (int i = 0; i < single_card_count; i++) {
			if (single_cards_data[i] != Constants_GZHBZP.FLOWER_CARD && single_cards_data[i] != Constants_GZHBZP.CARD_BIG_MAGIC
					&& single_cards_data[i] != Constants_GZHBZP.CARD_SMALL_MAGIC) {
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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_gzhbzp.Builder liang_pai_result = LiangPai_Result_gzhbzp.newBuilder();
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

	public void caculate_score() {
		int max_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._call_banker_opreate[i] == GameConstants.BZP_GZH_GUN_LONG_TYPE) {
				this._round_score[i] = 36;
				GRR._game_score[i] += 36;
				max_score = (int) GRR._game_score[i];
				break;
			} else if (this._call_banker_opreate[i] == GameConstants.BZP_GZH_ALL_SINGLE_TYPE) {

				GRR._game_score[i] += 18;
				max_score = (int) GRR._game_score[i];
				this._round_score[i] = 18;
				break;
			}
		}
		if (max_score > 0) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (GRR._game_score[i] > 0)
					continue;
				this._round_score[i] = -max_score / (this.getTablePlayerNumber() - 1);
				GRR._game_score[i] = this._round_score[i];
			}
			return;
		}
		if (this._is_yi_da_san == true) {
			if (this._chuwan_shunxu[0] == GRR._banker_player) {
				this._round_score[GRR._banker_player] = 9;
				max_score = 9;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._round_score[i] != 0)
						continue;
					this._round_score[i] += -max_score / (this.getTablePlayerNumber() - 1);
				}
			} else {
				this._round_score[GRR._banker_player] = -9;
				max_score = -9;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._round_score[i] != 0)
						continue;
					this._round_score[i] += -max_score / (this.getTablePlayerNumber() - 1);
				}

			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this._award_dou[i] = 0;
				this._award_plane[i] = 0;
				this._zhua_pai[i] = 0;
				GRR._game_score[i] = this._round_score[i];
			}
			return;
		} else if (this._is_yi_da_yi == true) {
			this._round_score[this._chuwan_shunxu[0]] = 16;
			this._round_score[this._chuwan_shunxu[1]] = -18;
			this._round_score[this._chuwan_shunxu[2]] = 1;
			this._round_score[this._chuwan_shunxu[3]] = 1;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this._award_dou[i] = 0;
				this._award_plane[i] = 0;
				this._zhua_pai[i] = 0;
				GRR._game_score[i] = this._round_score[i];
			}
			return;
		} else {
			if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[1]) {
				this._round_score[this._chuwan_shunxu[0]] = 2;
				this._round_score[this._chuwan_shunxu[1]] = 2;
				this._round_score[this._chuwan_shunxu[2]] = -2;
				this._round_score[this._chuwan_shunxu[3]] = -2;
			} else if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[2]) {
				if (this._zhua_pai[this._chuwan_shunxu[0]] + this._zhua_pai[this._chuwan_shunxu[2]]
						+ this._zhua_pai[this._chuwan_shunxu[3]] < this._zhua_pai[this._chuwan_shunxu[1]]) {
					this._round_score[this._chuwan_shunxu[0]] = -1;
					this._round_score[this._chuwan_shunxu[2]] = -1;
					this._round_score[this._chuwan_shunxu[1]] = 1;
					this._round_score[this._chuwan_shunxu[3]] = 1;
				} else {
					this._round_score[this._chuwan_shunxu[0]] = 1;
					this._round_score[this._chuwan_shunxu[2]] = 1;
					this._round_score[this._chuwan_shunxu[1]] = -1;
					this._round_score[this._chuwan_shunxu[3]] = -1;
				}

			} else if (this._friend_seat[this._chuwan_shunxu[0]] == this._chuwan_shunxu[3]) {
				if (this._zhua_pai[this._chuwan_shunxu[0]] + this._zhua_pai[this._chuwan_shunxu[3]] < this._zhua_pai[this._chuwan_shunxu[1]]
						+ this._zhua_pai[this._chuwan_shunxu[2]]) {

					this._round_score[this._chuwan_shunxu[0]] = -1;
					this._round_score[this._chuwan_shunxu[3]] = -1;
					this._round_score[this._chuwan_shunxu[1]] = 1;
					this._round_score[this._chuwan_shunxu[2]] = 1;

				} else {
					this._round_score[this._chuwan_shunxu[0]] = 1;
					this._round_score[this._chuwan_shunxu[3]] = 1;
					this._round_score[this._chuwan_shunxu[1]] = -1;
					this._round_score[this._chuwan_shunxu[2]] = -1;
				}

			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] = this._round_score[i] + this._award_dou[i] + this._award_plane[i];
			}
		}
		return;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int call_opreate, int card_data, List<Integer> list) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_CALL_BANKER: {
			_handler_call_banker.handler_call_banker(this, seat_index, call_opreate);
			return true;
		}
		case GAME_OPREATE_TYPE_SORT_CARD_CUSTOM: {
			deal_sort_card_custom(seat_index, list);
			return true;
		}
		case GAME_OPREATE_TYPE_LIANG_PAI:{
			this.deal_liang_pai(seat_index, card_data);
		}
		}
		return true;
	}

	public void deal_sort_card(int seat_index) {
		if (GRR == null)
			return;

		if (GRR._card_count[seat_index] == 0)
			return;

		if (_sort_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_sort_type[seat_index] = GameConstants.WSK_ST_ORDER;
		} else if (_sort_type[seat_index] == GameConstants.WSK_ST_ORDER) {
			_sort_type[seat_index] = GameConstants.WSK_ST_COUNT;
		}

		_logic.sort_card_list_before_card_change(GRR._cards_data[seat_index], GRR._card_count[seat_index], _sort_type[seat_index]);

		refresh_card(seat_index);
	}

	public void deal_sort_card_custom(int seat_index, List<Integer> list) {
		if (GRR == null)
			return;

		int count = list.size();

		if (count == 0) {
			boolean hasCustomer = false;
			for (int j = 0; j < GRR._card_count[seat_index]; j++) {
				if (GRR._cards_data[seat_index][j] > Constants_GZHBZP.SPECIAL_CARD_TYPE) {
					hasCustomer = true;
					break;
				}
			}
			if (hasCustomer) {
				// 如果已经有手动理过的牌，根据上一次的理牌类型还原手牌显示
				player_sort_card[seat_index] = 0;

				for (int j = 0; j < GRR._card_count[seat_index]; j++) {
					if (GRR._cards_data[seat_index][j] > Constants_GZHBZP.SPECIAL_CARD_TYPE) {
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
						if (GRR._cards_data[seat_index][j] > Constants_GZHBZP.SPECIAL_CARD_TYPE) {
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
					if (GRR._cards_data[seat_index][i] > Constants_GZHBZP.SPECIAL_CARD_TYPE) {
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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_REFRESH_CARD);
		// 发送数据
		RefreshCardData_gzhbzp.Builder refresh_card = RefreshCardData_gzhbzp.newBuilder();
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
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_PAI_SCORE);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		PaiFenData.Builder pai_score_data = PaiFenData.newBuilder();

		int count = 0;
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

		for (int i = 0; i < _out_pai_score_count; i++) {
			if (_logic.get_card_logic_value(_out_pai_score_card[i]) == Constants_GZHBZP.CARD_THIRTEEN) {
				cards.addItem(_out_pai_score_card[i]);
				count++;
			}
		}

		for (int i = 0; i < _out_pai_score_count; i++) {
			if (_logic.get_card_logic_value(_out_pai_score_card[i]) == Constants_GZHBZP.CARD_TEN) {
				cards.addItem(_out_pai_score_card[i]);
				count++;
			}
		}

		for (int i = 0; i < _out_pai_score_count; i++) {
			if (_logic.get_card_logic_value(_out_pai_score_card[i]) == Constants_GZHBZP.CARD_FIVE) {
				cards.addItem(_out_pai_score_card[i]);
				count++;
			}
		}

		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);

		pai_score_data.setYuScore(_out_pai_score);

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
