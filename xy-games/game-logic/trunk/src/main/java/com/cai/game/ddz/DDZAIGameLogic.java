/**
 * 
 */
package com.cai.game.ddz;

import com.cai.common.constant.GameConstants;

//手牌权值结构  
class HandCardValue {
	int SumValue; // 手牌总价值
	int NeedRound; // 需要打几手牌
};

// 牌型组合数据结构
class CardGroupData {
	// 枚举类型
	int cgType = GameConstants.DDZ_CT_ERROR;
	// 该牌的价值
	int nValue = 0;
	// 含牌的个数
	int nCount = 0;
	// 牌中决定大小的牌值，用于对比
	int nMaxCard = 0;
};

public class DDZAIGameLogic {

	public DDZGameLogic _logic = null;

	public DDZAIGameLogic() {
		_logic = new DDZGameLogic();
	}

	public static void AI_QIANG_DI_ZHU(DDZTable table, int seat_index) {
		if (table._banker_select == GameConstants.INVALID_SEAT) {
			table._handler_call_banker.handler_call_banker(table, seat_index, 1, -1);
		} else {
			table._handler_call_banker.handler_call_banker(table, seat_index, -1, 0);
		}
	}

	public static void AI_Land_Score(DDZTable table, int seat_index) {
		table._handler_call_banker.handler_call_banker(table, seat_index, 3, -1);
	}

	public static void AI_Out_Card(DDZTable table, int seat_index) {
		if (table._turn_out_card_count == 0) {
			int card_data[] = new int[table.get_hand_card_count_max()];
			card_data[0] = table.GRR._cards_data[seat_index][table.GRR._card_count[seat_index] - 1];
			table._handler_out_card_operate.reset_status(seat_index, card_data, 1, 1);
			table._handler = table._handler_out_card_operate;
			table._handler.exe(table);
		} else {
			table._handler_out_card_operate.reset_status(seat_index, null, 0, 0);
			table._handler = table._handler_out_card_operate;
			table._handler.exe(table);
		}

	}

	public static void AI_Add_times(DDZTable table, int seat_index) {
		table._handler_add_times.handler_call_banker(table, seat_index, 0);
	}

	// 叫分判定
	/*
	 * 封装好的获取各类牌型组合结构函数
	 * 
	 * CardGroupType cgType：牌型 int MaxCard：决定大小的牌值 int Count：牌数
	 * 
	 * 返回值：CardGroupData
	 */
	CardGroupData get_GroupData(int cgType, int MaxCard, int Count) {
		CardGroupData uctCardGroupData = new CardGroupData();

		uctCardGroupData.cgType = cgType;
		uctCardGroupData.nCount = Count;
		uctCardGroupData.nMaxCard = MaxCard;

		// 不出牌型
		if (cgType == GameConstants.DDZ_CT_PASS)
			uctCardGroupData.nValue = 0;
		// 单牌类型
		else if (cgType == GameConstants.DDZ_CT_SINGLE)
			uctCardGroupData.nValue = MaxCard - 10;
		// 对牌类型
		else if (cgType == GameConstants.DDZ_CT_DOUBLE)
			uctCardGroupData.nValue = MaxCard - 10;
		// 三条类型
		else if (cgType == GameConstants.DDZ_CT_THREE)
			uctCardGroupData.nValue = MaxCard - 10;
		// 单连类型
		else if (cgType == GameConstants.DDZ_CT_SINGLE_LINE)
			uctCardGroupData.nValue = MaxCard - 10 + 1;
		// 对连类型
		else if (cgType == GameConstants.DDZ_CT_DOUBLE_LINE)
			uctCardGroupData.nValue = MaxCard - 10 + 1;
		// 三连类型
		else if (cgType == GameConstants.DDZ_CT_THREE_LINE)
			uctCardGroupData.nValue = (MaxCard - 3 + 1) / 2;
		// 三带一单
		else if (cgType == GameConstants.DDZ_CT_THREE_TAKE_ONE)
			uctCardGroupData.nValue = MaxCard - 10;
		// 三带一对
		else if (cgType == GameConstants.DDZ_CT_THREE_TAKE_TWO)
			uctCardGroupData.nValue = MaxCard - 10;
		// 三带一单连
		else if (cgType == GameConstants.DDZ_CT_THREE_LINE_TAKE_ONE)
			uctCardGroupData.nValue = (MaxCard - 3 + 1) / 2;
		// 三带一对连
		else if (cgType == GameConstants.DDZ_CT_THREE_LINE_TAKE_TWO)
			uctCardGroupData.nValue = (MaxCard - 3 + 1) / 2;
		// 四带两单
		else if (cgType == GameConstants.DDZ_CT_FOUR_TAKE_ONE)
			uctCardGroupData.nValue = (MaxCard - 3) / 2;
		// 四带两对
		else if (cgType == GameConstants.DDZ_CT_FOUR_TAKE_TWO)
			uctCardGroupData.nValue = (MaxCard - 3) / 2;
		// 炸弹类型
		else if (cgType == GameConstants.DDZ_CT_BOMB_CARD)
			uctCardGroupData.nValue = MaxCard - 3 + 7;
		// 王炸类型
		else if (cgType == GameConstants.DDZ_CT_MISSILE_CARD)
			uctCardGroupData.nValue = 20;
		// 错误牌型
		else
			uctCardGroupData.nValue = 0;
		return uctCardGroupData;
	}
	// 拆牌

	// 机器人出牌算法

}
