package com.cai.game.mj.handler.jszz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_JangSu_ZZ extends MJHandlerDispatchCard<MJTable_JangSu_ZZ> {

	@Override
	public void exe(MJTable_JangSu_ZZ table) {
		// 用户状态--清除状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			// table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		Arrays.fill(table._can_not_open_index[_seat_index], 0);

		int llcard = 0;
		// if(table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {//带混玩法
		// 剩余14张 结算
		// llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		// }

		// 荒庄结束
		if (table.GRR._left_card_count <= llcard) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			if (!table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
				table._current_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
						% table.getTablePlayerNumber();// 若荒装，则当局庄家的下家继续坐庄。
			}

			// 流局
			table.handler_game_finish(table._current_banker, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			table._send_card_count++;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

			--table.GRR._left_card_count;
		} else {
			table._send_card_count++;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

			--table.GRR._left_card_count;
		}

		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x11;
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
		} else if (_type == GameConstants.HU_CARD_TYPE_AN_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_AN_GANG_KAI;
		} else if (_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_BU_GANG_KAI;
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type);// 自摸

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		if (action != GameConstants.WIK_NULL) {
			int outcard_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table._out_card_index[_seat_index][i] > 0) {
					outcard_count += table._out_card_index[_seat_index][i];
				}
			}
			// 添加动作
			if (!(outcard_count == 0 && table._logic.magic_count(table.GRR._cards_index[_seat_index]) != 0)) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			}
		} else {
			chr.set_empty();
		}

		// 癞子
		int real_card = _send_card_data;

		int outcard_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table._out_card_index[_seat_index][i] > 0) {
				outcard_count += table._out_card_index[_seat_index][i];
			}
		}
		if (outcard_count == 0 && table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			table._xian_chu_count[_seat_index]++;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DA;

			int no_da_outcard_count = 0;
			int da_outcard_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table._out_card_index[_seat_index][i] > 0 && !table._logic.is_magic_index(i)) {
					no_da_outcard_count += table._out_card_index[_seat_index][i];
				}
				if (table._out_card_index[_seat_index][i] > 0 && table._logic.is_magic_index(i)) {
					da_outcard_count += table._out_card_index[_seat_index][i];
				}
			}
			if (no_da_outcard_count > 0 && da_outcard_count == 0) {
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { GameConstants.EFFECT_GUANERDA }, 1, GameConstants.INVALID_SEAT);
			}

			int dacard_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table._logic.is_magic_index(i)) {
					dacard_count += table.GRR._cards_index[_seat_index][i];
				}
			}
			dacard_count -= table._xian_chu_count[_seat_index];
			long effect_indexs[] = new long[dacard_count];
			if (da_outcard_count == 0) {
				table.operate_effect_action(_seat_index, GameConstants.Effect_Action_Other, dacard_count, effect_indexs,
						1, GameConstants.INVALID_SEAT);
			} else {
				table.operate_effect_action(_seat_index, GameConstants.Effect_Action_Other, 0, effect_indexs, 1,
						_seat_index);
			}
		}

		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		// if(curPlayerStatus.has_zi_mo() &&
		// table.has_rule(MJGameConstants.GAME_TYPE_ZZ_JIANPAOHU)){
		// //见炮胡
		// table.exe_jian_pao_hu(_seat_index,MJGameConstants.WIK_ZI_MO,_send_card_data);
		// return ;
		// }

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌
		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count > llcard) {

			int cbActionMask = table._logic.analyse_gang_card_jszz(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true);

			if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					curPlayerStatus.add_action(GameConstants.WIK_GANG);// 转转就是杠
				}
			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			// curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			// curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);//
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
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
	public boolean handler_operate_card(MJTable_JangSu_ZZ table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

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
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		// playerStatus.clean_status();
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌无效
			if (table._playerStatus[seat_index].has_zi_mo()) {
				table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
			}
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			// table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				// table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			table._user_pao_da[_seat_index] = false;

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
							false);
					return true;
				}
			}
		}
			break;
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			if (table._user_pao_da[_seat_index]) {
				ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
				chr.opr_or(GameConstants.CHR_JIANGSU_ZZ_PAO_DA);
			}
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);

			if (!table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
				Arrays.fill(table.bao_pei_palyer, GameConstants.INVALID_SEAT);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._peng_palyer_count[_seat_index][i] + table._gang_palyer_count[_seat_index][i] >= 3) {
						table._is_bao_pei = true;
						table.bao_pei_palyer[_seat_index] = i;
						break;
					}
				}
			}
			if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
				if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
					// 接杠杠开算放炮
					table._is_gangkai = true;
					table.process_chi_hu_player_score_jszz(_seat_index,
							table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index] - 1].provide_player,
							operate_card, true, false, false, true);
				} else if (_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI) {
					table._is_gangkai = true;
					table.process_chi_hu_player_score_jszz(_seat_index, table._bu_gang_provide_player, operate_card,
							true, false, false, true);

				} else {
					table.process_chi_hu_player_score_jszz(_seat_index, _seat_index, operate_card, true, false, false,
							false);
				}

			} else {
				if (table._is_bao_pei) {
					table.process_chi_hu_player_score_jszz(_seat_index, table.bao_pei_palyer[_seat_index], operate_card,
							true, false, true, false);
				} else {
					if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
						// 接杠杠开算放炮
						table._is_gangkai = true;
						table.process_chi_hu_player_score_jszz(_seat_index,
								table.GRR._weave_items[_seat_index][table.GRR._weave_count[_seat_index]
										- 1].provide_player,
								operate_card, true, false, false, true);
					} else if (_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI) {
						table._is_gangkai = true;
						table.process_chi_hu_player_score_jszz(_seat_index, table._bu_gang_provide_player, operate_card,
								true, false, false, true);
					} else {
						table.process_chi_hu_player_score_jszz(_seat_index, _seat_index, operate_card, true, false,
								false, false);
					}
				}
			}

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_JangSu_ZZ table, int seat_index) {
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

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_PENG) {
					if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)
							&& table._da_er_da[i] / 4 < 3) {
						weaveItem_item.setCenterCard(
								table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
					} else if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
						weaveItem_item.setCenterCard(
								table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG
						|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_GANG) {
					if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)
							&& table._da_er_da[i] / 4 < 4) {
						weaveItem_item.setCenterCard(
								table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
					} else if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
						weaveItem_item.setCenterCard(
								table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				}
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
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
		int outcard_count = 0;
		int outcard_da_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table._out_card_index[seat_index][i] > 0) {
				outcard_count += table._out_card_index[seat_index][i];
			}
			if (table._logic.is_magic_index(i)) {
				outcard_da_count += table._out_card_index[seat_index][i];
			}
		}

		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j]) && outcard_count == 0) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			} else if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DA;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		// 癞子
		int real_card = _send_card_data;
		if (table._logic.is_magic_card(_send_card_data) && outcard_count == 0) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
		} else if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DA;
		}
		// 摸牌
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		// 刷新手上2搭牌
		for (int index = 0; index < table.getTablePlayerNumber(); index++) {
			int dacard_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table._logic.is_magic_index(i)) {
					dacard_count += table.GRR._cards_index[index][i];
				}
			}
			dacard_count -= table._xian_chu_count[index];
			if (dacard_count > 0) {
				long effect_indexs[] = new long[dacard_count];
				if (outcard_da_count == 0) {
					table.operate_effect_action(index, GameConstants.Effect_Action_Other, dacard_count, effect_indexs,
							1, seat_index);
				} else {
					table.operate_effect_action(index, GameConstants.Effect_Action_Other, 0, effect_indexs, 1,
							seat_index);
				}
			}
		}

		return true;
	}

	public void other_deal(MJTable_JangSu_ZZ table) {
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

		card_type = GameConstants.HU_CARD_TYPE_AN_GANG_KAI;

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		table._current_player = _seat_index;// 轮到操作的人是自己
		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type);// 自摸

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		if (action != GameConstants.WIK_NULL) {
			int outcard_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table._out_card_index[_seat_index][i] > 0) {
					outcard_count += table._out_card_index[_seat_index][i];
				}
			}
			// 添加动作
			if (!(outcard_count == 0 && table._logic.magic_count(table.GRR._cards_index[_seat_index]) != 0)) {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			}
		} else {
			chr.set_empty();
		}

		if (curPlayerStatus.has_action()) {// 有动作
			// curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			// curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);//
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}
}
