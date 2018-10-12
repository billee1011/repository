/**
 * 
 */
package com.cai.game.shisanzhang.ssz_jd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EMoneyOperateType;
import com.cai.common.domain.AddMoneyResultModel;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.rmi.ICenterRMIServer;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.shisanzhang.SSZTable;
import com.cai.game.shisanzhang.SSZType;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.SysParamUtil;

import protobuf.clazz.Protocol;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.Response;
import protobuf.clazz.Protocol.Response.ResponseType;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ssz.SszRsp.CallBankerResult;
import protobuf.clazz.ssz.SszRsp.GameCallBankerSsz;
import protobuf.clazz.ssz.SszRsp.GameJettonSsz;
import protobuf.clazz.ssz.SszRsp.JettonResult;
import protobuf.clazz.ssz.SszRsp.PukeGameEndSsz;
import protobuf.clazz.ssz.SszRsp.RoomInfoSsz;
import protobuf.clazz.ssz.SszRsp.RoomPlayerResponseSsz;
import protobuf.clazz.ssz.SszRsp.SSZ_CallBankerRequest;
import protobuf.clazz.ssz.SszRsp.SSZ_JettonRequest;
import protobuf.clazz.ssz.SszRsp.SSZ_OpenCardRequest;
import protobuf.clazz.ssz.SszRsp.SSZ_OpenCardResult;
import protobuf.clazz.ssz.SszRsp.Send_Card_Data;
import protobuf.clazz.ssz.SszRsp.TableResponseSsz;

///////////////////////////////////////////////////////////////////////////////////////////////
public class SSZTable_JD extends SSZTable {
	/**
	 * 
	 */
	public boolean _is_jetton[];
	public boolean _is_call_banker[];
	public int _call_banker[];
	public int _jetton_score[];
	public boolean _is_open_card[];
	public int _win_num[];
	public int _lose_num[];
	public int _draw_num[];

	public SSZTable_JD() {
		super(SSZType.GAME_TYPE_SSZ_JD);

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		is_game_start = 0;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());

		_is_jetton = new boolean[getTablePlayerNumber()];
		_is_call_banker = new boolean[getTablePlayerNumber()];
		_call_banker = new int[getTablePlayerNumber()];
		_jetton_score = new int[getTablePlayerNumber()];
		_is_open_card = new boolean[getTablePlayerNumber()];
		_is_special = new boolean[getTablePlayerNumber()];
		_win_num = new int[getTablePlayerNumber()];
		_lose_num = new int[getTablePlayerNumber()];
		_draw_num = new int[getTablePlayerNumber()];
		this._logic._game_type_index = _game_type_index;
		this._logic._game_rule_index = _game_rule_index;
		Arrays.fill(_lose_num, 0);
		Arrays.fill(_win_num, 0);
		Arrays.fill(_draw_num, 0);

		if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_DEAL_CARD_60)) {
			_deal_card_time = 60;
		} else if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_DEAL_CARD_40)) {
			_deal_card_time = 40;
		} else if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_DEAL_CARD_25)) {
			_deal_card_time = 25;
		} else {
			_deal_card_time = 0;
		}

		this.setMinPlayerCount(2);
	}

	@Override
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {
		// TODO Auto-generated method stub

	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		this.log_error("gme_status:" + this._game_status);
		reset_init_data();
		//
		if (_repertory_card == null) {
			_repertory_card = new int[GameConstants.CARD_COUNT_SSZ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_SSZ);
		} else {
			shuffle(_repertory_card, GameConstants.CARD_DATA_SSZ);
		}

		Arrays.fill(_is_jetton, false);
		Arrays.fill(_is_call_banker, false);
		Arrays.fill(_call_banker, 0);
		Arrays.fill(_is_open_card, false);
		Arrays.fill(_is_special, false);
		Arrays.fill(_jetton_score, 0);

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		game_cell = 1;
		this.GRR._banker_player = GameConstants.INVALID_SEAT;
		_cur_banker = GameConstants.INVALID_SEAT;
		_pre_opreate_type = 0;
		Arrays.fill(_call_banker, 0);

		return game_start_sszjd();
	}

	private void progress_banker_select() {
		int targetindex = RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getPlayerCount();
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			if (index == targetindex) {
				_cur_banker = i;
				_current_player = i;
				return;
			}
			index++;
		}
	}

	@Override
	public boolean game_start_sszjd() {
		this.log_error("_repertory_card:" + Arrays.toString(_repertory_card));

		this._current_player = _cur_banker;
		_game_status = GameConstants.GS_MJ_PLAY;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_QIANG_ZHUANG)) {
			this.set_timer(ID_TIMER_START_TO_CALLBANKER, 2);
		} else {
			if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_ZHUANG_TONGBI)) {
				_cur_banker = GameConstants.INVALID_SEAT;
			} else {
				Player own_player = this.getCreate_player();
				if (own_player != null) {
					_cur_banker = own_player.get_seat_index();
					if (_cur_banker == GameConstants.INVALID_SEAT) {
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (this.get_players()[i] == null) {
								continue;
							}
							_cur_banker = i;
						}
					}
				} else {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this.get_players()[i] == null) {
							continue;
						}
						_cur_banker = i;
					}
				}

			}
			this.GRR._banker_player = _cur_banker;
			set_timer(ID_TIMER_START_TO_SEND_CARD, 2);
		}
		return true;
	}

	public void call_banker_start() {
		_game_status = GameConstants.GS_CALL_BANKER;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_CALL_BANKER);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		GameCallBankerSsz.Builder callbanker_ssz = GameCallBankerSsz.newBuilder();
		RoomInfoSsz.Builder room_info = getRoomInfoSsz();
		callbanker_ssz.setRoomInfo(room_info);
		callbanker_ssz.setCellScore((int) this.game_cell);
		callbanker_ssz.setDisplayTime(10);
		callbanker_ssz.setCurrentPlayer(GameConstants.INVALID_SEAT);
		this.load_player_info_data_call_banker_start(callbanker_ssz);
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_ssz));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}

	@Override
	public boolean jetton_start_sszjd() {
		_game_status = GameConstants.GS_MJ_PAO;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_JETTON_START);
		roomResponse.setGameStatus(this._game_status);

		// 发送数据
		GameJettonSsz.Builder gamejetton_ssz = GameJettonSsz.newBuilder();
		RoomInfoSsz.Builder room_info = getRoomInfoSsz();
		gamejetton_ssz.setRoomInfo(room_info);

		gamejetton_ssz.setCellScore((int) this.game_cell);
		for (int i = 1; i < 5; i++) {
			gamejetton_ssz.addJettonScore(i);
		}
		gamejetton_ssz.setBankerPlayer(this.GRR._banker_player);
		gamejetton_ssz.setDisplayTime(10);
		this.load_player_info_data_jetton_start(gamejetton_ssz);
		roomResponse.setCommResponse(PBUtil.toByteString(gamejetton_ssz));

		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return true;
	}

	// 十三张发牌
	@Override
	public boolean Send_card() {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		// 刷新手牌
		_deal_start_time = System.currentTimeMillis() / 1000L;
		for (int index = 0; index < this.getTablePlayerNumber(); index++) {
			if (this.get_players()[index] == null) {
				continue;
			}
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_SSZ_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			// 发送数据
			Send_Card_Data.Builder send_card = Send_Card_Data.newBuilder();
			RoomInfoSsz.Builder room_info = getRoomInfoSsz();
			send_card.setRoomInfo(room_info);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (index == i) {
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				}

				send_card.addCardCount(GRR._card_count[i]);

				send_card.addCardsData(cards_card);
			}
			int tui_jian_card[][] = new int[3][this.get_hand_card_count_max()];
			int tui_jian_type[][] = new int[3][3];
			for (int i = 0; i < 3; i++) {
				Arrays.fill(tui_jian_type[i], GameConstants.SSZ_CT_INVALID);
			}

			int type_count = this._logic.search_all_type(GRR._cards_data[index], GRR._card_count[index], tui_jian_card, tui_jian_type);
			for (int i = 0; i < type_count; i++) {
				Int32ArrayResponse.Builder tui_jian_cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder tui_jian_cards_type = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < get_hand_card_count_max(); j++) {
					tui_jian_cards_card.addItem(tui_jian_card[i][j]);
				}
				for (int j = 0; j < 3; j++) {
					if (tui_jian_type[i][j] != GameConstants.SSZ_CT_INVALID) {
						tui_jian_cards_type.addItem(tui_jian_type[i][j]);
					}
				}
				send_card.addTuiJianCard(tui_jian_cards_card);
				send_card.addCardType(tui_jian_cards_type);
			}
			send_card.setBankerPlayer(this.GRR._banker_player);
			long display_time = _deal_card_time - ((System.currentTimeMillis() / 1000L) - _deal_start_time);
			send_card.setDisplayTime((int) display_time);
			// gamestart_ssz.setCardType(_logic.GetCardType(GRR._cards_data[index],
			// GRR._card_count[index]));

			roomResponse.setCommResponse(PBUtil.toByteString(send_card));

			this.send_response_to_player(index, roomResponse);
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_SEND_CARD);
		roomResponse.setGameStatus(this._game_status);
		// 发送数据
		Send_Card_Data.Builder send_card = Send_Card_Data.newBuilder();
		RoomInfoSsz.Builder room_info = getRoomInfoSsz();
		send_card.setRoomInfo(room_info);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			send_card.addCardCount(GRR._card_count[i]);

			send_card.addCardsData(cards_card);
		}
		send_card.setBankerPlayer(_cur_banker);
		send_card.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(send_card));

		GRR.add_room_response(roomResponse);

		if (_deal_card_time != 0) {
			this.set_timer(ID_TIMER_DEAL_CARD, _deal_card_time);
		}
		return true;
	}

	@Override
	public int get_hand_card_count_max() {
		return GameConstants.SSZ_MAX_COUNT;
	}

	/**
	 * 是否托管
	 * 
	 * @param seat_index
	 * @return
	 */
	@Override
	public boolean isTrutess(int seat_index) {
		if (seat_index < 0 || seat_index > this.getTablePlayerNumber())
			return false;
		return istrustee[seat_index];
	}

	@Override
	public boolean animation_timer(int timer_id) {
		switch (timer_id) {
		case ID_TIMER_DEAL_CARD: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 1 && !_is_open_card[i]) {
					int tui_jian_card[][] = new int[3][this.get_hand_card_count_max()];
					int tui_jian_type[][] = new int[3][3];
					for (int j = 0; j < 3; j++) {
						Arrays.fill(tui_jian_type[j], GameConstants.SSZ_CT_INVALID);
					}
					int type_count = this._logic.search_all_type(GRR._cards_data[i], GRR._card_count[i], tui_jian_card, tui_jian_type);
					List<Integer> tui_jian_cards_card = new ArrayList<Integer>();
					for (int j = 0; j < get_hand_card_count_max(); j++) {
						tui_jian_cards_card.add(tui_jian_card[0][j]);
					}

					int is_special_type = 0;
					if (this._logic.GetSpecialCard_type(GRR._cards_data[i], get_hand_card_count_max()) != GameConstants.SSZ_CT_INVALID) {
						is_special_type = 1;
					}
					handler_open_card(i, tui_jian_cards_card, GRR._card_count[i], is_special_type);
				}
			}

			return true;
		}
		case ID_TIMER_START_TO_CALLBANKER: {
			call_banker_start();
			return true;
		}
		case ID_TIMER_START_TO_SEND_CARD: {
			Send_card();
			return true;
		}
		case ID_TIMER_AUTO_READY: {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				if (this._player_ready[i] == 0) {
					this.handler_player_ready(i, false);
				}
			}
			return true;
		}
		}
		return true;
	}

	private void test_cards() {

		// int cards[] = new int[] { 0x03, 0x23, 0x33, 0x34, 0x35, 0x06, 0x17,
		// 0x27, 0x08, 0x28, 0x1a, 0x3a, 0x1c, 0x3C, 0x2d,
		// 0x13, 0x04, 0x14, 0x24, 0x15, 0x16, 0x26, 0x36, 0x37, 0x38, 0x19,
		// 0x39, 0x2a, 0x0B, 0x3b,
		// 0x05, 0x25, 0x07, 0x18, 0x09, 0x29, 0x0a, 0x1b, 0x2b, 0x0c, 0x2c,
		// 0x1d, 0x3d, 0x31, 0x32};
		// int cards[] = new int[]
		// {0x01,0x1D,0x1C,0x3B,0x1B,0x3A,0x37,0x07,0x16,0x06,0x14,0x04,0x03};
		int cards[] = new int[] { 0x01, 0x0D, 0x0C, 0x11, 0x15, 0x17, 0x18, 0x19, 0x31, 0x32, 0x38, 0x39, 0x3a, 41, 60, 56, 21, 23, 36, 53, 24, 5, 3,
				57, 44, 45 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				if (this._player_ready[i] == 1) {
					GRR._cards_data[i][j] = cards[index++];
				}
			}
		}

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 13) {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testRealyCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				} else {
					int[] temps = new int[debug_my_cards.length];
					System.arraycopy(debug_my_cards, 0, temps, 0, temps.length);
					testSameCard(temps, debug_my_cards.length);
					debug_my_cards = null;
				}
			}

		}
		int count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			_logic.SortCardList(GRR._cards_data[i], GRR._card_count[i]);
		}
	}

	@Override
	public void load_player_info_data_jetton_start(GameJettonSsz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseSsz.Builder room_player = RoomPlayerResponseSsz.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void load_player_info_data_call_banker_start(GameCallBankerSsz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseSsz.Builder room_player = RoomPlayerResponseSsz.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void load_player_info_data_game_end(PukeGameEndSsz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseSsz.Builder room_player = RoomPlayerResponseSsz.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void load_player_info_data_reconnect(TableResponseSsz.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponseSsz.Builder room_player = RoomPlayerResponseSsz.newBuilder();
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.get_hand_card_count_max(); j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		int send_count;
		int have_send_count = 0;
		int player_number = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1) {
				player_number++;
			}
		}
		_cur_banker = 0;
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				if (_player_ready[i] == 1) {
					for (int j = 0; j < count / player_number; j++) {
						GRR._cards_data[i][j] = realyCards[k++];
					}
				}

			}
			if (i == this.getTablePlayerNumber())
				break;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards, int count) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_index[i][j] = 0;
			}

		}
		this._repertory_card = cards;
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[k++];
			}
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	// 摸起来的牌 派发牌
	@Override
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {

		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		ret = this.handler_game_finish_sszjd(seat_index, reason);
		return ret;
	}

	public boolean handler_game_finish_sszjd(int seat_index, int reason) {
		int real_reason = reason;

		if (_game_status == GameConstants.GS_MJ_WAIT) {
			real_reason = GameConstants.Game_End_RELEASE_NO_BEGIN;
		}
		_game_status = GameConstants.GS_MJ_WAIT;
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		int fitst_Win[][] = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		int second_Win[][] = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		int three_Win[][] = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		int end_score[] = new int[this.getTablePlayerNumber()];
		int daqiang_score[] = new int[this.getTablePlayerNumber()];
		int quanleida_score[] = new int[this.getTablePlayerNumber()];
		int extera_score_first[] = new int[this.getTablePlayerNumber()];
		int extera_score_second[] = new int[this.getTablePlayerNumber()];
		int extera_score_three[] = new int[this.getTablePlayerNumber()];
		int special_score[][] = new int[getTablePlayerNumber()][getTablePlayerNumber()];
		int first_sort[] = new int[this.getTablePlayerNumber()];
		int second_sort[] = new int[this.getTablePlayerNumber()];
		int three_sort[] = new int[this.getTablePlayerNumber()];
		Arrays.fill(end_score, 0);
		Arrays.fill(daqiang_score, 0);
		Arrays.fill(quanleida_score, 0);
		Arrays.fill(first_sort, GameConstants.INVALID_SEAT);
		Arrays.fill(second_sort, GameConstants.INVALID_SEAT);
		Arrays.fill(three_sort, GameConstants.INVALID_SEAT);
		int sort_num = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Arrays.fill(fitst_Win[i], 0);
			Arrays.fill(second_Win[i], 0);
			Arrays.fill(three_Win[i], 0);
			Arrays.fill(special_score[i], 0);

			if (this._player_ready[i] == 1) {
				first_sort[sort_num] = i;
				second_sort[sort_num] = i;
				three_sort[sort_num++] = i;
			}

		}
		if (GRR != null && reason == GameConstants.Game_End_NORMAL) {
			// 计算分数
			cal_score_ssz_jd(end_score, fitst_Win, second_Win, three_Win, special_score, extera_score_first, extera_score_second, extera_score_three,
					daqiang_score, quanleida_score);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (end_score[i] > 0) {
					_win_num[i]++;
				} else if (end_score[i] < 0) {
					_lose_num[i]++;
				} else {
					_draw_num[i]++;
				}
			}
			cal_win_sort(first_sort, second_sort, three_sort);
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndSsz.Builder game_end_ssz = PukeGameEndSsz.newBuilder();

		RoomInfoSsz.Builder room_info = getRoomInfoSsz();
		game_end_ssz.setRoomInfo(room_info);
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {
		}
		game_end_ssz.setGameRound(_game_round);
		game_end_ssz.setCurRound(_cur_round);

		if (GRR != null) {
			game_end_ssz.setBankerPlayer(GRR._banker_player);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addGameScore(end_score[i]);

				game_end_ssz.addEndScore(end_score[i]);

				int first = 0;
				int second = 0;
				int three = 0;
				int is_quan_lei_da = 1;// 是否全垒打
				int is_special_type = 0;// 是否存在特殊牌型
				Int32ArrayResponse.Builder is_da_qiang = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder special_type_score = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (this.get_players()[j] == null || this._player_ready[j] == 0) {
						is_da_qiang.addItem(0);
						is_quan_lei_da = 0;
						continue;
					}
					if (i == j) {
						is_da_qiang.addItem(0);
						continue;
					}
					first += fitst_Win[i][j];
					second += second_Win[i][j];
					three += three_Win[i][j];
					special_type_score.addItem(special_score[i][j]);
					if (special_score[i][j] > 0) {
						is_special_type = 1;
					}
					if (fitst_Win[i][j] > 0 && second_Win[i][j] > 0 && three_Win[i][j] > 0) {
						if (!has_rule(GameConstants.GAME_RULE_SSZ_JD_DAQIANG_DOUBLE) && !has_rule(GameConstants.GAME_RULE_SSZ_JD_DAQIANG_ADDONE)) {
							is_da_qiang.addItem(0);
						} else {
							is_da_qiang.addItem(1);
						}

					} else {
						is_da_qiang.addItem(0);
						is_quan_lei_da = 0;
					}
				}
				if (!has_rule(GameConstants.GAME_RULE_SSZ_JD_SWAT_DOUBLE)) {
					is_quan_lei_da = 0;
				}
				if (first_sort[i] != GameConstants.INVALID_SEAT) {
					game_end_ssz.addFirstSort(first_sort[i]);
				}
				if (second_sort[i] != GameConstants.INVALID_SEAT) {
					game_end_ssz.addSecondSort(second_sort[i]);
				}
				if (three_sort[i] != GameConstants.INVALID_SEAT) {
					game_end_ssz.addThreeSort(three_sort[i]);
				}

				game_end_ssz.addExtraScoreFirst(extera_score_first[i]);
				game_end_ssz.addExtraScoreSecond(extera_score_second[i]);
				game_end_ssz.addExtraScoreThree(extera_score_three[i]);

				game_end_ssz.addSpecialScore(special_type_score);
				game_end_ssz.addIsQuanLeiDa(is_quan_lei_da);
				game_end_ssz.addIsDaQiang(is_da_qiang);
				game_end_ssz.addFirstScore(first);
				game_end_ssz.addSecondScore(second);
				game_end_ssz.addThreeScore(three);
				game_end_ssz.addJettonScore(this._jetton_score[i]);
				game_end_ssz.addDaQiangScore(daqiang_score[i]);
				game_end_ssz.addQuanLeiScore(quanleida_score[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder cards_type = Int32ArrayResponse.newBuilder();
				int card_data[] = new int[GRR._card_count[i]];
				for (int j = 0; j < GRR._card_count[i]; j++) {
					card_data[j] = GRR._cards_data[i][j];
				}
				int card_type = this._logic.GetSpecialCard_type(card_data, GRR._card_count[i]);
				if (GameConstants.SSZ_CT_INVALID != card_type && this._is_special[i]) {
					cards_type.addItem(card_type);
					is_special_type = 1;
				} else {
					int card_temp[] = new int[5];
					int type = GameConstants.SSZ_CT_INVALID;
					for (int j = 0; j < 3; j++) {
						card_temp[j] = GRR._cards_data[i][j];
					}

					type = _logic.GetCardType(card_temp, 3);
					if (type == GameConstants.SSZ_CT_THREE) {
						type = GameConstants.SSZ_CT_CHONG_SAN;
					}
					cards_type.addItem(type);
					for (int j = 0; j < 5; j++) {
						card_temp[j] = GRR._cards_data[i][3 + j];
					}
					type = _logic.GetCardType(card_temp, 5);
					if (type == GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE) {
						type = GameConstants.SSZ_CT_ZHOGNDUN_HULU;
					}
					cards_type.addItem(type);
					for (int j = 0; j < 5; j++) {
						card_temp[j] = GRR._cards_data[i][8 + j];
					}
					type = _logic.GetCardType(card_temp, 5);
					cards_type.addItem(type);
				}
				if (reason == GameConstants.Game_End_NORMAL) {
					game_end_ssz.addCardCount(GRR._card_count[i]);
					for (int j = 0; j < GRR._card_count[i]; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}

				} else {
					game_end_ssz.addCardCount(0);
				}
				game_end_ssz.addIsSpecialType(is_special_type);
				game_end_ssz.addCardType(cards_type);
				game_end_ssz.addCardsData(cards_card);
			}
			game_end.setGameTypeIndex(GRR._game_type_index);
			game_end_ssz.setStartTime(GRR._start_time);
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setPlayerResult(this.process_player_result(reason));
				game_end_ssz.setStartTime(this._game_start_time);
				real_reason = GameConstants.Game_End_ROUND_OVER;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_ssz.addAllEndScore((int) _player_result.game_score[i]);
					game_end_ssz.addLoseNum(_lose_num[i]);
					game_end_ssz.addWinNum(_win_num[i]);
					game_end_ssz.addDrawNum(_draw_num[i]);
				}

				if (this._release_scheduled != null) {
					RoomResponse.Builder roomResponse_release = RoomResponse.newBuilder();
					roomResponse_release.setReleaseTime(150);
					roomResponse_release.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
					roomResponse_release.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
					roomResponse_release.setOperateCode(1);
					int l = (int) (_request_release_time - System.currentTimeMillis() / 1000);
					if (l <= 0) {
						l = 1;
					}
					roomResponse_release.setLeftTime(l);
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						roomResponse_release.addReleasePlayers(2);
					}
					this.send_response_to_room(roomResponse);
					_release_scheduled.cancel(false);
					_release_scheduled = null;
				}

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			game_end_ssz.setStartTime(this._game_start_time);
			game_end.setPlayerResult(this.process_player_result(reason));
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_ssz.addAllEndScore((int) _player_result.game_score[i]);
				game_end_ssz.addLoseNum(_lose_num[i]);
				game_end_ssz.addWinNum(_win_num[i]);
				game_end_ssz.addDrawNum(_draw_num[i]);
			}
			if (real_reason != GameConstants.Game_End_RELEASE_NO_BEGIN) {
				real_reason = GameConstants.Game_End_RELEASE_PLAY;
			}

			end = true;
		}
		game_end_ssz.setReason(real_reason);
		game_end_ssz.setEndTime(System.currentTimeMillis() / 1000L);

		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		load_player_info_data_game_end(game_end_ssz);
		game_end.setCommResponse(PBUtil.toByteString(game_end_ssz));
		game_end.setRoundOverType(1);

		roomResponse.setGameEnd(game_end);
		this.send_response_to_room(roomResponse);
		if (real_reason != GameConstants.Game_End_RELEASE_NO_BEGIN) {
			record_game_round(game_end, real_reason);
		}

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
		GRR = null;

		Arrays.fill(_is_open_card, false);

		this.set_timer(ID_TIMER_AUTO_READY, _auto_ready_time + 15);
		refresh_colock(GameConstants.INVALID_SEAT);
		// 错误断言
		return false;
	}

	// 结算分数
	public void cal_score_ssz_jd(int end_score[], int fitst_Win[][], int second_Win[][], int three_Win[][], int special_score[][],
			int extera_score_first[], int extera_score_second[], int extera_score_three[], int da_qiang_score[], int quan_lei_da_score[]) {
		if (!has_rule(GameConstants.GAME_RULE_SSZ_JD_ZHUANG_TONGBI)) {

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null || this._player_ready[i] == 0) {
					continue;
				}

				if (i == this.GRR._banker_player) {
					continue;
				}

				if (_is_special[i] || _is_special[GRR._banker_player]) {
					int card_data[] = new int[GRR._card_count[i]];
					int card_data_other[] = new int[GRR._card_count[GRR._banker_player]];
					for (int j = 0; j < GRR._card_count[i]; j++) {
						card_data[j] = GRR._cards_data[i][j];
					}
					for (int j = 0; j < GRR._card_count[GRR._banker_player]; j++) {
						card_data_other[j] = GRR._cards_data[GRR._banker_player][j];
					}
					int special_type = GameConstants.SSZ_CT_INVALID;
					int special_type_ohter = GameConstants.SSZ_CT_INVALID;
					if (_is_special[i]) {
						special_type = _logic.GetSpecialCard_type(card_data, GRR._card_count[i]);
					}
					if (_is_special[GRR._banker_player]) {
						special_type_ohter = _logic.GetSpecialCard_type(card_data_other, GRR._card_count[GRR._banker_player]);
					}
					int score = 0;
					if (special_type >= special_type_ohter) {
						score = _logic.get_special_score_jd(special_type);
					} else {
						score = _logic.get_special_score_jd(special_type_ohter);
					}
					if (special_type != GameConstants.SSZ_CT_INVALID || special_type_ohter != GameConstants.SSZ_CT_INVALID) {
						if (special_type >= special_type_ohter) {
							special_score[i][GRR._banker_player] += score;
							special_score[GRR._banker_player][i] -= score;
							end_score[i] += score;
							end_score[GRR._banker_player] -= score;
						} else {
							special_score[GRR._banker_player][i] += score;
							special_score[i][GRR._banker_player] -= score;
							end_score[GRR._banker_player] += score;
							end_score[i] -= score;
						}
					}
					continue;
				}

				int bFirstList[] = new int[13];
				int bNextList[] = new int[13];
				for (int j = 0; j < 3; j++) {
					bFirstList[j] = GRR._cards_data[GRR._banker_player][j];
					bNextList[j] = GRR._cards_data[i][j];
				}
				int type = _logic.CompareCard(bFirstList, bNextList, 3, 3, true, false);
				if (type == 1) {
					fitst_Win[i][GRR._banker_player]++;
					fitst_Win[GRR._banker_player][i]--;
					// 头道三条
					int card_type = _logic.GetCardType(bNextList, 3);
					if (card_type == GameConstants.SSZ_CT_THREE) {
						fitst_Win[i][GRR._banker_player] += 2;
						fitst_Win[GRR._banker_player][i] -= 2;
						extera_score_first[i] += 2;
						extera_score_first[GRR._banker_player] -= 2;
					}
				} else {

					fitst_Win[i][GRR._banker_player]--;
					fitst_Win[GRR._banker_player][i]++;
					// 头道三条
					int card_type = _logic.GetCardType(bFirstList, 3);
					if (card_type == GameConstants.SSZ_CT_THREE) {
						fitst_Win[i][GRR._banker_player] -= 2;
						fitst_Win[GRR._banker_player][i] += 2;
						extera_score_first[i] -= 2;
						extera_score_first[GRR._banker_player] += 2;
					}
				}
				for (int j = 0; j < 5; j++) {
					bFirstList[j] = GRR._cards_data[GRR._banker_player][3 + j];
					bNextList[j] = GRR._cards_data[i][3 + j];
				}
				type = _logic.CompareCard(bFirstList, bNextList, 5, 5, true, false);
				if (type == 1) {
					second_Win[i][GRR._banker_player]++;
					second_Win[GRR._banker_player][i]--;
					// 中道葫芦
					int card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE) {
						second_Win[i][GRR._banker_player] += 1;
						second_Win[GRR._banker_player][i] -= 1;
						extera_score_second[i] += 1;
						extera_score_second[GRR._banker_player] -= 1;
					}
					// 中道铁枝
					card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
						second_Win[i][GRR._banker_player] += 7;
						second_Win[GRR._banker_player][i] -= 7;
						extera_score_second[i] += 7;
						extera_score_second[GRR._banker_player] -= 7;
					}

					// 中道同花顺
					card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
						second_Win[i][GRR._banker_player] += 9;
						second_Win[GRR._banker_player][i] -= 9;
						extera_score_second[i] += 9;
						extera_score_second[GRR._banker_player] -= 9;
					}
					card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
						second_Win[i][GRR._banker_player] += 9;
						second_Win[GRR._banker_player][i] -= 9;
						extera_score_second[i] += 9;
						extera_score_second[GRR._banker_player] -= 9;
					}
				} else {
					second_Win[i][GRR._banker_player]--;
					second_Win[GRR._banker_player][i]++;
					// 中道葫芦
					int card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE) {
						second_Win[i][GRR._banker_player] -= 1;
						second_Win[GRR._banker_player][i] += 1;
						extera_score_second[i] -= 1;
						extera_score_second[GRR._banker_player] += 1;
					}
					// 中道铁枝
					card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
						second_Win[i][GRR._banker_player] -= 7;
						second_Win[GRR._banker_player][i] += 7;
						extera_score_second[i] -= 7;
						extera_score_second[GRR._banker_player] += 7;
					}
					// 中道同花顺
					card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
						second_Win[i][GRR._banker_player] -= 9;
						second_Win[GRR._banker_player][i] += 9;
						extera_score_second[i] -= 9;
						extera_score_second[GRR._banker_player] += 9;
					}
					card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
						second_Win[i][GRR._banker_player] -= 9;
						second_Win[GRR._banker_player][i] += 9;
						extera_score_second[i] -= 9;
						extera_score_second[GRR._banker_player] += 9;
					}
				}
				for (int j = 0; j < 5; j++) {
					bFirstList[j] = GRR._cards_data[GRR._banker_player][8 + j];
					bNextList[j] = GRR._cards_data[i][8 + j];
				}
				type = _logic.CompareCard(bFirstList, bNextList, 5, 5, true, false);
				if (type == 1) {
					three_Win[i][GRR._banker_player]++;
					three_Win[GRR._banker_player][i]--;
					// 尾道铁枝
					int card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
						three_Win[i][GRR._banker_player] += 3;
						three_Win[GRR._banker_player][i] -= 3;
						extera_score_three[i] += 3;
						extera_score_three[GRR._banker_player] -= 3;
					}
					// 尾道同花顺
					card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
						three_Win[i][GRR._banker_player] += 4;
						three_Win[GRR._banker_player][i] -= 4;
						extera_score_three[i] += 4;
						extera_score_three[GRR._banker_player] -= 4;
					}
					card_type = _logic.GetCardType(bNextList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
						three_Win[i][GRR._banker_player] += 4;
						three_Win[GRR._banker_player][i] -= 4;
						extera_score_three[i] += 4;
						extera_score_three[GRR._banker_player] -= 4;
					}
				} else {
					three_Win[i][GRR._banker_player]--;
					three_Win[GRR._banker_player][i]++;
					// 尾道铁枝
					int card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
						three_Win[i][GRR._banker_player] -= 3;
						three_Win[GRR._banker_player][i] += 3;
						extera_score_three[i] -= 3;
						extera_score_three[GRR._banker_player] += 3;
					}
					// 尾道同花顺
					card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
						three_Win[i][GRR._banker_player] -= 4;
						three_Win[GRR._banker_player][i] += 4;
						extera_score_three[i] -= 4;
						extera_score_three[GRR._banker_player] += 4;
					}
					card_type = _logic.GetCardType(bFirstList, 5);
					if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
						three_Win[i][GRR._banker_player] -= 4;
						three_Win[GRR._banker_player][i] += 4;
						extera_score_three[i] -= 4;
						extera_score_three[GRR._banker_player] += 4;
					}
				}
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i != GRR._banker_player) {
					int score = (int) this.game_cell;
					end_score[i] += (fitst_Win[i][GRR._banker_player] + second_Win[i][GRR._banker_player] + three_Win[i][GRR._banker_player]) * score;
					end_score[GRR._banker_player] -= (fitst_Win[i][GRR._banker_player] + second_Win[i][GRR._banker_player]
							+ three_Win[i][GRR._banker_player]) * score;
					_player_result.game_score[i] += end_score[i];
				}
			}
			_player_result.game_score[GRR._banker_player] += end_score[GRR._banker_player];
		} else {
			int win_index = GameConstants.INVALID_SEAT;
			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				if (this.get_players()[play_index] == null || this._player_ready[play_index] == 0) {
					continue;
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null || this._player_ready[i] == 0) {
						continue;
					}
					if (i == play_index) {
						continue;
					}
					if (!_is_special[i] && !_is_special[play_index]) {
						continue;
					}
					int card_data[] = new int[GRR._card_count[play_index]];
					int card_data_other[] = new int[GRR._card_count[i]];
					for (int j = 0; j < GRR._card_count[play_index]; j++) {
						card_data[j] = GRR._cards_data[play_index][j];
					}
					for (int j = 0; j < GRR._card_count[i]; j++) {
						card_data_other[j] = GRR._cards_data[i][j];
					}
					int special_type = GameConstants.SSZ_CT_INVALID;
					int special_type_ohter = GameConstants.SSZ_CT_INVALID;
					if (_is_special[play_index]) {
						special_type = _logic.GetSpecialCard_type(card_data, GRR._card_count[play_index]);
					}
					if (_is_special[i]) {
						special_type_ohter = _logic.GetSpecialCard_type(card_data_other, GRR._card_count[i]);
					}
					if (special_type > special_type_ohter) {
						int score = _logic.get_special_score_jd(special_type);
						score = _logic.get_special_score_jd(special_type);
						special_score[play_index][i] += score;
						end_score[play_index] += score;
						end_score[i] -= score;
					}
				}
				for (int i = play_index + 1; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null || this._player_ready[i] == 0) {
						continue;
					}
					if (i == play_index) {
						continue;
					}
					if (_is_special[i] || _is_special[play_index]) {
						continue;
					}
					int bFirstList[] = new int[13];
					int bNextList[] = new int[13];
					for (int j = 0; j < 3; j++) {
						bFirstList[j] = GRR._cards_data[play_index][j];
						bNextList[j] = GRR._cards_data[i][j];
					}
					int type = _logic.CompareCard(bFirstList, bNextList, 3, 3, true, false);
					if (type == 1) {
						fitst_Win[i][play_index]++;
						fitst_Win[play_index][i]--;
						// 头道三条
						int card_type = _logic.GetCardType(bNextList, 3);
						if (card_type == GameConstants.SSZ_CT_THREE) {
							fitst_Win[i][play_index] += 2;
							fitst_Win[play_index][i] -= 2;
							extera_score_first[i] += 2;
							extera_score_first[play_index] -= 2;
						}
					} else if (type == -1) {
						fitst_Win[i][play_index]--;
						fitst_Win[play_index][i]++;
						// 头道三条
						int card_type = _logic.GetCardType(bFirstList, 3);
						if (card_type == GameConstants.SSZ_CT_THREE) {
							fitst_Win[i][play_index] -= 2;
							fitst_Win[play_index][i] += 2;
							extera_score_first[i] -= 2;
							extera_score_first[play_index] += 2;
						}
					}
					for (int j = 0; j < 5; j++) {
						bFirstList[j] = GRR._cards_data[play_index][3 + j];
						bNextList[j] = GRR._cards_data[i][3 + j];
					}
					type = _logic.CompareCard(bFirstList, bNextList, 5, 5, true, false);
					if (type == 1) {
						second_Win[i][play_index]++;
						second_Win[play_index][i]--;
						// 中道葫芦
						int card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE) {
							second_Win[i][play_index] += 1;
							second_Win[play_index][i] -= 1;
							extera_score_second[i] += 1;
							extera_score_second[play_index] -= 1;
						}
						// 中道铁枝
						card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
							second_Win[i][play_index] += 7;
							second_Win[play_index][i] -= 7;
							extera_score_second[i] += 7;
							extera_score_second[play_index] -= 7;
						}

						// 中道同花顺
						card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
							second_Win[i][play_index] += 9;
							second_Win[play_index][i] -= 9;
							extera_score_second[i] += 9;
							extera_score_second[play_index] -= 9;
						}
						card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
							second_Win[i][play_index] += 9;
							second_Win[play_index][i] -= 9;
							extera_score_second[i] += 9;
							extera_score_second[play_index] -= 9;
						}
					} else if (type == -1) {
						second_Win[i][play_index]--;
						second_Win[play_index][i]++;
						// 中道葫芦
						int card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_THREE_DEOUBLE) {
							second_Win[i][play_index] -= 1;
							second_Win[play_index][i] += 1;
							extera_score_second[i] -= 1;
							extera_score_second[play_index] += 1;
						}
						// 中道铁枝
						card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
							second_Win[i][play_index] -= 7;
							second_Win[play_index][i] += 7;
							extera_score_second[i] -= 7;
							extera_score_second[play_index] += 7;
						}
						// 中道同花顺
						card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
							second_Win[i][play_index] -= 9;
							second_Win[play_index][i] += 9;
							extera_score_second[i] -= 9;
							extera_score_second[play_index] += 9;
						}
						card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
							second_Win[i][play_index] -= 9;
							second_Win[play_index][i] += 9;
							extera_score_second[i] -= 9;
							extera_score_second[play_index] += 9;
						}
					}
					for (int j = 0; j < 5; j++) {
						bFirstList[j] = GRR._cards_data[play_index][8 + j];
						bNextList[j] = GRR._cards_data[i][8 + j];
					}
					type = _logic.CompareCard(bFirstList, bNextList, 5, 5, true, false);
					if (type == 1) {
						three_Win[i][play_index]++;
						three_Win[play_index][i]--;
						// 尾道铁枝
						int card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
							three_Win[i][play_index] += 3;
							three_Win[play_index][i] -= 3;
							extera_score_three[i] += 3;
							extera_score_three[play_index] -= 3;
						}
						// 尾道同花顺
						card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
							three_Win[i][play_index] += 4;
							three_Win[play_index][i] -= 4;
							extera_score_three[i] += 4;
							extera_score_three[play_index] -= 4;
						}
						card_type = _logic.GetCardType(bNextList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
							three_Win[i][play_index] += 4;
							three_Win[play_index][i] -= 4;
							extera_score_three[i] += 4;
							extera_score_three[play_index] -= 4;
						}
					} else if (type == -1) {
						three_Win[i][play_index]--;
						three_Win[play_index][i]++;
						// 尾道铁枝
						int card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_FOUR_ONE) {
							three_Win[i][play_index] -= 3;
							three_Win[play_index][i] += 3;
							extera_score_three[i] -= 3;
							extera_score_three[play_index] += 3;
						}
						// 尾道同花顺
						card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH_FIRST_A) {
							three_Win[i][play_index] -= 4;
							three_Win[play_index][i] += 4;
							extera_score_three[i] -= 4;
							extera_score_three[play_index] += 4;
						}
						card_type = _logic.GetCardType(bFirstList, 5);
						if (card_type == GameConstants.SSZ_CT_FIVE_STRAIGHT_FLUSH) {
							three_Win[i][play_index] -= 4;
							three_Win[play_index][i] += 4;
							extera_score_three[i] -= 4;
							extera_score_three[play_index] += 4;
						}
					}
				}
			}
			int player_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 1) {
					player_count++;
				}
			}
			boolean is_quan_lei_da[] = new boolean[getTablePlayerNumber()];
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (_player_ready[i] == 0) {
					continue;
				}
				is_quan_lei_da[i] = true;
			}
			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				if (_player_ready[play_index] == 0) {
					continue;
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (_player_ready[i] == 0 || i == play_index) {
						continue;
					}
					if (player_count >= 4) {
						if (fitst_Win[play_index][i] <= 0 || second_Win[play_index][i] <= 0 || three_Win[play_index][i] <= 0) {
							is_quan_lei_da[play_index] = false;
						}
					} else {
						is_quan_lei_da[play_index] = false;
					}

				}
			}

			int quan_lei_da_times = 1;
			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				if (_player_ready[play_index] == 0) {
					continue;
				}
				if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_SWAT_DOUBLE)) {
					if (is_quan_lei_da[play_index]) {
						quan_lei_da_times *= 2;
					}
				}
				for (int i = play_index + 1; i < getTablePlayerNumber(); i++) {
					if (_player_ready[i] == 0) {
						continue;
					}
					int score = (int) this.game_cell;
					int da_qinag_times = 1;
					if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_DAQIANG_DOUBLE)) {
						if (fitst_Win[play_index][i] > 0 && second_Win[play_index][i] > 0 && three_Win[play_index][i] > 0) {
							da_qinag_times *= 2;
						}
						if (fitst_Win[play_index][i] < 0 && second_Win[play_index][i] < 0 && three_Win[play_index][i] < 0) {
							da_qinag_times *= 2;
						}
						end_score[play_index] += (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i]) * score
								* da_qinag_times;
						if (da_qinag_times > 1) {
							da_qiang_score[play_index] += (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i]) * score;
						}
						end_score[i] -= (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i]) * score * da_qinag_times;
						if (da_qinag_times > 1) {
							da_qiang_score[i] -= (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i]) * score;
						}
					} else if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_DAQIANG_ADDONE)) {
						int add_score = 0;
						if (fitst_Win[play_index][i] > 0 && second_Win[play_index][i] > 0 && three_Win[play_index][i] > 0) {
							add_score = 1;
						}
						if (fitst_Win[play_index][i] < 0 && second_Win[play_index][i] < 0 && three_Win[play_index][i] < 0) {
							add_score = -1;
						}
						end_score[play_index] += (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i] + add_score)
								* score;
						da_qiang_score[play_index] += add_score;
						end_score[i] -= (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i] + add_score) * score;
						da_qiang_score[i] += add_score;
					} else {

						end_score[play_index] += (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i]) * score;
						end_score[i] -= (fitst_Win[play_index][i] + second_Win[play_index][i] + three_Win[play_index][i]) * score;
					}

				}
			}

			// 全垒打分数
			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				if (_player_ready[play_index] == 0) {
					continue;
				}
				end_score[play_index] *= quan_lei_da_times;
				if (quan_lei_da_times > 1) {
					quan_lei_da_score[play_index] += end_score[play_index] / 2;
				}
			}

			for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {
				_player_result.game_score[play_index] += end_score[play_index];
			}

		}

	}

	/**
	 * @return
	 */
	@Override
	public RoomInfoSsz.Builder getRoomInfoSsz() {
		RoomInfoSsz.Builder room_info = RoomInfoSsz.newBuilder();
		room_info.setRoomId(this.getRoom_id());
		room_info.setGameRuleIndex(_game_rule_index);
		room_info.setGameRuleDes(get_game_des());
		room_info.setGameTypeIndex(_game_type_index);
		room_info.setGameRound(_game_round);
		room_info.setCurRound(_cur_round);
		room_info.setGameStatus(_game_status);
		room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		room_info.setBankerPlayer(this._cur_banker);
		room_info.setCreateName(this.getRoom_owner_name());
		room_info.setCellScore((int) this.game_cell);
		if (commonGameRuleProtos != null) {
			room_info.setNewRules(commonGameRuleProtos);
		}

		int beginLeftCard = GameConstants.CARD_COUNT_HH_YX - 60;
		room_info.setBeginLeftCard(beginLeftCard);
		return room_info;
	}

	@Override
	public boolean handler_exit_room_observer(Player player) {
		if (observers().exist(player.getAccount_id())) {
			// PlayerServiceImpl.getInstance().getPlayerMap().remove(player.getAccount_id());
			RoomResponse.Builder quit_roomResponse = RoomResponse.newBuilder();
			quit_roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);// 直接拉出游戏
			observers().send(player, quit_roomResponse);
			observers().exit(player.getAccount_id());
			return true;
		}
		return false;
	}

	@Override
	public boolean handler_observer_be_in_room(Player player) {
		// if(player.getAccount_id() == this.getRoom_owner_account_id())
		// {
		// control_game_start();
		// }
		// if ((GameConstants.GS_MJ_FREE != _game_status &&
		// GameConstants.GS_MJ_WAIT != _game_status) && player != null) {
		// // this.send_play_data(seat_index);
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_ZJH_RECONNECT_DATA);
		//
		// TableResponseZJH.Builder tableResponse_zjh =
		// TableResponseZJH.newBuilder();
		// load_player_info_data_reconnect(tableResponse_zjh);
		// RoomInfoZjh.Builder room_info = getRoomInfoZjh();
		// tableResponse_zjh.setRoomInfo(room_info);
		//
		// tableResponse_zjh.setBankerPlayer(GRR._banker_player);
		// tableResponse_zjh.setCurrentPlayer(_current_player);
		// tableResponse_zjh.setPrevPlayer(_prev_palyer);
		//
		// for(int i=0;i<getTablePlayerNumber();i++){
		// tableResponse_zjh.addIsGiveUp(this.isGiveup[i]);
		// tableResponse_zjh.addIsLookCards(this.isLookCard[i]);
		// tableResponse_zjh.addIsIsLose(this.isLose[i]);
		// tableResponse_zjh.addJettonScore(_user_jetton_score[i]);
		//
		// Int32ArrayResponse.Builder
		// cards_card=Int32ArrayResponse.newBuilder();
		// if(this.get_players()[i] != null && this._player_ready[i] == 1){
		// for(int j=0;j<GRR._card_count[i];j++){
		// cards_card.addItem(GameConstants.BLACK_CARD);
		// }
		// }else{
		// for(int j=0;j<3;j++){
		// cards_card.addItem(GameConstants.INVALID_CARD);
		// }
		// }
		// tableResponse_zjh.addCardCount(GRR._card_count[i]);
		// tableResponse_zjh.addCardsData(cards_card);
		// }
		// tableResponse_zjh.setJettonTotalScore(_jetton_total_score);
		//
		//
		//
		// roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_zjh));
		// observers().sendAll(roomResponse);
		//
		// send_jetton_round();
		// return true;
		// }
		// if (_gameRoomRecord != null) {
		// if (_gameRoomRecord.request_player_seat !=
		// GameConstants.INVALID_SEAT) {
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
		//
		// SysParamModel sysParamModel3007 =
		// SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1)
		// .get(3007);
		// int delay = 60;
		// if (sysParamModel3007 != null) {
		// delay = sysParamModel3007.getVal1();
		// }
		//
		// roomResponse.setReleaseTime(delay);
		// roomResponse.setOperateCode(0);
		// roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
		// roomResponse.setLeftTime((_request_release_time -
		// System.currentTimeMillis()) / 1000);
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
		// }
		// observers().sendAll(roomResponse);
		// }
		// }

		return true;
	}

	@Override
	public boolean handler_enter_room_observer(Player player) {

		int limitCount = 20;
		if (SystemConfig.gameDebug == 1) {
			limitCount = 5;
		}
		// 限制围观者数量，未来加到配置表控制
		if (player.getAccount_id() != getRoom_owner_account_id() && observers().count() >= limitCount) {
			this.send_error_notify(player, 1, "当前游戏围观位置已满,下次赶早!");
			return false;
		}
		observers().enter(player);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setAppId(this.getGame_id());

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		observers().send(player, roomResponse);
		int game_id = SysGameTypeDict.getInstance().getGameIDByTypeIndex(_game_type_index);
		if (player.getAccount_id() == getRoom_owner_account_id()) {
			if (this.is_game_start == 1) {
				control_game_start();
				return true;
			}
			int _cur_count = 0;
			boolean flag = false;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					_player_ready[i] = 0;
				} else {
					_cur_count += 1;
				}

			}
			if ((this.getPlayerCount() >= 2) && (getPlayerCount() == _cur_count)) {
				if (this._cur_round == 0 && has_rule(GameConstants.GAME_RULE_ZJH_CONTORL_START)) {
					this.is_game_start = 1;
					control_game_start();
				}
			} else {
				control_game_start();
			}

		}
		return true;
	}

	@Override
	public boolean control_game_start() {
		// if(has_rule(GameConstants.GAME_RULE_CONTORL_START) == false)
		// return false;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ROOM_OWNER_START);
		roomResponse.setEffectType(this.is_game_start);
		roomResponse.setPaoDes(getRoom_owner_name());
		if (this.is_game_start != 2)
			this.send_response_to_room(roomResponse);
		Player player = observers().getPlayer(getRoom_owner_account_id());
		if (player == null) {
			player = this.get_player(getRoom_owner_account_id());
			if (player != null)
				this.send_response_to_player(player.get_seat_index(), roomResponse);
		} else {
			observers().send(player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		this.log_error("gme_status:" + this._game_status + " seat_index:" + seat_index);
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SSZ_RECONNECT_DATA);

			TableResponseSsz.Builder tableResponse_ssz = TableResponseSsz.newBuilder();
			load_player_info_data_reconnect(tableResponse_ssz);
			RoomInfoSsz.Builder room_info = getRoomInfoSsz();
			tableResponse_ssz.setRoomInfo(room_info);
			tableResponse_ssz.setBankerPlayer(GRR._banker_player);
			tableResponse_ssz.setCurrentPlayer(_current_player);
			tableResponse_ssz.setCellScore((int) this.game_cell);
			tableResponse_ssz.setGameStatus(this._game_status);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				tableResponse_ssz.addCardsData(cards_card);
				tableResponse_ssz.addJettonScore(this._jetton_score[i]);
				tableResponse_ssz.addIsOpenCard(_is_open_card[i]);
				tableResponse_ssz.addCallBankerStatus(_call_banker[i]);
				tableResponse_ssz.addIsCallBanker(_is_call_banker[i]);
				tableResponse_ssz.addIsJettonScore(_is_jetton[i]);
			}

			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[seat_index]; j++) {
				cards_card.addItem(GRR._cards_data[seat_index][j]);
			}
			tableResponse_ssz.setCardType(_logic.GetSpecialCard_type(GRR._cards_data[seat_index], GRR._card_count[seat_index]));
			tableResponse_ssz.setCardsData(seat_index, cards_card);
			if (!_is_open_card[seat_index] && GRR._card_count[seat_index] > 0) {
				int tui_jian_card[][] = new int[3][this.get_hand_card_count_max()];
				int tui_jian_type[][] = new int[3][3];
				for (int i = 0; i < 3; i++) {
					Arrays.fill(tui_jian_type[i], GameConstants.SSZ_CT_INVALID);
				}

				int type_count = this._logic.search_all_type(GRR._cards_data[seat_index], GRR._card_count[seat_index], tui_jian_card, tui_jian_type);
				for (int i = 0; i < type_count; i++) {
					Int32ArrayResponse.Builder tui_jian_cards_card = Int32ArrayResponse.newBuilder();
					Int32ArrayResponse.Builder tui_jian_cards_type = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < get_hand_card_count_max(); j++) {
						tui_jian_cards_card.addItem(tui_jian_card[i][j]);
					}
					for (int j = 0; j < 3; j++) {
						if (tui_jian_type[i][j] != GameConstants.SSZ_CT_INVALID) {
							tui_jian_cards_type.addItem(tui_jian_type[i][j]);
						}
					}
					tableResponse_ssz.addTuiJianCard(tui_jian_cards_card);
					tableResponse_ssz.addTuiJianCardType(tui_jian_cards_type);
				}
			}
			long display_time = _deal_card_time - ((System.currentTimeMillis() / 1000L) - _deal_start_time);
			if (_deal_card_time == 0) {
				display_time = 0;
			}
			tableResponse_ssz.setDisplayTime((int) display_time);

			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ssz));
			send_response_to_player(seat_index, roomResponse);
		}
		if (_gameRoomRecord != null) {
			if (_gameRoomRecord.request_player_seat != GameConstants.INVALID_SEAT) {
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);

				SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
				int delay = 60;
				if (sysParamModel3007 != null) {
					delay = sysParamModel3007.getVal1();
				}

				roomResponse.setReleaseTime(delay);
				roomResponse.setOperateCode(0);
				roomResponse.setRequestPlayerSeat(_gameRoomRecord.request_player_seat);
				roomResponse.setLeftTime((_request_release_time - System.currentTimeMillis()) / 1000);
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}

		if (GameConstants.GS_MJ_WAIT == _game_status) {
			refresh_colock(seat_index);
		}
		// if(this._cur_round > 0 )
		// return handler_player_ready(seat_index,false);
		return true;

	}

	/**
	 * //用户出牌
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean handler_player_out_card(int seat_index, int card) {

		// if (this._handler != null) {
		// this._handler.handler_player_out_card(this, seat_index, card);
		// }

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param out_cards[]
	 * @param card_count
	 * @param b_out_card
	 * @return
	 */
	public boolean handler_operate_out_card_mul(int get_seat_index, List<Integer> list, int card_count, int b_out_card) {
		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(int seat_index, int operate_code, int operate_card, int luoCode) {

		// if (this._handler != null) {
		// this._handler.handler_operate_card(this, seat_index, operate_code,
		// operate_card, luoCode);
		// }

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param operate_code
	 * @return
	 */
	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		if (_game_status != GameConstants.GS_MJ_PAO) {
			return false;
		}
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	public boolean handler_open_card(int get_seat_index, List<Integer> list, int card_count, int is_special) {
		if (_is_open_card[get_seat_index]) {
			return true;
		}
		int open_cards[] = new int[card_count];
		for (int i = 0; i < list.size(); i++) {
			open_cards[i] = list.get(i);

		}

		if (is_special <= 0) {
			int bopen_cardsList[] = new int[13];
			for (int i = 0; i < 3; i++) {
				bopen_cardsList[i] = open_cards[i];
			}
			this._logic.SortCardList(bopen_cardsList, 3);
			for (int i = 0; i < 3; i++) {
				open_cards[i] = bopen_cardsList[i];
			}
			for (int i = 0; i < 5; i++) {
				bopen_cardsList[i] = open_cards[3 + i];
			}
			this._logic.SortCardList(bopen_cardsList, 5);
			for (int i = 0; i < 5; i++) {
				open_cards[3 + i] = bopen_cardsList[i];
			}

			for (int i = 0; i < 5; i++) {
				bopen_cardsList[i] = open_cards[8 + i];
			}
			this._logic.SortCardList(bopen_cardsList, 5);
			for (int i = 0; i < 5; i++) {
				open_cards[8 + i] = bopen_cardsList[i];
			}

			int type = this._logic.pailie_zhengque(open_cards, card_count);
			if (type == 1) {
				this.send_error_notify(get_seat_index, 2, "第二道必须大于第一道");
				return false;
			} else if (type == 2) {
				this.send_error_notify(get_seat_index, 2, "第三道必须大于第二道");
				return false;
			}
			_is_special[get_seat_index] = false;
		} else {
			if (_logic.GetSpecialCard_type(open_cards, card_count) == GameConstants.SSZ_CT_INVALID) {
				this.send_error_notify(get_seat_index, 2, "请选择正确的牌型");
				return false;
			}
			_is_special[get_seat_index] = true;
		}

		if (!this._logic.remove_cards_by_data(GRR._cards_data[get_seat_index], card_count, open_cards, card_count)) {
			return false;
		}
		_is_open_card[get_seat_index] = true;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_OPEN_CARD);
		// 发送数据
		SSZ_OpenCardResult.Builder open_card_ssz = SSZ_OpenCardResult.newBuilder();
		open_card_ssz.setOpreateSeatIndex(get_seat_index);

		roomResponse.setCommResponse(PBUtil.toByteString(open_card_ssz));

		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

		for (int i = 0; i < card_count; i++) {
			GRR._cards_data[get_seat_index][i] = open_cards[i];
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (!_is_open_card[i] && this._player_ready[i] == 1) {
				return true;
			}
		}

		// GameSchedule.put(new GameFinishRunnable(getRoom_id(),
		// this.GRR._banker_player, GameConstants.Game_End_NORMAL),
		// GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		this.kill_timer();
		handler_game_finish(this.GRR._banker_player, GameConstants.Game_End_NORMAL);

		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 * @return
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		if (!this.has_rule(GameConstants.GAME_RULE_SSZ_JD_QIANG_ZHUANG)) {
			return false;
		}
		if (_cur_banker != GameConstants.INVALID_SEAT || _game_status != GameConstants.GS_CALL_BANKER) {
			return false;
		}
		if (_is_call_banker[seat_index]) {
			return false;
		}
		_is_call_banker[seat_index] = true;
		_call_banker[seat_index] = call_banker;
		boolean call_banker_finish = true;
		int call_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null || _player_ready[i] == 0) {
				continue;
			}
			if (!_is_call_banker[i]) {
				call_banker_finish = false;
				break;
			}
			if (_call_banker[i] == 1) {
				call_count++;
			}

		}
		if (call_banker_finish) {
			if (call_count == 0) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null || _player_ready[i] == 0) {
						continue;
					}
					call_count++;
				}
				int tager_index = (RandomUtil.getRandomNumber(Integer.MAX_VALUE)) % call_count;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null || _player_ready[i] == 0) {
						continue;
					}
					if (tager_index == 0) {
						_cur_banker = i;
						break;
					} else {
						tager_index--;
					}
				}
			} else {
				int tager_index = (RandomUtil.getRandomNumber(Integer.MAX_VALUE)) % call_count;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this.get_players()[i] == null || _player_ready[i] == 0) {
						continue;
					}
					if (tager_index == 0) {
						if (_call_banker[i] == 1) {
							_cur_banker = i;
							break;
						}
					} else {
						if (_call_banker[i] == 1) {
							tager_index--;
						}
					}
				}
			}

			this.GRR._banker_player = _cur_banker;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_CALLBANKER_RESULT);
		// 发送数据
		CallBankerResult.Builder callbanker_result_ssz = CallBankerResult.newBuilder();
		callbanker_result_ssz.setCallBanker(call_banker);
		callbanker_result_ssz.setOpreateSeatIndex(seat_index);
		callbanker_result_ssz.setBankerSeatIndex(this.GRR._banker_player);
		callbanker_result_ssz.setCurrentSeatIndex(GameConstants.INVALID_SEAT);

		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result_ssz));

		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		if (this.GRR._banker_player != GameConstants.INVALID_SEAT) {
			// this.Send_card();
			set_timer(ID_TIMER_START_TO_SEND_CARD, 2);
		}
		return true;
	}

	@Override
	public boolean handler_Jetton_score(int seat_index, int jetton_score) {
		if (_game_status != GameConstants.GS_MJ_PAO) {
			return false;
		}
		if (_is_jetton[seat_index]) {
			return false;
		}
		if (jetton_score > 4) {
			return false;
		}
		_is_jetton[seat_index] = true;
		_jetton_score[seat_index] = jetton_score;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SSZ_JETTON_RESULT);

		// 发送数据
		JettonResult.Builder jetton_result_ssz = JettonResult.newBuilder();
		jetton_result_ssz.setJettonScore(jetton_score);
		jetton_result_ssz.setOpreateSeatIndex(seat_index);

		roomResponse.setCommResponse(PBUtil.toByteString(jetton_result_ssz));

		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (!_is_jetton[i] && i != this.GRR._banker_player) {
				return true;
			}
		}
		this.Send_card();
		// GameSchedule.put(new SSZTimeRunnable(getRoom_id(), this,1),
		// 2, TimeUnit.SECONDS);
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_SSZ_JETTON) {
			SSZ_JettonRequest req = PBUtil.toObject(room_rq, SSZ_JettonRequest.class);
			return handler_Jetton_score(seat_index, req.getJettonScore());
		}
		if (type == MsgConstants.REQUST_SSZ_CALLBANKER) {
			SSZ_CallBankerRequest req = PBUtil.toObject(room_rq, SSZ_CallBankerRequest.class);
			return handler_call_banker(seat_index, req.getCallBanker());
		}
		if (type == MsgConstants.REQUST_SSZ_OPENCARD) {
			SSZ_OpenCardRequest req = PBUtil.toObject(room_rq, SSZ_OpenCardRequest.class);
			return handler_open_card(seat_index, req.getCardsDataList(), req.getCardCount(), req.getIsSpecial());
		}
		return true;
	}

	@Override
	public boolean handler_ox_game_start(int room_id, long account_id) {
		if (is_game_start == 2)
			return true;
		is_game_start = 2;
		control_game_start();

		handler_game_start();
		boolean nt = true;
		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return true;
	}

	/**
	 * @param seat_index
	 * @param jetton
	 * @return
	 */
	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	/**
	 * @param seat_index
	 * @param open_flag
	 * @return
	 */
	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

	/**
	 * 释放
	 */
	@Override
	public boolean process_release_room() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_FORCE_EXIT);
		this.send_response_to_room(roomResponse);

		if (_cur_round == 0) {
			Player player = null;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				player = this.get_players()[i];
				if (player == null)
					continue;
				send_error_notify(i, 2, "游戏等待超时解散");

			}
		} else {
			this.handler_game_finish(GameConstants.INVALID_SEAT, GameConstants.Game_End_RELEASE_PLAY);

		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this.get_players()[i] = null;

		}

		if (_table_scheduled != null)
			_table_scheduled.cancel(false);

		// 删除房间
		PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());

		return true;

	}

	// 18 玩家申请解散房间( operate_code=1发起解散2=同意解散3=不同意解散)
	/*
	 * 
	 * Release_Room_Type_SEND = 1, //发起解散 Release_Room_Type_AGREE, //同意
	 * Release_Room_Type_DONT_AGREE, //不同意 Release_Room_Type_CANCEL,
	 * //还没开始,房主解散房间 Release_Room_Type_QUIT //还没开始,普通玩家退出房间
	 */
	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {

		return false;

	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {

		return true;
	}

	/////////////////////////////////////////////////////// send///////////////////////////////////////////
	/////
	/**
	 * 基础状态
	 * 
	 * @return
	 */
	@Override
	public boolean operate_player_status() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_STATUS);// 29
		this.load_common_status(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 刷新玩家信息
	 * 
	 * @return
	 */
	@Override
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	/**
	 * 在玩家的前面显示出的牌 --- 发送玩家出牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param type
	 * @param to_player
	 * @return
	 */
	//
	@Override
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {
		return false;
	}

	/**
	 * 效果
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
	@Override
	public boolean operate_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		if (to_player == GameConstants.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long effect_indexs[], int time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(effect_type);
		roomResponse.setTarget(seat_index);
		roomResponse.setEffectCount(effect_count);
		for (int i = 0; i < effect_count; i++) {
			roomResponse.addEffectsIndex(effect_indexs[i]);
		}

		roomResponse.setEffectTime(time);

		GRR.add_room_response(roomResponse);

		return true;
	}

	/**
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 * @return
	 */
	@Override
	public boolean operate_player_cards() {

		return true;
	}

	@Override
	public boolean operate_player_cards_flag(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(2);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 删除牌堆的牌 (吃碰杆的那张牌 从废弃牌堆上移除)
	 * 
	 * @param seat_index
	 * @param discard_index
	 * @return
	 */

	@Override
	public boolean operate_remove_discard(int seat_index, int discard_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REMOVE_DISCARD);
		roomResponse.setTarget(seat_index);
		roomResponse.setDiscardIndex(discard_index);

		GRR.add_room_response(roomResponse);
		this.send_response_to_room(roomResponse);

		return true;
	}

	/***
	 * 刷新特殊描述
	 * 
	 * @param txt
	 * @param type
	 * @return
	 */
	@Override
	public boolean operate_especial_txt(String txt, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_ESPECIAL_TXT);
		roomResponse.setEspecialTxtType(type);
		roomResponse.setEspecialTxt(txt);
		this.send_response_to_room(roomResponse);
		return true;
	}

	/**
	 * 牌局中分数结算
	 * 
	 * @param seat_index
	 * @param score
	 * @return
	 */
	@Override
	public boolean operate_player_score(int seat_index, float[] score) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_PLAYER_SCORE);
		roomResponse.setTarget(seat_index);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addScore(_player_result.game_score[i]);
			roomResponse.addOpereateScore(score[i]);
		}
		this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);

		// 解散的时候默认同意解散
		// if(_gameRoomRecord!=null &&
		// (_gameRoomRecord.request_player_seat!=MJGameConstants.INVALID_SEAT)){
		// this.handler_release_room(player,
		// MJGameConstants.Release_Room_Type_AGREE);
		// }

		return true;
	}

	@Override
	public boolean send_play_data(int seat_index) {
		return true;

	}

	private PlayerResultResponse.Builder process_player_result(int reason) {
		this.huan_dou(reason);
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

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null)
				continue;
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
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
			room_player.setPao(_player_result.pao[i] < 0 ? 0 : _player_result.pao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public boolean send_response_to_special(int seat_index, int to_player, RoomResponse.Builder roomResponse) {

		roomResponse.setRoomId(super.getRoom_id());// 日志用的
		Player player = null;

		player = this.get_players()[to_player];
		if (player == null)
			return true;
		if (to_player == seat_index)
			return true;
		Response.Builder responseBuilder = Response.newBuilder();
		responseBuilder.setResponseType(ResponseType.ROOM);
		responseBuilder.setExtension(Protocol.roomResponse, roomResponse.build());

		PlayerServiceImpl.getInstance().send(this.get_players()[to_player], responseBuilder.build());

		return true;
	}

	public boolean is_mj_type(int type) {
		return _game_type_index == type;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish() {

		return true;
	}

	@Override
	public boolean exe_add_discard(int seat_index, int card_count, int card_data[], boolean send_client, int delay) {
		if (delay == 0) {
			this.runnable_add_discard(seat_index, card_count, card_data, send_client);
			return true;
		}
		GameSchedule.put(new AddDiscardRunnable(getRoom_id(), seat_index, card_count, card_data, send_client, getMaxCount()), delay,
				TimeUnit.MILLISECONDS);

		return true;

	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */
	@Override
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d,
			int delay) {
		delay = GameConstants.PAO_SAO_TI_TIME;
		int gameId = this.getGame_id() == 0 ? 5 : this.getGame_id();
		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
		if (sysParamModel1104 != null && sysParamModel1104.getVal3() > 0 && sysParamModel1104.getVal3() < 10000) {
			delay = sysParamModel1104.getVal3();
		}

		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(this.getRoom_id(), seat_index, provide_player, center_card, action, type, depatch, self, d), delay,
					TimeUnit.MILLISECONDS);
		} else {

		}
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {

		return true;

	}

	/**
	 * 
	 * @param seat_index
	 * @param provide_player
	 * @param center_card
	 * @param action
	 * @param type
	 *            //共杠还是明杠
	 * @param self
	 *            自己摸的
	 * @param d
	 *            双杠
	 * @return
	 */

	/**
	 * 切换到出牌处理器
	 * 
	 * @param seat_index
	 * @param card
	 * @return
	 */
	@Override
	public boolean exe_out_card(int seat_index, int card, int type) {

		return true;
	}

	@Override
	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int lou_operate) {

		return true;
	}

	/**
	 * 调度,加入牌堆
	 **/
	@Override
	public void runnable_add_discard(int seat_index, int card_count, int card_data[], boolean send_client) {
		if (seat_index == GameConstants.INVALID_SEAT) {
			return;
		}
		if (GRR == null)
			return;// 最后一张
		for (int i = 0; i < card_count; i++) {
			GRR._discard_count[seat_index]++;

			GRR._discard_cards[seat_index][GRR._discard_count[seat_index] - 1] = card_data[i];
		}
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	@Override
	public void runnable_remove_middle_cards(int seat_index) {

	}

	/**
	 * 
	 * @param seat_index
	 * @param type
	 */
	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
		// 删掉出来的那张牌
		operate_out_card(seat_index, 0, null, type, GameConstants.INVALID_SEAT);
	}

	public static void main(String[] args) {
		int value = 0x00000200;
		int value1 = 0x200;

		System.out.println(value);
		System.out.println(value1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#handler_request_trustee(int, boolean)
	 */
	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_IS_TRUSTEE);
		roomResponse.setOperatePlayer(get_seat_index);
		roomResponse.setIstrustee(isTrustee);
		boolean isTing = _playerStatus[get_seat_index]._hu_card_count <= 0 ? false : true;
		roomResponse.setIsTing(isTing);
		if (isTrustee && !isTing) {
			roomResponse.setIstrustee(false);
			send_response_to_player(get_seat_index, roomResponse);
			return false;
		}
		if (isTrustee && SysParamUtil.is_auto(GameConstants.GAME_ID_FLS_LX)) {
			istrustee[get_seat_index] = isTrustee;
		}
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		return false;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {

		return true;
	}

	@Override
	public boolean runnable_chuli_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	@Override
	public boolean runnable_dispatch_first_card_data(int _seat_index, int _type, boolean _tail) {

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cai.common.domain.Room#getTablePlayerNumber()
	 */
	@Override
	public int getTablePlayerNumber() {
		if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_PLAYER_TWO)) {
			return 2;
		} else if (this.has_rule(GameConstants.GAME_RULE_XP_SSZ_THREE_PEOPLE)) {
			return 3;
		} else if (this.has_rule(GameConstants.GAME_RULE_SSZ_JD_PLAYER_FOUR)) {
			return 4;
		}
		return 3;
	}

	@Override
	public boolean exe_finish(int reason) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear_score_in_gold_room() {
		ICenterRMIServer centerRMIServer = SpringService.getBean(ICenterRMIServer.class);
		int score = 0;

		// 门槛判断
		SysParamModel sysParamModel = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(getGame_id()).get(5006);
		int beilv = sysParamModel.getVal2().intValue();// 倍率
		float[] scores = _player_result.getGame_score();
		// GameConstants.GAME_PLAYER
		for (int i = 0; i < scores.length; i++) {
			score = (int) (scores[i] * beilv);

			StringBuilder buf = new StringBuilder();
			buf.append("牌局消耗:" + scores[i]).append("game_id:" + getGame_id());
			AddMoneyResultModel addGoldResultModel = centerRMIServer.addAccountMoney(this.get_players()[i].getAccount_id(), score, false,
					buf.toString(), EMoneyOperateType.ROOM_COST);
			if (addGoldResultModel.isSuccess() == false) {
				// 扣费失败
				logger.error("玩家:" + this.get_players()[i].getAccount_id() + ",扣费:" + score + "失败");
			}
			// 结算后 清0
			scores[i] = 0;
		}
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		// this.send_response_to_room(roomResponse);
		return true;
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	@Override
	public boolean open_card_timer() {
		return false;
	}

	@Override
	public boolean robot_banker_timer() {
		return false;
	}

	@Override
	public boolean ready_timer() {
		return false;
	}

	@Override
	public boolean add_jetton_timer() {
		return false;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
