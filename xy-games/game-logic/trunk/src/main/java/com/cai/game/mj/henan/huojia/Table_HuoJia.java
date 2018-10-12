package com.cai.game.mj.henan.huojia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_HuoJia;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_HuoJia extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	/**
	 * 所有玩家，风成句的数目统计
	 */
	private int[] feng_cj_count = new int[getTablePlayerNumber()];
	/**
	 * 所有玩家，一九的数目统计
	 */
	private int[] yi_jiu_count = new int[getTablePlayerNumber()];
	/**
	 * 所有玩家，缺门的数目统计
	 */
	private int[] que_men_count = new int[getTablePlayerNumber()];

	/**
	 * 从什么地方走的分析胡牌算分，1表示从获取听牌数据的地方，2表示自摸或接炮的时候正常分析胡牌
	 */
	public int analyse_state;
	public static final int FROM_TING = 1;
	public static final int NORMAL = 2;

	/**
	 * 多少张牌的时候，流局
	 */
	public int left_card_draw;

	/**
	 * 玩法是否已经报听
	 */
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];

	/**
	 * 是否是有报听时并且点击了报听
	 */
	public boolean[] has_bao_ting_and_clicked = new boolean[getTablePlayerNumber()];

	public Table_HuoJia() {
		super(MJType.GAME_TYPE_MJ_HUO_JIA);
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
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);

			// 少人模式
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			// 处理玩家是否已经报听
			if (null != is_bao_ting) {
				room_player.setBiaoyan(is_bao_ting[i] ? 1 : 0);
			} else {
				room_player.setBiaoyan(0);
			}

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuoJia.HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, boolean has_feng) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		if (has_feng) {
			max_ting_count = GameConstants.MAX_ZI_FENG;
		}

		PerformanceTimer timer = new PerformanceTimer();

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuoJia.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (timer.get() > 50) {
			logger.warn("room id: " + getRoom_id() + "cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
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
		_handler_chi_peng = new HandlerChiPeng_HuoJia();
		_handler_dispath_card = new HandlerDispatchCard_HuoJia();
		_handler_gang = new HandlerGang_HuoJia();
		_handler_out_card_operate = new HandlerOutCardOperate_HuoJia();
	}

	@Override
	protected boolean on_game_start() {
		feng_cj_count = new int[getTablePlayerNumber()];
		yi_jiu_count = new int[getTablePlayerNumber()];
		que_men_count = new int[getTablePlayerNumber()];

		// 剩余12张算流局，多一杠加2张
		left_card_draw = 12;

		is_bao_ting = new boolean[getTablePlayerNumber()];

		has_bao_ting_and_clicked = new boolean[getTablePlayerNumber()];

		_game_status = GameConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
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

			// 重新装载玩家信息
			load_player_info_data(roomResponse);

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

		setGameEndBasicPrama(game_end);

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
				// TODO: 荒庄荒杠
				if (GRR._end_type != GameConstants.Game_End_DRAW) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];
						}
					}
				}

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

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
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
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
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
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (is_bao_ting[_seat_index] == false && analyse_state == NORMAL) {
			// 没报听不能胡牌
			return GameConstants.WIK_NULL;
		}

		boolean has_19_cheng_ju = has_rule(Constants_HuoJia.GAME_RULE_YI_JIU_CHENG_JU);
		boolean has_feng_cheng_ju = has_rule(Constants_HuoJia.GAME_RULE_FENG_CHENG_JU);
		boolean has_que_men = has_rule(Constants_HuoJia.GAME_RULE_QUE_MEN);

		if (!_logic.is_valid_card(cur_card)) {
			return GameConstants.WIK_NULL;
		}

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}
		tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int cbChiHuKind = GameConstants.WIK_NULL;

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		PerformanceTimer timer = new PerformanceTimer();

		boolean can_win = newLogic.analyse_card_huo_jia(tmp_cards_index, weaveItems, weave_count, analyseItemArray, has_19_cheng_ju,
				has_feng_cheng_ju);

		if (timer.get() > 100) {
			logger.warn("room id: " + getRoom_id() + "cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		if (!can_win) {
			chiHuRight.set_empty();
			return cbChiHuKind;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_HuoJia.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuoJia.CHR_ZI_MO);
		} else if (card_type == Constants_HuoJia.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_HuoJia.CHR_JIE_PAO);
		} else if (card_type == Constants_HuoJia.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_HuoJia.CHR_JIE_PAO);
		}

		// 正常自摸或接炮分析的时候才存储统计数据
		if (analyse_state == NORMAL && can_win) {
			yi_jiu_count[_seat_index] = 0;
			feng_cj_count[_seat_index] = 0;
			que_men_count[_seat_index] = 0;

			int max_score_count = 0;
			int max_score_index = 0;

			for (int i = 0; i < analyseItemArray.size(); i++) {
				int tmp_max_score_count = 0;

				AnalyseItem analyseItem = analyseItemArray.get(i);
				for (int j = 0; j < 4; j++) {
					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
						continue;
					}

					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_SI_DUI) {
						tmp_max_score_count++;
					}
					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_YAO_JIU) {
						tmp_max_score_count++;
					}
					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_FENG_CHI) {
						tmp_max_score_count++;
					}
				}

				if (tmp_max_score_count > max_score_count) {
					max_score_count = tmp_max_score_count;
					max_score_index = i;
				}
			}

			AnalyseItem analyseItem = analyseItemArray.get(max_score_index);
			for (int j = 0; j < 4; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
					continue;
				}

				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_SI_DUI) {
					chiHuRight.opr_or(Constants_HuoJia.CHR_SI_DUI);
				}
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_YAO_JIU) {
					chiHuRight.opr_or(Constants_HuoJia.CHR_YI_JIU);
					yi_jiu_count[_seat_index]++;
				}
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_FENG_CHI) {
					chiHuRight.opr_or(Constants_HuoJia.CHR_FENG_CHENG_JU);
					feng_cj_count[_seat_index]++;
				}
			}

			if (has_que_men) {
				int colorCount = _logic.get_se_count(tmp_cards_index, weaveItems, weave_count);
				que_men_count[_seat_index] = 3 - colorCount;
			}
		}

		return cbChiHuKind;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_HuoJia.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
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

			if (GRR._left_card_count > left_card_draw) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && !is_bao_ting[i]) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (is_bao_ting[i]) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					} else {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				analyse_state = NORMAL;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_HuoJia.HU_CARD_TYPE_JIE_PAO, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
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
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int lChiHuScore = GameConstants.CELL_SCORE;

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
			int s = lChiHuScore;
			s += feng_cj_count[seat_index] + yi_jiu_count[seat_index] + que_men_count[seat_index];

			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuoJia.CHR_SI_DUI).is_empty()) {
				s += 1;
			}

			if (has_rule(Constants_HuoJia.GAME_RULE_ZI_MO_FAN_BEI)) {
				s = s * 2;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore;
			s += feng_cj_count[seat_index] + yi_jiu_count[seat_index] + que_men_count[seat_index];

			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuoJia.CHR_SI_DUI).is_empty()) {
				s += 1;
			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuoJia.CHR_JIE_PAO).is_empty()) {
				GRR._chi_hu_rights[provide_index].opr_or(Constants_HuoJia.CHR_FANG_PAO);
			}
		}
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
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

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;

		int[] jie_gang = new int[getTablePlayerNumber()];
		int[] fang_gang = new int[getTablePlayerNumber()];
		int[] an_gang = new int[getTablePlayerNumber()];
		// TODO: 荒庄荒杠
		if (GRR._end_type != GameConstants.Game_End_DRAW) {
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				if (GRR != null) {
					for (int w = 0; w < GRR._weave_count[player]; w++) {
						if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}

						if (GRR._weave_items[player][w].public_card == 0) {
							an_gang[player]++;
						} else {
							jie_gang[player]++;
							fang_gang[GRR._weave_items[player][w].provide_player]++;
						}
					}
				}
			}
		}

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_HuoJia.CHR_ZI_MO)
						result.append(" 自摸");

					if (type == Constants_HuoJia.CHR_JIE_PAO)
						result.append(" 接炮");
				} else if (type == Constants_HuoJia.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (!GRR._chi_hu_rights[player].opr_and(Constants_HuoJia.CHR_SI_DUI).is_empty()) {
					result.append(" 四对");
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (feng_cj_count[player] > 0) {
					result.append(" 风成句x" + feng_cj_count[player]);
				}
				if (yi_jiu_count[player] > 0) {
					result.append(" 19成句x" + yi_jiu_count[player]);
				}
				if (que_men_count[player] > 0) {
					result.append(" 缺门x" + que_men_count[player]);
				}
			}

			// TODO: 荒庄荒杠
			if (GRR._end_type != GameConstants.Game_End_DRAW) {
				if (an_gang[player] > 0) {
					result.append(" 暗杠x" + an_gang[player]);
				}
				if (fang_gang[player] > 0) {
					result.append(" 放杠x" + fang_gang[player]);
				}
				if (jie_gang[player] > 0) {
					result.append(" 接杠x" + jie_gang[player]);
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x19, 0x19, 0x29, 0x29, 0x21, 0x21, 0x31, 0x32, 0x33, 0x34, 0x33 };
		int[] cards_of_player1 = new int[] { 0x35, 0x35, 0x36, 0x36, 0x37, 0x37, 0x31, 0x31, 0x01, 0x01, 0x09, 0x09, 0x01 };
		int[] cards_of_player3 = new int[] { 0x35, 0x35, 0x36, 0x36, 0x37, 0x37, 0x31, 0x31, 0x01, 0x01, 0x09, 0x09, 0x01 };
		int[] cards_of_player2 = new int[] { 0x35, 0x35, 0x36, 0x36, 0x37, 0x37, 0x31, 0x31, 0x01, 0x01, 0x09, 0x09, 0x01 };

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
