package com.cai.game.sdh.handler.xtsdh;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.common.util.PBUtil;
import com.cai.game.sdh.handler.SDHHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sdh.SdhRsp.TableResponseSdh;

public class SDHHandlerCallBankerOperate_XT<T extends SDHTable_XT> extends SDHHandler<T> {

	private static Logger logger = Logger.getLogger(SDHHandlerCallBankerOperate_XT.class);

	public boolean success = true;
	public int preSeat = -1;

	@Override
	public void exe(SDHTable_XT table) {
		int seatIndex = table._current_player, score = table.score;
		if (table.callScore[seatIndex] == SDHConstants_XT.SDH_ERROR_NUMBER) { // 已放弃
			logger.error("玩家" + seatIndex + "已放弃叫分, 不能叫分");
			success = false;
			return;
		}
		int playerCount = table.getTablePlayerNumber();
		for (int i = 0; i < playerCount; i++) {
			if (table.callScore[i] > 0 && score >= table.callScore[i]) {
				logger.error("玩家" + seatIndex + "当前叫分不能比其他玩家小");
				//容错：系统下家操作还是为操作玩家再次下发操作面板
				if(table.nextPlayer == seatIndex){
					table.operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_BANKER },
							SDHConstants_XT.SDH_OPERATOR_TIME, seatIndex);
					table.showPlayerOperate(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_BANKER },
							SDHConstants_XT.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);
				}
				success = false;
				return;
			}
		}

		preSeat = seatIndex;
		table.callScore[seatIndex] = score;
		table.sendCallBankerInfo(seatIndex);

		int count = 0, banker = -1, diFen = 0;
		for (int i = 0; i < playerCount; i++) {
			if (table.callScore[i] == SDHConstants_XT.SDH_ERROR_NUMBER) {
				count++;
			} else {
				banker = i;
				diFen = table.callScore[i];
			}
		}
		if (playerCount == count) { // 所有玩家弃庄
			table.exe_finish(SDHConstants_XT.SDH_ALL_PLAYER_GIVE_UP);
			return;
		} else if (playerCount == count + 1 && diFen > 0) { // 其他玩家都弃庄
			callBankerSuccess(table, banker, diFen);
			return;
		} else if (score == SDHConstants_XT.SDH_MIN_DIFEN) {
			table.allScore = 0;
			callBankerSuccess(table, table._current_player, SDHConstants_XT.SDH_MIN_DIFEN);
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
		if (nextSeat == seatIndex) {
			logger.error("叫分出现错误, 又轮到了自己");
			return;
		}
		table._current_player = nextSeat;
		table.nextPlayer = nextSeat;

		table.maxCall = table._di_fen - 5;
		table.minCall = 0;
		if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_MAX_DI) && table._di_fen > 60) { // 60分起叫
			table.maxCall = -5;
		} else if (!table.has_rule(SDHConstants_XT.GAME_RULE_SDH_MAX_DI) && table._di_fen > 80) {
			table.maxCall = -5;
		}
		if ((table.maxCall < 0) && table.ntMainCount[nextSeat] >= 6) {
			table.maxCall = 60;
			table.minCall = 5;
		} else if (table.maxCall >= 55 && table.ntMainCount[nextSeat] >= 6) {
			table.maxCall = 55;
			table.minCall = 5;
		}
		if (table.maxCall < 0) {
			table.maxCall = table._di_fen - 5;
			table.minCall = 0;
		}
		
		if (table.has_rule(SDHConstants_XT.GAME_RULE_SDH_MAX_DI) && table.has_rule(SDHConstants_XT.GAME_RULE_SDH_NEVER_TURN_BACK) && table.callScore[nextSeat] == 60) {
			table.callScore[nextSeat] = SDHConstants_XT.SDH_ERROR_NUMBER;
			do {
				nextSeat = (nextSeat + 1) % playerCount;
				if (nextSeat == seatIndex) {
					break;
				}
			} while (table.callScore[nextSeat] < 0);
			table.nextPlayer = nextSeat;
			table._current_player = nextSeat;
			
			count = 0;
			banker = -1;
			diFen = 0;
			for (int i = 0; i < playerCount; i++) {
				if (table.callScore[i] == SDHConstants_XT.SDH_ERROR_NUMBER || table.callScore[i] == 60) {
					count++;
				} else {
					banker = i;
					diFen = table.callScore[i];
				}
			}
			if (playerCount == count) { // 所有玩家弃庄
				table.exe_finish(SDHConstants_XT.SDH_ALL_PLAYER_GIVE_UP);
				return;
			} else if (playerCount == count + 1 && diFen > 0) { // 其他玩家都弃庄
				callBankerSuccess(table, banker, diFen);
				return;
			} else if (score == SDHConstants_XT.SDH_MIN_DIFEN) {
				table.allScore = 0;
				callBankerSuccess(table, table._current_player, SDHConstants_XT.SDH_MIN_DIFEN);
				return;
			}
		}

		if (preSeat != nextSeat) {
			table.operate_effect_action(nextSeat, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_BANKER },
					SDHConstants_XT.SDH_OPERATOR_TIME, nextSeat);
			table.showPlayerOperate(nextSeat, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_BANKER },
					SDHConstants_XT.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);
		}
	}

	public void callBankerSuccess(SDHTable_XT table, int seatIndex, int score) {
		Arrays.fill(table.callScore, 0);
		table._banker_select = seatIndex;
		table._cur_banker = seatIndex;
		table._di_fen = score;

		if (table._di_fen >= 55) {
			table.rate = 1;
		} else if (table._di_fen >= 35) {
			table.rate = 2;
		} else if (table._di_fen >= 5) {
			table.rate = 3;
		}
		table.stall = table.rate;

		table.joinHandCards(); // 收底牌
		table.currentGameStatus = SDHConstants_XT.GAME_STATUS_DINGZHU; // 这里先切换一下状态
		table._game_status = SDHConstants_XT.GAME_STATUS_DINGZHU;
		table.sendRate();

		table.switch_call_main(seatIndex);
	}

	public boolean isSuccess() {
		return success;
	}

	@Override
	public boolean handler_player_be_in_room(SDHTable_XT table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SDHConstants_XT.RESPONSE_RECONNECT_DATA);

		TableResponseSdh.Builder tableResponseSdh = TableResponseSdh.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		if (table.GRR != null) {
			tableResponseSdh.setCurrentPlayer(table.nextPlayer);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponseSdh.addOutCardsCount(table.GRR._cur_round_count[i]);
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (i == seat_index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}
				tableResponseSdh.addCardsData(i, cards);
				tableResponseSdh.addCallBankerScore(table.callScore[i]);
			}
			tableResponseSdh.setStall(0);
			tableResponseSdh.setRate(0);
			tableResponseSdh.setDifen(0);
			tableResponseSdh.setScore(table.allScore);
			tableResponseSdh.setBankerPlayer(-1);
			tableResponseSdh.setMainColor(-1);
			tableResponseSdh.setGameStatus(table._game_status);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponseSdh));
		table.send_response_to_player(seat_index, roomResponse);
		int endTime = (int) (SDHConstants_XT.SDH_OPERATOR_TIME - (System.currentTimeMillis() - table.beginTime) / 1000);
		if (table.nextPlayer == seat_index && this.preSeat != seat_index && table._game_status == SDHConstants_XT.GAME_STATUS_JIAOZHUANG) {
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_BANKER },
					endTime, seat_index);
		}
		table.showPlayerOperate(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_BANKER }, endTime,
				GameConstants.INVALID_SEAT);

		return true;
	}

}
