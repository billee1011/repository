package com.cai.game.mj.yu.tong_cheng;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.GameConstants_TC;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.define.ECardType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJAIGameLogic;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class Table_TC extends AbstractMJTable {
	private static final long serialVersionUID = -7305160558193980076L;
	protected MJHandlerPiao_TC _handler_pao;
	private MJHandler_CaiGang _handler_cai_gang;

	public int[] default_pao = new int[getTablePlayerNumber()];
	public int[] start_hu_right = new int[getTablePlayerNumber()];
	public int[] _gang_score = new int[getTablePlayerNumber()];

	public StringBuffer hu_type_des = new StringBuffer();

	public int analyse_state;
	public static final int FROM_TING = 1;
	public static final int NORMAL_STATE = 2;

	public int[] player_hao_hua_count = new int[getTablePlayerNumber()]; // 豪华七对的数目，1个豪华5分

	public boolean[] is_si_lai_zi = new boolean[getTablePlayerNumber()]; // 胡牌时是否是光四赖子胡牌

	public Table_TC() {
		super(MJType.GAME_TYPE_TONG_CHENG);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_TC();
		_handler_out_card_operate = new MJHandlerOutCardOperate_TC();
		_handler_gang = new MJHandlerGang_TC();
		_handler_chi_peng = new MJHandlerChiPeng_TC();
		_handler_pao = new MJHandlerPiao_TC();
		_handler_cai_gang = new MJHandler_CaiGang();

		default_pao = new int[getTablePlayerNumber()];

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			default_pao[p] = 1;
		}
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		_player_result.nao[player.get_seat_index()] = nao;

		operate_player_data();

		if (_handler == _handler_dispath_card && player.get_seat_index() == _last_dispatch_player) {
			GameSchedule.put(new OutCardRunnable(getRoom_id(), _last_dispatch_player, _provide_card), GameConstants_KWX.GANG_LAST_CARD_DELAY,
					TimeUnit.MILLISECONDS);
		}

		return true;
	}

	@Override
	public boolean reset_init_data() {
		super.reset_init_data();

		start_hu_right = new int[getTablePlayerNumber()];
		_gang_score = new int[getTablePlayerNumber()];
		player_hao_hua_count = new int[getTablePlayerNumber()];

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_player_result.pao[p] = -1;
			_player_result.ming_gang_count[p] = 0;
			_player_result.an_gang_count[p] = 0;
			_player_result.biaoyan[p] = 0;
			_player_result.ziba[p] = 0;
		}

		return true;
	}

	public boolean on_game_start_real() {
		handler_refresh_all_player_data();

		_game_status = GameConstants_TC.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_TC.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_TC.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_TC.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_TC.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		seleMagicCard();

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i],
					i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		exe_dispatch_card(_current_player, GameConstants_TC.DispatchCard_Type_Tian_Hu, GameConstants_TC.DELAY_SEND_CARD_DELAY);
		// GameSchedule.put(new Runnable() {
		// @Override
		// public void run() {
		// seleMagicCard();
		// }
		// }, 1000, TimeUnit.MILLISECONDS);
		//
		// GameSchedule.put(new Runnable() {
		// @Override
		// public void run() {
		// exe_dispatch_card(_current_player, GameConstants_TC.WIK_NULL,
		// GameConstants_TC.DELAY_SEND_CARD_DELAY);
		// }
		// }, 1100, TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		load_common_status(roomResponse);
		load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间

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
				if (GRR._win_order[i] != 1) {
					continue;
				}
				for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
					for (int k = 0; k < getTablePlayerNumber(); k++) {
						lGangScore[k] += GRR._gang_score[i].scores[j][k]; // 杠牌，每个人的分数
					}
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
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

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					int tmpCard = GRR._chi_hu_card[i][j];
					if (_logic.is_magic_card(tmpCard))
						tmpCard += GameConstants_TC.CARD_ESPECIAL_TYPE_LAI_ZI;
					hc.addItem(tmpCard);
				}

				int tmpCard = GRR._chi_hu_card[i][0];
				if (_logic.is_magic_card(tmpCard))
					tmpCard += GameConstants_TC.CARD_ESPECIAL_TYPE_LAI_ZI;
				game_end.addHuCardData(tmpCard);
				game_end.addHuCardArray(hc);
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			StringBuffer b = new StringBuffer();
			int hu_count = 0;
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				if (GRR._chi_hu_rights[p].is_valid()) {
					hu_count++;
				}
			}

			if (hu_count > 1) {
				b.append("一炮多响");
			} else {
				b.append(hu_type_des);
			}

			game_end.addResultDes(b.toString());
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
				game_end.addCardsData(cs);

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

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(_gang_score[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addStartHuRight(start_hu_right[i]);
				game_end.addResultDes(GRR._result_des[i]);
				game_end.addJettonScore(_player_result.biaoyan[i]);

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
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		analyse_state = FROM_TING;

		int cbCardIndexTemp[] = new int[GameConstants_TC.MAX_INDEX];
		for (int i = 0; i < GameConstants_TC.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_TC.MAX_ZI;
		int real_max_ting_count = GameConstants_TC.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			if (has_rule(GameConstants_TC.GAME_RULE_TWO_MEN) && _logic.get_card_color(cbCurrentCard) == 1) {
				continue;
			}
			chr.set_empty();

			if (GameConstants_TC.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_TC.CHR_ZI_MO, seat_index)) {
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

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
	 * 
	 * Release_Room_Type_SEND = 1, //发起解散 Release_Room_Type_AGREE, //同意
	 * Release_Room_Type_DONT_AGREE, //不同意 Release_Room_Type_CANCEL,
	 * //还没开始,房主解散房间 Release_Room_Type_QUIT //还没开始,普通玩家退出房间
	 */
	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		if (is_sys()) {
			return handler_release_room_in_gold(player, opr_code);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();

			if (GameConstants.INVALID_SEAT == seat_index) {
				seat_index = getPlayerIndex(player.getAccount_id());

				if (GameConstants.INVALID_SEAT == seat_index) {
					MongoDBServiceImpl.getInstance().server_error_log(getRoom_id(), ELogType.roomLogicError, ThreadUtil.getStack(),
							player.getAccount_id(), SysGameTypeDict.getInstance().getGameDescByTypeIndex(getGameTypeIndex()), getGame_id());
				}
			}
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(3007);
		int delay = 150;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}

		switch (opr_code) {
		case GameConstants.Release_Room_Type_SEND: {
			// 发起解散
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能发起解散
				return false;
			}
			// 有人申请解散了
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

			// 取消之前的调度
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_request_release_time = System.currentTimeMillis() + delay * 1000;
			if (GRR == null) {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT), delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(
						new GameFinishRunnable(this.getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT), delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = this.get_players()[i];
			// if(pl==null || pl.isOnline()==false){
			// _gameRoomRecord.release_players[i] = 1;//同意
			// }
			// }
			int count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_gameRoomRecord.release_players[i] == 1) {// 都同意了

					count++;
				}
			}
			if (count == getTablePlayerNumber()) {
				if (GRR == null) {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					player = this.get_players()[j];
					if (player == null)
						continue;
					send_error_notify(j, 1, "游戏解散成功!");

				}
				return true;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setOperateCode(0);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setLeftTime(delay);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			// 没人发起解散
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			if (_gameRoomRecord.release_players[seat_index] == 1)
				return false;

			_gameRoomRecord.release_players[seat_index] = 1;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setReleaseTime(delay);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			this.send_response_to_room(roomResponse);

			int agree_count = 0;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] == 1) {
					agree_count++;
				}
			}
			if (getTablePlayerNumber() == 2 && agree_count != 2) {
				return false;
			}
			if ((getTablePlayerNumber() == 3 || getTablePlayerNumber() == 4) && agree_count < (getTablePlayerNumber() - 1)) {
				return false;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
			}

			return false;
		}

		case GameConstants.Release_Room_Type_DONT_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				// 游戏还没开始,不能同意解散
				return false;
			}
			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}
			_gameRoomRecord.release_players[seat_index] = 2;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(delay);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
			roomResponse.setOperateCode(1);
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			this.send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + this.get_players()[seat_index].getNick_name() + "]不同意解散");

			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {// 房主未开始游戏 解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经来时
				return false;
			}
			if (_cur_round == 0) {
				// _gameRoomRecord.request_player_seat =
				// MJGameConstants.INVALID_SEAT;

				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
				this.send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					Player p = this.get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
			} else {
				return false;

			}
		}
			break;
		case GameConstants.Release_Room_Type_QUIT: {// 玩家未开始游戏 退出
			if (GameConstants.GS_MJ_FREE != _game_status) {
				// 游戏已经开始
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_player(seat_index, quit_roomResponse);

			send_error_notify(seat_index, 2, "您已退出该游戏");

			if (seat_index == GameConstants.INVALID_SEAT) {

				MongoDBServiceImpl.getInstance().server_error_log(getRoom_id(), ELogType.unkownError, ThreadUtil.getStack(),
						player == null ? 0 : player.getAccount_id(), Arrays.toString(get_players()));

				send_error_notify(player, 2, "您已退出该游戏");

				if (player != null) {
					PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
				}
				if (_kick_schedule != null) {
					_kick_schedule.cancel(false);
					_kick_schedule = null;
				}

				seat_index = getPlayerIndex(player.getAccount_id());
				this.get_players()[seat_index] = null;
				_player_ready[seat_index] = 0;

				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				this.load_player_info_data(refreshroomResponse);
				//
				send_response_to_other(seat_index, refreshroomResponse);

				// 通知代理
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);

				return true;
			}

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}

			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;
			// WalkerGeek 允许少人模式
			super.init_less_param();

			if (_kick_schedule != null) {
				_kick_schedule.cancel(false);
				_kick_schedule = null;
			}
			// 刷新玩家
			RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
			refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
			this.load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			this.send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = this.get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			this.huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		}
			break;
		}

		return true;

	}

	@Override
	public void change_player_status(int seat_index, int st) {
		if (_trustee_schedule == null) {
			_trustee_schedule = new ScheduledFuture[GameConstants.GAME_PLAYER];
		}

		if (_trustee_schedule[seat_index] != null) {
			_trustee_schedule[seat_index].cancel(false);
			_trustee_schedule[seat_index] = null;
		}

		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态

		// 如果是出牌或者操作状态 需要倒计时托管&& !istrustee[seat_index]
		// if (has_rule(GameConstants_TC.GAME_RULE_ISTRUSTEE)
		// && (st == GameConstants.Player_Status_OPR_CARD || st ==
		// GameConstants.Player_Status_OUT_CARD)) {
		// _trustee_schedule[seat_index] = GameSchedule.put(new
		// TuoGuanRunnable(getRoom_id(), seat_index),
		// GameConstants.TRUSTEE_TIME_OUT_SECONDS,
		// TimeUnit.SECONDS);
		// }
	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		if (_playerStatus == null || istrustee == null) {
			send_error_notify(get_seat_index, 2, "游戏未开始,无法托管!");
			return false;
		}

		// 游戏开始前 无法托管
		if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) {
			send_error_notify(get_seat_index, 2, "游戏还未开始,无法托管!");
			return false;
		}

		istrustee[get_seat_index] = isTrustee;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		this.send_response_to_room(roomResponse);

		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

		if (is_match() || isCoinRoom() || isClubMatch()) {
			if (istrustee[get_seat_index] && this._current_player == get_seat_index) {
				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OUT_CARD) {
					int card = MJAIGameLogic.get_card(this, get_seat_index);
					if (card != 0) {
						this.handler_player_out_card(get_seat_index, card);
					}
				} else if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
				}
			} else if (istrustee[get_seat_index]) {
				if (_playerStatus[get_seat_index].get_status() == GameConstants.Player_Status_OPR_CARD) {
					MJAIGameLogic.AI_Operate_Card(this, get_seat_index);
				}
			}
		}

		if (_handler != null && isTrustee) {
			_handler.handler_be_set_trustee(this, get_seat_index);
		}
		return true;

	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_TC.GAME_RULE_PLAYER_2)) {
			return 2;
		}
		if (has_rule(GameConstants_TC.GAME_RULE_PLAYER_3)) {
			return 3;
		}

		if (playerNumber > 0) {
			return playerNumber;
		}
		return 4;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	public boolean exe_pao() {
		set_handler(_handler_pao);
		_handler_pao.exe(this);
		return true;
	}

	public boolean exe_cai_gang(int gang_seat_index, int gang_card_data) {
		set_handler(_handler_cai_gang);
		_handler_cai_gang.reset(gang_seat_index, gang_card_data, this);
		_handler_cai_gang.exe(this);
		return true;
	}

	public int get_hao_hua_count(int[] cards_index) {

		int count = 0;
		if (!has_rule(GameConstants_TC.GAME_RULE_HAO_HUA_QI_DUI)) {
			return count;
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 4)
				count++;
		}

		if (has_rule(GameConstants_TC.GAME_RULE_LAI_ZI)) {
			if (cards_index[_logic.get_magic_card_index(0)] == 4) {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
					int cbCardCount = cards_index[i];

					if (cbCardCount == 1 || cbCardCount == 3) {
						// 如果赖子有四张 但是有单牌 豪华减一
						count--;
						break;
					}
				}
			}
		}

		return count;
	}

	public boolean check_cai_gang(int seat_index) {
		if (!has_rule(GameConstants_TC.GAME_RULE_CAI_GANG)) {
			return false;
		}

		// 牌桌上的牌
		List<Integer> discards = Lists.newArrayList();
		for (int pdis = 0; pdis < getTablePlayerNumber(); pdis++) {
			for (int dis = 0; dis < GRR._discard_count[pdis]; dis++) {
				discards.add(GRR._discard_cards[pdis][dis]);
			}
			for (int wc = 0; wc < GRR._weave_count[pdis]; wc++) {
				switch (GRR._weave_items[pdis][wc].weave_kind) {
				case GameConstants.WIK_LEFT: {
					discards.add(GRR._weave_items[pdis][wc].center_card);
					discards.add(GRR._weave_items[pdis][wc].center_card + 1);
					discards.add(GRR._weave_items[pdis][wc].center_card + 2);
					break;
				}
				case GameConstants.WIK_CENTER: {
					discards.add(GRR._weave_items[pdis][wc].center_card - 1);
					discards.add(GRR._weave_items[pdis][wc].center_card);
					discards.add(GRR._weave_items[pdis][wc].center_card + 1);
					break;
				}
				case GameConstants.WIK_RIGHT: {
					discards.add(GRR._weave_items[pdis][wc].center_card - 2);
					discards.add(GRR._weave_items[pdis][wc].center_card - 1);
					discards.add(GRR._weave_items[pdis][wc].center_card);
					break;
				}
				case GameConstants.WIK_PENG: {
					discards.add(GRR._weave_items[pdis][wc].center_card);
					break;
				}
				case GameConstants.WIK_GANG: {
					if (GRR._weave_items[pdis][wc].public_card != 0) {
						discards.add(GRR._weave_items[pdis][wc].center_card);
					}
					break;
				}
				}
			}
		}

		boolean cai_gang = false;
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == seat_index) {
				continue;
			}
			if (_playerStatus[p]._hu_card_count == 0) {
				continue;
			}

			int[] cards = new int[42];
			int card_index = 0;
			if (_playerStatus[p]._hu_card_count == 1 && _playerStatus[p]._hu_cards[0] == -1) {
				for (int ph = 0; ph < GameConstants_TC.MAX_ZI; ph++) {
					if (GRR._cards_index[p][ph] > 0) {
						continue;
					}

					if (discards.contains(_logic.switch_to_card_data(ph))) {
						continue;
					}

					cards[card_index++] = _logic.switch_to_card_data(ph);
					cai_gang = true;
				}

				if (cai_gang) {
					_playerStatus[p].add_action(GameConstants_TC.WIK_CAI_GANG);
					_playerStatus[p].add_liang_card(cards, p);
				}
			} else {
				for (int ph = 0; ph < _playerStatus[p]._hu_card_count; ph++) {
					if (GRR._cards_index[p][_logic.switch_to_card_index(_playerStatus[p]._hu_cards[ph])] > 0) {
						continue;
					}

					if (discards.contains(_playerStatus[p]._hu_cards[ph])) {
						continue;
					}

					cards[card_index++] = _playerStatus[p]._hu_cards[ph];
					cai_gang = true;
				}

				if (cai_gang) {
					_playerStatus[p].add_action(GameConstants_TC.WIK_CAI_GANG);
					_playerStatus[p].add_liang_card(cards, p);
				}
			}
		}

		return cai_gang;
	}

	public int analyse_da_hu_first_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == GameConstants_TC.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_TC.CHR_ZI_MO);
		} else if (card_type == GameConstants_TC.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_TC.CHR_QING_GANG_HU);
		} else if (card_type == GameConstants_TC.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_TC.CHR_GNAG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants_TC.CHR_SHU_FAN);
		}

		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (_logic.is_valid_card(cur_card)) {
			int index = _logic.switch_to_card_index(cur_card);
			tmp_cards_index[index]++;
		}

		// 四赖子胡
		boolean can_si_lai_zi = false;
		if (has_rule(GameConstants_TC.GAME_RULE_LAI_ZI)) {
			int magicIndex = _logic.get_magic_card_index(0);
			if (tmp_cards_index[magicIndex] == 4) {
				can_si_lai_zi = true;
			}
		}

		if (card_type == GameConstants_TC.HU_CARD_TYPE_ZIMO && _send_card_count == 1 && has_rule(GameConstants_TC.GAME_RULE_TIAN_HU))
			chiHuRight.opr_or(GameConstants_TC.CHR_TIAN_HU);

		// 平胡
		boolean can_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		// 七小对
		boolean can_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (can_qi_xiao_dui)
			chiHuRight.opr_or(GameConstants_TC.CHR_XIAO_QI_DUI);
		// 清一色
		boolean can_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card) && (can_ping_hu || can_qi_xiao_dui);
		if (can_qing_yi_se)
			chiHuRight.opr_or(GameConstants_TC.CHR_QING_YI_SE);
		// 将一色
		boolean can_jiang_yi_se = _logic.check_hubei_jiang_yi_se(tmp_cards_index, weaveItems, weave_count) && (can_ping_hu || can_qi_xiao_dui);
		if (can_jiang_yi_se)
			chiHuRight.opr_or(GameConstants_TC.CHR_JIANG_YI_SE);

		if (!can_ping_hu && !can_qi_xiao_dui) {
			chiHuRight.set_empty();
			return 0;
		}

		if (can_si_lai_zi)
			chiHuRight.opr_or(GameConstants_TC.CHR_SI_LAI_ZI);

		// 硬平胡
		boolean can_ping_hu_ying = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index,
				_logic.switch_to_card_index(cur_card), null, 0);
		// 硬七小对
		boolean can_qi_xiao_dui_ying = _logic.check_hubei_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		// 硬清一色
		boolean can_qing_yi_se_ying = _logic.check_hubei_ying_qing_yi_se(tmp_cards_index, weaveItems, weave_count)
				&& (can_ping_hu_ying || can_qi_xiao_dui_ying);
		// 硬将一色
		boolean can_jiang_yi_se_ying = _logic.check_hubei_ying_jiang_yi_se(tmp_cards_index, weaveItems, weave_count)
				&& (can_ping_hu_ying || can_qi_xiao_dui_ying);

		if (analyse_state == NORMAL_STATE) {
			player_hao_hua_count[_seat_index] = 0;
			if (can_qi_xiao_dui)
				player_hao_hua_count[_seat_index] = get_hao_hua_count(tmp_cards_index);
		}

		if (can_qi_xiao_dui || can_qing_yi_se || can_jiang_yi_se) {
			boolean can_ying_hu = true;

			if (can_qi_xiao_dui && !can_qi_xiao_dui_ying)
				can_ying_hu = false;
			if (can_qing_yi_se && !can_qing_yi_se_ying)
				can_ying_hu = false;
			if (can_jiang_yi_se && !can_jiang_yi_se_ying)
				can_ying_hu = false;

			if (can_ying_hu && has_rule(GameConstants_TC.GAME_RULE_LAI_ZI))
				chiHuRight.opr_or(GameConstants_TC.CHR_YING_HU);
		} else {
			boolean need_to_continue = true;
			if (can_ping_hu_ying) {
				boolean can_258_ying = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), null, 0);
				if (can_258_ying && has_rule(GameConstants_TC.GAME_RULE_LAI_ZI)) {
					chiHuRight.opr_or(GameConstants_TC.CHR_YING_HU);
					need_to_continue = false;
				}
			}
			if (can_ping_hu && need_to_continue) {
				boolean can_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
				if (!can_258 && !can_si_lai_zi) {
					chiHuRight.set_empty();
					return 0;
				}
				if (!can_258 && can_si_lai_zi) {
					return 0;
				}
				if (!can_258 && can_si_lai_zi)
					is_si_lai_zi[_seat_index] = true;
			}
		}

		if (has_rule(GameConstants_TC.GAME_RULE_HAI_DI_HU) && GRR._left_card_count <= getTablePlayerNumber()) {
			chiHuRight.opr_or(GameConstants_TC.CHR_HAI_DI_HU);
		}

		int tmp_score = getFanShu(_seat_index, chiHuRight);

		return tmp_score;
	}

	public int analyse_ying_hu_first_new(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight,
			int card_type, int _seat_index) {
		if (card_type == GameConstants_TC.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_TC.CHR_ZI_MO);
		} else if (card_type == GameConstants_TC.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_TC.CHR_QING_GANG_HU);
		} else if (card_type == GameConstants_TC.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_TC.CHR_GNAG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants_TC.CHR_SHU_FAN);
		}

		int[] tmp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		if (_logic.is_valid_card(cur_card)) {
			int index = _logic.switch_to_card_index(cur_card);
			tmp_cards_index[index]++;
		}

		// 四赖子胡
		boolean can_si_lai_zi = false;
		if (has_rule(GameConstants_TC.GAME_RULE_LAI_ZI)) {
			int magicIndex = _logic.get_magic_card_index(0);
			if (tmp_cards_index[magicIndex] == 4) {
				can_si_lai_zi = true;
			}
		}

		if (card_type == GameConstants_TC.HU_CARD_TYPE_ZIMO && _send_card_count == 1 && has_rule(GameConstants_TC.GAME_RULE_TIAN_HU))
			chiHuRight.opr_or(GameConstants_TC.CHR_TIAN_HU);

		// 平胡
		boolean can_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		// 七小对
		boolean can_qi_xiao_dui = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		// 清一色
		boolean can_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card) && (can_ping_hu || can_qi_xiao_dui);
		// 将一色
		boolean can_jiang_yi_se = _logic.check_hubei_jiang_yi_se(tmp_cards_index, weaveItems, weave_count) && (can_ping_hu || can_qi_xiao_dui);

		if (!can_ping_hu && !can_qi_xiao_dui) {
			chiHuRight.set_empty();
			return 0;
		}

		if (can_si_lai_zi)
			chiHuRight.opr_or(GameConstants_TC.CHR_SI_LAI_ZI);

		// 硬平胡
		boolean can_ping_hu_ying = AnalyseCardUtil.analyse_win_by_cards_index_4_hand_cards_index_length(cards_index,
				_logic.switch_to_card_index(cur_card), null, 0);
		// 硬七小对
		boolean can_qi_xiao_dui_ying = _logic.check_hubei_ying_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		// 硬清一色
		boolean can_qing_yi_se_ying = _logic.check_hubei_ying_qing_yi_se(tmp_cards_index, weaveItems, weave_count)
				&& (can_ping_hu_ying || can_qi_xiao_dui_ying);
		// 硬将一色
		boolean can_jiang_yi_se_ying = _logic.check_hubei_ying_jiang_yi_se(tmp_cards_index, weaveItems, weave_count)
				&& (can_ping_hu_ying || can_qi_xiao_dui_ying);

		if (analyse_state == NORMAL_STATE) {
			player_hao_hua_count[_seat_index] = 0;
			if (can_qi_xiao_dui)
				player_hao_hua_count[_seat_index] = get_hao_hua_count(tmp_cards_index);
		}

		if (can_qi_xiao_dui_ying || can_qing_yi_se_ying || can_jiang_yi_se_ying) {
			if (has_rule(GameConstants_TC.GAME_RULE_LAI_ZI))
				chiHuRight.opr_or(GameConstants_TC.CHR_YING_HU);

			if (can_qi_xiao_dui_ying)
				chiHuRight.opr_or(GameConstants_TC.CHR_XIAO_QI_DUI);
			if (can_qing_yi_se_ying)
				chiHuRight.opr_or(GameConstants_TC.CHR_QING_YI_SE);
			if (can_jiang_yi_se_ying)
				chiHuRight.opr_or(GameConstants_TC.CHR_JIANG_YI_SE);
		} else if (can_qi_xiao_dui || can_qing_yi_se || can_jiang_yi_se) {
			if (can_qi_xiao_dui)
				chiHuRight.opr_or(GameConstants_TC.CHR_XIAO_QI_DUI);
			if (can_qing_yi_se)
				chiHuRight.opr_or(GameConstants_TC.CHR_QING_YI_SE);
			if (can_jiang_yi_se)
				chiHuRight.opr_or(GameConstants_TC.CHR_JIANG_YI_SE);
		} else {
			boolean need_to_continue = true;
			if (can_ping_hu_ying) {
				boolean can_258_ying = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), null, 0);
				if (can_258_ying && has_rule(GameConstants_TC.GAME_RULE_LAI_ZI)) {
					chiHuRight.opr_or(GameConstants_TC.CHR_YING_HU);
					need_to_continue = false;
				}
			}
			if (can_ping_hu && need_to_continue) {
				boolean can_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count());
				if (!can_258 && !can_si_lai_zi) {
					chiHuRight.set_empty();
					return 0;
				}
				if (!can_258 && can_si_lai_zi)
					return 0;
				if (!can_258 && can_si_lai_zi)
					is_si_lai_zi[_seat_index] = true;
			}
		}
		if (has_rule(GameConstants_TC.GAME_RULE_HAI_DI_HU) && GRR._left_card_count <= getTablePlayerNumber()) {
			chiHuRight.opr_or(GameConstants_TC.CHR_HAI_DI_HU);
		}

		int tmp_score = getFanShu(_seat_index, chiHuRight);

		return tmp_score;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (analyse_state == NORMAL_STATE)
			is_si_lai_zi[_seat_index] = false;

		chiHuRight.set_empty();
		int tmp_score = analyse_da_hu_first_new(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index);
		if (analyse_state == NORMAL_STATE) {
			GRR._chi_hu_rights[_seat_index] = chiHuRight;
		}

		chiHuRight.set_empty();
		int tmp_score_2 = analyse_ying_hu_first_new(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index);

		if (tmp_score_2 > tmp_score) {
			tmp_score = tmp_score_2;

			if (analyse_state == NORMAL_STATE) {
				GRR._chi_hu_rights[_seat_index] = chiHuRight;
			}
		} else {
			chiHuRight.set_empty();
			analyse_da_hu_first_new(cards_index, weaveItems, weave_count, cur_card, chiHuRight, card_type, _seat_index);
			if (analyse_state == NORMAL_STATE) {
				GRR._chi_hu_rights[_seat_index] = chiHuRight;
			}
		}

		return tmp_score > 0 ? GameConstants.WIK_CHI_HU : 0;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = getFanShu(seat_index, chr);

		countCardType(chr, seat_index);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = wFanShu;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = getDiFen();

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore * wFanShu;
				GRR._start_hu_score[i] -= s;
				GRR._start_hu_score[seat_index] += s;

				float gang_score = (1 * _player_result.ming_gang_count[seat_index] * lChiHuScore)
						+ (2 * _player_result.an_gang_count[seat_index] * lChiHuScore);
				if (!chr.opr_and(GameConstants_TC.CHR_JIANG_YI_SE).is_empty() || !chr.opr_and(GameConstants_TC.CHR_QING_YI_SE).is_empty())
					gang_score = 0;
				_gang_score[i] -= gang_score;
				_gang_score[seat_index] += gang_score;
				s += gang_score;

				int pao_score = _player_result.pao[seat_index] + _player_result.pao[i];
				start_hu_right[i] -= pao_score;
				start_hu_right[seat_index] += pao_score;
				s += pao_score;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		} else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_TC.CHR_FANG_PAO);
			float s = lChiHuScore * wFanShu;
			GRR._start_hu_score[provide_index] -= s;
			GRR._start_hu_score[seat_index] += s;

			float gang_score = (1 * _player_result.ming_gang_count[seat_index] * lChiHuScore)
					+ (2 * _player_result.an_gang_count[seat_index] * lChiHuScore);
			if (!chr.opr_and(GameConstants_TC.CHR_JIANG_YI_SE).is_empty() || !chr.opr_and(GameConstants_TC.CHR_QING_YI_SE).is_empty())
				gang_score = 0;
			_gang_score[provide_index] -= gang_score;
			_gang_score[seat_index] += gang_score;
			s += gang_score;

			int pao_score = _player_result.pao[seat_index] + _player_result.pao[provide_index];
			start_hu_right[provide_index] -= pao_score;
			start_hu_right[seat_index] += pao_score;
			s += pao_score;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants_TC.INVALID_VALUE);
	}

	public int getFanShu(int seat_index, ChiHuRight chr) {
		int fanShu = 0;
		if (!chr.opr_and(GameConstants_TC.CHR_TIAN_HU).is_empty()) {
			if (!chr.opr_and(GameConstants_TC.CHR_YING_HU).is_empty()) {
				// 天胡+硬胡
				fanShu += 5 * 2;
			} else {
				// 天胡+软胡
				fanShu += 5;
			}

			if (!chr.opr_and(GameConstants_TC.CHR_XIAO_QI_DUI).is_empty()) {
				fanShu += 5;
			}
			if (!chr.opr_and(GameConstants_TC.CHR_QING_YI_SE).is_empty()) {
				fanShu += 5;
			}
			if (!chr.opr_and(GameConstants_TC.CHR_JIANG_YI_SE).is_empty()) {
				fanShu += 10;
			}
			fanShu = (fanShu == 0 && !is_si_lai_zi[seat_index]) ? 1 : fanShu;
			fanShu += player_hao_hua_count[seat_index] * 5;
			if (has_rule(GameConstants_TC.GAME_RULE_FOUR_LAI_ZI_REWARD) && !chr.opr_and(GameConstants_TC.CHR_SI_LAI_ZI).is_empty()) {
				fanShu += 5;
			}
		} else {
			if (!chr.opr_and(GameConstants_TC.CHR_YING_HU).is_empty()) {
				// 硬胡
				boolean checked = false;
				if (!chr.opr_and(GameConstants_TC.CHR_JIANG_YI_SE).is_empty()) {
					fanShu += 10;
					if (!checked) {
						fanShu *= 2;
						checked = true;
					}
				}
				if (!chr.opr_and(GameConstants_TC.CHR_XIAO_QI_DUI).is_empty()) {
					fanShu += 5;
					if (!checked) {
						fanShu *= 2;
						checked = true;
					}
				}
				if (!chr.opr_and(GameConstants_TC.CHR_QING_YI_SE).is_empty()) {
					fanShu += 5;
					if (!checked) {
						fanShu *= 2;
						checked = true;
					}
				}
				fanShu = (fanShu == 0 && !is_si_lai_zi[seat_index]) ? 1 : fanShu;
				if (!checked)
					fanShu *= 2;
				fanShu += player_hao_hua_count[seat_index] * 5;
				if (has_rule(GameConstants_TC.GAME_RULE_FOUR_LAI_ZI_REWARD) && !chr.opr_and(GameConstants_TC.CHR_SI_LAI_ZI).is_empty()) {
					fanShu += 5;
				}
			} else {
				// 软胡
				if (!chr.opr_and(GameConstants_TC.CHR_XIAO_QI_DUI).is_empty()) {
					fanShu += 5;
				}
				if (!chr.opr_and(GameConstants_TC.CHR_QING_YI_SE).is_empty()) {
					fanShu += 5;
				}
				if (!chr.opr_and(GameConstants_TC.CHR_JIANG_YI_SE).is_empty()) {
					fanShu += 10;
				}
				fanShu = (fanShu == 0 && !is_si_lai_zi[seat_index]) ? 1 : fanShu;
				fanShu += player_hao_hua_count[seat_index] * 5;
				if (has_rule(GameConstants_TC.GAME_RULE_FOUR_LAI_ZI_REWARD) && !chr.opr_and(GameConstants_TC.CHR_SI_LAI_ZI).is_empty()) {
					fanShu += 5;
				}
			}
		}

		return fanShu;
	}

	private int getDiFen() {
		if (has_rule(GameConstants_TC.GAME_RULE_DI_FEN_1)) {
			return 1;
		}
		if (has_rule(GameConstants_TC.GAME_RULE_DI_FEN_2)) {
			return 2;
		}
		if (has_rule(GameConstants_TC.GAME_RULE_DI_FEN_5)) {
			return 5;
		}

		return 1;
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
		for (int i = 0; i < curPlayerStatus._action_count; i++) {
			roomResponse.addActions(curPlayerStatus._action[i]);
		}
		// 组合数据
		for (int i = 0; i < curPlayerStatus._weave_count; i++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setCenterCard(curPlayerStatus._action_weaves[i].center_card);
			weaveItem_item.setProvidePlayer(curPlayerStatus._action_weaves[i].provide_player);
			weaveItem_item.setPublicCard(curPlayerStatus._action_weaves[i].public_card);
			weaveItem_item.setWeaveKind(curPlayerStatus._action_weaves[i].weave_kind);
			for (int wd = 0; wd < curPlayerStatus._action_weaves[i].weave_card.length; wd++) {
				if (curPlayerStatus._action_weaves[i].weave_card[wd] != 0) {
					weaveItem_item.addWeaveCard(curPlayerStatus._action_weaves[i].weave_card[wd]);
				}
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		boolean qiang_gang_hu = false;
		hu_type_des = new StringBuffer().append("平胡");
		boolean clear_hu_type_des = false;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_TC.CHR_QING_GANG_HU) {
						result.append(" 抢杠胡");
						qiang_gang_hu = true;
					}
					if (type == GameConstants_TC.CHR_GNAG_KAI) {
						result.append(" 杠上花");
					}
					if (type == GameConstants_TC.CHR_YING_HU) {
						result.append(" 硬胡");
					}
					if (type == GameConstants_TC.CHR_QING_YI_SE) {
						result.append(" 清一色");
						if (!clear_hu_type_des) {
							clear_hu_type_des = true;
							hu_type_des = new StringBuffer().append("清一色");
						} else {
							hu_type_des.append("清一色");
						}
					}
					if (type == GameConstants_TC.CHR_XIAO_QI_DUI) {
						String tmp = "";
						if (player_hao_hua_count[player] == 3)
							tmp = "三豪华";
						if (player_hao_hua_count[player] == 2)
							tmp = "双豪华";
						if (player_hao_hua_count[player] == 1)
							tmp = "豪华";
						tmp += "七对";
						result.append(" " + tmp);
						if (!clear_hu_type_des) {
							clear_hu_type_des = true;
							hu_type_des = new StringBuffer().append(tmp);
						} else {
							hu_type_des.append(tmp);
						}
					}
					if (type == GameConstants_TC.CHR_JIANG_YI_SE) {
						result.append(" 将一色");
						if (!clear_hu_type_des) {
							clear_hu_type_des = true;
							hu_type_des = new StringBuffer().append("将一色");
						} else {
							hu_type_des.append("将一色");
						}
					}
					if (has_rule(GameConstants_TC.GAME_RULE_FOUR_LAI_ZI_REWARD) && type == GameConstants_TC.CHR_SI_LAI_ZI) {
						result.append(" 四赖子胡");
					}
					if (type == GameConstants_TC.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == GameConstants_TC.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_TC.CHR_SHU_FAN) {
						result.append(" 捉铳");
					}

				} else if (type == GameConstants_TC.CHR_FANG_PAO) {
					if (qiang_gang_hu) {
						result.append(" 被抢杠");
					} else {
						result.append(" 放铳");
					}
				}
			}

			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;

			if (GRR != null) {
				for (int tmpPlayer = 0; tmpPlayer < getTablePlayerNumber(); tmpPlayer++) {
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
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, UniversalConstants.INVALID_SEAT);

		operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);

		for (int i = 0; i < hand_card_count; i++)
			if (_logic.is_magic_card(cards[i]))
				cards[i] += GameConstants_TC.CARD_ESPECIAL_TYPE_LAI_ZI;

		int huCard = operate_card;
		if (_logic.is_magic_card(huCard))
			huCard += GameConstants_TC.CARD_ESPECIAL_TYPE_LAI_ZI;
		cards[hand_card_count++] = huCard;

		operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();

		// 庄家选择
		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;
		if (has_rule(GameConstants_TC.GAME_RULE_TWO_MEN)) {
			_repertory_card = new int[GameConstants_TC.CARD_DATA_WAN_TONG.length];
			shuffle(_repertory_card, GameConstants_TC.CARD_DATA_WAN_TONG);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l,
								this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {
			logger.error("card_log", e);
		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}

		return on_game_start();
	}

	public int getSelectPaoFen() {
		if (has_rule(GameConstants_TC.GAME_RULE_PAO_1)) {
			return 1;
		}
		if (has_rule(GameConstants_TC.GAME_RULE_PAO_2)) {
			return 2;
		}
		if (has_rule(GameConstants_TC.GAME_RULE_PAO_3)) {
			return 3;
		}

		return 0;
	}

	@Override
	protected boolean on_game_start() {
		if (getSelectPaoFen() != 0) {
			return exe_pao();
		}
		if (has_rule(GameConstants_TC.GAME_RULE_DING_PAO_1)) {
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				_player_result.pao[p] = 1;
			}
		}
		if (has_rule(GameConstants_TC.GAME_RULE_DING_PAO_2)) {
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				_player_result.pao[p] = 2;
			}
		}
		if (has_rule(GameConstants_TC.GAME_RULE_DING_PAO_3)) {
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				_player_result.pao[p] = 3;
			}
		}
		if (has_rule(GameConstants_TC.GAME_RULE_NON_PAO)) {
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				_player_result.pao[p] = 0;
			}
		}

		return on_game_start_real();
	}

	public int analyse_gang(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult, boolean check_weave,
			int cards_abandoned_gang[]) {
		int cbActionMask = GameConstants.WIK_NULL;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
				continue;
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants.WIK_GANG;
				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;
				gangCardResult.type[index] = GameConstants_TC.TC_GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind == GameConstants.WIK_PENG) {
					for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
						if (cards_index[j] != 1 || _logic.is_magic_index(j) || cards_abandoned_gang[j] != 0) {
							continue;
						} else {
							if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
								cbActionMask |= GameConstants.WIK_GANG;

								int index = gangCardResult.cbCardCount++;
								gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
								gangCardResult.isPublic[index] = 1;// 明刚
								gangCardResult.type[index] = GameConstants_TC.TC_GANG_TYPE_ADD_GANG;
								break;
							}
						}
					}
				}
			}
		}

		return cbActionMask;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			ChiHuRight chr = GRR._chi_hu_rights[i];
			chr.set_empty();

			int cbWeaveCount = GRR._weave_count[i];
			action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants_TC.HU_CARD_TYPE_QIANG_GANG,
					i);

			if (action != 0) {
				int hu_fan = getFanShu(i, chr);
				int abandoned_hu_fan_max = 0;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_TC.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == 0) {
						break;
					}

					ChiHuRight chiHuRight = new ChiHuRight();
					analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, tmp_cards_data_hu[x], chiHuRight,
							GameConstants_TC.HU_CARD_TYPE_QIANG_GANG, i);
					abandoned_hu_fan_max = abandoned_hu_fan_max > getFanShu(i, chiHuRight) ? abandoned_hu_fan_max : getFanShu(i, chiHuRight);
				}

				if (hu_fan > abandoned_hu_fan_max) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);
					chr.opr_or(GameConstants_TC.CHR_QING_GANG_HU);
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

	@Override
	public boolean handler_refresh_all_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		load_player_info_data(roomResponse);
		send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}

	/**
	 * 后期需要添加的摇骰子的效果
	 */
	@Override
	public void show_tou_zi(int seat_index) {
		tou_zi_dian_shu[0] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;
		tou_zi_dian_shu[1] = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6 + 1;

		// operate_tou_zi_effect(tou_zi_dian_shu[0], tou_zi_dian_shu[1],
		// time_for_tou_zi_animation, time_for_tou_zi_fade);
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		tou_zi_dian_shu = new int[2];
		// 洗完牌之后，发牌之前，需要摇骰子
		show_tou_zi(GRR._banker_player);

		int send_count;
		int have_send_count = 0;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			send_count = (GameConstants.MAX_COUNT - 1);
			GRR._left_card_count -= send_count;

			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;
		}

		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	private boolean seleMagicCard() {
		if (has_rule(GameConstants_TC.GAME_RULE_NON_LAI_ZI)) {
			return false;
		}

		_logic.clean_magic_cards();

		int _da_dian_card = _repertory_card[_all_card_len - 1 - (tou_zi_dian_shu[0] + tou_zi_dian_shu[1]) * 2];

		if (DEBUG_CARDS_MODE)
			_da_dian_card = 0x14;

		if (DEBUG_MAGIC_CARD) {
			_da_dian_card = magic_card_decidor;
			DEBUG_MAGIC_CARD = false;
		}

		operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

		int card_next = 0;

		int cur_value = _logic.get_card_value(_da_dian_card);
		if (cur_value == 9) {
			card_next = _da_dian_card - 8;
		} else {
			card_next = _da_dian_card + 1;
		}

		_logic.add_magic_card_index(_logic.switch_to_card_index(card_next));
		GRR._especial_show_cards[GRR._especial_card_count++] = _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_CAN_WIN;
		GRR._especial_show_cards[GRR._especial_card_count++] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				operate_show_card(_cur_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
			}
		}, 2000, TimeUnit.MILLISECONDS);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (_logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		return true;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants_TC.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_TC.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					int[] cards_index_temp = Arrays.copyOf(GRR._cards_index[i], GRR._cards_index[i].length);
					cards_index_temp[_logic.switch_to_card_index(card)] -= 2;
					int hand_cards_count = _logic.get_card_count_by_index(cards_index_temp);

					int hand_hz_count = 0;
					for (int mc = 0; mc < _logic.get_magic_card_count(); mc++) {
						hand_hz_count += cards_index_temp[_logic.get_magic_card_index(mc)];
					}

					if (hand_cards_count != hand_hz_count && _player_result.nao[i] != 1) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_TC.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			boolean can_hu_this_card = true;
			int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
			for (int x = 0; x < GameConstants_TC.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data_hu[x] == card) {
					can_hu_this_card = false;
					break;
				}
			}
			if (can_hu_this_card) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				analyse_state = NORMAL_STATE;

				int card_type = GameConstants_TC.HU_CARD_TYPE_JIE_PAO;
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);
				if (action != 0) {
					int hu_fan = getFanShu(i, chr);
					int abandoned_hu_fan_max = 0;
					for (int x = 0; x < GameConstants_TC.MAX_ABANDONED_CARDS_COUNT; x++) {
						if (tmp_cards_data_hu[x] == 0) {
							break;
						}

						ChiHuRight chiHuRight = new ChiHuRight();
						analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, tmp_cards_data_hu[x], chiHuRight, card_type, i);
						abandoned_hu_fan_max = abandoned_hu_fan_max > getFanShu(i, chiHuRight) ? abandoned_hu_fan_max : getFanShu(i, chiHuRight);
					}

					if (hu_fan > abandoned_hu_fan_max) {
						chr.set_empty();
						analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

						_playerStatus[i].add_action(GameConstants_TC.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			}
		}

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();

		int[] cards_index_temp = Arrays.copyOf(GRR._cards_index[chi_seat_index], GRR._cards_index[chi_seat_index].length);
		int hand_cards_count = _logic.get_card_count_by_index(cards_index_temp);
		int hand_hz_count = 0;
		for (int mc = 0; mc < _logic.get_magic_card_count(); mc++) {
			hand_hz_count += cards_index_temp[_logic.get_magic_card_index(mc)];
		}

		action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
		if (_player_result.nao[chi_seat_index] == 1) {
			action = GameConstants.WIK_NULL;
		}

		if ((action & GameConstants.WIK_LEFT) != 0) {
			cards_index_temp = Arrays.copyOf(GRR._cards_index[chi_seat_index], GRR._cards_index[chi_seat_index].length);
			int cbRemoveCard[] = new int[] { card + 1, card + 2 };
			if (!_logic.remove_cards_by_index(cards_index_temp, cbRemoveCard, 2)) {
				log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			if (hand_cards_count != hand_hz_count) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
			}
		}

		if ((action & GameConstants.WIK_CENTER) != 0) {
			cards_index_temp = Arrays.copyOf(GRR._cards_index[chi_seat_index], GRR._cards_index[chi_seat_index].length);
			int cbRemoveCard[] = new int[] { card - 1, card + 1 };
			if (!_logic.remove_cards_by_index(cards_index_temp, cbRemoveCard, 2)) {
				log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			if (hand_cards_count != hand_hz_count) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
			}
		}

		if ((action & GameConstants.WIK_RIGHT) != 0) {
			cards_index_temp = Arrays.copyOf(GRR._cards_index[chi_seat_index], GRR._cards_index[chi_seat_index].length);
			int cbRemoveCard[] = new int[] { card - 1, card - 2 };
			if (!_logic.remove_cards_by_index(cards_index_temp, cbRemoveCard, 2)) {
				log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			if (hand_cards_count != hand_hz_count) {
				_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
				_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
			}
		}

		if (_playerStatus[chi_seat_index].has_action()) {
			bAroseAction = true;
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants_TC.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x15, 0x15, 0x15, 0x01, 0x02, 0x03, 0x03, 0x03, 0x07, 0x12, 0x13, 0x1, 0x14 };
		int[] cards_of_player1 = new int[] { 0x11, 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x12, 0x13, 0x13, 0x13, 0x13, 0x14 };
		int[] cards_of_player2 = new int[] { 0x15, 0x15, 0x15, 0x16, 0x16, 0x16, 0x21, 0x22, 0x23, 0x24, 0x25, 0x28, 0x28 };
		int[] cards_of_player3 = new int[] { 0x15, 0x15, 0x15, 0x16, 0x16, 0x16, 0x21, 0x22, 0x23, 0x24, 0x25, 0x28, 0x28 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_KWX.MAX_INDEX; j++) {
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

	public boolean is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		if (cbWeaveCount != 0)
			return false;

		int cbReplaceCount = 0;
		int nGenCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_logic.get_magic_card_count() > 0) {
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					if (i == _logic.get_magic_card_index(m))
						continue;

					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		if (_logic.get_magic_card_count() > 0) {
			int count = 0;

			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}

			if (cbReplaceCount > count) {
				return false;
			}
		} else {
			if (cbReplaceCount > 0)
				return false;
		}

		if (nGenCount > 0) {
			if (nGenCount >= 2) {
				return true;
			}
			return true;
		} else {
			return true;
		}
	}
}
