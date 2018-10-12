package com.cai.game.mj.yingjing;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_XTDGK;
import com.cai.common.constant.game.mj.GameConstants_YINGJING;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 摸牌
 *
 * @author WalkerGeek date: 2018年4月12日 下午9:27:17 <br/>
 */
public class MJHandlerDispatchCardFirst_YingJing extends MJHandlerDispatchCard<MJTable_YingJing> {
	boolean ting_send_card = false;

	@Override
	public void exe(MJTable_YingJing table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].chi_peng_round_valid();// 可以胡了

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._provide_player = _seat_index;

		table.add_dispatchcard_num(_seat_index);

		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x11;
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		// 胡牌检测
		int action = GameConstants.WIK_NULL;
		if (!table.getISHU(_seat_index)) {
			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO,
					_seat_index, GameConstants.INVALID_SEAT);// 自摸
		}
		boolean has_hu = false;
		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			has_hu = true;
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		table.addDispatchcardNum(_seat_index);
		//if (!has_hu) {
		this.getTingData(table, curPlayerStatus);
		//}

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		// 发送数据只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		// 设置变量
		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true);
			if (cbActionMask != GameConstants.WIK_NULL) {
				int count = 0 ;
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if(!table.checkLangQiCard(_seat_index, m_gangCardResult.cbCardData[i])){
						// 加上杠
						count ++;
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}
				}
				if(count > 0 ){
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}
		}
		// 其他玩家的报听
		boolean otherAction = ckeck_bao_ting(table);

		if (curPlayerStatus.has_action() || otherAction) {
			// 发送操作
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}

		} else {
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
		return;
	}

	public boolean ckeck_bao_ting(MJTable_YingJing table) {
		boolean flag = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index) {
				continue;
			}
			table._playerStatus[i].reset();
			table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards,
					table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i);
			if (table._playerStatus[i]._hu_card_count > 0) {
				// 报听
				table._playerStatus[i].add_action(GameConstants.WIK_BAO_TING);
				// 有动作
				flag = true;
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
			}
		}
		return flag;
	}

	/**
	 * 获取本人的听牌数据
	 * 
	 * @param table
	 * @param curPlayerStatus
	 */
	public void getTingData(MJTable_YingJing table, PlayerStatus curPlayerStatus) {
		// 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

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

			int cards[] = new int[GameConstants_XTDGK.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
			if (!table._player_result.is_qing_hu(_seat_index) && table.GRR._left_card_count >= 10) {
				curPlayerStatus.add_action(GameConstants_YINGJING.WIK_LNAG_QI);
			}
			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
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
	public boolean handler_operate_card(MJTable_YingJing table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		// 处理其他玩家的报听操作
		if (seat_index != _seat_index) {
			if (operate_code == GameConstants.WIK_BAO_TING) {
				table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { operate_code }, 1);
				// 记录玩家的操作
				playerStatus.operate(operate_code, operate_card);
				playerStatus.clean_status();

				table.operate_player_action(seat_index, true);
				// 设置为报听状态
				table._playerStatus[seat_index].set_card_status(GameConstants.CARD_STATUS_BAO_TING);

				// 刷新玩家状态
				table.handler_refresh_all_player_data();
				// 效果
				table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { GameConstants.WIK_BAO_TING }, 1, GameConstants.INVALID_SEAT);

			} else if (operate_code == GameConstants.WIK_NULL) {
				table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { GameConstants.WIK_NULL }, 1);
				table._playerStatus[seat_index].clean_action();
				table._playerStatus[seat_index].clean_status();
			}
			this.doAfterAtion(table);
			return true;
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

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			boolean flag = false;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action() && !table._playerStatus[i].is_respone()) {
					flag = true;
				}
			}
			if (!flag) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			doAfterAtion(table);

		}
			break;
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.chang_zhang(_seat_index, false);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.ISHUVaild(_seat_index, operate_card);
			table.ISHUVaild(_seat_index);
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			doAfterAtion(table);
			// 结束
			return true;
		}
		}

		return true;
	}

	/**
	 * 完成处理第一张牌的后续操作
	 * 
	 * @param table
	 * @return
	 */
	public void doAfterAtion(MJTable_YingJing table) {
		boolean flag = false;
		// 发送操作
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action() && !table._playerStatus[i].is_respone()) {
				flag = true;
			}
		}
		if (!flag) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			PlayerStatus playerStatus = table._playerStatus[_seat_index];
			int operate_card = playerStatus.get_operate_card();
			int operate_code = playerStatus.get_perform();

			switch (operate_code) {
			case GameConstants_YINGJING.WIK_LNAG_QI:
				// 报警判断
				table.operate_show_card(_seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI, GameConstants.INVALID_SEAT);
				table.exe_out_card(_seat_index, operate_card, GameConstants.WIK_NULL);
//				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//					if(i == _seat_index){
//						continue;
//					}
//					table.sendBaoJing(i,false,false);
//				}
				break;
			case GameConstants.WIK_GANG: // 杠牌操作
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (operate_card == m_gangCardResult.cbCardData[i]) {
						// 是否有抢杠胡
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i],
								true, false);
					}
				}
				break;

			case GameConstants.WIK_ZI_MO: // 自摸
				// 结束
				int next_player = table.getNextPalyerIndex(_seat_index);
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
				break;
			default:
				// 出牌状态
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

		}

	}

	@Override
	public boolean handler_player_be_in_room(MJTable_YingJing table, int seat_index) {
		
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addWinnerOrder(0);
			if (i == _seat_index) {
				tableResponse.addCardCount(
						table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1 - table.getLangQiCount(i));
			} else {
				tableResponse.addCardCount(
						table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - table.getLangQiCount(i));
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_XTDGK.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		hand_cards = table.getRealCards(hand_cards, hand_card_count, seat_index);
		//相公
		boolean flag = true;
		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] != 0 && !table.allLangQiCard.contains(hand_cards[j])) {
				flag = false;
			}
		}
		roomResponse.setIsXiangGong(flag);
		PlayerStatus playerStatus = table._playerStatus[_seat_index];
		int operate_code = playerStatus.get_perform();
		// 如果断线重连的人是自己
		if (seat_index == _seat_index && operate_code!=GameConstants_YINGJING.WIK_LNAG_QI  ) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == 0) {
					continue;
				}
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants_XTDGK.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

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

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		if(operate_code!=GameConstants_YINGJING.WIK_LNAG_QI){
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		}
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

		table.roomResponseShowHuCard(seat_index);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.operate_show_card(i, GameConstants_YINGJING.SHOW_CARD_LANG_QI, GameConstants.INVALID_SEAT);
		}
		return true;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable_YingJing table, int seat_index, int card) {
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

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_Bao_Hu) {
			table.exe_out_card_bao_ting(_seat_index, card, GameConstants.WIK_NULL);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}
}
