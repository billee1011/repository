package com.cai.game.mj;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.HuCardInfo;
import com.cai.game.mj.sichuan.leshan.Table_LeShan;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Basic.HuCard;
import protobuf.clazz.mj.Basic.HuCardList;
import protobuf.clazz.mj.Basic.RoundHuCards;

public final class MahjongUtils {
	private MahjongUtils() {
	}

	/**
	 * 断线重连，显示玩家的停牌数据
	 * 
	 * @param table
	 * @param seat_index
	 */
	public static void showTingPai(AbstractMJTable table, int seat_index) {
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
	}

	/**
	 * 四川麻将，断线重连，显示骰子点数、杠之后的发牌张数和已经胡牌的玩家的胡牌数据
	 * 
	 * @param table
	 * @param roomResponse
	 */
	public static void showTouZiSiChuan(AbstractSiChuanMjTable table, RoomResponse.Builder roomResponse) {
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		for (int i = 0; i < table.table_hu_card_count; i++) {
			roomResponse.addCardsList(table.table_hu_cards[i]);
		}

		roomResponse.setPageSize(table.gang_dispatch_count);
	}

	/**
	 * 四川麻将，断线重连时，显示所有玩家胡的牌
	 * 
	 * @param table
	 * @param roomResponse
	 */
	public static void showHuCardsSiChuan(AbstractSiChuanMjTable table, RoomResponse.Builder roomResponse) {
		RoundHuCards.Builder rhd = RoundHuCards.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			HuCardList.Builder hcl = HuCardList.newBuilder();

			HuCardInfo info = table.player_hu_card_info[i];
			for (int j = 0; j < info.count; j++) {
				HuCard.Builder hc = HuCard.newBuilder();
				hc.setCard(info.hu_cards[j]);
				hc.setProviderIndex(info.provider_index[j]);

				hcl.addHuCards(hc);
			}

			rhd.addAllHuCardsList(hcl);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(rhd));
	}

	/**
	 * 断线重连，加载一些房间的基础信息
	 * 
	 * @param table
	 * @param roomResponse
	 * @param tableResponse
	 */
	public static void dealCommonDataReconnect(AbstractMJTable table, RoomResponse.Builder roomResponse, TableResponse.Builder tableResponse) {
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
	 * 断线重连，处理所有玩家的废牌堆、组合牌、和牌张数的信息。组合牌无指向标（就是提供者没加特殊值）。无特殊牌
	 * 
	 * @param table
	 * @param tableResponse
	 */
	public static void dealAllPlayerCardsNoSpecial(AbstractMJTable table, TableResponse.Builder tableResponse) {
		if (table instanceof Table_LeShan) {
			dealAllPlayerCardsLsyj(table, tableResponse);
			return;
		}

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
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (table._handler == table._handler_dispath_card && i == table._current_player) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
	}

	public static void dealAllPlayerCardsLaiZi(AbstractMJTable table, TableResponse.Builder tableResponse) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int tmpCard = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(tmpCard))
					tmpCard += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(tmpCard);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (table._handler == table._handler_dispath_card && i == table._current_player) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
	}

	/**
	 * 断线重连，落地牌带有提供者箭头指向标
	 * 
	 * @param table
	 * @param tableResponse
	 */
	public static void dealAllPlayerCardsWithDirection(AbstractMJTable table, TableResponse.Builder tableResponse) {
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
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (table._handler == table._handler_dispath_card && i == table._current_player) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
	}

	/**
	 * 断线重连，处理所有玩家的废牌堆、组合牌、和牌张数的信息。组合牌无指向标（就是提供者没加特殊值）。无特殊牌
	 * 
	 * @param table
	 * @param tableResponse
	 */
	private static void dealAllPlayerCardsLsyj(AbstractMJTable table, TableResponse.Builder tableResponse) {
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

			boolean showBlack = true;

			for (int j = 0; j < table.GRR._weave_count[i]; j++) {
				if (table.GRR._weave_items[i][j].public_card == 1)
					showBlack = false;
			}

			for (int j = 0; j < table.GRR._weave_count[i]; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				if (showBlack && i != table._current_player) {
					weaveItem_item.setCenterCard(GameConstants.BLACK_CARD);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (table._handler == table._handler_dispath_card && i == table._current_player) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
	}

	/**
	 * 有多人有胡时，只有有人点了胡，记录所有人点击的操作
	 * 
	 * @param table
	 */
	public static void recordActionWhenSomeoneClickWin(AbstractMJTable table) {
		boolean has_player_click_win = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action() && (table._playerStatus[i].get_perform() == GameConstants.WIK_ZI_MO
					|| table._playerStatus[i].get_perform() == GameConstants.WIK_CHI_HU)) {
				has_player_click_win = true;
			}
		}

		if (has_player_click_win == true) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action() && table._playerStatus[i].get_perform() != GameConstants.WIK_NULL) {
					table.record_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { table._playerStatus[i].get_perform() }, 1);
				}
			}
		}
	}

	/**
	 * 检查牌桌上是否已经有吃碰杠
	 * 
	 * @param table
	 * @return
	 */
	public static boolean hasLuoDiPai(AbstractMJTable table) {
		if (table.GRR != null) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table.GRR._weave_count[i] != 0)
					return true;
			}
		}
		return false;
	}

	/**
	 * 血流成河，断线重连的时候，处理读秒时间
	 * 
	 * @param table
	 */
	public static void dealScheduleCounter(AbstractSiChuanMjTable table) {
		if (table.schedule_start_time != -1) {
			long cur_time = System.currentTimeMillis();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table.over_time_left[i] != -1) {
					table.over_time_left[i] = table.get_over_time_value() - (int) (cur_time - table.schedule_start_time) / 1000;
				}
				if (table.over_time_left[i] <= 0) {
					table.over_time_left[i] = -1;
				}
			}
		}
	}
}
