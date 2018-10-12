package com.cai.game.tdz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.TDZConstants;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddJettonRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OpenCardRunnable;
import com.cai.future.runnable.ReadyRunnable;
import com.cai.future.runnable.RobotBankerRunnable;
import com.cai.future.runnable.ShanXiTDZGameStartRunnable;
import com.cai.future.runnable.ShanXiTDZSendJettonInfoRunnable;
import com.cai.future.runnable.ShanXiTDZSendTouZiRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.tdz.handler.TDZHandler;
import com.cai.game.tdz.handler.TDZHandlerAddJetton;
import com.cai.game.tdz.handler.TDZHandlerCallBanker;
import com.cai.game.tdz.handler.TDZHandlerDispatchCard;
import com.cai.game.tdz.handler.TDZHandlerFinish;
import com.cai.game.tdz.handler.TDZHandlerOpenCard;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;
import com.google.common.base.Strings;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.tdz.TDZRsp.AddJetton_TDZ;
import protobuf.clazz.tdz.TDZRsp.CallBankerInfo_TDZ;
import protobuf.clazz.tdz.TDZRsp.CallBanker_TDZ;
import protobuf.clazz.tdz.TDZRsp.GameStart_TDZ;
import protobuf.clazz.tdz.TDZRsp.OpenCard_TDZ;
import protobuf.clazz.tdz.TDZRsp.PukeGameEndTDZ;
import protobuf.clazz.tdz.TDZRsp.RoomPlayerResponseTDZ;
import protobuf.clazz.tdz.TDZRsp.SendBanker_TDZ;
import protobuf.clazz.tdz.TDZRsp.SendCard_TDZ;
import protobuf.clazz.tdz.TDZRsp.SendTouZi_TDZ;
import protobuf.clazz.tdz.TDZRsp.Timer_OX_TDZ;

/**
 * 推对子 2017-10-10
 * 
 * @author hexinqi
 *
 */
public class TDZTable extends AbstractRoom {

	private static final long serialVersionUID = 7060061356475703643L;

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	public int _call_banker[]; // 用户是否叫庄
	public int _add_Jetton[]; // 用户下注
	public boolean _open_card[]; // 用户摊牌
	public int _next_banker; // 下一轮的庄家
	public boolean _player_status[]; // 用户状态
	public int _player_count; // 用户数量
	public int bankerScore; // 庄家分数
	public int initScore; // 初始化分数
	public int touzi1; // 骰子结果
	public int touzi2; // 骰子结果
	public String firstScore; // 第一局固定分数、从配置读取

	public int _jetton_info[]; // 下注数据
	public int _call_banker_info[]; // 叫庄信息
	public int _operate_start_time; // 操作开始时间
	public int _cur_operate_time; // 可操作时间
	public int _cur_game_timer; // 当前游戏定时器
	public boolean add; // 庄家是否已经加分

	public int _win_num[] = new int[this.getTablePlayerNumber()];// 赢的局数
	public int _lose_num[] = new int[this.getTablePlayerNumber()];// 输的局数
	public int _game_score_max[] = new int[this.getTablePlayerNumber()];// 得分最高

	public TDZCardGroup[] groups = new TDZCardGroup[getTablePlayerNumber()];

	private long _request_release_time;
	private ScheduledFuture<Object> _release_scheduled;
	private ScheduledFuture<Object> _table_scheduled;
	private ScheduledFuture<Object> _game_scheduled;

	public TDZHandler<? super TDZTable> _handler;

	public TDZHandlerDispatchCard<? super TDZTable> _handler_dispath_card;
	public TDZHandlerCallBanker<? super TDZTable> _handler_Call_banker;
	public TDZHandlerAddJetton<? super TDZTable> _handler_add_jetton;
	public TDZHandlerOpenCard<? super TDZTable> _handler_open_card;

	public TDZHandlerFinish _handler_finish; // 结束

	public int request_player_seat = GameConstants.INVALID_SEAT;
	int[] release_players;

	public TDZTable() {
		super(RoomType.OX, TDZConstants.TDZ_RULE_SIX);
		// 玩家
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_OX_FREE;

		// 游戏变量
		_player_ready = new int[getTablePlayerNumber()];
		_player_open_less = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_call_banker = new int[getTablePlayerNumber()]; // 用户是否叫庄
		_add_Jetton = new int[getTablePlayerNumber()]; // 用户下注
		release_players = new int[getTablePlayerNumber()]; // 用户下注
		_open_card = new boolean[getTablePlayerNumber()]; // 用户摊牌
		_player_status = new boolean[getTablePlayerNumber()];

		_call_banker_info = new int[5];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			groups[i] = new TDZCardGroup(this, i);
		}
		game_cell = 1;
		add = false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;

		if (has_rule(TDZConstants.TDZ_RULE_BASE_100)) { // 扣豆跟分数相关
			_game_round = 2;
		} else {
			_game_round = 1;
		}

		_cur_round = 0;
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_jetton_info = new int[getTablePlayerNumber()];

		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
		}

		game_cell = 1;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), getTablePlayerNumber());

		_handler_add_jetton = new TDZHandlerAddJetton();
		_handler_open_card = new TDZHandlerOpenCard();
		_handler_Call_banker = new TDZHandlerCallBanker();

		_handler_finish = new TDZHandlerFinish();
		_call_banker = new int[getTablePlayerNumber()]; // 用户是否叫庄
		_add_Jetton = new int[getTablePlayerNumber()]; // 用户下注
		_open_card = new boolean[getTablePlayerNumber()]; // 用户摊牌
		_player_status = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}
		_cur_banker = 0;
		if (has_rule(TDZConstants.TDZ_RULE_BASE_50)) {
			bankerScore = getRuleValue(TDZConstants.TDZ_RULE_BASE_50);
		} else if (has_rule(TDZConstants.TDZ_RULE_BASE_100)) {
			bankerScore = getRuleValue(TDZConstants.TDZ_RULE_BASE_100);
		}
		initScore = bankerScore;
		getFirstScore(bankerScore);

		this.setMinPlayerCount(this.getTablePlayerNumber());
	}

	public boolean reset_init_data() {
		record_game_room();

		GRR = new GameRoundRecord(getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[getTablePlayerNumber()];
		istrustee = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(TDZConstants.HAND_CARD_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}
		_call_banker = new int[getTablePlayerNumber()]; // 用户是否叫庄
		_add_Jetton = new int[getTablePlayerNumber()]; // 用户下注
		_open_card = new boolean[getTablePlayerNumber()]; // 用户摊牌
		_player_status = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}

		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;

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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null) {
				continue;
			}
			GRR._video_recode.addPlayers(getRoomPlayer(rplayer, i));
		}

		GRR._video_recode.setBankerPlayer(this._cur_banker);
		game_cell = 1;
		touzi1 = 0;
		touzi2 = 0;
		Arrays.fill(_jetton_info, -1);
		return true;
	}

	public RoomPlayerResponse.Builder getRoomPlayer(Player rplayer, int seatIndex) {
		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(rplayer.getAccount_id());
		room_player.setHeadImgUrl(rplayer.getAccount_icon());
		room_player.setIp(rplayer.getAccount_ip());
		room_player.setUserName(rplayer.getNick_name());
		room_player.setSeatIndex(rplayer.get_seat_index());
		room_player.setOnline(rplayer.isOnline() ? 1 : 0);
		room_player.setIpAddr(rplayer.getAccount_ip_addr());
		room_player.setSex(rplayer.getSex());
		room_player.setScore(_player_result.game_score[seatIndex]);
		room_player.setReady(_player_ready[seatIndex]);
		room_player.setOpenThree(_player_open_less[seatIndex] == 0 ? false : true);
		room_player.setMoney(rplayer.getMoney());
		room_player.setGold(rplayer.getGold());
		if (rplayer.locationInfor != null) {
			room_player.setLocationInfor(rplayer.locationInfor);
		}
		return room_player;
	}

	/**
	 * 游戏开始
	 */
	public boolean on_handler_game_start() {

		_game_status = GameConstants.GS_OX_FREE;

		reset_init_data();

		GRR._banker_player = _cur_banker;

		_repertory_card = new int[getCard().length];
		shuffle(_repertory_card, getCard());

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		return game_start();
	}

	public boolean game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态

		if (this._cur_round == 0) {
			reset_init_data();

			GRR._banker_player = _cur_banker;

			_repertory_card = new int[getCard().length];
			shuffle(_repertory_card, getCard());
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		if (this._cur_round > 1) {
			this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, _cur_operate_time, true);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
				if (has_rule(TDZConstants.TDZ_RULE_FANG_ZHU_ZHUANG) && this.get_players()[i].getAccount_id() == getCreate_player().getAccount_id()) {
					_cur_banker = this.get_players()[i].get_seat_index();
				}
			} else {
				this._player_status[i] = false;
			}
		}

		on_game_start();
		return true;
	}

	/**
	 * 叫庄
	 * 
	 * @return
	 */
	public boolean call_banker() {
		this._game_status = GameConstants.GS_OX_CALL_BANKER;// 叫庄

		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._player_status[i] = this.get_players()[i] != null;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		this.load_room_info_data(roomResponse);
		CallBankerInfo_TDZ.Builder call_banker_info = CallBankerInfo_TDZ.newBuilder();
		if (this._cur_round == 1) {
			this.load_player_info_data(roomResponse);
		}
		for (int j = 0; j < 2; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this._player_status[i] != true) {
				continue;
			}
			call_banker_info.setDisplayTime(_cur_operate_time);
			roomResponse.setType(TDZConstants.RESPONSE_CALL_BANKER);
			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_info));
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		this.load_player_info_data(roomResponse);
		GRR = new GameRoundRecord(getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, this._game_status);

		return true;
	}

	public void add_call_banker(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 回放数据
		CallBanker_TDZ.Builder call_banker = CallBanker_TDZ.newBuilder();
		call_banker.setSeatIndex(seat_index);
		call_banker.setCallBanker(_call_banker[seat_index]);
		roomResponse.setType(TDZConstants.RESPONSE_SELECT_BANKER);
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker));
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			this.send_response_to_player(i, roomResponse);
		}
		// 回放
		this.GRR.add_room_response(roomResponse);
	}

	public void add_jetton_ox(int seat_index) {
		if (GRR != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			// 回放数据
			AddJetton_TDZ.Builder add_jetton = AddJetton_TDZ.newBuilder();
			add_jetton.setSeatIndex(seat_index);
			add_jetton.setJettonScore(_add_Jetton[seat_index]);
			roomResponse.setType(TDZConstants.RESPONSE_ADD_JETTON);
			roomResponse.setCommResponse(PBUtil.toByteString(add_jetton));
			// 发送数据
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				this.send_response_to_player(i, roomResponse);
			}
			// 回放
			this.GRR.add_room_response(roomResponse);
		}
	}

	public void send_touzi_data() {
		this.touzi1 = RandomUtil.getRandomNumber(6) + 1;
		this.touzi2 = RandomUtil.getRandomNumber(6) + 1;

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(TDZConstants.RESPONSE_SEND_TOUZI);

		SendTouZi_TDZ.Builder touzi = SendTouZi_TDZ.newBuilder();
		touzi.addTouZi(touzi1);
		touzi.addTouZi(touzi2);
		if (this._cur_round == 1) { // 第一局固定分数
			touzi.setIsFirst(true);
			int count = this.getTablePlayerNumber();
			int touziValue = this.touzi1 + this.touzi2;
			int seatIndex = touziValue + count;
			String[] socres = this.firstScore.split(",");
			int index = 0;
			do {
				int seat = seatIndex-- % count;
				if (seat != this._cur_banker) {
					int score = Integer.parseInt(socres[index++]);
					touzi.addScore(score);
					this._jetton_info[seat] = score;
					this._add_Jetton[seat] = score;
					touzi.addSeatIndex(seat);
				}
			} while (seatIndex > touziValue);
		} else {
			touzi.setIsFirst(false);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(touzi));

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			this.send_response_to_player(i, roomResponse);
		}
		this.GRR.add_room_response(roomResponse);
	}

	public void send_card_data() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_OPEN_CARD;// 设置状态
		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			SendCard_TDZ.Builder send_card = SendCard_TDZ.newBuilder();
			send_card.addTouZi(touzi1);
			send_card.addTouZi(touzi2);
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(k == i ? GRR._cards_data[k][j] : GameConstants.BLACK_CARD);
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}
			send_card.setDisplayTime(this._cur_operate_time);

			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			roomResponse.setType(TDZConstants.RESPONSE_SEND_CARD);
			this.send_response_to_player(i, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(TDZConstants.RESPONSE_SEND_CARD);

		SendCard_TDZ.Builder send_card = SendCard_TDZ.newBuilder();
		send_card.addTouZi(touzi1);
		send_card.addTouZi(touzi2);
		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] == true) {
				for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			} else {
				for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			}
			send_card.addSendCard(k, cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return;
	}

	public void open_card(int seat_index) {
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		OpenCard_TDZ.Builder open_card = groups[seat_index].encode();
		roomResponse.setType(TDZConstants.RESPONSE_OPEN_CARD);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		this.GRR.add_room_response(roomResponse);
	}

	public void process_openCard() {
		// 从打骰子的玩家开始算分、顺时针 直到庄家分数为0时结束
		int touzi = this.touzi1 + this.touzi2;
		int seatIndex = touzi + getTablePlayerNumber();
		do {
			int i = seatIndex-- % getTablePlayerNumber();
			if (!(_player_status[i] == false || i == this._cur_banker)) {
				int baseScore = groups[i].calScore(groups[_cur_banker]);
				baseScore = baseScore * _add_Jetton[i];

				if (bankerScore <= baseScore) {
					baseScore = bankerScore;
				}
				// 得分
				GRR._game_score[i] += baseScore;
				GRR._game_score[this._cur_banker] -= baseScore;
				bankerScore -= baseScore;
			}
		} while (seatIndex > touzi && bankerScore > 0);
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > getTablePlayerNumber()) {
			return false;
		}
		return istrustee[seat_index];
	}

	protected int[] getCard() {
		return TDZConstants.TUI_DUI_ZI;
	}

	/**
	 * 洗牌
	 * 
	 * @param repertory_card
	 * @param mj_cards
	 */
	protected void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				TDZUtils.random_card_data(repertory_card, mj_cards);
			else
				TDZUtils.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * TDZConstants.HAND_CARD_COUNT + j];
			}
		}
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			groups[i].reset(GRR._cards_data[i], 1);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	public void updateJettonInfo(int level, int totalScore) {
		int end = level;
		for (int i = 0; i < 3 && end <= totalScore; i++) {
			this._jetton_info[i] = (i + 1) * level;
			end += level;
		}
	}

	public void sendBankerInfo() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_room_info_data(roomResponse);
		SendBanker_TDZ.Builder sendBanker = SendBanker_TDZ.newBuilder();
		sendBanker.setCurBanker(_cur_banker);
		if (this._cur_round == 1) {
			this.load_player_info_data(roomResponse);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(sendBanker));
		roomResponse.setType(TDZConstants.RESPONSE_SEND_BANKER);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			this.send_response_to_player(i, roomResponse);
		}
		this.GRR.add_room_response(roomResponse);
	}

	public void sendJettonInfo() {
		int level = initScore / 10;
		if (this._cur_round > 1) {
			updateJettonInfo(level, bankerScore);
		}
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			GameStart_TDZ.Builder game_start = GameStart_TDZ.newBuilder();
			game_start.setCurBanker(_cur_banker);
			if ((i != this._cur_banker && this._player_status[i] == true) && this._cur_round != 1) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true)) {
						for (int j = 0; j < 3; j++) {
							cards.addItem(_jetton_info[j]);
						}
					}
					game_start.addJettonCell(k, cards);
				}
			}
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			} else if (this._cur_round > 1) {
				if (bankerScore > level * 3) {
					game_start.setLevel(level);
					game_start.setMinScore(level * 4);
					game_start.setMaxScore(bankerScore);
				} else {
					game_start.setLevel(-1);
					game_start.setMinScore(-1);
					game_start.setMaxScore(-1);
				}
			}

			game_start.setDisplayTime(this._cur_operate_time);
			roomResponse.setCommResponse(PBUtil.toByteString(game_start));
			roomResponse.setType(TDZConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}
	}

	public void on_game_start() {
		sendBankerInfo();

		int delay = 2000;
		if (this._cur_round == 1) {
			delay += 1000;
		}
		GameSchedule.put(new ShanXiTDZSendTouZiRunnable(this.getRoom_id()), delay, TimeUnit.MILLISECONDS);

		delay += 2500;
		GameSchedule.put(new ShanXiTDZSendJettonInfoRunnable(this.getRoom_id()), delay, TimeUnit.MILLISECONDS);

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart_TDZ.Builder game_start = GameStart_TDZ.newBuilder();
		game_start.setCurBanker(_cur_banker);
		game_start.setDisplayTime(_cur_operate_time); // 减去延迟时间

		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (((k != this._cur_banker) && (this._player_status[k] == true))) {
				for (int j = 0; j < 3; j++) {
					cards.addItem(_jetton_info[j]);
				}
			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(game_start));
		roomResponse.setType(TDZConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);

		if (this._cur_round == 1) {
			GameSchedule.put(new ShanXiTDZGameStartRunnable(this.getRoom_id()), delay, TimeUnit.MILLISECONDS);
		} else {
			this._handler = _handler_add_jetton;
			_handler_add_jetton.reset_status(0, this._game_status);
		}
	}

	private void test_cards() {

		int cards[][] = { { 0x22, 0x28 }, { 0x29, 0x21 }, { 0x22, 0x23 }, { 0x22, 0x23 } };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[i][j];
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > TDZConstants.HAND_CARD_COUNT) {
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
	public void testRealyCard(int[] realyCards) {
		int count = realyCards.length / TDZConstants.HAND_CARD_COUNT;
		if (count > 6) {
			count = 6;
		}
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i++) {
				for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == count) {
				break;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < TDZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);
		return true;
	}

	/**
	 * 统计分数
	 * 
	 * @param _seat_index
	 */
	public void countAllScore(int _seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
			if (this.GRR._game_score[i] > _game_score_max[i]) {
				_game_score_max[i] = (int) this.GRR._game_score[i];
			}
			if (this.GRR._game_score[i] > 0) {
				_win_num[i]++;
			} else if (this.GRR._game_score[i] < 0) {
				_lose_num[i]++;
			}
		}
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return this.handler_game_finish_TDZ(seat_index, reason);
	}

	public boolean handler_game_finish_TDZ(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_READY_TIMER, GameConstants.HJK_READY_TIME_SECONDS, true);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(TDZConstants.RESPONSE_GAME_END);

		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndTDZ.Builder game_end_TDZ = PukeGameEndTDZ.newBuilder();

		this.load_room_info_data(roomResponse);

		game_end.setRoundOverType(1);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setCellScore(GameConstants.CELL_SCORE);

		game_end.setRoomInfo(roomResponse.getRoomInfo());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_TDZ.addWinCounts(_win_num[i]);
			game_end_TDZ.addLoseCounts(_lose_num[i]);
			game_end_TDZ.addScoreMax(_game_score_max[i]);
			game_end_TDZ.addScore(_player_result.game_score[i]);
		}

		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			// 特别显示的牌
			GRR._end_type = reason;
			this.load_player_info_data(roomResponse);
			game_end.setBankerPlayer(GRR._banker_player);// 专家

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(GRR._game_score[i]);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (this._player_result.game_score[this._cur_banker] <= 0 || this._player_result.game_score[this._cur_banker] >= initScore * 3) { // 庄家分数输完、或者赢了两倍底分
				end = true;
				game_end.setRoomOverType(1);
				this._player_result.game_score[this._cur_banker] -= initScore;
				game_end.setPlayerResult(this.process_player_result(reason));
				if (this._game_scheduled != null) {
					this.kill_timer();
				}
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN) {
				real_reason = reason;
			} else {
				real_reason = GameConstants.Game_End_RELEASE_PLAY;// 流局
			}
			game_end.setRoomOverType(1);
			if (add) {
				this._player_result.game_score[this._cur_banker] -= initScore;
			}
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		if (game_end.getRoomOverType() == 1) {
			game_end_TDZ.setBankerIndex(this._cur_banker);
			game_end_TDZ.setBaseScore(initScore);
		}

		game_end.setEndType(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(game_end_TDZ));

		// 总得分
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		if (null != GRR && _recordRoomRecord != null) {
			record_game_round(game_end, real_reason);
		}

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

		return false;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {// 取消准备
			if (this.get_players()[get_seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			_player_ready[get_seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(get_seat_index, roomResponse2);
			}
			return false;
		} else {
			return handler_player_ready(get_seat_index, is_cancel);
		}
	}

	/**
	 * 刷新玩家
	 * 
	 * @param seatIndex
	 */
	public void refreshPlayer(int seatIndex) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seatIndex, roomResponse);
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) { // 准备
			if (this.get_players()[seat_index] == null || (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)) {
				return false;
			}
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) { // 结束后刷新玩家
				refreshPlayer(seat_index);
			}
			return false;
		}
		if (this.get_players()[seat_index] == null || (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)) {
			return false;
		}
		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		if (this._cur_round > 0) { // 结束后刷新玩家
			refreshPlayer(seat_index);
		}
		_player_count = 0;
		int _cur_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}
		// 房间内所有玩家已准备 则开始游戏
		if ((_player_count == getTablePlayerNumber()) && (_player_count == _cur_count)) {
			// if ((_player_count >= 2) && (_player_count == _cur_count)) {
			// 除去首局叫庄第一把需要叫庄外、其他都是直接开始游戏
			if (this._cur_round == 0 && has_rule(TDZConstants.TDZ_RULE_QIANG_ZHUANG)) {
				this._cur_round++; // 临时方案、解决抢庄时游戏局数不对
				call_banker();
				this._cur_round--;
			} else {
				if (this._cur_round == 0) {
					this._player_result.game_score[this.getCreate_player().get_seat_index()] = initScore;
					add = true;
				}
				handler_game_start();
			}
		}

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
		}
		if (_gameRoomRecord != null) {
			if (request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}
				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		return true;
	}

	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		return true;
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
		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card, luoCode);
		}
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
	 * 叫庄
	 * 
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		if (this._handler != null) {
			this._handler.handler_call_banker(this, seat_index, call_banker);
		}
		return true;
	}

	/**
	 * 下注
	 * 
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		if (this._handler != null) {
			this._handler.handler_add_jetton(this, seat_index, jetton);
		}
		return true;
	}

	/**
	 * 开牌
	 * 
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		if (this._handler != null) {
			this._handler.handler_open_cards(this, seat_index, open_flag);
		}
		return true;
	}

	/**
	 * 释放
	 */
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);
		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null) {
					continue;
				}
				send_error_notify(i, 2, "游戏等待超时解散");
			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this.get_players()[i] = null;
		}

		if (_table_scheduled != null) {
			_table_scheduled.cancel(false);
		}
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id()); // 删除房间

		return true;

	}

	/**
	 * 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {

		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 150;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}
		if (DEBUG_CARDS_MODE)
			delay = 10;
		int playerNumber = getTablePlayerNumber();
		if (playerNumber == 0) {
			playerNumber = getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}
			if (this._game_scheduled != null) {
				this.kill_timer();
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

			for (int i = 0; i < playerNumber; i++) {
				release_players[i] = 0;
			}

			request_player_seat = seat_index;
			release_players[seat_index] = 1;// 同意

			int count = 0;
			for (int i = 0; i < playerNumber; i++) {
				if (release_players[i] == 1) {// 都同意了
					count++;
				}
			}
			if (count == playerNumber) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < playerNumber; j++) {
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
			roomResponse.setRequestPlayerSeat(request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (release_players[seat_index] == 1) {
				return false;
			}

			release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}

			this.send_response_to_room(roomResponse);
			for (int i = 0; i < playerNumber; i++) {
				if (get_players()[i] == null) {
					continue;
				}
				if (release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < playerNumber; j++) {
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
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			release_players[seat_index] = 2;
			request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.set_timer(this._cur_game_timer, this._cur_operate_time, false);
			Timer_OX_TDZ.Builder timer = Timer_OX_TDZ.newBuilder();
			timer.setDisplayTime(_cur_operate_time);
			roomResponse.setCommResponse(PBUtil.toByteString(timer));
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;

			for (int i = 0; i < playerNumber; i++) {
				release_players[i] = 0;
			}

			for (int j = 0; j < playerNumber; j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}
			return false;
		}
		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) { // 游戏已经开始
				return false;
			}
			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < playerNumber; i++) {
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
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出

			if (this._player_status[seat_index] == true) {
				if (GameConstants.GS_MJ_FREE != _game_status) {
					// 游戏已经开始
					return false;
				}
				send_error_notify(seat_index, 2, "您已经开始游戏了,不能退出游戏");
				return false;
			}
			// if (has_rule(GameConstants.GAME_RULE_AA_PAY)) {
			// this.huan_dou_aa(seat_index);
			// }
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());

			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				}
				if (this._player_status[i] == true) {
				}
				if (_player_ready[i] == 0) {
				}
				if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				}
			}
			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

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

	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 牌局中分数结算
	 * 
	 * @param seat_index
	 * @param score
	 * @return
	 */
	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

		return true;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}

		Arrays.fill(_player_result.win_order, -1);

		// 大赢家
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

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

	/**
	 * 加载基础状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(this._cur_banker);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			if (!Strings.isNullOrEmpty(GRR._especial_txt)) {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
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
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			room_player.setPao(_player_result.pao[i] < 0 ? 0 : _player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public List<RoomPlayerResponseTDZ> load_player_info_data() {
		Player rplayer;
		List<RoomPlayerResponseTDZ> temp = new ArrayList<>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null) {
				continue;
			}
			RoomPlayerResponseTDZ.Builder room_player = RoomPlayerResponseTDZ.newBuilder();
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
			temp.add(room_player.build());
		}
		return temp;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {
		this._handler = this._handler_finish;
		this._handler_finish.exe(this);
		return true;
	}

	/**
	 * 执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_card;
			this._handler_dispath_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		// operate_out_card(seat_index, 0, null, type,
		// GameConstants.INVALID_SEAT);
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
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee && SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			istrustee[get_seat_index] = isTrustee;
		}

		GRR.add_room_response(roomResponse);

		this.send_response_to_room(roomResponse);
		return false;
	}

	/**
	 * 获取房间可允许最大玩家数
	 */
	@Override
	public int getTablePlayerNumber() {
		if (has_rule(TDZConstants.TDZ_RULE_FOUR)) {
			return TDZConstants.TDZ_RULE_FOUR;
		}
		if (has_rule(TDZConstants.TDZ_RULE_SIX)) {
			return TDZConstants.TDZ_RULE_SIX;
		}
		return TDZConstants.TDZ_RULE_SIX;
	}

	@Override
	public boolean exe_finish(int reason) {
		return false;
	}

	@Override
	public int getGameTypeIndex() {
		return _game_type_index;
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public boolean set_timer(int timer_type, int time, boolean makeDBtimer) {
		SysParamModel sysParamModel = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(TDZConstants.TIMER_ID);
		if (sysParamModel == null || sysParamModel.getVal5() == 0) {
			return true;
		}
		if (makeDBtimer == false) {
			_cur_game_timer = timer_type;
			if (timer_type == GameConstants.HJK_READY_TIMER) {
				time = sysParamModel.getVal1();
				_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
				time = sysParamModel.getVal2();
				_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
				time = sysParamModel.getVal3();
				_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
				time = sysParamModel.getVal4();
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			}
			return true;
		}
		_cur_game_timer = timer_type;
		if (timer_type == GameConstants.HJK_READY_TIMER) {
			_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), sysParamModel.getVal1(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel.getVal1();
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), sysParamModel.getVal2(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel.getVal2();
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), sysParamModel.getVal3() + 5, TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel.getVal3();
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), sysParamModel.getVal4(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel.getVal4();
		}
		return true;
	}

	public void getFirstScore(int id) {
		SysParamModel sysParamModel = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(id);
		if (sysParamModel == null || Strings.isNullOrEmpty(sysParamModel.getStr1())) {
			return;
		}
		if (has_rule(TDZConstants.TDZ_RULE_FOUR)) {
			this.firstScore = sysParamModel.getStr1();
		} else if (has_rule(TDZConstants.TDZ_RULE_SIX)) {
			this.firstScore = sysParamModel.getStr2();
		}
	}

	public boolean kill_timer() {
		_game_scheduled.cancel(false);
		_game_scheduled = null;
		return false;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	public boolean open_card_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false) {
				continue;
			}
			if (this._open_card[i] == false) {
				this._handler.handler_open_cards(this, i, true);
			}
		}
		return false;
	}

	public boolean robot_banker_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false) {
				continue;
			}
			if (this._call_banker[i] == -1) {
				this._handler.handler_call_banker(this, i, 0);
			}
		}
		return false;
	}

	public boolean ready_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++)
			if (this.get_players()[i] != null) {
				if (this._player_ready[i] == 0) {
					handler_player_ready(i, false);
				}
			}
		return false;
	}

	public boolean add_jetton_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._add_Jetton[i] == 0) {
				this._handler.handler_add_jetton(this, i, 0);
			}
		}
		return false;
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
	}

	@Override
	public void clear_score_in_gold_room() {
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
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

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
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
}
