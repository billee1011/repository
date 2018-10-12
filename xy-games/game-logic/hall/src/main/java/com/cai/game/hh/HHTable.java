/**
 * 
 */
package com.cai.game.hh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
import com.cai.common.define.SysGameTypeEnum;
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
import com.cai.common.util.SpringService;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.ChulifirstCardRunnable;
import com.cai.future.runnable.CreateTimeOutRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.hh.handler.HHHandler;
import com.cai.game.hh.handler.HHHandlerChiPeng;
import com.cai.game.hh.handler.HHHandlerDispatchCard;
import com.cai.game.hh.handler.HHHandlerFinish;
import com.cai.game.hh.handler.HHHandlerGang;
import com.cai.game.hh.handler.HHHandlerOutCardOperate;
import com.cai.game.hh.handler.yxfphz.FPHZHandlerChiPeng_YX;
import com.cai.game.hh.handler.yxfphz.FPHZHandlerChuLiFirstCard_YX;
import com.cai.game.hh.handler.yxfphz.FPHZHandlerDispatchCard_YX;
import com.cai.game.hh.handler.yxfphz.FPHZHandlerDispatchFirstCard_YX;
import com.cai.game.hh.handler.yxfphz.FPHZHandlerGang_YX;
import com.cai.game.hh.handler.yxfphz.FPHZHandlerOutCardOperate_YX;
import com.cai.game.hh.handler.yxhh.HHHandlerChiPeng_YX;
import com.cai.game.hh.handler.yxhh.HHHandlerChuLiFirstCard_YX;
import com.cai.game.hh.handler.yxhh.HHHandlerDispatchCard_YX;
import com.cai.game.hh.handler.yxhh.HHHandlerDispatchFirstCard_YX;
import com.cai.game.hh.handler.yxhh.HHHandlerGang_YX;
import com.cai.game.hh.handler.yxhh.HHHandlerOutCardOperate_YX;
import com.cai.game.hh.handler.yxphz.PHZHandlerChiPeng_YX;
import com.cai.game.hh.handler.yxphz.PHZHandlerChuLiFirstCard_YX;
import com.cai.game.hh.handler.yxphz.PHZHandlerDispatchCard_YX;
import com.cai.game.hh.handler.yxphz.PHZHandlerDispatchFirstCard_YX;
import com.cai.game.hh.handler.yxphz.PHZHandlerGang_YX;
import com.cai.game.hh.handler.yxphz.PHZHandlerOutCardOperate_YX;
import com.cai.handler.LogicRoomHandler;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.RedisServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.LocationInfor;
import protobuf.clazz.Protocol.MsgAllResponse;
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
public class HHTable extends Room {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(HHTable.class);

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

	// 状态变量
	private boolean _status_send; // 发牌状态
	private boolean _status_gang; // 抢杆状态
	private boolean _status_cs_gang; // 长沙杆状态
	private boolean _status_gang_hou_pao; // 杠后炮状态

	public PlayerResult _player_result;
	public boolean _ti_two_long[];
	public boolean _is_xiang_gong[];

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

	public int _qiang_MAX_HH_COUNT; // 最大呛分
	public int _lian_zhuang_player; // 连庄玩家
	public int _shang_zhuang_player; // 上庄玩家

	// 出牌信息
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _out_card_count = GameConstants.INVALID_VALUE; // 出牌数目
	public int _all_card_len = 0;

	public int _last_card = 0; // 上次抓的牌
	public int _last_player = -1;// 上次发牌玩家
	public int _long_count[]; // 每个用户有几条龙
	public int _cannot_chi[][];// 不可以吃的牌
	public int _cannot_chi_count[];// 每个用户不可以吃牌的数量
	public int _cannot_peng[][];// 不可以碰的牌
	public int _cannot_peng_count[];// 每个用户不可以碰牌的数量
	// 胡息
	public HuCardInfo _hu_card_info;

	GameRoomRecord _gameRoomRecord;
	BrandLogModel _recordRoomRecord;

	public GameRoundRecord GRR;

	public HHGameLogic _logic = null;
	public LouWeaveItem _lou_weave_item[][];
	public int _hu_weave_count[]; //
	public int _hu_xi[];
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _hu_weave_items[][];

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

	public HHHandler _handler;

	public HHHandlerDispatchCard _handler_dispath_card;
	public HHHandlerOutCardOperate _handler_out_card_operate;
	public HHHandlerGang _handler_gang;
	public HHHandlerChiPeng _handler_chi_peng;
	public HHHandlerDispatchCard _handler_dispath_firstcards;
	public HHHandlerDispatchCard _handler_chuli_firstcards;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public HHHandlerFinish _handler_finish; // 结束

	private int cost_dou = 0;// 扣豆

	public HHTable() {
		super(RoomType.HH);

		_logic = new HHGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;

		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_player_open_less = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
		_hu_xi = new int[this.getTablePlayerNumber()];
		// 出牌信息
		_out_card_data = 0;
		// 上次抓的牌
		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		// 不能吃，碰
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
		}
		// 胡牌信息
		_hu_card_info = new HuCardInfo();
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}
		_status_cs_gang = false;

		_gang_card_data = new CardsData(GameConstants.MAX_HH_COUNT);
	}

	public void init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		// 上次抓的牌
		_last_card = 0;
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
			_ti_two_long[i] = false;
			_is_xiang_gong[i] = false;
		}
		// 不能吃，碰
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
		}
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}
		_hu_xi = new int[this.getTablePlayerNumber()];
		// 胡牌信息
		_hu_card_info = new HuCardInfo();
		boolean kd = this.kou_dou();
		if (kd == false) {
			return;
		}
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
		}

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			// 初始化基础牌局handler
			_handler_dispath_card = new HHHandlerDispatchCard_YX();
			_handler_out_card_operate = new HHHandlerOutCardOperate_YX();
			_handler_gang = new HHHandlerGang_YX();
			_handler_chi_peng = new HHHandlerChiPeng_YX();

			_handler_chuli_firstcards = new HHHandlerChuLiFirstCard_YX();
			_handler_dispath_firstcards = new HHHandlerDispatchFirstCard_YX();
			// _handler_dispath_last_card = new HHHandlerDispatchLastCard_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			_handler_dispath_card = new PHZHandlerDispatchCard_YX();
			_handler_out_card_operate = new PHZHandlerOutCardOperate_YX();
			_handler_gang = new PHZHandlerGang_YX();
			_handler_chi_peng = new PHZHandlerChiPeng_YX();

			_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_YX();
			_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			_handler_dispath_card = new FPHZHandlerDispatchCard_YX();
			_handler_out_card_operate = new FPHZHandlerOutCardOperate_YX();
			_handler_gang = new FPHZHandlerGang_YX();
			_handler_chi_peng = new FPHZHandlerChiPeng_YX();

			_handler_chuli_firstcards = new FPHZHandlerChuLiFirstCard_YX();
			_handler_dispath_firstcards = new FPHZHandlerDispatchFirstCard_YX();
		}
		_handler_finish = new HHHandlerFinish();

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

			if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
				// if (getTablePlayerNumber() != this.getTablePlayerNumber()) {
				// this.shuffle_players();
				//
				// for (int i = 0; i < getTablePlayerNumber(); i++) {
				// this.get_players()[i].set_seat_index(i);
				// if (this.get_players()[i].getAccount_id() ==
				// this.getRoom_owner_account_id()) {
				// this._banker_select = i;
				// }
				// }
				// }
				// 胡牌信息

			}

			record_game_room();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
		} // 不能吃，碰
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
		}
		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;
		this._handler = null;

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_HH_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
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

	public void getLocationTip() {
		try {
			String tipMsg = "";

			StringBuffer buf = new StringBuffer();
			HashSet<String> names = new HashSet<String>();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null)
					continue;
				Player player = this.get_players()[i];
				if (player.locationInfor == null || player.locationInfor.getPosX() == 0
						|| player.locationInfor.getPosX() == 0) {
					continue;
				}
				for (int j = i; j < this.getTablePlayerNumber(); j++) {
					if (this.get_players()[j] == null)
						continue;
					Player targetPlayer = this.get_players()[j];
					if (targetPlayer.locationInfor == null || targetPlayer.locationInfor.getPosX() == 0
							|| targetPlayer.locationInfor.getPosX() == 0) {
						continue;
					}
					if (targetPlayer.getAccount_id() == player.getAccount_id())
						continue;
					double distance = LocationUtil.LantitudeLongitudeDist(player.locationInfor.getPosX(),
							player.locationInfor.getPosY(), targetPlayer.locationInfor.getPosX(),
							targetPlayer.locationInfor.getPosY());

					int tipDistance = 1000;
					SysParamModel sysParamModel = SysParamDict.getInstance()
							.getSysParamModelDictionaryByGameId(getGame_id()).get(5005);
					if (sysParamModel != null && sysParamModel.getVal5() > 0) {
						tipDistance = sysParamModel.getVal5();
					}
					if (distance < tipDistance) {
						names.add(player.getNick_name());
						names.add(targetPlayer.getNick_name());
					}
				}
			}
			for (String name : names) {
				buf.append(name).append(" ");
			}
			if (names.size() > 1) {
				tipMsg = buf.append("距离过近").toString();
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Player player = this.get_players()[i];
					if (player == null)
						continue;
					send_error_notify(player, 2, tipMsg);
				}
			}

		} catch (Exception e) {
			logger.error("定位提示异常", e);
		}

	}

	// 游戏开始
	public boolean handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;
		this.log_error("gme_status:"+this._game_status);

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _banker_select;
		_current_player = GRR._banker_player;

		//
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);
		}

		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)){
			_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);
		}
//		 DEBUG_CARDS_MODE = true;
//		 BACK_DEBUG_CARDS_MODE = true;

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			return game_start_HH();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			return game_start_PHZ_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
		{
			return game_start_FPHZ_YX();
		}
		return false;
	}

	/**
	 * 开始 攸县红黑胡
	 * 
	 * @return
	 */
	private boolean game_start_HH() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
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

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
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

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards,
					GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}
	private boolean game_start_FPHZ_YX(){
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
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
			int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
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

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);

		this.GRR.add_room_response(roomResponse);

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

	
		
		return true;
	}
	private boolean game_start_PHZ_YX() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
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
			int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
					.get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
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

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);

		this.GRR.add_room_response(roomResponse);

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

	
		
		return true;
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

	/*
	 * public void start_card_begin() { int playerCount = getPlayerCount();
	 * this.GRR._banker_player = this._current_player = this._banker_select; //
	 * 游戏开始 this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
	 * GameStartResponse.Builder gameStartResponse =
	 * GameStartResponse.newBuilder();
	 * gameStartResponse.setBankerPlayer(this.GRR._banker_player);
	 * gameStartResponse.setCurrentPlayer(this._current_player);
	 * gameStartResponse.setLeftCardCount(this.GRR._left_card_count);
	 * 
	 * int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT]; //
	 * 发送数据 for (int i = 0; i < playerCount; i++) { int hand_card_count =
	 * this._logic.switch_to_cards_data(this.GRR._cards_index[i],
	 * hand_cards[i]); gameStartResponse.addCardsCount(hand_card_count); }
	 * 
	 * // 发送数据 for (int i = 0; i < playerCount; i++) {
	 * Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
	 * 
	 * // 只发自己的牌 gameStartResponse.clearCardData(); for (int j = 0; j <
	 * GameConstants.MAX_HH_COUNT; j++) {
	 * gameStartResponse.addCardData(hand_cards[i][j]); }
	 * 
	 * // 回放数据 this.GRR._video_recode.addHandCards(cards);
	 * 
	 * RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	 * this.load_room_info_data(roomResponse);
	 * this.load_common_status(roomResponse);
	 * 
	 * if (this._cur_round == 1) { // shuffle_players();
	 * this.load_player_info_data(roomResponse); }
	 * roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
	 * roomResponse.setGameStart(gameStartResponse);
	 * roomResponse.setCurrentPlayer( this._current_player ==
	 * GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * roomResponse.setGameStatus(this._game_status);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * this.send_response_to_player(i, roomResponse); }
	 * ////////////////////////////////////////////////// 回放
	 * RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	 * roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
	 * this.load_room_info_data(roomResponse);
	 * this.load_common_status(roomResponse);
	 * this.load_player_info_data(roomResponse); for (int i = 0; i <
	 * playerCount; i++) { Int32ArrayResponse.Builder cards =
	 * Int32ArrayResponse.newBuilder();
	 * 
	 * for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
	 * cards.addItem(hand_cards[i][j]); } gameStartResponse.addCardsData(cards);
	 * }
	 * 
	 * roomResponse.setGameStart(gameStartResponse);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * this.GRR.add_room_response(roomResponse);
	 * 
	 * }
	 */
	/**
	 * 开始游戏 逻辑
	 */
	/*
	 * public void game_start_real() { int playerCount = getPlayerCount();
	 * this.GRR._banker_player = this._current_player = this._banker_select; //
	 * 游戏开始 this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
	 * GameStartResponse.Builder gameStartResponse =
	 * GameStartResponse.newBuilder();
	 * gameStartResponse.setBankerPlayer(this.GRR._banker_player);
	 * gameStartResponse.setCurrentPlayer(this._current_player);
	 * gameStartResponse.setLeftCardCount(this.GRR._left_card_count);
	 * 
	 * int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT]; //
	 * 发送数据 for (int i = 0; i < playerCount; i++) { int hand_card_count =
	 * this._logic.switch_to_cards_data(this.GRR._cards_index[i],
	 * hand_cards[i]); gameStartResponse.addCardsCount(hand_card_count); }
	 * 
	 * // 发送数据 for (int i = 0; i < playerCount; i++) {
	 * Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
	 * 
	 * // 只发自己的牌 gameStartResponse.clearCardData(); for (int j = 0; j <
	 * GameConstants.MAX_HH_COUNT; j++) {
	 * gameStartResponse.addCardData(hand_cards[i][j]); }
	 * 
	 * // 回放数据 this.GRR._video_recode.addHandCards(cards);
	 * 
	 * RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	 * this.load_room_info_data(roomResponse);
	 * this.load_common_status(roomResponse);
	 * 
	 * if (this._cur_round == 1) { // shuffle_players();
	 * this.load_player_info_data(roomResponse); }
	 * roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
	 * roomResponse.setGameStart(gameStartResponse);
	 * roomResponse.setCurrentPlayer( this._current_player ==
	 * GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * roomResponse.setGameStatus(this._game_status);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * this.send_response_to_player(i, roomResponse); }
	 * ////////////////////////////////////////////////// 回放
	 * RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	 * roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
	 * this.load_room_info_data(roomResponse);
	 * this.load_common_status(roomResponse);
	 * this.load_player_info_data(roomResponse); for (int i = 0; i <
	 * playerCount; i++) { Int32ArrayResponse.Builder cards =
	 * Int32ArrayResponse.newBuilder();
	 * 
	 * for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
	 * cards.addItem(hand_cards[i][j]); } gameStartResponse.addCardsData(cards);
	 * }
	 * 
	 * roomResponse.setGameStart(gameStartResponse);
	 * roomResponse.setLeftCardCount(this.GRR._left_card_count);
	 * this.GRR.add_room_response(roomResponse);
	 * 
	 * // 检测听牌 for (int i = 0; i < playerCount; i++) {
	 * this._playerStatus[i]._hu_card_count =
	 * this.get_fls_ting_card(this._playerStatus[i]._hu_cards,
	 * this.GRR._cards_index[i], this.GRR._weave_items[i],
	 * this.GRR._weave_count[i]); if (this._playerStatus[i]._hu_card_count > 0)
	 * { this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count,
	 * this._playerStatus[i]._hu_cards); } }
	 * 
	 * this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0); }
	 */

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		_logic.random_card_data(repertory_card, mj_cards);

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		if(is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
		{
			for (int i = 0; i < count; i++) {
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
				GRR._left_card_count -= send_count;
				// 一人20张牌,庄家多一张
				_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
				have_send_count += send_count;
			}
			// test_cards();
			// 记录初始牌型
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
			return ;
			
		}
		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			GRR._left_card_count -= send_count;
			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	private void test_cards() {
//		int cards[] = new int[] {0x01,0x11,0x11,0x12,0x13,0x15,0x15,0x05,0x16,0x1a,0x0a,0x13,0x14,0x14,0x07,0x09,0x08,0x02,0x07,0x0a};
//		int cards1[] = new int[] {0x01,0x11,0x11,0x12,0x13,0x15,0x15,0x05,0x16,0x1a,0x0a,0x14,0x14,0x14,0x07,0x09,0x08,0x02,0x07,0x0a};
//		int cards2[] = new int[] {0x01,0x11,0x11,0x12,0x13,0x15,0x15,0x05,0x16,0x1a,0x0a,0x14,0x14,0x14,0x07,0x09,0x08,0x02,0x07,0x0a};
////		int cards3[] = new int[] {0x01,0x01,0x01,0x01,0x02,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04};
////		// //int cards[] = new int[]{011,011,011};
////		// // int cards2[] = new int[] { 0x12, 0x12, 0x12, 0x12, 0x01, 0x02,
////		// 0x04, 0x03, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17,
////		// // 0x17, 0x15, 0x19, 0x17,0x17,0x13 };
////		// // int cards3[] = new int[] { 0x13, 0x13, 0x14, 0x14, 0x01, 0x02,
////		// 0x04, 0x03, 0x05, 0x06, 0x15, 0x15, 0x16, 0x17,
////		// // 0x17, 0x15, 0x19, 0x18,0x18,0x16 };
////		//
////		//// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
////		//// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
////		//// GRR._cards_index[i][j] = 0;
////		//// }
////		//// }
////		//// int send_count = (GameConstants.MAX_HH_COUNT );
////		////
//		 _repertory_card[_all_card_len - GRR._left_card_count] = 0x14;
//		 if(this.getTablePlayerNumber() == GameConstants.GAME_PLAYER_HH)
//		 {
//			 for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
//			 GRR._cards_index[_current_player%this.getTablePlayerNumber()][j] =
//			 0;
//			 GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][j]
//			 = 0;
//			 GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][j]
//			 = 0;
//			 }
//		 }else
//		 {
//			 for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
//				 GRR._cards_index[_current_player%this.getTablePlayerNumber()][j] =
//				 0;
//				 GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][j]
//				 = 0;
//				 GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][j]
//				 = 0;
//				 GRR._cards_index[(_current_player+3)%this.getTablePlayerNumber()][j]
//						 = 0;
//				 }
//		 }
//		
//		 for(int j = 0; j< cards.length;j++)
//		 {
//		 GRR._cards_index[_current_player%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards[j])]
//		 += 1;
//		 }
//		 for(int j = 0; j<cards1.length;j++)
//		 {
//		 GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards1[j])]
//		 += 1;
//		 }
//		 for(int j = 0; j<cards2.length;j++)
//		 {
//		 GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards2[j])]
//		 += 1;
//		 }
//		 for(int j = 0; j<cards3.length;j++)
//		 {
//		 GRR._cards_index[(_current_player+3)%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards3[j])]
//		 += 1;
//		 }


//		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
//		 int[] realyCards = new int[] {1, 1, 5, 5,4,4,2, 3, 3, 3,20, 21, 22, 24, 9, 9, 25, 10, 10,24, 24,24, 17,
//				 1, 1, 2, 2, 19, 4, 20, 20, 6, 6, 22,22,25, 23, 10, 8, 26, 26, 26, 2, 18, 18, 19, 4, 5,
//				 5, 21, 21, 7, 7, 23,23, 8,9, 9, 25, 6, 10, 21, 7, 23, 2, 4, 23, 8, 2, 10, 21, 19, 17, 1,
//				 24, 20, 22, 17, 6, 18, 7, 24, 24};
//		 testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if(this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false)
				{
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
				else{	
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
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
//		_banker_select = 2;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

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
			if(this.is_mj_type(GameConstants.GAME_PLAYER_FPHZ) == false)
				send_count = (GameConstants.MAX_HH_COUNT - 1);
			else 
				send_count = GameConstants.MAX_FPHZ_COUNT - 1;
		
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
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
	public boolean handler_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;
		
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			ret = this.handler_game_finish_hh(seat_index, reason);
		}

		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			ret = this.handler_game_finish_PHZ(seat_index, reason);
		}
		if(is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)){
			ret = this.handler_game_finish_FPHZ(seat_index, reason);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}
	public boolean handler_game_finish_FPHZ(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
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

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int left_card_count = GRR._left_card_count;
			int cards[];
			cards = new int[left_card_count];
			int k = 0;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
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
				game_end.addCardsData(cs);// 牌
				int all_hu_xi = 0;
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				// for (int j = 0; j < GRR._weave_count[i]; j++) {
				// WeaveItemResponse.Builder weaveItem_item =
				// WeaveItemResponse.newBuilder();
				// weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				// weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				// weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				// all_hu_xi += GRR._weave_items[i][j].hu_xi;
				// weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
				// if (j < GRR._weave_count[i]) {
				// weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				// } else {
				// weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
				// }
				//
				// weaveItem_array.addWeaveItem(weaveItem_item);
				// }
				// 手牌中的组合牌

				if (_hu_weave_count[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
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
		if(!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}
	public boolean handler_game_finish_PHZ(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
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

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int left_card_count = GRR._left_card_count;
			int cards[] = new int[20];
			int k = 0;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
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
				game_end.addCardsData(cs);// 牌
				int all_hu_xi = 0;
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				// for (int j = 0; j < GRR._weave_count[i]; j++) {
				// WeaveItemResponse.Builder weaveItem_item =
				// WeaveItemResponse.newBuilder();
				// weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				// weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				// weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				// all_hu_xi += GRR._weave_items[i][j].hu_xi;
				// weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
				// if (j < GRR._weave_count[i]) {
				// weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				// } else {
				// weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
				// }
				//
				// weaveItem_array.addWeaveItem(weaveItem_item);
				// }
				// 手牌中的组合牌

				if (_hu_weave_count[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
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
		if(!is_sys()) {
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
		room_info.setBankerPlayer(this._banker_select);
		room_info.setCreateName(this.getRoom_owner_name());

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	public boolean handler_game_finish_hh(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
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

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int cards[] = new int[20];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
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
				game_end.addCardsData(cs);// 牌
				int all_hu_xi = 0;
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				// for (int j = 0; j < GRR._weave_count[i]; j++) {
				// WeaveItemResponse.Builder weaveItem_item =
				// WeaveItemResponse.newBuilder();
				// weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				// weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				// weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				// all_hu_xi += GRR._weave_items[i][j].hu_xi;
				// weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
				// if (j < GRR._weave_count[i]) {
				// weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				// } else {
				// weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
				// }
				//
				// weaveItem_array.addWeaveItem(weaveItem_item);
				// }
				// 手牌中的组合牌

				if (_hu_weave_count[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
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
		if(!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int seat_index,
			int provider_index, int cur_card, ChiHuRight chiHuRight, int card_type,int hu_xi[]) {

		if (GameConstants.GAME_TYPE_HH_YX == _game_type_index) {
			return analyse_chi_hu_card_hh(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card,
					chiHuRight, card_type,hu_xi);
		}
		if (GameConstants.GAME_TYPE_PHZ_YX == _game_type_index) {
			return analyse_chi_hu_card_phz(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card,
					chiHuRight, card_type,hu_xi);
		}
		if (GameConstants.GAME_TYPE_FPHZ_YX == _game_type_index) {
			return analyse_chi_hu_card_fphz(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card,
					chiHuRight, card_type,hu_xi);
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
	public int analyse_chi_hu_card_fphz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index,
			int provider_index, int cur_card, ChiHuRight chiHuRight, int card_type,int hu_xi_pao[]) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS);
		boolean bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index,
				cur_card, analyseItemArray, false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_SAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
							provider_index, cur_card, analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 5; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_SAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j],
										analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			boolean b_sao = false;
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

			}
			int sao_index = analyseItemArray.size();
			boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
					provider_index, cur_card, analyseItemArray, false, hu_xi);
			if (sao_index < analyseItemArray.size()) {
				bValue = temp_bValue;
				for (; sao_index < analyseItemArray.size(); sao_index++) {
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem = analyseItemArray.get(sao_index);
					for (int j = 0; j < 5; j++) {
						if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
							analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
							analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
							analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
							break;
						}
					}
				}

			}
		}

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			all_cards_count = _logic.calculate_all_pai_count(analyseItem);
			hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
			hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
			for (int j = 0; j < 5; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}
		if (max_hu_xi < 12) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		hu_xi_pao[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		for (int j = 0; j < 5; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if(this.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)){
			if((this.GRR._left_card_count == 23)&&(GRR._banker_player != seat_index))
			{
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
				return cbChiHuKind;
			}
			if((this.GRR._left_card_count == 23)&&(GRR._banker_player == seat_index))
			{
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}
				return cbChiHuKind;
			}
		}else{
			if((this.GRR._left_card_count == 22)&&(GRR._banker_player!= seat_index))
			{
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
				return cbChiHuKind;
			}
			if((this.GRR._left_card_count == 23)&&(GRR._banker_player == seat_index))
			{
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}
				return cbChiHuKind;
			}
		}
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
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
	public int analyse_chi_hu_card_phz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index,
			int provider_index, int cur_card, ChiHuRight chiHuRight, int card_type,int hu_xi_pao[]) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS);
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index,
				cur_card, analyseItemArray, false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_SAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
							provider_index, cur_card, analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_SAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j],
										analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			boolean b_sao = false;
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

			}
			int sao_index = analyseItemArray.size();
			boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
					provider_index, cur_card, analyseItemArray, false, hu_xi, yws_type);
			if (sao_index < analyseItemArray.size()) {
				bValue = temp_bValue;
				for (; sao_index < analyseItemArray.size(); sao_index++) {
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem = analyseItemArray.get(sao_index);
					for (int j = 0; j < 7; j++) {
						if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
							analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
							analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
							analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
							break;
						}
					}
				}

			}
		}

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			all_cards_count = _logic.calculate_all_pai_count(analyseItem);
			hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
			hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
			if (max_hu_xi < 18) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		if (max_hu_xi < 15) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		hu_xi_pao[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
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
	public int analyse_chi_hu_card_hh(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index,
			int provider_index, int cur_card, ChiHuRight chiHuRight, int card_type,int hu_xi_hh[]) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index,
				cur_card, analyseItemArray, false, hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_SAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
							provider_index, cur_card, analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_SAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j],
										analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			boolean b_sao = false;
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

			}
			int sao_index = analyseItemArray.size();
			boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index,
					provider_index, cur_card, analyseItemArray, false, hu_xi);
			if (sao_index < analyseItemArray.size()) {
				bValue = temp_bValue;
				for (; sao_index < analyseItemArray.size(); sao_index++) {
					AnalyseItem analyseItem = new AnalyseItem();
					analyseItem = analyseItemArray.get(sao_index);
					for (int j = 0; j < 7; j++) {
						if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
							analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
							analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
							analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
							break;
						}
					}
				}

			}
		}

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			
			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}
		if (max_hu_xi < 15) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}
		if (hong_pai_count >= 10 && hong_pai_count < 13) {
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
		}
		if (hong_pai_count >= 13) {
			chiHuRight.opr_or(GameConstants.CHR_THIRTEEN_HONG_PAI);
		}
		if (hong_pai_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
		}
		if (hei_pai_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HEI);
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
		}

		return cbChiHuKind;
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

	// 玩家出版的动作检测 提龙，扫
	public int estimate_player_ti_sao_respond_hh(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG,
					GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_SAO))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG,
						GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false, 1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL)
				&& (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_SAO;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_SAO;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false,
					1000);
			bAroseAction = GameConstants.WIK_SAO;
		}
		return bAroseAction;
	}

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_hh(int seat_index, int provider_index, int card_data, int pao_type[],
			boolean dispatch) {
		// 变量定义
		int bAroseAction = GameConstants.WIK_NULL;// 出现(是否)有
		pao_type[0] = 0;

		// 碰转跑
		if ((bAroseAction == GameConstants.WIK_NULL) && (dispatch == true)) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_PENG))
					continue;
				// this.exe_gang(seat_index, seat_index, card_data,
				// GameConstants.WIK_PAO,GameConstants.PAO_TYPE_MINE_PENG_PAO ,
				// true, false);
				pao_type[0] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}
		// 跑牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (seat_index != provider_index)) {
			if (_logic.check_pao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
				// this.exe_gang(seat_index, seat_index, card_data,
				// GameConstants.WIK_PAO,GameConstants.PAO_TYPE_OHTER_PAO ,
				// true, false);
				pao_type[0] = GameConstants.PAO_TYPE_OHTER_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}
		// 扫转跑
		if (seat_index != provider_index) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_SAO))
					continue;
				// this.exe_gang(seat_index, seat_index, card_data,
				// GameConstants.WIK_PAO,GameConstants.PAO_TYPE_MINE_PENG_PAO ,
				// true, false);
				pao_type[0] = GameConstants.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}

		}

		//
		return bAroseAction;
	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_hh(int seat_index, int card, boolean bDisdatch) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		//
		// _playerStatus[i].clean_action();
		// _playerStatus[i].clean_weave();
		// }

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		int next_index = 0;
		if (bDisdatch != true)
			next_index++;
		int player_pass[] = new int[this.getTablePlayerNumber()];
		int cur_card_index = _logic.switch_to_card_index(card);
		// _lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		// for(int i = 0; i<this.getTablePlayerNumber();i++){
		// for(int j = 0;j<7;j++)
		// _lou_weave_item[i][j] = new LouWeaveItem();
		// }

		do {

			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index -1+getTablePlayerNumber())%getTablePlayerNumber();
			next_index++;
			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			for (; chi_index < this._cannot_chi_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card) {
					break;
				}
			}

			if (chi_index != this._cannot_chi_count[chi_seat_index])
				continue;
			for(chi_index = 0; chi_index < GRR._discard_count[last_index];chi_index++){
				if(GRR._discard_cards[last_index][chi_index] == card)
				{
					break;
				}
			}
			if(chi_index != GRR._discard_count[last_index])
				continue;
			int card_count = 0;

			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (this.GRR._cards_index[chi_seat_index][j] < 3)
					card_count += this.GRR._cards_index[chi_seat_index][j];
			}
			if (card_count == 2)
				continue;

			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				int type_count[] = new int[10];
				int type_eat_count[] = new int[1];
				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count);

				if ((action & GameConstants.WIK_LEFT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index + 2]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_LEFT, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 0, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index,
								_lou_weave_item[chi_seat_index][0].nCount,
								_lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index - 1]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_CENTER, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 1, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount,
								_lou_weave_item[chi_seat_index][1].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index - 1]--;
						temp_cards_index[cur_card_index - 2]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 2, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index,
								_lou_weave_item[chi_seat_index][2].nCount,
								_lou_weave_item[chi_seat_index][2].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_XXD) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						if (_logic.get_card_color(card) == 0) {
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index + 10]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_XXD,
										type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 4,
										card_count - 3);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_XXD, type_count,
									type_eat_count[0], _lou_weave_item[chi_seat_index], 4, card_count - 3);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index,
								_lou_weave_item[chi_seat_index][4].nCount,
								_lou_weave_item[chi_seat_index][4].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_DDX) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						if (_logic.get_card_color(card) == 0) {
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index + 10]--;
							temp_cards_index[cur_card_index + 10]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_DDX, type_count,
									type_eat_count[0], _lou_weave_item[chi_seat_index], 5, card_count - 3);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_DDX,
										type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 5,
										card_count - 3);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index,
								_lou_weave_item[chi_seat_index][5].nCount,
								_lou_weave_item[chi_seat_index][5].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_EQS) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						int index[] = { 1, 6, 9 };
						int temp_index = ((_logic.get_card_color(card) == 1) ? 10 : 0);
						temp_cards_index[temp_index + index[0]]--;
						temp_cards_index[temp_index + index[1]]--;
						temp_cards_index[temp_index + index[2]]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_EQS, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 3, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index,
								_lou_weave_item[chi_seat_index][3].nCount,
								_lou_weave_item[chi_seat_index][3].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}
		} while (next_index < 2);

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {

				if (this.GRR._cards_index[i][j] < 3)
					card_count += this.GRR._cards_index[i][j];
			}

			if (card_count == 2)
				continue;
			int peng_index = 0;
			for (; peng_index < this._cannot_peng_count[i]; peng_index++) {
				if (this._cannot_peng[i][peng_index] == card) {
					break;
				}
			}
			if (peng_index != this._cannot_peng_count[i])
				continue;
			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {

					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
					player_pass[i] = 1;
				}
			}

		}
		for (int i = 0; i < 3; i++) {
			playerStatus = _playerStatus[i];
			if (player_pass[i] == 1) {
				_playerStatus[i].add_action(GameConstants.WIK_NULL);
				_playerStatus[i].add_pass(card, seat_index);

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
	public boolean estimate_player_out_card_respond_PHZ(int seat_index, int card, boolean bDisdatch) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		//
		// _playerStatus[i].clean_action();
		// _playerStatus[i].clean_weave();
		// }

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		int next_index = 0;
		if (bDisdatch != true)
			next_index++;
		int player_pass[] = new int[this.getTablePlayerNumber()];
		int cur_card_index = _logic.switch_to_card_index(card);
		// _lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		// for(int i = 0; i<this.getTablePlayerNumber();i++){
		// for(int j = 0;j<7;j++)
		// _lou_weave_item[i][j] = new LouWeaveItem();
		// }

		do {

			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index -1+getTablePlayerNumber())%getTablePlayerNumber();
			
			next_index++;
			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			for (; chi_index < this._cannot_chi_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card) {
					break;
				}
			}

			if (chi_index != this._cannot_chi_count[chi_seat_index])
				continue;
			for(chi_index = 0; chi_index < GRR._discard_count[last_index];chi_index++){
				if(GRR._discard_cards[last_index][chi_index] == card)
				{
					break;
				}
			}
			if(chi_index != GRR._discard_count[last_index])
				continue;
			int card_count = 0;

			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (this.GRR._cards_index[chi_seat_index][j] < 3)
					card_count += this.GRR._cards_index[chi_seat_index][j];
			}
			if (card_count == 2)
				continue;

			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				int type_count[] = new int[10];
				int type_eat_count[] = new int[1];
				boolean yws_type = has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS);
				action = _logic.check_chi_phz(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count,
						yws_type);

				if ((action & GameConstants.WIK_LEFT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index + 2]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_LEFT, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 0, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index,
								_lou_weave_item[chi_seat_index][0].nCount,
								_lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index - 1]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_CENTER,
								type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 1, card_count - 3,
								yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount,
								_lou_weave_item[chi_seat_index][1].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index - 1]--;
						temp_cards_index[cur_card_index - 2]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_RIGHT,
								type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 2, card_count - 3,
								yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index,
								_lou_weave_item[chi_seat_index][2].nCount,
								_lou_weave_item[chi_seat_index][2].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_XXD) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						if (_logic.get_card_color(card) == 0) {
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index + 10]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD,
										type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 4,
										card_count - 3, yws_type);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD,
									type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 4, card_count - 3,
									yws_type);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index,
								_lou_weave_item[chi_seat_index][4].nCount,
								_lou_weave_item[chi_seat_index][4].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_DDX) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						if (_logic.get_card_color(card) == 0) {
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index + 10]--;
							temp_cards_index[cur_card_index + 10]--;
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX,
									type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 5, card_count - 3,
									yws_type);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX,
										type_count, type_eat_count[0], _lou_weave_item[chi_seat_index], 5,
										card_count - 3, yws_type);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index,
								_lou_weave_item[chi_seat_index][5].nCount,
								_lou_weave_item[chi_seat_index][5].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_EQS) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						int index[] = { 1, 6, 9 };
						int temp_index = ((_logic.get_card_color(card) == 1) ? 10 : 0);
						temp_cards_index[temp_index + index[0]]--;
						temp_cards_index[temp_index + index[1]]--;
						temp_cards_index[temp_index + index[2]]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_EQS, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 3, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index,
								_lou_weave_item[chi_seat_index][3].nCount,
								_lou_weave_item[chi_seat_index][3].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_YWS) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						int index[] = { 0, 4, 9 };
						int temp_index = ((_logic.get_card_color(card) == 1) ? 10 : 0);
						temp_cards_index[temp_index + index[0]]--;
						temp_cards_index[temp_index + index[1]]--;
						temp_cards_index[temp_index + index[2]]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_YWS, type_count,
								type_eat_count[0], _lou_weave_item[chi_seat_index], 6, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_YWS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_YWS, seat_index,
								_lou_weave_item[chi_seat_index][6].nCount,
								_lou_weave_item[chi_seat_index][6].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}
			// 结果判断
			if (_playerStatus[chi_seat_index].has_action()) {
				bAroseAction = true;
			}

		} while (next_index < 2);

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {

				if (this.GRR._cards_index[i][j] < 3)
					card_count += this.GRR._cards_index[i][j];
			}

			if (card_count == 2)
				continue;
			int peng_index = 0;
			for (; peng_index < this._cannot_peng_count[i]; peng_index++) {
				if (this._cannot_peng[i][peng_index] == card) {
					break;
				}
			}
			if (peng_index != this._cannot_peng_count[i])
				continue;
			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {

					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
					player_pass[i] = 1;
				}
			}

		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			playerStatus = _playerStatus[i];
			if (player_pass[i] == 1) {
				_playerStatus[i].add_action(GameConstants.WIK_NULL);
				_playerStatus[i].add_pass(card, seat_index);

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
	public boolean estimate_gang_respond_hh(int seat_index, int card, int _action) {
		// 变量定义
		boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张
																			// 不算抢杠胡

		boolean bAroseAction = false;// 出现(是否)有

		

		return bAroseAction;
	}

	// 添加落牌组合
	public boolean add_lou_weave(int luoCode, int target_player, int target_card, int provide_player,
			LouWeaveItem lou_weave_item) {
		boolean bSuccess = false;
		if (lou_weave_item.nCount < luoCode)
			return false;
		if (luoCode < 0)
			return false;
		for (int i = 0; i < 2; i++) {
			switch (lou_weave_item.nLouWeaveKind[luoCode][i]) {
			case GameConstants.WIK_NULL:
				break;
			case GameConstants.WIK_LEFT: {
				int cbRemoveCard[] = new int[] { target_card, target_card + 1, target_card + 2 };
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_LEFT;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_CENTER: {
				int cbRemoveCard[] = new int[] { target_card, target_card - 1, target_card + 1 };
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_CENTER;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int cbRemoveCard[] = new int[] { target_card, target_card - 1, target_card - 2 };
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_RIGHT;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_DDX: {
				// 删除扑克
				int target_card_color = this._logic.get_card_color(target_card);

				int cbRemoveCard[] = new int[3];
				if (target_card_color == 0) {
					cbRemoveCard[0] = target_card + 16;
					cbRemoveCard[1] = target_card + 16;
				} else {
					cbRemoveCard[0] = target_card - 16;
					cbRemoveCard[1] = target_card;
				}
				cbRemoveCard[2] = target_card;
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_DDX;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_XXD: {
				// 删除扑克
				int target_card_color = this._logic.get_card_color(target_card);

				int cbRemoveCard[] = new int[3];
				if (target_card_color == 0) {
					cbRemoveCard[0] = target_card;
					cbRemoveCard[1] = target_card + 16;
				} else {
					cbRemoveCard[0] = target_card - 16;
					cbRemoveCard[1] = target_card - 16;
				}
				cbRemoveCard[2] = target_card;
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_XXD;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_EQS: {
				// 删除扑克
				int cbRemoveCard[] = new int[] { target_card, target_card, target_card };
				int target_card_value = this._logic.get_card_value(target_card);
				switch (target_card_value) {
				case 2:
					cbRemoveCard[0] = target_card + 5;
					cbRemoveCard[1] = target_card + 8;
					break;
				case 7:
					cbRemoveCard[0] = target_card - 5;
					cbRemoveCard[1] = target_card + 3;
					break;
				case 10:
					cbRemoveCard[0] = target_card - 8;
					cbRemoveCard[1] = target_card - 3;
					break;

				default:
					break;
				}
				cbRemoveCard[2] = target_card;
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_EQS;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_YWS: {
				// 删除扑克
				int cbRemoveCard[] = new int[] { target_card, target_card, target_card };
				int target_card_value = this._logic.get_card_value(target_card);
				switch (target_card_value) {
				case 1:
					cbRemoveCard[0] = target_card + 4;
					cbRemoveCard[1] = target_card + 9;
					break;
				case 5:
					cbRemoveCard[0] = target_card - 4;
					cbRemoveCard[1] = target_card + 5;
					break;
				case 10:
					cbRemoveCard[0] = target_card - 9;
					cbRemoveCard[1] = target_card - 5;
					break;

				default:
					break;
				}
				cbRemoveCard[2] = target_card;
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_YWS;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic
						.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			}
		}
		return bSuccess;
	}

	// 检查长沙麻将,杠牌
	public boolean estimate_gang_hh_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		

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

		// 机器人开房
		if (type == GameConstants.CREATE_ROOM_ROBOT) {
			GameSchedule.put(new CreateTimeOutRunnable(this.getRoom_id()), GameConstants.CREATE_ROOM_PROXY_TIME_GAP,
					TimeUnit.MINUTES);

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(LogicRoomHandler.getMaxNumber(this));
			roomRedisModel.setGame_round(this._game_round);
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

			return true;
		}

		// c成功
		get_players()[0] = player;
		player.set_seat_index(0);

		// 发送进入房间
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		//
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);

		return send_response_to_player(player.get_seat_index(), roomResponse);
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {

		int seat_index = GameConstants.INVALID_SEAT;

		if (playerNumber == 0) {// 未开始 才分配位置
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
			send_error_notify(player, 1, "游戏已经开始");
			return false;
		}

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
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
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);

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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(i, roomResponse2);
		}

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				count++;
			}
		}
		// 牌桌不是3人
		if (count != (this.getTablePlayerNumber() - 1)) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}

		if (openLess == readys && readys == this.getTablePlayerNumber() - 1) {
			playerNumber = readys;
			handler_game_start();

			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
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
			return handler_player_ready(get_seat_index);
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index) {
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
		if(is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
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
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		this.log_error("gme_status:"+this._game_status+" seat_index:" +seat_index);
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status)
				&& this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			this.log_error("gme_status:"+this._game_status+"GS_MJ_WAIT  seat_index:" +seat_index);
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

		return handler_player_ready(seat_index);

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
	public  boolean handler_call_banker(int seat_index,int call_banker)
	{
		return true;
	}
	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public  boolean handler_add_jetton( int seat_index, int jetton){
		return true;
	}
	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public  boolean handler_open_cards(int seat_index,boolean open_flag){
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

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1,
					GameConstants.INVALID_SEAT);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1,
					GameConstants.INVALID_SEAT);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1,
					GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		// this.operate_player_cards(seat_index, 0, null, 0, null);

		// if (rm) {
		// // 把摸的牌从手牌删掉,结算的时候不显示这张牌的
		// GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		// }

		// 显示胡牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i],
					this.GRR._weave_count[i], GameConstants.INVALID_SEAT);

		}
		return;
	}

	/**
	 * 长沙
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate_hh(int seat_index, int operate_card[], int card_count, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_HH_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards,

				this.GRR._weave_items[seat_index], this.GRR._weave_count[seat_index], GameConstants.INVALID_SEAT);

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
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
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
	public void process_chi_hu_player_score_hh(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr,seat_index);
		// 计算胡息
		int all_hu_xi = 0;
		// for(int i =0; i< this.GRR._weave_count[seat_index] ; i++){
		// all_hu_xi += this.GRR._weave_items[seat_index][i].hu_xi;
		// }
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = 1 + (all_hu_xi - 15) / 3;
		if(seat_index == provide_index){
			calculate_score +=1;
		}
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_ONE))
			calculate_score = calculate_score * 1;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_TWO))
			calculate_score = calculate_score * 2;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_THREE))
			calculate_score = calculate_score * 3;

		int wFanShu = _logic.get_chi_hu_action_rank_hh(chr);// 番数

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
		}

		float lChiHuScore = wFanShu * calculate_score;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;

			}
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
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr,seat_index);
		// 计算胡息
		int all_hu_xi = 0;
		// for(int i =0; i< this.GRR._weave_count[seat_index] ; i++){
		// all_hu_xi += this.GRR._weave_items[seat_index][i].hu_xi;
		// }
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = 0;
		if(is_mj_type(GameConstants.GAME_TYPE_PHZ_YX))
		{
			if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
				calculate_score = 1 + (all_hu_xi - 18) / 3;
			} else {
				calculate_score = 1 + (all_hu_xi - 15) / 3;
			}
		}
		else if(is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
		{
			calculate_score = 1 + (all_hu_xi - 12) / 3;
		}
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_ONE))
			calculate_score = calculate_score * 1;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_TWO))
			calculate_score = calculate_score * 2;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_THREE))
			calculate_score = calculate_score * 3;

		// int wFanShu = _logic.get_chi_hu_action_rank_hh(chr);// 番数

		// 统计
		// if (zimo) {
		// // 自摸
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// if (i == seat_index) {
		// // GRR._hu_result[i] = MJGameConstants.HU_RESULT_ZIMO;
		// continue;
		// }
		// GRR._lost_fan_shu[i][seat_index] = wFanShu;
		// }
		// }

		float lChiHuScore = calculate_score;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;

			}
		}

	}

	@Override
	public Player get_player(long account_id) {
		Player player = null;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player != null && player.getAccount_id() == account_id) {
				return player;
			}
		}

		return null;
	}

	@Override
	public boolean handler_audio_chat(Player player, com.google.protobuf.ByteString chat, int l, float audio_len) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_AUDIO_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setAudioChat(chat);
		roomResponse.setAudioSize(l);
		roomResponse.setAudioLen(audio_len);
//		this.log_error("nickname = "+player.getNick_name()+"audio_len=" +audio_len + "time="+System.currentTimeMillis());
		this.send_response_to_other(player.get_seat_index(), roomResponse);
		return true;
	}

	@Override
	public boolean handler_emjoy_chat(Player player, int id) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EMJOY_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setEmjoyId(id);
		this.send_response_to_room(roomResponse);

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
		if (is_mj_type(GameConstants.GAME_TYPE_HENAN_AY)) {
			roomResponse.setFlashTime(250);
			roomResponse.setStandTime(1500);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(1000);

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
	public boolean operate_show_card(int seat_index, int type, int count, int cards[], WeaveItem weaveitems[],
			int weave_count, int to_player) {
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
		int di_cards[];
		di_cards= new int[GRR._left_card_count];
		int k = 0;
		int left_card_count = GRR._left_card_count;
		for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
			di_cards[k] = _repertory_card[_all_card_len - left_card_count];
			roomResponse.addCardsList(di_cards[k]);
			k++;
			left_card_count--;
		}
		// 已有 组合牌
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);

				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		// 手牌中的组合牌
		//
		// if (hu_weave_count > 0) {
		// for (int j = 0; j < hu_weave_count; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(hu_weave_items[j].provide_player);
		// weaveItem_item.setPublicCard(hu_weave_items[j].public_card);
		// weaveItem_item.setWeaveKind(hu_weave_items[j].weave_kind);
		// weaveItem_item.setHuXi(weaveitems[j].hu_xi);
		// all_hu_xi += hu_weave_items[j].hu_xi;
		// weaveItem_item.setCenterCard(hu_weave_items[j].center_card);
		// roomResponse.addWeaveItems(weaveItem_item);
		// }
		// }
		//
		GRR.add_room_response(roomResponse);
		// roomResponse.setHuXiCount(all_hu_xi);
		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	// 添加牌到牌队
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

	/**
	 * 听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @returnt
	 */
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int seat_index, int provate_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index,
					provate_index, cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO,hu_xi)) {
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
	 * 胡息 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param hu_xi_kind_type
	 * @param hu_xi_count
	 * @param time
	 * @param to_player
	 * @return
	 */
	// public boolean operate_update_hu_xi(int seat_index, int
	// hu_xi_kind_type,int hu_xi_count, int time, int to_player) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_UPDATE_HU_XI);
	// roomResponse.setHuXiType(hu_xi_kind_type);
	// roomResponse.setHuXiCount(hu_xi_count);
	// roomResponse.setEffectTime(time);
	//
	// if (to_player == GameConstants.INVALID_SEAT) {
	// GRR.add_room_response(roomResponse);
	// this.send_response_to_room(roomResponse);
	// } else {
	// this.send_response_to_player(to_player, roomResponse);
	// }
	//
	// return true;
	// }

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
			weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
					| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS | GameConstants.WIK_YWS;
//			this.log_error("weave.kind" + curPlayerStatus._action_weaves[i].weave_kind + "center_card"
//					+ curPlayerStatus._action_weaves[i].center_card);
     		if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {
//							this.log_error("lou_qi_weave.kind" + curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);

							chi_group.addLouWeaveKind(curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);
						}
					}
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}
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
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player, boolean sao) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(1000);
			roomResponse.setInsertTime(150);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(1000);
			roomResponse.setInsertTime(150);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(1000);
			roomResponse.setInsertTime(150);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			if (sao == true) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);// 给别人 牌背
				}
			} else {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);// 给别人 牌数据
				}
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
//				GRR.add_room_response(roomResponse);
				return this.send_response_to_player(seat_index, roomResponse);
			} else {
				if (sao == true) {
					for (int i = 0; i < count; i++) {
						roomResponse.addCardData(GameConstants.BLACK_CARD);// 给别人 牌背
					}
				} else {
					for (int i = 0; i < count; i++) {
						roomResponse.addCardData(cards[i]);// 给别人 牌数据
					}
				}
//				GRR.add_room_response(roomResponse);
				return this.send_response_to_special(seat_index, to_player, roomResponse);
			}
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
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				if ((weaveitems[j].weave_kind == GameConstants.WIK_TI_LONG
						|| weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG) && weaveitems[j].public_card == 0) {
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

	public boolean operate_player_xiang_gong_flag(int seat_index, boolean is_xiang_gong) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_XIANGGONG);
		roomResponse.setProvidePlayer(seat_index);
		roomResponse.setIsXiangGong(is_xiang_gong);
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
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
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				// weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
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
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index, false);
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

	// /**
	// * //执行发牌 是否延迟
	// *
	// * @param seat_index
	// * @param delay
	// * @return
	// */
	// public boolean exe_dispatch_last_card(int seat_index, int type, int
	// delay_time) {
	//
	// if (delay_time > 0) {
	// GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(),
	// seat_index, type, false), delay_time,
	// TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
	// } else {
	// // 发牌
	// this._handler = this._handler_dispath_last_card;
	// this._handler_dispath_last_card.reset_status(seat_index, type);
	// this._handler.exe(this);
	// }
	//
	// return true;
	// }
	/**
	 * //执行发牌 首牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_dispatch_first_card(int seat_index, int type, int delay_time) {
		if (delay_time > 0) {
			GameSchedule.put(new DispatchFirstCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_dispath_firstcards;
			this._handler_dispath_firstcards.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * //处理首牌
	 * 
	 * @param seat_index
	 * @param delay_time
	 * @return
	 */
	public boolean exe_chuli_first_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new ChulifirstCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
					TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_chuli_firstcards;
			this._handler_chuli_firstcards.reset_status(seat_index, type);
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());

		return false;

	}

	public boolean send_error_notify(Player player, int type, String msg) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;

	}

	public boolean send_response_to_other(int seat_index, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			player = this.get_players()[i];
			if (player == null)
				continue;
			if (i == seat_index)
				continue;
			Response.Builder responseBuilder = Response.newBuilder();
			responseBuilder.setResponseType(ResponseType.ROOM);
			responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());
//			this.log_error("nickname = "+this.get_players()[seat_index].getNick_name() + "nickname =  "+this.get_players()[i].getNick_name() +roomResponse.getAudioLen()+ "time=" + System.currentTimeMillis());
			PlayerServiceImpl.getInstance().send(this.get_players()[i], responseBuilder.build());
		}

		return true;
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

	private void progress_banker_select() {
		if (_banker_select == GameConstants.INVALID_SEAT) {
			_banker_select = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJthis.getTablePlayerNumber();//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJthis.getTablePlayerNumber();

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

		_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

		// 设置战绩游戏ID
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			_gameRoomRecord.setGame_id(GameConstants.GAME_ID_HH_YX);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			_gameRoomRecord.setGame_id(GameConstants.GAME_ID_HH_YX);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			_gameRoomRecord.setGame_id(GameConstants.GAME_ID_HH_YX);
		}
		_recordRoomRecord = MongoDBServiceImpl.getInstance().parentBrand(_gameRoomRecord.getGame_id(),
				this.get_record_id(), "", _gameRoomRecord.to_json(), (long) this._game_round,
				(long) this._game_type_index, this.getRoom_id() + "", getRoom_owner_account_id());

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			MongoDBServiceImpl.getInstance().accountBrand(_gameRoomRecord.getGame_id(),
					this.get_players()[i].getAccount_id(), this.get_record_id(), getRoom_owner_account_id());
		}

	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	private void record_game_round(GameEndResponse.Builder game_end) {
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setMsg(_gameRoomRecord.to_json());
			MongoDBServiceImpl.getInstance().updateParenBrand(_recordRoomRecord);
		}

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			String stl = String.valueOf(id);
			game_end.setBrandIdStr(stl);
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			// 记录 to mangodb
			MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
					null, null, gzipByte, this.getRoom_id() + "",
					_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(), getRoom_owner_account_id());

		}

		if ((cost_dou > 0) && (this._cur_round == 1)) {
			// 不是正常结束的
			if ((game_end.getEndType() != GameConstants.Game_End_NORMAL)
					&& (game_end.getEndType() != GameConstants.Game_End_DRAW)) {
				// 还豆
				StringBuilder buf = new StringBuilder();
				buf.append("开局失败[" + game_end.getEndType() + "]" + ":" + this.getRoom_id())
						.append("game_id:" + this.getGame_id()).append(",game_type_index:" + _game_type_index)
						.append(",game_round:" + _game_round).append(",房主:" + this.getRoom_owner_account_id())
						.append(",豆+:" + cost_dou);
				// 把豆还给玩家
				AddGoldResultModel result = PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
						cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
				if (result.isSuccess() == false) {
					logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" + this.getRoom_owner_account_id() + "]还豆失败");
				}

			}
		}

	}

	public void log_error(String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + error);

	}

	public void log_player_error(int seat_index, String error) {

		logger.error("房间[" + this.getRoom_id() + "]" + " 玩家[" + seat_index + "]" + error);

	}
	private void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtianhu, "", 0, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdihu, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhyidianhong, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhonghu, "", 0,
						0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhongfantian, "", 0, 0l,
						this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhallhei, "", 0, 0l,
						this.getRoom_id());
			}


		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	/**
	 * 结算描述
	 */
	private void set_result_describe_hh() {
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
					if (type == GameConstants.CHR_TIAN_HU) {
						des += ",天胡";
					}
					if(is_mj_type(GameConstants.GAME_TYPE_PHZ_YX))
					{
						if(type == GameConstants.CHR_JEI_PAO_HU)
						{
							des += ",平胡";
						}
					}
					if(is_mj_type(GameConstants.GAME_TYPE_HH_YX))
					{
						if (type == GameConstants.CHR_JEI_PAO_HU) {
							des += ",平胡";
						}
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸  +3硬息";
						}

					}
					if(is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)){
						if(type == GameConstants.CHR_DI_HU){
							des += ",地胡";
						}
					}
					if(is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)&&is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸 ";
						}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += ",红胡×2";
					}

					if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
						des += ",红翻天×4";
					}
					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",红一粒珠×3";
					}
					if (type == GameConstants.CHR_ONE_HEI) {
						des += ",黑一粒珠×3";
					}
					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",全黑×5";
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	private void set_result_describe() {
		if (this.is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			set_result_describe_hh();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			set_result_describe_hh();
		}
//		 if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
//		 set_result_describe_hh();
//		 }
	}

	public String get_game_des() {
		return GameDescUtil.getGameDesc(_game_type_index, _game_rule_index);
	}



	public boolean is_zhuang_xian() {
		if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI) || is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			return false;
		}
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
	public boolean exe_finish() {

		this._handler = this._handler_finish;
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
			this._handler = this._handler_dispath_card;
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
			// 是否有抢杠胡
			this._handler = this._handler_gang;
			this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, depatch, self, d);
			this._handler.exe(this);
		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type,
			boolean depatch, boolean self, boolean d) {
		// 是否有抢杠胡
		this._handler = this._handler_gang;
		this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d, depatch);
		this._handler.exe(this);
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
		// 出牌
		this._handler = this._handler_out_card_operate;
		this._handler_out_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_chi_peng;
		this._handler_chi_peng.reset_status(seat_index, _out_card_player, action, card, type);
		this._handler.exe(this);

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
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, this.GRR._weave_items[seat_index],
				this.GRR._weave_count[seat_index], GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

	public boolean handler_requst_location(Player player, LocationInfor locationInfor) {
		// LocationUtil.LantitudeLongitudeDist(lon1, lat1, lon2, lat2);
		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		// 发牌
		this._handler = this._handler_chuli_firstcards;
		this._handler_chuli_firstcards.reset_status(_seat_index, _type);
		this._handler.exe(this);
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		this._handler = this._handler_dispath_firstcards;
		this._handler_dispath_firstcards.reset_status(_seat_index, _type);
		this._handler.exe(this);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if(is_mj_type(GameConstants.GAME_TYPE_HH_YX))
			return GameConstants.GAME_PLAYER_HH;
		if(is_mj_type(GameConstants.GAME_TYPE_PHZ_YX))
			return GameConstants.GAME_PLAYER_HH;
		if(is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
		{
			return GameConstants.GAME_PLAYER_FPHZ;
		}
		return GameConstants.GAME_PLAYER;
	}

	public boolean refresh_room_redis_data(int type, boolean notifyRedis) {

		if (type == GameConstants.PROXY_ROOM_UPDATE) {
			int cur_player_num = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] != null) {
					cur_player_num++;
				}
			}

			// 写入redis
			RedisService redisService = SpringService.getBean(RedisService.class);
			RoomRedisModel roomRedisModel = SpringService.getBean(RedisService.class).hGet(RedisConstant.ROOM,
					getRoom_id() + "", RoomRedisModel.class);
			roomRedisModel.setGameRuleDes(this.get_game_des());
			roomRedisModel.setRoomStatus(this._game_status);
			roomRedisModel.setPlayer_max(LogicRoomHandler.getMaxNumber(this));
			roomRedisModel.setCur_player_num(cur_player_num);
			roomRedisModel.setGame_round(this._game_round);
			// roomRedisModel.setCreate_time(System.currentTimeMillis());
			redisService.hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

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
		int game_type_index = this._game_type_index;
		//收费索引
		this.game_index = SysGameTypeEnum.getGameGoldTypeIndex(game_type_index);
		int game_id=SysGameTypeEnum.getGameIDByTypeIndex(game_type_index);
		this.setGame_id(SysGameTypeEnum.getGameIDByTypeIndex(game_type_index));
		if (is_sys()) {
			return true;
		}

		// 判断房卡
		SysParamModel sysParamModel1010 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
				.get(1010);
		SysParamModel sysParamModel1011 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
				.get(1011);
		SysParamModel sysParamModel1012 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
				.get(1012);
		int check_gold = 0;
		boolean create_result = true;

		if (_game_round == 4) {
			check_gold = sysParamModel1010.getVal2();
		} else if (_game_round == 8) {
			check_gold = sysParamModel1011.getVal2();
		} else if (_game_round == 16) {
			check_gold = sysParamModel1012.getVal2();
		}

		// 注意游戏ID不一样
		if (check_gold == 0) {
			create_result = false;
		} else {
			// 是否免费的
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id)
					.get(this.game_index);

			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				// 收费
				StringBuilder buf = new StringBuilder();
				buf.append("创建房间:" + this.getRoom_id()).append("game_id:" + this.getGame_id())
						.append(",game_type_index:" + game_type_index).append(",game_round:" + _game_round);
				AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(this.getRoom_owner_account_id(),
						check_gold, false, buf.toString());
				if (result.isSuccess() == false) {
					create_result = false;
				} else {
					// 扣豆成功
					cost_dou = check_gold;
				}
			}

		}
		if (create_result == false) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
			Player player = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.send_response_to_player(i, roomResponse);

				player = this.get_players()[i];
				if (player == null)
					continue;
				if (i == 0) {
					send_error_notify(i, 1, "闲逸豆不足,游戏解散");
				} else {
					send_error_notify(i, 1, "创建人闲逸豆不足,游戏解散");
				}

			}

			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			return false;
		}

		return true;
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

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
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
	public  boolean open_card_timer(){
		return false;
	}
	
	public  boolean robot_banker_timer(){
		return false;
	}
	
	public  boolean ready_timer(){
		return false;
	}
	
	public  boolean add_jetton_timer(){
		return false;
	}
}
