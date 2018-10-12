package com.cai.game.mj.yu.bao_ding;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.GameConstants_BD;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_BD extends AbstractMJTable {

	private ShuffleXCardHandler handler_shuffle_card;

	public int[][] kou_cards_index;
	public int[][] non_kou_cards_index;

	public int[] player_kou_count;

	public int[][] player_kou_feng_cards;
	public int[] player_kou_feng_cards_count;

	public int gen_zhuang_card; // 为-1 时本局没有更庄,为0时庄家还未出牌
	public int last_out_player_4_banker;

	@Override
	protected void onInitTable() {
		handler_shuffle_card = new ShuffleXCardHandler();
	}

	public void add_kou_feng_card(int seat_index, int card) {
		for (int i = 0; i < player_kou_feng_cards_count[seat_index]; i++)
			if (card == player_kou_feng_cards[seat_index][i])
				return;

		player_kou_feng_cards[seat_index][player_kou_feng_cards_count[seat_index]++] = card;
	}

	public void clean_feng_cards() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			player_kou_feng_cards_count[i] = 0;
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		player_kou_count = new int[getTablePlayerNumber()];
		player_kou_feng_cards = new int[getTablePlayerNumber()][];
		player_kou_feng_cards_count = new int[getTablePlayerNumber()];
		clean_feng_cards();

		if (has_rule(GameConstants_BD.GAME_RULE_KOU_PAI)) {
			return exe_shuffle_card();
		} else {
			return super.on_handler_game_start();
		}
	}

	public void initShuffle() {
		this.init_shuffle();
	}

	public int switch_to_cards_data(int seat_index, int[] cards_data) {

		if (!has_rule(GameConstants_BD.GAME_RULE_KOU_PAI))
			return _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards_data);

		int kou_cards_count = _logic.get_card_count_by_index(kou_cards_index[seat_index]);
		int non_kou_cards_count = _logic.get_card_count_by_index(non_kou_cards_index[seat_index]);
		int hand_cards_count = _logic.get_card_count_by_index(GRR._cards_index[seat_index]);

		// 牌错乱
		if (non_kou_cards_count + kou_cards_count != hand_cards_count)
			return 0;

		int cbPosition = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (kou_cards_index[seat_index][i] > 0) {
				for (int j = 0; j < kou_cards_index[seat_index][i]; j++) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(i) + GameConstants_BD.CARD_ESPECIAL_TYPE_KOU;
				}
			}
		}
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (non_kou_cards_index[seat_index][i] > 0) {
				for (int j = 0; j < non_kou_cards_index[seat_index][i]; j++) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}

		return cbPosition;
	}

	/// 洗牌
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;
		// 分发扑克
		for (int i = 0; i < getTablePlayerNumber() && !has_rule(GameConstants_BD.GAME_RULE_KOU_PAI); i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	public boolean exe_shuffle_card() {
		set_handler(handler_shuffle_card);
		handler_shuffle_card.reset_status(this);
		_handler.exe(this);
		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
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
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
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

			float lGangScore[] = new float[getTablePlayerNumber()];

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (reason == GameConstants.Game_End_NORMAL) {
					// 杠随赢家
					if (has_rule(GameConstants_BD.GAME_RULE_GANG_SUI_WIN) && GRR._win_order[seat_index] == 0)
						continue;
					// 砸胡免杠
					if (!GRR._chi_hu_rights[i].opr_and(GameConstants.CHR_FANG_PAO).is_empty())
						continue;

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];

				game_end.addGangScore(lGangScore[i]);
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count_fei[i]; j++) {
					pnc.addItem(GRR._player_niao_cards_fei[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);

				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

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
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
				Player player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	protected boolean on_game_start() {

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_HanShouWang.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = switch_to_cards_data(i, hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_HanShouWang.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_HanShouWang.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_HanShouWang.MAX_COUNT; j++) {
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
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants_BD.DispatchCard_Type_Tian_Hu, GameConstants_BD.DELAY_SEND_CARD_DELAY);

		return true;
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
			if (_playerStatus[i].is_bao_ting())
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_BD.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_BD.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_BD.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_BD.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_BD.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
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
		int cbCardIndexTemp[] = new int[GameConstants_BD.MAX_INDEX];
		for (int i = 0; i < GameConstants_BD.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_BD.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants_BD.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_BD.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.CHR_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants_BD.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_BD.CHR_QING_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_BD.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_BD.CHR_GNAG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		if (_logic.isShiSanYao(cards_index, weaveItems, weave_count)) {
			chiHuRight.opr_and(GameConstants_BD.CHR_SHI_SAN_YAO);
			return GameConstants_BD.WIK_CHI_HU;
		}

		if (_logic.is_feng_is_se(cards_index, weaveItems, weave_count, cur_card) != GameConstants.WIK_NULL) {
			chiHuRight.opr_and(GameConstants_BD.CHR_FENG_YI_SE);
			return GameConstants_BD.WIK_CHI_HU;
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card))
			chiHuRight.opr_and(GameConstants_BD.CHR_QING_YI_SE);

		if (is_yi_tiao_long(cards_index, cur_card))
			chiHuRight.opr_and(GameConstants_BD.CHR_YI_TIAO_LONG);

		int xiao_qi_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (xiao_qi_dui != GameConstants.WIK_NULL) {
			chiHuRight.opr_and(xiao_qi_dui);
			return GameConstants_BD.WIK_CHI_HU;
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants_HanShouWang.WIK_NULL;
		}

		return GameConstants_BD.WIK_CHI_HU;
	}

	public boolean is_yi_tiao_long(int[] cards_index, int cur_card) {
		int[] temp_card_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_card_index[_logic.switch_to_card_index(cur_card)]++;

		boolean flag = false;
		for (int i = 0; i < GameConstants_BD.MAX_ZI_FENG; i += 9) {
			for (int card = i; card < i + 9; card++) {
				if (temp_card_index[card] == 0) {
					flag = false;
					break;
				}

				flag = true;
			}

			if (flag)
				return true;
		}

		return false;
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

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4 && _logic.switch_to_card_data(i) == cur_card) {
				nGenCount++;
			}
		}

		if (cbReplaceCount > 0)
			return GameConstants.WIK_NULL;

		if (nGenCount > 0) {
			return GameConstants_BD.CHR_XIAO_QI_DUI;
		} else {
			return GameConstants_BD.CHR_HAO_HUA_QI_DUI;
		}
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = player_kou_count[seat_index] + player_kou_feng_cards_count[seat_index];// 番数

		countCardType(chr, seat_index);

		/////////////////////////////////////////////// 算分//////////////////////////
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

		float lChiHuScore = 1;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				if (has_rule(GameConstants_BD.GAME_RULE_ZHUAN_XIAN) && (seat_index == GRR._banker_player || i == GRR._banker_player))
					wFanShu++;
				float s = lChiHuScore * wFanShu; // 自摸加1分

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
			if (has_rule(GameConstants_BD.GAME_RULE_ZHUAN_XIAN) && (seat_index == GRR._banker_player || provide_index == GRR._banker_player))
				wFanShu++;
			float s = lChiHuScore * wFanShu;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		boolean qiang_gang_hu = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_BD.CHR_QING_GANG_HU) {
						result.append(" 抢杠胡");
						qiang_gang_hu = true;
					}
					if (type == GameConstants_BD.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_BD.CHR_SHU_FAN) {
						result.append(" 接炮");
					}
					if (type == GameConstants_BD.CHR_XIAO_QI_DUI) {
						result.append(" 小七对");
					}
					if (type == GameConstants_BD.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == GameConstants_BD.CHR_HAO_HUA_QI_DUI) {
						result.append(" 豪华七 小对");
					}
					if (type == GameConstants_BD.CHR_HUN_YI_SE) {
						result.append(" 混一色");
					}
					if (type == GameConstants_BD.CHR_SHI_SAN_YAO) {
						result.append(" 十三幺");
					}
					if (type == GameConstants_BD.CHR_FENG_YI_SE) {
						result.append(" 风一色");
					}

				} else if (type == GameConstants_HanShouWang.CHR_FANG_PAO) {
					if (qiang_gang_hu) {
						result.append(" 被抢杠");
					} else {
						result.append(" 放炮");
					}
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

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
