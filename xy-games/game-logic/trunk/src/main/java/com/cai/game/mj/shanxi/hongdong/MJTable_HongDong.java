package com.cai.game.mj.shanxi.hongdong;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.GameConstants_SXJY;
import com.cai.common.constant.game.mj.Constants_MJ_SXHD;
import com.cai.common.constant.game.mj.Constants_MJ_SXHS;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.Tuple;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_HongDong extends AbstractMJTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	public int[] tou_zi_dian_shu = new int[4];
	public int time_for_animation = 2000; // 摇骰子的动画时间(ms)
	public int time_for_fade = 500; // 摇骰子动画的消散延时(ms)

	protected ScheduledFuture<?> _release_scheduled;
	protected ScheduledFuture<?> _table_scheduled;
	protected ScheduledFuture<?> _game_scheduled;

	protected MJHandlerOutCardBaoTing_HongDong _handler_out_card_bao_ting; // 报听
	protected MJHandler_XuanWang _handler_xuan_wang;

	protected int tui_zi_shu = 0;
	private int _magic_card_count;// 癞子牌数量
	public boolean duo_xiang_has_zhuang = false; // 一炮多响中 庄家是不是胡牌的
	public int player_seat = 0; // 点炮者逆时针第一个胡牌的为下一局的庄家
	public boolean one = false;
	public boolean two = false;
	public boolean three = false;
	public int has_gang_count = 0;// 每一局是否有人开杠、
	public int left_card = 14;// 每一局是否有人开杠
	public int[] person_magic_card;// 个人王牌
	public boolean has_lai_zi = false;// 有癞子玩法
	public int gang_count;
	public int[] player_zfb_count;
	public int[] player_feng_count;

	public Tuple<Boolean, List<Integer>>[] _ying_kou;
	public Tuple<Boolean, Integer>[] _magic_peng_gang_color;
	public int[] player_magic_card;
	public boolean[] player_magic_card_show_non;

	public MJTable_HongDong() {
		super(MJType.GAME_TYPE_MJ_SXHD);
	}

	@Override
	protected void onInitTable() {
		_handler_gang = new MJHandlerGang_ShanXi_HongDong();
		_handler_chi_peng = new MJHandlerChiPeng_ShanXi_HongDong();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_HongDong();
		_handler_dispath_card = new MJHandlerDispatchCard_ShanXi_HongDong();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ShanXi_HongDong();
		_handler_xuan_wang = new MJHandler_XuanWang();
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_xuan_wang.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	public void xex_xuan_wang(int seat_index, int send_card_data, GangCardResult m_gangCardResult) {
		_handler_xuan_wang.reset(seat_index, send_card_data, m_gangCardResult);
		set_handler(_handler_xuan_wang);
		_handler_xuan_wang.exe(this);
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) {
			return 2;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) {
			return 3;
		}
		return 4;

	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
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

		int count = getTablePlayerNumber();
		if (count <= 0) {
			count = has_rule(GameConstants.GAME_RULE_HUNAN_THREE) ? getTablePlayerNumber() - 1 : getTablePlayerNumber();
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

	public boolean show_tou_zi() {

		tou_zi_dian_shu[0] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[1] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[2] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		tou_zi_dian_shu[3] = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		tui_zi_shu = tou_zi_dian_shu[0] + tou_zi_dian_shu[1];

		// if (this._cur_round == 1) {
		// GRR._banker_player = _cur_banker = (tou_zi_dian_shu[0] +
		// tou_zi_dian_shu[1]) % getTablePlayerNumber();
		// }
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

	@Override
	public void test_cards() {
		int[] cards_of_player1 = new int[] { 0x31, 0x31, 0x31, 0x32, 0x32, 0x32, 0x33, 0x33, 0x33, 0x35, 0x35, 0x35, 0x36 };
		int[] cards_of_player2 = new int[] { 0x31, 0x31, 0x31, 0x32, 0x32, 0x32, 0x33, 0x33, 0x33, 0x35, 0x35, 0x35, 0x36 };
		int[] cards_of_player0 = new int[] { 0x31, 0x31, 0x31, 0x32, 0x32, 0x32, 0x33, 0x33, 0x33, 0x35, 0x35, 0x35, 0x36 };
		int[] cards_of_player3 = new int[] { 0x21, 0x21, 0x21, 0x11, 0x11, 0x11, 0x13, 0x13, 0x13, 0x35, 0x36, 0x18, 0x18 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (this.getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (this.getTablePlayerNumber() == 2) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
			}
		}

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

	@SuppressWarnings("unchecked")
	@Override
	public boolean reset_init_data() {
		super.reset_init_data();
		gang_count = 0;
		_ying_kou = new Tuple[getTablePlayerNumber()];
		_magic_peng_gang_color = new Tuple[getTablePlayerNumber()];
		player_zfb_count = new int[getTablePlayerNumber()];
		player_feng_count = new int[getTablePlayerNumber()];
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_ying_kou[p] = new Tuple<Boolean, List<Integer>>();
			_ying_kou[p].setRight(Lists.newArrayList());

			if (has_rule(Constants_MJ_SXHD.GAME_RULE_QING_YI_SE) || has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)
					|| has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
				_magic_peng_gang_color[p] = new Tuple<Boolean, Integer>(true, -1);
			}
		}
		return true;
	}

	@Override
	protected boolean on_game_start() {

		// 游戏开始
		_logic.clean_magic_cards();
		player_magic_card = new int[getTablePlayerNumber()];
		player_magic_card_show_non = new boolean[getTablePlayerNumber()];

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
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

		// 勾选这些玩法厚选王牌是根据扔骰子确定的
		if (has_rule(Constants_MJ_SXHD.GAME_RULE_PING_HU) || has_rule(Constants_MJ_SXHD.GAME_RULE_PING_HU_NO_CAN_HU_PH)
				|| has_rule(Constants_MJ_SXHD.GAME_RULE_QING_YI_SE)) {
			// 发完牌后再扔一次骰子 根据骰子数选中王牌
			show_tou_zi();
			SetMagiCards();
			has_lai_zi = true;
		}

		this.exe_dispatch_card(_current_player, GameConstants.DispatchCard_Type_Tian_Hu, 0);

		return true;

	}

	public int get_real_card(int card) {

		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI + GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI + GameConstants.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		if (card > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			return card;
		}
		return card;
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
	public boolean operate_player_get_card(int seat_index, int count, int cards[], int to_player, int cardType) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SEND_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(cardType);// get牌
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

	// 选王牌
	public boolean SetMagiCards() {

		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB) || has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
			return false;
		}

		int icount = 0;
		int cards[] = new int[1];
		_logic.clean_magic_cards();
		icount = 1;
		cards[0] = _repertory_card[_all_card_len - tui_zi_shu * 2];

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			cards[0] = 0x18;
		}
		if (DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			cards[0] = magic_card_decidor;
			DEBUG_MAGIC_CARD = false;
		}

		int iNextCard = GetNextCard(cards[0]);
		_logic.add_magic_card_index(_logic.switch_to_card_index(iNextCard));
		_magic_card_count = 1;
		GRR._especial_card_count = 1;
		GRR._especial_show_cards[0] = iNextCard;
		operate_show_card(this.GRR._banker_player, GameConstants.Show_Card_Center, icount, cards, GameConstants.INVALID_SEAT);

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			// 刷新手牌包括组合
			int hand_cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[p], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
			operate_player_cards(p, hand_card_count, hand_cards, GRR._weave_count[p], GRR._weave_items[p]);
		}

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

	// 分析胡牌的方法 大将王
	public int analyse_chi_hu_card_da_jang_wang(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index, boolean need_to_multiply) {
		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		if (_ying_kou[_seat_index].getLeft() && !_ying_kou[_seat_index].getRight().contains(cur_card)) {
			return GameConstants.WIK_NULL;
		}

		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card != 0) {
			int cur_card_index = _logic.switch_to_card_index(cur_card);
			temp_cards_index[cur_card_index]++;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		boolean can_win = false; // 是否能胡牌
		boolean can_hu_xiao_hu = true;// 能胡小胡
		boolean has_qi_xiao_dui = false;// 七小对
		boolean has_qing_yi_se = false;// 清一色
		boolean has_chun_yi_se = false;// 纯一色
		boolean has_zi_yi_se = false;// 字一色

		// 判断是不是清一色
		boolean is_qing_yi_se = this.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_qing_yi_se) {
			can_win = true;
			has_qing_yi_se = true;
			can_hu_xiao_hu = false;
		}

		// 判断是不是纯一色
		boolean is_chun_yi_se = this.is_chun_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_chun_yi_se) {
			can_win = true;
			has_chun_yi_se = true;
			can_hu_xiao_hu = false;
		}

		// 判断是不是字一色
		boolean is_zi_yi_se = this.is_feng_is_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_zi_yi_se) {
			can_win = true;
			has_zi_yi_se = true;
			can_hu_xiao_hu = false;
		}

		boolean bValue = false;
		// ①所有玩法必须缺一门才能胡牌（共万筒条三门，字牌不计）；
		// ②必须报听才能胡牌。
		int colorCount = _logic.get_se_count(temp_cards_index, weaveItems, weave_count);
		if (colorCount > 2) {
			return GameConstants.WIK_NULL;
		}

		// 正常胡牌
		for (int i = 0; i < 3; i++) {
			bValue |= AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					new int[] { _logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + i) }, 1);
		}

		if (bValue) { // 如果能胡
			can_win = true;
		}

		if (_ying_kou[_seat_index].getLeft()) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_YING_KOU, need_to_multiply);
		}

		return GameConstants.WIK_NULL;
	}

	private boolean check_magic_diao_wang(int[] cards_index, int exclude_count, int seat_index) {
		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		for (int i = 0; i < 3; i++) {
			if (tmp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + i)] >= exclude_count) {
				tmp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + i)] -= exclude_count;
			} else {
				tmp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + i)] = 0;
			}
		}

		int select = _logic.get_card_count_by_index(tmp_cards_index) % 3;
		if (select == 1) {
			int ting_count = get_ting_card(new int[GameConstants.MAX_ZI_FENG], tmp_cards_index, null, 0, seat_index);
			if (ting_count == -1 || ting_count > 12) {
				return true;
			}
		} else if (select == 2) {
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				if (tmp_cards_index[i] > 0) {
					tmp_cards_index[i]--;
					int ting_count = get_ting_card(new int[GameConstants.MAX_ZI_FENG], tmp_cards_index, null, 0, seat_index);
					if (ting_count == -1 || ting_count > 12) {
						return true;
					}
					tmp_cards_index[i]++;
				}
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		int count = card_count;
		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
			if (player_magic_card_show_non[seat_index]) {
				liang_magic_count = 2;
			}
			for (int j = 0; j < count; j++) {
				if (get_real_card(cards[j]) != player_magic_card[seat_index]) {
					continue;
				}

				if (liang_magic_count == 2) {
					cards[j] = player_magic_card[seat_index] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[j] = player_magic_card[seat_index] + GameConstants.CARD_ESPECIAL_TYPE_GUI;
					liang_magic_count++;
					card_count--;
				}
				if (!player_magic_card_show_non[seat_index]) {
					roomResponse.setNao(player_magic_card[seat_index]);
				}
			}
		}
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
		for (int j = 0; j < count; j++) {
			if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && cards[j] >= GameConstants.CARD_ESPECIAL_TYPE_GUI
					&& cards[j] <= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
			roomResponse.addCardData(cards[j]);
		}
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
		int count = card_count;
		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
			if (player_magic_card_show_non[seat_index]) {
				liang_magic_count = 2;
			}
			for (int j = 0; j < count; j++) {
				if (cards[j] != player_magic_card[seat_index] && cards[j] != player_magic_card[seat_index] + GameConstants.CARD_ESPECIAL_TYPE_TING) {
					continue;
				}

				cards[j] = cards[j] > GameConstants.CARD_ESPECIAL_TYPE_TING ? cards[j] - GameConstants.CARD_ESPECIAL_TYPE_TING : cards[j];
				if (liang_magic_count == 2) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					liang_magic_count++;
					card_count--;
				}
				roomResponse.setNao(player_magic_card[seat_index]);
			}
		}
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
		for (int j = 0; j < count; j++) {
			if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && cards[j] >= GameConstants.CARD_ESPECIAL_TYPE_GUI
					&& cards[j] <= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
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
	public boolean operate_player_cards_with_ting_ed(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		int[] has_kou_out_card = new int[Constants_MJ_SXHD.HAND_CARD_MAX_COUNT];
		int kou_out_card_count = accpet_ying_kou(seat_index, _playerStatus[seat_index]._hu_out_card_count,
				_playerStatus[seat_index]._hu_out_card_ting, _playerStatus[seat_index]._hu_out_card_ting_count,
				_playerStatus[seat_index]._hu_out_cards, has_kou_out_card);

		for (int i = 0; i < card_count; i++) {
			// for (int j = 0; j < _playerStatus[seat_index]._hu_out_card_count;
			// j++) {
			// if (cards[i] == _playerStatus[seat_index]._hu_out_card_ting[j]) {
			// int ting_card_cout =
			// _playerStatus[seat_index]._hu_out_card_ting_count[j];
			// out: for (int k = 0; k < ting_card_cout; k++) {
			// for (int kk = 0; kk < kou_out_card_count; kk++) {
			// if (has_kou_out_card[kk] ==
			// _playerStatus[seat_index]._hu_out_cards[j][k]) {
			// cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
			// break out;
			// }
			// }
			// }
			// break;
			// }
			// }
			for (int kk = 0; kk < kou_out_card_count; kk++) {
				if (has_kou_out_card[kk] == cards[i]) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
					break;
				}
			}
			if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
				if (_logic.is_magic_card(cards[i]))
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		// 手牌数量
		int count = card_count;
		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
			if (player_magic_card_show_non[seat_index]) {
				liang_magic_count = 2;
			}
			for (int j = 0; j < count; j++) {
				if (cards[j] != player_magic_card[seat_index] && cards[j] != player_magic_card[seat_index] + GameConstants.CARD_ESPECIAL_TYPE_TING) {
					continue;
				}

				int card = cards[j] > GameConstants.CARD_ESPECIAL_TYPE_TING ? cards[j] - GameConstants.CARD_ESPECIAL_TYPE_TING : cards[j];
				if (liang_magic_count == 2) {
					// cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[j] = card + GameConstants.CARD_ESPECIAL_TYPE_GUI;
					liang_magic_count++;
					card_count--;
				}
				if (!player_magic_card_show_non[seat_index]) {
					roomResponse.setNao(player_magic_card[seat_index]);
				}
			}
		}
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
		for (int j = 0; j < count; j++) {
			if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && cards[j] >= GameConstants.CARD_ESPECIAL_TYPE_GUI
					&& cards[j] <= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
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
				if (_playerStatus[seat_index]._hu_out_card_ting[i] < GameConstants.HZ_MAGIC_CARD) {
					roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
				}
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

	// 分析胡牌的方法
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, boolean need_to_multiply) {

		int cbChiHuKind = GameConstants.WIK_NULL;

		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)) {
			List<Tuple<Integer, Integer>> chr_fan_list = Lists.newArrayList();
			for (int i = 0; i < 3; i++) {
				chiHuRight.set_empty();
				_logic.clean_magic_cards();
				_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + i));
				analyse_chi_hu_card_cal(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index, need_to_multiply);
				_logic.clean_magic_cards();
				chr_fan_list.add(new Tuple<Integer, Integer>(get_fan(chiHuRight, _seat_index), i));
			}

			int max_fan = 0;
			int max_fan_index = 0;
			for (int i = 0; i < chr_fan_list.size(); i++) {
				Tuple<Integer, Integer> tuple = chr_fan_list.get(i);
				if (max_fan <= tuple.getLeft()) {
					max_fan = tuple.getLeft();
					max_fan_index = tuple.getRight();
				}
			}
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + max_fan_index));
			chiHuRight.set_empty();
			cbChiHuKind = analyse_chi_hu_card_cal(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index,
					need_to_multiply);
			_logic.clean_magic_cards();
			return cbChiHuKind;
		}
		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
			_logic.clean_magic_cards();
			if (player_magic_card[_seat_index] != 0) {
				_logic.add_magic_card_index(_logic.switch_to_card_index(player_magic_card[_seat_index]));
			}
			cbChiHuKind = analyse_chi_hu_card_cal(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index,
					need_to_multiply);
			_logic.clean_magic_cards();
			return cbChiHuKind;
		}

		cbChiHuKind = analyse_chi_hu_card_cal(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index, need_to_multiply);
		return cbChiHuKind;
	}

	// 分析胡牌的方法
	public int analyse_chi_hu_card_cal(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, boolean need_to_multiply) {

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		if (_ying_kou[_seat_index].getLeft() != null && _ying_kou[_seat_index].getLeft() && !_ying_kou[_seat_index].getRight().contains(cur_card)) {
			return GameConstants.WIK_NULL;
		}

		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card != 0) {
			int cur_card_index = _logic.switch_to_card_index(cur_card);
			temp_cards_index[cur_card_index]++;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		boolean can_win = false; // 是否能胡牌
		boolean can_hu_xiao_hu = true;// 能胡小胡
		boolean has_qi_xiao_dui = false;// 七小对
		boolean has_qing_yi_se = false;// 清一色
		boolean has_chun_yi_se = false;// 纯一色
		boolean has_zi_yi_se = false;// 字一色
		boolean has_diao_wang = false;// 吊王
		boolean has_yi_tiao_long = false;// 一条龙
		boolean has_lou_shang_lou = false;// 楼上楼
		boolean has_da_san_yuan = false;// 大三元
		boolean has_zhuang_jia = false;// 庄家
		boolean has_hei_san_feng = false;// 黑三风
		boolean has_zhong_fa_bai = false;// 中发白
		boolean has_ying_kou = false;// 硬扣

		// 判断是否是七对
		int check_qi_xiao_dui = this.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (_ying_kou[_seat_index].getLeft() != null && _ying_kou[_seat_index].getLeft()) {
			check_qi_xiao_dui = GameConstants.WIK_NULL;
		}
		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			can_win = true;
			has_qi_xiao_dui = true;
			// can_hu_xiao_hu = false;
		}

		// 判断是不是清一色
		boolean is_qing_yi_se = this.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_qing_yi_se) {
			// can_win = true;
			has_qing_yi_se = true;
			// can_hu_xiao_hu = false;
		}

		// 判断是不是纯一色
		boolean is_chun_yi_se = this.is_chun_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_chun_yi_se) {
			// can_win = true;
			has_chun_yi_se = true;
			// can_hu_xiao_hu = false;
		}

		// 判断是不是字一色
		boolean is_zi_yi_se = this.is_feng_is_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_zi_yi_se) {
			// can_win = true;
			has_zi_yi_se = true;
			// can_hu_xiao_hu = false;
		}

		// 判断是不是大三元
		boolean is_da_san_yuan = check_da_xiao_san_yuan(temp_cards_index, weaveItems, weave_count);
		if (is_da_san_yuan && cur_card >= GameConstants.HZ_MAGIC_CARD) {
			// can_win = true;
			has_da_san_yuan = true;
			// can_hu_xiao_hu = false;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
			// magic_cards_index[i] = person_magic_card[_seat_index];
		}

		// 判断是不是一条龙
		if (this.is_yi_tiao_long_fc(temp_cards_index, weave_count, magic_cards_index, magic_card_count)) {
			can_win = true;
			has_yi_tiao_long = true;
			// can_hu_xiao_hu = false;
		}

		// ①所有玩法必须缺一门才能胡牌（共万筒条三门，字牌不计）；
		// ②必须报听才能胡牌。
		int colorCount = _logic.get_se_count(temp_cards_index, weaveItems, weave_count);
		if (colorCount > 2) {
			return GameConstants.WIK_NULL;
		}

		boolean bValue = false;

		// 正常胡牌
		if (_logic.get_magic_card_count() > 0) {
			int magic_count = temp_cards_index[_logic.get_magic_card_index(0)];
			for (int i = 0; i <= magic_count; i++) {
				temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
				if (cur_card != 0) {
					int cur_card_index = _logic.switch_to_card_index(cur_card);
					temp_cards_index[cur_card_index]++;
				}
				temp_cards_index[_logic.get_magic_card_index(0)] -= i;
				bValue |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(temp_cards_index, i);
			}
		}
		bValue |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (bValue) { // 如果能胡
			can_win = true;
		}
		if (can_win == false) { // 如果不能胡牌
			// 这里不能胡牌时也不能清空，因为选美胡时，需要叠加多张牌的CHR
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean magic_hu = false;
		magic_hu |= is_zi_yi_se |= is_qing_yi_se |= is_chun_yi_se;
		if (!magic_hu && (has_rule(Constants_MJ_SXHD.GAME_RULE_QING_YI_SE) || has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)
				|| has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB))) {
			return GameConstants.WIK_NULL;
		}

		// 所有玩法必须缺一门才能胡牌（共万筒条三门，字牌不计）
		if (get_card_color_ex_zi(temp_cards_index, has_qi_xiao_dui) > 2) {
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_MJ_SXHS.GAME_RULE_ZIMOHU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_ZI_MO, need_to_multiply); // 自摸
		}

		temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card != 0) {
			int cur_card_index = _logic.switch_to_card_index(cur_card);
			temp_cards_index[cur_card_index]++;
		}
		int n = temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)];
		for (int i = 0; i < 3; i++) {
			if (temp_cards_index[_logic.switch_to_card_index(i + GameConstants.HZ_MAGIC_CARD)] < n) {
				n = temp_cards_index[_logic.switch_to_card_index(i + GameConstants.HZ_MAGIC_CARD)];
			}
		}
		player_zfb_count[_seat_index] = 0;
		for (int j = 1; j <= n; j++) {
			temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)]--;
			temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)]--;
			temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)]--;

			boolean flag = false;
			if (_logic.get_magic_card_count() > 0) {
				int magic_count = temp_cards_index[_logic.get_magic_card_index(0)];
				for (int i = 0; i <= magic_count; i++) {
					int[] tmp_cards_index = Arrays.copyOf(temp_cards_index, cards_index.length);
					tmp_cards_index[_logic.get_magic_card_index(0)] -= i;
					flag |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(tmp_cards_index, i);
				}
			}
			flag |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(temp_cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
			if (flag) {
				player_zfb_count[_seat_index]++;
			} else {
				break;
			}
		}

		boolean diao_wan_filter = true;
		int cbCardIndexTemp[] = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card != 0) {
			int cur_card_index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[cur_card_index]++;
		}
		player_feng_count[_seat_index] = cal_hei_san_feng(cbCardIndexTemp);
		if (player_feng_count[_seat_index] > 0 && !has_qi_xiao_dui) { // 黑三风
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_HEI_SAN_FENG, need_to_multiply);
		}
		if (_ying_kou[_seat_index].getLeft() != null && _ying_kou[_seat_index].getLeft() && cur_card >= GameConstants.HZ_MAGIC_CARD
				&& player_zfb_count[_seat_index] >= 2) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_LOU_SHANG_LOU, need_to_multiply);
			int count = player_zfb_count[_seat_index];
			diao_wan_filter = false;
			if (check_magic_diao_wang(cards_index, 3, _seat_index)) {
				chiHuRight.opr_or(Constants_MJ_SXHD.CHR_DIAO_WANG);
			}
			player_zfb_count[_seat_index] = count;
			player_zfb_count[_seat_index] -= 2;
		}
		if (_ying_kou[_seat_index].getLeft() != null && _ying_kou[_seat_index].getLeft()
				&& chiHuRight.opr_and(Constants_MJ_SXHD.CHR_LOU_SHANG_LOU).is_empty()) { // 硬扣
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_YING_KOU, need_to_multiply);
			player_zfb_count[_seat_index]--;
		}
		if (player_zfb_count[_seat_index] >= 3 && has_da_san_yuan && chiHuRight.opr_and(Constants_MJ_SXHD.CHR_YING_KOU).is_empty()) { // 大三元
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_DA_SAN_YUAN, need_to_multiply);
			diao_wan_filter = false;
			if (check_magic_diao_wang(cards_index, 3, _seat_index)) {
				chiHuRight.opr_or(Constants_MJ_SXHD.CHR_DIAO_WANG);
			}
		}
		if (player_zfb_count[_seat_index] >= 1 && chiHuRight.opr_and(Constants_MJ_SXHD.CHR_DA_SAN_YUAN).is_empty()) { // 中发白
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_ZHONG_FA_BAI, need_to_multiply);
			int count = player_zfb_count[_seat_index];
			diao_wan_filter = false;
			if (check_magic_diao_wang(cards_index, count, _seat_index)) {
				chiHuRight.opr_or(Constants_MJ_SXHD.CHR_DIAO_WANG);
			}
			player_zfb_count[_seat_index] = count;
		}
		if (has_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_QI_DUI, need_to_multiply);
		}
		if (has_qing_yi_se && !has_zi_yi_se) { // 清一色
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_QING_YI_SE, need_to_multiply);
		}
		if (has_yi_tiao_long) { // 一条龙
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_YI_TIAO_LONG, need_to_multiply);

			int cur_card_color = _logic.get_card_color(cur_card);
			if (cur_card_color != 3 && !_logic.is_magic_card(cur_card)) {
				cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);
				if (cur_card != 0) {
					int cur_card_index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[cur_card_index]++;
				}
				boolean has_enough_cards = true;
				for (int i = cur_card_color * 9; i < cur_card_color * 9 + 9; i++) {
					if (cbCardIndexTemp[i] <= 0) {
						has_enough_cards = false;
						break;
					}
					cbCardIndexTemp[i]--;
				}

				if (has_enough_cards) {
					diao_wan_filter = false;
				}
			}
		}
		if (has_zi_yi_se) { // 字一色
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_ZI_YI_SE, need_to_multiply);
		}
		if (has_chun_yi_se) { // 纯一色
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_CHUN_YI_SE, need_to_multiply);
		}
		if (can_hu_xiao_hu) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHD.CHR_PING_HU, need_to_multiply);
		}
		if ((_playerStatus[_seat_index]._hu_cards[0] == -1 || _playerStatus[_seat_index]._hu_card_count > 12) && diao_wan_filter) { // 吊王
			chiHuRight.opr_or(Constants_MJ_SXHD.CHR_DIAO_WANG);
		}

		int gang_count = _player_result.zhi_gang_count[_seat_index] + _player_result.ming_gang_count[_seat_index]
				+ _player_result.an_gang_count[_seat_index];
		if (get_fan(chiHuRight, _seat_index) == 1 && has_rule(Constants_MJ_SXHD.GAME_RULE_PING_HU_NO_CAN_HU_PH) && gang_count == 0) {
			return GameConstants.WIK_NULL;
		}
		return cbChiHuKind;
	}

	public int cal_hei_san_feng(int[] cards_index) {
		int count = 0;
		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		int[][] feng_chi = new int[][] { { 1, 1, 1, 0 }, { 1, 1, 0, 1 }, { 1, 0, 1, 1 }, { 0, 1, 1, 1 } };
		int feng_chi_count_max = 4;
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();
		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		for (int i = 0; i < feng_chi_count_max; i++) {
			for (int fc = 0; fc < feng_chi.length; fc++) {
				boolean is_continue = true;
				for (int fcc = 0; fcc < feng_chi[fc].length; fcc++) {
					if (tmp_cards_index[GameConstants.MAX_ZI + fcc] < feng_chi[fc][fcc]) {
						is_continue = false;
						break;
					}
				}
				if (!is_continue) {
					continue;
				}
				for (int fcc = 0; fcc < feng_chi[fc].length; fcc++) {
					tmp_cards_index[GameConstants.MAX_ZI + fcc] -= feng_chi[fc][fcc];
				}

				// 正常胡牌
				boolean bValue = false;
				if (_logic.get_magic_card_count() > 0) {
					int magic_count = tmp_cards_index[_logic.get_magic_card_index(0)];
					for (int j = 0; j <= magic_count; j++) {
						tmp_cards_index[_logic.get_magic_card_index(0)] -= j;
						bValue |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(tmp_cards_index, j);
						tmp_cards_index[_logic.get_magic_card_index(0)] += j;
					}
				}
				bValue |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(tmp_cards_index, 0, magic_cards_index, magic_card_count);

				if (bValue) {
					count++;
				} else {
					for (int fcc = 0; fcc < feng_chi[fc].length; fcc++) {
						tmp_cards_index[GameConstants.MAX_ZI + fcc] += feng_chi[fc][fcc];
					}
				}
			}
		}
		return count;
	}

	public boolean check_card_ying_kou(int[] cards_index, int card, int _seat_index) {
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (card != 0) {
			int cur_card_index = _logic.switch_to_card_index(card);
			temp_cards_index[cur_card_index]++;
		}

		if (temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 0) {
			return false;
		}
		if (temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)] == 0) {
			return false;
		}
		if (temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)] == 0) {
			return false;
		}
		temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)]--;
		temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)]--;
		temp_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)]--;

		boolean flag = false;
		if (has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)) {
			for (int i = 0; i < 3; i++) {
				_logic.clean_magic_cards();
				_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + i));
				flag |= check_hu(temp_cards_index, _logic.get_all_magic_card_index()[0]);
				_logic.clean_magic_cards();
			}
		} else if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
			_logic.clean_magic_cards();
			if (player_magic_card[_seat_index] != 0) {
				_logic.add_magic_card_index(_logic.switch_to_card_index(player_magic_card[_seat_index]));
				flag |= check_hu(temp_cards_index, _logic.get_all_magic_card_index()[0]);
			} else {
				flag |= check_hu(temp_cards_index, -1);
			}
			_logic.clean_magic_cards();
		} else {
			flag |= check_hu(temp_cards_index, _logic.get_all_magic_card_index()[0]);
		}

		return flag;
	}

	private boolean check_hu(int[] cards_index, int magic_index) {
		boolean flag = false;
		if (magic_index != -1) {
			int magic_count = cards_index[magic_index];
			for (int i = 0; i <= magic_count; i++) {
				int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
				temp_cards_index[magic_index] -= i;
				flag |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(temp_cards_index, i);
			}
		}
		flag |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(cards_index, 0);
		return flag;
	}

	public int get_fan(ChiHuRight chiHuRight, int seat_index) {
		int fan = 0;
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_LOU_SHANG_LOU).is_empty()) {
			fan += 10;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_HEI_SAN_FENG).is_empty()) {
			fan += player_feng_count[seat_index];
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_ZHONG_FA_BAI).is_empty()) {
			fan += player_zfb_count[seat_index];
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_ZI_YI_SE).is_empty()) {
			fan += 30;
			// return fan;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_DA_SAN_YUAN).is_empty()) {
			fan += 30;
			// return fan;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_QI_DUI).is_empty()) { // 七小对
			fan = 10;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_DIAO_WANG).is_empty()) {
			fan += 2;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_PING_HU).is_empty()) {
			fan += 1;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_QING_YI_SE).is_empty()) {
			fan += 2;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_CHUN_YI_SE).is_empty()) {
			fan += 20;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_YI_TIAO_LONG).is_empty()) {
			fan += 10;
		}
		if (!chiHuRight.opr_and(Constants_MJ_SXHD.CHR_YING_KOU).is_empty()) {
			fan += 5;
		}

		return fan;
	}

	public int get_card_color_ex_zi(int[] cards_index, boolean has_qi_xiao_dui) {
		int color = 0;
		int[] color_flag = new int[3];
		for (int i = 0; i < GameConstants_KWX.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			if (_logic.is_magic_index(i) && !has_qi_xiao_dui) {
				continue;
			}
			color_flag[_logic.get_card_color(_logic.switch_to_card_data(i))] = 1;
		}

		for (int c = 0; c < 3; c++) {
			color += color_flag[c];
		}
		return color;
	}

	/**
	 * 一条龙牌型，能风吃
	 * 
	 * @param cards_index
	 * @param weave_count
	 * @return
	 */
	public boolean is_yi_tiao_long_fc(int[] cards_index, int weave_count, int[] magic_cards_index, int total_magic_card_indexs) {
		if (weave_count > 1) {
			return false;
		}

		int cbCardIndexTemp[] = Arrays.copyOf(cards_index, cards_index.length);

		boolean has_enough_cards = true;
		for (int i = 0; i < 9; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = false;
			// 正常胡牌
			if (_logic.get_magic_card_count() > 0) {
				int magic_count = cbCardIndexTemp[_logic.get_magic_card_index(0)];
				for (int i = 0; i <= magic_count; i++) {
					cbCardIndexTemp[_logic.get_magic_card_index(0)] -= i;
					can_win |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(cbCardIndexTemp, i);
				}
			}
			can_win |= AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, _logic.get_all_magic_card_index(),
					_logic.get_magic_card_count());

			if (can_win) {
				return true;
			}
		}

		cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);

		has_enough_cards = true;
		for (int i = 9; i < 18; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = false;
			// 正常胡牌
			if (_logic.get_magic_card_count() > 0) {
				int magic_count = cbCardIndexTemp[_logic.get_magic_card_index(0)];
				for (int i = 0; i <= magic_count; i++) {
					cbCardIndexTemp[_logic.get_magic_card_index(0)] -= i;
					can_win |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(cbCardIndexTemp, i);
				}
			}
			can_win |= AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, _logic.get_all_magic_card_index(),
					_logic.get_magic_card_count());
			if (can_win) {
				return true;
			}
		}

		cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);

		has_enough_cards = true;
		for (int i = 18; i < 27; i++) {
			if (cbCardIndexTemp[i] <= 0) {
				has_enough_cards = false;
				break;
			}
			cbCardIndexTemp[i]--;
		}

		if (has_enough_cards) {
			boolean can_win = false;
			// 正常胡牌
			if (_logic.get_magic_card_count() > 0) {
				int magic_count = cbCardIndexTemp[_logic.get_magic_card_index(0)];
				for (int i = 0; i <= magic_count; i++) {
					cbCardIndexTemp[_logic.get_magic_card_index(0)] -= i;
					can_win |= AnalyseCardUtil.analyse_feng_chi_by_cards_index_hd(cbCardIndexTemp, i);
				}
			}
			can_win |= AnalyseCardUtil.analyse_feng_chi_by_cards_index(cbCardIndexTemp, -1, _logic.get_all_magic_card_index(),
					_logic.get_magic_card_count());
			if (can_win) {
				return true;
			}
		}

		return false;
	}

	// 判断是否是大小三元
	public boolean check_da_xiao_san_yuan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		int[] magic_card = new int[] { GameConstants_KWX.HZ_MAGIC_CARD, GameConstants_KWX.BB_MAGIC_CARD, GameConstants_KWX.FC_MAGIC_CARD };
		boolean[] flag_shuang = new boolean[3];
		int count = 0;
		for (int m = 0; m < magic_card.length; m++) {
			for (int w = 0; w < weaveCount; w++) {
				if (weaveItems[w].weave_kind != GameConstants_KWX.WIK_PENG && weaveItems[w].weave_kind != GameConstants_KWX.WIK_GANG
						&& weaveItems[w].weave_kind != GameConstants_KWX.WIK_LIANG) {
					continue;
				}

				if (weaveItems[w].center_card == magic_card[m]) {
					count++;
					break;
				}
			}

			if (cards_index[_logic.switch_to_card_index(magic_card[m])] >= 3) {
				count++;
				continue;
			}
			if (cards_index[_logic.switch_to_card_index(magic_card[m])] == 2) {
				flag_shuang[m] = true;
			}
		}

		boolean da_hu = false;
		boolean flag = flag_shuang[0] | flag_shuang[1] | flag_shuang[2];
		// if (count == 2 && flag) {
		// da_hu = true;
		// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_XIAO_SAN_YUAN);
		// }
		if (count == 3) {
			return true;
			// chiHuRight.opr_or(GameConstants_KWX.CHR_HU_DA_SAN_YUAN);
		}
		return false;
	}

	// 七个对子，且万能牌只能当本身使用；
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		if (cur_card != 0) {
			int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[cbCurrentIndex]++;
		}

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// if (this._magic_card_count > 0) {
			// for (int m = 0; m < _magic_card_count; m++) {
			// // 王牌过滤
			// if (i == _logic.get_magic_card_index(m))
			// continue;
			//
			// // 单牌统计
			// if (cbCardCount == 1 || cbCardCount == 3)
			// cbReplaceCount++;
			//
			// if (cbCardCount == 4) {
			// nGenCount++;
			// }
			// }
			// } else {
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
			// }
		}

		// 王牌不够
		// if (this._magic_card_count > 0) {
		// int count = 0;
		// for (int m = 0; m < _magic_card_count; m++) {
		// count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
		// }
		//
		// if (cbReplaceCount > count) {
		// return GameConstants.WIK_NULL;
		// }
		// } else {
		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;
		// }

		if (nGenCount > 0) {
			return Constants_MJ_SXHD.CHR_QI_DUI;// 七小对
		} else {
			return Constants_MJ_SXHD.CHR_QI_DUI;
		}

	}

	// 判断是否是缺一门
	public boolean is_que_yi_men(int cards_index[], int cur_card) {
		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		return true;
	}

	// 清一色牌 缺两门胡牌，可有字牌，可有王牌
	public boolean is_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		// 胡牌判断
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			if (cards_index[i] != 0) {
				// 花色判断
				if (cbCardColor != 0xFF)
					return false;

				// 设置花色
				cbCardColor = (this._logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}
		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF && weaveCount > 0) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}
		/* && !this._logic.is_magic_card(cur_card) */
		if (cur_card != 0 && (cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor && cur_card < 0x31 && !_logic.is_magic_card(cur_card))
			return false;

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if (cbCenterCard >= 0x31) {
				continue;
			}
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	// 纯一色牌 //缺两门胡牌，且无字，如果有王牌，王牌本身也要是一样的花色
	public boolean is_chun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		// 胡牌判断
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			if (cards_index[i] != 0) {
				// 花色判断
				if (cbCardColor != 0xFF)
					return false;

				// 设置花色
				cbCardColor = (this._logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}
		}

		// 纯一色不能有字牌
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] != 0) {
				return false;
			}
		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if (cur_card != 0 && (cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
			return false;

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	// 判断是不是字一色
	public boolean is_feng_is_se(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card) {
		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			int index = _logic.switch_to_card_index(weaveItem.center_card);
			if (index < GameConstants.MAX_ZI) {
				return false;
			}
		}

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		// 插入数据
		if (cur_card != 0) {
			int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[cbCurrentIndex]++;
		}
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cbCardIndexTemp[i] > 0) {
				return false;
			}
		}

		return true;
	}

	public int filter_ting(int ting_count, int[] hu_out_card_ting_count) {
		if (!has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB) && !has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
			return ting_count;
		}

		int zi_ting_count = 0;
		for (int i = 0; i < ting_count; i++) {
			if (hu_out_card_ting_count[i] >= 0x31) {
				hu_out_card_ting_count[zi_ting_count++] = hu_out_card_ting_count[i];
			}
		}

		return zi_ting_count;
	}

	public int accpet_ying_kou(int seat_index, int ting_count, int[] hu_out_card_ting, int[] hu_out_card_ting_count, int[][] hu_out_card,
			int[] havs_kou_out_card) {
		int havs_kou_out_card_count = 0;
		boolean flag = false;
		for (int i = 0; i < ting_count; i++) {
			int[] hu_zfb = new int[3];
			boolean out_card_ying_kou = false;
			for (int j = 0; j < hu_out_card_ting_count[i]; j++) {
				if (hu_out_card[i][j] == -1) {
					hu_zfb[0] = 1;
					hu_zfb[1] = 1;
					hu_zfb[2] = 1;
					flag = true;
					break;
				}
				int card = hu_out_card[i][j] > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI ? hu_out_card[i][j] - GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI
						: hu_out_card[i][j];
				if (card >= GameConstants.HZ_MAGIC_CARD) {
					flag = true;
					hu_zfb[card % GameConstants.HZ_MAGIC_CARD] = 1;
				}
			}
			if (flag) {
				int[] cards_index = Arrays.copyOf(GRR._cards_index[seat_index], GRR._cards_index[seat_index].length);
				cards_index[_logic.switch_to_card_index(hu_out_card_ting[i])]--;
				for (int j = 0; j < 3; j++) {
					if (hu_zfb[j] == 0) {
						continue;
					}
					int[] tmep_cards_index = Arrays.copyOf(cards_index, cards_index.length);
					if (j == 0) { // 红中
						if (tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)] == 0
								|| tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)] == 0) {
							continue;
						}
						tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)]--;
						tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)]--;
					} else if (j == 1) { // 发财
						if (tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 0
								|| tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)] == 0) {
							continue;
						}
						tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)]--;
						tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 2)]--;
					} else if (j == 2) { // 白板
						if (tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)] == 0
								|| tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)] == 0) {
							continue;
						}
						tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD)]--;
						tmep_cards_index[_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD + 1)]--;
					}
					int cbTemp = analyse_chi_hu_card(tmep_cards_index, GRR._weave_items[seat_index], GRR._weave_count[seat_index], 0,
							new ChiHuRight(), Constants_MJ_SXHD.GAME_RULE_ZIMOHU, seat_index, false);

					if (cbTemp != GameConstants.WIK_NULL) {
						_ying_kou[seat_index].setLeft(false);
						out_card_ying_kou = true;
						break;
					}
				}
				cards_index[_logic.switch_to_card_index(hu_out_card_ting[i])]++;
			}
			if (out_card_ying_kou) {
				havs_kou_out_card[havs_kou_out_card_count++] = get_real_card(hu_out_card_ting[i]);
			}
		}

		return havs_kou_out_card_count;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		// 如果有红中癞子的玩法，是不需要判断红中的
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_MJ_SXHD.GAME_RULE_ZIMOHU, seat_index, false)) {
				if (_logic.switch_to_card_data(this._logic.get_magic_card_index(0)) == cbCurrentCard && has_lai_zi) {
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[count] = cbCurrentCard;
				}
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

	// 是否听牌
	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_SXJY.HU_CARD_TYPE_ZI_MO, seat_index, false))
				return true;
		}
		return false;
	}

	// 可以杠癞子
	public int analyse_gang_exclude_magic_card(int seat_index, int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean dipatch) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_ying_kou[seat_index].getLeft() != null && _ying_kou[seat_index].getLeft()
					&& _logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD) <= i) {
				continue;
			}
			// 打牌过程中，如果玩家已经碰、杠过某种花色（万筒条）的牌，则本局只能碰、杠这种花色的牌或字牌。 大将王黑风报
			if (has_rule(Constants_MJ_SXHD.GAME_RULE_QING_YI_SE) || has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)
					|| has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
				if (_magic_peng_gang_color[seat_index].getLeft() != null && _magic_peng_gang_color[seat_index].getLeft()
						&& _logic.get_card_color(_logic.switch_to_card_data(i)) != 3) {
					if (_magic_peng_gang_color[seat_index].getRight() != -1
							&& _magic_peng_gang_color[seat_index].getRight() != _logic.get_card_color(_logic.switch_to_card_data(i))) {
						continue;
					}
				}
			}
			if (cards_index[i] == 4 && check_gang_ed_4_hu(seat_index, cards_index, _logic.switch_to_card_data(i), 4, WeaveItem, cbWeaveCount)) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		// 免碰玩法是不能明杠的
		if (!this.has_rule(Constants_MJ_SXHD.GAME_RULE_MIAN_PENG)) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1) { // 癞子不能杠，少于一张牌也直接过滤
							continue;
						} else {
							if (!dipatch && get_real_card(card) == get_real_card(WeaveItem[i].center_card)) {
								continue;
							}
							if (_ying_kou[seat_index].getLeft() != null && _ying_kou[seat_index].getLeft()
									&& _logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD) >= j) {
								continue;
							}
							if (get_real_card(WeaveItem[i].center_card) == _logic.switch_to_card_data(j)
									&& check_gang_ed_4_hu(seat_index, cards_index, _logic.switch_to_card_data(j), 1, WeaveItem, cbWeaveCount)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = get_real_card(WeaveItem[i].center_card);
								gangCardResult.isPublic[index] = 1;// 明刚
								gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;

	}

	public boolean check_gang_ed_4_hu(int seat_index, int[] cards_index, int card, int remove_count, WeaveItem[] weaveItems, int weave_count) {
		if (_playerStatus[seat_index]._card_status != GameConstants.CARD_STATUS_BAO_TING
				&& _playerStatus[seat_index]._card_status != Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
			return true;
		}
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (temp_cards_index[_logic.switch_to_card_index(card)] < remove_count) {
			return false;
		}
		temp_cards_index[_logic.switch_to_card_index(card)] -= remove_count;

		if (get_ting_card(new int[GameConstants.MAX_INDEX], temp_cards_index, weaveItems, weave_count, seat_index) > 0) {
			return true;
		}
		return false;
	}

	// 洪洞麻将没有点炮胡 所有的只能自摸
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;

		// 用户状态
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// 打牌过程中，如果玩家已经碰、杠过某种花色（万筒条）的牌，则本局只能碰、杠这种花色的牌或字牌。 大将王黑风报
			if (has_rule(Constants_MJ_SXHD.GAME_RULE_QING_YI_SE) || has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)
					|| has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
				if (_magic_peng_gang_color[i].getLeft() != null && _magic_peng_gang_color[i].getLeft() && _logic.get_card_color(card) != 3) {
					if (_magic_peng_gang_color[i].getRight() != -1 && _magic_peng_gang_color[i].getRight() != _logic.get_card_color(card)) {
						continue;
					}
				}
			}

			if (GRR._left_card_count >= 0) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错

				// 免碰规则 不能碰 不能明杠 不能接杠
				if (!this.has_rule(Constants_MJ_SXHD.GAME_RULE_MIAN_PENG)) {
					// 碰牌判断
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && player_magic_card[i] != 0
							&& _logic.get_card_count_by_index(GRR._cards_index[i]) == 4) {
						action = 0;
					}
					if (action != 0 && (_playerStatus[i]._card_status != GameConstants.CARD_STATUS_BAO_TING
							&& _playerStatus[i]._card_status != Constants_MJ_SXHD.CARD_STATUS_YING_KOU)) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}

					// 杠牌判断
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (_ying_kou[i].getLeft() != null && _ying_kou[i].getLeft() && GameConstants.HZ_MAGIC_CARD <= card) {
						action = 0;
					}
					if (action != 0 && action == GameConstants.WIK_GANG
							&& check_gang_ed_4_hu(i, GRR._cards_index[i], card, 3, GRR._weave_items[i], GRR._weave_count[i])) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}

				}

			}

			// boolean can_hu = true;
			// int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
			// for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++)
			// {
			// if (tmp_cards_data[x] == card) {
			// can_hu = false;
			// break;
			// }
			// }
			//
			// if (_playerStatus[i].is_chi_hu_round() && can_hu) {
			// // 吃胡判断
			// ChiHuRight chr = GRR._chi_hu_rights[i];
			// chr.set_empty();
			// int cbWeaveCount = GRR._weave_count[i];
			// if (!has_rule(Constants_MJ_SXHD.GAME_RULE_ZIMOHU)) {
			// // 如果打出的牌是是癞子牌 就要单独的分析了
			// if (this._logic.is_magic_card(card)) {
			// action = analyse_card_jie_pao(GRR._cards_index[i],
			// GRR._weave_items[i], cbWeaveCount, card, chr,
			// Constants_MJ_SXHD.GAME_RULE_DIAN_PAO_HU, i, false);
			// } else {
			// action = analyse_chi_hu_card(GRR._cards_index[i],
			// GRR._weave_items[i], cbWeaveCount, card, chr,
			// Constants_MJ_SXHD.GAME_RULE_DIAN_PAO_HU, i, false);
			// }
			//
			// }
			//
			// // 结果判断
			// if (action != 0 && action == GameConstants.WIK_CHI_HU) {
			// _playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
			// _playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
			// bAroseAction = true;
			// }
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT; // 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	/**
	 * 接炮的胡牌算法
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
	public int analyse_card_jie_pao(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, boolean need_to_multiply) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		boolean can_win = false; // 是否能胡牌

		// 这些在能胡牌之后，用来设置CHR，否则选美时CHR会出现不能胡牌时的CHR
		boolean has_qi_xiao_dui = false;
		boolean has_qing_yi_se = false;
		boolean has_hao_hua_qi_xiao_dui = false;// 豪华七小对
		boolean need_258 = false;
		if (has_rule(Constants_MJ_SXHS.AME_RULE_HUNAN_258)) {
			need_258 = true;
		}

		// 判断是否是七对
		int check_qi_xiao_dui = this.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			need_258 = false;
			can_win = true;
			has_qi_xiao_dui = true;
		}

		// 判断是否是双豪华七对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == Constants_MJ_SXHS.CHR_HHQXD) {
			need_258 = false;
			can_win = true;
			has_hao_hua_qi_xiao_dui = true;
		}

		// 判断是不是清一色
		boolean is_qing_yi_se = this.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) {
			need_258 = false;
			has_qing_yi_se = true;

		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}
		if (_logic.is_valid_card(cur_card)) {
			tmp_hand_cards_index[_logic.switch_to_card_index(cur_card)]++;
		}

		// 如果勾选了红中癞子和红中，手中必须要有2.5.8任意一张
		boolean bValue = false;
		bValue = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (bValue) { // 如果能胡
			can_win = true;
		}
		if (can_win == false) { // 如果不能胡牌
			// 这里不能胡牌时也不能清空，因为选美胡时，需要叠加多张牌的CHR
			// chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_MJ_SXHS.GAME_RULE_ZIMOHU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_ZI_MO, need_to_multiply); // 自摸
		} else if (card_type == Constants_MJ_SXHS.GAME_RULE_DIAN_PAO_HU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_DIAN_PAO, need_to_multiply); // 点炮
		} else if (card_type == Constants_MJ_SXHS.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_QIANG_GANG, need_to_multiply); // 抢杠胡
		} else if (card_type == Constants_MJ_SXHS.HU_GANG_SHANG_KAI_HUA) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_GANG_SHANG_KAI_HUA, need_to_multiply); // 杠上开花
		}

		if (has_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_QI_XIAO_DUI, need_to_multiply);
		}
		if (has_qing_yi_se) { // 清一色
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_QING_YI_SE, need_to_multiply);
		}

		if (has_hao_hua_qi_xiao_dui) { // 豪华七对
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HHQXD, need_to_multiply);
		}

		return cbChiHuKind;

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
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		this.setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}
		if (GRR != null) {
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
			float lGangScore[] = new float[this.getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				if (GameConstants.Game_End_NORMAL == reason) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < this.getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人杠的分数
						}
					}
				}

				// 记录
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				// 每个小局的分数=胡分+杠分
				GRR._game_score[i] = GRR._game_score[i] + lGangScore[i];
				// 记录每个小局数分数的累加
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						GRR._chi_hu_card[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						GRR._cards_data[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
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
				game_end.addGameScore(GRR._game_score[i]); // 放炮的人？
				game_end.addGangScore(lGangScore[i]); // 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
				if (has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
					game_end.addJettonScore(player_magic_card[i]);
				}
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
			real_reason = GameConstants.Game_End_DRAW; // 流局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
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
			player_result.addDaHuZiMo(_player_result.zhi_gang_count[i]);
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

	public boolean PoChan() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_result.game_score[i] >= -100) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 吕梁麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_hd(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		/////////////////////////////////////////////// 算分//////////////////////////
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = get_fan(chr, seat_index);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore * wFanShu; // 自摸加1分-2018-9-20 去掉

				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					s += 1;
				}

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
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
		boolean qiang_gang_hu = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					// else if (type ==
					// GameConstants.CHR_JIANGSU_ZZ_BU_GANG_KAI) {
					// gameDesc.append(" 补杠杠开");
					// } else if (type ==
					// GameConstants.CHR_JIANGSU_ZZ_AN_GANG_KAI) {
					// gameDesc.append(" 暗杠杠开");
					// } else if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU)
					// {
					// gameDesc.append(" 抢杠胡");
					// }
					if (type == Constants_MJ_SXHD.CHR_TYPE_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_MJ_SXHD.CHR_TYPE_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_MJ_SXHD.CHR_TYPE_GANG_SHANG_KAI_HUA) {
						result.append(" 杠上花");
					}
					if (type == Constants_MJ_SXHD.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_MJ_SXHD.CHR_QI_DUI) {
						result.append(" 七对");
					}
					if (type == Constants_MJ_SXHD.CHR_DIAO_WANG) {
						result.append(" 吊王");
					}
					if (type == Constants_MJ_SXHD.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_MJ_SXHD.CHR_CHUN_YI_SE) {
						result.append(" 纯一色");
					}
					if (type == Constants_MJ_SXHD.CHR_ZI_YI_SE) {
						result.append(" 字一色");
					}
					if (type == Constants_MJ_SXHD.CHR_YI_TIAO_LONG) {
						result.append(" 一条龙");
					}
					if (type == Constants_MJ_SXHD.CHR_DA_SAN_YUAN) {
						result.append(" 大三元");
					}
					if (type == Constants_MJ_SXHD.CHR_HEI_SAN_FENG) {
						result.append(" 黑三风X").append(player_feng_count[player]);
					}
					if (type == Constants_MJ_SXHD.CHR_ZHONG_FA_BAI) {
						result.append(" 中发白X").append(player_zfb_count[player]);
					}
					if (type == Constants_MJ_SXHD.CHR_YING_KOU) {
						result.append(" 硬扣");
					}
					if (type == Constants_MJ_SXHD.CHR_JIE_PAO) {
						result.append(" 接炮");
					}
					if (type == Constants_MJ_SXHD.CHR_LOU_SHANG_LOU) {
						result.append(" 楼上楼");
					}

				} else if (type == GameConstants.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants_KWX.WIK_GANG) {
							continue;
						}
						if (tmpPlayer == player) {
							if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
								jie_gang++;
							} else {
								if (GRR._weave_items[tmpPlayer][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							if (GRR._weave_items[tmpPlayer][w].provide_player == player) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				result.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				result.append(" 补杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 明杠X" + jie_gang);
			}

			GRR._result_des[player] = result.toString();
		}
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
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		/////////////////////////////////////////////// 算分//////////////////////////
		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = get_fan(chr, seat_index);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore * wFanShu; // 自摸加1分

				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					s += 1;
				}

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

}
