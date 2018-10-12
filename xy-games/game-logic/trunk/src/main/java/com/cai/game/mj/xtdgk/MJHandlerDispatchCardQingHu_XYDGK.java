package com.cai.game.mj.xtdgk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.GameConstants_XTDGK;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

/**
 * 请胡
 * 
 * @author Administrator
 *
 */
public class MJHandlerDispatchCardQingHu_XYDGK extends MJHandlerDispatchCard<MJTable_XTDGK> {
	private int _out_card_data;

	@Override
	public void exe(MJTable_XTDGK table) {
		PlayerStatus playerStatus = table._playerStatus[_seat_index];

		// 重置玩家状态
		table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();
		//解除过手胡
		table.clear_jie_pao_hu_fan(_seat_index);

		_out_card_data = GameConstants.INVALID_CARD;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table.operate_player_action(_seat_index, true);

		// 设置为请胡状态
		table._player_result.qing_hu_valid(_seat_index);

		int[] card_index = table.GRR._cards_index[_seat_index];
		int cards_out[] = new int[GameConstants.MAX_COUNT];
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if(card_index[i] == 0){
				continue;
			}
			if (card_index[i] % 2 == 1) {
				if(!table._logic.is_magic_index(i)){
					cards_out[count] = table._logic.switch_to_card_data(i);
					count ++;
				}
			}
		}
		if(count > 0){
			_out_card_data = cards_out[0];
		} else {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if(card_index[i] == 0){
					continue;
				}
				if (card_index[i] % 2 == 0) {
					if(!table._logic.is_magic_index(i)){
						_out_card_data = table._logic.switch_to_card_data(i);
					}
				}
			}
		}
		//记录请胡番数
		//table.qiang_gang_hu[_seat_index] = 2;
		
		// 出牌记录
		table._out_card_count++;
		table._out_card_player = _seat_index;
		table._out_card_data = _out_card_data;
		table.add_out_card_num(_seat_index);
		//删除出牌
		table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], _out_card_data);
		// 用户切换
		int next_player = table.getNextPalyerIndex(_seat_index);
		table._current_player = next_player;

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_XTDGK.CHR_QING_HU },
				1,-1);

		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_COUNT];

		// 刷新自己手牌
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, 0,
				table.GRR.getWeaveItemsForOut(_seat_index, new WeaveItem[GameConstants.MAX_WEAVE]));

		// 出牌
		table.operate_out_card(_seat_index, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);

		boolean bAroseAction = table.estimate_player_out_card_respond(_seat_index, _out_card_data,GameConstants_XTDGK.INVALID_SEAT);
		table.handler_refresh_all_player_data();
		table.exe_add_discard(_seat_index, 1, new int[] { _out_card_data }, false,
				GameConstants.DELAY_SEND_CARD_DELAY);
		// 如果没有需要操作的玩家，派发扑克
		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}

			table.operate_player_action(_seat_index, true);
			
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _out_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index,GameConstants.INVALID_SEAT);// 自摸
			
			playerStatus.add_action(GameConstants.WIK_ZI_MO);
			playerStatus.add_zi_mo(card, _seat_index);// 吃胡的组合
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					handler_operate_card(table, _seat_index, GameConstants.WIK_ZI_MO, _out_card_data);
				}
			}, 2000, TimeUnit.MILLISECONDS);
			
		} else {
			// 等待别人操作这张牌
			int maxPlayer = table.getMaxActionPlayerIndex();
			// 告知客户端最高优先级操作的人--有优先级问题，客户端暂时只处理碰
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					if (playerStatus.has_chi_hu()) {
						table.operate_player_action(i, false);
					} else {
						// 协助解决客户端卡顿问题--客户端只处理碰 所以问题不大
						boolean isNotWait = maxPlayer == i ? true : false;
						table.operate_player_action(i, false, isNotWait);
					}

				}
			}
		}

	}

	/***
	 * 用户操作--当前玩家出牌之后 别人的操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_XTDGK table, int seat_index, int operate_code, int operate_card) {
		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}
		
		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
			// 记录过胡的时候，牌型的番数，变大了，本圈才能接炮
			table.fan_shu_when_abandoned_jie_pao[seat_index] = table.fan_shu_when_jie_pao_hu[seat_index];
			if(table._player_result.is_bao_hu(seat_index)){
				table.need_clear[seat_index] = false;
			}
		}
		
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);
			
			boolean needHu = true;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if(i == _seat_index){
					continue;
				}
				if(table._playerStatus[i]._perfrom_action != GameConstants.WIK_NULL){
					needHu = false;
				}
			}
			if(needHu){
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table._playerStatus[i].clean_action();
					table.change_player_status(i, GameConstants.INVALID_VALUE);
					if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
		                return false;
				}
				
				table.operate_player_action(_seat_index, true);
				
				ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
				chr.set_empty();
				table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], _out_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, _seat_index,GameConstants.INVALID_SEAT);// 自摸
				
				table.exe_add_discard(_seat_index, 1, new int[] { _out_card_data }, false,GameConstants.DELAY_SEND_CARD_DELAY);
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
				table._playerStatus[_seat_index].add_zi_mo(card, _seat_index);// 吃胡的组合
				GameSchedule.put(new Runnable() {
					@Override
					public void run() {
						handler_operate_card(table, _seat_index, GameConstants.WIK_ZI_MO, _out_card_data);
					}
				}, 1500, TimeUnit.MILLISECONDS);
			}
			
		}else if(operate_code == GameConstants.WIK_CHI_HU){
			table.chang_zhang(seat_index,false);
			table.ISHUVaild(seat_index, _out_card_data);
			table.GRR._chi_hu_rights[seat_index].set_valid(true);
			table.process_chi_hu_player_operate(seat_index, operate_card, false);
		}

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
                return false;
        }

		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		// 修改网络导致吃碰错误 9.26 WalkerGeek
		int target_card = _out_card_data;
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_CHI_HU: {
			int jie_pao_count = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false) || table.getISHU(i)) {
					continue;
				}
				jie_pao_count++;
			}
			
			if(jie_pao_count> 0 ){
				int zhuang = GameConstants.INVALID_SEAT;
				if (jie_pao_count == table.getTablePlayerNumber()-1) {
					zhuang = _seat_index;
					table.chang_zhang(zhuang,true);
				} 
				
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if ((i == _seat_index) || (table.GRR._chi_hu_rights[i].is_valid() == false) || table.getISHU(i)) {
						continue;
					}
					table.ISHUVaild(i);
					table.operate_player_get_card(i, 1, new int[] { table.getHuCard(i)}, i);
					table.process_chi_hu_player_operate(i, target_card, false);
					table.process_chi_hu_player_score(i, _seat_index, target_card, false);

					// 记录
					table._player_result.jie_pao_count[i]++;
					table._player_result.dian_pao_count[_seat_index]++;
				}
				// 结束
				int index = _seat_index;
				if(jie_pao_count > 1 && table.getMoreHuIndex(_seat_index)){
					index  = _seat_index;
				} else if(table.getHuIndex() != -1){
					index = table.getHuIndex();
				}
				int next_player = table.getNextPalyerIndex(index);
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}

			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.chang_zhang(_seat_index,false);
			
			table.ISHUVaild(_seat_index,operate_card);
			table.ISHUVaild(_seat_index);
			
			table.GRR._chi_hu_card[_seat_index][0] = operate_card;
			table.process_chi_hu_player_operate(_seat_index, operate_card, false);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			table._player_result.zi_mo_count[_seat_index]++;
			
			// 结束
			int next_player = table.getNextPalyerIndex(_seat_index);
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

			return true;
		}case GameConstants.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_seat_index, _out_card_data);
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_seat_index, _out_card_data);
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.remove_discard_after_operate(_seat_index, _out_card_data);
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}
			
			table.remove_discard_after_operate(_seat_index, _out_card_data);
			table.exe_chi_peng(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_OUT_CARD);
			return true;
		}
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_seat_index, _out_card_data);
			table.exe_gang(target_player, _seat_index, target_card, target_action,
					GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		}

		return true;
	}

}
