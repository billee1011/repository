package com.cai.game.mj.handler.shangqiu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_SQ;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.shangQiu.ShangQiuRsp.OtherResponse;

public class MJTable_ShangQiu extends AbstractMJTable {
	private static final long serialVersionUID = -4303498623861049138L;

	public MJHandlerPaoQiang_ShangQiu handlerPaoQiang_ShangQiu;

	public boolean anGangSuoSi; // 暗杆本局锁死

	public int kai_gang_count; // 开杠次数

	public int bu_hua_count; // 补花次数

	public MJHandlerOutCardBaoTing_ShangQiu baoTing_ShangQiu;

	public MJTable_ShangQiu() {
		super(MJType.GAME_TYPE_MJ_SHANG_QIU);
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x06, 0x06, 0x06, 0x36, 0x36, 0x36, 0x36, 0x27, 0x17, 0x18, 0x35, 0x35, 0x35 };
		int[] cards_of_player1 = new int[] { 0x01, 0x02, 0x03, 0x36, 0x36, 0x36, 0x36, 0x18, 0x19, 0x19, 0x19, 0x23, 0x24 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x36, 0x36, 0x36, 0x36, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24 };
		int[] cards_of_player2 = new int[] { 0x01, 0x02, 0x03, 0x36, 0x36, 0x36, 0x36, 0x19, 0x19, 0x23, 0x23, 0x23, 0x24 };

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

	/**
	 * 胡牌检测
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card(int seat_index, int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card, ChiHuRight chiHuRight,
			int card_type) {
		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 暗杆锁死不能点炮
		if (!anGangSuoSi && card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray, true);

		boolean ka_bian = false;
		// if (_playerStatus[seat_index]._hu_card_count == 1) {// 1.只胡一张；卡边吊：两嘴
		for (int i = 0; i < analyseItemArray.size(); i++) {
			// 变量定义
			AnalyseItem pAnalyseItem = analyseItemArray.get(i);
			for (int j = 0; j < 4; j++) {
				if (pAnalyseItem.cbWeaveKind[j] != GameConstants.WIK_LEFT) {
					continue;
				}
				if (pAnalyseItem.cbCenterCard[j] == (cur_card - 1)) {
					// 如果是中间的牌
					chiHuRight.opr_or(GameConstants.CHR_HENAN_KA_ZHANG);
					ka_bian = true;
					break;
				} else {
					// 边张
					int cbCardValue = _logic.get_card_value(pAnalyseItem.cbCenterCard[j]);
					if ((cbCardValue == 1) && (pAnalyseItem.cbCenterCard[j] == (cur_card - 2))) {
						chiHuRight.opr_or(GameConstants.CHR_HENAN_BIAN_ZHANG);
						ka_bian = true;
						break;
					} else if ((cbCardValue == 7) && (pAnalyseItem.cbCenterCard[j] == cur_card)) {
						chiHuRight.opr_or(GameConstants.CHR_HENAN_BIAN_ZHANG);
						ka_bian = true;
						break;
					}
				}

				if (ka_bian == true)
					break;
			}
		}

		if (ka_bian == false && _logic.is_dan_diao(cards_index, cur_card)) {
			// 单吊
			chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
		}
		// }

		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_MEN_QING)) {
			// 门清
			if (_logic.is_men_qing_henan_sq(cards_index, weaveItems, weaveCount) == GameConstants.CHR_HENAN_XY_MENQING) {
				chiHuRight.opr_or(GameConstants.CHR_HUNAN_MEN_QING);
			}
		}

		// 胡牌算缺门
		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_QING_YI_SE) && _logic.is_qing_yi_se(cards_index, weaveItems, weaveCount, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE);
		} else if (has_rule(GameConstants_SQ.GAME_RULE_SQ_JUE_MEN)) {
			int colorCount = this._logic.get_se_count(cbCardIndexTemp, weaveItems, weaveCount);
			chiHuRight.duanmen_count = 3 - colorCount;
		}

		// 暗卡
		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_AN_KA)) {
			// 手牌存在刻子为暗刻
			for (int i = 0; i < cards_index.length; i++) {
				if (cards_index[i] == 3) {
					chiHuRight.an_ka_count++;
				}
			}
		}
		// 可胡七对
		long qxd = GameConstants.WIK_NULL;
		if (!bValue) {
			if (has_rule(GameConstants_SQ.GAME_RULE_SQ_KE_HU_QI_DUI)) {
				qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weaveCount, cur_card);
				if (qxd != GameConstants.WIK_NULL) {
					cbChiHuKind = GameConstants.WIK_CHI_HU;
					chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
						chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					} else {
						chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					}
					return cbChiHuKind;
				}
			}
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		return cbChiHuKind;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		return 0;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return handlerPaoQiang_ShangQiu.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	/**
	 * 麻将出牌检测
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
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
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			// 碰牌判断
			if (playerStatus.is_bao_ting() == false) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants_SQ.GAME_RULE_SQ_LIANG_SI_DA_YI) && check_peng_after(GRR._cards_index[i], card, i)) {
					action = GameConstants.WIK_NULL;
				}
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				// 有杠而且杠完不换章
				if (action != 0) {
					if ((playerStatus.is_bao_ting() == false)) {
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

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round() && !has_rule(GameConstants_SQ.GAME_RULE_SQ_YING_KOU) && !_playerStatus[i].is_ying_kou) {
				// 报听做成可选
				if (has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING) && _playerStatus[i].is_bao_ting() == false) {
					continue;
				}
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(i, GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					// 硬扣
					_playerStatus[i].add_action(GameConstants.WIK_YING_KUO);
					_playerStatus[i].add_ying_kou(card, seat_index);// 吃胡的组合

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
	 * 检测碰牌后是否不能出牌
	 * 
	 * @return
	 */
	public boolean check_peng_after(int[] cards_index, int cur_card, int seat_index) {
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 减去扑克先减去碰的牌
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)] -= 2;
		}

		int card_index[] = new int[] { -1, -1, -1, -1 };

		// 判断剩余牌是不是全是亮牌
		// for (int i : cbCardIndexTemp) {
		for (int i = 0; i < cbCardIndexTemp.length; i++) {
			int card_count = cbCardIndexTemp[i];
			if (card_count == 0) {
				continue;
			}
			// 特殊情况
			if (i == _logic.switch_to_card_index(cur_card)) {
				int[] liang_card = GRR.get_player_liang_cards(seat_index);
				int count = 0;
				for (int j : liang_card) {
					if (cur_card == j) {
						count++;
					}
				}

				if (count <= 2) {
					continue;
				}
			}

			for (int j = 0; j < card_count; j++) {
				if (!GRR.is_liang_pai(_logic.switch_to_card_data(i), seat_index, card_index)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 检查杠牌后是否换章
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	public boolean check_gang_huan_zhang(int seat_index, int card) {
		// 不能换章，需要检测是否改变了听牌
		int gang_card_index = _logic.switch_to_card_index(card);
		int gang_card_count = GRR._cards_index[seat_index][gang_card_index];
		// 假如杠了
		GRR._cards_index[seat_index][gang_card_index] = 0;

		int hu_cards[] = new int[GameConstants.MAX_INDEX];

		// 检查听牌
		int hu_card_count = 0;
		hu_card_count = get_ting_card(seat_index, hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index], GRR._weave_count[seat_index]);

		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;

		if (hu_card_count != _playerStatus[seat_index]._hu_card_count) {
			return true;
		} else {
			for (int j = 0; j < hu_card_count; j++) {
				int real_card = get_real_card(_playerStatus[seat_index]._hu_cards[j]);
				if (real_card != hu_cards[j]) {
					return true;
				}
			}
		}

		return false;
	}

	public int get_ting_card(int seat_index, int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		int cbCurrentCard;
		int count = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX - GameConstants.CARD_HUA_COUNT; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(seat_index, cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		return count;
	}

	/**
	 * 检查这个杠有没有胡
	 * 
	 * @param seat_index
	 * @param card
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
			if (has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING) && _playerStatus[i].is_bao_ting() == false) {
				continue;
			}

			if (_playerStatus[i].is_ying_kou)
				continue;

			playerStatus = _playerStatus[i];
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(i, GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index; // 谁打的牌
			_provide_card = card; // 打的什么牌
			_resume_player = _current_player; // 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
		}

		return bAroseAction;
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
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
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
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index))
				return true;
		}
		return false;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_ShangQiu();
		_handler_dispath_card = new MJHandlerDispatchCard_ShangQiu();
		_handler_gang = new MJHandlerGang_ShangQiu();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ShangQiu();
		baoTing_ShangQiu = new MJHandlerOutCardBaoTing_ShangQiu();
		handlerPaoQiang_ShangQiu = new MJHandlerPaoQiang_ShangQiu();
	}

	@Override
	protected boolean on_game_start() {
		if (!has_rule(GameConstants_SQ.GAME_RULE_SQ_XIA_PAO)) {
			return real_game_start();
		} else {
			GRR._banker_player = _current_player = GameConstants.INVALID_SEAT;
			this.set_handler(this.handlerPaoQiang_ShangQiu);
			this.handlerPaoQiang_ShangQiu.exe(this);
			return false;
		}
	}

	@Override
	protected void init_shuffle() {
		List<Integer> list = new ArrayList<Integer>();
		int card_count = GameConstants_SQ.CARD_DATA_DEFAULT.length;
		for (int i = 0; i < GameConstants_SQ.CARD_DATA_DEFAULT.length; i++) {
			list.add(GameConstants_SQ.CARD_DATA_DEFAULT[i]);
		}

		if (!has_rule(GameConstants_SQ.GAME_RULE_SQ_BU_DAI_FENG)) {
			card_count += GameConstants_SQ.CARD_DATA_FEN.length;

			for (int i = 0; i < GameConstants_SQ.CARD_DATA_FEN.length; i++) {
				list.add(GameConstants_SQ.CARD_DATA_FEN[i]);
			}
		}

		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_YOU_HUA_WU_SHU)) {
			card_count += GameConstants_SQ.CARD_DATA_HUA.length;
			for (int i = 0; i < GameConstants_SQ.CARD_DATA_HUA.length; i++) {
				list.add(GameConstants_SQ.CARD_DATA_HUA[i]);
			}
		}

		// 初始化值
		_logic.clean_hua_index();
		// 添加花牌的index
		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_YOU_HUA_WU_SHU)) {
			int[] card = GameConstants_SQ.CARD_DATA_HUA;
			for (int i : card) {
				_logic.add_hua_card_index(_logic.switch_to_card_index(i));
			}
		} else if (has_rule(GameConstants_SQ.GAME_RULE_SQ_YOU_SHU_WU_HUA)) {
			_logic.add_hua_card_index(_logic.switch_to_card_index(0x36));
		}

		int cards[] = new int[card_count];
		for (int i = 0; i < list.size(); i++) {
			cards[i] = list.get(i);
		}

		_repertory_card = new int[card_count];
		shuffle(_repertory_card, cards);
	};

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_THREE)) {
			return GameConstants_SQ.GAME_PLAYER - 1;
		}
		return GameConstants_SQ.GAME_PLAYER;
	}

	public boolean real_game_start() {
		GRR.init_param_sq(getTablePlayerNumber());
		anGangSuoSi = true;
		kai_gang_count = 0;
		bu_hua_count = 0;

		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 随机选择亮牌
			if (has_rule(GameConstants_SQ.GAME_RULE_SQ_LIANG_SI_DA_YI)) {
				int[] liang_cards = RandomUtil.generateRandomNumberArrayFromExistingArray(hand_cards[i], GameConstants.GAME_LIANG_ZHANG_MAX);
				for (int j = 0; j < liang_cards.length; j++) {
					GRR.add_liang_pai(liang_cards[j], i);
				}
				this.operate_show_card_other(i, 1);
			}

			// 只发自己的牌
			gameStartResponse.clearCardData();
			int index_card = 0;
			for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
				// 处理亮张的牌
				int real_card = hand_cards[i][j];
				for (int k = index_card; k < GameConstants.GAME_LIANG_ZHANG_MAX; k++) {
					if (real_card == GRR.get_player_liang_card(i, k)) {
						real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
						index_card++;
					}
				}
				gameStartResponse.addCardData(real_card);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			if (_cur_round == 1) {
				// shuffle_players();
				load_player_info_data(roomResponse);
			}

			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
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
		///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// 检测听牌
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = this.get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		this.exe_dispatch_card(this._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		return true;
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this.baoTing_ShangQiu);
		this.baoTing_ShangQiu.reset_status(seat_index, card, type);
		this._handler.exe(this);

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
		// 查牌数据
		this.setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
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

			float lGangScore[] = new float[this.getTablePlayerNumber()];

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {

				if (GRR._end_type != GameConstants.Game_End_NORMAL) { // 荒庄荒杠
					continue;
				}

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < this.getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			this.load_player_info_data(roomResponse);

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
					// 客户端特殊处理的牌值
					/*
					 * for(int k = 0; k <
					 * GRR._weave_items[i][j].client_special_count; k++){
					 * weaveItem_item.addClientSpecialCard(GRR._weave_items[i][j
					 * ].client_special_card[k]); }
					 */
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

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;

		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 刷新花牌和亮牌
	 */
	public boolean operate_show_card_other(int seat_index, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_OTHER_CARD);
		roomResponse.setTarget(seat_index);

		OtherResponse.Builder otherBuilder = OtherResponse.newBuilder();
		// 亮牌
		if (type == 1 || type == 3) {
			Int32ArrayResponse.Builder liang_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < GRR.get_liang_card_count_show(seat_index); i++) {
				liang_card.addItem(GRR.get_player_liang_card_show(seat_index, i));
			}
			otherBuilder.setLiangZhang(liang_card);
		}

		// 花牌
		if (type == 2 || type == 3) {
			Int32ArrayResponse.Builder hua_cards = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < GRR._hua_pai_card[seat_index].length; i++) {
				if (GRR._hua_pai_card[seat_index][i] == 0) {
					continue;
				}
				hua_cards.addItem(GRR._hua_pai_card[seat_index][i]);
			}
			otherBuilder.setHuaCard(hua_cards);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(otherBuilder));

		GRR.add_room_response(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(final int seat_index, final int type, final int card, int delay) {
		if (delay > 0) {
			MJTable_ShangQiu mjTable_ShangQiu = this;
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					set_handler(_handler_dispath_card);
					_handler_dispath_card.reset_status(seat_index, type, card);
					_handler.exe(mjTable_ShangQiu);
				}
			}, delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this.set_handler(this._handler_dispath_card);
			this._handler_dispath_card.reset_status(seat_index, type, card);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 0;// 番数

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

		float lChiHuScore = wFanShu;

		lChiHuScore = get_hu_fen(chr, seat_index, lChiHuScore);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			// 自摸加成
			if (has_rule(GameConstants_SQ.GAME_RULE_SQ_ZIMO_JIA_CHENG)) {
				lChiHuScore += 1;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;
				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			float s = lChiHuScore;
			// 跑
			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	public float get_hu_fen(ChiHuRight chr, int seat_index, float lChiHuScore) {
		lChiHuScore += chr.hua_count;

		lChiHuScore += 1;

		// 清一色
		if (!(chr.opr_and(GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE)).is_empty()) {
			lChiHuScore += 2;
		} else if (has_rule(GameConstants_SQ.GAME_RULE_SQ_JUE_MEN) && GRR._chi_hu_rights[seat_index].duanmen_count > 0) {// 缺门
			lChiHuScore += 1;
		}

		// 暗卡
		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_AN_KA)) {
			lChiHuScore += chr.an_ka_count;
		}

		if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
			lChiHuScore += 1;
		}

		if (!(chr.opr_and(GameConstants.CHR_HENAN_KA_ZHANG)).is_empty() || !(chr.opr_and(GameConstants.CHR_HENAN_DAN_DIAO)).is_empty()) {
			lChiHuScore += 1;
		} else if (!(chr.opr_and(GameConstants.CHR_HENAN_BIAN_ZHANG)).is_empty()) {
			lChiHuScore += 2;
		}

		if (!(chr.opr_and(GameConstants.CHR_HUNAN_MEN_QING)).is_empty()) {
			lChiHuScore += 1;
		}

		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_YING_KOU) || _playerStatus[seat_index].is_ying_kou) {
			lChiHuScore += 1;
		}
		if (has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING)) {
			lChiHuScore += 1;
		}

		return lChiHuScore;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";
			int countAnKa = 0;
			int countYinko = 0;
			int countJueMen = 0;
			int countBaoTing = 0;
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
						if (has_rule(GameConstants_SQ.GAME_RULE_SQ_ZIMO_JIA_CHENG)) {
							des += " 自摸加成";
						}
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}

					if (type == GameConstants.CHR_HENAN_DAN_DIAO) {
						des += " 掐张";
					} else if (type == GameConstants.CHR_HENAN_KA_ZHANG) {
						des += " 掐张";
					} else if (type == GameConstants.CHR_HENAN_BIAN_ZHANG) {
						des += " 偏次";
					}

					if (countAnKa == 0 && GRR._chi_hu_rights[i].an_ka_count > 0) {
						des += " 暗卡X" + GRR._chi_hu_rights[i].an_ka_count;
						countAnKa++;
					}

					if (type == GameConstants.CHR_HUNAN_MEN_QING) {
						des += " 门清";
					}

					if (type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对";
					}

					if (countBaoTing == 0 && has_rule(GameConstants_SQ.GAME_RULE_SQ_BAO_TING)) {
						des += " 报听";
						countBaoTing++;
					}

					if (countJueMen == 0 && GRR._chi_hu_rights[i].duanmen_count > 0) {
						des += " 绝门";
						countJueMen++;
					}

					if (type == GameConstants.CHR_HENAN_ZHOU_KOU_QING_YI_SE) {
						des += " 清一色";
					}
					if (countYinko == 0 && (has_rule(GameConstants_SQ.GAME_RULE_SQ_YING_KOU) || _playerStatus[i].is_ying_kou)) {
						des += " 硬扣";
						countYinko++;
					}
					if (type == GameConstants.CHR_HUNAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}

			if (GRR._chi_hu_rights[i].hua_count > 0) {
				des += " 花牌X" + GRR._chi_hu_rights[i].hua_count;
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {// 自己
							// 接杠
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {// 明杠
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
							// 放杠
							if (GRR._weave_items[p][w].provide_player == i) {
								fang_gang++;
							}
						}
					}
				}
			}

			if (an_gang > 0) {
				des += " 暗杠X" + an_gang;
			}
			if (ming_gang > 0) {
				des += " 明杠X" + ming_gang;
			}
			if (fang_gang > 0) {
				des += " 放杠X" + fang_gang;
			}
			if (jie_gang > 0) {
				des += " 接杠X" + jie_gang;
			}

			GRR._result_des[i] = des;
		}
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[], int send_card) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		if (weave_count > 0) {
			this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		}

		this.send_response_to_other(seat_index, roomResponse);

		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD };
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = cards[i];
			for (int k = 0; k < GRR.get_liang_card_count(seat_index); k++) {
				if (index_card[k] == GameConstants.INVALID_CARD && real_card == GRR.get_player_liang_card(seat_index, k)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					index_card[k] = GRR.get_player_liang_card(seat_index, k);
				}
			}
			roomResponse.addCardData(real_card);
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

		if (weave_count > 0) {
			this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		}

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌
		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD };
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = cards[i];
			for (int k = 0; k < GRR.get_liang_card_count(seat_index); k++) {

				int liang_card = GRR.get_player_liang_card(seat_index, k);
				if (real_card > GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING && index_card[k] == GameConstants.INVALID_CARD
						&& real_card == liang_card + GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING) {
					index_card[k] = liang_card;
					continue;
				}
				if (index_card[k] == GameConstants.INVALID_CARD && real_card == liang_card) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					index_card[k] = liang_card;
				}
			}
			roomResponse.addCardData(real_card);
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
				if (GRR.get_liang_card_count(seat_index) == GameConstants_SQ.GAME_LIANG_ZHANG_MAX) {
					roomResponse
							.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG_BIAO_TING);
				} else {
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

	/**
	 * 封装落地牌组合
	 * 
	 * @param weave_count
	 * @param weaveitems
	 * @param roomResponse
	 */
	public void builderWeaveItemResponse(int weave_count, WeaveItem weaveitems[], RoomResponse.Builder roomResponse) {
		for (int j = 0; j < weave_count; j++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
			weaveItem_item.setPublicCard(weaveitems[j].public_card);
			weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
			weaveItem_item.setCenterCard(weaveitems[j].center_card);

			// 客户端特殊处理的牌值
			for (int i = 0; i < weaveitems[j].client_special_count; i++) {
				weaveItem_item.addClientSpecialCard(weaveitems[j].client_special_card[i]);
			}

			roomResponse.addWeaveItems(weaveItem_item);
		}
	}

	/**
	 * 商丘麻将出牌处理器特殊传值
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 * @param is_liang_zhang
	 * @return
	 */
	public boolean exe_out_card(int seat_index, int card, int type, boolean is_liang_zhang) {
		// 出牌
		this.set_handler(this._handler_out_card_operate);
		this._handler_out_card_operate.reset_status(seat_index, card, type, is_liang_zhang);
		this._handler.exe(this);

		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

}
