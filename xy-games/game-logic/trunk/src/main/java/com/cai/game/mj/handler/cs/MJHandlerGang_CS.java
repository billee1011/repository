package com.cai.game.mj.handler.cs;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerGang_CS extends MJHandlerGang<MJTable> {
	@Override
	public void exe(MJTable table) {
		// TODO Auto-generated method stub
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			// table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);
			return;
		}

		// 检查对这个杠有没有 胡
		boolean bAroseAction = table.estimate_gang_respond_cs(_seat_index, _center_card, _action);

		if (bAroseAction == false) {
			this.exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					// table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);//操作状态
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}

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
	public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
		// 抢杠胡

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
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
			table.process_chi_hu_player_operate_cs(seat_index, new int[] { _center_card }, 1, false);// 效果
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		// //清理这个玩家状态

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}

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
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action);
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

		operate_card = _center_card;

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		int jie_pao_count = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {// (i ==
																	// _provide_player)
																	// ||
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			if (jie_pao_count > 1) {
				// 结束
				table._cur_banker = _seat_index;
				// 红中通炮就不加鸟了
				table.set_niao_card_cs(_provide_player, GameConstants.INVALID_VALUE, true, 0, true);// 结束后设置鸟牌
			} else if (jie_pao_count == 1) {
				table._cur_banker = target_player;

				table.set_niao_card_cs(target_player, GameConstants.INVALID_VALUE, true, 0, false);// 结束后设置鸟牌
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {// (i ==
																		// _provide_player)
																		// ||
					continue;
				}

				table.GRR._chi_hu_card[i][0] = _center_card;

				table.process_chi_hu_player_score_cs(i, _seat_index, false);

				// 记录
				table._player_result.da_hu_jie_pao[i]++;
				table._player_result.da_hu_dian_pao[_seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		} else {
			// 选择了不胡
			// 发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	protected boolean exe_gang(MJTable table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table._player_result.an_gang_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌

			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// 删掉出来的那张牌
			// table.operate_out_card(_provide_player, 0,
			// null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
			table._player_result.ming_gang_count[_seat_index]++;
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
			table._player_result.ming_gang_count[_seat_index]++;
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		// 长沙麻将杠没有分
		if (_action == GameConstants.WIK_BU_ZHNAG) {
			// 从后面发一张牌给玩家
			table.exe_dispatch_card(_seat_index, _action, 0);
		} else if (_action == GameConstants.WIK_GANG) {
			if (table.is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
				// 从后面发两张牌给玩家 9.18 修改双杠上开花不显示bug WalkerGeek
				show_tou_zi(table);

				table.exe_gang_thj_cs(_seat_index, _double);
			} else {
				table.exe_gang_cs(_seat_index, _double);
			}
		}

		return true;

	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {
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
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

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
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	public void show_tou_zi(MJTable table) {
		int tou_zi_one = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		int tou_zi_two = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		operate_tou_zi_effect(table, tou_zi_one, tou_zi_two, table.time_for_tou_zi_animation, table.time_for_tou_zi_fade);
	}

	public boolean operate_tou_zi_effect(MJTable table, int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setTarget(0);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		table.send_response_to_room(roomResponse);

		if (table.GRR != null) {
			table.GRR.add_room_response(roomResponse);
		}

		return true;
	}
}
