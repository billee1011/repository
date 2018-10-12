package com.cai.game.wsk.handler.hts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKGameLogic_HTS;
import com.cai.game.wsk.WSKType;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.hts.htsRsp.GameStart_hts;
import protobuf.clazz.hts.htsRsp.OutCardData_hts;
import protobuf.clazz.hts.htsRsp.PukeGameEnd_hts;
import protobuf.clazz.hts.htsRsp.RefreshCardData_hts;
import protobuf.clazz.hts.htsRsp.TableResponse_hts;
import protobuf.clazz.hts.htsRsp.call_banker_request_hts;
import protobuf.clazz.hts.htsRsp.effect_type_hts;
import protobuf.clazz.hts.htsRsp.get_score_hts;
import protobuf.clazz.hts.htsRsp.lipai_request_hts;
import protobuf.clazz.hts.htsRsp.send_friend_hts;
import protobuf.clazz.sxth.SxthRsp.Opreate_RequestWsk_Sxth;

public class WSKTable_HTS extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间

	protected static final int ID_TIMER_CALL_BANKER = 2;// 叫庄

	protected static final int CARD_HONG_TAO_3 = 0x23;// 红桃三
	protected static final int CARD_HEI_TAO_3 = 0x33;// 黑桃三

	protected static final int GAME_OPREATE_TYPE_SORT = 5;// 排序

	public int _get_score[];
	public int _flower_score[];
	public int _is_call_banker[];
	public int _turn_have_score;
	public int _score_type[];
	public int _jiao_pai_card;
	public int _out_card_ming_ji;
	public int _end_score[];

	public boolean have_chengbao;

	public int frist_our_player;

	public int hei_san_player;

	public int cheng_bao_player;

	public int table_score;

	public int win_order[];

	public int winer_index;

	public boolean last_out_finish;

	public boolean show_hts_player;

	public int second_winer;

	public int score_card[][];
	public int score_card_count[];

	public int cur_score_card[];
	public int cur_score_card_count;

	public int blipai[];

	public List<Integer> list_cur_score_card;

	public List<Integer>[] list_score_card;

	// 大结算
	public int[] win_count;
	public int[] hei_tao_san_count;
	public int[] cheng_bao_count;

	public WSKGameLogic_HTS _logic;
	public HandlerCallBnaker_HTS _hander_call_banker;

	public WSKTable_HTS() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_HTS();
		_get_score = new int[getTablePlayerNumber()];
		_flower_score = new int[getTablePlayerNumber()];
		_xi_qian_total_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_friend_seat = new int[getTablePlayerNumber()];
		_is_call_banker = new int[getTablePlayerNumber()];
		_tou_xiang_times = new int[getTablePlayerNumber()];
		_max_end_score = new int[getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()];
		_turn_real_card_data = new int[get_hand_card_count_max()];
		_cur_out_car_type = new int[getTablePlayerNumber()];
		win_order = new int[getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = new int[getTablePlayerNumber()];
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_HTS();
		_hander_call_banker = new HandlerCallBnaker_HTS();
		win_count = new int[getTablePlayerNumber()];
		cheng_bao_count = new int[getTablePlayerNumber()];
		hei_tao_san_count = new int[getTablePlayerNumber()];
		cur_score_card = new int[20];
		score_card = new int[getTablePlayerNumber()][20];
		score_card_count = new int[getTablePlayerNumber()];
		blipai = new int[getTablePlayerNumber()];
		list_cur_score_card = new ArrayList<Integer>();
		list_score_card = new ArrayList[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = i;
			_max_end_score[i] = 0;
			_end_score[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_ORDER;
			list_score_card[i] = new ArrayList<Integer>();
		}

	}

	public void Refresh_user_get_score(int to_player, boolean is_dealy) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		get_score_hts.Builder refresh_user_getscore = get_score_hts.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_GET_SCORE);
		int banker_get = 0;
		int other_get = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == this.GRR._banker_player) {
				banker_get = _get_score[i];
			} else {
				other_get += _get_score[i];
			}
			refresh_user_getscore.addGetScore(_get_score[i]);
		}
		if (this.GRR._banker_player < 0) {
			banker_get = 0;
			other_get = 0;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			// for(int j = 0; j < this.score_card_count[i];j++){
			// cards_card.addItem(this.score_card[i][j]);
			// }
			for (int j = 0; j < this.list_score_card[i].size(); j++) {
				cards_card.addItem(this.list_score_card[i].get(j));
			}
			refresh_user_getscore.addScoreCard(cards_card);
		}
		refresh_user_getscore.addShuangFangFen(banker_get);
		refresh_user_getscore.addShuangFangFen(other_get);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));
		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	void InitParam() {

		_current_player = _cur_banker;

		have_chengbao = false;
		table_score = 0;
		winer_index = 0;
		hei_san_player = -1;
		cheng_bao_player = -1;
		frist_our_player = GameConstants.INVALID_SEAT;
		last_out_finish = false;
		show_hts_player = false;
		second_winer = -1;

		list_cur_score_card.clear();

		_turn_three_link_num = 0;
		_turn_out_card_count = 0;
		_turn_have_score = 0;
		_turn_out_card_type = GameConstants.HTS_CT_ERROR;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		Arrays.fill(_turn_real_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			list_score_card[i].clear();

			_get_score[i] = 0;
			win_order[i] = -1;
			_cur_out_card_count[i] = 0;
			_is_tou_xiang_agree[i] = -1;
			_is_tou_xiang[i] = -1;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_xi_qian_times[i] = 0;
			_flower_score[i] = 0;
			blipai[i] = 2;
			_cur_out_car_type[i] = GameConstants.SXTH_CT_ERROR;
			this._friend_seat[i] = (i + 2) % this.getTablePlayerNumber();

			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);

			score_card_count[i] = 0;
			for (int j = 0; j < 20; j++) {
				score_card[i][j] = GameConstants.INVALID_CARD;
				cur_score_card[j] = GameConstants.INVALID_CARD;
			}
		}

		_pai_score_count = 24;
		_pai_score = 200;
		_logic.hts_zuo_fei = false;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_out_card_ming_ji = GameConstants.INVALID_CARD;
		cur_score_card_count = 0;

		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		reset_init_data();

		InitParam();

		int rand = RandomUtil.generateRandomNumber(1, 4);
		if (rand == 1) {
			_repertory_card = new int[WSKConstants.CARD_DATA_HTS_NO_KING_1.length];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_HTS_NO_KING_1);
		} else if (rand == 2) {
			_repertory_card = new int[WSKConstants.CARD_DATA_HTS_NO_KING_2.length];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_HTS_NO_KING_2);
		} else if (rand == 3) {
			_repertory_card = new int[WSKConstants.CARD_DATA_HTS_NO_KING_3.length];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_HTS_NO_KING_3);
		} else if (rand == 4) {
			_repertory_card = new int[WSKConstants.CARD_DATA_HTS_NO_KING_4.length];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_HTS_NO_KING_4);
		} else {
			_repertory_card = new int[WSKConstants.CARD_DATA_HTS_NO_KING_1.length];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_HTS_NO_KING_1);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		// 庄家选择
		this.progress_banker_select();
		hei_san_player = find_hei_tao_san();
		Refresh_user_get_score(GameConstants.INVALID_SEAT, true);

		if (!find_specified_card(CARD_HONG_TAO_3)) {
			return false;
		}

		on_game_start();

		return true;
	}

	@Override
	protected boolean on_game_start() {

		GRR._banker_player = _cur_banker;
		this._current_player = frist_our_player;
		_game_status = GameConstants.GS_HTS_SEND_CARD;
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_hts.Builder gamestart = GameStart_hts.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			this.load_player_info_data_game_start(gamestart);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				gamestart.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				gamestart.addCardsData(cards_card);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_hts.Builder gamestart = GameStart_hts.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		this._current_player = frist_our_player;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart.addCardsData(cards_card);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		GRR.add_room_response(roomResponse);

		// schedule(ID_TIMER_CALL_BANKER, SheduleArgs.newArgs(), 2000);

		this.set_handler(this._hander_call_banker);
		this._hander_call_banker.reset_status(frist_our_player);
		_hander_call_banker.exe(this);

		return true;
	}

	@Override
	public int get_hand_card_count_max() {
		return 17;
	}

	// 所有玩家中找指定的牌
	public boolean find_specified_card(int card) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				if (GRR._cards_data[i][j] == card) {
					if (card == CARD_HONG_TAO_3)
						frist_our_player = i;
					return true;
				}
			}
		}
		return false;
	}

	// 所有玩家中找指定的牌
	public int find_hei_tao_san() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				if (GRR._cards_data[i][j] == CARD_HEI_TAO_3) {
					return i;
				}
			}
		}
		return -1;
	}

	// 指定的玩家找指定的牌
	public boolean find_specified_card_by_specified_player(int player, int card) {
		for (int j = 0; j < GRR._card_count[player]; j++) {
			if (GRR._cards_data[player][j] == card) {
				return true;
			}
		}
		return false;
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
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_VALUE);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 23, 11, 22, 1, 21, 12, 9, 6, 45, 13, 37, 19, 25, 20, 3, 29, 42, 5, 60, 55, 17, 7, 44, 36, 43, 53, 59, 41, 35, 54,
				27, 33, 10, 61, 26, 57, 40, 4, 50, 28, 18, 49, 39, 24, 51, 52, 34, 58, 38, 56, 8 };
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
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_VALUE);
		}
	}

	public void out_card_begin() {
		this._current_player = this.frist_our_player;
		_game_status = GameConstants.GS_HTS_PLAY;
		this.Refresh_user_get_score(GameConstants.INVALID_SEAT, false);
		this.set_handler(this._handler_out_card_operate);
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.HTS_CT_ERROR, GameConstants.INVALID_SEAT, false);

	}

	public void send_to_friend(int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_SEND_BANKER);
		send_friend_hts.Builder friend = send_friend_hts.newBuilder();
		friend.setType(type);
		friend.setBankerPlayer(GRR._banker_player);
		roomResponse.setCommResponse(PBUtil.toByteString(friend));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD: {
			// 投降
			if (this.has_rule(GameConstants.GAME_RULE_SXTH_TOU_XIANG)) {
				_game_status = GameConstants.GS_SXTH_TOU_XIANG;
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_TOUXIANG_BEGIN);
				this.send_response_to_room(roomResponse);
				this.set_handler(this._handler_out_card_operate);
			} else {
				out_card_begin();
			}

			return;
		}
		case ID_TIMER_CALL_BANKER: {
			this.set_handler(this._hander_call_banker);
			this._hander_call_banker.reset_status(frist_our_player);
			this._hander_call_banker.exe(this);
			return;
		}
		}
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
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

		if (this.have_chengbao) {
			this.cheng_bao_count[this.cheng_bao_player]++;
		}
		if (!this._logic.hts_zuo_fei) {
			this.hei_tao_san_count[this.hei_san_player]++;
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEnd_hts.Builder game_end_wsk = PukeGameEnd_hts.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		if (GRR != null) {
			if (this.have_chengbao) {
				game_end_wsk.setChengbaoPlayer(GRR._banker_player);
			} else {
				game_end_wsk.setChengbaoPlayer(-1);
			}
			process_game_score(end_score);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += end_score[i];
		}
		this.load_player_info_data_game_end(game_end_wsk);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				game_end_wsk.addHandCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_wsk.addHandCardData(cards_card);

			}

			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				game_end_wsk.addHandCardData(cards_card);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_wsk.addEndScore(end_score[i]);
			game_end_wsk.addGetScore(this._get_score[i]);
			game_end.addGameScore(end_score[i]);
			game_end_wsk.addWinOrder(this.win_order[i]);

		}
		// 大结算数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_wsk.addWinNum(this.win_count[i]);
			game_end_wsk.addCallBankerNum(this.cheng_bao_count[i]);
			game_end_wsk.addHtsNum(this.hei_tao_san_count[i]);
			game_end_wsk.addTotalEndScore((int) this._player_result.game_score[i]);
		}
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;

				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		game_end_wsk.setEndReason(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		this.operate_player_data();
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

		if (!is_sys()) {
			GRR = null;
		}
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		// 错误断言
		return false;
	}

	public void process_game_score(int[] endscore) {
		int frist_win = -1;
		int second_win = -1;
		int thirdly_win = -1;
		// 添加最后一游
		if (this.winer_index == 2) {
			int lastwiner = -1;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (win_order[i] == -1) {
					lastwiner = i;
					break;
				}
			}
			if (lastwiner == -1) {
				return;
			}
			win_order[lastwiner] = 2;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (win_order[i] == 0) {
				frist_win = i;
			}
			if (win_order[i] == 1) {
				second_win = i;
			}
			if (win_order[i] == 2) {
				thirdly_win = i;
			}
		}
		if (GRR._banker_player == -1 && !this.have_chengbao) {
			GRR._banker_player = this.hei_san_player;
		}
		if (frist_win == -1 || GRR._banker_player == -1) {
			return;
		}
		if (this.have_chengbao) {
			if (frist_win == GRR._banker_player) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					endscore[i] -= 3;
					endscore[GRR._banker_player] += 3;
				}
			} else {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					endscore[i] += 3;
					endscore[GRR._banker_player] -= 3;
				}
			}
		} else {
			if (GRR._banker_player == frist_win) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					endscore[i] -= 2;
					endscore[GRR._banker_player] += 2;
				}
			} else if (GRR._banker_player == second_win) {
				int other_get = 0;
				int banker_get = 0;
				banker_get = this._get_score[GRR._banker_player];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					other_get += _get_score[i];
				}
				if (banker_get > other_get) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == GRR._banker_player)
							continue;
						endscore[i] -= 1;
						endscore[GRR._banker_player] += 1;
					}
				} else if (banker_get < other_get) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == GRR._banker_player)
							continue;
						endscore[i] += 1;
						endscore[GRR._banker_player] -= 1;
					}
				}
			} else if (GRR._banker_player == thirdly_win) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player)
						continue;
					endscore[i] += 2;
					endscore[GRR._banker_player] -= 2;
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (endscore[i] > 0) {
				this.win_count[i]++;
			}
		}
	}

	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
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

	public void load_player_info_data_game_start(GameStart_hts.Builder roomResponse) {
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
			room_player.setScore(this._player_result.game_score[i]);
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

	public void load_player_info_data_reconnect(TableResponse_hts.Builder roomResponse) {
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
			room_player.setScore(this._player_result.game_score[i]);
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

	public void load_player_info_data_game_end(PukeGameEnd_hts.Builder roomResponse) {
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
			room_player.setScore(this._player_result.game_score[i]);
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, boolean is_deal) {

		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData_hts.Builder outcarddata = OutCardData_hts.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_OUT_CARD);// 201
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

			if (_turn_out_card_count == 0) {
				outcarddata.setIsFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
			}

			if (index == this._current_player) {
				int tip_out_card[][] = new int[GRR._card_count[index] * 10][GRR._card_count[index]];
				int tip_out_count[] = new int[GRR._card_count[index] * 10];
				int tip_type_count = 0;
				tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index], this._turn_out_card_data,
						this._turn_out_card_count, tip_out_card, tip_out_count, tip_type_count, this._turn_three_link_num);
				for (int i = 0; i < tip_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_out_count[i]; j++) {
						cards_card.addItem(tip_out_card[i][j]);
					}
					outcarddata.addUserCanOutData(cards_card);
					outcarddata.addUserCanOutCount(tip_out_count[i]);

				}
			} else {
				int tip_out_card[][] = new int[GRR._card_count[index] * 2][GRR._card_count[index]];
				int tip_out_count[] = new int[GRR._card_count[index] * 2];
				int tip_type_count = 0;
				tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index], null, 0, tip_out_card, tip_out_count,
						tip_type_count, this._turn_three_link_num);
				for (int i = 0; i < tip_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_out_count[i]; j++) {
						cards_card.addItem(tip_out_card[i][j]);
					}
					outcarddata.addUserCanOutData(cards_card);
					outcarddata.addUserCanOutCount(tip_out_count[i]);

				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == index) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				outcarddata.addWinOrder(this.win_order[i]);
			}

			outcarddata.setTableScore(this.table_score);
			outcarddata.setPlaneCount(this._turn_three_link_num);
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(index, roomResponse);

		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardData_hts.Builder outcarddata = OutCardData_hts.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_OUT_CARD);// 201
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

		if (_turn_out_card_count == 0) {
			outcarddata.setIsFirstOut(true);
		} else {
			outcarddata.setIsFirstOut(false);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(this.GRR._cards_data[i][j]);
			}
			outcarddata.addHandCardsData(cards_card);
			outcarddata.addHandCardCount(this.GRR._card_count[i]);
			outcarddata.addWinOrder(this.win_order[i]);
		}
		outcarddata.setTableScore(this.table_score);
		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_SXTH_OPERATE) {
			Opreate_RequestWsk_Sxth req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Sxth.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType());
		}
		if (type == MsgConstants.REQUST_CHENG_BAO_HTS) {
			call_banker_request_hts req = PBUtil.toObject(room_rq, call_banker_request_hts.class);
			return _hander_call_banker.handler_call_banker(this, seat_index, req.getOpreateType());
		}
		if (type == MsgConstants.REQUST_LI_PAI_HTS) {
			lipai_request_hts req = PBUtil.toObject(room_rq, lipai_request_hts.class);
			return set_lipai_state(seat_index, req.getType());
		}

		return true;
	}

	public boolean set_lipai_state(int seat_index, int type) {
		if (type != 1 && type != 2) {
			return false;
		}
		this.blipai[seat_index] = type;
		//
		if (type == 1) {
			this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], GameConstants.WSK_ST_510K);
		} else if (type == 2) {
			this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], GameConstants.WSK_ST_VALUE);
		}
		RefreshCard(seat_index);
		return true;
	}

	@Override
	public int getTablePlayerNumber() {
		return RoomComonUtil.getMaxNumber(getDescParams());
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_SORT: {
			this.deal_sort_card(seat_index);
			return true;
		}
		}
		return true;
	}

	public void deal_sort_card(int seat_index) {
		if (_game_status == GameConstants.GS_MJ_WAIT || _game_status == GameConstants.GS_MJ_FREE) {
			return;
		}
		if (_score_type[seat_index] == GameConstants.WSK_ST_ORDER) {
			_score_type[seat_index] = GameConstants.WSK_ST_COUNT;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_score_type[seat_index] = GameConstants.WSK_ST_ORDER;
		}

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], GameConstants.WSK_ST_VALUE);
		RefreshCard(seat_index);
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_REFRESH_CARD);
		// 发送数据
		RefreshCardData_hts.Builder refresh_card = RefreshCardData_hts.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (to_player == i) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
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
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
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

		return true;
	}

	@Override
	protected void set_result_describe() {
		// TODO Auto-generated method stub

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

	public void send_texiao(int type, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_EFFECT);
		effect_type_hts.Builder effect = effect_type_hts.newBuilder();
		effect.setSeatIndex(seat_index);
		effect.setType(type);
		roomResponse.setCommResponse(PBUtil.toByteString(effect));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}
}
