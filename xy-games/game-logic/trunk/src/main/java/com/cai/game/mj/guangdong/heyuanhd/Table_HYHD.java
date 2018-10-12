package com.cai.game.mj.guangdong.heyuanhd;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuaZhou;
import com.cai.common.constant.game.mj.GameConstants_HYHD;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.GdtdhPro.GDTDHGameEndResponse;
import protobuf.clazz.mj.HuiZhou.MJ_Hui_Zhou;
import protobuf.clazz.mj.HuiZhou.MJ_Hui_Zhou_Xiao_Ju;

/**
 * 广东河源惠搭麻将
 * 
 *
 * @author luoqinwei date: 2018年8月28日 下午4:27:11 <br/>
 */
public class Table_HYHD extends AbstractMJTable {

	public HandlerSelectMagic_HYHD handler_select_magic;
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
	public int[] _count_pick_niao;
	public int nMaxCardCount;

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
		_count_pick_niao = new int[getTablePlayerNumber()];
		hu_dec_type = new int[getTablePlayerNumber()];
		nMaxCardCount = 0;
		return true;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HYHD();
		_handler_dispath_card = new HandlerDispatchCard_HYHD();
		_handler_gang = new HandlerGang_HYHD();
		_handler_out_card_operate = new HandlerOutCardOperate_HYHD();
		handler_select_magic = new HandlerSelectMagic_HYHD();
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
		this.set_handler(this.handler_select_magic);
		handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);

		return true;
	}

	/**
	 * 河源惠搭重写洗牌方法
	 */
	@Override
	protected void init_shuffle() {
		List<Integer> cards_list = Lists.newArrayList();
		if (has_rule(GameConstants_HYHD.GAME_RULE_SI_HUA_GUI)) {
			cards_list = Lists.newArrayList();
			_repertory_card = new int[GameConstants_HYHD.CARD_DATA_SI_HUA.length];
			shuffle(_repertory_card, GameConstants_HYHD.CARD_DATA_SI_HUA);

		} else if (has_rule(GameConstants_HYHD.GAME_RULE_BA_HUA_GUI)) {
			cards_list = Lists.newArrayList();
			_repertory_card = new int[GameConstants_HYHD.CARD_DATA_BA_HUA.length];
			shuffle(_repertory_card, GameConstants_HYHD.CARD_DATA_BA_HUA);
		} else {
			cards_list = Lists.newArrayList();
			_repertory_card = new int[GameConstants_HYHD.CARD_DATA_NOT_HUA.length];
			shuffle(_repertory_card, GameConstants_HYHD.CARD_DATA_NOT_HUA);
		}
	}

	@Override
	public boolean on_game_start() {
		// 刷新玩家信息
		refresh_less_player();

		// 开局处理
		on_game_start_for_heyuan();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.INVALID_VALUE, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[GameConstants_HYHD.MAX_MAGIC_CARD];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 8) { // 一般只有两种癞子牌存在
			magic_card_count = 8;
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

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

		int hand_card_count = _logic.get_card_count_by_index(tmp_hand_cards_index);

		int card_color_count = _logic.get_se_count(tmp_hand_cards_index, weaveItems, weave_count);

		boolean has_feng = check_feng(tmp_hand_cards_index, weaveItems, weave_count);

		// 是不是七小对
		boolean is_qi_xiao_dui = false;

		is_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card) != GameConstants.WIK_NULL;

		// 是不是十三幺
		boolean is_shi_san_yao = isShiSanYao(tmp_hand_cards_index, weaveItems, weave_count);

		boolean is_pi_hu = true;

		if (is_shi_san_yao || is_qi_xiao_dui) {
			// 十三幺
			if (is_shi_san_yao) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_SHI_SAN_YAO);
				is_pi_hu = false;
			}

			if (is_qi_xiao_dui) {
				// 可胡七小对
				if (is_qi_xiao_dui) {
					if (card_color_count == 0 || (!has_feng && card_color_count == 1))
						chiHuRight.opr_or(GameConstants_HYHD.CHR_QING_QI_DUI);
					else
						chiHuRight.opr_or(GameConstants_HYHD.CHR_QI_DUI);
					is_pi_hu = false;
				}

			}
		} else if (can_win) {
			// 能不能碰胡
			boolean is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_for_huizhou(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
			// 有没有碰杠四次
			boolean exist_eat = exist_peng_or_gang_four(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
			boolean dui_dui_hu = is_dui_dui_hu && exist_eat;
			// 是不是幺九
			boolean have_yao_jiu = _logic.is_yao_jiu(tmp_hand_cards_index, weaveItems, weave_count);
			boolean is_yao_jiu = have_yao_jiu && dui_dui_hu;

			if (dui_dui_hu) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_DUI_DUI_HU);
				is_pi_hu = false;
			}

			if (has_feng && card_color_count == 1) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_HUN_YI_SE);
				is_pi_hu = false;
			}

			if (!has_feng && card_color_count == 1) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_QING_YI_SE);
				is_pi_hu = false;
			}

			if (has_feng && card_color_count == 1 && dui_dui_hu) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_HUN_DUI_DUI);
				is_pi_hu = false;
			}

			if (!has_feng && card_color_count == 1 && dui_dui_hu) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_QING_DUI_DUI);
				is_pi_hu = false;
			}

			// 幺九
			if (has_feng && is_yao_jiu && card_color_count != 0) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_YAO_JIU);
				is_pi_hu = false;
			}

			// 清幺九
			if (has_feng && have_yao_jiu && card_color_count == 1) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_QING_YAO_JIU);
				is_pi_hu = false;
			}

			// 全幺九
			if (!has_feng && have_yao_jiu) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_QUAN_YAO_JIU);
				is_pi_hu = false;
			}

		} else if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean have_hua_diao_hua = false;

		if (_logic.is_valid_card(cur_card)) {
			int count = 0;
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				boolean hu = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, i,
						magic_cards_index, magic_card_count);
				if (hu) {
					count++;
				}
			}
			if (count == GameConstants.MAX_INDEX) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_HUA_DAN_DIAO);
				// 花吊花
				if (_logic.is_magic_card(cur_card)) {
					chiHuRight.opr_or(GameConstants_HYHD.CHR_HUA_DIAO_HUA);
					have_hua_diao_hua = true;
				}
				is_pi_hu = false;
			}
		}

		if ((card_type == GameConstants_HYHD.HU_CARD_TYPE_JIE_PAO) && _logic.is_magic_card(cur_card)
				&& (!have_hua_diao_hua)) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants_HYHD.GAME_RULE_JI_HU_KE_HU) && is_pi_hu) {
			chiHuRight.opr_or(GameConstants_HYHD.CHR_PI_HU);
		}

		if (has_rule(GameConstants_HYHD.GAME_RULE_WU_HUA_JIA_BEI) && !is_have_gui(_seat_index)
				&& !_logic.is_magic_card(cur_card)) {
			chiHuRight.opr_or(GameConstants_HYHD.CHR_WU_GUI);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants_HYHD.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants_HYHD.HU_CARD_TYPE_JIE_PAO) {
			if (has_rule(GameConstants_HYHD.GAME_RULE_KE_JIE_PAO_HU)) {
				chiHuRight.opr_or(GameConstants_HYHD.CHR_JIE_PAO);
			}
		} else if (card_type == GameConstants_HYHD.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_HYHD.CHR_QIANG_GANG);
		}

		// 屁胡不可胡
		if (!has_rule(GameConstants_HYHD.GAME_RULE_JI_HU_KE_HU) && is_pi_hu) {
			cbChiHuKind = GameConstants.WIK_NULL;
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			playerStatus = _playerStatus[i];
			if (GRR._left_card_count > 0 && !_logic.is_magic_card(card)) {
				boolean can_peng_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng_this_card = false;
						break;
					}
				}
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && can_peng_this_card) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}
			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = GameConstants_HYHD.HU_CARD_TYPE_JIE_PAO;
				if (has_rule(GameConstants_HYHD.GAME_RULE_KE_JIE_PAO_HU)) {
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
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

		di_fen = get_hu_pai_xing_fen(chr, seat_index) * GameConstants_HYHD.CELL_SCORE;

		total_socre = di_fen;

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
			boolean bao_jiu_quan_bao = false;
			hu_dec_type[seat_index] = 1;
			if (has_rule(GameConstants_HYHD.GAME_RULE_BAO_JIU_QUAN_BAO)
					&& !chr.opr_and(GameConstants_HYHD.CHR_DUI_DUI_HU).is_empty()) {
				int weaveCount = GRR._weave_count[seat_index];
				int provide_player = 0;

				for (int i = weaveCount - 1; i >= 0; i--) {
					if (GRR._weave_items[seat_index][i].provide_player != seat_index) {
						provide_player = GRR._weave_items[seat_index][i].provide_player;
						bao_jiu_quan_bao = true;
						break;
					}
				}
				if (bao_jiu_quan_bao) {
					GRR._game_score[provide_player] -= (getTablePlayerNumber() - 1) * total_socre;
					GRR._game_score[seat_index] += (getTablePlayerNumber() - 1) * total_socre;
					cal_ma_gen_di_fen_and_gang_fen(seat_index, provide_index, false, this.getTablePlayerNumber() - 1,
							true);
					return;
				}

			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._game_score[i] -= total_socre;
				GRR._game_score[seat_index] += total_socre;

				GRR._chi_hu_rights[i].opr_or(GameConstants_HYHD.CHR_BEI_ZI_MO);
			}
			cal_ma_gen_di_fen_and_gang_fen(seat_index, provide_index, false, this.getTablePlayerNumber() - 1, true);

		} else {
			// 抢杠全包
			if (!chr.opr_and(GameConstants_HYHD.CHR_QIANG_GANG).is_empty()) {
				hu_dec_type[seat_index] = 3;
				GRR._game_score[provide_index] -= total_socre;
				GRR._game_score[seat_index] += total_socre;
				GRR._chi_hu_rights[provide_index].opr_or(GameConstants_HYHD.CHR_BEI_QIANG_GANG);
				cal_ma_gen_di_fen_and_gang_fen(seat_index, provide_index, false, 1, false);
			} else {
				hu_dec_type[seat_index] = 2;
				// 针对8分以上的特殊胡，如果A玩家让B玩家十二张落地（碰碰胡），如果B胡牌，则A包B胡牌的全部分数，抢杠优于报九
				boolean bao_jiu_quan_bao = true;
				if (has_rule(GameConstants_HYHD.GAME_RULE_BAO_JIU_QUAN_BAO)
						&& !chr.opr_and(GameConstants_HYHD.CHR_DUI_DUI_HU).is_empty()) {
					int weaveCount = GRR._weave_count[seat_index];
					int provide_player = 0;

					for (int i = weaveCount - 1; i >= 0; i--) {
						if (GRR._weave_items[seat_index][i].provide_player != seat_index) {
							provide_player = GRR._weave_items[seat_index][i].provide_player;
							bao_jiu_quan_bao = true;
							break;
						}
					}
					if (bao_jiu_quan_bao) {
						GRR._game_score[provide_player] -= total_socre;
						GRR._game_score[seat_index] += total_socre;
						cal_ma_gen_di_fen_and_gang_fen(seat_index, provide_index, false, 1, false);
						return;
					}
				}

				GRR._game_score[provide_index] -= total_socre;
				GRR._game_score[seat_index] += total_socre;
				if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuaZhou.CHR_JIE_PAO).is_empty())
					GRR._chi_hu_rights[provide_index].opr_or(GameConstants_HYHD.CHR_FANG_PAO);
				cal_ma_gen_di_fen_and_gang_fen(seat_index, provide_index, false, 1, false);
			}
		}
	}

	/**
	 * 
	 * 计算马跟底分and杠分
	 * 
	 * @param seat_index
	 *            胡牌的玩家
	 * @param provide_player
	 *            出分的玩家 (全包)
	 * @param is_bao
	 *            是否是一个人出分(抢杠全包/刚上开花)
	 * @param num
	 *            收几个人的分
	 */
	private void cal_ma_gen_di_fen_and_gang_fen(int seat_index, int provide_player, boolean is_bao, int num,
			boolean is_zimo) {
		MJ_Hui_Zhou_Xiao_Ju.Builder b = MJ_Hui_Zhou_Xiao_Ju.newBuilder();
		float lGangScore[] = new float[this.getTablePlayerNumber()];

		boolean have_gang = false;
		int zhong_ma_shu = GRR._player_niao_count[seat_index];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < this.getTablePlayerNumber(); k++) {
					if (i == seat_index) {
						if (GRR._gang_score[i].scores[j][k] > 0) {
							have_gang = true;
						}
						if (has_rule(GameConstants_HYHD.GAME_RULE_MA_GEN_GANG)) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k] * zhong_ma_shu;
						}
					}
					// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}
		boolean only_ma = true;// 只选了中马
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == GameConstants.INVALID_SEAT) {
				continue;
			}

			// 马跟底分
			if (has_rule(GameConstants_HYHD.GAME_RULE_MA_GEN_DIFEN)) {
				only_ma = false;
				if (i == seat_index) {
					GRR._game_score[i] += zhong_ma_shu * di_fen * num;
				} else {
					if (is_bao) {
						if (i == provide_player) {
							GRR._game_score[provide_player] -= zhong_ma_shu * di_fen * num;
						}
					} else {
						if (is_zimo) {
							GRR._game_score[i] -= zhong_ma_shu * di_fen;
						} else {
							if (i == provide_player)
								GRR._game_score[i] -= zhong_ma_shu * di_fen;
						}
					}
				}
			}
			// 马跟杠分
			if (has_rule(GameConstants_HYHD.GAME_RULE_MA_GEN_GANG)) {
				if (have_gang)
					only_ma = false;
				// GRR._game_score[i] += lGangScore[i] * zhong_ma_shu;
			}

			if (only_ma) {
				// 如果是选买马 一个马就是两分

				if (i == seat_index) {
					GRR._game_score[i] += zhong_ma_shu * num * 1;
				} else {
					if (is_zimo) {
						GRR._game_score[i] -= zhong_ma_shu * 1;
					} else {
						if (i == provide_player)
							GRR._game_score[i] -= zhong_ma_shu * 1;
					}
				}
			}

			GRR._game_score[i] += lGangScore[i];
			GRR._game_score[i] += GRR._start_hu_score[i];
			_player_result.game_score[i] += GRR._game_score[i];

			b.addFen(new Float(GRR._game_score[i]).intValue());

		}
		pai_ju_ji_lu[_cur_round - 1] = b.build();

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

		if (!chr.opr_and(GameConstants_HYHD.CHR_QUAN_YAO_JIU).is_empty()) {
			di_fen = 13;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_SHI_SAN_YAO).is_empty()) {
			di_fen = 13;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_YAO_JIU).is_empty()) {
			di_fen = 10;
			if (has_rule(GameConstants_HYHD.GAME_RULE_WU_HUA_JIA_BEI)) {
				di_fen = 8;
			}
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_DUI_DUI).is_empty()) {
			di_fen = 8;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_QI_DUI).is_empty()) {
			di_fen = 9;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_YAO_JIU).is_empty()) {
			di_fen = 8;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_HUN_DUI_DUI).is_empty()) {
			di_fen = 5;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_YI_SE).is_empty()) {
			di_fen = 5;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_QI_DUI).is_empty()) {
			di_fen = 4;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_DUI_DUI_HU).is_empty()) {
			di_fen = 3;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_HUN_YI_SE).is_empty()) {
			di_fen = 2;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_PI_HU).is_empty()) {
			di_fen = 1;
		}

		// 花吊花和花单吊累加
		if (!chr.opr_and(GameConstants_HYHD.CHR_HUA_DIAO_HUA).is_empty()) {
			di_fen += 4;
		} else if (!chr.opr_and(GameConstants_HYHD.CHR_HUA_DAN_DIAO).is_empty()) {
			di_fen += 2;
		}

		// 无鬼加倍
		if (has_rule(GameConstants_HYHD.GAME_RULE_WU_HUA_JIA_BEI)) {
			if (!chr.opr_and(GameConstants_HYHD.CHR_WU_GUI).is_empty()) {
				di_fen = di_fen * 2;
			}
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
				if (!chr.opr_and(GameConstants_HYHD.CHR_QUAN_YAO_JIU).is_empty()) {
					result.append(" 全幺九");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_SHI_SAN_YAO).is_empty()) {
					result.append(" 十三幺");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_YAO_JIU).is_empty()) {
					result.append(" 清幺九");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_QI_DUI).is_empty()) {
					result.append(" 清七对");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_DUI_DUI).is_empty()) {
					result.append(" 清碰");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_YAO_JIU).is_empty()) {
					result.append(" 幺九");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_HUN_DUI_DUI).is_empty()) {
					result.append(" 混碰");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_QING_YI_SE).is_empty()) {
					result.append(" 清一色");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_QI_DUI).is_empty()) {
					result.append(" 七对");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_DUI_DUI_HU).is_empty()) {
					result.append(" 碰碰胡");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_HUN_YI_SE).is_empty()) {
					result.append(" 混一色");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_PI_HU).is_empty()) {
					result.append(" 鸡胡");
				}
				if (!chr.opr_and(GameConstants_HYHD.CHR_HUA_DIAO_HUA).is_empty()) {
					result.append(" 花吊花");
				} else if (!chr.opr_and(GameConstants_HYHD.CHR_HUA_DAN_DIAO).is_empty()) {
					result.append(" 花单吊");
				}

				if (!chr.opr_and(GameConstants_HYHD.CHR_QIANG_GANG).is_empty()) {
					result.append(" 抢杠胡");
				}

			}
			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == GameConstants_HYHD.CHR_WU_GUI) {
						result.append(" 无鬼");
					}

					if (type == GameConstants_HYHD.CHR_ZI_MO) {
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
				result.append(" 碰杠X" + ming_gang);
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

		int max_ting_count = nMaxCardCount;
		int real_max_ting_count = nMaxCardCount;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants_HYHD.CHR_ZI_MO, seat_index)) {
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
	 * 检测有没有风牌（东南西北中发白）
	 * 
	 * @param cards_index
	 *            手牌
	 * @param weave_items
	 *            落地的牌
	 * @param weave_count
	 *            落地牌的数量
	 * @return true有/false没有
	 */
	public boolean check_feng(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int card_color = _logic.get_card_color(weave_items[i].center_card);
			if (card_color > 2)
				return true;
		}
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 0) {
				int card_color = _logic.get_card_color(_logic.switch_to_card_data(i));
				if (card_color > 2)
					return true;
			}
		}

		return false;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getNIaoNum() {
		int nNum = GameConstants.ZHANIAO_0;
		if (has_rule(GameConstants_HYHD.GAME_RULE_MA_2)) {
			nNum = GameConstants.ZHANIAO_2;
		}
		if (has_rule(GameConstants_HYHD.GAME_RULE_MA_4)) {
			nNum = GameConstants.ZHANIAO_4;
		}
		if (has_rule(GameConstants_HYHD.GAME_RULE_MA_6)) {
			nNum = GameConstants.ZHANIAO_6;
		}
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

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
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

				game_end.setCellScore(GameConstants_HYHD.CELL_SCORE);

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
	public void set_niao_card(int card, boolean show) {
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
		GRR._count_pick_niao = GRR._count_niao;

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = getCardValue(GRR._cards_data_niao[i]);
			// 中马以庄家的位置计算
			int seat = get_seat_by_value(nValue, GRR._banker_player);
			ChiHuRight chr = GRR._chi_hu_rights[seat];
			if (chr.is_valid()) {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i]
						+ GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				GRR._cards_data_niao[i] += GameConstants.CARD_ESPECIAL_TYPE_NIAO;
			} else {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			}
			GRR._player_niao_count[seat]++;
		}
	}

	private int getCardValue(int card) {
		if (card < GameConstants_HYHD.CHUN_MAGIC_CARD) {
			return _logic.get_card_value(card);
		}

		// 花牌
		switch (card) {
		case GameConstants_HYHD.CHUN_MAGIC_CARD:
			return 1;
		case GameConstants_HYHD.XIA_MAGIC_CARD:
			return 2;
		case GameConstants_HYHD.QIU_MAGIC_CARD:
			return 3;
		case GameConstants_HYHD.DONG_MAGIC_CARD:
			return 4;
		case GameConstants_HYHD.MEI_MAGIC_CARD:
			return 5;
		case GameConstants_HYHD.LAN_MAGIC_CARD:
			return 6;
		case GameConstants_HYHD.ZHU_MAGIC_CARD:
			return 7;
		case GameConstants_HYHD.JU_MAGIC_CARD:
			return 8;
		}
		return 0;
	}

	/**
	 * 游戏规则描述
	 * 
	 * @return
	 */
	public String getGameRuleDes() {
		StringBuilder stringBuilder = new StringBuilder();

		if (has_rule(GameConstants_HYHD.GAME_RULE_SI_HUA_GUI)) {
			stringBuilder.append("4花鬼");
		}
		if (has_rule(GameConstants_HYHD.GAME_RULE_BA_HUA_GUI)) {
			stringBuilder.append("8花鬼");
		}
		if (has_rule(GameConstants_HYHD.GAME_RULE_MAI_MA)) {
			stringBuilder.append("买马");
		}
		if (has_rule(GameConstants_HYHD.GAME_RULE_MA_GEN_DIFEN)) {
			stringBuilder.append("马跟底分");
		}
		if (has_rule(GameConstants_HYHD.GAME_RULE_MA_GEN_GANG)) {
			stringBuilder.append("马跟杠分");
		}
		return stringBuilder.toString();
	}

	/**
	 * 河源麻将牌局开始需处理
	 */
	public void on_game_start_for_heyuan() {
		di_fen = 0;
		gameRuleDes = getGameRuleDes();

		// 坐庄次数
		_player_result.lose_num[_cur_banker]++;

		nMaxCardCount = GameConstants.MAX_ZI_FENG;
		if (has_rule(GameConstants_HYHD.GAME_RULE_SI_HUA_GUI)) {
			nMaxCardCount = GameConstants.MAX_ZI_FENG + 4;
		} else if (has_rule(GameConstants_HYHD.GAME_RULE_BA_HUA_GUI)) {
			nMaxCardCount = GameConstants.MAX_INDEX;
		}

		exe_select_magic();
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
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_HYHD.HU_CARD_TYPE_QIANG_GANG, i);

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
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x09, 0x09, 0x09, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19,
				0x38 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x01, 0x09, 0x09, 0x09, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19,
				0x38 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x19, 0x21, 0x21, 0x21, 0x21,
				0x31 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x19, 0x21, 0x21, 0x21, 0x21,
				0x31 };

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

	// 显示鬼牌
	public void showSpecialCard(int seat_index) {
		if (has_rule(GameConstants_HYHD.GAME_RULE_SI_HUA_GUI) || has_rule(GameConstants_HYHD.GAME_RULE_BA_HUA_GUI)) {
			int[] magicCard = new int[_logic.get_magic_card_count()];
			magicCard = _logic.get_all_magic_card_index();
			operate_show_card(seat_index, GameConstants.Show_Card_Center, _logic.get_magic_card_count(), magicCard,
					seat_index);
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

	// 碰杠四手
	public boolean exist_peng_or_gang_four(WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount != GameConstants.MAX_WEAVE) {
			return false;
		}
		for (int i = 0; i < cbWeaveCount; i++) {
			if ((weaveItem[i].weave_kind != GameConstants.WIK_PENG)
					&& (weaveItem[i].weave_kind != GameConstants.WIK_GANG)) {
				return false;
			}
		}
		return true;
	}
}
