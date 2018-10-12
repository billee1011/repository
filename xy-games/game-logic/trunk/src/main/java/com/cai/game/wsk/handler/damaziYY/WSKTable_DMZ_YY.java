package com.cai.game.wsk.handler.damaziYY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.define.ERoomStatus;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dmz.DmzRsp.AnimationSeat;
import protobuf.clazz.dmz.DmzRsp.GameStart_Dmz;
import protobuf.clazz.dmz.DmzRsp.Ming_Pai_Result;
import protobuf.clazz.dmz.DmzRsp.Music_Anser_Dmz;
import protobuf.clazz.dmz.DmzRsp.Opreate_Ming_Pai_Request;
import protobuf.clazz.dmz.DmzRsp.OutCardDataDmz;
import protobuf.clazz.dmz.DmzRsp.PaiFenData;
import protobuf.clazz.dmz.DmzRsp.PukeGameEndDmz;
import protobuf.clazz.dmz.DmzRsp.RefreshScore;
import protobuf.clazz.dmz.DmzRsp.RoomInfoDmz;
import protobuf.clazz.dmz.DmzRsp.RoomPlayerResponseDmz;
import protobuf.clazz.dmz.DmzRsp.TableResponse_Dmz;
import protobuf.clazz.dmz.DmzRsp.TouXiang_Anser_Dmz;
import protobuf.clazz.dmz.DmzRsp.TouXiang_Result;

public class WSKTable_DMZ_YY extends AbstractWSKTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;
	protected static final int ID_TIMER_START_TO_SEND_CARD = 1;// 开始到发牌
	public int _xi_qian_times[];
	public int _xi_qian_score[];
	public int _xi_qian_total_score[];
	public int _get_score[];
	public int _turn_have_score; // 当局得分
	public long _init_account_id[];
	public int _is_bao_jing[];
	private Player[] _init_players;

	public int _end_score[];

	public WSKTable_DMZ_YY() {
		super(WSKType.GAME_TYPE_WSK_DMZ);
	}

	@Override
	protected void onInitTable() {

		_handler_out_card_operate = new WSKHandlerOutCardOperate_DMZ_YY();

		_end_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_total_score = new int[getTablePlayerNumber()];
		_get_score = new int[getTablePlayerNumber()];
		_is_ming_pai = new int[getTablePlayerNumber()];
		_init_account_id = new long[getTablePlayerNumber()];
		_is_bao_jing = new int[getTablePlayerNumber()];
		_is_tou_xiang = new int[getTablePlayerNumber()];
		_is_tou_xiang_agree = new int[getTablePlayerNumber()];

		Arrays.fill(_end_score, 0);
		Arrays.fill(_xi_qian_times, 0);
		Arrays.fill(_xi_qian_score, 0);
		Arrays.fill(_is_ming_pai, -1);
		Arrays.fill(_xi_qian_total_score, 0);
		Arrays.fill(_get_score, 0);
		_turn_have_score = 0;
		game_cell = 2;
		_shangyou_account_id = 0;

	}

	@Override
	public void progress_banker_select() {
		if (_shangyou_account_id == 0) {
			int have_card_seat[] = new int[2];
			have_card_seat[0] = GameConstants.INVALID_SEAT;
			have_card_seat[1] = GameConstants.INVALID_SEAT;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					if (this.GRR._cards_data[i][j] == _liang_card_value) {
						if (have_card_seat[0] == GameConstants.INVALID_SEAT) {
							have_card_seat[0] = i;
						} else {
							have_card_seat[1] = i;
						}
					}
				}
			}
			_cur_banker = have_card_seat[RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 2];
			if (have_card_seat[0] == GameConstants.INVALID_SEAT || have_card_seat[1] == GameConstants.INVALID_SEAT) {
				_cur_banker = 0;
			}

			GRR._banker_player = _cur_banker;
			_current_player = GRR._banker_player;
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Player player = null;
				player = this.get_players()[i];
				if (player != null && player.getAccount_id() == _shangyou_account_id) {
					_cur_banker = i;
				}
			}
			if (_cur_banker == GameConstants.INVALID_SEAT) {
				_cur_banker = 0;
			}
			GRR._banker_player = _cur_banker;
			_current_player = GRR._banker_player;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		if (this._cur_round == 0) {
			_init_players = new Player[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_init_account_id[i] = this.get_players()[i].getAccount_id();
				_init_players[i] = this.get_players()[i];
			}
		}
		reset_init_data();
		Arrays.fill(_end_score, 0);

		// if(this.has_rule(GameConstants.GAME_RULE_DMZ_MODUI)){
		// shuffle_players();
		// this.operate_player_data();
		// }

		this._current_player = _cur_banker;
		this._turn_out_card_count = 0;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_is_bao_jing[i] = 0;
			_is_tou_xiang_agree[i] = 0;
			_is_tou_xiang[i] = -1;
			_is_ming_pai[i] = -1;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}

		_game_status = GameConstants.GS_MJ_PLAY;
		_pai_score_count = 24;
		_pai_score = 200;
		_turn_have_score = 0;

		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		_repertory_card = new int[GameConstants.CARD_COUNT_WSK];
		shuffle(_repertory_card, GameConstants.CARD_DATA_WSK);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE)
				% GameConstants.CARD_COUNT_WSK];
		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "",
								GRR._cards_index[i][j], 0l, this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		if (has_rule(GameConstants.GAME_RULE_DMZ_MODUI)) {
			shuffle_players_data();
		}

		// 庄家选择
		this.progress_banker_select();

		if (has_rule(GameConstants.GAME_RULE_DMZ_MODUI) || this._cur_round == 1) {
			send_animation_seat();
		} else {
			on_game_start();
		}

		return true;
	}

	private void shuffle_players_data() {
		List<Player> pl = new ArrayList<Player>();
		int team_seat[] = new int[2];
		team_seat[0] = GameConstants.INVALID_SEAT;
		team_seat[1] = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (this.GRR._cards_data[i][j] == _liang_card_value) {
					if (team_seat[0] == GameConstants.INVALID_SEAT) {
						team_seat[0] = i;
					} else {
						team_seat[1] = i;
					}
				}
			}
		}
		if (team_seat[0] != team_seat[1] && team_seat[0] != GameConstants.INVALID_SEAT
				&& team_seat[1] != GameConstants.INVALID_SEAT) {
			// 换用户信息
			Player temp_player = new Player();
			temp_player = get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()] = get_players()[team_seat[1]];
			get_players()[team_seat[1]] = temp_player;
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()]
					.set_seat_index((team_seat[0] + 2) % this.getTablePlayerNumber());
			get_players()[team_seat[1]].set_seat_index(team_seat[1]);

			float temp_score = _player_result.game_score[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			_player_result.game_score[(team_seat[0] + 2)
					% this.getTablePlayerNumber()] = _player_result.game_score[team_seat[1]];
			_player_result.game_score[team_seat[1]] = temp_score;

			int xian_qian_total_temp = _xi_qian_total_score[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			_xi_qian_total_score[(team_seat[0] + 2) % this.getTablePlayerNumber()] = _xi_qian_total_score[team_seat[1]];
			_xi_qian_total_score[team_seat[1]] = xian_qian_total_temp;
			// 换用户牌数据
			int temp_data[] = new int[this.get_hand_card_count_max()];
			for (int j = 0; j < this.GRR._card_count[(team_seat[0] + 2) % this.getTablePlayerNumber()]; j++) {
				temp_data[j] = GRR._cards_data[(team_seat[0] + 2) % this.getTablePlayerNumber()][j];
			}
			for (int j = 0; j < this.GRR._card_count[(team_seat[0] + 2) % this.getTablePlayerNumber()]; j++) {
				GRR._cards_data[(team_seat[0] + 2) % this.getTablePlayerNumber()][j] = GRR._cards_data[team_seat[1]][j];
			}
			for (int j = 0; j < this.GRR._card_count[(team_seat[0] + 2) % this.getTablePlayerNumber()]; j++) {
				GRR._cards_data[team_seat[1]][j] = temp_data[j];
			}
		}

		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(_game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);
		this.send_response_to_room(roomResponse2);
		GRR.add_room_response(roomResponse2);

		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this);
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], GameConstants.WSK_ST_ORDER);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected void test_cards() {

		int cards[] = new int[] { 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11, 11,
				1 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 16) {
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
	public int get_hand_card_count_max() {
		return 27;
	}

	public void send_animation_seat() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_ANIMATION_SEAT);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		AnimationSeat.Builder animation_seat = AnimationSeat.newBuilder();
		animation_seat.setCardValue(_liang_card_value);
		animation_seat.setSeatIndex(this._cur_banker);
		roomResponse.setCommResponse(PBUtil.toByteString(animation_seat));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		set_timer(ID_TIMER_START_TO_SEND_CARD, 2);
	}

	@Override
	protected boolean on_game_start() {
		int FlashTime = 4000;
		int standTime = 1000;
		int displayertime = 10;
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
				.get(1104);
		if (sysParamModel1104 != null) {
			displayertime = sysParamModel1104.getVal1();
		}

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DMZ_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Dmz.Builder gamestart_dmz = GameStart_Dmz.newBuilder();
			RoomInfoDmz.Builder room_info = getRoomInfoDmz();
			gamestart_dmz.setRoomInfo(room_info);
			if (this._cur_round == 1) {
				this.load_player_info_data_game_start(gamestart_dmz);
			}
			this._current_player = GRR._banker_player;
			gamestart_dmz.setCurBanker(GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					gamestart_dmz.addCardCount(GRR._card_count[i]);
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else {
					gamestart_dmz.addCardCount(-1);
				}
				gamestart_dmz.addCardsData(cards_card);
			}
			gamestart_dmz.setDisplayTime(displayertime);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_dmz));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		Refresh_pai_score(GameConstants.INVALID_SEAT);
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data(roomResponse);
		// 发送数据
		GameStart_Dmz.Builder gamestart_dmz = GameStart_Dmz.newBuilder();
		RoomInfoDmz.Builder room_info = getRoomInfoDmz();
		gamestart_dmz.setRoomInfo(room_info);
		this.load_player_info_data_game_start(gamestart_dmz);
		this._current_player = GRR._banker_player;
		gamestart_dmz.setCurBanker(GRR._banker_player);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart_dmz.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart_dmz.addCardsData(cards_card);
		}

		gamestart_dmz.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_dmz));
		roomResponse.setFlashTime(FlashTime);
		roomResponse.setStandTime(standTime);

		GRR.add_room_response(roomResponse);

		this.set_handler(this._handler_out_card_operate);

		Refresh_pai_score(GameConstants.INVALID_SEAT);
		Refresh_user_get_score(GameConstants.INVALID_SEAT);
		// 自己才有牌数据
		return true;
	}

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000,
				TimeUnit.MILLISECONDS);
		_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
		this._cur_operate_time = time;

		return true;
	}

	public void kill_timer() {
		if (_game_scheduled != null) {
			_game_scheduled.cancel(false);
			_game_scheduled = null;
		}
	}

	@Override
	public boolean animation_timer(int timer_id) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_TIMER_START_TO_SEND_CARD: {
			on_game_start();
			return true;
		}
		}
		return true;
	}

	/**
	 * @return
	 */
	@Override
	public RoomInfoDmz.Builder getRoomInfoDmz() {
		RoomInfoDmz.Builder room_info = RoomInfoDmz.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setCreateName(this.getRoom_owner_name());

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void load_player_info_data_game_start(GameStart_Dmz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDmz.Builder room_player = RoomPlayerResponseDmz.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndDmz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDmz.Builder room_player = RoomPlayerResponseDmz.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_Dmz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDmz.Builder room_player = RoomPlayerResponseDmz.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore.Builder refresh_user_getscore = RefreshScore.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(_get_score[i]);
		}
		refresh_user_getscore.setTableScore(_turn_have_score);

		// refresh_user_getscore.setTableScore(value);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
	}

	@Override
	public void Refresh_pai_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_PAI_SCORE);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		PaiFenData.Builder pai_score_data = PaiFenData.newBuilder();

		// 先传方块
		int count = 0;
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		for (int i = 0; i < this._pai_score_count; i++) {
			if (this._logic.GetCardColor(_pai_score_card[i]) == 0) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);
		// 先传梅花
		count = 0;
		cards.clear();
		for (int i = 0; i < this._pai_score_count; i++) {
			if (this._logic.GetCardColor(_pai_score_card[i]) == 1) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);
		// 先传红心
		count = 0;
		cards.clear();
		for (int i = 0; i < this._pai_score_count; i++) {
			if (this._logic.GetCardColor(_pai_score_card[i]) == 2) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);

		// 先传黑桃
		count = 0;
		cards.clear();
		for (int i = 0; i < this._pai_score_count; i++) {
			if (this._logic.GetCardColor(_pai_score_card[i]) == 3) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);
		pai_score_data.setYuScore(_pai_score);
		roomResponse.setCommResponse(PBUtil.toByteString(pai_score_data));

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (_game_status == GameConstants.GS_MJ_PAO) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (seat_index == i) {
						continue;
					}
					this._player_result.pao[i] = 0;
				}
			}
		}
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		ret = this.on_handler_game_finish(seat_index, reason);
		return ret;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndDmz.Builder game_end_dmz = PukeGameEndDmz.newBuilder();
		RoomInfoDmz.Builder room_info = this.getRoomInfoDmz();
		game_end_dmz.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());

		int end_score[] = new int[this.getTablePlayerNumber()];
		int dang_ju_score[] = new int[this.getTablePlayerNumber()];
		int jia_fa_socre[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		Arrays.fill(jia_fa_socre, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_dmz(end_score, dang_ju_score, seat_index, jia_fa_socre);
		}
		this.load_player_info_data_game_end(game_end_dmz);
		game_end_dmz.setGameRound(_game_round);
		game_end_dmz.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				game_end_dmz.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_dmz.addCardsData(i, cards_card);
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
						game_end.addGameScore(end_score[j]);
						break;
					}
				}

			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);

		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_xi_qian_total_score[i] += _xi_qian_score[i];
			Player player = this.get_players()[i];
			if (player != null) {
				game_end.addGameScore(player.getGame_score());
			}
			game_end_dmz.addEndScore(end_score[i]);
			game_end_dmz.addRewardScore(this._xi_qian_score[i]);
			game_end_dmz.addDangJuScore(dang_ju_score[i]);
			game_end_dmz.addZhuaFen(this._get_score[i]);
			game_end_dmz.addJiaFaSocre(jia_fa_socre[i]);
			game_end_dmz.addWinOrder(_chuwan_shunxu[i]);
			game_end_dmz.addTeamNumber(i % 2);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_dmz.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_dmz.addAllRewardScore(_xi_qian_total_score[i]);
				}
				float change_score[] = new float[getTablePlayerNumber()];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
							change_score[i] = _player_result.game_score[j];

						}
					}
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					_player_result.game_score[i] = change_score[i];
					this.get_players()[i] = _init_players[i];
					this.get_players()[i].set_seat_index(i);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_dmz.addAllEndScore((int) this._player_result.game_score[i]);
				game_end_dmz.addAllRewardScore(_xi_qian_total_score[i]);
			}

			float change_score[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
						change_score[i] = _player_result.game_score[j];

					}
				}
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.game_score[i] = change_score[i];
				this.get_players()[i] = _init_players[i];
				this.get_players()[i].set_seat_index(i);
			}

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}

		game_end_dmz.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_dmz));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (!is_sys()) {
			GRR = null;
		}
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		Arrays.fill(_xi_qian_score, 0);
		Arrays.fill(_get_score, 0);
		// 错误断言
		return false;
	}

	public void cal_score_dmz(int end_score[], int dang_ju_fen[], int win_seat_index, int jia_fa_socre[]) {

		boolean is_touxiang = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._is_tou_xiang[i] == 1 && (i + 2) % getTablePlayerNumber() != win_seat_index) {
				is_touxiang = true;
				break;
			}
		}
		if (!is_touxiang) {
			int shang_you_score = 0;
			int xia_you_score = 0;
			shang_you_score += _get_score[this._chuwan_shunxu[0]]
					+ _get_score[(this._chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()];
			xia_you_score += _get_score[(this._chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()]
					+ _get_score[(this._chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()];

			if ((_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber() == _chuwan_shunxu[1]) {
				shang_you_score += 100;
				xia_you_score -= 100;
				jia_fa_socre[_chuwan_shunxu[0]] += 100;
				jia_fa_socre[_chuwan_shunxu[1]] += 100;
				jia_fa_socre[_chuwan_shunxu[2]] -= 100;
				jia_fa_socre[_chuwan_shunxu[3]] -= 100;
			} else if ((_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber() == _chuwan_shunxu[2]) {
				shang_you_score += 50;
				xia_you_score -= 50;
				jia_fa_socre[_chuwan_shunxu[0]] += 50;
				jia_fa_socre[_chuwan_shunxu[2]] += 50;
				jia_fa_socre[_chuwan_shunxu[1]] -= 50;
				jia_fa_socre[_chuwan_shunxu[3]] -= 50;
			}

			if (shang_you_score > xia_you_score) {
				int score = (int) this.game_cell;
				if (xia_you_score < 0) {
					score *= 2;
				}
				dang_ju_fen[_chuwan_shunxu[0]] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()] -= score;
			} else if (shang_you_score < xia_you_score) {
				int score = (int) this.game_cell;
				if (shang_you_score < 0) {
					score *= 2;
				}
				dang_ju_fen[_chuwan_shunxu[0]] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()] += score;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i];
				this._player_result.game_score[i] += end_score[i];
			}
		} else {
			dang_ju_fen[win_seat_index] = 1;
			dang_ju_fen[(win_seat_index + 2) % getTablePlayerNumber()] = 1;
			dang_ju_fen[(win_seat_index + 1) % getTablePlayerNumber()] = -1;
			dang_ju_fen[(win_seat_index + 3) % getTablePlayerNumber()] = -1;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// if(_xi_qian_score[i]>0){
				//
				// }
				_xi_qian_score[i] /= game_cell;
				end_score[i] += dang_ju_fen[i] + this._xi_qian_score[i];

				this._player_result.game_score[i] += end_score[i];
			}
		}

	}

	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player,
			boolean is_deal) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)

		int displayertime = 10;
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
				.get(1104);
		if (sysParamModel1104 != null) {
			displayertime = sysParamModel1104.getVal1();
		}

		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataDmz.Builder outcarddata = OutCardDataDmz.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DMZ_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);

			outcarddata.setDisplayTime(displayertime);
			outcarddata.setPrOutCardType(_turn_out_card_type);

			if (_is_shou_chu == 1) {
				outcarddata.setIsFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);
			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (is_deal) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == index || (GRR._card_count[index] == 0 && (index + 2) % this.getTablePlayerNumber() == i)) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else if (_is_ming_pai[i] == 1) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else if (GRR._card_count[i] <= 5) {
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
				} else {
					outcarddata.addHandCardCount(-1);
				}
				outcarddata.addIsMingPai(this._is_ming_pai[i]);
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}
			if (seat_index == GameConstants.INVALID_SEAT) {
				outcarddata.setIsBaoJing(0);
			} else {
				outcarddata.setIsBaoJing(_is_bao_jing[seat_index]);
			}
			Int32ArrayResponse.Builder friend_cards_card = Int32ArrayResponse.newBuilder();
			if (this._is_ming_pai[index] == 1 || _is_ming_pai[(index + 2) % getTablePlayerNumber()] == 1) {
				for (int j = 0; j < GRR._card_count[(index + 2) % this.getTablePlayerNumber()]; j++) {
					friend_cards_card.addItem(GRR._cards_data[(index + 2) % this.getTablePlayerNumber()][j]);
				}
			}
			outcarddata.setFriendHandCardsData(friend_cards_card);
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(index, roomResponse);
		}
		if (seat_index != GameConstants.INVALID_SEAT) {
			_is_bao_jing[seat_index] = 0;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataDmz.Builder outcarddata = OutCardDataDmz.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);

		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
		}
		// 上一出牌数据
		outcarddata.setPrCardsCount(this._turn_out_card_count);
		for (int i = 0; i < this._turn_out_card_count; i++) {
			outcarddata.addPrCardsData(this._turn_out_card_data[i]);
		}
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setCurPlayer(this._current_player);
		outcarddata.setDisplayTime(10);
		outcarddata.setPrOutCardType(_turn_out_card_type);

		if (_is_shou_chu == 1) {
			outcarddata.setIsFirstOut(true);
		} else {
			outcarddata.setIsFirstOut(false);
		}
		if (_turn_out_card_count == 0) {
			outcarddata.setIsCurrentFirstOut(1);

		} else {
			outcarddata.setIsCurrentFirstOut(0);
		}
		if (_current_player != GameConstants.INVALID_SEAT) {
			if (this.GRR._card_count[_current_player] == 0) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}
		} else {
			outcarddata.setIsHaveNotCard(0);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(this.GRR._cards_data[i][j]);
			}
			outcarddata.addHandCardsData(cards_card);
			outcarddata.addHandCardCount(this.GRR._card_count[i]);
			outcarddata.addWinOrder(this._chuwan_shunxu[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	@Override
	protected void set_result_describe() {
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_DMZ_OPERATE) {
			Opreate_Ming_Pai_Request req = PBUtil.toObject(room_rq, Opreate_Ming_Pai_Request.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getMusicIndex());
		}
		return true;
	}

	public void deal_ming_pai_request(int seat_index) {
		if (_is_ming_pai[seat_index] != -1) {
			return;
		}
		if (this.GRR._card_count[seat_index] > 20) {
			send_error_notify(seat_index, 2, "明牌必须小于20张");
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.send_error_notify(i, 2, "玩家" + this.get_players()[seat_index].getNick_name() + "明牌");
		}

		deal_ming_pai_anser_agree(seat_index);

		_is_ming_pai[seat_index] = 1;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DMZ_CT_ERROR,
				GameConstants.INVALID_SEAT, false);
	}

	public void deal_ming_pai_anser_agree(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_MINGPAI_RESULT);
		// 发送数据
		Ming_Pai_Result.Builder ming_pai_result = Ming_Pai_Result.newBuilder();
		ming_pai_result.setOpreateSeatIndex(seat_index);
		ming_pai_result.setOpreateStr(this.get_players()[seat_index].getNick_name() + "明牌");
		ming_pai_result.setIsAgree(1);

		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai_result));

		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		// this.send_response_to_player((seat_index+2)%getTablePlayerNumber(),
		// roomResponse);

	}

	public void deal_ming_pai_anser_disagree(int seat_index) {
		if (_is_ming_pai[(seat_index + 2) % getTablePlayerNumber()] != 0) {
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_MINGPAI_RESULT);
		// 发送数据
		Ming_Pai_Result.Builder ming_pai_result = Ming_Pai_Result.newBuilder();
		ming_pai_result.setOpreateSeatIndex(seat_index);
		ming_pai_result.setOpreateStr("对家不同意明牌");
		ming_pai_result.setIsAgree(0);

		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai_result));
		this.send_response_to_player((seat_index + 2) % getTablePlayerNumber(), roomResponse);

		_is_ming_pai[(seat_index + 2) % getTablePlayerNumber()] = -1;
	}

	public void deal_tou_xiang_request(int seat_index, int to_player) {
		if (!this.has_rule(GameConstants.GAME_RULE_DMZ_YOU_TOUXIANG)) {
			this.send_error_notify(seat_index, 2, "不能投降");
			return;
		}
		if (_is_tou_xiang[seat_index] != -1) {
			return;
		}
		if (this._game_status != GameConstants.GS_MJ_PLAY) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] != -1) {
				this.send_error_notify(seat_index, 2, "有玩家在等待头像回复");
				return;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DMZ_TOUXIANG_ANSER);
			// 发送数据
			TouXiang_Anser_Dmz.Builder tou_xiang_anser = TouXiang_Anser_Dmz.newBuilder();
			tou_xiang_anser.setOpreateSeatIndex(seat_index);
			if (i == (seat_index + 2) % this.getTablePlayerNumber()) {
				tou_xiang_anser.setOpreateStr(
						"您的对家[" + this.get_players()[seat_index].getNick_name() + "]请求投降，您是否同意投降(投降输一分)");
			} else {
				tou_xiang_anser.setOpreateStr(
						"您的对手[" + this.get_players()[seat_index].getNick_name() + "]请求投降，您是否接受对手的投降(接受投降赢一分)");
			}
			roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_anser));
			// 自己才有牌数据
			this.send_response_to_player(i, roomResponse);
		}

		_is_tou_xiang[seat_index] = 1;
	}

	public void deal_tou_xiang_anser_agree(int seat_index) {

		boolean is_have_tou_xiang = false;
		int tou_xiang_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] == 1) {
				is_have_tou_xiang = true;
				tou_xiang_seat = i;
				break;
			}
		}
		if (!is_have_tou_xiang || tou_xiang_seat == seat_index) {
			return;
		}
		_is_tou_xiang_agree[seat_index] = 1;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_TOUXIANG_RESULT);
		// 发送数据
		TouXiang_Result.Builder tou_xiang_result = TouXiang_Result.newBuilder();
		tou_xiang_result.setOpreateSeatIndex(seat_index);
		tou_xiang_result.setOpreateSeatIndex(tou_xiang_seat);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			tou_xiang_result.addIsAgreeTouxiang(_is_tou_xiang_agree[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);

		int win_sort = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang_agree[i] != 1 && _is_tou_xiang[i] != 1) {
				return;
			} else if (_is_tou_xiang_agree[i] == 1 && _is_tou_xiang[(i + 2) % this.getTablePlayerNumber()] != 1
					&& win_sort == GameConstants.INVALID_SEAT) {
				win_sort = i;
			}
		}
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_sort, GameConstants.Game_End_NORMAL), 3,
				TimeUnit.SECONDS);
	}

	public void deal_tou_xiang_anser_disagree(int seat_index) {

		boolean is_have_tou_xiang = false;
		int tou_xiang_seat = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_tou_xiang[i] == 1) {
				is_have_tou_xiang = true;
				tou_xiang_seat = i;
				break;
			}
		}
		if (!is_have_tou_xiang || tou_xiang_seat == seat_index) {
			return;
		}

		_is_tou_xiang_agree[seat_index] = -1;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_TOUXIANG_RESULT);
		// 发送数据
		TouXiang_Result.Builder tou_xiang_result = TouXiang_Result.newBuilder();
		tou_xiang_result.setOpreateSeatIndex(seat_index);
		tou_xiang_result.setRequestTouXiang(tou_xiang_seat);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			tou_xiang_result.addIsAgreeTouxiang(_is_tou_xiang_agree[i]);
		}
		tou_xiang_result.setOpreateStr(this.get_players()[seat_index].getNick_name() + "玩家不同意");
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_is_tou_xiang[i] = -1;
			_is_tou_xiang_agree[i] = 0;
		}

	}

	public void deal_music_opreate(int seat_index, int opreate_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_MUSIC_RESULT);
		// 发送数据
		Music_Anser_Dmz.Builder music_result = Music_Anser_Dmz.newBuilder();
		music_result.setOpreateSeatIndex(seat_index);
		music_result.setMusicIndex(opreate_index);

		roomResponse.setCommResponse(PBUtil.toByteString(music_result));
		this.send_response_to_room(roomResponse);
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int opreate_index) {
		switch (opreate_type) {
		case 1: {
			// 明牌请求
			deal_ming_pai_request(seat_index);
			return true;
		}
		case 2: {
			// 明牌同意
			deal_ming_pai_anser_agree(seat_index);
			return true;
		}
		case 3: {
			deal_ming_pai_anser_disagree(seat_index);
			return true;
		}
		case 4: {
			deal_music_opreate(seat_index, opreate_index);
			return true;
		}
		case 5: {
			// 投降请求
			deal_tou_xiang_request(seat_index, GameConstants.INVALID_SEAT);
			return true;
		}
		case 6: {
			// 明牌同意
			deal_tou_xiang_anser_agree(seat_index);
			return true;
		}
		case 7: {
			// 明牌同意
			deal_tou_xiang_anser_disagree(seat_index);
			return true;
		}
		}
		return true;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
				TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {

	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {

		return card;
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;

		return true;
	}

	public void rand_tuozi(int seat_index) {
		int num1 = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		int num2 = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(num1);
		roomResponse.addEffectsIndex(num2);
		roomResponse.setEffectTime(1500);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(500); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
