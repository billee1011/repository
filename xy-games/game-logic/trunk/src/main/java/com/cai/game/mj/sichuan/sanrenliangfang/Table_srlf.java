package com.cai.game.mj.sichuan.sanrenliangfang;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;

@ThreeDimension
public class Table_srlf extends AbstractSiChuanMjTable {
	private static final long serialVersionUID = 1L;

	public Table_srlf(MJType type) {
		super(type);
	}

	@Override
	public void process_gang_score() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (win_order[i] != 0 || left_player_count == 1 || is_ting_state(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i)) {

				for (int w_index = 0; w_index < GRR._weave_count[i]; w_index++) {

					if (GRR._weave_items[i][w_index].weave_kind == GameConstants.WIK_GANG && GRR._weave_items[i][w_index].is_vavild) {
						int zhuan_yi_fen = 0;
						@SuppressWarnings("unused")
						int cbGangIndex = GRR._gang_score[i].gang_count++;

						if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_AN_GANG) {
							for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
								if (i == o_player)
									continue;

								if (GRR._weave_items[i][w_index].gang_gei_fen_valid[o_player] == false)
									continue;

								int score = 2 * GameConstants.CELL_SCORE;

								GRR._game_score[o_player] -= score;
								GRR._game_score[i] += score;
								zhuan_yi_fen += score;
							}

							_player_result.an_gang_count[i]++;
							GRR._gang_score[i].an_gang_count++;
						}

						if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_JIE_GANG) {
							if (has_rule(Constants_SiChuan.GAME_RULE_GUA_CHA)) {
								for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
									if (i == o_player)
										continue;

									if (GRR._weave_items[i][w_index].gang_gei_fen_valid[o_player] == false)
										continue;

									int score = GameConstants.CELL_SCORE;
									if (o_player == GRR._weave_items[i][w_index].provide_player)
										score *= 2;

									GRR._game_score[o_player] -= score;
									GRR._game_score[i] += score;
									zhuan_yi_fen += score;
								}
							} else {
								int score = 2 * GameConstants.CELL_SCORE;

								GRR._game_score[GRR._weave_items[i][w_index].provide_player] -= score;
								GRR._game_score[i] += score;
								zhuan_yi_fen += score;
							}

							_player_result.ming_gang_count[i]++;
							GRR._gang_score[i].ming_gang_count++;
						}

						if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_ADD_GANG) {
							for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
								if (i == o_player)
									continue;

								if (GRR._weave_items[i][w_index].gang_gei_fen_valid[o_player] == false)
									continue;

								int score = GameConstants.CELL_SCORE;

								GRR._game_score[o_player] -= score;
								GRR._game_score[i] += score;
								zhuan_yi_fen += score;
							}

							_player_result.ming_gang_count[i]++;
							GRR._gang_score[i].ming_gang_count++;
						}

						int zhuan_yi_seat = GRR._weave_items[i][w_index].gang_jie_pao_seat;
						if (zhuan_yi_seat != -1) {
							GRR._game_score[i] -= zhuan_yi_fen;
							GRR._game_score[zhuan_yi_seat] += zhuan_yi_fen;
						}
					}
				}
			}
		}
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

			analyse_state = FROM_NORMAL;
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index], cbCurrentCard, chr, card_type, seat_index)) {
				if (flag) {
					GRR._cards_index[seat_index][i] = 5;
				}

				int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
						cbCurrentCard);
				int fan_shu = get_fan_shu(chr, geng_count);

				// 清掉海底
				fan_shu--;

				int max_fan_shu = get_max_fan_shu();

				if (fan_shu > max_fan_shu) {
					fan_shu = max_fan_shu;
				}

				int score = 1 << fan_shu;

				if (score > max_score) {
					max_score = score;
					finallyFanShu[seat_index] = fan_shu;
				}
			}
		}

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

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		// 平胡
		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		long qi_dui = analyse_qi_xiao_dui(cbCardIndexTemp, weave_count);
		// 七对
		boolean can_win_qi_dui = (qi_dui != 0);
		// 龙七对
		boolean can_win_long_qi_dui = (qi_dui == Constants_SiChuan.CHR_LONG_QI_DUI);

		if (!can_win && !can_win_qi_dui) {
			return 0;
		}

		// 清一色
		boolean can_win_qing_yi_se = _logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weave_count);

		// 对对胡
		boolean can_win_dd_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		boolean is_jiang_yi_se = is_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		// 将对
		boolean can_win_jiang_dui = false;
		// 将七对
		boolean can_win_jiang_qi_dui = false;
		// 幺九
		boolean can_win_yao_jiu = false;
		if (has_rule(Constants_SiChuan.GAME_RULE_YAO_JIU_JIANG_DUI)) {
			can_win_jiang_dui = (can_win_dd_hu && is_jiang_yi_se);
			can_win_jiang_qi_dui = (can_win_long_qi_dui && is_jiang_yi_se);
			can_win_yao_jiu = AnalyseCardUtil.analyse_win_yao_jiu(cbCardIndexTemp, -1, magic_cards_index, 0)
					&& is_yao_jiu_weave(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
		}

		// 门清
		boolean can_win_men_qing = false;
		// 中张
		boolean can_win_zhong_zhang = false;
		if (has_rule(Constants_SiChuan.GAME_RULE_MEN_QING_ZHONG_ZHANG)) {
			can_win_men_qing = _logic.is_men_qing_b(weaveItems, weave_count);
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
			if (weave_count == 0 && _out_card_count == 1 && _out_card_player == GRR._banker_player && _seat_index != GRR._banker_player
					&& card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO) {
				can_win_di_hu = true;
			}
		}

		// 一条龙
		boolean can_win_yi_tiao_long = _logic.is_yi_tiao_long(cbCardIndexTemp, weave_count);

		// 金钩胡
		boolean can_win_jin_gou_hu = (_logic.get_card_count_by_index(cbCardIndexTemp) == 2);

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_KAI);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_PAO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QIANG_GANG);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_ZI_MO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIE_PAO);
		} else if (card_type == Constants_SiChuan.HU_CARD_TYPE_DIAN_GANG_GANG_KAI) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DG_GANG_KAI);
		}

		if (can_win_dd_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DUI_DUI_HU);
		}
		if (can_win_qing_yi_se) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
		}
		if (!can_win_long_qi_dui && can_win_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QI_DUI);
		}
		if (can_win_long_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_LONG_QI_DUI);
		}
		if (can_win_jiang_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIANG_DUI);
		}
		if (can_win_jiang_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIANG_QI_DUI);
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
		if (can_win_tian_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_TIAN_HU);
		}
		if (can_win_di_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DI_HU);
		}
		if (can_win_yi_tiao_long) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_YI_TIAO_LONG);
		}
		if (can_win_jin_gou_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIN_GOU_HU);
		}

		if (GRR._left_card_count == 0) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_HAI_DI);
		}

		if (has_rule(Constants_SiChuan.GAME_RULE_JIA_XIN_WU) && analyse_state == FROM_NORMAL) {
			boolean has_jia_xin_wu = false;

			int index_1 = _logic.switch_to_card_index(0x14);
			int index_2 = _logic.switch_to_card_index(0x15);
			int index_3 = _logic.switch_to_card_index(0x16);
			int index_4 = _logic.switch_to_card_index(0x24);
			int index_5 = _logic.switch_to_card_index(0x25);
			int index_6 = _logic.switch_to_card_index(0x26);

			int[] tmpCardIndex = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);

			if (cur_card == 0x15) {
				if (tmpCardIndex[index_1] > 0 && tmpCardIndex[index_2] > 0 && tmpCardIndex[index_3] > 0) {
					tmpCardIndex[index_1]--;
					tmpCardIndex[index_2]--;
					tmpCardIndex[index_3]--;

					has_jia_xin_wu = AnalyseCardUtil.analyse_win_by_cards_index(tmpCardIndex, -1, magic_cards_index, 0);

					if (has_jia_xin_wu) {
						jia_xin_wu_count[_seat_index]++;
					}
				}
			}

			if (cur_card == 0x25) {
				if (tmpCardIndex[index_4] > 0 && tmpCardIndex[index_5] > 0 && tmpCardIndex[index_6] > 0) {
					tmpCardIndex[index_4]--;
					tmpCardIndex[index_5]--;
					tmpCardIndex[index_6]--;

					has_jia_xin_wu = AnalyseCardUtil.analyse_win_by_cards_index(tmpCardIndex, -1, magic_cards_index, 0);

					if (has_jia_xin_wu) {
						jia_xin_wu_count[_seat_index]++;
					}
				}
			}

			if (has_jia_xin_wu) {
				chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIA_XIN_WU);
			}
		}

		int geng_count = get_geng_count(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index], GRR._weave_count[_seat_index], cur_card);
		int fan_shu = get_fan_shu(chiHuRight, geng_count);

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO || card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_PAO) {
			if (!has_rule(Constants_SiChuan.GAME_RULE_DIAN_PAO_KE_PING_HU) && card_type != Constants_SiChuan.HU_CARD_TYPE_GANG_PAO) {
				if (fan_shu <= 0) {
					chiHuRight.set_empty();
					return 0;
				}
			}
		}

		score_when_win[_seat_index] = 1 << fan_shu;

		return GameConstants.WIK_CHI_HU;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = GameConstants.CELL_SCORE;

		int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				GRR._chi_hu_card[seat_index][0]);
		int fan_shu = get_fan_shu(chr, geng_count);

		if (jia_xin_wu_count[seat_index] > 1) {
			fan_shu += (jia_xin_wu_count[seat_index] - 1);
		}

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

		int score = di_fen * (1 << fan_shu);

		if (zimo) {
			if (has_rule(Constants_SiChuan.GAME_RULE_ZM_JIA_DI))
				score += 1;

			if (has_rule(Constants_SiChuan.GAME_RULE_DGH_DIAN_PAO) && !chr.opr_and_long(Constants_SiChuan.CHR_DG_GANG_KAI).is_empty()) {
				// 点杠花（点炮）
				// 找到最后一个杠的提供者
				int p_index = -1;
				for (int w = GRR._weave_count[seat_index] - 1; w >= 0; w--) {
					if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_GANG) {
						p_index = GRR._weave_items[seat_index][w].provide_player;
						break;
					}
				}
				GRR._game_score[seat_index] += score;
				GRR._game_score[p_index] -= score;
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
				}
			}
		} else {
			GRR._game_score[seat_index] += score;
			GRR._game_score[provide_index] -= score;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public int get_fan_shu(ChiHuRight chr, int geng_count) {
		int fan = 0;

		boolean has_long_qi_dui = false;

		if (chr.opr_and_long(Constants_SiChuan.CHR_JIANG_DUI).is_empty() && !chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			fan += 1;
			if (has_rule(Constants_SiChuan.GAME_RULE_DUI_DUI_HU_LIANG_FAN)) {
				fan += 1;
			}
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			fan += 2;
		}
		if (chr.opr_and_long(Constants_SiChuan.CHR_JIANG_QI_DUI).is_empty() && !chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty()) {
			fan += 3;
			has_long_qi_dui = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIANG_DUI).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIANG_QI_DUI).is_empty()) {
			fan += 4;
			has_long_qi_dui = true;
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
		if (!chr.opr_and_long(Constants_SiChuan.CHR_YI_TIAO_LONG).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIN_GOU_HU).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIA_XIN_WU).is_empty()) {
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

		if (has_long_qi_dui) {
			fan += (geng_count - 1);
		} else {
			fan += geng_count;
		}

		return fan;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean has_jiang_dui = !GRR._chi_hu_rights[player].opr_and_long(Constants_SiChuan.CHR_JIANG_DUI).is_empty();
			boolean has_jiang_qi_dui = !GRR._chi_hu_rights[player].opr_and_long(Constants_SiChuan.CHR_JIANG_QI_DUI).is_empty();
			boolean has_long_qi_dui = false;

			if (dian_pao_count[player] > 0) {
				for (int j = 0; j < dian_pao_count[player]; j++) {
					result.append(" 点" + dian_pao_order[player][j] + "胡");
				}
			}

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

					if (!has_jiang_dui && type == Constants_SiChuan.CHR_DUI_DUI_HU) {
						result.append(" 对对胡");
					}
					if (type == Constants_SiChuan.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_SiChuan.CHR_QI_DUI) {
						result.append(" 七对");
					}
					if (!has_jiang_qi_dui && type == Constants_SiChuan.CHR_LONG_QI_DUI) {
						result.append(" 龙七对");
						has_long_qi_dui = true;
					}
					if (type == Constants_SiChuan.CHR_JIANG_DUI) {
						result.append(" 将对");
					}
					if (type == Constants_SiChuan.CHR_JIANG_QI_DUI) {
						result.append(" 将七对");
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
					if (type == Constants_SiChuan.CHR_JIA_XIN_WU) {
						result.append(" 夹心五x" + jia_xin_wu_count[player]);
					}
					if (type == Constants_SiChuan.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_SiChuan.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == Constants_SiChuan.CHR_YI_TIAO_LONG) {
						result.append(" 一条龙");
					}
					if (type == Constants_SiChuan.CHR_JIN_GOU_HU) {
						result.append(" 金钩胡");
					}
					if (type == Constants_SiChuan.CHR_HAI_DI) {
						result.append(" 海底");
					}
				}
			}

			if (an_gang_count[player] > 0) {
				result.append(" 暗杠x" + an_gang_count[player]);
			}
			if (zhi_gang_count[player] > 0) {
				result.append(" 直杠x" + zhi_gang_count[player]);
			}
			if (wan_gang_count[player] > 0) {
				result.append(" 弯杠x" + wan_gang_count[player]);
			}
			if (dian_gang_count[player] > 0) {
				result.append(" 点杠x" + dian_gang_count[player]);
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				int geng_count = get_geng_count(GRR._cards_index[player], GRR._weave_items[player], GRR._weave_count[player],
						GRR._chi_hu_card[player][0]);
				if (has_long_qi_dui) {
					if (geng_count > 1)
						result.append(" 根x" + (geng_count - 1));
				} else if (geng_count > 0) {
					result.append(" 根x" + geng_count);
				}
			}

			if (cha_da_jiao[player] || bei_cha_da_jiao[player]) {
				result.append(" 查大叫");
			}

			GRR._result_des[player] = result.toString();
		}
	}
}
