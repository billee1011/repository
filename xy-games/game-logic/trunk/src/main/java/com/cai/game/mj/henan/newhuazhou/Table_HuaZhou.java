package com.cai.game.mj.henan.newhuazhou;

import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuaZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.FengKanUtil;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_HuaZhou extends AbstractMJTable {
	private static final long serialVersionUID = 2570006934151976528L;

	private int[] feng_kan; // 风坎数
	protected HandlerPao_HuaZhou _handler_pao;

	private int[] hu_score; // 胡牌分
	private int[] fen_score; // 风坎分
	private int[] que_score; // 缺门分
	private int[] shai_score; // 晒张分
	private int[] pao_score; // 跑分

	private int[] ming_gang_score; // 明杠分
	private int[] an_gang_score; // 暗杠分
	private int[] dian_gang_score; // 点杠分

	public boolean[] have_shaizhang;// 是否有晒张

	public void cal_ming_gang_score(int seat_index, int score) {
		ming_gang_score[seat_index] += score;
	}

	public void cal_an_gang_score(int seat_index, int score) {
		an_gang_score[seat_index] += score;
	}

	public void cal_dian_gang_score(int seat_index, int score) {
		dian_gang_score[seat_index] += score;
	}

	public int get_ming_gang_score(int seat_index) {
		return ming_gang_score[seat_index];
	}

	public int get_an_gang_score(int seat_index) {
		return an_gang_score[seat_index];
	}

	public int get_dian_gang_score(int seat_index) {
		return dian_gang_score[seat_index];
	}

	public void cal_hu_score(int seat_index, int score) {
		hu_score[seat_index] += score;
	}

	public void cal_fen_score(int seat_index, int score) {
		fen_score[seat_index] += score;
	}

	public void cal_que_score(int seat_index, int score) {
		que_score[seat_index] += score;
	}

	public void cal_shai_score(int seat_index, int score) {
		shai_score[seat_index] += score;
	}

	public void cal_pao_score(int seat_index, int score) {
		pao_score[seat_index] += score;
	}

	public int get_hu_score(int seat_index) {
		return hu_score[seat_index];
	}

	public int get_fen_score(int seat_index) {
		return fen_score[seat_index];
	}

	public int get_que_score(int seat_index) {
		return que_score[seat_index];
	}

	public int get_shai_score(int seat_index) {
		return shai_score[seat_index];
	}

	public int get_pao_score(int seat_index) {
		return pao_score[seat_index];
	}

	public Table_HuaZhou() {
		super(MJType.GAME_TYPE_HE_NAN_HUA_XIAN);
	}

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		feng_kan = new int[getTablePlayerNumber()];
		hu_score = new int[getTablePlayerNumber()];
		fen_score = new int[getTablePlayerNumber()];
		que_score = new int[getTablePlayerNumber()];
		shai_score = new int[getTablePlayerNumber()];
		pao_score = new int[getTablePlayerNumber()];
		ming_gang_score = new int[getTablePlayerNumber()];
		dian_gang_score = new int[getTablePlayerNumber()];
		an_gang_score = new int[getTablePlayerNumber()];
		have_shaizhang = new boolean[getTablePlayerNumber()];
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (!_logic.is_valid_card(cur_card)) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = 0;

		boolean can_win = false;
		boolean has_dong_feng_ling = has_rule(Constants_HuaZhou.GAME_RULE_DONG_FENG_LING);

		if (has_dong_feng_ling) { 
			can_win = AnalyseCardUtil.analyse_feng_chi_dfl_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		} else {
			can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		if (can_win == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 字牌数量
		int zi_count = 0;
		int dong_count = cards_index[GameConstants.MAX_ZI];
		int tmp_cur_index = _logic.switch_to_card_index(cur_card);
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_FENG; i++) {
			zi_count += cards_index[i];
		}

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;
		
		if(has_rule(Constants_HuaZhou.GAME_RULE_QUE_MEN_HU)) {
			if (!_logic.is_que_yi_se(tmp_cards_index, weaveItems, weave_count)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		int[] tmp_feng_kan = new int[2];
		FengKanUtil.getFengKanCount(tmp_cards_index, tmp_feng_kan, has_dong_feng_ling);

		if (card_type == Constants_HuaZhou.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuaZhou.CHR_ZI_MO);
		} else if (card_type == Constants_HuaZhou.HU_CARD_TYPE_JIE_PAO) {
			if (has_dong_feng_ling && (zi_count % 3 == 1) && (tmp_cur_index == GameConstants.MAX_ZI)) {
				if (zi_count / 3 == dong_count - 1) {
					chiHuRight.opr_or(Constants_HuaZhou.CHR_JIE_PAO);
				} else { 
					chiHuRight.set_empty();
					return GameConstants.WIK_NULL;
				}
			} else if (has_dong_feng_ling && (zi_count % 3 == 2) && (tmp_feng_kan[0] - 1 == dong_count)
					&& (tmp_cur_index == GameConstants.MAX_ZI)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			} else {
				chiHuRight.opr_or(Constants_HuaZhou.CHR_JIE_PAO);
			}
		}

		feng_kan[_seat_index] = tmp_feng_kan[0] + tmp_feng_kan[1];

		int colorCount = _logic.get_se_count(tmp_cards_index, weaveItems, weave_count);
		chiHuRight.duanmen_count = 3 - colorCount;

		return cbChiHuKind;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				// 抢杠胡按点炮计算
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_HuaZhou.HU_CARD_TYPE_JIE_PAO, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = Constants_HuaZhou.HU_CARD_TYPE_JIE_PAO;

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						card_type, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, Constants_HuaZhou.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HuaZhou();
		_handler_dispath_card = new HandlerDispatchCard_HuaZhou();
		_handler_gang = new HandlerGang_HuaZhou();
		_handler_out_card_operate = new HandlerOutCardOperate_HuaZhou();
		_handler_pao = new HandlerPao_HuaZhou();
	}

	@Override
	protected boolean on_game_start() {
		// 初始化数据
		onInitParam();
		if (has_rule(Constants_HuaZhou.GAME_RULE_XIA_PAO_THREE) || has_rule(Constants_HuaZhou.GAME_RULE_XIA_PAO_FIVE)) {
			set_handler(_handler_pao);
			_handler_pao.exe(this);
		} else {
			on_game_start_real();
		}

		return true;
	}

	protected boolean on_game_start_real() {
		// 晒张
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			have_shaizhang[i] = false;
		}
		if (has_rule(Constants_HuaZhou.GAME_RULE_SHAI_ZHANG)) {
			select_shai_zhang_card();
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();

			refresh_shai_zhang_card(i, GameConstants.MAX_COUNT - 1, hand_cards[i]);

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		setGameEndBasicPrama(game_end);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// TODO: 荒庄荒杠
				if (GRR._end_type != GameConstants.Game_End_DRAW) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];
						}
					}
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += get_hu_score(i) + get_fen_score(i) + get_que_score(i) + get_pao_score(i)
						+ get_shai_score(i);
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				// 刷新晒张
				refresh_shai_zhang_card(i, hand_card_count, cards);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < hand_card_count; j++) {
					cs.addItem(cards[j]);
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					if (is_shai_zhang_card(i, GRR._weave_items[i][j].center_card)) {
						weaveItem_item.setCenterCard(
								GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
					} else {
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					}
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
				GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		// TODO: 总分=（底分 + 风坎数 + 断门数 + 相互之间的跑分+晒张）*（是否自摸）+ （相互之间的杠分）
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				// 胡牌
				int nHuScore = 2 * GameConstants.CELL_SCORE;
				GRR._lost_fan_shu[i][seat_index] += nHuScore;
				// 风坎
				int nFenScore = feng_kan[seat_index] * 2;
				// 缺门
				int nQueScore = GRR._chi_hu_rights[seat_index].duanmen_count * 2;
				// 跑分
				int nPaoScore = _player_result.pao[seat_index] + _player_result.pao[i];
				// 晒张
				int nShaiScore = 0;
				int index = get_shai_zhang_index(seat_index);
				if (index >= 0 && index < GameConstants.MAX_ZI) {
					nShaiScore = 2;
				} else if (index >= GameConstants.MAX_ZI && index < GameConstants.MAX_ZI_FENG) {
					nShaiScore = 4;
				}
				cal_hu_score(seat_index, nHuScore);
				cal_fen_score(seat_index, nFenScore);
				cal_que_score(seat_index, nQueScore);
				cal_pao_score(seat_index, nPaoScore);
				cal_shai_score(seat_index, nShaiScore);
				cal_hu_score(i, 0 - nHuScore);
				cal_fen_score(i, 0 - nFenScore);
				cal_que_score(i, 0 - nQueScore);
				cal_pao_score(i, 0 - nPaoScore);
				cal_shai_score(i, 0 - nShaiScore);
			}
		} else {
			// 胡牌
			int nHuScore = 3 * GameConstants.CELL_SCORE;
			GRR._lost_fan_shu[provide_index][seat_index] += nHuScore;
			// 风坎
			int nFenScore = feng_kan[seat_index] * 1;
			// 缺门
			int nQueScore = GRR._chi_hu_rights[seat_index].duanmen_count * 1;
			// 跑分
			int nPaoScore = _player_result.pao[seat_index] + _player_result.pao[provide_index];
			// 晒张
			int nShaiScore = 0;
			int index = get_shai_zhang_index(seat_index);
			if (index >= 0 && index < GameConstants.MAX_ZI) {
				nShaiScore = 1;
			} else if (index >= GameConstants.MAX_ZI && index < GameConstants.MAX_ZI_FENG) {
				nShaiScore = 2;
			}
			cal_hu_score(seat_index, nHuScore);
			cal_fen_score(seat_index, nFenScore);
			cal_que_score(seat_index, nQueScore);
			cal_pao_score(seat_index, nPaoScore);
			cal_shai_score(seat_index, nShaiScore);
			cal_hu_score(provide_index, 0 - nHuScore);
			cal_fen_score(provide_index, 0 - nFenScore);
			cal_que_score(provide_index, 0 - nQueScore);
			cal_pao_score(provide_index, 0 - nPaoScore);
			cal_shai_score(provide_index, 0 - nShaiScore);

			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuaZhou.CHR_JIE_PAO).is_empty())
				GRR._chi_hu_rights[provide_index].opr_or(Constants_HuaZhou.CHR_FANG_PAO);
		}
	}

	public void process_chi_hu_player_score_duopao(int seat_index, int provide_index, int operate_card,
			boolean duopao) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		// TODO: 总分=（底分 + 风坎数 + 断门数 + 相互之间的跑分+晒张）*（是否自摸）+ （相互之间的杠分）
		// 单点
		if (!duopao) {
			// 胡牌
			int nHuScore = 3 * GameConstants.CELL_SCORE;
			GRR._lost_fan_shu[provide_index][seat_index] += nHuScore;
			// 风坎
			int nFenScore = feng_kan[seat_index] * 1;
			// 缺门
			int nQueScore = GRR._chi_hu_rights[seat_index].duanmen_count * 1;
			// 跑分
			int nPaoScore = _player_result.pao[seat_index] + _player_result.pao[provide_index];
			// 晒张
			int nShaiScore = 0;
			int index = get_shai_zhang_index(seat_index);
			if (index >= 0 && index < GameConstants.MAX_ZI) {
				nShaiScore = 1;
			} else if (index >= GameConstants.MAX_ZI && index < GameConstants.MAX_ZI_FENG) {
				nShaiScore = 2;
			}
			cal_hu_score(seat_index, nHuScore);
			cal_fen_score(seat_index, nFenScore);
			cal_que_score(seat_index, nQueScore);
			cal_pao_score(seat_index, nPaoScore);
			cal_shai_score(seat_index, nShaiScore);
			cal_hu_score(provide_index, 0 - nHuScore);
			cal_fen_score(provide_index, 0 - nFenScore);
			cal_que_score(provide_index, 0 - nQueScore);
			cal_pao_score(provide_index, 0 - nPaoScore);
			cal_shai_score(provide_index, 0 - nShaiScore);
		} else {
			// 多炮
			// 胡牌
			int nHuScore = 2 * GameConstants.CELL_SCORE;
			GRR._lost_fan_shu[provide_index][seat_index] += nHuScore;
			// 风坎
			int nFenScore = feng_kan[seat_index] * 1;
			// 缺门
			int nQueScore = GRR._chi_hu_rights[seat_index].duanmen_count * 1;
			// 跑分
			int nPaoScore = _player_result.pao[seat_index] + _player_result.pao[provide_index];
			// 晒张
			int nShaiScore = 0;
			int index = get_shai_zhang_index(seat_index);
			if (index >= 0 && index < GameConstants.MAX_ZI) {
				nShaiScore = 1;
			} else if (index >= GameConstants.MAX_ZI && index < GameConstants.MAX_ZI_FENG) {
				nShaiScore = 2;
			}
			cal_hu_score(seat_index, nHuScore);
			cal_fen_score(seat_index, nFenScore);
			cal_que_score(seat_index, nQueScore);
			cal_pao_score(seat_index, nPaoScore);
			cal_shai_score(seat_index, nShaiScore);
			cal_hu_score(provide_index, 0 - nHuScore);
			cal_fen_score(provide_index, 0 - nFenScore);
			cal_que_score(provide_index, 0 - nQueScore);
			cal_pao_score(provide_index, 0 - nPaoScore);
			cal_shai_score(provide_index, 0 - nShaiScore);
		}
		if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuaZhou.CHR_JIE_PAO).is_empty())
			GRR._chi_hu_rights[provide_index].opr_or(Constants_HuaZhou.CHR_FANG_PAO);
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(
					_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;

		// int[] jie_gang = new int[getTablePlayerNumber()];
		// int[] fang_gang = new int[getTablePlayerNumber()];
		// int[] an_gang = new int[getTablePlayerNumber()];

		if (GRR._end_type == GameConstants.Game_End_DRAW) {
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				GRR._result_des[player] = " ";
			}
			return;
		}

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder(" ");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_HuaZhou.CHR_ZI_MO) {
						result.append(" 自摸");
						if (get_hu_score(player) > 0)
							result.append("+" + get_hu_score(player) / (getTablePlayerNumber() - 1));

						if (get_que_score(player) > 0)
							result.append(" 缺门+" + get_que_score(player) / (getTablePlayerNumber() - 1));

						if (get_fen_score(player) > 0)
							result.append(" 风坎+" + get_fen_score(player) / (getTablePlayerNumber() - 1));

						if (has_rule(Constants_HuaZhou.GAME_RULE_SHAI_ZHANG)) {
							if (get_shai_score(player) > 0)
								result.append(" 晒张+" + get_shai_score(player) / (getTablePlayerNumber() - 1));
						}
					}

					if (type == Constants_HuaZhou.CHR_JIE_PAO) {
						result.append(" 接炮");
						if (get_hu_score(player) > 0)
							result.append("+" + get_hu_score(player));
						else {
							result.append("" + get_hu_score(player));
						}

						if (get_que_score(player) > 0)
							result.append(" 缺门+" + get_que_score(player));

						if (get_fen_score(player) > 0)
							result.append(" 风坎+" + get_fen_score(player));

						if (has_rule(Constants_HuaZhou.GAME_RULE_SHAI_ZHANG)) {
							if (get_shai_score(player) > 0)
								result.append(" 晒张+" + get_shai_score(player));
						}

					}
				} else if (type == Constants_HuaZhou.CHR_FANG_PAO) {
					result.append(" 点炮");
				}

			}

			if (GRR._end_type != GameConstants.Game_End_DRAW) {
				int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
				if (GRR != null) {
					for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
						for (int w = 0; w < GRR._weave_count[p]; w++) {
							if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
								continue;
							}
							if (p == player) {// 自己
								// 接杠
								if (GRR._weave_items[p][w].provide_player != p) {
									jie_gang++;
								} else {
									if (GRR._weave_items[p][w].public_card == 1) {// 明杠
										ming_gang++;
									} else {
										an_gang++;
									}
								}
							} else {
								// 放杠
								if (GRR._weave_items[p][w].provide_player == player) {
									fang_gang++;
								}
							}
						}
					}
				}
				if (an_gang > 0) {
					result.append(" 暗杠X" + an_gang);
				}
				if (ming_gang > 0) {
					result.append(" 补杠X" + ming_gang);
				}
				if (jie_gang > 0) {
					result.append(" 接杠X" + jie_gang);
				}
				if (fang_gang > 0) {
					result.append(" 放杠X" + fang_gang);
				}
			}
			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x05, 0x05, 0x05, 0x02, 0x03, 0x04, 0x29, 0x29, 0x29, 0x31, 0x31, 0x35,
				0x35 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x15, 0x15, 0x33, 0x31,
				0x31 };
		int[] cards_of_player1 = new int[] { 0x05, 0x05, 0x05, 0x02, 0x03, 0x04, 0x29, 0x29, 0x29, 0x31, 0x31, 0x35,
				0x35 };
		int[] cards_of_player3 = new int[] { 0x05, 0x05, 0x05, 0x02, 0x03, 0x04, 0x29, 0x29, 0x29, 0x31, 0x31, 0x35,
				0x35 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

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

	// 庄家选择
	public void progress_banker_select() {
		// if (DEBUG_CARDS_MODE) {
		// _cur_banker = getOpenRoomIndex();// 创建者的玩家为庄家
		// _shang_zhuang_player = GameConstants.INVALID_SEAT;
		// _lian_zhuang_player = GameConstants.INVALID_SEAT;
		// return;
		// }
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = getOpenRoomIndex();// 创建者的玩家为庄家
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (_cur_round < 2 && !isOpenPlayerInRoom()) {// 创建者不在房间,随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	public int getTablePlayerNumber() {
		if (getRuleValue(GameConstants.GAME_RULE_CAN_LESS) != 0) {
			if (playerNumber > 0) {
				return playerNumber;
			}
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_FOUR_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 1;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 2;
		}
		return GameConstants.GAME_PLAYER;
	}

	// 随机选择晒张的牌
	public void select_shai_zhang_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Random random = new Random();
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);

			int rand = (random.nextInt(hand_card_count) + 1 + random.nextInt(hand_card_count) + 1) % hand_card_count;

			int nShaiZhangCard = hand_cards[rand];

			if (DEBUG_CARDS_MODE) {
				nShaiZhangCard = 0x05;
			}
			GRR._especial_show_cards[i] = nShaiZhangCard;
			GRR._especial_card_count++;
			have_shaizhang[i] = true;
		}
	}

	// 显示在玩家前面的牌,小胡牌,摸杠牌(晒张)
	public boolean operate_show_shai_zhang_card(int seat_index, int type, int count, int cards[], int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	public void refresh_cards_to_client() {
		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);

			if (has_rule(Constants_HuaZhou.GAME_RULE_SHAI_ZHANG)) {
				for (int j = 0; j < hand_card_count; j++) {
					if (hand_cards[j] == GRR._especial_show_cards[i]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
					}
				}
			}
			// 玩家客户端刷新一下手牌
			operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}
	}

	public int get_shai_zhang_index(int seat_index) {
		int nShaiZhangIndex = -1;
		if (has_rule(Constants_HuaZhou.GAME_RULE_SHAI_ZHANG)) {
			nShaiZhangIndex = _logic.switch_to_card_index(GRR._especial_show_cards[seat_index]);
		}
		return nShaiZhangIndex;
	}

	public boolean is_shai_zhang_card(int seat_index, int card) {
		int index = get_shai_zhang_index(seat_index);
		if (index == -1) {
			return false;
		}
		if (!_logic.is_valid_card(card)) {
			return false;
		}
		if (index == _logic.switch_to_card_index(card)) {
			return true;
		}
		return false;
	}

	// 晒张的牌刷新(起手)
	public boolean refresh_shai_zhang_card(int seat_index, int count, int cards[]) {
		// 手上没有晒张
		if (!have_shaizhang[seat_index]) {
			return false;
		}
		int index = get_shai_zhang_index(seat_index);
		if (index == -1) {
			return false;
		}
		for (int i = count - 1; i >= 0; i--) {
			if (index == _logic.switch_to_card_index(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
				return true;
			}
		}
		return false;
	}

	// 加载房间里的玩家信息(只给特定玩家刷新跑)
	public void load_player_info_data_to_player_pao(RoomResponse.Builder roomResponse, int seat_index) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			room_player.setAccountId(rplayer.getAccount_id());
			// room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			// room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);

			if (seat_index == i) {
				room_player.setPao(_player_result.pao[i]);
			} else if (_playerStatus[i]._is_pao_qiang) {
				room_player.setPao(_player_result.pao[i]);
				room_player.setHasPiao(1);
			} else {
				room_player.setPao(_player_result.pao[i]);
			}
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
	}

	// 刷新玩家信息(帮客户端解决跑分刷新多次)
	public boolean operate_player_data_to_player(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		// this.load_player_info_data(roomResponse);
		this.load_player_info_data_to_player_pao(roomResponse, seat_index);
		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	// 还原晒张的操作组合
	public void restore_shai_zhang_weave(int seat_index, WeaveItem WeaveItem[], int cbWeaveCount,
			WeaveItem destWeaveItem[]) {
		for (int i = 0; i < cbWeaveCount; i++) {
			destWeaveItem[i].public_card = WeaveItem[i].public_card;
			destWeaveItem[i].center_card = WeaveItem[i].center_card;
			destWeaveItem[i].weave_kind = WeaveItem[i].weave_kind;
			destWeaveItem[i].provide_player = WeaveItem[i].provide_player;

			if (is_shai_zhang_card(seat_index, WeaveItem[i].center_card)) {
				destWeaveItem[i].center_card = WeaveItem[i].center_card - GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			}
		}
	}
}
