package com.cai.game.mj.henan.newzhenzhou;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.define.ELogType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PerformanceTimer;
import com.cai.common.util.SpringService;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.handler.MJHandlerFinish;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJTable_New_ZhenZhou extends AbstractMJTable {
	private static final long serialVersionUID = 9171444142458092230L;

	private PerformanceTimer timer;

	public MJHandlerPao_New_ZhenZhou _handler_pao_henna;
	public MJHandlerQiShouHun_New_ZhenZhou _handler_qishou_hun;
	public MJHandlerHun_New_ZhenZhou _handler_hun;

	public int gang_provider = -2;

	public int get_dismis_delay() {
		if (getRuleValue(GameConstants.GAME_RULE_HENAN_THIRTY_SECONDS) != 0)
			return 30;
		if (getRuleValue(GameConstants.GAME_RULE_HENAN_ONE_MINUTE) != 0)
			return 60;
		if (getRuleValue(GameConstants.GAME_RULE_HENAN_TWO_MINUTE) != 0)
			return 120;
		if (getRuleValue(GameConstants.GAME_RULE_HENAN_THREE_MINUTE) != 0)
			return 180;
		return 60;
	}

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

		int delay = get_dismis_delay();

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
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_WAIT_TIME_OUT),
						delay, TimeUnit.SECONDS);
			} else {
				_release_scheduled = GameSchedule.put(new GameFinishRunnable(getRoom_id(), seat_index, GameConstants.Game_End_RELEASE_PLAY_TIME_OUT),
						delay, TimeUnit.SECONDS);
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;// 同意

			// 不在线的玩家默认同意
			// for (int i = 0; i < MJGameConstants.GAME_PLAYER; i++) {
			// Player pl = get_players()[i];
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
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					player = get_players()[j];
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
			send_response_to_room(roomResponse);

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

			send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			if (GRR == null) {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
			} else {
				handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
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
			send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;
			if (_release_scheduled != null)
				_release_scheduled.cancel(false);
			_release_scheduled = null;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < getTablePlayerNumber(); j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + get_players()[seat_index].getNick_name() + "]不同意解散");

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
				send_response_to_room(roomResponse);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					Player p = get_players()[i];
					if (p == null)
						continue;
					if (i == seat_index) {
						send_error_notify(i, 2, "游戏已解散");
					} else {
						send_error_notify(i, 2, "游戏已被创建者解散");
					}

				}
				huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
				// 删除房间
				PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
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
					PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), player.getAccount_id());
				}
				if (_kick_schedule != null) {
					_kick_schedule.cancel(false);
					_kick_schedule = null;
				}

				seat_index = getPlayerIndex(player.getAccount_id());
				get_players()[seat_index] = null;
				_player_ready[seat_index] = 0;

				// 刷新玩家
				RoomResponse.Builder refreshroomResponse = RoomResponse.newBuilder();
				refreshroomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
				load_player_info_data(refreshroomResponse);
				//
				send_response_to_other(seat_index, refreshroomResponse);

				// 通知代理
				refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);

				return true;
			}

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(getRoom_id(), player.getAccount_id());
			}

			get_players()[seat_index] = null;
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
			load_player_info_data(refreshroomResponse);
			//
			send_response_to_other(seat_index, refreshroomResponse);

			// 通知代理
			refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, false);
		}
			break;
		case GameConstants.Release_Room_Type_PROXY: {
			// 游戏还没开始,不能解散
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			send_response_to_room(roomResponse);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Player p = get_players()[i];
				if (p == null)
					continue;
				send_error_notify(i, 2, "游戏已被创建者解散");

			}
			huan_dou(GameConstants.Game_End_RELEASE_NO_BEGIN);
			// 删除房间
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());

		}
			break;
		}

		return true;

	}

	@Override
	public void fixBugTemp(int seat_index) {
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				int delay = get_dismis_delay();

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				send_response_to_player(seat_index, roomResponse);
			}
		}
	}

	public boolean exe_dispatch_card_gang(int seat_index, int type, int delay, int provide_index) {
		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			set_handler(_handler_dispath_card);
			_handler_dispath_card.reset_status(seat_index, type, provide_index, true);
			_handler.exe(this);
		}

		return true;
	}

	@Override
	public int getTablePlayerNumber() {
		if (getRuleValue(GameConstants.GAME_RULE_HUNAN_THREE) == 1) {
			return 3;
		}
		return 4;
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new MJHandlerDispatchCard_New_ZhenZhou();
		_handler_out_card_operate = new MJHandlerOutCardOperate_New_ZhenZhou();
		_handler_gang = new MJHandlerGang_New_ZhenZhou();
		_handler_chi_peng = new MJHandlerChiPeng_New_ZhenZhou();

		_handler_qishou_hun = new MJHandlerQiShouHun_New_ZhenZhou();
		_handler_pao_henna = new MJHandlerPao_New_ZhenZhou();
		_handler_hun = new MJHandlerHun_New_ZhenZhou();

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.pao[i] = 0;
		}

		_handler_finish = new MJHandlerFinish();
	}

	@Override
	protected void initBanker() {
		// if (has_rule(GameConstants.GAME_RULE_HENAN_THREE)) {
		// int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER -
		// 1);
		// _cur_banker = banker;
		// } else {
		// int banker = RandomUtil.getRandomNumber(GameConstants.GAME_PLAYER);
		// _cur_banker = banker;
		// }
	}

	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 0) {
			// 游戏开始时间
			timer = new PerformanceTimer();
		}

		reset_init_data();

		progress_banker_select();

		_game_status = GameConstants.GS_MJ_PLAY;

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_DAI_FENG_LZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_DAI_FENG_LZ);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_BU_DAI_FENG_LZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_BU_DAI_FENG_LZ);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._cards_index[i].length; j++) {
				if (GRR._cards_index[i][j] == 4) {
					MongoDBServiceImpl.getInstance().card_log(get_players()[i], ECardType.anLong, "", GRR._cards_index[i][j], 0l, getRoom_id());
				}
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}


		on_game_start();

		return true;
	}

	@Override
	public boolean on_game_start() {
		_logic.clean_magic_cards();

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_PAO)) {
			set_handler(_handler_pao_henna);
			_handler_pao_henna.exe(this);
			return true;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;
				_player_result.qiang[i] = 0;
			}
		}

		GRR._banker_player = _current_player = _cur_banker;

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

			if (_cur_round == 1) {
				load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			send_response_to_player(i, roomResponse);
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		load_player_info_data(roomResponse);
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

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			exe_hun(GRR._banker_player);
			return true;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_henan_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i]);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

		return true;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
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
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L); // 结束时间

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			game_end.addPao(_player_result.pao[i]);
		}

		setGameEndBasicPrama(game_end);

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

			if (reason != GameConstants.Game_End_DRAW || (has_rule(GameConstants.GAME_RULE_HENAN_HZBHG))) {
				for (int i = 0; i < getTablePlayerNumber(); i++) {

					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];
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
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);

				if (GRR._chi_hu_rights[i].bao_ting_card != 0) {
					game_end.addBaoTingCards(GRR._chi_hu_rights[i].bao_ting_card + GameConstants.CARD_ESPECIAL_TYPE_BAO_TING);// 报听的牌
				} else {
					game_end.addBaoTingCards(0);
				}
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					if (_logic.is_magic_card(GRR._cards_data[i][j])) {
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
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
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));

				long durration = timer.get();
				long minuts = TimeUnit.MILLISECONDS.toMinutes(durration);

				if (minuts == 0)
					minuts = 1;

				// 所有小局经历的时间差，单位：分钟
				game_end.setTunShu((int) minuts);
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

			long durration = timer.get();
			long minuts = TimeUnit.MILLISECONDS.toMinutes(durration);

			if (minuts == 0)
				minuts = 1;

			// 所有小局经历的时间差，单位：分钟
			game_end.setTunShu((int) minuts);
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

		if (end) {
			PlayerServiceImpl.getInstance().delRoomId(getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		return true;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weave_items, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if ((!has_rule(GameConstants.GAME_RULE_HENAN_HENAN_PAO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)))
			return GameConstants.WIK_NULL;

		if (cur_card == 0)
			return GameConstants.WIK_NULL;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			tmp_cards_index[i] = cards_index[i];
		}

		if (cur_card != 0)
			tmp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		int chiHuKind = GameConstants.WIK_NULL;

		int magic_count = _logic.magic_count(tmp_cards_index);
		magic_count %= 2;

		if (magic_count == 0) {
			int qi_xiao_dui = _logic.is_qi_xiao_dui_henan(tmp_cards_index, weave_items, weave_count);
			if (qi_xiao_dui != GameConstants.WIK_NULL) {
				chiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
				}
				chiHuRight.opr_or(qi_xiao_dui);
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_QUE_MEN)) {
			if (!_logic.is_que_yi_se(tmp_cards_index, weave_items, weave_count)) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			if ((cards_index[_logic.get_magic_card_index(0)] == 4)
					|| ((cards_index[_logic.get_magic_card_index(0)] == 3) && (_logic.is_magic_card(cur_card))
							&& (card_type == GameConstants.HU_CARD_TYPE_ZIMO || card_type == GameConstants.HU_CARD_TYPE_GANG_KAI))) {
				chiHuKind = GameConstants.WIK_CHI_HU;
				if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
					chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				} else {
					chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
					chiHuRight.opr_or(GameConstants.CHR_HENAN_QISHOU_HU);
				}
			}
		}

		if (chiHuRight.is_empty() == false) {
			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
				chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
			} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
				chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
			} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
			} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
				chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
			}
			return chiHuKind;
		}

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];
		int magic_card_count = _logic.get_magic_card_count();

		if (magic_card_count > 2) { // 一般只有两种癞子牌存在
			magic_card_count = 2;
		}

		for (int i = 0; i < magic_card_count; i++) {
			magic_cards_index[i] = _logic.get_magic_card_index(i);
		}

		boolean can_win = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
				magic_card_count);

		if (!can_win) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		boolean lai_zi_cheng_ju = true;
		if (_logic.is_magic_card(cur_card) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU)) {
			lai_zi_cheng_ju = AnalyseCardUtil.analyse_lai_zi_cheng_ju_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
					magic_cards_index, magic_card_count);
		}

		if (lai_zi_cheng_ju == false) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		if (has_rule(GameConstants.GAME_RULE_HENAN_258) && !has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			boolean has_258 = AnalyseCardUtil.analyse_258_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), magic_cards_index,
					magic_card_count);
			if (!has_258) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		chiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		} else if (card_type == GameConstants.HU_CARD_TYPE_QIANGGANG) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_QIANG_GANG_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_TIAN_HU) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_TIAN_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			chiHuRight.opr_or(GameConstants.CHR_HENAN_GANG_KAI);
		}

		return chiHuKind;
	}

	@Override
	protected void set_result_describe() {
		int lGangScore[] = new int[GameConstants.GAME_PLAYER];
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				for (int k = 0; k < GameConstants.GAME_PLAYER; k++) {
					lGangScore[k] += GRR._gang_score[i].scores[j][k];
				}
			}
		}

		int l;
		long type = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			String des = "";

			if (has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI) && i == GRR._banker_player) {
				des += " 庄家加底";
			}

			l = GRR._chi_hu_rights[i].type_count;
			for (int j = 0; j < l; j++) {
				type = GRR._chi_hu_rights[i].type_list[j];
				if (GRR._chi_hu_rights[i].is_valid()) {
					if (type == GameConstants.CHR_ZI_MO) {
						if (has_rule_ex(GameConstants.GAME_RULE_ZI_MO_FAN_BEI)) {
							des += " 自摸翻倍";
						} else {
							des += " 自摸";
						}
					}

					if (type == GameConstants.CHR_HENAN_QISHOU_HU) {
						if (is_mj_type(GameConstants.GAME_TYPE_HENAN_NY) || is_mj_type(GameConstants.GAME_TYPE_NEW_NAN_YANG)) {
							des += " 4金加倍";
						} else {
							des += " 4混加倍";
						}

					}

					if (type == GameConstants.CHR_SHU_FAN) {
						des += " 接炮";
					}
					if (type == GameConstants.CHR_HENAN_QIANG_GANG_HU) {
						des += " 抢杠胡";
					}
					if (type == GameConstants.CHR_HENAN_TIAN_HU) {
						des += " 天胡";
					}
					if (type == GameConstants.CHR_HENAN_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
							des += " 七小对加倍";
						} else {
							des += " 七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_HEI_ZI) {
						if (is_mj_type(GameConstants.GAME_TYPE_HENAN_PDS) || is_mj_type(GameConstants.GAME_TYPE_NEW_PING_DING_SHAN)) {
							des += "黑子";
						}
					}
					if (type == GameConstants.CHR_HENAN_HH_QI_XIAO_DUI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
							des += " 豪七四倍";
						} else {
							des += " 豪华七小对";
						}
					}
					if (type == GameConstants.CHR_HENAN_GANG_KAI) {
						if (has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
							des += " 杠上花加倍";
						} else {
							des += " 杠上花";
						}

						if (has_rule_ex(GameConstants.GAME_RULE_ZI_MO_FAN_BEI)) {
							des += " 自摸翻倍";
						}

					}
				} else {
					if (type == GameConstants.CHR_FANG_PAO) {
						des += " 放炮";
					}
				}
			}
			int jie_gang = 0, fang_gang = 0, ming_gang = 0, an_gang = 0;
			if (GRR != null) {
				for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
					for (int w = 0; w < GRR._weave_count[p]; w++) {
						if (GRR._weave_items[p][w].weave_kind != GameConstants.WIK_GANG) {
							continue;
						}
						if (p == i) {
							if (GRR._weave_items[p][w].provide_player != p) {
								jie_gang++;
							} else {
								if (GRR._weave_items[p][w].public_card == 1) {
									ming_gang++;
								} else {
									an_gang++;
								}
							}
						} else {
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
			if (lGangScore[i] != 0 && GRR._end_type == GameConstants.Game_End_NORMAL) {
				des += " 杠总分(" + lGangScore[i] + ")";
			}
			GRR._result_des[i] = des;
		}
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	public int get_henan_ting_card(int[] cards, int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		PerformanceTimer timer = new PerformanceTimer();

		int count = 0;

		int[] tmp_cards_index = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++)
			tmp_cards_index[i] = cards_index[i];

		int card_type_count = GameConstants.MAX_ZI;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAIFENG))
			card_type_count = GameConstants.MAX_ZI_FENG;

		ChiHuRight chiHuRight = new ChiHuRight();
		int tmp_card = 0;
		for (int i = 0; i < card_type_count; i++) {
			tmp_card = _logic.switch_to_card_data(i);
			chiHuRight.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(tmp_cards_index, weave_items, weave_count, tmp_card, chiHuRight,
					GameConstants.HU_CARD_TYPE_ZIMO, i)) {
				cards[count] = tmp_card;

				if (_logic.is_magic_index(i)) {
					if (chiHuRight.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI).is_empty()
							|| chiHuRight.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI).is_empty()) {
						cards[count] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}

				count++;
			}
		}

		if (count >= card_type_count) {
			count = 1;
			cards[0] = -1;
		}

		if (timer.get() > 50) {
			logger.warn("cost time too long " + Arrays.toString(cards_index) + ", cost time = " + timer.duration());
		}

		return count;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
	}

	public void process_chi_hu_player_score_henan(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = _logic.get_chi_hu_action_rank_henan(chr);

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

		float lChiHuScore = wFanShu;

		boolean zhuang_hu = (GRR._banker_player == seat_index ? true : false);
		boolean zhuang_fang_hu = (GRR._banker_player == provide_index ? true : false);
		boolean jia_di = has_rule(GameConstants.GAME_RULE_HENAN_JIA_DI);

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (jia_di) {
					if (zhuang_hu) {
						s += 1;
					} else if (GRR._banker_player == i) {
						s += 1;
					}
				}

				s += (_player_result.pao[i] + _player_result.pao[seat_index]);

				if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty() && has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
					if (gang_provider == -1 || gang_provider == i) {
						s *= 2;
					}
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty() && has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}
				if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
					s *= 2;
				}

				if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
					if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
						s *= 4;
					} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
						s *= 2;
					}

				}

				if (is_mj_type(GameConstants.GAME_TYPE_JIAO_ZUO_HUAN_HUAN) && has_rule_ex(GameConstants.GAME_RULE_ZI_MO_FAN_BEI)) {
					s *= 2;
				}

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

			}
		} else {
			float s = lChiHuScore;

			if (jia_di) {
				if (zhuang_hu) {
					s += 1;
				} else if (zhuang_fang_hu) {
					s += 1;
				}
			}

			s += (_player_result.pao[provide_index] + _player_result.pao[seat_index]);

			if (!(chr.opr_and(GameConstants.CHR_HENAN_GANG_KAI)).is_empty() && has_rule(GameConstants.GAME_RULE_HENAN_GANGHUA_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QI_XIAO_DUI)).is_empty() && has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
				s *= 2;
			}
			if (!(chr.opr_and(GameConstants.CHR_HENAN_QISHOU_HU)).is_empty()) {
				s *= 2;
			}

			if (!(chr.opr_and(GameConstants.CHR_HENAN_HH_QI_XIAO_DUI)).is_empty()) {
				if (has_rule(GameConstants.GAME_RULE_HENAN_HAOQI)) {
					s *= 4;
				} else if (has_rule(GameConstants.GAME_RULE_HENAN_QIDUI_DOUBLE)) {
					s *= 2;
				}

			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);
		}

		GRR._provider[seat_index] = provide_index;

		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	public boolean estimate_gang_respond_henan(int seat_index, int card) {
		boolean bAroseAction = false;

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];
			if (playerStatus.is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_QIANGGANG,
						i);

				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);
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

	public boolean estimate_player_out_card_respond_henan(int seat_index, int card) {
		boolean bAroseAction = false;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			playerStatus = _playerStatus[i];

			action = _logic.check_peng(GRR._cards_index[i], card);
			if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {
				action = 0;
			}
			if (action != 0) {
				playerStatus.add_action(action);
				playerStatus.add_peng(card, seat_index);
				bAroseAction = true;
			}

			if (GRR._left_card_count > llcard) {
				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN) && _logic.is_magic_card(card)) {// 鬼牌不能杠
					action = 0;
				}
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);// 加上杠
					bAroseAction = true;
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, GameConstants.HU_CARD_TYPE_PAOHU, i);
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

	public boolean exe_hun(int seat_index) {
		set_handler(_handler_hun);
		_handler_hun.reset_status(seat_index);
		_handler_hun.exe(this);

		return true;
	}

	public boolean exe_qishou_hun(int seat_index) {
		set_handler(_handler_qishou_hun);
		_handler_qishou_hun.reset_status(seat_index);
		_handler_qishou_hun.exe(this);
		return true;
	}

	@Override
	public boolean operate_player_cards_with_ting(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		load_common_status(roomResponse);

		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		send_response_to_other(seat_index, roomResponse);

		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			roomResponse.addOutCardTing(_playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int tmp_card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {
					if (_logic.is_magic_card(tmp_card)) {
						tmp_card += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}
				int_array.addItem(tmp_card);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

		return true;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_pao_henna.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}
}
