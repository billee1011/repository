package com.cai.game.mj.sichuan.guangan;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;

@ThreeDimension
public class Table_GuangAn extends AbstractSiChuanMjTable {
	private static final long serialVersionUID = 1L;

	public Table_GuangAn(MJType type) {
		super(type);
	}

	@Override
	protected void init_shuffle() {
		if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
			_repertory_card = new int[MJConstants.CARD_DATA_TIAO_TONG_ZFB.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_TIAO_TONG_ZFB);
		} else {
			_repertory_card = new int[MJConstants.CARD_DATA_WTT_ZFB.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_WTT_ZFB);
		}
	};

	@Override
	protected void onInitTable() {
		super.onInitTable();
		_handler_out_card_operate = new HandlerOutCardOperate_GuangAn();
		_handler_gang = new HandlerGang_GuangAn();
	}

	@Override
	public void process_gang_score() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int w_index = 0; w_index < GRR._weave_count[i]; w_index++) {
				if (GRR._weave_items[i][w_index].weave_kind == GameConstants.WIK_GANG) {
					if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_AN_GANG) {
						_player_result.an_gang_count[i]++;
						GRR._gang_score[i].an_gang_count++;
					}

					if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_JIE_GANG
							|| GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_ADD_GANG) {
						_player_result.ming_gang_count[i]++;
						GRR._gang_score[i].ming_gang_count++;
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
		analyse_state = FROM_MAX_COUNT;

		int di_fen = get_di_fen();

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

					int tmp_score = max_pai_xing_fen[i] * di_fen;
					if ((i == GRR._banker_player || j == GRR._banker_player) && hasRuleMaiMa)
						tmp_score *= 2;

					GRR._game_score[i] += tmp_score;
					GRR._game_score[j] -= tmp_score;

					if (bei_cha_da_jiao[j] == false) {
						// 飘赖统计。用来计算被查叫的次数
						bei_cha_da_jiao[j] = true;
						_player_result.piao_lai_count[j]++;
					}
				}
			}
		}

		if (hasRuleBiPai) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (win_order[i] != 0)
					continue;

				if (is_ting_state[i]) {
					for (int j = i; j < getTablePlayerNumber(); j++) {
						if (win_order[j] != 0)
							continue;

						if (is_ting_state[j]) {
							int beiLv = 1;
							if ((i == GRR._banker_player || j == GRR._banker_player) && hasRuleMaiMa)
								beiLv = 2;
							GRR._game_score[i] += (max_pai_xing_fen[i] - max_pai_xing_fen[j]) * beiLv * di_fen;
							GRR._game_score[j] -= (max_pai_xing_fen[i] - max_pai_xing_fen[j]) * beiLv * di_fen;
						}
					}
				}
			}
		}
	}

	@Override
	public int get_max_pai_xing_fen(int seat_index) {
		int max_score = -1;

		int cbCurrentCard = -1;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
				if (i < 9)
					continue;
			}

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
				int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
						cbCurrentCard);
				int fan_shu = get_fan_shu(chr, seat_index, geng_count);
				if (fan_shu > max_score) {
					max_pai_xing_desc[seat_index] = get_pai_xing_desc_str(chr, seat_index, geng_count);
					max_score = fan_shu;
				}
			}
		}

		finallyFanShu[seat_index] = max_score;

		return max_score;
	}

	public String get_pai_xing_desc_str(ChiHuRight chr, int seat_index, int geng_count) {
		StringBuilder result = new StringBuilder();

		int chrTypes = chr.type_count;

		boolean has_long_qi_dui = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty())
			has_long_qi_dui = true;

		boolean has_qi_dui = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty())
			has_qi_dui = true;

		boolean has_jin_gou = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIN_GOU_HU).is_empty())
			has_jin_gou = true;

		for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
			long type = chr.type_list[typeIndex];

			if (type == Constants_SiChuan.CHR_PING_HU) {
				result.append(" 平胡");
			}
			if (type == Constants_SiChuan.CHR_DUI_DUI_HU) {
				result.append(" 对对胡");
			}
			if (type == Constants_SiChuan.CHR_HUN_YI_SE) {
				result.append(" 混一色");
			}
			if (type == Constants_SiChuan.CHR_QING_YI_SE) {
				result.append(" 清一色");
			}
			if (type == Constants_SiChuan.CHR_QI_DUI && !has_long_qi_dui) {
				result.append(" 七对");
			}
			if (type == Constants_SiChuan.CHR_LONG_QI_DUI) {
				result.append(" 龙七对");
			}
			if (type == Constants_SiChuan.CHR_MEN_QING && !has_qi_dui) {
				result.append(" 门清");
			}
			if (type == Constants_SiChuan.CHR_BIAN_ZHANG) {
				result.append(" 边张");
			}
			if (type == Constants_SiChuan.CHR_KAN_ZHANG) {
				result.append(" 坎张");
			}
			if (type == Constants_SiChuan.CHR_DAN_DIAO && !has_qi_dui && !has_jin_gou) {
				result.append(" 单吊");
			}
			if (type == Constants_SiChuan.CHR_JIN_GOU_HU) {
				result.append(" 金钩钓");
			}
			if (type == Constants_SiChuan.CHR_XIAO_SAN_YUAN) {
				result.append(" 小三元");
			}
			if (type == Constants_SiChuan.CHR_DA_SAN_YUAN) {
				result.append(" 大三元");
			}
		}

		if (gang_fan[seat_index] > 0) {
			result.append(" 杠+" + gang_fan[seat_index]);
		}
		if (zfb_ke_fan[seat_index] > 0) {
			result.append(" 刻+" + zfb_ke_fan[seat_index]);
		}

		if (!has_long_qi_dui && geng_count > 0)
			result.append(" 根+" + geng_count);

		return result.toString();
	}

	@Override
	public int get_geng_count(int[] cards_index, WeaveItem[] weave_items, int weave_count, int hu_card) {
		int geng = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(hu_card))
			cbCardIndexTemp[_logic.switch_to_card_index(hu_card)]++;

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_PENG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 3;
			}
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] >= 4) {
				geng++;
			}
		}

		return geng;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (analyse_state == FROM_NORMAL || analyse_state == FROM_MAX_COUNT) {
			special_fan_shu[_seat_index] = 0;
			zfb_ke_fan[_seat_index] = 0;
		}

		ting_special_fan_shu[_seat_index] = 0;
		ting_zfb_ke_fan[_seat_index] = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(cur_card)) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int pai_se_count = _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);
		if (pai_se_count > 2)
			return 0;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		// 平胡
		boolean can_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		long qi_dui = analyse_qi_xiao_dui(cbCardIndexTemp, weave_count, cur_card, _seat_index);
		// 七对
		boolean can_qi_dui = (qi_dui != 0);

		if (analyse_state == FROM_NORMAL || analyse_state == FROM_MAX_COUNT)
			if (qi_dui != Constants_SiChuan.CHR_LONG_QI_DUI)
				special_fan_shu[_seat_index] = 0;

		boolean can_win = can_ping_hu || can_qi_dui;

		if (!can_win) {
			return 0;
		}

		boolean has_feng_pai = has_feng_pai(cbCardIndexTemp, weaveItems, weave_count);

		// 对对胡
		boolean can_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		// 混一色
		boolean can_hun_yi_se = pai_se_count == 1 && has_feng_pai;
		// 清一色
		boolean can_qing_yi_se = pai_se_count == 1 && !has_feng_pai;
		// 龙七对
		boolean can_long_qi_dui = (qi_dui == Constants_SiChuan.CHR_LONG_QI_DUI);
		// 平胡
		can_ping_hu = !can_dui_dui_hu && !can_hun_yi_se && !can_qing_yi_se && !can_qi_dui;

		int da_hu_count = 0;
		if (can_ping_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_PING_HU);
		}
		if (can_dui_dui_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DUI_DUI_HU);
			da_hu_count++;
		}
		if (can_hun_yi_se) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_HUN_YI_SE);
			da_hu_count++;
		}
		if (can_qing_yi_se) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
			da_hu_count++;
		}
		if (can_qi_dui && !can_long_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QI_DUI);
			da_hu_count++;
			if (cur_card >= 0x35) {
				ting_special_fan_shu[_seat_index] += 1;
				if (analyse_state == FROM_NORMAL || analyse_state == FROM_MAX_COUNT)
					special_fan_shu[_seat_index] += 1;
			}
		}
		if (can_long_qi_dui) {
			da_hu_count++;
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_LONG_QI_DUI);
		}

		if (da_hu_count >= 2) {
			ting_special_fan_shu[_seat_index] -= 2;
			if (analyse_state == FROM_NORMAL || analyse_state == FROM_MAX_COUNT)
				special_fan_shu[_seat_index] -= 2;
		}

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_ZI_MO && analyse_state != FROM_MAX_COUNT) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_ZI_MO);
		}
		if (_logic.is_men_qing_b(weaveItems, weave_count)) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_MEN_QING);
		}

		// 边张 坎张 单吊 判断
		long kan_bian_diao_result = check_yi_zhang_ying(cards_index, weaveItems, weave_count, cur_card);
		if (kan_bian_diao_result == Constants_SiChuan.CHR_BIAN_ZHANG) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_BIAN_ZHANG);
		} else if (kan_bian_diao_result == Constants_SiChuan.CHR_KAN_ZHANG) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_KAN_ZHANG);
		} else if (kan_bian_diao_result == Constants_SiChuan.CHR_DAN_DIAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DAN_DIAO);
		}

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_PAO);
		}
		if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_KAI || card_type == Constants_SiChuan.HU_CARD_TYPE_DIAN_GANG_GANG_KAI) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_GANG_KAI);
		}
		if (card_type == Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QIANG_GANG);
		}
		if (2 == _logic.get_card_count_by_index(cbCardIndexTemp)) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_JIN_GOU_HU);
		}
		if (GRR._left_card_count == 0 && analyse_state != FROM_MAX_COUNT) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_HAI_DI);
		}
		long san_yuan_result = check_da_xiao_san_yuan(cbCardIndexTemp, weaveItems, weave_count);
		if (san_yuan_result == Constants_SiChuan.CHR_XIAO_SAN_YUAN) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_XIAO_SAN_YUAN);
		}
		if (san_yuan_result == Constants_SiChuan.CHR_DA_SAN_YUAN) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DA_SAN_YUAN);
		}

		if (!can_qi_dui)
			process_ke_fan(cbCardIndexTemp, weaveItems, weave_count, _seat_index);

		return GameConstants.WIK_CHI_HU;
	}

	public long check_yi_zhang_ying(int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int cur_card) {
		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int count = 0;
		for (int i = 0; i < UniversalConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i]++;
			if (cbCardIndexTemp[i] <= 4 && analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount)) {
				count++;
				if (count > 1) {
					return 0;
				}
			}
			cbCardIndexTemp[i]--;
		}

		if (count == 1) {
			int color = _logic.get_card_color(cur_card);
			int value = _logic.get_card_value(cur_card);
			if (color == 3) {
				return Constants_SiChuan.CHR_DAN_DIAO;
			}
			if (value == 1 || value == 9) {
				return Constants_SiChuan.CHR_DAN_DIAO;
			} else if (value == 3) {
				int left_card_1 = cur_card - 2;
				int left_card_2 = cur_card - 1;
				int right_card_1 = cur_card + 1;
				int lc_index_1 = _logic.switch_to_card_index(left_card_1);
				int lc_index_2 = _logic.switch_to_card_index(left_card_2);
				int rc_index_1 = _logic.switch_to_card_index(right_card_1);
				if (cbCardIndexTemp[lc_index_1] > 0 && cbCardIndexTemp[lc_index_2] > 0) {
					cbCardIndexTemp[lc_index_1]--;
					cbCardIndexTemp[lc_index_2]--;

					boolean can_win = analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount);
					if (can_win)
						return Constants_SiChuan.CHR_BIAN_ZHANG;

					cbCardIndexTemp[lc_index_1]++;
					cbCardIndexTemp[lc_index_2]++;
				}
				if (cbCardIndexTemp[lc_index_2] > 0 && cbCardIndexTemp[rc_index_1] > 0) {
					cbCardIndexTemp[lc_index_2]--;
					cbCardIndexTemp[rc_index_1]--;

					boolean can_win = analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount);
					if (can_win)
						return Constants_SiChuan.CHR_KAN_ZHANG;

					cbCardIndexTemp[lc_index_2]++;
					cbCardIndexTemp[rc_index_1]++;
				}
			} else if (value == 7) {
				int left_card_1 = cur_card - 1;
				int right_card_1 = cur_card + 1;
				int right_card_2 = cur_card + 2;
				int lc_index_1 = _logic.switch_to_card_index(left_card_1);
				int rc_index_1 = _logic.switch_to_card_index(right_card_1);
				int rc_index_2 = _logic.switch_to_card_index(right_card_2);
				if (cbCardIndexTemp[rc_index_1] > 0 && cbCardIndexTemp[rc_index_2] > 0) {
					cbCardIndexTemp[rc_index_1]--;
					cbCardIndexTemp[rc_index_2]--;

					boolean can_win = analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount);
					if (can_win)
						return Constants_SiChuan.CHR_BIAN_ZHANG;

					cbCardIndexTemp[rc_index_1]++;
					cbCardIndexTemp[rc_index_2]++;
				}
				if (cbCardIndexTemp[lc_index_1] > 0 && cbCardIndexTemp[rc_index_1] > 0) {
					cbCardIndexTemp[lc_index_1]--;
					cbCardIndexTemp[rc_index_1]--;

					boolean can_win = analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount);
					if (can_win)
						return Constants_SiChuan.CHR_KAN_ZHANG;

					cbCardIndexTemp[lc_index_1]++;
					cbCardIndexTemp[rc_index_1]++;
				}
			} else {
				int left_card_1 = cur_card - 1;
				int right_card_1 = cur_card + 1;
				int lc_index_1 = _logic.switch_to_card_index(left_card_1);
				int rc_index_1 = _logic.switch_to_card_index(right_card_1);
				if (cbCardIndexTemp[lc_index_1] > 0 && cbCardIndexTemp[rc_index_1] > 0) {
					cbCardIndexTemp[lc_index_1]--;
					cbCardIndexTemp[rc_index_1]--;

					boolean can_win = analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount);
					if (can_win)
						return Constants_SiChuan.CHR_KAN_ZHANG;

					cbCardIndexTemp[lc_index_1]++;
					cbCardIndexTemp[rc_index_1]++;
				}
			}
		} else {
			return 0;
		}

		return Constants_SiChuan.CHR_DAN_DIAO;
	}

	public boolean analyse_chi_hu_card_simple(int[] cards_index, WeaveItem[] weaveItems, int weave_count) {
		boolean can_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, null, 0);
		long qi_dui = analyse_qi_xiao_dui(cards_index, weave_count);
		boolean can_qi_dui = (qi_dui != 0);
		boolean can_win = can_ping_hu || can_qi_dui;
		if (!can_win) {
			return false;
		}
		return true;
	}

	public void process_ke_fan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index) {
		boolean has_hz_gang = false;
		boolean has_fc_gang = false;
		boolean has_bb_gang = false;
		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		for (int i = 0; i < weaveCount; i++) {
			int card = weaveItems[i].center_card;
			int index = _logic.switch_to_card_index(card);
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				tmp_cards_index[index] += 4;
				if (card == 0x35) {
					has_hz_gang = true;
				} else if (card == 0x36) {
					has_fc_gang = true;
				} else if (card == 0x37) {
					has_bb_gang = true;
				}
			} else {
				tmp_cards_index[index] += 3;
			}
		}
		for (int i = 31; i < 34; i++) {
			if (tmp_cards_index[i] >= 3) {
				if (analyse_state == FROM_NORMAL || analyse_state == FROM_MAX_COUNT) {
					if (i == 31 && !has_hz_gang) {
						zfb_ke_fan[seat_index]++;
					} else if (i == 32 && !has_fc_gang) {
						zfb_ke_fan[seat_index]++;
					} else if (i == 33 && !has_bb_gang) {
						zfb_ke_fan[seat_index]++;
					}
				}

				if (i == 31 && !has_hz_gang) {
					ting_zfb_ke_fan[seat_index]++;
				} else if (i == 32 && !has_fc_gang) {
					ting_zfb_ke_fan[seat_index]++;
				} else if (i == 33 && !has_bb_gang) {
					ting_zfb_ke_fan[seat_index]++;
				}
			}
		}
	}

	public int get_di_fen() {
		int ruleValue = getRuleValue(Constants_SiChuan.GAME_RULE_DI_FENG);
		return ruleValue > 0 ? ruleValue : 1;
	}

	public long check_da_xiao_san_yuan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		for (int i = 0; i < weaveCount; i++) {
			int card = weaveItems[i].center_card;
			int index = _logic.switch_to_card_index(card);
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				tmp_cards_index[index] += 4;
			} else {
				tmp_cards_index[index] += 3;
			}
		}
		boolean all_four = true;
		for (int i = 31; i < 34; i++) {
			if (tmp_cards_index[i] < 3)
				return 0;
			if (tmp_cards_index[i] == 3)
				all_four = false;
		}
		if (all_four) {
			return Constants_SiChuan.CHR_DA_SAN_YUAN;
		} else {
			return Constants_SiChuan.CHR_XIAO_SAN_YUAN;
		}
	}

	public boolean has_feng_pai(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			int card = weaveItems[i].center_card;
			int color = _logic.get_card_color(card);
			if (color == 3)
				return true;
		}
		for (int i = 27; i < 34 && i < cards_index.length; i++) {
			if (cards_index[i] > 0)
				return true;
		}
		return false;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = get_di_fen();

		int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				GRR._chi_hu_card[seat_index][0]);
		int fan_shu = get_fan_shu(chr, seat_index, geng_count);

		finallyFanShu[seat_index] = fan_shu;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		int score = di_fen * fan_shu;

		if (zimo) {
			if (seat_index == GRR._banker_player && hasRuleMaiMa)
				score *= 2;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				if (GRR._win_order[i] != 0) {
					continue;
				}

				int tmp_score = score;
				if (i == GRR._banker_player && hasRuleMaiMa)
					tmp_score *= 2;

				GRR._game_score[seat_index] += tmp_score;
				GRR._game_score[i] -= tmp_score;
			}
		} else {
			int tmp_score = score;
			if ((seat_index == GRR._banker_player || provide_index == GRR._banker_player) && hasRuleMaiMa)
				tmp_score *= 2;

			GRR._game_score[seat_index] += tmp_score;
			GRR._game_score[provide_index] -= tmp_score;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public int get_fan_shu(ChiHuRight chr, int seat_index, int geng_count) {
		int fan = 0;

		boolean has_qi_dui = false;
		boolean has_jin_gou = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_PING_HU).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()) {
			fan += 7;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_HUN_YI_SE).is_empty()) {
			fan += 8;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()) {
			fan += 8;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty() && chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty()) {
			fan += 9;
			has_qi_dui = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty()) {
			fan += 14;
			has_qi_dui = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZI_MO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_MEN_QING).is_empty() && !has_qi_dui) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_BIAN_ZHANG).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_KAN_ZHANG).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_JIN_GOU_HU).is_empty()) {
			fan += 5;
			has_jin_gou = true;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DAN_DIAO).is_empty() && !has_qi_dui && !has_jin_gou) {
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_PAO).is_empty()) {
			fan += 5;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty()) {
			fan += 5;
			fan += 1;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_QIANG_GANG).is_empty()) {
			fan += 5;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_HAI_DI).is_empty()) {
			fan += 5;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_XIAO_SAN_YUAN).is_empty()) {
			fan += 8;
		}
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DA_SAN_YUAN).is_empty()) {
			fan += 10;
		}
		if (analyse_state == FROM_TING) {
			fan += ting_zfb_ke_fan[seat_index];
			fan += ting_special_fan_shu[seat_index];
		} else {
			fan += zfb_ke_fan[seat_index];
			fan += special_fan_shu[seat_index];
		}
		fan += gang_fan[seat_index];

		fan += geng_count;

		return fan;
	}

	@Override
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
				if (i < 9)
					continue;
			}

			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
		}

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

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			if (getTablePlayerNumber() == 2 || getTablePlayerNumber() == 3) {
				if (i < 9)
					continue;
			}

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;

				int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
						cbCurrentCard);
				// 能听的牌数据 有几番
				ting_pai_fan_shu[seat_index][ting_count][count] = get_fan_shu(chr, seat_index, geng_count);

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
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean has_long_qi_dui = false;
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (!GRR._chi_hu_rights[player].opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty())
					has_long_qi_dui = true;
			}

			boolean has_qi_dui = false;
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (!GRR._chi_hu_rights[player].opr_and_long(Constants_SiChuan.CHR_LONG_QI_DUI).is_empty()
						|| !GRR._chi_hu_rights[player].opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty())
					has_qi_dui = true;
			}

			boolean has_jin_gou = false;
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (!GRR._chi_hu_rights[player].opr_and_long(Constants_SiChuan.CHR_JIN_GOU_HU).is_empty())
					has_jin_gou = true;
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
					if (type == Constants_SiChuan.CHR_GANG_PAO) {
						result.append(" 杠上炮");
					}

					if (type == Constants_SiChuan.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_SiChuan.CHR_DUI_DUI_HU) {
						result.append(" 对对胡");
					}
					if (type == Constants_SiChuan.CHR_HUN_YI_SE) {
						result.append(" 混一色");
					}
					if (type == Constants_SiChuan.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_SiChuan.CHR_QI_DUI && !has_long_qi_dui) {
						result.append(" 七对");
					}
					if (type == Constants_SiChuan.CHR_LONG_QI_DUI) {
						result.append(" 龙七对");
					}
					if (type == Constants_SiChuan.CHR_MEN_QING && !has_qi_dui) {
						result.append(" 门清");
					}
					if (type == Constants_SiChuan.CHR_BIAN_ZHANG) {
						result.append(" 边张");
					}
					if (type == Constants_SiChuan.CHR_KAN_ZHANG) {
						result.append(" 坎张");
					}
					if (type == Constants_SiChuan.CHR_DAN_DIAO && !has_qi_dui && !has_jin_gou) {
						result.append(" 单吊");
					}
					if (type == Constants_SiChuan.CHR_JIN_GOU_HU) {
						result.append(" 金钩钓");
					}
					if (type == Constants_SiChuan.CHR_HAI_DI) {
						result.append(" 海底胡");
					}
					if (type == Constants_SiChuan.CHR_XIAO_SAN_YUAN) {
						result.append(" 小三元");
					}
					if (type == Constants_SiChuan.CHR_DA_SAN_YUAN) {
						result.append(" 大三元");
					}
				}
			}

			if (gang_fan[player] > 0 && GRR._win_order[player] != 0) {
				result.append(" 杠+" + gang_fan[player]);
			}
			if (zfb_ke_fan[player] > 0 && GRR._win_order[player] != 0) {
				result.append(" 刻+" + zfb_ke_fan[player]);
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				int geng_count = get_geng_count(GRR._cards_index[player], GRR._weave_items[player], GRR._weave_count[player],
						GRR._chi_hu_card[player][0]);
				if (!has_long_qi_dui && geng_count > 0) {
					result.append(" 根+" + geng_count);
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}
}
