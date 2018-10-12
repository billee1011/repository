package com.cai.game.mj.hunan.taojiang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGangDispatchCard_TaoJiang<T extends AbstractMJTable> extends AbstractMJHandler<Table_TaoJiang> {

	protected int _seat_index;
	protected CardsData _gang_card_data;
	protected int _xuan_mei_count;

	public HandlerGangDispatchCard_TaoJiang() {
		_gang_card_data = new CardsData(4);
	}

	public void reset_status(int seat_index, int xuan_mei_count) {
		_seat_index = seat_index;
		_xuan_mei_count = xuan_mei_count;
		_gang_card_data.clean_cards();
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_TaoJiang table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._provide_player = _seat_index;

		table._current_player = _seat_index;

		table.seat_index_when_win = _seat_index;

		// 这里基本不会走，杠牌时，牌堆里必然还有1张以上的牌，但是杠完选美之后，如果牌堆没牌，会直接流局
		if (_xuan_mei_count == 0)
			return;

		table._send_card_count += _xuan_mei_count;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		int tmp_xuan_mei_cards[] = new int[_xuan_mei_count];

		// 从剩余牌堆里顺序取出选美的牌
		table._logic.switch_to_cards_index(table._repertory_card, table._all_card_len - table.GRR._left_card_count, _xuan_mei_count, cbCardIndexTemp);

		// 往_gang_card_data进行存储，断线重连时能用得到，下面的判断也可以使用
		table._logic.switch_to_cards_data(cbCardIndexTemp, tmp_xuan_mei_cards);

		if (table.DEBUG_CARDS_MODE) {
			if (_xuan_mei_count == 2) {
				tmp_xuan_mei_cards = new int[] { 0x16, 0x17 };
			} else if (_xuan_mei_count == 3) {
				tmp_xuan_mei_cards = new int[] { 0x01, 0x26, 0x27 };
			}
		}

		// 处理鬼牌和存储杠牌数据
		// 在牌桌正中央显示牌时用_gang_card_data，分析吃碰胡时用tmp_xuan_mei_cards
		for (int i = 0; i < tmp_xuan_mei_cards.length; i++) {
			if (tmp_xuan_mei_cards[i] == table.joker_card_1 || tmp_xuan_mei_cards[i] == table.joker_card_2) {
				_gang_card_data.add_card(tmp_xuan_mei_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
			} else if (tmp_xuan_mei_cards[i] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				_gang_card_data.add_card(tmp_xuan_mei_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
			} else {
				_gang_card_data.add_card(tmp_xuan_mei_cards[i]);
			}
		}

		table._gang_card_data = _gang_card_data;

		table.GRR._left_card_count -= _xuan_mei_count;

		if (table.distance_to_ding_wang_card > 0) {
			table.distance_to_ding_wang_card = table.GRR._left_card_count - (table.tou_zi_dian_shu[0] + table.tou_zi_dian_shu[1]) * 2;
		}

		// 想选美的牌显示在牌桌正中央，分析完之后，重牌桌正中央删除并放到杠牌玩家的面前，吃碰杠了的牌不用显示
		table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, _xuan_mei_count, _gang_card_data.get_cards(), GameConstants.INVALID_SEAT,
				false);

		// 分析选美翻出来的牌时，所有玩家都要判断
		boolean bAroseAction = table.estimate_gang_fa_pai(_seat_index, tmp_xuan_mei_cards);

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
				table.operate_player_action(i, true);
			}

			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, _gang_card_data.get_cards(), GameConstants.INVALID_SEAT,
							false);

					// 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
					table.operate_out_card(_seat_index, table.get_xuan_mei_count(), _gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_MID,
							GameConstants.INVALID_SEAT);

					// 把选美的牌加入到废弃牌堆
					table.exe_add_discard(_seat_index, table.get_xuan_mei_count(), _gang_card_data.get_cards(), false, 0);

					// 发牌给下家
					table.exe_dispatch_card(table.get_banker_next_seat(_seat_index), GameConstants.WIK_NULL, 0);
				}
			}, table.DELAY_GANG_DISPATCH, TimeUnit.MILLISECONDS);
		} else {
			// 等待别人操作这些牌
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];

				if (table.is_match() || table.isClubMatch() || table.isCoinRoom()) {
					if (playerStatus.has_action()) {
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					}
				} else {
					if (table.is_bao_ting[i] || (table.istrustee[i] && table.is_gang_tuo_guan[i])) {
						if (playerStatus.has_zi_mo() || playerStatus.has_chi_hu() || playerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
							table.operate_player_action(i, false);
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						}
					} else if (table.istrustee[i]) {
						if (playerStatus.has_zi_mo()) {
							table.exe_jian_pao_hu(i, GameConstants.WIK_ZI_MO, table.win_card_at_gang[i]);
						} else if (playerStatus.has_chi_hu()) {
							table.exe_jian_pao_hu(i, GameConstants.WIK_CHI_HU, table.win_card_at_gang[i]);
						}
					} else {
						if (playerStatus.has_action()) {
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							table.operate_player_action(i, false);
						}
					}
				}
			}
		}
	}

	// 杠选美之后，有可能玩家会多次进入这个方法
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

		if (playerStatus.is_respone() == false) { // 如果已经相应了，不能重复设置操作状态
			playerStatus.operate(operate_code, operate_card);
		}
		playerStatus.clean_status();

		if (GameConstants.WIK_NULL == operate_code) {
			// 注意了，玩家在客户端点了过之后，玩家就已经相应了，需要记录一下，不然又是个坑
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			if (seat_index == _seat_index) {
				// TODO 开杠后，没胡牌，相当于进入自动托管
				table.handler_request_trustee(_seat_index, true, 0);
				table.is_gang_tuo_guan[_seat_index] = true;
			}
		}

		// 判断优先级，杠牌的那个人胡牌时优先级最高，点胡的时候，直接一次把所有能胡的牌都胡了
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();

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

		// 清空所有玩家的动作和状态
		// 下面这几行代码也要注意使用场景，别轻易用，容易造成bug，注意是clean方法别乱用，加一层过滤就可以了
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		// 注意下面这几行代码，有个大坑等你踩，如果你不注意的话
		// 方法体本身传入的operate_card和玩家状态的_operate_card会根据玩家在客户端点击的先后顺序，为0或者为实际操作的牌
		int tmp_operate_card = table._playerStatus[target_player]._operate_card;
		// if (operate_card == 0 && tmp_operate_card == 0) {
		// table.log_player_error(seat_index, "出牌，操作牌时出错");
		// return true;
		// }
		if (operate_card == 0 && tmp_operate_card != 0)
			operate_card = tmp_operate_card;

		// 记录哪些牌没人操作的，往废弃牌堆放
		int[] tmp_remove_cards = new int[_xuan_mei_count];
		int[] tmp_xuan_mei_cards = _gang_card_data.get_cards();
		int tmp_count = 0;
		boolean removed = false;
		for (int x = 0; x < _xuan_mei_count; x++) {
			if (table.get_real_card(tmp_xuan_mei_cards[x]) == operate_card && removed == false) {
				removed = true;
			} else {
				tmp_remove_cards[tmp_count++] = tmp_xuan_mei_cards[x];
			}
		}

		// 注意下面几个不同之处，‘吃’牌或者‘碰’牌的时候，在exe方法之前处理手牌；‘杠’牌的时候，进入handler的exe_gang方法才会处理手牌
		// 这个不同点，对有些处理器的一小部分代码会有影响，处理时要注意
		switch (target_action) {
		case GameConstants.WIK_NULL: {
			// 用户切换
			table._current_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			// 将翻出来的牌从牌桌的正中央移除
			table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, _gang_card_data.get_cards(), GameConstants.INVALID_SEAT, false);

			// 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
			table.operate_out_card(_seat_index, tmp_count, tmp_remove_cards, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

			// 将选美翻出来的牌里面没人操作的牌往费牌堆放
			table.exe_add_discard(_seat_index, tmp_count, tmp_remove_cards, false, 0);

			// 发牌
			table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.card_type_when_win = Constants_TaoJiang.HU_CARD_TYPE_GANG_HU;

			// 玩家出牌之后，有人胡牌，记录牌桌、出牌人以及胡牌人的状态信息
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table._cur_banker = target_player;

			table.set_niao_card(target_player);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_seat_index]++;

			table.GRR._chi_hu_rights[_seat_index].opr_or(Constants_TaoJiang.CHR_FANG_PAO);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			// 将翻出来的牌从牌桌的正中央移除
			table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 0, _gang_card_data.get_cards(), GameConstants.INVALID_SEAT, false);

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score(target_player, _seat_index, operate_card, false);

			if (table.get_da_hu_count(table.GRR._chi_hu_rights[_seat_index]) > 0) {
				table._player_result.da_hu_jie_pao[target_player]++;
				table._player_result.da_hu_dian_pao[_seat_index]++;
			} else {
				table._player_result.xiao_hu_jie_pao[target_player]++;
				table._player_result.xiao_hu_dian_pao[_seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), table.DELAY_GAME_FINISH,
					TimeUnit.MILLISECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.card_type_when_win = Constants_TaoJiang.HU_CARD_TYPE_GANG_KAI;

			// 点了胡牌之后，设置牌局和牌桌的一些状态值，计算分数并且立即结束本局游戏
			table.GRR._chi_hu_rights[target_player].set_valid(true);
			table._cur_banker = target_player;

			table.set_niao_card(target_player);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._player_result.zi_mo_count[target_player]++;

			// 将翻出来的牌从牌桌的正中央移除
			table.operate_show_card(target_player, GameConstants.Show_Card_Center, 0, _gang_card_data.get_cards(), GameConstants.INVALID_SEAT, false);

			// 客户端弹出来相应的动画效果，并处理手牌和落地的牌
			table.process_chi_hu_player_operate(target_player, operate_card, false);
			// 计算发牌时的自摸胡分数
			table.process_chi_hu_player_score(target_player, _seat_index, operate_card, true);

			if (table.get_da_hu_count(table.GRR._chi_hu_rights[target_player]) > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}

			// 发牌时点了胡牌，游戏会立即结束，出牌时有人点了操作，要等所有人操作完之后，游戏才结束
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), table.DELAY_GAME_FINISH,
					TimeUnit.MILLISECONDS);

			return true;
		}
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
		tableResponse.setCurrentPlayer(_seat_index);
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

		// 比吃碰断线重连多了一个在客户端牌桌正中央显示选美牌的效果
		table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, _xuan_mei_count, _gang_card_data.get_cards(), GameConstants.INVALID_SEAT,
				true);

		return true;
	}
}
