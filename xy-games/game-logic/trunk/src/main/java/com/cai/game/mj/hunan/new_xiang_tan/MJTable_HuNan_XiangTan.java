package com.cai.game.mj.hunan.new_xiang_tan;

import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.MJConstants_HuNan_XiangTan;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_HuNan_XiangTan extends AbstractMJTable {

	private static final long serialVersionUID = 4404964413830802705L;

	protected MJHandlerSelectMagicCard_HuNan_XiangTan _handler_select_magic_card;
	protected MJHandlerQiShouHu_HuNan_XiangTan _handler_qishou_hu;
	protected MJHandlerGangXuanMei_HuNan_XiangTan _handler_gang_xuan_mei;

	public MJTable_HuNan_XiangTan(MJType type) {
		super(type);
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		return GameConstants.WIK_NULL;
	}

	public int analyse_chi_hu_card_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index, boolean need_to_multiply) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		boolean need_to_check_258 = true; // 是否需要258将
		boolean is_xiao_hu = true; // 是否为小胡（平胡）
		boolean can_win = false; // 是否能胡牌

		// 这些在能胡牌之后，用来设置CHR，否则选美时CHR会出现不能胡牌时的CHR
		boolean has_qi_xiao_dui = false;
		boolean has_qing_yi_se = false;
		boolean has_men_qing = false;
		boolean has_peng_peng_hu = false;

		int check_qi_xiao_dui = _logic.sg_is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) { // 七小对，不需要258将，能直接胡牌
			need_to_check_258 = false;
			is_xiao_hu = false;
			can_win = true;
			has_qi_xiao_dui = true;

			// cbChiHuKind = GameConstants.WIK_CHI_HU;
			// chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI,
			// need_to_multiply);
		}

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) { // 清一色，不需要258将
			need_to_check_258 = false;
			is_xiao_hu = false;
			has_qing_yi_se = true;

			// cbChiHuKind = GameConstants.WIK_CHI_HU;
			// chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE,
			// need_to_multiply);
		}

		if (weave_count == 0) { // 门清，需要258将
			// 有七小对时，不计算门清，门清的前提是无吃碰杠并且其他牌是刻子或顺子，有一种特殊的七小对会把门清计算进去
			if (has_qi_xiao_dui == false) {
				is_xiao_hu = false;
				has_men_qing = true;
			}

			// cbChiHuKind = GameConstants.WIK_CHI_HU;
			// chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_MEN_QING,
			// need_to_multiply);
		}

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

		if (bValue) { // 如果能胡
			can_win = true;

			boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);

			boolean exist_eat = exist_eat(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);

			if (is_peng_peng_hu && !exist_eat) { // 碰碰胡
				need_to_check_258 = false;
				is_xiao_hu = false;
				has_peng_peng_hu = true;
				// chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU,
				// need_to_multiply);
			}

			if (need_to_check_258) { // 如果需要检查258将，也就是没有七小对、清一色、碰碰胡
				boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
						magic_card_count);

				if (has_258 == false)
					can_win = false;
			}
		}

		if (can_win == false) { // 如果不能胡牌
			// 这里不能胡牌时也不能清空，因为选美胡时，需要叠加多张牌的CHR
			// chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_ZI_MO, need_to_multiply); // 自摸
		} else if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_JIE_PAO, need_to_multiply); // 点炮
		} else if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_GANG_KAI, need_to_multiply); // 杠上开花（选美胡）
		} else if (card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_QIANG_GANG_HU, need_to_multiply); // 抢杠胡（选美炮胡）
		}

		if (is_xiao_hu) { // 平胡
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_PING_HU, need_to_multiply);
		}
		if (has_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI, need_to_multiply);
		}
		if (has_men_qing) { // 门清
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_MEN_QING, need_to_multiply);
		}
		if (has_qing_yi_se) { // 清一色
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE, need_to_multiply);
		}
		if (has_peng_peng_hu) { // 碰碰胡
			chiHuRight.opr_or_xt(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU, need_to_multiply);
		}

		return cbChiHuKind;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS); // 12更新玩家手牌数据
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

		this.send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			if (_logic.is_magic_card(_playerStatus[seat_index]._hu_out_card_ting[i])) {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
			} else {
				roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			}

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				int_array.addItem(card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

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

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO, seat_index, false)) {
				cards[count] = cbCurrentCard;
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
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

	/**
	 * 选美的牌，翻牌人，进行碰杠分析
	 * 
	 * @param cards_index
	 * @param card
	 * @param WeaveItem
	 * @param cbWeaveCount
	 * @param gangCardResult
	 * @return
	 */
	public int analyse_peng_gang(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				if (WeaveItem[i].center_card == card) {
					cbActionMask |= GameConstants.WIK_GANG;

					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
					gangCardResult.isPublic[index] = 1;
					gangCardResult.type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					break;
				}
			}
		}

		return cbActionMask;
	}

	public boolean estimate_xuan_mei_respond(int seat_index, int[] xuan_mei_cards) {
		boolean bAroseAction = false;
		int action = GameConstants.WIK_NULL;
		PlayerStatus playerStatus = null;

		// 清空所有玩家的动作和选美能胡的牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clear_hu_cards_of_xuan_mei();
		}

		// 对所有选美的牌，针对所有玩家进行‘胡’、‘杠’、‘碰’、‘吃’分析
		for (int p = 0; p < this.getTablePlayerNumber(); p++) {
			playerStatus = _playerStatus[p];
			// 对多张牌进行吃胡分析的时候，每个玩家的吃胡只清空一次，这样的话，胡多张牌时一次性就可以叠加计算分数了
			ChiHuRight chr = GRR._chi_hu_rights[p];
			chr.set_empty();

			boolean has_hu_self = false;
			boolean[] has_hu_others = new boolean[getTablePlayerNumber()];

			for (int i = 0; i < xuan_mei_cards.length; i++) {
				int card = xuan_mei_cards[i];

				if (p == seat_index) {
					// 翻牌人对翻出来的牌，可以进行胡、碰杠，不能进行吃、碰、暗杠；
					if (GRR._left_card_count > 0 && !_logic.is_magic_card(card)) {
						// 检测当前杠牌玩家是否有暗杠或碰杠，分析之前需要先加到手牌，分析完之后要从手牌减掉
						GangCardResult m_gangCardResult = new GangCardResult();
						m_gangCardResult.cbCardCount = 0;

						action = analyse_peng_gang(GRR._cards_index[p], card, GRR._weave_items[p], GRR._weave_count[p], m_gangCardResult);

						if (GameConstants.WIK_NULL != action && playerStatus.get_ting_state() == true) { // 碰杠，并且已经是听牌的
							boolean flag = false;
							for (int gCount = 0; gCount < m_gangCardResult.cbCardCount; gCount++) {
								// 删除手牌并放入落地牌之前，保存状态数据信息
								int tmp_card_index = _logic.switch_to_card_index(m_gangCardResult.cbCardData[gCount]);
								int tmp_card_count = GRR._cards_index[p][tmp_card_index];
								int tmp_weave_count = GRR._weave_count[p];

								// 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
								// 杠选美时，自身杠牌玩家，杠牌只要碰杠和暗杠这两种
								GRR._cards_index[p][tmp_card_index] = 0;
								if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[gCount]) {
									GRR._weave_items[p][tmp_weave_count].public_card = 0;
									GRR._weave_items[p][tmp_weave_count].center_card = m_gangCardResult.cbCardData[gCount];
									GRR._weave_items[p][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
									GRR._weave_items[p][tmp_weave_count].provide_player = p;
									GRR._weave_count[p]++;
								}

								boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], p);

								// 还原手牌数据和落地牌数据
								GRR._cards_index[p][tmp_card_index] = tmp_card_count;
								GRR._weave_count[p] = tmp_weave_count;

								// 杠牌之后还是听牌状态，并不需要更新听牌状态，只要出牌时更新就可以
								if (is_ting_state_after_gang) {
									playerStatus.add_gang(m_gangCardResult.cbCardData[gCount], p, m_gangCardResult.isPublic[gCount]);
									flag = true;
								}
							}
							if (flag) { // 如果能杠，当前用户状态加上杠牌动作
								playerStatus.add_action(GameConstants.WIK_GANG);
							}
						}
					}

					if (playerStatus.is_chi_hu_round() && playerStatus.isAbandoned() == false) {
						// if (_logic.is_magic_card(card))
						// card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;

						// 胡牌的牌类型算杠上开花
						action = analyse_chi_hu_card_new(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], card, chr,
								MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI, p, true);

						if (action != GameConstants.WIK_NULL) {
							playerStatus.add_action(GameConstants.WIK_ZI_MO);
							if (has_hu_self == false) {
								playerStatus.add_zi_mo(card, p);
								has_hu_self = true;
							}
							playerStatus.add_hu_cards_of_xuan_mei(card);
							bAroseAction = true;
						}
					}
				} else {
					if (!_logic.is_magic_card(card)) {
						// 非翻牌人对翻出来的牌，可以进行胡、接杠、吃、碰；
						if (p == this.get_banker_next_seat(seat_index)) {
							// 下家判断吃牌
							action = _logic.check_chi_xiang_tan(GRR._cards_index[p], card);
							if ((action & GameConstants.WIK_LEFT) != 0) {
								playerStatus.add_action(GameConstants.WIK_LEFT);
								playerStatus.add_chi(card, GameConstants.WIK_LEFT, seat_index);
							}
							if ((action & GameConstants.WIK_CENTER) != 0) {
								playerStatus.add_action(GameConstants.WIK_CENTER);
								playerStatus.add_chi(card, GameConstants.WIK_CENTER, seat_index);
							}
							if ((action & GameConstants.WIK_RIGHT) != 0) {
								playerStatus.add_action(GameConstants.WIK_RIGHT);
								playerStatus.add_chi(card, GameConstants.WIK_RIGHT, seat_index);
							}
							if (playerStatus.has_action()) {
								bAroseAction = true;
							}
						}

						// 判断碰
						action = _logic.check_peng(GRR._cards_index[p], card);
						if (action != 0) {
							playerStatus.add_action(GameConstants.WIK_PENG);
							playerStatus.add_peng(card, seat_index);
							bAroseAction = true;
						}

						if (GRR._left_card_count > 0) {
							// 判断接杠
							action = _logic.estimate_gang_card_out_card(GRR._cards_index[p], card);
							if (action != GameConstants.WIK_NULL) {
								if (playerStatus.get_ting_state() == true) { // 玩家已经听牌
									// 删除手牌并放入落地牌之前，保存状态数据信息
									int tmp_card_index = _logic.switch_to_card_index(card);
									int tmp_card_count = GRR._cards_index[p][tmp_card_index];
									int tmp_weave_count = GRR._weave_count[p];

									// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
									GRR._cards_index[p][tmp_card_index] = 0;
									GRR._weave_items[p][tmp_weave_count].public_card = 1;
									GRR._weave_items[p][tmp_weave_count].center_card = card;
									GRR._weave_items[p][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
									GRR._weave_items[p][tmp_weave_count].provide_player = seat_index;
									GRR._weave_count[p]++;

									boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], p);

									// 还原手牌数据和落地牌数据
									GRR._cards_index[p][tmp_card_index] = tmp_card_count;
									GRR._weave_count[p] = tmp_weave_count;

									// 杠牌之后还是听牌状态，并不需要在gang
									// handler里更新听牌状态，只要出牌时更新就可以
									if (is_ting_state_after_gang) {
										playerStatus.add_action(GameConstants.WIK_GANG);
										playerStatus.add_gang(card, seat_index, 1);
										bAroseAction = true;
									}
								}
							}
						}

						int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
						int magic_card_count = _logic.get_magic_card_count();

						if (magic_card_count > 2) { // 一般只有两种癞子牌存在
							magic_card_count = 2;
						}

						for (int j = 0; j < magic_card_count; j++) {
							magic_cards_index[j] = _logic.get_magic_card_index(j);
						}

						boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(GRR._cards_index[i],
								_logic.switch_to_card_index(card), magic_cards_index, magic_card_count);

						boolean exist_eat = exist_eat(GRR._weave_items[i], GRR._weave_count[i]);

						is_peng_peng_hu = is_peng_peng_hu && !exist_eat;

						// 碰牌或者吃牌之后，不能接炮，只能自摸；打王牌之后弃胡；注意碰碰胡是可以接炮的
						if (playerStatus.is_chi_hu_round() && playerStatus.isAbandoned() == false) {
							boolean can_hu_this_card = true;

							int[] tmp_cards_data = playerStatus.get_cards_abandoned_hu();

							for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
								if (tmp_cards_data[x] == card) {
									can_hu_this_card = false;
									break;
								}
							}

							if (can_hu_this_card) {
								if (GRR._weave_count[p] == 0 || is_peng_peng_hu) {
									// 吃胡判断
									action = analyse_chi_hu_card_new(GRR._cards_index[p], GRR._weave_items[p], GRR._weave_count[p], card, chr,
											MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU, p, true);

									if (action != GameConstants.WIK_NULL) {
										playerStatus.add_action(GameConstants.WIK_CHI_HU);
										if (has_hu_others[p] == false) {
											playerStatus.add_chi_hu(card, seat_index);
											has_hu_others[p] = true;
										}
										playerStatus.add_hu_cards_of_xuan_mei(card);
										bAroseAction = true;
									}
								}
							}
						}
					}
				}
			}
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

		if (_logic.is_magic_card(card)) // 打出来的鬼牌其他玩家不能操作
			return false;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错
				// TODO: 吃牌时有王牌，未打断点检查
				if (i == this.get_banker_next_seat(seat_index)) { // 如果是下家，可以吃上家打出来的牌
					action = _logic.check_chi_xiang_tan(GRR._cards_index[i], card);
					if ((action & GameConstants.WIK_LEFT) != 0) {
						playerStatus.add_action(GameConstants.WIK_LEFT);
						playerStatus.add_chi(card, GameConstants.WIK_LEFT, seat_index);
					}
					if ((action & GameConstants.WIK_CENTER) != 0) {
						playerStatus.add_action(GameConstants.WIK_CENTER);
						playerStatus.add_chi(card, GameConstants.WIK_CENTER, seat_index);
					}
					if ((action & GameConstants.WIK_RIGHT) != 0) {
						playerStatus.add_action(GameConstants.WIK_RIGHT);
						playerStatus.add_chi(card, GameConstants.WIK_RIGHT, seat_index);
					}
					if (playerStatus.has_action()) {
						bAroseAction = true;
					}
				}

				// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				// 杠牌判断，需要判断是否已听牌，并且杠了之后还是听牌状态
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (playerStatus.get_ting_state() == true) { // 玩家已经听牌
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = _logic.switch_to_card_index(card);
						int tmp_card_count = GRR._cards_index[i][tmp_card_index];
						int tmp_weave_count = GRR._weave_count[i];

						// 删除手牌并加入一个落地牌组合，别人出牌时，杠都是明杠，因为等下分析听牌时要用
						GRR._cards_index[i][tmp_card_index] = 0;
						GRR._weave_items[i][tmp_weave_count].public_card = 1;
						GRR._weave_items[i][tmp_weave_count].center_card = card;
						GRR._weave_items[i][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
						GRR._weave_items[i][tmp_weave_count].provide_player = seat_index;
						++GRR._weave_count[i];

						boolean is_ting_state_after_gang = is_ting_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);

						// 还原手牌数据和落地牌数据
						GRR._cards_index[i][tmp_card_index] = tmp_card_count;
						GRR._weave_count[i] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							playerStatus.add_action(GameConstants.WIK_GANG);
							playerStatus.add_gang(card, seat_index, 1);
							bAroseAction = true;
						}
					}
				}
			}

			int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
			int magic_card_count = _logic.get_magic_card_count();

			if (magic_card_count > 2) { // 一般只有两种癞子牌存在
				magic_card_count = 2;
			}

			for (int j = 0; j < magic_card_count; j++) {
				magic_cards_index[j] = _logic.get_magic_card_index(j);
			}

			boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(GRR._cards_index[i], _logic.switch_to_card_index(card),
					magic_cards_index, magic_card_count);

			boolean exist_eat = exist_eat(GRR._weave_items[i], GRR._weave_count[i]);

			is_peng_peng_hu = is_peng_peng_hu && !exist_eat;

			// 碰牌或者吃牌之后，不能接炮，只能自摸；打王牌之后弃胡；注意碰碰胡是可以接炮的
			if (_playerStatus[i].is_chi_hu_round() && !playerStatus.isAbandoned()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					if (GRR._weave_count[i] == 0 || is_peng_peng_hu) {
						// 吃胡判断
						ChiHuRight chr = GRR._chi_hu_rights[i];
						chr.set_empty();
						int cbWeaveCount = GRR._weave_count[i];
						action = analyse_chi_hu_card_new(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								MJConstants_HuNan_XiangTan.HU_CARD_TYPE_JIE_PAO, i, false);

						// 结果判断
						if (action != 0) {
							_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
							_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
							bAroseAction = true;
						}
					}
				}
			}
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

	public int get_xuan_mei_count() {
		int m_count = 0;

		if (this.has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XUAN_MEI_2)) {
			m_count = MJConstants_HuNan_XiangTan.COUNT_OF_MEI_2;
		} else if (this.has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XUAN_MEI_3)) {
			m_count = MJConstants_HuNan_XiangTan.COUNT_OF_MEI_3;
		} else if (this.has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_XUAN_MEI_4)) {
			m_count = MJConstants_HuNan_XiangTan.COUNT_OF_MEI_4;
		}

		if (m_count > GRR._left_card_count) {
			m_count = GRR._left_card_count;
		}

		return m_count;
	}

	// 是否听牌
	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_new(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO, seat_index, false))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_HuNan_XiangTan();
		_handler_dispath_card = new MJHandlerDispatchCard_HuNan_XiangTan();
		_handler_gang = new MJHandlerGang_HuNan_XiangTan();
		_handler_out_card_operate = new MJHandlerOutCardOperate_HuNan_XiangTan();

		_handler_select_magic_card = new MJHandlerSelectMagicCard_HuNan_XiangTan();
		_handler_qishou_hu = new MJHandlerQiShouHu_HuNan_XiangTan();
		_handler_gang_xuan_mei = new MJHandlerGangXuanMei_HuNan_XiangTan();
	}

	protected void exe_qishou_hu(int banker) {
		this.set_handler(_handler_qishou_hu);
		_handler_qishou_hu.reset_status(banker);
		_handler_qishou_hu.exe(this);
	}

	protected void exe_select_magic_card(int banker) {
		this.set_handler(_handler_select_magic_card);
		_handler_select_magic_card.reset_status(banker);
		_handler_select_magic_card.exe(this);
	}

	protected void exe_gang_xuan_mei(int seat_index, int xuan_mei_count) {
		this.set_handler(_handler_gang_xuan_mei);
		_handler_gang_xuan_mei.reset_status(seat_index, xuan_mei_count);
		_handler_gang_xuan_mei.exe(this);
	}

	@Override
	protected boolean on_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY; // 设置状态

		_logic.clean_magic_cards();

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[this.getTablePlayerNumber()][GameConstants.MAX_COUNT];

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL,
		// GameConstants.DELAY_SEND_CARD_DELAY);

		this.exe_select_magic_card(this.GRR._banker_player);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间
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

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				// 记录
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
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

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			// 设置中鸟数据
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

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				// 胡的牌
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

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
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

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 将MJConstants_HuNan_XiangTan里的胡牌的CHR常量进行存储
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		// 客户端弹出胡牌的效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		// 把摸的牌从手牌删掉，结算的时候不显示这张牌的，自摸胡的时候，需要删除，接炮胡时不用
		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 对手牌进行处理
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
		}

		// 将胡的牌加到手牌
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	public void process_chi_hu_player_operate_xt_xuan_mei(int seat_index, boolean rm) {
		// 将MJConstants_HuNan_XiangTan里的胡牌的CHR常量进行存储
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		// 客户端弹出胡牌的效果
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		// 对手牌进行处理
		int cards[] = new int[GameConstants.MAX_COUNT + 4];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
		}

		int[] hu_cards = this._playerStatus[seat_index].get_hu_cards_of_xuan_mei();

		for (int i = 0; i < hu_cards.length; i++) {
			// 将胡的牌加到手牌
			cards[hand_card_count++] = hu_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_HU;
		}

		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
	}

	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int hu_card_type, boolean is_zi_mo) {
		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;

		countCardType(chr, seat_index);

		wFanShu = this.get_fan_shu(hu_card_type, chr); // 根据牌型和吃胡类型获取番数

		int lChiHuScore = wFanShu * GameConstants.CELL_SCORE;

		// 算基础分
		if (is_zi_mo) { // 自摸
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else { // 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		// 算分
		if (is_zi_mo) { // 自摸
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int s = lChiHuScore;

				s += GRR._count_pick_niao;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else { // 点炮
			int s = lChiHuScore;

			s += GRR._count_pick_niao;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or_xt(MJConstants_HuNan_XiangTan.CHR_FANG_PAO, false); // 放炮
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		int tmp_count = 0;
		Map<Long, Integer> map = null;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;
			map = GRR._chi_hu_rights[player].get_map_for_type_and_count();

			if (GRR._chi_hu_rights[player].is_valid()) {
				for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
					type = GRR._chi_hu_rights[player].type_list[typeIndex];

					if (type == MJConstants_HuNan_XiangTan.CHR_ZI_MO) {
						gameDesc.append(" 自摸");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_JIE_PAO) {
						gameDesc.append(" 接炮");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU) {
						gameDesc.append(" 自摸 起手胡");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_PING_HU) {
						if (map.containsKey((long) MJConstants_HuNan_XiangTan.CHR_PING_HU)) {
							tmp_count = map.get((long) MJConstants_HuNan_XiangTan.CHR_PING_HU);
							gameDesc.append(" 平胡x" + tmp_count);
						} else {
							gameDesc.append(" 平胡");
						}
					}

					if (type == MJConstants_HuNan_XiangTan.CHR_MEN_QING) {
						if (map.containsKey((long) MJConstants_HuNan_XiangTan.CHR_MEN_QING)) {
							tmp_count = map.get((long) MJConstants_HuNan_XiangTan.CHR_MEN_QING);
							gameDesc.append(" 门清x" + tmp_count);
						} else {
							gameDesc.append(" 门清");
						}
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU) {
						if (map.containsKey((long) MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)) {
							tmp_count = map.get((long) MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU);
							gameDesc.append(" 碰碰胡x" + tmp_count);
						} else {
							gameDesc.append(" 碰碰胡");
						}
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI) {
						if (map.containsKey((long) MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)) {
							tmp_count = map.get((long) MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI);
							gameDesc.append(" 七小对x" + tmp_count);
						} else {
							gameDesc.append(" 七小对");
						}
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QING_YI_SE) {
						if (map.containsKey((long) MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)) {
							tmp_count = map.get((long) MJConstants_HuNan_XiangTan.CHR_QING_YI_SE);
							gameDesc.append(" 清一色x" + tmp_count);
						} else {
							gameDesc.append(" 清一色");
						}
					}

					if (type == MJConstants_HuNan_XiangTan.CHR_GANG_KAI) {
						gameDesc.append(" 杠上开花");
					}
					if (type == MJConstants_HuNan_XiangTan.CHR_QIANG_GANG_HU) {
						gameDesc.append(" 抢杠胡");
					}
				}

				if (GRR._count_pick_niao > 0) {
					gameDesc.append(" 中鸟X" + GRR._count_pick_niao);
				}
			} else if (!GRR._chi_hu_rights[player].opr_and(MJConstants_HuNan_XiangTan.CHR_FANG_PAO).is_empty()) {
				gameDesc.append(" 放炮");
			}

			/**
			 * int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			 * 
			 * if (GRR != null) { for (int tmpPlayer = 0; tmpPlayer <
			 * this.getTablePlayerNumber(); tmpPlayer++) { for (int w = 0; w <
			 * GRR._weave_count[tmpPlayer]; w++) { if
			 * (GRR._weave_items[tmpPlayer][w].weave_kind !=
			 * GameConstants.WIK_GANG) { continue; } if (tmpPlayer == player) {
			 * if (GRR._weave_items[tmpPlayer][w].provide_player != tmpPlayer) {
			 * jie_gang++; } else { if
			 * (GRR._weave_items[tmpPlayer][w].public_card == 1) { ming_gang++;
			 * } else { an_gang++; } } } else { if
			 * (GRR._weave_items[tmpPlayer][w].provide_player == player) {
			 * fang_gang++; } } } } }
			 * 
			 * if (an_gang > 0) { gameDesc.append(" 暗杠X" + an_gang); } if
			 * (ming_gang > 0) { gameDesc.append(" 明杠X" + ming_gang); } if
			 * (fang_gang > 0) { gameDesc.append(" 放杠X" + fang_gang); } if
			 * (jie_gang > 0) { gameDesc.append(" 接杠X" + jie_gang); }
			 **/

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	public void runnable_remove_da_dian_card(int seat_index) {
		// 将翻出来的牌从牌桌的正中央移除
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		// TODO 获取听牌数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		// 判断有没有起手胡的，注意一个特殊情况，刚好有两个玩家手上都抓了4个一样的王牌
		this.exe_qishou_hu(seat_index);
	}

	public void runnable_remove_xuan_mei_card(int seat_index) {
		// 将翻出来的牌从牌桌的正中央移除
		this.operate_show_card(seat_index, GameConstants.Show_Card_Center, 0, this._gang_card_data.get_cards(), GameConstants.INVALID_SEAT);

		// 估计这一行不能少，不然将选美的牌加入废牌堆时会出错
		this.operate_out_card(seat_index, this.get_xuan_mei_count(), this._gang_card_data.get_cards(), GameConstants.OUT_CARD_TYPE_MID,
				GameConstants.INVALID_SEAT);

		// 把选美的牌加入到废弃牌堆
		this.exe_add_discard(seat_index, this.get_xuan_mei_count(), this._gang_card_data.get_cards(), false, GameConstants.DELAY_SEND_CARD_DELAY);

		// 发牌给下家
		this.exe_dispatch_card(this.get_banker_next_seat(seat_index), GameConstants.WIK_NULL, 0);
	}

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = this.get_niao_card_num();

		if (GRR._count_niao > 0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			// 从剩余牌堆里顺序取奖码数目的牌
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			GRR._left_card_count -= GRR._count_niao;
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			if (_logic.is_magic_card(GRR._cards_data_niao[i])) { // 如果是鬼牌
				GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat_index]++;
			} else {
				int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
				int seat = (seat_index + (nValue - 1) % 4) % 4;
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
				GRR._player_niao_count[seat]++;
			}
		}

		// 设置鸟牌显示
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
				}
			} else {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					GRR._player_niao_cards[i][j] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);
				}
			}
		}

		// 中鸟个数
		GRR._count_pick_niao = this.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
	}

	private int get_niao_card_num() {
		int nNum = 0;

		if (has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_ZHUA_NIAO_2)) { // 奖2码
			nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_2;
		} else if (has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_ZHUA_NIAO_4)) { // 奖4码
			nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_4;
		} else if (has_rule(MJConstants_HuNan_XiangTan.GAME_RULE_HUNAN_ZHUA_NIAO_6)) { // 奖6码
			nNum = MJConstants_HuNan_XiangTan.COUNT_OF_NIAO_6;
		}

		if (nNum > GRR._left_card_count) {
			nNum = GRR._left_card_count;
		}

		return nNum;
	}

	private int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;

		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) // 如果有非法的牌
				return 0;

			if (_logic.is_magic_card(cards_data[i])) { // 鬼牌算中鸟
				cbPickNum++;
				continue;
			}

			int nValue = _logic.get_card_value(cards_data[i]);

			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}
		}

		return cbPickNum;
	}

	// 获取庄家上家的座位
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

	// 获取庄家下家的座位
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x15, 0x22, 0x27, 0x27, 0x27, 0x27 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x15, 0x22, 0x27, 0x27, 0x27, 0x27 };
		int[] cards_of_player2 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x15, 0x22, 0x27, 0x27, 0x27, 0x27 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x15, 0x22, 0x27, 0x27, 0x27, 0x27 };

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
	protected void countCardType(ChiHuRight chiHuRight, int seat_index) {
		try {
			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU)).is_empty()) { // 起手胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qishouhu, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()) { // 门清
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_menqing, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 碰碰胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_pengpenghu, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 清一色
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qingyise, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qixiaodui, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_GANG_KAI)).is_empty()) { // 选美胡自摸（杠上开花）
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_gangshanghua, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QIANG_GANG_HU)).is_empty()) { // 选美胡接炮（抢杠胡）
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qiangganghu, "", _game_type_index, 0l,
						this.getRoom_id());
			}

			if (!(chiHuRight.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_SHOU_HU)).is_empty()) { // 起手胡
				MongoDBServiceImpl.getInstance().card_log(this.get_players()[seat_index], ECardType.xt_qishou_sihun, "", _game_type_index, 0l,
						this.getRoom_id());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected int get_fan_shu(int hu_card_type, ChiHuRight chr) {
		int wFanShu = 1;

		if (hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QI_SHOU_HU) { // 起手胡
			// 起手胡直接给3分
			wFanShu = 3;
		} else if (hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_ZI_MO || hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_JIE_PAO) { // 自摸胡和点炮胡
			// 自摸和点炮时，只有平胡时底分为1分，有门清时底分为2分，有大胡时底分为3分
			int basic_score = 1;
			boolean has_men_qing = false;
			int big_win_count = 0;

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()) { // 门清
				has_men_qing = true;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 碰碰胡
				big_win_count++;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 清一色
				big_win_count++;
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				big_win_count++;
			}

			if (has_men_qing == false && big_win_count == 0) { // 小胡
				wFanShu = basic_score;
			} else if (has_men_qing == true && big_win_count == 0) { // 只有门清
				wFanShu = (basic_score * 2);
			} else if (big_win_count != 0) { // 有大胡
				if (has_men_qing) { // 有大胡也有门清
					big_win_count++;
				}
				if (big_win_count == 1) {
					wFanShu = (basic_score * 3);
				} else {
					wFanShu = (basic_score * 3) * (2 * (big_win_count - 1));
				}
			}
		} else if (hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_GANG_KAI
				|| hu_card_type == MJConstants_HuNan_XiangTan.HU_CARD_TYPE_QIANG_GANG_HU) { // 选美自摸胡（杠上开花）和选美点炮胡（抢杠胡）
			// 计算规则：3=3、6=3*(2)、12=3*(2+2)、18=3*(2+2+2)，最多会同时有3个大胡
			// 选美胡时底分为3分，所有胡牌类型都为大胡
			int basic_score = 3;
			int big_win_count = 0;
			int ping_hu_count = 0;

			Map<Long, Integer> map = chr.get_map_for_type_and_count();

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PING_HU)).is_empty()) { // 平胡
				ping_hu_count += map.get((long) MJConstants_HuNan_XiangTan.CHR_PING_HU);
			}

			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_MEN_QING)).is_empty()) { // 门清
				big_win_count += map.get((long) MJConstants_HuNan_XiangTan.CHR_MEN_QING);
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU)).is_empty()) { // 碰碰胡
				big_win_count += map.get((long) MJConstants_HuNan_XiangTan.CHR_PENG_PENG_HU);
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QING_YI_SE)).is_empty()) { // 清一色
				big_win_count += map.get((long) MJConstants_HuNan_XiangTan.CHR_QING_YI_SE);
			}
			if (!(chr.opr_and(MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI)).is_empty()) { // 七小对
				big_win_count += map.get((long) MJConstants_HuNan_XiangTan.CHR_QI_XIAO_DUI);
			}

			if (big_win_count == 0) {
				if (ping_hu_count == 0) {
					wFanShu = basic_score;
				} else {
					wFanShu = basic_score * ping_hu_count;
				}
			} else {
				wFanShu = basic_score * (2 * big_win_count);
			}
		}

		return wFanShu;
	}
}
