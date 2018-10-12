package com.cai.game.mj.hunan.zhuzhouwang;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import com.cai.common.constant.game.mj.GameConstants_ZhuZhouWang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;

/**
 * 
 * 
 *
 * @author shiguoqiong date: 2018年3月21日 下午5:25:12 <br/>
 */
public class HandlerSelectMagic_ZhuZhouWang extends AbstractMJHandler<Table_ZhuZhouWang> {

	protected int _banker;
	protected int _send_card_data;
	protected int _seat_index;

	public void reset_status(int banker) {
		_banker = banker;
		_seat_index = banker;
	}

	@Override
	public void exe(Table_ZhuZhouWang table) {
		// 选取鬼牌
		init_magicCard(table);

		// 处理每个玩家手上的牌,鬼牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			// 显示鬼牌
			// table.showSpecialCard(i);
			// 刷新手牌
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == table.ding_wang_card) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		// TODO 庄家发第14张牌
		++table._send_card_count;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x28;
		}
		
		if (table.DEBUG_SPECIAL_CARD) {
			_send_card_data = table.special_card_decidor;
		}
		
		table._send_card_data = _send_card_data;

		table._current_player = _banker;
		table._provide_player = _banker;
		
		// 判断天胡
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
		currentPlayerStatus.reset();

		// 自摸时的胡牌检测
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], table._send_card_data, chr,
				GameConstants_ZhuZhouWang.HU_CARD_TYPE_TIAN_HU, _seat_index);
		
		table.GRR._cards_index[_banker][table._logic.switch_to_card_index(_send_card_data)]++;
		
		// 处理王牌
		int real_card = _send_card_data;
		if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (real_card == table.ding_wang_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}
		// 客户端显示玩家抓牌
		table.operate_player_get_card(_banker, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		if (GameConstants.WIK_NULL != action) {
			currentPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			currentPlayerStatus.add_zi_mo(table._send_card_data, _seat_index);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			chr.set_empty();

			table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

			if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ER_WU_BA_JIANG)) {
				table.exe_qi_shou_hu(1, table._cur_banker, GameConstants.INVALID_VALUE);
			} else {
				table.exe_bao_ting(1, table._cur_banker, GameConstants.INVALID_VALUE);
			}

		}
	}

	// 鬼牌初始化
	public void init_magicCard(Table_ZhuZhouWang table) {
		table._logic.clean_magic_cards();

		int index = GameConstants.MAX_ZI;

		Random random = new Random();
		int rand = (random.nextInt(index) + 1 + random.nextInt(index) + 1) % index;
		int nShaiZhangCard = table._logic.switch_to_card_data(rand);
		int p = table.tou_zi_dian_shu[0] + table.tou_zi_dian_shu[1];
		int opsition = table._all_card_len - 1 - p * 2;
		if (opsition < table._all_card_len)
			nShaiZhangCard = table._repertory_card[table._all_card_len - 2 - (p - 1) * 2];

		if (table.DEBUG_CARDS_MODE) {
			nShaiZhangCard = 0x13;

		}

		if (table._logic.is_valid_card(table.magic_card_decidor)) {
			if (table._logic.switch_to_card_index(table.magic_card_decidor) >= 0
					&& table._logic.switch_to_card_index(table.magic_card_decidor) < GameConstants.MAX_ZI)
				nShaiZhangCard = table.magic_card_decidor;
		}

		table.ding_wang_card = nShaiZhangCard;

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { table.ding_wang_card },
				GameConstants.INVALID_SEAT);

		int cur_data = table._logic.get_card_value(table.ding_wang_card);

		boolean pre_out = false;
		boolean next_out = false;
		if (cur_data >= 2 && cur_data <= 8) {
			table.joker_card_1 = table.ding_wang_card + 1;
			table.joker_card_2 = table.ding_wang_card - 1;
		} else if (cur_data == 1) {
			table.joker_card_1 = table.ding_wang_card + 1;
			table.joker_card_2 = table.ding_wang_card + 8;
			pre_out = true;
		} else if (cur_data == 9) {
			table.joker_card_1 = table.ding_wang_card - 8;
			table.joker_card_2 = table.ding_wang_card - 1;
			next_out = true;
		}

		table.ding_wang_card_index = table._logic.switch_to_card_index(table.ding_wang_card);
		table.joker_card_index_1 = table._logic.switch_to_card_index(table.joker_card_1);
		table.joker_card_index_2 = table._logic.switch_to_card_index(table.joker_card_2);

		table.GRR._especial_card_count = 0;
		// 减1为纯王
		if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_CHUN_WANG_MINUS)) {
			table.GRR._especial_card_count = 2;

			table.GRR._especial_show_cards[0] = table.joker_card_2 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			table.GRR._especial_show_cards[1] = table.ding_wang_card + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;

			// TODO 需要清空一下
			table.joker_card_1 = 0;
			table.joker_card_index_1 = -1;

			// 正王为癞规则
			if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHENG_WANG_LAI)) {
				if (pre_out) {
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
					table._logic.add_magic_card_index(table.joker_card_index_2);
				} else {
					table._logic.add_magic_card_index(table.joker_card_index_2);
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
				}
			} else {
				table._logic.add_magic_card_index(table.joker_card_index_2);
			}

		} else if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_CHUN_WANG_PLUS)) {
			table.GRR._especial_card_count = 2;

			table.GRR._especial_show_cards[0] = table.ding_wang_card + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;

			table.GRR._especial_show_cards[1] = table.joker_card_1 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;

			// TODO 需要清空一下
			table.joker_card_2 = 0;
			table.joker_card_index_2 = -1;

			// 正王为癞规则
			if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHENG_WANG_LAI)) {
				if (next_out) {
					table._logic.add_magic_card_index(table.joker_card_index_1);
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
				} else {
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
					table._logic.add_magic_card_index(table.joker_card_index_1);
				}
			} else {
				table._logic.add_magic_card_index(table.joker_card_index_1);
			}

		} else if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_CHUN_WANG_SIGN)) {
			table.GRR._especial_card_count = 3;

			table.GRR._especial_show_cards[0] = table.joker_card_2 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			table.GRR._especial_show_cards[1] = table.ding_wang_card + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			table.GRR._especial_show_cards[2] = table.joker_card_1 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;

			// 正王为癞规则
			if (table.has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHENG_WANG_LAI)) {
				if (next_out) {
					table._logic.add_magic_card_index(table.joker_card_index_1);
					table._logic.add_magic_card_index(table.joker_card_index_2);
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
				} else if (pre_out) {
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
					table._logic.add_magic_card_index(table.joker_card_index_1);
					table._logic.add_magic_card_index(table.joker_card_index_2);
				} else {
					table._logic.add_magic_card_index(table.joker_card_index_2);
					table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.ding_wang_card));
					table._logic.add_magic_card_index(table.joker_card_index_1);
				}
			} else {
				if (!next_out && !pre_out) {
					table._logic.add_magic_card_index(table.joker_card_index_2);
					table._logic.add_magic_card_index(table.joker_card_index_1);
				} else {
					table._logic.add_magic_card_index(table.joker_card_index_1);
					table._logic.add_magic_card_index(table.joker_card_index_2);
				}
			}
		}
	}

	// 天胡选择操作
	@Override
	public boolean handler_operate_card(Table_ZhuZhouWang table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}
		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants.WIK_NULL }, 1);

			playerStatus.clean_action();
			playerStatus.clean_status();

			table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			table.exe_qi_shou_hu(1, table._cur_banker, GameConstants.INVALID_VALUE);
			return true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (operate_code) {
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			table._cur_banker = seat_index;

			table.hu_dec_type[seat_index] = 1;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				table.hu_dec_type[i] = 5;
			}

			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._player_result.zi_mo_count[seat_index]++;

			table.set_niao_card(seat_index, GameConstants.INVALID_VALUE, true);

			table.process_chi_hu_player_operate(seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_ZhuZhouWang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 骰子
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table.GRR._discard_cards[i][j] == table.joker_card_1
						|| table.GRR._discard_cards[i][j] == table.joker_card_2) {
					table.GRR._discard_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card) {
					table.GRR._discard_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(
						table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}

		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;

		if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (real_card == table.ding_wang_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);

		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
