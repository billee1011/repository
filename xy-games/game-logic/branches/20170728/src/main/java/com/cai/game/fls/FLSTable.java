/**
 * 
 */
package com.cai.game.fls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.Room;
import com.cai.common.domain.RoomRedisModel;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.LocationUtil;
import com.cai.common.util.RoomComonUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.fls.handler.FLSHandler;
import com.cai.game.fls.handler.FLSHandlerChiPeng;
import com.cai.game.fls.handler.FLSHandlerDispatchCard;
import com.cai.game.fls.handler.FLSHandlerFinish;
import com.cai.game.fls.handler.FLSHandlerGang;
import com.cai.game.fls.handler.FLSHandlerHaiDi;
import com.cai.game.fls.handler.FLSHandlerOutCardOperate;
import com.cai.game.fls.handler.FLSHandlerYaoHaiDi;
import com.cai.game.fls.handler.flsdp.FLSHandlerChiPeng_LXDP;
import com.cai.game.fls.handler.flsdp.FLSHandlerDispatchCard_LXDP;
import com.cai.game.fls.handler.flsdp.FLSHandlerDispatchLastCard_LXDP;
import com.cai.game.fls.handler.flsdp.FLSHandlerGang_LXDP;
import com.cai.game.fls.handler.flsdp.FLSHandlerGang_LXDP_DispatchCard;
import com.cai.game.fls.handler.flsdp.FLSHandlerOutCardOperate_LXDP;
import com.cai.game.fls.handler.flsdp.FLSHandlerPiao_FLS_DP;
import com.cai.game.fls.handler.flsdp.FLSHandlerXiPai_FLS_DP;
import com.cai.game.fls.handler.flsdp.FLSHandlerYaoHaiDi_LX_DP;
import com.cai.game.fls.handler.lxfls.FLSHandlerChiPeng_LX;
import com.cai.game.fls.handler.lxfls.FLSHandlerDispatchCard_LX;
import com.cai.game.fls.handler.lxfls.FLSHandlerDispatchLastCard_LX;
import com.cai.game.fls.handler.lxfls.FLSHandlerGang_LX;
import com.cai.game.fls.handler.lxfls.FLSHandlerGang_LX_DispatchCard;
import com.cai.game.fls.handler.lxfls.FLSHandlerOutCardOperate_LX;
import com.cai.game.fls.handler.lxfls.FLSHandlerPiao_FLS;
import com.cai.game.fls.handler.lxfls.FLSHandlerXiPai_FLS;
import com.cai.game.fls.handler.lxfls.FLSHandlerYaoHaiDi_LX;
import com.cai.game.fls.handler.twentyfls.FLSHandlerChiPeng_LX_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerDispatchCard_LX_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerDispatchLastCard_LX_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerGang_LX_DispatchCard_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerGang_LX_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerOutCardOperate_LX_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerPiao_FLS_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerXiPai_FLS_Twenty;
import com.cai.game.fls.handler.twentyfls.FLSHandlerYaoHaiDi_LX_Twenty;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.MsgAllResponse;
import protobuf.clazz.Protocol.PlayerResultFLSResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.redis.ProtoRedis.RedisResponse;
import protobuf.redis.ProtoRedis.RedisResponse.RsResponseType;
import protobuf.redis.ProtoRedis.RsAccountResponse;
import protobuf.redis.ProtoRedis.RsCmdResponse;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class FLSTable extends Room {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(FLSTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	/**
	 * 这个值 不能在这里改--这个参数测试用 通过后台改牌 改单个桌子的
	 */
	public boolean BACK_DEBUG_CARDS_MODE = false;
	/**
	 * 后台操作临时数组
	 */
	public int debug_my_cards[];

	public int _cur_round; // 局数
	public int _player_ready[]; // 准备
	public int _player_open_less[]; // 允许三人场
	public float _di_fen; // 底分

	public int _game_status;

	public boolean istrustee[]; // 托管状态

	public ScheduledFuture _trustee_schedule[];
	// 状态变量
	private boolean _status_send; // 发牌状态
	private boolean _status_gang; // 抢杆状态
	private boolean _status_cs_gang; // 长沙杆状态
	private boolean _status_gang_hou_pao; // 杠后炮状态

	public PlayerResult _player_result;

	// 运行变量
	public int _provide_card = GameConstants.INVALID_VALUE; // 供应扑克
	public int _resume_player = GameConstants.INVALID_SEAT; // 还原用户
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _provide_player = GameConstants.INVALID_SEAT; // 供应用户

	public PlayerStatus _playerStatus[];

	// 发牌信息
	public int _send_card_data = GameConstants.INVALID_VALUE; // 发牌扑克

	public CardsData _gang_card_data;

	public int _send_card_count = GameConstants.INVALID_VALUE; // 发牌数目

	public int _repertory_card[];

	public int _qiang_MAX_FLS_COUNT; // 最大呛分
	public int _lian_zhuang_player; // 连庄玩家
	public int _shang_zhuang_player; // 上庄玩家

	// 出牌信息
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _out_card_count = GameConstants.INVALID_VALUE; // 出牌数目
	public int _all_card_len = 0;

	GameRoomRecord _gameRoomRecord;
	BrandLogModel _recordRoomRecord;

	public GameRoundRecord GRR;

	public FLSGameLogic _logic = null;

	/**
	 * 当前牌桌的庄家
	 */
	public int _banker_select = GameConstants.INVALID_SEAT;

	/**
	 * 当前桌子内玩家数量
	 */
	private int playerNumber;

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public FLSHandler _handler;

	public FLSHandlerDispatchCard _handler_dispath_card;
	public FLSHandlerOutCardOperate _handler_out_card_operate;
	public FLSHandlerGang _handler_gang;
	public FLSHandlerChiPeng _handler_chi_peng;
	public FLSHandlerHaiDi _handler_hai_di;
	public FLSHandlerYaoHaiDi _handler_yao_hai_di;
	public FLSHandlerGang_LX_DispatchCard _handler_gang_fls;
	public FLSHandlerPiao_FLS _handler_piao_fls;
	public FLSHandlerXiPai_FLS _handler_xipai_fls;

	public FLSHandlerDispatchLastCard_LX _handler_dispath_last_card;

	// 20张
	public FLSHandlerPiao_FLS_Twenty _handler_piao_fls_twenty;
	public FLSHandlerGang_LX_DispatchCard_Twenty _handler_gang_fls_twenty;
	public FLSHandlerXiPai_FLS_Twenty _handler_xipai_fls_twenty;
	public FLSHandlerDispatchLastCard_LX_Twenty _handler_dispath_last_card_twenty;

	public FLSHandlerFinish _handler_finish; // 结束

	public AnalyseItemTwenty[] analyseItemTwenty;

	public FLSTable() {
		super(RoomType.FLS, GameConstants.GAME_PLAYER);

		_logic = new FLSGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			get_players()[i] = null;

		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[GameConstants.GAME_PLAYER];
		_player_open_less = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}

		// 出牌信息
		_out_card_data = 0;

		_status_cs_gang = false;

		_gang_card_data = new CardsData(GameConstants.MAX_FLS_COUNT);
	}

	public void init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		boolean kd = this.kou_dou();
		if (kd == false) {
			return;
		}
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new FLSHandlerDispatchCard_LX();
			_handler_out_card_operate = new FLSHandlerOutCardOperate_LX();
			_handler_gang = new FLSHandlerGang_LX();
			_handler_chi_peng = new FLSHandlerChiPeng_LX();

			_handler_gang_fls = new FLSHandlerGang_LX_DispatchCard();
			_handler_piao_fls = new FLSHandlerPiao_FLS();

			_handler_xipai_fls = new FLSHandlerXiPai_FLS();

			_handler_yao_hai_di = new FLSHandlerYaoHaiDi_LX();

			_handler_dispath_last_card = new FLSHandlerDispatchLastCard_LX();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new FLSHandlerDispatchCard_LX_Twenty();
			_handler_out_card_operate = new FLSHandlerOutCardOperate_LX_Twenty();
			_handler_gang = new FLSHandlerGang_LX_Twenty();
			_handler_chi_peng = new FLSHandlerChiPeng_LX_Twenty();

			_handler_gang_fls_twenty = new FLSHandlerGang_LX_DispatchCard_Twenty();
			_handler_piao_fls_twenty = new FLSHandlerPiao_FLS_Twenty();

			_handler_xipai_fls_twenty = new FLSHandlerXiPai_FLS_Twenty();

			_handler_yao_hai_di = new FLSHandlerYaoHaiDi_LX_Twenty();

			_handler_dispath_last_card_twenty = new FLSHandlerDispatchLastCard_LX_Twenty();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new FLSHandlerDispatchCard_LXDP();
			_handler_out_card_operate = new FLSHandlerOutCardOperate_LXDP();
			_handler_gang = new FLSHandlerGang_LXDP();
			_handler_chi_peng = new FLSHandlerChiPeng_LXDP();

			_handler_gang_fls = new FLSHandlerGang_LXDP_DispatchCard();
			_handler_piao_fls = new FLSHandlerPiao_FLS_DP();

			_handler_xipai_fls = new FLSHandlerXiPai_FLS_DP();

			_handler_yao_hai_di = new FLSHandlerYaoHaiDi_LX_DP();

			_handler_dispath_last_card = new FLSHandlerDispatchLastCard_LXDP();
		}
		_handler_finish = new FLSHandlerFinish();

	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	////////////////////////////////////////////////////////////////////////
	public boolean reset_init_data() {

		if (_cur_round == 0) {

			this.shuffle_players();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				this.get_players()[i].set_seat_index(i);
				if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
					this._banker_select = i;
				}
			}

			record_game_room();
		}

		_run_player_id = 0;
		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;
		
		if(is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_FLS, GameConstants.MAX_FLS_COUNT,
					getMaxIndex());
		}else{
			GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_FLS, GameConstants.MAX_FLS_COUNT,
					getMaxIndex());
		}

		
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;

		// 新建
		_playerStatus = new PlayerStatus[4];
		istrustee = new boolean[4];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_FLS_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].reset();
		}

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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);
		}

		GRR._video_recode.setBankerPlayer(this._banker_select);

		return true;
	}

	public void getLocationTip() {
		 RoomUtil.getLocationTip(this);
	}

	// 游戏开始
	public boolean handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _banker_select;
		_current_player = GRR._banker_player;

		//
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_FLS_LX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_FLS_LX);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_FLS_LX];
			shuffle_twenty(_repertory_card, GameConstants.CARD_DATA_FLS_LX);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_FLS_LX_DP];
			shuffle(_repertory_card, GameConstants.CARD_DATA_FLS_LX_DP);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		try {
			if (!is_sys() && GRR != null) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					int number = 0;
					if (GRR._cards_index[i] == null)
						continue;
					for (int j = 0; j < GRR._cards_index[i].length; j++) {
						if (GRR._cards_index[i][j] == 4) {
							MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "",
									GRR._cards_index[i][j], 0l, this.getRoom_id());
							number++;
						}
					}
					if (number == 2) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong2, "", 0, 0l,
								this.getRoom_id());
					}
				}
			}

		} catch (Exception e) {

		}

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			return game_start_fls();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			return game_start_fls_twenty();
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			return game_start_fls_dp();
		}
		return false;
	}

	/**
	 * 开始 福禄寿20张
	 * 
	 * @return
	 */
	private boolean game_start_fls_twenty() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			this._handler = this._handler_piao_fls_twenty;
			this._handler_piao_fls_twenty.exe(this);
			return true;
		} else {
			for (int i = 0; i < playerCount; i++) {
				istrustee[i] = false;
				if (_player_result.pao[i] < 0) {
					_player_result.pao[i] = 0;
				}
			}
		}
		exe_xi_pai_twenty();
		return true;
	}

	/**
	 * 开始福禄寿
	 * 
	 * @return
	 */
	private boolean game_start_fls() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			change_handler(_handler_piao_fls);
			this._handler_piao_fls.exe(this);
			return true;
		} else {
			for (int i = 0; i < playerCount; i++) {
				if (_player_result.pao[i] < 0) {
					_player_result.pao[i] = 0;
				}
				istrustee[i] = false;
			}
		}
		exe_xi_pai();
		return true;
	}

	/**
	 * 开始福禄寿
	 * 
	 * @return
	 */
	private boolean game_start_fls_dp() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			change_handler(_handler_piao_fls);
			this._handler_piao_fls.exe(this);
			return true;
		} else {
			for (int i = 0; i < playerCount; i++) {
				if (_player_result.pao[i] < 0) {
					_player_result.pao[i] = 0;
				}
				istrustee[i] = false;
			}
		}
		exe_xi_pai();
		return true;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > GameConstants.GAME_PLAYER)
			return false;
		boolean isStrustee = istrustee[seat_index];

		if (is_sys()) {// 金币场托管 不用需要听牌
			return isStrustee;
		}

		boolean isTing = _playerStatus[seat_index]._hu_card_count > 0;
		if (isStrustee && !isTing) {
			handler_request_trustee(seat_index, false);
		}
		return isStrustee && isTing;
	}

	public void start_card_begin() {
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_FLS_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

	}

	/**
	 * 开始游戏 逻辑
	 */
	public void game_start_real_twenty() {
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_FLS_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = this.get_fls_ting_card_twenty(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i], i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 开始游戏 逻辑
	 */
	public void game_start_real() {

		if ((is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY))) {
			game_start_real_twenty();
			return;
		}else 	if ((is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP))) {
			game_start_real_dp();
			return;
		}

		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_FLS_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = this.get_fls_ting_card(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
	}
	
	
	/**
	 * 开始游戏 逻辑
	 */
	public void game_start_real_dp() {

		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_FLS_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = this.get_fls_ting_card_dp(this._playerStatus[i]._hu_cards,
					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		_logic.random_card_data(repertory_card, mj_cards);

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();

		if (count != GameConstants.GAME_PLAYER) {
			for (int i = 0; i < count; i++) {
				send_count = (GameConstants.MAX_FLS_COUNT - 2);
				GRR._left_card_count -= send_count;
				// 一人18张牌,庄家多一张
				_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
				have_send_count += send_count;
			}
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				send_count = (GameConstants.MAX_FLS_COUNT - 2);
				if (isZhuangDui(i)) {// 庄家对面发2张
					send_count = 2;
				}

				GRR._left_card_count -= send_count;
				// 一人13张牌,庄家多一张
				_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
				have_send_count += send_count;
			}
		}
		if (_recordRoomRecord != null) {
			// 记录初始牌型
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}

	}

	/// 洗牌
	private void shuffle_twenty(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		_logic.random_card_data(repertory_card, mj_cards);

		int send_count;
		int have_send_count = 0;

		int count = 3;

		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_FLS_COUNT - 1);

			GRR._left_card_count -= send_count;
			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			// 记录初始牌型
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	/**
	 * 是否是 庄家的对家
	 * 
	 * @return
	 */
	private boolean isZhuangDui(int i) {
		if (getPlayerCount() == GameConstants.GAME_PLAYER) {
			return i != _banker_select && Math.abs(_banker_select - i) == 2;
		}
		return false;
	}

	private void test_cards() {
		// int cards[] = new int[] { 0x71, 0x71, 0x71, 0x71, 0x61, 0x62, 0x63,
		// 0x51, 0x52, 0x53, 0x21, 0x22, 0x23,0x02,0x02,0x02,0x03,0x03 };

		// 上大人 丘乙己 单调一个
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03,
		// 0x03, 0x03, 0x11, 0x11, 0x11, 0x12, 0x12,
		// 0x12, 0x13, 0x13, 0x13 };

		// int cards[] = new int[] { 0x02,0x02,0x02,0x23,0x23,0x23,
		// 0x03,0x03,0x03, 0x11,0x11,0x11, 0x12,0x12,0x12,0x13,0x13,0x13};

		// 上大人 丘乙己 来牌 0x13
		// int cards[] = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,
		// 0x03,0x03,0x03, 0x11,0x11,0x11, 0x12,0x12,0x12,0x13,0x13,0x23};

		// 20张
		 int cards[] = new int[] { 0x31, 0x31, 0x02, 0x02, 0x02, 0x02, 0x03,
		 0x03, 0x03, 0x11, 0x11, 0x11, 0x12, 0x12,
		 0x12, 0x13, 0x13, 0x23,0x23 };

		// int cards[] = new int[] { 0x31, 0x31, 0x01, 0x01, 0x02, 0x02, 0x03,
		// 0x03, 0x11, 0x11, 0x11, 0x11, 0x12, 0x12,
		// 0x12, 0x13, 0x13, 0x23,0x23 };

		// int cards[] = new int[] { 0x31, 0x31, 0x01, 0x02, 0x02, 0x02, 0x03,
		// 0x03, 0x03, 0x11, 0x11, 0x11, 0x12, 0x12,
		// 0x12, 0x13, 0x13, 0x23, 0x23 };

		// int cards[] = new int[] {
		// 0x02,0x02,0x02,0x02,0x12,0x13,0x22,0x23,0x21,0x41,0x42,0x43,0x52,0x51,0x53,0x71,0x72,0x73};

		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x01, 0x53, 0x21, 0x22,
		// 0x23, 0x41, 0x31, 0x42, 0x32, 0x43, 0x33,
		// 0x51, 0x52, 0x73, 0x71, 0x72 };

		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x11,
		// 0x11, 0x11, 0x12, 0x13, 0x21, 0x22, 0x22,
		// 0x22, 0x23, 0x31, 0x32, 0x33 };

		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03,
		// 0x03, 0x03, 0x01, 0x02, 0x03, 0x71, 0x72,
		// 0x73, 0x71, 0x72, 0x73, 0x71 };


		// int cards[] = new int[] { 0x01, 0x01, 0x02, 0x02, 0x22, 0x22, 0x03,
		// 0x03, 0x13, 0x13, 0x11, 0x11, 0x12, 0x12,
		// 0x12, 0x12, 0x13, 0x13, 0x23, 0x23 };

		// int cards[] = new int[] { 0x11, 0x12, 0x13, 0x33, 0x33, 0x33, 0x02,
		// 0x02, 0x02, 0x21, 0x21, 0x21, 0x23, 0x23,
		// 0x23, 0x41, 0x41, 0x41, 0x51, 0x51 };

		// int cards[] = new int[] { 0x11, 0x12, 0x13, 0x21, 0x22, 0x23, 0x31,
		// 0x32, 0x33, 0x41, 0x42, 0x43, 0x51, 0x52,
		// 0x53, 0x61, 0x62, 0x63, 0x51, 0x51 };

		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x01, 0x02, 0x03, 0x31,
		// 0x32, 0x33, 0x41, 0x42, 0x43, 0x51, 0x52,
		// 0x53, 0x71, 0x72, 0x73, 0x51, 0x51 };

		// 双对缺1 碰碰胡
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03,
		// 0x03, 0x03, 0x11, 0x11, 0x11, 0x12, 0x12,
		// 0x12, 0x13, 0x13, 0x23 };

		// int cards[] = new int[] { 0x01,0x02,0x03,0x11,0x12,0x13,
		// 0x21,0x22,0x23, 0x31,0x32,0x33, 0x42,0x41,0x43,0x53,0x53,0x53};

		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x11, 0x12, 0x13, 0x21,
		// 0x22, 0x23, 0x31, 0x32, 0x33, 0x42, 0x41,
		// 0x23, 0x53, 0x53, 0x53 };

		// 算胡系
		// int cards[] = new int[] { 0x31, 0x32, 0x33, 0x21, 0x22, 0x23, 0x11,
		// 0x12, 0x13, 0x41, 0x42, 0x43, 0x51, 0x52,
		// 0x53, 0x61, 0x62, 0x63 };

		// int cards[] = new int[] { 0x31, 0x31, 0x31, 0x41, 0x42, 0x43, 0x41,
		// 0x42, 0x43, 0x41, 0x42, 0x43, 0x51, 0x52,
		// 0x53, 0x61, 0x62, 0x63 };

		// int cards[] = new int[] { 0x31, 0x32, 0x33, 0x21, 0x22, 0x23, 0x11,
		// 0x12, 0x13, 0x41, 0x41, 0x51, 0x51, 0x52,
		// 0x53, 0x71, 0x72, 0x73 };

		// 真金真坎
		// int cards[] = new int[] { 0x31, 0x32, 0x33, 0x21, 0x22, 0x23, 0x61,
		// 0x62, 0x63, 0x41, 0x42, 0x43, 0x02, 0x02,
		// 0x02, 0x71, 0x72, 0x73 };

		// int cards[] = new int[] { 0x01, 0x21, 0x22, 0x23, 0x61, 0x62, 0x63,
		// 0x41, 0x42, 0x43, 0x03, 0x03, 0x03, 0x32,
		// 0x31, 0x51, 0x52, 0x53 };

		// 满天飞
		// int cards[] = new int[] { 0x31, 0x31, 0x33, 0x21, 0x21, 0x23, 0x11,
		// 0x12, 0x13, 0x41, 0x42, 0x43, 0x51, 0x51,
		// 0x52, 0x61, 0x62, 0x63 };

		// 碰碰胡

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x51, 0x51, 0x61,
		// 0x61, 0x61, 0x41, 0x41, 0x41, 0x21, 0x21,
		// 0x21, 0x33, 0x32, 0x33 };
		// int cards[] = new
		// int[]{0x02,0x02,0x02,0x011,0x11,0x11,0x31,0x22,0x22,0x22,0x32,0x33,0x41,0x42,0x43,0x71,0x72,0x73
		// };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x51, 0x51, 0x61,
		// 0x61, 0x61, 0x41, 0x41, 0x41, 0x21, 0x21,
		// 0x21, 0x33, 0x32, 0x33 };

		// 杠上炮
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x2, 0x2, 0x2, 0x2, 0x3,
		// 0x3, 0x3, 0x3, 0x11, 0x51, 0x51,
		// 0x51,0x61,0x61,0x61};

		// int cards[] = new int[] { 0x03, 0x03, 0x03, 0x02, 0x02, 0x01, 0x31,
		// 0x31, 0x41, 0x41, 0x41, 0x51, 0x51, 0x51,
		// 0x61, 0x61, 0x61, 0x22 };

		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x22, 0x23, 0x11,
		// 0x12, 0x13, 0x21, 0x42, 0x51, 0x51, 0x51,
		// 0x51, 0x71, 0x72, 0x73 };

		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x11, 0x12, 0x13, 0x31,
		// 0x32, 0x33, 0x41, 0x42, 0x43, 0x52, 0x52,
		// 0x72, 0x73, 0x13, 0x13 };

		// int cards[] = new int[] { 0x31, 0x31, 0x33, 0x21, 0x21, 0x23, 0x11,
		// 0x12, 0x13, 0x41, 0x42, 0x43, 0x31, 0x21,
		// 0x02, 0x01, 0x02, 0x03 };

		// 上大人 福禄寿 2句话
		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x02, 0x03, 0x03, 0x11,
		// 0x12, 0x13, 0x41, 0x42, 0x43, 0x51, 0x52,
		// 0x53, 0x02, 0x72, 0x72 };

		// int cards[] = new int[] { 0x11, 0x12, 0x13, 0x21, 0x22, 0x23, 0x31,
		// 0x32, 0x33, 0x41, 0x42, 0x43, 0x01, 0x02,
		// 0x03, 0x72, 0x72, 0x72 };

		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x02, 0x23, 0x11, 0x12,
		// 0x13, 0x31, 0x12, 0x51, 0x51, 0x51,0x52,
		// 0x71, 0x71, 0x71, 0x71 };

		// 4个人牌不一样 ---打开下面的代码 配置牌型即可
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// for (int j = 0; j < MJgetMaxIndex(); j++) {
		// GRR._cards_index[i][j] = 0;
		// }
		// }
		// int _cards0[] = new int[]
		// {0x01,0x02,0x04,0x04,0x05,0x07,0x07,0x08,0x14,0x19,0x21,0x23,0x29};
		// int _cards1[] = new int[]
		// {0x01,0x06,0x08,0x08,0x11,0x12,0x13,0x16,0x18,0x21,0x25,0x27,0x29};
		// int _cards2[] = new int[]
		// {0x02,0x03,0x05,0x07,0x09,0x12,0x14,0x18,0x24,0x21,0x25,0x25,0x27};
		// int _cards3[] = new int[]
		// {0x01,0x02,0x04,0x04,0x05,0x08,0x17,0x18,0x24,0x24,0x22,0x28,0x28};
		// for (int j = 0; j < 13; j++) {
		// GRR._cards_index[0][_logic.switch_to_card_index(_cards0[j])] += 1;
		// GRR._cards_index[1][_logic.switch_to_card_index(_cards1[j])] += 1;
		// GRR._cards_index[2][_logic.switch_to_card_index(_cards2[j])] += 1;
		// GRR._cards_index[3][_logic.switch_to_card_index(_cards3[j])] += 1;
		// }

		/** 线上问题牌型 */
		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x11, 0x12, 0x13, 0x31,
		// 0x32, 0x33, 0x41, 0x42, 0x43, 0x52, 0x52,
		// 0x72, 0x73, 0x13, 0x13 };

		// int cards[] = new int[] { 0x63, 0x63, 0x63, 0x73, 0x73, 0x73, 0x01,
		// 0x01, 0x01, 0x11, 0x11, 0x11, 0x12, 0x12,
		// 0x12, 0x32, 0x32, 0x32 };

		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x13, 0x13, 0x21, 0x22,
		// 0x32, 0x33, 0x31, 0x42, 0x43, 0x41, 0x52,
		// 0x51, 0x73, 0x72, 0x71 };

		// int cards[] = new int[] { 0x11, 0x12, 0x13, 0x31, 0x23, 0x21, 0x22,
		// 0x32, 0x33, 0x41, 0x42, 0x43, 0x53, 0x52,
		// 0x51, 0x71, 0x72, 0x72 };

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < getMaxIndex(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			int count = 3;
			for (int i = 0; i < count; i++) {
				int send_count = (GameConstants.MAX_FLS_COUNT - 1);
				for (int j = 0; j < send_count; j++) {
					GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
				}
			}
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				int send_count = (GameConstants.MAX_FLS_COUNT - 2);
				if (isZhuangDui(i)) {// 庄家对面发2张
					send_count = 2;
				}
				for (int j = 0; j < send_count; j++) {
					GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
				}
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		int[] realyCards = new int[] { 65, 17, 18, 33, 2, 115, 1, 83, 114, 82, 51, 51, 34, 17, 67, 19, 1, 114, 99, 35, 67, 66, 50, 35, 34, 115, 65, 82, 18, 97, 35, 97, 49, 98, 82, 33, 35, 66, 81, 50, 83, 99, 1, 18, 49, 114, 17, 51, 34, 3, 3, 82, 3, 2, 83, 51, 19, 67, 113, 65, 115, 98, 81, 67, 50, 81, 33, 66, 99, 113, 114, 99, 66, 18, 33, 98, 65, 98, 19, 49, 19, 1, 83, 115, 113, 34, 2, 97, 50, 49, 81, 2, 17, 3, 113, 97 };
		testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
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
			}

		}
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < getMaxIndex(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		// _banker_select = 0;
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			// 分发扑克
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				send_count = (GameConstants.MAX_FLS_COUNT - 1);
				GRR._left_card_count -= send_count;

				// 一人13张牌,庄家多一张
				_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

				have_send_count += send_count;
			}
		} else {
			// 分发扑克
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				send_count = (GameConstants.MAX_FLS_COUNT - 2);
				if (isZhuangDui(i)) {// 庄家对面发2张
					send_count = 2;
				}

				GRR._left_card_count -= send_count;

				// 一人13张牌,庄家多一张
				_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

				have_send_count += send_count;
			}
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < getMaxIndex(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			// 分发扑克
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				int send_count = (GameConstants.MAX_FLS_COUNT - 1);
				for (int j = 0; j < send_count; j++) {
					GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
				}
			}
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				int send_count = (GameConstants.MAX_FLS_COUNT - 2);
				if (isZhuangDui(i)) {// 庄家对面发2张
					send_count = 2;
				}
				for (int j = 0; j < send_count; j++) {
					GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
				}
			}
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		// 发牌
		change_handler(_handler_dispath_card);
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	// 游戏结束
	public boolean handler_game_finish(int seat_index, int reason) {
		if (is_sys()) {
			_game_status = GameConstants.GS_MJ_FREE;
		} else {
			_game_status = GameConstants.GS_MJ_WAIT;
		}
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		boolean ret = false;
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			ret = this.handler_game_finish_fls(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			ret = this.handler_game_finish_fls_twenty(seat_index, reason);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			ret = this.handler_game_finish_fls_dp(seat_index, reason);
		}
		clearHasPiao();
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	private void clearHasPiao() {
		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_player_result.haspiao[i] = 0;
		}
	}

	public boolean handler_game_finish_fls_dp(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
			istrustee[i] = false;
			////////////////////////////
			if (_playerStatus != null && _playerStatus[i] != null) {
				_playerStatus[i]._hu_card_count = 0;
			} else {
				logger.error("这个bug很诡异" + i);
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setRunPlayerId(_run_player_id);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
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
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				// 福禄寿 小结算 手牌展示中添加胡的牌
				if (GRR._chi_hu_card[i][0] != GameConstants.INVALID_VALUE) {
					cs.addItem(GRR._chi_hu_card[i][0]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE_FLS; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);
		// 是否是金币场
		// roomResponse.setIsGoldRoom(is_sys());
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public boolean handler_game_finish_fls(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
			istrustee[i] = false;
			////////////////////////////
			if (_playerStatus != null && _playerStatus[i] != null) {
				_playerStatus[i]._hu_card_count = 0;
			} else {
				logger.error("这个bug很诡异" + i);
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setRunPlayerId(_run_player_id);
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
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
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				// 福禄寿 小结算 手牌展示中添加胡的牌
				if (GRR._chi_hu_card[i][0] != GameConstants.INVALID_VALUE) {
					cs.addItem(GRR._chi_hu_card[i][0]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE_FLS; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);
		// 是否是金币场
		// roomResponse.setIsGoldRoom(is_sys());
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	/**
	 * @return
	 */
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setCreateName(this.getRoom_owner_name());
		room_info.setBankerPlayer(_banker_select);
		int beginLeftCard = GameConstants.CARD_COUNT_FLS_LX - 57;
		if (playerNumber != GameConstants.GAME_PLAYER) {
			beginLeftCard = GameConstants.CARD_COUNT_FLS_LX - 55;
		}
		if (GameConstants.GAME_TYPE_FLS_LX_TWENTY == _game_type_index) {
			beginLeftCard = GameConstants.CARD_COUNT_FLS_LX - 58;
		}

		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	public boolean handler_game_finish_fls_twenty(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
			istrustee[i] = false;
			////////////////////////////
			if (_playerStatus != null && _playerStatus[i] != null) {
				_playerStatus[i]._hu_card_count = 0;
			} else {
				logger.error("这个bug很诡异" + i);
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
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
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_FLS_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				// 福禄寿 小结算 手牌展示中添加胡的牌
				if (GRR._chi_hu_card[i][0] != GameConstants.INVALID_VALUE) {
					cs.addItem(GRR._chi_hu_card[i][0]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE_FLS; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

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
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index) {

		if (GameConstants.GAME_TYPE_FLS_LX == _game_type_index) {
			return analyse_chi_hu_card_fls(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		} else if (GameConstants.GAME_TYPE_FLS_LX_TWENTY == _game_type_index) {
			return analyse_chi_hu_card_fls_twenty(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type,
					seat_index);
		} else if (GameConstants.GAME_TYPE_FLS_LX_DP == _game_type_index) {
			return analyse_chi_hu_card_fls_dp(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
		}
		return 0;
	}

	/***
	 * 胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_fls_dp(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// cbCurrentCard一定不为0 !!!!!!!!!

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// if(_logic.get_card_count_by_index(cards_index)==0) {
		// chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);//满天飞
		// return GameConstants.WIK_CHI_HU;
		// }

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		int downScore = getDownScore(weaveItems, weaveCount);// 获取落地牌 分数
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false,
				cur_card);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 庄家的对家 没有 胡息限制--根据手牌数量为3 组合为0 判断 不需要管3人还是4人场
		boolean isZhuangDui = _logic.get_card_count_by_index(cbCardIndexTemp) == 3 && weaveCount == 0;

		boolean isMenQing = false;
		boolean isPengPengHu = false;

		if (isZhuangDui) {
			for (int i = 0; i < getMaxIndex(); i++) {
				if (cbCardIndexTemp[i] == 3) {
					isPengPengHu = true;
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_FLS_GUNGUN);// 3张一样自摸为“滚滚”
					} else {
						chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);// 接炮为“碰碰胡”
						isPengPengHu = true;
					}
					break;
				}
			}
		} else {
			// 牌型分析
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				// 碰碰和
				if (_logic.is_pengpeng_hu(analyseItem) && _logic.is_pengpeng_hu_down(weaveItems, weaveCount)) {
					chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);
					isPengPengHu = true;
					break;
				}

			}

			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				if (analyseItem.curCardEye) {
					chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);// 满天飞
					break;
				}
			}
		}

		boolean isCanJiePao = false;
		if (weaveCount == 0 && (card_type == GameConstants.HU_CARD_TYPE_ZIMO)) {// 自摸才算
			// 门清
			chiHuRight.opr_or(GameConstants.CHR_FLS_MENQING);
			isMenQing = true;
			isCanJiePao = true;
		}

		boolean isAnGang = _logic.is_an_gang(weaveItems, weaveCount);
		if (isAnGang && (card_type == GameConstants.HU_CARD_TYPE_ZIMO)) {
			chiHuRight.opr_or(GameConstants.CHR_FLS_MENQING);
			isMenQing = true;
			isCanJiePao = true;
		}

		if (_logic.get_card_count_by_index(cards_index) == 0) {// 手牌为0
			chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);// 满天飞

			isPengPengHu = _logic.is_pengpeng_hu_down(weaveItems, weaveCount);
			if (isPengPengHu) {
				chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);
			}

		}

		if (!isZhuangDui) {
			if (!isPengPengHu) {
				if (!isScoreEnough(analyseItemArray, downScore, cur_card, card_type)) {
					chiHuRight.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			// chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			// chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_FLS_GANGSHANGHUA);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or(GameConstants.CHR_FLS_GANGSHANGPAO);
		}

		// 可接炮的情况 1.门清 2.庄家的对家（只有两张牌） 3.碰碰胡
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			boolean hu = isMenQing || isZhuangDui || isPengPengHu || weaveCount == 0 || isCanJiePao || isAnGang;
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		return cbChiHuKind;
	}

	/***
	 * 胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_fls(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {
		// cbCurrentCard一定不为0 !!!!!!!!!

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// if(_logic.get_card_count_by_index(cards_index)==0) {
		// chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);//满天飞
		// return GameConstants.WIK_CHI_HU;
		// }

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		int downScore = getDownScore(weaveItems, weaveCount);// 获取落地牌 分数
		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false,
				cur_card);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 庄家的对家 没有 胡息限制--根据手牌数量为3 组合为0 判断 不需要管3人还是4人场
		boolean isZhuangDui = _logic.get_card_count_by_index(cbCardIndexTemp) == 3 && weaveCount == 0;

		boolean isMenQing = false;
		boolean isPengPengHu = false;

		if (isZhuangDui) {
			for (int i = 0; i < getMaxIndex(); i++) {
				if (cbCardIndexTemp[i] == 3) {
					isPengPengHu = true;
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_FLS_GUNGUN);// 3张一样自摸为“滚滚”
					} else {
						chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);// 接炮为“碰碰胡”
						isPengPengHu = true;
					}
					break;
				}
			}
		} else {
			// 牌型分析
			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				// 碰碰和
				if (_logic.is_pengpeng_hu(analyseItem) && _logic.is_pengpeng_hu_down(weaveItems, weaveCount)) {
					chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);
					isPengPengHu = true;
					break;
				}

			}

			for (int i = 0; i < analyseItemArray.size(); i++) {
				// 变量定义
				AnalyseItem analyseItem = analyseItemArray.get(i);
				if (analyseItem.curCardEye) {
					chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);// 满天飞
					break;
				}
			}
		}

		boolean isCanJiePao = false;
		if (weaveCount == 0 && (card_type == GameConstants.HU_CARD_TYPE_ZIMO)) {// 自摸才算
			// 门清
			chiHuRight.opr_or(GameConstants.CHR_FLS_MENQING);
			isMenQing = true;
			isCanJiePao = true;
		}

		boolean isAnGang = _logic.is_an_gang(weaveItems, weaveCount);
		if (isAnGang && (card_type == GameConstants.HU_CARD_TYPE_ZIMO)) {
			chiHuRight.opr_or(GameConstants.CHR_FLS_MENQING);
			isMenQing = true;
			isCanJiePao = true;
		}

		if (_logic.get_card_count_by_index(cards_index) == 0) {// 手牌为0
			chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);// 满天飞

			isPengPengHu = _logic.is_pengpeng_hu_down(weaveItems, weaveCount);
			if (isPengPengHu) {
				chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);
			}

		}

		if (!isZhuangDui) {
			if (!isPengPengHu) {
				if (!isScoreEnough(analyseItemArray, downScore, cur_card, card_type)) {
					chiHuRight.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			// chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			// chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_FLS_GANGSHANGHUA);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or(GameConstants.CHR_FLS_GANGSHANGPAO);
		}

		// 可接炮的情况 1.门清 2.庄家的对家（只有两张牌） 3.碰碰胡
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			boolean hu = isMenQing || isZhuangDui || isPengPengHu || weaveCount == 0 || isCanJiePao || isAnGang;
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		return cbChiHuKind;
	}

	/***
	 * 胡牌解析-20张
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_fls_twenty(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index) {
		// cbCurrentCard一定不为0 !!!!!!!!!

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克

		boolean hasJin = false;
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];

			if (cbCardIndexTemp[i] > 0) {
				int cbCardcolor = _logic.get_card_color(_logic.switch_to_card_data(i));
				if (cbCardcolor == 0 || cbCardcolor == 7) {
					hasJin = true;
				}
			}
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		int txd = _logic.is_ten_xiao_dui(cbCardIndexTemp, weaveItems, weaveCount);
		if (txd != GameConstants.WIK_NULL) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}
			chiHuRight.opr_or(txd);
			return GameConstants.WIK_CHI_HU;
		}

		List<AnalyseItemTwenty> analyseItemArray = new ArrayList<AnalyseItemTwenty>();
		int downScore = getDownScoreTwenty(weaveItems, weaveCount);// 获取落地牌 分数
		// 分析扑克
		boolean bValue = _logic.analyse_card_twenty(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, false,
				cur_card);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean isEnough = isScoreEnoughtwenty(analyseItemArray, downScore, cur_card, card_type);

		if (!isEnough) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}
			for (int i = 0; i < analyseItemArray.size(); i++) {
				AnalyseItemTwenty analyseItem = analyseItemArray.get(i);
				if (analyseItem.totalScore == 0 && !analyseItem.eyeDui && !hasJin) {
					chiHuRight.opr_or(GameConstants.CHR_FLS_HEI_HU);
					return GameConstants.WIK_CHI_HU;
				}
			}
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 牌型分析
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItemTwenty analyseItem = analyseItemArray.get(i);
			// 碰碰和
			ChiHuRight subChiHuRight = new ChiHuRight();
			analyseItem.subChiHuRight = subChiHuRight;

			if (_logic.is_ku_hu(analyseItem) && _logic.is_pengpeng_hu_down(weaveItems, weaveCount)
					&& analyseItem.totalScore > 0) {
				subChiHuRight.opr_or(GameConstants.CHR_FLS_KU_HU);
			}
			if (_logic.is_qing_hu(analyseItem, weaveItems, weaveCount, cur_card) && analyseItem.totalScore > 0) {
				subChiHuRight.opr_or(GameConstants.CHR_FLS_QING_HU);
			}
			if (analyseItem.totalScore == 11 || analyseItem.totalScore == 22 || analyseItem.totalScore == 33) {
				subChiHuRight.opr_or(GameConstants.CHR_FLS_KA_HU);
			}

			if (analyseItem.isDoubleCount) {
				int tempCount = analyseItem.totalScore - 1;// 2种算分
				if (tempCount == 11 || tempCount == 22 || tempCount == 33) {
					subChiHuRight.opr_or(GameConstants.CHR_FLS_KA_HU);
				}
			}

			if (analyseItem.totalScore >= 22) {
				subChiHuRight.opr_or(GameConstants.CHR_FLS_TAI_HU);
			}
			if (analyseItem.totalScore >= 33) {
				subChiHuRight.opr_or(GameConstants.CHR_FLS_ZHONG_HU);
			}
			if (_logic.is_hong_hu(analyseItem, weaveItems, weaveCount, cur_card) && analyseItem.totalScore > 0) {
				if (analyseItem.hong == 4) {
					subChiHuRight.opr_or(GameConstants.CHR_FLS_HONG_HU4);
				} else if (analyseItem.hong == 5) {
					subChiHuRight.opr_or(GameConstants.CHR_FLS_HONG_HU5);
				} else if (analyseItem.hong == 6) {
					subChiHuRight.opr_or(GameConstants.CHR_FLS_HONG_HU6);
				} else if (analyseItem.hong == 7) {
					subChiHuRight.opr_or(GameConstants.CHR_FLS_HONG_HU7);
				}

				subChiHuRight.opr_or(GameConstants.CHR_FLS_HONG_HU);
			}

			if (analyseItem.totalScore == 0 && !analyseItem.eyeDui && !hasJin) {
				subChiHuRight.opr_or(GameConstants.CHR_FLS_HEI_HU);
			}
		}

		int fan = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			AnalyseItemTwenty analyseItem = analyseItemArray.get(i);
			int wFanShu = _logic.get_chi_hu_action_rank_fls_twenty(analyseItem.subChiHuRight);
			if (wFanShu > fan) {
				fan = wFanShu;
				chiHuRight.set_empty();
				chiHuRight.copy(analyseItem.subChiHuRight);
				// analyseItemTwenty[seat_index] = analyseItem;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}
		return cbChiHuKind;
	}

	/**
	 * 获取落地的分数
	 * 
	 * @return
	 */
	private int getDownScore(WeaveItem weaveItems[], int weaveCount) {
		int score = 0;
		for (int i = 0; i < weaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				int j = 0;
				for (; j < 3; j++) {
					int card = weaveItem.getCenter_card();
					if (card > 0) {
						int cbCardcolor = _logic.get_card_color(card);
						if (cbCardcolor == 0 || cbCardcolor == 7) {// 只有“上大人”“福禄寿”两句话各算4胡息
							score += 4;
							break;
						}
					}
				}
				break;
			case GameConstants.WIK_PENG:
				if (weaveItem.center_card == 0x01 || weaveItem.center_card == 0x71) {
					score += 16;// 只算2个
				} else {
					score += 2;
				}
				break;
			case GameConstants.WIK_GANG:
			case GameConstants.WIK_ZHAO:
				if (weaveItem.center_card == 0x01 || weaveItem.center_card == 0x71) {
					score += 24;// 只算3个
				} else {
					score += 6;
				}
				break;
			}
		}
		return score;
	}

	/**
	 * 获取落地的分数
	 * 
	 * @return
	 */
	private int getDownScoreTwenty(WeaveItem weaveItems[], int weaveCount) {
		int score = 0;
		for (int i = 0; i < weaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				int j = 0;
				for (; j < 3; j++) {
					int card = weaveItem.getCenter_card();
					if (card > 0) {
						int cbCardcolor = _logic.get_card_color(card);
						if (cbCardcolor == 0 || cbCardcolor == 7) {// 只有“上大人”“福禄寿”两句话各算4胡息
							score += 4;
							break;
						}
					}
				}
				break;
			case GameConstants.WIK_PENG:
				if (weaveItem.center_card == 0x01 || weaveItem.center_card == 0x71) {
					score += 12;
				} else {
					score += 2;
				}
				break;
			case GameConstants.WIK_GANG:
			case GameConstants.WIK_ZHAO:
				if (weaveItem.center_card == 0x01 || weaveItem.center_card == 0x71) {
					score += 16;
				} else {
					score += 6;
				}
				break;
			}
		}
		return score;
	}

	/**
	 * 分数是否足够
	 * 
	 * @param analyseItemArray
	 * @return
	 */
	private boolean isScoreEnough(List<AnalyseItem> analyseItemArray, int downScore, int cur_card, int card_type) {
		if (downScore >= GameConstants.MIN_SCORE_FLS) {
			return true;
		}
		boolean isZimo = card_type == GameConstants.HU_CARD_TYPE_ZIMO
				|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI;
		int bestScore = 0;
		boolean handJin = false;// 手里是否有金
		boolean hasKan = false;// 手里是否有坎

		for (AnalyseItem analyseItem : analyseItemArray) {// 计算手牌分数
			int length = analyseItem.cbWeaveKind.length;
			int score = 0;
			boolean isInchi = isInChi(analyseItem, cur_card);
			for (int i = 0; i < length; i++) {
				int kind = analyseItem.cbWeaveKind[i];
				switch (kind) {
				case GameConstants.WIK_LEFT:
				case GameConstants.WIK_CENTER:
				case GameConstants.WIK_RIGHT:
					int j = 0;
					for (; j < 3; j++) {
						int cardValue = analyseItem.cbCardData[i][j];
						if (cardValue > 0) {
							int cbCardcolor = _logic.get_card_color(cardValue);
							if (cbCardcolor == 0 || cbCardcolor == 7) {// 只有“上大人”“福禄寿”两句话各算4胡息
								score += 4;
								handJin = true;
								break;
							}
						}
					}
					break;
				case GameConstants.WIK_PENG:
					if (analyseItem.cbCenterCard[i] == 0x01 || analyseItem.cbCenterCard[i] == 0x71) {
						score += 24;
					} else {
						if (analyseItem.cbCenterCard[i] == cur_card && !isZimo
								&& !_logic.isInCardEye(analyseItem, cur_card) && !isInchi) {
							score += 2;// 如果是接炮的 碰 算2分
						} else {
							score += 3;
							hasKan = true;
						}
					}
					break;
				case GameConstants.WIK_GANG:
				case GameConstants.WIK_ZHAO:
					if (analyseItem.cbCenterCard[i] == 0x01 || analyseItem.cbCenterCard[i] == 0x71) {
						score += 32;
					} else {
						score += 6;
					}
					break;
				}
			}

			if (score > bestScore) {
				bestScore = score;
			}

			if (downScore + score >= GameConstants.MIN_SCORE_FLS) {
				return true;
			}

			boolean full = isEyeScoreFull(analyseItem, handJin, hasKan, score, downScore, cur_card, card_type);

			if (full) {
				return full;
			}

		}
		return false;
	}

	/**
	 * 分数是否足够
	 * 
	 * @param analyseItemArray
	 * @return
	 */
	private boolean isScoreEnoughtwenty(List<AnalyseItemTwenty> analyseItemArray, int downScore, int cur_card,
			int card_type) {
		boolean isEnough = false;
		boolean isZimo = card_type == GameConstants.HU_CARD_TYPE_ZIMO;
		for (AnalyseItemTwenty analyseItem : analyseItemArray) {// 计算手牌分数
			int length = analyseItem.cbWeaveKind.length;
			int score = 0;

			boolean isDoubleCount = false;// 是否涉及多种算分

			boolean isInchi = isInChiTwenty(analyseItem, cur_card);
			for (int i = 0; i < length; i++) {
				int kind = analyseItem.cbWeaveKind[i];
				switch (kind) {
				case GameConstants.WIK_LEFT:
				case GameConstants.WIK_CENTER:
				case GameConstants.WIK_RIGHT:
					int j = 0;
					for (; j < 3; j++) {
						int cardValue = analyseItem.cbCardData[i][j];
						if (cardValue > 0) {
							int cbCardcolor = _logic.get_card_color(cardValue);
							if (cbCardcolor == 0 || cbCardcolor == 7) {// 只有“上大人”“福禄寿”两句话各算4胡息
								score += 4;
								break;
							}
						}
					}
					break;
				case GameConstants.WIK_PENG:
					if (analyseItem.cbCenterCard[i] == 0x01 || analyseItem.cbCenterCard[i] == 0x71) {
						score += 12;
					} else {
						if (analyseItem.cbCenterCard[i] == cur_card && !isZimo && !isInchi) {
							score += 2;// 如果是接炮的 碰 算2分
						} else {
							if (analyseItem.cbCenterCard[i] == cur_card && !isZimo && isInchi) {// 不是自摸
																								// 并且
																								// 当前牌在吃里面--也可以当作2分算
								isDoubleCount = true;// 总分减一分试试
							}
							score += 3;
						}
					}
					break;
				case GameConstants.WIK_GANG:
				case GameConstants.WIK_ZHAO:
					if (analyseItem.cbCenterCard[i] == 0x01 || analyseItem.cbCenterCard[i] == 0x71) {
						score += 16;
					} else {
						score += 6;
					}
					break;
				}
			}
			int eyeScore = getEyeScoreTwenty(analyseItem, cur_card);
			int total = downScore + score + eyeScore;
			analyseItem.totalScore = total;
			analyseItem.isDoubleCount = isDoubleCount;
			if (total < GameConstants.MIN_SCORE_FLS) {
				continue;
			}
			isEnough = true;

		}
		return isEnough;
	}

	private int getEyeScoreTwenty(AnalyseItemTwenty analyseItem, int cur_card) {
		if (analyseItem.eyeDui) {
			if (analyseItem.cbCardEye[0] == 0x01 || analyseItem.cbCardEye[1] == 0x71) {
				return 8;
			}
		} else {
			for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
				if (analyseItem.cbCardEye[i] == 0x01 || analyseItem.cbCardEye[i] == 0x71
						|| analyseItem.cbCardEye[i] == 0x01 || analyseItem.cbCardEye[i] == 0x71) {
					return 4;
				}
			}
		}
		return 0;
	}

	private boolean isInChi(AnalyseItem analyseItem, int cur_card) {
		boolean isInChi = false;
		int length = analyseItem.cbWeaveKind.length;
		for (int i = 0; i < length; i++) {
			int kind = analyseItem.cbWeaveKind[i];
			switch (kind) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					int cardValue = analyseItem.cbCardData[i][j];
					if (cur_card == cardValue) {
						isInChi = true;
						break;
					}
				}
			}
		}
		return isInChi;
	}

	private boolean isInChiTwenty(AnalyseItemTwenty analyseItem, int cur_card) {
		boolean isInChi = false;
		int length = analyseItem.cbWeaveKind.length;
		for (int i = 0; i < length; i++) {
			int kind = analyseItem.cbWeaveKind[i];
			switch (kind) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
				for (int j = 0; j < 3; j++) {
					int cardValue = analyseItem.cbCardData[i][j];
					if (cur_card == cardValue) {
						isInChi = true;
						break;
					}
				}
			}
		}

		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
			if (analyseItem.cbCardEye[i] == cur_card) {
				isInChi = true;
				break;
			}
		}

		return isInChi;
	}

	/***
	 * @return
	 */
	private boolean isEyeScoreFull(AnalyseItem analyseItem, boolean hasJin, boolean hasKan, int handScore,
			int downScore, int cur_card, int card_type) {
		// logger.warn("牌眼 索引 ====" + Arrays.toString(analyseItem.cbCardEye));
		// 把牌眼复制一份
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
			if (analyseItem.cbCardEye[i] == 0)
				continue;
			int index = _logic.switch_to_card_index(analyseItem.cbCardEye[i]);
			cbCardIndexTemp[index] += 1;
		}

		boolean isZimo = card_type == GameConstants.HU_CARD_TYPE_ZIMO
				|| card_type == GameConstants.HU_CARD_TYPE_GANG_KAI;
		// 判断成句的牌眼
		int eyeScore = 0;
		if (analyseItem.eyeKind == GameConstants.WIK_PENG) {
			int index = _logic.switch_to_card_index(analyseItem.eyeCenterCard);
			if (analyseItem.eyeCenterCard == 0x01 || analyseItem.eyeCenterCard == 0x71) {
				eyeScore += 24;
			} else {
				if (cur_card == analyseItem.eyeCenterCard && !isZimo) {
					eyeScore += 2;// 如果是接炮的 碰 算2分
				} else {
					eyeScore += 3;
					hasKan = true;
				}
			}
			cbCardIndexTemp[index] -= 3;// 将算了分的牌眼去掉
		} else if (analyseItem.eyeKind == GameConstants.WIK_LEFT) {
			if (analyseItem.eyeCenterCard == 0x01 || analyseItem.eyeCenterCard == 0x71) {// 只有“上大人”“福禄寿”两句话各算4胡息
				eyeScore += 4;
				hasJin = true;
			}
			int index = _logic.switch_to_card_index(analyseItem.eyeCenterCard);
			cbCardIndexTemp[index] -= 1;// 将算了分的牌眼去掉
			cbCardIndexTemp[index + 1] -= 1;
			cbCardIndexTemp[index + 2] -= 1;
		}

		int count = _logic.get_card_count_by_index(cbCardIndexTemp);

		boolean hasCountJinKan = false;// 是否 抵金 抵坎了
		boolean isCountKan = false;
		for (int i = 0; i < cbCardIndexTemp.length; i++) {
			if (cbCardIndexTemp[i] == 0)
				continue;
			int data = _logic.switch_to_card_data(i);
			if (cbCardIndexTemp[i] == 1) {// 单牌

				if ((data == 0x71 || data == 0x01)) {// 单牌是 上福
					if (count == 1) {
						eyeScore += 8;// 上 福--单调
						cbCardIndexTemp[i] -= 1;
						continue;
					}

					if (((i + 1) < cbCardIndexTemp.length && cbCardIndexTemp[i + 1] != 0)) {

						if (cbCardIndexTemp[i + 1] == 3) {
							eyeScore += 8;// 上

							if (cur_card == _logic.switch_to_card_data(i + 1) && !isZimo) {
								eyeScore += 2;// 如果是接炮的 碰 算2分
							} else {
								eyeScore += 3;
								hasKan = true;
							}
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 3;
						} else {
							// 有上 福 单个大 人 禄 寿 也算4福
							eyeScore += 4;//
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 1;
							hasJin = true;
						}
						continue;
					}

					if (((i + 2) < cbCardIndexTemp.length && cbCardIndexTemp[i + 2] != 0)) {
						// 有上 福 单个大 人 禄 寿 也算4福
						if (cbCardIndexTemp[i + 2] == 3) {
							eyeScore += 8;// 上
							if (cur_card == _logic.switch_to_card_data(i + 2) && !isZimo) {
								eyeScore += 2;// 如果是接炮的 碰 算2分
							} else {
								eyeScore += 3;
								hasKan = true;
							}
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 2] -= 3;
						} else {
							// 有上 福 单个大 人 禄 寿 也算4福
							eyeScore += 4;//
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 2] -= 1;
							hasJin = true;
						}
						continue;

					}

				}

				if (count == 1) {// 单调
									// 大、人、禄、寿、单个子也可以算4胡息
					if (data == 0x72 || data == 0x73 || data == 0x02 || data == 0x03) {// 大人
																						// 禄寿
						eyeScore += 4;// 分数肯定够
						cbCardIndexTemp[i] -= 1;
						// hasCountJinKan = true;
						continue;
					}
				}

				if (data == 0x72 || data == 0x02) {
					if (((i + 1) < cbCardIndexTemp.length && cbCardIndexTemp[i + 1] != 0)) {// 抵金了不抵坎

						if (cbCardIndexTemp[i + 1] == 3) {
							eyeScore += 4;// 大 禄
							if (cur_card == _logic.switch_to_card_data(i + 1) && !isZimo) {
								eyeScore += 2;// 如果是接炮的 碰 算2分
							} else {
								eyeScore += 3;
								hasKan = true;
							}
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 3;
							hasCountJinKan = true;
							continue;
						}

						if (isCountKan) {// 抵坎不抵金
							eyeScore += 1;
							hasCountJinKan = true;
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 1;
							continue;
						}

						if (!hasCountJinKan) {
							eyeScore += 4;
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 1;
							hasCountJinKan = true;
						}
						continue;// 只抵一次
					}
				}

			} else if (cbCardIndexTemp[i] == 2) {// 对子只算一个
				if (data == 0x71 || data == 0x01) {
					eyeScore += 16;
					cbCardIndexTemp[i] -= 2;
				} else if (data == 0x72 || data == 0x02) {
					if (((i + 1) < cbCardIndexTemp.length && cbCardIndexTemp[i + 1] != 0)) {// 抵金了不抵坎
						if (isCountKan) {// 抵坎不抵金
							eyeScore += 1;
							hasCountJinKan = true;
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 1;
							continue;
						}

						if (!hasCountJinKan) {
							eyeScore += 4;
							cbCardIndexTemp[i] -= 1;
							cbCardIndexTemp[i + 1] -= 1;
							hasCountJinKan = true;
						}
						continue;// 只抵一次
					} else if (!isCountKan) {
						if (!hasCountJinKan) {// 抵坎
							eyeScore += 3;
							hasCountJinKan = true;
							isCountKan = true;
							cbCardIndexTemp[i] -= 2;
						}
					}
				} else {
					if (!isCountKan) {
						if (!hasCountJinKan) {// 抵坎
							eyeScore += 3;
							hasCountJinKan = true;
							isCountKan = true;
							cbCardIndexTemp[i] -= 2;
						}
					}
				}

			} else if (cbCardIndexTemp[i] == 3) {// 对子只算一个
				if (data == 0x71 || data == 0x01) {
					eyeScore += 24;
					cbCardIndexTemp[i] -= 3;
				} else if (data == 0x72 || data == 0x02) {
					if (((i + 1) < cbCardIndexTemp.length && cbCardIndexTemp[i + 1] != 0)) {// 抵金了不抵坎
						eyeScore += 4;// 大 禄
						if (cur_card == _logic.switch_to_card_data(i) && !isZimo) {
							eyeScore += 2;// 如果是接炮的 碰 算2分
						} else {
							eyeScore += 3;
							hasKan = true;
						}
						cbCardIndexTemp[i] -= 3;
						cbCardIndexTemp[i + 1] -= 1;
						hasCountJinKan = true;
					} else {
						eyeScore += 3;
						cbCardIndexTemp[i] -= 3;
					}
				} else {
					eyeScore += 3;
					cbCardIndexTemp[i] -= 3;
				}
			} else if (cbCardIndexTemp[i] == 4) {// 对子只算一个
				if (data == 0x71 || data == 0x01) {
					eyeScore += 32;
					cbCardIndexTemp[i] -= 4;
				} else if (data == 0x72 || data == 0x73 || data == 0x02 || data == 0x03) {
					eyeScore += 4;// 大 禄
					eyeScore += 3;// 坎
					cbCardIndexTemp[i] -= 4;
					hasCountJinKan = true;
					continue;
				} else {
					eyeScore += 3;
					cbCardIndexTemp[i] -= 3;
				}
			}
		}

		// if (hasJin) {// 看看能不能抵金 （大人或者 禄寿）
		// for (int i = 0; i < cbCardIndexTemp.length; i++) {
		// if (cbCardIndexTemp[i] == 0 || cbCardIndexTemp[i] == 2)// 过滤掉
		// // 成对子的
		// // 过滤掉
		// continue;
		// int data = _logic.switch_to_card_data(i);
		//
		//
		// }
		// }
		int max_score = GameConstants.MIN_SCORE_FLS;// 手中有“上福”
		if (handScore + downScore + eyeScore >= max_score) {
			// logger.warn("table roomID ==" + getRoom_id() + " 手牌分数===" +
			// handScore + " 落地分数==" + downScore + " 牌眼分数=="
			// + eyeScore + "牌眼值==" + Arrays.toString(analyseItem.cbCardEye));
			return true;
		}
		return false;
	}

	/**
	 * 福禄寿获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_fls_ting_card_dp(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = getMaxIndex();

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls_dp(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 福禄寿获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_fls_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = getMaxIndex();

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 福禄寿获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_fls_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int seat_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = getMaxIndex();

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls_twenty(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_fls(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();

		if (!isZhuangDui(chi_seat_index)) {
			// 长沙麻将吃操作 转转麻将不能吃
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}
		}

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			if (!isZhuangDui(i)) {
				if (_playerStatus[i].lock_huan_zhang() == false) {
					//// 碰牌判断
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {

						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}
			}
			if (_playerStatus[i].lock_huan_zhang() == false) {// 不能连续杠
				int left = getTablePlayerNumber();
				// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
				if (GRR._left_card_count >= left) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						if (_playerStatus[i].lock_huan_zhang() == false) {
							playerStatus.add_action(GameConstants.WIK_ZHAO);
							playerStatus.add_zhao(card, seat_index, 1);// 加上补张
						}

						// 剩一张为海底
						if (GRR._left_card_count > left) {
							boolean is_ting = false;

							boolean can_gang = false;
							if (is_ting == true) {
								can_gang = true;
							} else {
								// 把可以杠的这张牌去掉。看是不是听牌
								int bu_index = _logic.switch_to_card_index(card);
								int save_count = GRR._cards_index[i][bu_index];
								GRR._cards_index[i][bu_index] = 0;

								int cbWeaveIndex = GRR._weave_count[i];

								GRR._weave_items[i][cbWeaveIndex].public_card = 1;
								GRR._weave_items[i][cbWeaveIndex].center_card = card;
								GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
								GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
								GRR._weave_count[i]++;

								can_gang = this.is_fls_ting_card(GRR._cards_index[i], GRR._weave_items[i],
										GRR._weave_count[i]);

								// 把牌加回来
								GRR._cards_index[i][bu_index] = save_count;
								GRR._weave_count[i] = cbWeaveIndex;

							}

							if (can_gang == true) {
								playerStatus.add_action(GameConstants.WIK_GANG);
								playerStatus.add_gang(card, seat_index, 1);// 加上杠
								bAroseAction = true;
							}
						}
					}
				}
			}

			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_fls_dp(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();

		if (!isZhuangDui(chi_seat_index)) {
			// 长沙麻将吃操作 转转麻将不能吃
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}
		}

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			if (!isZhuangDui(i)) {
				if (_playerStatus[i].lock_huan_zhang() == false) {
					//// 碰牌判断
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {

						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}
			}
			if (_playerStatus[i].lock_huan_zhang() == false) {// 不能连续杠
				int left = getTablePlayerNumber();
				// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
				if (GRR._left_card_count >= left) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						if (_playerStatus[i].lock_huan_zhang() == false) {
							playerStatus.add_action(GameConstants.WIK_ZHAO);
							playerStatus.add_zhao(card, seat_index, 1);// 加上补张
						}

						// 剩一张为海底
						if (GRR._left_card_count > left) {
							boolean is_ting = false;

							boolean can_gang = false;
							if (is_ting == true) {
								can_gang = true;
							} else {
								// 把可以杠的这张牌去掉。看是不是听牌
								int bu_index = _logic.switch_to_card_index(card);
								int save_count = GRR._cards_index[i][bu_index];
								GRR._cards_index[i][bu_index] = 0;

								int cbWeaveIndex = GRR._weave_count[i];

								GRR._weave_items[i][cbWeaveIndex].public_card = 1;
								GRR._weave_items[i][cbWeaveIndex].center_card = card;
								GRR._weave_items[i][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
								GRR._weave_items[i][cbWeaveIndex].provide_player = seat_index;
								GRR._weave_count[i]++;

								can_gang = this.is_fls_ting_card_dp(GRR._cards_index[i], GRR._weave_items[i],
										GRR._weave_count[i]);

								// 把牌加回来
								GRR._cards_index[i][bu_index] = save_count;
								GRR._weave_count[i] = cbWeaveIndex;

							}

							if (can_gang == true) {
								playerStatus.add_action(GameConstants.WIK_GANG);
								playerStatus.add_gang(card, seat_index, 1);// 加上杠
								bAroseAction = true;
							}
						}
					}
				}
			}

			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_fls_twenty(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();

		if (!isZhuangDui(chi_seat_index)) {
			// 长沙麻将吃操作 转转麻将不能吃
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
				if ((action & GameConstants.WIK_LEFT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
				}

				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}
		}

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			if (!isZhuangDui(i)) {
				if (_playerStatus[i].lock_huan_zhang() == false) {
					//// 碰牌判断
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}
			}
			if (_playerStatus[i].lock_huan_zhang() == false) {// 不能连续杠
				int left = getTablePlayerNumber();
				// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_ZHAO);
						playerStatus.add_zhao(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					}
				}
			}
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}
		return true;

	}

	// 检查杠牌,有没有胡的
	public boolean estimate_gang_respond_fls(int seat_index, int card, int _action) {
		// 变量定义
		boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张
																			// 不算抢杠胡

		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			// if(playerStatus.is_chi_hu_round()){
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					GameConstants.HU_CARD_TYPE_QIANGGANG, i);

			// 结果判断
			if (action != 0) {
				if (isGang) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
				}
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
				bAroseAction = true;
			}
			// }
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	// 检查长沙麻将,杠牌
	public boolean estimate_gang_fls_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];
		// playerStatus.clean_action();

		if (!isZhuangDui(seat_index)) {
			if (playerStatus.lock_huan_zhang() == false) {
				//// 碰牌判断
				// action = _logic.check_peng(GRR._cards_index[seat_index],
				// card);
				// if (action != 0) {
				// playerStatus.add_action(action);
				// playerStatus.add_peng(card, seat_index);
				//
				// bAroseAction = true;
				// }
			}
		}

		if (check_chi) {
			int chi_seat_index = (provider + 1) % getTablePlayerNumber();

			if (!isZhuangDui(chi_seat_index)) {
				if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
					// // 长沙麻将吃操作 转转麻将不能吃
					// action =
					// _logic.check_chi(GRR._cards_index[chi_seat_index], card);
					// if ((action & GameConstants.WIK_LEFT) != 0) {
					// _playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
					// _playerStatus[chi_seat_index].add_chi(card,
					// GameConstants.WIK_LEFT, seat_index);
					// }
					// if ((action & GameConstants.WIK_CENTER) != 0) {
					// _playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
					// _playerStatus[chi_seat_index].add_chi(card,
					// GameConstants.WIK_CENTER, seat_index);
					// }
					// if ((action & GameConstants.WIK_RIGHT) != 0) {
					// _playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
					// _playerStatus[chi_seat_index].add_chi(card,
					// GameConstants.WIK_RIGHT, seat_index);
					// }
					//
					// // 结果判断
					// if (_playerStatus[chi_seat_index].has_action()) {
					// bAroseAction = true;
					// }
				}
			}

		}
		int left = getTablePlayerNumber();
		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > left && _playerStatus[seat_index].lock_huan_zhang() == false) {
			// if
			// (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)]
			// == 3) {
			// if (_playerStatus[seat_index].lock_huan_zhang() == false) {
			// playerStatus.add_action(GameConstants.WIK_ZHAO);
			// playerStatus.add_zhao(card, provider, 1);// 加上补涨
			// }
			//
			// if (GRR._left_card_count > left) {
			// boolean is_ting = false;
			// boolean can_gang = false;
			// if (is_ting == true) {
			// can_gang = true;
			// } else {
			// // 把可以杠的这张牌去掉。看是不是听牌
			// int bu_index = _logic.switch_to_card_index(card);
			// int save_count = GRR._cards_index[seat_index][bu_index];
			// GRR._cards_index[seat_index][bu_index] = 0;
			//
			// int cbWeaveIndex = GRR._weave_count[seat_index];
			//
			// GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
			// GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
			// GRR._weave_items[seat_index][cbWeaveIndex].weave_kind =
			// GameConstants.WIK_GANG;// 接杠
			// GRR._weave_items[seat_index][cbWeaveIndex].provide_player =
			// provider;
			// GRR._weave_count[seat_index]++;
			//
			// can_gang = this.is_fls_ting_card(GRR._cards_index[seat_index],
			// GRR._weave_items[seat_index],
			// GRR._weave_count[seat_index]);
			//
			// GRR._weave_count[seat_index] = cbWeaveIndex;
			// GRR._cards_index[seat_index][bu_index] = save_count;
			// }
			//
			// if (can_gang == true) {
			//
			// playerStatus.add_action(GameConstants.WIK_GANG);
			// playerStatus.add_gang(card, provider, 1);// 加上杠
			//
			// }
			// }
			//
			// bAroseAction = true;
			// }

		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		// chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_GANG_PAO, seat_index);

		// 结果判断
		if (action != 0) {
			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			}

			bAroseAction = true;
		}

		return bAroseAction;
	}

	// 检查长沙麻将,杠牌
	public boolean estimate_gang_fls_respond_dp(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		playerStatus = _playerStatus[seat_index];
		// playerStatus.clean_action();

		if (!isZhuangDui(seat_index)) {
			if (playerStatus.lock_huan_zhang() == false) {
				//// 碰牌判断
				// action = _logic.check_peng(GRR._cards_index[seat_index],
				// card);
				// if (action != 0) {
				// playerStatus.add_action(action);
				// playerStatus.add_peng(card, seat_index);
				//
				// bAroseAction = true;
				// }
			}
		}

		if (check_chi) {
			int chi_seat_index = (provider + 1) % getTablePlayerNumber();

			if (!isZhuangDui(chi_seat_index)) {
				if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				}
			}

		}
		int left = getTablePlayerNumber();
		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > left && _playerStatus[seat_index].lock_huan_zhang() == false) {

		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		// chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_GANG_PAO, seat_index);

		// 结果判断
		if (action != 0) {
			if (_playerStatus[seat_index].has_chi_hu() == false) {
				_playerStatus[seat_index].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[seat_index].add_chi_hu(card, provider);// 吃胡的组合
			}

			bAroseAction = true;
		}

		return bAroseAction;
	}

	/**
	 * 创建房间
	 * 
	 * @return
	 */
	public boolean handler_create_room(Player player, int type, int maxNumber) {
		this.setCreate_type(type);
		this.setCreate_player(player);
		// 代理开房
		if (type == GameConstants.CREATE_ROOM_PROXY) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
					TimeUnit.MINUTES);

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(maxNumber);
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			// 发送进入房间
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_CREATE_RROXY_ROOM_SUCCESS);
			load_room_info_data(roomResponse);
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
			PlayerServiceImpl.getInstance().send(player, responseBuilder.build());
			return true;
		}

		// c成功
		get_players()[0] = player;
		player.set_seat_index(0);

		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setIsGoldRoom(is_sys());
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		return send_response_to_player(player.get_seat_index(), roomResponse);
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		// TODO Auto-generated method stub
		handler_request_trustee(_seat_index, true);

	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {

		if (_cur_round == 0 && !has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;

				// logger.error("tarplayer
				// ip=="+tarplayer.getAccount_ip()+"player
				// ip=="+player.getAccount_ip());
				if (player.getAccount_id() == tarplayer.getAccount_id())
					continue;
				if ((!is_sys()) && StringUtils.isNotEmpty(tarplayer.getAccount_ip())
						&& StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					player.setRoom_id(0);// 把房间信息清除--
					send_error_notify(player, 1, "不允许相同ip进入", 0);
					return false;
				}
			}
		}

		int seat_index = GameConstants.INVALID_SEAT;

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX) || is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			if (is_sys()) {
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					if (get_players()[i] == null) {
						get_players()[i] = player;
						seat_index = i;
						break;
					}
				}
			} else {
				if (playerNumber == 0) {// 未开始 才分配位置
					for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
						if (get_players()[i] == null) {
							get_players()[i] = player;
							seat_index = i;
							break;
						}
					}
				}
			}

		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			for (int i = 0; i < GameConstants.GAME_PLAYER - 1; i++) {
				if (get_players()[i] == null) {
					get_players()[i] = player;
					seat_index = i;
					break;
				}
			}
		}

		// if (seat_index == GameConstants.INVALID_SEAT &&
		// player.get_seat_index() != GameConstants.INVALID_SEAT) {
		// Player tarPlayer = get_players()[player.get_seat_index()];
		// if (tarPlayer.getAccount_id() == player.getAccount_id()) {
		// seat_index = player.get_seat_index();
		// }
		// }

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			send_error_notify(player, 1, "游戏已经开始", 0);
			return false;
		}

		check_if_kick_unready_player();

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setIsGoldRoom(is_sys());
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		roomResponse.setIsGoldRoom(is_sys());
		send_response_to_other(player.get_seat_index(), roomResponse);

		// 同步数据

		// ========同步到中心========
		RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
				getRoom_id() + "", RoomRedisModel.class);
		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		// 写入redis
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
		redisResponseBuilder.setRsResponseType(RsResponseType.ACCOUNT_UP);
		//
		RsAccountResponse.Builder rsAccountResponseBuilder = RsAccountResponse.newBuilder();
		rsAccountResponseBuilder.setAccountId(player.getAccount_id());
		rsAccountResponseBuilder.setRoomId(getRoom_id());
		//
		redisResponseBuilder.setRsAccountResponse(rsAccountResponseBuilder);
		RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(), ERedisTopicType.topicAll);

		return true;
	}

	// 玩家进入房间
	@Override
	public boolean handler_reconnect_room(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setIsGoldRoom(is_sys());
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		//
		this.load_player_info_data(roomResponse);
		this.load_room_info_data(roomResponse);

		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_other(player.get_seat_index(), roomResponse);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.cai.common.domain.Room#handler_requst_open_less(com.cai.common.domain
	 * .Player, boolean)
	 */
	@Override
	public boolean handler_requst_open_less(Player player, boolean openThree) {
		if (GameConstants.GS_MJ_FREE != _game_status) {
			return false;
		}
		// 已经开局 返回
		if (this._cur_round != 0) {
			return false;
		}

		if (_player_ready[player.get_seat_index()] == 0) {
			return false;
		}

		int less = openThree ? 1 : 0;
		_player_open_less[player.get_seat_index()] = less;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(i, roomResponse2);
		}

		int count = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (this.get_players()[i] != null) {
				count++;
			}
		}
		// 牌桌不是3人
		if (count != (GameConstants.GAME_PLAYER - 1)) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}

		if (openLess == readys && readys == GameConstants.GAME_PLAYER - 1) {
			playerNumber = readys;
			handler_game_start();

			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

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
		_player_open_less[seat_index] = 0;

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
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
					.get(5006);
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

	public boolean check_if_kick_unready_player() {
		if (!is_sys())
			return false;
		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule == null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (_player_ready[i] == 1) {
					_kick_schedule = GameSchedule.put(new KickRunnable(getRoom_id()),
							GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
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
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
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

		int number = playerNumber == 0 ? GameConstants.GAME_PLAYER : playerNumber;
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			number = playerNumber == 0 ? GameConstants.GAME_PLAYER - 1 : playerNumber;
		}
		for (int i = 0; i < number; i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}
			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
				return false;
			}
		}
		if (playerNumber == 0) {
			playerNumber = GameConstants.GAME_PLAYER;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			playerNumber = GameConstants.GAME_PLAYER - 1;
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

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

		if (is_sys())
			return true;
		return handler_player_ready(seat_index, false);

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
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count,
			int b_out_card) {
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
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
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
	 * @param account_id
	 * @return
	 */
	/**
	 * 释放
	 */
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	public boolean process_flush_time() {
		long old_flush = super.getLast_flush_time();
		setLast_flush_time(System.currentTimeMillis());
		long new_flush = super.getLast_flush_time();

		// 线程安全，先不要开放，加好锁后再处理
		// //大于10分钟通知redis
		// if(new_flush - old_flush > 1000L*60*10){
		// RoomRedisModel roomRedisModel =
		// SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
		// super.getRoom_id()+"", RoomRedisModel.class);
		// if(roomRedisModel!=null){
		// roomRedisModel.setLast_flush_time(System.currentTimeMillis());
		// //写入redis
		// SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM,
		// super.getRoom_id()+"", roomRedisModel);
		// }
		// }

		return true;
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)|| this.is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list,
					1, GameConstants.INVALID_SEAT, operate_card, true);
		} else if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list,
					1, GameConstants.INVALID_SEAT, operate_card, true);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_FLS_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 福禄寿
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_fls(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT, operate_card[0], true);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[getMaxIndex()];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,
				GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		if (isZimo) {
			_player_result.zi_mo_count[_seat_index]++;
		} else {
			_player_result.jie_pao_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_MENQING)).is_empty()) {
			_player_result.men_qing[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_PENGPENGHU)).is_empty()) {
			_player_result.peng_peng_hu[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GANGSHANGHUA)).is_empty()) {
			_player_result.gang_shang_hua[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HAIDI)).is_empty()) {
			_player_result.hai_di[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GUNGUN)).is_empty()) {
			_player_result.gun_gun[_seat_index]++;
		}
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	public void countChiHuTimesTwenty(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		boolean flag = false;
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_QING_HU)).is_empty()) {
			_player_result.qing[_seat_index]++;
			flag = true;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_KA_HU)).is_empty()) {
			_player_result.ka[_seat_index]++;
			flag = true;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_TAI_HU)).is_empty()) {
			_player_result.tai[_seat_index]++;
			flag = true;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_KU_HU)).is_empty()) {
			_player_result.ku[_seat_index]++;
			flag = true;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HEI_HU)).is_empty()) {
			_player_result.hei[_seat_index]++;
			flag = true;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU)).is_empty()) {
			_player_result.hong[_seat_index]++;
			flag = true;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_SHI_DUI)).is_empty()) {
			_player_result.shidui[_seat_index]++;
			flag = true;
		}

		if (!flag)
			_player_result.zi_mo_count[_seat_index]++;

	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_fls(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_fls(chr);// 番数
		countCardTypefls(chr, seat_index);

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		float lChiHuScore = wFanShu;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				// 跑
				s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;
			//
			// int addScore = _logic.is_an_gang(GRR._weave_items[provide_index],
			// GRR._weave_count[provide_index])
			// || GRR._weave_count[provide_index] == 0 ? 1 : 0;
			if (!(chr.opr_and(GameConstants.CHR_FLS_GANGSHANGPAO)).is_empty()) {// 杠上炮
																				// 赢多少
																				// 输多少
				int total = 0;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == provide_index) {
						continue;
					}
					total += lChiHuScore;
					// 跑
					total += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
							+ (_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index]));

					// total+=addScore;
				}
				// 胡牌分
				GRR._game_score[provide_index] -= total;
				GRR._game_score[seat_index] += total;
			} else {
				s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

				// 跑和呛
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			}

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	private void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_QING_HU)).is_empty()) {
				wFanShu += 1;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qing, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_KA_HU)).is_empty()) {
				wFanShu += 1;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.ka, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_TAI_HU)).is_empty()) {
				wFanShu += 1;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.tai, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_KU_HU)).is_empty()) {
				wFanShu += 5;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.ku, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_ZHONG_HU)).is_empty()) {
				wFanShu += 6;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.zhong, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HEI_HU)).is_empty()) {
				wFanShu += 4;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hei, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_SHI_DUI)).is_empty()) {
				wFanShu += 10;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.shidui, "", 0, 0l,
						this.getRoom_id());
			}

			boolean hasHong = false;
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU7)).is_empty()) {
				wFanShu += 7;
				hasHong = true;
			} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU6)).is_empty()) {
				wFanShu += 6;
				hasHong = true;
			} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU5)).is_empty()) {
				wFanShu += 5;
				hasHong = true;
			} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU4)).is_empty()) {
				wFanShu += 4;
				hasHong = true;
			} else if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HONG_HU)).is_empty()) {
				if (!hasHong) {
					wFanShu += 3;
				}
				hasHong = true;
			}

			if (hasHong)
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hong, "", 0, 0l,
						this.getRoom_id());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void countCardTypefls(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_MENQING)).is_empty()
					&& !(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				// 门清自摸3分（接炮算小胡，放炮者出1分）；
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.zimomq, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_PENGPENGHU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.pengpenghu, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_MANTIAN_FEI)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mantianfei, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GANGSHANGHUA)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.gangshanghua, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GANGSHANGPAO)).is_empty()) {
				wFanShu = 4;
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_HAIDI)).is_empty()
					&& !(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.haidi, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_FLS_GUNGUN)).is_empty()) {
				wFanShu = 5;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_fls_twenty(int seat_index, int provide_index, int operate_card,
			boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_fls_twenty(chr);// 番数
		countCardType(chr, seat_index);

		int di = 1;
		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IS_TWO)) {
			di = 2;
		}
		wFanShu += di;

		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_TONGPAO)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			// 统计
			if (zimo) {
				// 自摸
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						// GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
						continue;
					}
					GRR._lost_fan_shu[i][seat_index] = wFanShu;
				}
			} else {// 点炮
				GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
			}
		}

		float lChiHuScore = wFanShu;

		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_TONGPAO)) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				// 跑
				s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}

		} else {
			////////////////////////////////////////////////////// 自摸 算分
			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					float s = lChiHuScore;

					// 跑
					s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
							+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

					// 胡牌分
					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				float s = lChiHuScore;
				s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));
				// 跑和呛
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
				GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
			}

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// _playerStatus[seat_index].clean_status();
	}

	@Override
	public Player get_player(long account_id) {
		Player player = null;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player = this.get_players()[i];
			if (player != null && player.getAccount_id() == account_id) {
				return player;
			}
		}

		return null;
	}

	@Override
	public boolean handler_audio_chat(Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		return RoomUtil.handler_audio_chat(this, player, chat, l, audio_len);
	}

	@Override
	public boolean handler_emjoy_chat(Player player, int id) {
		RoomUtil.handler_emjoy_chat(this, player, id);
		return true;
	}

	public boolean handler_release_room_in_gold(Player player, int opr_code) {
		if (player == null || player.get_seat_index() == GameConstants.INVALID_SEAT)
			return false;
		if (opr_code != GameConstants.Release_Room_Type_QUIT)
			return false;

		int seat_index = player.get_seat_index();
		if (GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status) {
			force_kick_player_out_room(seat_index, "您已退出该游戏");
		} else {// 游戏中
				// 逃跑扣金币
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id())
					.get(5006);
			int beilv = sysParamModel.getVal2().intValue();
			int cost = sysParamModel.getVal3().intValue();
			if (player.getMoney() < cost) {// 金不足
				send_error_notify(seat_index, 2, "金币必须大于" + cost + "才能够逃跑!");
				return false;
			}
			// 更改其他人补偿金币
			int each_get = cost / (getPlayerCount() - 1);

			if (GRR != null && GRR._game_score != null) {
				for (int i = 0; i < GRR._game_score.length; i++) {
					if (get_players()[i] == null)
						continue;
					if (i == seat_index) {
						GRR._game_score[i] += ((float) (-cost)) / beilv;
					} else {
						GRR._game_score[i] += ((float) (each_get)) / beilv;
					}
				}
			}
			_run_player_id = player.getAccount_id();
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_IN_GOLD);// Game_End_RELEASE_IN_GOLD);

			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;
			PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
		}
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
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}
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
			playerNumber = GameConstants.GAME_PLAYER;
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			if (_gameRoomRecord == null) {
				logger.error("_gameRoomRecord IS null" + ThreadUtil.getStack());
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

			for (int i = 0; i < playerNumber; i++) {
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
			for (int i = 0; i < playerNumber; i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

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
			for (int i = 0; i < playerNumber; i++) {
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
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < playerNumber; i++) {
				if (_gameRoomRecord.release_players[i] != 1) {
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
			for (int i = 0; i < playerNumber; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
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
			_player_open_less[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			} else {
				logger.error("player is null" + ThreadUtil.getStack());
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

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
		if (_handler_piao_fls != null) {
			return _handler_piao_fls.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		} else if (_handler_piao_fls_twenty != null) {
			return _handler_piao_fls_twenty.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		}

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
		// GRR.add_room_response(roomResponse);
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
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player,
			boolean has_action) {
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
		roomResponse.setOperateLen(has_action ? 1 : 0);// add by zain需告知客户端
														// 该出牌是否导致他人有操作
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			roomResponse.setFlashTime(250);
			roomResponse.setStandTime(250);
		} else {
			int standTime = 400;
			int flashTime = 150;
			int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
			SysParamModel sysParamModel104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
			if (sysParamModel104 != null && sysParamModel104.getVal3() > 0 && sysParamModel104.getVal3() < 1000) {
				standTime = sysParamModel104.getVal3();
			}
			if (sysParamModel104 != null && sysParamModel104.getVal4() > 0 && sysParamModel104.getVal4() < 1000) {
				flashTime = sysParamModel104.getVal4();
			}
			roomResponse.setFlashTime(flashTime);
			roomResponse.setStandTime(standTime);

		}

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

	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		return operate_out_card(seat_index, count, cards, type, to_player, false);
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
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			roomResponse.setFlashTime(250);
			roomResponse.setStandTime(250);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(400);
		}

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

	// 显示在玩家前面的牌,小胡牌,摸杠牌
	public boolean operate_show_card(int seat_index, int type, int count, int cards[], int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 添加牌到牌堆
	private boolean operate_add_discard(int seat_index, int count, int cards[]) {
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

	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time, int to_player, int center_card, boolean is_public, boolean add_to_record) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);

		if (effect_type == GameConstants.EFFECT_ACTION_TYPE_HU) {
			if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
				int realEffect = 0;
				for (int i = 0; i < effect_count; i++) {
					if (effect_indexs[i] == GameConstants.CHR_FLS_HONG_HU4
							|| effect_indexs[i] == GameConstants.CHR_FLS_HONG_HU5
							|| effect_indexs[i] == GameConstants.CHR_FLS_HONG_HU6
							|| effect_indexs[i] == GameConstants.CHR_FLS_HONG_HU7) {
						continue;
					}
					realEffect++;
					roomResponse.addEffectsIndex(effect_indexs[i]);
				}
				roomResponse.setEffectCount(realEffect);
			} else {
				roomResponse.setEffectCount(effect_count);
				for (int i = 0; i < effect_count; i++) {
					roomResponse.addEffectsIndex(effect_indexs[i]);
				}
			}
		} else {
			roomResponse.setEffectCount(effect_count);
			for (int i = 0; i < effect_count; i++) {
				roomResponse.addEffectsIndex(effect_indexs[i]);
			}
		}

		long kind = 0;
		if (effect_type == GameConstants.EFFECT_ACTION_TYPE_ACTION) {
			kind = effect_indexs.length > 0 ? effect_indexs[0] : 0;
			roomResponse.setKindType(kind);
			roomResponse.setOperateCard(center_card);
			roomResponse.setProvidePlayer(is_public ? 1 : 0);// 公开标志
		} else if (effect_type == GameConstants.EFFECT_ACTION_TYPE_HU) {
			kind = 0;
			for (int i = 0; i < effect_count; i++) {
				if (effect_indexs[i] == GameConstants.WIK_CHI_HU) {
					kind = effect_indexs[i];
					break;
				} else if (effect_indexs[i] == GameConstants.WIK_ZI_MO) {
					kind = effect_indexs[i];
					break;
				}
			}
			roomResponse.setKindType(kind);
			roomResponse.setOperateCard(center_card);
			roomResponse.setProvidePlayer(is_public ? 1 : 0);// 公开标志
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (seat_index != i) {
					if ((kind == GameConstants.WIK_GANG || kind == GameConstants.WIK_ZHAO) && is_public == false) {
						roomResponse.setOperateCard(0);
					} else {
						roomResponse.setOperateCard(center_card);
					}
				} else {
					roomResponse.setOperateCard(center_card);
				}
				this.send_response_to_player(i, roomResponse);
				// GRR.add_room_response(roomResponse);
			}
			// GRR.add_room_response(roomResponse);
			// this.send_response_to_room(roomResponse);
		} else {
			if (seat_index != to_player) {
				if ((kind == GameConstants.WIK_GANG || kind == GameConstants.WIK_ZHAO) && is_public == false) {
					roomResponse.setOperateCard(0);
				} else {
					roomResponse.setOperateCard(center_card);
				}
			} else {
				roomResponse.setOperateCard(center_card);
			}
			this.send_response_to_player(to_player, roomResponse);
			// GRR.add_room_response(roomResponse);
		}
		if (add_to_record)
			GRR.add_room_response(roomResponse);
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
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
			int time, int to_player, int center_card, boolean is_public) {
		return operate_effect_action(seat_index, effect_type, effect_count, effect_indexs, time, to_player, center_card,
				is_public, true);
	}

	public boolean operate_player_action(int seat_index, boolean close, boolean add_to_record) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			if (add_to_record)
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
		if (add_to_record)
			GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

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
		return operate_player_action(seat_index, close, true);
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

		int standTime = 400;
		int flashTime = 150;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel108 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1108);
		if (sysParamModel108 != null && sysParamModel108.getVal2() > 0 && sysParamModel108.getVal2() < 1000) {
			flashTime = sysParamModel108.getVal2();
		}
		if (sysParamModel108 != null && sysParamModel108.getVal5() > 0 && sysParamModel108.getVal5() < 1000) {
			standTime = sysParamModel108.getVal5();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);
		roomResponse.setInsertTime(flashTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(GameConstants.BLACK_CARD);// 给别人 牌背
			}
			this.send_response_to_other(seat_index, roomResponse);
			roomResponse.clearCardData();

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
				// GRR.add_room_response(roomResponse);
				return this.send_response_to_player(seat_index, roomResponse);
			}
		}

		return false;
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
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
				if ((weaveitems[j].weave_kind == GameConstants.WIK_GANG
						|| weaveitems[j].weave_kind == GameConstants.WIK_ZHAO) && weaveitems[j].public_card == 0) {
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
		return operate_remove_discard(seat_index, discard_index, -1);
	}

	public boolean operate_remove_discard(int provide_seat_index, int discard_index, int target_seat) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(provide_seat_index);
		roomResponse.setDiscardIndex(discard_index);
		roomResponse.setCardTarget(target_seat);// 福禄寿需要知道 来源
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
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

		// 状态变量
		tableResponse.setActionCard(_provide_card);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(_out_card_data);
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

			// 组合扑克
			tableResponse.addWeaveCount(GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_FLS; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			if (_status_send == true) {
				// 牌

				if (i == _current_player) {
					tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]) - 1);
				} else {
					tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));
				}

			} else {
				// 牌
				tableResponse.addCardCount(_logic.get_card_count_by_index(GRR._cards_index[i]));

			}

		}

		// 数据
		tableResponse
				.setSendCardData(((_send_card_data != GameConstants.INVALID_VALUE) && (_provide_player == seat_index))
						? _send_card_data : GameConstants.INVALID_VALUE);
		int hand_cards[] = new int[GameConstants.MAX_FLS_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.MAX_FLS_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index);
		} else {
			if (_out_card_player != GameConstants.INVALID_SEAT && _out_card_data != GameConstants.INVALID_VALUE) {
				this.operate_out_card(_out_card_player, 1, new int[] { _out_card_data },
						GameConstants.OUT_CARD_TYPE_MID, seat_index);
			} else if (_status_cs_gang == true) {
				this.operate_out_card(this._provide_player, 2, this._gang_card_data.get_cards(),
						GameConstants.OUT_CARD_TYPE_MID, seat_index);
			}
		}

		if (_playerStatus[seat_index].has_action()) {
			this.operate_player_action(seat_index, false);
		}

		return true;

	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER;
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

		PlayerResultFLSResponse.Builder playerResultFLSResponse = PlayerResultFLSResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
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
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addGunGunCount(_player_result.gun_gun[i]);
			player_result.addPengPengHuCount(_player_result.peng_peng_hu[i]);
			player_result.addGangShangHuaCount(_player_result.gang_shang_hua[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			player_result.addPlayersId(i);

			playerResultFLSResponse.addHei(_player_result.hei[i]);
			playerResultFLSResponse.addHong(_player_result.hong[i]);
			playerResultFLSResponse.addKu(_player_result.ku[i]);
			playerResultFLSResponse.addKa(_player_result.ka[i]);
			playerResultFLSResponse.addQing(_player_result.qing[i]);
			playerResultFLSResponse.addShidui(_player_result.shidui[i]);
			playerResultFLSResponse.addTai(_player_result.tai[i]);
			// }
		}
		player_result.setFlsResponse(playerResultFLSResponse);

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
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				if (_logic.is_ding_gui_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(
							GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				} else {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i]);
				}

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
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			change_handler(_handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_last_cardtwenty(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			change_handler(_handler_dispath_last_card_twenty);
			this._handler_dispath_last_card_twenty.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/***
	 * 加载房间的玩法 状态信息
	 * 
	 * @param roomResponse
	 */
	public void load_room_info_data(RoomResponse.Builder roomResponse) {
		RoomInfo.Builder room_info = getRoomInfo();
		roomResponse.setRoomInfo(room_info);
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
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public boolean send_response_to_player(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		if (this.get_players()[seat_index] == null) {
			return false;
		}
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public boolean send_sys_response_to_player(int seat_index, String msg) {
		if (this.get_players()[seat_index] == null) {
			return false;
		}

		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		MsgAllResponse.Builder msgBuilder = MsgAllResponse.newBuilder();
		msgBuilder.setType(ESysMsgType.NONE.getId());
		msgBuilder.setMsg(msg);
		msgBuilder.setErrorId(EMsgIdType.ROOM_ERROR.getId());
		responseBuilder.setExtension(Protocol.msgAllResponse, msgBuilder.build());
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());
		return true;
	}

	public boolean send_response_to_room(RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;

			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		return true;
	}

	public boolean send_error_notify(int seat_index, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		Player tarPlayer = this.get_players()[seat_index];
		if (tarPlayer != null) {
			PlayerServiceImpl.getInstance().send(tarPlayer, responseBuilder.build());
		} else {
			logger.error("tarPlayer is null" + ThreadUtil.getStack());
		}

		return false;

	}

	public boolean send_error_notify(Player player, int type, String msg, int time) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		e.setTime(time);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		if (player != null) {
			PlayerServiceImpl.getInstance().send(player, responseBuilder.build());
		} else {
			logger.error("player is null" + ThreadUtil.getStack());
		}

		return false;

	}

	public boolean send_response_to_other(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;
			if (i == seat_index)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		return true;
	}

	private void progress_banker_select() {
		if (_banker_select == GameConstants.INVALID_SEAT) {
			_banker_select = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJGameConstants.GAME_PLAYER;//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_banker_select = rand % GameConstants.GAME_PLAYER;//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	private void record_game_room() {
		// 第一局开始
		_gameRoomRecord = new GameRoomRecord();

		this.set_record_id(BrandIdDict.getInstance().getId());

		_gameRoomRecord.set_record_id(this.get_record_id());
		_gameRoomRecord.setRoom_id(this.getRoom_id());
		_gameRoomRecord.setRoom_owner_account_id(this.getRoom_owner_account_id());
		_gameRoomRecord.setCreate_time(this.getCreate_time());
		_gameRoomRecord.setRoom_owner_name(this.getRoom_owner_name());
		_gameRoomRecord.set_player(_player_result);
		_gameRoomRecord.setPlayers(this.get_players());
		_gameRoomRecord.setCreate_player(this.getCreate_player());
		_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

		// 设置战绩游戏ID
		_gameRoomRecord.setGame_id(GameConstants.GAME_ID_FLS_LX);

		if (!is_sys()) {
			try {
				_recordRoomRecord = MongoDBServiceImpl.getInstance().parentBrand(_gameRoomRecord.getGame_id(),
						this.get_record_id(), "", _gameRoomRecord.to_json(), (long) this._game_round,
						(long) this._game_type_index, this.getRoom_id() + "", this.getRoom_owner_account_id());
			} catch (Exception e) {
				logger.error("房间[" + this.getRoom_id() + "]" + e);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				MongoDBServiceImpl.getInstance().accountBrand(_gameRoomRecord.getGame_id(),
						this.get_players()[i].getAccount_id(), this.get_record_id(), this.getRoom_owner_account_id());
			}
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	private void record_game_round(GameEndResponse.Builder game_end) {
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setMsg(_gameRoomRecord.to_json());
			// 非金币场才需要记录牌局
			if (!is_sys()) {
				MongoDBServiceImpl.getInstance().updateParenBrand(_recordRoomRecord);
			}
		}

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());
			// 非金币场才需要记录牌局
			if (!is_sys()) {
				// 记录 to mangodb
				MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
						null, null, gzipByte, this.getRoom_id() + "",
						_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(),
						this.getRoom_owner_account_id());
			}
		}

		// if ((cost_dou > 0) && (this._cur_round == 1)) {
		// // 不是正常结束的
		// if ((game_end.getEndType() != GameConstants.Game_End_NORMAL)
		// && (game_end.getEndType() != GameConstants.Game_End_DRAW)) {
		// // 还豆
		// StringBuilder buf = new StringBuilder();
		// buf.append("开局失败[" + game_end.getEndType() + "]" + ":" +
		// this.getRoom_id())
		// .append("game_id:" + this.getGame_id()).append(",game_type_index:" +
		// _game_type_index)
		// .append(",game_round:" + _game_round).append(",房主:" +
		// this.getRoom_owner_account_id())
		// .append(",豆+:" + cost_dou);
		// // 把豆还给玩家
		// AddGoldResultModel result =
		// PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
		// cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
		// if (result.isSuccess() == false) {
		// logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" +
		// this.getRoom_owner_account_id() + "]还豆失败");
		// }
		//
		// }
		// }

	}

	public void log_error(String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + error);

	}

	public void log_player_error(int seat_index, String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + " 玩家[" + seat_index + "]" + error);

	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_fls() {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}

					if (type == GameConstants.CHR_FLS_MENQING
							&& !(GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_ZI_MO).is_empty())) {
						des += " 门清";
					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_FLS_PENGPENGHU) {
						des += " 碰碰胡";
					}
					if (type == GameConstants.CHR_FLS_GANGSHANGHUA) {
						des += " 杠上开花";
					}
					if (type == GameConstants.CHR_FLS_GANGSHANGPAO) {
						des += " 杠上炮";
					}
					if (type == GameConstants.CHR_FLS_GUNGUN) {
						des += " 滚滚";
					}
					if (type == GameConstants.CHR_FLS_MANTIAN_FEI) {
						des += " 满天飞";
					}
					if (type == GameConstants.CHR_FLS_HAIDI) {
						des += " 海底捞月";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			// int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			// if (GRR != null) {
			// for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
			// for (int w = 0; w < GRR._weave_count[p]; w++) {
			// if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG)
			// {
			// continue;
			// }
			// if (p == i) {// 自己
			// // 接杠
			// if (GRR._weave_items[p][w].provide_player != p) {
			// jie_gang++;
			// } else {
			// if (GRR._weave_items[p][w].public_card == 1) {// 明杠
			// ming_gang++;
			// } else {
			// an_gang++;
			// }
			// }
			// } else {
			// // 放杠
			// if (GRR._weave_items[p][w].provide_player == i) {
			// fang_gang++;
			// }
			// }
			// }
			// }
			// }
			//
			// if (an_gang > 0) {
			// des += " 暗杠X" + an_gang;
			// }
			// if (ming_gang > 0) {
			// des += " 明杠X" + ming_gang;
			// }
			// if (fang_gang > 0) {
			// des += " 放杠X" + fang_gang;
			// }
			// if (jie_gang > 0) {
			// des += " 接杠X" + jie_gang;
			// }
			// if (lGangScore[i] != 0 && GRR._end_type ==
			// GameConstants.Game_End_NORMAL) {
			// des += " 杠总分(" + lGangScore[i] + ")";
			// }
			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_fls_twenty() {
		// 杠牌，每个人的分数
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			if (l > 0) {
				if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IS_TWO)) {
					des += "2底分";
				} else {
					des += "1底分";
				}
			}
			boolean haveHong = false;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (type == GameConstants.CHR_FLS_QING_HU) {
						des += " 清";
					}
					if (type == GameConstants.CHR_FLS_KA_HU) {
						des += " 卡";
					}
					if (type == GameConstants.CHR_FLS_TAI_HU) {
						des += " 台";
					}
					if (type == GameConstants.CHR_FLS_KU_HU) {
						des += " 枯";
					}
					if (type == GameConstants.CHR_FLS_HEI_HU) {
						des += " 黑原";
					}
					if (type == GameConstants.CHR_FLS_SHI_DUI) {
						des += " 十对";
					}
					if (type == GameConstants.CHR_FLS_ZHONG_HU) {
						des += " 重";
					}

					if (type == GameConstants.CHR_FLS_HONG_HU7 || type == GameConstants.CHR_FLS_HONG_HU6
							|| type == GameConstants.CHR_FLS_HONG_HU5 || type == GameConstants.CHR_FLS_HONG_HU4) {
						if (type == GameConstants.CHR_FLS_HONG_HU7) {
							des += " 7红原";
							haveHong = true;
						} else if (type == GameConstants.CHR_FLS_HONG_HU6) {
							des += " 6红原";
							haveHong = true;
						} else if (type == GameConstants.CHR_FLS_HONG_HU5) {
							des += " 5红原";
							haveHong = true;
						} else if (type == GameConstants.CHR_FLS_HONG_HU4) {
							des += " 4红原";
							haveHong = true;
						}
					} else {
						if (type == GameConstants.CHR_FLS_HONG_HU) {
							if (!haveHong) {
								des += " 红原";
							}
						}
					}

					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	private void set_result_describe() {
		if (this.is_mj_type(GameConstants.GAME_TYPE_FLS_LX) || this.is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			set_result_describe_fls();
		} else if (this.is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			set_result_describe_fls_twenty();
		}
	}

	// 是否听牌
	public boolean is_fls_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int handcount = _logic.get_card_count_by_index(cards_index);
		if (handcount == 1) {
			// 全求人
			return true;
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < getMaxIndex(); i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO))
				return true;
		}
		return false;
	}

	// 是否听牌-带赖子
	public boolean is_fls_ting_card_dp(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int handcount = _logic.get_card_count_by_index(cards_index);
		if (handcount == 1) {
			// 全求人
			return true;
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < getMaxIndex(); i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO))
				return true;
		}
		return false;
	}

	// 是否听牌
	public boolean is_fls_ting_card_Twenty(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		int handcount = _logic.get_card_count_by_index(cards_index);
		if (handcount == 1) {
			// 全求人
			return true;
		}

		// 复制数据
		int cbCardIndexTemp[] = new int[getMaxIndex()];
		for (int i = 0; i < getMaxIndex(); i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < getMaxIndex(); i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO))
				return true;
		}
		return false;
	}

	public String get_game_des() {
		return GameDescUtil.getGameDesc(_game_type_index, _game_rule_index);
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
		change_handler(_handler_finish);
		this._handler_finish.exe(this);
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
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay,
					TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			change_handler(_handler_dispath_card);
			this._handler_dispath_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

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
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean self,
			boolean d) {
		// 是否有抢杠胡

		change_handler(_handler_gang);
		this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d);
		this._handler.exe(this);

		return true;
	}

	/**
	 * 长沙麻将杠牌处理
	 * 
	 * @param seat_index
	 * @param d
	 * @return
	 */
	public boolean exe_gang_fls(int seat_index, boolean d) {
		change_handler(_handler_gang_fls);
		this._handler_gang_fls.reset_status(seat_index, d);
		this._handler_gang_fls.exe(this);
		return true;
	}

	/**
	 * 海底
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_hai_di(int start_index, int seat_index) {
		change_handler(_handler_hai_di);
		this._handler_hai_di.reset_status(start_index, seat_index);
		this._handler_hai_di.exe(this);
		return true;
	}

	/**
	 * 要海底
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_yao_hai_di(int seat_index) {
		change_handler(_handler_yao_hai_di);
		this._handler_yao_hai_di.reset_status(seat_index);
		this._handler_yao_hai_di.exe(this);
		return true;
	}

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean exe_out_card(int seat_index, int card, int type) {
		// 出牌
		change_handler(_handler_out_card_operate);
		this._handler_out_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		// 出牌
		change_handler(_handler_chi_peng);
		this._handler_chi_peng.reset_status(seat_index, _out_card_player, action, card, type);
		this._handler.exe(this);

		return true;
	}
	
	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type,int[] copy) {
		// 出牌
		change_handler(_handler_chi_peng);
		this._handler_chi_peng.reset_status(seat_index, _out_card_player, action, card, type,copy);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_xi_pai() {
		change_handler(_handler_xipai_fls);
		this._handler_xipai_fls.exe(this);
		return true;
	}

	public boolean exe_xi_pai_twenty() {
		this._handler = this._handler_xipai_fls_twenty;
		this._handler_xipai_fls_twenty.exe(this);
		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card),
				GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;

			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
		if (send_client == true) {
			this.operate_add_discard(seat_index, card_count, card_data);
		}
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards(int seat_index) {
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < getMaxIndex(); j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_FLS_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	public void runnable_create_time_out() {
		// 已经开始
		if (this._game_status != GameConstants.GS_MJ_FREE) {
			return;
		}

		// 把豆还给创建的人
		this.huan_dou(GameConstants.Game_End_RELEASE_SYSTEM);

		process_release_room();
	}

	/***
	 * 强制解散
	 */
	public boolean force_account() {
		if (this._cur_round == 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			Player player = null;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏已被系统解散");
			}
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_SYSTEM);
		}

		return false;
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			pl.add(this.get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(this.get_players());

		// 重新排下位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				continue;
			}
			get_players()[i].set_seat_index(i);
		}
	}

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

	}

	public boolean handler_requst_location(Player player, LocationInfor locationInfor) {
		// LocationUtil.LantitudeLongitudeDist(lon1, lat1, lon2, lat2);
		// 发送数据
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.send_response_to_player(i, roomResponse);
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee) {
		if (_playerStatus == null || istrustee == null)
			return false;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing && (!is_sys())) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (!SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX) && (!is_sys())) {
			return false;
		}

		// 金币场 游戏开始前 无法托管
		if (is_sys() && (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)) {
			send_error_notify(get_seat_index, 1, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
		this.send_response_to_room(roomResponse);

		if (_handler != null && is_sys()) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		// 发牌
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX_TWENTY)) {
			change_handler(_handler_dispath_last_card_twenty);
			this._handler_dispath_last_card_twenty.reset_status(cur_player, type);
			this._handler.exe(this);
		} else {
			change_handler(_handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		}

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

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {
		return true;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if (is_sys()) {
			playerNumber = GameConstants.GAME_PLAYER;
		}
		return playerNumber;
	}

	public boolean refresh_room_redis_data(int type, boolean notifyRedis) {

		if (type == GameConstants.PROXY_ROOM_UPDATE) {
			int cur_player_num = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (this.get_players()[i] != null) {
					cur_player_num++;
				}
			}

			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			if (roomRedisModel != null) {
				RedisService redisService = SpringService.getBean(RedisService.class);
				roomRedisModel.setGameRuleDes(this.get_game_des());
				roomRedisModel.setRoomStatus(this._game_status);
				roomRedisModel.setPlayer_max(RoomComonUtil.getMaxNumber(this._game_type_index, this._game_rule_index));
				roomRedisModel.setCur_player_num(cur_player_num);
				roomRedisModel.setGame_round(this._game_round);
				// roomRedisModel.setCreate_time(System.currentTimeMillis());
				redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			}

		} else if (type == GameConstants.PROXY_ROOM_RELEASE) {

		}
		if (notifyRedis) {
			// 通知redis消息队列
			RedisResponse.Builder redisResponseBuilder = RedisResponse.newBuilder();
			redisResponseBuilder.setRsResponseType(RsResponseType.CMD);
			RsCmdResponse.Builder rsCmdResponseBuilder = RsCmdResponse.newBuilder();
			rsCmdResponseBuilder.setType(3);
			rsCmdResponseBuilder.setAccountId(this.getRoom_owner_account_id());
			redisResponseBuilder.setRsCmdResponse(rsCmdResponseBuilder);
			RedisServiceImpl.getInstance().convertAndSendRsResponse(redisResponseBuilder.build(),
					ERedisTopicType.topicProxy);

		}
		return true;
	}

	private boolean kou_dou() {
		return RoomUtil.kou_dou(this);
		// int game_type_index = this._game_type_index;
		// // 收费索引
		// this.game_index =
		// SysGameTypeEnum.getGameGoldTypeIndex(game_type_index);
		// this.setGame_id(SysGameTypeEnum.getGameIDByTypeIndex(game_type_index));
		// if (is_sys()) {
		// return true;
		// }
		// SysParamModel sysParamModel1010 = SysParamDict.getInstance()
		// .getSysParamModelDictionaryByGameId(this.getGame_id()).get(1010);
		// SysParamModel sysParamModel1011 = SysParamDict.getInstance()
		// .getSysParamModelDictionaryByGameId(this.getGame_id()).get(1011);
		// SysParamModel sysParamModel1012 = SysParamDict.getInstance()
		// .getSysParamModelDictionaryByGameId(this.getGame_id()).get(1012);
		// int check_gold = 0;
		// boolean create_result = true;
		//
		// if (_game_round == 4) {
		// check_gold = sysParamModel1010.getVal2();
		// } else if (_game_round == 8) {
		// check_gold = sysParamModel1011.getVal2();
		// } else if (_game_round == 16) {
		// check_gold = sysParamModel1012.getVal2();
		// }
		//
		// // 注意游戏ID不一样
		// if (check_gold == 0) {
		// create_result = false;
		// } else {
		// // 是否免费的
		// SysParamModel sysParamModel = SysParamDict.getInstance()
		// .getSysParamModelDictionaryByGameId(this.getGame_id()).get(this.game_index);
		// if (sysParamModel != null && sysParamModel.getVal2() == 1) {
		// // 收费
		// StringBuilder buf = new StringBuilder();
		// buf.append("创建房间:" + this.getRoom_id()).append("game_id:" +
		// this.getGame_id())
		// .append(",game_type_index:" + game_type_index).append(",game_round:"
		// + _game_round);
		// AddGoldResultModel result =
		// PlayerServiceImpl.getInstance().subGold(this.getRoom_owner_account_id(),
		// check_gold, false, buf.toString());
		// if (result.isSuccess() == false) {
		// create_result = false;
		// } else {
		// // 扣豆成功
		// cost_dou = check_gold;
		// }
		// }
		//
		// }
		// if (create_result == false) {
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		// Player player = null;
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		// this.send_response_to_player(i, roomResponse);
		//
		// player = this.get_players()[i];
		// if (player == null)
		// continue;
		// if (i == 0) {
		// send_error_notify(i, 1, "闲逸豆不足,游戏解散");
		// } else {
		// send_error_notify(i, 1, "创建人闲逸豆不足,游戏解散");
		// }
		//
		// }
		//
		// // 删除房间
		// PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		// return false;
		// }
		//
		// return true;
	}

	private boolean huan_dou(int result) {
		if (cost_dou == 0)
			return false;
		boolean huan = false;
		if (result == GameConstants.Game_End_NORMAL || result == GameConstants.Game_End_DRAW
				|| result == GameConstants.Game_End_ROUND_OVER) {
			return false;
		} else if (result == GameConstants.Game_End_RELEASE_NO_BEGIN) {
			// 还没开始
			huan = true;
		} else if (result == GameConstants.Game_End_RELEASE_RESULT || result == GameConstants.Game_End_RELEASE_PLAY
				|| result == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| result == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			// 开始了 没打完
			if ((this._cur_round <= 1) && (GRR != null)) {
				huan = true;
			}
		} else if (result == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (this._cur_round <= 1) {
				huan = true;
			}
		} else {
			return false;
		}

		if (huan) {
			// 不是正常结束的
			// if((game_end.getEndType()!=GameConstants.Game_End_NORMAL) &&
			// (game_end.getEndType()!=GameConstants.Game_End_DRAW)){
			// 还豆
			StringBuilder buf = new StringBuilder();
			buf.append("开局失败" + ":" + this.getRoom_id()).append("game_id:" + this.getGame_id())
					.append(",game_type_index:" + _game_type_index).append(",game_round:" + _game_round)
					.append(",房主:" + this.getRoom_owner_account_id()).append(",豆+:" + cost_dou);
			// 把豆还给玩家
			AddGoldResultModel addresult = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
					cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
			if (addresult.isSuccess() == false) {
				logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
			}

			// }
		}
		return true;
	}

	public void be_in_room_trustee(int get_seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(false);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		istrustee[get_seat_index] = false;
		if (!SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			return;
		}
		this.send_response_to_other(get_seat_index, roomResponse);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_goods(int,
	 * protobuf.clazz.Protocol.RoomRequest)
	 */
	@Override
	public boolean handler_request_goods(int get_seat_index, RoomRequest room_rq) {
		long targetID = room_rq.getTargetAccountId();
		int goodsID = room_rq.getGoodsID();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GOODS);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setGoodID(goodsID);
		roomResponse.setTargetID(targetID);
		this.send_response_to_room(roomResponse);

		// 刷新玩家金币
		RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse2);
		this.send_response_to_room(roomResponse2);
		return false;
	}

	public void change_handler(FLSHandler dest_handler) {
		_handler = dest_handler;
	}

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
			_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index),
					GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
		}
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
			if (this.get_players()[i] == null) {
				continue;
			}
			score = (int) (scores[i] * beilv);
			// 逻辑处理
			this.get_players()[i].setMoney(this.get_players()[i].getMoney() + score);
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
		// playerNumber = 0;
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
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_room(roomResponse);
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
					_player_open_less[i] = 0;
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
	
	
	public int getMaxIndex() {
		if(is_mj_type(GameConstants.GAME_TYPE_FLS_LX_DP)) {
			return GameConstants.MAX_FLS_INDEX_DP;
		}else {
			return GameConstants.MAX_FLS_INDEX;
		}
	}

	@Override
	public void init_other_param(Object... objects) {
		// WalkerGeek Auto-generated method stub

	}

	/**
	 * @param seat_index
	 * @param call_banker
	 * @param qiang_banker
	 * @return
	 */
	@Override
	public boolean handler_requst_call_qiang_zhuang(int seat_index, int call_banker, int qiang_banker) {
		return true;
	}

}
