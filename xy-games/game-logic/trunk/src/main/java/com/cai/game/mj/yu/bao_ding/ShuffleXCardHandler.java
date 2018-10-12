package com.cai.game.mj.yu.bao_ding;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.GameConstants_BD;
import com.cai.common.define.ECardType;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.SpringService;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;

public class ShuffleXCardHandler extends AbstractMJHandler<Table_BD> {

	private int send_count; // 一般是发三次，每次发四张

	private int[][] send_cards;

	private int send_card_count;

	private boolean[] non_kou_player;

	public void reset_status(Table_BD table) {
		send_card_count = 4;
		send_cards = new int[table.getTablePlayerNumber()][send_card_count];
	}

	@Override
	public void exe(Table_BD table) {
		table.reset_init_data();
		table.kou_cards_index = new int[table.getTablePlayerNumber()][GameConstants_BD.MAX_INDEX];
		table.non_kou_cards_index = new int[table.getTablePlayerNumber()][GameConstants_BD.MAX_INDEX];
		non_kou_player = new boolean[table.getTablePlayerNumber()];

		// 庄家选择
		table.progress_banker_select();

		table._game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		table.GRR._banker_player = table._cur_banker;
		table._current_player = table.GRR._banker_player;

		table.initShuffle();

		int send_index = send_count * table.getTablePlayerNumber() * send_card_count;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._logic.switch_to_cards_index(table._repertory_card, send_index, send_card_count, table.GRR._cards_index[i]);
			table.GRR._left_card_count -= send_card_count;

			for (int j = 0; j < send_card_count; j++) {
				send_cards[i][j] = table._repertory_card[send_index + j];
				table.non_kou_cards_index[i][table._logic.switch_to_card_index(table._repertory_card[send_index + j])]++;
			}

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(i, cards);
			table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i], table.GRR._weave_items[i]);

			table._playerStatus[i].add_action(GameConstants_BD.WIK_KOU);
			table.operate_player_action(i, false);

			send_index += send_card_count;
		}

		send_count++;
	}

	public boolean send_all_card_2_player(Table_BD table) {
		for (int player = 0; player < table.getTablePlayerNumber() && send_count != 3; player++) {
			if (!non_kou_player[player])
				return false;
		}

		int send_index = send_count * table.getTablePlayerNumber() * send_card_count;
		for (int player = 0; player < table.getTablePlayerNumber(); player++) {
			for (int i = send_count; i <= 3; i++) {
				table._logic.switch_to_cards_index(table._repertory_card, send_index, send_card_count, table.GRR._cards_index[player]);
				table.GRR._left_card_count -= send_card_count;

				for (int j = 0; j < send_card_count; j++)
					table.non_kou_cards_index[player][table._logic.switch_to_card_index(table._repertory_card[send_index + j])]++;

				send_index += send_card_count;
			}

			// 第十三张牌
			table._logic.switch_to_cards_index(table._repertory_card, send_index, 1, table.GRR._cards_index[player]);
			table.GRR._left_card_count -= 1;
			table.non_kou_cards_index[player][table._logic.switch_to_card_index(table._repertory_card[send_index])]++;
			send_index++;

			// 推送数据
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(player, cards);
			table.operate_player_cards(player, hand_card_count, cards, table.GRR._weave_count[player], table.GRR._weave_items[player]);
		}
		return true;
	}

	@Override
	public boolean handler_operate_card(Table_BD table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants_BD.WIK_KOU) {
			table.player_kou_count[seat_index]++;
			table.non_kou_cards_index[seat_index] = new int[GameConstants_BD.MAX_INDEX];
			for (int i = 0; i < send_card_count; i++) {
				table.kou_cards_index[seat_index][table._logic.switch_to_card_index(send_cards[seat_index][i])]++;

				if (send_cards[seat_index][i] > GameConstants_BD.MAX_ZI)
					table.add_kou_feng_card(seat_index, send_cards[seat_index][i]);
			}

			// 推送数据
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table.switch_to_cards_data(seat_index, cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);
			non_kou_player[seat_index] = false;

		} else {
			non_kou_player[seat_index] = true;
		}

		// 检测下大家是否都操作没有
		for (int player = 0; player < table.getTablePlayerNumber(); player++) {
			if (table._playerStatus[player].is_respone())
				return true;
		}

		boolean end_send = send_all_card_2_player(table);

		if (!end_send) {
			int send_index = send_count * table.getTablePlayerNumber() * send_card_count;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._logic.switch_to_cards_index(table._repertory_card, send_index, send_card_count, table.GRR._cards_index[i]);
				table.GRR._left_card_count -= send_card_count;

				for (int j = 0; j < send_card_count; j++) {
					send_cards[i][j] = table._repertory_card[send_index + j];
					table.non_kou_cards_index[i][table._logic.switch_to_card_index(table._repertory_card[send_index + j])]++;
				}

				send_index += send_card_count;

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table.switch_to_cards_data(i, cards);
				table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i], table.GRR._weave_items[i]);

				table._playerStatus[i].add_action(GameConstants_BD.WIK_KOU);
				table.operate_player_action(i, false);
			}
			send_count++;
			return true;
		}

		table.getLocationTip();

		try {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table.GRR._cards_index[i].length; j++) {
					if (table.GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(table.get_players()[i], ECardType.anLong, "", table.GRR._cards_index[i][j], 0l,
								table.getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.istrustee[i] = false;
		}


		table.on_game_start();
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_BD table, int seat_index) {
		return super.handler_player_be_in_room(table, seat_index);
	}
}
