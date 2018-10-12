package com.cai.game.shengji.handler.lldq;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.shengji.SJGameLogic_Lldq;
import com.cai.game.shengji.SJTable;
import com.cai.game.shengji.handler.SJHandlerFinish;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lldq.lldqRsp.CallBankerResponse_Lldq;
import protobuf.clazz.lldq.lldqRsp.DiPaiCard_Lldq;
import protobuf.clazz.lldq.lldqRsp.GameStart_Lldq;
import protobuf.clazz.lldq.lldqRsp.MaiDiBegin_Lldq;
import protobuf.clazz.lldq.lldqRsp.MaiDiOpreate_Lldq;
import protobuf.clazz.lldq.lldqRsp.Opreate_RequestWsk_lldq;
import protobuf.clazz.lldq.lldqRsp.OutCardDataLldq;
import protobuf.clazz.lldq.lldqRsp.PukeGameEndLldq;
import protobuf.clazz.lldq.lldqRsp.RefreshCardData_Lldq;
import protobuf.clazz.lldq.lldqRsp.SendCardEnd_Lldq;
import protobuf.clazz.lldq.lldqRsp.SendCard_Lldq;
import protobuf.clazz.lldq.lldqRsp.TableScore_Lldq;
import protobuf.clazz.lldq.lldqRsp.Zhu_Count_Lldq;
import protobuf.clazz.lldq.lldqRsp.Zhu_card_Data;
import protobuf.clazz.lldq.lldqRsp.effect_type;
import protobuf.clazz.lldq.lldqRsp.sound_type;
import protobuf.clazz.xfgd.xfgdRsp.GameStart_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.OutCardDataXfgd;
import protobuf.clazz.xfgd.xfgdRsp.TableResponse_Xfgd;

public class SJTable_LLDQ extends SJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 1;
	protected static final int GAME_OPREATE_TYPE_OUT_XIANG = 2;
	protected static final int GAME_OPREATE_TYPE_GET_DI_CARD = 3;
	protected static final int GAME_OPREATE_TYPE_PUT_DI_CARD = 4;

	protected static final int ID_TIMER_SEND_CARD_SEND = 1;// 发牌间隔
	protected static final int ID_TIMER_SEND_CARD_END_WAIT = 2;// 发牌结束到等待
	protected static final int ID_TIMER_ANIMAL_DELAY = 3;// 发牌结束到等待
	protected static final int ID_TIMER_LIANG_ZHU_TO_MAI_DI = 4;// 亮主到埋牌

	// 特效
	protected static final int EFFECT_DI_PAI_FEI = 1;// 飞底牌
	protected static final int EFFECT_GONG_TOU = 2;// 工头
	protected static final int EFFECT_GONG_YOU = 3;// 工友
	protected static final int EFFECT_DIAN_PAI = 4;// 垫牌
	protected static final int EFFECT_BI_PAI = 5;// 毙牌
	protected static final int EFFECT_DA_ZHU = 6;// 打住
	protected static final int EFFECT_DIAO_ZHU = 7;// 钓主
	protected static final int EFFECT_SCORE_FEI = 8;// 飞分数
	protected static final int EFFECT_FAN_PAI_RED = 9;// 翻牌变红
	protected static final int EFFECT_FAN_PAI_WHITE = 10;// 翻牌变白
	protected static final int EFFECT_DI_PAI_YIN_CANG = 17;// 底牌隐藏
	protected static final int EFFECT_LIANG_ZHU_OR_FAN_PAI = 12;// 请选择亮主或翻牌
	protected static final int EFFECT_WAIT_SHOU_QI = 13;// 等待玩家亮首七
	protected static final int EFFECT_SELECT_DI_PAI = 14;// 请选择底牌
	protected static final int EFFECT_CLICK_AGAIN = 15;// 再次点击双亮
	protected static final int EFFECT_CALL_YIN_CANG = 16;// 叫主面板隐藏
	protected static final int EFFECT_GAME_END_XIAO_GUANG = 18;// 小光
	protected static final int EFFECT_GAME_END_DA_GUANG = 19;// 大光
	protected static final int EFFECT_GAME_END_MAN_GUAN = 20;// 满贯
	// 音效
	protected static final int SOUND_DI_PAI = 1;// 扣底牌
	protected static final int SOUND_OUT_CARD = 2;// 出牌
	public int _call_baker_data[];
	public int _call_banker_card_count;
	public int _first_call_banker;
	public int _end_score[];
	public int _game_type;
	public int _fan_pai_card_index;
	public int _fan_pai_seat_index;
	public boolean is_select_fan_pai;
	public int _kou_di_score;
	public int _kou_di_fan_bei;
	public int _kou_di_jia_ji;
	public boolean _is_shou_qi;
	public int win_total_times[];

	public SJTable_LLDQ() {
		_logic = new SJGameLogic_Lldq();
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
		_di_pai = new int[_di_pai_count];
		_init_di_pai = new int[_di_pai_count];
		_out_card_times = new int[this.getTablePlayerNumber()];
		_call_baker_data = new int[getTablePlayerNumber()];
		_out_cards_data = new int[get_hand_card_count_max()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_is_call_action[i] = false;
			_select_dang[i] = -1;
			_cur_out_card_type[i] = GameConstants.XFGD_CT_ERROR;
		}
		_handler_out_card_operate = new SJHandlerOutCardOperate_LLDQ();
		this._handler = this._handler_out_card_operate;
		_out_card_player = GameConstants.INVALID_SEAT;
		_first_call_banker = GameConstants.INVALID_SEAT;
		win_total_times = new int[getTablePlayerNumber()];

		_player_ready = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			win_total_times[i] = 0;
		}

		if (this.has_rule(GameConstants.GAME_RULE_LLDQ_THREE_JI_LIMIT)) {
			this._max_end_socre_limit = 3;
		} else {
			_max_end_socre_limit = 5;
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
			GRR._card_count[i] = 0;
		}
		_table_score = 0;
		_kou_di_score = 0;
		_kou_di_fan_bei = 0;
		_kou_di_jia_ji = 0;
		_is_shou_qi = false;
		for (int i = 0; i < _out_card_count; i++) {
			_out_cards_data[i] = GameConstants.INVALID_CARD;
		}
		_all_card_len = GameConstants.CARD_COUNT_LLDQ;
		GRR._left_card_count = this._all_card_len;
		if (this.has_rule(GameConstants.GAME_RULE_LLDQ_FAN_PAI_LIANG_ZHU)) {
			_fan_pai_card_index = (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (_all_card_len - this._di_pai_count));
			if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
				_fan_pai_card_index = 10;
		}

		_fan_pai_seat_index = GameConstants.INVALID_SEAT;
		_other_banker = GameConstants.INVALID_SEAT;

		_repertory_card = new int[GameConstants.CARD_COUNT_LLDQ];
		shuffle(_repertory_card, GameConstants.CARD_DATA_LLDQ);

		_table_score = 0;
		_max_card_seat = GameConstants.INVALID_SEAT;
		_first_call_banker = GameConstants.INVALID_SEAT;

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
		this._cur_banker = GameConstants.INVALID_SEAT;
		GRR._banker_player = GameConstants.INVALID_SEAT;
		_prv_call_player = GameConstants.INVALID_SEAT;
		_zhu_type = GameConstants.INVALID_CARD;
		Arrays.fill(_call_baker_data, GameConstants.INVALID_CARD);
		_call_banker_card_count = 0;
		_current_player = _cur_banker;
		_max_card_seat = GameConstants.INVALID_SEAT;
		_is_banker_tou_xiang = -1;
		is_select_fan_pai = false;
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

		int count = this.getTablePlayerNumber();
		// for (int i = 0; i < count; i++) {
		// for (int j = 0; j < this.get_hand_card_count_init(); j++) {
		// GRR._cards_data[i][j] = repertory_card[i *
		// this.get_hand_card_count_init() + j];
		// if (_logic.GetCardColor(GRR._cards_data[i][j]) == 4) {
		// GRR._cards_data[i][j] += _zhu_value;
		// }
		// }
		// GRR._card_count[i] = this.get_hand_card_count_init();
		// _logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		// }
		for (int i = 0; i < _di_pai_count; i++) {
			_di_pai[i] = repertory_card[count * this.get_hand_card_count_init() + i] + 0x100;
			_init_di_pai[i] = repertory_card[count * this.get_hand_card_count_init() + i];
		}

		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 28, 35, 59, 56, 9, 6, 29, 22, 27, 23, 38, 36, 54, 53, 58, 43, 45, 25, 5, 17, 4, 22, 34, 21, 24, 36, 8, 33, 12, 2,
				55, 11, 3, 18, 5, 18, 50, 61, 28, 79, 11, 50, 52, 23, 21, 61, 34, 19, 24, 35, 41, 20, 13, 6, 60, 13, 4, 38, 59, 42, 57, 39, 33, 20,
				79, 1, 58, 26, 42, 27, 39, 10, 44, 9, 55, 53, 78, 19, 37, 3, 56, 60, 43, 44, 54, 7, 41, 57, 29, 51, 40, 49, 1, 10, 78, 40, 2, 7, 26,
				25, 52, 45, 37, 12, 17, 51, 8, 49 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		this._repertory_card = cards;
		for (int i = 0; i < _di_pai_count; i++) {
			_di_pai[i] = _repertory_card[getTablePlayerNumber() * this.get_hand_card_count_init() + i] + 0x100;
			_init_di_pai[i] = _repertory_card[getTablePlayerNumber() * this.get_hand_card_count_init() + i];
		}
		// for (int i = 0; i < _di_pai_count; i++) {
		// _di_pai[i] = cards[getTablePlayerNumber() *
		// this.get_hand_card_count_init() + i] + 0x100;
		// _init_di_pai[i] = cards[getTablePlayerNumber() *
		// this.get_hand_card_count_init() + i];
		// }
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
		return GameConstants.LLDQ_MAX_COUT;
	}

	@Override
	public int get_hand_card_count_init() {
		return get_hand_card_count_max() - 8;
	}

	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_LLDQ_SEND_CARD;// 设置状态
		_game_type = GameConstants.GS_LLDQ_SEND_CARD;
		int FlashTime = 4000;
		int standTime = 1000;
		// RefreshScore(GameConstants.INVALID_SEAT, 0);
		// 初始化游戏变量
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_Lldq.Builder gamestart = GameStart_Lldq.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		if (this.has_rule(GameConstants.GAME_RULE_LLDQ_FAN_PAI_LIANG_ZHU)) {
			this.send_effect_type(GameConstants.INVALID_SEAT, EFFECT_FAN_PAI_RED, _repertory_card[_fan_pai_card_index], GameConstants.INVALID_SEAT);
		}

		if (this.has_rule(GameConstants.GAME_RULE_LLDQ_SHOU_QI)) {
			send_card(1);
		} else {
			send_card(2);
		}

		return true;
	}

	void send_card(int type) {
		// 发牌
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		int cards_data[][] = new int[getTablePlayerNumber()][1];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 1; j++) {
				if (this.has_rule(GameConstants.GAME_RULE_LLDQ_FAN_PAI_LIANG_ZHU)) {
					if (this._fan_pai_card_index == _all_card_len - GRR._left_card_count) {
						_fan_pai_seat_index = i;
						this.send_effect_type(i, EFFECT_FAN_PAI_RED, _repertory_card[_all_card_len - GRR._left_card_count],
								GameConstants.INVALID_SEAT);
					}
				}

				cards_data[i][j] = _repertory_card[_all_card_len - GRR._left_card_count];
				GRR._left_card_count--;

				GRR._cards_data[i][GRR._card_count[i]] = cards_data[i][j];
				GRR._card_count[i]++;
				this._logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
			}

		}
		for (int index = 0; index < getTablePlayerNumber(); index++) {
			roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			SendCard_Lldq.Builder send_card = SendCard_Lldq.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();
				if (index == i) {
					cards_card.addItem(cards_data[i][0]);
				}

				if (index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {

						hand_cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				send_card.addCardsData(cards_card);
				send_card.addCardCount(1);
				send_card.addHandCardsData(hand_cards_card);
				send_card.addHandCardCount(GRR._card_count[i]);
			}
			// 发送数据
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			// 自己才有牌数据
			this.send_response_to_player(index, roomResponse);
			Refresh_Color_count(index, false);
		}

		// 回放
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		SendCard_Lldq.Builder send_card = SendCard_Lldq.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();
			cards_card.addItem(cards_data[i][0]);

			for (int j = 0; j < GRR._card_count[i]; j++) {
				hand_cards_card.addItem(GRR._cards_data[i][j]);
			}
			send_card.addCardsData(cards_card);
			send_card.addCardCount(1);
			send_card.addHandCardsData(hand_cards_card);
			send_card.addHandCardCount(GRR._card_count[i]);
		}
		// 发送数据
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));
		GRR.add_room_response(roomResponse);

		if (type == 1) {
			send_effect_type(GameConstants.INVALID_SEAT, EFFECT_WAIT_SHOU_QI, 0, GameConstants.INVALID_SEAT);
			// 发第一张
			schedule(ID_TIMER_SEND_CARD_SEND, SheduleArgs.newArgs(), 3000);
		} else {
			if (GRR._card_count[0] < this.get_hand_card_count_init()) {
				send_effect_type(GameConstants.INVALID_SEAT, EFFECT_DI_PAI_YIN_CANG, 0, GameConstants.INVALID_SEAT);
				schedule(ID_TIMER_SEND_CARD_SEND, SheduleArgs.newArgs(), 300);
			}
		}

	}

	public void send_di_pai_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_DI_PAI_DATA);
		DiPaiCard_Lldq.Builder di_pai_data = DiPaiCard_Lldq.newBuilder();
		for (int i = 0; i < _di_pai_count; i++) {
			di_pai_data.addCardData(_di_pai[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(di_pai_data));
		if (seat_index == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	// 发送特效
	public void send_effect_type(int seat_index, int type, int data, int to_player) {
		// type=1：飞底牌 2：工头 3:公友 4:垫牌 5：毙了 6：打住 7：钓主 8：飞分数 9:翻牌红色10:翻牌变白 11:底牌隐藏
		// 12请选择亮主七或翻牌 13等待玩家亮首七 14请选择底牌 15再次点击双亮 16:隐藏叫主面板
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_EFFECT_TYPE);
		effect_type.Builder effect = effect_type.newBuilder();
		effect.setSeatIndex(seat_index);
		effect.setType(type);
		effect.setData(data);
		roomResponse.setCommResponse(PBUtil.toByteString(effect));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	// 发送音效
	public void send_sound_type(int seat_index, int type, int card_type, int count, int cards_data[], int effect_sound, int to_player) {
		// sound_type=1：扣底牌 2：出牌
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SOUND_TYPE);
		sound_type.Builder sound = sound_type.newBuilder();
		sound.setSeatIndex(seat_index);
		sound.setType(type);
		sound.setCardSound(card_type);
		sound.setEffectSound(effect_sound);
		for (int i = 0; i < count; i++) {
			sound.addCardData(cards_data[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(sound));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	public void send_zhu_card(int seat_index, int type, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_ZHU_CARD_DATA);
		Zhu_card_Data.Builder zhu_card = Zhu_card_Data.newBuilder();
		zhu_card.setIsAnimal(type);
		for (int i = 0; i < this._call_banker_card_count; i++) {
			zhu_card.addCardData(this._call_baker_data[i]);
		}
		if (is_select_fan_pai) {
			zhu_card.setIsFanPai(1);
		} else {
			zhu_card.setIsFanPai(0);
		}
		zhu_card.setSeatIndex(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(zhu_card));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	// 埋牌开始
	public void Mai_di_begin() {
		if (GRR._banker_player == GameConstants.INVALID_SEAT) {
			return;
		}
		// 庄家不需要抓分
		_get_score[GRR._banker_player] = -1;

		// 确认主花色
		this._zhu_type = this._logic.GetCardColor(_call_baker_data[0]);
		if (_zhu_type == 4) {
			if (this._logic.GetCardColor(_call_baker_data[1]) == 4) {
				_zhu_type = 4;
			} else {
				if (_call_baker_data[0] == 0x4E) {
					_zhu_type = 3;
				} else {
					_zhu_type = 2;
				}
			}
		}
		this._logic._zhu_type = this._zhu_type;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_BEIGN);
		MaiDiBegin_Lldq.Builder mai_di_begin = MaiDiBegin_Lldq.newBuilder();
		mai_di_begin.setSeatIndex(GRR._banker_player);
		mai_di_begin.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(mai_di_begin));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_effect_type(i, EFFECT_DI_PAI_YIN_CANG, 0, i);
		}

		if (this._call_banker_card_count == 2) {
			if (this._cur_banker == this._first_call_banker) {
				_game_type = GameConstants.GS_LLDQ_MAI_DI_WAIT;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player) {
						continue;
					}
					send_effect_type(GRR._banker_player, EFFECT_DI_PAI_FEI, 0, i);
				}
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
				MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
				mai_di_opetate.addOpreate(1);
				mai_di_opetate.addOpreate(2);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				this.send_response_to_player(GRR._banker_player, roomResponse);
			} else {
				_game_type = GameConstants.GS_LLDQ_MAI_DI;
				deal_send_di_pai(GRR._banker_player);

				send_effect_type(GRR._banker_player, EFFECT_DI_PAI_FEI, 0, GameConstants.INVALID_SEAT);
				send_effect_type(GRR._banker_player, EFFECT_SELECT_DI_PAI, 0, GRR._banker_player);
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
				MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
				mai_di_opetate.addOpreate(3);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				this.send_response_to_player(GRR._banker_player, roomResponse);
			}

		} else if (_call_banker_card_count == 1) {
			int count = 0;
			for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
				if (this._logic.GetCardColor(_call_baker_data[0]) == this._logic.GetCardColor(GRR._cards_data[GRR._banker_player][i])
						&& this._logic.GetCardValue(_call_baker_data[0]) == this._logic.GetCardValue(GRR._cards_data[GRR._banker_player][i])) {
					count++;
				}
			}

			if (count == 1) {

				deal_send_di_pai(GRR._banker_player);
				send_effect_type(GRR._banker_player, EFFECT_SELECT_DI_PAI, 0, GRR._banker_player);
				// 主牌数量
				count = 0;
				for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
					if (this._logic.GetCardColor(_call_baker_data[0]) == this._logic.GetCardColor(GRR._cards_data[GRR._banker_player][i])
							&& this._logic.GetCardValue(_call_baker_data[0]) == this._logic.GetCardValue(GRR._cards_data[GRR._banker_player][i])) {
						count++;
					}
				}
				_game_type = GameConstants.GS_LLDQ_MAI_DI;

				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
				MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
				if (count == 1) {
					send_effect_type(GRR._banker_player, EFFECT_DI_PAI_FEI, 0, GameConstants.INVALID_SEAT);
					mai_di_opetate.addOpreate(3);
				} else {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == GRR._banker_player) {
							continue;
						}
						send_effect_type(GRR._banker_player, EFFECT_DI_PAI_FEI, 0, i);
					}
					mai_di_opetate.addOpreate(1);
					mai_di_opetate.addOpreate(3);
				}

				roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				this.send_response_to_player(GRR._banker_player, roomResponse);
			} else {
				_game_type = GameConstants.GS_LLDQ_MAI_DI_WAIT;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player) {
						continue;
					}
					send_effect_type(GRR._banker_player, EFFECT_DI_PAI_FEI, 0, i);
				}

				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
				MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
				mai_di_opetate.addOpreate(1);
				mai_di_opetate.addOpreate(2);
				roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
				this.send_response_to_player(GRR._banker_player, roomResponse);
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (this._logic.GetCardColor(this.GRR._cards_data[i][j]) == this._zhu_type && this.GRR._cards_data[i][j] < _zhu_value) {
					this.GRR._cards_data[i][j] += this._zhu_value;
				} else {
					if ((this._logic.GetCardValue(this.GRR._cards_data[i][j]) == this._logic._chang_zhu_one
							|| this._logic.GetCardValue(this.GRR._cards_data[i][j]) == this._logic._chang_zhu_two
							|| this._logic.GetCardValue(this.GRR._cards_data[i][j]) == this._logic._chang_zhu_three
							|| this._logic.GetCardValue(this.GRR._cards_data[i][j]) == this._logic._chang_zhu_four)
							&& this.GRR._cards_data[i][j] < _zhu_value) {
						this.GRR._cards_data[i][j] += this._zhu_value;
					}
				}
			}
			this._logic.SortCardList(this.GRR._cards_data[i], this.GRR._card_count[i]);
			if (i == GRR._banker_player) {
				this.RefreshCard(0, i);
			} else {
				this.RefreshCard(1, i);
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			if (i == GRR._banker_player) {
				continue;
			}
			send_effect_type(GRR._banker_player, EFFECT_SELECT_DI_PAI, 0, i);
		}

	}

	public void Refresh_Color_count(int seat_index, boolean is_fanpai) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_ZHU_COUNT);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		Zhu_Count_Lldq.Builder color_count = Zhu_Count_Lldq.newBuilder();
		int big_king_count = 0;
		int samll_king_count = 0;
		int heitao_count = 0;
		int heitao_qi_count = 0;
		int hongxin_count = 0;
		int hongxin_qi_count = 0;
		int meihua_count = 0;
		int meihua_qi_count = 0;
		int fangkaui_count = 0;
		int fangkaui_qi_count = 0;

		int da_wang_value = 0x4F;
		int xiao_wang_value = 0x4E;
		int hei_tiao_value = 0x37;
		int hong_xin_value = 0x27;
		int mei_hua_value = 0x17;
		int fang_kuai_value = 0x07;
		for (int j = 0; j < GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] == 0x4F) {
				big_king_count++;
			} else if (GRR._cards_data[seat_index][j] == 0x4E) {
				samll_king_count++;
			} else if (this._logic.GetCardColor(GRR._cards_data[seat_index][j]) == 3) {
				heitao_count++;
			} else if (this._logic.GetCardColor(GRR._cards_data[seat_index][j]) == 2) {
				hongxin_count++;
			} else if (this._logic.GetCardColor(GRR._cards_data[seat_index][j]) == 1) {
				meihua_count++;
			} else if (this._logic.GetCardColor(GRR._cards_data[seat_index][j]) == 0) {
				fangkaui_count++;
			}

			if (GRR._cards_data[seat_index][j] == hei_tiao_value) {
				heitao_qi_count++;
			} else if (GRR._cards_data[seat_index][j] == hong_xin_value) {
				hongxin_qi_count++;
			} else if (GRR._cards_data[seat_index][j] == mei_hua_value) {
				meihua_qi_count++;
			} else if (GRR._cards_data[seat_index][j] == fang_kuai_value) {
				fangkaui_qi_count++;
			}
		}
		color_count.addCount(big_king_count);
		color_count.addCount(samll_king_count);
		color_count.addCount(heitao_count);
		color_count.addCount(hongxin_count);
		color_count.addCount(meihua_count);
		color_count.addCount(fangkaui_count);

		if (big_king_count == 2) {
			if (_call_banker_card_count == 0) {
				color_count.addIsLiang(true);
			} else if (_call_banker_card_count == 1) {
				if (this._cur_banker == seat_index) {
					color_count.addIsLiang(false);
				} else {
					color_count.addIsLiang(true);
				}
			} else {
				if (this.has_rule(GameConstants.GAME_RULE_LLDQ_KING_FAN_QI) && _call_baker_data[0] != xiao_wang_value
						&& _call_baker_data[0] != da_wang_value) {
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}

		} else {
			color_count.addIsLiang(false);
		}

		if (samll_king_count == 2) {
			if (_call_banker_card_count == 0) {
				color_count.addIsLiang(true);
			} else if (_call_banker_card_count == 1) {
				if (this._cur_banker == seat_index) {
					color_count.addIsLiang(false);
				} else {
					color_count.addIsLiang(true);
				}
			} else {
				if (this.has_rule(GameConstants.GAME_RULE_LLDQ_KING_FAN_QI) && _call_baker_data[0] != xiao_wang_value
						&& _call_baker_data[0] != da_wang_value) {
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}
		} else {
			color_count.addIsLiang(false);
		}

		if (heitao_qi_count > _call_banker_card_count) {
			if (_call_banker_card_count == 0) {
				color_count.addIsLiang(true);
			} else if (_call_banker_card_count == 1) {
				if (this._cur_banker == seat_index && this._call_baker_data[0] == hei_tiao_value) {
					// 双亮
					color_count.addIsLiang(true);
					send_effect_type(_cur_banker, EFFECT_CLICK_AGAIN, 0, _cur_banker);
				} else if (this._cur_banker != seat_index) {
					// 反主
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}

		} else {
			color_count.addIsLiang(false);
		}
		if (hongxin_qi_count > _call_banker_card_count) {
			if (_call_banker_card_count == 0) {
				color_count.addIsLiang(true);
			} else if (_call_banker_card_count == 1) {
				if (this._cur_banker == seat_index && this._call_baker_data[0] == hong_xin_value) {
					// 双亮
					color_count.addIsLiang(true);
					send_effect_type(_cur_banker, EFFECT_CLICK_AGAIN, 0, _cur_banker);
				} else if (this._cur_banker != seat_index) {
					// 反主
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}
		} else {
			color_count.addIsLiang(false);
		}
		if (meihua_qi_count > _call_banker_card_count) {
			if (_call_banker_card_count == 0) {
				color_count.addIsLiang(true);
			} else if (_call_banker_card_count == 1) {
				if (this._cur_banker == seat_index && this._call_baker_data[0] == mei_hua_value) {
					// 双亮
					color_count.addIsLiang(true);
					send_effect_type(_cur_banker, EFFECT_CLICK_AGAIN, 0, _cur_banker);
				} else if (this._cur_banker != seat_index) {
					// 反主
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}
		} else {
			color_count.addIsLiang(false);
		}
		if (fangkaui_qi_count > _call_banker_card_count) {
			if (_call_banker_card_count == 0) {
				color_count.addIsLiang(true);
			} else if (_call_banker_card_count == 1) {
				if (this._cur_banker == seat_index && this._call_baker_data[0] == fang_kuai_value) {
					// 双亮
					color_count.addIsLiang(true);
					send_effect_type(_cur_banker, EFFECT_CLICK_AGAIN, 0, _cur_banker);
				} else if (this._cur_banker != seat_index) {
					// 反主
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}
		} else {
			color_count.addIsLiang(false);
		}
		if (is_fanpai) {
			color_count.addIsLiang(true);
		} else {
			color_count.addIsLiang(false);
		}
		// 发送数据
		roomResponse.setCommResponse(PBUtil.toByteString(color_count));
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			if (_handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);

			}

		}
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

		return true;

	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD_SEND: {
			send_card(2);
			if (GRR._card_count[0] >= this.get_hand_card_count_init()) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SEND_CARD_END);
				SendCardEnd_Lldq.Builder send_card_end = SendCardEnd_Lldq.newBuilder();
				send_card_end.setCardCount(8);
				send_card_end.setIsAnimal(1);
				roomResponse.setCommResponse(PBUtil.toByteString(send_card_end));
				this.send_response_to_room(roomResponse);
				GRR.add_room_response(roomResponse);

				if (GRR._banker_player == GameConstants.INVALID_SEAT) {
					schedule(ID_TIMER_SEND_CARD_END_WAIT, SheduleArgs.newArgs(), 10000);
				} else {
					send_zhu_card(GRR._banker_player, 1, GameConstants.INVALID_SEAT);
					Mai_di_begin();
				}

			}
			return;
		}
		case ID_TIMER_SEND_CARD_END_WAIT: {
			if (this._cur_banker == GameConstants.INVALID_SEAT) {
				if (_fan_pai_seat_index == GameConstants.INVALID_SEAT) {
					send_effect_type(GameConstants.INVALID_SEAT, EFFECT_DI_PAI_YIN_CANG, 0, GameConstants.INVALID_SEAT);
					this.Reset();
					this.send_card(1);
				} else {
					this._game_type = GameConstants.GS_LLDQ_CALL_BANKER;
					this.send_effect_type(_fan_pai_seat_index, EFFECT_FAN_PAI_WHITE, _repertory_card[_fan_pai_card_index],
							GameConstants.INVALID_SEAT);
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (i == _fan_pai_seat_index) {
							this.Refresh_Color_count(i, true);
						} else {
							send_effect_type(i, EFFECT_CALL_YIN_CANG, 0, i);
						}

					}
					send_effect_type(_fan_pai_seat_index, EFFECT_LIANG_ZHU_OR_FAN_PAI, 0, GameConstants.INVALID_SEAT);
				}
			} else {
				GRR._banker_player = _cur_banker;
				send_zhu_card(GRR._banker_player, 1, GameConstants.INVALID_SEAT);
				Mai_di_begin();
			}
			return;
		}
		case ID_TIMER_ANIMAL_DELAY: {
			int seat_index = args.get("seat_index");
			int animal_type = args.get("type");
			int data = args.get("data");
			int to_player = args.get("to_player");
			send_effect_type(seat_index, animal_type, (int) args.get("data"), to_player);
			return;
		}
		case ID_TIMER_LIANG_ZHU_TO_MAI_DI: {
			Mai_di_begin();
			return;
		}
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

	public void load_player_info_data_game_end(PukeGameEndLldq.Builder roomResponse) {
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
		if (reason == GameConstants.Game_End_NORMAL) {
			if (seat_index == GameConstants.INVALID_SEAT) {
				// 投降
				int times = 1;
				if (this.GRR._card_count[GRR._banker_player] > this.get_hand_card_count_init()) {
					// 拿了底牌投降
					times = 2;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == GRR._banker_player) {
						continue;
					}
					win_total_times[i]++;
					end_score[i] += times;
					end_score[GRR._banker_player] -= end_score[i];
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this._player_result.game_score[i] += end_score[i];
				}
			} else {
				cal_score(end_score, seat_index);
			}

		}

		if (GRR != null) {
			this.operate_player_data();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_GAME_END);
		roomResponse.setRoomInfo(getRoomInfo());
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		game_end.setRoomInfo(getRoomInfo());

		load_player_info_data(roomResponse);
		PukeGameEndLldq.Builder gameend_sj = PukeGameEndLldq.newBuilder();
		load_player_info_data_game_end(gameend_sj);
		gameend_sj.setRoomInfo(this.getRoomInfo());
		int xian_get_score = 0;
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i != GRR._banker_player && i != this._other_banker) {
					xian_get_score += _get_score[i];
				}
			}
			if (xian_get_score < 0) {
				xian_get_score = 0;
			}
			if (GRR._banker_player != GameConstants.INVALID_SEAT) {

			}
			gameend_sj.setKouDiScore(this._kou_di_score);
			gameend_sj.setKouDiSj(this._kou_di_jia_ji);
			gameend_sj.setKouDiTimes(this._kou_di_fan_bei);
			gameend_sj.setZhuaFen(xian_get_score);
			gameend_sj.setBankerPlayer(GRR._banker_player);
			gameend_sj.setOtherBanker(this._other_banker);
			gameend_sj.setAllZhuaScore(xian_get_score + _kou_di_score * _kou_di_fan_bei);
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

			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end.addGameScore(end_score[i]);
			gameend_sj.addGameScore(end_score[i]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					gameend_sj.addAllEndScore((int) _player_result.game_score[i]);
					gameend_sj.addTotalWinTimes(this.win_total_times[i]);
				}
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameend_sj.addAllEndScore((int) _player_result.game_score[i]);
				gameend_sj.addTotalWinTimes(this.win_total_times[i]);
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

	public boolean game_end_type() {
		int times = 1;// 负数庄家赢，正数闲家赢
		int score = _select_dang[GRR._banker_player] * (int) game_cell;
		int xian_get_score = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i != GRR._banker_player && i != this._other_banker) {
				xian_get_score += _get_score[i];
			}
		}
		xian_get_score += this._kou_di_score * this._kou_di_fan_bei;
		int is_end_type = 0;
		if (xian_get_score == 0) {
			times = -this._max_end_socre_limit;
			is_end_type = EFFECT_GAME_END_DA_GUANG;
		} else if (xian_get_score < 40) {
			times = -2;
			is_end_type = EFFECT_GAME_END_XIAO_GUANG;
		} else if (xian_get_score <= 75) {
			times = -1;
		} else if (xian_get_score <= 115) {
			times = 1;
		} else if (xian_get_score <= 155) {
			times = 2;
		} else if (xian_get_score <= 195) {
			times = 3;
		} else if (xian_get_score <= 235) {
			times = 4;
		} else {
			times = 5;
		}

		boolean is_man_guan = false;
		if (times > 0) {
			times += this._kou_di_jia_ji;
			if (times >= _max_end_socre_limit) {
				is_man_guan = true;
			}
		}
		if (times <= -_max_end_socre_limit) {
			is_man_guan = true;
		}

		if (is_end_type != 0 && is_man_guan) {
			SheduleArgs nimal_one = SheduleArgs.newArgs();
			nimal_one.set("seat_index", GameConstants.INVALID_SEAT);
			nimal_one.set("to_player", GameConstants.INVALID_SEAT);
			nimal_one.set("data", 0);
			nimal_one.set("type", is_end_type);
			schedule(ID_TIMER_ANIMAL_DELAY, nimal_one, 1000);

			nimal_one.set("seat_index", GameConstants.INVALID_SEAT);
			nimal_one.set("to_player", GameConstants.INVALID_SEAT);
			nimal_one.set("data", 0);
			nimal_one.set("type", EFFECT_GAME_END_MAN_GUAN);
			schedule(ID_TIMER_ANIMAL_DELAY, nimal_one, 5000);

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), _current_player, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			return false;
		} else if (is_end_type != 0) {
			SheduleArgs nimal_one = SheduleArgs.newArgs();
			nimal_one.set("seat_index", GameConstants.INVALID_SEAT);
			nimal_one.set("to_player", GameConstants.INVALID_SEAT);
			nimal_one.set("data", 0);
			nimal_one.set("type", is_end_type);
			schedule(ID_TIMER_ANIMAL_DELAY, nimal_one, 1000);

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), _current_player, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			return false;
		} else if (is_man_guan) {
			SheduleArgs nimal_one = SheduleArgs.newArgs();
			nimal_one.set("seat_index", GameConstants.INVALID_SEAT);
			nimal_one.set("to_player", GameConstants.INVALID_SEAT);
			nimal_one.set("data", 0);
			nimal_one.set("type", EFFECT_GAME_END_MAN_GUAN);
			schedule(ID_TIMER_ANIMAL_DELAY, nimal_one, 5000);

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), _current_player, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			return false;
		}
		return true;
	}

	public void cal_score(int end_score[], int seat_index) {
		int times = 1;// 负数庄家赢，正数闲家赢

		int score = _select_dang[GRR._banker_player] * (int) game_cell;
		int xian_get_score = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i != GRR._banker_player && i != this._other_banker) {
				xian_get_score += _get_score[i];
			}
		}
		xian_get_score += this._kou_di_score * this._kou_di_fan_bei;
		if (xian_get_score == 0) {
			times = -this._max_end_socre_limit;
		} else if (xian_get_score < 40) {
			times = -2;
		} else if (xian_get_score <= 75) {
			times = -1;
		} else if (xian_get_score <= 115) {
			times = 1;
		} else if (xian_get_score <= 155) {
			times = 2;
		} else if (xian_get_score <= 195) {
			times = 3;
		} else if (xian_get_score <= 235) {
			times = 4;
		} else {
			times = 5;
		}

		if (times > 0) {
			times += this._kou_di_jia_ji;
			if (times > _max_end_socre_limit) {
				times = _max_end_socre_limit;
			}
		} else {
			if (times < -_max_end_socre_limit) {
				times = -_max_end_socre_limit;
			}
			_kou_di_jia_ji = 0;
			if (this._other_banker == GameConstants.INVALID_SEAT && this.has_rule(GameConstants.GAME_RULE_LLDQ_BANKER_DAN_WIN_DOUBLE)) {
				times *= 2;
			}
			if (_is_shou_qi) {
				times *= 2;
			}

		}
		if (this._other_banker == GameConstants.INVALID_SEAT) {
			int win_score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player) {
					continue;
				}
				end_score[i] += times;
				win_score -= end_score[i];
			}
			end_score[GRR._banker_player] += win_score;
		} else {
			int win_score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player || i == _other_banker) {
					continue;
				}
				end_score[i] += times;
				win_score -= end_score[i];
			}
			end_score[GRR._banker_player] += win_score * 2 / 3;
			end_score[_other_banker] += win_score / 3;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (end_score[i] > 0) {
				win_total_times[i]++;
			}
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
			OutCardDataLldq.Builder outcarddata = OutCardDataLldq.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_LLDQ_OUT_CARD);// 201
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

		if (type == MsgConstants.REQUST_SJ_LLDQ_OPERATE) {
			Opreate_RequestWsk_lldq req = PBUtil.toObject(room_rq, Opreate_RequestWsk_lldq.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getSelectType(), req.getCardsDataList());
		}
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int select_type, List<Integer> list) {
		switch (opreate_type) {
		case GAME_OPREATE_TYPE_CALL_BANKER: {
			deal_call_banker(seat_index, select_type);
			return true;
		}
		case GAME_OPREATE_TYPE_OUT_XIANG: {
			deal_tou_xiang(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_GET_DI_CARD: {
			deal_get_di_pai(seat_index);
			return true;
		}
		case GAME_OPREATE_TYPE_PUT_DI_CARD: {
			deal_put_di_pai(seat_index, list);
			return true;
		}
		}
		return true;
	}

	public void deal_send_di_pai(int seat_index) {

		for (int i = 0; i < _di_pai_count; i++) {
			if (this._logic.GetCardColor(this._di_pai[i]) == this._zhu_type && this._di_pai[i] < _zhu_value) {
				this._di_pai[i] += this._zhu_value;
			} else {
				if ((this._logic.GetCardValue(this._di_pai[i]) == this._logic._chang_zhu_one
						|| this._logic.GetCardValue(this._di_pai[i]) == this._logic._chang_zhu_two
						|| this._logic.GetCardValue(this._di_pai[i]) == this._logic._chang_zhu_three
						|| this._logic.GetCardValue(this._di_pai[i]) == this._logic._chang_zhu_four) && this._di_pai[i] < _zhu_value) {
					this._di_pai[i] += this._zhu_value;
				}
			}
		}

		if (GRR._card_count[seat_index] == this.get_hand_card_count_init()) {
			for (int i = 0; i < _di_pai_count; i++) {
				GRR._cards_data[seat_index][GRR._card_count[seat_index]++] = this._di_pai[i];
			}
		}

		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index]);
		RefreshCard(0, seat_index);
	}

	public void deal_put_di_pai(int seat_index, List<Integer> list) {
		if (seat_index != GRR._banker_player || this._game_type != GameConstants.GS_LLDQ_MAI_DI) {
			return;
		}
		int card_count = list.size();

		if (_di_pai_count != card_count) {
			return;
		}
		int card_all_count = GRR._card_count[GRR._banker_player];
		int card_data[] = new int[card_all_count];
		for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
			card_data[i] = GRR._cards_data[GRR._banker_player][i];
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
		GRR._card_count[GRR._banker_player] -= _di_pai_count;
		this._logic.SortCardList(GRR._cards_data[GRR._banker_player], GRR._card_count[GRR._banker_player]);
		this.RefreshCard(1, GRR._banker_player);
		this._game_type = GameConstants.GS_LLDQ_PLAY;
		this._game_status = GameConstants.GS_LLDQ_PLAY;// 设置状态
		this._current_player = GRR._banker_player;

		send_sound_type(GRR._banker_player, SOUND_DI_PAI, 0, 0, null, 0, GameConstants.INVALID_SEAT);
		// 隐藏所有叫主操作
		send_effect_type(GameConstants.INVALID_SEAT, EFFECT_DI_PAI_YIN_CANG, 0, GameConstants.INVALID_SEAT);
		// 工头动画
		send_effect_type(GRR._banker_player, EFFECT_GONG_TOU, 0, GameConstants.INVALID_SEAT);
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.LLDQ_CT_ERROR, GameConstants.INVALID_SEAT);

		// SheduleArgs nimal_one = SheduleArgs.newArgs();
		// nimal_one.set("seat_index", GRR._banker_player);
		// nimal_one.set("to_player", GameConstants.INVALID_SEAT);
		// nimal_one.set("data", 0);
		// nimal_one.set("type", 2);
		// schedule(ID_TIMER_ANIMAL_DELAY, nimal_one, 1000);

	}

	public void deal_get_di_pai(int seat_index) {
		if (seat_index != GRR._banker_player || this._game_type != GameConstants.GS_LLDQ_MAI_DI_WAIT) {
			return;
		}
		this.send_effect_type(seat_index, EFFECT_DI_PAI_FEI, 0, seat_index);
		deal_send_di_pai(seat_index);
		this._game_type = GameConstants.GS_LLDQ_MAI_DI;

		send_effect_type(GRR._banker_player, EFFECT_SELECT_DI_PAI, 0, GRR._banker_player);
		// 拿完底牌操作
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_MAIDI_OPREATE);
		MaiDiOpreate_Lldq.Builder mai_di_opetate = MaiDiOpreate_Lldq.newBuilder();
		mai_di_opetate.addOpreate(1);
		mai_di_opetate.addOpreate(3);
		roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
		this.send_response_to_player(GRR._banker_player, roomResponse);
	}

	public void deal_tou_xiang(int seat_index) {
		if (seat_index != GRR._banker_player) {
			return;
		}
		if (this._call_banker_card_count == 2) {
			if (_first_call_banker != seat_index) {
				return;
			}
		} else if (this._call_banker_card_count == 1) {
			int count = 0;
			for (int i = 0; i < GRR._card_count[GRR._banker_player]; i++) {
				if (this._logic.GetCardColor(_call_baker_data[0]) == this._logic.GetCardColor(GRR._cards_data[GRR._banker_player][i])
						&& this._logic.GetCardValue(_call_baker_data[0]) == this._logic.GetCardValue(GRR._cards_data[GRR._banker_player][i])) {
					count++;
				}
			}
			if (count == 1) {
				return;
			}
		}

		int delay = 1;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
	}

	public void deal_call_banker(int seat_index, int select_type) {
		if (this._game_type != GameConstants.GS_LLDQ_CALL_BANKER && this._game_type != GameConstants.GS_LLDQ_SEND_CARD) {
			return;
		}
		if (this._game_type == GameConstants.GS_LLDQ_CALL_BANKER && seat_index != _fan_pai_seat_index) {
			return;
		}
		send_effect_type(GameConstants.INVALID_SEAT, EFFECT_DI_PAI_YIN_CANG, 0, GameConstants.INVALID_SEAT);
		int big_king_count = 0;
		int samll_king_count = 0;
		int heitao_qi_count = 0;
		int hongxin_qi_count = 0;
		int meihua_qi_count = 0;
		int fangkaui_qi_count = 0;

		int da_wang_value = 0x4F;
		int xiao_wang_value = 0x4E;
		int hei_tiao_value = 0x37;
		int hong_xin_value = 0x27;
		int mei_hua_value = 0x17;
		int fang_kuai_value = 0x07;

		for (int j = 0; j < GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] == da_wang_value) {
				big_king_count++;
			} else if (GRR._cards_data[seat_index][j] == xiao_wang_value) {
				samll_king_count++;
			}

			if (GRR._cards_data[seat_index][j] == hei_tiao_value) {
				heitao_qi_count++;
			} else if (GRR._cards_data[seat_index][j] == hong_xin_value) {
				hongxin_qi_count++;
			} else if (GRR._cards_data[seat_index][j] == mei_hua_value) {
				meihua_qi_count++;
			} else if (GRR._cards_data[seat_index][j] == fang_kuai_value) {
				fangkaui_qi_count++;
			}
		}
		// 还没人亮主
		if (_call_banker_card_count == 0) {
			if (select_type == 0) {
				if (fangkaui_qi_count > _call_banker_card_count) {
					_call_baker_data[_call_banker_card_count++] = fang_kuai_value;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 1) {
				if (meihua_qi_count > _call_banker_card_count) {
					_call_baker_data[_call_banker_card_count++] = mei_hua_value;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 2) {
				if (hongxin_qi_count > _call_banker_card_count) {
					_call_baker_data[_call_banker_card_count++] = hong_xin_value;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 3) {
				if (heitao_qi_count > _call_banker_card_count) {
					_call_baker_data[_call_banker_card_count++] = hei_tiao_value;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 4) {
				if (samll_king_count == 2) {
					_call_baker_data[_call_banker_card_count++] = xiao_wang_value;
					_call_baker_data[_call_banker_card_count++] = xiao_wang_value;
					GRR._banker_player = seat_index;
					_cur_banker = seat_index;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 5) {
				if (big_king_count == 2) {
					_call_baker_data[_call_banker_card_count++] = da_wang_value;
					_call_baker_data[_call_banker_card_count++] = da_wang_value;
					GRR._banker_player = seat_index;
					_cur_banker = seat_index;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 6) {
				if (seat_index != _fan_pai_seat_index) {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
				if (this._game_type != GameConstants.GS_LLDQ_CALL_BANKER) {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
				is_select_fan_pai = true;
				_call_baker_data[_call_banker_card_count++] = _repertory_card[_fan_pai_card_index];
			} else {
				this.send_error_notify(seat_index, 2, "操作不正确");
				return;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_LLDQCALL_BANKER);
			CallBankerResponse_Lldq.Builder call_bnaker_response = CallBankerResponse_Lldq.newBuilder();
			call_bnaker_response.setSeatIndex(seat_index);
			if (GRR._card_count[seat_index] == 1 && this.has_rule(GameConstants.GAME_RULE_LLDQ_SHOU_QI)) {
				call_bnaker_response.setType(4);
				_is_shou_qi = true;
			} else {
				call_bnaker_response.setType(1);
			}

			for (int i = 0; i < _call_banker_card_count; i++) {
				call_bnaker_response.addCardsData(_call_baker_data[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_bnaker_response));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
			_cur_banker = seat_index;
			_first_call_banker = seat_index;

			if (this._game_type == GameConstants.GS_LLDQ_CALL_BANKER) {
				// 等待翻牌亮主阶段直接确定庄家
				GRR._banker_player = seat_index;
				send_zhu_card(GRR._banker_player, 1, GameConstants.INVALID_SEAT);
				SheduleArgs args = SheduleArgs.newArgs();
				schedule(ID_TIMER_LIANG_ZHU_TO_MAI_DI, args, 1000);

				return;
			} else {
				if (hasShedule(ID_TIMER_SEND_CARD_END_WAIT)) {
					this.cancelShedule(ID_TIMER_SEND_CARD_END_WAIT);
					schedule(ID_TIMER_SEND_CARD_END_WAIT, SheduleArgs.newArgs(), 10000);

				}
			}

		} else if (_call_banker_card_count == 1) {
			// 已经有人亮过主
			if (select_type == 0) {
				if (fangkaui_qi_count > _call_banker_card_count) {
					if (this._cur_banker != seat_index) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = fang_kuai_value;
						}
					} else if (_call_baker_data[0] == fang_kuai_value) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = fang_kuai_value;
						}
					} else {
						this.send_error_notify(seat_index, 2, "操作不正确");
						return;
					}

				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 1) {
				if (meihua_qi_count > _call_banker_card_count) {
					if (this._cur_banker != seat_index) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = mei_hua_value;
						}
					} else if (_call_baker_data[0] == mei_hua_value) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = mei_hua_value;
						}
					} else {
						this.send_error_notify(seat_index, 2, "操作不正确");
						return;
					}
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 2) {
				if (hongxin_qi_count > _call_banker_card_count) {
					if (this._cur_banker != seat_index) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = hong_xin_value;
						}
					} else if (_call_baker_data[0] == hong_xin_value) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = hong_xin_value;
						}
					} else {
						this.send_error_notify(seat_index, 2, "操作不正确");
						return;
					}
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 3) {
				if (heitao_qi_count > _call_banker_card_count) {
					if (this._cur_banker != seat_index) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = hei_tiao_value;
						}
					} else if (_call_baker_data[0] == hei_tiao_value) {
						_call_banker_card_count++;
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = hei_tiao_value;
						}
					} else {
						this.send_error_notify(seat_index, 2, "操作不正确");
						return;
					}
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 4) {
				if (samll_king_count == 2 && this._cur_banker != seat_index) {
					_call_banker_card_count++;
					GRR._banker_player = seat_index;
					_cur_banker = seat_index;
					for (int i = 0; i < _call_banker_card_count; i++) {
						_call_baker_data[i] = xiao_wang_value;
					}
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else if (select_type == 5) {
				if (big_king_count == 2 && this._cur_banker != seat_index) {
					_call_banker_card_count++;
					GRR._banker_player = seat_index;
					_cur_banker = seat_index;
					for (int i = 0; i < _call_banker_card_count; i++) {
						_call_baker_data[i] = da_wang_value;
					}
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else {
				this.send_error_notify(seat_index, 2, "操作不正确");
				return;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_LLDQCALL_BANKER);
			CallBankerResponse_Lldq.Builder call_bnaker_response = CallBankerResponse_Lldq.newBuilder();
			call_bnaker_response.setSeatIndex(seat_index);
			if (_cur_banker == seat_index) {
				call_bnaker_response.setType(3);
			} else {
				call_bnaker_response.setType(2);
			}

			for (int i = 0; i < _call_banker_card_count; i++) {
				call_bnaker_response.addCardsData(_call_baker_data[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_bnaker_response));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);

			if (has_rule(GameConstants.GAME_RULE_LLDQ_KING_FAN_QI)) {
				_cur_banker = seat_index;
			} else {
				GRR._banker_player = seat_index;
				_cur_banker = seat_index;
			}

			if (hasShedule(ID_TIMER_SEND_CARD_END_WAIT) && GRR._banker_player != GameConstants.INVALID_SEAT) {
				// 反主或双王直接确定庄家
				send_zhu_card(GRR._banker_player, 1, GameConstants.INVALID_SEAT);
				this.cancelShedule(ID_TIMER_SEND_CARD_END_WAIT);
				SheduleArgs args = SheduleArgs.newArgs();
				schedule(ID_TIMER_LIANG_ZHU_TO_MAI_DI, args, 1000);
				return;
			} else {
				if (hasShedule(ID_TIMER_SEND_CARD_END_WAIT)) {
					this.cancelShedule(ID_TIMER_SEND_CARD_END_WAIT);
					schedule(ID_TIMER_SEND_CARD_END_WAIT, SheduleArgs.newArgs(), 10000);
				}
			}
		} else {
			if (has_rule(GameConstants.GAME_RULE_LLDQ_KING_FAN_QI)) {
				if (select_type == 4) {
					if (samll_king_count == _call_banker_card_count && _cur_banker != seat_index && this._call_baker_data[0] != da_wang_value) {
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = xiao_wang_value;
						}
					} else {
						this.send_error_notify(seat_index, 2, "操作不正确");
						return;
					}
				} else if (select_type == 5) {
					if (big_king_count == _call_banker_card_count && _cur_banker != seat_index && this._call_baker_data[0] != xiao_wang_value) {
						for (int i = 0; i < _call_banker_card_count; i++) {
							_call_baker_data[i] = da_wang_value;
						}
					} else {
						this.send_error_notify(seat_index, 2, "操作不正确");
						return;
					}
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}
			} else {
				this.send_error_notify(seat_index, 2, "操作不正确");
				return;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_LLDQCALL_BANKER);
			CallBankerResponse_Lldq.Builder call_bnaker_response = CallBankerResponse_Lldq.newBuilder();
			call_bnaker_response.setSeatIndex(seat_index);
			if (_cur_banker == seat_index) {
				call_bnaker_response.setType(3);
			} else {
				call_bnaker_response.setType(2);
			}

			for (int i = 0; i < _call_banker_card_count; i++) {
				call_bnaker_response.addCardsData(_call_baker_data[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_bnaker_response));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);

			GRR._banker_player = seat_index;
			_cur_banker = seat_index;
			if (hasShedule(ID_TIMER_SEND_CARD_END_WAIT)) {
				this.cancelShedule(ID_TIMER_SEND_CARD_END_WAIT);
				send_zhu_card(GRR._banker_player, 1, GameConstants.INVALID_SEAT);
				Mai_di_begin();
				return;
			}
		}

		if (select_type != 6 && _fan_pai_card_index != GameConstants.INVALID_CARD) {
			_fan_pai_card_index = GameConstants.INVALID_CARD;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_fan_pai_seat_index != GameConstants.INVALID_SEAT) {
				Refresh_Color_count(i, true);
			} else {
				Refresh_Color_count(i, false);
			}

		}

	}

	public void RefreshCard(int isnomal, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_REFRES_CARD_DATA);
		// 发送数据
		RefreshCardData_Lldq.Builder refresh_card = RefreshCardData_Lldq.newBuilder();
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
		refresh_card.setIsNomal(isnomal);
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);

		// 回放
		refresh_card.clear();
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
	}

	public void RefreshScore(int to_player, int is_delay) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_LLDQ_SCORE);
		// 发送数据
		TableScore_Lldq.Builder refresh_score = TableScore_Lldq.newBuilder();
		int get_score = 0;
		int yu_score = 200;
		int banker_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			refresh_score.addGetScore(_get_score[i]);
		}
		refresh_score.setTableScore(this._table_score);
		refresh_score.setIsDelay(is_delay);
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
