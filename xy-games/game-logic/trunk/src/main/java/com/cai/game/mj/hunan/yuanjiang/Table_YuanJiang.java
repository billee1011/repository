package com.cai.game.mj.hunan.yuanjiang;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_YuanJiang;
import com.cai.common.define.ELogType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.ThreadUtil;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.MJType;
import com.cai.game.mj.NewAbstractMjTable;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.game.util.GameUtilConstants;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Basic.MJ_Game_End_Basic;
import protobuf.clazz.mj.Basic.PaiXing;
import protobuf.clazz.mj.Basic.PaiXingList;

public class Table_YuanJiang extends NewAbstractMjTable {
	private static final long serialVersionUID = -2456323602522819218L;

	public HandlerHaiDi_YuanJiang _handler_hai_di;
	public HandlerOutCardBaoTing_YuanJiang _handler_out_card_bao_ting;
	public HandlerBaoTing_YuanJiang _handler_bao_ting;
	public boolean is_hai_di_state = false;
	public boolean[] has_dispatch_hai_di = new boolean[getTablePlayerNumber()];
	public int[] hai_di_card = new int[getTablePlayerNumber()];
	public int[] happy_win_score = new int[getTablePlayerNumber()];
	public boolean[] has_caculated_xi_fen = new boolean[getTablePlayerNumber()];
	public int[] zhua_pai_count = new int[getTablePlayerNumber()];
	public boolean[] is_bao_ting = new boolean[getTablePlayerNumber()];
	public int[] ming_gang_score = new int[getTablePlayerNumber()];
	public int[] gong_gang_score = new int[getTablePlayerNumber()];
	public int[] an_gang_score = new int[getTablePlayerNumber()];
	public int[] pai_xing_fen = new int[getTablePlayerNumber()];
	public int win_type = 1;
	public boolean[] has_hai_di_gang = new boolean[getTablePlayerNumber()];
	public boolean[] has_hai_di_zi_mo = new boolean[getTablePlayerNumber()];

	public Map<Integer, Map<Integer, Integer>> paiXingMap = new HashMap<>();

	public int[] totalGangXiScore = new int[getTablePlayerNumber()];

	public Table_YuanJiang(MJType type) {
		super(type);
	}

	public boolean operate_player_info() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	public boolean operate_player_tmp_score() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SCORE_RECORD);
		roomResponse.setGameStatus(_game_status);

		load_player_info_data(roomResponse);

		if (GRR != null)
			GRR.add_room_response(roomResponse);

		send_response_to_room(roomResponse);

		return true;
	}

	@Override
	public boolean handler_release_room(Player player, int opr_code) {
		int pCount = getTablePlayerNumber();

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
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				return false;
			}

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

			for (int i = 0; i < pCount; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			_gameRoomRecord.request_player_seat = seat_index;
			_gameRoomRecord.release_players[seat_index] = 1;

			int count = 0;
			for (int i = 0; i < pCount; i++) {
				if (_gameRoomRecord.release_players[i] == 1) {
					count++;
				}
			}

			if ((pCount == 2 && count == pCount) || (pCount != 2 && (count == pCount || count == pCount - 1))) {
				if (GRR == null) {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_RESULT);
				} else {
					handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);
				}

				for (int j = 0; j < pCount; j++) {
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
			for (int i = 0; i < pCount; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}
			send_response_to_room(roomResponse);

			return false;
		}

		case GameConstants.Release_Room_Type_AGREE: {
			if (GameConstants.GS_MJ_FREE == _game_status) {
				return false;
			}

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
			int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
			if (l <= 0) {
				l = 1;
			}
			roomResponse.setLeftTime(l);
			for (int i = 0; i < pCount; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			send_response_to_room(roomResponse);

			int falseCount = 0;

			for (int i = 0; i < pCount; i++) {
				if (get_players()[i] == null)
					continue;

				if (_gameRoomRecord.release_players[i] == 2 || _gameRoomRecord.release_players[i] == 0) {
					falseCount++;
				}
			}

			if ((pCount == 2 && falseCount == 1) || (pCount != 2 && falseCount >= 2))
				return false;

			for (int j = 0; j < pCount; j++) {
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
				return false;
			}

			if (_gameRoomRecord.request_player_seat == GameConstants.INVALID_SEAT) {
				return false;
			}

			_gameRoomRecord.release_players[seat_index] = 2;

			int falseCount = 0;

			for (int i = 0; i < pCount; i++) {
				if (get_players()[i] == null)
					continue;

				if (_gameRoomRecord.release_players[i] == 2) {
					falseCount++;
				}
			}

			if ((pCount == 2 && falseCount == 0) || (pCount != 2 && falseCount <= 1)) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
				roomResponse.setReleaseTime(delay);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setOperateCode(1);
				int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
				if (l <= 0) {
					l = 1;
				}
				roomResponse.setLeftTime(l);
				for (int i = 0; i < pCount; i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}

				send_response_to_room(roomResponse);

				return false;
			}

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
			for (int i = 0; i < pCount; i++) {
				roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
			}

			send_response_to_room(roomResponse);

			_request_release_time = 0;
			_gameRoomRecord.request_player_seat = GameConstants.INVALID_SEAT;

			if (_release_scheduled != null)
				_release_scheduled.cancel(false);

			_release_scheduled = null;

			for (int i = 0; i < pCount; i++) {
				_gameRoomRecord.release_players[i] = 0;
			}

			for (int j = 0; j < pCount; j++) {
				player = get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散失败!玩家[" + get_players()[seat_index].getNick_name() + "]不同意解散");
			}

			return false;
		}

		case GameConstants.Release_Room_Type_CANCEL: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			if (_cur_round == 0) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT); // 直接拉出游戏
				send_response_to_room(roomResponse);

				for (int i = 0; i < pCount; i++) {
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
		case GameConstants.Release_Room_Type_QUIT: {
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT); // 直接拉出游戏
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
			if (GameConstants.GS_MJ_FREE != _game_status) {
				return false;
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT); // 直接拉出游戏
			send_response_to_room(roomResponse);

			for (int i = 0; i < pCount; i++) {
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
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = get_players()[i];

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
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			if (is_bao_ting != null) {
				room_player.setIsTrustee(is_bao_ting[i]);
			} else {
				room_player.setIsTrustee(false);
			}

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			// 玩家的杠分和喜分
			room_player.setJiaoDiZhu(totalGangXiScore[i]);

			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		set_handler(_handler_out_card_bao_ting);
		_handler_out_card_bao_ting.reset_status(seat_index, card, type);
		_handler.exe(this);

		return true;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_YuanJiang();
		_handler_dispath_card = new HandlerDispatchCard_YuanJiang();
		_handler_gang = new HandlerGang_YuanJiang();
		_handler_out_card_operate = new HandlerOutCardOperate_YuanJiang();
		_handler_hai_di = new HandlerHaiDi_YuanJiang();
		_handler_out_card_bao_ting = new HandlerOutCardBaoTing_YuanJiang();
		_handler_qi_shou = new HandlerQiShou_YuanJiang();
		_handler_bao_ting = new HandlerBaoTing_YuanJiang();

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Map<Integer, Integer> paiXingSubMap = new HashMap<>();

			for (PaiXingEnum type : PaiXingEnum.values()) {
				paiXingSubMap.put(type.getChr(), 0);
			}

			paiXingMap.put(i, paiXingSubMap);
		}
	}

	protected void exe_bao_ting(int seat_index) {
		set_handler(_handler_bao_ting);
		_handler_bao_ting.reset_status(seat_index);
		_handler_bao_ting.exe(this);
	}

	public boolean exe_hai_di(int seat_index, int type, int delay) {
		if (delay > 0) {
			GameSchedule.put(new DispatchCardRunnable(getRoom_id(), seat_index, type, false), delay, TimeUnit.MILLISECONDS);
		} else {
			set_handler(_handler_hai_di);
			_handler_hai_di.reset_status(seat_index, type);
			_handler.exe(this);
		}

		return true;
	}

	@Override
	protected boolean on_game_start() {
		return false;
	}

	@Override
	public void setGameEndBasicPrama(GameEndResponse.Builder game_end) {
		// 回放时、查牌时，头像旁边的跑分显示
		MJ_Game_End_Basic.Builder basic_game_end = MJ_Game_End_Basic.newBuilder();
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			basic_game_end.addPao(_player_result.pao[i]);
			basic_game_end.addNao(_player_result.nao[i]);
			basic_game_end.addQiang(_player_result.qiang[i]);
			basic_game_end.addBianYan(_player_result.biaoyan[i]);

			Map<Integer, Integer> subMap = paiXingMap.get(i);

			PaiXingList.Builder pxl = PaiXingList.newBuilder();

			for (Map.Entry<Integer, Integer> entry : subMap.entrySet()) {
				PaiXing.Builder px = PaiXing.newBuilder();
				px.setType(entry.getKey());
				px.setCount(entry.getValue());

				pxl.addPaiXingList(px);
			}

			basic_game_end.addAllPaiXingList(pxl);
		}
		game_end.setCommResponse(PBUtil.toByteString(basic_game_end));
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += GRR._start_hu_score[i];
				_player_result.game_score[i] += GRR._game_score[i];
			}

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);

			int cards[] = new int[GRR._left_card_count];// 显示剩余的牌数据
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}

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

			// 胡牌时是否有大胡
			game_end.setHuXi(win_type); // 1：小胡，2：大胡

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (is_hai_di_state && hai_di_card[i] != 0 && !has_hai_di_gang[i] && !has_hai_di_zi_mo[i]) {
					int tmpIndex = _logic.switch_to_card_index(hai_di_card[i]);
					GRR._cards_index[i][tmpIndex]++;
				}

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
					int kind = GRR._weave_items[i][j].weave_kind;
					int display = GRR._weave_items[i][j].public_card;
					int provider = GRR._weave_items[i][j].provide_player;
					if (kind == GameConstants.WIK_GANG) {
						if (display == 0) {
							weaveItem_item.setWeaveKind(GameConstants.WIK_AN_GANG_HB);
						} else {
							if (provider == i) {
								weaveItem_item.setWeaveKind(GameConstants.WIK_ADD_GANG_HB);
							} else {
								weaveItem_item.setWeaveKind(GameConstants.WIK_JIE_GANG_HB);
							}
						}
					} else {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					}
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);

				// 总结算的分
				game_end.addGameScore(GRR._game_score[i]);
				// 明杠
				game_end.addJettonScore(ming_gang_score[i]);
				// 公杠
				game_end.addGangScore(gong_gang_score[i]);
				// 暗杠
				game_end.addStartHuScore(an_gang_score[i]);
				// 牌型分
				game_end.addCardType(pai_xing_fen[i]);

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

		if (is_bao_ting != null)
			Arrays.fill(is_bao_ting, false);

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		if (_logic.is_valid_card(cur_card)) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		boolean can_win = false;

		int[] magic_cards_index = new int[GameUtilConstants.MAX_MAGIC_INDEX_COUNT];

		// 平胡
		boolean can_ping_hu = AnalyseCardUtil.analyse_win_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);
		// 将将胡
		boolean can_jiang_jiang_hu = _logic.is_jiangjiang_hu(cards_index, weaveItems, weave_count, cur_card);
		// 七小对
		int qi_xiao_dui_result = check_qi_xiao_dui(cbCardIndexTemp, weave_count);
		boolean can_qi_xiao_dui = (qi_xiao_dui_result != 0);
		// 碰碰胡
		boolean can_peng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cbCardIndexTemp, -1, magic_cards_index, 0);

		can_win = can_ping_hu || can_jiang_jiang_hu || can_qi_xiao_dui;

		if (!can_win) {
			chiHuRight.set_empty();
			return 0;
		}

		// 一条龙
		boolean can_yi_tiao_long = _logic.is_yi_tiao_long(cbCardIndexTemp, weave_count);
		// 清一色
		boolean can_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);

		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cbCardIndexTemp, hand_cards);
		// 一字翘
		boolean can_yi_zi_qiao = (hand_card_count == 2) && (hand_cards[0] == hand_cards[1]);

		// 门清
		boolean has_rule_men_qing = has_rule(Constants_YuanJiang.GAME_RULE_YOU_MEN_QING);
		boolean can_men_qing = has_rule_men_qing && (_logic.is_men_qing(weaveItems, weave_count) != 0)
				&& (can_jiang_jiang_hu || can_peng_hu || can_yi_tiao_long || can_qing_yi_se);

		if (can_jiang_jiang_hu)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_JIANG_JIANG_HU);
		if (can_qi_xiao_dui)
			chiHuRight.opr_or(qi_xiao_dui_result);
		if (can_peng_hu)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_PENG_PENG_HU);
		if (can_yi_tiao_long)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_YI_TIAO_LONG);
		if (can_qing_yi_se)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_QING_YI_SE);
		if (can_yi_zi_qiao)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_YI_ZI_QIAO);
		if (can_men_qing && !can_qi_xiao_dui)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_MEN_QING);

		// 报听
		boolean can_bao_ting = is_bao_ting[_seat_index];
		if (can_bao_ting)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_BAO_TING);

		if (card_type == Constants_YuanJiang.HU_CARD_TYPE_JIE_PAO) {
			int da_hu_count = get_da_hu_count(chiHuRight);

			if (da_hu_count == 1 && can_jiang_jiang_hu) {
				// 只有将将胡，不可接炮
				chiHuRight.set_empty();
				return 0;
			}

			if (da_hu_count == 2 && can_jiang_jiang_hu && can_yi_zi_qiao) {
				// 只有将将胡+一字撬，不可接炮
				chiHuRight.set_empty();
				return 0;
			}

			if (has_rule(Constants_YuanJiang.GAME_RULE_KA_QIAO)) {
				if (da_hu_count == 1 && can_peng_hu && is_liang_dui(cards_index)) {
					// 只有碰碰胡，并且是最后的两对
					chiHuRight.set_empty();
					return 0;
				}
			}

			if (has_rule(Constants_YuanJiang.GAME_RULE_MQJJH_BU_KE_JIE_PAO)) {
				if (da_hu_count == 2 && can_men_qing && can_jiang_jiang_hu) {
					// 只有门清和将将胡
					chiHuRight.set_empty();
					return 0;
				}
			}

			if (da_hu_count == 0) {
				// 只有平胡
				chiHuRight.set_empty();
				return 0;
			}
		}

		if (card_type == Constants_YuanJiang.HU_CARD_TYPE_GANG_SHANG_HUA) {
			chiHuRight.opr_or(Constants_YuanJiang.CHR_GANG_SHANG_HUA);
		} else if (card_type == Constants_YuanJiang.HU_CARD_TYPE_QIANG_GANG_HU) {
			chiHuRight.opr_or(Constants_YuanJiang.CHR_QIANG_GANG_HU);
		} else if (card_type == Constants_YuanJiang.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(Constants_YuanJiang.CHR_ZI_MO);
		} else if (card_type == Constants_YuanJiang.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(Constants_YuanJiang.CHR_JIE_PAO);
		}

		// 天胡
		boolean can_tian_hu = (weave_count == 0) && (_seat_index == GRR._banker_player) && (_out_card_count == 0);
		if (can_tian_hu)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_TIAN_HU);

		// 海底
		boolean can_hai_di = is_hai_di_state && card_type == Constants_YuanJiang.HU_CARD_TYPE_ZI_MO;
		if (can_hai_di)
			chiHuRight.opr_or(Constants_YuanJiang.CHR_HAI_DI);

		return GameConstants.WIK_CHI_HU;
	}

	public boolean is_liang_dui(int[] cards_index) {
		if (_logic.get_card_count_by_index(cards_index) > 4)
			return false;
		for (int i = 0; i < cards_index.length; i++) {
			if (cards_index[i] > 0 && cards_index[i] != 2)
				return false;
		}
		return true;
	}

	public int get_da_hu_count(ChiHuRight chr) {
		int da_hu_count = 0;

		if (!chr.opr_and(Constants_YuanJiang.CHR_JIANG_JIANG_HU).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_QI_XIAO_DUI).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_PENG_PENG_HU).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_YI_TIAO_LONG).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_QING_YI_SE).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_YI_ZI_QIAO).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_ONE_HH_QI_XIAO_DUI).is_empty())
			da_hu_count += 2;
		if (!chr.opr_and(Constants_YuanJiang.CHR_TWO_HH_QI_XIAO_DUI).is_empty())
			da_hu_count += 3;
		if (!chr.opr_and(Constants_YuanJiang.CHR_THREE_HH_QI_XIAO_DUI).is_empty())
			da_hu_count += 4;
		if (!chr.opr_and(Constants_YuanJiang.CHR_MEN_QING).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_HAI_DI).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_BAO_TING).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_TIAN_HU).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_GANG_SHANG_HUA).is_empty())
			da_hu_count++;
		if (!chr.opr_and(Constants_YuanJiang.CHR_QIANG_GANG_HU).is_empty())
			da_hu_count++;

		return da_hu_count;
	}

	public boolean is_qing_yi_se(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i))
				continue;

			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weave_items[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int cbCenterCard = weave_items[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	public boolean is_qing_yi_se_ying(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int cbCardColor = 0xFF;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] != 0) {
				if (cbCardColor != 0xFF)
					return false;

				cbCardColor = (_logic.switch_to_card_data(i) & GameConstants.LOGIC_MASK_COLOR);

				i = (i / 9 + 1) * 9 - 1;
			}
		}

		if (cbCardColor == 0xFF) {
			cbCardColor = weave_items[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		}

		for (int i = 0; i < weave_count; i++) {
			if (weave_items[i].weave_kind == GameConstants.WIK_SHOW_CARD)
				continue;

			int cbCenterCard = weave_items[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return false;
		}

		return true;
	}

	public int check_qi_xiao_dui(int cards_index[], int cbWeaveCount) {
		if (cbWeaveCount != 0)
			return 0;

		int cbReplaceCount = 0;

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int gengCount = 0;

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 4)
				gengCount++;
		}

		if (cbReplaceCount > 0)
			return 0;

		if (gengCount >= 3)
			return Constants_YuanJiang.CHR_THREE_HH_QI_XIAO_DUI;
		if (gengCount >= 2)
			return Constants_YuanJiang.CHR_TWO_HH_QI_XIAO_DUI;
		if (gengCount >= 1)
			return Constants_YuanJiang.CHR_ONE_HH_QI_XIAO_DUI;

		return Constants_YuanJiang.CHR_QI_XIAO_DUI;
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int effect_count = chr.type_count;
		long effect_indexs[] = new long[effect_count];
		for (int i = 0; i < effect_count; i++) {
			effect_indexs[i] = chr.type_list[i];
		}

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);

		if (rm) {
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		return;
	}

	public void pai_xing_tong_ji(int seat_index) {
		if (GRR != null && GRR._win_order[seat_index] != 0) {
			ChiHuRight chr = GRR._chi_hu_rights[seat_index];

			Map<Integer, Integer> subMap = paiXingMap.get(seat_index);

			int chrTypes = chr.type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				int type = (int) chr.type_list[typeIndex];

				if (subMap.containsKey(type)) {
					int count = subMap.get(type);
					count++;

					subMap.replace(type, count);
				}
			}
		}
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		pai_xing_tong_ji(seat_index);

		int lChiHuScore = 0;
		int da_hu_count = get_da_hu_count(chr);
		if (da_hu_count == 0) {
			lChiHuScore = 1;
		} else {
			win_type = 2;
			lChiHuScore = 3 * (1 << (da_hu_count - 1));
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore;
		}

		if (zimo) {
			int s = lChiHuScore;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int tmp_s = s * (1 << (GRR._player_niao_count[i] + GRR._player_niao_count[seat_index]));

				if (tmp_s > 24 && has_rule(Constants_YuanJiang.GAME_RULE_24_BEI_FENG_DING))
					tmp_s = 24;

				pai_xing_fen[i] -= tmp_s;
				pai_xing_fen[seat_index] += tmp_s;

				GRR._game_score[i] -= tmp_s;
				GRR._game_score[seat_index] += tmp_s;
			}
		} else {
			int s = lChiHuScore * 2;

			s *= 1 << (GRR._player_niao_count[provide_index] + GRR._player_niao_count[seat_index]);

			if (s > 24 && has_rule(Constants_YuanJiang.GAME_RULE_24_BEI_FENG_DING))
				s = 24;

			pai_xing_fen[provide_index] -= s;
			pai_xing_fen[seat_index] += s;

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			GRR._chi_hu_rights[provide_index].opr_or(Constants_YuanJiang.CHR_FANG_PAO);
		}
	}

	@SuppressWarnings("unused")
	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;

		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == Constants_YuanJiang.CHR_ZI_MO)
						result.append(" 自摸");
					if (type == Constants_YuanJiang.CHR_JIE_PAO)
						result.append(" 接炮");
					if (type == Constants_YuanJiang.CHR_GANG_SHANG_HUA)
						result.append(" 杠爆");
					if (type == Constants_YuanJiang.CHR_QIANG_GANG_HU)
						result.append(" 抢杠胡");
					if (type == Constants_YuanJiang.CHR_JIANG_JIANG_HU)
						result.append(" 将将胡");
					if (type == Constants_YuanJiang.CHR_QI_XIAO_DUI)
						result.append(" 七小对");
					if (type == Constants_YuanJiang.CHR_PENG_PENG_HU)
						result.append(" 碰碰胡");
					if (type == Constants_YuanJiang.CHR_YI_TIAO_LONG)
						result.append(" 一条龙");
					if (type == Constants_YuanJiang.CHR_QING_YI_SE)
						result.append(" 清一色");
					if (type == Constants_YuanJiang.CHR_YI_ZI_QIAO)
						result.append(" 一字撬");
					if (type == Constants_YuanJiang.CHR_ONE_HH_QI_XIAO_DUI)
						result.append(" 豪华七小对");
					if (type == Constants_YuanJiang.CHR_TWO_HH_QI_XIAO_DUI)
						result.append(" 双豪华七小对");
					if (type == Constants_YuanJiang.CHR_THREE_HH_QI_XIAO_DUI)
						result.append(" 三豪华七小对");
					if (type == Constants_YuanJiang.CHR_MEN_QING)
						result.append(" 门清");
					if (type == Constants_YuanJiang.CHR_HAI_DI)
						result.append(" 海底");
					if (type == Constants_YuanJiang.CHR_BAO_TING)
						result.append(" 报听");
					if (type == Constants_YuanJiang.CHR_TIAN_HU)
						result.append(" 天胡");
				} else if (type == Constants_YuanJiang.CHR_FANG_PAO) {
					result.append(" 放炮");
				}
			}

			if (happy_win_score[player] > 0) {
				result.append(" 喜钱x" + happy_win_score[player]);
			}

			if (GRR._player_niao_count[player] > 0 && GRR._win_order[player] != 0) {
				result.append(" 全中");
			} else if (GRR._player_niao_count[player] > 0) {
				result.append(" 中鸟x" + GRR._player_niao_count[player]);
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

			/**
			 * if (an_gang > 0) { result.append(" 暗杠x" + an_gang); } if
			 * (ming_gang > 0) { result.append(" 明杠x" + ming_gang); } if
			 * (fang_gang > 0) { result.append(" 放杠x" + fang_gang); } if
			 * (jie_gang > 0) { result.append(" 接杠x" + jie_gang); }
			 **/

			if (fang_gang > 0) {
				result.append(" 放杠");
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_TING && card < GameConstants.CARD_ESPECIAL_TYPE_BAO_TING) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		return card;
	}

	public boolean is_ting_card(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		for (int i = 0; i < GameConstants.MAX_ZI; i++) {
			chr.set_empty();
			int cbCurrentCard = _logic.switch_to_card_data(i);

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_YuanJiang.HU_CARD_TYPE_ZI_MO, seat_index)) {
				return true;
			}
		}

		return false;
	}

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

			if (GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0 && !is_bao_ting[i]) {
					boolean can_peng_this_card = true;
					int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
					for (int x = 0; x < GameConstants.MAX_ZI_FENG; x++) {
						if (tmp_cards_data[x] == card) {
							can_peng_this_card = false;
							break;
						}
					}
					if (can_peng_this_card) {
						playerStatus.add_action(action);
						playerStatus.add_peng(card, seat_index);
						bAroseAction = true;
					}
				}

				action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
				if (action != 0 && !is_bao_ting[i]) {
					playerStatus.add_action(GameConstants.WIK_GANG);
					playerStatus.add_gang(card, seat_index, 1);
					bAroseAction = true;
				}
			}

			if (is_bao_ting[i]) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}

				if (_playerStatus[i].is_chi_hu_round() && can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();

					int hu_card_type = Constants_YuanJiang.HU_CARD_TYPE_JIE_PAO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
					if (action != 0) {
						playerStatus.add_action(GameConstants.WIK_CHI_HU);
						playerStatus.add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			} else if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				int hu_card_type = Constants_YuanJiang.HU_CARD_TYPE_JIE_PAO;

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr, hu_card_type, i);
				if (action != 0) {
					playerStatus.add_action(GameConstants.WIK_CHI_HU);
					playerStatus.add_chi_hu(card, seat_index);
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		}

		return bAroseAction;
	}

	public int get_next_seat(int seat_index) {
		int count = 0;
		int seat = seat_index;

		do {
			count++;
			seat = (seat + 1) % getTablePlayerNumber();
		} while (get_players()[seat] == null && count <= 5);
		return seat;
	}

	@Override
	public int get_ting_card(int[] cards, int[] cards_index, WeaveItem[] weaveItem, int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					Constants_YuanJiang.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count == 0) {
		} else if (count == max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {
		boolean bAroseAction = false;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (seat_index == i)
				continue;

			if (has_dispatch_hai_di[i])
				continue;

			if (is_bao_ting[i]) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}

				if (_playerStatus[i].is_chi_hu_round() && can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
							Constants_YuanJiang.HU_CARD_TYPE_QIANG_GANG_HU, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			} else if (_playerStatus[i].is_chi_hu_round()) {
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();

				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], card, chr,
						Constants_YuanJiang.HU_CARD_TYPE_QIANG_GANG_HU, i);

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

	public void set_niao_card(int seat_index) {
		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = true;

		GRR._count_niao = 1;

		if (GRR._count_niao > 0) {
			if (GRR._left_card_count == 0) {
				GRR._cards_data_niao[0] = GRR._chi_hu_card[seat_index][0];
			} else {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];

				// 从剩余牌堆里顺序取奖码数目的牌
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);

				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);

				if (DEBUG_CARDS_MODE) {
					GRR._cards_data_niao[0] = 0x04;
				}

				GRR._left_card_count -= GRR._count_niao;
			}
		}

		for (int i = 0; i < GRR._count_niao; i++) {
			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);

			int seat = get_seat_by_value(nValue, seat_index);

			GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] = GRR._cards_data_niao[i];

			GRR._player_niao_count[seat]++;

			// 用明杠表示中鸟次数
			_player_result.ming_gang_count[seat]++;
		}

		// 设置鸟牌显示
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GRR._player_niao_count[i]; j++) {
				GRR._player_niao_cards[i][j] = set_ding_niao_valid(GRR._player_niao_cards[i][j], true);
			}
		}
	}

	@Override
	public int get_seat_by_value(int nValue, int seat_index) {
		int seat = 0;
		if (getTablePlayerNumber() == 4) {
			seat = (seat_index + (nValue - 1) % getTablePlayerNumber()) % getTablePlayerNumber();
		} else if (getTablePlayerNumber() == 3) {
			switch (nValue) {
			// 147
			case 1:
			case 4:
			case 7:
				seat = seat_index;
				break;
			// 258
			case 2:
			case 5:
			case 8:
				seat = get_banker_next_seat(seat_index);
				break;
			// 369
			case 3:
			case 6:
			case 9:
				seat = get_banker_pre_seat(seat_index);
				break;
			default:
				break;
			}
		} else if (getTablePlayerNumber() == 2) {
			int v = (nValue - 1) % 4;
			switch (v) {
			case 0:
			case 2:
				seat = seat_index;
				break;
			case 1:
			case 3:
				seat = get_banker_next_seat(seat_index);
				break;
			default:
				break;
			}
		}
		return seat;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return false;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x07, 0x07, 0x07, 0x12, 0x12, 0x12, 0x16, 0x16, 0x16, 0x19, 0x19, 0x19, 0x26, 0x26 };
		int[] cards_of_player1 = new int[] { 0x07, 0x07, 0x07, 0x12, 0x12, 0x12, 0x16, 0x16, 0x16, 0x19, 0x19, 0x19, 0x26, 0x26 };
		int[] cards_of_player2 = new int[] { 0x07, 0x07, 0x07, 0x12, 0x12, 0x12, 0x16, 0x16, 0x16, 0x19, 0x19, 0x19, 0x26, 0x26 };
		int[] cards_of_player3 = new int[] { 0x07, 0x07, 0x07, 0x12, 0x12, 0x12, 0x16, 0x16, 0x16, 0x19, 0x19, 0x19, 0x26, 0x26 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
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

		if (cards_of_player0.length > 13) {
			// 庄家起手14张
			GRR._cards_index[GRR._banker_player][_logic.switch_to_card_index(cards_of_player0[13])]++;
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
	public boolean on_game_start_new() {
		is_hai_di_state = false;
		has_dispatch_hai_di = new boolean[getTablePlayerNumber()];
		hai_di_card = new int[getTablePlayerNumber()];
		happy_win_score = new int[getTablePlayerNumber()];
		has_caculated_xi_fen = new boolean[getTablePlayerNumber()];
		zhua_pai_count = new int[getTablePlayerNumber()];
		is_bao_ting = new boolean[getTablePlayerNumber()];
		ming_gang_score = new int[getTablePlayerNumber()];
		gong_gang_score = new int[getTablePlayerNumber()];
		an_gang_score = new int[getTablePlayerNumber()];
		pai_xing_fen = new int[getTablePlayerNumber()];
		win_type = 1;
		has_hai_di_gang = new boolean[getTablePlayerNumber()];
		has_hai_di_zi_mo = new boolean[getTablePlayerNumber()];

		_logic.clean_magic_cards();
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

			// 重新装载玩家信息
			load_player_info_data(roomResponse);

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

		exe_bao_ting(_cur_banker);

		return true;
	}

	@Override
	public int analyse_qi_shou_hu_pai(int[] cards_index, WeaveItem[] weaveItems, int weave_count, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		return 0;
	}
}
