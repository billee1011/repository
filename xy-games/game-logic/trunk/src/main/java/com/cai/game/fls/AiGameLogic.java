package com.cai.game.fls;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.game.fls.handler.FLSHandlerChiPeng;

public class AiGameLogic {
	public static void ai_out_card(FLSTable table, int seat_index) {
		int status = table._playerStatus[seat_index].get_status();
		if (status != GameConstants.Player_Status_OUT_CARD)
			return;

		int card = get_card(table, seat_index);
		table.handler_player_out_card(seat_index, card);
	}

	public static int get_card(FLSTable table, int seat_index) {
		int result_card = table._send_card_data;
		if (table._handler instanceof FLSHandlerChiPeng) {
			int cards[] = new int[GameConstants.MAX_FLS_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			int index = RandomUtil.getRandomNumber(hand_card_count);
			result_card = cards[index];
		}
		return result_card;
	}

	public static void ai_operate_card(FLSTable table, int seat_index) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		int operate_code = table._logic.get_max_rank_action(playerStatus._action_count, playerStatus._action, GameConstants.WIK_NULL);
		int operate_card = playerStatus.get_weave_card(operate_code);

		if (operate_code == GameConstants.WIK_NULL) {
			table.log_error("获取不到操作优先级，默认最高优先级，执行第一个操作：对应子游戏请尽快解决优先级问题");
			table.handler_operate_card(seat_index, GameConstants.WIK_NULL, 0, -1);
		} else if (playerStatus.has_action_by_code(operate_code) != false) {
			if (playerStatus.has_chi_hu() || playerStatus.has_zi_mo() || playerStatus.has_action_by_code(GameConstants.WIK_XIAO_HU)) {
				table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			} else {
				table.handler_operate_card(seat_index, GameConstants.WIK_NULL, operate_card, -1);
			}
		}
	}
}
