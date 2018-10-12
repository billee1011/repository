package com.cai.game.mj.universal;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MahjongUtil_Universal {
	/**
	 * 清空所有玩家的操作，并重置玩家的胡牌状态为有效，同时情况过圈的牌值数据
	 * 
	 * @param table
	 */
	public static void cleanPlayerAction(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();

			table.change_player_status(i, UniversalConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._playerStatus[handler.currentSeatIndex].chi_hu_round_valid();
		table._playerStatus[handler.currentSeatIndex].clear_cards_abandoned_hu();
		table._playerStatus[handler.currentSeatIndex].clear_cards_abandoned_peng();
	}

	/**
	 * 清空所有玩家的操作
	 * 
	 * @param table
	 */
	public static void cleanHandlerStateAction(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
	}

	/**
	 * 添加吃碰组合
	 * 
	 * @param table
	 * @param handler
	 */
	public static void addChiPengWeave(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		int seatIndex = handler.currentSeatIndex;
		int wIndex = table.GRR._weave_count[seatIndex]++;
		table.GRR._weave_items[seatIndex][wIndex].public_card = 1;
		table.GRR._weave_items[seatIndex][wIndex].center_card = handler.cardDataHandled;
		table.GRR._weave_items[seatIndex][wIndex].weave_kind = handler.currentAction;
		table.GRR._weave_items[seatIndex][wIndex].provide_player = handler.providerSeatIndex;
	}

	/**
	 * 显示吃碰效果，并从废牌堆里删除别人打出来的那张牌
	 * 
	 * @param table
	 * @param handler
	 */
	public static void chiPengEffectRemoveDiscard(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		table._current_player = handler.currentSeatIndex;

		table.operate_effect_action(handler.currentSeatIndex, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { handler.currentAction },
				1, UniversalConstants.INVALID_SEAT);

		table.operate_remove_discard(handler.providerSeatIndex, table.GRR._discard_count[handler.providerSeatIndex]);
	}

	/**
	 * 刷新玩家手牌，无特殊牌值
	 * 
	 * @param table
	 * @param seatIndex
	 */
	public static void refreshPlayerCardsNoMagic(AbstractMahjongTable_Universal table, int seatIndex) {
		int cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seatIndex], cards);
		table.operate_player_cards(seatIndex, hand_card_count, cards, table.GRR._weave_count[seatIndex], table.GRR._weave_items[seatIndex]);
	}

	/**
	 * 弹出操作条或切换到出牌状态
	 * 
	 * @param table
	 * @param seatIndex
	 */
	public static void operateOrOutCard(AbstractMahjongTable_Universal table, int seatIndex) {
		if (table._playerStatus[seatIndex].has_action()) {
			table.change_player_status(seatIndex, UniversalConstants.Player_Status_OPR_CARD);
			table.operate_player_action(seatIndex, false);
		} else {
			table.change_player_status(seatIndex, UniversalConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	/**
	 * 执行杠动作之前，进行基本的状态校验
	 * 
	 * @param table
	 * @param seatIndex
	 * @param operateCode
	 * @param handler
	 * @return
	 */
	public static boolean actionCheckBeforeOperate(AbstractMahjongTable_Universal table, int seatIndex, int operateCode,
			AbstractHandler_Universal<?> handler) {
		PlayerStatus playerStatus = table._playerStatus[seatIndex];

		if ((operateCode != UniversalConstants.WIK_NULL) && (playerStatus.has_action_by_code(operateCode) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seatIndex != handler.currentSeatIndex) {
			table.log_error("操作人不是当前玩家");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(handler.currentSeatIndex, "玩家已操作");
			return true;
		}

		return true;
	}

	/**
	 * 有操作时，玩家点了‘过’
	 * 
	 * @param table
	 * @param seatIndex
	 */
	public static void recordNullAction(AbstractMahjongTable_Universal table, int seatIndex) {
		table.record_effect_action(seatIndex, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { UniversalConstants.WIK_NULL }, 1);

		table._playerStatus[seatIndex].clean_action();
		table._playerStatus[seatIndex].clean_status();

		table.change_player_status(seatIndex, UniversalConstants.Player_Status_OUT_CARD);
		table.operate_player_status();
	}

	/**
	 * 客户端发送了出牌消息之后，服务端进行相应的验证，并切换到出牌的处理器
	 * 
	 * @param table
	 * @param seatIndex
	 * @param card
	 * @param handler
	 * @return
	 */
	public static boolean handlePlayerOutCard(AbstractMahjongTable_Universal table, int seatIndex, int card, AbstractHandler_Universal<?> handler) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌时，获取实际牌数据出错");
			return false;
		}

		if (seatIndex != handler.currentSeatIndex) {
			table.log_error("需要出牌的人不是当前玩家");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[handler.currentSeatIndex], card) == false) {
			table.log_error("出牌时，删除玩家的手牌出错");
			return false;
		}

		if (handler instanceof DispatchCardHandler_Univeral && handler.dispatchCardType == UniversalConstants.DISPATCH_CARD_TYPE_GANG) {
			table.exe_out_card(handler.currentSeatIndex, card, UniversalConstants.DISPATCH_CARD_TYPE_GANG);
		} else {
			table.exe_out_card(handler.currentSeatIndex, card, UniversalConstants.DISPATCH_CARD_TYPE_NORMAL);
		}

		return true;
	}

	/**
	 * 断线重连的时候，初始化一些基本数据
	 * 
	 * @param table
	 * @param roomResponse
	 * @param tableResponse
	 */
	public static void initialReconnectData(AbstractMahjongTable_Universal table, RoomResponse.Builder roomResponse,
			TableResponse.Builder tableResponse) {
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setIsGoldRoom(table.is_sys());

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		tableResponse.setSendCardData(0);
	}

	/**
	 * 断线重连时，显示听牌数据
	 * 
	 * @param table
	 * @param seatIndex
	 */
	public static void displayTingCards(AbstractMahjongTable_Universal table, int seatIndex) {
		int ting_cards[] = table._playerStatus[seatIndex]._hu_cards;
		int ting_count = table._playerStatus[seatIndex]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seatIndex, ting_count, ting_cards);
		}
	}

	/**
	 * 断线重连时，显示整个牌桌上的牌数据
	 * 
	 * @param table
	 * @param roomResponse
	 * @param tableResponse
	 * @param seatIndex
	 * @param handler
	 */
	public static void displayTableCards(AbstractMahjongTable_Universal table, RoomResponse.Builder roomResponse, TableResponse.Builder tableResponse,
			int seatIndex, AbstractHandler_Universal<?> handler) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < UniversalConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		int hand_cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seatIndex], hand_cards);

		if (handler instanceof DispatchCardHandler_Univeral && seatIndex == handler.currentSeatIndex) {
			table._logic.remove_card_by_data(hand_cards, handler.cardDataHandled);
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		if (handler instanceof DispatchCardHandler_Univeral) {
			table.operate_player_get_card(handler.currentSeatIndex, 1, new int[] { handler.cardDataHandled }, seatIndex);
		}
	}

	/**
	 * 处理流局
	 * 
	 * @param table
	 */
	public static void liuJu(AbstractMahjongTable_Universal table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.GRR._chi_hu_card[i][0] = UniversalConstants.INVALID_VALUE;
		}

		// 最后摸牌的是下一局的庄家
		table._cur_banker = table._last_dispatch_player;

		// 流局
		GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, UniversalConstants.Game_End_DRAW),
				UniversalConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	}

	/**
	 * 发牌之后，分析有没有自摸胡牌，包括自摸和杠上花
	 * 
	 * @param table
	 * @param handler
	 */
	public static void analyseZiMo(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		int currentSeatIndex = handler.currentSeatIndex;
		int cardDataHandled = handler.cardDataHandled;

		ChiHuRight chr = table.GRR._chi_hu_rights[currentSeatIndex];
		chr.set_empty();

		int hu_card_type = UniversalConstants.U_HU_CARD_TYPE_ZI_MO;
		if (handler.dispatchCardType == UniversalConstants.DISPATCH_CARD_TYPE_GANG) {
			hu_card_type = UniversalConstants.U_HU_CARD_TYPE_GANG_SHANG_HUA;
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[currentSeatIndex], table.GRR._weave_items[currentSeatIndex],
				table.GRR._weave_count[currentSeatIndex], cardDataHandled, chr, hu_card_type, currentSeatIndex);

		if (UniversalConstants.WIK_NULL != action) {
			table._playerStatus[currentSeatIndex].add_action(UniversalConstants.WIK_ZI_MO);
			table._playerStatus[currentSeatIndex].add_zi_mo(cardDataHandled, currentSeatIndex);
		} else {
			chr.set_empty();
		}
	}

	/**
	 * 通用的分析手牌是否有杠，包括暗杠、碰杠
	 * 
	 * @param table
	 * @param handler
	 */
	public static void anaylseGangCard(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		handler.gangCardResult.cbCardCount = 0;
		int currentSeatIndex = handler.currentSeatIndex;

		int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[currentSeatIndex], table.GRR._weave_items[currentSeatIndex],
				table.GRR._weave_count[currentSeatIndex], handler.gangCardResult, true);

		if (0 != cbActionMask) {
			table._playerStatus[currentSeatIndex].add_action(UniversalConstants.WIK_GANG);
			for (int i = 0; i < handler.gangCardResult.cbCardCount; i++) {
				table._playerStatus[currentSeatIndex].add_gang(handler.gangCardResult.cbCardData[i], currentSeatIndex,
						handler.gangCardResult.isPublic[i]);
			}
		}
	}

	/**
	 * 发牌处理器里，正常从牌堆抓一张牌，并重置当前玩家的玩家状态，包括是否有自摸动作这些
	 * 
	 * @param table
	 * @param handler
	 */
	@SuppressWarnings("static-access")
	public static void dispatchCard(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		table._send_card_count++;
		handler.cardDataHandled = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE) {
			handler.cardDataHandled = 0x12;
		}

		table._send_card_data = handler.cardDataHandled;
		table._current_player = handler.currentSeatIndex;
		table._provide_player = handler.currentSeatIndex;
		table._last_dispatch_player = handler.currentSeatIndex;
		table._provide_card = handler.cardDataHandled;

		table._playerStatus[handler.currentSeatIndex].reset();
	}

	/**
	 * 发牌之后，如果有自摸，处理自摸
	 * 
	 * @param table
	 * @param seatIndex
	 * @param operateCard
	 */
	public static void processZiMo(AbstractMahjongTable_Universal table, int seatIndex, int operateCard) {
		table.GRR._chi_hu_rights[seatIndex].set_valid(true);

		table._cur_banker = seatIndex;

		table.GRR._chi_hu_card[seatIndex][0] = operateCard;
		table._player_result.zi_mo_count[seatIndex]++;

		table.set_niao_card(seatIndex);

		table.process_chi_hu_player_operate(seatIndex, operateCard, true);
		table.process_chi_hu_player_score(seatIndex, seatIndex, operateCard, true);

		GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seatIndex, UniversalConstants.Game_End_NORMAL),
				UniversalConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	}

	/**
	 * 不通炮玩法时，杠牌或出牌之后，如果有接炮，处理接炮
	 * 
	 * @param table
	 * @param targetPlayer
	 * @param operateCard
	 * @param currentSeat
	 */
	public static void processJiePaoNoTp(AbstractMahjongTable_Universal table, int targetPlayer, int operateCard, int currentSeat) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == targetPlayer) {
				table.GRR._chi_hu_rights[i].set_valid(true);
			} else {
				table.GRR._chi_hu_rights[i].set_valid(false);
			}
		}

		table._cur_banker = targetPlayer;

		table.GRR._chi_hu_card[targetPlayer][0] = operateCard;
		table._player_result.jie_pao_count[targetPlayer]++;
		table._player_result.dian_pao_count[currentSeat]++;

		table.set_niao_card(targetPlayer);

		table.process_chi_hu_player_operate(targetPlayer, operateCard, false);
		table.process_chi_hu_player_score(targetPlayer, currentSeat, operateCard, false);

		GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, UniversalConstants.Game_End_NORMAL),
				UniversalConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
	}

	/**
	 * 开杠时，获取到杠牌的落地牌索引值，并相应的处理杠牌玩家的落地组合数目
	 * 
	 * @param table
	 * @param handler
	 * @return
	 */
	public static int processGangIndex(AbstractMahjongTable_Universal table, AbstractHandler_Universal<?> handler) {
		int cbWeaveIndex = -1;

		if (UniversalConstants.GANG_TYPE_AN_GANG == handler.gangType) {
			cbWeaveIndex = table.GRR._weave_count[handler.currentSeatIndex];
			table.GRR._weave_count[handler.currentSeatIndex]++;
		} else if (UniversalConstants.GANG_TYPE_JIE_GANG == handler.gangType) {
			cbWeaveIndex = table.GRR._weave_count[handler.currentSeatIndex];
			table.GRR._weave_count[handler.currentSeatIndex]++;

			table.operate_remove_discard(handler.providerSeatIndex, table.GRR._discard_count[handler.providerSeatIndex]);
		} else if (UniversalConstants.GANG_TYPE_ADD_GANG == handler.gangType) {
			for (int i = 0; i < table.GRR._weave_count[handler.currentSeatIndex]; i++) {
				int cbWeaveKind = table.GRR._weave_items[handler.currentSeatIndex][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[handler.currentSeatIndex][i].center_card;
				if ((cbCenterCard == handler.cardDataHandled) && (cbWeaveKind == UniversalConstants.WIK_PENG)) {
					cbWeaveIndex = i;
					break;
				}
			}
		}

		return cbWeaveIndex;
	}

	/**
	 * 获取当前杠牌玩家或出牌玩家的听牌数据并显示
	 * 
	 * @param table
	 * @param seatIndex
	 */
	public static void getAndDisplayTingCards(AbstractMahjongTable_Universal table, int seatIndex) {
		table._playerStatus[seatIndex]._hu_card_count = table.get_ting_card(table._playerStatus[seatIndex]._hu_cards,
				table.GRR._cards_index[seatIndex], table.GRR._weave_items[seatIndex], table.GRR._weave_count[seatIndex], seatIndex);

		int ting_cards[] = table._playerStatus[seatIndex]._hu_cards;
		int ting_count = table._playerStatus[seatIndex]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seatIndex, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(seatIndex, 1, ting_cards);
		}
	}

	/**
	 * 执行杠动作的时候，处理玩家的落地牌组合
	 * 
	 * @param table
	 * @param cardIndex
	 * @param weaveIndex
	 * @param handler
	 */
	public static void processGangWeave(AbstractMahjongTable_Universal table, int cardIndex, int weaveIndex, AbstractHandler_Universal<?> handler) {
		table.GRR._cards_index[handler.currentSeatIndex][cardIndex] = 0;

		table.GRR._weave_items[handler.currentSeatIndex][weaveIndex].public_card = (handler.gangType == UniversalConstants.GANG_TYPE_AN_GANG) ? 0 : 1;
		table.GRR._weave_items[handler.currentSeatIndex][weaveIndex].center_card = handler.cardDataHandled;
		table.GRR._weave_items[handler.currentSeatIndex][weaveIndex].weave_kind = handler.currentAction;
		table.GRR._weave_items[handler.currentSeatIndex][weaveIndex].type = handler.gangType;

		// 回头杠时，提供者不更新，暗杠明杠才更新
		if (UniversalConstants.GANG_TYPE_ADD_GANG != handler.gangType) {
			table.GRR._weave_items[handler.currentSeatIndex][weaveIndex].provide_player = handler.providerSeatIndex;
		} else {
			handler.providerSeatIndex = table.GRR._weave_items[handler.currentSeatIndex][weaveIndex].provide_player;
		}
	}

	/**
	 * 处理杠分
	 * 
	 * @param table
	 * @param basicScore
	 * @param handler
	 */
	public static void processGangScore(AbstractMahjongTable_Universal table, int basicScore, AbstractHandler_Universal<?> handler) {
		int seatIndex = handler.currentSeatIndex;
		int cbGangIndex = table.GRR._gang_score[seatIndex].gang_count++;

		if (UniversalConstants.GANG_TYPE_AN_GANG == handler.gangType) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == seatIndex)
					continue;

				table.GRR._gang_score[seatIndex].scores[cbGangIndex][i] -= basicScore;
				table.GRR._gang_score[seatIndex].scores[cbGangIndex][seatIndex] += basicScore;
			}

			table._player_result.an_gang_count[seatIndex]++;
		} else if (UniversalConstants.GANG_TYPE_JIE_GANG == handler.gangType) {
			table.GRR._gang_score[seatIndex].scores[cbGangIndex][seatIndex] += basicScore;
			table.GRR._gang_score[seatIndex].scores[cbGangIndex][handler.providerSeatIndex] -= basicScore;

			table._player_result.ming_gang_count[seatIndex]++;
		} else if (UniversalConstants.GANG_TYPE_ADD_GANG == handler.gangType) {
			table.GRR._gang_score[seatIndex].scores[cbGangIndex][seatIndex] += basicScore;
			table.GRR._gang_score[seatIndex].scores[cbGangIndex][handler.providerSeatIndex] -= basicScore;

			table._player_result.ming_gang_count[seatIndex]++;
		}
	}

	/**
	 * 杠牌或出牌的时候，获取不通炮玩法时，牌桌上最高优先级的玩家
	 * 
	 * @param table
	 * @param operateSeat
	 * @param operateCode
	 * @param currentSeat
	 * @return
	 */
	public static int getHpSeatNoTp(AbstractMahjongTable_Universal table, int operateSeat, int operateCode, int currentSeat) {
		int target_player = operateSeat;
		@SuppressWarnings("unused")
		int target_action = operateCode;
		int target_p = 0;

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (currentSeat + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (currentSeat + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		return target_player;
	}
}
