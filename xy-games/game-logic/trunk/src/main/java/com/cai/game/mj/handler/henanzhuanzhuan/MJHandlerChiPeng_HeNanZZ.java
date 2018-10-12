package com.cai.game.mj.handler.henanzhuanzhuan;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerChiPeng;

public class MJHandlerChiPeng_HeNanZZ extends MJHandlerChiPeng<MJTable> {

	// 执行吃碰操作
	@Override
	public void exe(MJTable table) {
		super.exe(table);

		// 回放
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		m_gangCardResult.cbCardCount = 0;
		// 如果牌堆还有牌，判断能不能杠
		if (table.GRR._left_card_count > 0) {
			// 只检测手上的牌能不能杠
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, false);

			if (cbActionMask != 0) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);// 转转就是杠
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			// curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);

		} else {
			// curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);//
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable table, int seat_index, int card) {
		card = table.get_real_card(card);

		// 错误断言
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

		// 出牌
		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	/***
	 * //用户操作 --杆操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
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

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			// table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}

		}
			break;
		}

		return true;
	}
}
