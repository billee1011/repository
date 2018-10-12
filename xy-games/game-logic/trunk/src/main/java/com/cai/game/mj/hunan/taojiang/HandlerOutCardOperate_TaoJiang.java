package com.cai.game.mj.hunan.taojiang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerOutCardOperate_TaoJiang extends MJHandlerOutCardOperate<Table_TaoJiang> {

	@Override
	public void exe(Table_TaoJiang table) {
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		if (_type != GameConstants.WIK_BAO_TING) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}

			// 记录一下当前牌局的状态信息
			table._out_card_count++;
			table._out_card_player = _out_card_player;
			table._out_card_data = _out_card_data;
			table._provide_player = _out_card_player;
			table._provide_card = _out_card_data;

			table._current_player = _out_card_player;

			table.seat_index_when_win = _out_card_player;

			// table._current_player = next_player;

			// 客户端刷新手牌
			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], hand_cards);
			// 处理王牌和定王牌
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}
			table.operate_player_cards(_out_card_player, hand_card_count, hand_cards, 0, null);

			// 客户端显示出牌
			int real_card = _out_card_data;
			if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
			table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

			// TODO 显示听牌数据
			table._playerStatus[_out_card_player]._hu_card_count = table.get_ting_card(table._playerStatus[_out_card_player]._hu_cards,
					table.GRR._cards_index[_out_card_player], table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player],
					_out_card_player);
			int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
			int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

			if (ting_count > 0) {
				// 如果玩家出牌之后是听牌状态，就设置一下听牌状态
				playerStatus.set_ting_state(true);

				table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);

				playerStatus.set_ting_state(false);
			}

			// 把出的牌加入到废弃牌堆
			table.exe_add_discard(_out_card_player, 1, new int[] { real_card }, false, 0);
		}

		if (table.has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER) && table._out_card_count == 1 && _type != GameConstants.WIK_BAO_TING) {
			// 八王玩法，出地一张牌后，判断别人的响应之前，处理报听
			// 处理闲家的‘报听’
			table.exe_bao_ting(2, table._out_card_player, _out_card_data);

			return;
		}

		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

		// 分析出的牌有没有人进行‘吃’、‘碰’、‘杠’、‘胡’，相应的动作，直接在分析方法里处理好了
		boolean bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data);

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}

			// 发牌
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 0);
		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _out_card_player) {
					continue;
				}

				playerStatus = table._playerStatus[i];

				if (playerStatus.has_action()) {
					if (table.is_match() || table.isClubMatch() || table.isCoinRoom()) {
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					} else {
						if (table.is_bao_ting[i] || (table.istrustee[i] && table.is_gang_tuo_guan[i])) {
							if (playerStatus.has_chi_hu() || playerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
								table.operate_player_action(i, false);
								table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							} else {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _out_card_data);
							}
						} else if (table.istrustee[i]) {
							if (playerStatus.has_chi_hu()) {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_CHI_HU, _out_card_data);
							} else {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _out_card_data);
							}
						} else {
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							table.operate_player_action(i, false);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_TaoJiang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (GameConstants.WIK_NULL == operate_code) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		// 如果本来是能胡的，但是你点了过，本圈之内除非牌型分变大了，才能接炮
		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			if (table.is_bao_ting[seat_index]) {
				table.ting_state_pass_jie_pao[seat_index] = true;
			}

			table.score_when_abandoned_jie_pao[seat_index] = table.get_pai_xing_fen_jie_pao(table.GRR._chi_hu_rights[seat_index], seat_index);

			// 清空CHR
			table.GRR._chi_hu_rights[seat_index].set_empty();
		}

		// 下面的代码直接获取优先级最高的人进行操作，'胡'>'碰'/'杠'>'吃'，而且所有玩家按逆时针顺序判断相同的操作
		// A、B两人能同时胡，但是A是B的上家，那么就只能A胡牌，注意和一炮多响的不同之处
		// TODO: 暂时没看懂这个逻辑是怎么来的
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();

			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		// 如果优先级最高的人还没有操作
		// 注意一些不同的地方，一些小细节的东西，整个handler里的代码，对逻辑数据的处理是相当混乱的，一不小心就会踩bug
		// 玩家状态的operate方法会更新是否已相应，那么在本方法的前面几行判断代码就要相应的做调整
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		operate_card = _out_card_data;

		// 清空所有玩家的动作和状态
		// 下面这几行代码也要注意使用场景，别轻易用，容易造成bug，注意是clean方法别乱用，加一层过滤就可以了
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		// 注意下面几个不同之处，‘吃’牌或者‘碰’牌的时候，在exe方法之前处理手牌；‘杠’牌的时候，进入handler的exe_gang方法才会处理手牌
		// 这个不同点，对有些处理器的一小部分代码会有影响，处理时要注意
		switch (target_action) {
		case GameConstants.WIK_LEFT: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card + 1, operate_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_RIGHT: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card - 2 };

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_CENTER: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_PENG: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card, operate_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			// 是否有抢杠胡
			table.exe_gang(target_player, _out_card_player, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants.WIK_NULL: {
			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			// 发牌
			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != target_player) {
					table.GRR._chi_hu_rights[i].set_valid(false);
					table.GRR._chi_hu_rights[i].set_empty();
				}
			}

			table.card_type_when_win = Constants_TaoJiang.HU_CARD_TYPE_JIE_PAO;

			// 玩家出牌之后，有人胡牌，记录牌桌、出牌人以及胡牌人的状态信息
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table._cur_banker = target_player;

			table.set_niao_card(target_player);

			table.GRR._chi_hu_card[target_player][0] = _out_card_data;
			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;

			table.GRR._chi_hu_rights[_out_card_player].opr_or(Constants_TaoJiang.CHR_FANG_PAO);

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score(target_player, _out_card_player, _out_card_data, false);

			if (table.is_bao_ting[_out_card_player]) {
				// 如果出牌人已经报听
				if (table.get_da_hu_count(table.GRR._chi_hu_rights[_out_card_player]) > 0) {
					table._player_result.da_hu_jie_pao[target_player]++;
					table._player_result.da_hu_dian_pao[_out_card_player]++;
				} else {
					table._player_result.xiao_hu_jie_pao[target_player]++;
					table._player_result.xiao_hu_dian_pao[_out_card_player]++;
				}
			} else {
				if (table.get_da_hu_count(table.GRR._chi_hu_rights[target_player]) > 0) {
					table._player_result.da_hu_jie_pao[target_player]++;
					table._player_result.da_hu_dian_pao[_out_card_player]++;
				} else {
					table._player_result.xiao_hu_jie_pao[target_player]++;
					table._player_result.xiao_hu_dian_pao[_out_card_player]++;
				}
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), table.DELAY_GAME_FINISH,
					TimeUnit.MILLISECONDS);

			return true;
		}
		default:
			return false;
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TaoJiang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 离地王牌还有多少张
		if (table.distance_to_ding_wang_card > 0) {
			roomResponse.setOperateLen(table.distance_to_ding_wang_card);
		}

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table.GRR._discard_cards[i][j] == table.joker_card_1 || table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);

			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO 显示听牌数据
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
