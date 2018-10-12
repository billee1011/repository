package com.cai.game.mj.chenchuang.xinyang;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_XIN_YANG;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

/**
 * chenchuang 信阳麻将
 */
@ThreeDimension
public class Table_XinYang extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	public HandlerNao_XinYang _handler_nao;

	// 牌局玩家连续坐庄次数
	public int continueBankerCount;
	// 上局是不是三家输
	public boolean isLastGameDianPao3;
	// 上局输赢的分数
	public int[] lastGameScore = new int[getTablePlayerNumber()];
	public int[] naoZhuangScore = new int[getTablePlayerNumber()];

	public int[] zhong_wu_count = new int[getTablePlayerNumber()];// 中5的个数
	public int[] lian_liu_count = new int[getTablePlayerNumber()];// 连六的个数
	public int[] san_feng_count = new int[getTablePlayerNumber()];// 三风的个数
	public int[] jiu_zhang_zhang_count = new int[getTablePlayerNumber()];// 九张涨的个数

	public int zui_count;// 嘴数

	public Table_XinYang(MJType mJType) {
		super(mJType);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_XinYang();
		_handler_dispath_card = new HandlerDispatchCard_XinYang();
		_handler_gang = new HandlerGang_XinYang();
		_handler_out_card_operate = new HandlerOutCardOperate_XinYang();
		_handler_nao = new HandlerNao_XinYang();
	}

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	@Override
	protected void init_shuffle() {
		List<Integer> cards_list = Lists.newArrayList();
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_BU_DAI_FENG)) {
			int[] cards = Constants_MJ_XIN_YANG.CARD_DATA108;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}

		} else {
			int[] cards = Constants_MJ_XIN_YANG.CARD_DATAS;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
		}

		int[] card = new int[cards_list.size()];
		for (int i = 0; i < cards_list.size(); i++) {
			card[i] = cards_list.get(i);
		}
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_NAO_ZHUANG) && continueBankerCount > 0 && isLastGameDianPao3)
			_handler_nao.handler_nao_zhuang(this, player.get_seat_index(), nao);
		return false;
	}
	
	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) 
			return playerNumber;
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_PEOPLE_COUNT_3)) 
			return 3;
		if(has_rule(Constants_MJ_XIN_YANG.GAME_RULE_PEOPLE_COUNT_2)) 
			return 2;
		return 4;
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


		GameSchedule.put(() -> {// 禁锢状态自动出牌
			on_game_start();
		}, 2, TimeUnit.SECONDS);
		return true;
	}

	/**
	 * 洗完牌执行开始
	 * 
	 * @param lian_liu_count
	 */
	@Override
	protected boolean on_game_start() {
		zhong_wu_count = new int[getTablePlayerNumber()];
		lian_liu_count = new int[getTablePlayerNumber()];
		san_feng_count = new int[getTablePlayerNumber()];
		jiu_zhang_zhang_count = new int[getTablePlayerNumber()];
		naoZhuangScore = new int[getTablePlayerNumber()];
		zui_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
			_player_result.qiang[i] = 0;
		}
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = switch_to_cards_data(i, GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
			if ((has_rule(Constants_MJ_XIN_YANG.GAME_RULE_NAO_ZHUANG) && continueBankerCount > 0 && isLastGameDianPao3)) {
				_player_result.pao[i] = hand_cards[i][(int) (Math.random() * 13)];
				switch_to_cards_data(i, GRR._cards_index[i], hand_cards[i]);
			}
		}
		// 发送给玩家手牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		// 发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_MJ_XIN_YANG.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_ZI_MO);// 自摸
		} else if (card_type == Constants_MJ_XIN_YANG.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_QIANG_GANG);// 抢杠胡
		} else if (card_type == Constants_MJ_XIN_YANG.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_JIE_PAO);// 点炮胡
		}
		if (GameConstants.CHR_ZI_MO != card_type) {
			zhong_wu_count[_seat_index] = 0;// 中5的个数
			lian_liu_count[_seat_index] = 0;// 连六的个数
			san_feng_count[_seat_index] = 0;// 三风的个数
			jiu_zhang_zhang_count[_seat_index] = 0;// 九张涨的个数
		}

		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		// 七对
		boolean is_qi_dui = is_qi_dui(temp_cards_index, weave_count);
		// 是否胡牌牌型
		boolean is_hu = false;
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
			is_hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, cur_card_index, null, 0);
		else
			is_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, null, 0);
		if (!(is_qi_dui || is_hu))// 不是胡牌牌型
			return GameConstants.WIK_NULL;

		// 风对倒不能点炮胡
		if (_logic.get_card_color(cur_card) == 3 && card_type == Constants_MJ_XIN_YANG.HU_CARD_TYPE_JIE_PAO
				&& has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG) && !is_qi_dui) {
			int[] temp_cards_index1 = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
			if (!is_feng_dan_diao(temp_cards_index, cur_card_index)) {
				if (temp_cards_index1[cur_card_index] < 3)
					return GameConstants.WIK_NULL;
				temp_cards_index1[cur_card_index] -= 3;
				if (!AnalyseCardUtil.analyse_feng_chi_by_cards_index(temp_cards_index1, -1, null, 0))
					return GameConstants.WIK_NULL;
			}
		}
		boolean sam = has_rule(Constants_MJ_XIN_YANG.GAME_RULE_SI_DA_ZUI) || has_rule(Constants_MJ_XIN_YANG.GAME_RULE_MAN_TNAG_PAO);
		// 清一色
		boolean is_qing_yi_se = is_qing_yi_se(temp_cards_index, weaveItems, weave_count);
		if (is_qing_yi_se)
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_QING_YI_SE);
		// 门清
		boolean men_qing = is_no_pg(weaveItems, weave_count);
		// 得到张数最多的牌,风牌不算
		int maxZhang = getMaxZhang(temp_cards_index, weaveItems, weave_count);

		/** 四大嘴 **/
		if (sam) {
			// ①大门清：胡牌时没有碰、明杠，暗杠也算门清；
			if (men_qing)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_DA_MEN_QING);
			// ②大清缺：胡牌时，手中的牌至少断一门且没有风牌，即为清缺；
			boolean is_da_qing_que = is_qing_que(temp_cards_index, weaveItems, weave_count, true);
			if (is_da_qing_que)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_DA_QING_QUE);
			// ③一条龙：胡牌时，手中的某一门花色能组成1-9的排序，剩下的牌为将牌和一个刻子/顺子/杠
			boolean is_yi_tiao_long = is_yi_tiao_long(temp_cards_index);
			if (is_yi_tiao_long)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_YI_TIAO_LONG);
			// ④十张：胡牌时，万筒条中有一门中有十张或以上。
			boolean is_shi_zhang = maxZhang >= 10;
			if (is_shi_zhang)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_SHI_ZHANG);
			// 四大嘴和满堂跑必须满足四大嘴中的一种
			if (!(men_qing || is_da_qing_que || is_yi_tiao_long || is_shi_zhang))
				return GameConstants.WIK_NULL;
			// 一旦碰了风牌，必须保证满足十张或者一条龙，否则无法胡牌
			boolean is_peng_feng = is_peng_feng(weaveItems, weave_count);
			if (is_peng_feng) {
				if (!is_shi_zhang && !is_yi_tiao_long)
					return GameConstants.WIK_NULL;
			}
		}

		/** 七小嘴 **/
		// ①牌钱：只要胡牌就算，自带1嘴；
		chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_PAI_QIAN);
		// ②小跑：只要胡牌就算，自带1嘴；
		chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_XIAO_PAO);
		// ③门清：胡牌时没有碰、明杠，暗杠也算门清；
		if (men_qing)
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_MEN_QING);
		// ④八张：胡牌时，条、饼、万任意一门牌有八张或超过八张；
		boolean is_ba_zhang = maxZhang >= 8;
		if (is_ba_zhang)
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_BA_ZHANG);
		// ⑥独赢：胡牌时，胡牌为单独的一张牌时，称为独赢，包括夹子、边张、单调将，独赢有且只能胡一张牌，如：7779胡到8胡到9都不算独赢；
		boolean is_du_ying = is_du_ying(cards_index);
		if (is_du_ying)
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_DU_YING);
		// ⑤夹子：胡牌时，胡的牌是相连的三张牌中间的那一张（边张），若出现23345胡到4，则不算夹子，3335胡到4也不算夹子，夹子也必须满足有且只能胡1张牌；
		if (is_du_ying) {
			boolean is_jia_zi = is_jia_zi(cards_index, cur_card);
			if (is_jia_zi)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_JIA_ZI);
		}
		// ⑦断门：胡牌时，万筒条有一门花色没有（包括清缺和混缺）；
		boolean is_duan_men = is_qing_que(temp_cards_index, weaveItems, weave_count, false);
		if (is_duan_men)
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_DUAN_MEN);

		/** 满堂跑 **/
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_MAN_TNAG_PAO)) {
			// ①中五：胡牌时，牌型中有每有一组456的顺子，算一嘴。若胡牌牌型把456拿出来后，其他的牌无法满足胡牌胡牌，则不算，比如牌型为345678，则按345
			// 678计算，无中五（七对牌型无中五）；
			int zhongWuCount = getZhongWuCount(temp_cards_index);
			if (zhongWuCount > 0 && (GameConstants.CHR_ZI_MO != card_type)) {
				// 中五的个数
				zhong_wu_count[_seat_index] = zhongWuCount;
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_ZHONG_WU);
			}
			// ②连六：胡牌时，牌型每有一个123456，或456789算一嘴，其余牌型不算；如有连六必含中五，123456789这种只能算1个连6，不可以复算多个，最多只会有2个连六（七对牌型无连六）；
			int lianLiuCount = getLianLiuCount(temp_cards_index);
			if (lianLiuCount > 0 && (GameConstants.CHR_ZI_MO != card_type)) {
				// 连六的个数
				lian_liu_count[_seat_index] = lianLiuCount;
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_LIAN_LIU);
			}
			// ③三七赢：是以三万三饼三条胡牌即算一嘴牌，同理，七一样；
			if ((_logic.get_card_value(cur_card) == 3 || _logic.get_card_value(cur_card) == 7) && _logic.get_card_color(cur_card) != 3)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_SAN_QI_YING);
			// ④三七将：胡牌时玩家手里有对三万、条、饼胡牌则算一嘴，同理，七一样；
			boolean is_san_qi_jiang = is_san_qi_jiang(temp_cards_index);
			if (is_qi_dui) {
				for (int i = 0; i < GameConstants.MAX_ZI; i++) {
					if (temp_cards_index[i] > 0) {
						int value = _logic.get_card_value(_logic.switch_to_card_data(i));
						if (value == 7 || value == 3) {
							chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_SAN_QI_JIANG);
							break;
						}
					}
				}
			}
			if (is_san_qi_jiang)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_SAN_QI_JIANG);
			// ⑤九张涨：当一门花色的牌超过9张时，每多一张同花色的牌，增加1嘴。9张默认1嘴。
			int jiu_zhang_zhang = maxZhang - 8;
			if (jiu_zhang_zhang > 0 && (GameConstants.CHR_ZI_MO != card_type)) {
				// 九张涨的数量
				jiu_zhang_zhang_count[_seat_index] = jiu_zhang_zhang;
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_JIU_ZHANG_ZHANG);
			}
		}

		// 三风个数
		int sanFengCount = getSanFengCount(temp_cards_index);
		if (sanFengCount > 0 && (GameConstants.CHR_ZI_MO != card_type)) {
			// 三风的数量
			san_feng_count[_seat_index] = sanFengCount;
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_SAN_FENG);
		}
		// 风单吊
		boolean is_feng_dan_diao = has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG) && is_feng_dan_diao(temp_cards_index, cur_card_index);
		if (is_feng_dan_diao && (GameConstants.CHR_ZI_MO != card_type)) {
			// 三风的数量
			san_feng_count[_seat_index]++;
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_SAN_FENG);
		}

		if (is_qi_dui && is_hu) {
			if (zhong_wu_count[_seat_index] > 0 || lian_liu_count[_seat_index] > 0 || san_feng_count[_seat_index] > 0)
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_PING_HU);
			else {
				if (_logic.get_card_color(cur_card) == 3 && (GameConstants.CHR_ZI_MO != card_type))
					san_feng_count[_seat_index]++;
				chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_QI_DUI);
			}
		} else if (is_qi_dui) {
			if (_logic.get_card_color(cur_card) == 3 && (GameConstants.CHR_ZI_MO != card_type))
				san_feng_count[_seat_index]++;
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_QI_DUI);
		} else if (is_hu)
			chiHuRight.opr_or(Constants_MJ_XIN_YANG.CHR_PING_HU);
		return GameConstants.WIK_CHI_HU;
	}

	/**
	 * 判断抢杠胡
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
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 不用判断是否过圈
			ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
			chr.set_empty();
			// 吃胡判断
			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					Constants_MJ_XIN_YANG.HU_CARD_TYPE_QIANG_GANG, i);

			// 结果判断
			if (action != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
				bAroseAction = true;
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

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		int wFanShu = 1;// 番数
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_QI_DUI).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_QING_YI_SE).is_empty())
			wFanShu *= 2;

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
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		int lChiHuScore = wFanShu * getZuiCount(chr, seat_index);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {// 自摸
			int score = lChiHuScore * 2;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_20_ZUI))
				score = score > 20 ? 20 : score;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_40_ZUI))
				score = score > 40 ? 40 : score;
			zui_count = score;
			_player_result.da_hu_zi_mo[seat_index] += zui_count;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;
				GRR._game_score[i] -= score;
				GRR._game_score[seat_index] += score;
			}
		} else {// 点炮
			int score = lChiHuScore;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_20_ZUI))
				score = lChiHuScore > 20 ? 20 : lChiHuScore;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_40_ZUI))
				score = lChiHuScore > 40 ? 40 : lChiHuScore;
			zui_count = score;
			_player_result.da_hu_zi_mo[seat_index] += zui_count;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_SHU_SAN_JIA)) {// 普通平胡，点炮三家付
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;
					GRR._game_score[i] -= score;
					GRR._game_score[seat_index] += score;
				}
			} else {
				GRR._game_score[provide_index] -= score;
				GRR._game_score[seat_index] += score;
			}
		}

		setNaoZhuangScore(seat_index, zimo);

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private void setNaoZhuangScore(int seat_index, boolean zimo) {
		int[] copylastGameScore = Arrays.copyOf(lastGameScore, lastGameScore.length);
		for (int i = 0; i < getTablePlayerNumber(); i++)
			lastGameScore[i] = (int) GRR._game_score[i];
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		boolean iszimo = zimo || !chr.opr_and(Constants_MJ_XIN_YANG.CHR_QIANG_GANG).is_empty();
		boolean is_banker_hu = seat_index == GRR._banker_player;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == GRR._banker_player)
				continue;
			int score = iszimo ? copylastGameScore[i] : copylastGameScore[i] / 2;
			if (_player_result.nao[GRR._banker_player] == 0 && _player_result.nao[i] == 1) {
				if (!is_banker_hu && seat_index == i) {
					naoZhuangScore[i] -= score;
					naoZhuangScore[GRR._banker_player] += score;
					GRR._game_score[i] -= score;
					GRR._game_score[GRR._banker_player] += score;
				}
			} else if (_player_result.nao[GRR._banker_player] == 1 && _player_result.nao[i] == 0) {
				if (is_banker_hu) {
					naoZhuangScore[i] += score;
					naoZhuangScore[GRR._banker_player] -= score;
					GRR._game_score[i] += score;
					GRR._game_score[GRR._banker_player] -= score;
				}
			} else if (_player_result.nao[GRR._banker_player] == 1 && _player_result.nao[i] == 1) {
				if (is_banker_hu) {
					naoZhuangScore[i] += score;
					naoZhuangScore[GRR._banker_player] -= score;
					GRR._game_score[i] += score;
					GRR._game_score[GRR._banker_player] -= score;
				} else if (seat_index == i) {
					naoZhuangScore[i] -= score;
					naoZhuangScore[GRR._banker_player] += score;
					GRR._game_score[i] -= score;
					GRR._game_score[GRR._banker_player] += score;
				}
			}

		}
	}

	private int getZuiCount(ChiHuRight chr, int seat_index) {
		int zuiCount = 0;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_PAI_QIAN).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_XIAO_PAO).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_MEN_QING).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DUAN_MEN).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_BA_ZHANG).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DU_YING).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_JIA_ZI).is_empty())
			zuiCount++;

		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DA_MEN_QING).is_empty())
			zuiCount += 5;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_YI_TIAO_LONG).is_empty())
			zuiCount += 5;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SHI_ZHANG).is_empty())
			zuiCount += 5;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DA_QING_QUE).is_empty())
			zuiCount += 5;

		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_ZHONG_WU).is_empty())
			zuiCount += zhong_wu_count[seat_index];
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_LIAN_LIU).is_empty())
			zuiCount += lian_liu_count[seat_index];
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SAN_QI_YING).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SAN_QI_JIANG).is_empty())
			zuiCount++;
		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_JIU_ZHANG_ZHANG).is_empty())
			zuiCount += jiu_zhang_zhang_count[seat_index];

		if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SAN_FENG).is_empty())
			zuiCount += san_feng_count[seat_index];

		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_20_ZUI))
			zuiCount = zuiCount > 20 ? 20 : zuiCount;
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_40_ZUI))
			zuiCount = zuiCount > 40 ? 40 : zuiCount;
		return zuiCount;
	}

	@Override
	protected void set_result_describe() {
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");
			ChiHuRight chr = GRR._chi_hu_rights[player];
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_QIANG_GANG).is_empty()) {
					result.append(" 抢杠胡");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_JIE_PAO).is_empty()) {
					result.append(" 点炮胡");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DA_MEN_QING).is_empty()) {
					result.append(" 大门清");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_YI_TIAO_LONG).is_empty()) {
					result.append(" 一条龙");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SHI_ZHANG).is_empty()) {
					result.append(" 十张");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DA_QING_QUE).is_empty()) {
					result.append(" 大清缺");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_PAI_QIAN).is_empty()) {
					result.append(" 牌钱");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_XIAO_PAO).is_empty()) {
					result.append(" 小跑");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_MEN_QING).is_empty()) {
					result.append(" 门清");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DUAN_MEN).is_empty()) {
					result.append(" 断门");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_BA_ZHANG).is_empty()) {
					result.append(" 八张");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_DU_YING).is_empty()) {
					result.append(" 独赢");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_JIA_ZI).is_empty()) {
					result.append(" 夹子");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SAN_QI_YING).is_empty()) {
					result.append(" 三七赢");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_SAN_QI_JIANG).is_empty()) {
					result.append(" 三七将");
				}

				if (zhong_wu_count[player] > 0)
					result.append(" 中五x" + zhong_wu_count[player]);
				if (lian_liu_count[player] > 0)
					result.append(" 连六x" + lian_liu_count[player]);
				if (jiu_zhang_zhang_count[player] > 0)
					result.append(" 九张x" + jiu_zhang_zhang_count[player]);
				if (san_feng_count[player] > 0)
					result.append(" 三风x" + san_feng_count[player]);

				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_ZI_MO).is_empty()) {
					result.append(" 自摸");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_PING_HU).is_empty()) {
					result.append(" 平胡");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_QI_DUI).is_empty()) {
					result.append(" 七对");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_QING_YI_SE).is_empty()) {
					result.append(" 清一色");
				}

			} else {
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_FANG_PAO).is_empty()) {
					result.append(" 点炮");
				}
				if (!chr.opr_and(Constants_MJ_XIN_YANG.CHR_BEI_QIANG_GANG).is_empty()) {
					result.append(" 被抢杠");
				}
			}

			GRR._result_des[player] = result.toString();
		}
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			@SuppressWarnings("unused")
			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			// 可以随便碰
			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && GRR._left_card_count > 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
			if (action != 0 && GRR._left_card_count > 0) {
				playerStatus.add_action(GameConstants.WIK_GANG);
				playerStatus.add_gang(card, seat_index, 1); // 加上杠
				bAroseAction = true;
			}

			// 没有漏胡
			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();
			int cbWeaveCount = GRR._weave_count[i];
			int card_type = Constants_MJ_XIN_YANG.HU_CARD_TYPE_JIE_PAO;
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);
			if (action != 0 && GRR._left_card_count >= 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
				_playerStatus[i].add_chi_hu(card, seat_index);
				bAroseAction = true;
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

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
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

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, GameConstants.CHR_ZI_MO,
					seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}

		// 全听
		if (count == GameConstants.MAX_ZI_FENG) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	// 杠牌分析 包括补杠 check_weave检查补杠
	public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		if (GRR._left_card_count > 0) {
			// 手上杠牌
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cards_index[i] == 4) {
					cbActionMask |= GameConstants.WIK_GANG;
					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
					gangCardResult.isPublic[index] = 0;// 暗刚
					gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
				}
			}

			if (check_weave) {
				// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
				for (int i = 0; i < cbWeaveCount; i++) {
					if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							if (cards_index[j] != 1) { // ||
														// cards_abandoned_gang[j]
														// != 0
								continue;
							} else {
								if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
									cbActionMask |= GameConstants.WIK_GANG;
									int index = gangCardResult.cbCardCount++;
									gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
									gangCardResult.isPublic[index] = 1;// 明刚
									gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
									break;
								}
							}
						}
					}
				}
			}
		}
		return cbActionMask;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
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
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
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
			// boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason
			// == GameConstants.Game_End_RELEASE_PLAY;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
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

			/*
			 * // 设置中鸟数据 for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i <
			 * GRR._count_niao; i++) {
			 * game_end.addCardsDataNiao(GRR._cards_data_niao[i]); } // 设置中鸟数据
			 * for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i <
			 * GRR._count_niao_fei; i++) {
			 * game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]); }
			 */
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
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][h])) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]);
					}
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = switch_to_cards_data(i, GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
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
				game_end.addPao(_player_result.pao[i]);
				game_end.addCardsDataNiao(naoZhuangScore[i]);
				naoZhuangScore[i] = 0;
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
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(reason);
		game_end.setHuXi(zui_count);

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

	public boolean is_no_pg(WeaveItem weaveItem[], int weaveCount) {
		if (weaveCount == 0)
			return true;

		// 都是暗杠
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind != GameConstants.WIK_GANG || weaveItem[i].public_card != 0)
				return false;
		}
		return true;
	}

	// 碰风牌
	public boolean is_peng_feng(WeaveItem weaveItem[], int weaveCount) {
		if (weaveCount == 0)
			return false;
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (_logic.get_card_color(weaveItem[i].center_card) == 3)
					return true;
			}
		}
		return false;
	}

	public boolean is_qi_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return false;
		int cbReplaceCount = 0;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int cbCardCount = cards_index[i];
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;
		}
		if (cbReplaceCount > 0)
			return false;
		return true;
	}

	/**
	 * 清一色不带风判断
	 */
	public boolean is_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		if (_logic.has_feng_pai(cards_index, weaveItem, weaveCount)) {
			return false;
		}

		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) != 1) {
			return false;
		}
		return true;
	}

	/**
	 * 清缺
	 */
	public boolean is_qing_que(int cards_index[], WeaveItem weaveItem[], int weaveCount, boolean isDa) {
		if (isDa && _logic.has_feng_pai(cards_index, weaveItem, weaveCount)) {
			return false;
		}

		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) == 3) {
			return false;
		}
		return true;
	}

	/**
	 * ③一条龙
	 */
	public boolean is_yi_tiao_long(int cards_index[]) {
		Set<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				set.add(_logic.get_card_color(_logic.switch_to_card_data(i)));
			}
		}
		if (set.size() != 3) {
			int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
			if (!set.contains(0)) {
				for (int i = 0; i < 9; i++)
					copyOf[i]--;
			} else if (!set.contains(1)) {
				for (int i = 9; i < 18; i++)
					copyOf[i]--;
			} else if (!set.contains(2)) {
				for (int i = 18; i < 27; i++)
					copyOf[i]--;
			}
			boolean hu;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
				hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
			else
				hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
			if (hu)
				return hu;
		}
		return false;
	}

	/**
	 * 得到张数最多的牌,风牌不算
	 */
	public int getMaxZhang(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		int[] colorCount = new int[3];
		for (int f = 0; f < weaveCount; f++) {
			int color = _logic.get_card_color(weaveItem[f].center_card);
			if (color == 3)
				continue;
			if (weaveItem[f].weave_kind == GameConstants.WIK_PENG) {
				colorCount[color] += 3;
			}
			if (weaveItem[f].weave_kind == GameConstants.WIK_GANG) {
				colorCount[color] += 4;
			}
		}
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] != 0) {
				int color = _logic.get_card_color(_logic.switch_to_card_data(i));
				colorCount[color] += cards_index[i];
			}
		}
		int max = 0;
		for (int j = 0; j < colorCount.length; j++) {
			if (colorCount[j] > max)
				max = colorCount[j];
		}
		return max;
	}

	/**
	 * ⑤夹子
	 */
	public boolean is_jia_zi(int cards_index[], int card) {
		int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
		if (_logic.get_card_color(card) == 3)
			return false;
		int value = _logic.get_card_value(card);
		if (value == 1 || value == 9)
			return false;
		if (value == 3) {
			if (copyOf[_logic.switch_to_card_index(card - 1)] != 0 && copyOf[_logic.switch_to_card_index(card - 2)] != 0) {
				copyOf[_logic.switch_to_card_index(card - 1)] -= 1;
				copyOf[_logic.switch_to_card_index(card - 2)] -= 1;
				boolean hu;
				if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
					hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				else
					hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
				if (hu)
					return hu;
				copyOf[_logic.switch_to_card_index(card - 1)] += 1;
				copyOf[_logic.switch_to_card_index(card - 2)] += 1;
			}
		} else if (value == 7) {
			if (copyOf[_logic.switch_to_card_index(card + 1)] != 0 && copyOf[_logic.switch_to_card_index(card + 2)] != 0) {
				copyOf[_logic.switch_to_card_index(card + 1)] -= 1;
				copyOf[_logic.switch_to_card_index(card + 2)] -= 1;
				boolean hu;
				if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
					hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
				else
					hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);
				if (hu)
					return hu;
				copyOf[_logic.switch_to_card_index(card + 1)] += 1;
				copyOf[_logic.switch_to_card_index(card + 2)] += 1;
			}
		}

		if (copyOf[_logic.switch_to_card_index(card - 1)] == 0 || copyOf[_logic.switch_to_card_index(card + 1)] == 0)
			return false;
		copyOf[_logic.switch_to_card_index(card - 1)] -= 1;
		copyOf[_logic.switch_to_card_index(card + 1)] -= 1;

		boolean hu;
		if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
			hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, -1, null, 0);
		else
			hu = AnalyseCardUtil.analyse_win_by_cards_index(copyOf, -1, null, 0);

		return hu;
	}

	/**
	 * 独赢
	 */
	public boolean is_du_ying(int cards_index[]) {
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			boolean hu;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
				hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, i, null, 0);
			else
				hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, i, null, 0);
			if (hu)
				count++;
		}
		return count == 1;
	}

	/**
	 * 得到中5的个数
	 */
	public int getZhongWuCount(int cards_index[]) {
		int[] copyOf = Arrays.copyOf(cards_index, cards_index.length);
		int count = 0;
		while (isZhongWu(copyOf, 3, 4, 5))
			count++;
		while (isZhongWu(copyOf, 12, 13, 14))
			count++;
		while (isZhongWu(copyOf, 21, 22, 23))
			count++;
		return count;
	}

	private boolean isZhongWu(int cards_index[], int i1, int i2, int i3) {
		boolean hu = false;
		if (cards_index[i1] != 0 && cards_index[i2] != 0 && cards_index[i3] != 0) {
			cards_index[i1] -= 1;
			cards_index[i2] -= 1;
			cards_index[i3] -= 1;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
				hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, -1, null, 0);
			else
				hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, null, 0);

			if (!hu) {
				cards_index[i1] += 1;
				cards_index[i2] += 1;
				cards_index[i3] += 1;
			}
		}
		return hu;
	}

	/** 风单吊 **/
	public boolean is_feng_dan_diao(int[] temp_cards_index, int cur_card_index) {
		if (_logic.get_card_color(_logic.switch_to_card_data(cur_card_index)) != 3)
			return false;
		if (temp_cards_index[cur_card_index] < 2)
			return false;
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		copyOf[cur_card_index] -= 2;
		boolean hu = true;
		for (int i = 0; i < 9; i++) {
			if (copyOf[i] >= 3)
				continue;
			copyOf[i]++;
			hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, i, null, 0);
			copyOf[i]--;
			if (!hu)
				return false;
		}
		return hu;
	}

	/** 三七将 **/
	public boolean is_san_qi_jiang(int[] temp_cards_index) {
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			int card_data = _logic.switch_to_card_data(i);
			if (!((_logic.get_card_value(card_data) == 3 || _logic.get_card_value(card_data) == 7) && copyOf[i] >= 2))
				continue;
			copyOf[i] -= 2;
			for (int j = 27; j < 34; j++) {
				if (copyOf[j] >= 3)
					continue;
				copyOf[j]++;
				boolean hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(copyOf, j, null, 0);
				copyOf[j]--;
				if (hu)
					return true;
			}
			copyOf[i] += 2;
		}
		return false;
	}

	public static int[][] fenchengju = { { 27, 28, 29 }, { 27, 28, 30 }, { 27, 29, 30 }, { 28, 29, 30 }, { 31, 32, 33 } };

	/** 三风的个数 **/
	public int getSanFengCount(int[] temp_cards_index) {
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		int count = 0;
		for (int i = 0; i < fenchengju.length; i++) {
			while (isZhongWu(copyOf, fenchengju[i][0], fenchengju[i][1], fenchengju[i][2]))
				count++;
		}
		return count;
	}

	public static int[][] lianliu = { { 0, 1, 2, 3, 4, 5 }, { 3, 4, 5, 6, 7, 8 }, { 9, 10, 11, 12, 13, 14 }, { 12, 13, 14, 15, 16, 17 },
			{ 18, 19, 20, 21, 22, 23 }, { 21, 22, 23, 24, 25, 26 } };

	/** 连6的个数 **/
	public int getLianLiuCount(int[] temp_cards_index) {
		int[] copyOf = Arrays.copyOf(temp_cards_index, temp_cards_index.length);
		int count = 0;
		for (int i = 0; i < lianliu.length; i++) {
			while (isLianLiu(copyOf, lianliu[i][0], lianliu[i][1], lianliu[i][2], lianliu[i][3], lianliu[i][4], lianliu[i][5]))
				count++;
		}
		return count;
	}

	private boolean isLianLiu(int cards_index[], int i1, int i2, int i3, int i4, int i5, int i6) {
		boolean hu = false;
		if (cards_index[i1] != 0 && cards_index[i2] != 0 && cards_index[i3] != 0 && cards_index[i4] != 0 && cards_index[i5] != 0
				&& cards_index[i6] != 0) {
			cards_index[i1] -= 1;
			cards_index[i2] -= 1;
			cards_index[i3] -= 1;
			cards_index[i4] -= 1;
			cards_index[i5] -= 1;
			cards_index[i6] -= 1;
			if (has_rule(Constants_MJ_XIN_YANG.GAME_RULE_LUAN_SAN_FENG))
				hu = AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, -1, null, 0);
			else
				hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, -1, null, 0);

			if (!hu) {
				cards_index[i1] += 1;
				cards_index[i2] += 1;
				cards_index[i3] += 1;
				cards_index[i4] += 1;
				cards_index[i5] += 1;
				cards_index[i6] += 1;
			}
		}
		return hu;
	}

	public boolean operate_appoint_player_cards(int seat_index, int card_count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);
		// 手牌数量
		roomResponse.setCardCount(card_count);
		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		roomResponse.setBiaoyanMin(1);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int seat_index, int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;
		boolean ispao = false;
		boolean isqiang = false;
		boolean isProhibit = _player_result.pao[seat_index] != 0 || _player_result.qiang[seat_index] != 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					int data = _logic.switch_to_card_data(i);
					if (data == _player_result.pao[seat_index] && !ispao) {
						ispao = true;
						continue;
					}
					if (data == _player_result.qiang[seat_index] && !isqiang && _player_result.pao[seat_index] == 0) {
						isqiang = true;
						continue;
					}
					if (isProhibit)
						cards_data[cbPosition++] = data + GameConstants.CARD_ESPECIAL_TYPE_GUI;
					else
						cards_data[cbPosition++] = data;
				}
			}
		}
		if (ispao)
			cards_data[cbPosition++] = _player_result.pao[seat_index];
		if (isqiang)
			cards_data[cbPosition++] = _player_result.qiang[seat_index];
		return cbPosition;
	}

	public boolean operate_player_cards_ting(int seat_index, int card_count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);

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

		roomResponse.setBiaoyanMin(1);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x12, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x14, 0x15, 0x16, 0x17, 0x17 };
		int[] cards_of_player1 = new int[] { 0x11, 0x12, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x14, 0x15, 0x16, 0x17, 0x17 };
		int[] cards_of_player3 = new int[] { 0x11, 0x12, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x14, 0x15, 0x16, 0x17, 0x17 };
		int[] cards_of_player2 = new int[] { 0x14, 0x15, 0x16, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x23, 0x23, 0x31, 0x32 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			} else if (getTablePlayerNumber() == 3) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			} else if (getTablePlayerNumber() == 2) {
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

}
