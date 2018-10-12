package com.cai.game.mj.jiangxi.yudu;

import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.RevomeBaoMiddleCardRunnableYD;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.AbstractMJHandler;

/**
 * 
 * @author WalkerGeek date: 2018年3月15日 下午3:39:07 <br/>
 */
public class MJHandlerBao_YD extends AbstractMJHandler<MJTable_YD> {

	protected int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(MJTable_YD table) {
		table._logic.clean_magic_cards();
		// 从牌堆随机
		_da_dian_card = table._repertory_card[RandomUtil.getRandomNumber(table._all_card_len - 1)];

		if (AbstractMJTable.DEBUG_CARDS_MODE) {
			_da_dian_card = 0x32;
		}

		// 显示
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card },
				GameConstants.INVALID_SEAT);

		
		// 添加宝牌
		int index = CalculateMasterCard(table, _da_dian_card);

		table._logic.add_magic_card_index(index);
		/*if (index <= table._logic.switch_to_card_index(0x37)) {
			table._logic.has_jia_bao_valid();
		}else{
			table._logic.clean_magic_cards();
			for (int i = table._logic.switch_to_card_index(0x38); i < 4; i++) {
				table._logic.add_magic_card_index(i);
			}
		}*/

		/*if (table._logic.get_has_jia_bao()) {
		for (int i = table._logic.switch_to_card_index(0x38); i < 4; i++) {
			table.GRR._especial_show_cards[table.GRR._especial_card_count] = table._logic.switch_to_card_data(i);
			table.GRR._especial_card_count++;
		}
	
		} else {
			table.GRR._especial_card_count = 1;
			table.GRR._especial_show_cards[0] = table._logic.switch_to_card_data(index);
		}
		*/
		
		
		
		//初始化宝牌
		table.GRR._especial_card_count = 0;
		for (int i = 0; i < 2; i++) {
			int card = _da_dian_card;
			if(i > 0){
				card = table._logic.switch_to_card_data(index);
				card+= GameConstants.CARD_ESPECIAL_TYPE_BAO;
				//将宝牌牌值存进变量中
			}
			
			table.GRR._especial_show_cards[table.GRR._especial_card_count] =  card;
			table.GRR._especial_card_count ++;
		}
		table.baoPai=table._logic.switch_to_card_data(index);

		// 延迟调度打点结束（发牌）
		GameSchedule.put(new RevomeBaoMiddleCardRunnableYD(table.getRoom_id(), _banker), 1, TimeUnit.SECONDS);

	}

	//计算宝牌
	public int CalculateMasterCard(MJTable_YD table, int card) {
		int index = table._logic.switch_to_card_index(card);
		int card_num = table._logic.get_card_value(card);
		int corlor = table._logic.get_card_color(card);
		int next_index = index;
		if (card_num > 0 && card_num < 9 && corlor < 3) { // 9的循环是
			next_index = index + 1;
		} else if (card_num == 9 && corlor < 3) {
			next_index = index - 8;
		} else{
			if (index == table._logic.switch_to_card_index(0x31)||index == table._logic.switch_to_card_index(0x35)||index == table._logic.switch_to_card_index(0x36)) {
				next_index = index +1;
			}
			else if (index == table._logic.switch_to_card_index(0x32)) {
				next_index = index +2;
			} 
			else if (index == table._logic.switch_to_card_index(0x33)) {
				next_index = index - 1;
			}
			else if (index == table._logic.switch_to_card_index(0x34)) {
				next_index = index - 3;
			} 
			else if (index == table._logic.switch_to_card_index(0x37)) {
				next_index = index - 2;
			}
		}  

		return next_index;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_YD table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
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
				if (table.is_bao_pai(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_BAO);
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

		// 显示
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[_da_dian_card], seat_index);

		return true;
	}

}
