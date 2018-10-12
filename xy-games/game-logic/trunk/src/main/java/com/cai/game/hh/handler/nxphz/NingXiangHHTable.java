package com.cai.game.hh.handler.nxphz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
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
import com.google.common.base.Strings;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class NingXiangHHTable extends HHTable {

	private static final long serialVersionUID = 1L;

	public int hong;
	public int shisanhong;
	public int xiaozihu;
	public int bianhu;
	public int dazihu;

	public boolean sanTi = false;

	public int dispatch_card_count;

	public int time_for_animation = 500; // 发牌动画的时间(ms)
	public int time_for_organize = 200; // 理牌的时间(ms)
	public int time_for_operate_dragon = 1000; // 发牌到执行提的延时(ms)
	public int time_for_add_discard = 900; // 加入废牌堆的延时(ms)
	public int time_for_dispatch_card = 1000; // 发牌的延时(ms)
	public int time_for_deal_first_card = 450; // 处理第一张牌的延时(ms)
	public int time_for_force_win = 0; // 强制胡牌的延时(ms)
	public int time_for_display_win_border = 3000; // 点了胡之后，到出现小结算的延时(ms)

	private PHZHandlerPiaoNiao_NingXiang _handler_piao;

	public NingXiangHHTable() {
		super();
	}

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0)
			return playerNumber;
		if (has_rule(GameConstants.GAME_RULE_NX_PLAYER_TWO)) {
			return 2;
		}
		return GameConstants.GAME_PLAYER_HH;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new PHZHandlerDispatchCard_NingXiang();
		_handler_out_card_operate = new PHZHandlerOutCardOperate_NingXiang();
		_handler_gang = new PHZHandlerGang_NingXiang();
		_handler_chi_peng = new PHZHandlerChiPeng_NingXiang();
		_handler_chuli_firstcards = new PHZHandlerChuLiFirstCard_NingXiang();
		_handler_dispath_firstcards = new PHZHandlerDispatchFirstCard_NingXiang();

		_handler_piao = new PHZHandlerPiaoNiao_NingXiang();

		hong = 0;
		shisanhong = 0;
		bianhu = 0;
		xiaozihu = 0;
		dazihu = 0;
		sanTi = false;

		setMinPlayerCount(getTablePlayerNumber());
	}

	@Override
	public boolean reset_init_data() {
		hong = 0;
		shisanhong = 0;
		bianhu = 0;
		xiaozihu = 0;
		dazihu = 0;
		sanTi = false;
		dispatch_card_count = 0;

		return super.reset_init_data();
	}

	@Override
	public void progress_banker_select() {
		if (_cur_round == 1) {
			_cur_banker = RandomUtil.getRandomNumber(getTablePlayerNumber());// 创建者的玩家为专家

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		int gameId = getGame_id() == 0 ? 275 : getGame_id();

		SysParamModel sysParamModel1104 = null;

		if (has_rule(GameConstants.GAME_RULE_NX_SPEED_FAST)) {
			sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1205);
			time_for_deal_first_card = 450;
		} else {
			sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		}

		if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
			time_for_animation = sysParamModel1104.getVal1();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
			time_for_organize = sysParamModel1104.getVal2();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			time_for_operate_dragon = sysParamModel1104.getVal3();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
			time_for_add_discard = sysParamModel1104.getVal4();
		}
		if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
			time_for_dispatch_card = sysParamModel1104.getVal5();
		}

		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants.CARD_COUNT_PHZ_HS];
		shuffle(_repertory_card, GameConstants.CARD_PHZ_DEFAULT);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

		return game_start_HH();
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
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
		int count = getTablePlayerNumber();

		if (count == 2) {
			_repertory_card = repertory_card = Arrays.copyOf(repertory_card, 60);
		}

		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		for (int i = 0; i < count; i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);

			GRR._left_card_count -= send_count;

			// 一人20张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
			room_player.setAccountId(rplayer.getAccount_id());
			room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(_player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	private boolean game_start_HH() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
		}

		if (has_rule(GameConstants.GAME_RULE_NX_PIAO_NIAO)) {
			_handler = _handler_piao;
			_handler_piao.exe(this);
		} else {
			on_game_start_real();
		}

		return true;
	}

	public boolean on_game_start_real() {
		_logic.clean_magic_cards();

		int playerCount = getPlayerCount();
		GRR._banker_player = _current_player = _cur_banker;
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		int ti_card_count[] = new int[getTablePlayerNumber()];
		int ti_card_index[][] = new int[getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = _logic.get_action_ti_Card(GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0) {
			}
		}

		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			roomResponse.setFlashTime(time_for_animation);
			roomResponse.setStandTime(time_for_organize);
			send_response_to_player(i, roomResponse);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);

		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		load_player_info_data(roomResponse);

		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		for (int i = 0; i < playerCount; i++) {
			_playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		_handler = _handler_dispath_firstcards;

		exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, time_for_animation + time_for_organize + time_for_organize);

		return true;
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
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
			if (_logic.is_magic_index(i))
				continue;

			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (timer.get() > 1000) {
			log_error("pao huzi  ting card cost time = " + timer.duration() + "and cards is =" + Arrays.toString(cbCardIndexTemp));
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (_is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		// TODO 起手提龙2提以上，需要知道进张打出相应的牌之后，才能胡牌
		if (_ti_mul_long[seat_index] > 0) {
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

		if (has_rule(GameConstants.GAME_RULE_NX_JIU_HU_KE_HU)) {
			if (max_hu_xi < 9) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		} else {
			if (max_hu_xi < 15) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		hu_xi_hh[0] = max_hu_xi;
		analyseItem = analyseItemArray.get(max_hu_index);

		hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);
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
			}
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if ((seat_index == provider_index) && (dispatch == true) && card_type != Constants_NingXiang.CHR_TIAN_HU) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_ZI_MO);
		}

		if (hong_pai_count >= 10) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_HONG_HU);
		} else if (1 == hong_pai_count) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_DIAN_HU);
		} else if (0 == hong_pai_count) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_WU_HU);
		}

		int countDui = NingXiangPHZUtils.count_hong_pai_duizi(_logic, analyseItem);
		int countKan = NingXiangPHZUtils.count_hong_pai_kan(_logic, analyseItem);
		int countTi = NingXiangPHZUtils.count_hong_pai_ti(_logic, analyseItem);

		// 扁胡
		if (2 == hong_pai_count && 1 == countDui) { // 二扁
			chiHuRight.opr_or(Constants_NingXiang.CHR_BIAN_HU);
			chiHuRight.opr_or(Constants_NingXiang.CHR_BIAN_HU2);
		} else if (3 == hong_pai_count && 1 == countKan) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_BIAN_HU);
			chiHuRight.opr_or(Constants_NingXiang.CHR_BIAN_HU3);
		} else if (4 == hong_pai_count && 1 == countTi) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_BIAN_HU);
			chiHuRight.opr_or(Constants_NingXiang.CHR_BIAN_HU4);
		}

		// 双飘 两坎/一坎一提/两提
		if ((6 == hong_pai_count && 2 == countKan) || (7 == hong_pai_count && 1 == countKan && 1 == countTi)
				|| (8 == hong_pai_count && 2 == countTi)) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_SHUANG_PIAO);
		}

		// 碰碰胡
		if (0 == NingXiangPHZUtils.count_chi_pai(_logic, analyseItem)) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_PENG_PENG_HU);
		}

		// 大字胡
		int bigZiPai = NingXiangPHZUtils.calculate_big_pai_count(_logic, analyseItem);
		if (bigZiPai >= 18 && has_rule(GameConstants.GAME_RULE_NX_SHI_BA_DA)) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_DA_ZI_HU);
		}

		int xiaoZiPai = NingXiangPHZUtils.calculate_xiao_pai_count(_logic, analyseItem);
		if (xiaoZiPai >= 18 && has_rule(GameConstants.GAME_RULE_NX_SHI_BA_XIAO)) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_XIAO_ZI_HU);
		}

		if (xiaoZiPai >= 16 && has_rule(GameConstants.GAME_RULE_NX_SHI_LIU_XIAO)) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_XIAO_ZI_HU);
		}

		if (_cur_banker != seat_index && dispatch_card_count == 1) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_DI_HU);
		}

		if (GRR._left_card_count == 0 && has_rule(GameConstants.GAME_RULE_NX_HAI_DI)) { // 最后一张牌胡了算海底胡
			chiHuRight.opr_or(Constants_NingXiang.CHR_HAI_DI_HU);
		}

		if (has_rule(GameConstants.GAME_RULE_NX_SHUA_HOU)
				&& _logic.switch_to_cards_data(GRR._cards_index[seat_index], GRR._cards_data[seat_index]) == 1) { // 耍猴
			chiHuRight.opr_or(Constants_NingXiang.CHR_SHUA_HOU);
		}

		if (card_type == Constants_NingXiang.CHR_TIAN_HU) {
			chiHuRight.opr_or(Constants_NingXiang.CHR_TIAN_HU);
		}

		chiHuRight.opr_or(Constants_NingXiang.CHR_HU);

		return cbChiHuKind;
	}

	public int estimate_player_ti_wei_respond_phz(int seat_index, int card_data) {
		int bAroseAction = GameConstants.WIK_NULL;

		if (_logic.estimate_pao_card_out_card(GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
			exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_TI_MINE_LONG, true, true, false, 1000);

			bAroseAction = GameConstants.WIK_TI_LONG;
		}

		if (bAroseAction == GameConstants.WIK_NULL) {
			for (int weave_index = 0; weave_index < GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = GRR._weave_items[seat_index][weave_index].center_card;

				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI)) {
					continue;
				}

				exe_gang(seat_index, seat_index, card_data, GameConstants.WIK_TI_LONG, GameConstants.PAO_TYPE_MINE_SAO_LONG, true, true, false, 1000);

				bAroseAction = GameConstants.WIK_TI_LONG;
			}
		}

		if ((bAroseAction == GameConstants.WIK_NULL) && (_logic.check_sao(GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL)) {
			int action = GameConstants.WIK_WEI;

			for (int i = 0; i < _cannot_peng_count[seat_index]; i++) {
				if (card_data == _cannot_peng[seat_index][i]) {
					action = GameConstants.WIK_CHOU_WEI;
				}
			}

			exe_gang(seat_index, seat_index, card_data, action, GameConstants.SAO_TYPE_MINE_SAO, true, true, false, 1000);

			bAroseAction = GameConstants.WIK_WEI;
		}

		return bAroseAction;
	}

	public int estimate_player_respond_phz(int seat_index, int provider_index, int card_data, int pao_type[], boolean dispatch) {
		int bAroseAction = GameConstants.WIK_NULL;
		pao_type[0] = 0;

		if ((bAroseAction == GameConstants.WIK_NULL) && (dispatch == true)) {
			for (int weave_index = 0; weave_index < GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = GRR._weave_items[seat_index][weave_index].center_card;

				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_PENG)) {
					continue;
				}

				pao_type[0] = GameConstants.PAO_TYPE_MINE_PENG_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}

		if ((bAroseAction == GameConstants.WIK_NULL) && (seat_index != provider_index)) {
			if (_logic.check_pao(GRR._cards_index[seat_index], card_data) != GameConstants.WIK_NULL) {
				pao_type[0] = GameConstants.PAO_TYPE_OHTER_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}
		}

		if (seat_index != provider_index) {
			for (int weave_index = 0; weave_index < GRR._weave_count[seat_index]; weave_index++) {
				int weave_kind = GRR._weave_items[seat_index][weave_index].weave_kind;
				int weave_card = GRR._weave_items[seat_index][weave_index].center_card;

				if ((weave_card != card_data) || (weave_kind != GameConstants.WIK_WEI)) {
					continue;
				}

				pao_type[0] = GameConstants.PAO_TYPE_OTHER_SAO_PAO;
				bAroseAction = GameConstants.WIK_PAO;
			}

		}

		return bAroseAction;
	}

	public int get_di_fen() {
		if (has_rule(GameConstants.GAME_RULE_NX_DI_FEN_2))
			return 2;
		if (has_rule(GameConstants.GAME_RULE_NX_DI_FEN_3))
			return 3;
		if (has_rule(GameConstants.GAME_RULE_NX_DI_FEN_4))
			return 4;
		return 2;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		countCardType(chr, seat_index);

		int all_hu_xi = 0;
		for (int i = 0; i < _hu_weave_count[seat_index]; i++) {
			all_hu_xi += _hu_weave_items[seat_index][i].hu_xi;
		}

		_hu_xi[seat_index] = all_hu_xi;

		int calculate_score = 0;
		int di_fen = get_di_fen();
		if (has_rule(GameConstants.GAME_RULE_NX_JIU_HU_KE_HU)) {
			calculate_score = di_fen + (all_hu_xi - 9) / 3;
		} else {
			calculate_score = di_fen + (all_hu_xi - 15) / 3;
		}

		float lChiHuScore = calculate_score;

		if (!sanTi) {
			int wFanShu = get_chi_hu_action_rank_phz(chr, _hu_weave_items[seat_index], _hu_weave_items[seat_index].length);// 番数
			int maxFanShu = -1;

			if (has_rule(GameConstants.GAME_RULE_NX_MAX_FIVE)) {
				maxFanShu = 5;
			} else if (has_rule(GameConstants.GAME_RULE_NX_MAX_TEN)) {
				maxFanShu = 10;
			}

			if (maxFanShu > 0 && wFanShu > maxFanShu) {
				wFanShu = maxFanShu;
			}

			if (zimo) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}

					GRR._lost_fan_shu[i][seat_index] = wFanShu;
				}
			}

			lChiHuScore = wFanShu * calculate_score;

			if (zimo && (seat_index == provide_index) && !chr.opr_and(Constants_NingXiang.CHR_ZI_MO).is_empty()) {
				if (has_rule(GameConstants.GAME_RULE_NX_JIAFEN)) {
					lChiHuScore += 1;
				} else if (has_rule(GameConstants.GAME_RULE_NX_JIAFEN_TWO)) {
					lChiHuScore += 2;
				}
			}
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float tmp_score = lChiHuScore;

				tmp_score += 2 * getDingNiao();

				if (has_rule(GameConstants.GAME_RULE_NX_PIAO_NIAO)) {
					tmp_score += _player_result.pao[i] + _player_result.pao[seat_index];
				}

				GRR._game_score[i] -= tmp_score;
				GRR._game_score[seat_index] += tmp_score;
			}
		}
	}

	public int getDingNiao() {
		int i = 0;
		if (has_rule(GameConstants.GAME_RULE_NX_ZHANIAO_ONE)) {
			i = 1;
		} else if (has_rule(GameConstants.GAME_RULE_NX_ZHANIAO)) {
			i = 2;
		} else if (has_rule(GameConstants.GAME_RULE_NX_ZHANIAO_THREE)) {
			i = 3;
		}
		return i;
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
			count = getTablePlayerNumber();
		}

		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
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

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);

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

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe(seat_index);

			if (has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					_huang_zhang_count++;
				} else {
					_huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);

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

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

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
			if (_cur_round >= _game_round) { // 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
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
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}
		if (!is_sys()) {
			GRR = null;
		}

		return false;
	}

	@Override
	public void countChiHuTimes(int _seat_index, boolean isZimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += GRR._game_score[i];
		}

		if (isZimo) {
			_player_result.hu_pai_count[_seat_index]++;
			_player_result.ying_xi_count[_seat_index] += _hu_xi[_seat_index];
		}

		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_HONG_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_SHI_SAN_HONG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_DIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_WU_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_BIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_SHUANG_PIAO)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_PENG_PENG_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_DA_ZI_HU)).is_empty()) { // 大字胡小字胡次数统计有问题
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_XIAO_ZI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_DI_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_HAI_DI_HU)).is_empty() && has_rule(GameConstants.GAME_RULE_NX_HAI_DI)) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_NingXiang.CHR_SHUA_HOU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (sanTi) {
			_player_result.ming_tang_count[_seat_index]++;
		}
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GRR._weave_items[i], GRR._weave_count[i],
					GameConstants.INVALID_SEAT);
		}

		return;
	}

	@Override
	public void set_result_describe(int seat_index) {
		int l;
		long type = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			StringBuffer des = new StringBuffer();

			l = GRR._chi_hu_rights[i].type_count;
			if (dazihu > 0 && has_rule(GameConstants.GAME_RULE_NX_SHI_BA_DA)) {
				des.append(",大字胡X").append(dazihu);
			}

			if (xiaozihu > 0 && has_rule(GameConstants.GAME_RULE_NX_SHI_BA_XIAO)) {
				des.append(",小字胡X").append(xiaozihu);
			}
			if (xiaozihu > 0 && has_rule(GameConstants.GAME_RULE_NX_SHI_LIU_XIAO)) {
				des.append(",小字胡X").append(xiaozihu);
			}

			boolean spe = false;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (sanTi) {
						spe = true;
					}
					if (type == Constants_NingXiang.CHR_HONG_HU) {
						if (hong == 0) {
							hong = 2;
						}
						des.append(",红胡X").append(hong);
					}
					if (type == Constants_NingXiang.CHR_SHUANG_PIAO) {
						des.append(",双飘X2");
					}
					if (type == Constants_NingXiang.CHR_BIAN_HU) {
						des.append(",扁胡X").append(bianhu);
					}
					if (type == Constants_NingXiang.CHR_DIAN_HU) {
						des.append(",点胡X4");
					}
					if (type == Constants_NingXiang.CHR_WU_HU) {
						des.append(",乌胡X5");
					}
					if (type == Constants_NingXiang.CHR_PENG_PENG_HU) {
						des.append(",碰碰胡X5");
					}
					if (type == Constants_NingXiang.CHR_TIAN_HU) {
						des.append(",天胡X5");
					}
					if (type == Constants_NingXiang.CHR_DI_HU) {
						des.append(",地胡X5");
					}
					if (has_rule(GameConstants.GAME_RULE_NX_HAI_DI) && type == Constants_NingXiang.CHR_HAI_DI_HU) {
						des.append(",海底胡X5");
					}
					if (type == Constants_NingXiang.CHR_SHUA_HOU) {
						des.append(",耍猴X5");
					}
					if (type == Constants_NingXiang.CHR_ZI_MO) {
						des.append(",自摸");
						if (has_rule(GameConstants.GAME_RULE_NX_JIAFEN)) {
							des.append("+1");
						} else if (has_rule(GameConstants.GAME_RULE_NX_JIAFEN_TWO)) {
							des.append("+2");
						}
					}
				}
			}
			if (spe) {
				des = new StringBuffer("天胡: 三提五坎");
			}
			if (Strings.isNullOrEmpty(des.toString())) {
				des.append(",平胡");
			}
			if (has_rule(GameConstants.GAME_RULE_NX_ZHANIAO)) {
				des.append(",坐飘+2");
			} else if (has_rule(GameConstants.GAME_RULE_NX_ZHANIAO_ONE)) {
				des.append(",坐飘+1");
			} else if (has_rule(GameConstants.GAME_RULE_NX_ZHANIAO_THREE)) {
				des.append(",坐飘+3");
			}

			GRR._result_des[i] = des.toString();
		}
	}

	public int get_chi_hu_action_rank_phz(ChiHuRight chr, WeaveItem weaveItems[], int weaveCount) {
		int wFanShu = 0;
		int countHong = NingXiangPHZUtils.calculate_hongOrHei_pai_count(_logic, weaveItems, weaveCount, false);

		if (countHong >= 10) { // 红胡
			wFanShu += 2;
			hong += 2;
			if (has_rule(GameConstants.GAME_RULE_NX_JIAOHONG)) {
				wFanShu += countHong - 10;
				hong += countHong - 10;
			}
		} else if (1 == countHong) { // 点胡
			wFanShu += 4;
		} else if (0 == countHong) { // 乌胡
			wFanShu += 5;
		}

		// 扁胡
		if (!(chr.opr_and(Constants_NingXiang.CHR_BIAN_HU2).is_empty())) { // 二扁
			wFanShu += 2;
			bianhu = 2;
		} else if (!(chr.opr_and(Constants_NingXiang.CHR_BIAN_HU3).is_empty())) {
			wFanShu += 3;
			bianhu = 3;
		} else if (!(chr.opr_and(Constants_NingXiang.CHR_BIAN_HU4).is_empty())) {
			wFanShu += 4;
			bianhu = 4;
		}

		// 双飘 两坎/一坎一提/两提
		if (!(chr.opr_and(Constants_NingXiang.CHR_SHUANG_PIAO).is_empty())) {
			wFanShu += 2;
		}
		// 碰碰胡
		if (!(chr.opr_and(Constants_NingXiang.CHR_PENG_PENG_HU).is_empty())) {
			wFanShu += 5;
		}
		// 耍猴
		if (!(chr.opr_and(Constants_NingXiang.CHR_SHUA_HOU).is_empty())) {
			wFanShu += 5;
		}

		if (has_rule(GameConstants.GAME_RULE_NX_SHI_BA_DA)) {
			// 大字胡
			int bigZiPai = NingXiangPHZUtils.calculate_big_pai_count(_logic, weaveItems, weaveCount);
			if (bigZiPai >= 18) {
				wFanShu += 5;
				dazihu = 5;
				if (has_rule(GameConstants.GAME_RULE_NX_JIAOHONG)) {
					wFanShu += bigZiPai - 18;
					dazihu += bigZiPai - 18;
				}
			}
		}

		if (has_rule(GameConstants.GAME_RULE_NX_SHI_BA_XIAO)) {
			// 小字胡
			int xiaoZiPai = NingXiangPHZUtils.calculate_xiao_pai_count(_logic, weaveItems, weaveCount);
			if (xiaoZiPai >= 18) {
				wFanShu += 5;
				xiaozihu = 5;
				if (has_rule(GameConstants.GAME_RULE_NX_JIAOHONG)) {
					wFanShu += xiaoZiPai - 18;
					xiaozihu += xiaoZiPai - 18;
				}
			}
		}

		if (has_rule(GameConstants.GAME_RULE_NX_SHI_LIU_XIAO)) {
			// 小字胡
			int xiaoZiPai = NingXiangPHZUtils.calculate_xiao_pai_count(_logic, weaveItems, weaveCount);
			if (xiaoZiPai >= 16) {
				wFanShu += 5;
				xiaozihu = 5;
				if (has_rule(GameConstants.GAME_RULE_NX_JIAOHONG)) {
					wFanShu += xiaoZiPai - 16;
					xiaozihu += xiaoZiPai - 16;
				}
			}
		}

		if (has_rule(GameConstants.GAME_RULE_NX_HAI_DI) && !chr.opr_and(Constants_NingXiang.CHR_HAI_DI_HU).is_empty()) { // 海底胡
			wFanShu += 5;
		}

		if (!chr.opr_and(Constants_NingXiang.CHR_TIAN_HU).is_empty() || !chr.opr_and(Constants_NingXiang.CHR_DI_HU).is_empty()) { // 天胡地胡五番
			wFanShu += 5;
		}

		return 0 == wFanShu ? 1 : wFanShu;
	}

	@Override
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		_repertory_card = realyCards;
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_HH_COUNT - 1);

			if (is_mj_type(GameConstants.GAME_TYPE_WMQ_AX))
				send_count = GameConstants.MAX_WMQ_COUNT - 1;

			if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
					|| is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD))
				send_count = (GameConstants.MAX_FPHZ_COUNT - 1);

			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(_repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		DEBUG_CARDS_MODE = false;
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");
	}

	@Override
	public void test_cards() {
		int cards[][] = { { 0x11, 0x11, 0x01, 0x12, 0x12, 0x02, 0x13, 0x13, 0x03, 0x14, 0x14, 0x04, 0x15, 0x15, 0x05, 0x16, 0x16, 0x06, 0x17, 0x17 },
				{ 0x11, 0x11, 0x01, 0x12, 0x12, 0x12, 0x13, 0x13, 0x03, 0x14, 0x14, 0x04, 0x15, 0x15, 0x05, 0x16, 0x16, 0x06, 0x16, 0x17 },
				{ 0x11, 0x11, 0x01, 0x12, 0x12, 0x12, 0x13, 0x13, 0x03, 0x14, 0x14, 0x04, 0x15, 0x15, 0x05, 0x16, 0x16, 0x06, 0x16, 0x17 }, };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < cards[i].length; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[i][j])] += 1;
			}
		}

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
				} else if (is_mj_type(GameConstants.GAME_TYPE_FPHZ_YX) || is_mj_type(GameConstants.GAME_TYPE_LHQ_HD)
						|| is_mj_type(GameConstants.GAME_TYPE_LHQ_HY) || is_mj_type(GameConstants.GAME_TYPE_LHQ_QD)) {
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
