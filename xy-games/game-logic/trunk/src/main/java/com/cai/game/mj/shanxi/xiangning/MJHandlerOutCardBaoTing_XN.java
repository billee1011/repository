package com.cai.game.mj.shanxi.xiangning;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_XiangNing;
import com.cai.common.domain.ChiHuRight;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 报听
 * 
 * @author Administrator
 *
 */
public class MJHandlerOutCardBaoTing_XN extends AbstractMJHandler<MJTable_XN> {
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _type;

	public void reset_status(int seat_index, int card, int type) {
		_out_card_player = seat_index;
		_out_card_data = card;
		_type = type;
	}

	public void exe(MJTable_XN table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table.operate_player_action(_out_card_player, true);

		// 设置为报听状态
		table._playerStatus[_out_card_player].set_card_status(GameConstants.CARD_STATUS_BAO_TING);

		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		// 用户切换
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;

		// 效果
		table.operate_effect_action(_out_card_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
				new long[] { GameConstants.WIK_BAO_TING }, 1, GameConstants.INVALID_SEAT);

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
		table.changeCard(cards);
		table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);

		int real_card = _out_card_data + GameConstants.CARD_ESPECIAL_TYPE_TING_XN;
		if (table._logic.is_magic_card(_out_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_JING;
		}
		// 出牌
		table.operate_out_card_bao_ting(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);

		// 摔金
		if (table._logic.is_magic_card(_out_card_data)) {
			table.operate_effect_action(_out_card_player, GameConstants.Effect_Action_Other, 1,
					new long[] { GameConstants_XiangNing.EFFECT_ACTION_OTHER_SHUAI_JING }, 1000,
					GameConstants.INVALID_SEAT);
			// 记录摔金次数
			table._player_result.biaoyan[_out_card_player]++;
			// 刷新摔金玩家摔金信息
			table.handler_refresh_all_player_data();
		}

		// 听的牌，保存，下次不用计算
		int ting_count = table._playerStatus[_out_card_player]._hu_out_card_count;
		for (int i = 0; i < ting_count; i++) {
			int out_card = table._playerStatus[_out_card_player]._hu_out_card_ting[i];
			if (out_card == _out_card_data) {
				int tc = table._playerStatus[_out_card_player]._hu_card_count = table._playerStatus[_out_card_player]._hu_out_card_ting_count[i];
				for (int j = 0; j < tc; j++) {
					table._playerStatus[_out_card_player]._hu_cards[j] = table._playerStatus[_out_card_player]._hu_out_cards[i][j];
				}
			}
		}
		
		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
		}

		// 引用权位
		ChiHuRight chr = table.GRR._chi_hu_rights[_out_card_player];

		chr.bao_ting_index = table.GRR._discard_count[_out_card_player];
		chr.bao_ting_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false,
				GameConstants.DELAY_SEND_CARD_DELAY);

		// 发牌
		table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

	}

	@Override
	public boolean handler_player_be_in_room(MJTable_XN table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
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
				int iCardIndex = table.GRR._discard_cards[i][j];
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					if(seat_index == i){
						if(table._logic.is_magic_card(iCardIndex)){
							iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_JING;
						}
						iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_TING_XN;
					}else{
						if(table._logic.is_magic_card(iCardIndex)){
							iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_JING;
							iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_TING_XN;
						}else{
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
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(
							table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
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
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		table.changeCard(cards);
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _out_card_data;
		if (table._logic.is_magic_card(_out_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		// 出牌
		table.operate_out_card(_out_card_player, 1, new int[] { real_card }, GameConstants.OUT_CARD_TYPE_MID,
				seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}

}
