/**
 * 
 */
package com.cai.game.hbzp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.phz.Constants_YongZhou;
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
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.ChulifirstCardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.hbzp.HBPHZGameLogic.AnalyseItem;
import com.cai.game.hbzp.handler.PHZHandler;
import com.cai.game.hbzp.handler.PHZHandlerChiPeng;
import com.cai.game.hbzp.handler.PHZHandlerDispatchCard;
import com.cai.game.hbzp.handler.PHZHandlerFinish;
import com.cai.game.hbzp.handler.PHZHandlerGang;
import com.cai.game.hbzp.handler.PHZHandlerOutCardOperate;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerChiPeng_DY;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerChuLiFirstCard_DY;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerDispatchCard_DY;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerDispatchFirstCard_DY;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerFanJiang_DY;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerGang_DY;
import com.cai.game.hbzp.handler.dayezp.PHZHandlerOutCardOperate_DY;
import com.cai.redis.service.RedisService;
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

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class HBPHZTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;
	private static final int ID_TIMER_ANIMATION_START = 1;

	private static Logger logger = Logger.getLogger(HBPHZTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;
	public int _last_banker = GameConstants.INVALID_SEAT;// 上盘庄家
	public boolean last_liu_ju = false;

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
	public int _fan_jiang_card; // 翻将牌
	// 胡息
	// public HuCardInfo _hu_card_info;

	public HBPHZGameLogic _logic = null;
	public LouWeaveItem _lou_weave_item[][];
	public int _hu_weave_count[]; //
	public int _hu_xi[];
	public int _tun_shu[];
	public int _fan_shu[];
	public int _hu_code[];
	public int _da_pai_count[];
	public int _xiao_pai_count[];
	public int _huang_zhang_count;
	public int _dian_deng_count;
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
	public boolean _is_di_hu;
	public boolean _is_tian_hu;
	public int _provider_hu;
	public int _is_cannot_kai_zhao[];
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _hu_weave_items[][];

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public PHZHandler _handler;

	public PHZHandlerDispatchCard _handler_dispath_card;
	public PHZHandlerOutCardOperate _handler_out_card_operate;
	public PHZHandlerGang _handler_gang;
	public PHZHandlerChiPeng _handler_chi_peng;
	public PHZHandlerDispatchCard _handler_dispath_firstcards;
	public PHZHandlerDispatchCard _handler_chuli_firstcards;
	public PHZHandler _handler_fan_jiang;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public PHZHandlerFinish _handler_finish; // 结束

	public HBPHZTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new HBPHZGameLogic();

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
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
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
		_is_di_hu = false;
		_is_tian_hu = false;
		_fan_jiang_card = 0;
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_is_cannot_kai_zhao = new int[this.getTablePlayerNumber()];
		_provider_hu = GameConstants.INVALID_SEAT;
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
			_is_cannot_kai_zhao[i] = -1;

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

		// 胡牌信息
		_huang_zhang_count = 0;
		_dian_deng_count = 0;
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

	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		// 上次抓的牌
		_last_card = 0;
		_provider_hu = GameConstants.INVALID_SEAT;
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
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_huang_zhang_count = 0;
		_dian_deng_count = 0;
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
		_is_di_hu = false;
		_is_tian_hu = false;
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_is_cannot_kai_zhao = new int[this.getTablePlayerNumber()];
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
			_is_cannot_kai_zhao[i] = -1;
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

		if (is_mj_type(GameConstants.GAME_TYPE_HB_DYZP)) {
			_handler_dispath_card = new PHZHandlerDispatchCard_DY();
			_handler_out_card_operate = new PHZHandlerOutCardOperate_DY();
			_handler_gang = new PHZHandlerGang_DY();
			_handler_chi_peng = new PHZHandlerChiPeng_DY();

			_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_DY();
			_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_DY();
			_handler_fan_jiang = new PHZHandlerFanJiang_DY();
		}

		_handler_finish = new PHZHandlerFinish();
		this.setMinPlayerCount(this.getTablePlayerNumber());

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
		if (is_mj_type(GameConstants.GAME_TYPE_HB_DYZP)) {
			_handler_dispath_card = new PHZHandlerDispatchCard_DY();
			_handler_out_card_operate = new PHZHandlerOutCardOperate_DY();
			_handler_gang = new PHZHandlerGang_DY();
			_handler_chi_peng = new PHZHandlerChiPeng_DY();

			_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_DY();
			_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_DY();
		}
		_handler_finish = new PHZHandlerFinish();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
		} // 不能吃，碰

		has_first_qi_shou_ti = new boolean[this.getTablePlayerNumber()];
		_provider_hu = GameConstants.INVALID_SEAT;
		_cannot_chi = new int[this.getTablePlayerNumber()][30];
		_cannot_chi_count = new int[this.getTablePlayerNumber()];
		_cannot_peng = new int[this.getTablePlayerNumber()][30];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_is_cannot_kai_zhao = new int[this.getTablePlayerNumber()];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = false;
		_is_tian_hu = false;
		_fan_jiang_card = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			has_shoot[i] = false;
			is_hands_up[i] = false;
			player_ti_count[i][0] = 0;
			player_ti_count[i][1] = 0;
			is_wang_diao[i] = false;
			is_wang_diao_wang[i] = false;
			is_wang_chuang[i] = false;
			_is_cannot_kai_zhao[i] = -1;
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

		_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);


		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
		exe_fan_jiang();
		GameSchedule.put(new AnimationRunnable(getRoom_id(), ID_TIMER_ANIMATION_START), 600, TimeUnit.MILLISECONDS);

		return true;

	}

	private boolean exe_fan_jiang() {
		this._handler = this._handler_fan_jiang;
		this._handler.exe(this);
		return true;
	}

	/**
	 * 开始 攸县红黑胡
	 * 
	 * @return
	 */
	private boolean game_start_dy() {
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
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

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
		int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x05, 0x05, 0x05, 0x05, 0x03, 0x03, 0x03, 0x03, 0x06, 0x06, 0x06, 0x06, 0x07, 0x07, 0x07,
				0x07, };
		// {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		/*
		 * int cards1[] = new int[]
		 * {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		 * int cards2[] = new int[]
		 * {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		 * int cards3[] = new int[]
		 * {0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21,0x21};
		 */
		//////// // //int cards[] = new int[]{011,011,011};
		//////// // // int cards2[] = new int[] { 0x12, 0x12, 0x12, 0x12, 0x01,
		// 0x02,
		//////// // 0x04, 0x03, 0x05, 0x06, 0x14, 0x15, 0x16, 0x17,
		//////// // // 0x17, 0x15, 0x19, 0x17,0x17,0x13 };
		//////// // // int cards3[] = new int[] { 0x13, 0x13, 0x14, 0x14, 0x01,
		// 0x02,
		//////// // 0x04, 0x03, 0x05, 0x06, 0x15, 0x15, 0x16, 0x17,
		//////// // // 0x17, 0x15, 0x19, 0x18,0x18,0x16 };
		//////// //
		//////// //// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		//////// //// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		//////// //// GRR._cards_index[i][j] = 0;
		//////// //// }
		//////// //// }
		//////// //// int send_count = (GameConstants.MAX_HH_COUNT );
		//////// ////
		// _repertory_card[_all_card_len - GRR._left_card_count] = 0x07;
		// if(this.getTablePlayerNumber() == GameConstants.GAME_PLAYER_HH)
		// {
		// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		// GRR._cards_index[_current_player%this.getTablePlayerNumber()][j] =
		// 0;
		// GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][j]
		// = 0;
		// GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][j]
		// = 0;
		// }
		// }else
		// {
		/*
		 * for (int j = 0; j < 21; j++) { GRR._cards_index[0][j] = cards[j];
		 * GRR._cards_index[1][j] = cards[j]; GRR._cards_index[2][j] = cards[j];
		 * GRR._cards_index[3][j] = cards[j]; }
		 */
		// }
		// 清除数据

		/*
		 * for(int j = 0; j<cards1.length;j++) {
		 * GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][
		 * _logic.switch_to_card_index(cards1[j])] += 1; } for(int j = 0;
		 * j<cards2.length;j++) {
		 * GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][
		 * _logic.switch_to_card_index(cards2[j])] += 1; } for(int j = 0;
		 * j<cards3.length;j++) {
		 * GRR._cards_index[(_current_player+3)%this.getTablePlayerNumber()][
		 * _logic.switch_to_card_index(cards3[j])] += 1; }
		 */

		// /*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可

		//
		// int[] realyCards = new int[] {
		// 10, 6, 4, 7, 23, 5, 10, 2, 6, 19, 3, 25, 21, 5, 1, 2, 3, 22, 17, 8,
		// 26, 20, 7,
		// 19, 20, 1, 21, 2, 26, 23, 10, 4, 26, 24, 21, 17, 3, 9, 1, 4, 4, 18,
		// 9, 10, 17, 6,
		// 19, 25, 5, 6, 19, 20, 17, 22, 25, 3, 18, 25, 20, 8, 23, 9, 8, 9, 23,
		// 26, 18, 5, 1,
		// 22, 8, 7, 22, 18, 2, 24, 7, 21, 24, 24
		// };
		// this._cur_banker = 0;
		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
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
			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		// this._cur_banker = 2;
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
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {

		// 发牌
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	// 游戏结束
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HB_DYZP)) {
			ret = this.handler_game_finish_phz_dy(seat_index, reason);
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean handler_game_finish_phz_dy(int seat_index, int reason) {
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
			if (seat_index == this._provider_hu)
				game_end.setWinLziFen(1); // 1自摸,2平胡
			else if (this._provider_hu != GameConstants.INVALID_SEAT)
				game_end.setWinLziFen(2);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(this._fan_jiang_card);

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
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len - 1; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

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
			game_end.setCountPickNiao(GRR._count_pick_niao);// 胡牌类型

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (has_rule(GameConstants.GAME_RULE_DIAN_DENG_CHD)) {
				if (this.last_liu_ju != true && reason == GameConstants.Game_End_DRAW) {

					if (this._dian_deng_count > 0)
						this._dian_deng_count--;
					this._huang_zhang_count = 0;

				}
				if (reason == GameConstants.Game_End_DRAW) {
					this._dian_deng_count++;
					this._huang_zhang_count++;
					this.last_liu_ju = true;
					this._last_banker = GRR._banker_player;
				}

			} else if (is_mj_type(GameConstants.GAME_TYPE_PHZ_CHD)) {
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
				if (seat_index != i || reason != GameConstants.Game_End_NORMAL) {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						all_hu_xi += GRR._weave_items[i][j].hu_xi;
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
							all_hu_xi += _hu_weave_items[i][j].hu_xi;
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
			ChiHuRight chiHuRight, int card_type, int hu_xi[], boolean dispatch) {
		// if (this._ti_mul_long[seat_index] >=1) {
		// return 0;
		// }
		if (cur_card == this._fan_jiang_card)
			return 0;
		return analyse_chi_hu_card_dy(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type, hu_xi);

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
	public int analyse_chi_hu_card_dy(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
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
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				this._fan_jiang_card, this._is_tian_hu);

		if (cur_card != 0) {

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_HBZP_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem, this._fan_jiang_card);
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, this._fan_jiang_card, this._is_tian_hu);
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
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items, this._fan_jiang_card);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}
		if (max_hu_xi < 10) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_cards_count = 0;
		hu_xi_hh[0] = max_hu_xi;
		int max_zhao_count = 0;
		int piao_tai_zhao = 0;
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_HBZP_LONG || analyseItem.cbWeaveKind[j] == GameConstants.WIK_HBZP_ZHAO)
				max_zhao_count++;
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_HBZP_LONG || analyseItem.cbWeaveKind[j] == GameConstants.WIK_HBZP_ZHAO
					|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_HBZP_KAN)
				piao_tai_zhao++;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j], this._fan_jiang_card);
			_hu_weave_count[seat_index] = j + 1;

		}
		// 查询胡牌类型
		for (int i = 6; i >= 0; i--) {
			for (int j = 0; j < 3; j++) {
				if (analyseItem.cbCardData[i][j] == cur_card && cur_card != 0) {
					this.GRR._count_pick_niao = analyseItem.cbWeaveKind[i];
					break;
				}
			}
		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_HBZP_DUI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
			if (analyseItem.cbCardEye == cur_card) {
				this.GRR._count_pick_niao = GameConstants.WIK_HBZP_DUI;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index)) {
			chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO);
		}
		// else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index
		// != provider_index)) {
		// chiHuRight.opr_or_long(GameConstants.CHR_JEI_PAO_HU);
		// }
		if (max_hu_xi >= 30) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_MAN_30);
		}
		if (max_zhao_count >= 3) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_SAN_ZHAO);
		}
		if (piao_tai_zhao == 0 && max_hu_xi >= 20) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_PIAO_TAI);
		} else if (piao_tai_zhao == 0) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_PIAO_HU);
		}
		if (piao_tai_zhao != 0 && hong_pai_count == 10) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_SHI_HONG);
		}
		if (piao_tai_zhao != 0 && hong_pai_count == 10 && max_hu_xi >= 20) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_SHI_HONG_TAI);
		} else if (piao_tai_zhao != 0 && max_hu_xi >= 20) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_TAI_HU);
		}
		if (piao_tai_zhao == 0 && hong_pai_count == 10) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_SHI_HONG_PIAO);
		}
		if (piao_tai_zhao == 0 && hong_pai_count > 10) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_CHUANG_PIAO);
		}
		if (piao_tai_zhao != 0 && hong_pai_count > 10 && max_hu_xi >= 20) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_CHUANG_TAI);
		} else if (piao_tai_zhao != 0 && hong_pai_count > 10) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_CHUANG_SHUANG);
		}
		if (hong_pai_count == 1) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_YI_ZHI_YAN);
		}
		if (_logic.is_kuai_bian(analyseItem)) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_YI_KUAI_BIAN);
		}
		if (_logic.is_jie_jie_liang(analyseItem) == 7) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_JIE_JIE_LIANG);
		}
		if (_logic.is_quan_hun(analyseItem)) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_QUAN_HUN);
		}
		if (hong_pai_count == 0) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_QUAN_HEI);
		}
		if (_is_tian_hu == false && seat_index == provider_index && seat_index == this._cur_banker) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_TIAN_HU);
		}
		if (_is_di_hu == false && seat_index != provider_index && provider_index == this._cur_banker) {
			chiHuRight.opr_or_long(GameConstants.CHR_HBZP_DI_HU);
		}
		// if (hong_pai_count >= 10 && hong_pai_count < 13) {
		// chiHuRight.opr_or_long(GameConstants.CHR_TEN_HONG_PAI_CHD);
		// }
		// if (hong_pai_count >= 13) {
		// chiHuRight.opr_or_long(GameConstants.CHR_THIRTEEN_HONG_PAI_CHD);
		// }
		// if (hong_pai_count == 1) {
		// chiHuRight.opr_or_long(GameConstants.CHR_ONE_HONG_CHD);
		// }
		// if (hei_pai_count == all_cards_count) {
		// chiHuRight.opr_or_long(GameConstants.CHR_ALL_HEI_CHD);
		// }

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
		// 跑牌判断
		if ((bAroseAction == GameConstants.WIK_NULL)) {
			if (_logic.check_pao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
				// this.exe_gang(seat_index, seat_index, card_data,
				// GameConstants.WIK_PAO,GameConstants.PAO_TYPE_OHTER_PAO ,
				// true, false);
				pao_type[0] = GameConstants.PAO_TYPE_OHTER_PAO;
				bAroseAction = GameConstants.WIK_HBZP_ZHAO;
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

				if ((action & GameConstants.WIK_HBZP_LEFT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index + 2]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_LEFT, seat_index,
								_lou_weave_item[chi_seat_index][0].nCount, _lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_CENTER) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index - 1]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_RIGHT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index - 1]--;
						temp_cards_index[cur_card_index - 2]--;
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_RIGHT, seat_index,
								_lou_weave_item[chi_seat_index][2].nCount, _lou_weave_item[chi_seat_index][2].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_XXD) != 0) {
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
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_XXD, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 4, card_count - 3);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_XXD, seat_index,
								_lou_weave_item[chi_seat_index][4].nCount, _lou_weave_item[chi_seat_index][4].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_DDX) != 0) {
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
							bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_DDX, type_count, type_eat_count[0],
										_lou_weave_item[chi_seat_index], 5, card_count - 3);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_DDX, seat_index,
								_lou_weave_item[chi_seat_index][5].nCount, _lou_weave_item[chi_seat_index][5].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_EQS) != 0) {
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
						bAction = _logic.check_lou_weave(temp_cards_index, card, GameConstants.WIK_HBZP_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_EQS, seat_index,
								_lou_weave_item[chi_seat_index][3].nCount, _lou_weave_item[chi_seat_index][3].nLouWeaveKind);
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

				if ((action & GameConstants.WIK_HBZP_LEFT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index + 2]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_LEFT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 0, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_LEFT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_LEFT, seat_index,
								_lou_weave_item[chi_seat_index][0].nCount, _lou_weave_item[chi_seat_index][0].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_CENTER) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index + 1]--;
						temp_cards_index[cur_card_index - 1]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_CENTER, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 1, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_CENTER);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_CENTER, seat_index,
								_lou_weave_item[chi_seat_index][1].nCount, _lou_weave_item[chi_seat_index][1].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_RIGHT) != 0) {
					boolean bAction = false;
					if (GRR._cards_index[chi_seat_index][cur_card_index] > 0) {
						int temp_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
						for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
							temp_cards_index[i] = GRR._cards_index[chi_seat_index][i];
						}
						temp_cards_index[cur_card_index]--;
						temp_cards_index[cur_card_index - 1]--;
						temp_cards_index[cur_card_index - 2]--;
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_RIGHT, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 2, card_count - 3, yws_type);

					} else
						bAction = true;
					if (card_count == ((this.GRR._cards_index[chi_seat_index][cur_card_index] + 1) * 3 - 1))
						bAction = false;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_RIGHT);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_RIGHT, seat_index,
								_lou_weave_item[chi_seat_index][2].nCount, _lou_weave_item[chi_seat_index][2].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_XXD) != 0) {
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
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_XXD, type_count,
										type_eat_count[0], _lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
							else
								bAction = true;
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_XXD, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 4, card_count - 3, yws_type);
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_XXD);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_XXD, seat_index,
								_lou_weave_item[chi_seat_index][4].nCount, _lou_weave_item[chi_seat_index][4].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_DDX) != 0) {
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
							bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_DDX, type_count, type_eat_count[0],
									_lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
						} else {
							temp_cards_index[cur_card_index - 10]--;
							temp_cards_index[cur_card_index]--;
							temp_cards_index[cur_card_index]--;
							if (temp_cards_index[cur_card_index] == 0)
								bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_DDX, type_count,
										type_eat_count[0], _lou_weave_item[chi_seat_index], 5, card_count - 3, yws_type);
							else
								bAction = true;
						}

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_DDX);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_DDX, seat_index,
								_lou_weave_item[chi_seat_index][5].nCount, _lou_weave_item[chi_seat_index][5].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}
				if ((action & GameConstants.WIK_HBZP_EQS) != 0) {
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
						bAction = _logic.check_lou_weave_phz(temp_cards_index, card, GameConstants.WIK_HBZP_EQS, type_count, type_eat_count[0],
								_lou_weave_item[chi_seat_index], 3, card_count - 3, yws_type);

					} else
						bAction = true;
					if (bAction == true) {
						_playerStatus[chi_seat_index].add_action(GameConstants.WIK_HBZP_EQS);
						_playerStatus[chi_seat_index].add_chi_hh(card, GameConstants.WIK_HBZP_EQS, seat_index,
								_lou_weave_item[chi_seat_index][3].nCount, _lou_weave_item[chi_seat_index][3].nLouWeaveKind);
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
					playerStatus.add_hh_peng(card, GameConstants.WIK_HBZP_PENG, seat_index);
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
			case GameConstants.WIK_HBZP_LEFT: {
				int cbRemoveCard[] = new int[] { target_card, target_card + 1, target_card + 2 };
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_HBZP_LEFT;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
				break;
			}
			case GameConstants.WIK_HBZP_CENTER: {
				int cbRemoveCard[] = new int[] { target_card, target_card - 1, target_card + 1 };
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_HBZP_CENTER;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
				break;
			}
			case GameConstants.WIK_HBZP_RIGHT: {
				int cbRemoveCard[] = new int[] { target_card, target_card - 1, target_card - 2 };
				if (!this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 3)) {
					this.log_player_error(target_player, "吃牌删除出错");
					return false;
				}
				int wIndex = this.GRR._weave_count[target_player]++;
				this.GRR._weave_items[target_player][wIndex].public_card = 1;
				this.GRR._weave_items[target_player][wIndex].center_card = target_card;
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_HBZP_RIGHT;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
				break;
			}
			case GameConstants.WIK_HBZP_DDX: {
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
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_HBZP_DDX;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
				break;
			}
			case GameConstants.WIK_HBZP_XXD: {
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
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_HBZP_XXD;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
				break;
			}
			case GameConstants.WIK_HBZP_EQS: {
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
				this.GRR._weave_items[target_player][wIndex].weave_kind = GameConstants.WIK_HBZP_EQS;
				this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
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
				this.GRR._weave_items[target_player][wIndex].hu_xi = _logic.get_weave_hu_xi(this.GRR._weave_items[target_player][wIndex],
						this._fan_jiang_card);
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
		// WalkerGeek 替换获取牌桌人数方式 没问题的话下个版本删除注释
		/*
		 * if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) ||
		 * this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)) count = 4; else
		 * count = 3;
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
			if (this._fan_jiang_card != 0) {
				operate_fan_jiang(-1, GameConstants.Show_Card_Center, 1, new int[] { this._fan_jiang_card }, seat_index);

			}
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
		if (this._cur_round > 0)
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

		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			switch (this._hu_weave_items[seat_index][i].weave_kind) {
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_HBZP_PENG:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_HBZP_DUI:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_HBZP_LEFT:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card + 2 == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_HBZP_CENTER:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_HBZP_RIGHT:
				if (this._hu_weave_items[seat_index][i].center_card == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (this._hu_weave_items[seat_index][i].center_card - 2 == operate_card) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_HBZP_EQS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(this._hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 2) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 7) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_YWS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(this._hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 1) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 5) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_HBZP_DDX:
			case GameConstants.WIK_HBZP_XXD:
				if (_logic.get_card_value(operate_card) == _logic.get_card_value(this._hu_weave_items[seat_index][i].center_card)) {
					this.GRR._count_pick_niao = this._hu_weave_items[seat_index][i].weave_kind;
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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i], this.GRR._weave_count[i],
					GameConstants.INVALID_SEAT);

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
	public void countChiHuTimes(int _seat_index, int _provide_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}

		_player_result.hu_pai_count[_seat_index]++;
		_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];

		if (_seat_index == _provide_index)
			_player_result.zi_mo_count[_seat_index]++;
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_MAN_30)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_SAN_ZHAO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_PIAO_TAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_SHI_HONG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_SHI_HONG_TAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_SHI_HONG_PIAO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_CHUANG_PIAO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_CHUANG_TAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_YI_ZHI_YAN)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_YI_KUAI_BIAN)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_JIE_JIE_LIANG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_QUAN_HUN)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_QUAN_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_PIAO_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_TAI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HBZP_CHUANG_SHUANG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		/*
		 * if (!(chiHuRight.opr_and(GameConstants.CHR_ZI_MO)).is_empty()) {
		 * _player_result.ming_tang_count[_seat_index]++; }
		 */
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_dy(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		_provider_hu = provide_index;
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
		int cell_score = 1;
		if (has_rule(GameConstants.GAME_RULE_HBDY_ONE_FEN))
			cell_score = 1;
		else if (has_rule(GameConstants.GAME_RULE_HBDY_TWO_FEN))
			cell_score = 2;
		else if (has_rule(GameConstants.GAME_RULE_HBDY_THREE_FEN))
			cell_score = 3;

		int wFanShu = _logic.get_chi_hu_action_rank_dy(chr);// 番数

		float lChiHuScore = wFanShu * cell_score;
		if (seat_index == provide_index) {
			lChiHuScore += cell_score;
		}
		////////////////////////////////////////////////////// 自摸 算分

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}

			// 胡牌分
			GRR._game_score[i] -= lChiHuScore;
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
	public void process_chi_hu_player_score_lba(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
		_provider_hu = provide_index;
		// 计算胡息
		int all_hu_xi = 0;
		// for(int i =0; i< this.GRR._weave_count[seat_index] ; i++){
		// all_hu_xi += this.GRR._weave_items[seat_index][i].hu_xi;
		// }
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}
		this._hu_xi[seat_index] = all_hu_xi;
		int cell_score = 1;
		if (has_rule(GameConstants.GAME_RULE_ZERO_DI_CHD))
			cell_score = 1;
		else if (has_rule(GameConstants.GAME_RULE_ONE_DI_CHD))
			cell_score = 2;
		else if (has_rule(GameConstants.GAME_RULE_TWO_DI_CHD))
			cell_score = 3;
		else if (has_rule(GameConstants.GAME_RULE_THREE_DI_CHD))
			cell_score = 4;
		else if (has_rule(GameConstants.GAME_RULE_FOUR_DI_CHD))
			cell_score = 5;
		else if (this.getRuleValue(GameConstants.GAME_RULE_WU_DI_CHD) == 1)
			cell_score = 6;
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = cell_score + (all_hu_xi - 15) / 3;
		if (seat_index == provide_index) {
			calculate_score += 1;
		}

		int wFanShu = 1;
		wFanShu = _logic.get_chi_hu_action_rank_phz_lba(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
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
		int times = 1;
		// if
		// (!(chr.opr_and(GameConstants.CHR_HUANG_FAN_CHD)).is_empty()&&_huang_zhang_count>0)
		// {
		// times = 1 + _huang_zhang_count;
		// }
		// else if
		// (!(chr.opr_and(GameConstants.CHR_HUANG_FAN_CHD)).is_empty()&&_dian_deng_count>0)
		// {
		// times = 2;
		// }
		float lChiHuScore = wFanShu * calculate_score * times;
		if (has_rule(GameConstants.GAME_RULE_500_MAX_CHD)) {
			if (lChiHuScore > 500)
				lChiHuScore = 500;
		} else if (has_rule(GameConstants.GAME_RULE_200_MAX_CHD)) {
			if (lChiHuScore > 200)
				lChiHuScore = 200;
		} else if (has_rule(GameConstants.GAME_RULE_100_MAX_CHD)) {
			if (lChiHuScore > 100)
				lChiHuScore = 100;
		} else if (has_rule(GameConstants.GAME_RULE_20_MAX_CHD)) {
			if (lChiHuScore > 20)
				lChiHuScore = 20;
		} else if (has_rule(GameConstants.GAME_RULE_10_MAX_CHD)) {
			if (lChiHuScore > 10)
				lChiHuScore = 10;
		}
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
		_provider_hu = provide_index;
		// 计算胡息
		int all_hu_xi = 0;
		// for(int i =0; i< this.GRR._weave_count[seat_index] ; i++){
		// all_hu_xi += this.GRR._weave_items[seat_index][i].hu_xi;
		// }
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}
		this._hu_xi[seat_index] = all_hu_xi;
		int cell_score = 1;
		if (has_rule(GameConstants.GAME_RULE_ZERO_DI_CHD))
			cell_score = 1;
		else if (has_rule(GameConstants.GAME_RULE_ONE_DI_CHD))
			cell_score = 2;
		else if (has_rule(GameConstants.GAME_RULE_TWO_DI_CHD))
			cell_score = 3;
		else if (has_rule(GameConstants.GAME_RULE_THREE_DI_CHD))
			cell_score = 4;
		else if (has_rule(GameConstants.GAME_RULE_FOUR_DI_CHD))
			cell_score = 5;
		else if (this.getRuleValue(GameConstants.GAME_RULE_WU_DI_CHD) == 1)
			cell_score = 6;
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = cell_score + (all_hu_xi - 15) / 3;
		if (seat_index == provide_index) {
			calculate_score += 1;
		}

		if (has_rule(GameConstants.GAME_RULE_DIAN_DENG_CHD)) {
			if (this.last_liu_ju != true && GRR._banker_player != this._last_banker) {

				if (this._dian_deng_count > 0)
					this._dian_deng_count--;

			}
			if (this.last_liu_ju != true && GRR._banker_player != this._last_banker && this._huang_zhang_count > 0) {
				this._huang_zhang_count = 0;

			}
			this._last_banker = GRR._banker_player;

			this.last_liu_ju = false;
		}
		int wFanShu = 1;
		wFanShu = _logic.get_chi_hu_action_rank_phz_chd(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
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
		int times = 1;
		if (!(chr.opr_and(GameConstants.CHR_HUANG_FAN_CHD)).is_empty() && _huang_zhang_count > 0) {
			times = 1 + _huang_zhang_count;
		} else if (!(chr.opr_and(GameConstants.CHR_HUANG_FAN_CHD)).is_empty() && _dian_deng_count > 0) {
			times = 2;
		}
		float lChiHuScore = wFanShu * calculate_score * times;
		if (has_rule(GameConstants.GAME_RULE_500_MAX_CHD)) {
			if (lChiHuScore > 500)
				lChiHuScore = 500;
		} else if (has_rule(GameConstants.GAME_RULE_200_MAX_CHD)) {
			if (lChiHuScore > 200)
				lChiHuScore = 200;
		} else if (has_rule(GameConstants.GAME_RULE_100_MAX_CHD)) {
			if (lChiHuScore > 100)
				lChiHuScore = 100;
		} else if (has_rule(GameConstants.GAME_RULE_20_MAX_CHD)) {
			if (lChiHuScore > 20)
				lChiHuScore = 20;
		} else if (has_rule(GameConstants.GAME_RULE_10_MAX_CHD)) {
			if (lChiHuScore > 10)
				lChiHuScore = 10;
		}
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
	public void process_chi_hu_player_score_dhd_chd(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		_provider_hu = provide_index;
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
		int cell_score = 1;
		if (has_rule(GameConstants.GAME_RULE_ZERO_DI_CHD))
			cell_score = 1;
		else if (has_rule(GameConstants.GAME_RULE_ONE_DI_CHD))
			cell_score = 2;
		else if (has_rule(GameConstants.GAME_RULE_TWO_DI_CHD))
			cell_score = 3;
		else if (has_rule(GameConstants.GAME_RULE_THREE_DI_CHD))
			cell_score = 4;
		else if (has_rule(GameConstants.GAME_RULE_FOUR_DI_CHD))
			cell_score = 5;
		else if (this.getRuleValue(GameConstants.GAME_RULE_WU_DI_CHD) == 1)
			cell_score = 6;
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = cell_score + (all_hu_xi - 15) / 3;

		int wFanShu = 1;
		if (has_rule(GameConstants.GAME_RULE_WAN_FA_CHD)) {
			if (seat_index == provide_index)
				calculate_score += 1;
			wFanShu = _logic.get_chi_hu_action_rank_dhd_chd(seat_index, this._hong_pai_count[seat_index], chr);// 番数
		}

		else if (has_rule(GameConstants.GAME_RULE_WAN_FA_TWO_CHD))
			wFanShu = _logic.get_chi_hu_action_rank_dhd_two_chd(seat_index, this._hong_pai_count[seat_index], chr);// 番数

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
		if (has_rule(GameConstants.GAME_RULE_20_MAX_CHD)) {
			if (lChiHuScore > 20)
				lChiHuScore = 20;
		} else if (has_rule(GameConstants.GAME_RULE_10_MAX_CHD)) {
			if (lChiHuScore > 10)
				lChiHuScore = 10;
		}
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
		roomResponse.setFlashTime(100);
		roomResponse.setStandTime(600);

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

	// 显示在玩家前面的牌
	public boolean operate_fan_jiang(int seat_index, int type, int count, int cards[], int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_FAN_JIANG);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		roomResponse.setFlashTime(300);
		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			roomResponse.setFlashTime(0);
			return this.send_response_to_player(to_player, roomResponse);
		}
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
		for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len - 1; i++) {
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
		long timer = System.currentTimeMillis();
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
		timer = System.currentTimeMillis() - timer;
		if (timer > 1000) {
			this.log_info("game_type_index = " + _game_type_index + "time = " + timer);
			String str = "out time ";
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				for (int j = 0; j < cbCardIndexTemp[i]; j++) {
					str += _logic.switch_to_card_data(i) + ",";
				}
			}
			this.log_info(str);
		}
		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION_RECORD);
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
			int eat_type = GameConstants.WIK_HBZP_LEFT | GameConstants.WIK_HBZP_CENTER | GameConstants.WIK_HBZP_RIGHT | GameConstants.WIK_HBZP_DDX
					| GameConstants.WIK_HBZP_XXD | GameConstants.WIK_HBZP_EQS | GameConstants.WIK_YWS;
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);
		roomResponse.setFlashTime(100);
		roomResponse.setStandTime(600);
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
					if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) && has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
							&& this._xt_display_an_long[seat_index] == true)
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
			_cur_banker = 0;// 创建者的玩家为专家

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	public void log_info(String info) {

		// logger.info("房间[" + this.getRoom_id() + "]" + info);

	}

	public void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtianhu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU_CHD)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhyidianhong, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhonghu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhongfantian, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhallhei, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU_CHD)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhaihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtinghu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_DA_HU_CHD).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhdahu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_XIAO_HU_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhxiaohu, "", 0, 0l, this.getRoom_id());
			}
			if (!(chiHuRight.opr_and(GameConstants.CHR_DUI_ZI_HU_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhduizihu, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_SHUA_HOU_CHD)).is_empty()) {
				wFanShu = 3;
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhshuahou, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_TUAN_YUAN_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhtuanyuan, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_HUANG_FAN_CHD).is_empty())) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhuangfan, "", 0, 0l, this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(GameConstants.CHR_HANG_HANG_XI_CHD)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.hhhanghangxi, "", 0, 0l, this.getRoom_id());
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_dy() {
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
						des += ",自摸  +1底";
					}

					if (type == GameConstants.CHR_HBZP_MAN_30) {
						des += ",满30胡息";
					}

					if (type == GameConstants.CHR_HBZP_SAN_ZHAO) {
						des += ",三朝";
					}
					if (type == GameConstants.CHR_HBZP_PIAO_TAI) {
						des += ",飘台";
					}
					if (type == GameConstants.CHR_HBZP_SHI_HONG) {
						des += ",十红";
					}
					if (type == GameConstants.CHR_HBZP_SHI_HONG_TAI) {
						des += ",十红台";
					}

					if (type == GameConstants.CHR_HBZP_SHI_HONG_PIAO) {
						des += ",十红飘";
					}

					if (type == GameConstants.CHR_HBZP_CHUANG_PIAO) {
						des += ",闯飘";
					}
					if (type == GameConstants.CHR_HBZP_CHUANG_TAI) {
						des += ",闯台";
					}
					if (type == GameConstants.CHR_HBZP_YI_ZHI_YAN) {
						des += ",一只眼";
					}

					if (type == GameConstants.CHR_HBZP_YI_KUAI_BIAN) {
						des += ",一块扁";
					}

					if (type == GameConstants.CHR_HBZP_JIE_JIE_LIANG) {
						des += ",节节亮";
					}
					if (type == GameConstants.CHR_HBZP_QUAN_HUN) {
						des += ",全荤";
					}
					if (type == GameConstants.CHR_HBZP_QUAN_HEI) {
						des += ",全黑";
					}

					if (type == GameConstants.CHR_HBZP_TIAN_HU) {
						des += ",天胡";
					}

					if (type == GameConstants.CHR_HBZP_DI_HU) {
						des += ",地胡";
					}
					if (type == GameConstants.CHR_HBZP_PIAO_HU) {
						des += ",飘胡";
					}
					if (type == GameConstants.CHR_HBZP_TAI_HU) {
						des += ",台胡";
					}
					if (type == GameConstants.CHR_HBZP_CHUANG_SHUANG) {
						des += ",闯双";
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
					if (type == GameConstants.CHR_TIAN_HU_CHD) {
						des += ",天胡×6";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸  +1囤";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU_CHD && l == 1) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU_CHD) {
						des += ",地胡×6";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI_CHD) {
						des += ",红胡×" + (3 + (_hong_pai_count[seat_index] - 10));
					}

					if (type == GameConstants.CHR_ONE_HONG_CHD) {
						des += ",点胡×6";
					}
					if (type == GameConstants.CHR_ALL_HEI_CHD) {
						des += ",乌胡×8";
					}
					if (type == GameConstants.CHR_TING_HU_CHD) {
						des += ",听胡×6";
					}
					if (type == GameConstants.CHR_HAI_HU_CHD) {
						des += ",海胡×6";
					}
					if (type == GameConstants.CHR_DA_HU_CHD) {
						des += ",大胡×" + (8 + this._da_pai_count[seat_index] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU_CHD) {
						des += ",小胡×" + (10 + this._xiao_pai_count[seat_index] - 16);
					}
					if (type == GameConstants.CHR_DUI_ZI_HU_CHD) {
						des += ",对子胡×8";
					}
					if (type == GameConstants.CHR_SHUA_HOU_CHD) {
						des += ",耍猴×8";
					}
					if (type == GameConstants.CHR_TUAN_CHD) {
						des += ",团×" + (8 * this._tuan_yuan_count[seat_index]);
					}
					if (type == GameConstants.CHR_HANG_HANG_XI_CHD) {
						des += ",行行息×8";
					}
					if (type == GameConstants.CHR_HANG_HANG_XI_lIU_CHD) {
						des += ",假行行息×4";
					}
					if (type == GameConstants.CHR_MAN_YUAN_HUA_CHD) {
						des += ",满园花×10";
					}
					if (type == GameConstants.CHR_TUAN_YUAN_CHD) {
						des += ",团圆×10";
					}
					if (type == GameConstants.CHR_MTH_XIAO_CHD) {
						des += ",小满堂红×6";
					}
					if (type == GameConstants.CHR_MTH_DA_CHD) {
						des += ",大满堂红×6";
					}
					if (type == GameConstants.CHR_HONG_FAN_TIAN_CHD) {
						des += ",红翻天×10";
					}

					// if (type == GameConstants.CHR_DIAN_DENG_CHD) {
					// des += ",点灯×2";
					// }
					if (type == GameConstants.CHR_GAI_CHD) {
						des += ",盖×4";
					}
					if (type == GameConstants.CHR_BEI_CHD) {
						des += ",背×8";
					}
					if (type == GameConstants.CHR_SI_QI_CHD) {
						des += ",四七红×3";
					}
					if (type == GameConstants.CHR_HUANG_FAN_CHD) {
						if (this._huang_zhang_count > 0)
							des += ",黄番×" + (1 + this._huang_zhang_count);
						else if (this._dian_deng_count > 0)
							des += ",黄番×2";

					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_lba(int seat_index) {
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
					if (type == GameConstants.CHR_TIAN_HU_CHD) {
						des += ",天胡×8";
					}

					if (type == GameConstants.CHR_ZI_MO) {
						des += ",自摸  +1囤";
					}
					if (type == GameConstants.CHR_JEI_PAO_HU_CHD && l == 1) {
						des += ",平胡";
					}
					if (type == GameConstants.CHR_DI_HU_CHD) {
						des += ",地胡×6";
					}

					if (type == GameConstants.CHR_TEN_HONG_PAI_CHD) {
						des += ",红胡×" + (3 + (_hong_pai_count[seat_index] - 10));
					}

					if (type == GameConstants.CHR_ONE_HONG_CHD) {
						des += ",点胡×5";
					}
					if (type == GameConstants.CHR_ALL_HEI_CHD) {
						des += ",黑胡×6";
					}
					if (type == GameConstants.CHR_HAI_HU_CHD) {
						des += ",海胡×6";
					}
					if (type == GameConstants.CHR_DA_HU_CHD) {
						des += ",大胡×" + (6 + this._da_pai_count[seat_index] - 18);
					}
					if (type == GameConstants.CHR_XIAO_HU_CHD) {
						des += ",小胡×" + (8 + this._xiao_pai_count[seat_index] - 16);
					}
					if (type == GameConstants.CHR_DUI_ZI_HU_CHD) {
						des += ",对子胡×6";
					}

					if (type == GameConstants.CHR_HANG_HANG_XI_CHD) {
						des += ",行行息×8";
					}
					if (type == GameConstants.CHR_HANG_HANG_XI_lIU_CHD) {
						des += ",假行行息×4";
					}

					if (type == GameConstants.CHR_TUAN_CHD) {
						des += ",团×" + (8 * this._tuan_yuan_count[seat_index]);
					}
					if (type == GameConstants.CHR_GAI_CHD) {
						des += ",盖×4";
					}
					if (type == GameConstants.CHR_BEI_CHD) {
						des += ",背×8";
					}
					if (type == GameConstants.CHR_DAN_PIAO_CHD) {
						des += ",漂×3";
					}
					if (type == GameConstants.CHR_SHUANG_PIAO_CHD) {
						des += ",漂×2";
					}
					if (type == GameConstants.CHR_SHUN_CHD) {
						des += ",顺×8";
					}
					if (type == GameConstants.CHR_SI_QI_CHD) {
						des += ",四七红×3";
					}
					if (type == GameConstants.CHR_YING_CHD) {
						des += ",印×2";
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_dhd_chd(int seat_index) {
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

					if (type == GameConstants.CHR_JEI_PAO_HU && l == 1) {
						des += ",平胡";
					}
					if (has_rule(GameConstants.GAME_RULE_WAN_FA_CHD)) {
						if (type == GameConstants.CHR_ZI_MO) {
							des += ",自摸  +1囤";
						}
						if (type == GameConstants.CHR_TEN_HONG_PAI) {
							des += ",红胡×" + (3 + (_hong_pai_count[seat_index] - 10));
						}

						if (type == GameConstants.CHR_ONE_HONG) {
							des += ",点胡×4";
						}
						if (type == GameConstants.CHR_ALL_HEI) {
							des += ",乌胡×6";
						}
						if (type == GameConstants.CHR_DUI_ZI_HU_CHD) {
							des += ",对子胡×4";
						}
					}
					if (has_rule(GameConstants.GAME_RULE_WAN_FA_TWO_CHD)) {
						if (type == GameConstants.CHR_TEN_HONG_PAI) {
							des += ",红胡×" + (2 + (_hong_pai_count[seat_index] - 10));
						}

						if (type == GameConstants.CHR_ONE_HONG) {
							des += ",点胡×3";
						}
						if (type == GameConstants.CHR_ALL_HEI) {
							des += ",乌胡×5";
						}
						if (type == GameConstants.CHR_DUI_ZI_HU_CHD) {
							des += ",对子胡×4";
						}
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	public void set_result_describe(int seat_index) {

		set_result_describe_dy();
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
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();

		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}
		if ((is_mj_type(GameConstants.GAME_TYPE_LHQ_HD) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HY)
				|| is_mj_type(GameConstants.GAME_TYPE_LHQ_QD))) {
			if (flag == true)
				delay = 100;
			else if (action != GameConstants.WIK_PAO)
				delay = 200;
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

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,
				TimeUnit.MILLISECONDS);

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
			chiHuRight.opr_or_long(GameConstants.CHR_ALL_HEI_TIAN_HU);
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
			chiHuRight.opr_or_long(GameConstants.CHR_LDH_TIAN_HU);
		}

		if (dui_zi == 9) {
			chiHuRight.opr_or_long(GameConstants.CHR_JIU_DUI_TIAN_HU);
		}

		if (si_bian_dui == 4) {
			chiHuRight.opr_or_long(GameConstants.CHR_SBD_TIAN_HU);
		}
		if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX) == false) {
			chiHuRight.opr_or_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
		}
		if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX)) {
			chiHuRight.opr_or_long(GameConstants.CHR_ZHA_DAN_WMQ);
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

		if (this.has_rule(GameConstants.GAME_RULE_HBDY_TWO_PLAYER)) {
			return 2;
		}
		return GameConstants.GAME_PLAYER_HH;
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

	public boolean animation_timer(int timer_id) {
		switch (timer_id) {
		case ID_TIMER_ANIMATION_START:
			return game_start_dy();
		}
		return true;
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
