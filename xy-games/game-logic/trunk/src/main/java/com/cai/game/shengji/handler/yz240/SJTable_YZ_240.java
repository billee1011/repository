package com.cai.game.shengji.handler.yz240;

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
import com.cai.game.shengji.SJConstants;
import com.cai.game.shengji.SJGameLogic_YZSJ;
import com.cai.game.shengji.SJTable;
import com.cai.game.shengji.handler.SJHandlerFinish;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Xpsj.XpsjRsp.Opreate_RequestWsk_Xpsj;
import protobuf.clazz.Xpsj.XpsjRsp.effect_type_xpsj;
import protobuf.clazz.yzsj.yzsjRsp.CallBankerResponse_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.Card_Arrary_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.DiPaiCard_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.GameStart_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.LiShiCard_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.MaiDiOpreate_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.OutCardDatayzsj;
import protobuf.clazz.yzsj.yzsjRsp.PaiFenData_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.PukeGameEndyzsj;
import protobuf.clazz.yzsj.yzsjRsp.RefreshCardData_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.SendCardEnd_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.SendCard_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.TableResponse_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.TableScore_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.Zhu_Count_yzsj;
import protobuf.clazz.yzsj.yzsjRsp.Zhu_card_Data_yzsj;

public class SJTable_YZ_240 extends SJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;
	protected static final int GAME_OPREATE_TYPE_CALL_BANKER = 1;
	protected static final int GAME_OPREATE_TYPE_OUT_XIANG = 2;
	protected static final int GAME_OPREATE_TYPE_GET_DI_CARD = 3;
	protected static final int GAME_OPREATE_TYPE_PUT_DI_CARD = 4;

	protected static final int GAME_OPREATE_CALL_TYPE_FANG = 0;
	protected static final int GAME_OPREATE_CALL_TYPE_MEI = 1;
	protected static final int GAME_OPREATE_CALL_TYPE_HONG = 2;
	protected static final int GAME_OPREATE_CALL_TYPE_HEI = 3;
	protected static final int GAME_OPREATE_CALL_TYPE_SMALL_KING = 4;
	protected static final int GAME_OPREATE_CALL_TYPE_BIG_KING = 5;
	protected static final int GAME_OPREATE_CALL_TYPE_NO = 6;

	protected static final int EFFECT_CALL_YIN_CANG = 1;// 叫主面板隐藏
	protected static final int EFFECT_CALL_BI_PAI = 2;// 毙牌
	protected static final int EFFECT_CALL_GAI_BI = 3;// 盖毙

	protected static final int ID_TIMER_SEND_CARD_SEND = 1;// 发牌间隔
	protected static final int ID_TIMER_SEND_CARD_END_WAIT = 2;// 发牌结束到等待
	protected static final int ID_TIMER_RE_SEND_CARD = 3;// 重新发牌
	public int _call_baker_data[];
	public int _call_banker_card_count;
	public int _first_call_banker;
	public int _end_score[];
	public int _fan_pai_card_index;
	public int _fan_pai_seat_index;
	public boolean is_select_fan_pai;
	public int _kou_di_score;
	public int _kou_di_fan_bei;
	public int _kou_di_jia_ji;
	public int win_total_times[];
	public boolean _have_5_must_A;

	public SJGameLogic_YZSJ _logic;

	public SJTable_YZ_240() {
		_logic = new SJGameLogic_YZSJ();
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
		_call_baker_data = new int[get_hand_card_count_max()];
		_get_score_card = new int[28];
		_user_get_score_card = new int[getTablePlayerNumber()][28];
		_user_get_score_count = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_is_call_action[i] = false;
			_select_dang[i] = -1;
			_cur_out_card_type[i] = GameConstants.XFGD_CT_ERROR;
		}
		_handler_out_card_operate = new SJHandlerOutCardOperate_YZ_240();
		this._handler = this._handler_out_card_operate;
		_out_card_player = GameConstants.INVALID_SEAT;
		_first_call_banker = GameConstants.INVALID_SEAT;
		win_total_times = new int[getTablePlayerNumber()];

		_player_ready = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			win_total_times[i] = 0;
		}
		this._logic.setRuleMap(this.getRuleMap());
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
			Arrays.fill(_user_get_score_card[i], GameConstants.INVALID_CARD);
			_user_get_score_count[i] = 0;
		}
		_table_score = 0;
		_kou_di_score = 0;
		_kou_di_fan_bei = 0;
		_kou_di_jia_ji = 0;
		Arrays.fill(_get_score_card, GameConstants.INVALID_CARD);
		_get_score_count = 0;
		_all_card_len = SJConstants.CARD_COUNT_XP_SJ;
		
		_fan_pai_card_index = (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (_all_card_len - this._di_pai_count));
		_fan_pai_card_index = 20;
		_fan_pai_seat_index = GameConstants.INVALID_SEAT;
		_other_banker = GameConstants.INVALID_SEAT;

		int score_card[] = { 0x05, 0x05, 0x15, 0x15, 0x25, 0x25, 0x35, 0x35, 0x0A, 0x0A, 0x1A, 0x1A, 0x2A, 0x2A, 0x3A, 0x3A, 0x0D, 0x0D, 0x1D, 0x1D,
				0x2D, 0x2D, 0x3D, 0x3D, 0x4E, 0x4E, 0x4F, 0x4F };
		_pai_score_card = score_card;
		_pai_score_count = 28;
		
		if(this.has_rule(GameConstants.GAME_RULE_YZSJ_300)){
			_pai_score = 300;
		}else{
			_pai_score = 240;
		}
		

		if (getTablePlayerNumber() == 3) {
			_all_card_len = SJConstants.YZ_SJ_CARD_COUNT_3;
			_repertory_card = new int[SJConstants.YZ_SJ_CARD_COUNT_3];
			shuffle(_repertory_card, GameConstants.CARD_DATA_YZ_240_3);
		} else {
			_all_card_len = SJConstants.YZ_SJ_CARD_COUNT_4;
			_repertory_card = new int[SJConstants.YZ_SJ_CARD_COUNT_4];
			shuffle(_repertory_card, GameConstants.CARD_DATA_YZ_240_4);
		}

		_table_score = 0;
		_max_card_seat = GameConstants.INVALID_SEAT;
		_first_call_banker = GameConstants.INVALID_SEAT;
		GRR._left_card_count = this._all_card_len;

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
		_have_5_must_A = false;
		this.RefreshScore(GameConstants.INVALID_SEAT, 0);
		//this.RefreshScore(GameConstants.INVALID_SEAT);
		this.Refresh_pai_score(GameConstants.INVALID_SEAT);
		this.send_history(GameConstants.INVALID_SEAT);
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

		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	protected void test_cards() {
		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		int cards[] = new int[] { 
				28, 34, 41, 9, 25, 56, 25, 57, 23, 10, 21, 41, 53, 39, 11, 37, 60, 58, 7, 
				11, 1, 55, 18, 26, 53, 59, 42, 33, 34, 50, 79, 49, 78, 40, 44, 37, 8, 79, 
				40, 5, 27, 18, 27, 56, 60, 28, 2, 23, 24, 13, 1, 29, 12, 5, 26, 61, 58, 8, 
				44, 13, 17, 10, 39, 29, 43, 24, 2, 9, 45, 50, 7, 55, 21, 33, 57, 45, 49, 42, 59, 43, 12, 78, 61, 17
				};
		this._repertory_card = cards;
		// int index = 0;
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// if (this.get_players()[i] == null) {
		// continue;
		// }
		// for (int j = 0; j < get_hand_card_count_init(); j++) {
		// GRR._cards_data[i][j] = cards[index++];
		// }
		// }

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
		if (this.getTablePlayerNumber() == 3) {
			return SJConstants.YZ_SJ_MAX_COUT_3;
		} else {
			return SJConstants.YZ_SJ_MAX_COUT_4;
		}

	}

	protected boolean on_game_start() {
		// 游戏开始
		this._game_status = SJConstants.GS_XPSJ_SEND_CARD;// 设置状态
		int FlashTime = 4000;
		int standTime = 1000;
		// RefreshScore(GameConstants.INVALID_SEAT, 0);
		// 初始化游戏变量
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		GameStart_yzsj.Builder gamestart = GameStart_yzsj.newBuilder();
		gamestart.setRoomInfo(this.getRoomInfo());
		this.load_player_info_data_game_start(gamestart);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart));
		// 自己才有牌数据
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		send_card(1);
		// 自己才有牌数据
		return true;
	}

	public void out_card_begin() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_effect_type(i, EFFECT_CALL_YIN_CANG, 0, i);
		}
		this._zhu_type = this._logic.GetCardColor(_call_baker_data[0]);
		this._logic._zhu_type = this._zhu_type;
		this._game_status = SJConstants.GS_XPSJ_PLAY;
		this.GRR._banker_player = _cur_banker;
		this._current_player = _cur_banker;
		Refresh_pai_score(GameConstants.INVALID_SEAT);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_one
						|| this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_two
						|| this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_three
						|| this._logic.GetCardValue(GRR._cards_data[i][j]) == this._logic._chang_zhu_four) {
					GRR._cards_data[i][j] += this._zhu_value;
				} else if (this._logic.GetCardColor(GRR._cards_data[i][j]) == this._zhu_type) {
					GRR._cards_data[i][j] += this._zhu_value;
				}
			}
			this._logic.SortCardList(this.GRR._cards_data[i], this.GRR._card_count[i]);
			this.RefreshCard(i);
		}
		RefreshCard(GameConstants.INVALID_SEAT);
		send_zhu_data(GameConstants.INVALID_SEAT);
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, SJConstants.XP_SJ_CT_ERROR, GameConstants.INVALID_SEAT);
	}

	@Override
	public void send_history(int seat_index) {
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_LI_SHI_CARD);
		LiShiCard_yzsj.Builder history_card = LiShiCard_yzsj.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Card_Arrary_yzsj.Builder card_array = Card_Arrary_yzsj.newBuilder();
			for (int j = 0; j < 2; j++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int x = 0; x < this._history_out_count[i][j]; x++) {
					cards_card.addItem(_history_out_card[i][j][x]);
				}
				card_array.addCardData(cards_card);
			}

			history_card.addCardData(card_array);
		}
		history_card.setBankerPlayer(this.GRR._banker_player);
		roomResponse.setCommResponse(PBUtil.toByteString(history_card));
		if (seat_index == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}
	}

	/**
	 * 测试线上 实际牌
	 */
	@Override
	public void testRealyCard(int[] realyCards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	@Override
	public void testSameCard(int[] cards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = 0;
			}

		}
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
			GRR._card_count[i] = count;
		}
		for (int j = 0; j < _di_pai_count; j++) {
			_di_pai[j] = _repertory_card[this.get_hand_card_count_init() + j] + 0x100;
			_init_di_pai[j] = _repertory_card[this.get_hand_card_count_init() + j];
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
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
			roomResponse.setType(SJConstants.RESPONSE_XPSJ_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setRoomInfo(getRoomInfo());
			SendCard_yzsj.Builder send_card = SendCard_yzsj.newBuilder();
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
			Refresh_Color_count(index);
		}

		// 回放
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		SendCard_yzsj.Builder send_card = SendCard_yzsj.newBuilder();
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

		if (GRR._card_count[0] < this.get_hand_card_count_max()) {
			schedule(ID_TIMER_SEND_CARD_SEND, SheduleArgs.newArgs(), 100);
		}

	}

	public void send_di_pai_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_DI_PAI_DATA);
		DiPaiCard_yzsj.Builder di_pai_data = DiPaiCard_yzsj.newBuilder();
		for (int i = 0; i < _di_pai_count; i++) {
			di_pai_data.addCardData(_di_pai[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(di_pai_data));
		this.send_response_to_player(seat_index, roomResponse);
	}

	// 发送特效
	public void send_effect_type(int seat_index, int type, int data, int to_player) {
		// type=1：隐藏叫主面板 2：毙牌
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_EFFECT_TYPE);
		effect_type_xpsj.Builder effect = effect_type_xpsj.newBuilder();
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

	// 发送主牌
	public void send_zhu_data(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_ZHU_CARD);
		Zhu_card_Data_yzsj.Builder zhu_data = Zhu_card_Data_yzsj.newBuilder();
		for (int i = 0; i < this._call_banker_card_count; i++) {
			zhu_data.addCardData(this._call_baker_data[i]);
		}
		zhu_data.setZhuType(this._logic.GetCardColor(_call_baker_data[0]));
		zhu_data.setBankerSeat(this.GRR._banker_player);
		roomResponse.setCommResponse(PBUtil.toByteString(zhu_data));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	public void Refresh_Color_count(int seat_index) {
		if (this._select_dang[seat_index] == GAME_OPREATE_CALL_TYPE_NO) {
			send_effect_type(seat_index, EFFECT_CALL_YIN_CANG, 0, seat_index);
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_ZHU_COUNT);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		Zhu_Count_yzsj.Builder color_count = Zhu_Count_yzsj.newBuilder();
		int big_king_count = 0;
		int samll_king_count = 0;
		int color_num[] = new int[4];
		int color_shi_num[] = new int[4];

		for (int j = 0; j < GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] == 0x4F) {
				big_king_count++;
			} else if (GRR._cards_data[seat_index][j] == 0x4E) {
				samll_king_count++;
			} else {
				color_num[this._logic.GetCardColor(GRR._cards_data[seat_index][j])]++;
			}

			if (this._logic.GetCardValue(GRR._cards_data[seat_index][j]) == this._logic._chang_zhu_three) {
				color_shi_num[this._logic.GetCardColor(GRR._cards_data[seat_index][j])]++;
			}
		}

		if (big_king_count == 2 && this._cur_banker != seat_index) {
			color_count.addIsLiang(true);
		} else {
			color_count.addIsLiang(false);
		}
		if (samll_king_count == 2 && this._cur_banker != seat_index) {
			if (this._logic.GetCardColor(this._call_baker_data[0]) != 4) {
				color_count.addIsLiang(true);
			} else {
				color_count.addIsLiang(false);
			}
		} else {
			color_count.addIsLiang(false);
		}

		for (int i = 3; i >= 0; i--) {
			if (_call_banker_card_count == 0) {
				if (color_shi_num[i] > 0) {
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			} else if (_call_banker_card_count == 1) {
				if (color_shi_num[i] == 2 && this._cur_banker != seat_index) {
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			} else {
				if (color_shi_num[i] == 2 && i > this._logic.GetCardColor(this._call_baker_data[0]) && this._cur_banker != seat_index) {
					color_count.addIsLiang(true);
				} else {
					color_count.addIsLiang(false);
				}
			}
		}
		if (_game_status == SJConstants.GS_XPSJ_CALL_BANKER) {
			if (this._select_dang[seat_index] == -1) {
				color_count.addIsLiang(true);
			} else {
				color_count.addIsLiang(false);
			}

		} else {
			color_count.addIsLiang(false);
		}
		if (_call_banker_card_count > 0) {
			color_count.setType(2);
		} else {
			color_count.setType(1);
		}
		// 发送数据
		roomResponse.setCommResponse(PBUtil.toByteString(color_count));
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);
	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_SEND_CARD_SEND: {
			send_card(2);
			if (GRR._card_count[0] >= this.get_hand_card_count_max()) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(SJConstants.RESPONSE_XPSJ_SEND_CARD_END);
				SendCardEnd_yzsj.Builder send_card_end = SendCardEnd_yzsj.newBuilder();
				send_card_end.setCardCount(8);
				send_card_end.setIsAnimal(1);
				roomResponse.setCommResponse(PBUtil.toByteString(send_card_end));
				this.send_response_to_room(roomResponse);
				GRR.add_room_response(roomResponse);

				this._game_status = SJConstants.GS_XPSJ_CALL_BANKER;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this.Refresh_Color_count(i);
				}

				if (this.has_rule(GameConstants.GAME_RULE_XPSJ_NO_CHANG_ZHU_REPATH)) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._logic.get_chang_zhu_count(this.GRR._cards_data[i], this.GRR._card_count[i]) == 0) {
							schedule(ID_TIMER_RE_SEND_CARD, SheduleArgs.newArgs(), 2000);
							for (int j = 0; j < this.getTablePlayerNumber(); j++) {
								send_effect_type(j, EFFECT_CALL_YIN_CANG, 0, j);
							}
							return;
						}
					}
					schedule(ID_TIMER_SEND_CARD_END_WAIT, SheduleArgs.newArgs(), 10000);
				} else {
					schedule(ID_TIMER_SEND_CARD_END_WAIT, SheduleArgs.newArgs(), 10000);
				}

			}
			return;
		}
		case ID_TIMER_SEND_CARD_END_WAIT: {
			if (this._call_banker_card_count > 0) {
				out_card_begin();

			}
			return;
		}
		case ID_TIMER_RE_SEND_CARD: {
			this.Reset();
			send_card(1);
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

	public void load_player_info_data_game_start(GameStart_yzsj.Builder roomResponse) {
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

	public void load_player_info_data_reconnect(TableResponse_yzsj.Builder roomResponse) {
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

	public void load_player_info_data_game_end(PukeGameEndyzsj.Builder roomResponse) {
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

	public void Refresh_pai_score(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_PAI_SCORE);
		roomResponse.setGameStatus(this._game_status);
		roomResponse.setRoomInfo(getRoomInfo());
		// 发送数据
		PaiFenData_yzsj.Builder pai_score_data = PaiFenData_yzsj.newBuilder();

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
			if (this._logic.GetCardColor(_pai_score_card[i]) == 1) {
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
			if (this._logic.GetCardColor(_pai_score_card[i]) == 2) {
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
			if (this._logic.GetCardColor(_pai_score_card[i]) == 3) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);
		// 先传王
		count = 0;
		cards.clear();
		for (int i = 0; i < this._pai_score_count; i++) {
			if (this._logic.GetCardColor(_pai_score_card[i]) == 4) {
				cards.addItem(_pai_score_card[i]);
				count++;
			}
		}
		pai_score_data.addCardsData(cards);
		pai_score_data.addCardsCount(count);
		pai_score_data.setYuScore(_pai_score);
		roomResponse.setCommResponse(PBUtil.toByteString(pai_score_data));

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
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
		Arrays.fill(end_score, 0);
		// 计算分数
		if (reason == GameConstants.Game_End_NORMAL) {
			cal_score(end_score, seat_index);
		}

		if (GRR != null) {
			this.operate_player_data();
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_GAME_END);
		roomResponse.setRoomInfo(getRoomInfo());
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		game_end.setRoomInfo(getRoomInfo());

		load_player_info_data(roomResponse);
		PukeGameEndyzsj.Builder gameend_sj = PukeGameEndyzsj.newBuilder();
		load_player_info_data_game_end(gameend_sj);
		gameend_sj.setRoomInfo(this.getRoomInfo());
		int xian_get_score = 0;
		if (GRR != null) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i != GRR._banker_player || i != this._other_banker) {
					xian_get_score += _get_score[i];
				}
			}
			if (GRR._banker_player != GameConstants.INVALID_SEAT) {

			}

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

			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder score_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this._user_get_score_count[i]; j++) {
				score_card.addItem(_user_get_score_card[i][j]);
			}
			game_end.addGameScore(end_score[i]);
			gameend_sj.addGameScore(end_score[i]);
			gameend_sj.addZhuaFen(this._get_score[i]);
			gameend_sj.addScoreCard(score_card);
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			Arrays.fill(_user_get_score_card[i], GameConstants.INVALID_CARD);
			_user_get_score_count[i] = 0;
		}

		if (!is_sys()) {
			GRR = null;
		}
		// 错误断言
		return false;
	}

	public void cal_score(int end_score[], int seat_index) {
		int all_socre = 240;
		if(this.has_rule(GameConstants.GAME_RULE_YZSJ_300)){
			all_socre = 300;
		}
		int base_score = all_socre / this.getTablePlayerNumber();
		int other_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			end_score[i] += _get_score[i] - base_score;
			if (_get_score[i] == 0 && this.has_rule(GameConstants.GAME_RULE_XPSJ_GUANG_TOU_DOUBLE)) {
				other_score += base_score;
				end_score[i] -= base_score;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_XPSJ_GUANG_TOU_DOUBLE)) {
			int win_num = 0;
			int win_total_score = 0;
			int other_score_div = 0;
			int win_total_div = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_get_score[i] > base_score) {
					win_total_score += _get_score[i];
				}
			}
			win_total_div = win_total_score / 5;

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_get_score[i] > base_score) {
					end_score[i] += 5 * (other_score / 5 * _get_score[i] / win_total_score);
					other_score_div += 5 * (other_score / 5 * _get_score[i] / win_total_score);
				}
			}

			if (other_score_div < other_score) {
				int max_get_score = 0;
				int seat = GameConstants.INVALID_SEAT;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (_get_score[i] > max_get_score) {
						max_get_score = _get_score[i];
						seat = i;
					}
				}
				end_score[seat] += other_score - other_score_div;
			}
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
			OutCardDatayzsj.Builder outcarddata = OutCardDatayzsj.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(SJConstants.RESPONSE_XPSJ_OUT_CARD);// 201
			roomResponse.setGameStatus(_game_status);
			roomResponse.setTarget(seat_index);
			roomResponse.setRoomInfo(this.getRoomInfo());
			for (int j = 0; j < count; j++) {
				outcarddata.addCardsData(cards_data[j]);
			}
			// 上一出牌数据
			outcarddata.setPrCardsCount(this._turn_out_card_count);
			outcarddata.setTurnCardsCount(this._turn_out_card_count);
			for (int i = 0; i < this._turn_out_card_count; i++) {
				outcarddata.addPrCardsData(this._origin_out_card_data[i]);
				outcarddata.addTurnCardsData(this._turn_out_card_data[i]);
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
			outcarddata.setIsMustA(_have_5_must_A);

			if (this.has_rule(GameConstants.GAME_RULE_XPSJ_HAVE_SCORE_BI_GUAN) && this._table_score > 0) {
				outcarddata.setIsScoreMust(true);
			} else {
				outcarddata.setIsScoreMust(false);
			}
			if (_turn_out_card_count == 0) {
				outcarddata.setIsCurrentFirstOut(1);
			} else {
				outcarddata.setIsCurrentFirstOut(0);
			}
			if (index == _current_player) {
				int can_out_card_data[] = new int[get_hand_card_count_max()];
				int can_out_count = 0;
				if (_out_card_times[index] == 0 && _current_player == GRR._banker_player && _logic._zhu_type == 4) {
					can_out_count = _logic.player_can_out_card_first_yz(GRR._cards_data[_current_player], GRR._card_count[_current_player],
							can_out_card_data, this);

				} else if (_out_card_times[_current_player] == 0 && _current_player == GRR._banker_player && _call_banker_card_count > 1) {
					can_out_count = _logic.player_can_out_card_first_yz(GRR._cards_data[_current_player], GRR._card_count[_current_player],
							can_out_card_data, this);

				} else {
					int must_out_data[] = new int[get_hand_card_count_max()];
					int must_out_count[] = new int[1];
					 can_out_count = _logic.Player_Can_out_card(GRR._cards_data[_current_player],GRR._card_count[_current_player], _origin_out_card_data,
					 _origin_out_card_count,can_out_card_data, this._turn_out_card_count,this._turn_out_card_data, must_out_data,
					 must_out_count, this._table_score, _have_5_must_A,false);
					//can_out_count = _logic.player_can_out_card_yz(GRR._cards_data[_current_player], GRR._card_count[_current_player],
					//		_origin_out_card_data, _origin_out_card_count, can_out_card_data, this._turn_out_card_count, this._turn_out_card_data,
					//		must_out_data, must_out_count, this._table_score, _have_5_must_A, false);
				}
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
		OutCardDatayzsj.Builder outcarddata = OutCardDatayzsj.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_OUT_CARD);// 201
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

		if (type == MsgConstants.REQUST_XPSJ_OPERATE) {
			Opreate_RequestWsk_Xpsj req = PBUtil.toObject(room_rq, Opreate_RequestWsk_Xpsj.class);
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
			// deal_get_di_pai(seat_index);
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
			GRR._cards_data[seat_index][GRR._card_count[seat_index]++] = this._di_pai[i];
		}
		this._logic.SortCardList(GRR._cards_data[seat_index], GRR._card_count[seat_index]);
		RefreshCard(seat_index);
	}

	public void deal_put_di_pai(int seat_index, List<Integer> list) {
		if (seat_index != GRR._banker_player) {
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
		this.RefreshCard(GRR._banker_player);
		this._current_player = GRR._banker_player;
		this.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.LLDQ_CT_ERROR, GameConstants.INVALID_SEAT);

	}

	public void deal_get_di_pai(int seat_index) {
		if (seat_index != GRR._banker_player) {
			return;
		}
		deal_send_di_pai(seat_index);

		// 拿完底牌操作
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_MAIDI_OPREATE);
		MaiDiOpreate_yzsj.Builder mai_di_opetate = MaiDiOpreate_yzsj.newBuilder();
		mai_di_opetate.addOpreate(1);
		mai_di_opetate.addOpreate(3);
		roomResponse.setCommResponse(PBUtil.toByteString(mai_di_opetate));
		this.send_response_to_player(GRR._banker_player, roomResponse);
	}

	public void deal_tou_xiang(int seat_index) {
		if (seat_index != GRR._banker_player) {
			return;
		}
		if (_first_call_banker != seat_index) {
			return;
		}
		int delay = 2;
		GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
	}

	public void deal_call_banker(int seat_index, int select_type) {

		if (_game_status != SJConstants.GS_XPSJ_SEND_CARD && _game_status != SJConstants.GS_XPSJ_CALL_BANKER) {
			return;
		}
		// 发牌状态不能不叫
		if (_game_status == SJConstants.GS_XPSJ_SEND_CARD && select_type == 6) {
			return;
		}
		if (this._select_dang[seat_index] == GAME_OPREATE_CALL_TYPE_NO) {
			return;
		}
		if (_select_dang[seat_index] != -1 && select_type == GAME_OPREATE_CALL_TYPE_NO) {
			return;
		}
		if (seat_index == this._cur_banker) {
			return;
		}

		int big_king_count = 0;
		int samll_king_count = 0;
		int color_num[] = new int[4];
		int color_shi_num[] = new int[4];

		for (int j = 0; j < GRR._card_count[seat_index]; j++) {
			if (GRR._cards_data[seat_index][j] == 0x4F) {
				big_king_count++;
			} else if (GRR._cards_data[seat_index][j] == 0x4E) {
				samll_king_count++;
			} else {
				color_num[this._logic.GetCardColor(GRR._cards_data[seat_index][j])]++;
			}

			if (this._logic.GetCardValue(GRR._cards_data[seat_index][j]) == this._logic._chang_zhu_three) {
				color_shi_num[this._logic.GetCardColor(GRR._cards_data[seat_index][j])]++;
				;
			}
		}
		this._select_dang[seat_index] = select_type;
		if (select_type == 4) {
			if (samll_king_count == 2 && this._logic.GetCardColor(_call_baker_data[0]) != 4) {
				_call_banker_card_count = 0;
				_call_baker_data[_call_banker_card_count++] = 0x4E;
				_call_baker_data[_call_banker_card_count++] = 0x4E;
			} else {
				this.send_error_notify(seat_index, 2, "操作不正确");
				return;
			}
		} else if (select_type == 5) {
			if (big_king_count == 2) {
				_call_banker_card_count = 0;
				_call_baker_data[_call_banker_card_count++] = 0x4F;
				_call_baker_data[_call_banker_card_count++] = 0x4F;
			} else {
				this.send_error_notify(seat_index, 2, "操作不正确");
				return;
			}
		} else if (select_type < 4) {
			if (_call_banker_card_count == 0) {
				if (color_shi_num[select_type] > 0) {
					_call_baker_data[_call_banker_card_count++] = select_type * 16 + this._logic._chang_zhu_three;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
					return;
				}

			} else if (color_shi_num[select_type] == 2) {
				_call_banker_card_count = 0;
				_call_baker_data[_call_banker_card_count++] = select_type * 16 + this._logic._chang_zhu_three;
				_call_baker_data[_call_banker_card_count++] = select_type * 16 + this._logic._chang_zhu_three;
			} else {
				if (color_shi_num[select_type] == 2 && select_type > this._logic.GetCardColor(_call_baker_data[0])) {
					_call_banker_card_count = 0;
					_call_baker_data[_call_banker_card_count++] = select_type * 16 + this._logic._chang_zhu_three;
					_call_baker_data[_call_banker_card_count++] = select_type * 16 + this._logic._chang_zhu_three;
				} else {
					this.send_error_notify(seat_index, 2, "操作不正确");
				}
			}
		} else if (select_type == 6) {
			this.Refresh_Color_count(seat_index);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_select_dang[i] != 6) {
					return;
				}
			}
			int delay = 3;
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_DRAW), delay, TimeUnit.SECONDS);
			return;
		} else {
			this.send_error_notify(seat_index, 2, "操作不正确");
			return;
		}

		_cur_banker = seat_index;
		_first_call_banker = seat_index;
		send_call_data(seat_index, GameConstants.INVALID_SEAT);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Refresh_Color_count(i);
		}

		if (_call_baker_data[0] != 0x4F) {
			this.cancelShedule(ID_TIMER_SEND_CARD_END_WAIT);
			schedule(ID_TIMER_SEND_CARD_END_WAIT, SheduleArgs.newArgs(), 10000);
		} else if (_call_baker_data[0] == 0x4F) {
			out_card_begin();
			this.cancelShedule(ID_TIMER_SEND_CARD_END_WAIT);
		}
	}

	public void send_call_data(int seat_index, int to_player) {
		if (to_player == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(SJConstants.RESPONSE_XPSJCALL_BANKER);
			CallBankerResponse_yzsj.Builder call_bnaker_response = CallBankerResponse_yzsj.newBuilder();
			call_bnaker_response.setSeatIndex(seat_index);

			for (int i = 0; i < _call_banker_card_count; i++) {
				call_bnaker_response.addCardsData(_call_baker_data[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_bnaker_response));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(SJConstants.RESPONSE_XPSJCALL_BANKER);
			CallBankerResponse_yzsj.Builder call_bnaker_response = CallBankerResponse_yzsj.newBuilder();
			call_bnaker_response.setSeatIndex(seat_index);

			for (int i = 0; i < _call_banker_card_count; i++) {
				call_bnaker_response.addCardsData(_call_baker_data[i]);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(call_bnaker_response));
			this.send_response_to_player(seat_index, roomResponse);
		}

	}

	public void RefreshCard(int to_player) {
		if (to_player == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(SJConstants.RESPONSE_XPSJ_REFRES_CARD_DATA);
			// 发送数据
			RefreshCardData_yzsj.Builder refresh_card = RefreshCardData_yzsj.newBuilder();
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
			roomResponse.setType(SJConstants.RESPONSE_XPSJ_REFRES_CARD_DATA);
			// 发送数据
			RefreshCardData_yzsj.Builder refresh_card = RefreshCardData_yzsj.newBuilder();
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

	public void RefreshScore(int to_player, int is_delay) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJConstants.RESPONSE_XPSJ_SCORE);
		// 发送数据
		TableScore_yzsj.Builder refresh_score = TableScore_yzsj.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			refresh_score.addGetScore(_get_score[i]);
		}
		refresh_score.setTableScore(_pai_score);
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
