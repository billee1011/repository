package com.cai.game.sdh.handler;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.game.sdh.SDHConstants;
import com.cai.game.sdh.SDHTable;

public class SDHHandlerCallBankerOperate<T extends SDHTable> extends SDHHandler<T> {

	private static Logger logger = Logger.getLogger(SDHHandlerCallBankerOperate.class);

	private boolean success = true;

	@Override
	public void exe(SDHTable table) {
		int seatIndex = table._current_player, score = table.score;
		if (table.callScore[seatIndex] == SDHConstants.SDH_ERROR_NUMBER) { // 已放弃
			logger.error("玩家" + seatIndex + "已放弃叫分, 不能叫分");
			success = false;
			return;
		}
		int playerCount = table.getTablePlayerNumber();
		for (int i = 0; i < playerCount; i++) {
			if (table.callScore[i] > 0 && score >= table.callScore[i]) {
				logger.error("玩家" + seatIndex + "当前叫分不能比其他玩家小");
				success = false;
				return;
			}
		}
		
		// 收起该玩家的效果通知
		table.operate_effect_action(table._current_player, GameConstants.Effect_Action_Other, 1, new long[] { SDHConstants.Player_Status_CALL_BANKER }, 1,
				table._current_player);

		// 广播告知其他玩家当前玩家的叫分情况
		table.callScore[seatIndex] = score;
		table.sendCallBankerInfo(seatIndex);
		table.sendInfoToPlayerAndSelf(table.get_players()[seatIndex], SDHConstants.SDH_ERROR_NUMBER);

		int count = 0, banker = -1, diFen = 0;
		for (int i = 0; i < playerCount; i++) {
			if (table.callScore[i] == SDHConstants.SDH_ERROR_NUMBER) {
				count++;
			} else {
				banker = i;
				diFen = table.callScore[i];
			}
		}
		if (playerCount == count) { // 所有玩家弃庄
			callBankerSuccess(table, table._banker_select == -1 ? 0 : table._banker_select, SDHConstants.SDH_MAX_FEN);
			return;
		} else if (playerCount == count + 1 && diFen > 0) { // 其他玩家都弃庄
			callBankerSuccess(table, banker, diFen);
			return;
		} else if (score == SDHConstants.SDH_MIN_DIFEN) {
			table.allScore = 0;
			callBankerSuccess(table, table._current_player, SDHConstants.SDH_MIN_DIFEN);
			return;
		}

		if (score > 0) {
			table._di_fen = score;
		}
		int nextSeat = seatIndex; // 通知下一个玩家叫分
		do {
			nextSeat = (nextSeat + 1) % playerCount;
			if (nextSeat == seatIndex) {
				logger.error("叫分出现错误, 又轮到了自己");
				return;
			}
		} while (table.callScore[nextSeat] < 0);
		table._current_player = nextSeat;
		table.operate_effect_action(nextSeat, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants.Player_Status_CALL_BANKER }, SDHConstants.SDH_OPERATOR_TIME, nextSeat);
		table.showPlayerOperate(nextSeat, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants.Player_Status_CALL_BANKER }, SDHConstants.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);
	}

	public void callBankerSuccess(SDHTable table, int seatIndex, int score) {
		Arrays.fill(table.callScore, 0);
//		table.callScore[seatIndex] = SDHConstants.SDH_BANKER_DATA;
		table._banker_select = seatIndex;
		table._cur_banker = seatIndex;
		table._di_fen = score;
		
		if (table._di_fen >= 55) {
			table.rate = 1;
		} else if (table._di_fen >= 35) {
			table.rate = 2;
		} else if (table._di_fen >= 5) {
			table.rate = table.stall == 3 ? 4 : 2;
		}
		// 收底牌
		table.sendRate();
		table.joinHandCards();
		table.sendInfoToPlayerAndSelf(table.get_players()[seatIndex], SDHConstants.Player_Status_BANKER_SELECT);
		table.operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants.Player_Status_CALL_MAIN }, 1, seatIndex);
		table.showPlayerOperate(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants.Player_Status_CALL_MAIN }, SDHConstants.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);
		table.currentGameStatus = SDHConstants.GAME_STATUS_DINGZHU;
		table._game_status = SDHConstants.GAME_STATUS_DINGZHU;
		table._current_player = seatIndex;
	}

	public boolean isSuccess() {
		return success;
	}

}
