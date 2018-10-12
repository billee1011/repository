package com.cai.game.dbd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.EGoldOperateType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.EMsgIdType;
import com.cai.common.define.ERedisTopicType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.DescParams;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.future.runnable.XiaoHuRunnable;
import com.cai.game.dbd.handler.DBDHandlerFinish;
import com.cai.game.gdy.handler.GDYHandlerFinish;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.zjh.ZjhRsp.GameStartZJH;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.laopai.LpRsp.LP_XU_JIPAIQI;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;

///////////////////////////////////////////////////////////////////////////////////////////////
public class DBDTable extends AbstractDBDTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(DBDTable.class);
	public DBDTable() {

		super();

		// 结束信息
	}

	@Override
	protected void onInitTable() {
		_handler_finish = new DBDHandlerFinish();
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		// TODO Auto-generated method stub
		handler_request_trustee(_seat_index, true, 0);

	}

	////////////////////////////////////////////////////////////////////////

	/**
	 * 第一轮 初始化庄家 默认第一个。需要的继承
	 */
	@Override
	protected void initBanker() {
		if (is_mj_type(GameConstants.GAME_TYPE_CS)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)) {
			this.shuffle_players();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this.get_players()[i].set_seat_index(i);
				if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
					this._cur_banker = i;
				}
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD) || is_mj_type(GameConstants.GAME_TYPE_HENAN_KF)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_NY) || is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_XX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XY) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS) || is_mj_type(GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ) || is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ)) {

			if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
				int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER - 1);
				this._cur_banker = banker;
			} else {
				int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
				this._cur_banker = banker;
			}

		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ))// 红中玩法
		{
			_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			// 安阳麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_AY];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_AY);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_AY];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_AY);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XY)) {
			// 信阳麻将
			_repertory_card = new int[GameConstants.CARD_COUNT_XY];
			shuffle(_repertory_card, GameConstants.CARD_DATA_XY);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_SMX)) {
			// 三门峡麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_SMX];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_SMX);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_SMX];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_SMX);
			}

		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_XC)) {
			// xuchang麻将
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LZ)) {
			// 林州
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN) || is_mj_type(GameConstants.GAME_TYPE_HENAN_ZMD)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_KF) || is_mj_type(GameConstants.GAME_TYPE_HENAN_NY)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_XX) || is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS)) {
			// 河南通用麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_HZ)) {
			// 河南红中麻将
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_HNHZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_HNHZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC)) {
			// 洛阳杠次初始化牌组 (使用林州牌组)
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
				_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
			} else {
				_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHOU_KOU)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 136张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_DAI_FENG];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_HONG_ZHONG_LAI_ZI];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_HENAN_LH)) {
			if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) { // 136张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_DAI_FENG];
				shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG);
			} else if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HENAN_HONG_ZHONG_LAI_ZI)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU_HONG_ZHONG_LAI_ZI];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HE_NAN_ZHOU_KOU];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else {
			// 晃晃麻将也可以用湖南麻将的牌（推倒胡）
			_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HU_NAN);
		}

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
		}

		on_game_start();
		return false;
	}

	@Override
	protected boolean on_game_start() {
		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int card_cards[]) {

	}

	@Override
	public void test_cards() {

	}

	




	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		
		return super.on_handler_game_finish(seat_index, reason);
	}


	public boolean handler_game_finish_xp(int seat_index, int reason) {
		int real_reason = reason;
		// 错误断言
		return false;
	}


	// 强制把玩家踢出房间
	public boolean force_kick_player_out_room(int seat_index, String tip) {

		if (seat_index == GameConstants.INVALID_SEAT)
			return false;
		Player p = this.get_players()[seat_index];
		if (p == null)
			return false;

		RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
		quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
		send_response_to_player(seat_index, quit_roomResponse);

		send_error_notify(seat_index, 2, tip);// "您已退出该游戏"
		this.get_players()[seat_index] = null;
		_player_ready[seat_index] = 0;
		// _player_open_less[seat_index] = 0;

		PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), p.getAccount_id());

		if (getPlayerCount() == 0) {// 释放房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		} else {
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			send_response_to_other(seat_index, refreshroomResponse);
		}
		if (_kick_schedule != null) {
			_kick_schedule.cancel(false);
			_kick_schedule = null;
		}
		return true;
	}

	public boolean check_if_kick_unready_player() {
		if (!is_sys())
			return false;
		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule == null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					_kick_schedule = GameSchedule.put(new KickRunnable(getRoom_id()), GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
					return true;
				}
			}
		}
		return false;
	}

	public boolean check_if_cancel_kick() {
		if (!is_sys())
			return false;

		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule != null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					return false;
				}
			}

			_kick_schedule.cancel(false);
			_kick_schedule = null;
		}
		return false;
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
			check_if_cancel_kick();

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
			Player p = this.get_players()[get_seat_index];
			if (p == null) {
				return false;
			}
			// 金币场配置
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
			// 判断金币是否足够
			long gold = p.getMoney();
			long entrygold = sysParamModel.getVal4().longValue();
			if (gold < entrygold) {
				force_kick_player_out_room(get_seat_index, "金币必须大于" + entrygold + "才能游戏!");
				return false;
			}
			boolean ret = handler_player_ready(get_seat_index, is_cancel);
			check_if_kick_unready_player();
			return ret;
		}
	}



	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;
		// return handler_player_ready(seat_index, false);
		return true;
	}

	@Override
	public void fixBugTemp(int seat_index) {
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
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
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
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end

		if (this._handler != null) {
			this._handler.handler_player_out_card(this, seat_index, card);
		}

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card,String desc) {
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
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card);
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



	


	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		/**
		 * if(_playerStatus[player.get_seat_index()]._is_pao_qiang ||
		 * _playerStatus[player.get_seat_index()]._is_pao){ return false; }
		 */


		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return false;

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
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);

		int flashTime = 150;
		int standTime = 130;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);
		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 150;
		int standTime = 150;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}
			this.send_response_to_other(seat_index, roomResponse);// 别人是背着的
		} else {
			if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);
				}
			}

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	// 添加牌到牌堆
	@Override
	public boolean operate_add_discard(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// 出牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
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
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
		GRR.add_room_response(roomResponse);

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
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 发牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param to_player
	 * @return
	 */
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_other(seat_index, roomResponse);

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}
			GRR.add_room_response(roomResponse);
			return this.send_response_to_player(seat_index, roomResponse);

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}
			// GRR.add_room_response(roomResponse);
			return this.send_response_to_player(to_player, roomResponse);
		}

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
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

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
	public boolean operate_player_weave_cards(int seat_index, int weave_count, WeaveItem weaveitems[]) {
		// System.out.println(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_WEAVE_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		// roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
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

		this.send_response_to_other(seat_index, roomResponse);

		/*
		 * // 手牌--将自己的手牌数据发给自己 for (int j = 0; j < card_count; j++) {
		 * roomResponse.addCardData(cards[j]); }
		 */
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

		// this.load_common_status(roomResponse);
		//
		// //手牌数量
		// roomResponse.setCardCount(card_count);
		// roomResponse.setWeaveCount(weave_count);
		// //组合牌
		// if (weave_count>0) {
		// for (int j = 0; j < weave_count; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
		// weaveItem_item.setPublicCard(weaveitems[j].public_card);
		// weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
		// weaveItem_item.setCenterCard(weaveitems[j].center_card);
		// roomResponse.addWeaveItems(weaveItem_item);
		// }
		// }
		//
		// this.send_response_to_other(seat_index, roomResponse);
		//
		// // 手牌
		// for (int j = 0; j < card_count; j++) {
		// roomResponse.addCardData(cards[j]);
		// }
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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	// 12更新玩家手牌数据
	// 13更新玩家牌数据(包含吃碰杠组合)
	// public boolean refresh_hand_cards(int seat_index, boolean send_weave) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// if (send_weave == true) {
	// roomResponse.setType(13);// 13更新玩家牌数据(包含吃碰杠组合)
	// } else {
	// roomResponse.setType(12);// 12更新玩家手牌数据
	// }
	// roomResponse.setGameStatus(_game_status);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
	// int hand_card_count =
	// _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);
	//
	// //手牌数量
	// roomResponse.setCardCount(hand_card_count);
	//
	// // 手牌
	// for (int j = 0; j < hand_card_count; j++) {
	// roomResponse.addCardData(hand_cards[j]);
	// }
	//
	// //组合牌
	// if (send_weave == true) {
	// // 13更新玩家牌数据(包含吃碰杠组合)
	// for (int j = 0; j < GRR._weave_count[seat_index]; j++) {
	// WeaveItemResponse.Builder weaveItem_item =
	// WeaveItemResponse.newBuilder();
	// weaveItem_item.setProvidePlayer(GRR._weave_items[seat_index][j].provide_player);
	// weaveItem_item.setPublicCard(GRR._weave_items[seat_index][j].public_card);
	// weaveItem_item.setWeaveKind(GRR._weave_items[seat_index][j].weave_kind);
	// weaveItem_item.setCenterCard(GRR._weave_items[seat_index][j].center_card);
	// roomResponse.addWeaveItems(weaveItem_item);
	// }
	// }
	// // 自己才有牌数据
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

	// 14摊开玩家手上的牌
	// public boolean send_show_hand_card(int seat_index, int cards[]) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(14);
	// roomResponse.setCardCount(cards.length);
	// roomResponse.setOperatePlayer(seat_index);
	// for (int i = 0; i < cards.length; i++) {
	// roomResponse.addCardData(cards[i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	// return true;
	// }

	// public boolean refresh_discards(int seat_index) {
	// if (seat_index == MJGameConstants.INVALID_SEAT) {
	// return false;
	// }
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_REFRESH_DISCARD);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int l = GRR._discard_count[seat_index];
	// for (int i = 0; i < l; i++) {
	// roomResponse.addCardData(GRR._discard_cards[seat_index][i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

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



	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// if(is_mj_type(MJGameConstants.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
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
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
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
		
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void clearHasPiao() {
		// int count = getTablePlayerNumber();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.haspiao[i] = 0;
		}
	}

	

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//
		}
	}


	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	public boolean is_mj(int id) {
		return this.getGame_id() == id;
	}

	


	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (getGame_id() == 1) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjpph, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.jjh, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidilao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidipao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.haohuaqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshanghua, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qiangganghu, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshangpao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.quanqiuren, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.shaohuaqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshanghua, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshangpao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				// 小胡
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.liuliushun, "", _game_type_index, 0l,
							this.getRoom_id());
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "", _game_type_index, 0l,
							this.getRoom_id());

			}

			if (getGame_id() == 2) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.heimo, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RUAN_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.ruanmo, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_ZHUO_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.zhuotong, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RE_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.rechong, "", _game_type_index, 0l,
							this.getRoom_id());
				}
			}

			if (getGame_id() == 3) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sihun, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henangang, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqiduihaohua, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henankaihua, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HZ_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henan4hong, "", _game_type_index, 0l,
							this.getRoom_id());
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	// 转转麻将结束描述
	@Override
	protected void set_result_describe() {

	}







	public String get_game_des() {
		DescParams params = GameDescUtil.params.get();
		
		putDescParam(params);
		params._game_rule_index = _game_rule_index;
		params._game_type_index = _game_type_index;
		if (gameRuleIndexEx != null) {
			params.game_rules = gameRuleIndexEx;
		}

		return GameDescUtil.getGameDesc(params);
	}

	@Override
	public boolean is_zhuang_xian() {
		return true;
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
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish(int reason) {
		this._end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		this.set_handler(this._handler_finish);
		this._handler_finish.exe(this);

		return true;
	}

	/***
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param card_data
	 * @param send_client
	 * @param delay
	 * @return
	 */
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
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {

		}

		return true;
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
		// add end


		return true;
	}

	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
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
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getPlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}


	// System.out.println("==进入心跳handler==" + start);

	// }

	// public StateMachine<MJTable> get_state_machine() {
	// return _state_machine;
	// }
	//
	// public void set_state_machine(StateMachine<MJTable> _state_machine) {
	// this._state_machine = _state_machine;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}
		if (!is_sys())// 麻将普通场 不需要托管
			return false;

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return true;

	}

	@Override
	public int set_ding_niao_valid(int card_data, boolean val) {
		// 先把值还原
		if (val) {
			if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_INVALID;
			} else if (card_data > GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.DING_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID : card_data);
		}
	}

	@Override
	public int set_fei_niao_valid(int card_data, boolean val) {
		if (val) {
			if (card_data > GameConstants.FEI_NIAO_INVALID && card_data < GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_INVALID;
			} else if (card_data > GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.FEI_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.FEI_NIAO_VALID ? card_data + GameConstants.FEI_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.FEI_NIAO_INVALID ? card_data + GameConstants.FEI_NIAO_INVALID : card_data);
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
		_kick_schedule = null;

		if (!is_sys())
			return false;

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			if (getPlayerCount() != GameConstants.GAME_PLAYER || _player_ready == null) {
				return false;
			}

			// 检查是否所有人都未准备
			int not_ready_count = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (get_players()[i] != null && _player_ready[i] == 0) {// 未准备的玩家
					not_ready_count++;
				}
			}
			if (not_ready_count == GameConstants.GAME_PLAYER)// 所有人都未准备 不用踢
				return false;

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Player rPlayer = get_players()[i];
				if (rPlayer != null && _player_ready[i] == 0) {// 未准备的玩家
					send_error_notify(i, 2, "您长时间未准备,被踢出房间!");

					RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
					quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
					send_response_to_player(i, quit_roomResponse);

					this.get_players()[i] = null;
					_player_ready[i] = 0;
					// _player_open_less[i] = 0;
					PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), rPlayer.getAccount_id());
				}
			}
			//
			if (getPlayerCount() == 0) {// 释放房间
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				this.load_player_info_data(refreshroomResponse);
				send_response_to_room(refreshroomResponse);
			}
			return true;
		}
		return false;
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


	/*
	 * public void change_handler(MJHandler dest_handler) { _handler =
	 * dest_handler; }
	 */
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		// 如果是出牌或者操作状态 需要倒计时托管&& !istrustee[seat_index]
		if (is_sys() && (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
			_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index), GameConstants.TRUSTEE_TIME_OUT_SECONDS,
					TimeUnit.SECONDS);
		}
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
			if (this.get_players()[i] == null) {
				continue;
			}
			score = (int) (scores[i] * beilv);
			// 逻辑处理
			this.get_players()[i].setMoney(this.get_players()[i].getMoney() + score);
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
		// playerNumber = 0;
	}

	@Override
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player,int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
