/*
 * *
 * 
 */
package com.cai.game.schcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.CardsData;
import com.cai.common.domain.ChiHuRight;
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
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.ChulifirstCardRunnable;
import com.cai.future.runnable.DispatchAddCardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchCardXiaoRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.DispatchTouCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.HandCardUpdateRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.schcp.SCHCPGameLogic.AnalyseItem;
import com.cai.game.schcp.handler.SCHCPHandler;
import com.cai.game.schcp.handler.SCHCPHandlerChiPeng;
import com.cai.game.schcp.handler.SCHCPHandlerDispatchCard;
import com.cai.game.schcp.handler.SCHCPHandlerFinish;
import com.cai.game.schcp.handler.SCHCPHandlerGang;
import com.cai.game.schcp.handler.SCHCPHandlerOutCardOperate;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerChiPeng_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerChuLiFirstCard_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerDispatchAddCard_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerDispatchCard_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerDispatchFirstCard_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerDispatchTouCard_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerGang_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerOutCardOperate_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerXiaoDispatchCard_DAZHUI;
import com.cai.game.schcp.handler.dazhui.SCHCPHandlerZhao_DAZHUI;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
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
import protobuf.clazz.schcp.schcpRsp.PlayerResultSchcp;
import protobuf.clazz.schcp.schcpRsp.PukeGameEndSchcp;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class SCHCPTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;
	private static final int ID_TIMER_ANIMATION_DISPLAY_CARD = 1;

	private static Logger logger = Logger.getLogger(SCHCPTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	public boolean[] has_shoot; // 判断玩家是否射跑，射跑之后不能主动进牌
	public int[] cards_has_wei; // 牌桌上玩家偎了的牌

	public boolean[] is_hands_up; // 耒阳字牌，举手
	public int[][] player_ti_count; // 耒阳字牌，每个玩家提的记录，分大字提和小字提
	public int card_for_fan_xing; // 耒阳字牌，翻醒的牌
	public int fan_xing_count; // 耒阳字牌，翻醒加的囤数

	public boolean[] is_wang_diao; // 永州扯胡子，王钓状态
	public boolean[] is_wang_diao_wang; // 永州扯胡子，王钓王状态
	public boolean[] is_wang_chuang; // 永州扯胡子，王闯状态

	public boolean istrustee[]; // 托管状态

	// 状态变量
	private boolean _status_send; // 发牌状态
	private boolean _status_cs_gang; // 长沙杆状态

	public boolean _ti_two_long[];
	public boolean _is_xiang_gong[];
	public int _ti_mul_long[]; // 提多条龙

	// 运行变量
	public int _provide_card = GameConstants.INVALID_VALUE; // 供应扑克
	public int _resume_player = GameConstants.INVALID_SEAT; // 还原用户
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _provide_player = GameConstants.INVALID_SEAT; // 供应用户

	// 发牌信息
	public int _send_card_data = GameConstants.INVALID_VALUE; // 发牌扑克

	public CardsData _gang_card_data;

	public int _send_card_count = GameConstants.INVALID_VALUE; // 发牌数目

	public int _qiang_MAX_HH_COUNT; // 最大呛分
	public int _lian_zhuang_player; // 连庄玩家

	public int _shang_zhuang_player; // 上庄玩家
	// 出牌信息
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _out_card_count = GameConstants.INVALID_VALUE; // 出牌数目

	public int _last_card = 0; // 上次抓的牌
	public int _last_player = -1;// 上次发牌玩家
	public int _long_count[]; // 每个用户有几条龙
	public int _cannot_chi_index[][];// 不可以吃的牌
	public int _cannot_peng_index[][];// 不可以碰的牌
	public int _guo_hu_xt[]; // 同一圈用户过胡
	public int _guo_hu_pai_cards[][]; // 过胡牌
	public int _guo_hu_pai_count[]; // 过胡牌数量
	public int _guo_hu_xi[][]; // 过胡胡息
	public int _guo_hu_hu_xi[][]; // 过胡时的胡息
	public int _out_card_index[][]; // 出过的牌

	public SCHCPGameLogic _logic = null;
	public LouWeaveItem _lou_weave_item[][];
	public int _hu_weave_count[]; //
	public int _hu_xi[];
	public int _tun_shu[];
	public int _fan_shu[];
	public int _hu_code[];
	public int _da_pai_count[];
	public int _xiao_pai_count[];
	public int _huang_zhang_count;
	public int _tuan_yuan_count[];
	public int _hong_pai_count[];
	public int _ying_hu_count[];
	public int _chun_ying_count[];
	public boolean _ting_card[];
	public int _liu_zi_fen[];
	public int _zong_liu_zi_fen;
	public int _all_xi;
	public int _all_lz_fen;
	public int _hu_pai_max_hu[];
	public boolean _xt_display_an_long[];
	public boolean _xian_ming_zhao[];
	public int _shang_xing_card;
	public int _shang_xing_count[];
	public int _ben_xing_count[];
	public int _ben_card;
	public int _xia_xing_card;
	public int _xia_xing_count[];
	public boolean _is_di_hu;
	public int _provider_hu;
	public int _fang_pao_index;
	public int _xing_card[];
	public int _xing_count;
	public int _hu_xing_count[];
	public int _xing_player[];
	public int _hu_pai_type[];
	public int _xing_card_player[][];

	public int _xiao_jia;
	public boolean _is_display;
	public int _zhao_card[][];
	public int _must_zhao_card[][];
	public boolean _zhe_tian_card;
	public boolean _zhe_di_card;
	public boolean _zhe_ding_card;
	public boolean _zhe_fu_card;
	public int _zhao_guo_card[][];
	public int _game_score[][];// 每局用户的分数
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _hu_weave_items[][];

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public SCHCPHandler _handler;

	public SCHCPHandlerDispatchCard _handler_dispath_card_xiao;
	public SCHCPHandlerDispatchCard _handler_dispath_card;
	public SCHCPHandlerDispatchCard _handler_dispath_add_card;
	public SCHCPHandlerOutCardOperate _handler_out_card_operate;
	public SCHCPHandlerGang _handler_gang;
	public SCHCPHandlerChiPeng _handler_chi_peng;
	public SCHCPHandlerDispatchCard _handler_dispath_firstcards;
	public SCHCPHandlerDispatchCard _handler_chuli_firstcards;
	public SCHCPHandlerDispatchCard _handler_dispatch_toucards;
	public SCHCPHandlerChiPeng _handler_zhao;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public SCHCPHandlerFinish _handler_finish; // 结束

	public SCHCPTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new SCHCPGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;

		has_shoot = new boolean[this.getTablePlayerNumber()];
		cards_has_wei = new int[GameConstants.MAX_CP_INDEX];
		is_hands_up = new boolean[this.getTablePlayerNumber()];
		player_ti_count = new int[this.getTablePlayerNumber()][2];
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		is_wang_diao = new boolean[this.getTablePlayerNumber()];
		is_wang_diao_wang = new boolean[this.getTablePlayerNumber()];
		is_wang_chuang = new boolean[this.getTablePlayerNumber()];

		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_player_open_less = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_player_open_less[i] = 0;
		}
		_is_di_hu = false;
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][9];
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		// 出牌信息
		_out_card_data = 0;
		// 上次抓的牌
		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		// 不能吃，碰
		_cannot_chi_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_cannot_peng_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_da_pai_count = new int[this.getTablePlayerNumber()];

		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_tuan_yuan_count = new int[this.getTablePlayerNumber()];
		_hong_pai_count = new int[this.getTablePlayerNumber()];
		_ying_hu_count = new int[this.getTablePlayerNumber()];
		_chun_ying_count = new int[this.getTablePlayerNumber()];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_hu_pai_type = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_pai_type, 0);
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_shang_xing_card = 0;

		_ben_card = 0;
		_xia_xing_card = 0;
		_xing_player = new int[this.getTablePlayerNumber()];
		Arrays.fill(_xing_player, 0);
		_shang_xing_count = new int[this.getTablePlayerNumber()];
		_hu_xing_count = new int[this.getTablePlayerNumber()];
		_ben_xing_count = new int[this.getTablePlayerNumber()];
		_xia_xing_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_shang_xing_count, 0);
		Arrays.fill(_hu_xing_count, 0);
		Arrays.fill(_xia_xing_count, 0);
		Arrays.fill(_shang_xing_count, 0);

		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
			_tuan_yuan_count[i] = 0;
			_hong_pai_count[i] = 0;
			_guo_hu_xt[i] = -1;
			_ti_mul_long[i] = 0;
			_guo_hu_pai_count[i] = 0;
			_ting_card[i] = false;
			_tun_shu[i] = 0;
			_fan_shu[i] = 0;
			_hu_code[i] = GameConstants.WIK_NULL;
			_liu_zi_fen[i] = 0;
			_hu_xi[i] = 0;
			_hu_pai_max_hu[i] = 0;
			_xt_display_an_long[i] = false;
			_xian_ming_zhao[i] = false;

		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;
			}
		}
		_provider_hu = GameConstants.INVALID_SEAT;
		_fang_pao_index = GameConstants.INVALID_SEAT;
		_xing_card = new int[GameConstants.MAX_HH_COUNT];
		_xing_card_player = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_COUNT];
		_xing_count = 0;
		_xing_player = new int[this.getTablePlayerNumber()];
		Arrays.fill(_xing_player, 0);
		_shang_xing_count = new int[this.getTablePlayerNumber()];
		_hu_xing_count = new int[this.getTablePlayerNumber()];
		_ben_xing_count = new int[this.getTablePlayerNumber()];
		_xia_xing_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_shang_xing_count, 0);
		Arrays.fill(_hu_xing_count, 0);
		Arrays.fill(_xia_xing_count, 0);
		Arrays.fill(_shang_xing_count, 0);
		// 胡牌信息
		_huang_zhang_count = 0;
		// _hu_card_info = new HuCardInfo();
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
			_xing_card_player[i] = new int[GameConstants.MAX_HH_COUNT];
			Arrays.fill(_xing_card_player[i], 0);
		}
		_status_cs_gang = false;
		_xiao_jia = -1;
		_is_display = false;
		_gang_card_data = new CardsData(GameConstants.MAX_CP_COUNT);
		_zhao_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_must_zhao_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_zhao_guo_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_out_card_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Arrays.fill(_zhao_card[i], 0);
			Arrays.fill(_must_zhao_card[i], 0);
			Arrays.fill(_out_card_index[i], 0);
			Arrays.fill(_zhao_guo_card[i], 0);
		}

		_zhe_tian_card = false;
		_zhe_di_card = false;
		_zhe_ding_card = false;
		_zhe_fu_card = false;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
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
		_cannot_chi_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_cannot_peng_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_huang_zhang_count = 0;
		_tuan_yuan_count = new int[this.getTablePlayerNumber()];
		_hong_pai_count = new int[this.getTablePlayerNumber()];
		_ying_hu_count = new int[this.getTablePlayerNumber()];
		_chun_ying_count = new int[this.getTablePlayerNumber()];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_shang_xing_card = 0;
		_xia_xing_card = 0;
		_xing_player = new int[this.getTablePlayerNumber()];
		Arrays.fill(_xing_player, 0);
		_shang_xing_count = new int[this.getTablePlayerNumber()];
		_hu_xing_count = new int[this.getTablePlayerNumber()];
		_ben_xing_count = new int[this.getTablePlayerNumber()];
		_xia_xing_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_shang_xing_count, 0);
		Arrays.fill(_hu_xing_count, 0);
		Arrays.fill(_xia_xing_count, 0);
		Arrays.fill(_shang_xing_count, 0);
		_hu_pai_type = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_pai_type, 0);
		_ben_card = 0;
		_is_di_hu = false;
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
			_tuan_yuan_count[i] = 0;
			_hong_pai_count[i] = 0;
			_guo_hu_xt[i] = -1;
			_ti_mul_long[i] = 0;
			_guo_hu_pai_count[i] = 0;
			_ting_card[i] = false;
			_tun_shu[i] = 0;
			_fan_shu[i] = 0;
			_hu_code[i] = GameConstants.WIK_NULL;
			_liu_zi_fen[i] = 0;
			_hu_xi[i] = 0;
			_hu_pai_max_hu[i] = 0;
			_xt_display_an_long[i] = false;
			_xian_ming_zhao[i] = false;
		}
		_provider_hu = GameConstants.INVALID_SEAT;
		_fang_pao_index = GameConstants.INVALID_SEAT;
		_xing_count = 0;
		_xing_card = new int[GameConstants.MAX_HH_COUNT];
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		_xing_card_player = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_COUNT];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;
			}
		}
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][9];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 9; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
			_xing_card_player[i] = new int[GameConstants.MAX_HH_COUNT];
			Arrays.fill(_xing_card_player[i], 0);
		}
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}

		// 胡牌信息
		// _hu_card_info = new HuCardInfo();
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des());
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
		}

		_xiao_jia = -1;
		_is_display = false;
		_zhao_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_must_zhao_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_zhao_guo_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_out_card_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_game_score = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Arrays.fill(_zhao_card[i], 0);
			Arrays.fill(_must_zhao_card[i], 0);
			Arrays.fill(_out_card_index[i], 0);
			Arrays.fill(_zhao_guo_card[i], 0);
			Arrays.fill(_game_score[i], 0);
		}
		_zhe_tian_card = false;
		_zhe_di_card = false;
		_zhe_ding_card = false;
		_zhe_fu_card = false;
		if (is_mj_type(GameConstants.GAME_TYPE_CP_DZHUI)) {
			_handler_dispath_card = new SCHCPHandlerDispatchCard_DAZHUI();
			_handler_out_card_operate = new SCHCPHandlerOutCardOperate_DAZHUI();
			_handler_gang = new SCHCPHandlerGang_DAZHUI();
			_handler_chi_peng = new SCHCPHandlerChiPeng_DAZHUI();
			_handler_dispath_add_card = new SCHCPHandlerDispatchAddCard_DAZHUI();
			_handler_chuli_firstcards = new SCHCPHandlerChuLiFirstCard_DAZHUI();
			_handler_dispath_firstcards = new SCHCPHandlerDispatchFirstCard_DAZHUI();
			_handler_dispatch_toucards = new SCHCPHandlerDispatchTouCard_DAZHUI();
			_handler_zhao = new SCHCPHandlerZhao_DAZHUI();
			_handler_dispath_card_xiao = new SCHCPHandlerXiaoDispatchCard_DAZHUI();
		}

		_handler_finish = new SCHCPHandlerFinish();
		this.setMinPlayerCount(this.getTablePlayerNumber());

	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int seat_index) {
		// TODO Auto-generated method stub

	}

	////////////////////////////////////////////////////////////////////////
	@Override
	public boolean reset_init_data() {

		if (_cur_round == 0) {

			if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
				// if (getTablePlayerNumber() != this.getTablePlayerNumber()) {
				// this.shuffle_players();
				//
				// for (int i = 0; i < getTablePlayerNumber(); i++) {
				// this.get_players()[i].setseat_index(i);
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
		}
		_is_di_hu = false;
		// 不能吃，碰
		_cannot_chi_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_cannot_peng_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][9];
		_guo_hu_xt = new int[this.getTablePlayerNumber()];
		_ti_mul_long = new int[this.getTablePlayerNumber()];
		_guo_hu_pai_count = new int[this.getTablePlayerNumber()];
		_ting_card = new boolean[this.getTablePlayerNumber()];
		_tun_shu = new int[this.getTablePlayerNumber()];
		_fan_shu = new int[this.getTablePlayerNumber()];
		_hu_code = new int[this.getTablePlayerNumber()];
		_liu_zi_fen = new int[this.getTablePlayerNumber()];
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_shang_xing_card = 0;
		_xia_xing_card = 0;
		_xing_player = new int[this.getTablePlayerNumber()];
		Arrays.fill(_xing_player, 0);
		_shang_xing_count = new int[this.getTablePlayerNumber()];
		_hu_xing_count = new int[this.getTablePlayerNumber()];
		_ben_xing_count = new int[this.getTablePlayerNumber()];
		_xia_xing_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_shang_xing_count, 0);
		Arrays.fill(_hu_xing_count, 0);
		Arrays.fill(_xia_xing_count, 0);
		Arrays.fill(_shang_xing_count, 0);
		_hu_pai_type = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_pai_type, 0);
		_ben_card = 0;
		_provider_hu = GameConstants.INVALID_SEAT;
		_fang_pao_index = GameConstants.INVALID_SEAT;
		_xing_count = 0;
		_xing_card = new int[GameConstants.MAX_CP_COUNT];
		_xing_card_player = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_COUNT];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			has_shoot[i] = false;
			is_hands_up[i] = false;
			player_ti_count[i][0] = 0;
			player_ti_count[i][1] = 0;
			is_wang_diao[i] = false;
			is_wang_diao_wang[i] = false;
			is_wang_chuang[i] = false;
		}
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			cards_has_wei[i] = 0;
		}
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 9; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
			_xing_card_player[i] = new int[GameConstants.MAX_HH_COUNT];
			Arrays.fill(_xing_card_player[i], 0);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_da_pai_count[i] = 0;
			_xiao_pai_count[i] = 0;
			_guo_hu_xt[i] = -1;
			_ti_mul_long[i] = 0;
			_guo_hu_pai_count[i] = 0;
			_ting_card[i] = false;
			_tun_shu[i] = 0;
			_fan_shu[i] = 0;
			_hu_code[i] = GameConstants.WIK_NULL;
			_liu_zi_fen[i] = 0;
			_hu_xi[i] = 0;
			_hu_pai_max_hu[i] = 0;
			_xt_display_an_long[i] = false;
			_xian_ming_zhao[i] = false;

		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;

			}
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

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_CP_WEAVE, GameConstants.MAX_CP_COUNT, GameConstants.MAX_CP_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_CP_COUNT);
		}

		_cur_round++;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}
		_xiao_jia = -1;
		_is_display = false;
		_zhao_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_must_zhao_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_zhao_guo_card = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		_out_card_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Arrays.fill(_zhao_card[i], 0);
			Arrays.fill(_must_zhao_card[i], 0);
			Arrays.fill(_out_card_index[i], 0);
			Arrays.fill(_zhao_guo_card[i], 0);
		}
		_zhe_tian_card = false;
		_zhe_di_card = false;
		_zhe_ding_card = false;
		_zhe_fu_card = false;
		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		if (commonGameRuleProtos != null)
			GRR._room_info.setNewRules(commonGameRuleProtos);

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

		GRR._video_recode.setBankerPlayer(this._cur_banker);

		return true;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {

		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		if (this._cur_round == 1) {
			this._cur_banker = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber();
		} else {
			this._cur_banker = (this._cur_banker + 1) % this.getTablePlayerNumber();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_CP_DZHUI)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_CP_SCH];
			shuffle(_repertory_card, GameConstants.CARD_DATA_CP_SCH);
		}

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		if (is_mj_type(GameConstants.GAME_TYPE_CP_DZHUI)) {
			return game_start_cp();
		}

		return false;
	}

	private boolean game_start_cp() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_CP_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_CP_COUNT; j++) {
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
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

			for (int j = 0; j < GameConstants.MAX_CP_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);

		this.GRR.add_room_response(roomResponse);
		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		// this._handler = this._handler_dispath_firstcards;
		// this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL,
		// FlashTime + standTime);
		this._handler = this._handler_chuli_firstcards;
		this.exe_chuli_first_card(this._cur_banker, GameConstants.WIK_NULL, 2500);
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
	public void shuffle(int repertory_card[], int mj_cards[]) {
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

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		_xiao_jia = (this._cur_banker - 1 + this.getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_CP_COUNT - 1);
			if (_xiao_jia == i)
				send_count = 5;
			if (this._cur_banker == i)
				send_count = GameConstants.MAX_CP_COUNT;

			GRR._left_card_count -= send_count;
			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}
		// test_cards();
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));

	}

	public void test_cards() {

		// int[] realyCards = new int[] {
		// 216, 70, 18, 248, 67, 10, 19, 21, 6, 7, 19, 11, 10, 70, 67, 10, 19,
		// 9, 18, 69,
		// 32, 32, 4, 80, 22, 19, 9, 9, 67, 80, 21, 128, 6, 102, 11, 18, 69, 5,
		// 80, 66, 22,
		// 21, 216, 248, 69, 22, 11, 18, 128, 248, 248, 5, 80, 7, 216, 102, 66,
		// 70, 4, 6, 69,
		// 66, 7, 70, 216, 6, 128, 4, 67, 5, 32, 102, 128, 7, 4, 21, 5, 66, 10,
		// 102, 9, 22, 32, 11
		//
		// };
		// this._cur_banker = 0;
		// testRealyCard(realyCards);

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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_CP_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;
		_xiao_jia = (this._cur_banker - 1 + this.getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_CP_COUNT - 1);
			if (_xiao_jia == i)
				send_count = 5;
			if (this._cur_banker == i)
				send_count = GameConstants.MAX_CP_COUNT;

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
			for (int j = 0; j < GameConstants.MAX_CP_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		_xiao_jia = (this._cur_banker - 1 + this.getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = (GameConstants.MAX_CP_COUNT - 1);
			if (_xiao_jia == i)
				send_count = 5;
			if (this._cur_banker == i)
				send_count = GameConstants.MAX_CP_COUNT;

			GRR._left_card_count -= send_count;
			// 一人20张牌,庄家多一张
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

		// 发牌
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public boolean dispatch_card_data_xiao(int cur_player, int type, boolean tail) {
		// 发牌
		this._handler = this._handler_dispath_card_xiao;
		this._handler_dispath_card_xiao.reset_status(cur_player, type);
		this._handler.exe(this);
		return false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_add_data(int cur_player, int type, boolean tail) {

		// 发牌
		this._handler = this._handler_dispath_add_card;
		this._handler_dispath_add_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	public boolean hand_card_update_data(int cur_player, int card, boolean tail) {
		int cards[] = new int[GameConstants.MAX_CP_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[cur_player], cards);
		operate_player_cards(cur_player, hand_card_count, cards, GRR._weave_count[cur_player], GRR._weave_items[cur_player]);
		operate_player_get_card(cur_player, 0, null, GameConstants.INVALID_SEAT, false);

		return true;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		ret = this.handler_game_finish_schcp(seat_index, reason);

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean handler_game_finish_schcp(int seat_index, int reason) {
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
		PukeGameEndSchcp.Builder game_end_schcp = PukeGameEndSchcp.newBuilder();
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

			game_end.setWinLziFen(this._provider_hu); // 1自摸，3放炮，4犯规
			game_end.setEspecialTxtType(this._fang_pao_index);
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
			// 设置中鸟数据
			for (int i = 0; i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_CP_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (seat_index != -1) {
				game_end.setTunShu(this._tun_shu[seat_index]);
				game_end.setFanShu(this._fan_shu[seat_index]);
				game_end.setCountPickNiao(this._hu_pai_type[seat_index]);//
				roomResponse.setHuXiCount(this._hu_xi[seat_index]);
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				int zhao_cards[] = new int[GameConstants.MAX_CP_COUNT];
				int zhao_count = _logic.switch_to_cards_data(this._zhao_guo_card[i], zhao_cards);
				Int32ArrayResponse.Builder Array_zhao = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < zhao_count; j++) {

					Array_zhao.addItem(zhao_cards[j]);
				}
				game_end_schcp.addZhaoCard(Array_zhao);
				game_end.addCardsData(cs);// 牌
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				// if (GRR._chi_hu_card[i][0] == 0 || reason !=
				// GameConstants.Game_End_NORMAL) {
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				// } else {
				//
				// if (_hu_weave_count[i] > 0) {
				// for (int j = 0; j < _hu_weave_count[i]; j++) {
				// WeaveItemResponse.Builder weaveItem_item =
				// WeaveItemResponse.newBuilder();
				// weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
				// weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
				// weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
				// weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
				// weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
				// weaveItem_array.addWeaveItem(weaveItem_item);
				// }
				// }
				// }
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

				// Int32ArrayResponse.Builder lfs =
				// Int32ArrayResponse.newBuilder();
				// for (int j = 0; j < count; j++) {
				// lfs.addItem(GRR._lost_fan_shu[i][j]);
				// }

				// game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
				game_end_schcp.setPlayerResult(this.process_player_result_schcp(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
			game_end_schcp.setPlayerResult(this.process_player_result_schcp(reason));
		}
		game_end.setEndType(real_reason);
		game_end.setCommResponse(PBUtil.toByteString(game_end_schcp));
		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

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

	public boolean handler_game_finish_bayi(int seat_index, int reason) {
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

			game_end.setWinLziFen(this._provider_hu);
			game_end.setEspecialTxtType(this._fang_pao_index);
			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				_player_result.game_score[i] += GRR._game_score[i];
				this._game_score[i][this._cur_round - 1] = (int) GRR._game_score[i];

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

			// 醒牌个数
			for (int i = 0; i < this._xing_count; i++) {
				game_end.addGangCount(this._xing_card[i]);
			}
			// game_end.setCountPickNiao(GRR._count_pick_niao);//

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_CP_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if (GRR._chi_hu_card[i][0] == 0 || reason != GameConstants.Game_End_NORMAL) {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						if (j < GRR._weave_count[i]) {
							weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						} else {
							weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
						}

						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				} else {

					if (_hu_weave_count[i] > 0) {
						for (int j = 0; j < _hu_weave_count[i]; j++) {
							WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
							weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
							weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
							weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
							weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
							weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
							weaveItem_array.addWeaveItem(weaveItem_item);
						}
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

				lfs.addItem(this._ben_xing_count[i]);
				lfs.addItem(this._shang_xing_count[i]);
				lfs.addItem(this._xia_xing_count[i]);
				lfs.addItem(this._hu_xing_count[i]);
				game_end.addLostFanShu(lfs);
				game_end.addCardsDataNiao(this._hu_pai_type[i]);

				game_end.addPao(this._tun_shu[i]);
				game_end.addQiang(this._fan_shu[i]);
				Int32ArrayResponse.Builder xing_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this._xing_count; j++) {
					if (this._xing_card_player[i][j] != 0)
						xing_card.addItem(this._xing_card_player[i][j]);
				}
				game_end.addPlayerNiaoCards(xing_card);

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
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

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

	/**
	 * @return
	 */
	@Override
	public RoomInfo.Builder getRoomInfo() {
		RoomInfo.Builder room_info = encodeRoomBase();

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
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_CP_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);

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
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

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

	public boolean handler_game_finish_phz_chd(int seat_index, int reason) {
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
			int cards[] = new int[GRR._left_card_count];
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
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_CP_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (this.has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					this._huang_zhang_count++;
				} else {
					this._huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
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
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

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

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {
		if (this._ti_two_long[seat_index] == true) {
			return 0;
		}

		if (GameConstants.GAME_TYPE_CP_DZHUI == _game_type_index) {
			return analyse_chi_hu_card_cp_dzhui(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
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
	public int analyse_chi_hu_card_cp_dzhui(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index,
			int cur_card, ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (((cur_card == 0x43 || cur_card == 0x16) && seat_index != this._xiao_jia)) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克

		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				dispatch);

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_pai_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;

		temp_hu_xi = 0;
		analyseItem = analyseItemArray.get(0);
		int d_h_count = 0;
		for (int j = 0; j < 9; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			WeaveItem weave_items = new WeaveItem();
			weave_items.center_card = analyseItem.cbCenterCard[j];
			weave_items.weave_kind = analyseItem.cbWeaveKind[j];
			temp_hu_xi += analyseItem.hu_xi[j];
			hong_pai_count += _logic.get_hong_pai_count(weave_items.weave_kind, weave_items.center_card);
			hei_pai_count += _logic.get_hei_pai_count(weave_items.weave_kind, weave_items.center_card);
			if (analyseItem.hu_xi[j] == 1 && (weave_items.center_card == 0x12 || weave_items.center_card == 0x0b))
				d_h_count++;

		}
		all_pai_count = hong_pai_count + hei_pai_count;
		if (seat_index == this._cur_banker) {
			if (temp_hu_xi < 20) {
				chiHuRight.set_empty();
				return GameConstants.CP_WIK_NULL;
			}
		} else if (seat_index == this._xiao_jia) {
			int dot = _logic.get_dot(cur_card);
			boolean flag = false;
			if (cur_card == 0x07 && seat_index != provider_index && hong_pai_count == 0) {
				flag = true;
			}
			if (temp_hu_xi < 5 && dot == 7 && cur_card != 0x07) {

				if (hei_pai_count == 5 && hong_pai_count == 1 && (seat_index == provider_index)) {
					flag = true;
				}
			}
			if (hei_pai_count == 6 && hong_pai_count == 0 && dot != 7) {
				flag = true;
			}
			if ((d_h_count == 1 && (hong_pai_count >= 3 || hei_pai_count >= 3)) && temp_hu_xi < 5) {
				flag = true;
			}
			if (flag == false && dot == 7 && cur_card == 0x07) {
				chiHuRight.set_empty();
				return GameConstants.CP_WIK_NULL;
			} else if ((flag == false && temp_hu_xi < 5) || (flag == false && cur_card == 0x07)) {
				chiHuRight.set_empty();
				return GameConstants.CP_WIK_NULL;
			}
		} else {
			if (temp_hu_xi < 18) {
				chiHuRight.set_empty();
				return GameConstants.CP_WIK_NULL;
			}
		}

		analyseItem = analyseItemArray.get(0);
		for (int j = 0; j < 9; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = analyseItem.hu_xi[j];
			_hu_weave_count[seat_index] = j + 1;

		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_CP_JEI_PAO_HU);
		} else if (dispatch == false) {
			chiHuRight.opr_or(GameConstants.CHR_CP_DIAN_PAO_HU);
		}
		if (seat_index == this._xiao_jia) {
			if (hong_pai_count == 0)
				chiHuRight.opr_or(GameConstants.CHR_CP_ALL_HEI);
			else if (hei_pai_count == 0)
				chiHuRight.opr_or(GameConstants.CHR_CP_ALL_HONG);
		}
		// if (hong_pai_count >= 10 && hong_pai_count < 13) {
		// chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
		// }
		// if (hong_pai_count >= 13) {
		// chiHuRight.opr_or(GameConstants.CHR_THIRTEEN_HONG_PAI);
		// }
		// if (hong_pai_count == 1) {
		// chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
		// }
		// if (hei_pai_count == all_cards_count) {
		// chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
		// }
		if ((_is_di_hu == true) && (GRR._banker_player == provider_index) && (dispatch == false)) {

			chiHuRight.opr_or(GameConstants.CHR_DI_HU);
		}

		return cbChiHuKind;
	}

	// 玩家出版的动作检测 内滑
	public int estimate_player_hua(int seat_index, int card_data) {
		int bAroseAction = GameConstants.CP_WIK_NULL;
		// 内滑判断
		if ((bAroseAction == GameConstants.CP_WIK_NULL)
				&& (_logic.check_nei_hua(this.GRR._cards_index[seat_index], card_data) != GameConstants.CP_WIK_NULL)) {
			int action = GameConstants.CP_WIK_HUA;
			// for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
			// if (card_data == this._cannot_peng[seat_index][i]) {
			// action = GameConstants.WIK_CHOU_SAO;
			// }
			// }
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.CP_TYPE_HUA, true, true, false, 1000);
			bAroseAction = GameConstants.CP_WIK_HUA;
		}
		return bAroseAction;
	}

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_glzp(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
		// 变量定义
		int bAroseAction = GameConstants.WIK_NULL;// 出现(是否)有

		//
		return bAroseAction;
	}

	// 玩家 动作召检测
	public int estimate_player_zhao(int seat_index, int card_data) {
		if (seat_index == this._xiao_jia)
			return GameConstants.CP_WIK_NULL;
		int bAroseAction = GameConstants.CP_WIK_NULL;
		if (this._must_zhao_card[seat_index][_logic.switch_to_card_index(card_data)] == 1) {
			_playerStatus[seat_index].add_action(GameConstants.CP_WIK_ZHAO);
			_playerStatus[seat_index].add_cp_zhao(card_data, GameConstants.CP_WIK_ZHAO, seat_index);

			bAroseAction = GameConstants.CP_WIK_ZHAO;
			return bAroseAction;
		}

		_playerStatus[seat_index].add_action(GameConstants.CP_WIK_ZHAO);
		_playerStatus[seat_index].add_cp_zhao(card_data, GameConstants.CP_WIK_ZHAO, seat_index);
		_playerStatus[seat_index].add_action(GameConstants.CP_WIK_NULL);
		_playerStatus[seat_index].add_pass(card_data, seat_index);

		bAroseAction = GameConstants.CP_WIK_ZHAO;
		return bAroseAction;

	}

	public boolean estimate_player_chi(int seat_index, int card, boolean bDisdatch) {
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

		do {

			int chiseat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chiseat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
			next_index++;

			if (chiseat_index == this._xiao_jia)
				continue;
			playerStatus = this._playerStatus[chiseat_index];
			int chi_index = 0;
			if (this._cannot_chi_index[chiseat_index][_logic.switch_to_card_index(card)] != 0)
				continue;
			int card_count = 0;

			for (int j = 0; j < GameConstants.MAX_CP_INDEX; j++) {
				if (this.GRR._cards_index[chiseat_index][j] < 2)
					card_count += this.GRR._cards_index[chiseat_index][j];
			}
			if (card_count == 1)
				continue;

			if (_playerStatus[chiseat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				int cards_count = 0;
				int cards[] = new int[3];
				int card_dot = _logic.get_dot(card);
				cards_count = _logic.switch_to_value_to_card(14 - card_dot, cards);
				for (int i = 0; i < cards_count; i++) {
					int cards_data[] = new int[2];
					int cards_data_count = 0;
					cards_data[cards_data_count++] = card;
					cards_data[cards_data_count++] = cards[i];
					action = _logic.get_chi_action(card, cards[i]);
					if (card_dot == 7) {
						if (this.GRR._cards_index[chiseat_index][_logic.switch_to_card_index(cards[i])] == 2)
							continue;
					}
					if (this.GRR._cards_index[chiseat_index][_logic.switch_to_card_index(cards[i])] > 0) {
						playerStatus.add_action(action);
						playerStatus.add_cp_chi(card, action, seat_index);
						player_pass[chiseat_index] = 1;
						bAroseAction = true;
					}

				}

			}
		} while (next_index < 2);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			playerStatus = _playerStatus[i];
			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_CP_INDEX; j++) {

				if (this.GRR._cards_index[i][j] < 3)
					card_count += this.GRR._cards_index[i][j];
			}

			if (card_count == 2)
				continue;

			if (this._cannot_peng_index[i][_logic.switch_to_card_index(card)] != 0)
				continue;

			if (_playerStatus[i].lock_huan_zhang() == false) {
				action = _logic.check_che(GRR._cards_index[i], card);
				if (action != 0) {

					if (_logic.get_dot(card) == 7) {
						player_pass[i] = 0;
						playerStatus.clean_action();
						playerStatus.clean_status();
						playerStatus.add_action(action);
						playerStatus.add_cp_peng(card, GameConstants.CP_WIK_CHE, seat_index);
						bAroseAction = true;

					} else {
						playerStatus.add_action(action);
						playerStatus.add_cp_peng(card, GameConstants.CP_WIK_CHE, seat_index);
						bAroseAction = true;
						player_pass[i] = 1;
					}

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

	public void exe_dispatch_add_card(int seat_index) {
		int discard_time = 2000;
		int gameId = getGame_id() == 0 ? 5 : getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
			discard_time = sysParamModel1104.getVal4();
		}
		int dispatch_time = 3000;
		SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
			dispatch_time = sysParamModel1105.getVal2();
		}
		exe_dispatch_add_card(seat_index, GameConstants.WIK_NULL, dispatch_time);
	}

	// 检查杠牌,有没有胡的
	public boolean estimate_gang_respond_hh(int seat_index, int card, int _action) {
		// 变量定义
		boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张
																			// 不算抢杠胡

		boolean bAroseAction = false;// 出现(是否)有

		return bAroseAction;
	}

	// 检查长沙麻将,杠牌
	public boolean estimate_gang_hh_respond(int seat_index, int provider, int card, boolean d, boolean check_chi) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		return bAroseAction;
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
		int count = this.getTablePlayerNumber();
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
			playerNumber = count;
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		// this.log_info("gme_status:" + this._game_status + " seat_index:" +
		// seat_index);
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			// this.log_info("gme_status:" + this._game_status + "GS_MJ_WAIT
			// seat_index:" + seat_index);
			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

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
		if (this._cur_round > 0 && this._player_ready[seat_index] == 0)
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
	@Override
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

		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);

		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			switch (this._hu_weave_items[seat_index][i].weave_kind) {
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_TUO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_DUI_ZI:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_LEFT:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card + 2 == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_CENTER:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_RIGHT:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card - 2 == operate_card) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_EQS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(this._hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 2) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 7) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_YWS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(this._hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 1) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 5) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
				if (_logic.get_card_value(operate_card) == _logic.get_card_value(this._hu_weave_items[seat_index][i].center_card)) {
					this._hu_pai_type[seat_index] = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			}
		}
		// 手牌删掉
		// this.operate_player_cards(seat_index, 0, null, 0, null);

		// if (rm) {
		// // 把摸的牌从手牌删掉,结算的时候不显示这张牌的
		// GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		// }

		// 显示胡牌
		if (this.getRuleValue(GameConstants.GAME_RULE_YPSHX) == 1)
			return;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i], this.GRR._weave_count[i],
					GameConstants.INVALID_SEAT);

		}
		return;
	}

	public void process_chi_hu_player_operate_lhq(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 效果
		// 手牌删掉
		// this.operate_player_cards(seat_index, 0, null, 0, null);

		// if (rm) {
		// // 把摸的牌从手牌删掉,结算的时候不显示这张牌的
		// GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		// }

		// 显示胡牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i], this.GRR._weave_count[i],
					GameConstants.INVALID_SEAT);

		}
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);
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
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			for (int i = 0; i < card_count; i++) {
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card[i])]--;
			}
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_CP_INDEX];
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
	 * @param seat_index
	 */
	public void countChiHuTimes_lhq(int target_player, int seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[target_player];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)) {
			_player_result.hu_pai_count[target_player]++;
			_player_result.ying_xi_count[target_player] += this._hu_xi[target_player];

		}
		if (isZimo == false) {
			if (!(chiHuRight.opr_and(GameConstants.CHR_DIAN_PAO_HU)).is_empty()) {
				_player_result.ming_tang_count[seat_index]++;
			}
		}
		return;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param seat_index
	 */
	public void countChiHuTimes_wmq(int target_player, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[target_player];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
			_player_result.liu_zi_fen[i] = this._liu_zi_fen[i];
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)) {
			_player_result.hu_pai_count[target_player]++;
			_player_result.ying_xi_count[target_player] += this._hu_xi[target_player];

		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUI_ZI_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIAN_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HONG_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DUO_HONG_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_YING_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_CHUN_YING_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_WU_DUI_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DA_ZI_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIAO_ZI_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHUO_FU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DIA_SHUN_ZHUO)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DS_DIA_TUO_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_HAI_DI_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_DAN_DI_DZ_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BA_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIA_BA_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BEI_KAO_BEI)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SHOU_QIAN_SHOU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_QUAN_QIU_REN_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_WEI_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LONG_BAI_WEI_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_XIANG_DUI_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}

		if (!(chiHuRight.opr_and_long(GameConstants.CHR_PIAO_DUI_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JI_DING_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_TIAN_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ALL_HEI_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_LDH_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_JIU_DUI_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_SBD_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_BIAN_KAN_HU)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHEN_BKB_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_KA_HU_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_ZHA_DAN_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}
		if (!(chiHuRight.opr_and_long(GameConstants.CHR_FBW_WMQ)).is_empty()) {
			_player_result.ming_tang_count[target_player]++;
		}

		return;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param seat_index
	 */
	public void countChiHuTimes(int seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		_player_result.hu_pai_count[seat_index]++;
		if (isZimo) {

			_player_result.ying_xi_count[seat_index] += this._hu_xi[seat_index];
		} else if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
			_player_result.hu_pai_count[seat_index]++;
			_player_result.ying_xi_count[seat_index] += this._hu_xi[seat_index];
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HEI)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI)).is_empty()) {
			_player_result.ming_tang_count[seat_index]++;
		}

	}

	public void countChiHuTimes_bayi(int seat_index, boolean isZimo) {
		_player_result.hu_pai_count[seat_index]++;
		_player_result.ying_xi_count[seat_index] += this._hu_xi[seat_index];
		if (isZimo) {
			_player_result.ming_tang_count[seat_index]++;
		}

	}

	public boolean no_card_out_game_end(int seat_index, int operate_card) {

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i], this.GRR._weave_count[i],
					GameConstants.INVALID_SEAT);

		}
		_fang_pao_index = seat_index;
		this._provider_hu = 4; // 犯规胡牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index)
				continue;
			GRR._game_score[i] = 16;
			GRR._game_score[seat_index] -= 16;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._game_score[i][this._cur_round - 1] = (int) GRR._game_score[i];
		}
		countChiHuTimes(seat_index, true);
		int delay = GameConstants.GAME_FINISH_DELAY_FLS;

		GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

		return true;
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_schcp(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		_fang_pao_index = provide_index;
		if (zimo == false) {
			this._provider_hu = 3; // 点炮是三
		} else
			this._provider_hu = 1; // 自摸
		// else
		// this._provider_hu = 2; // 平胡
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
		// 计算胡息
		int all_hu_xi = 0;
		int wFanShu = 0;// 番数
		int calculate_score = 1;
		// for(int i =0; i< this.GRR._weave_count[seat_index] ; i++){
		// all_hu_xi += this.GRR._weave_items[seat_index][i].hu_xi;
		// }
		if (!(chr.opr_and(GameConstants.CHR_CP_ALL_HEI)).is_empty()) {
			wFanShu++;
		}
		if (!(chr.opr_and(GameConstants.CHR_CP_ALL_HONG)).is_empty()) {
			wFanShu++;
		}

		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
			switch (this._hu_weave_items[seat_index][i].weave_kind) {
			case GameConstants.CP_WIK_LONG: {
				if (seat_index == this._xiao_jia) {
					wFanShu += 2;
					if (_logic.get_times_cards(this._hu_weave_items[seat_index][i].center_card) > 0)
						wFanShu++;
				} else if (_logic.get_times_cards(this._hu_weave_items[seat_index][i].center_card) > 0) {
					wFanShu += 2;
				}
				break;
			}
			case GameConstants.CP_WIK_CHE:
			case GameConstants.CP_WIK_KAN:
			case GameConstants.CP_WIK_HUA: {
				if (seat_index == this._xiao_jia) {
					wFanShu += 1;
					if (_logic.get_times_cards(this._hu_weave_items[seat_index][i].center_card) > 0)
						wFanShu++;
				} else if (_logic.get_times_cards(this._hu_weave_items[seat_index][i].center_card) > 0) {
					wFanShu += 1;
				}
				break;
			}
			}
		}
		this._hu_xi[seat_index] = all_hu_xi;

		if (seat_index == this._cur_banker) {
			if (this._hu_xi[seat_index] >= 32)
				wFanShu++;
			if (wFanShu >= 4)
				wFanShu = 4;
		} else if (seat_index == this._xiao_jia) {
			if (this._hu_xi[seat_index] >= 10)
				wFanShu++;
			if (wFanShu >= 3)
				wFanShu = 3;
		} else {
			if (this._hu_xi[seat_index] >= 30)
				wFanShu++;
			if (wFanShu >= 4)
				wFanShu = 4;
		}

		this._fan_shu[seat_index] = wFanShu;
		wFanShu = (int) Math.pow(2, wFanShu);
		if (wFanShu == 0)
			wFanShu = 1;
		calculate_score = wFanShu * calculate_score;
		if (has_rule(GameConstants.GAME_RULE_SCHCP_TWO_FEN))
			calculate_score *= 2;
		float lChiHuScore = calculate_score;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				if (seat_index == this._cur_banker) {
					GRR._game_score[i] -= lChiHuScore * 2;
					GRR._game_score[seat_index] += lChiHuScore * 2;
				} else if (i == this._cur_banker) {
					GRR._game_score[i] -= lChiHuScore * 2;
					GRR._game_score[seat_index] += lChiHuScore * 2;
				} else {
					GRR._game_score[i] -= lChiHuScore;
					GRR._game_score[seat_index] += lChiHuScore;
				}

			}
		} else {
			// 胡牌分
			if (seat_index == this._cur_banker) {

				lChiHuScore *= (this.getTablePlayerNumber() - 1);
				GRR._game_score[provide_index] -= lChiHuScore * 2;
				GRR._game_score[seat_index] += lChiHuScore * 2;
			} else if (provide_index == this._cur_banker) {
				lChiHuScore *= (this.getTablePlayerNumber() - 1);
				lChiHuScore += calculate_score; // 庄家多出一倍
				GRR._game_score[provide_index] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			} else {
				lChiHuScore *= (this.getTablePlayerNumber() - 1);
				lChiHuScore += calculate_score;// 庄家多出一倍
				GRR._game_score[provide_index] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}

		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._game_score[i][this._cur_round - 1] = (int) GRR._game_score[i];
		}
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
	public boolean operate_show_card(int seat_index, int type, int count, int cards[], WeaveItem weaveitems[], int weave_count, int to_player) {
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
		if ((this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)) && GRR != null) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				roomResponse.addScore(GRR._game_score[i]);
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

	// 不能出的牌
	public boolean operate_cannot_card(int seat_index, boolean bDisplay) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_CP_COUNT];
		// 刷新自己手牌
		int count = _logic.switch_to_cards_data(GRR._cannot_out_index[seat_index], cards);
		roomResponse.setType(MsgConstants.RESPONSE_CANNOT_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		if (bDisplay == true)
			GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	// 必须出牌
	public boolean operate_must_out_card(int seat_index, boolean bDisplay) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_CP_COUNT];
		// 刷新自己手牌
		int count = _logic.switch_to_cards_data(GRR._must_out_index[seat_index], cards);
		roomResponse.setType(MsgConstants.RESPONSE_MUST_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			roomResponse.addCardData(cards[i]);
		}
		if (bDisplay == true)
			GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);

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
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public boolean operate_dou_liu_zi(int seat_index, boolean win, int deng_shu) {
		if (this.has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_OFF))
			return true;
		int action = 0;
		if (win == false) {

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.has_rule(GameConstants.GAME_RULE_ZHX_ONE_FEN)) {
					if (i == GRR._banker_player)
						this._liu_zi_fen[i] = -30;
					else
						this._liu_zi_fen[i] = -20;
					this._zong_liu_zi_fen += -this._liu_zi_fen[i];
				}
				if (this.has_rule(GameConstants.GAME_RULE_ZHX_TWO_FEN)) {

					if (i == GRR._banker_player)
						this._liu_zi_fen[i] = -40;
					else
						this._liu_zi_fen[i] = -30;
					this._zong_liu_zi_fen += -this._liu_zi_fen[i];
				}
				if (this.has_rule(GameConstants.GAME_RULE_ZHX_THREE_FEN)) {
					if (i == GRR._banker_player)
						this._liu_zi_fen[i] = -50;
					else
						this._liu_zi_fen[i] = -40;
					this._zong_liu_zi_fen += -this._liu_zi_fen[i];

				}

				_player_result.game_score[i] += this._liu_zi_fen[i];
			}

			action = GameConstants.LZ_DOU_LZ;

		} else {
			int deng_fen = 0;
			if (this.has_rule(GameConstants.GAME_RULE_ONE_DENG)) {
				deng_fen = 200;
			}
			if (this.has_rule(GameConstants.GAME_RULE_TWO_DENG)) {
				deng_fen = 300;
			}
			if (this.has_rule(GameConstants.GAME_RULE_THREE_DENG)) {
				deng_fen = 450;
			}
			if (this._zong_liu_zi_fen > (deng_fen * deng_shu)) {
				this._liu_zi_fen[seat_index] = deng_fen * deng_shu;
				this._zong_liu_zi_fen -= deng_fen * deng_shu;
			} else {
				this._liu_zi_fen[seat_index] = this._zong_liu_zi_fen;
				this._zong_liu_zi_fen = 0;
			}
			_player_result.game_score[seat_index] += this._liu_zi_fen[seat_index];
			_all_lz_fen = this._liu_zi_fen[seat_index];
			action = GameConstants.LZ_WIN_LZ;

		}
		if (this._cur_round == this._game_round) {
			int score = this._zong_liu_zi_fen / 3;
			int max = 0;
			for (int i = 1; i < this.getTablePlayerNumber(); i++) {
				if (_player_result.game_score[max] < _player_result.game_score[i])
					max = i;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == max)
					this._liu_zi_fen[i] = score + this._zong_liu_zi_fen % 3;
				else
					this._liu_zi_fen[i] = score;
				_player_result.game_score[seat_index] += this._liu_zi_fen[i] = score;
			}
			action = GameConstants.LZ_FEN_LZ;

		}

		// 效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_LIU_ZI, 1, new long[] { action }, 1, GameConstants.INVALID_SEAT);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DOU_LIU_ZI);
		roomResponse.setZongliuzi(this._zong_liu_zi_fen);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			roomResponse.addDouliuzi(this._liu_zi_fen[i]);

		}
		this.load_player_info_data(roomResponse);
		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
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
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_CP_INDEX];
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_CP_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, true)) {
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
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
					| GameConstants.WIK_EQS | GameConstants.WIK_YWS;
			// this.log_error("weave.kind" +
			// curPlayerStatus._action_weaves[i].weave_kind + "center_card"
			// + curPlayerStatus._action_weaves[i].center_card);
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {
							// this.log_error("lou_qi_weave.kind" +
							// curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);

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
		operate_player_get_card(seat_index, count, cards, to_player, sao, 0);
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
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player, boolean sao, int is_add) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);
		roomResponse.setKindType(is_add); // 0不加到手牌 1加到手牌
		roomResponse.setFlashTime(150);
		roomResponse.setStandTime(1000);
		roomResponse.setInsertTime(150);

		if (sao == true && has_rule(GameConstants.GAME_RULE_DI_AN_WEI))
			roomResponse.setEffectType(100);
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
			if (seat_index != -1)
				this.send_response_to_player(seat_index, roomResponse);
			return true;

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
				// GRR.add_room_response(roomResponse);
				return this.send_response_to_player(seat_index, roomResponse);
			} else {
				if (sao == true) {
					for (int i = 0; i < count; i++) {
						roomResponse.addCardData(GameConstants.BLACK_CARD);// 给别人
																			// 牌背
					}
				} else {
					for (int i = 0; i < count; i++) {
						roomResponse.addCardData(cards[i]);// 给别人 牌数据
					}
				}
				// GRR.add_room_response(roomResponse);
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
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);

				if ((weaveitems[j].weave_kind == GameConstants.CP_WIK_LONG || weaveitems[j].weave_kind == GameConstants.CP_WIK_KAN)
						&& this._is_display == false) {
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
		roomResponse.setHuXiCount(this._hu_xi[seat_index]);
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

	/**
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		// roomResponse.setTarget(seat_index);
		//
		// for (int i = 0; i < count; i++) {
		// roomResponse.addChiHuCards(cards[i]);
		// }
		//
		// this.send_response_to_player(seat_index, roomResponse);
		// this.GRR.add_room_response(roomResponse);
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

	@Override
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
		tableResponse.setSendCardData(((_send_card_data != GameConstants.INVALID_VALUE) && (_provide_player == seat_index)) ? _send_card_data
				: GameConstants.INVALID_VALUE);
		int hand_cards[] = new int[GameConstants.MAX_CP_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.MAX_CP_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index, false);
		} else {
			if (_out_card_player != GameConstants.INVALID_SEAT && _out_card_data != GameConstants.INVALID_VALUE) {
				this.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, seat_index);
			} else if (_status_cs_gang == true) {
				this.operate_out_card(this._provide_player, 2, this._gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_MID, seat_index);
			}
		}

		if (_playerStatus[seat_index].has_action()) {
			this.operate_player_action(seat_index, false);
		}

		return true;

	}

	public PlayerResultResponse.Builder process_player_result(int reason) {
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
			player_result.addLiuZiFen(_player_result.liu_zi_fen[i]);

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

	public PlayerResultSchcp.Builder process_player_result_schcp(int reason) {

		PlayerResultSchcp.Builder player_result = PlayerResultSchcp.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			Int32ArrayResponse.Builder game_score = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < this._cur_round; j++) {
				game_score.addItem(this._game_score[i][j]);
			}
			player_result.addScore(game_score);
			player_result.addPlayerScore((int) _player_result.game_score[i]);
		}

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
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
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
	 * //处理首牌
	 * 
	 * @param seat_index
	 * @param delay_time
	 * @return
	 */
	public boolean exe_chuli_first_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new ChulifirstCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_chuli_firstcards;
			this._handler_chuli_firstcards.reset_status(seat_index, type);
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
	public boolean exe_Dispatch_tou_card_data(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchTouCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			this._handler = this._handler_dispatch_toucards;
			this._handler_dispatch_toucards.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public boolean runnable_Dispatch_tou_card_data(int seat_index, int type, boolean tail) {
		// 发牌
		this._handler = this._handler_dispatch_toucards;
		this._handler_dispatch_toucards.reset_status(seat_index, type);
		this._handler.exe(this);
		return false;
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

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	@Override
	public void log_info(String info) {

		logger.info("房间[" + this.getRoom_id() + "]" + info);

	}

	public void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtianhu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhyidianhong, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhonghu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhongfantian, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhallhei, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhaihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtinghu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdahu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhxiaohu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhduizihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhshuahou, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtuanyuan, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhuangfan, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhanghangxi, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_MAO_HU).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhmaohu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_QI_SHOU_HU).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhqishouhu, "", 0, 0l, this.getRoom_id());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_glzp() {

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (has_rule(GameConstants.GAME_RULE_ZM_FAN_BEI)) {
						if (type == GameConstants.CHR_ZI_MO) {

							des += ",自摸番2倍";
						}
					} else {
						if (type == GameConstants.CHR_ZI_MO) {

							des += ",自摸加一子";
						}
					}
					if (type == GameConstants.CHR_TIAN_HU) {
						des += ",天胡";
						if (has_rule(GameConstants.GAME_RULE_ZM_FAN_BEI)) {

							des += ",自摸番2倍";

						} else {
							des += ",自摸加一子";

						}
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡番2倍";
					}
					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海底加两醒";
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_ba_yi() {

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (has_rule(GameConstants.GAME_RULE_ZM_FAN_BEI)) {
						if (type == GameConstants.CHR_ZI_MO) {

							des += ",自摸番2倍";
						}
					} else {
						if (type == GameConstants.CHR_ZI_MO) {

							des += ",自摸加一子";
						}
					}
					if (type == GameConstants.CHR_TIAN_HU) {
						des += ",天胡";
						if (has_rule(GameConstants.GAME_RULE_ZM_FAN_BEI)) {

							des += ",自摸番2倍";

						} else {
							des += ",自摸加一子";

						}
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡";
					}
					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海底加3子";
					}

				}
			}

			GRR._result_des[i] = des;
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
					if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
						if (type == GameConstants.CHR_JEI_PAO_HU) {
							des += ",平胡";
						}
					}
					if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
						if (type == GameConstants.CHR_JEI_PAO_HU) {
							des += ",平胡";
						}
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸  +3硬息";
						}

					}
					if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
						if (type == GameConstants.CHR_DI_HU) {
							des += ",地胡";
						}
					}
					if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX) && is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX))
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

	/**
	 * 结算描述
	 */
	private void set_result_describe_phz_chd(int seat_index) {
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
						des += ",天胡×6";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸  +3硬息";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡×6";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += ",红胡×" + (3 + (_hong_pai_count[seat_index] - 10));
					}

					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",点胡×6";
					}
					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",黑胡×8";
					}
					if (type == GameConstants.CHR_TING_HU) {
						des += ",听胡×6";
					}
					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海胡×6";
					}
					if (type == GameConstants.CHR_DA_HU) {
						des += ",大胡×" + (8 + this._da_pai_count[seat_index] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU) {
						des += ",小胡×" + (10 + this._xiao_pai_count[seat_index] - 16);
					}
					if (type == GameConstants.CHR_DUI_ZI_HU) {
						des += ",对子胡×8";
					}
					if (type == GameConstants.CHR_SHUA_HOU) {
						des += ",耍猴×8";
					}
					if (type == GameConstants.CHR_HUANG_FAN) {
						des += ",黄番×" + (1 + this._huang_zhang_count);
					}
					if (type == GameConstants.CHR_TUAN_YUAN) {
						des += ",团圆×" + (8 * this._tuan_yuan_count[seat_index]);
					}
					if (type == GameConstants.CHR_HANG_HANG_XI) {
						des += ",行行息×8";
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_phz_xt_jiben_ming_tang(int seat_index) {
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
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸  +3硬息";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += ",红胡×2";
					}

					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",点胡×3";
					}
					if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
						des += ",红转弯×4";
					}
					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",黑胡×5";
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_phz_xt_all_ming_tang(int seat_index) {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TIAN_HU) {
						des += ",天胡×6";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸  +3硬息";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡×6";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += ",红胡×" + 4;
					}
					if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
						des += ",红乌×" + (4 + (_hong_pai_count[seat_index] - 13));
					}
					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",真点胡×5";
					}
					if (type == GameConstants.CHR_JIA_DIAN_HU) {
						des += ",假点胡×4";
					}
					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",黑胡×5";
					}
					if (type == GameConstants.CHR_TING_HU) {
						des += ",听胡×6";
					}
					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海底胡×2";
					}
					if (type == GameConstants.CHR_DA_HU) {
						des += ",大字胡×" + (8 + this._da_pai_count[seat_index] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU) {
						des += ",小字胡×" + (6 + this._xiao_pai_count[seat_index] - 16);
					}
					if (type == GameConstants.CHR_DUI_ZI_HU) {
						des += ",对子胡×4";
					}
					if (type == GameConstants.CHR_SHUA_HOU) {
						des += ",耍猴×5";
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_phz_xt_zimo_add_three(int seat_index) {
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
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸  +3硬息";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡";
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_phz_xt_no_ming_tang(int seat_index) {
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
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸 ";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡";
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	public void set_result_describe(int seat_index) {

		if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_GUILIN_ZP)) {
			set_result_describe_glzp();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_BAYI_ZP)) {
			set_result_describe_ba_yi();
		}

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
	public int get_real_card(int seat_index, int card) {
		return card;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 * 
	 * @param show
	 * 
	 *            翻醒
	 */
	public void set_niao_card(int seat_index, int card, boolean show) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		if (GRR._left_card_count == 0) {
			return;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._xing_player[i] == 0)
				continue;
			if (has_rule(GameConstants.Game_RULE_HU_XING) && this._hu_xing_count[i] == 0) {

				this._hu_xing_count[i] = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], card);

			}
		}

		GRR._show_bird_effect = show;
		GRR._count_niao = 0;
		int xing_card = 0;
		int count = 1;
		if (has_rule(GameConstants.GAME_RULE_SI_XING) && (has_rule(GameConstants.GAME_RULE_BEN_XING) || has_rule(GameConstants.GAME_RULE_SHANG_XING)
				|| has_rule(GameConstants.GAME_RULE_XIA_XING))) {

			while (this.GRR._left_card_count > 0) {
				count--;
				xing_card = this._repertory_card[this._all_card_len - this.GRR._left_card_count];
				--this.GRR._left_card_count;
				this._xing_card[_xing_count++] = xing_card;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {

					if (this._xing_player[i] == 0)
						continue;
					this._xing_card_player[i][_xing_count - 1] = xing_card;
				}
				if (has_rule(GameConstants.GAME_RULE_BEN_XING)) {
					this._ben_card = xing_card;
					boolean flag = false;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._xing_player[i] == 0)
							continue;
						int ben_xing_count = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], xing_card);
						this._ben_xing_count[i] += ben_xing_count;
						if (ben_xing_count == 4) {
							flag = true;
							this._xing_player[i]++;
						}

					}
					if (flag == true)
						count++;

				}

				if (has_rule(GameConstants.GAME_RULE_SHANG_XING)) {
					if (_logic.get_card_value(xing_card) == 10) {
						if (_logic.get_card_color(xing_card) == 0)
							this._shang_xing_card = 0x01;
						else
							this._shang_xing_card = 0x11;

					} else
						this._shang_xing_card = xing_card + 1;
					boolean flag = false;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._xing_player[i] == 0)
							continue;
						int shang_xing_count = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], this._shang_xing_card);
						this._shang_xing_count[i] += shang_xing_count;
						if (shang_xing_count == 4) {
							flag = true;
							this._xing_player[i]++;
						}

					}
					if (flag == true)
						count++;
				}
				if (has_rule(GameConstants.GAME_RULE_XIA_XING)) {
					if (_logic.get_card_value(xing_card) == 1) {
						if (_logic.get_card_color(xing_card) == 0)
							this._xia_xing_card = 0x0a;
						else
							this._xia_xing_card = 0x1a;

					} else
						this._xia_xing_card = xing_card - 1;
					boolean flag = false;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._xing_player[i] == 0)
							continue;
						int xia_xing_count = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], this._xia_xing_card);
						this._xia_xing_count[i] += xia_xing_count;
						if (xia_xing_count == 4) {
							flag = true;
							this._xing_player[i]++;
						}

					}
					if (flag == true)
						count++;

				}
				if (count == 0)
					break;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._xing_player[i] > 0)
						this._xing_player[i]--;
				}
			}
			return;
		}
		if (has_rule(GameConstants.GAME_RULE_BEN_XING) || has_rule(GameConstants.GAME_RULE_SHANG_XING)
				|| has_rule(GameConstants.GAME_RULE_XIA_XING)) {
			if (this.GRR._left_card_count == 0) {
				xing_card = GRR._chi_hu_card[seat_index][0];
				this._xing_card[_xing_count++] = GRR._chi_hu_card[seat_index][0];
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {

					if (this._xing_player[i] == 0)
						continue;
					this._xing_card_player[i][_xing_count - 1] = xing_card;
				}
				GRR._count_niao++;
			} else {
				xing_card = this._repertory_card[this._all_card_len - this.GRR._left_card_count];
				GRR._count_niao++;
				--this.GRR._left_card_count;
				this._xing_card[_xing_count++] = xing_card;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {

					if (this._xing_player[i] == 0)
						continue;
					this._xing_card_player[i][_xing_count - 1] = xing_card;
				}
			}
			// this.operate_player_get_card(GameConstants.INVALID_SEAT, 1, new
			// int[] { xing_card}, GameConstants.INVALID_SEAT, false);
		}
		if (has_rule(GameConstants.GAME_RULE_BEN_XING)) {
			this._ben_card = xing_card;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._xing_player[i] == 0)
					continue;
				int ben_xing_count = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], xing_card);
				this._ben_xing_count[i] += ben_xing_count;
			}

		}

		if (has_rule(GameConstants.GAME_RULE_SHANG_XING)) {
			if (_logic.get_card_value(xing_card) == 10) {
				if (_logic.get_card_color(xing_card) == 0)
					this._shang_xing_card = 0x01;
				else
					this._shang_xing_card = 0x11;

			} else
				this._shang_xing_card = xing_card + 1;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._xing_player[i] == 0)
					continue;
				int shang_xing_count = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], _shang_xing_card);
				this._shang_xing_count[i] += shang_xing_count;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_XIA_XING)) {
			if (_logic.get_card_value(xing_card) == 1) {
				if (_logic.get_card_color(xing_card) == 0)
					this._xia_xing_card = 0x0a;
				else
					this._xia_xing_card = 0x1a;

			} else
				this._xia_xing_card = xing_card - 1;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._xing_player[i] == 0)
					continue;
				int _xia_xing_count = _logic.get_xing_pai_count(this._hu_weave_items[i], this._hu_weave_count[i], _xia_xing_card);
				this._xia_xing_count[i] += _xia_xing_count;
			}

		}

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
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {
		if (seat_index == this._xiao_jia) {
			if (delay > 0) {
				GameSchedule.put(new DispatchCardXiaoRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
			} else {
				// 发牌
				this._handler = this._handler_dispath_card_xiao;
				this._handler_dispath_card_xiao.reset_status(seat_index, type);
				this._handler.exe(this);
			}
			return true;
		}
		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_card;
			this._handler_dispath_card.reset_status(seat_index, type);
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
	public boolean exe_dispatch_add_card(int seat_index, int type, int delay) {

		if (delay > 0) {
			GameSchedule.put(new DispatchAddCardRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_add_card;
			this._handler_dispath_add_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * //刷新手牌 延时
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_hand_card_update(int seat_index, int card, int delay) {

		if (delay > 0) {
			GameSchedule.put(new HandCardUpdateRunnable(this.getRoom_id(), seat_index, card, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this._handler = this._handler_dispath_add_card;
			this._handler_dispath_add_card.reset_status(seat_index, card);
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
			// 是否有抢杠胡
			this._handler = this._handler_gang;
			this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d, depatch);
			this._handler.exe(this);
		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
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

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_chi_peng;
		this._handler_chi_peng.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_zhao(int seat_index, int provider, int action, int card, int type, int lou_operate) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_zhao;
		this._handler_zhao.reset_status(seat_index, provider, action, card, type, lou_operate);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
				TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
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

	public void is_dui_qi(int seat_index) {
		int cards[] = new int[3];
		int count = _logic.switch_to_value_to_card(7, cards);
		for (int i = 0; i < count; i++) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(cards[i])] == 2) {
				if (GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(cards[i])] == 0)
					GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(cards[i])]++;
			}
		}
	}

	public void is_chi_pai(int seat_index, int card) {
		if (_logic.get_hong_dot(card) == 0)
			return;
		int cards[] = new int[3];
		int dot = _logic.get_dot(card);
		int count = _logic.switch_to_value_to_card(dot, cards);
		for (int i = 0; i < count; i++) {
			if (GRR._cards_index[seat_index][_logic.switch_to_card_index(cards[i])] == 2) {
				if (GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(cards[i])] == 0)
					GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(cards[i])]++;
			}
		}

	}

	/**
	 * 调度,加入牌堆
	 **/
	public void cannot_outcard(int seat_index, int card_data, int count, boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		if (seat_index == this._xiao_jia)
			return; // 小家所有牌都可以出
		if (card_data != 0 && count > 0) {
			if (GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] == 0)
				GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] += count;

		} else if (card_data != 0) {
			if (GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] > 0)
				GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] += count;
		}

		if (send_client == true) {
			this.operate_cannot_card(seat_index, true);
		}
	}

	public void cannot_chicard(int seat_index, int card) {
		int cards[] = new int[3];
		int count = _logic.switch_to_value_to_card(_logic.get_dot(card), cards);
		for (int i = 0; i < count; i++) {

			if (this._cannot_chi_index[seat_index][_logic.switch_to_card_index(cards[i])] == 0)
				this._cannot_chi_index[seat_index][_logic.switch_to_card_index(cards[i])]++;

		}
		return;
	}

	public void cannot_pengcard(int seat_index, int card) {
		int cards[] = new int[3];
		int count = _logic.switch_to_value_to_card(_logic.get_dot(card), cards);
		for (int i = 0; i < count; i++) {

			if (this._cannot_peng_index[seat_index][_logic.switch_to_card_index(cards[i])] == 0)
				this._cannot_peng_index[seat_index][_logic.switch_to_card_index(cards[i])]++;

		}
		return;
	}

	public boolean check_out_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] != 0) {
				if (GRR._must_out_index[seat_index][i] != 0)
					return true;
				if (GRR._cannot_out_index[seat_index][i] == 0)
					return true;
			}
		}
		return false;
	}

	public void set_must_out_card(int card_index) {
		if (card_index == 0)
			this._zhe_di_card = true;
		if (card_index == 20)
			this._zhe_tian_card = true;
		if (card_index == 1)
			this._zhe_ding_card = true;
		if (card_index == 19)
			this._zhe_fu_card = true;
	}

	public void must_out_card() {
		if (GRR == null)
			return;// 最后一张

		boolean is_must_out_card[] = new boolean[this.getTablePlayerNumber()];
		Arrays.fill(is_must_out_card, false);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Arrays.fill(GRR._must_out_index[i], 0);
			if (i == this._xiao_jia)
				continue; // 小家不受限制
			if (GRR._cards_index[i][0] + GRR._cards_index[i][20] == 1) {
				if (this._zhe_tian_card == true && GRR._cards_index[i][0] == 1) {
					is_must_out_card[i] = true;
					cannot_outcard(i, _logic.switch_to_card_data(0), -1, true);
					GRR._must_out_index[i][0]++;
				}
				if (this._zhe_di_card == true && GRR._cards_index[i][20] == 1 && is_must_out_card[i] == false) {
					is_must_out_card[i] = true;
					cannot_outcard(i, _logic.switch_to_card_data(20), -1, true);
					GRR._must_out_index[i][20]++;
				}
			}
			if (GRR._cards_index[i][1] + GRR._cards_index[i][19] == 1 && is_must_out_card[i] == false) {
				if (this._zhe_ding_card == true && GRR._cards_index[i][19] == 1 && is_must_out_card[i] == false) {
					is_must_out_card[i] = true;
					cannot_outcard(i, _logic.switch_to_card_data(19), -1, true);
					GRR._must_out_index[i][19]++;
				}
				if (this._zhe_fu_card == true && GRR._cards_index[i][1] == 1 && is_must_out_card[i] == false) {
					is_must_out_card[i] = true;
					cannot_outcard(i, _logic.switch_to_card_data(1), -1, true);
					GRR._must_out_index[i][1]++;
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == this._xiao_jia)
				continue;
			this.operate_must_out_card(i, true);
		}
	}

	public boolean is_can_out_card(int seat_index) {
		if (this._is_xiang_gong[seat_index] == true)
			return false;
		boolean flag = false;
		for (int i = 0; i < GameConstants.MAX_CP_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] >= 1) {
				if (GRR._cannot_out_index[seat_index][i] == 0) {
					flag = true;
					break;
				}

			}

		}
		return flag;
	}

	public void add_must_zhao(int seat_index, int card) {
		if (card == 0)
			return;
		boolean is_zhao = false;
		for (int i = 0; i < GRR._weave_count[seat_index]; i++) {
			switch (GRR._weave_items[seat_index][i].weave_kind) {
			case GameConstants.CP_WIK_CHE:
			case GameConstants.CP_WIK_HUA:
			case GameConstants.CP_WIK_KAN:
				if (GRR._weave_items[seat_index][i].center_card == card)
					is_zhao = true;
			}
		}
		int dot = _logic.get_dot(card);
		if (dot == 7)
			is_zhao = true;
		if (GRR._cards_index[seat_index][_logic.switch_to_card_index(card)] == 1)
			is_zhao = true;
		if (is_zhao == true) {
			if (this._must_zhao_card[seat_index][_logic.switch_to_card_index(card)] == 0)
				this._must_zhao_card[seat_index][_logic.switch_to_card_index(card)]++;
		}
	}

	public void add_zhao(int seat_index, int card) {
		if (card == 0)
			return;
		boolean is_zhao = false;
		for (int i = 0; i < GRR._weave_count[seat_index]; i++) {
			switch (GRR._weave_items[seat_index][i].weave_kind) {
			case GameConstants.CP_WIK_LEFT:
			case GameConstants.CP_WIK_CENTER:
			case GameConstants.CP_WIK_RIGHT:
				if (GRR._weave_items[seat_index][i].center_card == card)
					is_zhao = true;
			}
		}
		if (this._cannot_chi_index[seat_index][_logic.switch_to_card_index(card)] == 1)
			is_zhao = true;
		if (this._cannot_peng_index[seat_index][_logic.switch_to_card_index(card)] == 1)
			is_zhao = true;
		if (is_zhao == true) {
			if (this._zhao_card[seat_index][_logic.switch_to_card_index(card)] == 0)
				this._zhao_card[seat_index][_logic.switch_to_card_index(card)]++;
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
			for (int j = 0; j < GameConstants.MAX_CP_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_CP_COUNT];
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
	@Override
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
	public boolean runnable_chuli_first_card_data(int seat_index, int _type, boolean _tail) {
		// 发牌
		this._handler = this._handler_chuli_firstcards;
		this._handler_chuli_firstcards.reset_status(seat_index, _type);
		this._handler.exe(this);
		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int seat_index, int _type, boolean _tail) {

		this._handler = this._handler_dispath_firstcards;
		this._handler_dispath_firstcards.reset_status(seat_index, _type);
		this._handler.exe(this);
		return true;
	}

	@Override
	public boolean display_card(int seat_index, int card, boolean is_out_card) {

		if (is_out_card == true) {
			operate_player_get_card(_last_player, 0, null, GameConstants.INVALID_SEAT, false, 1);
			// 刷新自己手牌
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
			operate_player_cards(seat_index, hand_card_count, cards, GRR._weave_count[seat_index], GRR._weave_items[seat_index]);
			must_out_card();
			if (check_out_card(seat_index) == false) {
				no_card_out_game_end(seat_index, 0);
				return true;
			}
			this._playerStatus[seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			operate_player_status();
		} else {

			operate_player_get_card(_last_player, 0, null, GameConstants.INVALID_SEAT, false, 1);
			// 刷新自己手牌
			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
			operate_player_cards(seat_index, hand_card_count, cards, GRR._weave_count[seat_index], GRR._weave_items[seat_index]);
			operate_player_get_card(seat_index, 0, null, GameConstants.INVALID_SEAT, false);
			must_out_card();
			int chi_peng_cards[] = new int[3];
			int chi_peng_count = _logic.switch_to_value_to_card(_logic.get_dot(card), chi_peng_cards);
			for (int i = 0; i < chi_peng_count; i++) {
				_cannot_chi_index[seat_index][_logic.switch_to_card_index(chi_peng_cards[i])] = 0;
				_cannot_peng_index[seat_index][_logic.switch_to_card_index(chi_peng_cards[i])] = 0;
			}
			int s_cards[] = new int[3];
			int count = _logic.switch_to_value_to_card(7, s_cards);
			for (int i = 0; i < count; i++) {
				if (GRR._cards_index[seat_index][_logic.switch_to_card_index(s_cards[i])] == 2) {
					cannot_outcard(seat_index, s_cards[i], 1, true);
				}
			}

			exe_chuli_first_card(seat_index, GameConstants.WIK_NULL, 1000);
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {

		return GameConstants.GAME_PLAYER_FOUR;
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

	@Override
	public boolean open_card_timer() {
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		return false;
	}

	@Override
	public boolean ready_timer() {
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		return false;
	}

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	@Override
	public boolean hu_pai_timer(int seat_index, int operate_card, int wik_kind) {
		handler_operate_card(seat_index, wik_kind, operate_card, -1);
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
