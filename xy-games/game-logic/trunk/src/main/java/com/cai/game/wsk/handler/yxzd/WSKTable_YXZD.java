package com.cai.game.wsk.handler.yxzd;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKGameLogic_YXZD;
import com.cai.game.wsk.WSKMsgConstants;
import com.cai.game.wsk.WSKType;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RobotService;
import com.cai.service.RobotService.RobotRandom;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sxth.SxthRsp.RefreshCardData_Sxth;
import protobuf.clazz.sxth.SxthRsp.effect_type_sxth;
import protobuf.clazz.yxzd.yxzdkRsp.Contorl_Toward_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.FanPai_Begin_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.FanPai_Result_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.GameStart_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.Opreate_RequestWsk_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.OutCardDataWsk_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.PukeGameEndWsk_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.RefreshScore_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.SendCard_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.TableResponse_yxzd;

public class WSKTable_YXZD extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间
	protected static final int ID_TIMER_GAME_START = 2;// 开始时间
	protected static final int ID_TIMER_OUT_CARD = 3;// 开始时间

	public int _call_banker[];

	public int _turn_have_score; // 当局得分
	public int _score_type[];
	public int _jiao_pai_card;
	public int _out_card_ming_ji;
	public int _end_score_info[][];
	public int _get_score_info[][];
	public int _win_order_info[][];
	public int _fan_pai_card[];

	public int _is_contorled[];

	public int _di_pai[];
	public int _di_pai_count;
	protected static final int GAME_OPREATE_TYPE_OUT_CARD = 1;// 出牌

	protected static final int GAME_OPREATE_TYPE_FAN_PAI = 2;// 翻牌
	protected static final int GAME_OPREATE_TYPE_YI_TUO_ER = 3;// 一拖二
	protected static final int GAME_OPREATE_TYPE_CONTROL = 4;// 接管
	protected static final int GAME_OPREATE_TYPE_CONTROL_CANCEL = 5;// 取消接管
	private boolean is_touxiang;
	public WSKGameLogic_YXZD _logic;

	public WSKTable_YXZD() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_YXZD();
		_get_score = new int[getTablePlayerNumber()];
		_xi_qian_total_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_call_banker = new int[getTablePlayerNumber()];
		_tou_xiang_times = new int[getTablePlayerNumber()];
		_max_end_score = new int[getTablePlayerNumber()];
		_end_score_info = new int[this._game_round][getTablePlayerNumber()];
		_get_score_info = new int[this._game_round][getTablePlayerNumber()];
		_win_order_info = new int[this._game_round][getTablePlayerNumber()];
		_turn_real_card_data = new int[get_hand_card_count_max()];
		_cur_out_car_type = new int[getTablePlayerNumber()];
		_out_card_times = new int[getTablePlayerNumber()];
		_init_account_id = new long[getTablePlayerNumber()];
		_is_contorled = new int[getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = new int[getTablePlayerNumber()];
		_seat_team = new int[getTablePlayerNumber()];
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_YXZD();
		_handler_call_banker = new WSKHandlerCallBnakerYXZD();
		_di_pai_count = 20;
		_di_pai = new int[_di_pai_count];
		_fan_pai_card = new int[4];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_max_end_score[i] = 0;

			_score_type[i] = GameConstants.WSK_ST_CUSTOM;
		}
		for (int i = 0; i < this._game_round; i++) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				_end_score_info[i][j] = 0;
				_get_score_info[i][j] = 0;
				_win_order_info[i][j] = -1;
			}
		}
		_fan_pai_card[0] = 0x4E;
		_fan_pai_card[1] = 0x4E;
		_fan_pai_card[2] = 0x4E;
		_fan_pai_card[3] = 0x4F;
		this._logic.random_card_data(_fan_pai_card, _fan_pai_card);
	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_yxzd.Builder refresh_user_getscore = RefreshScore_yxzd.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addGetScore(_get_score[i]);
			refresh_user_getscore.addGameScore((int) this._player_result.game_score[i]);
		}
		refresh_user_getscore.setTableScore(_turn_have_score);

		// refresh_user_getscore.setTableScore(value);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();
		if (this._cur_round == 1) {
			_init_players = new Player[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_init_account_id[i] = this.get_players()[i].getAccount_id();
				_init_players[i] = this.get_players()[i];
			}
		}

		this._current_player = _cur_banker;
		this._turn_out_card_count = 0;

		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_is_tou_xiang_agree[i] = -1;
			_is_tou_xiang[i] = -1;
			_xi_qian_score[i] = 0;
			_xi_qian_times[i] = 0;
			_call_banker[i] = -1;
			_cur_out_car_type[i] = GameConstants.SXTH_CT_ERROR;
			_out_card_times[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_CUSTOM;
			this._seat_team[i] = i % 2;

			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}

		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_out_card_ming_ji = GameConstants.INVALID_CARD;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);

		_repertory_card = new int[WSKConstants.CARD_COUNT_YXZD];
		shuffle(_repertory_card, WSKConstants.CARD_DATA_YXZD);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			// test_cards();

			_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE)
					% GameConstants.CARD_COUNT_WSK];
		getLocationTip();

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}
		// 庄家选择
		this.progress_banker_select();

		on_game_start();

		return true;
	}

	@Override
	public int get_hand_card_count_max() {
		return (WSKConstants.CARD_COUNT_YXZD - _di_pai_count) / 4;
	}

	/// 洗牌
	@Override
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

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}
		for (int i = 0; i < _di_pai_count; i++) {
			_di_pai[i] = repertory_card[count * this.get_hand_card_count_max() + i];
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 27, 50, 2, 5, 28, 11, 45, 33, 39, 33, 49, 37, 53, 1, 23, 42, 38, 22, 45, 54, 60, 20,
				22, 79, 19, 3, 55, 10, 38, 12, 59, 24, 12, 24, 1, 20, 49, 53, 55, 27, 8, 21, 78, 44, 52, 79, 13, 18, 33,
				41, 60, 20, 42, 34, 35, 37, 5, 28, 26, 12, 2, 50, 79, 41, 42, 51, 4, 22, 17, 56, 49, 3, 43, 23, 40, 4,
				59, 58, 78, 8, 36, 10, 1, 11, 9, 53, 45, 26, 8, 56, 11, 37, 38, 56, 13, 6, 3, 40, 51, 60, 28, 27, 39,
				58, 58, 6, 36, 78, 57, 21, 23, 35, 34, 57, 59, 29, 43, 35, 9, 13, 36, 50, 17, 44, 25, 2, 39, 61, 61, 21,
				25, 7, 54, 19, 24, 52, 40, 17, 10, 61, 55, 41, 54, 51, 19, 79, 29, 29, 5, 18, 44, 57, 9, 25, 43, 6, 7,
				78, 4, 18, 7, 26, 52, 34 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			int card_count = get_hand_card_count_max();
			for (int j = 0; j < card_count; j++) {
				GRR._cards_data[i][j] = cards[index++];

			}
			GRR._card_count[i] = card_count;
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > get_hand_card_count_max()) {
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

	@Override
	protected boolean on_game_start() {
		// int FlashTime = 4000;
		// int standTime = 1000;
		is_touxiang = false;

		_game_status = WSKConstants.GS_YXZD_FAN_PAI;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_yxzd.Builder gamestart = GameStart_yxzd.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		gamestart.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		send_card();
		this.set_handler(this._handler_call_banker);
		this._handler_call_banker.exe(this);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_yi_tuo_er_status(i);
		}
		this.Refresh_user_get_score(GameConstants.INVALID_SEAT);

		return true;
	}

	public void send_fan_pai_result() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_FAN_RESULT);
		roomResponse.setGameStatus(_game_status);
		FanPai_Result_yxzd.Builder fan_pai_result = FanPai_Result_yxzd.newBuilder();
		fan_pai_result.setChair(this._current_player);
		fan_pai_result.setCardData(0x4F);
		roomResponse.setCommResponse(PBUtil.toByteString(fan_pai_result));
		send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}

	public void send_fan_pai_begin(int to_player) {
		if (to_player == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_FAN_BEGIN);
			roomResponse.setGameStatus(this._game_status);
			FanPai_Begin_yxzd.Builder fan_pai_begin = FanPai_Begin_yxzd.newBuilder();
			for (int i = 0; i < 4; i++) {
				boolean is_fan = false;
				for (int j = 0; j < 4; j++) {
					if (_call_banker[j] == i) {
						is_fan = true;
						break;
					}
				}
				if (is_fan) {
					fan_pai_begin.addCardData(this._fan_pai_card[i]);
				} else {
					fan_pai_begin.addCardData(GameConstants.BLACK_CARD);
				}
			}
			fan_pai_begin.setCurIndex(this._current_player);
			fan_pai_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(fan_pai_begin));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_FAN_BEGIN);
			roomResponse.setGameStatus(this._game_status);
			FanPai_Begin_yxzd.Builder fan_pai_begin = FanPai_Begin_yxzd.newBuilder();
			for (int i = 0; i < 4; i++) {
				boolean is_fan = false;
				for (int j = 0; j < 4; j++) {
					if (_call_banker[j] == i) {
						is_fan = true;
						break;
					}
				}
				if (is_fan) {
					fan_pai_begin.addCardData(this._fan_pai_card[i]);
				} else {
					fan_pai_begin.addCardData(GameConstants.BLACK_CARD);
				}
			}
			fan_pai_begin.setCurIndex(this._current_player);
			fan_pai_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(fan_pai_begin));
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 发送特效
	public void send_effect_type(int seat_index, int type, int data, int to_player) {
		// 1：分数动画 2：四小龙王 3：六六大顺 4：八仙过海 5：十二罗汉
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_EFFECT);
		effect_type_sxth.Builder effect = effect_type_sxth.newBuilder();
		effect.setSeatIndex(seat_index);
		effect.setType(type);
		effect.setData(data);
		roomResponse.setCommResponse(PBUtil.toByteString(effect));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	public void out_card_begin() {
		_game_status = GameConstants.GS_SXTH_PLAY;
		GRR._banker_player = _cur_banker;
		this._current_player = GRR._banker_player;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.SXTH_CT_ERROR,
				GameConstants.INVALID_SEAT, false);

	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD: {
			out_card_begin();
			return;
		}
		case ID_TIMER_OUT_CARD: {
			_game_status = GameConstants.GS_MJ_PLAY;
			send_fan_pai_result();
			operate_out_card(GameConstants.INVALID_SEAT, 0, null, WSKConstants.WSK_YXZD_CT_ERROR,
					GameConstants.INVALID_SEAT);
			set_handler(_handler_out_card_operate);
			return;
		}
		}
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (_game_status == GameConstants.GS_MJ_PAO) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (seat_index == i) {
						continue;
					}
					this._player_result.pao[i] = 0;
				}
			}
		}
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		ret = this.on_handler_game_finish(seat_index, reason);
		return ret;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_yxzd.Builder game_end_wsk = PukeGameEndWsk_yxzd.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		int score_type = -1;
		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {

			score_type = cal_score_wsk(end_score, seat_index);

		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += end_score[i];
		}
		this.load_player_info_data_game_end(game_end_wsk);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_get_score_info[this._cur_round - 1][i] = this._get_score[i];
				_end_score_info[this._cur_round - 1][i] = end_score[i];
				game_end_wsk.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_wsk.addCardsData(i, cards_card);
				game_end_wsk.addSeatTeam(this._seat_team[i]);
			}

			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			for (int i = 0; i < this._di_pai_count; i++) {
				game_end_wsk.addDiPaiCard(this._di_pai[i]);
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				game_end_wsk.addCardsData(cards_card);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (end_score[i] > _max_end_score[i]) {
				_max_end_score[i] = end_score[i];
			}
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
					game_end.addGameScore(end_score[j]);
					break;
				}
			}
			game_end_wsk.addEndScore(end_score[i]);
			game_end_wsk.addGetScore(this._get_score[i]);
			boolean is_out_finish = false;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (_chuwan_shunxu[j] == i) {
					game_end_wsk.addWinOrder(j);
					_win_order_info[this._cur_round - 1][i] = j;
					is_out_finish = true;
					break;
				}
			}
			if (!is_out_finish) {
				game_end_wsk.addWinOrder(GameConstants.INVALID_SEAT);
			}
		}
		for (int i = 0; i < this._cur_round; i++) {
			Int32ArrayResponse.Builder end_score_data = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder get_score_data = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder win_order_data = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				end_score_data.addItem(_end_score_info[i][j]);
				get_score_data.addItem(_get_score_info[i][j]);
				win_order_data.addItem(_win_order_info[i][j]);
			}
			game_end_wsk.addEndScoreInfo(end_score_data);
			game_end_wsk.addGetScoreInfo(get_score_data);
			game_end_wsk.addWinOrderInfo(win_order_data);
		}
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				float change_score[] = new float[getTablePlayerNumber()];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
							change_score[i] = _player_result.game_score[j];

						}
					}
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					_player_result.game_score[i] = change_score[i];
					this.get_players()[i] = _init_players[i];
					this.get_players()[i].set_seat_index(i);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			float change_score[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
						change_score[i] = _player_result.game_score[j];

					}
				}
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.game_score[i] = change_score[i];
				this.get_players()[i] = _init_players[i];
				this.get_players()[i].set_seat_index(i);
			}
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
		}
		game_end_wsk.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		this.load_player_info_data(roomResponse);
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		this.operate_player_data();
		record_game_round(game_end, real_reason);
		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (!is_sys()) {
			GRR = null;
		}
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		Arrays.fill(_get_score, 0);
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Player robot_player = this.get_players()[i];
			if (robot_player != null && robot_player.isRobot()) {
				this._player_ready[i] = 1;
			}
		}
		// 错误断言
		return false;
	}

	/**
	 * 
	 * @param end_score
	 * @param dang_ju_fen
	 * @param win_seat_index
	 * @param jia_fa_socre
	 */
	public int cal_score_wsk(int end_score[], int win_seat_index) {

		int score = (int) this.game_cell;
		int times = 0;
		int score_type = -1;
		if (win_seat_index == GameConstants.INVALID_SEAT) {

			// 一二游为同一家
			if (_seat_team[_chuwan_shunxu[0]] == _seat_team[_chuwan_shunxu[1]]) {
				int one_total_score = this._get_score[_chuwan_shunxu[0]] + this._get_score[_chuwan_shunxu[1]] + 400;
				int two_total_score = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._seat_team[i] != this._seat_team[_chuwan_shunxu[0]]) {
						two_total_score += _get_score[i];
					}
				}

				// 差距分数
				int gap_score = one_total_score - two_total_score;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._seat_team[i] != this._seat_team[_chuwan_shunxu[0]]) {
						end_score[i] -= gap_score;
					} else {
						end_score[i] += gap_score;
					}
				}

			} else if (_seat_team[_chuwan_shunxu[0]] == _seat_team[_chuwan_shunxu[2]]) {
				// 一三游一伙
				int one_total_score = this._get_score[_chuwan_shunxu[0]] + this._get_score[_chuwan_shunxu[2]];
				int two_total_score = this._get_score[_chuwan_shunxu[1]] + this._get_score[_chuwan_shunxu[3]];
				if (has_rule(WSKConstants.GAME_YXZD_SHANG_YOU_150)) {
					one_total_score += 300;
				} else {
					one_total_score += 200;
				}

				// 差距分数
				int gap_score = one_total_score - two_total_score;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._seat_team[i] != this._seat_team[_chuwan_shunxu[0]]) {
						end_score[i] -= gap_score;
					} else {
						end_score[i] += gap_score;
					}
				}
			} else {
				// 一四游一伙
				int one_total_score = this._get_score[_chuwan_shunxu[0]] + this._get_score[_chuwan_shunxu[3]];
				int two_total_score = this._get_score[_chuwan_shunxu[1]] + this._get_score[_chuwan_shunxu[2]];
				// 差距分数
				int gap_score = one_total_score - two_total_score;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._seat_team[i] != this._seat_team[_chuwan_shunxu[0]]) {
						end_score[i] -= gap_score;
					} else {
						end_score[i] += gap_score;
					}
				}
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != win_seat_index && this._seat_team[i] != this._friend_seat[win_seat_index]) {
					end_score[i] -= score;
				} else {
					end_score[i] += score;
				}
			}
		}

		return score_type;
	}

	public void load_player_info_data_game_start(GameStart_yxzd.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_yxzd.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndWsk_yxzd.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_yxzd.Builder outcarddata = OutCardDataWsk_yxzd.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_OUT_CARD);// 201
				roomResponse.setTarget(seat_index);
				for (int j = 0; j < count; j++) {
					outcarddata.addCardsData(cards_data[j]);
				}
				// 上一出牌数据
				outcarddata.setPrCardsCount(this._turn_out_card_count);
				for (int i = 0; i < this._turn_out_card_count; i++) {
					outcarddata.addPrCardsData(this._turn_out_card_data[i]);
				}
				outcarddata.setCardsCount(count);
				outcarddata.setOutCardPlayer(seat_index);
				outcarddata.setCardType(type);
				outcarddata.setCurPlayer(this._current_player);
				outcarddata.setDisplayTime(10);
				outcarddata.setPrOutCardType(_turn_out_card_type);

				if (_is_shou_chu == 1) {
					outcarddata.setIsFirstOut(true);
				} else {
					outcarddata.setIsFirstOut(false);
				}
				if (_turn_out_card_count == 0) {
					outcarddata.setIsCurrentFirstOut(1);

				} else {
					outcarddata.setIsCurrentFirstOut(0);
				}
				if (index == this._current_player) {
					int tip_out_card[][] = new int[GRR._card_count[index] * 10][GRR._card_count[index]];
					int tip_out_count[] = new int[GRR._card_count[index] * 10];
					int tip_type_count = 0;
					tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index],
							this._turn_out_card_data, this._turn_out_card_count, tip_out_card, tip_out_count,
							tip_type_count);
					for (int i = 0; i < tip_type_count; i++) {
						int card_type = this._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
						for (int j = 0; j < tip_out_count[i]; j++) {
							outcarddata.addUserCanOutData(tip_out_card[i][j]);
						}
						outcarddata.setUserCanOutCount(tip_out_count[i]);
						break;
					}
					if (tip_type_count == 0) {
						outcarddata.setUserCanOutCount(0);
					}
				} else if (this._current_player != GameConstants.INVALID_SEAT
						&& this._seat_team[index] == this._seat_team[this._current_player]) {
					Player player = this.get_players()[_current_player];
					if (this._is_contorled[(index + 2) % this.getTablePlayerNumber()] == 1
							|| (player != null && player.isRobot())) {
						int tip_out_card[][] = new int[GRR._card_count[this._current_player]
								* 10][GRR._card_count[this._current_player]];
						int tip_out_count[] = new int[GRR._card_count[this._current_player] * 10];
						int tip_type_count = 0;
						tip_type_count = this._logic.search_out_card(GRR._cards_data[this._current_player],
								GRR._card_count[this._current_player], this._turn_out_card_data,
								this._turn_out_card_count, tip_out_card, tip_out_count, tip_type_count);
						for (int i = 0; i < tip_type_count; i++) {
							int card_type = this._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
							for (int j = 0; j < tip_out_count[i]; j++) {
								outcarddata.addUserCanOutData(tip_out_card[i][j]);
							}
							outcarddata.setUserCanOutCount(tip_out_count[i]);
							break;
						}
						if (tip_type_count == 0) {
							outcarddata.setUserCanOutCount(0);
						}
					}
				}
				if ((index + 2) % this.getTablePlayerNumber() == this._current_player) {
					Player player = this.get_players()[(index + 2) % this.getTablePlayerNumber()];
					if (this._is_contorled[(index + 2) % this.getTablePlayerNumber()] == 1
							|| (player != null && player.isRobot())) {
						outcarddata.setIsCanOutFriendcards(true);
					} else {
						outcarddata.setIsCanOutFriendcards(false);
					}
				} else {
					outcarddata.setIsCanOutFriendcards(false);
				}
				if (this._is_contorled[index] == 1) {
					outcarddata.setIsCanOpreate(false);
				} else {
					outcarddata.setIsCanOpreate(true);
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					Player player = this.get_players()[i];
					if (player == null) {
						continue;
					}
					if (i == index) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					} else if (player.isRobot() && this._seat_team[index] == this._seat_team[i]) {
						for (int j = 0; j < GRR._card_count[i]; j++) {

							cards_card.addItem(GRR._cards_data[i][j]);
						}
					} else if (this._seat_team[index] == this._seat_team[i]
							&& this.has_rule(WSKConstants.GAME_YXZD_CAN_YI_TUO_ER)) {
						for (int j = 0; j < GRR._card_count[i]; j++) {

							cards_card.addItem(GRR._cards_data[i][j]);
						}
					}

					outcarddata.addHandCardsData(cards_card);
					outcarddata.addHandCardCount(this.GRR._card_count[i]);

					boolean is_out_finish = false;
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (_chuwan_shunxu[j] == i) {
							outcarddata.addWinOrder(j);
							is_out_finish = true;
							break;
						}

					}
					if (!is_out_finish) {
						outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
					}
				}

				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_yxzd.Builder outcarddata = OutCardDataWsk_yxzd.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_turn_out_card_type);

			if (_is_shou_chu == 1) {
				outcarddata.setIsFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);

			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				boolean is_out_finish = false;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (_chuwan_shunxu[j] == i) {
						outcarddata.addWinOrder(j);
						is_out_finish = true;
						break;
					}

				}
				if (!is_out_finish) {
					outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_yxzd.Builder outcarddata = OutCardDataWsk_yxzd.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);
			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_turn_out_card_type);

			if (_is_shou_chu == 1) {
				outcarddata.setIsFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);

			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (to_player == this._current_player) {
				int tip_out_card[][] = new int[GRR._card_count[to_player] * 10][GRR._card_count[to_player]];
				int tip_out_count[] = new int[GRR._card_count[to_player] * 10];
				int tip_type_count = 0;
				tip_type_count = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player],
						this._turn_out_card_data, this._turn_out_card_count, tip_out_card, tip_out_count,
						tip_type_count);
				for (int i = 0; i < tip_type_count; i++) {
					int card_type = this._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
					for (int j = 0; j < tip_out_count[i]; j++) {
						outcarddata.addUserCanOutData(tip_out_card[i][j]);
					}
					outcarddata.setUserCanOutCount(tip_out_count[i]);
					break;
				}
				if (tip_type_count == 0) {
					outcarddata.setUserCanOutCount(0);
				}
			} else if (this._current_player != GameConstants.INVALID_SEAT
					&& this._seat_team[to_player] == this._seat_team[this._current_player]) {
				Player player = this.get_players()[_current_player];
				if (this._is_contorled[this._current_player] == 1 || (player != null && player.isRobot())) {
					int tip_out_card[][] = new int[GRR._card_count[to_player] * 10][GRR._card_count[to_player]];
					int tip_out_count[] = new int[GRR._card_count[to_player] * 10];
					int tip_type_count = 0;
					tip_type_count = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player],
							this._turn_out_card_data, this._turn_out_card_count, tip_out_card, tip_out_count,
							tip_type_count);
					for (int i = 0; i < tip_type_count; i++) {
						int card_type = this._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
						for (int j = 0; j < tip_out_count[i]; j++) {
							outcarddata.addUserCanOutData(tip_out_card[i][j]);
						}
						outcarddata.setUserCanOutCount(tip_out_count[i]);
						break;
					}
					if (tip_type_count == 0) {
						outcarddata.setUserCanOutCount(0);
					}
				}
			}
			if ((to_player + 2) % this.getTablePlayerNumber() == this._current_player) {
				Player player = this.get_players()[(to_player + 2) % this.getTablePlayerNumber()];
				if (this._is_contorled[(to_player + 2) % this.getTablePlayerNumber()] == 1
						|| (player != null && player.isRobot())) {
					outcarddata.setIsCanOutFriendcards(true);
				} else {
					outcarddata.setIsCanOutFriendcards(false);
				}
			} else {
				outcarddata.setIsCanOutFriendcards(false);
			}
			if (this._is_contorled[to_player] == 1) {
				outcarddata.setIsCanOpreate(false);
			} else {
				outcarddata.setIsCanOpreate(true);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Player player = this.get_players()[i];
				if (player == null) {
					continue;
				}
				if (i == to_player) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else if (player.isRobot() && this._seat_team[to_player] == this._seat_team[i]) {
					for (int j = 0; j < GRR._card_count[i]; j++) {

						cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else if (this._seat_team[to_player] == this._seat_team[i]
						&& this.has_rule(WSKConstants.GAME_YXZD_CAN_YI_TUO_ER)) {
					for (int j = 0; j < GRR._card_count[i]; j++) {

						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);

				boolean is_out_finish = false;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (_chuwan_shunxu[j] == i) {
						outcarddata.addWinOrder(j);
						is_out_finish = true;
						break;
					}

				}
				if (!is_out_finish) {
					outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	void send_card() {
		for (int index = 0; index < getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			SendCard_yxzd.Builder send_card = SendCard_yxzd.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();
				Player player = this.get_players()[i];
				if (player == null) {
					continue;
				}
				if (index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {

						hand_cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else if (player.isRobot() && this._seat_team[index] == this._seat_team[i]) {
					for (int j = 0; j < GRR._card_count[i]; j++) {

						hand_cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				send_card.addCardsData(hand_cards_card);
				send_card.addCardCount(GRR._card_count[i]);
			}
			// 发送数据
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			// 自己才有牌数据
			this.send_response_to_player(index, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		SendCard_yxzd.Builder send_card = SendCard_yxzd.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GRR._card_count[i]; j++) {

				hand_cards_card.addItem(GRR._cards_data[i][j]);
			}
			send_card.addCardsData(hand_cards_card);
			send_card.addCardCount(GRR._card_count[i]);
		}
		// 发送数据
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));
		GRR.add_room_response(roomResponse);
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_YXZD_OPERATE) {
			Opreate_RequestWsk_yxzd req = PBUtil.toObject(room_rq, Opreate_RequestWsk_yxzd.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getFanPaiIndex(),
					req.getCardsDataList(), req.getOutType(), req.getSeatIndex());
		}

		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int call_index, List<Integer> list,
			int out_type, int other_seat_index) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_OUT_CARD: {
			this.deal_out_card(other_seat_index, list, out_type);
			return true;
		}
		case GAME_OPREATE_TYPE_FAN_PAI: {
			this.deal_fan_pai(seat_index, call_index);
			return true;
		}
		case GAME_OPREATE_TYPE_YI_TUO_ER: {
			this.deal_yi_tuo_er(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_CONTROL: {
			deal_control_friend(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_CONTROL_CANCEL: {
			deal_control_friend_cancel(seat_index);
			return true;
		}
		}
		return true;
	}

	public void deal_out_card(int seat_index, List<Integer> list, int out_type) {
		this._handler = this._handler_out_card_operate;
		int card_count = list.size();
		int out_cards[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			out_cards[i] = list.get(i);
		}

		this._handler_out_card_operate.reset_status(seat_index, out_cards, card_count, out_type);
		this._handler.exe(this);
	}

	public void deal_fan_pai(int seat_index, int call_index) {
		if (this._game_status != WSKConstants.GS_YXZD_FAN_PAI) {
			return;
		}
		this._handler_call_banker.handler_call_banker(this, seat_index, call_index);
	}

	public void deal_control_friend_cancel(int seat_index) {
		if (this._game_status == GameConstants.GS_MJ_FREE || !this.has_rule(WSKConstants.GAME_YXZD_CAN_YI_TUO_ER)) {
			return;
		}
		if (this._is_contorled[seat_index] == 0) {
			return;
		}
		this._is_contorled[seat_index] = 0;
		this.send_yi_tuo_er_status(seat_index);
		this.send_yi_tuo_er_status((seat_index + 2) % this.getTablePlayerNumber());

		this._handler.handler_player_be_in_room(this, seat_index);
		this._handler.handler_player_be_in_room(this, (seat_index + 2) % this.getTablePlayerNumber());
	}

	public void deal_control_friend(int seat_index) {
		if (this._game_status == GameConstants.GS_MJ_FREE || !this.has_rule(WSKConstants.GAME_YXZD_CAN_YI_TUO_ER)) {
			return;
		}
		if (this._is_contorled[seat_index] == 1
				|| this._is_contorled[(seat_index + 2) % this.getTablePlayerNumber()] == 1) {
			return;
		}
		this._is_contorled[(seat_index + 2) % this.getTablePlayerNumber()] = 1;
		this.send_yi_tuo_er_status(seat_index);
		this.send_yi_tuo_er_status((seat_index + 2) % this.getTablePlayerNumber());

		this._handler.handler_player_be_in_room(this, seat_index);
		this._handler.handler_player_be_in_room(this, (seat_index + 2) % this.getTablePlayerNumber());
	}

	public void deal_yi_tuo_er(int seat_index) {
		if (this._game_status != GameConstants.GS_MJ_FREE || !this.has_rule(WSKConstants.GAME_YXZD_CAN_YI_TUO_ER)) {
			return;
		}
		Player player = this.get_players()[(seat_index + 2) % this.getTablePlayerNumber()];
		if (player != null && player.isRobot()) {
			this._player_ready[(seat_index + 2) % this.getTablePlayerNumber()] = 0;
			this.get_players()[(seat_index + 2) % this.getTablePlayerNumber()] = null;
			this.operate_player_data();
			this.send_yi_tuo_er_status(seat_index);
			return;
		} else if (player != null) {
			return;
		}

		Player new_player = new Player();
		RobotRandom robotRandom = RobotService.getInstance().getRobotRandom();
		new_player = robotRandom.getRandomNomalPlayer((seat_index + 2) % this.getTablePlayerNumber(),
				this.getRoom_id());
		this.get_players()[(seat_index + 2) % this.getTablePlayerNumber()] = new_player;
		this.operate_player_data();
		this.send_yi_tuo_er_status(seat_index);

		this.handler_player_ready((seat_index + 2) % this.getTablePlayerNumber(), false);
	}

	public void deal_sort_card(int seat_index) {
		if (_game_status == GameConstants.GS_MJ_WAIT || _game_status == GameConstants.GS_MJ_FREE) {
			return;
		}
		if (_score_type[seat_index] == GameConstants.WSK_ST_CUSTOM) {
			_score_type[seat_index] = GameConstants.WSK_ST_ORDER;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_ORDER) {
			_score_type[seat_index] = GameConstants.WSK_ST_COUNT;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_score_type[seat_index] = GameConstants.WSK_ST_CUSTOM;
		}

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type[seat_index]);
		RefreshCard(seat_index);
	}

	public void send_yi_tuo_er_status(int seat_index) {

		Player player = this.get_players()[(seat_index + 2) % this.getTablePlayerNumber()];
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_YI_TUO_ER);
		// 发送数据
		Contorl_Toward_yxzd.Builder can_yi_tuo_er = Contorl_Toward_yxzd.newBuilder();
		if (this.has_rule(WSKConstants.GAME_YXZD_CAN_YI_TUO_ER)) {
			if (player == null) {
				can_yi_tuo_er.setType(1);
			} else {
				if (player.isRobot()) {
					if (this._game_status == GameConstants.GS_MJ_FREE) {
						can_yi_tuo_er.setType(2);
					} else {
						can_yi_tuo_er.setType(6);
					}
				} else {
					if (this._game_status == GameConstants.GS_MJ_FREE) {
						can_yi_tuo_er.setType(3);
					} else {
						if (_is_contorled[seat_index] == 0) {
							if (_is_contorled[(seat_index + 2) % this.getTablePlayerNumber()] == 0) {
								can_yi_tuo_er.setType(4);
							} else {
								can_yi_tuo_er.setType(6);
							}
						} else {
							can_yi_tuo_er.setType(5);
						}

					}
				}
			}
		} else {
			can_yi_tuo_er.setType(3);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(can_yi_tuo_er));
		this.send_response_to_player(seat_index, roomResponse);
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_REFRESH_CARD);
		// 发送数据
		RefreshCardData_Sxth.Builder refresh_card = RefreshCardData_Sxth.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (to_player == i) {
				tagAnalyseIndexResult_WSK hand_index = new tagAnalyseIndexResult_WSK();
				this._logic.AnalysebCardDataToIndex(GRR._cards_data[i], GRR._card_count[i], hand_index);

				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					if (j == 0 || this._logic.GetCardValue(GRR._cards_data[i][j - 1]) != this._logic
							.GetCardValue(GRR._cards_data[i][j])) {
						int count = 1;
						for (int x = j + 1; x < this.GRR._card_count[i]; x++) {
							if (this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic
									.GetCardValue(GRR._cards_data[i][x])) {
								count++;
							} else {
								break;
							}
						}
						cards_card.addItem(GRR._cards_data[i][j] + count * 0x100);
					} else {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

			}
			refresh_card.addHandCardsData(cards_card);
			refresh_card.addHandCardCount(GRR._card_count[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);

	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_yi_tuo_er_status(i);
		}

		return true;
	}

	@Override
	protected void set_result_describe() {
		// TODO Auto-generated method stub

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
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
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
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Player robot_player = this.get_players()[i];
				if (robot_player != null && robot_player.isRobot()) {
					_gameRoomRecord.release_players[i] = 1;// 同意
				}
			}
			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
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
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i].isRobot()) {
					roomResponse.addReleasePlayers(1);
				} else {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}

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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

				for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.send_yi_tuo_er_status(i);
				Player robotplayer = this.get_players()[i];
				if (robotplayer != null && robotplayer.isRobot()) {
					_player_ready[i] = 0;
					this.get_players()[i] = null;
				}

			}
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

			if (player.getAccount_id() == getRoom_owner_account_id()) {
				this.getCreate_player().set_seat_index(GameConstants.INVALID_SEAT);
			}
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card,
			String desc) {
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, card_count, b_out_card);
			this._handler.exe(this);
		}
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 是否春天
	 * 
	 * @return
	 */
	public boolean checkChunTian(int seatIndex) {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (player == seatIndex) {
				continue;
			}
			// 27张牌，一张没出
			if (GRR._card_count[player] != get_hand_card_count_max()) {
				return false;
			}
		}
		return true;
	}
}
