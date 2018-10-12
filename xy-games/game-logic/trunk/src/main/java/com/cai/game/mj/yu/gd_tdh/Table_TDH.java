package com.cai.game.mj.yu.gd_tdh;

import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_TDH;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.GdtdhPro.GDTDHGameEndResponse;

public class Table_TDH extends AbstractMJTable {

	public static final int HU_TYPE_ZI_MO = 1;
	public static final int HU_TYPE_JIE_PAO = 2;
	public static final int HU_TYPE_QIANG_GANG_HU = 3;
	public static final int HU_TYPE_GANG_KAI = 4;

	private static final long serialVersionUID = 4010366908486260271L;

	public int gen_zhuang_card; // 为-1 时本局没有更庄,为0时庄家还未出牌
	public int last_out_player_4_banker;
	public int niao_num;

	public HandlerSelectMagic_TDH handler_select_magic;
	public MJHandlerOutCardBaoTing_TDH _handler_out_card_bao_ting;

	public int[][] an_gang_card;
	public int[] an_gang_count;
	public int[][] ming_gang_card;
	public int[] ming_gang_count;
	public int[][] jie_gang_card;
	public int[] jie_gang_count;
	public int[][] dian_gang_card;
	public int[] dian_gang_count;
	public int[] hu_count;
	public int[] win_score_count;
	public int[] banker_count;
	public int[] hu_dec_type;
	public boolean[] gang_bao_quan_bao;
	public boolean gen_zhuang;
	public int[] _count_pick_niao;
	public int continue_banker;

	public Table_TDH() {
		super(MJType.GAME_TYPE_MJ_TDH);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_TDH();
		_handler_dispath_card = new HandlerDispatchCard_TDH();
		_handler_gang = new HandlerGang_TDH();
		_handler_out_card_operate = new HandlerOutCardOperate_TDH();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_TDH();
		handler_select_magic = new HandlerSelectMagic_TDH();

		hu_count = new int[getTablePlayerNumber()];
		win_score_count = new int[getTablePlayerNumber()];
		banker_count = new int[getTablePlayerNumber()];
	}

	public boolean exe_select_magic() {
		this.set_handler(this.handler_select_magic);
		handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);

		return true;
	}

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	@Override
	protected void init_shuffle() {
		List<Integer> cards_list = Lists.newArrayList();
		int[] cards = GameConstants_TDH.CARD_DATA_DAI_FENG_DAI_WAN;
		for (int i = 0; i < cards.length; i++) {
			cards_list.add(cards[i]);
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_NON_DAI_FENG)) {
			cards_list = Lists.newArrayList();
			cards = GameConstants_TDH.CARD_DATA_NON_DAI_FENG;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
			if (has_rule(GameConstants_TDH.GAME_RULE_HZ_GUI)) {
				cards_list.add(GameConstants_TDH.HZ_MAGIC_CARD);
				cards_list.add(GameConstants_TDH.HZ_MAGIC_CARD);
				cards_list.add(GameConstants_TDH.HZ_MAGIC_CARD);
				cards_list.add(GameConstants_TDH.HZ_MAGIC_CARD);
			} else if (has_rule(GameConstants_TDH.GAME_RULE_BB_GUI)) {
				cards_list.add(GameConstants_TDH.BB_MAGIC_CARD);
				cards_list.add(GameConstants_TDH.BB_MAGIC_CARD);
				cards_list.add(GameConstants_TDH.BB_MAGIC_CARD);
				cards_list.add(GameConstants_TDH.BB_MAGIC_CARD);
			}
		} else if (has_rule(GameConstants_TDH.GAME_RULE_NON_DAI_WAN)) {
			cards_list = Lists.newArrayList();
			cards = GameConstants_TDH.CARD_DATA_NON_DAI_WAN;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
		} else if (has_rule(GameConstants_TDH.GAME_RULE_DAI_FENG_DAI_WAN)) {
			cards_list = Lists.newArrayList();
			cards = GameConstants_TDH.CARD_DATA_DAI_FENG_DAI_WAN;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_SI_HUA_GUI)) {
			cards_list.add(GameConstants_TDH.CHUN_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.XIA_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.QIU_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.DONG_MAGIC_CARD);
		} else if (has_rule(GameConstants_TDH.GAME_RULE_BA_HUA_GUI)) {
			cards_list.add(GameConstants_TDH.CHUN_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.XIA_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.QIU_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.DONG_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.MEI_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.LAN_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.ZHU_MAGIC_CARD);
			cards_list.add(GameConstants_TDH.JU_MAGIC_CARD);
		}

		int[] card = new int[cards_list.size()];
		for (int i = 0; i < cards_list.size(); i++) {
			card[i] = cards_list.get(i);
		}
		_repertory_card = new int[card.length];
		shuffle(_repertory_card, card);
	}

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
		gang_bao_quan_bao = new boolean[getTablePlayerNumber()];
		_count_pick_niao = new int[getTablePlayerNumber()];
		gen_zhuang = false;
		return true;
	}

	@Override
	protected boolean on_game_start() {

		exe_select_magic();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		banker_count[_cur_banker]++;

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_TDH.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_TDH.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_TDH.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_TDH.MAX_COUNT; j++) {
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

		exe_dispatch_card(_current_player, GameConstants_TDH.DispatchCard_Type_Tian_Hu, GameConstants_TDH.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == GameConstants_TDH.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_TDH.CHR_ZI_MO);
		} else if (card_type == GameConstants_TDH.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_TDH.CHR_QING_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_TDH.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_TDH.CHR_GNAG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants_TDH.CHR_SHU_FAN);
		}

		if (GRR._left_card_count == getNIaoNum()) {
			chiHuRight.opr_or(GameConstants_TDH.CHR_HAI_DI_HU);
		}

		if (has_rule(GameConstants_TDH.GAME_RULE_HU_QI_DUI)) {
			int xiao_qi_dui = _logic.is_qi_xiao_dui_hy(cards_index, weaveItems, weave_count, cur_card);
			if (xiao_qi_dui != GameConstants_TDH.WIK_NULL) {
				int magic_count = 0;
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					magic_count += cards_index[_logic.get_magic_card_index(m)];
				}
				if (_logic.is_magic_card(cur_card)) {
					magic_count++;
				}
				if (magic_count == 0 && has_rule(GameConstants_TDH.GAME_RULE_WU_GUI_JIA_BEI)) {
					chiHuRight.opr_or(GameConstants_TDH.CHR_WU_GUI_JIA_BEI);
				}
				chiHuRight.opr_or(GameConstants_TDH.CHR_XIAO_QI_DUI);
				return GameConstants_TDH.WIK_CHI_HU;
			}
		}

		// 四鬼胡
		if (has_rule(GameConstants_TDH.GAME_RULE_SI_GUI_HU) && is_si_gui_hu(cards_index, cur_card, card_type)) {
			chiHuRight.opr_or(GameConstants_TDH.CHR_SI_GUI_HU);
			return GameConstants_TDH.WIK_CHI_HU;
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants_TDH.WIK_NULL;
		}

		int magic_count = 0;
		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			magic_count += cards_index[_logic.get_magic_card_index(m)];
		}
		if (_logic.is_magic_card(cur_card)) {
			magic_count++;
		}
		if (magic_count == 0 && has_rule(GameConstants_TDH.GAME_RULE_WU_GUI_JIA_BEI)) {
			chiHuRight.opr_or(GameConstants_TDH.CHR_WU_GUI_JIA_BEI);
		}

		return GameConstants_TDH.WIK_CHI_HU;
	}

	public boolean is_si_gui_hu(int[] cards_index, int cur_card, int card_type) {
		if (card_type != GameConstants_TDH.HU_CARD_TYPE_ZIMO && card_type != GameConstants_TDH.HU_CARD_TYPE_GANG_KAI_HUA) {
			return false;
		}

		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		if (has_rule(GameConstants_TDH.GAME_RULE_HZ_GUI) || has_rule(GameConstants_TDH.GAME_RULE_BB_GUI)
				|| has_rule(GameConstants_TDH.GAME_RULE_FAN_GUI)) {
			if (temp_cards_index[_logic.get_magic_card_index(0)] == 4) {
				return true;
			}
		}

		if (has_rule(GameConstants_TDH.GAME_RULE_FAN_SHUANG_GUI)) {
			if (temp_cards_index[_logic.get_magic_card_index(0)] == 4) {
				return true;
			}
			if (temp_cards_index[_logic.get_magic_card_index(1)] == 4) {
				return true;
			}
		}

		if (has_rule(GameConstants_TDH.GAME_RULE_SI_HUA_GUI) || has_rule(GameConstants_TDH.GAME_RULE_BA_HUA_GUI)) {
			boolean flag = true;
			for (int card = GameConstants_TDH.CHUN_MAGIC_CARD; card <= GameConstants_TDH.DONG_MAGIC_CARD; card++) {
				if (_logic.switch_to_card_index(card) == GameConstants.MAX_INDEX) {
					continue;
				}
				if (temp_cards_index[_logic.switch_to_card_index(card)] == 0) {
					flag = false;
					break;
				}
			}

			if (flag) {
				return true;
			}

			flag = true;
			for (int card = GameConstants_TDH.MEI_MAGIC_CARD; card <= GameConstants_TDH.JU_MAGIC_CARD; card++) {
				if (_logic.switch_to_card_index(card) == GameConstants.MAX_INDEX) {
					continue;
				}
				if (temp_cards_index[_logic.switch_to_card_index(card)] == 0) {
					flag = false;
					break;
				}
			}

			if (flag) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @return
	 */
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants_TDH.WIK_NULL;

		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round() && has_rule(GameConstants_TDH.GAME_RULE_QIANG_GANG_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_TDH.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants_TDH.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants_TDH.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
	}

	public int get_di_fen() {
		if (has_rule(GameConstants_TDH.GAME_RULE_DI_FEN_1)) {
			return 1;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_DI_FEN_2)) {
			return 2;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_DI_FEN_3)) {
			return 3;
		}
		return 1;
	}

	private int get_fan_shu(int seat_index) {
		int fan = 1;
		ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];
		if (!chiHuRight.opr_and(GameConstants_TDH.CHR_XIAO_QI_DUI).is_empty() && has_rule(GameConstants_TDH.GAME_RULE_QI_DUI_JIA_BEI)) {
			fan *= 2;
		}
		if (!chiHuRight.opr_and(GameConstants_TDH.CHR_ZI_MO).is_empty()) {
			fan *= 2;
		}
		if (!chiHuRight.opr_and(GameConstants_TDH.CHR_QING_GANG_HU).is_empty()) {
			fan *= 2;
		}
		if (!chiHuRight.opr_and(GameConstants_TDH.CHR_GNAG_KAI).is_empty()) {
			fan *= 2;
		}
		if (!chiHuRight.opr_and(GameConstants_TDH.CHR_WU_GUI_JIA_BEI).is_empty()) {
			fan *= 2;
		}
		if (!chiHuRight.opr_and(GameConstants_TDH.CHR_HAI_DI_HU).is_empty()) {
			fan *= 2;
		}
		return fan;
	}

	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo, int type) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		hu_count[seat_index]++;

		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_fan_shu(seat_index);// 番数

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

		float lChiHuScore = get_di_fen();
		int ma_fen = GRR._count_pick_niao;
		if (has_rule(GameConstants_TDH.GAME_RULE_YI_MA_QUAN_ZHONG)) {
			ma_fen += 2 * continue_banker;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_MA_GEN_DI_FEN)) {
			ma_fen *= get_di_fen() * wFanShu;
		} else {
			ma_fen *= 2;
		}

		switch (type) {
		case HU_TYPE_GANG_KAI: {
			hu_dec_type[seat_index] = 4;

			if (has_rule(GameConstants_TDH.GAME_RULE_GANG_BAO_QUAN_BAO) && seat_index != provide_index) {
				gang_bao_quan_bao[seat_index] = true;
				hu_dec_type[provide_index] = 11;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					float s = lChiHuScore * wFanShu; // 自摸加1分
					s += ma_fen;

					GRR._game_score[provide_index] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					hu_dec_type[i] = 5;

					float s = lChiHuScore * wFanShu; // 自摸加1分
					s += ma_fen;

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			}
			break;
		}
		case HU_TYPE_JIE_PAO: {
			hu_dec_type[seat_index] = 2;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_TDH.CHR_FANG_PAO);
			float s = lChiHuScore * wFanShu;
			s += ma_fen;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			break;
		}
		case HU_TYPE_QIANG_GANG_HU: {
			hu_dec_type[seat_index] = 3;

			if (has_rule(GameConstants_TDH.GAME_RULE_QIANG_GANG_QUAN_BAO)) {
				hu_dec_type[provide_index] = 10;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					float s = lChiHuScore * wFanShu; // 自摸翻倍
					s += ma_fen;

					GRR._game_score[provide_index] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				hu_dec_type[provide_index] = 9;

				float s = lChiHuScore * wFanShu;
				s += ma_fen;
				// 胡牌分
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
				hu_dec_type[seat_index] = 9;
			}
			break;
		}
		case HU_TYPE_ZI_MO: {
			hu_dec_type[seat_index] = 1;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				hu_dec_type[i] = 5;
				float s = lChiHuScore * wFanShu; // 自摸加1分
				s += ma_fen;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
			break;
		}

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants_TDH.INVALID_VALUE);
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (GRR._chi_hu_rights[i].is_valid()) {
				// 显示i胡牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
				hand_card_count++;
				this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
			} else {
				// 显示i胡牌
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
			}
		}
		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		hu_count[seat_index]++;

		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;// 番数

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

		float lChiHuScore = 1;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore * wFanShu; // 自摸加1分

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_TDH.CHR_FANG_PAO);
			float s = lChiHuScore * wFanShu;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants_TDH.INVALID_VALUE);
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
					if (type == GameConstants_TDH.CHR_QING_GANG_HU) {
						result.append(" 抢杠胡 x2");
						qiang_gang_hu = true;
					}
					if (type == GameConstants_TDH.CHR_ZI_MO) {
						result.append(" 自摸 x2");
					}
					if (type == GameConstants_TDH.CHR_SI_GUI_HU) {
						result.append(" 四鬼胡 x1");
					}
					if (type == GameConstants_TDH.CHR_HAI_DI_HU) {
						result.append(" 海底胡");
					}
					if (type == GameConstants_TDH.CHR_WU_GUI_JIA_BEI) {
						result.append(" 无鬼加倍 x2");
					}
					if (type == GameConstants_TDH.CHR_GNAG_KAI) {
						if (has_rule(GameConstants_TDH.GAME_RULE_GANG_BAO_QUAN_BAO) && gang_bao_quan_bao[player]) {
							result.append(" 杠爆全包 x3");
						} else {
							result.append(" 杠上花 x2");
						}
					}
					if (type == GameConstants_TDH.CHR_XIAO_QI_DUI) {
						if (has_rule(GameConstants_TDH.GAME_RULE_QI_DUI_JIA_BEI)) {
							result.append(" 七小对 x2");
						} else {
							result.append(" 七小对 x1");
						}
					}

				} else if (type == GameConstants_TDH.CHR_FANG_PAO) {
					if (hu_dec_type[player] == 10) {
						result.append(" 包牌");
						continue;
					}
					if (qiang_gang_hu) {
						if (has_rule(GameConstants_TDH.GAME_RULE_QIANG_GANG_QUAN_BAO)) {
							result.append(" 抢杠全包 x3");
						} else {
							result.append(" 被抢杠");
						}
					} else {
						result.append(" 放炮");
					}
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < this.getTablePlayerNumber(); tmpPlayer++) {
					for (int w = 0; w < GRR._weave_count[tmpPlayer]; w++) {
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants_TDH.WIK_GANG) {
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
				result.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			if (player == GRR._banker_player && gen_zhuang) {
				result.append(" 跟庄");
			}
			if (GRR._chi_hu_rights[player].is_valid() && continue_banker > 1) {
				result.append(" 连庄X").append(continue_banker - 1);
			}
			GRR._result_des[player] = result.toString();
		}
	}

	public void set_niao_card_gang(int seat_index, int card, boolean show, int add_niao) {
		if (GRR._left_card_count == 0) {
			return;
		}
		int niao_num = getNIaoNum();
		if (niao_num > GRR._left_card_count) {
			niao_num = GRR._left_card_count;
		}

		int cbCardIndexTemp[] = new int[GameConstants_TDH.MAX_INDEX];
		int[] niao_card_data = new int[30];
		_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, niao_num, cbCardIndexTemp);
		GRR._left_card_count -= niao_num;
		_logic.switch_to_cards_data(cbCardIndexTemp, niao_card_data);

		for (int i = 0; i < niao_num; i++) {
			GRR._cards_data_niao[GRR._count_niao++] = niao_card_data[i];
		}

		if (has_rule(GameConstants_TDH.GAME_RULE_YI_MA_QUAN_ZHONG)) {
			_count_pick_niao[seat_index] = getCardValue(niao_card_data[0]);
			GRR._player_niao_cards[seat_index][0] = this.set_ding_niao_valid(niao_card_data[0], true);// 胡牌的鸟生效
			GRR._player_niao_count[seat_index] = 1;
		} else {
			for (int i = 0; i < niao_num; i++) {
				GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = niao_card_data[i];
				GRR._player_niao_count[seat_index]++;
			}

			for (int j = 0; j < GRR._player_niao_count[seat_index]; j++) {
				int nValue = getCardValue(GRR._player_niao_cards[seat_index][j]);
				int seat = (GRR._banker_player + (nValue - 1) + 4) % 4;
				if (seat_index == seat) {
					_count_pick_niao[seat_index]++;
					GRR._count_pick_niao++;
					GRR._player_niao_cards[seat_index][j] = this.set_ding_niao_valid(GRR._player_niao_cards[seat_index][j], true);// 胡牌的鸟生效
				} else {
					GRR._player_niao_cards[seat_index][j] = this.set_ding_niao_valid(GRR._player_niao_cards[seat_index][j], false);// 胡牌的鸟生效
				}
			}
		}
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
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {

		GRR._cards_data_niao = new int[30];
		for (int i = 0; i < GameConstants_TDH.MAX_NIAO_CARD + 20; i++) {
			GRR._cards_data_niao[i] = GameConstants_TDH.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			GRR._player_niao_cards[i] = new int[30];
			for (int j = 0; j < GameConstants_TDH.MAX_NIAO_CARD + 20; j++) {
				GRR._player_niao_cards[i][j] = GameConstants_TDH.INVALID_VALUE;
			}
		}

		if (GRR._left_card_count == 0) {
			return;
		}
		GRR._show_bird_effect = show;
		GRR._count_niao = getNIaoNum();
		if (GRR._left_card_count < GRR._count_niao) {
			GRR._count_niao = GRR._left_card_count;
		}

		if (GRR._count_niao > GameConstants_TDH.ZHANIAO_0) {
			if (card == GameConstants_TDH.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants_TDH.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数z
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		if (has_rule(GameConstants_TDH.GAME_RULE_YI_MA_QUAN_ZHONG)) {
			GRR._count_pick_niao = getCardValue(GRR._cards_data_niao[0]);
			_count_pick_niao[seat_index] = getCardValue(GRR._cards_data_niao[0]);
			GRR._player_niao_cards[seat_index][0] = this.set_ding_niao_valid(GRR._cards_data_niao[0], true);// 胡牌的鸟生效
			GRR._player_niao_count[seat_index] = 1;
		} else {
			for (int i = 0; i < GRR._count_niao; i++) {
				int nValue = getCardValue(GRR._cards_data_niao[i]);
				int seat = (GRR._banker_player + (nValue - 1) + 4) % 4;
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
			int[] player_niao_count = new int[GameConstants_TDH.GAME_PLAYER];
			int[][] player_niao_cards = new int[GameConstants_TDH.GAME_PLAYER][GameConstants_TDH.MAX_NIAO_CARD + 20];
			for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
				player_niao_count[i] = 0;
				for (int j = 0; j < GameConstants_TDH.MAX_NIAO_CARD + 20; j++) {
					player_niao_cards[i][j] = GameConstants_TDH.INVALID_VALUE;
				}
			}
			GRR._count_pick_niao = 0;
			for (int i = 0; i < GameConstants_TDH.GAME_PLAYER; i++) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					if (seat_index == i) {
						GRR._count_pick_niao++;
						_count_pick_niao[seat_index]++;
						player_niao_cards[seat_index][player_niao_count[seat_index]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
					} else {
						player_niao_cards[seat_index][player_niao_count[seat_index]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
					}
					player_niao_count[seat_index]++;
				}
			}
			GRR._player_niao_cards = player_niao_cards;
			GRR._player_niao_count = player_niao_count;
		}
	}

	private int getCardValue(int card) {
		if (card < GameConstants_TDH.CHUN_MAGIC_CARD) {
			return _logic.get_card_value(card);
		}

		// 花牌
		switch (card) {
		case GameConstants_TDH.CHUN_MAGIC_CARD:
			return 1;
		case GameConstants_TDH.XIA_MAGIC_CARD:
			return 2;
		case GameConstants_TDH.QIU_MAGIC_CARD:
			return 3;
		case GameConstants_TDH.DONG_MAGIC_CARD:
			return 4;
		case GameConstants_TDH.MEI_MAGIC_CARD:
			return 5;
		case GameConstants_TDH.LAN_MAGIC_CARD:
			return 6;
		case GameConstants_TDH.ZHU_MAGIC_CARD:
			return 7;
		case GameConstants_TDH.JU_MAGIC_CARD:
			return 8;
		}
		return 0;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getNIaoNum() {
		int nNum = GameConstants_TDH.ZHANIAO_0;
		if (has_rule(GameConstants_TDH.GAME_RULE_MA_2)) {
			nNum = GameConstants_TDH.ZHANIAO_2;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_MA_4)) {
			nNum = GameConstants_TDH.ZHANIAO_4;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_MA_6)) {
			nNum = GameConstants_TDH.ZHANIAO_6;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_MA_8)) {
			nNum = GameConstants_TDH.ZHANIAO_8;
		}
		if (has_rule(GameConstants_TDH.GAME_RULE_YI_MA_QUAN_ZHONG)) {
			nNum = GameConstants_TDH.ZHANIAO_1;
		}

		if (niao_num == 0) {
			niao_num = nNum;
		}
		return niao_num > GRR._left_card_count ? GRR._left_card_count : niao_num;
	}

	public void set_banker(int banker, boolean _continue) {
		if (_cur_banker == banker) {
			if (has_rule(GameConstants_TDH.GAME_RULE_JIE_JIE_GAO) && !has_rule(GameConstants_TDH.GAME_RULE_YI_MA_QUAN_ZHONG)) {
				niao_num += 2;
			}
			if (_continue) {
				continue_banker++;
			} else {
				continue_banker = 0;
			}
		} else {
			niao_num = 0;
			continue_banker = 0;
		}
		_cur_banker = banker;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants_TDH.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			if (_playerStatus[i].is_bao_ting())
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_TDH.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_TDH.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			// if (_playerStatus[i].is_chi_hu_round()) {
			// boolean can_hu_this_card = true;
			// int[] tmp_cards_data_hu =
			// _playerStatus[i].get_cards_abandoned_hu();
			// for (int x = 0; x < GameConstants_TDH.MAX_ABANDONED_CARDS_COUNT;
			// x++) {
			// if (tmp_cards_data_hu[x] == card) {
			// can_hu_this_card = false;
			// break;
			// }
			// }
			// if (can_hu_this_card) {
			// ChiHuRight chr = GRR._chi_hu_rights[i];
			// chr.set_empty();
			// int cbWeaveCount = GRR._weave_count[i];
			//
			// int card_type = GameConstants_TDH.HU_CARD_TYPE_JIE_PAO;
			// action = analyse_chi_hu_card(GRR._cards_index[i],
			// GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);
			// if (action != 0) {
			// _playerStatus[i].add_action(GameConstants_TDH.WIK_CHI_HU);
			// _playerStatus[i].add_chi_hu(card, seat_index);
			// bAroseAction = true;
			// }
			// }
			// }
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants_TDH.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

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
			int ma_count = 0;
			for (int mp = 0; mp < getTablePlayerNumber(); mp++) {
				ma_count += _count_pick_niao[mp];
			}
			if (has_rule(GameConstants_TDH.GAME_RULE_YI_MA_QUAN_ZHONG)) {
				ma_count += 2 * continue_banker;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count && reason == GameConstants.Game_End_NORMAL; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						float gang_score = GRR._gang_score[i].scores[j][k];
						// 马跟杠
						if (has_rule(GameConstants_TDH.GAME_RULE_MA_GEN_GANG)) {
							gang_score *= (GRR._count_pick_niao + 1);
						}

						lGangScore[k] += gang_score;// 杠牌，每个人的分数
					}
				}

				// 记录
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i]; // 起手胡分数

				// 记录
				_player_result.game_score[i] += GRR._game_score[i];

				if (GRR._game_score[i] > 0) {
					win_score_count[i]++;
				}
			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GRR._count_niao; i++) {
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

		handler_game_end(game_end);
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
			gameEndBuilder.addHuCount(hu_count[p]);
			gameEndBuilder.addWinScoreCount(win_score_count[p]);
			gameEndBuilder.addBankerCount(banker_count[p]);
			gameEndBuilder.addGangCount(_player_result.ming_gang_count[p] + _player_result.an_gang_count[p]);
		}
		game_end.setCommResponse(PBUtil.toByteString(gameEndBuilder));
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_TDH.MAX_INDEX];
		for (int i = 0; i < GameConstants_TDH.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_TDH.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants_TDH.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_TDH.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_TDH.CHR_ZI_MO, seat_index)) {
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

	protected void test_cards() {

		// 七对
		int cards[] = new int[] { 0x11, 0x11, 0x12, 0x12, 0x14, 0x14, 0x25, 0x25, 0x16, 0x16, 0x17, 0x17, 0x19 };

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		// if (BACK_DEBUG_CARDS_MODE) {
		if (true) {
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
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

}
