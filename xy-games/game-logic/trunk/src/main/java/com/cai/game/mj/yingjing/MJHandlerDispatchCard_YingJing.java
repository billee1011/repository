package com.cai.game.mj.yingjing;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_YINGJING;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.util.SysParamServerUtil;

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
public class MJHandlerDispatchCard_YingJing extends MJHandlerDispatchCard<MJTable_YingJing> {
	boolean ting_send_card = false;

	@Override
	public void exe(MJTable_YingJing table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		// 解除过手胡
		table.fan_shu_when_abandoned_jie_pao[_seat_index] = 0;
		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		table._playerStatus[_seat_index].chi_peng_round_valid();// 可以胡了

		if (table.needFinish()) {
			// 庄家切换
			table._cur_banker = table.next_banker_player;

			table.process_show_hand_card();
			// table.process_gang_score();

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			return;
		}
		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			 //table.GRR._left_card_count = 0;
		}
		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			// 查大叫>> 庄家切换
			int zhuang = table.cha_da_jiao();
			if (zhuang != GameConstants.INVALID_SEAT) {
				table._cur_banker = zhuang;
			} else {
				table._cur_banker = table.next_banker_player;
			}

			table.process_show_hand_card();
			//table.process_gang_score();

			// 流局
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_DRAW),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		//WalkerGeek 新加需求  :已听牌未廊起，手里牌全部是其他玩家廊起后的炮牌，在轮到自己摸牌前，先弹出廊起按钮，给他一次廊起的机会
		if(_type != GameConstants_YINGJING.WIK_LNAG_QI_GUO && table._playerStatus[_seat_index]._hu_card_count > 0 && !table._player_result.is_qing_hu(_seat_index)){
			boolean flag = true;
			//所有牌是不是廊起的炮牌
			for(int i = 0; i < GameConstants.MAX_INDEX; i++ ){
				if(table.GRR._cards_index[_seat_index][i] > 0){
					if(!table.allLangQiCard.contains(table._logic.switch_to_card_data(i))){
						flag = false;
					}
				}
			}
			if(flag){
				curPlayerStatus.add_action(GameConstants_YINGJING.WIK_LNAG_QI2);
				// 操作状态
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
				return ;
			}
		}
		
		

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._provide_player = _seat_index;
		table.add_dispatchcard_num(_seat_index);
		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_send_card_data = 0x11;
			// table.GRR._left_card_count = 0;
		}

		table._send_card_data = _send_card_data;

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			type = GameConstants_YINGJING.HU_CARD_TYPE_GANG_KAI;
		}
		// 胡牌检测
		int action = GameConstants.WIK_NULL;
		if (!table.getISHU(_seat_index)) {
			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, type, _seat_index,
					GameConstants.INVALID_SEAT);// 自摸
		}

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			// }
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
		// 记录摸牌次数
		table.addDispatchcardNum(_seat_index);

		if (SysParamServerUtil.is_new_algorithm(3000, 3000, 1)) {
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
							table._playerStatus[_seat_index]._hu_out_cards[ting_count],
							table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index);

					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0
							&& !table.allLangQiCard.contains(table._logic.switch_to_card_data(i))) {
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

				int cards[] = new int[GameConstants_YINGJING.MAX_COUNT];
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

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		// 发送数据只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		table.sendBaoJing(_seat_index,table._playerStatus[_seat_index]._hu_card_count > 0,true,_send_card_data);
		// 设置变量
		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = table.analyse_gang_card(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
					_seat_index);
			if (cbActionMask != GameConstants.WIK_NULL) {

				boolean has_gang = false;

				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					boolean flag = false;
					if(table.isLangQiPaoPai(_seat_index, m_gangCardResult.cbCardData[i])){
						if(table._player_result.is_bao_hu(_seat_index) || table._player_result.is_qing_hu(_seat_index) || table._playerStatus[_seat_index].is_bao_ting() ){
							if(!table.check_gang_huan_zhang(_seat_index, m_gangCardResult.cbCardData[i]) && m_gangCardResult.type[i] != GameConstants.GANG_TYPE_AN_GANG){
								flag =true;
							}
						}else{
							flag = true;
						}
					}
					if (flag) {
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
		if (curPlayerStatus.has_zi_mo() && table._player_result.is_qing_hu(_seat_index)) {
			// 见炮胡
			table.exe_jian_pao_hu(_seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
			return;
		}

		if (curPlayerStatus.has_action()) {// 有动作
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
			} else {
				// 出牌状态
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
	public boolean handler_operate_card(MJTable_YingJing table, int seat_index, int operate_code, int operate_card) {
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
			if((playerStatus.has_action_by_code(GameConstants_YINGJING.WIK_LNAG_QI2))){
				table.exe_dispatch_card(seat_index, GameConstants_YINGJING.WIK_LNAG_QI_GUO, 0);
			}else if (table._playerStatus[_seat_index].is_bao_ting()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						SysParamServerUtil.auto_out_card_time_mj(), TimeUnit.MILLISECONDS);
			}else{
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
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

			table.chang_zhang(_seat_index, false);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.ISHUVaild(_seat_index, operate_card);
			table.ISHUVaild(_seat_index);

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			// 结束
			int next_player = table.getNextPalyerIndex(_seat_index);
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

			return true;
		}

		}

		return true;
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
				if(table._playerStatus[seat_index].has_action_by_code(GameConstants_YINGJING.WIK_LNAG_QI2)){
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-table.getLangQiCount(i));
				}else{
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1-table.getLangQiCount(i));
				}
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-table.getLangQiCount(i));
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants_YINGJING.MAX_COUNT];
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
		// 如果断线重连的人是自己
		if (seat_index == _seat_index && !table._playerStatus[seat_index].has_action_by_code(GameConstants_YINGJING.WIK_LNAG_QI2)) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		
		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				if(hand_cards[j] == 0){
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

		for (int i = 0; i < GameConstants_YINGJING.MAX_COUNT; i++) {
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

		if(!table._playerStatus[seat_index].has_action_by_code(GameConstants_YINGJING.WIK_LNAG_QI2)){
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
		for(int i = 0; i < table.getTablePlayerNumber(); i++){
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
		} else if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants_YINGJING.HU_CARD_TYPE_GANG_PAO);
		} else {
			// 出牌
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}
}
