/**
 * 
 */
package com.cai.game.xykl;

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
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.xykl.xyklRsp.CallBankerResult;
import protobuf.clazz.xykl.xyklRsp.EnterRoomConfirm;
import protobuf.clazz.xykl.xyklRsp.GameStart_Xykl;
import protobuf.clazz.xykl.xyklRsp.JettonArea;
import protobuf.clazz.xykl.xyklRsp.JettonResult;
import protobuf.clazz.xykl.xyklRsp.OpenCardResult;
import protobuf.clazz.xykl.xyklRsp.Opreate_Request_Xykl;
import protobuf.clazz.xykl.xyklRsp.PukeGameEndXykl;
import protobuf.clazz.xykl.xyklRsp.RefreshCardsXykl;
import protobuf.clazz.xykl.xyklRsp.RoomInfoXykl;
import protobuf.clazz.xykl.xyklRsp.RoomPlayerResponseXykl;
import protobuf.clazz.xykl.xyklRsp.SendCardsXykl;
import protobuf.clazz.xykl.xyklRsp.TableResponse_Xykl;

///////////////////////////////////////////////////////////////////////////////////////////////
public class XYKLTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;
	private static final int ID_TIMER_ANIMATION_START = 1;// 自动开始
	private static final int ID_TIMER_ANIMATION_CALL_BANKER = 2;// 自动叫庄
	private static final int ID_TIMER_ANIMATION_JETTON = 3;// 自动下注
	private static final int ID_TIMER_ANIMATION_OPEN_CARD = 4;// 自动开牌
	private static final int ID_TIMER_ANIMATION_CUO_PAI = 5;// 搓牌限时

	private static Logger logger = Logger.getLogger(XYKLTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	public int is_game_start; // 游戏是否开始

	public boolean istrustee[]; // 托管状态
	public ScheduledFuture _trustee_schedule[];// 托管定时器

	public XYKLGameLogic _logic = null;

	public int _banker_select = GameConstants.INVALID_SEAT;

	//
	public int _current_player = GameConstants.INVALID_SEAT;
	public int _prev_palyer = GameConstants.INVALID_SEAT;
	public int _pre_opreate_type;

	private long _request_release_time;
	public int _cur_game_timer; // 当前游戏定时器
	public int _cur_operate_time; // 可操作时间
	public int _operate_start_time; // 操作开始时间
	public int _scheduled_start_time;// 开始定时器时间
	public int _scheduled_call_banker_time;// 叫庄定时器时间
	public int _scheduled_jetton_time;// 下注定时器时间
	public int _scheduled_open_card_time;// 开牌定时器时间
	public int _scheduled_cuo_pai_time;// 搓牌定时器时间
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;
	private ScheduledFuture _game_scheduled;

	public int _jetton_total_score;
	public int _jetton_total_temp_score;
	public int _user_jetton_score[];
	public int _di_zhu_score[];
	public int _end_score[];
	public int _is_qiang_mai[];
	public boolean _is_open_card[];
	public int _is_cuo_pai[];
	public int _user_jetton_max;
	public int _user_jetton_min;
	public boolean _is_half_join[];
	public long _start_time;

	public XYKLTable() {
		super(RoomType.HH, 6);

		_logic = new XYKLGameLogic();
		// 设置等待状态

	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		is_game_start = 0;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_is_qiang_mai = new int[getTablePlayerNumber()];
		_is_open_card = new boolean[getTablePlayerNumber()];
		_is_cuo_pai = new int[getTablePlayerNumber()];
		_user_jetton_score = new int[getTablePlayerNumber()];
		_di_zhu_score = new int[getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()];
		_is_half_join = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_is_open_card[i] = false;
			_is_cuo_pai[i] = 0;
			_is_half_join[i] = true;
		}

		playerNumber = 0;
		_scheduled_start_time = 15;
		_scheduled_call_banker_time = 20;
		_scheduled_jetton_time = 20;
		_scheduled_open_card_time = 10;
		_scheduled_cuo_pai_time = 10;

		this.setMinPlayerCount(2);
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

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, this.get_hand_card_count_max(),
				GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
			if (this._player_ready[i] == 0) {
				_is_qiang_mai[i] = -1;
			} else {
				_is_qiang_mai[i] = 0;
			}

			_is_open_card[i] = false;
			_is_cuo_pai[i] = 0;
			_user_jetton_score[i] = 0;
			_di_zhu_score[i] = 0;
			_end_score[i] = 0;

		}
		_cur_round++;
		if (_cur_round == 1) {
			_start_time = GRR._start_time;
		}
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
		//
		_repertory_card = new int[GameConstants.CARD_COUNT_XYKL];
		shuffle(_repertory_card, GameConstants.CARD_DATA_XYKL);

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		int value = this.getRuleValue(GameConstants.GAME_RULE_XYKL_INITIAL);
		if (value == 1) {
			this.game_cell = 5;
			this._user_jetton_max = 200;
		} else if (value == 2) {
			this.game_cell = 10;
			this._user_jetton_max = 400;
		} else if (value == 3) {
			this.game_cell = 20;
			this._user_jetton_max = 800;
		}

		_pre_opreate_type = 0;
		// progress_banker_select();
		_banker_select = GameConstants.INVALID_SEAT;
		this._current_player = GameConstants.INVALID_SEAT;

		kill_timer();
		return game_start_xykl();
	}

	private void progress_banker_select() {
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

	// 闲逸咔陇开始
	public boolean game_start_xykl() {
		_game_status = GameConstants.GS_CALL_BANKER;// 设置状态

		int is_jetton = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1) {
				if (this.has_rule(GameConstants.GAME_RULE_DICHI_KONG_XIAZHU)) {
					_di_zhu_score[i] = (int) game_cell;
					is_jetton = 1;
				} else {
					if (_jetton_total_score == 0) {
						_di_zhu_score[i] = (int) game_cell;
						is_jetton = 1;
					}

				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_jetton_total_score += _di_zhu_score[i];
		}
		// 刷新玩家下注信息
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			_is_half_join[i] = false;
			_player_result.game_score[i] -= _di_zhu_score[i];
		}
		operate_player_data();

		if (this.has_rule(GameConstants.GAME_RULE_MIN_JETTON)) {
			if (10 >= _jetton_total_score) {
				_user_jetton_min = _jetton_total_score;
			} else {
				_user_jetton_min = 10;
			}

		} else {
			_user_jetton_min = 1;
		}

		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			if (this.get_players()[index] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_XYKL_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			GameStart_Xykl.Builder gamestart_xykl = GameStart_Xykl.newBuilder();
			RoomInfoXykl.Builder room_info = getRoomInfoXykl();
			gamestart_xykl.setRoomInfo(room_info);
			gamestart_xykl.setJettonTotalScore(_jetton_total_score);
			this.load_player_info_data_game_start(gamestart_xykl);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == index) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else {
					if (this.get_players()[i] == null) {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.INVALID_CARD);
						}
					} else {
						for (int j = 0; j < GRR._card_count[i]; j++) {
							cards_card.addItem(GameConstants.BLACK_CARD);
						}
					}

				}
				gamestart_xykl.addCardsData(cards_card);
				gamestart_xykl.addCardCount(GRR._card_count[i]);
			}
			gamestart_xykl.setIsJetton(is_jetton);
			gamestart_xykl.setDisplayTime(_scheduled_call_banker_time);
			roomResponse.setCommResponse(PBUtil.toByteString(gamestart_xykl));
			this.send_response_to_player(index, roomResponse);
		}

		this.set_timer(ID_TIMER_ANIMATION_CALL_BANKER, _scheduled_call_banker_time + 3);

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		GameStart_Xykl.Builder gamestart_xykl = GameStart_Xykl.newBuilder();
		RoomInfoXykl.Builder room_info = getRoomInfoXykl();
		gamestart_xykl.setRoomInfo(room_info);
		this.load_player_info_data_game_start(gamestart_xykl);
		gamestart_xykl.setJettonTotalScore(_jetton_total_score);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (this.get_players()[i] == null) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.BLACK_CARD);
				}
			}

			gamestart_xykl.addCardsData(cards_card);
			gamestart_xykl.addCardCount(GRR._card_count[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_xykl));
		// 旁观
		observers().sendAll(roomResponse);
		// 回放
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (this.get_players()[i] == null) {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GRR._cards_data[i][j]);
				}
			}

			gamestart_xykl.setCardsData(i, cards_card);
		}
		gamestart_xykl.setDisplayTime(_scheduled_call_banker_time);
		roomResponse.setCommResponse(PBUtil.toByteString(gamestart_xykl));
		GRR.add_room_response(roomResponse);

		return true;
	}

	public int get_hand_card_count_max() {
		return GameConstants.XYKL_MAX_COUNT;
	}

	public int get_hand_card_count_inital() {
		return 5;
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

		GRR._left_card_count = GameConstants.CARD_COUNT_XYKL;

		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			if (this.get_players()[i] == null) {
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = GameConstants.INVALID_CARD;
				}
				GRR._card_count[i] = 0;
			} else {
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					GRR._cards_data[i][j] = repertory_card[i * (GameConstants.XYKL_MAX_COUNT - 1) + j];
				}
				GRR._card_count[i] = get_hand_card_count_max() - 1;
			}

			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
			GRR._left_card_count -= get_hand_card_count_max() - 1;
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {

		int cards[] = new int[] { 0x0D, 0x01, 0x1, 0x01, 0x1, 0x01 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 2; j++) {
				if (this._player_ready[i] == 1) {
					GRR._cards_data[i][j] = cards[index++];
				}
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false) {
					if (debug_my_cards.length > 20) {
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
				} else {
					if (debug_my_cards.length > 15) {
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

	public void load_player_info_data_game_start(GameStart_Xykl.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseXykl.Builder room_player = RoomPlayerResponseXykl.newBuilder();
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

	public void load_player_info_data_game_end(PukeGameEndXykl.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseXykl.Builder room_player = RoomPlayerResponseXykl.newBuilder();
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

	public void load_player_info_data_reconnect(TableResponse_Xykl.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseXykl.Builder room_player = RoomPlayerResponseXykl.newBuilder();
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

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
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
		switch (timer_id) {
		case ID_TIMER_ANIMATION_START: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null && this._player_ready[i] == 0 && !_is_half_join[i]) {
					handler_player_ready(i, false);
				}
				if (this.get_players()[i] != null && _is_half_join[i]) {
					opreate_enter_room_confirm(get_players()[i], 0);
				}
			}
			return true;
		}
		case ID_TIMER_ANIMATION_CALL_BANKER: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null && this._player_ready[i] == 1 && _is_qiang_mai[i] == 0) {
					opreate_call_banker(i, -1);
				}
			}
			return true;
		}
		case ID_TIMER_ANIMATION_JETTON: {
			opreate_jetton(this._current_player, _user_jetton_min);
			return true;
		}
		case ID_TIMER_ANIMATION_OPEN_CARD: {
			opreate_open_card(this._current_player);
			return true;
		}
		case ID_TIMER_ANIMATION_CUO_PAI: {
			opreate_open_card(this._current_player);
			return true;
		}
		}

		return false;
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		int send_count;
		int have_send_count = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			GRR._left_card_count -= send_count;

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			if (this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false)
				send_count = (GameConstants.MAX_HH_COUNT - 1);
			else
				send_count = GameConstants.MAX_FPHZ_COUNT - 1;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
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

		ret = this.handler_game_finish_xykl(seat_index, reason);

		return ret;
	}

	public boolean handler_game_finish_xykl(int seat_index, int reason) {
		int real_reason = reason;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndXykl.Builder game_end_xykl = PukeGameEndXykl.newBuilder();
		RoomInfoXykl.Builder room_info = getRoomInfoXykl();
		game_end_xykl.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {
		}
		this.load_player_info_data_game_end(game_end_xykl);
		game_end_xykl.setGameRound(_game_round);
		game_end_xykl.setCurRound(_cur_round);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			game_end_xykl.addEndScore(this._end_score[i]);
		}
		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (this.get_players()[i] == null) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}
				game_end_xykl.addCardsData(cards_card);
				game_end_xykl.addCardCount(GRR._card_count[i]);
				game_end_xykl.addUserIsCallBanker(this._is_qiang_mai[i]);
				game_end_xykl.addUserJettonScore(this._user_jetton_score[i]);
				game_end_xykl.addGameCell(_di_zhu_score[i]);
			}
		} else {
			game_end_xykl.setEndTime(System.currentTimeMillis() / 1000);
			game_end_xykl.setStartTime(System.currentTimeMillis() / 1000);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				int di_chi_score[] = new int[this.getTablePlayerNumber()];
				if (this._jetton_total_score != 0) {
					int lose_max = 0;
					int count = 0;

					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this.get_players()[i] != null) {
							count++;
						}
						di_chi_score[i] = 0;
					}
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this.get_players()[i] != null) {
							_player_result.game_score[i] += _jetton_total_score / count;
							di_chi_score[i] += _jetton_total_score / count;
							if (lose_max > _player_result.game_score[i]) {
								lose_max = (int) _player_result.game_score[i];
							}
						}
					}
					if (_jetton_total_score % count != 0) {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (this.get_players()[i] != null && _player_result.game_score[i] == lose_max) {
								_player_result.game_score[i] += _jetton_total_score % count;
								di_chi_score[i] += _jetton_total_score % count;
								break;
							}
						}
					}
				}
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_xykl.addAllEndScore((int) _player_result.game_score[i]);
					game_end_xykl.addDiChiScore(di_chi_score[i]);
				}
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end.setPlayerResult(this.process_player_result(reason));
			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			int di_chi_score[] = new int[this.getTablePlayerNumber()];
			if (this._jetton_total_score != 0) {
				int lose_max = 0;
				int count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] != null) {
						count++;
					}
					di_chi_score[i] = 0;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] != null) {
						_player_result.game_score[i] += _jetton_total_score / count;
						di_chi_score[i] += _jetton_total_score / count;
						if (lose_max > _player_result.game_score[i]) {
							lose_max = (int) _player_result.game_score[i];
						}
					}
				}
				if (_jetton_total_score % count != 0) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (this.get_players()[i] != null && _player_result.game_score[i] == lose_max) {
							_player_result.game_score[i] += _jetton_total_score % count;
							di_chi_score[i] += _jetton_total_score % count;
							break;
						}
					}
				}
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_xykl.addAllEndScore((int) _player_result.game_score[i]);
				game_end_xykl.addDiChiScore(di_chi_score[i]);
			}
			end = true;
		}
		game_end_xykl.setStartTime(_start_time);
		game_end_xykl.setEndTime(System.currentTimeMillis() / 1000);
		game_end_xykl.setTotalJettonScore(this._jetton_total_score);
		game_end_xykl.setReason(real_reason);
		game_end_xykl.setDisplayTime(_scheduled_start_time);
		////////////////////////////////////////////////////////////////////// 得分总的
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addGameScore(_end_score[i]);
		}
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_xykl));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		_jetton_total_temp_score = _jetton_total_score;
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
		if (!end) {
			int no_half_join_count = 0;
			int score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_is_half_join[i] == false && this.get_players()[i] != null) {
					no_half_join_count++;
				}
			}
			if (no_half_join_count != 0) {
				score = _jetton_total_temp_score / no_half_join_count;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_player_ready[i] == 1 || this.get_players()[i] == null) {
					continue;
				}
				EnterRoomConfirm.Builder enter_room_confirm = EnterRoomConfirm.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_XYKL_ENTER_ROOM);
				enter_room_confirm.setDesc("现在坐下需要先投注" + score + "分");
				enter_room_confirm.setDisplayTime(_scheduled_start_time);
				roomResponse.setCommResponse(PBUtil.toByteString(enter_room_confirm));
				this.send_response_to_player(i, roomResponse);
			}
		}
		// 重制准备
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		GRR = null;

		this.set_timer(ID_TIMER_ANIMATION_START, _scheduled_start_time);
		// 错误断言
		return false;
	}

	/**
	 * @return
	 */
	public RoomInfoXykl.Builder getRoomInfoXykl() {
		RoomInfoXykl.Builder room_info = RoomInfoXykl.newBuilder();
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
		if (commonGameRuleProtos != null) {
			room_info.setNewRules(commonGameRuleProtos);
		}

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
		if ((GameConstants.GS_MJ_FREE != _game_status)) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XYKL_RECONNECT_DATA);

			TableResponse_Xykl.Builder tableResponse_xykl = TableResponse_Xykl.newBuilder();
			load_player_info_data_reconnect(tableResponse_xykl);
			RoomInfoXykl.Builder room_info = getRoomInfoXykl();
			tableResponse_xykl.setRoomInfo(room_info);
			if (GRR == null) {
				tableResponse_xykl.setBankerPlayer(GameConstants.INVALID_SEAT);
			} else {
				tableResponse_xykl.setBankerPlayer(GRR._banker_player);
			}

			tableResponse_xykl.setCurrentPlayer(_current_player);
			tableResponse_xykl.setPrevPlayer(_prev_palyer);
			tableResponse_xykl.setTotalJettonScore(this._jetton_total_score);
			if (GRR != null) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if (this._player_ready[i] == 1) {
						tableResponse_xykl.addCardCount(this.GRR._card_count[i]);
						if (_is_qiang_mai[i] != -1) {
							if (this._is_open_card[i]) {
								cards.addItem(this.GRR._cards_data[i][0]);
								cards.addItem(this.GRR._cards_data[i][2]);
								cards.addItem(this.GRR._cards_data[i][1]);
							} else if (_is_qiang_mai[i] >= 0) {
								for (int j = 0; j < this.GRR._card_count[i]; j++) {
									cards.addItem(GameConstants.BLACK_CARD);
								}
							}
						}
						if (_is_qiang_mai[i] == 0) {
							tableResponse_xykl.addUserStatus(0);
						} else if (_is_qiang_mai[i] == -1 && _game_status == GameConstants.GS_CALL_BANKER) {
							tableResponse_xykl.addUserStatus(-1);
						} else if (_is_qiang_mai[i] == -1) {
							tableResponse_xykl.addUserStatus(4);
						} else if (_user_jetton_score[i] == 0) {
							tableResponse_xykl.addUserStatus(1);
						} else if (_is_open_card[i]) {
							tableResponse_xykl.addUserStatus(4);
						} else if (_is_cuo_pai[i] == 0) {
							tableResponse_xykl.addUserStatus(2);
						} else {
							tableResponse_xykl.addUserStatus(3);
						}
					} else {
						tableResponse_xykl.addCardCount(0);
						tableResponse_xykl.addUserStatus(4);
					}

					tableResponse_xykl.setIsCanCuoPai(0);

					tableResponse_xykl.addCardsData(cards);
					tableResponse_xykl.addIsCallBanekr(_is_qiang_mai[i]);
					tableResponse_xykl.addIsCuoPai(_is_cuo_pai[i]);
					tableResponse_xykl.addUserWinLoseScore(this._end_score[i]);

					if (_is_open_card[i]) {
						tableResponse_xykl.addIsOpenCard(1);
					} else {
						tableResponse_xykl.addIsOpenCard(0);
					}
					tableResponse_xykl.addUserJettonScore(_user_jetton_score[i]);
				}
			}

			int dsplay_time = 0;
			if (_current_player == GameConstants.INVALID_SEAT) {
				dsplay_time = _scheduled_call_banker_time;
			} else {
				if (_user_jetton_score[_current_player] == 0) {
					dsplay_time = _scheduled_jetton_time;
				} else if (_is_cuo_pai[_current_player] == 1) {
					dsplay_time = _scheduled_cuo_pai_time;
				} else {
					dsplay_time = _scheduled_open_card_time;
				}
			}
			tableResponse_xykl.setDisplayTime(dsplay_time);
			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_xykl));
			observers().send(player, roomResponse);
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
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				} else
					handler_game_start();
			} else {
				if (has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					this.is_game_start = 0;
					control_game_start();
				}

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
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				}
			} else {
				if (has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					control_game_start();
				}

			}

		}

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)) {
			return true;
		}
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
			if (player != null && player.get_seat_index() > GameConstants.INVALID_SEAT)
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
		_player_ready[seat_index] = 1;
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
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

		operate_player_data();
		int cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				cur_count++;
			} else {
				continue;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}
		if (cur_count < 2) {
			return false;
		}
		int _cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
		}

		if ((this.getPlayerCount() >= 2) && (getPlayerCount() == _cur_count)) {
			if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
				this.is_game_start = 1;
				control_game_start();
			} else
				handler_game_start();
		} else {
			this.is_game_start = 0;
			control_game_start();
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

		if (GameConstants.GS_MJ_FREE != _game_status && has_rule(GameConstants.GAME_RULE_START_FORBID_XYKL)) {
			send_error_notify(player, 1, "游戏中禁止加入");
			return false;
		}
		if (this._cur_round == this._game_round) {
			send_error_notify(player, 1, "游戏已到最后一局禁止加入");
			return false;
		}
		int seat_index = GameConstants.INVALID_SEAT;
		/**
		 * 1) 勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，但只有房主能坐下（有空位时），此时普通用户进入时界面上只有“观战”按键。
		 * 2)不勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，且均能坐下（有空位时）。
		 * 3)游戏开始前进入桌子观战的用户，不管是否勾选此选项，只要有空位可随时坐下
		 */

		// if (has_rule(GameConstants.GAME_RULE_AA_PAY)) {
		// if (kou_dou_aa(player, seat_index, false) == false)
		// return false;
		// }
		if (playerNumber == 0) {// 未开始 才分配位置
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[i] == null) {
					get_players()[i] = player;
					seat_index = i;
					break;
				}
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT
				&& player.get_seat_index() < getTablePlayerNumber()) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer != null && tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			send_error_notify(player, 1, "游戏人数已满");
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
		if (has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
			if (this.is_game_start == 1)
				this.is_game_start = 0;
			control_game_start();
		}

		if (GameConstants.GS_MJ_WAIT == _game_status) {
			int no_half_join_count = 0;
			int score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_is_half_join[i] == false && this.get_players()[i] != null) {
					no_half_join_count++;
				}
			}
			if (no_half_join_count != 0) {
				score = _jetton_total_temp_score / no_half_join_count;
			}
			EnterRoomConfirm.Builder enter_room_confirm = EnterRoomConfirm.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XYKL_ENTER_ROOM);
			enter_room_confirm.setDesc("现在坐下需要先投注" + score + "分");
			enter_room_confirm.setDisplayTime(_scheduled_start_time);
			roomResponse.setCommResponse(PBUtil.toByteString(enter_room_confirm));
			this.send_response_to_player(seat_index, roomResponse);
		}
		if (_cur_game_timer == ID_TIMER_ANIMATION_START) {
			this.kill_timer();
			set_timer(ID_TIMER_ANIMATION_START, _scheduled_start_time);
		}
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {

		RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
		refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(refreshroomResponse);
		//
		send_response_to_other(seat_index, refreshroomResponse);
		if ((GameConstants.GS_MJ_FREE != _game_status) && this.get_players()[seat_index] != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XYKL_RECONNECT_DATA);

			TableResponse_Xykl.Builder tableResponse_xykl = TableResponse_Xykl.newBuilder();
			load_player_info_data_reconnect(tableResponse_xykl);
			RoomInfoXykl.Builder room_info = getRoomInfoXykl();
			tableResponse_xykl.setRoomInfo(room_info);
			if (GRR == null) {
				tableResponse_xykl.setBankerPlayer(GameConstants.INVALID_SEAT);
			} else {
				tableResponse_xykl.setBankerPlayer(GRR._banker_player);
			}

			tableResponse_xykl.setCurrentPlayer(_current_player);
			tableResponse_xykl.setPrevPlayer(_prev_palyer);
			tableResponse_xykl.setTotalJettonScore(this._jetton_total_score);
			if (GRR != null) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if (this._player_ready[i] == 1) {
						tableResponse_xykl.addCardCount(this.GRR._card_count[i]);
						if (i == seat_index && _is_qiang_mai[i] >= 0) {
							if (_user_jetton_score[i] > 0) {
								cards.addItem(this.GRR._cards_data[i][0]);
								cards.addItem(this.GRR._cards_data[i][2]);
								cards.addItem(this.GRR._cards_data[i][1]);
							} else {
								cards.addItem(this.GRR._cards_data[i][0]);
								cards.addItem(this.GRR._cards_data[i][1]);
							}

						} else {
							if (_is_qiang_mai[i] != -1) {
								if (this._is_open_card[i]) {
									cards.addItem(this.GRR._cards_data[i][0]);
									cards.addItem(this.GRR._cards_data[i][2]);
									cards.addItem(this.GRR._cards_data[i][1]);
								} else if (_is_qiang_mai[i] >= 0) {
									for (int j = 0; j < this.GRR._card_count[i]; j++) {
										cards.addItem(GameConstants.BLACK_CARD);
									}
								}
							}

						}
						if (_is_qiang_mai[i] == 0) {
							tableResponse_xykl.addUserStatus(0);
						} else if (_is_qiang_mai[i] == -1 && _game_status == GameConstants.GS_CALL_BANKER) {
							tableResponse_xykl.addUserStatus(-1);
						} else if (_is_qiang_mai[i] == -1) {
							tableResponse_xykl.addUserStatus(4);
						} else if (_user_jetton_score[i] == 0) {
							tableResponse_xykl.addUserStatus(1);
						} else if (_is_open_card[i]) {
							tableResponse_xykl.addUserStatus(4);
						} else if (_is_cuo_pai[i] == 0) {
							tableResponse_xykl.addUserStatus(2);
						} else {
							tableResponse_xykl.addUserStatus(3);
						}
					} else {
						tableResponse_xykl.addCardCount(0);
						tableResponse_xykl.addUserStatus(4);
					}

					if (!has_rule(GameConstants.GAME_RULE_FORBID_CHOU_PAI_XYKL)) {
						if (seat_index == _current_player && _user_jetton_score[seat_index] > 0 && _is_open_card[seat_index] == false
								&& _is_qiang_mai[seat_index] == 1) {
							tableResponse_xykl.setIsCanCuoPai(1);
						} else {
							tableResponse_xykl.setIsCanCuoPai(0);
						}
					} else {
						tableResponse_xykl.setIsCanCuoPai(0);
					}

					tableResponse_xykl.addCardsData(cards);
					tableResponse_xykl.addIsCallBanekr(_is_qiang_mai[i]);
					tableResponse_xykl.addIsCuoPai(_is_cuo_pai[i]);
					tableResponse_xykl.addUserWinLoseScore(this._end_score[i]);

					if (_is_open_card[i]) {
						tableResponse_xykl.addIsOpenCard(1);
					} else {
						tableResponse_xykl.addIsOpenCard(0);
					}
					tableResponse_xykl.addUserJettonScore(_user_jetton_score[i]);
				}
			}

			int dsplay_time = 0;
			if (_current_player == GameConstants.INVALID_SEAT) {
				dsplay_time = _scheduled_call_banker_time;
			} else {
				if (_user_jetton_score[_current_player] == 0) {
					dsplay_time = _scheduled_jetton_time;
				} else if (_is_cuo_pai[_current_player] == 1) {
					dsplay_time = _scheduled_cuo_pai_time;
				} else {
					dsplay_time = _scheduled_open_card_time;
				}
			}
			tableResponse_xykl.setDisplayTime(dsplay_time);
			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_xykl));
			send_response_to_player(seat_index, roomResponse);

			if (!(seat_index != _current_player || this._user_jetton_score[seat_index] != 0 || this.GRR._card_count[seat_index] == 3
					|| this._is_open_card[seat_index])) {
				//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
				JettonArea.Builder jetton_area = JettonArea.newBuilder();
				int jetton_scoreMax = _jetton_total_score;
				if (_user_jetton_max < jetton_scoreMax) {
					jetton_scoreMax = _user_jetton_max;
				}
				jetton_area.setJettonScoreMax(jetton_scoreMax);
				jetton_area.setJettonScoreMin(_user_jetton_min);
				jetton_area.setJettonSeatIndex(_current_player);
				jetton_area.setDisplayTime(_scheduled_jetton_time);
				roomResponse.setType(MsgConstants.RESPONSE_XYKL_JETTON_AREA);// 201

				roomResponse.setCommResponse(PBUtil.toByteString(jetton_area));
				this.send_response_to_player(seat_index, roomResponse);
			}

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
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				} else {
					handler_game_start();
				}

			} else {
				if (has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					this.is_game_start = 0;
					control_game_start();
				}
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
		if (type == MsgConstants.REQUST_XYKL_OPERATE) {

			Opreate_Request_Xykl req = PBUtil.toObject(room_rq, Opreate_Request_Xykl.class);
			// 逻辑处理
			return handler_requst_opreate(player, seat_index, req.getOpreateType(), req.getAddJettonScore(), req.getIsQiangMai(), req.getIsJoin());
		}
		return true;
	}

	public boolean handler_ox_game_start(int room_id, long account_id) {
		if (is_game_start == 2)
			return true;
		int cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				cur_count++;
			} else {
				continue;
			}

			if (_player_ready[i] == 0) {
				return false;
			}
		}
		if (cur_count < 2) {
			return false;
		}

		is_game_start = 2;
		control_game_start();

		handler_game_start();
		boolean nt = true;
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return true;
	}

	public boolean handler_requst_opreate(Player player, int seat_index, int opreate_type, int jetton_score, int is_qiang_mai, int is_join) {
		switch (opreate_type) {
		case 1: {
			opreate_call_banker(seat_index, is_qiang_mai);
			break;
		}
		case 2: {
			opreate_jetton(seat_index, jetton_score);
			break;
		}
		case 3: {
			opreate_open_card(seat_index);
			break;
		}
		case 4: {
			opreate_cuo_pai(seat_index);
			break;
		}
		case 5: {

			opreate_enter_room_confirm(player, is_join);

			break;
		}
		}
		return true;
	}

	public void opreate_call_banker(int seat_index, int is_qiang_mai) {
		// 叫庄操作
		if (_game_status != GameConstants.GS_CALL_BANKER) {
			return;
		}

		if (_is_qiang_mai[seat_index] != 0) {
			return;
		}
		if (this._player_ready[seat_index] == 0) {
			return;
		}
		_playerStatus[seat_index]._is_pao_qiang = true;
		_is_qiang_mai[seat_index] = is_qiang_mai;

		boolean is_callbanker_finish = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null || this._player_ready[i] == 0) {
				continue;
			}
			if (_is_qiang_mai[i] == 0) {
				is_callbanker_finish = false;
				break;
			}
		}
		if (is_callbanker_finish) {
			int count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				if (_is_qiang_mai[i] == 1) {
					count++;
				}
			}
			if (count > 0) {
				int targetindex = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % count);
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null || _player_ready[i] == 0) {
						continue;
					}
					if (targetindex == 0) {
						if (_is_qiang_mai[i] == 1) {
							_banker_select = i;
							break;
						}
					} else {
						if (_is_qiang_mai[i] == 1) {
							targetindex--;
						}
					}
				}
				if (DEBUG_CARDS_MODE) {
					_banker_select = 0;
				}
				this._current_player = _banker_select;
				_game_status = GameConstants.GS_MJ_PLAY;
			} else {
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), 2,
						TimeUnit.SECONDS);
			}

		} else {
			_banker_select = GameConstants.INVALID_SEAT;
			this._current_player = GameConstants.INVALID_SEAT;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_CALLBANKER);
		roomResponse.setGameStatus(this._game_status);
		// 叫庄结果
		CallBankerResult.Builder callbankerresult_xykl = CallBankerResult.newBuilder();
		callbankerresult_xykl.setOpreateSeatIndex(seat_index);
		callbankerresult_xykl.setIsCallBanekr(_is_qiang_mai[seat_index]);
		callbankerresult_xykl.setBankerSeatIndex(_banker_select);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			callbankerresult_xykl.addSeatIndexSisCall(_is_qiang_mai[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(callbankerresult_xykl));
		this.send_response_to_room(roomResponse);
		this.GRR.add_room_response(roomResponse);

		jetton_score_area(this._current_player);
	}

	public void opreate_jetton(int seat_index, int jetton_score) {
		// 下注操作
		if (_game_status != GameConstants.GS_MJ_PLAY) {
			return;
		}
		if (_banker_select == GameConstants.INVALID_SEAT) {
			return;
		}
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (this._current_player != seat_index) {
			return;
		}
		if (_user_jetton_score[seat_index] != 0) {
			return;
		}
		if (jetton_score > _jetton_total_score || jetton_score > _user_jetton_max) {
			return;
		}
		if (_user_jetton_min > jetton_score) {
			return;
		}
		kill_timer();
		_user_jetton_score[seat_index] = jetton_score;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_JETTON);
		roomResponse.setGameStatus(this._game_status);
		// 下注结果
		JettonResult.Builder jettonresult_xykl = JettonResult.newBuilder();
		jettonresult_xykl.setOpreateSeatIndex(seat_index);
		jettonresult_xykl.setJettonScore(jetton_score);
		if (jetton_score == this._user_jetton_max || jetton_score == _jetton_total_score) {
			jettonresult_xykl.setIsAllIn(1);
		} else {
			jettonresult_xykl.setIsAllIn(0);
		}
		jettonresult_xykl.setTotalJettonScore(_jetton_total_score);
		roomResponse.setCommResponse(PBUtil.toByteString(jettonresult_xykl));
		this.send_response_to_room(roomResponse);
		this.GRR.add_room_response(roomResponse);

		exe_dispatch_card(_current_player, 1);
	}

	public void opreate_open_card(int seat_index) {
		if (_game_status != GameConstants.GS_MJ_PLAY) {
			return;
		}
		if (this._current_player != seat_index || _current_player == GameConstants.INVALID_SEAT) {
			return;
		}
		if (_user_jetton_score[seat_index] == 0) {
			return;
		}
		kill_timer();
		_is_open_card[seat_index] = true;
		_is_cuo_pai[seat_index] = 0;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_OPENCARD);
		roomResponse.setGameStatus(this._game_status);
		// 开牌结果
		OpenCardResult.Builder openresult_xykl = OpenCardResult.newBuilder();
		openresult_xykl.setOpreateSeatIndex(seat_index);
		openresult_xykl.setCardCount(this.GRR._card_count[seat_index]);
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		openresult_xykl.addCardsData(GRR._cards_data[seat_index][0]);
		openresult_xykl.addCardsData(GRR._cards_data[seat_index][2]);
		openresult_xykl.addCardsData(GRR._cards_data[seat_index][1]);
		int firstvalue = this._logic.get_card_value(GRR._cards_data[seat_index][0]);
		int twovalue = this._logic.get_card_value(GRR._cards_data[seat_index][1]);
		int threevalue = this._logic.get_card_value(GRR._cards_data[seat_index][2]);
		if (threevalue < firstvalue && threevalue > twovalue) {
			openresult_xykl.setWinOrLose(1);
			this._jetton_total_score -= _user_jetton_score[seat_index];
			this._player_result.game_score[seat_index] += _user_jetton_score[seat_index];
			_end_score[seat_index] += _user_jetton_score[seat_index];
			openresult_xykl.setScore(_user_jetton_score[seat_index]);
		} else {
			openresult_xykl.setWinOrLose(0);
			this._jetton_total_score += _user_jetton_score[seat_index];
			this._player_result.game_score[seat_index] -= _user_jetton_score[seat_index];
			_end_score[seat_index] -= _user_jetton_score[seat_index];
			openresult_xykl.setScore(-_user_jetton_score[seat_index]);
		}
		int next_player = (this._current_player + 1) % this.getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_is_qiang_mai[next_player] == 0 || _is_qiang_mai[next_player] == -1 || this._player_ready[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			} else {
				break;
			}
		}
		if (_is_open_card[next_player] || _jetton_total_score == 0) {
			this._current_player = GameConstants.INVALID_SEAT;
		} else {
			this._current_player = next_player;
		}
		openresult_xykl.setUserJettonScore(_user_jetton_score[seat_index]);
		openresult_xykl.setTotalJettonScore(_jetton_total_score);
		openresult_xykl.setBankerSeatIndex(this._current_player);
		openresult_xykl.setCurrentPlayer(this._current_player);
		openresult_xykl.setDisplayTime(_scheduled_open_card_time);

		roomResponse.setCommResponse(PBUtil.toByteString(openresult_xykl));
		this.send_response_to_room(roomResponse);
		this.GRR.add_room_response(roomResponse);

		if (has_rule(GameConstants.GAME_RULE_MIN_JETTON)) {
			if (10 > _jetton_total_score) {
				_user_jetton_min = _jetton_total_score;
			} else {
				_user_jetton_min = 10;
			}
		}

		this.operate_player_data();

		if (this._current_player != GameConstants.INVALID_SEAT) {
			this.jetton_score_area(this._current_player);
		} else {
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), 2, TimeUnit.SECONDS);
		}

	}

	public void opreate_cuo_pai(int seat_index) {
		if (has_rule(GameConstants.GAME_RULE_FORBID_CHOU_PAI_XYKL)) {
			return;
		}
		if (_is_cuo_pai[seat_index] == 1) {
			return;
		}
		this.kill_timer();
		this.set_timer(ID_TIMER_ANIMATION_CUO_PAI, _scheduled_cuo_pai_time);
		_is_cuo_pai[seat_index] = 1;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setTarget(seat_index);
		roomResponse.setLeftTime(_scheduled_cuo_pai_time);
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_CHOU_PAI);
		this.send_response_to_room(roomResponse);
	}

	public void opreate_enter_room_confirm(Player player, int is_join) {
		if (GameConstants.GS_MJ_WAIT != _game_status || _player_result.game_score[player.get_seat_index()] != 0) {
			return;
		} else {
			// 中途加入需要先下注
			if (GameConstants.GS_MJ_FREE != _game_status) {
				if (is_join == 1) {
					int no_half_join_count = 0;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_is_half_join[i] == false && this.get_players()[i] != null) {
							no_half_join_count++;
						}
					}
					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					_di_zhu_score[player.get_seat_index()] = _jetton_total_temp_score / no_half_join_count;
					_jetton_total_score += _di_zhu_score[player.get_seat_index()];
					// 下注结果
					roomResponse.setType(MsgConstants.RESPONSE_XYKL_JETTON);
					JettonResult.Builder jettonresult_xykl = JettonResult.newBuilder();
					jettonresult_xykl.setOpreateSeatIndex(GameConstants.INVALID_SEAT);
					jettonresult_xykl.setJettonScore(_di_zhu_score[player.get_seat_index()]);
					jettonresult_xykl.setIsAllIn(0);
					jettonresult_xykl.setTotalJettonScore(_jetton_total_score);
					roomResponse.setCommResponse(PBUtil.toByteString(jettonresult_xykl));
					this.send_response_to_room(roomResponse);
					if (this.GRR != null) {
						this.GRR.add_room_response(roomResponse);
					}

					// 刷新玩家下注信息
					_player_result.game_score[player.get_seat_index()] -= _di_zhu_score[player.get_seat_index()];
					operate_player_data();
					_is_half_join[player.get_seat_index()] = true;
					handler_player_ready(player.get_seat_index(), false);
				} else {
					RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
					quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
					send_response_to_player(player.get_seat_index(), quit_roomResponse);

					send_error_notify(player.get_seat_index(), 2, "您已退出该游戏");
					this.get_players()[player.get_seat_index()] = null;
					_player_ready[player.get_seat_index()] = 0;

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
					send_response_to_other(player.get_seat_index(), refreshroomResponse);
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
						if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
							this.is_game_start = 1;
							control_game_start();
						} else
							handler_game_start();
					} else {
						this.is_game_start = 0;
						control_game_start();
					}
				}

			}
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
	public void refresh_player_cards() {
		for (int palyer_index = 0; palyer_index < this.getTablePlayerNumber(); palyer_index++) {
			//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RefreshCardsXykl.Builder refresh_cards = RefreshCardsXykl.newBuilder();

			roomResponse.setType(MsgConstants.RESPONSE_XYKL_REFRESH_PLAYER_CARDS);// 201
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._is_qiang_mai[i] == 1) {
					refresh_cards.addCardCount(this.GRR._card_count[i]);
					if (i == palyer_index || this._is_open_card[i]) {
						for (int j = 0; j < this.GRR._card_count[i]; j++) {
							cards.addItem(this.GRR._cards_data[i][j]);
						}
					}
				} else {
					refresh_cards.addCardCount(0);
				}
				refresh_cards.addCardsData(cards);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(refresh_cards));
			this.send_response_to_player(palyer_index, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RefreshCardsXykl.Builder refresh_cards = RefreshCardsXykl.newBuilder();

		roomResponse.setType(MsgConstants.RESPONSE_XYKL_REFRESH_PLAYER_CARDS);// 201
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			refresh_cards.addCardCount(this.GRR._card_count[i]);
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				cards.addItem(this.GRR._cards_data[i][j]);
			}
			refresh_cards.addCardsData(cards);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_cards));
		GRR.add_room_response(roomResponse);

	}

	public void jetton_score_area(int seat_index) {
		if (_game_status != GameConstants.GS_MJ_PLAY) {
			return;
		}
		if (seat_index != _current_player) {
			return;
		}
		if (this._user_jetton_score[seat_index] != 0 || this.GRR._card_count[seat_index] == 3 || this._is_open_card[seat_index]) {
			return;
		}

		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		JettonArea.Builder jetton_area = JettonArea.newBuilder();
		int jetton_scoreMax = _jetton_total_score;
		if (_user_jetton_max < jetton_scoreMax) {
			jetton_scoreMax = _user_jetton_max;
		}
		jetton_area.setJettonScoreMax(jetton_scoreMax);
		jetton_area.setJettonScoreMin(_user_jetton_min);
		jetton_area.setJettonSeatIndex(_current_player);
		jetton_area.setDisplayTime(_scheduled_jetton_time);
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_JETTON_AREA);// 201

		roomResponse.setCommResponse(PBUtil.toByteString(jetton_area));
		this.send_response_to_room(roomResponse);
		kill_timer();
		this.set_timer(ID_TIMER_ANIMATION_JETTON, _scheduled_jetton_time);
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int seat_index, int send_count, boolean tail) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		if (GRR._card_count[seat_index] >= 3) {
			return false;
		}
		int cards_data[] = new int[send_count];
		boolean is_send_to_observer = false;
		for (int i = 0; i < send_count; i++) {
			cards_data[i] = _repertory_card[GRR._left_card_count];
			GRR._left_card_count--;

			GRR._cards_data[seat_index][GRR._card_count[seat_index]] = cards_data[i];
			GRR._card_count[seat_index]++;
		}
		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
			if (this.get_players()[play_index] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			SendCardsXykl.Builder send_cards_gdy = SendCardsXykl.newBuilder();
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_XYKL_SEND_CARD);// 201
			roomResponse.setTarget(seat_index);
			if (seat_index == play_index) {
				for (int i = 0; i < send_count; i++) {
					send_cards_gdy.addCardsData(cards_data[i]);
				}
				for (int i = 0; i < this.GRR._card_count[seat_index]; i++) {
					send_cards_gdy.addHandCardsData(GRR._cards_data[play_index][i]);
				}
			} else {
				for (int i = 0; i < send_count; i++) {
					send_cards_gdy.addCardsData(GameConstants.BLACK_CARD);
				}
				for (int i = 0; i < this.GRR._card_count[seat_index]; i++) {
					send_cards_gdy.addHandCardsData(GameConstants.BLACK_CARD);
				}
			}
			send_cards_gdy.setHandCardCount(GRR._card_count[seat_index]);
			send_cards_gdy.setCardCount(send_count);
			send_cards_gdy.setSendCardPlayer(seat_index);
			send_cards_gdy.setDisplayTime(_scheduled_open_card_time);

			if (!has_rule(GameConstants.GAME_RULE_FORBID_CHOU_PAI_XYKL)) {
				send_cards_gdy.setIsCanCuoPai(1);
			} else {
				send_cards_gdy.setIsCanCuoPai(0);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(send_cards_gdy));
			this.send_response_to_player(play_index, roomResponse);
			if (seat_index != play_index && !is_send_to_observer) {
				this.observers().sendAll(roomResponse);
				is_send_to_observer = true;
			}
		}
		this.kill_timer();
		this.set_timer(ID_TIMER_ANIMATION_OPEN_CARD, _scheduled_open_card_time);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		SendCardsXykl.Builder send_cards_gdy = SendCardsXykl.newBuilder();
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XYKL_SEND_CARD);// 201
		roomResponse.setTarget(seat_index);
		for (int i = 0; i < send_count; i++) {
			send_cards_gdy.addCardsData(cards_data[i]);
		}
		send_cards_gdy.setCardCount(send_count);
		send_cards_gdy.setSendCardPlayer(seat_index);

		roomResponse.setCommResponse(PBUtil.toByteString(send_cards_gdy));
		GRR.add_room_response(roomResponse);

		return true;
	}

	// 发送扑克
	public boolean exe_dispatch_card(int seat_index, int send_count) {

		GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, send_count, false), 200, TimeUnit.MILLISECONDS);

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
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;

			}
			kill_timer();
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

			this.set_timer(_cur_game_timer, _cur_operate_time);
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
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				} else
					handler_game_start();
			} else if (has_rule(GameConstants.GAME_RULE_XYKL_CONTORL_START)) {
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
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

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

		return getRuleValue(GameConstants.GAME_RULE_XYKL_PLAYER);
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
