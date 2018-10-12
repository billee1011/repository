package com.cai.game.hh.handler.ldfpf;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.HHGameLogic;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.game.hh.handler.ldfpf.Constants_LouDiFangPaoFa.ChrType;
import com.google.common.base.Strings;

public abstract class LouDiFangPaoFaUtils {

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
			table.GRR._game_score[table._cur_banker] -= 10;//流局庄家-10分
			table._player_result.game_score[table._cur_banker] -= 10;

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
			// if (isHei) { 这里把一五十也当成红牌了
			// count += logic.calculate_weave_hei_pai(weaveItems[i]);
			// } else {
			// count += logic.calculate_weave_hong_pai(weaveItems[i]);
			// }
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
			if (!logic.color_hei(analyseItem.cbCenterCard[i])) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 计算红牌门子(一旦其他门子有红牌则返回0)
	 * 
	 * @param logic
	 * @param analyseItem
	 * @return
	 */
	public static int count_hong_pai_menzi(HHGameLogic logic, AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_DUI_ZI:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_EQS:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
				if (!logic.color_hei(analyseItem.cbCenterCard[i])) {
					count++;
				}
				break;
			default:
				for (int j = 0; j < analyseItem.cbCardData[i].length; j++) {
					if (!logic.color_hei(analyseItem.cbCardData[i][j])) {
						return 0;
					}
				}
				break;
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

	public static void judge_hu_pai(HHTable table, int _seat_index, int _send_card_data) {
		int send_index = table._logic.switch_to_card_index(_send_card_data);
		boolean is_fa_pai = false;
		int seat[] = new int[table.getTablePlayerNumber()]; // 按照庄家-下家-上家的顺序依次天胡
		seat[0] = _seat_index;
		seat[1] = (_seat_index + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber(); // 下家
		seat[2] = (_seat_index + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber(); // 上家
		for (int k = 0; k < seat.length; k++) {
			int i = seat[k];
			int ti_count = 0;
			int sao_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if ((i == table._current_player) && (j == send_index)) {
					table.GRR._cards_index[i][j]++;
				}
				if (table.GRR._cards_index[i][j] == 4) {
					ti_count++;
					if ((i == table._current_player) && (j == send_index)) {
						is_fa_pai = true;
					}
				}
				if (table.GRR._cards_index[i][j] == 3) {
					sao_count++;
					if ((i == table._current_player) && (j == send_index)) {
						is_fa_pai = true;
					}
				}
				if ((i == table._current_player) && (j == send_index)) {
					table.GRR._cards_index[i][j]--;
				}
			}
			if ((ti_count >= 4) || (sao_count >= 5) || (ti_count + sao_count) > 5) {
				ChiHuRight chr = table.GRR._chi_hu_rights[i];
				int card_type = ChrType.CHR_ZI_MO.getIndex();
				chr.set_empty();
				int all_hu_xi = 0;
				for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
					if ((i == table._current_player) && (j == send_index)) {
						table.GRR._cards_index[i][j]++;
					}
					if (table.GRR._cards_index[i][j] == 4) {
						if (j < 10) {
							all_hu_xi += 12;
						} else {
							all_hu_xi += 9;
						}
						ti_count++;
					}
					if (table.GRR._cards_index[i][j] == 3) {
						if (j < 10) {
							all_hu_xi += 6;
						} else {
							all_hu_xi += 3;
						}
						sao_count++;
					}
					if ((i == table._current_player) && (j == send_index)) {
						table.GRR._cards_index[i][j]--;
					}
				}
				if (all_hu_xi >= Constants_LouDiFangPaoFa.GAME_QI_HU_XI) {
					int weave_count = 0;
					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
						if ((i == table._current_player) && (j == send_index)) {
							table.GRR._cards_index[i][j]++;
						}
						if (table.GRR._cards_index[i][j] == 4) {
							table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_AN_LONG;
							table._hu_weave_items[i][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
							weave_count++;
						}
						if (table.GRR._cards_index[i][j] == 3) {
							table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_KAN;
							table._hu_weave_items[i][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
							weave_count++;
						}
						if ((i == table._current_player) && (j == send_index)) {
							table.GRR._cards_index[i][j]--;
						}
					}
					int hu_card = table._hu_weave_items[i][weave_count - 1].center_card;
					table._hu_weave_count[i] = weave_count;
					if (card_type == ChrType.CHR_ZI_MO.getIndex()) {
						chr.opr_or(ChrType.CHR_TIAN_HU.getIndex());
						if (i == _seat_index) {
							chr.opr_or(ChrType.CHR_ZI_MO.getIndex());
						}
					}
					PlayerStatus curPlayerStatus = table._playerStatus[i];
					curPlayerStatus.reset();
					if ((i == table._current_player) && (is_fa_pai == true)) { // 发送数据
																				// 只有自己才有数值
						table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);
					}
					// 添加动作
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(hu_card, i);
					if (curPlayerStatus.has_action()) {
						table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);
						break;
					}
				} else {
					chr.set_empty();
				}
			}

		}
	}

	public static void hu_pai(HHTable table, int _seat_index, int _provide_player) {
		table.GRR._chi_hu_rights[_seat_index].set_valid(true);
		table.GRR._chi_hu_card[_seat_index][0] = table._send_card_data;
		table._cur_banker = _seat_index;
		table._shang_zhuang_player = _seat_index;
		table.process_chi_hu_player_operate(_seat_index, table._send_card_data, true);
		table.process_chi_hu_player_score_phz(_seat_index, _provide_player, table._send_card_data, true);

//		table.operate_player_get_card(table._provide_player, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

		// 记录
		if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
			table._player_result.da_hu_zi_mo[_seat_index]++;
		} else {
			table._player_result.xiao_hu_zi_mo[_seat_index]++;
		}
		table.countChiHuTimes(_seat_index, true);

		/*int delay = GameConstants.GAME_FINISH_DELAY_FLS;
		if (table.GRR._chi_hu_rights[_seat_index].type_count > 2) {
			delay += table.GRR._chi_hu_rights[_seat_index].type_count - 2;
		}*/
		GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), 0, TimeUnit.MILLISECONDS);
	}
	
	public static int get_ai_out_card(HHTable table, int seat_index) {
		
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		Set<Integer> can_not_shoot = new HashSet<Integer>();
        // 先注释掉，等客户端一起联调
        for (int x = 0; x < hand_card_count; x++) {
            if (((LouDiFangPaoFaHHTable)table).is_card_has_wei(cards[x])) { // 如果是偎的牌
                // 判断打出这张牌是否能听牌
                table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(cards[x])]--;
                boolean b_is_ting_state = ((LouDiFangPaoFaHHTable)table).is_ting_state(table.GRR._cards_index[seat_index],
                        table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
                table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(cards[x])]++;

                if (!b_is_ting_state)
                	can_not_shoot.add(cards[x]);
            }
        }
		
		int[] cards_index = table.GRR._cards_index[seat_index];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX/2; i++) {
			if(can_not_shoot.contains(table._logic.switch_to_card_data(i)))
				continue;
			if (cards_index[i] == 1)
			{
				if(i == 1)
				{
					if(cards_index[6] == 1)
						continue;
					if(cards_index[9] == 1)
						continue;
				}
				if(i == 6)
				{
					if(cards_index[1] == 1)
						continue;
					if(cards_index[9] == 1)
						continue;
				}
				if(i == 9)
				{
					if(cards_index[6] == 1)
						continue;
					if(cards_index[1] == 1)
						continue;
				}
				if(i-1>=0&&cards_index[i-1]==1)
					continue;
				if(i-2>=0&&cards_index[i-2]==1)
					continue;
				if(i+1<GameConstants.MAX_HH_INDEX/2&&cards_index[i+1]==1)
					continue;
				if(i+2<GameConstants.MAX_HH_INDEX/2&&cards_index[i+2]==1)
					continue;
				if(cards_index[i]==1&&cards_index[i+10]>=1)
					continue;
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 10 ; i< GameConstants.MAX_HH_INDEX;i++)
		{
			if(can_not_shoot.contains(table._logic.switch_to_card_data(i)))
				continue;
			if(i == 11)
			{
				if(cards_index[16] == 1)
					continue;
				if(cards_index[19] == 1)
					continue;
			}
			if(i == 16)
			{
				if(cards_index[11] == 1)
					continue;
				if(cards_index[19] == 1)
					continue;
			}
			if(i == 19)
			{
				if(cards_index[16] == 1)
					continue;
				if(cards_index[11] == 1)
					continue;
			}
			if (cards_index[i] == 1)
			{
				if(i-1>=10&&cards_index[i-1]==1)
					continue;
				if(i-2>=10&&cards_index[i-2]==1)
					continue;
				if(i+1<GameConstants.MAX_HH_INDEX&&cards_index[i+1]==1)
					continue;
				if(i+2<GameConstants.MAX_HH_INDEX&&cards_index[i+2]==1)
					continue;
				if(cards_index[i]==1&&cards_index[i-10]>=1)
					continue;
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 10 ; i< GameConstants.MAX_HH_INDEX;i++)
		{
			if (cards_index[i] == 1)
			{
				if(can_not_shoot.contains(table._logic.switch_to_card_data(i)))
					continue;
				if((i+1<GameConstants.MAX_HH_INDEX&&cards_index[i+1]==1)&&i+2<GameConstants.MAX_HH_INDEX&&cards_index[i+2]==1)
				{
					i += 1;
					continue;
				}
				if(cards_index[i] + cards_index[i-10] == 3)
				{
					continue;
				}
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX/2;i++){
			if (cards_index[i] == 1)
			{
				if(can_not_shoot.contains(table._logic.switch_to_card_data(i)))
					continue;
				
				if((i+1<GameConstants.MAX_HH_INDEX/2&&cards_index[i+1]==1)&&(i+2<GameConstants.MAX_HH_INDEX/2&&cards_index[i+2]==1))
				{
					i+=1;
					continue;
				}
				if(cards_index[i] + cards_index[i+10] == 3)
				{
					continue;
				}
				return table._logic.switch_to_card_data(i);
			}
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX;i++)
		{
			if (cards_index[i] == 1){
				if(can_not_shoot.contains(table._logic.switch_to_card_data(i)))
					continue;
				return table._logic.switch_to_card_data(i);
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 2){
				if(can_not_shoot.contains(table._logic.switch_to_card_data(i)))
					continue;
				return table._logic.switch_to_card_data(i);
			}
		}
		if(can_not_shoot.size() > 0){
			for (int card : can_not_shoot) {
				return card;
			}
		}
		return 1;
	}

}
