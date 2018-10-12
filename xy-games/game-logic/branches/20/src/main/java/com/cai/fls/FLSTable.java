/**
 * 
 */
package com.cai.fls;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMsgIdType;
import com.cai.common.define.ESysMsgType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoomRecord;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.Room;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.FvMask;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysParamDict;
import com.cai.fls.handler.FLSHandler;
import com.cai.fls.handler.FLSHandlerChiPeng;
import com.cai.fls.handler.FLSHandlerDispatchCard;
import com.cai.fls.handler.FLSHandlerFinish;
import com.cai.fls.handler.FLSHandlerGang;
import com.cai.fls.handler.FLSHandlerHaiDi;
import com.cai.fls.handler.FLSHandlerOutCardOperate;
import com.cai.fls.handler.FLSHandlerYaoHaiDi;
import com.cai.fls.handler.lxfls.FLSHandlerChiPeng_LX;
import com.cai.fls.handler.lxfls.FLSHandlerDispatchCard_LX;
import com.cai.fls.handler.lxfls.FLSHandlerGang_LX;
import com.cai.fls.handler.lxfls.FLSHandlerGang_LX_DispatchCard;
import com.cai.fls.handler.lxfls.FLSHandlerOutCardOperate_LX;
import com.cai.fls.handler.lxfls.FLSHandlerPiao_FLS;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.service.PtAPIServiceImpl;

import protobuf.clazz.Protocol;
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
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

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

	public int _game_type_index; // 游戏类型
	public int _game_rule_index; // 游戏规则
	public int _game_round; // 局数
	public int _cur_round; // 局数
	public int _player_ready[]; // 准备
	public int _player_open_less[]; // 允许三人场
	public float _di_fen; // 底分

	public int _game_status;

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

	public FLSHandlerFinish _handler_finish; // 结束

	public FLSTable() {
		super(RoomType.FLS);

		_logic = new FLSGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			get_players()[i] = null;

		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
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
		}
		_handler_finish = new FLSHandlerFinish();

	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	////////////////////////////////////////////////////////////////////////
	public boolean reset_init_data() {
		int game_id = 0;
		int game_type_index = this._game_type_index;
		int index = 0;
		if (game_type_index == GameConstants.GAME_TYPE_FLS_LX) {
			game_id = GameConstants.GAME_ID_FLS_LX;
			index = 1210;//
		}

		if (_cur_round == 0) {
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
						.get(index);

				if (sysParamModel != null && sysParamModel.getVal2() == 1) {
					StringBuilder buf = new StringBuilder();
					buf.append("创建房间:" + this.getRoom_id()).append("game_id:" + game_id)
							.append(",game_type_index:" + game_type_index).append(",game_round:" + _game_round);
					AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(this.getRoom_owner_account_id(),
							check_gold, false, buf.toString());
					if (result.isSuccess() == false) {
						create_result = false;
					}
				}

			}
			if (create_result == false) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
				Player player = null;
				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
				PlayerServiceImpl.getInstance().getRoomMap().remove(this.getRoom_id());
				return false;
			}

			/*
			 * || is_mj_type(MJGameConstants.GAME_TYPE_HENAN_AY) ||
			 * is_mj_type(MJGameConstants.GAME_TYPE_HENAN_LZ)
			 */
			if (is_mj_type(GameConstants.GAME_TYPE_CS)) {
				this.shuffle_players();

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					this.get_players()[i].set_seat_index(i);
					if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
						this._banker_select = i;
					}
				}
			}

			if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
				if (getTablePlayerNumber() != GameConstants.GAME_PLAYER) {
					this.shuffle_players();

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						this.get_players()[i].set_seat_index(i);
						if (this.get_players()[i].getAccount_id() == this.getRoom_owner_account_id()) {
							this._banker_select = i;
						}
					}
				}
			}

			record_game_room();
		}

		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_FLS, GameConstants.MAX_FLS_COUNT,
				GameConstants.MAX_FLS_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[4];
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
			room_player.setOpenThree(_player_open_less[i]==0?false:true);
			if(rplayer.locationInfor!=null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);
		}

		GRR._video_recode.setBankerPlayer(this._banker_select);

		return true;
	}

	// 游戏开始
	public boolean handler_game_start() {

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _banker_select;
		_current_player = GRR._banker_player;

		//
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_FLS_LX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_FLS_LX);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			return game_start_fls();
		}
		return false;
	}

	/**
	 * 开始 河南麻将
	 * 
	 * @return
	 */
	private boolean game_start_fls() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			this._handler = this._handler_piao_fls;
			this._handler_piao_fls.exe(this);
			return true;
		} else {
			for (int i = 0; i < playerCount; i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
		}

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

		return true;
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
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

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
		// int cards[] = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,
		// 0x03,0x03,0x03, 0x11,0x11,0x11, 0x12,0x12,0x12,0x13,0x13,0x13};

		// int cards[] = new int[] { 0x02,0x02,0x02,0x23,0x23,0x23,
		// 0x03,0x03,0x03, 0x11,0x11,0x11, 0x12,0x12,0x12,0x13,0x13,0x13};

		// 上大人 丘乙己 来牌 0x13
		// int cards[] = new int[] { 0x01,0x01,0x01,0x02,0x02,0x02,
		// 0x03,0x03,0x03, 0x11,0x11,0x11, 0x12,0x12,0x12,0x13,0x13,0x23};

		// 双对缺1 碰碰胡
//		int cards[] = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x11, 0x11, 0x11, 0x12, 0x12,
//				0x12, 0x13, 0x13, 0x23 };

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
		int cards[] = new int[] { 0x31, 0x32, 0x33, 0x21, 0x22, 0x23, 0x11, 0x12, 0x13, 0x41, 0x42, 0x43, 0x51, 0x51,
				0x51, 0x71, 0x72, 0x73 };

		// 上大人 福禄寿 2句话
		// int cards[] = new int[] { 0x01, 0x02, 0x03, 0x21, 0x22, 0x23, 0x11,
		// 0x12, 0x13, 0x41, 0x42, 0x43, 0x51, 0x52,
		// 0x53, 0x71, 0x72, 0x73 };

		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x02, 0x23, 0x11, 0x12,
		// 0x13, 0x31, 0x12, 0x51, 0x51, 0x51,0x52,
		// 0x71, 0x71, 0x71, 0x71 };

		// 4个人牌不一样 ---打开下面的代码 配置牌型即可
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// for (int j = 0; j < MJGameConstants.MAX_FLS_INDEX; j++) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_FLS_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int send_count = (GameConstants.MAX_FLS_COUNT - 2);
			if (isZhuangDui(i)) {// 庄家对面发2张
				send_count = 2;
			}
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		// int[] realyCards = new int[] {8, 22, 24, 18, 3, 8, 36, 1, 23, 25, 24,
		// 34, 37, 23, 41, 34, 23, 20, 19, 25, 25, 9, 1, 2, 3, 39, 4, 1, 5, 17,
		// 53, 2, 37, 23, 18, 8, 21, 6, 38, 3, 33, 53, 33, 36, 22, 53, 36, 4,
		// 22, 33, 21, 35, 38, 9, 19, 38, 20, 40, 21, 6, 38, 24, 24, 3, 53, 17,
		// 35, 33, 37, 18, 17, 34, 35, 20, 8, 9, 36, 5, 41, 20, 7, 19, 4, 5, 40,
		// 1, 34, 18, 37, 35, 40, 41, 22, 39, 2, 21, 19, 6, 41, 7, 5, 9, 7, 4,
		// 7, 2, 39, 40, 39, 25, 6, 17};
		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 18) {
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
			for (int j = 0; j < GameConstants.MAX_FLS_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		_banker_select = 0;
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
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_FLS_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 13; j++) {
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
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			return this.handler_game_finish_fls(seat_index, reason);
		}
		return false;
	}

	public boolean handler_game_finish_fls(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		game_end.setRoomInfo(room_info);

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
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
				for (int j = 0; j < getTablePlayerNumber(); j++) {
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
				game_end.setPlayerResult(this.process_player_result());
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
			game_end.setPlayerResult(this.process_player_result());
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
		GRR = null;

		// 错误断言
		return false;
	}

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type) {

		if (GameConstants.GAME_TYPE_FLS_LX == _game_type_index) {
			return analyse_chi_hu_card_fls(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type);
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
		int cbCardIndexTemp[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
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

		if (!isZhuangDui) {
			if (!isScoreEnough(analyseItemArray, downScore)) {
				return GameConstants.WIK_NULL;
			}
		}

		boolean isPengPengHu = false;

		if (isZhuangDui) {
			for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
				if (cbCardIndexTemp[i] == 3) {
					isPengPengHu = true;
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_FLS_GUNGUN);// 3张一样自摸为“滚滚”
					} else {
						chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);// 接炮为“碰碰胡”
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
				if (_logic.is_pengpeng_hu(analyseItem) && _logic.is_pengpeng_hu_down(weaveItems)) {
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

		if (weaveCount == 0 && card_type == GameConstants.HU_CARD_TYPE_ZIMO) {// 自摸才算
																				// 门清
			chiHuRight.opr_or(GameConstants.CHR_FLS_MENQING);
		}

		if (_logic.get_card_count_by_index(cards_index) == 0) {// 手牌为0
			chiHuRight.opr_or(GameConstants.CHR_FLS_MANTIAN_FEI);// 满天飞

			isPengPengHu = _logic.is_pengpeng_hu_down(weaveItems);
			if (isPengPengHu) {
				chiHuRight.opr_or(GameConstants.CHR_FLS_PENGPENGHU);
			}

		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		// 可接炮的情况 1.门清 2.庄家的对家（只有两张牌） 3.碰碰胡
		if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			boolean hu = weaveCount == 0 || isZhuangDui || isPengPengHu;
			if (!hu) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
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
	 * 分数是否足够
	 * 
	 * @param analyseItemArray
	 * @return
	 */
	private boolean isScoreEnough(List<AnalyseItem> analyseItemArray, int downScore) {
		if (downScore >= GameConstants.MIN_SCORE_FLS) {
			return true;
		}

		int bestScore = 0;
		boolean handJin = false;// 手里是否有金
		boolean hasKan = false;// 手里是否有坎
		for (AnalyseItem analyseItem : analyseItemArray) {// 计算手牌分数
			int length = analyseItem.cbWeaveKind.length;
			int score = 0;
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
						score += 3;
					}
					hasKan = true;
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

			boolean full = isEyeScoreFull(analyseItem, handJin, hasKan, score, downScore);

			if (full) {
				return full;
			}

		}
		return false;
	}

	/***
	 * @return
	 */
	private boolean isEyeScoreFull(AnalyseItem analyseItem, boolean hasJin, boolean hasKan, int handScore,
			int downScore) {
		logger.warn("牌眼 索引 ====" + Arrays.toString(analyseItem.cbCardEye));
		// 把牌眼复制一份
		int cbCardIndexTemp[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < analyseItem.cbCardEye.length; i++) {
			if (analyseItem.cbCardEye[i] == 0)
				continue;
			int index = _logic.switch_to_card_index(analyseItem.cbCardEye[i]);
			cbCardIndexTemp[index] += 1;
		}

		// 判断成句的牌眼
		int eyeScore = 0;
		if (analyseItem.eyeKind == GameConstants.WIK_PENG) {
			if (analyseItem.eyeCenterCard == 0x01 || analyseItem.eyeCenterCard == 0x71) {
				eyeScore += 24;
			} else {
				eyeScore += 3;
			}
			cbCardIndexTemp[_logic.switch_to_card_index(analyseItem.eyeCenterCard)] = 0;// 将算了分的牌眼去掉
			hasKan = true;
		} else if (analyseItem.eyeKind == GameConstants.WIK_LEFT) {
			if (analyseItem.eyeCenterCard == 0x01 || analyseItem.eyeCenterCard == 0x71) {// 只有“上大人”“福禄寿”两句话各算4胡息
				eyeScore += 4;
				hasJin = true;

				int index = _logic.switch_to_card_index(analyseItem.eyeCenterCard);
				cbCardIndexTemp[index] = 0;// 将算了分的牌眼去掉
				cbCardIndexTemp[index + 1] = 0;
				cbCardIndexTemp[index + 2] = 0;

			}
		}

		boolean isCountShangFu = false;// 是否计算了 单个上福
		boolean hasCountJinKan = false;// 是否 抵金 抵坎了
		boolean isCountPeng = false;
		for (int i = 0; i < cbCardIndexTemp.length; i++) {
			if (cbCardIndexTemp[i] == 0)
				continue;
			int data = _logic.switch_to_card_data(i);
			if (cbCardIndexTemp[i] == 1) {// 单牌
				if (data == 0x71 || data == 0x01) {// 上 福
					eyeScore += 8;
					isCountShangFu = true;
				}
				if (hasJin && hasKan) {// 在真金真坎的情况下，大、人、禄、寿、单个子也可以算4胡息
					if (data == 0x72 || data == 0x72 || data == 0x02 || data == 0x03) {// 大人
																						// 禄寿
						eyeScore += 4;
						isCountShangFu = true;// 最后就不用算了
						hasCountJinKan = true;
					}
				}
			} else if (cbCardIndexTemp[i] == 2) {// 对子只算一个
				if (data == 0x71 || data == 0x01) {
					eyeScore += 16;
				} else {
					if (!isCountPeng) {
						if (hasJin && !hasCountJinKan) {// 手中有上或者福2句话 并且没有抵坎
							eyeScore += 3;
							hasCountJinKan = true;
						} else {
							eyeScore += 2;
						}
						isCountPeng = true;
					}
				}
			}
		}

		if (!isCountShangFu && hasJin) {// 看看能不能抵金 （大人或者 禄寿）
			for (int i = 0; i < cbCardIndexTemp.length; i++) {
				if (cbCardIndexTemp[i] == 0 || cbCardIndexTemp[i] == 2)// 过滤掉
																		// 成对子的
					// 过滤掉
					continue;
				int data = _logic.switch_to_card_data(i);
				if (data != 0x71 && data != 0x01) {
					if (_logic.get_card_color(data) == 7 || _logic.get_card_color(data) == 0) {
						if ((i + 1) < cbCardIndexTemp.length && cbCardIndexTemp[i + 1] != 0
								&& (_logic.get_card_color(_logic.switch_to_card_data(i + 1)) == 7
										|| _logic.get_card_color(_logic.switch_to_card_data(i + 1)) == 0)) {
							eyeScore += 4;
							if (hasCountJinKan)
								eyeScore -= 1;// 抵金了不抵坎
							break;// 只抵一次
						}
					}
				}

			}
		}
		int max_score = GameConstants.MIN_SCORE_FLS;// 手中有“上福”
		if (handScore + downScore + eyeScore >= max_score) {
			logger.warn("table roomID ==" + getRoom_id() + " 手牌分数===" + handScore + " 落地分数==" + downScore + " 牌眼分数=="
					+ eyeScore + "牌眼值==" + Arrays.toString(analyseItem.cbCardEye));
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
	public int get_fls_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_FLS_INDEX;

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
			// 杠牌判断 如果剩余牌大于1，是否有杠,剩一张为海底
			if (GRR._left_card_count > 1) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_ZHAO);
					playerStatus.add_zhao(card, seat_index, 1);// 加上补张

					// 剩一张为海底
					if (GRR._left_card_count > 1) {
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

							GRR._weave_items[i][cbWeaveIndex].public_card = 0;
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
			// 如果是自摸胡
			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

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
					GameConstants.HU_CARD_TYPE_QIANGGANG);

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
				action = _logic.check_peng(GRR._cards_index[seat_index], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);

					bAroseAction = true;
				}
			}
		}

		if (check_chi) {
			int chi_seat_index = (provider + 1) % getTablePlayerNumber();

			if (!isZhuangDui(chi_seat_index)) {
				if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
					// 长沙麻将吃操作 转转麻将不能吃
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

		}

		// 杠牌判断 如果剩余牌大于1，是否有杠
		if (GRR._left_card_count > 1) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 3) {
				playerStatus.add_action(GameConstants.WIK_ZHAO);
				playerStatus.add_bu_zhang(card, provider, 1);// 加上补涨

				if (GRR._left_card_count > 2) {
					boolean is_ting = false;
					boolean can_gang = false;
					if (is_ting == true) {
						can_gang = true;
					} else {
						// 把可以杠的这张牌去掉。看是不是听牌
						int bu_index = _logic.switch_to_card_index(card);
						int save_count = GRR._cards_index[seat_index][bu_index];
						GRR._cards_index[seat_index][bu_index] = 0;

						int cbWeaveIndex = GRR._weave_count[seat_index];

						GRR._weave_items[seat_index][cbWeaveIndex].public_card = 1;
						GRR._weave_items[seat_index][cbWeaveIndex].center_card = card;
						GRR._weave_items[seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;// 接杠
						GRR._weave_items[seat_index][cbWeaveIndex].provide_player = provider;
						GRR._weave_count[seat_index]++;

						can_gang = this.is_fls_ting_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
								GRR._weave_count[seat_index]);

						GRR._weave_count[seat_index] = cbWeaveIndex;
						GRR._cards_index[seat_index][bu_index] = save_count;
					}

					if (can_gang == true) {

						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, provider, 1);// 加上杠

					}
				}

				bAroseAction = true;
			}

		}

		// 吃胡判断
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
		// chr.set_empty();
		int cbWeaveCount = GRR._weave_count[seat_index];
		action = analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], cbWeaveCount, card,
				chr, GameConstants.HU_CARD_TYPE_PAOHU);

		// 结果判断
		if (action != 0) {
			chr.opr_or(GameConstants.CHR_FLS_GANGSHANGPAO);
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
	public boolean handler_create_room(Player player) {
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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (get_players()[i] == null) {
				get_players()[i] = player;
				seat_index = i;
				break;
			}
		}
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		if (_cur_round == 0 && !has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;
				if (tarplayer.get_seat_index() == seat_index)
					continue;
				if (StringUtils.isNotEmpty(tarplayer.getAccount_ip()) && StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					player.setRoom_id(0);// 把房间信息清除--
					send_error_notify(seat_index, 1, "不允许相同ip进入");
					return false;
				}
			}
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
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		if (this._cur_round != 0) {
			return false;
		}

		if (_player_ready[player.get_seat_index()] == 0) {
			return false;
		}

		int less = openThree ? 1 : 0;
		_player_open_less[player.get_seat_index()] = less;

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

		if (openLess == readys && readys >= GameConstants.GAME_PLAYER - 1) {
			playerNumber = readys;
			handler_game_start();
		}

		return false;
	}

	@Override
	public boolean handler_player_ready(int seat_index) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (!has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			Player player = get_players()[seat_index];
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;
				if (tarplayer.get_seat_index() == seat_index)
					continue;
				if (player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					send_error_notify(seat_index, 1, "不允许相同ip进入");
					return false;
				}
			}
		}

		_player_ready[seat_index] = 1;
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (_player_ready[i] == 0) {
				return false;
			}
		}

		handler_game_start();
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
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card) {

		if (this._handler != null) {
			this._handler.handler_operate_card(this, seat_index, operate_code, operate_card);
		}

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
		PlayerServiceImpl.getInstance().getRoomMap().remove(this.getRoom_id());

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

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list,
					1, GameConstants.INVALID_SEAT);
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
	 * 长沙
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
		int cards[] = new int[GameConstants.MAX_FLS_INDEX];
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
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			// 跑和呛
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		_playerStatus[seat_index].clean_status();
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_AUDIO_CHAT);
		roomResponse.setOperatePlayer(player.get_seat_index());
		roomResponse.setAudioChat(chat);
		roomResponse.setAudioSize(l);
		roomResponse.setAudioLen(audio_len);
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
		int seat_index = player.get_seat_index();

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 60;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == getTablePlayerNumber()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				// 删除房间
				PlayerServiceImpl.getInstance().getRoomMap().remove(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出

			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(player.getAccount_id());

			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);
		}
			break;

		}

		return true;

	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		if (_handler_piao_fls != null) {
			return _handler_piao_fls.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		}

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
			roomResponse.setStandTime(250);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(150);

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
			roomResponse.setStandTime(150);
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
		GRR.add_room_response(roomResponse);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

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
			// GRR.add_room_response(roomResponse);
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
		// GRR.add_room_response(roomResponse);
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

		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			roomResponse.setFlashTime(300);
			roomResponse.setStandTime(300);
		}

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
				GRR.add_room_response(roomResponse);
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

	private PlayerResultResponse.Builder process_player_result() {

		// 大赢家
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);

			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addGunGunCount(_player_result.gun_gun[i]);
			player_result.addPengPengHuCount(_player_result.peng_peng_hu[i]);
			player_result.addGangShangHuaCount(_player_result.gang_shang_hua[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
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

	/***
	 * 加载房间的玩法 状态信息
	 * 
	 * @param roomResponse
	 */
	public void load_room_info_data(RoomResponse.Builder roomResponse) {
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
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
			room_player.setOpenThree(_player_open_less[i]==0?false:true);
			if(rplayer.locationInfor!=null) {
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
		PlayerServiceImpl.getInstance().send(this.get_players()[seat_index], responseBuilder.build());

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
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			_gameRoomRecord.setGame_id(GameConstants.GAME_ID_FLS_LX);
		}

		_recordRoomRecord = MongoDBServiceImpl.getInstance().parentBrand(_gameRoomRecord.getGame_id(),
				this.get_record_id(), "", _gameRoomRecord.to_json(), (long) this._game_round,
				(long) this._game_type_index, this.getRoom_id() + "");

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			MongoDBServiceImpl.getInstance().accountBrand(_gameRoomRecord.getGame_id(),
					this.get_players()[i].getAccount_id(), this.get_record_id());
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
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			// 记录 to mangodb
			MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "",
					null, null, gzipByte, this.getRoom_id() + "",
					_recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray());

		}

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

					if (type == GameConstants.CHR_FLS_MENQING) {
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

	// 转转麻将结束描述
	private void set_result_describe() {
		if (this.is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			set_result_describe_fls();
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
		int cbCardIndexTemp[] = new int[GameConstants.MAX_FLS_INDEX];
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_fls(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO))
				return true;
		}
		return false;
	}

	public String get_game_des() {
		String des = "";
		if (is_mj_type(GameConstants.GAME_TYPE_FLS_LX)) {
			return get_game_des_fls();
		}
		return des;
	}

	private String get_game_des_fls() {
		String des = "";
		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {
			des += "轮庄";
		} else {
			des += "胡牌者庄";
		}

		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_PIAO)) {
			des += " 飘分";
		}

		if (has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_IP)) {
			if (des.length() == 0) {
				des += "允许相同ip进入";
			} else {
				des += "\n" + "允许相同ip进入";
			}
		} else {
			des += "\n" + "不允许相同ip进入";
		}

		return des;
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
		this._handler = this._handler_gang_fls;
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
		this._handler = this._handler_hai_di;
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
		this._handler = this._handler_yao_hai_di;
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
		this._handler = this._handler_out_card_operate;
		this._handler_out_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		// 出牌
		this._handler = this._handler_chi_peng;
		this._handler_chi_peng.reset_status(seat_index, _out_card_player, action, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card),
				GameConstants.DELAY_JIAN_PAO_HU, TimeUnit.MILLISECONDS);

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
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_FLS_INDEX; j++) {
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
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
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
			PlayerServiceImpl.getInstance().getRoomMap().remove(this.getRoom_id());

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

//		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
//		roomResponse.setType(MsgConstants.RESPONSE_LOCATION);
//		player.locationInfor = locationInfor;
//		for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
//			Player tarPlayer = this.get_players()[j];
//			if (tarPlayer == null || tarPlayer.locationInfor == null)
//				continue;
////			roomResponse.addLocationInfor(tarPlayer.locationInfor);
//		}
//		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int _seat_index, int _type, boolean _tail) {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if (playerNumber == 0) {
			playerNumber = GameConstants.GAME_PLAYER;
		}
		return playerNumber;
	}

}
