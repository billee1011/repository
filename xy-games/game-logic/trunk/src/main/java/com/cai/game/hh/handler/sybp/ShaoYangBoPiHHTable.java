package com.cai.game.hh.handler.sybp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.domain.SheduleArgs;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 
 * @author admin
 *
 */

public class ShaoYangBoPiHHTable extends HHTable {

	private static final long serialVersionUID = 1L;

	protected static final int ID_TIMER_REPARE_CARD = 1;// 补牌
	protected static final int ID_TIMER_REPARE_CARD_TO_OUT = 2;

	public boolean canDiHu = false;
	public PHZHandlerOutFirstCardOperate_ShaoYangBoPi _handler_out_first_card_operate;
	public int liuJuCount[];

	public ShaoYangBoPiHHTable() {
		super();
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants.GAME_RULE_SYBP_TWO_PLAYER)) {
			return 2;
		} else {
			return 3;
		}
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_ShaoYangBoPi();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_ShaoYangBoPi();
		_handler_out_first_card_operate = new PHZHandlerOutFirstCardOperate_ShaoYangBoPi();
		_handler_gang = new PHZHandlerGang_ShaoYangBoPi();
		_handler_chi_peng = new PHZHandlerChiPeng_ShaoYangBoPi();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_ShaoYangBoPi();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_ShaoYangBoPi();
		canDiHu = false;
		Arrays.fill(cards_has_wei, 0);
		liuJuCount = new int[this.getTablePlayerNumber()];

		this.setMinPlayerCount(this.getTablePlayerNumber());
	}

	@Override
	public boolean reset_init_data() {
		canDiHu = false;
		Arrays.fill(cards_has_wei, 0);
		return super.reset_init_data();
	}

	@Override
	public void progress_banker_select() {
		if (this._cur_round == 1) {
			_cur_banker = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber();
			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;
		// log_error("gme_status:" + this._game_status);

		reset_init_data();

		// 庄家选择
		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants.CARD_COUNT_PHZ_HS];
		shuffle(_repertory_card, GameConstants.CARD_PHZ_DEFAULT);

		if (has_rule(GameConstants.GAME_RULE_SYBP_TWO_PLAYER)) {
			GRR._left_card_count -= 20;
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

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

		int ti_card_count[] = new int[this.getTablePlayerNumber()];
		int ti_card_index[][] = new int[this.getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = this._logic.get_action_ti_Card(this.GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0) {
			}
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

		int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
			FlashTime = sysParamModel1104.getVal1();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
			standTime = sysParamModel1104.getVal2();
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
										|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI)) {
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								}
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

		if (max_hu_xi < Constants_SHAOYANGBOPI.GAME_QI_HU_XI) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);

		int countGang = 0;
		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
				break;
			}
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
			if (_hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_AN_LONG
					|| _hu_weave_items[seat_index][j].weave_kind == GameConstants.WIK_PAO || _hu_weave_items[seat_index][j].hu_xi == 9
					|| _hu_weave_items[seat_index][j].hu_xi == 12) {
				countGang++;
			}
		}
		if (analyseItem.curCardEye == true && countGang > 0) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		for (int j = 0; j < 7; j++) { // 不足七门方子不能胡牌
			if (_hu_weave_items[seat_index][j].center_card == GameConstants.WIK_NULL) {
				return GameConstants.WIK_NULL;
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if ((seat_index == provider_index) && (dispatch == true) && card_type != Constants_SHAOYANGBOPI.CHR_TIAN_HU) { // 天胡不算自摸
			chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_ZI_MO);
		}

		int hong_count = this._logic.get_hong_count(analyseItem);

		if (this.has_rule(GameConstants.GAME_RULE_SYBP_HONG_HEI_DIAN)) {
			if (hong_count == 1) {
				chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_YI_DIAN_ZHU);
			}
			if (hong_count >= 13) {
				chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_HONG_WU);
			} else if (hong_count >= 10) {
				chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_HONG_HU);
			}
			if (hong_count == 0) {
				chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_HEI_HU);
			}
		}

		if (this.has_rule(GameConstants.GAME_RULE_SYBP_TIAN_DI_HU)) {
			if (card_type == Constants_SHAOYANGBOPI.CHR_DI_HU) {
				chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_DI_HU);
			} else if (card_type == Constants_SHAOYANGBOPI.CHR_TIAN_HU) {
				chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_TIAN_HU);
			}
		}

		chiHuRight.opr_or(Constants_SHAOYANGBOPI.CHR_HU);
		return cbChiHuKind;
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
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI)) {
					continue;
				}
				this.exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false,
						1000);
				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}
		// 扫牌判断
		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(this.GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;
			for (int i = 0; i < this._cannot_peng_count[seat_index]; i++) {
				if (card_data == this._cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
				}
			}
			this.exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);
			bAroseAction = GameConstants.WIK_WEI;
		}
		return bAroseAction;
	}

	// 玩家出牌的动作检测 跑
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
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_PENG)) {
					continue;
				}
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
				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI)) {
					continue;
				}
				pao_type[0] = GameConstants.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}

		}

		//
		return bAroseAction;
	}

	@Override
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += this.GRR._game_score[i];
		}
		if (isZimo) {
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] = _hu_xi[_seat_index];
		}

		if (!(chiHuRight.opr_and(Constants_SHAOYANGBOPI.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		// if
		// (!(chiHuRight.opr_and(Constants_SHAOYANGBOPI.CHR_DI_HU)).is_empty())
		// {
		// _player_result.ming_tang_count[_seat_index]++;
		// }
		if (!(chiHuRight.opr_and(Constants_SHAOYANGBOPI.CHR_ZI_MO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
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
		this._hu_xi[seat_index] = all_hu_xi;
		int calculate_score = all_hu_xi;

		// 天胡或地胡+10
		if (!chr.opr_and(Constants_SHAOYANGBOPI.CHR_TIAN_HU).is_empty() || !chr.opr_and(Constants_SHAOYANGBOPI.CHR_DI_HU).is_empty()) {
			calculate_score += 10;
		}

		if (!chr.opr_and(Constants_SHAOYANGBOPI.CHR_YI_DIAN_ZHU).is_empty() || !chr.opr_and(Constants_SHAOYANGBOPI.CHR_HONG_HU).is_empty()) {
			calculate_score *= 2;
		}

		if (!chr.opr_and(Constants_SHAOYANGBOPI.CHR_HONG_WU).is_empty()) {
			if (calculate_score < 30) {
				calculate_score = 60;
			} else {
				calculate_score *= 2;
			}
		}
		if (!chr.opr_and(Constants_SHAOYANGBOPI.CHR_HEI_HU).is_empty()) {
			if (calculate_score < 30) {
				calculate_score = 60;
			} else {
				calculate_score *= 2;
			}
		}
		// 自摸
		if (zimo && (seat_index == provide_index) && !chr.opr_and(Constants_SHAOYANGBOPI.CHR_ZI_MO).is_empty()) {
			calculate_score += 10;
		}
		// int add = calculate_score % 10; // 代理要求总结算再四舍五入
		// calculate_score -= add;
		// calculate_score += add >= 5 ? 10 : 0;

		GRR._game_score[seat_index] += calculate_score;
	}

	/**
	 * 出第一张牌
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean exe_out_first_card(int seat_index, int card, int type) {
		// 出牌
		this._handler = this._handler_out_first_card_operate;
		this._handler_out_first_card_operate.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public void exe_repairCard(int seat_index, int out_seat_index, int card, int type) {
		_handler_out_first_card_operate.repairCard(this, false, seat_index);
		operate_player_get_card(seat_index, 0, null, GameConstants.INVALID_SEAT, true);
		if ((seat_index + 1) % getTablePlayerNumber() == out_seat_index) {
			// 特效定时器
			SheduleArgs nimal_one = SheduleArgs.newArgs();
			nimal_one.set("seat_index", seat_index);
			nimal_one.set("out_seat_index", out_seat_index);
			nimal_one.set("card", card);
			nimal_one.set("type", type);
			schedule(ID_TIMER_REPARE_CARD_TO_OUT, nimal_one, 500);

		} else {
			int next_player = (seat_index + 1) % getTablePlayerNumber();
			if (next_player != out_seat_index) {
				// 特效定时器
				SheduleArgs nimal_one = SheduleArgs.newArgs();
				nimal_one.set("seat_index", next_player);
				nimal_one.set("out_seat_index", out_seat_index);
				nimal_one.set("card", card);
				nimal_one.set("type", type);
				schedule(ID_TIMER_REPARE_CARD, nimal_one, 500);
			}
		}
	}

	/**
	 * 调度回调
	 */
	@Override
	protected void timerCallBack(SheduleArgs args) {
		switch (args.getTimerId()) {
		case ID_TIMER_REPARE_CARD: {
			int seat_index = args.get("seat_index");
			int out_seat_index = args.get("out_seat_index");
			int card = args.get("card");
			int type = args.get("type");
			exe_repairCard(seat_index, out_seat_index, card, type);
			return;
		}
		case ID_TIMER_REPARE_CARD_TO_OUT: {
			int out_seat_index = args.get("out_seat_index");
			int card = args.get("card");
			int type = args.get("type");
			exe_out_first_card(out_seat_index, card, type);
			return;
		}
		}
	}

	/**
	 * 每一轮结束后计算最后得分
	 * 
	 * @param score
	 * @param isDouble
	 * @return
	 */
	public int calculate_score(int score, boolean isDouble) {
		return score;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
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
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
				game_end.addPao(this.liuJuCount[i]); // 流局次数
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			if (reason == GameConstants.Game_End_DRAW) {
				this._huang_zhang_count++;
			} else {
				this._huang_zhang_count = 0;
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if (_hu_weave_count[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
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
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			// if (_cur_round >= _game_round) { // 局数到了
			// end = true;
			// game_end.setRoomOverType(1);
			// game_end.setPlayerResult(this.process_player_result(reason));
			// } else {
			// // 确定下局庄家
			// // 以后谁胡牌，下局谁做庄。
			// // 流局,庄家下一个
			// // 通炮,放炮的玩家
			// }
			if (reason == GameConstants.Game_End_NORMAL) {
				if ((GRR != null && GRR._banker_player != seat_index) || !has_rule(GameConstants.GAME_RULE_SYBP_LIAN_ZHUANG)) { // 如果庄家胡牌则继续
					// 否则满100分结束游戏
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						if (_player_result.game_score[i] >= 100) {
							end = true;
						}
					}
				} else {
					// 否则满100分结束游戏
					int max_limit_score = 10000000;
					if (this.has_rule(GameConstants.GAME_RULE_SYBP_LIMIT_100)) {
						max_limit_score = 100;
					} else if (this.has_rule(GameConstants.GAME_RULE_SYBP_LIMIT_200)) {
						max_limit_score = 200;
					} else if (this.has_rule(GameConstants.GAME_RULE_SYBP_LIMIT_300)) {
						max_limit_score = 300;
					}
					for (int i = 0; i < getTablePlayerNumber(); i++) {

						if (_player_result.game_score[i] >= max_limit_score) {
							end = true;
						}
					}
				}
				if (end) { // 游戏结束
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						_player_result.ying_xi_count[i] = (int) _player_result.game_score[i];
						int add = (int) (_player_result.game_score[i] % 10); // 代理要求总结算再四舍五入
						_player_result.game_score[i] -= add;
						_player_result.game_score[i] += add >= 5 ? 10 : 0;
					}
					float player[] = new float[3];
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						int pre = (i + 1 + getTablePlayerNumber()) % getTablePlayerNumber();
						int next = (i + 2 + getTablePlayerNumber()) % getTablePlayerNumber();
						float temp = _player_result.game_score[i];
						player[i] = (temp - _player_result.game_score[pre]) + (temp - _player_result.game_score[next]);
					}
					for (int i = 0; i < getTablePlayerNumber(); i++) {
						_player_result.game_score[i] = player[i]; // 十胡息一分
					}

					game_end.setRoomOverType(1);
					game_end.setPlayerResult(this.process_player_result(reason));
				}
			} else if (reason == GameConstants.Game_End_DRAW) { // 流局 庄家-10分
				game_end.setGameScore(seat_index, -10);
				_cur_banker = seat_index;
				_player_result.game_score[seat_index] -= 10;
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 流局
			float player[] = new float[3];
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.ying_xi_count[i] = (int) _player_result.game_score[i];
				int add = (int) (_player_result.game_score[i] % 10); // 代理要求总结算再四舍五入
				_player_result.game_score[i] -= add;
				_player_result.game_score[i] += add >= 5 ? 10 : 0;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				int pre = (i + 1 + getTablePlayerNumber()) % getTablePlayerNumber();
				int next = (i + 2 + getTablePlayerNumber()) % getTablePlayerNumber();
				float temp = _player_result.game_score[i];
				player[i] = (temp - _player_result.game_score[pre]) + (temp - _player_result.game_score[next]);
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.game_score[i] = player[i]; // 十胡息一分
			}
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

		if (end) { // 删除
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
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
			StringBuffer des = new StringBuffer();

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == Constants_SHAOYANGBOPI.CHR_TIAN_HU) {
						des.append(",天胡+10");
					}
					if (type == Constants_SHAOYANGBOPI.CHR_DI_HU) {
						des.append(",地胡+10");
					}
					if (type == Constants_SHAOYANGBOPI.CHR_ZI_MO) {
						des.append(",自摸+10");
					}
					if (type == Constants_SHAOYANGBOPI.CHR_HONG_HU) {
						des.append(",红胡");
					}
					if (type == Constants_SHAOYANGBOPI.CHR_HEI_HU) {
						des.append(",黑胡");
					}
					if (type == Constants_SHAOYANGBOPI.CHR_HONG_WU) {
						des.append(",红乌");
					}
					if (type == Constants_SHAOYANGBOPI.CHR_YI_DIAN_ZHU) {
						des.append(",一点朱");
					}
				}
			}

			GRR._result_des[i] = des.toString();
		}
	}

	public boolean is_card_has_wei(int card) {
		boolean bTmp = false;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (this.cards_has_wei[i] != 0) {
				if (i == this._logic.switch_to_card_index(card)) {
					bTmp = true;
					break;
				}
			}
		}

		return bTmp;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
		}

		return card;
	}

	// 是否听牌
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
				return true;
		}
		return false;
	}

	@Override
	public void test_cards() {
		// 天胡-三提
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02,
		// 0x02, 0x03, 0x03, 0x03, 0x03, 0x05, 0x17, 0x17, 0x15, 0x19, 0x18,
		// 0x18, 0x16 };

		// 天胡-五坎
		// int cards[] = new int[] { 0x01, 0x01, 0x01, 0x04, 0x02, 0x02, 0x02,
		// 0x04, 0x03, 0x03, 0x03, 0x04, 0x05, 0x05, 0x05, 0x15, 0x19, 0x18,
		// 0x18, 0x16 };
		// int cards[] = new int[] { 0x14, 0x14, 0x14, 0x11, 0x01, 0x01, 0x01,
		// 0x11, 0x06, 0x06, 0x16, 0x11, 0x03, 0x03, 0x03, 0x18, 0x18, 0x08,
		// 0x18, 0x08 };
		// int cards[][] = new int[][] {
		// { 0x11, 0x11, 0x13, 0x13, 0x15, 0x15, 0x14, 0x14, 0x01, 0x01, 0x03,
		// 0x03, 0x04, 0x04, 0x06, 0x06, 0x08,
		// 0x08, 0x09, 0x09, },
		// { 0x11, 0x11, 0x13, 0x13, 0x15, 0x15, 0x14, 0x14, 0x01, 0x01, 0x03,
		// 0x03, 0x04, 0x04, 0x06, 0x06, 0x08,
		// 0x08, 0x09, 0x09, },
		// { 0x18, 0x18, 0x18, 0x11, 0x11, 0x1, 0x3, 0x3, 0x4, 0x4, 0x5, 0x8,
		// 0x9, 0x9, 0x2, 0x7, 0xa, 0x12, 0x17,
		// 0x1a, } };

		int cards[] = new int[] { 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0x1, 0x2, 0x11,
				0x12, 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0x1, 0x2, 0x11, 0x12, 0x11, 0x12, 0x11,
				0x12, 0x11, 0x12, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0x1, 0x2, 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x1,
				0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8, 0x9, 0xa, 0x1, 0x2, 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x11, 0x12, 0x1, 0x2, 0x3, 0x4, 0x5, 0x6,
				0x7, 0x8, 0x9, 0xa, 0x1, 0x2, };
		int index = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < (GameConstants.MAX_HH_COUNT - 1); j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[index++])] += 1;
			}
		}
		_repertory_card = cards;
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {

				if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX)) {
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
				} else if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
						|| this.is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || this.is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)) {
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
}
