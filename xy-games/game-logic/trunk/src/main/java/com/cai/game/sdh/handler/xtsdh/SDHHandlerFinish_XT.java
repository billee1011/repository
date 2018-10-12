package com.cai.game.sdh.handler.xtsdh;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.game.sdh.handler.SDHHandler;

public class SDHHandlerFinish_XT<T extends SDHTable_XT> extends SDHHandler<T> {

	@Override
	public void exe(SDHTable_XT table) {
		int xiaoGuang = 30, totalScore = table.allScore;
		if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_XIAOGUANG_25)) { // 25分小光玩法
			xiaoGuang = 25;
		}
		boolean giveUp = false; 
		if (table._end_reason == SDHConstants_XT.Player_Status_GIVE_UP) { // 认输
			giveUp = true;
			if (table.hasOutCard) { // 出牌后认输算大倒
				totalScore = table._di_fen + 70;
			} else { // 还没出牌就认输 算垮庄*分数的倍率
				if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_SHUANGJINDANCHU)) {
					if (table.rate == 3) {
						totalScore = table._di_fen + 70;
					} else if (table.rate == 2) {
						totalScore = table._di_fen + 40;
					} else if (table.rate == 1) {
						if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_YI_DANG_REN_SHU)) {
							totalScore = table._di_fen + 40;
						} else {
							totalScore = table._di_fen;
						}
					}
				} else {
					if (table.rate == 3) {
						totalScore = table._di_fen + 70;
					} else if (table.rate == 2) {
						totalScore = table._di_fen + 40;
					} else if (table.rate == 1) {
						totalScore = table._di_fen;
					}
				}
			}
		}
		// 计算个人得分
		int currentScore = 0;
		int winner = table._banker_select;
		if (table._di_fen <= totalScore) { // 闲家赢
			winner = (winner + 1) % table.getTablePlayerNumber();
			if (table._di_fen <= totalScore - 70) { // 大倒
				currentScore = 3;
				table._end_reason = 3;
			} else if (table._di_fen <= totalScore - 40) { // 小倒
				currentScore = 2;
				table._end_reason = 2;
				if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_YI_DANG_REN_SHU) && table.rate == 1 && !giveUp) {
					currentScore++;
					table._end_reason++;
				}
			} else if (table._di_fen <= totalScore) { // 垮庄
				currentScore = 1;
				table._end_reason = 1;
				if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_YI_DANG_REN_SHU) && table.rate == 1 && !giveUp) {
					currentScore++;
					table._end_reason++;
				}
			}
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
			if (totalScore == SDHConstants_XT.SDH_QING_GUANG) { // 清光
				currentScore = 3;
				table._end_reason = 4;
			} else if (totalScore <= xiaoGuang) { // 小光
				currentScore = 2;
				table._end_reason = 5;
			} else if (totalScore >= xiaoGuang) { // 成庄
				currentScore = 1;
				table._end_reason = 6;
			}
			currentScore = getRealScore(table, currentScore);
			table.winBanker[table._banker_select]++;
			if (table.has_rule(GameConstants.GAME_RULE_SDH_SHUANGJINDANCHU)) {
				currentScore *= 2;
			}
			table.totalScore[table._banker_select] += currentScore * (table.getTablePlayerNumber() - 1);
			table._player_result.game_score[table._banker_select] += currentScore * (table.getTablePlayerNumber() - 1);
			table.currentScore[table._banker_select] = currentScore * (table.getTablePlayerNumber() - 1);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table._banker_select) {
					table.fail[i]++;
					table.totalScore[i] -= currentScore;
					table.currentScore[i] = -currentScore;
					table._player_result.game_score[i] -= currentScore;
				}
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.maxWin[i] < table.currentScore[i]) {
				table.maxWin[i] = table.currentScore[i]; // 设置最大赢点
			}
			table.refresh_player_score(i, 0); // 刷新分数
		}
		table.handler_game_finish(winner, GameConstants.Game_End_NORMAL);
		table._banker_select = winner;
	}

	private int getRealScore(SDHTable_XT table, int currentScore) {
		return currentScore * table.rate;
	}

}
