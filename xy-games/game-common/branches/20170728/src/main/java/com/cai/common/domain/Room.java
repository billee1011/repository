package com.cai.common.domain;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cai.common.constant.GameConstants;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.SpringService;
import com.google.protobuf.ByteString;

import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;

/**
 * 房间
 * 
 * @author run
 *
 */
public abstract class Room implements Serializable {

	public Logger logger = LoggerFactory.getLogger(getClass());

	private static final long serialVersionUID = -1070175849016765655L;

	private boolean is_sys_room;// 是否是系统开的房间
	private int room_id;

	protected ScheduledFuture _kick_schedule;

	protected long _run_player_id;
	private int game_id;
	public int _game_rule_index; // 游戏规则
	public int _game_type_index;

	public int _game_round;
	public int base_score;// 底分
	public int max_times;// 最大倍数
	/**
	 * 房间里的人
	 */
	private final Player[] _players;// new RoomPlayer[4];

	/**
	 * 观察者
	 */
	private final GameObserverGroup observers = new GameObserverGroup(this);

	/**
	 * 创建时间
	 */
	private long create_time;

	/**
	 * 创建人
	 */
	private long room_owner_account_id;

	private String room_owner_name;

	private long _record_id;

	/**
	 * 最后刷新的时间(通信)
	 */
	private long last_flush_time;

	private int create_type;

	private Player create_player;

	public int _game_status;

	public int _end_reason;
	/**
	 * 游戏index --收费
	 */
	public int game_index;

	public int cost_dou = 0;// 扣豆

	public String groupID;

	public String groupName;

	public int isInner;

	protected int[] gameRuleIndexEx;

	public int getCreate_type() {
		return create_type;
	}

	public void setCreate_type(int create_type) {
		this.create_type = create_type;
	}

	private ReentrantLock roomLock = new ReentrantLock();

	/**
	 * 房间类型枚举
	 */
	private RoomType roomType;

	public enum RoomType {
		MJ, FLS, ZP, HH, OX, HJK
	}

	public Room(RoomType roomType, int maxNumber) {
		this.roomType = roomType;
		create_time = System.currentTimeMillis();
		last_flush_time = create_time;
		_end_reason = GameConstants.INVALID_VALUE;
		_players = new Player[maxNumber];
	}

	// =================================================================
	abstract public boolean process_release_room();

	abstract public boolean process_flush_time();

	/**
	 * 强制结算
	 * 
	 * @return
	 */
	public abstract boolean force_account();

	/**
	 * 最大数目 手牌
	 * 
	 * @return
	 */
	public int getMaxCount() {
		if (roomType == RoomType.MJ) {
			return GameConstants.MAX_COUNT;
		} else if (roomType == RoomType.FLS) {
			return GameConstants.MAX_FLS_COUNT;
		} else if (roomType == RoomType.ZP) {
			return GameConstants.MAX_ZP_COUNT;
		} else if (roomType == RoomType.HH) {
			return GameConstants.MAX_HH_COUNT;
		}
		return 0;
	}

	public long getCreate_time() {
		return create_time;
	}

	public void setCreate_time(long create_time) {
		this.create_time = create_time;
	}

	public long getRoom_owner_account_id() {
		return room_owner_account_id;
	}

	public void setRoom_owner_account_id(long room_owner_account_id) {
		this.room_owner_account_id = room_owner_account_id;
	}

	public int getRoom_id() {
		return room_id;
	}

	public int[] getGameRuleIndexEx() {
		return gameRuleIndexEx;
	}

	public void setGameRuleIndexEx(int[] gameRuleIndexEx) {
		this.gameRuleIndexEx = gameRuleIndexEx;
	}

	public void setRoom_id(int room_id) {
		this.room_id = room_id;
	}

	public Player[] get_players() {
		return _players;
	}

	public int getPlayerCount() {
		int count = 0;
		for (Player player : _players) {
			if (player == null)
				continue;
			count++;
		}
		return count;
	}

	public long get_record_id() {
		return _record_id;
	}

	public void set_record_id(long _record_id) {
		this._record_id = _record_id;
	}

	public String getRoom_owner_name() {
		return room_owner_name;
	}

	public void setRoom_owner_name(String room_owner_name) {
		this.room_owner_name = room_owner_name;
	}

	public long getLast_flush_time() {
		return last_flush_time;
	}

	public void setLast_flush_time(long last_flush_time) {
		this.last_flush_time = last_flush_time;
	}

	public ReentrantLock getRoomLock() {
		return roomLock;
	}

	public void setRoomLock(ReentrantLock roomLock) {
		this.roomLock = roomLock;
	}

	/**
	 * @return the observers
	 */
	public GameObserverGroup observers() {
		return observers;
	}

	/****************************** 以下方法需要在所有的table中改一下 ****************************************************************************/
	/**
	 * @param player
	 * @return
	 */
	public abstract boolean handler_reconnect_room(Player player);

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public abstract boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card);

	/**
	 * @param get_seat_index
	 * @param operateCode
	 * @param operateCard
	 * @return
	 */
	public abstract boolean handler_operate_card(int get_seat_index, int operateCode, int operateCard, int luoCode);

	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	public abstract boolean handler_operate_button(int seat_index, int operate_code);

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	public abstract boolean handler_call_banker(int seat_index, int call_banker);

	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	public abstract boolean handler_add_jetton(int seat_index, int jetton);

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	public abstract boolean handler_open_cards(int seat_index, boolean open_flag);

	/**
	 * @param account_id
	 * @return
	 */
	public abstract Player get_player(long account_id);

	/**
	 * @param get_seat_index
	 * @param operateCard
	 * @return
	 */
	public abstract boolean handler_player_out_card(int get_seat_index, int operateCard);

	/**
	 * @param get_seat_index
	 * @return
	 */
	public abstract boolean handler_player_be_in_room(int get_seat_index);

	/**
	 * 围观者是没有位置的
	 * 
	 * @param player
	 * @return
	 */
	public boolean handler_observer_be_in_room(Player player) {
		return false;
	}

	/**
	 * @param get_seat_index
	 * @return
	 */
	public abstract boolean handler_player_ready(int get_seat_index, boolean is_cancel);

	// 金币场 玩家准备
	public abstract boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel);

	/**
	 * @param player
	 * @param operateCode
	 * @return
	 */
	public abstract boolean handler_release_room(Player player, int operateCode);

	/**
	 * @param player
	 * @return
	 */
	public abstract boolean handler_enter_room(Player player);

	/**
	 * 进入观战
	 * 
	 * @param player
	 * @param observer
	 * @return
	 */
	public boolean handler_enter_room_observer(Player player) {
		return false;
	}

	/**
	 * 离开观战
	 * 
	 * @param player
	 * @return
	 */
	public boolean handler_exit_room_observer(Player player) {
		return false;
	}

	/**
	 * @param player
	 * @param audioChat
	 * @param audioSize
	 * @param audioLen
	 * @return
	 */
	public abstract boolean handler_audio_chat(Player player, ByteString audioChat, int audioSize, float audioLen);

	/**
	 * @param player
	 * @param emjoyId
	 * @return
	 */
	public abstract boolean handler_emjoy_chat(Player player, int emjoyId);

	/**
	 * @param player
	 * @param pao
	 * @param qiang
	 * @return
	 */
	public abstract boolean handler_requst_pao_qiang(Player player, int pao, int qiang);

	/**
	 * @param player
	 * @param nao
	 * @return
	 */
	public abstract boolean handler_requst_nao_zhuang(Player player, int nao);

	/**
	 * @param seat_index
	 * @param call_banker
	 * @param qiang_banker
	 * @return
	 */
	public abstract boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker);

	/**
	 * @param player
	 * @param locationInfor
	 * @return
	 */
	public abstract boolean handler_requst_location(Player player, LocationInfor locationInfor);

	/**
	 * @param player
	 */
	public abstract boolean handler_player_offline(Player player);

	/**
	 * @param game_type_index
	 * @param game_rule_index
	 * @param game_round
	 */
	public abstract void init_table(int game_type_index, int game_rule_index, int game_round);

	public abstract void init_other_param(Object... objects);

	/**
	 * @param player
	 */
	public abstract boolean handler_create_room(Player player, int type, int maxNumber);

	public abstract boolean dispatch_card_data(int cur_player, int type, boolean tail);

	/**
	 * @param _seat_index
	 * @param _reason
	 */
	public abstract boolean handler_game_finish(int _seat_index, int _reason);

	/**
	 * @param _seat_index
	 * @param _type
	 * @param _tail
	 */
	public abstract boolean runnable_dispatch_last_card_data(int _seat_index, int _type, boolean _tail);

	/**
	 * @param _seat_index
	 * @param _type
	 * @param _tail
	 */
	public abstract boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail);

	/**
	 * @param _seat_index
	 * @param _type
	 * @param _tail
	 */
	public abstract boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail);

	/*
	 * @param seat_index
	 * 
	 * @param provide_player
	 * 
	 * @param center_card
	 * 
	 * @param action
	 * 
	 * @param type //共杠还是明杠
	 * 
	 * @param self 自己摸的
	 * 
	 * @param d 双杠
	 * 
	 * @return
	 */
	public abstract boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch,
			boolean self, boolean d);

	/**
	 * @param _seat_index
	 * @param _type
	 */
	public abstract void runnable_remove_out_cards(int _seat_index, int _type);

	public abstract void runnable_set_trustee(int _seat_index);

	/**
	 * @param _seat_index
	 * @param _card_count
	 * @param _card_data
	 * @param _send_client
	 */
	public abstract void runnable_add_discard(int _seat_index, int _card_count, int[] _card_data, boolean _send_client);

	/**
	 * 
	 */
	public abstract void runnable_create_time_out();

	/**
	 * 
	 */

	public abstract boolean refresh_room_redis_data(int type, boolean notifyRedis);

	public abstract boolean exe_finish(int reason);

	public int getGame_id() {
		return game_id;
	}

	public void setGame_id(int game_id) {
		this.game_id = game_id;
	}

	public void set_is_sys(boolean issys) {
		this.is_sys_room = issys;
	}

	public boolean is_sys() {
		return this.is_sys_room;
	}

	public abstract int getTablePlayerNumber();

	/**
	 * @param player
	 * @param openThree
	 * @return
	 */
	public abstract boolean handler_requst_open_less(Player player, boolean openThree);

	/**
	 * @param get_seat_index
	 * @param isTrustee
	 * @return
	 */
	public abstract boolean handler_request_trustee(int get_seat_index, boolean isTrustee);

	/**
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @return
	 */
	public abstract boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time);

	public Player getCreate_player() {
		return create_player;
	}

	public void setCreate_player(Player create_player) {
		this.create_player = create_player;
	}

	/**
	 * 31位之前的
	 * 
	 * @param cbRule
	 * @return
	 */
	public boolean has_rule(int cbRule) {
		return GameDescUtil.has_rule(_game_rule_index, cbRule);
	}

	/**
	 * 32位之后的
	 * 
	 * @param cbRule
	 * @return
	 */
	public boolean has_rule_ex(int cbRule) {
		return GameDescUtil.has_rule(gameRuleIndexEx, cbRule);
	}

	/**
	 * @param get_seat_index
	 * @param room_rq
	 * @return
	 */
	public abstract boolean handler_request_goods(int get_seat_index, RoomRequest room_rq);

	public void log_player_info(int seat_index, long accountID, String msg) {
		logger.info("房间[" + this.getRoom_id() + "]" + " 玩家[" + accountID + "seat:" + seat_index + "]" + msg);
	}

	public int getGameTypeIndex() {
		return _game_type_index;
	}

	public int getGameRuleIndex() {
		return _game_rule_index;
	}

	public abstract void clear_score_in_gold_room();

	public abstract boolean handler_refresh_player_data(int seat);

	public abstract boolean kickout_not_ready_player();

	public boolean handler_refresh_all_player_data() {
		return false;
	}

	public abstract boolean open_card_timer();

	public abstract boolean robot_banker_timer();

	public abstract boolean ready_timer();

	public abstract boolean add_jetton_timer();

}
