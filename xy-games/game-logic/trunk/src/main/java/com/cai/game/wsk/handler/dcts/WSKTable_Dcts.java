package com.cai.game.wsk.handler.dcts;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.game.RoomUtil;
import com.cai.game.wsk.AbstractWSKTable;
import com.cai.game.wsk.Player_EX;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKGameLogic_DCTS;
import com.cai.game.wsk.WSKMsgConstants;
import com.cai.game.wsk.WSKType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dcts.dctsRsp.GameStart_dcts;
import protobuf.clazz.dcts.dctsRsp.OutCardDataWsk_dcts;
import protobuf.clazz.dcts.dctsRsp.PukeGameEndWsk_dcts;
import protobuf.clazz.dcts.dctsRsp.RefreshScore_dcts;
import protobuf.clazz.dcts.dctsRsp.SendCard_dcts;
import protobuf.clazz.dcts.dctsRsp.TableResponse_dcts;
import protobuf.clazz.dcts.dctsRsp.effect_type_dcts;
import protobuf.clazz.gfWsk.gfWskRsp.Opreate_RequestWsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.RefreshCardData;
import protobuf.clazz.zzshA.zzshARsp.Refresh_Pai_zzshA;

public class WSKTable_Dcts extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	public int _get_score[];
	public int _is_call_banker[];

	public int _turn_have_score; // 当局得分
	public boolean _is_yi_da_san;
	public int _score_type;
	public int _out_boom_card_data[][][];
	public int _out_boom_card_count[][];

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间
	public int sort[];// 排序标志
	public WSKGameLogic_DCTS _logic;

	public WSKTable_Dcts() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_DCTS();
		_get_score = new int[getTablePlayerNumber()];
		_xi_qian_total_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_xi_qian_times = new int[getTablePlayerNumber()];
		_friend_seat = new int[getTablePlayerNumber()];
		_is_call_banker = new int[getTablePlayerNumber()];
		_tou_xiang_times = new int[getTablePlayerNumber()];
		sort = new int[getTablePlayerNumber()];
		_max_end_score = new int[getTablePlayerNumber()];
		_out_boom_card_data = new int[getTablePlayerNumber()][this.get_hand_card_count_max()][this
				.get_hand_card_count_max()];
		_out_boom_card_count = new int[getTablePlayerNumber()][this.get_hand_card_count_max()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = GameConstants.WSK_ST_ORDER;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_Dcts();
		_player_info = new Player_EX[getTablePlayerNumber()];
		this._seat_team = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_max_end_score[i] = 0;
			_seat_team[i] = 0;
			_player_info[i] = new Player_EX();
		}
	}

	@Override
	public void Refresh_user_get_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshScore_dcts.Builder refresh_user_getscore = RefreshScore_dcts.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_USER_GET_SCORE);
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			refresh_user_getscore.addUserGetScore(_get_score[i]);
			refresh_user_getscore.addXianQianScore(this._xi_qian_score[i]);
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

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// if(this.has_rule(GameConstants.GAME_RULE_DMZ_MODUI)){
		// shuffle_players();
		// this.operate_player_data();
		// }

		this._turn_out_card_count = 0;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_xi_qian_score[i] = 0;
			_xi_qian_times[i] = 0;
			_seat_team[i] = 0;
			Arrays.fill(_out_boom_card_count[i], 0);
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
		}
		_score_type = GameConstants.WSK_ST_ORDER;
		_is_yi_da_san = false;
		_turn_have_score = 0;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		_repertory_card = new int[WSKConstants.CARD_COUNT_DCTS];
		shuffle(_repertory_card, WSKConstants.CARD_DATA_DEGAULE_WSK);

		progress_banker_select();
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

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

		// 庄家选择
		// this.progress_banker_select();

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
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	public int get_hand_card_count_max() {
		return 27;
	}

	@Override
	protected boolean on_game_start() {

		int team_one_num = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._seat_team[i] == 1) {
				team_one_num++;
			}
		}
		if (team_one_num == 1) {
			this._is_yi_da_san = true;
		}
		this._current_player = _cur_banker;
		_game_status = GameConstants.GS_MJ_PLAY;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_dcts.Builder gamestart = GameStart_dcts.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);

		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		this.set_handler(this._handler_call_banker);

		Refresh_user_get_score(GameConstants.INVALID_SEAT);
		Send_card();
		this.schedule(ID_TIMER_SEND_CARD, SheduleArgs.newArgs(), 1500);
		this._handler = this._handler_out_card_operate;

		return true;
	}

	// 发送特效
	public void send_effect_type(int seat_index, int type, int data[], int is_animation, int to_player) {
		//
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_EFFECT);
		effect_type_dcts.Builder effect = effect_type_dcts.newBuilder();
		effect.setSeatIndex(seat_index);
		effect.setType(type);
		effect.setIsAnimation(is_animation);
		if (data != null) {
			for (int i = 0; i < data.length; i++) {
				effect.addData(data[i]);
			}
		}

		roomResponse.setCommResponse(PBUtil.toByteString(effect));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	public void Send_card() {
		// 初始化游戏变量
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			SendCard_dcts.Builder send_card = SendCard_dcts.newBuilder();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == play_index) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}
				send_card.addCardsData(cards_card);
				send_card.addCardCount(GRR._card_count[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);

		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		SendCard_dcts.Builder send_card = SendCard_dcts.newBuilder();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(this.GRR._cards_data[i][j]);
			}
			send_card.addCardsData(cards_card);
			send_card.addCardCount(GRR._card_count[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));
		GRR.add_room_response(roomResponse);

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

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD: {
			out_card_begin();
			return;
		}
		}
	}

	public void out_card_begin() {
		_game_status = GameConstants.GS_SXTH_PLAY;
		GRR._banker_player = _cur_banker;
		this._current_player = GRR._banker_player;
		_jiao_pai_card = this._logic.get_liang_pai(this.GRR._cards_data[this._current_player],
				this.GRR._card_count[this._current_player]);
		int data[] = new int[1];
		data[0] = _jiao_pai_card;
		this.send_effect_type(_cur_banker, 1, data, 1, GameConstants.INVALID_SEAT);

		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, WSKConstants.ZZSHA_CT_ERROR,
				GameConstants.INVALID_SEAT, false);

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
		roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_dcts.Builder game_end_wsk = PukeGameEndWsk_dcts.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());
		game_end_wsk.setBankerPlayer(GameConstants.INVALID_SEAT);

		int end_score[] = new int[this.getTablePlayerNumber()];
		int dang_ju_score[] = new int[this.getTablePlayerNumber()];
		int jia_fa_socre[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		Arrays.fill(jia_fa_socre, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL && GRR != null) {
			cal_score_wsk(end_score, dang_ju_score, seat_index, jia_fa_socre);
		}

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder fei_wang_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_wsk.addCardsData(cards_card);

			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_xi_qian_total_score[i] += _xi_qian_score[i];
			if (end_score[i] > _max_end_score[i]) {
				_max_end_score[i] = end_score[i];
			}
			if (end_score[i] > 0) {
				this._player_info[i]._win_num++;
			} else if (end_score[i] < 0) {
				this._player_info[i]._lose_num++;
			}
			game_end.addGameScore(end_score[i]);
			game_end_wsk.addEndScore(end_score[i]);
			game_end_wsk.addRewardScore(this._xi_qian_score[i]);
			boolean is_out_finish = false;
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				if (_chuwan_shunxu[j] == i) {
					game_end_wsk.addWinOrder(j);
					is_out_finish = true;
					break;
				}

			}
			if (!is_out_finish) {
				game_end_wsk.addWinOrder(GameConstants.INVALID_SEAT);
			}
		}

		this.load_player_info_data_game_end(game_end_wsk);
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_wsk.addLoseNum(_player_info[i]._lose_num);
					game_end_wsk.addWinNum(this._player_info[i]._win_num);
					game_end_wsk.addRewardTimes(0);
					game_end_wsk.addEndScoreMax(0);
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
				game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
				game_end_wsk.addLoseNum(_player_info[i]._lose_num);
				game_end_wsk.addWinNum(this._player_info[i]._win_num);
				game_end_wsk.addRewardTimes(0);
				game_end_wsk.addEndScoreMax(0);
			}

			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		game_end_wsk.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_wsk));
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_chuwan_shunxu[i] = GameConstants.INVALID_SEAT;
		}
		// 错误断言
		return false;
	}

	/**
	 *
	 * @param end_score
	 * @param dang_ju_fen
	 * @param win_seat_index
	 * @param jia_fa_socre
	 */
	public void cal_score_wsk(int end_score[], int dang_ju_fen[], int win_seat_index, int jia_fa_socre[]) {
		if (this._is_yi_da_san) {
			int banker = GameConstants.INVALID_SEAT;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._seat_team[i] == 1) {
					banker = i;
					this._player_info[i]._banker_timer++;
					break;
				}
			}
			if (this._chuwan_shunxu[0] == banker) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == banker) {
						this._player_info[i]._guan_men_times++;
						continue;
					}
					end_score[i] -= 2;
					end_score[banker] -= end_score[i];
				}
			} else if (this._chuwan_shunxu[3] == banker) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == banker) {
						continue;
					}
					this._player_info[i]._guan_men_times++;
					end_score[i] += 2;
					end_score[banker] -= end_score[i];
				}
			} else {
				int banker_get_score = this._get_score[banker];
				int xian_get_score = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == banker) {
						continue;
					}
					xian_get_score += this._get_score[i];
				}
				if (banker_get_score > xian_get_score) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == banker) {
							continue;
						}
						end_score[i] -= 1;
						end_score[banker] -= end_score[i];
					}
				} else if (banker_get_score < xian_get_score) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == banker) {
							continue;
						}
						end_score[i] += 1;
						end_score[banker] -= end_score[i];
					}
				}

			}
		} else {
			if (this._seat_team[this._chuwan_shunxu[0]] == this._seat_team[this._chuwan_shunxu[1]]) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (_seat_team[i] == _seat_team[this._chuwan_shunxu[0]]) {
						this._player_info[i]._guan_men_times++;
						end_score[i] += 2;
					} else {
						end_score[i] -= 2;
					}
				}
			} else if (this._seat_team[this._chuwan_shunxu[0]] == this._seat_team[this._chuwan_shunxu[2]]) {
				jia_fa_socre[this._chuwan_shunxu[0]] += 10;
				jia_fa_socre[this._chuwan_shunxu[2]] += 10;
				jia_fa_socre[this._chuwan_shunxu[1]] -= 10;
				jia_fa_socre[this._chuwan_shunxu[3]] -= 10;
				int shang_you_score = this._get_score[this._chuwan_shunxu[0]] + this._get_score[this._chuwan_shunxu[2]]
						+ jia_fa_socre[this._chuwan_shunxu[0]] + jia_fa_socre[this._chuwan_shunxu[2]];
				int xia_you_score = this._get_score[this._chuwan_shunxu[1]] + this._get_score[this._chuwan_shunxu[3]]
						+ jia_fa_socre[this._chuwan_shunxu[1]] + jia_fa_socre[this._chuwan_shunxu[3]];
				if (shang_you_score > xia_you_score) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_seat_team[i] == _seat_team[this._chuwan_shunxu[0]]) {
							end_score[i] += 1;
						} else {
							end_score[i] -= 1;
						}
					}
				} else if (shang_you_score < xia_you_score) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_seat_team[i] == _seat_team[this._chuwan_shunxu[0]]) {
							end_score[i] -= 2;
						} else {
							end_score[i] += 2;
						}
					}
				}
			} else {
				int shang_you_score = this._get_score[this._chuwan_shunxu[0]] + this._get_score[this._chuwan_shunxu[3]]
						+ jia_fa_socre[this._chuwan_shunxu[0]] + jia_fa_socre[this._chuwan_shunxu[3]];
				int xia_you_score = this._get_score[this._chuwan_shunxu[1]] + this._get_score[this._chuwan_shunxu[2]]
						+ jia_fa_socre[this._chuwan_shunxu[1]] + jia_fa_socre[this._chuwan_shunxu[2]];
				if (shang_you_score > xia_you_score) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_seat_team[i] == _seat_team[this._chuwan_shunxu[0]]) {
							end_score[i] += 1;
						} else {
							end_score[i] -= 1;
						}
					}
				} else if (shang_you_score < xia_you_score) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_seat_team[i] == _seat_team[this._chuwan_shunxu[0]]) {
							end_score[i] -= 1;
						} else {
							end_score[i] += 1;
						}
					}
				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += end_score[i];
		}
	}

	public void load_player_info_data_game_start(GameStart_dcts.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_dcts.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndWsk_dcts.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player,
			boolean is_deal) {
		// if (seat_index == GameConstants.INVALID_SEAT) {
		// return false;
		// }
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_dcts.Builder outcarddata = OutCardDataWsk_dcts.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_OUT_CARD);// 201
				roomResponse.setTarget(seat_index);

				for (int j = 0; j < count; j++) {
					outcarddata.addCardsData(cards_data[j]);
				}
				// 上一出牌数据
				outcarddata.setPrCardsCount(this._turn_out_card_count);
				if (type == WSKConstants.ZZSHA_CT_PLANE || type == WSKConstants.ZZSHA_CT_THREE_TAKE_TWO) {
					for (int i = 0; i < this._turn_out_card_count; i++) {
						outcarddata.addPrCardsData(this._turn_out_card_data[i]);
					}
					if (this._turn_three_link_num * 5 != _turn_out_card_count) {
						for (int i = 0; i < _turn_three_link_num * 5 - _turn_out_card_count; i++) {
							outcarddata.addPrCardsData(GameConstants.INVALID_CARD);
						}
					}
					outcarddata.setPrCardsCount(_turn_three_link_num * 5);
				} else {
					for (int i = 0; i < this._turn_out_card_count; i++) {
						outcarddata.addPrCardsData(this._turn_out_card_data[i]);
					}
					outcarddata.setPrCardsCount(_turn_out_card_count);
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

				// 出票提示
				if (index == this._current_player) {
					int tip_out_card[][] = new int[GRR._card_count[index] * 4][GRR._card_count[index]];
					int tip_out_count[] = new int[GRR._card_count[index] * 4];
					int tip_type_count = 0;
					tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index],
							this._turn_out_card_data, this._turn_out_card_count, this._turn_three_link_num,
							tip_out_card, tip_out_count, tip_type_count);
					if (tip_type_count > 0) {
						for (int i = 0; i < 1; i++) {
							for (int j = 0; j < tip_out_count[i]; j++) {
								outcarddata.addUserCanOutData(tip_out_card[i][j]);
							}
							outcarddata.setUserCanOutCount(tip_out_count[i]);
						}
					}
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (i == index) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					}

					outcarddata.addHandCardsData(cards_card);
					outcarddata.addHandCardCount(this.GRR._card_count[i]);
					boolean is_out_finish = false;
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						if (_chuwan_shunxu[j] == i) {
							outcarddata.addWinOrder(j);
							is_out_finish = true;
							break;
						}

					}
					if (!is_out_finish) {
						outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
					}
				}
				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_dcts.Builder outcarddata = OutCardDataWsk_dcts.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_OUT_CARD);// 201
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

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				boolean is_out_finish = false;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (_chuwan_shunxu[j] == i) {
						outcarddata.addWinOrder(j);
						is_out_finish = true;
						break;
					}

				}
				if (!is_out_finish) {
					outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_dcts.Builder outcarddata = OutCardDataWsk_dcts.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_DCTS_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			if (type == WSKConstants.ZZSHA_CT_PLANE || type == WSKConstants.ZZSHA_CT_THREE_TAKE_TWO) {
				for (int i = 0; i < this._turn_out_card_count; i++) {
					outcarddata.addPrCardsData(this._turn_out_card_data[i]);
				}
				if (this._turn_three_link_num * 5 != _turn_out_card_count) {
					for (int i = 0; i < _turn_three_link_num * 5 - _turn_out_card_count; i++) {
						outcarddata.addPrCardsData(GameConstants.INVALID_CARD);
					}
				}
				outcarddata.setPrCardsCount(_turn_three_link_num * 5);
			} else {
				for (int i = 0; i < this._turn_out_card_count; i++) {
					outcarddata.addPrCardsData(this._turn_out_card_data[i]);
				}
				outcarddata.setPrCardsCount(_turn_out_card_count);
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

			// 出票提示
			if (to_player == this._current_player) {
				int tip_out_card[][] = new int[GRR._card_count[to_player] * 2][GRR._card_count[to_player]];
				int tip_out_count[] = new int[GRR._card_count[to_player] * 2];
				int tip_type_count = 0;
				tip_type_count = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player],
						this._turn_out_card_data, this._turn_out_card_count, this._turn_three_link_num, tip_out_card,
						tip_out_count, tip_type_count);
				if (tip_type_count > 0) {
					for (int i = 0; i < 1; i++) {
						for (int j = 0; j < tip_out_count[i]; j++) {
							outcarddata.addUserCanOutData(tip_out_card[i][j]);
						}
						outcarddata.setUserCanOutCount(tip_out_count[i]);
					}
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == to_player) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}

				outcarddata.addHandCardsData(cards_card);
				outcarddata.addHandCardCount(this.GRR._card_count[i]);
				boolean is_out_finish = false;
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (_chuwan_shunxu[j] == i) {
						outcarddata.addWinOrder(j);
						is_out_finish = true;
						break;
					}

				}
				if (!is_out_finish) {
					outcarddata.addWinOrder(GameConstants.INVALID_SEAT);
				}
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);

		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_GF_WSK_OPERATE) {
			Opreate_RequestWsk_GF req = PBUtil.toObject(room_rq, Opreate_RequestWsk_GF.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getCardData(), req.getSortCardList());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int card_data, List<Integer> list) {
		switch (opreate_type) {

		}
		return true;
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_REFRESH_CARD);
		// 发送数据
		RefreshCardData.Builder refresh_card = RefreshCardData.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (to_player == i) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
			}
			refresh_card.addHandCardsData(cards_card);
			refresh_card.addHandCardCount(GRR._card_count[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);
	}

	@Override
	public int getTablePlayerNumber() {
		return 4;
	}

	@Override
	protected void set_result_describe() {
		// TODO Auto-generated method stub

	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card,
			String desc) {
		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, card_count, b_out_card);
			this._handler.exe(this);
		}
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 是否春天
	 *
	 * @return
	 */
	public boolean checkChunTian(int seatIndex) {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (player == seatIndex) {
				continue;
			}
			// 27张牌，一张没出
			if (GRR._card_count[player] != get_hand_card_count_max()) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void test_cards() {
		// int cards[] = new int[] { 0x0d, 0x1d, 0x2d, 0x3D, 0x0D, 0x1D, 0x2D,
		// 0x4E, 0x4F, 0x06, 0x16, 0x26, 0x36, 0x07, 0x07, 0x07 };
		// int cards[] = new int[] { 0x02, 0x12, 0x22, 0x32, 0x02, 0x12, 0x22,
		// 0x01, 0x4F, 0x07, 0x17, 0x27, 0x37, 0x07, 0x17, 0x27, 0x01, 0x09,
		// 0x0a, 0x1a, 0x2a, 0x3a, 0x0a, 0x1a, 0x2a, 0x01, 0x19, };

		int cards[] = new int[] { 45, 61, 1, 33, 2, 18, 3, 19, 4, 20, 5, 21, 6, 22, 7, 23, 8, 24, 9, 25, 10, 26, 11, 27,
				12, 28, 35, 51, 36, 52, 37, 53, 38, 54, 39, 55, 40, 56, 41, 57, 42, 58, 43, 59, 44, 60, 13, 29, 17, 49,
				34, 50 };
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_seat_team[i] = 0;
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[index++];
				if (GRR._cards_data[i][j] == 0x33) {
					_cur_banker = i;
				}
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > this.get_hand_card_count_max()) {
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
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
		}

	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
			}
			_seat_team[i] = 0;
		}
		this._repertory_card = realyCards;

		int send_count;
		int have_send_count = 0;
		_cur_banker = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = realyCards[k++];
					if (GRR._cards_data[i][j] == 0x33) {
						_cur_banker = i;
					}
				}
			}
			if (i == this.getTablePlayerNumber())
				break;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_index[i][j] = 0;
			}

		}
		this._repertory_card = cards;
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[k++];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	/**
	 * 刷新队友牌
	 *
	 * @param to_player
	 */
	public void Refresh_Dui_You_Card(int seat_index) {
		if (GRR._card_count[seat_index] != 0) {
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_ZZSHA_REFRESH_DUIYOU_CARD);
		// 发送数据
		Refresh_Pai_zzshA.Builder refresh_card = Refresh_Pai_zzshA.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (this._seat_team[seat_index] == i) {
				refresh_card.setSeatIndex(i);
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					refresh_card.addCardsData(this.GRR._cards_data[i][j]);
				}
				refresh_card.setCardCount(GRR._card_count[i]);

				roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
				// 自己才有牌数据h
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
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

		if (is_sys())
			return true;
		// if (GameConstants.GS_MJ_FREE != _game_status) {
		// return handler_player_ready(seat_index, false);
		// }
		return true;
	}
}
