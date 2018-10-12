package com.cai.game.shengji.handler.wzdd;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
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
import com.cai.game.shengji.SJGameLogic_Wzdd;
import com.cai.game.shengji.SJTable;
import com.cai.game.shengji.handler.SJHandlerFinish;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.wzdd.wzddRsp.DiPaiCard_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.Effect_type_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.GameStart_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.JiaoZhuBegin_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.LiShiCard_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.MaiDiBegin_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.Opreate_RequestWsk_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.OutCardDataWzdd;
import protobuf.clazz.wzdd.wzddRsp.PukeGameEndWzdd;
import protobuf.clazz.wzdd.wzddRsp.RefreshCardData_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.SendCard_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.TableResponse_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.TableScore_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.Zhu_Pai_Card_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.Zhu_pai_type_Wzdd;

public class SJTable_WZDD extends SJTable {

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

	protected static final int ID_TIMER_OUT_CARD_ROUND = 1;// 出牌一轮出完停留
	protected static final int ID_TIMER_ANIMAL_DELAY = 2;// 动画延迟

	protected static final int EFFECT_SHA = 1;// 杀
	protected static final int EFFECT_GIVEUP = 2;// 弃牌
	protected static final int EFFECT_GET_SCORE = 3;// 捡分
	protected static final int EFFECT_DIAO_ZHU = 4;// 吊住
	public int _cheng_pai_score;// 成牌分数
	public int _po_pai_score;// 破牌分数
	public int _cheng_pai_num[];// 成牌次数
	public int _po_pai_num[];// 破牌次数
	public SJGameLogic_Wzdd _logic;

	public SJTable_WZDD() {
		_logic = new SJGameLogic_Wzdd();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		is_game_start = 0;
		_di_pai_count = 3;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_handler_finish = new SJHandlerFinish();
		this.setMinPlayerCount(2);

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
		_get_score_card = new int[22];
		_di_pai = new int[_di_pai_count];
		_init_di_pai = new int[_di_pai_count];
		_out_card_times = new int[this.getTablePlayerNumber()];
		_cheng_pai_num = new int[getTablePlayerNumber()];
		_out_cards_data = new int[get_hand_card_count_max()];
		_po_pai_num = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_is_call_action[i] = false;
			_select_dang[i] = -1;
			_cur_out_card_type[i] = GameConstants.WZDD_CT_ERROR;
		}
		_handler_call_banker = new SJHandlerCallBanker_WZDD();
		_handler_out_card_operate = new SJHandlerOutCardOperate_WZDD();
		_out_card_player = GameConstants.INVALID_SEAT;

		if (this.has_rule(GameConstants.GAME_RULE_WZDD_CHENG_PO_PAI_ONE)) {
			_cheng_pai_score = 20;
			_po_pai_score = 20;
		} else if (this.has_rule(GameConstants.GAME_RULE_WZDD_CHENG_PO_PAI_TWO)) {
			_cheng_pai_score = 40;
			_po_pai_score = 40;
		} else {
			_cheng_pai_score = 40;
			_po_pai_score = 20;
		}
	}

	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_OUT_CARD_ROUND: {
			int seat_index = args.get("seat_index");
			this._current_player = seat_index;
			operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WZDD_CT_ERROR, GameConstants.INVALID_SEAT);
			return;
		}
		case ID_TIMER_ANIMAL_DELAY: {
			int seat_index = args.get("seat_index");
			int card_count = args.get("card_count");
			int card_type = args.get("card_type");
			int effect_type = args.get("effect_type");
			int card_data[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				card_data[i] = args.get("card_data_" + i);
			}
			send_effect_type(seat_index, effect_type, card_data, card_count, card_type, GameConstants.INVALID_SEAT);
			return;
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
		if (this._cur_round != 1) {
			_cur_banker = (_cur_banker + 1) % this.getTablePlayerNumber();
		}
		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//
		}
	}

	@Override
	public void Reset() {

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_is_call_action[i] = false;
			_select_dang[i] = -1;
			_tou_xiang_agree[i] = -1;
			_cur_out_card_type[i] = GameConstants.WZDD_CT_ERROR;
			_get_score[i] = 0;
		}
		_handler_call_banker.reset_status(GameConstants.INVALID_SEAT, -1);
		_min_dang = 250;
		_out_card_count = 0;
		for (int i = 0; i < _out_card_count; i++) {
			_out_cards_data[i] = GameConstants.INVALID_CARD;
		}
		_max_dang = 500;
		_table_score = 0;
		_is_po_dang = false;
		GRR._left_card_count = this._all_card_len;
		Arrays.fill(_get_score_card, GameConstants.INVALID_CARD);
		_get_score_count = 0;
		_repertory_card = new int[GameConstants.CARD_COUNT_WZDD];
		shuffle(_repertory_card, GameConstants.CARD_DATA_WZDD);

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
		_turn_out_card_type = GameConstants.WZDD_CT_ERROR;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_cur_out_card_data[i], 0);
			_cur_out_card_type[i] = GameConstants.WZDD_CT_ERROR;
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
		_zhu_type = GameConstants.INVALID_CARD;
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
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				_history_out_count[i][j] = 0;
				Arrays.fill(_history_out_card[i][j], GameConstants.INVALID_CARD);
			}
			_out_card_times[i] = 0;
		}

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
		if (this.DEBUG_CARDS_MODE) {
			int cards[] = new int[] { 41, 53, 59, 60, 78, 5, 11, 2, 39, 37, 79, 50, 58, 28, 57, 43, 4, 3, 52, 6, 19, 35, 23, 44, 38, 34, 40, 45, 18,
					26, 54, 8, 56, 9, 29, 24, 10, 42, 22, 7, 21, 55, 61, 12, 20, 1, 13, 33, 49, 25, 27, 36, 51, 17 };
			repertory_card = cards;
		}

		int count = this.getTablePlayerNumber();
		int banker_card = 0x33;
		for (int i = 0; i < _di_pai_count; i++) {
			_di_pai[i] = repertory_card[count * this.get_hand_card_count_init() + i] + 0x100;
			_init_di_pai[i] = repertory_card[count * this.get_hand_card_count_init() + i];
		}
		for (int i = 0; i < _di_pai_count; i++) {
			if (_di_pai[i] % 0x100 == banker_card) {
				banker_card = banker_card - 16;
				i = -1;
			}
		}
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < this.get_hand_card_count_init(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * this.get_hand_card_count_init() + j];
				if (_cur_banker == GameConstants.INVALID_SEAT) {
					if (GRR._cards_data[i][j] == banker_card && this._cur_round == 1) {
						_cur_banker = i;
					}
				}

			}
			GRR._card_count[i] = this.get_hand_card_count_init();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}

		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 43, 34, 19, 7, 17, 11, 5, 35, 40, 4, 45, 8, 38, 42, 41, 54, 51, 58, 50, 12, 59, 3, 78, 28, 44, 2, 52, 6, 33, 23, 37,
				21, 55, 29, 79, 1, 24, 57, 9, 25, 60, 56, 20, 10, 49, 39, 53, 36, 13, 18, 26, 61, 27, 22 };
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
			for (int j = 0; j < get_hand_card_count_init(); j++) {
				GRR._cards_data[i][j] = cards[index++];
			}
			GRR._card_count[i] = get_hand_card_count_init();
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
				if (debug_my_cards.length > get_hand_card_count_init() + _di_pai_count) {
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
		return GameConstants.WZDD_MAX_COUT;
	}

	@Override
	public int get_hand_card_count_init() {
		return get_hand_card_count_max() - 3;
	}

	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_WZDD_SEND_CARD;// 设置状态
		int FlashTime = 4000;
		int standTime = 1000;
		RefreshScore(GameConstants.INVALID_SEAT);
		// 初始化游戏变量
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_Wzdd.Builder gamestart = GameStart_Wzdd.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		gamestart.setCurrentPlayer((this._current_player + this.getTablePlayerNumber() - 1) % getTablePlayerNumber());
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		Send_card();
		// 自己才有牌数据
		return true;
	}

	@Override
	public void Send_card() {
		// 初始化游戏变量
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			// 发送数据
			SendCard_Wzdd.Builder send_card = SendCard_Wzdd.newBuilder();

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
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		SendCard_Wzdd.Builder send_card = SendCard_Wzdd.newBuilder();

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
		_game_status = GameConstants.GS_WZDD_CALL_BANKER;
		// 进入叫庄流程
		this._handler = _handler_call_banker;
		_handler_call_banker.exe(this);
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

	public void load_player_info_data_game_start(GameStart_Wzdd.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_Wzdd.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndWzdd.Builder roomResponse) {
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

	// 发送特效
	public void send_effect_type(int seat_index, int type, int card_data[], int card_count, int card_type, int to_player) {
		// type= 1.牌型 2.杀牌 等等
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_EFFECT_TYPE);
		Effect_type_Wzdd.Builder effect_data = Effect_type_Wzdd.newBuilder();
		effect_data.setSeatIndex(seat_index);
		effect_data.setType(type);
		for (int i = 0; i < card_count; i++) {
			effect_data.addCarddata(card_data[i]);
		}
		effect_data.setCardtype(card_type);
		roomResponse.setCommResponse(PBUtil.toByteString(effect_data));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
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
		int ji_chu_score[] = new int[getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score(end_score, seat_index, ji_chu_score);
		}

		if (GRR != null) {
			this.operate_player_data();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_GAME_END);
		roomResponse.setRoomInfo(getRoomInfo());
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		game_end.setRoomInfo(getRoomInfo());

		load_player_info_data(roomResponse);
		PukeGameEndWzdd.Builder gameend_sj = PukeGameEndWzdd.newBuilder();
		load_player_info_data_game_end(gameend_sj);
		gameend_sj.setRoomInfo(this.getRoomInfo());
		if (GRR != null) {
			if (GRR._banker_player != GameConstants.INVALID_SEAT) {
				if (seat_index == GameConstants.INVALID_SEAT) {
					gameend_sj.setScoreReason(2);
				} else {
					if (this._get_score[GRR._banker_player] >= _select_dang[GRR._banker_player]) {
						gameend_sj.setScoreReason(0);
						gameend_sj.setChengDang((this._get_score[GRR._banker_player] - _select_dang[GRR._banker_player]) / this._cheng_pai_score);

						_cheng_pai_num[GRR._banker_player]++;

					} else {
						gameend_sj.setScoreReason(1);
						gameend_sj.setChengDang((_select_dang[GRR._banker_player] - this._get_score[GRR._banker_player]) / this._po_pai_score);
						_po_pai_num[GRR._banker_player]++;
					}
				}

				gameend_sj.setJiaoFen(_select_dang[GRR._banker_player]);
				gameend_sj.setZhuaFen(this._get_score[GRR._banker_player]);
			} else {
				gameend_sj.setScoreReason(-1);
				gameend_sj.setJiaoFen(0);
				gameend_sj.setZhuaFen(0);
			}

			gameend_sj.setBankerPlayer(GRR._banker_player);
		}

		for (

				int i = 0; i < this._di_pai_count; i++) {
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
				gameend_sj.addJichuScore(ji_chu_score[i]);
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
					gameend_sj.addChengPaiNum(this._cheng_pai_num[i]);
					gameend_sj.addPoPaiNum(this._po_pai_num[i]);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameend_sj.addAllEndScore((int) _player_result.game_score[i]);
				gameend_sj.addChengPaiNum(this._cheng_pai_num[i]);
				gameend_sj.addPoPaiNum(this._po_pai_num[i]);
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

	public void cal_score(int end_score[], int seat_index, int ji_chu_score[]) {
		int times = 1;

		if (seat_index != GameConstants.INVALID_SEAT) {
			int score = (int) game_cell;
			int zhuang_get_score = this._get_score[GRR._banker_player];
			if (_select_dang[GRR._banker_player] >= 320) {
				score *= 2;
			}
			if (zhuang_get_score >= _select_dang[GRR._banker_player]) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] -= score + 2 * ((zhuang_get_score - _select_dang[GRR._banker_player]) / this._cheng_pai_score);
						end_score[GRR._banker_player] += score + 2 * ((zhuang_get_score - _select_dang[GRR._banker_player]) / this._cheng_pai_score);
						ji_chu_score[i] -= score;
						ji_chu_score[GRR._banker_player] += score;
					}
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						end_score[i] += score + 2 * ((_select_dang[GRR._banker_player] - zhuang_get_score) / this._po_pai_score);
						end_score[GRR._banker_player] -= score + 2 * ((_select_dang[GRR._banker_player] - zhuang_get_score) / this._po_pai_score);

						ji_chu_score[i] += score;
						ji_chu_score[GRR._banker_player] -= score;
					}
				}
			}
		} else {
			if (GRR._banker_player != GameConstants.INVALID_SEAT) {
				int score = (int) game_cell;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i != GRR._banker_player) {
						ji_chu_score[i] += score;
						end_score[i] += score;
						end_score[GRR._banker_player] -= score;
						ji_chu_score[GRR._banker_player] -= score;
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
			OutCardDataWzdd.Builder outcarddata = OutCardDataWzdd.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_OUT_CARD);// 201
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
		OutCardDataWzdd.Builder outcarddata = OutCardDataWzdd.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_OUT_CARD);// 201
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

	@Override
	public void send_history(int seat_index) {
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_LI_SHI_CARD);
		LiShiCard_Wzdd.Builder history_card = LiShiCard_Wzdd.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int x = 0; x < this._history_out_count[i][0]; x++) {
				cards_card.addItem(_history_out_card[i][0][x]);
			}
			history_card.addCardData(cards_card);
		}
		history_card.setBankerPlayer(this.GRR._banker_player);
		roomResponse.setCommResponse(PBUtil.toByteString(history_card));
		if (seat_index == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}
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

		if (type == MsgConstants.REQUST_WZDD_OPERATE) {
			Opreate_RequestWsk_Wzdd req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Wzdd.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getSelectDang(), req.getSelectType(), req.getCardsDataList(),
					req.getCardIndex());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int select_dang, int select_type, List<Integer> list, int card_index) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_JIAO_DANG: {
			if (_handler_call_banker != null) {
				this._handler = _handler_call_banker;
				_handler_call_banker.reset_status(seat_index, select_dang);
				_handler_call_banker.handler_call_banker(this, seat_index, select_dang);

			}
			return true;
		}
		case GAME_OPREATE_TYPE_DING_ZHU: {
			deal_ding_zhu(seat_index, select_type);
			return true;
		}
		case GAME_OPREATE_TYPE_TOU_XIANG: {
			deal_tou_xiang(seat_index);
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
		}
		return true;
	}

	public void deal_dipai(int seat_index) {
		if (seat_index != GRR._banker_player) {
			this.send_error_notify(seat_index, 2, "不是庄家不能查看底牌");
			return;
		}
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_DI_PAI_CARD);
		DiPaiCard_Wzdd.Builder di_pai_card = DiPaiCard_Wzdd.newBuilder();
		for (int i = 0; i < this._di_pai_count; i++) {
			di_pai_card.addCardData(this._di_pai[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
		this.send_response_to_player(seat_index, roomResponse);
	}

	public void send_zhu_pai_type(int seat_index) {
		if (_zhu_type == GameConstants.INVALID_CARD) {
			return;
		}
		if (seat_index == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_ZHU_TYPE);
			Zhu_pai_type_Wzdd.Builder di_pai_card = Zhu_pai_type_Wzdd.newBuilder();
			di_pai_card.setZhuType(this._zhu_type);
			roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);

		} else {
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_ZHU_TYPE);
			Zhu_pai_type_Wzdd.Builder di_pai_card = Zhu_pai_type_Wzdd.newBuilder();
			di_pai_card.setZhuType(this._zhu_type);
			roomResponse.setCommResponse(PBUtil.toByteString(di_pai_card));
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void send_ding_zhu_begin(int seat_index) {

		if (seat_index == GameConstants.INVALID_SEAT) {
			_game_status = GameConstants.GS_WZDD_DING_ZHU;
			_current_player = GRR._banker_player;
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				// 发送数据
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WZDD_JIAO_ZHU_BEGIN);
				roomResponse.setRoomInfo(getRoomInfo());
				JiaoZhuBegin_Wzdd.Builder jiaozhu_begin = JiaoZhuBegin_Wzdd.newBuilder();
				if (index == GRR._banker_player) {
					for (int i = 0; i < 4; i++) {
						int count = _logic.GetCardColor_Count(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player], i);
						jiaozhu_begin.addCount(count);
					}
					if (this._select_dang[GRR._banker_player] > 250) {
						jiaozhu_begin.setIsGiveUp(0);
					} else {
						jiaozhu_begin.setIsGiveUp(1);
					}
				}

				jiaozhu_begin.setSeatIndex(GRR._banker_player);
				jiaozhu_begin.setDisplayTime(10);
				roomResponse.setCommResponse(PBUtil.toByteString(jiaozhu_begin));

				this.send_response_to_player(index, roomResponse);
				if (index == GRR._banker_player) {
					GRR.add_room_response(roomResponse);
				}
			}

			_cur_banker = GRR._banker_player;
		} else {
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_JIAO_ZHU_BEGIN);
			roomResponse.setRoomInfo(getRoomInfo());
			JiaoZhuBegin_Wzdd.Builder jiaozhu_begin = JiaoZhuBegin_Wzdd.newBuilder();
			for (int i = 3; i >= 0; i--) {
				int count = _logic.GetCardColor_Count(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player], i);
				jiaozhu_begin.addCount(count);
			}
			if (this._select_dang[GRR._banker_player] > 250) {
				jiaozhu_begin.setIsGiveUp(0);
			} else {
				jiaozhu_begin.setIsGiveUp(1);
			}
			jiaozhu_begin.setSeatIndex(GRR._banker_player);
			jiaozhu_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(jiaozhu_begin));

			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void get_di_pai_card() {
		int card_data[] = new int[GRR._card_count[GRR._banker_player] + _di_pai_count];
		for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
			card_data[i] = GRR._cards_data[GRR._banker_player][i];
		}
		for (int i = 0; i < _di_pai_count; i++) {
			GRR._cards_data[GRR._banker_player][GRR._card_count[GRR._banker_player]++] = _di_pai[i];

		}
		this._logic.SortCardList(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player]);
		this.RefreshCard(GRR._banker_player);
	}

	public void send_mai_di_begin(int seat_index) {
		_game_status = GameConstants.GS_WZDD_MAI_DI;
		// 发送数据
		if (seat_index == GameConstants.INVALID_SEAT) {
			for (int index = 0; index < this.getTablePlayerNumber(); index++) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WZDD_MAI_DI_BEGIN);
				roomResponse.setRoomInfo(this.getRoomInfo());
				MaiDiBegin_Wzdd.Builder mai_pai_begin = MaiDiBegin_Wzdd.newBuilder();

				if (index == GRR._banker_player) {
					for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
						mai_pai_begin.addCardsData(GRR._cards_data[GRR._banker_player][i]);

					}
				}
				mai_pai_begin.setSeatIndex(GRR._banker_player);
				mai_pai_begin.setDisplayTime(10);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_pai_begin));
				this.send_response_to_player(index, roomResponse);
			}
		} else {
			if (seat_index == GRR._banker_player) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WZDD_MAI_DI_BEGIN);
				roomResponse.setRoomInfo(this.getRoomInfo());
				MaiDiBegin_Wzdd.Builder mai_pai_begin = MaiDiBegin_Wzdd.newBuilder();

				if (seat_index == GRR._banker_player) {
					for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
						mai_pai_begin.addCardsData(GRR._cards_data[GRR._banker_player][i]);

					}
				}
				mai_pai_begin.setSeatIndex(GRR._banker_player);
				mai_pai_begin.setDisplayTime(10);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_pai_begin));
				this.send_response_to_player(seat_index, roomResponse);
			} else {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WZDD_MAI_DI_BEGIN);
				roomResponse.setRoomInfo(this.getRoomInfo());
				MaiDiBegin_Wzdd.Builder mai_pai_begin = MaiDiBegin_Wzdd.newBuilder();

				mai_pai_begin.setSeatIndex(GRR._banker_player);
				mai_pai_begin.setDisplayTime(10);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_pai_begin));
				this.send_response_to_player(seat_index, roomResponse);
			}
		}

	}

	public void send_di_pai(int seat_index) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 发送数据
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_WZDD_DI_PAI_CARD);
				DiPaiCard_Wzdd.Builder di_pai_card = DiPaiCard_Wzdd.newBuilder();
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
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_DI_PAI_CARD);
			DiPaiCard_Wzdd.Builder di_pai_card = DiPaiCard_Wzdd.newBuilder();
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
		boolean is_send = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._logic.GetZhu_Count(this.GRR._cards_data[i], this.GRR._card_count[i]) == 0) {
				is_send = true;
				break;
			}
		}
		if (is_send) {
			// 发送数据
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_ZHU_CARD);
			Zhu_Pai_Card_Wzdd.Builder zhu_card = Zhu_Pai_Card_Wzdd.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				int card_data[] = new int[GRR._card_count[i]];
				int card_index[] = new int[GameConstants.WZDD_MAX_INDEX];
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

	public void deal_tou_xiang(int seat_index) {
		if (seat_index != GRR._banker_player || this._game_status != GameConstants.GS_WZDD_DING_ZHU) {
			return;
		}
		if (this._select_dang[GRR._banker_player] > 250) {
			return;
		}
		this._game_status = GameConstants.GS_MJ_WAIT;
		int delay = 3;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
	}

	public void deal_ding_zhu(int seat_index, int select_type) {
		if (seat_index == GRR._banker_player && _zhu_type == GameConstants.INVALID_CARD && this._game_status == GameConstants.GS_WZDD_DING_ZHU) {
			_zhu_type = select_type - 1;
			this._logic._zhu_type = _zhu_type;
			if (_zhu_type >= 0) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						if (_logic.GetCardColor(GRR._cards_data[i][j]) == _zhu_type) {
							if (GRR._cards_data[i][j] < _zhu_value) {
								GRR._cards_data[i][j] += _zhu_value;
							}
						} else if (_logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_one
								|| _logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_two
								|| _logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_three
								|| _logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_four) {
							if (GRR._cards_data[i][j] < _zhu_value) {
								GRR._cards_data[i][j] += _zhu_value;
							}

						}

					}
					this._logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
					RefreshCard(i);
				}
			}

			send_zhu_pai_type(GameConstants.INVALID_SEAT);
			send_mai_di_begin(GameConstants.INVALID_SEAT);

		}
	}

	public void deal_mai_di(int seat_index, List<Integer> list) {
		if (seat_index == GRR._banker_player) {
			int card_count = list.size();
			int di_pai_card[] = new int[card_count];
			if (_di_pai_count != card_count) {
				return;
			}

			for (int i = 0; i < card_count; i++) {
				di_pai_card[i] = list.get(i);
			}
			if (!this._logic.is_mai_di_right(di_pai_card, card_count, GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player])) {
				this.send_error_notify(seat_index, 2, "不能埋分牌");
				return;
			}
			if (!this._logic.remove_cards_by_data(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player], di_pai_card,
					card_count)) {
				return;
			}
			GRR._card_count[GRR._banker_player] -= card_count;
			for (int i = 0; i < card_count; i++) {
				_di_pai[i] = di_pai_card[i];
			}
			this._logic.SortCardList(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player]);
			this.RefreshCard(GRR._banker_player);
			send_di_pai(GameConstants.INVALID_SEAT);
			_game_status = GameConstants.GS_WZDD_PLAY;
			this._current_player = GRR._banker_player;
			this._handler = _handler_out_card_operate;
			this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WZDD_CT_ERROR, GameConstants.INVALID_SEAT);

		}

	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_REFRESH_CARD);
		// 发送数据
		RefreshCardData_Wzdd.Builder refresh_card = RefreshCardData_Wzdd.newBuilder();
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

		GRR.add_room_response(roomResponse);
	}

	@Override
	public void RefreshScore(int to_player) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_REFRESH_SCORE);
		// 发送数据
		TableScore_Wzdd.Builder refresh_score = TableScore_Wzdd.newBuilder();
		int get_score = 0;
		int banker_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player) {
				banker_score += this._get_score[i];
				continue;
			}
			get_score += this._get_score[i];
		}
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int i = 0; i < _get_score_count; i++) {
			cards_card.addItem(_get_score_card[i]);
		}
		refresh_score.setCardData(cards_card);
		refresh_score.setGetScore(banker_score);
		if (GameConstants.INVALID_SEAT == GRR._banker_player) {
			refresh_score.setYuFen(0);
		} else {
			refresh_score.setYuFen(this._select_dang[GRR._banker_player]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_score));
		// 自己才有牌数据
		if (to_player != GameConstants.INVALID_SEAT) {
			this.send_response_to_player(to_player, roomResponse);
		} else {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		}

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
