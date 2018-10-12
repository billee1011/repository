/**
 * 
 */
package com.cai.game.ddz.handler.txw;

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
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.ddz.DDZConstants;
import com.cai.game.ddz.DDZGameLogic_TXW;
import com.cai.game.ddz.DDZMsgConstants;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.Player_EX;
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
import protobuf.clazz.txw.TxwRsp.CallBankerResult_Txw;
import protobuf.clazz.txw.TxwRsp.Card_Arrary_Txw;
import protobuf.clazz.txw.TxwRsp.GameStart_Txw;
import protobuf.clazz.txw.TxwRsp.Opreate_RequestWsk_Txw;
import protobuf.clazz.txw.TxwRsp.OutCardData_Txw;
import protobuf.clazz.txw.TxwRsp.PlayerControl_Txw;
import protobuf.clazz.txw.TxwRsp.PlyarTimes;
import protobuf.clazz.txw.TxwRsp.PukeGameEnd_Txw;
import protobuf.clazz.txw.TxwRsp.SendCardTxw;
import protobuf.clazz.txw.TxwRsp.TableResponse_Txw;
import protobuf.clazz.txw.TxwRsp.round_end_Txw;

///////////////////////////////////////////////////////////////////////////////////////////////
public class TXW_Table extends DDZTable {

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;
	public static final int ID_TIMER_START_TO_CALL_BANKER = 1;// 开始到叫庄
	public static final int ID_TIMER_CALL_BANKER_TO_ADD_TIME = 2;// 叫庄到加倍
	public static final int ID_TIMER_ROUND_FINISH = 3;// 回合结束
	public static final int ID_TIMER_ADD_TIME_OUT_OPREATE = 4;// 加倍到出牌
	public static final int ID_TIMER_AUTO_OPREATE = 5;// 自动出牌
	public static final int ID_TIMER_AUTO_TRUESS = 6;// 自动托管
	public static final int ID_TIMER_AUTO_PASS = 7;// 自动过牌
	public static final int ID_TIMER_AUTO_READY = 8;// 自动准备
	public static final int ID_TIMER_AUTO_ALL_OUT = 9;// 自动出完

	protected static final int GAME_OPREATE_TYPE_CALL_BANKER_NO = 1;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 2;
	protected static final int GAME_OPREATE_TYPE_TI_NO = 3;
	protected static final int GAME_OPREATE_TYPE_TI = 4;
	protected static final int GAME_OPREATE_TYPE_ADD_TI = 5;
	protected static final int GAME_OPREATE_TYPE_HUI_TI = 6;
	protected static final int GAME_OPREATE_TYPE_GEN_TI = 7;

	public int _truess_time = 60000;
	public int _round;
	public int _is_out[]; // 是否出局
	public int _end_score[][];
	public int _init_hand_card[][][];
	public int _init_hand_card_count[][];
	public int _aggin_add_times[];
	DDZGameLogic_TXW _logic;

	public TXW_Table() {
		_logic = new DDZGameLogic_TXW();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;

		_turn_out_card_data = new int[DDZConstants.TXW_MAX_COUT];
		_turn_out_real_data = new int[DDZConstants.TXW_MAX_COUT];
		_add_times_operate = new boolean[this.getTablePlayerNumber()];
		playerNumber = 0;

	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;

		_logic._game_rule_index = _game_rule_index;
		_times = 1;
		_di_pai_card_count = 0;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_rule_des(), this.getTablePlayerNumber());
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		_game_score = new int[getTablePlayerNumber()];
		_player_info = new Player_EX[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
			istrustee[i] = false;
			_player_info[i] = new Player_EX();
		}
		_is_out = new int[getTablePlayerNumber()];
		_call_action = new int[getTablePlayerNumber()];
		_call_banker = new int[getTablePlayerNumber()];
		_add_times = new int[getTablePlayerNumber()];
		_user_times = new int[getTablePlayerNumber()];
		_end_score = new int[2][getTablePlayerNumber()];
		_aggin_add_times = new int[getTablePlayerNumber()];
		_init_hand_card = new int[2][getTablePlayerNumber()][this.get_hand_card_count_max()];
		_init_hand_card_count = new int[2][getTablePlayerNumber()];

		// 初始化基础牌局handle
		_handler_out_card_operate = new DDZHandlerOutCardOperate_TXW();
		_handler_call_banker = new DDZHandlerCallBanker_TXW();
		_handler_add_times = new DDZHandlerAddtimes_TXW();
		_handler_add_times_again = new DDZHandlerAddtimesAgain_TXW();

		// 游戏变量
		_out_card_times = new int[getTablePlayerNumber()];
		_player_ready = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		if (this.has_rule(DDZConstants.GAME_RULE_TXW_BOOM_LIMIT_32)) {
			this._boom_count_limit = 32;
		} else if (this.has_rule(DDZConstants.GAME_RULE_TXW_BOOM_LIMIT_64)) {
			this._boom_count_limit = 64;
		} else if (this.has_rule(DDZConstants.GAME_RULE_TXW_BOOM_LIMIT_128)) {
			this._boom_count_limit = 128;
		} else {
			this._boom_count_limit = 10000;
		}

		_handler_finish = new DDZHandlerFinish();
		this.setMinPlayerCount(3);
	}

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		cancelShedule(ID_TIMER_AUTO_READY);
		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:" + this._game_status);

		reset_init_data();

		_round = 1;
		// 庄家选择
		_turn_out_card_count = 0;
		GRR._banker_player = GameConstants.INVALID_SEAT;
		Arrays.fill(_add_times, -1);
		Arrays.fill(_user_times, 1);
		Arrays.fill(_out_card_times, 0);
		Arrays.fill(_call_action, -1);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_aggin_add_times[i] = -1;
			_is_out[i] = 0;
			GRR._cur_card_type[i] = DDZConstants.TXW_CT_ERROR;

		}

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				_end_score[i][j] = 0;
			}
		}
		for (int i = 0; i < this.get_hand_card_count_max(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
		}

		_turn_out_card_type = GameConstants.DDZ_CT_ERROR;
		//

		if (this.getTablePlayerNumber() == 2 || this.getTablePlayerNumber() == 3) {
			_repertory_card = new int[DDZConstants.CARD_COUNT_TXW_TWO];
			shuffle(_repertory_card, DDZConstants.CARD_DATA_TXW_TWO);
		} else if (this.getTablePlayerNumber() == 4) {
			_repertory_card = new int[DDZConstants.CARD_COUNT_TXW_FOUR];
			shuffle(_repertory_card, DDZConstants.CARD_DATA_TXW_FOUR);
		} else {
			_repertory_card = new int[DDZConstants.CARD_COUNT_TXW_FIVE];
			shuffle(_repertory_card, DDZConstants.CARD_DATA_TXW_FIVE);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}

		progress_banker_select();
		// 比赛场更新数据
		PlayerServiceImpl.getInstance().updateRoomInfo(getRoom_id());
		return on_game_start();
	}

	public void round_reset() {
		_round++;
		shuffle_next();
		GRR._banker_player = GameConstants.INVALID_SEAT;
		Arrays.fill(_add_times, -1);
		Arrays.fill(_user_times, 1);
		Arrays.fill(_out_card_times, 0);
		Arrays.fill(_call_action, -1);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_aggin_add_times[i] = -1;
			_is_out[i] = 0;

		}
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		_turn_out_card_count = 0;
		Arrays.fill(_turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_out_card_times[i] = 0;
			this.GRR._cur_round_count[i] = 0;
			this.GRR._cur_card_type[i] = GameConstants.SXTH_CT_ERROR;
			Arrays.fill(this.GRR._cur_round_data[i], GameConstants.INVALID_CARD);
			this.handler_request_trustee(i, false, 0);
		}
		this._handler = this._handler_call_banker;

	}

	// 开始
	public boolean on_game_start() {
		// 设置玩家状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].set_status(GameConstants.Player_Status_NULL);
			_playerStatus[i]._call_banker = -1;
			_playerStatus[i]._qiang_banker = -1;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_GAME_START);
		GameStart_Txw.Builder gameStartResponse = GameStart_Txw.newBuilder();

		this.load_player_info_data_game_start(gameStartResponse);
		gameStartResponse.setRoomInfo(this.getRoomInfo());
		roomResponse.setCommResponse(PBUtil.toByteString(gameStartResponse));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		Send_Card();
		schedule(ID_TIMER_START_TO_CALL_BANKER, SheduleArgs.newArgs(), 1500);
		this._handler = this._handler_call_banker;

		return true;
	}

	public void Send_Card() {
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_SEND_CARD);
			SendCardTxw.Builder send_card = SendCardTxw.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == play_index) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}
				send_card.addCardCount(this.GRR._card_count[i]);
				send_card.addCardsData(cards_card);
			}
			send_card.setRound(this._round);
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			this.send_response_to_player(play_index, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_SEND_CARD);
		SendCardTxw.Builder send_card = SendCardTxw.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(this.GRR._cards_data[i][j]);
			}
			send_card.addCardCount(this.GRR._card_count[i]);
			send_card.addCardsData(cards_card);
		}
		send_card.setRound(this._round);
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));
		GRR.add_room_response(roomResponse);
	}

	public int get_hand_card_count_max() {
		if (this._round == 1) {
			return DDZConstants.TXW_MAX_COUT - 2;
		}
		return DDZConstants.TXW_MAX_COUT;
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
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * get_hand_card_count_max() + j];
				_init_hand_card[this._round - 1][i][j] = repertory_card[i * get_hand_card_count_max() + j];
			}
			_init_hand_card_count[this._round - 1][i] = get_hand_card_count_max();
			GRR._card_count[i] = get_hand_card_count_max();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);

		}

		if (this.getTablePlayerNumber() == 2) {
			this._di_pai_card_count = 8;
			_di_pai_card_data = new int[_di_pai_card_count];
			for (int i = 0; i < this._di_pai_card_count; i++) {
				this._di_pai_card_data[i] = repertory_card[getTablePlayerNumber() * 8 + i];
			}
		}
		for (int i = 0; i < count; i++) {
			this._logic.remove_cards_by_data(repertory_card, repertory_card.length, GRR._cards_data[i],
					GRR._card_count[i]);
		}

		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void shuffle_next() {
		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = _repertory_card[i * get_hand_card_count_max() + j];
				_init_hand_card[this._round - 1][i][j] = _repertory_card[i * get_hand_card_count_max() + j];
			}
			_init_hand_card_count[this._round - 1][i] = get_hand_card_count_max();
			GRR._card_count[i] = get_hand_card_count_max();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
	}

	private void test_cards() {
		int repertory_card[] = new int[] { 0x03, 0x13, 0x23, 0x04, 0x14, 0x24, 0x05, 0x15, 0x25, 0x06, 0x16, 0x26, 0x3a,
				0x3b, 0x3c, 0x3d, 0x31, 0x07, 0x17, 0x27, 0x08, 0x18, 0x28, 0x09, 0x19, 0x29, 0x0a, 0x1a, 0x2a, 0x35,
				0x36, 0x37, 0x38, 0x39, 0x0b, 0x1b, 0x2b, 0x0c, 0x1c, 0x2c, 0x0d, 0x1d, 0x2d, 0x01, 0x11, 0x21, 0x12,
				0x22, 0x33, 0x34, 0x4E, 0x4F };
		// int cards[] = new int[] { 58, 79, 11, 7, 37, 61, 4, 9, 12, 5, 22, 52,
		// 29, 24, 26, 42, 40, 17, 54, 2, 6, 94, 25,
		// 18, 19, 60, 59, 57, 35, 56, 78, 43, 21, 39, 20, 45, 8, 10, 36, 13, 1,
		// 33, 27, 49, 44, 28, 23, 55, 3, 51,
		// 38, 41, 53 };
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * get_hand_card_count_max() + j];
				_init_hand_card[this._round - 1][i][j] = repertory_card[i * get_hand_card_count_max() + j];
			}
			_init_hand_card_count[this._round - 1][i] = get_hand_card_count_max();
			GRR._card_count[i] = get_hand_card_count_max();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);

		}

		if (this.getTablePlayerNumber() == 2) {
			this._di_pai_card_count = 8;
			_di_pai_card_data = new int[_di_pai_card_count];
			for (int i = 0; i < this._di_pai_card_count; i++) {
				this._di_pai_card_data[i] = repertory_card[getTablePlayerNumber() * 8 + i];
			}
		}
		for (int i = 0; i < count; i++) {
			this._logic.remove_cards_by_data(repertory_card, repertory_card.length, GRR._cards_data[i],
					GRR._card_count[i]);
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > get_no_banker_card_count() + _di_pai_card_count) {
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = _repertory_card[i * get_hand_card_count_max() + j];
				_init_hand_card[this._round - 1][i][j] = _repertory_card[i * get_hand_card_count_max() + j];
			}
			_init_hand_card_count[this._round - 1][i] = get_hand_card_count_max();
			GRR._card_count[i] = get_hand_card_count_max();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);

		}

		if (this.getTablePlayerNumber() == 2) {
			this._di_pai_card_count = 8;
			_di_pai_card_data = new int[_di_pai_card_count];
			for (int i = 0; i < this._di_pai_card_count; i++) {
				this._di_pai_card_data[i] = _repertory_card[getTablePlayerNumber() * 8 + i];
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._logic.remove_cards_by_data(_repertory_card, _repertory_card.length, GRR._cards_data[i],
					GRR._card_count[i]);
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
			for (int j = 0; j < get_no_banker_card_count(); j++) {
				GRR._cards_data[i][j] = cards[k++];
			}
		}
		for (int j = 0; j < _di_pai_card_count; j++) {
			_di_pai_card_data[j] = cards[get_no_banker_card_count() + j];
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	public void load_player_info_data_game_start(GameStart_Txw.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEnd_Txw.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponse_Txw.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = this.newPlayerBaseBuilder(rplayer);
			roomResponse.addPlayers(room_player);
		}
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
		ret = this.handler_game_finish_txw(seat_index, reason);

		return ret;
	}

	public boolean handler_game_finish_txw(int seat_index, int reason) {

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
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEnd_Txw.Builder game_end_txw = PukeGameEnd_Txw.newBuilder();
		game_end_txw.setRoomInfo(this.getRoomInfo());
		game_end_txw.setPlayerNum(count);
		game_end_txw.setGameRound(_game_round);
		game_end_txw.setCurRound(_cur_round);

		boolean end = false;
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {

		}

		if (GRR != null) {
			game_end_txw.setBankerPlayer(GRR._banker_player);// 庄家
			game_end_txw.setBankerPlayer(this.GRR._banker_player);
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			for (int i = 0; i < this._di_pai_card_count; i++) {
				game_end_txw.addDiPaiCardData(this._di_pai_card_data[i]);
			}
			game_end_txw.setDiPaiCount(_di_pai_card_count);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder end_score_info = Int32ArrayResponse.newBuilder();
				for (int x = 0; x < 2; x++) {
					end_score_info.addItem(this._end_score[x][i]);
				}
				end_score_info.addItem(this._end_score[0][i] + this._end_score[1][i]);
				game_end_txw.addEndScore(end_score_info);
				game_end.addGameScore(this._end_score[0][i] + this._end_score[1][i]);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_end_score[0][i] + _end_score[1][i] < 0) {
					_player_info[i]._lose_num++;
				} else if (_end_score[0][i] + _end_score[1][i] > 0) {
					_player_info[i]._win_num++;
				}
				if (_end_score[0][i] + _end_score[1][i] > _player_info[i]._end_score_max) {
					_player_info[i]._end_score_max = _end_score[0][i] + _end_score[1][i];
				}
			}
		} else {
			reason = GameConstants.Game_End_RELEASE_RESULT;
		}

		for (int x = 0; x < this._round; x++) {
			Card_Arrary_Txw.Builder cards_card_array = Card_Arrary_Txw.newBuilder();
			Int32ArrayResponse.Builder cards_count = Int32ArrayResponse.newBuilder();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				cards_count.addItem(this._init_hand_card_count[x][i]);

				for (int j = 0; j < _init_hand_card_count[x][i]; j++) {
					cards_card.addItem(_init_hand_card[x][i][j]);
				}
				cards_card_array.addCardData(cards_card);
			}
			game_end_txw.addCardCount(cards_count);
			game_end_txw.addCardsData(cards_card_array);
		}
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_txw.addAllEndScore((int) _player_result.game_score[i]);
					game_end_txw.addLoseNum(this._player_info[i]._lose_num);
					game_end_txw.addWinNum(this._player_info[i]._win_num);
					game_end_txw.addEndScoreMax(this._player_info[i]._end_score_max);
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
			if (reason != GameConstants.Game_End_RELEASE_RESULT) {
				real_reason = GameConstants.Game_End_RELEASE_PLAY;
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_txw.addAllEndScore((int) _player_result.game_score[i]);
				game_end_txw.addLoseNum(this._player_info[i]._lose_num);
				game_end_txw.addWinNum(this._player_info[i]._win_num);
				game_end_txw.addEndScoreMax(this._player_info[i]._end_score_max);
			}
		}

		this.load_player_info_data_game_end(game_end_txw);
		game_end_txw.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		RoomInfo.Builder room_info_end = getRoomInfo();
		game_end.setRoomInfo(room_info_end);
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_txw));
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

		GRR = null;
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		} else {
			this.schedule(ID_TIMER_AUTO_READY, SheduleArgs.newArgs(), 5000);
		}

		_round = 0;
		// 错误断言
		return false;
	}

	public void cal_score_txw() {
		int win_player = GameConstants.INVALID_SEAT;
		int win_score = 0;
		int end_score[] = new int[this.getTablePlayerNumber()];
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

		}
		if (this.GRR._banker_player != GameConstants.INVALID_SEAT) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (win_player == this.GRR._banker_player) {
					if (i != win_player) {
						if (_is_out[i] == 0) {
							end_score[i] -= (int) (_user_times[i] * this.game_cell);
						}

					} else {
						end_score[i] = (int) (_user_times[i] * this.game_cell);
					}
				} else {
					if (i != this.GRR._banker_player) {
						if (_is_out[i] == 0) {
							end_score[i] = (int) (_user_times[i] * this.game_cell);
						}
					} else {
						end_score[i] -= (int) (_user_times[i] * this.game_cell);
					}
				}
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i != win_player) {
					end_score[i] -= (int) (_user_times[i] * this.game_cell);
					end_score[win_player] += (int) (_user_times[i] * this.game_cell);
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			_end_score[this._round - 1][i] = end_score[i];
			_player_result.game_score[i] += end_score[i];
		}
		this.operate_player_data();

	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_TXW_OPERATE) {
			Opreate_RequestWsk_Txw req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Txw.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType());
		}

		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_CALL_BANKER:
		case GAME_OPREATE_TYPE_CALL_BANKER_NO: {
			if (this._handler_call_banker != null) {
				this._handler_call_banker.handler_call_banker(this, seat_index, opreate_type, -1);
			}
			break;
		}
		case GAME_OPREATE_TYPE_TI_NO:
		case GAME_OPREATE_TYPE_TI:
		case GAME_OPREATE_TYPE_GEN_TI:
		case GAME_OPREATE_TYPE_HUI_TI:
		case GAME_OPREATE_TYPE_ADD_TI: {
			if (this._game_status == GameConstants.GS_DDZ_ADD_TIMES) {
				this._handler_add_times.handler_call_banker(this, seat_index, opreate_type);
			} else {
				this._handler_add_times_again.handler_call_banker(this, seat_index, opreate_type);
			}
			break;
		}

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
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		if (is_cancel) {
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setIsCancelReady(true);
			roomResponse.setOperatePlayer(seat_index);
			send_response_to_room(roomResponse);
			return true;
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

			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}

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

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(istrustee[seat_index]);
		this.send_response_to_room(roomResponse);
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
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, List<Integer> change_list,
			int card_count, int b_out_card) {
		if (get_seat_index != this._current_player) {
			return false;
		}
		if (this._handler != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			int out_change_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
				out_change_cards[i] = change_list.get(i);
			}

			this._handler_out_card_operate.reset_status(get_seat_index, out_cards, out_change_cards, card_count,
					b_out_card);
			this._handler.exe(this);
		}

		return true;
	}

	// 回合结束
	public void send_round_finish(int seat_index) {
		if (seat_index != GameConstants.INVALID_SEAT) {
			cal_score_txw();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_ROUND_END);
		round_end_Txw.Builder round_end = round_end_Txw.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (_round > 1) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards.addItem(GRR._cards_data[i][j]);
				}
			} else {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					cards.addItem(GameConstants.BLACK_CARD);
				}
			}

			round_end.addEndScore(this._end_score[this._round - 1][i]);
			round_end.addCardData(cards);
		}
		if (seat_index != GameConstants.INVALID_SEAT) {
			round_end.setEndType(1);
		} else {
			round_end.setEndType(0);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(round_end));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		_handler_out_card_operate = new DDZHandlerOutCardOperate_TXW();
		this._handler = null;
	}

	public void send_player_times(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_PLAYER_TIMES);
		PlyarTimes.Builder player_times = PlyarTimes.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._is_out[i] == 1) {
				player_times.addTimes(0);
			} else {
				player_times.addTimes(this._user_times[i]);
			}

		}

		roomResponse.setCommResponse(PBUtil.toByteString(player_times));
		if (seat_index == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public boolean exe_call_banker_finish() {
		if (this._handler_add_times != null) {
			this._handler = this._handler_add_times;
			this._handler_add_times.reset_status((this.GRR._banker_player + 1) % this.getTablePlayerNumber());
			this._handler_add_times.exe(this);
			if (this._current_player == this._current_player && istrustee[this._current_player]
					&& this._game_status == GameConstants.GS_DDZ_ADD_TIMES) {
				this._handler_add_times.handler_call_banker(this, this._current_player, 3);
			}
		}
		return true;

	}

	public boolean exe_add_times_finish() {
		int min_hong_tao_card = GameConstants.INVALID_CARD;
		this._current_player = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._is_out[i] == 1) {
				continue;
			}
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (this._logic.get_card_color(GRR._cards_data[i][j]) == 2) {
					if (min_hong_tao_card == GameConstants.INVALID_CARD) {
						this._current_player = i;
						min_hong_tao_card = GRR._cards_data[i][j];
					} else {
						if (this._logic.GetCardLogicValue(GRR._cards_data[i][j]) < this._logic
								.GetCardLogicValue(min_hong_tao_card)) {
							this._current_player = i;
							min_hong_tao_card = GRR._cards_data[i][j];
						}
					}

				}
			}
		}
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		send_callbaner_result();
		if (this._current_player == GameConstants.INVALID_SEAT) {
			if (this._round == 1) {
				this.send_round_finish(GameConstants.INVALID_SEAT);
				this.schedule(this.ID_TIMER_ROUND_FINISH, SheduleArgs.newArgs(), 3000);
			} else {
				int delay = 3;
				this.send_round_finish(GameConstants.INVALID_SEAT);
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), _out_card_player, GameConstants.Game_End_DRAW),
						delay, TimeUnit.SECONDS);
			}

		} else {
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DDZ_CT_ERROR,
					GameConstants.INVALID_SEAT);
			if (this.istrustee[this._current_player]) {
				this.schedule(this.ID_TIMER_AUTO_OPREATE, SheduleArgs.newArgs(), 1000);
			} else {
				this.schedule(this.ID_TIMER_AUTO_TRUESS, SheduleArgs.newArgs(), this._truess_time);
			}
			this._handler = this._handler_out_card_operate;
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

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				OutCardData_Txw.Builder outcarddata = OutCardData_Txw.newBuilder();

				roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_OUT_CARD);// 201
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
						outcarddata.setIsCanYa(1);
					} else {
						if (_logic.SearchOutCard_txw(GRR._cards_data[_current_player], GRR._card_count[_current_player],
								_turn_out_card_data, _turn_out_card_count) == 0) {
							outcarddata.setDisplayTime(3);
							outcarddata.setIsCanYa(0);
						} else {
							outcarddata.setDisplayTime(20);
							outcarddata.setIsCanYa(1);
						}
					}

				} else {
					outcarddata.setDisplayTime(20);
					outcarddata.setIsCanYa(1);
				}
				if (_turn_out_card_count == 0) {
					outcarddata.setIsFirstOut(true);
					outcarddata.setIsCurrentFirstOut(true);
				} else {
					outcarddata.setIsFirstOut(false);
					outcarddata.setIsCurrentFirstOut(false);
				}
				// 刷新玩家手牌数量
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if (i == index) {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards.addItem(GRR._cards_data[index][j]);
						}
					}

					outcarddata.addHandCardsData(cards);
					outcarddata.addHandCardCount(GRR._card_count[i]);
				}

				roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
				this.send_response_to_player(index, roomResponse);
			}

			// 回放
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData_Txw.Builder outcarddata = OutCardData_Txw.newBuilder();

			roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_OUT_CARD);// 201
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

			outcarddata.setIsCanYa(1);
			if (_turn_out_card_count == 0) {
				outcarddata.setIsFirstOut(true);
				outcarddata.setIsCurrentFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
				outcarddata.setIsCurrentFirstOut(false);
			}
			// 刷新玩家手牌数量
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards.addItem(GRR._cards_data[i][j]);
				}
				outcarddata.addHandCardsData(cards);
				outcarddata.addHandCardCount(GRR._card_count[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardData_Txw.Builder outcarddata = OutCardData_Txw.newBuilder();

			roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_OUT_CARD);// 201
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
				if (_turn_out_card_count == 0 || this._current_player != to_player) {
					outcarddata.setDisplayTime(20);
					outcarddata.setIsCanYa(1);
				} else {
					if (_logic.SearchOutCard_txw(GRR._cards_data[_current_player], GRR._card_count[_current_player],
							_turn_out_card_data, _turn_out_card_count) == 0) {
						outcarddata.setDisplayTime(3);
						outcarddata.setIsCanYa(0);
					} else {
						outcarddata.setDisplayTime(20);
						outcarddata.setIsCanYa(1);
					}
				}

			} else {
				outcarddata.setDisplayTime(20);
				outcarddata.setIsCanYa(1);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsFirstOut(true);
				outcarddata.setIsCurrentFirstOut(true);
			} else {
				outcarddata.setIsFirstOut(false);
				outcarddata.setIsCurrentFirstOut(false);
			}
			// 刷新玩家手牌数量
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (i == to_player) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards.addItem(GRR._cards_data[to_player][j]);
					}
				}

				outcarddata.addHandCardsData(cards);
				outcarddata.addHandCardCount(GRR._card_count[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;

	}

	public void out_card_time_finish(int seat_index) {

	}

	public void auto_out_card(int seat_index) {
		int card[] = new int[1];

		int out_card_data[] = new int[this.get_hand_card_count_max()];
		int out_card_count = 0;
		out_card_count = _logic.AiAutoOutCard(GRR._cards_data[seat_index], GRR._card_count[seat_index],
				_turn_out_card_data, this._turn_out_card_count, out_card_data);
		if (out_card_count == 0) {
			this._handler_out_card_operate.reset_status(seat_index, card, 0, DDZConstants.TXW_CT_PASS);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
		} else {
			int card_type = this._logic.GetCardType(out_card_data, out_card_count, out_card_data);
			this._handler_out_card_operate.reset_status(seat_index, out_card_data, out_card_count, card_type);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
		}
	}

	public void send_callbaner_result() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_CALL_BANKER_RESULT);
		CallBankerResult_Txw.Builder call_banker_ddz = CallBankerResult_Txw.newBuilder();
		call_banker_ddz.setBankerPlayer(this.GRR._banker_player);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._is_out[i] == 1) {
				call_banker_ddz.addOutPlayer(i);
			}
			call_banker_ddz.addIsTi(this._add_times[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz));
		send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}

	/*
	 * prv_type 上一个操作类型 1：不叫 2:叫 3：不踢 4：提 5：加提 6：回提 call_player 操作玩家 call_action
	 * 操作动作 cur_player 当前玩家 cur_action 当前可操作动作 to_player 发送玩家
	 */
	public void call_banker_resopnse(int prv_type, int call_player, int call_action, int cur_player, int cur_action[],
			int to_player) {
		if (to_player == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_CALL_BANKER_RESPONSE);
			PlayerControl_Txw.Builder call_banker_ddz = PlayerControl_Txw.newBuilder();
			call_banker_ddz.setPrvType(prv_type);
			call_banker_ddz.setCallPlayer(call_player);
			call_banker_ddz.setCallAction(call_action);
			call_banker_ddz.setCurrentPlayer(cur_player);
			call_banker_ddz.setDisplayTime(10);
			for (int i = 0; i < cur_action.length; i++) {
				call_banker_ddz.addCurrentPlayerCallAction(cur_action[i]);
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				call_banker_ddz.addAllCallAction(this._call_action[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz));
			send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_CALL_BANKER_RESPONSE);
			PlayerControl_Txw.Builder call_banker_ddz = PlayerControl_Txw.newBuilder();
			call_banker_ddz.setCallPlayer(call_player);
			call_banker_ddz.setCallAction(call_action);
			call_banker_ddz.setCurrentPlayer(cur_player);
			call_banker_ddz.setDisplayTime(10);
			for (int i = 0; i < cur_action.length; i++) {
				call_banker_ddz.addCurrentPlayerCallAction(cur_action[i]);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				call_banker_ddz.addAllCallAction(this._call_action[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz));
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	public void call_banker_start() {
		if (this._handler_call_banker != null) {
			this._handler = this._handler_call_banker;
			this._current_player = this._cur_banker;
			this._handler_call_banker.reset_status(this._current_player, _game_status);
			this._handler.exe(this);
			if (istrustee[this._current_player] && this._game_status == GameConstants.GS_CALL_BANKER) {
				this._handler_call_banker.handler_call_banker(this, this._current_player, 1, -1);
			} else {
				schedule(ID_TIMER_AUTO_TRUESS, SheduleArgs.newArgs(), _truess_time);
			}
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

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
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
		if (get_seat_index == GameConstants.INVALID_SEAT) {
			return true;
		}
		if (this._game_status == GameConstants.GS_MJ_FREE || this._game_status == GameConstants.GAME_STATUS_WAIT) {
			this.send_error_notify(get_seat_index, 2, "游戏未开始，不能托管");
			return true;
		}
		istrustee[get_seat_index] = isTrustee;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(istrustee[get_seat_index]);
		this.send_response_to_room(roomResponse);

		if (get_seat_index == this._current_player && istrustee[get_seat_index]
				&& this._game_status == GameConstants.GS_MJ_PLAY) {
			this.auto_out_card(this._current_player);
		} else if (get_seat_index == this._current_player && istrustee[get_seat_index]
				&& this._game_status == GameConstants.GS_CALL_BANKER) {
			this._handler_call_banker.handler_call_banker(this, get_seat_index, 1, -1);
		} else if (get_seat_index == this._current_player && istrustee[get_seat_index]
				&& this._game_status == GameConstants.GS_DDZ_ADD_TIMES) {
			this._handler_add_times.handler_call_banker(this, get_seat_index, 3);
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
		return RoomComonUtil.getMaxNumber(getDescParams());
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

	public void auto_out_card(int seat_index, int card_date[], int card_count) {
		this._handler_out_card_operate.reset_status(seat_index, card_date, card_date, card_count, 1);
		this._handler.exe(this);
	}

	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_START_TO_CALL_BANKER: {
			call_banker_start();
			return;
		}
		case ID_TIMER_CALL_BANKER_TO_ADD_TIME: {
			exe_call_banker_finish();
			return;
		}
		case ID_TIMER_ROUND_FINISH: {
			this.round_reset();
			Send_Card();
			schedule(ID_TIMER_START_TO_CALL_BANKER, SheduleArgs.newArgs(), 1500);
			return;
		}
		case ID_TIMER_ADD_TIME_OUT_OPREATE: {
			exe_add_times_finish();
			return;
		}
		case ID_TIMER_AUTO_OPREATE: {
			if (!this.has_rule(DDZConstants.GAME_RULE_TXW_TRUESS)) {
				return;
			}
			if (this._current_player == GameConstants.INVALID_SEAT) {
				return;
			}
			if (!this.istrustee[this._current_player]) {
				return;
			}
			if (this._game_status == GameConstants.GS_MJ_PLAY) {
				this.auto_out_card(this._current_player);
			} else if (istrustee[this._current_player] && this._game_status == GameConstants.GS_CALL_BANKER) {
				this._handler_call_banker.handler_call_banker(this, this._current_player, 1, -1);
			} else if (this._current_player == this._current_player && istrustee[this._current_player]
					&& this._game_status == GameConstants.GS_DDZ_ADD_TIMES) {
				this._handler_add_times.handler_call_banker(this, this._current_player, 3);
			}
			return;
		}
		case ID_TIMER_AUTO_TRUESS: {
			if (!this.has_rule(DDZConstants.GAME_RULE_TXW_TRUESS)) {
				return;
			}
			if (this._current_player == GameConstants.INVALID_SEAT) {
				return;
			}
			this.handler_request_trustee(this._current_player, true, 1);
			return;
		}
		case ID_TIMER_AUTO_PASS: {
			if (this._current_player == GameConstants.INVALID_SEAT) {
				return;
			}
			this._handler_out_card_operate.reset_status(this._current_player, null, 0, DDZConstants.TXW_CT_PASS);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
			return;
		}
		case ID_TIMER_AUTO_READY: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 0) {
					this.handler_player_ready(i, false);
				}

			}

			return;
		}
		case ID_TIMER_AUTO_ALL_OUT: {
			if (this._current_player == GameConstants.INVALID_SEAT) {
				return;
			}
			this._handler_out_card_operate.reset_status(this._current_player, this.GRR._cards_data[_current_player],
					this.GRR._card_count[_current_player], 1);
			this._handler = this._handler_out_card_operate;
			this._handler.exe(this);
			return;
		}
		}
		return;
	}

}
