package com.cai.game.mj.hunan.zhuzhouwang;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.GameConstants_ZhuZhouWang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;
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
import protobuf.clazz.mj.GdtdhPro.GDTDHGameEndResponse;
import protobuf.clazz.mj.HuiZhou.MJ_Hui_Zhou;
import protobuf.clazz.mj.HuiZhou.MJ_Hui_Zhou_Xiao_Ju;

/**
 * 
 *
 * 
 * @author luoqinwei date: 2018年9月20日 下午4:27:11 <br/>
 */
public class Table_ZhuZhouWang extends AbstractMJTable {

	public HandlerSelectMagic_ZhuZhouWang _handler_select_magic;
	public HandlerPiao_ZhuZhouWang _handler_piao;
	public HandlerQiShouHu_ZhuZhouWang _handler_qi_shou_hu;
	public HandlerBaoTing_ZhuZhouWang _handler_bao_ting;

	public MJHandlerHaiDi_ZhuZhouWang _handler_hai_di;
	public MJHandlerYaoHaiDi_ZhuZhouWang _handler_yao_hai_di;

	// public HandlerPiao_ZhuZhouWang _handler_piao;

	// 玩家起手是否听牌，用来处理‘报听’
	public boolean[] qi_shou_ting;

	// 玩家是的点击了‘报听’
	public boolean[] is_bao_ting;

	// 起手报听听牌数
	public int[] _bao_ting_count_qishou;
	// 起手报听听的牌
	public int[][] _bao_ting_card_qishou;

	/**
	 * 
	 */
	private static final long serialVersionUID = -4834189625040429103L;
	public int[][] an_gang_card;
	public int[] an_gang_count;
	public int[][] ming_gang_card;
	public int[] ming_gang_count;
	public int[][] jie_gang_card;
	public int[] jie_gang_count;
	public int[][] dian_gang_card;
	public int[] dian_gang_count;
	public int[] hu_dec_type;
	public int _count_zhong_niao;

	public int[] select_piao_score;
	public int[] select_zuo_piao;

	/**
	 * 每一局结算时候的底分
	 */
	private int di_fen;

	/**
	 * 算分规则描述
	 */
	private String gameRuleDes;

	/**
	 * 牌局战绩记录
	 */
	private MJ_Hui_Zhou_Xiao_Ju[] pai_ju_ji_lu;

	/**
	 * 被抢杠玩家(没有为-1)
	 */

	public int joker_card_1 = 0;
	public int joker_card_index_1 = -1;
	public int joker_card_2 = 0;
	public int joker_card_index_2 = -1;
	public int ding_wang_card = 0;
	public int ding_wang_card_index = -1;

	@Override
	public boolean reset_init_data() {
		super.reset_init_data();
		an_gang_card = new int[getTablePlayerNumber()][4];
		ming_gang_card = new int[getTablePlayerNumber()][4];
		jie_gang_card = new int[getTablePlayerNumber()][4];
		dian_gang_card = new int[getTablePlayerNumber()][4];
		an_gang_count = new int[getTablePlayerNumber()];
		ming_gang_count = new int[getTablePlayerNumber()];
		jie_gang_count = new int[getTablePlayerNumber()];
		dian_gang_count = new int[getTablePlayerNumber()];

		hu_dec_type = new int[getTablePlayerNumber()];

		is_bao_ting = new boolean[getTablePlayerNumber()];
		qi_shou_ting = new boolean[getTablePlayerNumber()];
		_count_zhong_niao = 0;

		select_piao_score = new int[getTablePlayerNumber()];
		select_zuo_piao = new int[getTablePlayerNumber()];

		_bao_ting_count_qishou = new int[getTablePlayerNumber()];
		_bao_ting_card_qishou = new int[getTablePlayerNumber()][GameConstants.MAX_ZI];
		return true;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_ZhuZhouWang();
		_handler_dispath_card = new HandlerDispatchCard_ZhuZhouWang();
		_handler_gang = new HandlerGang_ZhuZhouWang();
		_handler_out_card_operate = new HandlerOutCardOperate_ZhuZhouWang();

		_handler_select_magic = new HandlerSelectMagic_ZhuZhouWang();
		_handler_piao = new HandlerPiao_ZhuZhouWang();
		_handler_qi_shou_hu = new HandlerQiShouHu_ZhuZhouWang();
		_handler_bao_ting = new HandlerBaoTing_ZhuZhouWang();

		_handler_hai_di = new MJHandlerHaiDi_ZhuZhouWang();
		_handler_yao_hai_di = new MJHandlerYaoHaiDi_ZhuZhouWang();
		pai_ju_ji_lu = new MJ_Hui_Zhou_Xiao_Ju[_game_round];
	}

	/**
	 * 牌局人数
	 */
	@Override
	public int getTablePlayerNumber() {
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_FOUR_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 1;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) != 0) {
			return GameConstants.GAME_PLAYER - 2;
		}
		return GameConstants.GAME_PLAYER;
	}

	public boolean exe_select_magic() {
		this.set_handler(this._handler_select_magic);
		_handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);
		return true;
	}

	public void exe_qi_shou_hu(int type, int seat_index, int card_data) {
		set_handler(_handler_qi_shou_hu);
		_handler_qi_shou_hu.reset_status(type, seat_index, card_data);
		_handler_qi_shou_hu.exe(this);
	}

	protected void exe_bao_ting(int type, int seat_index, int card_data) {
		set_handler(_handler_bao_ting);
		_handler_bao_ting.reset_status(type, seat_index, card_data);
		_handler_bao_ting.exe(this);
	}

	public boolean exe_hai_di(int seat_index, int type) {
		set_handler(this._handler_hai_di);
		_handler_hai_di.reset_status(seat_index, type);
		_handler_hai_di.exe(this);
		return true;
	}

	public boolean exe_yao_hai_di(int seat_index, int type) {
		set_handler(this._handler_yao_hai_di);
		_handler_yao_hai_di.reset_status(seat_index, type);
		_handler_yao_hai_di.exe(this);
		return true;
	}

	protected boolean on_game_start_real() {
		// 打骰子
		show_tou_zi(GRR._banker_player);

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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

		exe_select_magic();

		return true;
	}

	@Override
	public boolean on_game_start() {
		// 刷新玩家信息
		refresh_less_player();

		// 开局处理
		on_game_start_for_ZhuZhouWang();

		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_PIAO)) {
			set_handler(_handler_piao);
			_handler_piao.exe(this);
		} else {
			on_game_start_real();
		}
		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameConstants_ZhuZhouWang.MAX_MAGIC_CARD];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > GameConstants_ZhuZhouWang.MAX_MAGIC_CARD) { // 一般只有两种癞子牌存在
			magic_card_count = GameConstants_ZhuZhouWang.MAX_MAGIC_CARD;
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

		// 癞子大于8张过滤
		int magic_count = 0;
		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			magic_count += tmp_hand_cards_index[_logic.get_magic_card_index(m)];
		}
		if (magic_count > 8) {
			return GameConstants.WIK_NULL;
		}

		int card_color_count = _logic.get_se_count(tmp_hand_cards_index, weaveItems, weave_count);

		// 是不是七小对
		boolean is_qi_xiao_dui = false;
		int qi_xiao_dui_type = is_qi_xiao_dui_zimo(cards_index, weaveItems, weave_count, cur_card);
		is_qi_xiao_dui = qi_xiao_dui_type != GameConstants.WIK_NULL;

		// 是不是黑天胡
		boolean is_hu_hei_tian_hu = false;
		is_hu_hei_tian_hu = is_hei_tian_hu(tmp_hand_cards_index);

		// 是不是四王
		boolean is_hu_si_wang = false;
		is_hu_si_wang = is_si_wang(tmp_hand_cards_index);

		// 将将胡
		boolean is_hu_jiang_jiang_hu = false;
		is_hu_jiang_jiang_hu = is_jiang_yi_se(tmp_hand_cards_index, weaveItems, weave_count, cur_card, true);

		boolean is_dui_dui_hu = false;
		boolean exist_chi = exist_chi(weaveItems, weave_count);
		if (!exist_chi) {
			is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		// 无王
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		boolean is_wu_wang = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index,
				0);

		boolean is_pi_hu = true;

		boolean can_win_ping = false;
		boolean can_hu_da = false;
		boolean can_hu = false;
		can_win_ping = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		boolean is_qing_yi_se = false;
		boolean is_liu_wang = false;
		is_qing_yi_se = can_win_ping && (card_color_count == 1);
		is_liu_wang = can_win_ping && is_liu_wang(tmp_hand_cards_index);

		if (is_qi_xiao_dui || is_hu_hei_tian_hu || is_hu_si_wang || is_hu_jiang_jiang_hu || is_dui_dui_hu || is_wu_wang
				|| is_qing_yi_se || is_liu_wang) {
			// 七小对
			if (is_qi_xiao_dui) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_QI_XIAO_DUI);
				if (qi_xiao_dui_type == GameConstants_ZhuZhouWang.QI_XIAO_DUI_YING)
					chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_WU_WANG);
			}

			// 碰碰胡
			if (is_dui_dui_hu) {
				if (qi_xiao_dui_type != GameConstants_ZhuZhouWang.QI_XIAO_DUI_YING) {
					chiHuRight.set_empty();
					chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_PENG_PENG_HU);
				}
			}

			// 黑天胡
			if (is_hu_hei_tian_hu) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HEI_TIAN_HU);
			}

			// 四王
			if (is_hu_si_wang) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_SI_WANG);
			}

			// 将将胡
			if (is_hu_jiang_jiang_hu) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_JIANG_JIANG_HU);
			}

			// 无王
			if (is_wu_wang) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_WU_WANG);
			}

			// 清一色
			if (is_qing_yi_se) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_QING_YI_SE);
			}

			// 六王
			if (is_liu_wang) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_LIU_WANG);
			}

			can_hu_da = true;
			is_pi_hu = false;
		}

		boolean can_win_258 = false;
		if (!can_hu_da && has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ER_WU_BA_JIANG)) {
			can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			if (can_win_258) {
				can_hu = true;
			}
		}

		if (!can_hu_da && !has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ER_WU_BA_JIANG)) {
			if (can_win_ping) {
				can_hu = true;
			}
		}

		if (can_hu_da) {
			can_hu = true;
		}

		if (can_hu) {
			// 海底捞月
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_HAI_DI_LAO_HU) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HAI_DI_LAO_YUE);
				is_pi_hu = false;
			}
			// 海底炮
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_HAI_DI_PAO_HU) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HAI_DI_PAO);// 海底炮
				is_pi_hu = false;
			}
			// 天胡
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_TIAN_HU);
				can_hu_da = true;
				is_pi_hu = false;
			}
			// 报听
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_BAO_TING || is_bao_ting[_seat_index]) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_BAO_TING);
				can_hu_da = true;
				is_pi_hu = false;
			}
			// 杠上跑
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_SHANG_PAO) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_GANG_SHANG_PAO);
				can_hu_da = true;
				is_pi_hu = false;
			}
			// 杠开
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_GANG_KAI);
				can_hu_da = true;
				is_pi_hu = false;
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_ZI_MO);
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_JIE_PAO) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_JIE_PAO);
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_QIANG_GANG) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_JIE_PAO);
			}
		}

		if (!can_hu) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		} else {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		if (is_pi_hu) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_PING_HU);
		}

		if (magic_count == 0) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_WU_WANG);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_pao(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameConstants_ZhuZhouWang.MAX_MAGIC_CARD];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > GameConstants_ZhuZhouWang.MAX_MAGIC_CARD) { // 一般只有两种癞子牌存在
			magic_card_count = GameConstants_ZhuZhouWang.MAX_MAGIC_CARD;
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

		// 癞子大于8张过滤
		int magic_count = 0;
		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			magic_count += tmp_hand_cards_index[_logic.get_magic_card_index(m)];
		}
		if (magic_count > 8) {
			return GameConstants.WIK_NULL;
		}

		int card_color_count = _logic.get_se_count(tmp_hand_cards_index, weaveItems, weave_count);

		// 是不是七小对
		boolean is_qi_xiao_dui = false;
		int qi_xiao_dui_type = is_qi_xiao_dui_pao(cards_index, weaveItems, weave_count, cur_card);
		is_qi_xiao_dui = qi_xiao_dui_type != GameConstants.WIK_NULL;

		// 是不是黑天胡(可不满足基本胡牌牌型)
		boolean is_hu_hei_tian_hu = false;
		is_hu_hei_tian_hu = is_hei_tian_hu(cards_index);

		// 是不是四王(可不满足基本胡牌牌型)
		boolean is_hu_si_wang = false;
		is_hu_si_wang = is_si_wang(cards_index);

		// 将将胡
		boolean is_hu_jiang_jiang_hu = false;
		is_hu_jiang_jiang_hu = is_jiang_yi_se(tmp_hand_cards_index, weaveItems, weave_count, cur_card, false);

		boolean is_dui_dui_hu = false;
		boolean exist_chi = exist_chi(weaveItems, weave_count);
		if (!exist_chi) {
			if (_logic.is_magic_card(cur_card)) {
				is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_nc(cards_index,
						_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count, true);
			} else {
				is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_nc(cards_index,
						_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count, false);
			}
		}

		// 无王
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		boolean is_wu_wang = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index,
				0);

		boolean is_pi_hu = true;

		boolean can_win_ping = false;
		boolean can_hu_da = false;
		boolean can_hu = false;

		if (_logic.is_magic_card(cur_card)) {
			can_win_ping = AnalyseCardUtil.analyse_win_by_cards_index_shanxill(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		} else {
			can_win_ping = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		boolean is_qing_yi_se = false;
		boolean is_liu_wang = false;
		is_qing_yi_se = can_win_ping && (card_color_count == 1);
		is_liu_wang = can_win_ping && is_liu_wang(tmp_hand_cards_index);

		if (is_qi_xiao_dui || is_hu_hei_tian_hu || is_hu_si_wang || is_hu_jiang_jiang_hu || is_dui_dui_hu || is_wu_wang
				|| is_qing_yi_se || is_liu_wang) {
			// 七小对
			if (is_qi_xiao_dui) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_QI_XIAO_DUI);
			}

			// 碰碰胡
			if (is_dui_dui_hu) {
				if (qi_xiao_dui_type != GameConstants_ZhuZhouWang.QI_XIAO_DUI_YING) {
					chiHuRight.set_empty();
					chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_PENG_PENG_HU);
				}
			}

			// 黑天胡
			if (is_hu_hei_tian_hu) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HEI_TIAN_HU);
			}

			// 四王
			if (is_hu_si_wang) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_SI_WANG);
			}

			// 将将胡
			if (is_hu_jiang_jiang_hu) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_JIANG_JIANG_HU);
			}

			// 无王
			if (is_wu_wang) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_WU_WANG);
			}

			// 清一色
			if (is_qing_yi_se) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_QING_YI_SE);
			}

			// 六王
			if (is_liu_wang) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_LIU_WANG);
			}
			can_hu_da = true;
			is_pi_hu = false;
		}

		boolean can_win_258 = false;
		if (!can_hu_da && has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ER_WU_BA_JIANG)) {
			if (_logic.is_magic_card(cur_card)) {
				can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index_zhuzhou(cards_index,
						_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
			} else {
				can_win_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index,
						_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
			}
			if (can_win_258) {
				can_hu = true;
			}
		}

		if (!can_hu_da && !has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ER_WU_BA_JIANG)) {
			if (can_win_ping) {
				can_hu = true;
			}
		}

		if (can_hu_da) {
			can_hu = true;
		}

		if (can_hu) {
			// 海底捞月
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_HAI_DI_LAO_HU) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HAI_DI_LAO_YUE);
				is_pi_hu = false;
			}
			// 海底炮
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_HAI_DI_PAO_HU) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HAI_DI_PAO);// 海底炮
				is_pi_hu = false;
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_TIAN_HU);
				can_hu_da = true;
				is_pi_hu = false;
			}
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_BAO_TING || is_bao_ting[_seat_index]) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_BAO_TING);
				can_hu_da = true;
				is_pi_hu = false;
			}
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_SHANG_PAO) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_GANG_SHANG_PAO);
				can_hu_da = true;
				is_pi_hu = false;
			}
			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_GANG_KAI);
				can_hu_da = true;
				is_pi_hu = false;
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_ZI_MO) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_ZI_MO);
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_JIE_PAO) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_JIE_PAO);
			}

			if (card_type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_QIANG_GANG) {
				chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_JIE_PAO);
			}
		}

		if (!can_hu) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		} else {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		if (is_pi_hu) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_PING_HU);
		}

		return cbChiHuKind;
	}

	/**
	 * 玩家手上是否有鬼牌
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean is_have_gui(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int card_num = GRR._cards_index[seat_index][i];

			if (card_num > 0 && _logic.is_magic_index(i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 预测玩家出牌以后的结果
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		int next_player = (seat_index + 1) % getTablePlayerNumber();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			playerStatus = _playerStatus[i];
			if (GRR._left_card_count > 0) {
				boolean can_peng_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng_this_card = false;
						break;
					}
				}
				if (!is_bao_ting[i]) {
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0 && can_peng_this_card) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}

				if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_KE_CHI)) {
					if (!is_bao_ting[i] && next_player == i) {
						action = check_chi_zhuzhouwang(GRR._cards_index[i], card);
						if ((action & GameConstants.WIK_LEFT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
						}
						if ((action & GameConstants.WIK_CENTER) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
						}
						if ((action & GameConstants.WIK_RIGHT) != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
						}
						if (action != 0) {
							bAroseAction = true;
						}
					}
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					boolean flag = true;
					if (is_bao_ting[i]) {
						int[] tmp_cards_index = GRR._cards_index[i].clone();
						tmp_cards_index[_logic.switch_to_card_index(card)] = 0;
						WeaveItem tmp_weave_item[] = GRR._weave_items[i].clone();
						int tmp_weave_count = GRR._weave_count[i];

						int[] tmp_hu_cards = new int[GameConstants.MAX_INDEX];
						int tmp_ting_count = get_ting_card(tmp_hu_cards, tmp_cards_index, tmp_weave_item,
								tmp_weave_count, i);

						if (!check_ting_equals(tmp_ting_count, tmp_hu_cards, _bao_ting_count_qishou[i],
								_bao_ting_card_qishou[i])) {
							flag = false;
						}
					}
					if (flag) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_KE_PAO_HU)) {
				if (_playerStatus[i].is_chi_hu_round()) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_ZhuZhouWang.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_SHANG_PAO) {
						card_type = GameConstants_ZhuZhouWang.HU_CARD_TYPE_GANG_SHANG_PAO;
					}

					action = analyse_chi_hu_card_pao(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}

				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		// 底分
		int total_socre = 0;

		di_fen = get_hu_pai_xing_fen(chr, seat_index) * GameConstants_ZhuZhouWang.CELL_SCORE;

		total_socre = di_fen;

		// 飘
		int[] select_piao = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_result.pao[i] < 0)
				select_piao[i] = 0;
			else
				select_piao[i] = _player_result.pao[i];
		}

		// 坐飘
		int score_zuo_piao = 0;
		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZUO_PIAO_ONE)) {
			score_zuo_piao = 1;
		} else if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZUO_PIAO_TWO)) {
			score_zuo_piao = 2;
		} else if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZUO_PIAO_THREE)) {
			score_zuo_piao = 3;
		} else if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZUO_PIAO_FOUR)) {
			score_zuo_piao = 4;
		} else if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZUO_PIAO_FIVE)) {
			score_zuo_piao = 5;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = total_socre;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = total_socre;
		}
		if (zimo) {
			// 针对8分以上的特殊胡，如果A玩家让B玩家十二张落地（碰碰胡），如果B胡牌，则A包B胡牌的全部分数，抢杠优于报九
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._game_score[i] -= total_socre * (_count_zhong_niao + 1);
				GRR._game_score[seat_index] += total_socre * (_count_zhong_niao + 1);

				select_piao_score[i] -= select_piao[i] + select_piao[seat_index];
				select_piao_score[seat_index] += select_piao[i] + select_piao[seat_index];

				select_zuo_piao[i] -= score_zuo_piao * 2;
				select_zuo_piao[seat_index] += score_zuo_piao * 2;

			}

		} else {
			GRR._game_score[provide_index] -= total_socre * (_count_zhong_niao + 1);
			GRR._game_score[seat_index] += total_socre * (_count_zhong_niao + 1);

			select_piao_score[provide_index] -= select_piao[provide_index] + select_piao[seat_index];
			select_piao_score[seat_index] += select_piao[provide_index] + select_piao[seat_index];

			select_zuo_piao[provide_index] -= score_zuo_piao * 2;
			select_zuo_piao[seat_index] += score_zuo_piao * 2;

			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_ZhuZhouWang.CHR_JIE_PAO).is_empty())
				GRR._chi_hu_rights[provide_index].opr_or(GameConstants_ZhuZhouWang.CHR_FANG_PAO);
		}

	}

	/**
	 * 相应牌型的牌型分
	 * 
	 * @param chr
	 *            吃胡类型
	 * @param seat_index
	 *            玩家座位
	 * @return
	 */
	private int get_hu_pai_xing_fen(ChiHuRight chr, int seat_index) {
		int di_fen = 0;

		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ER_WU_BA_JIANG)) {
			// 天胡
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_TIAN_HU).is_empty()) {
				di_fen += 7;
			}
			// 碰碰胡
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_PENG_PENG_HU).is_empty()) {
				di_fen += 7;
			}
			// 将将胡
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_JIANG_JIANG_HU).is_empty()) {
				di_fen += 7;
			}
			// 清一色
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_QING_YI_SE).is_empty()) {
				di_fen += 7;
			}
			// 海底捞月
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_HAI_DI_LAO_YUE).is_empty()) {
				di_fen += 7;
			}
			// 海底炮
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_HAI_DI_PAO).is_empty()) {
				di_fen += 7;
			}
			// 七小对
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_QI_XIAO_DUI).is_empty()) {
				di_fen += 7;
			}
			// 杠上开花
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_GANG_KAI).is_empty()) {
				di_fen += 7;
			}
			// 杠上炮
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_GANG_SHANG_PAO).is_empty()) {
				di_fen += 7;
			}
			// 黑天胡
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_HEI_TIAN_HU).is_empty()) {
				di_fen += 7;
			}
			// 四王
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_SI_WANG).is_empty()) {
				di_fen += 7;
			}
			// 无王
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_WU_WANG).is_empty()) {
				di_fen += 7;
			}
			// 六王
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_LIU_WANG).is_empty()) {
				di_fen += 7;
			}
			// 报听
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_BAO_TING).is_empty()) {
				di_fen += 7;
			}

			// 无王无将
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_WU_WANG_WU_JIANG).is_empty()) {
				di_fen += 7;
			}
			// 一枝花
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_YI_ZHI_HUA).is_empty()) {
				di_fen += 7;
			}
			// 缺一色
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_QUE_YI_SE).is_empty()) {
				di_fen += 7;
			}
			// 平胡
			if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_PING_HU).is_empty()) {
				di_fen += 2;
			}
		} else {
			di_fen += 2;
		}

		return di_fen;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		boolean is_des = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			ChiHuRight chr = GRR._chi_hu_rights[player];
			if (chr.is_valid()) {
				is_des = true;
			}
		}
		if (!is_des) {
			return;
		}
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");
			ChiHuRight chr = GRR._chi_hu_rights[player];
			// 只显示番数最大的
			if (chr.is_valid()) {
				// 天胡
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_TIAN_HU).is_empty()) {
					result.append(" 天胡");
				}
				// 碰碰胡
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_PENG_PENG_HU).is_empty()) {
					result.append(" 碰碰胡");
				}
				// 将将胡
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_JIANG_JIANG_HU).is_empty()) {
					result.append(" 将将胡");
				}
				// 清一色
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_QING_YI_SE).is_empty()) {
					result.append(" 清一色");
				}
				// 海底捞月
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_HAI_DI_LAO_YUE).is_empty()) {
					result.append(" 海底捞月");
				}
				// 海底炮
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_HAI_DI_PAO).is_empty()) {
					result.append(" 海底炮");
				}
				// 七小对
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_QI_XIAO_DUI).is_empty()) {
					result.append(" 七小对");
				}
				// 杠上开花
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_GANG_KAI).is_empty()) {
					result.append(" 杠上开花");
				}
				// 杠上炮
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_GANG_SHANG_PAO).is_empty()) {
					result.append(" 杠上炮");
				}
				// 黑天胡
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_HEI_TIAN_HU).is_empty()) {
					result.append(" 黑天胡");
				}
				// 四王
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_SI_WANG).is_empty()) {
					result.append(" 四王");
				}
				// 无王
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_WU_WANG).is_empty()) {
					result.append(" 无王");
				}
				// 六王
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_LIU_WANG).is_empty()) {
					result.append(" 六王");
				}
				// 报听
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_BAO_TING).is_empty()) {
					result.append(" 报听");
				}

				// 无王无将
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_WU_WANG_WU_JIANG).is_empty()) {
					result.append(" 无王无将");
				}
				// 一枝花
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_YI_ZHI_HUA).is_empty()) {
					result.append(" 一枝花");
				}
				// 缺一色
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_QUE_YI_SE).is_empty()) {
					result.append(" 缺一色");
				}
				// 平胡
				if (!chr.opr_and(GameConstants_ZhuZhouWang.CHR_PING_HU).is_empty()) {
					result.append(" 平胡");
				}

				// if (select_piao_score[player] > 0) {
				// result.append(" 飘分+" + select_piao_score[player]);
				// }
				// if (select_zuo_piao[player] > 0) {
				// result.append(" 坐飘+" + select_zuo_piao[player]);
				// }
			}

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants_ZhuZhouWang.CHR_ZI_MO) {
						result.append(" 自摸");
					}
				} else if (type == GameConstants.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants.WIK_GANG) {
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
				result.append(" 接杠X" + jie_gang);
			}

			GRR._result_des[player] = result.toString();
		}
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		int real_max_ting_count = max_ting_count;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants_ZhuZhouWang.CHR_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int get_ting_card_bao_ting(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount,
			int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;
		int real_max_ting_count = max_ting_count;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants_ZhuZhouWang.HU_CARD_TYPE_BAO_TING, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getNIaoNum() {

		int nNum = GameConstants.ZHANIAO_0;
		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHUA_NIAO_ONE)) {
			nNum = GameConstants.ZHANIAO_1;
		}
		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHUA_NIAO_TWO)) {
			nNum = GameConstants.ZHANIAO_2;
		}
		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHUA_NIAO_THREE)) {
			nNum = GameConstants.ZHANIAO_3;
		}
		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHUA_NIAO_FOUR)) {
			nNum = GameConstants.ZHANIAO_4;
		}
		if (has_rule(GameConstants_ZhuZhouWang.GAME_RULE_ZHUA_NIAO_SIX)) {
			nNum = GameConstants.ZHANIAO_6;
		}

		if (GRR != null) {
			if (nNum > GRR._left_card_count) {
				nNum = GRR._left_card_count;
			}
		} else
			nNum = 0;
		return nNum;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;
		int niao_num = getNIaoNum();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		if (GRR != null) {
			float[] lGangScore = new float[getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}
			}

			if (reason == GameConstants.Game_End_NORMAL) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					GRR._game_score[i] += lGangScore[i];
					GRR._game_score[i] += select_piao_score[i];
					GRR._game_score[i] += select_zuo_piao[i];
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_player_result.game_score[i] += GRR._game_score[i];
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
			GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

			roomResponse.setLeftCardCount(0);

			this.load_common_status(roomResponse);
			this.load_room_info_data(roomResponse);

			RoomInfo.Builder room_info = getRoomInfo();
			game_end.setEspecialTxt(gameRuleDes == null ? "" : gameRuleDes);
			game_end.setRoomInfo(room_info);
			game_end.setRunPlayerId(_run_player_id);
			game_end.setRoundOverType(0);
			game_end.setGamePlayerNumber(this.getTablePlayerNumber());
			game_end.setEndTime(System.currentTimeMillis() / 1000L);
			if (GRR != null) {
				game_end.setRoundOverType(1);
				game_end.setStartTime(GRR._start_time);

				game_end.setGameTypeIndex(GRR._game_type_index);
				roomResponse.setLeftCardCount(GRR._left_card_count);

				for (int i = 0; i < GRR._especial_card_count; i++) {
					game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
				}

				GRR._end_type = reason;

				this.load_player_info_data(roomResponse);

				game_end.setGameRound(_game_round);
				game_end.setCurRound(_cur_round);

				game_end.setCellScore(GameConstants_ZhuZhouWang.CELL_SCORE);

				game_end.setBankerPlayer(GRR._banker_player);
				game_end.setLeftCardCount(GRR._left_card_count);
				game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

				for (int i = 0; i < niao_num && i < GRR._count_niao; i++) {
					game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
				}
				for (int i = 0; i < niao_num && i < GRR._count_niao_fei; i++) {
					game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
				}
				game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

				for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
					Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._player_niao_count[i]; j++) {
						pnc.addItem(GRR._player_niao_cards[i][j]);
					}
					for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
						pnc.addItem(GRR._player_niao_cards_fei[i][j]);
					}
					game_end.addPlayerNiaoCards(pnc);
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end.addHuResult(GRR._hu_result[i]);
					Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}

					for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]);
					}

					game_end.addHuCardArray(hc);
				}

				// 现在权值只有一位
				long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

				this.set_result_describe();

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

					Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cs.addItem(GRR._cards_data[i][j]);
					}
					game_end.addCardsData(cs);

					WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse
							.newBuilder();
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}

					// 赢分记录
					if (GRR._game_score[i] > 0) {
						_player_result.win_num[i]++;
					}

					game_end.addWeaveItemArray(weaveItem_array);

					GRR._chi_hu_rights[i].get_right_data(rv);
					game_end.addChiHuRight(rv[0]);

					GRR._start_hu_right[i].get_right_data(rv);
					game_end.addStartHuRight(rv[0]);

					game_end.addProvidePlayer(GRR._provider[i]);
					game_end.addGameScore(GRR._game_score[i]);
					// game_end.addGangScore(lGangScore[i]);
					game_end.addStartHuScore(GRR._start_hu_score[i]);
					game_end.addResultDes(GRR._result_des[i]);

					game_end.addWinOrder(GRR._win_order[i]);

					Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						lfs.addItem(GRR._lost_fan_shu[i][j]);
					}

					game_end.addLostFanShu(lfs);

				}

			}

			boolean end = false;
			if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
				if (_cur_round >= _game_round && (!is_sys())) {
					end = true;
					game_end.setRoomOverType(1);
					game_end.setPlayerResult(this.process_player_result(reason));
				} else {
				}
			} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
					|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN
					|| reason == GameConstants.Game_End_RELEASE_RESULT
					|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
				end = true;
				real_reason = GameConstants.Game_End_DRAW;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			}
			game_end.setEndType(real_reason);

			handler_game_end(game_end);

			roomResponse.setGameEnd(game_end);

			this.send_response_to_room(roomResponse);

			record_game_round(game_end);

			if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
					|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
		}
		return false;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 *            固定的鸟
	 * @param show
	 * @param add_niao
	 *            额外奖鸟
	 * @param isTongPao
	 *            --通炮不抓飞鸟
	 */
	public void set_niao_card(int seat_index, int card, boolean show) {
		int niao_num = getNIaoNum();
		for (int i = 0; i < niao_num; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < niao_num; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;
		if (card == GameConstants.INVALID_VALUE) {
			GRR._count_niao = getNIaoNum();
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
				// GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		// 中鸟个数
		// GRR._count_pick_niao =
		// _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

			GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = GRR._cards_data_niao[i];

			for (int player = 0; player < getTablePlayerNumber(); player++) {
				if (player == seat_index) {
					if (nValue == 1 || nValue == 5 || nValue == 9) {
						GRR._player_niao_cards[player][GRR._player_niao_count[player]] = this
								.set_ding_niao_valid(GRR._cards_data_niao[i], true);// 胡牌的鸟生效
						_count_zhong_niao++;
					} else {
						GRR._player_niao_cards[player][GRR._player_niao_count[player]] = GRR._cards_data_niao[i];
					}
					GRR._player_niao_count[player]++;
				} else {
					// GRR._player_niao_cards[player][GRR._player_niao_count[player]] =
					// GRR._cards_data_niao[i];
				}

			}

		}
		GRR._count_pick_niao = GRR._player_niao_count[seat_index];

		_player_result.piao_lai_count[seat_index] += _count_zhong_niao;
	}

	// private int getCardValue(int card) {
	// if (card < GameConstants_ZhuZhouWang.CHUN_MAGIC_CARD) {
	// return _logic.get_card_value(card);
	// }
	//
	// // 花牌
	// switch (card) {
	// case GameConstants_ZhuZhouWang.CHUN_MAGIC_CARD:
	// return 1;
	// case GameConstants_ZhuZhouWang.XIA_MAGIC_CARD:
	// return 2;
	// case GameConstants_ZhuZhouWang.QIU_MAGIC_CARD:
	// return 3;
	// case GameConstants_ZhuZhouWang.DONG_MAGIC_CARD:
	// return 4;
	// case GameConstants_ZhuZhouWang.MEI_MAGIC_CARD:
	// return 5;
	// case GameConstants_ZhuZhouWang.LAN_MAGIC_CARD:
	// return 6;
	// case GameConstants_ZhuZhouWang.ZHU_MAGIC_CARD:
	// return 7;
	// case GameConstants_ZhuZhouWang.JU_MAGIC_CARD:
	// return 8;
	// }
	// return 0;
	// }

	/**
	 * 游戏规则描述
	 * 
	 * @return
	 */
	public String getGameRuleDes() {
		StringBuilder stringBuilder = new StringBuilder();

		return stringBuilder.toString();
	}

	/**
	 * 株洲麻将牌局开始需处理
	 */
	public void on_game_start_for_ZhuZhouWang() {
		di_fen = 0;
		gameRuleDes = getGameRuleDes();

		// 坐庄次数
		_player_result.lose_num[_cur_banker]++;

		// 跑分初始化
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = -1;
		}
	}

	/**
	 * 惠州麻将
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return
	 */
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据g
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int magic_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			if (cbCardCount <= 0) {
				continue;
			}

			if (_logic.is_magic_index(i)) {
				magic_card_count++;
				continue;
			}

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (cbReplaceCount > magic_card_count) {
			return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				// 双豪华七小对
				return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			}
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}
	}

	// 十三夭牌
	public boolean isShiSanYao(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		// 组合判断
		if (weaveCount != 0)
			return false;

		int que_pai_count = 0;

		boolean bCardEye = false;
		// 一九判断
		for (int i = 0; i < 27; i += 9) {
			// 无效判断
			if (cards_index[i] == 0)
				que_pai_count++;
			if (cards_index[i + 8] == 0)
				que_pai_count++;
			// 牌眼判断
			if ((bCardEye == false) && (cards_index[i] == 2))
				bCardEye = true;
			if ((bCardEye == false) && (cards_index[i + 8] == 2))
				bCardEye = true;
		}

		// 番子判断
		for (int i = 27; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (cards_index[i] == 0)
				que_pai_count++;
			if ((bCardEye == false) && (cards_index[i] == 2))
				bCardEye = true;
		}

		int magic_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];
			if (cbCardCount <= 0) {
				continue;
			}

			if (_logic.is_magic_index(i)) {
				magic_card_count++;
				continue;
			}
		}
		if (magic_card_count < que_pai_count) {
			return false;
		}

		if (magic_card_count == que_pai_count && !bCardEye) {
			return false;
		}
		return true;
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
				action = analyse_chi_hu_card_pao(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_ZhuZhouWang.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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
	 * 显示胡的牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @return
	 */
	@Override
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
			MJ_Hui_Zhou.Builder b = MJ_Hui_Zhou.newBuilder();
			if (_cur_round != 0) {
				for (int j = 0; j < _cur_round; j++) {
					if (pai_ju_ji_lu[j] != null) {
						b.addMsgs(pai_ju_ji_lu[j]);
					}
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(b));
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void test_cards() {
		// int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x19, 0x19,
		// 0x19, 0x19, 0x21, 0x21, 0x21, 0x21,
		// 0x01 };
		int[] cards_of_player1 =new int[] { 0x01, 0x01, 0x06, 0x06, 0x07, 0x09, 0x11, 0x13, 0x14, 0x16, 0x17, 0x19,
				0x19 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x06, 0x06, 0x07, 0x09, 0x11, 0x13, 0x14, 0x16, 0x17, 0x19,
				0x19 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x06, 0x06, 0x07, 0x09, 0x11, 0x13, 0x14, 0x16, 0x17, 0x19,
				0x19 };
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x06, 0x06, 0x07, 0x09, 0x11, 0x13, 0x14, 0x16, 0x17, 0x19,
				0x19 };

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

	/**
	 * 重写abstractMjTable为了解决解散房间不弹提示
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 60;
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
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), seat_index,
						GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
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
				// send_error_notify(j, 1, "游戏解散成功!");

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
						// send_error_notify(i, 2, "游戏已解散");
					} else {
						// send_error_notify(i, 2, "游戏已被创建者解散");
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
			// WalkerGeek 允许少人模式
			super.init_less_param();

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				// send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	// 庄家选择
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = getOpenRoomIndex();// 创建者的玩家为庄家
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}

		if (_cur_round < 2 && !isOpenPlayerInRoom()) {// 创建者不在房间,随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 结算附加值
	private void handler_game_end(GameEndResponse.Builder game_end) {
		GDTDHGameEndResponse.Builder gameEndBuilder = GDTDHGameEndResponse.newBuilder();

		Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			cards.clear();
			for (int an = 0; an < an_gang_count[p]; an++) {
				cards.addItem(an_gang_card[p][an]);
			}
			gameEndBuilder.addAnGangCard(cards);

			cards.clear();
			for (int ming = 0; ming < ming_gang_count[p]; ming++) {
				cards.addItem(ming_gang_card[p][ming]);
			}
			gameEndBuilder.addMingGangCard(cards);

			cards.clear();
			for (int jie = 0; jie < jie_gang_count[p]; jie++) {
				cards.addItem(jie_gang_card[p][jie]);
			}
			gameEndBuilder.addJieGangCard(cards);

			cards.clear();
			for (int d = 0; d < dian_gang_count[p]; d++) {
				cards.addItem(dian_gang_card[p][d]);
			}
			gameEndBuilder.addDianGangCard(cards);

			gameEndBuilder.addHuDes(String.valueOf(hu_dec_type[p]));
			gameEndBuilder.addHuCount(_player_result.zi_mo_count[p]);
			gameEndBuilder.addWinScoreCount(_player_result.win_num[p]);
			gameEndBuilder.addBankerCount(banker_count[p]);
			gameEndBuilder.addGangCount(_player_result.ming_gang_count[p] + _player_result.an_gang_count[p]);
		}
		game_end.setCommResponse(PBUtil.toByteString(gameEndBuilder));
	}

	// 无王无将
	public boolean is_wu_wang_wu_jiang(int[] cards_index) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		if (ding_wang_card_index >= 0 && ding_wang_card_index < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[ding_wang_card_index] > 0)
				return false;
		}

		if (joker_card_index_1 >= 0 && joker_card_index_1 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_1] > 0)
				return false;
		}

		if (joker_card_index_2 >= 0 && joker_card_index_2 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_2] > 0)
				return false;
		}

		if (tmp_hand_cards_index[1] > 0 || tmp_hand_cards_index[4] > 0 || tmp_hand_cards_index[7] > 0
				|| tmp_hand_cards_index[10] > 0 || tmp_hand_cards_index[13] > 0 || tmp_hand_cards_index[16] > 0
				|| tmp_hand_cards_index[19] > 0 || tmp_hand_cards_index[22] > 0 || tmp_hand_cards_index[25] > 0)
			return false;

		return true;
	}

	// 缺一色
	public boolean is_que_yi_se(int[] cards_index) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = tmp_hand_cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			if (_logic.is_magic_index(i))
				continue;
			int card = _logic.switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}

		int count = 0;
		for (int i = 0; i < 3; i++) {
			if (cbQueYiMenColor[i] == 0) {
				count += 1;
			}
		}

		if (count == 2)
			return true;

		return false;
	}

	// 一枝花
	public boolean is_yi_zhi_hua(int[] cards_index) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		boolean have_term_one = false;
		boolean have_term_two = true;
		boolean have_term_three = false;

		if (ding_wang_card_index >= 0 && ding_wang_card_index < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[ding_wang_card_index] == 1)
				have_term_one = true;
		}

		if (joker_card_index_1 >= 0 && joker_card_index_1 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_1] > 0)
				have_term_two = false;
		}

		if (joker_card_index_2 >= 0 && joker_card_index_2 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_2] > 0)
				have_term_two = false;
		}

		WeaveItem[] weaveItem = new WeaveItem[1];
		int count = _logic.get_se_count(tmp_hand_cards_index, weaveItem, 0);
		if (count == 1)
			have_term_three = true;

		return have_term_one && have_term_two && have_term_three;
	}

	// 黑天胡
	public boolean is_hei_tian_hu(int[] cards_index) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		if (ding_wang_card_index >= 0 && ding_wang_card_index < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[ding_wang_card_index] >= 3)
				return true;
		}

		return false;
	}

	// 四王
	public boolean is_si_wang(int[] cards_index) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		int count = 0;
		if (joker_card_index_1 >= 0 && joker_card_index_1 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_1] > 0)
				count += tmp_hand_cards_index[joker_card_index_1];
		}

		if (joker_card_index_2 >= 0 && joker_card_index_2 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_2] > 0)
				count += tmp_hand_cards_index[joker_card_index_2];
		}

		return (count >= 4);
	}

	// 六王
	public boolean is_liu_wang(int[] cards_index) {
		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		int count = 0;
		if (ding_wang_card_index >= 0 && ding_wang_card_index < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[ding_wang_card_index] > 0)
				count += tmp_hand_cards_index[ding_wang_card_index];
		}

		if (joker_card_index_1 >= 0 && joker_card_index_1 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_1] > 0)
				count += tmp_hand_cards_index[joker_card_index_1];
		}

		if (joker_card_index_2 >= 0 && joker_card_index_2 < GameConstants.MAX_ZI) {
			if (tmp_hand_cards_index[joker_card_index_2] > 0)
				count += tmp_hand_cards_index[joker_card_index_2];
		}

		return (count >= 6);
	}

	// 起手胡分析
	public int analyse_qi_shou_hu_card(int[] cards_index, ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (GameConstants_ZhuZhouWang.HU_CARD_TYPE_QI_SHOU_HU != card_type)
			return GameConstants.WIK_NULL;

		int[] tmp_hand_cards_index = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_hand_cards_index[i] = cards_index[i];
		}

		int hand_card_count = _logic.get_card_count_by_index(tmp_hand_cards_index);

		if (hand_card_count != GameConstants.MAX_COUNT - 1)
			return GameConstants.WIK_NULL;

		int cbChiHuKind = GameConstants.WIK_NULL;
		// 无王无将
		if (is_wu_wang_wu_jiang(cards_index)) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_WU_WANG_WU_JIANG);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		// 缺一色
		if (is_que_yi_se(cards_index)) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_QUE_YI_SE);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		// 一枝花
		if (is_yi_zhi_hua(cards_index)) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_YI_ZHI_HUA);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		// 黑天胡
		if (is_hei_tian_hu(cards_index)) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_HEI_TIAN_HU);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		// 四王
		if (is_si_wang(cards_index)) {
			chiHuRight.opr_or(GameConstants_ZhuZhouWang.CHR_SI_WANG);
			cbChiHuKind = GameConstants.WIK_CHI_HU;
		}

		return cbChiHuKind;
	}

	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	// 局数总结算
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

			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);

			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);

			// 中鸟次数
			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
			// 起手胡
			player_result.addMenQingCount(_player_result.men_qing[i]);
			// 开杠次数
			player_result.addSevenCount(_player_result.an_gang_count[i] + _player_result.ming_gang_count[i]);

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
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA
				&& card < GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI
				&& card < GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_WANG_BA + GameConstants.CARD_ESPECIAL_TYPE_TING
				&& card < GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI + GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA + GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI + GameConstants.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI + GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	/**
	 * 株洲王重写洗牌方法
	 */
	@Override
	protected void init_shuffle() {
		_repertory_card = new int[GameConstants_ZhuZhouWang.CARD_DATA.length];
		shuffle(_repertory_card, GameConstants_ZhuZhouWang.CARD_DATA);
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui_zimo(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;
		int laiZi_count = 0;
		int three_num = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;
		int[] cards = new int[14];
		int hand_card_count = _logic.switch_to_cards_data(cbCardIndexTemp, cards);
		if (hand_card_count != 14)
			return GameConstants.WIK_NULL;

		boolean is_ying_qi_dui = true;
		for (int i = 0; i < hand_card_count; i = i + 2) {
			if (cards[i] != cards[i + 1]) {
				is_ying_qi_dui = false;
				break;
			}
		}

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_logic.get_magic_card_count() > 0) {
				boolean flag = false;
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m)) {
						// 王牌数量统计
						laiZi_count += cbCardCount;
						flag = true;
						continue;
					}
				}
				if (flag) {
					continue;
				}
			}
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 3) {
				three_num++;
			}

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			if (cbReplaceCount > laiZi_count) {
				return GameConstants.WIK_NULL;
			}
			// 四张牌数量
			nGenCount += three_num;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (is_ying_qi_dui)
			return GameConstants_ZhuZhouWang.QI_XIAO_DUI_YING;
		if (laiZi_count == 0) {
			return GameConstants_ZhuZhouWang.QI_XIAO_DUI_YING;
		} else {
			return GameConstants_ZhuZhouWang.QI_XIAO_DUI_RUAN;
		}

	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui_pao(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;
		int laiZi_count = 0;
		int three_num = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		if (!_logic.is_magic_index(cbCurrentIndex)) {
			cbCardIndexTemp[cbCurrentIndex]++;
		} else {
			cbReplaceCount++;
		}

		int[] cards = new int[14];
		int hand_card_count = _logic.switch_to_cards_data(cbCardIndexTemp, cards);

		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_logic.get_magic_card_count() > 0) {
				boolean flag = false;
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m)) {
						// 王牌数量统计
						laiZi_count += cbCardCount;
						flag = true;
						continue;
					}
				}
				if (flag) {
					continue;
				}
			}
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 3) {
				three_num++;
			}

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			if (cbReplaceCount > laiZi_count) {
				return GameConstants.WIK_NULL;
			}
			// 四张牌数量
			nGenCount += three_num;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (laiZi_count == 0) {
			return GameConstants_ZhuZhouWang.QI_XIAO_DUI_YING;
		} else {
			return GameConstants_ZhuZhouWang.QI_XIAO_DUI_RUAN;
		}

	}

	// 将一色(炮胡癞子牌还原本身)
	public boolean is_jiang_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count, int cur_card,
			boolean zimo) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (cards_index[i] > 0)
				return false;
		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}

			if (_logic.is_magic_index(i))
				continue;

			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_LEFT
					|| weave_items[i].weave_kind == GameConstants.WIK_CENTER
					|| weave_items[i].weave_kind == GameConstants.WIK_RIGHT)
				return false;
		}

		for (int i = 0; i < weave_count; i++) {
			int color = _logic.get_card_color(weave_items[i].center_card);
			if (color > 2)
				return false;

			int cbValue = _logic.get_card_value(weave_items[i].center_card);

			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8)) {
				return false;
			}
		}

		int curIndex = _logic.switch_to_card_index(cur_card);
		int curValue = _logic.get_card_value(cur_card);
		if (_logic.is_magic_index(curIndex)) {
			if (!zimo) {
				if ((curValue != 2) && (curValue != 5) && (curValue != 8)) {
					return false;
				}
			}
		} else {
			if ((curValue != 2) && (curValue != 5) && (curValue != 8)) {
				return false;
			}
		}

		return true;
	}

	public boolean check_ting_equals(int dstCount, int[] dstCards, int srcCount, int[] srcCards) {
		if (dstCount != srcCount)
			return false;
		for (int i = 0; i < dstCount; i++) {
			if (dstCards[i] != srcCards[i])
				return false;
		}
		return true;
	}

	// 吃牌判断
	public int check_chi_zhuzhouwang(int cards_index[], int cur_card) {
		// 参数效验
		if (_logic.is_valid_card(cur_card) == false) {
			return GameConstants.WIK_NULL;
		}

		// 过滤判断
		if (cur_card >= 0x31)
			return GameConstants.WIK_NULL;

		// 变量定义
		int excursion[] = new int[] { 0, 1, 2 };
		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 吃牌判断
		int eat_type = 0, first_index = 0;
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		for (int i = 0; i < 3; i++) {
			int value_index = cur_card_index % 9;
			if ((value_index >= excursion[i]) && ((value_index - excursion[i]) <= 6)) {
				// 吃牌判断
				first_index = cur_card_index - excursion[i];

				if ((cur_card_index != first_index) && (cards_index[first_index] == 0))
					continue;
				if ((cur_card_index != (first_index + 1)) && (cards_index[first_index + 1] == 0))
					continue;
				if ((cur_card_index != (first_index + 2)) && (cards_index[first_index + 2] == 0))
					continue;

				// 设置类型
				eat_type |= eat_type_check[i];

			}
		}

		return eat_type;
	}

	// 存在吃
	public boolean exist_chi(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if ((weaveItem[i].weave_kind == GameConstants.WIK_LEFT)
					|| (weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
					|| (weaveItem[i].weave_kind == GameConstants.WIK_RIGHT)) {
				return true;
			}
		}
		return false;
	}
}
