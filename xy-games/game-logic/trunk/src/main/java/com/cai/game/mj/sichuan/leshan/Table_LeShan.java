package com.cai.game.mj.sichuan.leshan;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.MjAnalyseUtil;
import com.cai.game.mj.sichuan.ScoreRowType;
import com.cai.game.mj.sichuan.Solution;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;

@ThreeDimension
public class Table_LeShan extends AbstractSiChuanMjTable {
	private static final long serialVersionUID = 1L;

	public Table_LeShan(MJType type) {
		super(type);
	}

	@Override
	protected void init_shuffle() {
		if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
			_repertory_card = new int[MJConstants.CARD_DATA_TIAO_TONG.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_TIAO_TONG);
		} else {
			_repertory_card = new int[MJConstants.CARD_DATA_WAN_TIAO_TONG.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_WAN_TIAO_TONG);
		}
	};

	@Override
	public void process_show_hand_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], cards, ding_que_pai_se[i]);

			if (win_order[i] != 0) {
				if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
					cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_HU;
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		long effect_indexs[] = new long[1];
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZI_MO).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
			effect_indexs[0] = Constants_SiChuan.CHR_ZI_MO;
		} else {
			effect_indexs[0] = Constants_SiChuan.CHR_JIE_PAO;
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, effect_indexs, 1, GameConstants.INVALID_SEAT);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(get_real_card(operate_card))]--;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(GameConstants.Show_Card_Si_Chuan);
		roomResponse.setCardCount(1);
		if (rm) {
			roomResponse.addCardData(GameConstants.BLACK_CARD);
		} else {
			roomResponse.addCardData(operate_card);
		}
		roomResponse.setOperateLen(_logic.get_card_count_by_index(GRR._cards_index[seat_index]));

		send_response_to_other(seat_index, roomResponse);

		// 回放数据
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[seat_index], cards, ding_que_pai_se[seat_index]);
		cards[hand_card_count++] = get_real_card(operate_card) + GameConstants.CARD_ESPECIAL_TYPE_HU;

		RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
		tmp_roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		tmp_roomResponse.setTarget(seat_index);
		tmp_roomResponse.setCardType(1);
		tmp_roomResponse.setCardCount(hand_card_count);

		for (int j = 0; j < hand_card_count; j++) {
			tmp_roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(tmp_roomResponse);
	}

	@Override
	public void process_duan_xian_chong_lian(int seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (win_order[i] != 0) {
				RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
				tmp_roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
				tmp_roomResponse.setTarget(i);
				tmp_roomResponse.setCardType(GameConstants.Show_Card_Si_Chuan);
				tmp_roomResponse.setCardCount(1);
				if (win_type[i] == ZI_MO_HU || !GRR._chi_hu_rights[i].opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
					tmp_roomResponse.addCardData(GameConstants.BLACK_CARD);
				} else {
					tmp_roomResponse.addCardData(GRR._chi_hu_card[i][0]);
				}
				tmp_roomResponse.setOperateLen(_logic.get_card_count_by_index(GRR._cards_index[i]));

				if (i == seat_index)
					operate_player_get_card(i, 1, new int[] { GRR._chi_hu_card[i][0] }, i);

				send_response_to_other(i, tmp_roomResponse);
			}
		}
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		boolean showBlack = true;

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				if (weaveitems[j].public_card == 1)
					showBlack = false;
			}
		}

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (showBlack) {
					weaveItem_item.setCenterCard(GameConstants.BLACK_CARD);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			roomResponse.clearDouliuzi();

			int count = _playerStatus[i]._hu_card_count;
			for (int j = 0; j < count; j++) {
				int fanShu = ting_pai_fan_shu[i][0][j];
				roomResponse.addDouliuzi(fanShu);
			}

			send_response_to_player(i, roomResponse);
		}

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		if (showBlack) {
			roomResponse.clearWeaveItems();

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
		}

		roomResponse.clearDouliuzi();

		int count = _playerStatus[seat_index]._hu_card_count;
		for (int j = 0; j < count; j++) {
			int fanShu = ting_pai_fan_shu[seat_index][0][j];
			roomResponse.addDouliuzi(fanShu);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

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

		boolean showBlack = true;

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				if (weaveitems[j].public_card == 1)
					showBlack = false;
			}
		}

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (showBlack) {
					weaveItem_item.setCenterCard(GameConstants.BLACK_CARD);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			roomResponse.clearDouliuzi();

			int count = _playerStatus[i]._hu_card_count;
			for (int j = 0; j < count; j++) {
				int fanShu = ting_pai_fan_shu[i][0][j];
				roomResponse.addDouliuzi(fanShu);
			}

			send_response_to_player(i, roomResponse);
		}

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		roomResponse.clearDouliuzi();

		if (showBlack) {
			roomResponse.clearWeaveItems();

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
		}

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];

			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);

			for (int j = 0; j < ting_card_cout; j++) {
				roomResponse.addDouliuzi(ting_pai_fan_shu[seat_index][i][j]);
			}
		}

		for (int i = 0; i < table_hu_card_count; i++) {
			roomResponse.addCardsList(table_hu_cards[i]);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void process_gang_score() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean is_ting_state = is_ting_state(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);
			if (win_order[i] != 0 || left_player_count == 1 || is_ting_state) {

				for (int w_index = 0; w_index < GRR._weave_count[i]; w_index++) {
					WeaveItem weave = GRR._weave_items[i][w_index];

					if (weave.is_vavild && (weave.weave_kind == GameConstants.WIK_GANG || weave.weave_kind == GameConstants.WIK_SUO_GANG_1
							|| weave.weave_kind == GameConstants.WIK_SUO_GANG_2 || weave.weave_kind == GameConstants.WIK_SUO_GANG_3)) {
						@SuppressWarnings("unused")
						int cbGangIndex = GRR._gang_score[i].gang_count++;

						if (weave.type == GameConstants.GANG_TYPE_AN_GANG) {
							for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
								if (i == o_player)
									continue;

								if (weave.gang_gei_fen_valid[o_player] == false)
									continue;

								// 有幺鸡2分 无幺鸡4分
								int score = 2 * GameConstants.CELL_SCORE;
								if (weave.weave_kind == GameConstants.WIK_GANG && hasRuleWuJiGangDouble)
									score *= 2;

								GRR._game_score[o_player] -= score;
								GRR._game_score[i] += score;
							}

							_player_result.an_gang_count[i]++;
							GRR._gang_score[i].an_gang_count++;
						}

						if (weave.type == GameConstants.GANG_TYPE_JIE_GANG) {
							// 有幺鸡2分 无幺鸡4分
							int score = 2 * GameConstants.CELL_SCORE;
							if (weave.weave_kind == GameConstants.WIK_GANG && hasRuleWuJiGangDouble)
								score *= 2;

							GRR._game_score[weave.provide_player] -= score;
							GRR._game_score[i] += score;

							_player_result.ming_gang_count[i]++;
							GRR._gang_score[i].ming_gang_count++;
						}

						if (weave.type == GameConstants.GANG_TYPE_ADD_GANG) {
							for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
								if (i == o_player)
									continue;

								if (weave.gang_gei_fen_valid[o_player] == false)
									continue;

								// 有幺鸡1分 无幺鸡2分
								int score = GameConstants.CELL_SCORE;
								if (weave.weave_kind == GameConstants.WIK_GANG && hasRuleWuJiGangDouble)
									score *= 2;

								GRR._game_score[o_player] -= score;
								GRR._game_score[i] += score;
							}

							_player_result.ming_gang_count[i]++;
							GRR._gang_score[i].ming_gang_count++;
						}
					}
				}
			}

			if (win_order[i] == 0 && left_player_count > 1 && !is_ting_state) {
				// 删除流水里面的当前玩家的杠牌流水
				Iterator<int[]> itr = scoreDetails.iterator();
				while (itr.hasNext()) {
					int[] r = itr.next();

					if (r[i + 1] > 0)
						itr.remove();
				}
			}
		}
	}

	@Override
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;
		
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int tmp_geng_count = finalGengCount[seat_index];

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
				if (i < 9)
					continue;
			}

			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				finalGengCount[seat_index] = tmp_geng_count;

				return true;
			}
		}

		finalGengCount[seat_index] = tmp_geng_count;

		return false;
	}

	@Override
	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index, int ting_count) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		int tmp_geng_count = finalGengCount[seat_index];

		for (int i = 0; i < max_ting_count; i++) {
			if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
				if (i < 9)
					continue;
			}

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				// 能听的牌数据 有几番
				ting_pai_fan_shu[seat_index][ting_count][count] = get_fan_shu(chr, finalGengCount[seat_index]);

				cards[count] = cbCurrentCard;
				count++;
			}
		}

		finalGengCount[seat_index] = tmp_geng_count;

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 查大叫，流局时，没听牌的玩家，配付给听牌玩家，最大的牌型分，胡牌了的，不用管
	 */
	@Override
	public void cha_da_jiao() {
		int[] max_pai_xing_fen = new int[getTablePlayerNumber()];
		boolean[] is_ting_state = new boolean[getTablePlayerNumber()];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			max_pai_xing_fen[i] = -1;

			if (win_order[i] != 0)
				continue;

			is_ting_state[i] = is_ting_state(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

			if (is_ting_state[i]) {
				// 获取最大牌型分
				analyse_state = FROM_MAX_COUNT;
				max_pai_xing_fen[i] = get_max_pai_xing_fen(i);
			} else {
				is_ting_when_finish[i] = false;
			}
		}

		// 赔付
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (win_order[i] != 0)
				continue;

			if (is_ting_state[i]) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (i == j)
						continue;

					if (win_order[j] != 0)
						continue;

					if (is_ting_state[j])
						continue;

					cha_da_jiao[i] = true;

					int[] row = new int[SCORE_DETAIL_COLUMN];
					row[0] = ScoreRowType.CHA_JIAO.getType();
					row[i + 1] += max_pai_xing_fen[i];
					row[j + 1] -= max_pai_xing_fen[i];

					scoreDetails.add(row);

					GRR._game_score[i] += max_pai_xing_fen[i];
					GRR._game_score[j] -= max_pai_xing_fen[i];

					if (bei_cha_da_jiao[j] == false) {
						// 飘赖统计。用来计算被查叫的次数
						bei_cha_da_jiao[j] = true;
						_player_result.piao_lai_count[j]++;
					}
				}
			}
		}
	}

	@Override
	public int get_max_pai_xing_fen(int seat_index) {
		int max_score = -1;
		int max_geng_count = 0;

		int cbCurrentCard = -1;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);

			// 如果当前牌，不是听牌数据里的数据。这样能节省很多时间。
			if (!is_ting_card(cbCurrentCard, seat_index)) {
				continue;
			}

			ChiHuRight chr = new ChiHuRight();
			chr.set_empty();

			int card_type = Constants_SiChuan.HU_CARD_TYPE_ZI_MO;

			boolean flag = false;
			if (GRR._cards_index[seat_index][i] == 5) {
				flag = true;
				GRR._cards_index[seat_index][i] = 2;
			}

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index], cbCurrentCard, chr, card_type, seat_index)) {
				if (flag) {
					GRR._cards_index[seat_index][i] = 5;
				}

				int fan_shu = get_fan_shu(chr, finalGengCount[seat_index]);

				int max_fan_shu = get_max_fan_shu();

				if (fan_shu > max_fan_shu) {
					fan_shu = max_fan_shu;
				}

				int score = 1 << fan_shu;

				if (score > max_score) {
					max_pai_xing_desc[seat_index] = get_pai_xing_desc_str(chr, seat_index);
					max_score = score;
					max_geng_count = finalGengCount[seat_index];
					finallyFanShu[seat_index] = fan_shu;
				}
			}
		}

		finalGengCount[seat_index] = max_geng_count;

		return max_score;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card >= 0x01 && cur_card <= 0x29) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int temp_cards[] = new int[GameConstants.MAX_COUNT];
		int temp_hand_card_count = _logic.switch_to_cards_data_sichuan(cbCardIndexTemp, temp_cards, ding_que_pai_se[_seat_index]);

		for (int i = 0; i < temp_hand_card_count; i++) {
			int pai_se = _logic.get_card_color(temp_cards[i]);
			if ((pai_se + 1) == ding_que_pai_se[_seat_index]) {
				// 手牌里有定缺的牌色的牌，不能胡牌
				return 0;
			}
		}

		// 牌型番按照最大的算 不叠加
		boolean need_to_continue = true;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_count = _logic.get_magic_card_count();
		for (int i = 0; i < magic_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 平胡
		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_count);
		// 七对
		long qi_dui = analyse_qi_xiao_dui(cbCardIndexTemp, weave_count);

		if (can_win == false && qi_dui == 0) {
			return 0;
		}

		// 清一色
		boolean can_win_qing_yi_se = _logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weave_count);

		// 对对胡
		boolean can_win_dd_hu = (qi_dui == 0) && AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_count);

		boolean is_jiang_yi_se = is_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		// 将对
		boolean can_win_jiang_dui = false;
		// 幺九
		boolean can_win_yao_jiu = false;
		if (has_rule(Constants_SiChuan.GAME_RULE_YAO_JIU_JIANG_DUI)) {
			can_win_jiang_dui = is_jiang_yi_se;
			can_win_yao_jiu = AnalyseCardUtil.analyse_win_yao_jiu(cbCardIndexTemp, -1, magic_cards_index, magic_count)
					&& is_yao_jiu_weave(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
		}

		// 门清
		boolean can_win_men_qing = false;
		// 中张
		boolean can_win_zhong_zhang = false;
		if (has_rule(Constants_SiChuan.GAME_RULE_MEN_QING_ZHONG_ZHANG)) {
			can_win_men_qing = is_men_qing_b(weaveItems, weave_count);
			can_win_zhong_zhang = is_zhong_zhang(cbCardIndexTemp, weaveItems, weave_count)
					&& is_zhong_zhang_weave(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
		}

		// 天胡
		boolean can_win_tian_hu = false;
		// 地胡-庄家打出的第一张牌
		boolean can_win_di_hu = false;
		if (has_rule(Constants_SiChuan.GAME_RULE_TIAN_DI_HU)) {
			if (_out_card_count == 0 && _seat_index == GRR._banker_player) {
				can_win_tian_hu = true;
			}
			if (weave_count == 0 && _seat_index != GRR._banker_player && mo_pai_count[_seat_index] <= 1) {
				can_win_di_hu = true;
			}
		}

		// 金钩胡
		boolean can_win_jin_gou_hu = (_logic.get_card_count_by_index(cbCardIndexTemp) == 2);

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_KAI);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_PAO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QIANG_GANG);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_ZI_MO && analyse_state != FROM_MAX_COUNT) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_ZI_MO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIE_PAO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_DIAN_GANG_GANG_KAI) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DG_GANG_KAI);
		}

		if (need_to_continue && can_win_qing_yi_se && qi_dui == Constants_SiChuan.CHR_SAN_LONG_QI_DUI) {
			// 清一色+三龙七对 7
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && can_win_qing_yi_se && qi_dui == Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI) {
			// 清一色+双龙七对 6
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && qi_dui == Constants_SiChuan.CHR_SAN_LONG_QI_DUI) {
			// 三龙七对 5
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && can_win_qing_yi_se && qi_dui == Constants_SiChuan.CHR_LONG_QI_DUI) {
			// 清一色+龙七对 5
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && qi_dui == Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI) {
			// 双龙七对 4
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && can_win_qing_yi_se && qi_dui == Constants_SiChuan.CHR_QI_DUI) {
			// 清一色+七对 4
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && can_win_dd_hu && can_win_qing_yi_se) {
			// 清一色+对对胡 3
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DUI_DUI_HU);
			need_to_continue = false;
		}
		if (need_to_continue && qi_dui == Constants_SiChuan.CHR_LONG_QI_DUI) {
			// 龙七对 3
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && qi_dui == Constants_SiChuan.CHR_QI_DUI) {
			// 七对 2
			chiHuRight.opr_or_long(qi_dui);
			need_to_continue = false;
		}
		if (need_to_continue && can_win_qing_yi_se) {
			// 清一色 2
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			need_to_continue = false;
		}
		if (need_to_continue && can_win_dd_hu) {
			// 对对胡 1
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DUI_DUI_HU);
			need_to_continue = false;
		}

		if (can_win_jiang_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIANG_DUI);
		}
		if (can_win_yao_jiu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_YAO_JIU);
		}
		if (can_win_men_qing) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_MEN_QING);
		}
		if (can_win_zhong_zhang) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_ZHONG_ZHANG);
		}
		if (can_win_tian_hu && analyse_state != FROM_TING) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_TIAN_HU);
		}
		if (can_win_di_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DI_HU);
		}
		if (can_win_jin_gou_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIN_GOU_HU);
		}

		if (GRR._left_card_count == 0 && analyse_state != FROM_MAX_COUNT) {
			// 海底
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_HAI_DI);
		}

		// 无幺鸡
		boolean is_wu_yao_ji = is_wu_yao_ji(cbCardIndexTemp, weave_count, weaveItems);
		if (is_wu_yao_ji)
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_WU_YAO_JI);

		int geng_count = get_geng_count(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index], GRR._weave_count[_seat_index], cur_card,
				chiHuRight);
		int fan_shu = get_fan_shu(chiHuRight, geng_count);
		score_when_win[_seat_index] = 1 << fan_shu;
		finalGengCount[_seat_index] = geng_count;

		return GameConstants.WIK_CHI_HU;
	}

	public boolean is_men_qing_b(WeaveItem weaveItems[], int cbWeaveCount) {
		if (cbWeaveCount == 0) {
			return true;
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			if (weaveItem.weave_kind != GameConstants.WIK_GANG && weaveItem.weave_kind != GameConstants.WIK_SUO_GANG_1
					&& weaveItem.weave_kind != GameConstants.WIK_SUO_GANG_2 && weaveItem.weave_kind != GameConstants.WIK_SUO_GANG_3) {
				return false;
			}
			if (weaveItem.public_card == 1) {
				return false;
			}
		}

		return true;
	}

	public int get_geng_count(int[] cards_index, WeaveItem[] weave_items, int weave_count, int hu_card, ChiHuRight chr) {
		if (analyse_state != FROM_NORMAL && analyse_state != FROM_MAX_COUNT)
			return 0;
		
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_SAN_LONG_QI_DUI).is_empty()) {
			// 七对的时候 不再计算根
			return 0;
		}

		int geng = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(hu_card))
			cbCardIndexTemp[_logic.switch_to_card_index(hu_card)]++;

		int handMagicCount = cbCardIndexTemp[magicCardIndex];

		if (handMagicCount > 0) {
			List<Integer> cardList = new ArrayList<>();

			int[] handCards = new int[GameConstants.MAX_COUNT];
			int cardCount = _logic.switch_to_cards_data_sichuan(cbCardIndexTemp, handCards, 0);

			for (int i = 0; i < cardCount; i++) {
				if (get_real_card(handCards[i]) == magicCard)
					continue;

				cardList.add(handCards[i]);
			}

			for (int i = 0; i < handMagicCount; i++)
				cardList.add(0xFF);

			Solution bestSolution = new Solution();
			MjAnalyseUtil analyseUtil = new MjAnalyseUtil(weave_count, weave_items);

			if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty())
				analyseUtil.setHasDuiDuiHu(true);
			if (!chr.opr_and_long(Constants_SiChuan.CHR_JIANG_DUI).is_empty())
				analyseUtil.setHasJiangDui(true);
			if (!chr.opr_and_long(Constants_SiChuan.CHR_ZHONG_ZHANG).is_empty())
				analyseUtil.setHasZhongZhang(true);
			if (!chr.opr_and_long(Constants_SiChuan.CHR_YAO_JIU).is_empty())
				analyseUtil.setHasYaoJiu(true);

			boolean tmpBValue = analyseUtil.getSolution(cardList);
			if (tmpBValue) {
				bestSolution = analyseUtil.getBestSolution();
				geng = bestSolution.gengCount;
			}
		} else {
			for (int i = 0; i < weave_count; i++) {
				int card = weave_items[i].center_card;
				int card_index = _logic.switch_to_card_index(card);
				int kind = weave_items[i].weave_kind;
				if (kind == GameConstants.WIK_GANG || weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_1
						|| weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_2 || weave_items[i].weave_kind == GameConstants.WIK_SUO_GANG_3) {
					cbCardIndexTemp[card_index] += 4;
				}
				if (weave_items[i].weave_kind == GameConstants.WIK_PENG || weave_items[i].weave_kind == GameConstants.WIK_SUO_PENG_1
						|| weave_items[i].weave_kind == GameConstants.WIK_SUO_PENG_2) {
					cbCardIndexTemp[card_index] += 3;
				}
			}

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cbCardIndexTemp[i] >= 4) {
					geng++;
				}
			}
		}

		return geng;
	}

	@Override
	public boolean is_zhong_zhang(int[] cards_index, WeaveItem[] weaveItems, int weave_count) {
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (cards_index[i] > 0) {
				int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if (card_value == 1 || card_value == 9) {
					return false;
				}
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == 1 || weaveItems[i].weave_kind == 9) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean is_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			if (_logic.is_magic_index(i))
				continue;

			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		if (_logic.exist_eat(weave_items, weave_count))
			return false;

		for (int i = 0; i < weave_count; i++) {
			int color = _logic.get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = _logic.get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}

		}

		return true;
	}

	@Override
	public long analyse_qi_xiao_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return 0;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return 0;
			}

			if (count > 0) {
				for (int i = 0; i < GameConstants.MAX_INDEX && count > 0; i++) {
					if (i == magicCardIndex)
						continue;

					int cbCardCount = cbCardIndexTemp[i];

					if (cbReplaceCount > 0 && ((cbCardCount == 1 || cbCardCount == 3))) {
						if (cbCardCount + count >= 4) {
							count -= 4 - cbCardCount;
							cbReplaceCount--;

							if (cbReplaceCount > count) {
								// 如果王牌完成映射之后，需要补齐的牌张数比王牌还多
								cbReplaceCount++;
								count += 4 - cbCardCount;
							} else {
								nGenCount++;
							}
						}
					} else if (cbReplaceCount == 0 && cbCardCount > 0 && cbCardCount < 4) {
						if (cbCardCount + count >= 4) {
							count -= 4 - cbCardCount;
							nGenCount++;
						}
					}
				}
				if (count == 4 && cbReplaceCount == 0) {
					nGenCount++;
				}
			}
		} else {
			if (cbReplaceCount > 0)
				return 0;
		}

		if (nGenCount > 0) {
			if (nGenCount == 3)
				return Constants_SiChuan.CHR_SAN_LONG_QI_DUI;
			else if (nGenCount == 2)
				return Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI;
			else
				return Constants_SiChuan.CHR_LONG_QI_DUI;
		} else {
			return Constants_SiChuan.CHR_QI_DUI;
		}
	}

	public boolean is_wu_yao_ji(int[] cards_index, int weaveCount, WeaveItem[] weaveItems) {
		if (cards_index[magicCardIndex] > 0)
			return false;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_SUO_PENG_1 || weaveItems[i].weave_kind == GameConstants.WIK_SUO_PENG_2
					|| weaveItems[i].weave_kind == GameConstants.WIK_SUO_GANG_1 || weaveItems[i].weave_kind == GameConstants.WIK_SUO_GANG_2
					|| weaveItems[i].weave_kind == GameConstants.WIK_SUO_GANG_3)
				return false;
		}
		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = GameConstants.CELL_SCORE;

		int fan_shu = get_fan_shu(chr, finalGengCount[seat_index]);
		int max_fan_shu = get_max_fan_shu();

		if (zimo)
			if (has_rule(Constants_SiChuan.GAME_RULE_ZM_FAN_BEI))
				fan_shu++;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (fan_shu > max_fan_shu) {
			fan_shu = max_fan_shu;
		}

		finallyFanShu[seat_index] = fan_shu;

		int score = di_fen * (1 << fan_shu);

		if (zimo) {
			int[] row = new int[SCORE_DETAIL_COLUMN];
			row[0] = ScoreRowType.ZI_MO.getType();

			if (has_rule(Constants_SiChuan.GAME_RULE_ZM_JIA_DI))
				score += 1;

			if (has_rule(Constants_SiChuan.GAME_RULE_DGH_DIAN_PAO) && !chr.opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
				// 点杠花（点炮）
				// 找到最后一个杠的提供者
				int p_index = -1;
				for (int w = GRR._weave_count[seat_index] - 1; w >= 0; w--) {
					if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_GANG
							|| GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_1
							|| GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_2
							|| GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_3) {
						p_index = GRR._weave_items[seat_index][w].provide_player;
						break;
					}
				}
				GRR._game_score[seat_index] += score;
				GRR._game_score[p_index] -= score;

				row[seat_index + 1] += score;
				row[p_index + 1] -= score;
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					if (GRR._win_order[i] != 0) {
						continue;
					}

					GRR._game_score[seat_index] += score;
					GRR._game_score[i] -= score;

					row[seat_index + 1] += score;
					row[i + 1] -= score;
				}
			}

			scoreDetails.add(row);
		} else {
			GRR._game_score[seat_index] += score;
			GRR._game_score[provide_index] -= score;

			int[] row = new int[SCORE_DETAIL_COLUMN];
			row[0] = ScoreRowType.JIE_PAO.getType();
			row[seat_index + 1] += score;
			row[provide_index + 1] -= score;
			scoreDetails.add(row);
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public int get_fan_shu(ChiHuRight chr, int geng_count) {
		int fan = 0;

		boolean has_long_qi_dui = false;

		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty()) {
			fan += 3;
			has_long_qi_dui = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI).is_empty()) {
			fan += 4;
			has_long_qi_dui = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_SAN_LONG_QI_DUI).is_empty()) {
			fan += 5;
			has_long_qi_dui = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIANG_DUI).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YAO_JIU).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_MEN_QING).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZHONG_ZHANG).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DI_HU).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_WU_YAO_JI).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIN_GOU_HU).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_HAI_DI).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_PAO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QIANG_GANG).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
			fan += 1;
		}

		if (!has_long_qi_dui) {
			fan += geng_count;
		}

		return fan;
	}

	public String get_pai_xing_desc_str(ChiHuRight chr, int seat_index) {
		StringBuilder result = new StringBuilder();

		int chrTypes = chr.type_count;

		boolean has_long_qi_dui = false;

		for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
			long type = chr.type_list[typeIndex];

			if (type == Constants_SiChuan.CHR_DUI_DUI_HU) {
				result.append(" 对对胡");
			}
			if (type == Constants_SiChuan.CHR_QING_YI_SE) {
				result.append(" 清一色");
			}
			if (type == Constants_SiChuan.CHR_QI_DUI) {
				result.append(" 七对");
			}
			if (type == Constants_SiChuan.CHR_LONG_QI_DUI) {
				result.append(" 龙七对");
				has_long_qi_dui = true;
			}
			if (type == Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI) {
				result.append(" 双龙七对");
				has_long_qi_dui = true;
			}
			if (type == Constants_SiChuan.CHR_SAN_LONG_QI_DUI) {
				result.append(" 三龙七对");
				has_long_qi_dui = true;
			}
			if (type == Constants_SiChuan.CHR_JIANG_DUI) {
				result.append(" 将对");
			}
			if (type == Constants_SiChuan.CHR_YAO_JIU) {
				result.append(" 幺九");
			}
			if (type == Constants_SiChuan.CHR_MEN_QING) {
				result.append(" 门清");
			}
			if (type == Constants_SiChuan.CHR_ZHONG_ZHANG) {
				result.append(" 中张");
			}
			if (type == Constants_SiChuan.CHR_JIN_GOU_HU) {
				result.append(" 金钩胡");
			}
			if (type == Constants_SiChuan.CHR_WU_YAO_JI) {
				result.append(" 无幺鸡");
			}
		}

		if (!has_long_qi_dui) {
			if (finalGengCount[seat_index] > 0)
				result.append(" 根x" + finalGengCount[seat_index]);
		}

		return result.toString();
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean has_long_qi_dui = false;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_SiChuan.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_SiChuan.CHR_JIE_PAO) {
						result.append(" 接炮");
					}
					if (type == Constants_SiChuan.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_SiChuan.CHR_GANG_KAI) {
						result.append(" 杠上花");
					}
					if (type == Constants_SiChuan.CHR_DG_GANG_KAI) {
						result.append(" 点杠杠上花");
					}
					if (type == Constants_SiChuan.CHR_GANG_PAO) {
						result.append(" 杠上炮");
					}

					if (type == Constants_SiChuan.CHR_DUI_DUI_HU) {
						result.append(" 对对胡");
					}
					if (type == Constants_SiChuan.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_SiChuan.CHR_QI_DUI) {
						result.append(" 七对");
					}
					if (type == Constants_SiChuan.CHR_LONG_QI_DUI) {
						result.append(" 龙七对");
						has_long_qi_dui = true;
					}
					if (type == Constants_SiChuan.CHR_SHUANG_LONG_QI_DUI) {
						result.append(" 双龙七对");
						has_long_qi_dui = true;
					}
					if (type == Constants_SiChuan.CHR_SAN_LONG_QI_DUI) {
						result.append(" 三龙七对");
						has_long_qi_dui = true;
					}
					if (type == Constants_SiChuan.CHR_JIANG_DUI) {
						result.append(" 将对");
					}
					if (type == Constants_SiChuan.CHR_YAO_JIU) {
						result.append(" 幺九");
					}
					if (type == Constants_SiChuan.CHR_MEN_QING) {
						result.append(" 门清");
					}
					if (type == Constants_SiChuan.CHR_ZHONG_ZHANG) {
						result.append(" 中张");
					}
					if (type == Constants_SiChuan.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_SiChuan.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == Constants_SiChuan.CHR_JIN_GOU_HU) {
						result.append(" 金钩胡");
					}
					if (type == Constants_SiChuan.CHR_HAI_DI) {
						result.append(" 海底");
					}
					if (type == Constants_SiChuan.CHR_WU_YAO_JI) {
						result.append(" 无幺鸡");
					}
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (!has_long_qi_dui) {
					if (finalGengCount[player] > 0)
						result.append(" 根x" + finalGengCount[player]);
				}
			}

			if (an_gang_count[player] > 0 || zhi_gang_count[player] > 0 || wan_gang_count[player] > 0 || dian_gang_count[player] > 0)
				result.append(" (");

			boolean has_first = false;
			if (an_gang_count[player] > 0) {
				if (has_first) {
					result.append(" 暗杠x" + an_gang_count[player]);
				} else {
					result.append("暗杠x" + an_gang_count[player]);
					has_first = true;
				}
			}
			if (zhi_gang_count[player] > 0) {
				if (has_first) {
					result.append(" 明杠x" + zhi_gang_count[player]);
				} else {
					result.append("明杠x" + zhi_gang_count[player]);
					has_first = true;
				}
			}
			if (wan_gang_count[player] > 0) {
				if (has_first) {
					result.append(" 巴杠x" + wan_gang_count[player]);
				} else {
					result.append("巴杠x" + wan_gang_count[player]);
					has_first = true;
				}
			}
			if (dian_gang_count[player] > 0) {
				if (has_first) {
					result.append(" 点杠x" + dian_gang_count[player]);
				} else {
					result.append("点杠x" + dian_gang_count[player]);
					has_first = true;
				}
			}

			if (an_gang_count[player] > 0 || zhi_gang_count[player] > 0 || wan_gang_count[player] > 0 || dian_gang_count[player] > 0)
				result.append(")");

			GRR._result_des[player] = result.toString();
		}
	}
}
