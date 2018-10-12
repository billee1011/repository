package com.cai.game.hh.handler.ahphz;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_AnHua;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.dictionary.SysParamDict;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
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

public class AnHuaHHTable extends HHTable {

	private static final long serialVersionUID = -283999092390496485L;

	public int[][] laizi_operate;

	public int[] hu_count;

	public int continue_huan_zhuang;

	public AnHuaHHTable() {
		super();
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_AnHua.GAME_RULE_PLAYER_2)) {
			return 2;
		}
		return GameConstants.GAME_PLAYER_HH;
	}

	// @Override
	// public String get_game_des() {
	// StringBuffer buffer = new StringBuffer();
	//
	// buffer.append("15胡息起胡");
	// if (has_rule(GameConstants_AnHua.GAME_RULE_ONE_XI_ONE_TUN)) {
	// buffer.append(" 一息一囤");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_THREE_XI_ONE_TUN)) {
	// buffer.append(" 三息一囤");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_ADD_FAN)) {
	// buffer.append(" 自摸翻倍");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_ADD_TUN)) {
	// buffer.append(" 自摸加一囤");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_NULL)) {
	// buffer.append(" 自摸不加息");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_SAN_TI_WU_KAN)) {
	// buffer.append(" 三提五坎");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_DI_FEN_1)) {
	// buffer.append(" 底分为1");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_DI_FEN_2)) {
	// buffer.append(" 底分为2");
	// }
	// if (has_rule(GameConstants_AnHua.GAME_RULE_DI_FEN_3)) {
	// buffer.append(" 底分为3");
	// }
	// return buffer.toString();
	// }

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_AnHua();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_AnHua();
		_handler_gang = new PHZHandlerGang_AnHua();
		_handler_chi_peng = new PHZHandlerChiPeng_AnHua();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_AnHua();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_AnHua();

		hu_count = new int[getTablePlayerNumber()];
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;
		log_info("gme_status:" + this._game_status);

		laizi_operate = new int[getTablePlayerNumber()][2];

		reset_init_data();

		// 庄家选择
		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants.CARD_COUNT_PHZ_HS];
		shuffle(_repertory_card, GameConstants.CARD_PHZ_DEFAULT);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		boolean can_ti = false;
		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		int FlashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				FlashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(FlashTime);
			roomResponse.setStandTime(standTime);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			this._playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (this._playerStatus[i]._hu_card_count > 0) {
				this.operate_chi_hu_cards(i, this._playerStatus[i]._hu_card_count, this._playerStatus[i]._hu_cards);
			}
		}
		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		// 分析扑克
		this._hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray, false,
				hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];
							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_cards_count = 0;
		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < 15) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		// 过账的牌胡息未改变则不能胡牌
		for (int index = 0; index < _guo_hu_pai_count[seat_index]; index++) {
			if (_guo_hu_pai_cards[seat_index][index] != cur_card) {
				continue;
			}

			if (_guo_hu_xi[seat_index][index] == max_hu_xi) {
				return GameConstants.WIK_NULL;
			}
		}
		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		for (int n = 0; n < 7; n++) {
			int kind = analyseItem.cbWeaveKind[n];
			int centerCard = analyseItem.cbCenterCard[n];
			// if (kind == GameConstants.WIK_KAN)
			// continue;
			int[] realCard = new int[4];
			_logic.get_weave_card(kind, centerCard, realCard);
			for (int m = 0; m < realCard.length; m++)
				if (cur_card == realCard[m]) {
					this.laizi_operate[seat_index][0] = cur_card;
					this.laizi_operate[seat_index][1] = kind;
				}
		}
		if (analyseItem.curCardEye == true) {
			if (analyseItem.cbCardEye == cur_card) {
				this.laizi_operate[seat_index][0] = cur_card;
				this.laizi_operate[seat_index][1] = GameConstants.WIK_DUI_ZI;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index == provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_ZIMO && (seat_index != provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants.CHR_JEI_PAO_HU);
		} else if (dispatch == false) {
			chiHuRight.opr_or(GameConstants.CHR_DIAN_PAO_HU);
		}
		if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_TIAN_HU);
		}
		if (card_type == GameConstants.OutCard_Type_Di_Hu) {
			chiHuRight.opr_or(GameConstants.CHR_DI_HU);
		}
		if (_logic.is_shi_ba_xiao(analyseItem)) {
			chiHuRight.opr_or(GameConstants.CHR_SHI_BA_XIAO);
			return cbChiHuKind;
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants.CHR_ALL_HEI);
			return cbChiHuKind;
		}
		if (hong_pai_count >= 13) {
			chiHuRight.opr_or(GameConstants.CHR_THIRTEEN_HONG_PAI);
			return cbChiHuKind;
		}
		if (hong_pai_count == 1) {
			chiHuRight.opr_or(GameConstants.CHR_ONE_HONG);
			return cbChiHuKind;
		}
		if (hong_pai_count >= 10 && hong_pai_count < 13) {
			chiHuRight.opr_or(GameConstants.CHR_TEN_HONG_PAI);
			return cbChiHuKind;
		}
		return cbChiHuKind;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION_RECORD);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);
		if (GRR == null) {
			return true;
		}
		GRR.add_room_response(roomResponse);

		return true;
	}

	public int estimate_player_ti_wei_respond_phz(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			bAroseAction = GameConstants.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
					bAroseAction = GameConstants.WIK_CHOU_WEI;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
		}
		return bAroseAction;
	}

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_phz(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
		// 变量定义
		int bAroseAction = GameConstants.WIK_NULL;// 出现(是否)有
		pao_type[0] = 0;

		// 碰转跑
		if ((bAroseAction == GameConstants.WIK_NULL) && (dispatch == true)) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_PENG))
					continue;
				pao_type[0] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}
		// 跑牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (seat_index != provider_index)) {
			if (_logic.check_pao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
				pao_type[0] = GameConstants.PAO_TYPE_OHTER_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}
		// 扫转跑
		if (seat_index != provider_index) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI))
					continue;
				pao_type[0] = GameConstants.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}

		}

		//
		return bAroseAction;
	}

	/**
	 * 统计胡牌类型
	 * 
	 * @param _seat_index
	 */
	@Override
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		if (isZimo) {
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] += this._hu_xi[_seat_index];
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}

		if (!(chiHuRight.opr_and(GameConstants.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_HAI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_TING_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants.CHR_SHI_BA_XIAO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
		if (GRR != null) {
			roomResponse.setLeftCardCount(GRR._left_card_count);
			for (int i = 0; i < GRR._especial_card_count; i++) {
				if (_logic.is_ding_gui_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_DING_GUI);
				} else if (_logic.is_lai_gen_card(GRR._especial_show_cards[i])) {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
				} else {
					roomResponse.addEspecialShowCards(GRR._especial_show_cards[i]);
				}

			}

			if (GRR._especial_txt != "") {
				roomResponse.setEspecialTxt(GRR._especial_txt);
				roomResponse.setEspecialTxtType(GRR._especial_txt_type);
			}
		}
		roomResponse.setGameStatus(_game_status);
		roomResponse.setQiangMax(continue_huan_zhuang);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * 算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		int tunJiShu = 1; // 囤基数
		if (has_rule(GameConstants_AnHua.GAME_RULE_ONE_XI_ONE_TUN)) {
			tunJiShu = 1;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_THREE_XI_ONE_TUN)) {
			tunJiShu = 3;
		}
		int baseFen = 0;
		if (has_rule(GameConstants_AnHua.GAME_RULE_DI_FEN_1)) {
			baseFen = 1;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_DI_FEN_2)) {
			baseFen = 2;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_DI_FEN_3)) {
			baseFen = 3;
		}

		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);
		// 计算胡息
		int all_hu_xi = 0;
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}

		int wFanShu = get_chi_hu_action_rank_phz(chr);// 番数
		this._hu_xi[seat_index] = all_hu_xi;
		int tun = 1 + (all_hu_xi - 15) / tunJiShu;
		if (!chr.opr_and(GameConstants.CHR_TIAN_HU).is_empty()) {
			wFanShu *= 2;
		} else if (seat_index == provide_index) {
			if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_ADD_TUN)) {
				tun++;
			}
			if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_ADD_FAN)) {
				wFanShu *= 2;
			}
		}

		// 统计
		if (zimo) {
			// 自摸
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		}

		this._tun_shu[seat_index] = tun;
		this._fan_shu[seat_index] = wFanShu;
		float lChiHuScore = wFanShu * tun * baseFen;
		int rule = getRuleValue(GameConstants_AnHua.GAME_RULE_HU_ADD_1);
		if (rule != 0) {
			lChiHuScore += rule;
		}

		lChiHuScore *= get_pa_po();
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 胡牌分
				GRR._game_score[i] -= lChiHuScore;
				GRR._game_score[seat_index] += lChiHuScore;

			}
		}

	}

	public int get_pa_po() {
		int fan = 1 << continue_huan_zhuang;
		fan = fan > get_pa_po_limit() ? get_pa_po_limit() : fan;
		return fan == 0 ? 1 : fan;
	}

	private int get_pa_po_limit() {
		if (has_rule(GameConstants_AnHua.GAME_RULE_PA_PO_1)) {
			return 2;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_PA_PO_2)) {
			return 4;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_PA_PO_3)) {
			return 8;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_PA_PO_4)) {
			return 16;
		}
		if (has_rule(GameConstants_AnHua.GAME_RULE_PA_PO_5)) {
			return 32;
		}
		return 0;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (reason == GameConstants_AnHua.Game_End_DRAW) {
			continue_huan_zhuang++;
		} else {
			continue_huan_zhuang = 0;
		}

		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL ||
							// reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			int cards[] = new int[GRR._left_card_count];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			if (seat_index != -1) {
				game_end.setCountPickNiao(laizi_operate[seat_index][1]);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (seat_index != -1) {
				game_end.setTunShu(this._tun_shu[seat_index]);
				game_end.setFanShu(this._fan_shu[seat_index]);
			}
			if (this.has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					this._huang_zhang_count++;
				} else {
					this._huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				int all_hu_xi = 0;
				// // 组合
				// WeaveItemResponseArrayResponse.Builder weaveItem_array =
				// WeaveItemResponseArrayResponse.newBuilder();
				// if (_hu_weave_count[i] > 0) {
				// for (int j = 0; j < _hu_weave_count[i]; j++) {
				// WeaveItemResponse.Builder weaveItem_item =
				// WeaveItemResponse.newBuilder();
				// weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
				// weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
				// weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
				// weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
				// all_hu_xi += _hu_weave_items[i][j].hu_xi;
				// weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
				// weaveItem_array.addWeaveItem(weaveItem_item);
				// }
				// }

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if (_hu_weave_count[i] > 0 && GRR._win_order[i] == 1) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						all_hu_xi += _hu_weave_items[i][j].hu_xi;
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				} else {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	@Override
	public PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
		// 大赢家
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < count; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < count; j++) {
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

		for (int i = 0; i < count; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < count; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			player_result.addHuPaiCount(_player_result.hu_pai_count[i]);
			player_result.addMingTangCount(_player_result.ming_tang_count[i]);
			player_result.addYingXiCount(hu_count[i]);
			player_result.addLiuZiFen(_player_result.liu_zi_fen[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);

			player_result.addPlayersId(i);
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

	// 转转麻将结束描述
	@Override
	public void set_result_describe(int seat_index) {
		// 杠牌，每个人的分数
		int lGangScore[] = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < getTablePlayerNumber(); k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			boolean show = true;
			boolean has_ping_hu = false;
			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_TIAN_HU) {
						des += ",天胡 x2";
					}
					if (type == GameConstants.CHR_ZI_MO) {
						if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_ADD_FAN)) {
							des += ",自摸 x2";
						}
						if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_ADD_TUN)) {
							des += ",自摸 +1";
						}
						if (has_rule(GameConstants_AnHua.GAME_RULE_ZI_MO_NULL)) {
							des += ",自摸";
						}
					}
					if (type == GameConstants.CHR_TEN_HONG_PAI) {
						des += ",红胡 x2";
						show = false;
					}
					if (type == GameConstants.CHR_ONE_HONG) {
						des += ",真点胡 x3";
						show = false;
					}
					if (type == GameConstants.CHR_THIRTEEN_HONG_PAI) {
						des += ",红乌 x4";
						show = false;
					}
					if (type == GameConstants.CHR_ALL_HEI) {
						des += ",乌胡 x5";
						show = false;
					}
					if (type == GameConstants.CHR_SHI_BA_XIAO) {
						des += ",十八小 x6";
						show = false;
					}
					if (type == GameConstants.CHR_JEI_PAO_HU && show) {
						has_ping_hu = true;
					}
				}
			}

			if (show & has_ping_hu) {
				des += ",平胡";
			}

			GRR._result_des[i] = des;
		}
	}

	public int get_chi_hu_action_rank_phz(ChiHuRight chr) {
		int wFanShu = 0;
		if (!(chr.opr_and(GameConstants.CHR_TEN_HONG_PAI)).is_empty()) {
			wFanShu = 2;
		}
		if (!(chr.opr_and(GameConstants.CHR_ONE_HONG)).is_empty()) {
			wFanShu = 3;
		}
		if (!(chr.opr_and(GameConstants.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			wFanShu = 4;
		}
		if (!(chr.opr_and(GameConstants.CHR_ALL_HEI)).is_empty()) {
			wFanShu = 5;
		}
		if (!(chr.opr_and(GameConstants.CHR_SHI_BA_XIAO)).is_empty()) {
			wFanShu = 6;
		}
		return wFanShu == 0 ? 1 : wFanShu;
	}

	/**
	 * 碰、跑、偎、提的字牌
	 * 
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public int calculate_hong_pai_count(WeaveItem weaveItems[], int weaveCount) {
		int count = 0;
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 3;
				break;
			}
		}
		return count;

	}

	/**
	 * 黑胡队
	 * 
	 * @param weaveItems
	 * @param weaveCount
	 * @return
	 */
	public boolean chunHeiDui(WeaveItem weaveItems[], int weaveCount) {
		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					return false;
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
				return false;
			}
		}
		return true;

	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x11, 0x12, 0x13, 0x14, 0x14, 0x16, 0x16, 0x15,
				0x17, 0x17, 0x17 };
		int[] cards_of_player1 = new int[] { 0x11, 0x13, 0x14, 0x15, 0x11, 0x13, 0x14, 0x15, 0x11, 0x13, 0x14, 0x3, 0x4, 0x5, 0x3, 0x4, 0x5, 0x18,
				0x19, 0x9 };
		int[] cards_of_player2 = new int[] { 0x11, 0x13, 0x14, 0x15, 0x11, 0x13, 0x14, 0x15, 0x11, 0x13, 0x14, 0x3, 0x4, 0x5, 0x3, 0x4, 0x5, 0x18,
				0x19, 0x9 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x12, 0x12, 0x13, 0x13, 0x14, 0x14, 0x15, 0x15, 0x16, 0x16, 0x17, 0x17, 0x18, 0x18, 0x07,
				0x19, 0x19 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (this.getTablePlayerNumber() <= 3) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			}
		} else {
			for (int j = 0; j < 16; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 20) {
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
