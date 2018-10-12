package com.cai.game.mj.fujianmj;

import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_MJ_GuangZe;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.yu.sx.GameConstants_SX;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
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

public class MJTable_FuJian_GuangZe extends AbstractMJTable {

	private static final long serialVersionUID = -5573878136860256778L;
	public int niao_num;// 抓鸟的数
	private int[] dispatchcardNum; // 摸牌次数
	private int _magic_card_count;// 癞子牌数量
	public int zhong_niao[];// 抓鸟的数
	public Boolean has_zi_mo;// 判断是否有自摸
	public Boolean has_dian_pao;// 判断是否有点炮
	public Boolean has_qiang_gang;// 判断是否有抢杠

	public MJTable_FuJian_GuangZe(MJType game_type) {
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
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_PLayer2)) {
			int[] cards = Constants_MJ_GuangZe.CARD_DATA_NO_TIAO;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}

		} else if (has_rule(Constants_MJ_GuangZe.GAME_RULE_PLayer3)) {
			int[] cards = Constants_MJ_GuangZe.CARD_DATA_HONGZHONG;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}
		} else {
			int[] cards = Constants_MJ_GuangZe.CARD_DATA_HONGZHONG;
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

		// 当是平胡的时候 平胡只有在抢杠和地胡情况下可以接炮，其他情况下只能自摸。
		boolean can_hu_xiao_hu = true;
		// 点炮胡 是不能胡小胡的 除了地胡和抢杠胡）
		// if (card_type == 2) {
		// can_hu_xiao_hu = false;
		// }
		// 3是抢杠胡 1.是自摸胡
		// if (card_type == 3 || card_type == 1) {
		// can_hu_xiao_hu = true;
		// }

		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (_playerStatus[_seat_index].isAbandoned()) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;
		boolean can_win = false; // 是否能胡牌

		// 这些在能胡牌之后，用来设置CHR，否则选美时CHR会出现不能胡牌时的CHR
		boolean has_qi_xiao_dui = false;// 七小对
		boolean has_qing_yi_se = false;// 清一色
		boolean has_hao_hua_qi_xiao_dui = false;// 豪华七小对
		boolean has_shuang_hao_hua_qi_xiao_dui = false;// 双豪华七小对
		boolean has_peng_peng_hu = false;// 碰碰胡 也叫七大对
		boolean can_win_tian_hu = false; // 天胡
		boolean can_win_di_hu = false;// 地胡-庄家打出的第一张牌 或者闲家摸到的第一张牌
		boolean has_da_san_yuan = false;// 大三元

		// 判断是否是七对
		int check_qi_xiao_dui = this.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			can_win = true;
			can_hu_xiao_hu = false;
			has_qi_xiao_dui = true;
		}

		// 判断是否是豪华七对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == Constants_MJ_GuangZe.CHR_HHQXD) {
			can_win = true;
			can_hu_xiao_hu = false;
			has_hao_hua_qi_xiao_dui = true;
		}

		// 判断是否是双豪华七对
		if (check_qi_xiao_dui != GameConstants.WIK_NULL && check_qi_xiao_dui == Constants_MJ_GuangZe.CHR_SHHQXD) {
			can_win = true;
			can_hu_xiao_hu = false;
			has_shuang_hao_hua_qi_xiao_dui = true;
		}

		// 判断是不是清一色
		boolean is_qing_yi_se = this.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		if (is_qing_yi_se) {
			can_hu_xiao_hu = false;
			has_qing_yi_se = true;

		}

		// 判断是不是大三元

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				magic_cards_index, magic_card_count);

		if (bValue) { // 如果能胡
			can_win = true;

			boolean is_peng_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
					_logic.switch_to_card_index(cur_card), magic_cards_index, magic_card_count);

			boolean exist_eat = exist_eat(GRR._weave_items[_seat_index], GRR._weave_count[_seat_index]);

			if (is_peng_peng_hu && !exist_eat) { // 碰碰胡
				has_peng_peng_hu = true;
				can_hu_xiao_hu = false;
			}

			if (is_peng_peng_hu && !exist_eat && weave_count == 0) { // 大三元
				has_da_san_yuan = true;
				can_hu_xiao_hu = false;
			}

		}

		// 庄家起手自摸算天胡
		if (_out_card_count == 0 && _seat_index == GRR._banker_player) {
			can_win_tian_hu = true;
			can_hu_xiao_hu = false;
		}
		// 庄家打的第一张牌点炮 给闲家 也叫地胡
		if (weave_count == 0 && _out_card_count == 1 && _out_card_player == GRR._banker_player
				&& _seat_index != GRR._banker_player && card_type == Constants_MJ_GuangZe.HU_CARD_TYPE_JIE_PAO) {
			can_win_di_hu = true;
			can_hu_xiao_hu = false;
		}
		// 闲家摸的第一张牌可以胡 也叫地胡
		if (isDispatchcardNum(_seat_index)) {
			if (_seat_index != GRR._banker_player && weave_count == 0 && _out_card_player == GRR._banker_player
					&& card_type == Constants_MJ_GuangZe.HU_CARD_TYPE_ZI_MO) {
				can_hu_xiao_hu = false;
				can_win_di_hu = true;
				// GRR._chi_hu_rights[_seat_index].opr_or(Constants_MJ_GuangZe.CHR_DI_HU);
			}
		}

		if (can_win == false) {
			return GameConstants.WIK_NULL;
		}

		// 如果是点炮胡 并且是平胡 则不能胡这个牌
		if (card_type == 2 && can_hu_xiao_hu) {
			return GameConstants.WIK_NULL;
		}
		// cbChiHuKind = GameConstants.WIK_NULL;
		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == Constants_MJ_GuangZe.GAME_RULE_ZIMOHU) {
			// chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_TYPE_ZI_MO,
			// need_to_multiply); // 自摸
			cbChiHuKind = Constants_MJ_GuangZe.WIK_ZI_MO;
		} else if (card_type == Constants_MJ_GuangZe.GAME_RULE_DIAN_PAO_HU) {
			// chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_TYPE_DIAN_PAO,
			// need_to_multiply); // 点炮
			cbChiHuKind = Constants_MJ_GuangZe.WIK_CHI_HU;
		} else if (card_type == Constants_MJ_GuangZe.HU_GANG_SHANG_KAI_HUA) {
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_TYPE_GANG_SHANG_KAI_HUA, need_to_multiply); // 杠上开花
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_CHI_HU;
		}

		if (can_hu_xiao_hu) { // 平胡
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_PING_HU, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_ZI_MO;// 平胡自摸显示自摸
		}
		// 七大对和单吊不能同时存在 同时存在按照单吊算
		if (has_peng_peng_hu && !(_logic.get_card_count_by_index(cards_index) == 1) && !has_da_san_yuan) { // 七大对
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_QI_DA_DUI, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_QI_DA_DUI;
		}
		if (has_qi_xiao_dui && !has_hao_hua_qi_xiao_dui && !has_shuang_hao_hua_qi_xiao_dui) { // 七小对
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_QI_XIAO_DUI, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_QI_XIAO_DUI;
		}
		if (has_qing_yi_se) { // 清一色
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_QING_YI_SE, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_QING_YI_SE;
		}
		if (_logic.get_card_count_by_index(cards_index) == 1 && !has_da_san_yuan) {// 单吊
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_DAN_DIAO, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_DAN_DIAO;
		}
		if (has_hao_hua_qi_xiao_dui && !has_shuang_hao_hua_qi_xiao_dui) { // 豪华七小对
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_HHQXD, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_HHQXD;
		}
		if (can_win_di_hu) { // 地胡
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_DI_HU, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_DI_HU;
		}
		if (can_win_tian_hu) { // 天胡
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_TIAN_HU, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_TIAN_HU;
		}
		if (has_da_san_yuan) { // 大三元
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_DA_SAN_YUAN, need_to_multiply);
			// cbChiHuKind = Constants_MJ_GuangZe.WIK_DA_SAN_YUAN;
		}
		if (has_shuang_hao_hua_qi_xiao_dui) { // 双豪华七小对
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_SHHQXD, need_to_multiply);
			// cbChiHuKind =
			// Constants_MJ_GuangZe.WIK_SHUANG_HAO_HUA_QI_XIAO_DUI;
		}

		if (card_type == Constants_MJ_GuangZe.HU_CARD_TYPE_QIANG_GANG_HU) {// 抢杠胡
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_TYPE_QIANG_GANG, need_to_multiply);
			cbChiHuKind = Constants_MJ_GuangZe.WIK_QIANG_GANG;
		}

		return cbChiHuKind;

	}

	// 判断是否是大小三元
	public boolean check_da_xiao_san_yuan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		for (int i = 0; i < weaveCount; i++) {
			int card = weaveItems[i].center_card;
			int index = _logic.switch_to_card_index(card);
			if (weaveItems[i].weave_kind == GameConstants.WIK_GANG) {
				tmp_cards_index[index] += 4;
			} else {
				tmp_cards_index[index] += 3;
			}
		}
		boolean all_four = true;
		for (int i = 31; i < 34; i++) {
			if (tmp_cards_index[i] < 3)
				return false;
			if (tmp_cards_index[i] == 3)
				all_four = false;
		}
		if (all_four) {
			return true;
		} else {
			return false;
		}
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

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_GuangZe.HU_CARD_TYPE_QIANG_GANG_HU, i, false);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(action);
					// _playerStatus[i].add_normal_wik(card,
					// Constants_MJ_GuangZe.WIK_QIANG_GANG, seat_index);// 吃胡的组合
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					// 抢杠胡
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

			if (GRR._left_card_count > niao_num) { // 牌堆还有牌才能碰和杠，不然流局算庄会出错
				// 碰牌判断
				boolean can_peng = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng = false;
						break;
					}
				}
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && can_peng) {
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
			if (_playerStatus[i].is_chi_hu_round()) {
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu = false;
						break;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round() && can_hu) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int hu_action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i, false);

				// 结果判断
				if (hu_action != 0) {
					// _playerStatus[i].add_action(hu_action);
					// _playerStatus[i].add_normal_wik(card, hu_action,
					// seat_index); // 吃胡的组合
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
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_PLayer2)) {
			return 2; // 2人场
		} else if (has_rule(Constants_MJ_GuangZe.GAME_RULE_PLayer3)) {
			return 3; // 3人场
		} else if (has_rule(Constants_MJ_GuangZe.GAME_RULE_PLayer4)) {
			return 4; // 3人场
		} else {
			return 4; // 默认4人场
		}

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

			int hu_card = analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index, false);

			if (hu_card == Constants_MJ_GuangZe.WIK_DI_HU || hu_card == Constants_MJ_GuangZe.WIK_QI_DA_DUI
					|| hu_card == Constants_MJ_GuangZe.WIK_QI_XIAO_DUI || hu_card == Constants_MJ_GuangZe.WIK_QING_YI_SE
					|| hu_card == Constants_MJ_GuangZe.WIK_DAN_DIAO || hu_card == Constants_MJ_GuangZe.WIK_HHQXD
					|| hu_card == Constants_MJ_GuangZe.WIK_TIAN_HU || hu_card == Constants_MJ_GuangZe.WIK_QIANG_GANG
					|| hu_card == Constants_MJ_GuangZe.WIK_ZI_MO) {
				hu_card = GameConstants.WIK_CHI_HU;
			}

			if (GameConstants.WIK_CHI_HU == hu_card) {
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

	// public int get_ting_card(int[] cards, int cards_index[], WeaveItem
	// weaveItem[], int cbWeaveCount, int seat_index) {
	// // 复制数据
	// int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
	// for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
	// cbCardIndexTemp[i] = cards_index[i];
	// }
	//
	// ChiHuRight chr = new ChiHuRight();
	//
	// int count = 0;
	// int cbCurrentCard;
	//
	// int max_ting_count = GameConstants.MAX_ZI_FENG;
	//
	// // 如果有红中癞子的玩法，是不需要判断红中的
	// for (int i = 0; i < max_ting_count; i++) {
	// cbCurrentCard = _logic.switch_to_card_data(i);
	// chr.set_empty();
	//
	// if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp,
	// weaveItem, cbWeaveCount, cbCurrentCard,
	// chr, Constants_MJ_GuangZe.GAME_RULE_ZIMOHU, seat_index, false)) {
	// if (_logic.switch_to_card_data(this._logic.get_magic_card_index(0)) ==
	// cbCurrentCard
	// && has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
	// cards[count] = cbCurrentCard + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
	// } else {
	// cards[count] = cbCurrentCard;
	// }
	// count++;
	// }
	// }
	//
	// if (count == 0) {
	// if (has_rule(Constants_MJ_SXHS.GAME_RULE_HONG_ZHONG_LAI_ZI)
	// && cards_index[this._logic.get_magic_card_index(0)] == 3) {
	// cards[count] =
	// _logic.switch_to_card_data(this._logic.get_magic_card_index(0));
	// count++;
	// } else {
	// if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp,
	// weaveItem, cbWeaveCount,
	// GameConstants.HZ_MAGIC_CARD, chr, Constants_MJ_SXHS.GAME_RULE_ZIMOHU,
	// seat_index, false)) {
	// cards[count] = GameConstants.HZ_MAGIC_CARD;
	// count++;
	// }
	// }
	// } else if (count > 0 && count < max_ting_count) {
	// } else {
	// // 全听
	// count = 1;
	// cards[0] = -1;
	// }
	//
	// return count;
	// }

	@Override
	protected boolean on_handler_game_start() {
		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

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
		_handler_chi_peng = new MJHandlerChiPeng_FuJian_GuangZe();
		_handler_dispath_card = new MJHandlerDispatchCard_FuJian_GuangZe();
		_handler_gang = new MJHandlerGang_FuJian_GuangZe();
		_handler_out_card_operate = new MJHandlerOutCardOperate_FuJian_GuangZe();

		// 创建面板的时候进来的抓鸟
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_NO)) {
			niao_num = 0;
		} else if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_TWO)) {
			niao_num = 2;
		} else if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_FOUR)) {
			niao_num = 4;
		} else if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_SIX)) {
			niao_num = 6;
		} else {
			niao_num = 0;
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
		for (int i = 0; i < getTablePlayerNumber(); i++) {
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

			// 跟庄的分数 游戏结束的时候再算
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				// 每个小局的分数=胡分+杠分
				GRR._game_score[i] = GRR._game_score[i] + lGangScore[i];
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

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
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
		return on_game_start_real();
	}

	protected boolean on_game_start_real() {
		_game_status = GameConstants.GS_MJ_PLAY; // 设置状态
		dispatchcardNum = new int[getTablePlayerNumber()];
		zhong_niao = new int[getTablePlayerNumber()];
		has_zi_mo = false;
		has_dian_pao = false;
		has_qiang_gang = false;
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, Constants_MJ_GuangZe.CHR_TIAN_HU, GameConstants_SX.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// 牌型如果满足条件均可以叠加
		int basi_score = 0;
		boolean only_is_qi_da_dui = false;// 仅仅只有七大对
		boolean other_hu = false;// 除七大对其他的牌型
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_PING_HU).is_empty() && has_zi_mo) {// 平胡（自摸）
			basi_score += 2;
		}

		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_PING_HU).is_empty()
				&& !chr.opr_and(Constants_MJ_GuangZe.CHR_TYPE_QIANG_GANG).is_empty()) {// 平胡（抢杠）
			basi_score += 3;
			other_hu = true;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_QI_XIAO_DUI).is_empty()
				&& chr.opr_and(Constants_MJ_GuangZe.CHR_HHQXD).is_empty()) {// 七小对
			basi_score += 8;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_HHQXD).is_empty()
				&& chr.opr_and(Constants_MJ_GuangZe.CHR_SHHQXD).is_empty()) {// 豪华七小对
			basi_score += 15;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_QING_YI_SE).is_empty()) {// 清一色
			basi_score += 10;
			other_hu = true;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_TIAN_HU).is_empty()) {// 天胡
			other_hu = true;
			// basi_score += 15;
			basi_score += 20;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_DI_HU).is_empty()) {// 地胡
			other_hu = true;
			basi_score += 15;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_QI_DA_DUI).is_empty()) {// 七大对
			basi_score += 6;
			only_is_qi_da_dui = true;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_DAN_DIAO).is_empty()) {// 单吊
			other_hu = true;
			basi_score += 15;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_DA_SAN_YUAN).is_empty()) {// 大三元
			other_hu = true;
			basi_score += 20;
		}
		if (!chr.opr_and(Constants_MJ_GuangZe.CHR_SHHQXD).is_empty()) {// 双豪华七小对
			other_hu = true;
			basi_score += 30;
		}

		// 中鸟均以159，且每个鸟为2分，七大对1个鸟算6(迭代需求 现在七大对一个鸟算2分)分，单吊和清一色七大对依然每个鸟2分。
		int niao_score = GRR._count_pick_niao;
		if (only_is_qi_da_dui && !other_hu) {
			// niao_score = 6 * niao_score;
			niao_score = 2 * niao_score;
		} else {
			niao_score = 2 * niao_score;
		}

		countCardType(chr, seat_index);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			ChiHuRight chiHuRight = GRR._chi_hu_rights[seat_index];// playerStatus._chiHuRight;
			chiHuRight.opr_or_xt(Constants_MJ_GuangZe.CHR_TYPE_ZI_MO, false); // 自摸
			float lChiHuScore = basi_score + niao_score;// 这个是自摸分数
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}

		} else { // 3.点炮中鸟需要改成承包，即中鸟分数=中鸟个数*2*（n-1） n=玩牌人数
			// 举例：如果四人场，A玩家大对子胡牌，中2个鸟，鸟分=2*2*3=12分
			////////////////////////////////////////////////////// 枪杆胡（分为放炮和抢杠胡）
			////////////////////////////////////////////////////// 算分
			int niao_num = GRR._count_pick_niao;
			int niao_score_new = niao_num * 2 * (this.getTablePlayerNumber() - 1);
			int bei_shu = this.getTablePlayerNumber() - 1;
			if (has_qiang_gang) {
				int real_player = provide_index;
				GRR._chi_hu_rights[real_player].opr_or(Constants_MJ_GuangZe.CHR_FANG_PAO);
				GRR._chi_hu_rights[seat_index].opr_or(Constants_MJ_GuangZe.CHR_JIE_PAO);
				float lChiHuScore = bei_shu * basi_score;

				// 点炮胡 谁点炮谁付钱
				GRR._game_score[real_player] -= lChiHuScore + niao_score_new;
				GRR._game_score[seat_index] += lChiHuScore + niao_score_new;

			}
			////////////////////////////////////////////////////// 点炮（分为放炮和抢杠胡）
			////////////////////////////////////////////////////// 算分
			else if (has_dian_pao) {
				int real_player = provide_index;
				GRR._chi_hu_rights[real_player].opr_or(Constants_MJ_GuangZe.CHR_FANG_PAO);
				GRR._chi_hu_rights[seat_index].opr_or(Constants_MJ_GuangZe.CHR_JIE_PAO);
				float lChiHuScore = basi_score;
				// 点炮胡 谁点炮谁付钱
				GRR._game_score[real_player] -= lChiHuScore + niao_score_new;
				GRR._game_score[seat_index] += lChiHuScore + niao_score_new;

			}

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
		int[] cards_of_player1 = new int[] { 0x06, 0x06, 0x09, 0x28, 0x25, 0x22, 0x04, 0x05, 0x06, 0x24, 0x24, 0x24,
				0x07 };
		int[] cards_of_player0 = new int[] { 0x02, 0x02, 0x02, 0x02, 0x06, 0x06, 0x06, 0x06, 0x08, 0x08, 0x09, 0x09,
				0x05 };
		// int[] cards_of_player0 = new int[] { 0x02, 0x02, 0x02, 0x09, 0x09,
		// 0x09, 0x28, 0x28, 0x28, 0x25, 0x25, 0x25,
		// 0x07 };
		int[] cards_of_player2 = new int[] { 0x05, 0x05, 0x12, 0x13, 0x14, 0x15, 0x17, 0x17, 0x18, 0x19, 0x24, 0x26,
				0x07 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x04, 0x04, 0x07,
				0x07 };
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

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {

					if (type == Constants_MJ_GuangZe.CHR_TYPE_ZI_MO) {
						gameDesc.append(" 自摸");

					}

					if (type == Constants_MJ_GuangZe.CHR_JIE_PAO) {
						gameDesc.append(" 接炮");
					}

					if (type == Constants_MJ_GuangZe.CHR_PING_HU) {
						gameDesc.append(" 平胡");

					}
					if (type == Constants_MJ_GuangZe.CHR_QI_XIAO_DUI) {
						gameDesc.append(" 七小对");
					}
					if (type == Constants_MJ_GuangZe.CHR_HHQXD) {
						gameDesc.append(" 豪华七小对");
					}
					if (type == Constants_MJ_GuangZe.CHR_TYPE_GANG_SHANG_KAI_HUA) {
						gameDesc.append(" 杠上开花");
					}
					if (type == Constants_MJ_GuangZe.CHR_QING_YI_SE) {
						gameDesc.append(" 清一色");
					}
					if (type == Constants_MJ_GuangZe.CHR_QI_DA_DUI) {
						gameDesc.append(" 七大对");
					}
					if (type == Constants_MJ_GuangZe.CHR_DAN_DIAO) {
						gameDesc.append(" 单吊");
					}
					if (type == Constants_MJ_GuangZe.CHR_TIAN_HU) {
						gameDesc.append(" 天胡");
					}
					if (type == Constants_MJ_GuangZe.CHR_DI_HU) {
						gameDesc.append(" 地胡");
					}
					if (type == Constants_MJ_GuangZe.CHR_DA_SAN_YUAN) {
						gameDesc.append(" 大三元");
					}
					if (type == Constants_MJ_GuangZe.CHR_SHHQXD) {
						gameDesc.append(" 双豪华七小对");
					}
					if (type == Constants_MJ_GuangZe.CHR_TYPE_QIANG_GANG) {
						gameDesc.append(" 抢杠胡");
					}

				} else if (type == Constants_MJ_GuangZe.CHR_FANG_PAO) {
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

			if (GRR._chi_hu_rights[player].is_valid() && zhong_niao[player] > 0) {
				gameDesc.append(" 中鸟X" + zhong_niao[player]);
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

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

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
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (this._magic_card_count > 0) {
				for (int m = 0; m < _magic_card_count; m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (this._magic_card_count > 0) {
			int count = 0;
			for (int m = 0; m < _magic_card_count; m++) {
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
			if (nGenCount >= 2) {
				// 双豪华七小对
				return Constants_MJ_GuangZe.CHR_SHHQXD;// 双豪华七对
			}
			return Constants_MJ_GuangZe.CHR_HHQXD;// 豪华七对
		} else {
			return Constants_MJ_GuangZe.CHR_QI_XIAO_DUI;// 七对
		}

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

	// 杠牌分析 自己摸起来的牌能不能杠
	public int analyse_gang_exclude_magic_card(int cards_index[], int card, WeaveItem WeaveItem[], int cbWeaveCount,
			GangCardResult gangCardResult, int cards_abandoned_gang[]) {
		// 设置变量,
		int cbActionMask = GameConstants.WIK_NULL;

		// 万条筒的暗杠
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		// 万条筒的碰杠
		for (int i = 0; i < cbWeaveCount; i++) {
			if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
				for (int j = 0; j < GameConstants.MAX_ZI; j++) {
					if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 少于一张牌也直接过滤,能接杠但是不杠的牌也过滤掉
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
		if (GRR._left_card_count > niao_num) {
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

	/**
	 * 参数初始化
	 */
	protected void onInitParam() {
		dispatchcardNum = new int[getTablePlayerNumber()];
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
	public void set_niao_card(int seat_index, int card) {

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;
		GRR._count_niao = getNIaoNum();
		GRR._count_niao = GRR._count_niao > GRR._left_card_count ? GRR._left_card_count : GRR._count_niao;
		if (GRR._count_niao == 0) {
			return;
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao,
						cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		int cbPosition = 0;
		int cards_data[] = new int[GameConstants.MAX_NIAO_CARD];
		for (int card_value : GRR._cards_data_niao) {
			int real_card = _logic.get_card_value(card_value);
			if (real_card == 1 || real_card == 5 || real_card == 9) {
				cards_data[cbPosition] = card_value;
			}
			cbPosition++;
		}

		// GRR._cards_data_niao = Arrays.copyOf(cards_data,
		// GameConstants.MAX_NIAO_CARD);

		// cards_data_niao 中鸟的数 player_niao_cards 翻出来的牌
		// 中鸟个数z
		GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		for (int i = 0; i < GRR._count_niao; i++) {
			int seat = seat_index;
			if (seat == seat_index) {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this
						.set_ding_niao_valid(GRR._cards_data_niao[i], true);//
			} else {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this
						.set_ding_niao_valid(GRR._cards_data_niao[i], false);//
			}

			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				zhong_niao[seat]++;
				GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], true);//
			} else {
				GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);
			}
		}

	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getNIaoNum() {
		int nNum = 0;
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_NO)) {
			nNum = 0;
		}
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_TWO)) {
			nNum = 2;
		}
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_FOUR)) {
			nNum = 4;
		}
		if (has_rule(Constants_MJ_GuangZe.GAME_RULE_ZHUA_NIAO_SIX)) {
			nNum = 6;
		}

		return nNum;
	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) {
				return 0;
			}

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	public int get_seat_by_value(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
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
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
				seat = seat_index;
				break;
			case 1:
				seat = get_null_seat_for_two_player(seat_index);
				break;
			case 2:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				seat = get_null_seat_for_two_player((seat_index + 1) / getTablePlayerNumber());
				break;
			}
		}
		return seat;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {

		// 自摸牌等级
		if (player_action == Constants_MJ_GuangZe.WIK_TIAN_HU) {
			return 80;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_DI_HU) {
			return 80;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_HHQXD) {
			return 80;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_DAN_DIAO) {
			return 80;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_QING_YI_SE) {
			return 70;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_QI_XIAO_DUI) {
			return 65;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_QI_DA_DUI) {
			return 60;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_QIANG_GANG) {
			return 50;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_ZI_MO) {
			return 40;
		}
		if (player_action == Constants_MJ_GuangZe.WIK_CHI_HU) {
			return 35;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		return 0;
	}

	// 获取动作序列最高等级
	public int get_action_list_rank(int action_count, int action[]) {
		int max_index = 0;

		for (int i = 0; i < action_count; i++) {
			int index = get_action_rank(action[i]);
			if (max_index < index) {
				max_index = index;
			}
		}

		return max_index;
	}

}
