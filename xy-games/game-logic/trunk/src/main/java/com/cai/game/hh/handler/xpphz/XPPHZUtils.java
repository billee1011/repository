package com.cai.game.hh.handler.xpphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.HHGameLogic;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.google.common.base.Strings;

public abstract class XPPHZUtils {

	/**
	 * 听牌基础判断
	 * 
	 * @param table
	 * @param _seat_index
	 */
	public static void ting_basic(HHTable table, int _seat_index) {
		table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[_seat_index]._hu_cards,
				table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, _seat_index);

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
	 * 
	 * @param table
	 *            房间信息
	 * @param _seat_index
	 *            当前玩家
	 * @param delay
	 *            延迟
	 * @param sendCardData
	 *            发牌数据
	 * @param info
	 *            日志信息
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
	 * 
	 * @param table
	 */
	public static void cleanPlayerStatus(HHTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
	}

	/**
	 * 荒庄结束
	 * 
	 * @param table
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
					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i], table.GRR._weave_count[i],
							GameConstants.INVALID_SEAT);
				}
			}
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			// 流局 选择当前庄家继续当庄
			table.handler_game_finish(table.GRR._banker_player, GameConstants.Game_End_DRAW);

			return true;
		}
		return false;
	}

	/**
	 * 计算胡牌后的红牌或者黑牌数量
	 * 
	 * @param logic
	 * @param weaveItems
	 * @param weaveCount
	 * @param isHei
	 *            是否黑牌
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
			case GameConstants.WIK_EQS: // 二七十
			case GameConstants.WIK_DDX: // 大大吃
			case GameConstants.WIK_XXD: // 小小吃
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
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					if (logic.color_hei(weaveItems[i].center_card + j) == isHei) {
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
	 * 
	 * @param logic
	 * @param weaveItems
	 * @param weaveCount
	 * @return
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
	 * 
	 * @param logic
	 * @param analyseItem
	 * @return
	 */
	public static int count_hong_pai_duizi(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if (analyseItem.cbWeaveKind[i] == GameConstants.WIK_DUI_ZI && !logic.color_hei(analyseItem.cbCenterCard[i])) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 统计红牌坎数
	 * 
	 * @param logic
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public static int count_hong_pai_kan(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_KAN && !logic.color_hei(weaveItems[i].center_card)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 统计红牌坎数
	 * 
	 * @param logic
	 * @param analyseItem
	 * @return
	 */
	public static int count_hong_pai_kan(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			if (analyseItem.cbWeaveKind[i] == GameConstants.WIK_KAN && !logic.color_hei(analyseItem.cbCenterCard[i])) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 统计红牌提数
	 * 
	 * @param logic
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public static int count_hong_pai_ti(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
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
	 * 
	 * @param logic
	 * @param analyseItem
	 * @return
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
	 * 
	 * @param logic
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public static int count_chi_pai(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_EQS: // 二七十
			case GameConstants.WIK_DDX: // 大大吃
			case GameConstants.WIK_XXD: // 小小吃
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
	 * 
	 * @param logic
	 * @param analyseItem
	 * @return
	 */
	public static int count_chi_pai(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_EQS: // 二七十
			case GameConstants.WIK_DDX: // 大大吃
			case GameConstants.WIK_XXD: // 小小吃
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
	 * 
	 * @param logic
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public static int calculate_big_pai_count(HHGameLogic logic, WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			count += logic.get_da_card(weaveItems[i].weave_kind, weaveItems[i].center_card);
		}
		return count;
	}

	public static int calculate_big_pai_count(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			count += logic.get_da_card(analyseItem.cbWeaveKind[i], analyseItem.cbCenterCard[i]);
		}
		return count;
	}

	public static void hu_pai(HHTable table, int _seat_index, int provide) {
		table.GRR._chi_hu_rights[_seat_index].set_valid(true);
		table.GRR._chi_hu_card[_seat_index][0] = table._send_card_data;

		table.process_chi_hu_player_operate(_seat_index, table._send_card_data, true);
		table.process_chi_hu_player_score_phz(_seat_index, _seat_index, table._send_card_data, true);

		// table.operate_player_get_card(_seat_index, 1, new int[] {
		// table._send_card_data }, GameConstants.INVALID_SEAT, false);

		// 记录
		if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
			table._player_result.da_hu_zi_mo[_seat_index]++;
		} else {
			table._player_result.xiao_hu_zi_mo[_seat_index]++;
		}
		table.countChiHuTimes(_seat_index, true);

		int delay = GameConstants.GAME_FINISH_DELAY_FLS;
		if (table.GRR._chi_hu_rights[_seat_index].type_count > 2) {
			delay += table.GRR._chi_hu_rights[_seat_index].type_count - 2;
		}

		table._cur_banker = _seat_index;
		table._shang_zhuang_player = _seat_index;
		table._provide_player = provide;
		
		GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
	}

}
