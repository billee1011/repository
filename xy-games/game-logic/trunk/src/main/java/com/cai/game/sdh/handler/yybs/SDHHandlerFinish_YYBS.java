package com.cai.game.sdh.handler.yybs;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_YYBS;
import com.cai.game.sdh.handler.SDHHandler;

public class SDHHandlerFinish_YYBS<T extends SDHTable_YYBS> extends SDHHandler<T> {

	@Override
	public void exe(SDHTable_YYBS table) {
		int xiaoGuang = 30, totalScore = table.allScore;
		// 计算个人得分
		int currentScore = 0;
		int winner = table._banker_select;
		int friendIndex = GameConstants.INVALID_SEAT;
		int num = 1;
		if (table.hasFriend) {
			num = table.getTablePlayerNumber() -1;
			friendIndex = (table._banker_select + 2) % table.getTablePlayerNumber();
		}
		int losePlayerNum = num - table.getTablePlayerNumber();
		
		if (80 <= totalScore) { // 闲家赢
			winner = (winner + 1) % table.getTablePlayerNumber();
			if (table.has_rule(SDHConstants_YYBS.GAME_RULE_YYBS_FAN_120_155)) {
				if (totalScore >= 155) { // 大倒
					currentScore = 3;
					table._end_reason = 3;
				} else if (totalScore >= 120 && totalScore < 155) { // 小倒
					currentScore = 2;
					table._end_reason = 2;
				} else if (80 <= totalScore && totalScore < 120) { // 垮庄
					currentScore = 1;
					table._end_reason = 1;
				}
			} else {
				if (totalScore >= 160) { // 大倒
					currentScore = 3;
					table._end_reason = 3;
				} else if (totalScore >= 130 && totalScore < 160) { // 小倒
					currentScore = 2;
					table._end_reason = 2;
				} else if (80 <= totalScore && totalScore < 130) { // 垮庄
					currentScore = 1;
					table._end_reason = 1;
				}
			}

			currentScore = getRealScore(table, currentScore);
			
			int score = losePlayerNum * currentScore;
			
			table.currentScore[table._banker_select] = score;
			table.totalScore[table._banker_select] += score;
			table.fail[table._banker_select]++;
			table._player_result.game_score[table._banker_select] -= currentScore;
			// 找盆友算分
			if (friendIndex != GameConstants.INVALID_SEAT) {
				table.currentScore[friendIndex] = score;
				
				table.totalScore[friendIndex] += score;
				table.fail[friendIndex]++;
				table._player_result.game_score[friendIndex] -= currentScore;
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table._banker_select && !table.isZhuangFriend(i)) {
					table.winFree[i]++;
					int value = currentScore ;
					table.totalScore[i] += value;
					table._player_result.game_score[i] += value;
					table.currentScore[i] = value;
				}
			}
		} else { // 庄家赢
			losePlayerNum =  table.getTablePlayerNumber() - num;
			if (totalScore == SDHConstants_YYBS.SDH_QING_GUANG) { // 清光
				currentScore = 3;
				table._end_reason = 4;
			} else if (totalScore < xiaoGuang) { // 小光
				currentScore = 2;
				table._end_reason = 5;
			} else if (totalScore >= xiaoGuang && totalScore < SDHConstants_YYBS.SDH_GUO_ZHUANG) { // 过庄
				currentScore = 1;
				table._end_reason = 6;
			}
			currentScore = getRealScore(table, currentScore);
			int score = losePlayerNum * currentScore;
			table.winBanker[table._banker_select]++;
			table.totalScore[table._banker_select] += score;
			table._player_result.game_score[table._banker_select] += score;
			table.currentScore[table._banker_select] = score;
			if (friendIndex != GameConstants.INVALID_SEAT) {
				table.winBanker[friendIndex]++;
				table.totalScore[friendIndex] += score;
				table._player_result.game_score[friendIndex] += score;
				table.currentScore[friendIndex] = score;
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table._banker_select && !table.isZhuangFriend(i)) {
					table.fail[i]++;
					int value = currentScore ;
					table.totalScore[i] -= value;
					table.currentScore[i] = -value;
					table._player_result.game_score[i] -= value;
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

	private int getRealScore(SDHTable_YYBS table, int currentScore) {
		return currentScore * table.score;
	}

}
