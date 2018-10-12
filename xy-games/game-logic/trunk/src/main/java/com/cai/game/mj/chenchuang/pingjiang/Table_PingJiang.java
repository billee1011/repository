package com.cai.game.mj.chenchuang.pingjiang;

import java.util.Arrays;
import java.util.LinkedList;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_JING_DE_ZHEN;
import com.cai.common.constant.game.mj.Constants_PING_JIANG;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.ThreeDimension;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * chenchuang 平江扎鸟
 */
@ThreeDimension
public class Table_PingJiang extends AbstractMJTable {

	private static final long serialVersionUID = 1L;

	public LinkedList<String> dami_cards = new LinkedList<String>();
	public int[] bao_ting_cards = new int[27];
	public boolean is_bao_ting;
	public boolean has_bao_ting;
	public boolean is_da_mi_out_card;
	public int card258count;
	public int cardqyscount;

	public Table_PingJiang() {
		super(MJType.GAME_TYPE_MJ_PJ_ZHA_NIAO);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_PingJiang();
		_handler_dispath_card = new HandlerDispatchCard_PingJiang();
		_handler_gang = new HandlerGang_PingJiang();
		_handler_out_card_operate = new HandlerOutCardOperate_PingJiang();
	}

	/**
	 * 洗完牌执行开始
	 */
	@Override
	protected boolean on_game_start() {
		dami_cards = new LinkedList<String>();
		is_bao_ting = false;
		bao_ting_cards = new int[27];
		has_bao_ting = false;
		is_da_mi_out_card = false;
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.biaoyan[i] = 0;
			_player_result.ziba[i] = -1;
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

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i, 0);
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
		if (card_type == Constants_PING_JIANG.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_ZI_MO);// 自摸
		} else if (card_type == Constants_PING_JIANG.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_QIANG_GANG);// 抢杠胡
		} else if (card_type == Constants_PING_JIANG.HU_CARD_TYPE_DIAN_PAO) {
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_DIAN_PAO);// 点炮胡
		}
		if (GRR._left_card_count == 0)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_HAI_DI_PAI);
		// 拷贝把当前牌加入手牌
		int cur_card_index = _logic.switch_to_card_index(cur_card);
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[cur_card_index]++;
		int deluxe = 0;// 豪华数量

		// 七对4倍
		boolean xiao_qi_dui = false;
		int hu = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (hu != GameConstants.WIK_NULL) {
			xiao_qi_dui = true;
			for (int i = 0; i < GameConstants.MAX_ZI; i++) {
				if (temp_cards_index[i] == 4)
					deluxe++;
			}
		}
		// 是否胡牌牌型
		boolean analyse_win_by_cards_index = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, cur_card_index, null, 0);
		if (!(analyse_win_by_cards_index || xiao_qi_dui)) {
			return GameConstants.WIK_NULL;
		}
		// 将将胡--- 全部由258组成的牌型
		boolean is_jiang_jiang_hu = is_jiang_jiang_hu(temp_cards_index, weaveItems, weave_count);
		// 碰碰胡
		boolean is_peng_peng_hu = !exist_eat(weaveItems, weave_count)
				&& AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, cur_card_index, null, 0);
		// 清一色2倍
		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (exist_eat(weaveItems, weave_count) && !is_qing_yi_se)
			return GameConstants.WIK_NULL;
		if (!(is_jiang_jiang_hu || xiao_qi_dui || is_peng_peng_hu || is_qing_yi_se || deluxe > 0)) {
			if (!AnalyseCardUtil.analyse_258_by_cards_index(cards_index, cur_card_index, null, 0))
				return GameConstants.WIK_NULL;
			if (card_type == Constants_PING_JIANG.HU_CARD_TYPE_DIAN_PAO || card_type == Constants_PING_JIANG.HU_CARD_TYPE_QIANG_GANG) {
				if (!(GRR._left_card_count == 0 || is_da_mi_out_card || _handler_out_card_operate._type == Constants_PING_JIANG.HU_CARD_DA_MI_GSKH))
					return GameConstants.WIK_NULL;
			}
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_PING_HU);// 平胡
		}
		if (is_jiang_jiang_hu)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_JIANG_JIANG_HU);
		if (is_peng_peng_hu)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_PENG_PENG_HU);
		if (is_qing_yi_se)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_QING_YI_SE);
		if (xiao_qi_dui)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_XIAO_QI_DUI);
		if (deluxe == 1)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_HAO_HUA_1);
		if (deluxe == 2)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_HAO_HUA_2);
		if (deluxe == 3)
			chiHuRight.opr_or(Constants_PING_JIANG.CHR_HAO_HUA_3);
		return GameConstants.WIK_CHI_HU;
	}

	public int get_eat_color(WeaveItem weaveItem[], int cbWeaveCount) {
		for (int i = 0; i < cbWeaveCount; i++) {
			if (weaveItem[i].weave_kind == GameConstants.WIK_LEFT || weaveItem[i].weave_kind == GameConstants.WIK_RIGHT
					|| weaveItem[i].weave_kind == GameConstants.WIK_CENTER)
				return _logic.get_card_color(weaveItem[i].center_card);
		}

		return -1;
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

	public boolean is_ban_ban_hu(int[] temp_cards_index) {
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
			if (_playerStatus[i].is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_PING_JIANG.HU_CARD_TYPE_QIANG_GANG, i);

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
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		if (GRR._left_card_count == 0) {
			if (chr.opr_and(Constants_PING_JIANG.CHR_HAI_DI_PAI).is_empty())
				chr.opr_or(Constants_PING_JIANG.CHR_HAI_DI_PAI);
		}
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位

		int wFanShu = 0;// 番数

		countCardType(chr, seat_index);
		/*
		 * !chr.opr_and(Constants_PING_JIANG.CHR_PENG_PENG_HU).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_JIANG_JIANG_HU).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_XIAO_QI_DUI).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_QING_YI_SE).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_1).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_2).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_3).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_DA_MI).is_empty()||
		 * !chr.opr_and(Constants_PING_JIANG.CHR_HAI_DI_PAI).is_empty()
		 */

		wFanShu = getPaiXingScore(chr);

		boolean is_big_hu = is_big_hu(chr);

		// 设置大结算的显示
		if (is_big_hu) {
			if (zimo)
				_player_result.da_hu_zi_mo[seat_index]++;
			else {
				_player_result.da_hu_jie_pao[seat_index]++;
			}
		} else if (zimo) {
			_player_result.xiao_hu_zi_mo[seat_index]++;
		}

		wFanShu = is_big_hu ? wFanShu - 1 : wFanShu;
		// if(has_rule(Constants_PING_JIANG.GAME_RULE_DOUBLE_SEAL_TOP))

		/////////////////////////////////////////////// 算分//////////////////////////
		int lChiHuScore = wFanShu + GRR._player_niao_count[seat_index];

		boolean is_banker = seat_index == GRR._banker_player;
		////////////////////////////////////////////////////// 自摸 算分
		/*
		 * int basic = is_banker ? 2 : 1; if(zimo && is_big_hu) basic =
		 * is_banker ? 4 : 3; else if(!zimo && is_big_hu) basic = is_banker ? 7
		 * : 6; lChiHuScore *= basic;
		 */
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				boolean is_b = is_banker || i == GRR._banker_player;
				int basic = is_b ? 2 : 1;
				if (is_big_hu)
					basic = is_b ? 4 : 3;
				int fan = GRR._player_niao_count[i] + lChiHuScore;
				GRR._player_niao_count[seat_index] += GRR._player_niao_count[i];
				if (has_rule(Constants_PING_JIANG.GAME_RULE_DOUBLE_SEAL_TOP))
					fan = fan > 1 ? 1 : fan;
				int score = (1 << fan) * basic;
				GRR._game_score[i] -= score;
				GRR._game_score[seat_index] += score;
			}
		} else {
			boolean is_b = is_banker || provide_index == GRR._banker_player;
			int basic = is_b ? 2 : 1;
			if (is_big_hu)
				basic = is_b ? 7 : 6;
			int fan = GRR._player_niao_count[provide_index] + lChiHuScore;
			GRR._player_niao_count[seat_index] += GRR._player_niao_count[provide_index];
			if (has_rule(Constants_PING_JIANG.GAME_RULE_DOUBLE_SEAL_TOP))
				fan = fan > 1 ? 1 : fan;
			int score = (1 << fan) * basic;
			GRR._game_score[provide_index] -= score;
			GRR._game_score[seat_index] += score;
		}
		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	public int getPaiXingScore(ChiHuRight chr) {
		int wFanShu = 0;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_PENG_PENG_HU).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_JIANG_JIANG_HU).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_QING_YI_SE).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_XIAO_QI_DUI).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_HAI_DI_PAI).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_DA_MI).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_DA_MI_HU_2).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_DA_MI_HU_3).is_empty())
			wFanShu += 2;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_1).is_empty())
			wFanShu += 1;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_2).is_empty())
			wFanShu += 2;
		if (!chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_3).is_empty())
			wFanShu += 3;
		return wFanShu;
	}

	public boolean is_big_hu(ChiHuRight chr) {
		return !chr.opr_and(Constants_PING_JIANG.CHR_PENG_PENG_HU).is_empty() || !chr.opr_and(Constants_PING_JIANG.CHR_JIANG_JIANG_HU).is_empty()
				|| !chr.opr_and(Constants_PING_JIANG.CHR_XIAO_QI_DUI).is_empty() || !chr.opr_and(Constants_PING_JIANG.CHR_QING_YI_SE).is_empty()
				|| !chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_1).is_empty() || !chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_2).is_empty()
				|| !chr.opr_and(Constants_PING_JIANG.CHR_HAO_HUA_3).is_empty() || !chr.opr_and(Constants_PING_JIANG.CHR_DA_MI).is_empty()
				|| !chr.opr_and(Constants_PING_JIANG.CHR_HAI_DI_PAI).is_empty();
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
					if (type == Constants_PING_JIANG.CHR_DIAN_PAO) {
						result.append(" 点炮");
					}
					if (type == Constants_PING_JIANG.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == Constants_PING_JIANG.CHR_PING_HU) {
						result.append(" 平胡");
					}
					if (type == Constants_PING_JIANG.CHR_BAN_BAN_HU) {
						result.append(" 板板胡");
					}
					if (type == Constants_PING_JIANG.CHR_JIANG_JIANG_HU) {
						result.append(" 将将胡");
					}
					if (type == Constants_PING_JIANG.CHR_QIANG_GANG) {
						result.append(" 抢杠胡");
					}
					if (type == Constants_PING_JIANG.CHR_XIAO_QI_DUI) {
						result.append(" 小七对");
					}
					if (type == Constants_PING_JIANG.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == Constants_PING_JIANG.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == Constants_PING_JIANG.CHR_DA_MI) {
						result.append(" 打米");
					}
					if (type == Constants_PING_JIANG.CHR_HAI_DI_PAI) {
						result.append(" 海底牌");
					}
					if (type == Constants_PING_JIANG.CHR_HAO_HUA_1 || type == Constants_PING_JIANG.CHR_HAO_HUA_2
							|| type == Constants_PING_JIANG.CHR_HAO_HUA_3) {
						result.append(" 豪华");
					}
				} else if (type == Constants_PING_JIANG.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			/*
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
			 * if (an_gang > 0) { result.append(" 暗杠X" + an_gang); } if
			 * (ming_gang > 0) { result.append(" 明杠X" + ming_gang); } if
			 * (fang_gang > 0) { result.append(" 放杠X" + fang_gang); } if
			 * (jie_gang > 0) { result.append(" 接杠X" + jie_gang); }
			 */

			if (GRR._chi_hu_rights[player].is_valid() && GRR._player_niao_count[player] > 0) {
				result.append(" 中鸟X" + GRR._player_niao_count[player]);
			}

			GRR._result_des[player] = result.toString();
		}
	}

	/**
	 * 设置鸟
	 */
	public void set_niao_card() {

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
		GRR._count_niao = get_bird_num();
		if (GRR._left_card_count < GRR._count_niao) {
			if (GRR._left_card_count == 0 || GRR._left_card_count == 1) {
				GRR._cards_data_niao[0] = _repertory_card[_all_card_len - 1];
				if (GRR._count_niao > 1)
					GRR._cards_data_niao[1] = -(RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1);
				if (GRR._count_niao > 2)
					GRR._cards_data_niao[2] = -(RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1);
			} else if (GRR._left_card_count == 2) {
				GRR._cards_data_niao[0] = _repertory_card[_all_card_len - 1];
				GRR._cards_data_niao[1] = _repertory_card[_all_card_len - 2];
				GRR._cards_data_niao[2] = -(RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1);
			}
		} else {
			int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
			_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
			// GRR._left_card_count -= GRR._count_niao;
			_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
		}
		// 中鸟个数z
		for (int i = 0; i < getTablePlayerNumber(); i++)
			set_pick_niao_cards(GRR._cards_data_niao, GRR._count_niao, i);
	}

	private static final int[][] birdVaule4 = new int[][] { { 1, 5, 9 }, { 2, 6 }, { 3, 7 }, { 4, 8 } };
	private static final int[][] birdVaule3 = new int[][] { { 1, 4, 7 }, { 2, 5, 8 }, { 3, 6, 9 } };
	private static final int[][] birdVaule2 = new int[][] { { 1, 5, 9 }, { 3, 7 } };

	public void set_pick_niao_cards(int cards_data[], int card_num, int seat_index) {
		int seat = (seat_index - GRR._banker_player + getTablePlayerNumber()) % getTablePlayerNumber();
		for (int i = 0; i < card_num; i++) {
			int nValue = cards_data[i] < 0 ? -cards_data[i] : _logic.get_card_value(cards_data[i]);
			boolean flag = false;
			int[][] birdVaule = birdVaule4;
			if (getTablePlayerNumber() == 3) {
				birdVaule = birdVaule3;
			} else if (getTablePlayerNumber() == 2)
				birdVaule = birdVaule2;
			for (int v = 0; v < birdVaule[seat].length; v++) {
				if (nValue == birdVaule[seat][v]) {
					flag = true;
					break;
				}
			}
			if (flag) {
				if (cards_data[i] < 0) {
					GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] - GameConstants.DING_NIAO_VALID;
				} else
					GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i] + GameConstants.DING_NIAO_VALID;
				GRR._player_niao_count[seat_index]++;
			} else {
				GRR._player_niao_cards[seat_index][i] = GRR._cards_data_niao[i];
			}

		}
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

	public boolean estimate_player_out_card_respond_chi(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}
		int i = get_banker_next_seat(seat_index);
		int action = GameConstants.WIK_NULL;
		// int eat_color = get_eat_color(GRR._weave_items[i],
		// GRR._weave_count[i]);
		// int color = _logic.get_card_color(card);
		if (GRR._left_card_count > 0
				&& _player_result.biaoyan[i] != 1/*
													 * && (eat_color == -1 ||
													 * eat_color == color)
													 */) {
			action = _logic.check_chi(GRR._cards_index[i], card);

			if ((action & GameConstants.WIK_RIGHT) != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}
			if ((action & GameConstants.WIK_CENTER) != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_CENTER);
				_playerStatus[i].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
			if ((action & GameConstants.WIK_LEFT) != 0) {
				_playerStatus[i].add_action(GameConstants.WIK_LEFT);
				_playerStatus[i].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}

			if (_playerStatus[i].has_action()) {
				bAroseAction = true;
			}
		}
		return bAroseAction;
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
			 * int eat_color = get_eat_color(GRR._weave_items[i],
			 * GRR._weave_count[i]); int color = _logic.get_card_color(card); if
			 * (i == get_banker_next_seat(seat_index) && GRR._left_card_count >
			 * 0 && _player_result.biaoyan[i] != 1 && (eat_color == -1 ||
			 * eat_color == color)) { action =
			 * _logic.check_chi(GRR._cards_index[i], card);
			 * 
			 * if ((action & GameConstants.WIK_RIGHT) != 0) {
			 * _playerStatus[i].add_action(GameConstants.WIK_RIGHT);
			 * _playerStatus[i].add_chi(card, GameConstants.WIK_RIGHT,
			 * seat_index); } if ((action & GameConstants.WIK_CENTER) != 0) {
			 * _playerStatus[i].add_action(GameConstants.WIK_CENTER);
			 * _playerStatus[i].add_chi(card, GameConstants.WIK_CENTER,
			 * seat_index); } if ((action & GameConstants.WIK_LEFT) != 0) {
			 * _playerStatus[i].add_action(GameConstants.WIK_LEFT);
			 * _playerStatus[i].add_chi(card, GameConstants.WIK_LEFT,
			 * seat_index); }
			 * 
			 * if (_playerStatus[i].has_action()) { bAroseAction = true; } }
			 */

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {// 如果牌堆已经没有牌，出的最后一张牌了不能碰，杠
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && _player_result.biaoyan[i] != 1) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						if (GRR._left_card_count >= get_bird_num())
							playerStatus.add_action(Constants_PING_JIANG.WIK_DA_MI_GANG);
						playerStatus.add_action(Constants_PING_JIANG.WIK_DA_MI_GANG_MO_PAI);
						// playerStatus.add_action(GameConstants.WIK_GANG);
						if (GRR._left_card_count >= get_bird_num())
							add_action_weave(playerStatus, card, seat_index, 1, Constants_PING_JIANG.WIK_DA_MI_GANG);
						add_action_weave(playerStatus, card, seat_index, 1, Constants_PING_JIANG.WIK_DA_MI_GANG_MO_PAI);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_PING_JIANG.HU_CARD_TYPE_DIAN_PAO;
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

	public void add_action_weave(PlayerStatus curPlayerStatus, int card, int provider, int bai_count, int wik) {
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].public_card = bai_count;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].center_card = card;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].weave_kind = wik;
		curPlayerStatus._action_weaves[curPlayerStatus._weave_count].provide_player = provider;
		curPlayerStatus._weave_count++;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int index) {
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
				if (!chr.opr_and(Constants_PING_JIANG.CHR_JIANG_JIANG_HU).is_empty()) {
					is_bao_ting = true;
					bao_ting_cards[index] = 1;
				}
			}
		}

		// 全听
		if (count == 25) {
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
	public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave) {
		// 设置变量
		int cbActionMask = GameConstants.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
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
			// boolean liu_ju = reason == GameConstants.Game_End_DRAW || reason
			// == GameConstants.Game_End_RELEASE_PLAY;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 荒庄荒杠
				/*
				 * if(!liu_ju ||
				 * has_rule(Constants_PING_JIANG.GAME_RULE_BU_HUANG_GANG)){ for
				 * (int j = 0; j < GRR._gang_score[i].gang_count; j++) { for
				 * (int k = 0; k < getTablePlayerNumber(); k++) { lGangScore[k]
				 * += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数 } } }
				 */

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
		if (has_rule(Constants_PING_JIANG.GAME_RULE_BIRD_1))
			num = 1;
		if (has_rule(Constants_PING_JIANG.GAME_RULE_BIRD_2))
			num = 2;
		if (has_rule(Constants_PING_JIANG.GAME_RULE_BIRD_3))
			num = 3;
		return num;
	}

	/**
	 * 处理吃胡的玩家,板板胡
	 */
	public void process_chi_hu_player_operate_ban(int seat_index, int operate_card) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

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

		if (seat_index == GRR._banker_player) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(HandlerDispatchCard_PingJiang._send_card_data)]--;
		}

		// 显示胡牌
		int cards[] = new int[14];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		if (seat_index == GRR._banker_player) {
			cards[hand_card_count] = HandlerDispatchCard_PingJiang._send_card_data + GameConstants.CARD_ESPECIAL_TYPE_HU;
			hand_card_count++;
		}
		if (operate_card != -1) {
			cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
			hand_card_count++;
		}
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	public boolean operate_player_da_mi_card() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(19);
		for (String card : dami_cards) {
			roomResponse.addCardData(Integer.valueOf(card.split(",")[0]));
		}
		roomResponse.setCardCount(dami_cards.size());
		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	public static void main(String[] args) {
		LinkedList<String> dami_cards = new LinkedList<String>();
		dami_cards.add("1");
		dami_cards.add("2");
		dami_cards.add("3");
		System.out.println(dami_cards.size());
		for (String card : dami_cards) {
			System.out.println(card);
		}

	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data_bao_ting(int cards_index[], int cards_data[], int seat_index) {
		// 转换扑克
		int cbPosition = 0;
		card258count = 0;
		cardqyscount = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					int data = _logic.switch_to_card_data(i);
					boolean flag = false;
					if (has_bao_ting) {
						int value = _logic.get_card_value(data);
						if (value == 2 || value == 5 || value == 8) {
							card258count++;
							flag = true;
						}
					}
					boolean flagq = _logic.get_card_color(data) == _player_result.ziba[seat_index];
					if (flagq)
						cardqyscount++;
					if (flagq || flag)
						cards_data[cbPosition++] = data + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
					else
						cards_data[cbPosition++] = data;
				}
			}
		}
		return cbPosition;
	}

	/**
	 * 获取操作的优先等级
	 * 
	 **/
	// 获取动作等级
	public int get_action_rank(int player_action) {
		// 自摸牌等级
		if (player_action == GameConstants.WIK_ZI_MO) {
			return 50;
		}

		// 吃胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 地胡牌等级
		if (player_action == GameConstants.WIK_CHI_HU) {
			return 40;
		}

		// 硬扣等级
		if (player_action == GameConstants.WIK_YING_KUO) {
			return 35;
		}

		// 杠牌等级
		if (player_action == GameConstants.WIK_GANG) {
			return 30;
		}
		// 杠牌等级
		if (player_action == Constants_MJ_JING_DE_ZHEN.WIK_MING_GANG) {
			return 30;
		}
		// 杠牌等级
		if (player_action == Constants_MJ_JING_DE_ZHEN.WIK_WAN_GANG) {
			return 30;
		}

		// 补
		if (player_action == GameConstants.WIK_XIA_ZI_BU) {
			return 30;
		}

		// 补张牌等级
		if (player_action == GameConstants.WIK_BU_ZHNAG) {
			return 30;
		}

		// 笑
		if (player_action == GameConstants.WIK_MENG_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_DIAN_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_HUI_TOU_XIAO) {
			return 30;
		}
		if (player_action == GameConstants.WIK_XIAO_CHAO_TIAN) {
			return 30;
		}
		if (player_action == GameConstants.WIK_DA_CHAO_TIAN) {
			return 30;
		}
		if (player_action == GameConstants.WIK_YAO_YI_SE) {
			return 30;
		}

		// 碰牌等级
		if (player_action == GameConstants.WIK_PENG) {
			return 20;
		}

		if (player_action == GameConstants.WIK_BAO_TING) {
			return 15;
		}

		// 上牌等级
		if (player_action == GameConstants.WIK_RIGHT || player_action == GameConstants.WIK_CENTER || player_action == GameConstants.WIK_LEFT) {
			return 10;
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

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}
		if (getRuleValue(Constants_PING_JIANG.GAME_RULE_PEOPLE_COUNT_3) == 1) {
			return 3;
		}
		if (getRuleValue(Constants_PING_JIANG.GAME_RULE_PEOPLE_COUNT_2) == 1) {
			return 2;
		}
		return 4;
	}

	@Override
	public void test_cards() {
		// int[] cards_of_player0 = new int[] {
		// 0x02,0x02,0x02,0x05,0x05,0x05,0x08,0x08,0x08,0x18,0x18,0x18,0x13 };
		int[] cards_of_player0 = new int[] { 0x13, 0x13, 0x13, 0x11, 0x11, 0x11, 0x11, 0x23, 0x23, 0x01, 0x01, 0x01, 0x18 };
		int[] cards_of_player1 = new int[] { 0x13, 0x13, 0x13, 0x11, 0x11, 0x11, 0x23, 0x23, 0x23, 0x17, 0x18, 0x18, 0x19 };
		int[] cards_of_player2 = new int[] { 0x02, 0x02, 0x12, 0x12, 0x22, 0x22, 0x05, 0x05, 0x15, 0x15, 0x18, 0x18, 0x18 };
		int[] cards_of_player3 = new int[] { 0x13, 0x13, 0x13, 0x11, 0x11, 0x11, 0x23, 0x23, 0x23, 0x01, 0x01, 0x01, 0x14 };

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
