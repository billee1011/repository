/**
 * 
 */
package com.cai.game.zjh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.zjh.handler.ZJHHandler;
import com.cai.game.zjh.handler.ZJHHandlerFinish;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.zjh.ZjhRsp.GameStartZJH;
import protobuf.clazz.zjh.ZjhRsp.Give_Up_Result;
import protobuf.clazz.zjh.ZjhRsp.Jetton_Round;
import protobuf.clazz.zjh.ZjhRsp.Liang_Pai_Result;
import protobuf.clazz.zjh.ZjhRsp.Look_Card_Result;
import protobuf.clazz.zjh.ZjhRsp.Opreate_Request;
import protobuf.clazz.zjh.ZjhRsp.PukeGameEndZjh;
import protobuf.clazz.zjh.ZjhRsp.RoomInfoZjh;
import protobuf.clazz.zjh.ZjhRsp.RoomPlayerResponseZjh;
import protobuf.clazz.zjh.ZjhRsp.Score_Result;
import protobuf.clazz.zjh.ZjhRsp.Send_card;
import protobuf.clazz.zjh.ZjhRsp.TableResponseZJH;
import protobuf.clazz.zjh.ZjhRsp.User_Can_Opreate;

///////////////////////////////////////////////////////////////////////////////////////////////
public class ZJHTable extends AbstractRoom {
	/**
	 * 
	 */
	protected static final long serialVersionUID = 7060061356475703643L;

	protected static Logger logger = Logger.getLogger(ZJHTable.class);

	protected static final int ID_TIMER_START_TO_SEND_CARD = 1;// 开始到发牌
	protected static final int ID_TIMER_SEND_TO_OPREATE = 2;// 发牌到操作
	protected static final int ID_AUTO_OPERATE = 3;// 自动操作
	protected static final int ID_AUTO_READY = 4;// 自动操作
	protected static final int ID_AUTO_GEN = 5;// 自动操作
	protected static final int ID_AUTO_PIN = 6;// 自动操作
	protected static final int ID_AUTO_SYSTEAM = 7;// 系统自动操作
	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	public int _hand_card_count[]; //
	public int _hand_cards_data[][]; //

	public int _jetton_round;
	public int _cur_jetton_round;
	public int _jetton_current; // 当前
	public int _xian_qian_times;
	public int _compare_current; // 可比拍轮数
	public int _jetton_max; // 下注最高分
	public int _user_jetton_score[];
	public int _jetton_total_score;
	public boolean _b_round_opreate[];
	public boolean isGiveup[]; // 玩家是否放弃
	public boolean isLookCard[];// 玩家是否看牌
	public boolean isLose[]; // 玩家是否输了
	public int _user_gen_score[];
	public int _can_add_jetton_score[];
	public int _user_can_add_jetton_score[][];
	public int compare_ing_seat_index;// 正在选择比牌玩家
	public int _must_men_jetton_round;// 比闷圈数
	public int _user_pin_score[];

	public int _is_opreate_look[];// 是否可以操作看牌
	public int _is_opreate_give_up[];// 是否可以操作放弃
	public int _is_opreate_compare[];// 是否可以操作比牌
	public int _is_opreate_gen[];// 是否可以操作跟注
	public int _is_opreate_add[];// 是否可以操作加注
	public int _is_opreate_liangpai[];// 是否可以操作亮牌
	public int is_game_start; // 游戏是否开始
	public int _operate_time;
	public int _start_time;

	public boolean istrustee[]; // 托管状态
	public int _is_onGame[];
	public int _time_out[];
	public ScheduledFuture _trustee_schedule[];// 托管定时器

	public ZJHGameLogic _logic = null;

	public int _banker_select = GameConstants.INVALID_SEAT;

	//
	public int _current_player = GameConstants.INVALID_SEAT;
	public int _prev_palyer = GameConstants.INVALID_SEAT;
	public int _pre_opreate_type;
	public int _is_gen_dao_di[];

	public int _cur_game_timer; // 当前游戏定时器
	public int _cur_operate_time; // 可操作时间
	public int _operate_start_time; // 操作开始时间
	protected long _request_release_time;
	protected ScheduledFuture _release_scheduled;
	protected ScheduledFuture _table_scheduled;
	protected ScheduledFuture _game_scheduled;

	public ZJHHandler _handler;
	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public ZJHHandlerFinish _handler_finish; // 结束

	public ZJHTable() {
		super(RoomType.HH, 6);

		_logic = new ZJHGameLogic();
		_jetton_round = 5;
		_compare_current = 3;
		_operate_time = 60;
		_start_time = 20;
		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_hand_card_count = new int[getTablePlayerNumber()];
		_hand_cards_data = new int[getTablePlayerNumber()][this.get_hand_card_count_max()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}
		_user_jetton_score = new int[getTablePlayerNumber()];
		_b_round_opreate = new boolean[getTablePlayerNumber()];
		isGiveup = new boolean[getTablePlayerNumber()];
		isLookCard = new boolean[getTablePlayerNumber()];
		_user_gen_score = new int[getTablePlayerNumber()];
		isLose = new boolean[getTablePlayerNumber()];
		_is_opreate_look = new int[getTablePlayerNumber()];
		_is_opreate_give_up = new int[getTablePlayerNumber()];
		_is_opreate_compare = new int[getTablePlayerNumber()];
		_is_opreate_gen = new int[getTablePlayerNumber()];
		_is_opreate_add = new int[getTablePlayerNumber()];
		_is_opreate_liangpai = new int[getTablePlayerNumber()];
		_can_add_jetton_score = new int[getTablePlayerNumber()];
		_user_can_add_jetton_score = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		_is_gen_dao_di = new int[getTablePlayerNumber()];
		Arrays.fill(_is_opreate_look, 0);
		Arrays.fill(_is_opreate_give_up, 0);
		Arrays.fill(_is_opreate_compare, 0);
		Arrays.fill(_is_opreate_gen, 0);
		Arrays.fill(_is_opreate_add, 0);
		Arrays.fill(_is_opreate_liangpai, 0);
		Arrays.fill(_is_gen_dao_di, 0);

		playerNumber = 0;
		_jetton_total_score = 0;
		_xian_qian_times = 10;
	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		is_game_start = 0;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		_handler_finish = new ZJHHandlerFinish();

		this.setMinPlayerCount(2);

	}

	public boolean has_rule(int cbRule) {
		return ruleMap.containsKey(cbRule);
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////
	public boolean reset_init_data() {

		record_game_room();

		this._handler = null;

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.ZJH_MAX_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
		}

		this.log_error("reset_init_data _cur_round:" + this._cur_round);
		_cur_round++;

		istrustee = new boolean[4];
		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());

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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		_cur_jetton_round = 1;
		//

		Arrays.fill(_user_jetton_score, 0);
		Arrays.fill(_b_round_opreate, false);
		Arrays.fill(isGiveup, false);
		Arrays.fill(isLookCard, false);
		Arrays.fill(isLose, false);

		Arrays.fill(_is_opreate_look, 0);
		Arrays.fill(_is_opreate_give_up, 0);
		Arrays.fill(_is_opreate_compare, 0);
		Arrays.fill(_is_opreate_gen, 0);
		Arrays.fill(_is_opreate_add, 0);
		Arrays.fill(_is_opreate_liangpai, 0);
		_jetton_total_score = 0;
		compare_ing_seat_index = GameConstants.INVALID_SEAT;

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;

		game_cell = getRuleValue(GameConstants.GAME_RULE_ZJH_CELL_ONE);

		if (game_cell == 0) {
			if (has_rule(GameConstants.GAME_RULE_ZJH_CELL_ONE)) {
				game_cell = 1;
			} else if (has_rule(GameConstants.GAME_RULE_ZJH_CELL_TWO)) {
				game_cell = 2;
			} else if (has_rule(GameConstants.GAME_RULE_ZJH_CELL_THREE)) {
				game_cell = 3;
			} else if (has_rule(GameConstants.GAME_RULE_ZJH_CELL_FOUR)) {
				game_cell = 4;
			} else {
				game_cell = 5;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_ZJH_ROUND_20)) {
			_jetton_round = 20;
		} else if (has_rule(GameConstants.GAME_RULE_ZJH_ROUND_30)) {
			_jetton_round = 30;
		} else if (has_rule(GameConstants.GAME_RULE_ZJH_ROUND_40)) {
			_jetton_round = 40;
		}

		if (has_rule(GameConstants.GAME_RULE_ZJH_JETTON_10)) {
			_jetton_max = 10;

			_can_add_jetton_score[0] = 2;
			_can_add_jetton_score[1] = 5;
			_can_add_jetton_score[2] = 10;
		} else if (has_rule(GameConstants.GAME_RULE_ZJH_JETTON_20)) {
			_jetton_max = 20;
			_can_add_jetton_score[0] = 2;
			_can_add_jetton_score[1] = 5;
			_can_add_jetton_score[2] = 10;
			_can_add_jetton_score[3] = 20;
		} else if (has_rule(GameConstants.GAME_RULE_ZJH_JETTON_30)) {
			_jetton_max = 30;
			_can_add_jetton_score[0] = 5;
			_can_add_jetton_score[1] = 10;
			_can_add_jetton_score[2] = 20;
			_can_add_jetton_score[3] = 30;
		}
		if (has_rule(GameConstants.GAME_RULE_ZJH_COMPARE_CARD_ROUND_ONE)) {
			_compare_current = 1;
		} else {
			_compare_current = 3;
		}

		_jetton_current = (int) game_cell;
		Arrays.fill(_user_gen_score, _jetton_current);
		_pre_opreate_type = 0;
		progress_banker_select();
		game_start_zjhjd();

		return true;
	}

	private void progress_banker_select() {
		if (has_rule(GameConstants.GAME_RULE_ZJH_JETTON_NEXT)) {
			if (this._cur_round == 1) {
				int targetindex = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getPlayerCount());
				int index = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null) {
						continue;
					}
					if (index == targetindex) {
						_banker_select = i;
						_current_player = i;
						return;
					}
					index++;
				}
			} else {
				int next_player = (_banker_select + 1) % this.getTablePlayerNumber();
				while (_player_ready[next_player] == 0) {
					next_player = (next_player + 1) % this.getTablePlayerNumber();
				}

				_banker_select = next_player;
				_current_player = next_player;
			}

		} else if (has_rule(GameConstants.GAME_RULE_ZJH_JETTON_WIN_NEXT)) {
			int next_player = (_banker_select + 1) % this.getTablePlayerNumber();
			while (_player_ready[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			}
			_banker_select = next_player;
			_current_player = next_player;
		} else {
			int targetindex = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getPlayerCount());
			int index = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				if (index == targetindex) {
					_banker_select = i;
					_current_player = i;
					return;
				}
				index++;
			}
		}

	}

	// 炸金花开始
	public boolean game_start_zjhjd() {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		GameStartZJH.Builder gamestart_zjh = GameStartZJH.newBuilder();
		RoomInfoZjh.Builder room_info = getRoomInfoZjh();
		gamestart_zjh.setRoomInfo(room_info);
		this.load_player_info_data_game_start(gamestart_zjh);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_zjh));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		set_timer(ID_TIMER_START_TO_SEND_CARD, 2);

		return true;
	}

	public void send_card() {
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			if (this.get_players()[index] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_ZJH_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			Send_card.Builder send_card = Send_card.newBuilder();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this.get_players()[i] != null && this._player_ready[i] == 1) {
					if (index == i) {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.BLACK_CARD);
						}
					} else {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}

				} else {
					for (int j = 0; j < 3; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				send_card.addJettonScore(_user_jetton_score[i]);
				send_card.addCardCount(GRR._card_count[i]);

				send_card.addCardsData(cards_card);
			}
			send_card.setJettonScoreMax(_jetton_max);
			send_card.setCurrentPlayer(_banker_select);
			send_card.setDisplayTime(20);
			send_card.setJettonTotalScore(_jetton_total_score);

			roomResponse.setCommResponse(PBUtil.toByteString(send_card));

			this.send_response_to_player(index, roomResponse);
		}
		// 旁观
		if (true) {
			boolean is_card_value = false;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_ZJH_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			Send_card.Builder send_card = Send_card.newBuilder();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this.get_players()[i] != null && this._player_ready[i] == 1) {
					if (!is_card_value) {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.BLACK_CARD);
						}
						is_card_value = true;
					} else {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}

				} else {
					for (int j = 0; j < 3; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				send_card.addJettonScore(_user_jetton_score[i]);
				send_card.addCardCount(GRR._card_count[i]);

				send_card.addCardsData(cards_card);
			}
			send_card.setJettonScoreMax(_jetton_max);
			send_card.setCurrentPlayer(_banker_select);
			send_card.setDisplayTime(20);
			send_card.setJettonTotalScore(_jetton_total_score);

			roomResponse.setCommResponse(PBUtil.toByteString(send_card));

			this.observers().sendAll(roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		Send_card.Builder send_card = Send_card.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (this.get_players()[i] != null && this._player_ready[i] == 1) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
			} else {
				for (int j = 0; j < 3; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
			}
			send_card.addJettonScore(_user_jetton_score[i]);
			send_card.addCardCount(GRR._card_count[i]);

			send_card.addCardsData(cards_card);
		}
		send_card.setJettonScoreMax(_jetton_max);
		send_card.setCurrentPlayer(_banker_select);
		send_card.setDisplayTime(20);
		send_card.setJettonTotalScore(_jetton_total_score);

		roomResponse.setCommResponse(PBUtil.toByteString(send_card));
		GRR.add_room_response(roomResponse);

		set_timer(ID_TIMER_SEND_TO_OPREATE, 2);
	}

	public int get_hand_card_count_max() {
		return GameConstants.ZJH_MAX_COUNT;
	}

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		if (time == 0) {
			return true;
		}
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000, TimeUnit.MILLISECONDS);
		_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
		this._cur_operate_time = time;

		return true;
	}

	public void kill_timer() {
		if (_game_scheduled != null) {
			_game_scheduled.cancel(false);
			_game_scheduled = null;
		}
	}

	public boolean animation_timer(int timer_id) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_TIMER_START_TO_SEND_CARD: {
			// 刷新手牌
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 1) {
					_user_jetton_score[i] += game_cell;
					_jetton_total_score += game_cell;
				}
			}
			_repertory_card = new int[GameConstants.CARD_COUNT_ZJH];
			shuffle(_repertory_card, GameConstants.CARD_DATA_ZJH);

			if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
				test_cards();
			this.send_card();
			send_jetton_round();
			return false;
		}
		case ID_TIMER_SEND_TO_OPREATE: {
			Arrays.fill(_is_opreate_look, 1);
			Arrays.fill(_is_opreate_give_up, 1);
			Arrays.fill(_is_opreate_compare, 0);
			if (_cur_jetton_round >= _compare_current) {
				_is_opreate_compare[_banker_select] = _user_gen_score[_banker_select] * 2;

			}

			Arrays.fill(_is_opreate_liangpai, -1);
			_is_opreate_gen[_banker_select] = _user_gen_score[_banker_select];
			_is_opreate_add[_banker_select] = 1;

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null || this._player_ready[i] == 0) {
					continue;
				}
				Arrays.fill(_user_can_add_jetton_score[i], 0);
				int index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[i]) {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
						}
					}
				}
				send_opreate(i, _user_can_add_jetton_score[i], true);
			}
			return false;
		}
		case ID_AUTO_OPERATE: {
			if (this.has_rule(GameConstants.GAME_RULE_ZJH_TRUSTEE_JETTON)) {
				this.deal_gen_jetton(this._current_player);
			} else {
				this.deal_give_up(this._current_player);
			}
			return true;
		}
		}
		return false;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int card_cards[]) {

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, card_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			if (this.get_players()[i] == null) {
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = GameConstants.INVALID_CARD;
					_hand_cards_data[i][j] = GameConstants.INVALID_CARD;
				}
			} else {
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = repertory_card[i * GameConstants.ZJH_MAX_COUNT + j];
					_hand_cards_data[i][j] = repertory_card[i * GameConstants.ZJH_MAX_COUNT + j];
				}
			}

			GRR._card_count[i] = get_hand_card_count_max();
			_hand_card_count[i] = get_hand_card_count_max();
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}
		GRR._left_card_count = GameConstants.DDZ_DI_PAI_COUNT_JD;
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {

		int cards[] = new int[] { 0x01, 0x11, 0x21, 0x11, 0x22, 0x13, 0x22, 0x32, 0x23, 0x24, 0x25, 0x37, 0x05, 0x05, 0x05 };
		// int cards[] = new int[] {60, 8, 1, 57, 36, 25, 44, 19, 41, 54, 5, 10,
		// 56, 13, 7, 6, 43, 35, 11, 17, 61, 27, 23, 59, 3, 22, 2, 53, 33, 4,
		// 42, 26, 52, 9, 21, 24, 45, 28, 39, 38, 12, 40, 37, 29, 20, 58, 51,
		// 55};
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[index];
				_hand_cards_data[i][j] = cards[index++];
			}
		}
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 3) {
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

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
				_hand_cards_data[i][j] = 0;
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
				if (this.get_players()[i] == null) {
					continue;
				}
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = realyCards[k];
					_hand_cards_data[i][j] = realyCards[k++];
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
				GRR._cards_data[i][j] = cards[k];
				_hand_cards_data[i][j] = cards[k++];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	public void load_player_info_data_game_start(GameStartZJH.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseZjh.Builder room_player = RoomPlayerResponseZjh.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndZjh.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseZjh.Builder room_player = RoomPlayerResponseZjh.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponseZJH.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseZjh.Builder room_player = RoomPlayerResponseZjh.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		ret = this.handler_game_finish_zjhjd(seat_index, reason);

		return ret;
	}

	public boolean handler_game_finish_zjhjd(int seat_index, int reason) {

		// 错误断言
		return false;
	}

	/**
	 * @return
	 */
	public RoomInfoZjh.Builder getRoomInfoZjh() {
		RoomInfoZjh.Builder room_info = RoomInfoZjh.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
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
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			// PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			observers().send(player, quit_roomResponse);
			observers().exit(player.getAccount_id());
			return true;
		}
		return false;
	}

	@Override
	public boolean handler_observer_be_in_room(Player player) {
		if (player.getAccount_id() == this.getRoom_owner_account_id()) {
			control_game_start();
		}
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && player != null) {
			// this.send_play_data(seat_index);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_ZJH_RECONNECT_DATA);

			TableResponseZJH.Builder tableResponse_zjh = TableResponseZJH.newBuilder();
			load_player_info_data_reconnect(tableResponse_zjh);
			RoomInfoZjh.Builder room_info = getRoomInfoZjh();
			tableResponse_zjh.setRoomInfo(room_info);

			tableResponse_zjh.setBankerPlayer(GRR._banker_player);
			tableResponse_zjh.setCurrentPlayer(_current_player);
			tableResponse_zjh.setPrevPlayer(_prev_palyer);
			boolean is_card_value = true;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				tableResponse_zjh.addIsGiveUp(this.isGiveup[i]);
				tableResponse_zjh.addIsLookCards(this.isLookCard[i]);
				tableResponse_zjh.addIsIsLose(this.isLose[i]);
				tableResponse_zjh.addJettonScore(_user_jetton_score[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this.get_players()[i] != null && this._player_ready[i] == 1 && is_card_value) {
					is_card_value = false;
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GameConstants.BLACK_CARD);
					}
				} else {
					for (int j = 0; j < 3; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				tableResponse_zjh.addCardCount(GRR._card_count[i]);
				tableResponse_zjh.addCardsData(cards_card);
			}
			tableResponse_zjh.setDisplayTime(20);
			tableResponse_zjh.setJettonTotalScore(_jetton_total_score);

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_zjh));
			observers().send(player, roomResponse);

			send_jetton_round();
		} else {
			int _cur_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					if (_player_ready[i] != 0) {
						_cur_count += 1;
					}
				}
			}

			if ((this.getPlayerCount() >= 2) && (getPlayerCount() == _cur_count)) {
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				} else
					handler_game_start();
			} else {
				this.is_game_start = 0;
				control_game_start();
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
				observers().sendAll(roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {

		int limitCount = 20;
		if (SystemConfig.gameDebug == 1) {
			limitCount = 5;
		}
		// 限制围观者数量，未来加到配置表控制
		if (player.getAccount_id() != getRoom_owner_account_id() && observers().count() >= limitCount) {
			this.send_error_notify(player, 1, "当前游戏围观位置已满,下次赶早!");
			return false;
		}
		observers().enter(player);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		observers().send(player, roomResponse);
		int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index);
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			if (this.is_game_start == 1) {
				control_game_start();
				return true;
			}
			int _cur_count = 0;
			boolean flag = false;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					_cur_count += 1;
				}

			}
			if ((this.getPlayerCount() >= 2) && (getPlayerCount() == _cur_count)) {
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				}
			} else {
				control_game_start();
			}

		}

		// if ((GameConstants.GS_MJ_FREE != _game_status &&
		// GameConstants.GS_MJ_WAIT != _game_status)) {
		//
		// RoomResponse.Builder roomResponse_reconect =
		// RoomResponse.newBuilder();
		// roomResponse_reconect.setType(MsgConstants.RESPONSE_ZJH_RECONNECT_DATA);
		//
		// TableResponseZJH.Builder tableResponse_zjh =
		// TableResponseZJH.newBuilder();
		// load_player_info_data_reconnect(tableResponse_zjh);
		// RoomInfoZjh.Builder room_info = getRoomInfoZjh();
		// tableResponse_zjh.setRoomInfo(room_info);
		//
		// tableResponse_zjh.setBankerPlayer(GRR._banker_player);
		// tableResponse_zjh.setCurrentPlayer(_current_player);
		// tableResponse_zjh.setPrevPlayer(_prev_palyer);
		// boolean is_card_value=true;
		// for(int i=0;i<getTablePlayerNumber();i++){
		// tableResponse_zjh.addIsGiveUp(this.isGiveup[i]);
		// tableResponse_zjh.addIsLookCards(this.isLookCard[i]);
		// tableResponse_zjh.addIsIsLose(this.isLose[i]);
		// tableResponse_zjh.addJettonScore(_user_jetton_score[i]);
		//
		// Int32ArrayResponse.Builder
		// cards_card=Int32ArrayResponse.newBuilder();
		// if(this.get_players()[i] != null && this._player_ready[i] == 1 &&
		// is_card_value){
		// is_card_value=false;
		// for(int j=0;j<GRR._card_count[i];j++){
		// cards_card.addItem(GameConstants.BLACK_CARD);
		// }
		// }else{
		// for(int j=0;j<3;j++){
		// cards_card.addItem(GameConstants.INVALID_CARD);
		// }
		// }
		// tableResponse_zjh.addCardCount(GRR._card_count[i]);
		// tableResponse_zjh.addCardsData(cards_card);
		// }
		// tableResponse_zjh.setJettonTotalScore(_jetton_total_score);
		//
		//
		//
		// roomResponse_reconect.setCommResponse(PBUtil.toByteString(tableResponse_zjh));
		// observers().send(player, roomResponse_reconect);
		//
		// return true;
		// }
		return true;
	}

	public boolean control_game_start() {
		// if(has_rule(GameConstants.GAME_RULE_CONTORL_START) == false)
		// return false;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ROOM_OWNER_START);
		roomResponse.setEffectType(this.is_game_start);
		roomResponse.setPaoDes(getRoom_owner_name());
		if (this.is_game_start != 2)
			this.send_response_to_room(roomResponse);
		Player player = observers().getPlayer(getRoom_owner_account_id());
		if (player == null) {
			player = this.get_player(getRoom_owner_account_id());
			if (player != null)
				this.send_response_to_player(player.get_seat_index(), roomResponse);
		} else {
			observers().send(player, roomResponse);
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
		if (_is_onGame != null) {
			this._is_onGame[seat_index] = 1;
		}

		_player_ready[seat_index] = 1;
		if (is_cancel) {
			_player_ready[seat_index] = 0;
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

		observers().sendAll(roomResponse);

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}
		int cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null && this._is_onGame[i] != 0) {
				cur_count++;
			} else {
				continue;
			}

		}
		int _cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				if (_player_ready[i] != 0) {
					_cur_count += 1;
				}
			}
		}

		if ((cur_count >= 2) && (_cur_count == cur_count)) {
			if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
				this.is_game_start = 1;
				control_game_start();
			} else
				handler_game_start();
		} else {
			this.is_game_start = 0;
			control_game_start();
			if (_cur_game_timer != ID_AUTO_READY && GameConstants.GS_MJ_WAIT == _game_status) {
				this.set_timer(ID_AUTO_READY, _start_time);
			}
		}

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {

		if (getRuleValue(GameConstants.GAME_RULE_IP) > 0) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;

				// logger.error("tarplayer
				// ip=="+tarplayer.getAccount_ip()+"player
				// ip=="+player.getAccount_ip());
				if (player.getAccount_id() == tarplayer.getAccount_id())
					continue;
				if ((!is_sys()) && StringUtils.isNotEmpty(tarplayer.getAccount_ip()) && StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					// player.setRoom_id(0);// 把房间信息清除--
					send_error_notify(player, 1, "不允许相同ip进入");
					return false;
				}
			}
		}

		int seat_index = GameConstants.INVALID_SEAT;
		/**
		 * 1) 勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，但只有房主能坐下（有空位时），此时普通用户进入时界面上只有“观战”按键。
		 * 2)不勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，且均能坐下（有空位时）。
		 * 3)游戏开始前进入桌子观战的用户，不管是否勾选此选项，只要有空位可随时坐下
		 */

		if (playerNumber == 0) {// 未开始 才分配位置
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[i] == null) {
					get_players()[i] = player;
					seat_index = i;
					break;
				}
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}
		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			send_error_notify(player, 1, "游戏已经开始");
			return false;
		}

		// 从观察者列表移出
		if (!observers().sit(player.getAccount_id())) {
			logger.error(String.format("玩家[%s]必须先成为观察者才能坐下!", player));
			// return false;
		}

		if (!onPlayerEnterUpdateRedis(player.getAccount_id())) {
			send_error_notify(player, 1, "已在其他房间中");
			return false;
		}

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);

		// 同步数据

		// ========同步到中心========
		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		roomRedisModel.getNames().add(player.getNick_name());
		// 写入redis
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index);
		if (has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
			if (this.is_game_start == 1)
				this.is_game_start = 0;
			control_game_start();
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if (compare_ing_seat_index == seat_index) {
			compare_ing_seat_index = GameConstants.INVALID_SEAT;
		}
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_ZJH_RECONNECT_DATA);

			TableResponseZJH.Builder tableResponse_zjh = TableResponseZJH.newBuilder();
			load_player_info_data_reconnect(tableResponse_zjh);
			RoomInfoZjh.Builder room_info = getRoomInfoZjh();
			tableResponse_zjh.setRoomInfo(room_info);

			tableResponse_zjh.setBankerPlayer(GRR._banker_player);
			tableResponse_zjh.setCurrentPlayer(_current_player);
			tableResponse_zjh.setPrevPlayer(_prev_palyer);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				tableResponse_zjh.addIsGiveUp(this.isGiveup[i]);
				tableResponse_zjh.addIsLookCards(this.isLookCard[i]);

				tableResponse_zjh.addIsIsLose(this.isLose[i]);
				tableResponse_zjh.addJettonScore(_user_jetton_score[i]);

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this.get_players()[i] != null && this._player_ready[i] == 1) {
					if (seat_index == i) {
						if (!this.isLookCard[seat_index]) {
							for (int j = 0; j < GRR._card_count[i]; j++) {
								cards_card.addItem(GameConstants.BLACK_CARD);
							}
						} else {
							for (int j = 0; j < GRR._card_count[i]; j++) {
								cards_card.addItem(GRR._cards_data[seat_index][j]);
							}
						}

					} else {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}

				} else {
					for (int j = 0; j < 3; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				tableResponse_zjh.addCardCount(GRR._card_count[i]);
				tableResponse_zjh.addCardsData(cards_card);
			}
			tableResponse_zjh.setDisplayTime(20);
			tableResponse_zjh.setJettonTotalScore(_jetton_total_score);

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_zjh));
			send_response_to_player(seat_index, roomResponse);

			Arrays.fill(_user_can_add_jetton_score[seat_index], 0);
			if (_is_opreate_add[seat_index] == 1) {
				int index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[seat_index]) {
							_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x];
						}

					}
				}
			}
			if (this._player_ready[seat_index] == 1 && _cur_game_timer != ID_TIMER_START_TO_SEND_CARD
					&& _cur_game_timer != ID_TIMER_SEND_TO_OPREATE) {
				send_opreate(seat_index, _user_can_add_jetton_score[seat_index], false);
			}
			send_jetton_round();
		} else {
			int _cur_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					if (_player_ready[i] != 0) {
						_cur_count += 1;
					}
				}
			}

			if ((this.getPlayerCount() >= 2) && (getPlayerCount() == _cur_count)) {
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				} else
					handler_game_start();
			} else {
				this.is_game_start = 0;
				control_game_start();
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
	 * //用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {

		// if (this._handler != null) {
		// this._handler.handler_player_out_card(this, seat_index, card);
		// }

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
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

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_OPREATE) {

			Opreate_Request req = PBUtil.toObject(room_rq, Opreate_Request.class);
			// 逻辑处理
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getComparePlayer(), req.getAddJettonScore());
		}
		return true;
	}

	public boolean handler_ox_game_start(int room_id, long account_id) {
		if (is_game_start == 2)
			return true;
		is_game_start = 2;
		control_game_start();

		handler_game_start();
		boolean nt = true;
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int compareyplayer, int jetton_score) {

		return true;
	}

	public boolean deal_liang_pai(int seat_index) {
		if (_game_status != GameConstants.GS_MJ_WAIT) {
			return true;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_LIANG_PAI_RESULT);
		// 发送数据
		Liang_Pai_Result.Builder liang_pai_result = Liang_Pai_Result.newBuilder();
		liang_pai_result.setOpreatePlayer(seat_index);
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int i = 0; i < _hand_card_count[seat_index]; i++) {
			liang_pai_result.addCardsData(_hand_cards_data[seat_index][i]);
		}
		liang_pai_result.setCardCount(_hand_card_count[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
		this.send_response_to_room(roomResponse);
		observers().sendAll(roomResponse);
		return true;
	}

	public boolean deal_cancel_compare(int seat_index) {
		if (isGiveup[seat_index] || isLose[seat_index] || seat_index != this._current_player || _player_ready[seat_index] == 0) {
			return true;
		}
		if (seat_index != this._current_player) {
			return true;
		}
		// 取消比牌
		compare_ing_seat_index = GameConstants.INVALID_SEAT;
		return true;
	}

	public boolean deal_add_jetton(int seat_index, int jetton_index) {
		int jetton_score = _user_can_add_jetton_score[seat_index][jetton_index - 1];
		if (isLookCard[seat_index]) {
			// 加注
			if (jetton_score / 2 < _jetton_current || jetton_score / 2 > _jetton_max) {
				this.send_error_notify(seat_index, 2, "目前已加到最高注数，无法继续加注");
				return true;
			}
		} else {
			// 加注
			if (jetton_score < _jetton_current || jetton_score > _jetton_max) {
				this.send_error_notify(seat_index, 2, "目前已加到最高注数，无法继续加注");
				return true;
			}
		}

		if (isGiveup[seat_index] || isLose[seat_index] || _current_player == GameConstants.INVALID_SEAT) {
			return true;
		}
		if (seat_index != this._current_player) {
			return true;
		}
		if (isLookCard[seat_index]) {
			if (jetton_score % 2 != 0) {
				return true;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
					continue;
				}
				if (isLookCard[i]) {
					_user_gen_score[i] = jetton_score;
				} else {
					_user_gen_score[i] = jetton_score / 2;
				}

			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
					continue;
				}
				if (isLookCard[i]) {
					_user_gen_score[i] = jetton_score * 2;
				} else {
					_user_gen_score[i] = jetton_score;
				}
			}
		}
		if (isLookCard[seat_index]) {
			_jetton_current = jetton_score / 2;
		} else {
			_jetton_current = jetton_score;
		}

		_user_jetton_score[seat_index] += jetton_score;
		_jetton_total_score += jetton_score;
		_b_round_opreate[seat_index] = true;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_JETTON_RESULT);
		// 发送数据
		Score_Result.Builder score_result = Score_Result.newBuilder();

		score_result.setAddJettonScore(jetton_score);
		score_result.setOpreatePlayer(seat_index);

		// 是否已经下注一轮
		boolean b_round = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			score_result.addJettonScore(_user_jetton_score[i]);
			score_result.addUserGenScore(_user_gen_score[i]);
			if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
				continue;
			}
			if (!this._b_round_opreate[i]) {
				b_round = false;
			}
		}
		if (b_round) {
			Arrays.fill(_b_round_opreate, false);
			_cur_jetton_round++;
			send_jetton_round();
		}
		if (_cur_jetton_round >= _compare_current) {
			score_result.setCompareOpreate(1);
		} else {
			score_result.setCompareOpreate(0);
		}
		score_result.setJettonTotalScore(_jetton_total_score);
		score_result.setDisplayTime(20);
		if (isLookCard[seat_index]) {
			// 加注
			if (jetton_score / 2 == _jetton_max) {
				score_result.setJettonType(5);
			} else {
				score_result.setJettonType(3);
			}
		} else {
			// 加注
			if (jetton_score == _jetton_max) {
				score_result.setJettonType(5);
			} else {
				score_result.setJettonType(3);
			}
		}

		if (_cur_jetton_round > _jetton_round) {
			_current_player = GameConstants.INVALID_SEAT;
		} else {
			int nextplayer = (seat_index + 1) % this.getTablePlayerNumber();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[nextplayer] == null || isGiveup[nextplayer] || isLose[nextplayer] || _player_ready[nextplayer] == 0) {
					nextplayer = (nextplayer + 1) % this.getTablePlayerNumber();
				} else {
					break;
				}
			}
			_current_player = nextplayer;
		}
		score_result.setCurrentPlayer(_current_player);

		roomResponse.setCommResponse(PBUtil.toByteString(score_result));
		GRR.add_room_response(roomResponse);
		observers().sendAll(roomResponse);
		this.send_response_to_room(roomResponse);

		// 轮数达到强制结束
		if (_cur_jetton_round > _jetton_round) {
			// 轮数达到强制结束
			_current_player = GameConstants.INVALID_SEAT;

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), 2, TimeUnit.SECONDS);
			Arrays.fill(_is_opreate_gen, -1);
			Arrays.fill(_is_opreate_add, -1);
			Arrays.fill(_is_opreate_compare, -1);
			Arrays.fill(_is_opreate_give_up, -1);
			Arrays.fill(_is_opreate_look, -1);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Arrays.fill(_user_can_add_jetton_score[i], 0);
				int index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[i]) {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
						}

					}
				}
				send_opreate(i, _user_can_add_jetton_score[i], false);
			}
			return true;
		} else {
			_is_opreate_gen[seat_index] = 0;
			_is_opreate_add[seat_index] = 0;
			_is_opreate_compare[seat_index] = 0;

			if (_cur_jetton_round >= _compare_current) {
				_is_opreate_compare[_current_player] = _user_gen_score[_current_player] * 2;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == _current_player || i == seat_index || _player_ready[i] == 0) {
						continue;
					}
					Arrays.fill(_user_can_add_jetton_score[i], 0);
					int index = 0;
					for (int x = 0; x < 6; x++) {
						if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
							if (this.isLookCard[i]) {
								_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
							} else {
								_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
							}

						}
					}
					send_opreate(i, _user_can_add_jetton_score[i], false);
				}
			} else {
				_is_opreate_compare[_current_player] = 0;
			}
			_is_opreate_gen[_current_player] = _user_gen_score[_current_player];
			if (_jetton_current == _jetton_max) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.isLose[i] || this.isGiveup[i]) {
						_is_opreate_add[i] = -1;
					} else {
						_is_opreate_add[i] = 0;
					}
				}
			} else {
				_is_opreate_add[seat_index] = 0;
				_is_opreate_add[_current_player] = 1;
			}

			// 操作玩家
			Arrays.fill(_user_can_add_jetton_score[seat_index], 0);
			int index = 0;
			for (int x = 0; x < 6; x++) {
				if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
					if (this.isLookCard[seat_index]) {
						_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x] * 2;
					} else {
						_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x];
					}

				}
			}
			send_opreate(seat_index, _user_can_add_jetton_score[seat_index], false);

			// 当前玩家
			Arrays.fill(_user_can_add_jetton_score[_current_player], 0);
			index = 0;
			for (int x = 0; x < 6; x++) {
				if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
					if (this.isLookCard[_current_player]) {
						_user_can_add_jetton_score[_current_player][index++] = _can_add_jetton_score[x] * 2;
					} else {
						_user_can_add_jetton_score[_current_player][index++] = _can_add_jetton_score[x];
					}

				}
			}
			send_opreate(_current_player, _user_can_add_jetton_score[_current_player], false);
		}

		return true;
	}

	public boolean deal_give_up(int seat_index) {
		// 放弃
		if (isGiveup[seat_index] || _current_player == GameConstants.INVALID_SEAT) {
			return false;
		}
		if (compare_ing_seat_index != GameConstants.INVALID_SEAT) {
			this.send_error_notify(seat_index, 2, "玩家比牌选择中，暂时无法弃牌");
			return false;
		}
		// 判断还剩几人没放弃没输
		int playernum = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
				continue;
			}
			playernum++;
		}
		if (playernum <= 1) {
			return false;
		}

		isGiveup[seat_index] = true;
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_GIVE_UP_RESULT);
		// 发送数据
		Give_Up_Result.Builder give_result = Give_Up_Result.newBuilder();
		give_result.setOpreatePlayer(seat_index);
		boolean b_round = true;
		if (seat_index == this._current_player) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
					continue;
				}
				if (!this._b_round_opreate[i]) {
					b_round = false;
				}
			}
			if (b_round) {
				Arrays.fill(_b_round_opreate, false);
				_cur_jetton_round++;
				send_jetton_round();
			}
		} else {
			b_round = false;
		}

		if (_cur_jetton_round >= _compare_current) {
			give_result.setCompareOpreate(1);
		} else {
			give_result.setCompareOpreate(0);
		}
		int nextplayer = GameConstants.INVALID_SEAT;
		if (_cur_jetton_round > _jetton_round) {
			_current_player = GameConstants.INVALID_SEAT;
		} else {
			if (seat_index == this._current_player) {
				nextplayer = (seat_index + 1) % this.getTablePlayerNumber();
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (get_players()[nextplayer] == null || isGiveup[nextplayer] || isLose[nextplayer] || _player_ready[nextplayer] == 0) {
						nextplayer = (nextplayer + 1) % this.getTablePlayerNumber();
					} else {
						break;
					}
				}
				_current_player = nextplayer;
			}
		}
		// 判断还剩几人没放弃没输
		playernum = 0;
		int winorder = GameConstants.INVALID_SEAT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
				continue;
			}
			winorder = i;
			playernum++;
		}

		if (playernum == 1) {
			give_result.setCurrentPlayer(GameConstants.INVALID_SEAT);
		} else {
			give_result.setDisplayTime(20);
			give_result.setCurrentPlayer(_current_player);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(give_result));
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		observers().sendAll(roomResponse);

		_is_opreate_look[seat_index] = -1;
		_is_opreate_compare[seat_index] = -1;
		_is_opreate_give_up[seat_index] = -1;
		_is_opreate_gen[seat_index] = -1;
		_is_opreate_add[seat_index] = -1;

		Arrays.fill(_user_can_add_jetton_score[seat_index], 0);
		int index = 0;
		for (int x = 0; x < 6; x++) {
			if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
				if (this.isLookCard[seat_index]) {
					_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x] * 2;
				} else {
					_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x];
				}

			}
		}
		send_opreate(seat_index, _user_can_add_jetton_score[seat_index], false);

		// 剩一个玩家游戏结束
		if (playernum == 1) {
			_current_player = GameConstants.INVALID_SEAT;
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), winorder, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);

			Arrays.fill(_is_opreate_gen, -1);
			Arrays.fill(_is_opreate_add, -1);
			Arrays.fill(_is_opreate_compare, -1);
			Arrays.fill(_is_opreate_give_up, -1);
			Arrays.fill(_is_opreate_look, -1);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Arrays.fill(_user_can_add_jetton_score[i], 0);
				index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[i]) {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
						}
					}
				}
				send_opreate(i, _user_can_add_jetton_score[i], false);
			}
		} else if (_cur_jetton_round > _jetton_round) {
			// 轮数达到强制结束
			_current_player = GameConstants.INVALID_SEAT;

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), 2, TimeUnit.SECONDS);
			Arrays.fill(_is_opreate_gen, -1);
			Arrays.fill(_is_opreate_add, -1);
			Arrays.fill(_is_opreate_compare, -1);
			Arrays.fill(_is_opreate_give_up, -1);
			Arrays.fill(_is_opreate_look, -1);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Arrays.fill(_user_can_add_jetton_score[i], 0);
				index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[i]) {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
						}
					}
				}
				send_opreate(i, _user_can_add_jetton_score[i], false);
			}
		} else {
			if (_cur_jetton_round >= _compare_current) {
				_is_opreate_compare[_current_player] = _user_gen_score[_current_player] * 2;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (i == _current_player || i == seat_index || _player_ready[i] == 0) {
						continue;
					}
					Arrays.fill(_user_can_add_jetton_score[i], 0);
					index = 0;
					for (int x = 0; x < 6; x++) {
						if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
							if (this.isLookCard[i]) {
								_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
							} else {
								_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
							}
						}
					}
					send_opreate(i, _user_can_add_jetton_score[i], false);
				}
			} else {
				_is_opreate_compare[_current_player] = 0;
			}
			_is_opreate_gen[_current_player] = _user_gen_score[_current_player];
			if (_jetton_current == _jetton_max) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.isLose[i] || this.isGiveup[i]) {
						_is_opreate_add[i] = -1;
					} else {
						_is_opreate_add[i] = 0;
					}
				}
			} else {
				_is_opreate_add[_current_player] = 1;
			}

			Arrays.fill(_user_can_add_jetton_score[_current_player], 0);
			index = 0;
			for (int x = 0; x < 6; x++) {
				if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
					if (this.isLookCard[_current_player]) {
						_user_can_add_jetton_score[_current_player][index++] = _can_add_jetton_score[x] * 2;
					} else {
						_user_can_add_jetton_score[_current_player][index++] = _can_add_jetton_score[x];
					}
				}
			}
			send_opreate(_current_player, _user_can_add_jetton_score[_current_player], false);
		}

		return true;
	}

	public boolean deal_look_card(int seat_index) {
		// 看牌
		if (isGiveup[seat_index] || isLookCard[seat_index] || isLose[seat_index] || _game_status != GameConstants.GS_MJ_PLAY) {
			return true;
		}
		isLookCard[seat_index] = true;
		_user_gen_score[seat_index] = _jetton_current * 2;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_LOOK_CARD_RESULT);
		// 发送数据
		Look_Card_Result.Builder look_result = Look_Card_Result.newBuilder();
		look_result.setOpreatePlayer(seat_index);
		roomResponse.setCommResponse(PBUtil.toByteString(look_result));
		GRR.add_room_response(roomResponse);
		this.send_response_to_other(seat_index, roomResponse);
		observers().sendAll(roomResponse);

		// 发送自己数据
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int i = 0; i < GRR._card_count[seat_index]; i++) {
			look_result.addCardsData(GRR._cards_data[seat_index][i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(look_result));
		this.send_response_to_player(seat_index, roomResponse);

		_is_opreate_look[seat_index] = -1;

		if (seat_index == this._current_player) {
			_is_opreate_gen[seat_index] = _user_gen_score[seat_index];
		}

		if (_cur_jetton_round >= _compare_current) {
			_is_opreate_compare[_current_player] = _user_gen_score[_current_player] * 2;
		}

		Arrays.fill(_user_can_add_jetton_score[seat_index], 0);
		int index = 0;
		for (int x = 0; x < 6; x++) {
			if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
				if (this.isLookCard[seat_index]) {
					_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x] * 2;
				} else {
					_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x];
				}
			}
		}
		send_opreate(seat_index, _user_can_add_jetton_score[seat_index], false);
		return true;
	}

	public boolean deal_gen_jetton(int seat_index) {
		// 跟注
		if (_jetton_current > _jetton_max || _current_player == GameConstants.INVALID_SEAT) {
			return true;
		}
		if (isGiveup[seat_index] || isLose[seat_index]) {
			return true;
		}
		if (seat_index != this._current_player) {
			return true;
		}
		int jettonscore = _jetton_current;
		if (isLookCard[seat_index]) {
			jettonscore *= 2;
		}
		_user_jetton_score[seat_index] += jettonscore;
		_jetton_total_score += jettonscore;
		_b_round_opreate[seat_index] = true;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZJH_JETTON_RESULT);
		// 发送数据
		Score_Result.Builder score_result = Score_Result.newBuilder();

		score_result.setAddJettonScore(jettonscore);
		score_result.setOpreatePlayer(seat_index);
		boolean b_round = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			score_result.addJettonScore(_user_jetton_score[i]);
			score_result.addUserGenScore(_user_gen_score[i]);
			if (this.get_players()[i] == null || isGiveup[i] || isLose[i] || _player_ready[i] == 0) {
				continue;
			}
			if (!this._b_round_opreate[i]) {
				b_round = false;
			}
		}
		if (b_round) {
			Arrays.fill(_b_round_opreate, false);
			_cur_jetton_round++;
			send_jetton_round();
		}
		if (_cur_jetton_round >= _compare_current) {
			score_result.setCompareOpreate(1);
		} else {
			score_result.setCompareOpreate(0);
		}
		if (_cur_jetton_round > _jetton_round) {
			_current_player = GameConstants.INVALID_SEAT;
		} else {
			int nextplayer = (seat_index + 1) % this.getTablePlayerNumber();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[nextplayer] == null || isGiveup[nextplayer] || isLose[nextplayer] || _player_ready[nextplayer] == 0) {
					nextplayer = (nextplayer + 1) % this.getTablePlayerNumber();
				} else {
					break;
				}
			}
			_current_player = nextplayer;
		}

		score_result.setCurrentPlayer(_current_player);
		score_result.setJettonTotalScore(_jetton_total_score);
		score_result.setDisplayTime(20);
		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1) {
				count++;
			}
		}
		if (_jetton_total_score == (int) count * this.game_cell + jettonscore) {
			score_result.setJettonType(2);
		} else {
			score_result.setJettonType(1);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(score_result));
		GRR.add_room_response(roomResponse);

		this.send_response_to_room(roomResponse);
		observers().sendAll(roomResponse);

		if (_cur_jetton_round > _jetton_round) {

			// 轮数达到强制结束
			_current_player = GameConstants.INVALID_SEAT;
			_cur_jetton_round = _jetton_round;

			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), 2, TimeUnit.SECONDS);
			Arrays.fill(_is_opreate_gen, -1);
			Arrays.fill(_is_opreate_add, -1);
			Arrays.fill(_is_opreate_compare, -1);
			Arrays.fill(_is_opreate_give_up, -1);
			Arrays.fill(_is_opreate_look, -1);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Arrays.fill(_user_can_add_jetton_score[i], 0);
				int index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[i]) {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
						}
					}
				}
				send_opreate(i, _user_can_add_jetton_score[i], false);
			}
			return true;
		}

		_is_opreate_gen[seat_index] = 0;

		_is_opreate_compare[seat_index] = 0;

		if (_jetton_current == _jetton_max) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.isLose[i] || this.isGiveup[i]) {
					_is_opreate_add[i] = -1;
				} else {
					_is_opreate_add[i] = 0;
				}
			}
		} else {
			_is_opreate_add[seat_index] = 0;
			_is_opreate_add[_current_player] = 1;
		}
		// 操作玩家
		Arrays.fill(_user_can_add_jetton_score[seat_index], 0);
		int index = 0;
		for (int x = 0; x < 6; x++) {
			if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
				if (this.isLookCard[seat_index]) {
					_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x] * 2;
				} else {
					_user_can_add_jetton_score[seat_index][index++] = _can_add_jetton_score[x];
				}
			}
		}
		send_opreate(seat_index, _user_can_add_jetton_score[seat_index], false);

		if (_cur_jetton_round >= _compare_current) {
			_is_opreate_compare[_current_player] = _user_gen_score[_current_player] * 2;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == _current_player || i == seat_index || _player_ready[i] == 0) {
					continue;
				}
				Arrays.fill(_user_can_add_jetton_score[i], 0);
				index = 0;
				for (int x = 0; x < 6; x++) {
					if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
						if (this.isLookCard[i]) {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x] * 2;
						} else {
							_user_can_add_jetton_score[i][index++] = _can_add_jetton_score[x];
						}
					}
				}
				send_opreate(i, _user_can_add_jetton_score[i], false);
			}
		} else {
			_is_opreate_compare[_current_player] = 0;
		}
		_is_opreate_gen[_current_player] = _user_gen_score[_current_player];

		Arrays.fill(_user_can_add_jetton_score[_current_player], 0);
		index = 0;
		for (int x = 0; x < 6; x++) {
			if (_can_add_jetton_score[x] > this._jetton_current && _can_add_jetton_score[x] <= this._jetton_max) {
				if (this.isLookCard[_current_player]) {
					_user_can_add_jetton_score[_current_player][index++] = _can_add_jetton_score[x] * 2;
				} else {
					_user_can_add_jetton_score[_current_player][index++] = _can_add_jetton_score[x];
				}
			}
		}
		send_opreate(_current_player, _user_can_add_jetton_score[_current_player], false);
		return true;
	}

	// 发送玩家操作
	public void send_opreate(int seat_index, int score[], boolean first_round) {
		//
		RoomResponse.Builder roomResponse_opreate = RoomResponse.newBuilder();
		roomResponse_opreate.setType(MsgConstants.RESPONSE_ZJH_REFRESH_OPREATE);
		roomResponse_opreate.setGameStatus(this._game_status);
		// 发送数据
		User_Can_Opreate.Builder user_can_opreate = User_Can_Opreate.newBuilder();
		user_can_opreate.setLookCard(_is_opreate_look[seat_index]);
		user_can_opreate.setGiveUp(_is_opreate_give_up[seat_index]);
		user_can_opreate.setCompareCard(_is_opreate_compare[seat_index]);
		user_can_opreate.setAddScore(_is_opreate_add[seat_index]);

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1) {
				count++;
			}
		}
		if (_jetton_total_score == (int) count * this.game_cell) {
			user_can_opreate.setXiaZhu(_is_opreate_gen[seat_index]);
			user_can_opreate.setGenZhu(-1);
		} else {
			user_can_opreate.setXiaZhu(-1);
			user_can_opreate.setGenZhu(_is_opreate_gen[seat_index]);

		}
		if (_cur_jetton_round >= _compare_current) {
			user_can_opreate.setCanCompareRound(_is_opreate_compare[seat_index]);
		} else {
			user_can_opreate.setCanCompareRound(_compare_current - _cur_jetton_round);
		}

		user_can_opreate.setLiangPai(_is_opreate_liangpai[seat_index]);
		for (int i = 0; i < score.length; i++) {
			user_can_opreate.addAddJettonScore(score[i]);
		}

		roomResponse_opreate.setCommResponse(PBUtil.toByteString(user_can_opreate));
		this.send_response_to_player(seat_index, roomResponse_opreate);
	}

	public void send_jetton_round() {
		RoomResponse.Builder roomResponse_jetton_round = RoomResponse.newBuilder();
		roomResponse_jetton_round.setType(MsgConstants.RESPONSE_ZJH_JETTON_ROUND_REFRESH);
		roomResponse_jetton_round.setGameStatus(this._game_status);
		// 发送数据
		Jetton_Round.Builder jetton_round = Jetton_Round.newBuilder();
		if (_cur_jetton_round > _jetton_round) {
			jetton_round.setCurJettonRound(_jetton_round);
			jetton_round.setIsRoundFinish(1);
		} else {
			jetton_round.setCurJettonRound(_cur_jetton_round);
			jetton_round.setIsRoundFinish(0);
		}

		jetton_round.setJettonRound(_jetton_round);
		roomResponse_jetton_round.setCommResponse(PBUtil.toByteString(jetton_round));
		this.send_response_to_room(roomResponse_jetton_round);
		GRR.add_room_response(roomResponse_jetton_round);
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
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null) {
					_gameRoomRecord.release_players[i] = 0;
				}

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
				if (this.get_players()[i] != null) {
					if (_gameRoomRecord.release_players[i] == 1) {// 都同意了
						count++;
					}
				}

			}
			if (count == this.getPlayerCount()) {
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
				if (get_players()[i] == null) {
					continue;
				}
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
					if (player == null) {
						send_error_notify(i, 2, "游戏已被创建者解散");
					} else {
						if (i == seat_index) {
							send_error_notify(i, 2, "游戏已解散");
						} else {
							send_error_notify(i, 2, "游戏已被创建者解散");
						}
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
			observers().sendAll(refreshroomResponse);
			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);

			int _cur_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					if (_player_ready[i] != 0) {
						_cur_count += 1;
					}
				}
			}

			if ((this.getPlayerCount() >= 2) && (getPlayerCount() == _cur_count)) {
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				} else
					handler_game_start();
			} else {
				this.is_game_start = 0;
				control_game_start();
			}
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
	 * 刷新玩家信息
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
		return false;
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
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
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

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
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

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @return
	 */
	public boolean operate_player_cards() {

		return true;
	}

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
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
		player_result.setGameRuleDes(get_game_des());
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
			if (this.get_players()[i] == null)
				continue;
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
		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

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
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d,
			int delay) {
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}

		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(this.getRoom_id(), seat_index, provide_player, center_card, action, type, depatch, self, d), delay,
					TimeUnit.MILLISECONDS);
		} else {

		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {

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
			if (this.get_players()[i] == null)
				continue;
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee && SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			istrustee[get_seat_index] = isTrustee;
		}
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
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
		if (is_mj_type(GameConstants.GAME_TYPE_ZJH_JD)) {
			return GameConstants.GAME_PLAYER_SIX;
		}
		return GameConstants.GAME_PLAYER_SIX;
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
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(this.get_players()[i].getAccount_id(), score, false,
					buf.toString(), EMoneyOperateType.ROOM_COST);
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
