package com.cai.game.mj.hubei.huangzhou;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_HuangZhou extends AbstractMJTable {
	private static final long serialVersionUID = -2825277964284802327L;

	public HandlerSelectMagic_HuangZhou _handler_select_magic;
	public HandlerLiangLaiZi_HuangZhou _handler_liang_lai_zi;

	public int qi_shou_lai_zi[] = new int[getTablePlayerNumber()];
	public int total_lai_zi[] = new int[getTablePlayerNumber()];
	public boolean can_win_pi_hu[] = new boolean[getTablePlayerNumber()];
	public boolean can_ruan_hu[] = new boolean[getTablePlayerNumber()];
	public boolean can_only_zi_mo[] = new boolean[getTablePlayerNumber()];
	public boolean has_liang_lai_zi[] = new boolean[getTablePlayerNumber()];
	public int da_dian_card;

	public boolean gang_status;

	public Table_HuangZhou() {
		super(MJType.GAME_TYPE_HU_BEI_HUANG_ZHOU);
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

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

		send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean basic_hu = true;
		boolean has_qi_dui = false;

		int check_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;

			if (check_qi_xiao_dui == Constants_HuangZhou.CHR_HAO_HUA_QI_DUI)
				chiHuRight.opr_or(Constants_HuangZhou.CHR_HAO_HUA_QI_DUI);
			else
				chiHuRight.opr_or(Constants_HuangZhou.CHR_QI_DUI);

			basic_hu = false;
			has_qi_dui = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (has_qi_dui)
			can_win = true;

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (is_peng_hu) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat) {
				chiHuRight.opr_or(Constants_HuangZhou.CHR_PENG_PENG_HU);
				basic_hu = false;
			}
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card);

		boolean is_ying_qing_yi_se = is_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) {
			basic_hu = false;
		}

		boolean is_men_qing = is_men_qing(weaveItems, weave_count);
		if (card_type == Constants_HuangZhou.HU_CARD_TYPE_ZI_MO || card_type == Constants_HuangZhou.HU_CARD_TYPE_GANG_KAI) {
			if (is_men_qing) {
				chiHuRight.opr_or(Constants_HuangZhou.CHR_MEN_QIAN_QING);
				basic_hu = false;
			}
		}

		if (basic_hu) {
			if (qi_shou_lai_zi[_seat_index] > 1 && is_men_qing == false) { // 起手2癞子以上，并且开口了
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			} else if (total_lai_zi[_seat_index] > 1 && is_men_qing == false && _player_result.nao[_seat_index] == 0) { // 起手癞子数<=1，中间过程癞子数>1，并且开口了，并且没选择亮癞子
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			} else {
				chiHuRight.opr_or(Constants_HuangZhou.CHR_PI_HU);
			}
		}

		int[] tmp_magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int tmp_magic_card_count = 0;

		boolean is_ying_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), tmp_magic_cards_index,
				tmp_magic_card_count);

		if (is_ying_hu) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_YING_HU);

			if (is_ying_qing_yi_se)
				chiHuRight.opr_or(Constants_HuangZhou.CHR_QING_YI_SE);
		} else {
			int ying_qi_xiao_dui = is_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
			if (ying_qi_xiao_dui != GameConstants.WIK_NULL) {
				if (is_ying_qing_yi_se)
					chiHuRight.opr_or(Constants_HuangZhou.CHR_QING_YI_SE);

				chiHuRight.opr_or(Constants_HuangZhou.CHR_YING_HU);
				is_ying_hu = true;
			}
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);

		if (magic_count > 0 && can_win && can_ruan_hu[_seat_index]) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_RUAN_HU);

			if (is_qing_yi_se)
				chiHuRight.opr_or(Constants_HuangZhou.CHR_QING_YI_SE);
		} else if (is_ying_hu == false) { // 不能胡软胡，同时也胡不了硬胡
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_HuangZhou.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_ZI_MO);
		} else if (card_type == Constants_HuangZhou.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_JIE_PAO);
		} else if (card_type == Constants_HuangZhou.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_QIANG_GANG);
		} else if (card_type == Constants_HuangZhou.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_GANG_KAI);
			chiHuRight.opr_or(Constants_HuangZhou.CHR_ZI_MO);
		}

		return cbChiHuKind;
	}

	public int analyse_chi_hu_card_from_ting(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean basic_hu = true;
		boolean has_qi_dui = false;

		int check_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;

			if (check_qi_xiao_dui == Constants_HuangZhou.CHR_HAO_HUA_QI_DUI)
				chiHuRight.opr_or(Constants_HuangZhou.CHR_HAO_HUA_QI_DUI);
			else
				chiHuRight.opr_or(Constants_HuangZhou.CHR_QI_DUI);

			basic_hu = false;
			has_qi_dui = true;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (has_qi_dui)
			can_win = true;

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean is_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (is_peng_hu) {
			boolean exist_eat = exist_eat(weaveItems, weave_count);
			if (!exist_eat) {
				chiHuRight.opr_or(Constants_HuangZhou.CHR_PENG_PENG_HU);
				basic_hu = false;
			}
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card);

		boolean is_ying_qing_yi_se = is_ying_qing_yi_se(cbCardIndexTemp, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) {
			basic_hu = false;
		}

		boolean is_men_qing = is_men_qing(weaveItems, weave_count);
		if (card_type == Constants_HuangZhou.HU_CARD_TYPE_ZI_MO || card_type == Constants_HuangZhou.HU_CARD_TYPE_GANG_KAI) {
			if (is_men_qing) {
				chiHuRight.opr_or(Constants_HuangZhou.CHR_MEN_QIAN_QING);
				basic_hu = false;
			}
		}

		int tmp_total_lai_zi = total_lai_zi[_seat_index];
		if (_logic.is_magic_card(cur_card)) {
			tmp_total_lai_zi++;
		}

		if (basic_hu) {
			if (qi_shou_lai_zi[_seat_index] > 1 && is_men_qing == false) { // 起手2癞子以上，并且开口了
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			} else if (tmp_total_lai_zi > 1 && is_men_qing == false && _player_result.nao[_seat_index] == 0) { // 起手癞子数<=1，中间过程癞子数>1，并且开口了，并且没选择亮癞子
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			} else {
				chiHuRight.opr_or(Constants_HuangZhou.CHR_PI_HU);
			}
		}

		int[] tmp_magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int tmp_magic_card_count = 0;

		boolean is_ying_hu = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), tmp_magic_cards_index,
				tmp_magic_card_count);

		if (is_ying_hu) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_YING_HU);

			if (is_ying_qing_yi_se)
				chiHuRight.opr_or(Constants_HuangZhou.CHR_QING_YI_SE);
		} else {
			int ying_qi_xiao_dui = is_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
			if (ying_qi_xiao_dui != GameConstants.WIK_NULL) {
				if (is_ying_qing_yi_se)
					chiHuRight.opr_or(Constants_HuangZhou.CHR_QING_YI_SE);

				chiHuRight.opr_or(Constants_HuangZhou.CHR_YING_HU);
				is_ying_hu = true;
			}
		}

		int magic_count = _logic.magic_count(cbCardIndexTemp);

		if (magic_count > 0 && can_win && can_ruan_hu[_seat_index]) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_RUAN_HU);

			if (is_qing_yi_se)
				chiHuRight.opr_or(Constants_HuangZhou.CHR_QING_YI_SE);
		} else if (is_ying_hu == false) { // 不能胡软胡，同时也胡不了硬胡
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_HuangZhou.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_ZI_MO);
		} else if (card_type == Constants_HuangZhou.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_JIE_PAO);
		} else if (card_type == Constants_HuangZhou.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_QIANG_GANG);
		} else if (card_type == Constants_HuangZhou.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_HuangZhou.CHR_GANG_KAI);
			chiHuRight.opr_or(Constants_HuangZhou.CHR_ZI_MO);
		}

		return cbChiHuKind;
	}

	public boolean is_ying_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
			return false;

		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	public boolean exe_liang_lai_zi(int seat_index, int send_card_data, int type) {
		set_handler(_handler_liang_lai_zi);
		_handler_liang_lai_zi.reset_status(seat_index, send_card_data, type);
		_handler_liang_lai_zi.exe(this);
		return true;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_HuangZhou.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

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

			if (GRR._left_card_count > 0) {
				int magic_count = _logic.magic_count(GRR._cards_index[i]);
				int card_count = _logic.get_card_count_by_index(GRR._cards_index[i]);

				if (i == get_banker_next_seat(seat_index)) {
					if (card_count - magic_count > 2) {
						// TODO: 癞子牌不能参与 吃 碰 杠，并且除了癞子牌，还能有其他牌可以打出去
						action = _logic.check_chi_xiang_tan(GRR._cards_index[i], card);

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
				}

				if (card_count - magic_count > 2) { // TODO: 能有非癞子牌可以打
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && can_only_zi_mo[i] == false) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				int card_type = Constants_HuangZhou.HU_CARD_TYPE_JIE_PAO;

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
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

	public boolean exe_select_magic_card(int seat_index) {
		set_handler(_handler_select_magic);
		_handler_select_magic.reset_status(seat_index);
		_handler_select_magic.exe(this);
		return true;
	}

	public boolean is_men_qing(WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount == 0)
			return true;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_PENG
					|| (weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card != 0)
					|| weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER) {
				return false;
			}
		}

		return true;
	}

	@Override
	public boolean exist_eat(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return true;
		}

		return false;
	}

	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		int magic_card_count = _logic.get_magic_card_count();

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (magic_card_count > 0) {
				for (int m = 0; m < magic_card_count; m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			return Constants_HuangZhou.CHR_HAO_HUA_QI_DUI;
		} else {
			return Constants_HuangZhou.CHR_QI_DUI;
		}
	}

	public int is_ying_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			return Constants_HuangZhou.CHR_HAO_HUA_QI_DUI;
		} else {
			return Constants_HuangZhou.CHR_QI_DUI;
		}
	}

	public int get_di_fen() {
		int di_fen = 0;

		if (has_rule(Constants_HuangZhou.GAME_RULE_DI_FEN_1))
			di_fen = 1;
		else if (has_rule(Constants_HuangZhou.GAME_RULE_DI_FEN_2))
			di_fen = 2;
		else if (has_rule(Constants_HuangZhou.GAME_RULE_DI_FEN_5))
			di_fen = 5;
		else if (has_rule(Constants_HuangZhou.GAME_RULE_DI_FEN_10))
			di_fen = 10;

		return di_fen;
	}

	public int get_fan_shu(ChiHuRight chr) {
		int fan_shu = 0;

		if (!chr.opr_and(Constants_HuangZhou.CHR_HAO_HUA_QI_DUI).is_empty()) {
			fan_shu = 3;
		} else if (!chr.opr_and(Constants_HuangZhou.CHR_QI_DUI).is_empty()) {
			fan_shu = 2;
		} else if (!chr.opr_and(Constants_HuangZhou.CHR_PENG_PENG_HU).is_empty()) {
			fan_shu = 2;
		} else if (!chr.opr_and(Constants_HuangZhou.CHR_QING_YI_SE).is_empty()) {
			fan_shu = 2;
		} else if (!chr.opr_and(Constants_HuangZhou.CHR_MEN_QIAN_QING).is_empty()) {
			fan_shu = 1;
		}

		if (!chr.opr_and(Constants_HuangZhou.CHR_YING_HU).is_empty()) {
			fan_shu += 1;
		}
		if (!chr.opr_and(Constants_HuangZhou.CHR_QIANG_GANG).is_empty()) {
			fan_shu += 1;
		}
		if (!chr.opr_and(Constants_HuangZhou.CHR_GANG_KAI).is_empty()) {
			fan_shu += 1;
		}
		if (!chr.opr_and(Constants_HuangZhou.CHR_ZI_MO).is_empty()) {
			fan_shu += 1;
		}

		return fan_shu;
	}

	public int get_gang_fan_shu(int lost_player, int win_player) {
		int gang_fan_shu = 0;
		gang_fan_shu += GRR._gang_score[lost_player].an_gang_count + GRR._gang_score[win_player].an_gang_count;
		gang_fan_shu += GRR._gang_score[lost_player].ming_gang_count + GRR._gang_score[win_player].ming_gang_count;

		return gang_fan_shu;
	}

	public int get_real_fan_shu(int fan_shu) {
		int real_fan_shu = 1;

		if (fan_shu == 0) {
			real_fan_shu = 1;
		} else {
			for (int i = 0; i < fan_shu; i++) {
				real_fan_shu *= 2;
			}
		}

		return real_fan_shu;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (getTablePlayerNumber() + seat - 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	protected int get_seat(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) { // 四人场
			seat = (seat_index + (nValue - 1) % 4) % 4;
		} else { // 三人场，所有胡牌人的对家都是那个空位置
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

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants.MAX_ZI + 3;

		for (int i = 0; i < max_ting_count; i++) {
			if (i >= GameConstants.MAX_ZI && i <= GameConstants.MAX_ZI + 3)
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_from_ting(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
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

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HuangZhou();
		_handler_dispath_card = new HandlerDispatchCard_HuangZhou();
		_handler_gang = new HandlerGang_HuangZhou();
		_handler_out_card_operate = new HandlerOutCardOperate_HuangZhou();
		_handler_select_magic = new HandlerSelectMagic_HuangZhou();
		_handler_liang_lai_zi = new HandlerLiangLaiZi_HuangZhou();
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
	protected boolean on_game_start() {
		_logic.clean_magic_cards();

		// TODO: nao字段用来存储是否亮癞子，0表示没有，1表示亮了癞子
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
		}

		qi_shou_lai_zi = new int[getTablePlayerNumber()];
		total_lai_zi = new int[getTablePlayerNumber()];
		can_win_pi_hu = new boolean[getTablePlayerNumber()];
		can_ruan_hu = new boolean[getTablePlayerNumber()];
		can_only_zi_mo = new boolean[getTablePlayerNumber()];
		has_liang_lai_zi = new boolean[getTablePlayerNumber()];
		da_dian_card = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			can_ruan_hu[i] = true;
		}

		gang_status = false;

		_game_status = GameConstants.GS_MJ_PLAY;

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

		exe_select_magic_card(GRR._banker_player);

		// 检测起手癞子数
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			qi_shou_lai_zi[i] = _logic.magic_count(GRR._cards_index[i]);
			total_lai_zi[i] = qi_shou_lai_zi[i];
		}

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
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

			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				// for (int k = 0; k < getTablePlayerNumber(); k++) {
				// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				// }
				// }

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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

			set_result_describe();

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
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

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
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i]))
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int di_fen = get_di_fen();
		int max_fen = di_fen * 10;
		int fan_shu = get_fan_shu(chr);

		countCardType(chr, seat_index);

		int lChiHuScore = di_fen * GameConstants.CELL_SCORE;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int gang_fan_shu = get_gang_fan_shu(i, seat_index);

				int real_fan_shu = get_real_fan_shu(fan_shu + gang_fan_shu);

				int s = lChiHuScore * real_fan_shu;

				if (s > max_fen)
					s = max_fen;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuangZhou.CHR_QIANG_GANG).is_empty()) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int gang_fan_shu = get_gang_fan_shu(i, seat_index);

					int real_fan_shu = get_real_fan_shu(fan_shu + gang_fan_shu);

					int s = lChiHuScore * real_fan_shu;

					if (s > max_fen)
						s = max_fen;

					GRR._game_score[provide_index] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					int gang_fan_shu = get_gang_fan_shu(i, seat_index);

					int real_fan_shu = get_real_fan_shu(fan_shu + gang_fan_shu);

					if (i == provide_index)
						real_fan_shu = get_real_fan_shu(fan_shu + gang_fan_shu + 1);

					int s = lChiHuScore * real_fan_shu;

					if (s > max_fen)
						s = max_fen;

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}

				if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_HuangZhou.CHR_JIE_PAO).is_empty())
					GRR._chi_hu_rights[provide_index].opr_or(Constants_HuangZhou.CHR_FANG_PAO);
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean is_ying_hu = false;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_HuangZhou.CHR_ZI_MO)
						result.append(" 自摸");
					if (type == Constants_HuangZhou.CHR_JIE_PAO)
						result.append(" 接炮");
					if (type == Constants_HuangZhou.CHR_QIANG_GANG)
						result.append(" 抢杠胡");
					if (type == Constants_HuangZhou.CHR_GANG_KAI)
						result.append(" 杠上花");
					if (type == Constants_HuangZhou.CHR_PENG_PENG_HU)
						result.append(" 碰碰胡");
					if (type == Constants_HuangZhou.CHR_QING_YI_SE)
						result.append(" 清一色");
					if (type == Constants_HuangZhou.CHR_QI_DUI)
						result.append(" 七对");
					if (type == Constants_HuangZhou.CHR_HAO_HUA_QI_DUI)
						result.append(" 豪华七对");
					if (type == Constants_HuangZhou.CHR_MEN_QIAN_QING)
						result.append(" 门前清");
					if (type == Constants_HuangZhou.CHR_YING_HU) {
						result.append(" 硬胡");
						is_ying_hu = true;
					}
				} else if (type == Constants_HuangZhou.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			if (GRR._chi_hu_rights[player].is_valid() && is_ying_hu == false)
				result.append(" 软胡");

			if (GRR._gang_score[player].an_gang_count > 0) {
				result.append(" 暗杠X" + GRR._gang_score[player].an_gang_count);
			}
			if (GRR._gang_score[player].ming_gang_count > 0) {
				result.append(" 明杠X" + GRR._gang_score[player].ming_gang_count);
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x03, 0x04, 0x04, 0x06, 0x08, 0x08, 0x09, 0x11, 0x11, 0x17, 0x14, 0x14 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x03, 0x04, 0x04, 0x06, 0x08, 0x08, 0x09, 0x11, 0x11, 0x17, 0x14, 0x14 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x03, 0x04, 0x04, 0x06, 0x08, 0x08, 0x09, 0x11, 0x11, 0x17, 0x14, 0x14 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x03, 0x04, 0x04, 0x06, 0x08, 0x08, 0x09, 0x11, 0x11, 0x17, 0x14, 0x14 };

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
