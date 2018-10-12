package com.cai.game.shengji.handler.xfgd_four;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.shengji.SJTable;
import com.cai.game.shengji.data.tagAnalyseCardType;
import com.cai.game.shengji.handler.SJHandlerFinish;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.xfgd.xfgdRsp.DiPaiCard_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.GameStart_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.MaiDiBegin_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.Opreate_RequestWsk_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.OutCardDataXfgd;
import protobuf.clazz.xfgd.xfgdRsp.PukeGameEndXfgd;
import protobuf.clazz.xfgd.xfgdRsp.RefreshCardData_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.SendCard_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.TableResponse_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.TableScore_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.TouXiangBegin_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.TouXiangResult_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.Zhu_Pai_Card;
import protobuf.clazz.xfgd.xfgdRsp.Zhu_pai_type;

public class SJTable_XFGD_Four extends SJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;
	protected static final int GAME_OPREATE_TYPE_JIAO_DANG = 1;
	protected static final int GAME_OPREATE_TYPE_DING_ZHU = 2;
	protected static final int GAME_OPREATE_TYPE_MAI_DI = 3;
	protected static final int GAME_OPREATE_TYPE_TOU_XIANG = 4;
	protected static final int GAME_OPREATE_TYPE_AGREE = 5;
	protected static final int GAME_OPREATE_TYPE_DIS_AGREE = 6;
	protected static final int GAME_OPREATE_TYPE_DI_PAI = 7;
	protected static final int GAME_OPREATE_TYPE_HISTORY = 8;
	protected static final int GAME_OPREATE_TYPE_QIE_PAI = 9;
	protected static final int ID_TIMER_CLEAR_CARD = 1;// 清理牌

	public int _boom_times;
	public int _boom_limit;
	public int _end_score[];
	public int _out_card_time;

	public SJTable_XFGD_Four() {
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		is_game_start = 0;
		_di_pai_count = 8;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_handler_finish = new SJHandlerFinish();
		this.setMinPlayerCount(2);

		_end_score = new int[getTablePlayerNumber()];
		_is_call_action = new boolean[getTablePlayerNumber()];
		_select_dang = new int[getTablePlayerNumber()];
		_turn_out_card_data = new int[this.get_hand_card_count_max()];
		_turn_out_shuai_pai_data = new int[get_hand_card_count_max()];
		_cur_out_card_data = new int[getTablePlayerNumber()][get_hand_card_count_max()];
		_cur_out_card_count = new int[getTablePlayerNumber()];
		_cur_out_card_type = new int[getTablePlayerNumber()];
		_get_score = new int[getTablePlayerNumber()];
		_history_out_count = new int[getTablePlayerNumber()][get_hand_card_count_max()];
		_tou_xiang_agree = new int[getTablePlayerNumber()];
		_origin_out_card_data = new int[get_hand_card_count_max()];
		_history_out_card = new int[getTablePlayerNumber()][get_hand_card_count_max()][this.get_hand_card_count_max()];
		_out_cards_data = new int[get_hand_card_count_max()];
		_di_pai = new int[_di_pai_count];
		_init_di_pai = new int[_di_pai_count];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_is_call_action[i] = false;
			_select_dang[i] = -1;
			_cur_out_card_type[i] = GameConstants.XFGD_CT_ERROR;
		}
		_handler_call_banker = new SJHandlerCallBanker_XFGD_FOUR();
		_handler_out_card_operate = new SJHandlerOutCardOperate_XFGD_FOUR();
		_out_card_player = GameConstants.INVALID_SEAT;
		if (has_rule(GameConstants.GAME_RULE_XFGD_GAME_CELL_ONE)) {
			this.game_cell = 1;
		} else if (has_rule(GameConstants.GAME_RULE_XFGD_GAME_CELL_TWO)) {
			this.game_cell = 2;
		}
	}

	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_CLEAR_CARD: {
			_kill_ed = GameConstants.INVALID_SEAT;
			_kill_ing = GameConstants.INVALID_SEAT;
			_current_player = _max_card_seat;
			_max_card_seat = GameConstants.INVALID_SEAT;
			operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.XFGD_CT_ERROR, GameConstants.INVALID_SEAT);
			break;
		}
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
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());
			;
		}
		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//
		}
	}

	@Override
	public void Reset() {
		Arrays.fill(_end_score, 0);

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_is_call_action[i] = false;
			_select_dang[i] = -1;
			_tou_xiang_agree[i] = -1;
			_cur_out_card_type[i] = GameConstants.XFGD_CT_ERROR;
			_get_score[i] = 0;
		}
		_min_dang = 1;
		if (has_rule(GameConstants.GAME_RULE_XFGD_CALL_LIMIT_SEVERN)) {
			_max_dang = 7;
		} else if (has_rule(GameConstants.GAME_RULE_XFGD_CALL_LIMIT_EIGHT)) {
			_max_dang = 8;
		} else if (has_rule(GameConstants.GAME_RULE_XFGD_CALL_LIMIT_NINE)) {
			_max_dang = 9;
		} else {
			_max_dang = 10;
		}
		_out_card_count = 0;
		for (int i = 0; i < _out_card_count; i++) {
			_out_cards_data[i] = GameConstants.INVALID_CARD;
		}
		_table_score = 0;
		_is_po_dang = false;
		GRR._left_card_count = this._all_card_len;
		_repertory_card = new int[GameConstants.CARD_COUNT_XFGD];
		shuffle(_repertory_card, GameConstants.CARD_DATA_XFGD);

		_table_score = 0;
		_max_card_seat = GameConstants.INVALID_SEAT;

		for (int i = 0; i < _turn_out_card_count; i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
		}
		for (int i = 0; i < _origin_out_card_count; i++) {
			_origin_out_card_data[i] = GameConstants.INVALID_CARD;
		}
		_turn_out_card_count = 0;
		_origin_out_card_count = 0;
		_turn_out_card_type = GameConstants.XFGD_CT_ERROR;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_cur_out_card_data[i], 0);
			_cur_out_card_type[i] = GameConstants.XFGD_CT_ERROR;
			_cur_out_card_count[i] = 0;
		}
		// 庄家选择
		this.progress_banker_select();
		GRR._banker_player = GameConstants.INVALID_SEAT;
		_prv_call_player = GameConstants.INVALID_SEAT;
		_zhu_type = GameConstants.INVALID_CARD;
		_current_player = _cur_banker;
		_max_card_seat = GameConstants.INVALID_SEAT;
		_is_banker_tou_xiang = -1;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}
		_zhu_type = 4;
		this._logic._zhu_type = _zhu_type;
		reset_init_data();

		Reset();
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
			_out_card_times[i] = 0;
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				_history_out_count[i][j] = 0;
				Arrays.fill(_history_out_card[i][j], GameConstants.INVALID_CARD);
			}
		}

		on_game_start();

		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int card_cards[]) {
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);
		int count = this.getTablePlayerNumber();
		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int double_link_count = 0;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_init(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_init() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_init();
			tagAnalyseCardType card_data_type = new tagAnalyseCardType();
			this._logic.Analyse_card_type_No_Zhu(GRR._cards_data[i], GRR._card_count[i], card_data_type);
			for (int j = 0; j < card_data_type.type_count; j++) {
				if (card_data_type.type[j] == GameConstants.XFGD_CT_DOUBLE_LINK) {
					double_link_count++;
				}
			}
			if (double_link_count > 2) {
				_logic.random_card_data(repertory_card, card_cards);
				break;
			}

			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}

		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_init(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_init() + j];
			}
			GRR._card_count[i] = this.get_hand_card_count_init();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}
		for (int i = 0; i < _di_pai_count; i++) {
			_di_pai[i] = repertory_card[count * this.get_hand_card_count_init() + i] + 0x100;
			_init_di_pai[i] = repertory_card[count * this.get_hand_card_count_init() + i];
		}

		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 28, 13, 7, 3, 37, 26, 1, 2, 17, 59, 6, 51, 12, 58, 52, 6, 33, 59, 53, 42, 33, 49, 57, 79, 21, 10, 41, 43, 27, 38,
				55, 21, 4, 40, 57, 13, 11, 39, 27, 20, 7, 36, 19, 9, 40, 22, 23, 50, 2, 11, 61, 29, 17, 42, 35, 45, 78, 5, 78, 38, 18, 60, 53, 8, 37,
				60, 22, 26, 58, 54, 41, 5, 19, 36, 55, 54, 8, 61, 34, 9, 4, 39, 49, 52, 45, 12, 35, 1, 51, 25, 24, 29, 20, 43, 18, 44, 24, 56, 25, 56,
				3, 28, 79, 10, 44, 50, 23, 34 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < get_hand_card_count_init(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			GRR._card_count[i] = this.get_hand_card_count_init();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				GRR._cards_data[i][j] = cards[index++];

			}
		}
		for (int i = 0; i < _di_pai_count; i++) {
			_di_pai[i] = cards[getTablePlayerNumber() * this.get_hand_card_count_init() + i] + 0x100;
			_init_di_pai[i] = cards[getTablePlayerNumber() * this.get_hand_card_count_init() + i];
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > get_hand_card_count_init()) {
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

	@Override
	public int get_hand_card_count_max() {
		return GameConstants.XFGD_MAX_COUT - 9;
	}

	@Override
	public int get_hand_card_count_init() {
		return get_hand_card_count_max() - 8;
	}

	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_XFGD_SEND_CARD;// 设置状态
		_boom_times = 1;
		int FlashTime = 4000;
		RefreshScore(GameConstants.INVALID_SEAT);
		// 初始化游戏变量
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_Xfgd.Builder gamestart = GameStart_Xfgd.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		gamestart.setCurrentPlayer((this._current_player + this.getTablePlayerNumber() - 1) % getTablePlayerNumber());
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		return true;
	}

	@Override
	public void Send_card() {

		// 初始化游戏变量
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			SendCard_Xfgd.Builder send_card = SendCard_Xfgd.newBuilder();

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
		// 进入叫庄流程
		this._handler = _handler_call_banker;

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		SendCard_Xfgd.Builder send_card = SendCard_Xfgd.newBuilder();

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

		animationSchedule(ID_TIMER_SEND_CARD_TO_CALL_BANKER, 2000);
	}

	public void load_player_info_data_game_start(GameStart_Xfgd.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_Xfgd.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndXfgd.Builder roomResponse) {
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

		int end_score[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL && GRR != null) {
			cal_score(end_score, seat_index);
		} else if (reason == GameConstants.Game_End_NORMAL) {

		}

		if (GRR != null) {
			this.operate_player_data();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_GAME_END);
		roomResponse.setRoomInfo(getRoomInfo());
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		game_end.setRoomInfo(getRoomInfo());

		load_player_info_data(roomResponse);
		PukeGameEndXfgd.Builder gameend_sj = PukeGameEndXfgd.newBuilder();
		load_player_info_data_game_end(gameend_sj);
		gameend_sj.setRoomInfo(this.getRoomInfo());
		int xian_get_score = 0;
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i != GRR._banker_player) {
					xian_get_score += _get_score[i];
				}
			}
			if (GRR._banker_player != GameConstants.INVALID_SEAT) {
				if (seat_index == GameConstants.INVALID_SEAT) {
					gameend_sj.setScoreReason(2);
					gameend_sj.setBeiShu(1);
				} else {
					if (xian_get_score >= ((10 - _select_dang[GRR._banker_player]) * 10 + 5) * 2 && has_rule(GameConstants.GAME_RULE_XFGD_DAOGUANG)) {
						gameend_sj.setScoreReason(3);
						gameend_sj.setBeiShu(2);
					} else if (xian_get_score > (10 - _select_dang[GRR._banker_player]) * 10) {
						if (_select_dang[GRR._banker_player] == 10 && has_rule(GameConstants.GAME_RULE_XFGD_DAOGUANG)) {
							gameend_sj.setScoreReason(3);
							gameend_sj.setBeiShu(2);
						} else {
							gameend_sj.setScoreReason(2);
							gameend_sj.setBeiShu(1);
						}

					} else if (_select_dang[GRR._banker_player] == 10) {
						if (xian_get_score == 0 && this.has_rule(GameConstants.GAME_RULE_XFGD_GUNAGTOU)) {
							gameend_sj.setScoreReason(4);
							gameend_sj.setBeiShu(2);
						} else {
							gameend_sj.setScoreReason(1);
							gameend_sj.setBeiShu(1);
						}
					} else if (xian_get_score == (10 - _select_dang[GRR._banker_player]) * 10) {
						gameend_sj.setScoreReason(0);
						gameend_sj.setBeiShu(1);
					} else {
						if (xian_get_score == 0 && has_rule(GameConstants.GAME_RULE_XFGD_GUNAGTOU)) {
							gameend_sj.setScoreReason(4);
							gameend_sj.setBeiShu(2);
						} else {
							gameend_sj.setScoreReason(1);
							gameend_sj.setBeiShu(1);
						}
					}
				}

				gameend_sj.setJiaoFen(_select_dang[GRR._banker_player]);
			} else {
				gameend_sj.setScoreReason(-1);
				gameend_sj.setBeiShu(1);
				gameend_sj.setJiaoFen(0);
			}
			gameend_sj.setZhuaFen(xian_get_score);
			gameend_sj.setBankerPlayer(GRR._banker_player);
		}

		for (int i = 0; i < this._di_pai_count; i++) {
			gameend_sj.addInitDiPai(this._init_di_pai[i]);
		}
		for (int i = 0; i < this._di_pai_count; i++) {
			gameend_sj.addBankerMaiPai(this._di_pai[i]);
		}

		if (GRR != null) {

			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
				gameend_sj.addCardCount(GRR._card_count[i]);
				gameend_sj.addCardsData(cards_card);
				gameend_sj.addGameScore(end_score[i]);
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end.addGameScore(end_score[i]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					gameend_sj.addAllEndScore((int) _player_result.game_score[i]);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameend_sj.addAllEndScore((int) _player_result.game_score[i]);
			}
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;

		}

		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		gameend_sj.setReason(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(gameend_sj));
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
		} else {
			if (reason == GameConstants.Game_End_DRAW) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this.handler_player_ready(i, false);
				}
			}
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

	public void cal_score(int end_score[], int seat_index) {
		int times = 1;

		if (seat_index != GameConstants.INVALID_SEAT) {
			int score = _select_dang[GRR._banker_player] * (int) game_cell;
			int xian_get_score = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i != GRR._banker_player) {
					xian_get_score += _get_score[i];
				}
			}
			if (xian_get_score >= ((10 - _select_dang[GRR._banker_player]) * 10 + 5) * 2 && has_rule(GameConstants.GAME_RULE_XFGD_DAOGUANG)) {
				score *= 2;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] += score;
						end_score[GRR._banker_player] -= score;
					}
				}
			} else if (xian_get_score > (10 - _select_dang[GRR._banker_player]) * 10) {
				if (_select_dang[GRR._banker_player] == 10 && has_rule(GameConstants.GAME_RULE_XFGD_DAOGUANG)) {
					score *= 2;
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] += score;
						end_score[GRR._banker_player] -= score;
					}
				}
			} else if (xian_get_score != (10 - _select_dang[GRR._banker_player]) * 10) {
				if (xian_get_score == 0 && this.has_rule(GameConstants.GAME_RULE_XFGD_GUNAGTOU)) {
					score *= 2;
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] -= score;
						end_score[GRR._banker_player] += score;
					}
				}
			} else if (_select_dang[GRR._banker_player] == 10) {
				if (xian_get_score == 0 && this.has_rule(GameConstants.GAME_RULE_XFGD_GUNAGTOU)) {
					score *= 2;
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] -= score;
						end_score[GRR._banker_player] += score;
					}
				}
			}
		} else {
			if (GRR != null) {
				int score = _select_dang[GRR._banker_player] * (int) game_cell;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] += score;
						end_score[GRR._banker_player] -= score;
					}
				}
			}

		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += end_score[i];
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			OutCardDataXfgd.Builder outcarddata = OutCardDataXfgd.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_OUT_CARD);// 201
			roomResponse.setTarget(seat_index);
			roomResponse.setRoomInfo(this.getRoomInfo());

			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._origin_out_card_data[i]);
			}
			for (int i = 0; i < this._turn_out_shuai_pai_count; i++) {
				outcarddata.addCardDataFailure(this._turn_out_shuai_pai_data[i]);
			}
			outcarddata.setCardsCount(count);
			outcarddata.setOutCardPlayer(seat_index);
			outcarddata.setCardType(type);
			outcarddata.setCurPlayer(this._current_player);
			outcarddata.setDisplayTime(10);
			outcarddata.setPrOutCardType(_turn_out_card_type);
			outcarddata.setMaxValueSeat(_max_card_seat);
			outcarddata.setKilledSeat(this._kill_ed);
			outcarddata.setKillingSeat(this._kill_ing);
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);
			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (index == _current_player) {
				int can_out_card_data[] = new int[get_hand_card_count_max()];
				int can_out_count = _logic.Player_Can_out_card(GRR._cards_data[_current_player], GRR._card_count[_current_player],
						_origin_out_card_data, _origin_out_card_count, can_out_card_data);
				for (int i = 0; i < can_out_count; i++) {
					outcarddata.addUserCanOutData(can_out_card_data[i]);
				}
				outcarddata.setUserCanOutCount(can_out_count);
			}
			if (this._max_card_seat == GameConstants.INVALID_SEAT) {
				outcarddata.setIsround(true);
			} else {
				outcarddata.setIsround(false);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == index) {
					for (int j = 0; j < this.GRR._card_count[i]; j++) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}
				}
				outcarddata.addHandCardData(cards_card);
				outcarddata.addHandCardCount(GRR._card_count[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));

			this.send_response_to_player(index, roomResponse);

		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		OutCardDataXfgd.Builder outcarddata = OutCardDataXfgd.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_OUT_CARD);// 201
		roomResponse.setTarget(seat_index);
		roomResponse.setRoomInfo(this.getRoomInfo());

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
		outcarddata.setMaxValueSeat(_max_card_seat);
		outcarddata.setKilledSeat(this._kill_ed);
		outcarddata.setKillingSeat(this._kill_ing);
		if (_turn_out_card_count == 0) {
			outcarddata.setIsCurrentFirstOut(1);
		} else {
			outcarddata.setIsCurrentFirstOut(0);
		}
		if (this._max_card_seat == GameConstants.INVALID_SEAT) {
			outcarddata.setIsround(true);
		} else {
			outcarddata.setIsround(false);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards_card.addItem(this.GRR._cards_data[i][j]);
			}
			outcarddata.addHandCardData(cards_card);
			outcarddata.addHandCardCount(GRR._card_count[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(outcarddata));
		GRR.add_room_response(roomResponse);
		return true;
	}

	public void auto_out_card(int seat_index, int cards[], int card_count, int out_type) {
		this._handler_out_card_operate.reset_status(seat_index, cards, card_count, out_type);
		this._handler_out_card_operate.exe(this);
	}

	@Override
	public boolean animation_timer(int timer_id) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_TIMER_START_TO_SEND_CARD: {
			this.Send_card();
			return true;
		}
		case ID_TIMER_SEND_CARD_TO_CALL_BANKER: {
			_handler_call_banker.exe(this);
			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		super.handler_request_trustee(get_seat_index, isTrustee, Trustee_type);

		return true;
	}

	// 发送扑克
	public boolean exe_dispatch_card(int seat_index, int send_count, boolean tail, int dely) {

		GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, send_count, tail), dely, TimeUnit.MILLISECONDS);

		return true;
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

		if (type == MsgConstants.REQUST_SJ_SFGD_OPERATE) {
			Opreate_RequestWsk_Xfgd req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Xfgd.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getSelectDang(), req.getSelectType(), req.getCardsDataList(),
					req.getCardIndex());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int select_dang, int select_type, List<Integer> list, int card_index) {
		if (GRR == null) {
			return false;
		}
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_JIAO_DANG: {
			if (_handler_call_banker != null) {
				this._handler = _handler_call_banker;
				_handler_call_banker.handler_call_banker(this, seat_index, select_dang);

			}
			return true;
		}
		case GAME_OPREATE_TYPE_DING_ZHU: {
			deal_ding_zhu(seat_index, select_type);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG: {
			deal_tou_xiang_result();
			return true;
		}
		case GAME_OPREATE_TYPE_AGREE: {
			deal_tou_agree(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_DIS_AGREE: {
			deal_tou_disagree(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_MAI_DI: {
			deal_mai_di(seat_index, list);
			return true;
		}
		case GAME_OPREATE_TYPE_DI_PAI: {
			deal_dipai(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_HISTORY: {
			send_history(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_QIE_PAI: {
			QiePaiEnd(seat_index, card_index);
			return true;
		}
		}
		return true;
	}

	public void deal_tou_disagree(int seat_index) {
		if (!has_rule(GameConstants.GAME_RULE_XFGD_TOU_XIANG)) {
			return;
		}
		if (!this._is_po_dang) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				continue;
			}
			if (this._tou_xiang_agree[i] == 0) {
				return;
			}
		}
		_tou_xiang_agree[seat_index] = 0;
		send_tou_xiang_result(GameConstants.INVALID_CARD);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i != seat_index) {
				this.send_error_notify(i, 2, this.get_players()[seat_index].getNick_name() + "不同意投降");
			}

		}

	}

	public void deal_tou_agree(int seat_index) {
		if (!has_rule(GameConstants.GAME_RULE_XFGD_TOU_XIANG)) {
			return;
		}
		if (!this._is_po_dang) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				continue;
			}
			if (this._tou_xiang_agree[i] == 0) {
				return;
			}
		}
		_tou_xiang_agree[seat_index] = 1;
		send_tou_xiang_result(GameConstants.INVALID_CARD);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				continue;
			}
			if (this._tou_xiang_agree[i] == 0 || this._tou_xiang_agree[i] == -1) {
				return;
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_TOU_XIANG_ANIMAL);
		this.send_response_to_room(roomResponse);
		int delay = 3;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
	}

	public void deal_tou_xiang_result() {
		if (!has_rule(GameConstants.GAME_RULE_XFGD_TOU_XIANG)) {
			return;
		}
		if (!this._is_po_dang) {
			if (_game_status == GameConstants.GS_XFGD_MAI_DI) {
				_game_status = GameConstants.GS_MJ_WAIT;
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_XFGD_TOU_XIANG_ANIMAL);
				this.send_response_to_room(roomResponse);

				int delay = 3;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), delay,
						TimeUnit.SECONDS);
				return;
			} else {
				return;
			}
		}
		if (_game_status != GameConstants.GS_XFGD_PLAY || GRR == null || _is_banker_tou_xiang == 1) {
			return;
		}
		_is_banker_tou_xiang = 1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				continue;
			}
			if (this._tou_xiang_agree[i] == 0) {
				return;
			}
		}
		send_tou_xiang_result(GameConstants.INVALID_CARD);
	}

	public void send_tou_xiang_result(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_TOU_XIANG);
		TouXiangResult_Xfgd.Builder tou_xiang_result = TouXiangResult_Xfgd.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				tou_xiang_result.setTouXiangSeat(i);
				tou_xiang_result.addIsAgree(_tou_xiang_agree[i]);
			} else {
				tou_xiang_result.addIsAgree(_tou_xiang_agree[i]);
			}
		}
		tou_xiang_result.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));

		if (seat_index == GameConstants.INVALID_CARD) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void deal_dipai(int seat_index) {
		if (seat_index != GRR._banker_player) {
			this.send_error_notify(seat_index, 2, "不是庄家不能查看底牌");
			return;
		}
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_DI_PAI_CARD);
		DiPaiCard_Xfgd.Builder di_pai_card = DiPaiCard_Xfgd.newBuilder();
		for (int i = 0; i < this._di_pai_count; i++) {
			di_pai_card.addCardData(this._di_pai[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
		this.send_response_to_player(seat_index, roomResponse);
	}

	public void send_zhu_pai_type(int seat_index) {
		if (seat_index == GameConstants.INVALID_CARD) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_ZHU_TYPE);
			Zhu_pai_type.Builder di_pai_card = Zhu_pai_type.newBuilder();
			di_pai_card.setZhuType(this._zhu_type);
			roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_ZHU_TYPE);
			Zhu_pai_type.Builder di_pai_card = Zhu_pai_type.newBuilder();
			di_pai_card.setZhuType(this._zhu_type);
			roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void send_di_pai(int seat_index) {
		if (seat_index == GameConstants.INVALID_CARD) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 发送数据
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_XFGD_DI_PAI_CARD);
				DiPaiCard_Xfgd.Builder di_pai_card = DiPaiCard_Xfgd.newBuilder();
				if (i == GRR._banker_player) {
					for (int j = 0; j < this._di_pai_count; j++) {
						di_pai_card.addCardData(this._di_pai[j]);
					}
				}
				roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
				this.send_response_to_player(i, roomResponse);
				if (i == GRR._banker_player) {
					GRR.add_room_response(roomResponse);
				}
			}
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_DI_PAI_CARD);
			DiPaiCard_Xfgd.Builder di_pai_card = DiPaiCard_Xfgd.newBuilder();
			if (seat_index == GRR._banker_player) {
				for (int j = 0; j < this._di_pai_count; j++) {
					di_pai_card.addCardData(this._di_pai[j]);
				}
			}
			roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void send_zhu_count(int seat_index) {
		if (this.has_rule(GameConstants.GAME_RULE_XFGD_BAO_ZHU_NO)) {
			return;
		}
		boolean is_send = false;
		int have_not_zhu_num = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._logic.GetZhu_Count(this.GRR._cards_data[i], this.GRR._card_count[i]) == 0) {
				have_not_zhu_num++;
			}
		}
		if (have_not_zhu_num > 1) {
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_ZHU_CARD);
			Zhu_Pai_Card.Builder zhu_card = Zhu_Pai_Card.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int card_data[] = new int[GRR._card_count[i]];
				int card_index[] = new int[GameConstants.XFGD_MAX_INDEX];
				int count = 0;
				int double_count = 0;
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					if (this.GRR._cards_data[i][j] > this._zhu_value) {
						card_data[count++] = GRR._cards_data[i][j];
					}
				}
				double_count = this._logic.get_card_double(card_data, count);
				zhu_card.setZhuType(this._zhu_type);
				zhu_card.addZhuCardCount(count);
				zhu_card.addZhuCardDoubleCount(double_count);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(zhu_card));
			if (seat_index == GameConstants.INVALID_SEAT) {
				this.send_response_to_room(roomResponse);
			} else {
				this.send_response_to_player(seat_index, roomResponse);
			}

		}

	}

	public void deal_ding_zhu(int seat_index, int select_type) {
		if (select_type < 1 || select_type > 4) {
			return;
		}
		if (seat_index == GRR._banker_player && _zhu_type == GameConstants.INVALID_CARD) {
			_zhu_type = select_type - 1;
			this._logic._zhu_type = _zhu_type;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.GetCardColor(GRR._cards_data[i][j]) == _zhu_type) {
						GRR._cards_data[i][j] += _zhu_value;
					} else if (_logic.GetCardValue(GRR._cards_data[i][j]) == 2) {
						GRR._cards_data[i][j] += _fu_2_value;
					} else {
						if (_logic.GetCardValue(GRR._cards_data[i][j]) == 14 || _logic.GetCardValue(GRR._cards_data[i][j]) == 15) {
							GRR._cards_data[i][j] += _zhu_value;
						}
					}

				}
				this._logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
				RefreshCard(i);
			}

			_game_status = GameConstants.GS_XFGD_MAI_DI;
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_MAI_DI_BEGIN);
			roomResponse.setRoomInfo(this.getRoomInfo());
			MaiDiBegin_Xfgd.Builder mai_pai_begin = MaiDiBegin_Xfgd.newBuilder();

			int card_data[] = new int[GRR._card_count[GRR._banker_player] + _di_pai_count];
			for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
				card_data[i] = GRR._cards_data[GRR._banker_player][i];
			}
			for (int i = 0; i < _di_pai_count; i++) {
				if (_logic.GetCardColor(_di_pai[i]) == _zhu_type) {
					_di_pai[i] += _zhu_value;
				} else if (_logic.GetCardValue(_di_pai[i]) == 2) {
					_di_pai[i] += _fu_2_value;
				} else if (_logic.GetCardValue(_di_pai[i]) == 14 || _logic.GetCardValue(_di_pai[i]) == 15) {
					_di_pai[i] += _zhu_value;
				}
				card_data[GRR._card_count[GRR._banker_player] + i] = _di_pai[i];
			}
			this._logic.SortCardList(card_data, GRR._card_count[GRR._banker_player] + _di_pai_count);
			for (int i = 0; i < GRR._card_count[GRR._banker_player] + _di_pai_count; i++) {
				mai_pai_begin.addCardsData(card_data[i]);
			}
			mai_pai_begin.setSeatIndex(GRR._banker_player);
			mai_pai_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(mai_pai_begin));

			send_response_to_room(roomResponse);

			send_zhu_pai_type(GameConstants.INVALID_CARD);
		}
	}

	public void deal_mai_di(int seat_index, List<Integer> list) {
		if (seat_index == GRR._banker_player && _zhu_type != GameConstants.INVALID_CARD) {
			int card_count = list.size();
			int di_cards[] = new int[card_count];

			if (_di_pai_count != card_count) {
				return;
			}
			int card_all_count = GRR._card_count[GRR._banker_player] + _di_pai_count;
			int card_data[] = new int[card_all_count];
			for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
				card_data[i] = GRR._cards_data[GRR._banker_player][i];
			}
			for (int i = 0; i < _di_pai_count; i++) {
				card_data[GRR._card_count[GRR._banker_player] + i] = _di_pai[i];
			}
			for (int i = 0; i < card_count; i++) {
				_di_pai[i] = list.get(i);
				for (int j = 0; j < card_all_count; j++) {
					if (card_data[j] == _di_pai[i]) {
						card_data[j] = 0;
						break;
					}
				}
			}
			int num = 0;
			for (int i = 0; i < card_all_count; i++) {
				if (card_data[i] != 0) {
					GRR._cards_data[GRR._banker_player][num++] = card_data[i];
				}
			}
			this._logic.SortCardList(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player]);
			this.RefreshCard(GRR._banker_player);
			_game_status = GameConstants.GS_XFGD_PLAY;
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.XFGD_CT_ERROR, GameConstants.INVALID_SEAT);
		}

		send_di_pai(GameConstants.INVALID_CARD);
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_REFRESH_CARD);
		// 发送数据
		RefreshCardData_Xfgd.Builder refresh_card = RefreshCardData_Xfgd.newBuilder();
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

		// 回放
		refresh_card.clear();
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
		GRR.add_room_response(roomResponse);
	}

	@Override
	public void RefreshScore(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_REFRESH_SCORE);
		// 发送数据
		TableScore_Xfgd.Builder refresh_score = TableScore_Xfgd.newBuilder();
		int get_score = 0;
		int yu_score = 200;
		int banker_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				banker_score += this._get_score[i];
				continue;
			}
			get_score += this._get_score[i];
		}
		yu_score -= get_score + banker_score;
		if (yu_score < 0) {
			yu_score = 0;
		}
		refresh_score.setGetScore(get_score);
		refresh_score.setYuFen(yu_score);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_score));
		// 自己才有牌数据
		if (to_player != GameConstants.INVALID_SEAT) {
			this.send_response_to_player(to_player, roomResponse);

		} else {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		}

	}

	public void tou_xiang_begin() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_TOU_XIANG_BEGIN);
		// 发送数据
		TouXiangBegin_Xfgd.Builder tou_xiang_begin = TouXiangBegin_Xfgd.newBuilder();
		tou_xiang_begin.setDeatil("分数已破挡是否投降！");
		tou_xiang_begin.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_begin));
		// 自己才有牌数据
		this.send_response_to_player(GRR._banker_player, roomResponse);
		GRR.add_room_response(roomResponse);
	}

	public boolean handler_operate_call_banker(int get_seat_index, int call_action) {

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
