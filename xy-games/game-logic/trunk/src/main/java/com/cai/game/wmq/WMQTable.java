/**
 * 
 */
package com.cai.game.wmq;

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
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
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
import com.cai.game.wmq.data.AnalyseItem;
import com.cai.game.wmq.data.LouWeaveItem;
import com.cai.game.wmq.handler.WMQHandler;
import com.cai.game.wmq.handler.WMQHandlerChiPeng;
import com.cai.game.wmq.handler.WMQHandlerDispatchCard;
import com.cai.game.wmq.handler.WMQHandlerFinish;
import com.cai.game.wmq.handler.WMQHandlerGang;
import com.cai.game.wmq.handler.WMQHandlerOutCardOperate;
import com.cai.game.wmq.handler.axwmq.WMQHandlerChiPeng_AX;
import com.cai.game.wmq.handler.axwmq.WMQHandlerChuLiFirstCard_AX;
import com.cai.game.wmq.handler.axwmq.WMQHandlerDispatchCard_AX;
import com.cai.game.wmq.handler.axwmq.WMQHandlerDispatchFirstCard_AX;
import com.cai.game.wmq.handler.axwmq.WMQHandlerGang_AX;
import com.cai.game.wmq.handler.axwmq.WMQHandlerOutCardOperate_AX;
import com.cai.game.wmq.handler.axwmqs.WMQHandlerChiPeng_AX_S;
import com.cai.game.wmq.handler.axwmqs.WMQHandlerChuLiFirstCard_AX_S;
import com.cai.game.wmq.handler.axwmqs.WMQHandlerDispatchCard_AX_S;
import com.cai.game.wmq.handler.axwmqs.WMQHandlerDispatchFirstCard_AX_S;
import com.cai.game.wmq.handler.axwmqs.WMQHandlerGang_AX_S;
import com.cai.game.wmq.handler.axwmqs.WMQHandlerOutCardOperate_AX_S;
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
public class WMQTable extends AbstractRoom {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7060061356475703643L;

	private static Logger logger = Logger.getLogger(WMQTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

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
	public int _cannot_peng[][];// 不可以碰的牌
	public int _guo_hu_xt[]; // 同一圈用户过胡
	public int _guo_hu_pai_cards[][]; // 过胡牌
	public int _guo_hu_pai_count[]; // 过胡牌数量
	// 胡息
	// public HuCardInfo _hu_card_info;

	public WMQGameLogic _logic = null;
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
	public int _game_mid_score[];
	public int _game_weave_score[];
	public int _game_other_mid_score[];
	public int _zhe_zhe_count[];
	public int _provider_hu;
	public int _hu_pai_action[];
	public int _hu_special_type[][];
	public int _hu_special_count[];
	/**
	 * 牌桌上的组合扑克 (落地的牌组合)
	 */
	public WeaveItem _hu_weave_items[][];

	/**
	 * 当前桌子内玩家数量
	 */
	private int playerNumber;

	private long _request_release_time;
	private ScheduledFuture _release_scheduled;
	private ScheduledFuture _table_scheduled;

	public WMQHandler _handler;

	public WMQHandlerDispatchCard _handler_dispath_card;
	public WMQHandlerOutCardOperate _handler_out_card_operate;
	public WMQHandlerGang _handler_gang;
	public WMQHandlerChiPeng _handler_chi_peng;
	public WMQHandlerDispatchCard _handler_dispath_firstcards;
	public WMQHandlerDispatchCard _handler_chuli_firstcards;

	// public HHHandlerDispatchLastCard_YX _handler_dispath_last_card;

	public WMQHandlerFinish _handler_finish; // 结束

	public WMQTable() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new WMQGameLogic();

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
		_cannot_chi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_cannot_peng = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
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
		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_provider_hu = GameConstants.INVALID_SEAT;
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_game_mid_score = new int[this.getTablePlayerNumber()];
		_game_other_mid_score = new int[this.getTablePlayerNumber()];
		_game_weave_score = new int[this.getTablePlayerNumber()];
		_zhe_zhe_count = new int[this.getTablePlayerNumber()];
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
			_game_mid_score[i] = 0;
			_game_weave_score[i] = 0;
			_game_other_mid_score[i] = 0;
		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++)
				_guo_hu_pai_cards[i][j] = 0;
		}
		// 胡牌信息
		_hu_pai_action = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_pai_action, 0);
		_huang_zhang_count = 0;
		// _hu_card_info = new HuCardInfo();
		_lou_weave_item = new LouWeaveItem[this.getTablePlayerNumber()][7];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		_cannot_chi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_cannot_peng = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];

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
		_hu_pai_action = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_pai_action, 0);
		_hu_special_type = new int[this.getTablePlayerNumber()][GameConstants.CHR_WMQ_SPECIAL_MAX_COUNT];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_hu_special_type[i] = new int[GameConstants.CHR_WMQ_SPECIAL_MAX_COUNT];
			Arrays.fill(_hu_special_type[i], 0);
		}
		_hu_special_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_special_count, 0);

		_zong_liu_zi_fen = 0;
		_all_xi = 0;
		_all_lz_fen = 0;
		_provider_hu = GameConstants.INVALID_SEAT;
		_hu_xi = new int[this.getTablePlayerNumber()];
		_hu_pai_max_hu = new int[this.getTablePlayerNumber()];
		_xt_display_an_long = new boolean[this.getTablePlayerNumber()];
		_game_mid_score = new int[this.getTablePlayerNumber()];
		_game_other_mid_score = new int[this.getTablePlayerNumber()];
		_game_weave_score = new int[this.getTablePlayerNumber()];
		_zhe_zhe_count = new int[this.getTablePlayerNumber()];
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
			_game_mid_score[i] = 0;
			_game_weave_score[i] = 0;
			_game_other_mid_score[i] = 0;
		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++)
				_guo_hu_pai_cards[i][j] = 0;
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
		}

		if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
			_handler_dispath_card = new WMQHandlerDispatchCard_AX();
			_handler_out_card_operate = new WMQHandlerOutCardOperate_AX();
			_handler_gang = new WMQHandlerGang_AX();
			_handler_chi_peng = new WMQHandlerChiPeng_AX();

			_handler_chuli_firstcards = new WMQHandlerChuLiFirstCard_AX();
			_handler_dispath_firstcards = new WMQHandlerDispatchFirstCard_AX();
		}
		if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX_S)) {
			_handler_dispath_card = new WMQHandlerDispatchCard_AX_S();
			_handler_out_card_operate = new WMQHandlerOutCardOperate_AX_S();
			_handler_gang = new WMQHandlerGang_AX_S();
			_handler_chi_peng = new WMQHandlerChiPeng_AX_S();

			_handler_chuli_firstcards = new WMQHandlerChuLiFirstCard_AX_S();
			_handler_dispath_firstcards = new WMQHandlerDispatchFirstCard_AX_S();
		}

		_handler_finish = new WMQHandlerFinish();

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
		_cannot_chi = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
		_cannot_peng = new int[this.getTablePlayerNumber()][GameConstants.MAX_HH_INDEX];
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
		_game_mid_score = new int[this.getTablePlayerNumber()];
		_game_other_mid_score = new int[this.getTablePlayerNumber()];
		_game_weave_score = new int[this.getTablePlayerNumber()];
		_zhe_zhe_count = new int[this.getTablePlayerNumber()];

		_all_xi = 0;
		_all_lz_fen = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
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
			_game_mid_score[i] = 0;
			_game_weave_score[i] = 0;
			_game_other_mid_score[i] = 0;
		}
		_guo_hu_pai_cards = new int[this.getTablePlayerNumber()][15];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 15; j++)
				_guo_hu_pai_cards[i][j] = 0;
		}
		_provider_hu = GameConstants.INVALID_SEAT;

		_last_card = 0;
		_last_player = -1;// 上次发牌玩家
		_long_count = new int[this.getTablePlayerNumber()]; // 每个用户有几条龙
		_ti_two_long = new boolean[this.getTablePlayerNumber()];
		_is_xiang_gong = new boolean[this.getTablePlayerNumber()];
		_hu_pai_action = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_pai_action, 0);
		_hu_special_type = new int[this.getTablePlayerNumber()][GameConstants.CHR_WMQ_SPECIAL_MAX_COUNT];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_hu_special_type[i] = new int[GameConstants.CHR_WMQ_SPECIAL_MAX_COUNT];
			Arrays.fill(_hu_special_type[i], 0);
		}
		_hu_special_count = new int[this.getTablePlayerNumber()];
		Arrays.fill(_hu_special_count, 0);
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
		this.log_error("gme_status:" + this._game_status);

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		_cur_banker = GRR._banker_player;
		_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return game_start_PHZ_YX();

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
		gameStartResponse.setXiaoHuTag(this._zong_liu_zi_fen);

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
			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX_S)) {
				sysParamModel1104 = SysParamServerDict.getInstance()
						.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(1104);
			}
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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_hh_ting_card_twenty(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i], i, i);

			int ting_cards[] = this._playerStatus[i]._hu_cards;
			int ting_count = this._playerStatus[i]._hu_card_count;

			if (ting_count > 0) {
				this.operate_chi_hu_cards(i, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				this.operate_chi_hu_cards(i, 1, ting_cards);
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

		int send_count;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {

			send_count = GameConstants.MAX_WMQ_COUNT - 1;

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
		// int cards[] = new int[]
		// {0x07,0x07,0x07,0x04,0x02,0x02,0x02,0x04,0x12,0x13,0x11,0x12,0x13,0x19,0x19,0x19,0x18,0x18,0x18,0x11};
		// int cards1[] = new int[]
		// {0x08,0x06,0x04,0x04,0x02,0x02,0x02,0x04,0x12,0x13,0x11,0x12,0x13,0x19,0x19,0x19,0x18,0x18,0x18,0x11};
		// int cards2[] = new int[]
		// {0x08,0x06,0x04,0x04,0x02,0x02,0x02,0x04,0x12,0x13,0x11,0x12,0x13,0x19,0x19,0x19,0x18,0x18,0x18,0x11};
		//////// int cards3[] = new int[]
		// {0x01,0x01,0x01,0x01,0x02,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04};
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
		// for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
		// GRR._cards_index[_current_player%this.getTablePlayerNumber()][j] =
		// 0;
		// GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][j]
		// = 0;
		// GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][j]
		// = 0;
		// GRR._cards_index[(_current_player+3)%this.getTablePlayerNumber()][j]
		// = 0;
		// }
		// }
		//
		// for(int j = 0; j< cards.length;j++)
		// {
		// GRR._cards_index[_current_player%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards[j])]
		// += 1;
		// }
		// for(int j = 0; j<cards1.length;j++)
		// {
		// GRR._cards_index[(_current_player+1)%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards1[j])]
		// += 1;
		// }
		// for(int j = 0; j<cards2.length;j++)
		// {
		// GRR._cards_index[(_current_player+2)%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards2[j])]
		// += 1;
		// }
		// for(int j = 0; j<cards3.length;j++)
		// {
		// GRR._cards_index[(_current_player+3)%this.getTablePlayerNumber()][_logic.switch_to_card_index(cards3[j])]
		// += 1;
		// }

		// /*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可
		// ***********************/
		// int[] realyCards = new int[] {
		// 2, 24, 4, 9, 21, 4, 4, 6, 21, 1, 3, 22, 25, 20, 5, 26, 23, 23, 3, 8,
		// 7, 6, 10, 25, 1,
		// 26, 18, 7, 17, 5, 23, 17, 22, 18, 3, 5, 2, 18, 22, 24, 21, 17, 26,
		// 25, 20, 10, 8, 8, 9,
		// 21, 4, 6, 2, 8, 20, 10, 1, 20, 19, 19, 19, 2, 5, 23, 10, 22, 25, 6,
		// 9, 24, 19, 7, 9, 18,
		// 17, 26, 24, 7, 1, 3
		// };
		// this._cur_banker = 0;
		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
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
		// GRR._banker_player = 2;
		// _current_player = GRR._banker_player;
		int send_count;
		int have_send_count = 0;
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			send_count = GameConstants.MAX_WMQ_COUNT - 1;

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
			send_count = GameConstants.MAX_WMQ_COUNT - 1;
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
		if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
			ret = this.handler_game_finish_wmq(seat_index, reason);
		else if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX_S))
			ret = this.handler_game_finish_wmq_s(seat_index, reason);

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		return ret;
	}

	public boolean handler_game_finish_wmq(int seat_index, int reason) {
		int real_reason = reason;
		// _game_round = 2;
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		if (this._cur_round == this._game_round || (!(reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)))
			operate_dou_liu_zi(seat_index, true, 0, true);
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
			if (seat_index != -1)
				this.set_result_describe(seat_index);
			if (seat_index != -1) {
				game_end.setTunShu(this._tun_shu[seat_index]);
				game_end.setFanShu(this._fan_shu[seat_index]);
				game_end.setHuXi(this._hu_xi[seat_index]);
			}
			game_end.setZongXi(this._all_xi);
			game_end.setWinLziFen(this._all_lz_fen);
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

	public boolean handler_game_finish_wmq_s(int seat_index, int reason) {
		int real_reason = reason;
		// _game_round = 2;
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		if (this._cur_round == this._game_round || (!(reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)))
			operate_dou_liu_zi(seat_index, true, 0, true);
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
		if (seat_index != -1)
			game_end.setCellScore(this._hu_pai_action[seat_index]);

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			if (seat_index == this._provider_hu)
				game_end.setCountPickNiao(1); // 1自摸,2平胡
			else if (this._provider_hu != GameConstants.INVALID_SEAT)
				game_end.setCountPickNiao(2);
			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());

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
			if (seat_index != -1)
				this.set_result_describe(seat_index);
			if (seat_index != -1) {
				game_end.setTunShu(this._tun_shu[seat_index]);
				game_end.setFanShu(this._fan_shu[seat_index]);
				game_end.setHuXi(this._hu_xi[seat_index]);
			}
			game_end.setZongXi(this._all_xi);
			game_end.setWinLziFen(this._all_lz_fen);
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

	@Override
	protected boolean checkHuanDou(int end_type) {
		return end_type != GameConstants.Game_End_DRAW;
	}

	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int hu_xi[], boolean dispatch) {
		if (this._ti_two_long[seat_index] == true) {
			return 0;
		}

		if (GameConstants.GAME_TYPE_WMQ_AX == _game_type_index) {
			return analyse_chi_hu_card_wmq(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);

		}
		if (GameConstants.GAME_TYPE_WMQ_AX_S == _game_type_index) {
			return analyse_chi_hu_card_wmq_s(cards_index, weaveItems, weave_count, seat_index, provider_index, cur_card, chiHuRight, card_type,
					dispatch);
		}
		return 0;
	}

	public int get_max_hu(AnalyseItem analyseItem, int max_hu_xi, int card_count, int cur_card, int seat_index, int provider_index,
			WeaveItem weaveItems[], int weaveCount, int card_type, int cbMingIndexTemp[]) {
		ChiHuRight chiHuRight = new ChiHuRight();
		this._chun_ying_count[seat_index] = 0;
		this._ying_hu_count[seat_index] = 0;
		int ying_xi = max_hu_xi;
		int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
		int all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		int hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		int dui_zi_count = _logic.calculate_dui_zi_hu_count(analyseItem);
		boolean is_hua_man_yuan = _logic.is_hua_man_yuan(analyseItem);
		this._da_pai_count[seat_index] = _logic.get_analyse_da_card(analyseItem);
		this._xiao_pai_count[seat_index] = _logic.get_analyse_xiao_card(analyseItem);
		int tuan_yuan_index[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, true);
		this._ying_hu_count[seat_index] = _logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, false);
		if (this._ying_hu_count[seat_index] * 4 == this._hong_pai_count[seat_index]) {
			this._chun_ying_count[seat_index] = this._ying_hu_count[seat_index];

		}

		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {

			if (this._hong_pai_count[seat_index] >= 10)
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
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
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
			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (ying_xi == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (ying_xi == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card && dui_zi_count == 7) {
				chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
			} else if (analyseItem.cbCardEye == cur_card)
				chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			if (_logic.is_kan_bian(cur_card, analyseItem, weaveCount) == true && analyseItem.cbCardEye != cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_KAN_HU);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0 && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount - 1]) == 8)) {
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
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);
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
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}
			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}

			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}

			if (ying_xi == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (cur_card == analyseItem.cbCardEye)
				chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
			}

		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);
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
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}
			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (ying_xi == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (ying_xi == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card && dui_zi_count == 7) {
				chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
			} else if (analyseItem.cbCardEye == cur_card)
				chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
			}

		}
		int wFanShu = 0;
		int hu_xi = 0;
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_lmt_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_lmt_wmq(seat_index, chiHuRight);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_qmt_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_qmt_wmq(seat_index, chiHuRight);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_xzb_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_xzb_wmq(seat_index, chiHuRight);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_dzb_wmq(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_dzb_wmq(seat_index, chiHuRight);// 番数

		}
		int deng_fen = 0;
		int liu_zi_fen = 0;
		if (max_hu_xi - 17 > 0) {
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_ONE_FEN)) {
				deng_fen = 200;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_TWO_FEN)) {
				deng_fen = 300;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_THREE_FEN)) {
				deng_fen = 450;
			}
			if (this._zong_liu_zi_fen > (deng_fen * (max_hu_xi - 17))) {
				liu_zi_fen = deng_fen * (max_hu_xi - 17);

			} else {
				liu_zi_fen = this._zong_liu_zi_fen;

			}
		}
		int temp_hu_xi = hu_xi + max_hu_xi * wFanShu + liu_zi_fen;
		return temp_hu_xi;
	}

	public int get_max_hu_s(AnalyseItem analyseItem, int max_hu_xi, int card_count, int cur_card, int seat_index, int provider_index,
			WeaveItem weaveItems[], int weaveCount, int card_type, int cbMingIndexTemp[]) {
		ChiHuRight chiHuRight = new ChiHuRight();
		this._chun_ying_count[seat_index] = 0;
		this._ying_hu_count[seat_index] = 0;
		int ying_xi = max_hu_xi;
		int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
		int all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		int hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		int dui_zi_count = _logic.calculate_dui_zi_hu_count(analyseItem);
		boolean is_hua_man_yuan = _logic.is_hua_man_yuan(analyseItem);
		this._da_pai_count[seat_index] = _logic.get_analyse_da_card(analyseItem);
		this._xiao_pai_count[seat_index] = _logic.get_analyse_xiao_card(analyseItem);
		int tuan_yuan_index[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, true);
		this._ying_hu_count[seat_index] = _logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, false);
		if (this._ying_hu_count[seat_index] * 4 == this._hong_pai_count[seat_index]) {
			this._chun_ying_count[seat_index] = this._ying_hu_count[seat_index];

		}

		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true && ying_xi == 2)
				chiHuRight.opr_or_long(GameConstants.CHR_2_MAN_YUAN_HUA);
			else if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_logic.is_kan_bian(cur_card, analyseItem, weaveCount) == true && analyseItem.cbCardEye != cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_KAN_HU);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0 && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount - 1]) == 8)) {
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
			if (this._zhe_zhe_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_ZHE_ZHE_HU);

			if (_logic.is_dui_dao_hu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_TUI_DAO_HU);
			}
			if (_logic.is_huo_zhuo_xiao_san(cur_card, analyseItem, weaveCount) == true && (seat_index == provider_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_HUO_ZHOU_3);
			}
			if (ying_xi == 10 && _logic.is_two_hong_two_hei_peng(analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_2_HONG_2_HEI);
			}
			if (_logic.is_xing_lian_xing(analyseItem) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_XING_LIAN_XING);
			}
			if (_logic.is_yuan_yuan_ding(analyseItem, cur_card) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_YUAN_YUAN_DI);
			}
			if (_logic.is_bian_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_DING);
			}
			if (card_count == 1 && _logic.color_hei(cur_card) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_MM_CAI_DAN_CHE);
			}
			if (_logic.is_two_long_xi_zhu(analyseItem) == true && this._hong_pai_count[seat_index] == 2) {
				chiHuRight.opr_or_long(GameConstants.CHR_2_LONG_XI_ZHU);
			}
			if (_logic.is_ge_shan_da_niu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_GE_SHAN_DA_NIU);
			}
			if (_logic.is_yi_tiao_long(analyseItem) == true && analyseItem.cbCardEye == cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
			}

		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_ZI_MO_WMQ) == 1 && seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true && ying_xi == 2)
				chiHuRight.opr_or_long(GameConstants.CHR_2_MAN_YUAN_HUA);
			else if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_logic.is_kan_bian(cur_card, analyseItem, weaveCount) == true && analyseItem.cbCardEye != cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_KAN_HU);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0 && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount - 1]) == 8)) {
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
			if (this._zhe_zhe_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_ZHE_ZHE_HU);

			if (this.getRuleValue(GameConstants.GAME_RULE_DUI_DAO_HU) == 1 && _logic.is_dui_dao_hu(_playerStatus[seat_index]._hu_cards,
					_playerStatus[seat_index]._hu_card_count, cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_TUI_DAO_HU);
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_HUO_ZHOU_XIAO_SAN) == 1
					&& _logic.is_huo_zhuo_xiao_san(cur_card, analyseItem, weaveCount) == true && (seat_index == provider_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_HUO_ZHOU_3);
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_LIANG_HONG_LIANG_HEI) == 1 && ying_xi == 10
					&& _logic.is_two_hong_two_hei_peng(analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_2_HONG_2_HEI);
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_XING_LIAN_XING) == 1 && _logic.is_xing_lian_xing(analyseItem) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_XING_LIAN_XING);
			}
			if (_logic.is_yuan_yuan_ding(analyseItem, cur_card) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_YUAN_YUAN_DI);
			}
			if (_logic.is_bian_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_DING);
			}
			if (_logic.is_ge_shan_da_niu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true && this.getRuleValue(GameConstants.GAME_RULE_GE_SAN_DA_NIU) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_GE_SHAN_DA_NIU);
			}
			if (_logic.is_yi_tiao_long(analyseItem) == true && analyseItem.cbCardEye == cur_card
					&& this.getRuleValue(GameConstants.GAME_RULE_YI_TIAO_LONG) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
			}
			if (this.getRuleValue(GameConstants.GMAE_RULE_GA_NUAN_DA) == 1 && _logic.is_ga_nuan_da(cur_card, analyseItem, weaveCount)) {
				this._hu_special_type[seat_index][GameConstants.CHR_WMQ_SPECIAL_GA_NUAN_DA] = 1;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1 && this.getRuleValue(GameConstants.GAME_RULE_QUAN_QIU_REN) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index) && this.getRuleValue(GameConstants.GAME_RULE_DA_LONG_BAI_WEI) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)
					&& this.getRuleValue(GameConstants.GAME_RULE_SHANG_XIA_WU_QNIAN) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				// if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0
				// && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount
				// - 1]) == 8)) {
				// chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
				// } else
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
			}

		}

		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true && ying_xi == 2)
				chiHuRight.opr_or_long(GameConstants.CHR_2_MAN_YUAN_HUA);
			else if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				// if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0
				// && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount
				// - 1]) == 8)) {
				// chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
				// } else
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
			if (this._zhe_zhe_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_ZHE_ZHE_HU);

			if (_logic.is_dui_dao_hu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_TUI_DAO_HU);
			}
			if (_logic.is_yuan_yuan_ding(analyseItem, cur_card) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_YUAN_YUAN_DI);
			}
			if (_logic.is_bian_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_DING);
			}

		}
		int wFanShu = 0;
		int hu_xi = 0;
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_lmt_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index],
					this._zhe_zhe_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_lmt_wmq_s(seat_index, chiHuRight);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_qmt_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index],
					this._zhe_zhe_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_qmt_wmq_s(seat_index, chiHuRight);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_xzb_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_xzb_wmq_s(seat_index, chiHuRight);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_dzb_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index],
					this._zhe_zhe_count[seat_index], chiHuRight);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_dzb_wmq_s(seat_index, chiHuRight);// 番数

		}
		int deng_fen = 0;
		int liu_zi_fen = 0;
		if (max_hu_xi - 17 > 0) {
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_ONE_FEN)) {
				deng_fen = 200;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_TWO_FEN)) {
				deng_fen = 300;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_THREE_FEN)) {
				deng_fen = 450;
			}
			if (this._zong_liu_zi_fen > (deng_fen * (max_hu_xi - 17))) {
				liu_zi_fen = deng_fen * (max_hu_xi - 17);

			} else {
				liu_zi_fen = this._zong_liu_zi_fen;

			}
		}
		int temp_hu_xi = hu_xi + max_hu_xi * wFanShu + liu_zi_fen;
		return temp_hu_xi;
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
		boolean yws_type = false;
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
		boolean hu_pai = false;
		int ying_xi = 0;
		_hu_pai_max_hu[seat_index] = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			// 构造扑克
			int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
			_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
			temp_hu_xi = _logic.get_cal_all_hu_xi_awq(analyseItem, cbAnalyseIndexTemp, cbMingIndexTemp);
			if (temp_hu_xi < 10)
				continue;
			else
				hu_pai = true;
			ChiHuRight chr = new ChiHuRight();
			ying_xi = temp_hu_xi;
			temp_hu_xi = get_max_hu(analyseItem, temp_hu_xi, card_count, cur_card, seat_index, provider_index, weaveItems, weaveCount, card_type,
					cbMingIndexTemp);
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				this._hu_pai_max_hu[seat_index] = ying_xi;
				max_hu_xi = temp_hu_xi;
			}

		}
		if (hu_pai == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

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
		this._chun_ying_count[seat_index] = 0;
		this._ying_hu_count[seat_index] = 0;
		analyseItem = analyseItemArray.get(max_hu_index);
		int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
		int all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		int hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		int dui_zi_count = _logic.calculate_dui_zi_hu_count(analyseItem);
		boolean is_hua_man_yuan = _logic.is_hua_man_yuan(analyseItem);
		this._da_pai_count[seat_index] = _logic.get_analyse_da_card(analyseItem);
		this._xiao_pai_count[seat_index] = _logic.get_analyse_xiao_card(analyseItem);
		int tuan_yuan_index[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, true);
		this._ying_hu_count[seat_index] = _logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, false);
		if (this._ying_hu_count[seat_index] * 4 == this._hong_pai_count[seat_index] && this._hong_pai_count[seat_index] != 0) {
			this._chun_ying_count[seat_index] = this._ying_hu_count[seat_index];

		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {

			if (this._hong_pai_count[seat_index] >= 10)
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
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
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
			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_logic.is_kan_bian(cur_card, analyseItem, weaveCount) == true && analyseItem.cbCardEye != cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_KAN_HU);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0 && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount - 1]) == 8)) {
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
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}
			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}

			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (cur_card == analyseItem.cbCardEye)
				chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
			}

		}

		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);
			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);
			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}

			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}
			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}
			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
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
	public int analyse_chi_hu_card_wmq_s(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
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
		boolean yws_type = false;
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
		boolean hu_pai = false;
		int ying_xi = 0;
		_hu_pai_max_hu[seat_index] = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			// 构造扑克
			int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
			_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
			temp_hu_xi = _logic.get_cal_all_hu_xi_awq(analyseItem, cbAnalyseIndexTemp, cbMingIndexTemp);
			if (temp_hu_xi < 10) {
				if (!(_logic.is_hua_man_yuan(analyseItem) && temp_hu_xi == 2 && has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN) == false))
					continue;
			} else
				hu_pai = true;
			ChiHuRight chr = new ChiHuRight();
			ying_xi = temp_hu_xi;
			Arrays.fill(this._hu_special_type[seat_index], 0);
			temp_hu_xi = get_max_hu_s(analyseItem, temp_hu_xi, card_count, cur_card, seat_index, provider_index, weaveItems, weaveCount, card_type,
					cbMingIndexTemp);
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				this._hu_pai_max_hu[seat_index] = ying_xi;
				max_hu_xi = temp_hu_xi;
				hu_pai = true;
			}

		}
		if (hu_pai == false) {
			chiHuRight.set_empty();
			Arrays.fill(this._hu_special_type[seat_index], 0);
			return GameConstants.WIK_NULL;
		}

		analyseItem = analyseItemArray.get(max_hu_index);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi_wmq(_hu_weave_items[seat_index][j]);
			_hu_weave_items[seat_index][j].public_card = analyseItem.cbPublicCard[j];
			_da_pai_count[seat_index] += _logic.get_da_card(_hu_weave_items[seat_index][j].weave_kind, _hu_weave_items[seat_index][j].center_card);
			_xiao_pai_count[seat_index] += _logic.get_xiao_card(_hu_weave_items[seat_index][j].weave_kind,
					_hu_weave_items[seat_index][j].center_card);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].public_card = 100;
			_hu_weave_count[seat_index]++;

		}
		this._chun_ying_count[seat_index] = 0;
		this._ying_hu_count[seat_index] = 0;
		this._hu_pai_action[seat_index] = 0;
		analyseItem = analyseItemArray.get(max_hu_index);
		int cbAnalyseIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.analyse_item_to_card(analyseItem, cbAnalyseIndexTemp);
		int all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		this._hong_pai_count[seat_index] = _logic.calculate_hong_pai_count(analyseItem);
		int hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		int dui_zi_count = _logic.calculate_dui_zi_hu_count(analyseItem);
		boolean is_hua_man_yuan = _logic.is_hua_man_yuan(analyseItem);
		this._da_pai_count[seat_index] = _logic.get_analyse_da_card(analyseItem);
		this._xiao_pai_count[seat_index] = _logic.get_analyse_xiao_card(analyseItem);
		int tuan_yuan_index[] = new int[GameConstants.MAX_HH_INDEX];
		_logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, true);
		this._ying_hu_count[seat_index] = _logic.get_analyse_tuan_yuan(analyseItem, cbAnalyseIndexTemp, tuan_yuan_index, false);
		if (this._ying_hu_count[seat_index] * 4 == this._hong_pai_count[seat_index] && this._hong_pai_count[seat_index] != 0) {
			this._chun_ying_count[seat_index] = this._ying_hu_count[seat_index];
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		Arrays.fill(this._hu_special_type[seat_index], 0);

		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true && ying_xi == 2)
				chiHuRight.opr_or_long(GameConstants.CHR_2_MAN_YUAN_HUA);
			else if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_LEFT;
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_DUI_ZI;
			}
			if (_logic.is_kan_bian(cur_card, analyseItem, weaveCount) == true && analyseItem.cbCardEye != cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_KAN_HU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_LEFT;
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0 && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount - 1]) == 8)) {
					chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_feng_bai_wei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_FBW_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;

			}
			if (this._zhe_zhe_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_ZHE_ZHE_HU);

			if (_logic.is_dui_dao_hu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_TUI_DAO_HU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_huo_zhuo_xiao_san(cur_card, analyseItem, weaveCount) == true && (seat_index == provider_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_HUO_ZHOU_3);
			}
			if (ying_xi == 10 && _logic.is_two_hong_two_hei_peng(analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_2_HONG_2_HEI);
			}
			if (_logic.is_xing_lian_xing(analyseItem) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_XING_LIAN_XING);
			}
			if (_logic.is_yuan_yuan_ding(analyseItem, cur_card) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_YUAN_YUAN_DI);
			}
			if (_logic.is_bian_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_DING);
			}
			if (card_count == 1 && _logic.color_hei(cur_card) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_MM_CAI_DAN_CHE);
			}
			if (_logic.is_two_long_xi_zhu(analyseItem) == true && this._hong_pai_count[seat_index] == 2) {
				chiHuRight.opr_or_long(GameConstants.CHR_2_LONG_XI_ZHU);
			}
			if (_logic.is_ge_shan_da_niu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_GE_SHAN_DA_NIU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (analyseItem.cbCardEye == cur_card && _logic.is_yi_tiao_long(analyseItem) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
			}
		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index && this.getRuleValue(GameConstants.GAME_RULE_ZI_MO_WMQ) == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true && ying_xi == 2)
				chiHuRight.opr_or_long(GameConstants.CHR_2_MAN_YUAN_HUA);
			else if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_LEFT;
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}
			if (_logic.is_kan_bian(cur_card, analyseItem, weaveCount) == true && analyseItem.cbCardEye != cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_KAN_HU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_LEFT;
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0 && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount - 1]) == 8)) {
					chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_feng_bai_wei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_FBW_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;

			}
			if (this._zhe_zhe_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_ZHE_ZHE_HU);

			if (_logic.is_dui_dao_hu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true && this.getRuleValue(GameConstants.GAME_RULE_DUI_DAO_HU) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_TUI_DAO_HU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_huo_zhuo_xiao_san(cur_card, analyseItem, weaveCount) == true && (seat_index == provider_index)
					&& this.getRuleValue(GameConstants.GAME_RULE_HUO_ZHOU_XIAO_SAN) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_HUO_ZHOU_3);
			}
			if (ying_xi == 10 && _logic.is_two_hong_two_hei_peng(analyseItem, weaveCount) == true
					&& this.getRuleValue(GameConstants.GAME_RULE_LIANG_HONG_LIANG_HEI) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_2_HONG_2_HEI);
			}
			if (_logic.is_xing_lian_xing(analyseItem) == true && this.getRuleValue(GameConstants.GAME_RULE_XING_LIAN_XING) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_XING_LIAN_XING);
			}
			if (_logic.is_yuan_yuan_ding(analyseItem, cur_card) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_YUAN_YUAN_DI);
			}
			if (_logic.is_bian_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_DING);
			}
			if (_logic.is_ge_shan_da_niu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true && this.getRuleValue(GameConstants.GAME_RULE_GE_SAN_DA_NIU) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_GE_SHAN_DA_NIU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (this.getRuleValue(GameConstants.GAME_RULE_YI_TIAO_LONG) == 1 && _logic.is_yi_tiao_long(analyseItem) == true
					&& analyseItem.cbCardEye == cur_card) {
				chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
			}
			if (this.getRuleValue(GameConstants.GMAE_RULE_GA_NUAN_DA) == 1 && _logic.is_ga_nuan_da(cur_card, analyseItem, weaveCount)) {
				this._hu_special_type[seat_index][GameConstants.CHR_WMQ_SPECIAL_GA_NUAN_DA] = 1;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);

			if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1 && this.getRuleValue(GameConstants.GAME_RULE_QUAN_QIU_REN) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index) && this.getRuleValue(GameConstants.GAME_RULE_SHANG_XIA_WU_QNIAN) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)
					&& this.getRuleValue(GameConstants.GAME_RULE_DA_LONG_BAI_WEI) == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_LEFT;
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				// if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0
				// && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount
				// - 1]) == 8)) {
				// chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
				// } else
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}

		}

		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {

			if (this.GRR._left_card_count == 22 && seat_index == provider_index) {
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or_long(GameConstants.CHR_TIAN_HU_WMQ);
				}
			}
			if (seat_index == provider_index)
				chiHuRight.opr_or_long(GameConstants.CHR_ZI_MO_WMQ);
			if (is_hua_man_yuan == true && ying_xi == 2)
				chiHuRight.opr_or_long(GameConstants.CHR_2_MAN_YUAN_HUA);
			else if (is_hua_man_yuan == true)
				chiHuRight.opr_or_long(GameConstants.CHR_MAN_YUAN_HUA_WMQ);

			if (this._hong_pai_count[seat_index] >= 10)
				chiHuRight.opr_or_long(GameConstants.CHR_HONG_HU_WMQ);
			if (this._hong_pai_count[seat_index] > 10)
				chiHuRight.opr_or_long(GameConstants.CHR_DUO_HONG_WMQ);
			if (this._chun_ying_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_CHUN_YING_WMQ);
			if (this._ying_hu_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_YING_HU_WMQ);

			if (dui_zi_count == 7) {
				if (this._hong_pai_count[seat_index] == 0)
					chiHuRight.opr_or_long(GameConstants.CHR_WU_DUI_WMQ);
				else
					chiHuRight.opr_or_long(GameConstants.CHR_DUI_ZI_HU_WMQ);
			}
			if (this._hong_pai_count[seat_index] == 1)
				chiHuRight.opr_or_long(GameConstants.CHR_DIAN_HU_WMQ);
			if (dui_zi_count != 7 && this._hong_pai_count[seat_index] == 0)
				chiHuRight.opr_or_long(GameConstants.CHR_WU_HU_WMQ);
			if (this._da_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_DA_ZI_HU_WMQ);
			if (this._xiao_pai_count[seat_index] == all_cards_count)
				chiHuRight.opr_or_long(GameConstants.CHR_XIAO_ZI_HU_WMQ);

			if (_logic.is_zu_sun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_ZU_SUN_ZHUO);
			} else if (_logic.is_two_jie_mei_zhuo(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_TWO_JIE_MEI_ZHUO);
			} else if (_logic.is_die_shun_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DS_DIA_TUO_WMQ);
			} else if (_logic.is_si_luan_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_SI_LUAN_ZHUO_WMQ);
			} else if (_logic.is_die_shun_zhuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_DIA_SHUN_ZHUO);
			} else if (_logic.is_jie_mei_zhuo_dai_tuo_hu(tuan_yuan_index)) {

				chiHuRight.opr_or_long(GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ);
			} else if (_logic.is_san_luan_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SAN_LUAN_ZHUO_WMQ);
			} else if (_logic.is_jie_mei_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIE_MEI_ZHUO_WMQ);
			} else if (_logic.is_zhuo_hu(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHUO_FU_WMQ);
			}

			if (GRR._left_card_count == 0) {
				chiHuRight.opr_or_long(GameConstants.CHR_HAI_DI_HU_WMQ);
			}
			if (_logic.is_zhen_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BA_WMQ);
			}
			if (_logic.is_jia_ba_peng_tou(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIA_BA_WMQ);
			}
			if (card_count == 1) {
				chiHuRight.opr_or_long(GameConstants.CHR_QUAN_QIU_REN_WMQ);
			}
			if (_logic.is_long_bai_wei(tuan_yuan_index)) {
				chiHuRight.opr_or_long(GameConstants.CHR_LONG_BAI_WEI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.color_hei(analyseItem.cbCardEye) == false) {
				chiHuRight.opr_or_long(GameConstants.CHR_XIANG_DUI_WMQ);
			}
			if (dui_zi_count == 7 && _logic.piao_dui(analyseItem)) {
				chiHuRight.opr_or_long(GameConstants.CHR_PIAO_DUI_WMQ);
			}
			if (this._hu_pai_max_hu[seat_index] == 10) {
				chiHuRight.opr_or_long(GameConstants.CHR_KA_HU_WMQ);
			}

			if (this._hu_pai_max_hu[seat_index] == 10 && _logic.is_sx_wu_qian_nian(analyseItem, weaveItems, weaveCount, cbMingIndexTemp, cur_card)) {
				chiHuRight.opr_or_long(GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ);
			}

			if (_logic.is_ji_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_JI_DING_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_LEFT;
			} else if (analyseItem.cbCardEye == cur_card) {
				if (dui_zi_count == 7) {
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_DZ_WMQ);
				} else
					chiHuRight.opr_or_long(GameConstants.CHR_DAN_DI_WMQ);
			}

			if (_logic.is_bei_kao_bei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				// if (_logic.get_card_value(cur_card) == 8 || (cur_card == 0
				// && _logic.get_card_value(analyseItem.cbCenterCard[weaveCount
				// - 1]) == 8)) {
				// chiHuRight.opr_or_long(GameConstants.CHR_ZHEN_BKB_WMQ);
				// } else
				chiHuRight.opr_or_long(GameConstants.CHR_BEI_KAO_BEI);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_shou_qian_shou(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_SHOU_QIAN_SHOU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_feng_bai_wei(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_FBW_WMQ);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;

			}
			if (this._zhe_zhe_count[seat_index] > 0)
				chiHuRight.opr_or_long(GameConstants.CHR_ZHE_ZHE_HU);

			if (_logic.is_dui_dao_hu(_playerStatus[seat_index]._hu_cards, _playerStatus[seat_index]._hu_card_count, cur_card, analyseItem,
					weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_TUI_DAO_HU);
				this._hu_pai_action[seat_index] = GameConstants.WIK_PENG;
			}
			if (_logic.is_yuan_yuan_ding(analyseItem, cur_card) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_YUAN_YUAN_DI);
			}
			if (_logic.is_bian_ding(cur_card, analyseItem, weaveCount) == true) {
				chiHuRight.opr_or_long(GameConstants.CHR_BIAN_DING);
			}

		}
		if (this._hu_pai_action[seat_index] == 0 && cur_card != 0) {
			if (cur_card == analyseItem.cbCardEye) {
				this._hu_pai_action[seat_index] = GameConstants.WIK_DUI_ZI;
			}
			for (int i = 5; i >= 0; i--) {
				for (int j = 0; j < 3; j++) {
					if (analyseItem.cbCardData[i][j] == cur_card) {
						this._hu_pai_action[seat_index] = analyseItem.cbWeaveKind[i];
						break;
					}

				}
			}

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
			if (this._cannot_peng[seat_index][_logic.switch_to_card_index(card_data)] != 0)
				action = GameConstants.WIK_CHOU_SAO;
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
			if (this._cannot_peng[seat_index][_logic.switch_to_card_index(card_data)] != 0)
				action = GameConstants.WIK_CHOU_WEI;
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_WEI;
		}
		return bAroseAction;
	}

	public int estimate_player_wei_respond_wmq(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_is_xiang_gong[seat_index] == true)
			return bAroseAction;
		// 偎牌判断
		if ((bAroseAction == GameConstants.WIK_NULL)
				&& (_logic.check_wei_wmq(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			if (this._cannot_peng[seat_index][this._logic.switch_to_card_index(card_data)] != 0)
				action = GameConstants.WIK_CHOU_WEI;

			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_WEI;
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
			if (this._cannot_chi[chi_seat_index][cur_card_index] != 0)
				continue;
			for (chi_index = 0; chi_index < GRR._discard_count[last_index]; chi_index++) {
				if ((GRR._discard_cards[last_index][chi_index] & 0xff) == card) {
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
			if (this._cannot_peng[i][cur_card_index] != 0)
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
	public boolean estimate_player_out_card_respond_wmq(int seat_index, int card, boolean bDisdatch) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		//
		// _playerStatus[i].clean_action();
		// _playerStatus[i].clean_weave();
		// }

		PlayerStatus playerStatus = null;
		int card_index = _logic.switch_to_card_index(card);

		int action = GameConstants.WIK_NULL;
		int next_index = 0;
		if (bDisdatch != true)
			next_index++;
		int player_pass[] = new int[this.getTablePlayerNumber()];

		do {

			int chi_seat_index = (seat_index + next_index) % getTablePlayerNumber();
			int last_index = (chi_seat_index - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
			next_index++;
			int chi_index = 0;
			if (_is_xiang_gong[chi_seat_index] == true)
				continue;
			if (this._zhe_zhe_count[chi_seat_index] > 0)
				continue;
			if (this._cannot_chi[chi_seat_index][card_index] != 0)
				continue;

			int card_count = 0;

			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				card_count += this.GRR._cards_index[chi_seat_index][j];
			}
			if (card_count == 2)
				continue;
			if (GRR._cards_index[chi_seat_index][card_index] >= 2)
				continue;
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false) {

				action = _logic.check_chi_wmq(GRR._cards_index[chi_seat_index], card);

				if ((action & GameConstants.WIK_LEFT) != 0) {

					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
					player_pass[chi_seat_index] = 1;
				}
				if ((action & GameConstants.WIK_CENTER) != 0) {

					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
					player_pass[chi_seat_index] = 1;

				}
				if ((action & GameConstants.WIK_RIGHT) != 0) {

					_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
					_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					player_pass[chi_seat_index] = 1;

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
				card_count += this.GRR._cards_index[i][j];
			}
			if (_is_xiang_gong[i] == true)
				continue;
			if (this._zhe_zhe_count[i] > 0)
				continue;
			if (card_count == 2)
				continue;
			int peng_index = 0;
			if (this._cannot_peng[i][card_index] != 0)
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

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			this.log_error("gme_status:" + this._game_status + "GS_MJ_WAIT  seat_index:" + seat_index);
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
		if (this._zhe_zhe_count[seat_index] > 0)
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_ZHE_ZHE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);
		else
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
		if (this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)) {
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
	public void countChiHuTimes_wmq(int target_player, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[target_player];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];

		}
		_player_result.hu_pai_count[target_player]++;
		_player_result.ying_xi_count[target_player] += this._hu_xi[target_player];

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
	public void process_chi_hu_player_score_wmq_s(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		this._hu_xi[seat_index] = this._hu_pai_max_hu[seat_index];
		int calculate_score = this._hu_xi[seat_index];
		this._provider_hu = provide_index;
		int wFanShu = 0;
		int hu_xi = 0;
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_lmt_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], _zhe_zhe_count[seat_index],
					chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_lmt_wmq_s(seat_index, chr);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
			hu_xi = _logic.get_chi_hu_action_rank_qmt_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], _zhe_zhe_count[seat_index],
					chr);// 番数
			hu_xi += _logic.get_chi_hu_action_rank_qmt_wmq_special_s(this._hu_special_type[seat_index]);
			wFanShu = _logic.get_chi_hu_ying_xi_qmt_wmq_s(seat_index, chr);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_xzb_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_xzb_wmq_s(seat_index, chr);// 番数

		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {
			hu_xi = _logic.get_chi_hu_action_rank_dzb_wmq_s(seat_index, _da_pai_count[seat_index], _xiao_pai_count[seat_index],
					this._ying_hu_count[seat_index], this._chun_ying_count[seat_index], this._hong_pai_count[seat_index], _zhe_zhe_count[seat_index],
					chr);// 番数
			wFanShu = _logic.get_chi_hu_ying_xi_dzb_wmq_s(seat_index, chr);// 番数

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
		this.countChiHuTimes_wmq(seat_index, true);
		if (has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_ON)) {
			if (this._hu_xi[seat_index] - 17 > 0)
				operate_dou_liu_zi(seat_index, true, this._hu_xi[seat_index] - 17, false);

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
		this.countChiHuTimes_wmq(seat_index, true);
		if (has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_ON)) {
			if (this._hu_xi[seat_index] - 17 > 0)
				operate_dou_liu_zi(seat_index, true, this._hu_xi[seat_index] - 17, false);

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
				// send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			// 流局

			if (has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_OFF) || this._zong_liu_zi_fen <= 0)
				operate_effect_action(_cur_banker, GameConstants.EFFECT_ACTION_DRAW, 1, new long[] { GameConstants.ACT_DRAW }, 1,
						GameConstants.INVALID_SEAT);
			if (GRR == null) {
				if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX) && has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_ON))
					GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT),
							1000, TimeUnit.MILLISECONDS);
				else
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX) && has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_ON))
					GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY), 1000,
							TimeUnit.MILLISECONDS);
				else
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
		if (this.is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(900);
		} else {
			roomResponse.setFlashTime(100);
			roomResponse.setStandTime(600);
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
		if (GRR != null) {
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
	public boolean operate_cannot_card(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
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
			if (GRR != null)
				GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public boolean operate_dou_liu_zi(int seat_index, boolean win, int deng_shu, boolean is_fen_zi) {
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
						this._liu_zi_fen[i] = -50;
					this._zong_liu_zi_fen += -this._liu_zi_fen[i];

				}

				_player_result.game_score[i] += this._liu_zi_fen[i];
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				GRR._game_score[i] += this._liu_zi_fen[i];
			}
			action = GameConstants.LZ_DOU_LZ;

		} else if (is_fen_zi != true) {
			int deng_fen = 0;
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_ONE_FEN)) {
				deng_fen = 200;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_TWO_FEN)) {
				deng_fen = 300;
			}
			if (this.has_rule(GameConstants.GAME_RULE_ZHX_THREE_FEN)) {
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
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				GRR._game_score[i] += this._liu_zi_fen[i];
				_player_result.liu_zi_fen[i] += this._liu_zi_fen[i];
			}

		}

		if (is_fen_zi == true) {
			int score = this._zong_liu_zi_fen / this.getTablePlayerNumber();
			int max = _cur_banker;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_player_result.game_score[max] < _player_result.game_score[i])
					max = i;
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == max)
					this._liu_zi_fen[i] = score + this._zong_liu_zi_fen % this.getTablePlayerNumber();
				else
					this._liu_zi_fen[i] = score;
				_player_result.game_score[i] += this._liu_zi_fen[i];
				_player_result.liu_zi_fen[i] += this._liu_zi_fen[i];
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
		// this.load_player_info_data(roomResponse);
		if (GRR != null)
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

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);// get牌
		roomResponse.setCardCount(count);
		if (this.is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
			roomResponse.setFlashTime(150);
			roomResponse.setStandTime(900);
		} else {
			roomResponse.setFlashTime(100);
			roomResponse.setStandTime(600);
		}

		roomResponse.setInsertTime(150);
		if (sao == true)
			roomResponse.setEffectType(100);
		if (to_player == -2)
			roomResponse.setEffectType(100);

		if (to_player == GameConstants.INVALID_SEAT || to_player == -2) {
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
	 * 游戏中每个玩家的分数
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
	public boolean operate_game_mid_score(int seat_index, boolean display) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_MID_SCORE);
		roomResponse.setOperatePlayer(seat_index);
		if (display == true)
			roomResponse.setFlashTime(this._game_weave_score[seat_index]);
		roomResponse.setStandTime(this._game_other_mid_score[seat_index]);
		this.send_response_to_other(seat_index, roomResponse);
		roomResponse.setFlashTime(this._game_weave_score[seat_index]);
		roomResponse.setStandTime(this._game_mid_score[seat_index]);
		if (display == true)
			GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public boolean operate_zhe_zhe_count(int seat_index, boolean is_grr) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ZHE_ZHE_HU_COUNT);
		roomResponse.setCardCount(this._zhe_zhe_count[seat_index]);
		roomResponse.setOperatePlayer(seat_index);
		if (is_grr == true) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(seat_index, roomResponse);
		}
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

	private void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJthis.getTablePlayerNumber();//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJthis.getTablePlayerNumber();

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	private void countCardType(ChiHuRight chiHuRight, int seat_index) {
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

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * 结算描述
	 */
	private void set_result_describe_wmq(int seat_index) {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			// des+=",总息:"+this._all_xi;
			// if(seat_index != -1)
			// des += ",硬息:"+this._hu_xi[seat_index];
			// des+=",分溜子:"+this._all_lz_fen;

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡 :硬息×4";
						}
						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:硬息×6";
						}

						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡 :硬息×2";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×3";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+50息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+10";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (10 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+50息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+50息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:" + "+" + _ying_hu_count[i] * 10 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:" + "+" + _ying_hu_count[i] * 10 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 10 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 10 + "息";
							}
						}
					}

					else if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息 ×8";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×4";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×6";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+100息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+20息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (20 * (this._hong_pai_count[i] - 10)) + "息";
						}

						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:" + "+" + _ying_hu_count[i] * 20 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:" + "+" + _ying_hu_count[i] * 20 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 20 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 20 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:硬息×10";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+100息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+100" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",桌胡:+20" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+40" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+60" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+80" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+150" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+200" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+150" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+20" + "息";
						}
						if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {
							if (type == GameConstants.CHR_DAN_DI_WMQ)
								des += ",单丁:+20" + "息";
						}

						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+200" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+100" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+20" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+20" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+100" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+20" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+20" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",龙摆尾:+100" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+20" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+20" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+50" + "息";
						}

						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+100" + "息";
						}

					} else if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
						if (type == GameConstants.CHR_ZI_MO_WMQ) {
							des += ",自摸:+30息";
						}
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息×10";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×6";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×8";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+200" + "息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+30" + "息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (30 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 30 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 200 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 200 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 200 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 200 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:+200" + "息";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+200" + "息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+200" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",桌胡:+40" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+80" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+120" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+150" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+300" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+450" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+400" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_WMQ) {
							des += ",单丁:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
							des += ",单丁:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+300" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+200" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+50" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+50" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+150" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+50" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+50" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",龙摆尾:+200" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+50" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+50" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+100" + "息";
						}

						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_ALL_HEI_TIAN_HU) {
							des += ",全黑天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_NO_TEN_XI_TIAN_HU) {
							des += ",无息天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_LDH_TIAN_HU) {
							des += ",六对红天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_JIU_DUI_TIAN_HU) {
							des += ",九对天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_SBD_TIAN_HU) {
							des += ",四边对天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_ONE_TEN_TIAN_HU) {
							des += ",一到十天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_BIAN_KAN_HU) {
							des += ",边坎:+30" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BKB_WMQ) {
							des += ",真背靠背:+100" + "息";
						}
						if (type == GameConstants.CHR_KA_HU_WMQ) {
							des += ",卡胡:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHA_DAN_WMQ) {
							des += ",红炸弹天胡:+" + _ying_hu_count[i] * 150 + "息";
						}
						if (type == GameConstants.CHR_HEI_ZHA_DAN_WMQ) {
							des += ",黑炸弹天胡:+" + _chun_ying_count[i] * 100 + "息";
						}
						if (type == GameConstants.CHR_FBW_WMQ) {
							des += ",凤摆尾:+50" + "息";
						}
					} else if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息×10";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×6";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×8";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+150" + "息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+30" + "息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (30 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 30 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:+200" + "息";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+150" + "息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+150" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",桌胡:+40" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+80" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+120" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+150" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+300" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+450" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+300" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+30" + "息";
						}

						if (type == GameConstants.CHR_DAN_DI_WMQ) {
							des += ",单丁:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
							des += ",单丁:+50" + "息";
						}

						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+300" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+200" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+50" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+50" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+150" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+50" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+50" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",龙摆尾:+150" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+50" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+50" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+100" + "息";
						}
						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+150" + "息";
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
	private void set_result_describe_wmq_s(int seat_index) {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			// des+=",总息:"+this._all_xi;
			// if(seat_index != -1)
			// des += ",硬息:"+this._hu_xi[seat_index];
			// des+=",分溜子:"+this._all_lz_fen;

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {

					if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG)) {
						if (type == GameConstants.CHR_ZI_MO_WMQ) {
							des += ",自摸:+30息";
						}
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息×10";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×6";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×8";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+200" + "息";
						}
						if (type == GameConstants.CHR_2_MAN_YUAN_HUA) {
							des += ",2息满园花:+150" + "息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+30" + "息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (30 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 30 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:硬息×15";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+200" + "息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+200" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",两乱桌:+50" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+100" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+150" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+200" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+300" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+400" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+300" + "息";
						}
						if (type == GameConstants.CHR_TWO_JIE_MEI_ZHUO) {
							des += ",两姊妹桌:+400" + "息";
						}
						if (type == GameConstants.CHR_ZU_SUN_ZHUO) {
							des += ",祖孙桌:+500" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+50" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_WMQ) {
							des += ",单丁:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
							des += ",单丁:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+400" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+200" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+50" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+50" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+150" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+50" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+100" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",大龙摆尾:+150" + "息";
						}
						if (type == GameConstants.CHR_FBW_WMQ) {
							des += ",小龙摆尾:+50" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+50" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+50" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+100" + "息";
						}
						if (type == GameConstants.CHR_BIAN_DING) {
							des += ",边丁:+50" + "息";
						}
						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_ALL_HEI_TIAN_HU) {
							des += ",全黑天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_NO_TEN_XI_TIAN_HU) {
							des += ",无息天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_LDH_TIAN_HU) {
							des += ",六对红天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_JIU_DUI_TIAN_HU && i != GRR._banker_player) {
							des += ",九对天胡:+150" + "息";
						} else if (type == GameConstants.CHR_JIU_DUI_TIAN_HU && i == GRR._banker_player) {
							des += ",十对天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_SBD_TIAN_HU) {
							des += ",四边对天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_BIAN_KAN_HU) {
							des += ",边坎:+30" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BKB_WMQ) {
							des += ",真背靠背:+100" + "息";
						}
						if (type == GameConstants.CHR_KA_HU_WMQ) {
							des += ",卡胡:+30" + "息";
						}
						if (type == GameConstants.CHR_ZHA_DAN_WMQ) {
							des += ",红炸弹天胡:+200" + "息";
						}
						if (type == GameConstants.CHR_HEI_ZHA_DAN_WMQ) {
							des += ",黑炸弹天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_TUI_DAO_HU) {
							des += ",对倒胡:+30" + "息";
						}
						if (type == GameConstants.CHR_YUAN_YUAN_DI) {
							des += ",圆圆丁:+100" + "息";
						}
						if (type == GameConstants.CHR_ZHE_ZHE_HU) {
							des += ",啫啫胡:+" + (30 * (this._zhe_zhe_count[i])) + "息";
						}
						if (type == GameConstants.CHR_XING_LIAN_XING) {
							des += ",心连心:+30" + "息";
						}
						if (type == GameConstants.CHR_HUO_ZHOU_3) {
							des += ",活捉小三:+30" + "息";
						}
						if (type == GameConstants.CHR_2_HONG_2_HEI) {
							des += ",两红两黑:+30" + "息";
						}
						if (type == GameConstants.CHR_MM_CAI_DAN_CHE) {
							des += ",美女踩单车:+50" + "息";
						}
						if (type == GameConstants.CHR_2_LONG_XI_ZHU) {
							des += ",二龙戏珠:+50" + "息";
						}
						if (type == GameConstants.CHR_GE_SHAN_DA_NIU) {
							des += ",隔山打牛 +30息";
						}
						if (type == GameConstants.CHR_ONE_TEN_TIAN_HU) {
							des += ",一条龙+150息";
						}
					}

					else if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN)) {

						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息×6";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×3";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×5";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+100" + "息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+10" + "息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (10 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:+" + _ying_hu_count[i] * 20 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:+" + _ying_hu_count[i] * 20 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 20 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 20 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 100 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:硬息×8";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+100" + "息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+100" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",两乱桌:+20" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+40" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+60" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+80" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+150" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+300" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+200" + "息";
						}
						if (type == GameConstants.CHR_TWO_JIE_MEI_ZHUO) {
							des += ",两姊妹桌:+300" + "息";
						}
						if (type == GameConstants.CHR_ZU_SUN_ZHUO) {
							des += ",祖孙桌:+400" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+20" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_WMQ) {
							des += ",单丁:+10" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
							des += ",单丁:+20" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+200" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+100" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+20" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+20" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+100" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+20" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+50" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",大龙摆尾:+100" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+20" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+20" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+50" + "息";
						}
						if (type == GameConstants.CHR_BIAN_DING) {
							des += ",边丁:+20" + "息";
						}
						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+50" + "息";
						}
						if (type == GameConstants.CHR_NO_TEN_XI_TIAN_HU) {
							des += ",无息天胡:+50" + "息";
						}

					} else if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
						if (type == GameConstants.CHR_ZI_MO_WMQ) {
							des += ",自摸:+30息";
						}
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息×10";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×4";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×6";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+150" + "息";
						}
						if (type == GameConstants.CHR_2_MAN_YUAN_HUA) {
							des += ",2息满园花:+120" + "息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+20" + "息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (20 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 30 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:硬息×15";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+150" + "息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+150" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",两乱桌:+40" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+80" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+100" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+150" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+300" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+400" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+300" + "息";
						}
						if (type == GameConstants.CHR_TWO_JIE_MEI_ZHUO) {
							des += ",两姊妹桌:+400" + "息";
						}
						if (type == GameConstants.CHR_ZU_SUN_ZHUO) {
							des += ",祖孙桌:+500" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_WMQ) {
							des += ",单丁:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
							des += ",单丁:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+300" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+150" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+50" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+50" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+150" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+50" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+50" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",大龙摆尾:+150" + "息";
						}
						if (type == GameConstants.CHR_FBW_WMQ) {
							des += ",小龙摆尾:+50" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+50" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+50" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+100" + "息";
						}
						if (type == GameConstants.CHR_BIAN_DING) {
							des += ",边丁:+50" + "息";
						}
						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_BIAN_KAN_HU) {
							des += ",边坎:+30" + "息";
						}
						if (type == GameConstants.CHR_ALL_HEI_TIAN_HU) {
							des += ",全黑天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_NO_TEN_XI_TIAN_HU) {
							des += ",无息天胡:+100" + "息";
						}

						if (type == GameConstants.CHR_KA_HU_WMQ) {
							des += ",卡胡:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHA_DAN_WMQ) {
							des += ",红炸弹天胡:+200" + "息";
						}
						if (type == GameConstants.CHR_HEI_ZHA_DAN_WMQ) {
							des += ",黑炸弹天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_LDH_TIAN_HU) {
							des += ",六对红天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_JIU_DUI_TIAN_HU && i != GRR._banker_player) {
							des += ",九对天胡:+120" + "息";
						} else if (type == GameConstants.CHR_JIU_DUI_TIAN_HU && i == GRR._banker_player) {
							des += ",十对天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_SBD_TIAN_HU) {
							des += ",四边对天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_TUI_DAO_HU) {
							des += ",对倒胡:+30" + "息";
						}
						if (type == GameConstants.CHR_YUAN_YUAN_DI) {
							des += ",圆圆丁:+100" + "息";
						}
						if (type == GameConstants.CHR_ZHE_ZHE_HU) {
							des += ",啫啫胡:+" + (30 * (this._zhe_zhe_count[i])) + "息";
						}
						if (type == GameConstants.CHR_XING_LIAN_XING) {
							des += ",心连心:+30" + "息";
						}
						if (type == GameConstants.CHR_HUO_ZHOU_3) {
							des += ",活捉小三:+30" + "息";
						}
						if (type == GameConstants.CHR_2_HONG_2_HEI) {
							des += ",两红两黑:+30" + "息";
						}
						if (type == GameConstants.CHR_GE_SHAN_DA_NIU) {
							des += ",隔山打牛 +30息";
						}
						if (type == GameConstants.CHR_ONE_TEN_TIAN_HU) {
							des += ",一条龙+150息";
						}

					} else if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {
						if (type == GameConstants.CHR_DUI_ZI_HU_WMQ) {
							des += ",对子胡:硬息×10";
						}
						if (type == GameConstants.CHR_DIAN_HU_WMQ) {
							des += ",点胡:硬息×4";
						}
						if (type == GameConstants.CHR_WU_HU_WMQ) {
							des += ",乌胡:硬息×6";
						}
						if (type == GameConstants.CHR_MAN_YUAN_HUA_WMQ) {
							des += ",满园花:+150" + "息";
						}
						if (type == GameConstants.CHR_2_MAN_YUAN_HUA) {
							des += ",2息满园花:+120" + "息";
						}
						if (type == GameConstants.CHR_HONG_HU_WMQ) {
							des += ",红胡:+20" + "息";
						}
						if (type == GameConstants.CHR_DUO_HONG_WMQ) {
							des += ",多红:+" + (20 * (this._hong_pai_count[i] - 10)) + "息";
						}
						if (type == GameConstants.CHR_YING_HU_WMQ) {
							if (this._ying_hu_count[i] == 1) {
								des += ",一口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 2) {
								des += ",两口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 3) {
								des += ",三口印:+" + _ying_hu_count[i] * 30 + "息";
							}
							if (this._ying_hu_count[i] == 4) {
								des += ",四口印:+" + _ying_hu_count[i] * 30 + "息";
							}
						}
						if (type == GameConstants.CHR_CHUN_YING_WMQ) {
							if (this._chun_ying_count[i] == 1) {
								des += ",一口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 2) {
								des += ",两口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 3) {
								des += ",三口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
							if (this._chun_ying_count[i] == 4) {
								des += ",四口纯印:+" + this._chun_ying_count[i] * 150 + "息";
							}
						}

						if (type == GameConstants.CHR_WU_DUI_WMQ) {
							des += ",乌对:硬息×15";
						}

						if (type == GameConstants.CHR_DA_ZI_HU_WMQ) {
							des += ",大字胡:+150" + "息";
						}
						if (type == GameConstants.CHR_XIAO_ZI_HU_WMQ) {
							des += ",小字胡:+150" + "息";
						}
						if (type == GameConstants.CHR_ZHUO_FU_WMQ) {
							des += ",两乱桌:+40" + "息";
						}
						if (type == GameConstants.CHR_JIE_MEI_ZHUO_WMQ) {
							des += ",姊妹桌:+80" + "息";
						}
						if (type == GameConstants.CHR_SAN_LUAN_ZHUO_WMQ) {
							des += ",三乱桌:+100" + "息";
						}
						if (type == GameConstants.CHR_JM_DIA_TUO_ZHUO_WMQ) {
							des += ",姊妹桌带拖:+150" + "息";
						}
						if (type == GameConstants.CHR_DIA_SHUN_ZHUO) {
							des += ",爹孙桌:+300" + "息";
						}
						if (type == GameConstants.CHR_DS_DIA_TUO_WMQ) {
							des += ",爹孙桌带拖:+400" + "息";
						}
						if (type == GameConstants.CHR_SI_LUAN_ZHUO_WMQ) {
							des += ",四乱桌:+300" + "息";
						}
						if (type == GameConstants.CHR_TWO_JIE_MEI_ZHUO) {
							des += ",两姊妹桌:+400" + "息";
						}
						if (type == GameConstants.CHR_ZU_SUN_ZHUO) {
							des += ",祖孙桌:+500" + "息";
						}
						if (type == GameConstants.CHR_HAI_DI_HU_WMQ) {
							des += ",海底胡:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_WMQ) {
							des += ",单丁:+30" + "息";
						}
						if (type == GameConstants.CHR_DAN_DI_DZ_WMQ) {
							des += ",单丁:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BA_WMQ) {
							des += ",真八碰头:+300" + "息";
						}

						if (type == GameConstants.CHR_JIA_BA_WMQ) {
							des += ",假八碰头:+150" + "息";
						}
						if (type == GameConstants.CHR_BEI_KAO_BEI) {
							des += ",背靠背:+50" + "息";
						}
						if (type == GameConstants.CHR_SHOU_QIAN_SHOU) {
							des += ",手牵手:+50" + "息";
						}
						if (type == GameConstants.CHR_QUAN_QIU_REN_WMQ) {
							des += ",全球人:+150" + "息";
						}
						if (type == GameConstants.CHR_SX_WU_QIAN_NIAN_WMQ) {
							des += ",上下五千年:+50" + "息";
						}
						if (type == GameConstants.CHR_KA_WEI_WMQ) {
							des += ",卡偎:+50" + "息";
						}
						if (type == GameConstants.CHR_LONG_BAI_WEI_WMQ) {
							des += ",大龙摆尾:+150" + "息";
						}
						if (type == GameConstants.CHR_FBW_WMQ) {
							des += ",小龙摆尾:+50" + "息";
						}
						if (type == GameConstants.CHR_XIANG_DUI_WMQ) {
							des += ",项对:+50" + "息";
						}

						if (type == GameConstants.CHR_PIAO_DUI_WMQ) {
							des += ",飘对:+50" + "息";
						}
						if (type == GameConstants.CHR_JI_DING_WMQ) {
							des += ",鸡丁:+100" + "息";
						}
						if (type == GameConstants.CHR_BIAN_DING) {
							des += ",边丁:+50" + "息";
						}
						if (type == GameConstants.CHR_TIAN_HU_WMQ) {
							des += ",天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_ALL_HEI_TIAN_HU) {
							des += ",全黑天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_NO_TEN_XI_TIAN_HU) {
							des += ",无息天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_LDH_TIAN_HU) {
							des += ",六对红天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_JIU_DUI_TIAN_HU && i != GRR._banker_player) {
							des += ",九对天胡:+150" + "息";
						} else if (type == GameConstants.CHR_JIU_DUI_TIAN_HU && i == GRR._banker_player) {
							des += ",十对天胡:+150" + "息";
						}
						if (type == GameConstants.CHR_SBD_TIAN_HU) {
							des += ",四边对天胡:+120" + "息";
						}
						if (type == GameConstants.CHR_ZHEN_BKB_WMQ) {
							des += ",真背靠背:+100" + "息";
						}
						if (type == GameConstants.CHR_KA_HU_WMQ) {
							des += ",卡胡:+50" + "息";
						}
						if (type == GameConstants.CHR_ZHA_DAN_WMQ) {
							des += ",红炸弹天胡:+200" + "息";
						}
						if (type == GameConstants.CHR_HEI_ZHA_DAN_WMQ) {
							des += ",黑炸弹天胡:+100" + "息";
						}
						if (type == GameConstants.CHR_TUI_DAO_HU) {
							des += ",对倒胡:+30" + "息";
						}
						if (type == GameConstants.CHR_YUAN_YUAN_DI) {
							des += ",圆圆丁:+100" + "息";
						}
						if (type == GameConstants.CHR_ZHE_ZHE_HU) {
							des += ",啫啫胡:+" + (30 * (this._zhe_zhe_count[i])) + "息";
						}
						if (type == GameConstants.CHR_XING_LIAN_XING) {
							des += ",心连心:+30" + "息";
						}
						if (type == GameConstants.CHR_HUO_ZHOU_3) {
							des += ",活捉小三:+30" + "息";
						}
						if (type == GameConstants.CHR_2_HONG_2_HEI) {
							des += ",两红两黑:+30" + "息";
						}
					}

				}
			}
			if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG)) {
				if (this._hu_special_type[seat_index][GameConstants.CHR_WMQ_SPECIAL_GA_NUAN_DA] == 1)
					des += ",嘎暖哒:+30" + "息";
			}

			GRR._result_des[i] = des;
		}
	}

	// 转转麻将结束描述
	private void set_result_describe(int seat_index) {

		if (this.is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
			set_result_describe_wmq(seat_index);
		}
		if (this.is_mj_type(GameConstants.GAME_TYPE_WMQ_AX_S)) {
			set_result_describe_wmq_s(seat_index);
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
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX_S)) {
			sysParamModel1104 = SysParamServerDict.getInstance()
					.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index)).get(1104);
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}

		if (action == GameConstants.WIK_WEI || action == GameConstants.WIK_CHOU_WEI) {
			delay = 300;
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
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (_logic.switch_to_card_index(card_data) == i)
				GRR._cannot_out_index[seat_index][_logic.switch_to_card_index(card_data)] += card_count;
		}

		if (send_client == true) {
			this.operate_cannot_card(seat_index);
		}
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

	public boolean is_tian_hu_s(int seat_index, int provider, ChiHuRight chiHuRight, int cur_card_data, int is_common[]) {
		boolean is_tian_hu = false;
		if (seat_index != provider)
			cur_card_data = 0;
		_hu_weave_count[seat_index] = 0;
		_hu_pai_max_hu[seat_index] = 0;
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
		int hu_xi[] = new int[1];
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if ((seat_index == provider) && analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index, provider, cur_card_data, chiHuRight, card_type, hu_xi, true) != GameConstants.WIK_NULL) {
			is_tian_hu = true;
			is_common[0] = 1;

		}
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.color_hei(hand_cards_data[i]) == true) {
				all_hei_count++;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_QUAN_MING_TANG) == true) {
			if (seat_index == provider) {
				int cards_index[] = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				cards_index[_logic.switch_to_card_index(cur_card_data)]++;
				int cards_count = 0;
				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					cards_index[i] += GRR._cards_index[seat_index][i];
					if (cards_index[i] > 0)
						cards_count++;
				}
				// if(cards_count == 20)
				// {
				// chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
				// is_tian_hu = true;
				// }
			}
			if (all_hei_count == hand_card_count) {
				chiHuRight.opr_or_long(GameConstants.CHR_ALL_HEI_TIAN_HU);
				is_tian_hu = true;
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

			if (hong_dui >= 6) {
				chiHuRight.opr_or_long(GameConstants.CHR_LDH_TIAN_HU);
				is_tian_hu = true;
			}

			if ((dui_zi >= 9 && seat_index != provider) || (dui_zi == 10 && seat_index == provider)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIU_DUI_TIAN_HU);
				is_tian_hu = true;
			}

			if (si_bian_dui == 4) {
				chiHuRight.opr_or_long(GameConstants.CHR_SBD_TIAN_HU);
				is_tian_hu = true;
			}
			if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX)) {
				chiHuRight.opr_or_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
				is_tian_hu = true;
			}
			if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX, true)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHA_DAN_WMQ);
				is_tian_hu = true;
			}
			if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX, false)) {
				chiHuRight.opr_or_long(GameConstants.CHR_HEI_ZHA_DAN_WMQ);
				is_tian_hu = true;
			}

			if (is_tian_hu == true)
				return true;
		}
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG) == true) {
			if (seat_index == provider) {
				int cards_index[] = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				cards_index[_logic.switch_to_card_index(cur_card_data)]++;
				int cards_count = 0;
				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					cards_index[i] += GRR._cards_index[seat_index][i];
					if (cards_index[i] > 0)
						cards_count++;
				}
				// if(cards_count == 20)
				// {
				// chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
				// is_tian_hu = true;
				// }
			}
			if (all_hei_count == hand_card_count) {
				chiHuRight.opr_or_long(GameConstants.CHR_ALL_HEI_TIAN_HU);
				is_tian_hu = true;
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

			if (hong_dui >= 6) {
				chiHuRight.opr_or_long(GameConstants.CHR_LDH_TIAN_HU);
				is_tian_hu = true;
			}

			if ((dui_zi >= 9 && seat_index != provider) || (dui_zi == 10 && seat_index == provider)) {
				chiHuRight.opr_or_long(GameConstants.CHR_JIU_DUI_TIAN_HU);
				is_tian_hu = true;
			}

			if (si_bian_dui == 4) {
				chiHuRight.opr_or_long(GameConstants.CHR_SBD_TIAN_HU);
				is_tian_hu = true;
			}
			if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX)) {
				chiHuRight.opr_or_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
				is_tian_hu = true;
			}
			if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX, true)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHA_DAN_WMQ);
				is_tian_hu = true;
			}
			if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX, false)) {
				chiHuRight.opr_or_long(GameConstants.CHR_HEI_ZHA_DAN_WMQ);
				is_tian_hu = true;
			}

			if (is_tian_hu == true)
				return true;
		}
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN)) {

			if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX)) {
				chiHuRight.opr_or_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
				is_tian_hu = true;
			}
			if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX, true)) {
				chiHuRight.opr_or_long(GameConstants.CHR_ZHA_DAN_WMQ);
				is_tian_hu = true;
			}
			if (_logic.is_zha_dan_tian_hu(hand_index_data, GameConstants.MAX_HH_INDEX, false)) {
				chiHuRight.opr_or_long(GameConstants.CHR_HEI_ZHA_DAN_WMQ);
				is_tian_hu = true;
			}

			if (is_tian_hu == true)
				return true;
		}
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN) == true) {

			if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX)) {
				chiHuRight.opr_or_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
				is_tian_hu = true;
			}

			if (is_tian_hu == true)
				return true;
		}

		return false;
	}

	public boolean is_tian_hu(int seat_index, int provider, ChiHuRight chiHuRight, int cur_card_data) {
		boolean is_tian_hu = false;
		if (has_rule(GameConstants.GAME_RULE_DA_ZHUO_BAN))
			return is_tian_hu;
		if (has_rule(GameConstants.GAME_RULE_XIAO_ZHUO_BAN))
			return is_tian_hu;
		if (has_rule(GameConstants.GAME_RULE_LAO_MING_TANG))
			return is_tian_hu;
		if (seat_index != provider)
			cur_card_data = 0;
		_hu_weave_count[seat_index] = 0;
		_hu_pai_max_hu[seat_index] = 0;
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
		int hu_xi[] = new int[1];
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if ((seat_index == provider) && analyse_chi_hu_card(GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index],
				seat_index, provider, cur_card_data, chiHuRight, card_type, hu_xi, true) != GameConstants.WIK_NULL) {
			is_tian_hu = true;

		}
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.color_hei(hand_cards_data[i]) == true) {
				all_hei_count++;
			}
		}
		if (seat_index == provider) {
			int cards_index[] = new int[GameConstants.MAX_HH_INDEX];
			Arrays.fill(cards_index, 0);
			cards_index[_logic.switch_to_card_index(cur_card_data)]++;
			int cards_count = 0;
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				cards_index[i] += GRR._cards_index[seat_index][i];
				if (cards_index[i] > 0)
					cards_count++;
			}
			if (cards_count == 20) {
				chiHuRight.opr_or_long(GameConstants.CHR_ONE_TEN_TIAN_HU);
				is_tian_hu = true;
			}
		}
		if (all_hei_count == hand_card_count) {
			chiHuRight.opr_or_long(GameConstants.CHR_ALL_HEI_TIAN_HU);
			is_tian_hu = true;
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

		if (hong_dui >= 6) {
			chiHuRight.opr_or_long(GameConstants.CHR_LDH_TIAN_HU);
			is_tian_hu = true;
		}

		if (dui_zi >= 9) {
			chiHuRight.opr_or_long(GameConstants.CHR_JIU_DUI_TIAN_HU);
			is_tian_hu = true;
		}

		if (si_bian_dui == 4) {
			chiHuRight.opr_or_long(GameConstants.CHR_SBD_TIAN_HU);
			is_tian_hu = true;
		}
		if (_logic.is_not_ten_xi(hand_index_data, GameConstants.MAX_HH_INDEX)) {
			chiHuRight.opr_or_long(GameConstants.CHR_NO_TEN_XI_TIAN_HU);
			is_tian_hu = true;
		}
		this._ying_hu_count[seat_index] = _logic.is_zha_dan_tian_hu_count(hand_index_data, GameConstants.MAX_HH_INDEX, true);
		if (this._ying_hu_count[seat_index] > 0) {
			chiHuRight.opr_or_long(GameConstants.CHR_ZHA_DAN_WMQ);
			is_tian_hu = true;
		}
		this._chun_ying_count[seat_index] = _logic.is_zha_dan_tian_hu_count(hand_index_data, GameConstants.MAX_HH_INDEX, false);
		if (this._chun_ying_count[seat_index] > 0) {
			chiHuRight.opr_or_long(GameConstants.CHR_HEI_ZHA_DAN_WMQ);
			is_tian_hu = true;
		}

		if (is_tian_hu == true)
			return true;

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

		if (this.getRuleValue(GameConstants.GAME_RULE_WMQ_TWO_PLAYER) == 1)
			return 2;
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		if (has_rule(GameConstants.GAME_RULE_DOU_LIU_ZI_ON))
			this.operate_dou_liu_zi(seat_index, false, 0, false);
		else
			operate_effect_action(_cur_banker, GameConstants.EFFECT_ACTION_DRAW, 1, new long[] { GameConstants.ACT_DRAW }, 1,
					GameConstants.INVALID_SEAT);
		return false;
	}

	@Override
	public boolean hu_pai_timer(int seat_index, int operate_card, int wik_kind) {
		handler_operate_card(seat_index, wik_kind, operate_card, -1);
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		// TODO Auto-generated method stub
		return false;
	}
}
