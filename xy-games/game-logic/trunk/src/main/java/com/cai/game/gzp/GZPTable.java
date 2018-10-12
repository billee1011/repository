/**
 * 
 */
package com.cai.game.gzp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.gzp.handler.GZPHandler;
import com.cai.game.gzp.handler.GZPHandlerChiPeng;
import com.cai.game.gzp.handler.GZPHandlerDispatchCard;
import com.cai.game.gzp.handler.GZPHandlerFinish;
import com.cai.game.gzp.handler.GZPHandlerGang;
import com.cai.game.gzp.handler.GZPHandlerOutCardOperate;
import com.cai.game.gzp.handler.GZPHandlerPickUpOperate;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerChiPeng_DDWF;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerDispatchCard_DDWF;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerDispatchFirstCard_DDWF;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerGang_DDWF;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerOutCardOperate_DDWF;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerPiao_DDWF;
import com.cai.game.gzp.handler.gzpddwf.GZPHandlerPickUpCard_DDWF;
import com.cai.game.gzp.handler.gzptc.GZPHandlerChiPeng_TC;
import com.cai.game.gzp.handler.gzptc.GZPHandlerDispatchCard_TC;
import com.cai.game.gzp.handler.gzptc.GZPHandlerDispatchFirstCard_TC;
import com.cai.game.gzp.handler.gzptc.GZPHandlerGang_TC;
import com.cai.game.gzp.handler.gzptc.GZPHandlerGang_TC_DispatchCard;
import com.cai.game.gzp.handler.gzptc.GZPHandlerOutCardOperate_TC;
import com.cai.game.gzp.handler.gzptc.GZPHandlerPiao_TC;
import com.cai.game.gzp.handler.gzptc.GZPHandlerPickUpCard_TC;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class GZPTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(GZPTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;


	public ScheduledFuture _trustee_schedule[];
	// 状态变量
	private boolean _status_send; // 发牌状态
	private boolean _status_gang; // 抢杆状态
	private boolean _status_cs_gang; // 长沙杆状态
	private boolean _status_gang_hou_pao; // 杠后炮状态


	// 运行变量
	public int _provide_card = GameConstants.INVALID_VALUE; // 供应扑克
	public int _resume_player = GameConstants.INVALID_SEAT; // 还原用户
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _provide_player = GameConstants.INVALID_SEAT; // 供应用户

	// 发牌信息
	public int _send_card_data = GameConstants.INVALID_VALUE; // 发牌扑克

	public int _send_card_count = GameConstants.INVALID_VALUE; // 发牌数目


	public int _qiang_GZP_MAX_COUNT; // 最大呛分
	public int _lian_zhuang_player; // 连庄玩家
	public int _shang_zhuang_player; // 上庄玩家

	// 出牌信息
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _out_card_count = GameConstants.INVALID_VALUE; // 出牌数目
	public int _sheng_guan_index [][] ;              // 生观的牌记录
	public int _pick_up_index[][];                   //捡的牌
	public int _game_mid_score[];
	public int _game_weave_score[];
	public WeaveItem _hu_weave_items[][];
	public int _hu_weave_count[]; //
	public int _max_gzp_count[];
	public int _max_hu_count[];
	public int _cell_score;
	public int _flower_count[];
	public int _cannot_pickup_index[][];
	public int _guo_hu[][];
	public int _guo_zhao[][];
	public int _guo_peng[][];
	public int _temp_guan[][];
	public boolean _guo_qi_guan[];

	public GZPGameLogic _logic = null;
	public int _guan_sheng_count;   //观生的数量
	public int _hua_count;  //滑操作
	public int _pu_card[]; 
	public int _pu_count;
	public int _first_card ;
	public int _pick_up_card; 
	/**
	 * 当前牌桌的庄家
	 */
	public int _banker_select = GameConstants.INVALID_SEAT;

	/**
	 * 当前桌子内玩家数量
	 *//*
	private int playerNumber;
*/
	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public GZPHandler _handler;

	public GZPHandlerDispatchCard _handler_dispath_card;
	public GZPHandlerOutCardOperate _handler_out_card_operate;
	public GZPHandlerGang _handler_gang;
	public GZPHandlerChiPeng _handler_chi_peng;
	public GZPHandlerGang_TC_DispatchCard _handler_gang_gzp;
	public GZPHandlerPickUpOperate    _handler_pick_up_card;
	public  GZPHandler                _handler_piao;
	public GZPHandlerDispatchCard _handler_dispath_firstcards;


	// 20张

	public GZPHandlerFinish _handler_finish; // 结束

	public AnalyseItemTwenty[] analyseItemTwenty;

	public GZPTable() {
		super(RoomType.GZP, GameConstants.GAME_PLAYER);

		_logic = new GZPGameLogic();

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

		// 出牌信息
		_out_card_data = 0;

		_status_cs_gang = false;
	}

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		_cell_score = 7;
		_flower_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_flower_count,0);
		_hu_weave_items =  new WeaveItem[this.getTablePlayerNumber()][GameConstants.GZP_MAX_HU_WEAVE];
		_max_hu_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_max_hu_count, 0);
		_max_gzp_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_max_gzp_count, 0);
		_hu_weave_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_weave_count, 0);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.GZP_MAX_HU_WEAVE; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}
		_pu_card = new int[2];
		_pu_count = 0;
		_first_card = 0;
		_pick_up_card = 0;
		_sheng_guan_index = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_pick_up_index = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_cannot_pickup_index = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_guo_hu =  new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_guo_peng =  new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_guo_zhao =  new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_temp_guan = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_game_mid_score = new int[this.getTablePlayerNumber()];
		_game_weave_score = new int[this.getTablePlayerNumber()];
		_guo_qi_guan = new boolean[this.getTablePlayerNumber()];
		Arrays.fill(_guo_qi_guan, false);
		Arrays.fill(_game_mid_score, 0);
		Arrays.fill(_game_weave_score, 0);
		for( int i = 0;  i < this.getTablePlayerNumber();i++)
		{
			_sheng_guan_index[i] = new int [GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_sheng_guan_index[i], 0);
			_pick_up_index[i] = new int [GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_pick_up_index[i], 0);
			_cannot_pickup_index[i] = new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_cannot_pickup_index[i], 0);
			_guo_hu[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_guo_hu[i], 0);
			_guo_peng[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_guo_peng[i], 0);
			_guo_zhao[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_guo_zhao[i], 0);
			_temp_guan[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_temp_guan[i], 0);
		}
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index,
				_game_rule_index, _game_round, this.get_game_des());

		
			// 初始化基础牌局handler
		if(this.is_mj_type(GameConstants.GAME_TYPE_GZP)){
			_handler_dispath_card = new GZPHandlerDispatchCard_TC();
			_handler_out_card_operate = new GZPHandlerOutCardOperate_TC();
			_handler_gang = new GZPHandlerGang_TC();
			_handler_chi_peng = new GZPHandlerChiPeng_TC();
			_handler_pick_up_card = new GZPHandlerPickUpCard_TC();
			_handler_gang_gzp = new GZPHandlerGang_TC_DispatchCard();
			_handler_piao = new GZPHandlerPiao_TC();
			_handler_dispath_firstcards = new GZPHandlerDispatchFirstCard_TC();
		}
		else if(this.is_mj_type(GameConstants.GAME_TYPE_GZP_DDWF)){
			_handler_dispath_card = new GZPHandlerDispatchCard_DDWF();
			_handler_out_card_operate = new GZPHandlerOutCardOperate_DDWF();
			_handler_gang = new GZPHandlerGang_DDWF();
			_handler_chi_peng = new GZPHandlerChiPeng_DDWF();
			_handler_pick_up_card = new GZPHandlerPickUpCard_DDWF();
			_handler_piao = new GZPHandlerPiao_DDWF();
			_handler_dispath_firstcards = new GZPHandlerDispatchFirstCard_DDWF();
		}

		this.setMinPlayerCount(this.getTablePlayerNumber());
		_handler_finish = new GZPHandlerFinish();
		_guan_sheng_count = 0;
		_hua_count = 0;

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
		_flower_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_flower_count,0);
		_max_gzp_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_max_gzp_count, 0);
		_hu_weave_items =  new WeaveItem[this.getTablePlayerNumber()][GameConstants.GZP_MAX_HU_WEAVE];
		_hu_weave_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_weave_count, 0);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.GZP_MAX_HU_WEAVE; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}
		_pu_card = new int[2];
		_pu_count = 0;
		_first_card = 0;
		_pick_up_card = 0;
		_guo_qi_guan = new boolean[this.getTablePlayerNumber()];
		Arrays.fill(_guo_qi_guan, false);
		_pick_up_index = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_sheng_guan_index = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_guo_hu =  new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_guo_peng =  new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_guo_zhao =  new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		_temp_guan = new int[this.getTablePlayerNumber()][GameConstants.GZP_MAX_INDEX];
		for( int i = 0;  i < this.getTablePlayerNumber();i++)
		{
			_sheng_guan_index[i] = new int [GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_sheng_guan_index[i], 0);
			_pick_up_index[i] = new int[GameConstants.GZP_MAX_INDEX];
			_guo_hu[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_guo_hu[i], 0);
			_guo_peng[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_guo_peng[i], 0);
			_guo_zhao[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_guo_zhao[i], 0);
			_temp_guan[i] =  new int[GameConstants.GZP_MAX_INDEX];
			Arrays.fill(_temp_guan[i], 0);
		}
		_game_mid_score = new int[this.getTablePlayerNumber()];
		_game_weave_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_game_mid_score, 0);
		Arrays.fill(_game_weave_score, 0);
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
		GRR = new GameRoundRecord(this.getTablePlayerNumber(),GameConstants.GZP_MAX_WEAVE, GameConstants.GZP_MAX_COUNT,
				getMaxIndex());


		
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.GZP_MAX_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}
		_guan_sheng_count = 0;
		_hua_count = 0;
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
	protected boolean on_handler_game_start(){
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _banker_select;
		_current_player = GRR._banker_player;

		//
		
		_repertory_card = new int[GameConstants.GZP_CARD_COUNT];
		shuffle(_repertory_card, GameConstants.GZP_CARD_DATA);
		
		

//		DEBUG_CARDS_MODE = true;
//		BACK_DEBUG_CARDS_MODE = true;
//		this.enableRobot();
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
//		this.enableRobot();
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_DIA_PAO)==1)
			game_start_paio_fen();
		else 
			game_start_gzp();
		
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
		boolean isStrustee = istrustee[seat_index];

		if (is_sys()) {// 金币场托管 不用需要听牌
			return isStrustee;
		}

		boolean isTing = _playerStatus[seat_index]._hu_card_count > 0;
		if (isStrustee && !isTing&&this.is_robot(seat_index) == false) {
			handler_request_trustee(seat_index, false,0);
		}
		return isStrustee && isTing;
	}
	public void game_start_paio_fen(){
		if (this.getRuleValue(GameConstants.GAME_RULE_TC_DIA_PAO)==1) {
			this._handler = this._handler_piao;
			this._handler_piao.exe(this);
			return ;
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_player_result.pao[i] < 0) {
					_player_result.pao[i] = 0;
				}
				istrustee[i] = false;
			}
		}
	}
	public void game_start_gzp() {
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_THREE_PEOPLE) == 1)
		{
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_SEVEN_COUNT)==1)
				this._cell_score = 7;
			else
				this._cell_score = 9;
		}
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_FOUR_PEOPLE) == 1)
			this._cell_score = 7;
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._banker_select;
		
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.GZP_MAX_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.GZP_MAX_COUNT; j++) {
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
			// 检查听牌
			_playerStatus[i]._hu_card_count = get_gzp_ting_card_twenty(
					_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i],i,i);

			int ting_cards[] = _playerStatus[i]._hu_cards;
			int ting_count = _playerStatus[i]._hu_card_count;

			if (ting_count > 0) {
				operate_chi_hu_cards(i, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				operate_chi_hu_cards(i, 1, ting_cards);
			}
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.GZP_MAX_COUNT; j++) {

				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);
		// 检测听牌
//		for (int i = 0; i < playerCount; i++) {
//			this._playerStatus[i]._hu_card_count = this.get_fls_ting_card_twenty(this._playerStatus[i]._hu_cards,
//					this.GRR._cards_index[i], this.GRR._weave_items[i], this.GRR._weave_count[i], i);
//			if (this._playerStatus[i]._hu_card_count > 0) {
//				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
//			}
//		}
		this._handler = this._handler_dispath_card;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, 0);
	}




	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(5,10) ;
		while(xi_pai_count < 10 && xi_pai_count < rand)
		{
			if(xi_pai_count == 0 )
				_logic.random_card_data(repertory_card, mj_cards);
			else 
				_logic.random_card_data(repertory_card, repertory_card);
			xi_pai_count ++;
		}

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();

		for (int i = 0; i < count; i++) {
			send_count = (20 - 1);
			GRR._left_card_count -= send_count;
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			GRR._card_count[i] = 0;
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
		if (getPlayerCount() == this.getTablePlayerNumber()) {
			return i != _banker_select && Math.abs(_banker_select - i) == 2;
		}
		return false;
	}

	private void test_cards() {
		
	

//		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
//		int[] realyCards = new int[] { 
//				0x01, 0x01, 0x01, 0x23, 0x03, 0x03, 0x03, 0x25, 0x02, 0x02, 0x02, 0x02, 0x06, 0x06, 0x06, 0x06, 0x05, 0x07,  
//				0x21, 0x09, 0x25, 0x27, 0x29, 0x27, 0x29, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x1c, 0x12, 0x19, 0x1a,   
//			    0x02, 0x04, 0x0a, 0x02, 0x04, 0x06, 0x08, 0x0a, 0x02, 0x04, 0x06, 0x08, 0x0a, 0x11, 0x12, 0x13, 0x1b, 0x1c,
//				
//			    0x21,	0x11,  0x02, 0x04, 0x06, 0x08, 0x0a, 0x02, 0x1a, 0x1b, 0x1c, 0x1b, 0x1c, 0x1a,  
//				0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
//		};
//		this._banker_select = 1;
//		testRealyCard(realyCards);
//		DEBUG_CARDS_MODE = true;
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCard
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > GameConstants.GZP_MAX_COUNT-1) {
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (20 - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
	
//        this._banker_select = 2;
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < getMaxIndex(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = (20 - 1);
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
			GRR._card_count[i] = 0;
			for(int j = 0; j<send_count;j++)
				GRR._cards_data[i][GRR._card_count[i] ++ ] = cards[j];
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
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (is_sys()) {
			_game_status = GameConstants.GS_MJ_FREE;
		} else {
			_game_status = GameConstants.GS_MJ_WAIT;
		}
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		boolean ret = false;
	
		ret = this.handler_game_finish_gzp(seat_index, reason);
		
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

	public boolean handler_game_finish_gzp(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}
		if(seat_index != -1)
			game_end.setHuXi(this._max_gzp_count[seat_index]);
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
			int cards[] = new int[GRR._left_card_count];
			int index = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[index] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[index]);

				index++;
				left_card_count--;
			}
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
				for (int j = 0; j < GameConstants.GZP_MAX_COUNT; j++) {
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

				

//				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if(GRR._chi_hu_card[i][0]  == 0  ||reason !=  GameConstants.Game_End_NORMAL )
				{
					Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_count[i]; j++) {

						cs.addItem(GRR._cards_data[i][j]);
					}
					// 福禄寿 小结算 手牌展示中添加胡的牌
					if (GRR._chi_hu_card[i][0] != GameConstants.INVALID_VALUE) {
						cs.addItem(GRR._chi_hu_card[i][0]);
					}
					game_end.addCardsData(cs);// 牌
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						 WeaveItemResponse.Builder weaveItem_item =
						 WeaveItemResponse.newBuilder();
						 weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						 weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						 weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						 for(int k = 0; k< GRR._weave_items[i][j].weave_card.length;k++)
						 {
							weaveItem_item.addWeaveCard(GRR._weave_items[i][j].weave_card[k]);
						 }
						 weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						 if (j < GRR._weave_count[i]) {
						     weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						 } else {
							 weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
						 }
					
						 weaveItem_array.addWeaveItem(weaveItem_item);
					 }
				}
				else
				{

					if (_hu_weave_count[i] > 0) {
						for (int j = 0; j < _hu_weave_count[i]; j++) {
							WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
							weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
							weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
							weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
							weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
							 for(int k = 0; k< _hu_weave_items[i][j].weave_card.length;k++)
							 {
								weaveItem_item.addWeaveCard(_hu_weave_items[i][j].weave_card[k]);
							 }
							weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
							weaveItem_array.addWeaveItem(weaveItem_item);
						}
						Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
					
						game_end.addCardsData(cs);// 牌
					}
					else{
						Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
						for (int j = 0; j < GRR._card_count[i]; j++) {

							cs.addItem(GRR._cards_data[i][j]);
						}
						// 福禄寿 小结算 手牌展示中添加胡的牌
						if (GRR._chi_hu_card[i][0] != GameConstants.INVALID_VALUE) {
							cs.addItem(GRR._chi_hu_card[i][0]);
						}
						game_end.addCardsData(cs);// 牌
					}
					
					
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

		record_game_round(game_end, real_reason);

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
		RoomInfo.Builder room_info = encodeRoomBase();
		room_info.setBankerPlayer(_banker_select);
		int beginLeftCard =  0;
		

		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[],int weave_count,int guan_card[],int pickup_card[], int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index,int provider) {

		if(is_mj_type(GameConstants.GAME_TYPE_GZP))
			return analyse_chi_hu_card_gzp(cards_index, weaveItems, weave_count,guan_card,pickup_card, cur_card, chiHuRight, card_type,seat_index,provider);
		else if(is_mj_type(GameConstants.GAME_TYPE_GZP_DDWF))
			return analyse_chi_hu_card_gzp_ddwf(cards_index, weaveItems, weave_count,guan_card,pickup_card, cur_card, chiHuRight, card_type,seat_index,provider);
		
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
	public int analyse_chi_hu_card_gzp_ddwf(int cards_index[], WeaveItem weaveItems[], int weaveCount, int guan_card[],int pickup_card[],int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index,int provider) {
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
		// 分析扑克
////		if(this.getRuleValue(GameConstants.GAME_RULE_TC_SHI_DUI)==1)
//		{
//			this._hu_weave_count[seat_index] = 0;
//			this._hu_weave_items[seat_index] = new WeaveItem[GameConstants.GZP_MAX_HU_WEAVE]; 
//			for(int i = 0; i<GameConstants.GZP_MAX_HU_WEAVE;i++)
//			{
//				this._hu_weave_items[seat_index][i] = new WeaveItem();
//			}
//			int count = _logic.analyse_card_ten_dui(cbCardIndexTemp,this._hu_weave_items[seat_index],guan_card,pickup_card);
//			if(count == 10)
//			{
//				this._hu_weave_count[seat_index] = 10;
//				this._max_gzp_count[seat_index] = 0;
//				int hong_pai_count = 0;
//				int hei_pai_count = 0;
//				int all_cards_count = 0;
//				for(int i = 0 ; i< this._hu_weave_count[seat_index];i++)
//				{
//					
//					this._hu_weave_items[seat_index][i].hu_xi = _logic.get_weave_items_gzp(this._hu_weave_items[seat_index], i+1);
//					this._max_gzp_count[seat_index]+= this._hu_weave_items[seat_index][i].hu_xi;
//					for(int k = 0; k<this._hu_weave_items[seat_index][i].weave_card.length;k++)
//					{
//						if(this._hu_weave_items[seat_index][i].weave_card[k] != 0)
//						{
//							if(_logic.is_hong(this._hu_weave_items[seat_index][i].weave_card[k] ))
//								hong_pai_count++;
//							else
//								hei_pai_count ++;
//							all_cards_count ++;
//						}
//					
//					}
//				}
//				cbChiHuKind = GameConstants.GZP_WIK_CHI_HU;
//				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider)) {
//					chiHuRight.opr_or(GameConstants.CHR_GZP_ZI_MO);
//				} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider)) {
//					chiHuRight.opr_or(GameConstants.CHR_GZP_DIAN_PAO);
//				}
//				if (hei_pai_count == all_cards_count) 
//					chiHuRight.opr_or(GameConstants.CHR_GZP_SHI_DUI_ALL_BANKER);
//				else
//					chiHuRight.opr_or(GameConstants.CHR_GZP_TEN_TEAM);
//				if(GRR._left_card_count <= 2)
//				{
//					chiHuRight.opr_or(GameConstants.CHR_GZP_HAI_DI);
//				}
//				if (hong_pai_count == all_cards_count) {
//					chiHuRight.opr_or(GameConstants.CHR_GZP_ALL_RED);
//				}
//			
//				return cbChiHuKind;
//			}
//		}
		boolean bValue = _logic.analyse_card_ddwf(cbCardIndexTemp, weaveItems, weaveCount, guan_card,pickup_card, analyseItemArray, false,
				cur_card,seat_index,provider);
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
		WeaveItem weave_items[];
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			weave_items = new WeaveItem[7];
			hong_pai_count = 0;
			hei_pai_count = 0;
			all_cards_count = 0;
			for (int j = 0; j < 6; j++) {
				weave_items[j] = new WeaveItem();
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
	
				weave_items[j].center_card = analyseItem.cbCenterCard[j];
				weave_items[j].weave_kind = analyseItem.cbWeaveKind[j];
				for(int k = 0; k<analyseItem.cbCardData[j].length;k++)
				{
					weave_items[j].weave_card[k] =analyseItem.cbCardData[j][k]; 
					if(analyseItem.cbCardData[j][k] != 0)
					{
						if(_logic.is_hong(analyseItem.cbCardData[j][k]))
							hong_pai_count++;
						else
							hei_pai_count ++;
						all_cards_count ++;
					}
				
				}
				if(j < weaveCount)
					weave_items[j].hu_xi = analyseItem.cbGzshu[j];
				else
					weave_items[j].hu_xi = _logic.get_weave_items_gzp(weave_items, j+1);
				temp_hu_xi += weave_items[j].hu_xi;
				
			}
			if(analyseItem.curCardEye == true)
			{
				weave_items[6] = new WeaveItem();
				weave_items[6].center_card = analyseItem.cbCardEye[0];
				weave_items[6].weave_kind = GameConstants.GZP_WIK_BAN_JIU;
				for(int k = 0; k<2;k++)
				{
					weave_items[6].weave_card[k] = analyseItem.cbCardEye[k];
					
				}
				if(_logic.is_hong(analyseItem.cbCardEye[0]))
					hong_pai_count++;
				else
					hei_pai_count ++;
				all_cards_count ++;
				if(_logic.is_hong(analyseItem.cbCardEye[1]))
					hong_pai_count++;
				else
					hei_pai_count ++;
				all_cards_count ++;
				weave_items[6].hu_xi = _logic.get_weave_items_gzp(weave_items, 7);
				temp_hu_xi += weave_items[6].hu_xi;
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
				
			}
		}
		if (max_hu_xi < this._cell_score) {
			if(hei_pai_count != all_cards_count)
			{
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
			
		}
		hong_pai_count = 0;
		hei_pai_count = 0;
		all_cards_count = 0;
		_max_gzp_count[seat_index] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		for (int j = 0; j < 6; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j] = new WeaveItem();
			this._hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			this._hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			for(int k = 0; k<analyseItem.cbCardData[j].length;k++)
			{
				this._hu_weave_items[seat_index][j].weave_card[k] =analyseItem.cbCardData[j][k]; 
				if(analyseItem.cbCardData[j][k] != 0)
				{
					if(_logic.is_hong(this._hu_weave_items[seat_index][j].weave_card[k]))
						hong_pai_count++;
					else
						hei_pai_count ++;
					all_cards_count ++;
				}
				
			}
			if(j < weaveCount)
				this._hu_weave_items[seat_index][j].hu_xi = analyseItem.cbGzshu[j];
			else
				this._hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_items_gzp(this._hu_weave_items[seat_index], j + 1);
		}
		if(analyseItem.curCardEye == true)
		{
			this._hu_weave_items[seat_index][6].center_card = analyseItem.cbCardEye[0];
			this._hu_weave_items[seat_index][6].weave_kind = GameConstants.GZP_WIK_BAN_JIU;
			this._hu_weave_items[seat_index][6].weave_card[0] = analyseItem.cbCardEye[0];
			this._hu_weave_items[seat_index][6].weave_card[1] = analyseItem.cbCardEye[1];
			this._hu_weave_items[seat_index][6].hu_xi = _logic.get_weave_items_gzp(this._hu_weave_items[seat_index], 7);
			if(_logic.is_hong(analyseItem.cbCardEye[0]))
				hong_pai_count++;
			else
				hei_pai_count ++;
			all_cards_count ++;
			if(_logic.is_hong(analyseItem.cbCardEye[1]))
				hong_pai_count++;
			else
				hei_pai_count ++;
			all_cards_count ++;
		}
		this._hu_weave_count[seat_index] = 7;
		cbChiHuKind = GameConstants.GZP_WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider)) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider)) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_DIAN_PAO);
		}
		if (hong_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_ALL_RED);
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_ALL_BLACK);
		}
		if(GRR._left_card_count <= 2)
		{
			chiHuRight.opr_or(GameConstants.CHR_GZP_HAI_DI);
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
	public int analyse_chi_hu_card_gzp(int cards_index[], WeaveItem weaveItems[], int weaveCount, int guan_card[],int pickup_card[],int cur_card,
			ChiHuRight chiHuRight, int card_type, int seat_index,int provider) {
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
		// 分析扑克
//		if(this.getRuleValue(GameConstants.GAME_RULE_TC_SHI_DUI)==1)
		{
			this._hu_weave_count[seat_index] = 0;
			this._hu_weave_items[seat_index] = new WeaveItem[GameConstants.GZP_MAX_HU_WEAVE]; 
			for(int i = 0; i<GameConstants.GZP_MAX_HU_WEAVE;i++)
			{
				this._hu_weave_items[seat_index][i] = new WeaveItem();
			}
			int count = _logic.analyse_card_ten_dui(cbCardIndexTemp,this._hu_weave_items[seat_index],guan_card,pickup_card);
			if(count == 10)
			{
				this._hu_weave_count[seat_index] = 10;
				this._max_gzp_count[seat_index] = 0;
				int hong_pai_count = 0;
				int hei_pai_count = 0;
				int all_cards_count = 0;
				for(int i = 0 ; i< this._hu_weave_count[seat_index];i++)
				{
					
					this._hu_weave_items[seat_index][i].hu_xi = _logic.get_weave_items_gzp(this._hu_weave_items[seat_index], i+1);
					this._max_gzp_count[seat_index]+= this._hu_weave_items[seat_index][i].hu_xi;
					for(int k = 0; k<this._hu_weave_items[seat_index][i].weave_card.length;k++)
					{
						if(this._hu_weave_items[seat_index][i].weave_card[k] != 0)
						{
							if(_logic.is_hong(this._hu_weave_items[seat_index][i].weave_card[k] ))
								hong_pai_count++;
							else
								hei_pai_count ++;
							all_cards_count ++;
						}
					
					}
				}
				cbChiHuKind = GameConstants.GZP_WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider)) {
					chiHuRight.opr_or(GameConstants.CHR_GZP_ZI_MO);
				} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider)) {
					chiHuRight.opr_or(GameConstants.CHR_GZP_DIAN_PAO);
				}
				if (hei_pai_count == all_cards_count) 
					chiHuRight.opr_or(GameConstants.CHR_GZP_SHI_DUI_ALL_BANKER);
				else
					chiHuRight.opr_or(GameConstants.CHR_GZP_TEN_TEAM);
				if(GRR._left_card_count <= 2)
				{
					chiHuRight.opr_or(GameConstants.CHR_GZP_HAI_DI);
				}
				if (hong_pai_count == all_cards_count) {
					chiHuRight.opr_or(GameConstants.CHR_GZP_ALL_RED);
				}
			
				return cbChiHuKind;
			}
		}
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, guan_card,pickup_card, analyseItemArray, false,
				cur_card,seat_index,provider);
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
		WeaveItem weave_items[];
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);
			weave_items = new WeaveItem[7];
			hong_pai_count = 0;
			hei_pai_count = 0;
			all_cards_count = 0;
			for (int j = 0; j < 6; j++) {
				weave_items[j] = new WeaveItem();
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
	
				weave_items[j].center_card = analyseItem.cbCenterCard[j];
				weave_items[j].weave_kind = analyseItem.cbWeaveKind[j];
				for(int k = 0; k<analyseItem.cbCardData[j].length;k++)
				{
					weave_items[j].weave_card[k] =analyseItem.cbCardData[j][k]; 
					if(analyseItem.cbCardData[j][k] != 0)
					{
						if(_logic.is_hong(analyseItem.cbCardData[j][k]))
							hong_pai_count++;
						else
							hei_pai_count ++;
						all_cards_count ++;
					}
				
				}
				if(j < weaveCount)
					weave_items[j].hu_xi = analyseItem.cbGzshu[j];
				else
					weave_items[j].hu_xi = _logic.get_weave_items_gzp(weave_items, j+1);
				temp_hu_xi += weave_items[j].hu_xi;
				
			}
			if(analyseItem.curCardEye == true)
			{
				weave_items[6] = new WeaveItem();
				weave_items[6].center_card = analyseItem.cbCardEye[0];
				weave_items[6].weave_kind = GameConstants.GZP_WIK_BAN_JIU;
				for(int k = 0; k<2;k++)
				{
					weave_items[6].weave_card[k] = analyseItem.cbCardEye[k];
					
				}
				if(_logic.is_hong(analyseItem.cbCardEye[0]))
					hong_pai_count++;
				else
					hei_pai_count ++;
				all_cards_count ++;
				if(_logic.is_hong(analyseItem.cbCardEye[1]))
					hong_pai_count++;
				else
					hei_pai_count ++;
				all_cards_count ++;
				weave_items[6].hu_xi = _logic.get_weave_items_gzp(weave_items, 7);
				temp_hu_xi += weave_items[6].hu_xi;
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
				
			}
		}
		if (max_hu_xi < this._cell_score) {
			if(hei_pai_count != all_cards_count)
			{
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
			
		}
		hong_pai_count = 0;
		hei_pai_count = 0;
		all_cards_count = 0;
		_max_gzp_count[seat_index] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		for (int j = 0; j < 6; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j] = new WeaveItem();
			this._hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			this._hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			for(int k = 0; k<analyseItem.cbCardData[j].length;k++)
			{
				this._hu_weave_items[seat_index][j].weave_card[k] =analyseItem.cbCardData[j][k]; 
				if(analyseItem.cbCardData[j][k] != 0)
				{
					if(_logic.is_hong(this._hu_weave_items[seat_index][j].weave_card[k]))
						hong_pai_count++;
					else
						hei_pai_count ++;
					all_cards_count ++;
				}
				
			}
			if(j < weaveCount)
				this._hu_weave_items[seat_index][j].hu_xi = analyseItem.cbGzshu[j];
			else
				this._hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_items_gzp(this._hu_weave_items[seat_index], j + 1);
		}
		if(analyseItem.curCardEye == true)
		{
			this._hu_weave_items[seat_index][6].center_card = analyseItem.cbCardEye[0];
			this._hu_weave_items[seat_index][6].weave_kind = GameConstants.GZP_WIK_BAN_JIU;
			this._hu_weave_items[seat_index][6].weave_card[0] = analyseItem.cbCardEye[0];
			this._hu_weave_items[seat_index][6].weave_card[1] = analyseItem.cbCardEye[1];
			this._hu_weave_items[seat_index][6].hu_xi = _logic.get_weave_items_gzp(this._hu_weave_items[seat_index], 7);
			if(_logic.is_hong(analyseItem.cbCardEye[0]))
				hong_pai_count++;
			else
				hei_pai_count ++;
			all_cards_count ++;
			if(_logic.is_hong(analyseItem.cbCardEye[1]))
				hong_pai_count++;
			else
				hei_pai_count ++;
			all_cards_count ++;
		}
		this._hu_weave_count[seat_index] = 7;
		cbChiHuKind = GameConstants.GZP_WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider)) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider)) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_DIAN_PAO);
		}
		if (hong_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_ALL_RED);
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_GZP_ALL_BLACK);
		}
		if(GRR._left_card_count <= 2)
		{
			chiHuRight.opr_or(GameConstants.CHR_GZP_HAI_DI);
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
	public boolean is_zi_mo(int seat_index,int send_card){
		for(int i = 0; i<_playerStatus[seat_index]._hu_card_count;i++)
		{
			if(send_card == _playerStatus[seat_index]._hu_cards[i])
				return true;
		}
		return false;
	}
	public int  control_zi_mo(int seat_index,int send_card){
		int card = send_card;
			
		if(_playerStatus[seat_index]._hu_card_count>0 && this._player_result.getGame_score()[seat_index]>0&&is_zi_mo(seat_index,send_card)){
			SysParamModel sysParamModel20 = SysParamServerDict.getInstance()
					.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(20);
			if(sysParamModel20 == null || sysParamModel20.getVal5() == 0)
				return send_card;
			int count = 0;
			int is_rand_rate = (int) RandomUtil.generateRandomNumber(0,1000) ;
			int sys_val1 = 500;
			if(sysParamModel20.getVal1()>=500&&sysParamModel20.getVal1()<1000)
				sys_val1 = sysParamModel20.getVal1();
			if(is_rand_rate < sys_val1)
			{
				int sys_val2 = 5;
				if(sysParamModel20.getVal2()>1&&sysParamModel20.getVal2()<10)
					sys_val2 = sysParamModel20.getVal2();
				while(count++ < sys_val2&&is_zi_mo(seat_index,send_card)==false)
				{
					int is_rand_control =(int) RandomUtil.generateRandomNumber(_all_card_len - GRR._left_card_count,_all_card_len) ;
					card =  _repertory_card[is_rand_control];
					_repertory_card[is_rand_control] = send_card;
					send_card = card;
				}
			}
			
				
		}
		return card;
	}


	//玩家是否可以生观
	public void estimate_player_sheng_guan(int seat_index,int card)
	{
		int card_temp[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		_logic.get_logic_count(card_temp,GRR._cards_index[seat_index]);
		int guan_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		_logic.get_logic_count(guan_index,this._sheng_guan_index[seat_index]);
		int pickup_card[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
		_logic.get_logic_count(pickup_card,this._pick_up_index[seat_index]);
		for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
		{
			card_temp[i] = card_temp[i]-pickup_card[i];
		}
		if(card != 0)
		{
			int card_index = 0;
			if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
			{
				 card_index = _logic.get_card_value(card)-1;
			}
			else 
				card_index = _logic.switch_to_card_index(card);
			card_temp[card_index]++;
		}
		for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
		{
			if(card_temp[i] >= 4&&this._sheng_guan_index[seat_index][i] == 0)
			{
				_playerStatus[seat_index].add_action(GameConstants.GZP_WIK_GUAN);
				_playerStatus[seat_index].add_guan(_logic.switch_to_card_data(i), GameConstants.GZP_WIK_GUAN,seat_index);
				
			}
		}
	}
	//玩家是否可以滑
	public void estimate_player_hua_ddwf(int seat_index,int card,boolean is_out)
	{	
	
		for(int j = 0; j<this.getTablePlayerNumber();j++)
		{
			if(is_out == true && seat_index == j)
				continue;
			if(is_out== false && seat_index != j)
				continue;
			int card_temp[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(card_temp,GRR._cards_index[j]);
			int pickup_card[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(pickup_card,this._pick_up_index[j]);
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				card_temp[i] = card_temp[i]-pickup_card[i];
			}
			if(card != 0)
			{
				int card_index = 0;
				if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
				{
					 card_index = _logic.get_card_value(card)-1;
				}
				else 
					card_index = _logic.switch_to_card_index(card);
				if(is_out == false && seat_index == j)
					card_temp[card_index]++;
				else if(is_out== true && seat_index != j)
					card_temp[card_index]++;
				if(((this._temp_guan[seat_index][card_index] != 0)&&(seat_index==j))||card_temp[card_index]==5)
				{
					if(this._sheng_guan_index[seat_index][card_index] == 0 && GRR._left_card_count== 6 )
						continue;
					_playerStatus[j].add_action(GameConstants.GZP_WIK_HUA);
					_playerStatus[j].add_hua(card, GameConstants.GZP_WIK_HUA, seat_index);
					
				}
			}
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				int card_index = 0;
				if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
				{
					 card_index = _logic.get_card_value(card)-1;
				}
				else 
					card_index = _logic.switch_to_card_index(card);
				if((is_out ==  true)&&(card_index !=i))
					continue;
				if(i == card_index)
					continue;
				boolean flag = false;
				if((card_temp[i] == 1)&&this._temp_guan[seat_index][i] != 0&&flag==false){
					_playerStatus[j].add_action(GameConstants.GZP_WIK_HUA);
					_playerStatus[j].add_hua(_logic.switch_to_card_data(i), GameConstants.GZP_WIK_HUA, seat_index);
					flag = true;
				}
				if((card_temp[i] == 5))
				{
					if(this._sheng_guan_index[seat_index][i] == 0 && GRR._left_card_count== 6 )
						continue;
				
					_playerStatus[j].add_action(GameConstants.GZP_WIK_HUA);
					_playerStatus[j].add_hua(_logic.switch_to_card_data(i), GameConstants.GZP_WIK_HUA, seat_index);
					
					
					
				}
			}
		}
	
	}
	//玩家是否可以滑
	public void estimate_player_hua(int seat_index,int card,boolean is_out)
	{	
	
		for(int j = 0; j<this.getTablePlayerNumber();j++)
		{
			if(is_out == true && seat_index == j)
				continue;
			if(is_out== false && seat_index != j)
				continue;
			int card_temp[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(card_temp,GRR._cards_index[j]);
			int pickup_card[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(pickup_card,this._pick_up_index[j]);
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				card_temp[i] = card_temp[i]-pickup_card[i];
			}
			if(card != 0)
			{
				int card_index = 0;
				if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
				{
					 card_index = _logic.get_card_value(card)-1;
				}
				else 
					card_index = _logic.switch_to_card_index(card);
				if(is_out == false && seat_index == j)
					card_temp[card_index]++;
				else if(is_out== true && seat_index != j)
					card_temp[card_index]++;
				if(((this._temp_guan[seat_index][card_index] != 0)&&(seat_index==j))||card_temp[card_index]==5)
				{
					_playerStatus[j].add_action(GameConstants.GZP_WIK_HUA);
					_playerStatus[j].add_hua(card, GameConstants.GZP_WIK_HUA, seat_index);
					
				}
			}
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				int card_index = 0;
				if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
				{
					 card_index = _logic.get_card_value(card)-1;
				}
				else 
					card_index = _logic.switch_to_card_index(card);
				if((is_out ==  true)&&(card_index !=i))
					continue;
				if(i == card_index)
					continue;
				boolean flag = false;
				if((card_temp[i] == 1)&&this._temp_guan[seat_index][i] != 0&&flag==false){
					_playerStatus[j].add_action(GameConstants.GZP_WIK_HUA);
					_playerStatus[j].add_hua(_logic.switch_to_card_data(i), GameConstants.GZP_WIK_HUA, seat_index);
					flag = true;
				}
				if((card_temp[i] == 5))
				{
					
					_playerStatus[j].add_action(GameConstants.GZP_WIK_HUA);
					_playerStatus[j].add_hua(_logic.switch_to_card_data(i), GameConstants.GZP_WIK_HUA, seat_index);
					
				}
			}
		}
	
	}
	//玩家是否可以碰
		public void estimate_player_peng_ddwf(int seat_index,int card)
		{	
			
			for(int j = 0; j<this.getTablePlayerNumber();j++)
			{
				int card_temp[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
				_logic.get_logic_count(card_temp,GRR._cards_index[j]);
				int guan_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
				_logic.get_logic_count(guan_index,this._sheng_guan_index[j]);
				int pickup_card[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
				_logic.get_logic_count(pickup_card,this._pick_up_index[j]);
				if(seat_index == j)
					continue;
				for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
				{
					card_temp[i] = card_temp[i]-pickup_card[i];
				}
				int card_index = 0;
				if(card != 0)
				{
					if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
					{
						 card_index = _logic.get_card_value(card)-1;
					}
					else 
						card_index = _logic.switch_to_card_index(card);
					card_temp[card_index]++;
				}
				if(is_mj_type(GameConstants.GAME_TYPE_GZP_DDWF)&&pickup_card[card_index] != 0)
					continue;
				int card_peng_index = _logic.switch_to_card_index(card);
				if(this._guo_peng[j][card_peng_index] != 0)
					continue;
				if(card_temp[card_index] >= 3&&guan_index[card_index] == 0 )
				{
					_playerStatus[j].add_action(GameConstants.GZP_WIK_PENG);
					_playerStatus[j].add_gzp_peng(card, GameConstants.GZP_WIK_PENG, seat_index);
				}
				
			}
		}
	//玩家是否可以碰
	public void estimate_player_peng(int seat_index,int card)
	{	
		
		for(int j = 0; j<this.getTablePlayerNumber();j++)
		{
			int card_temp[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(card_temp,GRR._cards_index[j]);
			int guan_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(guan_index,this._sheng_guan_index[j]);
			int pickup_card[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(pickup_card,this._pick_up_index[j]);
			if(seat_index == j)
				continue;
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				card_temp[i] = card_temp[i]-pickup_card[i];
			}
			int card_index = 0;
			if(card != 0)
			{
				if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
				{
					 card_index = _logic.get_card_value(card)-1;
				}
				else 
					card_index = _logic.switch_to_card_index(card);
				card_temp[card_index]++;
			}
			if(is_mj_type(GameConstants.GAME_TYPE_GZP_DDWF)&&pickup_card[card_index] != 0)
				continue;
			if(this._guo_peng[j][card_index] != 0)
				continue;
			if(card_temp[card_index] >= 3&&guan_index[card_index] == 0 )
			{
				_playerStatus[j].add_action(GameConstants.GZP_WIK_PENG);
				_playerStatus[j].add_gzp_peng(card, GameConstants.GZP_WIK_PENG, seat_index);
			}
			
		}
	}
	//玩家是否可以碰
	public void estimate_player_zhao(int seat_index,int card)
	{	
		
		for(int j = 0; j<this.getTablePlayerNumber();j++)
		{
			int card_temp[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(card_temp,GRR._cards_index[j]);
			int guan_index[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(guan_index,this._sheng_guan_index[j]);
			int pickup_card[] = new int[GameConstants.GZP_MAX_LOGIC_INDEX];
			_logic.get_logic_count(pickup_card,this._pick_up_index[j]);
			if(seat_index == j)
				continue;
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				card_temp[i] = card_temp[i]-pickup_card[i];
			}
			int card_index = 0;
			if(card != 0)
			{
				
				if(_logic.switch_to_card_index(card)>=GameConstants.GZP_MAX_LOGIC_INDEX)
				{
					 card_index = _logic.get_card_value(card)-1;
				}
				else 
					card_index = _logic.switch_to_card_index(card);
				card_temp[card_index]++;
			}
			for(int i= 0 ;i<GameConstants.GZP_MAX_LOGIC_INDEX;i++)
			{
				if(this._guo_zhao[j][i] != 0)
					continue;
				if(card_temp[i] >= 4&&guan_index[i] == 0&&card_index == i)
				{
					_playerStatus[j].add_action(GameConstants.GZP_WIK_ZHAO);
					_playerStatus[j].add_gzp_zhao(card, GameConstants.GZP_WIK_ZHAO, seat_index);
					
				}
			}
		}
	}
	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_pickpu(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有
		// 用户状态
		PlayerStatus playerStatus = null;
		int action = GameConstants.WIK_NULL;
		int pickup_seat_index = (seat_index + 1) % getTablePlayerNumber();
		// 这里可能有问题 应该是 |=
		_playerStatus[pickup_seat_index].add_action(GameConstants.GZP_WIK_PICKUP);
		_playerStatus[pickup_seat_index].add_pick_up(card, GameConstants.GZP_WIK_PICKUP, seat_index);
		// 结果判断
		if (_playerStatus[pickup_seat_index].has_action()) {
			bAroseAction = true;
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
		handler_request_trustee(_seat_index, true,0);

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
		if (getPlayerCount() == this.getTablePlayerNumber() && _kick_schedule == null) {// 人满
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

		if (getPlayerCount() == this.getTablePlayerNumber() && _kick_schedule != null) {// 人满
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		if(_player_ready[seat_index] == 1)
		{
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

		int number  = this.getTablePlayerNumber();
		for (int i = 0; i < number; i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}
			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
				return false;
			}
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

//		if (is_sys())
//			return true;
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
			int b_out_card,String desc) {
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

		
		// 效果
		if(rm == true)
		{
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_ZI_MO,1, chr.type_list,					1, GameConstants.INVALID_SEAT);
		}
//		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU,1, chr.type_list,
//				1, GameConstants.INVALID_SEAT);
		// 手牌删掉
//		this.operate_player_cards(seat_index, 0, null, 0, null);
//
//		if (rm) {
//			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
//			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
//		}

		// 显示胡牌
		int cards[] = new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
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
		if(this._max_gzp_count[_seat_index] >_player_result.ying_xi_count[_seat_index] )
			_player_result.ying_xi_count[_seat_index] = this._max_gzp_count[_seat_index];
			
		_player_result.hu_pai_count[_seat_index]++;

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
	public int get_chi_hu_action_rank_gzp(ChiHuRight chiHuRight) {
		int wFanShu = 1;
		
		if (!(chiHuRight.opr_and(GameConstants.CHR_GZP_ALL_BLACK)).is_empty()) {
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_ALL_HEI_3) == 1)
				wFanShu = 3;
			else
				wFanShu  = 2;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_GZP_ALL_RED)).is_empty()) {
			wFanShu = 10;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_GZP_TEN_TEAM)).is_empty()) {
			wFanShu = 4;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_GZP_SHI_DUI_ALL_BANKER)).is_empty()) {
			wFanShu = 5;
		}
		

		return wFanShu;
	}
	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_gzp(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		this._flower_count[seat_index] = _logic.calculate_flower_count(this._hu_weave_items[seat_index],this._hu_weave_count[seat_index]);
		int wFanShu = get_chi_hu_action_rank_gzp(chr);// 番数
		countCardTypefls(chr, seat_index);
		int calculate_score = 1;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_FIVE_FEN)==1)
			calculate_score = 5;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_FOUR_FEN)==1)
			calculate_score = 4;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_THREE_FEN)==1)
			calculate_score = 3;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_TWO_FEN)==1)
			calculate_score = 2;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_ONE_FEN)==1)
			calculate_score = 1;
		int times = this._max_gzp_count[seat_index]/this._cell_score;
		if(times<1)
		     times = 1;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_HAI_DI_ADD_DI)==1)
		{
			if (!(chr.opr_and(GameConstants.CHR_GZP_HAI_DI)).is_empty()) {
				times += 1;
			}
		}
	
		wFanShu = wFanShu * times;
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
		
		float lChiHuScore = wFanShu *calculate_score ;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_TEN_HUA)==1)
			lChiHuScore += this._flower_count[seat_index]*calculate_score  ;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_LIU_HUA)==1)
			lChiHuScore += 1*calculate_score ;
		int pao_score = 0;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_PAO) == 1)
		{
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_ONE)==1)
				pao_score = 1;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_TWO)==1)
				pao_score = 2;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_THREE)==1)
				pao_score = 3;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_FOUR)==1)
				pao_score = 4;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_FIVE)==1)
				pao_score = 5;
			for(int i = 0; i<this.getTablePlayerNumber();i++)
			{
				_player_result.pao[i] = pao_score;
			}
		}
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
		
			s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

			// 跑和呛
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		// _playerStatus[seat_index].clean_status();
	}
	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_gzp_ddwf(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		this._flower_count[seat_index] = _logic.calculate_flower_count(this._hu_weave_items[seat_index],this._hu_weave_count[seat_index]);
		int quan_quan_count  = _logic.calculate_quan_quan_count(this._hu_weave_items[seat_index],this._hu_weave_count[seat_index]);
		
		countCardTypefls(chr, seat_index);
		int calculate_score = 1;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_FIVE_FEN)==1)
			calculate_score = 5;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_FOUR_FEN)==1)
			calculate_score = 4;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_THREE_FEN)==1)
			calculate_score = 3;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_TWO_FEN)==1)
			calculate_score = 2;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_ONE_FEN)==1)
			calculate_score = 1;
		int times = this._max_gzp_count[seat_index]/this._cell_score;
		if(times<1)
		     times = 1;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_HAI_DI_ADD_DI)==1)
		{
			if (!(chr.opr_and(GameConstants.CHR_GZP_HAI_DI)).is_empty()) {
				times += 1;
			}
		}
		int  wFanShu = 1;
		if (!(chr.opr_and(GameConstants.CHR_GZP_ALL_BLACK)).is_empty()) {
			wFanShu = this._max_gzp_count[seat_index]/this._cell_score;
			wFanShu  += 2;
		}
		if (!(chr.opr_and(GameConstants.CHR_GZP_ALL_RED)).is_empty()) {
			wFanShu = 10;
		}
		if (!(chr.opr_and(GameConstants.CHR_GZP_TEN_TEAM)).is_empty()) {
			wFanShu = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_GZP_SHI_DUI_ALL_BANKER)).is_empty()) {
			wFanShu = 5;
		}
		wFanShu = wFanShu * times;
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
		
		float lChiHuScore = wFanShu *calculate_score ;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_TEN_HUA)==1)
			lChiHuScore += this._flower_count[seat_index]*calculate_score  ;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_LIU_HUA)==1)
			lChiHuScore += 1*calculate_score ;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_QUANQ_SHU)==1)
			lChiHuScore += calculate_score * quan_quan_count;
		int pao_score = 0;
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_PAO) == 1)
		{
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_ONE)==1)
				pao_score = 1;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_TWO)==1)
				pao_score = 2;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_THREE)==1)
				pao_score = 3;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_FOUR)==1)
				pao_score = 4;
			if(this.getRuleValue(GameConstants.GAME_RULE_TC_DING_FIVE)==1)
				pao_score = 5;
			for(int i = 0; i<this.getTablePlayerNumber();i++)
			{
				_player_result.pao[i] = pao_score;
			}
		}
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
		
			s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

			// 跑和呛
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

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
			playerNumber = this.getTablePlayerNumber();
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
			// for (int i = 0; i < MJthis.getTablePlayerNumber(); i++) {
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
		if(_playerStatus[player.get_seat_index()]._is_pao_qiang || _playerStatus[player.get_seat_index()]._is_pao){
			return false;
		}
		if (_handler_piao != null) {
			return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
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

		int standTime = 200;
		int flashTime = 10;
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
		int di_cards[];
		di_cards = new int[GRR._left_card_count];
		int k = 0;
		int left_card_count = GRR._left_card_count;
		for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
			di_cards[k] = _repertory_card[_all_card_len - left_card_count];
			roomResponse.addCardsList(di_cards[k]);
			k++;
			left_card_count--;
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

	// 不能出的牌
	public boolean operate_cannot_card(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		int cards[] = new int[GameConstants.GZP_MAX_INDEX];
		// 刷新自己手牌
		int count = _logic.switch_to_cards_data(GRR._cannot_out_index[seat_index], cards);
		roomResponse.setType(MsgConstants.RESPONSE_CANNOT_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}
	// 不能出的牌
	public boolean operate_cannot_pickup_card(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		int cards[] = new int[GameConstants.GZP_MAX_INDEX];
		// 刷新自己手牌
		int count = _logic.switch_to_cards_data(this._cannot_pickup_index[seat_index], cards);
		roomResponse.setType(MsgConstants.RESPONSE_CANNOT_OUT_PICKUP_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}
	// 捡的牌
	public boolean operate_pick_up_card(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		int cards[] = new int[GameConstants.GZP_MAX_INDEX];
		// 刷新自己手牌
		int count = _logic.switch_to_cards_data(_pick_up_index[seat_index], cards);
		roomResponse.setType(MsgConstants.RESPONSE_UPDATE_PICKUP_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}
	// 添加捡来的牌
    public  boolean operate_pick_up_single_card(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_UPDATE_PICKUP_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// 捡来来的单张牌
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

//	/**
//	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
//	 * 
//	 * @param seat_index
//	 * @param effect_type
//	 * @param effect_count
//	 * @param effect_indexs
//	 * @param time
//	 * @param to_player
//	 * @return
//	 */
//	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[],
//			int time, int to_player, int center_card, boolean is_public) {
//		
//	}
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
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}
	public boolean operate_player_action(int seat_index, boolean close, boolean add_to_record) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			if (add_to_record&&GRR != null)
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
		if (add_to_record&&GRR != null)
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
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player,boolean is_display ,int is_hand_card) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);
		roomResponse.setKindType(is_hand_card); //1代表加到手牌，0不加到手牌
		int standTime = 400;
		int flashTime = 10;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel108 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1108);
		if (sysParamModel108 != null && sysParamModel108.getVal2() > 0 && sysParamModel108.getVal2() < 1000) {
			flashTime = sysParamModel108.getVal2();
		}
		if (sysParamModel108 != null && sysParamModel108.getVal5() > 0 && sysParamModel108.getVal5() < 1000) {
			standTime = sysParamModel108.getVal5();
		}
		if(is_hand_card  == 1)
		{
			roomResponse.setFlashTime(flashTime);
			roomResponse.setStandTime(40);
			roomResponse.setInsertTime(flashTime);
		}
		else{
			roomResponse.setFlashTime(flashTime);
			roomResponse.setStandTime(standTime);
			roomResponse.setInsertTime(flashTime);
		}

	
		

		if (to_player == GameConstants.INVALID_SEAT) {
			if(is_display == true)
			{
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);// 给别人 牌数据
				}
			}
			else
			{
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(GameConstants.BLACK_CARD);// 给别人 牌背
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
				// GRR.add_room_response(roomResponse);
				return this.send_response_to_player(seat_index, roomResponse);
			}
		}

		return false;
	}
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		operate_player_cards(seat_index,  card_count,  cards,  weave_count,
				 weaveitems, true) ;
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[],boolean is_grr) {
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
	
		
		for (int j = 0; j < weave_count; j++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
			weaveItem_item.setPublicCard(weaveitems[j].public_card);
			weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
			weaveItem_item.setCenterCard(weaveitems[j].center_card);
			weaveItem_item.setHuXi(weaveitems[j].hu_xi);
			for(int k = 0; k< weaveitems[j].weave_card.length;k++)
			{
				weaveItem_item.addWeaveCard(weaveitems[j].weave_card[k]);
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		

		this.send_response_to_other(seat_index, roomResponse);

		

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		if(is_grr == true)
			GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}
	public boolean operate_player_connect_cards(int seat_index, int card_count, int cards[], int weave_count,
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
	
		
		for (int j = 0; j < weave_count; j++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
			weaveItem_item.setPublicCard(weaveitems[j].public_card);
			weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
			weaveItem_item.setCenterCard(weaveitems[j].center_card);
			weaveItem_item.setHuXi(weaveitems[j].hu_xi);
			for(int k = 0; k< weaveitems[j].weave_card.length;k++)
			{
				weaveItem_item.addWeaveCard(weaveitems[j].weave_card[k]);
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
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

			// if(is_mj_type(MJGameConstants.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);
			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
	
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
//			change_handler(_handler_dispath_last_card);
//			this._handler_dispath_last_card.reset_status(seat_index, type);
//			this._handler.exe(this);
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
//			change_handler(_handler_dispath_last_card_twenty);
//			this._handler_dispath_last_card_twenty.reset_status(seat_index, type);
//			this._handler.exe(this);
		}

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

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_banker_select = rand % this.getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_gzp() {
		// 杠牌，每个人的分数
		int count = 0;
		for(int i = 0;i<this.getTablePlayerNumber();i++)
		{
			if(GRR._chi_hu_card[i][0]  != 0)
				count ++;
		}
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			int calculate_score = 1;
			if(l > 0&&GRR._chi_hu_card[i][0]!=0)
			{
				
				if(this.getRuleValue(GameConstants.GAME_RULE_TC_FIVE_FEN)==1)
					calculate_score = 5;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_FOUR_FEN)==1)
					calculate_score = 4;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_THREE_FEN)==1)
					calculate_score = 3;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_TWO_FEN)==1)
					calculate_score = 2;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_ONE_FEN)==1)
					calculate_score = 1;
				
				des += ","+calculate_score+"底分";
				
			}
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if(type == GameConstants.CHR_GZP_ZI_MO)
					{
						des += ",自摸";
					}
					if(type == GameConstants.CHR_GZP_DIAN_PAO)
					{
						des += ",点炮";
					}
					if(type == GameConstants.CHR_GZP_ALL_RED)
					{
						des += ",全红10倍 ";
					}
					if(type == GameConstants.CHR_GZP_ALL_BLACK)
					{
						if(this.getRuleValue(GameConstants.GAME_RULE_TC_ALL_HEI_3) == 1)
							des += ",全黑3倍";
						else
							des += ",全黑2倍";
					}
					if(type == GameConstants.CHR_GZP_TEN_TEAM)
					{
						des += ",十对4倍";
					}
					if(type == GameConstants.CHR_GZP_SHI_DUI_ALL_BANKER)
					{
						des += ",十对全黑5倍";
					}
					if(type == GameConstants.CHR_GZP_HAI_DI)
					{
						if(this.getRuleValue(GameConstants.GAME_RULE_TC_HAI_DI_ADD_DI)==1)
							des += ",海底捞"+calculate_score+"分";
						else
							des += ",海底捞";
					}
					
				}
			}
			if(l == 0 && count == 2)
				des += ",放炮";
			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_gzp_ddwf() {
		// 杠牌，每个人的分数
		int count = 0;
		for(int i = 0;i<this.getTablePlayerNumber();i++)
		{
			if(GRR._chi_hu_card[i][0]  != 0)
				count ++;
		}
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			int calculate_score = 1;
			if(l > 0&&GRR._chi_hu_card[i][0]!=0)
			{
				
				if(this.getRuleValue(GameConstants.GAME_RULE_TC_FIVE_FEN)==1)
					calculate_score = 5;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_FOUR_FEN)==1)
					calculate_score = 4;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_THREE_FEN)==1)
					calculate_score = 3;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_TWO_FEN)==1)
					calculate_score = 2;
				else if(this.getRuleValue(GameConstants.GAME_RULE_TC_ONE_FEN)==1)
					calculate_score = 1;
				
				des += ","+calculate_score+"底分";
				
			}
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if(type == GameConstants.CHR_GZP_ZI_MO)
					{
						des += ",自摸";
					}
					if(type == GameConstants.CHR_GZP_DIAN_PAO)
					{
						des += ",点炮";
					}
					if(type == GameConstants.CHR_GZP_ALL_RED)
					{
						des += ",全红10倍 ";
					}
					if(type == GameConstants.CHR_GZP_ALL_BLACK)
					{
							des += ",全黑2倍";
					}
					if(type == GameConstants.CHR_GZP_TEN_TEAM)
					{
						des += ",十对4倍";
					}
					if(type == GameConstants.CHR_GZP_SHI_DUI_ALL_BANKER)
					{
						des += ",十对全黑5倍";
					}
					if(type == GameConstants.CHR_GZP_HAI_DI)
					{
						if(this.getRuleValue(GameConstants.GAME_RULE_TC_HAI_DI_ADD_DI)==1)
							des += ",海底捞"+calculate_score+"分";
						else
							des += ",海底捞";
					}
					
				}
			}
			if(l == 0 && count == 2)
				des += ",放炮";
			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	private void set_result_describe() {
		 if (this.is_mj_type(GameConstants.GAME_TYPE_GZP)) {
			 set_result_describe_gzp();
			
		}
		 if (this.is_mj_type(GameConstants.GAME_TYPE_GZP_DDWF)) {
			 set_result_describe_gzp_ddwf();
		 }
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
//		change_handler(_handler_finish);
//		this._handler_finish.exe(this);
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
	 * //执行发牌 首牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_dispatch_first_card(int seat_index, int type, int delay_time) {
		if (delay_time > 0) {
			GameSchedule.put(new DispatchFirstCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_dispath_firstcards;
			this._handler_dispath_firstcards.reset_status(seat_index, type);
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

		this._handler = this._handler_gang;
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
//		change_handler(_handler_gang_fls);
//		this._handler_gang_fls.reset_status(seat_index, d);
//		this._handler_gang_fls.exe(this);
		return true;
	}

	/**
	 * 海底
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_hai_di(int start_index, int seat_index) {
//		change_handler(_handler_hai_di);
//		this._handler_hai_di.reset_status(start_index, seat_index);
//		this._handler_hai_di.exe(this);
		return true;
	}

	/**
	 * 要海底
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_yao_hai_di(int seat_index) {
//		change_handler(_handler_yao_hai_di);
//		this._handler_yao_hai_di.reset_status(seat_index);
//		this._handler_yao_hai_di.exe(this);
		return true;
	}
	/**
	 * 捡牌操作
	 */
	public boolean exe_jian_card(int seat_index,int provide_seat,int action, int out_card_data)
	{
		this._handler = this._handler_pick_up_card;
		this._handler_pick_up_card.reset_status(seat_index,provide_seat,  action,out_card_data);
		this._handler.exe(this);
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
		this._handler = _handler_out_card_operate;
		this._handler_out_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		// 出牌
		this._handler = _handler_chi_peng;
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
//		change_handler(_handler_xipai_fls);
//		this._handler_xipai_fls.exe(this);
		return true;
	}

	public boolean exe_xi_pai_twenty() {
//		this._handler = this._handler_xipai_fls_twenty;
//		this._handler_xipai_fls_twenty.exe(this);
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
	 * 调度,不能出的牌
	 **/
	public void cannot_outcard(int seat_index, int card_count, int card_data, boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		int flower_index = _logic.switch_to_card_flower_index(card_data);
		int common_index = _logic.switch_to_card_common_index(card_data);
		
		GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] += card_count;
		if(flower_index != -1)
			GRR._cannot_out_index[seat_index][flower_index] += card_count;
	    if(common_index != -1)
			GRR._cannot_out_index[seat_index][common_index] += card_count;	
			
		

		if (send_client == true) {
			this.operate_cannot_card(seat_index);
		}
	}
	/**
	 * 调度,不能出的捡牌
	 **/
	public void cannot_pickup_card(int seat_index, int card_count, int card_data, boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张		
		this._cannot_pickup_index[seat_index] = new int[GameConstants.GZP_MAX_INDEX];
		Arrays.fill(this._cannot_pickup_index[seat_index], 0);
		if(card_data == 0)
		{
			if (send_client == true) {
				this.operate_cannot_pickup_card(seat_index);
			}
			return ;
		}
		int flower_index = _logic.switch_to_card_flower_index(card_data);
		int common_index = _logic.switch_to_card_common_index(card_data);
		
			
		_cannot_pickup_index[seat_index][_logic.switch_to_card_index(card_data)]+=card_count;
		if(flower_index != -1)
			_cannot_pickup_index[seat_index][flower_index] += card_count;
		if(common_index != -1)
			_cannot_pickup_index[seat_index][common_index] += card_count;
		
			
		

		if (send_client == true) {
			this.operate_cannot_pickup_card(seat_index);
		}
	}
	/**
	 * 调度,不能出的捡牌
	 **/
	public void cannot_pickup_card_ddwf(int seat_index, int card_count, int card_data, boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张		
		this._cannot_pickup_index[seat_index] = new int[GameConstants.GZP_MAX_INDEX];
		Arrays.fill(this._cannot_pickup_index[seat_index], 0);
		if(card_data == 0)
		{
			if (send_client == true) {
				this.operate_cannot_pickup_card(seat_index);
			}
			return ;
		}
		int flower_index = _logic.switch_to_card_flower_index(card_data);
		int common_index = _logic.switch_to_card_common_index(card_data);
		
			
		_cannot_pickup_index[seat_index][_logic.switch_to_card_index(card_data)]+=card_count;
		if(flower_index != -1)
			_cannot_pickup_index[seat_index][flower_index] += card_count;
		if(common_index != -1)
			_cannot_pickup_index[seat_index][common_index] += card_count;
		
			
		

		if (send_client == true) {
			this.operate_cannot_pickup_card(seat_index);
		}
	}
	public void cannot_hu_zhao_peng_card(int seat_index,int card_data){
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if(card_data == 0)
			return; 
		int flower_index = _logic.switch_to_card_flower_index(card_data);
		int common_index = _logic.switch_to_card_common_index(card_data);

		this._guo_zhao[seat_index][_logic.switch_to_card_index(card_data)]++;
		this._guo_peng[seat_index][_logic.switch_to_card_index(card_data)]++;
		if(flower_index != -1)
		{
			this._guo_peng[seat_index][flower_index]++;
			this._guo_zhao[seat_index][flower_index]++;
		}
		if(common_index != -1)
		{			
			
			this._guo_zhao[seat_index][common_index]++;
			
		
			this._guo_peng[seat_index][common_index]++;
		}
		
		
	}
	public void cannot_hu_zhao_peng_ddwf_card(int seat_index,int card_data){
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if(card_data == 0)
			return; 
		int flower_index = _logic.switch_to_card_flower_index(card_data);
		int common_index = _logic.switch_to_card_common_index(card_data);

		this._guo_zhao[seat_index][_logic.switch_to_card_index(card_data)]++;
		this._guo_peng[seat_index][_logic.switch_to_card_index(card_data)]++;
		if(flower_index != -1)
		{
			this._guo_zhao[seat_index][flower_index]++;
		}
		if(common_index != -1)
		{			
			
			this._guo_zhao[seat_index][common_index]++;
			
		
			this._guo_peng[seat_index][common_index]++;
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

//		// 刷新有癞子的牌
//		for (int i = 0; i < getTablePlayerNumber(); i++) {
//			boolean has_lai_zi = false;
//			for (int j = 0; j < getMaxIndex(); j++) {
//				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
//					has_lai_zi = true;
//					break;
//				}
//			}
//			if (has_lai_zi) {
//				// 刷新自己手牌
//				int cards[] = new int[GameConstants.GZP_MAX_COUNT];
//				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
//				for (int j = 0; j < hand_card_count; j++) {
//					if (_logic.is_magic_card(cards[j])) {
//						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
//					}
//				}
//				this.operate_player_cards(i, hand_card_count, cards, 0, null);
//			}
//		}

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


	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee,int Trustee_type) {
		if (_playerStatus == null || istrustee == null)
			return false;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee &&!isTing&& (!is_sys())&&is_robot(get_seat_index) == false) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
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
//			change_handler(_handler_dispath_last_card_twenty);
//			this._handler_dispath_last_card_twenty.reset_status(cur_player, type);
			this._handler.exe(this);
		} else {
//			change_handler(_handler_dispath_last_card);
//			this._handler_dispath_last_card.reset_status(cur_player, type);
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
		this._handler = this._handler_dispath_firstcards;
		this._handler_dispath_firstcards.reset_status(_seat_index, _type);
		this._handler.exe(this);
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
		if(this.getRuleValue(GameConstants.GAME_RULE_TC_THREE_PEOPLE) == 1)
			return 3;
		else if(this.getRuleValue(GameConstants.GAME_RULE_TC_FOUR_PEOPLE) == 1)
			return 4;
		return 4;
		//return GameConstants.GAME_PLAYER;
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
	/**
	 * 听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @returnt
	 */
	public int get_gzp_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.GZP_MAX_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
					GRR._weave_count[seat_index],_sheng_guan_index[seat_index],_pick_up_index[seat_index], cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO,seat_index,seat_index)) {
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
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION_RECORD);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);
		if(GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean is_robot(int seat_index){
		if(isEnableRobot())
			return true;
		return false;
	}
	public void change_handler(GZPHandler dest_handler) {
		_handler = dest_handler;
	}

	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[this.getTablePlayerNumber()];
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
		// this.getTablePlayerNumber()
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
			if (getPlayerCount() != this.getTablePlayerNumber() || _player_ready == null) {
				return false;
			}

			// 检查是否所有人都未准备
			int not_ready_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (get_players()[i] != null && _player_ready[i] == 0) {// 未准备的玩家
					not_ready_count++;
				}
			}
			if (not_ready_count == this.getTablePlayerNumber())// 所有人都未准备 不用踢
				return false;

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		return GameConstants.GZP_MAX_INDEX;
	}

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public  boolean handler_requst_message_deal(Player player,int seat_index, RoomRequest room_rq,int type){
		return true;
	}
	public boolean hu_pai_timer(int seat_index, int operate_card, int wik_kind)
	{
		handler_operate_card(seat_index, wik_kind,operate_card, -1);
		return false;
	}
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
