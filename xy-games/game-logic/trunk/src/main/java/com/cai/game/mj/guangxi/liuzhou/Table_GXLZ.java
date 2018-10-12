package com.cai.game.mj.guangxi.liuzhou;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_GXLZ;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_GXLZ extends AbstractMJTable {

	private static final long serialVersionUID = -1883134402466366270L;

	private int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
	public int[] card_cannot_eat = new int[getTablePlayerNumber()];
	private static final int EAT_LEFT = 0x01;
	private static final int EAT_RIGHT = 0x02;
	private static final int EAT_CENTER = 0x04;

	// 玩家能接炮胡。有接炮胡的时候，有吃碰，点了吃碰不能重置‘有效胡’状态
	public boolean[] state_of_can_jie_pao = new boolean[getTablePlayerNumber()];

	public Table_GXLZ() {
		super(MJType.GAME_TYPE_GX_LIU_ZHOU);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean basic_hu = true;

		int check_qi_xiao_dui = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(Constants_GXLZ.CHR_XIAO_QI_DUI);
			basic_hu = false;
		}

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se == true) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(Constants_GXLZ.CHR_QING_YI_SE);
			basic_hu = false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, true);

		if (bValue == false && check_qi_xiao_dui == GameConstants.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		for (AnalyseItem analyseItem : analyseItemArray) {
			if (_logic.is_pengpeng_hu(analyseItem)) {
				chiHuRight.opr_or(Constants_GXLZ.CHR_DUI_DUI_HU);
				basic_hu = false;
				break;
			}
		}

		int card_count = _logic.get_card_count_by_index(cards_index);
		if (card_count == 1 && GRR._gang_score[seat_index].an_gang_count == 0) {
			chiHuRight.opr_or(Constants_GXLZ.CHR_QUAN_QIU_REN);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_GXLZ.HU_CARD_TYPE_ZI_MO) { // 自摸
			chiHuRight.opr_or(Constants_GXLZ.CHR_ZI_MO);
		} else if (card_type == Constants_GXLZ.HU_CARD_TYPE_QIANG_GANG) { // 抢杠
			chiHuRight.opr_or(Constants_GXLZ.CHR_QIANG_GANG_HU);
		} else if (card_type == Constants_GXLZ.HU_CARD_TYPE_GANG_KAI) { // 杠上花
			chiHuRight.opr_or(Constants_GXLZ.CHR_GANG_SHANG_HUA);
		} else if (card_type == Constants_GXLZ.HU_CARD_TYPE_GANG_PAO) { // 杠上炮
			chiHuRight.opr_or(Constants_GXLZ.CHR_GANG_SHANG_PAO);
		} else if (card_type == Constants_GXLZ.HU_CARD_TYPE_JIE_PAO) { // 接炮
			chiHuRight.opr_or(Constants_GXLZ.CHR_JIE_PAO);
		} else if (card_type == Constants_GXLZ.HU_CARD_TYPE_DIAN_GANG_GANG_KAI) {
			// TODO 点杠杠开算杠上花，但是需要点杠人包给分。
			chiHuRight.opr_or(Constants_GXLZ.CHR_DIAN_GANG_GANG_KAI);
		}

		if (basic_hu == true) {
			chiHuRight.opr_or(Constants_GXLZ.CHR_JI_BEN_HU); // 基本胡
		}

		if (has_rule(Constants_GXLZ.GAME_RULE_MEN_QING)) { // 勾了门清时，有门清按门清计分，没门清正常算分。包括暗杠的杠上花
			if (!chiHuRight.opr_and(Constants_GXLZ.CHR_ZI_MO).is_empty() || !chiHuRight.opr_and(Constants_GXLZ.CHR_GANG_SHANG_HUA).is_empty()) {
				boolean check_men_qing = is_men_qing(weaveItems, weave_count);
				if (check_men_qing == true) {
					chiHuRight.opr_or(Constants_GXLZ.CHR_MEN_QING); // 门清
				}
			}
		}

		return cbChiHuKind;
	}

	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		// TODO 向其他人发送组合牌数据的时候，暗杠不能发center_card数据
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].weave_kind == GameConstants.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		send_response_to_other(seat_index, roomResponse);

		// TODO 向自己发送手牌数据，自己的数据里，暗杠是能看到的
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		GRR.add_room_response(roomResponse);
		send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			Player player = get_players()[seat_index];
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_DUI_DUI_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxduiduihu, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_QING_YI_SE)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxqingyise, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_XIAO_QI_DUI)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxxiaoqidui, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_QIANG_GANG_HU)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxqiangganghu, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_GANG_SHANG_HUA)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxgangshanghua, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_GANG_SHANG_PAO)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxgangshangpao, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_QUAN_QIU_REN)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxquanqiuren, "", _game_type_index, 0l, getRoom_id());
			}
			if (!(chiHuRight.opr_and(Constants_GXLZ.CHR_MEN_QING)).is_empty()) {
				MongoDBServiceImpl.getInstance().card_log(player, ECardType.gxmenqing, "", _game_type_index, 0l, getRoom_id());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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

			if (playerStatus.isAbandoned())
				continue;

			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_GXLZ.HU_CARD_TYPE_QIANG_GANG, i);

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

	public boolean check_eat(int card, int action, int hand_cards_count, final int[] hand_cards) {
		int[] cards_cant_out = new int[2];
		int can_out_count = 0;

		get_cards_cant_out(card, action, cards_cant_out);

		for (int x = 0; x < 2; x++) {
			for (int c = 0; c < hand_cards_count; c++) {
				if (cards_cant_out[x] == hand_cards[c])
					can_out_count++;
			}
		}

		if (can_out_count + 2 == hand_cards_count)
			return false;
		else
			return true;
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
				if (i == get_banker_next_seat(seat_index)) {
					int eat_type = check_card_can_eat(i, card);

					int tmp_hand_cards_count = _logic.get_card_count_by_index(GRR._cards_index[i]);
					int tmp_hand_cards[] = new int[GameConstants.MAX_COUNT];
					_logic.switch_to_cards_data(GRR._cards_index[i], tmp_hand_cards);

					action = _logic.check_chi(GRR._cards_index[i], card);
					if ((action & GameConstants.WIK_LEFT) != 0 && (eat_type & EAT_LEFT) != 0) {
						boolean tmp_can_eat = check_eat(card, action, tmp_hand_cards_count, tmp_hand_cards);

						if (tmp_can_eat) {
							_playerStatus[i].add_action(GameConstants.WIK_LEFT);
							_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
						}
					}
					if ((action & GameConstants.WIK_CENTER) != 0 && (eat_type & EAT_CENTER) != 0) {
						boolean tmp_can_eat = check_eat(card, action, tmp_hand_cards_count, tmp_hand_cards);

						if (tmp_can_eat) {
							_playerStatus[i].add_action(GameConstants.WIK_CENTER);
							_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
						}
					}
					if ((action & GameConstants.WIK_RIGHT) != 0 && (eat_type & EAT_RIGHT) != 0) {
						boolean tmp_can_eat = check_eat(card, action, tmp_hand_cards_count, tmp_hand_cards);

						if (tmp_can_eat) {
							_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
							_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
						}
					}

					if (_playerStatus[i].has_action()) {
						bAroseAction = true;
					}
				}

				if (card != card_cannot_eat[i]) {
					boolean can_peng_this_card = true;
					int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
					for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
						if (tmp_cards_data[x] == card) {
							can_peng_this_card = false;
							break;
						}
					}
					if (can_peng_this_card) {
						action = _logic.check_peng(GRR._cards_index[i], card);
						if (action != 0) {
							playerStatus.add_action(action);
							playerStatus.add_peng(card, seat_index);
							bAroseAction = true;
						}
					}
				}

				if (GRR._left_card_count > get_niao_card_num()) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];
					int card_type = Constants_GXLZ.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.GANG_TYPE_AN_GANG || type == GameConstants.GANG_TYPE_ADD_GANG
							|| type == GameConstants.GANG_TYPE_JIE_GANG) {
						card_type = Constants_GXLZ.HU_CARD_TYPE_GANG_PAO;
					}
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction)

		{
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	protected int get_big_win_count(ChiHuRight chr) {
		int big_win_count = 0;

		if (!chr.opr_and(Constants_GXLZ.CHR_QING_YI_SE).is_empty()) {
			big_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_DUI_DUI_HU).is_empty()) {
			big_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_XIAO_QI_DUI).is_empty()) {
			big_win_count++;
		}

		return big_win_count;
	}

	protected int get_special_win_count(ChiHuRight chr) {
		int special_win_count = 0;

		if (!chr.opr_and(Constants_GXLZ.CHR_QUAN_QIU_REN).is_empty()) {
			special_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_QIANG_GANG_HU).is_empty()) {
			special_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_GANG_SHANG_PAO).is_empty()) {
			special_win_count++;
		}

		return special_win_count;
	}

	protected int get_zi_mo_special_win_count(ChiHuRight chr) {
		int zi_mo_special_win_count = 0;

		if (!chr.opr_and(Constants_GXLZ.CHR_QUAN_QIU_REN).is_empty()) {
			zi_mo_special_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_GANG_SHANG_HUA).is_empty()) {
			zi_mo_special_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_DIAN_GANG_GANG_KAI).is_empty()) {
			zi_mo_special_win_count++;
		}
		if (!chr.opr_and(Constants_GXLZ.CHR_MEN_QING).is_empty()) {
			zi_mo_special_win_count++;
		}

		return zi_mo_special_win_count;
	}

	protected int check_card_can_eat(int seat_index, int card) {
		int result = EAT_LEFT | EAT_RIGHT | EAT_CENTER;

		if (card_cannot_eat[seat_index] == 0)
			return result;

		int card_value = _logic.get_card_value(card);
		int card_color = _logic.get_card_color(card);
		int card_index = _logic.switch_to_card_index(card);

		if (card == card_cannot_eat[seat_index] || card_color > 2)
			return result & 0;
		else {
			int tmp_card_value = _logic.get_card_value(card_cannot_eat[seat_index]);
			int tmp_card_color = _logic.get_card_color(card_cannot_eat[seat_index]);

			if (card_color == tmp_card_color && card_color < 3) {
				if (card_value - 2 > 0 && card_value - 3 == tmp_card_value) {
					if (GRR._cards_index[seat_index][card_index - 1] >= 1 && GRR._cards_index[seat_index][card_index - 2] >= 1)
						result &= (EAT_LEFT | EAT_CENTER);
				}

				if (card_value + 2 < 10 && card_value + 3 == tmp_card_value) {
					if (GRR._cards_index[seat_index][card_index + 1] >= 1 && GRR._cards_index[seat_index][card_index + 2] >= 1)
						result &= (EAT_RIGHT | EAT_CENTER);
				}
			}
		}

		return result;
	}

	protected void get_cards_cant_out(int card, int action, int[] cards_cant_out) {
		if (cards_cant_out.length != 2)
			return;

		cards_cant_out[0] = card;

		if (GameConstants.WIK_CENTER == action)
			return;

		int card_value = _logic.get_card_value(card);

		if (card_value == 1) {
			cards_cant_out[1] = card + 3;
			return;
		}

		if (card_value == 9) {
			cards_cant_out[1] = card - 3;
			return;
		}

		if (GameConstants.WIK_LEFT == action) {
			if (card_value != 7) {
				cards_cant_out[1] = card + 3;
				return;
			}
		} else if (GameConstants.WIK_RIGHT == action) {
			if (card_value != 3) {
				cards_cant_out[1] = card - 3;
				return;
			}
		}
	}

	protected int get_di_fen(ChiHuRight chr) {
		// TODO 胡牌算分，还是各算各的思想。
		// 自摸时，可以将‘对对胡’、‘清一色’、‘小七对’、‘杠上开花’、‘全求人’这些，理解成2分一个，有多少个牌型就叠加多少个2。比如‘对对胡+全求人+杠上花’=‘2+2+2’每人给6分，如果是点杠杠上花，点杠人包给18分，如果是碰杠或暗杠杠上花，每人给6分。
		// 点炮时，可以将‘对对胡’、‘清一色’、‘小七对’、‘抢杠胡’、‘杠上炮’、‘全求人’这些，理解成3分一个，有多少个牌型就叠加多少个3。比如A点炮给B，胡‘杠上炮+清一色’=‘3+3’，A给B6分。

		int di_fen = 1;
		int big_win_count = get_big_win_count(chr); // 对对胡、清一色、小七对
		int special_win_count = get_special_win_count(chr); // 全求人、抢杠胡、杠上炮
		int zi_mo_special_win_count = get_zi_mo_special_win_count(chr); // 门清、全求人、杠上花、点杠杠上花

		// TODO: 注意基本胡的时候的算分
		if (!chr.opr_and(Constants_GXLZ.CHR_ZI_MO).is_empty() || !chr.opr_and(Constants_GXLZ.CHR_GANG_SHANG_HUA).is_empty()
				|| !chr.opr_and(Constants_GXLZ.CHR_DIAN_GANG_GANG_KAI).is_empty()) { // 自摸、杠上花、点杠杠上花
			if (big_win_count == 0 && zi_mo_special_win_count == 0) {
				di_fen = 1;
			} else {
				di_fen = 2 * big_win_count + 2 * zi_mo_special_win_count;
			}
		} else { // 点炮、抢杠胡、杠上炮
			if (big_win_count == 0 && special_win_count == 0) {
				di_fen = 1;
			} else {
				di_fen = 3 * big_win_count + 3 * special_win_count;
			}
		}

		return di_fen;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	protected int get_niao_card_num() {
		int nNum = 0;

		if (has_rule(Constants_GXLZ.GAME_RULE_YI_MA_ZHONG_TE)) {
			nNum = 1;
		} else {
			if (has_rule(Constants_GXLZ.GAME_RULE_DIAO_YU_2)) {
				nNum = 2;
			} else if (has_rule(Constants_GXLZ.GAME_RULE_DIAO_YU_4)) {
				nNum = 4;
			} else if (has_rule(Constants_GXLZ.GAME_RULE_DIAO_YU_6)) {
				nNum = 6;
			} else if (has_rule(Constants_GXLZ.GAME_RULE_DIAO_YU_8)) {
				nNum = 8;
			} else if (has_rule(Constants_GXLZ.GAME_RULE_DIAO_YU_10)) {
				nNum = 10;
			} else if (has_rule(Constants_GXLZ.GAME_RULE_DIAO_YU_12)) {
				nNum = 12;
			}
		}

		return nNum;
	}

	protected int get_pick_niao_count(int seat_index) {
		int cbPickNum = 0;

		if (has_rule(Constants_GXLZ.GAME_RULE_GEN_ZHUANG_DIAO_YU)) {
			if (seat_index == GRR._banker_player) {
				for (int i = 0; i < GRR.liu_zhou_player_niao_count[seat_index]; i++) {
					if (!_logic.is_valid_card(GRR.liu_zhou_player_cards_data_niao[seat_index][i]))
						return 0;

					int nValue = _logic.get_card_value(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);

					if (nValue == 1 || nValue == 5 || nValue == 9) {
						cbPickNum++;
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], true);
					} else {
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], false);
					}
				}
			} else if (seat_index == get_banker_next_seat(GRR._banker_player)) {
				for (int i = 0; i < GRR.liu_zhou_player_niao_count[seat_index]; i++) {
					if (!_logic.is_valid_card(GRR.liu_zhou_player_cards_data_niao[seat_index][i]))
						return 0;

					int nValue = _logic.get_card_value(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);

					if (nValue == 2 || nValue == 6) {
						cbPickNum++;
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], true);
					} else {
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], false);
					}
				}
			} else if (seat_index == get_banker_pre_seat(GRR._banker_player)) {
				for (int i = 0; i < GRR.liu_zhou_player_niao_count[seat_index]; i++) {
					if (!_logic.is_valid_card(GRR.liu_zhou_player_cards_data_niao[seat_index][i]))
						return 0;

					int nValue = _logic.get_card_value(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);

					if (nValue == 4 || nValue == 8) {
						cbPickNum++;
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], true);
					} else {
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], false);
					}
				}
			} else {
				for (int i = 0; i < GRR.liu_zhou_player_niao_count[seat_index]; i++) {
					if (!_logic.is_valid_card(GRR.liu_zhou_player_cards_data_niao[seat_index][i]))
						return 0;

					int nValue = _logic.get_card_value(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);

					if (nValue == 3 || nValue == 7) {
						cbPickNum++;
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], true);
					} else {
						GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this
								.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i], false);
					}
				}
			}
		} else if (has_rule(Constants_GXLZ.GAME_RULE_YI_MA_ZHONG_TE)) {
			for (int i = 0; i < GRR.liu_zhou_player_niao_count[seat_index]; i++) {
				if (!_logic.is_valid_card(GRR.liu_zhou_player_cards_data_niao[seat_index][i]))
					return 0;

				int nValue = _logic.get_card_value(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);
				int nColor = _logic.get_card_color(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);

				if (nColor == 3) {
					cbPickNum += 5;
				} else {
					cbPickNum += nValue;
				}

				GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i],
						true);
			}
		} else {
			for (int i = 0; i < GRR.liu_zhou_player_niao_count[seat_index]; i++) {
				if (!_logic.is_valid_card(GRR.liu_zhou_player_cards_data_niao[seat_index][i]))
					return 0;

				int nValue = _logic.get_card_value(GRR.liu_zhou_player_cards_data_niao[seat_index][i]);

				if (nValue == 1 || nValue == 5 || nValue == 9) {
					cbPickNum++;
					GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i],
							true);
				} else {
					GRR.liu_zhou_player_cards_data_niao[seat_index][i] = this.set_ding_niao_valid(GRR.liu_zhou_player_cards_data_niao[seat_index][i],
							false);
				}
			}
		}

		return cbPickNum;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT)
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
		return card;
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

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_GXLZ.HU_CARD_TYPE_ZI_MO, seat_index)) {
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

	protected boolean is_men_qing(WeaveItem weaveItem[], int cbWeaveCount) {
		if (cbWeaveCount == 0)
			return true;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (!(weaveItem[i].weave_kind == GameConstants.WIK_GANG && weaveItem[i].public_card == 0)) {
				return false;
			}
		}

		return true;
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
					Constants_GXLZ.HU_CARD_TYPE_ZI_MO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_GXLZ();
		_handler_dispath_card = new HandlerDispatchCard_GXLZ();
		_handler_gang = new HandlerGang_GXLZ();
		_handler_out_card_operate = new HandlerOutCardOperate_GXLZ();

		player_niao_count = new int[GameConstants.GAME_PLAYER];
	}

	@Override
	protected boolean on_game_start() {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
		}

		state_of_can_jie_pao = new boolean[getTablePlayerNumber()];

		card_cannot_eat = new int[getTablePlayerNumber()];
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

				if (GRR._end_type != GameConstants.Game_End_DRAW) { // 荒庄荒杠
				}
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

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

			// 鱼牌设置
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder intArray = Int32ArrayResponse.newBuilder();

				for (int j = 0; j < GRR.liu_zhou_player_niao_count[i]; j++)
					intArray.addItem(GRR.liu_zhou_player_cards_data_niao[i][j]);

				game_end.addPlayerNiaoCards(intArray);
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

	public void process_fang_pao_operate(int seat_index) {
		long effect_indexs[] = new long[1];

		effect_indexs[0] = Constants_GXLZ.CHR_FANG_PAO;

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, effect_indexs, 1, GameConstants.INVALID_SEAT);
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

		int di_fen = get_di_fen(chr);

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
			if (!GRR._chi_hu_rights[seat_index].opr_and(Constants_GXLZ.CHR_DIAN_GANG_GANG_KAI).is_empty()) {
				// TODO
				// 点杠杠上花时(类似于自摸)，先计算点杠人包给三个玩家的自摸牌型分；然后如果其他人和胡牌人有吃三比关系，需要给胡牌人一个3倍的自摸牌型分。
				// 先计算点杠人包给的分

				int[] lose_score = new int[getTablePlayerNumber()];
				lose_score[_provide_player] = lChiHuScore * (getTablePlayerNumber() - 1);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					if (_chi_pai_count[seat_index][i] >= 3 || _chi_pai_count[i][seat_index] >= 3) { // 玩家和胡牌人有吃三比关系，给一个3倍牌型分
						lose_score[i] += lChiHuScore * 3;
					}
				}

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index)
						continue;

					if (has_rule(Constants_GXLZ.GAME_RULE_ZIMO_FENG_DING_2)) {
						if (lose_score[i] > 2)
							lose_score[i] = 2;
					}
					if (has_rule(Constants_GXLZ.GAME_RULE_ZIMO_FENG_DING_4)) {
						if (lose_score[i] > 4)
							lose_score[i] = 4;
					}
					if (has_rule(Constants_GXLZ.GAME_RULE_ZIMO_FENG_DING_6)) {
						if (lose_score[i] > 6)
							lose_score[i] = 6;
					}

					int s = lose_score[i] * (GRR.liu_zhou_player_valid_niao_count[seat_index] + 1);

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				// TODO 自摸时，两玩家之间形成了吃三比关系的，给三倍分；
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					int tmp_score = lChiHuScore;

					if (i == seat_index)
						continue;

					if (_chi_pai_count[seat_index][i] >= 3 || _chi_pai_count[i][seat_index] >= 3) { // 玩家和胡牌人有吃三比关系，多给2个牌型分
						tmp_score *= 3;
					}

					if (has_rule(Constants_GXLZ.GAME_RULE_ZIMO_FENG_DING_2)) {
						if (tmp_score > 2)
							tmp_score = 2;
					}
					if (has_rule(Constants_GXLZ.GAME_RULE_ZIMO_FENG_DING_4)) {
						if (tmp_score > 4)
							tmp_score = 4;
					}
					if (has_rule(Constants_GXLZ.GAME_RULE_ZIMO_FENG_DING_6)) {
						if (tmp_score > 6)
							tmp_score = 6;
					}

					int s = tmp_score * (GRR.liu_zhou_player_valid_niao_count[seat_index] + 1);

					GRR._game_score[i] -= s;
					GRR._game_score[seat_index] += s;
				}
			}
		} else {
			// TODO
			// 点炮时，接炮者吃了点炮者或者点炮者吃了接炮者三比的，給两倍分；点炮时，接炮者吃了非点炮者三比的，非点炮者需要给一个牌型分；
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int tmp_score = lChiHuScore;

				if (i == seat_index)
					continue;

				if (i == provide_index) { // 点炮人和胡牌人有吃三比，多给1个牌型分
					if (_chi_pai_count[seat_index][provide_index] >= 3 || _chi_pai_count[provide_index][seat_index] >= 3) {
						tmp_score *= 2;
					}
				} else {
					if (_chi_pai_count[seat_index][i] < 3 && _chi_pai_count[i][seat_index] < 3) { // 其他人和胡牌人没吃三比，不用给给分；有吃三比，给1个牌型分
						tmp_score = 0;
					}
					if (GRR._win_order[i] == 1) { // 如果两人同时胡牌，吃三比连带关系消失
						tmp_score = 0;
					}
				}

				if (!has_rule(Constants_GXLZ.GAME_RULE_ZIMO_WU_XIAN_FAN)) {
					if (tmp_score > 6)
						tmp_score = 6;
				}

				int s = tmp_score * (GRR.liu_zhou_player_valid_niao_count[seat_index] + 1);

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}

			if (!(chr.opr_and(Constants_GXLZ.CHR_JIE_PAO)).is_empty()) {
				GRR._chi_hu_rights[provide_index].opr_or(Constants_GXLZ.CHR_FANG_PAO);
			}
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	public void set_niao_card_liu_zhou(int out_or_dispatch_index) {
		GRR._show_bird_effect = true;

		GRR._count_niao = get_niao_card_num();

		if (GRR._count_niao > 0) {
			if (GRR._win_order[out_or_dispatch_index] == 1) {
				if (GRR._left_card_count > 0) {
					GRR.liu_zhou_player_niao_count[out_or_dispatch_index] = GRR._count_niao;

					if (GRR._count_niao > GRR._left_card_count)
						GRR.liu_zhou_player_niao_count[out_or_dispatch_index] = GRR._left_card_count;

					int tmp_cards_index[] = new int[GameConstants.MAX_INDEX];
					_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count,
							GRR.liu_zhou_player_niao_count[out_or_dispatch_index], tmp_cards_index);
					_logic.switch_to_cards_data(tmp_cards_index, GRR.liu_zhou_player_cards_data_niao[out_or_dispatch_index]);

					GRR._left_card_count -= GRR.liu_zhou_player_niao_count[out_or_dispatch_index];
				}
			}
		}
	}

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_LAI_ZI_PI_ZI_GANG; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_LAI_ZI_PI_ZI_GANG; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = get_niao_card_num();

		if (GRR._count_niao > 0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

			if (DEBUG_CARDS_MODE)
				GRR._cards_data_niao[0] = 0x09;

			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

			int seat = 0;
			if (has_rule(Constants_GXLZ.GAME_RULE_GEN_ZHUANG_DIAO_YU)) {
				seat = get_seat(nValue, GRR._banker_player);
			} else if (has_rule(Constants_GXLZ.GAME_RULE_YI_MA_ZHONG_TE)) {
				seat = seat_index;
			} else {
				seat = get_seat(nValue, seat_index);
			}

			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}
	}

	public void get_player_niao_count(int player, int provider) {
		if (has_rule(Constants_GXLZ.GAME_RULE_159_DIAO_YU)) { // 159钓鱼
			player_niao_count[player] = GRR._player_niao_count[provider];
		} else if (has_rule(Constants_GXLZ.GAME_RULE_GEN_ZHUANG_DIAO_YU)) { // 跟庄钓鱼
			player_niao_count[player] = GRR._player_niao_count[player];
		} else if (has_rule(Constants_GXLZ.GAME_RULE_YI_MA_ZHONG_TE)) { // 一码中特
			int nValue = _logic.get_card_value(GRR._player_niao_cards[provider][0]);
			int nColor = _logic.get_card_color(GRR._player_niao_cards[provider][0]);

			if (nColor == 3) {
				player_niao_count[player] += 5;
			} else {
				player_niao_count[player] += nValue;
			}
		}
	}

	public void get_player_niao_count_liu_zhou() {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			if (GRR._win_order[i] == 1) {
				GRR.liu_zhou_player_valid_niao_count[i] = get_pick_niao_count(i);
			}
		}
	}

	public void set_niao_valid(int provider) {
		if (has_rule(Constants_GXLZ.GAME_RULE_159_DIAO_YU)) { // 159钓鱼
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (i == provider) {
					for (int j = 0; j < GRR._player_niao_count[i]; j++) {
						GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
					}
				} else {
					for (int j = 0; j < GRR._player_niao_count[i]; j++) {
						GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], false);
					}
				}
			}
		} else if (has_rule(Constants_GXLZ.GAME_RULE_GEN_ZHUANG_DIAO_YU)) { // 跟庄钓鱼
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				if (player_niao_count[i] != 0) {
					for (int j = 0; j < GRR._player_niao_count[i]; j++) {
						GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
					}
				} else {
					for (int j = 0; j < GRR._player_niao_count[i]; j++) {
						GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], false);
					}
				}
			}
		} else if (has_rule(Constants_GXLZ.GAME_RULE_YI_MA_ZHONG_TE)) { // 一码中特
			GRR._player_niao_cards[provider][0] = set_ding_niao_valid(GRR._player_niao_cards[provider][0], true);
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x22, 0x35, 0x35 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x22, 0x35, 0x35 };
		int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x22, 0x35, 0x35 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x22, 0x35, 0x35 };

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

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			boolean has_chi_san_bi = false;

			for (int x = 0; x < getTablePlayerNumber(); x++) {
				if (x == player)
					continue;

				if (_chi_pai_count[player][x] >= 3) {
					has_chi_san_bi = true;
					break;
				}
			}

			if (has_chi_san_bi == true) {
				result.append("吃三比");
			}

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_GXLZ.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_GXLZ.CHR_JIE_PAO) {
						result.append(" 接炮");
					}
					if (type == Constants_GXLZ.CHR_JI_BEN_HU) {
						result.append(" 基本胡");
					}
					if (type == Constants_GXLZ.CHR_DUI_DUI_HU) {
						result.append(" 对对胡");
					}
					if (type == Constants_GXLZ.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_GXLZ.CHR_XIAO_QI_DUI) {
						result.append(" 小七对");
					}
					if (type == Constants_GXLZ.CHR_DIAN_GANG_GANG_KAI) {
						result.append(" 点杠杠上花");
					}
					if (type == Constants_GXLZ.CHR_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_GXLZ.CHR_GANG_SHANG_HUA) {
						result.append(" 杠上开花");
					}
					if (type == Constants_GXLZ.CHR_GANG_SHANG_PAO) {
						result.append(" 杠上炮");
					}
					if (type == Constants_GXLZ.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}
					if (type == Constants_GXLZ.CHR_MEN_QING) {
						result.append(" 门清");
					}
				} else if (type == Constants_GXLZ.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (GRR.liu_zhou_player_valid_niao_count[player] > 0) {
					result.append(" 中鱼X" + GRR.liu_zhou_player_valid_niao_count[player]);
				}
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
			if (ming_gang > 0) {
				result.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			// }

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

}
