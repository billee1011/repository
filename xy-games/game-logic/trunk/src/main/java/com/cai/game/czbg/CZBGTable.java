package com.cai.game.czbg;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.czbg.CZBGConstants;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.CZBGStartRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.TrusteeRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.czbg.handler.CZBGHandler;
import com.cai.game.czbg.handler.CZBGHandlerAddJetton;
import com.cai.game.czbg.handler.CZBGHandlerCallBanker;
import com.cai.game.czbg.handler.CZBGHandlerFinish;
import com.cai.game.czbg.handler.CZBGHandlerOpenCard;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.czbg.CZBGRsp.AddJetton_CZBG;
import protobuf.clazz.czbg.CZBGRsp.CallBankerInfo_CZBG;
import protobuf.clazz.czbg.CZBGRsp.CardType_CZBG;
import protobuf.clazz.czbg.CZBGRsp.GameStart_CZBG;
import protobuf.clazz.czbg.CZBGRsp.OpenCardRequest_CZBG;
import protobuf.clazz.czbg.CZBGRsp.OpenCard_CZBG;
import protobuf.clazz.czbg.CZBGRsp.Prompt_CZBG;
import protobuf.clazz.czbg.CZBGRsp.PukeGameEndCZBG;
import protobuf.clazz.czbg.CZBGRsp.SendCard_CZBG;

/**
 * 郴州八怪
 * 
 * @author hexinqi
 *
 */
public class CZBGTable extends AbstractRoom {

	private static final long serialVersionUID = 1L;

	// private static Logger logger = Logger.getLogger(CZBGTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	public ScheduledFuture<?> _trustee_schedule[];

	public int _call_banker[]; // 用户是否叫庄
	public int _add_Jetton[]; // 用户下注
	public boolean _open_card[]; // 用户摊牌
	public int _cur_call_banker; // 叫庄用户
	public int _next_banker; // 下一轮的庄家
	public boolean _player_status[]; // 用户状态
	public int _player_count; // 用户数量
	public int _jetton_info_sever[]; // 下注数据
	public int _jetton_count; // 筹码个数
	public int _cur_jetton_count[]; // 可以真实下注筹码个数
	public int _jetton_info_cur[][]; // 当前下注数据
	public int _call_banker_info[]; // 叫庄信息
	public int _banker_times; // 庄家倍数
	public int _banker_max_times; // 庄家最大倍数
	public int _can_tuizhu_player[]; // 可以推注用户
	public CZBGGameLogic _logic = null;
	public int _operate_start_time; // 操作开始时间
	public int _cur_operate_time; // 可操作时间
	public int _cur_game_timer; // 当前游戏定时器
	public int _trustee_type[];// 托管的内容
	public boolean _wait_cancel_trustee[];// 取消托管
	public int is_game_start; // 游戏是否开始
	public int _own_room_seat; // 房主的位置
	public int roundScore[];
	public int douNiuScore[];
	public int gameScore[];
	public int baseScore; // 底分
	public CZBGCardGroup[] cardGroup;

	private long _request_release_time;
	private ScheduledFuture<?> _release_scheduled;
	private ScheduledFuture<?> _table_scheduled;
	private ScheduledFuture<?> _game_scheduled;

	public CZBGHandler _handler;

	public CZBGHandlerOpenCard _handler_open_card;
	public CZBGHandlerAddJetton _handler_add_jetton;
	public CZBGHandlerCallBanker _handler_call_banker;
	public CZBGHandlerFinish _handler_finish; // 结束
	public int beishu; // 抢庄倍数
	public boolean canBaoDao[];

	public CZBGTable() {
		super(RoomType.OX, CZBGConstants.CZBG_RULE_PLAYER_6);

		_logic = new CZBGGameLogic();

		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			get_players()[i] = null;
		}
		_game_status = CZBGConstants.GS_CZBG_FREE;
		// 游戏变量
		_player_ready = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		_player_open_less = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		_call_banker = new int[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户是否叫庄
		_add_Jetton = new int[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户下注
		_open_card = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户摊牌
		_cur_call_banker = 0;
		_player_status = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];
		_call_banker_info = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		_can_tuizhu_player = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		_trustee_type = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		_wait_cancel_trustee = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];// 取消托管
		roundScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6];// 当局得分
		douNiuScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6];// 斗牛得分
		gameScore = new int[CZBGConstants.CZBG_RULE_PLAYER_6];// 当局得分
		canBaoDao = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];// 当局得分
		is_game_start = 0;
		_own_room_seat = -1;
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_can_tuizhu_player[i] = 0;
			_trustee_type[i] = 0;
			_wait_cancel_trustee[i] = false;
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
		_own_room_seat = -1;
		_jetton_info_sever = new int[CZBGConstants.CZBG_RULE_PLAYER_6];

		if (has_rule(CZBGConstants.CZBG_RULE_BASE_SCORE_1)) {
			Arrays.fill(_jetton_info_sever, 1);
			baseScore = 1;
		} else if (has_rule(CZBGConstants.CZBG_RULE_BASE_SCORE_2)) {
			Arrays.fill(_jetton_info_sever, 2);
			baseScore = 2;
		} else if (has_rule(CZBGConstants.CZBG_RULE_BASE_SCORE_3)) {
			Arrays.fill(_jetton_info_sever, 3);
			baseScore = 3;
		} else {
			Arrays.fill(_jetton_info_sever, 4);
			baseScore = 4;
		}
		_cur_jetton_count = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		Arrays.fill(_cur_jetton_count, 0);
		_jetton_info_cur = new int[CZBGConstants.CZBG_RULE_PLAYER_6][11];
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			for (int j = 0; j < 10; j++) {
				_jetton_info_cur[i][j] = j + 1;
			}
		}
		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
		}
		istrustee = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];
		_trustee_type = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		_wait_cancel_trustee = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];// 取消托管
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_trustee_type[i] = 0;
			istrustee[i] = false;
			_wait_cancel_trustee[i] = false;
		}
		game_cell = 1;
		_banker_times = 1;
		_banker_max_times = 1;
		is_game_start = 0;
		// 新建
		_playerStatus = new PlayerStatus[CZBGConstants.CZBG_RULE_PLAYER_6];

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), CZBGConstants.CZBG_RULE_PLAYER_6);

		_call_banker = new int[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户是否叫庄
		_add_Jetton = new int[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户下注
		_open_card = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户摊牌
		_player_status = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}
		_cur_jetton_count = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		Arrays.fill(_cur_jetton_count, 0);
		_jetton_info_cur = new int[CZBGConstants.CZBG_RULE_PLAYER_6][11];
		this.cardGroup = new CZBGCardGroup[CZBGConstants.CZBG_RULE_PLAYER_6];
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			for (int j = 0; j < 10; j++) {
				_jetton_info_cur[i][j] = j + 1;
			}
			this.cardGroup[i] = new CZBGCardGroup();
		}
		_cur_call_banker = 0;
		_cur_banker = 0;
		_banker_times = 1;

		_handler_open_card = new CZBGHandlerOpenCard();
		_handler_add_jetton = new CZBGHandlerAddJetton();
		_handler_call_banker = new CZBGHandlerCallBanker();
		_handler_finish = new CZBGHandlerFinish();

		this.setMinPlayerCount(2);
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	public boolean has_trustee(int cbRule, int seat_index) {
		return FvMask.has_any(this._trustee_type[seat_index], FvMask.mask(cbRule));
	}

	public boolean reset_init_data() {
		record_game_room();

		GRR = new GameRoundRecord(CZBGConstants.CZBG_RULE_PLAYER_6, GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT,
				GameConstants.MAX_HH_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[CZBGConstants.CZBG_RULE_PLAYER_6];

		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_playerStatus[i] = new PlayerStatus(CZBGConstants.OX_CARD_COUNT);
		}
		_cur_round++;
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_playerStatus[i].reset();
		}
		_call_banker = new int[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户是否叫庄
		_add_Jetton = new int[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户下注
		_open_card = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6]; // 用户摊牌
		_player_status = new boolean[CZBGConstants.CZBG_RULE_PLAYER_6];
		_cur_jetton_count = new int[CZBGConstants.CZBG_RULE_PLAYER_6];
		Arrays.fill(_cur_jetton_count, 0);
		Arrays.fill(roundScore, 0);
		Arrays.fill(douNiuScore, 0);
		Arrays.fill(_call_banker, 0);
		Arrays.fill(canBaoDao, false);
		_jetton_info_cur = new int[CZBGConstants.CZBG_RULE_PLAYER_6][11];
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			for (int j = 0; j < 10; j++) {
				_jetton_info_cur[i][j] = j + 1;
			}
		}
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}
		_operate_start_time = 0; // 操作开始时间
		_cur_operate_time = 0; // 可操作时间
		_cur_game_timer = 0;
		beishu = 1;
		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				GRR._room_info.addGameRuleIndexEx(ruleEx[i]);
			}

		}
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());

		Player rplayer;
		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
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
	@Override
	protected boolean on_handler_game_start() {
		_game_status = CZBGConstants.GS_CZBG_FREE;

		reset_init_data();

		if (has_rule(CZBGConstants.CZBG_RULE_TRANSFORM_WITH_KING)) {
			_repertory_card = new int[CZBGConstants.CARD_DATA_WITH_KING.length];
			shuffle(_repertory_card, CZBGConstants.CARD_DATA_WITH_KING);
		} else {
			_repertory_card = new int[CZBGConstants.CARD_DATA_WITH_OUT_KING.length];
			shuffle(_repertory_card, CZBGConstants.CARD_DATA_WITH_OUT_KING);
		}

		if (this._cur_round == 1) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.cardGroup[i].setTable(this);
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		if (has_rule(CZBGConstants.CZBG_RULE_BANKER_WITH_RATE)) { // 有倍抢庄才需要抢庄
			this._game_status = CZBGConstants.GS_CZBG_CALL_BANKER;
			this._cur_banker = -1;
			return game_start_banker_with_rate();
		} else if (has_rule(CZBGConstants.CZBG_RULE_WITH_OUT_BANKER)) { // 通比直接下注
			Arrays.fill(this._call_banker, -1);
		} else if (has_rule(CZBGConstants.CZBG_RULE_BANKER_WITH_FIXED)) { // 固定庄
			Arrays.fill(this._call_banker, -1);
			if (this._cur_round == 1) {
				int count = 0;
				int seat[] = new int[this.getTablePlayerNumber()];
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null) {
						continue;
					}
					seat[count++] = i;
				}
				this._cur_banker = seat[RandomUtil.getRandomNumber(count)];
				this._call_banker[this._cur_banker] = 1;
			}
			this._call_banker[this._cur_banker] = 1;
		} else if (has_rule(CZBGConstants.CZBG_RULE_BANKER_WITH_TRUN)) { // 轮庄
			if (this._cur_round == 1) { // 第一局随机庄
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] != null) {
						this._call_banker[i] = 1;
					}
				}
			} else { // 第二局开始轮庄
				Arrays.fill(this._call_banker, -1);
				int count = 0;
				int seatIndex = 0;
				do {
					seatIndex = (this._cur_banker + 1 + count) % this.getTablePlayerNumber();
					count++;
					if (this.get_players()[seatIndex] == null) {
						continue;
					}
					break;
				} while (count < this.getTablePlayerNumber());
				this._call_banker[seatIndex] = 1;
			}
		}
		switch_add_jetton();

		return true;
	}

	public boolean send_call_banker() {
		this._game_status = CZBGConstants.GS_CZBG_CALL_BANKER;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setRoomInfo(this.getRoomInfo());
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_CALL_BANKER);

		CallBankerInfo_CZBG.Builder callBanker = CallBankerInfo_CZBG.newBuilder();
		callBanker.addCallButton(-1);
		if (this.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			for (int i = 1; i < 4; i++) {
				callBanker.addCallButton(i);
			}
		} else {
			callBanker.addCallButton(4);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(callBanker));

		RoomUtil.send_response_to_room(this, roomResponse);

		this.switch_call_banker();

		return true;
	}

	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = RoomInfo.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				room_info.addGameRuleIndexEx(ruleEx[i]);
			}
		}
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());

		return room_info;
	}

	public void switch_call_banker() {
		this._handler = _handler_call_banker;
	}

	public void switch_add_jetton() {
		this._handler = _handler_add_jetton;
		this._game_status = CZBGConstants.GS_CZBG_GAME_XIA_ZHU;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setRoomInfo(this.getRoomInfo());
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_ADD_JETTON);

		AddJetton_CZBG.Builder addJetton = AddJetton_CZBG.newBuilder();
		addJetton.setMinValue(1);
		addJetton.setMaxValue(baseScore);

		int maxValue = -1, count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._call_banker[i] > maxValue) {
				maxValue = this._call_banker[i];
			}
		}
		CallBankerInfo_CZBG.Builder callBankerInfo = CallBankerInfo_CZBG.newBuilder();
		if (-1 == maxValue) {
			this._cur_banker = -1;
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._call_banker[i] == maxValue) {
					this._call_banker_info[count++] = i;
					callBankerInfo.addCallBankerInfo(i);
				}
			}
			this._cur_banker = this._call_banker_info[RandomUtil.getRandomNumber(count)];
		}
		beishu = maxValue > 0 ? maxValue : 1;
		addJetton.setCurBanker(this._cur_banker);
		addJetton.setCallBanker(callBankerInfo);
		if (this._cur_banker >= 0) {
			addJetton.setMinValue(this._call_banker[this._cur_banker]); // low
																		// 这里不新增键值了
																		// 用这个表示抢庄玩家的倍数
		} else {
			addJetton.setMinValue(-1); // low 这里不新增键值了 用这个表示抢庄玩家的倍数
		}
		roomResponse.setRoomInfo(this.getRoomInfo());

		roomResponse.setCommResponse(PBUtil.toByteString(addJetton));

		RoomUtil.send_response_to_room(this, roomResponse);
	}

	public boolean sendCard() {
		GRR._banker_player = this._cur_banker;

		if (has_rule(CZBGConstants.CZBG_RULE_WITH_OUT_BANKER)) {
			this._cur_banker = -1;
			GRR._banker_player = this._cur_banker;
			if (has_rule(CZBGConstants.CZBG_RULE_BA_GUAI)) {
				return game_start();
			}
			return game_start_without_banker();
		} else if (has_rule(CZBGConstants.CZBG_RULE_BANKER_WITH_FIXED)) { // 固定庄
			return game_start_banker_with_fixed();
		} else if (has_rule(CZBGConstants.CZBG_RULE_BANKER_WITH_TRUN)) { // 轮庄
			return game_start_banker_with_turn();
		} else { // 有倍数抢庄
			this._game_status = CZBGConstants.GS_CZBG_GAME_XIA_ZHU;
			return game_start_banker_with_rate();
		}
	}

	public int getPlayerCount() {
		int _cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}
		return _cur_count;
	}

	/**
	 * 轮庄
	 * 
	 * @return
	 */
	public boolean game_start_banker_with_turn() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_status[i] = this.get_players()[i] != null;
		}

		if (has_rule(CZBGConstants.CZBG_RULE_BA_GUAI)) {
			return game_start();
		}

		return game_start_without_banker();
	}

	/**
	 * 有倍抢庄
	 * 
	 * @return
	 */
	public boolean game_start_banker_with_rate() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_status[i] = this.get_players()[i] != null;
		}

		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		this.set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		int cardCount = CZBGConstants.MAX_CARD_COUNT;
		// 先牛后怪 是看四张抢庄
		if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			cardCount = CZBGConstants.OX_CARD_COUNT;
			if (this._game_status == CZBGConstants.GS_CZBG_CALL_BANKER) {
				cardCount = CZBGConstants.OX_CARD_COUNT - 1;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] != true) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();

			gameStart.setRoomInfo(this.getRoomInfo());
			gameStart.setCurBanker(this._cur_banker);
			gameStart.setIsFifth(cardCount == CZBGConstants.OX_CARD_COUNT);
			SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < cardCount; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < cardCount; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}
						this.cardGroup[i].reset(GRR._cards_data[i], cardCount == 4 ? CZBGConstants.OX_CARD_COUNT : cardCount);
					} else {
						for (int j = 0; j < cardCount; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}
				this.GRR._video_recode.addHandCards(k, cards);
				sendCard.addSendCard(k, cards);
				gameStart.setSendCard(sendCard);

			}
			this.load_room_info_data(roomResponse);
			gameStart.setDisplayTime(this._cur_operate_time);
			if (cardCount == CZBGConstants.MAX_CARD_COUNT) {
				this.cardGroup[i].cards = new int[CZBGConstants.MAX_CARD_COUNT];
				for (int k = 0; k < cardCount; k++) {
					this.cardGroup[i].cards[k] = this.GRR._cards_data[i][k];
				}
				this.canBaoDao[i] = this._logic.judgeEight(this.cardGroup[i], cardCount);
			}
			gameStart.setCanBaoDao(this.canBaoDao[i]);
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(gameStart));
			if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
				roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START);
				this.send_response_to_player(i, roomResponse);
			} else {
				roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_GUAI);
				if (this._add_Jetton[i] > 0 || i == this._cur_banker) {
					this.send_response_to_player(i, roomResponse);
				}
			}
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START);
		} else {
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_GUAI);
		}
		GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();
		gameStart.setIsFifth(cardCount == CZBGConstants.OX_CARD_COUNT);
		gameStart.setRoomInfo(this.getRoomInfo());
		gameStart.setCurBanker(this._cur_banker);
		// 发送数据
		SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
		for (int k = 0; k < this.getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] == true) {
				for (int j = 0; j < cardCount; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			} else {
				for (int j = 0; j < cardCount; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			}
			sendCard.addSendCard(k, cards);
		}
		gameStart.setSendCard(sendCard);
		roomResponse.setCommResponse(PBUtil.toByteString(gameStart));

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);

		if (this._game_status == CZBGConstants.GS_CZBG_CALL_BANKER) {
			this.send_call_banker();
		} else {
			if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
				this._game_status = CZBGConstants.GS_CZBG_OPEN_CARD;
			} else {
				this._game_status = CZBGConstants.GS_CZBG_OPEN_CARD_GUAI;
			}

			this._handler = this._handler_open_card;
		}

		return true;
	}

	/**
	 * 固定庄家
	 * 
	 * @return
	 */
	private boolean game_start_banker_with_fixed() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_status[i] = this.get_players()[i] != null;
		}

		if (has_rule(CZBGConstants.CZBG_RULE_BA_GUAI)) {
			return game_start();
		}
		return game_start_without_banker();
	}

	private boolean game_start_without_banker() {
		this._game_status = CZBGConstants.GS_CZBG_OPEN_CARD;

		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		this.set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		int cardCount = CZBGConstants.MAX_CARD_COUNT;
		if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			cardCount = CZBGConstants.OX_CARD_COUNT;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] != true) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();

			gameStart.setCurBanker(this._cur_banker);
			gameStart.setRoomInfo(this.getRoomInfo());
			gameStart.setIsFifth(false);
			SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < cardCount; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < cardCount; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}
						this.cardGroup[i].reset(GRR._cards_data[i], cardCount);
					} else {
						for (int j = 0; j < cardCount; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}
				this.GRR._video_recode.addHandCards(k, cards);
				sendCard.addSendCard(k, cards);
				gameStart.setSendCard(sendCard);

			}
			this.load_room_info_data(roomResponse);
			gameStart.setDisplayTime(this._cur_operate_time);
			if (cardCount == CZBGConstants.MAX_CARD_COUNT) {
				this.cardGroup[i].cards = new int[CZBGConstants.MAX_CARD_COUNT];
				for (int k = 0; k < cardCount; k++) {
					this.cardGroup[i].cards[k] = this.GRR._cards_data[i][k];
				}
				this.canBaoDao[i] = this._logic.judgeEight(this.cardGroup[i], cardCount);
			}
			gameStart.setCanBaoDao(this.canBaoDao[i]);
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(gameStart));
			if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
				roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START);
			} else {
				roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_GUAI);
			}
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		if (has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START);
		} else {
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_GUAI);
		}
		GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();
		gameStart.setCurBanker(this._cur_banker);
		gameStart.setRoomInfo(this.getRoomInfo());
		gameStart.setIsFifth(false);
		// 发送数据
		SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
		for (int k = 0; k < this.getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] == true) {
				for (int j = 0; j < cardCount; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			} else {
				for (int j = 0; j < cardCount; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			}
			sendCard.addSendCard(k, cards);
		}
		gameStart.setSendCard(sendCard);
		roomResponse.setCommResponse(PBUtil.toByteString(gameStart));

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);

		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return true;
	}

	/**
	 * 八怪开始
	 * 
	 * @param callBankerCount
	 *            参与叫庄的玩家数据
	 * @param callBankerInfo
	 *            参与叫庄的玩家座位
	 * @return
	 */
	public boolean game_start() {
		this._game_status = CZBGConstants.GS_CZBG_OPEN_CARD_GUAI;

		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		int cardCount = CZBGConstants.MAX_CARD_COUNT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] != true) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();
			gameStart.setCurBanker(this._cur_banker);
			gameStart.setRoomInfo(this.getRoomInfo());
			gameStart.setIsFifth(false);

			SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < cardCount; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < cardCount; j++) {
							cards.addItem(GRR._cards_data[i][j]);
						}
					} else {
						int begin = 0;
						if (this.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
							begin = 5;
							for (int j = 0; j < begin; j++) {
								cards.addItem(GRR._cards_data[i][j]);
							}
						}
						for (int j = begin; j < cardCount; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}
				this.GRR._video_recode.addHandCards(k, cards);
				sendCard.addSendCard(k, cards);
				gameStart.setSendCard(sendCard);
			}
			this.load_room_info_data(roomResponse);
			gameStart.setDisplayTime(this._cur_operate_time);
			if (cardCount == CZBGConstants.MAX_CARD_COUNT) {
				this.cardGroup[i].cards = new int[CZBGConstants.MAX_CARD_COUNT];
				for (int k = 0; k < cardCount; k++) {
					this.cardGroup[i].cards[k] = this.GRR._cards_data[i][k];
				}
				this.canBaoDao[i] = this._logic.judgeEight(this.cardGroup[i], cardCount);
			}
			gameStart.setCanBaoDao(this.canBaoDao[i]);
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(gameStart));
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_GUAI);
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_GUAI);
		GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();
		gameStart.setCurBanker(this._cur_banker);
		gameStart.setRoomInfo(this.getRoomInfo());
		gameStart.setIsFifth(false);
		// 发送数据
		SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
		for (int k = 0; k < this.getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] == true) {
				for (int j = 0; j < cardCount; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			} else {
				int begin = 0;
				if (this.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
					begin = 5;
					for (int j = 0; j < begin; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				}
				for (int j = begin; j < cardCount; j++) {
					cards.addItem(GameConstants.BLACK_CARD);
				}
			}
			sendCard.addSendCard(k, cards);
		}
		gameStart.setSendCard(sendCard);
		roomResponse.setCommResponse(PBUtil.toByteString(gameStart));

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);

		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return true;
	}

	public boolean game_start_after_ox() {
		this._game_status = CZBGConstants.GS_CZBG_OPEN_CARD_GUAI;

		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		this.set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				this._player_status[i] = true;
			} else {
				this._player_status[i] = false;
			}
		}

		int cardCount = CZBGConstants.MAX_CARD_COUNT;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] != true) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();
			gameStart.setCurBanker(this._cur_banker);
			gameStart.setIsFifth(false);

			SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
			for (int k = 0; k < this.getTablePlayerNumber(); k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (this._player_status[k] != true) {
					for (int j = 0; j < cardCount; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < cardCount; j++) {
							cards.addItem(GRR._cards_data[i][j]);
						}
					} else {
						int begin = 0;
						if (this.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
							begin = 5;
							for (int j = 0; j < begin; j++) {
								cards.addItem(GRR._cards_data[i][j]);
							}
						}
						for (int j = begin; j < cardCount; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}
				this.GRR._video_recode.addHandCards(k, cards);
				sendCard.addSendCard(k, cards);
				gameStart.setSendCard(sendCard);

			}
			this.load_room_info_data(roomResponse);
			gameStart.setDisplayTime(this._cur_operate_time);
			if (cardCount == CZBGConstants.MAX_CARD_COUNT) {
				this.cardGroup[i].cards = new int[CZBGConstants.MAX_CARD_COUNT];
				for (int k = 0; k < cardCount; k++) {
					this.cardGroup[i].cards[k] = this.GRR._cards_data[i][k];
				}
				this.canBaoDao[i] = this._logic.judgeEight(this.cardGroup[i], cardCount);
			}
			gameStart.setCanBaoDao(this.canBaoDao[i]);
			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(gameStart));
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_AFTER_OX);
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_GAME_START_AFTER_OX);
		GameStart_CZBG.Builder gameStart = GameStart_CZBG.newBuilder();
		gameStart.setCurBanker(this._cur_banker);
		gameStart.setIsFifth(false);
		// 发送数据
		SendCard_CZBG.Builder sendCard = SendCard_CZBG.newBuilder();
		for (int k = 0; k < this.getTablePlayerNumber(); k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (this._player_status[k] == true) {
				for (int j = 0; j < cardCount; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			} else {
				int begin = 0;
				if (this.has_rule(CZBGConstants.CZBG_RULE_XIAN_NIU_HOU_GUAI)) {
					begin = 5;
					for (int j = 0; j < begin; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				}
				for (int j = begin; j < cardCount; j++) {
					cards.addItem(GameConstants.BLACK_CARD);
				}
			}
			sendCard.addSendCard(k, cards);
		}
		gameStart.setSendCard(sendCard);
		roomResponse.setCommResponse(PBUtil.toByteString(gameStart));

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		this.GRR.add_room_response(roomResponse);

		this._handler = _handler_open_card;
		_handler_open_card.reset_status(this._game_status);

		return true;
	}

	/**
	 * 斗牛开牌
	 * 
	 * @param seat_index
	 */
	public void open_card_ox(int seat_index) {
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.GRR._video_recode.addHandCards(cards);
		for (int j = 0; j < CZBGConstants.OX_CARD_COUNT; j++) {
			cards.addItem(GRR._cards_data[seat_index][j]);
		}
		CardType_CZBG.Builder card_type = CardType_CZBG.newBuilder();
		card_type.addCardType(this.cardGroup[seat_index].point);
		card_type.setTime(this.cardGroup[seat_index].getRate());
		OpenCard_CZBG.Builder open_card = OpenCard_CZBG.newBuilder();
		open_card.setSeatIndex(seat_index);
		open_card.setOpen(true);
		open_card.setCardType(card_type);
		open_card.setPoint(this.cardGroup[seat_index].point);
		for (int j = 0; j < CZBGConstants.OX_CARD_COUNT; j++) {
			open_card.addCards(this.cardGroup[seat_index].cards[j]);
		}
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_OPEN_CARD);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));
		roomResponse.setTarget(seat_index);
		RoomUtil.send_response_to_room(this, roomResponse);

		if (this.GRR != null) {
			this.GRR.add_room_response(roomResponse);
		}

		int count = 0;
		this._open_card[seat_index] = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._open_card[i]) {
				count++;
			}
		}

		int _cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((this.get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}
		if (count == _cur_count) { // 斗牛结算后进入八怪
			Arrays.fill(this._open_card, false);
			calScoreOX(this.getTablePlayerNumber());

			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_OX_RESULT);
			PukeGameEndCZBG.Builder end = PukeGameEndCZBG.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					end.addScore(-0xFFFF);
					end.addEndScore(-0xFFFF);
				} else {
					end.addScore(this.roundScore[i]);
					end.addEndScore(this.gameScore[i] + this.roundScore[i]);
					this.douNiuScore[i] = this.roundScore[i];
					this.roundScore[i] = 0;
				}

				CardType_CZBG.Builder cardType = CardType_CZBG.newBuilder();
				cardType.addCardType(this.cardGroup[i].point);
				cardType.setTime(this.cardGroup[i].getRate());
				OpenCard_CZBG.Builder openCard = OpenCard_CZBG.newBuilder();
				openCard.setSeatIndex(i);
				openCard.setOpen(true);
				openCard.setCardType(cardType);
				for (int j = 0; j < CZBGConstants.OX_CARD_COUNT; j++) {
					open_card.addCards(this.cardGroup[seat_index].cards[j]);
				}
				end.addOpenCard(openCard);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(end));
			RoomUtil.send_response_to_room(this, roomResponse);

			if (this.GRR != null) {
				this.GRR.add_room_response(roomResponse);
			}

			int delay = 2500;
			SysParamModel sysParamModel3008 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(3008);
			if (sysParamModel3008.getVal5() == 1) {
				delay = sysParamModel3008.getVal1();
			}
			GameSchedule.put(new CZBGStartRunnable(this.getRoom_id()), delay, TimeUnit.MILLISECONDS);
		}
	}

	public void open_card_eight(int seat_index) {
		if (GRR == null) {
			return;
		}
		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		CardType_CZBG.Builder card_type = CardType_CZBG.newBuilder();
		for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
			cards.addItem(GRR._cards_data[seat_index][j]);
		}
		for (int i = 0; i < 3; i++) { // 头道、中道、尾道
			card_type.addCardType(this.cardGroup[seat_index].cardTypes[i]);
		}
		card_type.setTime(this.cardGroup[seat_index].getRate());

		OpenCard_CZBG.Builder open_card = OpenCard_CZBG.newBuilder();
		for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
			open_card.addCards(GRR._cards_data[seat_index][j]);
		}
		open_card.setSeatIndex(seat_index);
		open_card.setOpen(true);
		open_card.setCardType(card_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_OPEN_CARD_GUAI);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card));

		RoomUtil.send_response_to_room(this, roomResponse);

		if (this.GRR != null) {
			this.GRR.add_room_response(roomResponse);
		}

		int count = 0;
		this._open_card[seat_index] = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._open_card[i]) {
				count++;
			}
		}

		int _cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				_cur_count += 1;
			}
		}
		if (count == _cur_count) { // 结算进入八怪
			calScoreEight(_cur_count);
			int delay = 1000;
			SysParamModel sysParamModel3008 = SysParamDict.getInstance()
					.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(3008);
			if (sysParamModel3008.getVal5() == 1) {
				delay = sysParamModel3008.getVal2();
			}
			GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), 0, GameConstants.Game_End_NORMAL), delay, TimeUnit.MILLISECONDS);
			// on_room_game_finish(seat_index, GameConstants.Game_End_NORMAL);
		}
	}

	/**
	 * 斗牛算分
	 */
	public void calScoreOX(int count) {
		if (this._cur_banker == -1) { // 通比
			for (int i = 0; i < count; i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				for (int j = i + 1; j < count; j++) {
					if (this.get_players()[j] == null) {
						continue;
					}
					int value = this.cardGroup[i].calScore(this.cardGroup[j]) * this._add_Jetton[i] * beishu;
					this.roundScore[i] += value;
					this.roundScore[j] -= value;
				}
			}
		} else {
			for (int i = 0; i < count; i++) {
				if (this.get_players()[i] == null || i == this._cur_banker) {
					continue;
				}
				int value = this.cardGroup[i].calScore(this.cardGroup[this._cur_banker]) * this._add_Jetton[i] * beishu;
				this.roundScore[i] += value;
				this.roundScore[this._cur_banker] -= value;
			}
		}
	}

	/**
	 * 八怪算分
	 */
	public void calScoreEight(int count) {
		if (this._cur_banker == -1) { // 通比
			for (int i = 0; i < count; i++) {
				for (int j = i + 1; j < count; j++) {
					int value = this.cardGroup[i].calScoreEight(this.cardGroup[j]) * this._add_Jetton[i] * beishu;
					this.roundScore[i] += value;
					this.roundScore[j] -= value;
				}
			}
		} else {
			for (int i = 0; i < count; i++) {
				if (i == this._cur_banker) {
					continue;
				}
				int value = this.cardGroup[i].calScoreEight(this.cardGroup[this._cur_banker]) * this._add_Jetton[i] * beishu;
				this.roundScore[i] += value;
				this.roundScore[this._cur_banker] -= value;
			}
		}

		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// this.roundScore[i] += this.cardGroup[i].getBaseCardTypeScore();
		// }
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber()) {
			return false;
		}
		return istrustee[seat_index];
	}

	/**
	 * 洗牌
	 * 
	 * @param repertory_card
	 * @param mj_cards
	 */
	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * CZBGConstants.MAX_CARD_COUNT + j];
			}
		}
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {

		int cards[][] = { { 0x11, 0x21, 0x21, 0x4E, 0x4F, 0x31, 0x14, 0x3a, }, { 0x4E, 0x15, 0x05, 0x0d, 0x2d, 0x07, 0x24, 0x26, },
				{ 0x31, 0x33, 0x04, 0x35, 0x39, 0x3b, 0x1d, 0x4E }, { 0x31, 0x33, 0x04, 0x35, 0x39, 0x3b, 0x1d, 0x4E },
				{ 0x31, 0x33, 0x04, 0x35, 0x39, 0x3b, 0x1d, 0x4E }, { 0x31, 0x33, 0x04, 0x35, 0x39, 0x3b, 0x1d, 0x4E }, };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[i][j];
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > CZBGConstants.OX_CARD_COUNT) {
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
		int count = realyCards.length / CZBGConstants.MAX_CARD_COUNT;
		if (count > this.getTablePlayerNumber()) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}

		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i++) {
				for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}
			if (i == count) {
				break;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < CZBGConstants.MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = CZBGConstants.GS_CZBG_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._wait_cancel_trustee[i] == true) {
				this._wait_cancel_trustee[i] = false;
				handler_request_trustee(i, false, 0);
			}
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return this.handler_game_finish_czbg(seat_index, reason);
	}

	public boolean handler_game_finish_czbg(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = CZBGConstants.CZBG_RULE_PLAYER_6;
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		if (this._game_scheduled != null) {
			this.kill_timer();
		}
		this.set_timer(GameConstants.HJK_READY_TIMER, GameConstants.HJK_READY_TIME_SECONDS, true);
		this.set_trustee_timer(GameConstants.HJK_READY_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_RESULT_GUAI);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndCZBG.Builder geme_end_CZBG = PukeGameEndCZBG.newBuilder();

		this.load_room_info_data(roomResponse);

		game_end.setRoomInfo(this.getRoomInfo());
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		game_end.setRoundOverType(1);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setGamePlayerNumber(getTablePlayerNumber());

		this.load_player_info_data(roomResponse);

		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
			GRR._end_type = reason;
		}

		game_end.setBankerPlayer(this._cur_banker); //
		for (int i = 0; i < count; i++) {
			if (this.get_players()[i] == null) {
				game_end.addGameScore(-0xFFFF);
				geme_end_CZBG.addScore(-0xFFFF);
				geme_end_CZBG.addEndScore(-0xFFFF);
				continue;
			}
			game_end.addGameScore(this.roundScore[i] + this.douNiuScore[i]); // 战绩界面

			geme_end_CZBG.addScore(this.roundScore[i]);
			this.gameScore[i] += this.roundScore[i] + this.douNiuScore[i];
			geme_end_CZBG.addEndScore(this.gameScore[i]);

			_player_result.game_score[i] = this.gameScore[i];
		}

		for (int i = 0; i < count; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			CardType_CZBG.Builder card_type = CardType_CZBG.newBuilder();
			for (int j = 0; j < this.cardGroup[i].cards.length; j++) {
				cards.addItem(this.cardGroup[i].cards[j]);
			}
			for (int j = 0; j < 3; j++) { // 头道、中道、尾道
				card_type.addCardType(this.cardGroup[i].cardTypes[j]);
			}
			card_type.setTime(this.cardGroup[i].getRate());

			OpenCard_CZBG.Builder open_card = OpenCard_CZBG.newBuilder();

			for (int j = 0; j < this.cardGroup[i].cards.length; j++) {
				if (this.cardGroup[i].cards[j] == 0 && this.GRR != null) {
					open_card.addCards(this.GRR._cards_data[i][j]);
				} else {
					open_card.addCards(this.cardGroup[i].cards[j]);
				}
			}
			open_card.setSeatIndex(i);
			open_card.setOpen(true);
			open_card.setCardType(card_type);
			geme_end_CZBG.addOpenCard(open_card);

			geme_end_CZBG.addFirstScore(this.cardGroup[i].comScore[0]);
			geme_end_CZBG.addSecondScore(this.cardGroup[i].comScore[1]);
			geme_end_CZBG.addThreeScore(this.cardGroup[i].comScore[2]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				real_reason = 3;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
				if (this._game_scheduled != null) {
					this.kill_timer();
				}
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN) {
				real_reason = reason;
			}
			real_reason = 5;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		geme_end_CZBG.setRoomInfo(this.getRoomInfo());
		geme_end_CZBG.setReason(real_reason);

		// 总得分
		game_end.setCommResponse(PBUtil.toByteString(geme_end_CZBG));
		roomResponse.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(geme_end_CZBG));

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
		RoomUtil.send_response_to_room(this, roomResponse);
		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < CZBGConstants.CZBG_RULE_PLAYER_6; j++) {
				Player player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end) { // 删除
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			observers().send(player, quit_roomResponse);
			observers().exit(player.getAccount_id());
			return true;
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
		if (is_cancel) {
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
			if (this.is_game_start == 1) {
				this.is_game_start = 0;
			}
			if (this._cur_round > 0) {
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS); // 刷新玩家
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
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}
		_player_count = 0;
		int _cur_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		if (this.istrustee[seat_index]) {
			this.istrustee[seat_index] = false;
			if (this._trustee_schedule[seat_index] != null) {
				this._trustee_schedule[seat_index].cancel(false);
				this._trustee_schedule[seat_index] = null;
			}
			this._trustee_type[seat_index] = 0;
		}
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			if (this._handler != null) {
				this._handler.handler_player_be_in_room(this, seat_index);
			}
		} else if (GameConstants.GS_MJ_WAIT == _game_status && this.get_players()[seat_index] != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

			this.load_room_info_data(roomResponse);
			this.load_player_info_data(roomResponse);
			roomResponse.setGameStatus(this._game_status);

			int display_time = this._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - this._operate_start_time);
			if (display_time > 0) {

			}
			this.send_response_to_player(seat_index, roomResponse);
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

	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		return true;
	}

	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		return true;
	}

	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		return true;
	}

	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		if (this._handler != null) {
			this._handler.handler_call_banker(this, seat_index, call_banker);
		}
		return true;
	}

	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		if (this._handler != null) {
			this._handler.handler_add_jetton(this, seat_index, jetton);
		}
		return true;
	}

	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		if (this._handler != null) {
			this._handler.handler_open_cards(this, seat_index, open_flag);
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
			for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
				player = this.get_players()[i];
				if (player == null) {
					continue;
				}
				send_error_notify(i, 2, "游戏等待超时解散");
			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
		}

		for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
			this.get_players()[i] = null;
		}

		if (_table_scheduled != null) {
			_table_scheduled.cancel(false);
		}

		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	@Override
	protected boolean canEnter(Player player) {
		return this._game_status == GameConstants.GAME_STATUS_FREE;
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
		if (DEBUG_CARDS_MODE) {
			delay = 10;
		}
		int playerNumber = getTablePlayerNumber();
		if (playerNumber == 0) {
			playerNumber = CZBGConstants.CZBG_RULE_PLAYER_6;
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}
			if (this._game_scheduled != null) {
				this.kill_timer();
			}
			if (_release_scheduled != null) {
				_release_scheduled.cancel(false);
			}
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意
			int count = 0;
			for (int i = 0; i < playerNumber; i++) {
				if (_gameRoomRecord.release_players[i] == 1) {
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
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1) {
				return false;
			}
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
				if (get_players()[i] == null) {
					continue;
				}
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < playerNumber; j++) {
				player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}

			if (_release_scheduled != null) {
				_release_scheduled.cancel(false);
			}
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
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.set_timer(this._cur_game_timer, this._cur_operate_time, false);
			this.set_trustee_timer(this._cur_game_timer, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);
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
			if (_release_scheduled != null) {
				_release_scheduled.cancel(false);
			}
			_release_scheduled = null;

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < playerNumber; j++) {
				player = this.get_players()[j];
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");
			}
			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			// if (null != this.getCreate_player() && (player != null &&
			// this.getCreate_player().getAccount_id() !=
			// player.getAccount_id())) {
			// send_error_notify(seat_index, 2, "只有创建者才能解散房间");
			// return false;
			// }

			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < playerNumber; i++) {
					Player p = this.get_players()[i];
					if (p == null) {
						continue;
					}
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;
			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: { // 玩家未开始游戏 退出
			if (this._player_status[seat_index] == true) {
				if (GameConstants.GS_MJ_FREE != _game_status) { // 游戏已经开始
					return false;
				}
				send_error_notify(seat_index, 2, "您已经开始游戏了,不能退出游戏");
				return false;
			}

			if (get_players()[seat_index] != null) {
				if (get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id())
					this._own_room_seat = -1;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT); // 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());

			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);

			send_response_to_other(seat_index, refreshroomResponse);
			int _cur_count = 0;
			int player_count = 0;
			for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
					continue;
				} else {
					_cur_count += 1;
				}
				if (this._player_status[i] == true) {

				}
				if (_player_ready[i] == 1) {
					player_count += 1;
				}
			}

			if ((player_count >= 2) && (player_count == _cur_count)) {
				handler_game_start();
			}

			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < CZBGConstants.CZBG_RULE_PLAYER_6; i++) {
				Player p = this.get_players()[i];
				if (p == null) {
					continue;
				}
				send_error_notify(i, 2, "游戏已被创建者解散");
			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);

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
	 * 在玩家的前面显示出的牌 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
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
			roomResponse.setStandTime(1500);
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

	public boolean handler_player_offline(Player player) {
		if (observers().exist(player.getAccount_id())) {
			this.handler_exit_room_observer(player);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setGameStatus(_game_status);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS); // 刷新玩家
			this.load_player_info_data(roomResponse);

			send_response_to_other(player.get_seat_index(), roomResponse);
		}

		return true;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = CZBGConstants.CZBG_RULE_PLAYER_6;
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int wiCZBGer = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
				if (_player_result.win_order[j] != -1) {
					continue;
				}
				if (_player_result.game_score[j] > s) {
					s = _player_result.game_score[j];
					wiCZBGer = j;
				}
			}
			if (s >= max_score) {
				max_score = s;
			} else {
				win_idx++;
			}
			if (wiCZBGer != -1) {
				_player_result.win_order[wiCZBGer] = win_idx;
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			player_result.addPlayersId(i);
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
		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				player_result.addGameRuleIndexEx(ruleEx[i]);
			}

		}
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
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null) {
				continue;
			}
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
			// room_player.setScore(this.gameScore[i]);
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
		this._handler_finish.exe(this);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			return false;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);

		if ((!(_game_status == GameConstants.GS_MJ_WAIT || _game_status == GameConstants.GS_MJ_FREE)) && isTrustee == false) {
			send_error_notify(get_seat_index, 2, "托管将在本轮结束后取消!");
			_wait_cancel_trustee[get_seat_index] = true;
			return false;
		}

		istrustee[get_seat_index] = isTrustee;
		_trustee_type[get_seat_index] = Trustee_type;
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}
		if (istrustee[get_seat_index] == true) {
			this.set_trustee_timer(this._cur_game_timer, GameConstants.OX_TRUESTEE_OPERATE_TIME, false);
		} else {
			if (_trustee_schedule[get_seat_index] != null) {
				_trustee_schedule[get_seat_index].cancel(false);
				_trustee_schedule[get_seat_index] = null;
			}
		}
		this.send_response_to_room(roomResponse);

		return false;
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(CZBGConstants.CZBG_RULE_PLAYER_2)) {
			return 2;
		} else if (has_rule(CZBGConstants.CZBG_RULE_PLAYER_3)) {
			return 3;
		} else if (has_rule(CZBGConstants.CZBG_RULE_PLAYER_4)) {
			return 4;
		} else if (has_rule(CZBGConstants.CZBG_RULE_PLAYER_5)) {
			return 5;
		}
		return CZBGConstants.CZBG_RULE_PLAYER_6;
	}

	@Override
	public boolean exe_finish(int reason) {
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {

	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS); // 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public boolean set_trustee_timer(int timer_type, int time, boolean makeDBtimer) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[CZBGConstants.CZBG_RULE_PLAYER_6];
		}
		if (timer_type == GameConstants.HJK_READY_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			time = 2;
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			time = 2;
		}
		if (makeDBtimer == false) {
			time = 0;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (istrustee[i] == true) {
				if (_trustee_schedule[i] != null) {
					_trustee_schedule[i].cancel(false);
					_trustee_schedule[i] = null;
					_trustee_schedule[i] = GameSchedule.put(new TrusteeRunnable(getRoom_id(), timer_type, i), time, TimeUnit.SECONDS);
					_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				} else {
					_trustee_schedule[i] = GameSchedule.put(new TrusteeRunnable(getRoom_id(), timer_type, i), time, TimeUnit.SECONDS);
					_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				}
			} else {
				if (_trustee_schedule[i] != null) {
					_trustee_schedule[i].cancel(false);
					_trustee_schedule[i] = null;
				}
			}
		}
		return true;
	}

	public boolean set_timer(int timer_type, int time, boolean makeDBtimer) {
		return true;
	}

	public boolean kill_timer() {
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
			if (this._player_status[i] == false) {
				continue;
			}
			if (this._open_card[i] == false) {
				this._handler.handler_open_cards(this, i, true);
			}
		}
		return false;
	}

	public boolean robot_banker_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false) {
				continue;
			}
			if (this._call_banker[i] == -1) {
				this._handler.handler_call_banker(this, i, 0);
			}
		}
		return false;
	}

	public boolean ready_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				if (this._player_ready[i] == 0) {
					handler_player_ready(i, false);
				}
			}
		}

		return false;
	}

	public boolean add_jetton_timer() {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_status[i] == false || i == _cur_banker) {
				continue;
			}
			if (this._add_Jetton[i] == 0) {
				this._handler.handler_add_jetton(this, i, 0);
			}
		}
		return false;
	}

	public boolean trustee_timer(int operate_id, int seat_index) {
		if (operate_id == GameConstants.HJK_READY_TIMER) {
			handler_player_ready(seat_index, false);
		}

		return true;
	}

	/**
	 * @param seat_index
	 * @param call_banker
	 * @param qiang_banker
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_CZBG_OPEN_CARD) {
			OpenCardRequest_CZBG req = PBUtil.toObject(room_rq, OpenCardRequest_CZBG.class);
			if (req.getType() == CZBGConstants.RESPONSE_CZBG_CALL_BANKER) {
				this.handler_call_banker(seat_index, req.getCallBanker());
			} else if (req.getType() == CZBGConstants.RESPONSE_CZBG_ADD_JETTON) {
				this.handler_add_jetton(seat_index, req.getAddJetton());
			} else if (req.getType() == CZBGConstants.RESPONSE_CZBG_BAO_DAO) {
				return handler_open_cards(seat_index, false);
			} else {
				this.cardGroup[seat_index].clean();
				this.cardGroup[seat_index].initEightCard(req.getCardsDataList(), req.getCardCount());
				return handler_open_cards(seat_index, true);
			}
		} else if (type == CZBGConstants.RESPONSE_CZBG_OPEN_CARD) {
			return handler_open_cards(seat_index, true);
		} else if (type == CZBGConstants.RESPONSE_CZBG_PROMPT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(CZBGConstants.RESPONSE_CZBG_PROMPT);
			Prompt_CZBG.Builder prompt = Prompt_CZBG.newBuilder();
			prompt.setPoint(this.cardGroup[seat_index].point);
			for (int i = 0; i < CZBGConstants.OX_CARD_COUNT; i++) {
				prompt.addCardDatas(this.cardGroup[seat_index].cards[i]);
			}
			prompt.setSeatIndex(seat_index);
			roomResponse.setCommResponse(PBUtil.toByteString(prompt));

			RoomUtil.send_response_to_player(this, seat_index, roomResponse);
		}
		return true;
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int _seat_index, int _type, boolean _tail) {
		return false;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {
		return false;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {
		return false;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return false;
	}

	@Override
	public void runnable_remove_out_cards(int _seat_index, int _type) {
	}

	@Override
	public void runnable_add_discard(int _seat_index, int _card_count, int[] _card_data, boolean _send_client) {
	}

	public void add_jetton_ox(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		AddJetton_CZBG.Builder add_jetton = AddJetton_CZBG.newBuilder();

		add_jetton.setCurPlayer(seat_index);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			add_jetton.addAddJettionInfo(this._add_Jetton[i]);
		}

		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_ADD_JETTON_PLAY);
		roomResponse.setCommResponse(PBUtil.toByteString(add_jetton));

		RoomUtil.send_response_to_room(this, roomResponse);

		this.GRR.add_room_response(roomResponse);
	}

	public void add_call_banker(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		CallBankerInfo_CZBG.Builder callBanker = CallBankerInfo_CZBG.newBuilder();

		callBanker.setCurPlayer(seat_index);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			callBanker.addCallBankerInfo(this._call_banker[i]);
		}

		roomResponse.setType(CZBGConstants.RESPONSE_CZBG_CALL_BANKER_PLAY);
		roomResponse.setCommResponse(PBUtil.toByteString(callBanker));

		RoomUtil.send_response_to_room(this, roomResponse);

		this.GRR.add_room_response(roomResponse);
	}
}
