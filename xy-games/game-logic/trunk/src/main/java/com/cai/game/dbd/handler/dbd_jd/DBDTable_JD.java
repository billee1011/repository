package com.cai.game.dbd.handler.dbd_jd;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.dbd.AbstractDBDTable;
import com.cai.game.dbd.DBDType;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dbd.DbdRsp.Call_Banker_DBD_Request;
import protobuf.clazz.dbd.DbdRsp.GameStartDBD;
import protobuf.clazz.dbd.DbdRsp.OutCardData;
import protobuf.clazz.dbd.DbdRsp.OutCardData_Request_DBD;
import protobuf.clazz.dbd.DbdRsp.PukeGameEndDbd;
import protobuf.clazz.dbd.DbdRsp.RoomInfoDbd;
import protobuf.clazz.dbd.DbdRsp.RoomPlayerResponseDbd;
import protobuf.clazz.dbd.DbdRsp.TableResponseDBD;

public class DBDTable_JD extends AbstractDBDTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;
	public static final int ID_TIMER_CALLBANKER_FINISH = 1;// 自动开始
	public int _out_card_times[];// 出牌次数

	public int _call_banker_score_max;
	public int _call_banker_score_min;
	public int _call_banker_score_current;
	public int _call_banker_score_current_max;
	public int _banker_call_score;
	public int _user_call_banker_socre[];
	public float _user_game_score[]; // 游戏分数
	public int _tang_zi_score;
	public int _di_chi_score;
	public int _prv_di_chi_score;
	public int _init_di_chi_score;
	public int _bu_tang_score[];
	public int _call_tang_score[];
	public int _di_pai_card_data[]; // 底牌扑克
	public int _di_pai_card_count; // 底牌数目
	public int _boom_score;
	public int _end_score[][];
	public int _call_banker_timer;
	public int _out_card_timer;

	public DBDTable_JD() {
		super(DBDType.GAME_TYPE_DBD);
	}

	@Override
	protected void onInitTable() {

		_handler_out_card_operate = new DBDHandlerOutCardOperate_JD();
		_handler_call_banker = new DBDHandlerCallBanker_JD();
		_user_call_banker_socre = new int[getTablePlayerNumber()];
		_out_card_times = new int[getTablePlayerNumber()];
		_bu_tang_score = new int[getTablePlayerNumber()];
		_call_tang_score = new int[getTablePlayerNumber()];
		_end_score = new int[_game_round][getTablePlayerNumber()];
		_user_game_score = new float[getTablePlayerNumber()];

		for (int i = 0; i < _game_round; i++) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				_end_score[i][j] = 0;
			}
		}
		for (int j = 0; j < getTablePlayerNumber(); j++) {
			_user_game_score[j] = 0;
		}

		_boom_score = 0;
		_di_pai_card_count = 5;
		_di_pai_card_data = new int[_di_pai_card_count];
		_call_banker_score_max = 0;
		_call_banker_score_current_max = 0;
		_call_banker_score_min = 0;
		_tang_zi_score = 0;
		_di_chi_score = 0;
		_prv_di_chi_score = 0;
		_init_di_chi_score = 0;
		_call_banker_timer = 10;
		_out_card_timer = 10;

	}

	public boolean reset_init_data() {

		if (_cur_round == 0) {
			record_game_room();
		}

		this._handler = null;

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_LAOPAI, get_hand_card_count_max(),
				GameConstants.MAX_INDEX_LAOPAI);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_LAOPAI_COUNT);
			_user_call_banker_socre[i] = -1;
			_out_card_times[i] = 0;
			_boom_score = 0;
		}
		_cur_round++;

		// 设置变量
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		istrustee = new boolean[getTablePlayerNumber()];

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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		return true;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家
			_cur_banker = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());
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

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		// 信阳麻将
		GRR._banker_player = GameConstants.INVALID_SEAT;
		_current_player = _cur_banker;
		_all_card_len = GameConstants.CARD_COUNT_DBD;
		GRR._left_card_count = this._all_card_len;

		_repertory_card = new int[GameConstants.CARD_COUNT_DBD];
		shuffle(_repertory_card, GameConstants.CARD_DATA_DBD);

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

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
			_bu_tang_score[i] = 0;
		}
		

		if (has_rule(GameConstants.GAME_RULE_DBD_DIFEN_TWO)) {
			this.game_cell = 2;
			_call_banker_score_max = 40;
			_tang_zi_score = 10;
		} else if (has_rule(GameConstants.GAME_RULE_DBD_DIFEN_THREE)) {
			this.game_cell = 3;
			_call_banker_score_max = 50;
			_tang_zi_score = 10;
		} else if (has_rule(GameConstants.GAME_RULE_DBD_DIFEN_FIVE)) {
			this.game_cell = 5;
			_call_banker_score_max = 100;
			_tang_zi_score = 20;
		}
		_call_banker_score_min = 0;
		_call_banker_score_current = _tang_zi_score / 2;
		_banker_call_score = 0;
		on_game_start();

		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_inital(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_inital() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_inital();
			GRR._left_card_count -= this.get_hand_card_count_inital();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}
		int di_pai_index = 0;
		for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
			_di_pai_card_data[di_pai_index++] = repertory_card[i];
		}

		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		int cards[] = new int[] { 0x4E, 0x01, 0x11, 0x21, 0x2D, 0x2D, 0x3D, 0x0B, 0x03, 0x13, 0x23, 0x33 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_inital(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_inital(); j++) {
				if (this._player_ready[i] == 1) {
					GRR._cards_data[i][j] = cards[j];
				}
			}
		}

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

	public int get_hand_card_count_inital() {
		return 12;
	}

	public int get_hand_card_count_max() {
		return GameConstants.DBD_MAX_COUT;
	}

	@Override
	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		_win_player = GameConstants.INVALID_SEAT;
		int FlashTime = 4000;
		int standTime = 1000;

		// 玩家补堂
		if (_di_chi_score < _tang_zi_score * getTablePlayerNumber()) {
			if (_di_chi_score == 0) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this._player_result.game_score[i] -= _tang_zi_score;
					_di_chi_score += _tang_zi_score;
					_bu_tang_score[i] += _tang_zi_score;
					_end_score[this._cur_round - 1][i] -= _tang_zi_score;
				}
			} else {
				if (_di_chi_score < _tang_zi_score * getTablePlayerNumber()) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						this._player_result.game_score[i] -= _tang_zi_score;
						_di_chi_score += _tang_zi_score;
						_bu_tang_score[i] += _tang_zi_score;
						_end_score[this._cur_round - 1][i] -= _tang_zi_score;
					}
				}
			}
		}
		this.operate_player_data();
		// 开局堂子
		_init_di_chi_score = _di_chi_score;

		if (_call_banker_score_max > _di_chi_score) {
			_call_banker_score_current_max = _di_chi_score - _di_chi_score % 5;
		} else {
			_call_banker_score_current_max = _call_banker_score_max;
		}

		// 初始化游戏变量
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DBD_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			load_player_info_data(roomResponse);
			// 发送数据
			GameStartDBD.Builder gamestart_dbd = GameStartDBD.newBuilder();
			RoomInfoDbd.Builder room_info = getRoomInfoDbd();
			gamestart_dbd.setRoomInfo(room_info);
			this.load_player_info_data_game_start(gamestart_dbd);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 手牌--将自己的手牌数据发给自己
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == play_index) {
					for (int j = 0; j < GRR._card_count[play_index]; j++) {
						cards_card.addItem(GRR._cards_data[play_index][j]);
					}
				}
				gamestart_dbd.addCardCount(GRR._card_count[i]);
				gamestart_dbd.addCardsData(cards_card);
			}
			gamestart_dbd.setDiPaiCardCount(GRR._left_card_count);
			gamestart_dbd.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_dbd));

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

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		GameStartDBD.Builder gamestart_dbd = GameStartDBD.newBuilder();
		RoomInfoDbd.Builder room_info = getRoomInfoDbd();
		gamestart_dbd.setRoomInfo(room_info);
		this.load_player_info_data_game_start(gamestart_dbd);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart_dbd.addCardCount(GRR._card_count[i]);
			gamestart_dbd.addCardsData(cards_card);
		}
		gamestart_dbd.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_dbd));

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

		this.set_handler(_handler_call_banker);
		this._handler_call_banker.reset_status(_current_player, _game_status);
		this._handler.exe(this);
		// 自己才有牌数据
		return true;
	}

	/**
	 * @return
	 */
	public RoomInfoDbd.Builder getRoomInfoDbd() {
		RoomInfoDbd.Builder room_info = RoomInfoDbd.newBuilder();
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

	public void load_player_info_data_game_start(GameStartDBD.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDbd.Builder room_player = RoomPlayerResponseDbd.newBuilder();
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndDbd.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDbd.Builder room_player = RoomPlayerResponseDbd.newBuilder();
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponseDBD.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDbd.Builder room_player = RoomPlayerResponseDbd.newBuilder();
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
			roomResponse.addPlayers(room_player);
		}
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
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

		int bomb_score[] = new int[this.getTablePlayerNumber()];
		int tang_zi_score[] = new int[this.getTablePlayerNumber()];
		int cell_score[] = new int[this.getTablePlayerNumber()];
		int cal_score[] = new int[getTablePlayerNumber()];
		Arrays.fill(bomb_score, 0);
		Arrays.fill(tang_zi_score, 0);
		Arrays.fill(cell_score, 0);
		Arrays.fill(cal_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_dbd_jd(cal_score, seat_index, bomb_score, tang_zi_score, cell_score);
		}

		if (GRR != null) {
			this.operate_player_data();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBD_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndDbd.Builder game_end_dbd = PukeGameEndDbd.newBuilder();
		RoomInfoDbd.Builder room_info = getRoomInfoDbd();
		game_end_dbd.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(_end_score[this._cur_round - 1][i]);
			}
		} else {

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this._di_chi_score -= _bu_tang_score[i] + _call_tang_score[i];
				game_end.addGameScore(_end_score[this._cur_round - 1][i] + _bu_tang_score[i] + _call_tang_score[i]);
			}
		}
		this.load_player_info_data_game_end(game_end_dbd);
		game_end_dbd.setGameRound(_game_round);
		game_end_dbd.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_dbd.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_dbd.addCardsData(i, cards_card);
				game_end_dbd.addCalScore(cal_score[i]);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			game_end_dbd.setBankerPlayer(GRR._banker_player);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_dbd.addBuTangScore(_bu_tang_score[i]);
			game_end_dbd.addTangZiScore(tang_zi_score[i]);
			game_end_dbd.addDiScore(cell_score[i]);
			game_end_dbd.addBoomScore(bomb_score[i]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					_user_game_score[i] += (float) _di_chi_score / getTablePlayerNumber();
					_player_result.game_score[i] = _user_game_score[i];
					game_end_dbd.addAllEndScore(_player_result.game_score[i]);
				}
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_end_score[this._cur_round - 1][i] += this._bu_tang_score[i];
				_end_score[this._cur_round - 1][i] += _call_tang_score[i];
				_user_game_score[i] += (float) _di_chi_score / getTablePlayerNumber();
				_player_result.game_score[i] = _user_game_score[i];
				game_end_dbd.addAllEndScore(_player_result.game_score[i]);
			}
		}
		for (int i = 0; i < this._game_round; i++) {
			Int32ArrayResponse.Builder end_score = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				end_score.addItem(_end_score[i][j]);
			}
			game_end_dbd.addEndScore(end_score);
		}
		game_end_dbd.setInitDiChiScore(_init_di_chi_score);
		game_end_dbd.setDiChiScore(_di_chi_score);
		game_end_dbd.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_dbd));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_bu_tang_score[i] = 0;
			_call_tang_score[i] = 0;
		}
		// 变量设置
		_prv_di_chi_score = _di_chi_score;
		// 错误断言
		return false;
	}

	public void cal_score_dbd_jd(int cal_score[], int win_seat_index, int bomb_score[], int tang_zi_score[], int cell_score[]) {

		if (win_seat_index == this.GRR._banker_player) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player) {
					continue;
				}
				_end_score[this._cur_round - 1][i] -= this.game_cell;
				_end_score[this._cur_round - 1][i] -= this._boom_score;
				cal_score[i] -= this.game_cell;
				cal_score[i] -= this._boom_score;
				cell_score[i] -= game_cell;
				_end_score[this._cur_round - 1][GRR._banker_player] += game_cell;
				_end_score[this._cur_round - 1][GRR._banker_player] += _boom_score;
				cal_score[GRR._banker_player] += game_cell;
				cal_score[GRR._banker_player] += _boom_score;
				cell_score[GRR._banker_player] += game_cell;
				bomb_score[i] -= _boom_score;
				bomb_score[GRR._banker_player] += _boom_score;
			}
			_end_score[this._cur_round - 1][GRR._banker_player] += this._user_call_banker_socre[GRR._banker_player];
			cal_score[GRR._banker_player] += this._user_call_banker_socre[GRR._banker_player];
			tang_zi_score[GRR._banker_player] += _user_call_banker_socre[GRR._banker_player];
			this._di_chi_score -= _user_call_banker_socre[GRR._banker_player];
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player) {
					continue;
				}
				_end_score[this._cur_round - 1][i] += this.game_cell;
				_end_score[this._cur_round - 1][i] += this._boom_score;
				_end_score[this._cur_round - 1][GRR._banker_player] -= game_cell;
				_end_score[this._cur_round - 1][GRR._banker_player] -= _boom_score;
				cal_score[i] += this.game_cell;
				cal_score[i] += this._boom_score;
				cal_score[GRR._banker_player] -= game_cell;
				cal_score[GRR._banker_player] -= _boom_score;
				cell_score[i] += game_cell;
				cell_score[GRR._banker_player] -= game_cell;
				bomb_score[i] += _boom_score;
				bomb_score[GRR._banker_player] -= _boom_score;
			}
			_end_score[this._cur_round - 1][GRR._banker_player] -= this._user_call_banker_socre[GRR._banker_player];
			cal_score[GRR._banker_player] -= this._user_call_banker_socre[GRR._banker_player];
			tang_zi_score[GRR._banker_player] -= _user_call_banker_socre[GRR._banker_player];
			this._di_chi_score += _user_call_banker_socre[GRR._banker_player];
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += cal_score[i];
			_user_game_score[i] = _player_result.game_score[i];
		}
	}

	public boolean animation_timer(int timer_id) {
		switch (timer_id) {
		case ID_TIMER_CALLBANKER_FINISH: {
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DBD_CT_ERROR, GameConstants.INVALID_SEAT);
			return true;
		}
		}
		return true;
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		for (int player_index = 0; player_index < this.getTablePlayerNumber(); player_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData.Builder outcarddata = OutCardData.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DBD_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (player_index == i) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}
				outcarddata.addUserCardsData(cards_card);
				outcarddata.addUserCardCount(GRR._card_count[i]);
			}
			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addChangeCardsData(this._turn_out_card_data[i]);
				outcarddata.addPrChangeCardsData(_turn_out_card_data[i]);
				outcarddata.setPrCardsCount(_turn_out_card_count);
				outcarddata.addPrCardsData(_turn_out_real_card_data[i]);
				outcarddata.setPrOutCardType(_turn_out_card_type);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setDisplayTime(_out_card_timer);
			outcarddata.setBombScore(_boom_score);

			if (_current_player != GameConstants.INVALID_SEAT) {
				if (this._turn_out_card_count == 0) {
					outcarddata.setCurPlayerYaPai(1);
					if (this._current_player == this.GRR._banker_player && _out_card_times[_current_player] == 0) {
						outcarddata.setIsFirstOut(2);
					} else {
						outcarddata.setIsFirstOut(1);
					}

				} else {
					if (this._logic.search_card_data(this._turn_out_card_data, _turn_out_real_card_data, _turn_out_card_count,
							GRR._cards_data[_current_player], GRR._card_count[_current_player])) {
						outcarddata.setCurPlayerYaPai(1);
					} else {
						outcarddata.setCurPlayerYaPai(0);
					}
					outcarddata.setIsFirstOut(0);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			this.send_response_to_player(player_index, roomResponse);
		}

		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardData.Builder outcarddata = OutCardData.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBD_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(this.GRR._cards_data[i][j]);
			}
			outcarddata.addUserCardsData(cards_card);
			outcarddata.addUserCardCount(GRR._card_count[i]);
		}
		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
		}
		// 上一出牌数据
		outcarddata.setPrCardsCount(this._turn_out_card_count);
		for (int i = 0; i < this._turn_out_card_count; i++) {
			outcarddata.addChangeCardsData(this._turn_out_card_data[i]);
		}
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setCurPlayer(this._current_player);
		outcarddata.setBombScore(_boom_score);
		outcarddata.setDisplayTime(10);

		if (this._turn_out_card_count == 0) {
			if (this._current_player == this.GRR._banker_player && _out_card_times[_current_player] == 0) {
				outcarddata.setIsFirstOut(2);
			} else {
				outcarddata.setIsFirstOut(1);
			}

		} else {
			outcarddata.setIsFirstOut(0);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	// 发送扑克
	public boolean exe_dispatch_card(int seat_index, int send_count) {

		GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, send_count, false), 200, TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
		if (_release_scheduled == null) {
			int release_players[] = new int[getTablePlayerNumber()];
			Arrays.fill(release_players, 2);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(100);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(seat_index);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime(50);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			send_response_to_room(roomResponse);
		}
		if (is_sys())
			return true;
		// if(GameConstants.GS_MJ_FREE != _game_status){
		// return handler_player_ready(seat_index, false);
		// }
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
		if (type == MsgConstants.REQUST_DBD_OUT_CARD_LAIZI) {

			OutCardData_Request_DBD req = PBUtil.toObject(room_rq, OutCardData_Request_DBD.class);
			// 逻辑处理
			return handler_operate_out_card_mul(seat_index, req.getOutCardsList(), req.getChangeOutCardsList(), req.getOutCardCount(),
					req.getBOutCardType());
		} else if (type == MsgConstants.REQUST_DBD_CALL_BANKER) {
			Call_Banker_DBD_Request req = PBUtil.toObject(room_rq, Call_Banker_DBD_Request.class);
			return handler_call_banker(seat_index, req.getSelectCallBankerScore());
		}

		return true;
	}

	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, List<Integer> change_list, int card_count, int b_out_card) {
		if (this.GRR._banker_player == GameConstants.INVALID_SEAT)
			return false;
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			int change_out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
				change_out_cards[i] = change_list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, change_out_cards, card_count, b_out_card);
			this._handler.exe(this);
		}
		return true;
	}

	public boolean handler_call_banker(int seat_index, int call_banker_score) {
		if (this._handler_call_banker != null) {
			this._handler = this._handler_call_banker;
			this._handler_call_banker.handler_call_banker(this, seat_index, call_banker_score);
		}
		return true;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY

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
		int num1 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		int num2 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

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
