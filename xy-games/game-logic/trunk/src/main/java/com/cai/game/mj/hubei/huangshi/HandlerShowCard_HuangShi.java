package com.cai.game.mj.hubei.huangshi;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerShowCard_HuangShi extends AbstractMJHandler<Table_HuangShi> {
	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(Table_HuangShi table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_weave();
		}

		boolean bAroseAction = false;

		// TODO 每个玩家都分析一遍有没有中发白的组合，如果点了亮，将所有的中发白组合落在地上
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			boolean has_zhong_fa_bai = table.check_zhong_fa_bai(table.GRR._cards_index[i], i);

			if (has_zhong_fa_bai) {
				bAroseAction = true;
				table._playerStatus[i].add_action(GameConstants.WIK_SHOW_CARD);
				table._playerStatus[i].add_show_card(Constants_HuangShi.HONG_ZHONG_CARD, GameConstants.WIK_SHOW_CARD, i);
			}
		}

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}

			table.operate_player_action(_banker, true);

			table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				PlayerStatus playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_HuangShi table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		} else if (operate_code == GameConstants.WIK_SHOW_CARD) {
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1,
					GameConstants.INVALID_SEAT);

			for (int i = 0; i < table.player_zhong_fa_bai_weave_count[seat_index]; i++) {
				int wIndex = table.GRR._weave_count[seat_index]++;
				table.GRR._weave_items[seat_index][wIndex].public_card = 1;
				table.GRR._weave_items[seat_index][wIndex].center_card = Constants_HuangShi.HONG_ZHONG_CARD;
				table.GRR._weave_items[seat_index][wIndex].weave_kind = GameConstants.WIK_SHOW_CARD;
				table.GRR._weave_items[seat_index][wIndex].provide_player = seat_index;

				table.GRR._cards_index[seat_index][Constants_HuangShi.HONG_ZHONG_INDEX]--;
				table.GRR._cards_index[seat_index][Constants_HuangShi.FA_CAI_INDEX]--;
				table.GRR._cards_index[seat_index][Constants_HuangShi.BAI_BAN_INDEX]--;
			}

			boolean can_fa_cai_gang = table.has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_huangshi(table.GRR._cards_index[seat_index], cards, can_fa_cai_gang);

			for (int i = 0; i < hand_card_count; i++) {
				if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
					if (table._logic.is_magic_card(cards[i])) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					} else if (cards[i] == Constants_HuangShi.HONG_ZHONG_CARD) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
					} else if (cards[i] == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
					}
				}
			}

			int tmp_player_fan_shu = table.get_player_fan_shu(seat_index);
			if (table.player_fan_shu[seat_index] != tmp_player_fan_shu) {
				table.operate_player_info();
				table.player_fan_shu[seat_index] = tmp_player_fan_shu;

				// 每次牌桌上有番变动，都重新获取一次听牌数据
				for (int p = 0; p < table.getTablePlayerNumber(); p++) {
					table._playerStatus[p]._hu_card_count = table.get_ting_card(table._playerStatus[p]._hu_cards, table.GRR._cards_index[p],
							table.GRR._weave_items[p], table.GRR._weave_count[p], p);
					int tmp_ting_cards[] = table._playerStatus[p]._hu_cards;
					int tmp_ting_count = table._playerStatus[p]._hu_card_count;

					if (tmp_ting_count > 0) {
						table.operate_chi_hu_cards(p, tmp_ting_count, tmp_ting_cards);

						// 牌桌上番变动之后，如果有人有听牌数据了，显示‘自动胡牌’按钮
						table.operate_auto_win_card(p, true);
					} else {
						tmp_ting_cards[0] = 0;
						table.operate_chi_hu_cards(p, 1, tmp_ting_cards);
					}
				}
			}

			// 刷新手牌
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_action_by_code(GameConstants.WIK_SHOW_CARD)))
				return false;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_HuangShi table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		// roomResponse.setTarget(seat_index);
		// roomResponse.setScoreType(table.get_player_fan_shu(seat_index));
		// table.send_response_to_other(seat_index, roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_banker);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		boolean can_fa_cai_gang = table.has_rule(Constants_HuangShi.GAME_RULE_HONG_ZHONG_FA_CAI_GANG);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int real_card = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (real_card == Constants_HuangShi.HONG_ZHONG_CARD) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_GANG;
				} else if (real_card == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_GANG;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0
						&& i != seat_index) {
					weaveItem_item.setCenterCard(0);

					for (int x = 0; x < 4; x++) {
						weaveItem_item.addWeaveCard(-1);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);

					int[] weave_cards = new int[4];
					int count = table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind,
							table.GRR._weave_items[i][j].center_card, weave_cards);
					for (int x = 0; x < count; x++) {
						if (table._logic.is_magic_card(weave_cards[x]))
							weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

						weaveItem_item.addWeaveCard(weave_cards[x]);
					}
				}

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data_huangshi(table.GRR._cards_index[seat_index], hand_cards, can_fa_cai_gang);

		for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
			if (table._logic.is_magic_card(hand_cards[j])) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else if (hand_cards[j] == Constants_HuangShi.HONG_ZHONG_CARD) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
			} else if (hand_cards[j] == Constants_HuangShi.FA_CAI_CARD && can_fa_cai_gang) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GANG;
			}
			tableResponse.addCardsData(hand_cards[j]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _banker, false); // 效果
		} else {
			// 听牌显示
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}
}
