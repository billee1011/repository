package com.cai.game.gdy.handler.gdy_jd;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.GDYAutoOutCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.gdy.AbstractGDYTable;
import com.cai.game.gdy.GDYType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gdy.gdyRsp.GameStart_Gdy;
import protobuf.clazz.gdy.gdyRsp.OutCardDataGdy;
import protobuf.clazz.gdy.gdyRsp.OutCardData_Request_GDY;
import protobuf.clazz.gdy.gdyRsp.PukeGameEndGdy;
import protobuf.clazz.gdy.gdyRsp.RefreshCardsGdy;
import protobuf.clazz.gdy.gdyRsp.SendCardsGdy;
import protobuf.clazz.gdy.gdyRsp.TableResponse_Gdy;

public class GDYTable_JD extends AbstractGDYTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	public int _boom_times;
	public int _boom_limit;
	public int _end_score[];
	public int _out_card_time;

	public GDYTable_JD() {
		super(GDYType.GAME_TYPE_GDY);
	}

	@Override
	protected void onInitTable() {

		_handler_out_card_operate = new GDYHandlerOutCardOperate_JD();
		_end_score = new int[getTablePlayerNumber()];
		_boom_times = 1;
		_boom_limit = 0;
		_all_card_len = GameConstants.CARD_COUNT_GDY;
		_logic.ruleMap = ruleMap;

		if (this.has_rule(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_TWO)) {
			_boom_limit = getRuleValue(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_TWO);
		} else if (this.has_rule(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_FOUR)) {
			_boom_limit = getRuleValue(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_FOUR);
		} else if (this.has_rule(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_EIGHT)) {
			_boom_limit = getRuleValue(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_EIGHT);
		} else if (this.has_rule(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_SIXTEEN)) {
			_boom_limit = getRuleValue(GameConstants.GAME_RULE_GDY_BOOM_LIMIT_SIXTEEN);
		}

		if (this.has_rule(GameConstants.GAME_RULE_GDY_TWO_SCORE)) {
			this.game_cell = getRuleValue(GameConstants.GAME_RULE_GDY_TWO_SCORE);
		} else if (this.has_rule(GameConstants.GAME_RULE_GDY_ONE_SCORE)) {
			this.game_cell = getRuleValue(GameConstants.GAME_RULE_GDY_ONE_SCORE);
		} else if (this.has_rule(GameConstants.GAME_RULE_GDY_FOUR_SCORE)) {
			this.game_cell = getRuleValue(GameConstants.GAME_RULE_GDY_FOUR_SCORE);
		} else {
			this.game_cell = getRuleValue(GameConstants.GAME_RULE_GDY_THREE_SCORE);
		}
		_out_card_time = 15;
		this.setMinPlayerCount(getTablePlayerNumber());
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		if (is_cancel) {
			_player_ready[seat_index] = 0;
		} else {
			_player_ready[seat_index] = 1;
		}

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIsCancelReady(is_cancel);
		send_response_to_room(roomResponse);

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public void progress_banker_select() {
		if (this.has_rule(GameConstants.GAME_RULE_GDY_WIN_ZHUANG)) {
			if (this._cur_round == 1) {
				_cur_banker = 0;// 创建者的玩家为专家
				_cur_banker = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());
			}
		} else if (has_rule(GameConstants.GAME_RULE_GDY_RAND_ZHUANG)) {
			_cur_banker = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());
		} else if (has_rule(GameConstants.GAME_RULE_GDY_OWNER_ZHUANG)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Player player = null;
				player = this.get_players()[i];
				if (player != null && player.getAccount_id() == getRoom_owner_account_id()) {
					_cur_banker = i;
					break;
				}
			}
			if (_cur_banker == GameConstants.INVALID_SEAT) {
				_cur_banker = 0;
			}
		} else {
			int min_value = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (min_value == 0) {
					min_value = GRR._cards_data[i][GRR._card_count[i] - 1];
					_cur_banker = i;
				} else if (_logic.GetCardLogicValue(min_value) > _logic
						.GetCardLogicValue(GRR._cards_data[i][GRR._card_count[i] - 1])) {
					min_value = GRR._cards_data[i][GRR._card_count[i] - 1];
					_cur_banker = i;
				} else if (_logic.GetCardLogicValue(min_value) == _logic
						.GetCardLogicValue(GRR._cards_data[i][GRR._card_count[i] - 1])) {
					if (min_value > GRR._cards_data[i][GRR._card_count[i] - 1]) {
						min_value = GRR._cards_data[i][GRR._card_count[i] - 1];
						_cur_banker = i;
					}
				}
			}
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
		Arrays.fill(_end_score, 0);

		// 信阳麻将

		GRR._left_card_count = this._all_card_len;
		_repertory_card = new int[GameConstants.CARD_COUNT_GDY];
		shuffle(_repertory_card, GameConstants.CARD_DATA_GDY);
		// 庄家选择
		this.progress_banker_select();
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

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

		if (DEBUG_CARDS_MODE) {
			int cards[] = new int[] { 7, 51, 10, 38, 58, 35, 37, 11, 56, 20, 44, 33, 43, 3, 24, 79, 55, 45, 40, 49, 6,
					23, 39, 1, 53, 13, 34, 36, 17, 27, 59, 5, 12, 26, 78, 19, 21, 25, 41, 9, 54, 61, 8, 4, 18, 60, 2,
					28, 57, 52, 22, 42, 29, 50 };
			repertory_card = cards;
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
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 0x4F, 0x04, 0x06, 0x07, 0x35, 0x1B, 0x0B };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < cards.length; j++) {
				GRR._cards_data[i][j] = cards[j];

			}
			GRR._card_count[i] = cards.length;
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 6) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				}
			}

		}
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}
	}

	public int get_hand_card_count_inital() {
		return 5;
	}

	public int get_hand_card_count_max() {
		return GameConstants.GAY_MAX_COUT;
	}

	@Override
	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		_win_player = GameConstants.INVALID_SEAT;
		_boom_times = 1;
		int FlashTime = 4000;
		int standTime = 1000;

		// 初始化游戏变量
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GDEYE_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			load_player_info_data(roomResponse);
			// 发送数据
			GameStart_Gdy.Builder gamestart_gdy = GameStart_Gdy.newBuilder();
			gamestart_gdy.setRoomInfo(this.getRoomInfo());
			this.load_player_info_data_game_start(gamestart_gdy);
			this._current_player = GRR._banker_player;
			gamestart_gdy.setCurBanker(GRR._banker_player);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gamestart_gdy.addCardCount(GRR._card_count[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				gamestart_gdy.addCardsData(i, cards_card);
			}

			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			gamestart_gdy.setDisplayTime(_out_card_time);
			gamestart_gdy.setLeftCardCount(GRR._left_card_count);
			gamestart_gdy.setGameCell((int) this.game_cell);
			gamestart_gdy.setTimesNum(this._boom_times);
			if (this.has_rule(GameConstants.GAME_RULE_GDY_CARD_MIN_ZHUANG)) {
				gamestart_gdy.setMinCardValue(GRR._cards_data[_current_player][GRR._card_count[_current_player] - 1]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_gdy));

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
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
		GameStart_Gdy.Builder gamestart_gdy = GameStart_Gdy.newBuilder();
		gamestart_gdy.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart_gdy);
		this._current_player = GRR._banker_player;
		gamestart_gdy.setCurBanker(GRR._banker_player);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart_gdy.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gamestart_gdy.addCardsData(i, cards_card);
		}

		gamestart_gdy.setDisplayTime(10);
		gamestart_gdy.setLeftCardCount(GRR._left_card_count);
		gamestart_gdy.setGameCell((int) this.game_cell);
		gamestart_gdy.setTimesNum(this._boom_times);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_gdy));

		int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
			FlashTime = sysParamModel1104.getVal1();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
			standTime = sysParamModel1104.getVal2();
		}
		roomResponse.setFlashTime(FlashTime);
		roomResponse.setStandTime(standTime);

		GRR.add_room_response(roomResponse);

		this._handler = this._handler_out_card_operate;

		exe_dispatch_card(_current_player, 1, true, 1000);

		if (has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
			this.set_timer(ID_TIMER_OUT_CARD, _out_card_time);
		}
		// 自己才有牌数据
		return true;
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
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_start(GameStart_Gdy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndGdy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_Gdy.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
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

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_gdy_jd(end_score, seat_index);
		}

		if (GRR != null) {
			this.operate_player_data();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndGdy.Builder game_end_gdy = PukeGameEndGdy.newBuilder();
		game_end_gdy.setRoomInfo(getRoomInfo());
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {

		}
		this.load_player_info_data_game_end(game_end_gdy);
		game_end_gdy.setGameRound(_game_round);
		game_end_gdy.setCurRound(_cur_round);
		game_end_gdy.setGameCell((int) this.game_cell);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.GRR._card_count[i] == 5) {
					game_end_gdy.addTimesNum(this._boom_times * 2);
				} else {
					game_end_gdy.addTimesNum(this._boom_times);
				}
				game_end.addGameScore(end_score[i]);
				game_end_gdy.addCardCount(GRR._card_count[i]);
				game_end_gdy.addEndScore(end_score[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this.has_rule(GameConstants.GAME_RULE_GDY_CARD_NUMBER)) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

				if (GRR._card_count[i] == 5) {
					game_end_gdy.addIsBeiGuan(1);
				} else {
					game_end_gdy.addIsBeiGuan(0);
				}
				game_end_gdy.addCardsData(i, cards_card);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_gdy.addAllEndScore((int) this._player_result.game_score[i]);
				}
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_gdy.addAllEndScore((int) this._player_result.game_score[i]);
			}
		}

		game_end_gdy.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_gdy));
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

		if (this.has_rule(GameConstants.GAME_RULE_GDY_WIN_ZHUANG)) {
			this._cur_banker = seat_index;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			istrustee[i] = false;
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(i);
			roomResponse.setIstrustee(istrustee[i]);
			this.send_response_to_room(roomResponse);
		}
		// 错误断言
		return false;
	}

	public void cal_score_gdy_jd(int end_score[], int win_seat_index) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (win_seat_index == i) {
				continue;
			}
			if (this.GRR._card_count[i] == 5) {
				end_score[i] -= this.GRR._card_count[i] * this._boom_times * 2 * game_cell;
				end_score[win_seat_index] += this.GRR._card_count[i] * this._boom_times * 2 * game_cell;
			} else {
				if (!this.has_rule(GameConstants.GAME_RULE_GDY_ONE_CARD_SCORE) && GRR._card_count[i] == 1) {
					continue;
				}
				end_score[i] -= this.GRR._card_count[i] * this._boom_times * game_cell;
				end_score[win_seat_index] += this.GRR._card_count[i] * this._boom_times * game_cell;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += end_score[i];
		}
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards() {
		for (int palyer_index = 0; palyer_index < this.getTablePlayerNumber(); palyer_index++) {
			//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RefreshCardsGdy.Builder refresh_cards = RefreshCardsGdy.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GDEYE_REFRESH_PLAYER_CARDS);// 201
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				refresh_cards.addCardCount(this.GRR._card_count[i]);
				if (i == palyer_index) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards.addItem(this.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}
				refresh_cards.addCardsData(cards);
			}
			refresh_cards.setLeftCardCount(GRR._left_card_count);
			refresh_cards.setTimesNum(this._boom_times);
			refresh_cards.setGameCell((int) this.game_cell);

			roomResponse.setCommResponse(PBUtil.toByteString(refresh_cards));
			this.send_response_to_player(palyer_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshCardsGdy.Builder refresh_cards = RefreshCardsGdy.newBuilder();

		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_REFRESH_PLAYER_CARDS);// 201
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			refresh_cards.addCardCount(this.GRR._card_count[i]);
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards.addItem(this.GRR._cards_data[i][j]);
			}
			refresh_cards.addCardsData(cards);
		}
		refresh_cards.setLeftCardCount(GRR._left_card_count);
		refresh_cards.setTimesNum(this._boom_times);
		refresh_cards.setGameCell((int) this.game_cell);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_cards));

		GRR.add_room_response(roomResponse);
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int change_cards_data[], int type,
			int first_out, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		boolean is_can_guan = false;
		for (int player_index = 0; player_index < this.getTablePlayerNumber(); player_index++) {
			//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataGdy.Builder outcarddata = OutCardDataGdy.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			roomResponse.setType(MsgConstants.RESPONSE_GDEYE_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);
			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addChangeCardsData(change_cards_data[i]);
				outcarddata.addPrCardsData(this._turn_out_card_data[i]);
				outcarddata.addPrChangeCardsData(this._turn_out_card_data[i]);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder hand_cards = Int32ArrayResponse.newBuilder();
				if (i == player_index) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						hand_cards.addItem(this.GRR._cards_data[i][j]);
					}
				}
				outcarddata.addHandCardsData(hand_cards);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setPrOutCardType(this._turn_out_card_type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setIsFirstOut(first_out);
			outcarddata.setLeftCardCount(GRR._left_card_count);
			outcarddata.setTimesNum(this._boom_times);
			outcarddata.setGameCell((int) this.game_cell);
			if (this._turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);
			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (this._turn_out_card_count == 0) {
				outcarddata.setCurPlayerYaPai(1);
				outcarddata.setDisplayTime(_out_card_time);
				is_can_guan = true;

			} else {
				if (_current_player != GameConstants.INVALID_SEAT) {
					if (this._logic.search_card_data(_turn_out_card_data, _turn_out_card_count,
							this.GRR._cards_data[_current_player], GRR._card_count[_current_player])) {
						outcarddata.setCurPlayerYaPai(1);
						outcarddata.setDisplayTime(_out_card_time);
						is_can_guan = true;
					} else {
						outcarddata.setCurPlayerYaPai(0);
						if (player_index == _current_player) {
							outcarddata.setDisplayTime(3);
						} else {
							outcarddata.setDisplayTime(_out_card_time);
						}

					}
				} else {
					outcarddata.setCurPlayerYaPai(1);
					outcarddata.setDisplayTime(_out_card_time);
				}
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			this.send_response_to_player(player_index, roomResponse);
		}
		if (has_rule(GameConstants.GAME_RULE_GDY_CHU_FINISH)) {
			if (!is_can_guan && GRR._card_count[seat_index] != 0) {
				if (this.istrustee[_current_player]) {
					_auto_out_card_scheduled = GameSchedule.put(
							new GDYAutoOutCardRunnable(getRoom_id(), _current_player, this, null, null, 0, 0), 1,
							TimeUnit.SECONDS);
				} else {
					_auto_out_card_scheduled = GameSchedule.put(
							new GDYAutoOutCardRunnable(getRoom_id(), _current_player, this, null, null, 0, 0), 3,
							TimeUnit.SECONDS);
				}

			} else {
				if (_current_player != GameConstants.INVALID_SEAT && this._turn_out_card_count != 0) {
					if (this.istrustee[_current_player]) {
						AI_out_card_data();
					}
					if (has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
						this.set_timer(ID_TIMER_OUT_CARD, _out_card_time);
					}
				} else if (_current_player != GameConstants.INVALID_SEAT && this.GRR._left_card_count == 0) {
					if (this.istrustee[_current_player]) {
						AI_out_card_data();
					}
					if (has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
						this.set_timer(ID_TIMER_OUT_CARD, _out_card_time);
					}
				}

			}
		} else {
			if (!is_can_guan && _current_player != GameConstants.INVALID_SEAT) {
				if (this.istrustee[_current_player]) {
					_auto_out_card_scheduled = GameSchedule.put(
							new GDYAutoOutCardRunnable(getRoom_id(), _current_player, this, null, null, 0, 0), 1,
							TimeUnit.SECONDS);
				} else {
					_auto_out_card_scheduled = GameSchedule.put(
							new GDYAutoOutCardRunnable(getRoom_id(), _current_player, this, null, null, 0, 0), 3,
							TimeUnit.SECONDS);
				}

			} else {
				if (_current_player != GameConstants.INVALID_SEAT && this._turn_out_card_count != 0) {
					if (this.istrustee[_current_player]) {
						AI_out_card_data();
					}
					if (has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
						this.set_timer(ID_TIMER_OUT_CARD, _out_card_time);
					}
				} else if (_current_player != GameConstants.INVALID_SEAT && this.GRR._left_card_count == 0) {
					if (this.istrustee[_current_player]) {
						AI_out_card_data();
					}
					if (has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
						this.set_timer(ID_TIMER_OUT_CARD, _out_card_time);
					}
				}

			}
		}

		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataGdy.Builder outcarddata = OutCardDataGdy.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);
		for (int j = 0; j < count; j++) {
			outcarddata.addCardsData(cards_data[j]);
		}
		// 上一出牌数据
		outcarddata.setPrCardsCount(this._turn_out_card_count);
		for (int i = 0; i < this._turn_out_card_count; i++) {
			outcarddata.addChangeCardsData(change_cards_data[i]);
			outcarddata.addPrCardsData(this._turn_out_card_data[i]);
			outcarddata.addPrChangeCardsData(this._turn_out_card_data[i]);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder hand_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				hand_cards.addItem(this.GRR._cards_data[i][j]);
			}
			outcarddata.addHandCardsData(hand_cards);
			outcarddata.addHandCardCount(this.GRR._card_count[i]);
		}
		outcarddata.setCardsCount(count);
		outcarddata.setOutCardPlayer(seat_index);
		outcarddata.setCardType(type);
		outcarddata.setPrOutCardType(this._turn_out_card_type);
		outcarddata.setCurPlayer(this._current_player);
		outcarddata.setLeftCardCount(GRR._left_card_count);
		outcarddata.setTimesNum(this._boom_times);
		outcarddata.setGameCell((int) this.game_cell);
		outcarddata.setIsFirstOut(first_out);
		outcarddata.setDisplayTime(15);
		if (this._turn_out_card_count == 0) {
			outcarddata.setIsCurrentFirstOut(1);
		} else {
			outcarddata.setIsCurrentFirstOut(0);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	public void auto_out_card(int seat_index, int cards[], int cahnge_cards[], int card_count, int out_type) {
		this._handler_out_card_operate.reset_status(seat_index, cards, cahnge_cards, card_count, out_type);
		this._handler_out_card_operate.exe(this);
	}

	public boolean animation_timer(int timer_id) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_TIMER_OUT_CARD: {
			if (!has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
				return false;
			}
			this.handler_request_trustee(this._current_player, true, 1);
			return true;
		}
		}

		return true;
	}

	public void AI_out_card_data() {
		if (this._turn_out_card_count == 0) {
			int out_card_data[] = new int[this.get_hand_card_count_max()];
			int out_card_change_data[] = new int[get_hand_card_count_max()];
			int hand_index[] = new int[GameConstants.GDY_MAX_INDEX];
			int out_card_cout = 0;
			int magic_temp_count = 0;
			int value = this._logic
					.GetCardLogicValue(GRR._cards_data[_current_player][GRR._card_count[_current_player] - 1]);
			_logic.switch_to_card_index(GRR._cards_data[_current_player], GRR._card_count[_current_player], hand_index);
			magic_temp_count = this._logic.get_magic_card_count(hand_index);

			if (hand_index[this._logic.get_card_index(value)] + magic_temp_count == GRR._card_count[_current_player]) {
				for (int i = GRR._card_count[_current_player] - 1; i >= 0; i--) {
					if (_logic.GetCardLogicValue(GRR._cards_data[_current_player][i]) == value) {
						out_card_data[out_card_cout] = GRR._cards_data[_current_player][i];
						out_card_change_data[out_card_cout++] = GRR._cards_data[_current_player][i];
					}
					if (_logic.get_card_index(GRR._cards_data[_current_player][i]) == 13
							|| _logic.get_card_index(GRR._cards_data[_current_player][i]) == 14) {
						out_card_data[out_card_cout] = GRR._cards_data[_current_player][i];
						out_card_change_data[out_card_cout++] = value;
					}
				}
			} else {
				for (int i = GRR._card_count[_current_player] - 1; i >= 0; i--) {
					if (_logic.GetCardLogicValue(GRR._cards_data[_current_player][i]) == value) {
						out_card_data[out_card_cout] = GRR._cards_data[_current_player][i];
						out_card_change_data[out_card_cout++] = GRR._cards_data[_current_player][i];
					}
				}
			}
			_auto_out_card_scheduled = GameSchedule.put(new GDYAutoOutCardRunnable(getRoom_id(), _current_player, this,
					out_card_data, out_card_change_data, out_card_cout, 2), 1, TimeUnit.SECONDS);
		} else {
			if (_current_player != GameConstants.INVALID_SEAT) {
				int out_card_data[] = new int[this.get_hand_card_count_max()];
				int out_card_change_data[] = new int[get_hand_card_count_max()];
				int out_card_cout = 0;
				out_card_cout = _logic.get_trustee_card(this._turn_out_card_data, _turn_out_card_count,
						this.GRR._cards_data[_current_player], GRR._card_count[_current_player], out_card_data,
						out_card_change_data);
				if (out_card_cout == 0) {
					_auto_out_card_scheduled = GameSchedule.put(new GDYAutoOutCardRunnable(getRoom_id(),
							_current_player, this, out_card_data, out_card_change_data, out_card_cout, 0), 1,
							TimeUnit.SECONDS);
				} else {
					_auto_out_card_scheduled = GameSchedule.put(new GDYAutoOutCardRunnable(getRoom_id(),
							_current_player, this, out_card_data, out_card_change_data, out_card_cout, 2), 1,
							TimeUnit.SECONDS);
				}

			}
		}
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		super.handler_request_trustee(get_seat_index, isTrustee, Trustee_type);

		if (this._current_player == get_seat_index && istrustee[get_seat_index]) {
			this.AI_out_card_data();
		}
		return true;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int seat_index, int send_count, boolean tail) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		int cards_data[] = new int[send_count];
		for (int i = 0; i < send_count; i++) {
			if (this.DEBUG_CARDS_MODE) {
				cards_data[i] = 0x29;
			} else {
				cards_data[i] = _repertory_card[_all_card_len - GRR._left_card_count];
			}

			GRR._left_card_count--;

			GRR._cards_data[seat_index][GRR._card_count[seat_index]] = cards_data[i];
			GRR._card_count[seat_index]++;
			this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index]);
		}
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			SendCardsGdy.Builder send_cards_gdy = SendCardsGdy.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder hand_cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GDEYE_SEND_CARD);// 201
			roomResponse.setTarget(seat_index);
			if (seat_index == play_index) {
				for (int i = 0; i < send_count; i++) {
					send_cards_gdy.addCardsData(cards_data[i]);
				}
				for (int j = 0; j < this.GRR._card_count[play_index]; j++) {
					send_cards_gdy.addHandCardsData(this.GRR._cards_data[play_index][j]);
				}
			}
			send_cards_gdy.setLeftCardCount(GRR._left_card_count);
			send_cards_gdy.setHandCardCount(GRR._card_count[seat_index]);
			send_cards_gdy.setCardCount(send_count);
			send_cards_gdy.setSendCardPlayer(seat_index);
			roomResponse.setCommResponse(PBUtil.toByteString(send_cards_gdy));
			this.send_response_to_player(play_index, roomResponse);
		}

		if (seat_index == _current_player) {
			if (this.istrustee[_current_player]) {
				AI_out_card_data();
			}
			if (has_rule(GameConstants.GAME_RULE_GDY_TRUSTEE)) {
				this.set_timer(ID_TIMER_OUT_CARD, _out_card_time);
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		SendCardsGdy.Builder send_cards_gdy = SendCardsGdy.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		Int32ArrayResponse.Builder hand_cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_SEND_CARD);// 201
		roomResponse.setTarget(seat_index);
		for (int i = 0; i < send_count; i++) {
			send_cards_gdy.addCardsData(cards_data[i]);
		}
		for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
			send_cards_gdy.addHandCardsData(this.GRR._cards_data[seat_index][j]);
		}
		send_cards_gdy.setLeftCardCount(GRR._left_card_count);
		send_cards_gdy.setHandCardCount(GRR._card_count[seat_index]);
		send_cards_gdy.setCardCount(send_count);
		send_cards_gdy.setSendCardPlayer(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(send_cards_gdy));
		GRR.add_room_response(roomResponse);

		if (!tail) {
			if (this.has_rule(GameConstants.GAME_RULE_GDY_ALL_BUPAI) && GRR._left_card_count > 0) {
				seat_index = (seat_index + 1) % getTablePlayerNumber();
				if (seat_index != _current_player) {
					exe_dispatch_card(seat_index, 1, false, 200);
				} else {
					int change_out_card[] = new int[this.GRR._card_count[_current_player]];
					_logic.make_magic_card(this.GRR._cards_data[_current_player], change_out_card,
							GRR._card_count[_current_player]);
					if (this._logic.GetCardType_GDY(change_out_card,
							this.GRR._card_count[_current_player]) != GameConstants.GDY_CT_ERROR) {
						_auto_out_card_scheduled = GameSchedule.put(new GDYAutoOutCardRunnable(getRoom_id(),
								_current_player, this, GRR._cards_data[_current_player], change_out_card,
								GRR._card_count[_current_player], 1), _out_card_time + 3, TimeUnit.SECONDS);
					}
				}
			}
			this.operate_player_cards();
		}

		return true;
	}

	// 发送扑克
	public boolean exe_dispatch_card(int seat_index, int send_count, boolean tail, int dely) {

		GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, send_count, tail), dely,
				TimeUnit.MILLISECONDS);

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
		if (type == MsgConstants.REQUST_GDY_OUT_CARD_LAIZI) {

			OutCardData_Request_GDY req = PBUtil.toObject(room_rq, OutCardData_Request_GDY.class);
			// 逻辑处理
			return handler_operate_out_card_mul(seat_index, req.getOutCardsList(), req.getChangeOutCardsList(),
					req.getOutCardCount(), req.getBOutCardType());
		}

		return true;
	}

	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, List<Integer> change_list,
			int card_count, int b_out_card) {
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			int change_out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
				change_out_cards[i] = change_list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, change_out_cards, card_count,
					b_out_card);
			this._handler.exe(this);
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
