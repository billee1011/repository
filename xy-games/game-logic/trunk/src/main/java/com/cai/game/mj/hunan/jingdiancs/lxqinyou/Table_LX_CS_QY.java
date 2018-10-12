package com.cai.game.mj.hunan.jingdiancs.lxqinyou;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_JingDian_CS;
import com.cai.common.constant.game.Constants_LX_CS_QY;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
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

public class Table_LX_CS_QY extends NewAbstractMjTable {
	private static final long serialVersionUID = 6668261272281498768L;

	private HandlerGangDispatchCard_LX_CS_QY _handler_gang_dispatch_card;
	private HandlerGangDispatchCard_THJ_LX_CS_QY _handler_gang_dispatch_card_thj;
	private HandlerPiao_LX_CS_QY _handler_piao;
	private HandlerXiaoHu_LX_CS_QY _handler_xiao_hu;

	public boolean[] operated_zt_si_xi = new boolean[getTablePlayerNumber()];
	public boolean[] operated_zt_lls = new boolean[getTablePlayerNumber()];

	public int zi_mo_count = 0;
	public int[] jie_pao_count = new int[getTablePlayerNumber()];

	public int[][] da_si_xi_card_index = new int[getTablePlayerNumber()][GameConstants.MAX_ZI];
	public int[] da_si_xi_count = new int[getTablePlayerNumber()];

	public boolean[] has_xiao_hu = new boolean[getTablePlayerNumber()];

	// 用来存储玩家是点了‘补’还是‘杠’
	public int gang_or_bu;
	public static final int GANG_STATE = 1;
	public static final int BU_STATE = 0;

	public int[][] lls_card_index = new int[getTablePlayerNumber()][GameConstants.MAX_ZI * 2];
	public int[] lls_count = new int[getTablePlayerNumber()];

	public int[] effective_zt_lls_count = new int[getTablePlayerNumber()];
	public int[] effective_zt_dsx_count = new int[getTablePlayerNumber()];

	public final int time_for_tou_zi_animation = 1000;
	public final int time_for_tou_zi_fade = 200;
	public final int time_for_run_delay = 500;

	/**
	 * 胡牌时玩家的输赢牌型分
	 */
	public int[] pai_xing_fen = new int[getTablePlayerNumber()];
	/**
	 * 胡牌时玩家的中鸟输赢分
	 */
	public int[] bird_score = new int[getTablePlayerNumber()];
	public boolean is_hai_di = false;
	public boolean analyse_from_ting = false;

	public Table_LX_CS_QY() {
		super(MJType.GAME_TYPE_MJ_LX_CS_QY);
	}

	public boolean check_da_si_xi(int[] cards_index, ChiHuRight chr, int seat_index) {
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 4) {
				chr._index_da_si_xi = i;

				da_si_xi_card_index[seat_index][da_si_xi_count[seat_index]++] = i;

				chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DA_SI_XI);

				return true;
			}
		}

		return false;
	}

	public boolean check_zt_da_si_xi(int[] cards_index, ChiHuRight chr, int seat_index, int card_data) {
		cards_index[_logic.switch_to_card_index(card_data)]++;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 4) {
				boolean can_win_zt_si_xi = true;

				for (int j = 0; j < da_si_xi_count[seat_index]; j++) {
					if (da_si_xi_card_index[seat_index][j] == i) {
						can_win_zt_si_xi = false;

						break;
					}
				}

				if (can_win_zt_si_xi) {
					chr._index_da_si_xi = i;

					da_si_xi_card_index[seat_index][da_si_xi_count[seat_index]++] = i;

					// TODO 中途四喜判断，不能把chr的值加入放到这里，因为如果点了过，并不能直接set_empty();
					// 因为其他的小胡并不是中途的，如果set_empty()会把之前的小胡类型也清空掉了，所以放在发牌处理的show_zt_si_xi方法里
					// chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZT_DA_SI_XI);

					cards_index[_logic.switch_to_card_index(card_data)]--;

					return true;
				}
			}
		}

		cards_index[_logic.switch_to_card_index(card_data)]--;

		return false;
	}

	public boolean check_ban_ban_hu(int[] cards_index, ChiHuRight chr) {
		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, hand_cards);

		for (int i = 0; i < hand_card_count; i++) {
			int card_value = _logic.get_card_value(hand_cards[i]);
			int card_color = _logic.get_card_color(hand_cards[i]);

			if ((card_value == 2 || card_value == 5 || card_value == 8) && card_color < 3)
				return false;
		}

		chr._show_all = true;
		chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_BAN_BAN_HU);
		return true;
	}

	public boolean check_que_yi_se(int cards_index[], ChiHuRight chr) {
		boolean exist[] = new boolean[3];

		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, hand_cards);

		for (int i = 0; i < hand_card_count; i++) {
			int card_color = _logic.get_card_color(hand_cards[i]);

			if (card_color > 2)
				continue;

			exist[card_color] = true;
		}

		int count = 0;

		for (int i = 0; i < 3; i++) {
			if (exist[i]) {
				count += 1;
			}
		}

		if (count < 3) {
			chr._show_all = true;
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QUE_YI_SE);
			return true;
		} else
			return false;
	}

	public boolean check_liu_liu_shun(int[] cards_index, ChiHuRight chr, int seat_index) {
		int index_a = -1;
		int index_b = -1;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] >= 3) {
				if (index_a == -1)
					index_a = i;
				else if (index_b == -1)
					index_b = i;
			}
			if (index_a != -1 && index_b != -1) {
				chr._index_liul_liu_shun_1 = index_a;
				chr._index_liul_liu_shun_2 = index_b;

				lls_card_index[seat_index][lls_count[seat_index]] = index_a;
				lls_card_index[seat_index][lls_count[seat_index] + 1] = index_b;

				lls_count[seat_index] += 2;

				chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_LIU_LIU_SHUN);
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查中途六六顺，和六六顺的两个索引值不能重复
	 * 
	 * @param cards_index
	 * @param chr
	 * @return
	 */
	public boolean check_zt_liu_liu_shun(int[] cards_index, ChiHuRight chr, int card_data, int seat_index) {
		cards_index[_logic.switch_to_card_index(card_data)]++;

		int index_a = -1;
		int index_b = -1;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] >= 3) {
				if (index_a == -1) {
					boolean finded = false;
					for (int j = 0; j < lls_count[seat_index]; j++) {
						if (i == lls_card_index[seat_index][j]) {
							finded = true;
							break;
						}
					}

					if (!finded) {
						index_a = i;
					}
				} else if (index_b == -1) {
					boolean finded = false;
					for (int j = 0; j < lls_count[seat_index]; j++) {
						if (i == lls_card_index[seat_index][j]) {
							finded = true;
							break;
						}
					}

					if (!finded) {
						index_b = i;
					}
				}
			}

			if (index_a != -1 && index_b != -1) {
				chr._index_zt_lls_1 = index_a;
				chr._index_zt_lls_2 = index_b;

				// TODO 中途四喜判断，不能把chr的值加入放到这里，因为如果点了过，并不能直接set_empty();
				// 因为其他的小胡并不是中途的，如果set_empty()会把之前的小胡类型也清空掉了，所以放在发牌处理的show_zt_si_xi方法里
				// chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZT_LLS);

				lls_card_index[seat_index][lls_count[seat_index]] = index_a;
				lls_card_index[seat_index][lls_count[seat_index] + 1] = index_b;

				lls_count[seat_index] += 2;

				cards_index[_logic.switch_to_card_index(card_data)]--;

				return true;
			}
		}

		cards_index[_logic.switch_to_card_index(card_data)]--;

		return false;
	}

	public boolean check_jie_jie_gao(int[] cards_index, ChiHuRight chr) {
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] >= 2) {
				int card = _logic.switch_to_card_data(i);
				int card_value = _logic.get_card_value(card);

				if (card_value <= 7) {
					if (cards_index[i + 1] >= 2 && cards_index[i + 2] >= 2) {
						chr._index_jie_jie_gao = i;
						chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_JIE_GAO);
						return true;
					}
				}
			}
		}

		return false;
	}

	public boolean check_san_tong(int[] cards_index, ChiHuRight chr) {
		for (int i = 0; i < 9; i++) {
			if (cards_index[i] >= 2 && cards_index[i + 9] >= 2 && cards_index[i + 18] >= 2) {
				chr._index_san_tong = i;
				chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_SAN_TONG);
				return true;
			}
		}

		return false;
	}

	public boolean check_yi_zhi_hua(int[] cards_index, ChiHuRight chr) {
		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < 9; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		int hand_card_count = _logic.get_card_count(tmp_cards_index, 0, 9);
		if (hand_card_count == 1 && tmp_cards_index[4] == 1) {
			chr._show_all = true;
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_YI_ZHI_HUA);
			return true;
		}

		for (int i = 9; i < 18; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		hand_card_count = _logic.get_card_count(tmp_cards_index, 9, 18);
		if (hand_card_count == 1 && tmp_cards_index[13] == 1) {
			chr._show_all = true;
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_YI_ZHI_HUA);
			return true;
		}

		for (int i = 18; i < 27; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		hand_card_count = _logic.get_card_count(tmp_cards_index, 18, 27);
		if (hand_card_count == 1 && tmp_cards_index[22] == 1) {
			chr._show_all = true;
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_YI_ZHI_HUA);
			return true;
		}

		int hua_count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (tmp_cards_index[i] == 0)
				continue;

			int card = _logic.switch_to_card_data(i);
			int card_value = _logic.get_card_value(card);

			if (card_value == 2 || card_value == 8)
				return false;

			if (card_value == 5)
				hua_count += tmp_cards_index[i];

			if (hua_count > 1)
				return false;
		}

		if (hua_count == 1) {
			chr._show_all = true;
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_YI_ZHI_HUA);
			return true;
		}

		return false;
	}

	public boolean check_jin_tong_yu_nv(int[] cards_index, ChiHuRight chr) {
		int index1 = _logic.switch_to_card_index(0x12);
		int index2 = _logic.switch_to_card_index(0x22);
		if (cards_index[index1] >= 2 && cards_index[index2] >= 2) {
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIN_TONG_YU_NV);
			return true;
		}

		return false;
	}

	public boolean check_hou_ba_lun(int[] cards_index, ChiHuRight chr) {
		int index1 = _logic.switch_to_card_index(0x28);
		if (cards_index[index1] >= 3) {
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_HOU_BA_LUN);
			return true;
		}

		return false;
	}

	public boolean check_mei_hua_san_nong(int[] cards_index, ChiHuRight chr) {
		int index1 = _logic.switch_to_card_index(0x25);
		if (cards_index[index1] >= 3) {
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_MEI_HUA_SAN_NONG);
			return true;
		}

		return false;
	}

	public boolean check_yi_dian_hong(int[] cards_index, ChiHuRight chr) {
		int index1 = _logic.switch_to_card_index(0x02);
		int index2 = _logic.switch_to_card_index(0x05);
		int index3 = _logic.switch_to_card_index(0x08);

		int index4 = _logic.switch_to_card_index(0x12);
		int index5 = _logic.switch_to_card_index(0x15);
		int index6 = _logic.switch_to_card_index(0x18);

		int index7 = _logic.switch_to_card_index(0x22);
		int index8 = _logic.switch_to_card_index(0x25);
		int index9 = _logic.switch_to_card_index(0x28);

		int count = cards_index[index1] + cards_index[index2] + cards_index[index3];
		count += cards_index[index4] + cards_index[index5] + cards_index[index6];
		count += cards_index[index7] + cards_index[index8] + cards_index[index9];

		if (count == 1) {
			chr._show_all = true;
			chr.opr_or_jd_cs(Constants_LX_CS_QY.CHR_YI_DIAN_HONG);
			return true;
		}

		return false;
	}

	public int analyse_chi_hu_card_xiaohu(int cards_index[], ChiHuRight chiHuRight, int seat_index) {
		chiHuRight.reset_card();
		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		boolean has_da_si_xi = false;
		boolean has_ban_ban_hu = false;
		boolean has_que_yi_se = false;
		boolean has_liu_liu_shun = false;
		boolean has_jie_jie_gao = false;
		boolean has_san_tong = false;
		boolean has_yi_zhi_hua = false;

		boolean has_jin_tong_yu_nv = false;
		boolean has_hou_ba_lun = false;
		boolean has_mei_hua_san_nong = false;
		boolean has_yi_dian_hong = false;

		if (true || has_rule(Constants_LX_CS_QY.GAME_RULE_DA_SI_XI))
			has_da_si_xi = check_da_si_xi(cbCardIndexTemp, chiHuRight, seat_index);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_BAN_BAN_HU))
			has_ban_ban_hu = check_ban_ban_hu(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_QUE_YI_SE))
			has_que_yi_se = check_que_yi_se(cbCardIndexTemp, chiHuRight);

		if (true || has_rule(Constants_LX_CS_QY.GAME_RULE_LIU_LIU_SHUN))
			has_liu_liu_shun = check_liu_liu_shun(cbCardIndexTemp, chiHuRight, seat_index);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_JIE_JIE_GAO))
			has_jie_jie_gao = check_jie_jie_gao(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_SAN_TONG))
			has_san_tong = check_san_tong(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_YI_ZHI_HUA))
			has_yi_zhi_hua = check_yi_zhi_hua(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_JIN_TONG_YU_NV))
			has_jin_tong_yu_nv = check_jin_tong_yu_nv(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_HOU_BA_LUN))
			has_hou_ba_lun = check_hou_ba_lun(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_MEI_HUA_SAN_NONG))
			has_mei_hua_san_nong = check_mei_hua_san_nong(cbCardIndexTemp, chiHuRight);

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_YI_DIAN_HONG))
			has_yi_dian_hong = check_yi_dian_hong(cbCardIndexTemp, chiHuRight);

		if (has_da_si_xi || has_ban_ban_hu || has_que_yi_se || has_liu_liu_shun || has_jie_jie_gao || has_san_tong || has_yi_zhi_hua
				|| has_jin_tong_yu_nv || has_hou_ba_lun || has_mei_hua_san_nong || has_yi_dian_hong)
			cbChiHuKind = GameConstants.WIK_CHI_HU;

		return cbChiHuKind;
	}

	@Override
	public int analyse_qi_shou_hu_pai(int cards_index[], WeaveItem weaveItem[], int weaveCount, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		boolean has_big_win = false;

		long qxd = is_qi_xiao_dui_qi_shou(cards_index, weaveItem, weaveCount);

		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or_jd_cs(qxd);
			has_big_win = true;
		}

		if (_logic.is_jiangjiang_hu_qi_shou(cards_index, weaveItem, weaveCount)) {
			chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIANG_JIANG_HU);
			has_big_win = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
		boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);

		if (bValue == false) {
			if (has_big_win == false) {
				// TODO 不能直接重置CHR
				// chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		// if (_logic.is_dan_diao(cards_index, cur_card)) {
		// chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QUAN_QIU_REN);
		// has_big_win = true;
		// }

		if (_logic.is_qing_yi_se_qishou(cards_index, weaveItem, weaveCount)) {
			chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QING_YI_SE);
			has_big_win = true;
		}

		boolean has_men_qing = false;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_MEN_QING_ZI_MO) || has_rule(Constants_LX_CS_QY.GAME_RULE_MEN_QING_JIE_PAO)) {
			if (_logic.is_men_qing(weaveItem, weaveCount) != GameConstants.WIK_NULL) {
				has_men_qing = true;
			}
		}

		boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);

		if (is_peng_peng_hu) {
			boolean exist_eat = exist_eat(weaveItem, weaveCount);
			if (!exist_eat) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_PENG_PENG_HU);
				has_big_win = true;
			}
		}

		boolean has_jia_jiang_hu = false;
		if (getRuleValue(Constants_LX_CS_QY.GAME_RULE_JIA_JIANG_HU) != 0) {
			if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI || card_type == Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG
					|| card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO || is_hai_di) {
				if (!has_big_win && !has_258) {
					chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIA_JIANG_HU);
					has_jia_jiang_hu = true;
					has_men_qing = false;
				}
			}
		}

		if (has_big_win || has_jia_jiang_hu) {
			if (has_men_qing)
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_MEN_QING);

			if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_KAI);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_PAO);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QIANG_GANG);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				}
			} else {
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55 && _out_card_count == 1 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68 && _out_card_count == 1 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81 && _out_card_count == 1 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				}
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
			}

			return GameConstants.WIK_CHI_HU;
		}

		if (has_258) {
			if (has_men_qing)
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_MEN_QING);

			if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_KAI);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_PAO);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QIANG_GANG);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				}
			} else {
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55 && _out_card_count == 1)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68 && _out_card_count == 1)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81 && _out_card_count == 1)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				}
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
			}
			return GameConstants.WIK_CHI_HU;
		}

		// TODO 不能直接重置CHR
		// chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItem, int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		boolean has_big_win = false;

		long qxd = is_qi_xiao_dui(cards_index, weaveItem, weaveCount, cur_card);

		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or_jd_cs(qxd);
			has_big_win = true;
		}

		if (_logic.is_jiangjiang_hu(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIANG_JIANG_HU);
			has_big_win = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (bValue == false) {
			if (has_big_win == false) {
				// TODO 不能直接重置CHR
				// chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (_logic.is_dan_diao(cards_index, cur_card)) {
			if (getRuleValue(Constants_LX_CS_QY.GAME_RULE_ALL_OPEN) == 1 || has_258) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QUAN_QIU_REN);
				has_big_win = true;
			}
		}

		boolean has_men_qing = false;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_MEN_QING_ZI_MO)
				&& (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO || card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI)) {
			if (_logic.is_men_qing(weaveItem, weaveCount) != GameConstants.WIK_NULL) {
				has_men_qing = true;
			}
		} else if (has_rule(Constants_LX_CS_QY.GAME_RULE_MEN_QING_JIE_PAO)) {
			if (_logic.is_men_qing(weaveItem, weaveCount) != GameConstants.WIK_NULL) {
				has_men_qing = true;
			}
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItem, weaveCount, cur_card)) {
			chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QING_YI_SE);
			has_big_win = true;
		}

		boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (is_peng_peng_hu) {
			boolean exist_eat = exist_eat(weaveItem, weaveCount);
			if (!exist_eat) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_PENG_PENG_HU);
				has_big_win = true;
			}
		}

		boolean has_jia_jiang_hu = false;
		if (getRuleValue(Constants_LX_CS_QY.GAME_RULE_JIA_JIANG_HU) != 0) {
			if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI || card_type == Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG
					|| card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO || is_hai_di) {
				if (!has_big_win && !has_258) {
					chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIA_JIANG_HU);
					has_jia_jiang_hu = true;
					has_men_qing = false;
				}
			}
		}

		if (has_big_win || has_jia_jiang_hu) {
			if (has_men_qing)
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_MEN_QING);

			if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				if (!has_jia_jiang_hu)
					chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_KAI);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
				if (!has_jia_jiang_hu)
					chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_PAO);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG) {
				if (!has_jia_jiang_hu)
					chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QIANG_GANG);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				}
			} else {
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55 && _out_card_count == 1 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68 && _out_card_count == 1 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81 && _out_card_count == 1 && !has_jia_jiang_hu)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				}
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
			}

			return GameConstants.WIK_CHI_HU;
		}

		if (has_258) {
			if (has_men_qing)
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_MEN_QING);

			if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_KAI);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_GANG_PAO);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_QIANG_GANG);
			} else if (card_type == Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_ZI_MO);
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_TIAN_HU);
				}
			} else {
				if (getTablePlayerNumber() == 4) {
					if (GRR._left_card_count == 55 && _out_card_count == 1)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 3) {
					if (GRR._left_card_count == 68 && _out_card_count == 1)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				} else if (getTablePlayerNumber() == 2) {
					if (GRR._left_card_count == 81 && _out_card_count == 1)
						chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_DI_HU);
				}
				chiHuRight.opr_or_jd_cs(Constants_LX_CS_QY.CHR_JIE_PAO);
			}
			return GameConstants.WIK_CHI_HU;
		}

		// TODO 不能直接重置CHR
		// chiHuRight.set_empty();
		return GameConstants.WIK_NULL;
	}

	@Override
	protected boolean on_game_start() {
		return false;
	}

	@Override
	public boolean on_game_start_new() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
		}

		if (has_rule(Constants_LX_CS_QY.GAME_RULE_PIAO_FEN)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
			return true;
		}

		return on_game_start_real();
	}

	public boolean on_game_start_real() {
		_game_status = GameConstants.GS_MJ_PLAY;

		zi_mo_count = 0;
		jie_pao_count = new int[getTablePlayerNumber()];

		operated_zt_si_xi = new boolean[getTablePlayerNumber()];
		operated_zt_lls = new boolean[getTablePlayerNumber()];

		da_si_xi_count = new int[getTablePlayerNumber()];
		da_si_xi_card_index = new int[getTablePlayerNumber()][GameConstants.MAX_ZI];

		lls_count = new int[getTablePlayerNumber()];
		lls_card_index = new int[getTablePlayerNumber()][GameConstants.MAX_ZI * 2];

		effective_zt_lls_count = new int[getTablePlayerNumber()];
		effective_zt_dsx_count = new int[getTablePlayerNumber()];

		has_xiao_hu = new boolean[getTablePlayerNumber()];

		pai_xing_fen = new int[getTablePlayerNumber()];
		bird_score = new int[getTablePlayerNumber()];

		is_hai_di = false;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			PlayerStatus playerStatus = _playerStatus[i];

			int action = analyse_chi_hu_card_xiaohu(GRR._cards_index[i], GRR._start_hu_right[i], i);

			if (action != GameConstants.WIK_NULL) {
				playerStatus.add_action(GameConstants.WIK_XIAO_HU);
				_game_status = GameConstants.GS_MJ_XIAOHU;
			} else {
				GRR._start_hu_right[i].set_empty();
			}
		}

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

				cards.addItem(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);

		if (_cur_round == 1) {
			load_player_info_data(roomResponse);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
			// WalkerGeek 庄家第一张发牌
			if (i == GRR._banker_player) {
				_send_card_data = hand_cards[i][13];
			}
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player)
				continue;

			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		if (_game_status == GameConstants.GS_MJ_XIAOHU) {
			exe_xiao_hu(_current_player);
		} else {
			exe_qi_shou(_current_player, GameConstants.WIK_NULL);
		}
		// exe_dispatch_card_xiaohu(_current_player);

		return _game_status == GameConstants.GS_MJ_XIAOHU;
	}

	@Override
	public void runnable_xiao_hu(int seat_index, boolean is_dispatch) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

		boolean change_handler = true;
		int cards[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			change_player_status(i, GameConstants.INVALID_VALUE);

			if (GRR._start_hu_right[i].is_valid()) {
				if (change_handler && is_dispatch == false && i != _cur_banker) {
					int hand_card_count_zhuang = _logic.switch_to_cards_data(GRR._cards_index[_cur_banker], cards);

					operate_player_cards(_cur_banker, hand_card_count_zhuang, cards, 0, null);
				}

				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

				operate_player_cards(i, hand_card_count, cards, 0, null);

				operate_show_card(i, GameConstants.Show_Card_XiaoHU, 0, null, GameConstants.INVALID_SEAT);

				change_handler = false;
			}
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		if (is_dispatch) {
			exe_qi_shou(seat_index, GameConstants.WIK_NULL);
		}
	}

	/**
	 * //执行小胡发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	// public boolean exe_dispatch_card_xiaohu(int seat_index) {
	// // 发牌
	// set_handler(_handler_dispatchCard_xiaohu_jingdian_cs);
	// _handler_dispatchCard_xiaohu_jingdian_cs.reset_status(seat_index,
	// GameConstants.WIK_NULL);
	// _handler.exe(this);
	// return true;
	// }

	@Override
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_from_ting = true;

		PerformanceTimer timer = new PerformanceTimer();

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;

		// TODO 处理一种特殊情况下的听牌数据，刚发第一张牌时，获取听牌数据，剩余牌为55时，特殊处理一下
		GRR._left_card_count--;
		for (int i = 0; i < l; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		GRR._left_card_count++;

		if (count == l) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		analyse_from_ting = false;

		return count;
	}

	@Override
	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}

		return false;
	}

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				return (int) Constants_LX_CS_QY.CHR_SHH_QI_XIAO_DUI;
			}
			return (int) Constants_LX_CS_QY.CHR_HH_QI_XIAO_DUI;
		} else {
			return (int) Constants_LX_CS_QY.CHR_QI_XIAO_DUI;
		}
	}

	public int is_qi_xiao_dui_qi_shou(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				return (int) Constants_LX_CS_QY.CHR_SHH_QI_XIAO_DUI;
			}
			return (int) Constants_LX_CS_QY.CHR_HH_QI_XIAO_DUI;
		} else {
			return (int) Constants_LX_CS_QY.CHR_QI_XIAO_DUI;
		}
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];

			if (gang_or_bu == GANG_STATE) {
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_LX_CS_QY.HU_CARD_TYPE_QIANG_GANG, i);
			} else {
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_LX_CS_QY.HU_CARD_TYPE_JIE_PAO, i);
			}

			if (action != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);
				bAroseAction = true;
			}
		}

		if (bAroseAction) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public boolean estimate_player_fan_pai_response_thj(int seat_index, int provide_index, int card, int[][] special_player_cards, int c) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = _playerStatus[seat_index];

		int action = GameConstants.WIK_NULL;

		if (playerStatus.lock_huan_zhang() == false) {
			if (seat_index == (provide_index + 1) % getTablePlayerNumber()) {
				action = _logic.check_chi(GRR._cards_index[seat_index], card);

				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				if (_playerStatus[seat_index].has_action()) {
					bAroseAction = true;
				}
			}

			action = _logic.check_peng(GRR._cards_index[seat_index], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);

				bAroseAction = true;
			}
		}

		if (GRR._left_card_count > 1) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				if (playerStatus.lock_huan_zhang() == false) {
					playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
					playerStatus.add_bu_zhang(card, provide_index, 1);
				}

				boolean can_gang = false;
				if (GRR._left_card_count > 2) {
					int bu_index = _logic.switch_to_card_index(card);
					int save_count = GRR._cards_index[seat_index][bu_index];
					GRR._cards_index[seat_index][bu_index] = 0;

					int cbWeaveIndex = GRR._weave_count[seat_index];

					GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
					GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
					GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
					GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provide_index;
					GRR._weave_count[seat_index]++;

					if (playerStatus.lock_huan_zhang()) {
						boolean has_huan_zhang = check_gang_huan_zhang(seat_index, card);
						can_gang = !has_huan_zhang;
					} else {
						can_gang = is_cs_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
								seat_index);
					}

					GRR._weave_count[seat_index] = cbWeaveIndex;
					GRR._cards_index[seat_index][bu_index] = save_count;

					if (can_gang == true) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, provide_index, 1);
					}
				}

				bAroseAction = true;
			}
		}

		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card,
				GRR._chi_hu_rights[seat_index], Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO, seat_index);

		if (action != 0) {
			special_player_cards[seat_index][c] += GameConstants.CARD_ESPECIAL_TYPE_CAN_WIN;

			jie_pao_count[seat_index]++;

			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provide_index);
			}

			bAroseAction = true;
		}

		return bAroseAction;
	}

	public boolean estimate_player_fan_pai_response(int seat_index, int provide_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = _playerStatus[seat_index];

		int action = GameConstants.WIK_NULL;

		if (playerStatus.lock_huan_zhang() == false) {
			if (seat_index == (provide_index + 1) % getTablePlayerNumber()) {
				action = _logic.check_chi(GRR._cards_index[seat_index], card);

				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				if (_playerStatus[seat_index].has_action()) {
					bAroseAction = true;
				}
			}

			action = _logic.check_peng(GRR._cards_index[seat_index], card);
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);

				bAroseAction = true;
			}
		}

		if (GRR._left_card_count > 1) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				if (playerStatus.lock_huan_zhang() == false) {
					playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
					playerStatus.add_bu_zhang(card, provide_index, 1);
				}

				boolean can_gang = false;
				if (GRR._left_card_count > 2) {
					int bu_index = _logic.switch_to_card_index(card);
					int save_count = GRR._cards_index[seat_index][bu_index];
					GRR._cards_index[seat_index][bu_index] = 0;

					int cbWeaveIndex = GRR._weave_count[seat_index];

					GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
					GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
					GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
					GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provide_index;
					GRR._weave_count[seat_index]++;

					if (playerStatus.lock_huan_zhang()) {
						boolean has_huan_zhang = check_gang_huan_zhang(seat_index, card);
						can_gang = !has_huan_zhang;
					} else {
						can_gang = is_cs_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
								seat_index);
					}

					GRR._weave_count[seat_index] = cbWeaveIndex;
					GRR._cards_index[seat_index][bu_index] = save_count;

					if (can_gang == true) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, provide_index, 1);
					}
				}

				bAroseAction = true;
			}
		}

		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card,
				GRR._chi_hu_rights[seat_index], Constants_LX_CS_QY.HU_CARD_TYPE_GANG_PAO, seat_index);

		if (action != 0) {
			jie_pao_count[seat_index]++;

			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provide_index);
			}

			bAroseAction = true;
		}

		return bAroseAction;
	}

	public boolean check_gang_huan_zhang(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];

		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		int hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);

		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != hu_cards[j]) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean estimate_player_out_card_response(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (_playerStatus[i].lock_huan_zhang() == false) {
				if ((seat_index + 1) % getTablePlayerNumber() == i) {
					action = _logic.check_chi(GRR._cards_index[i], card);
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
			}

			if (GRR._left_card_count > 0) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (playerStatus.lock_huan_zhang() == false) {
						playerStatus.add_action(GameConstants.WIK_BU_ZHNAG);
						playerStatus.add_bu_zhang(card, seat_index, 1);
					}

					boolean can_gang = false;
					if (GRR._left_card_count > 2) {
						int bu_index = _logic.switch_to_card_index(card);
						int save_count = GRR._cards_index[i][bu_index];
						GRR._cards_index[i][bu_index] = 0;

						int cbWeaveIndex = GRR._weave_count[i];

						GRR._weave_items[i][cbWeaveIndex].public_card = 0;
						GRR._weave_items[i][cbWeaveIndex].center_card = card;
						GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
						GRR._weave_count[i]++;

						if (playerStatus.lock_huan_zhang()) {
							boolean has_huan_zhang = check_gang_huan_zhang(i, card);
							can_gang = !has_huan_zhang;
						} else {
							can_gang = is_cs_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);
						}

						GRR._cards_index[i][bu_index] = save_count;
						GRR._weave_count[i] = cbWeaveIndex;

						if (can_gang == true) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = Constants_LX_CS_QY.HU_CARD_TYPE_JIE_PAO;

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

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

	public boolean is_cs_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_from_ting = true;

		int handcount = _logic.get_card_count_by_index(cards_index);
		if (handcount == 1) {
			return true;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_LX_CS_QY.HU_CARD_TYPE_ZI_MO, seat_index)) {
				analyse_from_ting = false;
				return true;
			}
		}

		return false;
	}

	public int get_niao_card_num(boolean is_hai_di) {
		int num = 0;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUA_1_NIAO))
			num = 1;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUA_2_NIAO))
			num = 2;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUA_4_NIAO))
			num = 4;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUA_6_NIAO))
			num = 6;

		if (is_hai_di)
			return num;

		if (num > GRR._left_card_count)
			num = GRR._left_card_count;

		return num;
	}

	public int get_chi_hu_action_rank_cs(ChiHuRight chiHuRight) {
		int wFanShu = 0;

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_MEN_QING)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_MEN_QING))
				wFanShu += 6;
		}

		boolean has_jia_jiang_hu = false;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_JIA_JIANG_HU)).is_empty()) {
			has_jia_jiang_hu = true;
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_JIA_JIANG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_PENG_PENG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_PENG_PENG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_JIANG_JIANG_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_JIANG_JIANG_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_QING_YI_SE)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_QING_YI_SE))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_HAI_DI_LAO_YUE)).is_empty() && !has_jia_jiang_hu) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_HAI_DI_LAO_YUE))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_HAI_DI_PAO)).is_empty() && !has_jia_jiang_hu) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_HAI_DI_PAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_QI_XIAO_DUI))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_HH_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 12;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_HH_QI_XIAO_DUI))
				wFanShu += 12;
		}
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_GANG_KAI)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_GANG_KAI))
				wFanShu += 6;
		}
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_QIANG_GANG)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_QIANG_GANG))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_GANG_PAO)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_GANG_PAO))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_QUAN_QIU_REN)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_QUAN_QIU_REN))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_SHH_QI_XIAO_DUI)).is_empty()) {
			wFanShu += 18;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_SHH_QI_XIAO_DUI))
				wFanShu += 18;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_TIAN_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_TIAN_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_DI_HU)).is_empty()) {
			wFanShu += 6;
			if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_DI_HU))
				wFanShu += 6;
		}

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_DA_SI_XI)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_BAN_BAN_HU)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_QUE_YI_SE)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_LIU_LIU_SHUN)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_JIE_JIE_GAO)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_SAN_TONG)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_YI_ZHI_HUA)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_ZT_DA_SI_XI)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_ZT_LLS)).is_empty())
			wFanShu += 2;

		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_JIN_TONG_YU_NV)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_HOU_BA_LUN)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_MEI_HUA_SAN_NONG)).is_empty())
			wFanShu += 2;
		if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_YI_DIAN_HONG)).is_empty())
			wFanShu += 2;

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_JIE_PAO)).is_empty()) {
				wFanShu = 1;
				if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_JIE_PAO)) {
					wFanShu = 2;
				}
			}

		}

		if (wFanShu == 0) {
			if (!(chiHuRight.opr_and_long(Constants_LX_CS_QY.CHR_ZI_MO)).is_empty()) {
				wFanShu = 2;
				if (chiHuRight.is_mul_long(Constants_LX_CS_QY.CHR_ZI_MO)) {
					wFanShu = 4;
				}
			}
		}

		return wFanShu;
	}

	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public boolean on_handler_game_finish(int seat_index, int reason) {
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

		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

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
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];
					}
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];

				// GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			int cards[] = new int[GRR._left_card_count];// 显示剩余的牌数据
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					int card = GRR._player_niao_cards[i][j];
					if (card < GameConstants.DING_NIAO_INVALID) {
						card += GameConstants.DING_NIAO_INVALID;
					}
					pnc.addItem(card);
				}

				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					int card = GRR._player_niao_cards_fei[i][j];
					if (card < GameConstants.DING_NIAO_INVALID) {
						card += GameConstants.FEI_NIAO_INVALID;
					}
					pnc.addItem(card);
				}
				// 每个玩家的中鸟的牌数据，包括定鸟和飞鸟
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

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
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);
				game_end.addProvidePlayer(GRR._provider[i]);

				if (is_mj_type(GameConstants.GAME_TYPE_THJ_JD_CS)) {
					// 胡牌分数
					game_end.addJettonScore(pai_xing_fen[i]);
					// 中鸟计分
					game_end.addCardType(bird_score[i]);
				}
				// 飘分
				game_end.addGangScore(_player_result.pao[i]);
				// 小胡分数
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				// 总结算
				game_end.addGameScore(GRR._game_score[i] + GRR._start_hu_score[i]);

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
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
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

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return true;
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

	protected int get_seat(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
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
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_null_seat_for_two_player(seat_index);
				break;
			case 2:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				seat = get_null_seat_for_two_player((seat_index + 1) / getTablePlayerNumber());
				break;
			}
		}
		return seat;
	}

	/**
	 * 二人场以seat_index为视角的下一个空座位
	 * 
	 * @param seat_index
	 *            当前座位
	 * @return
	 */
	@Override
	protected int get_null_seat_for_two_player(int seat_index) {
		if (seat_index == 0) {
			// 等于0时返回2
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (get_players()[i] == null) {
					return i;
				}
			}
		}

		if (seat_index == 1) {
			// 等于1时返回3
			for (int i = GameConstants.GAME_PLAYER - 1; i >= 0; i--) {
				if (get_players()[i] == null) {
					return i;
				}
			}
		}

		return -1;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_LX_CS_QY();
		_handler_dispath_card = new HandlerDispatchCard_LX_CS_QY();
		_handler_gang = new HandlerGang_LX_CS_QY();
		_handler_hai_di = new HandlerHaiDi_LX_CS_QY();
		_handler_out_card_operate = new HandlerOutCardOperate_LX_CS_QY();
		_handler_yao_hai_di = new HandlerYaoHaiDi_LX_CS_QY();

		_handler_xiao_hu = new HandlerXiaoHu_LX_CS_QY();
		_handler_piao = new HandlerPiao_LX_CS_QY();
		_handler_gang_dispatch_card = new HandlerGangDispatchCard_LX_CS_QY();
		_handler_gang_dispatch_card_thj = new HandlerGangDispatchCard_THJ_LX_CS_QY();

		_handler_qi_shou = new HandlerQiShou_LX_CS_QY();
	}

	public boolean exe_xiao_hu(int seat_index) {
		set_handler(_handler_xiao_hu);
		_handler_xiao_hu.reset_status(seat_index);
		_handler_xiao_hu.exe(this);

		return true;
	}

	public boolean exe_gang_cs(int seat_index) {
		set_handler(_handler_gang_dispatch_card);

		_handler_gang_dispatch_card.reset_status(seat_index);
		_handler_gang_dispatch_card.exe(this);

		return true;
	}

	public boolean exe_gang_thj_cs(int seat_index) {
		set_handler(_handler_gang_dispatch_card_thj);

		_handler_gang_dispatch_card_thj.reset_status(seat_index);
		_handler_gang_dispatch_card_thj.exe(this);

		return true;
	}

	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {
			initBanker();
			record_game_room();
		}

		_last_dispatch_player = -1;

		_card_can_not_out_after_chi = new int[getTablePlayerNumber()];
		_chi_pai_count = new int[getTablePlayerNumber()][getTablePlayerNumber()];

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

		_playerStatus = new PlayerStatus[4];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus();
		}

		_cur_round++;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

		// TODO 设置房间的xml玩法信息
		if (commonGameRuleProtos != null) {
			GRR._room_info.setNewRules(commonGameRuleProtos);
		}

		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
	protected void initBanker() {
		shuffle_players();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i].set_seat_index(i);
			if (get_players()[i].getAccount_id() == getRoom_owner_account_id()) {
				_cur_banker = i;
			}
		}
	}
	
	
	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_PLAYER_3)) {
			return 3;
		}
		return 4;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int[] operate_card, int card_count, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		int cards[] = new int[GameConstants.MAX_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_chi_hu_action_rank_cs(chr);

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		int zuoPiao = getZuoPiaoScore();
		boolean isMutlpNiap = has_rule(Constants_LX_CS_QY.GAME_RULE_ZHONG_NIAO_FAN_BEI);

		if (isMutlpNiap) {
			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int s = lChiHuScore;

					if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUANG_XIAN)) {
						if (seat_index == GRR._banker_player || i == GRR._banker_player) {
							if (zi_mo_count == 2)
								s += 2;
							else
								s += 1;
						}
					}

					pai_xing_fen[i] -= s;
					pai_xing_fen[seat_index] += s;

					bird_score[i] -= s * (GRR._player_niao_count[seat_index] + GRR._player_niao_count[i]);
					bird_score[seat_index] += s * (GRR._player_niao_count[seat_index] + GRR._player_niao_count[i]);

					s = s * (1 + GRR._player_niao_count[seat_index] + GRR._player_niao_count[i]);

					s += _player_result.pao[i] + _player_result.pao[seat_index];

					s += zuoPiao * 2; // 坐飘

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				int s = lChiHuScore;

				if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUANG_XIAN)) {
					if (seat_index == GRR._banker_player || provide_index == GRR._banker_player) {
						if (jie_pao_count[seat_index] == 2)
							s += 2;
						else
							s += 1;
					}
				}

				pai_xing_fen[provide_index] -= s;
				pai_xing_fen[seat_index] += s;

				bird_score[provide_index] -= s * (GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index]);
				bird_score[seat_index] += s * (GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index]);

				s = s * (1 + GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index]);

				s += _player_result.pao[provide_index] + _player_result.pao[seat_index];

				s += zuoPiao * 2; // 坐飘

				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int s = lChiHuScore;

					if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUANG_XIAN)) {
						if (seat_index == GRR._banker_player || i == GRR._banker_player) {
							if (zi_mo_count == 2)
								s += 2;
							else
								s += 1;
						}
					}

					pai_xing_fen[i] -= s;
					pai_xing_fen[seat_index] += s;

					bird_score[i] -= GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];
					bird_score[seat_index] += GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];

					s += GRR._player_niao_count[seat_index] + GRR._player_niao_count[i];

					s += _player_result.pao[i] + _player_result.pao[seat_index];

					s += zuoPiao * 2; // 坐飘

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				int s = lChiHuScore;

				if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZHUANG_XIAN)) {
					if (seat_index == GRR._banker_player || provide_index == GRR._banker_player) {
						if (jie_pao_count[seat_index] == 2)
							s += 2;
						else
							s += 1;
					}
				}

				pai_xing_fen[provide_index] -= s;
				pai_xing_fen[seat_index] += s;

				bird_score[provide_index] -= GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];
				bird_score[seat_index] += GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];

				s += GRR._player_niao_count[seat_index] + GRR._player_niao_count[provide_index];

				s += _player_result.pao[provide_index] + _player_result.pao[seat_index];

				s += zuoPiao * 2; // 坐飘

				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			}
		}

		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();

		return;
	}

	public void set_niao_card(int seat_index, boolean show, boolean is_hai_di) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;

		GRR._count_niao = get_niao_card_num(is_hai_di);

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (is_hai_di) {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = _send_card_data;
				}
			} else {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			}
		}
		if(AbstractMJTable.DEBUG_CARDS_MODE){
			for (int i = 0; i < GRR._count_niao; i++) {
				GRR._cards_data_niao[i] = 1;
			}
		}
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

			int seat = get_seat(nValue, seat_index);

			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
			}
		}
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;

		boolean has_da_hu = false;
		boolean dahu[] = { false, false, false, false };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (GRR._chi_hu_rights[i].is_valid() && GRR._chi_hu_rights[i].da_hu_count > 0) {
				dahu[i] = true;
				has_da_hu = true;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];
			String des = "";

			if (GRR._start_hu_right[i].is_valid()) {
				l = GRR._start_hu_right[i].type_count;

				for (int j = 0; j < l; j++) {
					type = GRR._start_hu_right[i].type_list[j];

					if (type == Constants_LX_CS_QY.CHR_DA_SI_XI) {
						des += " 大四喜";
					}
					if (type == Constants_LX_CS_QY.CHR_ZT_DA_SI_XI) {
						des += " 中途四喜";

						if (effective_zt_dsx_count[i] > 1) {
							des += "x" + effective_zt_dsx_count[i];
						}
					}
					if (type == Constants_LX_CS_QY.CHR_BAN_BAN_HU) {
						des += " 板板胡";
					}
					if (type == Constants_LX_CS_QY.CHR_QUE_YI_SE) {
						des += " 缺一色";
					}
					if (type == Constants_LX_CS_QY.CHR_LIU_LIU_SHUN) {
						des += " 六六顺";
					}
					if (type == Constants_LX_CS_QY.CHR_ZT_LLS) {
						des += " 中途六六顺";

						if (effective_zt_lls_count[i] > 1) {
							des += "x" + effective_zt_lls_count[i];
						}
					}
					if (type == Constants_LX_CS_QY.CHR_JIE_JIE_GAO) {
						des += " 节节高";
					}
					if (type == Constants_LX_CS_QY.CHR_SAN_TONG) {
						des += " 三同";
					}
					if (type == Constants_LX_CS_QY.CHR_YI_ZHI_HUA) {
						des += " 一枝花";
					}

					if (type == Constants_LX_CS_QY.CHR_JIN_TONG_YU_NV) {
						des += "  金童玉女";
					}
					if (type == Constants_LX_CS_QY.CHR_HOU_BA_LUN) {
						des += " 后八轮";
					}
					if (type == Constants_LX_CS_QY.CHR_MEI_HUA_SAN_NONG) {
						des += " 梅花三弄";
					}
					if (type == Constants_LX_CS_QY.CHR_YI_DIAN_HONG) {
						des += " 一点红";
					}
				}
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == Constants_LX_CS_QY.CHR_MEN_QING) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_MEN_QING)) {
							des += " 门清*2";
						} else {
							des += " 门清";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_PENG_PENG_HU) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_PENG_PENG_HU)) {
							des += " 碰碰胡*2";
						} else {
							des += " 碰碰胡";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_JIANG_JIANG_HU) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_JIANG_JIANG_HU)) {
							des += " 将将胡*2";
						} else {
							des += " 将将胡";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_QING_YI_SE) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_QING_YI_SE)) {
							des += " 清一色*2";
						} else {
							des += " 清一色";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_HAI_DI_LAO_YUE) {
						des += " 海底捞";
					}
					if (type == Constants_LX_CS_QY.CHR_HAI_DI_PAO) {
						des += " 海底炮";
					}
					if (type == Constants_LX_CS_QY.CHR_QI_XIAO_DUI) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_QI_XIAO_DUI)) {
							des += " 七小对*2";
						} else {
							des += " 七小对";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_HH_QI_XIAO_DUI) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_HH_QI_XIAO_DUI)) {
							des += " 豪华七小对*2";
						} else {
							des += " 豪华七小对";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_GANG_KAI) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_GANG_KAI)) {
							des += " 杠上开花*2";
						} else {
							des += " 杠上开花";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_QIANG_GANG) {
						des += " 抢杠胡";
					}
					if (type == Constants_LX_CS_QY.CHR_GANG_PAO) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_GANG_PAO)) {
							des += " 杠上炮*2";
						} else {
							des += " 杠上炮";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_QUAN_QIU_REN) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_QUAN_QIU_REN)) {
							des += " 全求人*2";
						} else {
							des += " 全求人";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_SHH_QI_XIAO_DUI) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_SHH_QI_XIAO_DUI)) {
							des += " 双豪华七小对*2";
						} else {
							des += " 双豪华七小对";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_TIAN_HU) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_TIAN_HU)) {
							des += " 天胡*2";
						} else {
							des += " 天胡";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_DI_HU) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_DI_HU)) {
							des += " 地胡*2";
						} else {
							des += " 地胡";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_JIA_JIANG_HU) {
						if (chr.is_mul_long(Constants_LX_CS_QY.CHR_JIA_JIANG_HU)) {
							des += " 假将胡*2";
						} else {
							des += " 假将胡";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_ZI_MO) {
						if (dahu[i] == true) {
							des += " 大胡自摸";
						} else {
							des += " 小胡自摸";
						}
					}
					if (type == Constants_LX_CS_QY.CHR_JIE_PAO) {
						if (dahu[i] == true) {
							des += " 大胡接炮";
						} else {
							des += " 小胡接炮";
						}
					}
				} else {
					if (type == Constants_LX_CS_QY.CHR_FANG_PAO) {
						if (has_da_hu == true) {
							des += " 大胡放炮";
						} else {
							des += " 小胡放炮";
						}
					}
				}
			}

			// int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			// TODO 去掉杠牌显示
			// if (GRR != null) {
			// for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber();
			// tmpPlayer++) {
			// for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
			// if (GRR._weave_items[tmpPlayer][w].weave_kind !=
			// GameConstants.WIK_GANG
			// && GRR._weave_items[tmpPlayer][w].weave_kind !=
			// GameConstants.WIK_BU_ZHNAG) {
			// continue;
			// }
			// if (tmpPlayer == i) {
			// if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
			// jie_gang++;
			// } else {
			// if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
			// ming_gang++;
			// } else {
			// an_gang++;
			// }
			// }
			// } else {
			// if (GRR._weave_items[tmpPlayer][w].provide_player == i) {
			// fang_gang++;
			// }
			// }
			// }
			// }
			//
			// if (an_gang > 0) {
			// des += " 暗杠X" + an_gang;
			// }
			// if (ming_gang > 0) {
			// des += " 明杠X" + ming_gang;
			// }
			// if (fang_gang > 0) {
			// des += " 放杠X" + fang_gang;
			// }
			// if (jie_gang > 0) {
			// des += " 接杠X" + jie_gang;
			// }
			// }

			if (GRR._player_niao_count[i] > 0) {
				des += " 中鸟X" + GRR._player_niao_count[i];
			}

			if (getZuoPiaoScore() > 0) {
				des += " 坐飘" + getZuoPiaoScore() + "分";
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 获取坐飘分
	 */
	public int getZuoPiaoScore() {
		int score = 0;
		if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZUO_PIAO_1)) {
			return 1;
		} else if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZUO_PIAO_2)) {
			return 2;
		} else if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZUO_PIAO_3)) {
			return 3;
		} else if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZUO_PIAO_4)) {
			return 4;
		} else if (has_rule(Constants_LX_CS_QY.GAME_RULE_ZUO_PIAO_5)) {
			return 5;
		}
		return score;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x02, 0x02, 0x11, 0x11, 0x12, 0x12, 0x13, 0x13, 0x22, 0x22, 0x15, 0x15, 0x15, 0x16 };
		int[] cards_of_player1 = new int[] { 0x22, 0x22, 0x02, 0x27, 0x27, 0x27, 0x21, 0x13, 0x14, 0x14, 0x15, 0x15, 0x05, 0x16 };
		int[] cards_of_player2 = new int[] { 0x01, 0x05, 0x09, 0x27, 0x27, 0x27, 0x11, 0x12, 0x13, 0x14, 0x14, 0x15, 0x15, 0x15 };
		int[] cards_of_player3 = new int[] { 0x01, 0x05, 0x09, 0x27, 0x27, 0x27, 0x11, 0x12, 0x13, 0x14, 0x14, 0x15, 0x15, 0x15 };

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

		if (cards_of_player0.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards_of_player0[13])]++;
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
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		_repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 庄家起手14张牌，闲家起手13张牌。和之前的有点区别。GameStart之后，不会再发牌。直接走新的Handler。
			if (i == GRR._banker_player) {
				send_count = GameConstants.MAX_COUNT;
			} else {
				send_count = (GameConstants.MAX_COUNT - 1);
			}

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		if (cards.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards[13])]++;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}
}
