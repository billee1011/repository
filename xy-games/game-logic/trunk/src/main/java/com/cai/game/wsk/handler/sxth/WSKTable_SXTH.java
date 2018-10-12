package com.cai.game.wsk.handler.sxth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKGameLogic_SXTH;
import com.cai.game.wsk.WSKType;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sxth.SxthRsp.GameStart_Wsk_Sxth;
import protobuf.clazz.sxth.SxthRsp.Opreate_RequestWsk_Sxth;
import protobuf.clazz.sxth.SxthRsp.OutCardDataWsk_Sxth;
import protobuf.clazz.sxth.SxthRsp.PukeGameEndWsk_Sxth;
import protobuf.clazz.sxth.SxthRsp.RefreshCardData_Sxth;
import protobuf.clazz.sxth.SxthRsp.RefreshScore_Sxth;
import protobuf.clazz.sxth.SxthRsp.Switch_Seat_Sxth;
import protobuf.clazz.sxth.SxthRsp.TableResponse_Sxth;
import protobuf.clazz.sxth.SxthRsp.TouXiang_Ask_Sxth;
import protobuf.clazz.sxth.SxthRsp.effect_type_sxth;

public class WSKTable_SXTH extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间
	protected static final int ID_TIMER_GAME_START = 2;// 开始时间
	protected static final int ID_TIMER_SWITCH_SEAT = 3;// 开始时间
	// 特效
	protected static final int EFFECT_SCORE = 1;// 分数
	protected static final int EFFECT_FOUR_KING = 2;// 四小龙王
	protected static final int EFFECT_SIX_KING = 3;// 六六大顺
	protected static final int EFFECT_EIGHT_KING = 4;// 八仙过海
	protected static final int EFFECT_TWELVE_GOD = 5;// 十二罗汉
	protected static final int EFFECT_TOU_XIANG_YIN_CANG = 6;// 投降按钮消失
	protected static final int EFFECT_FEI_JI = 7;// 废机
	protected static final int EFFECT_WAIT_FRIEND_TOUXIANG = 8;// 等待对家投降
	protected static final int EFFECT_TOU_XIANG_ANSER_YIN_CANG = 9;// 询问投降界面隐藏
	public int _get_score[];
	public int _flower_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public int _score_type[];
	public int _jiao_pai_card;
	public int _out_card_ming_ji;
	public int _end_score[];
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG = 1;// 投降
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_NO = 2;// 不投降
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_AGREE = 3;// 同意投降
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG_REJECT = 4;// 拒绝投降
	protected static final int GAME_OPREATE_TYPE_SORT = 5;// 排序
	private boolean is_touxiang;
	public WSKGameLogic_SXTH _logic;

	public WSKTable_SXTH() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_SXTH();
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
		_out_card_times = new int[getTablePlayerNumber()];
		_init_account_id = new long[getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = new int[getTablePlayerNumber()];
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_SXTH();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = i;
			_max_end_score[i] = 0;
			_end_score[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_CUSTOM;
		}

		if (this.has_rule(GameConstants.GAME_RULE_SXTH_CELL_TWO)) {
			game_cell = 2;
		} else if (has_rule(GameConstants.GAME_RULE_SXTH_CELL_THREE)) {
			game_cell = 3;
		} else if (has_rule(GameConstants.GAME_RULE_SXTH_CELL_FOUR)) {
			game_cell = 4;
		} else if (has_rule(GameConstants.GAME_RULE_SXTH_CELL_FIVE)) {
			game_cell = 5;
		} else {
			game_cell = 1;
		}

	}

	public void Refresh_user_get_score(int to_player, boolean is_dealy) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_Sxth.Builder refresh_user_getscore = RefreshScore_Sxth.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addHuaScore(this._flower_score[i]);
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addAwardScore(this._xi_qian_score[i]);
		}
		refresh_user_getscore.setIsDelay(is_dealy);
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
		// if(this.has_rule(GameConstants.GAME_RULE_DMZ_MODUI)){
		// shuffle_players();
		// this.operate_player_data();
		// }

		this._current_player = _cur_banker;
		this._turn_out_card_count = 0;

		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_is_tou_xiang_agree[i] = -1;
			_is_tou_xiang[i] = -1;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_xi_qian_times[i] = 0;
			_flower_score[i] = 0;
			_cur_out_car_type[i] = GameConstants.SXTH_CT_ERROR;
			_out_card_times[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_CUSTOM;
			this._friend_seat[i] = (i + 2) % this.getTablePlayerNumber();

			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}

		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_out_card_ming_ji = GameConstants.INVALID_CARD;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);

		if (this.has_rule(GameConstants.GAME_RULE_SXTH_WU_JI)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_SXTH - 8];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_SXTH_NO_KING);
		} else if (this.has_rule(GameConstants.GAME_RULE_SXTH_FOUR_JI)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_SXTH - 4];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_SXTH_FOUR_KING);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_SXTH];
			shuffle(_repertory_card, WSKConstants.CARD_DATA_SXTH_EIGHT_KING);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE)
				% GameConstants.CARD_COUNT_WSK];
		getLocationTip();

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		// 庄家选择
		this.progress_banker_select();

		schedule(ID_TIMER_GAME_START, SheduleArgs.newArgs(), 1000);

		return true;
	}

	private final void shuffle_players() {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_CHAGNE_SEAT);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		Switch_Seat_Sxth.Builder switch_seat = Switch_Seat_Sxth.newBuilder();
		Player rplayer;

		float old_game_score[] = new float[getTablePlayerNumber()];
		int old_card_data[][] = new int[getTablePlayerNumber()][this.get_hand_card_count_max()];
		int old_max_end_score[] = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			switch_seat.addPlayersOld(room_player);

			old_game_score[i] = _player_result.game_score[i];
			old_max_end_score[i] = _max_end_score[i];
			for (int j = 0; j < GRR._card_count[i]; j++) {
				old_card_data[i][j] = GRR._cards_data[i][j];
			}
		}

		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < this.get_players().length; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		// 重新排下位置
		for (int i = 0; i < this.get_players().length; i++) {
			if (i < pl.size()) {
				int origin_seat = pl.get(i).get_seat_index();
				int target_seat = i;
				_player_result.game_score[target_seat] = old_game_score[origin_seat];

				_max_end_score[target_seat] = old_max_end_score[origin_seat];

				// 换用户牌数据
				for (int j = 0; j < this.GRR._card_count[target_seat]; j++) {
					GRR._cards_data[target_seat][j] = old_card_data[origin_seat][j];
				}

				get_players()[i] = pl.get(i);
				get_players()[i].set_seat_index(i);
				if (getCreate_player() != null
						&& get_players()[i].getAccount_id() == getCreate_player().getAccount_id()) {
					// _cur_banker = i;
					getCreate_player().set_seat_index(i);
				}
			} else {
				get_players()[i] = null;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			switch_seat.addPlayersNew(room_player);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(switch_seat));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}

	@Override
	public int get_hand_card_count_max() {
		if (this.has_rule(GameConstants.GAME_RULE_SXTH_WU_JI)) {
			return (GameConstants.CARD_COUNT_SXTH - 8) / 4;
		} else if (has_rule(GameConstants.GAME_RULE_SXTH_FOUR_JI)) {
			return (GameConstants.CARD_COUNT_SXTH - 4) / 4;
		} else {
			return GameConstants.CARD_COUNT_SXTH / 4;
		}
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
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 8, 44, 5, 24, 27, 79, 1, 29, 19, 7, 9, 24, 36, 51, 8, 60, 59, 12, 1, 39, 13, 52, 50,
				21, 34, 41, 25, 19, 41, 45, 33, 42, 8, 61, 29, 28, 26, 51, 37, 61, 28, 35, 39, 13, 38, 54, 22, 9, 17,
				11, 5, 45, 54, 3, 33, 11, 41, 3, 33, 56, 6, 27, 23, 17, 10, 22, 22, 54, 3, 53, 20, 6, 1, 6, 38, 4, 53,
				25, 18, 38, 26, 49, 42, 21, 57, 9, 40, 20, 26, 24, 60, 58, 42, 52, 4, 40, 40, 58, 43, 53, 59, 7, 7, 18,
				57, 55, 37, 57, 12, 18, 60, 20, 56, 11, 2, 39, 34, 10, 27, 50, 36, 36, 21, 35, 43, 12, 17, 43, 13, 10,
				44, 28, 4, 35, 2, 61, 55, 5, 2, 23, 29, 51, 25, 23, 56, 52, 55, 34, 50, 19, 37, 49, 45, 58, 44, 49, 78,
				78, 59, 79, 79, 79, 78, 78, };
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

		_game_status = GameConstants.GS_SXTH_SEND_CARD;
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Wsk_Sxth.Builder gamestart = GameStart_Wsk_Sxth.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			this.load_player_info_data_game_start(gamestart);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				gamestart.addCardCount(GRR._card_count[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
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
				gamestart.addCardsData(cards_card);
			}
			gamestart.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_Wsk_Sxth.Builder gamestart = GameStart_Wsk_Sxth.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		this._current_player = GRR._banker_player;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
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
			gamestart.addCardsData(cards_card);
		}

		gamestart.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		GRR.add_room_response(roomResponse);

		Refresh_user_get_score(GameConstants.INVALID_SEAT, false);
		schedule(ID_TIMER_SEND_CARD, SheduleArgs.newArgs(), 3000);
		// 游戏计算开始奖励分
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			tagAnalyseIndexResult_WSK card_index = new tagAnalyseIndexResult_WSK();
			this._logic.AnalysebCardDataToIndex(GRR._cards_data[i], GRR._card_count[i], card_index);
			if (this.has_rule(GameConstants.GAME_RULE_SXTH_EIGHT_JI)) {
				if (card_index.card_index[14] == 4 && card_index.card_index[13] == 4) {
					_xi_qian_score[i] += 15;
					send_effect_type(i, EFFECT_EIGHT_KING, 0, GameConstants.INVALID_SEAT);
				} else if (card_index.card_index[14] >= 3 && card_index.card_index[13] >= 3) {
					_xi_qian_score[i] += 10;
					send_effect_type(i, EFFECT_SIX_KING, 0, GameConstants.INVALID_SEAT);
				} else if (card_index.card_index[14] == 4 || card_index.card_index[13] == 4) {
					_xi_qian_score[i] += 6;
					send_effect_type(i, EFFECT_FOUR_KING, 0, GameConstants.INVALID_SEAT);
				}
				for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
					if (card_index.card_index[j] == 12) {
						_xi_qian_score[i] += 20;
						send_effect_type(i, EFFECT_TWELVE_GOD, 0, GameConstants.INVALID_SEAT);
					}
				}
			} else {
				if (card_index.card_index[14] == 4 && card_index.card_index[13] == 4) {
					_xi_qian_score[i] += 15;
					send_effect_type(i, EFFECT_EIGHT_KING, 0, GameConstants.INVALID_SEAT);
				} else if (card_index.card_index[14] >= 3 && card_index.card_index[13] >= 3) {
					_xi_qian_score[i] += 10;
					send_effect_type(i, EFFECT_SIX_KING, 0, GameConstants.INVALID_SEAT);
				} else if (card_index.card_index[14] == 2 && card_index.card_index[13] == 2) {
					_xi_qian_score[i] += 6;
					send_effect_type(i, EFFECT_FOUR_KING, 0, GameConstants.INVALID_SEAT);
				}
				for (int j = 0; j < GameConstants.WSK_MAX_INDEX; j++) {
					if (card_index.card_index[j] == 12) {
						_xi_qian_score[i] += 20;
						send_effect_type(i, EFFECT_TWELVE_GOD, 0, GameConstants.INVALID_SEAT);
					}
				}
			}

		}
		return true;
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
		this.Refresh_user_get_score(GameConstants.INVALID_SEAT, false);
		this.set_handler(this._handler_out_card_operate);
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
			// 投降
			if (this.has_rule(GameConstants.GAME_RULE_SXTH_TOU_XIANG)) {
				_game_status = GameConstants.GS_SXTH_TOU_XIANG;
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_TOUXIANG_BEGIN);
				this.send_response_to_room(roomResponse);
				this.set_handler(this._handler_out_card_operate);
				this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.SXTH_CT_ERROR,
						GameConstants.INVALID_SEAT, false);
			} else {
				out_card_begin();
			}

			return;
		}
		case ID_TIMER_GAME_START: {
			if (has_rule(GameConstants.GAME_RULE_SXTH_CHANGE_SEAT)) {
				this.shuffle_players();
				schedule(ID_TIMER_SWITCH_SEAT, SheduleArgs.newArgs(), 3000);
			} else {
				on_game_start();
			}
			return;
		}
		case ID_TIMER_SWITCH_SEAT: {
			on_game_start();
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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_Sxth.Builder game_end_wsk = PukeGameEndWsk_Sxth.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		int score_type = -1;
		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {

			score_type = cal_score_wsk(end_score, seat_index);
		} else if (GRR != null) {
			get_hand_flower_score();
			get_hand_reward_score_touxiang();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > 0) {
				game_end_wsk.addFriend(1);
			} else {
				game_end_wsk.addFriend(2);
			}
		}
		// 单局分数结算
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				if (i != j && j != this._friend_seat[i]) {
					end_score[j] -= this._flower_score[i];
					end_score[i] += this._flower_score[i];
				}
				if (i != j) {
					end_score[j] -= this._xi_qian_score[i];
					end_score[i] += this._xi_qian_score[i];
				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += end_score[i];
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
		}
		this.load_player_info_data(roomResponse);
		this.operate_player_data();
		// 加载用户信息
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			game_end_wsk.addPlayers(room_player);
		}

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
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
			game_end_wsk.addRewardScore(this._xi_qian_score[i]);
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
			game_end_wsk.addFlowerScore(this._flower_score[i]);

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
			game_end_wsk.addAllEndScoreOther((int) this._player_result.game_score[i]);
		}
		// 大结算用户信息
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			game_end_wsk.addPlayersOther(room_player);
		}

		game_end_wsk.setScoreTyep(score_type);
		game_end_wsk.setGameCell((int) this.game_cell);
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
		Arrays.fill(_flower_score, 0);

		// 错误断言
		return false;
	}

	public void cacl_floser_score(int play_index) {

		tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
		this._logic.AnalysebCardDataToIndex(this.GRR._cards_data[play_index], this.GRR._card_count[play_index],
				hand_card_idnex);
		int five_num = 0;
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (hand_card_idnex.card_index[i] >= 5) {
				five_num++;
			}
		}
		// 炸弹分
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (hand_card_idnex.card_index[i] == 5
					&& hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] > 0) {
				int color_count = 0;
				boolean is_tong_hua = false;
				for (int color = 0; color < 4; color++) {
					color_count = 0;
					for (int y = 0; y < hand_card_idnex.card_index[i]; y++) {
						if (color == this._logic.GetCardColor(hand_card_idnex.card_data[i][y])) {
							color_count++;
						}
						if (color_count >= 3 && five_num > 1) {
							this._flower_score[play_index] += 1;
							hand_card_idnex.card_index[i] = 0;
							five_num--;
							is_tong_hua = true;
							break;
						}
					}
				}
				if (is_tong_hua) {
					continue;
				}
				this._flower_score[play_index] += hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
				hand_card_idnex.card_index[i] = 0;
				hand_card_idnex.card_index[13] = 0;
				hand_card_idnex.card_index[14] = 0;
				five_num--;
			}
		}
		// 炸弹分
		if (hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14] == 0) {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
				if (hand_card_idnex.card_index[i] == 6) {
					int color_count = 0;
					int tonghua_count = 0;
					for (int color = 0; color < 4; color++) {
						color_count = 0;
						for (int y = 0; y < hand_card_idnex.card_index[i]; y++) {
							if (color == this._logic.GetCardColor(hand_card_idnex.card_data[i][y])) {
								color_count++;
							}
							if (color_count >= 3) {
								tonghua_count++;
								break;
							}
						}
					}
					if (tonghua_count == 2) {
						this._flower_score[play_index] += 2;
					} else {
						this._flower_score[play_index] += (hand_card_idnex.card_index[i]
								+ hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14]) - 5;
						hand_card_idnex.card_index[13] = 0;
						hand_card_idnex.card_index[14] = 0;

					}
					hand_card_idnex.card_index[i] = 0;
					five_num--;
				} else if (hand_card_idnex.card_index[i] > 6) {
					this._flower_score[play_index] += (hand_card_idnex.card_index[i] + hand_card_idnex.card_index[13]
							+ hand_card_idnex.card_index[14]) - 5;
					hand_card_idnex.card_index[i] = 0;
					hand_card_idnex.card_index[13] = 0;
					hand_card_idnex.card_index[14] = 0;
					five_num--;
				}
			}
		} else {
			for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
				if (hand_card_idnex.card_index[i] == 6) {
					int color_count = 0;
					int tonghua_count = 0;
					for (int color = 0; color < 4; color++) {
						color_count = 0;
						for (int y = 0; y < hand_card_idnex.card_index[i]; y++) {
							if (color == this._logic.GetCardColor(hand_card_idnex.card_data[i][y])) {
								color_count++;
							}
							if (color_count >= 3) {
								tonghua_count++;
								break;
							}
						}
					}
					if (tonghua_count == 2 && five_num > 1) {
						this._flower_score[play_index] += 2;
					} else {
						this._flower_score[play_index] += (hand_card_idnex.card_index[i]
								+ hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14]) - 5;
						hand_card_idnex.card_index[13] = 0;
						hand_card_idnex.card_index[14] = 0;
					}
					hand_card_idnex.card_index[i] = 0;
					five_num--;
				} else if (hand_card_idnex.card_index[i] > 6) {
					this._flower_score[play_index] += (hand_card_idnex.card_index[i] + hand_card_idnex.card_index[13]
							+ hand_card_idnex.card_index[14]) - 5;
					hand_card_idnex.card_index[i] = 0;
					hand_card_idnex.card_index[13] = 0;
					hand_card_idnex.card_index[14] = 0;
					five_num--;
				}
			}
		}

		// 同花
		for (int i = 0; i < GameConstants.WSK_MAX_INDEX - 2; i++) {
			if (hand_card_idnex.card_index[i] >= 3) {
				int color_count = 0;
				boolean is_tong_hua = false;
				for (int color = 0; color < 4; color++) {
					color_count = 0;
					for (int y = 0; y < hand_card_idnex.card_index[i]; y++) {
						if (color == this._logic.GetCardColor(hand_card_idnex.card_data[i][y])) {
							color_count++;
						}
						if (color_count >= 3) {
							this._flower_score[play_index] += 1;
							is_tong_hua = true;
							break;
						}
					}
				}
			}
		}

	}

	public void get_hand_flower_score() {

		if (GRR == null) {
			return;
		}
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			if (this._chuwan_shunxu[0] != GameConstants.INVALID_SEAT
					&& this._chuwan_shunxu[1] != GameConstants.INVALID_SEAT
					&& this._chuwan_shunxu[0] == _friend_seat[_chuwan_shunxu[1]]) {
				if (play_index == _chuwan_shunxu[0] || play_index == _chuwan_shunxu[1]) {
					cacl_floser_score(play_index);
				}
			} else {
				cacl_floser_score(play_index);
			}

		}

	}

	public void get_hand_reward_score_touxiang() {
		if (has_rule(GameConstants.GAME_RULE_SXTH_SHAO_JI)) {
			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
				this._logic.AnalysebCardDataToIndex(this.GRR._cards_data[play_index], this.GRR._card_count[play_index],
						hand_card_idnex);
				// 烧机
				boolean have_boom = false;
				for (int i = 0; i < GameConstants.WSK_MAX_INDEX; i++) {
					if (hand_card_idnex.card_index[i] >= 5) {
						have_boom = true;
						break;
					}
				}
				if (!have_boom) {
					this._xi_qian_score[play_index] -= hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
				}
			}
		}

	}

	public void get_hand_reward_score() {
		if (has_rule(GameConstants.GAME_RULE_SXTH_SHAO_JI)) {
			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				tagAnalyseIndexResult_WSK hand_card_idnex = new tagAnalyseIndexResult_WSK();
				this._logic.AnalysebCardDataToIndex(this.GRR._cards_data[play_index], this.GRR._card_count[play_index],
						hand_card_idnex);

				this._xi_qian_score[play_index] -= hand_card_idnex.card_index[13] + hand_card_idnex.card_index[14];
			}
		}

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
			if (this._chuwan_shunxu[0] == _friend_seat[_chuwan_shunxu[1]]) {
				int one_total_score = this._get_score[_chuwan_shunxu[0]] + this._get_score[_chuwan_shunxu[1]];
				int two_total_score = this._get_score[_chuwan_shunxu[2]] + this._get_score[_chuwan_shunxu[3]];
				if (two_total_score == 0) {
					if (one_total_score == 300) {
						score_type = 3;
						times = 4;
					} else {
						score_type = 2;
						times = 3;
					}
				} else if (two_total_score < 150) {
					times = 2;
					score_type = 0;
				} else {

					if (one_total_score + 150 >= two_total_score - 150) {
						times = 1;
					} else {
						times = -1;
					}
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i != _chuwan_shunxu[0] && i != _chuwan_shunxu[1]) {
						end_score[i] -= score * times;
					} else {
						end_score[i] += score * times;
					}
				}

			} else if (this._chuwan_shunxu[0] == _friend_seat[_chuwan_shunxu[2]]) {
				// 一三游一伙
				int one_total_score = this._get_score[_chuwan_shunxu[0]] + this._get_score[_chuwan_shunxu[2]];
				int two_total_score = this._get_score[_chuwan_shunxu[1]] + this._get_score[_chuwan_shunxu[3]];
				if (two_total_score < 75) {
					score_type = 0;
					times = 2;
				} else {
					if (one_total_score + 75 >= two_total_score - 75) {
						times = 1;
					} else {
						times = -1;
					}
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i != _chuwan_shunxu[0] && i != _chuwan_shunxu[2]) {
						end_score[i] -= score * times;
					} else {
						end_score[i] += score * times;
					}
				}
			} else {
				// 一四游一伙
				int one_total_score = this._get_score[_chuwan_shunxu[0]] + this._get_score[_chuwan_shunxu[3]];
				int two_total_score = this._get_score[_chuwan_shunxu[1]] + this._get_score[_chuwan_shunxu[2]];
				score_type = 1;
				if (one_total_score >= two_total_score) {
					times = 1;
				} else {
					times = -1;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i != _chuwan_shunxu[0] && i != _chuwan_shunxu[3]) {
						end_score[i] -= score * times;
					} else {
						end_score[i] += score * times;
					}
				}
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != win_seat_index && i != this._friend_seat[win_seat_index]) {
					end_score[i] -= score;
				} else {
					end_score[i] += score;
				}
			}
		}

		return score_type;
	}

	public void load_player_info_data_game_start(GameStart_Wsk_Sxth.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_Sxth.Builder roomResponse) {
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
				OutCardDataWsk_Sxth.Builder outcarddata = OutCardDataWsk_Sxth.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_OUT_CARD);// 201
				roomResponse.setTarget(seat_index);

				tagAnalyseIndexResult_WSK out_card_index = new tagAnalyseIndexResult_WSK();
				if (count > 0) {
					this._logic.AnalysebCardDataToIndex(cards_data, count, out_card_index);
				}
				for (int j = 0; j < count; j++) {
					if (j == 0
							|| this._logic.GetCardValue(cards_data[j - 1]) != this._logic.GetCardValue(cards_data[j])) {
						int same_count = 1;
						for (int x = j + 1; x < count; x++) {
							if (this._logic.GetCardValue(cards_data[j]) == this._logic.GetCardValue(cards_data[x])) {
								same_count++;
							} else {
								break;
							}
						}
						outcarddata.addCardsData(cards_data[j] + same_count * 0x100);
					} else {
						outcarddata.addCardsData(cards_data[j]);
					}
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
						Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
						int card_type = this._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
						for (int j = 0; j < tip_out_count[i]; j++) {
							cards_card.addItem(tip_out_card[i][j]);
						}
						outcarddata.addUserCanOutType(card_type);
						outcarddata.addUserCanOutData(cards_card);
						outcarddata.addUserCanOutCount(tip_out_count[i]);

					}
				}

				// 炸弹数据
				int tip_boom_out_card[][] = new int[GRR._card_count[index] * 10][GRR._card_count[index]];
				int tip_boom_out_count[] = new int[GRR._card_count[index] * 10];
				int tip_boom_type_count = 0;
				tip_boom_type_count = this._logic.search_boom(GRR._cards_data[index], GRR._card_count[index],
						tip_boom_out_card, tip_boom_out_count, tip_boom_type_count);
				for (int i = 0; i < tip_boom_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_boom_out_count[i]; j++) {
						cards_card.addItem(tip_boom_out_card[i][j]);
					}
					outcarddata.addBoomCardData(cards_card);
					outcarddata.addBoomCardDataCount(tip_boom_out_count[i]);
				}
				// 510K数据
				int tip_510K_out_card[][] = new int[GRR._card_count[index] * 10][GRR._card_count[index]];
				int tip_510K_out_count[] = new int[GRR._card_count[index] * 10];
				int tip_510K_type_count = 0;
				tip_510K_type_count = this._logic.search_real_510k(GRR._cards_data[index], GRR._card_count[index],
						tip_510K_out_card, tip_510K_out_count, tip_510K_type_count);
				for (int i = 0; i < tip_510K_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_510K_out_count[i]; j++) {
						cards_card.addItem(tip_510K_out_card[i][j]);
					}
					outcarddata.addFiveTenKCardData(cards_card);
					outcarddata.addFiveTenKCardDataCount(tip_510K_out_count[i]);
				}
				// 510K数据
				int tip_510K_out_card_fu[][] = new int[GRR._card_count[index] * 10][GRR._card_count[index]];
				int tip_510K_out_count_fu[] = new int[GRR._card_count[index] * 10];
				int tip_510K_type_count_fu = 0;
				tip_510K_type_count_fu = this._logic.search_false_510k(GRR._cards_data[index], GRR._card_count[index],
						tip_510K_out_card_fu, tip_510K_out_count_fu, tip_510K_type_count_fu);

				for (int i = 0; i < tip_510K_type_count_fu; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_510K_out_count_fu[i]; j++) {
						cards_card.addItem(tip_510K_out_card_fu[i][j]);
					}
					outcarddata.addFiveTenKCardDataFu(cards_card);
					outcarddata.addFiveTenKCardDataCountFu(tip_510K_out_count_fu[i]);
				}
				// 同花数据
				int tip_tonghua_out_card[][] = new int[GRR._card_count[index] * 10][GRR._card_count[index]];
				int tip_tonghua_out_count[] = new int[GRR._card_count[index] * 10];
				int tip_tonghua_type_count = 0;
				tip_tonghua_type_count = this._logic.search_tong_hua(GRR._cards_data[index], GRR._card_count[index],
						tip_tonghua_out_card, tip_tonghua_out_count, tip_tonghua_type_count);
				for (int i = 0; i < tip_tonghua_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_tonghua_out_count[i]; j++) {
						cards_card.addItem(tip_tonghua_out_card[i][j]);
					}
					outcarddata.addTonghuaCardData(cards_card);
					outcarddata.addTonghuaCardDataCount(tip_tonghua_out_count[i]);
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (i == index) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							if (j == 0 || this._logic.GetCardValue(GRR._cards_data[i][j - 1]) != this._logic
									.GetCardValue(GRR._cards_data[i][j])) {
								int same_count = 1;
								for (int x = j + 1; x < this.GRR._card_count[i]; x++) {
									if (this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic
											.GetCardValue(GRR._cards_data[i][x])) {
										same_count++;
									} else {
										break;
									}
								}
								cards_card.addItem(GRR._cards_data[i][j] + same_count * 0x100);
							} else {
								cards_card.addItem(GRR._cards_data[i][j]);
							}
						}
					}

					outcarddata.addHandCardsData(cards_card);
					if (i == index || (this._friend_seat[index] == i && GRR._card_count[index] == 0)) {
						outcarddata.addHandCardCount(this.GRR._card_count[i]);
					} else if (this.GRR._card_count[i] > 15) {
						outcarddata.addHandCardCount(-1);
					} else if (this.GRR._card_count[i] <= 15 && GRR._card_count[i] > 0) {
						outcarddata.addHandCardCount(-2);
					} else {
						outcarddata.addHandCardCount(0);
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

				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_Sxth.Builder outcarddata = OutCardDataWsk_Sxth.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_OUT_CARD);// 201
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
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_Sxth.Builder outcarddata = OutCardDataWsk_Sxth.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			tagAnalyseIndexResult_WSK out_card_index = new tagAnalyseIndexResult_WSK();
			if (count > 0) {
				this._logic.AnalysebCardDataToIndex(cards_data, count, out_card_index);
			}
			for (int j = 0; j < count; j++) {
				if (j == 0 || this._logic.GetCardValue(cards_data[j - 1]) != this._logic.GetCardValue(cards_data[j])) {
					int same_count = 1;
					for (int x = j + 1; x < count; x++) {
						if (this._logic.GetCardValue(cards_data[j]) == this._logic.GetCardValue(cards_data[x])) {
							same_count++;
						} else {
							break;
						}
					}
					outcarddata.addCardsData(cards_data[j] + same_count * 0x100);
				} else {
					outcarddata.addCardsData(cards_data[j]);
				}
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
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					int card_type = this._logic.GetCardType(tip_out_card[i], tip_out_count[i]);
					for (int j = 0; j < tip_out_count[i]; j++) {
						cards_card.addItem(tip_out_card[i][j]);
					}
					outcarddata.addUserCanOutType(card_type);
					outcarddata.addUserCanOutData(cards_card);
					outcarddata.addUserCanOutCount(tip_out_count[i]);

				}
			}

			// 炸弹数据
			int tip_boom_out_card[][] = new int[GRR._card_count[to_player] * 10][GRR._card_count[to_player]];
			int tip_boom_out_count[] = new int[GRR._card_count[to_player] * 10];
			int tip_boom_type_count = 0;
			tip_boom_type_count = this._logic.search_boom(GRR._cards_data[to_player], GRR._card_count[to_player],
					tip_boom_out_card, tip_boom_out_count, tip_boom_type_count);

			for (int i = 0; i < tip_boom_type_count; i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < tip_boom_out_count[i]; j++) {
					cards_card.addItem(tip_boom_out_card[i][j]);
				}
				outcarddata.addBoomCardData(cards_card);
				outcarddata.addBoomCardDataCount(tip_boom_out_count[i]);
			}
			// 510K数据
			int tip_510K_out_card[][] = new int[GRR._card_count[to_player] * 10][GRR._card_count[to_player]];
			int tip_510K_out_count[] = new int[GRR._card_count[to_player] * 10];
			int tip_510K_type_count = 0;
			tip_510K_type_count = this._logic.search_real_510k(GRR._cards_data[to_player], GRR._card_count[to_player],
					tip_510K_out_card, tip_510K_out_count, tip_510K_type_count);
			for (int i = 0; i < tip_510K_type_count; i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < tip_510K_out_count[i]; j++) {
					cards_card.addItem(tip_510K_out_card[i][j]);
				}
				outcarddata.addFiveTenKCardData(cards_card);
				outcarddata.addFiveTenKCardDataCount(tip_510K_out_count[i]);
			}
			// 510K数据
			int tip_510K_out_card_fu[][] = new int[GRR._card_count[to_player] * 10][GRR._card_count[to_player]];
			int tip_510K_out_count_fu[] = new int[GRR._card_count[to_player] * 10];
			int tip_510K_type_count_fu = 0;
			tip_510K_type_count_fu = this._logic.search_false_510k(GRR._cards_data[to_player],
					GRR._card_count[to_player], tip_510K_out_card_fu, tip_510K_out_count_fu, tip_510K_type_count_fu);

			for (int i = 0; i < tip_510K_type_count_fu; i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < tip_510K_out_count_fu[i]; j++) {
					cards_card.addItem(tip_510K_out_card_fu[i][j]);
				}
				outcarddata.addFiveTenKCardDataFu(cards_card);
				outcarddata.addFiveTenKCardDataCountFu(tip_510K_out_count_fu[i]);
			}
			// 同花数据
			int tip_tonghua_out_card[][] = new int[GRR._card_count[to_player] * 10][GRR._card_count[to_player]];
			int tip_tonghua_out_count[] = new int[GRR._card_count[to_player] * 10];
			int tip_tonghua_type_count = 0;
			tip_tonghua_type_count = this._logic.search_tong_hua(GRR._cards_data[to_player], GRR._card_count[to_player],
					tip_tonghua_out_card, tip_tonghua_out_count, tip_tonghua_type_count);
			for (int i = 0; i < tip_tonghua_type_count; i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < tip_tonghua_out_count[i]; j++) {
					cards_card.addItem(tip_tonghua_out_card[i][j]);
				}
				outcarddata.addTonghuaCardData(cards_card);
				outcarddata.addTonghuaCardDataCount(tip_tonghua_out_count[i]);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == to_player) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						if (j == 0 || this._logic.GetCardValue(GRR._cards_data[i][j - 1]) != this._logic
								.GetCardValue(GRR._cards_data[i][j])) {
							int same_count = 1;
							for (int x = j + 1; x < this.GRR._card_count[i]; x++) {
								if (this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic
										.GetCardValue(GRR._cards_data[i][x])) {
									same_count++;
								} else {
									break;
								}
							}
							cards_card.addItem(GRR._cards_data[i][j] + same_count * 0x100);
						} else {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					}
				}

				outcarddata.addHandCardsData(cards_card);
				if (i == to_player || (this._friend_seat[to_player] == i && GRR._card_count[to_player] == 0)) {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else if (this.GRR._card_count[i] > 15) {
					outcarddata.addHandCardCount(-1);
				} else if (this.GRR._card_count[i] <= 15 && GRR._card_count[i] > 0) {
					outcarddata.addHandCardCount(-2);
				} else {
					outcarddata.addHandCardCount(0);
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

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_SXTH_OPERATE) {
			Opreate_RequestWsk_Sxth req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Sxth.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType());
		}

		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_TOU_XIANG: {
			this.deal_tou_xiang(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG_NO: {
			this.deal_tou_xiang_no(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG_AGREE: {
			this.deal_tou_xiang_anser_agree(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG_REJECT: {
			this.deal_tou_xiang_anser_reject(seat_index);
			return true;
		}
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

	public void send_tou_xiang_ask(int seat_index, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_TOUXIANG_ASK);
		// 发送数据
		TouXiang_Ask_Sxth.Builder tou_xiang_anser = TouXiang_Ask_Sxth.newBuilder();
		tou_xiang_anser.setSeatIndex(seat_index);
		tou_xiang_anser.setOpreateStr("您的搭档[" + this.get_players()[seat_index].getNick_name() + "]请求投降，您是否同意投降");
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_anser));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);
	}

	public void deal_tou_xiang(int seat_index) {
		if (_is_tou_xiang[this._friend_seat[seat_index]] == 1 || is_touxiang != false
				|| _game_status != GameConstants.GS_SXTH_TOU_XIANG) {
			return;
		}
		_is_tou_xiang[seat_index] = 1;
		send_tou_xiang_ask(seat_index, _friend_seat[seat_index]);

		send_effect_type(seat_index, EFFECT_TOU_XIANG_YIN_CANG, 0, seat_index);
		send_effect_type(this._friend_seat[seat_index], EFFECT_TOU_XIANG_YIN_CANG, 0, this._friend_seat[seat_index]);
		send_effect_type(seat_index, EFFECT_WAIT_FRIEND_TOUXIANG, 0, seat_index);
	}

	public void deal_tou_xiang_no(int seat_index) {
		if (_is_tou_xiang[this._friend_seat[seat_index]] == 1 || _game_status != GameConstants.GS_SXTH_TOU_XIANG) {
			return;
		}
		_is_tou_xiang[seat_index] = 0;
		send_effect_type(seat_index, EFFECT_TOU_XIANG_YIN_CANG, 0, seat_index);
		send_effect_type(seat_index, EFFECT_WAIT_FRIEND_TOUXIANG, 0, seat_index);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] != 0) {
				return;
			}
		}

		is_touxiang = false;

		this.out_card_begin();
	}

	public void deal_tou_xiang_anser_agree(int seat_index) {
		if (_is_tou_xiang[this._friend_seat[seat_index]] != 1 || _game_status != GameConstants.GS_SXTH_TOU_XIANG) {
			return;
		}

		if (GRR == null) {
			return;
		}
		_is_tou_xiang[seat_index] = 1;
		_is_tou_xiang_agree[seat_index] = 1;

		int win_sort = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i != seat_index && i != _friend_seat[seat_index]) {
				win_sort = i;
				break;
			}
		}
		get_hand_flower_score();
		get_hand_reward_score_touxiang();
		is_touxiang = true;
		_game_status = GameConstants.GAME_STATUS_WAIT;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_sort, GameConstants.Game_End_NORMAL), 1,
				TimeUnit.SECONDS);
		// }
	}

	public void deal_tou_xiang_anser_reject(int seat_index) {
		if (_is_tou_xiang[this._friend_seat[seat_index]] != 1 || _game_status != GameConstants.GS_SXTH_TOU_XIANG) {
			return;
		}

		_is_tou_xiang[seat_index] = 0;
		_is_tou_xiang[this._friend_seat[seat_index]] = 0;
		send_effect_type(seat_index, EFFECT_TOU_XIANG_ANSER_YIN_CANG, 0, seat_index);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] != 0) {
				return;
			}
		}
		is_touxiang = false;
		this.out_card_begin();

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

		if (_game_status == GameConstants.GS_SXTH_TOU_XIANG) {
			if (_is_tou_xiang[seat_index] == -1) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_SXTH_TOUXIANG_BEGIN);
				this.send_response_to_player(seat_index, roomResponse);
			}
			if (_is_tou_xiang[seat_index] == 1) {
				send_effect_type(seat_index, EFFECT_WAIT_FRIEND_TOUXIANG, 0, seat_index);
			}
			if (_is_tou_xiang[_friend_seat[seat_index]] == 1 && this._is_tou_xiang_agree[seat_index] == -1) {
				send_tou_xiang_ask(_friend_seat[seat_index], seat_index);
				send_effect_type(seat_index, EFFECT_TOU_XIANG_YIN_CANG, 0, seat_index);
			}
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.SXTH_CT_ERROR, seat_index, false);

		}
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
