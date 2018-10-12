package com.cai.game.mj.wuyuanmj;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuiZhou;
import com.cai.common.constant.game.Constants_WuYuan;
import com.cai.common.constant.game.Constants_YangZhong;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
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
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 婺源麻将
 * 
 * @author admin
 *
 */
public class Table_WuYuan extends AbstractMJTable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4834189625040429103L;

	public HandlerSelectMagicCard_WuYuan _handler_select_magic_card;
	/**
	 * 癞子牌(百变牌)
	 */
	public int joker_card_1 = 0;

	public int joker_card_index_1 = -1;
	/**
	 * 飞宝(打出去的癞子牌)
	 */
	public int fei_bao[];
	/**
	 * 每局摸到的宝牌数
	 */
	public int mo_bao_pai[];
	/**
	 * 摸牌次数
	 */
	public int dispatchcardNum[];
	/**
	 * 补杠
	 */
	public int bu_gang[];

	/**
	 * 当前胡牌玩家
	 */
	public int hu_pai_player;

	public Table_WuYuan() {
		super(MJType.GAME_TYPE_MJ_HUIZHOU);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_WuYuan();
		_handler_dispath_card = new HandlerDispatchCard_WuYuan();
		_handler_gang = new HandlerGang_WuYuan();
		_handler_out_card_operate = new HandlerOutCardOperate_WuYuan();
		_handler_select_magic_card = new HandlerSelectMagicCard_WuYuan();

		fei_bao = new int[GameConstants.GAME_PLAYER_FOUR];
		mo_bao_pai = new int[GameConstants.GAME_PLAYER_FOUR];
		dispatchcardNum = new int[GameConstants.GAME_PLAYER_FOUR];
		bu_gang = new int[GameConstants.GAME_PLAYER_FOUR];
		hu_pai_player = GameConstants.INVALID_SEAT;
	}

	protected void exe_select_magic_card() {
		set_handler(_handler_select_magic_card);
		_handler_select_magic_card.exe(this);
	}

	@Override
	public boolean on_game_start() {
		on_game_start_for_wuyuan();
		exe_select_magic_card();
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
			operate_di_fen_bei_shu(i);
		}

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
				if (_logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
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
		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_WuYuan.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_ZI_MO);
		} else if (card_type == Constants_WuYuan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_JIE_PAO);
		} else if (card_type == Constants_WuYuan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_ZI_MO);
			chiHuRight.opr_or(Constants_WuYuan.CHR_GANG_SHANG_KAI_HUA);
		} else if (card_type == Constants_WuYuan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_ZI_MO);
			chiHuRight.opr_or(Constants_WuYuan.CHR_QIANG_GANG);
		}

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int[] magic_cards_index = new int[Constants_HuiZhou.MAX_MAGIC_CARD];
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
		int magic_count = tmp_hand_cards_index[_logic.get_magic_card_index(0)];
		if (_logic.is_valid_card(cur_card)) {
			tmp_hand_cards_index[_logic.switch_to_card_index(cur_card)]++;
		}

		float total_fen = 0.5f;
		boolean can_win = false;
		if (_logic.is_magic_card(cur_card)
				&& (card_type == Constants_WuYuan.HU_CARD_TYPE_JIE_PAO || card_type == Constants_WuYuan.HU_CARD_TYPE_QIANG_GANG)) {
			can_win = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
		} else {
			can_win = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
		}

		int hand_card_count = _logic.get_card_count_by_index(tmp_hand_cards_index);

		int card_color_count = get_se_count(tmp_hand_cards_index, weaveItems, weave_count, false);

		boolean has_feng = check_feng(tmp_hand_cards_index, weaveItems, weave_count);

		int qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, false);

		// 十三烂--
		int shiSanLan = isQXShiSanLan(tmp_hand_cards_index, weaveItems, weave_count, false);
		// 是不是幺九
		int is_yao_jiu = is_yao_jiu(tmp_hand_cards_index, weaveItems, weave_count, false);
		// 硬胡
		boolean can_win_ying_hu = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index,
				_logic.switch_to_card_index(cur_card), magic_cards_index, 0);
		int qi_xiao_dui_ying_hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, true);
		int card_color_count_ying_hu = get_se_count(tmp_hand_cards_index, weaveItems, weave_count, true);
		int shiSanLan_ying_hu = isQXShiSanLan(tmp_hand_cards_index, weaveItems, weave_count, true);
		int is_yao_jiu_ying_hu = is_yao_jiu(tmp_hand_cards_index, weaveItems, weave_count, true);

		boolean is_pi_hu = true;

		if (qi_xiao_dui > 0 || shiSanLan > 0 || is_yao_jiu > 0) {
			is_pi_hu = false;
			// 天胡、地胡
			if (dispatchcardNum(_seat_index) == 1 && hand_card_count == 14 && _out_card_count == 0) {
				if (_seat_index == _cur_banker) {
					chiHuRight.opr_or(Constants_WuYuan.CHR_TIAN_HU);
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					return cbChiHuKind;
				}
			}

			if (dispatchcardNum(_cur_banker) == 1 && dispatchcardNum(_seat_index) == 0 && hand_card_count == 14
					&& chiHuRight.opr_and(Constants_WuYuan.HU_CARD_TYPE_QIANG_GANG).is_empty()) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_DI_HU);
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				return cbChiHuKind;
			}

			// 七对
			if (qi_xiao_dui == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_HAO_HUA_QI_XIAO_DUI);
			} else if (qi_xiao_dui == GameConstants.CHR_HUNAN_QI_XIAO_DUI && card_color_count == 1) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QING_QI_SE_QI_DUI);
			} else if (qi_xiao_dui == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QI_DUI);

			}

			if (shiSanLan == 1) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_SHI_YAN_LAN);
			} else if (shiSanLan == 2) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QI_XING_SHI_SAN_LAN);
			}

			if (is_yao_jiu == 1) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_JIU_YAO);
				is_pi_hu = false;
			} else if (is_yao_jiu == 2) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QI_XING_JIU_YAO);
				is_pi_hu = false;
			}
		} else if (can_win) {
			// 天胡、地胡
			if (dispatchcardNum(_seat_index) == 1 && hand_card_count == 14 && _out_card_count == 0) {
				if (_seat_index == _cur_banker) {
					chiHuRight.opr_or(Constants_WuYuan.CHR_TIAN_HU);
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					return cbChiHuKind;
				}
			}

			if (dispatchcardNum(_cur_banker) == 1 && dispatchcardNum(_seat_index) == 0 && hand_card_count == 14
					&& chiHuRight.opr_and(Constants_WuYuan.HU_CARD_TYPE_QIANG_GANG).is_empty()) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_DI_HU);
				cbChiHuKind = GameConstants.WIK_CHI_HU;
				return cbChiHuKind;
			}

			// 能不能碰胡
			boolean is_dui_dui_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_for_huizhou(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
			// 有没有吃牌
			boolean exist_eat = exist_eat(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
			boolean dui_dui_hu = is_dui_dui_hu && !exist_eat;

			if (dui_dui_hu) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_PENG_PENG_HU);
				is_pi_hu = false;
			}

			if (!has_feng && card_color_count == 1) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QING_YI_SE);
				is_pi_hu = false;
			}

			if (!has_feng && card_color_count == 1 && dui_dui_hu) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QING_YI_SE_PENG_PENG_HU);
				is_pi_hu = false;
			}
		} else if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (fei_bao[_seat_index] > 0) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_FEI_BAO);
		}

		if (_logic.is_valid_card(cur_card)) {
			int count = 0;
			for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
				boolean hu = false;
				if (qi_xiao_dui > 0) {
					hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, _logic.switch_to_card_data(i), false) > 0;
				} else {
					hu = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, i, magic_cards_index, magic_card_count);
				}
				if (hu) {
					count++;
				}
			}
			if (count == GameConstants.MAX_ZI_FENG) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_BAO_DIAO);
			}
		}

		if (is_pi_hu) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_PI_HU);
		}

		if (is_yao_jiu == 1 && is_yao_jiu_ying_hu == 1) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_WU_BAO);
		} else if (is_yao_jiu == 2 && is_yao_jiu_ying_hu == 2) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_WU_BAO);
		} else if (!is_have_gui(_seat_index) && !_logic.is_magic_card(cur_card)) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_WU_BAO);
		}

		if (magic_count == 4) {
			chiHuRight.opr_or(Constants_WuYuan.CHR_SI_BAO);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		int pai_xing_fen = get_pai_xing_fen(chiHuRight);
		total_fen *= pai_xing_fen * get_fan_bei_shu(chiHuRight, _cur_banker);

		/**
		 * 硬胡计算
		 *
		 */
		ChiHuRight chiHuRight_ying_hu = new ChiHuRight();
		if (card_type == Constants_WuYuan.HU_CARD_TYPE_ZI_MO) {
			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_ZI_MO);
		} else if (card_type == Constants_WuYuan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_JIE_PAO);
		} else if (card_type == Constants_WuYuan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_ZI_MO);
			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_GANG_SHANG_KAI_HUA);
		} else if (card_type == Constants_WuYuan.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_ZI_MO);
			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_QIANG_GANG);
		}
		float total_fen1 = 0;
		boolean is_pi_hu_ying_hu = true;

		if (qi_xiao_dui_ying_hu > 0 || shiSanLan_ying_hu > 0 || is_yao_jiu_ying_hu > 0) {
			is_pi_hu_ying_hu = false;
			// 七对
			if (qi_xiao_dui_ying_hu == GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_HAO_HUA_QI_XIAO_DUI);
			} else if (qi_xiao_dui_ying_hu == GameConstants.CHR_HUNAN_QI_XIAO_DUI && card_color_count == 1) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_QING_QI_SE_QI_DUI);
			} else if (qi_xiao_dui_ying_hu == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_QI_DUI);

			}
			if (shiSanLan == 1) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_SHI_YAN_LAN);
			} else if (shiSanLan == 2) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_QI_XING_SHI_SAN_LAN);
			}

			if (is_yao_jiu == 1) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_JIU_YAO);
			} else if (is_yao_jiu == 2) {
				chiHuRight.opr_or(Constants_WuYuan.CHR_QI_XING_JIU_YAO);
			}

			if (fei_bao[_seat_index] > 0) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_FEI_BAO);
			}

			if (is_pi_hu_ying_hu) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_PI_HU);
			}

			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_WU_BAO);

			if (_logic.is_valid_card(cur_card)) {
				int count = 0;
				for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
					boolean hu = false;
					if (qi_xiao_dui_ying_hu > 0) {
						hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, _logic.switch_to_card_data(i), false) > 0;
					}
					if (hu) {
						count++;
					}
				}
				if (count == GameConstants.MAX_ZI_FENG) {
					chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_BAO_DIAO);
				}
			}
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			total_fen1 = 0.5F;
		} else if (can_win_ying_hu) {
			// 能不能碰胡
			boolean is_dui_dui_hu_ying_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index_for_huizhou(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, 0);
			// 有没有吃牌
			boolean exist_eat = exist_eat(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);
			boolean dui_dui_hu_ying_hu = is_dui_dui_hu_ying_hu && !exist_eat;

			if (dui_dui_hu_ying_hu) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_PENG_PENG_HU);
				is_pi_hu_ying_hu = false;
			}

			if (!has_feng && card_color_count_ying_hu == 1) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_QING_YI_SE);
				is_pi_hu_ying_hu = false;
			}

			if (!has_feng && card_color_count_ying_hu == 1 && dui_dui_hu_ying_hu) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_QING_YI_SE_PENG_PENG_HU);
				is_pi_hu_ying_hu = false;
			}

			if (fei_bao[_seat_index] > 0) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_FEI_BAO);
			}

			if (is_pi_hu_ying_hu) {
				chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_PI_HU);
			}

			chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_WU_BAO);

			if (_logic.is_valid_card(cur_card)) {
				int count = 0;
				for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
					boolean hu = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, i, magic_cards_index,
							magic_card_count);
					if (hu) {
						count++;
					}
				}
				if (count == GameConstants.MAX_ZI_FENG) {
					chiHuRight_ying_hu.opr_or(Constants_WuYuan.CHR_BAO_DIAO);
				}
			}

			cbChiHuKind = GameConstants.WIK_CHI_HU;
			total_fen1 = 0.5F;
		} else if (!can_win && !can_win_ying_hu && qi_xiao_dui <= 0 && shiSanLan <= 0 && is_yao_jiu_ying_hu <= 0) {
			chiHuRight_ying_hu.set_empty();
			return GameConstants.WIK_NULL;
		}

		int pai_xing_fen1 = get_pai_xing_fen(chiHuRight_ying_hu);
		total_fen1 *= pai_xing_fen1 * get_fan_bei_shu(chiHuRight_ying_hu, _cur_banker);
		if (total_fen1 >= 4) {
			if (total_fen1 > total_fen) {
				// chiHuRight = chiHuRight_ying_hu;
				chiHuRight.setType_list(chiHuRight_ying_hu.getType_list());
				chiHuRight.setType_count(chiHuRight_ying_hu.getType_count());
				chiHuRight.m_dwRight = chiHuRight_ying_hu.m_dwRight;
			}
		} else {
			if (total_fen < 4) {
				return GameConstants.WIK_NULL;
			}
		}
		// 除地胡以外不能接炮
		if (!chiHuRight.opr_and(Constants_WuYuan.CHR_JIE_PAO).is_empty() && chiHuRight.opr_and(Constants_WuYuan.CHR_DI_HU).is_empty()) {
			return GameConstants.WIK_NULL;
		}
		return cbChiHuKind;
	}

	/**
	 * 获取花色门数
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @param is_ying_hu
	 *            硬胡
	 * @return
	 */
	public int get_se_count(int cards_index[], WeaveItem weaveItem[], int weaveCount, boolean is_ying_hu) {
		int cbQueYiMenColor[] = new int[] { 1, 1, 1 };// 缺一色
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cards_index[i];// 数量
			if (cbCardCount == 0) {
				continue;
			}
			if (_logic.is_magic_index(i) && !is_ying_hu)
				continue;
			int card = _logic.switch_to_card_data(i);
			// 缺一色：起完牌后，玩家手上筒、索、万任缺一门，即可胡牌。（等同小胡自摸）
			int cbCardColor = _logic.get_card_color(card);
			if (cbCardColor > 2)
				continue;
			cbQueYiMenColor[cbCardColor] = 0;
		}
		for (int i = 0; i < weaveCount; i++) {
			int cbCardColor = _logic.get_card_color(weaveItem[i].center_card);
			// 字牌过滤
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
		return count;
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
			if ((seat_index + 1) % getTablePlayerNumber() == i) {
				action = check_chi_ignore_magic(GRR._cards_index[i], card);
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

				if (_playerStatus[i].has_action()) {
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > 0) {
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
			// 只有地胡可以接炮
			boolean hu = true;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				if (j == _cur_banker) {
					continue;
				}
				if (dispatchcardNum(j) > 0) {
					hu = false;
				}
			}
			if (_playerStatus[i].is_chi_hu_round() && hu && seat_index == _cur_banker && dispatchcardNum(_cur_banker) == 1 && _out_card_count == 1) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_WuYuan.HU_CARD_TYPE_JIE_PAO, i);
				if (action != GameConstants.WIK_NULL) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				} else {
					chr.set_empty();
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
		float total_fen = 0.5f;
		// 牌型分
		int pai_xing_fen = get_pai_xing_fen(chr);

		total_fen *= pai_xing_fen * get_fan_bei_shu(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				// GRR._lost_fan_shu[i][seat_index] = total_fen;
			}
		} else {
			// GRR._lost_fan_shu[provide_index][seat_index] = total_fen;
		}

		if (zimo) {
			float s = total_fen;
			if (!chr.opr_and(Constants_WuYuan.CHR_TIAN_HU).is_empty()) {
				s = 16;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			if (!chr.opr_and(Constants_WuYuan.CHR_DI_HU).is_empty()) {
				float s = 16;
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			} else if (!chr.opr_and(Constants_WuYuan.CHR_QIANG_GANG).is_empty()) {
				// 抢杠胡算自摸
				float s = total_fen;
				GRR._game_score[provide_index] -= (getTablePlayerNumber() - 1) * s;
				GRR._game_score[seat_index] += (getTablePlayerNumber() - 1) * s;
			}
			// else {
			// float s = total_fen;
			// GRR._game_score[provide_index] -= s;
			// GRR._game_score[seat_index] += s;
			// GRR._chi_hu_rights[provide_index].opr_or(Constants_HuangShan.CHR_FANG_PAO);
			// }
		}
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
	public int dispatchcardNum(int seat_index) {
		return dispatchcardNum[seat_index];
	}

	/**
	 * 牌型分
	 * 
	 * @param chr
	 * @return
	 */
	private int get_pai_xing_fen(ChiHuRight chr) {
		int fan_shu = 1;
		if (!chr.opr_and(Constants_WuYuan.CHR_HAO_HUA_QI_XIAO_DUI).is_empty())
			fan_shu = 6;
		else if (!chr.opr_and(Constants_WuYuan.CHR_QING_QI_SE_QI_DUI).is_empty())
			fan_shu = 6;
		else if (!chr.opr_and(Constants_WuYuan.CHR_QING_YI_SE_PENG_PENG_HU).is_empty())
			fan_shu = 6;
		else if (!chr.opr_and(Constants_WuYuan.CHR_QI_XING_SHI_SAN_LAN).is_empty())
			fan_shu = 3;
		else if (!chr.opr_and(Constants_WuYuan.CHR_QI_XING_JIU_YAO).is_empty())
			fan_shu = 3;
		else if (!chr.opr_and(Constants_WuYuan.CHR_QING_YI_SE).is_empty())
			fan_shu = 3;
		else if (!chr.opr_and(Constants_WuYuan.CHR_QI_DUI).is_empty())
			fan_shu = 3;
		else if (!chr.opr_and(Constants_WuYuan.CHR_PENG_PENG_HU).is_empty())
			fan_shu = 3;
		else if (!chr.opr_and(Constants_WuYuan.CHR_SHI_YAN_LAN).is_empty())
			fan_shu = 1;
		else if (!chr.opr_and(Constants_WuYuan.CHR_JIU_YAO).is_empty())
			fan_shu = 1;
		else if (!chr.opr_and(Constants_WuYuan.CHR_PI_HU).is_empty())
			fan_shu = 1;
		return fan_shu;
	}

	/**
	 * 获得翻倍倍数
	 * 
	 * @param chr
	 */
	private int get_fan_bei_shu(ChiHuRight chr, int _seat_index) {
		int fan_shu = 1;

		if (!chr.opr_and(Constants_WuYuan.CHR_GANG_SHANG_KAI_HUA).is_empty()) {
			fan_shu *= 2;
		}
		if (!chr.opr_and(Constants_WuYuan.CHR_QIANG_GANG).is_empty()) {
			fan_shu *= 2;
		}
		if (!chr.opr_and(Constants_WuYuan.CHR_WU_BAO).is_empty()) {
			fan_shu *= 2;
		}
		if (!chr.opr_and(Constants_WuYuan.CHR_BAO_DIAO).is_empty()) {
			fan_shu *= 2;
		}
		if (!chr.opr_and(Constants_WuYuan.CHR_ZI_MO).is_empty()) {
			fan_shu *= 4;
		}

		if (fei_bao[_seat_index] > 0) {
			fan_shu *= 1 << fei_bao[_seat_index];
		}

		return fan_shu;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;
			ChiHuRight chr = GRR._chi_hu_rights[player];

			if (chr.is_valid()) {
				if (!chr.opr_and(Constants_WuYuan.CHR_QING_YI_SE_PENG_PENG_HU).is_empty()) {
					result.append(" 清一色碰碰胡");
				} else if (!chr.opr_and(Constants_WuYuan.CHR_QING_YI_SE).is_empty()) {
					result.append(" 清一色");
				} else if (!chr.opr_and(Constants_WuYuan.CHR_PENG_PENG_HU).is_empty()) {
					result.append(" 碰碰胡");
				}
			}
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				boolean tian_hu = false;
				if (!GRR._chi_hu_rights[player].opr_and(Constants_WuYuan.CHR_TIAN_HU).is_empty()
						|| !GRR._chi_hu_rights[player].opr_and(Constants_WuYuan.CHR_DI_HU).is_empty()) {
					tian_hu = true;
				}
				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_WuYuan.CHR_ZI_MO && !tian_hu)
						result.append(" 自摸");

					if (type == Constants_WuYuan.CHR_JIE_PAO)
						result.append(" 接炮");

					if (type == Constants_WuYuan.CHR_PI_HU)
						result.append(" 屁胡");

					if (type == Constants_WuYuan.CHR_TIAN_HU)
						result.append(" 天胡");

					if (type == Constants_WuYuan.CHR_DI_HU)
						result.append(" 地胡");

					if (type == Constants_WuYuan.CHR_GANG_SHANG_KAI_HUA)
						result.append(" 杠开");

					if (type == Constants_WuYuan.CHR_QIANG_GANG && !tian_hu)
						result.append(" 抢杠");

					if (type == Constants_WuYuan.CHR_SI_BAO)
						result.append(" 四宝");

					if (type == Constants_WuYuan.CHR_FEI_BAO)
						result.append(" 飞宝");

					if (type == Constants_WuYuan.CHR_WU_BAO)
						result.append(" 无宝");

					if (type == Constants_WuYuan.CHR_BAO_DIAO)
						result.append(" 宝吊");

					if (type == Constants_WuYuan.CHR_HAO_HUA_QI_XIAO_DUI)
						result.append(" 豪华小七对");

					if (type == Constants_WuYuan.CHR_QING_QI_SE_QI_DUI)
						result.append(" 清一色七对");

					if (type == Constants_WuYuan.CHR_QI_XING_SHI_SAN_LAN)
						result.append(" 七星十三烂");

					if (type == Constants_WuYuan.CHR_QI_XING_JIU_YAO)
						result.append(" 七星九幺");

					if (type == Constants_WuYuan.CHR_QI_DUI)
						result.append(" 七对");

					if (type == Constants_WuYuan.CHR_SHI_YAN_LAN)
						result.append(" 十三烂");

					if (type == Constants_WuYuan.CHR_JIU_YAO)
						result.append(" 九幺");

				} else if (type == Constants_YangZhong.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_WuYuan.CHR_SI_BAO)
					result.append(" 四宝");
			}

			// if (GRR._end_type != GameConstants.Game_End_DRAW) { // 荒庄荒杠
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
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
			if (ming_gang + jie_gang - bu_gang[player] > 0) {
				result.append(" 直杠X" + (ming_gang + jie_gang - bu_gang[player]));
			}
			if (bu_gang[player] > 0) {
				result.append(" 补杠X" + bu_gang[player]);
			}
			// if (fang_gang > 0) {
			// result.append(" 放杠X" + fang_gang);
			// }
			// if () {
			// result.append(" 接杠X" + jie_gang);
			// }
			// }

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

		int max_ting_count = GameConstants.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_WuYuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			} else {
				chr.set_empty();
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		} else if (count > 0 && count < real_max_ting_count) {
			boolean add = true;
			int magic_card = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			for (int i = 0; i < count; i++) {
				if (cards[i] == magic_card) {
					add = false;
					break;
				}
			}
			if (add) {
				ChiHuRight chr_tmp = new ChiHuRight();
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, magic_card, chr_tmp,
						Constants_WuYuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
					float total_fen = 0.5f;
					int pai_xing_fen = get_pai_xing_fen(chr_tmp);
					total_fen *= pai_xing_fen * get_fan_bei_shu(chr_tmp, _cur_banker);
					if (total_fen >= 4) {
						cards[count] = magic_card;
						count++;
					}
				}
			}
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
	 * 癞子牌是不是19或者风牌
	 * 
	 * @return
	 */
	public boolean check_lai_zi() {
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			int card_value = _logic.get_card_value(_logic.switch_to_card_data(i));
			int card_color = _logic.get_card_color(_logic.switch_to_card_data(i));
			if (card_color > 2 || card_value == 1 || card_value == 9) {
				if (_logic.is_magic_index(i)) {
					return true;
				}
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
		if (has_rule(Constants_HuiZhou.GAME_RULE_MAI_2_MA)) {
			nNum = GameConstants.ZHANIAO_2;
		}
		if (has_rule(Constants_HuiZhou.GAME_RULE_MAI_4_MA)) {
			nNum = GameConstants.ZHANIAO_4;
		}
		if (has_rule(Constants_HuiZhou.GAME_RULE_MAI_6_MA)) {
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

		// 计算是否有四宝分加成
		int si_bao_player_index = -1;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (mo_bao_pai[i] == 4) {
				si_bao_player_index = i;
				GRR._chi_hu_rights[si_bao_player_index].opr_or(Constants_WuYuan.CHR_SI_BAO);
				break;
			}
		}
		if (si_bao_player_index > -1) {
			int si_bao_fen = 0;
			if (reason == GameConstants.Game_End_DRAW) {
				si_bao_fen = 40;
			} else {
				si_bao_fen = 24;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (si_bao_player_index == i) {
					continue;
				}
				GRR._game_score[i] -= si_bao_fen;
				GRR._game_score[si_bao_player_index] += si_bao_fen;
			}
		}
		// 计算是否有四宝分加成
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);
		game_end.setShowBirdEffect(hu_pai_player);
		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 杠牌，每个人的分数
			float lGangScore[] = new float[GameConstants.GAME_PLAYER];
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						// _allGRR._gang_score[i].scores[j][k]+=
						// GRR._gang_score[i].scores[j][k];//杠牌，每个人的分数

						// allGangScore[k]+=_allGRR._gang_score[i].scores[j][k];
					}
				}
			}

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 流局不算杠分
				GRR._game_score[i] += lGangScore[i];

				GRR._game_score[i] += GRR._start_hu_score[i];// 起手胡分数

				// 记录
				// _all_start_hu_score[i]+=_start_hu_score[i];//起手胡分数
				_player_result.game_score[i] += GRR._game_score[i];

			}
			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

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
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_NIAO);
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
				game_end.addJettonScore(1 << fei_bao[i]);

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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

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

		return false;
	}

	/**
	 * 惠州麻将牌局开始需处理
	 */
	public void on_game_start_for_wuyuan() {
		joker_card_1 = 0;
		joker_card_index_1 = -1;
		Arrays.fill(fei_bao, 0);
		Arrays.fill(mo_bao_pai, 0);
		Arrays.fill(dispatchcardNum, 0);
		Arrays.fill(bu_gang, 0);
		hu_pai_player = GameConstants.INVALID_SEAT;
	}

	/**
	 * 是否七对
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param cur_card
	 * @return 是否硬胡
	 */
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, boolean is_ying_hu) {

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
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌
		int magic_card_count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];
			if (cbCardCount <= 0) {
				continue;
			}

			if (_logic.is_magic_index(i) && !is_ying_hu) {
				magic_card_count += cbCardCount;
				continue;
			}

			// 单牌统计
			if (cbCardCount == 1) {
				cbReplaceCount++;
			}
			if (cbCardCount == 3) {
				cbReplaceCount++;
				nGenCount++;
			}
			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (cbReplaceCount > magic_card_count) {
			return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			// if (nGenCount >= 2) {
			// // 双豪华七小对
			// return GameConstants.CHR_HUNAN_SHUANG_HAO_HUA_QI_XIAO_DUI;
			// }
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
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
						Constants_WuYuan.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				} else {
					chr.set_empty();
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
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
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
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x37, 0x37, 0x37, 0x37, 0x04, 0x04, 0x03, 0x06, 0x07, 0x07, 0x08, 0x26, 0x27 };
		int[] cards_of_player1 = new int[] { 0x25, 0x37, 0x04, 0x16, 0x13, 0x14, 0x15, 0x16, 0x16, 0x17, 0x17, 0x29, 0x29 };
		int[] cards_of_player2 = new int[] { 0x28, 0x28, 0x28, 0x16, 0x13, 0x14, 0x15, 0x16, 0x16, 0x17, 0x17, 0x29, 0x29 };
		int[] cards_of_player3 = new int[] { 0x11, 0x12, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x16, 0x16, 0x17, 0x17, 0x17 };

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

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(Constants_WuYuan.GAME_RULE_SI_REN)) {
			return 4;
		} else if (has_rule(Constants_WuYuan.GAME_RULE_SAN_REN)) {
			return 3;
		} else if (has_rule(Constants_WuYuan.GAME_RULE_ER_REN)) {
			return 2;
		}
		return 4;
	}

	/**
	 * 宝飞
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean operate_di_fen_bei_shu(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// 刷新手牌
		roomResponse.setType(MsgConstants.RESPONSE_DI_FEN_BEI_SHU);
		roomResponse.setTarget(seat_index);
		for (int i = 0; i < getPlayerCount(); i++) {
			if (fei_bao[i] > 0) {
				roomResponse.addCardData(1 << fei_bao[i]);
			} else {
				roomResponse.addCardData(GameConstants.INVALID_SEAT);
			}
		}
		// GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/**
	 * 
	 * @param cards_index
	 * @param weaveItem
	 * @param weaveCount
	 * @param ying_hu
	 * @return 0 非九幺 1 九幺 2七星九幺
	 */
	public int is_yao_jiu(int cards_index[], WeaveItem weaveItem[], int weaveCount, boolean ying_hu) {
		int cbValue = 0;
		int cl = 0;

		int cbCardIndexTemp[] = Arrays.copyOf(cards_index, cards_index.length);

		int magic_count = _logic.magic_count(cbCardIndexTemp);
		int need_laizi = 0;

		for (int i = 0; i < 7; i++) {
			int index = _logic.switch_to_card_index(0x31) + i;
			if (cbCardIndexTemp[index] != 1) {
				need_laizi++;
			}
		}
		for (int i = 0; i < weaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_CENTER
					|| weaveItem[i].weave_kind == GameConstants.WIK_RIGHT) {
				return 0;
			}
			cbValue = _logic.get_card_value(weaveItem[i].center_card);
			cl = _logic.get_card_color(weaveItem[i].center_card);

			// 风牌过滤
			if (cl > 2) {
				continue;
			}

			// 单牌统计
			if ((cbValue != 1) && (cbValue != 9)) {
				return 0;
			}

		}

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if ((i == 0) || (i == 8) || (i == 9) || (i == 17) || (i == 18) || (i == 26)) {
				continue;
			}

			if (_logic.is_magic_index(i) && !ying_hu) {
				continue;
			}

			if (cards_index[i] > 0) {
				return 0;
			}
		}

		if (ying_hu) {
			magic_count = 0;
		}
		if (magic_count >= need_laizi) {
			return 2;
		} else {
			return 1;
		}
	}

	/**
	 * 宝牌检测
	 * 
	 * @param cards_index
	 */
	public void check_bao_pai(int seaxIndex, int cards_index[]) {
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cards_index[i] > 0 && _logic.is_magic_index(i)) {
				mo_bao_pai[seaxIndex] += cards_index[i];
			}
		}
	}

	// 不是十三烂0 十三烂1 七星十三烂2
	public int isQXShiSanLan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, boolean ying_hu) {
		if (weaveCount != 0) {
			return 0;
		}
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);

		// cbCardIndexTemp[_logic.get_magic_card_index(0)] = 0;

		int need_laizi = 0;

		for (int i = 0; i < 7; i++) {
			int index = _logic.switch_to_card_index(0x31) + i;
			if (cbCardIndexTemp[index] != 1) {
				need_laizi++;
			}
		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0/* ||is_magic_index(j) */) {
					continue;
				}
				if (_logic.is_magic_index(j) && !ying_hu) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return 0;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return 0;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return 0;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}

			if (_logic.is_magic_index(i) && !ying_hu) {
				continue;
			}

			if (cbCardIndexTemp[i] > 1) {
				return 0;
			}
		}
		if (ying_hu) {
			magic_count = 0;
		}
		if (magic_count >= need_laizi) {
			return 2;
		} else {
			return 1;
		}
		// if (magic_count >= need_laizi) {
		// return true;
		// } else {
		// return -1;
		// }
	}

	public int check_chi_ignore_magic(int[] cards_index, int cur_card) {
		// 如果牌非法
		if (_logic.is_valid_card(cur_card) == false)
			return GameConstants.WIK_NULL;

		// 如果是风牌或者王牌
		if (_logic.get_card_color(cur_card) > 2)
			return GameConstants.WIK_NULL;

		// 构造数据
		int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		// 插入扑克
		tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int eat_type = GameConstants.WIK_NULL;

		int first_card_index = 0;

		int cur_card_index = _logic.switch_to_card_index(cur_card);

		int eat_type_check[] = new int[] { GameConstants.WIK_LEFT, GameConstants.WIK_CENTER, GameConstants.WIK_RIGHT };

		// 轮询判断，可以有多种吃法，首先判断左吃，然后判断中吃，最后判断右吃
		for (int i = 0; i < 3; i++) {

			int card_value = _logic.get_card_value(cur_card);

			// 牌值判断，比如左吃时，牌值可以是1,2...7；右吃时，牌值可以是3,4...9
			if ((card_value >= (i + 1)) && (card_value <= 7 + i)) {

				// 吃牌时，第一张牌的实际索引值，按照左吃、中吃、右吃的顺序来
				// 比如左吃时，第一张牌的索引就是当前的牌，中吃的时候就是当前牌的前面一张牌
				// 为什么要用索引呢？因为传进来的数据是索引数据，哈哈哈
				first_card_index = cur_card_index - i;

				// 如果不是王牌的玩法，直接判断，第一张牌，第二牌，和第三张牌存不存在
				if (tmp_cards_index[first_card_index] != 0 && tmp_cards_index[first_card_index + 1] != 0
						&& tmp_cards_index[first_card_index + 2] != 0) {
					// 使用或运算符的原因是，底层判断是否有多个吃的时候，用的位运算
					eat_type |= eat_type_check[i];
				}
			}
		}

		return eat_type;
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
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI
						+ GameConstants.CARD_ESPECIAL_TYPE_TING);
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

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);
		if (!is_mj_type(GameConstants.GAME_TYPE_DT_MJ_HUNAN_CHEN_ZHOU)) {
			// 跑分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
			// 闹庄
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.nao[i] = 0;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(fei_bao, 0);
			operate_di_fen_bei_shu(i);
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
			player_result.addMingGangCount(_player_result.ming_gang_count[i] + bu_gang[i]);
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
}
