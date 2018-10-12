package com.cai.game.mj.universal;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public abstract class AbstractMahjongTable_Universal extends AbstractMJTable {
	private static final long serialVersionUID = -2456323602522819218L;

	protected AbstractHandler_Universal<?> chiPengHandler;
	protected AbstractHandler_Universal<?> dispatchCardHandler;
	protected AbstractHandler_Universal<?> gangHandler;
	protected AbstractHandler_Universal<?> outCardOperateHandler;

	public AbstractMahjongTable_Universal(MJType type) {
		super(type);
	}

	@Override
	protected void onInitTable() {
		chiPengHandler = new ChiPengHandler_Univeral();
		dispatchCardHandler = new DispatchCardHandler_Univeral();
		gangHandler = new GangHandler_Universal();
		outCardOperateHandler = new OutCardHandler_Universal();
	}

	@Override
	protected boolean on_game_start() {
		_game_status = UniversalConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][UniversalConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < UniversalConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			// 重新装载玩家信息
			load_player_info_data(roomResponse);

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == UniversalConstants.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < UniversalConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		exe_dispatch_card(_current_player, UniversalConstants.WIK_NULL, UniversalConstants.DELAY_SEND_CARD_DELAY);

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
				if (GRR._end_type != UniversalConstants.Game_End_DRAW) {
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
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(UniversalConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < UniversalConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < UniversalConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < UniversalConstants.GAME_PLAYER; i++) {
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
				for (int j = 0; j < UniversalConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[UniversalConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
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
		if (reason == UniversalConstants.Game_End_NORMAL || reason == UniversalConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == UniversalConstants.Game_End_RELEASE_PLAY || reason == UniversalConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == UniversalConstants.Game_End_RELEASE_RESULT || reason == UniversalConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == UniversalConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == UniversalConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = UniversalConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == UniversalConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == UniversalConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(cur_card)) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		boolean can_win = false;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		// 平胡
		boolean can_win_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		if (card_type == UniversalConstants.U_HU_CARD_TYPE_GANG_SHANG_HUA) {
			chiHuRight.opr_or(UniversalConstants.U_CHR_GANG_SHANG_HUA);
		} else if (card_type == UniversalConstants.U_HU_CARD_TYPE_GANG_SHANG_PAO) {
			chiHuRight.opr_or(UniversalConstants.U_CHR_GANG_SHANG_PAO);
		} else if (card_type == UniversalConstants.U_HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or(UniversalConstants.U_CHR_QIANG_GANG_HU);
		} else if (card_type == UniversalConstants.U_HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(UniversalConstants.U_CHR_ZI_MO);
		} else if (card_type == UniversalConstants.U_HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(UniversalConstants.U_CHR_JIE_PAO);
		}

		can_win = can_win_ping_hu;

		if (can_win) {
			return UniversalConstants.WIK_CHI_HU;
		}

		return 0;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, UniversalConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, UniversalConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + UniversalConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, UniversalConstants.Show_Card_HU, hand_card_count, cards, UniversalConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		@SuppressWarnings("unused")
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int lChiHuScore = UniversalConstants.CELL_SCORE;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore;
		}

		if (zimo) {
			int s = lChiHuScore;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(UniversalConstants.U_CHR_FANG_PAO);
		}
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;

		int[] jie_gang = new int[getTablePlayerNumber()];
		int[] fang_gang = new int[getTablePlayerNumber()];
		int[] an_gang = new int[getTablePlayerNumber()];
		// 荒庄荒杠
		if (GRR._end_type != UniversalConstants.Game_End_DRAW) {
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				if (GRR != null) {
					for (int w = 0; w < GRR._weave_count[player]; w++) {
						if (GRR._weave_items[player][w].weave_kind != UniversalConstants.WIK_GANG) {
							continue;
						}

						if (GRR._weave_items[player][w].public_card == 0) {
							an_gang[player]++;
						} else {
							jie_gang[player]++;
							fang_gang[GRR._weave_items[player][w].provide_player]++;
						}
					}
				}
			}
		}

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == UniversalConstants.U_CHR_ZI_MO)
						result.append(" 自摸");

					if (type == UniversalConstants.U_CHR_JIE_PAO)
						result.append(" 接炮");
				} else if (type == UniversalConstants.U_CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			// 荒庄荒杠
			if (GRR._end_type != UniversalConstants.Game_End_DRAW) {
				if (an_gang[player] > 0) {
					result.append(" 暗杠x" + an_gang[player]);
				}
				if (fang_gang[player] > 0) {
					result.append(" 放杠x" + fang_gang[player]);
				}
				if (jie_gang[player] > 0) {
					result.append(" 接杠x" + jie_gang[player]);
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public int get_real_card(int card) {
		if (card > UniversalConstants.CARD_ESPECIAL_TYPE_TING && card < UniversalConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= UniversalConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < UniversalConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			if (UniversalConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					UniversalConstants.U_HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
		}

		return false;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = UniversalConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0 && type == UniversalConstants.DISPATCH_CARD_TYPE_NORMAL) {
				// 杠之后打的牌，其他玩家只能胡，不能吃碰杠
				if ((seat_index + 1) % getTablePlayerNumber() == i) {
					action = _logic.check_chi(GRR._cards_index[i], card);
					if ((action & UniversalConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(UniversalConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, UniversalConstants.WIK_LEFT, seat_index);
					}
					if ((action & UniversalConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(UniversalConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, UniversalConstants.WIK_CENTER, seat_index);
					}
					if ((action & UniversalConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(UniversalConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, UniversalConstants.WIK_RIGHT, seat_index);
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(UniversalConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);
					bAroseAction = true;
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int hu_card_type = UniversalConstants.U_HU_CARD_TYPE_JIE_PAO;
				if (type == UniversalConstants.DISPATCH_CARD_TYPE_GANG)
					hu_card_type = UniversalConstants.U_HU_CARD_TYPE_GANG_SHANG_PAO;

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
				if (action != 0) {
					playerStatus.add_action(UniversalConstants.WIK_CHI_HU);
					playerStatus.add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = UniversalConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;

		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);

		return seat;
	}

	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = UniversalConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (UniversalConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					UniversalConstants.U_HU_CARD_TYPE_ZI_MO, seat_index)) {
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
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = UniversalConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						UniversalConstants.U_HU_CARD_TYPE_QIANG_GANG_HU, i);

				if (action != 0) {
					_playerStatus[i].add_action(UniversalConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = UniversalConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < UniversalConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = UniversalConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < UniversalConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = UniversalConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = get_niao_card_num();

		if (GRR._count_niao > 0) {
			int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];

			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			if (DEBUG_CARDS_MODE) {
				GRR._cards_data_niao[0] = 0x04;
			}

			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

			int seat = get_seat_by_value(nValue, seat_index);

			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];

			GRR._player_niao_count[seat]++;
		}

		// 设置鸟牌显示
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
			}
		}
	}

	@Override
	public int get_seat_by_value(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
			switch (nValue) {
			// 147
			case 1:
			case 4:
			case 7:
				seat = seat_index;
				break;
			// 258
			case 2:
			case 5:
			case 8:
				seat = get_banker_next_seat(seat_index);
				break;
			// 369
			case 3:
			case 6:
			case 9:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
			case 2:
				seat = seat_index;
				break;
			case 1:
			case 3:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	public int get_niao_card_num() {
		int nNum = 0;

		if (has_rule(UniversalConstants.U_GAME_RULE_ONE_BIRD)) {
			nNum = 1;
		} else if (has_rule(UniversalConstants.U_GAME_RULE_TWO_BIRD)) {
			nNum = 2;
		} else if (has_rule(UniversalConstants.U_GAME_RULE_THREE_BIRD)) {
			nNum = 3;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}
}
