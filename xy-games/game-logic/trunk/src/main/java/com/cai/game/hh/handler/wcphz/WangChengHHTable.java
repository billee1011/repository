package com.cai.game.hh.handler.wcphz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_WangCheng;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class WangChengHHTable extends HHTable {

	private int[][] laizi_operate = new int[4][2];

	public int[] mian_zhang;

	public int[] huo_piao;

	public PHZHandlerPiao_WangCheng _handler_piao;

	public WangChengHHTable() {
		super();
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_3))
			return 3;
		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_2))
			return 2;
		return 4;
	}

	// @Override
	// public String get_game_des() {
	// StringBuilder gameDesc = new StringBuilder("");
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_3)) {
	// gameDesc.append("三人玩法");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_4)) {
	// gameDesc.append("四人玩法");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_CHOU_ZHUANG)) {
	// gameDesc.append("臭牌臭庄");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_NON_CHOU_ZHUANG)) {
	// gameDesc.append("臭牌不臭庄");
	// }
	// int rule = getRuleValue(7);
	// if (rule != 0) {
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_0) {
	// gameDesc.append("30胡以上见1加0");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_1) {
	// gameDesc.append("30胡以上见1加1");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_2) {
	// gameDesc.append("30胡以上见1加2");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_3) {
	// gameDesc.append("30胡以上见1加3");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_4) {
	// gameDesc.append("30胡以上见1加4");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_5) {
	// gameDesc.append("30胡以上见1加5");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_6) {
	// gameDesc.append("30胡以上见1加6");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_7) {
	// gameDesc.append("30胡以上见1加7");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_8) {
	// gameDesc.append("30胡以上见1加8");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_9) {
	// gameDesc.append("30胡以上见1加9");
	// }
	// if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_10) {
	// gameDesc.append("30胡以上见1加10");
	// }
	// } else {
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_0)) {
	// gameDesc.append("30胡以上见1加0");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_1)) {
	// gameDesc.append("30胡以上见1加1");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_2)) {
	// gameDesc.append("30胡以上见1加2");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_3)) {
	// gameDesc.append("30胡以上见1加3");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_4)) {
	// gameDesc.append("30胡以上见1加4");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_5)) {
	// gameDesc.append("30胡以上见1加5");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_6)) {
	// gameDesc.append("30胡以上见1加6");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_7)) {
	// gameDesc.append("30胡以上见1加7");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_8)) {
	// gameDesc.append("30胡以上见1加8");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_9)) {
	// gameDesc.append("30胡以上见1加9");
	// }
	// if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_10)) {
	// gameDesc.append("30胡以上见1加10");
	// }
	// }
	// return gameDesc.toString();
	// }

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {

		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_WangCheng();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_WangCheng();
		_handler_gang = new PHZHandlerGang_WangCheng();
		_handler_chi_peng = new PHZHandlerChiPeng_WangCheng();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_WangCheng();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_WangCheng();
		_handler_piao = new PHZHandlerPiao_WangCheng();
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		// _logic.random_card_data(repertory_card, mj_cards);
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);
		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);
			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;
		int count = this.getTablePlayerNumber();

		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_2)) {
			GRR._left_card_count -= 19;
		}

		for (int i = 0; i < count; i++) {
			if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_4)) {
				send_count = GameConstants.MAX_FPHZ_COUNT + 1;
			} else {
				send_count = GameConstants.MAX_HH_COUNT - 2;
			}

			GRR._left_card_count -= send_count;
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	public void progress_banker_select() {
		if (_cur_round == 1) {
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());// 创建者的玩家为专家

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	public void on_game_start_real() {
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

		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// 检测听牌
		PerformanceTimer timer = new PerformanceTimer();
		for (int i = 0; i < playerCount; i++) {
			_playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		// }
		// }).start();

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, FlashTime + standTime);

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

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;
		log_info("gme_status:" + this._game_status);

		reset_init_data();

		mian_zhang = new int[getTablePlayerNumber()];
		huo_piao = new int[getTablePlayerNumber()];
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_player_result.pao[p] = 0;
		}
		if (has_rule(GameConstants_WangCheng.GAME_RULE_HUO_PIAO)) {
			_handler = _handler_piao;
			this._handler_piao.exe(this);
			return true;
		} else {
			on_game_start_real();
		}
		return true;

	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	/**
	 * 听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @returnt
	 */
	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {

		// 复制数据
		PerformanceTimer timer = new PerformanceTimer();
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (this._logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, false)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}
		if (timer.get() > 1000) {
			this.log_error("pao huzi  ting card cost time = " + timer.duration() + "and cards is =" + Arrays.toString(cbCardIndexTemp));
		}
		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public int analyse_chi_hu_card_sixteen(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index, int provider_index,
			int cur_card, ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (this._is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		_hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;

		boolean bValue = _logic.analyse_card_WC(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card)
						&& ((weaveItems[i].weave_kind == GameConstants.WIK_PENG) || (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_WC(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
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

			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			boolean flag = false;
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
				flag = true;

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = false;
				if (flag)
					temp_bValue = _logic.analyse_card_WC(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
						sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								if (analyseItem.invisibleMagicCard == 0) {
									sao_WeaveItem.weave_kind = GameConstants.WIK_TI_LONG;
									sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);
									analyseItem.invisibleMagicCard = sao_WeaveItem.center_card;
									analyseItem.invisibleMagicCardKind = sao_WeaveItem.weave_kind;
								}
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

		for (int i = 0; i < analyseItemArray.size(); i++) {
			AnalyseItem analyseItem = analyseItemArray.get(i);
			if (analyseItem.invisibleMagicCard == 0 && analyseItem.curCardEye == true) {
				analyseItem.curCardEye = false;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCardEye;
				if (cur_card != weave_items.center_card || cards_index[_logic.switch_to_card_index(cur_card)] == 2) {
					weave_items.weave_kind = GameConstants.WIK_KAN;
				} else if (seat_index == provider_index) {
					weave_items.weave_kind = GameConstants.WIK_WEI;
				} else {
					weave_items.weave_kind = GameConstants.WIK_PENG;
				}
				analyseItem.cbCenterCard[6] = weave_items.center_card;
				analyseItem.cbWeaveKind[6] = weave_items.weave_kind;
				analyseItem.hu_xi[6] = _logic.get_weave_hu_xi(weave_items);

				analyseItem.invisibleMagicCard = analyseItem.cbCenterCard[6];
				analyseItem.invisibleMagicCardKind = analyseItem.cbWeaveKind[6];
			}
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
					continue;
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

		if (card_type != GameConstants_WangCheng.HU_CARD_SHE_PAO && max_hu_xi < tokenBaseHuXi()) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		boolean magic_located = false;

		if (dispatch) {
			try {
				for (int j = 0; j < 7; j++) {
					if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
						continue;
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
			} catch (Exception e) {
				// 此处有一BUG无法重现，需在此处加一try保持牌局正常，出现问题时能够更好的捕获牌局信息以便解决
				e.printStackTrace();
			}

			laizi_operate[seat_index][0] = analyseItem.invisibleMagicCard;
			laizi_operate[seat_index][1] = analyseItem.invisibleMagicCardKind;
		}
		cbChiHuKind = GameConstants.WIK_CHI_HU;

		chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

		return cbChiHuKind;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {

		if (true)
			return analyse_chi_hu_card_sixteen(cards_index, weaveItems, weaveCount, seat_index, provider_index, cur_card, chiHuRight, card_type,
					hu_xi_hh, dispatch);

		@SuppressWarnings("unused")
		int cbChiHuKind = GameConstants.WIK_NULL;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

		int cardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cardIndexTemp[i] = cards_index[i];
		}

		boolean[] has_four_card = new boolean[GameConstants.MAX_HH_INDEX]; // 用来处理手牌里有四张一样的牌时的胡牌分析情况

		int real_max_hu_xi = 0;
		AnalyseItem real_analyse_item = new AnalyseItem();

		// PerformanceTimer timer = new PerformanceTimer();

		for (int c = 0; c < GameConstants.MAX_HH_INDEX; c++) {
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				cbCardIndexTemp[i] = cards_index[i];
			}

			if (cur_card != GameConstants.INVALID_VALUE) {
				int index = _logic.switch_to_card_index(cur_card);
				cbCardIndexTemp[index]++;
			}

			cbCardIndexTemp[c]++;

			int tmp_weave_count = weaveCount;
			WeaveItem[] tmp_weave_items = new WeaveItem[tmp_weave_count + 1];

			for (int i = 0; i < tmp_weave_count + 1; i++) {
				tmp_weave_items[i] = new WeaveItem();
			}

			for (int w = 0; w < tmp_weave_count; w++) {
				tmp_weave_items[w] = weaveItems[w];
			}

			// 把王牌加进来之后，如果牌刚好有4张，有两种情况可以胡：1. 当成提；2. 把坎落下来，最后一张牌当成其他牌；

			// 如果有提，暂时把提落下来
			if (cbCardIndexTemp[c] == 4) {
				if (has_four_card[c] == false) { // 当成提
					has_four_card[c] = true;
					tmp_weave_items[tmp_weave_count].public_card = 1;
					tmp_weave_items[tmp_weave_count].center_card = _logic.switch_to_card_data(c);
					if (cur_card == _logic.switch_to_card_data(c)) {
						tmp_weave_items[tmp_weave_count].weave_kind = GameConstants.WIK_PAO;
					} else {
						tmp_weave_items[tmp_weave_count].weave_kind = GameConstants.WIK_TI_LONG;
					}
					tmp_weave_items[tmp_weave_count].provide_player = seat_index;
					tmp_weave_items[tmp_weave_count].hu_xi = _logic.get_weave_hu_xi(tmp_weave_items[tmp_weave_count]);
					tmp_weave_count++;
					cbCardIndexTemp[c] = 0;
				} else if (has_four_card[c] == true) { // 当成坎
					tmp_weave_items[tmp_weave_count].public_card = 1;
					tmp_weave_items[tmp_weave_count].center_card = _logic.switch_to_card_data(c);
					tmp_weave_items[tmp_weave_count].weave_kind = GameConstants.WIK_KAN;
					tmp_weave_items[tmp_weave_count].provide_player = seat_index;
					tmp_weave_items[tmp_weave_count].hu_xi = _logic.get_weave_hu_xi(tmp_weave_items[tmp_weave_count]);
					tmp_weave_count++;
					cbCardIndexTemp[c] = 1;
					has_four_card[c] = false;
				}
			}

			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

			_hu_xi[seat_index] = 0;
			int hu_xi[] = new int[1];
			hu_xi[0] = 0;

			boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, tmp_weave_items, tmp_weave_count, seat_index, provider_index, cur_card,
					analyseItemArray, false, hu_xi, false);

			// System.out.println("第" + c + "轮耗时:" + timer.getStr());

			if (cur_card != 0) {
				for (int i = 0; i < tmp_weave_count; i++) {
					if ((cur_card == tmp_weave_items[i].center_card) && ((tmp_weave_items[i].weave_kind == GameConstants.WIK_PENG)
							|| (tmp_weave_items[i].weave_kind == GameConstants.WIK_WEI))) {

						int index = _logic.switch_to_card_index(cur_card);
						cbCardIndexTemp[index]--;
						int temp_index = analyseItemArray.size();

						boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, tmp_weave_items, tmp_weave_count, seat_index, provider_index,
								cur_card, analyseItemArray, false, hu_xi, false);

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

				WeaveItem sao_WeaveItem = new WeaveItem();
				int cur_index = _logic.switch_to_card_index(cur_card);
				if (cards_index[cur_index] == 3) {
					cbCardIndexTemp[cur_index] = 1;
					sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
					sao_WeaveItem.center_card = cur_card;
					sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				}
				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, tmp_weave_items, tmp_weave_count, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, false);
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

			if (!bValue) {
				bValue = analyse_card_2_7_10(cbCardIndexTemp, c, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
						hu_xi);
			}

			if (!bValue) {
				if (has_four_card[c] == true) {
					c--;
				}
				continue;
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

			if (max_hu_xi < tokenBaseHuXi()) {
				if (has_four_card[c] == true) {
					c--;
				}
				continue;
			}

			if (max_hu_xi >= real_max_hu_xi) {
				real_max_hu_xi = max_hu_xi;
				real_analyse_item = analyseItemArray.get(max_hu_index);

				for (int n = 0; n < 7; n++) {
					int kind = real_analyse_item.cbWeaveKind[n];
					int centerCard = real_analyse_item.cbCenterCard[n];

					boolean isWeave = false;
					for (int w = 0; w < weaveCount; w++)
						if (weaveItems[w].weave_kind == kind && weaveItems[w].getCenter_card() == centerCard) {
							isWeave = true;
							break;
						}

					if (isWeave)
						continue;

					int[] realCard = new int[4];
					_logic.get_weave_card(kind, centerCard, realCard);
					for (int m = 0; m < realCard.length; m++)
						if (_logic.switch_to_card_data(c) == realCard[m]) {
							this.laizi_operate[seat_index][0] = c;
							this.laizi_operate[seat_index][1] = kind;
						}
				}

				if (real_analyse_item.curCardEye == true)
					if (real_analyse_item.cbCardEye == _logic.switch_to_card_data(c)) {
						this.laizi_operate[seat_index][0] = c;
						this.laizi_operate[seat_index][1] = GameConstants.WIK_DUI_ZI;
					}

			}

			if (has_four_card[c] == true) {
				c--;
			}
		}

		// int[] laizi_operate = new int[2];
		// WeaveItem hu_weave_items[][] = new
		// WeaveItem[this.getTablePlayerNumber()][7];
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// for (int j = 0; j < 7; j++) {
		// hu_weave_items[i][j] = new WeaveItem();
		// }
		// }
		// int hu_weave_count[] = new int[this.getTablePlayerNumber()];
		// int tmp_weave_count = weaveCount;
		// WeaveItem[] tmp_weave_items = new WeaveItem[tmp_weave_count];
		// for (int w = 0; w < tmp_weave_count; w++) {
		// tmp_weave_items[w] = weaveItems[w];
		// }
		// int tempHUxi = analyse_chi_hu_card_lai2wa(cardIndexTemp,
		// tmp_weave_items, tmp_weave_count, seat_index, provider_index,
		// cur_card, chiHuRight,
		// card_type, hu_xi_hh, dispatch, hu_weave_items, hu_weave_count,
		// laizi_operate);
		// if (tempHUxi > real_max_hu_xi) {
		// hu_xi_hh[0] = tempHUxi;
		// this.laizi_operate[0] = laizi_operate[0];
		// this.laizi_operate[1] = laizi_operate[1];
		// for (int j = 0; j < 7; j++) {
		// _hu_weave_items[seat_index][j] = hu_weave_items[seat_index][j];
		// _hu_weave_count[seat_index] = j + 1;
		// }
		// cbChiHuKind = GameConstants.WIK_CHI_HU;
		// chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		// return cbChiHuKind;
		// }

		if (real_max_hu_xi >= tokenBaseHuXi()) {
			hu_xi_hh[0] = real_max_hu_xi;

			for (int j = 0; j < 7; j++) {
				if (real_analyse_item.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				_hu_weave_items[seat_index][j].weave_kind = real_analyse_item.cbWeaveKind[j];
				_hu_weave_items[seat_index][j].center_card = real_analyse_item.cbCenterCard[j];
				_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
				_hu_weave_count[seat_index] = j + 1;
			}

			if (real_analyse_item.curCardEye == true) {
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = real_analyse_item.cbCardEye;
				_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
				_hu_weave_count[seat_index]++;
			}

			cbChiHuKind = GameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);

			return cbChiHuKind;
		} else {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}
	}

	private boolean analyse_card_2_7_10(int[] cards_index, int laizi_card, WeaveItem[] weaveItems, int weaveCount, int seatIndex, int provider_index,
			int cur_card, List<AnalyseItem> analyseItemArray, int hu_xi[]) {
		int[] cbCardBuffer = new int[3];
		int cur_card_value = _logic.get_card_value(_logic.switch_to_card_data(laizi_card));
		boolean is_2_7_10 = false;
		switch (cur_card_value) {
		case 2: {
			cbCardBuffer[0] = laizi_card;
			cbCardBuffer[1] = laizi_card + 5;
			cbCardBuffer[2] = laizi_card + 8;
			is_2_7_10 = true;
			break;
		}
		case 7: {
			cbCardBuffer[0] = laizi_card;
			cbCardBuffer[1] = laizi_card - 5;
			cbCardBuffer[2] = laizi_card + 3;
			is_2_7_10 = true;
			break;
		}
		case 10: {
			cbCardBuffer[0] = laizi_card;
			cbCardBuffer[1] = laizi_card - 8;
			cbCardBuffer[2] = laizi_card - 3;
			is_2_7_10 = true;
			break;
		}
		}

		if (!is_2_7_10)
			return false;

		int cardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cardIndexTemp[i] = cards_index[i];
		}

		if (cardIndexTemp[cbCardBuffer[0]] == 0)
			return false;
		if (cardIndexTemp[cbCardBuffer[1]] == 0 || cardIndexTemp[cbCardBuffer[1]] >= 3)
			return false;
		if (cardIndexTemp[cbCardBuffer[2]] == 0 || cardIndexTemp[cbCardBuffer[2]] >= 3)
			return false;
		cardIndexTemp[cbCardBuffer[0]]--;
		cardIndexTemp[cbCardBuffer[1]]--;
		cardIndexTemp[cbCardBuffer[2]]--;

		int tmp_weave_count = weaveCount;
		WeaveItem[] tmp_weave_items = new WeaveItem[tmp_weave_count + 1];
		for (int i = 0; i < tmp_weave_count + 1; i++) {
			tmp_weave_items[i] = new WeaveItem();
		}
		for (int w = 0; w < tmp_weave_count; w++) {
			tmp_weave_items[w] = weaveItems[w];
		}

		WeaveItem weaveItem_2_7_10 = new WeaveItem();
		weaveItem_2_7_10.center_card = _logic.switch_to_card_data(laizi_card);
		weaveItem_2_7_10.weave_kind = GameConstants.WIK_EQS;
		weaveItem_2_7_10.provide_player = seatIndex;
		weaveItem_2_7_10.hu_xi = _logic.get_weave_hu_xi(weaveItem_2_7_10);
		tmp_weave_items[tmp_weave_count++] = weaveItem_2_7_10;

		boolean bValue = _logic.analyse_card_phz(cardIndexTemp, tmp_weave_items, tmp_weave_count, seatIndex, provider_index, cur_card,
				analyseItemArray, false, hu_xi, false);

		if (cur_card != 0) {
			for (int i = 0; i < tmp_weave_count; i++) {
				if ((cur_card == tmp_weave_items[i].center_card)
						&& ((tmp_weave_items[i].weave_kind == GameConstants.WIK_PENG) || (tmp_weave_items[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					if (cardIndexTemp[index] == 0)
						break;
					cardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cardIndexTemp, tmp_weave_items, tmp_weave_count, seatIndex, provider_index,
							cur_card, analyseItemArray, false, hu_xi, false);

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

			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cardIndexTemp[cur_index] == 3) {
				cardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cardIndexTemp, tmp_weave_items, tmp_weave_count, seatIndex, provider_index, cur_card,
						analyseItemArray, false, hu_xi, false);
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

		return bValue;
	}

	public int analyse_chi_hu_card_lai2wa(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,

			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch, WeaveItem _hu_weave_items[][], int _hu_weave_count[],
			int[] laizi_operate) {
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

		WeaveItem[] weaveItemsTemp = new WeaveItem[weaveCount];
		for (int i = 0; i < weaveCount; i++) {
			weaveItemsTemp[i] = weaveItems[i];
		}
		AnalyseItem real_analyse_item = new AnalyseItem();
		int real_max_hu_xi = 0;

		int lai_kind = -1;
		int card = -1;
		for (int n = 0; n < weaveCount; n++) {
			int kind = weaveItemsTemp[n].weave_kind;
			if (kind == GameConstants.WIK_WEI || kind == GameConstants.WIK_CHOU_WEI) {
				weaveItemsTemp[n].weave_kind = GameConstants.WIK_TI_LONG;
			} else if (kind == GameConstants.WIK_PENG) {
				weaveItemsTemp[n].weave_kind = GameConstants.WIK_PAO;
			} else {
				continue;
			}

			List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
			// 分析扑克
			this._hu_xi[seat_index] = 0;
			int hu_xi[] = new int[1];
			hu_xi[0] = 0;
			boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItemsTemp, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
					false, hu_xi);

			if (cur_card != 0) {
				for (int i = 0; i < weaveCount; i++) {
					if ((cur_card == weaveItemsTemp[i].center_card) && ((weaveItemsTemp[i].weave_kind == GameConstants.WIK_PENG)
							|| (weaveItemsTemp[i].weave_kind == GameConstants.WIK_WEI))) {

						int index = _logic.switch_to_card_index(cur_card);
						cbCardIndexTemp[index]--;
						int temp_index = analyseItemArray.size();

						boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItemsTemp, weaveCount, seat_index, provider_index, cur_card,
								analyseItemArray, false, hu_xi);
						cbCardIndexTemp[index]++;

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
					boolean temp_bValue = _logic.analyse_card(cbCardIndexTemp, weaveItemsTemp, weaveCount, seat_index, provider_index, cur_card,
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

			weaveItemsTemp[n].weave_kind = kind;

			if (!bValue) {
				chiHuRight.set_empty();
				continue;
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

			if (max_hu_xi < tokenBaseHuXi()) {
				continue;
			}

			if (real_max_hu_xi < max_hu_xi) {
				real_max_hu_xi = max_hu_xi;
				real_analyse_item = analyseItemArray.get(max_hu_index);
				card = weaveItemsTemp[n].center_card;
				lai_kind = kind;
			}
		}

		if (real_max_hu_xi < tokenBaseHuXi()) {
			return -1;
		}

		if (dispatch)
			for (int i = 0; i < weaveCount; i++) {
				if (weaveItems[i].center_card != card)
					continue;
				if (weaveItems[i].weave_kind != lai_kind)
					continue;

				if (lai_kind == GameConstants.WIK_WEI || lai_kind == GameConstants.WIK_CHOU_WEI) {
					weaveItems[i].weave_kind = GameConstants.WIK_TI_LONG;
					laizi_operate[0] = _logic.switch_to_card_index(weaveItems[i].center_card);
					laizi_operate[1] = weaveItems[i].weave_kind;
				} else if (lai_kind == GameConstants.WIK_PENG) {
					weaveItems[i].weave_kind = GameConstants.WIK_PAO;
					laizi_operate[0] = _logic.switch_to_card_index(weaveItems[i].center_card);
					laizi_operate[1] = weaveItems[i].weave_kind;
				}
			}

		for (int j = 0; j < 7; j++) {
			if (real_analyse_item.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = real_analyse_item.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = real_analyse_item.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;

		}
		if (real_analyse_item.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = real_analyse_item.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}
		return real_max_hu_xi;
	}

	private int tokenBaseHuXi() {
		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_3))
			return 21;
		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_2))
			return 21;
		return 15;
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
					GameConstants_WangCheng.HU_CARD_SHE_PAO, hu_xi_chi, false))
				return true;
		}
		return false;
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

		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_4))
			all_hu_xi += 6;
		int tun = 0;
		if (all_hu_xi >= 30) {
			int baseJian = 0;
			int rule = getRuleValue(7);
			if (rule != 0) {
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_0)
					baseJian = 0;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_1)
					baseJian = 1;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_2)
					baseJian = 2;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_3)
					baseJian = 3;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_4)
					baseJian = 4;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_5)
					baseJian = 5;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_6)
					baseJian = 6;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_7)
					baseJian = 7;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_8)
					baseJian = 8;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_9)
					baseJian = 9;
				if (rule == GameConstants_WangCheng.GAME_RULE_DE_FEN_10)
					baseJian = 10;
			} else {
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_0))
					baseJian = 0;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_1))
					baseJian = 1;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_2))
					baseJian = 2;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_3))
					baseJian = 3;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_4))
					baseJian = 4;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_5))
					baseJian = 5;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_6))
					baseJian = 6;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_7))
					baseJian = 7;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_8))
					baseJian = 8;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_9))
					baseJian = 9;
				if (has_rule(GameConstants_WangCheng.GAME_RULE_DE_FEN_10))
					baseJian = 10;
			}

			int base = 10;
			if (has_rule(GameConstants_WangCheng.GAME_RULE_30_HU_15_PAI))
				base = 15;

			tun = base + (all_hu_xi - 30) * baseJian;
		} else {
			tun = 1 + (all_hu_xi - 21) / tunJiShu;
		}

		int wFanShu = get_chi_hu_action_rank_phz(chr);// 番数
		// 统计
		if (zimo) {
			if (seat_index == provide_index)
				chr.opr_or(GameConstants_WangCheng.WIK_ZI_MO);
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
		float lChiHuScore = wFanShu * tun;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			if (seat_index == provide_index && has_rule(GameConstants_WangCheng.GAME_RULE_ZI_MO_ADD_1_FEN))
				lChiHuScore++;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;
				s += getDingPiao() * 2 + huo_piao[i] + huo_piao[seat_index];
				// 胡牌分
				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		}
	}

	private int getDingPiao() {
		if (has_rule(GameConstants_WangCheng.GAME_RULE_ZUO_PIAO_1)) {
			return 1;
		}
		if (has_rule(GameConstants_WangCheng.GAME_RULE_ZUO_PIAO_2)) {
			return 2;
		}
		if (has_rule(GameConstants_WangCheng.GAME_RULE_ZUO_PIAO_3)) {
			return 3;
		}
		if (has_rule(GameConstants_WangCheng.GAME_RULE_ZUO_PIAO_4)) {
			return 4;
		}
		if (has_rule(GameConstants_WangCheng.GAME_RULE_ZUO_PIAO_5)) {
			return 5;
		}
		if (has_rule(GameConstants_WangCheng.GAME_RULE_ZUO_PIAO_NON)) {
			return 0;
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

			if (seat_index != -1) {
				game_end.setTunShu(laizi_operate[seat_index][0]);
				game_end.setFanShu(laizi_operate[seat_index][1]);
			}

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

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);
			// if (seat_index != -1) {
			// game_end.setTunShu(this._tun_shu[seat_index]);
			// game_end.setFanShu(this._fan_shu[seat_index]);
			// }
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
				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				if (_hu_weave_count[i] > 0) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						if (GameConstants.WIK_NULL == _hu_weave_items[i][j].weave_kind) {
							continue;
						}
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
		boolean show_zi_mo = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			String des = "";

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					boolean show = true;

					if (type == GameConstants_WangCheng.WIK_ZI_MO)
						show_zi_mo = true;

					if (type == GameConstants.CHR_TIAN_HU) {
						des += ",天胡 x2";
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
						des += ",平胡";
					}
				}
			}

			if (show_zi_mo && GRR._chi_hu_rights[i].is_valid() && has_rule(GameConstants_WangCheng.GAME_RULE_ZI_MO_ADD_1_FEN))
				des += "自摸+1牌";
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
		// int[] cards_of_player0 = new int[] { 0x1, 0x1, 0x1, 0x4, 0x4, 0x4,
		// 0x11, 0x13, 0x12, 0x17, 0x1a, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19,
		// 0x19,
		// 0x19 };
		// int[] cards_of_player1 = new int[] { 0x11, 0x12, 0x12, 0x13, 0x13,
		// 0x18, 0x18, 0x18, 0x02, 0x07, 0x0a, 0x03, 0x03, 0x03, 0x0a, 0x09,
		// 0x08,
		// 0x14, 0x14 };
		// int[] cards_of_player2 = new int[] { 0x11, 0x12, 0x12, 0x13, 0x13,
		// 0x18, 0x18, 0x18, 0x02, 0x07, 0x0a, 0x03, 0x03, 0x03, 0x0a, 0x09,
		// 0x08,
		// 0x14, 0x14, };
		// int[] cards_of_player3 = new int[] { 0x13, 0x13, 0x15, 0x11, 0x11,
		// 0x01, 0x04, 0x05, 0x18, 0x18, 0x12, 0x17, 0x1a, 0x02, 0x07, 0x0a };
		//
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
		// GRR._cards_index[i][j] = 0;
		// }
		// }
		//
		// if (this.getTablePlayerNumber() == 3) {
		// for (int j = 0; j < GameConstants.MAX_HH_COUNT - 2; j++) {
		// GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])]
		// += 1;
		// GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])]
		// += 1;
		// GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])]
		// += 1;
		// }
		// } else {
		// for (int j = 0; j < 16; j++) {
		// GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])]
		// += 1;
		// GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])]
		// += 1;
		// GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])]
		// += 1;
		// GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])]
		// += 1;
		// }
		// }

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

	/**
	 * 模拟牌型--相同牌
	 */
	@Override
	public void testSameCard(int[] cards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			int send_count = GameConstants.MAX_WMQ_COUNT - 1;
			if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_4)) {
				send_count = 16;
			}
			for (int j = 0; j < send_count; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	/**
	 * 测试线上 实际牌
	 */
	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int have_send_count = 0;
		int send_count = GameConstants.MAX_WMQ_COUNT - 1;
		if (has_rule(GameConstants_WangCheng.GAME_RULE_PLAYER_4)) {
			send_count = 16;
		}
		// 分发扑克
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

}
