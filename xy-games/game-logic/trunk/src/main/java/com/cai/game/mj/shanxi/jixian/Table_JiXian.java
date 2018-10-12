package com.cai.game.mj.shanxi.jixian;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.dictionary.SysParamDict;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.FengKanUtil;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
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

@ThreeDimension
public class Table_JiXian extends AbstractMJTable {
	private static final long serialVersionUID = -2456323602522819218L;

	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];

	public HandlerOutCardBaoTing_JiXian _handler_out_card_bao_ting;

	public int analyse_state = 0;
	public final int ANALYSE_NORMAL = 1;
	public final int ANALYSE_TING = 2;

	public int[] zhua_pai_count = new int[getTablePlayerNumber()];

	// 19夹数量
	public int[] one_nine_count = new int[getTablePlayerNumber()];
	// 28夹数量
	public int[] two_eight_count = new int[getTablePlayerNumber()];
	// 叶数量
	public int[] leaf_count = new int[getTablePlayerNumber()];
	// 中心五数量
	public int[] center_five_count = new int[getTablePlayerNumber()];
	// 黑三方数量
	public int[] black_three_count = new int[getTablePlayerNumber()];
	// 缺门数量
	public int[] que_men_count = new int[getTablePlayerNumber()];
	// 三元数量
	public int[] san_yuan_count = new int[getTablePlayerNumber()];
	// 是否胡牌时是特殊黑三方
	public boolean[] is_special_black_three = new boolean[getTablePlayerNumber()];
	// 是否胡牌时是特殊三元
	public boolean[] is_special_san_yuan = new boolean[getTablePlayerNumber()];

	// 是否存在超过30分的牌型
	public boolean[] exist_more_than_30 = new boolean[getTablePlayerNumber()];

	public Table_JiXian(MJType type) {
		super(type);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new HandleDispatchCard_JiXian();
		_handler_gang = new HandleGang_JiXian();
		_handler_out_card_operate = new HandleOutCard_JiXian();
		_handler_out_card_bao_ting = new HandlerOutCardBaoTing_JiXian();
	}

	@Override
	protected boolean on_game_start() {
		is_bao_ting = new boolean[getTablePlayerNumber()];
		zhua_pai_count = new int[getTablePlayerNumber()];
		exist_more_than_30 = new boolean[getTablePlayerNumber()];
		gang_dispatch_count = 0;

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
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		set_handler(_handler_out_card_bao_ting);
		_handler_out_card_bao_ting.reset_status(seat_index, card, type);
		_handler.exe(this);

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
		que_men_count[_seat_index] = 0;
		leaf_count[_seat_index] = 0;
		black_three_count[_seat_index] = 0;
		san_yuan_count[_seat_index] = 0;
		center_five_count[_seat_index] = 0;
		one_nine_count[_seat_index] = 0;
		two_eight_count[_seat_index] = 0;
		is_special_black_three[_seat_index] = false;
		is_special_san_yuan[_seat_index] = false;

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
		boolean can_win_ping_hu = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		// 是否有断门
		boolean has_que_men = _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count) < 3;

		// 是否有中心五
		boolean has_center_five = false;
		if (can_win_ping_hu && !has_que_men) {
			int index_1 = _logic.switch_to_card_index(0x04);
			int index_2 = _logic.switch_to_card_index(0x05);
			int index_3 = _logic.switch_to_card_index(0x06);
			int index_4 = _logic.switch_to_card_index(0x14);
			int index_5 = _logic.switch_to_card_index(0x15);
			int index_6 = _logic.switch_to_card_index(0x16);
			int index_7 = _logic.switch_to_card_index(0x24);
			int index_8 = _logic.switch_to_card_index(0x25);
			int index_9 = _logic.switch_to_card_index(0x26);

			if (cbCardIndexTemp[index_1] > 0 && cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_3] > 0) {
				cbCardIndexTemp[index_1]--;
				cbCardIndexTemp[index_2]--;
				cbCardIndexTemp[index_3]--;

				has_center_five = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

				cbCardIndexTemp[index_1]++;
				cbCardIndexTemp[index_2]++;
				cbCardIndexTemp[index_3]++;
			}

			if (!has_center_five) {
				if (cbCardIndexTemp[index_4] > 0 && cbCardIndexTemp[index_5] > 0 && cbCardIndexTemp[index_6] > 0) {
					cbCardIndexTemp[index_4]--;
					cbCardIndexTemp[index_5]--;
					cbCardIndexTemp[index_6]--;

					has_center_five = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_4]++;
					cbCardIndexTemp[index_5]++;
					cbCardIndexTemp[index_6]++;
				}
			}

			if (!has_center_five) {
				if (cbCardIndexTemp[index_7] > 0 && cbCardIndexTemp[index_8] > 0 && cbCardIndexTemp[index_9] > 0) {
					cbCardIndexTemp[index_7]--;
					cbCardIndexTemp[index_8]--;
					cbCardIndexTemp[index_9]--;

					has_center_five = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_7]++;
					cbCardIndexTemp[index_8]++;
					cbCardIndexTemp[index_9]++;
				}
			}

			if (!has_center_five) {
				if (cbCardIndexTemp[index_2] >= 3) {
					cbCardIndexTemp[index_2] -= 3;

					has_center_five = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_2] += 3;
				}
			}

			if (!has_center_five) {
				if (cbCardIndexTemp[index_5] >= 3) {
					cbCardIndexTemp[index_5] -= 3;

					has_center_five = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_5] += 3;
				}
			}

			if (!has_center_five) {
				if (cbCardIndexTemp[index_8] >= 3) {
					cbCardIndexTemp[index_8] -= 3;

					has_center_five = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_8] += 3;
				}
			}
		}

		// 是否有黑三方
		boolean has_black_three = false;
		if (can_win_ping_hu && !has_que_men && !has_center_five) {
			int index_1 = _logic.switch_to_card_index(0x31);
			int index_2 = _logic.switch_to_card_index(0x32);
			int index_3 = _logic.switch_to_card_index(0x33);
			int index_4 = _logic.switch_to_card_index(0x34);

			if (cbCardIndexTemp[index_1] > 0 && cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_3] > 0) {
				cbCardIndexTemp[index_1]--;
				cbCardIndexTemp[index_2]--;
				cbCardIndexTemp[index_3]--;

				has_black_three = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

				cbCardIndexTemp[index_1]++;
				cbCardIndexTemp[index_2]++;
				cbCardIndexTemp[index_3]++;
			}

			if (!has_black_three) {
				if (cbCardIndexTemp[index_1] > 0 && cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_4] > 0) {
					cbCardIndexTemp[index_1]--;
					cbCardIndexTemp[index_2]--;
					cbCardIndexTemp[index_4]--;

					has_black_three = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_1]++;
					cbCardIndexTemp[index_2]++;
					cbCardIndexTemp[index_4]++;
				}
			}

			if (!has_black_three) {
				if (cbCardIndexTemp[index_1] > 0 && cbCardIndexTemp[index_3] > 0 && cbCardIndexTemp[index_4] > 0) {
					cbCardIndexTemp[index_1]--;
					cbCardIndexTemp[index_3]--;
					cbCardIndexTemp[index_4]--;

					has_black_three = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_1]++;
					cbCardIndexTemp[index_3]++;
					cbCardIndexTemp[index_4]++;
				}
			}

			if (!has_black_three) {
				if (cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_3] > 0 && cbCardIndexTemp[index_4] > 0) {
					cbCardIndexTemp[index_2]--;
					cbCardIndexTemp[index_3]--;
					cbCardIndexTemp[index_4]--;

					has_black_three = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

					cbCardIndexTemp[index_2]++;
					cbCardIndexTemp[index_3]++;
					cbCardIndexTemp[index_4]++;
				}
			}
		}

		// 是否有三元
		boolean has_three_honer = false;
		if (can_win_ping_hu && !has_que_men && !has_center_five && !has_black_three) {
			int index_1 = _logic.switch_to_card_index(0x35);
			int index_2 = _logic.switch_to_card_index(0x36);
			int index_3 = _logic.switch_to_card_index(0x37);

			if (cbCardIndexTemp[index_1] > 0 && cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_3] > 0) {
				cbCardIndexTemp[index_1]--;
				cbCardIndexTemp[index_2]--;
				cbCardIndexTemp[index_3]--;

				has_three_honer = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

				cbCardIndexTemp[index_1]++;
				cbCardIndexTemp[index_2]++;
				cbCardIndexTemp[index_3]++;
			}
		}

		// 七对
		boolean can_win_qi_dui = is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weave_count);

		// 乱一色
		boolean can_luan_yi_se = !can_win_ping_hu && !can_win_qi_dui && is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		// 乱方
		boolean can_luan_fang = !can_win_ping_hu && !can_win_qi_dui && is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count);

		can_win = (can_win_ping_hu || can_win_qi_dui) && (has_que_men || has_center_five || has_black_three || has_three_honer);

		if (!can_win && !can_luan_yi_se && !can_luan_fang) {
			chiHuRight.set_empty();
			return 0;
		}

		// 字一色 100分
		boolean can_zi_yi_se = can_win && is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		if (can_zi_yi_se) {
			chiHuRight.opr_or(UniversalConstants.U_CHR_JX_ZI_YI_SE);
		}

		// 是否需要继续判断
		boolean need_to_continue = !can_zi_yi_se;

		if (can_win && need_to_continue) {
			int index_1 = _logic.switch_to_card_index(0x35);
			int index_2 = _logic.switch_to_card_index(0x36);
			int index_3 = _logic.switch_to_card_index(0x37);

			// 中发白 100分
			boolean can_zhong_fa_bai = false;
			if ((cur_card == 0x35 || cur_card == 0x36 || cur_card == 0x37)
					&& (cbCardIndexTemp[index_1] >= 3 && cbCardIndexTemp[index_2] >= 3 && cbCardIndexTemp[index_3] >= 3)) {
				cbCardIndexTemp[index_1] -= 3;
				cbCardIndexTemp[index_2] -= 3;
				cbCardIndexTemp[index_3] -= 3;

				can_zhong_fa_bai = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
				if (can_zhong_fa_bai) {
					chiHuRight.opr_or(UniversalConstants.U_CHR_JX_ZHONG_FA_BAI);
				}

				cbCardIndexTemp[index_1] += 3;
				cbCardIndexTemp[index_2] += 3;
				cbCardIndexTemp[index_3] += 3;
			}

			need_to_continue = need_to_continue && !can_zhong_fa_bai;
		}

		if (can_win && need_to_continue) {
			// 七对 50
			if (can_win_qi_dui) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_QI_DUI);
			}

			need_to_continue = need_to_continue && !can_win_qi_dui;
		}

		if (can_win && need_to_continue) {
			// 清一色 50
			boolean can_qing_yi_se = is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count);
			if (can_qing_yi_se) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_QING_YI_SE);
			}

			need_to_continue = need_to_continue && !can_qing_yi_se;
		}

		if (can_win && need_to_continue) {
			// 天胡 50
			boolean can_tian_hu = (weave_count == 0) && (zhua_pai_count[_seat_index] == 1) && (_seat_index == GRR._banker_player);
			// 地胡 50
			boolean can_di_hu = (weave_count == 0) && (zhua_pai_count[_seat_index] == 1) && (_seat_index != GRR._banker_player);

			if (can_tian_hu) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_TIAN_HU);
			}
			if (can_di_hu) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_DI_HU);
			}

			need_to_continue = need_to_continue && !can_tian_hu && !can_di_hu;
		}

		if (can_win && need_to_continue) {
			int index_1 = _logic.switch_to_card_index(0x35);
			int index_2 = _logic.switch_to_card_index(0x36);
			int index_3 = _logic.switch_to_card_index(0x37);
			// 大肚子 50
			boolean can_da_du_zi = false;
			if (cur_card == 0x35 && cbCardIndexTemp[index_1] == 3 && cbCardIndexTemp[index_2] == 1 && cbCardIndexTemp[index_3] == 1) {
				can_da_du_zi = true;
			}
			if (cur_card == 0x36 && cbCardIndexTemp[index_1] == 1 && cbCardIndexTemp[index_2] == 3 && cbCardIndexTemp[index_3] == 1) {
				can_da_du_zi = true;
			}
			if (cur_card == 0x37 && cbCardIndexTemp[index_1] == 1 && cbCardIndexTemp[index_2] == 1 && cbCardIndexTemp[index_3] == 3) {
				can_da_du_zi = true;
			}

			if (can_da_du_zi) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_DA_DU_ZI);
			}

			need_to_continue = need_to_continue && !can_da_du_zi;
		}

		if (need_to_continue) {
			// 乱方 50
			if (can_luan_fang) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_LUAN_FANG);
			}

			need_to_continue = need_to_continue && !can_luan_fang;
		}

		if (need_to_continue) {
			// 乱一色 50
			if (can_luan_yi_se) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_JX_LUAN_YI_SE);
			}

			need_to_continue = need_to_continue && !can_luan_yi_se;
		}

		if (can_win && need_to_continue) {
			// 注意扣听的时候 除去自摸和门清 分数必须大于2
			if (analyse_state == ANALYSE_NORMAL) {
				// 如果没大于30分的大牌型 计算最优解
				int[] tmp_feng_kan = new int[2];
				if (can_win_ping_hu) {
					FengKanUtil.getFengKanCount(cbCardIndexTemp, tmp_feng_kan, false);
				}

				// 三元
				san_yuan_count[_seat_index] = tmp_feng_kan[1];

				if (san_yuan_count[_seat_index] > 0 && (cur_card == 0x35 || cur_card == 0x36 || cur_card == 0x37)) {
					// 是否是特殊三元
					is_special_san_yuan[_seat_index] = true;
				}

				if (is_special_san_yuan[_seat_index] && san_yuan_count[_seat_index] > 1) {
					// 如果是特殊三元 & 三元>=2
					return UniversalConstants.WIK_CHI_HU;
				}

				// 黑三方
				black_three_count[_seat_index] = tmp_feng_kan[0];

				if (black_three_count[_seat_index] > 0 && (cur_card == 0x31 || cur_card == 0x32 || cur_card == 0x33 || cur_card == 0x34)) {
					int index_1 = _logic.switch_to_card_index(0x31);
					int index_2 = _logic.switch_to_card_index(0x32);
					int index_3 = _logic.switch_to_card_index(0x33);
					int index_4 = _logic.switch_to_card_index(0x34);
					boolean checked = false;

					if ((cur_card == 0x31 || cur_card == 0x32 || cur_card == 0x33) && !checked && cbCardIndexTemp[index_1] > 0
							&& cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_3] > 0) {
						cbCardIndexTemp[index_1]--;
						cbCardIndexTemp[index_2]--;
						cbCardIndexTemp[index_3]--;

						boolean can_special = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
						if (can_special) {
							checked = true;
							is_special_black_three[_seat_index] = true;
						}

						cbCardIndexTemp[index_1]++;
						cbCardIndexTemp[index_2]++;
						cbCardIndexTemp[index_3]++;
					}

					if ((cur_card == 0x31 || cur_card == 0x32 || cur_card == 0x34) && !checked && cbCardIndexTemp[index_1] > 0
							&& cbCardIndexTemp[index_2] > 0 && cbCardIndexTemp[index_4] > 0) {
						cbCardIndexTemp[index_1]--;
						cbCardIndexTemp[index_2]--;
						cbCardIndexTemp[index_4]--;

						boolean can_special = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
						if (can_special) {
							checked = true;
							is_special_black_three[_seat_index] = true;
						}

						cbCardIndexTemp[index_1]++;
						cbCardIndexTemp[index_2]++;
						cbCardIndexTemp[index_4]++;
					}

					if ((cur_card == 0x31 || cur_card == 0x33 || cur_card == 0x34) && !checked && cbCardIndexTemp[index_1] > 0
							&& cbCardIndexTemp[index_3] > 0 && cbCardIndexTemp[index_4] > 0) {
						cbCardIndexTemp[index_1]--;
						cbCardIndexTemp[index_3]--;
						cbCardIndexTemp[index_4]--;

						boolean can_special = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
						if (can_special) {
							checked = true;
							is_special_black_three[_seat_index] = true;
						}

						cbCardIndexTemp[index_1]++;
						cbCardIndexTemp[index_3]++;
						cbCardIndexTemp[index_4]++;
					}

					if ((cur_card == 0x32 || cur_card == 0x33 || cur_card == 0x34) && !checked && cbCardIndexTemp[index_2] > 0
							&& cbCardIndexTemp[index_3] > 0 && cbCardIndexTemp[index_4] > 0) {
						cbCardIndexTemp[index_2]--;
						cbCardIndexTemp[index_3]--;
						cbCardIndexTemp[index_4]--;

						boolean can_special = AnalyseCardUtil.analyse_ji_xian_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
						if (can_special) {
							checked = true;
							is_special_black_three[_seat_index] = true;
						}

						cbCardIndexTemp[index_2]++;
						cbCardIndexTemp[index_3]++;
						cbCardIndexTemp[index_4]++;
					}
				}

				// 自摸
				chiHuRight.opr_or(UniversalConstants.U_CHR_ZI_MO);

				boolean can_yi_zhang_ying = false;
				if (!is_special_san_yuan[_seat_index]) {
					// 一张赢 特殊三元（1个）的时候 不能再计算一张赢
					can_yi_zhang_ying = _playerStatus[_seat_index]._hu_card_count == 1;
					if (can_yi_zhang_ying) {
						chiHuRight.opr_or(UniversalConstants.U_CHR_JX_YI_ZHANG_YING);
					}
				}

				// 门清
				boolean can_men_qing = _logic.is_men_qing_b(weaveItems, weave_count);
				if (can_men_qing) {
					chiHuRight.opr_or(UniversalConstants.U_CHR_JX_MEN_QING);
				}

				// 缺门
				que_men_count[_seat_index] = 3 - _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);

				// 叶数量
				leaf_count[_seat_index] = get_leaf_count(cbCardIndexTemp, weaveItems, weave_count);

				if (is_special_black_three[_seat_index]) {
					if (can_yi_zhang_ying) {
						black_three_count[_seat_index]--;
					}
				}

				List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
				boolean bValue = newLogic.analyse_card_ji_xian(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray);

				if (bValue) {
					int tmpCenterFive = 0;
					int tmpOneNine = 0;
					int tmpTwoEight = 0;
					int tmpMaxCount = 0;
					int maxCount = 0;
					int maxIndex = -1;

					for (int i = 0; i < analyseItemArray.size(); i++) {
						tmpCenterFive = 0;
						tmpOneNine = 0;
						tmpTwoEight = 0;
						tmpMaxCount = 0;

						AnalyseItem item = analyseItemArray.get(i);

						for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
							if (item.cbWeaveKind[j] == GameConstants.WIK_NULL)
								break;

							if (item.cbWeaveKind[j] == GameConstants.WIK_PENG_CENTER_FIVE) {
								tmpCenterFive++;
								tmpMaxCount++;
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_CHI_CENTER_FIVE) {
								tmpCenterFive++;
								tmpMaxCount++;
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_ONE_NINE_MIX) {
								tmpOneNine++;
								tmpMaxCount++;
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_TWO_EIGHT_MIX) {
								tmpTwoEight++;
								tmpMaxCount++;
							}
						}

						if (tmpMaxCount > maxCount) {
							maxCount = tmpMaxCount;
							maxIndex = i;
							// 中心五
							center_five_count[_seat_index] = tmpCenterFive;
							// 19夹
							one_nine_count[_seat_index] = tmpOneNine;
							// 28夹
							two_eight_count[_seat_index] = tmpTwoEight;
						}
					}

					if (can_yi_zhang_ying && maxIndex != -1) {
						// 一张赢 并且有 中心五 19夹 28夹 的时候，判断抓的那张牌，是否刚好组成一个公家
						AnalyseItem item = analyseItemArray.get(maxIndex);

						for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
							if (item.cbWeaveKind[j] == GameConstants.WIK_NULL)
								break;

							if (item.cbWeaveKind[j] == GameConstants.WIK_PENG_CENTER_FIVE) {
								if (item.cbCardData[j][0] == cur_card || item.cbCardData[j][1] == cur_card || item.cbCardData[j][2] == cur_card) {
									center_five_count[_seat_index]--;
									break;
								}
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_CHI_CENTER_FIVE) {
								if (item.cbCardData[j][0] == cur_card || item.cbCardData[j][1] == cur_card || item.cbCardData[j][2] == cur_card) {
									center_five_count[_seat_index]--;
									break;
								}
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_ONE_NINE_MIX) {
								if (item.cbCardData[j][0] == cur_card || item.cbCardData[j][1] == cur_card || item.cbCardData[j][2] == cur_card) {
									one_nine_count[_seat_index]--;
									break;
								}
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_TWO_EIGHT_MIX) {
								if (item.cbCardData[j][0] == cur_card || item.cbCardData[j][1] == cur_card || item.cbCardData[j][2] == cur_card) {
									two_eight_count[_seat_index]--;
									break;
								}
							}
						}
					}
				}
			} else if (analyse_state == ANALYSE_TING) {
				// 如果是获取听牌数据的时候

				int tmp_que_men_count = 0;
				int tmp_leaf_count = 0;
				int tmp_black_three_count = 0;
				int tmp_san_yuan_count = 0;

				boolean can_yi_zhang_ying = _playerStatus[_seat_index]._hu_card_count == 1;
				if (can_yi_zhang_ying) {
					// 一张赢
					return UniversalConstants.WIK_CHI_HU;
				}

				tmp_que_men_count = 3 - _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);

				if (tmp_que_men_count >= 2) {
					// 缺2门
					return UniversalConstants.WIK_CHI_HU;
				}

				tmp_leaf_count = get_leaf_count(cbCardIndexTemp, weaveItems, weave_count);
				if (tmp_leaf_count + tmp_que_men_count >= 2) {
					// 叶+缺门大于等于2
					return UniversalConstants.WIK_CHI_HU;
				}

				int[] tmp_feng_kan = new int[2];
				if (can_win_ping_hu) {
					FengKanUtil.getFengKanCount(cbCardIndexTemp, tmp_feng_kan, false);
				}

				// 黑三方
				tmp_black_three_count = tmp_feng_kan[0];
				// 三元
				tmp_san_yuan_count = tmp_feng_kan[1];

				if (tmp_san_yuan_count > 0) {
					// 三元大于0
					return UniversalConstants.WIK_CHI_HU;
				}

				if (tmp_leaf_count + tmp_que_men_count + tmp_black_three_count >= 2) {
					// 叶+缺门+黑三方 >= 2
					return UniversalConstants.WIK_CHI_HU;
				}

				List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
				boolean bValue = newLogic.analyse_card_ji_xian(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray);

				if (bValue) {
					for (int i = 0; i < analyseItemArray.size(); i++) {
						int tmpMaxCount = 0;

						AnalyseItem item = analyseItemArray.get(i);

						for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
							if (item.cbWeaveKind[j] == GameConstants.WIK_NULL)
								break;

							if (item.cbWeaveKind[j] == GameConstants.WIK_PENG_CENTER_FIVE) {
								tmpMaxCount++;
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_CHI_CENTER_FIVE) {
								tmpMaxCount++;
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_ONE_NINE_MIX) {
								tmpMaxCount++;
							}
							if (item.cbWeaveKind[j] == GameConstants.WIK_TWO_EIGHT_MIX) {
								tmpMaxCount++;
							}
						}

						if (tmpMaxCount + tmp_leaf_count + tmp_que_men_count + tmp_black_three_count >= 2) {
							// 中心五+19夹+28夹+叶+缺门+黑三方 >= 2
							return UniversalConstants.WIK_CHI_HU;
						}
					}
				}

				// 如果不能扣听
				return 0;
			}
		}

		return UniversalConstants.WIK_CHI_HU;
	}

	public int get_leaf_count(final int[] cardIndex, WeaveItem[] weaveItems, int weaveCount) {
		int count = 0;

		int[] tmpCardIndex = Arrays.copyOf(cardIndex, cardIndex.length);

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				int index = _logic.switch_to_card_index(weaveItems[i].center_card);
				tmpCardIndex[index] = 4;
			}
		}

		int singleColorCount = 0;
		for (int i = 0; i < 9; i++) {
			if (tmpCardIndex[i] > 0)
				singleColorCount += tmpCardIndex[i];
		}
		if (singleColorCount > 7) {
			count += singleColorCount - 7;
		}

		singleColorCount = 0;
		for (int i = 9; i < 18; i++) {
			if (tmpCardIndex[i] > 0)
				singleColorCount += tmpCardIndex[i];
		}
		if (singleColorCount > 7) {
			count += singleColorCount - 7;
		}

		singleColorCount = 0;
		for (int i = 18; i < 27; i++) {
			if (tmpCardIndex[i] > 0)
				singleColorCount += tmpCardIndex[i];
		}
		if (singleColorCount > 7) {
			count += singleColorCount - 7;
		}

		singleColorCount = 0;
		for (int i = 27; i < 34; i++) {
			if (tmpCardIndex[i] > 0)
				singleColorCount += tmpCardIndex[i];
		}
		if (singleColorCount > 7) {
			count += singleColorCount - 7;
		}

		return count;
	}

	@Override
	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = getGame_id() == 0 ? 1 : getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}

			send_response_to_player(seat_index, roomResponse);

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}

			send_response_to_other(seat_index, roomResponse);
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			}

			send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

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

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}

			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean is_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	public boolean is_zi_yi_se(final int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		return true;
	}

	public boolean is_qi_xiao_dui(final int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;
		}

		if (cbReplaceCount > 0)
			return false;

		return true;
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

	public int get_pai_xing_fen(ChiHuRight chr, int seat_index) {
		int pai_xing_feng = 0;

		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_ZI_YI_SE).is_empty()) {
			pai_xing_feng = 100;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_ZHONG_FA_BAI).is_empty()) {
			pai_xing_feng = 100;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_QI_DUI).is_empty()) {
			pai_xing_feng = 50;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_QING_YI_SE).is_empty()) {
			pai_xing_feng = 50;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_DA_DU_ZI).is_empty()) {
			pai_xing_feng = 50;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_DI_HU).is_empty()) {
			pai_xing_feng = 50;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_TIAN_HU).is_empty()) {
			pai_xing_feng = 50;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_LUAN_FANG).is_empty()) {
			pai_xing_feng = 50;
			exist_more_than_30[seat_index] = true;
		}
		if (!exist_more_than_30[seat_index] && !chr.opr_and(UniversalConstants.U_CHR_JX_LUAN_YI_SE).is_empty()) {
			pai_xing_feng = 30;
			exist_more_than_30[seat_index] = true;
		}

		if (!exist_more_than_30[seat_index]) {
			pai_xing_feng = 0;

			if (!chr.opr_and(UniversalConstants.U_CHR_ZI_MO).is_empty()) {
				pai_xing_feng += 2;
			}
			if (!chr.opr_and(UniversalConstants.U_CHR_JX_MEN_QING).is_empty()) {
				pai_xing_feng += 5;
			}
			if (!chr.opr_and(UniversalConstants.U_CHR_JX_YI_ZHANG_YING).is_empty()) {
				pai_xing_feng += 5;
			}

			pai_xing_feng += one_nine_count[seat_index];
			pai_xing_feng += two_eight_count[seat_index];
			pai_xing_feng += leaf_count[seat_index];
			pai_xing_feng += center_five_count[seat_index];
			pai_xing_feng += que_men_count[seat_index];

			if (is_special_black_three[seat_index]) {
				if (black_three_count[seat_index] == 4) {
					pai_xing_feng += 15;
				} else if (black_three_count[seat_index] == 3) {
					pai_xing_feng += 15;
				} else if (black_three_count[seat_index] == 2) {
					pai_xing_feng += 5;
				} else if (black_three_count[seat_index] == 1) {
					pai_xing_feng += 2;
				}
			} else {
				pai_xing_feng += black_three_count[seat_index];
			}

			if (is_special_san_yuan[seat_index]) {
				if (san_yuan_count[seat_index] == 4) {
					pai_xing_feng += 100;
				} else if (san_yuan_count[seat_index] == 3) {
					pai_xing_feng += 100;
				} else if (san_yuan_count[seat_index] == 2) {
					pai_xing_feng += 50;
				} else if (san_yuan_count[seat_index] == 1) {
					pai_xing_feng += 10;
				}
			} else {
				pai_xing_feng += 5 * san_yuan_count[seat_index];
			}
		}

		return pai_xing_feng == 0 ? 1 : pai_xing_feng;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int pai_xing_fen = get_pai_xing_fen(chr, seat_index);

		int lChiHuScore = pai_xing_fen * UniversalConstants.CELL_SCORE;

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

				if (exist_more_than_30[player] && GRR._chi_hu_rights[player].is_valid()) {
					if (type == UniversalConstants.U_CHR_JX_ZI_YI_SE) {
						result.append(" 字一色x100");
					}
					if (type == UniversalConstants.U_CHR_JX_ZHONG_FA_BAI) {
						result.append(" 中发白x100");
					}
					if (type == UniversalConstants.U_CHR_JX_QI_DUI) {
						result.append(" 七对x50");
					}
					if (type == UniversalConstants.U_CHR_JX_QING_YI_SE) {
						result.append(" 清一色x50");
					}
					if (type == UniversalConstants.U_CHR_JX_DA_DU_ZI) {
						result.append(" 大肚子x50");
					}
					if (type == UniversalConstants.U_CHR_JX_DI_HU) {
						result.append(" 地胡x50");
					}
					if (type == UniversalConstants.U_CHR_JX_TIAN_HU) {
						result.append(" 天胡x50");
					}
					if (type == UniversalConstants.U_CHR_JX_LUAN_FANG) {
						result.append(" 乱方x50");
					}
					if (type == UniversalConstants.U_CHR_JX_LUAN_YI_SE) {
						result.append(" 乱一色x30");
					}
				} else if (!exist_more_than_30[player] && GRR._chi_hu_rights[player].is_valid()) {
					if (type == UniversalConstants.U_CHR_JX_YI_ZHANG_YING)
						result.append(" 一张赢x5");
					if (type == UniversalConstants.U_CHR_JX_MEN_QING)
						result.append(" 门清x5");
					if (type == UniversalConstants.U_CHR_ZI_MO)
						result.append(" 庄x2");
				}
			}

			if (!exist_more_than_30[player] && GRR._chi_hu_rights[player].is_valid()) {
				if (san_yuan_count[player] > 0)
					result.append(" 三元x" + san_yuan_count[player]);
				if (que_men_count[player] > 0)
					result.append(" 缺门x" + que_men_count[player]);
				if (black_three_count[player] > 0)
					result.append(" 黑三方x" + black_three_count[player]);
				if (center_five_count[player] > 0)
					result.append(" 中心五x" + center_five_count[player]);
				if (leaf_count[player] > 0)
					result.append(" 叶x" + leaf_count[player]);
				if (one_nine_count[player] > 0)
					result.append(" 一九夹x" + one_nine_count[player]);
				if (two_eight_count[player] > 0)
					result.append(" 二八夹x" + two_eight_count[player]);
			}

			// 荒庄荒杠
			if (GRR._end_type != UniversalConstants.Game_End_DRAW) {
				if (an_gang[player] > 0) {
					result.append(" 暗杠x" + an_gang[player]);
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
		for (int i = 0; i < UniversalConstants.MAX_ZI_FENG; i++) {
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
		return false;
	}

	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;

		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);

		return seat;
	}

	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index) {
		analyse_state = ANALYSE_TING;

		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = UniversalConstants.MAX_ZI_FENG;

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
		return false;
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

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x09, 0x12, 0x12, 0x18, 0x22, 0x23, 0x24, 0x26, 0x28, 0x29, 0x29 };
		int[] cards_of_player1 = new int[] { 0x06, 0x07, 0x08, 0x15, 0x15, 0x02, 0x03, 0x08, 0x09, 0x13, 0x14, 0x28, 0x28 };
		int[] cards_of_player2 = new int[] { 0x15, 0x15, 0x03, 0x05, 0x07, 0x08, 0x09, 0x18, 0x18, 0x18, 0x19, 0x19, 0x25 };
		int[] cards_of_player3 = new int[] { 0x09, 0x09, 0x13, 0x02, 0x02, 0x02, 0x01, 0x01, 0x01, 0x12, 0x21, 0x22, 0x23 };

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
}
