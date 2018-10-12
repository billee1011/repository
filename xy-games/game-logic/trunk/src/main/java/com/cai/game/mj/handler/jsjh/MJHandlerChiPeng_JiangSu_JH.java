package com.cai.game.mj.handler.jsjh;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_JiangSu_JH extends MJHandlerChiPeng<MJTable_JiangSu_JH> {
	private GangCardResult m_gangCardResult;

	public MJHandlerChiPeng_JiangSu_JH() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(MJTable_JiangSu_JH table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			// table._playerStatus[i].clean_status();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		if (table._logic.is_da_gen_card(_card)) {
			_action = GameConstants.WIK_GANG;
		}
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		table._peng_palyer_count[_seat_index][_provider]++;
		// 设置用户
		table._current_player = _seat_index;

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		if (table._logic.is_magic_card(_card)) {
			table._out_card_index[_seat_index][table._logic.switch_to_card_index(_card)] += 2;
		}
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		// 癞子
		int outcard_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table._out_card_index[_seat_index][i] > 0) {
				outcard_count += table._out_card_index[_seat_index][i];
			}
		}
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j]) && outcard_count == 0) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			} else if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DA;

			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];// table.GRR._weave_items[_seat_index];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
			// 癞子
			if (weaves[i].weave_kind == GameConstants.WIK_PENG) {
				if (table._logic.is_magic_card(weaves[i].center_card) && table._da_er_da[_seat_index] / 4 < 3) {
					weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
				} else if (table._logic.is_magic_card(weaves[i].center_card)) {
					weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
				}
			} else if (weaves[i].weave_kind == GameConstants.WIK_GANG || weaves[i].weave_kind == GameConstants.WIK_AN_GANG) {
				if (table._logic.is_magic_card(weaves[i].center_card) && table._da_er_da[_seat_index] / 4 < 4) {
					weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_TOU_DA;
				} else if (table._logic.is_magic_card(weaves[i].center_card)) {
					weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_ER_DA;
				}
			}
		}
		// 刷新手牌
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		if (table._logic.is_magic_card(_card)) {
			int dacard_count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (table._logic.is_magic_index(i)) {
					dacard_count += table.GRR._cards_index[_seat_index][i];
				}
			}
			dacard_count -= table._xian_chu_count[_seat_index];
			long effect_indexs[] = new long[dacard_count];
			table.operate_effect_action(_seat_index, GameConstants.Effect_Action_Other, dacard_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		}

		// 删掉出来的那张牌
		// table.operate_out_card(this._provider, 0,
		// null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		// 回放
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		int llcard = 0;
		if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混玩法
																	// 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		// 跟张
		table._gen_player = GameConstants.INVALID_SEAT;
		table._gen_out_card = GameConstants.INVALID_CARD;

		if (table._logic.is_da_gen_card(_card)) {
			PengtoGang(table);
		}

		m_gangCardResult.cbCardCount = 0;
		// 如果牌堆还有牌，判断能不能杠
		if (table.GRR._left_card_count > llcard) {
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true);

			if (cbActionMask != 0) {
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (table.has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && table._logic.is_magic_card(m_gangCardResult.cbCardData[i])) {// 鬼牌不能杆
						continue;
					}
					// 加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					curPlayerStatus.add_action(GameConstants.WIK_GANG);// 转转就是杠
				}
			}
		}

		if (curPlayerStatus.has_action()) {
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

	public void PengtoGang(MJTable_JiangSu_JH table) {
		table._player_result.ming_gang_count[_seat_index]++;
		if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_PEI_CHONG)) {
			if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index) {
						continue;
					}
					int score = (int) (table._player_result.game_score[i] - table._di_fen);
					if (score < -table._yuanzi_fen) {
						table._player_result.game_score[i] -= table._yuanzi_fen + table._player_result.game_score[i];
						table._player_result.game_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[i];
						table._end_score[i] -= table._yuanzi_fen + table._player_result.game_score[i];
						table._end_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[i];
					} else {
						table._player_result.game_score[i] -= table._di_fen;
						table._player_result.game_score[_seat_index] += table._di_fen;
						table._end_score[i] -= table._di_fen;
						table._end_score[_seat_index] += table._di_fen;
					}
				}

			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index) {
						continue;
					}
					table._player_result.game_score[i] -= table._di_fen;
					table._player_result.game_score[_seat_index] += table._di_fen;
					table._end_score[i] -= table._di_fen;
					table._end_score[_seat_index] += table._di_fen;
				}
			}
		} else {
			if (table.has_rule(GameConstants.GAME_RULE_JIANGSU_YUAN_ZI)) {
				int score = (int) (table._player_result.game_score[_provider] - table._di_fen * 3);
				if (score < -table._yuanzi_fen) {
					table._player_result.game_score[_provider] -= table._yuanzi_fen + table._player_result.game_score[_provider];
					table._player_result.game_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[_provider];
					table._end_score[_provider] -= table._yuanzi_fen + table._player_result.game_score[_provider];
					table._end_score[_seat_index] += table._yuanzi_fen + table._player_result.game_score[_provider];
				} else {
					table._player_result.game_score[_provider] -= table._di_fen * 3;
					table._player_result.game_score[_seat_index] += table._di_fen * 3;
					table._end_score[_provider] -= table._di_fen * 3;
					table._end_score[_seat_index] += table._di_fen * 3;
				}
			} else {
				int gang_cell = table._di_fen;
				if (table._b_double && table.has_rule(GameConstants.GAME_RULE_JIANGSU_DOUBLE)) {
					gang_cell *= 2;
				}
				table._player_result.game_score[_provider] -= gang_cell * 3;
				table._player_result.game_score[_seat_index] += gang_cell * 3;
				table._end_score[_provider] -= gang_cell * 3;
				table._end_score[_seat_index] += gang_cell * 3;
			}
		}
		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(table._game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		table.load_player_info_data(roomResponse2);
		table.send_response_to_room(roomResponse2);
	}

	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable_JiangSu_JH table, int seat_index, int card) {
		card = table.get_real_card(card);
		// 错误断言
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		//
		// if
		// (table._logic.is_magic_card(card)&&table.is_mj_type(MJGameConstants.GAME_TYPE_HENAN))
		// {
		// table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
		// table.log_error("癞子牌不能出癞子");
		// return false;
		// }

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌--执行出牌切换状态
		table.exe_out_card(_seat_index, card, _action);

		return true;
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
	public boolean handler_operate_card(MJTable_JiangSu_JH table, int seat_index, int operate_code, int operate_card) {
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

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			// table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			// table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}

		}
			break;
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_JiangSu_JH table, int seat_index) {
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
					if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card) && table._da_er_da[i] / 4 < 3) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
					} else if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
				} else if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG
						|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_GANG) {
					if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card) && table._da_er_da[i] / 4 < 4) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_TOU_DA);
					} else if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_ER_DA);
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
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
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		int outcard_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (table._out_card_index[seat_index][i] > 0 && !table._logic.is_magic_index(i)) {
				outcard_count += table._out_card_index[seat_index][i];
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

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
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
				table.operate_effect_action(index, GameConstants.Effect_Action_Other, dacard_count, effect_indexs, 1, seat_index);
			}
		}

		return true;
	}
}
