/**
 * 
 */
package com.cai.game.ddz.handler.henanddz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DDZAutoOpreateRunnable;
import com.cai.future.runnable.DDZAutoOutCardRunnable;
import com.cai.future.runnable.DDZCallBnakerRunnable;
import com.cai.future.runnable.DDZOutCardHandleRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.ddz.DDZGameLogic;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.handler.DDZHandlerAddtimes;
import com.cai.game.ddz.handler.DDZHandlerCallBanker;
import com.cai.game.ddz.handler.DDZHandlerFinish;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.ddz.DdzRsp.AddTimesDDZResult;
import protobuf.clazz.ddz.DdzRsp.Add_Times_Request;
import protobuf.clazz.ddz.DdzRsp.Call_Banker_Request;
import protobuf.clazz.ddz.DdzRsp.GameStartDDZ;
import protobuf.clazz.ddz.DdzRsp.OutCardData;
import protobuf.clazz.ddz.DdzRsp.OutCardData_Request;
import protobuf.clazz.ddz.DdzRsp.PukeGameEndDdz;
import protobuf.clazz.ddz.DdzRsp.ReDispath;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;

///////////////////////////////////////////////////////////////////////////////////////////////
public class DDZ_HENAN_Table extends DDZTable {

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public DDZ_HENAN_Table() {

		_logic = new DDZGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		_turn_out_card_data = new int[GameConstants.DDZ_MAX_COUNT_JD];
		_di_pai_card_count = GameConstants.DDZ_DI_PAI_COUNT_JD;
		_di_pai_card_data = new int[GameConstants.DDZ_DI_PAI_COUNT_JD];
		_di_pai_type = GameConstants.DDZ_CT_DI_PAI_ERROR;
		_add_times_operate = new boolean[this.getTablePlayerNumber()];
		playerNumber = 0;

	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		game_cell = 1;
		_times = 1;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_rule_des());
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		_init_hand_card = new int[getTablePlayerNumber()][this.get_hand_card_count_max()];
		_init_hand_card_count = new int[getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
		}
		_user_times = new int[getTablePlayerNumber()];
		// 初始化基础牌局handle
		_handler_out_card_operate = new DDZHandlerOutCardOperate_HENAN();
		_handler_call_banker = new DDZHandlerCallBanker();
		_handler_add_times = new DDZHandlerAddtimes();
		_handler_call_banker = new DDZHandlerCallBanker_HENAN();

		_handler_finish = new DDZHandlerFinish();
		this.setMinPlayerCount(3); // 最小开局人数为2
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		_turn_out_card_count = 0;
		_di_pai_card_count = GameConstants.DDZ_DI_PAI_COUNT_JD;
		_call_banker_type = 0;
		_call_banker_status = 0;
		_call_banker_score = 0;
		Arrays.fill(_qiang_action, -1);
		Arrays.fill(_call_action, -1);
		Arrays.fill(_add_times, -1);
		Arrays.fill(_user_times, 1);
		Arrays.fill(_out_card_times, 0);

		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
		}
		for (int i = 0; i < _di_pai_card_count; i++) {
			_di_pai_card_data[i] = GameConstants.INVALID_CARD;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_add_times_operate[i] = false;
		}
		_turn_out_card_type = GameConstants.DDZ_CT_ERROR;
		//
		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_HENAN)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_DDZ_JD];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DDZ);
		}
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}

		return game_start_ddzjd();
	}

	// 经典斗地主开始
	public boolean game_start_ddzjd() {
		_game_status = GameConstants.GS_CALL_BANKER;// 设置状态
		// 设置玩家状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].set_status(GameConstants.Player_Status_NULL);
			_playerStatus[i]._call_banker = -1;
			_playerStatus[i]._qiang_banker = -1;
		}
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_GAME_START);
			GameStartDDZ.Builder gameStartResponse = GameStartDDZ.newBuilder();
			RoomInfoDdz.Builder room_info = getRoomInfoDdz();
			gameStartResponse.setRoomInfo(room_info);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data_game_start(gameStartResponse);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameStartResponse.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				gameStartResponse.addCardsData(i, cards_card);
				gameStartResponse.addDifenBombDes(get_boom_difen_des(i));
			}
			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[index]; j++) {
				cards_card.addItem(GRR._cards_data[index][j]);
			}
			gameStartResponse.setDiPaiCardCount(GameConstants.DDZ_DI_PAI_COUNT_JD);
			gameStartResponse.setCardsData(index, cards_card);

			roomResponse.setCommResponse(PBUtil.toByteString(gameStartResponse));
			this.send_response_to_player(index, roomResponse);
		}
		GameSchedule.put(new DDZCallBnakerRunnable(getRoom_id(), this), 1500, TimeUnit.MILLISECONDS);

		this._handler = this._handler_call_banker;
		// 回放功能
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_GAME_START);
		GameStartDDZ.Builder gameStartResponse = GameStartDDZ.newBuilder();
		RoomInfoDdz.Builder room_info = getRoomInfoDdz();
		gameStartResponse.setRoomInfo(room_info);
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			if (this._cur_round == 1) {
				this.load_player_info_data_game_start(gameStartResponse);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameStartResponse.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				gameStartResponse.addCardsData(i, cards_card);
			}
			gameStartResponse.addDifenBombDes(get_boom_difen_des(index));
			gameStartResponse.setDiPaiCardCount(GameConstants.DDZ_DI_PAI_COUNT_JD);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(gameStartResponse));
		GRR.add_room_response(roomResponse);
		return true;
	}

	public int get_hand_card_count_max() {
		return GameConstants.DDZ_MAX_COUNT_JD;
	}

	public int get_no_banker_card_count() {
		return GameConstants.DDZ_MAX_COUNT_JD - GameConstants.DDZ_DI_PAI_COUNT_JD;
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int card_cards[]) {

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 10 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < get_no_banker_card_count(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * get_no_banker_card_count() + j];
			}
			GRR._card_count[i] = get_no_banker_card_count();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
		for (int i = 0; i < _di_pai_card_count; i++) {
			_di_pai_card_data[i] = repertory_card[count * get_no_banker_card_count() + i];
		}
		_logic.sort_card_date_list(_di_pai_card_data, _di_pai_card_count);
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {
		int cards[] = new int[] { 0x23, 0x13, 0x03, 0x33, 0x05, 0x15, 0x25, 0x0a, 0x1a, 0x2a, 0x0b, 0x2b, 0x1b, 0x0c,
				0x1c, 0x2c, 0x19 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_no_banker_card_count(); j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		int di_cards[] = new int[] { 0x03, 0x13, 0x12 };
		for (int i = 0; i < _di_pai_card_count; i++) {
			_di_pai_card_data[i] = di_cards[i];
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 17) {
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
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
		_logic.sort_card_date_list(_di_pai_card_data, _di_pai_card_count);
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
		_banker_select = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < count / getTablePlayerNumber(); j++) {
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

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {

		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_HENAN)) {
			ret = this.handler_game_finish_ddzjd(seat_index, reason);
		}

		return ret;
	}

	public boolean handler_game_finish_ddzjd(int seat_index, int reason) {
		int real_reason = reason;
		boolean b_record = false;
		if (_game_status != GameConstants.GS_MJ_WAIT) {
			b_record = true;
		}
		_game_status = GameConstants.GS_MJ_WAIT;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndDdz.Builder game_end_ddz = PukeGameEndDdz.newBuilder();

		RoomInfoDdz.Builder room_info = getRoomInfoDdz();
		game_end_ddz.setRoomInfo(room_info);
		game_end_ddz.setPlayerNum(count);
		game_end_ddz.setGameRound(_game_round);
		game_end_ddz.setCurRound(_cur_round);

		boolean end = false;
		int end_score[] = new int[this.getTablePlayerNumber()];
		int chuntian[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(chuntian, -1);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score_ddz_jd(end_score, chuntian);
		}
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_ddz.addAllEndScore((int) _player_result.game_score[i]);
				}
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_ddz.addAllEndScore((int) _player_result.game_score[i]);
			}
		}

		if (GRR != null) {
			game_end.setGameTypeIndex(GRR._game_type_index);
			game_end_ddz.setBankerPlayer(this.GRR._banker_player);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				game_end_ddz.addCardCount(GRR._card_count[i]);
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				game_end_ddz.addCardsData(i, cards_card);
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end_ddz.addEndScore(end_score[i]);
			game_end.addGameScore(end_score[i]);
			game_end_ddz.addWinNum(this._win_num[i]);
			game_end_ddz.addLoseNum(this._lose_num[i]);
			game_end_ddz.addTimes(this._user_times[i]);
			if (chuntian[i] != -1) {
				game_end_ddz.addChunTianPlayer(chuntian[i]);
			}
		}
		this.load_player_info_data_game_end(game_end_ddz);
		game_end_ddz.setReason(real_reason);
		game_end_ddz.setCellScore((int) this.game_cell);
		////////////////////////////////////////////////////////////////////// 得分总的
		RoomInfo.Builder room_info_end = getRoomInfo();
		game_end.setStartTime(this.getStartGameTime());
		game_end.setRoomInfo(room_info_end);
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_ddz));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		if (b_record) {
			record_game_round(game_end, reason);
		}

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
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		istrustee = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse_reustee = RoomResponse.newBuilder();
			roomResponse_reustee.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse_reustee.setOperatePlayer(i);
			roomResponse_reustee.setIstrustee(istrustee[i]);
			this.send_response_to_room(roomResponse_reustee);
		}
		if (has_rule(GameConstants.GAME_RULE_RANDOM_CALL_BANKER)) {
			this._banker_select = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % getTablePlayerNumber());
		}
		if (!is_sys()) {
			GRR = null;
		}
		// 错误断言
		return false;
	}

	public void cal_score_ddz_jd(int end_score[], int chuntian[]) {
		int win_player = GameConstants.INVALID_SEAT;
		int win_score = 0;
		int chuntian_index = 0;
		boolean bChunTian = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.GRR._card_count[i] == 0) {
				win_player = GameConstants.INVALID_SEAT;
				win_player = i;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (win_player == i) {
				continue;
			}
			if (win_player == this.GRR._banker_player) {
				if (this._out_card_times[i] != 0) {
					bChunTian = false;
				}
			} else {
				if (this._out_card_times[GRR._banker_player] > 1) {
					bChunTian = false;
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (win_player == this.GRR._banker_player) {
				if (i != win_player) {
					if (bChunTian) {
						end_score[i] = (int) (-this._user_times[i] * this.game_cell);
						chuntian[chuntian_index++] = i;
					} else {
						end_score[i] = (int) (-this._user_times[i] * this.game_cell);
					}
				} else {
					if (bChunTian) {
						end_score[i] = (int) (this._user_times[i] * this.game_cell);
					} else {
						end_score[i] = (int) (this._user_times[i] * this.game_cell);
					}
				}

			} else {
				if (i != this.GRR._banker_player) {
					if (bChunTian) {
						end_score[i] = (int) (this._user_times[i] * this.game_cell);
					} else {
						end_score[i] = (int) (this._user_times[i] * this.game_cell);
					}
				} else {
					if (bChunTian) {
						end_score[this.GRR._banker_player] = (int) (-this._user_times[this.GRR._banker_player]
								* this.game_cell);
						chuntian[chuntian_index++] = i;
					} else {
						end_score[this.GRR._banker_player] = (int) (-this._user_times[this.GRR._banker_player]
								* this.game_cell);
					}
				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] < 0) {
				this._lose_num[i]++;
			} else {
				this._win_num[i]++;
			}
			_game_score[i] += end_score[i];
			_player_result.game_score[i] += end_score[i];
		}

	}

	/**
	 * @return
	 */
	public RoomInfoDdz.Builder getRoomInfoDdz() {
		RoomInfoDdz.Builder room_info = RoomInfoDdz.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_rule_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._banker_select);
		room_info.setCreateName(this.getRoom_owner_name());

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (GRR == null) {
			return true;
		}
		if (type == MsgConstants.REQUST_CALL_QIANG_ZHUANG) {
			Call_Banker_Request req = PBUtil.toObject(room_rq, Call_Banker_Request.class);
			// 逻辑处理
			return handler_requst_call_qiang_zhuang(seat_index, req.getSelectCallBanker(), req.getSelectQiangBanker());
		} else if (type == MsgConstants.REQUST_DDZ_OUT_CARD_MUL) {
			OutCardData_Request req = PBUtil.toObject(room_rq, OutCardData_Request.class);
			// 逻辑处理
			return handler_operate_out_card_mul(seat_index, req.getOutCardsList(), req.getOutCardCount(),
					req.getBOutCardType());
		} else if (type == MsgConstants.REQUST_DDZ_ADD_TIMES) {
			Add_Times_Request req = PBUtil.toObject(room_rq, Add_Times_Request.class);
			// 逻辑处理
			return handler_operate_add_times(seat_index, req.getAddTimes());
		}

		return true;
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

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		_player_ready[seat_index] = 1;
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
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

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}
		handler_game_start();

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		// 游戏开始后刷新玩家
		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setGameStatus(_game_status);
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);
		this.send_response_to_player(seat_index, roomResponse2);

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
						.get(3007);
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
		// if(this._cur_round > 0 )
		// return handler_player_ready(seat_index,false);
		return true;

	}

	/**
	 * //用户出牌
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
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count,
			int b_out_card) {
		if (get_seat_index != this._current_player) {
			return false;
		}
		if (this._handler != null) {
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

	public void exe_dispath() {
		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_HENAN)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_DDZ_JD];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DDZ);
		}
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			ReDispath.Builder re_dispath = ReDispath.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_RE_DISPATH);// 201
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				re_dispath.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				re_dispath.addCardsData(i, cards_card);
			}
			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[index]; j++) {
				cards_card.addItem(GRR._cards_data[index][j]);
			}
			re_dispath.setCardsData(index, cards_card);

			roomResponse.setCommResponse(PBUtil.toByteString(re_dispath));
			this.send_response_to_player(index, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		ReDispath.Builder re_dispath = ReDispath.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_RE_DISPATH);// 201
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			re_dispath.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			re_dispath.addCardsData(i, cards_card);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(re_dispath));
		GRR.add_room_response(roomResponse);
		this._banker_select = GameConstants.INVALID_SEAT;
		GameSchedule.put(new DDZCallBnakerRunnable(getRoom_id(), this), 1500, TimeUnit.MILLISECONDS);
	}

	public boolean exe_call_banker_finish(int seat_index, boolean bOut) {
		Arrays.fill(_add_times, -1);
		Arrays.fill(_qiang_banker, -1);
		Arrays.fill(_call_banker, -1);
		Arrays.fill(_qiang_action, -1);
		Arrays.fill(_call_action, -1);
		if (seat_index == GameConstants.INVALID_SEAT) {
			exe_dispath();
			return true;
		}
		if (has_rule(GameConstants.GAME_RULE_QIANG_DIZHU) && !bOut) {
			if (this._handler_add_times != null) {
				this._handler_add_times.reset_status(seat_index);
				this._handler_add_times.exe(this);
			}
		} else {
			_user_times[_banker_select] = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player) {
					continue;
				}
				_user_times[_banker_select] += _user_times[i];
			}
			// 比赛场更新数据
			PlayerServiceImpl.getInstance().updateRoomInfo(getRoom_id());

			_game_status = GameConstants.GS_MJ_PLAY;
			// 确定出牌玩家
			RoomResponse.Builder roomResponse_outplayer = RoomResponse.newBuilder();
			OutCardData.Builder outcarddata = OutCardData.newBuilder();
			roomResponse_outplayer.setType(MsgConstants.RESPONSE_DDZ_OUT_CARD);// 201
			roomResponse_outplayer.setTarget(seat_index);
			outcarddata.setOutCardPlayer(GameConstants.INVALID_SEAT);
			outcarddata.setCurPlayer(seat_index);
			outcarddata.setIsFirstOut(2);
			outcarddata.setDisplayTime(20);
			outcarddata.setCurPlayerYaPai(1);

			// 刷新玩家手牌数量
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				outcarddata.addUserCardCount(GRR._card_count[i]);
				outcarddata.addUserCardsData(cards_card);
				outcarddata.addDifenBombDes(get_boom_difen_des(i));
			}

			// 手牌--将自己的手牌数据发给自己

			for (int j = 0; j < GRR._card_count[seat_index]; j++) {
				cards_card.addItem(GRR._cards_data[seat_index][j]);
			}
			outcarddata.addUserCardsData(seat_index, cards_card);
			roomResponse_outplayer.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse_outplayer);

			int delay = 0;
			delay = 20;
			if (istrustee[_current_player]) {
				_trustee_auto_opreate_scheduled[_current_player] = GameSchedule
						.put(new DDZAutoOpreateRunnable(getRoom_id(), this, _current_player), 1, TimeUnit.SECONDS);
			}
			_auto_out_card_scheduled = GameSchedule.put(new DDZAutoOutCardRunnable(getRoom_id(), _current_player, this),
					delay, TimeUnit.SECONDS);

			return this.send_response_to_room(roomResponse_outplayer);
		}
		return true;

	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {

		// if (this._handler != null) {
		// this._handler.handler_operate_card(this, seat_index, operate_code,
		// operate_card, luoCode);
		// }

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
		return true;
	}

	/**
	 * @param seat_index
	 * @param call_banker
	 * @param qiang_banker
	 * @return
	 */
	@Override
	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
		if (seat_index != this._current_player) {
			return false;
		}
		if (this._handler_call_banker != null) {
			this._handler = this._handler_call_banker;
			this._handler_call_banker.handler_call_banker(this, seat_index, call_banker, qiang_banker);
		}
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

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

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
			if (GameConstants.GS_MJ_FREE == _game_status) {
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
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
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
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
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
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

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
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
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
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (player != null && getRoom_owner_account_id() != player.getAccount_id()) {
				send_error_notify(seat_index, 2, "不是创建者不能解散房间");
				return false;
			}
			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

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
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出
			if (GameConstants.GS_MJ_FREE != _game_status) {
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
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
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

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);// 29
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * f 刷新玩家信息
	 * 
	 * @return
	 */
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		if (GRR._card_count[seat_index] == 0) {
			boolean bChunTian = true;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (seat_index == i) {
					continue;
				}
				if (seat_index == this.GRR._banker_player) {
					if (this._out_card_times[i] != 0) {
						bChunTian = false;
					}
				} else {
					if (this._out_card_times[GRR._banker_player] > 1) {
						bChunTian = false;
					}
				}
			}
			if (bChunTian) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					_user_times[i] *= 2;
				}
			}
		}

		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData.Builder outcarddata = OutCardData.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_OUT_CARD);// 201
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
			outcarddata.setPrOutCardType(this._turn_out_card_type);
			outcarddata.setCurPlayer(this._current_player);
			if (this._current_player != GameConstants.INVALID_SEAT) {
				if (_turn_out_card_count == 0 || this._current_player != index) {
					outcarddata.setDisplayTime(20);
					outcarddata.setCurPlayerYaPai(1);
				} else {
					if (!_logic.SearchOutCard(GRR._cards_data[_current_player], GRR._card_count[_current_player],
							_turn_out_card_data, _turn_out_card_count)) {
						outcarddata.setDisplayTime(10);
						outcarddata.setCurPlayerYaPai(0);
					} else {
						outcarddata.setDisplayTime(20);
						outcarddata.setCurPlayerYaPai(1);
					}
				}

			} else {
				outcarddata.setDisplayTime(20);
				outcarddata.setCurPlayerYaPai(1);
			}

			if (_turn_out_card_count == 0) {
				outcarddata.setIsFirstOut(1);
			} else {
				outcarddata.setIsFirstOut(0);
			}
			// 刷新玩家手牌数量
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				outcarddata.addUserCardCount(GRR._card_count[i]);
				outcarddata.addUserCardsData(cards_card);

			}
			// 手牌--将自己的手牌数据发给自己
			for (int j = 0; j < GRR._card_count[index]; j++) {
				cards_card.addItem(GRR._cards_data[index][j]);
			}
			outcarddata.addUserCardsData(index, cards_card);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				outcarddata.addDifenBombDes(get_boom_difen_des(i));
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			this.send_response_to_player(index, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardData.Builder outcarddata = OutCardData.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_OUT_CARD);// 201
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
		outcarddata.setPrOutCardType(this._turn_out_card_type);
		outcarddata.setCurPlayer(this._current_player);
		if (this._current_player != GameConstants.INVALID_SEAT) {
			if (_turn_out_card_count == 0) {
				outcarddata.setDisplayTime(20);
				outcarddata.setCurPlayerYaPai(1);
			} else {
				if (!_logic.SearchOutCard(GRR._cards_data[_current_player], GRR._card_count[_current_player],
						_turn_out_card_data, _turn_out_card_count)) {
					outcarddata.setDisplayTime(10);
					outcarddata.setCurPlayerYaPai(0);
				} else {
					outcarddata.setDisplayTime(20);
					outcarddata.setCurPlayerYaPai(1);
				}
			}

		} else {
			outcarddata.setDisplayTime(20);
			outcarddata.setCurPlayerYaPai(1);
		}

		if (_turn_out_card_count == 0) {
			outcarddata.setIsFirstOut(1);
		} else {
			outcarddata.setIsFirstOut(0);
		}
		// 刷新玩家手牌数量

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			outcarddata.addUserCardCount(GRR._card_count[i]);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			outcarddata.addDifenBombDes(get_boom_difen_des(i));
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			outcarddata.addUserCardsData(cards_card);
			outcarddata.addDifenBombDes(get_boom_difen_des(i));
		}
		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;

	}

	public void out_card_time_finish(int seat_index) {
		// if(this.is_sys()){
		//
		// }else{
		// if(this._out_card_player == seat_index && GRR._card_count[seat_index]
		// == 1){
		// this._handler_out_card_operate.reset_status(seat_index,GRR._cards_data[seat_index],GRR._card_count[seat_index],GameConstants.DDZ_CT_SINGLE);
		// this._handler.exe(this);
		// }else{
		// int delay=10;
		// if(this._out_card_player == seat_index){
		// delay=5;
		// }else if(!_logic.SearchOutCard(GRR._cards_data[seat_index],
		// GRR._card_count[seat_index],
		// _turn_out_card_data, _turn_out_card_count)){
		// delay=1;
		// }else{
		// delay=5;
		// }
		// _trustee_auto_opreate_scheduled[_current_player]=GameSchedule.put(new
		// DDZAutoOpreateRunnable(getRoom_id(), this,seat_index ),
		// delay, TimeUnit.SECONDS);
		// }
		// }
		if (_turn_out_card_count != 0 && !_logic.SearchOutCard(GRR._cards_data[seat_index], GRR._card_count[seat_index],
				_turn_out_card_data, _turn_out_card_count)) {
			int card[] = new int[1];
			this._handler_out_card_operate.reset_status(seat_index, card, 0, GameConstants.DDZ_CT_PASS);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
		} else {

		}
	}

	public void auto_out_card(int seat_index) {
		if (this.istrustee[seat_index] && this._out_card_player == seat_index) {
			int card_type = this._logic.GetCardType(GRR._cards_data[seat_index], GRR._card_count[seat_index],
					GRR._cards_data[seat_index]);
			if (card_type != GameConstants.DDZ_CT_ERROR) {
				this._handler_out_card_operate.reset_status(seat_index, GRR._cards_data[seat_index],
						GRR._card_count[seat_index], card_type);
				this._handler.exe(this);
				return;
			}
		}
		int card[] = new int[1];

		int out_card_data[] = new int[GameConstants.DDZ_MAX_COUNT_JD];
		int out_card_count = 0;
		out_card_count = _logic.AiAutoOutCard(GRR._cards_data[seat_index], GRR._card_count[seat_index],
				_turn_out_card_data, this._turn_out_card_count, out_card_data, seat_index, this.GRR._banker_player,
				_turn_out__player);
		if (out_card_count == 0) {
			this._handler_out_card_operate.reset_status(seat_index, card, 0, GameConstants.DDZ_CT_PASS);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
		} else {
			int card_type = this._logic.GetCardType(out_card_data, out_card_count, out_card_data);
			this._handler_out_card_operate.reset_status(seat_index, out_card_data, out_card_count, card_type);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
		}
	}

	public void call_banker_start() {
		if (this._handler_call_banker != null) {
			this._handler = this._handler_call_banker;
			this._current_player = GameConstants.INVALID_SEAT;
			this._handler_call_banker.reset_status(this._current_player, _game_status);
			this._handler.exe(this);
		}
	}

	public void call_banker(int seat_index, int type) {
		if (seat_index != this._current_player) {
			return;
		}
		if (this._handler_call_banker != null) {
			this._handler = this._handler_call_banker;
			int call_banker = -1;
			int qiang_banker = -1;
			if (type == 1) {
				call_banker = 0;
			} else {
				qiang_banker = 0;
			}
			this._handler_call_banker.handler_call_banker(this, seat_index, call_banker, qiang_banker);
		}
	}

	public void auto_add_time(int seat_index) {
		if (_add_times_operate[seat_index]) {
			return;
		}
		_add_times_operate[seat_index] = true;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_ADD_TIMES_RESULE);
		AddTimesDDZResult.Builder add_time_result = AddTimesDDZResult.newBuilder();
		add_time_result.setAddtimesaction(0);
		add_time_result.setOpreatePlayer(seat_index);

		for (int x = 0; x < getTablePlayerNumber(); x++) {
			add_time_result.addDifenBombDes(get_boom_difen_des(x));
		}
		roomResponse.setCommResponse(PBUtil.toByteString(add_time_result));
		send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		if (_auto_add_time_scheduled[seat_index] != null) {
			_auto_add_time_scheduled[seat_index].cancel(false);
			_auto_add_time_scheduled[seat_index] = null;
		}
		if (_trustee_auto_opreate_scheduled[seat_index] != null) {
			_trustee_auto_opreate_scheduled[seat_index].cancel(false);
			_trustee_auto_opreate_scheduled[seat_index] = null;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (!_add_times_operate[i]) {
				return;
			}
		}
		GameSchedule.put(
				new DDZOutCardHandleRunnable(getRoom_id(), GRR._banker_player, this, -1, -1, -1, -1, true, false),
				GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

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
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time) {
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

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 删除牌堆的牌 (吃碰杆的那张牌 从废弃牌堆上移除)
	 * 
	 * @param seat_index
	 * @param discard_index
	 * @return
	 */

	public boolean operate_remove_discard(int seat_index, int discard_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setDiscardIndex(discard_index);

		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

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

		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	public boolean send_play_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		this.load_common_status(roomResponse);
		// 游戏变量
		tableResponse.setBankerPlayer(GRR._banker_player);
		tableResponse.setCurrentPlayer(_current_player);
		tableResponse.setCellScore(0);

		// 历史记录

		tableResponse.setOutCardPlayer(_out_card_player);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			//
			tableResponse.addWinnerOrder(0);

		}

		return true;

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
				// win_idx++;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);

			player_result.addPlayersId(i);
			// }
		}

		player_result.setRoomId(this.getRoom_id());
		player_result.setRoomOwnerAccountId(this.getRoom_owner_account_id());
		player_result.setRoomOwnerName(this.getRoom_owner_name());
		player_result.setCreateTime(this.getCreate_time());
		player_result.setRecordId(this.get_record_id());
		player_result.setGameRound(_game_round);
		player_result.setGameRuleDes(get_rule_des());
		player_result.setGameRuleIndex(_game_rule_index);
		player_result.setGameTypeIndex(_game_type_index);
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
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
			room_player.setJiaoDiZhu(_playerStatus[i]._call_banker);
			room_player.setQiangDiZhu(_playerStatus[i]._qiang_banker);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public boolean send_response_to_special(int seat_index, int to_player, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;

		player = this.get_players()[to_player];
		if (player == null)
			return true;
		if (to_player == seat_index)
			return true;
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

		PlayerServiceImpl.getInstance().send(this.get_players()[to_player], responseBuilder.build());

		return true;
	}

	private void progress_banker_select() {
		if (_banker_select == GameConstants.INVALID_SEAT) {
			_banker_select = 0;// 创建者的玩家为专家
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	public String get_rule_des() {
		String des = "";
		if (has_rule(GameConstants.GAME_RULE_JIAO_DI_ZHU)) {
			des += "叫地主玩法;";
		} else if (has_rule(GameConstants.GAME_RULE_JIAO_FEN)) {
			des += "叫分玩法;";
		} else {
			des += "抢地主玩法;";
		}
		if (this.has_rule(GameConstants.GAME_RULE_LIMIT_FOUR_BOMB)) {
			des += "炸弹上限：2炸;";
		} else if (this.has_rule(GameConstants.GAME_RULE_LIMIT_FIVE_BOMB)) {
			des += "炸弹上限：3炸;";
		} else if (this.has_rule(GameConstants.GAME_RULE_LIMIT_SIX_BOMB)) {
			des += "炸弹上限：4炸;";
		}
		des += "底分：" + game_cell + "分\t" + "倍数：" + _times + "倍;";
		return des;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {

		this._handler = this._handler_finish;
		// this._handler_finish.exe(this);
		return true;
	}

	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(
				new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()),
				delay, TimeUnit.MILLISECONDS);

		return true;

	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch,
			boolean self, boolean d, int delay) {
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}

		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(this.getRoom_id(), seat_index, provide_player, center_card, action,
					type, depatch, self, d), delay, TimeUnit.MILLISECONDS);
		} else {

		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {

		return true;

	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_out_card(int seat_index, int card, int type) {

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
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
	public void runnable_remove_middle_cards(int seat_index) {

	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i].set_seat_index(i);
		}
	}

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (this.is_sys()) {
			istrustee[get_seat_index] = !istrustee[get_seat_index];
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(get_seat_index);
			roomResponse.setIstrustee(istrustee[get_seat_index]);
			this.send_response_to_room(roomResponse);

			if (istrustee[get_seat_index] && _call_banker_finish_scheduled != null) {
				_trustee_auto_opreate_scheduled[get_seat_index] = GameSchedule
						.put(new DDZAutoOpreateRunnable(getRoom_id(), this, get_seat_index), 1, TimeUnit.SECONDS);
			} else if (!istrustee[get_seat_index]) {
				if (_trustee_auto_opreate_scheduled[get_seat_index] != null)
					_trustee_auto_opreate_scheduled[get_seat_index].cancel(false);
				_trustee_auto_opreate_scheduled[get_seat_index] = null;
			}
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if (this.has_rule(GameConstants.GAME_RULE_TWO_PLAY_DDZ)) {
			return 2;
		}
		return 3;
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
				.get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(
					this.get_players()[i].getAccount_id(), score, false, buf.toString(), EMoneyOperateType.ROOM_COST);
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
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	public boolean open_card_timer() {
		return false;
	}

	public boolean robot_banker_timer() {
		return false;
	}

	public boolean ready_timer() {
		return false;
	}

	public boolean add_jetton_timer() {
		return false;
	}

}
