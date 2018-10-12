package com.cai.game.mj.chenchuang.xianning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.mj.GameConstants_MJ_XIAN_NING;
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

public class Table_XianNing extends AbstractMJTable {
	private static final long serialVersionUID = -8673836743823288270L;

	public HandlerSelectMagic_XianNing _handler_select_magic;

	public int[] tou_zi_dian_shu = new int[2]; // 用来储存2个骰子的点数

	public int time_for_animation = 2000; // 摇骰子的动画时间(ms)
	public int time_for_fade = 500; // 摇骰子动画的消散延时(ms)

	public int qi_pai_index = 0; // 起牌的索引位置，一共有4个牌堆

	public boolean can_win_but_without_enough_score = false; // 能胡，但是没达到起胡分

	// 牌桌上，每局游戏，所有玩家的番数记录
	public int[] player_multiple_count = new int[getTablePlayerNumber()];
	// 玩家癞子杠数量
	public int[] player_lai_zi_gang_count;

	// 记录每个玩家第三次开口的提供者（玩家位置索引+1，为0表示没有），用来处理包赔（胡牌者必须有清一色或者将一色的牌型）
	public int[] player_kai_kou3;
	// 记录每个玩家开口的次数
	public int[] player_kai_kou;

	// 抢杠失败状态
	public boolean qiang_fail[] = new boolean[getTablePlayerNumber()];
	// 存在包赔的玩家
	public int bao_pei_i;

	public boolean is_si_lai;

	// 接炮时，记录一下牌型分
	public int[][] score_when_jie_pao_hu;
	// 有接炮时，不胡，再次存储一下，胡牌时的牌型分，和score_when_abandoned_win进行比较
	public int[][] score_when_abandoned_jie_pao; // 可以胡没有胡记录可以胡的牌型翻数

	public int auto_out_card_delay = 500; // 自动出牌的延迟时间
	public int action_wait_time = 3000; // 自动胡牌托管之后，如果有操作，等待3秒，3秒之后自动出牌
	public int auto_deal_win_delay = 500; // 自动胡牌的延迟时间

	public Table_XianNing() {
		super(MJType.GAME_TYPE_MJ_XIAN_NING);
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

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], time_for_animation, time_for_fade);
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0)
			return playerNumber;
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1)
			return 2;
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1)
			return 3;
		return 4;
	}

	public int entrySeabedCount() {
		if (getTablePlayerNumber() == 2)
			return 10;
		if (getTablePlayerNumber() == 3)
			return 11;
		return 12;
	}

	public float getBottomScore() {
		if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_BOTTOM_SCORE_2))
			return 2;
		if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_BOTTOM_SCORE_5))
			return 5;
		if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_BOTTOM_SCORE_HALF))
			return 0.5f;
		return 1;
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

	@Override
	protected void init_shuffle() {
		int[] card = GameConstants_MJ_XIAN_NING.CARD_DATA_MJ_XIAN_NING;
		if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_TWO_DOOR_CARD))
			card = GameConstants_MJ_XIAN_NING.CARD_DATA_MJ_XIAN_NING_2;
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
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
				if (!GRR._chi_hu_rights[i].opr_and(GameConstants_MJ_XIAN_NING.CHR_JIE_PAO).is_empty()) {
					room_player.setNao(1);
				} else if (!GRR._chi_hu_rights[i].opr_and(GameConstants_MJ_XIAN_NING.CHR_FANG_PAO).is_empty()) {
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			// room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setBiaoyan(get_gang_fen(i));

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
		roomResponse.setScoreType(get_gang_fen(seat_index));
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
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
			// roomResponse.addOutCardTing(this.GRR._cards_index[seat_index][this._logic.switch_to_card_index(cards[i])]);
			roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[0][i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setScoreType(get_gang_fen(seat_index));
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
				roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public void process_chi_hu_player_operate1(int seat_index, int operate_card) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = 1;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i]))
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;// 胡牌肯定没有红中
		}

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
				else if (cards[i] == Constants_EZ.HZ_CARD) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
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
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;// 胡牌肯定没有红中
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
				else if (cards[i] == Constants_EZ.HZ_CARD) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
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
		// game_end.setHuXi(winner_pai_xing_fen);

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
			int cards[] = new int[GRR._left_card_count];// 显示剩余的牌数据
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

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

		player_multiple_count = new int[getTablePlayerNumber()];
		operate_player_info();

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
		// 抢暗杠失败后不能再胡牌
		if (cur_card == 0 || qiang_fail[seat_index]) {
			return GameConstants.WIK_NULL;
		}

		// 复制手牌
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		// 手里有红中不能胡牌
		if (cbCardIndexTemp[Constants_EZ.HZ_INDEX] > 0)
			return GameConstants.WIK_NULL;

		// 没有开口不能胡
		boolean is_kai_kou = false;
		if (weave_count > 0) {
			for (int i = 0; i < weave_count; i++) {
				if (weaveItems[i].public_card != 0) {
					is_kai_kou = true;
					break;
				}
			}
		}
		if (!is_kai_kou)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		// 得到鬼牌和数量
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();
		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 胡牌的方式
		if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_ZI_MO);// 自摸
		} else if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_JIE_PAO);// 接炮
		} else if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG);// 抢杠
		} else if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_GANG_KAI_HUA);// 杠上开花
		}

		// 分析胡牌，带癞子和不带癞子
		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);
		boolean analyse_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean analyse_258_ying = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				0);

		// if (can_win_without_magic)
		// chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_YING_HU);
		// 分析将一色，带癞子和不带癞子
		boolean can_win_jiang_yi_se_with_magic = is_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count, false); // 将一色，不用一对牌+3n，也可以胡牌
		boolean can_win_jiang_yi_se_without_magic = is_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count, true);
		if (can_win_jiang_yi_se_with_magic)
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE);

		// 不能胡牌
		if (!(can_win_with_magic || can_win_jiang_yi_se_with_magic)) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		// 能胡牌
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		// 硬清一色，带癞子和不带癞子
		boolean is_qing_yi_se = check_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, false);
		boolean is_ying_qing_yi_se = check_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, true);
		if (is_qing_yi_se)
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE);

		// 判断手牌是不是碰碰胡
		boolean exist_eat = exist_eat(weaveItems, weave_count);
		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean can_ying_peng_hu = !exist_eat
				&& AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index, 0);
		boolean is_peng_peng_hu = can_peng_hu && !exist_eat;
		if (is_peng_peng_hu)
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_PENG_PENG_HU);

		// 判断全求人
		boolean is_quan_qiu_ren = false;
		int tmp_card_count = _logic.get_card_count_by_index(cbCardIndexTemp);
		if (tmp_card_count == 2 && card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_JIE_PAO) {
			if (analyse_258)
				is_quan_qiu_ren = true;
		}
		if (is_quan_qiu_ren)
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN);

		if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG
				&& (can_ying_peng_hu || analyse_258_ying || is_ying_qing_yi_se || can_win_jiang_yi_se_without_magic))
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_YING_HU);

		if (can_win_jiang_yi_se_without_magic || (is_ying_qing_yi_se && can_win_without_magic) || can_ying_peng_hu)
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_YING_HU);
		else if (!(can_win_jiang_yi_se_with_magic || is_qing_yi_se || is_peng_peng_hu) && analyse_258_ying)
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_YING_HU);

		if (!is_big_hu(chiHuRight)) {// 屁胡和全求人必须258做将
			if (!analyse_258)
				return GameConstants.WIK_NULL;
		}
		boolean is_hai_di = GRR._left_card_count < entrySeabedCount()
				|| (GRR._left_card_count == entrySeabedCount() && (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_ZI_MO));
		if (is_hai_di) {
			chiHuRight.opr_or(GameConstants_MJ_XIAN_NING.CHR_HAI_DI_LAO);
		}

		// 当手牌上有两个或以上赖子时不能胡屁胡，但可以胡大胡
		if (cbCardIndexTemp[_logic.get_magic_card_index(0)] >= 2) {
			if (!is_big_hu(chiHuRight) && card_type != GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_GANG_KAI && !is_hai_di
					&& card_type != GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		// 抢杠胡不需要翻数达到要求
		if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG) {
			return cbChiHuKind;
		}

		// 处理8分起胡
		boolean is_zi_mo = card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_ZI_MO || card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_GANG_KAI
				? true
				: false;
		int tmp_score[] = analyse_tmp_score(chiHuRight, seat_index, provide_index, is_zi_mo, cbCardIndexTemp, cur_card);
		if (card_type == GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_JIE_PAO) {
			score_when_jie_pao_hu[seat_index][0] = tmp_score[1];
			score_when_jie_pao_hu[seat_index][1] = tmp_score[2];
			score_when_jie_pao_hu[seat_index][2] = tmp_score[3];
		}

		if (tmp_score[0] < 8) {
			can_win_but_without_enough_score = true;
			chiHuRight.set_empty();
		}
		// return GameConstants.WIK_NULL;

		return cbChiHuKind;
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

			if (GRR._left_card_count >= entrySeabedCount()) {
				if (i == get_banker_next_seat(seat_index)) {
					action = _logic.check_chi(GRR._cards_index[i], card);

					if ((action & GameConstants.WIK_RIGHT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CENTER);
						_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_LEFT) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_LEFT);
						_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);// 碰
				if (action != 0 && can_peng) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > entrySeabedCount()) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			// 接炮时，牌型分大，才能接炮胡
			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];

			int card_type = GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_JIE_PAO;

			action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i, seat_index);

			boolean is_can_hu = false;
			if (score_when_jie_pao_hu[i][0] > score_when_abandoned_jie_pao[i][0])
				is_can_hu = true;
			else if (score_when_jie_pao_hu[i][0] == score_when_abandoned_jie_pao[i][0]) {
				if (score_when_jie_pao_hu[i][1] > score_when_abandoned_jie_pao[i][1])
					is_can_hu = true;
				else if (score_when_jie_pao_hu[i][1] == score_when_abandoned_jie_pao[i][1]) {
					if (score_when_jie_pao_hu[i][2] > score_when_abandoned_jie_pao[i][2])
						is_can_hu = true;
				}
			}
			if (_playerStatus[i].is_chi_hu_round() || is_can_hu) {
				if (action != 0 && !can_win_but_without_enough_score) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
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
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_HZ
				&& card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING + GameConstants.CARD_ESPECIAL_TYPE_HZ) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_HZ;
		} else if (card > 20000 && card < 20200) {
			card -= 20000;
		}

		return card;
	}

	public int get_ting_card(int[] cards, int[] cards_hu, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_TWO_DOOR_CARD)) {
				if (i > 8 && i < 18)
					continue;
			}
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_EZ.HU_CARD_TYPE_ZI_MO, seat_index, seat_index)) {
				cards[count] = cbCurrentCard;
				cards_hu[count] = get_ting_pai_fan(chr);
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			}
		}

		if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_TWO_DOOR_CARD)) {
			max_ting_count = 18;
		}
		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_ting_card_qiang_gang(int[] cards, int[] cards_hu, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_TWO_DOOR_CARD)) {
				if (i > 8 && i < 18)
					continue;
			}
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG, seat_index, seat_index)) {
				cards[count] = cbCurrentCard;
				cards_hu[count] = get_ting_pai_fan(chr);
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				count++;
			}
		}

		if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_TWO_DOOR_CARD)) {
			max_ting_count = 18;
		}
		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_XianNing();
		_handler_dispath_card = new HandlerDispatchCard_XianNing();
		_handler_gang = new HandlerGang_XianNing();
		_handler_out_card_operate = new HandlerOutCardOperate_XianNing();
		_handler_select_magic = new HandlerSelectMagic_XianNing();
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

	@Override
	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(getRoom_id(), seat_index, action, card), auto_deal_win_delay, TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	public boolean handler_request_trustee(int seat_index, boolean isTrustee, int trustee_type) {
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

	/**
	 * 所有玩家都准备了会走这里
	 */
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

	/**
	 * 开局第1局随机选庄
	 */
	@Override
	protected void initBanker() {
		_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());
	}

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

		player_multiple_count = new int[getTablePlayerNumber()];
		player_lai_zi_gang_count = new int[getTablePlayerNumber()];
		player_kai_kou3 = new int[getTablePlayerNumber()];
		player_kai_kou = new int[getTablePlayerNumber()];
		qiang_fail = new boolean[getTablePlayerNumber()];
		bao_pei_i = -1;
		is_si_lai = false;

		score_when_jie_pao_hu = new int[getTablePlayerNumber()][3];
		score_when_abandoned_jie_pao = new int[getTablePlayerNumber()][3];

		// TODO: 用之前的鸟牌数据来存储杠番的牌和统计
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_LAI_ZI_PI_ZI_GANG; j++) {
				GRR._lai_zi_pi_zi_gang[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 把红中杠和癞子放到最左边
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, _playerStatus[i]._hu_out_cards_fan[0], GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);

				// 显示自动胡牌按钮
				// operate_auto_win_card(i, true);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);

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

	/** 分析胡牌的总分 */
	public int[] analyse_tmp_score(ChiHuRight chr, int seat_index, int provide_index, boolean zimo, int[] cards, int operate_card) {
		int[] scores = { 0, 0, 0, 0 };// 与单个玩家的得分，总分，与提供者得分，与提供者得番
		int score = 0;
		int fan_shu = get_pai_xing_fan(chr) + player_multiple_count[seat_index];

		// 包赔
		int bao_pei_player = -1;

		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE).is_empty()
				|| !chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE).is_empty()) {
			bao_pei_player = player_kai_kou3[seat_index] - 1;
		}

		if (!zimo && chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG).is_empty()) {
			int value = _logic.get_card_value(operate_card);
			if (value == 2 || value == 5 || value == 8) {
				if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN).is_empty()) {
					int cards1[] = new int[GameConstants.MAX_COUNT];
					int hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[provide_index], cards1);
					if (hand_card_count == 1) {
						int value_h = _logic.get_card_value(cards1[0]);
						if (!(value_h == 2 || value_h == 5 || value_h == 8 || _logic.is_magic_card(cards1[0]))) {
							bao_pei_player = provide_index;
						}
					}
					if (player_kai_kou[provide_index] == 0)
						bao_pei_player = provide_index;
					else if (_playerStatus[provide_index]._hu_card_count == 0)
						bao_pei_player = provide_index;
				}
			}
		}

		boolean is_bao_pei = bao_pei_player != -1 && bao_pei_player != seat_index;
		if (is_bao_pei) {
			/*
			 * zimo = true; if(is_big_hu(chr)) fan_shu++;
			 */
		}

		if (cards[_logic.get_magic_card_index(0)] + player_lai_zi_gang_count[seat_index] == 4)
			fan_shu++;
		int fan_shu_fen = 1 << fan_shu;
		int fan_top = 0;
		// 开口玩家的个数
		int kai_count = 0;
		if (is_big_hu(chr)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;
				int kai_fan = player_kai_kou[i] > 0 ? 1 : 0;
				int fan = player_multiple_count[i] + fan_shu + kai_fan;
				if (player_kai_kou[i] > 0)
					kai_count++;
				if (fan >= 5)
					fan_top++;
			}
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;
				int kai_fan = player_kai_kou[i] > 0 ? 1 : 0;
				int score1 = fan_shu_fen * (1 << (player_multiple_count[i] + kai_fan));
				score += score1;
				if (score1 > scores[0])
					scores[0] = score1;
			}
		} else {
			if (is_big_hu(chr)) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;
					int kai_fan = player_kai_kou[i] > 0 ? 1 : 0;
					int score1 = 0;
					if (i == provide_index) {
						score1 = (1 << fan_shu + 1) * (1 << (player_multiple_count[i] + kai_fan));
					} else {
						score1 = fan_shu_fen * (1 << (player_multiple_count[i] + kai_fan));
					}
					if (fan_top >= 3) {
						if (kai_count == 0)
							score1 = 60;
						else {
							if (player_kai_kou[i] == 0) {
								score1 = 50;
							} else {
								score1 = 40;
							}
						}

					} else {
						score1 = score1 > 30 ? 30 : score1;
					}
					score += score1;
					if (i == provide_index) {
						scores[2] = score1;
						scores[3] = fan_shu + 1 + player_multiple_count[i] + kai_fan;
					}
					if (score1 > scores[0])
						scores[0] = score1;
				}
			} else {
				int kai_fan = player_kai_kou[provide_index] > 0 ? 1 : 0;
				int score1 = fan_shu_fen * (1 << (player_multiple_count[provide_index] + kai_fan));
				score1 = score1 > 30 ? 30 : score1;
				score += score1;
				scores[2] = score1;
				scores[3] = fan_shu + player_multiple_count[provide_index] + kai_fan;
				if (score1 > scores[0])
					scores[0] = score1;
			}
		}
		scores[1] = score;
		return scores;
	}

	/** 得到玩家的杠分 */
	public int get_gang_fen(int i) {
		if (player_multiple_count == null)
			return 0;
		int fan = player_multiple_count[i];
		return fan == 0 ? 0 : 1 << fan;
	}

	/** 得到牌型的番数 */
	public int get_pai_xing_fan(ChiHuRight chr) {
		int fen = 0;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_YING_HU).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_PENG_PENG_HU).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_HAI_DI_LAO).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_GANG_KAI_HUA).is_empty())
			fen++;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_ZI_MO).is_empty() && is_big_hu(chr))
			fen++;
		return fen;
	}

	public int get_ting_pai_fan(ChiHuRight chr) {
		int fen = 0;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_YING_HU).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_PENG_PENG_HU).is_empty())
			fen += 1;
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE).is_empty())
			fen += 1;
		return fen;
	}

	public boolean is_big_hu(ChiHuRight chr) {
		return !(chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE).is_empty()
				&& chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE).is_empty()
				&& chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_PENG_PENG_HU).is_empty()
				&& chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN).is_empty());
	}

	/**
	 * 分析抢杠胡和暗杠的猜胡 card = 0 表示暗杠的猜胡
	 */
	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			if (card != 0) {
				action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_QIANG_GANG, i, seat_index);

				// 结果判断
				if (action != 0 && !can_win_but_without_enough_score) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG);// 抢杠胡
					bAroseAction = true;
				}
			} else if (has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_KQAG)) {// 暗杠
				// 结果判断
				if (get_cai_hu_cards(i).size() > 0) {
					_playerStatus[i].add_action(GameConstants_MJ_XIAN_NING.WIK_CAI_GANG);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		// 包赔玩家
		int bao_pei_player = -1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE).is_empty()
				|| !chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE).is_empty()) {
			bao_pei_player = player_kai_kou3[seat_index] - 1;
		}

		if (!zimo && chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG).is_empty()) {
			int value = _logic.get_card_value(operate_card);
			if (value == 2 || value == 5 || value == 8) {
				if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN).is_empty()) {
					int cards[] = new int[GameConstants.MAX_COUNT];
					int hand_card_count = _logic.switch_to_cards_data_ezhou(GRR._cards_index[provide_index], cards);
					if (hand_card_count == 1) {
						int value_h = _logic.get_card_value(cards[0]);
						if (!(value_h == 2 || value_h == 5 || value_h == 8 || _logic.is_magic_card(cards[0]))) {
							bao_pei_player = provide_index;
						}
					}
					if (player_kai_kou[provide_index] == 0)
						bao_pei_player = provide_index;
					else if (_playerStatus[provide_index]._hu_card_count == 0)
						bao_pei_player = provide_index;
				}
			}
		}

		int fan_shu = get_pai_xing_fan(chr) + player_multiple_count[seat_index];
		boolean is_bao_pei = bao_pei_player != -1 && bao_pei_player != seat_index;
		if (is_bao_pei) {
			bao_pei_i = bao_pei_player;
			/*
			 * zimo = true; if(is_big_hu(chr)) fan_shu++;
			 */
		}

		int[] copyOf = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);
		if (zimo) {
			copyOf[_logic.switch_to_card_index(operate_card)]++;
		}
		if (copyOf[_logic.get_magic_card_index(0)] + player_lai_zi_gang_count[seat_index] == 4) {
			fan_shu++;
			is_si_lai = true;
		}

		float fan_shu_fen = 1 << fan_shu;
		float bottomScore = getBottomScore();
		fan_shu_fen *= bottomScore;
		countCardType(chr, seat_index);
		// 抢杠胡直接算分
		if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG).is_empty()) {
			float score = 40 * bottomScore;
			if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_YING_HU).is_empty()) {
				score = 50 * bottomScore;
			}
			if (!chr.opr_and(GameConstants_MJ_XIAN_NING.CHR_CAI_GNAG).is_empty()) {
				score = 50 * bottomScore;
			}
			if (is_bao_pei)
				GRR._game_score[bao_pei_player] -= score;
			else
				GRR._game_score[provide_index] -= score;
			GRR._game_score[seat_index] += score;
		} else {
			// 5番的个数
			int fan_top = 0;
			// 开口玩家的个数
			int kai_count = 0;
			if (zimo || is_big_hu(chr)) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;
					int kai_fan = player_kai_kou[i] > 0 ? 1 : 0;
					int fan = player_multiple_count[i] + fan_shu + kai_fan;
					if (provide_index == i && !zimo)
						fan++;
					if (player_kai_kou[i] > 0)
						kai_count++;
					if (fan >= 5)
						fan_top++;
					GRR._lost_fan_shu[i][seat_index] = fan;
				}
			} else {
				int kai_fan = player_kai_kou[provide_index] > 0 ? 1 : 0;
				GRR._lost_fan_shu[provide_index][seat_index] = player_multiple_count[provide_index] + fan_shu + kai_fan;
			}

			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;
					int kai_fan = player_kai_kou[i] > 0 ? 1 : 0;
					float score = fan_shu_fen * (1 << (player_multiple_count[i] + kai_fan));
					if (fan_top >= getTablePlayerNumber() - 1) {
						if (kai_count == 0 && fan_top == 3)
							score = 60 * bottomScore;
						else {
							if (player_kai_kou[i] == 0) {
								score = 50 * bottomScore;
							} else {
								score = 40 * bottomScore;
							}
						}

					} else {
						score = score > 30 * bottomScore ? 30 * bottomScore : score;
					}
					score = score < 1 ? 1 : score;
					if (is_bao_pei)
						GRR._game_score[bao_pei_player] -= score;
					else
						GRR._game_score[i] -= score;
					GRR._game_score[seat_index] += score;

				}
			} else {
				if (is_big_hu(chr)) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index)
							continue;
						int kai_fan = player_kai_kou[i] > 0 ? 1 : 0;
						float score = 0;
						if (i == provide_index)
							score = (1 << (fan_shu + 1)) * (1 << (player_multiple_count[i] + kai_fan)) * getBottomScore();
						else
							score = fan_shu_fen * (1 << (player_multiple_count[i] + kai_fan));
						if (fan_top >= getTablePlayerNumber() - 1) {
							if (kai_count == 0 && fan_top == 3)
								score = 60 * bottomScore;
							else {
								if (player_kai_kou[i] == 0) {
									score = 50 * bottomScore;
								} else {
									score = 40 * bottomScore;
								}
							}

						} else {
							score = score > 30 * bottomScore ? 30 * bottomScore : score;
						}
						score = score < 1 ? 1 : score;
						if (is_bao_pei)
							GRR._game_score[bao_pei_player] -= score;
						else
							GRR._game_score[i] -= score;
						GRR._game_score[seat_index] += score;
					}
				} else {
					int kai_fan = player_kai_kou[provide_index] > 0 ? 1 : 0;
					float score = fan_shu_fen * (1 << (player_multiple_count[provide_index] + kai_fan));
					score = score > 30 * bottomScore ? 30 * bottomScore : score;
					score = score < 1 ? 1 : score;
					if (is_bao_pei)
						GRR._game_score[bao_pei_player] -= score;
					else
						GRR._game_score[provide_index] -= score;
					GRR._game_score[seat_index] += score;
				}
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");
			chrTypes = GRR._chi_hu_rights[player].type_count;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_MJ_XIAN_NING.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_GANG_KAI_HUA) {
						result.append(" 杠上开花");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_JIE_PAO) {
						result.append(" 接炮");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE) {
						result.append(" 将一色");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_YING_HU) {
						result.append(" 硬胡");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}
					if (type == GameConstants_MJ_XIAN_NING.CHR_HAI_DI_LAO) {
						result.append(" 海底捞");
					}
					if (is_si_lai) {
						if (!result.toString().contains("四赖到底"))
							result.append(" 四赖到底");
					}
				} else if (type == GameConstants_MJ_XIAN_NING.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == GameConstants_MJ_XIAN_NING.CHR_BEI_QIANG_GNAG) {
					result.append(" 被抢杠");
				}
			}

			if (GRR._gang_score[player].an_gang_count > 0) {
				result.append(" 暗杠x" + GRR._gang_score[player].an_gang_count);
			}
			if (GRR._gang_score[player].ming_gang_count > 0) {
				result.append(" 明杠x" + GRR._gang_score[player].ming_gang_count);
			}

			if (qiang_fail[player])
				result.append(" 猜胡失败");
			if (bao_pei_i == player)
				result.append(" 包赔");
			GRR._result_des[player] = result.toString();
		}
	}

	public boolean is_jiang_yi_se(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount, boolean checkYing) {
		// 不能有吃
		if (exist_eat(weaveItem, weaveCount))
			return false;
		// 258判断
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (hand_indexs[i] == 0)
				continue;
			if (_logic.is_magic_index(i) && !checkYing)
				continue;
			if (i >= GameConstants.MAX_ZI)
				return false;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 2 && value != 5 && value != 8)
				return false;
		}

		// 落地牌都是258
		for (int i = 0; i < weaveCount; i++) {
			if (_logic.switch_to_card_index(weaveItem[i].center_card) >= GameConstants.MAX_ZI)
				return false;
			int value = _logic.get_card_value(weaveItem[i].center_card);
			if (value != 2 && value != 5 && value != 8)
				return false;
		}
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	public List<Integer> get_cai_hu_cards(int seat_index) {
		List<Integer> arr = new ArrayList<Integer>();
		int count = get_ting_card_qiang_gang(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_out_cards_fan[0],
				GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], seat_index);
		int[] hu_cards = _playerStatus[seat_index]._hu_cards;
		if (count > 0) {
			if (hu_cards[0] == -1)
				count = 27;
			for (int i = 0; i < count; i++) {
				boolean flag = true;
				int card = hu_cards[i];
				if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && _logic.is_magic_card(card - GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI))
					continue;
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
				for (int c = 0; c < hand_card_count; c++) {
					if (cards[c] == card) {
						flag = false;
						break;
					}
				}
				for (int p = 0; p < getTablePlayerNumber() && flag; p++) {
					for (int j = 0; j < GRR._discard_count[p]; j++) {
						if (GRR._discard_cards[p][j] == card) {
							flag = false;
							break;
						}
					}
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].public_card == 1) {
							if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_LEFT)
								if (GRR._weave_items[p][w].center_card == card || GRR._weave_items[p][w].center_card + 1 == card
										|| GRR._weave_items[p][w].center_card + 2 == card) {
									flag = false;
									break;
								}
							if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_CENTER)
								if (GRR._weave_items[p][w].center_card == card || GRR._weave_items[p][w].center_card - 1 == card
										|| GRR._weave_items[p][w].center_card + 1 == card) {
									flag = false;
									break;
								}
							if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_RIGHT)
								if (GRR._weave_items[p][w].center_card == card || GRR._weave_items[p][w].center_card - 1 == card
										|| GRR._weave_items[p][w].center_card - 2 == card) {
									flag = false;
									break;
								}
							if (GRR._weave_items[p][w].weave_kind == GameConstants.WIK_PENG
									|| GRR._weave_items[p][w].weave_kind == GameConstants.WIK_GANG) {
								if (GRR._weave_items[p][w].center_card == card) {
									flag = false;
									break;
								}
							}
						}
					}
				}
				if (flag)
					arr.add(hu_cards[i]);
			}
		}
		return arr;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x12, 0x12, 0x12, 0x03, 0x04, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };
		int[] cards_of_player1 = new int[] { 0x12, 0x12, 0x12, 0x03, 0x04, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x11, 0x18, 0x14, 0x16, 0x15, 0x15, 0x16, 0x16, 0x22, 0x22, 0x22 };
		int[] cards_of_player2 = new int[] { 0x22, 0x07, 0x08, 0x15, 0x18, 0x18, 0x15, 0x15, 0x22, 0x05, 0x05, 0x05, 0x05 };
		/*
		 * int[] cards_of_player0 = new int[] { 0x14, 0x14, 0x14, 0x14, 0x16,
		 * 0x16, 0x16, 0x17, 0x18, 0x22, 0x22, 0x22, 0x22 }; int[]
		 * cards_of_player1 = new int[] { 0x16, 0x16, 0x11, 0x19, 0x12, 0x12,
		 * 0x13, 0x13, 0x22, 0x22, 0x27, 0x27, 0x27 }; int[] cards_of_player3 =
		 * new int[] { 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x13, 0x13, 0x22,
		 * 0x22, 0x27, 0x27, 0x27 }; int[] cards_of_player2 = new int[] { 0x11,
		 * 0x11, 0x11, 0x12, 0x12, 0x12, 0x13, 0x13, 0x22, 0x22, 0x27, 0x27,
		 * 0x27 };
		 */

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

	public boolean has_chi_hu(PlayerStatus playerStatus) {
		for (int i = 0; i < playerStatus._action_count; i++) {
			if (playerStatus._action[i] == GameConstants.WIK_CHI_HU || playerStatus._action[i] == GameConstants.WIK_ZI_MO
					|| playerStatus._action[i] == GameConstants_MJ_XIAN_NING.WIK_CAI_GANG) {
				return true;
			}
		}
		return false;
	}

	public boolean has_chi_cai_hu(PlayerStatus playerStatus) {
		for (int i = 0; i < playerStatus._action_count; i++) {
			if (playerStatus._action[i] == GameConstants_MJ_XIAN_NING.WIK_CAI_GANG) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 玩家动作含猜杠--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	@Override
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			if (has_chi_cai_hu(curPlayerStatus)) {
				weaveItem_item.setWeaveKind(GameConstants_MJ_XIAN_NING.WIK_CAI_GANG);
				weaveItem_item.addAllWeaveCard(get_cai_hu_cards(seat_index));
			} else
				weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;
			this.send_response_to_player(i, roomResponse);
		}
		GRR.add_room_response(roomResponse);

		return true;
	}

	// 判断落地牌是将一色
	public boolean is_jiang_yi_se(WeaveItem[] weaveItems, int count) {
		for (int i = 0; i < count; i++) {
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if (value != 2 && value != 5 && value != 8)
				return false;
			if (weaveItems[i].weave_kind != GameConstants.WIK_GANG && weaveItems[i].weave_kind != GameConstants.WIK_PENG)
				return false;
		}
		return true;
	}

	// 判断落地牌是清一色
	public boolean is_qing_yi_se(WeaveItem[] weaveItems, int count) {
		int j = -1;
		for (int i = 0; i < count; i++) {
			int card_index = _logic.get_card_color(weaveItems[i].center_card);
			if (j == -1)
				j = card_index;
			else if (card_index != j)
				return false;
		}
		return true;
	}

	// 显示胡牌动画
	public void showAction(int _seat_index) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		ArrayList<Long> a = new ArrayList<Long>();

		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_QING_YI_SE).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 10));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIANG_YI_SE).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 11));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_PENG_PENG_HU).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 12));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_QUAN_QIU_REN).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 13));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_QIANG_GANG).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 14));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_HAI_DI_LAO).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 15));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_GANG_KAI_HUA).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 16));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_ZI_MO).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 17));
		if (!chiHuRight.opr_and(GameConstants_MJ_XIAN_NING.CHR_JIE_PAO).is_empty())
			a.add((long) (GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 18));

		long[] arr = new long[a.size()];
		for (int i = 0; i < a.size(); i++) {
			arr[i] = a.get(i);
		}
		operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, arr, 1, GameConstants.INVALID_SEAT);
	}

	public boolean check_qing_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count, boolean checkYing) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i) && !checkYing)
				continue;

			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weave_items[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int cbCenterCard = weave_items[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	/***
	 */
	public void exe_add_discard(int seat_index, int card_count, int card_data[]) {
		GRR._discard_count[seat_index]++;
		GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[0];
		card_data[0] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
		operate_add_discard(seat_index, card_count, card_data);
	}

}
