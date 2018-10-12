package com.cai.game.mj.handler.shanxill;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.BrandLogModel;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AnimationRunnable;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_LVlIANG extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	public int _difen;
	BrandLogModel _recordRoomRecord;
	public int gang_shang_pao_score[];
	public int an_gang_laizi_score[];// 癞子暗杆分
	public int ming_gang_laizi_score[];// 癞子明杆分
	public int[] tou_zi_dian_shu = new int[4];
	public int time_for_animation = 2000; // 摇骰子的动画时间(ms)
	public int time_for_fade = 500; // 摇骰子动画的消散延时(ms)

	public int iHuangZhuangDun = 6;//

	public int iBaoPaiPlayer = GameConstants.INVALID_SEAT;// 包杠包胡的玩家
	public int iHuPaiPlayer = GameConstants.INVALID_SEAT;// 胡牌玩家
	public int weibaotingfangpao = GameConstants.INVALID_SEAT;// 为报听放炮的玩家
	protected boolean bBaiFenPoChan = false;
	protected boolean[] bHavePoChan = new boolean[4];

	// public final DianGangInfo strDianGangInfo[];

	public int _cur_game_timer; // 当前游戏定时器
	public int _cur_operate_time; // 可操作时间
	public int _operate_start_time; // 操作开始时间
	protected long _request_release_time;
	protected ScheduledFuture<?> _release_scheduled;
	protected ScheduledFuture<?> _table_scheduled;
	protected ScheduledFuture<?> _game_scheduled;

	protected MJHandlerOutCardBaoTing_ll _handler_out_card_bao_ting; // 报听

	protected static final int ID_YAO_SE_ZI = 1;// 开始到发牌
	protected static final int ID_SHOW_CARD = 2;// 显示癞子
	protected static final int ID_AUTO_ZIMO_HU = 3;// 自动自摸胡牌
	protected static final int ID_AUTO_FANGPAO_HU = 4;// 自动放炮胡牌
	protected static final int ID_AUTO_QIANGGANG_HU = 5;// 自动抢杠胡
	protected static final int HAOZIINDEX = 12;// 耗子索引
	protected static final int AUTO_HUPAI_TIMER = 10;// 自动胡牌的时间

	public int autozimoplyer = GameConstants.INVALID_SEAT;// 自动胡牌的玩家
	public int autocard = GameConstants.INVALID_SEAT;// 自动胡的牌
	public boolean has_rule_pochan;//破产规则
	
	protected boolean bBaoTingCard = false;// 是否是报听的牌
	protected boolean bAutoZiMoHuPai = false;// 自动自摸胡牌
	protected boolean bAutoHuPai[] = new boolean[4];// 放炮自动胡牌
	protected boolean bAutoQiangGangHu[] = new boolean[4];// 抢杠自动胡牌


	public MJTable_LVlIANG() {
		super(MJType.GAME_TYPE_HENANPY);
	}

	@Override
	protected void onInitTable() {
		_handler_gang = new MJHandlerGang_ShanXi_ll();
		_handler_chi_peng = new MJHandlerChiPeng_ShanXi_ll();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_ll();
		_handler_dispath_card = new MJHandlerDispatchCard_ShanXi_ll();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ShanXi_ll();
		gang_shang_pao_score = new int[this.getTablePlayerNumber()];
		_difen = 1;
		Arrays.fill(gang_shang_pao_score, 0);
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean set_timer(int timer_type, int time) {
		_cur_game_timer = timer_type;
		if (_game_scheduled != null)
			this.kill_timer();
		if (time == 0) {
			return true;
		}
		_game_scheduled = GameSchedule.put(new AnimationRunnable(getRoom_id(), timer_type), time * 1000, TimeUnit.MILLISECONDS);
		_operate_start_time = (int) (System.currentTimeMillis() / 1000L);
		this._cur_operate_time = time;
		return true;
	}

	public void kill_timer() {
		if (_game_scheduled != null) {
			_game_scheduled.cancel(false);
			_game_scheduled = null;
		}
	}

	public boolean animation_timer(int timer_id) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_SHOW_CARD: {
			operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
			break;
		}
		case ID_AUTO_ZIMO_HU: {
			if (bAutoZiMoHuPai && autozimoplyer != GameConstants.INVALID_SEAT && autocard != GameConstants.INVALID_SEAT) {

				this._handler_dispath_card.handler_operate_card(this, autozimoplyer, GameConstants.WIK_ZI_MO, this.autocard);
				break;
			}
		}
		case ID_AUTO_FANGPAO_HU: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.bAutoHuPai[i] && autocard != GameConstants.INVALID_SEAT) {
					this._handler_out_card_operate.handler_operate_card(this, i, GameConstants.WIK_CHI_HU, this.autocard);
				}
			}
			break;
		}
		case ID_AUTO_QIANGGANG_HU: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.bAutoQiangGangHu[i] && autocard != GameConstants.INVALID_SEAT) {
					this._handler_gang.handler_operate_card(this, i, GameConstants.WIK_CHI_HU, this.autocard);
				}
			}

			break;
		}
		}
		return false;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (is_sys()) {
			Random random = new Random();
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		iBaoPaiPlayer = GameConstants.INVALID_SEAT;// 包杠包胡的玩家
		iHuPaiPlayer = GameConstants.INVALID_SEAT;// 胡牌玩家
		weibaotingfangpao = GameConstants.INVALID_SEAT;
		an_gang_laizi_score = new int[this.getTablePlayerNumber()];
		ming_gang_laizi_score = new int[this.getTablePlayerNumber()];
		Arrays.fill(an_gang_laizi_score, 0);
		Arrays.fill(ming_gang_laizi_score, 0);
		has_rule_pochan = this.has_rule(GameConstants.GAME_RULE_DA_GUO_ZI);
		for (int i = 0; i < 4; i++) {
			bHavePoChan[i] = false;
			tou_zi_dian_shu[i] = 0;
			bAutoHuPai[i] = false;
			bAutoQiangGangHu[i] = false;
		}
		reset_init_data();
		this.progress_banker_select();
		show_tou_zi();
		CanStart();

		return true;
	}

	protected boolean CanStart() {

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;

		this.init_shuffle();
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

		return on_game_start();
	}

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

		// show_tou_zi(GRR._banker_player);

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

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	public boolean show_tou_zi() {

		tou_zi_dian_shu[0] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[1] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[2] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[3] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		if (this._cur_round == 1) {
			GRR._banker_player = _cur_banker = (tou_zi_dian_shu[0] + tou_zi_dian_shu[1]) % getTablePlayerNumber();
		}
		return operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1], tou_zi_dian_shu[2], tou_zi_dian_shu[3], time_for_animation,
				time_for_fade);
	}

	public boolean operate_tou_zi_effect(int tou_zi_one, int tou_zi_two, int tou_zi_three, int tou_zi_four, int time_for_animate, int time_for_fade) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		if (GRR._banker_player != GameConstants.INVALID_SEAT)
			roomResponse.setTarget(GRR._banker_player);
		else
			roomResponse.setTarget(0);
		if (this._cur_round == 1) {
			roomResponse.setEffectCount(4);
			roomResponse.addEffectsIndex(tou_zi_one);
			roomResponse.addEffectsIndex(tou_zi_two);
			roomResponse.addEffectsIndex(tou_zi_three);
			roomResponse.addEffectsIndex(tou_zi_four);
			roomResponse.setEffectTime(time_for_animate);
			roomResponse.setStandTime(time_for_fade);
		} else {
			roomResponse.setEffectCount(2);
			roomResponse.addEffectsIndex(tou_zi_one);
			roomResponse.addEffectsIndex(tou_zi_two);
			roomResponse.setEffectTime(time_for_animate);
			roomResponse.setStandTime(time_for_fade);
		}

		send_response_to_room(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		return true;
	}

	protected void test_cards() {
		int cards[] = new int[] { 
				0x06, 0x06, 0x06, 0x06, 0x13, 0x14, 0x15, 0x17, 0x19, 0x27, 0x28, 0x22, 0x36,
				0x11, 0x06, 0x12, 0x12, 0x13, 0x13, 0x14, 0x15, 0x16, 0x19, 0x21, 0x33, 0x33, 
				0x21, 0x21, 0x22, 0x22, 0x23, 0x23, 0x26, 0x27, 0x31, 0x32, 0x33, 0x33, 0x34, 
				0x01, 0x06, 0x11, 0x12, 0x03, 0x13, 0x21, 0x22, 0x23, 0x32, 0x33, 0x33, 0x34};

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[13 * i + j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

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

	@Override
	protected boolean on_game_start() {

		// 游戏开始
		_logic.clean_magic_cards();

		// 设耗子
		if (has_rule(GameConstants.GAME_RULE_DAN_HAO) || has_rule(GameConstants.GAME_RULE_SHUANG_HAO)) {
			SetMagiCards();
		}

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);

			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;

	}

	public boolean SetMagiCards() {
		int icount = 0;
		int cards[] = new int[2];
		_logic.clean_magic_cards();
		if (has_rule(GameConstants.GAME_RULE_DAN_HAO)) {
			icount = 1;
			cards[0] = _repertory_card[_all_card_len - HAOZIINDEX];

			if (DEBUG_MAGIC_CARD) {
				// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
				cards[0] = magic_card_decidor;
			}
			if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
				cards[0] = 0x32;
			}

			int iNextCard = GetNextCard(cards[0]);
			_logic.add_magic_card_index(_logic.switch_to_card_index(iNextCard));
			GRR._especial_card_count = 1;
			GRR._especial_show_cards[0] = cards[0];

		} else if (has_rule(GameConstants.GAME_RULE_SHUANG_HAO)) {
			icount = 2;
			cards[0] = _repertory_card[_all_card_len - HAOZIINDEX];

			if (DEBUG_MAGIC_CARD) {
				// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
				cards[0] = magic_card_decidor;
			}
			if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
				cards[0] = 0x07;
			}

			cards[1] = GetNextCard(cards[0]);
			_logic.add_magic_card_index(_logic.switch_to_card_index(cards[0]));
			_logic.add_magic_card_index(_logic.switch_to_card_index(cards[1]));
			GRR._especial_card_count = 2;
			GRR._especial_show_cards[0] = cards[0];
			GRR._especial_show_cards[1] = cards[1];
		}
		operate_show_card(this.GRR._banker_player, GameConstants.Show_Card_Center, icount, cards, GameConstants.INVALID_SEAT);
		set_timer(ID_SHOW_CARD, 1);
		return true;
	}

	public int GetNextCard(int iCard) {
		int cur_value = _logic.get_card_value(iCard);
		int cur_color = _logic.get_card_color(iCard);
		int iNextCard = 0;
		int itemp = 0;
		if (cur_color < 3) {
			if (cur_value < 9) {
				itemp = cur_value + 1;
			} else {
				itemp = 1;
			}
		} else {
			if (cur_value == 4) {
				itemp = 1;
			} else if (cur_value == 7) {
				itemp = 5;
			} else {
				itemp = cur_value + 1;
			}
		}
		iNextCard = (cur_color << 4) + itemp;
		return iNextCard;
	}

	/***
	 * 吕梁麻将胡牌解析
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_shanxi_ll(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}
		
		//打出的癞子，不能胡，自摸能胡癞子
		if((card_type == GameConstants.HU_CARD_TYPE_PAOHU || card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) 
				&& this._logic.is_magic_card(cur_card)){
			return GameConstants.WIK_NULL;
		}

		// 1、2、不能胡牌
		int iCardValue = _logic.get_card_value(cur_card);
		int iCardColor = _logic.get_card_color(cur_card);
		iCardValue = iCardColor > 2 ? 10 : iCardValue;
		if (_logic.is_magic_card(cur_card)) {
			iCardValue = 10;
		}
		if (iCardValue < 3) {
			return GameConstants.WIK_NULL;
		}

		// 3、4、5、只能自摸
		if (iCardValue < 6 && (card_type == GameConstants.HU_CARD_TYPE_PAOHU || card_type == GameConstants.HU_CARD_TYPE_QIANGGANG
				|| card_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI)) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		long qxd = _logic.is_qi_xiao_dui_ll(cbCardIndexTemp, weaveItems, weaveCount);
		if (has_rule(GameConstants.GAME_RULE_DAN_HAO) || has_rule(GameConstants.GAME_RULE_SHUANG_HAO))
			qxd = GameConstants.WIK_NULL;

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
		} else if (card_type == GameConstants.HU_CARD_TYPE_BU_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI);
		} else if (card_type == GameConstants.HU_CARD_TYPE_AN_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI);
		}

		if (qxd != GameConstants.WIK_NULL /*&& has_rule(GameConstants.GAME_RULE_TE_SHU_HU)*/) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(qxd);// 都是七小对
			return cbChiHuKind;
		}
		// 设置变量

		/////////////////////////////// 旧方法胡牌判断///////////////////////////////////////
		// List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems,
		/////////////////////////////// weaveCount, analyseItemArray, true);
		/////////////////////////////// 旧方法胡牌判断///////////////////////////////////////

		/////////////////////////////// 新方法胡牌判断///////////////////////////////////////
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);
		/////////////////////////////// 新方法胡牌判断///////////////////////////////////////

		if (bValue) {
			boolean bDaHu = false;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			}
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			if (!has_rule(GameConstants.GAME_RULE_DAN_HAO) && !has_rule(GameConstants.GAME_RULE_SHUANG_HAO)) {
				if (_logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weaveCount, cur_card) && has_rule(GameConstants.GAME_RULE_TE_SHU_HU)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
					bDaHu = true;
				}
				if (_logic.is_yi_tiao_long(cbCardIndexTemp, weaveCount)) {
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_YI_TIAO_LONG);
					bDaHu = true;
				}
			}
			if (card_type == GameConstants.HU_CARD_TYPE_PAOHU && !bDaHu) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
		}
		return cbChiHuKind;
	}

	/***
	 * 吕梁麻将分析当前牌不当癞子的胡牌判断
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_not_lai(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type, boolean lastcardlaizi) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 1、2、不能胡牌
		int iCardValue = _logic.get_card_value(cur_card);
		int iCardColor = _logic.get_card_color(cur_card);
		iCardValue = iCardColor > 2 ? 10 : iCardValue;
		if (_logic.is_magic_card(cur_card)) {
			iCardValue = 10;
		}
		if (iCardValue < 3) {
			return GameConstants.WIK_NULL;
		}

		/*
		 * // 3、4、5、只能自摸 if (iCardValue < 6 && (card_type ==
		 * GameConstants.HU_CARD_TYPE_PAOHU || card_type ==
		 * GameConstants.HU_CARD_TYPE_QIANGGANG || card_type ==
		 * GameConstants.HU_CARD_TYPE_BU_GANG_KAI)) { return
		 * GameConstants.WIK_NULL; }
		 */

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		long qxd = _logic.is_qi_xiao_dui_ll(cbCardIndexTemp, weaveItems, weaveCount);
		if (has_rule(GameConstants.GAME_RULE_DAN_HAO) || has_rule(GameConstants.GAME_RULE_SHUANG_HAO))
			qxd = GameConstants.WIK_NULL;

		/////////////////////////////// 新方法胡牌判断///////////////////////////////////////
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = false;
		if (lastcardlaizi) {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index_shanxill(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
		} else {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
		}

		/////////////////////////////// 新方法胡牌判断///////////////////////////////////////

		if (bValue || qxd != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;

		}
		return cbChiHuKind;
	}

	/**
	 * 三门峡麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (cbCardIndexTemp[_logic.switch_to_card_index(cbCurrentCard)] == 4) {
				continue;
			}
			if (_logic.is_magic_index(i)) {
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_not_lai(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, true)) {
					cards[count] = cbCurrentCard;
					count++;
				}
			} else {
				// if (GameConstants.WIK_CHI_HU ==
				// analyse_chi_hu_card_shanxi_ll(cbCardIndexTemp, weaveItem,
				// cbWeaveCount, cbCurrentCard, chr,
				// GameConstants.HU_CARD_TYPE_PAOHU)) {
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_not_lai(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, false)) {

					cards[count] = cbCurrentCard;
					count++;
				}
			}
		}

		if (count > 0) {
			for (int j = 0; j < _logic.get_magic_card_count(); j++) {
				cards[count++] = _logic.switch_to_card_data(_logic.get_magic_card_index(j)) + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		if (count >= GameConstants.MAX_ZI_FENG) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_gang_respond_ll(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		boolean bHaveHu = false;
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断,单吊耗子不能抢杠胡
			if (playerStatus.is_chi_hu_round() && _playerStatus[i].is_bao_ting() && _playerStatus[i]._hu_card_count < 25) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_shanxi_ll(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					// chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;

					// 添加抢杠胡
					bHaveHu = true;
					this.autocard = card;
					this.bAutoQiangGangHu[i] = true;
				}
			}
		}
		if (bHaveHu) {
			this.set_timer(ID_AUTO_QIANGGANG_HU, AUTO_HUPAI_TIMER);
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	// 吕梁麻将
	public boolean estimate_player_out_card_respond_shanxi_ll(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有
		
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = 0;
		int gang_total_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			gang_total_count += GRR._gang_score[seat_index].gang_count;
		}
		llcard = 12 + gang_total_count * 2;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		
		//if (_logic.is_magic_card(card)) {
		//	return false;
		//}

		boolean bHaveHu = false;
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			
			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			//// 碰牌判断
			if(can_peng){
				action = _logic.check_peng(GRR._cards_index[i], card);
			}
			
			if (action != 0 && !_playerStatus[i].is_bao_ting()) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}
			if (GRR._left_card_count > llcard) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (_playerStatus[i].is_bao_ting()) {
						int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							cbCardIndexTemp[j] = GRR._cards_index[i][j];
						}
						cbCardIndexTemp[_logic.switch_to_card_index(card)] = 0;
						int ting_cards[] = _playerStatus[i]._hu_cards;
						int ting_count = get_ting_card(ting_cards, cbCardIndexTemp, GRR._weave_items[i], GRR._weave_count[i] + 1);
						if (ting_count > 0) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);// 加上杠
							bAroseAction = true;
						}
					} else {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1);// 加上杠
						bAroseAction = true;
					}

				}
			}

			// 判断胡牌 , 打出的癞子不能胡
			if (_playerStatus[i]._hu_card_count > 0 && _playerStatus[i].is_bao_ting() == true && !_logic.is_magic_card(card)) {
				// 可以胡的情况 判断,单吊耗子不能炮胡
				if (_playerStatus[i].is_chi_hu_round() && _playerStatus[i]._hu_card_count < 25) {
					// 吃胡判断
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					action = analyse_chi_hu_card_shanxi_ll(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							GameConstants.HU_CARD_TYPE_PAOHU);

					// 结果判断
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
						bAroseAction = true;
						bHaveHu = true;
						// 添加到自动胡牌
						this.autocard = card;
						this.bAutoHuPai[i] = true;

					}
				}
			}
		}
		if (bHaveHu) {
			this.set_timer(ID_AUTO_FANGPAO_HU, AUTO_HUPAI_TIMER);
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

	protected int GetNextHuPaiPlayer(int seat_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int iNextPlayer = (seat_index + i) % getTablePlayerNumber();
			if (!GRR._chi_hu_rights[iNextPlayer].is_valid()) {
				continue;
			}
			return iNextPlayer;
		}
		return seat_index;
	}

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		// 荒庄不荒杠结束时算杠分
		// 杠牌，每个人的分数
		if (GRR != null) {
			if(!this.has_rule_pochan){
				if (reason == GameConstants.Game_End_NORMAL) {
					float lGangScore[] = new float[getTablePlayerNumber()];
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
							for (int k = 0; k < getTablePlayerNumber(); k++) {
								lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
							}
						}
						lGangScore[i] += an_gang_laizi_score[i] + ming_gang_laizi_score[i];
					}

					for (int i = 0; i < getTablePlayerNumber(); i++) {
						GRR._game_score[i] += lGangScore[i];
						_player_result.game_score[i] += GRR._game_score[i];
					}
				}
				// 流局算耗子杠的分
				if (reason == GameConstants.Game_End_DRAW) {
					for (int player = 0; player < getTablePlayerNumber(); player++) {
						GRR._game_score[player] = an_gang_laizi_score[player] + ming_gang_laizi_score[player];
						_player_result.game_score[player] += GRR._game_score[player];
					}
				}
			}else{
				if (reason == GameConstants.Game_End_NORMAL) {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						_player_result.game_score[i] += GRR._game_score[i];
					}
				}
				if (reason == GameConstants.Game_End_DRAW) {
					int laizi_gang[] = new int[getTablePlayerNumber()];
					for(int i = 0;i < getTablePlayerNumber();i++){
						laizi_gang[i] = 0;
					}
					this.process_liuju_score_pochan(laizi_gang);
					for(int i = 0;i < getTablePlayerNumber();i++){
						_player_result.game_score[i] += laizi_gang[i];
					}
				}
			}

		}

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
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			if (has_rule(GameConstants.GAME_RULE_DAN_HAO)) {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					int iNextCard = GetNextCard(GRR._especial_show_cards[i]);
					game_end.addEspecialShowCards(iNextCard);
				}
			} else {
				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
				}
			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(_difen);

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
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j]))
						hc.addItem(GRR._chi_hu_card[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					else
						hc.addItem(GRR._chi_hu_card[i][j]);
				}
				if (_logic.is_magic_card(GRR._chi_hu_card[i][0]))
					game_end.addHuCardData(GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				else
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
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else
						cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					if (GRR._weave_items[i][j].public_card == 2)
						GRR._weave_items[i][j].public_card = 0;
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
				game_end.addGangScore(0);// 杠牌得分
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
			if ((_cur_round >= _game_round && (!is_sys())) || (bBaiFenPoChan)) {// 局数到了
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
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

	public boolean PoChan() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_result.game_score[i] <= -100) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 吕梁麻将算分，无破产算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_ll(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);

		int iCardColor = _logic.get_card_color(operate_card);
		int iCardValue = _logic.get_card_value(operate_card);
		iCardValue = iCardColor > 2 ? 10 : iCardValue;

		// 如果胡的牌是癞子，按照最大的点数来算
		if (_logic.is_magic_card(operate_card)) {
			iCardValue = get_max_value(seat_index);
		}
		// 如果是单吊耗子按10点算
		if (_playerStatus[seat_index]._hu_card_count >= 25) {
			iCardValue = 10;
		}

		if (has_rule(GameConstants.GAME_RULE_TE_SHU_HU)) {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				iCardValue *= 2;
			} else if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				iCardValue *= 2;
			} else if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
				iCardValue *= 2;
			} else if (!(chr.opr_and(GameConstants.CHR_HUNAN_YI_TIAO_LONG)).is_empty()) {
				iCardValue *= 2;
			}
		}
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			boolean zimo_fan = false;
			float lChiHuScore = 1;
			if ((chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()){
				zimo_fan = true;
				lChiHuScore = 2;
			}
				
			lChiHuScore *= iCardValue;
			//lChiHuScore *= _difen;
			if (has_rule(GameConstants.GAME_RULE_DAI_ZHUANHG)) {
				if (zhuang_hu) {
					if(zimo_fan){
						lChiHuScore += 20;
					}else{
						lChiHuScore += 10;
					}
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						GRR._game_score[i] -= lChiHuScore;
						GRR._game_score[seat_index] += lChiHuScore;
					}
				} else {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						if (i == GRR._banker_player) {
							if(zimo_fan){
								GRR._game_score[i] -= lChiHuScore + 20;
								GRR._game_score[seat_index] += lChiHuScore + 20;
							}else{
								GRR._game_score[i] -= lChiHuScore + 10;
								GRR._game_score[seat_index] += lChiHuScore + 10;
							}

						} else {
							GRR._game_score[i] -= lChiHuScore;
							GRR._game_score[seat_index] += lChiHuScore;
						}
					}
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					GRR._game_score[i] -= lChiHuScore;
					GRR._game_score[seat_index] += lChiHuScore;
				}
			}

		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float lChiHuScore = 1;
			lChiHuScore *= iCardValue;
			//lChiHuScore *= _difen;
			if (_playerStatus[provide_index].is_bao_ting()) {// 报听
				if (has_rule(GameConstants.GAME_RULE_DAI_ZHUANHG)) {// 带庄
					if (zhuang_hu) {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							GRR._game_score[i] -= lChiHuScore + 10;
							GRR._game_score[seat_index] += lChiHuScore + 10;
						}
					} else {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							if (i == GRR._banker_player) {
								GRR._game_score[i] -= lChiHuScore + 10;
								GRR._game_score[seat_index] += lChiHuScore + 10;
							} else {
								GRR._game_score[i] -= lChiHuScore;
								GRR._game_score[seat_index] += lChiHuScore;
							}

						}
					}

				} else {// 没带庄
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						GRR._game_score[i] -= lChiHuScore;
						GRR._game_score[seat_index] += lChiHuScore;
					}
				}
			} else {// 没报听 ，包胡分,包杠分
				// lChiHuScore *= 3;
				iBaoPaiPlayer = provide_index;// 包杠包胡的玩家
				iHuPaiPlayer = seat_index;// 胡牌玩家
				if (has_rule(GameConstants.GAME_RULE_DAI_ZHUANHG)) {// 带庄
					if (zhuang_hu || zhuang_fang_hu) {
						lChiHuScore += 10;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							GRR._game_score[provide_index] -= lChiHuScore;
							GRR._game_score[seat_index] += lChiHuScore;
						}
					} else {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							if (i == GRR._banker_player) {
								GRR._game_score[provide_index] -= lChiHuScore + 10;
								GRR._game_score[seat_index] += lChiHuScore + 10;
							} else {
								GRR._game_score[provide_index] -= lChiHuScore;
								GRR._game_score[seat_index] += lChiHuScore;
							}
						}
					}

				} else {// 没带庄
					GRR._game_score[provide_index] -= lChiHuScore * 3;
					GRR._game_score[seat_index] += lChiHuScore * 3;
				}

			}
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}
		
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}
	
	/**
	 * 吕梁麻将算分,有破产算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_pochan(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 胡的人是庄家
		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		// 是否是庄家放胡
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);

		int iCardColor = _logic.get_card_color(operate_card);
		int iCardValue = _logic.get_card_value(operate_card);
		iCardValue = iCardColor > 2 ? 10 : iCardValue;

		// 如果胡的牌是癞子，按照最大的点数来算
		if (_logic.is_magic_card(operate_card)) {
			iCardValue = get_max_value(seat_index);
		}
		// 如果是单吊耗子按10点算
		if (_playerStatus[seat_index]._hu_card_count >= 25) {
			iCardValue = 10;
		}

		if (has_rule(GameConstants.GAME_RULE_TE_SHU_HU)) {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				iCardValue *= 2;
			} else if (!(chr.opr_and(GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI)).is_empty()) {
				iCardValue *= 2;
			} else if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
				iCardValue *= 2;
			} else if (!(chr.opr_and(GameConstants.CHR_HUNAN_YI_TIAO_LONG)).is_empty()) {
				iCardValue *= 2;
			}
		}
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			boolean zimo_fan = false;
			float lChiHuScore = 1;
			if ((chr.opr_and(GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI)).is_empty()){
				zimo_fan = true;
				lChiHuScore = 2;
			}
			lChiHuScore *= iCardValue;
			//lChiHuScore *= _difen;
			if (has_rule(GameConstants.GAME_RULE_DAI_ZHUANHG)) {
				if (zhuang_hu) {
					if(zimo_fan){
						lChiHuScore += 20;
					}else{
						lChiHuScore += 10;
					}
					
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						GRR._game_score[i] -= lChiHuScore;
						GRR._game_score[seat_index] += lChiHuScore;
					}
				} else {
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						if (i == GRR._banker_player) {
							if(zimo_fan){
								GRR._game_score[i] -= lChiHuScore + 20;
								GRR._game_score[seat_index] += lChiHuScore + 20;
							}else{
								GRR._game_score[i] -= lChiHuScore + 10;
								GRR._game_score[seat_index] += lChiHuScore + 10;
							}

						} else {
							GRR._game_score[i] -= lChiHuScore;
							GRR._game_score[seat_index] += lChiHuScore;
						}
					}
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					GRR._game_score[i] -= lChiHuScore;
					GRR._game_score[seat_index] += lChiHuScore;
				}
			}

		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float lChiHuScore = 1;
			lChiHuScore *= iCardValue;
			//lChiHuScore *= _difen;
			if (_playerStatus[provide_index].is_bao_ting()) {// 报听
				if (has_rule(GameConstants.GAME_RULE_DAI_ZHUANHG)) {// 带庄
					if (zhuang_hu) {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							GRR._game_score[i] -= lChiHuScore + 10;
							GRR._game_score[seat_index] += lChiHuScore + 10;
						}
					} else {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							if (i == GRR._banker_player) {
								GRR._game_score[i] -= lChiHuScore + 10;
								GRR._game_score[seat_index] += lChiHuScore + 10;
							} else {
								GRR._game_score[i] -= lChiHuScore;
								GRR._game_score[seat_index] += lChiHuScore;
							}

						}
					}

				} else {// 没带庄
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (i == seat_index) {
							continue;
						}
						GRR._game_score[i] -= lChiHuScore;
						GRR._game_score[seat_index] += lChiHuScore;
					}
				}
			} else {// 没报听 ，包胡分,包杠分
				// lChiHuScore *= 3;
				iBaoPaiPlayer = provide_index;// 包杠包胡的玩家
				iHuPaiPlayer = seat_index;// 胡牌玩家
				if (has_rule(GameConstants.GAME_RULE_DAI_ZHUANHG)) {// 带庄
					if (zhuang_hu || zhuang_fang_hu) {
						lChiHuScore += 10;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							GRR._game_score[provide_index] -= lChiHuScore;
							GRR._game_score[seat_index] += lChiHuScore;
						}
					} else {
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							if (i == seat_index) {
								continue;
							}
							if (i == GRR._banker_player) {
								GRR._game_score[provide_index] -= lChiHuScore + 10;
								GRR._game_score[seat_index] += lChiHuScore + 10;
							} else {
								GRR._game_score[provide_index] -= lChiHuScore;
								GRR._game_score[seat_index] += lChiHuScore;
							}
						}
					}

				} else {// 没带庄
					GRR._game_score[provide_index] -= lChiHuScore * 3;
					GRR._game_score[seat_index] += lChiHuScore * 3;
				}

			}
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		//打锅子玩法，超过100分只算100分
		if(this.has_rule(GameConstants.GAME_RULE_DA_GUO_ZI)){
			for(int i = 0;i < this.getTablePlayerNumber();i++){
				if(seat_index == i) continue;
				int tempscore = (int)(_player_result.game_score[i] + GRR._game_score[i]);
				if(tempscore <= -100){
					bBaiFenPoChan = true;
					int fan_huai = Math.abs(tempscore) - 100;
					GRR._game_score[i] += fan_huai;
					GRR._game_score[seat_index] -= fan_huai;
				}
			}
		}
		
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;
		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	// 获取最大的点数
	protected int get_max_value(int seat_index) {
		int imax = 0;
		for (int i = 0; i < _playerStatus[seat_index]._hu_card_count; i++) {
			// if(_logic.is_magic_card(_playerStatus[seat_index]._hu_cards[i]))
			// continue;
			if (_playerStatus[seat_index]._hu_cards[i] >= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
				continue;
			int iTempCardColor = _logic.get_card_color(_playerStatus[seat_index]._hu_cards[i]);
			int iTempCardValue = _logic.get_card_value(_playerStatus[seat_index]._hu_cards[i]);
			iTempCardValue = iTempCardColor > 2 ? 10 : iTempCardValue;

			imax = imax > iTempCardValue ? imax : iTempCardValue;
		}
		return imax;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;

		int[] jie_gang = new int[getTablePlayerNumber()];
		int[] fang_gang = new int[getTablePlayerNumber()];
		int[] an_gang = new int[getTablePlayerNumber()];
		int[] ming_gang = new int[getTablePlayerNumber()];
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (GRR != null) {
				if (GRR._end_type != GameConstants.Game_End_DRAW) {
					for (int w = 0; w < GRR._weave_count[player]; w++) {
						if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}

						if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
							an_gang[player]++;
						} else {
							if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
								ming_gang[player]++;
							} else {
								jie_gang[player]++;
								fang_gang[GRR._weave_items[player][w].provide_player]++;
							}

						}
					}
				} else {
					for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
						for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
							if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
								continue;
							}
							if (GRR._weave_items[tmpPlayer][w].public_card == 0 && _logic.is_magic_card(GRR._weave_items[tmpPlayer][w].center_card)) {
								if (tmpPlayer == player)
									an_gang[player]++;
							}
						}
					}
				}
			}
		}

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");
			chrTypes = GRR._chi_hu_rights[player].type_count;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					} else if (type == GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI) {
						gameDesc.append(" 补杠杠开");
					} else if (type == GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI) {
						gameDesc.append(" 暗杠杠开");
					} else if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						gameDesc.append(" 接炮");
					}
					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						gameDesc.append(" 七对");
					}
					if (type == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
						gameDesc.append(" 豪华七对");
					}
					if (type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == GameConstants.CHR_HUNAN_YI_TIAO_LONG) {
						gameDesc.append(" 一条龙");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
				}
			}

			// if(player != weibaotingfangpao){
			if (an_gang[player] > 0) {
				gameDesc.append(" 暗杠X" + an_gang[player]);
			}
			if (ming_gang[player] > 0) {
				gameDesc.append(" 补杠X" + ming_gang[player]);
			}
			if (fang_gang[player] > 0) {
				gameDesc.append(" 放杠X" + fang_gang[player]);
			}
			if (jie_gang[player] > 0) {
				gameDesc.append(" 接杠X" + jie_gang[player]);
			}
			// }
			GRR._result_des[player] = gameDesc.toString();
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		if (delay_time > 0) {
			GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time, TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY
		} else {
			// 发牌
			if (_handler_dispath_last_card != null) {
				this.set_handler(this._handler_dispath_last_card);
				this._handler_dispath_last_card.reset_status(seat_index, type);
				this._handler.exe(this);
			}
		}

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return;
		// add end

		// 去掉
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// 刷新有癞子的牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				this.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._playerStatus[i]._hu_card_count = this.get_ting_card(this._playerStatus[i]._hu_cards, this.GRR._cards_index[i],
					this.GRR._weave_items[i], this.GRR._weave_count[i]);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		//
		// this.exe_dispatch_card(seat_index,MJGameConstants.WIK_NULL, 0);
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

		if (_handler_dispath_last_card != null) {
			// 发牌
			this.set_handler(this._handler_dispath_last_card);
			this._handler_dispath_last_card.reset_status(cur_player, type);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		// TODO Auto-generated method stub

	}

	// 为报听去杠分
	public void process_bao_gang_score_ll(int provide_index) {
		// 先把所有的杠分都清空
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					GRR._gang_score[i].scores[j][k] = 0;// 杠牌，每个人的分数
				}
			}
		}
		// 重新计算杠分
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (player == provide_index)
				continue;
			int iGangcount = 0;
			for (int w = 0; w < GRR._weave_count[player]; w++) {
				if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG)
					continue;
				if (_logic.is_magic_card(GRR._weave_items[player][w].center_card)) {
					continue;
				}
				int iCardValue = 0;
				int iCardColor = 0;
				iCardColor = _logic.get_card_color(GRR._weave_items[player][w].center_card);
				iCardValue = _logic.get_card_value(GRR._weave_items[player][w].center_card);
				iCardValue = iCardColor > 2 ? 10 : iCardValue;
				if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = _difen * iCardValue * 2;
						GRR._gang_score[player].scores[iGangcount][provide_index] -= score;
						GRR._gang_score[player].scores[iGangcount][player] += score;
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = _difen * iCardValue;
						GRR._gang_score[player].scores[iGangcount][provide_index] -= score;
						GRR._gang_score[player].scores[iGangcount][player] += score;
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
					// 勾选点杠包杠
					/*if (this.has_rule(GameConstants.GAME_RULE_BAO_GANG)) {
						// 已经报听
						if (GRR._weave_items[player][w].is_lao_gang) {
							for (int m = 0; m < getTablePlayerNumber(); m++) {
								if (m == player)
									continue;
								int score = _difen * iCardValue;
								GRR._gang_score[player].scores[iGangcount][provide_index] -= score;
								GRR._gang_score[player].scores[iGangcount][player] += score;
							}
						}
						// 未报听
						else {
							for (int m = 0; m < getTablePlayerNumber(); m++) {
								if (m == player)
									continue;
								int score = _difen * iCardValue;
								GRR._gang_score[player].scores[iGangcount][GRR._weave_items[player][w].provide_player] -= score;
								GRR._gang_score[player].scores[iGangcount][player] += score;
							}
						}

					}*/
					// 未选点杠包杠
					//else {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = _difen * iCardValue;
							GRR._gang_score[player].scores[iGangcount][provide_index] -= score;
							GRR._gang_score[player].scores[iGangcount][player] += score;
						}
					//}
					iGangcount++;
				}
			}
		}
	}
	
	//破产模式，正常算杠分
	public void process_not_bao_gang_score_pochan() {
		// 先把所有的杠分都清空
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					GRR._gang_score[i].scores[j][k] = 0;// 杠牌，每个人的分数
				}
			}
		}
		int has_score[] = new int[getTablePlayerNumber()];
		for(int i = 0;i < getTablePlayerNumber();i++){
			has_score[i] = (int)_player_result.game_score[i];
		}
		
		// 重新计算杠分
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			int iGangcount = 0;
			for (int w = 0; w < GRR._weave_count[player]; w++) {
				boolean mac_card = false;
				if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG)
					continue;
				//癞子
				if (_logic.is_magic_card(GRR._weave_items[player][w].center_card)) {
					mac_card = true;
					if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = 100;
							int tempscore = has_score[m] - score;
							if(tempscore < -100){
								bBaiFenPoChan = true;
								int getscore = 100 - Math.abs(has_score[m]);
								GRR._game_score[m] -= getscore;
								GRR._game_score[player] += getscore;
								has_score[m] -= getscore;
								has_score[player] += getscore;
								
							}else{
								GRR._game_score[m] -= score;
								GRR._game_score[player] += score;
								has_score[m] -= score;
								has_score[player] += score;
							}	
						}
						iGangcount++;
					} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = 100;
							
							int provide = GRR._weave_items[player][w].provide_player;
							int tempscore = has_score[provide] - score;
							if(tempscore < -100){
								bBaiFenPoChan = true;
								int getscore = 100 - Math.abs(has_score[provide]);
								GRR._game_score[provide] -= getscore;
								GRR._game_score[player] += getscore;
								has_score[provide] -= getscore;
								has_score[player] += getscore;
								
							}else{
								GRR._game_score[provide] -= score;
								GRR._game_score[player] += score;
								has_score[provide] -= score;
								has_score[player] += score;
							}	
						}
						iGangcount++;
					} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = 100;
							
							int provide = GRR._weave_items[player][w].provide_player;
							int tempscore = has_score[provide] - score;
							if(tempscore < -100){
								bBaiFenPoChan = true;
								int getscore = 100 - Math.abs(has_score[provide]);
								GRR._game_score[provide] -= getscore;
								GRR._game_score[player] += getscore;
								has_score[provide] -= getscore;
								has_score[player] += getscore;
								
							}else{
								GRR._game_score[provide] -= score;
								GRR._game_score[player] += score;
								has_score[provide] -= score;
								has_score[player] += score;
							}	
						}	
						iGangcount++;
					}
				}
				//非癞子杠
				else{
					int iCardValue = 0;
					int iCardColor = 0;
					iCardColor = _logic.get_card_color(GRR._weave_items[player][w].center_card);
					iCardValue = _logic.get_card_value(GRR._weave_items[player][w].center_card);
					iCardValue = iCardColor > 2 ? 10 : iCardValue;
					if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = _difen * iCardValue * 2;
							if(mac_card){
								score = 100;
							}
							int tempscore = has_score[m] - score;
							if(tempscore < -100){
								bBaiFenPoChan = true;
								int getscore = 100 - Math.abs(has_score[m]);
								GRR._game_score[m] -= getscore;
								GRR._game_score[player] += getscore;
								has_score[m] -= getscore;
								has_score[player] += getscore;
								
							}else{
								GRR._game_score[m] -= score;
								GRR._game_score[player] += score;
								has_score[m] -= score;
								has_score[player] += score;
							}	
						}
						iGangcount++;
					} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = _difen * iCardValue;
							if(mac_card){
								score = 100;
							}
							int tempscore = has_score[m] - score;
							if(tempscore < -100){
								bBaiFenPoChan = true;
								int getscore = 100 - Math.abs(has_score[m]);
								GRR._game_score[m] -= getscore;
								GRR._game_score[player] += getscore;
								has_score[m] -= getscore;
								has_score[player] += getscore;
								
							}else{
								GRR._game_score[m] -= score;
								GRR._game_score[player] += score;
								has_score[m] -= score;
								has_score[player] += score;
							}	
						}
						iGangcount++;
					} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
						// 勾选点杠包杠
						// 已经报听
						if (GRR._weave_items[player][w].is_lao_gang) {
							for (int m = 0; m < getTablePlayerNumber(); m++) {
								if (m == player)
									continue;
								int score = _difen * iCardValue;
								if(mac_card){
									score = 100;
								}
								int tempscore = has_score[m] - score;
								if(tempscore < -100){
									bBaiFenPoChan = true;
									int getscore = 100 - Math.abs(has_score[m]);
									GRR._game_score[m] -= getscore;
									GRR._game_score[player] += getscore;
									has_score[m] -= getscore;
									has_score[player] += getscore;
									
								}else{
									GRR._game_score[m] -= score;
									GRR._game_score[player] += score;
									has_score[m] -= score;
									has_score[player] += score;
								}	
							}
						}else{
							if(has_rule(GameConstants.GAME_RULE_BAO_GANG)){
								for (int m = 0; m < getTablePlayerNumber(); m++) {
									if (m == player)
										continue;
									int score = _difen * iCardValue;
									if(mac_card){
										score = 100;
									}
									int tempscore = has_score[GRR._weave_items[player][w].provide_player] - score;
									if(tempscore < -100){
										bBaiFenPoChan = true;
										int getscore = 100 - Math.abs(has_score[GRR._weave_items[player][w].provide_player]);
										GRR._game_score[GRR._weave_items[player][w].provide_player] -= getscore;
										GRR._game_score[player] += getscore;
										has_score[GRR._weave_items[player][w].provide_player] -= getscore;
										has_score[player] += getscore;
										
									}else{
										GRR._game_score[GRR._weave_items[player][w].provide_player] -= score;
										GRR._game_score[player] += score;
										has_score[GRR._weave_items[player][w].provide_player] -= score;
										has_score[player] += score;
									}	
								}
							}else{
								for (int m = 0; m < getTablePlayerNumber(); m++) {
									if (m == player)
										continue;
									int score = _difen * iCardValue;
									if(mac_card){
										score = 100;
									}
									int tempscore = has_score[m] - score;
									if(tempscore < -100){
										bBaiFenPoChan = true;
										int getscore = 100 - Math.abs(has_score[m]);
										GRR._game_score[m] -= getscore;
										GRR._game_score[player] += getscore;
										has_score[m] -= getscore;
										has_score[player] += getscore;
										
									}else{
										GRR._game_score[m] -= score;
										GRR._game_score[player] += score;
										has_score[m] -= score;
										has_score[player] += score;
									}	
								}
							}
						}
						iGangcount++;
					}
				}
			}
		}
	}
	
	//破产模式，包杠算杠分
	public void process_bao_gang_score_pochan(int provide_index) {
		// 先把所有的杠分都清空
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					GRR._gang_score[i].scores[j][k] = 0;// 杠牌，每个人的分数
				}
			}
		}
		int has_score[] = new int[getTablePlayerNumber()];
		for(int i = 0;i < getTablePlayerNumber();i++){
			has_score[i] = (int)_player_result.game_score[i];
		}
		
		// 重新计算杠分
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			if (player == provide_index)
				continue;
			int iGangcount = 0;
			for (int w = 0; w < GRR._weave_count[player]; w++) {
				boolean mac_card = false;
				if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG)
					continue;
				if (_logic.is_magic_card(GRR._weave_items[player][w].center_card)) {
					mac_card = true;
				}
				int iCardValue = 0;
				int iCardColor = 0;
				iCardColor = _logic.get_card_color(GRR._weave_items[player][w].center_card);
				iCardValue = _logic.get_card_value(GRR._weave_items[player][w].center_card);
				iCardValue = iCardColor > 2 ? 10 : iCardValue;
				if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = _difen * iCardValue * 2;
						if(mac_card){
							score = 100;
						}
						int provide = m;
						if(mac_card){
							provide = m;
						}else{
							provide = provide_index;
						}
						int tempscore = has_score[provide] - score;
						if(tempscore < -100){
							bBaiFenPoChan = true;
							int getscore = 100 - Math.abs(has_score[provide]);
							GRR._game_score[provide] -= getscore;
							GRR._game_score[player] += getscore;
							has_score[provide] -= getscore;
							has_score[player] += getscore;
							
						}else{
							GRR._game_score[provide] -= score;
							GRR._game_score[player] += score;
							has_score[provide] -= score;
							has_score[player] += score;
						}	
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = _difen * iCardValue;
						if(mac_card){
							score = 100;
						}
						int provide = GRR._weave_items[player][w].provide_player;
						if(mac_card){
							provide = GRR._weave_items[player][w].provide_player;
						}else{
							provide = provide_index;
						}
						
						int tempscore = has_score[provide] - score;
						if(tempscore < -100){
							bBaiFenPoChan = true;
							int getscore = 100 - Math.abs(has_score[provide]);
							GRR._game_score[provide] -= getscore;
							GRR._game_score[player] += getscore;
							has_score[provide] -= getscore;
							has_score[player] += getscore;
							
						}else{
							GRR._game_score[provide] -= score;
							GRR._game_score[player] += score;
							has_score[provide] -= score;
							has_score[player] += score;
						}	
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
					// 勾选点杠包杠
					/*if (this.has_rule(GameConstants.GAME_RULE_BAO_GANG)) {
						// 已经报听
						if (GRR._weave_items[player][w].is_lao_gang) {
							for (int m = 0; m < getTablePlayerNumber(); m++) {
								if (m == player)
									continue;
								int score = _difen * iCardValue;
								if(mac_card){
									score = 100;
								}
								int tempscore = has_score[provide_index] - score;
								if(tempscore < -100){
									bBaiFenPoChan = true;
									int getscore = 100 - Math.abs(has_score[provide_index]);
									GRR._game_score[provide_index] -= getscore;
									GRR._game_score[player] += getscore;
									has_score[provide_index] -= getscore;
									has_score[player] += getscore;
									
								}else{
									GRR._game_score[provide_index] -= score;
									GRR._game_score[player] += score;
									has_score[provide_index] -= score;
									has_score[player] += score;
								}	
							}
						}
						// 未报听
						else {
							for (int m = 0; m < getTablePlayerNumber(); m++) {
								if (m == player)
									continue;
								int score = _difen * iCardValue;
								if(mac_card){
									score = 100;
								}
								int tempscore = has_score[GRR._weave_items[player][w].provide_player] - score;
								if(tempscore < -100){
									bBaiFenPoChan = true;
									int getscore = 100 - Math.abs(has_score[GRR._weave_items[player][w].provide_player]);
									GRR._game_score[GRR._weave_items[player][w].provide_player] -= getscore;
									GRR._game_score[player] += getscore;
									has_score[GRR._weave_items[player][w].provide_player] -= getscore;
									has_score[player] += getscore;
									
								}else{
									GRR._game_score[GRR._weave_items[player][w].provide_player] -= score;
									GRR._game_score[player] += score;
									has_score[GRR._weave_items[player][w].provide_player] -= score;
									has_score[player] += score;
								}	
							}
						}

					}*/
					// 未选点杠包杠
					//else {
						for (int m = 0; m < getTablePlayerNumber(); m++) {
							if (m == player)
								continue;
							int score = _difen * iCardValue;
							if(mac_card){
								score = 100;
							}
							int provide = GRR._weave_items[player][w].provide_player;
							if(mac_card){
								provide = GRR._weave_items[player][w].provide_player;
							}else{
								provide = provide_index;
							}
							int tempscore = has_score[provide] - score;
							if(tempscore < -100){
								bBaiFenPoChan = true;
								int getscore = 100 - Math.abs(has_score[provide]);
								GRR._game_score[provide] -= getscore;
								GRR._game_score[player] += getscore;
								has_score[provide] -= getscore;
								has_score[player] += getscore;
								
							}else{
								GRR._game_score[provide] -= score;
								GRR._game_score[player] += score;
								has_score[provide] -= score;
								has_score[player] += score;
							}	
						}
					//}
					iGangcount++;
				}
			}
		}
	}
	
	//破产模式，流局算耗子杠分
	public void process_liuju_score_pochan(int []laizi_gang_score) {
		
		// 先把所有的杠分都清空
		int has_score[] = new int[getTablePlayerNumber()];
		for(int i = 0;i < getTablePlayerNumber();i++){
			has_score[i] = (int)_player_result.game_score[i];
		}
		
		// 重新计算杠分
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			int iGangcount = 0;
			for (int w = 0; w < GRR._weave_count[player]; w++) {
				if (GRR._weave_items[player][w].weave_kind != GameConstants.WIK_GANG)
					continue;
				if (!_logic.is_magic_card(GRR._weave_items[player][w].center_card)) {
					continue;
				}
				if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_AN_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = 100;
						int tempscore = has_score[m] - score;
						if(tempscore < -100){
							bBaiFenPoChan = true;
							int getscore = 100 - Math.abs(has_score[m]);
							laizi_gang_score[m] -= getscore;
							laizi_gang_score[player] += getscore;
							has_score[m] -= getscore;
							has_score[player] += getscore;
							
						}else{
							laizi_gang_score[m] -= score;
							laizi_gang_score[player] += score;
							has_score[m] -= score;
							has_score[player] += score;
						}	
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_ADD_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = 100;
						int tempscore = has_score[GRR._weave_items[player][w].provide_player] - score;
						if(tempscore < -100){
							bBaiFenPoChan = true;
							int getscore = 100 - Math.abs(has_score[GRR._weave_items[player][w].provide_player]);
							laizi_gang_score[GRR._weave_items[player][w].provide_player] -= getscore;
							laizi_gang_score[player] += getscore;
							has_score[GRR._weave_items[player][w].provide_player] -= getscore;
							has_score[player] += getscore;
							
						}else{
							laizi_gang_score[GRR._weave_items[player][w].provide_player] -= score;
							laizi_gang_score[player] += score;
							has_score[GRR._weave_items[player][w].provide_player] -= score;
							has_score[player] += score;
						}	
					}
					iGangcount++;
				} else if (GRR._weave_items[player][w].type == GameConstants.GANG_TYPE_JIE_GANG) {
					for (int m = 0; m < getTablePlayerNumber(); m++) {
						if (m == player)
							continue;
						int score = 100;
						int tempscore = has_score[GRR._weave_items[player][w].provide_player] - score;
						if(tempscore < -100){
							bBaiFenPoChan = true;
							int getscore = 100 - Math.abs(has_score[GRR._weave_items[player][w].provide_player]);
							laizi_gang_score[GRR._weave_items[player][w].provide_player] -= getscore;
							laizi_gang_score[player] += getscore;
							has_score[GRR._weave_items[player][w].provide_player] -= getscore;
							has_score[player] += getscore;
							
						}else{
							laizi_gang_score[GRR._weave_items[player][w].provide_player] -= score;
							laizi_gang_score[player] += score;
							has_score[GRR._weave_items[player][w].provide_player] -= score;
							has_score[player] += score;
						}	
					}
					iGangcount++;
				}
			}
		}
	}

	public class DianGangInfo {
		public int iValue; // 点杠的值
		public int iDianGangCount; // 点杠次数
		public int iBeiGangPlayer; // 被杠玩家
		public int iDianGangPlayer;// 点杠玩家

		public DianGangInfo() {
			iValue = 0;
			iDianGangCount = 0;
			iBeiGangPlayer = GameConstants.INVALID_SEAT;
			iDianGangPlayer = GameConstants.INVALID_SEAT;
		}
	}

}
