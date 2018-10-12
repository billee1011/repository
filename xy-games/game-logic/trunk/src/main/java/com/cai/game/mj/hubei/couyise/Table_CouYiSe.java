package com.cai.game.mj.hubei.couyise;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_CouYiSe;
import com.cai.common.constant.game.Constants_HuangShi;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
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

public class Table_CouYiSe extends AbstractMJTable {
	private static final long serialVersionUID = -6620091147172720434L;

	public HandlerSelectMagic_CouYiSe _handler_select_magic;

	public int pi_zi = -1;
	public int magic_card = -1;
	public int magic_card_index = -1;

	public int[] tou_zi_dian_shu = new int[2]; // 用来储存2个骰子的点数

	public int time_for_animation = 2000; // 摇骰子的动画时间(ms)
	public int time_for_fade = 500; // 摇骰子动画的消散延时(ms)

	public int qi_pai_index = 0; // 起牌的索引位置，一共有4个牌堆

	public boolean can_win_but_without_enough_score = false; // 能胡，但是没达到起胡分

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_CouYiSe();
		_handler_dispath_card = new HandlerDispatchCard_CouYiSe();
		_handler_gang = new HandlerGang_CouYiSe();
		_handler_out_card_operate = new HandlerOutCardOperate_CouYiSe();
		_handler_select_magic = new HandlerSelectMagic_CouYiSe();
	}

	public boolean operate_cant_win_info(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CAN_WIN_BUT_WITHOUT_ENOUGH_SCORE);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsXiangGong(true); // TODO 表示牌型能胡但是没达到起胡分

		send_response_to_player(seat_index, roomResponse);

		return true;
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

		pi_zi = 0;
		magic_card = 0;
		magic_card_index = -1;

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
			int hand_card_count = _logic.switch_to_cards_data_couyise(GRR._cards_index[i], hand_cards[i], pi_zi);
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
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
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
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data_couyise(GRR._cards_index[i], GRR._cards_data[i], pi_zi);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else if (GRR._cards_data[i][j] == pi_zi) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_PI_ZI);
					} else if (GRR._cards_data[i][j] == Constants_CouYiSe.HZ_CARD) {
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

		return false;
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

		// TODO: 手里有红中或痞子不能胡牌
		if (cbCardIndexTemp[Constants_CouYiSe.HZ_INDEX] > 0 || cbCardIndexTemp[_logic.switch_to_card_index(pi_zi)] > 0)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count); // 带癞子时能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0); // 癞子牌还原之后能胡或者手牌没癞子牌能胡

		boolean is_cou_yi_se = can_win_with_magic && _logic.check_hubei_couyise(cbCardIndexTemp, weaveItems, weave_count); // 1.凑一色
																															// 风牌加任意一门花色组成的牌型
		boolean is_ying_cou_yi_se = is_cou_yi_se && can_win_without_magic
				&& _logic.check_hubei_ying_couyise(cbCardIndexTemp, weaveItems, weave_count); // 1.硬凑一色
		if (is_cou_yi_se)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_COU_YI_SE);

		boolean is_feng_yi_se = _logic.check_hubei_feng_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 2.风一色
		boolean is_ying_feng_yi_se = is_feng_yi_se && _logic.check_hubei_ying_feng_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 2.硬风一色
		if (is_feng_yi_se)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_FENG_YI_SE);

		boolean is_qing_yi_se = can_win_with_magic && _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 3.清一色
		boolean is_ying_qing_yi_se = is_qing_yi_se && can_win_without_magic
				&& _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 3.硬清一色
		if (is_qing_yi_se)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_QING_YI_SE);

		boolean is_jiang_yi_se = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 4.将一色
		boolean is_ying_jiang_yi_se = is_jiang_yi_se && _logic.check_hubei_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count); // 4.硬将一色
		if (is_jiang_yi_se)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_JIANG_YI_SE);

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);
		boolean exist_eat = _logic.exist_eat_hubei(weaveItems, weave_count);

		boolean is_hun_peng = can_peng_hu && !exist_eat; // 5.混碰
		boolean is_ying_hun_peng = can_ying_peng_hu && !exist_eat && !_logic.exist_suo_pai(weaveItems, weave_count); // 5.硬混碰
		if (is_hun_peng)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_HUN_PENG);

		boolean is_hun_qi = _logic.check_hubei_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 6.混七
		boolean is_ying_hun_qi = _logic.check_hubei_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); // 6.硬混七
		if (is_hun_qi)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_HUN_QI);

		boolean is_cou_peng = is_hun_peng && is_cou_yi_se; // 7.凑碰
		boolean is_ying_cou_peng = is_ying_hun_peng && is_ying_cou_yi_se; // 7.硬凑碰
		if (is_cou_peng)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_COU_PENG);

		boolean is_cou_qi = is_hun_qi && is_cou_yi_se; // 8.凑七
		boolean is_ying_cou_qi = is_ying_hun_qi && is_ying_cou_yi_se; // 8.硬凑七
		if (is_cou_qi)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_COU_QI);

		boolean is_qing_peng = is_hun_peng && is_qing_yi_se; // 9.清碰
		boolean is_ying_qing_peng = is_ying_hun_peng && is_ying_qing_yi_se; // 9.硬清碰
		if (is_qing_peng)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_QING_PENG);

		boolean is_qing_qi = is_hun_qi && is_qing_yi_se; // 10.清七
		boolean is_ying_qing_qi = is_ying_hun_qi && is_ying_qing_yi_se; // 10.硬清七
		if (is_qing_qi)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_QING_QI);

		boolean is_jiang_peng = is_jiang_yi_se && is_hun_peng; // 11.将碰
		boolean is_ying_jiang_peng = is_ying_jiang_yi_se && is_ying_hun_peng; // 11.硬将碰
		if (is_jiang_peng)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_JIANG_PENG);

		// TODO 不是下面的任意一种牌型，不能胡牌，即牌型低于2分不能胡牌
		if (!is_cou_yi_se && !is_feng_yi_se && !is_qing_yi_se && !is_jiang_yi_se && !is_hun_peng && !is_hun_qi && !is_cou_peng && !is_cou_qi
				&& !is_qing_peng && !is_qing_qi && !is_jiang_peng) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		boolean can_ying_hu = true;

		if ((is_cou_yi_se && !is_ying_cou_yi_se) || (is_feng_yi_se && !is_ying_feng_yi_se) || (is_qing_yi_se && !is_ying_qing_yi_se)
				|| (is_jiang_yi_se && !is_ying_jiang_yi_se) || (is_hun_peng && !is_ying_hun_peng) || (is_hun_qi && !is_ying_hun_qi)
				|| (is_cou_peng && !is_ying_cou_peng) || (is_cou_qi && !is_ying_cou_qi) || (is_qing_peng && !is_ying_qing_peng)
				|| (is_qing_qi && !is_ying_qing_qi) || (is_jiang_peng && !is_ying_jiang_peng))
			can_ying_hu = false;

		if (can_ying_hu)
			chiHuRight.opr_or(Constants_HuangShi.CHR_YING_HU);
		else
			chiHuRight.opr_or(Constants_HuangShi.CHR_RUAN_HU);

		boolean zi_mo = false;
		if (card_type == Constants_CouYiSe.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_CouYiSe.CHR_ZI_MO);
			zi_mo = true;
		} else if (card_type == Constants_CouYiSe.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_CouYiSe.CHR_ZI_MO);
			chiHuRight.opr_or(Constants_CouYiSe.CHR_GANG_KAI);
			zi_mo = true;
		} else {
			chiHuRight.opr_or(Constants_CouYiSe.CHR_JIE_PAO);
			if (weave_count == 4)
				chiHuRight.opr_or(Constants_CouYiSe.CHR_QUAN_QIU_REN);
		}

		if (GRR._left_card_count == 0)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_HAI_DI_LAO);

		if (GRR._weave_count[seat_index] == 0)
			chiHuRight.opr_or(Constants_CouYiSe.CHR_MEN_QIAN_QING);

		if (get_tmp_score(chiHuRight, seat_index, provide_index, zi_mo) < get_min_score()) {
			can_win_but_without_enough_score = true;

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		return cbChiHuKind;
	}

	public int get_min_score() {
		int min_score = 8;

		if (ruleMap.containsKey(Constants_CouYiSe.GAME_RULE_QI_HU_FEN))
			min_score = ruleMap.get(Constants_CouYiSe.GAME_RULE_QI_HU_FEN);

		return min_score;
	}

	public int get_max_fen() {
		int max_fen = 30;

		if (ruleMap.containsKey(Constants_CouYiSe.GAME_RULE_FENG_DING))
			max_fen = ruleMap.get(Constants_CouYiSe.GAME_RULE_FENG_DING);

		return max_fen;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_CouYiSe.HU_CARD_TYPE_ZI_MO, seat_index, seat_index)) {
				cards[count] = cbCurrentCard;
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				else if (cbCurrentCard == pi_zi)
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count - 1) {
			count = 1;
			cards[0] = -1;
		}

		return count;
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
					// TODO 先处理癞子牌不参与吃
					action = _logic.check_chi(GRR._cards_index[i], card);

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

					// TODO 再处理癞子牌参与吃
					action = _logic.check_chi_with_suo_pai(GRR._cards_index[i], card);
					if ((action & GameConstants.WIK_SUO_CHI_LEFT_1) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_SUO_CHI_LEFT_1);
						_playerStatus[i].add_chi(card, GameConstants.WIK_SUO_CHI_LEFT_1, seat_index);
					}
					if ((action & GameConstants.WIK_SUO_CHI_LEFT_2) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_SUO_CHI_LEFT_2);
						_playerStatus[i].add_chi(card, GameConstants.WIK_SUO_CHI_LEFT_2, seat_index);
					}
					if ((action & GameConstants.WIK_SUO_CHI_CENTER_1) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_SUO_CHI_CENTER_1);
						_playerStatus[i].add_chi(card, GameConstants.WIK_SUO_CHI_CENTER_1, seat_index);
					}
					if ((action & GameConstants.WIK_SUO_CHI_CENTER_2) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_SUO_CHI_CENTER_2);
						_playerStatus[i].add_chi(card, GameConstants.WIK_SUO_CHI_CENTER_2, seat_index);
					}
					if ((action & GameConstants.WIK_SUO_CHI_RIGHT_1) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_SUO_CHI_RIGHT_1);
						_playerStatus[i].add_chi(card, GameConstants.WIK_SUO_CHI_RIGHT_1, seat_index);
					}
					if ((action & GameConstants.WIK_SUO_CHI_RIGHT_2) != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_SUO_CHI_RIGHT_2);
						_playerStatus[i].add_chi(card, GameConstants.WIK_SUO_CHI_RIGHT_2, seat_index);
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				// TODO 癞子牌可以参与碰
				action = _logic.check_peng_couyise(GRR._cards_index[i], card, pi_zi);
				if (action != 0) {
					int wik_type = GameConstants.WIK_PENG;

					if ((action & GameConstants.WIK_SUO_PENG_1) != GameConstants.WIK_NULL)
						wik_type = GameConstants.WIK_SUO_PENG_1;
					else if ((action & GameConstants.WIK_SUO_PENG_2) != GameConstants.WIK_NULL)
						wik_type = GameConstants.WIK_SUO_PENG_2;

					playerStatus.add_action(GameConstants.WIK_PENG);
					playerStatus.add_peng_couyise(card, wik_type, seat_index);

					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						int wik_type = GameConstants.WIK_GANG;

						if ((action & GameConstants.WIK_SUO_GANG_1) != GameConstants.WIK_NULL)
							wik_type = GameConstants.WIK_SUO_GANG_1;
						else if ((action & GameConstants.WIK_SUO_GANG_2) != GameConstants.WIK_NULL)
							wik_type = GameConstants.WIK_SUO_GANG_2;
						else if ((action & GameConstants.WIK_SUO_GANG_3) != GameConstants.WIK_NULL)
							wik_type = GameConstants.WIK_SUO_GANG_3;

						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang_with_suo_pai(card, i, 1, wik_type);

						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = Constants_CouYiSe.HU_CARD_TYPE_JIE_PAO;

				action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i, seat_index);

				if (action != 0) {
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

	public boolean exe_select_magic_card(int seat_index) {
		set_handler(_handler_select_magic);
		_handler_select_magic.reset_status(seat_index);
		_handler_select_magic.exe(this);
		return true;
	}

	@Override
	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_1
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_2 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_1
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_2 || weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_1
					|| weaveItem[i].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_2)
				return true;
		}

		return false;
	}

	public boolean is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

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
				return false;
			}
		} else {
			if (cbReplaceCount > 0)
				return false;
		}

		return true;
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
					Constants_CouYiSe.HU_CARD_TYPE_ZI_MO, seat_index, seat_index))
				return true;
		}
		return false;
	}

	public boolean is_ying_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return false;

		return true;
	}

	public boolean is_ying_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
			return false;

		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
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

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_PI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_PI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_HZ) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_HZ && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HZ;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public int get_real_fan_shu(int fan_shu) {
		int real_fan_shu = 1;

		if (fan_shu == 0) {
			real_fan_shu = 1;
		} else {
			for (int i = 0; i < fan_shu; i++) {
				real_fan_shu *= 2;
			}
		}

		return real_fan_shu;
	}

	public int get_di_fen() {
		int di_fen = 1;

		if (ruleMap.containsKey(Constants_CouYiSe.GAME_RULE_DI_FEN))
			di_fen = ruleMap.get(Constants_CouYiSe.GAME_RULE_DI_FEN);

		return di_fen;
	}

	public int get_gang_fan_shu(int lost_player, int win_player) {
		int gang_fan_shu = 0;

		if (GRR != null) {
			gang_fan_shu += (GRR._gang_score[lost_player].an_gang_count + GRR._gang_score[win_player].an_gang_count) * 2;
			gang_fan_shu += GRR._gang_score[lost_player].ming_gang_count + GRR._gang_score[win_player].ming_gang_count;
		}

		return gang_fan_shu;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
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
			room_player.setNao(_player_result.nao[i]);
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

	// TODO 获取番数的方法需要调整
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

			for (int w = 0; w < GRR._weave_count[seat_index]; w++) {
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_1) {
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_2) {
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_1) {
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_2) {
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_1) {
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_2) {
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_PENG_1) {
					fan_shu += 1;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_PENG_2) {
					fan_shu += 2;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_1) {
					fan_shu += 1;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_2) {
					fan_shu += 2;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_3) {
					fan_shu += 3;
				}
			}
		}

		if (fan_shu == 0)
			return 0;
		else
			return (int) Math.pow(2, fan_shu);
	}

	// TODO 除了和get_player_fan_shu一样之外，还有一个‘4个同样的牌’的番数需要处理
	public int get_player_final_fan_shu(int seat_index) {
		int fan_shu = 0;

		if (GRR != null) {
			for (int x = 0; x < GRR._player_niao_count[seat_index]; x++) {
				if (_logic.is_magic_card(GRR._lai_zi_pi_zi_gang[seat_index][x]))
					fan_shu += 2;
				else
					fan_shu += 1;
			}

			fan_shu += GRR._gang_score[seat_index].ming_gang_count + GRR._gang_score[seat_index].an_gang_count * 2;

			int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];

			for (int i = 0; i < GameConstants.MAX_INDEX; i++)
				tmp_cards_index[i] = GRR._cards_index[seat_index][i];

			for (int w = 0; w < GRR._weave_count[seat_index]; w++) {
				int card_index = _logic.switch_to_card_index(GRR._weave_items[seat_index][w].center_card);
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_LEFT) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index + 1]++;
					tmp_cards_index[card_index + 2]++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_CENTER) {
					tmp_cards_index[card_index - 1]++;
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index + 1]++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_RIGHT) {
					tmp_cards_index[card_index - 2]++;
					tmp_cards_index[card_index - 1]++;
					tmp_cards_index[card_index]++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_1) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index + 2]++;
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_LEFT_2) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index + 1]++;
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_1) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index + 1]++;
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_CENTER_2) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index - 1]++;
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_1) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index - 1]++;
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_CHI_RIGHT_2) {
					tmp_cards_index[card_index]++;
					tmp_cards_index[card_index - 2]++;
					fan_shu++;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_PENG) {
					tmp_cards_index[card_index] += 3;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_PENG_1) {
					tmp_cards_index[card_index] += 2;
					fan_shu += 1;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_PENG_2) {
					tmp_cards_index[card_index] += 1;
					fan_shu += 2;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_1) {
					tmp_cards_index[card_index] += 3;
					fan_shu += 1;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_2) {
					tmp_cards_index[card_index] += 2;
					fan_shu += 2;
				}
				if (GRR._weave_items[seat_index][w].weave_kind == GameConstants.WIK_SUO_GANG_3) {
					tmp_cards_index[card_index] += 1;
					fan_shu += 3;
				}
			}
		}

		if (fan_shu == 0)
			return 0;
		else
			return (int) Math.pow(2, fan_shu);
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setScoreType(get_player_fan_shu(seat_index));
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);

				int[] weave_cards = new int[4];
				int count = _logic.get_weave_card(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
				for (int x = 0; x < count; x++) {
					if (_logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					else if (weave_cards[x] == pi_zi)
						weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

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

		this.load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);

				int[] weave_cards = new int[4];
				int count = _logic.get_weave_card(weaveitems[j].weave_kind, weaveitems[j].center_card, weave_cards);
				for (int x = 0; x < count; x++) {
					if (_logic.is_magic_card(weave_cards[x]))
						weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					else if (weave_cards[x] == pi_zi)
						weave_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;

					weaveItem_item.addWeaveCard(weave_cards[x]);
				}

				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
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
				else if (pi_zi == card)
					card += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		return 0;
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
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
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
		int hand_card_count = _logic.switch_to_cards_data_couyise(GRR._cards_index[seat_index], cards, pi_zi);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i]))
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			else if (cards[i] == pi_zi)
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
		}

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	public int get_tmp_score(ChiHuRight chr, int seat_index, int provide_index, boolean zimo) {
		int score = 0;

		int di_fen = get_di_fen();

		int pai_xing_fen = get_pai_xing_fen(chr);

		int fan_shu = 0;

		int lChiHuScore = di_fen * pai_xing_fen * GameConstants.CELL_SCORE;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_CouYiSe.CHR_ZI_MO).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_YING_HU).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_GANG_KAI).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_HAI_DI_LAO).is_empty())
					fan_shu += 1;

				fan_shu += get_player_final_fan_shu(i) + get_player_final_fan_shu(seat_index);

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				// TODO 任何一个人给的分低于起胡分，不能胡牌
				if (lChiHuScore * real_fan_shu < get_min_score())
					return 0;

				score += lChiHuScore * real_fan_shu;

				fan_shu = tmp_fan_shu;
			}
		} else {
			int tmp_fan_shu = fan_shu;

			if (!chr.opr_and(Constants_CouYiSe.CHR_YING_HU).is_empty())
				fan_shu += 1;
			if (!chr.opr_and(Constants_CouYiSe.CHR_HAI_DI_LAO).is_empty())
				fan_shu += 1;
			if (!chr.opr_and(Constants_CouYiSe.CHR_QUAN_QIU_REN).is_empty())
				fan_shu += 1;

			fan_shu += get_player_final_fan_shu(provide_index) + get_player_final_fan_shu(seat_index);

			int real_fan_shu = (int) Math.pow(2, fan_shu);

			// TODO 任何一个人给的分低于起胡分，不能胡牌
			if (lChiHuScore * real_fan_shu < get_min_score())
				return 0;

			score += lChiHuScore * real_fan_shu;

			fan_shu = tmp_fan_shu;
		}

		return score;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = get_di_fen();

		int max_fen = get_max_fen();

		int pai_xing_fen = get_pai_xing_fen(chr);

		int fan_shu = 0;

		countCardType(chr, seat_index);

		int lChiHuScore = di_fen * pai_xing_fen * GameConstants.CELL_SCORE;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_fan_shu = fan_shu;

				if (!chr.opr_and(Constants_CouYiSe.CHR_ZI_MO).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_YING_HU).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_GANG_KAI).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_HAI_DI_LAO).is_empty())
					fan_shu += 1;
				if (!chr.opr_and(Constants_CouYiSe.CHR_MEN_QIAN_QING).is_empty())
					fan_shu += 1;

				fan_shu += get_player_final_fan_shu(i) + get_player_final_fan_shu(seat_index);

				int real_fan_shu = (int) Math.pow(2, fan_shu);

				int s = lChiHuScore * real_fan_shu;

				if (s > max_fen)
					s = max_fen;

				fan_shu = tmp_fan_shu;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int tmp_fan_shu = fan_shu;

			if (!chr.opr_and(Constants_CouYiSe.CHR_YING_HU).is_empty())
				fan_shu += 1;
			if (!chr.opr_and(Constants_CouYiSe.CHR_HAI_DI_LAO).is_empty())
				fan_shu += 1;
			if (!chr.opr_and(Constants_CouYiSe.CHR_QUAN_QIU_REN).is_empty())
				fan_shu += 1;
			if (!chr.opr_and(Constants_CouYiSe.CHR_MEN_QIAN_QING).is_empty())
				fan_shu += 1;

			fan_shu += get_player_final_fan_shu(provide_index) + get_player_final_fan_shu(seat_index);

			int real_fan_shu = (int) Math.pow(2, fan_shu);

			int s = lChiHuScore * real_fan_shu;

			if (s > max_fen)
				s = max_fen;

			fan_shu = tmp_fan_shu;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_CouYiSe.CHR_JIE_PAO).is_empty())
				GRR._chi_hu_rights[provide_index].opr_or(Constants_CouYiSe.CHR_FANG_PAO);
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

			boolean is_ying_hu = false;
			boolean pai_xing_finded = false;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_CouYiSe.CHR_ZI_MO)
						result.append(" 自摸");

					if (type == Constants_CouYiSe.CHR_JIE_PAO) {
						result.append(" 接炮");
						if (type == Constants_CouYiSe.CHR_QUAN_QIU_REN)
							result.append(" 全求人");
					}

					if (type == Constants_CouYiSe.CHR_YING_HU) {
						result.append(" 硬胡");
						is_ying_hu = true;
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_JIANG_PENG) {
							result.append(" 将碰");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_QING_QI) {
							result.append(" 清七");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_QING_PENG) {
							result.append(" 清碰");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_COU_QI) {
							result.append(" 凑七");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_COU_PENG) {
							result.append(" 凑碰");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_HUN_QI) {
							result.append(" 混七");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_HUN_PENG) {
							result.append(" 混碰");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_JIANG_YI_SE) {
							result.append(" 将一色");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_QING_YI_SE) {
							result.append(" 清一色");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_FENG_YI_SE) {
							result.append(" 风一色");
							pai_xing_finded = true;
						}
					}

					if (pai_xing_finded == false) {
						if (type == Constants_CouYiSe.CHR_COU_YI_SE) {
							result.append(" 凑一色");
							pai_xing_finded = true;
						}
					}

					if (type == Constants_CouYiSe.CHR_HAI_DI_LAO)
						result.append(" 海底捞");

				} else if (type == Constants_CouYiSe.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

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

	public int get_pai_xing_fen(ChiHuRight chr) {
		int pai_xing_fen = 2;

		if (!chr.opr_and(Constants_CouYiSe.CHR_COU_YI_SE).is_empty())
			pai_xing_fen = 2;

		if (!chr.opr_and(Constants_CouYiSe.CHR_FENG_YI_SE).is_empty())
			pai_xing_fen = 4;

		if (!chr.opr_and(Constants_CouYiSe.CHR_QING_YI_SE).is_empty())
			pai_xing_fen = 4;

		if (!chr.opr_and(Constants_CouYiSe.CHR_JIANG_YI_SE).is_empty())
			pai_xing_fen = 4;

		if (!chr.opr_and(Constants_CouYiSe.CHR_HUN_PENG).is_empty())
			pai_xing_fen = 4;

		if (!chr.opr_and(Constants_CouYiSe.CHR_HUN_QI).is_empty())
			pai_xing_fen = 4;

		if (!chr.opr_and(Constants_CouYiSe.CHR_COU_PENG).is_empty())
			pai_xing_fen = 8;

		if (!chr.opr_and(Constants_CouYiSe.CHR_COU_QI).is_empty())
			pai_xing_fen = 8;

		if (!chr.opr_and(Constants_CouYiSe.CHR_QING_PENG).is_empty())
			pai_xing_fen = 16;

		if (!chr.opr_and(Constants_CouYiSe.CHR_QING_QI).is_empty())
			pai_xing_fen = 16;

		if (!chr.opr_and(Constants_CouYiSe.CHR_JIANG_PENG).is_empty())
			pai_xing_fen = 16;

		return pai_xing_fen;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x11, 0x11, 0x35, 0x35 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x03, 0x05, 0x05, 0x06, 0x07, 0x07, 0x08, 0x11, 0x12, 0x17, 0x17 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x03, 0x05, 0x05, 0x06, 0x07, 0x07, 0x08, 0x11, 0x12, 0x17, 0x17 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x03, 0x05, 0x05, 0x06, 0x07, 0x07, 0x08, 0x11, 0x12, 0x17, 0x17 };

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
}
