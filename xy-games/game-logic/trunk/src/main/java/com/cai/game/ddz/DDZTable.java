/**
 * 
 */
package com.cai.game.ddz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
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
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.ddz.handler.DDZHandler;
import com.cai.game.ddz.handler.DDZHandlerAddtimes;
import com.cai.game.ddz.handler.DDZHandlerCallBanker;
import com.cai.game.ddz.handler.DDZHandlerFinish;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;
import com.cai.game.ddz.handler.DDZHandlerPiao;
import com.cai.game.ddz.handler.jdddz.DDZHandlerOutCardOperate_JD;
import com.cai.game.ddz.handler.txw.DDZHandlerAddtimesAgain_TXW;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.ddz.DdzRsp.AddTimesDDZ;
import protobuf.clazz.ddz.DdzRsp.Add_Times_Request;
import protobuf.clazz.ddz.DdzRsp.CallBankerResult;
import protobuf.clazz.ddz.DdzRsp.Call_Banker_Request;
import protobuf.clazz.ddz.DdzRsp.GameStartDDZ;
import protobuf.clazz.ddz.DdzRsp.OutCardData;
import protobuf.clazz.ddz.DdzRsp.OutCardData_Request;
import protobuf.clazz.ddz.DdzRsp.PukeGameEndDdz;
import protobuf.clazz.ddz.DdzRsp.RefreshCards_DDZ;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;
import protobuf.clazz.ddz.DdzRsp.RoomPlayerResponseDdz;
import protobuf.clazz.ddz.DdzRsp.Send_LaiZi_DDZ;
import protobuf.clazz.ddz.DdzRsp.TableResponseDDZ;
import protobuf.clazz.lpsddz.lpsDdzRsp.GameStartDDZ_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.PukeGameEndDdz_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.TableResponseDDZ_lps;

///////////////////////////////////////////////////////////////////////////////////////////////
public class DDZTable extends AbstractRoom {
	/**
	 * 
	 */
	protected static final long serialVersionUID = 7060061356475703643L;

	protected static Logger logger = Logger.getLogger(DDZTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	public int _times; // 倍数
	public int _qiang_time; // 抢地主倍数
	public int _boom_count;// 炸弹数目
	public int _game_score[];
	public int _init_hand_card[][];
	public int _init_hand_card_count[];
	public int _boom_count_limit;

	public ScheduledFuture _trustee_schedule[];// 托管定时器

	public DDZGameLogic _logic = null;

	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _prev_palyer = GameConstants.INVALID_SEAT; // 上一用户
	public int _banker_select = GameConstants.INVALID_SEAT;// 地主用户
	public int _win_num[];// 赢的局数
	public int _lose_num[];// 输的局数
	public int _out_card_times[];// 出牌次数
	public int _banker_num[];// 当地主次数
	public int _boom_num[];// 炸弹次数
	public int _turn_out_card_count; // 上一次出牌数目
	public int _turn_out_card_data[]; // 上一次 出牌扑克
	public int _turn_out_real_data[]; // 上一次 出牌扑克
	public int _turn_out_card_type;
	public int _turn_out__player;

	public int _qiang_banker[];
	public int _call_banker[];
	public int _qiang_action[];
	public int _call_action[];
	public int _call_banker_type;
	public int _add_times[];
	public int _call_banker_status;// 叫庄状态 0：不叫庄状态 1：叫地主状态 2:抢地主状态
	public int _call_banker_score;

	public int _di_pai_card_data[]; // 底牌扑克
	public int _di_pai_card_count; // 底牌数目
	public int _di_pai_type; // 底牌类型
	public int _di_pai_time; // 底牌倍数
	public int _rang_card_count[];
	public int _piao_fen[]; // 飘分
	public int _magic_card;
	public int _fan_pai_data;

	public Player_EX[] _player_info;

	public boolean _add_times_operate[];

	public int _cur_game_timer; // 当前游戏定时器
	public int _cur_operate_time; // 可操作时间
	public int _operate_start_time; // 操作开始时间
	protected long _request_release_time;
	protected ScheduledFuture _release_scheduled;
	protected ScheduledFuture _table_scheduled;
	public ScheduledFuture _auto_out_card_scheduled;
	public ScheduledFuture _trustee_auto_opreate_scheduled[];
	public ScheduledFuture _auto_call_banker_scheduled;
	public ScheduledFuture _auto_add_time_scheduled[];
	public ScheduledFuture _call_banker_finish_scheduled;
	protected ScheduledFuture _game_scheduled;

	public DDZHandler _handler;

	public DDZHandlerOutCardOperate _handler_out_card_operate;
	public DDZHandlerCallBanker _handler_call_banker;
	public DDZHandlerAddtimes _handler_add_times;
	public DDZHandlerAddtimesAgain_TXW _handler_add_times_again;
	public DDZHandlerPiao _handlerPiao;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public DDZHandlerFinish _handler_finish; // 结束

	public DDZTable() {
		super(RoomType.MJ, 5);
		_player_ready = new int[this.getTablePlayerNumber()];
		_game_score = new int[getTablePlayerNumber()];
		_out_card_times = new int[this.getTablePlayerNumber()];
		_win_num = new int[getTablePlayerNumber()];
		_lose_num = new int[getTablePlayerNumber()];
		_banker_num = new int[getTablePlayerNumber()];
		_boom_num = new int[getTablePlayerNumber()];
		_qiang_banker = new int[getTablePlayerNumber()];
		_call_banker = new int[getTablePlayerNumber()];
		_add_times = new int[getTablePlayerNumber()];
		_user_times = new int[getTablePlayerNumber()];
		_trustee_auto_opreate_scheduled = new ScheduledFuture[getTablePlayerNumber()];
		_auto_add_time_scheduled = new ScheduledFuture[getTablePlayerNumber()];
		_piao_fen = new int[getTablePlayerNumber()];
		_qiang_action = new int[2];
		_call_action = new int[4];
		_call_banker_type = 0;
		_call_banker_status = 0;
		_call_banker_score = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_game_score[i] = 0;
			_out_card_times[i] = 0;
			_win_num[i] = 0;
			_lose_num[i] = 0;
			_call_banker[i] = -1;
			_qiang_banker[i] = -1;
			_add_times[i] = -1;
			_banker_num[i] = 0;
			_boom_num[i] = 0;
		}
		Arrays.fill(_qiang_action, -1);
		Arrays.fill(_call_action, -1);
		Arrays.fill(_user_times, 1);

		// 比赛场重置倍数
		PlayerServiceImpl.getInstance().updateRoomInfo(getRoom_id());
	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		game_cell = 1;
		_times = 1;
		_boom_count = 0;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());
		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_JD)) {
			// 初始化基础牌局handler

			_handler_out_card_operate = new DDZHandlerOutCardOperate_JD();
			_handler_add_times = new DDZHandlerAddtimes();

		}

		_handler_finish = new DDZHandlerFinish();

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

		if (_cur_round == 0) {
			record_game_room();
		}

		this._handler = null;

		GRR = new GameRoundRecord(getTablePlayerNumber(), GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT,
				GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.DDZ_MAX_COUNT_JD);
			_game_score[i] = 0;
			_out_card_times[i] = 0;
		}

		_cur_round++;
		game_cell = 1;
		_times = 1;
		_boom_count = 0;

		// 牌局回放
		GRR._room_info = this.getRoomInfo();

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

		GRR._video_recode.setBankerPlayer(this._banker_select);

		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		//
		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_JD)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_DDZ_JD];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DDZ);
		}

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_JD)) {
			return game_start_ddzjd();
		}

		return false;
	}

	// 经典斗地主开始
	public boolean game_start_ddzjd() {

		return true;
	}

	public int get_hand_card_count_max() {
		return GameConstants.DDZ_MAX_COUNT_JD;
	}

	public int get_no_banker_card_count() {
		return GameConstants.DDZ_MAX_COUNT_JD - GameConstants.DDZ_DI_PAI_COUNT_JD;
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

		_logic.random_card_data(repertory_card, card_cards);

		int count = this.getPlayerCount();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < get_no_banker_card_count(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * get_no_banker_card_count() + j];
			}
			GRR._card_count[i] = get_no_banker_card_count();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
		}
		GRR._left_card_count = GameConstants.DDZ_DI_PAI_COUNT_JD;
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {

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
		_banker_select = 0;
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

	public void send_add_times(int cur_player, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_ADD_TIMES);
		AddTimesDDZ.Builder add_time_ddz = AddTimesDDZ.newBuilder();
		add_time_ddz.addAddtimesaction(0);
		add_time_ddz.addAddtimesaction(1);
		add_time_ddz.setDisplayTime(10);
		add_time_ddz.setCurrentPlayer(cur_player);
		roomResponse.setCommResponse(PBUtil.toByteString(add_time_ddz));
		if (to_player != GameConstants.INVALID_SEAT) {
			send_response_to_player(to_player, roomResponse);
		} else {
			this.send_response_to_room(roomResponse);
		}
		GRR.add_room_response(roomResponse);

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
		if (is_mj_type(GameConstants.GAME_TYPE_DDZ_JD)) {
			ret = this.handler_game_finish_ddzjd(seat_index, reason);
		}

		return ret;
	}

	public boolean handler_game_finish_ddzjd(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER_OX;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndDdz.Builder game_end_ddz = PukeGameEndDdz.newBuilder();
		this.load_player_info_data_game_end(game_end_ddz);
		RoomInfoDdz.Builder room_info = getRoomInfoDdz();
		game_end_ddz.setRoomInfo(room_info);
		game_end_ddz.setPlayerNum(count);
		game_end_ddz.setGameRound(_game_round);
		game_end_ddz.setCurRound(_cur_round);
		game_end_ddz.setBankerPlayer(GRR._banker_player);// 庄家
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
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
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN)
				real_reason = reason;
			else
				real_reason = GameConstants.Game_End_RELEASE_PLAY;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setRoomInfo(getRoomInfo());

		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_ddz));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setStartTime(GRR._start_time);
		game_end.setGameTypeIndex(GRR._game_type_index);
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER_OX; j++) {
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
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

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

	public void exe_dispath() {

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
		int count = 3;
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
			count = 4;
		else
			count = 3;
		for (int i = 0; i < count; i++) {
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
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			// if (this._handler != null)
			// this._handler.handler_player_be_in_room(this, seat_index);

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_DDZ_RECONNECT_DATA);

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
		if (this._cur_round > 0)
			return handler_player_ready(seat_index, false);
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
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, List<Integer> change_list,
			int card_count, int b_out_card, String desc) {
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

	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card,
			String desc) {
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

	public boolean handler_operate_add_times(int seat_index, int times) {
		if (this._game_status != GameConstants.GS_DDZ_ADD_TIMES
				&& this._game_status != GameConstants.GS_LPS3_ADD_TIMES) {
			return false;
		}
		if (this._handler_add_times != null) {
			this._handler_add_times.handler_call_banker(this, seat_index, times);
		}
		return true;
	}

	public boolean handler_operate_piao_fen(int seat_index, int piao_fen) {
		if (this._game_status != GameConstants.GS_MJ_PIAO) {
			return false;
		}
		if (this._handlerPiao != null) {
			this._handlerPiao.handler_Piao_fen(this, seat_index, piao_fen);
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
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_CALL_QIANG_ZHUANG) {
			Call_Banker_Request req = PBUtil.toObject(room_rq, Call_Banker_Request.class);
			// 逻辑处理
			return handler_requst_call_qiang_zhuang(seat_index, req.getSelectCallBanker(), req.getSelectQiangBanker());
		} else if (type == MsgConstants.REQUST_DDZ_OUT_CARD_MUL) {
			OutCardData_Request req = PBUtil.toObject(room_rq, OutCardData_Request.class);
			// 逻辑处理
			return handler_operate_out_card_mul(seat_index, req.getOutCardsList(), req.getChangeOutCardsList(),
					req.getOutCardCount(), req.getBOutCardType(), room_rq.getRoomPw());
		} else if (type == MsgConstants.REQUST_DDZ_ADD_TIMES) {
			Add_Times_Request req = PBUtil.toObject(room_rq, Add_Times_Request.class);
			// 逻辑处理
			return handler_operate_add_times(seat_index, req.getAddTimes());
		}

		return true;
	}

	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
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
		return true;
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

	public boolean exe_call_banker_finish(int seat_index, boolean bOut) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER_RESULT);
		CallBankerResult.Builder call_banker_ddz_result = CallBankerResult.newBuilder();

		call_banker_ddz_result.setBankerPlayer(seat_index);
		call_banker_ddz_result.setCallPlayer(seat_index);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			call_banker_ddz_result.addDifenBombDes(get_boom_difen_des(i));

		}
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz_result));
		this.send_response_to_room(roomResponse);

		// 确定出牌玩家
		RoomResponse.Builder roomResponse_outplayer = RoomResponse.newBuilder();
		OutCardData.Builder outcarddata = OutCardData.newBuilder();
		roomResponse_outplayer.setType(MsgConstants.RESPONSE_DDZ_OUT_CARD);// 201
		roomResponse_outplayer.setTarget(seat_index);
		outcarddata.setCurPlayer(seat_index);
		outcarddata.setIsFirstOut(2);
		roomResponse_outplayer.setCommResponse(PBUtil.toByteString(outcarddata));
		if (seat_index == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse_outplayer);
			return this.send_response_to_room(roomResponse_outplayer);
		} else {
			return this.send_response_to_player(seat_index, roomResponse_outplayer);
		}
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

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
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
			des += "炸弹上限：4炸;";
		} else if (this.has_rule(GameConstants.GAME_RULE_LIMIT_FIVE_BOMB)) {
			des += "炸弹上限：5炸;";
		} else if (this.has_rule(GameConstants.GAME_RULE_LIMIT_SIX_BOMB)) {
			des += "炸弹上限：6炸;";
		}
		des += "底分：" + game_cell + "分\t" + "倍数：" + _times + "倍;";
		return des;
	}

	public String get_boom_difen_des(int seat_index) {
		String des = "";
		if (has_rule(GameConstants.GAME_RULE_JIAO_DI_ZHU)) {
			des += "叫地主玩法;";
		} else if (has_rule(GameConstants.GAME_RULE_JIAO_FEN)) {
			des += "叫分玩法;";
		} else {
			des += "抢地主玩法;";
		}
		if (this.has_rule(GameConstants.GAME_RULE_LIMIT_FOUR_BOMB)) {
			des += "炸弹上限：4炸;";
		} else if (this.has_rule(GameConstants.GAME_RULE_LIMIT_FIVE_BOMB)) {
			des += "炸弹上限：5炸;";
		} else if (this.has_rule(GameConstants.GAME_RULE_LIMIT_SIX_BOMB)) {
			des += "炸弹上限：6炸;";
		}
		des += "底分：" + (int) game_cell + "分;";
		des += "倍数：" + this._user_times[seat_index] + "倍;";
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

	public void load_player_info_data_game_start(GameStartDDZ.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDdz.Builder room_player = RoomPlayerResponseDdz.newBuilder();
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

	public void load_player_info_data_game_start_lps(GameStartDDZ_lps.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			// RoomPlayerResponseDdz_lps.Builder room_player =
			// RoomPlayerResponseDdz_lps.newBuilder();
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_game_end(PukeGameEndDdz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDdz.Builder room_player = RoomPlayerResponseDdz.newBuilder();
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

	public void load_player_info_data_game_end_lps(PukeGameEndDdz_lps.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			// RoomPlayerResponseDdz_lps.Builder room_player =
			// RoomPlayerResponseDdz_lps.newBuilder();
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
			roomResponse.addPlayers(room_player);
		}
	}

	public void load_player_info_data_reconnect(TableResponseDDZ.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseDdz.Builder room_player = RoomPlayerResponseDdz.newBuilder();
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

	public void load_player_info_data_reconnect_lps(TableResponseDDZ_lps.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			// RoomPlayerResponseDdz_lps.Builder room_player =
			// RoomPlayerResponseDdz_lps.newBuilder();
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
			roomResponse.addPlayers(room_player);
		}
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
		return RoomComonUtil.getMaxNumber(getDescParams());
	}

	public void call_banker_start() {

	}

	public void call_banker(int seat_index, int type) {

	}

	public void out_card_time_finish(int seat_index) {

	}

	public void auto_out_card(int seat_index) {
		if (this.is_sys()) {

		} else {
			if (this._out_card_player == seat_index) {

			} else {
				int card[] = new int[1];
				operate_out_card(seat_index, 0, card, GameConstants.DDZ_CT_ERROR, GameConstants.INVALID_SEAT);
			}
		}
	}

	public void auto_add_time(int seat_index) {

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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean set_timer(int timer_type, int time) {

		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		if (time == 0) {
			return true;
		}
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time,
				TimeUnit.MILLISECONDS);
		_operate_start_time = (int) (System.currentTimeMillis());
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
		return true;
	}

	public void RefreshCard(int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_REFRESH_PLAYER_CARDS);
		// 发送数据
		RefreshCards_DDZ.Builder refresh_card = RefreshCards_DDZ.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if (to_player == i) {
				for (int j = 0; j < this.GRR._card_count[i]; j++) {
					if (_logic.switch_card_to_idnex(this.GRR._cards_data[i][j]) == this._logic.magic_card[0]
							&& this._magic_card != GameConstants.INVALID_CARD) {
						cards_card.addItem(this.GRR._cards_data[i][j] + 0x100);
					} else {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					}

				}
			}
			refresh_card.addHandCardsData(cards_card);
			refresh_card.addHandCardCount(GRR._card_count[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		// 自己才有牌数据
		this.send_response_to_player(to_player, roomResponse);

		refresh_card.clear();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this.GRR._card_count[i]; j++) {
				if (_logic.switch_card_to_idnex(this.GRR._cards_data[i][j]) == this._logic.magic_card[0]
						&& this._magic_card != GameConstants.INVALID_CARD) {
					cards_card.addItem(this.GRR._cards_data[i][j] + 0x100);
				} else {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				}
			}
			refresh_card.addHandCardsData(cards_card);
			refresh_card.addHandCardCount(GRR._card_count[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_card));
		GRR.add_room_response(roomResponse);
	}

	public void send_lai_zi(int is_animal, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_SEND_LAI_ZI);
		// 发送数据
		Send_LaiZi_DDZ.Builder send_data = Send_LaiZi_DDZ.newBuilder();
		send_data.setLaiZiData(_logic.switch_index_to_card(_logic.magic_card[0]));
		send_data.setIsAnimal(is_animal);
		roomResponse.setCommResponse(PBUtil.toByteString(send_data));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
		if (is_animal == 1) {
			this.GRR.add_room_response(roomResponse);
		}
	}
}
