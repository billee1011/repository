/**
 * 
 */
package com.cai.game.xpbh;

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
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.DispatchFirstCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.xpbh.XPBHGameLogic.AnalyseItem;
import com.cai.game.xpbh.handler.BHHandler;
import com.cai.game.xpbh.handler.BHHandlerChiPeng;
import com.cai.game.xpbh.handler.BHHandlerDispatchCard;
import com.cai.game.xpbh.handler.BHHandlerFinish;
import com.cai.game.xpbh.handler.BHHandlerGang;
import com.cai.game.xpbh.handler.BHHandlerOutCardOperate;
import com.cai.game.xpbh.handler.bh.BHHandlerChiPeng_XP;
import com.cai.game.xpbh.handler.bh.BHHandlerChuLiFirstCard_XP;
import com.cai.game.xpbh.handler.bh.BHHandlerDispatchCard_XP;
import com.cai.game.xpbh.handler.bh.BHHandlerDispatchFirstCard_XP;
import com.cai.game.xpbh.handler.bh.BHHandlerGang_XP;
import com.cai.game.xpbh.handler.bh.BHHandlerOutCardOperate_XP;
import com.cai.game.xpbh.handler.bh.BHHandlerPiao_XP;
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
import protobuf.clazz.xpbh.xpbhRsp.Opreate_Xpbh_Request;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

///////////////////////////////////////////////////////////////////////////////////////////////
public class XPBHTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(XPBHTable.class);

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

	public int _qiang_XPBH_MAX_COUNT; // 最大呛分
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
	public int _cannot_peng[][];// 不可以碰的牌
	public int _guo_hu_xt[]; // 同一圈用户过胡
	public int _guo_hu_pai_cards[][]; // 过胡牌
	public int _guo_hu_pai_count[]; // 过胡牌数量
	public int _guo_hu_xi[][]; // 过胡胡息
	public int _guo_hu_hu_xi[][]; // 过胡时的胡息
	public int _can_hu_pai_card; // 要胡的牌
	public int _is_dispatch; // 是否是发的牌
	public int _is_zha_hu_card; // 是否胡牌
	public int _is_chi_type[][]; // 吃牌type
	public int _game_score[];  //用户分数
	/**
	 * 组合扑克--组合数目
	 */
	public int _weave_count[]; //
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _yao_weave_item[];
	// 胡息
	// public HuCardInfo _hu_card_info;

	public XPBHGameLogic _logic = null;
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
	public boolean _xian_ming_zhao_not[];
	public boolean _is_di_hu;
	public int _provider_hu;
	
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _hu_weave_items[][];

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public BHHandler _handler;

	public BHHandlerDispatchCard _handler_dispath_card;
	public BHHandlerOutCardOperate _handler_out_card_operate;
	public BHHandlerGang _handler_gang;
	public BHHandlerChiPeng _handler_chi_peng;
	public BHHandlerDispatchCard _handler_dispath_firstcards;
	public BHHandlerDispatchCard _handler_chuli_firstcards;
	public BHHandler _handler_piao_fen ;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public BHHandlerFinish _handler_finish; // 结束

	public XPBHTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new XPBHGameLogic();

		// 设置等待状态

		// 玩家
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;

		has_shoot = new boolean[this.getTablePlayerNumber()];
		cards_has_wei = new int[GameConstants.XPBH_MAX_INDEX];
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

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
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
		_cannot_chi = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_cannot_peng = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = false;
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao_not = new boolean[this.getTablePlayerNumber()];
		_provider_hu = GameConstants.INVALID_SEAT;
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
			_xian_ming_zhao_not[i] = false;

		}
		// 组合扑克
		_weave_count = new int[this.getTablePlayerNumber()];
		_yao_weave_item = new WeaveItem[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_yao_weave_item[i] = new WeaveItem();
		
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
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
		for (

				int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.XPBH_MAX_WEAVE; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}
		_status_cs_gang = false;

		_gang_card_data = new CardsData(GameConstants.XPBH_MAX_COUNT);
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
		_cannot_chi = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_cannot_peng = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = false;
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao = new boolean[this.getTablePlayerNumber()];
		_xian_ming_zhao_not = new boolean[this.getTablePlayerNumber()];
		_is_chi_type = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
		_game_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_game_score, 0);
		_can_hu_pai_card = 0; // 要胡的牌
		_is_dispatch = -1; // 是否是发的牌
		_is_zha_hu_card = -1;
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
			_xian_ming_zhao_not[i] = false;
			_is_chi_type[i] = new int[GameConstants.XPBH_MAX_WEAVE];
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

		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.XPBH_MAX_WEAVE; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
		}
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.XPBH_MAX_WEAVE; j++)
				_lou_weave_item[i][j] = new LouWeaveItem();
		}
		_weave_count = new int[this.getTablePlayerNumber()];
		_yao_weave_item = new WeaveItem[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_yao_weave_item[i] = new WeaveItem();
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

		if (is_mj_type(GameConstants.GAME_TYPE_XP_BOHU)) {
			_handler_dispath_card = new BHHandlerDispatchCard_XP();
			_handler_out_card_operate = new BHHandlerOutCardOperate_XP();
			_handler_gang = new BHHandlerGang_XP();
			_handler_chi_peng = new BHHandlerChiPeng_XP();

			_handler_chuli_firstcards = new BHHandlerChuLiFirstCard_XP();
			_handler_dispath_firstcards = new BHHandlerDispatchFirstCard_XP();
			_handler_piao_fen = new BHHandlerPiao_XP();

		}
		_handler_finish = new BHHandlerFinish();
		this.setMinPlayerCount(this.getTablePlayerNumber());

	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int seat_index) {
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
		if (is_mj_type(GameConstants.GAME_TYPE_XP_BOHU)) {
			_handler_dispath_card = new BHHandlerDispatchCard_XP();
			_handler_out_card_operate = new BHHandlerOutCardOperate_XP();
			_handler_gang = new BHHandlerGang_XP();
			_handler_chi_peng = new BHHandlerChiPeng_XP();

			_handler_chuli_firstcards = new BHHandlerChuLiFirstCard_XP();
			_handler_dispath_firstcards = new BHHandlerDispatchFirstCard_XP();
			_handler_piao_fen = new BHHandlerPiao_XP();
		}

		_handler_finish = new BHHandlerFinish();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_long_count[i] = 0;
		} // 不能吃，碰

		has_first_qi_shou_ti = new boolean[this.getTablePlayerNumber()];
		_provider_hu = GameConstants.INVALID_SEAT;
		_cannot_chi = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_cannot_peng = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_hu_weave_count = new int[this.getTablePlayerNumber()]; //
		_da_pai_count = new int[this.getTablePlayerNumber()];
		_xiao_pai_count = new int[this.getTablePlayerNumber()];
		_hu_weave_items = new WeaveItem[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
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
		_guo_hu_hu_xi = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_INDEX];
		_is_chi_type = new int[this.getTablePlayerNumber()][GameConstants.XPBH_MAX_WEAVE];
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_is_di_hu = false;
		_can_hu_pai_card = 0; // 要胡的牌
		_is_dispatch = -1; // 是否是发的牌
		_is_zha_hu_card = -1;
		_game_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(_game_score, 0);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			has_shoot[i] = false;
			is_hands_up[i] = false;
			player_ti_count[i][0] = 0;
			player_ti_count[i][1] = 0;
			is_wang_diao[i] = false;
			is_wang_diao_wang[i] = false;
			is_wang_chuang[i] = false;
			_is_chi_type[i] = new int[GameConstants.XPBH_MAX_WEAVE];
		}
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			cards_has_wei[i] = 0;
		}
		card_for_fan_xing = 0;
		fan_xing_count = 0;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.XPBH_MAX_WEAVE; j++) {
				_hu_weave_items[i][j] = new WeaveItem();
			}
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

		GRR = new GameRoundRecord(GameConstants.XPBH_MAX_WEAVE, GameConstants.XPBH_MAX_COUNT, GameConstants.XPBH_MAX_INDEX);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.XPBH_MAX_COUNT);
		}
		_weave_count = new int[this.getTablePlayerNumber()];
		_yao_weave_item = new WeaveItem[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_yao_weave_item[i] = new WeaveItem();
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
		if(commonGameRuleProtos != null)
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

		_repertory_card = new int[GameConstants.XPBH_CARD_COUNT];
		shuffle(_repertory_card, GameConstants.CARD_DATA_BH_XP);

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();
		if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_CF)==1)
		{
			_game_status = GameConstants.GS_MJ_PIAO;// 设置状态
			game_start_paio_fen();
			return true;
		}
			
		return game_start_bh();

	}
	public void game_start_paio_fen() {

		this._handler = this._handler_piao_fen;
		this._handler_piao_fen.exe(this);
		return;

	}
	/**
	 * 开始 攸县红黑胡
	 * 
	 * @return
	 */
	public boolean game_start_bh() {
		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.XPBH_MAX_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this.switch_index_to_card(i, hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.XPBH_MAX_COUNT; j++) {
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

			for (int j = 0; j < GameConstants.XPBH_MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
//		for (int i = 0; i < playerCount; i++) {
//			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
//					GRR._weave_count[i], i, i);
//			if (this._playerStatus[i]._hu_card_count > 0) {
//				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
//			}
//		}
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
	 * int hand_cards[][] = new int[playerCount][GameConstants.XPBH_MAX_COUNT];
	 * // 发送数据 for (int i = 0; i < playerCount; i++) { int hand_card_count =
	 * this._logic.switch_to_cards_data(this.GRR._cards_index[i],
	 * hand_cards[i]); gameStartResponse.addCardsCount(hand_card_count); }
	 * 
	 * // 发送数据 for (int i = 0; i < playerCount; i++) {
	 * Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
	 * 
	 * // 只发自己的牌 gameStartResponse.clearCardData(); for (int j = 0; j <
	 * GameConstants.XPBH_MAX_COUNT; j++) {
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
	 * for (int j = 0; j < GameConstants.XPBH_MAX_COUNT; j++) {
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
	 * int hand_cards[][] = new int[playerCount][GameConstants.XPBH_MAX_COUNT];
	 * // 发送数据 for (int i = 0; i < playerCount; i++) { int hand_card_count =
	 * this._logic.switch_to_cards_data(this.GRR._cards_index[i],
	 * hand_cards[i]); gameStartResponse.addCardsCount(hand_card_count); }
	 * 
	 * // 发送数据 for (int i = 0; i < playerCount; i++) {
	 * Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
	 * 
	 * // 只发自己的牌 gameStartResponse.clearCardData(); for (int j = 0; j <
	 * GameConstants.XPBH_MAX_COUNT; j++) {
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
	 * for (int j = 0; j < GameConstants.XPBH_MAX_COUNT; j++) {
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
			send_count = (GameConstants.XPBH_MAX_COUNT - 1);
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
		//////// //// for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
		//////// //// GRR._cards_index[i][j] = 0;
		//////// //// }
		//////// //// }
		//////// //// int send_count = (GameConstants.XPBH_MAX_COUNT );
		//////// ////
		// _repertory_card[_all_card_len - GRR._left_card_count] = 0x07;
		// if(this.getTablePlayerNumber() == GameConstants.GAME_PLAYER_HH)
		// {
		// for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
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
//		 int[] realyCards = new int[] {
//				 19, 6, 20, 2, 34, 26, 17, 9, 40, 26, 18, 38, 41, 35, 18, 8, 39, 1, 18, 10, 10, 19, 38, 36, 35, 5, 21, 4, 37, 22, 7, 8, 2, 26, 38, 41, 39, 8, 5, 5, 9, 17, 22, 42, 21, 23, 35, 6, 39, 22, 4, 25, 33, 41, 37, 24, 21, 26, 20, 18, 9, 19, 25, 40, 39, 20, 34, 10, 9, 7, 38, 3, 2, 22, 33, 33, 42, 25, 1, 40, 42, 33, 25, 34, 20, 17, 24, 3, 1, 6, 36, 17, 5, 42, 35, 4, 8, 40, 36, 24, 37, 41, 37, 23, 34, 1, 23, 19, 3, 36, 7, 7, 3, 24, 2, 23, 10, 4, 6, 21
//		 };
//		 
//		 
//		 this._cur_banker = 3;
//		 testRealyCard(realyCards);
//		 DEBUG_CARDS_MODE = true;

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				{
					if (debug_my_cards.length > 26) {
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
			for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
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
			send_count = (GameConstants.XPBH_MAX_COUNT - 1);
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
			for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = 0;
			send_count = (GameConstants.XPBH_MAX_COUNT - 1);
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

		if (is_mj_type(GameConstants.GAME_TYPE_XP_BOHU)) {
			ret = this.handler_game_finish_phz_bh(seat_index, reason);
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean handler_game_finish_phz_bh(int seat_index, int reason) {
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

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}
			
			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(this._is_zha_hu_card);  //1是胡牌， 0是诈胡

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
				for (int j = 0; j < GameConstants.XPBH_MAX_COUNT; j++) {
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

			} else if (is_mj_type(GameConstants.GAME_TYPE_XP_BOHU)) {
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
						for(int index = 0; index<GRR._weave_items[i][j].weave_card_count;index++)
						{
							weaveItem_item.addWeaveCard( GRR._weave_items[i][j].weave_card[index]);
						}
						all_hu_xi += GRR._weave_items[i][j].hu_xi;
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						if (j < GRR._weave_count[i]) {
							weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						} else {
							weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
						}

						weaveItem_array.addWeaveItem(weaveItem_item);
					}
					if (_yao_weave_item[i].weave_card_count > 0) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(this._yao_weave_item[i].provide_player);
						weaveItem_item.setPublicCard(this._yao_weave_item[i].public_card);
						weaveItem_item.setWeaveKind(this._yao_weave_item[i].weave_kind);
						weaveItem_item.setHuXi(this._yao_weave_item[i].hu_xi);
						for (int j = 0; j < this._yao_weave_item[i].weave_card_count; j++) {
							weaveItem_item.addWeaveCard(this._yao_weave_item[i].weave_card[j]);
						}

						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				} else {

					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						for(int index = 0; index<GRR._weave_items[i][j].weave_card_count;index++)
						{
							weaveItem_item.addWeaveCard( GRR._weave_items[i][j].weave_card[index]);
						}
						all_hu_xi += GRR._weave_items[i][j].hu_xi;
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						if (j < GRR._weave_count[i]) {
							weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						} else {
							weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
						}

						weaveItem_array.addWeaveItem(weaveItem_item);
					}
					int weave_count = GRR._weave_count[i];
					if (_yao_weave_item[i].weave_card_count > 0) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(this._yao_weave_item[i].provide_player);
						weaveItem_item.setPublicCard(this._yao_weave_item[i].public_card);
						weaveItem_item.setWeaveKind(this._yao_weave_item[i].weave_kind);
						weaveItem_item.setHuXi(this._yao_weave_item[i].hu_xi);
						for (int j = 0; j < this._yao_weave_item[i].weave_card_count; j++) {
							weaveItem_item.addWeaveCard(this._yao_weave_item[i].weave_card[j]);
						}
						weave_count ++;
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
					game_end.setWinLziFen(weave_count);
					if (_hu_weave_count[i] > 0) {
						for (int j = 0; j < _hu_weave_count[i]; j++) {
							if(_hu_weave_items[i][j].weave_card_count==0)
								continue;
							WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
							weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
							weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
							weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
							weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
							for(int index = 0; index<_hu_weave_items[i][j].weave_card_count;index++)
							{
								weaveItem_item.addWeaveCard(_hu_weave_items[i][j].weave_card[index]);
							}
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
			for(int i = 0; i< this.getTablePlayerNumber();i++)
			{
				if(reason == GameConstants.Game_End_NORMAL&&i==seat_index){
					continue;
				}
				_player_result.pao[i] = 0;
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
		if (this._ti_two_long[seat_index] == true) {
			return 0;
		}

		return analyse_chi_hu_card_bh(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type, hu_xi,dispatch);

	}
	public int  switch_index_to_card(int seat_index,int cards[]){
		int count = 0;
		for(int i = 0; i < GameConstants.XPBH_MAX_INDEX ; i++){
			int card = 0;
			if(GRR._cards_index[seat_index][i] > 0){
				card  =  _logic.switch_to_card_data(i);
			}
//			if(_logic.yao_card(card) == true)
//				card |= GameConstants.XPBH_IS_YAO;
			if(GRR._can_ting_out_index[seat_index][i]>0)
			{
				card |= GameConstants.XPBH_IS_TING_XIAN_MING_ZHAO;
			}
			else if(GRR._cannot_out_index[seat_index][i]>0)
			{
				card |= GameConstants.XPBH_IS_XIAN_MING_ZHAO;
			}
			if(GRR._must_out_index[seat_index][i] >0 )
			{
				card |= GameConstants.XPBH_IS_MUST_OUT_CARD;
			}
			for(int j = 0; j< GRR._cards_index[seat_index][i] ;j++)
			{
				cards[count++] = card;
			}
			
		}
		return count ;
	}
	public void set_xian_ming_zhao( int seat_index,int weave_kind,int card[],int card_count){
		int she_index[] = new int[GameConstants.XPBH_MAX_INDEX];
		
		if(weave_kind != GameConstants.WIK_BH_SHE && (weave_kind&GameConstants.WIK_BH_SHE)!=0){
			for(int i = 0; i<getTablePlayerNumber();i++)
			{
				if(i == seat_index)
					continue;
				Arrays.fill(she_index, 0);
				for(int j = 0; j< card_count; j++)
				{
					if(this._logic.yao_card(card[j])== true)
						return ;
					if(she_index[this._logic.switch_to_card_index(card[j]) ]!=0)
						continue;
					if((card[j]&GameConstants.WIK_BH_SHE)!=0){
						she_index[this._logic.switch_to_card_index(card[j]) ]++;
						cannot_outcard(i, 1, (card[j]&0xFF), true);
					}
				}
				
			}
			
		}
	}
	public void can_out_xian_ming_zhao_card(int seat_index){
		Arrays.fill(GRR._must_out_index[seat_index],0);
	
		Arrays.fill(GRR._can_ting_out_index[seat_index],0);

		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++)
		{
			if(GRR._cannot_out_index[seat_index][i] >= 1&&GRR._cards_index[seat_index][i]>0)
			{
				int cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
				Arrays.fill(cards_index,0);
				for(int j = 0; j< GameConstants.XPBH_MAX_INDEX;j++)
				{
					if( j == i && GRR._cards_index[seat_index][j]>0)
					{
						cards_index[j] = GRR._cards_index[seat_index][j];
						cards_index[j] --;
					}
					else{
						cards_index[j] = GRR._cards_index[seat_index][j];
					}
				}
				_playerStatus[seat_index]._hu_card_count = get_hh_ting_card_twenty(
						_playerStatus[seat_index]._hu_cards, cards_index ,
						GRR._weave_items[seat_index], GRR._weave_count[seat_index],seat_index,seat_index);

				int ting_cards[] = _playerStatus[seat_index]._hu_cards;
				int ting_count = _playerStatus[seat_index]._hu_card_count;

				if (ting_count > 0) {
					GRR._can_ting_out_index[seat_index][i] = 1;
					
				} 
			}
		}
		cannot_outcard(seat_index, 0, 0, true);
		
		if(this._xian_ming_zhao[seat_index] == false)
			return ;
		for(int i = 0 ;i < GameConstants.XPBH_MAX_INDEX;i++)
		{
			if(_logic.yao_card(_logic.switch_to_card_data(i))==true)
				continue;
			if(GRR._cards_index[seat_index][i]>0&&GRR._cards_index[seat_index][i]<3)
			{
				
				if(GRR._cannot_out_index[seat_index][i] != 1)
				{
					must_out_card(seat_index, 1,i, true);
					continue;
				}
				int cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
				Arrays.fill(cards_index,0);
				for(int j = 0; j< GameConstants.XPBH_MAX_INDEX;j++)
				{
					if( j == i && GRR._cards_index[seat_index][j]>0)
					{
						cards_index[j] = GRR._cards_index[seat_index][j];
						cards_index[j] --;
					}
					else{
						cards_index[j] = GRR._cards_index[seat_index][j];
					}
				}
				int ting_cards[] = new int[GameConstants.XPBH_MAX_COUNT];
				int ting_count = 0;
				ting_count = get_hh_ting_card_twenty(
						ting_cards, cards_index ,
						GRR._weave_items[seat_index], GRR._weave_count[seat_index],seat_index,seat_index);

				

				if (ting_count > 0) {
					must_out_card(seat_index, 1,i, true);
				}
				
			}
		}
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
	public int analyse_chi_hu_card_bh(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi_hh[],boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.XPBH_MAX_INDEX];
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}



		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;

		for(int i = 0; i< GameConstants.XPBH_MAX_WEAVE;i++)
		{
			this._hu_weave_items[seat_index][i] = new WeaveItem();
		}
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, this._yao_weave_item[seat_index], weaveItems, weaveCount, seat_index, provider_index,
				cur_card, analyseItemArray, false, hu_xi,dispatch,this._hu_weave_items[seat_index]);
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
		this._hu_weave_count[seat_index] = hu_xi[0];
		int hu_pai_type = 0;
		this._yao_weave_item[seat_index].weave_card[this._yao_weave_item[seat_index].weave_card_count++] = cur_card;
		for(int i = 0; i<this._yao_weave_item[seat_index].weave_card_count;i++ ){
			for(int j = hu_xi[0]-1;j>=0;j--){
				boolean flag = false;
				int count = 0;
				if(this._hu_weave_items[seat_index][j].weave_kind != 0)
					hu_pai_type = this._hu_weave_items[seat_index][j].weave_kind;
				for(int k = 0; k< this._hu_weave_items[seat_index][j].weave_card_count;k++)
				{	
					if((!(this._hu_weave_items[seat_index][j].weave_card[k] == this._yao_weave_item[seat_index].weave_card[i])
							&&flag == false)||flag == true)
					{
						this._hu_weave_items[seat_index][j].weave_card[count++] = this._hu_weave_items[seat_index][j].weave_card[k];
						
					}
					else{
						flag = true;
					}
				}
				if(flag == true)
				{
					this._hu_weave_items[seat_index][j].weave_card_count = count;
					break;
				}
			}
		
		}
		this._yao_weave_item[seat_index].weave_card_count--;
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if ( (seat_index == provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if ( (seat_index != provider_index)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		}
		if(hu_pai_type == GameConstants.WIK_BH_PENG)
		{
			chiHuRight.opr_or(GameConstants.CHR_BH_PENG_HU);
		}
		else if(hu_pai_type == GameConstants.WIK_BH_KAIZ)
		{
			chiHuRight.opr_or(GameConstants.CHR_BH_KZH_HU);
		}
		else if(hu_pai_type == GameConstants.WIK_BH_DAGUN)
		{
			chiHuRight.opr_or(GameConstants.CHR_BH_DGU_HU);
		}else if(hu_pai_type == GameConstants.WIK_BH_SHE)
		{
			chiHuRight.opr_or(GameConstants.CHR_BH_SHE_HU);
		}
		else if(hu_pai_type == GameConstants.WIK_BH_ZHUA_LONG)
		{
			chiHuRight.opr_or(GameConstants.CHR_BH_ZHL_HU);
		}
		return cbChiHuKind;
	}

	/***
	 * /** 福禄寿获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */

	public int estimate_player_ti_wei_respond_phz_chd(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		bAroseAction = _logic.check_she(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card_data);

//		if (bAroseAction != GameConstants.WIK_NULL) {
//			this.exe_gang(seat_index, seat_index, card_data, bAroseAction, GameConstants.XPBH_TYPE_SHE, true, true, false, 1000);
//		}
		for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
			int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
			// 转换判断
			if ((weave_kind & GameConstants.WIK_BH_SHE) == 0)
				continue;
			boolean is_she = false;
			for (int i = 0; i < GRR._weave_items[seat_index][weave_index].weave_card_count; i++) {
				if (GRR._weave_items[seat_index][weave_index].weave_card[i] == (card_data | GameConstants.WIK_BH_SHE)) {
					is_she = true;
					break;
				}
			}
			if (is_she == true) {
				bAroseAction = GameConstants.WIK_BH_ZHUA_LONG;
//				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_BH_ZHUA_LONG, GameConstants.XPBH_TYPE_ZHUA_LONG, true, true, false, 1000);
				break;
			}

		}
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
				// 转换判断
				if ((weave_kind & GameConstants.WIK_BH_PENG) == 0)
					continue;
				boolean is_peng = false;
				for (int i = 0; i < GRR._weave_items[seat_index][weave_index].weave_card_count; i++) {
					if (GRR._weave_items[seat_index][weave_index].weave_card[i] == (card_data | GameConstants.WIK_BH_PENG)) {
						is_peng = true;
						break;
					}
				}
				if (is_peng == true) {
					pao_type[0] = GameConstants.XPBH_TYPE_DA_GUN;
					bAroseAction = GameConstants.WIK_BH_DAGUN;
					break;
				}

			}
		}

		for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
			int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
			// 转换判断
			if ((weave_kind & GameConstants.WIK_BH_SHE) == 0)
				continue;
			boolean is_she = false;
			for (int i = 0; i < GRR._weave_items[seat_index][weave_index].weave_card_count; i++) {
				if (GRR._weave_items[seat_index][weave_index].weave_card[i] == (card_data | GameConstants.WIK_BH_SHE)) {
					is_she = true;
					break;
				}
			}
			if (is_she == true) {
				pao_type[0] = GameConstants.XPBH_TYPE_KAI_ZHAI;
				bAroseAction = GameConstants.WIK_BH_KAIZ;
				break;
			}

		}
		//
		return bAroseAction;
	}

	public int estimate_player_hu_pai(int seat_index, int card, boolean bDisDatch) {
//		int bAroseAction = 0;// 出现(是否)有
//		if (_logic.is_yao_card(card)) {
//			return 1;
//		}
//		int action = _logic.check_peng(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card);
//		if (action != 0) {
//
//			return 1;
//		}
//		boolean bAction = false;
//		int type_count[] = new int[10];
//		int type_eat_count[] = new int[1];
//		int shang_type_count[] = new int[10];
//		int shang_eat_count[] = new int[1];
//		int lou_count[] = new int[20];
//		int cur_card_index = _logic.switch_to_card_index(card);
//		_logic.check_chi(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card, type_count, type_eat_count);
//		_logic.check_shang(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index], card, shang_type_count,
//				shang_eat_count);
//		for (int i = 0; i < type_eat_count[0]; i++) {
//			if (GRR._cards_index[seat_index][cur_card_index] == 0) {
//				return 1;
//			} else {
//
//				int temp_cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
//				for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
//					temp_cards_index[j] = GRR._cards_index[seat_index][j];
//				}
//				int kind_card[] = new int[3];
//				int kind_card_count = _logic.get_kind_card(type_count[i], card, kind_card);
//				for (int j = 0; j < kind_card_count; j++) {
//					temp_cards_index[_logic.switch_to_card_index(kind_card[j])]--;
//				}
//				bAction = _logic.check_lou_weave(temp_cards_index, GRR._weave_items[seat_index], GRR._weave_count[seat_index], card, lou_count[i],
//						_lou_weave_item[seat_index], i);
//				if (bAction == true) {
//					return 1;
//
//				}
//			}
//
//		}
		return 1;
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
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;
		
			playerStatus = _playerStatus[i];
			int card_count = 0;
			for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {

				if (this.GRR._cards_index[i][j] < 3 || GRR._cannot_out_index[i][j] == 0)
					card_count += this.GRR._cards_index[i][j];
			}

			if (card_count == 2)
				continue;
			if (this._cannot_peng[i][_logic.switch_to_card_index(card)] != 0)
				continue;
			if (_playerStatus[i].lock_huan_zhang() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card);
				if (action != 0) {

					playerStatus.add_action(action);
					playerStatus.add_hh_peng(card, action, seat_index);
					bAroseAction = true;
					if (_logic.is_yao_card(card) == false)
					{		
						player_pass[i] = 1;
					}
					else
						playerStatus.delete_action(GameConstants.WIK_BH_NULL);
				}
			}
		}
		int peng_index = -1;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			playerStatus = _playerStatus[i];
			if (player_pass[i] == 1) {
				if(this._xian_ming_zhao[i] == true)
				{
					_playerStatus[i].clean_action();
					_playerStatus[i].clean_status();
				}
				else{
					_playerStatus[i].add_action(GameConstants.WIK_NULL);
					_playerStatus[i].add_pass(card, seat_index);
					peng_index = i;
				}
				

			}
		}
		do {

			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
			next_index++;
			for (int i = 0; i < GameConstants.XPBH_MAX_WEAVE; i++) {
				_lou_weave_item[chi_seat_index][i] = new LouWeaveItem();
			}
			int chi_index = 0;
			if (_logic.is_yao_card(card))
				break;
			if(this._xian_ming_zhao[chi_seat_index] == true)
				continue;
			if (this._cannot_chi[chi_seat_index][_logic.switch_to_card_index(card)] != 0)
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
			if (this._playerStatus[chi_seat_index].has_bh_peng())
				continue;
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {
				// 这里可能有问题 应该是 |=
				boolean bAction = false;
				int type_count[] = new int[10];
				int weave_card[] = new int[10];
				int type_eat_count[] = new int[1];
				int shang_type_count[] = new int[10];
				int shang_eat_count[] = new int[1];
				int lou_count[] = new int[10];
				int card_count = 0;
				for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {

					if (this.GRR._cards_index[chi_seat_index][j] < 3 || GRR._cannot_out_index[chi_seat_index][j] == 0)
						card_count += this.GRR._cards_index[chi_seat_index][j];
				}
				int chi_pai_index[] = new int [GameConstants.XPBH_MAX_INDEX];
				for(int i = 0; i< GameConstants.XPBH_MAX_INDEX ; i ++){
					chi_pai_index[i] = GRR._cards_index[chi_seat_index][i];
				}
				for(int i = 0 ; i< this._yao_weave_item[chi_seat_index].weave_card_count;i++){
					chi_pai_index[_logic.switch_to_card_index(this._yao_weave_item[chi_seat_index].weave_card[i])]++;
				}
				_logic.check_chi(chi_pai_index,GRR._weave_items[chi_seat_index],GRR._weave_count[chi_seat_index], card, type_count,weave_card, type_eat_count);
				_logic.check_shang(GRR._cards_index[chi_seat_index],GRR._weave_items[chi_seat_index],GRR._weave_count[chi_seat_index], card, shang_type_count ,shang_eat_count);
				int  flag_int = 0;
				for(int i = 0; i< type_eat_count[0] ; i++){
					if(card == weave_card[i])
						flag_int = 1;
				}
				for(int i = 0; i<type_eat_count[0];i++){
					int cur_flag_int = 0;
					
					if(card == weave_card[i])
						cur_flag_int = 1;
					if(GRR._cards_index[chi_seat_index][cur_card_index] == 0&&flag_int == 0 )
					{

						_lou_weave_item[chi_seat_index][i].nWeaveKind = type_count[i];
						_playerStatus[chi_seat_index].add_action(type_count[i]);
						_playerStatus[chi_seat_index].add_chi_hh(card, type_count[i], seat_index, _lou_weave_item[chi_seat_index][i].nCount,
								_lou_weave_item[chi_seat_index][i].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					} else {
						int index = type_count[i]&0xf;
						int temp_cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
						for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
							temp_cards_index[j] = chi_pai_index[j];
						}
						
						int chi_type = type_count[i];
						int center_card = 0;
						int weave_count = GRR._weave_count[chi_seat_index];
						if(cur_flag_int == 1){
							chi_type = type_count[i]-index;
							center_card = weave_card[i];
							
						}
						int kind_card[] = new int[3];
						int kind_card_count = _logic.get_kind_card(chi_type, card, kind_card);
						if(center_card != 0)
						{
							int chi_count = 0;
							for(int kind_index = 0; kind_index<kind_card_count;kind_index++){
								if(kind_card[kind_index] != center_card){
									kind_card[chi_count++] = kind_card[kind_index];
								}
							}
							kind_card_count--;
							weave_count = 0;
						}
						for (int j = 0; j < kind_card_count; j++) {
							temp_cards_index[_logic.switch_to_card_index(kind_card[j])]--;
						}
						bAction = _logic.check_lou_weave(temp_cards_index, GRR._weave_items[chi_seat_index], weave_count, card,
								lou_count[i], _lou_weave_item[chi_seat_index], i,chi_type);
						_lou_weave_item[chi_seat_index][i].nWeaveKind = type_count[i];
						int type = GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|
								GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119|
								GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228|
								GameConstants.WIK_BH_CHI_H;
						if((chi_type&type)!=0||flag_int==1)
						{
							int shang_index = type_count[i]&0xf;	
							int shang_type = shang_index| GameConstants.WIK_BH_SHANG;
							_lou_weave_item[chi_seat_index][i].nLouWeaveKind[_lou_weave_item[chi_seat_index][i].nCount++][0] = shang_type;
							bAction = true;
						}
						if (bAction == true) {
//							if (card_count == 5)
//								continue;
							_playerStatus[chi_seat_index].add_action(type_count[i]);
							_playerStatus[chi_seat_index].add_chi_hh(card, type_count[i], seat_index, _lou_weave_item[chi_seat_index][i].nCount,
									_lou_weave_item[chi_seat_index][i].nLouWeaveKind);
							player_pass[chi_seat_index] = 1;

						}
					}

				}
				for (int i = 0; i < shang_eat_count[0]; i++) {
					if (GRR._cards_index[chi_seat_index][cur_card_index] == 0) {
						_playerStatus[chi_seat_index].add_action(shang_type_count[i]);
						_playerStatus[chi_seat_index].add_chi_hh(card, shang_type_count[i], seat_index, _lou_weave_item[chi_seat_index][i].nCount,
								_lou_weave_item[chi_seat_index][i].nLouWeaveKind);
						player_pass[chi_seat_index] = 1;
					}
				}

				// 结果判断
				if (_playerStatus[chi_seat_index].has_action()) {
					bAroseAction = true;
				}
			}
		} while (next_index < 2);

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

	// 添加落牌组合
	public boolean add_lou_weave(int luoCode, int target_player, int target_card, int provide_player, int target_action,boolean is_delete,int weave_index_count[]) {
		boolean bSuccess = false;
		int index = 0;
		for (int i = 0; i < GameConstants.XPBH_MAX_WEAVE; i++) {
			if ((_lou_weave_item[target_player][i].nWeaveKind == 0) || _lou_weave_item[target_player][i].nWeaveKind == target_action) {
				index = i;
				break;
			}
		}
		if (_lou_weave_item[target_player][index].nCount == 0)
			return true;
		if (luoCode < 0)
			return true;
	
		if ((_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & GameConstants.WIK_BH_SHANG) != 0
				|| (_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & GameConstants.WIK_BH_XIA) != 0) {
			int shang_index = _lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & (0xF);
			if(shang_index == 0){
				shang_index = GRR._weave_count[target_player]-1;
				for(int i = GRR._weave_items[target_player][shang_index].weave_card_count++; i>0;i-- )
				{
					GRR._weave_items[target_player][shang_index].weave_card[i] = GRR._weave_items[target_player][shang_index].weave_card[i-1];
				}
				int cbRemoveCard[] = {target_card};
				if (is_delete == true && !_logic.remove_cards_by_index(GRR._cards_index[target_player], cbRemoveCard, 1)) {
					log_player_error(target_player, "上吃牌删除出错");
					return false;
				}
				_logic.get_weave_hu_xi(this.GRR._weave_items[target_player], shang_index, this.GRR._weave_count[target_player]);

			}
			else{
				shang_index--;
				shang_index = weave_index_count[shang_index];
				int cbRemoveCard[] = {target_card};
				if (is_delete == true && !_logic.remove_cards_by_index(GRR._cards_index[target_player], cbRemoveCard, 1)) {
					log_player_error(target_player, "上吃牌删除出错");
					return false;
				}
				GRR._weave_items[target_player][shang_index].weave_card[GRR._weave_items[target_player][shang_index].weave_card_count++] = target_card;
				
				_logic.get_weave_hu_xi(this.GRR._weave_items[target_player], shang_index, this.GRR._weave_count[target_player]);

			}
			
		}else if ((_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & GameConstants.WIK_BH_SHANG_TWO) != 0
				|| (_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & GameConstants.WIK_BH_XIA_TWO) != 0) {
			int shang_index = _lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & (0xF);
			if(shang_index == 0){
				return false;

			}
			else{
				shang_index--;
				shang_index = weave_index_count[shang_index];
				int cbRemoveCard[] = new int[2];
				int count = 0;
				if((_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & GameConstants.WIK_BH_SHANG_TWO) != 0)
					cbRemoveCard[count++] = target_card-1;
				else
					cbRemoveCard[count++] = target_card+1;
				if(is_delete == true)
					cbRemoveCard[count++] = target_card;
				if ( !_logic.remove_cards_by_index(GRR._cards_index[target_player], cbRemoveCard, count)) {
					log_player_error(target_player, "上吃牌删除出错");
					return false;
				}
				for(int i = 0; i < count ;i++){
					GRR._weave_items[target_player][shang_index].weave_card[GRR._weave_items[target_player][shang_index].weave_card_count++] = cbRemoveCard[i];
					
				}
				
				_logic.get_weave_hu_xi(this.GRR._weave_items[target_player], shang_index, this.GRR._weave_count[target_player]);

			}
			
		} else if (_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] != GameConstants.WIK_NULL) {
			// 删除扑克
			int weave_card[] = new int[3];
			if (_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] == 0)
				return false;
			int heng_type = GameConstants.WIK_BH_CHI_H
					|GameConstants.WIK_BH_CHI_A98|GameConstants.WIK_BH_CHI_A99|GameConstants.WIK_BH_CHI_AA9|GameConstants.WIK_BH_CHI_119
					|GameConstants.WIK_BH_CHI_337|GameConstants.WIK_BH_CHI_228;
			int weave_index = _lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & (0xF);
			int chi_kind = _lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0]&heng_type;
			weave_index--;
			if(weave_index != -1)
			{
				weave_index = weave_index_count[weave_index];
				 int remove_card[] = new int[3];
				 int remove_count = this._logic.get_must_kind_card(chi_kind,GRR._weave_items[target_player][weave_index].center_card,GRR._weave_items[target_player][weave_index].center_card,remove_card);
				 int other_card = 0;
				 for(int i = 0; i< remove_count;i++){
					 int temp_remove[] = new int[1];
			    		temp_remove[0] = remove_card[i];
			    		if( remove_card[i] != target_card)
			    			other_card = remove_card[i];
			    		if (is_delete==true&&!_logic.remove_cards_by_index(GRR._cards_index[target_player], temp_remove, 1)) {
							log_player_error(target_player, "碰牌删除出错");
							return false;
			    		}
				 }
				 int kind_count = GRR._weave_items[target_player][weave_index].weave_card_count-1;
			     	int kind_card [] = new int[kind_count];
				    for(int i = 0; i< kind_count; i++){
				    	kind_card[i] = GRR._weave_items[target_player][weave_index].weave_card[i];
				    }
			        GRR._weave_items[target_player][weave_index].weave_card_count = 0;
					GRR._weave_items[target_player][weave_index].weave_kind |= chi_kind;
					GRR._weave_items[target_player][weave_index].provide_player = _out_card_player;
					if(target_card == (kind_card[0]&0xff)){
						GRR._weave_items[target_player][weave_index].weave_card[GRR._weave_items[target_player][weave_index].weave_card_count++] = kind_card[0]&0xff;
						for(int i =GRR._weave_items[target_player][weave_index].weave_card_count;i < GRR._weave_items[target_player][weave_index].weave_card_count+kind_count;i++) 	
							GRR._weave_items[target_player][weave_index].weave_card[i] = kind_card[0] ;
						GRR._weave_items[target_player][weave_index].weave_card_count += kind_count;
						GRR._weave_items[target_player][weave_index].weave_card[GRR._weave_items[target_player][weave_index].weave_card_count++] = remove_card[0];
						GRR._weave_items[target_player][weave_index].weave_card[GRR._weave_items[target_player][weave_index].weave_card_count++] = remove_card[1];
					}else{
						GRR._weave_items[target_player][weave_index].weave_card[GRR._weave_items[target_player][weave_index].weave_card_count++] = target_card;
						GRR._weave_items[target_player][weave_index].weave_card[GRR._weave_items[target_player][weave_index].weave_card_count++] = other_card;
						GRR._weave_items[target_player][weave_index].weave_card[GRR._weave_items[target_player][weave_index].weave_card_count++] = kind_card[0]&0xff;
						for(int i =GRR._weave_items[target_player][weave_index].weave_card_count;i < GRR._weave_items[target_player][weave_index].weave_card_count+kind_count;i++) 	
							GRR._weave_items[target_player][weave_index].weave_card[i] = kind_card[i- GRR._weave_items[target_player][weave_index].weave_card_count] ;
						GRR._weave_items[target_player][weave_index].weave_card_count += kind_count;
					}
					
					_logic.get_weave_hu_xi(GRR._weave_items[target_player], weave_index, GRR._weave_count[target_player]);
					set_xian_ming_zhao(target_player, GRR._weave_items[target_player][weave_index].weave_kind, GRR._weave_items[target_player][weave_index].weave_card,GRR._weave_items[target_player][weave_index].weave_card_count);
					return true;
				  
			}
			int weave_card_count = _logic.get_kind_card(_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0]&heng_type, target_card, weave_card);
			
			int same_type = GameConstants.WIK_BH_PENG|GameConstants.WIK_BH_SHE|GameConstants.WIK_BH_KAIZ|GameConstants.WIK_BH_DAGUN
					|GameConstants.WIK_BH_ZHUA_LONG;
			if((_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0] & heng_type) != 0)
			{
				int remove_card[] = new int[3];
				int remove_count = 0;
				int card_action[] = new int[3];
				int card_count[] = new int[3];
				int card_data[] = new int[3];
				int two_delete_card[] = new int[1];
				int target_card_index = -1;
				for(int i = 0; i< weave_card_count;i++)
				{
					if(GRR._cards_index[target_player][_logic.switch_to_card_index(weave_card[i])] == 0){
						boolean flag = false;
						for(int j = 0; j < _yao_weave_item[target_player].weave_card_count;j++){
							if(_yao_weave_item[target_player].weave_card[j] == weave_card[i])
							{
								flag = true;
								break;
							}
						}
						if(flag == false)
							remove_card[remove_count++] = weave_card[i];
					}
					else
						two_delete_card[0]  = weave_card[i];
			
				}
				if(remove_count >= 2)
				{
					int weave_count = 0;
					int user_card_count = 0;
					for(int i = 0; i<GRR._weave_count[target_player];i++){
						if((GRR._weave_items[target_player][i].weave_kind&same_type)!=0){
							boolean is_index = false;
							for(int k = 0; k<remove_count;k++){
								if((GRR._weave_items[target_player][i].weave_card[0]&0xff)==remove_card[k])
								{
									is_index = true;
									break;
								}
							}
							
							
							if(is_index == true){
								if((GRR._weave_items[target_player][i].weave_card[0]&0xff) == target_card )
									target_card_index = user_card_count;
								card_action[user_card_count] = GRR._weave_items[target_player][i].weave_kind;
								card_count[user_card_count] = GRR._weave_items[target_player][i].weave_card_count;
								card_data[user_card_count++] = GRR._weave_items[target_player][i].weave_card[0];
								GRR._weave_items[target_player][i].weave_card_count = 0;
								GRR._weave_items[target_player][i].hu_xi = 0;
								GRR._weave_items[target_player][i].weave_kind = 0;
							}
							else {
								if(weave_count!=i){
									GRR._weave_items[target_player][weave_count].center_card = GRR._weave_items[target_player][i].center_card;
									GRR._weave_items[target_player][weave_count].weave_card_count = GRR._weave_items[target_player][i].weave_card_count;
									GRR._weave_items[target_player][i].weave_card_count = 0;
									for (int j = 0; j < GRR._weave_items[target_player][weave_count].weave_card_count; j++) {
										GRR._weave_items[target_player][weave_count].weave_card[j] = GRR._weave_items[target_player][i].weave_card[j];
									}
									GRR._weave_items[target_player][weave_count].weave_kind = GRR._weave_items[target_player][i].weave_kind;
									GRR._weave_items[target_player][i].weave_card_count = 0;
									GRR._weave_items[target_player][i].weave_kind = 0;
									GRR._weave_items[target_player][i].hu_xi = 0;
								}
								weave_count++;
							}
			
							
						} else {
							if(weave_count!=i){
								GRR._weave_items[target_player][weave_count].center_card = GRR._weave_items[target_player][i].center_card;
								GRR._weave_items[target_player][weave_count].weave_card_count = GRR._weave_items[target_player][i].weave_card_count;
								for (int j = 0; j < GRR._weave_items[target_player][weave_count].weave_card_count; j++) {
									GRR._weave_items[target_player][weave_count].weave_card[j] = GRR._weave_items[target_player][i].weave_card[j];
								}
								GRR._weave_items[target_player][weave_count].weave_kind = GRR._weave_items[target_player][i].weave_kind;
								GRR._weave_items[target_player][i].weave_card_count = 0;
								GRR._weave_items[target_player][i].weave_kind = 0;
								GRR._weave_items[target_player][i].hu_xi = 0;
							}
							weave_count++;
						}
					}
					if(weave_count + user_card_count != GRR._weave_count[target_player]){
						log_player_error(target_player, "吃牌删除出错");
						return false;
					}
					if(two_delete_card[0] != 0){
						
						if(_logic.get_card(two_delete_card[0], _yao_weave_item[target_player].weave_card, _yao_weave_item[target_player].weave_card_count)){
							int yao_count = 0;
							boolean flag = false;
							for(int i = 0; i<_yao_weave_item[target_player].weave_card_count;i++){
								if(_yao_weave_item[target_player].weave_card[i] != two_delete_card[0]|| flag == true)
									_yao_weave_item[target_player].weave_card[yao_count++] = _yao_weave_item[target_player].weave_card[i];
								else 
									flag = true;
							}
							if(flag == false)
							{
								log_player_error(target_player, "吃牌删除出错");
									return false;
								
							}
							_yao_weave_item[target_player].weave_card_count = yao_count;
						}
				    	else{
				    		if (is_delete == true&&!_logic.remove_cards_by_index(GRR._cards_index[target_player], two_delete_card, 1)) {
								log_player_error(target_player, "碰牌删除出错");
								return false;
				    		}
								
				    	}
						
					}
					target_action = _lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0]-(_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0]&0xf);
					GRR._weave_count[target_player] -= user_card_count;
					int wIndex = GRR._weave_count[target_player]++;
					GRR._weave_items[target_player][wIndex].public_card = 1;
					GRR._weave_items[target_player][wIndex].center_card = target_card;
					GRR._weave_items[target_player][wIndex].weave_kind = target_action|card_action[0]|card_action[1]|card_action[2];
					
					GRR._weave_items[target_player][wIndex].weave_card_count = 0;
					if( target_card_index != -1  ){
						for(int j = GRR._weave_items[target_player][wIndex].weave_card_count; j<GRR._weave_items[target_player][wIndex].weave_card_count+card_count[target_card_index];j++){
							if(j == 0)
								GRR._weave_items[target_player][wIndex].weave_card[j] = target_card;
							else
								GRR._weave_items[target_player][wIndex].weave_card[j] = target_card|card_action[target_card_index];
						}
						GRR._weave_items[target_player][wIndex].weave_card_count += card_count[target_card_index];
						if( two_delete_card[0] != 0)
							GRR._weave_items[target_player][wIndex].weave_card[GRR._weave_items[target_player][wIndex].weave_card_count++] = two_delete_card[0];
						
					}
					else{
						GRR._weave_items[target_player][wIndex].weave_card[GRR._weave_items[target_player][wIndex].weave_card_count++] = target_card;
					}
					for(int i = 0; i< remove_count;i++){
						if( (card_data[i]&0xff) == target_card)
							continue;
						for(int j = GRR._weave_items[target_player][wIndex].weave_card_count; j<GRR._weave_items[target_player][wIndex].weave_card_count+card_count[i];j++){
							if(j == GRR._weave_items[target_player][wIndex].weave_card_count)
								GRR._weave_items[target_player][wIndex].weave_card[j] = card_data[i]&0xff;
							else
								GRR._weave_items[target_player][wIndex].weave_card[j] = card_data[i];
						}
						GRR._weave_items[target_player][wIndex].weave_card_count += card_count[i];
					}
					GRR._weave_items[target_player][wIndex].provide_player = provide_player;
					_logic.get_weave_hu_xi(GRR._weave_items[target_player], GRR._weave_count[target_player]-1, GRR._weave_count[target_player]);
					set_xian_ming_zhao(target_player, GRR._weave_items[target_player][wIndex].weave_kind, GRR._weave_items[target_player][wIndex].weave_card,GRR._weave_items[target_player][wIndex].weave_card_count);
					
					return true;
				}
			}
			int cbRemoveCard[] = new int[3];
			int count = 0;
			weave_card_count = _logic.get_kind_card(_lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0], target_card, weave_card);
			int temp_card[] = new int[3];
			int temp_count = 0;
			for (int j = 0; j < weave_card_count; j++) {

				cbRemoveCard[count++] = weave_card[j];
				if(is_delete == false && weave_card[j] == target_card)
					continue;
				temp_card[temp_count++] = weave_card[j];
			}
		
			if ( !this._logic.remove_cards_by_index(this.GRR._cards_index[target_player], temp_card, temp_count)) {
				this.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			int wIndex = this.GRR._weave_count[target_player]++;
			this.GRR._weave_items[target_player][wIndex].public_card = 1;
			this.GRR._weave_items[target_player][wIndex].center_card = target_card;
			this.GRR._weave_items[target_player][wIndex].weave_kind = _lou_weave_item[target_player][index].nLouWeaveKind[luoCode][0];
			GRR._weave_items[target_player][wIndex].weave_card[GRR._weave_items[target_player][wIndex].weave_card_count++] = target_card;
			
			for (int j = 0; j <  3; j++) {
				if(cbRemoveCard[j] == target_card)
					continue;
				GRR._weave_items[target_player][wIndex].weave_card[GRR._weave_items[target_player][wIndex].weave_card_count++] = cbRemoveCard[j];
			}
			this.GRR._weave_items[target_player][wIndex].provide_player = provide_player;
			_logic.get_weave_hu_xi(this.GRR._weave_items[target_player], wIndex, this.GRR._weave_count[target_player]);
			set_xian_ming_zhao(target_player, GRR._weave_items[target_player][wIndex].weave_kind, GRR._weave_items[target_player][wIndex].weave_card,GRR._weave_items[target_player][wIndex].weave_card_count);
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

		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_HU_PAI_TYPE, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);

		

		// 显示胡牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
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
			int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
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
		int cards[] = new int[GameConstants.XPBH_MAX_INDEX];
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
	public void countChiHuTimes(int seat_index, boolean isZimo,int provider_index) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		if (isZimo&&seat_index == provider_index ) {
			_player_result.zi_mo_count[seat_index]++;
		}
		else if(isZimo&&seat_index != provider_index){
			_player_result.jie_pao_count[seat_index]++;
		}
		else {
			_player_result.dian_pao_count[provider_index]++;
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
	public void process_chi_hu_player_score_bh(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
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
		
		this._hu_xi[seat_index] = all_hu_xi;
		
		if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_5_DF)==1)
			calculate_score = 5;
		else if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_4_DF)==1)
			calculate_score = 4;
		else if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_3_DF)==1)
			calculate_score = 3;
		else if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_2_DF)==1)
			calculate_score = 2;
		else if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_1_DF)==1)
			calculate_score = 1;
		int lChiHuScore = calculate_score;
		////////////////////////////////////////////////////// 自摸 算分
		if(this.getRuleValue(GameConstants.GAME_RULE_XP_BH_ZX)==1){
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

		}
		else{

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					// 胡牌分
					GRR._game_score[i] -= lChiHuScore;
					GRR._game_score[seat_index] += lChiHuScore;

				}

		}
		////////////////////////////////////////////////////// 自摸 算分

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}

			float s = 0;

			// 跑
			s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));

			// 胡牌分
			GRR._game_score[i] -= s*calculate_score;
			GRR._game_score[seat_index] += s*calculate_score;

		}

		for(int i = 0; i< this.getTablePlayerNumber();i++)
		{
			_game_score[i] = (int)GRR._game_score[i];
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

		if (_playerStatus[player.get_seat_index()]._is_pao_qiang || _playerStatus[player.get_seat_index()]._is_pao) {
			return false;
		}
		if (this._handler != null) {
			return _handler.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
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
		roomResponse.setStandTime(800);

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
		int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
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
		int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
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
		int cbCardIndexTemp[] = new int[GameConstants.XPBH_MAX_INDEX];
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.XPBH_MAX_INDEX;

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
			for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
			for (int k = 0; k < curPlayerStatus._action_weaves[i].weave_card_count; k++) {
				weaveItem_item.addWeaveCard(curPlayerStatus._action_weaves[i].weave_card[k]);
			}
			int eat_type = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9 | GameConstants.WIK_BH_CHI_119
					| GameConstants.WIK_BH_CHI_337 | GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_H | GameConstants.WIK_BH_CHI_L
					| GameConstants.WIK_BH_CHI_C | GameConstants.WIK_BH_CHI_R;
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
		roomResponse.setStandTime(800);
		if(sao == true)
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

				if ((weaveitems[j].weave_kind == GameConstants.WIK_BH_SHE) && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
					for (int k = 0; k < weaveitems[j].weave_card_count; k++) {
						weaveItem_item.addWeaveCard(0);
					}
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
					for (int k = 0; k < weaveitems[j].weave_card_count; k++) {
						weaveItem_item.addWeaveCard(weaveitems[j].weave_card[k]);
					}
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}

		}

		if (_yao_weave_item[seat_index].weave_card_count > 0) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(this._yao_weave_item[seat_index].provide_player);
			weaveItem_item.setPublicCard(this._yao_weave_item[seat_index].public_card);
			weaveItem_item.setWeaveKind(this._yao_weave_item[seat_index].weave_kind);
			weaveItem_item.setHuXi(this._yao_weave_item[seat_index].hu_xi);
			for (int k = 0; k < this._yao_weave_item[seat_index].weave_card_count; k++) {
				weaveItem_item.addWeaveCard(this._yao_weave_item[seat_index].weave_card[k]);
			}

			roomResponse.addWeaveItems(weaveItem_item);
		}
		this.send_response_to_other(seat_index, roomResponse);
		WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
		roomResponse.clearWeaveItems();
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				for (int k = 0; k < weaveitems[j].weave_card_count; k++) {
					weaveItem_item.addWeaveCard(weaveitems[j].weave_card[k]);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		if(_yao_weave_item[seat_index].weave_card_count>0){	
			weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(this._yao_weave_item[seat_index].provide_player);
			weaveItem_item.setPublicCard(this._yao_weave_item[seat_index].public_card);
			weaveItem_item.setWeaveKind(this._yao_weave_item[seat_index].weave_kind);
			weaveItem_item.setHuXi(this._yao_weave_item[seat_index].hu_xi);
			for (int k = 0; k < this._yao_weave_item[seat_index].weave_card_count; k++) {
				weaveItem_item.addWeaveCard(this._yao_weave_item[seat_index].weave_card[k]);
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		// 手牌--将自己的手牌数据发给自己
		this.switch_index_to_card(seat_index, cards);
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
			for (int j = 0; j < GameConstants.XPBH_MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				for (int k = 0; k < GRR._weave_items[i][j].weave_card.length; k++) {
					weaveItem_item.addWeaveCard(GRR._weave_items[i][j].weave_card[k]);
				}
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
		int hand_cards[] = new int[GameConstants.XPBH_MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.XPBH_MAX_COUNT; i++) {
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

			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);

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
	private void set_result_describe_bh() {
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
					

					if (type == GameConstants.CHR_BH_PENG_HU) {
						des += ",碰胡";
					}

					if (type == GameConstants.CHR_BH_SHE_HU) {
						des += ",舍胡";
					}
					if (type == GameConstants.CHR_BH_KZH_HU) {
						des += ",开斋胡";
					}
					if (type == GameConstants.CHR_BH_ZHL_HU) {
						des += ",抓龙胡";
					}
					if (type == GameConstants.CHR_BH_DGU_HU) {
						des += ",打滚胡";
					}

				}
			}

			GRR._result_des[i] = des;
		}
	}

	
	

	// 转转麻将结束描述
	public void set_result_describe(int seat_index) {
		
			set_result_describe_bh();
		
		

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
		return card&0xff;
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
			for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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

		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
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
			for (int j = 0; j < GameConstants.XPBH_MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.XPBH_MAX_COUNT];
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {

		return 4;
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

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_XPBH_OPERATE) {
			if(GRR == null)
				return false;
			_hu_weave_count[seat_index] = 0;
			if(_hu_weave_count[seat_index] != 0)
			{
				this.send_error_notify(seat_index, 1, "您已经，操作胡牌了，不能重复，操作!");
				return false;
			}
			Opreate_Xpbh_Request req = PBUtil.toObject(room_rq, Opreate_Xpbh_Request.class);
			if (req == null)
				return false;
			
			List<WeaveItemResponse> weaveItem_array = req.getWeaveItemList();
			for (int i = 0; i < req.getWeaveItemCount(); i++) {
				WeaveItemResponse weaveItem = weaveItem_array.get(i);

				_hu_weave_items[seat_index][i].weave_card_count = weaveItem.getWeaveCardCount();
				for (int j = 0; j < _hu_weave_items[seat_index][i].weave_card_count; j++) {
					_hu_weave_items[seat_index][i].weave_card[j] = weaveItem.getWeaveCard(j);
				}
				if(i < GRR._weave_count[seat_index])
				{
					_hu_weave_items[seat_index][i].weave_kind = GRR._weave_items[seat_index][i].weave_kind;
					_hu_weave_items[seat_index][i].center_card = GRR._weave_items[seat_index][i].center_card;
				}
					
			}
			_hu_weave_count[seat_index] = req.getWeaveItemCount();
			ChiHuRight chr = new ChiHuRight();
			chr = GRR._chi_hu_rights[seat_index];
			int cbChiHuKind = GameConstants.WIK_NULL;
			_is_zha_hu_card = is_hu_pai(seat_index);
			int operate_code = GameConstants.WIK_BH_ZI_MO;
			if (this._is_dispatch == 2)
				operate_code = GameConstants.WIK_BH_CHI_HU;
//			if(_is_zha_hu_card == 1)
//				this.send_error_notify(seat_index, 1, "胡牌成功 ! 赏="+_all_xi);
//			else 
//				this.send_error_notify(seat_index, 1, "咋胡 ! 赏="+_all_xi);
			this.handler_operate_card(seat_index, operate_code, this._can_hu_pai_card, -1);
			// 逻辑处理

		}
		return true;
	}

	public int is_hu_pai(int seat_index) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		if (_can_hu_pai_card == -1)
			return GameConstants.WIK_BH_NULL;
		if (_is_dispatch == -1 && seat_index != this._cur_banker)
			return GameConstants.WIK_BH_NULL;
		// 变量定义
		int kai_zhao = 0;
		int yao_peng = 0;
		int da_gun = 0;
		if (this._playerStatus[seat_index].has_action_by_bh_code(GameConstants.WIK_BH_KAIZ))
			kai_zhao = 1;
		if (this._playerStatus[seat_index].has_action_by_bh_code(GameConstants.WIK_BH_DAGUN))
			da_gun = 1;
		if (this._playerStatus[seat_index].has_action_by_bh_code(GameConstants.WIK_BH_PENG) && _logic.is_yao_card(_can_hu_pai_card) == true)
			yao_peng = 1;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.XPBH_MAX_INDEX];
		for (int i = 0; i < GameConstants.XPBH_MAX_INDEX; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
		}
		int cur_card_index = _logic.switch_to_card_index(_can_hu_pai_card);
		// 插入扑克
		if (_can_hu_pai_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[cur_card_index]++;
		}
		for (int i = 0; i < this._yao_weave_item[seat_index].weave_card_count; i++)
			cbCardIndexTemp[_logic.switch_to_card_index(this._yao_weave_item[seat_index].weave_card[i])]++;
		int special_chi = GameConstants.WIK_BH_CHI_A98 | GameConstants.WIK_BH_CHI_A99 | GameConstants.WIK_BH_CHI_AA9 | GameConstants.WIK_BH_CHI_119
				| GameConstants.WIK_BH_CHI_228 | GameConstants.WIK_BH_CHI_337 | GameConstants.WIK_BH_CHI_H;
		int eat_type = GameConstants.WIK_BH_CHI_L | GameConstants.WIK_BH_CHI_R | GameConstants.WIK_BH_CHI_C;
		int same_type = GameConstants.WIK_BH_SHE | GameConstants.WIK_BH_KAIZ|GameConstants.WIK_BH_PENG|GameConstants.WIK_BH_SHE|GameConstants.WIK_BH_ZHUA_LONG;

		for (int i = 0; i < GRR._weave_count[seat_index]; i++) {
			if (this._hu_weave_items[seat_index][i].weave_card_count == GRR._weave_items[seat_index][i].weave_card_count) {
				 _logic.get_weave_hu_xi(_hu_weave_items[seat_index], i, i + 1);
			} else {
				int weave_kind = special_chi & this._hu_weave_items[seat_index][i].weave_kind;
				if (weave_kind != 0) {
					int weave_card[] = new int[3];
					int weave_card_count = _logic.get_special_card(weave_kind, weave_card);
					int cur_card_count = 0;
					for (int j = GRR._weave_items[seat_index][i].weave_card_count; j < this._hu_weave_items[seat_index][i].weave_card_count; j++) {
						if (_logic.get_card(GRR._weave_items[seat_index][i].weave_card[j]&0xFF, weave_card, weave_card_count)) {
							int index = _logic.switch_to_card_index(GRR._weave_items[seat_index][i].weave_card[j]);
							if (cbCardIndexTemp[index] > 0) {
								cbCardIndexTemp[index]--;
								if (index == cur_card_index)
									cur_card_count++;
							} else
								return GameConstants.WIK_NULL;
						} else
							return GameConstants.WIK_NULL;
					}
					if (cur_card_count == 2) {
						int k = -1;
						for (int j = 0; j < this._hu_weave_items[seat_index][i].weave_card_count; j++) {
							if (_hu_weave_items[seat_index][i].weave_card[j] == _can_hu_pai_card) {
								k = j;
							}
							if ((k != -1) && _hu_weave_items[seat_index][i].weave_card[j] != _can_hu_pai_card) {
								_hu_weave_items[seat_index][i].weave_card[k++] = _hu_weave_items[seat_index][i].weave_card[j];
								GRR._weave_items[seat_index][i].weave_card[j] = 0;
							} else if (k != -1) {
								_hu_weave_items[seat_index][i].weave_card[j] = 0;
							}

						}
						_hu_weave_items[seat_index][i].weave_card_count -= 3;
						int target_action = GameConstants.WIK_BH_PENG;
						if (_is_dispatch == 1 && this._current_player == seat_index)
							target_action = GameConstants.WIK_BH_SHE;
						_hu_weave_items[seat_index][i].weave_kind |= target_action;
						_hu_weave_items[seat_index][i].provide_player = seat_index;
						_hu_weave_items[seat_index][i].weave_card[_hu_weave_items[seat_index][i].weave_card_count] = _can_hu_pai_card
								| GameConstants.WIK_BH_PENG;
						_hu_weave_items[seat_index][i].weave_card[_hu_weave_items[seat_index][i].weave_card_count++] = _can_hu_pai_card
								| GameConstants.WIK_BH_PENG;
						_hu_weave_items[seat_index][i].weave_card[_hu_weave_items[seat_index][i].weave_card_count++] = _can_hu_pai_card
								| GameConstants.WIK_BH_PENG;
						yao_peng = 2;
					}

				}
				weave_kind = same_type & this._hu_weave_items[seat_index][i].weave_kind;
				if (weave_kind != 0) {
					int weave_card[] = new int[10];
					int temp_special_card = special_chi & this._hu_weave_items[seat_index][i].weave_kind;
					int weave_card_count = 0;
					if(temp_special_card == 0){
						 for(int j = GRR._weave_items[seat_index][i].weave_card_count; j<this._hu_weave_items[seat_index][i].weave_card_count;j++)
							 weave_card[weave_card_count++] = _hu_weave_items[seat_index][i].weave_card[j];
						temp_special_card = _logic.get_special_kind(weave_card,weave_card_count, this._hu_weave_items[seat_index][i].weave_card[0]&0xFF);
						if(temp_special_card == -1 )
							return GameConstants.WIK_NULL;
						temp_special_card = special_chi & temp_special_card;
						_hu_weave_items[seat_index][i].weave_kind |= temp_special_card;
					}
					if(temp_special_card !=  GameConstants.WIK_BH_CHI_H){
						weave_card_count = _logic.get_special_card(temp_special_card, weave_card);
					}
					else if(temp_special_card ==  GameConstants.WIK_BH_CHI_H){
						int value = _logic.get_card_value(_hu_weave_items[seat_index][i].weave_card[0]);
						weave_card[0] =  value;
						weave_card[1] = value + 10;
						weave_card[2] = value + 20;
					}
					else {
						weave_card[0] = _hu_weave_items[seat_index][i].weave_card[0]&0xff;
					}
					int cur_card_count = 0;
					int same_card = 0;
					int other_card = 0;
					for (int j = 0; j < this._hu_weave_items[seat_index][i].weave_card_count; j++) {
						if (same_card == 0 && (this._hu_weave_items[seat_index][i].weave_card[j] & same_type) != 0)
							same_card = this._hu_weave_items[seat_index][i].weave_card[j];
						else if (same_card != 0 && other_card == 0 && (this._hu_weave_items[seat_index][i].weave_card[j] & same_type) != 0)
							other_card = this._hu_weave_items[seat_index][i].weave_card[j];
						if (j >= GRR._weave_items[seat_index][i].weave_card_count) {
							if (_logic.get_card(GRR._weave_items[seat_index][i].weave_card[j]&0xFF, weave_card, weave_card_count)==false) {
								int index = _logic.switch_to_card_index(this._hu_weave_items[seat_index][i].weave_card[j]);
								if (cbCardIndexTemp[index] > 0) {
									cbCardIndexTemp[index]--;
									if (index == cur_card_index)
										cur_card_count++;
								} else
									return GameConstants.WIK_NULL;
							} else
								return GameConstants.WIK_NULL;
						}
					}
					int target_action = GameConstants.WIK_BH_NULL;
					if (cur_card_count == 1) {
						if ((same_card & 0xff) == _can_hu_pai_card) {
							if ((same_card & same_type) == GameConstants.WIK_BH_PENG && _is_dispatch != 2) {
								target_action = same_card & same_type;
								da_gun = 2;
							} else if ((same_card & same_type) == GameConstants.WIK_BH_SHE) {
								target_action = same_card & same_type;
								kai_zhao = 2;
							}
						} else if ((other_card & 0xff) == _can_hu_pai_card) {
							if ((other_card & same_type) == GameConstants.WIK_BH_PENG && _is_dispatch != 2) {
								target_action = other_card & same_type;
								da_gun = 2;
							} else if ((other_card & same_type) == GameConstants.WIK_BH_SHE) {
								target_action = other_card & same_type;
								kai_zhao = 2;
							}

						}
						if (target_action != GameConstants.WIK_BH_NULL) {
							int k = -1;
							for (int j = 0; j < _hu_weave_items[seat_index][i].weave_card_count; j++) {
								if ((_hu_weave_items[seat_index][i].weave_card[j] & 0xFF) == _can_hu_pai_card) {
									k = j;
								}
								if ((k != -1) && (_hu_weave_items[seat_index][i].weave_card[j] & 0xFF) != _can_hu_pai_card) {
									_hu_weave_items[seat_index][i].weave_card[k++] = _hu_weave_items[seat_index][i].weave_card[j];
									GRR._weave_items[seat_index][i].weave_card[j] = 0;
								} else if (k != -1) {
									_hu_weave_items[seat_index][i].weave_card[j] = 0;
								}

							}
							_hu_weave_items[seat_index][i].weave_card_count -= 4;
							_hu_weave_items[seat_index][i].weave_kind |= target_action;
							_hu_weave_items[seat_index][i].provide_player = seat_index;
							_hu_weave_items[seat_index][i].weave_card[_hu_weave_items[seat_index][i].weave_card_count] = _can_hu_pai_card
									| target_action;
							_hu_weave_items[seat_index][i].weave_card[_hu_weave_items[seat_index][i].weave_card_count++] = _can_hu_pai_card
									| target_action;
							_hu_weave_items[seat_index][i].weave_card[_hu_weave_items[seat_index][i].weave_card_count++] = _can_hu_pai_card
									| target_action;

						}

					}
				}
				weave_kind = eat_type & this._hu_weave_items[seat_index][i].weave_kind;
				if (weave_kind != 0) {
					int max_card = 0;
					int min_card = 0xff;
					int cards_index[] = new int[GameConstants.XPBH_MAX_INDEX];
					for (int j = 0; j < this._hu_weave_items[seat_index][i].weave_card_count; j++) {
						if (this._hu_weave_items[seat_index][i].weave_card[j] > max_card)
							max_card = this._hu_weave_items[seat_index][i].weave_card[j];
						else if (this._hu_weave_items[seat_index][i].weave_card[j] < min_card)
							min_card = this._hu_weave_items[seat_index][i].weave_card[j];
						int index = _logic.switch_to_card_index(this._hu_weave_items[seat_index][i].weave_card[j]);
						if (index % 10 == 0)
							return GameConstants.WIK_BH_NULL;
						if (cards_index[index] == 0)
							cards_index[index]++;
						else
							return GameConstants.WIK_BH_NULL;
						if (j > GRR._weave_items[seat_index][i].weave_card_count) {
							if (cbCardIndexTemp[index] > 0) {
								cbCardIndexTemp[index]--;
							} else
								return GameConstants.WIK_NULL;
						} else
							return GameConstants.WIK_NULL;
					}
					for (int j = _logic.switch_to_card_index(min_card); j <= _logic.switch_to_card_index(max_card); j++) {
						if (cards_index[j] == 0)
							return GameConstants.WIK_NULL;
					}

					if (max_card - min_card + 1 != this._hu_weave_items[seat_index][i].weave_card_count)
						return GameConstants.WIK_BH_NULL;
				}

				_logic.get_weave_hu_xi(_hu_weave_items[seat_index], i, i + 1);
			}

		}
		for (int i = GRR._weave_count[seat_index]; i < _hu_weave_count[seat_index]; i++) {
			int count = 0;
			for (int j = 0; j < _hu_weave_items[seat_index][i].weave_card_count; j++) {
				int index = this._logic.switch_to_card_index(_hu_weave_items[seat_index][i].weave_card[j]);
				if (cbCardIndexTemp[index] > 0) {
					cbCardIndexTemp[index]--;
					count++;
				} else
					return GameConstants.WIK_NULL;
			}
			int action = _logic.get_card_action( _hu_weave_items[seat_index][i].weave_card, _hu_weave_items[seat_index][i].weave_card_count,this._can_hu_pai_card);
			if(action == 0)
			{
				return GameConstants.WIK_BH_NULL;
			}
			else {
				if((action&GameConstants.WIK_BH_PENG) != 0)
				{
					for(int j = 0; j < _hu_weave_items[seat_index][i].weave_card_count; j++){
						if(_hu_weave_items[seat_index][i].weave_card[j] == this._can_hu_pai_card)
							_hu_weave_items[seat_index][i].weave_card[j] |= action&GameConstants.WIK_BH_PENG;
					}
					if(_logic.is_yao_card(_can_hu_pai_card))
						yao_peng++;
				}
				_hu_weave_items[seat_index][i].weave_kind = action;
				if( action == GameConstants.WIK_BH_LUO_YAO){
					_hu_weave_items[seat_index][i].hu_xi = _hu_weave_items[seat_index][i].weave_card_count;
				}
				else
					_logic.get_weave_hu_xi(_hu_weave_items[seat_index], i, i + 1);
			}

		}
		int all_shang = 0;
		for(int i = 0; i<_hu_weave_count[seat_index];i++)
		{
			all_shang += _hu_weave_items[seat_index][i].hu_xi;
		}
		
			
		if (kai_zhao == 1)
			return GameConstants.WIK_NULL;
		if (da_gun == 1)
			return GameConstants.WIK_NULL;
		if (yao_peng == 1)
			return GameConstants.WIK_NULL;
		this._all_xi = all_shang;
		if(all_shang < 20)
			return GameConstants.WIK_NULL;
		return 1;
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
