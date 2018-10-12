package com.cai.game.mj.sichuan.luzhougui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.MjAnalyseUtil;
import com.cai.game.mj.sichuan.Solution;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;

import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;

@ThreeDimension
public class Table_LuZhouGui extends AbstractSiChuanMjTable {
	private static final long serialVersionUID = 1L;

	private HandlerBaoTing_LuZhouGui _handler_out_card_bao_ting;

	public Table_LuZhouGui(MJType type) {
		super(type);
	}

	@Override
	protected void init_shuffle() {
		if (hasRuleFourMagic) {
			_repertory_card = new int[MJConstants.CARD_DATA_FOUR_HZ.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_FOUR_HZ);
		} else if (hasRuleEightMagic) {
			_repertory_card = new int[MJConstants.CARD_DATA_EIGHT_HZ.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_EIGHT_HZ);
		} else if (hasRuleTwelveMagic) {
			_repertory_card = new int[MJConstants.CARD_DATA_TWELVE_HZ.length];
			shuffle(_repertory_card, MJConstants.CARD_DATA_TWELVE_HZ);
		}
	};

	@Override
	public void process_show_hand_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data_sichuan(GRR._cards_index[i], cards, ding_que_pai_se[i]);

			if (win_order[i] != 0) {
				if (GRR._chi_hu_card[i][0] > GameConstants.CARD_ESPECIAL_TYPE_LU_ZHOU_GUI) {
					cards[hand_card_count++] = GRR._chi_hu_card[i][0];
				} else if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
					cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[hand_card_count++] = GRR._chi_hu_card[i][0];
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}

	@Override
	protected void onInitTable() {
		super.onInitTable();

		_handler_chi_peng = new HandlerChiPeng_LuZhouGui();
		_handler_dispath_card = new HandlerDispatchCard_LuZhouGui();
		_handler_gang = new HandlerGang_LuZhouGui();
		_handler_out_card_operate = new HandlerOutCardOperate_LuZhouGui();
		_handler_out_card_bao_ting = new HandlerBaoTing_LuZhouGui();
		_handler_qi_shou = new HandlerQiShou_LuZhouGui();

		Arrays.fill(display_ruan_peng, true);
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		set_handler(_handler_out_card_bao_ting);
		_handler_out_card_bao_ting.reset_status(seat_index, card, type);
		_handler.exe(this);

		return true;
	}

	@Override
	public void process_gang_score() {
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
	public boolean is_ting_card(int card, int seat_index) {
		int count = _playerStatus[seat_index]._hu_card_count;

		if (count == 0)
			return true;

		for (int i = 0; i < count; i++) {
			int tmp_card = get_real_card(_playerStatus[seat_index]._hu_cards[i]);
			if (card == tmp_card) {
				return true;
			}
		}

		if (count == 1 && _playerStatus[seat_index]._hu_cards[0] == -1) {
			// 全听
			return true;
		}

		return false;
	}

	@Override
	public int get_max_pai_xing_fen(int seat_index) {
		int max_score = -1;
		int max_geng_count = 0;

		int cbCurrentCard = -1;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (i >= GameConstants.MAX_ZI && i != magicCardIndex)
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);

			// 如果当前牌，不是听牌数据里的数据。这样能节省很多时间。
			if (!is_ting_card(cbCurrentCard, seat_index)) {
				continue;
			}

			ChiHuRight chr = new ChiHuRight();
			chr.set_empty();

			int card_type = Constants_SiChuan.HU_CARD_TYPE_JIE_PAO;

			boolean flag = false;
			if (GRR._cards_index[seat_index][i] == 5) {
				flag = true;
				GRR._cards_index[seat_index][i] = 2;
			}

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index], cbCurrentCard, chr, card_type, seat_index, seat_index)) {
				if (flag) {
					GRR._cards_index[seat_index][i] = 5;
				}

				int magic_count = get_magic_count(GRR._cards_index[seat_index], GRR._weave_count[seat_index], GRR._weave_items[seat_index],
						cbCurrentCard);

				int pai_xing_fen = get_pai_xing_fen(chr, finalGengCount[seat_index], magic_count);

				int max_pai_xing_fen = get_max_pai_xing_fen();

				if (pai_xing_fen > max_pai_xing_fen) {
					pai_xing_fen = max_pai_xing_fen;
				}

				if (pai_xing_fen > max_score) {
					max_pai_xing_desc[seat_index] = get_pai_xing_desc_str(chr, seat_index);

					max_score = pai_xing_fen;
					max_geng_count = finalGengCount[seat_index];

					finallyFanShu[seat_index] = pai_xing_fen;
				}
			}
		}

		finalGengCount[seat_index] = max_geng_count;

		return max_score;
	}

	public String get_pai_xing_desc_str(ChiHuRight chr, int seat_index) {
		StringBuilder result = new StringBuilder();

		int chrTypes = chr.type_count;

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
			if (type == Constants_SiChuan.CHR_NO_MAGIC) {
				result.append(" 无鬼");
			}
			if (type == Constants_SiChuan.CHR_BAO_JIAO) {
				result.append(" 报叫");
			}
		}

		if (finalGengCount[seat_index] > 0)
			result.append(" 归x" + finalGengCount[seat_index]);

		return result.toString();
	}

	@Override
	public long analyse_qi_xiao_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return 0;

		int cbReplaceCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (i == magicCardIndex)
				continue;

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;
		}

		int count = 0;
		for (int m = 0; m < 1; m++) {
			count += cbCardIndexTemp[magicCardIndex];
		}

		if (cbReplaceCount > count) {
			return 0;
		}

		return Constants_SiChuan.CHR_QI_DUI;
	}

	public int get_magic_count(int[] cbCardIndex, int weave_count, WeaveItem[] weave_items, int cur_card) {
		int count = 0;

		count += cbCardIndex[magicCardIndex];

		if (cur_card == magicCard)
			count++;

		for (int i = 0; i < weave_count; i++) {
			int kind = weave_items[i].weave_kind;

			if (kind == GameConstants.WIK_SUO_GANG_1 || kind == GameConstants.WIK_SUO_PENG_1)
				count += 1;

			if (kind == GameConstants.WIK_SUO_GANG_2 || kind == GameConstants.WIK_SUO_PENG_2)
				count += 2;

			if (kind == GameConstants.WIK_SUO_GANG_3)
				count += 3;
		}

		return count;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, int _provide_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card == 0x35 || (cur_card >= 0x01 && cur_card <= 0x29)) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int pai_se_count = _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);
		if (pai_se_count > 2)
			return 0;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = 1;
		magic_cards_index[0] = magicCardIndex;

		int magic_count = get_magic_count(cbCardIndexTemp, weave_count, weaveItems, -1);

		// 平胡
		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		long qi_dui = analyse_qi_xiao_dui(cbCardIndexTemp, weave_count);
		// 七对
		boolean can_win_qi_dui = (qi_dui != 0);

		if (!can_win && !can_win_qi_dui) {
			return 0;
		}

		// 清一色
		boolean can_win_qing_yi_se = _logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weave_count);

		// 对对胡
		boolean can_win_dd_hu = !can_win_qi_dui
				&& AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);

		// 天胡
		boolean can_win_tian_hu = false;
		if (_out_card_count == 0 && _seat_index == GRR._banker_player) {
			can_win_tian_hu = true;
		}

		if (can_win_qi_dui) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QI_DUI);
		}
		if (can_win_dd_hu) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_DUI_DUI_HU);
		}
		if (can_win_qing_yi_se) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_QING_YI_SE);
		}
		if (can_win_tian_hu && analyse_state != FROM_TING) {
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_TIAN_HU);
		}

		if (magic_count == 0) {
			// 无鬼
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_NO_MAGIC);
		}

		if (is_bao_ting[_seat_index]) {
			// 报叫
			chiHuRight.opr_or_long(Constants_SiChuan.CHR_BAO_JIAO);
		}

		if (card_type == Constants_SiChuan.HU_CARD_TYPE_GANG_PAO || card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO) {
			if (is_bao_ting[_provide_index]) {
				// 杀报
				chiHuRight.opr_or_long(Constants_SiChuan.CHR_SHA_BAO);
			}
		}

		int geng_count = get_geng_count(GRR._cards_index[_seat_index], GRR._weave_items[_seat_index], GRR._weave_count[_seat_index], cur_card,
				chiHuRight);

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
		}

		int fan_shu = get_fan_shu(chiHuRight, geng_count, magic_count);

		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_80KE) != 0 && fan_shu < 5 && card_type == Constants_SiChuan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.set_empty();
			return 0;
		}

		if (fan_shu == 0) {
			chiHuRight.set_empty();
			return 0;
		}

		finalGengCount[_seat_index] = geng_count;

		score_when_win[_seat_index] = get_pai_xing_fen(chiHuRight, geng_count, magic_count);

		return GameConstants.WIK_CHI_HU;
	}

	public int get_pai_xing_fen(ChiHuRight chr, int geng_count, int magic_count) {
		int fan = get_fan_shu(chr, geng_count, magic_count);
		int feng = fan;

		boolean has_ming_tang = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty())
			has_ming_tang = true;

		boolean has_gang_pai = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_QIANG_GANG).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_GANG_PAO).is_empty())
			has_gang_pai = true;

		boolean has_bao = !chr.opr_and_long(Constants_SiChuan.CHR_BAO_JIAO).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_SHA_BAO).is_empty();

		boolean has_both_sha_jiao = !chr.opr_and_long(Constants_SiChuan.CHR_BAO_JIAO).is_empty()
				&& !chr.opr_and_long(Constants_SiChuan.CHR_SHA_BAO).is_empty();

		if (!chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty() && has_ming_tang) {
			// 天胡牌型如果有名堂，直接按照封顶算
		} else if (magic_count == 0 && has_gang_pai) {
			// 无鬼和抢杠、杠上花、杠上炮同时存在时，直接按照封顶算
		} else if (has_ming_tang) {
			if (magic_count == 0)
				feng *= 4;

			if (has_bao)
				feng *= 4;

			if (has_both_sha_jiao)
				feng *= 4;

			if (has_gang_pai)
				feng *= 4;

			if (geng_count > 0) {
				if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty())
					feng *= 1 << (geng_count - 1);
				else
					feng *= 1 << geng_count;
			}
		} else if (!has_ming_tang) {
			if (magic_count == 0) {
				if (geng_count > 0) {
					if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty())
						feng *= 1 << (geng_count - 1);
					else
						feng *= 1 << geng_count;
				}
			} else {
				if (has_gang_pai || !chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty() || has_bao) {
					if (geng_count > 0) {
						if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty())
							feng *= 1 << (geng_count - 1);
						else
							feng *= 1 << geng_count;
					}
				} else {
					if (geng_count >= 2) {
						feng *= 1 << (geng_count - 2);
					}
				}
			}

			if (has_both_sha_jiao)
				feng *= 4;
		}

		int max_fen = get_max_pai_xing_fen();
		if (feng > max_fen)
			feng = max_fen;

		return feng;
	}

	public int get_fan_shu(ChiHuRight chr, int geng_count, int magic_count) {
		int fan = 0;

		boolean has_ming_tang = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty())
			has_ming_tang = true;

		boolean has_gang_pai = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_GANG_KAI).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_QIANG_GANG).is_empty()
				|| !chr.opr_and_long(Constants_SiChuan.CHR_GANG_PAO).is_empty())
			has_gang_pai = true;

		boolean has_zi_mo = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_ZI_MO).is_empty())
			has_zi_mo = true;

		boolean has_bao = false;
		if (!chr.opr_and_long(Constants_SiChuan.CHR_BAO_JIAO).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_SHA_BAO).is_empty())
			has_bao = true;

		if (!chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty() && has_ming_tang) {
			// 天胡牌型如果有名堂，直接按照封顶算
			fan = get_max_pai_xing_fan();
		} else if (magic_count == 0 && has_gang_pai) {
			// 无鬼和抢杠、杠上花、杠上炮同时存在时，直接按照封顶算
			fan = get_max_pai_xing_fan();
		} else if (has_ming_tang) {
			if (has_zi_mo)
				fan += 1;

			if (!chr.opr_and_long(Constants_SiChuan.CHR_QING_YI_SE).is_empty()
					&& (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty() || !chr.opr_and_long(Constants_SiChuan.CHR_DUI_DUI_HU).is_empty()))
				fan += 4;
			else
				fan += 2;
		} else if (!has_ming_tang) {
			if (magic_count == 0) {
				fan = 3;

				if (has_zi_mo)
					fan += 1;
				if (has_bao)
					fan += 1;
			} else {
				if (has_gang_pai) {
					fan = 3;
				} else if (!chr.opr_and_long(Constants_SiChuan.CHR_TIAN_HU).is_empty()) {
					fan = 4;
				} else if (has_bao) {
					fan = 2;

					if (has_zi_mo)
						fan += 1;
				} else {
					if (geng_count >= 2)
						fan = 2;
					else if (geng_count == 1)
						fan = 1;

					if (has_zi_mo)
						fan += 1;
				}
			}
		}

		if (fan == 0) {
			if (analyse_state == FROM_MAX_COUNT)
				return 1;
			else
				return 0;
		}
		if (fan == 1)
			return 2;
		if (fan == 2)
			return 5;
		if (fan == 3)
			return 10;
		if (fan == 4)
			return 20;
		if (fan == 5)
			return 40;
		if (fan >= 6)
			return 80;

		return 0;
	}

	public int get_geng_count(int[] cards_index, WeaveItem[] weave_items, int weave_count, int hu_card, ChiHuRight chr) {
		if (analyse_state != FROM_NORMAL && analyse_state != FROM_MAX_COUNT)
			return 0;

		int geng = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(hu_card))
			cbCardIndexTemp[_logic.switch_to_card_index(hu_card)]++;

		int handMagicCount = cbCardIndexTemp[magicCardIndex];

		if (!chr.opr_and_long(Constants_SiChuan.CHR_QI_DUI).is_empty()) {
			for (int i = 0; i < GameConstants.MAX_INDEX && handMagicCount > 0; i++) {
				if (cbCardIndexTemp[i] == 1 || cbCardIndexTemp[i] == 3) {
					handMagicCount -= 1;
					cbCardIndexTemp[magicCardIndex]--;
					cbCardIndexTemp[i]++;
				}
			}

			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cbCardIndexTemp[i] >= 4) {
					geng++;
				} else if (cbCardIndexTemp[i] == 2 && handMagicCount >= 2) {
					handMagicCount -= 2;
					geng++;
				}
			}

			return geng;
		}

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
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;
		
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int tmp_geng_count = finalGengCount[seat_index];

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (i >= GameConstants.MAX_ZI && i != magicCardIndex)
				continue;

			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index, seat_index)) {
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

		int max_ting_count = GameConstants.MAX_ZI_FENG;
		int tmp_geng_count = finalGengCount[seat_index];

		for (int i = 0; i < max_ting_count; i++) {
			if (i >= GameConstants.MAX_ZI && i != magicCardIndex)
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_SiChuan.HU_CARD_TYPE_ZI_MO, seat_index, seat_index)) {
				int magic_count = get_magic_count(cbCardIndexTemp, cbWeaveCount, weaveItem, cbCurrentCard);
				// 能听的牌数据 有几番
				ting_pai_fan_shu[seat_index][ting_count][count] = get_pai_xing_fen(chr, finalGengCount[seat_index], magic_count);

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

	public int get_max_pai_xing_fen() {
		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_20KE) != 0) {
			return 20;
		}
		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_40KE) != 0) {
			return 40;
		}
		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_80KE) != 0) {
			return 80;
		}
		return 40;
	}

	public int get_max_pai_xing_fan() {
		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_20KE) != 0) {
			return 4;
		}
		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_40KE) != 0) {
			return 5;
		}
		if (getRuleValue(Constants_SiChuan.GAME_RULE_FENG_DING_80KE) != 0) {
			return 6;
		}
		return 5;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = GameConstants.CELL_SCORE;

		int magic_count = get_magic_count(GRR._cards_index[seat_index], GRR._weave_count[seat_index], GRR._weave_items[seat_index],
				GRR._chi_hu_card[seat_index][0]);

		int pai_xing_fen = get_pai_xing_fen(chr, finalGengCount[seat_index], magic_count);

		finallyFanShu[seat_index] = pai_xing_fen;

		int max_pai_xing_fen = get_max_pai_xing_fen();

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (pai_xing_fen > max_pai_xing_fen) {
			pai_xing_fen = max_pai_xing_fen;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				if (GRR._win_order[i] != 0) {
					continue;
				}

				int tmpScore = pai_xing_fen;
				if (is_bao_ting[i]) {
					chr.opr_or_long(Constants_SiChuan.CHR_SHA_BAO);
					tmpScore *= 4;
				}

				if (tmpScore > max_pai_xing_fen)
					tmpScore = max_pai_xing_fen;

				GRR._game_score[seat_index] += tmpScore;
				GRR._game_score[i] -= tmpScore;
			}
		} else {
			GRR._game_score[seat_index] += pai_xing_fen;
			GRR._game_score[provide_index] -= pai_xing_fen;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		int pai_se = _logic.get_card_color(card) + 1;

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

			if (win_order[i] != 0) {
				continue;
			}

			if (pai_se == ding_que_pai_se[i]) {
				continue;
			}

			playerStatus = _playerStatus[i];

			boolean can_peng_this_card = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng_this_card = false;
					break;
				}
			}

			int[] colorFlag = new int[4];
			int colorCount = 0;
			for (int j = 0; j < GRR._weave_count[i]; j++) {
				int wCard = GRR._weave_items[i][j].center_card;
				int wColor = _logic.get_card_color(wCard);
				if (colorFlag[wColor] == 0) {
					colorFlag[wColor] = 1;
					colorCount++;
				}
			}
			int tmpColor = _logic.get_card_color(card);
			boolean can_gang_this_card = true;
			boolean can_win_this_card = true;
			if (colorCount == 2 && colorFlag[tmpColor] == 0) {
				can_peng_this_card = false;
				can_gang_this_card = false;
				can_win_this_card = false;
			}

			if (can_peng_this_card && !is_bao_ting[i]) {
				action = _logic.check_peng_with_suo_pai_luzhougui(GRR._cards_index[i], card, display_ruan_peng[i]);
				if (action != 0) {
					if ((action & GameConstants.WIK_PENG) != 0) {
						playerStatus.add_normal_wik(card, GameConstants.WIK_PENG, seat_index);
						playerStatus.add_action(GameConstants.WIK_PENG);
						bAroseAction = true;
					} else if ((action & GameConstants.WIK_SUO_PENG_1) != 0) {
						playerStatus.add_normal_wik(card, GameConstants.WIK_SUO_PENG_1, seat_index);
						playerStatus.add_action(GameConstants.WIK_PENG);
						bAroseAction = true;
					}
				}
			}

			if (GRR._left_card_count > 0 && can_gang_this_card) {
				if (is_bao_ting[i]) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state = is_ting_state(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					}
				} else {
					action = _logic.estimate_gang_card_with_suo_pai_luzhougui(GRR._cards_index[i], card, display_ruan_peng[i]);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						bAroseAction = true;

						if ((action & GameConstants.WIK_GANG) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_GANG, seat_index, 1);
						}
						if ((action & GameConstants.WIK_SUO_GANG_1) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_SUO_GANG_1, seat_index, 1);
						}
						if ((action & GameConstants.WIK_SUO_GANG_2) != 0) {
							playerStatus.add_normal_gang_wik(card, GameConstants.WIK_SUO_GANG_2, seat_index, 1);
						}
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && can_win_this_card) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int hu_card_type = Constants_SiChuan.HU_CARD_TYPE_JIE_PAO;
				if (type == GameConstants.WIK_GANG)
					hu_card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_PAO;

				analyse_state = FROM_NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i, seat_index);
				if (action != 0) {
					if (is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
						playerStatus.add_action(GameConstants.WIK_CHI_HU);
						playerStatus.add_chi_hu(card, seat_index);
						bAroseAction = true;
					} else {
						// 接炮时，总得分变大了，才能接炮胡
						if (score_when_win[i] > score_when_abandoned_win[i]) {
							playerStatus.add_action(GameConstants.WIK_CHI_HU);
							playerStatus.add_chi_hu(card, seat_index);
							bAroseAction = true;
						}
					}
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

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round() && win_order[i] == 0) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = FROM_NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_SiChuan.HU_CARD_TYPE_QIANG_GANG, i, seat_index);

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

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (room_rq.hasQiang()) {
			display_ruan_peng[seat_index] = room_rq.getQiang() > 0 ? true : false;
			operate_player_data();
		}

		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
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

			// 玩家定缺的牌色，1，万，2，条，3，筒，0，还没选定缺
			room_player.setPao(null != ding_que_pai_se ? ding_que_pai_se[i] : 0);
			// 玩家胡牌的顺序，1，2，3
			room_player.setNao(null != win_order ? win_order[i] : 0);
			// 玩家胡牌的类型，1，自摸，2，胡
			room_player.setQiang(null != win_type ? win_type[i] : 0);
			// 玩家是否已经换三张，1，是，2，否，3，没换三张玩法
			if (!has_rule(Constants_SiChuan.GAME_RULE_HUAN_SAN_ZHANG)) {
				room_player.setBiaoyan(3);
			} else {
				room_player.setBiaoyan((null != had_switch_card) ? (had_switch_card[i] ? 1 : 2) : 2);
			}

			// 是否已经报叫
			if (is_bao_ting != null) {
				room_player.setIsTrustee(is_bao_ting[i]);
			} else {
				room_player.setIsTrustee(false);
			}

			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);

			if (display_ruan_peng != null) {
				// 贴鬼碰/杠 0不勾中 1勾中
				room_player.setZiBa(display_ruan_peng[i] ? 1 : 0);
			} else {
				room_player.setZiBa(0);
			}

			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
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

					if (type == Constants_SiChuan.CHR_DUI_DUI_HU) {
						result.append(" 对对胡");
					}
					if (type == Constants_SiChuan.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_SiChuan.CHR_QI_DUI) {
						result.append(" 七对");
					}
					if (type == Constants_SiChuan.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == Constants_SiChuan.CHR_NO_MAGIC) {
						result.append(" 无鬼");
					}
					if (type == Constants_SiChuan.CHR_BAO_JIAO) {
						result.append(" 报叫");
					}
					if (type == Constants_SiChuan.CHR_SHA_BAO) {
						result.append(" 杀报");
					}
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (finalGengCount[player] > 0) {
					result.append(" 归x" + finalGengCount[player]);
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		return 0;
	}
}
