package com.cai.game.mj.chenchuang.huaihua;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_HUAI_HUA;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

/**
 * chenchuang 怀化麻将
 */
@ThreeDimension
public class Table_HUAI_HUA extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	public HandlerSelectMagic_HUAI_HUA handler_select_magic;
	public boolean is_duo_xiang;
	public int[] fa_pai_count = new int[getTablePlayerNumber()];
	public boolean[] is_gang_yao = new boolean[getTablePlayerNumber()];

	public Table_HUAI_HUA() {
		super(MJType.GAME_TYPE_MJ_HUAI_HUA);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HUAI_HUA();
		_handler_dispath_card = new HandlerDispatchCard_HUAI_HUA();
		_handler_gang = new HandlerGang_HUAI_HUA();
		_handler_out_card_operate = new HandlerOutCardOperate_HUAI_HUA();
		handler_select_magic = new HandlerSelectMagic_HUAI_HUA();
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
		if (has_rule(Constants_HUAI_HUA.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
			int[] cards = Constants_HUAI_HUA.CARD_DATA_HONG_ZHONG;
			for (int i = 0; i < cards.length; i++) {
				cards_list.add(cards[i]);
			}

		} else {
			int[] cards = Constants_HUAI_HUA.CARD_DATA108;
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

	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		// 选鬼
		_logic.clean_magic_cards();
		is_duo_xiang = false;
		fa_pai_count = new int[getTablePlayerNumber()];
		is_gang_yao = new boolean[getTablePlayerNumber()];
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}
		// 发送给玩家手牌
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
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);
		exe_select_magic();
		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		// 发第一张牌
		exe_dispatch_card(_current_player, GameConstants.GANG_TYPE_HONG_ZHONG, GameConstants.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == Constants_HUAI_HUA.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_ZI_MO);// 自摸
		} else if (card_type == Constants_HUAI_HUA.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_QIANG_GANG);// 抢杠胡
		} else if (card_type == Constants_HUAI_HUA.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_DIAN_PAO);// 点炮胡
		}
		boolean is_you_da_hu = true;
		// 拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;

		// 手上四个红中直接胡牌
		boolean is_si_hong_zhong = temp_cards_index[Constants_HUAI_HUA.HONG_ZHONG_INDEX] == 4;
		// 板板胡 牌型中没有258牌子即可胡牌，不用满足基本胡牌条件
		boolean is_ban_ban_hu = ((card_type == GameConstants.CHR_ZI_MO && fa_pai_count[_seat_index] == 0)
				|| (card_type == Constants_HUAI_HUA.HU_CARD_TYPE_ZI_MO && fa_pai_count[_seat_index] == 1)) && is_you_da_hu
				&& has_rule(Constants_HUAI_HUA.GAME_RULE_BAN_BAN_HU) && is_ban_ban_hu(temp_cards_index, weaveItems, weave_count);
		// 将将胡--- 全部由258组成的牌型，不用满足基本胡牌条件
		boolean is_jiang_jiang_hu = is_you_da_hu && has_rule(Constants_HUAI_HUA.GAME_RULE_JIANG_JIANG_HU)
				&& is_jiang_jiang_hu(temp_cards_index, weaveItems, weave_count);
		// 七对4倍
		boolean xiao_qi_dui = false;
		int qi_dui_value = 0;
		int hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (hu != GameConstants.WIK_NULL) {
			xiao_qi_dui = true;
			qi_dui_value = hu;
		}

		// 得到癞子牌的个数，和索引
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();
		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}
		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}
		// 是否胡牌牌型
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, magic_cards_index,
				magic_card_count);
		if (!(analyse_win_by_cards_index || xiao_qi_dui || is_si_hong_zhong || is_ban_ban_hu || is_jiang_jiang_hu)) {
			return GameConstants.WIK_NULL;
		}
		// 碰碰胡
		boolean is_peng_peng_hu = is_you_da_hu && has_rule(Constants_HUAI_HUA.GAME_RULE_PENG_PENG_HU)
				&& AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, magic_cards_index, magic_card_count);
		// 清一色2倍
		boolean is_qing_yi_se = is_you_da_hu && has_rule(Constants_HUAI_HUA.GAME_RULE_QING_YI_SE)
				&& _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (!(card_type == Constants_HUAI_HUA.HU_CARD_TYPE_GANG_KAI_HUA || is_ban_ban_hu || is_jiang_jiang_hu || xiao_qi_dui || is_peng_peng_hu
				|| is_qing_yi_se)) {
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_PING_HU);// 平胡
		}
		if (card_type == Constants_HUAI_HUA.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_ZI_MO);// 自摸
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_GANG_SHANG_HUA);// 杠上花
		}
		if (is_ban_ban_hu)
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_BAN_BAN_HU);
		if (is_jiang_jiang_hu)
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_JIANG_JIANG_HU);
		if (is_peng_peng_hu)
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_PENG_PENG_HU);
		if (is_qing_yi_se)
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_QING_YI_SE);
		if (qi_dui_value > 0x20 && is_you_da_hu && has_rule(Constants_HUAI_HUA.GAME_RULE_HH_QI_DUI))
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_HH_QI_DUI);
		else if (xiao_qi_dui && is_you_da_hu && has_rule(Constants_HUAI_HUA.GAME_RULE_QI_DUI))
			chiHuRight.opr_or(Constants_HUAI_HUA.CHR_QI_DUI);
		return GameConstants.WIK_CHI_HU;
	}

	private boolean is_jiang_jiang_hu(int[] temp_cards_index, WeaveItem[] weaveItems, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if (value != 2 && value != 5 && value != 8)
				return false;
		}
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (temp_cards_index[i] != 0) {
				int value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if (value != 2 && value != 5 && value != 8)
					return false;
			}
		}
		return true;
	}

	private boolean is_ban_ban_hu(int[] temp_cards_index, WeaveItem[] weaveItems, int weave_count) {
		for (int i = 0; i < weave_count; i++) {
			int value = _logic.get_card_value(weaveItems[i].center_card);
			if (value == 2 || value == 5 || value == 8)
				return false;
		}
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (temp_cards_index[i] != 0) {
				int value = _logic.get_card_value(_logic.switch_to_card_data(i));
				if (value == 2 || value == 5 || value == 8)
					return false;
			}
		}
		return true;
	}

	/**
	 * 判断抢杠胡
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

			// 可以胡的情况 判断 ,强杠胡不用判断是否过圈
			if ((_playerStatus[i].is_chi_hu_round() || has_rule(Constants_HUAI_HUA.GAME_RULE_LOU_HU_KE_HU))
					&& has_rule(Constants_HUAI_HUA.GAME_RULE_QIANG_GANG_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_HUAI_HUA.HU_CARD_TYPE_QIANG_GANG, i);

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

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 1;// 番数

		countCardType(chr, seat_index);

		if (!chr.opr_and(Constants_HUAI_HUA.CHR_ZI_MO).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_PENG_PENG_HU).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_JIANG_JIANG_HU).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_QING_YI_SE).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_GANG_SHANG_HUA).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_QI_DUI).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_HH_QI_DUI).is_empty())
			wFanShu *= 4;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_GANG_SHANG_PAO).is_empty())
			wFanShu *= 2;
		if (!chr.opr_and(Constants_HUAI_HUA.CHR_QIANG_GANG).is_empty())
			wFanShu *= 2;

		int ma = GRR._player_niao_count[seat_index];// 中马的个数
		/////////////////////////////////////////////// 算分//////////////////////////
		int lChiHuScore = wFanShu;
		if (has_rule(Constants_HUAI_HUA.GAME_RULE_BIRD_JIA_BEI))
			lChiHuScore = wFanShu * (1 << ma);
		else if (has_rule(Constants_HUAI_HUA.GAME_RULE_BIRD_JIA_FEN))
			lChiHuScore = wFanShu + ma;
		else if (has_rule(Constants_HUAI_HUA.GAME_RULE_BIRD_FANG_WEI))
			lChiHuScore = wFanShu * (ma + 1);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				if (has_rule(Constants_HUAI_HUA.GAME_RULE_BIRD_FANG_WEI)) {
					GRR._game_score[i] -= lChiHuScore + wFanShu * GRR._player_niao_count[i];
					GRR._game_score[seat_index] += lChiHuScore + wFanShu * GRR._player_niao_count[i];
				} else {
					GRR._game_score[i] -= lChiHuScore;
					GRR._game_score[seat_index] += lChiHuScore;
				}
			}
		} else {// 抢杠全包
			if (!chr.opr_and(Constants_HUAI_HUA.CHR_QIANG_GANG).is_empty()
					|| !chr.opr_and(Constants_HUAI_HUA.CHR_GANG_SHANG_PAO).is_empty()) {
				GRR._game_score[provide_index] -= lChiHuScore * (getTablePlayerNumber() - 1);
				GRR._game_score[seat_index] += lChiHuScore * (getTablePlayerNumber() - 1);
			} else {
				GRR._game_score[provide_index] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;
			}
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
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
		ArrayList<Long> arr = new ArrayList<Long>();
		for (int i = 0; i < chr.type_count; i++) {
			arr.add(chr.type_list[i]);
		}
		if(!arr.contains((long)Constants_HUAI_HUA.CHR_PING_HU)){
			arr.remove((long)Constants_HUAI_HUA.CHR_ZI_MO);
			arr.remove((long)Constants_HUAI_HUA.CHR_DIAN_PAO);
		}
		arr.remove((long)Constants_HUAI_HUA.CHR_PING_HU);
		long[] type_arr = new long[arr.size()];
		for (int i = 0; i < arr.size(); i++) {
			type_arr[i] = arr.get(i);
		}
		this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, type_arr.length, type_arr, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_HUAI_HUA.CHR_DIAN_PAO) {
						result.append(" 点炮");
					}
					if (type == Constants_HUAI_HUA.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_HUAI_HUA.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_HUAI_HUA.CHR_BAN_BAN_HU) {
						result.append(" 板板胡");
					}
					if (type == Constants_HUAI_HUA.CHR_JIANG_JIANG_HU) {
						result.append(" 将将胡");
					}
					if (type == Constants_HUAI_HUA.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_HUAI_HUA.CHR_GANG_SHANG_PAO) {
						result.append(" 杠上炮");
					}
					if (type == Constants_HUAI_HUA.CHR_QI_DUI) {
						result.append(" 七对");
					}
					if (type == Constants_HUAI_HUA.CHR_HH_QI_DUI) {
						result.append(" 豪华七对");
					}
					if (type == Constants_HUAI_HUA.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_HUAI_HUA.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == Constants_HUAI_HUA.CHR_GANG_SHANG_HUA) {
						result.append(" 杠上花");
					}
				} else if (type == Constants_HUAI_HUA.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == Constants_HUAI_HUA.CHR_FANG_GANG) {
					result.append(" 被抢杠");
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
				result.append(" 明杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}

			if (GRR._player_niao_count[player] > 0) {
				result.append(" 中鸟X" + GRR._player_niao_count[player]);
			}
			String des = result.toString();
			if(des.matches("杠上炮"))
				des = des.replace(" 点炮", "");
			GRR._result_des[player] = des;
		}
	}

	private static final int[][] birdVaule = new int[][] { { 1, 5, 9 }, { 2, 6 }, { 3, 7 }, { 4, 8 } };
	private static final int[][] birdVaule3 = new int[][] { { 1, 4, 7 }, { 2, 5, 8 }, { 3, 6, 9 } };

	public void set_pick_direction_niao_cards(int cards_data[], int card_num, int seat_index) {
		int seat = (seat_index - _cur_banker + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			boolean flag = false;
			if (getTablePlayerNumber() == 4) {
				for (int v = 0; v < birdVaule[seat].length; v++) {
					if (nValue == birdVaule[seat][v]) {
						flag = true;
						break;
					}
				}
			} else {
				for (int v = 0; v < birdVaule3[seat].length; v++) {
					if ((nValue == birdVaule3[seat][v] && cards_data[i] != 0x35) || (seat_index == _cur_banker && cards_data[i] == 0x35)) {
						flag = true;
						break;
					}
				}
			}
			if (flag) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			} else {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
	}

	/**
	 * 设置鸟
	 */
	public void set_niao_card(int seat_index, boolean is_yi_pao_duo_xiang) {

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
		int bird_num = get_bird_num();
		GRR._count_niao = bird_num > GRR._left_card_count ? GRR._left_card_count : bird_num;

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			if (has_rule(Constants_HUAI_HUA.GAME_RULE_BIRD_FANG_WEI)) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					set_pick_direction_niao_cards(GRR._cards_data_niao, GRR._count_niao, i);
				}
			} else {
				if (is_yi_pao_duo_xiang) {
					set_pick_niao_cards_duo_xiang(GRR._cards_data_niao, GRR._count_niao);
					// 中鸟个数z
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (GRR._chi_hu_rights[i].is_valid())
							set_pick_niao_cards_duo_xiang(GRR._cards_data_niao, GRR._count_niao, i);
					}
				} else {
					set_pick_niao_cards(GRR._cards_data_niao, GRR._count_niao, seat_index);
				}
			}
		}
	}

	public void set_pick_niao_cards_duo_xiang(int cards_data[], int card_num) {
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				GRR._player_niao_cards[_cur_banker][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
			} else {
				GRR._player_niao_cards[_cur_banker][i] = GRR._cards_data_niao[i];
			}

		}
	}

	public void set_pick_niao_cards_duo_xiang(int cards_data[], int card_num, int seat_index) {
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				GRR._player_niao_count[seat_index]++;
			}
		}
	}

	public void set_pick_niao_cards(int cards_data[], int card_num, int seat_index) {
		for (int i = 0; i < card_num; i++) {
			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			} else {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			/*
			 * boolean can_peng = true; int[] tmp_cards_data =
			 * _playerStatus[i].get_cards_abandoned_peng(); for (int x = 0; x <
			 * GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) { if
			 * (tmp_cards_data[x] == card) { can_peng = false; break; } }
			 */
			if (GRR._left_card_count > 0 && card != Constants_HUAI_HUA.HONG_ZHONG_DATA && !is_gang_yao[i]) {// 如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						if (is_can_gang_yao(i, card)) {
							playerStatus.add_action(Constants_HUAI_HUA.WIK_GANG_YAO);
							add_gang_yao(playerStatus, card, i, 1);
						}
						bAroseAction = true;
					}
				}
			}

			if ((has_rule(Constants_HUAI_HUA.GAME_RULE_DAIN_PAO_HU) || type == Constants_HUAI_HUA.HU_CARD_TYPE_GANG_KAI_HUA)
					&& (_playerStatus[i].is_chi_hu_round() || has_rule(Constants_HUAI_HUA.GAME_RULE_LOU_HU_KE_HU))) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_HUAI_HUA.HU_CARD_TYPE_DIAN_PAO;
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
		} else {
			return false;
		}

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

		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, GameConstants.CHR_ZI_MO,
					seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}
		if (has_rule(Constants_HUAI_HUA.GAME_RULE_HONG_ZHONG_LAI_ZI)) {
			cbCurrentCard = _logic.switch_to_card_data(Constants_HUAI_HUA.HONG_ZHONG_INDEX);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr, GameConstants.CHR_ZI_MO,
					seat_index)) {
				cards[count++] = cbCurrentCard;
			}
		}

		// 全听
		if (count == 28) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	/**
	 * 混一色判断
	 * 
	 * @return
	 */
	public boolean is_hun_yi_se(int cards_index[], WeaveItem weaveItem[], int weaveCount) {
		if (_logic.get_se_count(cards_index, weaveItem, weaveCount) != 1) {
			return false;
		}

		if (_logic.has_feng_pai(cards_index, weaveItem, weaveCount) == false) {
			return false;
		}
		return true;
	}

	public boolean is_yao_jiu(int[] hand_indexs, WeaveItem weaveItem[], int weaveCount) {
		// 一九判断
		for (int i = 0; i < GameConstants.MAX_ZI_FENG; i++) {
			if (hand_indexs[i] == 0)
				continue;
			if (_logic.is_magic_index(i))
				continue;
			if (i >= GameConstants.MAX_ZI)
				return false;
			// 无效判断
			int value = _logic.get_card_value(_logic.switch_to_card_data(i));
			if (value != 1 && value != 9)
				return false;
		}

		// 落地牌都是19
		for (int i = 0; i < weaveCount; i++) {
			if (_logic.switch_to_card_index(weaveItem[i].center_card) >= GameConstants.MAX_ZI)
				return false;
			int value = _logic.get_card_value(weaveItem[i].center_card);
			if (value != 1 && value != 9)
				return false;
		}
		return true;
	}

	// 杠牌分析 包括补杠 check_weave检查补杠
	public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int cards_abandoned_gang[]) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4 && cards_abandoned_gang[i] != 1 && i != Constants_HUAI_HUA.HONG_ZHONG_INDEX) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 暗刚
				gangCardResult.type[index] = GameConstants.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1) {
							continue;
						} else if (WeaveItem[i].is_vavild && cards_abandoned_gang[j] != 1) {
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
		}

		return cbActionMask;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	@Override
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
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
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
			boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason == GameConstants.Game_End_RELEASE_PLAY;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 荒庄荒杠
				if (!liu_ju || has_rule(Constants_HUAI_HUA.GAME_RULE_BU_HUANG_GANG)) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
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

			}

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int cards[] = new int[GRR._left_card_count];// 显示剩余的牌数据
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
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
				for (int j = 0; j < GRR._count_niao; j++) {
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
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				if (_logic.is_magic_card(GRR._chi_hu_card[i][0])) {
					game_end.addHuCardData(GRR._chi_hu_card[i][0] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				}

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
		game_end.setHuXi(is_duo_xiang ? 2 : (has_rule(Constants_HUAI_HUA.GAME_RULE_BIRD_FANG_WEI) ? 1 : 0));

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

	public int get_bird_num() {
		int num = 0;
		if (has_rule(Constants_HUAI_HUA.GAME_RULE_ZHUA_BIRD_1))
			num = 1;
		if (has_rule(Constants_HUAI_HUA.GAME_RULE_ZHUA_BIRD_2))
			num = 2;
		if (has_rule(Constants_HUAI_HUA.GAME_RULE_ZHUA_BIRD_4))
			num = 4;
		return num;
	}

	public void add_gang_yao(PlayerStatus curPlayerStatus, int card, int provider, int p) {
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			if ((curPlayerStatus._action_weaves[i].center_card == card)
					&& (curPlayerStatus._action_weaves[i].weave_kind == Constants_HUAI_HUA.WIK_GANG_YAO)) {
				return;
			}
		}
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].public_card = p;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].center_card = card;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].weave_kind = Constants_HUAI_HUA.WIK_GANG_YAO;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].provide_player = provider;
		curPlayerStatus._weave_count++;
	}

	public boolean is_can_gang_yao(int seat_index, int card) {
		if (!has_rule(Constants_HUAI_HUA.GAME_RULE_GANG_SHANG_HUA) || is_gang_yao[seat_index])
			return false;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = GRR._cards_index[seat_index][i];
		}
		cbCardIndexTemp[_logic.switch_to_card_index(card)] = 0;

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = GRR._weave_count[seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = GRR._weave_items[seat_index][i].weave_kind;
			weaves[i].center_card = GRR._weave_items[seat_index][i].center_card;
			weaves[i].public_card = GRR._weave_items[seat_index][i].public_card;
			weaves[i].provide_player = GRR._weave_items[seat_index][i].provide_player;
		}
		if (weave_count < 4) {// 防止下标越界
			weaves[weave_count] = new WeaveItem();
			weaves[weave_count].weave_kind = GameConstants.WIK_GANG;
			weaves[weave_count].center_card = card;
			weaves[weave_count].public_card = 0;
			weaves[weave_count].provide_player = seat_index;
			weave_count++;
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaves, weave_count, cbCurrentCard, chr, GameConstants.CHR_ZI_MO,
					seat_index)) {
				count++;
			}
		}
		if (count > 0)
			return true;

		return false;
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int cbReplaceCount3 = 0;
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

			if (_logic.get_magic_card_count() > 0) {
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m))
						continue;

					// 单牌统计
					if (cbCardCount == 1)
						cbReplaceCount++;
					if (cbCardCount == 3)
						cbReplaceCount3++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1)
					cbReplaceCount++;
				if (cbCardCount == 3)
					cbReplaceCount3++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		int count = 0;
		if (_logic.get_magic_card_count() > 0) {
			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount + cbReplaceCount3 > count) {
				return GameConstants.WIK_NULL;
			}
			// //王牌不够
			// if( get_magic_card_index() != MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > cbCardIndexTemp[get_magic_card_index()] ||
			// get_magic_card_index() == MJGameConstants.MAX_INDEX &&
			// cbReplaceCount > 0 )
			// return MJGameConstants.WIK_NULL;
		} else {
			if (cbReplaceCount + cbReplaceCount3 > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0 || (count - cbReplaceCount - cbReplaceCount3) > 1 || (count > 0 && cbReplaceCount3 > 0)) {
			return GameConstants.CHR_HUNAN_HAOHUA_QI_XIAO_DUI;
		} else {
			return GameConstants.CHR_HUNAN_QI_XIAO_DUI;
		}

	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x11, 0x11, 0x11, 0x16, 0x16, 0x15, 0x15, 0x15, 0x12, 0x12, 0x12, 0x16, 0x13 };
		int[] cards_of_player1 = new int[] { 0x21, 0x21, 0x21, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x29, 0x29, 0x29, 0x03 };
		int[] cards_of_player2 = new int[] { 0x21, 0x21, 0x21, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x29, 0x29, 0x29, 0x03 };
		int[] cards_of_player3 = new int[] { 0x21, 0x21, 0x21, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19, 0x29, 0x29, 0x29, 0x03 };

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

}
