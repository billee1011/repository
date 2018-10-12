package com.cai.game.mj.huangshan.tunxi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.Constants_ShanXi;
import com.cai.common.constant.game.mj.Constants_HuangShan;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_HuangShan extends AbstractMJTable {
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

	public HandlerSelectMagicCard_HuangShan _handler_select_magic_card;

	/**
	 * 飘飞
	 */
	public int piao_fei[];
	/**
	 * 碰飞
	 */
	public int peng_fei[];

	/**
	 * 摸牌次数
	 */
	public int dispatchcardNum[];

	/**
	 * 手牌的王类型
	 */
	public static final int SINGLE_MAGIC = 1;
	public static final int THREE_MAGIC = 2;
	public static final int FOUR_MAGIC = 3;
	public static final int SIX_MAGIC = 4;
	public static final int SEVEN_MAGIC = 5;
	public static final int NONE_MAGIC = 6;

	public Table_HuangShan() {
		super(MJType.GAME_TYPE_MJ_HUANGSHAN);
	}

	public int get_magic_type(int[] cards_index) {
		int joker_count_1 = cards_index[joker_card_index_1];
		int joker_count_2 = cards_index[joker_card_index_2];

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
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HuangShan();
		_handler_dispath_card = new HandlerDispatchCard_HuangShan();
		_handler_gang = new HandlerGang_HuangShan();
		_handler_out_card_operate = new HandlerOutCardOperate_HuangShan();

		_handler_select_magic_card = new HandlerSelectMagicCard_HuangShan();

		piao_fei = new int[getTablePlayerNumber()];

		peng_fei = new int[getTablePlayerNumber()];

		dispatchcardNum = new int[getTablePlayerNumber()];
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;

		this.init_shuffle();

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
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
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = ThreadLocalRandom.current().nextInt(getTablePlayerNumber());// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJGameConstants.GAME_PLAYER;//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	protected void init_shuffle() {
		List<Integer> cards_list = new ArrayList<>();
		int all_cards[] = Constants_HuangShan.CARD_DATA;
		for (int i : all_cards) {
			cards_list.add(i);
		}
		if (!has_rule(Constants_HuangShan.GAME_RULE_QUE_YI_MEN)) {
			for (int i : Constants_HuangShan.WAN_ZI) {
				cards_list.add(i);
			}
		}
		_repertory_card = new int[cards_list.size()];
		int[] card = new int[cards_list.size()];
		for (int i = 0; i < cards_list.size(); i++) {
			card[i] = cards_list.get(i);
		}
		shuffle(_repertory_card, card);
	}

	@Override
	protected boolean on_game_start() {
		joker_card_1 = 0;
		joker_card_index_1 = -1;
		joker_card_2 = 0;
		joker_card_index_2 = -1;
		ding_wang_card = 0;
		ding_wang_card_index = -1;
		Arrays.fill(piao_fei, 0);
		Arrays.fill(peng_fei, 0);
		Arrays.fill(dispatchcardNum, 0);
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
				// GRR._game_score[i] += lGangScore[i];
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
					if (GRR._cards_data[i][j] == joker_card_2) {
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

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seatIndex) {
		int cbChiHuKind = GameConstants.WIK_NULL;
		if (cur_card == 0) {
			return cbChiHuKind;
		}
		if (has_rule(Constants_HuangShan.GAME_RULE_ZHI_XU_ZI_MU)) {
			if (card_type != Constants_HuangShan.HU_CARD_TYPE_ZI_MO) {
				return cbChiHuKind;
			}
		}
		// 转换手牌
		boolean is_all_lai_zi = true;
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
			if (cards_index[i] > 0 && !_logic.is_magic_index(i)) {
				is_all_lai_zi = false;
			}
		}

		if (_logic.is_valid_card(cur_card)) {
			tmp_hand_cards_index[_logic.switch_to_card_index(cur_card)]++;
		}

		// 剩余手牌是不是全是鬼牌
		int fei_pai = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (tmp_hand_cards_index[i] > 0 && _logic.is_magic_index(i)) {
				fei_pai += tmp_hand_cards_index[i];
			}
		}

		// 四财神
		int[] magic_cards_index = new int[4];
		int magic_card_count = _logic.get_magic_card_count();
		if (magic_card_count > 8) { // 一般只有两种癞子牌存在
			magic_card_count = 8;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 收铳必须要手里没有飞或飞能归位（当做本身）的情况下才可以胡
		boolean can_win_withount_magic_card = false;
		// if (card_type == Constants_HuangShan.HU_CARD_TYPE_JIE_PAO ||
		// card_type == Constants_HuangShan.HU_CARD_TYPE_QIANG_GANG) {
		if (fei_pai > 0 && fei_pai < 4) {
			can_win_withount_magic_card = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					_logic.get_all_magic_card_index(), 0);
			// if (!can_win_withount_magic_card) {
			// chiHuRight.set_empty();
			// return cbChiHuKind;
			// }
		} else if (fei_pai <= 0) {
			can_win_withount_magic_card = true;
		}
		// }

		int hand_card_count = _logic.get_card_count_by_index(cards_index);

		boolean si_cai_shen = false;
		if (fei_pai == 4) {
			si_cai_shen = true;
		}
		// 四财神
		boolean can_win_with_magic_card = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		boolean is_pi_hu = true;

		// 牌型计算
		if (si_cai_shen) {
			chiHuRight.opr_or(Constants_HuangShan.CHR_SI_CAI_SHEN);
			is_pi_hu = false;
		} else if (!can_win_with_magic_card) {
			chiHuRight.set_empty();
			return cbChiHuKind;
		}

		// 跑飞
		if (hand_card_count == 1 && fei_pai >= 1 && is_all_lai_zi) {
			chiHuRight.opr_or(Constants_HuangShan.CHR_PAO_FEI);
			is_pi_hu = false;
		}

		// 天胡、地胡
		if (dispatchcardNum(seatIndex) == 1 && hand_card_count == 13) {
			if (seatIndex == _cur_banker) {
				chiHuRight.opr_or(Constants_HuangShan.CHR_TIAN_HU);
				is_pi_hu = false;
			}
		}

		if (dispatchcardNum(_cur_banker) == 1 && dispatchcardNum(seatIndex) == 0 && hand_card_count == 13) {
			chiHuRight.opr_or(Constants_HuangShan.CHR_DI_HU);
			is_pi_hu = false;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_HuangShan.HU_CARD_TYPE_ZI_MO) {
			if (chiHuRight.opr_and(Constants_HuangShan.CHR_PAO_FEI).is_empty()) {
				if (fei_pai > 0) {
					chiHuRight.opr_or(Constants_HuangShan.CHR_YOU_FEI_ZI_MO);
				} else {
					chiHuRight.opr_or(Constants_HuangShan.CHR_WU_FEI_ZI_MO);
				}
			} else {
				chiHuRight.opr_or(Constants_HuangShan.CHR_ZI_MO);
			}
		} else if (card_type == Constants_HuangShan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_HuangShan.CHR_JIE_PAO);
		} else if (card_type == Constants_HuangShan.HU_CARD_TYPE_GANG_KAI) {
			if (fei_pai > 0) {
				chiHuRight.opr_or(Constants_HuangShan.CHR_FEI_JI_GANG);
			} else {
				chiHuRight.opr_or(Constants_HuangShan.CHR_WU_FEI_GANG_KAI);
			}
		} else if (card_type == Constants_HuangShan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_HuangShan.CHR_WU_FEI_QIANG_GANG);
		}

		if (card_type == Constants_HuangShan.HU_CARD_TYPE_JIE_PAO || card_type == Constants_HuangShan.HU_CARD_TYPE_QIANG_GANG) {
			if (fei_pai > 0 && !can_win_withount_magic_card) {
				return GameConstants.WIK_NULL;
			}
		}

		if (is_pi_hu) {
			if (fei_pai > 0 && piao_fei[seatIndex] <= 0 && !can_win_withount_magic_card) {
				if (has_rule(Constants_HuangShan.GAME_RULE_PI_HU) && card_type == Constants_HuangShan.HU_CARD_TYPE_ZI_MO) {
					chiHuRight.opr_or(Constants_HuangShan.CHR_PI_HU);
					return cbChiHuKind;
				} else {
					if (get_pai_xing_fen(chiHuRight, seatIndex) < 2) {
						return GameConstants.WIK_NULL;
					}
				}
			}
		}

		if (!has_rule(Constants_HuangShan.GAME_RULE_PI_HU) && !can_win_withount_magic_card) {
			if (card_type == Constants_HuangShan.HU_CARD_TYPE_ZI_MO || card_type == Constants_HuangShan.HU_CARD_TYPE_JIE_PAO) {
				if (fei_pai > 0) {
					if ((piao_fei[seatIndex] > 0 || get_pai_xing_fen(chiHuRight, seatIndex) >= 2)) {
						return cbChiHuKind;
					} else {
						return GameConstants.WIK_NULL;
					}
				}
			}
		}
		return cbChiHuKind;
	}

	/**
	 * 牌型分
	 * 
	 * @param chr
	 * @return
	 */
	public int get_pai_xing_fen(ChiHuRight chr, int seat_index) {
		int pai_xing_fen = 1;
		// 有碰飞 直接算8分
		if (peng_fei[seat_index] > 0) {
			chr.opr_or(Constants_HuangShan.CHR_PENG_FEI);
			pai_xing_fen = 8;
		} else if (!chr.opr_and(Constants_HuangShan.CHR_SI_CAI_SHEN).is_empty())
			pai_xing_fen = 8;
		else if (!chr.opr_and(Constants_HuangShan.CHR_TIAN_HU).is_empty())
			pai_xing_fen = 8;
		else if (!chr.opr_and(Constants_HuangShan.CHR_DI_HU).is_empty())
			pai_xing_fen = 8;
		else if (!chr.opr_and(Constants_HuangShan.CHR_FEI_JI_GANG).is_empty())
			pai_xing_fen = 4;
		else if (!chr.opr_and(Constants_HuangShan.CHR_WU_FEI_GANG_KAI).is_empty())
			pai_xing_fen = 4;
		else if (!chr.opr_and(Constants_HuangShan.CHR_WU_FEI_QIANG_GANG).is_empty())
			pai_xing_fen = 4;
		else if (!chr.opr_and(Constants_HuangShan.CHR_WU_FEI_ZI_MO).is_empty())
			pai_xing_fen = 2;
		// 跑飞
		else if (!chr.opr_and(Constants_HuangShan.CHR_PAO_FEI).is_empty()) {
			pai_xing_fen = 2;
		} else if (!chr.opr_and(Constants_HuangShan.CHR_YOU_FEI_ZI_MO).is_empty())
			pai_xing_fen = 1;

		return pai_xing_fen;
	}

	/**
	 * 飞牌加成
	 * 
	 * @return
	 */
	private int get_fei_fen(int seaxIndex, ChiHuRight chr) {
		int fei_fan_shu = 1;
		// 飘飞
		fei_fan_shu <<= piao_fei[seaxIndex];
		// // 跑飞
		// if (!chr.opr_and(Constants_HuangShan.CHR_PAO_FEI).is_empty())
		// fei_fan_shu <<= 2;
		// 碰飞
		// fei_fan_shu <<= peng_fei[seaxIndex];
		return fei_fan_shu;
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
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int total_fen = 1;

		total_fen *= get_pai_xing_fen(chr, seat_index) * get_fei_fen(seat_index, chr);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = total_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = total_fen;
		}

		if (zimo) {
			int s = 0;
			if (!chr.opr_and(Constants_HuangShan.CHR_TIAN_HU).is_empty()) {
				s = 8;
			} else {
				s = total_fen;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			if (!chr.opr_and(Constants_HuangShan.CHR_DI_HU).is_empty()) {
				if (has_rule(Constants_HuangShan.GAME_RULE_DI_HU_YI_JIA_CHU)) {
					GRR._game_score[provide_index] -= 8;
					GRR._game_score[seat_index] += 8;
				} else if (has_rule(Constants_HuangShan.GAME_RULE_DI_HU_SAN_JIA_CHU)) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index)
							continue;
						GRR._game_score[i] -= 8;
						GRR._game_score[seat_index] += 8;
					}
				}
			} else {
				int s = total_fen;
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;

				GRR._chi_hu_rights[provide_index].opr_or(Constants_HuangShan.CHR_FANG_PAO);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (GRR._game_score[i] > (getTablePlayerNumber() - 1) * 8) {
				GRR._game_score[i] = (getTablePlayerNumber() - 1) * 8;
			} else if (GRR._game_score[i] < -8) {
				GRR._game_score[i] = -8;
			}

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
					if (type == Constants_HuangShan.CHR_ZI_MO)
						result.append(" 自摸");
					if (type == Constants_HuangShan.CHR_JIE_PAO)
						result.append(" 收铳");
					if (type == Constants_HuangShan.CHR_SI_CAI_SHEN)
						result.append(" 四财神");
					if (type == Constants_HuangShan.CHR_YOU_FEI_ZI_MO)
						result.append(" 有飞自摸");
					if (type == Constants_HuangShan.CHR_WU_FEI_ZI_MO)
						result.append(" 无飞自摸");
					if (type == Constants_HuangShan.CHR_PAO_FEI)
						result.append(" 跑飞");
					if (type == Constants_HuangShan.CHR_FEI_JI_GANG)
						result.append(" 飞机杠");
					if (type == Constants_HuangShan.CHR_WU_FEI_GANG_KAI)
						result.append(" 无飞杠开");
					if (type == Constants_HuangShan.CHR_WU_FEI_QIANG_GANG)
						result.append(" 无飞抢杠");
					if (type == Constants_HuangShan.CHR_DI_HU)
						result.append(" 地胡");
					if (type == Constants_HuangShan.CHR_TIAN_HU)
						result.append(" 天胡");
					if (type == Constants_HuangShan.CHR_PENG_FEI)
						result.append(" 碰飞");
				} else if (type == Constants_HuangShan.CHR_FANG_PAO) {
					result.append(" 放铳");
				}
			}

			if (piao_fei[player] > 0) {
				result.append(" 飘飞X" + piao_fei[player]);
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
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

			// if (an_gang > 0) {
			// result.append(" 暗杠X" + an_gang);
			// }
			// if (ming_gang > 0) {
			// result.append(" 明杠X" + ming_gang);
			// }
			// if (fang_gang > 0) {
			// result.append(" 放杠X" + fang_gang);
			// }
			// if (jie_gang > 0) {
			// result.append(" 接杠X" + jie_gang);
			// }

			GRR._result_des[player] = result.toString();
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
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			int action = analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, Constants_ShanXi.HU_CARD_TYPE_ZI_MO,
					seat_index);
			if (GameConstants.WIK_CHI_HU == action) {
				return true;
			} else {
				chr.set_empty();
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
				// int hand_card_count =
				// _logic.get_card_count_by_index(GRR._cards_index[i]);
				// int joker_count = GRR._cards_index[i][joker_card_index_1];
				// if (joker_card_index_2 != -1) {
				// joker_count += GRR._cards_index[i][joker_card_index_2];
				// }

				// if ((seat_index + 1) % getTablePlayerNumber() == i &&
				// (joker_count + 2 < hand_card_count)) {
				// 杠之后打的牌，其他玩家只能胡，不能吃碰杠
				if ((seat_index + 1) % getTablePlayerNumber() == i) {
					action = check_chi_ignore_magic(GRR._cards_index[i], card);
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

				boolean can_peng_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng_this_card = false;
						break;
					}
				}

				action = _logic.check_peng(GRR._cards_index[i], card);
				// if (action != 0 && (joker_count + 2 < hand_card_count) &&
				// can_peng_this_card) {
				if (action != 0 && can_peng_this_card) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);
					bAroseAction = true;
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

					int hu_card_type = Constants_ShanXi.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.DISPATCH_CARD_TYPE_GANG)
						hu_card_type = Constants_ShanXi.HU_CARD_TYPE_GANG_PAO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_CHI_HU);
						playerStatus.add_chi_hu(card, seat_index);
						bAroseAction = true;
					} else {
						chr.set_empty();
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

			int action = analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, Constants_HuangShan.HU_CARD_TYPE_ZI_MO,
					seat_index);
			if (GameConstants.WIK_CHI_HU == action) {
				cards[count] = cbCurrentCard;
				count++;
			} else {
				chr.set_empty();
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
						Constants_HuangShan.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				} else {
					chr.set_empty();
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

	// public void set_niao_card(int seat_index) {
	// for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
	// GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
	// }
	//
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// GRR._player_niao_count[i] = 0;
	// for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
	// GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
	// }
	// }
	//
	// GRR._show_bird_effect = true;
	//
	// GRR._count_niao = get_niao_card_num();
	//
	// if (GRR._count_niao > 0) {
	// int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
	//
	// // 从剩余牌堆里顺序取奖码数目的牌
	// _logic.switch_to_cards_index(_repertory_card, _all_card_len -
	// GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
	//
	// _logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
	//
	// if (DEBUG_CARDS_MODE) {
	// GRR._cards_data_niao[0] = 0x04;
	// }
	//
	// GRR._left_card_count -= GRR._count_niao;
	// }
	//
	// for (int i = 0; i < GRR._count_niao; i++) {
	// int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
	//
	// int seat = get_seat_by_value(nValue, seat_index);
	//
	// GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] =
	// GRR._cards_data_niao[i];
	//
	// GRR._player_niao_count[seat]++;
	// }
	//
	// // 设置鸟牌显示
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// for (int j = 0; j < GRR._player_niao_count[i]; j++) {
	// GRR._player_niao_cards[i][j] =
	// set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
	// }
	// }
	// }

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

	// public int get_niao_card_num() {
	// int nNum = 0;
	//
	// if (has_rule(Constants_AnHua.GAME_RULE_ONE_BIRD)) {
	// nNum = 1;
	// } else if (has_rule(Constants_AnHua.GAME_RULE_TWO_BIRD)) {
	// nNum = 2;
	// } else if (has_rule(Constants_AnHua.GAME_RULE_THREE_BIRD)) {
	// nNum = 3;
	// }
	//
	// if (nNum > GRR._left_card_count) {
	// nNum = GRR._left_card_count;
	// }
	//
	// return nNum;
	// }

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(Constants_HuangShan.GAME_RULE_PLAYER_THREE)) {
			return 3;
		} else if (has_rule(Constants_HuangShan.GAME_RULE_PLAYER_ER)) {
			return 2;
		} else {
			return 4;
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x35, 0x35, 0x35, 0x01, 0x02, 0x03, 0x16, 0x17, 0x15, 0x15, 0x15, 0x27, 0x08 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x12, 0x12, 0x13, 0x13, 0x14, 0x14, 0x16, 0x17, 0x18, 0x18, 0x19 };
		int[] cards_of_player2 = new int[] { 0x21, 0x21, 0x22, 0x22, 0x23, 0x23, 0x24, 0x24, 0x26, 0x27, 0x28, 0x28, 0x29 };
		int[] cards_of_player1 = new int[] { 0x15, 0x15, 0x15, 0x03, 0x03, 0x03, 0x04, 0x04, 0x06, 0x07, 0x08, 0x08, 0x09 };

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

	/** 摸牌数累计 */
	public void addDispatchcardNum(int seat_index) {
		dispatchcardNum[seat_index]++;
	}

	/**
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public int dispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index];
	}

	public int check_chi_ignore_magic(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (_logic.is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		// 如果是风牌或者王牌
		if (_logic.get_card_color(cur_card) > 2)
			return GameConstants.WIK_NULL;

		// 构造数据
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		// 插入扑克
		tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int eat_type = GameConstants.WIK_NULL;

		int first_card_index = 0;

		int cur_card_index = _logic.switch_to_card_index(cur_card);

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 轮询判断，可以有多种吃法，首先判断左吃，然后判断中吃，最后判断右吃
		for (int i = 0; i < 3; i++) {

			int card_value = _logic.get_card_value(cur_card);

			// 牌值判断，比如左吃时，牌值可以是1,2...7；右吃时，牌值可以是3,4...9
			if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {

				// 吃牌时，第一张牌的实际索引值，按照左吃、中吃、右吃的顺序来
				// 比如左吃时，第一张牌的索引就是当前的牌，中吃的时候就是当前牌的前面一张牌
				// 为什么要用索引呢？因为传进来的数据是索引数据，哈哈哈
				first_card_index = cur_card_index - i;

				// 如果不是王牌的玩法，直接判断，第一张牌，第二牌，和第三张牌存不存在
				if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
						&& tmp_cards_index[first_card_index + 2] != 0) {
					// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
	}

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			// if (_logic.is_magic_index(i))
			// continue; // 红中不能杠
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
							continue;
						} else {
							if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明刚
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

}
