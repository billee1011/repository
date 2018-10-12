package com.cai.game.mj.shanxi.hongdong;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_SXHD;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.yu.mygy.GameConstants_MYGY;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_ShanXi_HongDong extends MJHandlerDispatchCard<MJTable_HongDong> {

	boolean can_bao_ting = false;
	protected GangCardResult m_gangCardResult;
	private boolean ting_send_card = false;
	protected int _type;

	public MJHandlerDispatchCard_ShanXi_HongDong() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@Override
	public void exe(MJTable_HongDong table) {
		// 用户状态 清除
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		// 免碰流局：摸完牌才流局，无论有多少个杠；
		if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_MIAN_PENG)) {
			// 荒庄结束
			if (table.GRR._left_card_count == 0) {

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				// 流局
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

				return;
			}
			// 如果非有免碰规则 全局杠数≤1时，剩牌还剩14张即流局，杠数＞1时，每多一个杠，剩牌多留1张，2杠15,3杠16
		} else {
			// 荒庄结束
			int card_num = 14;
			if (table.gang_count > 1) {
				card_num += table.gang_count - 1;
			}
			if (_type == Constants_MJ_SXHD.HU_GANG_SHANG_KAI_HUA) {
				card_num = 0;
			}
			if (table.GRR._left_card_count <= card_num) {

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}
				if (table.gang_count > 0) {
					table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				} else {
					table._cur_banker = table._cur_banker; // 上一家的庄家继续坐庄
				}

				// 流局
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

				return;
			}

		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index; // 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x14;
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int action = GameConstants.WIK_NULL;
		int show_card_type = 2;
		// 胡牌检测
		int card_type = Constants_MJ_SXHD.GAME_RULE_ZIMOHU;
		if (_type == Constants_MJ_SXHD.HU_GANG_SHANG_KAI_HUA) {
			card_type = Constants_MJ_SXHD.HU_GANG_SHANG_KAI_HUA;
			show_card_type = 5;
		}
		// 胡牌检测
		if (table._playerStatus[_seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
				|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index, false); // 自摸
		}

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		if (table._playerStatus[_seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
				|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
		} else {
			int count = 0;
			int ting_count = 0;
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			ting_send_card = false;

			int card_type_count = GameConstants.MAX_ZI_FENG;
			int default_ting = 0;
			if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)) {
				default_ting = 27;
				card_type_count = GameConstants.MAX_FENG;
			}
			if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
				default_ting = 31;
				card_type_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = default_ting; i < card_type_count; i++) {
				// if (table._logic.is_magic_index(i))
				// continue;
				if (table.player_magic_card[_seat_index] != 0 && table._logic.switch_to_card_index(table.player_magic_card[_seat_index]) == i
						&& table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table.player_magic_card[_seat_index])] <= 2) {
					continue;
				}

				count = table.GRR._cards_index[_seat_index][i];

				if (count > 0) {
					table.GRR._cards_index[_seat_index][i]--;

					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
							table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

						ting_count++;

						if (send_card_index == i) {
							ting_send_card = true;
						}
					}

					table.GRR._cards_index[_seat_index][i]++;
				}
			}

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;
			// ting_count = table.filter_ting(ting_count,
			// table._playerStatus[_seat_index]._hu_out_card_ting);
			if (ting_count > 0) {
				can_bao_ting = true;
				table.GRR._cards_index[_seat_index][send_card_index]--;

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							// 如果既是听牌 又是癞子的牌
							// if (table._logic.is_magic_card(cards[i])) {
							// cards[i] +=
							// (GameConstants.CARD_ESPECIAL_TYPE_TING +
							// GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
							// } else {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							// }

							break;
						}
					}
					if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
						if (table._logic.is_magic_card(cards[i]))
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
				if (0 != table.accpet_ying_kou(_seat_index, table._playerStatus[_seat_index]._hu_out_card_count,
						table._playerStatus[_seat_index]._hu_out_card_ting, table._playerStatus[_seat_index]._hu_out_card_ting_count,
						table._playerStatus[_seat_index]._hu_out_cards, new int[Constants_MJ_SXHD.HAND_CARD_MAX_COUNT])
						&& (ting_count != 1 || (ting_count == 1 && table._playerStatus[_seat_index]._hu_out_card_ting_count[0] != 1))) {
					table._playerStatus[_seat_index].add_action(Constants_MJ_SXHD.WIK_YING_KOU);
				}
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		}

		// 发送数据
		// 只有自己才有数值
		int show_send_card = _send_card_data;
		// if (table._logic.is_magic_card(_send_card_data) && ting_send_card) {
		// show_send_card = show_send_card +
		// GameConstants.CARD_ESPECIAL_TYPE_TING +
		// GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		// } else
		if (table._logic.is_magic_card(_send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (ting_send_card) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[_seat_index] == _send_card_data) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT, show_card_type);

		// 设置变量
		table._provide_card = _send_card_data; // 提供的牌

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table.analyse_gang_exclude_magic_card(_seat_index, table.GRR._cards_index[_seat_index], _send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true);

			if (table._playerStatus[_seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
					|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
				if (0 != cbActionMask) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
						int tmp_card_count = table.GRR._cards_index[_seat_index][tmp_card_index];
						int tmp_weave_count = table.GRR._weave_count[_seat_index];

						// 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
						// 发牌时，杠牌只要碰杠和暗杠这两种

						// 如果是风杠的牌型
						table.GRR._cards_index[_seat_index][tmp_card_index] = 0;

						if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[i]) {
							table.GRR._weave_items[_seat_index][tmp_weave_count].public_card = 0;
							table.GRR._weave_items[_seat_index][tmp_weave_count].center_card = m_gangCardResult.cbCardData[i];
							table.GRR._weave_items[_seat_index][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
							table.GRR._weave_items[_seat_index][tmp_weave_count].provide_player = _seat_index;
							++table.GRR._weave_count[_seat_index];
						}

						boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

						// 还原手牌数据和落地牌数据
						table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;

						table.GRR._weave_count[_seat_index] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							curPlayerStatus.add_action(GameConstants.WIK_GANG);
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						}
					}
				}
			} else {
				if (0 != cbActionMask) {
					for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
						curPlayerStatus.add_action(GameConstants.WIK_GANG);
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}
				}
			}
		}

		if (table.GRR._left_card_count >= 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = 0;
			cbActionMask = table.analyse_gang_exclude_magic_card(_seat_index, table.GRR._cards_index[_seat_index], _send_card_data,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true);

			if (cbActionMask != GameConstants.WIK_NULL) { // 有杠
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上杠
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && _type == GameConstants.DispatchCard_Type_Tian_Hu) {
			table.xex_xuan_wang(_seat_index, _send_card_data, m_gangCardResult);
			return;
		}
		if (curPlayerStatus.has_action()) { // 有动作
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
					|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_MYGY.DELAY_JIAN_PAO_HU_NEW,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_HongDong table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		operate_card = table.get_real_card(operate_card);

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			// 能胡不胡，要过圈
			if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
				table._playerStatus[seat_index].chi_hu_round_invalid();
			}

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			if (table._playerStatus[_seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
					|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_JIAN_PAO_HU_NEW,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			table.has_gang_count++;
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == table.get_real_card(m_gangCardResult.cbCardData[i])) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);
			table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			if (_seat_index == table._cur_banker) {
				table._cur_banker = _seat_index;
			} else {
				table._cur_banker = (table._cur_banker + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			}

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_hd(_seat_index, _seat_index, operate_card, true);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index) {
					continue;
				}
			}
			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY,
					TimeUnit.SECONDS);
			return true;
		}
		case Constants_MJ_SXHD.WIK_YING_KOU: //
		{
			if (operate_card == 0) {
				PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				curPlayerStatus.add_action(Constants_MJ_SXHD.WIK_YING_KOU);
				int[] has_kou_out_card = new int[Constants_MJ_SXHD.HAND_CARD_MAX_COUNT];
				int kou_out_card_count = table.accpet_ying_kou(_seat_index, table._playerStatus[seat_index]._hu_out_card_count,
						table._playerStatus[seat_index]._hu_out_card_ting, table._playerStatus[seat_index]._hu_out_card_ting_count,
						table._playerStatus[seat_index]._hu_out_cards, has_kou_out_card);

				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

				table.operate_player_cards_with_ting_ed(_seat_index, hand_card_count, cards, 0, null);

				if (0 != kou_out_card_count) {
					ting_send_card = false;
					for (int kk = 0; kk < kou_out_card_count; kk++) {
						if (has_kou_out_card[kk] == _send_card_data) {
							ting_send_card = true;
							break;
						}
					}
				}
				int show_send_card = _send_card_data;
				// if (table._logic.is_magic_card(_send_card_data) &&
				// ting_send_card) {
				// show_send_card = show_send_card +
				// GameConstants.CARD_ESPECIAL_TYPE_TING +
				// GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				// } else
				if (table._logic.is_magic_card(_send_card_data)) {
					show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (ting_send_card) {
					show_send_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
				} else if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[_seat_index] == _send_card_data) {
					show_send_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				table.operate_player_get_card(_seat_index, 1, new int[] { show_send_card }, GameConstants.INVALID_SEAT);
			} else {
				operate_card -= operate_card > GameConstants.CARD_ESPECIAL_TYPE_TING ? GameConstants.CARD_ESPECIAL_TYPE_TING : 0;
				if (table._logic.is_valid_card(operate_card) == false) {
					table.log_error("出牌,牌型出错");
					return false;
				}
				// 效验参数
				if (seat_index != _seat_index) {
					table.log_error("出牌,没到出牌");
					return false;
				}

				// 删除扑克
				if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
					table.log_error("出牌删除出错");
					return false;
				}

				// 报听
				table.exe_out_card_bao_ting(_seat_index, operate_card, Constants_MJ_SXHD.CARD_STATUS_YING_KOU);
				return true;
			}
		}
		case GameConstants.WIK_BAO_TING: //
		{
			operate_card -= operate_card > GameConstants.CARD_ESPECIAL_TYPE_TING ? GameConstants.CARD_ESPECIAL_TYPE_TING : 0;
			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}
			// 效验参数
			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.CARD_STATUS_BAO_TING);
			return true;

		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_HongDong table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 色子
		if (table._cur_round == 1) {
			roomResponse.setEffectCount(4);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[2]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[3]);
		} else {
			roomResponse.setEffectCount(2);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
		}

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int iCardIndex = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {

				}
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					if (iCardIndex > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
						iCardIndex -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
				}
				int_array.addItem(iCardIndex);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌

			if (i == _seat_index) {
				// 牌
				if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && !table.player_magic_card_show_non[i]) {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 3);
				} else {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
				}
			} else {
				// 牌
				if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && !table.player_magic_card_show_non[i]) {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 2);
				} else {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
				}
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(cards, _send_card_data);
		}
		// 癞子
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		int h = hand_card_count;
		if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
			if (table.player_magic_card_show_non[seat_index]) {
				liang_magic_count = 2;
			}
			for (int j = 0; j < h; j++) {
				if (cards[j] != table.player_magic_card[seat_index]) {
					continue;
				}

				if (liang_magic_count == 2) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					liang_magic_count++;
					hand_card_count--;
				}
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber() && table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB); p++) {
			if (!table.player_magic_card_show_non[seat_index]) {
				tableResponse.addHuXi(table.player_magic_card[p]);
			}
		}
		for (int i = 0; i < h; i++) {
			if (cards[i] > GameConstants.CARD_ESPECIAL_TYPE_GUI && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		boolean bSendCardBaoTing = false;
		if (_seat_index == seat_index) {
			// 报听
			int send_card_index = table._logic.switch_to_card_index(_send_card_data);
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int baotingcards[] = new int[GameConstants.MAX_COUNT];
			int baotingcount = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], baotingcards);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			if (table._playerStatus[_seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
				// 如果可以报听,刷新自己的手牌
				int ting_count = table._playerStatus[_seat_index]._hu_out_card_count;
				if (ting_count > 0) {
					for (int i = 0; i < baotingcount; i++) {
						for (int j = 0; j < ting_count; j++) {
							if (table._logic.is_magic_card(baotingcards[i]))
								baotingcards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							if (baotingcards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j])
								baotingcards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							if (_send_card_data == table._playerStatus[_seat_index]._hu_out_card_ting[j])
								bSendCardBaoTing = true;
						}
					}
					table.operate_player_cards_with_ting(_seat_index, baotingcount, baotingcards, 0, null);
				}
			}
		}

		// 癞子
		int real_card = _send_card_data;
		if (bSendCardBaoTing) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[_seat_index] == _send_card_data) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		// 摸牌
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0 && table._playerStatus[seat_index]._card_status == GameConstants.CARD_STATUS_BAO_TING
				|| table._playerStatus[_seat_index]._card_status == Constants_MJ_SXHD.CARD_STATUS_YING_KOU == true) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
