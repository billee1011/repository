package com.cai.game.hongershi.hy;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandler;

/**
 * 起手有对应的操作-- 偷、暗碰、暗杠
 * 
 * @author admin
 *
 */
public class Handler_Bao_Ting extends HHHandler<HongErShiTable_HY> {

	private boolean _banker_gang;

	public void reset(boolean banker_gang) {
		_banker_gang = banker_gang;
	}

	@Override
	public void exe(HongErShiTable_HY table) {

		table._game_status = GameConstants.GS_MJ_PIAO;

		// 等待别人操作这张牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(i, false);
			}
		}

		return;
	}

	@Override
	public boolean handler_operate_card(HongErShiTable_HY table, int seat_index, int operate_code, int operate_card, int luoCode) {
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

		table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 5,
				GameConstants.INVALID_SEAT);

		if (operate_code == HongErShiConstants.WIK_BAO_TING) {

			table.player_bao_ting[seat_index] = true;
			// 设置为报听状态
			table._playerStatus[seat_index].set_card_status(GameConstants.CARD_STATUS_BAO_TING);

		} else if (operate_code == GameConstants.WIK_NULL) {

			table.player_bao_ting[seat_index] = false;

		} else if (operate_code == HongErShiConstants.WIK_ZI_MO) {
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

		// 等待别人操作这张牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action() && !table._playerStatus[i].is_respone()) {
				return true;
			}
		}

		ChiHuRight chr = table.GRR._chi_hu_rights[table._cur_banker];
		int hu = table.analyse_chi_hu_card(table._cur_banker, table._cur_banker, 0, HongErShiConstants.WIK_TIAN_HU, chr, true);
		if (seat_index == table._cur_banker && operate_code == GameConstants.WIK_NULL) {
			hu = GameConstants.WIK_NULL;
		}

		table._game_status = GameConstants.GS_MJ_PLAY;
		table.refresh_game_status(true);
		if (hu != GameConstants.WIK_NULL) {
			if (_banker_gang) {
				chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_HUA);
			}
			table._playerStatus[table._cur_banker].add_action(HongErShiConstants.WIK_ZI_MO);
			// table._playerStatus[table._cur_banker].add_zi_mo(table._send_card_data,
			// table._cur_banker);
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

		return true;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HongErShiTable_HY table, int seat_index, int card) {
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
	public boolean handler_player_be_in_room(HongErShiTable_HY table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}

}
