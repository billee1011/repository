package com.cai.game.mj.henan.jiyuan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_MJ_JiYuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.MJType;
import com.cai.service.PlayerServiceImpl;
import com.google.common.base.Strings;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_JiYuan extends AbstractMJTable {

	private static final long serialVersionUID = -3740668572580145190L;

	public boolean isLast; // 是否最后一张牌
	public int isBian[]; // 是否胡的是边章
	public int canNotHu[]; // 每个玩家不能胡的牌 (包括过手胡、跟庄胡)
	public int canNotGang[]; // 每个玩家当前不能杠的牌[有杠不杠选择碰牌、要过一轮之后才能杠]
	public int outCardRound[]; // 补杠辅助判断
	public boolean isQiangGang; // 是否抢杠
	public boolean isDahu;
	public boolean isDianPao;
	public boolean isLiuJu;
	public ChiHuRight[] chr; // 胡牌类型
	public int jiePao[];
	public int dianPao[];

	private HandlerPao_JiYuan _handler_pao;
	public int min_pao = -1;
	public int max_pao = -1;

	public Table_JiYuan() {
		super(MJType.GAME_TYPE_MJ_JI_YUAN);
	}

	@Override
	public int getTablePlayerNumber() {
		return GameConstants.GAME_PLAYER;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_JiYuan();
		_handler_dispath_card = new HandlerDispatchCard_JiYuan();
		_handler_gang = new HandlerGang_JiYuan();
		_handler_out_card_operate = new HandlerOutCardOperate_JiYuan();
		_handler_pao = new HandlerPao_JiYuan();

		Arrays.fill(_player_result.pao, 0);
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (get_players()[seat_index] == null) {
			return false;
		}

		if (GameConstants.GS_MJ_XIAOHU != _game_status && GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {
			_player_ready[seat_index] = 0;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);

			if (_cur_round > 0) {
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS); // 刷新玩家

				load_player_info_data(roomResponse2);
				send_response_to_player(seat_index, roomResponse2);
			}

			return false;
		}

		if (_player_ready[seat_index] == 1)
			return true;

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (get_players()[seat_index].getAccount_id() == getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		Arrays.fill(_player_result.pao, -1);
		Arrays.fill(_player_result.nao, -1);

		if (_cur_round > 0) {
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);

			load_player_info_data(roomResponse2);
			send_response_to_player(seat_index, roomResponse2);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);
		isBian = new int[getTablePlayerNumber()];
		canNotHu = new int[getTablePlayerNumber()];
		canNotGang = new int[getTablePlayerNumber()];
		outCardRound = new int[getTablePlayerNumber()];
		jiePao = new int[getTablePlayerNumber()];
		dianPao = new int[getTablePlayerNumber()];
		isLast = false;
		isQiangGang = false;
		isDahu = false;
		isDianPao = false;
		isLiuJu = false;

		chr = new ChiHuRight[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			chr[i] = new ChiHuRight();
		}

		setMinPlayerCount(getTablePlayerNumber());
	}

	@Override
	public boolean reset_init_data() {
		isLast = false;
		isQiangGang = false;
		isDahu = false;
		isDianPao = false;
		isLiuJu = false;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			chr[i] = new ChiHuRight();
		}
		Arrays.fill(isBian, 0);
		Arrays.fill(canNotHu, 0);
		Arrays.fill(canNotGang, 0);
		Arrays.fill(outCardRound, 0);
		Arrays.fill(jiePao, 0);
		Arrays.fill(dianPao, 0);
		return super.reset_init_data();
	}

	@Override
	protected void init_shuffle() {
		int[] cards = MJConstants.DEFAULT;
		if (!has_rule(Constants_MJ_JiYuan.GAME_RULE_DAI_FENG)) {
			cards = MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
		_repertory_card = new int[cards.length];
		shuffle(_repertory_card, cards);
	};

	private void deal_max_min_pao() {
		if (has_rule(Constants_MJ_JiYuan.GAME_RULE_GU_DING_PAO_1)) {
			Arrays.fill(_player_result.pao, 1);
		} else if (has_rule(Constants_MJ_JiYuan.GAME_RULE_GU_DING_PAO_2)) {
			Arrays.fill(_player_result.pao, 2);
		} else if (has_rule(Constants_MJ_JiYuan.GAME_RULE_GU_DING_PAO_3)) {
			Arrays.fill(_player_result.pao, 3);
		} else if (has_rule(Constants_MJ_JiYuan.GAME_RULE_GU_DING_PAO_4)) {
			Arrays.fill(_player_result.pao, 4);
		} else if (has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_YOU_PAO_0_4)) {
			Arrays.fill(_player_result.pao, -1);
			min_pao = 0;
			max_pao = 4;
		} else if (has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_YOU_PAO_1_4)) {
			Arrays.fill(_player_result.pao, -1);
			min_pao = 1;
			max_pao = 4;
		} else if (has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_YOU_PAO_2_4)) {
			Arrays.fill(_player_result.pao, -1);
			min_pao = 2;
			max_pao = 4;
		} else {
			Arrays.fill(_player_result.pao, 0);
			min_pao = 0;
			max_pao = 0;
		}
	}

	@Override
	protected boolean on_game_start() {
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}

		deal_max_min_pao();
		operate_player_data();

		if (has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_YOU_PAO_0_4) || has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_YOU_PAO_1_4)
				|| has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_YOU_PAO_2_4)) {
			set_handler(_handler_pao);
			_handler_pao.exe(this);
			return true;
		}

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
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);

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

			GRR._end_type = reason;

			float lGangScore[] = new float[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (GRR._end_type != GameConstants.Game_End_DRAW && GRR._end_type != GameConstants.Game_End_RELEASE_PLAY) { // 荒庄荒杠
																															// 中途解散也荒杠
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}
				}
				if (GRR._end_type == GameConstants.Game_End_RELEASE_PLAY) {
					isLiuJu = true;
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

			game_end.setCellScore(get_di_fen());

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);

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
				if (player == null) {
					continue;
				}
				send_error_notify(j, 1, "游戏解散成功!");
			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		} else {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		int cbChiHuKind = GameConstants.WIK_NULL;

		int check_qi_xiao_dui = _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);

		if (check_qi_xiao_dui != GameConstants.WIK_NULL) {
			cbChiHuKind = GameConstants.WIK_CHI_HU;
			chiHuRight.opr_or(Constants_MJ_JiYuan.CHR_XIAO_QI_DUI);

			return cbChiHuKind;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weave_count, analyseItemArray, true);

		if (bValue == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (card_type == Constants_MJ_JiYuan.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(Constants_MJ_JiYuan.CHR_GANG_SHANG_HUA);
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		chiHuRight.opr_or(cbChiHuKind);

		return cbChiHuKind;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int di_fen = get_di_fen();

		countCardType(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = di_fen;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = di_fen;
		}

		if (zimo) {
			int tmpScore = 2;
			if (!(chr.opr_and(Constants_MJ_JiYuan.CHR_XIAO_QI_DUI)).is_empty() || isBian[seat_index] > 0) { // 七对
				tmpScore = 3;
			}
			int score = tmpScore * di_fen;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				int nScore = score * (1 + _player_result.pao[i] + _player_result.pao[seat_index]);
				GRR._game_score[i] -= nScore;
				GRR._game_score[seat_index] += nScore;
			}
		} else {
			int tmpScore = 2;
			int score = tmpScore * di_fen;
			int nScore = score * (1 + _player_result.pao[provide_index] + _player_result.pao[seat_index]);
			GRR._game_score[provide_index] -= nScore;
			GRR._game_score[seat_index] += nScore;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
	}

	@Override
	protected void set_result_describe() {
		// int l;
		// long type = 0;
		if (!isLiuJu) { // 不是流局才计算
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				StringBuffer des = new StringBuffer();

				// l = GRR._chi_hu_rights[i].type_count;
				// for (int j = 0; j < l; j++) {
				// type = GRR._chi_hu_rights[i].type_list[j];
				// if (GRR._chi_hu_rights[i].is_valid()) {
				// if (type == Constants_MJ_JiYuan.CHR_XIAO_QI_DUI) {
				// des.append(",七对");
				// }
				// }
				// }

				if (!(chr[i].opr_and(Constants_MJ_JiYuan.CHR_BA_TOU_GANG).is_empty())
						|| !(chr[i].opr_and(Constants_MJ_JiYuan.CHR_FANG_PAO).is_empty())) {
					if (dianPao[i] > 0) {
						des.append(",放炮");
					} else if (jiePao[i] > 0) {
						des.append(",拔头杠");
					}
				} else if (!(chr[i].opr_and(Constants_MJ_JiYuan.CHR_JIE_PAO).is_empty())
						|| !(chr[i].opr_and(Constants_MJ_JiYuan.CHR_FANG_PAO).is_empty())) {
					if (dianPao[i] > 0) {
						des.append(",放炮");
					} else if (jiePao[i] > 0) {
						des.append(",接炮");
					}
				}
				if (!(chr[i].opr_and(Constants_MJ_JiYuan.CHR_ZI_MO).is_empty())) {
					des.append(",自摸");
					if (!(chr[i].opr_and(Constants_MJ_JiYuan.CHR_DA_HU).is_empty())) {
						des.append(",大胡");
					}
				}
				if (!(chr[i].opr_and(Constants_MJ_JiYuan.CHR_GANG_SHANG_HUA).is_empty())) {
					des.append(",杠上开花");
				}

				if (GRR._gang_score[i].an_gang_count > 0) {
					des.append(",暗杠X").append(GRR._gang_score[i].an_gang_count);
				}
				if (GRR._gang_score[i].ming_gang_count > 0) {
					des.append(",接杠X").append(GRR._gang_score[i].ming_gang_count);
				}
				if (GRR._gang_score[i].provide_gang_count > 0) {
					des.append(",放杠X").append(GRR._gang_score[i].provide_gang_count);
				}

				GRR._result_des[i] = Strings.isNullOrEmpty(des.toString()) ? "" : des.substring(1, des.length());
			}
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			if (playerStatus.isAbandoned())
				continue;

			if (playerStatus.is_chi_hu_round() && !has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_MO_HU)) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int cbWeaveCount = GRR._weave_count[i];

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						Constants_MJ_JiYuan.HU_CARD_TYPE_QIANG_GANG, i);

				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;
			_provide_card = card;
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
		}

		return bAroseAction;
	}

	/**
	 * 其他玩家对当前出牌信息的响应
	 * 
	 * @param seat_index
	 * @param card
	 * @param type
	 * @return
	 */
	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;
		int action = GameConstants.WIK_NULL;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			playerStatus = _playerStatus[i];
			if (!isLast) { // 最后一张牌不需要吃碰操作
				boolean can_peng_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
				for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
					if (tmp_cards_data[x] == card) {
						can_peng_this_card = false;
						break;
					}
				}
				if (can_peng_this_card) {
					action = _logic.check_peng(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1); // 加上杠
					bAroseAction = true;
				}
			}

			// 可点炮 且听的是一张牌 且不是跟庄胡不是过手胡
			if (isBian[i] > 0 && !has_rule(Constants_MJ_JiYuan.GAME_RULE_ZI_MO_HU) && canNotHu[i] != card) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				int card_type = Constants_MJ_JiYuan.HU_CARD_TYPE_JIE_PAO;
				if (type == GameConstants.GANG_TYPE_AN_GANG || type == GameConstants.GANG_TYPE_ADD_GANG || type == GameConstants.GANG_TYPE_JIE_GANG) {
					card_type = Constants_MJ_JiYuan.HU_CARD_TYPE_GANG_PAO;
				}
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

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		long effect[] = new long[1];
		if (!(chr.opr_and(Constants_MJ_JiYuan.CHR_GANG_SHANG_HUA).is_empty())) { // 杠上开花
			effect[0] = Constants_MJ_JiYuan.CHR_GANG_SHANG_HUA;
		} else {
			effect[0] = Constants_MJ_JiYuan.CHR_ZI_MO;
		}
		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, effect, 1, GameConstants.INVALID_SEAT);

		// 手牌删掉
		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	protected int get_di_fen() {
		return has_rule(Constants_MJ_JiYuan.GAME_RULE_123_CHANG) ? 1 : 5;
	}

	@Override
	protected int get_banker_pre_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (4 + seat - 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	protected int get_banker_next_seat(int banker_seat) {
		int count = 0;
		int seat = banker_seat;
		do {
			count++;
			seat = (seat + 1) % 4;
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
		}
		return card;
	}

	protected int get_seat(int nValue, int seat_index) {
		return (seat_index + (nValue - 1) % 4) % 4;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_MJ_JiYuan.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		if (count == 1 && cards[0] > 0) { // 胡一张牌的都能被点炮
			isBian[seat_index] = 1;
		} else {
			isBian[seat_index] = 0;
		}

		return count;
	}

	public int getCardType(int seatIndex, int card, int[] cardIndex) {
		return 0;
	}

	@Override
	protected void test_cards() {

		int cards[] = new int[] { 0x11, 0x11, 0x12, 0x13, 0x14, 0x15, 0x15, 0x15, 0x22, 0x22, 0x27, 0x27, 0x27 };
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 13; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[j])] += 1;
			}
		}

		// testRealyCard(realyCards);

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

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
