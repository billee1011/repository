package com.cai.game.mj.hubei.ezhou;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_EZ extends AbstractMJTable {
	private static final long serialVersionUID = -8673836743823288270L;

	public HandlerSelectMagic_EZ _handler_select_magic;

	public boolean can_win_pi_hu[] = new boolean[getTablePlayerNumber()];
	public boolean can_ruan_hu[] = new boolean[getTablePlayerNumber()];
	public boolean can_only_zi_mo[] = new boolean[getTablePlayerNumber()];
	public int da_dian_card;
	public int magic_card_index;

	public boolean gang_status;
	public boolean gang_da_kao;
	public int gang_pai_player = GameConstants.INVALID_SEAT;
	public int hong_zhong_gang_count;

	public int[] effective_weave_count = new int[getTablePlayerNumber()];

	public int left_card_count_after_gang = 0; // 红中杠的时候，存储一下牌堆里还有多少张牌

	public int[] tou_zi_dian_shu = new int[2]; // 用来储存2个骰子的点数

	public int time_for_animation = 2000; // 摇骰子的动画时间(ms)
	public int time_for_fade = 500; // 摇骰子动画的消散延时(ms)

	public int qi_pai_index = 0; // 起牌的索引位置，一共有4个牌堆

	public boolean can_win_but_without_enough_score = false; // 能胡，但是没达到起胡分

	// 牌桌上，每局游戏，所有玩家的番数记录，用于出牌包赔判断
	public int[] player_multiple_count = new int[getTablePlayerNumber()];
	// 牌桌上，开始包赔判断
	public boolean start_compensation_judge;
	// 接炮时，记录一下牌型分
	public int[] score_when_jie_pao_hu = new int[getTablePlayerNumber()];
	// 有接炮时，不胡，再次存储一下，胡牌时的牌型分，和score_when_abandoned_win进行比较
	public int[] score_when_abandoned_jie_pao = new int[getTablePlayerNumber()];

	public int auto_out_card_delay = 500; // 自动出牌的延迟时间
	public int action_wait_time = 5000; // 自动胡牌托管之后，如果有操作，等待5秒，5秒之后自动出牌
	public int auto_deal_win_delay = 500; // 自动胡牌的延迟时间

	private int winner_pai_xing_fen = 0; // 牌型分

	/**
	 * 从什么地方走的分析胡牌算分，1表示从获取听牌数据的地方，2表示自摸或接炮的时候正常分析胡牌
	 */
	public int analyse_state;
	public static final int FROM_TING = 1;
	public static final int NORMAL = 2;

	public Table_EZ() {
		super(MJType.GAME_TYPE_ER_ZHOU);
	}

	/**
	 * 后期需要添加的摇骰子的效果
	 * 
	 * @param table
	 * @param seat_index
	 */
	@Override
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		// operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1],
		// time_for_animation, time_for_fade);
	}

	/**
	 * 在牌桌上显示摇骰子的效果
	 * 
	 * @param tou_zi_one
	 *            骰子1的点数
	 * @param tou_zi_two
	 *            骰子2的点数
	 * @param time_for_animate
	 *            动画时间
	 * @param time_for_fade
	 *            动画保留时间
	 * @return
	 */
	@Override
	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		if (GRR != null)
			roomResponse.setTarget(GRR._banker_player);
		else
			roomResponse.setTarget(0);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 根据2个骰子的点数和当前赢家的位置，确定起拍的位置
	 * 
	 * @param tou_zi_one
	 * @param tou_zi_two
	 * @return
	 */
	public int get_qi_pai_player(int tou_zi_one, int tou_zi_two) {
		int banker_player = 0;
		if (GRR != null)
			banker_player = GRR._banker_player;

		qi_pai_index = (banker_player + (tou_zi_one + tou_zi_two - 1) % 4) % 4;

		return qi_pai_index;
		// return get_seat(tou_zi_one + tou_zi_two, banker_player);
	}

	public int get_player_fan_shu(int seat_index) {
		int fan_shu = 0;

		if (GRR != null) {
			for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
				if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
					fan_shu += 2;
				else
					fan_shu += 1;
			}

			fan_shu += GRR._gang_score[seat_index].ming_gang_count + GRR._gang_score[seat_index].an_gang_count * 2;
		}

		if (effective_weave_count != null) {
			if (effective_weave_count[seat_index] >= 3)
				fan_shu += 1;
		}

		if (fan_shu == 0)
			return 0;
		else
			return (int) Math.pow(2, fan_shu);
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			// TODO 闹字段，0表示正常，1表示接炮，2表示放炮
			if (GRR != null) {
				if (!GRR._chi_hu_rights[i].opr_and(Constants_EZ.CHR_JIE_PAO).is_empty()) {
					room_player.setNao(1);
				} else if (!GRR._chi_hu_rights[i].opr_and(Constants_EZ.CHR_FANG_PAO).is_empty()) {
					room_player.setNao(2);
				} else {
					room_player.setNao(0);
				}
			} else {
				room_player.setNao(0);
			}
			// room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			// room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setBiaoyan(get_player_fan_shu(i));

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setScoreType(get_player_fan_shu(seat_index));
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		// TODO 向其他人发送组合牌数据的时候，暗杠不能发center_card数据
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].weave_kind == GameConstants.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		send_response_to_other(seat_index, roomResponse);

		// TODO 向自己发送手牌数据，自己的数据里，暗杠是能看到的
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		GRR.add_room_response(roomResponse);
		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setScoreType(get_player_fan_shu(seat_index));
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		// TODO 向其他人发送组合牌数据的时候，暗杠不能发center_card数据
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].weave_kind == GameConstants.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		send_response_to_other(seat_index, roomResponse);

		// TODO 向自己发送手牌数据，自己的数据里，暗杠是能看到的
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			int out_card = _playerStatus[seat_index]._hu_out_card_ting[i];

			roomResponse.addOutCardTing(out_card + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);

				if (is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
					roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[i][j]);
				}
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		// 暂时只显示一个CHR，好配合摆牌的时间。
		// int effect_count = chr.type_count;
		int effect_count = 1;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i]))
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		// 把其他玩家的牌也倒下来展示3秒
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			operate_player_cards(i, 0, null, 0, null);

			hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[i], cards);

			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(cards[j]))
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		// 小结算界面显示牌型分
		game_end.setHuXi(winner_pai_xing_fen);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				// for (int k = 0; k < getTablePlayerNumber(); k++) {
				// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				// }
				// }

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			if (is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
				int k = 0;
				int left_card_count = GRR._left_card_count;
				int cards[] = new int[GRR._left_card_count];
				for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
					cards[k] = _repertory_card[_all_card_len - left_card_count];
					game_end.addCardsList(cards[k]);
					k++;
					left_card_count--;
				}
			}

			for (int i = 0; i < GameConstants.MAX_LAI_ZI_PI_ZI_GANG && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_LAI_ZI_PI_ZI_GANG && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._lai_zi_pi_zi_gang[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][h])) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]);
					}
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_ezhou(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else if (GRR._cards_data[i][j] == Constants_EZ.HZ_CARD) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HZ);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				// TODO 用game_end的jetton_score字段存储牌型底分
				game_end.addJettonScore(0);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		// 每一局游戏结束之后，初始化托管状态，并隐藏托管按钮
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
			operate_auto_win_card(i, false);
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		return 0;
	}

	public int analyse_chi_hu_card_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index, int provide_index) {
		can_win_but_without_enough_score = false;

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// TODO: 手里有红中不能胡牌
		if (cbCardIndexTemp[Constants_EZ.HZ_INDEX] > 0)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		if (card_type == Constants_EZ.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_EZ.CHR_ZI_MO);
		} else if (card_type == Constants_EZ.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_EZ.CHR_JIE_PAO);
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count); // 带癞子时能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0); // 癞子牌还原之后能胡或者手牌没癞子牌能胡

		boolean can_win_qi_dui_with_magic = false;
		boolean can_win_qi_dui_without_magic = false;

		// if (has_rule(Constants_EZ.GAME_RULE_QI_DUI)) {
		int check_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		can_win_qi_dui_with_magic = check_qi_xiao_dui != GameConstants.WIK_NULL; // 带癞子时能胡七对
		can_win_qi_dui_without_magic = _logic.check_hubei_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 硬七对

		if (can_win_qi_dui_with_magic) {
			if (check_qi_xiao_dui == Constants_EZ.CHR_SAN_HAO_HUA_QI_DUI)
				chiHuRight.opr_or(Constants_EZ.CHR_SAN_HAO_HUA_QI_DUI);
			else if (check_qi_xiao_dui == Constants_EZ.CHR_SHH_QI_DUI)
				chiHuRight.opr_or(Constants_EZ.CHR_SHH_QI_DUI);
			else if (check_qi_xiao_dui == Constants_EZ.CHR_HAO_HUA_QI_DUI)
				chiHuRight.opr_or(Constants_EZ.CHR_HAO_HUA_QI_DUI);
			else
				chiHuRight.opr_or(Constants_EZ.CHR_QI_DUI);
		}
		// }

		boolean can_win_jiang_yi_se_with_magic = false;
		boolean can_win_jiang_yi_se_without_magic = false;

		if (has_rule(Constants_EZ.GAME_RULE_JIANG_YI_SE)) {
			can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 将一色，不用一对牌+3n，也可以胡牌
			can_win_jiang_yi_se_without_magic = _logic.check_hubei_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 硬将一色

			if (can_win_jiang_yi_se_with_magic)
				chiHuRight.opr_or(Constants_EZ.CHR_JIANG_YI_SE);
		}

		boolean can_win = can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic; // 能胡牌

		if (can_win == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		boolean can_win_qing_yi_se_with_magic = false;
		boolean can_win_qing_yi_se_without_magic = false;
		if (has_rule(Constants_EZ.GAME_RULE_QING_YI_SE)) {
			can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 清一色
			can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count) && can_win_without_magic; // 硬清一色

			if (can_win_qing_yi_se_with_magic)
				chiHuRight.opr_or(Constants_EZ.CHR_QING_YI_SE);
		}

		boolean is_men_qing = is_men_qing(weaveItems, weave_count);
		if (card_type == Constants_EZ.HU_CARD_TYPE_ZI_MO) {
			if (is_men_qing) {
				chiHuRight.opr_or(Constants_EZ.CHR_MEN_QIAN_QING);
			}
		}

		boolean can_win_peng_peng_hu_with_magic = false;
		boolean can_win_peng_peng_hu_without_magic = false;

		// if (has_rule(Constants_EZ.GAME_RULE_PENG_PENG_HU)) {
		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);

		boolean exist_eat = exist_eat(weaveItems, weave_count);

		can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat; // 碰碰胡
		can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat; // 硬碰碰胡

		if (can_win_peng_peng_hu_with_magic)
			chiHuRight.opr_or(Constants_EZ.CHR_PENG_PENG_HU);
		// }

		if (can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			boolean can_ying_hu = true;

			if (can_win_qi_dui_with_magic && can_win_qi_dui_without_magic == false) // 能胡七对但是不能胡硬七对
				can_ying_hu = false;
			if (can_win_jiang_yi_se_with_magic && can_win_jiang_yi_se_without_magic == false) // 能胡将一色但是不能胡硬将一色
				can_ying_hu = false;
			if (can_win_qing_yi_se_with_magic && can_win_qing_yi_se_without_magic == false) // 能胡清一色但是不能胡硬清一色
				can_ying_hu = false;
			if (can_win_peng_peng_hu_with_magic && can_win_peng_peng_hu_without_magic == false) // 能胡碰碰胡但是不能胡硬碰碰胡
				can_ying_hu = false;

			if (can_ying_hu)
				chiHuRight.opr_or(Constants_EZ.CHR_YING_HU);
			else
				chiHuRight.opr_or(Constants_EZ.CHR_RUAN_HU);
		} else {
			if (can_win_without_magic)
				chiHuRight.opr_or(Constants_EZ.CHR_YING_HU);
			else
				chiHuRight.opr_or(Constants_EZ.CHR_RUAN_HU);
		}

		// TODO: 处理8分起胡
		boolean is_zi_mo = false;
		if (card_type == Constants_EZ.HU_CARD_TYPE_ZI_MO) {
			is_zi_mo = true;
		}

		int tmp_score = caculate_tmp_score(chiHuRight, seat_index, provide_index, is_zi_mo);

		if (card_type == Constants_EZ.HU_CARD_TYPE_JIE_PAO) {
			score_when_jie_pao_hu[seat_index] = get_pai_xing_fen(chiHuRight);
		}

		if (tmp_score < get_min_score()) {
			can_win_but_without_enough_score = true;

			if (analyse_state == NORMAL) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		return cbChiHuKind;
	}

	public int get_min_score() {
		int min_score = 0;

		if (ruleMap.containsKey(Constants_EZ.GAME_RULE_QI_HU_FEN))
			min_score = ruleMap.get(Constants_EZ.GAME_RULE_QI_HU_FEN);

		return min_score;
	}

	public boolean is_men_qing(WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount == 0)
			return true;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG
					|| (weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card != 0)
					|| weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}

		return false;
	}

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 3) {
				return Constants_EZ.CHR_SAN_HAO_HUA_QI_DUI;
			} else if (nGenCount >= 2) {
				return Constants_EZ.CHR_SHH_QI_DUI;
			} else {
				return Constants_EZ.CHR_HAO_HUA_QI_DUI;
			}
		} else {
			return Constants_EZ.CHR_QI_DUI;
		}
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				if (i == get_banker_next_seat(seat_index)) {
					// TODO: 癞子参与吃的时候，算打出了一个癞子杠
					action = _logic.check_chi_ezhou(GRR._cards_index[i], card);

					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			// 全听时，不能接炮
			if (_playerStatus[i].is_chi_hu_round() && !(_playerStatus[i]._hu_card_count == 1 && _playerStatus[i]._hu_cards[0] == -1)) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = Constants_EZ.HU_CARD_TYPE_JIE_PAO;

				analyse_state = NORMAL;
				action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i, seat_index);

				if (action != 0) {
					// 接炮时，牌型分有变动，才能接炮胡
					if (score_when_jie_pao_hu[i] > score_when_abandoned_jie_pao[i]) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				} else {
					// TODO 表示牌型能胡但是没达到起胡分
					if (can_win_but_without_enough_score) {
						operate_cant_win_info(i);
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (getTablePlayerNumber() + seat - 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	protected int get_seat(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) { // 四人场
			seat = (seat_index + (nValue - 1) % 4) % 4;
		} else { // 三人场，所有胡牌人的对家都是那个空位置
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_banker_next_seat(seat_index);
				break;
			case 2:
				seat = get_null_seat();
				break;
			case 3:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = NORMAL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_EZ.HU_CARD_TYPE_ZI_MO, seat_index, seat_index))
				return true;
		}

		return false;
	}

	public boolean exe_select_magic_card(int seat_index) {
		set_handler(_handler_select_magic);
		_handler_select_magic.reset_status(seat_index);
		_handler_select_magic.exe(this);
		return true;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_HZ) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_HZ && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HZ;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public int get_ting_card(int[] cards, int[] cards_hu, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants.MAX_ZI + 3;

		for (int i = 0; i < max_ting_count; i++) {
			if (i >= GameConstants.MAX_ZI && i <= GameConstants.MAX_ZI + 3)
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_EZ.HU_CARD_TYPE_ZI_MO, seat_index, seat_index)) {
				cards[count] = cbCurrentCard;

				cards_hu[count] = get_chr_fan_shu(chr);

				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count - 1) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_EZ();
		_handler_dispath_card = new HandlerDispatchCard_EZ();
		_handler_gang = new HandlerGang_EZ();
		_handler_out_card_operate = new HandlerOutCardOperate_EZ();
		_handler_select_magic = new HandlerSelectMagic_EZ();
	}

	public boolean operate_cant_win_info(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CAN_WIN_BUT_WITHOUT_ENOUGH_SCORE);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(true); // TODO 表示牌型能胡但是没达到起胡分

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_auto_win_card(int seat_index, boolean isTurnOn) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SWITCH_AUTO_WIN_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(isTurnOn); // TODO false 表示隐藏 true 表示显示

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean estimate_gang_da_kao(int[] cards_index, int card_data) {
		if (exist_dan_zhang(cards_index, card_data) == false)
			return false;

		int card_color = _logic.get_card_color(card_data);
		if (card_color == 3)
			return false;

		int card_value = _logic.get_card_value(card_data);
		int card_index = _logic.switch_to_card_index(card_data);

		if (card_value == 1) {
			if (cards_index[card_index + 1] > 0 || cards_index[card_index + 2] > 0)
				return true;
		} else if (card_value == 2) {
			if (cards_index[card_index - 1] > 0 || cards_index[card_index + 1] > 0 || cards_index[card_index + 2] > 0)
				return true;
		} else if (card_value >= 3 && card_value <= 7) {
			if (cards_index[card_index - 2] > 0 || cards_index[card_index - 1] > 0 || cards_index[card_index + 1] > 0
					|| cards_index[card_index + 2] > 0)
				return true;
		} else if (card_value == 8) {
			if (cards_index[card_index - 2] > 0 || cards_index[card_index - 1] > 0 || cards_index[card_index + 1] > 0)
				return true;
		} else if (card_value == 9) {
			if (cards_index[card_index - 2] > 0 || cards_index[card_index - 1] > 0)
				return true;
		}

		return false;
	}

	@Override
	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(getRoom_id(), seat_index, action, card), auto_deal_win_delay, TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	public boolean handler_request_trustee(int seat_index, boolean isTrustee, int trustee_type) {
		if (!is_match() && !isClubMatch() && !isCoinRoom()) {
			if (_playerStatus == null || istrustee == null) {
				send_error_notify(seat_index, 2, "游戏未开始,无法进行自动胡牌托管!");
				return false;
			}

			if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
				send_error_notify(seat_index, 2, "游戏还未开始,无法进行自动胡牌托管!");
				return false;
			}

			if (istrustee[seat_index] == false) {
				int card_count = _logic.get_card_count_by_index(GRR._cards_index[seat_index]);
				if (card_count % 3 != 1) {
					send_error_notify(seat_index, 2, "您还未听牌,无法进行自动胡牌托管!");
					return false;
				} else if (!is_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index)) {
					send_error_notify(seat_index, 2, "您还未听牌,无法进行自动胡牌托管!");
					return false;
				}
			}
		}

		istrustee[seat_index] = isTrustee;

		// 这个取消托管的操作，由客户端主动发起，如果玩家是托管状态，并点了操作，取消定时任务
		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(isTrustee);

		send_response_to_player(seat_index, roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, seat_index);
		}

		return true;

	}

	/**
	 * 玩家在一种特殊情况下，需要主动取消托管
	 * 
	 * @param seat_index
	 * @param isTrustee
	 */
	public void cancel_trustee(int seat_index, boolean isTrustee) {
		istrustee[seat_index] = isTrustee;

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(isTrustee);

		send_response_to_player(seat_index, roomResponse);
	}

	public boolean check_more_than_5_pair(int[] cards_index, int card_data, int weaveCount) {
		if (weaveCount > 1)
			return false;

		// 注意，癞子牌可以参与成对判断
		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		tmp_cards_index[_logic.switch_to_card_index(card_data)]++;

		int magic_count = _logic.magic_count(tmp_cards_index);

		int dan_pai_count = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (i == magic_card_index)
				continue;

			if (tmp_cards_index[i] == 1) {
				dan_pai_count++;
			} else if (tmp_cards_index[i] == 2) {
				count += 1;
			} else if (tmp_cards_index[i] == 3) {
				count += 1;
				dan_pai_count++;
			} else if (tmp_cards_index[i] == 4) {
				count += 2;
			}
		}

		if (magic_count - dan_pai_count > 0) {
			count += (dan_pai_count) + (magic_count - dan_pai_count) / 2;
		} else {
			count += (magic_count);
		}

		return count >= 5;
	}

	public int get_diao_zhang(int[] cards_index, int card_data, int[] cards) {
		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		tmp_cards_index[_logic.switch_to_card_index(card_data)]++;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (tmp_cards_index[i] > 1)
				continue;

			if (tmp_cards_index[i] > 0) {
				int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));

				if (card_value == 1) {
					if (tmp_cards_index[i + 1] == 0 && tmp_cards_index[i + 2] == 0) {
						cards[count++] = _logic.switch_to_card_data(i);
					}
				} else if (card_value == 9) {
					if (tmp_cards_index[i - 2] == 0 && tmp_cards_index[i - 1] == 0) {
						cards[count++] = _logic.switch_to_card_data(i);
					}
				} else if (card_value == 2) {
					if (tmp_cards_index[i - 1] == 0 && tmp_cards_index[i + 1] == 0 && tmp_cards_index[i + 2] == 0) {
						cards[count++] = _logic.switch_to_card_data(i);
					}
				} else if (card_value == 8) {
					if (tmp_cards_index[i - 2] == 0 && tmp_cards_index[i - 1] == 0 && tmp_cards_index[i + 1] == 0) {
						cards[count++] = _logic.switch_to_card_data(i);
					}
				} else if (card_value > 2 && card_value < 8) {
					if (tmp_cards_index[i - 2] == 0 && tmp_cards_index[i - 1] == 0 && tmp_cards_index[i + 1] == 0 && tmp_cards_index[i + 2] == 0) {
						cards[count++] = _logic.switch_to_card_data(i);
					}
				}
			}
		}

		// 发财白板的吊张判断
		for (int i = Constants_EZ.HZ_INDEX + 1; i <= Constants_EZ.HZ_INDEX + 2; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (tmp_cards_index[i] > 1)
				continue;

			if (tmp_cards_index[i] == 1) {
				cards[count++] = _logic.switch_to_card_data(i);
			}
		}

		return count;
	}

	public int get_bian_zhang(int[] cards_index, int card_data, int[] cards) {
		// 单色牌的两头两尾的除了牌数大于1的几张牌
		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		tmp_cards_index[_logic.switch_to_card_index(card_data)]++;

		int first_index = -1;
		int last_index = -1;
		// 万
		for (int i = 0; i < 9; i++) {
			if (i == magic_card_index)
				continue;

			if (tmp_cards_index[i] == 1) {
				first_index = i;
				break;
			}
		}
		if (first_index != -1) {
			for (int i = 8; i >= 0; i--) {
				if (i == magic_card_index)
					continue;

				if (tmp_cards_index[i] == 1) {
					last_index = i;
					break;
				}
			}
		}
		if (first_index != -1 && last_index != -1 && first_index != last_index) {
			cards[count++] = _logic.switch_to_card_data(first_index);
			cards[count++] = _logic.switch_to_card_data(last_index);
		}

		first_index = -1;
		last_index = -1;
		// 条
		for (int i = 9; i < 18; i++) {
			if (i == magic_card_index)
				continue;

			if (tmp_cards_index[i] == 1) {
				first_index = i;
				break;
			}
		}
		if (first_index != -1) {
			for (int i = 17; i >= 9; i--) {
				if (i == magic_card_index)
					continue;

				if (tmp_cards_index[i] == 1) {
					last_index = i;
					break;
				}
			}
		}
		if (first_index != -1 && last_index != -1 && first_index != last_index) {
			cards[count++] = _logic.switch_to_card_data(first_index);
			cards[count++] = _logic.switch_to_card_data(last_index);
		}

		first_index = -1;
		last_index = -1;
		// 筒
		for (int i = 18; i < 27; i++) {
			if (i == magic_card_index)
				continue;

			if (tmp_cards_index[i] == 1) {
				first_index = i;
				break;
			}
		}
		if (first_index != -1) {
			for (int i = 26; i >= 18; i--) {
				if (i == magic_card_index)
					continue;

				if (tmp_cards_index[i] == 1) {
					last_index = i;
					break;
				}
			}
		}
		if (first_index != -1 && last_index != -1 && first_index != last_index) {
			cards[count++] = _logic.switch_to_card_data(first_index);
			cards[count++] = _logic.switch_to_card_data(last_index);
		}

		return count;
	}

	public boolean exist_dan_zhang(int[] cards_index, int card_data) {
		boolean exist = false;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		tmp_cards_index[_logic.switch_to_card_index(card_data)]++;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (tmp_cards_index[i] > 1)
				continue;

			if (tmp_cards_index[i] > 0) {
				int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));

				if (card_value == 1) {
					if (tmp_cards_index[i + 1] == 0 && tmp_cards_index[i + 2] == 0) {
						exist = true;
						break;
					}
				} else if (card_value == 9) {
					if (tmp_cards_index[i - 2] == 0 && tmp_cards_index[i - 1] == 0) {
						exist = true;
						break;
					}
				} else if (card_value == 2) {
					if (tmp_cards_index[i - 1] == 0 && tmp_cards_index[i + 1] == 0 && tmp_cards_index[i + 2] == 0) {
						exist = true;
						break;
					}
				} else if (card_value == 8) {
					if (tmp_cards_index[i - 2] == 0 && tmp_cards_index[i - 1] == 0 && tmp_cards_index[i + 1] == 0) {
						exist = true;
						break;
					}
				} else if (card_value > 2 && card_value < 8) {
					if (tmp_cards_index[i - 2] == 0 && tmp_cards_index[i - 1] == 0 && tmp_cards_index[i + 1] == 0 && tmp_cards_index[i + 2] == 0) {
						exist = true;
						break;
					}
				}
			}
		}

		return exist;
	}

	@Override
	protected boolean on_handler_game_start() {
		tou_zi_dian_shu = new int[2];

		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		// TODO 后期可能会使用新的洗牌算法，随抓牌动画，摇骰子一起
		init_shuffle();

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l, getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			logger.error("card_log", e);
		}

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	@Override
	protected void init_shuffle() {
		_repertory_card = new int[mjType.getCardLength()];
		shuffle(_repertory_card, mjType.getCards());
	};

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		// TODO 洗完牌之后，发牌之前，需要摇骰子
		qi_pai_index = 0;
		show_tou_zi(GRR._banker_player);

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	@Override
	protected boolean on_game_start() {
		_logic.clean_magic_cards();

		// TODO: nao字段用来存储是否亮癞子，0表示没有，1表示亮了癞子
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}

		can_win_pi_hu = new boolean[getTablePlayerNumber()];
		can_ruan_hu = new boolean[getTablePlayerNumber()];
		can_only_zi_mo = new boolean[getTablePlayerNumber()];
		da_dian_card = 0;
		magic_card_index = -1;
		hong_zhong_gang_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			can_ruan_hu[i] = true;
		}

		effective_weave_count = new int[getTablePlayerNumber()];
		player_multiple_count = new int[getTablePlayerNumber()];
		start_compensation_judge = false;

		left_card_count_after_gang = 0;

		score_when_jie_pao_hu = new int[getTablePlayerNumber()];
		score_when_abandoned_jie_pao = new int[getTablePlayerNumber()];

		// TODO: 用之前的鸟牌数据来存储杠番的牌和统计
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_LAI_ZI_PI_ZI_GANG; j++) {
				GRR._lai_zi_pi_zi_gang[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		gang_status = false;
		gang_da_kao = false;
		gang_pai_player = GameConstants.INVALID_SEAT;

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		exe_select_magic_card(GRR._banker_player);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);
			be_in_room_trustee_match(seat_index);

		} else {
			if (null != istrustee) {
				istrustee[seat_index] = false;
				operate_auto_win_card(seat_index, false);
			}
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;

		return true;
	}

	public int caculate_tmp_score(ChiHuRight chr, int seat_index, int provide_index, boolean zimo) {
		int score = 0;

		int di_fen = get_di_fen();
		int pai_xing_fen = get_pai_xing_fen(chr);

		di_fen *= pai_xing_fen;

		int fan_shu = 0;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_EZ.CHR_ZI_MO).is_empty())
					if (check_big_win(chr))
						fan_shu += 1;
				if (!chr.opr_and(Constants_EZ.CHR_YING_HU).is_empty())
					fan_shu += 1;

				if (check_big_win(chr) == false) {
					if (!chr.opr_and(Constants_EZ.CHR_MEN_QIAN_QING).is_empty()) {
						if (!chr.opr_and(Constants_EZ.CHR_YING_HU).is_empty())
							fan_shu += 2;
						else
							fan_shu += 1;
					}
				}

				fan_shu += get_gang_fan_shu(i, seat_index);

				if (effective_weave_count[seat_index] >= 3)
					fan_shu += 1;
				if (effective_weave_count[i] >= 3)
					fan_shu += 1;

				for (int x = 0; x < GRR._player_niao_count[i]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[i][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}
				for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				score += di_fen * real_fan_shu;

				fan_shu = tmp_fan_shu;
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_EZ.CHR_YING_HU).is_empty())
					fan_shu += 1;

				fan_shu += get_gang_fan_shu(i, seat_index);

				if (effective_weave_count[seat_index] >= 3)
					fan_shu += 1;
				if (effective_weave_count[i] >= 3)
					fan_shu += 1;

				for (int x = 0; x < GRR._player_niao_count[i]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[i][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}
				for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				int s = di_fen;

				if (i == provide_index) {
					if (check_big_win(chr)) {
						s += pai_xing_fen / 2;
					} else {
						s *= 2;
					}
				}

				s *= real_fan_shu;

				score += s;

				fan_shu = tmp_fan_shu;
			}
		}

		return score;
	}

	public int get_pai_xing_fen(ChiHuRight chr) {
		int fen = 1;

		if (check_big_win(chr) == false) { // 小胡
			if (!chr.opr_and(Constants_EZ.CHR_ZI_MO).is_empty()) { // 自摸
				fen = 3;
			} else { // 接炮
				fen = 1;
			}
		} else { // 大胡
			if (!chr.opr_and(Constants_EZ.CHR_JIANG_YI_SE).is_empty()) // 将一色
				fen = 6;
			if (!chr.opr_and(Constants_EZ.CHR_QING_YI_SE).is_empty()) // 清一色
				fen = 6;
			if (!chr.opr_and(Constants_EZ.CHR_PENG_PENG_HU).is_empty()) // 碰碰胡
				fen = 6;
			if (!chr.opr_and(Constants_EZ.CHR_QI_DUI).is_empty()) // 七对
				fen = 6;
			if (!chr.opr_and(Constants_EZ.CHR_HAO_HUA_QI_DUI).is_empty()) // 豪华七对
				fen = 12;
			if (!chr.opr_and(Constants_EZ.CHR_SHH_QI_DUI).is_empty()) // 双豪华七对
				fen = 24;
			if (!chr.opr_and(Constants_EZ.CHR_SAN_HAO_HUA_QI_DUI).is_empty()) // 三豪华七对
				fen = 48;
		}

		return fen;
	}

	public boolean check_big_win(ChiHuRight chr) {
		boolean has_big_win = false;

		if (!chr.opr_and(Constants_EZ.CHR_PENG_PENG_HU).is_empty() || !chr.opr_and(Constants_EZ.CHR_QI_DUI).is_empty()
				|| !chr.opr_and(Constants_EZ.CHR_HAO_HUA_QI_DUI).is_empty() || !chr.opr_and(Constants_EZ.CHR_SHH_QI_DUI).is_empty()
				|| !chr.opr_and(Constants_EZ.CHR_QING_YI_SE).is_empty() || !chr.opr_and(Constants_EZ.CHR_JIANG_YI_SE).is_empty()
				|| !chr.opr_and(Constants_EZ.CHR_SAN_HAO_HUA_QI_DUI).is_empty())
			has_big_win = true;

		return has_big_win;
	}

	public int get_max_fen() {
		int max_fen = 0;

		if (ruleMap.containsKey(Constants_EZ.GAME_RULE_FENG_DING))
			max_fen = ruleMap.get(Constants_EZ.GAME_RULE_FENG_DING);

		if (max_fen == 0)
			max_fen = Integer.MAX_VALUE;

		return max_fen;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = get_di_fen();
		int pai_xing_fen = get_pai_xing_fen(chr);

		int fan_shu = 0;
		int max_fen = get_max_fen();

		countCardType(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (zimo) {
			winner_pai_xing_fen = pai_xing_fen;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_EZ.CHR_ZI_MO).is_empty()) {
					// 大胡自摸时
					if (check_big_win(chr) == true) {
						fan_shu += 1;
					}
				}
				if (!chr.opr_and(Constants_EZ.CHR_YING_HU).is_empty()) {
					// 硬胡
					fan_shu += 1;
				}

				if (check_big_win(chr) == false) {
					// 小胡自摸时
					if (!chr.opr_and(Constants_EZ.CHR_MEN_QIAN_QING).is_empty()) {
						fan_shu += 1;
					}
				} else if (!chr.opr_and(Constants_EZ.CHR_PENG_PENG_HU).is_empty()) {
					// 碰碰胡自摸时，如果有门清
					if (!chr.opr_and(Constants_EZ.CHR_MEN_QIAN_QING).is_empty()) {
						fan_shu += 1;
					}
				}

				fan_shu += get_gang_fan_shu(i, seat_index);

				if (effective_weave_count[seat_index] >= 3)
					fan_shu += 1;
				if (effective_weave_count[i] >= 3)
					fan_shu += 1;

				for (int x = 0; x < GRR._player_niao_count[i]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[i][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}
				for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				int s = di_fen * pai_xing_fen * real_fan_shu;

				if (s > max_fen)
					s = max_fen;

				fan_shu = tmp_fan_shu;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_EZ.CHR_YING_HU).is_empty()) {
					// 硬胡
					fan_shu += 1;
				}

				fan_shu += get_gang_fan_shu(i, seat_index);

				if (effective_weave_count[seat_index] >= 3)
					fan_shu += 1;
				if (effective_weave_count[i] >= 3)
					fan_shu += 1;

				for (int x = 0; x < GRR._player_niao_count[i]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[i][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}
				for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
					if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
						fan_shu += 2;
					else
						fan_shu += 1;
				}

				int tmp_pai_xing_fen = pai_xing_fen;

				if (!GRR._chi_hu_rights[i].opr_and(Constants_EZ.CHR_FANG_PAO).is_empty()) {
					// 放炮者小胡时，牌型分加一倍
					if (check_big_win(chr) == false) {
						tmp_pai_xing_fen = tmp_pai_xing_fen + tmp_pai_xing_fen;
					} else {
						// 大胡时，碰碰胡，牌型分加一倍
						if (!chr.opr_and(Constants_EZ.CHR_PENG_PENG_HU).is_empty()) {
							tmp_pai_xing_fen = tmp_pai_xing_fen + tmp_pai_xing_fen;
						} else {
							// 七对、豪华七对、双豪华七对、三豪华七对时，牌型分加一半
							tmp_pai_xing_fen = tmp_pai_xing_fen + (tmp_pai_xing_fen / 2);
						}
					}

					winner_pai_xing_fen = tmp_pai_xing_fen;
				}

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				int s = di_fen * tmp_pai_xing_fen * real_fan_shu;

				if (s > max_fen)
					s = max_fen;

				fan_shu = tmp_fan_shu;

				if (start_compensation_judge && gang_da_kao) { // 有人打靠张，包赔
					GRR._game_score[provide_index] -= s;
					GRR._game_score[seat_index] += s;
				} else {
					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public int get_chr_fan_shu(ChiHuRight chr) {
		int fan_shu = 0;

		if (!chr.opr_and(Constants_EZ.CHR_ZI_MO).is_empty()) {
			// 大胡自摸时
			if (check_big_win(chr) == true) {
				fan_shu += 1;
			}
		}
		if (!chr.opr_and(Constants_EZ.CHR_YING_HU).is_empty()) {
			// 硬胡
			fan_shu += 1;
		}

		if (check_big_win(chr) == false) {
			// 小胡自摸时
			if (!chr.opr_and(Constants_EZ.CHR_MEN_QIAN_QING).is_empty()) {
				fan_shu += 1;
			}
		} else if (!chr.opr_and(Constants_EZ.CHR_PENG_PENG_HU).is_empty()) {
			// 碰碰胡自摸时，如果有门清
			if (!chr.opr_and(Constants_EZ.CHR_MEN_QIAN_QING).is_empty()) {
				fan_shu += 1;
			}
		}

		return fan_shu;
	}

	public int get_gang_fan_shu(int lost_player, int win_player) {
		int gang_fan_shu = 0;
		gang_fan_shu += (GRR._gang_score[lost_player].an_gang_count + GRR._gang_score[win_player].an_gang_count) * 2;
		gang_fan_shu += GRR._gang_score[lost_player].ming_gang_count + GRR._gang_score[win_player].ming_gang_count;

		return gang_fan_shu;
	}

	public int get_di_fen() {
		int di_fen = 1;

		if (ruleMap.containsKey(Constants_EZ.GAME_RULE_DI_FEN))
			di_fen = ruleMap.get(Constants_EZ.GAME_RULE_DI_FEN);

		return di_fen;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean is_ying_hu = false;
			boolean has_big_win = false;
			boolean has_zi_mo = false;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_EZ.CHR_ZI_MO) {
						result.append(" 自摸");
						has_zi_mo = true;
					}
					if (type == Constants_EZ.CHR_JIE_PAO) {
						result.append(" 接炮");
						if (start_compensation_judge && gang_da_kao) {
							result.append(" 包赔");
						}
					}
					if (type == Constants_EZ.CHR_QING_YI_SE) {
						result.append(" 清一色");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_JIANG_YI_SE) {
						result.append(" 将一色");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_QI_DUI) {
						result.append(" 七对");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_HAO_HUA_QI_DUI) {
						result.append(" 豪华七对");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_SHH_QI_DUI) {
						result.append(" 双豪华七对");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_SAN_HAO_HUA_QI_DUI) {
						result.append(" 三豪华七对");
						has_big_win = true;
					}
					if (type == Constants_EZ.CHR_YING_HU) {
						result.append(" 硬胡");
						is_ying_hu = true;
					}
				} else if (type == Constants_EZ.CHR_FANG_PAO) {
					result.append(" 放炮");
					// if (gang_status == true && gang_da_kao == true) {
					// result.append(" 杠后打靠张");
					// }
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (has_zi_mo == true && has_big_win == false && effective_weave_count[player] == 0) {
					result.append(" 门前清");
				} else if (has_zi_mo == true && effective_weave_count[player] == 0
						&& !GRR._chi_hu_rights[player].opr_and(Constants_EZ.CHR_PENG_PENG_HU).is_empty()) {
					result.append(" 门前清");
				}
			}

			if (effective_weave_count[player] >= 3)
				result.append(" 三枪");

			if (GRR._chi_hu_rights[player].is_valid() && is_ying_hu == false)
				result.append(" 软胡");

			if (GRR._gang_score[player].an_gang_count > 0) {
				result.append(" 暗杠" + GRR._gang_score[player].an_gang_count * 2 + "番");
			}
			if (GRR._gang_score[player].ming_gang_count > 0) {
				result.append(" 明杠" + GRR._gang_score[player].ming_gang_count + "番");
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x02, 0x08, 0x09, 0x14, 0x15, 0x16, 0x17, 0x17, 0x17, 0x23, 0x23, 0x36 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x06, 0x06, 0x06, 0x15, 0x16, 0x17, 0x21, 0x21, 0x24, 0x25 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x06, 0x06, 0x06, 0x15, 0x16, 0x17, 0x21, 0x21, 0x24, 0x25 };
		int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x06, 0x06, 0x06, 0x15, 0x16, 0x17, 0x21, 0x21, 0x24, 0x25 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps);
					debug_my_cards = null;
				}
			}
		}
	}

	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		_repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}

		int seat_index = 0;

		if (player != null) {
			seat_index = player.get_seat_index();
		}

		int game_id = getGame_id();
		int delay = 60;

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(3007);

		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT),
						delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT),
						delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			int count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == getTablePlayerNumber()) {
				if (GRR == null) {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					player = get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}
			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					Player p = get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
			} else {
				return false;
			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), player.getAccount_id());
			}

			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			load_player_info_data(refreshroomResponse);
			send_response_to_other(seat_index, refreshroomResponse);

			refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
			send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());

		}
			break;
		}

		return true;

	}

	@Override
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);

			if (is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
				roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[0][i]);
			}
		}

		send_response_to_player(seat_index, roomResponse);
		return true;
	}
}
