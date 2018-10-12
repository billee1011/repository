package com.cai.game.hh.handler.czsrphz;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.Constants_CZ_SR;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.game.hh.HHTable;
import com.cai.game.hh.HHGameLogic.AnalyseItem;

public class Table_CZ_SR extends HHTable {

	private static final long serialVersionUID = -6411443691444147984L;

	public Table_CZ_SR() {
		super();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new HandlerDispatchCard_CZ_SR();
		_handler_out_card_operate = new HandlerOutCardOperate_CZ_SR();
		_handler_gang = new HandlerGang_CZ_SR();
		_handler_chi_peng = new HandlerChiPeng_CZ_SR();
		_handler_chuli_firstcards = new HandlerChuLiFirstCard_CZ_SR();
		_handler_dispath_firstcards = new HandlerDispatchFirstCard_CZ_SR();
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = false;
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG && dispatch == true)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& ((analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG && dispatch == true)
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		int basic_hu_xi = this.get_basic_hu_xi();
		if (max_hu_xi < basic_hu_xi) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == Constants_CZ_SR.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(Constants_CZ_SR.CHR_ZI_MO);
		} else if (card_type == Constants_CZ_SR.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(Constants_CZ_SR.CHR_JIE_PAO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_FAN_PAI) {
			chiHuRight.opr_or(Constants_CZ_SR.CHR_CHI_HU);
		}

		return cbChiHuKind;
	}

	protected int get_hu_xi_mei_tun() {
		if (has_rule(Constants_CZ_SR.GAME_RULE_3_XI_MEI_TUN))
			return 3;
		return 1;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int all_hu_xi = 0;
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}

		this._hu_xi[seat_index] = all_hu_xi;

		int calculate_score = 1 + (all_hu_xi - this.get_basic_hu_xi()) / this.get_hu_xi_mei_tun();

		int wFanShu = this.get_fan_shu(chr);

		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu * this.getTablePlayerNumber();
		}

		int lChiHuScore = wFanShu * calculate_score;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		} else {
			int s = lChiHuScore * this.getTablePlayerNumber();

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(Constants_CZ_SR.CHR_FANG_PAO);
		}

		GRR._provider[seat_index] = provide_index;
	}

	protected int get_xiang_gong_tun_count(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int tun = 0;

		int all_hu_xi = 0;
		for (int i = 0; i < weave_count; i++) {
			all_hu_xi += _logic.get_weave_hu_xi(weave_items[i]);
		}
		for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
			if (cards_index[j] == 4) {
				if (j < 10)
					all_hu_xi += 12;
				else
					all_hu_xi += 9;
			}
			if (cards_index[j] == 3) {
				if (j < 10)
					all_hu_xi += 6;
				else
					all_hu_xi += 3;
			}
		}

		tun = 1 + (all_hu_xi - this.get_basic_hu_xi()) / this.get_hu_xi_mei_tun();

		return tun;
	}

	protected int get_basic_hu_xi() {
		if (has_rule(Constants_CZ_SR.GAME_RULE_3_XI_QI_HU)) {
			return 3;
		} else if (has_rule(Constants_CZ_SR.GAME_RULE_6_XI_QI_HU)) {
			return 6;
		} else if (has_rule(Constants_CZ_SR.GAME_RULE_9_XI_QI_HU)) {
			return 8;
		}

		return 9;
	}

	protected int get_fan_shu(ChiHuRight chr) {
		int fan = 1;
		if (has_rule(Constants_CZ_SR.GAME_RULE_ZI_MO_DOUBLE)) {
			if (!chr.opr_and(Constants_CZ_SR.CHR_ZI_MO).is_empty())
				fan = 2;
		}
		if (!chr.opr_and(Constants_CZ_SR.CHR_TIAN_HU).is_empty())
			fan = 4;
		if (!chr.opr_and(Constants_CZ_SR.CHR_DI_HU).is_empty())
			fan = 4;
		return fan;
	}

	@Override
	public void set_result_describe(int seat_index) {
		int chr_count;
		long chr_type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");
			boolean hasFirst = false;

			chr_count = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chr_count; typeIndex++) {
				chr_type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (chr_type == Constants_CZ_SR.CHR_CHI_HU) {
						if (hasFirst) {
							gameDesc.append(" 胡");
						} else {
							gameDesc.append("胡");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_CZ_SR.CHR_ZI_MO) {
						if (hasFirst) {
							gameDesc.append(" 自摸");
						} else {
							gameDesc.append("自摸");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_CZ_SR.CHR_JIE_PAO) {
						if (hasFirst) {
							gameDesc.append(" 接炮");
						} else {
							gameDesc.append("接炮");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_CZ_SR.CHR_TIAN_HU) {
						if (hasFirst) {
							gameDesc.append(" 天胡");
						} else {
							gameDesc.append("天胡");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_CZ_SR.CHR_DI_HU) {
						if (hasFirst) {
							gameDesc.append(" 天胡");
						} else {
							gameDesc.append("天胡");
							hasFirst = true;
						}
					}
				} else if (chr_type == Constants_CZ_SR.CHR_FANG_PAO) {
					if (hasFirst) {
						gameDesc.append(" 放炮");
					} else {
						gameDesc.append("放炮");
						hasFirst = true;
					}
				}
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	protected int get_hong_pai_count(WeaveItem weaveItems[], int weaveCount, int cards_index[]) {
		int count = 0;

		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 3;
				break;
			}
		}

		int hand_cards_data[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, hand_cards_data);
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.color_hei(hand_cards_data[i]) == false)
				count++;
		}

		return count;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
		}

		return card;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x03, 0x04, 0x05, 0x08, 0x09, 0x0a, 0x13, 0x14, 0x15, 0x18, 0x19, 0x1a, 0x01, 0x01, 0x11, 0x06, 0x06,
				0x16, 0x07, 0x07 };
		int[] cards_of_player1 = new int[] { 0x03, 0x04, 0x05, 0x08, 0x09, 0x0a, 0x13, 0x14, 0x15, 0x18, 0x19, 0x1a, 0x01, 0x01, 0x11, 0x06, 0x06,
				0x16, 0x07, 0x07 };
		int[] cards_of_player2 = new int[] { 0x03, 0x04, 0x05, 0x08, 0x09, 0x0a, 0x13, 0x14, 0x15, 0x18, 0x19, 0x1a, 0x01, 0x01, 0x11, 0x06, 0x06,
				0x16, 0x07, 0x07 };
		int[] cards_of_player3 = new int[] { 0x03, 0x04, 0x05, 0x08, 0x09, 0x0a, 0x13, 0x14, 0x15, 0x18, 0x19, 0x1a, 0x01, 0x01, 0x11, 0x06, 0x06,
				0x16, 0x07, 0x07 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (this.getTablePlayerNumber() == 3) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			}
		} else {
			for (int j = 0; j < GameConstants.MAX_FPHZ_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 20) {
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
	public void countChiHuTimes(int _seat_index, boolean zimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}

		if (zimo) {
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];
		}

		if (!(chiHuRight.opr_and(Constants_CZ_SR.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_CZ_SR.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, Constants_CZ_SR.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean is_card_has_wei(int card) {
		boolean bTmp = false;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.cards_has_wei[i] != 0) {
				if (i == this._logic.switch_to_card_index(card)) {
					bTmp = true;
					break;
				}
			}
		}

		return bTmp;
	}

	// 是否听牌
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard, chr,
					Constants_CZ_SR.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
				return true;
		}
		return false;
	}
}
