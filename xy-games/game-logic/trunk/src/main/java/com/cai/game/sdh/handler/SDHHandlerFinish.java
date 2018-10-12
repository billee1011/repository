package com.cai.game.sdh.handler;

import com.cai.common.constant.GameConstants;
import com.cai.game.sdh.SDHConstants;
import com.cai.game.sdh.SDHTable;

public class SDHHandlerFinish<T extends SDHTable> extends SDHHandler<T> {

	@Override
	public void exe(SDHTable table) {
		boolean isGiveUp = false;
		if (table._end_reason == SDHConstants.Player_Status_GIVE_UP) {
			// 无论叫分多少、认输都是小光
			isGiveUp = true;
			table.allScore = table._di_fen + 5;
		}
		// 计算个人得分
		int currentScore = 0;
		int winner = table._banker_select;
		if (table._di_fen <= table.allScore) { // 闲家赢
			winner = (winner + 1) % table.getTablePlayerNumber();
			if (table._di_fen <= table.allScore - SDHConstants.SDH_DA_GUANG) { // 大光
				currentScore = SDHConstants.SDH_DA_GUANG_SCORE;
				table._end_reason = 2;
			} else {
				currentScore = SDHConstants.SDH_XIAO_GUANG_SCORE;
				table._end_reason = 1;
			}
			// if (table._di_fen <= table.allScore -
			// SDHConstants.SDH_XIAO_GUANG) { 没有下庄美术图 暂不处理
			// table._end_reason = 4;
			// }
			currentScore = getRealScore(table, currentScore);
			table.currentScore[table._banker_select] = (1 - table.getTablePlayerNumber()) * currentScore;
			table.totalScore[table._banker_select] -= (table.getTablePlayerNumber() - 1) * currentScore;
			table.fail[table._banker_select]++;
			table._player_result.game_score[table._banker_select] -= currentScore;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table._banker_select) {
					table.winFree[i]++;
					int value = currentScore;
					table.totalScore[i] += value;
					table._player_result.game_score[i] += value;
					table.currentScore[i] = value;
				}
			}
		} else { // 庄家赢
			if (table.allScore == SDHConstants.SDH_QING_GUANG) {
				currentScore = SDHConstants.SDH_QING_GUANG_SCORE;
				table._end_reason = 3;
			} else if (table.allScore < table._di_fen) {
				currentScore = SDHConstants.SDH_XIAO_GUANG_SCORE;
				table._end_reason = 1;
			}
			// if (table.allScore < 30) { 没有下庄美术图 暂不处理
			// table._end_reason = 1;
			// }
			currentScore = getRealScore(table, currentScore);
			table.winBanker[table._banker_select]++;
			if (table.has_rule(GameConstants.GAME_RULE_SDH_SHUANGJINDANCHU)) {
				currentScore *= 2;
			}
			table.totalScore[table._banker_select] += currentScore * 3;
			table._player_result.game_score[table._banker_select] += currentScore * 3;
			table.currentScore[table._banker_select] = currentScore * 3;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table._banker_select) {
					table.fail[i]++;
					table.totalScore[i] -= currentScore;
					table.currentScore[i] = -currentScore;
					table._player_result.game_score[i] -= currentScore;
				}
			}
		}

		if (isGiveUp) {
			// 无论叫分多少、认输都是小光
			table.allScore = 0;
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.refreshScore(i);
		}
		table.handler_game_finish(winner, GameConstants.Game_End_NORMAL);
		table._banker_select = winner;
	}

	private int getRealScore(SDHTable table, int currentScore) {
		if (table.has_rule(GameConstants.GAME_RULE_SDH_SANDANG)) {
			if (table._di_fen >= 55) {
				currentScore *= 1;
			} else if (table._di_fen >= 35) {
				currentScore *= 2;
			} else {
				currentScore *= 4;
			}
		} else if (table.has_rule(GameConstants.GAME_RULE_SDH_LIANGDANG)) {
			if (table._di_fen >= 55) {
				currentScore *= 1;
			} else if (table._di_fen >= 5) {
				currentScore *= 2;
			}
		}
		return currentScore;
	}

}
