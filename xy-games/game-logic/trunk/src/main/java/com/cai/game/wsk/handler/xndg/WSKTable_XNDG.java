package com.cai.game.wsk.handler.xndg;

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
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKGameLogic_XNDG;
import com.cai.game.wsk.WSKType;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gfWsk.gfWskRsp.LiangPai_Result_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TouXiang_Anser_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TouXiang_Result_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.UserCardData;
import protobuf.clazz.xndg.XndgRsp.GameStart_Wsk_xndg;
import protobuf.clazz.xndg.XndgRsp.Opreate_RequestWsk_xndg;
import protobuf.clazz.xndg.XndgRsp.OutCardDataWsk_xndg;
import protobuf.clazz.xndg.XndgRsp.PukeGameEndWsk_xndg;
import protobuf.clazz.xndg.XndgRsp.RefreshCardData_xndg;
import protobuf.clazz.xndg.XndgRsp.RefreshMingPai;
import protobuf.clazz.xndg.XndgRsp.RefreshScore_xndg;
import protobuf.clazz.xndg.XndgRsp.TableResponse_xndg;

public class WSKTable_XNDG extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	public int _get_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public boolean _is_yi_da_san;
	public int _score_type[];
	public int _jiao_pai_card;
	public int _out_card_ming_ji;
	public int _lose_num[];
	public int _win_num[];
	public int _tou_num[];
	public int _du_num[];
	public int _qi_xi_num[];
	public int _ba_xi_num[];
	public int _510_num[];
	public int _four_num[];
	public int _end_score[];
	public int _zhua_score_max[];
	public List<UserCardData.Builder> _user_data;
	protected static final int GAME_OPREATE_TYPE_LIANG_PAI = 1;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 2;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER_NO = 3;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_CARD = 4;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_COUNT = 5;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_510K = 6;
	private boolean is_touxiang;

	public WSKTable_XNDG() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_XNDG();
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
		_end_score = new int[getTablePlayerNumber()];
		_turn_real_card_data = new int[get_hand_card_count_max()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_user_data = new ArrayList<>();
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = new int[getTablePlayerNumber()];
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_XNDG();
		_handler_call_banker = new WSKHandlerCallBnakerXNDG();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = i;
			_max_end_score[i] = 0;
			_user_data.add(UserCardData.newBuilder());
			_tou_num[i] = 0;
			_du_num[i] = 0;
			_qi_xi_num[i] = 0;
			_ba_xi_num[i] = 0;
			_end_score[i] = 0;
			_four_num[i] = 0;
			_510_num[i] = 0;
			_zhua_score_max[i] = 0;
			_score_type[i] = GameConstants.WSK_ST_ORDER;
		}
	}

	public void Refresh_Ming_Pai(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshMingPai.Builder refresh_ming_pai = RefreshMingPai.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_REFRESH_MING_PAI);

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
			_is_tou_xiang[i] = 0;
			_xi_qian_score[i] = 0;
			_is_call_banker[i] = 0;
			_xi_qian_times[i] = 0;

			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
			_user_data.get(i).clear();
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
		shuffle(_repertory_card, GameConstants.CARD_DATA_WSK);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE) % GameConstants.CARD_COUNT_WSK];
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

		int cards[] = new int[] { 19, 33, 78, 35, 2, 49, 79, 20, 5, 17, 44, 59, 39, 36, 8, 60, 1, 26, 52, 42, 29, 22, 43, 56, 21, 13, 2, 23, 20, 28,
				3, 11, 24, 53, 60, 24, 51, 22, 10, 8, 59, 79, 54, 58, 58, 42, 43, 40, 11, 53, 18, 39, 34, 5, 10, 17, 27, 78, 61, 51, 44, 56, 33, 57,
				52, 18, 13, 6, 26, 3, 7, 23, 29, 34, 55, 49, 55, 21, 54, 37, 12, 28, 27, 38, 57, 36, 40, 6, 50, 1, 7, 4, 45, 0x15, 0x1a, 0x1d, 0x05,
				0x0a, 0x0d, 0x15, 0x1a, 0x1d, 0x05, 0x0a, 0x0d, 0x15, 0x1a, 0x1d };
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
				GRR._cards_data[i][j] = cards[index++];

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
		GRR._banker_player = _cur_banker;
		this._current_player = GRR._banker_player;

		// 喜钱
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int xian_qian_score = _logic.GetHandCardXianScore(GRR._cards_data[i], GRR._card_count[i]);
			if (xian_qian_score > 0) {
				_xi_qian_score[i] += xian_qian_score * (getTablePlayerNumber() - 1);

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (i == j) {
						continue;
					}
					_xi_qian_score[j] -= xian_qian_score;
				}
			}
			int ba_xi_score = this._logic.get_card_num_count(GRR._cards_data[i], GRR._card_count[i], 8) * 2;
			int qi_xi_score = this._logic.get_card_num_count(GRR._cards_data[i], GRR._card_count[i], 7);
			int wang_score = 0;
			int five_ten_K_score = this._logic.Get_510K_Count(GRR._cards_data[i], GRR._card_count[i]);
			this._ba_xi_num[i] += ba_xi_score * (getTablePlayerNumber() - 1);
			this._qi_xi_num[i] += qi_xi_score * (getTablePlayerNumber() - 1);
			if (this._logic.Get_Wang_Count(GRR._cards_data[i], GRR._card_count[i]) == 4) {
				wang_score = 1;
			}
			this._four_num[i] += wang_score * (getTablePlayerNumber() - 1);
			this._510_num[i] += five_ten_K_score * (getTablePlayerNumber() - 1);

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (i == j) {
					continue;
				}
				_ba_xi_num[j] -= ba_xi_score;
				_qi_xi_num[j] -= qi_xi_score;
				_four_num[j] -= wang_score;
				_510_num[j] -= five_ten_K_score;
			}
		}

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Wsk_xndg.Builder gamestart = GameStart_Wsk_xndg.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			this.load_player_info_data_game_start(gamestart);

			gamestart.setCurBanker(GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				if (has_rule(GameConstants.GAME_RULE_XNDG_DISPLAY_CARD) || play_index == i) {
					gamestart.addCardCount(GRR._card_count[i]);
				} else {
					gamestart.addCardCount(-1);
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
		GameStart_Wsk_xndg.Builder gamestart = GameStart_Wsk_xndg.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		this._current_player = GRR._banker_player;
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

		Refresh_user_get_score(GameConstants.INVALID_SEAT);

		return true;
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

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
			if (_get_score[i] > _zhua_score_max[i]) {
				_zhua_score_max[i] = _get_score[i];
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_xndg.Builder game_end_wsk = PukeGameEndWsk_xndg.newBuilder();
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
				_end_score[i] += end_score[i];
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
			game_end_wsk.addWinOrder(_chuwan_shunxu[i]);
			if (this._is_yi_da_san) {
				game_end_wsk.addDuPai(win_times[i]);
				game_end_wsk.addBanLong(0);
				game_end_wsk.addHeiLong(0);
				game_end_wsk.addLong(0);
				game_end_wsk.addDraw(0);
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
				if (GRR != null) {
					if (i == GRR._banker_player || i == _friend_seat[this.GRR._banker_player]) {
						game_end_wsk.addFriend(1);
					} else {
						game_end_wsk.addFriend(2);
					}
				} else {
					game_end_wsk.addFriend(1);
				}

				if (times == 1) {
					game_end_wsk.addDuPai(0);
					game_end_wsk.addBanLong(win_times[i]);
					game_end_wsk.addHeiLong(0);
					game_end_wsk.addLong(0);
					game_end_wsk.addDraw(0);
				} else if (times == 2) {
					game_end_wsk.addDuPai(0);
					game_end_wsk.addBanLong(0);
					game_end_wsk.addHeiLong(0);
					game_end_wsk.addLong(win_times[i]);
					game_end_wsk.addDraw(0);
				} else if (times == 0) {
					game_end_wsk.addDuPai(0);
					game_end_wsk.addBanLong(0);
					game_end_wsk.addHeiLong(0);
					game_end_wsk.addLong(0);
					game_end_wsk.addDraw(1);
				} else {
					game_end_wsk.addDuPai(0);
					game_end_wsk.addBanLong(0);
					game_end_wsk.addHeiLong(win_times[i]);
					game_end_wsk.addLong(0);
					game_end_wsk.addDraw(0);
				}
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
					game_end_wsk.addWinLoseScore(this._end_score[i]);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
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
				game_end_wsk.addWinLoseScore(this._end_score[i]);
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
						times = 2;
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
						times = 2;
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
				if (_friend_seat[_chuwan_shunxu[0]] == _chuwan_shunxu[2]) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] && i == _chuwan_shunxu[2]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1]) {
							xia_you_score += _get_score[i];
						}
					}
					if (xia_you_score <= 100) {
						if (xia_you_score == 0) {
							times = 2;
						} else if (xia_you_score < 100) {
							times = 1;
						} else if (xia_you_score == 100) {
							times = 0;
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
						if (xia_you_score == 200) {
							times = 2;
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
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _chuwan_shunxu[0] || i == _chuwan_shunxu[3]) {
							shang_you_score += _get_score[i];
						}
						if (i == _chuwan_shunxu[1] || i == _chuwan_shunxu[2]) {
							xia_you_score += _get_score[i];
						}
					}
					// if (shang_you_score == 0) {
					// times = 2;
					// score *= times;
					// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					// if (i == _chuwan_shunxu[1] || i == _chuwan_shunxu[2]) {
					// end_score[i] += score;
					// win_times[i] = times;
					// } else {
					// end_score[i] -= score;
					// win_times[i] = -times;
					// }
					// }
					// return times;
					// }
					if (shang_you_score < 100) {
						times = 1;
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
					}
					if (shang_you_score <= 200) {
						times = 1;
						if (shang_you_score == 100) {
							times = 0;
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

	public void load_player_info_data_game_start(GameStart_Wsk_xndg.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_xndg.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndWsk_xndg.Builder roomResponse) {
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)

		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_xndg.Builder outcarddata = OutCardDataWsk_xndg.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_OUT_CARD);// 201
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
				if (has_rule(GameConstants.GAME_RULE_XNDG_DISPLAY_CARD) || index == i) {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else {
					outcarddata.addHandCardCount(-1);
				}

				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(index, roomResponse);

		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataWsk_xndg.Builder outcarddata = OutCardDataWsk_xndg.newBuilder();
		// Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_OUT_CARD);// 201
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
			outcarddata.addWinOrder(this._chuwan_shunxu[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_XNDG_OPERATE) {
			Opreate_RequestWsk_xndg req = PBUtil.toObject(room_rq, Opreate_RequestWsk_xndg.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getCardData(), req.getSortCardList());
		}

		return true;
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
		case GAME_OPREATE_TYPE_SORT_BY_COUNT: {
			deal_sort_card_by_count(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_SORT_BY_510K: {
			deal_sort_card_by_count(seat_index);
			return true;
		}
		}
		return true;
	}

	public void deal_tou_xiang_request(int seat_index, int to_player) {
		if (this._game_status != GameConstants.GS_GFWSK_TOU_XIANG) {
			return;
		}
		if (_is_tou_xiang[seat_index] != 0) {
			return;
		}
		if (_is_yi_da_san) {
			if (seat_index == this.GRR._banker_player) {
				return;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || seat_index == i) {
					continue;
				}
				if (_is_tou_xiang[i] == 1) {
					this.send_error_notify(seat_index, 2, "等待搭档投降确认");
					return;
				}
			}
		} else if (_is_tou_xiang[_friend_seat[seat_index]] == 1) {
			this.send_error_notify(seat_index, 2, "等待搭档投降确认");
			return;
		}
		if (!_is_yi_da_san) {
			if (_tou_xiang_times[seat_index] >= 1) {
				int win_sort = GameConstants.INVALID_SEAT;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i != seat_index && i != _friend_seat[seat_index]) {
						win_sort = i;
						break;
					}
				}

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
				// 发送数据
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
					if (i == seat_index || i == _friend_seat[seat_index]) {
						tou_xiang_result.setOpreateSeatIndex(seat_index);
						tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name() + "强烈要求投降");
					} else {
						tou_xiang_result.setOpreateSeatIndex(seat_index);
						tou_xiang_result.setOpreateStr("对方已投降");
					}
					tou_xiang_result.setIsOkCancel(0);
					roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
					this.send_response_to_player(i, roomResponse);
				}

				GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_sort, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			} else {
				send_is_tou_xiang(seat_index, true, _friend_seat[seat_index]);
				send_is_tou_xiang(seat_index, true, seat_index);
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == this.GRR._banker_player) {
					continue;
				}
				send_is_tou_xiang(seat_index, true, i);
			}
		}
		_is_tou_xiang_agree[seat_index] = 1;
		_is_tou_xiang[seat_index] = 1;
		_tou_xiang_times[seat_index]++;
	}

	public void deal_request_no_tou_xiang_(int seat_index) {
		if (this._game_status != GameConstants.GS_GFWSK_TOU_XIANG) {
			return;
		}
		if (_is_yi_da_san) {
			if (seat_index == this.GRR._banker_player) {
				return;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || seat_index == i) {
					continue;
				}
				if (_is_tou_xiang[i] == 1) {
					this.send_error_notify(seat_index, 2, "等待搭档投降确认");
					return;
				}
			}
		} else if (_is_tou_xiang[_friend_seat[seat_index]] == 1) {
			this.send_error_notify(seat_index, 2, "等待搭档投降确认");
			return;
		}

		if (!_is_yi_da_san) {
			send_is_tou_xiang(seat_index, false, _friend_seat[seat_index]);
			send_is_tou_xiang(seat_index, false, seat_index);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == this.GRR._banker_player) {
					continue;
				}
				send_is_tou_xiang(seat_index, false, i);
				_is_tou_xiang[i] = 2;
			}

		}
		_is_tou_xiang[seat_index] = 2;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] < 2) {
				return;
			}
		}
		this._game_status = GameConstants.GS_GFWSK_PLAY;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);
	}

	public void deal_sort_card_by_data(int seat_index, List<Integer> list) {
		if (GRR == null) {
			return;
		}
		int out_cards[] = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			out_cards[i] = list.get(i);
		}
		for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] > 0x100) {
				GRR._cards_data[seat_index][j] -= 0x100;
			}
		}
		for (int i = 0; i < list.size(); i++) {
			for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
				if (GRR._cards_data[seat_index][j] == out_cards[i]) {
					GRR._cards_data[seat_index][j] += 0x100;
					break;
				}
			}
		}
		_score_type[seat_index] = GameConstants.WSK_ST_CUSTOM;
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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XNDG_REFRESH_CARD);
		// 发送数据
		RefreshCardData_xndg.Builder refresh_card = RefreshCardData_xndg.newBuilder();
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

	public void send_is_tou_xiang(int seat_index, boolean is_touxiang, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_ANSER);
		// 发送数据
		TouXiang_Anser_Wsk_GF.Builder tou_xiang_anser = TouXiang_Anser_Wsk_GF.newBuilder();
		tou_xiang_anser.setOpreateSeatIndex(seat_index);
		tou_xiang_anser.setOpreateStr("您的搭档[" + this.get_players()[seat_index].getNick_name() + "]请求投降，您是否同意投降(投降输两分)");
		tou_xiang_anser.setIsTouXiang(is_touxiang);
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_anser));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);
	}

	public void deal_tou_xiang_anser_agree(int seat_index) {
		// 广丰五十K一打三不能投降
		// if (_is_yi_da_san) {
		// if (seat_index == this.GRR._banker_player) {
		// return;
		// }
		//
		// boolean is_touxiang = true;
		// boolean is_have_request = false;
		// int tou_xiang_seat = GameConstants.INVALID_SEAT;
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// if (i == GRR._banker_player || seat_index == i) {
		// continue;
		// }
		// if (_is_tou_xiang[i] != 0) {
		// is_have_request = true;
		// tou_xiang_seat = i;
		// break;
		// }
		// }
		// if (!is_have_request) {
		// // 没有玩家进行投降请求
		// return;
		// }
		//
		// _is_tou_xiang[seat_index] = 1;
		// _is_tou_xiang_agree[seat_index] = 1;
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// if (i == GRR._banker_player || tou_xiang_seat == i) {
		// continue;
		// }
		// if (_is_tou_xiang_agree[i] != 1) {
		// is_touxiang = false;
		// break;
		// }
		// }
		//
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
		// // 发送数据
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// TouXiang_Result_Wsk_GF.Builder tou_xiang_result =
		// TouXiang_Result_Wsk_GF.newBuilder();
		// if (is_touxiang) {
		// if (i == GRR._banker_player) {
		// tou_xiang_result.setOpreateSeatIndex(seat_index);
		// tou_xiang_result.setOpreateStr("对方已投降");
		// }
		// } else {
		// if (i == GRR._banker_player) {
		// continue;
		// }
		// tou_xiang_result.setOpreateSeatIndex(seat_index);
		// tou_xiang_result.setRequestTouXiang(tou_xiang_seat);
		// tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name()
		// + "同意投降");
		// }
		// if (!"".equals(tou_xiang_result.getOpreateStr())) {
		// tou_xiang_result.setIsOkCancel(0);
		// roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
		// this.send_response_to_player(i, roomResponse);
		// }
		// }
		// if (is_touxiang) {
		// GameSchedule.put(new GameFinishRunnable(getRoom_id(),
		// GRR._banker_player, GameConstants.Game_End_NORMAL), 3,
		// TimeUnit.SECONDS);
		// }
		// return;
		// } else {
		if (_is_tou_xiang[this._friend_seat[seat_index]] != 1) {
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

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
			if (i == seat_index || i == _friend_seat[seat_index]) {
				tou_xiang_result.setOpreateSeatIndex(seat_index);
				tou_xiang_result.setOpreateStr("你和搭档都同意投降");
			} else {
				tou_xiang_result.setOpreateSeatIndex(seat_index);
				tou_xiang_result.setOpreateStr("对方已投降");
			}
			tou_xiang_result.setIsOkCancel(0);
			roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
			this.send_response_to_player(i, roomResponse);
		}

		is_touxiang = true;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_sort, GameConstants.Game_End_NORMAL), 1, TimeUnit.SECONDS);
		// }
	}

	public void deal_tou_xiang_anser_disagree(int seat_index) {
		if (_is_yi_da_san) {
			if (seat_index == this.GRR._banker_player) {
				return;
			}

			// boolean is_touxiang=true;
			boolean is_have_request = false;
			int tou_xiang_seat = GameConstants.INVALID_SEAT;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || seat_index == i) {
					continue;
				}
				if (_is_tou_xiang[i] != 0) {
					is_have_request = true;
					tou_xiang_seat = i;
					break;
				}
			}
			if (!is_have_request || tou_xiang_seat == seat_index) {
				// 没有玩家进行投降请求
				return;
			}

			_is_tou_xiang[tou_xiang_seat] = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_is_tou_xiang_agree[i] = 0;
				if (i == GRR._banker_player) {
					continue;
				}
				_is_tou_xiang[i] = 2;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
			// 发送数据
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
				if (i == GRR._banker_player) {
					continue;
				}
				tou_xiang_result.setOpreateSeatIndex(seat_index);
				tou_xiang_result.setRequestTouXiang(tou_xiang_seat);
				tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name() + "拒绝投降");
				tou_xiang_result.setIsOkCancel(0);
				roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
				this.send_response_to_player(i, roomResponse);
			}

			return;
		} else {
			if (_is_tou_xiang[this._friend_seat[seat_index]] != 1) {
				return;
			}

			_is_tou_xiang_agree[seat_index] = 0;
			_is_tou_xiang[this._friend_seat[seat_index]] = 0;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
			// 发送数据
			TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
			tou_xiang_result.setOpreateSeatIndex(seat_index);
			if (_tou_xiang_times[_friend_seat[seat_index]] == 1) {
				tou_xiang_result.setOpreateStr("你的搭档拒绝投降，你是否投降");
			}
			tou_xiang_result.setIsOkCancel(1);
			tou_xiang_result.setOpreateSeatIndex(seat_index);
			roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
			this.send_response_to_player(_friend_seat[seat_index], roomResponse);
		}

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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_Wsk_GF.Builder liang_pai_result = LiangPai_Result_Wsk_GF.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		liang_pai_result.setCardData(card_data);
		liang_pai_result.addSeatIndex(seat_index);
		liang_pai_result.addSeatIndex(other_seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		this._game_status = GameConstants.GS_GFWSK_PLAY;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);

	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
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
