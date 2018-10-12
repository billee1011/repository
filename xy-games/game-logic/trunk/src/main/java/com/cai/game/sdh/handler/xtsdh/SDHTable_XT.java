package com.cai.game.sdh.handler.xtsdh;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.sdh.SDHConstants_XT;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.SDHCallBankerRunnable;
import com.cai.future.runnable.SDHClearRoundRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.sdh.SDHConstants;
import com.cai.game.sdh.SDHGameLogic;
import com.cai.game.sdh.SDHTable;
import com.cai.game.sdh.handler.SDHHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sdh.SdhRsp.CallBankerRequest;
import protobuf.clazz.sdh.SdhRsp.CallBankerResponse;
import protobuf.clazz.sdh.SdhRsp.CallMainResponse;
import protobuf.clazz.sdh.SdhRsp.OutCardDataRequestSdh;
import protobuf.clazz.sdh.SdhRsp.PukeGameEndSdh;
import protobuf.clazz.sdh.SdhRsp.RoomInfoSdh;
import protobuf.clazz.sdh.SdhRsp.RoomPlayerResponseSdh;
import protobuf.clazz.sdh.SdhRsp.StallRate;
import protobuf.clazz.sdh.SdhRsp.TableResponseSdh;

/**
 * 
 * @author hexinqi 三打哈房间操作
 */
public class SDHTable_XT extends SDHTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(SDHTable_XT.class);

	public boolean hasOutCard; // 是否出牌
	public int colorNumber[]; // 记录庄家叫主时每个花色的卡牌数量
	public int maxWin[];
	public int freeScoreCards[];
	public int freeScoreCardsCount;
	public boolean hasMain[]; // 是否有主牌
	public int ntMainCount[]; // 常主数量
	public int maxCall;
	public int minCall;
	public int guard[];
	public int nextPlayer;
	public boolean bankerHasOut = false; // 所有闲家报副后庄家是否出过牌

	public ScheduledFuture<?> _trustee_schedule[];// 托管定时器

	private long _request_release_time;
	private ScheduledFuture<?> _release_scheduled;
	private ScheduledFuture<?> _table_scheduled;

	public SDHHandler<SDHTable_XT> _handler;
	public SDHHandlerFinish_XT<SDHTable_XT> _handler_finish; // 结束
	public SDHHandlerCallBankerOperate_XT<SDHTable_XT> callBankerHandler; // 叫庄
	public SDHHandlerCallMainOperate_XT<SDHTable_XT> callMainHandler; // 叫主
	public SDHHandlerMaiCardOperate_XT<SDHTable_XT> maiCardHandler; // 埋牌
	public SDHHandlerOutCardOperate_XT<SDHTable_XT> outcardHandler;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		return SDHConstants_XT.SDH_GAME_PLAYER;
	}

	public SDHTable_XT() {
		super();
		// 常主2, 主牌7
		_logic = new SDHGameLogic(2, SDHConstants_XT.SDH_ERROR_NUMBER, 7);
		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GAME_STATUS_FREE;
		this.currentGameStatus = GameConstants.GAME_STATUS_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;
		_game_rule_index = game_rule_index;
		_game_round = game_round;

		cardCount = SDHConstants_XT.SDH_CELL_PACK_REMOVE6 * SDHConstants_XT.SDH_PACK_COUNT;
		handCardCount = SDHConstants_XT.SDH_CARD_COUNT_REMOVE6;
		maxHandCardCount = SDHConstants_XT.SDH_MAX_COUNT_REMOVE6;
		if (has_rule(SDHConstants_XT.GAME_RULE_SDH_MAX_DI)) {
			this._di_fen = 60 + 5;
		} else {
			this._di_fen = 80 + 5;
		}

		_banker_select = GameConstants.INVALID_SEAT;
		_cur_round = 0;
		allScore = 0;
		beginTime = 0;
		callScore = new int[this.getTablePlayerNumber()];
		playerScores = new int[this.getTablePlayerNumber()];
		diPai = new int[SDHConstants_XT.SDH_DIPAI_COUNT];
		cardsValues = new int[this.getTablePlayerNumber()][SDHConstants_XT.SDH_COLOR_COUNT + 1][SDHConstants_XT.SDH_ONE_COLOR_COUNT + 4];
		outCards = new int[this.getTablePlayerNumber()][handCardCount + 10];
		outCardsCount = new int[this.getTablePlayerNumber()];
		scoreCards = new int[this.getTablePlayerNumber()][SDHConstants_XT.SDH_SCORE_CARD_COUNT];
		freeScoreCards = new int[SDHConstants_XT.SDH_SCORE_CARD_COUNT];
		freeScoreCardsCount = 0;
		scoreCardsCount = new int[this.getTablePlayerNumber()];
		_logic.maxCard = new int[this.getTablePlayerNumber()][SDHConstants_XT.SDH_COLOR_COUNT + 1];
		firstOutColor = SDHConstants_XT.SDH_ERROR_NUMBER;
		winBanker = new int[this.getTablePlayerNumber()];
		winFree = new int[this.getTablePlayerNumber()];
		fail = new int[this.getTablePlayerNumber()];
		totalScore = new int[this.getTablePlayerNumber()];
		currentScore = new int[this.getTablePlayerNumber()];
		maxWin = new int[this.getTablePlayerNumber()];
		hasMain = new boolean[this.getTablePlayerNumber()];
		ntMainCount = new int[this.getTablePlayerNumber()];
		guard = new int[this.getTablePlayerNumber()];
		colorNumber = new int[5];
		currentGameStatus = GameConstants.GAME_STATUS_FREE;
		reconnectOutCards = SDHConstants_XT.SDH_ERROR_NUMBER;
		outRound = 0;
		firstPlayer = SDHConstants_XT.SDH_ERROR_NUMBER;
		this._cur_banker = -1;
		this.hasOutCard = false;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des());
		_player_result.game_score = new float[this.getTablePlayerNumber()];
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(handCardCount);
			_playerStatus[i].reset();
		}

		GRR = new GameRoundRecord(getTablePlayerNumber(), 0, maxHandCardCount, maxHandCardCount);

		_handler_finish = new SDHHandlerFinish_XT<>();
		callBankerHandler = new SDHHandlerCallBankerOperate_XT<>();
		callMainHandler = new SDHHandlerCallMainOperate_XT<>();
		maiCardHandler = new SDHHandlerMaiCardOperate_XT<>();
		outcardHandler = new SDHHandlerOutCardOperate_XT<>();

		this.setMinPlayerCount(this.getTablePlayerNumber());
	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {

	}

	/**
	 * 重置数据
	 * 
	 * @return
	 */
	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {
			record_game_room();
		}

		cardCount = SDHConstants_XT.SDH_CELL_PACK_REMOVE6 * SDHConstants_XT.SDH_PACK_COUNT;
		handCardCount = SDHConstants_XT.SDH_CARD_COUNT_REMOVE6;
		maxHandCardCount = SDHConstants_XT.SDH_MAX_COUNT_REMOVE6;

		this._handler = null;

		GRR = new GameRoundRecord(0, maxHandCardCount, maxHandCardCount);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		GRR._cur_round++;
		_cur_round++;

		allScore = 0;
		beginTime = 0;
		this.rate = 0;
		bankerHasOut = false;
		playerScores = new int[this.getTablePlayerNumber()];
		callScore = new int[this.getTablePlayerNumber()];
		diPai = new int[SDHConstants_XT.SDH_DIPAI_COUNT];
		cardsValues = new int[this.getTablePlayerNumber()][SDHConstants_XT.SDH_COLOR_COUNT + 1][SDHConstants_XT.SDH_ONE_COLOR_COUNT + 4];
		outCards = new int[this.getTablePlayerNumber()][handCardCount + 10];
		outCardsCount = new int[this.getTablePlayerNumber()];
		scoreCards = new int[this.getTablePlayerNumber()][SDHConstants_XT.SDH_SCORE_CARD_COUNT];
		scoreCardsCount = new int[this.getTablePlayerNumber()];
		_logic.maxCard = new int[this.getTablePlayerNumber()][SDHConstants_XT.SDH_COLOR_COUNT + 1];
		_logic.m_cbMainColor = -1;
		this._cur_banker = -1;
		this.hasOutCard = false;

		firstOutColor = SDHConstants_XT.SDH_ERROR_NUMBER;
		Arrays.fill(currentScore, 0);
		Arrays.fill(colorNumber, 0);
		Arrays.fill(freeScoreCards, 0);
		Arrays.fill(hasMain, true);
		Arrays.fill(ntMainCount, 0);
		Arrays.fill(guard, -1);
		currentGameStatus = GameConstants.GAME_STATUS_FREE;
		outRound = 0;
		firstPlayer = SDHConstants_XT.SDH_ERROR_NUMBER;
		freeScoreCardsCount = 0;
		outcardHandler.firstCount = 0;
		outcardHandler.outNumber = 0;

		callScore = new int[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];

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
			if (rplayer == null) {
				continue;
			}
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);
		}
		GRR._video_recode.setBankerPlayer(this._banker_select);

		return true;
	}

	@Override
	public boolean handler_reconnect_room(Player player) {
		// 发送进入房间
		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse1.setGameStatus(_game_status);
		roomResponse1.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse1);
		this.load_player_info_data(roomResponse1);

		send_response_to_player(player.get_seat_index(), roomResponse1);
		roomResponse1.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse1);

		int seat_index = player.get_seat_index();
		if ((GameConstants.GAME_STATUS_FREE != _game_status && GameConstants.GAME_STATUS_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
				roomResponse.setReleaseTime((_request_release_time - System.currentTimeMillis()) / 1000);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GAME_STATUS_PLAY;
		this.currentGameStatus = GameConstants.GAME_STATUS_PLAY;

		reset_init_data();
		GRR._banker_player = _banker_select; // 临时庄家

		_repertory_card = new int[cardCount];
		shuffle(_repertory_card, SDHConstants_XT.CARD_DATA_SDH_REMOVE6);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

		return game_start();
	}

	/**
	 * 游戏开始
	 * 
	 * @return
	 */
	@Override
	public boolean game_start() {
		int playerCount = getPlayerCount();

		// 游戏开始
		this._game_status = GameConstants.GAME_STATUS_PLAY;// 设置状态
		this.currentGameStatus = GameConstants.GAME_STATUS_PLAY;
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);

		int hand_cards[][] = new int[playerCount][handCardCount];

		for (int i = 0; i < playerCount; i++) { // 发牌
			hand_cards[i] = GRR._cards_data[i];
			gameStartResponse.addCardsCount(handCardCount);
		}

		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < handCardCount; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
				if (_logic.isTheMain(hand_cards[i][j])) {
					this.ntMainCount[i]++;
				}
			}
			// 回放数据
			this.GRR._video_recode.addHandCards(cards);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_player_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(i);
			roomResponse.setGameStatus(this._game_status);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			int flashTime = 0, standTime = 0;
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				flashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(flashTime);
			roomResponse.setStandTime(standTime);
			roomResponse.setCurrentPlayer(i);

			GRR.add_room_response(roomResponse);
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
			for (int j = 0; j < handCardCount; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		observers().sendAll(roomResponse);

		// 延迟1S后通知第一个玩家叫分
		this._di_fen = 80 + 5;
		if (has_rule(SDHConstants_XT.GAME_RULE_SDH_MAX_DI)) {
			this._di_fen = 60 + 5;
		}
		int begin = this._banker_select == -1 ? 0 : this._banker_select;

		maxCall = this._di_fen - 5;
		minCall = 0;
		this.nextPlayer = begin;
		if (this.ntMainCount[begin] >= 6) {
			maxCall = 60;
			minCall = 5;
		}
		this.handler_call_banker(begin, begin);

		return true;
	}

	@Override
	public int get_hand_card_count_max() {
		return handCardCount;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	@Override
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber()) {
			return false;
		}
		return istrustee[seat_index];
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.randomCardData(repertory_card, card_cards);
			else
				_logic.randomCardData(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getPlayerCount();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = SDHConstants_XT.SDH_NOT_DI_PAI | repertory_card[i * handCardCount + j];
			}
			GRR._card_count[i] = handCardCount;
			_logic.switch_cards_to_index(i, GRR._cards_data[i], GRR._card_count[i], this.cardsValues[i]);
			_logic.sortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}
		GRR._left_card_count = SDHConstants_XT.SDH_DIPAI_COUNT;
		for (int i = 1; i <= SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
			this.diPai[i - 1] = _repertory_card[this.cardCount - i];
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {

		// int cards[] = new int[] { 0x05, 0x05, 0x08, 0x08, 0x09, 0x41, 0x41,
		// 0x09, 0x42, 0x42, 0x0b, 0x0b, 0x07, 0x07,
		// 0x17, 0x17, 0x0a, 0x0a, 0x02 };
		// // int cards[] = new int[] {
		// //
		// 0x05,0x05,0x08,0x08,0x09,0x41,0x41,0x09,0x42,0x42,0x0b,0x0b,0x07,0x07,0x17,0x17,0x0a,0x0a
		// // };
		// // int cards_t[][] = { { 0x02, 0x09, 0x09, 0x0a, 0x0a }, { 0x02,
		// 0x02,
		// // 0x07, 0x07, 0x27, }, { 0x09, 0x09, 0x0a, 0x0a, 0x02, },
		// // { 0x09, 0x09, 0x0a, 0x0a, 0x02, } };
		// // int diCards[] = new int[] { 0x41, 0x41, 0x27, 0x27, 0x42, 0x42,
		// 0x22,
		// // 0x22 };
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// for (int j = 0; j < 19; j++) {
		// GRR._cards_data[i][j] = 0;
		// }
		// }
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < cards.length; j++) {
		// GRR._cards_data[i][j] = cards[j];
		// // GRR._cards_data[i][j] = cards[i * 19 + j];
		// }
		// GRR._card_count[i] = cards.length;
		// this._logic.sortCardList(GRR._cards_data[i], 19);
		// }
		// for (int i = 0; i < diCards.length; i++) {
		// this.diPai[i] = diCards[i];
		// }
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		// int[] realyCards = new int[] {10,44,7, 14, 44, 56, 59, 1, 60, 33, 26,
		// 1, 21, 13, 33, 28, 59, 49, 11,
		// 66, 65, 14, 7, 44, 56, 55, 26, 28, 27, 37, 61, 5, 25, 39, 9, 28, 8,
		// 58,
		// 44, 25, 33, 21, 27, 51, 37, 22, 12, 53, 57, 56, 5, 45, 61, 11, 9, 1,
		// 23,
		// 44, 8, 42, 42, 53, 44, 66, 65, 55, 7,39 , 51, 49, 57, 28, 13, 58, 45,
		// 60,
		// 17, 41, 2, 10, 29, 7, 56, 1 };
		// testRealyCard(realyCards);

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > handCardCount) {
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

	/**
	 * 测试线上 实际牌
	 */
	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < maxHandCardCount; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		int send_count = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < handCardCount; j++) {
				GRR._cards_data[i][j] = _repertory_card[j + send_count];
			}
			_logic.sortCardList(GRR._cards_data[i], GRR._card_count[i]);
			send_count += handCardCount;
			GRR._card_count[i] = handCardCount;
		}

		GRR._left_card_count = SDHConstants_XT.SDH_DIPAI_COUNT;
		if (GRR._left_card_count >= SDHConstants_XT.SDH_DIPAI_COUNT) {
			for (int i = 1; i <= SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
				this.diPai[i - 1] = _repertory_card[this.cardCount - i];
			}
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
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		handCardCount = cards.length;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < handCardCount; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
			_logic.sortCardList(GRR._cards_data[i], GRR._card_count[i]);
			GRR._card_count[i] = handCardCount;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GAME_STATUS_WAIT;
		this.currentGameStatus = GameConstants.GAME_STATUS_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return this.handler_game_finish_sdh(seat_index, reason);
	}

	@Override
	public boolean handler_game_finish_sdh(int seat_index, int reason) {
		int real_reason = reason;
		if (reason == SDHConstants_XT.SDH_ALL_PLAYER_GIVE_UP) {
			real_reason = 2;
		}

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndSdh.Builder gameEndSdh = PukeGameEndSdh.newBuilder();

		_logic.m_cbMainColor = -1;
		_di_fen = 0;
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfoSdh.Builder room_info = RoomInfoSdh.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		gameEndSdh.setRoomInfo(room_info);
		gameEndSdh.setPlayerNum(count);
		gameEndSdh.setGameRound(_game_round);
		gameEndSdh.setCurRound(_cur_round);
		gameEndSdh.setDifen(this._di_fen);
		gameEndSdh.setScore(this.allScore);
		gameEndSdh.setWinner(seat_index == _banker_select ? 1 : 0);
		gameEndSdh.setReason(this._end_reason);
		if (reason == SDHConstants_XT.SDH_ALL_PLAYER_GIVE_UP) {
			gameEndSdh.setReason(0); // 所有人不叫 比常主
			reason = GameConstants.Game_End_NORMAL;
		}
		gameEndSdh.setStall(this.stall);
		gameEndSdh.setRate(this.rate);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gameEndSdh.addEndScore(this.currentScore[i]);
			gameEndSdh.addWinBankerNum(this.winBanker[i]);
			gameEndSdh.addWinFreeNum(this.winFree[i]);
			gameEndSdh.addAllEndScore(this.totalScore[i]);
			gameEndSdh.addFailNum(this.fail[i]);
			gameEndSdh.addMaxWin(this.maxWin[i]);
			game_end.addGameScore(this.currentScore[i]);
		}
		gameEndSdh.setDiCardCount(0);
		if (this.diPai != null && this.diPai[0] != 0) {
			for (int i = 0; i < SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
				gameEndSdh.addDiCardsData(this.diPai[i]);
			}
			gameEndSdh.setDiCardCount(SDHConstants_XT.SDH_DIPAI_COUNT);
		}

		if (GRR != null) {
			this.load_player_info_data(roomResponse);
			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}
		game_end.setRoomOverType(0);

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				game_end.setRoomOverType(1);
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN) {
				real_reason = reason;
			}
			gameEndSdh.setReason(7); // 解散 流局
			game_end.setPlayerResult(this.process_player_result(reason));
			game_end.setRoomOverType(1);
		} else if (reason == SDHConstants_XT.Player_Status_GIVE_UP) { // 认输
			gameEndSdh.setWinner(0);
			_banker_select = (seat_index + 1) % this.getTablePlayerNumber();
		}
		game_end.setEndType(real_reason);

		// 得分总的
		game_end.setRoomInfo(getRoomInfo());
		// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		roomResponse.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(gameEndSdh));
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse); // 客户端要求记录最后结算操作
		}
		if (this.callBankerHandler != null) {
			this.callBankerHandler.preSeat = -1;
			this.callBankerHandler.success = true;
		}
		record_game_round(game_end, real_reason);
		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < count; j++) {
				Player player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}
		Arrays.fill(this.currentScore, 0);
		Arrays.fill(this.diPai, 0);

		if (end) { // 删除
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}
		// 错误断言
		return false;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
			_player_result.game_score[i] = this.totalScore[i];
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					winner = j;
				}
			}
			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (winner != -1) {
				_player_result.win_order[winner] = win_idx;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);
			player_result.addPlayersId(i);
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_game_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);
		return player_result;
	}

	/**
	 * @return
	 */
	@Override
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = encodeRoomBase();

		int beginLeftCard = SDHConstants_XT.SDH_DIPAI_COUNT;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	/**
	 * 创建房间
	 * 
	 * @return
	 */
	@Override
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		this.setCreate_type(type);
		this.setCreate_player(player);
		roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "", RoomRedisModel.class);
		if (type == GameConstants.CREATE_ROOM_PROXY) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);
			writeToRedis(0);
			// 发送进入房间
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_CREATE_RROXY_ROOM_SUCCESS);
			TableResponseSdh.Builder sdh = TableResponseSdh.newBuilder();
			sdh.setBankerPlayer(this._cur_banker);
			roomResponse.setCommResponse(PBUtil.toByteString(sdh));
			load_room_info_data(roomResponse);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			PlayerServiceImpl.getInstance().send(player, responseBuilder.build());
			return true;
		} else if (type == GameConstants.CREATE_ROOM_ROBOT) { // 机器人开房
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);
			writeToRedis(0);
			return true;
		}

		if (club_id > 0) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP, TimeUnit.MINUTES);
			RedisService redisService = SpringService.getBean(RedisService.class);
			roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM, getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
			roomRedisModel.getNames().add(player.getNick_name());
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);
		}

		// 创建成功
		get_players()[0] = player;
		player.set_seat_index(0);
		onPlayerEnterUpdateRedis(player.getAccount_id());
		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		return send_response_to_player(player.get_seat_index(), roomResponse);
	}

	@Override
	public void sendRate() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());
		this.load_room_info_data(roomResponse);

		StallRate.Builder stallRate = StallRate.newBuilder();
		stallRate.setStall(this.stall);
		stallRate.setRate(this.rate);
		stallRate.setCurBanker(this._cur_banker);
		stallRate.setCurPlayer(this._current_player);
		stallRate.setDifen(this._di_fen);

		for (int i = 0; i <= 3; i++) { // 可以叫的主花色及每个花色的数量
			stallRate.addMainColorList(i);
			stallRate.addMainColorNumber(colorNumber[i]);
		}
		stallRate.addMainColorNumber(this.ntMainCount[this._banker_select]);
		stallRate.setMainColorCount(5);

		roomResponse.setCommResponse(PBUtil.toByteString(stallRate));
		roomResponse.setType(SDHConstants_XT.ROOM_RATE);// 几档几倍
		send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
	}

	public void sendMainColor(int seatIndex, int color) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());

		CallMainResponse.Builder response = CallMainResponse.newBuilder();
		response.setCurBanker(this._cur_banker);
		response.setMainColor(color);

		roomResponse.setCommResponse(PBUtil.toByteString(response));
		roomResponse.setType(SDHConstants_XT.RESPONSE_CALL_MAIN_COLOR); // 叫主信息
		send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
	}

	@Override
	public void sendCallBankerInfo(int seatIndex) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());

		CallBankerResponse.Builder callBankerResponse = CallBankerResponse.newBuilder();
		callBankerResponse.setCallCurrentPlayer(seatIndex);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			callBankerResponse.addScore(callScore[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(callBankerResponse));
		roomResponse.setType(SDHConstants_XT.RESPONSE_CALL_BANKER); // 叫分信息
		send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
			if (this.get_players()[get_seat_index] == null) {
				return false;
			}
			if (GameConstants.GAME_STATUS_FREE != _game_status && GameConstants.GAME_STATUS_WAIT != _game_status) {
				return false;
			}
			_player_ready[get_seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);

			return false;
		} else {
			return handler_player_ready(get_seat_index, is_cancel);
		}
	}

	public void refresh_player_score(int seatIndex, int curScore) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());
		roomResponse.setCurrentPlayer(this._current_player);

		TableResponseSdh.Builder tableResponseSdh = TableResponseSdh.newBuilder();

		tableResponseSdh.clearPlayerStatus();
		tableResponseSdh.setMainColor(this._logic.m_cbMainColor);
		tableResponseSdh.setBankerPlayer(this._cur_banker);
		tableResponseSdh.setScore(this.allScore); // 当前总得分
		tableResponseSdh.setCurScore(curScore); // 当前轮得分

		// 回看数据
		int min = this.outCardsCount[0];
		for (int i = 1; i < this.getTablePlayerNumber(); i++) {
			min = min > this.outCardsCount[i] ? this.outCardsCount[i] : min;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			RoomPlayerResponseSdh.Builder playerSdh = RoomPlayerResponseSdh.newBuilder();
			playerSdh.clear();

			playerSdh.setScore(0);
			playerSdh.setJifen(this.totalScore[i]);
			for (int j = 0; j < min; j++) { // 出牌
				playerSdh.addOutCardsData(this.outCards[i][j]);
			}
			playerSdh.setOutCardsCount(min);
			for (int j = 0; j < this.freeScoreCardsCount; j++) { // 分牌
				playerSdh.addScoreCardsData(this.freeScoreCards[j]);
			}
			playerSdh.setScoreCardsCount(this.freeScoreCardsCount);
			for (int z = 0; z < this.getTablePlayerNumber(); z++) {
				Int32ArrayResponse.Builder value = Int32ArrayResponse.newBuilder();
				for (int j = 0; j <= SDHConstants_XT.SDH_COLOR_COUNT; j++) {
					value.addItem(this._logic.maxCard[z][j]);
				}
				playerSdh.addMaxCardXt(value);
			}
			for (int j = 0; j <= SDHConstants.SDH_COLOR_COUNT; j++) {
				playerSdh.addMaxCard(this._logic.maxCard[i][j]);
			}
			playerSdh.setHasMain(this._logic.isTheMain(this._logic.maxCard[i][SDHConstants_XT.SDH_COLOR_MAIN]));
			tableResponseSdh.addPlayers(playerSdh);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponseSdh));
		roomResponse.setType(SDHConstants_XT.RESPONSE_REFRESH_PLAYER_SCORE);// 刷新玩家分数

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		this.send_response_to_room(roomResponse);
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		_player_ready[seat_index] = SDHConstants_XT.SDH_PLAYER_READY;
		if (GameConstants.GAME_STATUS_FREE != _game_status && GameConstants.GAME_STATUS_WAIT != _game_status) {
			return false;
		}

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		int cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				cur_count++;
			} else {
				continue;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}
		if (cur_count < this.getTablePlayerNumber()) {
			return false;
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GAME_STATUS_FREE != _game_status && GameConstants.GAME_STATUS_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
				roomResponse.setReleaseTime((_request_release_time - System.currentTimeMillis()) / 1000);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
		return true;

	}

	/**
	 * 用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		return true;
	}

	/**
	 * 用户出牌 / 埋牌
	 * 
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		int[] cardDatas = new int[card_count];
		for (int i = 0; i < list.size(); i++) {
			cardDatas[i] = list.get(i);
		}
		if (b_out_card == 0 && card_count == 0) { // 自动出牌
			list = outcardHandler.getOutCard(this, get_seat_index);
			card_count = list.size();
		}
		outcardHandler.resetStatus(get_seat_index, cardDatas, card_count);
		outcardHandler.exe(this);
		if (!outcardHandler.isSuccess()) { // 该玩家出牌出错
			send_error_notify(get_seat_index, 2, "请选择正确的牌型");
			// 刷新手牌
			refreshPlayerCards(get_seat_index);

			if (this.GRR != null && this.GRR._card_count[get_seat_index] > 0) {
				// 通知该玩家出牌
				operate_effect_action(get_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { SDHConstants_XT.Player_Status_OUT_CARDS }, 1, get_seat_index);
			}
		}
		return outcardHandler.isSuccess();
	}

	/***
	 * 用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		this._handler = this.callBankerHandler;
		this.currentGameStatus = SDHConstants_XT.GAME_STATUS_JIAOZHUANG;
		this._game_status = SDHConstants_XT.GAME_STATUS_JIAOZHUANG;
		this._current_player = seat_index;
		// 发牌后 延迟3s发送叫分
		GameSchedule.put(new SDHCallBankerRunnable(this.getRoom_id(), seat_index, SDHConstants_XT.Game_CALL_BANKER_WAIT_TIME_OUT), 3000,
				TimeUnit.MILLISECONDS);

		return true;
	}

	public boolean switch_call_main(int seatIndex) {
		this._handler = this.callMainHandler;

		this.currentGameStatus = SDHConstants_XT.GAME_STATUS_DINGZHU;
		this._game_status = SDHConstants_XT.GAME_STATUS_DINGZHU;
		this._current_player = seatIndex;

		this.beginTime = System.currentTimeMillis();
		this.operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_MAIN }, 1,
				seatIndex);
		this.showPlayerOperate(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_CALL_MAIN },
				SDHConstants_XT.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);

		return true;
	}

	public boolean switch_mai_card(int seatIndex) {
		this._handler = this.maiCardHandler;

		this.beginTime = System.currentTimeMillis();
		this.currentGameStatus = SDHConstants_XT.GAME_STATUS_MAICARD;
		this._game_status = SDHConstants_XT.GAME_STATUS_MAICARD;
		operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 2,
				new long[] { SDHConstants_XT.Player_Status_MAI_CARD, SDHConstants_XT.Player_Status_GIVE_UP }, SDHConstants_XT.SDH_OPERATOR_TIME,
				seatIndex);
		showPlayerOperate(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_MAI_CARD },
				SDHConstants_XT.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);

		return true;
	}

	public boolean switch_out_card(int seatIndex) {
		this._handler = this.outcardHandler;

		this.currentGameStatus = SDHConstants_XT.GAME_STATUS_OUTCARD;
		this._game_status = SDHConstants_XT.GAME_STATUS_OUTCARD;
		this.beginTime = System.currentTimeMillis();
		this.nextPlayer = seatIndex;
		// 通知出牌
		operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_OUT_CARDS },
				SDHConstants_XT.SDH_OPERATOR_TIME, seatIndex);
		// 通知所有玩家看底牌
		operate_effect_action(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_LOOK_DIPAI }, -1,
				GameConstants.INVALID_SEAT);
		showPlayerOperate(seatIndex, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { SDHConstants_XT.Player_Status_OUT_CARDS },
				SDHConstants_XT.SDH_OPERATOR_TIME, GameConstants.INVALID_SEAT);

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		this._current_player = seat_index;
		switch (type) {
		case MsgConstants.REQUST_SDH_OPERATE: // 叫庄
			CallBankerRequest cbr = PBUtil.toObject(room_rq, CallBankerRequest.class);
			if (cbr.getOpreateType() == SDHConstants_XT.Player_Status_CALL_MAIN) {
				return handlerSelectMainColor(seat_index, cbr.getMainColor());
			} else if (cbr.getOpreateType() == SDHConstants_XT.Player_Status_GIVE_UP) {
				this._end_reason = SDHConstants_XT.Player_Status_GIVE_UP;
				this._handler_finish.exe(this);
				return true;
			} else if (cbr.getOpreateType() == SDHConstants_XT.RESPONSE_GUARD) {
				return handlerGuard(seat_index, cbr.getMainColor());
			}
			return handlerCallBankerRequest(seat_index, cbr.getOpreateType(), cbr.getScore());
		case MsgConstants.REQUST_SDH_OUT_CARD_MUL: // 出牌
			OutCardDataRequestSdh req = PBUtil.toObject(room_rq, OutCardDataRequestSdh.class);
			if (SDHConstants_XT.Player_Status_MAI_CARD == req.getOutCardType()) { // 埋牌
				return handlerMaiCard(seat_index, req.getCardsDataList(), req.getCardsCount());
			} else if (SDHConstants_XT.Player_Status_OUT_CARDS == req.getOutCardType()) {
				return handler_operate_out_card_mul(seat_index, req.getCardsDataList(), req.getCardsCount(), req.getOutCardType(), "");
			}
		case SDHConstants_XT.Player_Status_GIVE_UP:
			this._end_reason = SDHConstants_XT.Player_Status_GIVE_UP;
			this._handler_finish.exe(this);
			return true;
		}
		return true;
	}

	public boolean handlerGuard(int seatIndex, int color) {
		if (color < 0 || color > 4) {
			logger.error("湘潭三打哈错误的留守花色: " + color);
			return false;
		}

		if (this._handler != null) {
			this.outcardHandler.status = 0;
		}

		if (has_rule(SDHConstants_XT.GAME_RULE_SDH_BAOFULIUSHOU) && !this.hasMain[seatIndex] && this.guard[seatIndex] == -1) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.clear();
			roomResponse.setType(SDHConstants_XT.RESPONSE_BAO_FU);
			roomResponse.setHuXiType(this.firstPlayer == seatIndex ? 1 : 0);
			roomResponse.setCurrentPlayer(seatIndex);
			roomResponse.setOperateCode(color);
			GRR.add_room_response(roomResponse);
			send_response_to_room(roomResponse);

			this.guard[seatIndex] = color;
		}
		int delay = 0;
		if (this.firstPlayer == SDHConstants_XT.SDH_ERROR_NUMBER) {
			delay = 1000;
		}
		GameSchedule.put(new SDHClearRoundRunnable(this.getRoom_id(), this.nextPlayer, 2), delay, TimeUnit.MILLISECONDS);
		return true;
	}

	/**
	 * 定主
	 * 
	 * @param seatIndex
	 * @param mainColor
	 * @return
	 */
	private boolean handlerSelectMainColor(int seatIndex, int mainColor) {
		if (seatIndex != _banker_select) {
			logger.error("不是庄家不能定主");
			return false;
		}
		if (mainColor < 0 || mainColor > 4) {
			logger.error("定主花色错误");
			return false;
		}
		if (_game_status != SDHConstants_XT.GAME_STATUS_DINGZHU) {
			logger.error("SDHTable_XT 不是叫主状态");
			return false;
		}
		if (has_rule(SDHConstants_XT.GAME_RULE_SDH_BU_DA_WU_ZHU) && 4 == mainColor) {
			logger.error("SDHTable_XT 不打无主玩法不能打无主");
			send_error_notify(seatIndex, 2, "不打无主");
			return false;
		}
		_logic.m_cbMainColor = mainColor;
		this.sendMainColor(seatIndex, mainColor);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			refreshPlayerCards(i);
		}

		this.switch_mai_card(seatIndex);

		return true;
	}

	@Override
	public void refreshPlayerCards(int seatIndex) {
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();

		if (GRR == null) {
			logger.error("SDHTable_XT GRR为空 不能继续刷新手牌");
			return;
		}
		if (GRR._card_count[seatIndex] < 0) {
			return;
		}

		_logic.sortCardList(GRR._cards_data[seatIndex], GRR._card_count[seatIndex]);

		gameStartResponse.clearCardData();
		for (int j = 0; j < GRR._card_count[seatIndex]; j++) {
			cards.addItem(GRR._cards_data[seatIndex][j]);
			gameStartResponse.addCardData(GRR._cards_data[seatIndex][j]);
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		TableResponseSdh.Builder tableResponseSdh = TableResponseSdh.newBuilder();
		tableResponseSdh.clearPlayerStatus();
		for (int i = 0; i < SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
			tableResponseSdh.addDiCardsData(seatIndex == this._banker_select ? this.diPai[i] : -2);
		}
		tableResponseSdh.setDiCardCount(SDHConstants_XT.SDH_DIPAI_COUNT);
		tableResponseSdh.setBankerPlayer(this._banker_select); // 庄家

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponseSdh));

		if (_cur_round == 1) {
			load_player_info_data(roomResponse);
		}
		roomResponse.setType(MsgConstants.RESPONSE_SDH_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setCurrentPlayer(seatIndex);
		roomResponse.setGameStatus(_game_status);
		this.load_room_info_data(roomResponse);

		GRR.add_room_response(roomResponse);
		send_response_to_player(seatIndex, roomResponse);

		if (has_rule(SDHConstants_XT.GAME_RULE_SDH_BAOFULIUSHOU) && seatIndex != this._banker_select && this.hasMain[seatIndex]) {
			this.hasMain[seatIndex] = this._logic.hasTheMain(GRR._cards_data[seatIndex][0]);
			if (!this.hasMain[seatIndex]) {
				roomResponse.clear();
				roomResponse.setType(SDHConstants_XT.RESPONSE_BAO_FU);
				roomResponse.setHuXiType(this.firstPlayer == seatIndex ? 1 : 0);
				roomResponse.setOperateCode(5);
				roomResponse.setCurrentPlayer(seatIndex);
				GRR.add_room_response(roomResponse);
				send_response_to_room(roomResponse);
			}
		}
	}

	/**
	 * 埋牌操作
	 * 
	 * @param seat_index
	 * @param cardsDataList
	 * @param cardsCount
	 * @return
	 */
	private boolean handlerMaiCard(int seat_index, List<Integer> cardsDataList, int cardsCount) {
		if (seat_index != _banker_select) {
			logger.error("不是庄家不能埋牌");
			return false;
		}
		if (cardsCount != SDHConstants_XT.SDH_DIPAI_COUNT) {
			logger.error("埋牌数量不对");
			return false;
		}
		if (cardsCount != cardsDataList.size()) {
			logger.error("埋牌数与实际埋牌数不一致");
			return false;
		}
		if (_game_status != SDHConstants_XT.GAME_STATUS_MAICARD) {
			logger.error("SDHTable_XT 不是埋牌状态");
			return false;
		}

		int[] removeDatas = new int[cardsCount];
		for (int i = 0; i < cardsCount; i++) {
			removeDatas[i] = cardsDataList.get(i);
		}
		_logic.removeCardsByData(GRR._cards_data[seat_index], GRR._card_count[seat_index], removeDatas, cardsCount, 0);
		GRR._card_count[seat_index] -= cardsCount;
		_logic.sortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index]);
		_logic.switch_cards_to_index(seat_index, GRR._cards_data[seat_index], GRR._card_count[seat_index], this.cardsValues[seat_index]);
		diPai = removeDatas;

		// 收起该玩家的效果通知
		operate_effect_action(seat_index, GameConstants.Effect_Action_Other, 2,
				new long[] { SDHConstants_XT.Player_Status_MAI_CARD, SDHConstants_XT.Player_Status_GIVE_UP }, 1, seat_index);

		this._game_status = SDHConstants_XT.GAME_STATUS_OUTCARD;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			refreshPlayerCards(i);
		}

		this.recordMaxCard();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this.refresh_player_score(i, 0);
		}
		switch_out_card(seat_index);

		return true;
	}

	@Override
	public boolean handlerCallBankerRequest(int seatIndex, int type, int score) {
		this.score = score;

		if (this._current_player != seatIndex) {
			logger.error("不是该玩家操作叫分" + seatIndex);
			return false;
		}
		this._current_player = seatIndex;
		if (SDHConstants_XT.SDH_ERROR_NUMBER == type) {
			this.score = SDHConstants_XT.SDH_ERROR_NUMBER;
		}

		_handler.exe(this);

		return true;
	}

	public boolean handlerMaiPaiRequest(int seatIndex) {
		this._current_player = seatIndex;

		_handler.exe(this);

		return true;
	}

	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

	/**
	 * 释放
	 */
	@Override
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null) {
			_table_scheduled.cancel(false);
		}

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
	 * 
	 * Release_Room_Type_SEND = 1, //发起解散 Release_Room_Type_AGREE, //同意
	 * Release_Room_Type_DONT_AGREE, //不同意 Release_Room_Type_CANCEL,
	 * //还没开始,房主解散房间 Release_Room_Type_QUIT //还没开始,普通玩家退出房间
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 150;
		this.beginTime = System.currentTimeMillis();
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}
		if (DEBUG_CARDS_MODE)
			delay = 10;
		int playerNumber = getTablePlayerNumber();
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GAME_STATUS_FREE == _game_status) {
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
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了
					count++;
				}
			}
			if (count == this.getTablePlayerNumber()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					player = this.get_players()[j];
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GAME_STATUS_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1) {
				return false;
			}

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;
				}
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}

			if (_release_scheduled != null) {
				_release_scheduled.cancel(false);
			}
			_release_scheduled = null;
			if (GRR == null) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GAME_STATUS_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null) {
				_release_scheduled.cancel(false);
			}
			_release_scheduled = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");
			}
			return false;
		}
		case GameConstants.Release_Room_Type_CANCEL: // 房主未开始游戏 解散
			if (GameConstants.GAME_STATUS_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}
				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
			break;
		case GameConstants.Release_Room_Type_QUIT: // 玩家未开始游戏 退出
			if (GameConstants.GAME_STATUS_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}
			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			this.load_room_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
			break;
		case GameConstants.Release_Room_Type_PROXY:
			// 游戏还没开始,不能解散
			if (GameConstants.GAME_STATUS_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			break;
		}

		return true;

	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {

		return true;
	}

	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	@Override
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);// 29
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	@Override
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);
		load_room_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, int winner) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌类型
		roomResponse.setCardTarget(this.firstOutColor); // 出牌花色
		roomResponse.setOperatePlayer(winner);// 设置当前轮最大牌玩家座位号
		roomResponse.setCardCount(count);
		roomResponse.setHuXiType(this.firstPlayer == seat_index ? 1 : 0);

		roomResponse.setFlashTime(150);
		roomResponse.setStandTime(1000);

		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards_data[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	/**
	 * 重连时使用
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards_data
	 * @param type
	 * @param to_player
	 * @param winner
	 * @return
	 */
	@Override
	public boolean operate_out_card_type() {
		if (this.nextPlayer == GameConstants.INVALID_SEAT) {
			return false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SDH_OUT_CARD_TYPE);
		roomResponse.setTarget(this.nextPlayer);
		roomResponse.setCardType(this.outcardHandler.firstType);// 出牌类型
		roomResponse.setCardCount(this.outcardHandler.firstCount);
		roomResponse.setCardTarget(this.firstOutColor);
		roomResponse.setOperatePlayer(this._logic.compareCardArrayWithOutLimit(this, this.getTablePlayerNumber(), this.outcardHandler.outCardsDatas,
				this.outcardHandler.firstCount, this.firstPlayer));// 设置当前轮最大牌玩家座位号

		roomResponse.setFlashTime(150);
		roomResponse.setStandTime(1000);

		return this.send_response_to_player(this.nextPlayer, roomResponse);
	}

	/**
	 * 效果
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	@Override
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		roomResponse.setScoreType(_di_fen);
		roomResponse.setGameStatus(this._game_status);
		if (this.outcardHandler.firstType != SDHConstants_XT.SDH_ERROR_NUMBER) {
			roomResponse.setCardCount(this.outcardHandler.firstCount);
		}
		this.load_room_info_data(roomResponse);

		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		time = time > 0 ? time : 0;
		roomResponse.setEffectTime(time);
		if (this._game_status == SDHConstants_XT.GAME_STATUS_JIAOZHUANG) {
			roomResponse.setQiangMin(this.minCall); // 可以叫的最低分
			roomResponse.setQiangMax(this.maxCall); // 可以叫的最高分
		} else if (this._game_status == SDHConstants_XT.GAME_STATUS_OUTCARD) {
			roomResponse.setIsXiangGong(this.firstPlayer == SDHConstants_XT.SDH_ERROR_NUMBER); // 是否首出
		}

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	/**
	 * 显示是哪个玩家在操作
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @return
	 */
	@Override
	public boolean showPlayerOperate(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SDH_OUT_CARD_PLAYER);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(this._current_player);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		time = time > 0 ? time : 0;
		roomResponse.setEffectTime(time);
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);
		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	@Override
	public boolean send_play_data(int seat_index) {
		return true;

	}

	/**
	 * 加载基础状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null) {
				continue;
			}
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(this.totalScore[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
		this.load_room_info_data(roomResponse);
	}

	@Override
	public boolean is_sdh_type(int type) {
		return _game_type_index == type;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	@Override
	public boolean exe_finish() {
		if (this._handler != null) {
			_handler = this._handler_finish;
			_handler.exe(this);
		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;
	}

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean exe_out_card(int seat_index, int card, int type) {
		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;
			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	@Override
	public void runnable_remove_middle_cards(int seat_index) {

	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		if (isTrustee) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee) {
			istrustee[get_seat_index] = isTrustee;
		}
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	public int getMaxValueSeatIndex(int seatIndex[], int index, int count) {
		int temp[] = new int[this.getTablePlayerNumber() + 1];
		for (int i = 0; i < count; i++) {
			temp[seatIndex[i]] = _logic.getCardSortOrder(this.GRR._cards_data[seatIndex[i]][index]);
		}
		int maxIndex[] = new int[count];
		Arrays.fill(maxIndex, -1);
		int maxCount = 0;
		int maxCount2 = 1;
		maxIndex[maxCount] = seatIndex[0];
		for (int i = 1; i < count; i++) {
			if (temp[seatIndex[i]] > temp[maxIndex[maxCount]]) {
				maxIndex[maxCount] = seatIndex[i];
				maxCount = 0;
				maxCount2 = 1;
			} else if (temp[seatIndex[i]] == temp[maxIndex[maxCount]]) {
				maxIndex[++maxCount] = seatIndex[i];
				maxCount2++;
			}
		}

		if (maxCount2 == 1) {
			for (int i = 0; i < maxIndex.length; i++) {
				if (maxIndex[i] != -1) {
					return maxIndex[i];
				}
			}
			// return maxIndex[maxCount];
		} else if (maxCount2 > 1) {
			return getMaxValueSeatIndex(maxIndex, index + 1, maxCount2);
		}
		return -1;
	}

	@Override
	public boolean exe_finish(int reason) {
		int maxIndex[] = new int[this.getTablePlayerNumber()];
		int maxCount = 1;
		for (int i = 1; i < this.getTablePlayerNumber(); i++) {
			if (this.ntMainCount[i] > this.ntMainCount[maxIndex[0]]) {
				maxIndex[0] = i;
				maxCount = 1;
			} else if (this.ntMainCount[i] == this.ntMainCount[maxIndex[0]]) {
				maxIndex[maxCount++] = i;
			}
		}
		int lostIndex = -1;
		if (maxCount > 1) {
			lostIndex = getMaxValueSeatIndex(maxIndex, 0, maxCount);
		} else {
			lostIndex = maxIndex[0];
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == lostIndex) {
				this.fail[i]++;
				int value = 6;
				this.totalScore[i] -= value;
				this._player_result.game_score[i] -= value;
				this.currentScore[i] -= value;
			} else {
				this.winFree[i]++;
				int value = 2;
				this.totalScore[i] += value;
				this._player_result.game_score[i] += value;
				this.currentScore[i] = value;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.maxWin[i] < this.currentScore[i]) {
				this.maxWin[i] = this.currentScore[i]; // 设置最大赢点
			}
			this.refresh_player_score(i, 0); // 刷新分数
		}
		this.handler_game_finish(0, reason);
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(this.get_players()[i].getAccount_id(), score, false,
					buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	@Override
	public boolean open_card_timer() {
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		return false;
	}

	@Override
	public boolean ready_timer() {
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	/**
	 * 收底牌
	 * 
	 * @param table
	 */
	@Override
	public void joinHandCards() {
		_playerStatus[_banker_select].set_status(SDHConstants_XT.Player_Status_MAI_CARD);
		for (int i = 0; i < SDHConstants_XT.SDH_DIPAI_COUNT; i++) {
			GRR._cards_data[_banker_select][i + handCardCount] = SDHConstants_XT.SDH_IS_DI_PAI | diPai[i];
			// GRR._cards_data[_banker_select][i + handCardCount] = diPai[i];
		}
		GRR._card_count[_banker_select] += SDHConstants_XT.SDH_DIPAI_COUNT;
		_logic.sortCardList(GRR._cards_data[_banker_select], GRR._card_count[_banker_select]);
		for (int i = 0; i < GRR._card_count[_banker_select]; i++) {
			if (!_logic.isTheMain(GRR._cards_data[_banker_select][i])) { // 不是常主
				this.colorNumber[_logic.getCardColor(GRR._cards_data[_banker_select][i])]++;
			}
		}
		this.refreshPlayerCards(_banker_select);
	}

	@Override
	public void recordMaxCard() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (GRR._card_count[i] > 0) {
				_logic.sortCardList(GRR._cards_data[i], GRR._card_count[i]);
				// 记录玩家每个花色的卡牌数据 并在玩家出牌之后记录该玩家每个花色最大的一张牌 甩牌初始判断
				_logic.switch_cards_to_index(i, GRR._cards_data[i], GRR._card_count[i], this.cardsValues[i]);
			}
		}
	}

	public boolean judgeAllhadNoMain() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i != this._banker_select && this.hasMain[i]) {
				return false;
			}
		}
		return true;
	}

	public void sendGuardInfo(int seatIndex) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());
		this.load_room_info_data(roomResponse);

		StallRate.Builder stallRate = StallRate.newBuilder();
		stallRate.setCurBanker(this._cur_banker);
		stallRate.setCurPlayer(seatIndex);

		for (int i = 0; i <= 4; i++) { // 可以叫的主花色及每个花色的数量
			stallRate.addMainColorList(i);
		}
		stallRate.setMainColorCount(5);

		roomResponse.setCommResponse(PBUtil.toByteString(stallRate));
		roomResponse.setType(SDHConstants_XT.RESPONSE_GUARD);// 报副
		send_response_to_player(seatIndex, roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
	}

}
