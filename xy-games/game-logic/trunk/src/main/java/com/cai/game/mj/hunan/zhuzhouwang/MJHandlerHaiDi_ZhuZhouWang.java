package com.cai.game.mj.hunan.zhuzhouwang;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.handler.MJHandlerHaiDi;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerHaiDi_ZhuZhouWang extends AbstractMJHandler<Table_ZhuZhouWang> {

	private static Logger logger = Logger.getLogger(MJHandlerHaiDi_ZhuZhouWang.class);

	/**
	 * 1 = 庄家起手14张，出牌之前；2 = 庄家起手14张，并打出第一张牌之后；
	 */

	protected int _seat_index;
	protected int _type;

	public void reset_status(int seat_index, int type) {
		_type = type;
		_seat_index = seat_index;
	}

	/**
	 * 报听的时候，类型是什么，座位好是多少，出的牌是什么
	 * 
	 * @param _seat_index
	 * @param _card_data
	 */

	@Override
	public void exe(Table_ZhuZhouWang table) {
		// TODO Auto-generated method stub
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].chi_hu_round_valid();// 可以胡了
		}

		curPlayerStatus.add_action(GameConstants.WIK_YAO_HAI_DI);
		curPlayerStatus.add_yao_hai_di();
		table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
		table.operate_player_action(_seat_index, false);

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
	public boolean handler_operate_card(Table_ZhuZhouWang table, int seat_index, int operate_code, int operate_card) {
		if (seat_index != _seat_index) {
			logger.error("[海底],操作失败," + seat_index + "不是当前操作玩家");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			// 清除要海底动作
			table._playerStatus[seat_index].clean_action(GameConstants.WIK_YAO_HAI_DI);

			// 不要海底
			_seat_index = (_seat_index + 1) % table.getTablePlayerNumber();
			if (_seat_index == seat_index) {
				table._cur_banker = seat_index;

				// 流局
				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

				return true;
			}

			table.exe_hai_di(seat_index, _type);
		} else if (operate_code == GameConstants.WIK_YAO_HAI_DI) {
			table.exe_yao_hai_di(_seat_index,_type);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_ZhuZhouWang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		// 骰子
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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
				if (table.GRR._discard_cards[i][j] == table.joker_card_1
						|| table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
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
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
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
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (hand_cards[i] == table.joker_card_1 || hand_cards[i] == table.joker_card_2) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[i] == table.ding_wang_card) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
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
		// table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION,
		// 1, new long[]{GameConstants.WIK_YAO_HAI_DI}, 0,seat_index);

		// table.operate_player_get_card(_seat_index, 1, new int[]{_send_card_data});
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
