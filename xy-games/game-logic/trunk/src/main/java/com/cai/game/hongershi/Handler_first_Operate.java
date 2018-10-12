package com.cai.game.hongershi;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandler;

/**
 * 起手有对应的操作-- 偷、暗碰、暗杠
 * 
 * @author admin
 *
 */
public class Handler_first_Operate extends HHHandler<HongErShiTable> {

	private int _start_check_tou_seat_index;

	private boolean an_peng_pass[];
	private boolean an_gang_pass[];
	public boolean banker_gang;

	public void reset(HongErShiTable table, int start_check_tou_seat_index) {
		_start_check_tou_seat_index = start_check_tou_seat_index;
		an_peng_pass = new boolean[table.getTablePlayerNumber()];
		an_gang_pass = new boolean[table.getTablePlayerNumber()];
		banker_gang = false;
	}

	@Override
	public void exe(HongErShiTable table) {
		table._game_status = GameConstants.GS_MJ_PIAO;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int p = (table._cur_banker + i + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
			if (table.touCards[p]) {
				table._playerStatus[p].add_action(HongErShiConstants.WIK_TOU);
				table._playerStatus[p].add_tou(HongErShiConstants.MAGIC_CARD_KING, HongErShiConstants.WIK_TOU, 1);

				table._playerStatus[p].set_status(GameConstants.Player_Status_OPR_CARD);//
				table.operate_player_action(p, false);

				return;
			}

			int cardsIndex[] = new int[14];
			table.logic.switch_to_cards_index_value(table.GRR._cards_data[p], 0, table.GRR._card_count[p], cardsIndex);
			for (int w = 0; w < table.GRR._weave_count[p]; w++) {
				if (table.GRR._weave_items[p][w].weave_kind != HongErShiConstants.WIK_AN_PENG) {
					continue;
				}
				if (cardsIndex[table.logic.switch_to_card_index(table.GRR._weave_items[p][w].center_card)] != 0) {
					table._playerStatus[p].add_action(HongErShiConstants.WIK_GANG);
					table._playerStatus[p].add_tou(table.GRR._weave_items[p][w].center_card, HongErShiConstants.WIK_GANG, 0);

					table._playerStatus[p].add_action(GameConstants.WIK_NULL);
					table._playerStatus[p].add_pass(0, p);
					table._playerStatus[p].set_status(GameConstants.Player_Status_OPR_CARD);//
					table.operate_player_action(p, false);

					return;
				}
			}

			// 没有人要偷牌 则开始检查暗杠暗碰
			int[] cardsFour = new int[2]; // 暗杠
			int[] cardsThree = new int[2]; // 暗碰
			int[] countNumber = new int[2]; // 暗杠、暗碰数量
			table.logic.checkLgThree(table.GRR._cards_data[p], table.GRR._card_count[p], cardsFour, cardsThree, countNumber);

			for (int g = 0; g < countNumber[0] && !an_gang_pass[p]; g++) {
				table._playerStatus[p].add_action(HongErShiConstants.WIK_AN_PENG);
				table._playerStatus[p].add_tou(cardsFour[g], HongErShiConstants.WIK_AN_PENG, 0);
				table._playerStatus[p].add_action(HongErShiConstants.WIK_GANG);
				table._playerStatus[p].add_tou(cardsFour[g], HongErShiConstants.WIK_GANG, 0);

				table._playerStatus[p].add_action(GameConstants.WIK_NULL);
				table._playerStatus[p].add_pass(0, p);
				table._playerStatus[p].set_status(GameConstants.Player_Status_OPR_CARD);//
				table.operate_player_action(p, false);

				return;
			}
			for (int peng = 0; peng < countNumber[1] && !an_peng_pass[p]; peng++) {
				table._playerStatus[p].add_action(HongErShiConstants.WIK_AN_PENG);
				table._playerStatus[p].add_tou(cardsThree[peng], HongErShiConstants.WIK_AN_PENG, 0);

				table._playerStatus[p].add_action(GameConstants.WIK_NULL);
				table._playerStatus[p].add_pass(0, p);
				table._playerStatus[p].set_status(GameConstants.Player_Status_OPR_CARD);//
				table.operate_player_action(p, false);

				return;
			}
		}

		/*
		 * for (int i = 0; i < table.getTablePlayerNumber(); i++) { int p =
		 * (table._cur_banker + i + table.getTablePlayerNumber()) %
		 * table.getTablePlayerNumber(); // 没有人要偷牌 则开始检查暗杠暗碰 int[] cardsFour =
		 * new int[2]; // 暗杠 int[] cardsThree = new int[2]; // 暗碰 int[]
		 * countNumber = new int[2]; // 暗杠、暗碰数量
		 * table.logic.checkLgThree(table.GRR._cards_data[p],
		 * table.GRR._card_count[p], cardsFour, cardsThree, countNumber);
		 * 
		 * for (int g = 0; g < countNumber[0] && !an_gang_pass[p]; g++) {
		 * table._playerStatus[p].add_action(HongErShiConstants.WIK_GANG);
		 * table._playerStatus[p].add_tou(cardsFour[g],
		 * HongErShiConstants.WIK_GANG, 0);
		 * 
		 * table._playerStatus[p].add_action(GameConstants.WIK_NULL);
		 * table._playerStatus[p].add_pass(0, p);
		 * table._playerStatus[p].set_status(GameConstants.
		 * Player_Status_OPR_CARD);// table.operate_player_action(p, false);
		 * 
		 * return; } for (int peng = 0; peng < countNumber[1] &&
		 * !an_peng_pass[p]; peng++) {
		 * table._playerStatus[p].add_action(HongErShiConstants.WIK_AN_PENG);
		 * table._playerStatus[p].add_tou(cardsThree[peng],
		 * HongErShiConstants.WIK_AN_PENG, 0);
		 * 
		 * table._playerStatus[p].add_action(GameConstants.WIK_NULL);
		 * table._playerStatus[p].add_pass(0, p);
		 * table._playerStatus[p].set_status(GameConstants.
		 * Player_Status_OPR_CARD);// table.operate_player_action(p, false);
		 * 
		 * return; } }
		 */

		if (table.estimate_player_bao_ting()) {
			table.exe_Handler_bao_ting(banker_gang);
			return;
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[table._cur_banker];
		int hu = table.analyse_chi_hu_card(table._cur_banker, table._cur_banker, 0, HongErShiConstants.WIK_TIAN_HU, chr, true);

		table._game_status = GameConstants.GS_MJ_PLAY;
		table.refresh_game_status(true);
		if (hu != GameConstants.WIK_NULL) {
			if (banker_gang) {
				chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_HUA);
			}
			table._playerStatus[table._cur_banker].add_action(HongErShiConstants.WIK_ZI_MO);
			table._playerStatus[table._cur_banker].add_tou(table._send_card_data, HongErShiConstants.WIK_ZI_MO, table._cur_banker);

			table._playerStatus[table._cur_banker].add_action(GameConstants.WIK_NULL);
			table._playerStatus[table._cur_banker].add_pass(table._send_card_data, table._cur_banker);

			if (table._playerStatus[table._cur_banker].has_action()) {
				table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OPR_CARD);//
				// 操作状态
				table.operate_player_action(table._cur_banker, false);
			}
		} else {
			table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		}

		return;
	}

	@Override
	public boolean handler_operate_card(HongErShiTable table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action_by_code(operate_code) == false) {
			table.log_info("没有这个操作:" + operate_code);
			return false;
		}
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		playerStatus.clean_action();

		if (operate_code == HongErShiConstants.WIK_TOU) {

			table.touCards[seat_index] = false;

			int king_count = table.logic.countKingNumber(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index]);

			int[] send_card = new int[king_count];
			int cardsIndex[] = new int[53];
			table.logic.switch_to_cards_index_real(table.GRR._cards_data[seat_index], 0, table.GRR._card_count[seat_index], cardsIndex);
			for (int i = 0; i < king_count; i++) {

				if (!table.has_king_tou[seat_index]) {
					int cbWeaveIndex = table.GRR._weave_count[seat_index];
					table.GRR._weave_count[seat_index]++;
					table.GRR._weave_items[seat_index][cbWeaveIndex].public_card = 0;
					table.GRR._weave_items[seat_index][cbWeaveIndex].center_card = HongErShiConstants.MAGIC_CARD_KING;
					table.GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = HongErShiConstants.WIK_TOU;
					table.GRR._weave_items[seat_index][cbWeaveIndex].provide_player = seat_index;
					table.GRR._weave_items[seat_index][cbWeaveIndex].weave_card = new int[] { HongErShiConstants.MAGIC_CARD_KING };

					table.has_king_tou[seat_index] = true;
				} else {
					for (int wc = 0; wc < table.GRR._weave_count[seat_index]; wc++) {
						if (table.GRR._weave_items[seat_index][wc].center_card != HongErShiConstants.MAGIC_CARD_KING) {
							continue;
						}

						int[] king_card = new int[HongErShiConstants.KING_MAX_COUNT];
						int king_count1 = 0;
						king_card[king_count1++] = HongErShiConstants.MAGIC_CARD_KING;
						for (int c = 0; c < table.GRR._weave_items[seat_index][wc].weave_card.length; c++) {
							king_card[king_count1++] = HongErShiConstants.MAGIC_CARD_KING;
						}

						table.GRR._weave_items[seat_index][wc].weave_card = Arrays.copyOf(king_card, king_count1);
					}
				}

				table.logic.remove_card_by_index(cardsIndex, HongErShiConstants.MAGIC_CARD_KING);
				table._send_card_count++;
				int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				table.GRR._left_card_count--;

				if (table.DEBUG_CARDS_MODE) {
					send_card_data = 0x2;
				}

				send_card[i] = send_card_data;
			}

			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { HongErShiConstants.WIK_TOU }, 5,
					GameConstants.INVALID_SEAT);

			int cards[] = new int[HongErShiConstants.MAX_COUNT];
			int hand_card_count = table.logic.switch_to_cards_data(cardsIndex, cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
			table.operate_player_mo_card(seat_index, king_count, send_card, hand_card_count, GameConstants.INVALID_SEAT, true);

			for (int i = 0; i < king_count; i++) {
				cardsIndex[table.logic.switch_to_card_index(send_card[i])]++;
			}
			table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);

			if (table.logic.countKingNumber(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index]) > 0) {
				table._playerStatus[seat_index].add_action(HongErShiConstants.WIK_TOU);
				table._playerStatus[seat_index].add_tou(HongErShiConstants.MAGIC_CARD_KING, HongErShiConstants.WIK_TOU, 1);

				GameSchedule.put(new Runnable() {

					@Override
					public void run() {
						handler_operate_card(table, seat_index, HongErShiConstants.WIK_TOU, operate_card, luoCode);
					}
				}, 150 + 800, TimeUnit.MILLISECONDS);
				return true;
			}
		} else if (operate_code == HongErShiConstants.WIK_GANG) {

			boolean is_add_gang = false;
			int add_gang_weave_index = -1;
			for (int w = 0; w < table.GRR._weave_count[seat_index]; w++) {
				if (table.logic.get_card_value(table.GRR._weave_items[seat_index][w].center_card) == table.logic.get_card_value(operate_card)
						&& table.GRR._weave_items[seat_index][w].weave_kind == HongErShiConstants.WIK_AN_PENG) {
					is_add_gang = true;
					add_gang_weave_index = w;
					break;
				}
			}

			if (seat_index == table._cur_banker) {
				banker_gang = true;
			}

			if (is_add_gang) {

				int[] weave_cards = new int[4];
				for (int i = 0; i < 4; i++) {
					weave_cards[i] = ((i & GameConstants.LOGIC_MASK_VALUE) << 4) | table.logic.get_card_value(operate_card);
				}
				table.GRR._weave_items[seat_index][add_gang_weave_index].weave_kind = GameConstants.GANG_TYPE_ADD_GANG;
				table.GRR._weave_items[seat_index][add_gang_weave_index].weave_card = weave_cards;

				// 删除手上的牌
				table.GRR._card_count[seat_index] = table.logic.remove_card_by_card_value(table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index], operate_card, new int[4]);

				int cardsIndex[] = new int[53];
				table.logic.switch_to_cards_index_real(table.GRR._cards_data[seat_index], 0, table.GRR._card_count[seat_index], cardsIndex);

				table._send_card_count++;
				int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				table.GRR._left_card_count--;

				table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { HongErShiConstants.WIK_GANG }, 5,
						GameConstants.INVALID_SEAT);

				int card_count = table.GRR._card_count[seat_index];
				table.operate_player_cards(seat_index, card_count, table.GRR._cards_data[seat_index], table.GRR._weave_count[seat_index],
						table.GRR._weave_items[seat_index]);
				table.operate_player_mo_card(seat_index, 1, new int[] { send_card_data }, card_count, GameConstants.INVALID_SEAT, true);

				cardsIndex[table.logic.switch_to_card_index(send_card_data)]++;
				table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);
			} else {
				int[] weave_cards = new int[4];
				for (int i = 0; i < 4; i++) {
					weave_cards[i] = ((i & GameConstants.LOGIC_MASK_VALUE) << 4) | table.logic.get_card_value(operate_card);
				}

				int cbWeaveIndex = table.GRR._weave_count[seat_index];
				table.GRR._weave_count[seat_index]++;
				table.GRR._weave_items[seat_index][cbWeaveIndex].public_card = 0;
				table.GRR._weave_items[seat_index][cbWeaveIndex].center_card = operate_card;
				table.GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = HongErShiConstants.WIK_AN_GANG;
				table.GRR._weave_items[seat_index][cbWeaveIndex].provide_player = seat_index;
				table.GRR._weave_items[seat_index][cbWeaveIndex].weave_card = weave_cards;

				// 删除手上的牌
				table.GRR._card_count[seat_index] = table.logic.remove_card_by_card_value(table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index], operate_card, new int[4]);

				WeaveItem weaves[] = new WeaveItem[HongErShiConstants.MAX_WEAVE];
				int weave_count = table.GRR._weave_count[seat_index];
				for (int i = 0; i < weave_count; i++) {
					weaves[i] = new WeaveItem();
					weaves[i].weave_kind = table.GRR._weave_items[seat_index][i].weave_kind;
					weaves[i].center_card = table.GRR._weave_items[seat_index][i].center_card;
					weaves[i].public_card = table.GRR._weave_items[seat_index][i].public_card;
					weaves[i].weave_card = table.GRR._weave_items[seat_index][i].weave_card;
					weaves[i].provide_player = table.GRR._weave_items[seat_index][i].provide_player + HongErShiConstants.WEAVE_SHOW_DIRECT;
				}

				table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { HongErShiConstants.WIK_GANG }, 5,
						GameConstants.INVALID_SEAT);

				int hand_card_count = table.GRR._card_count[seat_index];
				table.operate_player_cards(seat_index, hand_card_count, table.GRR._cards_data[seat_index], weave_count, weaves);
				int cardsIndex[] = new int[53];
				table.logic.switch_to_cards_index_real(table.GRR._cards_data[seat_index], 0, table.GRR._card_count[seat_index], cardsIndex);

				int[] send_card = new int[2];
				for (int i = 0; i < 2; i++) {
					table._send_card_count++;
					int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
					table.GRR._left_card_count--;

					send_card[i] = send_card_data;
					cardsIndex[table.logic.switch_to_card_index(send_card_data)]++;
				}
				table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);
				table.operate_player_mo_card(seat_index, 2, send_card, hand_card_count, GameConstants.INVALID_SEAT, true);
			}
			if (table.logic.countKingNumber(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index]) > 0) {
				table._playerStatus[seat_index].add_action(HongErShiConstants.WIK_TOU);
				table._playerStatus[seat_index].add_tou(HongErShiConstants.MAGIC_CARD_KING, HongErShiConstants.WIK_TOU, 1);

				GameSchedule.put(new Runnable() {

					@Override
					public void run() {
						handler_operate_card(table, seat_index, HongErShiConstants.WIK_TOU, operate_card, luoCode);
					}
				}, 600, TimeUnit.MILLISECONDS);
				return true;
			}

		} else if (operate_code == HongErShiConstants.WIK_AN_PENG) {

			// 删除手上的牌
			int[] remove_data = new int[3];
			table.GRR._card_count[seat_index] = table.logic.remove_card_by_card_value(table.GRR._cards_data[seat_index],
					table.GRR._card_count[seat_index], operate_card, remove_data);
			int remove_count = 0;
			for (int r = 0; r < 3; r++) {
				if (remove_data[r] != 0) {
					remove_count++;
				} else {
					break;
				}
			}

			if (remove_count != 3) {
				table.log_error("暗碰错误");
				return false;
			}

			int cbWeaveIndex = table.GRR._weave_count[seat_index];
			table.GRR._weave_count[seat_index]++;
			table.GRR._weave_items[seat_index][cbWeaveIndex].public_card = 0;
			table.GRR._weave_items[seat_index][cbWeaveIndex].center_card = operate_card;
			table.GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = HongErShiConstants.WIK_AN_PENG;
			table.GRR._weave_items[seat_index][cbWeaveIndex].provide_player = seat_index;
			table.GRR._weave_items[seat_index][cbWeaveIndex].weave_card = Arrays.copyOf(remove_data, remove_count);

			WeaveItem weaves[] = new WeaveItem[HongErShiConstants.MAX_WEAVE];
			int weave_count = table.GRR._weave_count[seat_index];
			for (int i = 0; i < weave_count; i++) {
				weaves[i] = new WeaveItem();
				weaves[i].weave_kind = table.GRR._weave_items[seat_index][i].weave_kind;
				weaves[i].center_card = table.GRR._weave_items[seat_index][i].center_card;
				weaves[i].public_card = table.GRR._weave_items[seat_index][i].public_card;
				weaves[i].weave_card = table.GRR._weave_items[seat_index][i].weave_card;
				weaves[i].provide_player = table.GRR._weave_items[seat_index][i].provide_player + HongErShiConstants.WEAVE_SHOW_DIRECT;
			}

			int cardsIndex[] = new int[53];
			table.logic.switch_to_cards_index_real(table.GRR._cards_data[seat_index], 0, table.GRR._card_count[seat_index], cardsIndex);

			table._send_card_count++;
			int send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			if (table.DEBUG_CARDS_MODE) {
				send_card_data = 0x2b;
			}
			table.GRR._left_card_count--;

			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { HongErShiConstants.WIK_AN_PENG }, 5,
					GameConstants.INVALID_SEAT);

			int card_count = table.GRR._card_count[seat_index];
			table.operate_player_cards(seat_index, card_count, table.GRR._cards_data[seat_index], weave_count, weaves);
			table.operate_player_mo_card(seat_index, 1, new int[] { send_card_data }, card_count, GameConstants.INVALID_SEAT, true);

			cardsIndex[table.logic.switch_to_card_index(send_card_data)]++;
			table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);

			if (seat_index == table._cur_banker) {
				banker_gang = true;
			}
			if (table.logic.countKingNumber(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index]) > 0) {
				table._playerStatus[seat_index].add_action(HongErShiConstants.WIK_TOU);
				table._playerStatus[seat_index].add_tou(HongErShiConstants.MAGIC_CARD_KING, HongErShiConstants.WIK_TOU, 1);

				GameSchedule.put(new Runnable() {

					@Override
					public void run() {
						handler_operate_card(table, seat_index, HongErShiConstants.WIK_TOU, operate_card, luoCode);
					}
				}, 600, TimeUnit.MILLISECONDS);
				return true;
			}

			cardsIndex[table.logic.switch_to_card_index(send_card_data)]--;
			table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);
			if (table.estimate_player_mo_card_response(seat_index, send_card_data, operate_card, true)) {
				table.operate_player_action(seat_index, false);
				cardsIndex[table.logic.switch_to_card_index(send_card_data)]++;
				table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);
				return true;
			}
			cardsIndex[table.logic.switch_to_card_index(send_card_data)]++;
			table.GRR._card_count[seat_index] = table.logic.switch_to_cards_data(cardsIndex, table.GRR._cards_data[seat_index]);
		} else if (operate_code == GameConstants.WIK_NULL) {
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();

			an_gang_pass[seat_index] = true;
			an_peng_pass[seat_index] = true;
		} else if (operate_code == HongErShiConstants.WIK_ZI_MO) {
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { HongErShiConstants.WIK_ZI_MO }, 5,
					GameConstants.INVALID_SEAT);
			table.GRR._chi_hu_rights[table._cur_banker].set_valid(true);

			table.GRR._chi_hu_card[table._cur_banker][0] = operate_card;

			table._cur_banker = table._cur_banker;

			table._shang_zhuang_player = table._cur_banker;

			table.process_chi_hu_player_operate(table._cur_banker, operate_card, true);

			if (table._cur_banker == seat_index) {
				table.GRR._chi_hu_rights[table._cur_banker].opr_or(HongErShiConstants.WIK_TIAN_HU);
			}

			table.process_chi_hu_player_score_phz(table._cur_banker, seat_index, operate_card, true);

			table.countChiHuTimes(table._cur_banker, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[table._cur_banker].type_count > 2) {
				delay += table.GRR._chi_hu_rights[table._cur_banker].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			return true;
		}

		exe(table);
		return true;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HongErShiTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table.logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != table._cur_banker) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		if (table._playerStatus[table._cur_banker].get_status() != GameConstants.Player_Status_OUT_CARD) {
			table.log_error("状态不对不能出牌");
			return false;
		}

		// 删除扑克
		int card_count = table.logic.remove_cards_by_cards(table.GRR._cards_data[table._cur_banker], table.GRR._card_count[table._cur_banker],
				new int[] { card }, 1);
		if (card_count == -1) {
			table.log_error("出牌删除出错");
			return false;
		} else {
			table.GRR._card_count[table._cur_banker] = card_count;
		}

		// 出牌
		table.exe_out_card(table._cur_banker, card, HongErShiConstants.WIK_DI_HU);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HongErShiTable table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}

}
