package com.cai.game.mj.hunan.taojiang;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_TaoJiang extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	public int joker_card_1 = 0;
	public int joker_card_index_1 = -1;
	public int joker_card_2 = 0;
	public int joker_card_index_2 = -1;
	public int ding_wang_card = 0;
	public int ding_wang_card_index = -1;

	// 用来储存2个骰子的点数
	public int[] tou_zi_dian_shu = new int[2];

	// 有接炮时，不胡，存储一下，胡牌时的牌型分
	public int[] score_when_abandoned_jie_pao = new int[getTablePlayerNumber()];

	public HandlerSelectMagicCard_TaoJiang _handler_select_magic_card;
	@SuppressWarnings("rawtypes")
	public HandlerGangDispatchCard_TaoJiang _handler_gang_xuan_mei;
	public HandlerBaoTing_TaoJiang _handler_bao_ting;
	public HandlerPiao_TaoJiang _handler_piao;
	public HandlerXiaoJieSuan_TaoJiang _handler_xiao_jie_suan;

	// 胡牌时，每个人手上的王牌数目
	public int[] joker_count_when_win = new int[getTablePlayerNumber()];

	// 玩家起手是否听牌，用来处理‘报听’
	public boolean[] qi_shou_ting = new boolean[getTablePlayerNumber()];

	// 玩家是的点击了‘报听’
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];

	// 玩家一手牌，可以胡的最大牌型分，每次获取听牌数据的时候，进行更新
	public int[] max_win_score = new int[getTablePlayerNumber()];

	// 杠牌时，存储的最大牌型分的牌
	public int[] win_card_at_gang = new int[getTablePlayerNumber()];

	// 赢牌时，判断是哪种类型，如果是开杠时被人抢了，或者杠翻出来的牌被人胡了(都是抢杠胡)，需要判断开杠者手牌的最大牌型分
	// 1，自摸；2，抓炮；3，杠开；4，抢杠；5，杠胡；
	public int card_type_when_win;

	// 有人胡牌时，牌桌上最后一个出牌人或抓牌人或杠牌人
	public int seat_index_when_win;

	// 本圈是否可以抢杠
	public boolean[] can_qiang_gang = new boolean[getTablePlayerNumber()];

	// 摸牌统计
	public int[] mo_pai_count = new int[getTablePlayerNumber()];

	// 报听状态下，接炮点了过
	public boolean[] ting_state_pass_jie_pao = new boolean[getTablePlayerNumber()];

	/**
	 * 各种时间设置 ，单位毫秒
	 */
	public final int DELAY_GAME_START = 500;
	public final int DELAY_GAME_FINISH = 500;
	public final int DELAY_AUTO_OPERATE = 300;
	public final int DELAY_GANG_DISPATCH = 500;

	public boolean is_game_start = false;

	public int distance_to_ding_wang_card = 108;

	public RoomResponse.Builder saved_room_response = null;
	public GameEndResponse.Builder saved_game_end_response = null;

	public boolean can_reconnect = false;

	// 是否是杠牌之后的自动托管
	public boolean[] is_gang_tuo_guan = new boolean[getTablePlayerNumber()];

	public Table_TaoJiang(MJType type) {
		super(type);
	}

	public boolean is_card_258(int card) {
		if (_logic == null) {
			return false;
		}
		int card_value = _logic.get_card_value(card);
		if (card_value == 2 || card_value == 5 || card_value == 8) {
			return true;
		}
		return false;
	}

	@Override
	public boolean handler_request_trustee(int seat_index, boolean isTrustee, int Trustee_type) {
		if (!is_match() && !isClubMatch() && !isCoinRoom()) {
			if (_playerStatus == null || istrustee == null) {
				send_error_notify(seat_index, 2, "游戏未开始,无法自动胡牌托管!");
				return false;
			}

			if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
				send_error_notify(seat_index, 2, "游戏还未开始,无法自动胡牌托管!");
				return false;
			}

			if (is_bao_ting[seat_index]) {
				send_error_notify(seat_index, 2, "您已经报听,无法自动胡牌托管!");
				return false;
			}

			if (is_gang_tuo_guan[seat_index]) {
				send_error_notify(seat_index, 2, "您是杠后的自动胡牌托管,不能取消自动胡牌托管!");
				return false;
			}

			if (_handler != null && _handler == _handler_chi_peng) {
				send_error_notify(seat_index, 2, "吃碰过后,出牌之前,无法自动胡牌托管!");
				return false;
			}

			if (_handler != null && _handler == _handler_dispath_card && _current_player == seat_index) {
				send_error_notify(seat_index, 2, "抓牌之后,出牌之前,无法自动胡牌托管!");
				return false;
			}

			if (istrustee[seat_index] == false) {
				if (_playerStatus[seat_index]._hu_card_count <= 0) {
					send_error_notify(seat_index, 2, "您还未听牌,无法自动胡牌托管!");
					return false;
				}
			}
		}

		istrustee[seat_index] = isTrustee;

		// 这个取消托管的操作，由客户端主动发起，如果玩家是托管状态，并点了操作，取消定时任务
		if (_trustee_schedule != null && _trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(seat_index);
		roomResponse.setIstrustee(isTrustee);

		send_response_to_player(seat_index, roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (istrustee[seat_index]) {
			int card_data = GameConstants.INVALID_VALUE;
			if (_handler != null) {
				if (_handler == _handler_dispath_card) {
					card_data = _send_card_data;
				}
				if (_handler == _handler_out_card_operate) {
					card_data = _out_card_data;
				}
				if (_handler == _handler_gang_xuan_mei) {
					card_data = win_card_at_gang[seat_index];
				}
				if (_handler == _handler_gang) {
					card_data = _provide_card;
				}
			}

			PlayerStatus curPlayerStatus = _playerStatus[seat_index];
			if (curPlayerStatus.has_action()) {
				operate_player_action(seat_index, true);

				if (curPlayerStatus.has_zi_mo() && card_data != GameConstants.INVALID_VALUE) {
					exe_jian_pao_hu(seat_index, GameConstants.WIK_ZI_MO, card_data);
				} else if (curPlayerStatus.has_chi_hu() && card_data != GameConstants.INVALID_VALUE) {
					exe_jian_pao_hu(seat_index, GameConstants.WIK_CHI_HU, card_data);
				} else {
					exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, card_data);
				}
			}
		}

		return true;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_TaoJiang();
		_handler_dispath_card = new HandlerDispatchCard_TaoJiang();
		_handler_gang = new HandlerGang_TaoJiang();
		_handler_out_card_operate = new HandlerOutCardOperate_TaoJiang();

		_handler_select_magic_card = new HandlerSelectMagicCard_TaoJiang();
		_handler_gang_xuan_mei = new HandlerGangDispatchCard_TaoJiang<Table_TaoJiang>();
		_handler_bao_ting = new HandlerBaoTing_TaoJiang();
		_handler_piao = new HandlerPiao_TaoJiang();
		_handler_xiao_jie_suan = new HandlerXiaoJieSuan_TaoJiang();
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		tou_zi_dian_shu = new int[2];
		// 洗完牌之后，发牌之前，需要摇骰子
		show_tou_zi(GRR._banker_player);

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	/**
	 * 后期需要添加的摇骰子的效果
	 * 
	 * @param table
	 * @param seat_index
	 */
	@Override
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			tou_zi_dian_shu[0] = 3;
			tou_zi_dian_shu[1] = 1;
		}
	}

	/**
	 * 在牌桌上显示摇骰子的效果
	 * 
	 * @param tou_zi_one
	 *            骰子1的点数
	 * @param tou_zi_two
	 *            骰子2的点数
	 * @param time_for_animate
	 *            动画时间
	 * @param time_for_fade
	 *            动画保留时间
	 * @return
	 */
	@Override
	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		if (GRR != null)
			roomResponse.setTarget(GRR._banker_player);
		else
			roomResponse.setTarget(0);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2); // get牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			// 离地王牌还有多少张
			if (distance_to_ding_wang_card > 0) {
				roomResponse.setOperateLen(distance_to_ding_wang_card);
			}

			// 实时存储牌桌上的数据，方便回放时，任意进度读取
			operate_player_cards_record(seat_index, 1);

			send_response_to_other(seat_index, roomResponse);

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}

			GRR.add_room_response(roomResponse);

			return send_response_to_player(seat_index, roomResponse);
		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}
			return send_response_to_player(to_player, roomResponse);
		}
	}

	public boolean operate_show_card(int seat_index, int type, int count, int cards[], int to_player, boolean reconnect) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);
		roomResponse.setCardCount(count);

		if (count > 0 && !reconnect) {
			// 离地王牌还有多少张
			if (distance_to_ding_wang_card > 0) {
				roomResponse.setOperateLen(distance_to_ding_wang_card);
			}
		}

		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}

		GRR.add_room_response(roomResponse);

		if (to_player == GameConstants.INVALID_SEAT) {
			return send_response_to_room(roomResponse);
		} else {
			return send_response_to_player(to_player, roomResponse);
		}
	}

	protected boolean on_game_start_real() {
		joker_card_1 = 0;
		joker_card_index_1 = -1;
		joker_card_2 = 0;
		joker_card_index_2 = -1;
		ding_wang_card = 0;
		ding_wang_card_index = -1;

		is_game_start = true;

		score_when_abandoned_jie_pao = new int[getTablePlayerNumber()];
		joker_count_when_win = new int[getTablePlayerNumber()];

		qi_shou_ting = new boolean[getTablePlayerNumber()];
		is_bao_ting = new boolean[getTablePlayerNumber()];

		max_win_score = new int[getTablePlayerNumber()];

		win_card_at_gang = new int[getTablePlayerNumber()];
		Arrays.fill(win_card_at_gang, -1);

		card_type_when_win = -1;
		seat_index_when_win = -1;

		can_qiang_gang = new boolean[getTablePlayerNumber()];
		Arrays.fill(can_qiang_gang, true);

		mo_pai_count = new int[getTablePlayerNumber()];
		ting_state_pass_jie_pao = new boolean[getTablePlayerNumber()];
		distance_to_ding_wang_card = 108;

		is_gang_tuo_guan = new boolean[getTablePlayerNumber()];

		_game_status = GameConstants.GS_MJ_PLAY;

		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			// 重新装载玩家信息
			load_player_info_data(roomResponse);

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		load_room_info_data(roomResponse);
		load_common_status(roomResponse);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);

		GRR.add_room_response(roomResponse);

		exe_select_magic_card(GRR._banker_player);

		return true;
	}

	@Override
	protected boolean on_game_start() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
		}

		if (has_rule(Constants_TaoJiang.GAME_RULE_PIAO)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
			return true;
		}

		_game_status = GameConstants.GS_MJ_YAO_TOU_ZI;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SHAKE_TOU_ZI);
		load_room_info_data(roomResponse);
		send_response_to_room(roomResponse);

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], 500, 200);

		GameSchedule.put(() -> {
			on_game_start_real();
		}, 500 + 200 + 300, TimeUnit.MILLISECONDS);

		return true;
	}

	public void process_xiao_jie_suan(int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		// 游戏结束的时候，清掉听牌状态
		// is_bao_ting = new boolean[getTablePlayerNumber()];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间

		if (GRR != null && is_game_start) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count); // 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();

				for (int card : GRR._player_niao_cards[i]) {
					if (card > 0)
						pnc.addItem(card);
				}

				for (int card : GRR._player_niao_cards_fei[i]) {
					if (card > 0)
						pnc.addItem(card);
				}

				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();

				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]); // 放炮的人？
				game_end.addGangScore(lGangScore[i]); // 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			real_reason = GameConstants.Game_End_DRAW; // 流局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		// 得分总的
		roomResponse.setGameEnd(game_end);

		saved_room_response = roomResponse;
		saved_game_end_response = game_end;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		if (reason != GameConstants.Game_End_NORMAL && reason != GameConstants.Game_End_DRAW)
			process_xiao_jie_suan(reason);

		send_response_to_room(saved_room_response);

		record_game_round(saved_game_end_response);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
		}

		if (end == false)
			can_reconnect = true;

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		is_game_start = false;

		return true;
	}

	@Override
	public boolean exe_finish(int reason) {
		_end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		// TODO 操蛋的，在处理小结算数据之前，需要把_game_status设置成GAME_STATUS_WAIT
		_game_status = GameConstants.GAME_STATUS_WAIT;
		process_xiao_jie_suan(reason);

		set_handler(_handler_xiao_jie_suan);

		_handler_xiao_jie_suan.exe(this);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if (can_reconnect && get_players()[seat_index] != null && _release_scheduled == null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);

			return true;
		} else if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;

		return true;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA && card < GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_NEW_TING
				&& card < (GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA)) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING;
		} else if (card > (GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA)
				&& card < (GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI)) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (card > (GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI)
				&& card < (GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG)) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		return card;
	}

	protected void exe_select_magic_card(int banker) {
		set_handler(_handler_select_magic_card);
		_handler_select_magic_card.reset_status(banker);
		_handler_select_magic_card.exe(this);
	}

	protected void exe_gang_xuan_mei(int seat_index, int xuan_mei_count) {
		set_handler(_handler_gang_xuan_mei);
		_handler_gang_xuan_mei.reset_status(seat_index, xuan_mei_count);
		_handler_gang_xuan_mei.exe(this);
	}

	protected void exe_bao_ting(int type, int seat_index, int card_data) {
		set_handler(_handler_bao_ting);
		_handler_bao_ting.reset_status(type, seat_index, card_data);
		_handler_bao_ting.exe(this);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (is_bao_ting[_seat_index]) {
			boolean can_win = false;
			int ting_cards[] = _playerStatus[_seat_index]._hu_cards;
			int ting_count = _playerStatus[_seat_index]._hu_card_count;

			if (ting_count == 1 && ting_cards[0] == -1) {
				can_win = true;
			} else {
				for (int i = 0; i < ting_count; i++) {
					if (get_real_card(ting_cards[i]) == cur_card) {
						can_win = true;
						break;
					}
				}
			}
			if (!can_win)
				return 0;
		}

		if (card_type == Constants_TaoJiang.HU_CARD_TYPE_ZI_MO || card_type == Constants_TaoJiang.HU_CARD_TYPE_GANG_KAI) {
			return analyse_card_zi_mo_gang_kai(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index);
		} else {
			if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
				return analyse_card_jie_pao_eight_joker(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index);
			} else {
				return analyse_card_jie_pao_four_joker(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index);
			}
		}
	}

	public boolean is_fei_zi_mo() {
		if (card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_GANG_HU || card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG
				|| card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_JIE_PAO) {
			return true;
		}

		return false;
	}

	public boolean is_qi_shou(int card_type) {
		if (_out_card_count == 0 && card_type == Constants_TaoJiang.HU_CARD_TYPE_ZI_MO) {
			return true;
		}
		return false;
	}

	public int analyse_card_zi_mo_gang_kai(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (is_qi_shou(card_type)) {
			cbCardIndexTemp[_logic.switch_to_card_index(_send_card_data)]--;
			cards_index[_logic.switch_to_card_index(_send_card_data)]--;
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (card_type == Constants_TaoJiang.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_TaoJiang.CHR_ZI_MO);
		} else if (card_type == Constants_TaoJiang.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_TaoJiang.CHR_GANG_KAI);
		}

		int joker_count = 0;
		joker_count += cbCardIndexTemp[joker_card_index_1];
		if (joker_card_index_2 != -1) {
			joker_count += cbCardIndexTemp[joker_card_index_2];
		}

		int real_joker_count = 0;
		real_joker_count += cards_index[joker_card_index_1];
		if (joker_card_index_2 != -1) {
			real_joker_count += cards_index[joker_card_index_2];
		}

		joker_count_when_win[_seat_index] = joker_count;

		if (is_fei_zi_mo()) {
			joker_count_when_win[_seat_index] = real_joker_count;
			joker_count = real_joker_count;
		}

		boolean need_to_check_258 = true;
		boolean can_win = false;
		boolean has_normal_win = false;
		boolean has_tian_hu = false;
		boolean has_tian_tian_hu = false;

		if (weave_count == 0 && joker_count == 0 && ((_out_card_count == 0 && _seat_index == GRR._banker_player)
				|| (_seat_index != GRR._banker_player && mo_pai_count[_seat_index] == 1))) {
			boolean is_hei_tian_hu = check_hei_tian_hu(cbCardIndexTemp);
			if (is_hei_tian_hu) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_HEI_TIAN_HU);
				can_win = true;
			}
		}

		if (cbCardIndexTemp[ding_wang_card_index] == 4) {
			if (!is_fei_zi_mo() || cur_card != ding_wang_card) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_DI_DI_HU);

				int check_qi_xiao_dui = qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
				int check_ying_qi_xiao_dui = ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

				boolean tt_has_qi_dui = false;

				if (check_ying_qi_xiao_dui != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(check_ying_qi_xiao_dui);
					chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
					tt_has_qi_dui = true;
				} else if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(check_qi_xiao_dui);
					tt_has_qi_dui = true;
				}

				if (tt_has_qi_dui) {
					if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
						if (joker_count == 4) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
						} else if (joker_count >= 5) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
						}
					} else {
						if (joker_count == 3) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
						} else if (joker_count >= 4) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
						}
					}

					if (_out_card_count == 0 && _seat_index == GRR._banker_player) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_DAO_DI_HU);
					}
				}

				if (is_qi_shou(card_type)) {
					cards_index[_logic.switch_to_card_index(_send_card_data)]++;
				}

				return GameConstants.WIK_CHI_HU;
			}
		}

		if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
			if (joker_count == 4) {
				has_tian_hu = true;
			} else if (joker_count >= 5) {
				has_tian_tian_hu = true;
			}
		} else {
			if (joker_count == 3) {
				has_tian_hu = true;
			} else if (joker_count >= 4) {
				has_tian_tian_hu = true;
			}
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) {
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		int winCardChr = 0;

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		int check_qi_xiao_dui = 0;
		int check_ying_qi_xiao_dui = 0;

		if (is_fei_zi_mo()) {
			check_qi_xiao_dui = qi_xiao_dui_no_cur_card(cards_index, weaveItems, weave_count, cur_card);
			check_ying_qi_xiao_dui = ying_qi_xiao_dui_no_cur_card(cards_index, weaveItems, weave_count, cur_card);
		} else {
			check_qi_xiao_dui = qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
			check_ying_qi_xiao_dui = ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		}

		boolean can_win_qi_dui_with_magic = check_qi_xiao_dui != GameConstants.WIK_NULL;
		boolean can_win_qi_dui_without_magic = check_ying_qi_xiao_dui != GameConstants.WIK_NULL;

		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		boolean can_win_jiang_yi_se_without_magic = _logic.check_sg_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		boolean exist_eat = exist_eat(weaveItems, weave_count);

		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat;
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat;

		boolean can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& (can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_peng_peng_hu_with_magic);
		boolean can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& (can_win_without_magic || can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_peng_peng_hu_without_magic);

		boolean can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, magic_card_count);
		boolean can_win_258_without_magic = AnalyseCardUtil.analyse_258_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		boolean can_win_ying_da_hu = false;
		boolean can_win_ruan_da_hu = false;
		boolean th_can_win_ying_da_hu = false;

		boolean th_can_win_without_magic = false;
		boolean th_can_win_qi_dui_without_magic = false;
		boolean th_can_win_jiang_yi_se_without_magic = false;
		boolean th_can_win_peng_peng_hu_without_magic = false;
		boolean th_can_win_qing_yi_se_without_magic = false;
		boolean th_can_win_258_without_magic = false;

		if (has_tian_tian_hu) {
		} else if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER) && has_tian_hu) {
		} else if (has_tian_hu) {
			cbCardIndexTemp[joker_card_index_1] = 0;

			th_can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

			th_can_win_qi_dui_without_magic = false;

			th_can_win_jiang_yi_se_without_magic = _logic.check_sg_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count)
					&& is_card_258(joker_card_1);

			boolean th_can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

			th_can_win_peng_peng_hu_without_magic = th_can_ying_peng_hu && !exist_eat;

			cbCardIndexTemp[joker_card_index_1] = 3;
			th_can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
					&& (th_can_win_without_magic || th_can_win_qi_dui_without_magic || th_can_win_jiang_yi_se_without_magic
							|| th_can_win_peng_peng_hu_without_magic);
			cbCardIndexTemp[joker_card_index_1] = 0;

			th_can_win_258_without_magic = AnalyseCardUtil.analyse_258_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

			if (th_can_win_qi_dui_without_magic || th_can_win_jiang_yi_se_without_magic || th_can_win_qing_yi_se_without_magic
					|| th_can_win_peng_peng_hu_without_magic) {
				th_can_win_ying_da_hu = true;
			}
		}

		if (can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_qing_yi_se_without_magic
				|| can_win_peng_peng_hu_without_magic) {
			can_win_ying_da_hu = true;
		}

		if (can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			can_win_ruan_da_hu = true;
		}

		int max_score = 0;
		int tmp_score = 0;
		int caculate_type = 0;

		if (th_can_win_ying_da_hu || th_can_win_258_without_magic || can_win_ying_da_hu || can_win_ruan_da_hu) {
			need_to_check_258 = false;
			can_win = true;
			has_normal_win = true;
		}

		if (th_can_win_ying_da_hu) {
			ChiHuRight tmpChr = new ChiHuRight();
			tmpChr.set_empty();

			tmpChr.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);

			if (th_can_win_jiang_yi_se_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (th_can_win_qing_yi_se_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}
			if (th_can_win_peng_peng_hu_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
			}

			if (has_tian_tian_hu) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
			} else if (has_tian_hu) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
			}

			tmp_score = get_pai_xing_fen(tmpChr, _seat_index);
			if (max_score < tmp_score) {
				max_score = tmp_score;
				caculate_type = 1;
			}
		}
		if (th_can_win_258_without_magic) {
			ChiHuRight tmpChr = new ChiHuRight();
			tmpChr.set_empty();

			tmpChr.opr_or(Constants_TaoJiang.CHR_PING_HU);

			tmpChr.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);

			if (has_tian_tian_hu) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
			} else if (has_tian_hu) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
			}

			tmp_score = get_pai_xing_fen(tmpChr, _seat_index);
			if (max_score < tmp_score) {
				max_score = tmp_score;
				caculate_type = 2;
			}
		}
		if (can_win_ying_da_hu) {
			ChiHuRight tmpChr = new ChiHuRight();
			tmpChr.set_empty();

			tmpChr.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);

			if (can_win_jiang_yi_se_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (can_win_qing_yi_se_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}

			boolean t_has_hao_hua_qi_dui = false;
			boolean t_has_peng_hu = false;

			if (check_ying_qi_xiao_dui == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI
					|| check_ying_qi_xiao_dui == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI
					|| check_ying_qi_xiao_dui == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
				t_has_hao_hua_qi_dui = true;
			}

			if (t_has_hao_hua_qi_dui) {
				tmpChr.opr_or(check_ying_qi_xiao_dui);
			} else {
				if (can_win_peng_peng_hu_without_magic) {
					t_has_peng_hu = true;
					tmpChr.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
				}
				if (t_has_peng_hu == false) {
					if (check_ying_qi_xiao_dui != 0) {
						tmpChr.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);
					}
				}
			}

			if (can_win_jiang_yi_se_without_magic) {
				if (has_tian_tian_hu) {
					tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
				} else if (has_tian_hu) {
					tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
				}
			}

			tmp_score = get_pai_xing_fen(tmpChr, _seat_index);
			if (max_score < tmp_score) {
				max_score = tmp_score;
				caculate_type = 3;
			}
		}
		if (can_win_ruan_da_hu) {
			ChiHuRight tmpChr = new ChiHuRight();
			tmpChr.set_empty();

			if (can_win_jiang_yi_se_with_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (can_win_qing_yi_se_with_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}

			boolean t_has_hao_hua_qi_dui = false;
			boolean t_has_peng_hu = false;

			if (check_qi_xiao_dui == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI || check_qi_xiao_dui == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI
					|| check_qi_xiao_dui == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
				t_has_hao_hua_qi_dui = true;
			}

			if (t_has_hao_hua_qi_dui) {
				tmpChr.opr_or(check_qi_xiao_dui);
			} else {
				if (can_win_peng_peng_hu_with_magic) {
					t_has_peng_hu = true;
					tmpChr.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
				}
				if (t_has_peng_hu == false) {
					if (check_qi_xiao_dui != 0) {
						tmpChr.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);
					}
				}
			}

			if (has_tian_tian_hu) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
			} else if (has_tian_hu) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
			}

			tmp_score = get_pai_xing_fen(tmpChr, _seat_index);
			if (max_score < tmp_score) {
				max_score = tmp_score;
				caculate_type = 4;
			}
		}

		boolean has_qi_dui = false;

		if (!need_to_check_258) {
			if (caculate_type == 1) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
				winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;

				if (th_can_win_jiang_yi_se_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
					winCardChr |= Constants_TaoJiang.CHR_JIANG_JIANG_HU;
				}
				if (th_can_win_qing_yi_se_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
					winCardChr |= Constants_TaoJiang.CHR_QING_YI_SE;
				}
				if (th_can_win_peng_peng_hu_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
					winCardChr |= Constants_TaoJiang.CHR_PENG_PENG_HU;
				}

				if (has_tian_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
				} else if (has_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
				}
			} else if (caculate_type == 2) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_PING_HU);

				chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
				winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;

				if (has_tian_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
				} else if (has_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
				}
			} else if (caculate_type == 3) {
				if (has_tian_tian_hu) {
				} else if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER) && has_tian_hu) {
				} else if (has_tian_hu) {
					cbCardIndexTemp[joker_card_index_1] = 3;
				}

				chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
				winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;

				if (can_win_jiang_yi_se_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
					winCardChr |= Constants_TaoJiang.CHR_JIANG_JIANG_HU;
				}
				if (can_win_qing_yi_se_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
					winCardChr |= Constants_TaoJiang.CHR_QING_YI_SE;
				}

				boolean t_has_hao_hua_qi_dui = false;
				boolean t_has_peng_hu = false;

				if (check_ying_qi_xiao_dui == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI
						|| check_ying_qi_xiao_dui == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI
						|| check_ying_qi_xiao_dui == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
					t_has_hao_hua_qi_dui = true;
				}

				if (t_has_hao_hua_qi_dui) {
					chiHuRight.opr_or(check_ying_qi_xiao_dui);
					winCardChr |= check_ying_qi_xiao_dui;

					has_qi_dui = true;
				} else {
					if (can_win_peng_peng_hu_without_magic) {
						t_has_peng_hu = true;
						chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
						winCardChr |= Constants_TaoJiang.CHR_PENG_PENG_HU;
					}
					if (t_has_peng_hu == false) {
						if (check_ying_qi_xiao_dui != 0) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);
							winCardChr |= Constants_TaoJiang.CHR_QI_XIAO_DUI;

							has_qi_dui = true;
						}
					}
				}

				if (can_win_jiang_yi_se_without_magic) {
					if (has_tian_tian_hu) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
					} else if (has_tian_hu) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
					}
				}
			} else if (caculate_type == 4) {
				if (has_tian_tian_hu) {
				} else if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER) && has_tian_hu) {
				} else if (has_tian_hu) {
					cbCardIndexTemp[joker_card_index_1] = 3;
				}

				if (can_win_jiang_yi_se_with_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
					winCardChr |= Constants_TaoJiang.CHR_JIANG_JIANG_HU;
				}
				if (can_win_qing_yi_se_with_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
					winCardChr |= Constants_TaoJiang.CHR_QING_YI_SE;
				}

				boolean t_has_hao_hua_qi_dui = false;
				boolean t_has_peng_hu = false;

				if (check_qi_xiao_dui == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI || check_qi_xiao_dui == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI
						|| check_qi_xiao_dui == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
					t_has_hao_hua_qi_dui = true;
				}

				if (t_has_hao_hua_qi_dui) {
					chiHuRight.opr_or(check_qi_xiao_dui);
					winCardChr |= check_qi_xiao_dui;

					has_qi_dui = true;
				} else {
					if (can_win_peng_peng_hu_with_magic) {
						t_has_peng_hu = true;
						chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
						winCardChr |= Constants_TaoJiang.CHR_PENG_PENG_HU;
					}
					if (t_has_peng_hu == false) {
						if (check_qi_xiao_dui != 0) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);
							winCardChr |= Constants_TaoJiang.CHR_QI_XIAO_DUI;

							has_qi_dui = true;
						}
					}
				}

				if (has_tian_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
				} else if (has_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
				}
			}
		} else if (need_to_check_258) {
			if (can_win_258) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_PING_HU);

				can_win = true;
				has_normal_win = true;

				if (has_tian_tian_hu) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_TIAN_HU);
				}

				if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
					if (has_tian_hu) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
					}
					if (can_win_258_without_magic) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
						winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;
					}
				} else {
					// 四王时，如果有天胡+硬庄，肯定不会走这里。要么只有硬庄，要么只有天胡。先判断天胡，再判断硬庄
					if (has_tian_hu) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_TIAN_HU);
					}
					if (can_win_258_without_magic && !has_tian_hu) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
						winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;
					}
				}
			}
		}

		if (cbCardIndexTemp[ding_wang_card_index] == 3) {
			if (!is_fei_zi_mo() || cur_card != ding_wang_card) {
				if (has_normal_win) {
					if (th_can_win_ying_da_hu || !has_qi_dui) {
						cbCardIndexTemp[ding_wang_card_index] = 0;

						int tmpWeaveCount = weave_count;
						WeaveItem[] tmpWeaveItems = Arrays.copyOf(weaveItems, GameConstants.MAX_WEAVE);
						tmpWeaveItems[tmpWeaveCount].weave_kind = GameConstants.WIK_PENG;
						tmpWeaveItems[tmpWeaveCount].center_card = ding_wang_card;
						tmpWeaveItems[tmpWeaveCount].public_card = 1;
						tmpWeaveItems[tmpWeaveCount].provide_player = 0;
						tmpWeaveCount++;

						int tmpWinCardChr = getDiHuPai(cbCardIndexTemp, tmpWeaveItems, tmpWeaveCount);

						if (tmpWinCardChr == winCardChr) {
							chiHuRight.opr_or(Constants_TaoJiang.CHR_DI_HU);
							can_win = true;
						}
					}
				} else if (card_type == Constants_TaoJiang.HU_CARD_TYPE_ZI_MO) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_DI_HU);
					can_win = true;
				}
			}
		}

		if (can_win == false)

		{
			chiHuRight.set_empty();

			if (is_qi_shou(card_type)) {
				cards_index[_logic.switch_to_card_index(_send_card_data)]++;
			}

			return GameConstants.WIK_NULL;
		}

		if (has_normal_win && _out_card_count == 0 && _seat_index == GRR._banker_player) {
			chiHuRight.opr_or(Constants_TaoJiang.CHR_DAO_DI_HU);
		}

		if (is_qi_shou(card_type)) {
			cards_index[_logic.switch_to_card_index(_send_card_data)]++;
		}

		return GameConstants.WIK_CHI_HU;
	}

	public int analyse_card_jie_pao_four_joker(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int cbChiHuKind = GameConstants.WIK_CHI_HU;

		// 统计胡牌时，手里的王牌数目
		int joker_count = 0;
		joker_count += cards_index[joker_card_index_1];
		if (joker_card_index_2 != -1) {
			joker_count += cards_index[joker_card_index_2];
		}

		if (joker_count >= 3) {
			// 手里有3王以上就不能接炮
			return GameConstants.WIK_NULL;
		}

		chiHuRight.opr_or(Constants_TaoJiang.CHR_JIE_PAO);

		// 是否需要258将
		boolean need_to_check_258 = true;
		// 是否能胡牌
		boolean can_win = false;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win_with_magic = false;
		boolean can_win_without_magic = false;
		boolean can_win_qi_dui_with_magic = false;
		boolean can_win_qi_dui_without_magic = false;
		boolean can_win_jiang_yi_se_with_magic = false;
		boolean can_win_jiang_yi_se_without_magic = false;
		boolean can_win_peng_peng_hu_with_magic = false;
		boolean can_win_peng_peng_hu_without_magic = false;
		boolean can_win_qing_yi_se_with_magic = false;
		boolean can_win_qing_yi_se_without_magic = false;
		boolean can_win_258 = false;
		boolean can_win_258_without_magic = false;
		int check_qi_xiao_dui = 0;
		int check_ying_qi_xiao_dui = 0;

		if (cur_card == joker_card_1 || cur_card == joker_card_2) {
			// 带癞子时能胡
			can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			// 癞子牌还原之后能胡或者手牌没癞子牌能胡
			can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, 0);

			check_qi_xiao_dui = qi_xiao_dui_taojiang(cards_index, weaveItems, weave_count, cur_card);
			check_ying_qi_xiao_dui = ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

			// 七对
			can_win_qi_dui_with_magic = check_qi_xiao_dui != GameConstants.WIK_NULL;
			// 硬七对
			can_win_qi_dui_without_magic = check_ying_qi_xiao_dui != GameConstants.WIK_NULL;

			// 将将胡
			can_win_jiang_yi_se_with_magic = _logic.check_taojiang_jiang_yi_se(cards_index, weaveItems, weave_count, cur_card);
			// 硬将将胡
			can_win_jiang_yi_se_without_magic = _logic.check_sg_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);

			boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, 0);

			boolean exist_eat = exist_eat(weaveItems, weave_count);

			// 碰碰胡
			can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat;
			// 硬碰碰胡
			can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat;

			// 清一色，清一色的判断，放到牌型判断的最后
			can_win_qing_yi_se_with_magic = _logic.check_taojiang_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)
					&& (can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_peng_peng_hu_with_magic);
			// 硬清一色
			can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count) && (can_win_without_magic
					|| can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_peng_peng_hu_without_magic);

			can_win_258 = AnalyseCardUtil.analyse_taojiang_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
			can_win_258_without_magic = AnalyseCardUtil.analyse_taojiang_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, 0);
		} else {
			// 带癞子时能胡
			can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
			// 癞子牌还原之后能胡或者手牌没癞子牌能胡
			can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					0);

			check_qi_xiao_dui = qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
			check_ying_qi_xiao_dui = ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

			// 七对
			can_win_qi_dui_with_magic = check_qi_xiao_dui != GameConstants.WIK_NULL;
			// 硬七对
			can_win_qi_dui_without_magic = check_ying_qi_xiao_dui != GameConstants.WIK_NULL;

			// 将将胡
			can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);
			// 硬将将胡
			can_win_jiang_yi_se_without_magic = _logic.check_sg_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);

			boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, 0);

			boolean exist_eat = exist_eat(weaveItems, weave_count);

			// 碰碰胡
			can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat;
			// 硬碰碰胡
			can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat;

			// 清一色，清一色的判断，放到牌型判断的最后
			can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
					&& (can_win_with_magic || can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_peng_peng_hu_with_magic);
			// 硬清一色
			can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count) && (can_win_without_magic
					|| can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_peng_peng_hu_without_magic);

			can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
			can_win_258_without_magic = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, 0);
		}

		// 临时存储最大牌型分
		int tmp_max_score = 0;
		int caculate_type = 0;

		if (can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_qing_yi_se_without_magic
				|| can_win_peng_peng_hu_without_magic) {
			// 如果能胡，硬庄+大胡
			need_to_check_258 = false;
			can_win = true;

			ChiHuRight tmpChr = new ChiHuRight();
			tmpChr.set_empty();

			tmpChr.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);

			if (can_win_jiang_yi_se_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (can_win_qing_yi_se_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}

			boolean tmp_has_qi_dui = false;

			// 根据是否有‘豪华七对’玩法，判断七对牌型
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI) && check_ying_qi_xiao_dui != GameConstants.WIK_NULL) {
				tmpChr.opr_or(check_ying_qi_xiao_dui);

				tmp_has_qi_dui = true;
			} else if (can_win_qi_dui_without_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);

				tmp_has_qi_dui = true;
			}

			if (!tmp_has_qi_dui) {
				// 七对、碰碰胡，不能共存，先判断七对，再判断碰碰胡
				if (can_win_peng_peng_hu_without_magic) {
					tmpChr.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
				}
			}

			// 计算硬庄+大胡的分
			tmp_max_score = get_pai_xing_fen_jie_pao(tmpChr, _seat_index);
			caculate_type = 1;
		}

		if (can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			// 如果能胡，软胡+大胡
			need_to_check_258 = false;
			can_win = true;

			ChiHuRight tmpChr = new ChiHuRight();
			tmpChr.set_empty();

			if (can_win_jiang_yi_se_with_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (can_win_qing_yi_se_with_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}

			boolean tmp_has_qi_dui = false;

			// 根据是否有‘豪华七对’玩法，判断七对牌型
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI) && check_qi_xiao_dui != GameConstants.WIK_NULL) {
				tmpChr.opr_or(check_qi_xiao_dui);

				tmp_has_qi_dui = true;
			} else if (can_win_qi_dui_with_magic) {
				tmpChr.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);

				tmp_has_qi_dui = true;
			}

			if (!tmp_has_qi_dui) {
				// 七对、碰碰胡，不能共存，先判断七对，再判断碰碰胡
				if (can_win_peng_peng_hu_with_magic) {
					tmpChr.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
				}
			}

			// 计算软胡+大胡的分
			int new_tmp_max_score = get_pai_xing_fen_jie_pao(tmpChr, _seat_index);
			if (new_tmp_max_score > tmp_max_score) {
				tmp_max_score = new_tmp_max_score;
				caculate_type = 2;
			}
		}

		if (!need_to_check_258) {
			if (caculate_type == 1) {
				// 如果硬胡+大胡的分多一些
				chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);

				if (can_win_jiang_yi_se_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
				}
				if (can_win_qing_yi_se_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
				}

				boolean tmp_has_qi_dui = false;

				// 根据是否有‘豪华七对’玩法，判断七对牌型
				if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI) && check_ying_qi_xiao_dui != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(check_ying_qi_xiao_dui);

					tmp_has_qi_dui = true;
				} else if (can_win_qi_dui_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);

					tmp_has_qi_dui = true;
				}

				if (!tmp_has_qi_dui) {
					// 七对、碰碰胡，不能共存，先判断七对，再判断碰碰胡
					if (can_win_peng_peng_hu_without_magic) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
					}
				}
			} else if (caculate_type == 2) {
				// 如果软胡+大胡的分多一些
				if (can_win_jiang_yi_se_with_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
				}
				if (can_win_qing_yi_se_with_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
				}

				boolean tmp_has_qi_dui = false;

				// 根据是否有‘豪华七对’玩法，判断七对牌型
				if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI) && check_qi_xiao_dui != GameConstants.WIK_NULL) {
					chiHuRight.opr_or(check_qi_xiao_dui);

					tmp_has_qi_dui = true;
				} else if (can_win_qi_dui_with_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);

					tmp_has_qi_dui = true;
				}

				if (!tmp_has_qi_dui) {
					// 七对、碰碰胡，不能共存，先判断七对，再判断碰碰胡
					if (can_win_peng_peng_hu_with_magic) {
						chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
					}
				}
			}
		} else {
			// 如果只能胡小胡
			if (can_win_258) {
				if (is_bao_ting[_provide_player] || is_bao_ting[_seat_index]) {
					// 放炮人报听了或者胡牌人报听了，平胡非硬庄也可以胡
					chiHuRight.opr_or(Constants_TaoJiang.CHR_PING_HU);

					can_win = true;
				}

				if (can_win_258_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_PING_HU);

					can_win = true;

					// 如果能胡小胡+硬庄
					chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
				}
			}
		}

		if (can_win == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		return cbChiHuKind;
	}

	/**
	 * 八王玩法时的接炮胡牌分析
	 */
	public int analyse_card_jie_pao_eight_joker(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int cbChiHuKind = GameConstants.WIK_CHI_HU;

		chiHuRight.opr_or(Constants_TaoJiang.CHR_JIE_PAO);

		// 是否需要258将
		boolean need_to_check_258 = true;
		// 是否能胡牌
		boolean can_win = false;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		// 癞子牌还原之后能胡或者手牌没癞子牌能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);

		int check_ying_qi_xiao_dui = ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		// 硬七对
		boolean can_win_qi_dui_without_magic = check_ying_qi_xiao_dui != GameConstants.WIK_NULL;

		// 硬将将胡
		boolean can_win_jiang_yi_se_without_magic = _logic.check_sg_ying_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);

		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, 0);

		boolean exist_eat = exist_eat(weaveItems, weave_count);

		// 硬碰碰胡
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat;

		// 硬清一色，清一色的判断，放到牌型判断的最后
		boolean can_win_qing_yi_se_without_magic = _logic.check_hubei_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count)
				&& (can_win_without_magic || can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_peng_peng_hu_without_magic);

		if (can_win_qi_dui_without_magic || can_win_jiang_yi_se_without_magic || can_win_qing_yi_se_without_magic
				|| can_win_peng_peng_hu_without_magic) {
			// 如果能胡，硬庄+大胡
			need_to_check_258 = false;
			can_win = true;

			chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);

			if (can_win_jiang_yi_se_without_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (can_win_qing_yi_se_without_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}

			boolean tmp_has_qi_dui = false;

			// 根据是否有‘豪华七对’玩法，判断七对牌型
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI) && check_ying_qi_xiao_dui != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(check_ying_qi_xiao_dui);

				tmp_has_qi_dui = true;
			} else if (can_win_qi_dui_without_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);

				tmp_has_qi_dui = true;
			}

			if (!tmp_has_qi_dui) {
				// 七对、碰碰胡，不能共存，先判断七对，再判断碰碰胡
				if (can_win_peng_peng_hu_without_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
				}
			}
		}

		if (need_to_check_258) {
			// 如果只能胡小胡
			boolean can_win_258_without_magic = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, 0);
			if (can_win_258_without_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_PING_HU);

				can_win = true;

				chiHuRight.opr_or(Constants_TaoJiang.CHR_YING_ZHUANG);
			}
		}

		if (can_win == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		return cbChiHuKind;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		@SuppressWarnings("unused")
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// 如果本圈可以抢杠
			if (can_qiang_gang[i]) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				// 抢杠胡时的胡牌检测
				action = analyse_card_from_ting(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public boolean estimate_an_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		@SuppressWarnings("unused")
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			if (_playerStatus[i]._hu_card_count != 1 || _playerStatus[i]._hu_cards[0] != -1) {
				continue;
			}

			playerStatus = _playerStatus[i];

			// 如果本圈可以抢杠
			if (can_qiang_gang[i]) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				// 抢杠胡时的胡牌检测
				action = analyse_card_from_ting(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	public int analyse_card_from_ting(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		if (card_type == Constants_TaoJiang.HU_CARD_TYPE_GANG_HU) {
			chiHuRight.opr_or(Constants_TaoJiang.CHR_GANG_HU);
		} else if (card_type == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_TaoJiang.CHR_QIANG_GANG);
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		int check_qi_xiao_dui = qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		// 七对
		boolean can_win_qi_dui_with_magic = check_qi_xiao_dui != GameConstants.WIK_NULL;

		// 将将胡
		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);

		// 清一色
		boolean can_win_qing_yi_se_with_magic = _logic.check_hubei_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count) && can_win_with_magic;

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		boolean exist_eat = exist_eat(weaveItems, weave_count);

		// 碰碰胡
		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat;

		if (can_win_qi_dui_with_magic || can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;

			if (can_win_jiang_yi_se_with_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_JIANG_JIANG_HU);
			}
			if (can_win_qing_yi_se_with_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_QING_YI_SE);
			}
			if (can_win_peng_peng_hu_with_magic) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_PENG_PENG_HU);
			}

			// 根据是否有‘豪华七对’玩法，判断七对牌型
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI) && check_qi_xiao_dui != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(check_qi_xiao_dui);
			} else {
				if (can_win_qi_dui_with_magic) {
					chiHuRight.opr_or(Constants_TaoJiang.CHR_QI_XIAO_DUI);
				}
			}
		} else {
			boolean can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
			if (can_win_258) {
				chiHuRight.opr_or(Constants_TaoJiang.CHR_PING_HU);
				cbChiHuKind = GameConstants.WIK_CHI_HU;
			}
		}

		return cbChiHuKind;
	}

	/**
	 * 接炮牌型时，单独获取一次牌型分
	 * 
	 * @param chr
	 * @param seat_index
	 * @return
	 */
	public int get_pai_xing_fen_jie_pao(ChiHuRight chr, int seat_index) {
		int pai_xing_fen = 0;

		int big_win_count = get_da_hu_count(chr);

		if (is_bao_ting[seat_index]) {
			// 如果接炮人是报听状态
			pai_xing_fen = 3 * (big_win_count + 1);
		} else {
			if (big_win_count == 0) {
				pai_xing_fen = 1;
			} else {
				pai_xing_fen = 3 * big_win_count;
			}
		}

		// 硬庄，牌型分翻倍
		if (!chr.opr_and(Constants_TaoJiang.CHR_YING_ZHUANG).is_empty()) {
			pai_xing_fen = pai_xing_fen * 2;
		}

		return pai_xing_fen;
	}

	/**
	 * 倒地胡、报听、杠上开花，不能和平胡共存
	 * 
	 * @param chr
	 * @param seat_index
	 * @return
	 */
	public int get_co_exist_count(ChiHuRight chr, int seat_index) {
		int co_count = 0;

		if (is_bao_ting[seat_index]) {
			co_count++;
		}
		if (!chr.opr_and(Constants_TaoJiang.CHR_GANG_KAI).is_empty()) {
			co_count++;
		}
		if (!chr.opr_and(Constants_TaoJiang.CHR_DAO_DI_HU).is_empty()) {
			co_count++;
		}

		return co_count;
	}

	public int get_pai_xing_fen(ChiHuRight chr, int seat_index) {
		int pai_xing_fen = 0;

		int da_hu_count = get_da_hu_count(chr);

		int special_win_count = get_special_win_count(chr, seat_index);

		int co_exist_count = get_co_exist_count(chr, seat_index);

		if (is_bao_ting[seat_index] && (da_hu_count == 0 && chr.opr_and(Constants_TaoJiang.CHR_PING_HU).is_empty())) {
			co_exist_count--;
		}

		if (da_hu_count == 0) {
			if (co_exist_count > 0) {
				// 有倒地胡、报听、杠上开花
				pai_xing_fen = 2 * co_exist_count;
			} else if (!chr.opr_and(Constants_TaoJiang.CHR_PING_HU).is_empty()) {
				// 只有平胡
				pai_xing_fen = 1;
			}

			if (special_win_count > 0) {
				// 有地胡、天胡
				pai_xing_fen += 2 * special_win_count;
			}
		} else {
			// 有大胡
			pai_xing_fen = 2 * da_hu_count;

			if (co_exist_count > 0) {
				// 有倒地胡、报听、杠上开花
				pai_xing_fen += 2 * co_exist_count;
			}

			if (special_win_count > 0) {
				// 有地胡、天胡
				pai_xing_fen += 2 * special_win_count;
			}
		}

		// 硬庄，分翻倍
		if (!chr.opr_and(Constants_TaoJiang.CHR_YING_ZHUANG).is_empty()) {
			pai_xing_fen = pai_xing_fen * 2;
		}

		return pai_xing_fen;
	}

	// 天胡、地胡、黑天胡判断
	public int get_special_win_count(ChiHuRight chr, int seat_index) {
		int special_win_count = 0;

		if (!chr.opr_and(Constants_TaoJiang.CHR_DI_HU).is_empty()) {
			special_win_count++;
		}
		if (!chr.opr_and(Constants_TaoJiang.CHR_DI_DI_HU).is_empty()) {
			special_win_count += 2;
		}

		if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
			if (joker_count_when_win[seat_index] >= 5 && !chr.opr_and(Constants_TaoJiang.CHR_TIAN_TIAN_HU).is_empty()) {
				special_win_count += joker_count_when_win[seat_index] - 4 + 1;
			} else if (joker_count_when_win[seat_index] >= 4 && !chr.opr_and(Constants_TaoJiang.CHR_TIAN_HU).is_empty()) {
				special_win_count++;
			}
		} else {
			if (joker_count_when_win[seat_index] >= 4 && !chr.opr_and(Constants_TaoJiang.CHR_TIAN_TIAN_HU).is_empty()) {
				special_win_count += joker_count_when_win[seat_index] - 3 + 1;
			} else if (joker_count_when_win[seat_index] >= 3 && !chr.opr_and(Constants_TaoJiang.CHR_TIAN_HU).is_empty()) {
				special_win_count++;
			}
		}

		if (!chr.opr_and(Constants_TaoJiang.CHR_HEI_TIAN_HU).is_empty()) {
			special_win_count++;
		}

		return special_win_count;
	}

	public int get_da_hu_count(ChiHuRight chr) {
		int da_hu_count = 0;

		if (!chr.opr_and(Constants_TaoJiang.CHR_QING_YI_SE).is_empty()) {
			da_hu_count++;
		}
		if (!chr.opr_and(Constants_TaoJiang.CHR_PENG_PENG_HU).is_empty()) {
			da_hu_count++;
		}
		if (!chr.opr_and(Constants_TaoJiang.CHR_JIANG_JIANG_HU).is_empty()) {
			da_hu_count++;
		}

		if (!chr.opr_and(Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI).is_empty()) {
			da_hu_count += 4;
		} else if (!chr.opr_and(Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI).is_empty()) {
			da_hu_count += 3;
		} else if (!chr.opr_and(Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI).is_empty()) {
			da_hu_count += 2;
		} else if (!chr.opr_and(Constants_TaoJiang.CHR_QI_XIAO_DUI).is_empty()) {
			da_hu_count += 1;
		}

		return da_hu_count;
	}

	public boolean is_card_value_258(int card_value) {
		if (card_value == 2 || card_value == 5 || card_value == 8) {
			return true;
		}
		return false;
	}

	public int getDiHuPai(int[] cards_index, WeaveItem[] weaveItems, int weave_count) {
		int card_value = _logic.get_card_value(ding_wang_card);
		int card_color = _logic.get_card_color(ding_wang_card);

		// 是否需要258将
		boolean need_to_check_258 = true;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 用一个int值保存判断地胡之前的所有牌型值
		int winCardChr = 0;

		// 带癞子时能胡
		boolean can_win_with_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
		// 癞子牌还原之后能胡或者手牌没癞子牌能胡
		boolean can_win_without_magic = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, magic_cards_index, 0);

		// 将将胡
		boolean can_win_jiang_yi_se_with_magic = _logic.check_hubei_jiang_yi_se(cards_index, weaveItems, weave_count)
				&& is_card_value_258(card_value);
		// 硬将将胡
		boolean can_win_jiang_yi_se_without_magic = _logic.check_hubei_ying_jiang_yi_se(cards_index, weaveItems, weave_count)
				&& is_card_value_258(card_value);

		boolean can_qing_yi_se = _logic.check_hubei_qing_yi_se(cards_index, weaveItems, weave_count) && can_win_with_magic;
		boolean can_ying_qing_yi_se = _logic.check_hubei_ying_qing_yi_se(cards_index, weaveItems, weave_count) && can_win_without_magic;

		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		_logic.switch_to_cards_data(cards_index, hand_cards);
		int tt_color = _logic.get_card_color(hand_cards[0]);

		// 清一色
		boolean can_win_qing_yi_se_with_magic = can_qing_yi_se && (tt_color == card_color);
		// 硬清一色
		boolean can_win_qing_yi_se_without_magic = can_ying_qing_yi_se && (tt_color == card_color);

		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
		boolean can_ying_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, -1, magic_cards_index, 0);

		boolean exist_eat = exist_eat(weaveItems, weave_count);

		// 碰碰胡
		boolean can_win_peng_peng_hu_with_magic = can_peng_hu && !exist_eat;
		// 硬碰碰胡
		boolean can_win_peng_peng_hu_without_magic = can_ying_peng_hu && !exist_eat;

		boolean can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, -1, magic_cards_index, magic_card_count);
		boolean can_win_258_without_magic = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, -1, magic_cards_index, 0);

		if (can_win_jiang_yi_se_without_magic || can_win_qing_yi_se_without_magic || can_win_peng_peng_hu_without_magic) {
			// 如果能胡，硬庄+大胡
			need_to_check_258 = false;

			winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;

			if (can_win_jiang_yi_se_without_magic) {
				winCardChr |= Constants_TaoJiang.CHR_JIANG_JIANG_HU;
			}
			if (can_win_qing_yi_se_without_magic) {
				winCardChr |= Constants_TaoJiang.CHR_QING_YI_SE;
			}
			if (can_win_peng_peng_hu_without_magic) {
				winCardChr |= Constants_TaoJiang.CHR_PENG_PENG_HU;
			}
		} else if (can_win_258_without_magic) {
			// 如果能胡平胡+硬张
			need_to_check_258 = false;

			winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;
		} else if (can_win_jiang_yi_se_with_magic || can_win_qing_yi_se_with_magic || can_win_peng_peng_hu_with_magic) {
			// 如果能胡大胡
			need_to_check_258 = false;

			if (can_win_jiang_yi_se_with_magic) {
				winCardChr |= Constants_TaoJiang.CHR_JIANG_JIANG_HU;
			}
			if (can_win_qing_yi_se_with_magic) {
				winCardChr |= Constants_TaoJiang.CHR_QING_YI_SE;
			}
			if (can_win_peng_peng_hu_with_magic) {
				winCardChr |= Constants_TaoJiang.CHR_PENG_PENG_HU;
			}
		}

		if (need_to_check_258) {
			// 如果只能胡小胡
			if (can_win_258) {
				if (can_win_258_without_magic) {
					// 如果能胡小胡+硬庄
					winCardChr |= Constants_TaoJiang.CHR_YING_ZHUANG;
				}
			}
		}

		return winCardChr;
	}

	/**
	 * 黑天胡，无王，无将（258），无刻子
	 * 
	 * @param cards_index
	 * @return
	 */
	public boolean check_hei_tian_hu(final int[] cards_index) {
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				if (i == joker_card_index_1 || i == joker_card_index_2) {
					return false;
				}

				if (cards_index[i] > 2) {
					return false;
				}

				int tmp_card_value = _logic.get_card_value(_logic.switch_to_card_data(i));

				if (is_card_value_258(tmp_card_value)) {
					return false;
				}
			}
		}

		return true;
	}

	public int qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count == 1) {
				if (i == joker_card_index_1)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			} else if (magic_card_count == 2) {
				if (i == joker_card_index_1 || i == joker_card_index_2)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI)) {
				if (nGenCount >= 3) {
					return Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 2) {
					return Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 1) {
					return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
				}

				return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
			} else {
				return Constants_TaoJiang.CHR_QI_XIAO_DUI;
			}
		} else {
			return Constants_TaoJiang.CHR_QI_XIAO_DUI;
		}
	}

	public int qi_xiao_dui_no_cur_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count == 1) {
				if (i == joker_card_index_1)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4 && i != cbCurrentIndex) {
					nGenCount++;
				}
			} else if (magic_card_count == 2) {
				if (i == joker_card_index_1 || i == joker_card_index_2)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4 && i != cbCurrentIndex) {
					nGenCount++;
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4 && i != cbCurrentIndex) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI)) {
				if (nGenCount >= 3) {
					return Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 2) {
					return Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 1) {
					return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
				}

				return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
			} else {
				return Constants_TaoJiang.CHR_QI_XIAO_DUI;
			}
		} else {
			return Constants_TaoJiang.CHR_QI_XIAO_DUI;
		}
	}

	public int qi_xiao_dui_taojiang(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count == 1) {
				if (i == joker_card_index_1)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			} else if (magic_card_count == 2) {
				if (i == joker_card_index_1 || i == joker_card_index_2)
					continue;

				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 最后的王牌，只能癞子还原
		cbReplaceCount++;

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			// 最后的王牌，只能癞子还原
			count--;

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI)) {
				if (nGenCount >= 3) {
					return Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 2) {
					return Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 1) {
					return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
				}

				return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
			} else {
				return Constants_TaoJiang.CHR_QI_XIAO_DUI;
			}
		} else {
			return Constants_TaoJiang.CHR_QI_XIAO_DUI;
		}
	}

	public int ying_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0) {
			return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI)) {
				if (nGenCount >= 3) {
					return Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 2) {
					return Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 1) {
					return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
				}

				return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
			} else {
				return Constants_TaoJiang.CHR_QI_XIAO_DUI;
			}
		} else {
			return Constants_TaoJiang.CHR_QI_XIAO_DUI;
		}
	}

	public int ying_qi_xiao_dui_no_cur_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4 && i != cbCurrentIndex) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0) {
			return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_HAO_HUA_QI_DUI)) {
				if (nGenCount >= 3) {
					return Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 2) {
					return Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI;
				} else if (nGenCount >= 1) {
					return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
				}

				return Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI;
			} else {
				return Constants_TaoJiang.CHR_QI_XIAO_DUI;
			}
		} else {
			return Constants_TaoJiang.CHR_QI_XIAO_DUI;
		}
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			int tmp_card = _playerStatus[seat_index]._hu_out_card_ting[i];
			if (tmp_card == joker_card_1 || tmp_card == joker_card_2) {
				roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
			} else if (tmp_card == ding_wang_card && has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
			} else {
				roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_NEW_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];
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
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);

			if (istrustee != null) {
				if (is_bao_ting != null && is_bao_ting[i]) {
					room_player.setIsTrustee(false);
				} else {
					room_player.setIsTrustee(istrustee[i]);
				}
			}

			// 少人模式
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			// 处理玩家是否已经报听
			if (null != is_bao_ting) {
				room_player.setBiaoyan(is_bao_ting[i] ? 1 : 0);
			} else {
				room_player.setBiaoyan(0);
			}

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				// 牌堆还有牌才能碰和杠，不然流局算庄会出错
				if (i == get_banker_next_seat(seat_index) && !istrustee[i]) {
					action = _logic.check_chi_tao_jiang(GRR._cards_index[i], card);

					if ((action & GameConstants.WIK_LEFT) != 0) {
						playerStatus.add_action(GameConstants.WIK_LEFT);
						playerStatus.add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						playerStatus.add_action(GameConstants.WIK_CENTER);
						playerStatus.add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						playerStatus.add_action(GameConstants.WIK_RIGHT);
						playerStatus.add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if (playerStatus.has_action()) {
						bAroseAction = true;
					}
				}

				// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && !istrustee[i]) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				// 杠牌判断，需要判断是否已听牌，并且杠了之后还是听牌状态
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					boolean is_ting_state_before_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

					if (is_ting_state_before_gang) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				if (!is_bao_ting[i] || !ting_state_pass_jie_pao[i]) {
					// 胡牌判断
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					// 非出牌人，接炮时的胡牌判断
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							Constants_TaoJiang.HU_CARD_TYPE_JIE_PAO, i);

					if (action != 0) {
						int tmp_score = get_pai_xing_fen_jie_pao(GRR._chi_hu_rights[i], i);
						if (tmp_score > score_when_abandoned_jie_pao[i]) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index);
							bAroseAction = true;
						}
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_card_from_ting(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_TaoJiang.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				if (cbCurrentCard == joker_card_1 || cbCurrentCard == joker_card_2)
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				if (cbCurrentCard == ding_wang_card && has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	// 是否听牌
	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_card_from_ting(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_TaoJiang.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	public int get_xuan_mei_count() {
		int m_count = 0;

		if (has_rule(Constants_TaoJiang.GAME_RULE_GANG_CARD_TWO)) {
			m_count = 2;
		} else if (has_rule(Constants_TaoJiang.GAME_RULE_GANG_CARD_THREE)) {
			m_count = 3;
		}

		if (m_count > GRR._left_card_count) {
			m_count = GRR._left_card_count;
		}

		return m_count;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 将CHR常量进行存储
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		// 客户端弹出胡牌的效果
		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 2, GameConstants.INVALID_SEAT);

		// 手牌删掉
		operate_player_cards(seat_index, 0, null, 0, null);

		// 把摸的牌从手牌删掉，结算的时候不显示这张牌的，自摸胡的时候，需要删除，接炮胡时不用
		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 对手牌进行处理
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		// 处理王牌和定王牌
		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == joker_card_1 || hand_cards[j] == joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == ding_wang_card && has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		// 将胡的牌加到手牌
		hand_cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, hand_cards, GameConstants.INVALID_SEAT);

		// 把其他玩家的牌也倒下来展示3秒
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;

			operate_player_cards(i, 0, null, 0, null);

			hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);

			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == joker_card_1 || hand_cards[j] == joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == ding_wang_card && has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, hand_cards, GameConstants.INVALID_SEAT);
		}

		return;
	}

	// 获取庄家上家的座位
	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	// 获取庄家下家的座位
	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	public boolean estimate_gang_fa_pai(int seat_index, int[] gang_cards) {
		boolean bAroseAction = false;
		int action = GameConstants.WIK_NULL;
		PlayerStatus playerStatus = null;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clear_hu_cards_of_xuan_mei();
		}

		int[] tmp_joker_count_when_win = new int[getTablePlayerNumber()];

		// 对所有翻出来的牌，针对所有玩家进行胡牌分析
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			playerStatus = _playerStatus[p];
			// 零时清空一下最大牌型分
			max_win_score[p] = 0;
			win_card_at_gang[p] = -1;

			for (int i = 0; i < gang_cards.length; i++) {
				int card = gang_cards[i];

				// 对多张牌进行吃胡分析的时候，只保留胡牌分最大的那张牌
				ChiHuRight chr = new ChiHuRight();
				chr.set_empty();

				if (p == seat_index) {
					// 翻牌人对翻出来的牌，可以进行胡牌操作
					if (playerStatus.is_chi_hu_round()) {
						// 开杠人，杠上开花的胡牌分析判断
						action = analyse_chi_hu_card(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], card, chr,
								Constants_TaoJiang.HU_CARD_TYPE_GANG_KAI, p);

						if (action != GameConstants.WIK_NULL) {
							// 牌型分最大的
							int tmp_pai_xing_fen = get_pai_xing_fen(chr, p);

							if (max_win_score[p] < tmp_pai_xing_fen) {
								// 存储牌值比较大的CHR和牌数据
								GRR._chi_hu_rights[p] = chr;
								win_card_at_gang[p] = card;
								max_win_score[p] = tmp_pai_xing_fen;

								tmp_joker_count_when_win[p] = joker_count_when_win[p];
							}
						}
					}
				} else {
					// 非翻牌人对翻出来的牌，可以进行胡牌操作
					if (playerStatus.is_chi_hu_round()) {
						// 非开杠人，对杠之后翻出来的牌，进行胡牌检测，走的和获取听牌数据一样的路径
						action = analyse_card_from_ting(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], card, chr,
								Constants_TaoJiang.HU_CARD_TYPE_GANG_HU, p);

						if (action != GameConstants.WIK_NULL) {
							// 牌型分最大的
							int tmp_pai_xing_fen = get_pai_xing_fen(chr, p);

							if (max_win_score[p] < tmp_pai_xing_fen) {
								// 存储牌值比较大的CHR和牌数据
								GRR._chi_hu_rights[p] = chr;
								win_card_at_gang[p] = card;
								max_win_score[p] = tmp_pai_xing_fen;

								tmp_joker_count_when_win[p] = joker_count_when_win[p];
							}
						}
					}
				}
			}
		}

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			// 翻出来的牌，保存最大的手牌癞子数目
			joker_count_when_win[p] = tmp_joker_count_when_win[p];

			if (p == seat_index) {
				if (win_card_at_gang[p] != -1) {
					_playerStatus[p].add_action(GameConstants.WIK_ZI_MO);
					_playerStatus[p].add_zi_mo(win_card_at_gang[p], seat_index);
					bAroseAction = true;
				} else {
					// 开杠后，杠牌人，没胡牌，相当于进入自动托管
					handler_request_trustee(p, true, 0);
					is_gang_tuo_guan[p] = true;
				}
			} else {
				if (win_card_at_gang[p] != -1) {
					_playerStatus[p].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[p].add_chi_hu(win_card_at_gang[p], seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = get_niao_card_num();

		if (GRR._count_niao > 0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			if (DEBUG_CARDS_MODE) {
				GRR._cards_data_niao[0] = 0x04;
			}

			GRR._left_card_count -= GRR._count_niao;
		}

		if (getTablePlayerNumber() == 2) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_159_ZHONG_NIAO) || has_rule(Constants_TaoJiang.GAME_RULE_DAN_ZHONG_NIAO)) {
				int winIndex = -1;
				int lostIndex = -1;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						winIndex = i;
					else
						lostIndex = i;
				}

				int tmpCount = 0;
				if (has_rule(Constants_TaoJiang.GAME_RULE_159_ZHONG_NIAO)) {
					for (int i = 0; i < GRR._count_niao; i++) {
						int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
						if (nValue == 1 || nValue == 5 || nValue == 9) {
							GRR._player_niao_cards[winIndex][GRR._player_niao_count[winIndex]] = GRR._cards_data_niao[i];
							GRR._player_niao_count[winIndex]++;
						} else {
							GRR._player_niao_cards[lostIndex][tmpCount++] = GRR._cards_data_niao[i];
						}
					}
				} else if (has_rule(Constants_TaoJiang.GAME_RULE_DAN_ZHONG_NIAO)) {
					for (int i = 0; i < GRR._count_niao; i++) {
						int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
						if (nValue % 2 == 1) {
							GRR._player_niao_cards[winIndex][GRR._player_niao_count[winIndex]] = GRR._cards_data_niao[i];
							GRR._player_niao_count[winIndex]++;
						} else {
							GRR._player_niao_cards[lostIndex][tmpCount++] = GRR._cards_data_niao[i];
						}
					}
				}

				for (int j = 0; j < GRR._player_niao_count[winIndex]; j++) {
					GRR._player_niao_cards[winIndex][j] = set_ding_niao_valid(GRR._player_niao_cards[winIndex][j], true);
				}
				for (int j = 0; j < tmpCount; j++) {
					GRR._player_niao_cards[lostIndex][j] = set_ding_niao_valid(GRR._player_niao_cards[lostIndex][j], false);
				}
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

					int seat = get_seat_by_value(nValue, seat_index);

					GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];

					GRR._player_niao_count[seat]++;
				}

				// 设置鸟牌显示
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._player_niao_count[i]; j++) {
						GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
					}
				}
			}
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

				int seat = get_seat_by_value(nValue, seat_index);

				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];

				GRR._player_niao_count[seat]++;
			}

			// 设置鸟牌显示
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
				}
			}
		}
	}

	@Override
	public int get_seat_by_value(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
			switch (nValue) {
			// 147
			case 1:
			case 4:
			case 7:
				seat = seat_index;
				break;
			// 258
			case 2:
			case 5:
			case 8:
				seat = get_banker_next_seat(seat_index);
				break;
			// 369
			case 3:
			case 6:
			case 9:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
			case 2:
				seat = seat_index;
				break;
			case 1:
			case 3:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	private int get_niao_card_num() {
		int nNum = 0;

		if (has_rule(Constants_TaoJiang.GAME_RULE_ONE_BIRD)) {
			nNum = 1;
		} else if (has_rule(Constants_TaoJiang.GAME_RULE_TWO_BIRD)) {
			nNum = 2;
		} else if (has_rule(Constants_TaoJiang.GAME_RULE_THREE_BIRD)) {
			nNum = 3;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	public int get_bei_lv() {
		int bei_lv = 1;

		if (ruleMap.containsKey(Constants_TaoJiang.GAME_RULE_BEI_LV))
			bei_lv = ruleMap.get(Constants_TaoJiang.GAME_RULE_BEI_LV);

		return bei_lv;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE * get_bei_lv();

		// 算基础分
		if (zimo) { // 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else { // 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		if (card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG || card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_GANG_HU) {
			// 如果是抢杠胡或者被杠胡，获取开杠者，能胡最大牌型分的牌型
			int tmp_lChiHuScore = lChiHuScore * get_max_win_score(seat_index_when_win);

			int tmp_niao_count = GRR._player_niao_count[seat_index] + GRR._player_niao_count[seat_index_when_win];

			int tmp_niao_power = (int) Math.pow(2, tmp_niao_count);

			GRR._game_score[seat_index] += tmp_lChiHuScore * (getTablePlayerNumber() - 1) * tmp_niao_power;
			GRR._game_score[seat_index_when_win] -= tmp_lChiHuScore * (getTablePlayerNumber() - 1) * tmp_niao_power;

			GRR._game_score[seat_index] += _player_result.pao[seat_index] + _player_result.pao[seat_index_when_win];
			GRR._game_score[seat_index_when_win] -= _player_result.pao[seat_index] + _player_result.pao[seat_index_when_win];
		} else if (zimo) {
			if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
				int pai_xing_fen = get_pai_xing_fen(GRR._chi_hu_rights[seat_index], seat_index);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int tmp_niao_count = GRR._player_niao_count[i] + GRR._player_niao_count[seat_index];

					int tmp_niao_power = (int) Math.pow(2, tmp_niao_count);

					if (is_bao_ting[i]) {
						// 八王玩法时，有人自摸，报听人都得按手牌的最大牌型分赔付
						int s = lChiHuScore * get_max_win_score(i) * tmp_niao_power * 1 * (getTablePlayerNumber() - 1);

						GRR._game_score[i] -= s;
						GRR._game_score[seat_index] += s;
					} else {
						int s = lChiHuScore * pai_xing_fen * tmp_niao_power;

						GRR._game_score[i] -= s;
						GRR._game_score[seat_index] += s;
					}

					GRR._game_score[i] -= _player_result.pao[seat_index] + _player_result.pao[i];
					GRR._game_score[seat_index] += _player_result.pao[seat_index] + _player_result.pao[i];
				}
			} else {
				int pai_xing_fen = get_pai_xing_fen(GRR._chi_hu_rights[seat_index], seat_index);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int tmp_niao_count = GRR._player_niao_count[i] + GRR._player_niao_count[seat_index];

					int tmp_niao_power = (int) Math.pow(2, tmp_niao_count);

					int s = lChiHuScore * pai_xing_fen * tmp_niao_power;

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;

					GRR._game_score[i] -= _player_result.pao[seat_index] + _player_result.pao[i];
					GRR._game_score[seat_index] += _player_result.pao[seat_index] + _player_result.pao[i];
				}
			}
		} else {
			if (is_bao_ting[provide_index]) {
				// 如果放炮者是报听状态，获取放炮者报听时，能胡最大牌型分的牌型
				int tmp_lChiHuScore = lChiHuScore * get_max_win_score(provide_index);

				int tmp_niao_count = GRR._player_niao_count[provide_index] + GRR._player_niao_count[seat_index];

				int tmp_niao_power = (int) Math.pow(2, tmp_niao_count);

				GRR._game_score[provide_index] -= tmp_lChiHuScore * (getTablePlayerNumber() - 1) * tmp_niao_power;
				GRR._game_score[seat_index] += tmp_lChiHuScore * (getTablePlayerNumber() - 1) * tmp_niao_power;

				GRR._game_score[provide_index] -= _player_result.pao[seat_index] + _player_result.pao[provide_index];
				GRR._game_score[seat_index] += _player_result.pao[seat_index] + _player_result.pao[provide_index];
			} else {
				int tmp_niao_count = GRR._player_niao_count[provide_index] + GRR._player_niao_count[seat_index];

				int tmp_niao_power = (int) Math.pow(2, tmp_niao_count);

				int s = lChiHuScore * get_pai_xing_fen_jie_pao(chr, seat_index) * tmp_niao_power;

				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;

				GRR._game_score[provide_index] -= _player_result.pao[seat_index] + _player_result.pao[provide_index];
				GRR._game_score[seat_index] += _player_result.pao[seat_index] + _player_result.pao[provide_index];
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public boolean is_ting_card(int card, int seat_index) {
		int count = _playerStatus[seat_index]._hu_card_count;
		for (int i = 0; i < count; i++) {
			int tmp_card = get_real_card(_playerStatus[seat_index]._hu_cards[i]);
			if (card == tmp_card) {
				return true;
			}
		}

		if (count == 1 && _playerStatus[seat_index]._hu_cards[0] == -1) {
			// 全听
			return true;
		}

		return false;
	}

	/**
	 * 获取玩家手牌数据的最大牌型分。抢杠胡，杠胡时，需要获取开杠人的最大牌型分和CHR值。放炮时，放炮人是报听状态。
	 * 
	 * @param seat_index
	 * @return
	 */
	public int get_max_win_score(int seat_index) {
		max_win_score[seat_index] = 0;

		int cbCurrentCard = -1;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);

			// 如果当前牌，不是听牌数据里的数据。这样能节省很多时间。
			if (!is_ting_card(cbCurrentCard, seat_index)) {
				continue;
			}

			ChiHuRight chr = new ChiHuRight();
			chr.set_empty();

			int card_type = 0;
			if (card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_GANG_HU || card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG) {
				card_type = Constants_TaoJiang.HU_CARD_TYPE_GANG_KAI;
			} else {
				card_type = Constants_TaoJiang.HU_CARD_TYPE_ZI_MO;
			}

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index], cbCurrentCard, chr, card_type, seat_index)) {

				int tmp_win_score = get_pai_xing_fen(chr, seat_index);

				if (tmp_win_score > max_win_score[seat_index]) {
					max_win_score[seat_index] = tmp_win_score;

					GRR._chi_hu_rights[seat_index] = chr;
				}
			}
		}

		return max_win_score[seat_index];
	}

	@Override
	protected void set_result_describe() {
		if (card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG || card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_GANG_HU) {
			// 抢杠胡和杠胡时，需要显示开杠人的最大牌型的CHR列表
			int chrTypes;
			long type = 0;
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				StringBuilder result = new StringBuilder("");

				if (is_bao_ting[player]) {
					result.append(" 报听");
				}

				if (player == seat_index_when_win) {
					chrTypes = GRR._chi_hu_rights[player].type_count;

					for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
						type = GRR._chi_hu_rights[player].type_list[typeIndex];

						if (type == Constants_TaoJiang.CHR_PING_HU) {
							result.append(" 平胡");
						}
						if (type == Constants_TaoJiang.CHR_QING_YI_SE) {
							result.append(" 清一色");
						}
						if (type == Constants_TaoJiang.CHR_PENG_PENG_HU) {
							result.append(" 碰碰胡");
						}
						if (type == Constants_TaoJiang.CHR_JIANG_JIANG_HU) {
							result.append(" 将将胡");
						}
						if (type == Constants_TaoJiang.CHR_QI_XIAO_DUI) {
							result.append(" 七小对");
						}
						if (type == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI) {
							result.append(" 豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI) {
							result.append(" 双豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
							result.append(" 三豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_YING_ZHUANG) {
							result.append(" 硬庄");
						}

						if (type == Constants_TaoJiang.CHR_GANG_KAI) {
							result.append(" 杠上花");
						}

						if (type == Constants_TaoJiang.CHR_DI_HU) {
							result.append(" 地胡");
						}
						if (type == Constants_TaoJiang.CHR_DI_DI_HU) {
							result.append(" 地地胡");
						}

						if (type == Constants_TaoJiang.CHR_TIAN_HU) {
							result.append(" 天胡");
						}
						if (type == Constants_TaoJiang.CHR_TIAN_TIAN_HU) {
							result.append(" 天天胡");

							if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
								if (joker_count_when_win[player] > 5) {
									result.append("+" + (joker_count_when_win[player] - 5));
								}
							} else {
								if (joker_count_when_win[player] > 4) {
									result.append("+" + (joker_count_when_win[player] - 4));
								}
							}
						}
					}

					if (card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG)
						result.append(" 被抢杠");
					else
						result.append(" 被杠胡");
				} else if (GRR._chi_hu_rights[player].is_valid()) {
					chrTypes = GRR._chi_hu_rights[player].type_count;

					for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
						type = GRR._chi_hu_rights[player].type_list[typeIndex];

						if (type == Constants_TaoJiang.CHR_PING_HU) {
							result.append(" 平胡");
						}

						if (type == Constants_TaoJiang.CHR_QING_YI_SE) {
							result.append(" 清一色");
						}
						if (type == Constants_TaoJiang.CHR_PENG_PENG_HU) {
							result.append(" 碰碰胡");
						}
						if (type == Constants_TaoJiang.CHR_JIANG_JIANG_HU) {
							result.append(" 将将胡");
						}
						if (type == Constants_TaoJiang.CHR_QI_XIAO_DUI) {
							result.append(" 七小对");
						}
						if (type == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI) {
							result.append(" 豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI) {
							result.append(" 双豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
							result.append(" 三豪华七对");
						}
					}

					if (card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_QIANG_GANG)
						result.append(" 抢杠胡");
					else
						result.append(" 杠胡");
				}

				if (GRR._player_niao_count[player] > 0) {
					result.append(" 中鸟x" + GRR._player_niao_count[player]);
				}

				GRR._result_des[player] = result.toString();
			}
		} else {
			int chrTypes;
			long type = 0;
			for (int player = 0; player < getTablePlayerNumber(); player++) {
				StringBuilder result = new StringBuilder("");

				if (is_bao_ting[player]) {
					result.append(" 报听");
				}

				chrTypes = GRR._chi_hu_rights[player].type_count;

				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];

					if (GRR._chi_hu_rights[player].is_valid()
							|| (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER) && is_bao_ting[player]
									&& card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_ZI_MO)
							|| (is_bao_ting[player] && card_type_when_win == Constants_TaoJiang.HU_CARD_TYPE_JIE_PAO)) {
						if (type == Constants_TaoJiang.CHR_ZI_MO) {
							result.append(" 自摸");
						}

						if (type == Constants_TaoJiang.CHR_PING_HU) {
							result.append(" 平胡");
						}
						if (type == Constants_TaoJiang.CHR_JIE_PAO) {
							result.append(" 接炮");
						}
						if (type == Constants_TaoJiang.CHR_QING_YI_SE) {
							result.append(" 清一色");
						}
						if (type == Constants_TaoJiang.CHR_PENG_PENG_HU) {
							result.append(" 碰碰胡");
						}
						if (type == Constants_TaoJiang.CHR_JIANG_JIANG_HU) {
							result.append(" 将将胡");
						}
						if (type == Constants_TaoJiang.CHR_QI_XIAO_DUI) {
							result.append(" 七小对");
						}
						if (type == Constants_TaoJiang.CHR_ONE_HH_QI_XIAO_DUI) {
							result.append(" 豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_TWO_HH_QI_XIAO_DUI) {
							result.append(" 双豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_THREE_HH_QI_XIAO_DUI) {
							result.append(" 三豪华七对");
						}
						if (type == Constants_TaoJiang.CHR_YING_ZHUANG) {
							result.append(" 硬庄");
						}

						if (type == Constants_TaoJiang.CHR_GANG_KAI) {
							result.append(" 杠上花");
						}
						if (type == Constants_TaoJiang.CHR_QIANG_GANG) {
							result.append(" 抢杠胡");
						}

						if (type == Constants_TaoJiang.CHR_DI_HU) {
							result.append(" 地胡");
						}
						if (type == Constants_TaoJiang.CHR_DI_DI_HU) {
							result.append(" 地地胡");
						}

						if (type == Constants_TaoJiang.CHR_HEI_TIAN_HU) {
							result.append(" 黑天胡");
						}

						if (type == Constants_TaoJiang.CHR_TIAN_HU) {
							result.append(" 天胡");
						}
						if (type == Constants_TaoJiang.CHR_TIAN_TIAN_HU) {
							result.append(" 天天胡");

							if (has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
								if (joker_count_when_win[player] > 5) {
									result.append("+" + (joker_count_when_win[player] - 5));
								}
							} else {
								if (joker_count_when_win[player] > 4) {
									result.append("+" + (joker_count_when_win[player] - 4));
								}
							}
						}

						if (type == Constants_TaoJiang.CHR_DAO_DI_HU) {
							result.append(" 倒地胡");
						}
					} else if (type == Constants_TaoJiang.CHR_FANG_PAO) {
						result.append(" 放炮");
					}
				}

				if (GRR._player_niao_count[player] > 0) {
					result.append(" 中鸟x" + GRR._player_niao_count[player]);
				}

				GRR._result_des[player] = result.toString();
			}
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x12, 0x12, 0x12, 0x05, 0x08, 0x08, 0x15, 0x18, 0x22, 0x25, 0x25, 0x28, 0x28 };
		int[] cards_of_player1 = new int[] { 0x06, 0x07, 0x08, 0x15, 0x15, 0x02, 0x03, 0x08, 0x09, 0x13, 0x14, 0x28, 0x28 };
		int[] cards_of_player2 = new int[] { 0x15, 0x15, 0x03, 0x05, 0x07, 0x08, 0x09, 0x18, 0x18, 0x18, 0x19, 0x19, 0x25 };
		int[] cards_of_player3 = new int[] { 0x09, 0x09, 0x13, 0x02, 0x02, 0x02, 0x01, 0x01, 0x01, 0x12, 0x21, 0x22, 0x23 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

		// BACK_DEBUG_CARDS_MODE = true;
		// debug_my_cards = new int[] { 17, 36, 25, 9, 40, 6, 34, 36, 17, 20, 8,
		// 23, 8, 18, 36, 2, 7, 1, 35, 35, 1, 18, 2, 24, 7, 36, 8, 17, 37, 2, 1,
		// 6,
		// 3, 19, 39, 1, 23, 20, 34, 3, 3, 25, 37, 19, 33, 24, 18, 39, 24, 5,
		// 41, 41, 7, 9, 41, 4, 3, 40, 8, 38, 37, 9, 5, 2, 25, 34, 41, 5, 20,
		// 21, 39, 37, 22, 21, 19, 38, 19, 22, 21, 20, 22, 6, 34, 39, 33, 4, 24,
		// 23, 35, 7, 21, 38, 23, 40, 4, 17, 9, 33, 18, 35, 25, 4, 6, 38,
		// 33, 5, 22, 40 };

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
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
}
