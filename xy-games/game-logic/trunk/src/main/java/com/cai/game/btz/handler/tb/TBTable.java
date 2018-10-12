/**
 * 
 */
package com.cai.game.btz.handler.tb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.BTZConstants;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddJettonRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OpenCardRunnable;
import com.cai.future.runnable.ReadyRunnable;
import com.cai.future.runnable.RobotBankerRunnable;
import com.cai.future.runnable.TbAddJettonRunnable;
import com.cai.future.runnable.TbReadyRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.btz.BTZCardGroup;
import com.cai.game.btz.BTZCardType;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.BTZUtils;
import com.cai.game.btz.handler.BTZHandler;
import com.cai.game.btz.handler.BTZHandlerFinish;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.btz.BTZRsp.AddJetton_BTZ;
import protobuf.clazz.btz.BTZRsp.CallBankerInfo_BTZ;
import protobuf.clazz.btz.BTZRsp.CallBanker_BTZ;
import protobuf.clazz.btz.BTZRsp.GameStart_BTZ;
import protobuf.clazz.btz.BTZRsp.OpenCard_BTZ;
import protobuf.clazz.btz.BTZRsp.PukeGameEndBTZ;
import protobuf.clazz.btz.BTZRsp.RoomPlayerResponseBTZ;
import protobuf.clazz.btz.BTZRsp.SendCard_BTZ;
import protobuf.clazz.btz.BTZRsp.Timer_OX_BTZ;
import protobuf.clazz.btz.BTZRsp.Trustee_BTZ;

///////////////////////////////////////////////////////////////////////////////////////////////
/**
 * @author demon_t date: 2017年8月9日 下午3:54:25 <br/>
 */
public class TBTable extends BTZTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private ScheduledFuture<?> _release_scheduled;
	private ScheduledFuture<?> _table_scheduled;
	private ScheduledFuture<?> _game_scheduled;

	public BTZCardGroup[] groups = new BTZCardGroup[getTablePlayerNumber()];

	private long _request_release_time;

	public BTZHandler<? super TBTable> _handler;

	public TBHandlerCallBanker _handler_Call_banker;
	public TBHandlerAddJetton _handler_add_jetton;
	public TBHandlerOpenCard _handler_open_card;

	// 豹子以及豹子以上玩家 豹子坐庄玩法需要用到
	public BTZCardGroup doubleGroup = null;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public int request_player_seat = GameConstants.INVALID_SEAT;
	int[] release_players;

	public int _win_num[] = new int[this.getTablePlayerNumber()];// 赢的局数
	public int _lose_num[] = new int[this.getTablePlayerNumber()];// 输的局数
	public int _game_score_max[] = new int[this.getTablePlayerNumber()];// 得分最高
	public int trusteeJetton[] = new int[this.getTablePlayerNumber()]; // 托管后的下分信息
	public boolean trusteeAdd[] = new boolean[this.getTablePlayerNumber()]; // 是否垒注
	public boolean trusteeBanker[] = new boolean[this.getTablePlayerNumber()]; // 是否抢庄

	public TBTable(int game_rule_index) {
		super(game_rule_index);
		this._game_rule_index = game_rule_index;
		// 玩家
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i] = null;

		}

		_game_status = GameConstants.GS_OX_FREE;
		// 游戏变量

		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_call_banker = new int[getTablePlayerNumber()]; // 用户是否叫庄
		_add_Jetton = new int[getTablePlayerNumber()]; // 用户下注
		release_players = new int[getTablePlayerNumber()]; // 用户下注
		_open_card = new boolean[getTablePlayerNumber()]; // 用户摊牌
		_cur_call_banker = 0;
		_player_status = new boolean[getTablePlayerNumber()];

		_call_banker_info = new int[5];
		_can_tuizhu_player = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_can_tuizhu_player[i] = 0;
			groups[i] = new BTZCardGroup(this, i);
		}
		_banker_max_times = 1;
		game_cell = 1;
	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;

		_game_round = game_round;
		_cur_round = 0;
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_jetton_info_sever_ox = new int[3];

		_jetton_info_sever_ox[0] = 1;
		_jetton_info_sever_ox[1] = 2;
		_jetton_info_sever_ox[2] = 3;
		_jetton_info_cur = new int[getTablePlayerNumber()][5];
		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
		}

		game_cell = 1;
		_banker_times = 1;
		_banker_max_times = 1;
		istrustee = new boolean[getTablePlayerNumber()];

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), getTablePlayerNumber());

		_handler_add_jetton = new TBHandlerAddJetton();
		_handler_open_card = new TBHandlerOpenCard();
		_handler_Call_banker = new TBHandlerCallBanker();

		_handler_finish = new BTZHandlerFinish();
		_call_banker = new int[getTablePlayerNumber()]; // 用户是否叫庄
		_add_Jetton = new int[getTablePlayerNumber()]; // 用户下注
		_open_card = new boolean[getTablePlayerNumber()]; // 用户摊牌
		_player_status = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}
		_jetton_info_cur = new int[getTablePlayerNumber()][5];
		_cur_call_banker = 0;
		_cur_banker = 0;
		_banker_times = 1;

		this.setMinPlayerCount(2); // 推饼最小开局人数2
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////
	public boolean reset_init_data() {

		record_game_room();

		GRR = new GameRoundRecord(getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(BTZConstants.HAND_CARD_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}
		_call_banker = new int[getTablePlayerNumber()]; // 用户是否叫庄
		_add_Jetton = new int[getTablePlayerNumber()]; // 用户下注
		_open_card = new boolean[getTablePlayerNumber()]; // 用户摊牌
		_player_status = new boolean[getTablePlayerNumber()];
		_jetton_info_cur = new int[getTablePlayerNumber()][5];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;

		}
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		GRR._video_recode.setBankerPlayer(this._cur_banker);
		_banker_max_times = 1;
		game_cell = 1;
		return true;
	}

	// 游戏开始
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_OX_FREE;

		reset_init_data();

		GRR._banker_player = _cur_banker;
		// if (has_rule(GameConstants.GAME_RULE_MAX_ONE_TIMES))
		_banker_max_times = 1;
		// if (has_rule(GameConstants.GAME_RULE_MAX_TWO_TIMES))
		// _banker_max_times = 2;
		// if (has_rule(GameConstants.GAME_RULE_MAX_THREE_TIMES))
		// _banker_max_times = 3;
		// if (has_rule(GameConstants.GAME_RULE_MAX_FOUR_TIMES))
		// _banker_max_times = 4;
		//

		_repertory_card = new int[getCard().length];
		shuffle(_repertory_card, getCard());

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
		if (has_rule(BTZConstants.BTZ_RULE_FANG_ZHU_ZHUANG)) {
			return game_start_fang_zhu_zhuang();
		}
		if (has_rule(BTZConstants.BTZ_RULE_DOULE_ZHUANG)) {
			return game_start_bao_zi_zhuang();
		}
		if (has_rule(BTZConstants.TB_RULE_LUN) && this._cur_round > 1) {
			return game_start_fang_lun_zhuang();
		}
		return call_banker_ZYQOX();
	}

	protected boolean game_start_fang_lun_zhuang() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态

		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		int index = this._cur_banker;
		int number = 0;
		do {
			number++;
			index = (index + 1) % this.getTablePlayerNumber();
			if (get_players()[index] == null) {
				continue;
			} else {
				this._cur_banker = index;
				break;
			}
		} while (number < this.getTablePlayerNumber());
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
				if (i == this._cur_banker) {
					this.doubleGroup = groups[this._cur_banker];
				}
			} else {
				this._player_status[i] = false;
			}
		}

		on_game_start();
		return true;
	}

	private boolean game_start_bao_zi_zhuang() {
		if (doubleGroup != null) {
			this._cur_banker = doubleGroup.seat_index;
			// 游戏开始
			this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态
			if (this._game_scheduled != null)
				this.kill_timer();
			this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null) {
					this._player_status[i] = true;
				} else {
					this._player_status[i] = false;
				}
			}
			on_game_start();
		} else {
			call_banker_ZYQOX();
		}

		return true;
	}

	protected boolean game_start_fang_zhu_zhuang() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态

		_cur_banker = 0;
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
				if (this.get_players()[i].getAccount_id() == getCreate_player().getAccount_id()) {
					_cur_banker = this.get_players()[i].get_seat_index();
					this.doubleGroup = groups[this._cur_banker];
				}
			} else {
				this._player_status[i] = false;
			}
		}

		on_game_start();
		return true;
	}

	public boolean call_banker_ZYQOX() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_CALL_BANKER;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		this.load_room_info_data(roomResponse);
		CallBankerInfo_BTZ.Builder call_banker_info = CallBankerInfo_BTZ.newBuilder();
		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}
		if (this._cur_round == 1) {
			// shuffle_players();
			this.load_player_info_data(roomResponse);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this._player_status[i] != true) {
				continue;
			}

			call_banker_info.setDisplayTime(_cur_operate_time);

			roomResponse.setType(BTZConstants.RESPONSE_CALL_BANKER);
			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_info));
			this.send_response_to_player(i, roomResponse);
		}
		if (observers().count() > 0) {
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, this._game_status);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.isTrutess(i)) { // 自动叫庄
				this.handler_call_banker(i, this.trusteeBanker[i] ? 1 : 0);
			}
		}

		return true;
	}

	public void add_call_banker(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 回放数据
		CallBanker_BTZ.Builder call_banker = CallBanker_BTZ.newBuilder();
		call_banker.setSeatIndex(seat_index);
		call_banker.setCallBanker(_call_banker[seat_index]);
		roomResponse.setType(BTZConstants.RESPONSE_SELECT_BANKER);
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker));

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// if(this._player_status[i] != true)
			// continue;
			if (this.get_players()[i] == null) {
				continue;
			}
			this.send_response_to_player(i, roomResponse);
		}
		if (observers().count() > 0) {
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		// 回放
		this.GRR.add_room_response(roomResponse);
	}

	public void add_jetton_ox(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 回放数据
		AddJetton_BTZ.Builder add_jetton = AddJetton_BTZ.newBuilder();
		add_jetton.setSeatIndex(seat_index);
		add_jetton.setJettonScore(_add_Jetton[seat_index]);
		roomResponse.setType(BTZConstants.RESPONSE_ADD_JETTON);
		roomResponse.setCommResponse(PBUtil.toByteString(add_jetton));

		this.load_player_info_data(roomResponse);
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			this.send_response_to_player(i, roomResponse);
		}
		if (observers().count() > 0) {
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		// 回放
		this.GRR.add_room_response(roomResponse);
	}

	public void send_card_date_ox() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_OPEN_CARD;// 设置状态
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		int touzi1 = RandomUtil.getRandomNumber(6) + 1;
		int touzi2 = RandomUtil.getRandomNumber(6) + 1;
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			SendCard_BTZ.Builder send_card = SendCard_BTZ.newBuilder();
			send_card.addTouZi(touzi1);
			send_card.addTouZi(touzi2);
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
							cards.addItem(GRR._cards_data[k][j]);

						}
					} else {
						for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}
			send_card.setDisplayTime(this._cur_operate_time);

			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			roomResponse.setType(BTZConstants.RESPONSE_SEND_CARD);
			this.send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			SendCard_BTZ.Builder send_card = SendCard_BTZ.newBuilder();
			send_card.addTouZi(touzi1);
			send_card.addTouZi(touzi2);
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				}
				// 回放数据
				this.GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}
			send_card.setDisplayTime(this._cur_operate_time);

			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			roomResponse.setType(BTZConstants.RESPONSE_SEND_CARD);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_SEND_CARD);

		SendCard_BTZ.Builder send_card = SendCard_BTZ.newBuilder();
		send_card.addTouZi(touzi1);
		send_card.addTouZi(touzi2);
		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] == true) {
				for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			} else {
				for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			}

			send_card.addSendCard(k, cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return;
	}

	public boolean game_start() {
		// 游戏开始
		this._game_status = GameConstants.GS_OX_ADD_JETTON;// 设置状态

		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		for (int j = _call_banker_info.length - 1; j >= 0; j--) {
			int chairID[] = new int[getTablePlayerNumber()];
			int chair_count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this._player_status[i] == false)
					continue;
				if ((this._player_status[i] == true) && (_call_banker_info[j] == this._call_banker[i])) {
					chairID[chair_count++] = i;
				}
			}
			if (chair_count > 0) {
				Random random = new Random();
				int temp = random.nextInt(chair_count);

				this._cur_banker = chairID[temp];
				this.doubleGroup = groups[this._cur_banker];
				_banker_times = _call_banker[this._cur_banker];
				if (_banker_times == 0)
					_banker_times = 1;
				break;
			}
		}

		on_game_start();
		return true;
	}

	public void on_game_start() {
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

			this.load_room_info_data(roomResponse);
			this.load_player_info_data(roomResponse);
			GameStart_BTZ.Builder game_start = GameStart_BTZ.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if ((i != this._cur_banker && this._player_status[i] == true)) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((i != this._cur_banker) && (this._player_status[k] == true)) {
						for (int j = 0; j < 3; j++) {
							cards.addItem(_jetton_info_sever_ox[j]);
							_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
						}

						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][3] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][3]);
						}
					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}
			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}

			game_start.setDisplayTime(this._cur_operate_time);
			roomResponse.setCommResponse(PBUtil.toByteString(game_start));
			roomResponse.setType(BTZConstants.RESPONSE_GAME_START);
			this.send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			GameStart_BTZ.Builder game_start = GameStart_BTZ.newBuilder();
			game_start.setCurBanker(_cur_banker);

			for (int k = 0; k < getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] == true) {
					for (int j = 0; j < 3; j++) {
						cards.addItem(_jetton_info_sever_ox[j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
					}
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][3] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][3]);
					}
				}
				game_start.addJettonCell(k, cards);
			}
			game_start.setDisplayTime(this._cur_operate_time);
			roomResponse.setCommResponse(PBUtil.toByteString(game_start));
			this.load_player_info_data(roomResponse);
			roomResponse.setType(BTZConstants.RESPONSE_GAME_START);
			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_room_info_data(roomResponse);
		GameStart_BTZ.Builder game_start = GameStart_BTZ.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (((k != this._cur_banker) && (this._player_status[k] == true))) {
				for (int j = 0; j < 3; j++) {
					cards.addItem(_jetton_info_sever_ox[j]);
					_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
				}
				if (_can_tuizhu_player[k] > 0) {
					_jetton_info_cur[k][3] = _can_tuizhu_player[k];
					cards.addItem(_jetton_info_cur[k][2]);
				}

			}
			game_start.addJettonCell(k, cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(game_start));
		roomResponse.setType(BTZConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);
		this._handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, this._game_status);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.isTrutess(i) && i != this._cur_banker) {
				int value = this.trusteeJetton[i];
				if (this.trusteeAdd[i] && this._jetton_info_cur[i][3] > 0) { // 垒注
					value = 3;
				}
				GameSchedule.put(new TbAddJettonRunnable(this.getRoom_id(), i, value), 2, TimeUnit.SECONDS);
			}
		}
	}

	public void open_card_ox(int seat_index) {
		// 发送数据
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		OpenCard_BTZ.Builder open_card = groups[seat_index].encode();
		roomResponse.setType(BTZConstants.RESPONSE_OPEN_CARD);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		if (observers().count() > 0) {
			RoomUtil.send_response_to_observer(this, roomResponse);
		}
		if (GRR != null) {
			this.GRR.add_room_response(roomResponse);
		}

	}

	public void process_openCard() {

		BTZCardGroup group = null;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// if (_ping_Player_ox[i] == true)
			// continue;
			if (_player_status[i] == false)
				continue;

			if (i == this._cur_banker) {
				continue;
			}

			int baseScore = groups[i].calScore(groups[_cur_banker]);
			if (has_rule(BTZConstants.TB_RULE_THREE)) {
				baseScore = groups[i].calScoreThree(groups[_cur_banker]);
			}

			if (baseScore > 0 && groups[i].type.get() >= BTZCardType.DOUBLE.get()) {
				// 豹子以上的保存。豹子坐庄
				if (group == null) {
					group = groups[i];
				} else if (groups[i].compareTo(group) == BTZConstants.WIN) {
					group = groups[i];
				}
			}
			baseScore = baseScore * _banker_times * _add_Jetton[i];

			// 胡牌分
			GRR._game_score[i] += baseScore;
			GRR._game_score[this._cur_banker] -= baseScore;
		}

		int tui_zhu_max = 0;

		// 检查推注封顶
		if (has_rule(BTZConstants.BTZ_RULE_24_LEI)) {
			tui_zhu_max = 24;
		} else if (has_rule(BTZConstants.BTZ_RULE_48_LEI)) {
			tui_zhu_max = 48;
		} else if (has_rule(BTZConstants.BTZ_RULE_LEI)) {
			tui_zhu_max = Integer.MAX_VALUE;
		}

		if (tui_zhu_max > 0) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_status[i] == false)
					continue;
				if (i == this._cur_banker)
					continue;
				if (this._can_tuizhu_player[i] >= 0) {
					if (GRR._game_score[i] > 0) {
						int temp = (int) GRR._game_score[i] + this._add_Jetton[i];
						this._can_tuizhu_player[i] = temp;

						this._can_tuizhu_player[i] = Math.min(tui_zhu_max, this._can_tuizhu_player[i]);
					} else if (GRR._game_score[i] < 0) {
						this._can_tuizhu_player[i] = 0;
					}
				}
			}

		}

		if (group != null) {
			if (group != doubleGroup && !has_rule(BTZConstants.BTZ_RULE_FANG_ZHU_ZHUANG)) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					this._can_tuizhu_player[i] = 0;
				}
			}
			doubleGroup = group;
		}

	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	protected int[] getCard() {
		return BTZConstants.CARD_DATAS;
	}

	/// 洗牌
	protected void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				BTZUtils.random_card_data(repertory_card, mj_cards);
			else
				BTZUtils.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		// repertory_card[0]= 0x21;
		// repertory_card[1]= 0x21;
		// repertory_card[2]= 0x21;
		// repertory_card[3]= 0x21;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * BTZConstants.HAND_CARD_COUNT + j];
			}
		}
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			groups[i].reset(GRR._cards_data[i], 1);
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {

		int cards[][] = { { 0x22, 0x28 }, { 0x21, 0x29 } };

		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				GRR._cards_data[i][j] = cards[i][j];
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > BTZConstants.HAND_CARD_COUNT) {
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

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		int count = realyCards.length / BTZConstants.HAND_CARD_COUNT;
		if (count > 6)
			count = 6;
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i++) {
				for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == count)
				break;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < BTZConstants.HAND_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		// 发牌
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return this.handler_game_finish_tbox(seat_index, reason);
	}

	public boolean handler_game_finish_tbox(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		if (this._game_scheduled != null)
			this.kill_timer();
		this.set_timer(GameConstants.HJK_READY_TIMER, GameConstants.HJK_READY_TIME_SECONDS, true);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_GAME_END);

		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		PukeGameEndBTZ.Builder game_end_btz = PukeGameEndBTZ.newBuilder();

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		game_end.setRoundOverType(1);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setCellScore(GameConstants.CELL_SCORE);

		game_end.setRoomInfo(roomResponse.getRoomInfo());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end_btz.addWinCounts(_win_num[i]);
			game_end_btz.addLoseCounts(_lose_num[i]);
			game_end_btz.addScoreMax(_game_score_max[i]);
			game_end_btz.addScore(_player_result.game_score[i]);
		}

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);

			// 特别显示的牌
			GRR._end_type = reason;
			this.load_player_info_data(roomResponse);
			game_end.setBankerPlayer(GRR._banker_player);// 专家

			for (int i = 0; i < getTablePlayerNumber(); i++)
				game_end.addGameScore(GRR._game_score[i]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
				if (this._game_scheduled != null)
					this.kill_timer();
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN)
				real_reason = reason;
			else
				real_reason = GameConstants.Game_End_RELEASE_PLAY;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		// game_end_btz.setRealReason(real_reason);
		// game_end_btz.addAllPlayers(load_player_info_data());
		game_end.setCommResponse(PBUtil.toByteString(game_end_btz));

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end) { // 删除
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		} else {
			GameSchedule.put(new TbReadyRunnable(this.getRoom_id()), 2, TimeUnit.SECONDS);
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	public void countAllScore(int _seat_index) {

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];

			if (this.GRR._game_score[i] > _game_score_max[i]) {
				_game_score_max[i] = (int) this.GRR._game_score[i];
			}

			if (this.GRR._game_score[i] > 0) {
				_win_num[i]++;
			} else if (this.GRR._game_score[i] < 0) {
				_lose_num[i]++;
			}
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) {//
			if (this.get_players()[seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
				return false;
			}
			_player_ready[seat_index] = 0;
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
			return false;
		}
		if (this.get_players()[seat_index] == null) {
			return false;
		}

		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
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
		int _player_count = 0;
		int _cur_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}

		if ((_player_count >= 2) && (_player_count == _cur_count)) {
			handler_game_start();
		}

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if (this.isTrutess(seat_index)) {
			this.reSendTrusteeToPlayer(seat_index);
		}
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);
			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
		}
		if (_gameRoomRecord != null) {
			if (request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(release_players[i]);
				}

				this.send_response_to_player(seat_index, roomResponse);

				if (this.isTrutess(seat_index)) {
					this.reSendTrusteeToPlayer(seat_index);
				}
			}
		}

		return true;
		// handler_player_ready(seat_index);

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

		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card, luoCode);
		}

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
		if (this._handler != null) {
			this._handler.handler_call_banker(this, seat_index, call_banker);
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

		if (this._handler != null) {
			this._handler.handler_add_jetton(this, seat_index, jetton);
		}

		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {

		if (this._handler != null && this.GRR != null) {
			this._handler.handler_open_cards(this, seat_index, open_flag);
		}

		return true;
	}

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
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
			playerNumber = getTablePlayerNumber();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}
			if (this._game_scheduled != null) {
				this.kill_timer();
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

			for (int i = 0; i < playerNumber; i++) {
				release_players[i] = 0;
			}

			request_player_seat = seat_index;
			release_players[seat_index] = 1;// 同意

			int count = 0;
			for (int i = 0; i < playerNumber; i++) {
				if (release_players[i] == 1) {// 都同意了
					count++;
				}
			}
			if (count == playerNumber) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < playerNumber; j++) {
					player = this.get_players()[j];
					if (player == null) {
						continue;
					}
					send_error_notify(j, 1, "游戏解散成功!");
				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(release_players[i]);
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
			if (request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (release_players[seat_index] == 1)
				return false;

			release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < playerNumber; i++) {
				if (get_players()[i] == null)
					continue;
				if (release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < playerNumber; j++) {
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
			if (request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			release_players[seat_index] = 2;
			request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.set_timer(this._cur_game_timer, this._cur_operate_time, false);
			Timer_OX_BTZ.Builder timer = Timer_OX_BTZ.newBuilder();
			timer.setDisplayTime(_cur_operate_time);
			roomResponse.setCommResponse(PBUtil.toByteString(timer));
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;

			for (int i = 0; i < playerNumber; i++) {
				release_players[i] = 0;
			}

			for (int j = 0; j < playerNumber; j++) {
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

				for (int i = 0; i < playerNumber; i++) {
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

			if (this._player_status[seat_index] == true) {
				if (GameConstants.GS_MJ_FREE != _game_status) {
					// 游戏已经开始
					return false;
				}
				send_error_notify(seat_index, 2, "您已经开始游戏了,不能退出游戏");
				return false;
			}
			// if (has_rule(GameConstants.GAME_RULE_AA_PAY)) {
			// this.huan_dou_aa(seat_index);
			// }
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}

			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);
			int _cur_count = 0;
			int player_count = 0;
			boolean flag = false;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					_cur_count += 1;
				}
				if (this._player_status[i] == true)
					flag = true;
				if (_player_ready[i] == 0) {

				}
				if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
					player_count += 1;
				}
			}
			if ((player_count >= 2) && (player_count == _cur_count) && (flag == false))
				handler_game_start();

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);
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
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				if ((weaveitems[j].weave_kind == GameConstants.WIK_TI_LONG || weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG)
						&& weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

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
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
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
		if (observers().exist(player.getAccount_id())) {
			this.handler_exit_room_observer(player);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setGameStatus(_game_status);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse);

			send_response_to_other(player.get_seat_index(), roomResponse);
		}
		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
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

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		// 特殊处理庄家 (不知道这个游戏为什么没有记录庄家位置)
		int seatIndex = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Player player = this.get_players()[i];
			if (player != null && player.getAccount_id() == this.getCreate_player().getAccount_id()) {
				seatIndex = i;
				break;
			}
		}
		room_player.setSeatIndex(seatIndex);
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(this._cur_banker);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {

			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
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
	public List<RoomPlayerResponseBTZ> load_player_info_data() {
		Player rplayer;
		List<RoomPlayerResponseBTZ> temp = new ArrayList<>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseBTZ.Builder room_player = RoomPlayerResponseBTZ.newBuilder();
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
			temp.add(room_player.build());
		}
		return temp;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {
		this._handler = this._handler_finish;
		this._handler_finish.exe(this);
		return true;
	}

	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {

		return true;

	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_card;
			this._handler_dispath_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		roomResponse.setType(BTZConstants.RESPONSE_TRUSTEE_RE);

		istrustee[get_seat_index] = isTrustee;

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		Trustee_BTZ.Builder trustee = Trustee_BTZ.newBuilder();

		trustee.setAddJetton(this.trusteeJetton[get_seat_index]);
		trustee.setIsAddJetten(this.trusteeAdd[get_seat_index]);
		trustee.setIsCallBanker(this.trusteeBanker[get_seat_index]);
		trustee.setIsTrustee(this.istrustee[get_seat_index]);

		roomResponse.setCommResponse(PBUtil.toByteString(trustee));

		this.send_response_to_player(get_seat_index, roomResponse);

		return true;
	}

	public void sendTrusteeToPlayer(int seatIndex, RoomResponse.Builder roomResponse) {
	}

	/**
	 * 重连
	 * 
	 * @param seatIndex
	 */
	public void reSendTrusteeToPlayer(int seatIndex) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_TRUSTEE_RE);
		roomResponse.setOperatePlayer(seatIndex);

		Trustee_BTZ.Builder trustee = Trustee_BTZ.newBuilder();

		trustee.setAddJetton(this.trusteeJetton[seatIndex]);
		trustee.setIsAddJetten(this.trusteeAdd[seatIndex]);
		trustee.setIsCallBanker(this.trusteeBanker[seatIndex]);
		trustee.setIsTrustee(this.istrustee[seatIndex]);

		roomResponse.setCommResponse(PBUtil.toByteString(trustee));

		this.send_response_to_player(seatIndex, roomResponse);
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		// // 发牌
		// this._handler = this._handler_chuli_firstcards;
		// this._handler_chuli_firstcards.reset_status(_seat_index, _type);
		// this._handler.exe(this);
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;
	}

	/*
	 */
	@Override
	public int getTablePlayerNumber() {
		if (has_rule(BTZConstants.BTZ_RULE_FOUR)) {
			return 4;
		}

		if (has_rule(BTZConstants.BTZ_RULE_FIVE)) {
			return 6;
		}

		if (has_rule(BTZConstants.BTZ_RULE_EIGHT)) {
			return 8;
		}
		return 8;
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {

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

	public boolean set_timer(int timer_type, int time, boolean makeDBtimer) {
		// if (!is_sys())
		// return false;

		SysParamModel sysParamModel3008 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(3008);
		if (sysParamModel3008.getVal5() == 0)
			return true;
		if (makeDBtimer == false) {
			_cur_game_timer = timer_type;
			if (timer_type == GameConstants.HJK_READY_TIMER) {
				_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
				_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
				_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				this._cur_operate_time = time;
			}
			return true;
		}
		_cur_game_timer = timer_type;
		if (timer_type == GameConstants.HJK_READY_TIMER) {
			_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), sysParamModel3008.getVal1(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal1();
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), sysParamModel3008.getVal2(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal2();
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), sysParamModel3008.getVal3(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal3();
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), sysParamModel3008.getVal4(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			this._cur_operate_time = sysParamModel3008.getVal4();
		}
		return true;

	}

	public boolean kill_timer() {
		// if (!is_sys())
		// return false;
		_game_scheduled.cancel(false);
		_game_scheduled = null;

		return false;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	public boolean open_card_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._open_card[i] == false) {
				this._handler.handler_open_cards(this, i, true);
			}
		}
		return false;
	}

	public boolean robot_banker_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._call_banker[i] == -1) {

				this._handler.handler_call_banker(this, i, 0);

			}
		}
		return false;
	}

	public boolean ready_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++)
			if (this.get_players()[i] != null) {
				if (this._player_ready[i] == 0) {
					handler_player_ready(i, false);
				}
			}
		return false;
	}

	public boolean add_jetton_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false)
				continue;
			if (this._add_Jetton[i] == 0) {

				this._handler.handler_add_jetton(this, i, 0);

			}
		}
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_BTZ_TRUSTEE) { // 托管
			if (seat_index < 0) {
				return false;
			}
			Trustee_BTZ trustee = PBUtil.toObject(room_rq, Trustee_BTZ.class);
			if (trustee.getIsTrustee()) {
				this.trusteeJetton[seat_index] = trustee.getAddJetton();
				if (has_rule(BTZConstants.BTZ_RULE_FANG_ZHU_ZHUANG) || has_rule(BTZConstants.BTZ_RULE_DOULE_ZHUANG)) {
					this.trusteeAdd[seat_index] = trustee.getIsAddJetten();
				}
				this.trusteeBanker[seat_index] = trustee.getIsCallBanker();
			} else {
				this.trusteeJetton[seat_index] = -1;
				this.trusteeAdd[seat_index] = false;
				this.trusteeBanker[seat_index] = false;
			}
			this.handler_request_trustee(seat_index, trustee.getIsTrustee(), 0);

			if (trustee.getIsTrustee()) {
				switch (_game_status) {
				case GameConstants.GS_OX_CALL_BANKER:
					this.handler_call_banker(seat_index, this.trusteeBanker[seat_index] ? 1 : 0);
					break;
				case GameConstants.GS_OX_ADD_JETTON:
					int value = this.trusteeJetton[seat_index];
					if (this.trusteeAdd[seat_index] && this._jetton_info_cur[seat_index][3] > 0) { // 垒注
						value = 3;
					}
					this._handler.handler_add_jetton(this, seat_index, value);
					break;
				case GameConstants.GAME_STATUS_WAIT:
					this.handler_player_ready(seat_index, false);
					break;
				}
			}
		}
		return false;
	}

	@Override
	public void runnable_remove_out_cards(int _seat_index, int _type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void runnable_add_discard(int _seat_index, int _card_count, int[] _card_data, boolean _send_client) {
		// TODO Auto-generated method stub

	}
}
