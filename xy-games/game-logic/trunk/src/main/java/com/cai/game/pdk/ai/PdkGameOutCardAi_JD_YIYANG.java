package com.cai.game.pdk.ai;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.pdk.PDKGameLogicAI_JD_YIYANG;
import com.cai.game.pdk.PDKTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_PDK_JD_YY }, desc = "经典跑得快", msgIds = {
		MsgConstants.RESPONSE_PDK_GAME_START, MsgConstants.RESPONSE_PDK_OUT_CARD })
public class PdkGameOutCardAi_JD_YIYANG extends AbstractAi<PDKTable> {

	public PDKGameLogicAI_JD_YIYANG _logic = new PDKGameLogicAI_JD_YIYANG();

	public PdkGameOutCardAi_JD_YIYANG() {
	}

	@Override
	protected boolean isNeedExe(PDKTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		int current_player = table._current_player;
		return current_player == seat_index;
	}

	@Override
	public void onExe(PDKTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		int current_player = table._current_player;
		_logic.setRuleMap(table.getRuleMap());
		if (current_player == seat_index) {
			int card_data[] = new int[16];
			if (table.has_rule(GameConstants.GAME_RULE_KE_BU_YAO) && table._turn_out_card_count != 0) {
				table._handler_out_card_operate.reset_status(current_player, null, 0, 0, "");
				table._handler_out_card_operate.exe(table);
			} else {
				if (table.GRR._banker_player == current_player && table._out_card_times[current_player] == 0
						&& table._cur_round == 1) {
					int out_card_count = _logic.search_out_card_first_out(table.GRR._cards_data[current_player],
							table.GRR._card_count[current_player], table._turn_out_card_data,
							table._turn_out_card_count, card_data);
					if (out_card_count != 0) {
						table._handler = table._handler_out_card_operate;
						table._logic.sort_card_date_list(card_data, out_card_count);
						table._handler_out_card_operate.reset_status(current_player, card_data, out_card_count, 1, "");
						table._handler_out_card_operate.exe(table);
					}

				} else {

					int out_card_count = _logic.Ai_Out_Card(table.GRR._cards_data[current_player],
							table.GRR._card_count[current_player], table._turn_out_card_data,
							table._turn_out_card_count, card_data,
							table.GRR._card_count[(current_player + 1) % table.getTablePlayerNumber()] == 1);
					if (out_card_count != 0) {
						table._handler = table._handler_out_card_operate;
						table._logic.sort_card_date_list(card_data, out_card_count);
						table._handler_out_card_operate.reset_status(current_player, card_data, out_card_count, 1, "");
						table._handler_out_card_operate.exe(table);
					}
				}
			}
		}
	}

	@Override
	protected AiWrap needDelay(PDKTable table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (MsgConstants.RESPONSE_PDK_GAME_START == rsp.getType()) {
			if (player.isAuto()) {
				return new AiWrap(RandomUtils.nextInt(1000) + 3000);
			}
		} else {
			if (player.isAuto()) {
				return new AiWrap(RandomUtils.nextInt(1000) + 2000);
			}
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(2000);
		}
		// 超时出牌
		return new AiWrap(true, table.getDelay_play_card_time());
	}

	@Override
	public long getMaxTrusteeTime(PDKTable table) {
		int delay = table.getDelay_play_card_time();
		return delay;
	}

}
