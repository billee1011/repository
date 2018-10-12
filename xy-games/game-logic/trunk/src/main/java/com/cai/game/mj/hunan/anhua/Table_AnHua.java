package com.cai.game.mj.hunan.anhua;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_AnHua;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

@ThreeDimension
public class Table_AnHua extends AbstractMJTable {
	private static final long serialVersionUID = -2456323602522819218L;

	/**
	 * 系统自动出牌的延时，毫秒
	 */
	public final int AUTO_OUT_CARD_DELAY = 1000;

	/**
	 * 王牌的相关信息
	 */
	public int joker_card_1 = 0;
	public int joker_card_index_1 = -1;
	public int joker_card_2 = 0;
	public int joker_card_index_2 = -1;
	public int ding_wang_card = 0;
	public int ding_wang_card_index = -1;

	public HandlerSelectMagicCard_AnHua _handler_select_magic_card;
	public HandlerGangDispatchCard_AnHua _handler_gang_dispatch_card;

	/**
	 * 手牌的王类型
	 */
	public static final int SINGLE_MAGIC = 1;
	public static final int THREE_MAGIC = 2;
	public static final int FOUR_MAGIC = 3;
	public static final int SIX_MAGIC = 4;
	public static final int SEVEN_MAGIC = 5;
	public static final int NONE_MAGIC = 6;

	public int[] max_win_score = new int[getTablePlayerNumber()];
	public int[] win_card_at_gang = new int[getTablePlayerNumber()];

	public Table_AnHua(MJType type) {
		super(type);
	}

	public int get_magic_type(int[] cards_index) {
		int joker_count_1 = cards_index[joker_card_index_1];
		int joker_count_2 = 0;
		if (joker_card_index_2 != -1) {
			joker_count_2 = cards_index[joker_card_index_2];
		}

		if (joker_count_1 + joker_count_2 == 0) {
			return NONE_MAGIC;
		}
		if (joker_count_1 <= 2 && joker_count_2 <= 2) {
			return SINGLE_MAGIC;
		}
		if (joker_count_1 == 3 && joker_count_2 < 3) {
			return THREE_MAGIC;
		}
		if (joker_count_1 < 3 && joker_count_2 == 3) {
			return THREE_MAGIC;
		}
		if (joker_count_1 < 3 && joker_count_2 == 4) {
			return FOUR_MAGIC;
		}
		if (joker_count_1 == 3 && joker_count_2 == 3) {
			return SIX_MAGIC;
		}
		if (joker_count_1 == 3 && joker_count_2 == 4) {
			return SEVEN_MAGIC;
		}

		return NONE_MAGIC;
	}

	protected void exe_select_magic_card() {
		set_handler(_handler_select_magic_card);
		_handler_select_magic_card.exe(this);
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			room_player.setPao(_player_result.pao[i] < 0 ? 0 : _player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setIsTrustee(istrustee == null ? false : istrustee[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());
			room_player.setStatus(rplayer.getStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_AnHua();
		_handler_dispath_card = new HandlerDispatchCard_AnHua();
		_handler_gang = new HandlerGang_AnHua();
		_handler_out_card_operate = new HandlerOutCardOperate_AnHua();

		_handler_select_magic_card = new HandlerSelectMagicCard_AnHua();
		_handler_gang_dispatch_card = new HandlerGangDispatchCard_AnHua();
	}

	public boolean exe_gang_dispatch_card(int seat_index) {
		set_handler(_handler_gang_dispatch_card);

		_handler_gang_dispatch_card.reset_status(seat_index);
		_handler_gang_dispatch_card.exe(this);

		return true;
	}

	@Override
	protected boolean on_game_start() {
		joker_card_1 = 0;
		joker_card_index_1 = -1;
		joker_card_2 = 0;
		joker_card_index_2 = -1;
		ding_wang_card = 0;
		ding_wang_card_index = -1;

		_logic.clean_magic_cards();
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

		exe_select_magic_card();

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
				// 荒庄荒杠
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

			game_end.setBankerPlayer(_cur_banker);
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
					if (GRR._chi_hu_card[i][j] != 0 && (GRR._chi_hu_card[i][j] == joker_card_1 || GRR._chi_hu_card[i][j] == joker_card_2)) {
						hc.addItem(GRR._chi_hu_card[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					if (GRR._chi_hu_card[i][h] != 0 && (GRR._chi_hu_card[i][h] == joker_card_1 || GRR._chi_hu_card[i][h] == joker_card_2)) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
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
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (GRR._cards_data[i][j] == joker_card_1 || GRR._cards_data[i][j] == joker_card_2) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
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

		if (istrustee != null)
			Arrays.fill(istrustee, false);

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(cur_card)) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_HUA) {
			chiHuRight.opr_or(Constants_AnHua.CHR_GANG_SHANG_HUA);
		} else if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO) {
			chiHuRight.opr_or(Constants_AnHua.CHR_GANG_SHANG_PAO);
		} else if (card_type == Constants_AnHua.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or(Constants_AnHua.CHR_QIANG_GANG_HU);
		} else if (card_type == Constants_AnHua.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_AnHua.CHR_ZI_MO);
		} else if (card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_AnHua.CHR_JIE_PAO);
		}

		int magic_type = get_magic_type(cbCardIndexTemp);

		if (card_type == Constants_AnHua.HU_CARD_TYPE_QIANG_GANG_HU || card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO) {
			if (magic_type == THREE_MAGIC || magic_type == FOUR_MAGIC || magic_type == SIX_MAGIC || magic_type == SEVEN_MAGIC) {
				// 抢杠胡时 三王 四王 六王 七王 不能抢 其他都可以
				chiHuRight.set_empty();
				return 0;
			}
		}

		if (card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
			if (magic_type != SINGLE_MAGIC && magic_type != NONE_MAGIC) {
				// 三王、四王、六王、七王。不可以接炮
				chiHuRight.set_empty();
				return 0;
			}
		}

		boolean can_win = false;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) {
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 平胡
		boolean can_win_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		boolean can_win_ping_hu_ying = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		// 七小对
		boolean can_win_qi_xiao_dui = is_qi_xiao_dui(cbCardIndexTemp, weave_count);
		boolean can_win_qi_xiao_dui_ying = is_qi_xiao_dui_ying(cbCardIndexTemp, weave_count);

		if (card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
			if (magic_type == SINGLE_MAGIC && !can_win_ping_hu_ying && !can_win_qi_xiao_dui_ying) {
				// 单王。接炮，必须胡硬庄
				chiHuRight.set_empty();
				return 0;
			}
		}

		can_win = can_win_ping_hu || can_win_qi_xiao_dui;

		if (!can_win) {
			chiHuRight.set_empty();
			return 0;
		}

		// 清一色
		boolean can_win_qing_yi_se = is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count) && (can_win_ping_hu || can_win_qi_xiao_dui);
		boolean can_win_qing_yi_se_ying = is_qing_yi_se_ying(cbCardIndexTemp, weaveItems, weave_count)
				&& (can_win_ping_hu_ying || can_win_qi_xiao_dui_ying);

		int magic_count = _logic.magic_count(cbCardIndexTemp);

		int caculate_type = 0;
		if (!has_rule(Constants_AnHua.GAME_RULE_REPLACABLE_MAGIC) && magic_count > 0) {
			// 如果没勾选代王硬，牌型处理比较简单
			if (can_win_qi_xiao_dui)
				chiHuRight.opr_or(Constants_AnHua.CHR_XIAO_QI_DUI);
			if (can_win_qing_yi_se)
				chiHuRight.opr_or(Constants_AnHua.CHR_QING_YI_SE);

			if (magic_type == THREE_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_SAN_WANG);
			if (magic_type == FOUR_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_SI_WANG);
			if (magic_type == SIX_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_LIU_WANG);
			if (magic_type == SEVEN_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_QI_WANG);
		} else {
			int max_score = 0;
			int tmp_score = 0;

			if (can_win_ping_hu_ying && !can_win_qi_xiao_dui_ying && !can_win_qing_yi_se_ying
					&& AnalyseCardUtil.analyse_258_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0)) {
				ChiHuRight tmpChr = new ChiHuRight();
				tmpChr.set_empty();

				if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_HUA) {
					tmpChr.opr_or(Constants_AnHua.CHR_GANG_SHANG_HUA);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO) {
					tmpChr.opr_or(Constants_AnHua.CHR_GANG_SHANG_PAO);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_QIANG_GANG_HU) {
					tmpChr.opr_or(Constants_AnHua.CHR_QIANG_GANG_HU);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_ZI_MO) {
					tmpChr.opr_or(Constants_AnHua.CHR_ZI_MO);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
					tmpChr.opr_or(Constants_AnHua.CHR_JIE_PAO);
				}

				// 硬庄时，没王牌类型
				tmpChr.opr_or(Constants_AnHua.CHR_YING_ZHUANG);

				if (magic_type == THREE_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_SAN_WANG);
				if (magic_type == FOUR_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_SI_WANG);
				if (magic_type == SIX_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_LIU_WANG);
				if (magic_type == SEVEN_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_QI_WANG);

				tmp_score = get_pai_xing_fen(tmpChr);
				if (tmp_score > max_score) {
					max_score = tmp_score;
					caculate_type = 1;
				}
			} else if (can_win_qi_xiao_dui_ying || can_win_qing_yi_se_ying) {
				ChiHuRight tmpChr = new ChiHuRight();
				tmpChr.set_empty();

				if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_HUA) {
					tmpChr.opr_or(Constants_AnHua.CHR_GANG_SHANG_HUA);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO) {
					tmpChr.opr_or(Constants_AnHua.CHR_GANG_SHANG_PAO);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_QIANG_GANG_HU) {
					tmpChr.opr_or(Constants_AnHua.CHR_QIANG_GANG_HU);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_ZI_MO) {
					tmpChr.opr_or(Constants_AnHua.CHR_ZI_MO);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
					tmpChr.opr_or(Constants_AnHua.CHR_JIE_PAO);
				}

				// 硬庄时，没王牌类型
				tmpChr.opr_or(Constants_AnHua.CHR_YING_ZHUANG);
				if (can_win_qi_xiao_dui_ying)
					tmpChr.opr_or(Constants_AnHua.CHR_XIAO_QI_DUI);
				if (can_win_qing_yi_se_ying)
					tmpChr.opr_or(Constants_AnHua.CHR_QING_YI_SE);

				if (magic_type == THREE_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_SAN_WANG);
				if (magic_type == FOUR_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_SI_WANG);
				if (magic_type == SIX_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_LIU_WANG);
				if (magic_type == SEVEN_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_QI_WANG);

				tmp_score = get_pai_xing_fen(tmpChr);
				if (tmp_score > max_score) {
					max_score = tmp_score;
					caculate_type = 2;
				}
			} else {
				ChiHuRight tmpChr = new ChiHuRight();
				tmpChr.set_empty();

				if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_HUA) {
					tmpChr.opr_or(Constants_AnHua.CHR_GANG_SHANG_HUA);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO) {
					tmpChr.opr_or(Constants_AnHua.CHR_GANG_SHANG_PAO);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_QIANG_GANG_HU) {
					tmpChr.opr_or(Constants_AnHua.CHR_QIANG_GANG_HU);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_ZI_MO) {
					tmpChr.opr_or(Constants_AnHua.CHR_ZI_MO);
				} else if (card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
					tmpChr.opr_or(Constants_AnHua.CHR_JIE_PAO);
				}

				if (magic_type == THREE_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_SAN_WANG);
				if (magic_type == FOUR_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_SI_WANG);
				if (magic_type == SIX_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_LIU_WANG);
				if (magic_type == SEVEN_MAGIC)
					tmpChr.opr_or(Constants_AnHua.CHR_QI_WANG);

				if (can_win_qi_xiao_dui)
					tmpChr.opr_or(Constants_AnHua.CHR_XIAO_QI_DUI);
				if (can_win_qing_yi_se)
					tmpChr.opr_or(Constants_AnHua.CHR_QING_YI_SE);

				tmp_score = get_pai_xing_fen(tmpChr);
				if (tmp_score > max_score) {
					max_score = tmp_score;
					caculate_type = 3;
				}
			}

			if (caculate_type == 1) {
				// 硬庄时，没王牌类型
				chiHuRight.opr_or(Constants_AnHua.CHR_YING_ZHUANG);
			} else if (caculate_type == 2) {
				// 硬庄时，没王牌类型
				chiHuRight.opr_or(Constants_AnHua.CHR_YING_ZHUANG);
				if (can_win_qi_xiao_dui_ying)
					chiHuRight.opr_or(Constants_AnHua.CHR_XIAO_QI_DUI);
				if (can_win_qing_yi_se_ying)
					chiHuRight.opr_or(Constants_AnHua.CHR_QING_YI_SE);
			} else if (caculate_type == 3) {
				if (can_win_qi_xiao_dui)
					chiHuRight.opr_or(Constants_AnHua.CHR_XIAO_QI_DUI);
				if (can_win_qing_yi_se)
					chiHuRight.opr_or(Constants_AnHua.CHR_QING_YI_SE);
			}

			if (magic_type == THREE_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_SAN_WANG);
			if (magic_type == FOUR_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_SI_WANG);
			if (magic_type == SIX_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_LIU_WANG);
			if (magic_type == SEVEN_MAGIC)
				chiHuRight.opr_or(Constants_AnHua.CHR_QI_WANG);
		}

		if (chiHuRight.opr_and(Constants_AnHua.CHR_XIAO_QI_DUI).is_empty() && chiHuRight.opr_and(Constants_AnHua.CHR_QING_YI_SE).is_empty()) {
			if (magic_type == SINGLE_MAGIC && card_type == Constants_AnHua.HU_CARD_TYPE_JIE_PAO) {
				if (AnalyseCardUtil.analyse_258_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0)) {
					chiHuRight.opr_or(Constants_AnHua.CHR_PING_HU);
				} else {
					chiHuRight.set_empty();
					return 0;
				}
			} else {
				if (AnalyseCardUtil.analyse_258_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count)) {
					chiHuRight.opr_or(Constants_AnHua.CHR_PING_HU);
				} else {
					chiHuRight.set_empty();
					return 0;
				}
			}
		}

		return GameConstants.WIK_CHI_HU;
	}

	public int weave_magic_count(WeaveItem[] weaveItems, int weaveCount) {
		int count = 0;

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_LEFT || weaveItems[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItems[i].weave_kind == GameConstants.WIK_CENTER) {
				int[] weaveCards = new int[4];
				int weaveCardCount = _logic.get_weave_card(weaveItems[i].weave_kind, weaveItems[i].center_card, weaveCards);

				for (int j = 0; j < weaveCardCount; j++) {
					if (weaveCards[j] == joker_card_1 || weaveCards[j] == joker_card_2) {
						count++;
					}
				}
			}
		}

		return count;
	}

	public int get_pai_xing_fen(ChiHuRight chr) {
		int fan_shu = 0;

		if (!chr.opr_and(Constants_AnHua.CHR_QING_YI_SE).is_empty())
			fan_shu += 1;
		if (!chr.opr_and(Constants_AnHua.CHR_YING_ZHUANG).is_empty())
			fan_shu += 1;
		if (!chr.opr_and(Constants_AnHua.CHR_SAN_WANG).is_empty())
			fan_shu += 1;
		if (!chr.opr_and(Constants_AnHua.CHR_XIAO_QI_DUI).is_empty())
			fan_shu += 1;
		if (!chr.opr_and(Constants_AnHua.CHR_GANG_SHANG_HUA).is_empty())
			fan_shu += 1;
		if (!chr.opr_and(Constants_AnHua.CHR_QIANG_GANG_HU).is_empty())
			fan_shu += 1;
		if (!chr.opr_and(Constants_AnHua.CHR_GANG_SHANG_PAO).is_empty())
			fan_shu += 1;

		if (!chr.opr_and(Constants_AnHua.CHR_SI_WANG).is_empty())
			fan_shu += 2;
		if (!chr.opr_and(Constants_AnHua.CHR_LIU_WANG).is_empty())
			fan_shu += 2;

		if (!chr.opr_and(Constants_AnHua.CHR_QI_WANG).is_empty())
			fan_shu += 3;

		return (2 << fan_shu);
	}

	public boolean is_qing_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
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

	public boolean is_qing_yi_se_ying(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
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

	public boolean is_qi_xiao_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count == 1) {
				if (i == joker_card_index_1)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;
			} else if (magic_card_count == 2) {
				if (i == joker_card_index_1 || i == joker_card_index_2)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;
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

	public boolean is_qi_xiao_dui_ying(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;
		}

		if (cbReplaceCount > 0)
			return false;

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
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (cards[j] == joker_card_1 || cards[j] == joker_card_2) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
		}

		if (operate_card == joker_card_1 || operate_card == joker_card_2) {
			cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else {
			cards[hand_card_count] = operate_card;
		}
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int lChiHuScore = get_pai_xing_fen(chr);

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_s = s * (1 << (GRR._player_niao_count[i] + GRR._player_niao_count[seat_index]));

				GRR._game_score[i] -= tmp_s;
				GRR._game_score[seat_index] += tmp_s;
			}
		} else {
			int s = lChiHuScore;

			s *= 1 << (GRR._player_niao_count[provide_index] + GRR._player_niao_count[seat_index]);

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(Constants_AnHua.CHR_FANG_PAO);
		}
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
					if (type == Constants_AnHua.CHR_ZI_MO)
						result.append(" 自摸");
					if (type == Constants_AnHua.CHR_JIE_PAO)
						result.append(" 接炮");
					if (type == Constants_AnHua.CHR_GANG_SHANG_HUA)
						result.append(" 杠上花");
					if (type == Constants_AnHua.CHR_QIANG_GANG_HU)
						result.append(" 抢杠胡");
					if (type == Constants_AnHua.CHR_GANG_SHANG_PAO)
						result.append(" 杠上炮");
					if (type == Constants_AnHua.CHR_QING_YI_SE)
						result.append(" 清一色");
					if (type == Constants_AnHua.CHR_XIAO_QI_DUI)
						result.append(" 小七对");
					if (type == Constants_AnHua.CHR_YING_ZHUANG)
						result.append(" 硬庄");
					if (type == Constants_AnHua.CHR_SAN_WANG)
						result.append(" 三王");
					if (type == Constants_AnHua.CHR_SI_WANG)
						result.append(" 四王");
					if (type == Constants_AnHua.CHR_LIU_WANG)
						result.append(" 六王");
					if (type == Constants_AnHua.CHR_QI_WANG)
						result.append(" 七王");
				} else if (type == Constants_AnHua.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			if (GRR._player_niao_count[player] > 0) {
				result.append(" 中鸟x" + GRR._player_niao_count[player]);
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA && card < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_NEW_TING
				&& card < GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}

		return card;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_AnHua.HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
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

			if (GRR._left_card_count > 0 && type == GameConstants.DISPATCH_CARD_TYPE_NORMAL) {
				// 玩家吃碰后只剩下王牌，无法出牌，则这一轮不允许该玩家吃、碰操作
				int hand_card_count = _logic.get_card_count_by_index(GRR._cards_index[i]);
				int joker_count = GRR._cards_index[i][joker_card_index_1];
				if (joker_card_index_2 != -1) {
					joker_count += GRR._cards_index[i][joker_card_index_2];
				}

				// 杠之后打的牌，其他玩家只能胡，不能吃碰杠
				if ((seat_index + 1) % getTablePlayerNumber() == i && (joker_count + 2 < hand_card_count)) {
					action = _logic.check_chi_shuang_wang(GRR._cards_index[i], card);
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
				if (action != 0 && (joker_count + 2 < hand_card_count)) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					int tmp_card_index = _logic.switch_to_card_index(card);
					int tmp_card_count = GRR._cards_index[i][tmp_card_index];

					GRR._cards_index[i][tmp_card_index] = 0;

					boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

					GRR._cards_index[i][tmp_card_index] = tmp_card_count;

					if (is_ting_state_after_gang) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();

					int hu_card_type = Constants_AnHua.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.DISPATCH_CARD_TYPE_GANG)
						hu_card_type = Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_CHI_HU);
						playerStatus.add_chi_hu(card, seat_index);
						bAroseAction = true;
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

	public boolean estimate_gang_fa_pai(int seat_index, int card_count, int[] gang_cards, int[][] player_gang_cards) {
		boolean bAroseAction = false;
		int action = GameConstants.WIK_NULL;
		PlayerStatus playerStatus = null;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clear_hu_cards_of_xuan_mei();
		}

		// 对所有翻出来的牌，针对所有玩家进行胡牌分析
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			playerStatus = _playerStatus[p];
			// 零时清空一下最大牌型分
			max_win_score[p] = 0;
			win_card_at_gang[p] = -1;

			for (int i = 0; i < card_count; i++) {
				int card = gang_cards[i];

				// 对多张牌进行吃胡分析的时候，只保留胡牌分最大的那张牌
				ChiHuRight chr = new ChiHuRight();
				chr.set_empty();

				if (p == seat_index) {
					// 翻牌人对翻出来的牌，可以进行胡牌操作
					if (playerStatus.is_chi_hu_round()) {
						// 开杠人，杠上开花的胡牌分析判断
						action = analyse_chi_hu_card(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], card, chr,
								Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_HUA, p);

						if (action != GameConstants.WIK_NULL) {
							player_gang_cards[p][i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_WIN;
							// 牌型分最大的
							int tmp_pai_xing_fen = get_pai_xing_fen(chr);

							if (max_win_score[p] < tmp_pai_xing_fen) {
								// 存储牌值比较大的CHR和牌数据
								GRR._chi_hu_rights[p] = chr;
								win_card_at_gang[p] = card;
								max_win_score[p] = tmp_pai_xing_fen;
							}
						}
					}
				} else {
					// 非翻牌人对翻出来的牌，可以进行胡牌操作
					if (playerStatus.is_chi_hu_round() && !_logic.is_magic_card(card)) {
						// 非开杠人，对杠之后翻出来的牌，进行胡牌检测，走的和获取听牌数据一样的路径
						action = analyse_chi_hu_card(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], card, chr,
								Constants_AnHua.HU_CARD_TYPE_GANG_SHANG_PAO, p);

						if (action != GameConstants.WIK_NULL) {
							player_gang_cards[p][i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_WIN;
							// 牌型分最大的
							int tmp_pai_xing_fen = get_pai_xing_fen(chr);

							if (max_win_score[p] < tmp_pai_xing_fen) {
								// 存储牌值比较大的CHR和牌数据
								GRR._chi_hu_rights[p] = chr;
								win_card_at_gang[p] = card;
								max_win_score[p] = tmp_pai_xing_fen;
							}
						}
					}
				}
			}
		}

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == seat_index) {
				if (win_card_at_gang[p] != -1) {
					_playerStatus[p].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[p].add_zi_mo(win_card_at_gang[p], seat_index);
					bAroseAction = true;
				} else {
					// 开杠后，杠牌人，没胡牌，相当于进入自动托管
					istrustee[p] = true;
					operate_player_data();
				}
			} else {
				if (win_card_at_gang[p] != -1) {
					_playerStatus[p].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[p].add_chi_hu(win_card_at_gang[p], seat_index);
					bAroseAction = true;
				}
			}
		}

		return bAroseAction;
	}

	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;

		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);

		return seat;
	}

	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_AnHua.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
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

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_AnHua.HU_CARD_TYPE_QIANG_GANG_HU, i);

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

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = get_niao_card_num();

		if (GRR._count_niao > 0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			if (DEBUG_CARDS_MODE) {
				GRR._cards_data_niao[0] = 0x04;
			}

			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

			int seat = get_seat_by_value(nValue, seat_index);

			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];

			GRR._player_niao_count[seat]++;
		}

		// 设置鸟牌显示
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
			}
		}
	}

	@Override
	public int get_seat_by_value(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
			switch (nValue) {
			// 147
			case 1:
			case 4:
			case 7:
				seat = seat_index;
				break;
			// 258
			case 2:
			case 5:
			case 8:
				seat = get_banker_next_seat(seat_index);
				break;
			// 369
			case 3:
			case 6:
			case 9:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
			case 2:
				seat = seat_index;
				break;
			case 1:
			case 3:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	public int get_niao_card_num() {
		int nNum = 0;

		if (has_rule(Constants_AnHua.GAME_RULE_ONE_BIRD)) {
			nNum = 1;
		} else if (has_rule(Constants_AnHua.GAME_RULE_TWO_BIRD)) {
			nNum = 2;
		} else if (has_rule(Constants_AnHua.GAME_RULE_THREE_BIRD)) {
			nNum = 3;
		} else if (has_rule(Constants_AnHua.GAME_RULE_FOUR_BIRD)) {
			nNum = 4;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x24, 0x24, 0x24, 0x25, 0x25, 0x25, 0x25, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15 };
		int[] cards_of_player1 = new int[] { 0x04, 0x04, 0x04, 0x25, 0x25, 0x25, 0x26, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16 };
		int[] cards_of_player2 = new int[] { 0x15, 0x15, 0x03, 0x05, 0x07, 0x08, 0x09, 0x18, 0x18, 0x18, 0x19, 0x19, 0x25 };
		int[] cards_of_player3 = new int[] { 0x09, 0x09, 0x13, 0x02, 0x02, 0x02, 0x01, 0x01, 0x01, 0x12, 0x21, 0x22, 0x23 };

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
}
