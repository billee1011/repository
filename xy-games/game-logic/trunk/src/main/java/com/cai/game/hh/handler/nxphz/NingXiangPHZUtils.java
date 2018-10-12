package com.cai.game.hh.handler.nxphz;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.game.hh.HHGameLogic;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.google.common.base.Strings;

public abstract class NingXiangPHZUtils {

	/**
	 * 听牌基础判断
	 */
	public static void ting_basic(HHTable table, int _seat_index) {
		table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[_seat_index]._hu_cards,
				table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
				_seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}
	}

	/**
	 * 设置下一个发牌用户
	 */
	public static void setNextPlay(HHTable table, int _seat_index, int delay, int sendCardData, String info) {
		ting_basic(table, _seat_index);

		int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

		table._current_player = next_player;
		table._last_player = next_player;
		table._last_card = sendCardData;

		table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, delay);

		if (!Strings.isNullOrEmpty(info)) {
			table.log_player_error(_seat_index, info);
		}
	}

	/**
	 * 清除用户状态
	 */
	public static void cleanPlayerStatus(HHTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
	}

	/**
	 * 荒庄结束
	 */
	public static boolean endHuangZhuang(HHTable table, boolean showHuPai) {
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			if (showHuPai) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.MAX_HH_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);
					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
				}
			}

			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;

			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return true;
		}
		return false;
	}

	/**
	 * 计算胡牌后的红牌或者黑牌数量
	 * 
	 * @return
	 */
	public static int calculate_hongOrHei_pai_count(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount, boolean isHei) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (logic.color_hei(weaveItems[i].center_card) == isHei) {
					count += 4;
				}
				break;
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
				if (logic.color_hei(weaveItems[i].center_card) == isHei) {
					count += 3;
				}
				break;
			case GameConstants.WIK_DUI_ZI:
				if (logic.color_hei(weaveItems[i].center_card) == isHei) {
					count += 2;
				}
				break;
			case GameConstants.WIK_LEFT:
				for (int j = 0; j < 3; j++) {
					if (logic.color_hei(weaveItems[i].center_card + j) == isHei) {
						count += 1;
					}
				}
				break;
			case GameConstants.WIK_CENTER:
				if (logic.color_hei(weaveItems[i].center_card - 1) == isHei) {
					count += 1;
				}
				if (logic.color_hei(weaveItems[i].center_card) == isHei) {
					count += 1;
				}
				if (logic.color_hei(weaveItems[i].center_card + 1) == isHei) {
					count += 1;
				}
				break;
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					if (logic.color_hei(weaveItems[i].center_card - j) == isHei) {
						count += 1;
					}
				}
				break;
			}
		}
		return count;
	}

	/**
	 * 统计红牌对子数
	 */
	public static int count_hong_pai_duizi(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_DUI_ZI && !logic.color_hei(weaveItems[i].center_card)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 统计红牌对子数
	 */
	public static int count_hong_pai_duizi(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if (analyseItem.cbWeaveKind[i] == GameConstants.WIK_DUI_ZI && !logic.color_hei(analyseItem.cbCenterCard[i])) {
				count++;
			}
		}
		if ((analyseItem.curCardEye != false) && (!logic.color_hei(analyseItem.cbCardEye))) {
			count++;
		}
		return count;
	}

	/**
	 * 统计红牌坎数
	 */
	public static int count_hong_pai_kan(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			if ((weaveItems[i].weave_kind == GameConstants.WIK_KAN)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 统计红牌坎数
	 */
	public static int count_hong_pai_kan(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if ((analyseItem.cbWeaveKind[i] == GameConstants.WIK_KAN || analyseItem.cbWeaveKind[i] == GameConstants.WIK_PENG
					|| analyseItem.cbWeaveKind[i] == GameConstants.WIK_WEI || analyseItem.cbWeaveKind[i] == GameConstants.WIK_CHOU_WEI)
					&& !logic.color_hei(analyseItem.cbCenterCard[i])) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 统计红牌提数
	 */
	public static int count_hong_pai_ti(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
				if (!logic.color_hei(weaveItems[i].center_card)) {
					count++;
				}
				break;
			}
		}
		return count;
	}

	/**
	 * 统计红牌提数
	 */
	public static int count_hong_pai_ti(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (!logic.color_hei(analyseItem.cbCenterCard[i])) {
					count++;
				}
				break;
			}
		}
		return count;
	}

	/**
	 * 计算吃牌数
	 */
	public static int count_chi_pai(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				count++;
				break;
			}
		}
		return count;
	}

	/**
	 * 计算吃牌数
	 */
	public static int count_chi_pai(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				count++;
				break;
			}
		}
		return count;
	}

	/**
	 * 计算大字牌数量
	 */
	public static int calculate_big_pai_count(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			count += logic.get_da_card(weaveItems[i].weave_kind, weaveItems[i].center_card);
		}
		return count;
	}

	public static int calculate_xiao_pai_count(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			count += logic.get_xiao_card(weaveItems[i].weave_kind, weaveItems[i].center_card);
		}
		return count;
	}

	public static int calculate_big_pai_count(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			count += logic.get_da_card(analyseItem.cbWeaveKind[i], analyseItem.cbCenterCard[i]);
		}
		if (analyseItem.curCardEye && analyseItem.cbCardEye > 16) {
			count += 2;
		}
		return count;
	}

	public static int calculate_xiao_pai_count(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			count += logic.get_xiao_card(analyseItem.cbWeaveKind[i], analyseItem.cbCenterCard[i]);
		}
		if (analyseItem.curCardEye && analyseItem.cbCardEye < 16) {
			count += 2;
		}
		return count;
	}
}
