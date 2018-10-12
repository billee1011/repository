package com.cai.game.mj.hubei.hsw;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.constant.game.GameConstants_WangCheng;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.AbstractMJTable;
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

/**
 * 汉寿王
 * 
 * @author yu
 *
 */
public class Table_HSW extends AbstractMJTable {

	public Table_HSW() {
		super(MJType.GAME_TYPE_HAN_SHOU_WANG);
	}

	@Override
	public int get_real_card(int card) {
		if (card >= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			return card;
		}

		if (card > GameConstants_WangCheng.CARD_ESPECIAL_TYPE_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		return card;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HSW();
		_handler_dispath_card = new HandlerDispatchCard_HSW();
		_handler_gang = new HandlerGang_HSW();
		_handler_out_card_operate = new HandlerOutCardOperate_HSW();
	}

	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;

		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			for (int i = 0; i < cards_index[_logic.get_magic_card_index(m)]; i++) {
				cards_data[cbPosition++] = _logic.switch_to_card_data(_logic.get_magic_card_index(m)) + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			}
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}

		return cbPosition;
	}

	/**
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

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round() && has_rule(GameConstants_HanShouWang.GAME_RULE_WANG_NOT_QIANG_GANG_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_HanShouWang.HU_CARD_TYPE_QIANG_GANG, i);

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

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants_HanShouWang.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_HanShouWang.MAX_ABANDONED_CARDS_COUNT; x++) {
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
					int[] cards_index_temp = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
					cards_index_temp[_logic.switch_to_card_index(card)] -= 2;

					int hand_cards_count = _logic.get_card_count_by_index(cards_index_temp);
					int hand_hz_count = cards_index_temp[_logic.switch_to_card_index(GameConstants_HanShouWang.HZ_MAGIC_CARD)];

					if (hand_cards_count != hand_hz_count) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_HanShouWang.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_HanShouWang.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (!has_rule(GameConstants_HanShouWang.GAME_RULE_DIAN_PAO_HU)) {
					can_hu_this_card = false;
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_HanShouWang.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_HanShouWang.WIK_CHI_HU);
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

	@Override
	public int getTablePlayerNumber() {
		if (playerNumber > 0) {
			return playerNumber;
		}

		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) {
			return 2;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) {
			return 3;
		}
		return 4;
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

				if (reason == GameConstants.Game_End_NORMAL || has_rule(GameConstants_HanShouWang.GAME_RULE_HUAN_ZHUAN_NOT_HUAN_GANG))
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
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
				GRR._card_count[i] = switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
					} else {
						cs.addItem(GRR._cards_data[i][j]);
					}
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

	/**
	 * 初始化洗牌数据:默认(如不能满足请重写此方法)
	 * 
	 * @return
	 */
	protected void init_shuffle() {
		int[] cards_data = null;
		if (has_rule(GameConstants_HanShouWang.GAME_RULE_HZ_NUMBER_8)) {
			cards_data = GameConstants_HanShouWang.CARD_DATA_HAN_SHOU_WANG_116;
		} else {
			cards_data = GameConstants_HanShouWang.CARD_DATA_HAN_SHOU_WANG_112;
		}
		_repertory_card = new int[cards_data.length];
		shuffle(_repertory_card, cards_data);
	};

	@Override
	protected boolean on_game_start() {
		_logic.clean_magic_cards();

		_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants.HZ_MAGIC_CARD));

		_game_status = GameConstants_HanShouWang.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_HanShouWang.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
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

		exe_dispatch_card(_current_player, GameConstants_HanShouWang.DispatchCard_Type_Tian_Hu, GameConstants_HanShouWang.DELAY_SEND_CARD_DELAY);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_HanShouWang.MAX_INDEX];
		for (int i = 0; i < GameConstants_HanShouWang.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_HanShouWang.MAX_ZI;
		int real_max_ting_count = GameConstants_HanShouWang.MAX_ZI + 1;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_HanShouWang.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		cbCurrentCard = GameConstants_HanShouWang.HZ_MAGIC_CARD;
		chr.set_empty();
		if (GameConstants_HanShouWang.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
				Constants_HuangZhou.HU_CARD_TYPE_ZI_MO, seat_index)) {
			cards[count] = cbCurrentCard;
			if (_logic.is_magic_card(cbCurrentCard))
				cards[count] += GameConstants_HanShouWang.CARD_ESPECIAL_TYPE_WANG_BA;
			count++;
		}

		if (count == 0) {
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean is_feng_is_se(int cards_index[], WeaveItem weaveItems[], int cbWeaveCount, int cur_card) {
		for (int i = 0; i < cbWeaveCount; i++) {
			WeaveItem weaveItem = weaveItems[i];
			int index = _logic.switch_to_card_index(weaveItem.center_card);
			if (index < GameConstants.MAX_ZI) {
				return false;
			}
		}

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (cbCardIndexTemp[i] > 0) {
				return false;
			}
		}

		if (cbCardIndexTemp[_logic.switch_to_card_index(GameConstants_HanShouWang.HZ_MAGIC_CARD)] != 0)
			return false;

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		if (card_type == GameConstants_HanShouWang.HU_CARD_TYPE_JIE_PAO && !has_rule(GameConstants_HanShouWang.GAME_RULE_DIAN_PAO_HU))
			return GameConstants_HanShouWang.WIK_NULL;

		int[] cardTemp = Arrays.copyOf(cards_index, GameUtilConstants.MAX_CARD_TYPE);
		if (cur_card != GameConstants.INVALID_VALUE) {
			cardTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		int hz_number_hu = 4;
		if (has_rule(GameConstants_HanShouWang.GAME_RULE_HZ_NUMBER_8))
			hz_number_hu = 8;
		if (cardTemp[_logic.switch_to_card_index(GameConstants_HanShouWang.HZ_MAGIC_CARD)] == hz_number_hu) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			return GameConstants_HanShouWang.WIK_CHI_HU;
		}

		// if (GameConstants.WIK_NULL != _logic.is_qi_xiao_dui(cards_index,
		// weaveItems, weave_count, cur_card)) {
		// if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
		// chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		// } else if (card_type ==
		// GameConstants_HanShouWang.HU_CARD_TYPE_QIANG_GANG) {
		// chiHuRight.opr_or(GameConstants_HanShouWang.CHR_HUNAN_QIANG_GANG_HU);//
		// 抢杠胡
		// } else {
		// chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		// }
		// return GameConstants_HanShouWang.WIK_CHI_HU;
		// }

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants_HanShouWang.WIK_NULL;
		}

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants_HanShouWang.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_HanShouWang.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}
		return GameConstants_HanShouWang.WIK_CHI_HU;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = 2;// 番数
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

		float lChiHuScore = wFanShu;

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = lChiHuScore + 2 * GRR._count_pick_niao;

				// 无王加番
				// if
				// (GRR._cards_index[seat_index][_logic.switch_to_card_index(GameConstants_HanShouWang.HZ_MAGIC_CARD)]
				// == 0)
				// s *= 2;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

			lChiHuScore = 1;
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_HanShouWang.CHR_HUNAN_QIANG_GANG_HU).is_empty()) {
				lChiHuScore = 2;
			}
			float s = lChiHuScore + 2 * GRR._count_pick_niao;

			// 无王加番
			// if
			// (GRR._cards_index[seat_index][_logic.switch_to_card_index(GameConstants_HanShouWang.HZ_MAGIC_CARD)]
			// == 0)
			// s *= 2;

			if (!chr.opr_and(GameConstants_HanShouWang.CHR_HUNAN_QIANG_GANG_HU).is_empty()) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (i == seat_index) {
						continue;
					}
					GRR._game_score[provide_index] -= s;
					GRR._game_score[seat_index] += s;
				}
			} else {
				// 胡牌分
				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			} // 抢杠胡)
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
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
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao, int hu_card) {

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;
		GRR._count_niao = getCsDingNiaoNum();

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}
		// 中鸟个数
		GRR._count_pick_niao = _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			int seat = 0;
			seat = (seat_index + (nValue - 1) % 4) % 4;
			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];
			GRR._player_niao_count[seat]++;
		}

		// GRR._count_pick_niao = 0;
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < GRR._player_niao_count[i]; j++) {
		// if (seat_index == i) {
		// GRR._count_pick_niao++;
		// GRR._player_niao_cards[i][j] =
		// this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
		// 胡牌的鸟生效
		// } else {
		// GRR._player_niao_cards[i][j] =
		// this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
		// 胡牌的鸟生效
		// }
		// }
		// }

		int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		int[][] player_niao_cards = new int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._count_pick_niao = 0;
		if (has_rule(GameConstants_HanShouWang.GAME_RULE_HU_JI_JIANG_JI)) {
			player_niao_cards[seat_index][player_niao_count[seat_index]] = this.set_ding_niao_valid(hu_card, true);
			player_niao_count[seat_index]++;
			if (hu_card == GameConstants_HanShouWang.HZ_MAGIC_CARD) {
				GRR._count_pick_niao = 10;
			} else {
				GRR._count_pick_niao = _logic.get_card_value(hu_card);
			}
		} else {
			for (int i = 0; i < 4; i++) {
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					if (seat_index == i) {
						GRR._count_pick_niao++;
						player_niao_cards[seat_index][player_niao_count[seat_index]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);// 胡牌的鸟生效
					} else {
						player_niao_cards[seat_index][player_niao_count[seat_index]] = this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);// 胡牌的鸟生效
					}
					player_niao_count[seat_index]++;
				}
			}
		}
		GRR._player_niao_cards = player_niao_cards;
		GRR._player_niao_count = player_niao_count;
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		int nNum = GameConstants.ZHANIAO_0;

		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_NOT_ZHUA_NIAO))
			return GameConstants.ZHANIAO_0;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_ZHUA_NIAO_2))
			return GameConstants.ZHANIAO_2;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_ZHUA_NIAO_4))
			return GameConstants.ZHANIAO_4;
		if (GameDescUtil.has_rule(gameRuleIndexEx, GameConstants_HanShouWang.GAME_RULE_ZHUA_NIAO_6))
			return GameConstants.ZHANIAO_6;

		return nNum;
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
					if (type == GameConstants_HanShouWang.CHR_HUNAN_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_HanShouWang.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_HanShouWang.CHR_SHU_FAN) {
						result.append(" 接炮");
					}

				} else if (type == GameConstants_HanShouWang.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (GRR._count_pick_niao > 0)
					result.append(" 中码X" + GRR._count_pick_niao);
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
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x3, 0x3, 0x3, 0x11, 0x11, 0x11, 0x14, 0x14, 0x14, 0x15, 0x15, 0x15, 0x35 };
		int[] cards_of_player1 = new int[] { 0x3, 0x3, 0x3, 0x11, 0x11, 0x11, 0x14, 0x14, 0x14, 0x15, 0x15, 0x15, 0x35 };
		int[] cards_of_player2 = new int[] { 0x3, 0x3, 0x3, 0x11, 0x11, 0x11, 0x14, 0x14, 0x14, 0x15, 0x15, 0x15, 0x35 };
		int[] cards_of_player3 = new int[] { 0x3, 0x3, 0x3, 0x11, 0x11, 0x11, 0x14, 0x14, 0x14, 0x15, 0x15, 0x15, 0x35 };

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
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
