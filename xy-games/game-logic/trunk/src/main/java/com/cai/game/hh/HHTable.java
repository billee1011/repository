/**
 * 
 */
package com.cai.game.hh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_ChenZhou;
import com.cai.common.constant.game.Constants_New_ChenZhou;
import com.cai.common.constant.game.Constants_WuGang;
import com.cai.common.constant.game.Constants_YZCHZ;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.define.ECardType;
import com.cai.common.define.ELogType;
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
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.ChulifirstCardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.handler.HHHandler;
import com.cai.game.hh.handler.HHHandlerChiPeng;
import com.cai.game.hh.handler.HHHandlerDispatchCard;
import com.cai.game.hh.handler.HHHandlerFinish;
import com.cai.game.hh.handler.HHHandlerGang;
import com.cai.game.hh.handler.HHHandlerOutCardOperate;
import com.cai.game.hh.handler.chdphz.PHZHandlerChiPeng_CHD;
import com.cai.game.hh.handler.chdphz.PHZHandlerChuLiFirstCard_CHD;
import com.cai.game.hh.handler.chdphz.PHZHandlerDispatchCard_CHD;
import com.cai.game.hh.handler.chdphz.PHZHandlerDispatchFirstCard_CHD;
import com.cai.game.hh.handler.chdphz.PHZHandlerGang_CHD;
import com.cai.game.hh.handler.chdphz.PHZHandlerOutCardOperate_CHD;
import com.cai.game.hh.handler.hdlhq.LHQHandlerChiPeng_HD;
import com.cai.game.hh.handler.hdlhq.LHQHandlerChuLiFirstCard_HD;
import com.cai.game.hh.handler.hdlhq.LHQHandlerDispatchCard_HD;
import com.cai.game.hh.handler.hdlhq.LHQHandlerDispatchFirstCard_HD;
import com.cai.game.hh.handler.hdlhq.LHQHandlerGang_HD;
import com.cai.game.hh.handler.hdlhq.LHQHandlerOutCardOperate_HD;
import com.cai.game.hh.handler.hgw468.HGW468HandlerChiPeng_HH;
import com.cai.game.hh.handler.hgw468.HGW468HandlerChuLiFirstCard_HH;
import com.cai.game.hh.handler.hgw468.HGW468HandlerDispatchCard_HH;
import com.cai.game.hh.handler.hgw468.HGW468HandlerDispatchFirstCard_HH;
import com.cai.game.hh.handler.hgw468.HGW468HandlerGang_HH;
import com.cai.game.hh.handler.hgw468.HGW468HandlerOutCardOperate_HH;
import com.cai.game.hh.handler.hhhgw.HGWHandlerChiPeng_HH;
import com.cai.game.hh.handler.hhhgw.HGWHandlerChuLiFirstCard_HH;
import com.cai.game.hh.handler.hhhgw.HGWHandlerDispatchCard_HH;
import com.cai.game.hh.handler.hhhgw.HGWHandlerDispatchFirstCard_HH;
import com.cai.game.hh.handler.hhhgw.HGWHandlerGang_HH;
import com.cai.game.hh.handler.hhhgw.HGWHandlerOutCardOperate_HH;
import com.cai.game.hh.handler.hhyl.YLHandlerChiPeng_HH;
import com.cai.game.hh.handler.hhyl.YLHandlerChuLiFirstCard_HH;
import com.cai.game.hh.handler.hhyl.YLHandlerDispatchCard_HH;
import com.cai.game.hh.handler.hhyl.YLHandlerDispatchFirstCard_HH;
import com.cai.game.hh.handler.hhyl.YLHandlerGang_HH;
import com.cai.game.hh.handler.hhyl.YLHandlerOutCardOperate_HH;
import com.cai.game.hh.handler.thkhy.THKHandlerChiPeng_HY;
import com.cai.game.hh.handler.thkhy.THKHandlerChuLiFirstCard_HY;
import com.cai.game.hh.handler.thkhy.THKHandlerDispatchCard_HY;
import com.cai.game.hh.handler.thkhy.THKHandlerDispatchFirstCard_HY;
import com.cai.game.hh.handler.thkhy.THKHandlerGang_HY;
import com.cai.game.hh.handler.thkhy.THKHandlerOutCardOperate_HY;
import com.cai.game.hh.handler.xpphz.Constants_XPPHZ;
import com.cai.game.hh.handler.xpphz.HHGameLogic_XP;
import com.cai.game.hh.handler.xtphz.PHZHandlerChiPeng_XT;
import com.cai.game.hh.handler.xtphz.PHZHandlerChuLiFirstCard_XT;
import com.cai.game.hh.handler.xtphz.PHZHandlerDispatchCard_XT;
import com.cai.game.hh.handler.xtphz.PHZHandlerDispatchFirstCard_XT;
import com.cai.game.hh.handler.xtphz.PHZHandlerGang_XT;
import com.cai.game.hh.handler.xtphz.PHZHandlerOutCardOperate_XT;
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
import com.cai.game.hh.handler.yyzhz.GameConstants_YYZHZ;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

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

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class HHTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(HHTable.class);

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

	public boolean[] has_first_qi_shou_ti = new boolean[4];

	// 状态变量
	private boolean _status_send; // 发牌状态
	private boolean _status_gang; // 抢杆状态
	private boolean _status_cs_gang; // 长沙杆状态
	private boolean _status_gang_hou_pao; // 杠后炮状态

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
	public int _cannot_chi[][];// 不可以吃的牌
	public int _cannot_chi_count[];// 每个用户不可以吃牌的数量
	public int _cannot_peng[][];// 不可以碰的牌
	public int _cannot_peng_count[];// 每个用户不可以碰牌的数量
	public int _guo_hu_xt[]; // 同一圈用户过胡
	public int _guo_hu_pai_cards[][]; // 过胡牌
	public int _guo_hu_pai_count[]; // 过胡牌数量
	public int _guo_hu_xi[][]; // 过胡胡息
	public int _guo_hu_hu_xi[][]; // 过胡时的胡息
	public int _all_out_card_index[]; // 所有出过的牌
	// 胡息
	// public HuCardInfo _hu_card_info;

	public HHGameLogic _logic = null;
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
	public boolean _xian_ming_zhao_not[];
	public int _is_di_hu[];
	public boolean _is_tian_hu;
	public boolean isEnd = false; // 六胡抢金币场 胡牌的瞬间有玩家逃跑 导致BUG
	public boolean[] gu;
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _hu_weave_items[][];

	public long _request_release_time;
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

	public HHTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new HHGameLogic();

		// 设置等待状态

		// 玩家
		Player[] players = new Player[this.getTablePlayerNumber()];
		set_players(players);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;

		has_shoot = new boolean[this.getTablePlayerNumber()];
		cards_has_wei = new int[GameConstants.MAX_HH_INDEX];
		is_hands_up = new boolean[this.getTablePlayerNumber()];
		player_ti_count = new int[this.getTablePlayerNumber()][2];
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		is_wang_diao = new boolean[this.getTablePlayerNumber()];
		is_wang_diao_wang = new boolean[this.getTablePlayerNumber()];
		is_wang_chuang = new boolean[this.getTablePlayerNumber()];

		has_first_qi_shou_ti = new boolean[this.getTablePlayerNumber()];

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
		_cannot_chi = new int[this.getTablePlayerNumber()][60];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][60];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_all_out_card_index = new int[GameConstants.MAX_HH_INDEX];
		gu = new boolean[this.getTablePlayerNumber()];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao_not = new boolean[this.getTablePlayerNumber()];
		_is_di_hu = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
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
			_xian_ming_zhao_not[i] = false;
			_is_di_hu[i] = 0;
			_all_out_card_index[i] = 0;

		}
		_is_tian_hu = true;
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;
			}
		}

		// 胡牌信息
		_huang_zhang_count = 0;
		// _hu_card_info = new HuCardInfo();
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (

				int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}
		_status_cs_gang = false;

		_gang_card_data = new CardsData(GameConstants.MAX_HH_COUNT);
	}

	public void init_param() {
		// super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new HHGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;

		has_shoot = new boolean[this.getTablePlayerNumber()];
		cards_has_wei = new int[GameConstants.MAX_HH_INDEX];
		is_hands_up = new boolean[this.getTablePlayerNumber()];
		player_ti_count = new int[this.getTablePlayerNumber()][2];
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		is_wang_diao = new boolean[this.getTablePlayerNumber()];
		is_wang_diao_wang = new boolean[this.getTablePlayerNumber()];
		is_wang_chuang = new boolean[this.getTablePlayerNumber()];

		has_first_qi_shou_ti = new boolean[this.getTablePlayerNumber()];

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
		_cannot_chi = new int[this.getTablePlayerNumber()][60];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][60];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao_not = new boolean[this.getTablePlayerNumber()];
		_is_di_hu = new int[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
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
			_xian_ming_zhao_not[i] = false;
			_is_di_hu[i] = 0;

		}
		_is_tian_hu = true;
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;
			}
		}

		gu = new boolean[this.getTablePlayerNumber()];

		// 胡牌信息
		_huang_zhang_count = 0;
		// _hu_card_info = new HuCardInfo();
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (

				int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}
		_status_cs_gang = false;

		_gang_card_data = new CardsData(GameConstants.MAX_HH_COUNT);
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
		_cannot_chi = new int[this.getTablePlayerNumber()][60];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][60];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao_not = new boolean[this.getTablePlayerNumber()];
		_is_di_hu = new int[this.getTablePlayerNumber()];

		gu = new boolean[this.getTablePlayerNumber()];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
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
			_xian_ming_zhao_not[i] = false;
			_is_di_hu[i] = 0;
		}
		_is_tian_hu = true;
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		_guo_hu_xi = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++) {
				_guo_hu_pai_cards[i][j] = 0;
				_guo_hu_xi[i][j] = 0;
				_guo_hu_hu_xi[i][j] = 0;
			}
		}
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //

		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XP)) {
			// WalkerGeek 溆浦跑胡子箍臭组合最大10个
			_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][10];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < 10; j++) {
					_hu_weave_items[i][j] = new WeaveItem();
				}
			}
			_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][10];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < 10; j++)
					_lou_weave_item[i][j] = new LouWeaveItem();
			}
		} else {
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
		}

		// 胡牌信息
		// _hu_card_info = new HuCardInfo();
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des());
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.hu_pai_count[i] = 0;
			_player_result.ming_tang_count[i] = 0;
			_player_result.ying_xi_count[i] = 0;
			_player_result.dian_pao_count[i] = 0;
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
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
			// 2017年11月21日 20:56:10 modify by 何鑫奇 默认玩法 low实现
			if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)) {
				if (!has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)) {
					this._game_rule_index = this._game_rule_index | (1 << GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS);
					this._game_rule_index = this._game_rule_index | (1 << GameConstants.GAME_RULE_DI_AN_WEI);
				}
				if (!has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN) && !has_rule(GameConstants.GAME_RULE_HAI_DI_LAO_YUE)) {
					this._game_rule_index = this._game_rule_index | (1 << GameConstants.GAME_RULE_HAI_DI_LAO_YUE);
				}
			} else if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)) {
				if (!has_rule(GameConstants.GAME_RULE_DI_MING_WEI)) {
					this._game_rule_index = this._game_rule_index | (1 << GameConstants.GAME_RULE_DI_MING_WEI);
				}
			} else if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
				if (!has_rule(GameConstants.GAME_RULE_DI_MING_WEI)) {
					this._game_rule_index = this._game_rule_index | (1 << GameConstants.GAME_RULE_DI_MING_WEI);
				}
			}
			_handler_dispath_card = new LHQHandlerDispatchCard_HD();
			_handler_out_card_operate = new LHQHandlerOutCardOperate_HD();
			_handler_gang = new LHQHandlerGang_HD();
			_handler_chi_peng = new LHQHandlerChiPeng_HD();

			_handler_chuli_firstcards = new LHQHandlerChuLiFirstCard_HD();
			_handler_dispath_firstcards = new LHQHandlerDispatchFirstCard_HD();
		}

		if (is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
			_handler_dispath_card = new THKHandlerDispatchCard_HY();
			_handler_out_card_operate = new THKHandlerOutCardOperate_HY();
			_handler_gang = new THKHandlerGang_HY();
			_handler_chi_peng = new THKHandlerChiPeng_HY();

			_handler_chuli_firstcards = new THKHandlerChuLiFirstCard_HY();
			_handler_dispath_firstcards = new THKHandlerDispatchFirstCard_HY();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHD)) {
			_handler_dispath_card = new PHZHandlerDispatchCard_CHD();
			_handler_out_card_operate = new PHZHandlerOutCardOperate_CHD();
			_handler_gang = new PHZHandlerGang_CHD();
			_handler_chi_peng = new PHZHandlerChiPeng_CHD();

			_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_CHD();
			_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_CHD();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)) {
			_handler_dispath_card = new PHZHandlerDispatchCard_XT();
			_handler_out_card_operate = new PHZHandlerOutCardOperate_XT();
			_handler_gang = new PHZHandlerGang_XT();
			_handler_chi_peng = new PHZHandlerChiPeng_XT();

			_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_XT();
			_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_XT();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HGW_HH)) {
			_handler_dispath_card = new HGWHandlerDispatchCard_HH();
			_handler_out_card_operate = new HGWHandlerOutCardOperate_HH();
			_handler_gang = new HGWHandlerGang_HH();
			_handler_chi_peng = new HGWHandlerChiPeng_HH();

			_handler_chuli_firstcards = new HGWHandlerChuLiFirstCard_HH();
			_handler_dispath_firstcards = new HGWHandlerDispatchFirstCard_HH();

		}
		if (is_mj_type(GameConstants.GAME_TYPE_YL_PHZ)) {
			_handler_dispath_card = new YLHandlerDispatchCard_HH();
			_handler_out_card_operate = new YLHandlerOutCardOperate_HH();
			_handler_gang = new YLHandlerGang_HH();
			_handler_chi_peng = new YLHandlerChiPeng_HH();

			_handler_chuli_firstcards = new YLHandlerChuLiFirstCard_HH();
			_handler_dispath_firstcards = new YLHandlerDispatchFirstCard_HH();

		}
		if (is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			_handler_dispath_card = new HGW468HandlerDispatchCard_HH();
			_handler_out_card_operate = new HGW468HandlerOutCardOperate_HH();
			_handler_gang = new HGW468HandlerGang_HH();
			_handler_chi_peng = new HGW468HandlerChiPeng_HH();

			_handler_chuli_firstcards = new HGW468HandlerChuLiFirstCard_HH();
			_handler_dispath_firstcards = new HGW468HandlerDispatchFirstCard_HH();

		}
		_handler_finish = new HHHandlerFinish();
		this.setMinPlayerCount(this.getTablePlayerNumber());

	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
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
		_is_tian_hu = true;
		has_first_qi_shou_ti = new boolean[this.getTablePlayerNumber()];

		_cannot_chi = new int[this.getTablePlayerNumber()][60];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][60];
		_cannot_peng_count = new int[this.getTablePlayerNumber()];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][7];
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
		_xian_ming_zhao_not = new boolean[this.getTablePlayerNumber()];
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_all_out_card_index = new int[GameConstants.MAX_HH_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = new int[this.getTablePlayerNumber()];
		isEnd = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			has_shoot[i] = false;
			is_hands_up[i] = false;
			player_ti_count[i][0] = 0;
			player_ti_count[i][1] = 0;
			is_wang_diao[i] = false;
			is_wang_diao_wang[i] = false;
			is_wang_chuang[i] = false;
			_is_di_hu[i] = 0;
			_all_out_card_index[i] = 0;
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cards_has_wei[i] = 0;
		}
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_cannot_chi_count[i] = 0;
			_cannot_peng_count[i] = 0;
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
			_xian_ming_zhao_not[i] = false;

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

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, GameConstants.MAX_HH_COUNT, Constants_YongZhou.MAX_CARD_INDEX);
		GRR.setRoom(this);
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

		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU)) {
			_repertory_card = new int[Constants_YZCHZ.CARD_COUNT_YZCHZ];
			shuffle(_repertory_card, Constants_YZCHZ.CARD_FOR_YZCHZ);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);
		}

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (this._cur_round == 1 && getRuleValue(GameConstants.GAME_RULE_RANDOM_SEAT) == 1) {
			int rand = (RandomUtil.getRandomNumber(Integer.MAX_VALUE));
			this._cur_banker = rand % this.getTablePlayerNumber();
		}
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
		// enableRobot();
		if (this.getRuleValue(GameConstants.GAME_RULE_CAN_LESS) == 1) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_MINUE_20_CARD) == 1 && this.playerNumber == 2)
				GRR._left_card_count -= 20;
		} else if (this.getRuleValue(GameConstants.GAME_RULE_HGW_MINUE_20_CARD) == 1)
			GRR._left_card_count -= 20;
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			return game_start_HH();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ)) {
			return game_start_HH();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHD)) {
			return game_start_HH();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHEN_ZHOU) || is_mj_type(GameConstants.GAME_TYPE_PHZ_LEI_YANG)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_WU_GANG) || is_mj_type(GameConstants.GAME_TYPE_PHZ_CZ_SR)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU) || is_mj_type(GameConstants.GAME_TYPE_PHZ_CZ_SR)) {
			return game_start_HH();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)) {
			return game_start_XT();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			return game_start_PHZ_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			return game_start_FPHZ_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
			return game_start_FPHZ_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
			return game_start_thk();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
			return game_start_PHZ_YX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			return game_start_HH468();
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
		this.GRR._banker_player = this._current_player = this._cur_banker;
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
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
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}

	private boolean game_start_HH468() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();

		this.GRR._banker_player = this._current_player = this._cur_banker;
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
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
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}

	/**
	 * 开始 湘潭跑胡子
	 * 
	 * @return
	 */
	private boolean game_start_XT() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			SysParamModel sysParamModel1110 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1110);

			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}

			if (has_rule(GameConstants.GAME_RULE_ADD_SPEED) && sysParamModel1110 != null && sysParamModel1110.getVal1() > 0
					&& sysParamModel1110.getVal1() < 10000) {
				FlashTime = sysParamModel1110.getVal1();
			}
			if (has_rule(GameConstants.GAME_RULE_ADD_SPEED) && sysParamModel1110 != null && sysParamModel1110.getVal2() > 0
					&& sysParamModel1110.getVal2() < 10000) {
				standTime = sysParamModel1110.getVal2();
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
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}

	private boolean game_start_FPHZ_YX() {
		_logic.clean_magic_cards();
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP))
			if (this.getRuleValue(GameConstants.GAME_RULE_LHQ_MINUE_20_CARD) == 1)
				GRR._left_card_count -= 20;
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
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
		this.GRR._banker_player = this._current_player = this._cur_banker;
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
		// int ti_card_count[] = new int[this.getTablePlayerNumber()];
		// int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];
		//
		// for (int i = 0; i < playerCount; i++) {
		// ti_card_count[i] =
		// this._logic.get_action_ti_Card(this.GRR._cards_index[i],
		// ti_card_index[i]);
		// if (ti_card_count[i] > 0)
		// can_ti = true;
		// }
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

	private boolean game_start_thk() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
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
		// int ti_card_count[] = new int[this.getTablePlayerNumber()];
		// int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];
		//
		// for (int i = 0; i < playerCount; i++) {
		// ti_card_count[i] =
		// this._logic.get_action_ti_Card(this.GRR._cards_index[i],
		// ti_card_index[i]);
		// if (ti_card_count[i] > 0)
		// can_ti = true;
		// }
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
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
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
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		// _logic.random_card_data(repertory_card, mj_cards);
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
		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
					|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
					|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP))
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHEN_ZHOU))
				if (has_rule(Constants_ChenZhou.GAME_RULE_PLAYER_4))
					send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_PHZ_WU_GANG))
				if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4))
					send_count = (GameConstants.MAX_FPHZ_COUNT + 1);

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
		// int cards[] = new int[] { 0x01, 0x11, 0x11, 0x15, 0x15, };
		// {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		/*
		 * int cards1[] = new int[]
		 * {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		 * int cards2[] = new int[]
		 * {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		 * int cards3[] = new int[]
		 * {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		 */
		// int cards[] = new int[] { 011, 011, 011 };
		// int cards2[] = new int[] { 0x12, 0x12, 0x12, 0x12, 0x01, 0x02, 0x04,
		// 0x03, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17, 0x17, 0x15, 0x19, 0x17,
		// 0x17, 0x13 };
		// int cards3[] = new int[] { 0x13, 0x13, 0x14, 0x14, 0x01, 0x02, 0x04,
		// 0x03, 0x05, 0x06, 0x15, 0x15, 0x16, 0x17, 0x17, 0x15, 0x19, 0x18,
		// 0x18, 0x16 };
		//
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		// GRR._cards_index[i][j] = 0;
		// }
		// }
		//// int send_count = (GameConstants.MAX_HH_COUNT );
		////
		// _repertory_card[_all_card_len - GRR._left_card_count] = 0x07;
		// if (this.getTablePlayerNumber() == GameConstants.GAME_PLAYER_HH) {
		// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		// GRR._cards_index[_current_player % this.getTablePlayerNumber()][j] =
		// 0;
		// GRR._cards_index[(_current_player + 1) %
		// this.getTablePlayerNumber()][j] = 0;
		// GRR._cards_index[(_current_player + 2) %
		// this.getTablePlayerNumber()][j] = 0;
		// }
		// }
		// else{
		/*
		 * for (int j = 0; j < 21; j++) { GRR._cards_index[0][j] = cards[j];
		 * GRR._cards_index[1][j] = cards[j]; GRR._cards_index[2][j] = cards[j];
		 * GRR._cards_index[3][j] = cards[j]; }
		 */
		// }
		// 清除数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		// for (int j = 0; j < cards.length; j++) {
		// GRR._cards_index[0][_logic.switch_to_card_index(cards[j])] += 1;
		// GRR._cards_index[1][_logic.switch_to_card_index(cards[j])] += 1;
		// GRR._cards_index[2][_logic.switch_to_card_index(cards[j])] += 1;
		// GRR._cards_index[3][_logic.switch_to_card_index(cards[j])] += 1;
		// }
		//

//		 int[] realyCards = new int[] {
//				 23, 6, 1, 24, 9, 18, 4, 24, 22, 21, 2, 7, 8, 20, 5, 18, 7, 3, 
//				 26, 19, 18, 6, 5, 26, 3, 17, 3, 9, 7, 10, 10, 6, 1, 9, 19, 4, 18, 2, 
//				 23, 19, 25, 24, 22, 8, 19, 21, 24, 7, 17, 5, 2, 20, 9, 6, 21, 26, 21,
//				 23, 5, 3, 10, 25, 17, 1, 
//		 };
//		 this._cur_banker = 0;
//		 testRealyCard(realyCards);
//		 this.DEBUG_CARDS_MODE = true;

		// this.testSameCard(new
		// int[]{0x11,0x11,0x11,0x12,0x12,0x12,0x13,0x13,0x13,0x14,0x14,0x14,0x15,0x15,0x15,0x16,0x16,0x17,0x17,0x17}
		// );

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {

				if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
					if (debug_my_cards.length > 19) {
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
				} else if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
						|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
						|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
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
				} else {
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
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (is_mj_type(GameConstants.GAME_TYPE_PHZ_WANG_CHENG))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
					|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
					|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP))
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
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
			send_count = (GameConstants.MAX_HH_COUNT - 1);
			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
					|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
					|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP))
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);
			if (send_count > cards.length) {
				send_count = cards.length;
			}
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

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			ret = this.handler_game_finish_hh(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ)) {
			ret = this.handler_game_finish_hh(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			ret = this.handler_game_finish_hh(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			ret = this.handler_game_finish_PHZ(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHD)) {
			ret = this.handler_game_finish_phz_chd(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) || is_mj_type(GameConstants.GAME_TYPE_PHZ_CHEN_ZHOU)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_WU_GANG) || is_mj_type(GameConstants.GAME_TYPE_PHZ_CZ_SR)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU) || is_mj_type(GameConstants.GAME_TYPE_PHZ_CZ_SR)) {
			ret = this.handler_game_finish_phz_chd(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_NEW_PHZ_CHEN_ZHOU)) {
			ret = handler_game_finish_new_chen_zhou(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			ret = this.handler_game_finish_FPHZ(seat_index, reason);
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
			ret = this.handler_game_finish_lhq(seat_index, reason);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
			ret = this.handler_game_finish_lhq(seat_index, reason);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean handler_game_finish_lhq(int seat_index, int reason) {
		int real_reason = reason;

		if (is_sys()) {
			_game_status = GameConstants.GAME_STATUS_WAIT;
		}

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
			// 设置中鸟数据
			for (int i = 0; i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

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
			this.set_result_describe(seat_index);
			if (seat_index != -1) {
				game_end.setTunShu(this._tun_shu[seat_index]);
				game_end.setFanShu(this._fan_shu[seat_index]);
			}

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
		if (is_sys()) {
			game_end.setRunPlayerId(_run_player_id);
			game_end.setRoomOverType(0);
		}

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

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}
		this.isEnd = false;

		// 错误断言
		return false;
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

	@Override
	protected boolean checkHuanDou(int end_type) {
		return end_type != GameConstants.Game_End_DRAW;
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
		if (reason != GameConstants.Game_End_DRAW)
			this._huang_zhang_count = 0;
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

	public boolean handler_game_finish_new_chen_zhou(int seat_index, int reason) {
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

		if (GRR != null) {
			// TODO 胡的那张牌，是在哪一个组合里面
			game_end.setCountPickNiao(GRR._count_pick_niao);

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			// TODO
			// 利用game_end_response里的win_lzi_fen来表示胡牌的类型：1表示自摸，2表示胡，3表示点炮，4表示流局
			int tmp_win_index = -1;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (GRR._win_order[i] == 1) {
					tmp_win_index = i;
					break;
				}
			}
			if (tmp_win_index == -1) {
				game_end.setWinLziFen(4);
			} else {
				ChiHuRight tmp_chr = GRR._chi_hu_rights[tmp_win_index];
				if (!tmp_chr.opr_and(Constants_New_ChenZhou.CHR_ZI_MO).is_empty()) {
					game_end.setWinLziFen(1);
				} else if (!tmp_chr.opr_and(Constants_New_ChenZhou.CHR_CHI_HU).is_empty()
						|| !tmp_chr.opr_and(Constants_New_ChenZhou.CHR_SPECAIL_TIAN_HU).is_empty()) {
					game_end.setWinLziFen(2);
				} else if (!tmp_chr.opr_and(Constants_New_ChenZhou.CHR_JEI_PAO_HU).is_empty()) {
					game_end.setWinLziFen(3);
				}
			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
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
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

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
				int all_hu_xi = 0;
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();

				if (_hu_weave_count[i] > 0 && GRR._win_order[i] == 1) {
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
				} else {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

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

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi[], boolean dispatch) {
		if (this._ti_two_long[seat_index] == true) {
			return 0;
		}
		if (GameConstants.GAME_TYPE_HH_YX == _game_type_index) {
			return analyse_chi_hu_card_hh(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type, hu_xi);
		}
		if (GameConstants.GAME_TYPE_HGW_HH == _game_type_index) {
			return analyse_chi_hu_card_hgw(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
		}
		if (GameConstants.GAME_TYPE_YL_PHZ == _game_type_index) {
			return analyse_chi_hu_card_hhyl(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
		}
		if (GameConstants.GAME_TYPE_468_HONG_GUAI_WAN == _game_type_index) {
			return analyse_chi_hu_card_hgw468(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
		}
		if (GameConstants.GAME_TYPE_PHZ_YX == _game_type_index) {
			return analyse_chi_hu_card_phz(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type, hu_xi);
		}
		if (GameConstants.GAME_TYPE_PHZ_CHD == _game_type_index) {
			return analyse_chi_hu_card_phz_chd(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					hu_xi);
		}
		if (GameConstants.GAME_TYPE_PHZ_XT == _game_type_index) {
			return analyse_chi_hu_card_phz_xt(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					hu_xi);
		}
		if (GameConstants.GAME_TYPE_FPHZ_YX == _game_type_index) {
			return analyse_chi_hu_card_fphz(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type, hu_xi);
		}
		if (GameConstants.GAME_TYPE_LHQ_HD == _game_type_index || GameConstants.GAME_TYPE_LHQ_HY == _game_type_index
				|| GameConstants.GAME_TYPE_LHQ_QD == _game_type_index || _game_type_index == GameConstants.GAME_TYPE_LHQ_QD_SAPP) {
			return analyse_chi_hu_card_lhqhd(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
		}
		if (GameConstants.GAME_TYPE_THK_HY == _game_type_index) {
			return analyse_chi_hu_card_thk(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
		}
		if (GameConstants.GAME_TYPE_WMQ_AX == _game_type_index) {
			return analyse_chi_hu_card_wmq(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
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
	public int analyse_chi_hu_card_wmq(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		int card_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
			if (cards_index[i] > 0)
				card_count += cards_index[i];
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
		int cbMingIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		boolean zimo = false;
		if (seat_index == provider_index)
			zimo = true;
		_logic.ming_index_temp(cbMingIndexTemp, weaveItems, weaveCount, zimo, cur_card);
		boolean bValue = _logic.analyse_card_wmq(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			// 构造扑克
			int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
			_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
			temp_hu_xi = _logic.get_all_hu_xi_awq(analyseItem, cbAnalyseIndexTemp, cbMingIndexTemp);

			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}
		if (max_hu_xi < 10) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		this._hu_pai_max_hu[seat_index] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi_wmq(_hu_weave_items[seat_index][j]);
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;

		}
		analyseItem = analyseItemArray.get(max_hu_index);
		int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
		int all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		int hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		this._ying_hu_count[seat_index] = _logic.calculate_ying_hu_count(cbAnalyseIndexTemp);
		int dui_zi_count = _logic.calculate_dui_zi_hu_count(analyseItem);
		boolean is_hua_man_yuan = _logic.is_hua_man_yuan(analyseItem);
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {

			if (this._hong_pai_count[seat_index] == 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);
			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);

			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			if (this.GRR._left_card_count == 22) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] == 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._hong_pai_count[seat_index] == this._ying_hu_count[seat_index] * 4 && this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			else if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (_logic.piao_dui(analyseItem) && this._hong_pai_count[seat_index] == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (max_hu_xi == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (max_hu_xi == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count == 1) {
				if (analyseItem.cbCardEye == cur_card) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count == 1) {
				if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				} else if (analyseItem.cbCardEye == cur_card) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count == 1) {
				if (analyseItem.cbCardEye == cur_card) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count >= 2) {
				if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					if (_logic.get_card_value(cur_card) == 8) {
						chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
					} else
						chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				}
				if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				}
				if (_logic.is_feng_bai_wei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_FBW_WMQ);

				}

			}
		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] == 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._hong_pai_count[seat_index] == this._ying_hu_count[seat_index] * 4 && this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			else if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}
			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (_logic.piao_dui(analyseItem) && this._hong_pai_count[seat_index] == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}

			if (max_hu_xi == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count == 1) {
				if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				} else if (analyseItem.cbCardEye == cur_card) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count >= 2) {
				if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				}
				if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				}

			}
		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] == 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._hong_pai_count[seat_index] == this._ying_hu_count[seat_index] * 4 && this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			else if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);
			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}

			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(cbAnalyseIndexTemp)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}
			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(cbAnalyseIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (_logic.piao_dui(analyseItem) && this._hong_pai_count[seat_index] == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (max_hu_xi == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (max_hu_xi == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count == 1) {
				if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				} else if (analyseItem.cbCardEye == cur_card) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_playerStatus[seat_index]._hu_card_count >= 2) {
				if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				}
				if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
						weaveCount) == true) {
					chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				}

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
	public int analyse_chi_hu_card_lhqhd(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {

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
		boolean bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG && dispatch == true)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					// 插入扑克
					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
						cbCardIndexTemp[j] = cards_index[j];
					}
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 5; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& ((analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG && dispatch == true)
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);

			if (cards_index[cur_index] == 3) {
				for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
					cbCardIndexTemp[j] = cards_index[j];
				}
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
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
		if (max_hu_xi < 6) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
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
		int flag = 0;
		if (this.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			if ((this._is_tian_hu == true) && (GRR._banker_player == seat_index) && (dispatch == true)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
					flag = 1;
				}

			}
		} else {

			if ((this._is_tian_hu == true) && (GRR._banker_player == seat_index) && (dispatch == true)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
					flag = 1;
				}
			}
		}
		flag = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (!(weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG))

			{
				flag = 1;
				break;
			}

		}
		if (flag == 0 && seat_index != this.GRR._banker_player) {
			chiHuRight.opr_or(GameConstants.CHR_DI_HU);
		}
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		} else if (dispatch == false) {
			chiHuRight.opr_or(GameConstants.CHR_DIAN_PAO_HU);
		}
		if (dispatch == true && has_rule(GameConstants.GAME_RULE_HAI_DI_LAO_YUE) && GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_HU);
		}

		if (has_rule(GameConstants.GAME_RULE_HONG_HEI_DIAN)) {

			if (is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
				if (hong_pai_count >= 8) {
					chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
				}

			} else {
				if (hong_pai_count >= 8 && hong_pai_count < 10) {
					chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
				}
				if (hong_pai_count >= 10) {
					chiHuRight.opr_or(GameConstants.CHR_THIRTEEN_HONG_PAI);
				}
				if (hong_pai_count == 1) {
					chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
				}
			}

			if (hei_pai_count == 1) {
				chiHuRight.opr_or(GameConstants.CHR_ONE_HEI);
			}
			if (hei_pai_count == all_cards_count) {
				chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
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
	public int analyse_chi_hu_card_thk(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		if (this._ti_mul_long[seat_index] != 0)
			return GameConstants.WIK_NULL;
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
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG && dispatch == true)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_XIAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& ((analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG && dispatch == true)
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_XIAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
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
		int calculate_score = 0;
		int max_calculate_score = 0;
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

			if (temp_hu_xi == 10)
				calculate_score = 3;
			else if (temp_hu_xi == 11)
				calculate_score = 1;
			else if (temp_hu_xi >= 12 && temp_hu_xi <= 14)
				calculate_score = 2;
			else if (temp_hu_xi >= 15 && temp_hu_xi < 18)
				calculate_score = 3;
			else if (temp_hu_xi >= 18)
				calculate_score = 3 + (18 - 15) / 3;
			if (temp_hu_xi > max_hu_xi && calculate_score > max_calculate_score) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
				max_calculate_score = calculate_score;
			} else if (calculate_score == max_calculate_score) {
				if (temp_hu_xi > max_hu_xi) {
					max_hu_index = i;
					max_hu_xi = temp_hu_xi;
				}

			}
		}
		if (max_hu_xi < 10) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
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
		int flag = 0;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		} else if (dispatch == false) {
			chiHuRight.opr_or(GameConstants.CHR_DIAN_PAO_HU);
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
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
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
	public int analyse_chi_hu_card_fphz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi_pao[]) {

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
		boolean bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_SAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 5; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_SAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_fphz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
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
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
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
		if (this.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			if ((this.GRR._left_card_count == 23) && (GRR._banker_player != seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
				return cbChiHuKind;
			}
			if ((this.GRR._left_card_count == 23) && (GRR._banker_player == seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}
				return cbChiHuKind;
			}
		} else {
			if ((this.GRR._left_card_count == 22) && (GRR._banker_player != seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
				return cbChiHuKind;
			}
			if ((this.GRR._left_card_count == 23) && (GRR._banker_player == seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
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
	public int analyse_chi_hu_card_phz(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi_pao[]) {

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
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_SAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_SAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
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

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
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
	public int analyse_chi_hu_card_hh(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi_hh[]) {

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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_SAO))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_SAO))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
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
	public int analyse_chi_hu_card_hgw(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		if (this._ti_mul_long[seat_index] > 0) {
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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| (analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI && provider_index != seat_index)))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								else if ((cur_card == analyseItem.cbCenterCard[j])
										&& (analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI && provider_index == seat_index))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_TI_LONG;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
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

		}

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		_da_pai_count[seat_index] = 0;
		_xiao_pai_count[seat_index] = 0;
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
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		boolean is_hu = false;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_ZMH_15) == 1)
				is_hu = true;
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}

		if (this._hong_pai_count[seat_index] >= 10) {
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
			is_hu = true;
		}

		if (this._hong_pai_count[seat_index] == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
			is_hu = true;
		}
		if (hei_pai_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HEI);
			is_hu = true;
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
			is_hu = true;
		}
		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_HU);
			is_hu = true;
		}
		if (this._da_pai_count[seat_index] >= 18) {
			chiHuRight.opr_or(GameConstants.CHR_DA_HU);
			is_hu = true;
		}
		if (this._xiao_pai_count[seat_index] >= 16) {
			chiHuRight.opr_or(GameConstants.CHR_XIAO_HU);
			is_hu = true;
		}
		if (_logic.calculate_pengpeng_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index])) {
			chiHuRight.opr_or(GameConstants.CHR_DUI_ZI_HU);
			is_hu = true;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		int flag = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (!(weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG))

			{
				flag = 1;
				break;
			}

		}
		if (flag == 0 && seat_index != this.GRR._banker_player && dispatch == true
				&& (this.GRR._left_card_count != (this._all_card_len - (this.getTablePlayerNumber() * (GameConstants.MAX_HH_COUNT - 1) - 1)))) {
			chiHuRight.opr_or(GameConstants.CHR_DI_HU);
			is_hu = true;
		}
		if (flag == 0 && (this._is_tian_hu == true) && (GRR._banker_player == seat_index)) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				is_hu = true;
			}
		}
		if (max_hu_xi < 18 && is_hu == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
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
	public int analyse_chi_hu_card_hhyl(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		if (this._ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int card_count = _logic.get_card_count_by_index(cards_index);
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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| (analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI && provider_index != seat_index)))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								else if ((cur_card == analyseItem.cbCenterCard[j])
										&& (analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI && provider_index == seat_index))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_TI_LONG;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
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

		}

		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		_da_pai_count[seat_index] = 0;
		_xiao_pai_count[seat_index] = 0;
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
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		boolean is_hu = false;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_ZMH_15) == 1)
				is_hu = true;
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}

		if (this._hong_pai_count[seat_index] >= 10) {
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
			is_hu = true;
		}

		if (this._hong_pai_count[seat_index] == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
			is_hu = true;
		}
		if (hei_pai_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HEI);
			is_hu = true;
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
			is_hu = true;
		}
		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_HU);
			is_hu = true;
		}
		if (this._da_pai_count[seat_index] >= 18) {
			chiHuRight.opr_or(GameConstants.CHR_DA_HU);
			is_hu = true;
		}
		if (this._xiao_pai_count[seat_index] >= 16) {
			chiHuRight.opr_or(GameConstants.CHR_XIAO_HU);
			is_hu = true;
		}
		if (_logic.calculate_pengpeng_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index])) {
			chiHuRight.opr_or(GameConstants.CHR_DUI_ZI_HU);
			is_hu = true;
		}
		if (_logic.calculate_tuan_yuan_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index]) > 0) {
			chiHuRight.opr_or(GameConstants.CHR_TUAN_CHD);
			is_hu = true;
		}
		if (card_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_SHUA_HOU);
			is_hu = true;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		int flag = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (!(weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG))

			{
				flag = 1;
				break;
			}

		}
		if (flag == 0 && seat_index != this.GRR._banker_player && dispatch == true
				&& (this.GRR._left_card_count != (this._all_card_len - (this.getTablePlayerNumber() * (GameConstants.MAX_HH_COUNT - 1) - 1)))) {
			chiHuRight.opr_or(GameConstants.CHR_DI_HU);
			is_hu = true;
		}
		if (flag == 0 && (this._is_tian_hu == true) && (GRR._banker_player == seat_index)) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				is_hu = true;
			}
		}
		if (max_hu_xi < 18 && is_hu == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_hgw468(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		if (this._ti_mul_long[seat_index] > 0) {
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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| (analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI && provider_index != seat_index)))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								else if ((cur_card == analyseItem.cbCenterCard[j])
										&& (analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI && provider_index == seat_index))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_TI_LONG;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
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

		}

		/*
		 * if(1==1) { cbChiHuKind = GameConstants.WIK_CHI_HU;
		 * chiHuRight.opr_or(GameConstants.CHR_TIAN_HU); return cbChiHuKind; }
		 */
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		_da_pai_count[seat_index] = 0;
		_xiao_pai_count[seat_index] = 0;
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
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		boolean is_hu = false;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_ZMH_15) == 1)
				is_hu = true;
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}

		if (this._hong_pai_count[seat_index] >= 10) {
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
			is_hu = true;
		}

		if (this._hong_pai_count[seat_index] == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
			is_hu = true;
		}
		if (hei_pai_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HEI);
			is_hu = true;
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
			is_hu = true;
		}
		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_HU);
			is_hu = true;
		}
		if (this._da_pai_count[seat_index] >= 18) {
			chiHuRight.opr_or(GameConstants.CHR_DA_HU);
			is_hu = true;
		}
		if (this._xiao_pai_count[seat_index] >= 16) {
			chiHuRight.opr_or(GameConstants.CHR_XIAO_HU);
			is_hu = true;
		}
		if (_logic.calculate_pengpeng_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index])) {
			chiHuRight.opr_or(GameConstants.CHR_DUI_ZI_HU);
			is_hu = true;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		int flag = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (!(weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG))

			{
				flag = 1;
				break;
			}

		}
		if (flag == 0 && seat_index != this.GRR._banker_player && dispatch == true
				&& (this.GRR._left_card_count != (this._all_card_len - (this.getTablePlayerNumber() * (GameConstants.MAX_HH_COUNT - 1) - 1)))) {
			chiHuRight.opr_or(GameConstants.CHR_DI_HU);
			is_hu = true;
		}
		if (flag == 0 && (this.GRR._left_card_count == (this._all_card_len - (this.getTablePlayerNumber() * (GameConstants.MAX_HH_COUNT - 1) - 1)))
				&& (GRR._banker_player == seat_index)) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				is_hu = true;
			}
		}
		if (max_hu_xi < 18 && is_hu == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
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
	public int analyse_chi_hu_card_phz_chd(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index,
			int cur_card, ChiHuRight chiHuRight, int card_type, int hu_xi_hh[]) {

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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
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
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
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
		int hang_hang_xi_count = 0;
		_da_pai_count[seat_index] = 0;
		_xiao_pai_count[seat_index] = 0;
		_tuan_yuan_count[seat_index] = 0;
		_hong_pai_count[seat_index] = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			if (_hu_weave_items[seat_index][j].hu_xi > 0) {
				hang_hang_xi_count++;
			}
			_hu_weave_count[seat_index] = j + 1;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);
		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;

			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_hu_weave_count[seat_index]++;
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}
		if (hong_pai_count >= 10) {
			_hong_pai_count[seat_index] = hong_pai_count;
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
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
		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_HU);
		}
		if (this._da_pai_count[seat_index] >= 18) {
			chiHuRight.opr_or(GameConstants.CHR_DA_HU);
		}
		if (this._xiao_pai_count[seat_index] >= 16) {
			chiHuRight.opr_or(GameConstants.CHR_XIAO_HU);
		}
		if (_logic.calculate_pengpeng_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index])) {
			chiHuRight.opr_or(GameConstants.CHR_DUI_ZI_HU);
		}
		if (this.has_rule(GameConstants.GAME_RULE_DI_HANG_HANG_XI)) {
			if (hang_hang_xi_count == 7) {
				chiHuRight.opr_or(GameConstants.CHR_HANG_HANG_XI);
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_DI_TUAN_YUAN)) {
			_tuan_yuan_count[seat_index] = _logic.calculate_tuan_yuan_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index]);

			if (_tuan_yuan_count[seat_index] > 0) {
				chiHuRight.opr_or(GameConstants.CHR_TUAN_YUAN);
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
			if (this._huang_zhang_count >= 1) {
				chiHuRight.opr_or(GameConstants.CHR_HUANG_FAN);
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_DI_SHUA_HOU)) {
			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				card_count += cards_index[j];
			}
			if (card_count == 1) {
				chiHuRight.opr_or(GameConstants.CHR_SHUA_HOU);
			}
		}
		int flag = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (!(weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG))

			{
				flag = 1;
				break;
			}

		}
		if (flag == 0) {
			chiHuRight.opr_or(GameConstants.CHR_TING_HU);
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (this.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			if ((this.GRR._left_card_count == 19) && (GRR._banker_player != seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
				return cbChiHuKind;
			}
			if ((this.GRR._left_card_count == 19) && (GRR._banker_player == seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}
				return cbChiHuKind;
			}
		} else {
			if ((this.GRR._left_card_count == 18) && (GRR._banker_player != seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
				return cbChiHuKind;
			}
			if ((this.GRR._left_card_count == 19) && (GRR._banker_player == seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}
				return cbChiHuKind;
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
	public int analyse_chi_hu_card_phz_xt(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi_hh[]) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		if (this._ti_mul_long[seat_index] > 0) {
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
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
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
		// if (has_rule(GameConstants.GAME_RULE_TEN_HU_XI_XT)) {
		// if (max_hu_xi < 10) {
		// chiHuRight.set_empty();
		// return GameConstants.WIK_NULL;
		// }
		// } else if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI))
		// {
		// if (max_hu_xi < 18) {
		// chiHuRight.set_empty();
		// return GameConstants.WIK_NULL;
		// }
		// } else
		{
			if (max_hu_xi < 15) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		_da_pai_count[seat_index] = 0;
		_xiao_pai_count[seat_index] = 0;
		_tuan_yuan_count[seat_index] = 0;
		_hong_pai_count[seat_index] = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);
		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;

			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind,
					_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card);
			_hu_weave_count[seat_index]++;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}

		if (this.has_rule(GameConstants.GAME_RULE_NO_MING_TANG)) {
			return cbChiHuKind;
		}
		if (this.has_rule(GameConstants.GAME_RULE_ZI_MO_ADD_THREE)) {
			return cbChiHuKind;
		}
		if (hong_pai_count >= 13) {
			_hong_pai_count[seat_index] = hong_pai_count;
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
		if (this.has_rule(GameConstants.GAME_RULE_JI_BENMING_TANG)) {
			if (hong_pai_count >= 10 && hong_pai_count < 13) {
				_hong_pai_count[seat_index] = hong_pai_count;
				chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
			}
			return cbChiHuKind;
		}
		if (this.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
			if ((this.GRR._left_card_count == 18) && (GRR._banker_player != seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}
			}
			if ((this.GRR._left_card_count == 19) && (GRR._banker_player == seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}
			}
		} else {
			if ((this.GRR._left_card_count == 18) && (GRR._banker_player != seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_DI_HU);
				}

			}
			if ((this.GRR._left_card_count == 19) && (GRR._banker_player == seat_index)) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
				}

			}
		}
		if (hong_pai_count > 10 && hong_pai_count < 13) {
			_hong_pai_count[seat_index] = hong_pai_count;
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
		}
		if (hong_pai_count == 10) {
			_hong_pai_count[seat_index] = hong_pai_count;
			chiHuRight.opr_or(GameConstants.CHR_JIA_DIAN_HU);
		}
		if (this.has_rule(GameConstants.GAME_RULE_SI_QI_ZHANG_HONG_HU)) {
			if (hong_pai_count == 4 || hong_pai_count == 7) {
				_hong_pai_count[seat_index] = hong_pai_count;
				chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
			}
		}
		if (this.GRR._left_card_count == 0) {
			chiHuRight.opr_or(GameConstants.CHR_HAI_HU);
		}
		if (this._da_pai_count[seat_index] >= 18) {
			chiHuRight.opr_or(GameConstants.CHR_DA_HU);
		}
		if (this._xiao_pai_count[seat_index] >= 16) {
			chiHuRight.opr_or(GameConstants.CHR_XIAO_HU);
		}
		if (_logic.calculate_pengpeng_count(_hu_weave_items[seat_index], _hu_weave_count[seat_index])) {
			chiHuRight.opr_or(GameConstants.CHR_DUI_ZI_HU);
		}

		int card_count = 0;
		for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
			card_count += cards_index[j];
		}
		if (card_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_SHUA_HOU);
		}

		int flag = 0;
		for (int i = 0; i < weaveCount; i++) {
			if (!((weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG) || (weaveItems[i].weave_kind == GameConstants.WIK_AN_LONG_LIANG)))

			{
				flag = 1;
				break;
			}

		}
		if (flag == 0) {
			chiHuRight.opr_or(GameConstants.CHR_TING_HU);
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
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_SAO))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_SAO;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_SAO;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_SAO;
		}
		return bAroseAction;
	}

	public int estimate_player_ti_wei_respond_phz_chd(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_WEI;
		}
		return bAroseAction;
	}

	public int estimate_player_ti_wei_respond_phz_czsr(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
					bAroseAction = GameConstants.WIK_CHOU_WEI;
				} else {
					bAroseAction = GameConstants.WIK_WEI;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
		}
		return bAroseAction;
	}

	public int estimate_player_wei_respond_wmq(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;

		// 偎牌判断
		if ((bAroseAction == GameConstants.WIK_NULL)
				&& (_logic.check_wei_wmq(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_WEI;
		}
		return bAroseAction;
	}

	public int estimate_player_ti_xiao_respond_phz_chd(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_XIAO))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_XIAO;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_XIAO;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_XIAO;
		}
		return bAroseAction;
	}

	public int estimate_player_respond_hh(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
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
				// GameConstants.WIK_PAO,GameConstants.PAO_TYPE_MINE_PENG_PAO
				// true, false);
				pao_type[0] = GameConstants.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}

		}

		//
		return bAroseAction;
	}

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_phz_chd(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
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
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI))
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

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_xiao(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
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
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_XIAO))
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

	public boolean estimate_player_out_card_respond_chen_zhou(int seat_index, int card, boolean bDisdatch) {
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
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
			next_index++;

			if (this.has_shoot[chi_seat_index]) // 射跑之后不能主动进牌
				continue;

			if (this.is_hands_up[chi_seat_index]) // 举手过后不能主动进牌
				continue;

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
			for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {
				if (GRR._discard_cards[last_index][chi_index] == card) {
					break;
				}
			}
			if (chi_index != GRR._discard_count[last_index])
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
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
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index, _lou_weave_item[chi_seat_index][4].nCount,
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
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index, _lou_weave_item[chi_seat_index][5].nCount,
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			if (this.has_shoot[i]) // 射跑之后不能主动进牌
				continue;

			if (this.is_hands_up[i]) // 举手之后不能主动进牌
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

	public boolean estimate_player_out_card_respond_yzchz(int seat_index, int card, boolean bDisdatch, int zuo_xing_seat) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int pCount = getTablePlayerNumber();

		boolean is_yz_chz = is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU);

		int action = GameConstants.WIK_NULL;

		int next_index = 0;
		if (bDisdatch != true)
			next_index++;

		int player_pass[] = new int[getTablePlayerNumber()];
		int cur_card_index = _logic.switch_to_card_index(card);

		do {
			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();

			if (is_yz_chz && pCount == 4 && chi_seat_index == zuo_xing_seat) {
				chi_seat_index = (seat_index + next_index + 1) % getTablePlayerNumber();
			}

			next_index++;

			if (has_shoot[chi_seat_index])
				continue;

			if (is_hands_up[chi_seat_index])
				continue;

			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}

			if (_cannot_chi[chi_seat_index][cur_card_index] != 0)
				continue;

			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				card_count += GRR._cards_index[chi_seat_index][j];
			}

			if (card_count == 2)
				continue;

			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {

				int type_count[] = new int[10];
				int type_eat_count[] = new int[1];

				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count);
				for (int i = 0; i < type_eat_count[0]; i++) {
					int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
						temp_cards_index[j] = GRR._cards_index[chi_seat_index][j];
					}
					boolean bAction = false;
					if (temp_cards_index[cur_card_index] == 0) {
						bAction = true;
						_playerStatus[chi_seat_index].add_action(type_count[i]);
						_playerStatus[chi_seat_index].add_chi_chz(card, type_count[i], seat_index, _lou_weave_item[chi_seat_index][0].nCount,
								_lou_weave_item[chi_seat_index][0].nSendCount, _lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						bAroseAction = true;
						player_pass[chi_seat_index] = 1;
						continue;
					}

					int temp_cards_index1[] = new int[GameConstants.MAX_HH_INDEX];
					for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
						temp_cards_index1[j] = temp_cards_index[j];
					}

					_logic.delete_chi_index(type_count[i], card, temp_cards_index1);
					if (temp_cards_index1[cur_card_index] < 0) {
						bAction = true;
						_playerStatus[chi_seat_index].add_action(type_count[i]);
						_playerStatus[chi_seat_index].add_chi_chz(card, type_count[i], seat_index, _lou_weave_item[chi_seat_index][0].nCount,
								_lou_weave_item[chi_seat_index][0].nSendCount, _lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						bAroseAction = true;
						player_pass[chi_seat_index] = 1;
						continue;
					}
					int lou_index = _logic.get_chi_index(type_count[i]);
					int type_count1[] = new int[10];
					int type_eat_count1[] = new int[1];
					if (card_count <= 5)
						continue;
					_logic.check_chi(temp_cards_index1, card, type_count1, type_eat_count1);
					if (temp_cards_index1[cur_card_index] == 0 && type_eat_count1[0] > 0) {
						bAction = true;
					}
					int first_count = 0;
					int jj_count = 0;
					for (int jj = 0; jj < type_eat_count1[0]; jj++) {

						int send_count = 0;

						int temp_cards_index2[] = new int[GameConstants.MAX_HH_INDEX];
						for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
							temp_cards_index2[j] = temp_cards_index1[j];
						}
						_logic.delete_chi_index(type_count1[jj], card, temp_cards_index2);
						if (temp_cards_index2[cur_card_index] < 0) {
							first_count++;
							_lou_weave_item[chi_seat_index][lou_index].nLouWeaveKind[jj_count][send_count++] = type_count1[jj];
							_lou_weave_item[chi_seat_index][lou_index].nSendCount[jj_count++] = send_count;
							continue;
						}
						int type_count2[] = new int[10];
						int type_eat_count2[] = new int[1];
						if (card_count <= 8)
							continue;
						_logic.check_chi(temp_cards_index2, card, type_count2, type_eat_count2);
						if (temp_cards_index2[cur_card_index] == 0 && type_eat_count2[0] > 0) {
							bAction = true;
							_lou_weave_item[chi_seat_index][lou_index].nLouWeaveKind[jj_count][send_count++] = type_count1[jj];
							first_count++;
						}
						for (int kkk = 0; kkk < type_eat_count2[0]; kkk++) {
							_lou_weave_item[chi_seat_index][lou_index].nLouWeaveKind[jj_count][send_count++] = type_count2[kkk];
						}
						if (type_eat_count2[0] != 0)
							_lou_weave_item[chi_seat_index][lou_index].nSendCount[jj_count++] = send_count;

					}
					_lou_weave_item[chi_seat_index][lou_index].nCount = first_count;
					if (temp_cards_index1[cur_card_index] == 0 && first_count == 0)
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(type_count[i]);
						_playerStatus[chi_seat_index].add_chi_chz(card, type_count[i], seat_index, _lou_weave_item[chi_seat_index][lou_index].nCount,
								_lou_weave_item[chi_seat_index][lou_index].nSendCount, _lou_weave_item[chi_seat_index][lou_index].nLouWeaveKind);
						bAroseAction = true;
						player_pass[chi_seat_index] = 1;
						continue;
					}

				}
			}
		} while (next_index < 2);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			if (is_yz_chz && pCount == 4 && i == zuo_xing_seat)
				continue;

			if (has_shoot[i])
				continue;

			if (is_hands_up[i])
				continue;

			playerStatus = _playerStatus[i];

			if (_cannot_peng[i][cur_card_index] != 0)
				continue;

			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				card_count += GRR._cards_index[i][j];
			}

			if (card_count == 2)
				continue;

			if (_playerStatus[i].lock_huan_zhang() == false) {
				action = _logic.check_peng(GRR._cards_index[i], card);

				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);

					bAroseAction = true;
					player_pass[i] = 1;
				}
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			playerStatus = _playerStatus[i];

			if (player_pass[i] == 1) {
				_playerStatus[i].add_action(GameConstants.WIK_NULL);
				_playerStatus[i].add_pass(card, seat_index);
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
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
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
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
			if (is_mj_type(GameConstants.GAME_TYPE_HH_YX) == false) {
				for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {
					if (GRR._discard_cards[last_index][chi_index] == card) {
						break;
					}
				}
				if (chi_index != GRR._discard_count[last_index])
					continue;
				for (chi_index = 0; chi_index < GRR._discard_count[chi_seat_index]; chi_index++) {
					if (GRR._discard_cards[chi_seat_index][chi_index] == card) {
						break;
					}
				}
				if (chi_index != GRR._discard_count[chi_seat_index])
					continue;
			}

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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
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
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index, _lou_weave_item[chi_seat_index][4].nCount,
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
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index, _lou_weave_item[chi_seat_index][5].nCount,
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

	/**
	 * 岳阳捉红字出牌动作检测
	 * 
	 * @param seat_index
	 * @param card
	 * @param bDisdatch
	 * @return
	 */
	public boolean estimate_player_out_card_respond_yyzhz(int seat_index, int card, boolean bDisdatch) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		int next_index = 0;
		if (bDisdatch != true)
			next_index++;
		int player_pass[] = new int[this.getTablePlayerNumber()];
		int cur_card_index = _logic.switch_to_card_index_yyzhz(card);
		do {

			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
			next_index++;
			for (int i = 0; i < GameConstants_YYZHZ.MAX_WEAVE_YYZHZ; i++) {
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
			for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {
				if (GRR._discard_cards[last_index][chi_index] == card) {
					break;
				}
			}
			if (chi_index != GRR._discard_count[last_index])
				continue;
			for (chi_index = 0; chi_index < GRR._discard_count[chi_seat_index]; chi_index++) {
				if (GRR._discard_cards[chi_seat_index][chi_index] == card) {
					break;
				}
			}
			if (chi_index != GRR._discard_count[chi_seat_index])
				continue;
			int card_count = 0;

			for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; j++) {
				if (this.GRR._cards_index[chi_seat_index][j] < 3)
					card_count += this.GRR._cards_index[chi_seat_index][j];
			}
			if (card_count == 2)
				continue;

			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				int type_count[] = new int[10];
				int type_eat_count[] = new int[1];
				action = _logic.check_chi_yyzhz(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count);

				if ((action & GameConstants.WIK_LEFT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
						for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index + 2]--;
						bAction = _logic.check_lou_weave_yyzhz(temp_cards_index, card, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
								_lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
						for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index - 1]--;
						bAction = _logic.check_lou_weave_yyzhz(temp_cards_index, card, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
						for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index - 1]--;
						temp_cards_index[cur_card_index - 2]--;
						bAction = _logic.check_lou_weave_yyzhz(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
								_lou_weave_item[chi_seat_index][2].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				/*
				 * if ((action & GameConstants.WIK_XXD) != 0) { boolean bAction
				 * = false; if (GRR._cards_index[chi_seat_index][cur_card_index]
				 * > 0) { int temp_cards_index[] = new
				 * int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX]; for (int i = 0; i <
				 * GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
				 * temp_cards_index[i] = GRR._cards_index[chi_seat_index][i]; }
				 * if (_logic.get_card_color(card) == 0) {
				 * temp_cards_index[cur_card_index]--;
				 * temp_cards_index[cur_card_index]--;
				 * temp_cards_index[cur_card_index + 10]--; if
				 * (temp_cards_index[cur_card_index] == 0) bAction =
				 * _logic.check_lou_weave_yyzhz(temp_cards_index, card,
				 * GameConstants.WIK_XXD, type_count, type_eat_count[0],
				 * _lou_weave_item[chi_seat_index], 4, card_count - 3); else
				 * bAction = true; } else { temp_cards_index[cur_card_index -
				 * 10]--; temp_cards_index[cur_card_index - 10]--;
				 * temp_cards_index[cur_card_index]--; bAction =
				 * _logic.check_lou_weave_yyzhz(temp_cards_index, card,
				 * GameConstants.WIK_XXD, type_count, type_eat_count[0],
				 * _lou_weave_item[chi_seat_index], 4, card_count - 3); }
				 * 
				 * } else bAction = true; if (bAction == true) {
				 * _playerStatus[chi_seat_index].add_action(GameConstants.
				 * WIK_XXD); _playerStatus[chi_seat_index].add_chi_hh(card,
				 * GameConstants.WIK_XXD, seat_index,
				 * _lou_weave_item[chi_seat_index][4].nCount,
				 * _lou_weave_item[chi_seat_index][4].nLouWeaveKind);
				 * player_pass[chi_seat_index] = 1; } } if ((action &
				 * GameConstants.WIK_DDX) != 0) { boolean bAction = false; if
				 * (GRR._cards_index[chi_seat_index][cur_card_index] > 0) { int
				 * temp_cards_index[] = new
				 * int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX]; for (int i = 0; i <
				 * GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
				 * temp_cards_index[i] = GRR._cards_index[chi_seat_index][i]; }
				 * if (_logic.get_card_color(card) == 0) {
				 * temp_cards_index[cur_card_index]--;
				 * temp_cards_index[cur_card_index + 10]--;
				 * temp_cards_index[cur_card_index + 10]--; bAction =
				 * _logic.check_lou_weave_yyzhz(temp_cards_index, card,
				 * GameConstants.WIK_DDX, type_count, type_eat_count[0],
				 * _lou_weave_item[chi_seat_index], 5, card_count - 3); } else {
				 * temp_cards_index[cur_card_index - 10]--;
				 * temp_cards_index[cur_card_index]--;
				 * temp_cards_index[cur_card_index]--; if
				 * (temp_cards_index[cur_card_index] == 0) bAction =
				 * _logic.check_lou_weave_yyzhz(temp_cards_index, card,
				 * GameConstants.WIK_DDX, type_count, type_eat_count[0],
				 * _lou_weave_item[chi_seat_index], 5, card_count - 3); else
				 * bAction = true; }
				 * 
				 * } else bAction = true; if (bAction == true) {
				 * _playerStatus[chi_seat_index].add_action(GameConstants.
				 * WIK_DDX); _playerStatus[chi_seat_index].add_chi_hh(card,
				 * GameConstants.WIK_DDX, seat_index,
				 * _lou_weave_item[chi_seat_index][5].nCount,
				 * _lou_weave_item[chi_seat_index][5].nLouWeaveKind);
				 * player_pass[chi_seat_index] = 1; } }
				 */
				if ((action & GameConstants.WIK_EQS) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants_YYZHZ.MAX_YYZHZ_INDEX];
						for (int i = 0; i < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						int index[] = { 1, 6, 9 };
						int temp_index = ((_logic.get_card_color(card) == 1) ? 10 : 0);
						temp_cards_index[temp_index + index[0]]--;
						temp_cards_index[temp_index + index[1]]--;
						temp_cards_index[temp_index + index[2]]--;
						bAction = _logic.check_lou_weave_yyzhz(temp_cards_index, card, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
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
			for (int j = 0; j < GameConstants_YYZHZ.MAX_YYZHZ_INDEX; j++) {

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
				action = _logic.check_peng_wmq(GRR._cards_index[i], card);
				if (action != 0) {

					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
					player_pass[i] = 1;
				}
			}

		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();

			next_index++;
			if (this._xian_ming_zhao_not[chi_seat_index]) // 掀明招后不能吃
				continue;
			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			for (; chi_index < this._cannot_chi_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card) {
					break;
				}
			}

			if (has_shoot[chi_seat_index]) {
				continue;
			}
			if (chi_index != this._cannot_chi_count[chi_seat_index])
				continue;
			if (!is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
				for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {

					if (GRR._discard_cards[last_index][chi_index] == card) {
						break;
					}
				}
				if (chi_index != GRR._discard_count[last_index])
					continue;
			}
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
				action = _logic.check_chi_phz(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count, yws_type);

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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
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
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index, _lou_weave_item[chi_seat_index][4].nCount,
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
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index, _lou_weave_item[chi_seat_index][5].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_YWS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 6, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_YWS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_YWS, seat_index, _lou_weave_item[chi_seat_index][6].nCount,
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
			if (this._xian_ming_zhao_not[i]) // 掀明招后不能吃
				continue;
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

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_PHZ_syzp(int seat_index, int card, boolean bDisdatch) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有
		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		int next_index = 0;
		if (bDisdatch != true)
			next_index++;
		int player_pass[] = new int[this.getTablePlayerNumber()];
		int cur_card_index = _logic.switch_to_card_index(card);

		do {

			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();

			next_index++;
			if (this._xian_ming_zhao[chi_seat_index]) // 掀明招后不能吃
				continue;
			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			for (; chi_index < this._cannot_chi_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card) {
					break;
				}
			}

			if (has_shoot[chi_seat_index]) {
				continue;
			}
			if (chi_index != this._cannot_chi_count[chi_seat_index])
				continue;
			if (!is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
				for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {

					if (GRR._discard_cards[last_index][chi_index] == card) {
						break;
					}
				}
				if (chi_index != GRR._discard_count[last_index])
					continue;
			}
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
				boolean yws_type = false;
				action = _logic.check_chi_phz_syzp(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count, yws_type);

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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
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
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index, _lou_weave_item[chi_seat_index][4].nCount,
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
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index, _lou_weave_item[chi_seat_index][5].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
								_lou_weave_item[chi_seat_index][3].nLouWeaveKind);
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
			if (this._xian_ming_zhao[i]) // 掀明招后不能吃
				continue;
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

	/**
	 * 邵阳剥皮吃碰校验
	 * 
	 * @param seat_index
	 * @param card
	 * @param bDisdatch
	 * @return
	 */
	public boolean estimate_player_out_card_respond_PHZ_sypb(int seat_index, int card, boolean bDisdatch) {
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
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();

			next_index++;
			if (this._xian_ming_zhao[chi_seat_index]) // 掀明招后不能吃
				continue;
			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			for (; chi_index < this._cannot_chi_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card) {
					break;
				}
			}

			if (has_shoot[chi_seat_index]) {
				continue;
			}
			if (chi_index != this._cannot_chi_count[chi_seat_index])
				continue;
			if (!is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
				for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {

					if (GRR._discard_cards[last_index][chi_index] == card) {
						break;
					}
				}
				if (chi_index != GRR._discard_count[last_index])
					continue;
			}
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
				boolean yws_type = false;
				action = _logic.check_chi_phz(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count, yws_type);

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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
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
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index, _lou_weave_item[chi_seat_index][4].nCount,
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
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index, _lou_weave_item[chi_seat_index][5].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_YWS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 6, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_YWS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_YWS, seat_index, _lou_weave_item[chi_seat_index][6].nCount,
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
			if (this._xian_ming_zhao[i]) // 掀明招后不能吃
				continue;
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
			if (_playerStatus[i].lock_huan_zhang() == false && this.has_shoot[i] == false) {
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

	/**
	 * 溆浦跑胡子
	 * 
	 * @param seatIndex
	 * @return
	 */
	public boolean canEatPengWei(int seatIndex) {
		int pai_count = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if ((GRR._cards_index[seatIndex][i] + GRR._cards_index[seatIndex][i + 20]) < 3) {
				pai_count += (GRR._cards_index[seatIndex][i] + GRR._cards_index[seatIndex][i + 20]);
			}
		}
		if (pai_count == 2) { // 吃碰畏之前手上单牌正好是2 就必须胡牌才能操作
			int huxi = 0;
			for (int i = 0; i < GRR._weave_count[seatIndex]; i++) {
				_hu_weave_items[seatIndex][i].public_card = GRR._weave_items[seatIndex][i].public_card;
				_hu_weave_items[seatIndex][i].center_card = GRR._weave_items[seatIndex][i].center_card;
				_hu_weave_items[seatIndex][i].weave_kind = GRR._weave_items[seatIndex][i].weave_kind;
				_hu_weave_items[seatIndex][i].provide_player = GRR._weave_items[seatIndex][i].provide_player;
				_hu_weave_items[seatIndex][i].hu_xi = GRR._weave_items[seatIndex][i].hu_xi;
				huxi += GRR._weave_items[seatIndex][i].hu_xi;
			}

			if (huxi < Constants_XPPHZ.GAME_QI_HU_XI) {
				return false;
			}
		}
		return true;
	}

	// 玩家出牌的动作检测
	public boolean estimate_player_out_card_respond_xpphz(int seat_index, int card, boolean bDisdatch, HHGameLogic_XP _logic) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		int next_index = 0;
		if (bDisdatch != true) {
			next_index++;
		}
		int player_pass[] = new int[this.getTablePlayerNumber()];
		int cur_card_index = _logic.switch_to_card_index(card) % 20;

		do {
			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
			int tmepIndex[] = new int[20];
			for (int i = 0; i < 20; i++) {
				tmepIndex[i] = GRR._cards_index[chi_seat_index][i] + GRR._cards_index[chi_seat_index][i + 20];
			}
			next_index++;

			if (this.has_shoot[chi_seat_index]) {
				continue;
			}
			for (int i = 0; i < 7; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			for (; chi_index < this._cannot_chi_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card % 32 || this._cannot_chi[chi_seat_index][chi_index] == card % 32 + 32) {
					break;
				}
			}

			if (chi_index != this._cannot_chi_count[chi_seat_index]) {
				continue;
			}
			for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card % 32 || this._cannot_chi[chi_seat_index][chi_index] == card % 32 + 32) {
					break;
				}
			}
			if (chi_index != GRR._discard_count[last_index]) {
				continue;
			}
			for (chi_index = 0; chi_index < GRR._discard_count[chi_seat_index]; chi_index++) {
				if (this._cannot_chi[chi_seat_index][chi_index] == card % 32 || this._cannot_chi[chi_seat_index][chi_index] == card % 32 + 32) {
					break;
				}
			}
			if (chi_index != GRR._discard_count[chi_seat_index]) {
				continue;
			}

			int card_count = 0;

			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (tmepIndex[j] < 3) {
					card_count += tmepIndex[j];
				}
			}
			if (card_count == 2) {
				continue;
			}

			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false && canEatPengWei(chi_seat_index)) {
				int type_count[] = new int[10];
				int type_eat_count[] = new int[1];
				action = _logic.check_chi(GRR._cards_index[chi_seat_index], card, type_count, type_eat_count);

				if ((action & GameConstants.WIK_LEFT) != 0) {
					boolean bAction = false;
					if (tmepIndex[cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = tmepIndex[i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index + 2]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_LEFT, seat_index, _lou_weave_item[chi_seat_index][0].nCount,
								_lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {
					boolean bAction = false;
					if (tmepIndex[cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = tmepIndex[i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index - 1]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {
					boolean bAction = false;
					if (tmepIndex[cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = tmepIndex[i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index - 1]--;
						temp_cards_index[cur_card_index - 2]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_RIGHT, seat_index, _lou_weave_item[chi_seat_index][2].nCount,
								_lou_weave_item[chi_seat_index][2].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_XXD) != 0) {
					boolean bAction = false;
					if (tmepIndex[cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = tmepIndex[i];
						}
						if (_logic.get_card_color(card) == 0) {
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index + 10]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_XXD, seat_index, _lou_weave_item[chi_seat_index][4].nCount,
								_lou_weave_item[chi_seat_index][4].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_DDX) != 0) {
					boolean bAction = false;
					if (tmepIndex[cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = tmepIndex[i];
						}
						if (_logic.get_card_color(card) == 0) {
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index + 10]--;
							temp_cards_index[cur_card_index + 10]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_DDX, seat_index, _lou_weave_item[chi_seat_index][5].nCount,
								_lou_weave_item[chi_seat_index][5].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_EQS) != 0) {
					boolean bAction = false;
					if (tmepIndex[cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = tmepIndex[i];
						}
						int index[] = { 1, 6, 9 };
						int temp_index = ((_logic.get_card_color(card) == 1) ? 10 : 0);
						temp_cards_index[temp_index + index[0]]--;
						temp_cards_index[temp_index + index[1]]--;
						temp_cards_index[temp_index + index[2]]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card % 32, GameConstants.WIK_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_EQS, seat_index, _lou_weave_item[chi_seat_index][3].nCount,
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
			if (seat_index == i || this.has_shoot[i]) {
				continue;
			}
			playerStatus = _playerStatus[i];
			int card_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if ((this.GRR._cards_index[i][j] + this.GRR._cards_index[i][j + 20]) < 3)
					card_count += this.GRR._cards_index[i][j] + this.GRR._cards_index[i][j + 20];
			}
			if (card_count == 2) {
				continue;
			}
			int peng_index = 0;
			for (; peng_index < this._cannot_peng_count[i]; peng_index++) {
				if (this._cannot_peng[i][peng_index] == card || this._cannot_peng[i][peng_index] == card % 32) {
					break;
				}
			}
			if (peng_index != this._cannot_peng_count[i]) {
				continue;
			}
			if (_playerStatus[i].lock_huan_zhang() == false && canEatPengWei(i)) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card % 32);
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

	// 检查杠牌,有没有胡的
	public boolean estimate_gang_respond_hh(int seat_index, int card, int _action) {
		// 变量定义
		boolean isGang = _action == GameConstants.WIK_GANG ? true : false;// 补张
																			// 不算抢杠胡

		boolean bAroseAction = false;// 出现(是否)有

		return bAroseAction;
	}

	// 添加落牌组合
	public boolean add_lou_weave_yzchz(int luoCode, int target_player, int target_card, int provide_player, LouWeaveItem lou_weave_item) {
		if (luoCode < 0)
			return false;
		int next_index = ((luoCode & 0xf0) >> 4) - 1;
		int first_index = (luoCode & 0xf) - 1;
		if (next_index < 0 || first_index < 0)
			return false;
		if (first_index > 0) {
			int cbRemoveCard[] = new int[3];
			_logic.get_weave_card(lou_weave_item.nLouWeaveKind[next_index][first_index], target_card, cbRemoveCard);
			if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
				this.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			int wIndex = this.GRR._weave_count[target_player]++;
			this.GRR._weave_items[target_player][wIndex].public_card = 1;
			this.GRR._weave_items[target_player][wIndex].center_card = target_card;
			this.GRR._weave_items[target_player][wIndex].weave_kind = lou_weave_item.nLouWeaveKind[next_index][first_index];
			this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
			this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
		}
		first_index = 0;
		int cbRemoveCard[] = new int[3];
		_logic.get_weave_card(lou_weave_item.nLouWeaveKind[next_index][first_index], target_card, cbRemoveCard);
		if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
			this.log_player_error(target_player, "吃牌删除出错");
			return false;
		}
		int wIndex = this.GRR._weave_count[target_player]++;
		this.GRR._weave_items[target_player][wIndex].public_card = 1;
		this.GRR._weave_items[target_player][wIndex].center_card = target_card;
		this.GRR._weave_items[target_player][wIndex].weave_kind = lou_weave_item.nLouWeaveKind[next_index][first_index];
		this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
		this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);

		return true;
	}

	// 添加落牌组合
	public boolean add_lou_weave(int luoCode, int target_player, int target_card, int provide_player, LouWeaveItem lou_weave_item) {
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			}
		}
		return bSuccess;
	}

	public void setWeaveCards_xpphz(int wIndex, int target_player, int target_card, int cbRemoveCard[]) {
		int k = 0;
		int cardsIndex[] = Arrays.copyOf(this.GRR._cards_index[target_player], Constants_XPPHZ.MAX_HH_INDEX);
		if (this.GRR._cards_index[target_player][_logic.switch_to_card_index(_logic.toLowCard(cbRemoveCard[0]))] > 0) {
			cardsIndex[_logic.switch_to_card_index(_logic.toLowCard(cbRemoveCard[0]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = _logic.toLowCard(cbRemoveCard[0]);
		} else {
			cardsIndex[_logic.switch_to_card_index(_logic.toUpCard(cbRemoveCard[0]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = _logic.toUpCard(cbRemoveCard[0]);
		}
		if (this.GRR._cards_index[target_player][_logic.switch_to_card_index(_logic.toLowCard(cbRemoveCard[1]))] > 0) {
			cardsIndex[_logic.switch_to_card_index(_logic.toLowCard(cbRemoveCard[1]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = _logic.toLowCard(cbRemoveCard[1]);
		} else {
			cardsIndex[_logic.switch_to_card_index(_logic.toUpCard(cbRemoveCard[1]))]--;
			this.GRR._weave_items[target_player][wIndex].weave_card[k++] = _logic.toUpCard(cbRemoveCard[1]);
		}
		this.GRR._weave_items[target_player][wIndex].weave_card[k++] = target_card;
	}

	public boolean add_lou_weave_xpphz(int luoCode, int target_player, int target_card, int provide_player, LouWeaveItem lou_weave_item) {
		int tempCard = target_card;
		if (target_card > 32) {
			tempCard -= 32;
		}
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
				int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2, target_card };

				this.setWeaveCards_xpphz(this.GRR._weave_count[target_player], target_player, target_card, cbRemoveCard);
				if (!this._logic.remove_cards_by_index_xpphz(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_LEFT;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi_xpphz(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_CENTER: {
				int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1, target_card };
				this.setWeaveCards_xpphz(this.GRR._weave_count[target_player], target_player, target_card, cbRemoveCard);
				if (!this._logic.remove_cards_by_index_xpphz(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_CENTER;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi_xpphz(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_RIGHT: {
				int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2, target_card };
				this.setWeaveCards_xpphz(this.GRR._weave_count[target_player], target_player, target_card, cbRemoveCard);
				if (!this._logic.remove_cards_by_index_xpphz(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_RIGHT;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi_xpphz(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_DDX: {
				// 删除扑克
				int target_card_color = this._logic.get_card_color(tempCard);

				int cbRemoveCard[] = new int[3];
				if (target_card_color == 0) {
					cbRemoveCard[0] = target_card + 16;
					cbRemoveCard[1] = target_card + 16;
				} else {
					cbRemoveCard[0] = target_card - 16;
					cbRemoveCard[1] = target_card;
				}
				cbRemoveCard[2] = target_card;
				this.setWeaveCards_xpphz(this.GRR._weave_count[target_player], target_player, target_card, cbRemoveCard);
				if (!this._logic.remove_cards_by_index_xpphz(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_DDX;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi_xpphz(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_XXD: {
				// 删除扑克
				int target_card_color = this._logic.get_card_color(tempCard);

				int cbRemoveCard[] = new int[3];
				if (target_card_color == 0) {
					cbRemoveCard[0] = target_card;
					cbRemoveCard[1] = target_card + 16;
				} else {
					cbRemoveCard[0] = target_card - 16;
					cbRemoveCard[1] = target_card - 16;
				}
				cbRemoveCard[2] = target_card;
				this.setWeaveCards_xpphz(this.GRR._weave_count[target_player], target_player, target_card, cbRemoveCard);
				if (!this._logic.remove_cards_by_index_xpphz(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_XXD;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi_xpphz(this.GRR._weave_items[target_player][wIndex]);
				break;
			}
			case GameConstants.WIK_EQS: {
				// 删除扑克
				int cbRemoveCard[] = new int[] { target_card, target_card, target_card };
				int target_card_value = this._logic.get_card_value(tempCard);
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
				this.setWeaveCards_xpphz(this.GRR._weave_count[target_player], target_player, target_card, cbRemoveCard);
				if (!this._logic.remove_cards_by_index_xpphz(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_EQS;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi_xpphz(this.GRR._weave_items[target_player][wIndex]);
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
		// WalkerGeek 替换获取牌桌人数方式 没问题的话下个版本删除注释
		/*
		 * if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)||is_mj_type(
		 * GameConstants.GAME_TYPE_LHQ_QD_SAPP)) count = 4; else count = 3;
		 */
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
			{
				this._handler.handler_player_be_in_room(this, seat_index);
				if(this.isCoinRoom())
				{
					for(int i = 0; i< this.getTablePlayerNumber();i++)
						this.handler_request_trustee(i, this.istrustee[i],0);
				}
					
			}
			

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
		if (is_sys()) {
			return true;
		}
		if (this.getRuleValue(GameConstants.GAME_RULE_CAN_LESS) == 1) {
			return handler_player_ready(seat_index, false);
		}
		if (this._cur_round > 0) {
			if (!(_game_type_index == GameConstants.GAME_TYPE_LHQ_HD || _game_type_index == GameConstants.GAME_TYPE_LHQ_QD
					|| _game_type_index == GameConstants.GAME_TYPE_LHQ_QD_SAPP || _game_type_index == GameConstants.GAME_TYPE_LHQ_HY)) {
				return handler_player_ready(seat_index, false);
			}
		}
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
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
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
	public void countChiHuTimes_lhq(int target_player, int seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[target_player];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
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
	 * @param _seat_index
	 */
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		if (isZimo) {
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];
		} else if (is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
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

		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
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
		countCardType(chr, seat_index);
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

		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX) && has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
			calculate_score = all_hu_xi;
			if (seat_index == provide_index) {
				calculate_score += 3;
			}
		} else if (seat_index == provide_index) {
			calculate_score += 1;
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
	public void process_chi_hu_player_score_lhq(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
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
		int wFanShu = 0;
		int di_fen = 1;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_TWO))
			di_fen = 2;
		if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)) {
			if (this.has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
				calculate_score = di_fen + (all_hu_xi - 6) / 3;

				wFanShu = _logic.get_chi_hu_action_rank_lhq(seat_index, chr);// 番数
				this._fan_shu[seat_index] = wFanShu;
				if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
					boolean zi_mo = false;
					if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;
					} else if (!(chr.opr_and(GameConstants.CHR_SPECAIL_TIAN_HU)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;

					} else if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;

					}

				} else {
					boolean zi_mo = false;
					if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;
					} else if (!(chr.opr_and(GameConstants.CHR_SPECAIL_TIAN_HU)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;

					}
				}
			}
			if (this.has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
				wFanShu = _logic.get_chi_hu_action_rank_lhq_oho_hd(seat_index, chr);// 番数
				calculate_score = all_hu_xi;
				if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
					boolean zi_mo = false;
					if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;
					} else if (!(chr.opr_and(GameConstants.CHR_SPECAIL_TIAN_HU)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;
					} else if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty() && zi_mo == false) {
						wFanShu *= 2;
						zi_mo = true;
					}

				}
			}

		}
		if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)) {
			if (this.has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
				calculate_score = di_fen + (all_hu_xi - 6) / 3;

				wFanShu = _logic.get_chi_hu_action_rank_lhq_hy(seat_index, chr);// 番数
				this._fan_shu[seat_index] = wFanShu;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
				wFanShu = _logic.get_chi_hu_action_rank_lhq_oho_hy(seat_index, chr);// 番数
				calculate_score = all_hu_xi;
			}
			if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
				boolean zi_mo = false;
				if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty() && zi_mo == false) {
					wFanShu *= 2;
					zi_mo = true;
				} else if (!(chr.opr_and(GameConstants.CHR_SPECAIL_TIAN_HU)).is_empty() && zi_mo == false) {
					wFanShu *= 2;
					zi_mo = true;
				} else if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty() && zi_mo == false) {
					wFanShu *= 2;
					zi_mo = true;
				}

			}
		}
		if (is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
			if (this.has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
				calculate_score = di_fen + (all_hu_xi - 6) / 3;

				wFanShu = _logic.get_chi_hu_action_rank_lhq_qd(seat_index, chr);// 番数
				this._fan_shu[seat_index] = wFanShu;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
				wFanShu = _logic.get_chi_hu_action_rank_lhq_oho_qd(seat_index, chr);// 番数
				calculate_score = all_hu_xi;
			}
			boolean zi_mo = false;
			if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
				if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty() && zi_mo == false) {
					wFanShu *= 2;
					zi_mo = true;
				} else if (!(chr.opr_and(GameConstants.CHR_SPECAIL_TIAN_HU)).is_empty() && zi_mo == false) {
					wFanShu *= 2;
					zi_mo = true;
				} else if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty() && zi_mo == false) {
					wFanShu *= 2;
					zi_mo = true;
				}

			}
		}

		calculate_score += GRR._count_pick_niao;
		this._tun_shu[seat_index] = calculate_score;

		this._fan_shu[seat_index] = wFanShu;
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
		} else {
			// 胡牌分
			lChiHuScore *= (this.getTablePlayerNumber() - 1);
			GRR._game_score[provide_index] -= lChiHuScore;
			GRR._game_score[seat_index] += lChiHuScore;
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
	public void process_chi_hu_player_score_thk(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
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
		if (all_hu_xi == 10)
			calculate_score = 3;
		else if (all_hu_xi == 11)
			calculate_score = 1;
		else if (all_hu_xi >= 12 && all_hu_xi <= 14)
			calculate_score = 2;
		else if (all_hu_xi >= 15 && all_hu_xi < 18)
			calculate_score = 3;
		else if (all_hu_xi >= 18)
			calculate_score = 3 + (all_hu_xi - 15) / 3;

		int wFanShu = _logic.get_chi_hu_action_rank_thk(seat_index, chr);// 番数
		if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			wFanShu *= 2;
		}
		calculate_score += GRR._count_pick_niao;

		if (this.has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_TWO))
			calculate_score = calculate_score + 1;
		else if (this.has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_THREE))
			calculate_score *= 2;
		this._tun_shu[seat_index] = calculate_score;

		this._fan_shu[seat_index] = wFanShu;

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
		} else {
			// 胡牌分
			lChiHuScore *= (this.getTablePlayerNumber() - 1);
			GRR._game_score[provide_index] -= lChiHuScore;
			GRR._game_score[seat_index] += lChiHuScore;
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
	public void process_chi_hu_player_score_phz_chd(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
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
		if (seat_index == provide_index) {
			calculate_score += 1;
		}

		int wFanShu = _logic.get_chi_hu_action_rank_phz_chd(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
				_tuan_yuan_count[seat_index], this._huang_zhang_count, this._hong_pai_count[seat_index], chr);// 番数

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
	public void process_chi_hu_player_score_yl(int seat_index, int provide_index, int operate_card, boolean zimo) {

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
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

		int wFanShu = _logic.get_chi_hu_action_rank_yl(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
				this._hong_pai_count[seat_index], chr);// 番数
		if (this._huang_zhang_count > 0) {
			wFanShu += 4 + (this._huang_zhang_count - 1);
			chr.opr_or(GameConstants.CHR_HUANG_FAN_CHD);
		}

		if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_ZIMO_ADD_1) == 1) {
				calculate_score += 1;
			} else
				wFanShu += 2;
		}

		if (wFanShu == 0)
			wFanShu = 1;
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
	public void process_chi_hu_player_score_hgw(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
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

		int wFanShu = _logic.get_chi_hu_action_rank_hgw(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
				this._hong_pai_count[seat_index], chr);// 番数
		if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_TH_4_F) == 1)
				wFanShu += 4;
			else
				wFanShu += 5;
		}
		if (!(chr.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_DH_4_F) == 1)
				wFanShu += 4;
			else
				wFanShu += 3;
		}
		if (!(chr.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_PPH_4_F) == 1)
				wFanShu += 4;
			else
				wFanShu += 5;
		}
		if (!(chr.opr_and(GameConstants.CHR_DA_HU)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_18DH_4_F) == 1)
				wFanShu += 4 + (_da_pai_count[seat_index] - 18);
			else {
				wFanShu += 5 + (_da_pai_count[seat_index] - 18);
			}
		}
		if (!(chr.opr_and(GameConstants.CHR_XIAO_HU)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_16XH_4_F) == 1)
				wFanShu += 4 + (_xiao_pai_count[seat_index] - 16);
			else {
				wFanShu += 5 + (_xiao_pai_count[seat_index] - 16);
			}
		}

		if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			if (this.getRuleValue(GameConstants.GAME_RULE_HGW_ZIMO_ADD_1) == 1) {
				calculate_score += 1;
			} else
				wFanShu += 2;
		}

		if (wFanShu == 0)
			wFanShu = 1;
		if (has_rule(GameConstants.GAME_RULE_NO_FEN_DING))
			wFanShu = wFanShu;
		else if (has_rule(GameConstants.GAME_RULE_FIVE_FAN)) {
			if (wFanShu > 5)
				wFanShu = 5;
		} else if (has_rule(GameConstants.GAME_RULE_TEN_FAN)) {
			if (wFanShu > 10)
				wFanShu = 10;
		}
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
	public void process_chi_hu_player_score_wmq(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		this._hu_xi[seat_index] = this._hu_pai_max_hu[seat_index];
		int calculate_score = this._hu_xi[seat_index];

		int wFanShu = 0;
		int hu_xi = 0;
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_lmt_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_lmt_wmq(seat_index, chr);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_qmt_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_qmt_wmq(seat_index, chr);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_xzb_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_xzb_wmq(seat_index, chr);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_dzb_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_dzb_wmq(seat_index, chr);// 番数

		}
		if (this._hu_xi[seat_index] - 18 > 0)
			operate_dou_liu_zi(seat_index, true, this._hu_xi[seat_index] - 18);
		else if (this._cur_round == this._game_round)
			operate_dou_liu_zi(seat_index, true, 0);
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

		float lChiHuScore = wFanShu * calculate_score + hu_xi;
		_all_xi = (int) lChiHuScore;

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
	public void process_chi_hu_player_score_hgw468(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
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

		int wFanShu = _logic.get_chi_hu_action_rank_hgw468(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
				this._hong_pai_count[seat_index], chr);// 番数
		if (!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			wFanShu += 6;
		}
		if (!(chr.opr_and(GameConstants.CHR_DUI_ZI_HU)).is_empty()) {
			wFanShu += 8;
		}
		if (!(chr.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
			calculate_score += 1;
		}
		if (wFanShu == 0)
			wFanShu = 1;
		/*
		 * if (has_rule(GameConstants.GAME_RULE_NO_FEN_DING)) wFanShu = wFanShu;
		 * else if (has_rule(GameConstants.GAME_RULE_FIVE_FAN)) { if (wFanShu >
		 * 5) wFanShu = 5; } else if (has_rule(GameConstants.GAME_RULE_TEN_FAN))
		 * { if (wFanShu > 10) wFanShu = 10; }
		 */
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
	public void process_chi_hu_player_score_phz_xt(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
		// 计算胡息
		int all_hu_xi = 0;
		// for(int i =0; i< this.GRR._weave_count[seat_index] ; i++){
		// all_hu_xi += this.GRR._weave_items[seat_index][i].hu_xi;
		// }
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = 1;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_ONE))
			calculate_score = 1;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_TWO))
			calculate_score = 2;
		if (has_rule(GameConstants.GAME_RULE_DI_FEN_SELECT_THREE))
			calculate_score = 3;
		// if (has_rule(GameConstants.GAME_RULE_TEN_HU_XI_XT)) {
		// calculate_score = calculate_score + (all_hu_xi - 10) / 3;
		// } else if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI))
		// {
		// if ((!(chr.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) &&
		// (all_hu_xi < 18))
		// calculate_score = calculate_score;
		// else
		// calculate_score = calculate_score + (all_hu_xi - 18) / 3;
		//
		// } else if (has_rule(GameConstants.GAME_RUEL_FIFTEEN_HU_XI_XT))
		if (has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
			calculate_score = all_hu_xi;
			if (this.has_rule(GameConstants.GAME_RULE_NO_MING_TANG) == false) {
				if (seat_index == provide_index) {
					calculate_score += 3;
				}
			}
		} else {
			calculate_score = calculate_score + (all_hu_xi - 15) / 3;
			if (this.has_rule(GameConstants.GAME_RULE_NO_MING_TANG) == false) {
				if (seat_index == provide_index) {
					calculate_score += 1;
				}
			}
		}

		int wFanShu = 1;
		if (this.has_rule(GameConstants.GAME_RULE_NO_MING_TANG) && this.has_rule(GameConstants.GAME_RULE_30_HU_FAN_FAN)) {
			if (all_hu_xi >= 30) {
				wFanShu = 2;
			}
		}
		if (this.has_rule(GameConstants.GAME_RULE_JI_BENMING_TANG)) {
			wFanShu = _logic.get_chi_hu_action_rank_hh(chr);// 番数
		} else if (this.has_rule(GameConstants.GAME_RULE_ALL_MING_TANG)) {
			wFanShu = _logic.get_chi_hu_action_rank_phz_xt(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					_tuan_yuan_count[seat_index], this._huang_zhang_count, this._hong_pai_count[seat_index], chr);// 番数
		}
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
		countCardType(chr, seat_index);
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

		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
			if (has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
				if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
					calculate_score = 1 + (all_hu_xi - 18);
				} else {
					calculate_score = 1 + (all_hu_xi - 15);
				}
			} else // if (has_rule(GameConstants.GAME_RULE_3_XI_YI_TUN))
			{
				if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
					calculate_score = 1 + (all_hu_xi - 18) / 3;
				} else {
					calculate_score = 1 + (all_hu_xi - 15) / 3;
				}
			}

		} else if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.isCoinRoom()) {
					GRR._game_score[i] *= this.getSettleBase(i);
				}
			}
		}

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

	public boolean handler_release_room_in_gold(Player player, int opr_code) {
		if (player == null || player.get_seat_index() == GameConstants.INVALID_SEAT)
			return false;
		if (opr_code != GameConstants.Release_Room_Type_QUIT)
			return false;
		if (this.isEnd) {
			return false;
		}

		int seat_index = player.get_seat_index();
		if (GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status || this.isEnd) {
			force_kick_player_out_room(seat_index, "您已退出该游戏");
		} else {// 游戏中
				// 逃跑扣金币
			SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
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
						_player_result.game_score[i] += ((float) (-cost)) / beilv;
						GRR._game_score[i] += ((float) (-cost)) / beilv;
					} else {
						_player_result.game_score[i] += ((float) (each_get)) / beilv;
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
			// _player_open_less[seat_index] = 0;
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
			return this.handler_release_room_in_gold(player, opr_code);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();

			if (GameConstants.INVALID_SEAT == seat_index) {
				MongoDBServiceImpl.getInstance().server_error_log(getRoom_id(), ELogType.unkownError, ThreadUtil.getStack(),
						player == null ? 0 : player.getAccount_id(), "座位号-1", getGame_id());
			}
		}
		int gameId = is_mj_type(GameConstants.GAME_TYPE_PHZ_LD_FANG_PAO_FA) ? getGame_id() : 1;
		gameId = is_mj_type(GameConstants.GAME_TYPE_PHZ_AH) ? getGame_id() : 1;
		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(3007);
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

			if (seat_index == GameConstants.INVALID_SEAT) {

				MongoDBServiceImpl.getInstance().server_error_log(getRoom_id(), ELogType.unkownError, ThreadUtil.getStack(),
						player == null ? 0 : player.getAccount_id(), Arrays.toString(get_players()));

				send_error_notify(player, 2, "您已退出该游戏");

				if (player != null) {
					PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
				}
				if (_kick_schedule != null) {
					_kick_schedule.cancel(false);
					_kick_schedule = null;
				}

				seat_index = getPlayerIndex(player.getAccount_id());
				this.get_players()[seat_index] = null;
				_player_ready[seat_index] = 0;

				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				this.load_player_info_data(refreshroomResponse);
				//
				send_response_to_other(seat_index, refreshroomResponse);

				// 通知代理
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);

				return true;
			}

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
		if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)) {
			if (has_rule(GameConstants.GAME_RULE_ADD_SPEED)) {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(750);
			} else {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(1500);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)) {
			if (has_rule(GameConstants.GAME_RULE_ADD_SPEED)) {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(750);
			} else {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(1000);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_PHZ_LEI_YANG)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(500);
		} else if ((is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ))
				&& this.getRuleValue(GameConstants.GAME_RULE_HGW_SPEED_ADD) == 1) {
			roomResponse.setFlashTime(1);
			roomResponse.setStandTime(500);
		} else if ((is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ))
				|| is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(750);
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
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) && GRR != null) {
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
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		// 刷新自己手牌
		int count = _logic.switch_to_cards_data(GRR._cannot_out_index[seat_index], cards);
		roomResponse.setType(MsgConstants.RESPONSE_CANNOT_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardCount(count);
		for (int i = 0; i < count; i++) {
			if (GRR._can_ting_out_index[seat_index][_logic.switch_to_card_index(cards[i])] == 1)
				roomResponse.addCardData((cards[i] | 0x100));
			else
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
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
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
		PerformanceTimer timer = new PerformanceTimer();
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
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (timer.get() > 10) {
			this.log_warn("seat_index="+seat_index+"pao huzi  ting card cost time = " + timer.duration() + "  and cards is =" + Arrays.toString(cbCardIndexTemp)
					+ "Arrays weaveItem" + Arrays.toString(weaveItem));
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
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX) || is_mj_type(GameConstants.GAME_TYPE_THK_HY) || is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_SYBP) || is_mj_type(GameConstants.GAME_TYPE_PHZ_NX)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_LEI_YANG) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| (is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ))
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_XP) || is_mj_type(GameConstants.GAME_TYPE_PHZ_WU_GANG)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_PHZ_CHEN_ZHOU) || is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)
				|| is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)
				|| is_mj_type(GameConstants.GAME_TYPE_PHZ_LD_FANG_PAO_FA) || is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU))
			roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION_RECORD);
		else
			roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);
		if (GRR == null) {
			return true;
		}
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
			if (GRR != null)
				GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU)) {
			for (int i = 0; i < curPlayerStatus._weave_count; i++) {
				curPlayerStatus.set_operate_card(curPlayerStatus._action_weaves[i].center_card);
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
				weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
				weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
				weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
				weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
				int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX
						| GameConstants.WIK_XXD | GameConstants.WIK_EQS | GameConstants.WIK_YWS;
				// this.log_error("weave.kind" +
				// curPlayerStatus._action_weaves[i].weave_kind + "center_card"
				// + curPlayerStatus._action_weaves[i].center_card);
				if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
					for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
						ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
						chi_group.setLouWeaveCount(j);
						for (int k = 0; k < curPlayerStatus._action_weaves[i].sub_lou_qi_count[j]; k++) {
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

		} else {
			for (int i = 0; i < curPlayerStatus._weave_count; i++) {
				curPlayerStatus.set_operate_card(curPlayerStatus._action_weaves[i].center_card);
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
				weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
				weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
				weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
				weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
				int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX
						| GameConstants.WIK_XXD | GameConstants.WIK_EQS | GameConstants.WIK_YWS;
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

		}
		if (GRR != null)
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

		roomResponse.setFlashTime(150);
		if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)) {
			if (has_rule(GameConstants.GAME_RULE_ADD_SPEED)) {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(750);
			} else {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(1500);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)) {
			if (has_rule(GameConstants.GAME_RULE_ADD_SPEED)) {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(750);
			} else {
				roomResponse.setFlashTime(150);
				roomResponse.setStandTime(1000);
			}
		} else if (is_mj_type(GameConstants.GAME_TYPE_PHZ_LEI_YANG)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(500);
		} else if ((is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ))
				&& this.getRuleValue(GameConstants.GAME_RULE_HGW_SPEED_ADD) == 1) {
			roomResponse.setFlashTime(1);
			roomResponse.setStandTime(500);
		} else if ((is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ))
				|| is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(750);
		} else {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(1000);

		}

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

				if ((weaveitems[j].weave_kind == GameConstants.WIK_TI_LONG || weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG
						|| weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG_LIANG) && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) && this._xt_display_an_long[seat_index] == true)
						weaveItem_item.setCenterCard(0);
					else {
						weaveItem_item.setCenterCard(weaveitems[j].center_card);

					}
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
		// if(GRR != null)
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
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);

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

	@Override
	public String get_game_des() {
		if (this.isCoinRoom() == true) {
			if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_YX)) {
				String des = "";

				if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)) {
					des += "吃：一五十" + " ";
				}
				if (has_rule(GameConstants.GAME_RULE_QIANG_ZHI_HU_PAI)) {
					des += "强制胡牌" + "\n";
				}
				if (has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)) {
					des += "18硬息" + "\n";
				} else {
					des += "15胡息 " + "\n";
				}
				if (has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
					des += "一胡一分" + " ";
				}
				if (has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
					des += "一息一囤" + " ";
				}
				if (has_rule(GameConstants.GAME_RULE_3_XI_YI_TUN)) {
					des += "三息一囤" + " ";
				}
				if (has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
					des += "亮牌" + "\n";
				} else {
					des += "不亮牌" + "\n";
				}

				return des;
			}

		}
		return super.get_game_des();
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
			_cur_banker = 0;
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	@Override
	public void log_info(String info) {

		// logger.info("房间[" + this.getRoom_id() + "]" + info);

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
	private void set_result_describe_lhq() {

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			boolean zi_mo = false;
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
							if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",天胡×2";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",天胡×2";
									zi_mo = true;
								}
							} else {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",天胡×2";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",天胡×2";
									zi_mo = true;
								}
							}
							if (type == GameConstants.CHR_JEI_PAO_HU) {
								if (l == 1)
									des += ",平胡";
							}

							if (type == GameConstants.CHR_DI_HU) {
								des += ",地胡 ×2";
							}
							if (type == GameConstants.CHR_DIAN_PAO_HU) {
								des += ",点炮胡 ";
							}
							if (type == GameConstants.CHR_TEN_HONG_PAI) {
								des += ",小红×2";
							}

							if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
								des += ",大红×4";
							}
							if (type == GameConstants.CHR_ONE_HONG) {
								des += ",一点红×3";
							}
							if (type == GameConstants.CHR_ALL_HEI) {
								des += ",黑胡×5";
							}
							if (type == GameConstants.CHR_HAI_HU) {
								des += ",海底捞月 ×2";
							}
						}
						if (has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
							if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",自摸×2";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",自摸×2";
									zi_mo = true;
								}
							} else {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",自摸";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",自摸";
									zi_mo = true;
								}
							}

							if (type == GameConstants.CHR_JEI_PAO_HU) {
								if (l == 1)
									des += ",平胡";
							}
							if (type == GameConstants.CHR_DIAN_PAO_HU) {
								des += ",点炮胡 ";
							}
							if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
								des += ",大红×2";
							}
							if (type == GameConstants.CHR_ALL_HEI) {
								des += ",黑胡×2";
							}

						}
					}
					if (is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
							if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",自摸×2";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",自摸×2";
									zi_mo = true;
								}
							} else {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",自摸";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",自摸";
									zi_mo = true;
								}
							}
							if (type == GameConstants.CHR_JEI_PAO_HU) {
								if (l == 1)
									des += ",平胡";
							}

							if (type == GameConstants.CHR_DIAN_PAO_HU) {
								des += ",点炮胡 ";
							}
							if (type == GameConstants.CHR_TEN_HONG_PAI) {
								des += ",小红×2";
							}

							if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
								des += ",大红×4";
							}
							if (type == GameConstants.CHR_ONE_HONG) {
								des += ",一点红×3";
							}
							if (type == GameConstants.CHR_ALL_HEI) {
								des += ",黑胡×5";
							}
						}
						if (has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {
							if (type == GameConstants.CHR_JEI_PAO_HU) {
								if (l == 1)
									des += ",平胡";
							}
							if (type == GameConstants.CHR_DIAN_PAO_HU) {
								des += ",点炮胡 ";
							}
							if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
								des += ",红胡×2";
							}
							if (type == GameConstants.CHR_ALL_HEI) {
								des += ",黑胡×2";
							}
							if (type == GameConstants.CHR_ONE_HONG) {
								des += ",一点红×2";
							}
						}
					}
					if (is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
						if (has_rule(GameConstants.GAME_RULE_ONE_TUN_ONE_FEN)) {
							if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI)) {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",自摸×2";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",自摸×2";
									zi_mo = true;
								}
							} else {
								if (type == GameConstants.CHR_TIAN_HU) {
									des += ",自摸";
									zi_mo = true;
								}
								if (type == GameConstants.CHR_SPECAIL_TIAN_HU) {
									des += ",自摸";
									zi_mo = true;
								}
							}
							if (type == GameConstants.CHR_JEI_PAO_HU) {
								if (l == 1)
									des += ",平胡";
							}

							if (type == GameConstants.CHR_DIAN_PAO_HU) {
								des += ",点炮胡 ";
							}
							if (type == GameConstants.CHR_TEN_HONG_PAI) {
								des += ",红胡×2";
							}
							if (type == GameConstants.CHR_ONE_HONG) {
								des += ",一点红×3";
							}
							if (type == GameConstants.CHR_ALL_HEI) {
								des += ",黑胡×5";
							}
						}
						if (has_rule(GameConstants.GAME_RULE_ONE_HU_ONE_FEN)) {

							if (type == GameConstants.CHR_JEI_PAO_HU) {
								if (l == 1)
									des += ",平胡";
							}
							if (type == GameConstants.CHR_DIAN_PAO_HU) {
								des += ",点炮胡 ";
							}
							if (type == GameConstants.CHR_TEN_HONG_PAI) {
								des += ",红胡×2";
							}
							if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
								des += ",红胡×2";
							}
							if (type == GameConstants.CHR_ALL_HEI) {
								des += ",黑胡×2";
							}

						}

					}
					if (has_rule(GameConstants.GAME_RULE_ZI_MO_TWO_BEI) && zi_mo == false) {
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸 ×2 ";
						}
					} else if (zi_mo == false) {
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸  ";
						}
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_wmq() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡 ×4";
						}
						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对×6";
						}

						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",乌胡 ×2";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",点胡×3";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花+50";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡+10";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红+" + (10 + 10 * (this._hong_pai_count[i] - 10));
						}
						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡+50";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡+50";
						}
					}
				}
				if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
					des += ",对子胡 ×10";
				}
				if (type == GameConstants.CHR_DIAN_HU_WMQ) {
					des += ",点胡×6";
				}
				if (type == GameConstants.CHR_WU_HU_WMQ) {
					des += ",乌胡 ×8";
				}
				if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
					des += ",满园花+150";
				}
				if (type == GameConstants.CHR_HONG_HU_WMQ) {
					des += ",红胡+30";
				}
				if (type == GameConstants.CHR_DUO_HONG_WMQ) {
					des += ",多红+" + (30 + 30 * (this._hong_pai_count[i] - 10));
				}
				if (type == GameConstants.CHR_YING_HU_WMQ) {
					des += ",印胡+" + this._ying_hu_count[i] * 30;
				}
				if (type == GameConstants.CHR_CHUN_YING_WMQ) {
					des += ",纯印+" + this._chun_ying_count[i] * 150;
				}

				if (type == GameConstants.CHR_WU_DUI_WMQ) {
					des += ",对子胡+200";
				}

				if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
					des += ",大字胡 +150";
				}
				if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
					des += ",小字胡+150";
				}
				if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
					des += ",桌胡+40";
				}
				if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
					des += ",姐妹桌+80";
				}
				if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
					des += ",三乱桌+120";
				}
				if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
					des += ",姐妹带拖桌+150";
				}
				if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
					des += ",爹孙桌+300";
				}
				if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
					des += ",爹孙带拖桌+450";
				}
				if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
					des += ",四乱桌+300";
				}
				if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
					des += ",海底胡+30";
				}
				if (type == GameConstants.CHR_DAN_DI_WMQ) {
					des += ",单丁+30";
				}
				if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
					des += ",对子胡单丁+50";
				}
				if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
					des += ",真八碰头+300";
				}

				if (type == GameConstants.CHR_JIA_BA_WMQ) {
					des += ",假八碰头+200";
				}
				if (type == GameConstants.CHR_BEI_KAO_BEI) {
					des += ",背靠背+50";
				}
				if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
					des += ",手牵手+50";
				}
				if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
					des += ",全球人+150";
				}
				if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
					des += ",上下五千年+50";
				}
				if (type == GameConstants.CHR_KA_WEI_WMQ) {
					des += ",卡偎+50";
				}
				if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
					des += ",龙摆尾+150";
				}
				if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
					des += ",项对+50";
				}

				if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
					des += ",飘对+50";
				}
				if (type == GameConstants.CHR_JI_DING_WMQ) {
					des += ",鸡丁+100";
				}
				if (this.has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
					if (type == GameConstants.CHR_TIAN_HU_WMQ) {
						des += ",天胡+100";
					}
				} else if (type == GameConstants.CHR_TIAN_HU_WMQ) {
					des += ",天胡+150";
				}
				if (type == GameConstants.CHR_ALL_HEI_TIAN_HU) {
					des += ",全黑天胡+150";
				}
				if (type == GameConstants.CHR_NO_TEN_XI_TIAN_HU) {
					des += ",天胡+150";
				}
				if (type == GameConstants.CHR_LDH_TIAN_HU) {
					des += ",天胡+150";
				}
				if (type == GameConstants.CHR_JIU_DUI_TIAN_HU) {
					des += ",天胡+150";
				}
				if (type == GameConstants.CHR_SBD_TIAN_HU) {
					des += ",天胡+150";
				}
				if (type == GameConstants.CHR_BIAN_KAN_HU) {
					des += ",边坎+30";
				}
				if (type == GameConstants.CHR_ZHEN_BKB_WMQ) {
					des += ",真背靠背+100";
				}
				if (type == GameConstants.CHR_KA_HU_WMQ) {
					des += ",卡胡+50";
				}
				if (type == GameConstants.CHR_ZHA_DAN_WMQ) {
					des += ",炸弹+150";
				}
				if (type == GameConstants.CHR_FBW_WMQ) {
					des += ",凤摆尾+50";
				}

			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_thk() {

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸翻2倍";
					}

					if (type == GameConstants.CHR_DIAN_PAO_HU) {
						des += ",点炮胡";
					}
					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += ",小红×2";
					}

					if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
						des += ",大红×5";
					}
					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",点胡×3";
					}
					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",黑胡×5";
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
	private void set_result_describe_hhyl() {
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
						des += ",天胡 ×4";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}

					if (getRuleValue(GameConstants.GAME_RULE_HGW_ZIMO_1_F) == 1) {
						if (type == GameConstants.CHR_ZI_MO) {

							des += ",自摸  翻倍";
						}
					} else {
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸加 +3胡息";
						}
					}

					if (type == GameConstants.CHR_DI_HU) {

						des += ",地胡 ×4";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += "," + this._hong_pai_count[i] + "红 ×" + ((this._hong_pai_count[i] - 10) + 4);
					}

					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",点胡 ×6";
					}

					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",乌胡 ×8";
					}

					if (type == GameConstants.CHR_DUI_ZI_HU) {
						des += ",碰碰胡  ×8";
					}

					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海底胡  ×4";
					}
					if (type == GameConstants.CHR_DA_HU) {
						des += "," + this._da_pai_count[i] + "大×" + (8 + this._da_pai_count[i] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU) {
						des += "," + this._xiao_pai_count[i] + "小×" + (8 + this._xiao_pai_count[i] - 16);
					}
					if (type == GameConstants.CHR_HUANG_FAN) {
						des += "," + "黄庄×" + (4 + this._huang_zhang_count - 1);
					}

					if (type == GameConstants.CHR_TUAN_CHD) {
						des += ",喜相逢×8";
					}
					if (type == GameConstants.CHR_SHUA_HOU) {
						des += ",耍猴×8";
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_hgw() {
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
						if (this.getRuleValue(GameConstants.GAME_RULE_HGW_TH_4_F) == 1)
							des += ",天胡 ×4";
						else
							des += ",天胡 ×5";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}

					if (getRuleValue(GameConstants.GAME_RULE_HGW_ZIMO_1_F) == 1) {
						if (type == GameConstants.CHR_ZI_MO) {

							des += ",自摸  ×2";
						}
					} else {
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸加 +3胡息";
						}
					}

					if (type == GameConstants.CHR_DI_HU) {
						if (this.getRuleValue(GameConstants.GAME_RULE_HGW_DH_3_F) == 1)
							des += ",地胡 ×3";
						else
							des += ",地胡 ×4";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += "," + this._hong_pai_count[i] + "红 ×" + ((this._hong_pai_count[i] - 10) + 2);
					}

					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",点胡 ×3";
					}

					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",乌胡 ×5";
					}

					if (type == GameConstants.CHR_DUI_ZI_HU) {
						if (this.getRuleValue(GameConstants.GAME_RULE_HGW_PPH_4_F) == 1)
							des += ",碰碰胡  ×4";
						else
							des += ",碰碰胡  ×5";
					}

					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海底胡  ×2";
					}
					if (type == GameConstants.CHR_DA_HU) {
						if (this.getRuleValue(GameConstants.GAME_RULE_HGW_18DH_4_F) == 1)
							des += "," + this._da_pai_count[i] + "大×" + (4 + this._da_pai_count[i] - 18);
						else
							des += "," + this._da_pai_count[i] + "大×" + (5 + this._da_pai_count[i] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU) {
						if (this.getRuleValue(GameConstants.GAME_RULE_HGW_16XH_4_F) == 1)
							des += "," + this._xiao_pai_count[i] + "小×" + (4 + this._xiao_pai_count[i] - 16);
						else
							des += "," + this._xiao_pai_count[i] + "小×" + (5 + this._xiao_pai_count[i] - 16);
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	private void set_result_describe_hgw468() {
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
						des += ",天胡 ×8";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸加 +3胡息";
					}

					if (type == GameConstants.CHR_DI_HU) {
						des += ",地胡 ×6";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += "," + this._hong_pai_count[i] + "红 ×" + ((this._hong_pai_count[i] - 10) + 4);
					}

					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",点胡 ×6";
					}

					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",乌胡 ×8";
					}

					if (type == GameConstants.CHR_DUI_ZI_HU) {
						des += ",碰碰胡  ×8";
					}

					if (type == GameConstants.CHR_HAI_HU) {
						des += ",海底胡  ×6";
					}
					if (type == GameConstants.CHR_DA_HU) {
						des += "," + this._da_pai_count[i] + "大×" + (6 + this._da_pai_count[i] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU) {
						des += "," + this._xiao_pai_count[i] + "小×" + (6 + this._xiao_pai_count[i] - 16);
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

	/**
	 * 结算描述
	 */
	private void set_result_describe_phz_xt(int seat_index) {

		if (has_rule(GameConstants.GAME_RULE_JI_BENMING_TANG)) {
			set_phz_xt_jiben_ming_tang(seat_index);
		}
		if (has_rule(GameConstants.GAME_RULE_ALL_MING_TANG)) {
			set_phz_xt_all_ming_tang(seat_index);
		}
		if (has_rule(GameConstants.GAME_RULE_ZI_MO_ADD_THREE)) {
			set_phz_xt_zimo_add_three(seat_index);
		}
		if (has_rule(GameConstants.GAME_RULE_NO_MING_TANG)) {
			set_phz_xt_no_ming_tang(seat_index);
		}

	}

	// 转转麻将结束描述
	public void set_result_describe(int seat_index) {
		if (this.is_mj_type(GameConstants.GAME_TYPE_HH_YX)) {
			set_result_describe_hh();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_HGW_HH)) {
			set_result_describe_hgw();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_YL_PHZ)) {
			set_result_describe_hhyl();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			set_result_describe_hgw468();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_CHD)) {
			set_result_describe_phz_chd(seat_index);
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT)) {
			set_result_describe_phz_xt(seat_index);
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			set_result_describe_hh();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
			set_result_describe_lhq();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_THK_HY)) {
			set_result_describe_thk();
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
			set_result_describe_wmq();
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
	public int get_real_card(int card) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}

		}
		GRR._show_bird_effect = show;
		GRR._count_niao = 0;
		if (has_rule(GameConstants.GAME_RULE_SUI_XING)) {
			GRR._cards_data_niao[0] = GRR._chi_hu_card[seat_index][0];
			GRR._count_niao++;

		}
		if (has_rule(GameConstants.GAME_RULE_FAN_XING)) {
			if (this.GRR._left_card_count == 0) {
				GRR._cards_data_niao[0] = GRR._chi_hu_card[seat_index][0];
				GRR._count_niao++;
			} else {
				GRR._cards_data_niao[0] = this._repertory_card[this._all_card_len - this.GRR._left_card_count];
				GRR._count_niao++;
				--this.GRR._left_card_count;
			}
			this.operate_player_get_card(GameConstants.INVALID_SEAT, 1, new int[] { GRR._cards_data_niao[0] }, GameConstants.INVALID_SEAT, false);
		}
		GRR._count_pick_niao = _logic.get_xing_pai_count(this._hu_weave_items[seat_index], this._hu_weave_count[seat_index], GRR._cards_data_niao[0]);

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
		boolean flag = false;
		if (delay == 300)
			flag = true;
		if (!is_mj_type(GameConstants.GAME_TYPE_PHZ_LEI_YANG))
			delay = GameConstants.PAO_SAO_TI_TIME;
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_NX)) {
			delay = 1000;
		}
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();

		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}
		if ((is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP))) {
			if (flag == true)
				delay = 100;
			else if (action != GameConstants.WIK_PAO)
				delay = 200;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) && has_rule(GameConstants.GAME_RULE_ADD_SPEED)) {
			SysParamModel sysParamModel1110 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1110);
			if (sysParamModel1110 != null && sysParamModel1110.getVal3() > 0 && sysParamModel1110.getVal3() < 10000) {
				delay = sysParamModel1110.getVal3();
			}
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HGW_HH) && this.getRuleValue(GameConstants.GAME_RULE_HGW_SPEED_ADD) == 1) {
			SysParamModel sysParamModel1110 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1110);
			if (sysParamModel1110 != null && sysParamModel1110.getVal3() > 0 && sysParamModel1110.getVal3() < 10000) {
				delay = sysParamModel1110.getVal3();
			}
		}
		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(this.getRoom_id(), seat_index, provide_player, center_card, action, type, depatch, self, d), delay,
					TimeUnit.MILLISECONDS);
		} else {
			// 是否有抢杠胡
			this._handler = this._handler_gang;
			this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, depatch, self, d);
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

	/**
	 * 调度,加入牌堆
	 **/
	public void cannot_outcard(int seat_index, int card_count, int card_data, boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		if (card_data != 0)
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (_logic.switch_to_card_index(card_data) == i)
					GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] += card_count;
			}

		if (send_client == true) {
			this.operate_cannot_card(seat_index, true);
		}
	}

	public void must_out_card(int seat_index, int card_count, int card_index, boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (card_index == i)
				GRR._must_out_index[seat_index][i] += card_count;
		}

		if (send_client == true)
			this.operate_must_out_card(seat_index, true);

	}

	public boolean is_can_out_card(int seat_index) {
		if (this._is_xiang_gong[seat_index] == true)
			return false;
		boolean flag = false;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (GRR._cards_index[seat_index][i] >= 1) {
				if (GRR._cannot_out_index[seat_index][i] == 0) {
					flag = true;
					break;
				}

			}

		}
		return flag;
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

	public boolean is_tian_hu(int seat_index, int provider, ChiHuRight chiHuRight, int cur_card_data) {
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN))
			return false;
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN))
			return false;
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG))
			return false;
		if (seat_index == provider)
			cur_card_data = 0;
		int hand_cards_data[] = new int[GameConstants.MAX_WMQ_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards_data);
		if (cur_card_data != 0)
			hand_cards_data[hand_card_count++] = cur_card_data;
		int hand_index_data[] = new int[GameConstants.MAX_HH_INDEX];
		int hand_index_count = _logic.switch_to_cards_index(hand_cards_data, 0, hand_card_count, hand_index_data);
		int all_hei_count = 0;
		int hong_dui = 0;
		int dui_zi = 0;
		int si_bian_dui = 0;
		chiHuRight.set_empty();
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.color_hei(hand_cards_data[i]) == true) {
				all_hei_count++;
			}
		}
		if (all_hei_count == hand_card_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI_TIAN_HU);
		}

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (_logic.color_hei(_logic.switch_to_card_data(i)) == false) {
				hong_dui += hand_index_data[i] / 2;
			}
			dui_zi += hand_index_data[i] / 2;
			if (i == 0 || i == 9 || i == 10 || i == 19) {
				if (hand_index_data[i] >= 2)
					si_bian_dui++;
			}
		}
		if (hong_dui == 6) {
			chiHuRight.opr_or(GameConstants.CHR_LDH_TIAN_HU);
		}

		if (dui_zi == 9) {
			chiHuRight.opr_or(GameConstants.CHR_JIU_DUI_TIAN_HU);
		}

		if (si_bian_dui == 4) {
			chiHuRight.opr_or(GameConstants.CHR_SBD_TIAN_HU);
		}
		if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX) == false) {
			chiHuRight.opr_or(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
		}
		if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX)) {
			chiHuRight.opr_or(GameConstants.CHR_ZHA_DAN_WMQ);
		}

		return false;
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
		istrustee[get_seat_index] = isTrustee;
		this.send_response_to_room(roomResponse);
		if (GRR == null)
			return false;
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
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU)) {
			if (has_rule(Constants_YongZhou.GAME_RULE_PLAYER_TWO))
				return 2;
			if (has_rule(Constants_YongZhou.GAME_RULE_PLAYER_THREE))
				return 3;
			if (has_rule(Constants_YongZhou.GAME_RULE_PLAYER_FOUR))
				return 4;
			return 3;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HH_YX))
			return GameConstants.GAME_PLAYER_HH;
		if ((is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ))
				&& this.getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1)
			return 2;
		else if ((is_mj_type(GameConstants.GAME_TYPE_HGW_HH) || is_mj_type(GameConstants.GAME_TYPE_YL_PHZ)))
			return GameConstants.GAME_PLAYER_HH;
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YX))
			return GameConstants.GAME_PLAYER_HH;
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHD))
			return GameConstants.GAME_PLAYER_HH;
		if (is_mj_type(GameConstants.GAME_TYPE_THK_HY))
			return GameConstants.GAME_PLAYER_HH;
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_XT))
			return GameConstants.GAME_PLAYER_HH;
		if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
			return GameConstants.GAME_PLAYER_HH;
		if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX)) {
			return GameConstants.GAME_PLAYER_FPHZ;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CZ_SR)) {
			return GameConstants.GAME_PLAYER_HH;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_SYBP)) {
			if (has_rule(GameConstants.GAME_RULE_SYBP_TWO_PLAYER)) {
				return 2;
			} else {
				return 3;
			}
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD_SAPP)) {
			if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) && has_rule(GameConstants.GAME_RULE_LHQ_HD_SAN_REN)) {
				return Constants_ChenZhou.GAME_PLAYER_OF_3;
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1)
				return Constants_ChenZhou.GAME_PLAYER_OF_3;
			if (this.getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1)
				return 2;
			return GameConstants.GAME_PLAYER_FPHZ;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHEN_ZHOU)) {
			if (has_rule(Constants_ChenZhou.GAME_RULE_PLAYER_4)) {
				return Constants_ChenZhou.GAME_PLAYER_OF_4;
			} else if (has_rule(Constants_ChenZhou.GAME_RULE_PLAYER_3)) {
				return Constants_ChenZhou.GAME_PLAYER_OF_3;
			}
		}
		if (is_mj_type(GameConstants.GAME_TYPE_NEW_PHZ_CHEN_ZHOU)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (has_rule(Constants_New_ChenZhou.GAME_RULE_PLAYER_4)) {
				return Constants_New_ChenZhou.GAME_PLAYER_OF_4;
			} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_PLAYER_3)) {
				return Constants_New_ChenZhou.GAME_PLAYER_OF_3;
			} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_PLAYER_2)) {
				return Constants_New_ChenZhou.GAME_PLAYER_OF_2;
			}
			return Constants_New_ChenZhou.GAME_PLAYER_OF_3;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_LEI_YANG)) {
			return GameConstants.GAME_PLAYER_HH;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_WU_GANG)) {
			if (has_rule(Constants_WuGang.GAME_RULE_PLAYER_4)) {
				return Constants_WuGang.GAME_PLAYER_OF_4;
			}
			return Constants_WuGang.GAME_PLAYER_OF_3;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU)) {
			return GameConstants.GAME_PLAYER_HH;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN) && this.getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1)
			return 2;
		else if (is_mj_type(GameConstants.GAME_TYPE_468_HONG_GUAI_WAN)) {
			return GameConstants.GAME_PLAYER_HH;
		}
		return GameConstants.GAME_PLAYER;
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
			if (null == this.get_players()[i]) {
				continue;
			}
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			this.get_players()[i].setMoney(this.get_players()[i].getMoney() + score);
			if (score != 0) {
				AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(this.get_players()[i].getAccount_id(), score, false,
						buf.toString(), EMoneyOperateType.ROOM_COST);
				if (addGoldResultModel.isSuccess() == false) {
					// 扣费失败
					logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
				}
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

	@Override
	public boolean handler_enter_room(Player player) {
		if (super.handler_enter_room(player)) {
			check_if_kick_unready_player();
			return true;
		}
		return false;
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
