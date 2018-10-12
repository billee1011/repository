package com.cai.game.mj.shanxi.linfeng;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.dictionary.SysParamDict;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.FengKanUtil;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

@ThreeDimension
public class Table_LinFeng extends AbstractMJTable {
	enum ChrString {
		GU_JIANG(UniversalConstants.U_CHR_LF_GU_JIANG, " 孤将+"), //
		YI_ZHANG_YING(UniversalConstants.U_CHR_LF_YI_ZHANG_YING, " 一张赢+"), //
		QUE_MEN(UniversalConstants.U_CHR_LF_QUE_MEN, " 缺门+"), //
		SAN_YUAN(UniversalConstants.U_CHR_LF_SAN_YUAN, " 三元+"), //
		SAN_FENG(UniversalConstants.U_CHR_LF_SAN_FENG, " 三风+"), //
		MEN_QING(UniversalConstants.U_CHR_LF_MEN_QING, " 门清+"), //
		QING_YI_SE(UniversalConstants.U_CHR_LF_QING_YI_SE, " 清一色+"), //
		YI_TIAO_LONG(UniversalConstants.U_CHR_LF_YI_TIAO_LONG, " 一条龙+"), //
		QI_XIAO_DUI(UniversalConstants.U_CHR_LF_QI_XIAO_DUI, " 七小对+"), //
		HAO_HUA_QXD(UniversalConstants.U_CHR_LF_HAO_HUA_QXD, " 豪华七小对+"), //
		SHUANG_HAO_HUA_QXD(UniversalConstants.U_CHR_LF_SHUANG_HAO_HUA_QXD, " 双豪华七小对+"), //
		ZI_YI_SE(UniversalConstants.U_CHR_LF_ZI_YI_SE, " 字一色+"), //
		QING_LONG(UniversalConstants.U_CHR_LF_QING_LONG, " 清龙+"), //
		QING_QI_DUI(UniversalConstants.U_CHR_LF_QING_QI_DUI, " 清七对+"), //
		;

		ChrString(long chr, String name) {
			this.chr = chr;
			this.name = name;
		}

		private long chr;
		private String name;

		public long getChr() {
			return chr;
		}

		public void setChr(long chr) {
			this.chr = chr;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private static final long serialVersionUID = -2456323602522819218L;

	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];

	public HandlerOutCardBaoTing_LinFeng _handler_out_card_bao_ting;

	public int analyse_state = 0;
	public final int ANALYSE_NORMAL = 1;
	public final int ANALYSE_TING = 2;

	public int[] zhua_pai_count = new int[getTablePlayerNumber()];

	/**
	 * 所有CHR类型对应的得分，Key：CHR值，Value：得分。因为只能自摸，所以可以不用数组去存。
	 */
	public Map<Long, Integer> scoreMap = new HashMap<>();

	public boolean hasRuleDisplayAnGang = false;

	public int pai_xing_fen = 0;

	public Table_LinFeng(MJType type) {
		super(type);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_LinFeng();
		_handler_dispath_card = new HandleDispatchCard_LinFeng();
		_handler_gang = new HandleGang_LinFeng();
		_handler_out_card_operate = new HandleOutCard_LinFeng();
		_handler_out_card_bao_ting = new HandlerOutCardBaoTing_LinFeng();

		for (ChrString chrstr : ChrString.values()) {
			if (!scoreMap.containsKey(chrstr.getChr()))
				scoreMap.put(chrstr.getChr(), 0);
		}

		hasRuleDisplayAnGang = has_rule(UniversalConstants.U_GAME_RULE_AN_GANG_KE_JIAN);
	}

	// 获取庄家上家的座位
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

	// 获取庄家下家的座位
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

	@Override
	protected boolean on_game_start() {
		is_bao_ting = new boolean[getTablePlayerNumber()];
		zhua_pai_count = new int[getTablePlayerNumber()];
		gang_dispatch_count = 0;
		pai_xing_fen = 0;

		reset_score_map();

		_game_status = UniversalConstants.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][UniversalConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < UniversalConstants.MAX_COUNT; j++) {
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
			roomResponse.setCurrentPlayer(_current_player == UniversalConstants.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < UniversalConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		exe_dispatch_card(_current_player, UniversalConstants.WIK_NULL, UniversalConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		set_handler(_handler_out_card_bao_ting);
		_handler_out_card_bao_ting.reset_status(seat_index, card, type);
		_handler.exe(this);

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
				if (GRR._end_type != UniversalConstants.Game_End_DRAW && GRR._win_order[i] != 0 && pai_xing_fen < 50) {
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
				// GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i] + lGangScore[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(UniversalConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < UniversalConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < UniversalConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < UniversalConstants.GAME_PLAYER; i++) {
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
				for (int j = 0; j < UniversalConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[UniversalConstants.MAX_RIGHT_COUNT];

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
				game_end.addGameScore(GRR._game_score[i] + lGangScore[i]); // 总分
				game_end.addGangScore(lGangScore[i]); // 杠分
				game_end.addStartHuScore((int) GRR._game_score[i]); // 胡牌分
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
		if (reason == UniversalConstants.Game_End_NORMAL || reason == UniversalConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == UniversalConstants.Game_End_RELEASE_PLAY || reason == UniversalConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == UniversalConstants.Game_End_RELEASE_RESULT || reason == UniversalConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == UniversalConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == UniversalConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = UniversalConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == UniversalConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == UniversalConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

	public void reset_score_map() {
		for (ChrString chrstr : ChrString.values()) {
			scoreMap.replace(chrstr.getChr(), 0);
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (analyse_state == ANALYSE_NORMAL)
			reset_score_map();

		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(cur_card)) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		// 平胡
		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		// 缺门
		int que_men_count = 3 - _logic.get_se_count(cbCardIndexTemp, weaveItems, weave_count);
		// 七小对
		long qi_xiao_dui_result = check_qi_xiao_dui(cbCardIndexTemp, weaveItems, weave_count, cur_card);
		boolean can_qi_xiao_dui = (qi_xiao_dui_result != 0) && que_men_count >= 2;

		if (!can_win && !can_qi_xiao_dui) {
			chiHuRight.set_empty();
			return 0;
		}

		if (analyse_state == ANALYSE_TING) {
			if (can_qi_xiao_dui) {
				return UniversalConstants.WIK_CHI_HU;
			}

			// 缺门的有效组数
			que_men_count = que_men_count >= 2 ? 3 : que_men_count;
			if (que_men_count == 3) {
				return UniversalConstants.WIK_CHI_HU;
			}

			int[] feng_kan = new int[2];
			FengKanUtil.getFengKanCount(cbCardIndexTemp, feng_kan, false);

			// 三元、三风的有效组数
			int san_feng_count = feng_kan[0];
			int san_yuan_count = feng_kan[1];
			if (san_yuan_count >= 2 || san_feng_count >= 2) {
				return UniversalConstants.WIK_CHI_HU;
			}

			// 一张赢的有效组数
			int yi_zhang_count = is_yi_zhang_ying(cards_index, weaveItems, weave_count) ? 1 : 0;

			if (san_yuan_count == 1 && (cur_card == 0x35 || cur_card == 0x36 || cur_card == 0x37)) {
				// 如果刚好是中发白的两张牌上听 计算三元之后 不再计算一张赢
				san_yuan_count++;
				yi_zhang_count = 0;
			}

			if (san_yuan_count + san_feng_count + que_men_count + yi_zhang_count >= 3) {
				return UniversalConstants.WIK_CHI_HU;
			}

			// 孤将的有效组数
			int gu_jiang_count = is_gu_jiang(cbCardIndexTemp, weaveItems, weave_count) ? 1 : 0;

			if (san_yuan_count + san_feng_count + que_men_count + yi_zhang_count + gu_jiang_count >= 3) {
				return UniversalConstants.WIK_CHI_HU;
			}

			chiHuRight.set_empty();
			return 0;
		} else if (analyse_state == ANALYSE_NORMAL) {
			// 门清
			boolean can_men_qing = _logic.is_men_qing_b(weaveItems, weave_count);
			// 清一色
			boolean can_qing_yi_se = is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count);
			// 一条龙
			boolean can_yi_tiao_long = _logic.is_yi_tiao_long_fc(cbCardIndexTemp, weave_count);
			// 七小对
			// 豪华七小对
			boolean can_hh_qi_xiao_dui = can_qi_xiao_dui && (qi_xiao_dui_result == UniversalConstants.U_CHR_LF_HAO_HUA_QXD);
			// 双豪华七小对
			boolean can_shh_qi_xiao_dui = can_qi_xiao_dui && (qi_xiao_dui_result == UniversalConstants.U_CHR_LF_SHUANG_HAO_HUA_QXD);
			// 字一色
			boolean can_zi_yi_se = is_zi_yi_se(cbCardIndexTemp, weaveItems, weave_count);
			// 清龙
			boolean can_qing_long = can_qing_yi_se && can_yi_tiao_long;
			// 清七对
			boolean can_qing_qi_dui = can_qi_xiao_dui && can_qing_yi_se;

			if (can_qing_qi_dui) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_QING_QI_DUI);
				scoreMap.replace(UniversalConstants.U_CHR_LF_QING_QI_DUI, 50);
				return UniversalConstants.WIK_CHI_HU;
			}
			if (can_qing_long) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_QING_LONG);
				scoreMap.replace(UniversalConstants.U_CHR_LF_QING_LONG, 50);
				return UniversalConstants.WIK_CHI_HU;
			}
			if (can_zi_yi_se) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_ZI_YI_SE);
				scoreMap.replace(UniversalConstants.U_CHR_LF_ZI_YI_SE, 50);
				return UniversalConstants.WIK_CHI_HU;
			}
			if (can_shh_qi_xiao_dui) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_SHUANG_HAO_HUA_QXD);
				scoreMap.replace(UniversalConstants.U_CHR_LF_SHUANG_HAO_HUA_QXD, 50);
				return UniversalConstants.WIK_CHI_HU;
			}

			int[] feng_kan = new int[2];
			FengKanUtil.getFengKanCount(cbCardIndexTemp, feng_kan, false);

			// 三风数目
			int san_feng_count = feng_kan[0];
			// 三元数目
			int san_yuan_count = feng_kan[1];

			if (san_yuan_count >= 3) {
				scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_YUAN, 50);
				return UniversalConstants.WIK_CHI_HU;
			}
			if (san_feng_count == 4) {
				scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_FENG, 50);
				return UniversalConstants.WIK_CHI_HU;
			}

			if (can_hh_qi_xiao_dui) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_HAO_HUA_QXD);
				scoreMap.replace(UniversalConstants.U_CHR_LF_HAO_HUA_QXD, 25);
				san_feng_count = 0;
				san_yuan_count = 0;
			}
			if (can_qi_xiao_dui && !can_hh_qi_xiao_dui) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_QI_XIAO_DUI);
				scoreMap.replace(UniversalConstants.U_CHR_LF_QI_XIAO_DUI, 10);
				san_feng_count = 0;
				san_yuan_count = 0;
			}
			if (can_yi_tiao_long) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_YI_TIAO_LONG);
				scoreMap.replace(UniversalConstants.U_CHR_LF_YI_TIAO_LONG, 10);
			}
			if (can_qing_yi_se) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_QING_YI_SE);
				scoreMap.replace(UniversalConstants.U_CHR_LF_QING_YI_SE, 10);
			}
			if (can_men_qing) {
				chiHuRight.opr_or(UniversalConstants.U_CHR_LF_MEN_QING);
				scoreMap.replace(UniversalConstants.U_CHR_LF_MEN_QING, 1);
			}
			if (san_feng_count > 0) {
				if (san_feng_count == 3)
					scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_FENG, 10);
				else if (san_feng_count == 2)
					scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_FENG, 3);
				else if (san_feng_count == 1)
					scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_FENG, 1);
			}
			if (que_men_count > 0) {
				if (que_men_count >= 2)
					scoreMap.replace(UniversalConstants.U_CHR_LF_QUE_MEN, 3);
				else if (que_men_count >= 1)
					scoreMap.replace(UniversalConstants.U_CHR_LF_QUE_MEN, 1);
			}
			if (is_gu_jiang(cbCardIndexTemp, weaveItems, weave_count) && qi_xiao_dui_result == 0) {
				scoreMap.replace(UniversalConstants.U_CHR_LF_GU_JIANG, 1);
			}

			if (_playerStatus[_seat_index]._hu_card_count == 1) {
				scoreMap.replace(UniversalConstants.U_CHR_LF_YI_ZHANG_YING, 1);
			}

			boolean special = false;
			if (san_yuan_count > 0) {
				special = (cur_card == 0x35 || cur_card == 0x36 || cur_card == 0x37) ? true : false;
				if (san_yuan_count == 2) {
					if (special) {
						scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_YUAN, 10);
						scoreMap.replace(UniversalConstants.U_CHR_LF_YI_ZHANG_YING, 0);
					} else {
						scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_YUAN, 5);
					}
				} else if (san_yuan_count == 1) {
					if (special) {
						scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_YUAN, 2);
						scoreMap.replace(UniversalConstants.U_CHR_LF_YI_ZHANG_YING, 0);
					} else {
						scoreMap.replace(UniversalConstants.U_CHR_LF_SAN_YUAN, 1);
					}
				}
			}
		}

		return UniversalConstants.WIK_CHI_HU;
	}

	public boolean is_yi_zhang_ying(int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount) {
		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int count = 0;
		for (int i = 0; i < UniversalConstants.MAX_ZI_FENG; i++) {
			cbCardIndexTemp[i]++;
			if (cbCardIndexTemp[i] <= 4 && analyse_chi_hu_card_simple(cbCardIndexTemp, weaveItem, cbWeaveCount)) {
				count++;
				if (count > 1) {
					return false;
				}
			}
			cbCardIndexTemp[i]--;
		}

		if (count == 1)
			return true;

		return false;
	}

	public boolean analyse_chi_hu_card_simple(int[] cards_index, WeaveItem[] weaveItems, int weave_count) {
		boolean can_win = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, -1, null, 0);
		int que_men_count = 3 - _logic.get_se_count(cards_index, weaveItems, weave_count);
		boolean qi_xiao_dui_result = is_qi_xiao_dui(cards_index, weaveItems, weave_count);
		boolean can_qi_xiao_dui = qi_xiao_dui_result && que_men_count >= 2;
		if (can_win || can_qi_xiao_dui)
			return true;
		return false;
	}

	public boolean is_zi_yi_se(final int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			int color = _logic.get_card_color(weaveItem[i].center_card);
			if (color < 3)
				return false;
		}

		return true;
	}

	public boolean is_gu_jiang(int[] tmp_cards_index, WeaveItem[] weaveItems, int weave_count) {
		int[] cards_index = Arrays.copyOf(tmp_cards_index, tmp_cards_index.length);
		for (int i = 0; i < weave_count; i++) {
			int card = weaveItems[i].center_card;
			int index = _logic.switch_to_card_index(card);
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG)
				cards_index[index] += 4;
			else
				cards_index[index] += 3;
		}
		int count = 0;
		for (int i = 0; i < 9; i++) {
			count += cards_index[i];
			if (count > 2)
				break;
		}
		if (count == 2)
			return true;
		count = 0;
		for (int i = 9; i < 18; i++) {
			count += cards_index[i];
			if (count > 2)
				break;
		}
		if (count == 2)
			return true;
		count = 0;
		for (int i = 18; i < 27; i++) {
			count += cards_index[i];
			if (count > 2)
				break;
		}
		if (count == 2)
			return true;
		return false;
	}

	@Override
	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == UniversalConstants.INVALID_SEAT) {
			return false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = getGame_id() == 0 ? 1 : getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == UniversalConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}

			send_response_to_player(seat_index, roomResponse);

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(UniversalConstants.BLACK_CARD);
			}

			send_response_to_other(seat_index, roomResponse);
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(UniversalConstants.BLACK_CARD);
				}
			}

			send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].public_card == 0 && !hasRuleDisplayAnGang) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		roomResponse.clearWeaveItems();

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].public_card == 0 && !hasRuleDisplayAnGang) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		roomResponse.clearWeaveItems();

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];

			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + UniversalConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}

			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean is_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & UniversalConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weaveItem[0].center_card & UniversalConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & UniversalConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	public boolean is_qi_xiao_dui(final int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;

		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;
		}

		if (cbReplaceCount > 0)
			return false;

		return true;
	}

	public long check_qi_xiao_dui(final int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return 0;

		int cbReplaceCount = 0;
		int gengCount = 0;
		boolean special = false;

		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				gengCount++;
				if (i == _logic.switch_to_card_index(cur_card))
					special = true;
			}
		}

		if (cbReplaceCount > 0)
			return 0;

		if (gengCount >= 2 && special)
			return UniversalConstants.U_CHR_LF_SHUANG_HAO_HUA_QXD;

		if (gengCount >= 1 && special)
			return UniversalConstants.U_CHR_LF_HAO_HUA_QXD;

		return UniversalConstants.U_CHR_LF_QI_XIAO_DUI;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		if (effect_count == 0) {
			effect_count = 1;
			effect_indexs = new long[1];
			effect_indexs[0] = UniversalConstants.U_CHR_ZI_MO;
		}

		operate_effect_action(seat_index, UniversalConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, UniversalConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + UniversalConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, UniversalConstants.Show_Card_HU, hand_card_count, cards, UniversalConstants.INVALID_SEAT);

		return;
	}

	public int get_pai_xing_fen() {
		int pai_xing_feng = 0;
		if (scoreMap != null) {
			for (ChrString chrstr : ChrString.values()) {
				pai_xing_feng += scoreMap.get(chrstr.getChr());
			}
		}
		return pai_xing_feng;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		int pai_xing_fen = get_pai_xing_fen();

		int lChiHuScore = pai_xing_fen * UniversalConstants.CELL_SCORE;

		this.pai_xing_fen = pai_xing_fen;

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

			boolean hasRule = has_rule(UniversalConstants.U_GAME_RULE_ZHUANG_JIA_LIANG_FEN);
			boolean alreadyAdded = false;
			if (seat_index == GRR._banker_player && hasRule) {
				s += 2;
				alreadyAdded = true;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmpS = s;
				if (!alreadyAdded) {
					if (i == GRR._banker_player && hasRule) {
						tmpS += 2;
					} else if (i != GRR._banker_player && hasRule) {
						tmpS += 1;
					}
				}

				tmpS = tmpS > 50 ? 50 : tmpS;
				if (tmpS == 50)
					this.pai_xing_fen = 50;

				GRR._game_score[i] -= tmpS;
				GRR._game_score[seat_index] += tmpS;
			}
		}
	}

	@Override
	protected void set_result_describe() {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			if (GRR._win_order[player] != 0) {
				for (ChrString chrstr : ChrString.values()) {
					int score = scoreMap.get(chrstr.getChr());
					if (score > 0) {
						result.append(chrstr.getName() + score);
					}
				}
			}

			// 荒庄荒杠
			if (GRR._end_type != UniversalConstants.Game_End_DRAW) {
				int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

				if (GRR != null) {
					for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
						for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
							if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
								continue;
							}
							if (tmpPlayer == player) {
								if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
									jie_gang++;
								} else {
									if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
										ming_gang++;
									} else {
										an_gang++;
									}
								}
							} else {
								if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
									fang_gang++;
								}
							}
						}
					}
				}

				if (an_gang > 0) {
					result.append(" 暗杠*" + an_gang);
				}
				if (ming_gang > 0) {
					result.append(" 明杠*" + ming_gang);
				}
				if (fang_gang > 0) {
					result.append(" 放杠*" + fang_gang);
				}
				if (jie_gang > 0) {
					result.append(" 接杠*" + jie_gang);
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public int get_real_card(int card) {
		if (card > UniversalConstants.CARD_ESPECIAL_TYPE_TING && card < UniversalConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= UniversalConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < UniversalConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			analyse_state = ANALYSE_TING;
			if (UniversalConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					UniversalConstants.U_HU_CARD_TYPE_ZI_MO, seat_index)) {
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

			if (GRR._left_card_count > 0) {
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
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;

		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);

		return seat;
	}

	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index) {
		analyse_state = ANALYSE_TING;

		int cbCardIndexTemp[] = new int[UniversalConstants.MAX_INDEX];
		for (int i = 0; i < UniversalConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = UniversalConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (UniversalConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					UniversalConstants.U_HU_CARD_TYPE_ZI_MO, seat_index)) {
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
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player1 = new int[] { 0x12, 0x12, 0x12, 0x12, 0x13, 0x13, 0x13, 0x13, 0x14, 0x14, 0x14, 0x16, 0x15 };
		int[] cards_of_player2 = new int[] { 0x06, 0x07, 0x08, 0x15, 0x15, 0x02, 0x03, 0x08, 0x09, 0x13, 0x14, 0x28, 0x28 };
		int[] cards_of_player0 = new int[] { 0x15, 0x15, 0x03, 0x05, 0x07, 0x08, 0x09, 0x18, 0x18, 0x18, 0x19, 0x19, 0x25 };
		int[] cards_of_player3 = new int[] { 0x09, 0x09, 0x13, 0x02, 0x02, 0x02, 0x01, 0x01, 0x01, 0x12, 0x21, 0x22, 0x23 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < UniversalConstants.MAX_INDEX; j++) {
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

		// BACK_DEBUG_CARDS_MODE = true;
		// debug_my_cards = new int[] { 36, 51, 34, 17, 2, 17, 2, 52, 17, 55,
		// 41, 8, 55, 18, 54, 24, 50, 23, 36, 21, 18, 19, 20, 51, 9, 55, 49, 39,
		// 21,
		// 53, 54, 35, 1, 55, 25, 52, 1, 37, 19, 21, 33, 23, 38, 7, 7, 21, 49,
		// 40, 8, 4, 39, 7, 6, 37, 53, 9, 50, 3, 3, 1, 6, 35, 33, 20, 22, 8,
		// 22, 8, 41, 24, 53, 19, 37, 6, 40, 1, 7, 18, 4, 9, 5, 39, 51, 35, 2,
		// 38, 36, 6, 34, 35, 25, 5, 20, 49, 24, 34, 23, 9, 3, 4, 50, 54, 51,
		// 41, 19, 5, 38, 25, 3, 38, 22, 34, 54, 33, 25, 18, 52, 39, 17, 23, 40,
		// 50, 36, 40, 52, 24, 49, 22, 41, 33, 4, 2, 20, 5, 53, 37 };

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
