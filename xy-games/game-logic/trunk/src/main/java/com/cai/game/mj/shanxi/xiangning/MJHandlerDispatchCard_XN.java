package com.cai.game.mj.shanxi.xiangning;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_XiangNing;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 
 * 
 *
 * @author WalkerGeek date: 2018年6月13日 下午4:17:09 <br/>
 */
public class MJHandlerDispatchCard_XN extends MJHandlerDispatchCard<MJTable_XN> {
	boolean ting_send_card = false;
	private boolean can_bao_ting;

	@Override
	public void exe(MJTable_XN table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		//table.GRR._left_card_count = 0;
		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._cur_banker = table._cur_banker;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._provide_player = _seat_index;

		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x05;
		}
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌 ：胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
		}

		// 不报听能胡
		if (table.has_rule(GameConstants_XiangNing.GAME_RULE_MJ_XN_BAO_TING_BU_BAO_HU)
				|| (table.has_rule(GameConstants_XiangNing.GAME_RULE_MJ_XN_BAO_TING_BAO_HU)
						&& table._playerStatus[_seat_index].is_bao_ting())) {
			table.tingCardCount = table.getTingCardCount(_seat_index);
			// 胡牌检测
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
					card_type, _seat_index);// 自摸
			if (action != GameConstants.WIK_NULL) {
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			} else {
				table.GRR._chi_hu_rights[_seat_index].set_empty();
				chr.set_empty();
			}
		}
		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;
				if(table._logic.is_magic_index(i)){
					table.outTingCardIsMagic = true;
				}
				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], false, _seat_index);

				if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
					table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic
							.switch_to_card_data(i);
					ting_count++;
					if (send_card_index == i) {
						ting_send_card = true;
					}
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			table.GRR._cards_index[_seat_index][send_card_index]--;
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			/*boolean isTrue = false;
			for (int j = 0; j < ting_count; j++) {
				if(!table._logic.is_magic_card(table._playerStatus[_seat_index]._hu_out_card_ting[j])){
					isTrue = true;
				}
			}
			if(isTrue){
				table.changeCard(cards);
			}
			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}*/
			table.changeCard(cards, table._playerStatus[_seat_index]._hu_out_card_ting, ting_count);
			table.GRR._cards_index[_seat_index][send_card_index]++;
			
			can_bao_ting = true;
			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
			if(!table._playerStatus[_seat_index].is_bao_ting() &&!table.has_rule(GameConstants_XiangNing.GAME_RULE_MJ_XN_BAO_TING_BU_BAO_HU)){
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- End
		int cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		int real_card = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			real_card = table.changeCard(_send_card_data);
		}
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} 

		// 发送数据 只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = table._logic.analyse_gang_card_XN(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,-1);
			if (cbActionMask != GameConstants.WIK_NULL) {
				boolean has_gang = false;
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if ((m_gangCardResult.type[i] != GameConstants.GANG_TYPE_AN_GANG)
							|| (table._playerStatus[_seat_index].is_bao_ting() == false)
							|| (table.check_gang_huan_zhang(_seat_index, m_gangCardResult.cbCardData[i]) == false)) {
						has_gang = true;
						// 加上杠
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
								m_gangCardResult.isPublic[i]);
					}
				}
				if (has_gang == true) {
					curPlayerStatus.add_action(GameConstants.WIK_GANG);// 杠
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			 // 不能换章,自动出牌
            if (table._playerStatus[_seat_index].is_bao_ting()) {
                GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
                        GameConstants.DELAY_AUTO_OUT_CARD+800, TimeUnit.MILLISECONDS);
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
	public boolean handler_operate_card(MJTable_XN table, int seat_index, int operate_code, int operate_card) {
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
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD + 800, TimeUnit.MILLISECONDS);
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
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
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
			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;

			// 结束
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_BAO_TING: {
			operate_card = table.get_real_card(operate_card);
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
			table.exe_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);
			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_XN table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

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
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					if (seat_index == i) {
						if (table._logic.is_magic_card(iCardIndex)) {
							iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_JING;
						}
						iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_TING_XN;
					} else {
						if (table._logic.is_magic_card(iCardIndex)) {
							iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_JING;
							iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_TING_XN;
						} else {
							iCardIndex = GameConstants.BLACK_CARD;
						}
					}
				} else {
					iCardIndex = table.changeCard(table.GRR._discard_cards[i][j]);
				}
				int_array.addItem(iCardIndex);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
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

			// 牌
			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		/*boolean isTrue = false;
		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						if(!table._logic.is_magic_card(table._playerStatus[seat_index]._hu_out_card_ting[k])){
							isTrue = true;
						}
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}
		if(isTrue){
			table.changeCard(hand_cards);
		}*/
		table.changeCard(hand_cards, table._playerStatus[_seat_index]._hu_out_card_ting, out_ting_count);
		

		roomResponse.setOutCardCount(out_ting_count);
		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(
					table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			real_card = table.changeCard(_send_card_data);
		}
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 听牌显示
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

	@Override
	public boolean handler_player_out_card(MJTable_XN table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		// if (card == MJGameConstants.ZZ_MAGIC_CARD &&
		// table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
		// table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
		// table.log_error("癞子牌不能出癞子");
		// return false;
		// }

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.HU_CARD_TYPE_GANG_PAO);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}
}
