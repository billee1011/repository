package com.cai.game.mj.henan.jiaozuo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_JZ;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.dictionary.SysParamDict;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
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

public class MJTable_JZ extends AbstractMJTable {

	protected MJHandlerPaoQiang_JZ _handlerPaoQiang_JZ;

	protected MJHandlerOutCardBaoTing_JZ _handler_out_card_bao_ting;

	protected int[] ting_count; // 听牌数量

	protected boolean is_bao_zhuang;

	private int[] only_zi_mo; // 只能自摸:过胡自摸玩法生效

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}

	/**
	 * 设置过胡自自能自摸
	 * 
	 * @param seat_index
	 */
	public void only_zi_mo_vaild(int seat_index) {
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_GUO_HU_ZI_MO)) {
			only_zi_mo[seat_index] = 1;
		}
	}

	/**
	 * 能否点炮胡
	 * 
	 * @param seat_index
	 * @return
	 */
	public boolean can_pao_hu(int seat_index) {
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_GUO_HU_ZI_MO)) {
			if (only_zi_mo[seat_index] == 0) {
				return true;
			} else {
				return false;
			}
		}
		return true;
	}

	public MJTable_JZ() {
		super(MJType.GAME_TYPE_JZ);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new MJHandlerChiPeng_JZ();
		_handler_dispath_card = new MJHandlerDispatchCard_JZ();
		_handler_gang = new MJHandlerGang_JZ();
		_handler_out_card_operate = new MJHandlerOutCardOperate_JZ();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_JZ();
		_handlerPaoQiang_JZ = new MJHandlerPaoQiang_JZ();
	}

	@Override
	protected boolean on_game_start() {
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_BUDAIPAO)) {
			return real_game_start();
		} else {
			GRR._banker_player = _current_player = GameConstants.INVALID_SEAT;
			this.set_handler(this._handlerPaoQiang_JZ);
			this._handlerPaoQiang_JZ.exe(this);
			return false;
		}
	}

	public void init_param() {
		ting_count = new int[getTablePlayerNumber()];
		is_bao_zhuang = false;
		only_zi_mo = new int[getTablePlayerNumber()];
	}

	public boolean real_game_start() {

		init_param();
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
			ting_count[i] = _playerStatus[i]._hu_card_count;
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		return true;
	}

	/**
	 * 包庄规则
	 */
	public void set_bao_zhuang() {
		if (!has_rule(GameConstants_JZ.GAME_RULE_JZ_515)) {
			if ((!_playerStatus[_cur_banker].is_bao_ting() && has_rule(GameConstants_JZ.GAME_RULE_JZ_BAO_TING))
					|| (ting_count[_cur_banker] == 0 && has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING))) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == _cur_banker) {
						continue;
					}
					if ((_playerStatus[i].is_bao_ting() && has_rule(GameConstants_JZ.GAME_RULE_JZ_BAO_TING))
							|| (ting_count[i] > 0 && has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING))) {
						// 算分
						GRR._game_score[_cur_banker] -= 2;
						GRR._game_score[i] += 2;
						is_bao_zhuang = true;
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return
	 */
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
			if (playerStatus.is_chi_hu_round() && can_pao_hu(i)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
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
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		// 变量定义
		if (cur_card == 0)
			return GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

		boolean hu = false; // 七小对 将将胡 可能不是能胡牌的牌型 优先判断
		long qxd = GameConstants.WIK_NULL;
		boolean ka_bian = false;
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING) && !is_515()) {
			qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		}
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
			// chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
			ka_bian = true;
			hu = true;
		}

		// 分析扑克--通用的判断胡牌方法
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 分析扑克--通用的判断胡牌方法
		// 设置变量
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);

		/*
		 * boolean bValue =
		 * AnalyseCardUtil.analyse_win_by_cards_index(cards_index,
		 * _logic.switch_to_card_index(cur_card), magic_cards_index,
		 * magic_card_count);
		 */

		// 胡牌分析
		if (bValue == false) {
			if (hu == false) {
				return GameConstants.WIK_NULL;
			}
		}

		if (ting_count[_seat_index] == 1) {// 1.只胡一张；卡边吊：两嘴
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
							chiHuRight.opr_or(GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ);
							ka_bian = true;
							break;
						} else if ((cbCardValue == 7) && (pAnalyseItem.cbCenterCard[j] == cur_card)) {
							chiHuRight.opr_or(GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ);
							ka_bian = true;
							break;
						}
					}

					if (ka_bian == true)
						break;
				}
			}

			if (ka_bian == false) {
				// 单
				ka_bian = true;
				chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
			}
		}

		// 独听不能点炮非边坎吊的牌型
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING) && !ka_bian
				&& card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_HU_BIANG_KA) && !ka_bian) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_515) && !ka_bian && card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 连6连9
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_LIANG_69)) {
			int type = check_liang6_liang9(cbCardIndexTemp);
			if (type != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(type);
			}
		}

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;
		}

		/*
		 * boolean is_peng_hu =
		 * AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
		 * _logic.switch_to_card_index(cur_card), magic_cards_index,
		 * magic_card_count);
		 * 
		 * if (is_peng_hu) { boolean exist_eat = exist_eat(weaveItems,
		 * weave_count); if (!exist_eat) {
		 * chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU); hu = true; }
		 * }
		 */

		if (hu == true) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		if (bValue) {
			hu = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		return GameConstants.WIK_NULL;
	}

	public int analyse_chi_hu_card_ting(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		// 变量定义
		if (cur_card == 0)
			return GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

		boolean hu = false; // 七小对 将将胡 可能不是能胡牌的牌型 优先判断
		long qxd = GameConstants.WIK_NULL;
		boolean ka_bian = false;
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING) && !is_515()) {
			qxd = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		}
		if (qxd != GameConstants.WIK_NULL) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
			// chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
			ka_bian = true;
			hu = true;
		}

		// 分析扑克--通用的判断胡牌方法
		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		// 分析扑克--通用的判断胡牌方法
		// 设置变量
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, false);

		/*
		 * boolean bValue =
		 * AnalyseCardUtil.analyse_win_by_cards_index(cards_index,
		 * _logic.switch_to_card_index(cur_card), magic_cards_index,
		 * magic_card_count);
		 */

		// 胡牌分析
		if (bValue == false) {
			if (hu == false) {
				return GameConstants.WIK_NULL;
			}
		}

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
						chiHuRight.opr_or(GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ);
						ka_bian = true;
						break;
					} else if ((cbCardValue == 7) && (pAnalyseItem.cbCenterCard[j] == cur_card)) {
						chiHuRight.opr_or(GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ);
						ka_bian = true;
						break;
					}
				}

				if (ka_bian == true)
					break;
			}
		}

		if (ka_bian == false) {
			ka_bian = true;
			// 单
			chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
		}

		// 独听不能点炮非边坎吊的牌型
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING) && !ka_bian
				&& card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_HU_BIANG_KA) && !ka_bian) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_515) && !ka_bian && card_type != GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 连6连9
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_LIANG_69)) {
			int type = check_liang6_liang9(cbCardIndexTemp);
			if (type != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(type);
			}
		}

		// 清一色牌
		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE);
			hu = true;
		}

		/*
		 * boolean is_peng_hu =
		 * AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
		 * _logic.switch_to_card_index(cur_card), magic_cards_index,
		 * magic_card_count);
		 * 
		 * if (is_peng_hu) { boolean exist_eat = exist_eat(weaveItems,
		 * weave_count); if (!exist_eat) {
		 * chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU); hu = true; }
		 * }
		 */

		if (hu == true) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			// 有大胡
			return GameConstants.WIK_CHI_HU;
		}

		if (bValue) {
			hu = true;
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			}
			return GameConstants.WIK_CHI_HU;
		}
		return GameConstants.WIK_NULL;
	}

	/*
	 * public int analyse_chi_hu_card_ting(int[] cards_index, WeaveItem[]
	 * weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int
	 * card_type, int _seat_index) { // 变量定义 if (cur_card == 0) return
	 * GameConstants.WIK_NULL; // 构造扑克 int cbCardIndexTemp[] = new
	 * int[GameConstants.MAX_INDEX]; for (int i = 0; i <
	 * GameConstants.MAX_INDEX; i++) { cbCardIndexTemp[i] = cards_index[i]; }
	 * 
	 * // 插入扑克 cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
	 * 
	 * boolean hu = false; //七小对 将将胡 可能不是能胡牌的牌型 优先判断 long qxd =
	 * GameConstants.WIK_NULL; if(
	 * has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING) &&
	 * !has_rule(GameConstants_JZ.GAME_RULE_JZ_HU_BIANG_KA) && !is_515()){ qxd =
	 * _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card); }
	 * if ( qxd != GameConstants.WIK_NULL ) {
	 * chiHuRight.opr_or(GameConstants.CHR_HUNAN_QI_XIAO_DUI);
	 * chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO); hu = true; }
	 * 
	 * // 分析扑克--通用的判断胡牌方法 int[] magic_cards_index = new
	 * int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT]; int magic_card_count =
	 * _logic.get_magic_card_count();
	 * 
	 * if (magic_card_count > 2) { // 一般只有两种癞子牌存在 magic_card_count = 2; }
	 * 
	 * for (int i = 0; i < magic_card_count; i++) { magic_cards_index[i] =
	 * _logic.get_magic_card_index(i); }
	 * 
	 * // 分析扑克--通用的判断胡牌方法 boolean bValue =
	 * AnalyseCardUtil.analyse_win_by_cards_index(cards_index,
	 * _logic.switch_to_card_index(cur_card), magic_cards_index,
	 * magic_card_count);
	 * 
	 * // 胡牌分析 if (bValue == false) { if (hu == false) { return
	 * GameConstants.WIK_NULL; } }
	 * 
	 * boolean is_ka_bian_diao = false; //卡 int ka =
	 * AnalyseCardUtil.analyse_ckeck_ka_bian_diao(cards_index,
	 * _logic.switch_to_card_index(cur_card), magic_cards_index,
	 * magic_card_count, false)[0]; if( ka != GameConstants.WIK_NULL && qxd ==
	 * GameConstants.WIK_NULL){
	 * chiHuRight.opr_or(GameConstants.CHR_HENAN_KA_ZHANG); is_ka_bian_diao =
	 * true; }
	 * 
	 * //边 int[] card_temp = new int[GameConstants.MAX_INDEX];
	 * 
	 * boolean flag = get_bian_card(cards_index, card_temp, cur_card); if(flag
	 * && qxd == GameConstants.WIK_NULL){ boolean can_hu =
	 * AnalyseCardUtil.analyse_win_by_cards_index(card_temp,
	 * GameConstants.INVALID_CARD, magic_cards_index, magic_card_count);
	 * if(can_hu){ chiHuRight.opr_or(GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ);
	 * is_ka_bian_diao = true; } } //吊 if
	 * (cbCardIndexTemp[_logic.switch_to_card_index(cur_card)] == 2 && qxd ==
	 * GameConstants.WIK_NULL) {// 1.只胡一张；卡边吊：两嘴 if(is_ka_bian_diao == false &&
	 * bValue == true){ chiHuRight.opr_or(GameConstants.CHR_HENAN_DAN_DIAO);
	 * is_ka_bian_diao = true; } }
	 * 
	 * 
	 * //独听不能点炮非边坎吊的牌型 if(has_rule(GameConstants_JZ.GAME_RULE_JZ_DU_TING) &&
	 * !is_ka_bian_diao && card_type != GameConstants.HU_CARD_TYPE_ZIMO){
	 * chiHuRight.set_empty(); return GameConstants.WIK_NULL; }
	 * 
	 * if(has_rule(GameConstants_JZ.GAME_RULE_JZ_HU_BIANG_KA) &&
	 * !is_ka_bian_diao){ chiHuRight.set_empty(); return GameConstants.WIK_NULL;
	 * }
	 * 
	 * if(has_rule(GameConstants_JZ.GAME_RULE_JZ_515) && !is_ka_bian_diao &&
	 * card_type != GameConstants.HU_CARD_TYPE_ZIMO){ chiHuRight.set_empty();
	 * return GameConstants.WIK_NULL; }
	 * 
	 * //连6连9 if(has_rule(GameConstants_JZ.GAME_RULE_JZ_LIANG_69)){ int type =
	 * check_liang6_liang9(cbCardIndexTemp); if(type != GameConstants.WIK_NULL){
	 * chiHuRight.opr_or(type); } }
	 * 
	 * // 清一色牌 if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count,
	 * cur_card)) { chiHuRight.opr_or(GameConstants.CHR_HUNAN_QING_YI_SE); hu =
	 * true; }
	 * 
	 * boolean is_peng_hu =
	 * AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index,
	 * _logic.switch_to_card_index(cur_card), magic_cards_index,
	 * magic_card_count);
	 * 
	 * if (is_peng_hu) { boolean exist_eat = exist_eat(weaveItems, weave_count);
	 * if (!exist_eat) { chiHuRight.opr_or(GameConstants.CHR_HUNAN_PENGPENG_HU);
	 * hu = true; } }
	 * 
	 * if (hu == true) { if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
	 * chiHuRight.opr_or(GameConstants.CHR_ZI_MO); } else {
	 * chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); } // 有大胡 return
	 * GameConstants.WIK_CHI_HU; }
	 * 
	 * if (bValue) { hu = true; if (card_type ==
	 * GameConstants.HU_CARD_TYPE_ZIMO) {
	 * chiHuRight.opr_or(GameConstants.CHR_ZI_MO); } else {
	 * chiHuRight.opr_or(GameConstants.CHR_SHU_FAN); } return
	 * GameConstants.WIK_CHI_HU; } return GameConstants.WIK_NULL; }
	 */

	public boolean get_bian_card(int[] cards_index, int[] card_temp, int card_data) {
		boolean flag = false;
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		int cur_card_index = _logic.switch_to_card_index(card_data);
		int val = _logic.get_card_value(card_data);
		int one_index = 0;
		int two_index = 0;

		if (val == 3) {
			one_index = cur_card_index - 1;
			two_index = cur_card_index - 2;
		} else if (val == 7) {
			one_index = cur_card_index + 1;
			two_index = cur_card_index + 2;
		} else {
			return flag;
		}

		if (cbCardIndexTemp[one_index] > 0) {
			cbCardIndexTemp[one_index]--;
		} else {
			return flag;
		}
		if (cbCardIndexTemp[two_index] > 0) {
			cbCardIndexTemp[two_index]--;
		} else {
			return flag;
		}
		flag = true;
		card_temp = cbCardIndexTemp;
		return flag;
	}

	/**
	 * 连6连9判断
	 * 
	 * @param card_index
	 * @return
	 */
	public int check_liang6_liang9(int[] card_index) {
		int wik_type = GameConstants.WIK_NULL;
		//int c = 0;
		for (int i = 0; i < 3; i++) {
			// 有连9提前跳出
			if (wik_type == GameConstants_JZ.CHR_HENAN_LIANG9) {
				return wik_type;
			}

			//c = 0;
			//获取单一花色的连牌值
			List<Integer> liangCards = new ArrayList<Integer>();
			for (int j = 0; j < 9; j++) {
				// 数组牌值坐标
				int index = ((i * 9) + (j));
				if (card_index[index] > 0) {
					liangCards.add(_logic.switch_to_card_data(index));
				} else {
					liangCards.clear();
				}
			}
			
			if(liangCards.size() >= 6){
				//优先检测连9牌型
				if(liangCards.size() == 9){
					int cardIndextemp[] = Arrays.copyOf(card_index, card_index.length);
					for(int c = 0; c < liangCards.size();c++){
						cardIndextemp[_logic.switch_to_card_index(liangCards.get(c))]--;
					}
					if(AnalyseCardUtil.analyse_win_by_cards_index(cardIndextemp,-1,null,0)){
						wik_type = GameConstants_JZ.CHR_HENAN_LIANG9;
					}
				}
				//连6检测
				if(wik_type != GameConstants_JZ.CHR_HENAN_LIANG9){
					for(int c = 0; c <=(liangCards.size() -6); c++  ){
						int cardIndextemp[] = Arrays.copyOf(card_index, card_index.length);
						for(int l = c; l - c == 6 ;l++){
							cardIndextemp[_logic.switch_to_card_index(liangCards.get(l))]--;
						}
						if(AnalyseCardUtil.analyse_win_by_cards_index(cardIndextemp,-1,null,0)){
							wik_type = GameConstants_JZ.CHR_HENAN_LIANG6;
						}
					}
				}
			}

			//
			// for(int j = 0; j < 9; j++ ){
			// //数组牌值坐标
			// int index = ((i*9) + (j));
			// if(card_index[index] > 0){
			// c++;
			// //一种花色判断
			// if(c >= 6 && c <= 8){
			// wik_type = GameConstants_JZ.CHR_HENAN_LIANG6;
			// }else if(c == 9){
			// wik_type = GameConstants_JZ.CHR_HENAN_LIANG9;
			// }
			// } else {
			// c = 0;
			// }
			// }
		}
		return wik_type;
	}

	
	public boolean operate_out_card_bao_ting(int seat_index, int count, int cards[], int type, int to_player) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return false;
		}
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_OUT_CARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(type);// 出牌
		roomResponse.setCardCount(count);
		int flashTime = 60;
		int standTime = 60;
		int gameId = this.getGame_id() == 0 ? 1 : this.getGame_id();
		SysParamModel sysParamModel105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		if (sysParamModel105 != null && sysParamModel105.getVal3() > 0 && sysParamModel105.getVal3() < 1000) {
			flashTime = sysParamModel105.getVal3();
			standTime = sysParamModel105.getVal4();
		}
		roomResponse.setFlashTime(flashTime);
		roomResponse.setStandTime(standTime);

		if (to_player == GameConstants.INVALID_SEAT) {
			for (int i = 0; i < count; i++) {

				roomResponse.addCardData(cards[i]);
			}
			this.send_response_to_player(seat_index, roomResponse);// 自己有值

			GRR.add_room_response(roomResponse);

//			roomResponse.clearCardData();
//			for (int i = 0; i < count; i++) {
//
//				roomResponse.addCardData(GameConstants.BLACK_CARD);
//			}
			this.send_response_to_other(seat_index, roomResponse);// 别人是背着的
		} else {
			//if (to_player == seat_index) {
				for (int i = 0; i < count; i++) {
					roomResponse.addCardData(cards[i]);
				}
//			} else {
//				for (int i = 0; i < count; i++) {
//					roomResponse.addCardData(GameConstants.BLACK_CARD);
//				}
//			}

			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}
	
	/**
	 * 听牌显示
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @param seat_index
	 * @return
	 */
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

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_ting(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_HU_BIANG_KA) && count > 1) {
			count = 0;
		}

		if (count == 0) {

		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_fan(chr, zimo);// 番数
		countCardType(chr, seat_index);

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
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = wFanShu;
		int di_fen = 1;
		// 515场底分
		if (is_515()) {
			if (is_bian_ka_diao(chr)) {
				if (zimo) {
					di_fen = 15;
				} else {
					di_fen = 10;
				}
			} else {
				di_fen = 10;
			}
			// 515不参与翻倍
			lChiHuScore = 1;
		}
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				// 庄闲
				int zx = 0;
				if (has_rule(GameConstants_JZ.GAME_RULE_JZ_ZHUANG_XIAN)) {
					if ((GRR._banker_player == i) || (GRR._banker_player == seat_index)) {
						zx = 1;
					}
				}
				float s = di_fen + zx;

				s += ((_player_result.pao[i] < 0 ? 0 : _player_result.pao[i])
						+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));// 跑分

				s *= Math.pow(2, lChiHuScore - 1);
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			// 庄闲
			int zx = 0;
			if (has_rule(GameConstants_JZ.GAME_RULE_JZ_ZHUANG_XIAN)) {
				if ((GRR._banker_player == provide_index) || (GRR._banker_player == seat_index)) {
					zx = 1;
				}
			}
			float s = (di_fen + zx);

			s += ((_player_result.pao[provide_index] < 0 ? 0 : _player_result.pao[provide_index])
					+ (_player_result.pao[seat_index] < 0 ? 0 : _player_result.pao[seat_index]));// 跑分

			s *= Math.pow(2, lChiHuScore - 1);
			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		change_player_status(seat_index, GameConstants.INVALID_VALUE);

	}

	public int get_fan(ChiHuRight chr, boolean zimo) {
		int fan = 1;

		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_QIN_YI_SE_FAN_BEI)) {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QING_YI_SE)).is_empty()) {
				fan += 1;
			}
		}
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_GANG_KAI_FAN_BEI)) {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_GANG_KAI)).is_empty()) {
				fan += 1;
			}
		}
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_QI_DUI_FAN_BEI)) {
			if (!(chr.opr_and(GameConstants.CHR_HUNAN_QI_XIAO_DUI)).is_empty()) {
				fan += 1;
			}
		}

		boolean liang6_liang9 = false;
		if (!(chr.opr_and(GameConstants_JZ.CHR_HENAN_LIANG6)).is_empty()) {
			fan += 1;
			liang6_liang9 = true;
		} else if (!(chr.opr_and(GameConstants_JZ.CHR_HENAN_LIANG9)).is_empty()) {
			fan += 2;
			liang6_liang9 = true;
		}

		if (!liang6_liang9 && zimo && !has_rule(GameConstants_JZ.GAME_RULE_JZ_ZIMO_BU_FANG_BEI)
				&& !has_rule(GameConstants_JZ.GAME_RULE_JZ_BAO_TING)) {
			if (is_bian_ka_diao(chr)) {
				fan += 1;
			}
		}

		return fan;
	}

	private boolean is_bian_ka_diao(ChiHuRight chr) {
		if (!(chr.opr_and(GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ)).is_empty()
				|| !(chr.opr_and(GameConstants_JZ.CHR_HENAN_DAN_DIAO)).is_empty()
				|| !(chr.opr_and(GameConstants_JZ.CHR_HENAN_KA_ZHANG)).is_empty()) {
			return true;
		}
		return false;
	}

	public boolean is_515() {
		if (has_rule(GameConstants_JZ.GAME_RULE_JZ_515)) {
			return true;
		}
		return false;
	}

	@Override
	protected void set_result_describe() {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						des += " 自摸";
					}
					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}

					if (has_rule(GameConstants_JZ.GAME_RULE_JZ_QI_DUI_FAN_BEI)
							&& type == GameConstants.CHR_HUNAN_QI_XIAO_DUI) {
						des += " 七小对翻倍";
					}

					if (has_rule(GameConstants_JZ.GAME_RULE_JZ_GANG_KAI_FAN_BEI)
							&& type == GameConstants.CHR_HUNAN_GANG_KAI) {
						des += " 杠上开花翻倍";
					}

					if (has_rule(GameConstants_JZ.GAME_RULE_JZ_QIN_YI_SE_FAN_BEI)
							&& type == GameConstants.CHR_HUNAN_QING_YI_SE) {
						des += " 清一色翻倍";
					}

					if (has_rule(GameConstants_JZ.GAME_RULE_JZ_LIANG_69)) {
						if (type == GameConstants_JZ.CHR_HENAN_LIANG6) {
							des += " 连6";
						} else if (type == GameConstants_JZ.CHR_HENAN_LIANG9) {
							des += " 连9";
						}
					}

					if (!has_rule(GameConstants_JZ.GAME_RULE_JZ_BAO_TING)) {
						if (type == GameConstants.CHR_HENAN_DAN_DIAO) {
							des += " 单吊";
						} else if (type == GameConstants.CHR_HENAN_KA_ZHANG) {
							des += " 卡张";
						} else if (type == GameConstants_JZ.CHR_HENAN_BIAN_ZHANG_JZ) {
							des += " 边张";
						}
					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			if (i == _cur_banker && is_bao_zhuang) {
				des += " 庄家未听牌包庄";
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#trustee_timer(int, int)
	 */
	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// WalkerGeek Auto-generated method stub
		return false;
	}

	/**
	 * @param gameTypeHenanAy
	 * @param _seat_index
	 * @param i
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
		int hu_card_count = get_ting_card(hu_cards, GRR._cards_index[seat_index], GRR._weave_items[seat_index],
				GRR._weave_count[seat_index], seat_index);

		// 还原手牌
		GRR._cards_index[seat_index][gang_card_index] = gang_card_count;
		if (hu_card_count > 0) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * @param _out_card_player
	 * @param _out_card_data
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
			if (playerStatus.is_bao_ting() == false) {
				//// 碰牌判断
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}
			if (GRR._left_card_count > 0) {
				// 杠牌判断
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					if (playerStatus.is_bao_ting() == false) {
						playerStatus.add_action(GameConstants.WIK_GANG);
						playerStatus.add_gang(card, i, 1);// 加上杠
						bAroseAction = true;
					} /*
						 * else { if (check_gang_huan_zhang(i, card) == false) {
						 * playerStatus.add_action(GameConstants.WIK_GANG);
						 * playerStatus.add_gang(card, i, 1);// 加上杠 bAroseAction
						 * = true; } }
						 */
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round() && can_pao_hu(i)) {

				if (has_rule(GameConstants_JZ.GAME_RULE_JZ_BAO_TING) && _playerStatus[i].is_bao_ting() == false) {
					continue;
				}
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
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

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handlerPaoQiang_JZ.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);

	}

	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	/**
	 * 处理牌桌结束逻辑
	 * 
	 * @param seat_index
	 * @param reason
	 */
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {

				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

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
				game_end.addGameScore(GRR._game_score[i]);
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);
		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x11, 0x12, 0x13,
				0x29 };
		int[] cards_of_player1 = new int[] { 0x22, 0x22, 0x23, 0x23, 0x12, 0x12, 0x14, 0x14, 0x14, 0x12, 0x23, 0x22,
				0x25 };
		int[] cards_of_player2 = new int[] { 0x22, 0x22, 0x23, 0x23, 0x12, 0x12, 0x14, 0x14, 0x14, 0x12, 0x23, 0x22,
				0x25 };
		int[] cards_of_player3 = new int[] { 0x22, 0x22, 0x23, 0x23, 0x12, 0x12, 0x14, 0x14, 0x14, 0x12, 0x23, 0x22,
				0x25 };
		// int[] cards_of_player3 = new int[]
		// {0x11,0x11,0x12,0x13,0x14,0x15,0x15,0x15,0x22,0x22,0x27,0x27,0x27};
		/*
		 * int[] cards_of_player1 = new int[] { 0x26, 0x26, 0x27, 0x27, 0x02,
		 * 0x02, 0x07, 0x07, 0x08, 0x11, 0x11, 0x13, 0x13 }; int[]
		 * cards_of_player2 = new int[] { 0x03, 0x04, 0x05, 0x15, 0x16, 0x17,
		 * 0x17, 0x18, 0x19, 0x01, 0x01, 0x25, 0x27 }; int[] cards_of_player3 =
		 * new int[] { 0x01, 0x02, 0x03, 0x05, 0x06, 0x07, 0x09, 0x09, 0x23,
		 * 0x24, 0x25, 0x27, 0x28 };
		 */

		// int[] cards_of_player0 = new int[] {
		// 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07 };
		// int[] cards_of_player1 = new int[] {
		// 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07 };
		// int[] cards_of_player2 = new int[] {
		// 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07};
		// int[] cards_of_player3 = new int[] {
		// 0x01,0x01,0x01,0x02,0x02,0x02,0x03,0x03,0x03,0x05,0x05,0x05,0x07 };
		//
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int j = 0; j < 13; j++) {
			if (this.getTablePlayerNumber() == 4) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
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
}
