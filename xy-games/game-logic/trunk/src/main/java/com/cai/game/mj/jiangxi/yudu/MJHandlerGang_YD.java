package com.cai.game.mj.jiangxi.yudu;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ND;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_YD extends MJHandlerGang<MJTable_YD> {

	// 执行杆操作
	@Override
	public void exe(MJTable_YD table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}


		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		
		//打出的宝牌吃碰杠都不算
		if(_center_card==table.baoPai){
			table.baoPaiCount[_provide_player]--;
			if(table.baoPaiCount[_provide_player]<0){
				table.baoPaiCount[_provide_player]=0;
			}
		}
		if(table.baoPaiCount[_provide_player]==0){
			table.baoPaiScore[_provide_player]=0;
		}
		else if(table.baoPaiCount[_provide_player]==1){
			table.baoPaiScore[_provide_player]=5;
		}
		else if(table.baoPaiCount[_provide_player]==2){
			table.baoPaiScore[_provide_player]=15;
		}
		else if(table.baoPaiCount[_provide_player]==3){
			table.baoPaiScore[_provide_player]=45;
		}
		else if(table.baoPaiCount[_provide_player]==4){
			table.baoPaiScore[_provide_player]=135;
		}

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);// 暗杆 接杆不能抢
			return;
		}

		// 检查对这个杠有没有 胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			this.exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}

	}

	/***
	 * //用户操作 --抢杠胡
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@SuppressWarnings("unused")
	@Override
	public boolean handler_operate_card(MJTable_YD table, int seat_index, int operate_code, int operate_card) {

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { operate_code }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			table.process_chi_hu_player_operate(seat_index, _center_card, false);// 效果
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		// 不能一炮多响
		/*for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}*/

		// 变量定义 优先级最高操作的玩家和操作
		int target_player = seat_index;
		int target_action = operate_code;
		// 执行判断
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform());
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action);
				}

				// 优先级别
				int cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform());

				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
				}
			}
		}
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
			return true;
		// 变量定义
		int target_card = _center_card;
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			if (jie_pao_count > 1) {
				// 通炮
				table._cur_banker = _seat_index;
				/*
				 * for (int i = 0; i < table.getTablePlayerNumber(); i++) { for
				 * (int j = 0; j < table.GRR._player_niao_count[i]; j++) { if (i
				 * == _seat_index) { table.GRR._count_pick_niao++;
				 * table.GRR._player_niao_cards[i][j] = table
				 * .set_ding_niao_valid(table.GRR._player_niao_cards[i][j],
				 * true);// 胡牌的鸟生效 } else { table.GRR._player_niao_cards[i][j] =
				 * table
				 * .set_ding_niao_valid(table.GRR._player_niao_cards[i][j],
				 * false);// 胡牌的鸟失效 } } }
				 */
			} else if (jie_pao_count == 1) {

				int index = target_player;
				if (table.has_rule(GameConstants_ND.GAME_RULE_MA_FANG_WEI)) {
					index = table._cur_banker;
				}
				table._cur_banker = target_player;

				/*
				 * table._cur_banker = target_player;
				 * table.set_niao_card(target_player,
				 * GameConstants.INVALID_VALUE, true, 0);// 结束后设置鸟牌
				 */ /*
					 * for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					 * for (int j = 0; j < table.GRR._player_niao_count[i]; j++)
					 * { boolean flag =false; if (i == _seat_index) {
					 * table.GRR._count_pick_niao++; flag = true; }
					 * table.GRR._player_niao_cards[i][j] = table
					 * .set_ding_niao_valid(table.GRR._player_niao_cards[i][j],
					 * flag);// 胡牌的鸟生效 } }
					 */
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table.GRR._chi_hu_card[i][0] = _center_card;

				table.process_chi_hu_player_score(i, _seat_index, _center_card, false);

				// 记录
				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_provide_player]++;
			}

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		} else {
			// 选择了不胡 发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	@Override
	protected boolean exe_gang(MJTable_YD table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			//如果是第一轮吃碰杠，将是否跟庄置为false
			if(table.dispatchcardNum[table._cur_banker]==1){
				table.ifGenZhuang=false;
			}
			
			// 别人打的牌
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// 删掉出来的那张牌
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		if(_center_card==table.baoPai){
			table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card+GameConstants.CARD_ESPECIAL_TYPE_BAO;
		}else{
			table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		}
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.changeCard(cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR.getWeaveItemsForOut(_seat_index, new WeaveItem[GameConstants.MAX_WEAVE]));

		///////////////////////////////////////////////////// 算分
		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		int score = GameConstants.CELL_SCORE;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			//如果杠的宝牌 分数固定20/20/40
			if(_center_card==table.baoPai){
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index)
						continue;
					// 暗杠宝牌每人40分
					table.gangBaoScore[i] = -40;// 暗杠，其他玩家扣分
					table.gangBaoScore[_seat_index] += 40;// 一共加分
				}
			}else{
				score = 5*table.baseScore;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index)
						continue;
					// 暗杠每人5分
					table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
					table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
				}
				table._player_result.an_gang_count[_seat_index]++;
			}
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			if(_center_card==table.baoPai){
				score = 20;
				table.gangBaoScore[_seat_index] = score;// m_pGameServiceOption->lCellScore*2;//配置参数乘
				table.gangBaoScore[_provide_player] = -score;// -m_pGameServiceOption->lCellScore*2;//配置参数乘
			}
			else{
				//明杠1分
				score = 1*table.baseScore;
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score;// m_pGameServiceOption->lCellScore*2;//配置参数乘
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -score;// -m_pGameServiceOption->lCellScore*2;//配置参数乘
				table._player_result.ming_gang_count[_seat_index]++;
			}
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			if(_center_card==table.baoPai){
				score = 20;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index)
						continue;
					table.gangBaoScore[i] = -score;// 补杠，其他玩家扣分
					table.gangBaoScore[_seat_index] += score;// 一共加分
				}
			}
			else{
				score = 2*table.baseScore;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _seat_index)
						continue;

					// 补杠每人2分
					table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -score;// 暗杠，其他玩家扣分
					table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += score;// 一共加分
				}
				table._player_result.ming_gang_count[_seat_index]++;
			}

		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = score;
		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, GameConstants_YD.HU_CARD_TYPE_GANG_SHANG_KAI_HUA, 0);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_YD table, int seat_index) {
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
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			// if(_self){
			// if(i == _seat_index){
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			// }else{
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			// }
			// }else{
			// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			// }

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		table.changeCard(hand_cards);
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
