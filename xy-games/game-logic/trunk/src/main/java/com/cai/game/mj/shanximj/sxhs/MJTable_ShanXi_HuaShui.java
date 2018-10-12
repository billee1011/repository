package com.cai.game.mj.shanximj.sxhs;

import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.mj.Constants_MJ_SXHS;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.SpringService;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_ShanXi_HuaShui extends AbstractMJTable {

	private static final long serialVersionUID = -5573878136860256778L;

	private int[] dispatchcardNum; // 摸牌次数
	boolean isCanGenZhuang; // 可以跟庄
	private int genZhuangCount; // 跟庄次数
	private int genZhuangCard; // 跟庄牌
	private int next_seat_index;
	private int di_fen;// 底分
	private int gu_ding_pao;// 固定炮
	boolean is_shang_ju_huang_zhuang = false;// 是否上局荒庄
	boolean is_gen_zhuang = false;// 是否勾选了跟庄
	boolean first_round_has_operate = false;// 跟庄判断用的 第一轮出牌 中间过程没有任何吃 碰 胡操作的
	int hu_before_bank = 0;// 胡牌之前的庄家
	int enter_gen_zhuang_method = 0;// 进入跟庄方法的次数

	protected MJHandlerPiao_ShanXi_HuaShui _handler_piao;
	private int _magic_card_count;// 癞子牌数量

	public MJTable_ShanXi_HuaShui(MJType game_type) {
		super(game_type);
	}

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	@Override
	protected void init_shuffle() {
		List<Integer> cards_list = Lists.newArrayList();
		if (has_rule(Constants_MJ_SXHS.GAME_RULE_DAI_FENG_PAI)) {
			int[] cards = Constants_MJ_SXHS.CARD_DATA_DAI_FENG;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}

		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
			int[] cards = Constants_MJ_SXHS.CARD_DATA_HONGZHONG;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
		} else {
			int[] cards = Constants_MJ_SXHS.CARD_DATA_NO_FENG;
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
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		return 0;
	}

	// 分析胡牌的方法
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index, boolean need_to_multiply) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		boolean can_win = false; // 是否能胡牌

		// 这些在能胡牌之后，用来设置CHR，否则选美时CHR会出现不能胡牌时的CHR
		boolean has_qi_xiao_dui = false;
		boolean has_qing_yi_se = false;
		boolean has_hao_hua_qi_xiao_dui = false;// 豪华七小对
		boolean need_258 = false;
		if (has_rule(Constants_MJ_SXHS.AME_RULE_HUNAN_258)) {
			need_258 = true;
		}

		// 判断是否是七对
		int check_qi_xiao_dui = this.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, card_type);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			need_258 = false;
			can_win = true;
			has_qi_xiao_dui = true;
		}

		// 判断是否是豪华七对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == Constants_MJ_SXHS.CHR_HHQXD) {
			need_258 = false;
			can_win = true;
			has_hao_hua_qi_xiao_dui = true;
		}

		// 判断是不是清一色
		boolean is_qing_yi_se = this.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) {
			need_258 = false;
			has_qing_yi_se = true;

		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
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

		// 如果勾选了红中癞子和红中，手中必须要有2.5.8任意一张
		boolean bValue = false;
		if (need_258) {
			boolean have_258 = check_have_258(tmp_hand_cards_index);
			bValue = have_258 && AnalyseCardUtil.analyse_258_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		} else {
			// can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index,
			// _logic.switch_to_card_index(cur_card),
			// magic_cards_index, magic_card_count);
			bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
		}

		if (bValue) { // 如果能胡
			can_win = true;
		}
		if (can_win == false) { // 如果不能胡牌
			// 这里不能胡牌时也不能清空，因为选美胡时，需要叠加多张牌的CHR
			// chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_MJ_SXHS.GAME_RULE_ZIMOHU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_ZI_MO, need_to_multiply); // 自摸
		} else if (card_type == Constants_MJ_SXHS.GAME_RULE_DIAN_PAO_HU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_DIAN_PAO, need_to_multiply); // 点炮
		} else if (card_type == Constants_MJ_SXHS.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_QIANG_GANG, need_to_multiply); // 抢杠胡
		} else if (card_type == Constants_MJ_SXHS.HU_GANG_SHANG_KAI_HUA) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_GANG_SHANG_KAI_HUA, need_to_multiply); // 杠上开花
		}

		if (has_qi_xiao_dui && !has_hao_hua_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_QI_XIAO_DUI, need_to_multiply);
		}
		if (has_qing_yi_se) { // 清一色
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_QING_YI_SE, need_to_multiply);
		}

		if (has_hao_hua_qi_xiao_dui) { // 豪华七对
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HHQXD, need_to_multiply);
		}

		if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_FAN) && is_shang_ju_huang_zhuang) {// 荒庄加番
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HUANG_ZHUANG_JIA_FAN, need_to_multiply);
		}
		if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_BEI) && is_shang_ju_huang_zhuang) {// 荒庄加倍
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HUANG_ZHUANG_JIA_BEI, need_to_multiply);
		}

		return cbChiHuKind;

	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned()) // 已经弃胡
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()/*
												 * && _logic.magic_count(GRR.
												 * _cards_index[i]) == 0
												 */) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				if (!has_rule(Constants_MJ_SXHS.GAME_RULE_ZIMOHU)) {
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
							Constants_MJ_SXHS.HU_CARD_TYPE_QIANG_GANG_HU, i, false);
				}

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(Constants_MJ_SXHS.CHR_TYPE_QIANG_GANG);// 抢杠胡
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
	 * 接炮的胡牌算法
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weave_count
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @param _seat_index
	 * @return
	 */
	public int analyse_card_jie_pao(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index, boolean need_to_multiply) {

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		boolean can_win = false; // 是否能胡牌

		// 这些在能胡牌之后，用来设置CHR，否则选美时CHR会出现不能胡牌时的CHR
		boolean has_qi_xiao_dui = false;
		boolean has_qing_yi_se = false;
		boolean has_hao_hua_qi_xiao_dui = false;// 豪华七小对
		boolean need_258 = false;
		if (has_rule(Constants_MJ_SXHS.AME_RULE_HUNAN_258)) {
			need_258 = true;
		}

		// 判断是否是七对
		int check_qi_xiao_dui = this.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, card_type);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			need_258 = false;
			can_win = true;
			has_qi_xiao_dui = true;
		}

		// 判断是否是双豪华七对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == Constants_MJ_SXHS.CHR_HHQXD) {
			need_258 = false;
			can_win = true;
			has_hao_hua_qi_xiao_dui = true;
		}

		// 判断是不是清一色
		boolean is_qing_yi_se = this.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) {
			need_258 = false;
			has_qing_yi_se = true;

		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
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

		// 如果勾选了红中癞子和红中，手中必须要有2.5.8任意一张
		boolean bValue = false;
		if (need_258) {
			boolean have_258 = check_have_258(tmp_hand_cards_index);
			bValue = have_258 && AnalyseCardUtil.analyse_taojiang_258_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		} else {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index_taojiang(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);
		}

		if (bValue) { // 如果能胡
			can_win = true;
		}
		if (can_win == false) { // 如果不能胡牌
			// 这里不能胡牌时也不能清空，因为选美胡时，需要叠加多张牌的CHR
			// chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == Constants_MJ_SXHS.GAME_RULE_ZIMOHU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_ZI_MO, need_to_multiply); // 自摸
		} else if (card_type == Constants_MJ_SXHS.GAME_RULE_DIAN_PAO_HU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_DIAN_PAO, need_to_multiply); // 点炮
		} else if (card_type == Constants_MJ_SXHS.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_QIANG_GANG, need_to_multiply); // 抢杠胡
		} else if (card_type == Constants_MJ_SXHS.HU_GANG_SHANG_KAI_HUA) {
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_TYPE_GANG_SHANG_KAI_HUA, need_to_multiply); // 杠上开花
		}

		if (has_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_QI_XIAO_DUI, need_to_multiply);
		}
		if (has_qing_yi_se) { // 清一色
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_QING_YI_SE, need_to_multiply);
		}

		if (has_hao_hua_qi_xiao_dui) { // 豪华七对
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HHQXD, need_to_multiply);
		}

		if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_FAN) && is_shang_ju_huang_zhuang) {// 荒庄加番
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HUANG_ZHUANG_JIA_FAN, need_to_multiply);
		}
		if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_BEI) && is_shang_ju_huang_zhuang) {// 荒庄加倍
			chiHuRight.opr_or_xt(Constants_MJ_SXHS.CHR_HUANG_ZHUANG_JIA_BEI, need_to_multiply);
		}

		return cbChiHuKind;

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

		// 动作判断
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count >= 0) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错
				// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && action == GameConstants.WIK_GANG) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}

			boolean can_hu = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_hu = false;
					break;
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && can_hu) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				if (!has_rule(Constants_MJ_SXHS.GAME_RULE_ZIMOHU)) {
					// 如果打出的牌是是癞子牌 就要单独的分析了
					if (this._logic.is_magic_card(card)) {
						action = analyse_card_jie_pao(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants.HU_CARD_TYPE_PAOHU, i, false);
					} else {
						action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
								GameConstants.HU_CARD_TYPE_PAOHU, i, false);
					}

				}

				// 结果判断
				if (action != 0 && action == GameConstants.WIK_CHI_HU) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index); // 吃胡的组合
					bAroseAction = true;
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

	@Override
	public int getTablePlayerNumber() {
		return 4; // 四人场
	}

	private int get_seat(int nValue, int seat_index) {
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
			if (this._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
			roomResponse.addCardData(cards[j]);
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
				int tmp_card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(tmp_card)) {
					tmp_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				int_array.addItem(tmp_card);
			}

			roomResponse.addOutCardTingCards(int_array);
		}

		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		// 如果有红中癞子的玩法，是不需要判断红中的
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, Constants_MJ_SXHS.GAME_RULE_ZIMOHU, seat_index, false)) {
				if (_logic.switch_to_card_data(this._logic.get_magic_card_index(0)) == cbCurrentCard
						&& has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
					cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[count] = cbCurrentCard;
				}
				count++;
			}
		}

		if (count == 0) {
			if (has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)
					&& cards_index[this._logic.get_magic_card_index(0)] == 3) {
				cards[count] = _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
				count++;
			} else {
				if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,
						GameConstants.HZ_MAGIC_CARD, chr, Constants_MJ_SXHS.GAME_RULE_ZIMOHU, seat_index, false)) {
					cards[count] = GameConstants.HZ_MAGIC_CARD;
					count++;
				}
			}
		} else if (count > 0 && count < max_ting_count) {
			/*
			 * if (has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)&&!
			 * has_rule(Constants_MJ_SXHS.AME_RULE_HUNAN_258)) { // 有胡的牌，红中肯定能胡
			 * cards[count] =
			 * _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
			 * count++; }
			 */
		} else {
			// 全听
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_XIAOHU != _game_status && GameConstants.GS_MJ_FREE != _game_status
				&& GameConstants.GS_MJ_WAIT != _game_status) {
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
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		hu_before_bank = GRR._banker_player;// 记录每次赢牌之前的庄家
		this.init_shuffle();
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "",
								GRR._cards_index[i][j], 0l, this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {

		}

		// 游戏开始时 初始化 未托管
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	@Override
	protected void onInitTable() {
		hu_before_bank = 0;
		_handler_chi_peng = new MJHandlerChiPeng_ShanXi_HuaShui();
		_handler_dispath_card = new MJHandlerDispatchCard_ShanXi_HuaShui();
		_handler_gang = new MJHandlerGang_ShanXi_HuaShui();
		_handler_out_card_operate = new MJHandlerOutCardOperate_ShanXi_HuaShui();

		_handler_piao = new MJHandlerPiao_ShanXi_HuaShui();

		// 每次进来勾选的底分
		if (has_rule(Constants_MJ_SXHS.GAME_RULE_DI_FEN_1)) {
			di_fen = 1;
		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_DI_FEN_2)) {
			di_fen = 2;
		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_DI_FEN_5)) {
			di_fen = 5;
		} else {
			di_fen = 0;
		}

		if (has_rule(Constants_MJ_SXHS.GAME_RULE_DAI_GEN_ZHUANG)) {
			is_gen_zhuang = true;
		}

		if (has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)) { // 红中癞子
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));
			this._logic.set_magic_card_count(1);
			_magic_card_count = 1;
		}
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
		this.setGameEndBasicPrama(game_end);

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
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			game_end.addPao(_player_result.pao[i]);
		}
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
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人杠的分数
					}
				}

				// 记录
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}

			}

			// 如果不是荒庄 就要将上局记录的重置
			if (GameConstants.Game_End_DRAW != reason) {
				is_shang_ju_huang_zhuang = false;
			}

			// 跟庄的分数 游戏结束的时候再算
			int[] gen_zhuang_fen = new int[getTablePlayerNumber()];
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 如果选择跟庄 并且跟庄成功
				if (is_gen_zhuang && this.getGenZhuangCount() >= 1) {
					if (i == this.hu_before_bank) {
						gen_zhuang_fen[i] = -3;
					} else {
						gen_zhuang_fen[i] = 1;
					}
				}

				// 每个小局的分数=胡分+杠分
				GRR._game_score[i] = GRR._game_score[i] + (lGangScore[i] + gen_zhuang_fen[i]) * di_fen;
				// 记录每个小局数分数的累加
				_player_result.game_score[i] += GRR._game_score[i];

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						GRR._chi_hu_card[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
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
						GRR._cards_data[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
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

		// 错误断言
		return false;
	}

	@Override
	protected boolean on_game_start() {
		clearGenZhuangCount();
		isCanGenZhuang = true;
		enter_gen_zhuang_method = 0;
		// 如果勾选自由炮 就要开始自己选择了
		if (has_rule(Constants_MJ_SXHS.GAME_RULE_ZI_YOU_PAO)) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		}
		if (_cur_round == 1) {
			set_pao_zi();
		}

		return on_game_start_real();
	}

	protected boolean on_game_start_real() {
		_game_status = GameConstants.GS_MJ_PLAY; // 设置状态

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
				if (this._logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				gameStartResponse.addCardData(hand_cards[i][j]);

			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse
					.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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
				if (this._logic.is_magic_card(hand_cards[i][j])) {
					hand_cards[i][j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		this.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;
		if (!chr.opr_and(Constants_MJ_SXHS.CHR_QING_YI_SE).is_empty()) {
			wFanShu *= 3;
		}
		if (!chr.opr_and(Constants_MJ_SXHS.CHR_TYPE_GANG_SHANG_KAI_HUA).is_empty()) {
			wFanShu *= 3;
		}
		if (!chr.opr_and(Constants_MJ_SXHS.CHR_TYPE_QIANG_GANG).is_empty()) {
			wFanShu *= 3;
		}
		if (!chr.opr_and(Constants_MJ_SXHS.CHR_QI_XIAO_DUI).is_empty()
				&& chr.opr_and(Constants_MJ_SXHS.CHR_HHQXD).is_empty()) {
			wFanShu *= 3;
		}
		if (!chr.opr_and(Constants_MJ_SXHS.CHR_QI_XIAO_DUI).is_empty()
				&& !chr.opr_and(Constants_MJ_SXHS.CHR_HHQXD).is_empty()) {
			wFanShu *= 6;
		}

		countCardType(chr, seat_index);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			float lChiHuScore = 2 * wFanShu;// 这个是自摸的番数
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = 0;
				// 这个 输家分数=（胡牌分数+下炮子分数+杠牌分数+跟庄分数）*底分 杠分和跟庄的分数 单独抽出来 在游戏结束的时候算

				int pao_fen = 0;
				// 如果选择了自由炮
				if (has_rule(Constants_MJ_SXHS.GAME_RULE_ZI_YOU_PAO)) {
					pao_fen = (this._player_result.pao[i] + this._player_result.pao[seat_index]) * 2;// 下炮子的分数
				} else {// 固定炮了
					pao_fen = (gu_ding_pao + gu_ding_pao) * 2; // 算炮子的时候
																// 要把自己的和输家的都算上
				}

				// 荒庄加番和荒庄加倍为互斥 不能够同时存在
				// 勾选荒庄加倍
				if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_BEI) && is_shang_ju_huang_zhuang) {
					s = (lChiHuScore * 2 + pao_fen) * di_fen;
				} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_FAN) && is_shang_ju_huang_zhuang) {
					s = (lChiHuScore + pao_fen) * di_fen + 2;
				} else {
					s = (lChiHuScore + pao_fen) * di_fen;
				}

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}

		}
		////////////////////////////////////////////////////// 点炮 算分
		else {// 陕西麻将的抢杠胡 点炮的倍数*抢杠胡的倍数（3*3）
			int real_player = provide_index;
			GRR._chi_hu_rights[real_player].opr_or(Constants_MJ_SXHS.CHR_FANG_PAO);
			GRR._chi_hu_rights[seat_index].opr_or(Constants_MJ_SXHS.CHR_JIE_PAO);
			float lChiHuScore = wFanShu * 3;
			// 这个 输家分数=（胡牌分数+下炮子分数+杠牌分数+跟庄分数）*底分 杠分和跟庄的分数 单独抽出来 在游戏结束的时候算

			int pao_fen = 0;
			// 如果选择了自由炮
			if (has_rule(Constants_MJ_SXHS.GAME_RULE_ZI_YOU_PAO)) {
				pao_fen = this._player_result.pao[real_player] + this._player_result.pao[seat_index];// 下炮子的分数
			} else {// 固定炮了
				pao_fen = gu_ding_pao + gu_ding_pao; // 算炮子的时候 要把自己的和输家的都算上
			}

			// 荒庄加番和荒庄加倍为互斥 不能够同时存在
			// 勾选荒庄加倍
			if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_BEI) && is_shang_ju_huang_zhuang) {
				lChiHuScore = (lChiHuScore * 2 + pao_fen) * di_fen;
			} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_HUANG_ZHUANG_JIA_FAN) && is_shang_ju_huang_zhuang) {
				lChiHuScore = (lChiHuScore + pao_fen) * di_fen + 3;
			} else {
				lChiHuScore = (lChiHuScore + pao_fen) * di_fen;
			}

			// 胡牌分，一个人出三家的钱
			GRR._game_score[real_player] -= lChiHuScore;
			GRR._game_score[seat_index] += lChiHuScore;

		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	// type: 0自摸胡，1抢杠胡，2点炮胡
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, int type) {

	}

	@Override
	public void test_cards() {
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04,
				0x35 };
		int[] cards_of_player2 = new int[] { 0x02, 0x02, 0x04, 0x04, 0x09, 0x09, 0x28, 0x28, 0x35, 0x02, 0x04, 0x09,
				0x17 };
		int[] cards_of_player3 = new int[] { 0x12, 0x12, 0x12, 0x13, 0x14, 0x15, 0x17, 0x17, 0x18, 0x19, 0x24, 0x26,
				0x27 };
		// int[] cards_of_player0 = new int[] { 0x35, 0x02, 0x03, 0x11, 0x11,
		// 0x11, 0x13, 0x13, 0x17, 0x18, 0x19, 0x23,
		// 0x23 };
		int[] cards_of_player0 = new int[] { 0x17, 0x17, 0x35, 0x35, 0x05, 0x12, 0x12, 0x12, 0x14, 0x14, 0x14, 0x16,
				0x17 };
		// int[] cards_of_player1 = new int[] { 0x35, 0x35, 0x05, 0x06, 0x07,
		// 0x08, 0x08, 0x13, 0x14, 0x15, 0x16, 0x19,
		// 0x19 };
		// int[] cards_of_player2 = new int[] { 0x02, 0x02, 0x04, 0x04, 0x09,
		// 0x09, 0x28, 0x28, 0x35, 0x02, 0x04, 0x09,
		// 0x17 };
		// int[] cards_of_player3 = new int[] { 0x12, 0x12, 0x12, 0x13, 0x14,
		// 0x15, 0x17, 0x17, 0x18, 0x19, 0x24, 0x26,
		// 0x27 };
		// int[] cards_of_player0 = new int[] { 0x17, 0x17, 0x13, 0x35, 0x01,
		// 0x02, 0x03, 0x16, 0x17, 0x17, 0x26, 0x27,
		// 0x27 };

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
			} else if (this.getTablePlayerNumber() == 2) {
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
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			// boolean has_hhqxd = false;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == Constants_MJ_SXHS.CHR_TYPE_ZI_MO) {
						gameDesc.append(" 自摸");

					}

					if (type == Constants_MJ_SXHS.CHR_TYPE_QIANG_GANG) {
						gameDesc.append(" 抢杠胡");

					}

					if (type == Constants_MJ_SXHS.CHR_JIE_PAO) {
						gameDesc.append(" 接炮");
					}

					if (type == Constants_MJ_SXHS.CHR_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == Constants_MJ_SXHS.CHR_HHQXD) {
						gameDesc.append(" 豪华七小对");
					}
					if (type == Constants_MJ_SXHS.CHR_TYPE_GANG_SHANG_KAI_HUA) {
						gameDesc.append(" 杠上开花");
					}
					if (type == Constants_MJ_SXHS.CHR_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == Constants_MJ_SXHS.CHR_HUANG_ZHUANG_JIA_FAN) {
						gameDesc.append(" 荒庄加番");
					}
					if (type == Constants_MJ_SXHS.CHR_HUANG_ZHUANG_JIA_BEI) {
						gameDesc.append(" 荒庄加倍");
					}

				} else if (type == Constants_MJ_SXHS.CHR_FANG_PAO) {
					gameDesc.append(" 放炮");
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
				gameDesc.append(" 暗杠X" + an_gang);
			}
			if (ming_gang > 0) {
				gameDesc.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				gameDesc.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				gameDesc.append(" 接杠X" + jie_gang);
			}

			// 如果选择跟庄 并且跟庄成功
			if (is_gen_zhuang && this.getGenZhuangCount() >= 1) {
				if (player == this.hu_before_bank) {
					gameDesc.append(" 跟庄" + "-3");
				} else {
					gameDesc.append(" 跟庄" + "+1");
				}
			}

			GRR._result_des[player] = gameDesc.toString();
		}

	}

	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
			WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);
		if (weave_count != 0)
			this.load_player_info_data(roomResponse);

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
			if (this._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。 type=1 是自摸过来的 type=2 是点炮过来的
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, int type) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 如果是点炮胡 并且打过来的牌是癞子牌
		boolean lai_zi_dang_ben_shen = false;// 癞子当本身
		if ((type == 2 || type == 3) && this._logic.is_magic_card(cur_card)) {
			lai_zi_dang_ben_shen = true;
		}

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;
		// 当有癞子的时候 判断
		int singleGenCount = 0;
		int doubleGenCount = 0;
		int threeGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// boolean has_three_same_card = false;
		// boolean has_four_same_hong_zhong = false;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m)) {
						if (cbCardIndexTemp[i] == 4) {
							// has_four_same_hong_zhong = true;
						}
						continue;
					}

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3) {
						cbReplaceCount++;
					}
					if (cbCardCount == 1) {
						singleGenCount++;
					}

					if (cbCardCount == 3) {
						threeGenCount++;
						// has_three_same_card = true;
					}
					if (cbCardCount == 2) {
						doubleGenCount++;
					}
					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 3) {
					threeGenCount++;
					// has_three_same_card = true;
				}

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (_magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
				if (lai_zi_dang_ben_shen) {// 如果癞子当本身
					count = count - 1;
				}
			}

			// 有四个红中并且 单牌数为0 豪华七小对
			if (count == 4 && cbReplaceCount == 0) {
				return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			}
			// 有暗杠 并且没有单牌的为豪华七小对
			if (nGenCount >= 1 && cbReplaceCount == 0) {
				return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			}
			// 刻子数大于等于1个 并且癞子数 大于单牌数的就算豪华七对
			if (threeGenCount >= 1 && count >= cbReplaceCount) {
				return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			}
			// 手上有暗杠的 只要癞子数大于单牌数就是豪华七对
			if (nGenCount >= 1 && count >= cbReplaceCount) {
				return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			}

			// 带癞子所有七小对的情况
			if (count >= cbReplaceCount) {
				return Constants_MJ_SXHS.CHR_QI_XIAO_DUI;// 七对
			}
			if (doubleGenCount == 7) {
				return Constants_MJ_SXHS.CHR_QI_XIAO_DUI;// 七对
			}

			// // 红中能代替某张牌 是确定的 凡是代替两个单牌的不算豪华七小对 单牌的数只能为1才算时候豪华七小对
			// if (cbReplaceCount < 2 && cbReplaceCount <= count &&
			// has_three_same_card) {
			// return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			// }
			//
			// if (cbReplaceCount < 2 && cbReplaceCount <= count && nGenCount >
			// 0) {
			// return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			// }
			//
			// if (cbReplaceCount < 2 && cbReplaceCount <= count &&
			// has_four_same_hong_zhong) {
			// return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
			// }
			//
			// if (singleGenCount >= 2 && cbReplaceCount < count &&
			// has_four_same_hong_zhong) {
			// return Constants_MJ_SXHS.CHR_QI_XIAO_DUI;// 七对
			// }
			//
			// if (cbReplaceCount <= count && !has_three_same_card) {
			// return Constants_MJ_SXHS.CHR_QI_XIAO_DUI;// 七对
			// }
			//
			// if (doubleGenCount == 7) {
			// return Constants_MJ_SXHS.CHR_QI_XIAO_DUI;// 七对
			// }

			return GameConstants.WIK_NULL;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			return Constants_MJ_SXHS.CHR_HHQXD;// 豪华七对
		} else {
			return Constants_MJ_SXHS.CHR_QI_XIAO_DUI;// 七对
		}

	}

	/**
	 * 跟庄操作
	 * 
	 * @param seat_index
	 * @param card
	 * @param isZhuang
	 */
	public void addGenZhuangCard(int seat_index, int card, boolean isZhuang) {
		enter_gen_zhuang_method++;
		if (isZhuang) {
			genZhuangCard = card;
			next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
		} else {
			if (genZhuangCard != card || seat_index != next_seat_index) {
				isCanGenZhuang = false;
			} else {
				next_seat_index = (seat_index + getTablePlayerNumber() + 1) % getTablePlayerNumber();
				if (next_seat_index == _cur_banker) {
					setGenZhuangCount();
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

	/**
	 * 跟庄次数
	 * 
	 * @return the genZhuangCount
	 */
	public int getGenZhuangCount() {
		return genZhuangCount;
	}

	/**
	 * 跟庄次数累计
	 * 
	 * @param genZhuangCount
	 *            the genZhuangCount to set
	 */
	public void setGenZhuangCount() {
		this.genZhuangCount++;
	}

	/**
	 * 还原跟庄的次数
	 */
	public void clearGenZhuangCount() {
		this.genZhuangCount = 0;
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	// 清一色牌
	public boolean is_qing_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount, int cur_card) {
		// 胡牌判断
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			if (cards_index[i] != 0 && i < GameConstants.MAX_ZI) {
				// 花色判断
				if (cbCardColor != 0xFF) {
					return false;
				}

				// 设置花色
				cbCardColor = (this._logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				// 设置索引
				i = (i / 9 + 1) * 9 - 1;
			}

			// 只要手上有风牌的 除了红中 直接过滤掉 不可能是风一色的
			if (GameConstants.MAX_ZI <= i && i < GameConstants.MAX_INDEX && i != 31) {
				if (cards_index[i] != 0) {
					return false;
				}
			}

		}

		// 如果手上只有王霸
		if (cbCardColor == 0xFF) {
			// 检查组合
			cbCardColor = weaveItem[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		if ((cur_card & GameConstants.LOGIC_MASK_COLOR) != cbCardColor && cur_card != GameConstants.HZ_MAGIC_CARD)
			return false;

		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItem[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	/**
	 * 是否有258
	 * 
	 * @param cards_index
	 * @return
	 */
	private boolean check_have_258(int[] cards_index) {
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 0) {
				continue;
			}
			int cbValue = _logic.get_card_value(_logic.switch_to_card_data(i));
			int cbColor = _logic.get_card_color(_logic.switch_to_card_data(i));
			if (cbColor > 2) {
				return false;
			}
			// 单牌统计
			if ((cbValue == 2) || (cbValue == 5) || (cbValue == 8)) {
				return true;
			}

		}
		return false;
	}

	// 可以杠癞子
	public int analyse_gang_exclude_magic_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			/*
			 * if (this.is_magic_index(i)) continue;
			 */
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
					if (cards_index[j] != 1) { // 癞子不能杠，少于一张牌也直接过滤
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

		return cbActionMask;

	}

	// 杠牌分析 包括补杠 check_weave检查补杠
	public int analyse_gang(PlayerStatus curPlayerStatus, int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;
		if (GRR._left_card_count > 0) {
			// 手上杠牌
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cards_index[i] == 4) {
					cbActionMask |= GameConstants.WIK_GANG;
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
					int index = gangCardResult.cbCardCount++;
					gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
					gangCardResult.isPublic[index] = 0;// 暗杠
					gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
				}
			}
			if (check_weave) {
				// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
				for (int i = 0; i < cbWeaveCount; i++) {
					if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG && WeaveItem[i].is_vavild) {
						for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
							if (cards_index[j] != 1) {
								continue;
							} else {
								if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
									cbActionMask |= GameConstants.WIK_GANG;
									curPlayerStatus.add_action(GameConstants.WIK_GANG);
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
	 * 固定下炮子数
	 */
	public void set_pao_zi() {
		if (has_rule(Constants_MJ_SXHS.GAME_RULE_PAO_ZI_1)) {
			gu_ding_pao = 1;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._player_result.pao[i] = 1;//
			}
		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_PAO_ZI_2)) {
			gu_ding_pao = 2;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._player_result.pao[i] = 2;//
			}
		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_PAO_ZI_3)) {
			gu_ding_pao = 3;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._player_result.pao[i] = 3;//
			}
		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_PAO_ZI_4)) {
			gu_ding_pao = 4;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._player_result.pao[i] = 4;//
			}
		} else if (has_rule(Constants_MJ_SXHS.GAME_RULE_PAO_ZI_O)) {
			gu_ding_pao = 0;
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._player_result.pao[i] = 0;//
			}
		} else {
			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				this._player_result.pao[i] = -1;//
			}
		}

		operate_player_data();
	}

	public void modify_gen_zhuang_state() {
		if (this.enter_gen_zhuang_method == 4) {
			this.isCanGenZhuang = false;
			this.genZhuangCount = 0;
		}
	}

}
