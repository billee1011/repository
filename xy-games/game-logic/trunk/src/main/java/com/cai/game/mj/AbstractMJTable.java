package com.cai.game.mj;

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
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.Constants_GXLZ;
import com.cai.common.constant.game.Constants_JingDian_CS;
import com.cai.common.constant.game.Constants_YangZhong;
import com.cai.common.constant.game.GameConstants_YiYang;
import com.cai.common.constant.game.GameConstants_YiYang_SZG;
import com.cai.common.constant.game.mj.Constants_AnHua;
import com.cai.common.constant.game.mj.Constants_GuangChang;
import com.cai.common.constant.game.mj.Constants_HUAI_HUA;
import com.cai.common.constant.game.mj.Constants_MJ_JING_DE_ZHEN;
import com.cai.common.constant.game.mj.Constants_MJ_NING_XIANG;
import com.cai.common.constant.game.mj.Constants_MJ_PING_XIANG_258;
import com.cai.common.constant.game.mj.Constants_MJ_QUAN_ZHOU;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.common.constant.game.mj.Constants_WanZai;
import com.cai.common.constant.game.mj.Constants_YuanJiang;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.define.ECardType;
import com.cai.common.define.ELogType;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.define.ERoomStatus;
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
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.common.util.ZipUtil;
import com.cai.dictionary.BrandIdDict;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.JianPaoHuRunnable;
import com.cai.future.runnable.KickRunnable;
import com.cai.future.runnable.TuoGuanRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.handler.MJHandlerChiPeng;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.handler.MJHandlerFinish;
import com.cai.game.mj.handler.MJHandlerGang;
import com.cai.game.mj.handler.MJHandlerHaiDi;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;
import com.cai.game.mj.handler.MJHandlerYaoHaiDi;
import com.cai.game.mj.handler.ay.MJHandlerOutCardBaoTing;
import com.cai.game.mj.handler.lxcs.MJHandlerDispatchCard_XiaoHu_CSLX;
import com.cai.game.mj.handler.xthh.MJHandlerDispatchLastCard;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.ClubMsgSender;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
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
import protobuf.clazz.mj.Basic.MJ_Game_End_Basic;

//效验类型
class EstimatKind {
	public static int EstimatKind_OutCard = 1; // 出牌效验
	public static int EstimatKind_GangCard = 2; // 杠牌效验
}

public abstract class AbstractMJTable extends AbstractRoom {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static Logger logger = Logger.getLogger(AbstractMJTable.class);

	/**
	 * 这个值 不能在这里改--这个参数开发用 通过 命令行改牌
	 */
	public static boolean DEBUG_CARDS_MODE = false;

	/**
	 * 这个值，只能通过10后的gm操作进行设置
	 */
	public boolean DEBUG_MAGIC_CARD = false;
	public boolean DEBUG_SPECIAL_CARD = false;

	// 状态变量
	protected boolean _status_send; // 发牌状态
	protected boolean _status_gang; // 抢杆状态
	protected boolean _status_gang_hou_pao;
	protected boolean _status_cs_gang;

	public int _last_dispatch_player; // 牌桌上发牌时，进行储存，用来处理流局的坐庄问题

	public int _biaoyan_count[] = new int[4];

	// 运行变量
	public int _provide_card = GameConstants.INVALID_VALUE; // 供应扑克
	public int _resume_player = GameConstants.INVALID_SEAT; // 还原用户
	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _provide_player = GameConstants.INVALID_SEAT; // 供应用户
	public int _bu_gang_provide_player = GameConstants.INVALID_SEAT;
	// 上次出现碰胡的玩家位置
	public int peng_index = GameConstants.INVALID_SEAT;

	public int[] _card_can_not_out_after_chi; // 广西柳州麻将，不能吃什么牌然后打什么牌
	public int[][] _chi_pai_count; // 广西柳州麻将，吃牌统计

	// add by zaintan

	public ScheduledFuture _trustee_schedule[];// 托管定时器
	// add end

	// 发牌信息
	public int _send_card_data = GameConstants.INVALID_VALUE; // 发牌扑克
	public int chi_peng_index = GameConstants.INVALID_SEAT; // 当前吃碰玩家 -- 比赛场用

	public CardsData _gang_card_data;

	public int _send_card_count = GameConstants.INVALID_VALUE; // 发牌数目

	public int _qiang_max_count; // 最大呛分
	public int _lian_zhuang_player; // 连庄玩家
	public int _shang_zhuang_player; // 上庄玩家

	// 信阳麻将
	public int _pre_bangker_player;
	public int _lian_zhuang_win_score;// 连庄玩家赢分

	// 出牌信息
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int _out_card_data = GameConstants.INVALID_VALUE; // 出牌扑克
	public int _out_card_count = GameConstants.INVALID_VALUE; // 出牌数目
	public int _gang_mo_posion = 0;
	public int _gang_mo_cards[] = new int[2];

	public MJGameLogic _logic = null;
	public NewMjGameLogic newLogic = null;

	/**
	 * 做庄次数
	 */
	protected int banker_count[];

	protected long _request_release_time;
	protected ScheduledFuture _release_scheduled;
	protected ScheduledFuture _table_scheduled;

	public AbstractMJHandler<? super AbstractMJTable> _handler;

	protected MJHandlerDispatchCard _handler_dispath_card;
	protected MJHandlerOutCardOperate _handler_out_card_operate;
	protected MJHandlerGang _handler_gang;
	protected MJHandlerChiPeng _handler_chi_peng;
	// public MJHandlerHaiDi<?> _handler_hai_di;
	protected MJHandlerYaoHaiDi _handler_yao_hai_di;
	protected MJHandlerHaiDi _handler_hai_di;
	protected MJHandlerOutCardBaoTing _handler_out_card_bao_ting; // 报听
	protected MJHandlerDispatchLastCard _handler_dispath_last_card;
	protected MJHandlerDispatchCard_XiaoHu_CSLX _handler_dispatchCard_xiaohu_cslx;

	// public AbstractMJHandler _handler_dispath_card;
	// public AbstractMJHandler _handler_out_card_operate;
	// public AbstractMJHandler _handler_gang;
	// public AbstractMJHandler _handler_chi_peng;

	public MJHandlerFinish _handler_finish; // 结束

	protected final MJType mjType;

	/**
	 * 牌桌上，用于10后台使用gm进行牌配置的时候，用来觉得癞子牌的，翻出来的那张牌的牌值，比如1万是翻出来的牌，2万就是癞子
	 */
	public int magic_card_decidor;
	/**
	 * 牌桌上，用于10后台使用gm进行牌配置的时候，用来配置特殊牌
	 */
	public int special_card_decidor;
	/**
	 * 虚拟的在线状态，用来处理切换屏幕
	 */
	public boolean[] virtual_online;
	/**
	 * 用来储存2个骰子的点数
	 */
	public int[] tou_zi_dian_shu = new int[2];
	/**
	 * 摇骰子的动画时间(ms)
	 */
	public int time_for_tou_zi_animation = 1500;
	/**
	 * 摇骰子动画的消散延时(ms)
	 */
	public int time_for_tou_zi_fade = 200;
	/**
	 * 统计桌面上杠之后，发的牌的张数
	 */
	public int gang_dispatch_count;
	/**
	 * 武陟麻将，存储亮四打一的配牌数据
	 */
	public int[] lsdy_debug_cards;
	public boolean debug_lsdy = false;

	protected PerformanceTimer gameTimer;

	public AbstractMJTable() {
		this(MJType.DEFAULT);
	}

	public AbstractMJTable(MJType mjType) {
		super(RoomType.MJ, GameConstants.GAME_PLAYER);
		this.mjType = mjType;
		this._game_type_index = mjType.getValue();

		_logic = new MJGameLogic(mjType);
		newLogic = new NewMjGameLogic(mjType);

		virtual_online = new boolean[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			virtual_online[i] = true;
		}
		gang_dispatch_count = 0;

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

		_card_can_not_out_after_chi = new int[this.getTablePlayerNumber()];
		_chi_pai_count = new int[this.getTablePlayerNumber()][this.getTablePlayerNumber()];

		// 结束信息

		// 用户状态
		// _response = new boolean[MJGameConstants.GAME_PLAYER];
		// _player_action = new int[MJGameConstants.GAME_PLAYER];
		// _operate_card = new int[MJGameConstants.GAME_PLAYER];
		// _perform_action = new int[MJGameConstants.GAME_PLAYER];
		// _player_status = new int[MJGameConstants.GAME_PLAYER];
		//
		// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
		// _player_action[i] = MJGameConstants.WIK_NULL;
		// }

		_gang_card_data = new CardsData(GameConstants.MAX_COUNT);
		banker_count = new int[this.getTablePlayerNumber()];
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		_game_type_index = game_type_index;//
		// _game_type_index = MJGameConstants.GAME_TYPE_XTHH;//

		int dddd = FvMask.mask(GameConstants.GAME_RULE_HEBEI_DI_FEN_10);
		dddd |= FvMask.mask(GameConstants.GAME_RULE_HEBEI_YI_LAI_DAO_DI);
		_game_rule_index = game_rule_index;
		// _game_rule_index =dddd;//;
		_game_round = game_round;

		_cur_round = 0;

		_card_can_not_out_after_chi = new int[this.getTablePlayerNumber()];
		_chi_pai_count = new int[this.getTablePlayerNumber()][this.getTablePlayerNumber()];

		_last_dispatch_player = -1;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des());

		onInitTable();

		_handler_finish = new MJHandlerFinish();

	}

	/**
	 * 初始化牌桌的handler 以及其他
	 */
	protected abstract void onInitTable();

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub
		if (_trustee_schedule == null)
			return;

		if (_trustee_schedule[_seat_index] != null) {
			_trustee_schedule[_seat_index] = null;
		}

		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
			return;

		// TODO Auto-generated method stub
		handler_request_trustee(_seat_index, true, 0);

	}

	///////////////////////////////////////////////////////////////////////

	/**
	 * 第一轮 初始化庄家 默认第一个。需要的继承
	 */
	protected void initBanker() {

	}

	/**
	 * 处理一些值的清理
	 * 
	 * @return
	 */
	@Override
	public boolean reset_init_data() {
		if (_cur_round == 0) {

			this.initBanker();
			record_game_room();
		}

		virtual_online = new boolean[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			virtual_online[i] = true;
		}

		gang_dispatch_count = 0;

		_last_dispatch_player = -1;

		_card_can_not_out_after_chi = new int[this.getTablePlayerNumber()];
		_chi_pai_count = new int[this.getTablePlayerNumber()][this.getTablePlayerNumber()];

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

		GRR = new GameRoundRecord();
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		_end_reason = GameConstants.INVALID_VALUE;
		istrustee = new boolean[4];
		// 新建
		_playerStatus = new PlayerStatus[4];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i] = new PlayerStatus();
		}

		// _cur_round=8;
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

		// TODO 设置房间的xml玩法信息
		if (commonGameRuleProtos != null) {
			GRR._room_info.setNewRules(commonGameRuleProtos);
		}

		Player rplayer;
		// WalkerGeek 允许少人模式
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;
		if (this.is_mj_type(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE) || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_MJ_CD_DT)) {
			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HONGZHONG)) { // 112张
				_repertory_card = new int[GameConstants.CARD_COUNT_HZ];
				shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
			} else { // 108张
				_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
				shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
			}
		} else {
			this.init_shuffle();
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			logger.error("card_log", e);
		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		// 再次同步一下托管状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			be_in_room_trustee_match(i);
		}
		return on_game_start();
	}

	/**
	 * 
	 * @return
	 */
	protected abstract boolean on_game_start();

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	protected void init_shuffle() {
		_repertory_card = new int[mjType.getCardLength()];
		shuffle(_repertory_card, mjType.getCards());
	};

	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}

		return false;
	}

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

		if (this.getClass().getAnnotation(ThreeDimension.class) != null) {
			show_tou_zi(GRR._banker_player);
		}

		int count = getTablePlayerNumber();
		if (count <= 0) {
			count = has_rule(GameConstants.GAME_RULE_HUNAN_THREE) ? GameConstants.GAME_PLAYER - 1 : GameConstants.GAME_PLAYER;
		}

		// 分发扑克
		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;

		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	protected void test_cards() {

		// 黑摸
		int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x12, 0x13, 0x13, 0x13, 0x13, 0x26 };
		// int cards[] = new int[] { 0x35, 0x04, 0x05, 0x06, 0x15, 0x16, 0x17,
		// 0x17, 0x17, 0x18, 0x24, 0x25, 0x26 };

		// 晃晃不能胡
		// int cards[] = new int[] { 0x12, 0x13, 0x16, 0x17, 0x18,0x22, 0x22,
		// 0x24, 0x25, 0x26, 0x28, 0x28, 0x28 };

		// 晃晃没显示听癞子
		// int cards[] = new int[] { 0x02, 0x04, 0x06, 0x06, 0x06,0x14, 0x15,
		// 0x16, 0x21, 0x21, 0x21, 0x22, 0x22 };

		// 晃晃没显示听癞子
		// int cards[] = new int[] { 0x22, 0x06, 0x06, 0x06, 0x06, 0x14, 0x15,
		// 0x16, 0x21, 0x21, 0x21, 0x21, 0x22 };

		// 双鬼没显示听鬼
		// int cards[] = new int[] { 0x01, 0x02, 0x15, 0x15, 0x16,0x17, 0x18,
		// 0x26, 0x26, 0x26, 0x27, 0x28, 0x29 };

		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x05, 0x06, 0x07,0x08,
		// 0x12, 0x13, 0x14, 0x17, 0x18, 0x19 };

		// 红中显示听多牌
		// int cards[] = new int[]
		// {0x02,0x03,0x04,0x07,0x08,0x09,0x11,0x11,0x11,0x18,0x18,0x25,0x26};
		// 杠5饼出错
		// int cards[] = new int[]
		// {0x02,0x02,0x06,0x07,0x22,0x22,0x22,0x25,0x25,0x02,0x27,0x28,0x29};

		// 五鬼不能胡
		// int cards[] = new int[]
		// {0x01,0x01,0x03,0x03,0x06,0x06,0x07,0x15,0x16,0x18,0x18,0x25,0x26};

		// 起手四红中

		// int cards[] = new int[]
		// {0x11,0x11,0x12,0x12,0x13,0x13,0x14,0x14,0x19,0x19,0x19,0x19,0x16};

		// 红中不能胡1
		// int cards[] = new int[]
		// {0x11,0x12,0x12,0x13,0x14,0x16,0x16,0x17,0x18,0x21,0x22,0x23,0x35};
		// 红中不能胡2
		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x17,0x17,0x21,0x22,0x22,0x23,0x24,0x26,0x28,0x35};
		// 杠牌重复/
		// int cards[] = new int[] { 0x09, 0x09, 0x18, 0x18, 0x19, 0x19, 0x19,
		// 0x26, 0x27, 0x28, 0x29, 0x29, 0x29 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x21, 0x22, 0x23, 0x12,
		// 0x13, 0x14, 0x15, 0x16, 0x17, 0x18 };

		// 花牌
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};

		// int cards[] = new int[]
		// {0x05,0x05,0x07,0x07,0x08,0x09,0x09,0x17,0x17,0x18,0x18,0x19,0x19};
		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x04,0x05,0x06,0x16,0x16,0x21,0x21,0x21,0x15,0x15};

		// int cards[] = new int[]
		// {0x01,0x02,0x03,0x04,0x05,0x06,0x16,0x16,0x21,0x21,0x21,0x15,0x15};
		// int cards[] = new int[]
		// {0x03,0x03,0x05,0x05,0x06,0x07,0x08,0x08,0x08,0x12,0x12,0x12,0x22};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// //int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};
		// int cards[] = new int[]
		// {0x09,0x09,0x18,0x18,0x19,0x19,0x19,0x26,0x27,0x28,0x29,0x29,0x38};

		// 单吊
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};
		// int cards[] = new int[]
		// {0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x04,0x04,0x04,0x06};

		// 全球人
		// int cards[] = new int[] { 0x13, 0x14, 0x15, 0x16, 0x17, 0x17, 0x17,
		// 0x18, 0x18, 0x18, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x23, 0x24, 0x25, 0x16, 0x17, 0x17, 0x17,
		// 0x08, 0x08, 0x08, 0x19, 0x19, 0x19 };
		// 河南麻将七小对
		/*
		 * int cards[] = new int[]
		 * {0x11,0x11,0x12,0x13,0x14,0x15,0x15,0x15,0x18,0x18,0x17,0x17,0x17};
		 */

		// int cards[] = new int[] { 0x21, 0x21, 0x21, 0x03, 0x03, 0x03, 0x06,
		// 0x06, 0x06, 0x07, 0x07, 0x07, 0x09 };

		// 吃
		// int cards[] = new int[]
		// {0x01,0x02,0x05,0x05,0x21,0x23,0x24,0x26,0x26,0x27,0x28,0x29,0x29};

		// 板板胡
		// int cards[] = new int[]
		// {0x03,0x14,0x16,0x16,0x17,0x17,0x17,0x21,0x21,0x23,0x23,0x19,0x19};
		// 红中中2鸟算分不对
		// int cards[] = new int[]
		// {0x07,0x09,0x12,0x12,0x15,0x17,0x21,0x21,0x21,0x23,0x23,0x23,0x35};
		// 杠牌胡两张

		// int cards[] = new int[] { 0x01, 0x01, 0x1, 0x4, 0x2, 0x3, 0x13, 0x14,
		// 0x12, 0x21, 0x23, 0x23, 0x23 };
		// int cards[] = new int[] { 0x01, 0x06, 0x06, 0x06, 0x06, 0x04, 0x05,
		// 0x08, 0x08, 0x08, 0x09, 0x09, 0x09 };
		// int cards[] = new int[]
		// {0x01,0x06,0x06,0x06,0x03,0x04,0x05,0x08,0x08,0x08,0x09,0x09,0x09};
		// int cards[] = new int[]
		// {0x01,0x06,0x06,0x06,0x03,0x04,0x05,0x08,0x08,0x08,0x09,0x09,0x09};

		// int cards[] = new int[]
		// {0x01,0x02,0x04,0x04,0x05,0x07,0x07,0x08,0x14,0x19,0x21,0x23,0x29};
		// 四喜

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x16,
		// 0x16, 0x17, 0x17, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };

		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19,
		//
		// 0x19, 0x19 };
		// int cards[] = new int[] { 0x15, 0x19, 0x13, 0x02, 0x04, 0x03, 0x15,
		// 0x15, 0x17, 0x17, 0x19,0x17, 0x19 };
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x19 };
		// int cards[] = new int[] { 0x15, 0x19, 0x13, 0x02, 0x04, 0x03, 0x15,
		// 0x15, 0x17, 0x17, 0x19,0x17, 0x19 };
		// //碰碰胡
		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x03, 0x03,0x11, 0x11,
		// 0x11, 0x15, 0x15, 0x21, 0x21, 0x21 };
		// 清一色碰碰胡
		// int cards[] = new int[] { 0x06, 0x06, 0x06, 0x06, 0x17, 0x17, 0x17,
		// 0x23, 0x33, 0x33, 0x33, 0x23, 0x23 };

		// 将将胡
		// int cards[] = new int[] { 0x02, 0x02, 0x02, 0x05, 0x05, 0x05, 0x08,
		// 0x08, 0x08, 0x12, 0x12, 0x12, 0x15 };

		// 十三幺
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 十三烂
		// int[] cards = new int[] { 0x01, 0x03, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 起手胡
		// int[] cards = new int[] { 0x01, 0x09, 0x11, 0x19, 0x21, 0x29, 0x31,
		// 0x32, 0x33, 0x34, 0x35, 0x36, 0x37 };
		// 七对
		// int cards[] = new int[] { 0x11, 0x11, 0x11, 0x11, 0x14, 0x14, 0x15,
		// 0x15, 0x17, 0x17, 0x19, 0x19, 0x04 };2
		// 王闯王
		// int cards[] = new int[] { 0x06, 0x07, 0x35, 0x13, 0x14, 0x15, 0x13,
		// 0x14, 0x15, 0x17, 0x17, 0x17, 0x36 };
		// int cards[] = new int[]
		// {0x05,0x06,0x06,0x07,0x07,0x34,0x34,0x19,0x019,0x19,0x19,0x23,0x23};

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		// int[] realyCards = new int[] { 34, 19, 25, 8, 9, 6, 4, 3, 24, 41, 20,
		// 35, 2, 7, 1, 18, 17, 7, 34, 8, 9, 6, 41,
		// 35, 21, 2, 9, 21, 1, 2, 38, 34, 5, 39, 40, 21, 39, 33, 18, 38, 23,
		// 38, 37, 3, 33, 19, 24, 20, 22, 4, 39,
		// 3, 7, 5, 8, 18, 35, 36, 22, 2, 1, 22, 24, 23, 40, 35, 17, 4, 25, 36,
		// 19, 8, 5, 41, 6, 33, 20, 24, 40,
		// 38, 40, 25, 17, 6, 17, 34, 4, 20, 37, 37, 7, 37, 36, 25, 3, 41, 23,
		// 33, 39, 36, 21, 18, 9, 1, 22, 23, 5,
		// 19 };
		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 14) {
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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;

		// 分发扑克
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		DEBUG_CARDS_MODE = false; // 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false; // 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end
		// 发牌
		this._handler = this._handler_dispath_card;
		this._handler_dispath_card.reset_status(cur_player, type);
		this._handler.exe(this);

		return true;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		// 查牌数据
		this.setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			if (this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN) || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_XIANG_TAN_DT)) {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				}
			} else {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
				}
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[getTablePlayerNumber()];

			if (this.is_mj_type(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE) || this.is_mj_type(GameConstants.GAME_TYPE_HUNAN_MJ_CD_DT)) {
				if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_HUANG_ZHUANG_HUANG_GANG)
						&& (reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY)) { // 流局并且荒庄荒杠
					for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
						for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
							_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
						}
					}
				} else {
					for (int i = 0; i < getTablePlayerNumber(); i++) {

						for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
							}
						}

						// 记录
						for (int j = 0; j < getTablePlayerNumber(); j++) {
							_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
						}

					}
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}

					// 记录
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}

				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
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
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
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

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		// 错误断言
		return false;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		// _game_status = GameConstants.GS_MJ_WAIT;
		if (is_sys()) {
			// 金币场 防重入
			if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
				return false;

			_game_status = GameConstants.GS_MJ_FREE;
		} else {
			_game_status = GameConstants.GS_MJ_WAIT;
		}
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		clearHasPiao();
		// WalkerGeek 牌局结束清理自建赛上局托管状态
		// if(isClubMatch()){
		// for(int i = 0; i <getTablePlayerNumber();i++){
		// istrustee[i] = false;
		// }
		// }

		return this.on_handler_game_finish(seat_index, reason);

	}

	/**
	 * 吃胡判断
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weave_count
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @param _seat_index
	 * @return
	 */
	public abstract int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index);

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_DADOU)).is_empty()) {
			_player_result.men_qing[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIADOU)).is_empty()) {
			_player_result.hai_di[_seat_index]++;
		}
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {

		if (super.handler_enter_room(player)) {
			check_if_kick_unready_player();
			return true;
		}
		return false;
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

	public boolean check_if_cancel_kick() {
		if (!is_sys())
			return false;

		if (getPlayerCount() == GameConstants.GAME_PLAYER && _kick_schedule != null) {// 人满
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
		if (GameConstants.GS_MJ_XIAOHU != _game_status && GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {//
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}

		if (_player_ready[seat_index] == 1)
			return true;

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);
		if (!is_mj_type(GameConstants.GAME_TYPE_DT_MJ_HUNAN_CHEN_ZHOU) && !is_mj_type(GameConstants.GAME_TYPE_MJ_JING_DE_ZHEN)
				&& !is_mj_type(GameConstants.GAME_TYPE_MJ_NING_XIANG)) {
			// 跑分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
			// 闹庄
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.nao[i] = 0;
			}
		}

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public void clear_open_less() {

		if (ruleMap.get(GameConstants.GAME_RULE_CAN_LESS) != null) {
			for (int i = 0; i < _player_open_less.length; i++) {
				_player_open_less[i] = 0;
			}
		}

	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);
			// WalkerGeek 重连后的托管状态
			be_in_room_trustee_match(seat_index);
		}

		if (GameConstants.GS_MJ_WAIT == _game_status) {
			RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
			tmp_roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_WHEN_GAME_FINISH);

			send_response_to_room(tmp_roomResponse);

			log_warn(" 小局结束的时候，断线重连了！！");
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

		if (is_sys())
			return true;

		return true;
	}

	public void fixBugTemp(int seat_index) {
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
	 * 胡牌后清除按钮
	 */
	public void cleanActionAfterHu() {

		if (is_mj_type(GameConstants.GAME_TYPE_MJ_NEW_HN_ZHEN_ZHOU) || is_mj_type(GameConstants.GAME_TYPE_HENAN_XX)
				|| is_mj_type(GameConstants.GAME_TYPE_NEW_HE_NAN) || is_mj_type(GameConstants.GAME_TYPE_MJ_NEW_HN_ZHEN_ZHOU)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_LYGC) || is_mj_type(GameConstants.GAME_TYPE_NEW_LUO_YANG_GANG_CI)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_LYGC_DIAN_PAO) || is_mj_type(GameConstants.GAME_TYPE_HALL_CHANG_SHA_MJ)
				|| is_mj_type(GameConstants.GAME_TYPE_JD_CHANG_SHA) || is_mj_type(GameConstants.GAME_TYPE_SX_JINGYUE)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_CS_QY)) {
			// WalkerGeek 河南麻将胡牌后的操作按钮消失
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_playerStatus[i].clean_action();
				// table._playerStatus[i].clean_status();
				change_player_status(i, GameConstants.INVALID_VALUE);
				operate_player_action(i, true);
			}
		}

	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
		cleanActionAfterHu();
		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
																																	// 2017/7/10
			int effect_count = chr.type_count;
			long effect_indexs[] = new long[effect_count];
			for (int i = 0; i < effect_count; i++) {
				if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
					effect_indexs[i] = GameConstants.CHR_HU;
				} else {
					effect_indexs[i] = chr.type_list[i];
				}

			}

			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);
		} else {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	public void process_chi_hu_player_operate_reconnect(int seat_index, int operate_card, boolean rm) {
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 胡牌算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public abstract void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo);

	// 获取庄家上家的座位
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	// 获取庄家下家的座位
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	protected int get_null_seat() {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (get_players()[i] == null) {
				return i;
			}
		}
		return -1;
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
			// _player_open_less[seat_index] = 0;
			PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
		}
		return true;
	}

	/**
	 * 解决“玩家发起解散游戏之后，断线了，断线的这段时间，有人拒绝了房间解散或所有人都同意了，之后，玩家重连上了，那个倒计时的界面还在”。
	 * 
	 * @param seat_index
	 */
	@Deprecated
	public void reconnect_when_release_room(int seat_index) {
		if (_release_scheduled == null) {
			int release_players[] = new int[getTablePlayerNumber()];
			Arrays.fill(release_players, 2);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(100);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(seat_index);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime(50);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			send_response_to_room(roomResponse);
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
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();

			if (GameConstants.INVALID_SEAT == seat_index) {
				seat_index = getPlayerIndex(player.getAccount_id());

				if (GameConstants.INVALID_SEAT == seat_index) {
					MongoDBServiceImpl.getInstance().server_error_log(getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(),
							player.getAccount_id(), SysGameTypeDict.getInstance().getGameDescByTypeIndex(getGameTypeIndex()), getGame_id());
				}
			}
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(3007);
		int delay = 150;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
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

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}

			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			// WalkerGeek 允许少人模式
			super.init_less_param();

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		for (int i = 0; i < count; i++) {

			roomResponse.addCardData(cards[i]);
		}

		if (to_player == GameConstants.INVALID_SEAT) {
			// 实时存储牌桌上的数据，方便回放时，任意进度读取
			operate_player_cards_record(seat_index, 2);

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
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

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
	public boolean operate_add_discard(int seat_index, int count, int cards[]) {
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
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
		GRR.add_room_response(roomResponse);

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

	public void record_discard_gang(int seat_index) {
		try {
			boolean gang = _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_GANG)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_DA_CHAO_TIAN)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_MENG_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_HUI_TOU_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_DIAN_XIAO)
					|| _playerStatus[seat_index].has_action_by_code(GameConstants.WIK_XIAO_CHAO_TIAN);
			if (gang) {
				for (int i = 0; i < _playerStatus[seat_index]._weave_count; i++) {
					if ((_playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_GANG
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_DA_CHAO_TIAN
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_MENG_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_HUI_TOU_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_DIAN_XIAO
							|| _playerStatus[seat_index]._action_weaves[i].weave_kind == GameConstants.WIK_XIAO_CHAO_TIAN)) {
						int card = _playerStatus[seat_index]._action_weaves[i].center_card;

						boolean find = false;
						for (int j = 0; j < GRR._discard_count_gang[seat_index]; j++) {
							if (GRR._discard_cards_gang[seat_index][j] == card) {
								find = true;
								break;
							}
						}
						if (!find) {
							GRR._discard_count_gang[seat_index]++;
							GRR._discard_cards_gang[seat_index][GRR._discard_count_gang[seat_index] - 1] = card;
						}

					}
				}
			}
		} catch (Exception e) {
			logger.error("error", e);
		}
	}

	public boolean isIndiscardGang(int seat_index, int card) {
		for (int j = 0; j < GRR._discard_count_gang[seat_index]; j++) {
			if (GRR._discard_cards_gang[seat_index][j] == card) {
				return true;
			}
		}
		return false;
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
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 显示玩家抓牌但是不往GRR里面记录，目前只有湖北黄州晃晃用了
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param to_player
	 * @return
	 */
	public boolean operate_player_get_card_without_record(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2); // get牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_other(seat_index, roomResponse);

			for (int i = 0; i < count; i++) {
				roomResponse.addCardData(cards[i]);
			}
			return this.send_response_to_player(seat_index, roomResponse);
		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
			}
			return this.send_response_to_player(to_player, roomResponse);
		}
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

		if (to_player == GameConstants.INVALID_SEAT) {
			// 实时存储牌桌上的数据，方便回放时，任意进度读取
			operate_player_cards_record(seat_index, 1);

			this.send_response_to_other(seat_index, roomResponse);

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
			}
			// GRR.add_room_response(roomResponse);
			return this.send_response_to_player(to_player, roomResponse);
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

	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);

		// this.load_common_status(roomResponse);
		//
		// //手牌数量
		// roomResponse.setCardCount(card_count);
		// roomResponse.setWeaveCount(weave_count);
		// //组合牌
		// if (weave_count>0) {
		// for (int j = 0; j < weave_count; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
		// weaveItem_item.setPublicCard(weaveitems[j].public_card);
		// weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
		// weaveItem_item.setCenterCard(weaveitems[j].center_card);
		// roomResponse.addWeaveItems(weaveItem_item);
		// }
		// }
		//
		// this.send_response_to_other(seat_index, roomResponse);
		//
		// // 手牌
		// for (int j = 0; j < card_count; j++) {
		// roomResponse.addCardData(cards[j]);
		// }
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @param weave_count
	 * @param weaveitems
	 * @return
	 */
	public boolean operate_player_cards_with_ting_gzcg(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
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

		// 手牌
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

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
			// roomResponse.addOutCardTing(this.GRR._cards_index[seat_index][this._logic.switch_to_card_index(cards[i])]);

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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	// 12更新玩家手牌数据
	// 13更新玩家牌数据(包含吃碰杠组合)
	// public boolean refresh_hand_cards(int seat_index, boolean send_weave) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// if (send_weave == true) {
	// roomResponse.setType(13);// 13更新玩家牌数据(包含吃碰杠组合)
	// } else {
	// roomResponse.setType(12);// 12更新玩家手牌数据
	// }
	// roomResponse.setGameStatus(_game_status);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int hand_cards[] = new int[MJGameConstants.MAX_COUNT];
	// int hand_card_count =
	// _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);
	//
	// //手牌数量
	// roomResponse.setCardCount(hand_card_count);
	//
	// // 手牌
	// for (int j = 0; j < hand_card_count; j++) {
	// roomResponse.addCardData(hand_cards[j]);
	// }
	//
	// //组合牌
	// if (send_weave == true) {
	// // 13更新玩家牌数据(包含吃碰杠组合)
	// for (int j = 0; j < GRR._weave_count[seat_index]; j++) {
	// WeaveItemResponse.Builder weaveItem_item =
	// WeaveItemResponse.newBuilder();
	// weaveItem_item.setProvidePlayer(GRR._weave_items[seat_index][j].provide_player);
	// weaveItem_item.setPublicCard(GRR._weave_items[seat_index][j].public_card);
	// weaveItem_item.setWeaveKind(GRR._weave_items[seat_index][j].weave_kind);
	// weaveItem_item.setCenterCard(GRR._weave_items[seat_index][j].center_card);
	// roomResponse.addWeaveItems(weaveItem_item);
	// }
	// }
	// // 自己才有牌数据
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

	// 14摊开玩家手上的牌
	// public boolean send_show_hand_card(int seat_index, int cards[]) {
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(14);
	// roomResponse.setCardCount(cards.length);
	// roomResponse.setOperatePlayer(seat_index);
	// for (int i = 0; i < cards.length; i++) {
	// roomResponse.addCardData(cards[i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	// return true;
	// }

	// public boolean refresh_discards(int seat_index) {
	// if (seat_index == MJGameConstants.INVALID_SEAT) {
	// return false;
	// }
	// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
	// roomResponse.setType(MsgConstants.RESPONSE_REFRESH_DISCARD);
	// roomResponse.setOperatePlayer(seat_index);
	//
	// int l = GRR._discard_count[seat_index];
	// for (int i = 0; i < l; i++) {
	// roomResponse.addCardData(GRR._discard_cards[seat_index][i]);
	// }
	//
	// this.send_response_to_room(roomResponse);
	//
	// return true;
	// }

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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
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
		tableResponse.setSendCardData(((_send_card_data != GameConstants.INVALID_VALUE) && (_provide_player == seat_index)) ? _send_card_data
				: GameConstants.INVALID_VALUE);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], hand_cards);

		if (_status_send == true) {
			// 牌
			if (seat_index == _current_player) {
				_logic.remove_card_by_data(hand_cards, _send_card_data);
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		this.send_response_to_player(seat_index, roomResponse);

		if (_status_send == true) {
			// 牌
			this.operate_player_get_card(_current_player, 1, new int[] { _send_card_data }, seat_index);
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

	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
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
				} else if (_logic.is_ci_card(_logic.switch_to_card_index(GRR._especial_show_cards[i]))) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_CI);
				} else if (_logic.is_wang_ba_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
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
	 * 玩家来回切换游戏界面和其他界面的时候，虚拟处理玩家的离线信息
	 */
	@Override
	public boolean handler_switch_screen(int type, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data_switch_screen(roomResponse, type, seat_index);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public void load_player_info_data_switch_screen(RoomResponse.Builder roomResponse, int type, int seat_index) {
		if (virtual_online != null)
			virtual_online[seat_index] = !virtual_online[seat_index];

		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			if (virtual_online != null)
				room_player.setOnline(virtual_online[i] ? 1 : 0);
			else
				room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			room_player.setAccountId(rplayer.getAccount_id());
			// room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			// room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
	}

	public void clearHasPiao() {
		// int count = getTablePlayerNumber();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.haspiao[i] = 0;
		}
	}

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_QIANGGANG,
						i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家

			// Random random = new Random();//
			// int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			// _banker_select = rand % MJGameConstants.GAME_PLAYER;//
			// ((lSiceCount>>24)+(lSiceCount>>16)-1)%MJGameConstants.GAME_PLAYER;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	public boolean is_mj(int id) {
		return this.getGame_id() == id;
	}

	public void record_game_round(GameEndResponse.Builder game_end) {

		if (GRR != null) {
			game_end.setRecord(GRR.get_video_record());
			long id = BrandIdDict.getInstance().getId();
			game_end.setBrandId(id);

			GameEndResponse ge = game_end.build();

			byte[] gzipByte = ZipUtil.gZip(ge.toByteArray());

			if (!is_sys()) {
				// 记录 to mangodb
				MongoDBServiceImpl.getInstance().childBrand(_gameRoomRecord.getGame_id(), id, this.get_record_id(), "", null, null, gzipByte,
						this.getRoom_id() + "", _recordRoomRecord == null ? "" : _recordRoomRecord.getBeginArray(), this.getRoom_owner_account_id());
			}

		}
		//
		// if ((cost_dou > 0) && (this._cur_round == 1)) {
		// // 不是正常结束的
		// if ((game_end.getEndType() != GameConstants.Game_End_NORMAL)
		// && (game_end.getEndType() != GameConstants.Game_End_DRAW)) {
		// // 还豆
		// StringBuilder buf = new StringBuilder();
		// buf.append("开局失败[" + game_end.getEndType() + "]" + ":" +
		// this.getRoom_id())
		// .append("game_id:" + this.getGame_id()).append(",game_type_index:" +
		// _game_type_index)
		// .append(",game_round:" + _game_round).append(",房主:" +
		// this.getRoom_owner_account_id())
		// .append(",豆+:" + cost_dou);
		// // 把豆还给玩家
		// AddGoldResultModel result =
		// PlayerServiceImpl.getInstance().addGold(this.getRoom_owner_account_id(),
		// cost_dou, false, buf.toString(), EGoldOperateType.FAILED_ROOM);
		// if (result.isSuccess() == false) {
		// logger.error("房间[" + this.getRoom_id() + "]" + "玩家[" +
		// this.getRoom_owner_account_id() + "]还豆失败");
		// }
		//
		// }
		// }

	}

	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			int wFanShu = 0;
			if (getGame_id() == GameConstants.GAME_ID_HUNAN) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_PENGPENG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjpph, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_JIANGJIANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.jjh, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_LAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidilao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAI_DI_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.cshaidipao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.haohuaqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshanghua, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qiangganghu, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.mjgangshangpao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_QUAN_QIU_REN)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.quanqiuren, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.shaohuaqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshanghua, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_SHUANG_GANG_SHANG_PAO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.smjgangshangpao, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				// 小胡
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_DA_SI_XI)).is_empty())
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_BAN_BAN_HU)).is_empty())// 8000
					wFanShu += 1;
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_LIU_LIU_SHUN)).is_empty())
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.liuliushun, "", _game_type_index, 0l,
							this.getRoom_id());
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUNAN_XIAO_QUE_YI_SE)).is_empty())// 10000
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.qingyise, "", _game_type_index, 0l,
							this.getRoom_id());

			}

			if (getGame_id() == GameConstants.GAME_ID_HUBEI) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_HEI_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.heimo, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RUAN_MO)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.ruanmo, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_ZHUO_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.zhuotong, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HUBEI_RE_CHONG)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.rechong, "", _game_type_index, 0l,
							this.getRoom_id());
				}
			}

			if (getGame_id() == GameConstants.GAME_ID_HENAN) {
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.sihun, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QIANG_GANG_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henangang, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty()
						|| !(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqidui, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henanqiduihaohua, "", _game_type_index, 0l,
							this.getRoom_id());
				}

				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henankaihua, "", _game_type_index, 0l,
							this.getRoom_id());
				}
				if (!(chiHuRight.opr_and(GameConstants.CHR_HENAN_HZ_QISHOU_HU)).is_empty()) {
					MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.henan4hong, "", _game_type_index, 0l,
							this.getRoom_id());
				}

			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 设置结算描述
	 */
	protected abstract void set_result_describe();

	/**
	 * 吃胡操作
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card[], int card_count, boolean rm) {
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
		int cards[] = new int[GameConstants.MAX_INDEX];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int i = 0; i < card_count; i++) {
			cards[hand_card_count++] = operate_card[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	public boolean is_zhuang_xian() {
		return true;
	}

	public int get_piao_lai_bei_shu(int seat_index, int target_seat) {
		int count = 1;
		for (int i = 0; i < GRR._piao_lai_count; i++) {
			if ((GRR._piao_lai_seat[i] == seat_index) || (GRR._piao_lai_seat[i] == target_seat)) {
				count *= 2;
			}
		}
		int hjh = this.hei_jia_hei();
		if ((seat_index == hjh) || (target_seat == hjh)) {
			count *= 2;
		}
		return count;

	}

	public int hei_jia_hei() {
		int same_player = GameConstants.INVALID_SEAT;
		if (GRR._piao_lai_count == 4) {
			same_player = GRR._piao_lai_seat[0];
			for (int i = 0; i < GRR._piao_lai_count; i++) {
				if (GRR._piao_lai_seat[i] != same_player) {
					return GameConstants.INVALID_SEAT;
				}
			}
		}
		return same_player;

	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {
		// 错误断言
		if (card > GameConstants.CARD_ESPECIAL_TYPE_GUI && card < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_GUI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_DING_GUI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_GUI && card < GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_GUI;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN && card < GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO_TING && card < GameConstants.CARD_ESPECIAL_TYPE_HUN) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_HUN && card < GameConstants.CARD_ESPECIAL_TYPE_CI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HUN;
		} else if (card == (GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI + GameConstants.HZ_MAGIC_CARD)) {// 滑水麻将
																																				// 特殊听牌和癞子重合叠加
			card -= (GameConstants.CARD_ESPECIAL_TYPE_TING + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CI && card < GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA && card < GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG && card < GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING && card < GameConstants.CARD_ESPECIAL_TYPE_YAOJIU) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_YAOJIU && card < GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_YAOJIU;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI && card < GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING) { // 窟窿带神神牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO) { // 窟窿带神神牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_SHENGPAI_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_BAO && card < GameConstants.CARD_ESPECIAL_TYPE_JING) { // 瑞金麻将宝牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_BAO;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_JING) { // 山西乡宁金牌
			card -= GameConstants.CARD_ESPECIAL_TYPE_JING;
		}

		return card;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	@Override
	public boolean exe_finish(int reason) {
		this._end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		this.set_handler(this._handler_finish);
		this._handler_finish.exe(this);

		return true;
	}

	/***
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param card_data
	 * @param send_client
	 * @param delay
	 * @return
	 */
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
			this.set_handler(this._handler_dispath_card);
			this._handler_dispath_card.reset_status(seat_index, type);
			this._handler.exe(this);
		}

		return true;
	}

	/**
	 * //执行特殊逻辑发牌 是否延迟 运用于小胡等需要二次走发牌逻辑的代码段
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(int seat_index, int type, int delay, boolean specialType) {

		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this.set_handler(this._handler_dispath_card);
			this._handler_dispath_card.reset_status(seat_index, type, specialType);
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
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean self, boolean d) {
		// 是否有抢杠胡
		this.set_handler(this._handler_gang);
		this._handler_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_suo_gang(int seat_index, int provide_player, int center_card, int operate_card, int action, int type) {
		set_handler(_handler_gang);
		_handler_gang.reset_suo_gang_status(seat_index, provide_player, center_card, operate_card, action, type);
		_handler.exe(this);

		return true;
	}

	/**
	 * 海底
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean exe_hai_di(int start_index, int seat_index) {
		this.set_handler(this._handler_hai_di);
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
		this.set_handler(this._handler_yao_hai_di);
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
		this.set_handler(this._handler_out_card_operate);
		this._handler_out_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type) {
		// 出牌
		this.set_handler(this._handler_chi_peng);
		this._handler_chi_peng.reset_status(seat_index, provider, action, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_jian_pao_hu(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_JIAN_PAO_HU, TimeUnit.MILLISECONDS);

		return true;
	}

	public boolean exe_jian_pao_hu_new(int seat_index, int action, int card) {

		GameSchedule.put(new JianPaoHuRunnable(this.getRoom_id(), seat_index, action, card), GameConstants.DELAY_JIAN_PAO_HU_NEW,
				TimeUnit.MILLISECONDS);

		return true;
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;
		// add end

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
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
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;

	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
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
	 * 调整出牌的处理器，牌打出之后，不管有没有人响应，先加到废牌堆，然后如果有人有人操作，将牌从废牌堆删除
	 * 
	 * @param seat_index
	 * @param card
	 */
	public void remove_discard_after_operate(int seat_index, int card) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}

		if (GRR == null)
			return;

		GRR._discard_cards[seat_index][GRR._discard_count[seat_index]--] = _logic.switch_to_card_index(card);
	}

	/**
	 * 调整出牌的处理器，牌打出之后，不管有没有人响应，先加到废牌堆，然后如果有人有人操作，将牌从废牌堆删除
	 * 
	 * @param seat_index
	 * @param card
	 */
	public void remove_discard_after_operate(int seat_index, int[] cards) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}

		if (GRR == null)
			return;
		for (int card : cards) {
			GRR._discard_cards[seat_index][GRR._discard_count[seat_index]--] = _logic.switch_to_card_index(card);
		}
	}

	/**
	 * 调度,小胡结束
	 **/
	public void runnable_xiao_hu(int seat_index, boolean is_dispatch) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		boolean change_handler = true;
		int cards[] = new int[GameConstants.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 清除动作
			change_player_status(i, GameConstants.INVALID_VALUE);
			if (GRR._start_hu_right[i].is_valid()) {
				if (change_handler && is_dispatch == false && i != _cur_banker) {
					// 刷新自己手牌
					int hand_card_count_zhuang = _logic.switch_to_cards_data(GRR._cards_index[_cur_banker], cards);

					this.operate_player_cards(_cur_banker, hand_card_count_zhuang, cards, 0, null);
				}
				// 刷新自己手牌
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				this.operate_player_cards(i, hand_card_count, cards, 0, null);

				// 去掉 小胡排。 经典长沙麻将，玩家抓牌之后，才立起牌墙
				this.operate_show_card(i, GameConstants.Show_Card_XiaoHU, 0, null, GameConstants.INVALID_SEAT);
				change_handler = false;
			}
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		if (is_dispatch) {

			this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
		} else {
			this.set_handler(this._handler_dispatchCard_xiaohu_cslx);
			this._handler_dispatchCard_xiaohu_cslx.reset_status(seat_index, GameConstants.WIK_NULL);
			this._handler.exe(this);
		}

	}

	/**
	 * 调度,小胡结束 WalkerGeek 优化小胡流程
	 **/
	public void runnable_xiao_hu_new(int seat_index) {
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;

		boolean change_handler = true;
		int cards[] = new int[GameConstants.MAX_COUNT];
		// 庄家删除手牌
		if (seat_index == _cur_banker) {
			GRR._left_card_count++;
			int card = _repertory_card[_all_card_len - GRR._left_card_count];
			GRR._cards_index[seat_index][_logic.switch_to_card_index(card)]--;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 清除动作
			change_player_status(i, GameConstants.INVALID_VALUE);
			if (GRR._start_hu_right[i].is_valid()) {
				if (change_handler && i != _cur_banker) {
					// 刷新自己手牌
					int hand_card_count_zhuang = _logic.switch_to_cards_data(GRR._cards_index[_cur_banker], cards);
					this.operate_player_cards(_cur_banker, hand_card_count_zhuang, cards, 0, null);
				}
				// 刷新自己手牌
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				this.operate_player_cards(i, hand_card_count, cards, 0, null);

				// 去掉 小胡排。 经典长沙麻将，玩家抓牌之后，才立起牌墙
				this.operate_show_card(i, GameConstants.Show_Card_XiaoHU, 0, null, GameConstants.INVALID_SEAT);
				change_handler = false;
			}
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 移除赖根
	 * 
	 * @param seat_index
	 */
	public void runnable_finish_lai_gen(int seat_index) {
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					GRR.mo_lai_count[i] = GRR._cards_index[i][j];// 摸赖次数
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				for (int j = 0; j < hand_card_count; j++) {
					if (_logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		this.exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (GRR._cards_index[i][j] > 0 && _logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}
			if (has_lai_zi) {
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_COUNT];
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
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	protected void shuffle_players() {
		List<Player> pl = new ArrayList<Player>();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

		// 后面抽出来再放到抽象层 -GAME-TODO
		ClubMsgSender.roomPlayerStatusUpdate(ERoomStatus.TABLE_REFRESH, this);
	}

	@Override
	public int getTablePlayerNumber() {
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) {
			return 2;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) {
			return 3;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_FOUR_PLAYER) == 1) {
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_DT_HONG_ZHONG)) {
			if (getRuleValue(GameConstants.GAME_RULE_HUNAN_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_YI_YANG_QIN_YOU)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (getRuleValue(GameConstants_YiYang.GAME_RULE_PEOPLE_COUNT_3) == 1) {
				return 3;
			}
			if (getRuleValue(GameConstants_YiYang.GAME_RULE_PEOPLE_COUNT_2) == 1) {
				return 2;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_PING_XIANG_258)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (getRuleValue(Constants_MJ_PING_XIANG_258.GAME_RULE_PEOPLE_COUNT_3) == 1) {
				return 3;
			}
			if (getRuleValue(Constants_MJ_PING_XIANG_258.GAME_RULE_PEOPLE_COUNT_2) == 1) {
				return 2;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_QUAN_ZHOU)) {
			if (getRuleValue(Constants_MJ_QUAN_ZHOU.GAME_RULE_PEOPLE_COUNT_3) == 1) {
				return 3;
			}
			if (getRuleValue(Constants_MJ_QUAN_ZHOU.GAME_RULE_PEOPLE_COUNT_2) == 1) {
				return 2;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_JING_DE_ZHEN)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (getRuleValue(Constants_MJ_JING_DE_ZHEN.GAME_RULE_PEOPLE_COUNT_3) == 1) {
				return 3;
			}
			if (getRuleValue(Constants_MJ_JING_DE_ZHEN.GAME_RULE_PEOPLE_COUNT_2) == 1) {
				return 2;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_JI_XIAN) || is_mj_type(GameConstants.GAME_TYPE_LIN_FENG)
				|| is_mj_type(GameConstants.GAME_TYPE_LF_YI_MEN_PAI)) {
			if (getRuleValue(UniversalConstants.U_GAME_RULE_PLAYER_TWO) == 1) {
				return 2;
			} else if (getRuleValue(UniversalConstants.U_GAME_RULE_PLAYER_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_ZHENG_ZHOU)) {
			if (getRuleValue(GameConstants.GAME_RULE_HUNAN_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_HUAI_HUA)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (getRuleValue(Constants_HUAI_HUA.GAME_RULE_PEOPLE_COUNT_3) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_NING_XIANG)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_PEOPLE_COUNT_2) == 1) {
				return 2;
			}
			if (getRuleValue(Constants_MJ_NING_XIANG.GAME_RULE_PEOPLE_COUNT_3) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_YUAN_JIANG)) {
			if (playerNumber > 0)
				return playerNumber;
			if (getRuleValue(Constants_YuanJiang.GAME_RULE_PLAYER_TWO) == 1) {
				return 2;
			}
			if (getRuleValue(Constants_YuanJiang.GAME_RULE_PLAYER_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_SRLF)) {
			if (getRuleValue(Constants_SiChuan.GAME_RULE_U_PLAYER_TWO) == 1) {
				return 2;
			}
			return 3;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)
				|| is_mj_type(GameConstants.GAME_TYPE_LU_ZHOU_GUI) || is_mj_type(GameConstants.GAME_TYPE_XUE_LIU_CHENG_HE)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (getRuleValue(Constants_SiChuan.GAME_RULE_U_PLAYER_TWO) == 1) {
				return 2;
			}
			if (getRuleValue(Constants_SiChuan.GAME_RULE_U_PLAYER_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_AN_HUA)) {
			if (getRuleValue(Constants_AnHua.GAME_RULE_PLAYER_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_QIONG_LAI)) {
			if (getRuleValue(Constants_SiChuan.GAME_RULE_PLAYER_TWO) == 1) {
				return 2;
			}
			if (getRuleValue(Constants_SiChuan.GAME_RULE_PLAYER_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_NEW_XU_CHANG)) {
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_TWO_PLAYER) == 1) {
				return 2;
			}
			if (getRuleValue(GameConstants.GAME_RULE_HENAN_THREE) == 1) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_ER_ZHOU) || is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
			if (has_rule(Constants_EZ.GAME_RULE_PLAYER_TWO)) {
				return 2;
			}
			if (has_rule(Constants_EZ.GAME_RULE_PLAYER_THREE)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_GUANG_CHANG)) {
			if (has_rule(Constants_GuangChang.GAME_RULE_PEOPLE_COUNT_2)) {
				return 2;
			}
			if (has_rule(Constants_GuangChang.GAME_RULE_PEOPLE_COUNT_3)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_MJ_WAN_ZAI)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (has_rule(Constants_WanZai.GAME_RULE_PEOPLE_COUNT_2)) {
				return 2;
			}
			if (has_rule(Constants_WanZai.GAME_RULE_PEOPLE_COUNT_3)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HUNAN_YIYANG_SZG) || is_mj_type(GameConstants.GAME_TYPE_THJ_YI_YANG_SGZ)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (has_rule(GameConstants_YiYang_SZG.GAME_RULE_PLAYER_3)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_GX_LIU_ZHOU)) {
			if (has_rule(Constants_GXLZ.GAME_RULE_PLAYER_OF_3)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_JS_YANG_ZHONG)) {
			if (has_rule(Constants_YangZhong.GAME_RULE_PLAYER_THREE)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_JD_CHANG_SHA) || is_mj_type(GameConstants.GAME_TYPE_THJ_JD_CS)
				|| is_mj_type(GameConstants.GAME_TYPE_MJ_DT_SHA_MA) || is_mj_type(GameConstants.GAME_TYPE_MJ_CS_QY)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (has_rule(Constants_JingDian_CS.GAME_RULE_PLAYER_3)) {
				return 3;
			}
			return 4;
		}
		if (is_mj_type(GameConstants.GAME_TYPE_HUNAN_TAO_JIANG)) {
			if (playerNumber > 0) {
				return playerNumber;
			}
			if (has_rule(Constants_TaoJiang.GAME_RULE_PLAYER_FOUR)) {
				return 4;
			}
			if (has_rule(Constants_TaoJiang.GAME_RULE_PLAYER_THREE)) {
				return 3;
			}
			if (has_rule(Constants_TaoJiang.GAME_RULE_PLAYER_TWO)) {
				return 2;
			}
			return 4;
		}
		return mjType.getPlayerCount();
	}

	public void be_in_room_trustee(int get_seat_index) {
		if (!is_sys())
			return;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		istrustee[get_seat_index] = false;
		roomResponse.setIstrustee(isTrutess(get_seat_index));
		this.send_response_to_other(get_seat_index, roomResponse);
	}

	/**
	 * 通知玩家刷新托管协议
	 * 
	 * @param seat_index
	 * @param isTrustee
	 */
	public void be_in_room_trustee_match(int seat_index) {
		if (!is_match() && !isCoinRoom() && !isClubMatch())
			return;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (!istrustee[i]) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
			roomResponse.setOperatePlayer(i);
			roomResponse.setIstrustee(istrustee[i]);

			send_response_to_player(seat_index, roomResponse);
		}
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > GameConstants.GAME_PLAYER || !is_sys())
			return false;
		return istrustee[seat_index];
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}
		if (!is_sys() && !is_match() && !isCoinRoom() && !isClubMatch())// 麻将普通场
																		// 不需要托管
			return false;

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (is_match() || isCoinRoom() || isClubMatch()) {
			if (istrustee[get_seat_index] && this._current_player == get_seat_index) {
				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
					int card = MJAIGameLogic.get_card(this, get_seat_index);
					if (card != 0) {
						this.handler_player_out_card(get_seat_index, card);
					}
				} else if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
				}
			} else if (istrustee[get_seat_index]) {
				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
				}
			}
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return true;

	}

	public int set_ding_niao_valid(int card_data, boolean val) {
		// 先把值还原
		if (val) {
			if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_INVALID;
			} else if (card_data > GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.DING_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID : card_data);
		}
	}

	public int set_fei_niao_valid(int card_data, boolean val) {
		if (val) {
			if (card_data > GameConstants.FEI_NIAO_INVALID && card_data < GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_INVALID;
			} else if (card_data > GameConstants.FEI_NIAO_VALID) {
				card_data -= GameConstants.FEI_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.FEI_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.FEI_NIAO_VALID ? card_data + GameConstants.FEI_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.FEI_NIAO_INVALID ? card_data + GameConstants.FEI_NIAO_INVALID : card_data);
		}

	}

	public void load_out_card_ting(int seat_index, RoomResponse.Builder roomResponse, TableResponse.Builder tableResponse) {
		int ting_count = _playerStatus[seat_index]._hu_out_card_count;
		if (ting_count <= 0)
			return;

		int l = tableResponse.getCardsDataCount();
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < ting_count; j++) {
				if (tableResponse.getCardsData(i) == _playerStatus[seat_index]._hu_out_card_ting[j]) {
					tableResponse.setCardsData(i, tableResponse.getCardsData(i) + GameConstants.CARD_ESPECIAL_TYPE_TING);
				}
			}
		}
		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(_playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}
	}

	public boolean send_error_notify(Player player, int type, String msg, int time) {
		MsgAllResponse.Builder e = MsgAllResponse.newBuilder();
		e.setType(type);
		e.setMsg(msg);
		e.setTime(time);
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.MSG);
		responseBuilder.setExtension(Protocol.msgAllResponse, e.build());
		PlayerServiceImpl.getInstance().send(player, responseBuilder.build());

		return false;

	}

	/*
	 * public void change_handler(MJHandler dest_handler) { _handler =
	 * dest_handler; }
	 */
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		// 如果是出牌或者操作状态 需要倒计时托管&& !istrustee[seat_index]
		// WalkerGeek AI介入不进行其他定时器
		if (!is_match() && !isClubMatch() && !isCoinRoom()) {
			if (is_sys() && (st == GameConstants.Player_Status_OPR_CARD || st == GameConstants.Player_Status_OUT_CARD)) {
				_trustee_schedule[seat_index] = GameSchedule.put(new TuoGuanRunnable(getRoom_id(), seat_index),
						GameConstants.TRUSTEE_TIME_OUT_SECONDS, TimeUnit.SECONDS);
			}
		}
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
			if (this.get_players()[i] == null) {
				continue;
			}
			score = (int) (scores[i] * beilv);
			// 逻辑处理
			this.get_players()[i].setMoney(this.get_players()[i].getMoney() + score);
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
		// playerNumber = 0;
	}

	/**
	 * 三人场麻将获取飞鸟位置
	 * 
	 * @param nValue
	 * @param banker_seat
	 * @return
	 */
	public int get_zhong_seat_by_value_three(int nValue, int banker_seat) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (banker_seat + (nValue - 1) % 4) % 4;
		} else {// 3人场//这里这么特殊处理是因为 算鸟是根据玩家逻辑位置,只有庄家上家,庄家下家,不存在对家
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:// 本家//159
				seat = banker_seat;
				break;
			case 1:// 26//下家
				seat = get_banker_next_seat(banker_seat);
				break;
			case 2:// 37//对家
				seat = get_null_seat();
				break;
			default:// 48//上家
				seat = get_banker_pre_seat(banker_seat);
				break;
			}
		}
		return seat;
	}

	public int get_seat_by_value_ZZ(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4 || getTablePlayerNumber() == 2) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_banker_next_seat(seat_index);
				break;
			case 2:
				seat = get_null_seat();
				break;
			case 3:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	public int get_seat_by_value(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_banker_next_seat(seat_index);
				break;
			case 2:
				seat = get_null_seat();
				break;
			case 3:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_null_seat_for_two_player(seat_index);
				break;
			case 2:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				seat = get_null_seat_for_two_player((seat_index + 1) / getTablePlayerNumber());
				break;
			}
		}
		return seat;
	}

	/**
	 * 二人场以seat_index为视角的下一个空座位
	 * 
	 * @param seat_index
	 *            当前座位
	 * @return
	 */
	protected int get_null_seat_for_two_player(int seat_index) {
		if (seat_index == 0) {
			// 等于0时返回2
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (get_players()[i] == null) {
					return i;
				}
			}
		}

		if (seat_index == 1) {
			// 等于1时返回3
			for (int i = GameConstants.GAME_PLAYER - 1; i >= 0; i--) {
				if (get_players()[i] == null) {
					return i;
				}
			}
		}

		return -1;
	}

	public void rand_tuozi(int seat_index) {

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
					// _player_open_less[i] = 0;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void set_handler(AbstractMJHandler _handler) {
		this._handler = _handler;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
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

	public int getMaxActionPlayerIndex() {
		int maxPlayer = -1;
		int maxAction = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_playerStatus == null) {
				break;
			}
			PlayerStatus playerStatus = _playerStatus[i];
			if (playerStatus.has_action()) {
				if (_playerStatus[i].get_max_operate() > maxAction) {// 协助解决客户端卡顿问题--暂时只处理碰吃，
																		// 胡跟位置有关未做
					maxAction = _playerStatus[i].get_max_operate();
					maxPlayer = i;
				}
			}
		}
		return maxPlayer;

	}

	/**
	 * 解决回放时、查牌时，头像旁边的跑分显示
	 * 
	 * @param game_end
	 */
	public void setGameEndBasicPrama(GameEndResponse.Builder game_end) {
		// 回放时、查牌时，头像旁边的跑分显示
		MJ_Game_End_Basic.Builder basic_game_end = MJ_Game_End_Basic.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			basic_game_end.addPao(_player_result.pao[i]);
			basic_game_end.addNao(_player_result.nao[i]);
			basic_game_end.addQiang(_player_result.qiang[i]);
			basic_game_end.addBianYan(_player_result.biaoyan[i]);
		}
		game_end.setCommResponse(PBUtil.toByteString(basic_game_end));
	}

	/**
	 * 默认的处理听牌数据的方法，没考虑癞子玩法。有需要的自己覆盖一份。
	 * 
	 * @param cards
	 *            保存的听牌数据，即打出哪些牌能听牌
	 * @param cards_index
	 *            手牌索引数据
	 * @param weaveItem
	 *            落地组合数据
	 * @param cbWeaveCount
	 *            落地组合数目
	 * @param seat_index
	 *            座位号
	 * @param has_feng
	 *            是否带风牌，默认万条筒108张，带风136张
	 * @return
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, boolean has_feng) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		if (has_feng) {
			max_ting_count = GameConstants.MAX_ZI_FENG;
		}

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 用86号协议，存储所有的玩家手牌和落地牌数据，并存入回放中
	 * 
	 * @return
	 */
	public boolean operate_player_cards_record(int seat_index, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_UPDATE_USER_DATA_RECORD);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 手牌
			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);

			if (type == 1) {
				if (seat_index == i) {
					_logic.remove_card_by_data(hand_cards, _provide_card);
				}
			}

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				tableResponse.addCardsData(hand_cards[j]);
			}

			// 组合
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GRR._weave_count[i]; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._discard_count[i]; j++) {
				int_array.addItem(GRR._discard_cards[i][j]);
			}

			tableResponse.addDiscardCards(int_array);
		}

		roomResponse.setTable(tableResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 后期需要添加的摇骰子的效果
	 */
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], time_for_tou_zi_animation, time_for_tou_zi_fade);
	}

	/**
	 * 在牌桌上显示摇骰子的效果
	 * 
	 * @param tou_zi_one
	 *            骰子1的点数
	 * @param tou_zi_two
	 *            骰子2的点数
	 * @param time_for_animate
	 *            动画时间
	 * @param time_for_fade
	 *            动画保留时间
	 * @return
	 */
	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		if (GRR != null)
			roomResponse.setTarget(GRR._banker_player);
		else
			roomResponse.setTarget(0);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(tou_zi_one);
		roomResponse.addEffectsIndex(tou_zi_two);
		roomResponse.setEffectTime(time_for_animate);
		roomResponse.setStandTime(time_for_fade);

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean operate_player_get_card_gang(int seat_index, int count, int cards[], int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(5); // 3D，杠之后的发牌，需要从尾牌堆发牌
		roomResponse.setCardCount(count);

		if (to_player == GameConstants.INVALID_SEAT) {
			// 实时存储牌桌上的数据，方便回放时，任意进度读取
			operate_player_cards_record(seat_index, 1);

			send_response_to_other(seat_index, roomResponse);

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
			}

			return send_response_to_player(to_player, roomResponse);
		}
	}

	/**
	 * 当前吃碰玩家
	 * 
	 * @param seat_index
	 */
	public void chi_peng_index(int seat_index) {
		chi_peng_index = seat_index;
	}

	public void chi_peng_index_invaild() {
		chi_peng_index = GameConstants.INVALID_SEAT;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// WalkerGeek Auto-generated method stub
		return false;
	}
}
