package com.cai.game.wsk.handler.dy;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKGameLogic_DY;
import com.cai.game.wsk.WSKType;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.hsdy.hsdyRsp.CallBankerResponse_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.GameStart_Wsk_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.LiangPai_Result_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.Opreate_RequestWsk_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.OutCardDataWsk_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.PaiFenData_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.PukeGameEndWsk_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.RefreshCardData_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.RefreshMingPai_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.TableResponse_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.record_out_card_hsdy;
import protobuf.clazz.xndg.XndgRsp.RefreshScore_xndg;

public class WSKTable_DY extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	public int _get_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public boolean _is_yi_da_san;
	public int _score_type[];

	public int _lose_num[];
	public int _win_num[];
	public int _tou_num[];
	public int _du_num[];
	public int _qi_xi_num[];
	public int _ba_xi_num[];
	public int _510_num[];
	public int _four_num[];
	public int _end_score[][];
	public int _zhua_score_max[];
	public int _all_card_count;
	protected static final int GAME_OPREATE_TYPE_LIANG_PAI = 1;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 2;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER_NO = 3;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_CARD = 4;
	private boolean is_touxiang;

	public WSKTable_DY() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_DY();
		_get_score = new int[getTablePlayerNumber()];
		_xi_qian_total_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_friend_seat = new int[getTablePlayerNumber()];
		_is_call_banker = new int[getTablePlayerNumber()];
		_tou_xiang_times = new int[getTablePlayerNumber()];
		_max_end_score = new int[getTablePlayerNumber()];
		_lose_num = new int[getTablePlayerNumber()];
		_win_num = new int[getTablePlayerNumber()];
		_tou_num = new int[getTablePlayerNumber()];
		_du_num = new int[getTablePlayerNumber()];
		_qi_xi_num = new int[getTablePlayerNumber()];
		_ba_xi_num = new int[getTablePlayerNumber()];
		_zhua_score_max = new int[getTablePlayerNumber()];
		_510_num = new int[getTablePlayerNumber()];
		_four_num = new int[getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()][this._game_round];
		_turn_real_card_data = new int[get_hand_card_count_max()];
		_cur_out_car_type = new int[this.getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = new int[getTablePlayerNumber()];
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_DY();
		_handler_call_banker = new WSKHandlerCallBnakerDY();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = i;
			_max_end_score[i] = 0;
			_tou_num[i] = 0;
			_du_num[i] = 0;
			_qi_xi_num[i] = 0;
			_ba_xi_num[i] = 0;
			_four_num[i] = 0;
			_510_num[i] = 0;
			_zhua_score_max[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_ORDER;
		}

		if (this.has_rule(GameConstants.GAME_RULE_HSDY_CELL_FIVE)) {
			game_cell = 5;
		} else if (this.has_rule(GameConstants.GAME_RULE_HSDY_CELL_EIGHT)) {
			game_cell = 8;
		} else {
			game_cell = 10;
		}
	}

	public void Refresh_Ming_Pai(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshMingPai_hsdy.Builder refresh_ming_pai = RefreshMingPai_hsdy.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_REFRESH_MING_PAI);

		refresh_ming_pai.setCardData(_out_card_ming_ji);
		if (_out_card_ming_ji == GameConstants.INVALID_CARD) {
			refresh_ming_pai.setSeatIndex(GameConstants.INVALID_SEAT);
		} else {
			refresh_ming_pai.setSeatIndex(this._friend_seat[this.GRR._banker_player]);
		}

		// refresh_user_getscore.setTableScore(value);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_ming_pai));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_xndg.Builder refresh_user_getscore = RefreshScore_xndg.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addXianQianScore(this._xi_qian_total_score[i]);
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
		GRR._banker_player = GameConstants.INVALID_SEAT;
		this._current_player = _cur_banker;
		this._turn_out_card_count = 0;
		_turn_out_card_type = GameConstants.HSDY_CT_ERROR;

		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_cur_out_car_type[i] = GameConstants.HSDY_CT_ERROR;
			_is_tou_xiang_agree[i] = -1;
			_is_tou_xiang[i] = 0;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_xi_qian_times[i] = 0;

			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}
		_is_yi_da_san = false;
		_game_status = GameConstants.GS_GFWSK_CALLBANKER;
		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_out_card_ming_ji = GameConstants.INVALID_CARD;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		_repertory_card = new int[GameConstants.CARD_COUNT_WSK];
		_all_card_count = GameConstants.CARD_COUNT_WSK;
		shuffle(_repertory_card, GameConstants.CARD_DATA_WSK);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

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
				if (this._logic.GetCardValue(GRR._cards_data[i][j]) == 5
						|| this._logic.GetCardValue(GRR._cards_data[i][j]) == 10
						|| this._logic.GetCardValue(GRR._cards_data[i][j]) == 13) {
					GRR._cards_data[i][j] += 0x100;
				}
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type[i]);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 0x4f, 0x4f, 0x4e, 0x4e, 0x01, 0x01, 0x11, 0x11, 0x21, 0x21, 0x31, 0x31, 0x02, 0x02,
				0x12, 0x12, 0x22, 0x22, 0x32, 0x04, 0x4, 0x14, 0x14, 0x24, 0x24, 0x34, 0x34 };
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
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[j];

			}
			GRR._card_count[i] = get_hand_card_count_max();
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
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
		}
	}

	@Override
	protected boolean on_game_start() {
		// int FlashTime = 4000;
		// int standTime = 1000;
		is_touxiang = false;
		this._current_player = _cur_banker;

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Wsk_hsdy.Builder gamestart = GameStart_Wsk_hsdy.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			this.load_player_info_data_game_start(gamestart);

			gamestart.setCurBanker(GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				if (!has_rule(GameConstants.GAME_RULE_HSDY_DISPLAY_CARD) && play_index != i) {
					gamestart.addCardCount(-1);
				} else {
					gamestart.addCardCount(GRR._card_count[i]);
				}

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
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
			this.send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_Wsk_hsdy.Builder gamestart = GameStart_Wsk_hsdy.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		gamestart.setCurBanker(GRR._banker_player);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder wang_cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}

			gamestart.addCardsData(cards_card);
		}

		gamestart.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		GRR.add_room_response(roomResponse);

		this.set_handler(this._handler_call_banker);
		this._handler.exe(this);

		Refresh_user_get_score(GameConstants.INVALID_SEAT);
		Refresh_pai_score(GameConstants.INVALID_SEAT);
		Refresh_Record_Out_Card(GameConstants.INVALID_SEAT);
		return true;
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
			if (_get_score[i] > _zhua_score_max[i]) {
				_zhua_score_max[i] = _get_score[i];
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_hsdy.Builder game_end_wsk = PukeGameEndWsk_hsdy.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		int end_score[] = new int[this.getTablePlayerNumber()];
		int dang_ju_score[] = new int[this.getTablePlayerNumber()];
		int jia_fa_socre[] = new int[this.getTablePlayerNumber()];
		int win_times[] = new int[this.getTablePlayerNumber()];
		int times = 1;
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		Arrays.fill(jia_fa_socre, 0);
		Arrays.fill(win_times, 1);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			times = cal_score_wsk(end_score, seat_index, win_times);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_end_score[i][this._cur_round - 1] += end_score[i];
				end_score[i] += this._xi_qian_score[i];
				_xi_qian_total_score[i] += _xi_qian_score[i];
				this._player_result.game_score[i] += end_score[i];
			}
		} else {
			times = 0;
		}
		this.load_player_info_data_game_end(game_end_wsk);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				game_end_wsk.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (GRR._cards_data[i][j] > 0x100) {
						cards_card.addItem(GRR._cards_data[i][j] - 0x100);
					} else {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end_wsk.addCardsData(i, cards_card);

			}

			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			game_end_wsk.setBankerPlayer(GRR._banker_player);
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				game_end_wsk.addCardsData(cards_card);
			}
			game_end_wsk.setBankerPlayer(GameConstants.INVALID_SEAT);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			if (end_score[i] > _max_end_score[i]) {
				_max_end_score[i] = end_score[i];
			}
			if (end_score[i] > 0) {
				this._win_num[i]++;
			} else if (end_score[i] < 0) {
				this._lose_num[i]++;
			}
			game_end.addGameScore(end_score[i]);
			game_end_wsk.addEndScore(end_score[i]);
			game_end_wsk.addRewardScore(this._xi_qian_score[i]);
			game_end_wsk.addXianQianScore(this._xi_qian_score[i]);
			game_end_wsk.addDangJuScore(dang_ju_score[i]);
			game_end_wsk.addZhuaFen(this._get_score[i]);

			boolean is_out_finish = false;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (_chuwan_shunxu[j] == i) {
					game_end_wsk.addWinOrder(j);
					is_out_finish = true;
					break;
				}
			}
			if (!is_out_finish) {
				game_end_wsk.addWinOrder(GameConstants.INVALID_SEAT);
			}

			if (this._is_yi_da_san) {
				game_end_wsk.setDuPai(win_times[i]);
				game_end_wsk.setTimes(0);
				if (GRR != null) {
					if (i == GRR._banker_player) {
						game_end_wsk.addFriend(1);
					} else {
						game_end_wsk.addFriend(2);
					}
				} else {
					game_end_wsk.addFriend(1);
				}

			} else {
				if (GRR != null && GRR._banker_player != GameConstants.INVALID_SEAT) {
					if (i == GRR._banker_player || i == _friend_seat[this.GRR._banker_player]) {
						game_end_wsk.addFriend(1);
					} else {
						game_end_wsk.addFriend(2);
					}
				} else {
					game_end_wsk.addFriend(1);
				}
				game_end_wsk.setTimes(times);
			}

			game_end_wsk.addGameCell((int) this.game_cell);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_wsk.addBankerTimes(this._du_num[i]);
					game_end_wsk.addTouYouTimes(this._tou_num[i]);
					game_end_wsk.addQiXiTimes(this._qi_xi_num[i]);
					game_end_wsk.addEndScoreZhua(this._zhua_score_max[i]);
					game_end_wsk.addBaXiTimes(this._ba_xi_num[i]);
					game_end_wsk.addFiveTenKTimes(this._510_num[i]);
					game_end_wsk.addFourKingTimes(this._four_num[i]);

					Int32ArrayResponse.Builder win_lose_score = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < this._cur_round; j++) {
						win_lose_score.addItem(_end_score[i][j]);
					}
					game_end_wsk.addWinLoseScore(win_lose_score);
					game_end_wsk.addWinTimes(this._win_num[i]);
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
				game_end_wsk.addBankerTimes(this._du_num[i]);
				game_end_wsk.addTouYouTimes(this._tou_num[i]);
				game_end_wsk.addQiXiTimes(this._qi_xi_num[i]);
				game_end_wsk.addEndScoreZhua(this._zhua_score_max[i]);
				game_end_wsk.addBaXiTimes(this._ba_xi_num[i]);
				game_end_wsk.addFiveTenKTimes(this._510_num[i]);
				game_end_wsk.addFourKingTimes(this._four_num[i]);
				Int32ArrayResponse.Builder win_lose_score = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this._cur_round; j++) {
					win_lose_score.addItem(_end_score[i][j]);
				}
				game_end_wsk.addWinLoseScore(win_lose_score);
				game_end_wsk.addWinTimes(this._win_num[i]);
			}

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}

		game_end_wsk.setReason(real_reason);
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

		Arrays.fill(_xi_qian_score, 0);
		Arrays.fill(_get_score, 0);
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		_is_yi_da_san = false;
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
	public int cal_score_wsk(int end_score[], int win_seat_index, int win_times[]) {

		int score = (int) this.game_cell;
		int shang_you_score = 0;
		int xia_you_score = 0;
		int times = 1;
		if (!_is_yi_da_san) {
			int you_num = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_chuwan_shunxu[i] != GameConstants.INVALID_SEAT) {
					you_num++;
				}
			}
			if (you_num == 1) {
				times = 4;
				score *= times;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
						end_score[i] += score;
						win_times[i] = times;
					} else {
						end_score[i] -= score;
						win_times[i] = -times;
					}
				}
				return times;
			} else if (you_num == 2) {
				// 一二游同队
				if (_friend_seat[_chuwan_shunxu[0]] == _chuwan_shunxu[1]) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i != _chuwan_shunxu[0] && i != _chuwan_shunxu[1]) {
							xia_you_score += _get_score[i];
						}
					}
					if (xia_you_score == 0) {
						times = 4;
					} else {
						times = 2;
					}
					score *= times;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
							end_score[i] += score;
							win_times[i] = times;
						} else {
							end_score[i] -= score;
							win_times[i] = -times;
						}
					}
					return times;
				} else {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1]) {
							xia_you_score += _get_score[i];
						}
					}
					if (xia_you_score == 200) {
						times = 4;
						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
						return times;
					} else if (xia_you_score >= 105) {
						times = 1;
						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _friend_seat[_chuwan_shunxu[1]]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
						return times;
					} else if (shang_you_score == 200) {
						times = 4;
						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
						return times;
					} else if (shang_you_score >= 105) {
						times = 1;
						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _friend_seat[_chuwan_shunxu[0]]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
						return times;
					}
				}
			} else {
				// 一三游同队
				if (_friend_seat[_chuwan_shunxu[0]] == _chuwan_shunxu[2]) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] && i == _chuwan_shunxu[2]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1] || i == _chuwan_shunxu[3]) {
							xia_you_score += _get_score[i];
						}
					}
					if (xia_you_score <= 100) {
						if (xia_you_score == 0) {
							times = 4;
						} else {
							times = 1;
						}
						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _chuwan_shunxu[2]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
					} else {
						if (xia_you_score == 0) {
							times = 4;
						} else {
							times = 1;
						}
						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _chuwan_shunxu[2]) {
								end_score[i] -= score;
								win_times[i] = times;
							} else {
								end_score[i] += score;
								win_times[i] = -times;
							}
						}
					}

					return times;
				} else {
					// 一四游同队
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _chuwan_shunxu[3]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1] || i == _chuwan_shunxu[2]) {
							xia_you_score += _get_score[i];
						}
					}
					if (shang_you_score < 100) {
						if (shang_you_score == 0) {
							times = 4;
						} else {
							times = 1;
						}

						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[1] || i == _chuwan_shunxu[2]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
						return times;
					} else if (shang_you_score <= 200) {
						if (shang_you_score == 200) {
							times = 4;
						} else {
							times = 1;
						}

						score *= times;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (i == _chuwan_shunxu[0] || i == _chuwan_shunxu[3]) {
								end_score[i] += score;
								win_times[i] = times;
							} else {
								end_score[i] -= score;
								win_times[i] = -times;
							}
						}
						return times;
					}
				}
			}

		} else {
			times = 4;
			score *= times;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (win_seat_index == this.GRR._banker_player) {
					if (i == this.GRR._banker_player) {
						win_times[i] = times * (getTablePlayerNumber() - 1);
						end_score[i] += score * (getTablePlayerNumber() - 1);
					} else {
						win_times[i] = -times;
						end_score[i] -= score;
					}
				} else {
					if (i == this.GRR._banker_player) {
						win_times[i] = -times * (getTablePlayerNumber() - 1);
						end_score[i] -= score * (getTablePlayerNumber() - 1);
					} else {
						win_times[i] = times;
						end_score[i] = score;
					}
				}

			}

		}

		return times;
	}

	public void load_player_info_data_game_start(GameStart_Wsk_hsdy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

	public void load_player_info_data_reconnect(TableResponse_hsdy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

	public void load_player_info_data_game_end(PukeGameEndWsk_hsdy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player,
			boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_hsdy.Builder outcarddata = OutCardDataWsk_hsdy.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_OUT_CARD);// 201
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
				if (is_deal) {
					outcarddata.setIsHaveNotCard(1);
				} else {
					outcarddata.setIsHaveNotCard(0);
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (this._is_yi_da_san || _out_card_ming_ji == GameConstants.INVALID_CARD) {
						if (i == index) {
							for (int j = 0; j < this.GRR._card_count[i]; j++) {
								cards_card.addItem(this.GRR._cards_data[i][j]);
							}
						}
					} else {
						if (i == index || (this._friend_seat[index] == i && GRR._card_count[index] == 0)) {
							for (int j = 0; j < this.GRR._card_count[i]; j++) {
								cards_card.addItem(this.GRR._cards_data[i][j]);
							}
						}
					}
					outcarddata.addHandCardsData(cards_card);
					if (!has_rule(GameConstants.GAME_RULE_HSDY_DISPLAY_CARD) && i != index) {
						outcarddata.addHandCardCount(-1);
					} else {
						outcarddata.addHandCardCount(this.GRR._card_count[i]);
					}

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
				// if (!this._is_yi_da_san && _out_card_ming_ji !=
				// GameConstants.INVALID_CARD) {
				// if (GRR._card_count[index] == 0) {
				// Int32ArrayResponse.Builder cards_card =
				// Int32ArrayResponse.newBuilder();
				// outcarddata.setHandCardsData(_friend_seat[index],
				// cards_card);
				// for (int j = 0; j <
				// this.GRR._card_count[_friend_seat[index]]; j++) {
				// cards_card.addItem(this.GRR._cards_data[_friend_seat[index]][j]);
				// }
				// outcarddata.setHandCardsData(index, cards_card);
				// outcarddata.setFriendSeatIndex(_friend_seat[index]);
				// } else {
				// outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);
				// }
				// } else {
				// outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);
				// }
				if (this._turn_out_card_count == 0 || this._logic.search_out_card(GRR._cards_data[index],
						GRR._card_count[index], this._turn_out_card_data, this._turn_out_card_count) > 0) {
					for (int j = 0; j < this.GRR._card_count[index]; j++) {
						outcarddata.addUserCanOutData(GRR._cards_data[index][j]);
					}
					outcarddata.setUserCanOutCount(GRR._card_count[index]);
				} else {
					outcarddata.setUserCanOutCount(0);
				}

				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_hsdy.Builder outcarddata = OutCardDataWsk_hsdy.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_OUT_CARD);// 201
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
			if (_current_player != GameConstants.INVALID_SEAT) {
				if (this.GRR._card_count[_current_player] == 0) {
					outcarddata.setIsHaveNotCard(1);
				} else {
					outcarddata.setIsHaveNotCard(0);
				}
			} else {
				outcarddata.setIsHaveNotCard(0);
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
			OutCardDataWsk_hsdy.Builder outcarddata = OutCardDataWsk_hsdy.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_OUT_CARD);// 201
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
			if (is_deal) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this._is_yi_da_san || _out_card_ming_ji == GameConstants.INVALID_CARD) {
					if (i == to_player) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					}
				} else {
					if (i == to_player || (this._friend_seat[to_player] == i && GRR._card_count[to_player] == 0)) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					}
				}
				outcarddata.addHandCardsData(cards_card);
				if (!has_rule(GameConstants.GAME_RULE_HSDY_DISPLAY_CARD) && i != to_player) {
					outcarddata.addHandCardCount(-1);
				} else {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				}

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
			// if (!this._is_yi_da_san && _out_card_ming_ji !=
			// GameConstants.INVALID_CARD) {
			// if (GRR._card_count[to_player] == 0) {
			// Int32ArrayResponse.Builder cards_card =
			// Int32ArrayResponse.newBuilder();
			// outcarddata.setHandCardsData(_friend_seat[to_player],
			// cards_card);
			// for (int j = 0; j <
			// this.GRR._card_count[_friend_seat[to_player]]; j++) {
			// cards_card.addItem(this.GRR._cards_data[_friend_seat[to_player]][j]);
			// }
			// outcarddata.setHandCardsData(to_player, cards_card);
			// outcarddata.setFriendSeatIndex(_friend_seat[to_player]);
			// } else {
			// outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);
			// }
			// } else {
			// outcarddata.setFriendSeatIndex(GameConstants.INVALID_SEAT);
			// }
			if (this._turn_out_card_count == 0 || this._logic.search_out_card(GRR._cards_data[to_player],
					GRR._card_count[to_player], this._turn_out_card_data, this._turn_out_card_count) > 0) {
				for (int j = 0; j < this.GRR._card_count[to_player]; j++) {
					outcarddata.addUserCanOutData(GRR._cards_data[to_player][j]);
				}
				outcarddata.setUserCanOutCount(GRR._card_count[to_player]);
			} else {
				outcarddata.setUserCanOutCount(0);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_HSDY_OPERATE) {
			Opreate_RequestWsk_hsdy req = PBUtil.toObject(room_rq, Opreate_RequestWsk_hsdy.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getCardData(), req.getSortCardList());
		}

		return true;
	}

	public void send_call_banker_result(int seat_index, int cur_player, int call_action) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_CALL_BANKER_RESULT);
		// 发送数据
		CallBankerResponse_hsdy.Builder callbanker_result = CallBankerResponse_hsdy.newBuilder();
		callbanker_result.setBankerPlayer(GRR._banker_player);
		callbanker_result.setOpreateAction(call_action);
		callbanker_result.setCallPlayer(seat_index);
		callbanker_result.setCurrentPlayer(cur_player);
		callbanker_result.setDisplayTime(10);
		callbanker_result.setRoomInfo(getRoomInfo());
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
		// 自己才有牌数据
		send_response_to_room(roomResponse);

		GRR.add_room_response(roomResponse);
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int card_data, List<Integer> list) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_LIANG_PAI: {
			// deal_liang_pai(seat_index, card_data);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER: {
			this._handler_call_banker.handler_call_banker(this, seat_index, 1);
			return true;
		}
		case GAME_OPREATE_TYPE_CALL_BANKER_NO: {
			this._handler_call_banker.handler_call_banker(this, seat_index, 0);
			return true;
		}
		case GAME_OPREATE_TYPE_SORT_BY_CARD: {
			deal_sort_card_by_data(seat_index, list);
			return true;
		}
		}
		return true;
	}

	public void deal_sort_card_by_data(int seat_index, List<Integer> list) {
		if (GRR == null) {
			return;
		}
		int out_cards[] = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			out_cards[i] = list.get(i);
		}
		if (_score_type[seat_index] == GameConstants.WSK_ST_ORDER) {
			_score_type[seat_index] = GameConstants.WSK_ST_COUNT;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_score_type[seat_index] = GameConstants.WSK_ST_VALUE;
		} else if (_score_type[seat_index] == GameConstants.WSK_ST_VALUE) {
			_score_type[seat_index] = GameConstants.WSK_ST_ORDER;
		}
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_count(int seat_index) {

		if (GRR == null) {
			return;
		}
		if (_score_type[seat_index] == GameConstants.WSK_ST_COUNT) {
			_score_type[seat_index] = GameConstants.WSK_ST_ORDER;
		} else {
			_score_type[seat_index] = GameConstants.WSK_ST_COUNT;
		}
		for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] > 0x100) {
				GRR._cards_data[seat_index][j] -= 0x100;
			}
		}
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_510K(int seat_index) {
		if (GRR == null) {
			return;
		}
		_score_type[seat_index] = GameConstants.WSK_ST_510K;
		for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] > 0x100) {
				GRR._cards_data[seat_index][j] -= 0x100;
			}
		}
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type[seat_index]);
		RefreshCard(seat_index);
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_REFRESH_CARD);
		// 发送数据
		RefreshCardData_hsdy.Builder refresh_card = RefreshCardData_hsdy.newBuilder();
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

	public void deal_liang_pai(int seat_index, int card_data) {
		// if (this._game_status != GameConstants.GS_GFWSK_LIANG_PAI ||
		// seat_index != this.GRR._banker_player) {
		// return;
		// }

		int other_seat_index = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (card_data == this.GRR._cards_data[i][j]) {
					other_seat_index = i;
					break;
				}
			}
			if (other_seat_index != GameConstants.INVALID_SEAT) {
				break;
			}
		}

		if (other_seat_index == GameConstants.INVALID_SEAT) {
			this.send_error_notify(seat_index, 2, "请选择正确的牌");
			return;
		} else {
			// 不能叫大小王
			if (_logic.GetCardColor(card_data) == 0x40) {
				send_error_notify(seat_index, 2, "请选择正确的牌型!");
				return;
			}
		}

		_jiao_pai_card = card_data;

		// 保存搭档信息
		_friend_seat[seat_index] = other_seat_index;
		_friend_seat[other_seat_index] = seat_index;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i != seat_index && i != other_seat_index) {
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (j != seat_index && j != other_seat_index && i != j) {
						_friend_seat[i] = j;
					}
				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_hsdy.Builder liang_pai_result = LiangPai_Result_hsdy.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(card_data);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		this._game_status = GameConstants.GS_GFWSK_PLAY;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
				GameConstants.INVALID_SEAT, false);

	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
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

	@Override
	public void Refresh_pai_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_PAI_SCORE);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		PaiFenData_hsdy.Builder pai_score_data = PaiFenData_hsdy.newBuilder();

		int count = 0;
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		for (int index = 3; index >= 0; index--) {
			// 5
			count = 0;
			cards.clear();
			for (int i = 0; i < this._pai_score_count; i++) {
				if (this._logic.GetCardColor(_pai_score_card[i]) == index) {
					if (this._logic.GetCardValue(_pai_score_card[i]) == 5) {
						cards.addItem(_pai_score_card[i]);
						count++;
					}
				}
			}
			pai_score_data.addCardsData(cards);
			pai_score_data.addCardsCount(count);
			// 10
			count = 0;
			cards.clear();
			for (int i = 0; i < this._pai_score_count; i++) {
				if (this._logic.GetCardColor(_pai_score_card[i]) == index) {
					if (this._logic.GetCardValue(_pai_score_card[i]) == 10) {
						cards.addItem(_pai_score_card[i]);
						count++;
					}
				}
			}
			pai_score_data.addCardsData(cards);
			pai_score_data.addCardsCount(count);
			// K
			count = 0;
			cards.clear();
			for (int i = 0; i < this._pai_score_count; i++) {
				if (this._logic.GetCardColor(_pai_score_card[i]) == index) {
					if (this._logic.GetCardValue(_pai_score_card[i]) == 13) {
						cards.addItem(_pai_score_card[i]);
						count++;
					}
				}
			}
			pai_score_data.addCardsData(cards);
			pai_score_data.addCardsCount(count);
		}

		pai_score_data.setYuScore(_pai_score);
		roomResponse.setCommResponse(PBUtil.toByteString(pai_score_data));

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	public void Refresh_Record_Out_Card(int to_player) {
		if (has_rule(GameConstants.GAME_RULE_HSDY_RECORD_OUT_CARD_NO)) {
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_RECORD_OUT_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		record_out_card_hsdy.Builder record = record_out_card_hsdy.newBuilder();
		tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
		this._logic.AnalysebCardDataToIndex(_repertory_card, _all_card_count, card_index);
		int count = 0;
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		for (int index = GameConstants.WSK_MAX_INDEX - 1; index >= 0; index--) {
			count = 0;
			cards.clear();
			for (int i = 0; i < card_index.card_index[index]; i++) {
				cards.addItem(card_index.card_data[index][i]);
				count++;
			}
			record.addCardsData(cards);
			record.addCardsCount(card_index.card_index[index]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(record));

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
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
