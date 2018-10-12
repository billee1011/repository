package com.cai.game.wsk.handler.pingxiang.three;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ERoomStatus;
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
import com.cai.game.wsk.WSKGameLogic_PXGT_TWO;
import com.cai.game.wsk.WSKMsgConstants;
import com.cai.game.wsk.WSKType;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pxgt.pxgtkRsp.GameStart_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.LiangPai_Result_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.Opreate_RequestWsk_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.OutCardDataWsk_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.PukeGameEndWsk_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.RefreshCardData_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.RefreshScore_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.Refresh_Pai_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.Reward_Score_Effect_pxgt;
import protobuf.clazz.pxgt.pxgtkRsp.TableResponse_pxgt;

public class WSKTable_PXGT_THREE extends AbstractWSKTable {

	private static final long serialVersionUID = -2419887548488809773L;

	protected static final int ID_TIMER_SEND_CARD = 1;// 发牌时间
	public int _get_score[];
	public int _is_call_banker[];
	public int _turn_have_score; // 当局得分
	public boolean _is_yi_da_san;
	public int _score_type;
	public int _jiao_pai_card;
	public int _jiao_pai_seat;
	private boolean is_touxiang;
	public int sort[];// 排序标志
	public int _boom_reward_cell;

	public int[] player_sort_card;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_ORDER = 0;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_COUNT = 1;
	protected static final int GAME_OPREATE_TYPE_SORT_BY_510K = 2;

	public WSKGameLogic_PXGT_TWO _logic;

	public WSKTable_PXGT_THREE() {
		super(WSKType.GAME_TYPE_WSK_GF);
	}

	@Override
	protected void onInitTable() {

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_logic = new WSKGameLogic_PXGT_TWO();
		_get_score = new int[getTablePlayerNumber()];
		_xi_qian_score = new int[getTablePlayerNumber()];
		_friend_seat = new int[getTablePlayerNumber()];
		sort = new int[getTablePlayerNumber()];
		_out_card_times = new int[getTablePlayerNumber()];
		_seat_team = new int[getTablePlayerNumber()];
		_player_info = new Player_EX[getTablePlayerNumber()];
		_cur_out_car_type = new int[getTablePlayerNumber()];
		_init_account_id = new long[getTablePlayerNumber()];
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des(), this.getTablePlayerNumber());
		_logic.ruleMap = this.ruleMap;
		_score_type = GameConstants.WSK_ST_ORDER;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_handler_out_card_operate = new WSKHandlerOutCardOperate_PXGT_THREE();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_friend_seat[i] = -1;
			_seat_team[i] = -1;
			_player_info[i] = new Player_EX();
		}
		if (this.has_rule(WSKConstants.GAME_RULE_SWK_AWARD_1_PX)) {
			_boom_reward_cell = 1;
		} else if (this.has_rule(WSKConstants.GAME_RULE_SWK_AWARD_2_PX)) {
			_boom_reward_cell = 2;
		} else if (this.has_rule(WSKConstants.GAME_RULE_SWK_AWARD_4_PX)) {
			_boom_reward_cell = 4;
		} else if (this.has_rule(WSKConstants.GAME_RULE_SWK_AWARD_3_PX)) {
			_boom_reward_cell = 3;
		} else {
			_boom_reward_cell = 5;
		}
	}

	public void Send_Reward_effect(int cur_score, int target_seat) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		Reward_Score_Effect_pxgt.Builder reware_effect = Reward_Score_Effect_pxgt.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_REWARD_SCORE_EFFECT);
		reware_effect.setCurScore(cur_score);
		reware_effect.setTargetSeat(target_seat);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			reware_effect.addXianQianScore(this._xi_qian_score[i]);
		}
		// refresh_user_getscore.setTableScore(value);
		roomResponse.setCommResponse(PBUtil.toByteString(reware_effect));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
	}

	public void Refresh_user_get_score(int cur_get_score, int target_seat, int type, int to_player) {

		if (to_player == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RefreshScore_pxgt.Builder refresh_user_getscore = RefreshScore_pxgt.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_USER_GET_SCORE);
			int our_score = 0;
			int other_score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				refresh_user_getscore.addXianQianScore(this._xi_qian_score[i]);
				refresh_user_getscore.addUserGetScore(_get_score[i]);
				refresh_user_getscore.addTeam(this._seat_team[i]);
			}
			refresh_user_getscore.setCurGetScore(cur_get_score);
			refresh_user_getscore.setType(type);
			refresh_user_getscore.setTableScore(_turn_have_score);
			refresh_user_getscore.setTargetSeat(target_seat);

			// refresh_user_getscore.setTableScore(value);
			roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RefreshScore_pxgt.Builder refresh_user_getscore = RefreshScore_pxgt.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_USER_GET_SCORE);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				refresh_user_getscore.addUserGetScore(_get_score[i]);
				refresh_user_getscore.addXianQianScore(this._xi_qian_score[i]);
				refresh_user_getscore.addTeam(this._seat_team[i]);
			}

			refresh_user_getscore.setCurGetScore(cur_get_score);
			refresh_user_getscore.setType(type);
			refresh_user_getscore.setTableScore(_turn_have_score);

			// refresh_user_getscore.setTableScore(value);
			roomResponse.setCommResponse(PBUtil.toByteString(refresh_user_getscore));
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
		if (this._cur_round == 1) {
			_init_players = new Player[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_init_account_id[i] = this.get_players()[i].getAccount_id();
				_init_players[i] = this.get_players()[i];
			}
		}
		// if(this.has_rule(GameConstants.GAME_RULE_DMZ_MODUI)){
		// shuffle_players();
		// this.operate_player_data();
		// }

		this._turn_out_card_count = 0;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cur_out_card_count[i] = 0;
			_cur_out_car_type[i] = WSKConstants.PXGT_CT_ERROR;
			_friend_seat[i] = (i + 2) % this.getTablePlayerNumber();
			if (i % 2 == 0) {
				_seat_team[i] = 0;
			} else {
				_seat_team[i] = 1;
			}
			_out_card_times[i] = 0;
			Arrays.fill(_cur_out_card_data[i], GameConstants.INVALID_CARD);
			sort[i] = GameConstants.WSK_ST_ORDER;
		}
		_score_type = GameConstants.WSK_ST_ORDER;
		_game_status = GameConstants.GS_GFWSK_CALLBANKER;
		_turn_have_score = 0;
		_jiao_pai_card = GameConstants.INVALID_CARD;
		_jiao_pai_seat = GameConstants.INVALID_SEAT;
		Arrays.fill(_chuwan_shunxu, GameConstants.INVALID_SEAT);
		_repertory_card = new int[WSKConstants.CARD_COUNT_PXGT_TWO];

		_liang_card_value = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % WSKConstants.CARD_COUNT_PXGT_TWO;
		shuffle(_repertory_card, WSKConstants.CARD_DATA_PXGT_TWO);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		on_game_start();
		return true;
	}

	private void shuffle_players_data() {
		List<Player> pl = new ArrayList<Player>();
		int team_seat[] = new int[2];
		team_seat[0] = GameConstants.INVALID_SEAT;
		team_seat[1] = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (this.GRR._cards_data[i][j] == _repertory_card[_liang_card_value]) {
					if (team_seat[0] == GameConstants.INVALID_SEAT) {
						team_seat[0] = i;
					} else {
						team_seat[1] = i;
					}
				}
			}
		}
		if (team_seat[0] == team_seat[1] || (team_seat[0] + 2) % this.getTablePlayerNumber() == team_seat[1]) {
			return;
		}

		if (team_seat[0] != team_seat[1] && team_seat[0] != GameConstants.INVALID_SEAT
				&& team_seat[1] != GameConstants.INVALID_SEAT) {
			if (_cur_banker == team_seat[1]) {
				_cur_banker = (team_seat[0] + 2) % this.getTablePlayerNumber();
			} else if (_cur_banker == (team_seat[0] + 2) % this.getTablePlayerNumber()) {
				_cur_banker = team_seat[1];
			}
			this._current_player = _cur_banker;
			// 换用户信息
			Player temp_player = new Player();
			temp_player = get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()] = get_players()[team_seat[1]];
			get_players()[team_seat[1]] = temp_player;
			get_players()[(team_seat[0] + 2) % this.getTablePlayerNumber()]
					.set_seat_index((team_seat[0] + 2) % this.getTablePlayerNumber());
			get_players()[team_seat[1]].set_seat_index(team_seat[1]);

			Player_EX temp_info = new Player_EX();
			temp_info = _player_info[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			_player_info[(team_seat[0] + 2) % this.getTablePlayerNumber()] = _player_info[team_seat[1]];
			_player_info[team_seat[1]] = temp_info;

			float temp_score = _player_result.game_score[(team_seat[0] + 2) % this.getTablePlayerNumber()];
			_player_result.game_score[(team_seat[0] + 2)
					% this.getTablePlayerNumber()] = _player_result.game_score[team_seat[1]];
			_player_result.game_score[team_seat[1]] = temp_score;

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

		this.RefreshCard(GameConstants.INVALID_SEAT);
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
				if (_liang_card_value == i * this.get_hand_card_count_max() + j) {
					this._cur_banker = i;
				}
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_max() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i], _score_type);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	public int get_hand_card_count_max() {
		return 26;
	}

	@Override
	protected boolean on_game_start() {
		is_touxiang = false;
		this._current_player = _cur_banker;

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			GameStart_pxgt.Builder gamestart = GameStart_pxgt.newBuilder();
			gamestart.setRoomInfo(this.getRoomInfo());
			gamestart.setCurBanker(GameConstants.INVALID_SEAT);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 分析扑克
				gamestart.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (play_index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

				gamestart.addCardsData(cards_card);
			}
			gamestart.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(this.getRoomInfo());
		// 发送数据
		GameStart_pxgt.Builder gamestart = GameStart_pxgt.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		if (this._cur_round == 1) {
			this.load_player_info_data_game_start(gamestart);
		}
		gamestart.setCurBanker(GameConstants.INVALID_SEAT);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gamestart.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}

			gamestart.addCardsData(cards_card);
		}

		gamestart.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));

		GRR.add_room_response(roomResponse);

		schedule(ID_TIMER_SEND_CARD, SheduleArgs.newArgs(), 3000);
		this.set_handler(this._handler_out_card_operate);
		this.send_liang_pai(_repertory_card[_liang_card_value]);
		Refresh_user_get_score(0, 0, 0, GameConstants.INVALID_SEAT);

		return true;
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
		roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndWsk_pxgt.Builder game_end_wsk = PukeGameEndWsk_pxgt.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		game_end_wsk.setRoomInfo(getRoomInfo());

		int end_score[] = new int[this.getTablePlayerNumber()];
		int dang_ju_score[] = new int[this.getTablePlayerNumber()];
		int reward_score[] = new int[this.getTablePlayerNumber()];
		int score_type = 1;
		Arrays.fill(end_score, 0);
		Arrays.fill(dang_ju_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			score_type = cal_score_wsk(end_score, dang_ju_score, reward_score);
		}

		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end_wsk.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				Int32ArrayResponse.Builder cards_card1 = Int32ArrayResponse.newBuilder();
				game_end_wsk.addCardsData(cards_card1);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (end_score[i] > 0) {
				this._player_info[i]._win_num++;
			} else if (end_score[i] < 0) {
				this._player_info[i]._lose_num++;
			}
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (this.get_players()[j].getAccount_id() == _init_account_id[i]) {
					game_end.addGameScore(end_score[j]);
					break;
				}
			}
			game_end_wsk.addDiFen(dang_ju_score[i]);
			game_end_wsk.addEndScore(end_score[i]);
			game_end_wsk.addWinOrder(_chuwan_shunxu[i]);
			game_end_wsk.addRewardScore(reward_score[i]);
			// if (reason != GameConstants.Game_End_RELEASE_RESULT) {
			// }else{
			// game_end_wsk_gf.addCardsData(UserCardData.newBuilder());
			// }
		}

		this.load_player_info_data_game_end(game_end_wsk);
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					game_end_wsk.addAllEndScore((int) this._player_result.game_score[i]);
					game_end_wsk.addLoseNum(_player_info[i]._lose_num);
					game_end_wsk.addWinNum(_player_info[i]._win_num);
					game_end_wsk.addAllFailNum(_player_info[i]._lose_num);
					game_end_wsk.addAllWinNum(_player_info[i]._win_num);
					game_end_wsk.addCloseDoorNum(_player_info[i]._guan_men_times);
					game_end_wsk.addBoomNum(_player_info[i]._boom_num);
				}
				Restore_Gamescore();
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
				game_end_wsk.addWinNum(_player_info[i]._win_num);
				game_end_wsk.addAllFailNum(_player_info[i]._lose_num);
				game_end_wsk.addAllWinNum(_player_info[i]._win_num);
				game_end_wsk.addCloseDoorNum(_player_info[i]._guan_men_times);
				game_end_wsk.addBoomNum(_player_info[i]._boom_num);
			}
			Restore_Gamescore();
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		game_end_wsk.setScoreType(score_type);
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
	public int cal_score_wsk(int end_score[], int dang_ju_fen[], int reward_score[]) {
		int score_type = 1;
		int shang_you_team = _seat_team[_chuwan_shunxu[0]];
		int shang_you_score = 0;
		int xia_you_score = 0;
		if (_seat_team[this._chuwan_shunxu[0]] == _seat_team[this._chuwan_shunxu[1]]) {
			score_type = 2;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				if (i == j) {
					continue;
				}
				reward_score[i] += this._xi_qian_score[i];
				reward_score[j] -= this._xi_qian_score[i];
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_seat_team[i] == shang_you_team) {
				shang_you_score += this._get_score[i];
			} else {
				xia_you_score += this._get_score[i];
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_seat_team[i] == shang_you_team) {
				dang_ju_fen[i] = (shang_you_score - xia_you_score) / 10;
			} else {
				dang_ju_fen[i] = (xia_you_score - shang_you_score) / 10;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			end_score[i] = dang_ju_fen[i] + reward_score[i];
			this._player_result.game_score[i] += end_score[i];
		}
		return score_type;
	}

	public void load_player_info_data_game_start(GameStart_pxgt.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_pxgt.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndWsk_pxgt.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			room_player.setScore(get_players()[i].getGame_score());
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player,
			boolean is_deal) {
		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardDataWsk_pxgt.Builder outcarddata = OutCardDataWsk_pxgt.newBuilder();
				// Int32ArrayResponse.Builder cards =
				// Int32ArrayResponse.newBuilder();
				roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_OUT_CARD);// 201
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

				// 出票提示
				if (index == this._current_player) {
					int tip_out_card[][] = new int[GRR._card_count[index] * 2][GRR._card_count[index]];
					int tip_out_count[] = new int[GRR._card_count[index] * 2];
					int tip_type_count = 0;
					tip_type_count = this._logic.search_out_card(GRR._cards_data[index], GRR._card_count[index],
							this._turn_out_card_data, this._turn_out_card_count, this._turn_three_link_num,
							tip_out_card, tip_out_count, tip_type_count);
					for (int i = 0; i < tip_type_count; i++) {
						Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
						for (int j = 0; j < tip_out_count[i]; j++) {
							cards_card.addItem(tip_out_card[i][j]);
						}
						outcarddata.addUserCanOutData(cards_card);
						outcarddata.addUserCanOutCount(tip_out_count[i]);
					}
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
				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

				this.send_response_to_player(index, roomResponse);

			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_pxgt.Builder outcarddata = OutCardDataWsk_pxgt.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_OUT_CARD);// 201
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
				outcarddata.addWinOrder(this._chuwan_shunxu[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataWsk_pxgt.Builder outcarddata = OutCardDataWsk_pxgt.newBuilder();
			// Int32ArrayResponse.Builder cards =
			// Int32ArrayResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_OUT_CARD);// 201
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

			// 出票提示
			if (to_player == this._current_player) {
				int tip_out_card[][] = new int[GRR._card_count[to_player] * 2][GRR._card_count[to_player]];
				int tip_out_count[] = new int[GRR._card_count[to_player] * 2];
				int tip_type_count = 0;
				tip_type_count = this._logic.search_out_card(GRR._cards_data[to_player], GRR._card_count[to_player],
						this._turn_out_card_data, this._turn_out_card_count, this._turn_three_link_num, tip_out_card,
						tip_out_count, tip_type_count);
				for (int i = 0; i < tip_type_count; i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < tip_out_count[i]; j++) {
						cards_card.addItem(tip_out_card[i][j]);
					}
					outcarddata.addUserCanOutData(cards_card);
					outcarddata.addUserCanOutCount(tip_out_count[i]);
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == to_player
						|| (to_player == (i + 2) % getTablePlayerNumber() && GRR._card_count[to_player] == 0)) {
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
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(to_player, roomResponse);

		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_PXGT_OPERATE) {
			Opreate_RequestWsk_pxgt req = PBUtil.toObject(room_rq, Opreate_RequestWsk_pxgt.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_SORT_BY_ORDER: {
			deal_sort_card_by_order(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_SORT_BY_COUNT: {
			deal_sort_card_by_count(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_SORT_BY_510K: {
			deal_sort_card_by_510K(seat_index);
			return true;
		}
		}
		return true;
	}

	public void out_card_sort_card_by_data(int seat_index, int out_card_data[], int out_card_count) {
		int out_cards[] = new int[out_card_count];
		int temp_count = 0;
		int cut_color = 0;
		for (int i = 0; i < out_card_count; i++) {
			if (out_card_data[i] > 0x100) {
				out_cards[temp_count++] = out_card_data[i];
			}
		}
		if (temp_count <= 0) {
			return;
		}
		int color_add = out_cards[0] / 0x100;
		if (color_add > 0) {
			cut_color++;
		}
		for (int i = 1; i < temp_count; i++) {
			if (color_add != out_cards[i] / 0x100) {
				color_add = out_cards[i] / 0x100;
				cut_color++;
			}
		}
		for (int i = 0; i < temp_count; i++) {
			if (out_cards[i] > 0x100) {
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {

					int temp = GRR._cards_data[seat_index][j];
					GRR._cards_data[seat_index][j] = temp & (~(out_cards[i] / 0x100 * 0x100));
				}

			}
		}

		int temp_color = 0;
		for (int i = 0; i < temp_count; i++) {
			if ((out_cards[i] / 0x100 < temp_color || temp_color == 0) && out_cards[i] > 0x100) {
				temp_color = out_cards[i] / 0x100;
				for (int j = 0; j < this.GRR._card_count[seat_index]; j++) {
					int temp = GRR._cards_data[seat_index][j];
					if (temp > (out_cards[i] / 0x100 * 0x100) && temp > 0x100) {
						GRR._cards_data[seat_index][j] = (temp % 0x100) + ((temp / 0x100 * 0x100) >> 1);
					}

				}
			}
		}

		player_sort_card[seat_index] -= cut_color;

		// int _score_type = sort[seat_index];
		// if (_score_type == GameConstants.WSK_ST_COUNT) {
		// _score_type = GameConstants.WSK_ST_ORDER;
		// } else if (_score_type == GameConstants.WSK_ST_ORDER || _score_type
		// == GameConstants.WSK_ST_CUSTOM) {
		// _score_type = GameConstants.WSK_ST_COUNT;
		// }
		// sort[seat_index] = _score_type;
		_score_type = GameConstants.WSK_ST_CUSTOM;
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], this.sort[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_order(int seat_index) {
		if (GRR == null) {
			return;
		}
		_score_type = GameConstants.WSK_ST_ORDER;
		sort[seat_index] = _score_type;
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], this.sort[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_count(int seat_index) {
		if (GRR == null) {
			return;
		}
		_score_type = GameConstants.WSK_ST_COUNT;
		sort[seat_index] = _score_type;

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type);
		RefreshCard(seat_index);
	}

	public void deal_sort_card_by_510K(int seat_index) {
		if (GRR == null) {
			return;
		}
		_score_type = GameConstants.WSK_ST_510K;
		sort[seat_index] = _score_type;

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index], _score_type);
		RefreshCard(seat_index);
	}

	public void RefreshCard(int to_player) {
		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_REFRESH_CARD);
				// 发送数据
				RefreshCardData_pxgt.Builder refresh_card = RefreshCardData_pxgt.newBuilder();
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					if (index == i) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards_card.addItem(this.GRR._cards_data[i][j]);
						}
					}
					refresh_card.addHandCardsData(cards_card);
					refresh_card.addHandCardCount(GRR._card_count[i]);
				}
				roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
				// 自己才有牌数据
				this.send_response_to_player(index, roomResponse);
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_REFRESH_CARD);
			// 发送数据
			RefreshCardData_pxgt.Builder refresh_card = RefreshCardData_pxgt.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
				refresh_card.addHandCardsData(cards_card);
				refresh_card.addHandCardCount(GRR._card_count[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_REFRESH_CARD);
			// 发送数据
			RefreshCardData_pxgt.Builder refresh_card = RefreshCardData_pxgt.newBuilder();
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

	}

	public void send_liang_pai(int card_data) {

		int team_seat[] = new int[2];
		team_seat[0] = GameConstants.INVALID_SEAT;
		team_seat[1] = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (this.GRR._cards_data[i][j] == _repertory_card[_liang_card_value]) {
					if (team_seat[0] == GameConstants.INVALID_SEAT) {
						team_seat[0] = i;
					} else {
						team_seat[1] = i;
					}
				}
			}
		}
		if (team_seat[0] == team_seat[1] || (team_seat[0] + 2) % this.getTablePlayerNumber() == team_seat[1]) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.send_error_notify(i, 2, "本局不需要换位置");
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_LIANG_PAI_RESULT);
		// 发送数据
		LiangPai_Result_pxgt.Builder liang_pai_result = LiangPai_Result_pxgt.newBuilder();
		liang_pai_result.setCardData(card_data);
		for (int i = 0; i < team_seat.length; i++) {
			liang_pai_result.addSeatIndex(team_seat[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

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

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD: {
			shuffle_players_data();
			operate_out_card(GameConstants.INVALID_SEAT, 0, null, _turn_out_card_type, GameConstants.INVALID_SEAT,
					false);

			return;
		}
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void test_cards() {
		// int cards[] = new int[] { 0x0d, 0x1d, 0x2d, 0x3D, 0x0D, 0x1D, 0x2D,
		// 0x4E, 0x4F, 0x06, 0x16, 0x26, 0x36, 0x07, 0x07, 0x07 };
		// int cards[] = new int[] { 0x02, 0x12, 0x22, 0x32, 0x02, 0x12, 0x22,
		// 0x01, 0x4F, 0x07, 0x17, 0x27, 0x37, 0x07, 0x17, 0x27, 0x01, 0x09,
		// 0x0a, 0x1a, 0x2a, 0x3a, 0x0a, 0x1a, 0x2a, 0x01, 0x19, };

		int cards[] = new int[] { 18, 7, 55, 58, 49, 43, 35, 40, 56, 42, 6, 37, 1, 5, 20, 52, 12, 9, 34, 55, 27, 61, 27,
				53, 57, 19, 60, 3, 23, 54, 43, 50, 58, 44, 38, 56, 49, 19, 35, 45, 51, 53, 45, 6, 23, 8, 3, 24, 39, 7,
				25, 33, 21, 8, 54, 10, 12, 61, 26, 4, 28, 59, 36, 20, 41, 38, 41, 4, 36, 57, 33, 18, 29, 50, 2, 34, 39,
				2, 13, 11, 13, 9, 51, 21, 28, 40, 1, 17, 22, 5, 60, 17, 24, 11, 37, 44, 22, 59, 42, 10, 26, 52, 25,
				29 };
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[index++];
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
		roomResponse.setType(WSKMsgConstants.RESPONSE_PXGT_REFRESH_DUIYOU_CARD);
		// 发送数据
		Refresh_Pai_pxgt.Builder refresh_card = Refresh_Pai_pxgt.newBuilder();
		refresh_card.setSeatIndex(_friend_seat[seat_index]);
		for (int j = 0; j < this.GRR._card_count[_friend_seat[seat_index]]; j++) {
			if (this.GRR._cards_data[_friend_seat[seat_index]][j] > 0x100) {
				refresh_card.addCardsData(this.GRR._cards_data[_friend_seat[seat_index]][j] - 0x100);
			} else {
				refresh_card.addCardsData(this.GRR._cards_data[_friend_seat[seat_index]][j]);
			}
		}
		refresh_card.setCardCount(GRR._card_count[_friend_seat[seat_index]]);

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据h
		this.send_response_to_player(seat_index, roomResponse);
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
