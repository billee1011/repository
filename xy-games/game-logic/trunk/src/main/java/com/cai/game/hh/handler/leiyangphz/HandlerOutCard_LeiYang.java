package com.cai.game.hh.handler.leiyangphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_LeiYang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerOutCard_LeiYang extends HHHandlerOutCardOperate<Table_LeiYang> {
	
	boolean bAroseAction;
	int bHupai;

    @Override
    public void exe(Table_LeiYang table) {
        _out_card_data = table.get_real_card(_out_card_data); // 获取真实的牌数据

        if(_out_card_player == table._cur_banker)
        	table.banker_out_card_count++;
        if(!(_out_card_player == table._cur_banker && table.banker_out_card_count == 1))
        	table.out_status[_out_card_player] = 1;
        
        if (table.GRR._cards_index[_out_card_player][table._logic.switch_to_card_index(_out_card_data)] >= 3) {
            table.log_info(_out_card_player + "出牌出错" + _out_card_data);
            return;
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();
        }

        PlayerStatus playerStatus = table._playerStatus[_out_card_player];
        playerStatus.reset();

        table._out_card_count++;
        table._out_card_player = _out_card_player;
        table._out_card_data = _out_card_data;
        table._last_card = _out_card_data;

        if (table.is_card_has_wei(_out_card_data)) { // 玩家射跑
            table.has_shoot[_out_card_player] = true;
        }

        table._cannot_chi[_out_card_player][table._cannot_chi_count[_out_card_player]++] = table._out_card_data;

        int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
        table._current_player = next_player;

        int cards[] = new int[GameConstants.MAX_HH_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);
        table.operate_player_cards(_out_card_player, hand_card_count, cards, table.GRR._weave_count[_out_card_player],
                table.GRR._weave_items[_out_card_player]);

        table.operate_out_card(_out_card_player, 1, new int[] { table._out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
                GameConstants.INVALID_SEAT);

        table._provide_player = _out_card_player;
        table._provide_card = table._out_card_data;

        int ti_sao = GameConstants.WIK_NULL;

        // 判断有没有人接炮
        ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            chr[i] = table.GRR._chi_hu_rights[i];
            chr[i].set_empty();
        }

        bHupai = 0;
        int action_hu[] = new int[table.getTablePlayerNumber()];
        int action_pao[] = new int[table.getTablePlayerNumber()];
        int pao_type[][] = new int[table.getTablePlayerNumber()][1];

        int card_type = Constants_LeiYang.HU_CARD_JIE_PAO;

        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();

            if (i == _out_card_player)
                continue;

            int hu_xi_chi[] = new int[1];
            hu_xi_chi[0] = 0;

            PlayerStatus tempPlayerStatus = table._playerStatus[i];
            tempPlayerStatus.reset();

            action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
                    table.GRR._weave_count[i], i, _out_card_player, table._out_card_data, chr[i], card_type, hu_xi_chi,
                    false);

            action_pao[i] = table.estimate_player_respond_phz_chd(i, _out_card_player, table._out_card_data,
                    pao_type[i], false);

            if (table._is_xiang_gong[i] == true)
                action_hu[i] = GameConstants.WIK_NULL;

            if (action_hu[i] != GameConstants.WIK_NULL) {
                bHupai = 1;

                tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
                tempPlayerStatus.add_chi_hu(table._out_card_data, i);

                if (action_pao[i] != GameConstants.WIK_PAO) {
                    tempPlayerStatus.add_action(GameConstants.WIK_NULL);
                    tempPlayerStatus.add_pass(table._out_card_data, _out_card_player);
                } else { // 如果出牌时，有玩家同时能跑牌或胡牌
                    tempPlayerStatus.add_action(GameConstants.WIK_PAO);
                    tempPlayerStatus.add_pao(table._out_card_data, _out_card_player);
                }

                if (table.has_rule(Constants_LeiYang.GAME_RULE_QIANG_ZHI_HU)) {
                    GameSchedule.put(new Runnable() {
                        @Override
                        public void run() {
                            table.handler_operate_card(i, GameConstants.WIK_CHI_HU, table._out_card_data, -1);
                        }
                    }, 1500, TimeUnit.MILLISECONDS);
                    return;
                }

                ti_sao = GameConstants.WIK_CHI_HU;
            } else {
                chr[i].set_empty();
            }
        }

        table._playerStatus[_out_card_player]._hu_card_count = table.get_hh_ting_card_twenty(
                table._playerStatus[_out_card_player]._hu_cards, table.GRR._cards_index[_out_card_player],
                table.GRR._weave_items[_out_card_player], table.GRR._weave_count[_out_card_player], _out_card_player,
                _out_card_player);

        int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
        int ting_count = table._playerStatus[_out_card_player]._hu_card_count;

        if (ting_count > 0) {
        	table.operate_chi_hu_cards(_out_card_player, ting_count, ting_cards);
        	if(table.banker_out_card_count == 1 && _out_card_player == table._cur_banker && table.has_rule(Constants_LeiYang.GAME_RULE_ZHUANG_HANDS_UP) && !table.has_rule(Constants_LeiYang.GAME_RULE_UP_NO_VOICE)){
        		table._game_status = GameConstants.GS_MJ_PAO_QIANG;
        		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
        		table.load_room_info_data(roomResponse);

        		table.operate_player_data();
        		
        		roomResponse.setTarget(_out_card_player);
				roomResponse.setPao(0);
				roomResponse.setPaoMin(0);
				roomResponse.setPaoMax(0);
				roomResponse.setPaoDes("");
				table.send_response_to_player(_out_card_player, roomResponse);
				table._game_status = GameConstants.GS_MJ_PLAY;
        	}
        } else {
            ting_cards[0] = 0;
            table.operate_chi_hu_cards(_out_card_player, 1, ting_cards);
        }

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            if ((action_pao[i] != GameConstants.WIK_NULL) && (bHupai == 0)) { // 如果没人胡牌，但是有人有跑，直接执行跑牌动作
                ti_sao = GameConstants.WIK_PAO;
                table.exe_gang(i, _out_card_player, table._out_card_data, action_pao[i], pao_type[i][0], false, true,
                        false, 500);
                return;
            } else if (action_pao[i] != GameConstants.WIK_NULL) { // 如果有人胡并且有人跑，不能先直接执行跑牌
                ti_sao = GameConstants.WIK_PAO;
            }
        }

        bAroseAction = false;

        if (ti_sao != GameConstants.WIK_PAO) { // 如果没人跑牌，判断有没有吃碰
            if (table.GRR._left_card_count > 0) { // 不是最后一张牌才判断吃碰
                bAroseAction = table.estimate_player_out_card_respond_chen_zhou(_out_card_player, table._out_card_data,
                        false); // 有没有吃碰的
            }
        }

        if (bAroseAction == false && bHupai == 0) { // 如果没人吃碰或者胡牌
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                table._playerStatus[i].clean_action();
                table._playerStatus[i].clean_status();
            }

            table.operate_player_action(_out_card_player, true);

            table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = table._out_card_data;

            table.operate_out_card(_out_card_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            int discard_time = 2000;
            int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
            SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
                    .get(1105);
            if (sysParamModel1105 != null && sysParamModel1105.getVal1() > 0 && sysParamModel1105.getVal1() < 10000) {
                discard_time = sysParamModel1105.getVal1();
            }
            table.exe_add_discard(_out_card_player, 1, new int[] { table._out_card_data }, true, 500);

            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();
            // _out_card_data = 0; // 这样代码可以回引起重连的bug
            table._last_player = _current_player;

            int dispatch_time = 3000;
            if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
                dispatch_time = sysParamModel1105.getVal2();
            }
            table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
        } else {
            for (int i = 0; i < table.getTablePlayerNumber(); i++) {
                playerStatus = table._playerStatus[i];
                if (table._playerStatus[i].has_action()) {
                    table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
                    table.operate_player_action(i, false);
                }
            }
        }
    }

    @Override
    public boolean handler_operate_card(Table_LeiYang table, int seat_index, int operate_code, int operate_card,
            int luoCode) {
        PlayerStatus playerStatus = table._playerStatus[seat_index];

        if (playerStatus.has_action() == false) {
            table.log_player_error(seat_index, "出牌,玩家操作已失效");
            return true;
        }
        if (playerStatus.is_respone()) {
            table.log_player_error(seat_index, "出牌,玩家已操作");
            return true;
        }
        if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
            table.log_player_error(seat_index, "出牌操作,没有这个动作");
            return true;
        }
        if ((operate_card != table._out_card_data)) {
            table.log_player_error(seat_index, "操作牌与当前牌不一样");
            return true;
        }
        
        if ((table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_CHI_HU) || table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_ZI_MO))&& ((operate_code != GameConstants.WIK_CHI_HU) || (operate_code != GameConstants.WIK_ZI_MO))) {
        	//添加过胡，大于不能胡的分才可以胡
        	int huScore = table.getHuScore(seat_index);
        	if(huScore > table.not_can_hu_score[seat_index])
        		table.not_can_hu_score[seat_index] = huScore;
		}

        table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code },
                1);
        playerStatus.operate(operate_code, operate_card);
        playerStatus.clean_status();

        if (operate_code == GameConstants.WIK_NULL) {
            boolean flag = false;

            for (int i = 0; i < playerStatus._action_count; i++) {
                switch (playerStatus._action[i]) {
                case GameConstants.WIK_LEFT:
                case GameConstants.WIK_CENTER:
                case GameConstants.WIK_RIGHT:
                case GameConstants.WIK_XXD:
                case GameConstants.WIK_DDX:
                case GameConstants.WIK_EQS: {
                    if (flag == false) {
                        table._cannot_chi[seat_index][table._cannot_chi_count[seat_index]++] = operate_card;
                        playerStatus.set_exe_pass(true);
                        flag = true;
                        break;
                    }
                }
                    break;
                case GameConstants.WIK_PENG: {
                    table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = operate_card;
                    playerStatus.set_exe_pass(true);
                    flag = true;
                }
                }
            }
        }

        switch (operate_code) {
        case GameConstants.WIK_LEFT:
        case GameConstants.WIK_CENTER:
        case GameConstants.WIK_RIGHT:
        case GameConstants.WIK_XXD:
        case GameConstants.WIK_DDX:
        case GameConstants.WIK_EQS:
            if (luoCode != -1)
                playerStatus.set_lou_pai_kind(luoCode);
        }

        // 变量定义 优先级最高操作的玩家和操作--不通炮的算法
        int target_player = seat_index;
        int target_action = operate_code;
        int target_lou_code = luoCode;
        int target_p = 0;
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {
            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            if (i == target_player) {
                target_p = table.getTablePlayerNumber() - p;
            }
        }
        int cbActionRank[] = new int[table.getTablePlayerNumber()];
        for (int p = 0; p < table.getTablePlayerNumber(); p++) {

            int i = (_out_card_player + p) % table.getTablePlayerNumber();
            int cbUserActionRank = 0;
            int cbTargetActionRank = 0;

            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    cbUserActionRank = table._logic.get_action_rank_with_pao_hu(table._playerStatus[i].get_perform())
                            + table.getTablePlayerNumber() - p;
                    cbActionRank[i] = cbUserActionRank;
                } else {
                    cbUserActionRank = table._logic.get_action_list_rank_with_pao_hu(
                            table._playerStatus[i]._action_count, table._playerStatus[i]._action)
                            + table.getTablePlayerNumber() - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    cbTargetActionRank = table._logic
                            .get_action_rank_with_pao_hu(table._playerStatus[target_player].get_perform()) + target_p;
                    cbActionRank[i] = cbUserActionRank;
                } else {
                    cbTargetActionRank = table._logic.get_action_list_rank_with_pao_hu(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;
                    target_action = table._playerStatus[i].get_perform();
                    target_lou_code = table._playerStatus[i].get_lou_kind();
                    target_p = table.getTablePlayerNumber() - p;
                }
            }
        }

        if (table._playerStatus[target_player].is_respone() == false) {
            table.log_info("优先级最高的人还没操作");
            return true;
        }

        int target_card = table._playerStatus[target_player]._operate_card;

        int last_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
        boolean flag = false;

        for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {
            switch (table._playerStatus[last_player]._action[j]) {
            case GameConstants.WIK_LEFT:
            case GameConstants.WIK_CENTER:
            case GameConstants.WIK_RIGHT:
            case GameConstants.WIK_XXD:
            case GameConstants.WIK_DDX:
            case GameConstants.WIK_EQS:
                if (target_action == GameConstants.WIK_NULL)
                    continue;
                if (flag == false)
                    if (table._playerStatus[last_player].get_exe_pass() == true) {
                        table._cannot_chi[last_player][table._cannot_chi_count[last_player]--] = 0;
                        flag = true;
                        table._playerStatus[last_player].set_exe_pass(false);
                    }

                break;
            }
        }

        int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
                | GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS;

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            boolean flag_temp = false;

            if (table._playerStatus[i].has_action()) {
                for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
                    switch (table._playerStatus[i]._action[j]) {
                    case GameConstants.WIK_LEFT:
                    case GameConstants.WIK_CENTER:
                    case GameConstants.WIK_RIGHT:
                    case GameConstants.WIK_XXD:
                    case GameConstants.WIK_DDX:
                    case GameConstants.WIK_EQS:
                        if (!((target_action == GameConstants.WIK_PENG) || (target_action == GameConstants.WIK_ZI_MO)))
                            continue;
                        if (flag_temp == false)
                            if (table._playerStatus[i].get_exe_pass() == true) {
                                table._cannot_chi[i][table._cannot_chi_count[i]--] = 0;
                                flag_temp = true;
                            }

                        break;
                    case GameConstants.WIK_PENG:
                        if (!((target_action == GameConstants.WIK_NULL)
                                || (target_action & eat_type) != GameConstants.WIK_NULL))
                            continue;
                        if (table._playerStatus[i].get_exe_pass() == false) {
                            table._cannot_peng[i][table._cannot_peng_count[i]++] = operate_card;
                        }
                        break;
                    }
                }
            }

            table._playerStatus[i].clean_action();
            table._playerStatus[i].clean_status();

            table.operate_player_action(i, true);
        }

        switch (target_action) {
        case GameConstants.WIK_LEFT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _out_card_player,
                    table._lou_weave_item[target_player][0]);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);

            return true;
        }
        case GameConstants.WIK_RIGHT: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _out_card_player,
                    table._lou_weave_item[target_player][2]);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);
            return true;
        }
        case GameConstants.WIK_CENTER: // 上牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _out_card_player,
                    table._lou_weave_item[target_player][1]);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);
            return true;
        }
        case GameConstants.WIK_XXD:// 吃小
        {
            // 删除扑克
            int target_card_color = table._logic.get_card_color(target_card);

            int cbRemoveCard[] = new int[2];
            if (target_card_color == 0) {
                cbRemoveCard[0] = target_card;
                cbRemoveCard[1] = target_card + 16;
            } else {
                cbRemoveCard[0] = target_card - 16;
                cbRemoveCard[1] = target_card - 16;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _out_card_player,
                    table._lou_weave_item[target_player][4]);
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);
            return true;
        }
        case GameConstants.WIK_DDX:// 吃大
        {
            // 删除扑克
            int target_card_color = table._logic.get_card_color(target_card);

            int cbRemoveCard[] = new int[2];
            if (target_card_color == 0) {
                cbRemoveCard[0] = target_card + 16;
                cbRemoveCard[1] = target_card + 16;
            } else {
                cbRemoveCard[0] = target_card - 16;
                cbRemoveCard[1] = target_card;
            }
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错");
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _out_card_player,
                    table._lou_weave_item[target_player][5]);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);
            return true;
        }
        case GameConstants.WIK_EQS:// 吃二七十
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            int target_card_value = table._logic.get_card_value(target_card);
            switch (target_card_value) {
            case 2:
                cbRemoveCard[0] = target_card + 5;
                cbRemoveCard[1] = target_card + 8;
                break;
            case 7:
                cbRemoveCard[0] = target_card - 5;
                cbRemoveCard[1] = target_card + 3;
                break;
            case 10:
                cbRemoveCard[0] = target_card - 8;
                cbRemoveCard[1] = target_card - 3;
                break;

            default:
                break;
            }
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "吃牌删除出错" + target_card);
                return false;
            }
            table.add_lou_weave(target_lou_code, target_player, target_card, _out_card_player,
                    table._lou_weave_item[target_player][3]);
            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);
            return true;
        }
        case GameConstants.WIK_PENG: // 碰牌操作
        {
            // 删除扑克
            int cbRemoveCard[] = new int[] { target_card, target_card };
            if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
                table.log_player_error(target_player, "碰牌删除出错");
                return false;
            }

            table.exe_chi_peng(target_player, _out_card_player, target_action, target_card,
                    GameConstants.CHI_PENG_TYPE_OUT_CARD, target_lou_code);
            return true;
        }
        case GameConstants.WIK_PAO: {
            int pao_type[] = new int[1];
            int action = table.estimate_player_respond_phz_chd(target_player, _out_card_player, table._out_card_data,
                    pao_type, false);

            if (action != GameConstants.WIK_NULL) {
                table.exe_gang(target_player, _out_card_player, table._out_card_data, action, pao_type[0], false, true,
                        false, 500);
            }

            return true;
        }
        case GameConstants.WIK_NULL: {
            // 显示出牌
            table.operate_out_card(_out_card_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
                    GameConstants.INVALID_SEAT);

            for (int i = 0; i < table.getTablePlayerNumber(); i++) { // 点过之后，还有再次判断是否有跑？？
                int pao_type[] = new int[1];
                int action = table.estimate_player_respond_phz_chd(i, seat_index, operate_card, pao_type, false);
                if (action != GameConstants.WIK_NULL) {
                    table.exe_gang(i, seat_index, operate_card, action, pao_type[0], true, true, false, 500);
                    return true;
                }
            }

            table.exe_add_discard(this._out_card_player, 1, new int[] { table._out_card_data }, true, 0);

            // 用户切换
            _current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1)
                    % table.getTablePlayerNumber();

            // 发牌
            table._last_player = _current_player;
            table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 1000);

            return true;
        }
        case GameConstants.WIK_CHI_HU: {
            table.GRR._chi_hu_rights[target_player].set_valid(true);

            table.GRR._chi_hu_card[target_player][0] = operate_card;

            table._cur_banker = target_player;

            table._shang_zhuang_player = target_player;

            // table.operate_out_card(this._out_card_player, 0, null,
            // GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

            if (table.has_rule(Constants_LeiYang.GAME_RULE_FAN_XING)) {
                table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true); // 结束后设置翻醒
            }
            table.hu_action = target_action;
            table.process_chi_hu_player_operate(target_player, operate_card, true);

            table.process_chi_hu_player_score_phz(target_player, _out_card_player, operate_card, false);

            table.countChiHuTimes(target_player, true);

            int delay = GameConstants.GAME_FINISH_DELAY_FLS;
            if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
                delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
            }
            GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
                    delay, TimeUnit.SECONDS);

            return true;
        }
        }

        return true;
    }

    @Override
    public boolean handler_player_be_in_room(Table_LeiYang table, int seat_index) {
        RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
        roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

        TableResponse.Builder tableResponse = TableResponse.newBuilder();

        table.load_room_info_data(roomResponse);
        table.load_player_info_data(roomResponse);
        table.load_common_status(roomResponse);

        // 游戏变量
        tableResponse.setBankerPlayer(table.GRR._banker_player);
        tableResponse.setCurrentPlayer(_out_card_player);
        tableResponse.setCellScore(0);

        // 状态变量
        tableResponse.setActionCard(0);
        // tableResponse.setActionMask((_response[seat_index] == false) ?
        // _player_action[seat_index] : MJGameConstants.WIK_NULL);
        // 历史记录
        tableResponse.setOutCardData(0);
        tableResponse.setOutCardPlayer(0);

        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
            tableResponse.addTrustee(false);// 是否托管
            // 剩余牌数
            tableResponse.addDiscardCount(table.GRR._discard_count[i]);
            Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
            for (int j = 0; j < 55; j++) {
                if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
                    // 癞子
                    int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
                } else {
                    int_array.addItem(table.GRR._discard_cards[i][j]);
                }
            }
            tableResponse.addDiscardCards(int_array);
            // 组合扑克
            tableResponse.addWeaveCount(table.GRR._weave_count[i]);
            WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
            for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
                WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
                if (seat_index != i) {
                    if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
                            || table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
                            || table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
                            && table.GRR._weave_items[i][j].public_card == 0) {
                        weaveItem_item.setCenterCard(0);
                    } else {
                        if (table.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)
                                && table.has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
                                && table._xt_display_an_long[i] == true)
                            weaveItem_item.setCenterCard(0);
                        else
                            weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                    }
                } else {
                    weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
                }
                weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
                weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
                weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
                weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
                weaveItem_array.addWeaveItem(weaveItem_item);
            }
            tableResponse.addWeaveItemArray(weaveItem_array);

            //
            tableResponse.addWinnerOrder(0);
            tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

        }

        // 数据
        tableResponse.setSendCardData(0);
        int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
        int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

        // 先注释掉，等客户端一起联调
        for (int x = 0; x < hand_card_count; x++) {
            if (table.is_card_has_wei(hand_cards[x])) { // 如果是偎的牌
                // 判断打出这张牌是否能听牌
                table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]--;
                boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[seat_index],
                        table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
                table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]++;

                if (b_is_ting_state)
                    hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
                else
                    hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
            }
        }

        for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
            tableResponse.addCardsData(hand_cards[i]);
        }

        roomResponse.setTable(tableResponse);

        table.send_response_to_player(seat_index, roomResponse);

        table.operate_out_card(table._out_card_player, 1, new int[] { table._out_card_data },
                GameConstants.OUT_CARD_TYPE_MID, seat_index);

        if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
            table.operate_player_action(seat_index, false);
        }
        
        if (bAroseAction == false && bHupai == 0) { // 如果没人吃碰或者胡牌
        	 table.operate_out_card(_out_card_player, 0, null, GameConstants.OUT_CARD_TYPE_MID,
                     GameConstants.INVALID_SEAT);
        }

        if (table._is_xiang_gong[seat_index] == true)
            table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

        table.istrustee[seat_index] = false;

        int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
        int ting_count = table._playerStatus[seat_index]._hu_card_count;

        if (ting_count > 0) {
            table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
        }

        return true;
    }
}
