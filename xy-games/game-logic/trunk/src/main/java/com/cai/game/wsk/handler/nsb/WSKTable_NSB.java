package com.cai.game.wsk.handler.nsb;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.wsk.GameConstants_NSB;
import com.cai.common.define.ECardType;
import com.cai.common.define.ERoomStatus;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.WSKType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dmz.DmzRsp.AnimationSeat;
import protobuf.clazz.dmz.DmzRsp.GameStart_Dmz;
import protobuf.clazz.dmz.DmzRsp.MingPai_Anser_Dmz;
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

public class WSKTable_NSB extends AbstractWSKTable {

	private static final long serialVersionUID = 5730201960275122956L;

	public int show_card;

	public int _get_score[];

	public int _turn_have_score; // 当局得分

	public int[] all_shang_you;
	public int[] all_xia_you;

	public int[] player_zhua_fen_max = new int[4]; // 最高抓分

	public List<Integer[]> history_zhua_fen = Lists.newArrayList();

	public int[] player_tou_you_num = new int[4];

	public List<Map<Long, Long>> record_end_score = Lists.newArrayList();

	public long _init_account_id[];
	private Player[] _init_players;

	private int[] player_game_seat;

	protected static final int ID_TIMER_START_TO_SEND_CARD = 1;// 开始到发牌

	public WSKTable_NSB() {
		super(WSKType.GAME_TYPE_WSK_DMZ);
		_logic = new WSKGameLogic_NSB();
	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
	}

	@Override
	public int get_hand_card_count_max() {
		return 27;
	}

	@Override
	protected void onInitTable() {
		_handler_out_card_operate = new WSKHandlerOutCardOperat_NSB();

		_get_score = new int[getTablePlayerNumber()];
		_is_ming_pai = new int[getTablePlayerNumber()];
		player_game_seat = new int[getTablePlayerNumber()];
		Arrays.fill(_is_ming_pai, -1);
		Arrays.fill(_get_score, 0);
		_turn_have_score = 0;
		game_cell = 2;
		if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU)) {
			_game_round = 100;
		}
		all_shang_you = new int[_game_round];
		all_xia_you = new int[_game_round];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player_game_seat[i] = i;
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		if (this._cur_round == 1) {
			_init_players = new Player[this.getTablePlayerNumber()];
			_init_account_id = new long[getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_init_account_id[i] = this.get_players()[i].getAccount_id();
				_init_players[i] = this.get_players()[i];
			}
		}

		// 庄家选择
		this.progress_banker_select();
		this._current_player = _cur_banker;
		this._turn_out_card_count = 0;
		Arrays.fill(_turn_out_card_data, GameConstants_NSB.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			Arrays.fill(_cur_out_card_data[i], GameConstants_NSB.INVALID_CARD);
		}

		_game_status = GameConstants_NSB.GS_MJ_PLAY;

		Arrays.fill(_chuwan_shunxu, GameConstants_NSB.INVALID_SEAT);
		_repertory_card = new int[GameConstants_NSB.CARD_COUNT_WSK];
		shuffle(_repertory_card, GameConstants_NSB.CARD_DATA_WSK);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		do {
			_liang_card_value = _repertory_card[RandomUtil.getRandomNumber(Integer.MAX_VALUE) % GameConstants.CARD_COUNT_WSK];
		} while (_liang_card_value == 0x4E || _liang_card_value == 0x4F);
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

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}
		// 庄家选择
		this.progress_banker_select();

		on_game_start();

		return true;
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
	public boolean reset_init_data() {
		super.reset_init_data();

		_pai_score = 200;
		_pai_score_count = 24;
		_turn_have_score = 0;

		_get_score = new int[getTablePlayerNumber()];

		for (int seat = 0; seat < getTablePlayerNumber(); seat++) {
			_get_score[seat] = 0;
		}
		return true;
	}

	private void progress_first() {
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
		if (team_seat[0] != GameConstants.INVALID_SEAT) {
			_cur_banker = team_seat[0];
		}

		if (team_seat[0] != team_seat[1] && team_seat[0] != GameConstants.INVALID_SEAT && team_seat[1] != GameConstants.INVALID_SEAT) {
			// 换用户信息
			Player temp_player = new Player();
			temp_player = get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()] = get_players()[team_seat[1]];
			get_players()[team_seat[1]] = temp_player;
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()].set_seat_index((team_seat[0] + 2) % this.getTablePlayerNumber());
			get_players()[team_seat[1]].set_seat_index(team_seat[1]);
			int game_seat = player_game_seat[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			player_game_seat[(team_seat[0] + 2) % this.getTablePlayerNumber()] = team_seat[1];
			player_game_seat[team_seat[1]] = game_seat;

			float game_score = this._player_result.game_score[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			this._player_result.game_score[(team_seat[0] + 2) % this.getTablePlayerNumber()] = _player_result.game_score[team_seat[1]];
			_player_result.game_score[team_seat[1]] = game_score;
			int win_lose = this._player_result.win_num[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			this._player_result.win_num[(team_seat[0] + 2) % this.getTablePlayerNumber()] = _player_result.win_num[team_seat[1]];
			_player_result.win_num[team_seat[1]] = win_lose;
			win_lose = this._player_result.lose_num[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			this._player_result.lose_num[(team_seat[0] + 2) % this.getTablePlayerNumber()] = _player_result.lose_num[team_seat[1]];
			_player_result.lose_num[team_seat[1]] = win_lose;
			int max_zhua_fen = player_zhua_fen_max[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			player_zhua_fen_max[(team_seat[0] + 2) % this.getTablePlayerNumber()] = player_zhua_fen_max[team_seat[1]];
			player_zhua_fen_max[team_seat[1]] = max_zhua_fen;

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

		send_animation_seat();
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

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000, TimeUnit.MILLISECONDS);
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

	public boolean refresh_player_seat() {
		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this); // 换位置后同步到中心服

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);
		load_common_status(roomResponse);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_response_to_player(i, roomResponse);
			if (GRR != null) {
				this.GRR.add_room_response(roomResponse);
			}
		}

		return true;
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
			if (this._logic.GetCardColor(_pai_score_card[i]) == 0x10) {
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
			if (this._logic.GetCardColor(_pai_score_card[i]) == 0x20) {
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
			if (this._logic.GetCardColor(_pai_score_card[i]) == 0x30) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);
		pai_score_data.setYuScore(_pai_score);
		roomResponse.setCommResponse(PBUtil.toByteString(pai_score_data));

		if (to_player == GameConstants_NSB.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
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

		int beginLeftCard = GameConstants_NSB.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
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

	@Override
	protected boolean on_game_start() {
		int FlashTime = 4000;
		int standTime = 1000;

		if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU)) {
			if (this._cur_round == 1)
				progress_first();
		} else {
			progress_first();
		}

		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(_game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);
		this.send_response_to_room(roomResponse2);
		GRR.add_room_response(roomResponse2);

		GRR._banker_player = _cur_banker;
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DMZ_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_Dmz.Builder gamestart_dmz = GameStart_Dmz.newBuilder();
			RoomInfoDmz.Builder room_info = getRoomInfoDmz();
			gamestart_dmz.setRoomInfo(room_info);
			// if (this._cur_round == 1) {
			// progress_first(); // 找朋友
			// this.load_player_info_data_game_start(gamestart_dmz);
			// }
			this._current_player = GRR._banker_player;
			gamestart_dmz.setCurBanker(GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gamestart_dmz.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				gamestart_dmz.addCardsData(cards_card);
			}
			gamestart_dmz.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_dmz));

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		Refresh_pai_score(GameConstants_NSB.INVALID_SEAT);
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
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
			gamestart_dmz.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart_dmz.addCardsData(cards_card);
		}

		gamestart_dmz.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_dmz));

		int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
			FlashTime = sysParamModel1104.getVal1();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
			standTime = sysParamModel1104.getVal2();
		}
		roomResponse.setFlashTime(FlashTime);
		roomResponse.setStandTime(standTime);

		GRR.add_room_response(roomResponse);

		this.set_handler(this._handler_out_card_operate);

		Refresh_pai_score(GameConstants_NSB.INVALID_SEAT);
		Refresh_user_get_score(GameConstants_NSB.INVALID_SEAT);

		return true;
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
		int[] all_zhua_fen = new int[getTablePlayerNumber()];
		int[] shao_fen = new int[getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		Arrays.fill(jia_fa_socre, 0);
		Arrays.fill(shao_fen, 0);
		// 计算分数
		if (reason == GameConstants_NSB.Game_End_NORMAL) {
			cal_score_dmz(end_score, dang_ju_score, seat_index, jia_fa_socre, all_zhua_fen, shao_fen);
		}

		int[] qian_fen_all_zhua_fens = new int[getTablePlayerNumber()];
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			int history_index = (p + 2) % 2;
			int qian_fen_all_zhua_fen = 0;
			for (Integer[] history : history_zhua_fen) {
				qian_fen_all_zhua_fen += history[history_index];
			}
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (this.get_players()[p].getAccount_id() == _init_account_id[j]) {
					qian_fen_all_zhua_fens[j] = qian_fen_all_zhua_fen;
				}
			}
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

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (has_rule(GameConstants_NSB.GAME_RULE_DAN_JU)) {
				_game_round = 1;
				if (has_rule(GameConstants_NSB.GAME_RULE_DAN_JU_1)) {
					_game_round = 1;
				} else if (has_rule(GameConstants_NSB.GAME_RULE_DAN_JU_2)) {
					_game_round = 2;
				} else if (has_rule(GameConstants_NSB.GAME_RULE_DAN_JU_4)) {
					_game_round = 4;
				}

				if (_cur_round >= _game_round) {// 局数到了
					end = true;
					float change_score[] = new float[getTablePlayerNumber()];
					int[] player_zhua_fen_max_temp = new int[getTablePlayerNumber()];
					int[] win_num = new int[getTablePlayerNumber()];
					int[] lose_num = new int[getTablePlayerNumber()];
					int[] all_zhua_fen_temp = new int[getTablePlayerNumber()];
					int[] dang_ju_score_temp = new int[getTablePlayerNumber()];
					int[] jia_fa_socre_temp = new int[getTablePlayerNumber()];
					int[] shao_fen_temp = new int[getTablePlayerNumber()];
					boolean[] flag = new boolean[getTablePlayerNumber()];
					int[] get_score = new int[getTablePlayerNumber()];
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
								change_score[i] = _player_result.game_score[j];
								player_zhua_fen_max_temp[i] = player_zhua_fen_max[j];
								lose_num[i] = _player_result.lose_num[j];
								win_num[i] = _player_result.win_num[j];
								all_zhua_fen_temp[i] = all_zhua_fen[j];
								dang_ju_score_temp[i] = dang_ju_score[j];
								jia_fa_socre_temp[i] = jia_fa_socre[j];
								shao_fen_temp[i] = shao_fen[j];
								get_score[i] = _get_score[j];

								for (int c = 0; c < getTablePlayerNumber(); c++) {
									if (!flag[c] && get_players()[j].get_seat_index() == _chuwan_shunxu[c]) {
										_chuwan_shunxu[c] = i;
										flag[c] = true;
									}
								}
							}
						}
					}
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						_player_result.game_score[i] = change_score[i];
						_player_result.win_num[i] = win_num[i];
						_player_result.lose_num[i] = lose_num[i];
						player_zhua_fen_max[i] = player_zhua_fen_max_temp[i];
						all_zhua_fen[i] = all_zhua_fen_temp[i];
						dang_ju_score[i] = dang_ju_score_temp[i];
						jia_fa_socre[i] = jia_fa_socre_temp[i];
						shao_fen[i] = shao_fen_temp[i];
						_get_score[i] = get_score[i];

						this.get_players()[i] = _init_players[i];
						this.get_players()[i].set_seat_index(i);
					}
					game_end.setPlayerResult(this.process_player_result(reason));

					real_reason = GameConstants_NSB.Game_End_ROUND_OVER;
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						game_end_dmz.addAllEndScore((int) this._player_result.game_score[i]);
						game_end_dmz.addWinNum(_player_result.win_num[i]);
						game_end_dmz.addLoseNum(_player_result.lose_num[i]);
						game_end_dmz.addEndZhuaFenMax(player_zhua_fen_max[i]);

						get_players()[i].setGame_score(0);
					}
				}
			} else if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU)) {
				for (int player = 0; player < getTablePlayerNumber(); player++) {
					int history_index = (player + 2) % 2;
					int qian_fen_all_zhua_fen = 0;
					for (Integer[] history : history_zhua_fen) {
						qian_fen_all_zhua_fen += history[history_index];
					}

					int game_end_ji_fen_max = 300;
					if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU_300)) {
						game_end_ji_fen_max = 300;
					} else if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU_500)) {
						game_end_ji_fen_max = 500;
					} else if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU_1000)) {
						game_end_ji_fen_max = 1000;
					}

					if (qian_fen_all_zhua_fen >= game_end_ji_fen_max) {
						float change_score[] = new float[getTablePlayerNumber()];
						int[] temp_all_zhua_fen = new int[getTablePlayerNumber()];
						int[] temp_player_zhua_fen_max = new int[getTablePlayerNumber()];
						int[] temp_player_tou_you_num = new int[getTablePlayerNumber()];
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							for (int j = 0; j < getTablePlayerNumber(); j++) {
								if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
									change_score[i] = _player_result.game_score[j];
									temp_all_zhua_fen[i] = all_zhua_fen[j];
									temp_player_zhua_fen_max[i] = player_zhua_fen_max[j];
									temp_player_tou_you_num[i] = player_tou_you_num[j];
								}
							}
						}
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							_player_result.game_score[i] = change_score[i];
							all_zhua_fen[i] = temp_all_zhua_fen[i];
							player_zhua_fen_max[i] = temp_player_zhua_fen_max[i];
							player_tou_you_num[i] = temp_player_tou_you_num[i];
							this.get_players()[i] = _init_players[i];
							this.get_players()[i].set_seat_index(i);
						}
						end = true;
						game_end.setPlayerResult(this.process_player_result(reason));
						real_reason = GameConstants_NSB.Game_End_ROUND_OVER;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							game_end_dmz.addAllEndScore((int) this._player_result.game_score[i]);
							game_end_dmz.addEndZhuaFenMax(player_zhua_fen_max[i]);
							game_end_dmz.addAllTouYou(player_tou_you_num[i]);

							get_players()[i].setGame_score(0);
						}
						break;
					}
				}
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)

		{
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants_NSB.Game_End_RELEASE_PLAY;
			end = true;
			if (has_rule(GameConstants_NSB.GAME_RULE_DAN_JU)) {
				float change_score[] = new float[getTablePlayerNumber()];
				int[] player_zhua_fen_max_temp = new int[getTablePlayerNumber()];
				int[] win_num = new int[getTablePlayerNumber()];
				int[] lose_num = new int[getTablePlayerNumber()];
				int[] all_zhua_fen_temp = new int[getTablePlayerNumber()];
				int[] dang_ju_score_temp = new int[getTablePlayerNumber()];
				int[] jia_fa_socre_temp = new int[getTablePlayerNumber()];
				int[] shao_fen_temp = new int[getTablePlayerNumber()];
				boolean[] flag = new boolean[getTablePlayerNumber()];
				int[] get_score = new int[getTablePlayerNumber()];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
							change_score[i] = _player_result.game_score[j];
							player_zhua_fen_max_temp[i] = player_zhua_fen_max[j];
							lose_num[i] = _player_result.lose_num[j];
							win_num[i] = _player_result.win_num[j];
							all_zhua_fen_temp[i] = all_zhua_fen[j];
							dang_ju_score_temp[i] = dang_ju_score[j];
							jia_fa_socre_temp[i] = jia_fa_socre[j];
							shao_fen_temp[i] = shao_fen[j];
							get_score[i] = _get_score[j];

							for (int c = 0; c < getTablePlayerNumber(); c++) {
								if (!flag[c] && get_players()[j].get_seat_index() == _chuwan_shunxu[c]) {
									_chuwan_shunxu[c] = i;
									flag[c] = true;
								}
							}
						}
					}
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					_player_result.game_score[i] = change_score[i];
					_player_result.win_num[i] = win_num[i];
					_player_result.lose_num[i] = lose_num[i];
					player_zhua_fen_max[i] = player_zhua_fen_max_temp[i];
					all_zhua_fen[i] = all_zhua_fen_temp[i];
					dang_ju_score[i] = dang_ju_score_temp[i];
					jia_fa_socre[i] = jia_fa_socre_temp[i];
					shao_fen[i] = shao_fen_temp[i];
					_get_score[i] = get_score[i];

					this.get_players()[i] = _init_players[i];
					this.get_players()[i].set_seat_index(i);
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_dmz.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_dmz.addWinNum(_player_result.win_num[i]);
					game_end_dmz.addLoseNum(_player_result.lose_num[i]);
					game_end_dmz.addEndZhuaFenMax(player_zhua_fen_max[i]);

					get_players()[i].setGame_score(0);
				}
			} else if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU)) {

				float change_score[] = new float[getTablePlayerNumber()];
				int[] temp_all_zhua_fen = new int[getTablePlayerNumber()];
				int[] temp_player_zhua_fen_max = new int[getTablePlayerNumber()];
				int[] temp_player_tou_you_num = new int[getTablePlayerNumber()];
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
							change_score[i] = _player_result.game_score[j];
							temp_all_zhua_fen[i] = all_zhua_fen[j];
							temp_player_zhua_fen_max[i] = player_zhua_fen_max[j];
							temp_player_tou_you_num[i] = player_tou_you_num[j];
						}
					}
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					_player_result.game_score[i] = change_score[i];
					all_zhua_fen[i] = temp_all_zhua_fen[i];
					player_zhua_fen_max[i] = temp_player_zhua_fen_max[i];
					player_tou_you_num[i] = temp_player_tou_you_num[i];
					this.get_players()[i] = _init_players[i];
					this.get_players()[i].set_seat_index(i);
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_dmz.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_dmz.addEndZhuaFenMax(player_zhua_fen_max[i]);
					game_end_dmz.addAllTouYou(player_tou_you_num[i]);

					get_players()[i].setGame_score(0);
				}
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_dmz.addEndScore(all_zhua_fen[i]);
			game_end_dmz.addDangJuScore(dang_ju_score[i]);
			game_end_dmz.addZhuaFen(this._get_score[i]);
			game_end_dmz.addJiaFaSocre(jia_fa_socre[i]);
			game_end_dmz.addWinOrder(_chuwan_shunxu[i]);
			game_end_dmz.addShaoFen(shao_fen[i]);
		}

		load_player_info_data(roomResponse);
		game_end_dmz.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_dmz));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		// this.send_response_to_room(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_dmz.addAllZhuaFen(qian_fen_all_zhua_fens[i]);
		}

		roomResponse.setGameEnd(game_end);

		if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU)) {
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				int index = player;
				if (end) {
					index = player_game_seat[player];
				}
				int history_index = (index + 2) % 2;
				int history_index_next = (index + 1 + 2) % 2;
				for (Integer[] history : history_zhua_fen) {
					Int32ArrayResponse.Builder mei_ju_zhua_fen = Int32ArrayResponse.newBuilder();
					mei_ju_zhua_fen.addItem(history[history_index]);
					mei_ju_zhua_fen.addItem(history[history_index_next]);
					game_end_dmz.addMeiJuZhuaFen(mei_ju_zhua_fen);
				}
				game_end.setCommResponse(PBUtil.toByteString(game_end_dmz));
				roomResponse.setGameEnd(game_end);
				this.send_response_to_player(player, roomResponse);
				game_end_dmz.clearMeiJuZhuaFen();
			}
		} else {
			this.send_response_to_room(roomResponse);
		}

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

		Arrays.fill(_get_score, 0);
		// 错误断言
		return false;
	}

	public void cal_score_dmz(int end_score[], int dang_ju_fen[], int win_seat_index, int jia_fa_socre[], int[] all_zhua_fen, int[] shao_fen) {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			player_zhua_fen_max[player] = player_zhua_fen_max[player] > _get_score[player] ? player_zhua_fen_max[player] : _get_score[player];
		}
		int shang_you_score = 0;
		int xia_you_score = 0;
		shang_you_score += _get_score[this._chuwan_shunxu[0]] + _get_score[(this._chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()];
		xia_you_score += _get_score[(this._chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()]
				+ _get_score[(this._chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()];

		// if ((_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber() ==
		// _chuwan_shunxu[1]) {
		// shang_you_score += 100;
		// xia_you_score -= 100;
		// } else if ((_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber() ==
		// _chuwan_shunxu[2]) {
		// shang_you_score += 50;
		// xia_you_score -= 50;
		// }

		all_shang_you[_cur_round - 1] = _chuwan_shunxu[0] % 2;
		all_xia_you[_cur_round - 1] = _chuwan_shunxu[3] % 2;
		player_tou_you_num[_chuwan_shunxu[0]]++;

		if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU)) {
			int shang_you_continue = 0;
			int jia_fa_score = 0;
			for (int i = _cur_round - 1; i >= 0; i--) {
				if (all_shang_you[i] == _chuwan_shunxu[0] % 2) {
					shang_you_continue++;
				} else {
					break;
				}
			}
			if (shang_you_continue == 7) {
				jia_fa_score = 280;
			} else if (shang_you_continue == 5) {
				jia_fa_score = 200;
			} else if (shang_you_continue == 3) {
				jia_fa_score = 120;
			}
			jia_fa_socre[_chuwan_shunxu[0]] = jia_fa_score;
			jia_fa_socre[(_chuwan_shunxu[0] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()] += jia_fa_score;
			shang_you_score += jia_fa_score;

			jia_fa_score = 0;
			int xia_you_continue = 0;
			for (int i = _cur_round - 1; i >= 0; i--) {
				if (all_xia_you[i] == _chuwan_shunxu[3] % 2) {
					xia_you_continue++;
				} else {
					break;
				}
			}
			if (xia_you_continue == 7) {
				jia_fa_score = 280;
			} else if (xia_you_continue == 5) {
				jia_fa_score = 200;
			} else if (xia_you_continue == 3) {
				jia_fa_score = 120;
			}
			jia_fa_socre[_chuwan_shunxu[3]] = -jia_fa_score;
			jia_fa_socre[(_chuwan_shunxu[3] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()] = -jia_fa_score;
			xia_you_score -= jia_fa_score;
		}

		shao_fen[_chuwan_shunxu[3]] = _logic.GetCardScore(GRR._cards_data[_chuwan_shunxu[3]], GRR._card_count[_chuwan_shunxu[3]]) * 2;
		if (_chuwan_shunxu[3] == (this._chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()) {
			shang_you_score -= shao_fen[_chuwan_shunxu[3]];
		} else {
			xia_you_score -= shao_fen[_chuwan_shunxu[3]];
		}
		all_zhua_fen[_chuwan_shunxu[0]] = shang_you_score;
		all_zhua_fen[(_chuwan_shunxu[0] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()] = shang_you_score;
		if (_chuwan_shunxu[3] == (this._chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()) {
			all_zhua_fen[_chuwan_shunxu[1]] = xia_you_score;
			all_zhua_fen[(_chuwan_shunxu[1] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()] = xia_you_score;
		} else {
			all_zhua_fen[_chuwan_shunxu[3]] = xia_you_score;
			all_zhua_fen[(_chuwan_shunxu[3] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()] = xia_you_score;
		}

		Integer[] history = new Integer[2];
		history[(_chuwan_shunxu[0] + 2) % 2] = shang_you_score;
		history[(_chuwan_shunxu[0] + 1 + 2) % 2] = xia_you_score;
		history_zhua_fen.add(history);

		if (has_rule(GameConstants_NSB.GAME_RULE_DAN_JU)) {
			if (shang_you_score >= xia_you_score) {
				int score = 2;
				if (xia_you_score < 0) {
					score = 4;
				} else if (xia_you_score < 50) {
					score = 3;
				}
				dang_ju_fen[_chuwan_shunxu[0]] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()] -= score;
				_player_result.win_num[_chuwan_shunxu[0]]++;
				_player_result.win_num[(_chuwan_shunxu[0] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.lose_num[(_chuwan_shunxu[0] + 1 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.lose_num[(_chuwan_shunxu[0] + 3 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
			} else {
				int score = 2;
				if (shang_you_score < 0) {
					score = 4;
				} else if (shang_you_score < 50) {
					score = 3;
				}
				dang_ju_fen[_chuwan_shunxu[0]] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()] += score;
				_player_result.lose_num[_chuwan_shunxu[0]]++;
				_player_result.lose_num[(_chuwan_shunxu[0] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.win_num[(_chuwan_shunxu[0] + 1 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.win_num[(_chuwan_shunxu[0] + 3 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += dang_ju_fen[i];

				this._player_result.game_score[i] += end_score[i];
				get_players()[i].setGame_score((long) _player_result.game_score[i]);
			}
		} else {
			int[] history_all = new int[2];
			for (Integer[] fen : history_zhua_fen) {
				history_all[0] += fen[0];
				history_all[1] += fen[1];
			}

			int min_all = history_all[0];
			int max_all = history_all[1];
			if (min_all > history_all[1]) {
				min_all = history_all[1];
				max_all = history_all[0];
			}

			boolean is_qian_fen = false;
			int game_end_ji_fen_max = 300;
			if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU_300)) {
				game_end_ji_fen_max = 300;
			} else if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU_500)) {
				game_end_ji_fen_max = 500;
			} else if (has_rule(GameConstants_NSB.GAME_RULE_QIAN_FEN_JU_1000)) {
				game_end_ji_fen_max = 1000;
				is_qian_fen = true;
			}

			if (max_all < game_end_ji_fen_max)
				return;

			if (shang_you_score >= xia_you_score) {
				int score = (int) this.game_cell;
				if (is_qian_fen) {
					if (min_all < 500) {
						score = 40;
					} else {
						score = 20;
					}
				}
				dang_ju_fen[_chuwan_shunxu[0]] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()] -= score;
				_player_result.win_num[_chuwan_shunxu[0]]++;
				_player_result.win_num[(_chuwan_shunxu[0] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.lose_num[(_chuwan_shunxu[0] + 1 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.lose_num[(_chuwan_shunxu[0] + 3 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
			} else {
				int score = (int) this.game_cell;
				if (is_qian_fen) {
					if (min_all < 500) {
						score = 40;
					} else {
						score = 20;
					}
				}
				dang_ju_fen[_chuwan_shunxu[0]] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 2) % this.getTablePlayerNumber()] -= score;
				dang_ju_fen[(_chuwan_shunxu[0] + 1) % this.getTablePlayerNumber()] += score;
				dang_ju_fen[(_chuwan_shunxu[0] + 3) % this.getTablePlayerNumber()] += score;
				_player_result.lose_num[_chuwan_shunxu[0]]++;
				_player_result.lose_num[(_chuwan_shunxu[0] + 2 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.win_num[(_chuwan_shunxu[0] + 1 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
				_player_result.win_num[(_chuwan_shunxu[0] + 3 + getTablePlayerNumber()) % getTablePlayerNumber()]++;
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] += dang_ju_fen[i];

				this._player_result.game_score[i] += end_score[i];
				get_players()[i].setGame_score((long) _player_result.game_score[i]);
			}
		}
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player, boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)

		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataDmz.Builder outcarddata = OutCardDataDmz.newBuilder();
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
			if (is_deal) {
				outcarddata.setIsHaveNotCard(1);
			} else {
				outcarddata.setIsHaveNotCard(0);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == index || (index == (i + 2) % getTablePlayerNumber() && GRR._card_count[index] == 0)) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				} else if (this._is_ming_pai[i] == 1) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}

			if (this._is_ming_pai[index] == 1) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[(index + 2 + getTablePlayerNumber()) % getTablePlayerNumber()]; j++) {
					cards_card.addItem(this.GRR._cards_data[(index + 2 + getTablePlayerNumber()) % getTablePlayerNumber()][j]);
				}
				outcarddata.setFriendHandCardsData(cards_card);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(index, roomResponse);

		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataDmz.Builder outcarddata = OutCardDataDmz.newBuilder();
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
		if (_current_player != GameConstants_NSB.INVALID_SEAT) {
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
	protected void test_cards() {
		int cards0[] = new int[] { 6, 57, 41, 61, 9, 49, 56, 50, 5, 38, 2, 60, 25, 79, 53, 56, 42, 27, 36, 57, 53, 50, 4, 52, 10, 22, 43 };
		int cards1[] = new int[] { 1, 34, 13, 58, 24, 17, 12, 26, 19, 11, 59, 40, 49, 79, 58, 34, 59, 35, 25, 60, 36, 39, 7, 8, 44, 78, 33 };
		int cards2[] = new int[] { 41, 51, 13, 54, 35, 54, 37, 52, 21, 37, 23, 3, 18, 42, 28, 44, 20, 55, 40, 21, 17, 61, 5, 12, 3, 45, 11 };
		int cards3[] = new int[] { 20, 23, 24, 8, 7, 28, 29, 10, 18, 45, 29, 39, 6, 51, 26, 43, 38, 1, 55, 27, 33, 78, 19, 2, 22, 4, 9 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}

		for (int j = 0; j < 27; j++) {
			GRR._cards_data[0][j] = cards0[j];
			GRR._cards_data[1][j] = cards1[j];
			GRR._cards_data[2][j] = cards2[j];
			GRR._cards_data[3][j] = cards3[j];
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_MINGPAI_ANSER);
		// 发送数据
		MingPai_Anser_Dmz.Builder ming_pai_anser = MingPai_Anser_Dmz.newBuilder();
		ming_pai_anser.setOpreateSeatIndex(seat_index);
		ming_pai_anser.setOpreateStr("对家请求明牌是否同意？");

		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai_anser));

		// 自己才有牌数据
		this.send_response_to_player((seat_index + 2) % getTablePlayerNumber(), roomResponse);
		_is_ming_pai[seat_index] = 0;
	}

	public void deal_ming_pai_anser_agree(int seat_index) {
		if (_is_ming_pai[(seat_index + 2) % getTablePlayerNumber()] != 0) {
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_MINGPAI_RESULT);
		// 发送数据
		Ming_Pai_Result.Builder ming_pai_result = Ming_Pai_Result.newBuilder();
		ming_pai_result.setOpreateSeatIndex((seat_index + 2) % getTablePlayerNumber());
		ming_pai_result.setOpreateStr(this.get_players()[(seat_index + 2) % getTablePlayerNumber()].getNick_name() + "明牌");
		ming_pai_result.setIsAgree(1);

		roomResponse.setCommResponse(PBUtil.toByteString(ming_pai_result));

		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		// this.send_response_to_player((seat_index+2)%getTablePlayerNumber(),
		// roomResponse);

		_is_ming_pai[(seat_index + 2) % getTablePlayerNumber()] = 1;

		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DMZ_CT_ERROR, GameConstants.INVALID_SEAT, false);
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
		}
		return true;
	}

	@Override
	protected void set_result_describe() {
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

}
