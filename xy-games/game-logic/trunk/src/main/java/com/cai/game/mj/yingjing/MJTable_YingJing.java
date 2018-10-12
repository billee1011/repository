package com.cai.game.mj.yingjing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_YINGJING;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Basic.MJ_Game_End_Basic;

public class MJTable_YingJing extends AbstractMJTable {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private MJHandlerDispatchCardFirst_YingJing cardFirst_YingJing;

	private boolean[] ISHU; // 胡牌玩家
	private int hu_count; // 胡牌次数
	private int[] HU_CARD; // 每个玩家胡的牌
	public int[] qiang_gang_hu; // 抢杠胡番数
	public int dispatch_num[]; // 摸牌张数
	public int dian_pao_player[][]; // 记录给哪个玩家点炮
	public int dian_pao_count[]; // 每个玩家的点炮数量
	public int bao_hu_fan[]; // 报胡番数
	public int qing_hu_fan[]; // 请胡番数
	public int out_card_num[]; // 出牌张数
	private int[] dispatchcardNum; // 摸牌次数
	private boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index; // 下个出牌玩家
	public int lang_qi_out_count[];

	public String[] cha_da_jiao_desc; // 查大叫描述
	/**
	 * 牌桌上有人开杠时，杠组合的序号，主要用来解决弯杠的时候，杠上炮的问题
	 */
	public int gang_pai_weave_index = 0;
	/**
	 * 接炮时，记录一下番数
	 */
	public int[] fan_shu_when_jie_pao_hu;

	/**
	 * 有接炮时，不胡，再次存储一下胡牌时的番数，和fan_shu_when_jie_pao_hu进行比较
	 */
	public int[] fan_shu_when_abandoned_jie_pao;

	// /**
	// * 自摸时，记录一下番数
	// */
	// public int[] fan_shu_when_zi_mo;
	// /**
	// * 自摸时不胡，再次存储一下胡牌时的番数，和fan_shu_when_abandoned_zi_mo进行比较
	// */
	// public int[] fan_shu_when_abandoned_zi_mo;

	/**
	 * 每个玩家，接杠能胡时，先碰牌的记录
	 */
	public int[][] passed_gang_cards;
	public int[] passed_gang_count;

	/**
	 * 下一局的庄家
	 */
	public int next_banker_player;

	public boolean[] lang_qi; // 廊起
	public int[][] lang_qi_card;// 每个玩家廊起牌值
	public Set<Integer> allLangQiCard; // 所有廊起牌

	/**
	 * 暗杠，直杠，弯杠，点杠，计数
	 */
	public int[] an_gang_count;
	public int[] zhi_gang_count;
	public int[] wan_gang_count;
	public int[] dian_gang_count;

	public boolean Geng; // 跟
	public boolean FangGeng; // 反跟
	private int notGengNumber; // 没有跟的人数
	public int needBaoPei[][]; // 是否包赔
	public int needBaoPeiCount[]; // 是否包赔数量

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		if (isZhuang) {
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			if (genZhuangCard == card && dispatchcardNum[seat_index] == 2 && notGengNumber == 1) {
				FangGeng = true;
			} else if (!isDispatchcardNum(seat_index)) {
				isCanGenZhuang = false;
			}
			genZhuangCard = card;
		} else {
			if (seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else if (genZhuangCard != card) {
				notGengNumber++;
				if (notGengNumber > 1) {
					isCanGenZhuang = false;
				} else {
					next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				}
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				if (next_seat_index == _cur_banker && notGengNumber == 0) {
					Geng = true;
				}
			}
		}
	}

	/**
	 * 跟庄状态
	 * 
	 * @return the isCanGenZhuang
	 */
	public boolean isCanGenZhuang() {
		return isCanGenZhuang;
	}

	/** 摸牌数累计 */
	public void addDispatchcardNum(int seat_index) {
		dispatchcardNum[seat_index]++;
	}

	/**
	 * 是否第一次摸牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isDispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index] == 1;
	}

	/**
	 * 玩家胡牌
	 * 
	 * @param seat_index
	 */
	public void ISHUVaild(int seat_index, int huCard) {
		hu_count++;
		_player_result.pao[seat_index] = hu_count;
		// ISHU[seat_index] = true;
		HU_CARD[seat_index] = huCard;
		sendClientRecordHuInfo(seat_index);
	}

	public int getHuCard(int seat_index) {
		return HU_CARD[seat_index];
	}

	public void ISHUVaild(int seat_index) {
		ISHU[seat_index] = true;
	}

	public void add_dispatchcard_num(int seat_index) {
		dispatch_num[seat_index]++;
	}

	public boolean is_first_dispatchcard_num(int seat_index) {
		return dispatch_num[seat_index] == 1;
	}

	public void add_out_card_num(int seat_index) {
		out_card_num[seat_index]++;
	}

	public int getLangQiCount(int seat_index) {
		int count = 0;
		for (int i = 0; i < lang_qi_card[seat_index].length; i++) {
			if (lang_qi_card[seat_index][i] != 0) {
				count++;
			}
		}
		return count;
	}

	public int getHuIndex() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (hu_count == _player_result.pao[i]) {
				return i;
			}
		}
		return -1;
	}

	public boolean getMoreHuIndex(int out_player_index) {
		if (hu_count == 3) {
			return false;
		}

		int index = (out_player_index + getTablePlayerNumber() + 2) % getTablePlayerNumber();
		if (ISHU[index]) {
			return false;
		}
		return true;
	}

	/**
	 * 定下局庄
	 * 
	 * @param seat_index
	 */
	public void chang_zhang(int seat_index, boolean needChang) {
		if (hu_count == 0 || needChang) {
			next_banker_player = seat_index;
		}
	}

	/**
	 * @return the iSHU
	 */
	public boolean getISHU(int seat_index) {
		return ISHU[seat_index];
	}

	/**
	 * 判断需不需要结束
	 * 
	 * @return
	 */
	public boolean needFinish() {
		int falseCount = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (!ISHU[i]) {
				falseCount++;
			}
		}
		boolean flag = false;
		if (falseCount == 1) {
			flag = true;
		}

		return flag;
	}

	public MJTable_YingJing() {
		super(MJType.GAME_TYPE_MJ_XTDGK);
	}

	@Override
	protected void onInitTable() {
		cardFirst_YingJing = new MJHandlerDispatchCardFirst_YingJing();
		_handler_dispath_card = new MJHandlerDispatchCard_YingJing();
		_handler_out_card_operate = new MJHandlerOutCardOperate_YingJing();
		_handler_gang = new MJHandlerGang_YingJing();
		_handler_chi_peng = new MJHandlerChiPeng_YingJing();
		passed_gang_cards = new int[getTablePlayerNumber()][20];
		passed_gang_count = new int[getTablePlayerNumber()];
		qing_hu_fan = new int[getTablePlayerNumber()];
		cha_da_jiao_desc = new String[getTablePlayerNumber()];
		dian_pao_count = new int[getTablePlayerNumber()];
		dian_pao_player = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		Geng = false;
		FangGeng = false;
		notGengNumber = 0;
		allLangQiCard = new HashSet<Integer>();

		dispatchcardNum = new int[getTablePlayerNumber()];

		ISHU = new boolean[getTablePlayerNumber()];
		HU_CARD = new int[getTablePlayerNumber()];
		qiang_gang_hu = new int[getTablePlayerNumber()];
		dispatch_num = new int[getTablePlayerNumber()];
		out_card_num = new int[getTablePlayerNumber()];
		bao_hu_fan = new int[getTablePlayerNumber()];
		lang_qi = new boolean[getTablePlayerNumber()];
		lang_qi_card = new int[getTablePlayerNumber()][11];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ISHU[i] = false;
			lang_qi[i] = false;
			HU_CARD[i] = GameConstants.INVALID_CARD;
			_player_result.pao[i] = 0;
			_player_result.qiang[i] = 0;
			_player_result.nao[i] = 0;
			dian_pao_count[i] = 0;
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				dian_pao_player[i][k] = GameConstants.INVALID_SEAT;
			}
			bao_hu_fan[i] = 0;
		}
	}

	public void addPassedGang(int seat_index, int card) {
		passed_gang_cards[seat_index][passed_gang_count[seat_index]] = card;
		passed_gang_count[seat_index]++;
	}

	/**
	 * 判断是否其他玩家炮牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean isLangQiPaoPai(int seat_index, int card) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			// 胡牌后取消限定的牌
			if (_player_result.pao[i] > 0) {
				continue;
			}
			if (!lang_qi[i]) {
				continue;
			}
			int ting_count = _playerStatus[i]._hu_card_count;

			for (int k = 0; k < ting_count; k++) {
				if (_playerStatus[i]._hu_cards[k] == card) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 是否过杠的牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean isPassedGang(int seat_index, int card) {
		for (int i = 0; i < passed_gang_count[seat_index]; i++) {
			int card_temp = passed_gang_cards[seat_index][i];
			if (card == card_temp) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getTablePlayerNumber() {
		return GameConstants_YINGJING.GAME_PLAYER;
	}

	public int getNextPalyerIndex(int seat_index) {
		int nextPalyerIndex = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
		boolean flag = false;
		do {
			if (ISHU[nextPalyerIndex]) {
				nextPalyerIndex = (nextPalyerIndex + getTablePlayerNumber() + 1) % getTablePlayerNumber();
			} else {
				flag = true;
			}
		} while (!flag);
		return nextPalyerIndex;
	}

	/**
	 * 洗牌发牌
	 */
	@Override
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

		// 分发扑克
		for (int i = 0; i < count; i++) {
			// 最大手牌数量
			send_count = (GameConstants_YINGJING.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	public void initParam() {
		ISHU = new boolean[getTablePlayerNumber()];
		HU_CARD = new int[getTablePlayerNumber()];
		qiang_gang_hu = new int[getTablePlayerNumber()];
		dispatch_num = new int[getTablePlayerNumber()];
		out_card_num = new int[getTablePlayerNumber()];
		dian_pao_player = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		dian_pao_count = new int[getTablePlayerNumber()];
		bao_hu_fan = new int[getTablePlayerNumber()];
		lang_qi = new boolean[getTablePlayerNumber()];
		lang_qi_card = new int[getTablePlayerNumber()][11];
		needBaoPei = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		needBaoPeiCount = new int[getTablePlayerNumber()];
		isCanGenZhuang = true;
		allLangQiCard = new HashSet<Integer>();
		dispatchcardNum = new int[getTablePlayerNumber()];
		lang_qi_out_count = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ISHU[i] = false;
			lang_qi[i] = false;
			HU_CARD[i] = GameConstants.INVALID_CARD;
			_player_result.pao[i] = 0;
			_player_result.qiang[i] = 0;
			_player_result.nao[i] = 0;
			dian_pao_count[i] = 0;
			for (int k = 0; k < getTablePlayerNumber(); k++) {
				needBaoPei[i][k] = -1;
				dian_pao_player[i][k] = GameConstants.INVALID_SEAT;
			}
			bao_hu_fan[i] = 0;
		}
		notGengNumber = 0;
		Geng = false;
		FangGeng = false;
		hu_count = 0;
		next_banker_player = GameConstants.INVALID_SEAT;
		fan_shu_when_jie_pao_hu = new int[getTablePlayerNumber()];
		fan_shu_when_abandoned_jie_pao = new int[getTablePlayerNumber()];
		// fan_shu_when_zi_mo = new int[getTablePlayerNumber()];
		// fan_shu_when_abandoned_zi_mo = new int[getTablePlayerNumber()];

		an_gang_count = new int[getTablePlayerNumber()];
		zhi_gang_count = new int[getTablePlayerNumber()];
		wan_gang_count = new int[getTablePlayerNumber()];
		dian_gang_count = new int[getTablePlayerNumber()];
		cha_da_jiao_desc = new String[getTablePlayerNumber()];
		gang_pai_weave_index = 0;

	}

	/**
	 * 庄家选择
	 */
	@Override
	public void progress_banker_select() {
		if (_cur_round == 1) {
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	protected boolean on_game_start() {
		initParam();
		handler_refresh_all_player_data();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			sendClientRecordHuInfo(i);
		}
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_YINGJING.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			sendClientRecordHuInfo(i);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_YINGJING.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

			if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants.GAME_RULE_ALL_RANDOM_INDEX)) {
				this.load_room_info_data(roomResponse);
				this.load_common_status(roomResponse);

				if (this._cur_round == 1) {
					this.load_player_info_data(roomResponse);
				}
			}

			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_YINGJING.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		exe_dispatch_card_first(_current_player, GameConstants.WIK_NULL, 0);
		return false;
	}

	public int analyse_qi_xiao_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		return GameConstants_YINGJING.CHR_QI_DUI;
	}

	public boolean is_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (_logic.is_magic_index(i)) {
				continue;
			}
			if (cards_index[i] == 0) {
				continue;
			}

			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		if (_logic.exist_eat(weave_items, weave_count))
			return false;

		for (int i = 0; i < weave_count; i++) {
			int color = _logic.get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = _logic.get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		return true;
	}

	public boolean has_yao_jiu(int[] cards_data, int card_count, WeaveItem[] weaveItems, int weaveCount, int type) {
		boolean flag = false;
		for (int i = 0; i < card_count; i++) {
			int value = _logic.get_card_value(cards_data[i]);
			if (type == 1) {
				if (_logic.is_magic_card(cards_data[i])) {
					continue;
				}
			}
			if (value == 9 || value == 1) {
				flag = true;
			}
		}

		for (int i = 0; i < weaveCount; i++) {
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if (value == 9 || value == 1) {
				flag = true;
			}
		}

		return flag;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {

		return GameConstants.WIK_CHI_HU;
	}

	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index, int provide_index) {
		// 构造数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {

			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		int temp_cards[] = new int[GameConstants_YINGJING.MAX_COUNT];
		_logic.switch_to_cards_data(cbCardIndexTemp, temp_cards);
		int[] magic_cards_index = _logic.get_all_magic_card_index();
		int magic_count = _logic.get_magic_card_count();

		if (magic_count > 2) { // 一般只有两种癞子牌存在
			magic_count = 2;
		}

		for (int i = 0; i < magic_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 数据分析
		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index,
				magic_count);
		// 暗5对
		boolean can_win_qi_dui = false;
		boolean qing_hu = checkQingHua(cbCardIndexTemp);
		if (qing_hu) {
			can_win_qi_dui = true;
		}
		boolean longWu = checkQingHuaLong(cbCardIndexTemp);
		if (!can_win && !can_win_qi_dui && !longWu) {
			return GameConstants.WIK_NULL;
		}
		// 清一色
		boolean can_win_qing_yi_se = _logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weave_count);
		// 对对胡
		boolean can_win_dd_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index,
				magic_count);
		// 带幺九
		boolean can_win_yao_jiu = AnalyseCardUtil.analyse_win_yao_jiu(cbCardIndexTemp, -1, magic_cards_index,
				magic_count) && is_yao_jiu_weave(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
		// 将一色
		boolean is_jiang_yi_se = is_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		// 天胡
		boolean can_win_tian_hu = false;
		// 地胡-庄家打出的第一张牌
		boolean can_win_di_hu = false;
		// 天点地
		boolean can_tian_dian_di = false;

		if (is_first_dispatchcard_num(_seat_index) && _seat_index == GRR._banker_player && _out_card_count == 0) {
			can_win_tian_hu = true;
		} else if (_seat_index == GRR._banker_player && _out_card_count == 1) {
			can_win_tian_hu = true;
		}
		// 地胡
		if (_playerStatus[_seat_index].is_bao_ting()) {
			can_win_di_hu = true;
		}

		// 天点地
		if (_playerStatus[_seat_index].is_bao_ting() && _seat_index != GRR._banker_player) {
			if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO && _out_card_count == 1) {
				can_tian_dian_di = true;
			}
		}

		// 根
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] == 4) {
				chiHuRight.an_ka_count++;
			}
		}
		// 组合中的根
		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
				if (weaveItems[i].center_card == cur_card) {
					chiHuRight.an_ka_count++;
				} else if (cards_index[_logic.switch_to_card_index(weaveItems[i].center_card)] > 0) {
					chiHuRight.an_ka_count++;
				}
			}
		}

		// 456卡
		// if ((GRR._left_card_count == 0 && card_type ==
		// GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO)
		// || card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO
		// || card_type == GameConstants_YINGJING.HU_CARD_TYPE_QIANG_GANG
		// || card_type == GameConstants_YINGJING.HU_CARD_TYPE_GANG_PAO) {
		// if (_logic.get_card_value(cur_card) == 5) {
		int index = _logic.switch_to_card_index(cur_card);
		int[] cardIndexTemp = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);
		if (cbCardIndexTemp[index - 1] > 0 && cbCardIndexTemp[index + 1] > 0) {
			cardIndexTemp[index - 1]--;
			cardIndexTemp[index + 1]--;
			cardIndexTemp[index]--;
			boolean can_win1 = AnalyseCardUtil.analyse_win_by_cards_index(cardIndexTemp, -1, magic_cards_index,
					magic_count);
			if (can_win1) {
				chiHuRight.ka_count++;
			}
		}
		// }
		/*
		 * } else { if (!can_win_qi_dui) { chiHuRight.ka_count =
		 * ka_count(cbCardIndexTemp, magic_cards_index, magic_count); } }
		 */

		if (can_win_dd_hu) { // 3番
			if (is_jiang_yi_se) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_JIANG_DUI);
			} else {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_DUI_DUI_HU);
			}
		} else if (longWu) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_LONG_DUI);
		} else if (can_win_qi_dui) { // 2 番 暗5对
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_QI_DUI);
		}
		// 带幺九
		if (can_win_yao_jiu) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_DAI_YAO_JIU);
		}
		// 天点地
		if (can_tian_dian_di) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_TIAN_DIAN_DI);
		} else {
			// 能胡的情况下再算天胡地胡
			if (can_win_tian_hu) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_TIAN_HU);
			}
			if (can_win_di_hu) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_DI_HU);
			}
		}

		// 清一色
		if (can_win_qing_yi_se) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_QING_YI_SE);
		}

		if (!can_win_qing_yi_se && chiHuRight.is_empty()) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_JI_HU);
		}

		if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_GANG_KAI);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_GANG_PAO);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_JIE_PAO);
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_QIANG_GANG);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_ZI_MO);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_JIE_PAO);
		}

		if (GRR._left_card_count == 0) {
			if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_HAI_DI_PAO);
			} else {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_HAI_DI_HUA);
			}
		}

		int geng_count = get_geng_count(cards_index, weaveItems, weave_count, cur_card);
		int fan_shu = get_fan_shu(chiHuRight, geng_count, _seat_index, provide_index);
		// 记录最大番数
		if (!isZiMo(card_type)) {
			fan_shu_when_jie_pao_hu[_seat_index] = 1 << fan_shu;
		}

		return GameConstants.WIK_CHI_HU;
	}

	public int analyse_chi_hu_card_cha(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index, int provide_index) {
		// 构造数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {

			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}
		int temp_cards[] = new int[GameConstants_YINGJING.MAX_COUNT];
		_logic.switch_to_cards_data(cbCardIndexTemp, temp_cards);
		int[] magic_cards_index = _logic.get_all_magic_card_index();
		int magic_count = _logic.get_magic_card_count();

		if (magic_count > 2) { // 一般只有两种癞子牌存在
			magic_count = 2;
		}

		for (int i = 0; i < magic_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 数据分析
		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index,
				magic_count);
		// 暗5对
		boolean can_win_qi_dui = false;
		boolean qing_hu = checkQingHua(cbCardIndexTemp);
		if (qing_hu) {
			can_win_qi_dui = true;
		}

		boolean longWu = checkQingHuaLong(cbCardIndexTemp);
		if (!can_win && !can_win_qi_dui && !longWu) {
			return GameConstants.WIK_NULL;
		}
		// 清一色
		boolean can_win_qing_yi_se = _logic.is_qing_yi_se_qishou(cbCardIndexTemp, weaveItems, weave_count);
		// 对对胡
		boolean can_win_dd_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index,
				magic_count);
		// 带幺九
		boolean can_win_yao_jiu = AnalyseCardUtil.analyse_win_yao_jiu(cbCardIndexTemp, -1, magic_cards_index,
				magic_count) && is_yao_jiu_weave(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
		// 将一色
		boolean is_jiang_yi_se = is_jiang_yi_se(cbCardIndexTemp, weaveItems, weave_count);
		// 天胡
		boolean can_win_tian_hu = false;
		// 地胡-庄家打出的第一张牌
		boolean can_win_di_hu = false;
		// 天点地
		boolean can_tian_dian_di = false;

		if (is_first_dispatchcard_num(_seat_index) && _seat_index == GRR._banker_player && _out_card_count == 0) {
			can_win_tian_hu = true;
		} else if (_seat_index == GRR._banker_player && _out_card_count == 1) {
			can_win_tian_hu = true;
		}
		// 地胡
		if (_playerStatus[_seat_index].is_bao_ting()) {
			can_win_di_hu = true;
		}

		// 天点地
		if (_playerStatus[_seat_index].is_bao_ting() && _seat_index != GRR._banker_player) {
			if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO && _out_card_count == 1) {
				can_tian_dian_di = true;
			}
		}

		// 根
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				chiHuRight.an_ka_count++;
			}
		}
		// 组合中的根
		for (int i = 0; i < weave_count; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_PENG) {
				if (cbCardIndexTemp[_logic.switch_to_card_index(weaveItems[i].center_card)] > 0) {
					chiHuRight.an_ka_count++;
				}
			}
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				chiHuRight.an_ka_count++;
			}
		}

		int index = _logic.switch_to_card_index(cur_card);
		int[] cardIndexTemp = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);
		if (cbCardIndexTemp[index - 1] > 0 && cbCardIndexTemp[index + 1] > 0) {
			cardIndexTemp[index - 1]--;
			cardIndexTemp[index + 1]--;
			cardIndexTemp[index]--;
			boolean can_win1 = AnalyseCardUtil.analyse_win_by_cards_index(cardIndexTemp, -1, magic_cards_index,
					magic_count);
			if (can_win1) {
				chiHuRight.ka_count++;
			}
		}

		if (can_win_dd_hu) { // 3番
			if (is_jiang_yi_se) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_JIANG_DUI);
			} else {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_DUI_DUI_HU);
			}
		} else if (longWu) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_LONG_DUI);
		} else if (can_win_qi_dui) { // 2 番 暗5对
			if (can_win_qi_dui) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_QI_DUI);
			}
		}
		// 带幺九
		if (can_win_yao_jiu) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_DAI_YAO_JIU);
		}
		// 天点地
		if (can_tian_dian_di) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_TIAN_DIAN_DI);
		} else {
			// 能胡的情况下再算天胡地胡
			if (can_win_tian_hu) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_TIAN_HU);
			}
			if (can_win_di_hu) {
				chiHuRight.opr_or(GameConstants_YINGJING.CHR_DI_HU);
			}
		}

		// 清一色
		if (can_win_qing_yi_se) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_QING_YI_SE);
		}

		if (!can_win_qing_yi_se && chiHuRight.is_empty()) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_JI_HU);
		}

		if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_GANG_KAI);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_GANG_PAO) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_GANG_PAO);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_JIE_PAO);
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_QIANG_GANG);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_ZI_MO);
		} else if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_YINGJING.CHR_JIE_PAO);
		}

		// if (GRR._left_card_count == 0) {
		// if (card_type == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO) {
		// chiHuRight.opr_or(GameConstants_YINGJING.CHR_HAI_DI_PAO);
		// } else {
		// chiHuRight.opr_or(GameConstants_YINGJING.CHR_HAI_DI_HUA);
		// }
		// }

		int geng_count = get_geng_count(cards_index, weaveItems, weave_count, cur_card);
		int fan_shu = get_fan_shu(chiHuRight, geng_count, _seat_index, provide_index);
		// 记录最大番数
		if (!isZiMo(card_type)) {
			fan_shu_when_jie_pao_hu[_seat_index] = 1 << fan_shu;
		}

		return GameConstants.WIK_CHI_HU;
	}

	public boolean is_yao_jiu_weave(WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			if (_logic.get_card_value(weave_items[i].center_card) != 1
					&& _logic.get_card_value(weave_items[i].center_card) != 9) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 456卡数量
	 * 
	 * @param card_index
	 * @return
	 */
	public int ka_count(int[] card_index, int[] magic_cards_index, int magic_count) {
		int count = 0;

		// 有癞子模式
		int[] cardIndexTemp = Arrays.copyOf(card_index, card_index.length);
		// 判断基础值
		for (int i = 0; i < GameConstants.MAX_ZI; i += 9) {
			int start_index = _logic.switch_to_card_index(0x04) + i;
			int min = 4;
			for (int j = 0; j < 3; j++) {
				int index = start_index + j;
				if (cardIndexTemp[index] < min) {
					min = cardIndexTemp[index];
				}
			}

			for (int u = 0; u < min; u++) {
				// 扣除手牌
				cardIndexTemp[start_index] -= 1;
				cardIndexTemp[start_index + 1] -= 1;
				cardIndexTemp[start_index + 2] -= 1;
				// 平胡
				boolean can_win1 = AnalyseCardUtil.analyse_win_by_cards_index(cardIndexTemp, -1, magic_cards_index,
						magic_count);
				if (can_win1) {
					count += 1;
				} else {

				}
			}

		}

		return count;
	}

	public void rmLangQiCard(int seat_index) {
		// lang_qi_card[seat_index] = new int[11];
		allLangQiCard.clear();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 胡牌后取消限定的牌
			if (_player_result.pao[i] > 0) {
				continue;
			}
			if (!lang_qi[i]) {
				continue;
			}
			int ting_count = _playerStatus[i]._hu_card_count;

			for (int k = 0; k < ting_count; k++) {
				allLangQiCard.add(_playerStatus[i]._hu_cards[k]);
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 清理报警按钮
			if (_player_result.is_bao_hu(i)) {
				int cards_temp[] = new int[GameConstants_YINGJING.MAX_COUNT];
				int hand_card_count_temp = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards_temp);
				for (int k = 0; k < hand_card_count_temp; k++) {
					if (!allLangQiCard.contains(cards_temp[k])) {
						_player_result.qiang[i] = 0;
						handler_refresh_player_data_room(i);
						break;
					}
				}
			}
			operate_show_card(i, GameConstants_YINGJING.SHOW_CARD_LANG_QI, GameConstants.INVALID_SEAT);
		}
	}

	public boolean handler_refresh_player_data_room(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		// this.send_response_to_player(seat_index, roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 算分
	 */
	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], operate_card);
		int wFanShu = get_fan_shu(chr, geng_count, seat_index, provide_index);

		countCardType(chr, seat_index);

		int lChiHuScore = (int) Math.pow(2, wFanShu) * get_di_feng();// wFanShu*m_pGameServiceOption->lCellScore;

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
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;//
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index || ISHU[i]) {
					continue;
				}

				wFanShu = get_fan_shu(chr, geng_count, seat_index, i);
				lChiHuScore = (int) Math.pow(2, wFanShu) * get_di_feng();// wFanShu*m_pGameServiceOption->lCellScore;
				int s = lChiHuScore;
				if (has_rule(GameConstants_YINGJING.GAME_RULE_ZIMO_JIA_DI)) {
					int maxfan = 4;
					if (has_rule(GameConstants_YINGJING.GAME_RULE_FENG_DING_3)) {
						maxfan = 3;
					}
					if ((int) Math.pow(2, maxfan) < lChiHuScore) {
						s += get_di_feng();
					}
				}
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			int s = lChiHuScore;
			GRR._game_score[provide_index] -= s;

			GRR._game_score[seat_index] += s;
			boolean isBaoPei = false;
			for (int i = 0; i < needBaoPeiCount[provide_index]; i++) {
				if (seat_index == needBaoPei[provide_index][i]) {
					isBaoPei = true;
				}
			}
			// 点炮包赔规则
			if (isBaoPei) {
				int maxfan = 4;
				if (has_rule(GameConstants_YINGJING.GAME_RULE_FENG_DING_3)) {
					maxfan = 3;
				}
				int bao_pei_score = (int) Math.pow(2, maxfan) * get_di_feng();
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == provide_index) {
						continue;
					}

					// 胡牌分
					GRR._game_score[provide_index] -= bao_pei_score;
					GRR._game_score[i] += bao_pei_score;
				}
			}
			// 记录点炮玩家index
			dian_pao_player[provide_index][dian_pao_count[provide_index]] = seat_index;
			dian_pao_count[provide_index]++;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_YINGJING.CHR_FANG_PAO);

		}
		GRR._provider[seat_index] = provide_index;

		// 设置变量

		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		return;

	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			boolean kaFlag = true;
			boolean gengFlag = true;
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants_YINGJING.CHR_ZI_MO) {
						des += " 自摸 ";
						if (has_rule(GameConstants_YINGJING.GAME_RULE_ZIMO_JIA_DI)) {
							des += " 自摸加底+" + get_di_feng();
						} else if (has_rule(GameConstants_YINGJING.GAME_RULE_ZIMO_JIA_FAN)) {
							des += " 自摸加番X1";
						}
					}

					if (type == GameConstants_YINGJING.CHR_JI_HU) {
						des += " 平胡";
					}

				} else {
					if (type == GameConstants_YINGJING.CHR_FANG_PAO) {
						des += " 放炮 ";
					}
				}
			}

			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants_YINGJING.CHR_DUI_DUI_HU) {
						des += " 对对胡X1";
					}
					if (type == GameConstants_YINGJING.CHR_QING_YI_SE) {
						des += " 清一色X2";
					}

					if (type == GameConstants_YINGJING.CHR_TIAN_HU) {
						des += " 天胡X2";
					}

					if (type == GameConstants_YINGJING.CHR_DI_HU) {
						des += " 地胡X1";
					}

					if (type == GameConstants_YINGJING.CHR_GANG_KAI) {
						des += " 杠开X2";
					}
					if (type == GameConstants_YINGJING.CHR_GANG_PAO) {
						des += " 杠上炮X1";
					}
					if (type == GameConstants_YINGJING.CHR_QIANG_GANG) {
						des += " 抢杠胡X1";
					}
					if (type == GameConstants_YINGJING.CHR_LONG_DUI) {
						des += " 龙对X3";
					}
					if (type == GameConstants_YINGJING.CHR_JIANG_DUI) {
						des += " 将对X3";
					}

					if (type == GameConstants_YINGJING.CHR_DAI_YAO_JIU) {
						des += " 带幺九X2";
					}

					if (type == GameConstants_YINGJING.CHR_HAI_DI_HUA) {
						des += " 海底花X1";
					}
					if (type == GameConstants_YINGJING.CHR_HAI_DI_PAO) {
						des += " 海底炮X1";
					}
					if (type == GameConstants_YINGJING.CHR_TIAN_DIAN_DI) {
						des += " 天点地X1";
					}
					if (type == GameConstants_YINGJING.CHR_LANG_QI) {
						des += " 廊起X1";
					}
					if (type == GameConstants_YINGJING.CHR_QI_DUI) {
						des += " 暗5对X2";
					}

					if (GRR._chi_hu_rights[i].an_ka_count > 0 && gengFlag) {
						des += " 根X1";
						gengFlag = false;
					}
					if (GRR._chi_hu_rights[i].ka_count > 0 && kaFlag) {
						des += " 夹X1";
						kaFlag = false;
					}

				}
			}
			if (GRR._banker_player == i) {
				if (Geng) {
					des += " 被跟";
				}
				if (FangGeng) {
					des += " 反跟";
				}
			}
			if (_player_result.is_bao_hu(i)) {
				des += " 报警";
			}
			if (_player_result.is_qing_hu(i)) {
				des += " 廊起X1";
			}
			for (int k = 0; k < dian_pao_count[i]; k++) {
				des += " 点" + _player_result.pao[dian_pao_player[i][k]] + "胡";
				boolean isBaoPei = false;
				for (int u = 0; u < needBaoPeiCount[i]; u++) {
					if (dian_pao_player[i][k] == needBaoPei[i][u]) {
						isBaoPei = true;
					}
				}
				if (isBaoPei) {
					des += " 包赔:" + _player_result.pao[dian_pao_player[i][k]] + "胡";
				}
			}

			if (an_gang_count[i] > 0) {
				des += " 暗杠X" + an_gang_count[i];
				// des += " 暗杠";
			}
			if (wan_gang_count[i] > 0) {
				des += " 明杠X" + wan_gang_count[i];
				// des += " 明杠";
			}
			if (dian_gang_count[i] > 0) {
				des += " 放杠";
				// des += " 放杠X" + dian_gang_count[i];
			}
			if (zhi_gang_count[i] > 0) {
				des += " 接杠X" + zhi_gang_count[i];
				// des += " 接杠";
			}

			if (cha_da_jiao_desc[i] != null && !cha_da_jiao_desc[i].isEmpty()) {
				des += " " + cha_da_jiao_desc[i];
			}

			GRR._result_des[i] = des;
		}

	}

	/**
	 * 查大叫描述
	 */
	protected String set_result_describe_cha(ChiHuRight chr, int seat_index) {
		int l = chr.type_count;
		long type = 0;
		String des = "查大叫：";

		for (int j = 0; j < l; j++) {
			type = chr.type_list[j];

			if (type == GameConstants_YINGJING.CHR_DUI_DUI_HU) {
				des += " 对对胡X1";
			}
			if (type == GameConstants_YINGJING.CHR_QING_YI_SE) {
				des += " 清一色X2";
			}

			if (type == GameConstants_YINGJING.CHR_TIAN_HU) {
				des += " 天胡X2";
			}

			if (type == GameConstants_YINGJING.CHR_DI_HU) {
				des += " 地胡X1";
			}

			if (type == GameConstants_YINGJING.CHR_GANG_KAI) {
				des += " 杠开X2";
			}
			if (type == GameConstants_YINGJING.CHR_GANG_PAO) {
				des += " 杠上炮X1";
			}
			if (type == GameConstants_YINGJING.CHR_QIANG_GANG) {
				des += " 抢杠胡X1";
			}
			if (type == GameConstants_YINGJING.CHR_LONG_DUI) {
				des += " 龙对X3";
			}
			if (type == GameConstants_YINGJING.CHR_JIANG_DUI) {
				des += " 将对X2";
			}

			if (type == GameConstants_YINGJING.CHR_DAI_YAO_JIU) {
				des += " 带幺九X2";
			}

			if (type == GameConstants_YINGJING.CHR_TIAN_DIAN_DI) {
				des += " 天点地X1";
			}
			// if (type == GameConstants_YINGJING.CHR_LANG_QI) {
			// des += " 廊起X1";
			// }
			if (type == GameConstants_YINGJING.CHR_QI_DUI) {
				des += " 暗5对X2";
			}

		}

		if (des.length() <= 7) {
			des += " 平胡";
		}
		int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], 0);
		if (chr.an_ka_count > 0 && geng_count == 0) {
			des += " 根X" + chr.an_ka_count;
		}
		if (chr.ka_count > 0) {
			des += " 夹X" + chr.ka_count;
		}
		return des;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int l = GameConstants.MAX_ZI;

		for (int i = 0; i < l; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index, GameConstants.INVALID_SEAT)) {
				cards[count] = cbCurrentCard;
				count++;
			}

		}

		if (count > 0 && count < l) {
		} else if (count == l) {
			count = 1;
			cards[0] = -1;
		}
		return count;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	public boolean operate_player_action(int seat_index, boolean close, boolean isNotWait) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		roomResponse.setIsGoldRoom(isNotWait);// 暂时用金币场这个字段
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
	 * 检测牌是不是廊起的牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean checkLangQiCard(int seat_index, int card) {
		for (int i = 0; i < lang_qi_card[seat_index].length; i++) {
			if (lang_qi_card[seat_index][i] == card) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 玩家出牌的动作检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card, int card_type) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i || ISHU[i]) {
				continue;
			}

			playerStatus = _playerStatus[i];
			// 碰牌判断
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && _playerStatus[seat_index].is_chi_peng_round() && !_player_result.is_qing_hu(i)
					&& !_playerStatus[i].is_bao_ting()) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);

				// 有杠而且杠完不换章
				if (action != 0 && !checkLangQiCard(i, card)) {
					if (!_player_result.is_bao_hu(i) && !_player_result.is_qing_hu(i)
							&& !_playerStatus[i].is_bao_ting()) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, i, 1);// 加上杠
						bAroseAction = true;
					} else {
						if (check_gang_huan_zhang(i, card) == false) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, i, 1);// 加上杠
							bAroseAction = true;
						}

					}
				}
			}

			if (card_type == GameConstants.INVALID_SEAT) {
				card_type = GameConstants.HU_CARD_TYPE_PAOHU;
			}
			// 可以胡的情况 判断
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type,
					i, seat_index);

			// 结果判断
			if (action != 0) {
				// 超过之前番数才能点炮
				if ((fan_shu_when_jie_pao_hu[i] > fan_shu_when_abandoned_jie_pao[i]
						&& !_playerStatus[i].is_chi_hu_round()) || _playerStatus[i].is_chi_hu_round()) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
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
	 * 是否自摸
	 * 
	 * @return
	 */
	public boolean isZiMo(int cardType) {
		boolean flag = true;
		if (cardType == GameConstants_YINGJING.HU_CARD_TYPE_JIE_PAO
				|| cardType == GameConstants_YINGJING.HU_CARD_TYPE_GANG_PAO
				|| cardType == GameConstants_YINGJING.HU_CARD_TYPE_QIANG_GANG) {
			flag = false;
		}
		return flag;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		// 回放时、查牌时，牌桌上胡牌的顺序
		MJ_Game_End_Basic.Builder basic_game_end = MJ_Game_End_Basic.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			basic_game_end.addPao(_player_result.pao[i]);
		}
		game_end.setCommResponse(PBUtil.toByteString(basic_game_end));
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
			float lGangScore[] = new float[getTablePlayerNumber()];

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
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == GRR._banker_player) {
					continue;
				}
				int score = 1;
				if (has_rule(GameConstants_YINGJING.GAME_RULE_ZIMO_JIA_DI)) {
					score += 1;
				}
				if (Geng) {
					GRR._game_score[i] += score;
					GRR._game_score[GRR._banker_player] -= score * 1;
				}
				if (FangGeng) {
					GRR._game_score[GRR._banker_player] += score * 2;
					GRR._game_score[i] -= score * 2;
				}
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// GRR._game_score[i] += lGangScore[i];
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

			// 鸟的数据 这里要注意三人场的特殊处理 三人场必须发四个人的鸟 不然显示不全
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
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
				for (int j = 0; j < GameConstants_YINGJING.MAX_COUNT; j++) {
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.qiang[i] = 0;
		}
		handler_refresh_all_player_data();
		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);
		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}
		// 删除
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#trustee_timer(int, int)
	 */
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	/**
	 * 请胡检测
	 * 
	 * @param card_index
	 * @return
	 */
	public boolean checkQingHua(int[] card_index) {
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (card_index[i] == 4) {
				return false;
			}
			if (card_index[i] >= 2) {
				count += card_index[i] / 2;
			}
		}
		if (count == 5) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (card_index[i] == 3) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean checkQingHuaLong(int[] card_index) {
		int count = 0;
		int si_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (card_index[i] == 4) {
				si_count++;
			}
			if (card_index[i] >= 2) {
				count += card_index[i] / 2;
			}
		}
		if (count == 5) {
			if (si_count == 1) {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					if (card_index[i] == 3) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected void test_cards() {

		// int cards[] = new int[] { 0x11, 0x11, 0x12, 0x12, 0x14, 0x14, 0x15,
		// 0x15, 0x22, 0x22 };
		// int cards1[] = new int[] { 0x11, 0x11, 0x12, 0x12, 0x14, 0x14, 0x15,
		// 0x15, 0x22, 0x22 };
		// int cards2[] = new int[] { 0x11, 0x11, 0x12, 0x12, 0x14, 0x14, 0x15,
		// 0x15, 0x22, 0x22 };
		// int cards4[] = new int[] { 0x11, 0x11, 0x12, 0x12, 0x14, 0x14, 0x15,
		// 0x15, 0x22, 0x22 };
		//
		// // int cards[] = new int[]
		// // {0x12,0x12,0x12,0x13,0x14,0x16,0x16,0x25,0x26,0x27};
		//
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
		// GRR._cards_index[i][j] = 0;
		// }
		// }
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < cards.length; j++) {
		// int card = 0;
		// if (i == 0) {
		// card = cards[j];
		// } else if (i == 1) {
		// card = cards1[j];
		// } else if (i == 2) {
		// card = cards2[j];
		// } else if (i == 3) {
		// card = cards4[j];
		// }
		// GRR._cards_index[i][_logic.switch_to_card_index(card)] += 1;
		// }
		// }

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/
		/*int[] realyCards = new int[] { 37, 20, 24, 39, 34, 21, 33, 40, 40, 36, 18, 22, 25, 19, 37, 35, 40, 21, 20, 17,
				35, 36, 17, 35, 40, 35, 34, 18, 38, 21, 24, 39, 37, 25, 36, 19, 39, 24, 33, 22, 37, 41, 23, 25, 41, 17,
				17, 22, 20, 33, 38, 18, 41, 20, 24, 19, 18, 34, 23, 41, 33, 38, 23, 23, 38, 34, 25, 39, 19, 22, 21,
				36 };
		testRealyCard(realyCards);*/

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
	@Override
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
			send_count = (GameConstants_YINGJING.MAX_COUNT - 1);
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
	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_YINGJING.MAX_COUNT - 1; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false; // 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	@Override
	public boolean exe_dispatch_card(int seat_index, int type, int delay) {
		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(this.getRoom_id(), seat_index, type, false), delay,
					TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this.set_handler(this._handler_dispath_card);
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
	public boolean exe_dispatch_card_first(int seat_index, int type, int delay) {
		// 发牌
		this.set_handler(this.cardFirst_YingJing);
		this.cardFirst_YingJing.reset_status(seat_index, type);
		this._handler.exe(this);
		return true;
	}

	/**
	 * 通知客户端更新数据
	 * 
	 * @param seat_index
	 */
	public void sendClientRecordHuInfo(int seat_index) {
		handler_refresh_all_player_data();
	}

	/**
	 * 显示胡的牌
	 * 
	 * @param hu_index
	 * @param card
	 */
	public void roomResponseShowHuCard(int hu_index) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int card = HU_CARD[i];
			if (card == GameConstants.INVALID_CARD) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
			roomResponse.setCardType(GameConstants.Show_Card_Si_Chuan);
			roomResponse.addCardData(card);
			roomResponse.setCardCount(1);
			roomResponse.setTarget(i);
			roomResponse.setOperateLen(_logic.get_card_count_by_index(GRR._cards_index[i]));

			if (hu_index == i) {
				operate_player_get_card(i, 1, new int[] { card }, i);
			}

			// if(GRR._chi_hu_rights[i].opr_and(GameConstants_YINGJING.CHR_ZI_MO).is_empty()){
			//
			// }
			send_response_to_other(i, roomResponse);
			// GRR.add_room_response(roomResponse);
		}
		// send_response_to_room(roomResponse,false);
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {

		// 胡牌走后移除廊起的牌
		rmLangQiCard(seat_index);
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
				GameConstants.INVALID_SEAT);

		// 手牌删掉
		// this.operate_player_cards(seat_index, 0, null, 0, null);
		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}
		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards, GameConstants.MAX_INDEX);
		// 刷新一下数据
		this.operate_player_cards(seat_index, hand_card_count, cards, 0, null, false);
		roomResponseShowHuCard(seat_index);
		// 回放数据
		cards[hand_card_count++] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;

		RoomResponse.Builder tmp_roomResponse = RoomResponse.newBuilder();
		tmp_roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		tmp_roomResponse.setTarget(seat_index);
		tmp_roomResponse.setCardType(GameConstants.Show_Card_Si_Chuan);
		tmp_roomResponse.setCardCount(hand_card_count);

		this.load_player_info_data(tmp_roomResponse);
		for (int j = 0; j < hand_card_count; j++) {
			tmp_roomResponse.addCardData(cards[j]);
		}

		GRR.add_room_response(tmp_roomResponse);
		return;
	}

	@Override
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
			}
		}

		PlayerResultResponse.Builder player_result = PlayerResultResponse.newBuilder();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.cha_da_jiao[i]); // 查大次数
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
	 * 牌局结束亮全部手牌
	 */
	public void process_show_hand_card() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			if (getISHU(i)) {
				cards[hand_card_count++] = GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_HU;
			}

			this.operate_player_cards_not(i, 0, null, 0, null);
			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
	}

	/**
	 * 查大叫，流局时，没听牌的玩家，配付给听牌玩家，最大的牌型分，胡牌了的，不用管
	 */
	// public int cha_da_jiao() {
	// int[] max_pai_xing_fen = new int[getTablePlayerNumber()];
	// boolean[] is_ting_state = new boolean[getTablePlayerNumber()];
	// List<Integer> not_ting = new ArrayList<Integer>();
	// boolean[] cha_jiao = new boolean[getTablePlayerNumber()];
	// //先记录停牌次数
	// int countNotHu = 0;
	// int tingCount = 0;
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// max_pai_xing_fen[i] = -1;
	//
	// if (getISHU(i))
	// continue;
	// countNotHu++;
	// if(is_ting_card(GRR._cards_index[i], GRR._weave_items[i],
	// GRR._weave_count[i], i)){
	// tingCount++;
	// }
	// }
	//
	// boolean type = false;
	// if(countNotHu == tingCount){
	// type = true;
	// }
	//
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// max_pai_xing_fen[i] = -1;
	//
	// if (getISHU(i))
	// continue;
	//
	// is_ting_state[i] = is_ting_card(GRR._cards_index[i], GRR._weave_items[i],
	// GRR._weave_count[i], i);
	//
	// if (is_ting_state[i]) {
	// if(!type || (type && !_player_result.is_qing_hu(i))){
	// // 获取最大牌型分
	// max_pai_xing_fen[i] = get_max_pai_xing_fen(i) * get_di_feng();
	// }else{
	// not_ting.add(i);
	// }
	// } else {
	// not_ting.add(i);
	// }
	// }
	//
	// // 赔付
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// if (getISHU(i))
	// continue;
	// if (is_ting_state[i]){
	// if(!type || (type && !_player_result.is_qing_hu(i))){
	// continue;
	// }
	// }
	//
	// for (int j = 0; j < getTablePlayerNumber(); j++) {
	// if (i == j)
	// continue;
	//
	// if (getISHU(j))
	// continue;
	// cha_jiao[j] = true;
	// // 记录
	// GRR._game_score[j] += max_pai_xing_fen[j];
	// GRR._game_score[i] -= max_pai_xing_fen[j];
	// }
	// }
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// if (cha_jiao[i]) {
	// _player_result.cha_da_jiao[i] += 1;
	// }
	// }
	//
	// // 选庄
	// int return_player = GameConstants.INVALID_SEAT;
	// if (next_banker_player == GameConstants.INVALID_SEAT) {
	// if (not_ting.size() == 0) {
	// return_player = GRR._banker_player;
	// } else if (not_ting.size() == getTablePlayerNumber() - 1) {
	// for (int i = 0; i < getTablePlayerNumber(); i++) {
	// if (is_ting_state[i]) {
	// return_player = i;
	// break;
	// }
	// }
	// } else {
	// int num = RandomUtil.generateRandomNumber(0, not_ting.size());
	// return_player = not_ting.get(num == not_ting.size() ? num - 1 : num);
	// }
	// }
	// return return_player;
	// }

	/**
	 * 查大叫，流局时，没听牌的玩家，配付给听牌玩家，最大的牌型分，胡牌了的，不用管
	 */
	public int cha_da_jiao() {
		int[] max_pai_xing_fen = new int[getTablePlayerNumber()];
		boolean[] is_ting_state = new boolean[getTablePlayerNumber()];
		List<Integer> not_ting = new ArrayList<Integer>();
		boolean[] cha_jiao = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			max_pai_xing_fen[i] = -1;

			if (getISHU(i))
				continue;

			is_ting_state[i] = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

			if (is_ting_state[i]) {
				// 获取最大牌型分
				max_pai_xing_fen[i] = get_max_pai_xing_fen(i) * get_di_feng();
			} else {
				not_ting.add(i);
			}
		}

		int[] peiFuPlayer = new int[getTablePlayerNumber()];
		Arrays.fill(peiFuPlayer, -1);
		// 赔付
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (getISHU(i))
				continue;

			if (is_ting_state[i])
				continue;

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (i == j)
					continue;

				if (getISHU(j))
					continue;
				if (!is_ting_state[j])
					continue;
				cha_jiao[j] = true;
				// 记录
				GRR._game_score[j] += max_pai_xing_fen[j];
				GRR._game_score[i] -= max_pai_xing_fen[j];
				peiFuPlayer[j] = 1;
			}
		}

		// 9.17 新规则：4人未胡，3人听牌不需要赔付廊起查叫
		// 9.26 规则修改 廊起的给已听牌没廊起的查叫只有在都听牌了的情况下才有。
		int count = 0;
		int huCount = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (getISHU(i)) {
				huCount++;
			}
			if (is_ting_state[i]) {
				count++;
			}
		}
		boolean flag = true;
		if (getTablePlayerNumber() - huCount != count) {
			flag = false;
		}
		if (flag) {
			// 赔付: 听牌廊起玩家付给听牌未廊起玩家查叫分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (getISHU(i) || !(is_ting_state[i] && _player_result.is_qing_hu(i))) {
					continue;
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (i == j || getISHU(j) || !is_ting_state[j])
						continue;
					if (_player_result.is_qing_hu(j)) {
						continue;
					}

					cha_jiao[j] = true;
					int maxfan = 0;
					if (has_rule(GameConstants_YINGJING.GAME_RULE_FENG_DING_3)) {
						maxfan = 3;
					} else {
						maxfan = 4;
					}
					int socre = max_pai_xing_fen[j];
					// 记录 *2是对方廊起番
					if (max_pai_xing_fen[j] < (1 << maxfan)) {
						socre *= 2;
					}
					GRR._game_score[j] += socre;
					GRR._game_score[i] -= socre;
					peiFuPlayer[j] = 1;
				}
			}
		}
		// 清空未赔付玩家的描述
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (peiFuPlayer[i] == -1) {
				cha_da_jiao_desc[i] = "";
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (cha_jiao[i]) {
				_player_result.cha_da_jiao[i] += 1;
			}
		}

		// 选庄
		int return_player = GameConstants.INVALID_SEAT;
		if (next_banker_player == GameConstants.INVALID_SEAT) {
			if (not_ting.size() == 0) {
				return_player = GRR._banker_player;
			} else if (not_ting.size() == getTablePlayerNumber() - 1) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (is_ting_state[i]) {
						return_player = i;
						break;
					}
				}
			} else {
				int num = RandomUtil.generateRandomNumber(0, not_ting.size());
				return_player = not_ting.get(num == not_ting.size() ? num - 1 : num);
			}
		}
		return return_player;
	}

	public int get_di_feng() {
		int di = 1;

		return di;
	}

	public int get_max_pai_xing_fen(int seat_index) {
		int max_score = -1;

		int cbCurrentCard = -1;

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);

			// 如果当前牌，不是听牌数据里的数据。这样能节省很多时间。
			if (!is_ting_card(cbCurrentCard, seat_index)) {
				continue;
			}

			ChiHuRight chr = new ChiHuRight();
			chr.set_empty();

			int card_type = GameConstants_YINGJING.HU_CARD_TYPE_ZIMO;

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_cha(GRR._cards_index[seat_index],
					GRR._weave_items[seat_index], GRR._weave_count[seat_index], cbCurrentCard, chr, card_type,
					seat_index, GameConstants.INVALID_SEAT)) {
				int geng_count = get_geng_count(GRR._cards_index[seat_index], GRR._weave_items[seat_index],
						GRR._weave_count[seat_index], cbCurrentCard);
				int fan_shu = get_fan_shu_cha(chr, geng_count, seat_index, GameConstants.INVALID_SEAT);
				int score = 1 << fan_shu;

				if (score > max_score) {
					// get_fan_shu_cha(chr, geng_count, seat_index,
					// GameConstants.INVALID_SEAT);
					cha_da_jiao_desc[seat_index] = set_result_describe_cha(chr, seat_index);
					max_score = score;
				}
			}
		}

		return max_score;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants_YINGJING.HU_CARD_TYPE_ZIMO, seat_index, GameConstants.INVALID_SEAT)) {
				return true;
			}
		}
		return false;
	}

	public int get_fan_shu(ChiHuRight chr, int geng_count, int seat_index, int provide_index) {
		int fan = 0;
		if (!chr.opr_and(GameConstants_YINGJING.CHR_DUI_DUI_HU).is_empty()) {
			fan += 1;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_QI_DUI).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_DAI_YAO_JIU).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_JIANG_DUI).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_LONG_DUI).is_empty()) {
			fan += 3;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_QING_YI_SE).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_TIAN_HU).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_HAI_DI_HUA).is_empty()) {
			fan += 1;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_HAI_DI_PAO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_DI_HU).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_TIAN_DIAN_DI).is_empty()) {
			fan += 1;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_GANG_KAI).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_GANG_PAO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_QIANG_GANG).is_empty()) {
			fan += 1;
		}

		// 自摸加番
		if (!chr.opr_and(GameConstants_YINGJING.CHR_ZI_MO).is_empty()
				&& has_rule(GameConstants_YINGJING.GAME_RULE_ZIMO_JIA_FAN)) {
			fan += 1;
		}
		// 廊起1番
		if (lang_qi[seat_index]) {
			fan += 1;
		}
		if (provide_index == GameConstants.INVALID_SEAT) {
			if (!chr.opr_and(GameConstants_YINGJING.CHR_ZI_MO).is_empty()) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index || ISHU[i]) {
						continue;
					}
					if (lang_qi[i]) {
						fan += 1;
						break;
					}
				}
			}
		} else {
			if (lang_qi[provide_index] && lang_qi_out_count[provide_index] > 1) {
				fan += 1;
			}
		}

		// 报听
		if (provide_index == GameConstants.INVALID_SEAT) {
			if (!chr.opr_and(GameConstants_YINGJING.CHR_ZI_MO).is_empty()) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index || ISHU[i]) {
						continue;
					}
					if (_playerStatus[i].is_bao_ting()) {
						fan += 1;
						break;
					}
				}
			}
		} else {
			if (_playerStatus[provide_index].is_bao_ting()) {
				fan += 1;
			}
		}

		// 杠番
		fan += geng_count > 0 ? 1 : 0;
		// 根番
		fan += chr.an_ka_count > 0 ? 1 : 0;
		// 夹番
		fan += chr.ka_count > 0 ? 1 : 0;

		int maxfan = 0;
		if (has_rule(GameConstants_YINGJING.GAME_RULE_FENG_DING_3)) {
			maxfan = 3;
		} else {
			maxfan = 4;
		}
		if (maxfan < fan) {
			fan = maxfan;
		}

		return fan;
	}

	public int get_fan_shu_cha(ChiHuRight chr, int geng_count, int seat_index, int provide_index) {
		int fan = 0;
		if (!chr.opr_and(GameConstants_YINGJING.CHR_DUI_DUI_HU).is_empty()) {
			fan += 1;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_QI_DUI).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_DAI_YAO_JIU).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_JIANG_DUI).is_empty()) {
			fan += 3;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_LONG_DUI).is_empty()) {
			fan += 3;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_QING_YI_SE).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_TIAN_HU).is_empty()) {
			fan += 2;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_HAI_DI_HUA).is_empty()) {
			fan += 1;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_HAI_DI_PAO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_DI_HU).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_TIAN_DIAN_DI).is_empty()) {
			fan += 1;
		}

		if (!chr.opr_and(GameConstants_YINGJING.CHR_GANG_KAI).is_empty()) {
			fan += 2;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_GANG_PAO).is_empty()) {
			fan += 1;
		}
		if (!chr.opr_and(GameConstants_YINGJING.CHR_QIANG_GANG).is_empty()) {
			fan += 1;
		}

		// 自摸加番
		// if (!chr.opr_and(GameConstants_YINGJING.CHR_ZI_MO).is_empty()
		// && has_rule(GameConstants_YINGJING.GAME_RULE_ZIMO_JIA_FAN)) {
		// fan += 1;
		// }
		// 廊起1番
		if (lang_qi[seat_index]) {
			fan += 1;
		}

		// 报听
		/*
		 * if (provide_index == GameConstants.INVALID_SEAT) { if
		 * (!chr.opr_and(GameConstants_YINGJING.CHR_ZI_MO).is_empty()) { for
		 * (int i = 0; i < getTablePlayerNumber(); i++) { if (i == seat_index ||
		 * ISHU[i]) { continue; } if (_playerStatus[i].is_bao_ting()) { fan +=
		 * 1; break; } } } } else { if
		 * (_playerStatus[provide_index].is_bao_ting()) { fan += 1; } }
		 */

		/*
		 * // 杠番 fan += geng_count > 0 ? 1 : 0;
		 */
		// 根番
		fan += chr.an_ka_count > 0 ? 1 : 0;
		// 夹番
		fan += chr.ka_count > 0 ? 1 : 0;

		int maxfan = 0;
		if (has_rule(GameConstants_YINGJING.GAME_RULE_FENG_DING_3)) {
			maxfan = 3;
		} else {
			maxfan = 4;
		}
		if (maxfan < fan) {
			fan = maxfan;
		}

		return fan;
	}

	public int get_geng_count(int[] cards_index, WeaveItem[] weave_items, int weave_count, int hu_card) {
		int geng = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (hu_card > 0 && hu_card < 0x29)
			cbCardIndexTemp[_logic.switch_to_card_index(hu_card)]++;

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_GANG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 4;
			}
			if (weave_items[i].weave_kind == GameConstants.WIK_PENG) {
				int card_index = _logic.switch_to_card_index(weave_items[i].center_card);
				cbCardIndexTemp[card_index] += 3;
			}
		}

		// for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
		// if (cbCardIndexTemp[i] == 4) {
		// geng++;
		// }
		// }

		return geng;
	}

	public boolean is_ting_card(int card, int seat_index) {
		int count = _playerStatus[seat_index]._hu_card_count;
		for (int i = 0; i < count; i++) {
			int tmp_card = get_real_card(_playerStatus[seat_index]._hu_cards[i]);
			if (card == tmp_card) {
				return true;
			}
		}

		if (count == 1 && _playerStatus[seat_index]._hu_cards[0] == -1) {
			// 全听
			return true;
		}

		return false;
	}

	/**
	 * 杠分结算
	 */
	@SuppressWarnings("unused")
	public void process_gang_score() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (getISHU(i) || is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i)) {

				for (int w_index = 0; w_index < GRR._weave_count[i]; w_index++) {

					if (GRR._weave_items[i][w_index].weave_kind == GameConstants.WIK_GANG
							&& GRR._weave_items[i][w_index].is_vavild) {
						int zhuan_yi_fen = 0;
						int cbGangIndex = GRR._gang_score[i].gang_count++;

						if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_AN_GANG) {
							for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
								if (i == o_player)
									continue;

								if (GRR._weave_items[i][w_index].gang_gei_fen_valid[o_player] == false)
									continue;

								int score = 2 * get_di_feng();

								GRR._game_score[o_player] -= score;
								GRR._game_score[i] += score;
								zhuan_yi_fen += score;
							}

							_player_result.an_gang_count[i]++;
							GRR._gang_score[i].an_gang_count++;
						}

						if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_JIE_GANG) {
							/*
							 * for (int o_player = 0; o_player <
							 * getTablePlayerNumber(); o_player++) { if (i ==
							 * o_player) continue;
							 */

							if (GRR._weave_items[i][w_index].gang_gei_fen_valid[GRR._weave_items[i][w_index].provide_player] == false)
								continue;

							int score = get_di_feng();
							/*
							 * if (o_player ==
							 * GRR._weave_items[i][w_index].provide_player)
							 * score *= 2;
							 */

							GRR._game_score[GRR._weave_items[i][w_index].provide_player] -= score;
							GRR._game_score[i] += score;
							zhuan_yi_fen += score;
							// }

							_player_result.ming_gang_count[i]++;
							GRR._gang_score[i].ming_gang_count++;
						}

						if (GRR._weave_items[i][w_index].type == GameConstants.GANG_TYPE_ADD_GANG) {
							for (int o_player = 0; o_player < getTablePlayerNumber(); o_player++) {
								if (i == o_player)
									continue;

								if (GRR._weave_items[i][w_index].gang_gei_fen_valid[o_player] == false)
									continue;

								int score = get_di_feng();

								GRR._game_score[o_player] -= score;
								GRR._game_score[i] += score;
								zhuan_yi_fen += score;
							}

							_player_result.ming_gang_count[i]++;
							GRR._gang_score[i].ming_gang_count++;
						}

						int zhuan_yi_seat = GRR._weave_items[i][w_index].gang_jie_pao_seat;

						if (zhuan_yi_seat != -1) {
							GRR._game_score[i] -= zhuan_yi_fen;
							GRR._game_score[zhuan_yi_seat] += zhuan_yi_fen;
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i || ISHU[i])
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 可以胡的情况 判断
			// 吃胡判断
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					GameConstants_YINGJING.HU_CARD_TYPE_QIANG_GANG, i, seat_index);

			// 结果判断
			if (action != 0) {
				// 超过之前番数才能点炮
				if ((fan_shu_when_jie_pao_hu[i] > fan_shu_when_abandoned_jie_pao[i]
						&& !_playerStatus[i].is_chi_hu_round()) || _playerStatus[i].is_chi_hu_round()) {
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

	/**
	 * 杠牌分析 (分析手中的牌是否有杆(暗杆 加杆))
	 * 
	 * @param cards_index
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @param check_weave
	 *            --是否需要检查碰的牌（加杆） markColor 不检测某种花色
	 * @return
	 */
	public int analyse_gang_card(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave, int seat_index) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗杠
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave == true) {
			// 组合杠牌
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					if (cards_index[_logic.switch_to_card_index(WeaveItem[i].center_card)] == 1) {
						cbActionMask |= GameConstants.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明杠
						gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					}
				}
			}

		}

		return cbActionMask;
	}

	public boolean check_gang_huan_zhang(int seat_index, int card) {
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];

		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];
		int hu_card_count = 0;

		hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], seat_index);

		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count > 0) {
			return false;
		} else {
			return true;
		}
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
	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		// 廊起玩家刷新
		if (_player_result.is_qing_hu(seat_index)) {
			return true;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);
		int lang_qi_count = 0;
		if (lang_qi[seat_index]) {
			for (int i = 0; i < lang_qi_card[seat_index].length; i++) {
				if (lang_qi_card[seat_index][i] == 0) {
					continue;
				}
				lang_qi_count++;
			}
		}
		int realCardCount = card_count - lang_qi_count;
		// 手牌数量
		roomResponse.setCardCount(realCardCount);
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

		int cards_temp[] = Arrays.copyOf(cards, card_count);
		int lang_qi_cards_temp[] = Arrays.copyOf(lang_qi_card[seat_index], lang_qi_card[seat_index].length);
		int real_cards[] = Arrays.copyOf(cards, card_count);
		if (lang_qi[seat_index]) {
			for (int i = 0; i < lang_qi_cards_temp.length; i++) {
				if (lang_qi_cards_temp[i] == 0) {
					continue;
				}
				for (int u = 0; u < cards_temp.length; u++) {
					if (cards_temp[u] == 0) {
						continue;
					}
					if (lang_qi_cards_temp[i] == cards_temp[u]) {
						lang_qi_cards_temp[i] = 0;
						cards_temp[u] = 0;
						break;
					}
				}
			}
			real_cards = new int[GameConstants_YINGJING.MAX_COUNT];
			int[] cards_index_temp = new int[GameConstants.MAX_INDEX];
			for (int u = 0; u < cards_temp.length; u++) {
				if (cards_temp[u] == 0) {
					continue;
				}
				cards_index_temp[_logic.switch_to_card_index(cards_temp[u])]++;
			}

			realCardCount = _logic.switch_to_cards_data(cards_index_temp, real_cards);
		}
		boolean flag = true;
		for (int j = 0; j < realCardCount; j++) {
			if (real_cards[j] != 0 && !allLangQiCard.contains(real_cards[j])) {
				flag = false;
			}
		}

		roomResponse.setIsXiangGong(flag);
		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < realCardCount; j++) {
			roomResponse.addCardData(real_cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		// 打出去可以听牌的个数
		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(
					_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

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
	@Override
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
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
				.get(1105);
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
		} else if (to_player == GameConstants.INVALID_CARD) {
			operate_player_cards_record(seat_index, 2);

			GRR.add_room_response(roomResponse);
			return this.send_response_to_other(seat_index, roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}

	}

	/**
	 * 扩展协议处理
	 * 
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_YJMJ_LANG_QI) {
			if (_player_result.is_qing_hu(seat_index)) {
				logger.error("玩家已廊起成功,无法再次廊起");
				return false;
			}
			// 无出牌流程
			if (room_rq.getOperateCard() == GameConstants.INVALID_SEAT) {

				List<Integer> outCardList = room_rq.getOutCardsList();
				int outCardCount = room_rq.getOutCardCount();
				int card_idnex[] = new int[GameConstants.MAX_INDEX];

				// 构造数组
				if (outCardCount > 0 && outCardList != null) {
					for (int i = 0; i < outCardCount; i++) {
						int real_card = get_real_card(outCardList.get(i));
						card_idnex[_logic.switch_to_card_index(real_card)]++;
					}
				}

				// 校验是否符合廊起规则
				boolean flag = false;
				int ting_card_cout = _playerStatus[seat_index]._hu_card_count;
				int count = 0;
				for (int j = 0; j < ting_card_cout; j++) {
					int tingCard = _playerStatus[seat_index]._hu_cards[j];

					if (analyse_chi_hu_card(card_idnex, GRR._weave_items[seat_index], GRR._weave_count[seat_index],
							tingCard, new ChiHuRight(), type, seat_index,
							GameConstants.INVALID_SEAT) != GameConstants.WIK_NULL) {
						count++;
					}
				}
				if (ting_card_cout == count) {
					flag = true;
					// 记录廊起胡牌
					for (int k = 0; k < ting_card_cout; k++) {
						allLangQiCard.add(_playerStatus[seat_index]._hu_cards[k]);
					}
				}

				if (!flag) {
					operate_show_card_error(seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI, seat_index);
					return false;
				}
				if (!checkOperate(GameConstants_YINGJING.WIK_LNAG_QI2, seat_index)) {
					logger.error("玩家已廊起成功,无法再次廊起");
					return false;
				}
				int cards[] = new int[GameConstants_YINGJING.MAX_COUNT];
				_logic.switch_to_cards_data(card_idnex, cards);
				lang_qi_card[seat_index] = cards;
				lang_qi[seat_index] = true;
				_player_result.qing_hu_valid(seat_index);

				handler_refresh_all_player_data();
				boolean flag1 = false;
				// 发送操作
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_playerStatus[i].has_action() && !_playerStatus[i].is_respone()) {
						flag1 = true;
					}
				}
				if (!flag1) {
					// 报警判断
					operate_show_card(seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI, GameConstants.INVALID_SEAT);
				} else {
					operate_show_card_null(seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI,
							GameConstants.INVALID_SEAT);
				}
				// 刷新自己手牌
				int cards_out[] = new int[GameConstants_YINGJING.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards_out);
				operate_player_cards(seat_index, hand_card_count, cards_out, 0, null, false);
				// 再执行发牌
				exe_dispatch_card(seat_index, GameConstants.WIK_NULL, 0);
				return true;
			}

			int card = get_real_card(room_rq.getOperateCard());
			int operate_code = GameConstants_YINGJING.WIK_LNAG_QI;

			List<Integer> outCardList = room_rq.getOutCardsList();
			int outCardCount = room_rq.getOutCardCount();
			int card_idnex[] = new int[GameConstants.MAX_INDEX];

			// 构造数组
			if (outCardCount > 0 && outCardList != null) {
				for (int i = 0; i < outCardCount; i++) {
					int real_card = get_real_card(outCardList.get(i));
					card_idnex[_logic.switch_to_card_index(real_card)]++;
				}
			}

			// 校验是否符合廊起规则
			int ting_count = _playerStatus[seat_index]._hu_out_card_count;
			boolean flag = false;
			for (int i = 0; i < ting_count; i++) {
				int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
				if (_playerStatus[seat_index]._hu_out_card_ting[i] == card) {
					int count = 0;
					for (int j = 0; j < ting_card_cout; j++) {
						int tingCard = _playerStatus[seat_index]._hu_out_cards[i][j];

						if (analyse_chi_hu_card(card_idnex, GRR._weave_items[seat_index], GRR._weave_count[seat_index],
								tingCard, new ChiHuRight(), type, seat_index,
								GameConstants.INVALID_SEAT) != GameConstants.WIK_NULL) {
							count++;
						}
					}
					if (ting_card_cout == count) {
						flag = true;
						// 记录廊起胡牌
						for (int k = 0; k < ting_card_cout; k++) {
							allLangQiCard.add(_playerStatus[seat_index]._hu_out_cards[i][k]);
						}
					}
				}
			}

			if (!flag) {
				operate_show_card_error(seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI, seat_index);
				return false;
			}

			if (!checkOperate(card, operate_code, seat_index)) {
				logger.error("玩家已廊起成功,无法再次廊起");
				return false;
			}
			int cards[] = new int[GameConstants_YINGJING.MAX_COUNT];
			_logic.switch_to_cards_data(card_idnex, cards);
			lang_qi_card[seat_index] = cards;
			lang_qi[seat_index] = true;
			_player_result.qing_hu_valid(seat_index);

			handler_refresh_all_player_data();
			boolean flag1 = false;
			// 发送操作
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_playerStatus[i].has_action() && !_playerStatus[i].is_respone()) {
					flag1 = true;
				}
			}
			if (!flag1) {
				// 报警判断
				operate_show_card(seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI, GameConstants.INVALID_SEAT);
				exe_out_card(seat_index, card, GameConstants.WIK_NULL);
				// for (int i = 0; i < getTablePlayerNumber(); i++) {
				// if(i == seat_index){
				// continue;
				// }
				// sendBaoJing(i,false,false);
				// }
			} else {
				operate_show_card_null(seat_index, GameConstants_YINGJING.SHOW_CARD_LANG_QI,
						GameConstants.INVALID_SEAT);
				// 刷新自己手牌
				int cards_out[] = new int[GameConstants_YINGJING.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards_out);
				operate_player_cards(seat_index, hand_card_count, cards_out, 0, null);
			}

		}
		return true;

	}

	/**
	 * 报警检测
	 */
	public void sendBaoJing(int seat_index, boolean isTing, boolean isDis, int send_card) {
		// 玩家已报警
		if (_player_result.is_bao_hu(seat_index)) {
			return;
		}
		int cards_temp[] = new int[GameConstants_YINGJING.MAX_COUNT];
		int hand_card_count_temp = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards_temp);
		int realCards[] = getRealCards(cards_temp, hand_card_count_temp, seat_index);
		Set<Integer> allLangQiCardTemp = new HashSet<Integer>();
		for (int k = 0; k < getTablePlayerNumber(); k++) {
			if (k == seat_index) {
				continue;
			}
			if (!_player_result.is_qing_hu(k)) {
				continue;
			}
			int ting_count = _playerStatus[k]._hu_card_count;
			for (int i = 0; i < ting_count; i++) {
				allLangQiCardTemp.add(_playerStatus[k]._hu_cards[i]);
			}
		}

		if (_player_result.is_qing_hu(seat_index)) {
			if (!allLangQiCardTemp.contains(send_card)) {
				return;
			}
		} else {
			if (isTing) {
				return;
			}
			for (int i = 0; i < hand_card_count_temp; i++) {
				if (!allLangQiCardTemp.contains(cards_temp[i])) {
					return;
				}
			}
		}
		_player_result.bao_hu_valid(seat_index);
		handler_refresh_all_player_data();
	}

	public void rmLangQiGang(int seat_index, int card) {
		int[] langQiTemp = new int[11];
		int count = 0;
		for (int i = 0; i < 11; i++) {
			int card_temp = lang_qi_card[seat_index][i];
			if (card_temp == 0 || card_temp == card) {
				continue;
			}
			langQiTemp[count++] = card_temp;
		}

		lang_qi_card[seat_index] = langQiTemp;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌 自动过滤廊起的牌
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[], boolean isPeng) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);
		int lang_qi_count = 0;
		if (lang_qi[seat_index]) {
			for (int i = 0; i < lang_qi_card[seat_index].length; i++) {
				if (lang_qi_card[seat_index][i] == 0) {
					continue;
				}
				lang_qi_count++;
			}
		}
		int realCardCount = card_count - lang_qi_count;
		// 手牌数量
		roomResponse.setCardCount(realCardCount);
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

		int cards_temp[] = Arrays.copyOf(cards, card_count);
		int lang_qi_cards_temp[] = Arrays.copyOf(lang_qi_card[seat_index], lang_qi_card[seat_index].length);
		int real_cards[] = Arrays.copyOf(cards, card_count);
		if (lang_qi[seat_index]) {
			for (int i = 0; i < lang_qi_cards_temp.length; i++) {
				if (lang_qi_cards_temp[i] == 0) {
					continue;
				}
				for (int u = 0; u < cards_temp.length; u++) {
					if (cards_temp[u] == 0) {
						continue;
					}
					if (lang_qi_cards_temp[i] == cards_temp[u]) {
						lang_qi_cards_temp[i] = 0;
						cards_temp[u] = 0;
						break;
					}
				}
			}
			real_cards = new int[GameConstants_YINGJING.MAX_COUNT];
			int[] cards_index_temp = new int[GameConstants.MAX_INDEX];
			for (int u = 0; u < cards_temp.length; u++) {
				if (cards_temp[u] == 0) {
					continue;
				}
				cards_index_temp[_logic.switch_to_card_index(cards_temp[u])]++;
			}

			realCardCount = _logic.switch_to_cards_data(cards_index_temp, real_cards);
		}
		boolean flag = true;
		for (int j = 0; j < realCardCount; j++) {
			if (real_cards[j] != 0 && !allLangQiCard.contains(real_cards[j])) {
				flag = false;
			}
		}

		roomResponse.setIsXiangGong(flag);
		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < realCardCount; j++) {
			roomResponse.addCardData(real_cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public int[] getRealCards(int cards[], int card_count, int seat_index) {
		int cards_temp[] = Arrays.copyOf(cards, card_count);
		int lang_qi_cards_temp[] = Arrays.copyOf(lang_qi_card[seat_index], lang_qi_card[seat_index].length);
		int real_cards[] = Arrays.copyOf(cards, card_count);
		if (lang_qi[seat_index]) {
			for (int i = 0; i < lang_qi_cards_temp.length; i++) {
				if (lang_qi_cards_temp[i] == 0) {
					continue;
				}
				for (int u = 0; u < cards_temp.length; u++) {
					if (cards_temp[u] == 0) {
						continue;
					}
					if (lang_qi_cards_temp[i] == cards_temp[u]) {
						lang_qi_cards_temp[i] = 0;
						cards_temp[u] = 0;
						break;
					}
				}
			}
			real_cards = new int[GameConstants_YINGJING.MAX_COUNT];
			int[] cards_index_temp = new int[GameConstants.MAX_INDEX];
			for (int u = 0; u < cards_temp.length; u++) {
				if (cards_temp[u] == 0) {
					continue;
				}
				cards_index_temp[_logic.switch_to_card_index(cards_temp[u])]++;
			}

			card_count = _logic.switch_to_cards_data(cards_index_temp, real_cards);
		}
		Arrays.fill(cards, 0);
		for (int i = 0; i < real_cards.length; i++) {
			cards[i] = real_cards[i];
		}
		return cards;
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
	public boolean operate_player_cards_not(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
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

	/**
	 * 廊起操作校验
	 * 
	 * @param card
	 * @param code
	 * @param seat_index
	 * @return
	 */
	public boolean checkOperate(int card, int code, int seat_index) {
		if (_logic.is_valid_card(card) == false) {
			log_error("出牌,牌型出错");
			return false;
		}
		if (_current_player != seat_index) {
			log_error("不是当前玩家玩家操作");
			return false;
		}

		// 删除扑克
		if (_logic.remove_card_by_index(GRR._cards_index[seat_index], card) == false) {
			log_error("出牌删除出错");
			return false;
		}

		PlayerStatus playerStatus = _playerStatus[seat_index];

		// 效验操作
		if ((code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(code) == false)) {
			log_error("没有这个操作");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(code, card);
		playerStatus.clean_status();
		return true;
	}

	/**
	 * 廊起操作校验
	 * 
	 * @param card
	 * @param code
	 * @param seat_index
	 * @return
	 */
	public boolean checkOperate(int code, int seat_index) {
		if (_current_player != seat_index) {
			log_error("不是当前玩家玩家操作");
			return false;
		}
		PlayerStatus playerStatus = _playerStatus[seat_index];

		// 效验操作
		if ((code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(code) == false)) {
			log_error("没有这个操作");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(code, -1);
		playerStatus.clean_status();
		return true;
	}

	/**
	 * 
	 * 廊起显示出牌
	 */
	public boolean operate_show_card(int seat_index, int type, int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		if (!_player_result.is_qing_hu(seat_index)) {
			return false;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setIsTing(true);
		int card_count = 0;
		for (int i = 0; i < GameConstants_YINGJING.MAX_COUNT; i++) {
			int card = lang_qi_card[seat_index][i];
			if (card == 0) {
				continue;
			}
			card_count++;
			roomResponse.addCardData(card);
		}
		if (allLangQiCard != null) {
			allLangQiCard.forEach(card -> {
				roomResponse.addCardsList(card);
			});
		}
		roomResponse.setCardCount(card_count);
		GRR.add_room_response(roomResponse);
		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	/**
	 * 
	 * 廊起显示出牌
	 */
	public boolean operate_show_card_null(int seat_index, int type, int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		if (!_player_result.is_qing_hu(seat_index)) {
			return false;
		}

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setIsTing(true);
		int card_count = 0;
		/*
		 * for (int i = 0; i < GameConstants_YINGJING.MAX_COUNT; i++) { int card
		 * = lang_qi_card[seat_index][i]; if (card == 0) { continue; }
		 * card_count++; roomResponse.addCardData(card); } if (allLangQiCard !=
		 * null) { allLangQiCard.forEach(card -> {
		 * roomResponse.addCardsList(card); }); }
		 */
		roomResponse.setCardCount(card_count);
		GRR.add_room_response(roomResponse);

		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	/**
	 * 廊起失败
	 * 
	 * @param seat_index
	 * @param type
	 * @param to_player
	 * @return
	 */
	public boolean operate_show_card_error(int seat_index, int type, int to_player) {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SHOW_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setIsTing(false);
		GRR.add_room_response(roomResponse);
		if (to_player == GameConstants.INVALID_SEAT) {
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	@Override
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}
}
