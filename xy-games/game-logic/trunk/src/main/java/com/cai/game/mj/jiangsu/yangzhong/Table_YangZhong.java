package com.cai.game.mj.jiangsu.yangzhong;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_YangZhong;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.MJType;
import com.cai.game.mj.NewAbstractMjTable;
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

public class Table_YangZhong extends NewAbstractMjTable {
	private static final long serialVersionUID = -3220101742189733307L;

	public int men_hua_men_feng[] = new int[getTablePlayerNumber()];
	public int player_di_fen[] = new int[getTablePlayerNumber()];
	public int out_hua_pai_count = 0;

	public Table_YangZhong() {
		super(MJType.GAME_TYPE_JS_YANG_ZHONG);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (can_win == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		if (_logic.is_valid_card(cur_card))
			tmp_hand_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int hand_card_count = _logic.get_card_count_by_index(tmp_hand_cards_index);

		int card_color_count = _logic.get_se_count(tmp_hand_cards_index, weaveItems, weave_count);
		boolean has_feng = check_feng(tmp_hand_cards_index, weaveItems, weave_count);
		// 能不能碰胡
		boolean is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		boolean exist_eat = exist_eat(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);

		boolean dui_dui_hu = is_dui_dui_hu && !exist_eat;

		boolean is_pi_hu = true;

		if (dui_dui_hu) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_DUI_DUI_HU);
			is_pi_hu = false;
		}

		if (has_feng && card_color_count == 1) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_HUN_YI_SE);
			is_pi_hu = false;
		}

		if (has_feng == false && card_color_count == 1) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_QING_YI_SE);
			is_pi_hu = false;
		}

		if (has_feng && card_color_count == 1 && dui_dui_hu) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_HUN_DUI_DUI);
			is_pi_hu = false;
		}

		if (has_feng == false && card_color_count == 1 && dui_dui_hu) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_QING_DUI_DUI);
			is_pi_hu = false;
		}

		if (has_feng && card_color_count == 0) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_DA_ZI);
			is_pi_hu = false;
		}

		if (GRR._left_card_count == 0 && card_type == Constants_YangZhong.HU_CARD_TYPE_ZI_MO)
			chiHuRight.opr_or(Constants_YangZhong.CHR_HAI_DI);

		if (hand_card_count == 2) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_DA_DIAO);
			is_pi_hu = false;
		}

		if (is_pi_hu)
			chiHuRight.opr_or(Constants_YangZhong.CHR_PI_HU);

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_YangZhong.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_ZI_MO);
		} else if (card_type == Constants_YangZhong.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_JIE_PAO);
		} else if (card_type == Constants_YangZhong.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_GANG_KAI);
		} else if (card_type == Constants_YangZhong.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_QIANG_GANG);
		}

		return cbChiHuKind;
	}

	public boolean check_feng(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int card_color = _logic.get_card_color(weave_items[i].center_card);
			if (card_color > 2)
				return true;
		}
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				int card_color = _logic.get_card_color(_logic.switch_to_card_data(i));
				if (card_color > 2)
					return true;
			}
		}

		return false;
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
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_YangZhong.HU_CARD_TYPE_QIANG_GANG, i);

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
				boolean can_peng_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng_this_card = false;
						break;
					}
				}
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && can_peng_this_card) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
				// 下家只能吃上家打出来的牌
				if (i == this.get_banker_next_seat(seat_index)) {
					action = _logic.check_chi(GRR._cards_index[i], card);
					if (action != 0) {
						if ((action & GameConstants.WIK_LEFT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
						}
						if ((action & GameConstants.WIK_CENTER) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
						}
						if ((action & GameConstants.WIK_RIGHT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
						}
						bAroseAction = true;
					}
				}
			}
			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_YangZhong.HU_CARD_TYPE_JIE_PAO, i);
				if (action != GameConstants.WIK_NULL) {
					// 屁胡
					if (isNeedHua(i)) {
						if ((has_rule(Constants_YangZhong.GAME_RULE_YI_MO_ER_CHONG) && player_di_fen[i] > 1)
								|| (has_rule(Constants_YangZhong.GAME_RULE_YING_ER_HUA) && player_di_fen[i] > 1)
								|| (has_rule(Constants_YangZhong.GAME_RULE_YING_SAN_HUA) && player_di_fen[i] > 2)
								|| (has_rule(Constants_YangZhong.GAME_RULE_YING_WU_HUA) && player_di_fen[i] > 4)) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);
							bAroseAction = true;
						}
					} else {
						// 非屁胡
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				} else {
					chr.set_empty();
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

	public int get_hua_pai_count(int[] cards_index) {
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				if (i >= GameConstants.MAX_ZI_FENG)
					count += cards_index[i];
			}
		}
		return count;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (getTablePlayerNumber() + seat - 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_real_card(int card) {
		return card;
	}

	protected int get_seat(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) { // 四人场
			seat = (seat_index + (nValue - 1) % 4) % 4;
		} else { // 三人场，所有胡牌人的对家都是那个空位置
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_banker_next_seat(seat_index);
				break;
			case 2:
				seat = get_null_seat();
				break;
			case 3:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	@Override
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_INDEX;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_YangZhong.HU_CARD_TYPE_ZI_MO, seat_index)) {
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
	public void initBanker() {
		_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_YangZhong.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_YangZhong();
		_handler_dispath_card = new HandlerDispatchCard_YangZhong();
		_handler_gang = new HandlerGang_YangZhong();
		_handler_out_card_operate = new HandlerOutCardOperate_YangZhong();

		_handler_qi_shou = new HandlerQiShou_YangZhong();
	}

	@Override
	protected boolean on_game_start() {
		return false;
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
				// for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				// for (int k = 0; k < getTablePlayerNumber(); k++) {
				// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				// }
				// }

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._game_score[i] += lGangScore[i];
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
				// for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
				// pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				// }
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
				// 流局不计分
				if (reason != GameConstants.Game_End_DRAW) {
					game_end.addGameScore(GRR._game_score[i]);
				}
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

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

	public void process_player_real_di_fen(int seat_index) {
		int chi_hu_index = _logic.switch_to_card_index(GRR._chi_hu_card[seat_index][0]);
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (GRR._cards_index[seat_index][i] == 3 || (GRR._cards_index[seat_index][i] == 2 && i == chi_hu_index)) {
				if (i < GameConstants.MAX_ZI + 4) {
					if (i + 1 - GameConstants.MAX_ZI == men_hua_men_feng[seat_index]) {
						player_di_fen[seat_index] += 2;
					} else {
						player_di_fen[seat_index] += 1;
					}
				} else {
					player_di_fen[seat_index] += 1;
				}
				operate_di_fen_bei_shu(seat_index);
			}
		}
	}

	public int get_di_fen(ChiHuRight chr, int seat_index) {
		int di_fen = 0;

		if (has_rule(Constants_YangZhong.GAME_RULE_HUN_SHI_QING_ER_SHI)) {
			if (!chr.opr_and(Constants_YangZhong.CHR_DA_ZI).is_empty())
				di_fen = 80;
			else if (!chr.opr_and(Constants_YangZhong.CHR_QING_DUI_DUI).is_empty())
				di_fen = 40;
			else if (!chr.opr_and(Constants_YangZhong.CHR_HUN_DUI_DUI).is_empty())
				di_fen = 20;
			else if (!chr.opr_and(Constants_YangZhong.CHR_QING_YI_SE).is_empty())
				di_fen = 20;
			else if (!chr.opr_and(Constants_YangZhong.CHR_HUN_YI_SE).is_empty())
				di_fen = 10;
			else if (!chr.opr_and(Constants_YangZhong.CHR_DUI_DUI_HU).is_empty())
				di_fen = 10;
			else if (!chr.opr_and(Constants_YangZhong.CHR_PI_HU).is_empty())
				di_fen = 1;
		} else {
			if (!chr.opr_and(Constants_YangZhong.CHR_DA_ZI).is_empty())
				di_fen = 40;
			else if (!chr.opr_and(Constants_YangZhong.CHR_QING_DUI_DUI).is_empty())
				di_fen = 20;
			else if (!chr.opr_and(Constants_YangZhong.CHR_HUN_DUI_DUI).is_empty())
				di_fen = 10;
			else if (!chr.opr_and(Constants_YangZhong.CHR_QING_YI_SE).is_empty())
				di_fen = 10;
			else if (!chr.opr_and(Constants_YangZhong.CHR_HUN_YI_SE).is_empty())
				di_fen = 5;
			else if (!chr.opr_and(Constants_YangZhong.CHR_DUI_DUI_HU).is_empty())
				di_fen = 5;
			else if (!chr.opr_and(Constants_YangZhong.CHR_PI_HU).is_empty())
				di_fen = 1;
		}

		di_fen += player_di_fen[seat_index];

		return di_fen;
	}

	public int get_fan_shu(ChiHuRight chr) {
		int fan_shu = 1;

		if (!chr.opr_and(Constants_YangZhong.CHR_HAI_DI).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_YangZhong.CHR_DA_DIAO).is_empty())
			fan_shu *= 2;
		if (!chr.opr_and(Constants_YangZhong.CHR_GANG_KAI).is_empty())
			fan_shu *= 2;
		// 最多翻两倍，不能叠加
		if (fan_shu > 2) {
			fan_shu = 2;
		}
		return fan_shu;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		process_player_real_di_fen(seat_index);

		int di_fen = get_di_fen(chr, seat_index);

		di_fen *= get_fan_shu(chr);

		int lChiHuScore = di_fen * GameConstants.CELL_SCORE;

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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int s = lChiHuScore;

				if (i == seat_index)
					continue;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			if (!chr.opr_and(Constants_YangZhong.CHR_QIANG_GANG).is_empty()) {
				int s = lChiHuScore;

				GRR._game_score[provide_index] -= (getTablePlayerNumber() - 1) * s;
				GRR._game_score[seat_index] += (getTablePlayerNumber() - 1) * s;
			} else {
				int s = lChiHuScore;

				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;

				GRR._chi_hu_rights[provide_index].opr_or(Constants_YangZhong.CHR_FANG_PAO);
			}
		}
	}

	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {
			initBanker();
			record_game_room();
		}

		men_hua_men_feng = new int[getTablePlayerNumber()];
		player_di_fen = new int[getTablePlayerNumber()];
		out_hua_pai_count = 0;
		if (getTablePlayerNumber() == 3) {
			men_hua_men_feng[_cur_banker] = 1;
			men_hua_men_feng[get_banker_next_seat(_cur_banker)] = 2;
			men_hua_men_feng[get_banker_pre_seat(_cur_banker)] = 4;
		} else {
			men_hua_men_feng[_cur_banker] = 1;
			men_hua_men_feng[get_banker_next_seat(_cur_banker)] = 2;
			men_hua_men_feng[(_cur_banker + 2) % getTablePlayerNumber()] = 3;
			men_hua_men_feng[get_banker_pre_seat(_cur_banker)] = 4;
		}

		_last_dispatch_player = -1;

		_run_player_id = 0;

		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;

		GRR = new GameRoundRecord();
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;
		istrustee = new boolean[4];
		// 新建
		_playerStatus = new PlayerStatus[4];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i] = new PlayerStatus();
		}

		_cur_round++;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

		GRR._video_recode.setBankerPlayer(_cur_banker);

		return true;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_YangZhong.CHR_ZI_MO)
						result.append(" 自摸");

					if (type == Constants_YangZhong.CHR_JIE_PAO)
						result.append(" 接炮");

					if (type == Constants_YangZhong.CHR_PI_HU)
						result.append(" 屁和");

					if (type == Constants_YangZhong.CHR_DUI_DUI_HU)
						result.append(" 对对和");

					if (type == Constants_YangZhong.CHR_HUN_YI_SE)
						result.append(" 混一色");

					if (type == Constants_YangZhong.CHR_QING_YI_SE)
						result.append(" 清一色");

					if (type == Constants_YangZhong.CHR_HUN_DUI_DUI)
						result.append(" 浑对对");

					if (type == Constants_YangZhong.CHR_QING_DUI_DUI)
						result.append(" 清对对");

					if (type == Constants_YangZhong.CHR_DA_ZI)
						result.append(" 大字");

					if (type == Constants_YangZhong.CHR_HAI_DI)
						result.append(" 海底");

					if (type == Constants_YangZhong.CHR_DA_DIAO)
						result.append(" 大吊");

					if (type == Constants_YangZhong.CHR_GANG_KAI)
						result.append(" 杠开");

					if (type == Constants_YangZhong.CHR_QIANG_GANG)
						result.append(" 抢杠");
				} else if (type == Constants_YangZhong.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			// if (GRR._end_type != GameConstants.Game_End_DRAW) { // 荒庄荒杠
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
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
				result.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			// }

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x13, 0x13, 0x06, 0x07, 0x08, 0x28, 0x28, 0x28, 0x04, 0x44, 0x38, 0x46 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x31, 0x31, 0x13, 0x13, 0x39, 0x41, 0x42 };
		int[] cards_of_player2 = new int[] { 0x12, 0x12, 0x11, 0x13, 0x07, 0x08, 0x09, 0x07, 0x09, 0x09, 0x44, 0x12, 0x43 };
		int[] cards_of_player3 = new int[] { 0x13, 0x13, 0x11, 0x12, 0x07, 0x08, 0x09, 0x09, 0x23, 0x24, 0x44, 0x11, 0x11 };

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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public int analyse_qi_shou_hu_pai(int[] cards_index, WeaveItem[] weaveItems, int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameUtilConstants.MAX_CARD_TYPE; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) {
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(tmp_hand_cards_index, -1, magic_cards_index, magic_card_count);

		if (can_win == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hand_card_count = _logic.get_card_count_by_index(tmp_hand_cards_index);

		int card_color_count = _logic.get_se_count(tmp_hand_cards_index, weaveItems, weave_count);
		boolean has_feng = check_feng(tmp_hand_cards_index, weaveItems, weave_count);

		boolean is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(tmp_hand_cards_index, -1, magic_cards_index, magic_card_count);

		boolean exist_eat = exist_eat(GRR._weave_items[seat_index], GRR._weave_count[seat_index]);

		boolean is_pi_hu = true;

		if (is_dui_dui_hu && !exist_eat) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_DUI_DUI_HU);
			is_pi_hu = false;
		}

		if (has_feng && card_color_count == 1) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_HUN_YI_SE);
			is_pi_hu = false;
		}

		if (has_feng == false && card_color_count == 1) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_QING_YI_SE);
			is_pi_hu = false;
		}

		if (has_feng && card_color_count == 1 && is_dui_dui_hu) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_HUN_DUI_DUI);
			is_pi_hu = false;
		}

		if (has_feng == false && card_color_count == 1 && is_dui_dui_hu) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_QING_DUI_DUI);
			is_pi_hu = false;
		}

		if (has_feng && card_color_count == 0) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_DA_ZI);
			is_pi_hu = false;
		}

		if (GRR._left_card_count == 0 && card_type == Constants_YangZhong.HU_CARD_TYPE_ZI_MO)
			chiHuRight.opr_or(Constants_YangZhong.CHR_HAI_DI);

		if (hand_card_count == 2) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_DA_DIAO);
			is_pi_hu = false;
		}

		if (is_pi_hu)
			chiHuRight.opr_or(Constants_YangZhong.CHR_PI_HU);

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_YangZhong.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_ZI_MO);
		} else if (card_type == Constants_YangZhong.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_JIE_PAO);
		} else if (card_type == Constants_YangZhong.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_GANG_KAI);
		} else if (card_type == Constants_YangZhong.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_YangZhong.CHR_QIANG_GANG);
		}

		return cbChiHuKind;
	}

	@Override
	public boolean on_game_start_new() {
		_game_status = GameConstants.GS_MJ_PLAY;
		GRR._hua_pai_card = new int[getTablePlayerNumber()][GameConstants.GAME_HUA_CARD_MAX];
		GRR._hua_pai_card_count = new int[getTablePlayerNumber()];
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
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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

		// 走新的shuffle发牌之后，庄家不再获取听牌数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == _current_player) {
				continue;
			}

			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_qi_shou(_current_player, GameConstants.WIK_NULL);

		return true;
	}

	protected void get_cards_cant_out(int card, int action, int[] cards_cant_out) {
		if (cards_cant_out.length != 2)
			return;

		cards_cant_out[0] = card;

		if (GameConstants.WIK_CENTER == action)
			return;

		int card_value = _logic.get_card_value(card);

		if (card_value == 1) {
			cards_cant_out[1] = card + 3;
			return;
		}

		if (card_value == 9) {
			cards_cant_out[1] = card - 3;
			return;
		}

		if (GameConstants.WIK_LEFT == action) {
			if (card_value != 7) {
				cards_cant_out[1] = card + 3;
				return;
			}
		} else if (GameConstants.WIK_RIGHT == action) {
			if (card_value != 3) {
				cards_cant_out[1] = card - 3;
				return;
			}
		}
	}

	/**
	 * 胡牌是否满足花个数才能胡
	 * 
	 * @return true 需要 false 不需要
	 */
	public boolean isNeedHua(int seat_index) {
		if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_YangZhong.CHR_DUI_DUI_HU).is_empty()
				|| !GRR._chi_hu_rights[seat_index].opr_and(Constants_YangZhong.CHR_HUN_YI_SE).is_empty()
				|| !GRR._chi_hu_rights[seat_index].opr_and(Constants_YangZhong.CHR_QING_YI_SE).is_empty()
				|| !GRR._chi_hu_rights[seat_index].opr_and(Constants_YangZhong.CHR_HUN_DUI_DUI).is_empty()
				|| !GRR._chi_hu_rights[seat_index].opr_and(Constants_YangZhong.CHR_QING_DUI_DUI).is_empty()
				|| !GRR._chi_hu_rights[seat_index].opr_and(Constants_YangZhong.CHR_DA_ZI).is_empty()) {
			return false;
		}
		return true;

	}

	/**
	 * 补花操作
	 * 
	 * @param _seat_index
	 */
	public void bu_hua(int _seat_index) {
		while (out_hua_pai_count < 8) {
			int hua_pai_count = get_hua_pai_count(GRR._cards_index[_seat_index]);

			if (hua_pai_count == 0) {
				break;
			}
			out_hua_pai_count += hua_pai_count;
			int hua_pai[] = new int[GameConstants.MAX_COUNT];
			int tmp_hua_pai_count = 0;

			for (int i = GameConstants.MAX_ZI_FENG; i < GameConstants.MAX_INDEX; i++) {
				if (GRR._cards_index[_seat_index][i] == 1) {
					GRR._player_niao_cards[_seat_index][GRR._player_niao_count[_seat_index]++] = _logic.switch_to_card_data(i);
					GRR._cards_index[_seat_index][i] = 0;

					int card_value_hua = _logic.get_card_value(i);
					if (card_value_hua > 5) {
						card_value_hua -= 5;
					} else {
						card_value_hua -= 1;
					}

					if (card_value_hua == men_hua_men_feng[_seat_index])
						player_di_fen[_seat_index] += 2;
					else
						player_di_fen[_seat_index] += 1;
					;

					hua_pai[tmp_hua_pai_count++] = _logic.switch_to_card_data(i);

					operate_di_fen_bei_shu(_seat_index);

				}
			}

			_send_card_count += hua_pai_count;
			GRR._left_card_count -= hua_pai_count - 1;

			int tmp_card_index[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, hua_pai_count, tmp_card_index);

			GRR._left_card_count -= hua_pai_count;

			// 补花
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (tmp_card_index[i] > 0) {
					GRR._cards_index[_seat_index][i] += tmp_card_index[i];
					GRR.addHuaCard(_seat_index, _logic.switch_to_card_data(i));
				}
			}

			// TODO: 在牌桌上显示出的花牌并刷新手牌
			operate_out_card(_seat_index, hua_pai_count, hua_pai, GameConstants.OUT_CARD_TYPE_HUA_PAI, GameConstants.INVALID_SEAT);

			exe_add_discard(_seat_index, hua_pai_count, hua_pai, false, 0);

			operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BU_HUA }, 1,
					GameConstants.INVALID_SEAT);
		}
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌(重写)
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
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

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		for (int i = 0; i < GRR._hua_pai_card_count[seat_index]; i++) {
			roomResponse.addEspecialShowCards(GRR._hua_pai_card[seat_index][i]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.cleanHuaCard(seat_index);
		return true;
	}

	/**
	 * 底分变化,推送客户端
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean operate_di_fen_bei_shu(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		roomResponse.setType(MsgConstants.RESPONSE_DI_FEN_BEI_SHU);
		roomResponse.setTarget(seat_index);
		for (int i = 0; i < getPlayerCount(); i++) {
			roomResponse.addCardData(player_di_fen[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}
}
