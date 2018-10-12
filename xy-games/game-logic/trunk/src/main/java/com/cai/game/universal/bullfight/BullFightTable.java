package com.cai.game.universal.bullfight;

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
import com.cai.common.define.ECardType;
import com.cai.common.domain.AddGoldResultModel;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.BullFightUtil;
import com.cai.common.util.FvMask;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AddJettonRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.OpenCardRunnable;
import com.cai.future.runnable.ReadyRunnable;
import com.cai.future.runnable.RobotBankerRunnable;
import com.cai.future.runnable.TrusteeRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.universal.bullfight.handler.BullFightHandler;
import com.cai.game.universal.bullfight.handler.BullFightHandlerAddJetton;
import com.cai.game.universal.bullfight.handler.BullFightHandlerCallBanker;
import com.cai.game.universal.bullfight.handler.BullFightHandlerDispatchCard;
import com.cai.game.universal.bullfight.handler.BullFightHandlerFinish;
import com.cai.game.universal.bullfight.handler.BullFightHandlerOpenCard;
import com.cai.game.universal.bullfight.handler.ansanzhang.BullFightHandlerAddJetton_AnSanZhang;
import com.cai.game.universal.bullfight.handler.ansanzhang.BullFightHandlerCallBanker_AnSanZhang;
import com.cai.game.universal.bullfight.handler.ansanzhang.BullFightHandlerOpenCard_AnSanZhang;
import com.cai.game.universal.bullfight.handler.fangzhu.BullFightHandlerAddJetton_FangZhu;
import com.cai.game.universal.bullfight.handler.fangzhu.BullFightHandlerOpenCard_FangZhu;
import com.cai.game.universal.bullfight.handler.kansizhang.BullFightHandlerAddJetton_KanSiZhang;
import com.cai.game.universal.bullfight.handler.kansizhang.BullFightHandlerCallBanker_KanSiZhang;
import com.cai.game.universal.bullfight.handler.kansizhang.BullFightHandlerOpenCard_KanSiZhang;
import com.cai.game.universal.bullfight.handler.lunliu.BullFightHandlerAddJetton_LunLiu;
import com.cai.game.universal.bullfight.handler.lunliu.BullFightHandlerOpenCard_LunLiu;
import com.cai.game.universal.bullfight.handler.mingsanzhang.BullFightHandlerAddJetton_MingSanZhang;
import com.cai.game.universal.bullfight.handler.mingsanzhang.BullFightHandlerCallBanker_MingSanZhang;
import com.cai.game.universal.bullfight.handler.mingsanzhang.BullFightHandlerOpenCard_MingSanZhang;
import com.cai.game.universal.bullfight.handler.niuniu.BullFightHandlerAddJetton_NiuNiu;
import com.cai.game.universal.bullfight.handler.niuniu.BullFightHandlerOpenCard_NiuNiu;
import com.cai.game.universal.bullfight.handler.tongbi.BullFightHandlerOpenCard_TongBi;
import com.cai.game.universal.bullfight.handler.ziyou.BullFightHandlerAddJetton_ZiYou;
import com.cai.game.universal.bullfight.handler.ziyou.BullFightHandlerCallBanker_ZiYou;
import com.cai.game.universal.bullfight.handler.ziyou.BullFightHandlerOpenCard_ZiYou;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.AddJetton;
import protobuf.clazz.Protocol.CallBanker;
import protobuf.clazz.Protocol.CallBankerInfo;
import protobuf.clazz.Protocol.CardType;
import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStart;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.OpenCard;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.TableResponseOX;
import protobuf.clazz.Protocol.Timer_OX;
import protobuf.clazz.Protocol.WeaveItemResponse;

public class BullFightTable extends AbstractRoom {

	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(BullFightTable.class);

	public static boolean DEBUG_CARDS_MODE = false;

	public ScheduledFuture _trustee_schedule[];

	/**
	 * 用户是否叫庄
	 */
	public int _call_banker[];
	/**
	 * 用户下注
	 */
	public int _add_Jetton[];
	/**
	 * 用户摊牌
	 */
	public boolean _open_card[];
	/**
	 * 叫庄用户
	 */
	public int _cur_call_banker;
	/**
	 * 下一轮的庄家
	 */
	public int _next_banker;
	/**
	 * 用户状态
	 */
	public boolean _player_status[];
	/**
	 * 用户数量
	 */
	public int _player_count;
	/**
	 * 通比牛牛赢家
	 */
	public int _win_player_oxtb;
	/**
	 * 牛牛赢家
	 */
	public boolean _win_player_ox[];
	/**
	 * 牛牛平
	 */
	public boolean _ping_Player_ox[];
	/**
	 * 下注数据
	 */
	public int _jetton_info_sever_ox[];
	/**
	 * 筹码个数
	 */
	public int _jetton_count;
	/**
	 * 可以真实下注筹码个数
	 */
	public int _cur_jetton_count[];
	/**
	 * 当前下注数据
	 */
	public int _jetton_info_cur[][];
	/**
	 * 叫庄信息
	 */
	public int _call_banker_info[];
	/**
	 * 庄家倍数
	 */
	public int _banker_times;
	/**
	 * 庄家最大倍数
	 */
	public int _banker_max_times;
	/**
	 * 可以推注用户
	 */
	public int _can_tuizhu_player[];

	public BullFightGameLogic _logic = null;

	/**
	 * 操作开始时间
	 */
	public int _operate_start_time;
	/**
	 * 可操作时间
	 */
	public int _cur_operate_time;
	/**
	 * 当前游戏定时器
	 */
	public int _cur_game_timer;
	/**
	 * 牌类型
	 */
	public int _card_type_ox[];
	/**
	 * 游戏底分
	 */
	public int game_cell;
	/**
	 * 托管的内容
	 */
	public int _trustee_type[];
	/**
	 * 取消托管
	 */
	public boolean _wait_cancel_trustee[];
	/**
	 * 游戏是否开始
	 */
	public int is_game_start;
	/**
	 * 房主的位置
	 */
	public int _own_room_seat;
	/**
	 * 推注倍数
	 */
	public int _tui_zhu_times;

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;
	private ScheduledFuture _game_scheduled;

	public BullFightHandler _handler;

	public BullFightHandlerDispatchCard _handler_dispath_card;
	public BullFightHandlerCallBanker _handler_Call_banker;
	public BullFightHandlerAddJetton _handler_add_jetton;
	public BullFightHandlerOpenCard _handler_open_card;

	public BullFightHandlerFinish _handler_finish;

	public BullFightTable() {
		super(RoomType.OX, GameConstants.GAME_PLAYER_SIX);

		_logic = new BullFightGameLogic();

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			get_players()[i] = null;
		}

		_game_status = GameConstants.GS_OX_FREE;

		_player_ready = new int[GameConstants.GAME_PLAYER_OX];
		_player_open_less = new int[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}

		_operate_start_time = 0;
		_cur_operate_time = 0;
		_cur_game_timer = 0;

		_call_banker = new int[GameConstants.GAME_PLAYER_OX];
		_add_Jetton = new int[GameConstants.GAME_PLAYER_OX];
		_open_card = new boolean[GameConstants.GAME_PLAYER_OX];

		_cur_call_banker = 0;

		_player_status = new boolean[GameConstants.GAME_PLAYER_OX];
		_ping_Player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_win_player_ox = new boolean[GameConstants.GAME_PLAYER_OX];

		_call_banker_info = new int[5];

		_can_tuizhu_player = new int[GameConstants.GAME_PLAYER_OX];
		_card_type_ox = new int[GameConstants.GAME_PLAYER_OX];
		_trustee_type = new int[GameConstants.GAME_PLAYER_OX];

		_wait_cancel_trustee = new boolean[GameConstants.GAME_PLAYER_OX];

		is_game_start = 0;

		_tui_zhu_times = 1;
		_own_room_seat = -1;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;

			_player_status[i] = false;

			_can_tuizhu_player[i] = 0;
			_card_type_ox[i] = -1;

			_trustee_type[i] = 0;

			_wait_cancel_trustee[i] = false;
		}

		_banker_max_times = 1;
		game_cell = 1;

		_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER_OX];
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;

		_game_round = game_round;
		_cur_round = 0;

		_operate_start_time = 0;
		_cur_operate_time = 0;
		_cur_game_timer = 0;

		_own_room_seat = -1;

		_jetton_info_sever_ox = new int[5];

		Arrays.fill(_jetton_info_sever_ox, 0);

		if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
			_jetton_info_sever_ox[0] = 1;
			_jetton_info_sever_ox[1] = 2;
			_jetton_count = 2;
		}

		if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
			_jetton_info_sever_ox[0] = 2;
			_jetton_info_sever_ox[1] = 4;
			_jetton_count = 2;
		}

		if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
			_jetton_info_sever_ox[0] = 4;
			_jetton_info_sever_ox[1] = 8;
			_jetton_count = 2;
		}

		if (has_rule(GameConstants.GAME_RULE_THREE_SIX)) {
			_jetton_info_sever_ox[0] = 3;
			_jetton_info_sever_ox[1] = 6;
			_jetton_count = 2;
		}

		if (has_rule(GameConstants.GAME_RULE_ONE_TO_FIVE)) {
			_jetton_info_sever_ox[0] = 1;
			_jetton_info_sever_ox[1] = 2;
			_jetton_info_sever_ox[2] = 3;
			_jetton_info_sever_ox[3] = 4;
			_jetton_info_sever_ox[4] = 5;
			_jetton_count = 5;
		}

		if (getRuleValue(GameConstants.GAME_RULE_FIVE_TO_TEN) == 1) {
			_jetton_info_sever_ox[0] = 5;
			_jetton_info_sever_ox[1] = 10;
			_jetton_count = 2;
		}

		if (has_rule(GameConstants.GAME_RULE_FIVE_TUI_ZHU))
			_tui_zhu_times = 5;
		else if (has_rule(GameConstants.GAME_RULE_PlAYER_TUI_ZHU))
			_tui_zhu_times = 10;
		else if (has_rule(GameConstants.GAME_RULE_FIFTEEN_TUI_ZHU))
			_tui_zhu_times = 15;
		else
			_tui_zhu_times = 0;

		_cur_jetton_count = new int[GameConstants.GAME_PLAYER_OX];
		Arrays.fill(_cur_jetton_count, 0);

		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_OX][11];

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < 10; j++) {
				_jetton_info_cur[i][j] = j + 1;
			}
		}

		for (int i = 0; i < 5; i++) {
			_call_banker_info[i] = i;
		}

		_win_player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_ping_Player_ox = new boolean[GameConstants.GAME_PLAYER_OX];
		_card_type_ox = new int[GameConstants.GAME_PLAYER_OX];
		istrustee = new boolean[GameConstants.GAME_PLAYER_OX];
		_trustee_type = new int[GameConstants.GAME_PLAYER_OX];
		_wait_cancel_trustee = new boolean[GameConstants.GAME_PLAYER_OX];

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_win_player_ox[i] = false;
			_ping_Player_ox[i] = false;
			_card_type_ox[i] = -1;
			_trustee_type[i] = 0;
			istrustee[i] = false;
			_wait_cancel_trustee[i] = false;
		}

		game_cell = 1;
		_banker_times = 1;
		_banker_max_times = 1;
		is_game_start = 0;

		_playerStatus = new PlayerStatus[GameConstants.GAME_PLAYER_OX];

		_player_result = new PlayerResult(getRoom_owner_account_id(), getRoom_id(), _game_type_index, _game_rule_index, _game_round, get_game_des(),
				GameConstants.GAME_PLAYER_OX);

		if (BullFightUtil.isTypeFangZhu(game_type_index)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_FangZhu();
			_handler_open_card = new BullFightHandlerOpenCard_FangZhu();
		}

		if (BullFightUtil.isTypeNiuNiu(game_type_index) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_ZBOX)
				|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_MBOX)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_NiuNiu();
			_handler_open_card = new BullFightHandlerOpenCard_NiuNiu();
		}

		if (BullFightUtil.isTypeLunLiu(game_type_index) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_LBOX)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_LunLiu();
			_handler_open_card = new BullFightHandlerOpenCard_LunLiu();
		}

		if (BullFightUtil.isTypeZiYou(game_type_index)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_ZiYou();
			_handler_open_card = new BullFightHandlerOpenCard_ZiYou();
			_handler_Call_banker = new BullFightHandlerCallBanker_ZiYou();
		}

		if (BullFightUtil.isTypeMingSanZhang(game_type_index) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_MSOX)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_MingSanZhang();
			_handler_open_card = new BullFightHandlerOpenCard_MingSanZhang();
			_handler_Call_banker = new BullFightHandlerCallBanker_MingSanZhang();
		}

		if (BullFightUtil.isTypeKanSiZhang(game_type_index) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_KFOX)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_KanSiZhang();
			_handler_open_card = new BullFightHandlerOpenCard_KanSiZhang();
			_handler_Call_banker = new BullFightHandlerCallBanker_KanSiZhang();
		}

		if (BullFightUtil.isTypeTongBi(game_type_index)) {
			_handler_open_card = new BullFightHandlerOpenCard_TongBi();
		}

		if (BullFightUtil.isTypeAnSanZhang(game_type_index)) {
			_handler_add_jetton = new BullFightHandlerAddJetton_AnSanZhang();
			_handler_open_card = new BullFightHandlerOpenCard_AnSanZhang();
			_handler_Call_banker = new BullFightHandlerCallBanker_AnSanZhang();
		}

		_handler_finish = new BullFightHandlerFinish();
		_call_banker = new int[GameConstants.GAME_PLAYER_OX];
		_add_Jetton = new int[GameConstants.GAME_PLAYER_OX];
		_open_card = new boolean[GameConstants.GAME_PLAYER_OX];
		_player_status = new boolean[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
		}

		_cur_jetton_count = new int[GameConstants.GAME_PLAYER_OX];
		Arrays.fill(_cur_jetton_count, 0);

		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_OX][11];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < 10; j++) {
				_jetton_info_cur[i][j] = j + 1;
			}
		}

		_cur_call_banker = 0;
		_cur_banker = 0;
		_banker_times = 1;

		setMinPlayerCount(2);
	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	public boolean has_trustee(int cbRule, int seat_index) {
		return FvMask.has_any(_trustee_type[seat_index], FvMask.mask(cbRule));
	}

	@Override
	public boolean reset_init_data() {
		record_game_room();

		GRR = new GameRoundRecord(GameConstants.GAME_PLAYER_OX, GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, GameConstants.MAX_HH_INDEX);

		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[GameConstants.GAME_PLAYER_OX];

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.OX_MAX_CARD_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_playerStatus[i].reset();
		}

		_call_banker = new int[GameConstants.GAME_PLAYER_OX];
		_add_Jetton = new int[GameConstants.GAME_PLAYER_OX];
		_open_card = new boolean[GameConstants.GAME_PLAYER_OX];
		_player_status = new boolean[GameConstants.GAME_PLAYER_OX];
		_cur_jetton_count = new int[GameConstants.GAME_PLAYER_OX];
		Arrays.fill(_cur_jetton_count, 0);

		_jetton_info_cur = new int[GameConstants.GAME_PLAYER_OX][11];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < 10; j++) {
				_jetton_info_cur[i][j] = j + 1;
			}
		}

		_card_type_ox = new int[GameConstants.GAME_PLAYER_OX];
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_call_banker[i] = -1;
			_add_Jetton[i] = 0;
			_open_card[i] = false;
			_player_status[i] = false;
			_win_player_ox[i] = false;
			_ping_Player_ox[i] = false;
			_card_type_ox[i] = -1;
		}

		_win_player_oxtb = -1;
		_operate_start_time = 0;
		_cur_operate_time = 0;
		_cur_game_timer = 0;

		GRR._room_info.setRoomId(getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setNewRules(commonGameRuleProtos);

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
		GRR._room_info.setCreatePlayerId(getRoom_owner_account_id());

		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			rplayer = get_players()[i];
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

		GRR._video_recode.setBankerPlayer(_cur_banker);

		_banker_max_times = 1;

		game_cell = 1;

		return true;
	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_OX_FREE;

		reset_init_data();

		GRR._banker_player = _cur_banker;
		if (has_rule(GameConstants.GAME_RULE_MAX_ONE_TIMES))
			_banker_max_times = 1;
		if (has_rule(GameConstants.GAME_RULE_MAX_TWO_TIMES))
			_banker_max_times = 2;
		if (has_rule(GameConstants.GAME_RULE_MAX_THREE_TIMES))
			_banker_max_times = 3;
		if (has_rule(GameConstants.GAME_RULE_MAX_FOUR_TIMES))
			_banker_max_times = 4;

		_repertory_card = new int[GameConstants.CARD_COUNT_OX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_OX);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (BullFightUtil.isTypeFangZhu(_game_type_index)) {
			return game_start_fang_zhu();
		}

		if (BullFightUtil.isTypeNiuNiu(_game_type_index)) {
			return game_start_niu_niu();
		}

		if (BullFightUtil.isTypeLunLiu(_game_type_index)) {
			return game_start_lun_liu();
		}

		if (BullFightUtil.isTypeZiYou(_game_type_index)) {
			return call_banker_zi_you();
		}

		if (BullFightUtil.isTypeMingSanZhang(_game_type_index)) {
			return call_banker_ming_san_zhang();
		}

		if (BullFightUtil.isTypeKanSiZhang(_game_type_index)) {
			return call_banker_kan_si_zhang();
		}

		if (BullFightUtil.isTypeTongBi(_game_type_index)) {
			return game_start_tong_bi();
		}

		if (BullFightUtil.isTypeAnSanZhang(_game_type_index)) {
			return call_banker_an_san_zhang();
		}

		return false;
	}

	public boolean call_banker_kan_si_zhang() {
		_game_status = GameConstants.GS_OX_CALL_BANKER;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < 4; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}
					} else {
						for (int j = 0; j < 4; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();

			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < 4; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}

		roomResponse_ox.setCallBankerInfo(call_banker_info);

		SendCard.Builder send_card = SendCard.newBuilder();

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (_player_status[k] != true) {
				for (int j = 0; j < 4; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < 4; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			}

			GRR._video_recode.addHandCards(k, cards);
			send_card.addSendCard(k, cards);
		}

		roomResponse_ox.setSendCard(send_card);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, _game_status);

		return true;
	}

	public boolean call_banker_an_san_zhang() {
		_game_status = GameConstants.GS_OX_CALL_BANKER;
		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < 3; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < 3; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}
					} else {
						for (int j = 0; j < 3; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < 3; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < 3; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}

		roomResponse_ox.setCallBankerInfo(call_banker_info);

		SendCard.Builder send_card = SendCard.newBuilder();

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (_player_status[k] != true) {
				for (int j = 0; j < 3; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < 3; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			}

			GRR._video_recode.addHandCards(k, cards);
			send_card.addSendCard(k, cards);
		}

		roomResponse_ox.setSendCard(send_card);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, _game_status);

		return true;
	}

	public boolean call_banker_ming_san_zhang() {
		_game_status = GameConstants.GS_OX_CALL_BANKER;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		{
			// 发送数据 && 围观
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < 3; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < 3; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

			RoomUtil.send_response_to_room(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}

		roomResponse_ox.setCallBankerInfo(call_banker_info);

		SendCard.Builder send_card = SendCard.newBuilder();

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (_player_status[k] != true) {
				for (int j = 0; j < 3; j++) {
					cards.addItem(GameConstants.INVALID_CARD);
				}
			} else {
				for (int j = 0; j < 3; j++) {
					cards.addItem(GRR._cards_data[k][j]);
				}
			}

			GRR._video_recode.addHandCards(k, cards);
			send_card.addSendCard(k, cards);
		}

		roomResponse_ox.setSendCard(send_card);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, _game_status);

		return true;
	}

	public boolean call_banker_zi_you() {
		_game_status = GameConstants.GS_OX_CALL_BANKER;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.HJK_ROBOT_BANKER_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ROBOT_BANKER_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		{
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

			for (int j = 0; j <= _banker_max_times; j++) {
				call_banker_info.addCallBankerInfo(_call_banker_info[j]);
			}

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse_ox.setCallBankerInfo(call_banker_info);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

			RoomUtil.send_response_to_room(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		CallBankerInfo.Builder call_banker_info = CallBankerInfo.newBuilder();

		for (int j = 0; j <= _banker_max_times; j++) {
			call_banker_info.addCallBankerInfo(_call_banker_info[j]);
		}

		roomResponse_ox.setCallBankerInfo(call_banker_info);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_CALL_BANKER);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_Call_banker;
		_handler_Call_banker.reset_status(0, _game_status);

		return true;
	}

	public void game_start_ZYQOX() {
		_game_status = GameConstants.GS_OX_ADD_JETTON;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int j = _call_banker_info.length - 1; j >= 0; j--) {
			int chairID[] = new int[GameConstants.GAME_PLAYER_OX];
			int chair_count = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (_player_status[i] == false)
					continue;
				if ((_player_status[i] == true) && (_call_banker_info[j] == _call_banker[i])) {
					chairID[chair_count++] = i;
				}
			}

			if (chair_count > 0) {
				int rand = (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
				int temp = rand % chair_count;
				_cur_banker = chairID[temp];
				_banker_times = _call_banker[_cur_banker];
				if (_banker_times == 0)
					_banker_times = 1;
				break;
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] == null)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();

			game_start.setCurBanker(_cur_banker);

			if ((i != _cur_banker && _player_status[i] == true) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if ((i != _cur_banker) && (_player_status[k] == true)) {
						if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
							for (int j = 0; j < 10; j++) {
								cards.addItem(j + 1);
								_jetton_info_cur[k][j] = j + 1;
							}
							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][11] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][11]);
							}
						} else {
							for (int j = 0; j < _jetton_count; j++) {
								cards.addItem(_jetton_info_sever_ox[j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
							}
							_cur_jetton_count[k] = _jetton_count;
							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][_jetton_count]);
								_cur_jetton_count[k]++;
							}
						}
					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (((i != _cur_banker) && (_player_status[i] == true))
						|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
					if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
						for (int j = 0; j < 10; j++) {
							cards.addItem(j + 1);
							_jetton_info_cur[i][j] = j + 1;
						}
						if (_can_tuizhu_player[i] > 0) {
							_jetton_info_cur[i][11] = _can_tuizhu_player[i];
							cards.addItem(_jetton_info_cur[i][11]);
						}
					} else {
						for (int j = 0; j < _jetton_count; j++) {
							cards.addItem(_jetton_info_sever_ox[j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[j];
						}

						if (_can_tuizhu_player[i] > 0) {
							_jetton_info_cur[i][_jetton_count] = _can_tuizhu_player[i];
							cards.addItem(_jetton_info_cur[i][_jetton_count]);
						}
					}

				}

				game_start.addJettonCell(i, cards);
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		load_room_info_data(roomResponse);

		GameStart.Builder game_start = GameStart.newBuilder();

		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (((k != _cur_banker) && (_player_status[k] == true)) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
					for (int j = 0; j < 10; j++) {
						cards.addItem(j + 1);
						_jetton_info_cur[k][j] = j + 1;
					}
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][11] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][11]);
					}
				} else {
					for (int j = 0; j < _jetton_count; j++) {
						cards.addItem(_jetton_info_sever_ox[j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
					}
					_cur_jetton_count[k] = _jetton_count;
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][_jetton_count]);
						_cur_jetton_count[k]++;
					}
				}

			}

			game_start.addJettonCell(k, cards);
		}

		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, _game_status);

		gu_ding_add_jetton();
	}

	public void add_call_banker(int seat_index) {
		{
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			CallBanker.Builder call_banker = CallBanker.newBuilder();

			call_banker.setSeatIndex(seat_index);
			call_banker.setCallBanker(_call_banker[seat_index]);

			roomResponse_ox.setCallBanker(call_banker);
			roomResponse.setType(MsgConstants.RESPONSE_SELECT_BANKER);
			roomResponse.setRoomResponseOx(roomResponse_ox);

			RoomUtil.send_response_to_room(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		CallBanker.Builder call_banker = CallBanker.newBuilder();

		call_banker.setSeatIndex(seat_index);
		call_banker.setCallBanker(_call_banker[seat_index]);

		roomResponse_ox.setCallBanker(call_banker);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_SELECT_BANKER);

		GRR.add_room_response(roomResponse);
	}

	public boolean game_start_lun_liu() {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		if (_cur_round == 1) {
			if (_own_room_seat != -1) {
				_cur_banker = _own_room_seat;
			} else {
				int chairID[] = new int[GameConstants.GAME_PLAYER_OX];
				int chair_count = 0;

				for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
					if (_player_status[i] == true) {
						chairID[chair_count++] = i;
					}
				}

				if (chair_count > 0) {
					int rand = (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
					int temp = rand % chair_count;
					_cur_banker = chairID[temp];
				}
			}
		} else
			_cur_banker = _next_banker;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		_game_status = GameConstants.GS_OX_ADD_JETTON;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if (i != _cur_banker || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if ((i != _cur_banker) && (_player_status[k] == true)) {
						if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
							for (int j = 0; j < 10; j++) {
								cards.addItem(j + 1);
								_jetton_info_cur[i][j] = j + 1;
							}
							if (_can_tuizhu_player[i] > 0) {
								_jetton_info_cur[i][11] = _can_tuizhu_player[i];
								cards.addItem(_jetton_info_cur[i][11]);
							}
						} else {
							for (int j = 0; j < _jetton_count; j++) {
								cards.addItem(_jetton_info_sever_ox[j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
							}
							_cur_jetton_count[k] = _jetton_count;
							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][_jetton_count]);
								_cur_jetton_count[k]++;
							}
						}

					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (((i != _cur_banker) && (_player_status[i] == true))
						|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
					if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
						for (int j = 0; j < 10; j++) {
							cards.addItem(j + 1);
							_jetton_info_cur[i][j] = j + 1;
						}
						if (_can_tuizhu_player[i] > 0) {
							_jetton_info_cur[i][11] = _can_tuizhu_player[i];
							cards.addItem(_jetton_info_cur[i][11]);
						}
					} else {
						for (int j = 0; j < _jetton_count; j++) {
							cards.addItem(_jetton_info_sever_ox[j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[j];
						}

						if (_can_tuizhu_player[i] > 0) {
							_jetton_info_cur[i][_jetton_count] = _can_tuizhu_player[i];
							cards.addItem(_jetton_info_cur[i][_jetton_count]);
						}
					}

				}

				game_start.addJettonCell(i, cards);
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		load_room_info_data(roomResponse);

		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (((k != _cur_banker) && (_player_status[k] == true)) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
					for (int j = 0; j < 10; j++) {
						cards.addItem(j + 1);
						_jetton_info_cur[k][j] = j + 1;
					}
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][11] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][11]);
					}
				} else {
					for (int j = 0; j < _jetton_count; j++) {
						cards.addItem(_jetton_info_sever_ox[j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
					}
					_cur_jetton_count[k] = _jetton_count;
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][_jetton_count]);
						_cur_jetton_count[k]++;
					}
				}

			}

			game_start.addJettonCell(k, cards);
		}
		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, _game_status);

		gu_ding_add_jetton();

		return true;
	}

	public boolean game_start_niu_niu() {
		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		_game_status = GameConstants.GS_OX_ADD_JETTON;

		if (_cur_round == 1) {
			int chairID[] = new int[GameConstants.GAME_PLAYER_OX];
			int chair_count = 0;

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (_player_status[i] == true) {
					chairID[chair_count++] = i;
				}
			}

			if (chair_count > 0) {
				int rand = (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
				int temp = rand % chair_count;
				_cur_banker = chairID[temp];

			}
		} else
			_cur_banker = _next_banker;

		_next_banker = _cur_banker;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if (i != _cur_banker || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if ((i != _cur_banker) && (_player_status[k] == true)) {
						if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
							for (int j = 0; j < 10; j++) {
								cards.addItem(j + 1);
								_jetton_info_cur[k][j] = j + 1;
							}
							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][11] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][11]);
							}
						} else {
							for (int j = 0; j < _jetton_count; j++) {
								cards.addItem(_jetton_info_sever_ox[j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
							}

							_cur_jetton_count[k] = _jetton_count;
							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][_jetton_count]);
								_cur_jetton_count[k]++;
							}
						}
					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();

			game_start.setCurBanker(_cur_banker);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (((i != _cur_banker) && (_player_status[i] == true))
						|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
					if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
						for (int j = 0; j < 10; j++) {
							cards.addItem(j + 1);
							_jetton_info_cur[i][j] = j + 1;
						}
						if (_can_tuizhu_player[i] > 0) {
							_jetton_info_cur[i][11] = _can_tuizhu_player[i];
							cards.addItem(_jetton_info_cur[i][11]);
						}
					} else {
						for (int j = 0; j < _jetton_count; j++) {
							cards.addItem(_jetton_info_sever_ox[j]);
							_jetton_info_cur[i][j] = _jetton_info_sever_ox[j];
						}

						if (_can_tuizhu_player[i] > 0) {
							_jetton_info_cur[i][_jetton_count] = _can_tuizhu_player[i];
							cards.addItem(_jetton_info_cur[i][_jetton_count]);
						}
					}
				}

				game_start.addJettonCell(i, cards);
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		load_room_info_data(roomResponse);

		GameStart.Builder game_start = GameStart.newBuilder();
		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (((k != _cur_banker) && (_player_status[k] == true)) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
					for (int j = 0; j < 10; j++) {
						cards.addItem(j + 1);
						_jetton_info_cur[k][j] = j + 1;
					}
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][11] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][11]);
					}
				} else {
					for (int j = 0; j < _jetton_count; j++) {
						cards.addItem(_jetton_info_sever_ox[j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
					}

					_cur_jetton_count[k] = _jetton_count;
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][_jetton_count]);
						_cur_jetton_count[k]++;
					}
				}
			}

			game_start.addJettonCell(k, cards);
		}

		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, _game_status);

		gu_ding_add_jetton();

		return true;
	}

	private boolean game_start_fang_zhu() {
		_game_status = GameConstants.GS_OX_ADD_JETTON;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.HJK_ADD_JETTON_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_ADD_JETTON_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			if (i != _cur_banker || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

					if ((i != _cur_banker) && (_player_status[k] == true)) {
						if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
							for (int j = 0; j < 10; j++) {
								cards.addItem(j + 1);
								_jetton_info_cur[k][j] = j + 1;
							}
							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][11] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][11]);
							}
						} else {
							for (int j = 0; j < _jetton_count; j++) {
								cards.addItem(_jetton_info_sever_ox[j]);
								_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
							}

							_cur_jetton_count[k] = _jetton_count;

							if (_can_tuizhu_player[k] > 0) {
								_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
								cards.addItem(_jetton_info_cur[k][_jetton_count]);
								_cur_jetton_count[k]++;
							}
						}

					}

					game_start.addJettonCell(k, cards);
				}
			} else {
				_can_tuizhu_player[i] = 0;
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			load_room_info_data(roomResponse);

			GameStart.Builder game_start = GameStart.newBuilder();
			game_start.setCurBanker(_cur_banker);

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (((k != _cur_banker) && (_player_status[k] == true))
						|| GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
					if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
						for (int j = 0; j < 10; j++) {
							cards.addItem(j + 1);
							_jetton_info_cur[k][j] = j + 1;
						}
						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][11] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][11]);
						}
					} else {
						for (int j = 0; j < _jetton_count; j++) {
							cards.addItem(_jetton_info_sever_ox[j]);
							_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
						}

						_cur_jetton_count[k] = _jetton_count;

						if (_can_tuizhu_player[k] > 0) {
							_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
							cards.addItem(_jetton_info_cur[k][_jetton_count]);
							_cur_jetton_count[k]++;
						}
					}

				}

				game_start.addJettonCell(k, cards);
			}

			roomResponse_ox.setGameStart(game_start);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		load_room_info_data(roomResponse);

		GameStart.Builder game_start = GameStart.newBuilder();

		game_start.setCurBanker(_cur_banker);

		for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			if (((k != _cur_banker) && (_player_status[k] == true)) || GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF)) {
				if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
					for (int j = 0; j < 10; j++) {
						cards.addItem(j + 1);
						_jetton_info_cur[k][j] = j + 1;
					}
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][11] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][11]);
					}
				} else {
					for (int j = 0; j < _jetton_count; j++) {
						cards.addItem(_jetton_info_sever_ox[j]);
						_jetton_info_cur[k][j] = _jetton_info_sever_ox[j];
					}

					_cur_jetton_count[k] = _jetton_count;
					if (_can_tuizhu_player[k] > 0) {
						_jetton_info_cur[k][_jetton_count] = _can_tuizhu_player[k];
						cards.addItem(_jetton_info_cur[k][_jetton_count]);
						_cur_jetton_count[k]++;
					}
				}

			}

			game_start.addJettonCell(k, cards);
		}

		roomResponse_ox.setGameStart(game_start);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_add_jetton;
		_handler_add_jetton.reset_status(0, _game_status);

		gu_ding_add_jetton();

		return true;
	}

	public void add_jetton_ox(int seat_index) {
		{
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			AddJetton.Builder add_jetton = AddJetton.newBuilder();
			add_jetton.setSeatIndex(seat_index);
			add_jetton.setJettonScore(_add_Jetton[seat_index]);

			roomResponse_ox.setAddJetton(add_jetton);
			roomResponse.setType(MsgConstants.RESPONSE_ADD_JETTON);
			roomResponse.setRoomResponseOx(roomResponse_ox);

			RoomUtil.send_response_to_room(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		AddJetton.Builder add_jetton = AddJetton.newBuilder();
		add_jetton.setSeatIndex(seat_index);
		add_jetton.setJettonScore(_add_Jetton[seat_index]);

		roomResponse_ox.setAddJetton(add_jetton);
		roomResponse.setType(MsgConstants.RESPONSE_ADD_JETTON);
		roomResponse.setRoomResponseOx(roomResponse_ox);

		GRR.add_room_response(roomResponse);
	}

	public void send_card_date_ox() {
		_game_status = GameConstants.GS_OX_OPEN_CARD;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] == null) {
				continue;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}
					} else {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);

			send_response_to_player(i, roomResponse);
		}

		{
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			SendCard.Builder send_card = SendCard.newBuilder();

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[i] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
					send_card.addSendCard(i, cards);
				} else {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
					send_card.addSendCard(i, cards);
				}
			}

			roomResponse_ox.setSendCard(send_card);

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);
			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] == true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				} else {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}

				send_card.addSendCard(k, cards);
			}
			roomResponse_ox.setSendCard(send_card);
		}

		roomResponse.setRoomResponseOx(roomResponse_ox);

		GRR.add_room_response(roomResponse);

		_handler = _handler_open_card;
		_handler_open_card.reset_status(_game_status);

		return;
	}

	private boolean game_start_tong_bi() {
		_game_status = GameConstants.GS_OX_OPEN_CARD;

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.HJK_OPEN_CARD_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_OPEN_CARD_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] != null) {
				_player_status[i] = true;
			} else {
				_player_status[i] = false;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
			game_cell = 1;
		}

		if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
			game_cell = 2;
		}

		if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
			game_cell = 4;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			GameStart.Builder game_start = GameStart.newBuilder();

			game_start.setCurBanker(-1);

			roomResponse_ox.setGameStart(game_start);

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					if (k == i) {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GRR._cards_data[k][j]);
						}
					} else {
						for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
							cards.addItem(GameConstants.BLACK_CARD);
						}
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			send_response_to_player(i, roomResponse);
		}

		if (observers().count() > 0) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
			GameStart.Builder game_start = GameStart.newBuilder();

			game_start.setCurBanker(-1);

			roomResponse_ox.setGameStart(game_start);

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] != true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				} else {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.BLACK_CARD);
					}
				}

				GRR._video_recode.addHandCards(k, cards);
				send_card.addSendCard(k, cards);
			}

			roomResponse_ox.setSendCard(send_card);

			load_room_info_data(roomResponse);

			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(_cur_operate_time);

			roomResponse_ox.setDisplayTime(timer);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}

			roomResponse.setRoomResponseOx(roomResponse_ox);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			RoomUtil.send_response_to_observer(this, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		GameStart.Builder game_start = GameStart.newBuilder();

		game_start.setCurBanker(-1);

		roomResponse_ox.setGameStart(game_start);

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			SendCard.Builder send_card = SendCard.newBuilder();

			for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {

				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

				if (_player_status[k] == true) {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GRR._cards_data[k][j]);
					}
				} else {
					for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}

				send_card.addSendCard(k, cards);
			}
			roomResponse_ox.setSendCard(send_card);
		}

		roomResponse.setRoomResponseOx(roomResponse_ox);

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		_handler = _handler_open_card;
		_handler_open_card.reset_status(_game_status);

		return true;
	}

	public void open_card_ox(int seat_index) {
		int times = 1;

		if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
			if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_FAN_BEN))
				times = _logic.get_times_mul(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);
			if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_PING_BEN))
				times = _logic.get_times_ping(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);
		} else {
			if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
				times = _logic.get_times_two(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);
			if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
				times = _logic.get_times_one(GRR._cards_data[seat_index], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);
		}

		{
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			GRR._video_recode.addHandCards(cards);

			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				cards.addItem(GRR._cards_data[seat_index][j]);
			}

			SendCard.Builder sendCard = SendCard.newBuilder();
			sendCard.addSendCard(cards);

			roomResponse_ox.setSendCard(sendCard);

			CardType.Builder card_type = CardType.newBuilder();
			card_type.setCardType(_card_type_ox[seat_index]);
			card_type.setTime(times);

			roomResponse_ox.setCardType(card_type);

			OpenCard.Builder open_card = OpenCard.newBuilder();
			open_card.setSeatIndex(seat_index);
			open_card.setOpen(true);

			roomResponse_ox.setOpenCard(open_card);
			roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);
			roomResponse.setRoomResponseOx(roomResponse_ox);

			RoomUtil.send_response_to_room(this, roomResponse);
		}

		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
			cards.addItem(GRR._cards_data[seat_index][j]);
		}

		SendCard.Builder sendCard = SendCard.newBuilder();
		sendCard.addSendCard(cards);

		roomResponse_ox.setSendCard(sendCard);

		OpenCard.Builder open_card = OpenCard.newBuilder();
		open_card.setSeatIndex(seat_index);
		open_card.setOpen(true);

		roomResponse_ox.setOpenCard(open_card);

		CardType.Builder card_type = CardType.newBuilder();
		card_type.setCardType(_card_type_ox[seat_index]);
		card_type.setTime(times);

		roomResponse_ox.setCardType(card_type);

		roomResponse.setType(MsgConstants.RESPONSE_OPEN_CARD);
		roomResponse.setRoomResponseOx(roomResponse_ox);

		GRR.add_room_response(roomResponse);
	}

	public void process_tbox_calulate_end() {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] == true) {
				_win_player_oxtb = i;
				break;
			}
		}

		for (int i = _win_player_oxtb + 1; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] != true)
				continue;

			boolean first_ox = _logic.get_ox_card(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
					_game_type_index);

			boolean next_ox = _logic.get_ox_card(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);

			boolean action = _logic.compare_card(GRR._cards_data[_win_player_oxtb], GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, first_ox,
					next_ox, _game_rule_index, _game_type_index);

			if (action == false) {
				_win_player_oxtb = i;
			}
		}
	}

	public void process_ox_calulate_end() {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (i == _cur_banker)
				continue;

			if (_player_status[i] != true)
				continue;

			boolean first_ox = _logic.get_ox_card(GRR._cards_data[_cur_banker], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);

			boolean next_ox = _logic.get_ox_card(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);

			boolean action = _logic.compare_card(GRR._cards_data[_cur_banker], GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, first_ox, next_ox,
					_game_rule_index, _game_type_index);

			int next_type = _logic.get_card_type(GRR._cards_data[_cur_banker], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);

			int first_type = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);

			if (has_rule(GameConstants.GAME_RULE_EQUAL_PING) && (next_type == first_type)) {
				_ping_Player_ox[i] = true;
				continue;
			}

			if (action == true) {
				_win_player_ox[i] = true;
			}
		}
	}

	private void countCardType(int card_type, int seat_index) {
		try {
			if (card_type == GameConstants.OX_FIVE_KING) {
				MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.oxwuhuaox, "", 0, 0l, getRoom_id());
			}

			if (card_type == GameConstants.OX_BOOM) {
				MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.oxboomox, "", 0, 0l, getRoom_id());
			}

			if (card_type == GameConstants.OX_WUXIAONIU) {
				MongoDBServiceImpl.getInstance().card_log(get_players()[seat_index], ECardType.oxwuxiaoox, "", 0, 0l, getRoom_id());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void process_chi_calulate_score() {
		GRR._win_order[_win_player_oxtb] = 1;

		int calculate_score = 0;

		if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
			if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_FAN_BEN))
				calculate_score = _logic.get_times_mul(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
						_game_type_index);
			if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_PING_BEN))
				calculate_score = _logic.get_times_ping(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
						_game_type_index);
		} else {
			if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
				calculate_score = _logic.get_times_two(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
						_game_type_index);
			if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
				calculate_score = _logic.get_times_one(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
						_game_type_index);
		}

		float lChiHuScore = calculate_score * game_cell;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] == false)
				continue;
			int card_type = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);
			countCardType(card_type, i);
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_ping_Player_ox[i] == true)
				continue;
			if (_player_status[i] == false)
				continue;
			if (i == _win_player_oxtb) {
				continue;
			}

			GRR._game_score[i] -= lChiHuScore;
			GRR._game_score[_win_player_oxtb] += lChiHuScore;
		}
	}

	public void process_chi_calulate_score_ox() {
		int calculate_score = 0;
		float lChiHuScore = calculate_score;

		_win_player_oxtb = _cur_banker;

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_player_status[i] == false)
				continue;

			int card_type = _logic.get_card_type(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index, _game_type_index);

			countCardType(card_type, i);
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_ping_Player_ox[i] == true)
				continue;

			if (_player_status[i] == false)
				continue;

			if (i == _cur_banker) {
				continue;
			}

			if (_win_player_ox[i] == true) {
				if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
					if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_FAN_BEN))
						calculate_score = _logic.get_times_mul(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_PING_BEN))
						calculate_score = _logic.get_times_ping(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					lChiHuScore = calculate_score * _banker_times * _add_Jetton[i];
				} else {
					if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
						calculate_score = _logic.get_times_two(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
						calculate_score = _logic.get_times_one(GRR._cards_data[_win_player_oxtb], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					lChiHuScore = calculate_score * _banker_times * _add_Jetton[i];
				}
			} else {
				if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
					if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_FAN_BEN))
						calculate_score = _logic.get_times_mul(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					if (GameDescUtil.has_rule(getGameRuleIndexEx(), GameConstants.GAME_RULE_JDOX_PING_BEN))
						calculate_score = _logic.get_times_ping(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					lChiHuScore = -calculate_score * _banker_times * _add_Jetton[i];
				} else {
					if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_ONE))
						calculate_score = _logic.get_times_two(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					if (has_rule(GameConstants.GAME_RULE_SELECT_OX_VALUSE_TWO))
						calculate_score = _logic.get_times_one(GRR._cards_data[i], GameConstants.OX_MAX_CARD_COUNT, _game_rule_index,
								_game_type_index);
					lChiHuScore = -calculate_score * _banker_times * _add_Jetton[i];
				}
			}

			GRR._game_score[i] -= lChiHuScore;
			GRR._game_score[_cur_banker] += lChiHuScore;
		}

		if (_tui_zhu_times > 0) {
			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (_player_status[i] == false)
					continue;

				if (i == _cur_banker)
					continue;

				if (_can_tuizhu_player[i] == 0) {
					if (GRR._game_score[i] > 0) {
						int temp = 0;

						if ((int) GRR._game_score[i] + _add_Jetton[i] > _jetton_info_cur[i][0] * _tui_zhu_times)
							temp = _jetton_info_cur[i][0] * _tui_zhu_times;
						else
							temp = (int) GRR._game_score[i] + _add_Jetton[i];

						if (_jetton_info_sever_ox[_jetton_count - 1] >= temp)
							_can_tuizhu_player[i] = 0;
						else
							_can_tuizhu_player[i] = temp;
					}
				}
			}
		}
	}

	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > GameConstants.GAME_PLAYER_OX)
			return false;

		return istrustee[seat_index];
	}

	private void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = repertory_card[i * 5 + j];
			}
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > GameConstants.OX_MAX_CARD_COUNT) {
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

	public void testRealyCard(int[] realyCards) {
		int count = realyCards.length / GameConstants.OX_MAX_CARD_COUNT;

		if (count > 6)
			count = 6;

		for (int i = 0; i < count; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}

		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i++) {
				for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
			}

			if (i == count)
				break;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;

		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");
	}

	public void testSameCard(int[] cards) {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

			for (int j = 0; j < GameConstants.OX_MAX_CARD_COUNT; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		_handler = _handler_dispath_card;
		_handler_dispath_card.reset_status(cur_player, type);
		_handler.exe(this);

		return true;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (_wait_cancel_trustee[i] == true) {
				_wait_cancel_trustee[i] = false;
				handler_request_trustee(i, false, 0);
			}
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return handler_game_finish_tbox(seat_index, reason);
	}

	public boolean handler_game_finish_tbox(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER_OX;
		}

		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		if (_game_scheduled != null)
			kill_timer();

		set_timer(GameConstants.HJK_READY_TIMER, GameConstants.HJK_READY_TIME_SECONDS, true);
		set_trustee_timer(GameConstants.HJK_READY_TIMER, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);

		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

		Timer_OX.Builder timer = Timer_OX.newBuilder();
		timer.setDisplayTime(_cur_operate_time);

		roomResponse_ox.setDisplayTime(timer);

		roomResponse.setRoomResponseOx(roomResponse_ox);

		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = RoomInfo.newBuilder();

		room_info.setRoomId(getRoom_id());
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
		room_info.setCreatePlayerId(getRoom_owner_account_id());

		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);

			GRR._end_type = reason;

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++)
				game_end.addGameScore(GRR._game_score[i]);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
				if (_game_scheduled != null)
					kill_timer();
			} else {
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			if (reason == GameConstants.Game_End_RELEASE_NO_BEGIN)
				real_reason = reason;
			else
				real_reason = GameConstants.Game_End_RELEASE_PLAY;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}

		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		RoomUtil.send_response_to_room(this, roomResponse);

		record_game_round(game_end, real_reason);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER_OX; j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	@Override
	public boolean handler_enter_room(Player player) {
		if (getRuleValue(GameConstants.GAME_RULE_IP) > 0) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;

				if (player.getAccount_id() == tarplayer.getAccount_id())
					continue;

				if ((!is_sys()) && StringUtils.isNotEmpty(tarplayer.getAccount_ip()) && StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					send_error_notify(player, 1, "不允许相同ip进入");
					return false;
				}
			}
		}

		int seat_index = GameConstants.INVALID_SEAT;

		/**
		 * 1) 勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，但只有房主能坐下（有空位时），此时普通用户进入时界面上只有“观战”按键。
		 * 2)不勾选此选项，游戏开始后房主和普通用户均可进入游戏观战，且均能坐下（有空位时）。
		 * 3)游戏开始前进入桌子观战的用户，不管是否勾选此选项，只要有空位可随时坐下
		 */
		if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
			if (player.getAccount_id() != getRoom_owner_account_id()
					&& GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_NOKEJIN)) {

				boolean flag = false;
				for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
					if (_player_status[i] == true)
						flag = true;
				}
				if (flag == true) {
					send_error_notify(player, 2, "该房间已经禁止其它玩家在游戏中进入");
					return false;
				}
			}
		} else {
			if (player.getAccount_id() != getRoom_owner_account_id() && has_rule(GameConstants.GAME_RULE_START_FORBID_JOIN)) {

				boolean flag = false;
				for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
					if (_player_status[i] == true)
						flag = true;
				}
				if (flag == true) {
					send_error_notify(player, 2, "该房间已经禁止其它玩家在游戏中进入");
					return false;
				}
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] == null) {
				get_players()[i] = player;
				seat_index = i;
				break;
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);
			send_error_notify(player, 1, "游戏已经开始");
			return false;
		}

		if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id())
			_own_room_seat = seat_index;

		if (!observers().sit(player.getAccount_id())) {
			logger.error(String.format("玩家[%s]必须先成为观察者才能坐下!", player));
		}

		if (!onPlayerEnterUpdateRedis(player.getAccount_id())) {
			send_error_notify(player, 1, "已在其他房间中");
			return false;
		}

		player.set_seat_index(seat_index);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

		send_response_to_other(player.get_seat_index(), roomResponse);

		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		roomRedisModel.getNames().add(player.getNick_name());

		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);

		if (has_rule(GameConstants.GAME_RULE_CONTORL_START)) {
			if (is_game_start == 1)
				is_game_start = 0;

			control_game_start();
		}

		return true;
	}

	public boolean control_game_start() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ROOM_OWNER_START);
		roomResponse.setEffectType(is_game_start);
		roomResponse.setPaoDes(getRoom_owner_name());

		if (is_game_start != 2)
			send_response_to_room(roomResponse);

		Player player = observers().getPlayer(getRoom_owner_account_id());
		if (player == null) {
			player = get_player(getRoom_owner_account_id());
			if (player != null)
				send_response_to_player(player.get_seat_index(), roomResponse);
		} else {
			observers().send(player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean handler_ox_game_start(int room_id, long account_id) {
		if (is_game_start == 2)
			return true;

		is_game_start = 2;

		control_game_start();

		handler_game_start();

		boolean nt = true;
		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {
		int limitCount = 20;
		if (SystemConfig.gameDebug == 1) {
			limitCount = 5;
		}

		// 限制围观者数量，未来加到配置表控制
		if (player.getAccount_id() != getRoom_owner_account_id() && observers().count() >= (limitCount + getTablePlayerNumber())) {
			send_error_notify(player, 1, "当前游戏围观位置已满,下次赶早!");
			return false;
		}

		observers().enter(player);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(getGame_id());

		load_room_info_data(roomResponse);
		load_player_info_data(roomResponse);

		observers().send(player, roomResponse);

		if (player.getAccount_id() == getRoom_owner_account_id()) {
			if (is_game_start == 1) {
				control_game_start();
				return true;
			}

			int _cur_count = 0;
			int player_count = 0;
			boolean flag = false;

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					_cur_count += 1;
				}
				if (_player_status[i] == true)
					flag = true;
				if (_player_ready[i] == 0) {

				}
				if ((get_players()[i] != null) && (_player_ready[i] == 1)) {
					player_count += 1;
				}
			}

			if ((_player_count >= 2) && (_player_count == _cur_count)) {
				if (_cur_round == 0 && has_rule(GameConstants.GAME_RULE_CONTORL_START)) {
					is_game_start = 1;
					control_game_start();
				}
			} else {
				control_game_start();
			}
		}

		return true;
	}

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
			observers().send(player, quit_roomResponse);
			observers().exit(player.getAccount_id());
			return true;
		}

		return false;
	}

	@Override
	public boolean handler_player_ready_in_gold(int get_seat_index, boolean is_cancel) {
		if (is_cancel) {
			if (get_players()[get_seat_index] == null) {
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

			if (_cur_round > 0) {
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家

				load_player_info_data(roomResponse2);

				send_response_to_player(get_seat_index, roomResponse2);
			}

			return false;
		} else {
			return handler_player_ready(get_seat_index, is_cancel);
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) {
			if (get_players()[seat_index] == null) {
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

			if (is_game_start == 1) {
				is_game_start = 0;
				control_game_start();
			}

			if (_cur_round > 0) {
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

				load_player_info_data(roomResponse2);

				send_response_to_player(seat_index, roomResponse2);
			}

			return false;
		}

		if (get_players()[seat_index] == null) {
			return false;
		}

		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);

		send_response_to_room(roomResponse);

		if (_cur_round > 0) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

			load_player_info_data(roomResponse2);

			send_response_to_player(seat_index, roomResponse2);
		}

		_player_count = 0;

		int _cur_count = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (get_players()[i] == null) {
				_player_ready[i] = 0;
			} else {
				_cur_count += 1;
			}
			if ((get_players()[i] != null) && (_player_ready[i] == 1)) {
				_player_count += 1;
			}
		}

		if ((_player_count >= 2) && (_player_count == _cur_count)) {
			if (_cur_round == 0 && has_rule(GameConstants.GAME_RULE_CONTORL_START)) {
				is_game_start = 1;
				control_game_start();
			} else
				handler_game_start();
		}

		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if (istrustee[seat_index]) {
			istrustee[seat_index] = false;
			if (_trustee_schedule[seat_index] != null) {
				_trustee_schedule[seat_index].cancel(false);
				_trustee_schedule[seat_index] = null;
			}
			_trustee_type[seat_index] = 0;
		}

		if (is_game_start == 1) {
			control_game_start();
		}

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);
		} else if (GameConstants.GS_MJ_WAIT == _game_status && get_players()[seat_index] != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

			TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

			load_room_info_data(roomResponse);
			load_player_info_data(roomResponse);

			tableResponse.setSceneInfo(_game_status);

			int display_time = _cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - _operate_start_time);
			if (display_time > 0) {
				Timer_OX.Builder timer = Timer_OX.newBuilder();
				timer.setDisplayTime(display_time);
				roomResponse_ox.setDisplayTime(timer);
			}

			roomResponse_ox.setTableResponseOx(tableResponse);
			roomResponse.setRoomResponseOx(roomResponse_ox);

			send_response_to_player(seat_index, roomResponse);
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

				send_response_to_player(seat_index, roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(Player player) {
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			control_game_start();
		}

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && player != null) {
			if (_handler != null)
				_handler.handler_observer_be_in_room(this, player);
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

				observers().send(player, roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(int seat_index, int card) {
		return true;
	}

	@Override
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {
		return true;
	}

	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {
		if (_handler != null) {
			_handler.handler_operate_card(this, seat_index, operate_code, operate_card, luoCode);
		}

		return true;
	}

	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		if (_handler != null) {
			_handler.handler_call_banker(this, seat_index, call_banker);
		}

		return true;
	}

	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		if (_handler != null) {
			_handler.handler_add_jetton(this, seat_index, jetton);
		}

		return true;
	}

	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		if (_handler != null) {
			_handler.handler_open_cards(this, seat_index, open_flag);
		}

		return true;
	}

	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);

		send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				player = get_players()[i];

				if (player == null)
					continue;

				send_error_notify(i, 2, "游戏等待超时解散");
			}
		} else {
			handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			get_players()[i] = null;
		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		PlayerServiceImpl.getInstance().delRoomId(getRoom_id());

		return true;
	}

	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			_player_result.game_score[i] += GRR._game_score[i];
		}
	}

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
			playerNumber = GameConstants.GAME_PLAYER_OX;
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			if (_game_scheduled != null) {
				kill_timer();
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_trustee_schedule[i] != null) {
					_trustee_schedule[i].cancel(false);
					_trustee_schedule[i] = null;
				}
			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_request_release_time = System.currentTimeMillis() + delay * 1000;

			if (GRR == null) {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT),
						delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT),
						delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;

			int count = 0;
			for (int i = 0; i < playerNumber; i++) {
				if (_gameRoomRecord.release_players[i] == 1) {
					count++;
				}
			}

			if (count == playerNumber) {
				if (GRR == null) {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < playerNumber; j++) {
					player = get_players()[j];
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

			send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

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

			send_response_to_room(roomResponse);

			for (int i = 0; i < playerNumber; i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;
				}
			}

			for (int j = 0; j < playerNumber; j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_release_scheduled = null;

			if (GRR == null) {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
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
			RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();

			set_timer(_cur_game_timer, _cur_operate_time, false);
			set_trustee_timer(_cur_game_timer, GameConstants.OX_TRUESTEE_OPERATE_TIME, true);

			if (_cur_operate_time > 0) {
				Timer_OX.Builder timer = Timer_OX.newBuilder();
				timer.setDisplayTime(_cur_operate_time);
				roomResponse_ox.setDisplayTime(timer);
			}

			roomResponse.setRoomResponseOx(roomResponse_ox);
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

			send_response_to_room(roomResponse);

			_request_release_time = 0;

			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_release_scheduled = null;

			for (int i = 0; i < playerNumber; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < playerNumber; j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + get_players()[seat_index].getNick_name() + "]不同意解散");
			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);

				send_response_to_room(roomResponse);

				for (int i = 0; i < playerNumber; i++) {
					Player p = get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}

				huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);

				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {
			if (_player_status[seat_index] == true) {
				if (GameConstants.GS_MJ_FREE != _game_status) {
					return false;
				}
				send_error_notify(seat_index, 2, "您已经开始游戏了,不能退出游戏");
				return false;
			}

			if (get_players()[seat_index] != null) {
				if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id())
					_own_room_seat = -1;
			}

			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);

			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");

			get_players()[seat_index] = null;

			_player_ready[seat_index] = 0;
			_player_open_less[seat_index] = 0;

			PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), player.getAccount_id());

			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

			load_player_info_data(refreshroomResponse);

			send_response_to_other(seat_index, refreshroomResponse);

			int _cur_count = 0;
			int player_count = 0;
			boolean flag = false;

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				if (get_players()[i] == null) {
					_player_ready[i] = 0;
					continue;
				} else {
					_cur_count += 1;
				}

				if (_player_status[i] == true)
					flag = true;

				if (_player_ready[i] == 1) {
					player_count += 1;
				}

			}

			if ((player_count >= 2) && (player_count == _cur_count)) {
				if (_cur_round == 0 && has_rule(GameConstants.GAME_RULE_CONTORL_START)) {
					is_game_start = 1;
					control_game_start();
				} else
					handler_game_start();
			}

			if ((player_count < 2) || (player_count != _cur_count)) {
				int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index);
				if (_cur_round == 0 && has_rule(GameConstants.GAME_RULE_CONTORL_START)) {
					is_game_start = 0;
					control_game_start();
				}
			}

			refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);

			send_response_to_room(roomResponse);

			for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
				Player p = get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}

			huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);

			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
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

	public boolean operate_player_status() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);

		load_common_status(roomResponse);

		return send_response_to_room(roomResponse);
	}

	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

		load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return send_response_to_room(roomResponse);
	}

	public boolean operate_out_card(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		load_common_status(roomResponse);

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
			return send_response_to_room(roomResponse);
		} else {
			return send_response_to_player(to_player, roomResponse);
		}

	}

	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);
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

			send_response_to_player(seat_index, roomResponse);

			GRR.add_room_response(roomResponse);

			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(GameConstants.BLACK_CARD);
			}

			send_response_to_other(seat_index, roomResponse);
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

			send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	private boolean operate_add_discard(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_ADD_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// 出牌
		roomResponse.setCardCount(count);

		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

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
			GRR.add_room_response(roomResponse);
			send_response_to_room(roomResponse);
		} else {
			send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
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

	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);

		load_common_status(roomResponse);

		if (close == true) {
			send_response_to_player(seat_index, roomResponse);
			return true;
		}

		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}

		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
					| GameConstants.WIK_EQS;
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {

							chi_group.addLouWeaveKind(curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);
						}
					}
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();

		load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);
		roomResponse.setCardCount(count);

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(2500);
			roomResponse.setInsertTime(150);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}

			send_response_to_other(seat_index, roomResponse);

			roomResponse.clearCardData();

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}

			GRR.add_room_response(roomResponse);

			return send_response_to_player(seat_index, roomResponse);
		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}

				GRR.add_room_response(roomResponse);

				return send_response_to_player(seat_index, roomResponse);
			}
		}

		return false;
	}

	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

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

		send_response_to_other(seat_index, roomResponse);

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

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_player_xiang_gong_flag(int seat_index, boolean is_xiang_gong) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_XIANGGONG);
		roomResponse.setProvidePlayer(seat_index);
		roomResponse.setIsXiangGong(is_xiang_gong);

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);
		return true;
	}

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);

		GRR.add_room_response(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_remove_discard(int seat_index, int discard_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setDiscardIndex(discard_index);

		GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);

		load_common_status(roomResponse);

		load_player_info_data(roomResponse);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public boolean handler_player_offline(Player player) {
		if (observers().exist(player.getAccount_id())) {
			handler_exit_room_observer(player);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setGameStatus(_game_status);
			roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

			load_player_info_data(roomResponse);

			send_response_to_other(player.get_seat_index(), roomResponse);
		}

		return true;
	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		huan_dou(reason);

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = GameConstants.GAME_PLAYER_OX;
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
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			player_result.addPlayersId(i);
		}

		player_result.setRoomId(getRoom_id());
		player_result.setRoomOwnerAccountId(getRoom_owner_account_id());
		player_result.setRoomOwnerName(getRoom_owner_name());
		player_result.setCreateTime(getCreate_time());
		player_result.setRecordId(get_record_id());
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

	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_cur_banker);

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

	public boolean exe_chuli_first_card(int seat_index, int type, int delay_time) {
		return true;
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			rplayer = get_players()[i];

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

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	public boolean is_zhuang_xian() {
		if ((GameConstants.GAME_TYPE_ZZ == _game_type_index) || is_mj_type(GameConstants.GAME_TYPE_HZ)
				|| is_mj_type(GameConstants.GAME_TYPE_SHUANGGUI) || is_mj_type(GameConstants.GAME_TYPE_ZHUZHOU)) {
			return false;
		}

		return true;
	}

	public int get_real_card(int card) {
		return card;
	}

	public boolean exe_finish() {
		_handler = _handler_finish;
		_handler_finish.exe(this);
		return true;
	}

	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (card_count > 0) {
			if (card_data[0] == 0)
				log_error(" 加入到牌堆" + seat_index + ':' + card_count + ':' + card_data[0]);

		}

		if (delay == 0) {
			runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}

		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

		return true;

	}

	public boolean exe_dispatch_card(int seat_index, int type, int delay) {
		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			_handler = _handler_dispath_card;
			_handler_dispath_card.reset_status(seat_index, type);
			_handler.exe(this);
		}

		return true;
	}

	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d) {
		return true;
	}

	public boolean exe_out_card(int seat_index, int card, int type) {
		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {
		GameSchedule.put(new JianPaoHuRunnable(getRoom_id(), seat_index, action, card), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
				TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}

		if (GRR == null)
			return;

		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;
			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}

		if (send_client == true) {
			operate_add_discard(seat_index, card_count, card_data);
		}
	}

	public void runnable_remove_middle_cards(int seat_index) {
	}

	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	private void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			pl.add(get_players()[i]);
		}

		Collections.shuffle(pl);
		pl.toArray(get_players());

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i].set_seat_index(i);
		}
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (_playerStatus == null || istrustee == null)
			return false;

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

		if (istrustee[get_seat_index] == true)
			set_trustee_timer(_cur_game_timer, GameConstants.OX_TRUESTEE_OPERATE_TIME, false);
		else {
			if (_trustee_schedule[get_seat_index] != null) {
				_trustee_schedule[get_seat_index].cancel(false);
				_trustee_schedule[get_seat_index] = null;
			}
		}

		send_response_to_room(roomResponse);

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

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;

	}

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER_OX;
	}

	private boolean kou_dou_aa(Player cur_player, int seat_index, boolean create_room) {
		int game_id = 0;
		int game_type_index = _game_type_index;
		int index = 0;

		game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(game_type_index);
		index = SysGameTypeDict.getInstance().getGameGoldTypeIndex(game_type_index);

		setGame_id(game_id);

		SysParamModel sysParamModel1007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1007);
		SysParamModel sysParamModel1008 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1008);
		SysParamModel sysParamModel1009 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(1009);

		int check_gold = 0;
		boolean create_result = true;

		if (_game_round == sysParamModel1007.getVal1()) {
			check_gold = sysParamModel1007.getVal3();
		} else if (_game_round == sysParamModel1008.getVal1()) {
			check_gold = sysParamModel1008.getVal3();
		} else if (_game_round == sysParamModel1009.getVal1()) {
			check_gold = sysParamModel1009.getVal3();
		}

		if (check_gold == 0) {
			create_result = false;
		} else {
			if (create_room == true) {
				SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);

				if (sysParamModel != null && sysParamModel.getVal2() == 1) {
					StringBuilder buf = new StringBuilder();
					buf.append("创建房间:" + getRoom_id()).append("game_id:" + getGame_id()).append(",game_type_index:" + game_type_index)
							.append(",game_round:" + _game_round);
					AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(getRoom_owner_account_id(), check_gold, false,
							buf.toString());
					if (result.isSuccess() == false) {
						create_result = false;
					} else {
						cost_dou = check_gold;
					}
				}
			}

			if (create_result == false) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);

				Player player = null;

				for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
					send_response_to_player(i, roomResponse);

					player = get_players()[i];
					if (player == null)
						continue;
					if (i == 0) {
						send_error_notify(i, 1, SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足,游戏解散"));
					} else {
						send_error_notify(i, 1, SysParamServerDict.getInstance().replaceGoldTipsWord("创建人闲逸豆不足,游戏解散"));
					}
				}

				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());

				return false;
			}

		}
		if (create_room == false) {
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(game_id).get(index);

			if (sysParamModel != null && sysParamModel.getVal2() == 1) {
				StringBuilder buf = new StringBuilder();
				AddGoldResultModel result = PlayerServiceImpl.getInstance().subGold(cur_player.getAccount_id(), check_gold, false, buf.toString());

				if (result.isSuccess() == false) {
					create_result = false;
				} else {
					cost_dou = check_gold;
				}
			}

			if (create_result == false) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);

				send_error_notify(cur_player, 1, SysParamServerDict.getInstance().replaceGoldTipsWord("您的闲逸豆不够，不能参与游戏"));

				return false;
			}
		}

		return true;
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
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

		load_player_info_data(roomResponse);

		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean set_trustee_timer(int timer_type, int time, boolean makeDBtimer) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER_OX];
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

		if (makeDBtimer == false)
			time = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		_cur_game_timer = timer_type;

		SysParamModel sysParamModel3008 = SysParamDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(3008);

		if (sysParamModel3008.getVal5() == 0)
			return true;

		if (makeDBtimer == false) {
			_cur_game_timer = timer_type;

			if (timer_type == GameConstants.HJK_READY_TIMER) {
				_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
				_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
				_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), time, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = time;
			}

			return true;
		}

		_cur_game_timer = timer_type;

		if (timer_type == GameConstants.HJK_READY_TIMER) {
			_game_scheduled = GameSchedule.put(new ReadyRunnable(getRoom_id()), sysParamModel3008.getVal1(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3008.getVal1();
		} else if (timer_type == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			_game_scheduled = GameSchedule.put(new RobotBankerRunnable(getRoom_id()), sysParamModel3008.getVal2(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3008.getVal2();
		} else if (timer_type == GameConstants.HJK_ADD_JETTON_TIMER) {
			_game_scheduled = GameSchedule.put(new AddJettonRunnable(getRoom_id()), sysParamModel3008.getVal3(), TimeUnit.SECONDS);
			_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
			_cur_operate_time = sysParamModel3008.getVal3();
		} else if (timer_type == GameConstants.HJK_OPEN_CARD_TIMER) {
			if (has_rule(GameConstants.GAME_RULE_FORBID_CHOU_PAI)) {
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), 5, TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = 5;
			} else {
				_game_scheduled = GameSchedule.put(new OpenCardRunnable(getRoom_id()), sysParamModel3008.getVal4(), TimeUnit.SECONDS);
				_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
				_cur_operate_time = sysParamModel3008.getVal4();
			}
		}

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

	@Override
	public boolean open_card_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;
			if (_open_card[i] == false) {
				_handler.handler_open_cards(this, i, true);
			}
		}

		return false;
	}

	public boolean gu_ding_add_jetton() {
		if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY) == false) {
			return false;
		}

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_GDYF) == false) {
			return false;
		}

		int jetton = 0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_ONE_FEN)) {
			jetton = 0;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_TWO_FEN)) {
			jetton = 1;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_THREE_FEN)) {
			jetton = 2;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_FOUR_FEN)) {
			jetton = 3;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_FIVE_FEN)) {
			jetton = 4;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_SIX_FEN)) {
			jetton = 5;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_SEVEN_FEN)) {
			jetton = 6;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_EIGHT_FEN)) {
			jetton = 7;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_NINE_FEN)) {
			jetton = 8;
		}
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_JDOX_TEN_FEN)) {
			jetton = 9;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (i == _cur_banker)
				continue;

			if (_add_Jetton[i] == 0) {
				_handler.handler_add_jetton(this, i, jetton);
			}
		}

		return true;
	}

	@Override
	public boolean robot_banker_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;
			if (_call_banker[i] == -1) {
				_handler.handler_call_banker(this, i, 0);
			}
		}

		return false;
	}

	@Override
	public boolean ready_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++)
			if (get_players()[i] != null) {
				if (_player_ready[i] == 0) {
					handler_player_ready(i, false);
				}
			}

		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_status[i] == false)
				continue;

			if (i == _cur_banker)
				continue;

			if (_add_Jetton[i] == 0) {
				_handler.handler_add_jetton(this, i, 0);
			}
		}

		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		if (operate_id == GameConstants.HJK_READY_TIMER) {
			handler_player_ready(seat_index, false);
		} else if (operate_id == GameConstants.HJK_ROBOT_BANKER_TIMER) {
			if (_player_status[seat_index] == false)
				return true;

			if (has_trustee(GameConstants.GAME_RULE_TG_NO_BNAKER, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 0);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_ONE_BEI, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 1);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_TWO_BEI, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 2);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_THREE_BEI, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 3);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_BEI, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 4);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_IS_ROBOT_BANKER, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 1);
			}
			if (has_trustee(GameConstants.GAME_RULE_TG_NO_BNAKER, seat_index)) {
				_handler.handler_call_banker(this, seat_index, 0);
			}

		} else if (operate_id == GameConstants.HJK_ADD_JETTON_TIMER) {
			if (_player_status[seat_index] == false)
				return true;

			if (_cur_banker == seat_index)
				return true;

			if (is_mj_type(GameConstants.GAME_TYPE_JDOX_YY)) {
				int jetton = 0;
				if (has_trustee(GameConstants.GAME_RULE_TG_ONE_FEN, seat_index)) {
					jetton = 0;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
					jetton = 1;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_THREE_FEN, seat_index)) {
					jetton = 2;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
					jetton = 3;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_FIVE_FEN, seat_index)) {
					jetton = 4;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_SIX_FEN, seat_index)) {
					jetton = 5;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_SEVEN_FEN, seat_index)) {
					jetton = 6;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_EIGHT_FEN, seat_index)) {
					jetton = 7;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_NINE_FEN, seat_index)) {
					jetton = 8;
				}
				if (has_trustee(GameConstants.GAME_RULE_TG_TEN_FEN, seat_index)) {
					jetton = 9;
				}

				_handler.handler_add_jetton(this, seat_index, jetton);
			} else {
				if (_player_status[seat_index] == false)
					return true;

				if (has_trustee(GameConstants.GAME_RULE_TG_IS_TUI_ZHU, seat_index) && _can_tuizhu_player[seat_index] > 0) {
					_handler.handler_add_jetton(this, seat_index, _cur_jetton_count[seat_index] - 1);
				}

				if (has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
					if (has_trustee(GameConstants.GAME_RULE_TG_ONE_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 0);
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 1);
					}
				}

				if (has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
					if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 0);
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 1);
					}
				}

				if (has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
					if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 0);
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_EIGHT_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 1);
					}
				}

				if (has_rule(GameConstants.GAME_RULE_THREE_SIX)) {
					if (has_trustee(GameConstants.GAME_RULE_TG_THREE_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 0);
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_SIX_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 1);
					}
				}

				if (getRuleValue(GameConstants.GAME_RULE_FIVE_TO_TEN) == 1) {
					if (has_trustee(GameConstants.GAME_RULE_TG_FIVE_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 0);
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_TEN_FEN, seat_index)) {
						_handler.handler_add_jetton(this, seat_index, 1);
					}
				}

				if (has_rule(GameConstants.GAME_RULE_ONE_TO_FIVE)) {
					int jetton = 0;
					if (has_trustee(GameConstants.GAME_RULE_TG_ONE_FEN, seat_index)) {
						jetton = 0;
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_TWO_FEN, seat_index)) {
						jetton = 1;
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_THREE_FEN, seat_index)) {
						jetton = 2;
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_FOUR_FEN, seat_index)) {
						jetton = 3;
					}
					if (has_trustee(GameConstants.GAME_RULE_TG_FIVE_FEN, seat_index)) {
						jetton = 4;
					}

					_handler.handler_add_jetton(this, seat_index, jetton);
				}
			}

		} else if (operate_id == GameConstants.HJK_OPEN_CARD_TIMER) {
			if (_player_status[seat_index] == false)
				return true;

			_handler.handler_open_cards(this, seat_index, true);
		}

		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
	}
}
