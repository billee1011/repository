package com.cai.game.hh.handler.hcphz;

import java.util.ArrayList;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HeChiHHTable extends HHTable {

	public HeChiHHTable() {
		super();
	}

	@Override
	public int getTablePlayerNumber() {

		int countPlayer = 0;
		if (has_rule(GameConstants_HeChi.GAME_RULE_PLAYER_2)) {
			countPlayer = GameConstants_HeChi.GAME_PLAYER_NUMBER_2;
		}
		if (has_rule(GameConstants_HeChi.GAME_RULE_PLAYER_3)) {
			countPlayer = GameConstants_HeChi.GAME_PLAYER_NUMBER_3;
		}
		return countPlayer;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_HeChi();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_HeChi();
		_handler_gang = new PHZHandlerGang_HeChi();
		_handler_chi_peng = new PHZHandlerChiPeng_HeChi();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_HeChi();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_HeChi();
	}

	// 游戏开开始
	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants_HeChi.GS_MJ_PLAY;

		reset_init_data();
		for (int i = 0; i < GameConstants_HeChi.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants_HeChi.INVALID_VALUE;
		}
		GRR._count_niao = 0;
		for (int i = 0; i < getPlayerCount(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants_HeChi.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants_HeChi.INVALID_VALUE;
			}
		}

		// 庄家选择
		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants_HeChi.CARD_COUNT_PHZ_HS];
		shuffle(_repertory_card, GameConstants_HeChi.CARD_PHZ_DEFAULT);


		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants_HeChi.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants_HeChi.MAX_HH_COUNT];
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
			for (int j = 0; j < GameConstants_HeChi.MAX_HH_COUNT; j++) {
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
			roomResponse.setCurrentPlayer(this._current_player == GameConstants_HeChi.INVALID_SEAT ? this._resume_player : this._current_player);
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

			for (int j = 0; j < GameConstants_HeChi.MAX_HH_COUNT; j++) {
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
		this.exe_dispatch_first_card(_current_player, GameConstants_HeChi.WIK_NULL, FlashTime + standTime);

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {

		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants_HeChi.WIK_NULL;
		}
		// 变量定义
		int cbChiHuKind = GameConstants_HeChi.WIK_NULL;
		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants_HeChi.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants_HeChi.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants_HeChi.INVALID_VALUE) {
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
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants_HeChi.WIK_PENG)
						|| (weaveItems[i].weave_kind == GameConstants_HeChi.WIK_WEI))) {

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
								if ((cur_card == analyseItem.cbCenterCard[j]) && (analyseItem.cbWeaveKind[j] == GameConstants_HeChi.WIK_PENG
										|| analyseItem.cbWeaveKind[j] == GameConstants_HeChi.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants_HeChi.WIK_PAO;
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
				sao_WeaveItem.weave_kind = GameConstants_HeChi.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants_HeChi.WIK_NULL) {
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
			return GameConstants_HeChi.WIK_NULL;
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
				if (analyseItem.cbWeaveKind[j] == GameConstants_HeChi.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi < getHuXiMin()) {
			chiHuRight.set_empty();
			return GameConstants_HeChi.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);
		all_cards_count = _logic.calculate_all_pai_count(analyseItem);
		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
		hei_pai_count = _logic.calculate_hei_pai_count(analyseItem);
		int daPaiCount = calculate_da_card_count(analyseItem);
		int xiaoPaiCount = calculate_xiao_card_count(analyseItem);
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants_HeChi.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants_HeChi.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		cbChiHuKind = GameConstants_HeChi.WIK_CHI_HU;
		if ((seat_index == provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_ZI_MO);
		} else if ((seat_index != provider_index) && (dispatch == true)) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_JEI_PAO_HU);
		} else if (dispatch == false) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_DIAN_PAO_HU);
		}
		if (card_type == GameConstants_HeChi.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_TIAN_HU);
		}
		if (card_type == GameConstants_HeChi.OutCard_Type_Di_Hu) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_DI_HU);
		}
		if (hong_pai_count >= 13) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_SHI_SAN_HONG);
		}
		if (daPaiCount >= 18) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_SHI_BA_DA);
		}
		if (daPaiCount >= 18 && hong_pai_count >= 13) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_SHI_BA_AND_SHI_SAN);
		}
		if (hei_pai_count == all_cards_count) {
			chiHuRight.opr_or(GameConstants_HeChi.CHR_ALL_HEI);
		}
		return cbChiHuKind;
	}

	public int estimate_player_ti_wei_respond_phz(int seat_index, int card_data) {
		int bAroseAction = GameConstants_HeChi.WIK_NULL;
		if (_logic.estimate_pao_card_out_card(this.GRR._cards_index[seat_index], card_data) != GameConstants_HeChi.WIK_NULL) {
			this.exe_gang(seat_index, seat_index, card_data, GameConstants_HeChi.WIK_TI_LONG, GameConstants_HeChi.PAO_TYPE_TI_MINE_LONG, true, true,
					false, 1000);
			bAroseAction = GameConstants_HeChi.WIK_TI_LONG;
		}
		if (bAroseAction == GameConstants_HeChi.WIK_NULL) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants_HeChi.WIK_WEI))
					continue;
				this.exe_gang(seat_index, seat_index, card_data, GameConstants_HeChi.WIK_TI_LONG, GameConstants_HeChi.PAO_TYPE_MINE_SAO_LONG, true,
						true, false, 1000);
				bAroseAction = GameConstants_HeChi.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants_HeChi.WIK_NULL)
				&& (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants_HeChi.WIK_NULL)) {
			int action = GameConstants_HeChi.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants_HeChi.WIK_CHOU_WEI;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants_HeChi.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants_HeChi.WIK_WEI;
		}
		return bAroseAction;
	}

	// 玩家出版的动作检测 跑
	public int estimate_player_respond_phz(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
		// 变量定义
		int bAroseAction = GameConstants_HeChi.WIK_NULL;// 出现(是否)有
		pao_type[0] = 0;

		// 碰转跑
		if ((bAroseAction == GameConstants_HeChi.WIK_NULL) && (dispatch == true)) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants_HeChi.WIK_PENG))
					continue;
				pao_type[0] = GameConstants_HeChi.PAO_TYPE_MINE_PENG_PAO;
				bAroseAction = GameConstants_HeChi.WIK_PAO;
			}
		}
		// 跑牌判断
		if ((bAroseAction == GameConstants_HeChi.WIK_NULL) && (seat_index != provider_index)) {
			if (_logic.check_pao(this.GRR._cards_index[seat_index], card_data) != GameConstants_HeChi.WIK_NULL) {
				pao_type[0] = GameConstants_HeChi.PAO_TYPE_OHTER_PAO;
				bAroseAction = GameConstants_HeChi.WIK_PAO;
			}
		}
		// 扫转跑
		if (seat_index != provider_index) {
			for (int weave_index = 0; weave_index < this.GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = this.GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = this.GRR._weave_items[seat_index][weave_index].center_card;
				// 转换判断
				if ((weave_card != card_data) || (weave_kind != GameConstants_HeChi.WIK_WEI))
					continue;
				pao_type[0] = GameConstants_HeChi.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants_HeChi.WIK_PAO;
			}
		}

		//
		return bAroseAction;
	}

	public int getHuXiMin() {
		int huxi = 0;
		if (has_rule(GameConstants_HeChi.GAME_RULE_QI_HU_10)) {
			huxi = 10;
		}
		if (has_rule(GameConstants_HeChi.GAME_RULE_QI_HU_15)) {
			huxi = 15;
		}
		return huxi;
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
		int base = 1;
		if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_3)) {
			base = 3;
		}
		if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_5)) {
			base = 5;
		}
		int wFanShu = get_chi_hu_action_rank_phz(chr);// 番数
		all_hu_xi *= wFanShu;
		int calculate_score = 1 + (all_hu_xi - getHuXiMin()) / base;
		if (seat_index == provide_index) {
			if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_MO_ADD_ZI)) {
				calculate_score += 1;
			} else if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_MO_ADD_FAN)) {
				calculate_score *= 2;
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

		int lChiHuScore = calculate_score;
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

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants_HeChi.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants_HeChi.Game_End_NORMAL || reason == GameConstants_HeChi.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
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

		if (GRR != null) {// reason ==
							// MJGameConstants_HeChi.Game_End_NORMAL ||
							// reason
							// == MJGameConstants_HeChi.Game_End_DRAW ||
			// (reason ==MJGameConstants_HeChi.Game_End_RELEASE_PLAY &&
			// GRR!=null)
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
			game_end.setCellScore(GameConstants_HeChi.CELL_SCORE);

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
				for (int j = 0; j < GameConstants_HeChi.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants_HeChi.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (this.has_rule(GameConstants_HeChi.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants_HeChi.Game_End_DRAW) {
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
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if (_hu_weave_count[i] > 0) {
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
		if (reason == GameConstants_HeChi.Game_End_NORMAL || reason == GameConstants_HeChi.Game_End_DRAW) {
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
		} else if (reason == GameConstants_HeChi.Game_End_RELEASE_PLAY || reason == GameConstants_HeChi.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants_HeChi.Game_End_RELEASE_RESULT || reason == GameConstants_HeChi.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_HeChi.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants_HeChi.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants_HeChi.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 超时解散
		if (reason == GameConstants_HeChi.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants_HeChi.Game_End_RELEASE_WAIT_TIME_OUT) {
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
		huan_dou(reason);
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
			player_result.addYingXiCount(_player_result.ying_xi_count[i]);
			player_result.addLiuZiFen(_player_result.liu_zi_fen[i]);

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
		return player_result;
	}
	//
	// @Override
	// public String get_game_des() {
	// StringBuffer buffer = new StringBuffer();
	//
	// if (has_rule(GameConstants_HeChi.GAME_RULE_PLAYER_2)) {
	// buffer.append(" 2人字牌");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_PLAYER_3)) {
	// buffer.append(" 3人字牌");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_QI_HU_10)) {
	// buffer.append(" 10油起胡");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_QI_HU_15)) {
	// buffer.append(" 15油起胡");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_3)) {
	// buffer.append(" 3油一子");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_5)) {
	// buffer.append(" 5油一子");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_MO_ADD_FAN)) {
	// buffer.append(" 自摸加番");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_MO_ADD_ZI)) {
	// buffer.append(" 自摸加子");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_PAO_HU)) {
	// buffer.append(" 可以点炮");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_ZI_MO)) {
	// buffer.append(" 不能点炮");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_PAO_HU_MORE)) {
	// buffer.append(" 一炮多响");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_XING_SHANG)) {
	// buffer.append(" 上醒");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_XING_XIA)) {
	// buffer.append(" 下醒");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_XING_BEN)) {
	// buffer.append(" 本醒");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_SI_XING_LIAN_FAN)) {
	// buffer.append(" 四醒连番");
	// }
	// if (has_rule(GameConstants_HeChi.GAME_RULE_BI_HU)) {
	// buffer.append(" 有胡必胡");
	// }
	//
	// return buffer.toString();
	// }

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

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants_HeChi.CHR_TIAN_HU) {
						des += ",天胡";
					}
					if (type == GameConstants_HeChi.CHR_ZI_MO) {
						des += ",自摸";
					}
					if (type == GameConstants_HeChi.CHR_JEI_PAO_HU) {
						des += ",接炮胡";
					}
					if (type == GameConstants_HeChi.CHR_SHI_SAN_HONG) {
						des += ",十三胡";
					}
					if (type == GameConstants_HeChi.CHR_SHI_BA_DA) {
						des += ",十八大";
					}
					if (type == GameConstants_HeChi.CHR_SHI_BA_XIAO) {
						des += ",十八小";
					}
					if (type == GameConstants_HeChi.CHR_SHI_BA_AND_SHI_SAN) {
						des += ",十三红+十八大";
					}
					if (type == GameConstants_HeChi.CHR_ALL_HEI) {
						des += ",全黑";
					}
				}
			}

			GRR._result_des[i] = des;
		}
	}

	public int get_chi_hu_action_rank_phz(ChiHuRight chr) {

		if (!(chr.opr_and(GameConstants_HeChi.CHR_SHI_SAN_HONG)).is_empty())
			return 2;
		if (!(chr.opr_and(GameConstants_HeChi.CHR_SHI_BA_DA)).is_empty())
			return 2;
		if (!(chr.opr_and(GameConstants_HeChi.CHR_SHI_BA_XIAO)).is_empty())
			return 2;
		if (!(chr.opr_and(GameConstants_HeChi.CHR_SHI_BA_AND_SHI_SAN)).is_empty())
			return 2;
		if (!(chr.opr_and(GameConstants_HeChi.CHR_ALL_HEI)).is_empty())
			return 2;

		return 1;
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
			case GameConstants_HeChi.WIK_TI_LONG:
			case GameConstants_HeChi.WIK_AN_LONG:
			case GameConstants_HeChi.WIK_PAO:
			case GameConstants_HeChi.WIK_SAO:
			case GameConstants_HeChi.WIK_PENG:
			case GameConstants_HeChi.WIK_CHOU_SAO:
			case GameConstants_HeChi.WIK_KAN:
			case GameConstants_HeChi.WIK_WEI:
			case GameConstants_HeChi.WIK_XIAO:
			case GameConstants_HeChi.WIK_CHOU_XIAO:
			case GameConstants_HeChi.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					return false;
			case GameConstants_HeChi.WIK_XXD:
			case GameConstants_HeChi.WIK_DDX:
				return false;
			}
		}
		return true;

	}

	@Override
	public void test_cards() {
		//// 天胡-三提
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02,
		// 0x02, 0x03, 0x03, 0x03, 0x03, 0x05, 0x17, 0x17, 0x15, 0x19, 0x18,
		// 0x18,
		// 0x16 };

		// 天胡-五坎
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x04, 0x02, 0x02, 0x02,
		// 0x04, 0x03, 0x03, 0x03, 0x04, 0x05, 0x05, 0x05, 0x15, 0x19, 0x18,
		// 0x18,
		// 0x16 };
		int cards[] = new int[] { 0x14, 0x14, 0x14, 0x12, 0x02, 0x02, 0x02, 0x017, 0x03, 0x03, 0x03, 0x1a, 0x05, 0x05, 0x05, 0x15, 0x16, 0x18, 0x18,
				0x08 };
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_HeChi.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < cards.length; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {

				if (is_mj_type(GameConstants_HeChi.GAME_TYPE_WMQ_AX)) {
					if (debug_my_cards.length > 19) {
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
				} else if (is_mj_type(GameConstants_HeChi.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants_HeChi.GAME_TYPE_LHQ_HD)
						|| this.is_mj_type(GameConstants_HeChi.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants_HeChi.GAME_TYPE_LHQ_QD)) {
					if (debug_my_cards.length > 15) {
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
				} else {
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
			_player_result.ying_xi_count[i] += this._hu_xi[_seat_index];
		}
		if (isZimo) {
			_player_result.hu_pai_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_TEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_THIRTEEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_ONE_HONG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_ONE_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_ALL_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(GameConstants_HeChi.CHR_TING_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
	}

	public int get_weave_hu_xi(WeaveItem weave_item) {
		switch (weave_item.weave_kind) {
		case GameConstants.WIK_TI_LONG:
		case GameConstants.WIK_AN_LONG:
		case GameConstants.WIK_AN_LONG_LIANG:
			return (_logic.get_card_color(weave_item.center_card) != 0) ? 12 : 9;
		case GameConstants.WIK_PAO:
			int base = (_logic.get_card_color(weave_item.center_card) != 0) ? 9 : 6;
			if (!_logic.color_hei(weave_item.center_card))
				base += 3;
			return base;
		case GameConstants.WIK_SAO:
		case GameConstants.WIK_CHOU_SAO:
		case GameConstants.WIK_KAN:
		case GameConstants.WIK_WEI:
		case GameConstants.WIK_XIAO:
		case GameConstants.WIK_CHOU_XIAO:
		case GameConstants.WIK_CHOU_WEI:
			return (_logic.get_card_color(weave_item.center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_PENG:
			return (_logic.get_card_color(weave_item.center_card) != 0) ? 3 : 1;
		case GameConstants.WIK_EQS:
		case GameConstants.WIK_YWS:
			return (_logic.get_card_color(weave_item.center_card) != 0) ? 6 : 3;
		case GameConstants.WIK_LEFT: {
			int card_value = _logic.get_card_value(weave_item.center_card);
			if (card_value == 1) {
				return (_logic.get_card_color(weave_item.center_card) != 0) ? 6 : 3;
			}
			break;
		}
		case GameConstants.WIK_CENTER: {
			int card_value = _logic.get_card_value(weave_item.center_card);
			if (card_value == 2) {
				return (_logic.get_card_color(weave_item.center_card) != 0) ? 6 : 3;
			}
			break;
		}
		case GameConstants.WIK_RIGHT: {
			int card_value = _logic.get_card_value(weave_item.center_card);
			if (card_value == 3) {
				return (_logic.get_card_color(weave_item.center_card) != 0) ? 6 : 3;
			}
			break;
		}

		}
		return 0;
	}

	/**
	 * 获取有多少张小牌
	 * 
	 * @param analyseItem
	 * @return
	 */
	public int calculate_xiao_card_count(AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (_logic.get_card_color(analyseItem.cbCenterCard[i]) == 0)
					count += 4;
				continue;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.get_card_color(analyseItem.cbCenterCard[i]) == 0)
					count += 3;
				continue;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_YWS:
				for (int j = 0; j < 3; j++) {
					if (_logic.get_card_color(analyseItem.cbCardData[i][j]) == 0)
						count += 1;
				}
				continue;
			}
		}

		if ((analyseItem.curCardEye != false) && (_logic.get_card_color(analyseItem.cbCardEye) == 0))
			count += 2;
		return count;

	}

	/**
	 * 获取有多少张大牌
	 * 
	 * @param analyseItem
	 * @return
	 */
	public int calculate_da_card_count(AnalyseItem analyseItem) {
		int count = 0;
		for (int i = 0; i < analyseItem.cbWeaveKind.length; i++) {
			switch (analyseItem.cbWeaveKind[i]) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (_logic.get_card_color(analyseItem.cbCenterCard[i]) != 0)
					count += 4;
				continue;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.get_card_color(analyseItem.cbCenterCard[i]) != 0)
					count += 3;
				continue;
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_YWS:
				for (int j = 0; j < 3; j++) {
					if (_logic.get_card_color(analyseItem.cbCardData[i][j]) != 0)
						count += 1;
				}
				continue;
			}
		}

		if ((analyseItem.curCardEye != false) && (_logic.get_card_color(analyseItem.cbCardEye) != 0))
			count += 2;
		return count;

	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
	@Override
	public boolean operate_player_action(int seat_index, boolean close) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_ACTION);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);

		if (close == true) {
			GRR.add_room_response(roomResponse);
			// 通知玩家关闭
			this.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		boolean pass = false;
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			if (has_rule(GameConstants_HeChi.GAME_RULE_BI_HU) && GameConstants_HeChi.WIK_ZI_MO == curPlayerStatus._action[i])
				pass = true;
			if (pass && curPlayerStatus._action[i] == GameConstants_HeChi.WIK_NULL)
				continue;

			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
					| GameConstants.WIK_EQS | GameConstants.WIK_YWS;
			// this.log_error("weave.kind" +
			// curPlayerStatus._action_weaves[i].weave_kind + "center_card"
			// + curPlayerStatus._action_weaves[i].center_card);
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {
							// this.log_error("lou_qi_weave.kind" +
							// curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);

							chi_group.addLouWeaveKind(curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);
						}
					}
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 
	 * @param seat_index
	 * @param card
	 * 
	 * @param show
	 * 
	 *            翻醒
	 */
	@Override
	public void set_niao_card(int seat_index, int card, boolean show) {
		GRR._show_bird_effect = show;

		if (this.GRR._left_card_count == 0) {
			GRR._cards_data_niao[GRR._count_niao] = GRR._chi_hu_card[seat_index][0];
			GRR._count_niao++;
		} else {
			GRR._cards_data_niao[GRR._count_niao] = this._repertory_card[this._all_card_len - this.GRR._left_card_count];
			GRR._count_niao++;
			--this.GRR._left_card_count;
		}

		this.operate_player_get_card(GameConstants_HeChi.INVALID_SEAT, 1, new int[] { GRR._cards_data_niao[GRR._count_niao - 1] },
				GameConstants_HeChi.INVALID_SEAT, false);

		boolean continueXing = false;
		GRR._count_pick_niao = get_xing_pai_count(this._hu_weave_items[seat_index], this._hu_weave_count[seat_index],
				getRealXingPai(GRR._cards_data_niao[GRR._count_niao - 1]), continueXing);
		if (continueXing) {
			set_niao_card(seat_index, GameConstants_HeChi.INVALID_VALUE, show);
		}
	}

	public Integer[] getRealXingPai(int card) {
		List<Integer> list = new ArrayList<Integer>();
		int cardIndex = _logic.switch_to_card_index(card);

		if (has_rule(GameConstants_HeChi.GAME_RULE_XING_XIA)) {
			if (cardIndex == 0 || cardIndex == 10) {
				list.add(_logic.switch_to_card_data(cardIndex + 9));
			} else {
				list.add(_logic.switch_to_card_data(cardIndex - 1));
			}
		}
		if (has_rule(GameConstants_HeChi.GAME_RULE_XING_SHANG)) {
			if (cardIndex == 9 || cardIndex == 19) {
				list.add(_logic.switch_to_card_data(cardIndex - 9));
			} else {
				list.add(_logic.switch_to_card_data(cardIndex + 1));
			}
		}
		if (has_rule(GameConstants_HeChi.GAME_RULE_XING_BEN)) {
			list.add(_logic.switch_to_card_data(cardIndex));
		}
		return list.toArray(new Integer[list.size()]);
	}

	public int get_xing_pai_count(WeaveItem weave_item[], int weave_count, Integer[] cards, boolean continueXing) {
		int allCount = 0;
		for (int card : cards) {
			int count = 0;
			for (int i = 0; i < weave_count; i++) {
				switch (weave_item[i].weave_kind) {
				case GameConstants.WIK_TI_LONG:
				case GameConstants.WIK_AN_LONG:
				case GameConstants.WIK_PAO:
					count += (weave_item[i].center_card == card) ? 4 : 0;
					break;
				case GameConstants.WIK_SAO:
				case GameConstants.WIK_CHOU_SAO:
				case GameConstants.WIK_KAN:
				case GameConstants.WIK_WEI:
				case GameConstants.WIK_XIAO:
				case GameConstants.WIK_CHOU_XIAO:
				case GameConstants.WIK_CHOU_WEI:
				case GameConstants.WIK_PENG:
					count += (weave_item[i].center_card == card) ? 3 : 0;
					break;
				case GameConstants.WIK_EQS: {
					if (weave_item[i].center_card < 16)
						switch (card) {
						case 0x02:
						case 0x07:
						case 0x0a:

							count += 1;
							break;
						}
					if (weave_item[i].center_card > 16)
						switch (card) {
						case 0x12:
						case 0x17:
						case 0x1a:

							count += 1;
							break;
						}
					break;

				}
				case GameConstants.WIK_YWS: {
					if (weave_item[i].center_card < 16)
						switch (card) {
						case 0x01:
						case 0x05:
						case 0x0a:
							count += 1;
							break;
						}
					if (weave_item[i].center_card > 16)
						switch (card) {
						case 0x11:
						case 0x15:
						case 0x1a:
							count += 1;
							break;
						}
					break;
				}

				case GameConstants.WIK_LEFT: {
					if ((weave_item[i].center_card == card) || (weave_item[i].center_card + 1 == card) || (weave_item[i].center_card + 2 == card)) {
						count += 1;
					}
					break;
				}
				case GameConstants.WIK_CENTER: {
					if ((weave_item[i].center_card == card) || (weave_item[i].center_card + 1 == card) || (weave_item[i].center_card - 1 == card)) {
						count += 1;
					}

					break;
				}
				case GameConstants.WIK_RIGHT: {
					if ((weave_item[i].center_card == card) || (weave_item[i].center_card - 1 == card) || (weave_item[i].center_card - 2 == card)) {
						count += 1;
					}

					break;
				}
				case GameConstants.WIK_DDX: {
					if (weave_item[i].center_card > 16) {
						if (weave_item[i].center_card == card)
							count += 2;
						else if (weave_item[i].center_card - 16 == card)
							count += 1;
					} else {
						if (weave_item[i].center_card < 16) {
							if (weave_item[i].center_card == card)
								count += 1;
							else if (weave_item[i].center_card + 16 == card)
								count += 2;
						}
					}
					break;
				}
				case GameConstants.WIK_XXD: {
					if (weave_item[i].center_card > 16) {
						if (weave_item[i].center_card == card)
							count += 1;
						else if (weave_item[i].center_card - 16 == card)
							count += 2;
					} else {
						if (weave_item[i].center_card < 16) {
							if (weave_item[i].center_card == card)
								count += 2;
							else if (weave_item[i].center_card + 16 == card)
								count += 1;
						}
					}
					break;
				}
				case GameConstants.WIK_DUI_ZI: {
					if (weave_item[i].center_card == card)
						count += 2;
				}

				}
			}
			if (has_rule(GameConstants_HeChi.GAME_RULE_SI_XING_LIAN_FAN) && count == 4) {
				continueXing = true;
			}
			allCount += count;
		}
		return allCount;
	}
}
