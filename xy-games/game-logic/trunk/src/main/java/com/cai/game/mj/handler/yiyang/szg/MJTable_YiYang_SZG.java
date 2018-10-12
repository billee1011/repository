package com.cai.game.mj.handler.yiyang.szg;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_YiYang_SZG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.game.mj.MJType;
import com.cai.game.mj.NewAbstractMjTable;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_YiYang_SZG extends NewAbstractMjTable {

	private static final long serialVersionUID = 1L;

	public boolean[] has_caculated_xi_fen = new boolean[getTablePlayerNumber()];

	// 处理闲家起手报听之后再发牌
	protected MJHandlerQiShouBaoTing_YiYang_SZG _handler_qi_shou_bao_ting;
	// 庄家闲家第一次出牌之后，处理报听
	protected MJHandlerChuPaiBaoTing_YiYang_SZG _handler_chu_pai_bao_ting;

	// 玩家抓的牌的数目，用来处理报听
	public int[] zhua_pai_count = new int[getTablePlayerNumber()];

	// 玩家起手是否听牌，用来处理‘报听’
	public boolean[] qi_shou_ting = new boolean[getTablePlayerNumber()];

	// 玩家是的点击了‘报听’
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];

	// 每个玩家的出牌数量
	public int[] chu_pai_count = new int[getTablePlayerNumber()];

	// 每个玩家出的第一张牌
	public int[] first_out_card = new int[getTablePlayerNumber()];

	// 报听状态下，接炮点了过
	public boolean[] ting_state_pass_jie_pao = new boolean[getTablePlayerNumber()];

	/**
	 * 记录报听之后，玩家接炮过胡的牌
	 */
	public int[][] bt_guo_pai = new int[getTablePlayerNumber()][GameConstants.MAX_ZI];

	// 喜钱
	public int[] happy_win_score = new int[getTablePlayerNumber()];

	// 跟庄的分
	public int[] gen_zhuang_fen = new int[getTablePlayerNumber()];

	/**
	 * 是否是海底牌
	 */
	public boolean is_hai_di_pai = false;
	/**
	 * 海底牌
	 */
	public int hai_di_card = -1;

	/**
	 * 游戏结束的延时，毫秒
	 */
	public int YY_GAME_FINISH_DELAY = 2500;

	/**
	 * 胡牌时玩家的输赢牌型分
	 */
	public int[] pai_xing_fen = new int[getTablePlayerNumber()];

	public MJTable_YiYang_SZG() {
		super(MJType.GAME_TYPE_HUNAN_YIYANG_SZG);
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	// 是否听牌
	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_card_from_ting(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_YiYang_SZG();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YiYang_SZG();
		_handler_gang = new MJHandlerGang_YiYang_SZG();
		_handler_chi_peng = new MJHandlerChiPeng_YiYang_SZG();

		_handler_qi_shou = new MJHandlerQiShou_YiYang_SZG();

		_handler_qi_shou_bao_ting = new MJHandlerQiShouBaoTing_YiYang_SZG();
		_handler_chu_pai_bao_ting = new MJHandlerChuPaiBaoTing_YiYang_SZG();
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
	protected boolean on_game_start() {
		return false;
	}

	public boolean is_out_card_first_round() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (chu_pai_count[i] != 1) {
				return false;
			}
		}

		return true;
	}

	public boolean is_same_out_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (first_out_card[i] == 0) {
				return false;
			}
			for (int j = i + 1; j < getTablePlayerNumber(); j++) {
				if (first_out_card[i] != first_out_card[j]) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public boolean on_game_start_new() {
		has_caculated_xi_fen = new boolean[getTablePlayerNumber()];
		zhua_pai_count = new int[getTablePlayerNumber()];
		qi_shou_ting = new boolean[getTablePlayerNumber()];
		is_bao_ting = new boolean[getTablePlayerNumber()];
		chu_pai_count = new int[getTablePlayerNumber()];
		first_out_card = new int[getTablePlayerNumber()];
		bt_guo_pai = new int[getTablePlayerNumber()][GameConstants.MAX_ZI];
		ting_state_pass_jie_pao = new boolean[getTablePlayerNumber()];
		happy_win_score = new int[getTablePlayerNumber()];
		is_hai_di_pai = false;
		hai_di_card = -1;
		gen_zhuang_fen = new int[getTablePlayerNumber()];
		pai_xing_fen = new int[getTablePlayerNumber()];

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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				continue;
			}

			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);

			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);

				// TODO 闲家起手能听牌
				qi_shou_ting[i] = true;
			}
		}

		exe_qi_shou(GRR._banker_player, GameConstants.WIK_NULL);

		return true;
	}

	public void display_bird_cards() {
		if (GRR != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DISPLAY_BIRD_CARDS);

			GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

			// 设置定鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			// 设置飞鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}

			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei); // 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}

				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}

				game_end.addPlayerNiaoCards(pnc);
			}

			roomResponse.setGameEnd(game_end);

			send_response_to_room(roomResponse);
		}
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		// 流局，罚款
		if (real_reason == GameConstants.Game_End_DRAW) {
			kou_fen();
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			// for (int i = 0; i < GRR._especial_card_count; i++) {
			// game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			// }
			if (is_hai_di_pai) {
				game_end.addEspecialShowCards(hai_di_card);
			}

			int card_count = _repertory_card.length;
			for (int i = 0; i < GRR._left_card_count; i++) {
				game_end.addCardsList(_repertory_card[card_count - 1 - GRR._left_card_count + i]);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k]; // 杠牌，每个人的分数
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置定鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			// 设置飞鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}

			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei); // 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

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
					// 这里有个特殊的bug，抓牌时有胡牌有暗杠，点了胡牌，小结算会多显示一个暗杠，所有把‘GameConstants.MAX_WEAVE’改成‘GRR._weave_count[i]’
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv); // 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv); // 获取权位数值
				game_end.addStartHuRight(rv[0]);
				game_end.addProvidePlayer(GRR._provider[i]);

				if (is_mj_type(GameConstants.GAME_TYPE_THJ_YI_YANG_SGZ)) {
					// 喜分
					game_end.addJettonScore(happy_win_score[i]);
					// 跟庄
					game_end.addCardType(gen_zhuang_fen[i]);
				}

				// 总结算
				game_end.addGameScore(GRR._game_score[i] + happy_win_score[i] + gen_zhuang_fen[i]);
				// 杠分
				game_end.addGangScore(lGangScore[i]);
				// 胡分
				game_end.addStartHuScore(pai_xing_fen[i]);

				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
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
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
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

		// 超时解散
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

		return true;
	}

	@Override
	public int analyse_qi_shou_hu_pai(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO || card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU || card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		// 有大胡
		boolean can_big_win = false;

		if (GameConstants.HU_CARD_TYPE_ZIMO == card_type && has_rule(GameConstants_YiYang_SZG.GAME_RULE_BAN_BAN_HU)) {
			if (weaveCount == 0 && banBanHu(cbCardIndexTemp) && (zhua_pai_count[_seat_index] == 0 || zhua_pai_count[_seat_index] == 1)) {
				// 板板胡，无258将牌
				chiHuRight.opr_or(GameConstants.CHR_BAN_BAN_HU);
				can_big_win = true;
			}
		}

		// 七对
		int qiXiaoDui = is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weaveCount);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qiXiaoDui);
			can_big_win = true;
		}

		// 将将胡
		if (_logic.is_jiangjiang_hu_qishou(cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
			can_big_win = true;
		}

		// 清一色
		if (_logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weaveCount)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_MEN_QING)) {
			// 门清
			if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		// 碰碰胡
		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		if (can_peng_hu) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			can_big_win = true;
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_YI_TIAO_LONG)) {
			// 一条龙
			if (can_win) {
				if (_logic.is_yi_tiao_long(cbCardIndexTemp, weaveCount)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_YI_TIAO_LONG);
					can_big_win = true;
				}
			}
		}

		if (can_win || can_big_win) {
			// 能胡平胡或者大胡
			return GameConstants.WIK_CHI_HU;
		}

		return GameConstants.WIK_NULL;
	}

	public int analyse_card_from_ting(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
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

		// 有大胡
		boolean can_big_win = false;

		// 七对
		int qiXiaoDui = is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weaveCount);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			can_big_win = true;
		}

		// 将将胡
		if (_logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card)) {
			can_big_win = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		// 碰碰胡
		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		if (can_peng_hu) {
			can_big_win = true;
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_YI_TIAO_LONG)) {
			// 一条龙
			if (can_win_with_magic) {
				if (_logic.is_yi_tiao_long(cbCardIndexTemp, weaveCount)) {
					can_big_win = true;
				}
			}
		}

		if (can_win_with_magic || can_big_win) {
			// 能胡平胡或者大胡
			return GameConstants.WIK_CHI_HU;
		}

		return GameConstants.WIK_NULL;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
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

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO || card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU || card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		// 有大胡
		boolean can_big_win = false;
		// 能接炮
		boolean can_jie_pao = false;

		if (GameConstants.HU_CARD_TYPE_ZIMO == card_type && has_rule(GameConstants_YiYang_SZG.GAME_RULE_BAN_BAN_HU)) {
			if (weaveCount == 0 && banBanHu(cbCardIndexTemp) && ((zhua_pai_count[_seat_index] == 0 && _seat_index == GRR._banker_player)
					|| (zhua_pai_count[_seat_index] == 1 && _seat_index != GRR._banker_player))) {
				// 板板胡，无258将牌
				chiHuRight.opr_or(GameConstants.CHR_BAN_BAN_HU);
				can_big_win = true;
			}
		}

		// 七对
		int qiXiaoDui = is_qi_xiao_dui(cbCardIndexTemp, weaveItems, weaveCount);
		if (qiXiaoDui != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(qiXiaoDui);
			can_big_win = true;
			can_jie_pao = true;
		}

		// 清一色
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			can_jie_pao = true;
		}

		// 全求人
		if (is_quan_qiu_ren(cbCardIndexTemp)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QUAN_QIU_REN);
			can_jie_pao = true;
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_MEN_QING)) {
			// 门清
			if (GameConstants.HU_CARD_TYPE_PAOHU != card_type && GameConstants.HU_CARD_TYPE_QIANGGANG != card_type) {
				// 接炮不能算门清了
				if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
				}
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index, 0);

		// 碰碰胡
		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				0);
		if (can_peng_hu) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
			can_big_win = true;
			can_jie_pao = true;
		}

		if (can_peng_hu && card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			// 接炮时，有碰胡，就不判断将将胡了
		} else {
			// 将将胡
			if (_logic.is_jiangjiang_hu(cards_index, weaveItems, weaveCount, cur_card)) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_JIANGJIANG_HU);
				can_big_win = true;

				if (_logic.is_men_qing(weaveItems, weaveCount) == GameConstants.CHR_HUNAN_MEN_QING) {
					// 将将胡，门清的牌型才能接炮，有碰有明杠都不能接炮
					can_jie_pao = true;
				}
			}
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_YI_TIAO_LONG)) {
			// 一条龙
			if (can_win) {
				if (_logic.is_yi_tiao_long(cbCardIndexTemp, weaveCount)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_YI_TIAO_LONG);
					can_big_win = true;
					can_jie_pao = true;
				}
			}
		}

		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			// 接炮时，必须是接炮的牌型或者已经报听
			if (is_bao_ting[_seat_index] || can_jie_pao) {
				if (can_win || can_big_win) {
					// 能胡平胡或者大胡
					return GameConstants.WIK_CHI_HU;
				} else {
					chiHuRight.set_empty();
					return GameConstants.WIK_NULL;
				}
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		} else {
			if (can_win || can_big_win) {
				// 能胡平胡或者大胡
				return GameConstants.WIK_CHI_HU;
			}
		}

		return GameConstants.WIK_NULL;
	}

	public boolean is_quan_qiu_ren(int cbCardIndexTemp[]) {
		int card_count = _logic.get_card_count_by_index(cbCardIndexTemp);
		if (card_count == 2) {
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if (cbCardIndexTemp[i] == 2) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean banBanHu(int cbCardIndexTemp[]) {
		if (cbCardIndexTemp[1] != 0 || cbCardIndexTemp[4] != 0 || cbCardIndexTemp[7] != 0)
			return false;

		if (cbCardIndexTemp[10] != 0 || cbCardIndexTemp[13] != 0 || cbCardIndexTemp[16] != 0)
			return false;

		if (cbCardIndexTemp[19] != 0 || cbCardIndexTemp[22] != 0 || cbCardIndexTemp[25] != 0)
			return false;

		return true;
	}

	@Override
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int seatIndex) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_card_from_ting(cards_index, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seatIndex)) {
				cards[count++] = cbCurrentCard;
			}
		}

		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		int tmp_pai_xing_fen = getScore(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = tmp_pai_xing_fen;

				pai_xing_fen[i] -= s;
				pai_xing_fen[seat_index] += s;

				int niao = GRR._count_pick_niao + GRR._player_niao_count_fei[seat_index] + GRR._player_niao_count_fei[i];
				if (niao > 0) {
					s *= 1 << niao;
				}

				s = s > getFenDing() ? getFenDing() : s;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = tmp_pai_xing_fen;

			pai_xing_fen[provide_index] -= s;
			pai_xing_fen[seat_index] += s;

			int niao = GRR._count_pick_niao + GRR._player_niao_count_fei[seat_index] + GRR._player_niao_count_fei[provide_index];
			if (niao > 0) {
				s *= 1 << niao;
			}

			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_FAN_PAO_FAN_BEI)) {
				// 如果有放炮翻倍玩法
				s = s * 2;
			}

			s = s > getFenDing() ? getFenDing() : s;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;

		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	public boolean check_gang_huan_zhang(int seat_index, int card) {
		// 不能换章，需要检测是否改变了听牌
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index);
		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != hu_cards[j]) {
					return true;
				}
			}
		}

		return false;
	}

	private void kou_fen() {
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_NON_TING_FA_FEN)) {
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				if (_playerStatus[player]._hu_card_count > 0) {
					continue;
				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == player) {
						continue;
					}

					// 胡牌分
					GRR._game_score[i] += 2;
					GRR._game_score[player] -= 2;
				}
			}
		}
	}

	private int getFenDing() {
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_FENG_DING_32)) {
			return 32;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_FENG_DING_64)) {
			return 64;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_FENG_DING_128)) {
			return 128;
		}
		return 32;
	}

	private int getScore(ChiHuRight chr, int seat_index) {
		int s = 1;

		int da_hu_count = get_da_hu_count(chr);
		int special_da_hu_count = get_special_da_hu_count(chr);
		int total_count = da_hu_count + special_da_hu_count;

		if (is_bao_ting[seat_index]) {
			total_count += 1;
			if (!chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING).is_empty()) {
				total_count += 1;
			}

			s = 2 * (1 << total_count);
		} else {
			if (total_count == 0) {
				if (!chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING).is_empty()) {
					s += 1;
				}
			} else {
				if (!chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING).is_empty()) {
					total_count += 1;
				}

				s = 2 * (1 << total_count);
			}
		}

		return s;
	}

	private int get_special_da_hu_count(ChiHuRight chr) {
		int count = 0;

		if (!chr.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI).is_empty()) {
			count += 1;
		}

		return count;
	}

	private int get_da_hu_count(ChiHuRight chr) {
		int count = 0;

		if (!chr.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI).is_empty()) {
			count += 2;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI).is_empty()) {
			count += 3;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE).is_empty()) {
			count += 4;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_YI_TIAO_LONG).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN).is_empty()) {
			count += 1;
		}
		if (!chr.opr_and(GameConstants.CHR_BAN_BAN_HU).is_empty()) {
			count += 1;
		}

		return count;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			GRR._chi_hu_rights[player].opr_or(GRR._chi_hu_rights[player].qi_shou_bao_ting);

			chrTypes = GRR._chi_hu_rights[player].type_count;

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (is_bao_ting[player]) {
					gameDesc.append(" 报听");
				}
			}

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants.CHR_HUNAN_MEN_QING) {
						gameDesc.append(" 门清");
					}
					if (type == GameConstants.CHR_HUNAN_PENGPENG_HU) {
						gameDesc.append(" 碰碰胡");
					}
					if (type == GameConstants.CHR_HUNAN_JIANGJIANG_HU) {
						gameDesc.append(" 将将胡");
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						gameDesc.append(" 豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI) {
						gameDesc.append(" 双豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE) {
						gameDesc.append(" 三豪华七小对");
					}
					if (type == GameConstants.CHR_HUNAN_HAI_DI_LAO) {
						gameDesc.append(" 海底胡");
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
					if (type == GameConstants.CHR_HUNAN_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
					if (type == GameConstants.CHR_HUNAN_YI_TIAO_LONG) {
						gameDesc.append(" 一条龙");
					}
					if (type == GameConstants.CHR_HUNAN_QUAN_QIU_REN) {
						gameDesc.append(" 一字撬");
					}
					if (type == GameConstants.CHR_BAN_BAN_HU) {
						gameDesc.append(" 板板胡");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			if (happy_win_score[player] != 0) {
				gameDesc.append(" 喜钱x" + happy_win_score[player]);
			}

			if (is_mj_type(GameConstants.GAME_TYPE_THJ_YI_YANG_SGZ)) {
				if (gen_zhuang_fen[player] != 0) {
					if (gen_zhuang_fen[player] > 0) {
						gameDesc.append(" 跟庄");
					}
					if (gen_zhuang_fen[player] < 0) {
						gameDesc.append(" 被跟庄");
					}
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (has_rule_ex(GameConstants_YiYang_SZG.GAME_RULE_HUNAN_ZHANIAO)) {
					if (GRR._player_niao_count[player] > 0) {
						gameDesc.append(" 中鸟x" + GRR._player_niao_count[player]);
					}
				}
			}

			if (has_rule_ex(GameConstants_YiYang_SZG.GAME_RULE_HUNAN_FEI_NIAO)) {
			}
			if (GRR._player_niao_count_fei[player] > 0) {
				gameDesc.append(" 飞鸟x" + GRR._player_niao_count_fei[player]);
			}

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
				gameDesc.append(" 暗杠x" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠x" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠x" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠x" + jie_gang);
			}

			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_NON_TING_FA_FEN)) {
				if (GRR != null && GRR._end_type == GameConstants.Game_End_DRAW && _playerStatus[player]._hu_card_count <= 0) {
					gameDesc.append(" 未听牌");
				}
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && !is_bao_ting[i]) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0 && !is_bao_ting[i]) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				if (!is_bao_ting[i] || !ting_state_pass_jie_pao[i]) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
							GameConstants.HU_CARD_TYPE_PAOHU, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);

						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	public int getFeiNiaoNum() {
		int num = 0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_HUNAN_CS_FEI_NIAO1)) {
			num = GameConstants.FEINIAO_1;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			num = GameConstants.FEINIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_HUNAN_CS_FEI_NIAO3)) {
			num = GameConstants.FEINIAO_3;
		}

		if (num > GRR._left_card_count) {
			num = GRR._left_card_count;
		}

		return num;
	}

	public int getFeiNiaoNumUncheck() {
		int num = 0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_HUNAN_CS_FEI_NIAO1)) {
			num = GameConstants.FEINIAO_1;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_HUNAN_CS_FEI_NIAO2)) {
			num = GameConstants.FEINIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_HUNAN_CS_FEI_NIAO3)) {
			num = GameConstants.FEINIAO_3;
		}

		return num;
	}

	public void set_fei_niao_1_niao(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao_fei[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count_fei[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards_fei[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		// TODO 海底公胡，鸟牌就是胡的那张牌
		if (GRR._left_card_count == 0) {
			int nValue = _logic.get_card_value(card);

			int seat = get_seat_by_value(nValue, seat_index);

			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]++] = set_fei_niao_valid(card, true);
		} else {
			GRR._count_niao_fei = getFeiNiaoNum();

			if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei, cbCardIndexTemp);
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);

				GRR._left_card_count -= GRR._count_niao_fei;
			}

			// 中鸟个数
			GRR._count_pick_niao_fei = get_pick_niao_count(GRR._cards_data_niao_fei, GRR._count_niao_fei);

			for (int i = 0; i < GRR._count_niao_fei; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao_fei[i]);

				int seat = get_seat_by_value(nValue, seat_index);

				GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = GRR._cards_data_niao_fei[i];

				GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = set_fei_niao_valid(
						GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]], true); // 飞鸟都是有效的

				GRR._player_niao_count_fei[seat]++;
			}
		}
	}

	public void set_fei_niao_3_niao(int seat_index, int card, boolean show, int add_niao) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao_fei[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count_fei[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards_fei[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		int fei_niao = GRR._count_niao_fei = getFeiNiaoNumUncheck();

		if (GRR._left_card_count == 0) {
			for (int i = 0; i < GRR._count_niao_fei; i++) {
				GRR._cards_data_niao_fei[i] = card;
			}
		} else if (GRR._left_card_count == 1) {
			if (fei_niao == 1) {
				if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
					int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

					_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei, cbCardIndexTemp);
					_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);

					GRR._left_card_count -= GRR._count_niao_fei;
				}
			} else if (fei_niao == 2 || fei_niao == 3) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, 1, cbCardIndexTemp);
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);

				GRR._left_card_count--;

				for (int j = 1; j < fei_niao; j++) {
					GRR._cards_data_niao_fei[j] = GRR._cards_data_niao_fei[0];
				}
			}
		} else if (GRR._left_card_count == 2) {
			if (fei_niao == 1 || fei_niao == 2) {
				if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
					int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

					_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei, cbCardIndexTemp);
					_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);

					GRR._left_card_count -= GRR._count_niao_fei;
				}
			} else if (fei_niao == 3) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, 2, cbCardIndexTemp);
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);

				GRR._left_card_count--;

				for (int j = 1; j < fei_niao; j++) {
					GRR._cards_data_niao_fei[j] = GRR._cards_data_niao_fei[1];
				}
			}
		} else if (GRR._left_card_count >= 3) {
			if (GRR._count_niao_fei > GameConstants.ZHANIAO_0) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao_fei, cbCardIndexTemp);
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao_fei);

				GRR._left_card_count -= GRR._count_niao_fei;
			}
		}

		// 中鸟个数
		GRR._count_pick_niao_fei = get_pick_niao_count(GRR._cards_data_niao_fei, GRR._count_niao_fei);

		for (int i = 0; i < GRR._count_niao_fei; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao_fei[i]);

			int seat = get_seat_by_value(nValue, seat_index);

			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = GRR._cards_data_niao_fei[i];

			GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]] = set_fei_niao_valid(
					GRR._player_niao_cards_fei[seat][GRR._player_niao_count_fei[seat]], true); // 飞鸟都是有效的

			GRR._player_niao_count_fei[seat]++;
		}
	}

	public void set_niao_card_fei(int seat_index, int card, boolean show, int add_niao) {
		// if (!GameDescUtil.has_rule(gameRuleIndexEx,
		// GameConstants_YiYang_SZG.GAME_RULE_HUNAN_FEI_NIAO)) {
		// return;
		// }

		if (has_rule(GameConstants_YiYang_SZG.GAME_RULE_HAI_DI_SUAN_1_NIAO)) {
			set_fei_niao_1_niao(seat_index, card, show, add_niao);
		} else if (has_rule(GameConstants_YiYang_SZG.GAME_RULE_HAI_DI_SUAN_3_NIAO)) {
			set_fei_niao_3_niao(seat_index, card, show, add_niao);
		} else {
			set_fei_niao_1_niao(seat_index, card, show, add_niao);
		}
	}

	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_NIAO_1)) {
			nNum = GameConstants.ZHANIAO_1;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_NIAO_2)) {
			nNum = GameConstants.ZHANIAO_2;
		} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_YiYang_SZG.GAME_RULE_NIAO_3)) {
			nNum = GameConstants.ZHANIAO_3;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {
		// if (!GameDescUtil.has_rule(gameRuleIndexEx,
		// GameConstants_YiYang_SZG.GAME_RULE_HUNAN_ZHANIAO)) {
		// return;
		// }

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

		// TODO 海底公胡，鸟牌就是胡的那张牌
		if (GRR._left_card_count == 0) {
			GRR._count_niao = 1;

			GRR._cards_data_niao[0] = card;

			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao, seat_index, seat_index);

			int nValue = _logic.get_card_value(card);

			int seat = get_seat_by_value(nValue, seat_index);

			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = set_ding_niao_valid(card, true);
		} else {
			GRR._count_niao = getCsDingNiaoNum();

			if (GRR._count_niao > GameConstants.ZHANIAO_0) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

				GRR._left_card_count -= GRR._count_niao;
			}

			// 中鸟个数
			GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao, seat_index, seat_index);

			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

				int seat = get_seat_by_value(nValue, seat_index);

				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];

				if (seat == seat_index) {
					GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = set_ding_niao_valid(
							GRR._player_niao_cards[seat][GRR._player_niao_count[seat]], true); // 胡牌人的鸟生效
				} else {
					GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = set_ding_niao_valid(
							GRR._player_niao_cards[seat][GRR._player_niao_count[seat]], false); // 非胡牌人的鸟不生效
				}

				GRR._player_niao_count[seat]++;
			}
		}
	}

	public int get_pick_niao_count(int cards_data[], int card_num, int banker, int seat_index) {
		int cbPickNum = 0;

		if (getTablePlayerNumber() == 2) {
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				int nValue = _logic.get_card_value(cards_data[i]);

				if (seat_index == banker) {
					if (nValue == 1 || nValue == 5 || nValue == 9 || nValue == 3 || nValue == 7) {
						cbPickNum++;
					}
				} else if (seat_index == (banker + 1) % getTablePlayerNumber()) {
					if (nValue == 2 || nValue == 6 || nValue == 4 || nValue == 8) {
						cbPickNum++;
					}
				}
			}
		} else if (getTablePlayerNumber() == 3) {
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				int nValue = _logic.get_card_value(cards_data[i]);

				if (seat_index == banker) {
					if (nValue == 1 || nValue == 5 || nValue == 9 || nValue == 4 || nValue == 8) {
						cbPickNum++;
					}
				} else if (seat_index == (banker + 1) % getTablePlayerNumber()) {
					if (nValue == 2 || nValue == 6) {
						cbPickNum++;
					}
				} else if (seat_index == (banker + 2) % getTablePlayerNumber()) {
					if (nValue == 3 || nValue == 7) {
						cbPickNum++;
					}
				}
			}
		} else if (getTablePlayerNumber() == 4) {
			for (int i = 0; i < card_num; i++) {
				if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
					return 0;

				int nValue = _logic.get_card_value(cards_data[i]);

				if (seat_index == banker) {
					if (nValue == 1 || nValue == 5 || nValue == 9) {
						cbPickNum++;
					}
				} else if (seat_index == (banker + 1) % getTablePlayerNumber()) {
					if (nValue == 2 || nValue == 6) {
						cbPickNum++;
					}
				} else if (seat_index == (banker + 2) % getTablePlayerNumber()) {
					if (nValue == 3 || nValue == 7) {
						cbPickNum++;
					}
				} else if (seat_index == (banker + 3) % getTablePlayerNumber()) {
					if (nValue == 4 || nValue == 8) {
						cbPickNum++;
					}
				}
			}
		}

		return cbPickNum;
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

	public boolean exe_qi_shou_bao_ting() {
		set_handler(_handler_qi_shou_bao_ting);

		_handler.exe(this);

		return true;
	}

	public boolean exe_chu_pai_bao_ting(int seat_index, int card, int type) {
		set_handler(_handler_chu_pai_bao_ting);

		_handler_chu_pai_bao_ting.reset_status(seat_index, card, type);
		_handler.exe(this);

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

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0) {
			return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount == 3) {
				return GameConstants.CHR_HUNAN_THREE_HAOHUA_QI_YI_SE;
			}
			if (nGenCount == 2) {
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}

			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	// 如果出的牌是能报听的牌
	public boolean out_card_is_ting_card(int seat_index, int out_card) {
		int ting_count = _playerStatus[seat_index]._hu_out_card_count;
		if (ting_count > 0) {
			for (int i = 0; i < ting_count; i++) {
				int tmp_card = _playerStatus[seat_index]._hu_out_card_ting[i];
				tmp_card = get_real_card(tmp_card);

				if (tmp_card == out_card) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x12, 0x12, 0x12, 0x15, 0x15, 0x15, 0x18, 0x18, 0x18, 0x27, 0x27 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x01, 0x12, 0x12, 0x12, 0x15, 0x15, 0x15, 0x18, 0x18, 0x18, 0x27, 0x27 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x01, 0x12, 0x12, 0x12, 0x15, 0x15, 0x15, 0x18, 0x18, 0x18, 0x27, 0x27 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x12, 0x12, 0x12, 0x15, 0x15, 0x15, 0x18, 0x18, 0x18, 0x27, 0x27 };

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

		if (cards_of_player0.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards_of_player0[13])]++;
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
			// 庄家起手14张牌，闲家起手13张牌。和之前的有点区别。GameStart之后，不会再发牌。直接走新的Handler。
			if (i == GRR._banker_player) {
				send_count = GameConstants.MAX_COUNT;
			} else {
				send_count = (GameConstants.MAX_COUNT - 1);
			}

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		if (cards.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards[13])]++;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	public void add_guo_pai(int card, int seat_index) {
		boolean exist = false;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (bt_guo_pai[seat_index][i] == card) {
				exist = true;
				break;
			}
		}
		if (!exist) {
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if (bt_guo_pai[seat_index][i] == 0) {
					bt_guo_pai[seat_index][i] = card;
					break;
				}
			}
		}
	}

	public boolean is_guo_pai(int card, int seat_index) {
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (bt_guo_pai[seat_index][i] == card) {
				return true;
			}
		}
		return false;
	}
}
