package com.cai.game.hh.handler.xpphz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 
 * @author hexinqi
 *
 */

public class HHTable_XP extends HHTable {

	private static final long serialVersionUID = 1L;

	public boolean sanTi = false;
	HHGameLogic_XP _logicXP = null;
	public PHZHandlerChongGuan_XP _handler_chongguan;

	public int[] chong;
	public boolean[] guoHu;
	public int baseScore;

	public HHTable_XP() {
		super();
		_logicXP = new HHGameLogic_XP();
	}

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER_HH;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_XP();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_XP();
		_handler_gang = new PHZHandlerGang_XP();
		_handler_chi_peng = new PHZHandlerChiPeng_XP();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_XP();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_XP();
		_handler_chongguan = new PHZHandlerChongGuan_XP();
		sanTi = false;
		chong = new int[this.getTablePlayerNumber()];
		guoHu = new boolean[this.getTablePlayerNumber()];
		gu = new boolean[this.getTablePlayerNumber()];

		if (has_rule(GameConstants.GAME_RULE_XP_DIFEN_FOUR)) {
			baseScore = 4;
		} else if (has_rule(GameConstants.GAME_RULE_XP_DIFEN_TWO)) {
			baseScore = 2;
		} else {
			baseScore = 1;
		}
	}

	@Override
	public boolean reset_init_data() {
		sanTi = false;
		if (_cur_round == 0) {
			record_game_room();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
		} // 不能吃，碰

		_cannot_chi = new int[this.getTablePlayerNumber()][50];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][50];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][GameConstants.MAX_WEAVE_HH_XP];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][Constants_XPPHZ.MAX_HH_INDEX];

		Arrays.fill(guoHu, false);
		Arrays.fill(gu, false);
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			has_shoot[i] = false;
			is_hands_up[i] = false;
			player_ti_count[i][0] = 0;
			player_ti_count[i][1] = 0;
			is_wang_diao[i] = false;
			is_wang_diao_wang[i] = false;
			is_wang_chuang[i] = false;
			_is_di_hu[i] = 0;
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cards_has_wei[i] = 0;
		}
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH_XP; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
			_guo_hu_xt[i] = -1;
			_ti_mul_long[i] = 0;
			_guo_hu_pai_count[i] = 0;
			_ting_card[i] = false;
			_tun_shu[i] = 0;
			_fan_shu[i] = 0;
			_hu_code[i] = GameConstants.WIK_NULL;
			_liu_zi_fen[i] = 0;
			_hu_xi[i] = 0;
			_hu_pai_max_hu[i] = 0;
			_xt_display_an_long[i] = false;
			_xian_ming_zhao[i] = false;

		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;

			}
		}
		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;
		this._handler = null;

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH_XP, Constants_XPPHZ.MAX_HH_COUNT, Constants_XPPHZ.MAX_HH_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(Constants_XPPHZ.MAX_HH_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}

		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());

		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		GRR._video_recode.setBankerPlayer(this._cur_banker);
		return true;
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		// _logicXP.random_card_data(repertory_card, mj_cards);
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);
		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logicXP.random_card_data(repertory_card, mj_cards);
			else
				_logicXP.random_card_data(repertory_card, repertory_card);
			xi_pai_count++;
		}

		// int ca[] = { 7, 55, 42, 2, 52, 56, 42, 6, 5, 17, 8, 36, 51, 9, 20,
		// 57, 21, 55, 10, 23, 34, 25, 9, 23, 37, 4, 2, 58, 10, 18, 3, 54, 3,
		// 38, 4, 26, 52, 1,
		// 41, 36, 49, 40, 53, 24, 8, 1, 5, 21, 54, 6, 57, 50, 41, 20, 19, 19,
		// 38, 18, 33, 22, 53, 33, 51, 34, 50, 49, 39, 24, 17, 26, 56, 35, 7,
		// 25, 58,
		// 37, 39, 22, 35, 40 };
		// for (int i = 0; i < ca.length; i++) {
		// repertory_card[i] = ca[i];
		// }
		// this._cur_banker = 1;

		int send_count;
		int have_send_count = 0;
		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			send_count = (Constants_XPPHZ.MAX_HH_COUNT - 1);
			GRR._left_card_count -= send_count;
			// 一人20张牌,庄家多一张
			_logicXP.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		if (player != null) {
			int seatIndex = player.get_seat_index();

			if (seatIndex < 0 || seatIndex > this.getTablePlayerNumber()) {
				return false;
			}
			if (this.chong[seatIndex] != 0) {
				return false;
			}
			this.chong[seatIndex] = pao;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setCurrentPlayer(this._cur_banker);
			roomResponse.setType(Constants_XPPHZ.RESPONSE_CHONG_RESULT);
			roomResponse.setGameStatus(this._game_status);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addActions(this.chong[i]);
			}

			this.send_response_to_room(roomResponse);

			if (GRR != null) {
				GRR.add_room_response(roomResponse);
			}

			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.chong[i] != 0) {
					count++;
				}
			}
			if (count == this.getTablePlayerNumber()) {
				this.on_handler_game_start();
			}
		}

		return true;
	}

	/**
	 * 箍臭
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber()) {
			return false;
		}
		if (this.gu[seat_index]) {
			return false;
		}
		if (this._playerStatus == null) {
			return false;
		}

		this.gu[seat_index] = true;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		roomResponse.setCurrentPlayer(this._cur_banker);
		roomResponse.setType(Constants_XPPHZ.RESPONSE_GUCHOU_RESULT);
		roomResponse.setGameStatus(this._game_status);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			roomResponse.addDouliuzi(this.gu[i] ? 1 : 0);
		}

		if (this.gu[seat_index] && this._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
			this.operate_player_action(seat_index, true);
			this.exe_dispatch_card((seat_index + 1) % this.getTablePlayerNumber(), GameConstants.WIK_NULL, 0);
		}

		this.send_response_to_room(roomResponse);
		if (this.GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (has_rule(GameConstants.GAME_RULE_XP_KE_CHONG)) {
			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (chong[i] != 0) {
					count++;
				}
			}
			if (count < this.getTablePlayerNumber()) {
				this._game_status = Constants_XPPHZ.STATUS_CHONG_GUAN;
				if (0 == count) {
					reset_init_data();

					this._handler = this._handler_chongguan;
					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					this.load_room_info_data(roomResponse);
					this.load_common_status(roomResponse);

					if (this._cur_round == 1) {
						this.load_player_info_data(roomResponse);
					}
					roomResponse.setCurrentPlayer(this._cur_banker);
					roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
					roomResponse.setGameStatus(this._game_status);
					roomResponse.setPaoMax(1);
					roomResponse.setPaoMax(3);
					this.send_response_to_room(roomResponse);

					if (GRR != null) {
						GRR.add_room_response(roomResponse);
					}
				}
				return true;
			}
		} else {
			reset_init_data();
		}
		_game_status = GameConstants.GS_MJ_PLAY;

		// 庄家选择
		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[Constants_XPPHZ.CARD_COUNT_PHZ];
		shuffle(_repertory_card, Constants_XPPHZ.XP_PHZ);


		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

		_logicXP.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][Constants_XPPHZ.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logicXP.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		int flashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < Constants_XPPHZ.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}
			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			for (int k = 0; k < playerCount; k++) {
				roomResponse.addActions(this.chong[i]);
				roomResponse.addDouliuzi(this.gu[i] ? 1 : 0);
			}

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				flashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(flashTime);
			roomResponse.setStandTime(standTime);
			this.send_response_to_player(i, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < Constants_XPPHZ.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);
		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, 1000);

		return true;
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
		// 复制数据
		PerformanceTimer timer = new PerformanceTimer();
		int cbCardIndexTemp[] = new int[Constants_XPPHZ.MAX_HH_INDEX];
		for (int i = 0; i < Constants_XPPHZ.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		ChiHuRight chr = new ChiHuRight();
		int count = 0;
		int cbCurrentCard;

		int mj_count = 20;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i)) {
				continue;
			}
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		if (timer.get() > 500) {
			this.log_warn("pao huzi  ting card cost time = " + timer.duration() + "  and cards is =" + Arrays.toString(cbCardIndexTemp)
					+ "Arrays weaveItem" + Arrays.toString(weaveItem));
		}
		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i] + cards_index[i + 20];
		}
		cur_card %= 32;
		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logicXP.switch_to_card_index(cur_card) % 20;
			cbCardIndexTemp[index]++;
		}
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean bValue = _logicXP.analyse_card_xpphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi);
		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == (weaveItems[i].center_card % 32))
						&& ((dispatch && weaveItems[i].weave_kind == GameConstants.WIK_PENG) || weaveItems[i].weave_kind == GameConstants.WIK_WEI)) {
					int index = _logicXP.switch_to_card_index(cur_card) % 20;
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logicXP.analyse_card_xpphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j] % 32) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI)) {
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								}
								analyseItem.hu_xi[j] = _logicXP.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];
							}
						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logicXP.switch_to_card_index(cur_card) % 20;
			if ((cards_index[cur_index] + cards_index[cur_index + 20]) == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logicXP.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logicXP.analyse_card_xpphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logicXP.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < Constants_XPPHZ.GAME_QI_HU_XI) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);

		int countGang = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
				break;
			}
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logicXP.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_AN_LONG
					|| _hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_PAO || _hu_weave_items[seat_index][j].hu_xi == 9
					|| _hu_weave_items[seat_index][j].hu_xi == 12) {
				countGang++;
			}
		}
		if (analyseItem.curCardEye == true && countGang > 0) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		for (int j = 0; j < 7; j++) { // 不足七门方子不能胡牌
			if (_hu_weave_items[seat_index][j].center_card == GameConstants.WIK_NULL) {
				return GameConstants.WIK_NULL;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if ((seat_index == provider_index) && (dispatch == true) && card_type != Constants_XPPHZ.CHR_TIAN_HU) {
			chiHuRight.opr_or(Constants_XPPHZ.CHR_ZI_MO);
		}

		if (card_type == Constants_XPPHZ.CHR_TIAN_HU) {
			chiHuRight.opr_or(Constants_XPPHZ.CHR_TIAN_HU);
		}
		if (card_type == Constants_XPPHZ.CHR_FANG_PAO) {
			chiHuRight.opr_or(Constants_XPPHZ.CHR_FANG_PAO);
		}
		chiHuRight.opr_or(Constants_XPPHZ.CHR_HU);
		return cbChiHuKind;
	}

	public int estimate_player_ti_wei_respond_phz(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logicXP.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((!_logicXP.compareCard(weave_card, card_data)) || (weave_kind != GameConstants.WIK_WEI)) {
					continue;
				}
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL)
				&& (_logicXP.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
				}
			}
			if (!canEatPengWei(seat_index)) {
				return GameConstants.WIK_NULL;
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_WEI;
		}
		return bAroseAction;
	}

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_phz(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
		// 变量定义
		int bAroseAction = GameConstants.WIK_NULL;// 出现(是否)有
		pao_type[0] = 0;
		// 碰转跑
		if ((bAroseAction == GameConstants.WIK_NULL) && (dispatch == true)) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if (!_logicXP.compareCard(weave_card, card_data) || (weave_kind != GameConstants.WIK_PENG)) {
					continue;
				}
				pao_type[0] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}
		// 跑牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (seat_index != provider_index)) {
			if (_logicXP.check_pao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
				pao_type[0] = GameConstants.PAO_TYPE_OHTER_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}
		// 扫转跑
		if (seat_index != provider_index) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if (!_logicXP.compareCard(weave_card, card_data) || (weave_kind != GameConstants.WIK_WEI)) {
					continue;
				}
				pao_type[0] = GameConstants.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}

		}

		return bAroseAction;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
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
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
					| GameConstants.WIK_EQS | GameConstants.WIK_YWS;
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {
							chi_group.addLouWeaveKind(curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);
						}
					}
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		int diScore = 0;
		if (!chr.opr_and(Constants_XPPHZ.CHR_FANG_PAO).is_empty()) {
			diScore = 3;
		} else if (!chr.opr_and(Constants_XPPHZ.CHR_ZI_MO).is_empty() || !chr.opr_and(Constants_XPPHZ.CHR_TIAN_HU).is_empty()) {
			diScore = 2;
		} else {
			diScore = 1;
		}
		int chongScore = 0;
		if (this.chong[seat_index] > 0) {
			chongScore += baseScore * this.chong[seat_index];
		}
		if (!chr.opr_and(Constants_XPPHZ.CHR_FANG_PAO).is_empty()) { // 放炮
			if (this.chong[provide_index] > 0) {
				chongScore += baseScore * this.chong[provide_index];
			}
			GRR._game_score[seat_index] += (chongScore + diScore * baseScore);
			GRR._game_score[provide_index] -= (chongScore + diScore * baseScore);
		} else {
			int score = 0;
			if (!chr.opr_and(Constants_XPPHZ.CHR_ZI_MO).is_empty()) {
				score += diScore * baseScore;
			} else {
				score += diScore * baseScore;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int iChongScore = 0;
				if (this.chong[i] > 0) {
					iChongScore += baseScore * this.chong[i];
				}
				GRR._game_score[i] -= (chongScore + iChongScore + score);
				GRR._game_score[seat_index] += (chongScore + iChongScore + score);
			}
		}

	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);

		// 显示胡牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = _logicXP.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i], this.GRR._weave_count[i],
					GameConstants.INVALID_SEAT);

		}
		return;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		boolean flag = this._game_status != Constants_XPPHZ.STATUS_CHONG_GUAN;
		_game_status = GameConstants.GS_MJ_WAIT;
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null && flag) {// reason == MJGameConstants.Game_End_NORMAL
									// ||
			// reason
			// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int cards[] = new int[GRR._left_card_count];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < Constants_XPPHZ.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (this.has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					this._huang_zhang_count++;
				} else {
					this._huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logicXP.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				int weaveCount = real_reason == GameConstants.Game_End_NORMAL && i == seat_index ? _hu_weave_count[i] : GRR._weave_count[i];
				if (weaveCount > 0) {
					for (int j = 0; j < weaveCount; j++) {
						if (real_reason == GameConstants.Game_End_NORMAL && i == seat_index) {
							_player_result.ying_xi_count[seat_index] += _hu_weave_items[i][j].hu_xi;

							WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
							weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
							weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
							weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
							weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
							weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
							for (int z = 0; z < 4; z++) {
								if (_hu_weave_items[i][j].weave_card[z] > 0) {
									weaveItem_item.addWeaveCard(_hu_weave_items[i][j].weave_card[z]);
								}
							}
							weaveItem_array.addWeaveItem(weaveItem_item);
						} else {
							WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
							weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
							weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
							weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
							weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
							weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
							for (int z = 0; z < 4; z++) {
								if (this.GRR._weave_items[i][j].weave_card[z] > 0) {
									weaveItem_item.addWeaveCard(this.GRR._weave_items[i][j].weave_card[z]);
								}
							}
							weaveItem_array.addWeaveItem(weaveItem_item);
						}
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);
				// game_end.add

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			roomResponse.addActions(this.chong[i]);
			game_end.addPao(this.chong[i]);
		}

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);
		Arrays.fill(chong, 0);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end) { // 删除
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}
		// 错误断言
		return false;
	}

	@Override
	public void set_result_describe(int seat_index) {
		if (seat_index < 0) {
			return;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			StringBuffer des = new StringBuffer();

			ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
			if (!chiHuRight.opr_and(Constants_XPPHZ.CHR_TIAN_HU).is_empty()) {
				des.append("天胡: 三提五坎");
			} else if (!chiHuRight.opr_and(Constants_XPPHZ.CHR_FANG_PAO).is_empty()) {
				des.append("炮胡");
			} else if (!chiHuRight.opr_and(Constants_XPPHZ.CHR_ZI_MO).is_empty()) {
				des.append("自摸");
			} else if (!chiHuRight.opr_and(Constants_XPPHZ.CHR_HU).is_empty()) {
				des.append("平胡");
			}

			GRR._result_des[i] = des.toString();
		}

	}

	@Override
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		_player_result.hu_pai_count[_seat_index]++;
		_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];

		if (!(chiHuRight.opr_and(Constants_XPPHZ.CHR_ZI_MO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}

	}

	public int get_chi_hu_action_rank_phz(ChiHuRight chr, WeaveItem weaveItems[], int weaveCount) {
		int wFanShu = 0;
		return 0 == wFanShu ? 1 : wFanShu;
	}

	public boolean is_card_has_wei(int card) {
		boolean bTmp = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.cards_has_wei[i] != 0) {
				if (i == this._logicXP.switch_to_card_index(card) % 20) {
					bTmp = true;
					break;
				}
			}
		}
		return bTmp;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
		}

		return card;
	}

	// 是否听牌
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[Constants_XPPHZ.MAX_HH_INDEX];
		for (int i = 0; i < Constants_XPPHZ.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		for (int i = 0; i < Constants_XPPHZ.MAX_HH_INDEX; i++) {
			int cbCurrentCard = _logicXP.switch_to_card_data(i) % 32;
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
				return true;
		}
		return false;
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		// 先注释掉，等客户端一起联调
		for (int x = 0; x < card_count; x++) {
			if (this.is_card_has_wei(cards[x])) { // 如果是偎的牌
				cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				for (int k = 0; k < 4; k++) {
					if (weaveitems[j].weave_card[k] > 0) {
						weaveItem_item.addWeaveCard(weaveitems[j].weave_card[k]);
					}
				}

				if ((weaveitems[j].weave_kind == GameConstants.WIK_TI_LONG || weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG
						|| weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG_LIANG) && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);

				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				for (int k = 0; k < 4; k++) {
					if (weaveitems[j].weave_card[k] > 0) {
						weaveItem_item.addWeaveCard(weaveitems[j].weave_card[k]);
					}
				}
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		roomResponse.setHuXiCount(this._hu_xi[seat_index]);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		if (this._handler != null) {
			this._handler.handler_player_out_card(this, seat_index, card);
		}
		return true;
	}

	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < Constants_XPPHZ.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (Constants_XPPHZ.MAX_HH_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
					|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD))
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logicXP.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < Constants_XPPHZ.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
					|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD))
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
			if (send_count > cards.length) {
				send_count = cards.length;
			}
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logicXP.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public void test_cards() {
		/*
		 * for (int i = 0; i < this.getTablePlayerNumber(); i++) { for (int j =
		 * 0; j < Constants_XPPHZ.MAX_HH_INDEX; j++) { GRR._cards_index[i][j] =
		 * 0; } } this._repertory_card = new int[]{5, 19, 33, 3, 19, 33, 4, 18,
		 * 34, 18, 9, 23, 26, 57, 54, 20, 56, 24, 55, 35, 39, 49, 52, 36, 21,
		 * 10, 53, 17, 25, 2, 8, 5, 58, 50, 57, 41, 4, 3, 51, 22, 40, 7, 53, 42,
		 * 22, 26, 55, 49, 7, 20, 23, 1, 10, 8, 35, 37, 58, 50, 2, 17, 9, 1, 52,
		 * 40, 37, 21, 42, 6, 54, 51, 38, 36, 39, 38, 6, 34, 56, 41, 25, 24};
		 * _all_card_len = _repertory_card.length; GRR._left_card_count =
		 * _all_card_len;
		 * 
		 * int send_count; int have_send_count = 0; // 分发扑克 for (int i = 0; i <
		 * this.getTablePlayerNumber(); i++) { send_count =
		 * (Constants_XPPHZ.MAX_HH_COUNT - 1); if
		 * (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) send_count =
		 * GameConstants.MAX_WMQ_COUNT - 1; if
		 * (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)) send_count =
		 * (GameConstants.MAX_FPHZ_COUNT - 1); GRR._left_card_count -=
		 * send_count;
		 * 
		 * // 一人20张牌,庄家多一张 _logicXP.switch_to_cards_index(_repertory_card,
		 * have_send_count, send_count, GRR._cards_index[i]);
		 * 
		 * have_send_count += send_count; } DEBUG_CARDS_MODE = false;// 把调试模式关闭
		 * BACK_DEBUG_CARDS_MODE = false;
		 * System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");
		 */
		//// 天胡-三提
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02,
		// 0x02, 0x03, 0x03, 0x03, 0x03, 0x05, 0x17, 0x17, 0x15, 0x19, 0x18,
		// 0x18,
		// 0x16 };

		// 天胡-五坎
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x04, 0x02, 0x02, 0x02,
		// 0x04, 0x03, 0x03, 0x03, 0x04, 0x05, 0x05, 0x05, 0x15, 0x19, 0x18,
		// 0x18,
		// 0x16 };
		// int cards[] = new int[] { 0x14, 0x14, 0x14, 0x11, 0x01, 0x01, 0x01,
		// 0x11, 0x06, 0x06, 0x16, 0x11, 0x03, 0x03, 0x03, 0x18, 0x18, 0x08,
		// 0x18, 0x08 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x16, 0x18, 0x18, 0x18,
		// 0x16, 0x13, 0x13, 0x13, 0x16, 0x14, 0x14, 0x14, 0x12, 0x11, 0x02,
		// 0x07, 0x0a };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x06, 0x08, 0x08, 0x08,
		// 0x06, 0x03, 0x03, 0x03, 0x06, 0x04, 0x04, 0x04, 0x02, 0x01, 0x12,
		// 0x17, 0x1a };

		// { 0x11, 0x11, 0x11, 0x33, 0x33, 0x33, 0x12, 0x12, 0x17, 0x17, 0x1a,
		// 0x1a, 0x0a, 0x09, 0x04, 0x14, 0x14, 0x05, 0x15, 0x15, },
		// int cards[][] = { {
		// 0x0a,0x0a,0x0a,0x18,0x19,0x1a,0x12,0x15,0x14,0x16,0x21,0x21,0x21,0x02,0x03,0x04,0x08,0x08,0x08,0x09
		// },
		// {
		// 0x0a,0x0a,0x0a,0x18,0x19,0x19,0x12,0x15,0x14,0x16,0x21,0x21,0x21,0x02,0x03,0x04,0x08,0x08,0x08,0x09},
		// {
		// 0x12,0x12,0x32,0x13,0x13,0x33,0x14,0x05,0x34,0x15,0x16,0x17,0x09,0x19,0x29,0x08,0x03,0x07,0x07,0x17
		// },
		// // {
		// //
		// 0x02,0x02,0x22,0x15,0x15,0x35,0x21,0x21,0x03,0x23,0x27,0x27,0x36,0x06,0x18,0x28,0x14,0x17,0x1a,
		// // 0x31 },
		// };
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < Constants_XPPHZ.MAX_HH_INDEX; j++) {
		// GRR._cards_index[i][j] = 0;
		// }
		// }
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < cards[i].length; j++) {
		// GRR._cards_index[i][_logicXP.switch_to_card_index(cards[i][j])] += 1;
		// }
		// }
		/*
		 * int[] temps =new int[]{8, 6, 5, 51, 18, 49, 3, 25, 36, 54, 52, 24,
		 * 42, 3, 9, 22, 35, 23, 22, 10, 26, 40, 19, 9, 21, 39, 52, 39, 34, 19,
		 * 41, 41, 37, 53, 38, 23, 4, 20, 54, 37, 35, 1, 7, 10, 57, 24, 1, 6, 2,
		 * 40, 36, 20, 38, 5, 56, 25, 50, 58, 51, 50, 42, 33, 2, 8, 55, 34, 33,
		 * 26, 49, 55, 17, 58, 4, 56, 18, 57, 53, 21, 17, 7};
		 * testRealyCard(temps);
		 */
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		// debug_my_cards = new
		// int[]{0x12,0x12,0x32,0x13,0x13,0x33,0x14,0x14,0x34,0x15,0x16,0x17,0x09,0x09,0x29,0x08,0x08,0x07,0x07,0x07,0x12,0x12,0x32,0x13,0x13,0x33,0x14,0x14,0x34,0x15,0x16,0x17,0x09,0x09,0x29,0x08,0x08,0x07,0x07,0x07,0x12,0x12,0x32,0x13,0x13,0x33,0x14,0x14,0x34,0x15,0x16,0x17,0x09,0x09,0x29,0x08,0x08,0x07,0x07,0x06,0x16,0x16,0x17,0x17,0x17};
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 20) {
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

	public void setWeaveCard(int target_player, int target_card, int[] cbRemoveCard) {
		int wIndex = this.GRR._weave_count[target_player];
		int k = 0;
		int cardsIndex[] = Arrays.copyOf(this.GRR._cards_index[target_player], Constants_XPPHZ.MAX_HH_INDEX);
		if (cardsIndex[this._logicXP.switch_to_card_index(this._logicXP.toLowCard(cbRemoveCard[0]))] > 0) {
			cardsIndex[this._logicXP.switch_to_card_index(this._logicXP.toLowCard(cbRemoveCard[0]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = this._logicXP.toLowCard(cbRemoveCard[0]);
		} else {
			cardsIndex[this._logicXP.switch_to_card_index(this._logicXP.toUpCard(cbRemoveCard[0]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = this._logicXP.toUpCard(cbRemoveCard[0]);
		}
		if (cardsIndex[this._logicXP.switch_to_card_index(this._logicXP.toLowCard(cbRemoveCard[1]))] > 0) {
			cardsIndex[this._logicXP.switch_to_card_index(this._logicXP.toLowCard(cbRemoveCard[0]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = this._logicXP.toLowCard(cbRemoveCard[1]);
		} else {
			cardsIndex[this._logicXP.switch_to_card_index(this._logicXP.toUpCard(cbRemoveCard[0]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = this._logicXP.toUpCard(cbRemoveCard[1]);
		}
		this.GRR._weave_items[target_player][wIndex].weave_card[k++] = target_card;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				if (_logic.is_ding_gui_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				} else {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i]);
				}

			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_playerStatus == null || _playerStatus[i] == null) {
				continue;
			}
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

}
