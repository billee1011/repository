/**
 * 
 */
package com.cai.game.paijiu.pj_deh_mg;

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
import com.cai.common.util.SpringService;
import com.cai.core.SystemConfig;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.AddDiscardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.paijiu.PJTable;
import com.cai.game.paijiu.PJType;
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
import protobuf.clazz.deh.DehRsp.Che_Pai_End;
import protobuf.clazz.deh.DehRsp.GameStartDEH;
import protobuf.clazz.deh.DehRsp.Make_BoBo_Result;
import protobuf.clazz.deh.DehRsp.Open_Card_Result;
import protobuf.clazz.deh.DehRsp.Opreate_Request_DEH;
import protobuf.clazz.deh.DehRsp.Opreate_Result;
import protobuf.clazz.deh.DehRsp.Opreate_open_card;
import protobuf.clazz.deh.DehRsp.PukeGameEndDeh;
import protobuf.clazz.deh.DehRsp.RefreshPlayerStatus;
import protobuf.clazz.deh.DehRsp.Refresh_Table_message;
import protobuf.clazz.deh.DehRsp.Score_Result;
import protobuf.clazz.deh.DehRsp.Send_card_Deh;
import protobuf.clazz.deh.DehRsp.Special_Remin;
import protobuf.clazz.deh.DehRsp.TableResponseDEH;
import protobuf.clazz.deh.DehRsp.User_Can_Opreate;
import protobuf.clazz.deh.DehRsp.Zi_Chan_Score;

///////////////////////////////////////////////////////////////////////////////////////////////
public class PJTable_DEH_MG_bak extends PJTable {
	public int _bobo_score[];
	public int _min_bobo_score;
	public int _xian_qian_cell;
	public boolean _is_bobo[];
	public boolean _is_bu_zi_chan[];
	public int _jetton_current;
	public int _first_jetton_count;
	public int _jetton_current_add_min;
	public int _end_index;
	public int _user_card_count[];
	public int _mang_chi_score;
	public int _mang_pi_score;
	public int _user_mang_pi_score[];
	public int _xiu_mang_cur_round;
	public int _xiu_mang_total_round;
	public boolean _is_mang_guo;
	public int _mang_guo_win_seat;

	public int _init_user_score[];
	public int _int_round_score[];
	public int _is_special_type[];// 是否选择特牌，-1：不是特牌 0：特牌进入等待玩家选择状态 1：已经确定

	public PJTable_DEH_MG_bak() {
		super(PJType.GAME_TYPE_PJ_DEH_JD);

	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;
		_cur_round = 0;
		_deh_game_staus = 0;
		is_game_start = 0;
		_min_bobo_score = 0;
		_xian_qian_cell = 0;
		_mang_chi_score = 0;
		_jetton_current_add_min = 0;

		_xiu_mang_total_round = 2;
		_xiu_mang_cur_round = 0;
		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des(), this.getTablePlayerNumber());
		// 玩家
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			get_players()[i] = null;
		}
		_game_status = GameConstants.GS_MJ_FREE;
		// 游戏变量
		_player_ready = new int[this.getTablePlayerNumber()];
		_offline_round = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
			_offline_round[i] = -1;
		}
		_is_jetton = new boolean[getTablePlayerNumber()];
		_is_pass = new boolean[getTablePlayerNumber()];
		_is_give_up = new boolean[getTablePlayerNumber()];
		_is_open_card = new boolean[getTablePlayerNumber()];
		_jetton_score = new int[getTablePlayerNumber()];
		_cur_round_jetton_score = new int[getTablePlayerNumber()];
		_win_num = new int[getTablePlayerNumber()];
		_lose_num = new int[getTablePlayerNumber()];
		_draw_num = new int[getTablePlayerNumber()];
		_zi_chan_score = new int[getTablePlayerNumber()];
		_bobo_score = new int[getTablePlayerNumber()];
		_is_bu_zi_chan = new boolean[getTablePlayerNumber()];
		_init_user_score = new int[getTablePlayerNumber()];
		_int_round_score = new int[getTablePlayerNumber()];
		_is_bobo = new boolean[getTablePlayerNumber()];
		_user_card_count = new int[getTablePlayerNumber()];
		_is_special_type = new int[getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()];
		_user_mang_pi_score = new int[getTablePlayerNumber()];
		this._logic._game_type_index = _game_type_index;
		this._logic._game_rule_index = _game_rule_index;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_lose_num[i] = 0;
			_win_num[i] = 0;
			_draw_num[i] = 0;
			if (this.has_rule(GameConstants.GAME_RULE_DEH_INIT_ZI_CHAN_THREE)) {
				_zi_chan_score[i] = 1000;
				_init_user_score[i] = 1000;
			} else if (this.has_rule(GameConstants.GAME_RULE_DEH_INIT_ZI_CHAN_TWO)) {
				_zi_chan_score[i] = 500;
				_init_user_score[i] = 500;
			} else {
				_zi_chan_score[i] = 300;
				_init_user_score[i] = 300;
			}

			_bobo_score[i] = 0;
			_is_bobo[i] = false;
			_is_bu_zi_chan[i] = false;
			_is_give_up[i] = false;
			_is_open_card[i] = false;
			_user_card_count[i] = 0;
			_user_mang_pi_score[i] = 0;
			_cur_round_jetton_score[i] = 0;
		}

		if (has_rule(GameConstants.GAME_RULE_DEH_MANG_PI_TWO)) {
			_mang_pi_score = 2;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_MANG_PI_THREE)) {
			_mang_pi_score = 3;
		} else {
			_mang_pi_score = 5;
		}
		if (has_rule(GameConstants.GAME_RULE_DEH_MIN_BOBO_50)) {
			_min_bobo_score = 50;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_BOBO_100)) {
			_min_bobo_score = 100;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_BOBO_150)) {
			_min_bobo_score = 150;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_BOBO_ALL)) {
			_min_bobo_score = _zi_chan_score[0];
		} else {
			_min_bobo_score = 10;
		}

		if (has_rule(GameConstants.GAME_RULE_DEH_MIN_HAN_5)) {
			_jetton_current_add_min = 5;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_HAN_10)) {
			_jetton_current_add_min = 10;
		} else {
			_jetton_current_add_min = 1;
		}
		if (has_rule(GameConstants.GAME_RULE_DEH_XI_QIAN_5)) {
			_xian_qian_cell = 5;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_XI_QIAN_10)) {
			_xian_qian_cell = 10;
		}

		_is_mang_guo = false;
		_mang_guo_win_seat = GameConstants.INVALID_SEAT;

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
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 && this.get_players()[i] != null) {
				if (_offline_round[i] == -1) {
					_offline_round[i] = this._cur_round;
				}
			}
		}
		reset_init_data();

		//
		if (_repertory_card == null) {
			_repertory_card = new int[GameConstants.CARD_COUNT_PJ];
			shuffle(_repertory_card, GameConstants.CARD_DATA_PJ);
		} else {
			shuffle(_repertory_card, GameConstants.CARD_DATA_PJ);
		}

		Arrays.fill(_is_jetton, false);
		Arrays.fill(_is_pass, false);
		Arrays.fill(_jetton_score, 0);

		// DEBUG_CARDS_MODE = true;
		// BACK_DEBUG_CARDS_MODE = true;
		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		int player_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1) {
				player_count++;
			}
		}
		if (has_rule(GameConstants.GAME_RULE_DEH_MIN_HAN_5)) {
			_jetton_current_add_min = 5;
		} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_HAN_10)) {
			_jetton_current_add_min = 10;
		} else {
			_jetton_current_add_min = 1;
		}
		game_cell = 1;
		progress_banker_select();
		GRR._banker_player = _cur_banker;
		_pre_opreate_type = 0;
		_jetton_current = 0;
		_end_index = 0;
		_total_jetton_score = 0;
		_cur_round_score = 0;
		_first_jetton_count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_is_bobo[i] = false;
			_is_open_card[i] = false;
			_is_give_up[i] = false;
			_bobo_score[i] = 0;
			_user_card_count[i] = 0;
			_is_special_type[i] = -1;
			_cur_round_jetton_score[i] = 0;
			_int_round_score[i] = (int) this._player_result.game_score[i] + this._zi_chan_score[i];

		}

		return game_start_dehjd();
	}

	private void progress_banker_select() {
		if (this._cur_round == 1) {
			int next_player = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[next_player] == 0) {
					next_player = (next_player + 1) % this.getTablePlayerNumber();
				} else {
					break;
				}
			}
			_cur_banker = next_player;
		} else {
			int next_player = (_cur_banker + 1) % this.getTablePlayerNumber();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[next_player] == 0) {
					next_player = (next_player + 1) % this.getTablePlayerNumber();
				} else {
					break;
				}
			}
			_cur_banker = next_player;

		}

	}

	public boolean game_start_dehjd() {
		this.kill_timer();
		_current_player = _cur_banker;
		_game_status = GameConstants.GS_MJ_PLAY;
		_deh_game_staus = GAME_BOBO;
		_cur_round_banker = _current_player;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			refresh_player_status(i);
		}

		int user_mang_pi = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || i == _mang_guo_win_seat) {
				continue;
			}
			if (_user_mang_pi_score[i] != 0) {
				user_mang_pi = _user_mang_pi_score[i];
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || i == _mang_guo_win_seat) {
				continue;
			}
			if (_user_mang_pi_score[i] < user_mang_pi) {
				this.Score_Result(i, user_mang_pi, 6);
				if (this._player_result.game_score[i] == 0) {
					this._zi_chan_score[i] -= user_mang_pi;
				} else {
					this._player_result.game_score[i] -= user_mang_pi;
				}
				_user_mang_pi_score[i] = user_mang_pi;
				this._mang_chi_score += user_mang_pi;

			}
		}
		this.refresh_table_message();

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_common_status(roomResponse);
			this.load_room_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_DEH_GAME_START);
			roomResponse.setGameStatus(this._game_status);
			GameStartDEH.Builder game_start_deh = GameStartDEH.newBuilder();
			load_player_info_data_jetton_start(game_start_deh);
			game_start_deh.setRoomInfo(getRoomInfo());
			game_start_deh.setMaxBoboScore((int) (_zi_chan_score[i] + _player_result.game_score[i]));
			game_start_deh.setDisplayTime(30);

			if ((int) this._player_result.game_score[i] == 0) {
				if (this._player_result.game_score[i] + _zi_chan_score[i] >= _min_bobo_score) {
					game_start_deh.setMinBoboScore(_min_bobo_score);
				} else {
					game_start_deh.setMinBoboScore((int) this._player_result.game_score[i] + _zi_chan_score[i]);
				}

			} else {
				if (this.has_rule(GameConstants.GAME_RULE_DEH_HALF_BOBO_BU)) {
					if (_player_result.game_score[i] < _min_bobo_score / 2) {
						if (this._player_result.game_score[i] + _zi_chan_score[i] >= _min_bobo_score) {
							game_start_deh.setMinBoboScore(_min_bobo_score);
						} else {
							game_start_deh.setMinBoboScore((int) this._player_result.game_score[i] + _zi_chan_score[i]);
						}
					} else {
						game_start_deh.setMinBoboScore((int) _player_result.game_score[i]);
					}
				} else {
					if (_player_result.game_score[i] < 10) {
						game_start_deh.setMinBoboScore(10);
					} else {
						game_start_deh.setMinBoboScore((int) _player_result.game_score[i]);
					}
				}

			}

			roomResponse.setCommResponse(PBUtil.toByteString(game_start_deh));
			this.send_response_to_player(i, roomResponse);
		}

		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_DEH_GAME_START);
		roomResponse.setGameStatus(this._game_status);
		GameStartDEH.Builder game_end_deh = GameStartDEH.newBuilder();
		load_player_info_data_jetton_start(game_end_deh);
		game_end_deh.setRoomInfo(getRoomInfo());
		game_end_deh.setDisplayTime(30);
		roomResponse.setCommResponse(PBUtil.toByteString(game_end_deh));
		GRR.add_room_response(roomResponse);

		refresh_bobo_score(GameConstants.INVALID_SEAT, GameConstants.INVALID_SEAT);
		refresh_game_status(true);
		return true;
	}

	// 发牌
	public boolean Send_card(int first_index, int end_index, boolean is_record) {
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		// 刷新手牌
		for (int player_index = 0; player_index < this.getTablePlayerNumber(); player_index++) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DEH_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			Send_card_Deh.Builder send_card = Send_card_Deh.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();
				if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
					if (player_index == i) {
						if (this._player_ready[i] == 1) {
							for (int j = first_index; j < end_index; j++) {
								cards_card.addItem(GRR._cards_data[i][j]);
							}
						} else {
							for (int j = first_index; j < end_index; j++) {
								cards_card.addItem(GameConstants.INVALID_CARD);
							}
						}

					} else {
						for (int j = first_index; j < end_index; j++) {
							if (j < 2) {
								cards_card.addItem(GameConstants.BLACK_CARD);
							} else {
								cards_card.addItem(GRR._cards_data[i][j]);
							}

						}
					}
				} else {
					for (int j = first_index; j < end_index; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				if (player_index == i) {
					if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
						for (int j = 0; j < end_index; j++) {
							hand_cards_card.addItem(GRR._cards_data[i][j]);
						}
					} else {
						if (this._player_ready[i] == 0) {
							for (int j = 0; j < end_index; j++) {
								hand_cards_card.addItem(GameConstants.INVALID_CARD);
							}
						} else if (_is_give_up[i]) {
							for (int j = 0; j < _user_card_count[i]; j++) {
								hand_cards_card.addItem(GRR._cards_data[i][j]);
							}
							for (int j = _user_card_count[i]; j < end_index; j++) {
								hand_cards_card.addItem(GameConstants.INVALID_CARD);
							}
						}

					}

				} else {
					if (this._player_ready[i] == 1) {
						if (!this._is_give_up[i]) {
							for (int j = 0; j < end_index; j++) {
								if (j < 2) {
									hand_cards_card.addItem(GameConstants.BLACK_CARD);
								} else {
									hand_cards_card.addItem(GRR._cards_data[i][j]);
								}
							}
						} else {
							for (int j = 0; j < _user_card_count[i]; j++) {
								if (j < 2) {
									hand_cards_card.addItem(GameConstants.BLACK_CARD);
								} else {
									hand_cards_card.addItem(GRR._cards_data[i][j]);
								}
							}
							for (int j = _user_card_count[i]; j < end_index; j++) {
								hand_cards_card.addItem(GameConstants.INVALID_CARD);
							}
						}

					} else {
						for (int j = 0; j < end_index; j++) {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}
				}
				if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
					_user_card_count[i] = end_index;
				}

				send_card.addHandCardsData(hand_cards_card);
				send_card.addHandCardCount(end_index);
				send_card.addCardCount(end_index);
				send_card.addCardsData(cards_card);
			}
			send_card.setCurrentPlayer(this._current_player);
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			send_response_to_player(player_index, roomResponse);
		}

		if (is_record) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DEH_SEND_CARD);
			roomResponse.setGameStatus(this._game_status);
			Send_card_Deh.Builder send_card = Send_card_Deh.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();
				if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
					for (int j = first_index; j < end_index; j++) {
						cards_card.addItem(GRR._cards_data[i][j]);
					}
				} else {
					for (int j = first_index; j < end_index; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				if (this._player_ready[i] == 1) {
					if (!this._is_give_up[i]) {
						for (int j = 0; j < end_index; j++) {
							hand_cards_card.addItem(GRR._cards_data[i][j]);
						}
					}
				} else {
					hand_cards_card.addItem(GameConstants.INVALID_CARD);
				}
				send_card.addHandCardsData(hand_cards_card);
				send_card.addHandCardCount(end_index);
				send_card.addCardCount(end_index);
				send_card.addCardsData(cards_card);
			}
			send_card.setCurrentPlayer(this._current_player);
			roomResponse.setCommResponse(PBUtil.toByteString(send_card));
			GRR.add_room_response(roomResponse);
		}

		return true;
	}

	@Override
	public int get_hand_card_count_max() {
		return GameConstants.DEH_MAX_COUNT;
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

	private void test_cards() {

		// int cards[] = new int[] { 0x03, 0x23, 0x33, 0x34, 0x35, 0x06, 0x17,
		// 0x27, 0x08, 0x28, 0x1a, 0x3a, 0x1c, 0x3C, 0x2d,
		// 0x13, 0x04, 0x14, 0x24, 0x15, 0x16, 0x26, 0x36, 0x37, 0x38, 0x19,
		// 0x39, 0x2a, 0x0B, 0x3b,
		// 0x05, 0x25, 0x07, 0x18, 0x09, 0x29, 0x0a, 0x1b, 0x2b, 0x0c, 0x2c,
		// 0x1d, 0x3d, 0x31, 0x32};
		int cards[] = new int[] { 0x2C, 0x18, 0x02, 0x28, 0x2C, 0x18, 0x02, 0x28, 0x08, 0x39, 0x17, 0x19, 54, 26, 27, 25, 6, 52, 35, 57, 12, 20, 58,
				2, 21, 56, 24, 4, 22, 39, 53, 7 };
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int index = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < get_hand_card_count_max(); j++) {
				GRR._cards_data[i][j] = cards[index++];
			}
		}

		_operate_time = 60;
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 4) {
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
		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {

				for (int j = 0; j < this.get_hand_card_count_max(); j++) {
					if (k < count) {
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

		ret = this.handler_game_finish_dehjd(seat_index, reason);
		return ret;
	}

	public boolean handler_game_finish_dehjd(int seat_index, int reason) {
		int real_reason = reason;

		if (_game_status == GameConstants.GS_MJ_WAIT) {
			real_reason = GameConstants.Game_End_RELEASE_NO_BEGIN;
		}
		_game_status = GameConstants.GS_MJ_WAIT;
		_deh_game_staus = GAME_YIN_CANG;
		this.refresh_game_status(true);
		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}

		int end_score[] = new int[this.getTablePlayerNumber()];
		int mang_pi[] = new int[this.getTablePlayerNumber()];
		int win_sort[] = new int[this.getTablePlayerNumber()];
		int end_score_except_mangpi[] = new int[this.getTablePlayerNumber()];
		int xian_qian_score[] = new int[getTablePlayerNumber()];
		if (reason == GameConstants.Game_End_NORMAL) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] = 0;
				win_sort[i] = (this._cur_banker + i) % this.getTablePlayerNumber();
				mang_pi[i] = 0;
				end_score_except_mangpi[i] = 0;
			}
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				end_score[i] = 0;
				win_sort[i] = GameConstants.INVALID_SEAT;
				mang_pi[i] = 0;
				end_score_except_mangpi[i] = 0;
			}
		}

		if (GRR != null && reason == GameConstants.Game_End_NORMAL) {
			// 计算分数
			if (seat_index == GameConstants.INVALID_SEAT) {
				cal_score_deh_jd(end_score, win_sort, mang_pi, end_score_except_mangpi, xian_qian_score);
			} else {

				mang_pi[seat_index] = _mang_chi_score - _user_mang_pi_score[seat_index];
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					win_sort[i] = GameConstants.INVALID_SEAT;
				}
				win_sort[0] = seat_index;
				int win_jetton_score = _jetton_score[seat_index];
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._is_give_up[i] || this._player_ready[i] == 0) {
						int score = _jetton_score[i];
						if (score > win_jetton_score) {
							if (win_jetton_score != 0) {
								score = win_jetton_score;
							}
						}
						mang_pi[i] -= _user_mang_pi_score[i];
						_jetton_score[seat_index] += score;
						end_score[i] -= score;
						end_score[seat_index] += score;
						end_score_except_mangpi[i] = end_score[i];
						end_score[i] -= _user_mang_pi_score[i];
						this._jetton_score[i] -= score;
						this._player_result.game_score[i] += _jetton_score[i];
					}
				}
				_jetton_score[seat_index] += _mang_chi_score;
				end_score_except_mangpi[seat_index] = end_score[seat_index];
				end_score[seat_index] += _mang_chi_score - _user_mang_pi_score[seat_index];
				this._player_result.game_score[seat_index] += _jetton_score[seat_index];
				_user_mang_pi_score[seat_index] = 0;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		PukeGameEndDeh.Builder game_end_deh = PukeGameEndDeh.newBuilder();
		load_player_info_data_game_end(game_end_deh);
		game_end_deh.setRoomInfo(getRoomInfo());
		game_end.setRoomInfo(getRoomInfo());

		if (reason == GameConstants.Game_End_NORMAL) {
		}
		game_end_deh.setGameRound(_game_round);
		game_end_deh.setCurRound(_cur_round);

		if (reason == GameConstants.Game_End_DRAW) {

			// 休芒
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this._player_result.game_score[i] += this._jetton_score[i];
			}
			if (_xiu_mang_cur_round <= _xiu_mang_total_round) {
				_xiu_mang_cur_round++;
			}
			if (_xiu_mang_cur_round > _xiu_mang_total_round) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this._player_result.game_score[i] += _user_mang_pi_score[i];
					if (_user_mang_pi_score[i] > 0) {
						game_end_deh.addWinMangChiIndex(i);
						game_end_deh.addWinMangChiScore(_user_mang_pi_score[i]);
					}
					_user_mang_pi_score[i] = 0;
				}
				_mang_chi_score = 0;
				_xiu_mang_cur_round = 0;
			} else {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0) {
						continue;
					}
					_mang_chi_score += _mang_pi_score * _xiu_mang_cur_round;
					this._player_result.game_score[i] -= _mang_pi_score * _xiu_mang_cur_round;
					this.Score_Result(i, _mang_pi_score, 6);
					_user_mang_pi_score[i] += _mang_pi_score * _xiu_mang_cur_round;
				}
			}

		} else if (win_sort[0] != GameConstants.INVALID_SEAT) {
			game_end_deh.addWinMangChiIndex(win_sort[0]);
			game_end_deh.addWinMangChiScore(mang_pi[win_sort[0]]);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (end_score[i] > 0) {
					_win_num[i]++;
				} else if (end_score[i] < 0) {
					_lose_num[i]++;
				} else {
					_draw_num[i]++;
				}
				_end_score[i] += end_score[i];
				_user_mang_pi_score[i] = 0;
				_mang_chi_score = 0;
			}
			_xiu_mang_cur_round = 0;
		} else {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (end_score[i] > 0) {
					_win_num[i]++;
				} else if (end_score[i] < 0) {
					_lose_num[i]++;
				} else {
					_draw_num[i]++;
				}
				_end_score[i] += end_score[i];
				_user_mang_pi_score[i] = 0;
				_mang_chi_score = 0;
			}
			_xiu_mang_cur_round = 0;
		}

		if (GRR != null) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder cards_type = Int32ArrayResponse.newBuilder();

				if (this._player_ready[i] == 1) {
					if (!this._is_open_card[i]) {
						for (int j = 0; j < _user_card_count[i]; j++) {
							if (j < 2) {
								cards_card.addItem(GameConstants.BLACK_CARD);
							} else {
								cards_card.addItem(GRR._cards_data[i][j]);
							}

						}
					} else {
						for (int j = 0; j < _user_card_count[i]; j++) {
							cards_card.addItem(GRR._cards_data[i][j]);
						}
					}

				} else {
					for (int j = 0; j < _user_card_count[i]; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}

				game_end_deh.addCardsData(cards_card);

				if (this._is_open_card[i] && reason == GameConstants.Game_End_NORMAL) {
					int card_one[] = new int[this.get_hand_card_count_max()];
					int card_two[] = new int[this.get_hand_card_count_max()];
					int type_one = 0;
					int type_two = 0;
					int win_num = 0;
					for (int card_index = 0; card_index < 2; card_index++) {
						card_one[card_index] = this.GRR._cards_data[i][card_index];
					}
					for (int card_index = 0; card_index < 2; card_index++) {
						card_two[card_index] = this.GRR._cards_data[i][card_index + 2];
					}
					if (this._player_ready[i] == 1 && seat_index == GameConstants.INVALID_SEAT) {
						this._logic.SortCardList(card_one, 2);
						this._logic.SortCardList(card_two, 2);
						type_one = this._logic.GetCardType(card_one, 2);
						type_two = this._logic.GetCardType(card_two, 2);
						cards_type.addItem(type_one);
						cards_type.addItem(type_two);
					}
				}

				game_end_deh.addCardType(cards_type);
			}
			game_end.setStartTime(GRR._start_time);
			game_end.setGameTypeIndex(GRR._game_type_index);
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			game_end_deh.addEndScore(end_score[i]);
			game_end_deh.addMangPiScore(mang_pi[i]);
			game_end_deh.addEndScoreExtraMangPi(end_score_except_mangpi[i]);
			game_end_deh.addXianQianScore(xian_qian_score[i]);
			game_end.addGameScore(end_score[i]);
		}
		if (reason == GameConstants.Game_End_NORMAL) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (win_sort[i] != GameConstants.INVALID_SEAT && end_score[win_sort[i]] > 0) {
					game_end_deh.addWinSort(win_sort[i]);
				}
				if (win_sort[i] != GameConstants.INVALID_SEAT && (end_score[win_sort[i]] == 0 || end_score[win_sort[i]] < 0)
						&& this._jetton_score[win_sort[i]] > 0) {
					game_end_deh.addBackScoreChair(win_sort[i]);
					game_end_deh.addBackScore(_jetton_score[win_sort[i]]);
				}
			}
		} else if (reason == GameConstants.Game_End_DRAW) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_jetton_score[i] > 0) {
					game_end_deh.addBackScoreChair(i);
					game_end_deh.addBackScore(_jetton_score[i]);
				}

			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (reason == GameConstants.Game_End_DRAW) {
				real_reason = GameConstants.Game_End_NORMAL;
			}
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end_deh.setStartTime(_game_start_time);
				game_end.setPlayerResult(this.process_player_result(reason));
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					game_end_deh.addAllEndScore(_end_score[i]);
					this._player_result.game_score[i] = _end_score[i];
				}
				real_reason = GameConstants.Game_End_ROUND_OVER;
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
			game_end.setPlayerResult(this.process_player_result(reason));
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end_deh.addAllEndScore(_end_score[i]);
				this._player_result.game_score[i] = _end_score[i];
			}
			game_end_deh.setStartTime(_game_start_time);

			real_reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
		}
		game_end_deh.setStartTime(_game_start_time);
		game_end_deh.setEndTime(System.currentTimeMillis() / 1000);
		game_end_deh.setReason(real_reason);
		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(game_end_deh));
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

		// 重制准备
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}
		for (int i = 0; i < count; i++) {
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(i, roomResponse2);
			}
		}
		this.refresh_table_message();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				continue;
			}
			refresh_player_status(i);
			deal_bu_score_query(i);
		}
		// 错误断言
		return false;
	}

	// 结算分数
	public void cal_score_deh_jd(int end_score[], int win_sort[], int mang_pi[], int end_score_except_mangpi[], int xian_qian_score[]) {
		int compare_sort[] = new int[getTablePlayerNumber()];
		int weidao_sort[] = new int[getTablePlayerNumber()];
		int win_times[] = new int[getTablePlayerNumber()];
		int jetton_score[] = new int[getTablePlayerNumber()];
		int finial_score[] = new int[getTablePlayerNumber()];

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			compare_sort[i] = (this._cur_banker + i) % this.getTablePlayerNumber();
			weidao_sort[i] = (this._cur_banker + i) % this.getTablePlayerNumber();
			jetton_score[i] = _jetton_score[i];
			finial_score[i] = 0;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = i + 1; j < getTablePlayerNumber(); j++) {
				if (_jetton_score[compare_sort[i]] > _jetton_score[compare_sort[j]]) {
					int temp = compare_sort[i];
					compare_sort[i] = compare_sort[j];
					compare_sort[j] = temp;
				}
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = i + 1; j < getTablePlayerNumber(); j++) {
				if (i == j) {
					continue;
				}
				if (this._player_ready[weidao_sort[i]] == 1 && this._player_ready[weidao_sort[j]] == 1 && !this._is_give_up[weidao_sort[i]]
						&& !this._is_give_up[weidao_sort[j]]) {
					int card_one_two[] = new int[this.get_hand_card_count_max()];
					int card_two_two[] = new int[this.get_hand_card_count_max()];
					int type_one_two = 0;
					int type_two_two = 0;
					for (int card_index = 0; card_index < 2; card_index++) {
						card_one_two[card_index] = this.GRR._cards_data[weidao_sort[i]][card_index + 2];
						card_two_two[card_index] = this.GRR._cards_data[weidao_sort[j]][card_index + 2];
					}
					this._logic.SortCardList(card_one_two, 2);
					this._logic.SortCardList(card_two_two, 2);

					int ncompare = _logic.compare_data(card_one_two, 2, card_two_two, 2);
					if (ncompare < 0) {
						int sort_temp = weidao_sort[i];
						weidao_sort[i] = weidao_sort[j];
						weidao_sort[j] = sort_temp;
					}

				} else if (_player_ready[weidao_sort[i]] == 0 || this._is_give_up[weidao_sort[i]]) {
					int sort_temp = weidao_sort[i];
					weidao_sort[i] = weidao_sort[j];
					weidao_sort[j] = sort_temp;
				}

			}
		}

		for (int index = 0; index < getTablePlayerNumber(); index++) {
			if (jetton_score[compare_sort[index]] != 0) {
				int socre_temp = jetton_score[compare_sort[index]];//
				int win_score[] = new int[getTablePlayerNumber()];// 当前奖池分数
				boolean is_win = false;
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (jetton_score[i] == 0) {
						continue;
					}
					jetton_score[i] -= socre_temp;
					win_score[i] = socre_temp;
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					if (win_score[weidao_sort[i]] == 0) {
						continue;
					}
					int win = 0;
					int lose = 0;
					int draw = 0;
					for (int j = 0; j < getTablePlayerNumber(); j++) {
						if (win_score[weidao_sort[j]] == 0 || j == i) {
							continue;
						}
						int card_one_one[] = new int[this.get_hand_card_count_max()];
						int card_one_two[] = new int[this.get_hand_card_count_max()];
						int card_two_one[] = new int[this.get_hand_card_count_max()];
						int card_two_two[] = new int[this.get_hand_card_count_max()];
						int win_num = 0;
						for (int card_index = 0; card_index < 2; card_index++) {
							card_one_one[card_index] = this.GRR._cards_data[weidao_sort[i]][card_index];
							card_two_one[card_index] = this.GRR._cards_data[weidao_sort[j]][card_index];
						}
						for (int card_index = 0; card_index < 2; card_index++) {
							card_one_two[card_index] = this.GRR._cards_data[weidao_sort[i]][card_index + 2];
							card_two_two[card_index] = this.GRR._cards_data[weidao_sort[j]][card_index + 2];
						}
						this._logic.SortCardList(card_one_one, 2);
						this._logic.SortCardList(card_two_one, 2);
						this._logic.SortCardList(card_one_two, 2);
						this._logic.SortCardList(card_two_two, 2);

						int ncompare = _logic.compare_data(card_one_one, 2, card_two_one, 2);
						if (ncompare > 0) {
							win_num++;
						} else if (ncompare < 0) {
							win_num--;
						} else {
							if (this.has_rule(GameConstants.GAME_RULE_DEH_DI_ZHUANG_FIRST)) {
								int i_distance = 0;
								int j_distance = 0;
								if (weidao_sort[i] >= _cur_banker) {
									i_distance = weidao_sort[i] - _cur_banker;
								} else {
									i_distance = this.getTablePlayerNumber() - _cur_banker + weidao_sort[i];
								}
								if (weidao_sort[j] >= _cur_banker) {
									j_distance = weidao_sort[j] - _cur_banker;
								} else {
									j_distance = this.getTablePlayerNumber() - _cur_banker + weidao_sort[j];
								}
								if (i_distance > j_distance) {
									win_num--;
								} else if (i_distance < j_distance) {
									win_num++;
								}
							}
						}

						ncompare = _logic.compare_data(card_one_two, 2, card_two_two, 2);
						if (ncompare > 0) {
							win_num++;
						} else if (ncompare < 0) {
							win_num--;
						} else {
							if (this.has_rule(GameConstants.GAME_RULE_DEH_DI_ZHUANG_FIRST)) {
								int i_distance = 0;
								int j_distance = 0;
								if (weidao_sort[i] >= _cur_banker) {
									i_distance = weidao_sort[i] - _cur_banker;
								} else {
									i_distance = this.getTablePlayerNumber() - _cur_banker + weidao_sort[i];
								}
								if (weidao_sort[j] >= _cur_banker) {
									j_distance = weidao_sort[j] - _cur_banker;
								} else {
									j_distance = this.getTablePlayerNumber() - _cur_banker + weidao_sort[j];
								}
								if (i_distance > j_distance) {
									win_num--;
								} else if (i_distance < j_distance) {
									win_num++;
								}
							}
						}
						if (!this._is_give_up[weidao_sort[i]]) {
							if (win_num > 0 || this._is_give_up[weidao_sort[j]]) {
								win_score[weidao_sort[i]] += win_score[weidao_sort[j]];
								win_score[weidao_sort[j]] = 0;
							}
						}
					}
				}
				for (int i = 0; i < getTablePlayerNumber(); i++) {
					finial_score[i] += win_score[i];
				}

			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			end_score[i] = finial_score[i] - _jetton_score[i];
		}

		if (!this.has_rule(GameConstants.GAME_RULE_DEH_DI_ZHUANG_FIRST)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				for (int j = i + 1; j < this.getTablePlayerNumber(); j++) {
					if (i == j) {
						continue;
					}
					int win_i = (this._cur_banker + i) % this.getTablePlayerNumber();
					int win_j = (this._cur_banker + j) % this.getTablePlayerNumber();

					int card_one_one[] = new int[this.get_hand_card_count_max()];
					int card_one_two[] = new int[this.get_hand_card_count_max()];
					int card_two_one[] = new int[this.get_hand_card_count_max()];
					int card_two_two[] = new int[this.get_hand_card_count_max()];
					for (int card_index = 0; card_index < 2; card_index++) {
						card_one_one[card_index] = this.GRR._cards_data[win_i][card_index];
						card_two_one[card_index] = this.GRR._cards_data[win_j][card_index];
					}
					for (int card_index = 0; card_index < 2; card_index++) {
						card_one_two[card_index] = this.GRR._cards_data[win_i][card_index + 2];
						card_two_two[card_index] = this.GRR._cards_data[win_j][card_index + 2];
					}
					this._logic.SortCardList(card_one_one, 2);
					this._logic.SortCardList(card_two_one, 2);
					this._logic.SortCardList(card_one_two, 2);
					this._logic.SortCardList(card_two_two, 2);
					int ncompare = _logic.compare_data(card_one_one, 2, card_two_one, 2);
					int nweicompare = _logic.compare_data(card_one_two, 2, card_two_two, 2);
					if (ncompare == 0 && nweicompare == 0) {
						if (end_score[win_i] > 0 || end_score[win_j] > 0) {
							int total_win_score = end_score[win_i] + end_score[win_j];
							finial_score[win_i] -= end_score[win_i];
							finial_score[win_j] -= end_score[win_j];
							end_score[win_i] = 0;
							end_score[win_j] = 0;
							if (_jetton_score[win_i] > _jetton_score[win_j]) {
								if (total_win_score / 2 > _jetton_score[win_j]) {
									finial_score[win_j] += _jetton_score[win_j];
									finial_score[win_i] += total_win_score - _jetton_score[win_j];
									end_score[win_j] += _jetton_score[win_j];
									end_score[win_i] += total_win_score - _jetton_score[win_j];
								} else {
									if (total_win_score % 2 == 0) {
										finial_score[win_j] += total_win_score / 2;
										finial_score[win_i] += total_win_score / 2;
										end_score[win_i] += total_win_score / 2;
										end_score[win_j] += total_win_score / 2;
									} else {
										finial_score[win_j] += total_win_score / 2;
										finial_score[win_i] += total_win_score / 2 + total_win_score % 2;
										end_score[win_j] += total_win_score / 2;
										end_score[win_i] += total_win_score / 2 + total_win_score % 2;
									}
								}
							} else {
								if (total_win_score / 2 > _jetton_score[win_i]) {
									finial_score[win_i] += _jetton_score[win_i];
									finial_score[win_j] += total_win_score - _jetton_score[win_j];
									end_score[win_i] += _jetton_score[win_i];
									end_score[win_j] += total_win_score - _jetton_score[win_i];
								} else {
									if (total_win_score % 2 == 0) {
										finial_score[win_i] += total_win_score / 2;
										finial_score[win_j] += total_win_score / 2;
										end_score[win_j] += total_win_score / 2;
										end_score[win_i] += total_win_score / 2;
									} else {
										finial_score[win_j] += total_win_score / 2;
										finial_score[win_i] += total_win_score / 2 + total_win_score % 2;
										end_score[win_j] += total_win_score / 2;
										end_score[win_i] += total_win_score / 2 + total_win_score % 2;
									}
								}
							}

						}
					}
				}

			}
		}

		// 赢分大小排序
		int is_win[] = new int[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = i + 1; j < this.getTablePlayerNumber(); j++) {
				if (end_score[win_sort[i]] < end_score[win_sort[j]] || this._player_ready[win_sort[i]] == 0 || this._is_give_up[win_sort[i]]) {
					int temp = win_sort[i];
					win_sort[i] = win_sort[j];
					win_sort[j] = temp;
				}
			}
			is_win[i] = -1;
		}

		// 芒皮
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[win_sort[i]] == 0 || this._is_give_up[win_sort[i]]) {
				continue;
			}
			for (int j = i + 1; j < this.getTablePlayerNumber(); j++) {
				if (_user_mang_pi_score[win_sort[j]] == 0) {
					continue;
				}
				if (this._player_ready[win_sort[j]] == 0 || this._is_give_up[win_sort[j]]) {
					continue;
				}

				int card_one_one[] = new int[this.get_hand_card_count_max()];
				int card_one_two[] = new int[this.get_hand_card_count_max()];
				int card_two_one[] = new int[this.get_hand_card_count_max()];
				int card_two_two[] = new int[this.get_hand_card_count_max()];
				int type_one_one = 0;
				int type_one_two = 0;
				int type_two_one = 0;
				int type_two_two = 0;
				int win_num = 0;
				for (int card_index = 0; card_index < 2; card_index++) {
					card_one_one[card_index] = this.GRR._cards_data[win_sort[i]][card_index];
					card_two_one[card_index] = this.GRR._cards_data[win_sort[j]][card_index];
				}
				for (int card_index = 0; card_index < 2; card_index++) {
					card_one_two[card_index] = this.GRR._cards_data[win_sort[i]][card_index + 2];
					card_two_two[card_index] = this.GRR._cards_data[win_sort[j]][card_index + 2];
				}
				this._logic.SortCardList(card_one_one, 2);
				this._logic.SortCardList(card_two_one, 2);
				this._logic.SortCardList(card_one_two, 2);
				this._logic.SortCardList(card_two_two, 2);
				type_one_one = this._logic.GetCardType(card_one_one, 2);
				type_one_two = this._logic.GetCardType(card_one_two, 2);
				type_two_one = this._logic.GetCardType(card_two_one, 2);
				type_two_two = this._logic.GetCardType(card_two_two, 2);

				if (_is_give_up[win_sort[i]] && !_is_give_up[win_sort[j]]) {
					win_num--;
				} else if (_is_give_up[win_sort[j]] && !_is_give_up[win_sort[i]]) {
					win_num++;
				} else if (!_is_give_up[win_sort[j]] && !_is_give_up[win_sort[i]]) {
					int ncompare = _logic.compare_data(card_one_two, 2, card_two_two, 2);
					if (ncompare > 0) {
						win_num++;
					} else if (ncompare < 0) {
						win_num--;
					} else {
						if (this.has_rule(GameConstants.GAME_RULE_DEH_DI_ZHUANG_FIRST)) {
							int i_distance = 0;
							int j_distance = 0;
							if (win_sort[i] >= _cur_banker) {
								i_distance = win_sort[i] - _cur_banker;
							} else {
								i_distance = this.getTablePlayerNumber() - _cur_banker + win_sort[i];
							}
							if (win_sort[j] >= _cur_banker) {
								j_distance = win_sort[j] - _cur_banker;
							} else {
								j_distance = this.getTablePlayerNumber() - _cur_banker + win_sort[j];
							}
							if (i_distance < j_distance) {
								win_num++;
							} else {
								win_num--;
							}
						}
					}
				}

				if (win_num < 0) {
					is_win[win_sort[i]] = 0;
					if (is_win[win_sort[j]] != 0) {
						is_win[win_sort[j]] = 1;
					}
				} else if (win_num > 0) {
					is_win[win_sort[j]] = 0;
					if (is_win[win_sort[i]] != 0) {
						is_win[win_sort[i]] = 1;
					}
				} else {
					if (is_win[win_sort[j]] != 0) {
						is_win[win_sort[j]] = 1;
					}

					if (is_win[win_sort[i]] != 0) {
						is_win[win_sort[i]] = 1;
					}
				}
			}
		}
		int win_count = 0;
		int lose_count = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (is_win[weidao_sort[i]] > 0) {
				win_count++;
			} else if (is_win[weidao_sort[i]] == 0) {
				lose_count++;
			}
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (is_win[weidao_sort[i]] > 0) {
				int win_score = 0;
				if (_mang_chi_score % win_count == 0) {
					win_score = _mang_chi_score / win_count;
				} else {
					win_score = _mang_chi_score / win_count + _mang_chi_score % win_count;
					_mang_chi_score -= _mang_chi_score % win_count;
				}
				finial_score[weidao_sort[i]] += win_score;
				mang_pi[weidao_sort[i]] = win_score - _user_mang_pi_score[weidao_sort[i]];
				end_score_except_mangpi[weidao_sort[i]] = end_score[weidao_sort[i]];
				end_score[weidao_sort[i]] += win_score - _user_mang_pi_score[weidao_sort[i]];
			} else {
				end_score_except_mangpi[weidao_sort[i]] = end_score[weidao_sort[i]];
				end_score[weidao_sort[i]] -= _user_mang_pi_score[weidao_sort[i]];
				mang_pi[weidao_sort[i]] -= _user_mang_pi_score[weidao_sort[i]];
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += finial_score[i];
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_jetton_score[i] = finial_score[i];
		}

		// 喜钱
		int xi_qian_count = 0;
		boolean is_xi_qian[] = new boolean[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || this._is_give_up[i]) {
				continue;
			}
			int card_one_one[] = new int[this.get_hand_card_count_max()];
			int card_one_two[] = new int[this.get_hand_card_count_max()];
			for (int card_index = 0; card_index < 2; card_index++) {
				card_one_one[card_index] = this.GRR._cards_data[i][card_index];
			}
			for (int card_index = 0; card_index < 2; card_index++) {
				card_one_two[card_index] = this.GRR._cards_data[i][card_index + 2];
			}
			if (this._logic.is_dui_zi(card_one_one, 2) && this._logic.is_dui_zi(card_one_two, 2)) {
				xi_qian_count++;
				is_xi_qian[i] = true;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || this._is_give_up[i]) {
				continue;
			}
			if (is_xi_qian[i]) {
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
					if (this._player_ready[j] == 0) {
						continue;
					}
					if (!is_xi_qian[j]) {
						if (this._xian_qian_cell * xi_qian_count > _player_result.game_score[j]) {
							xian_qian_score[i] += _player_result.game_score[j] / xi_qian_count;
							xian_qian_score[j] -= _player_result.game_score[j] / xi_qian_count;
						} else {
							xian_qian_score[i] += _xian_qian_cell;
							xian_qian_score[j] -= _xian_qian_cell;
						}
					}
				}
			}
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0) {
				continue;
			}
			_player_result.game_score[i] = _player_result.game_score[i] + xian_qian_score[i];
			end_score[i] += xian_qian_score[i];
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}
		if (is_cancel) {
			_player_ready[seat_index] = 0;
		}
		_offline_round[seat_index] = -1;
		if (this._player_result.game_score[seat_index] + this._zi_chan_score[seat_index] < 10) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);

			refresh_player_status(seat_index);

			this.send_error_notify(seat_index, 2, "分数不足");
		} else {
			if (!super.handler_player_ready(seat_index, is_cancel)) {
				return false;
			}
			refresh_player_status(seat_index);

			if (_game_status != GameConstants.GS_MJ_PLAY) {
				int cur_count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (_player_ready[i] == 1) {
						cur_count++;
					}
				}
				if (cur_count < 2) {
					return true;
				}

				if (cur_count >= 2 && _cur_game_timer != ID_TIMER_READY) {
					this.set_timer(ID_TIMER_READY, _ready_wait_time);
					refresh_colock(GameConstants.INVALID_SEAT);
				}
			}
		}
		return true;
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

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {
		super.handler_enter_room(player);

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

		if (_cur_game_timer == ID_TIMER_READY && _game_status != GameConstants.GS_MJ_PLAY) {
			refresh_colock(seat_index);
		}
		refresh_table_message();

		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			if (_deh_game_staus == GAME_YIN_CANG) {
				this.Refresh_User_Operate(seat_index);
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DEH_RECONNECT_DATA);

			TableResponseDEH.Builder tableResponse_deh = TableResponseDEH.newBuilder();
			load_player_info_data_reconnect(tableResponse_deh);
			tableResponse_deh.setRoomInfo(getRoomInfo());
			tableResponse_deh.setBankerPlayer(GRR._banker_player);
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				tableResponse_deh.addIsGiveUp(_is_give_up[i]);
				tableResponse_deh.addJettonScore(this._jetton_score[i]);
				tableResponse_deh.addIsBobo(this._is_bobo[i]);
				Int32ArrayResponse.Builder hand_cards_card = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder card_type = Int32ArrayResponse.newBuilder();
				if (_deh_game_staus == GAME_CHE_PAI_BEGIN) {
					if (seat_index == i) {
						if (_player_ready[i] == 0) {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						} else {
							for (int j = 0; j < _user_card_count[i]; j++) {
								hand_cards_card.addItem(this.GRR._cards_data[i][j]);
							}
						}
					} else {
						if (this._player_ready[i] == 1) {
							for (int j = 0; j < _user_card_count[i]; j++) {
								hand_cards_card.addItem(GameConstants.BLACK_CARD);
							}
						} else if (_player_ready[i] == 0) {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}
				} else if (_deh_game_staus == GAME_BI_WEI) {
					if (seat_index == i) {
						if (this._player_ready[i] == 1) {
							int card_one[] = new int[this.get_hand_card_count_max()];
							int card_two[] = new int[this.get_hand_card_count_max()];
							int type_one = 0;
							int type_two = 0;
							for (int card_index = 0; card_index < 2; card_index++) {
								card_one[card_index] = this.GRR._cards_data[seat_index][card_index];
							}
							for (int card_index = 0; card_index < 2; card_index++) {
								card_two[card_index] = this.GRR._cards_data[seat_index][card_index + 2];
							}
							this._logic.SortCardList(card_one, 2);
							this._logic.SortCardList(card_two, 2);
							type_one = this._logic.GetCardType(card_one, 2);
							type_two = this._logic.GetCardType(card_two, 2);
							card_type.addItem(type_one);
							card_type.addItem(type_two);
							for (int j = 0; j < _user_card_count[i]; j++) {
								hand_cards_card.addItem(this.GRR._cards_data[i][j]);
							}
						} else {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						}
					} else {
						if (this._player_ready[i] == 1) {
							int card_two[] = new int[this.get_hand_card_count_max()];
							int type_two = 0;
							for (int card_index = 0; card_index < 2; card_index++) {
								card_two[card_index] = this.GRR._cards_data[i][card_index + 2];
							}
							this._logic.SortCardList(card_two, 2);
							type_two = this._logic.GetCardType(card_two, 2);
							card_type.addItem(-1);
							card_type.addItem(type_two);
							for (int j = 0; j < _user_card_count[i]; j++) {
								if (j < 2) {
									hand_cards_card.addItem(GameConstants.BLACK_CARD);
								} else {
									hand_cards_card.addItem(this.GRR._cards_data[i][j]);
								}
							}
						} else {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}
				} else {
					if (seat_index == i) {
						if (_player_ready[i] == 0) {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						} else {
							for (int j = 0; j < _user_card_count[i]; j++) {
								hand_cards_card.addItem(this.GRR._cards_data[i][j]);
							}
						}
					} else {
						if (this._player_ready[i] == 1) {
							for (int j = 0; j < _user_card_count[i]; j++) {
								if (j < 2) {
									hand_cards_card.addItem(GameConstants.BLACK_CARD);
								} else {
									hand_cards_card.addItem(this.GRR._cards_data[i][j]);
								}
							}
						} else if (_player_ready[i] == 0) {
							hand_cards_card.addItem(GameConstants.INVALID_CARD);
						}
					}
				}

				if (_deh_game_staus == GAME_CHE_PAI_BEGIN && _is_open_card[seat_index] && i == seat_index) {
					int card_one[] = new int[this.get_hand_card_count_max()];
					int card_two[] = new int[this.get_hand_card_count_max()];
					int type_one = 0;
					int type_two = 0;
					for (int card_index = 0; card_index < 2; card_index++) {
						card_one[card_index] = this.GRR._cards_data[seat_index][card_index];
					}
					for (int card_index = 0; card_index < 2; card_index++) {
						card_two[card_index] = this.GRR._cards_data[seat_index][card_index + 2];
					}
					this._logic.SortCardList(card_one, 2);
					this._logic.SortCardList(card_two, 2);
					type_one = this._logic.GetCardType(card_one, 2);
					type_two = this._logic.GetCardType(card_two, 2);
					card_type.addItem(type_one);
					card_type.addItem(type_two);
				}
				tableResponse_deh.addCardType(card_type);
				tableResponse_deh.addIsOpenCard(this._is_open_card[i]);
				tableResponse_deh.addCardsData(hand_cards_card);
			}
			if (!this._is_bobo[seat_index]) {

				tableResponse_deh.setMaxBoboScore((int) (_zi_chan_score[seat_index] + _player_result.game_score[seat_index]));
				tableResponse_deh.setDisplayTime(30);

				if ((int) this._player_result.game_score[seat_index] == 0) {
					if (this._player_result.game_score[seat_index] + _zi_chan_score[seat_index] >= _min_bobo_score) {
						tableResponse_deh.setMinBoboScore(_min_bobo_score);
					} else {
						tableResponse_deh.setMinBoboScore((int) this._player_result.game_score[seat_index] + _zi_chan_score[seat_index]);
					}

				} else {
					tableResponse_deh.setMinBoboScore((int) _player_result.game_score[seat_index]);
				}
			}

			tableResponse_deh.setCurrentPlayer(this._current_player);
			tableResponse_deh.setDehGameStatus(this._deh_game_staus);
			roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_deh));
			send_response_to_player(seat_index, roomResponse);

			if (_deh_game_staus == GAME_BOBO) {
				refresh_bobo_score(GameConstants.INVALID_SEAT, seat_index);
			}
			if (_deh_game_staus == GAME_CHE_PAI_BEGIN && !_is_open_card[seat_index] && this._player_ready[seat_index] == 1) {
				che_pai_begin(seat_index);
			}
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

		if (seat_index != GameConstants.INVALID_SEAT) {
			deal_bu_score_query(seat_index);
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				refresh_player_status(i);
			}

		}
		this.refresh_game_status(false);
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

	public boolean deal_gen_jetton(int seat_index) {
		if (seat_index != this._current_player) {
			return true;
		}
		if (this._jetton_current == 0 || this._player_ready[seat_index] == 0 || _is_give_up[seat_index]) {
			return true;
		}
		if (_jetton_current - _cur_round_jetton_score[seat_index] > _player_result.game_score[seat_index]) {
			return true;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		this.kill_timer();
		_jetton_score[seat_index] += _jetton_current - _cur_round_jetton_score[seat_index];
		_total_jetton_score += _jetton_current - _cur_round_jetton_score[seat_index];
		_player_result.game_score[seat_index] -= _jetton_current - _cur_round_jetton_score[seat_index];
		_cur_round_jetton_score[seat_index] = _jetton_current;
		_is_jetton[seat_index] = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_is_pass[i] = false;
		}
		int next_player = (_current_player + 1) % this.getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[next_player] == 0 || _is_give_up[next_player] || this._player_result.game_score[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			} else {
				break;
			}
		}
		_current_player = next_player;

		Score_Result(seat_index, _jetton_current, 1);

		// 操作
		Operate_Result(seat_index, 5);

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
				continue;
			}
			if (_jetton_current <= this._cur_round_jetton_score[i]) {
				continue;
			}
			count++;
		}

		// 本轮还可操作人数
		if (count == 0) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_cur_round_jetton_score[i] = 0;
			}
			if (_end_index == 4) {
				_deh_game_staus = GAME_CHE_PAI_WAIT;
				refresh_game_status(true);
				set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
				this._current_player = GameConstants.INVALID_SEAT;
				Refresh_User_Operate(this._current_player);
				return true;
			} else {
				int player_count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 1) {
						player_count++;
					}
				}

				this._jetton_current = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					_is_jetton[i] = false;
					_is_pass[i] = false;
				}

				// 判断下一轮还有分数的人数
				count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || this._is_give_up[i]) {
						continue;
					}
					if (this._player_result.game_score[i] == 0) {
						continue;
					}
					count++;
				}
				// 下轮还可操作人数
				if (count <= 1) {
					// 仅剩一个玩家有分数直接开始扯牌
					_end_index = 4;

					this.Send_card(this._end_index, _end_index, true);
					_deh_game_staus = GAME_CHE_PAI_WAIT;
					refresh_game_status(true);
					set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
					this._current_player = GameConstants.INVALID_SEAT;
					Refresh_User_Operate(this._current_player);
					return true;
				} else {
					// 进入下一轮
					this.Send_card(this._end_index, _end_index + 1, true);
					if (_end_index >= 2) {
						_cur_round_banker = this._cur_banker;
						int index = this._cur_banker;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							index = (_cur_banker + i) % getTablePlayerNumber();
							if (this._player_ready[index] == 1 && !this._is_give_up[index]) {
								if (this._is_give_up[_cur_round_banker]) {
									_cur_round_banker = index;
								} else {
									if (_logic.GetCardLogicValue(GRR._cards_data[index][_end_index]) > _logic
											.GetCardLogicValue(GRR._cards_data[_cur_round_banker][_end_index])) {
										_cur_round_banker = index;
									}
								}
							}
						}
						if (this._player_result.game_score[_cur_round_banker] == 0) {
							for (int i = 0; i < getTablePlayerNumber(); i++) {
								index = (_cur_round_banker + i) % getTablePlayerNumber();
								if (this._is_give_up[index] || this._player_result.game_score[index] == 0 || this._player_ready[index] == 0) {
									continue;
								}
								_cur_round_banker = index;
								break;
							}
						}
					}
					this._current_player = _cur_round_banker;
					if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
						this._jetton_current_add_min = this._jetton_score[_current_player];
					} else {
						_jetton_current_add_min = 1;
					}
					_end_index += 1;
				}
			}
		}
		Refresh_User_Operate(this._current_player);
		set_timer(ID_TIMER_OPERATE, _operate_time);
		return true;
	}

	public boolean deal_give_up(int seat_index) {
		if (seat_index != this._current_player || _current_player == GameConstants.INVALID_SEAT) {
			return true;
		}
		if (this._player_ready[seat_index] == 0 || _is_give_up[seat_index]) {
			return true;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		// 庄家必管
		if (this._jetton_current == 0 && this._end_index == 2 && this.has_rule(GameConstants.GAME_RULE_DEH_BANKER_MUST_CALL)) {
			return true;
		}
		this.kill_timer();

		_is_give_up[seat_index] = true;
		int next_player = (_current_player + 1) % this.getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[next_player] == 0 || _is_give_up[next_player] || this._player_result.game_score[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			} else {
				break;
			}
		}
		_current_player = next_player;
		if (_is_special_type[seat_index] == 0) {
			this._player_result.game_score[seat_index] += _jetton_score[seat_index];
			_jetton_score[seat_index] = 0;
			_is_special_type[seat_index] = 1;
		} else {
			if (_jetton_score[seat_index] == 0) {
				_jetton_score[seat_index] += this.game_cell;
				this._player_result.game_score[seat_index] -= this.game_cell;
				this.Score_Result(seat_index, (int) this.game_cell, 1);
			}
		}
		// 操作
		if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
			int max_jetton_score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_jetton_score[i] > max_jetton_score) {
					max_jetton_score = _jetton_score[i];
				}
			}
			this._jetton_current_add_min = max_jetton_score * 2 - _jetton_score[_current_player] + _cur_round_jetton_score[_current_player];
		} else {
			_jetton_current_add_min = _jetton_current + 1;
		}

		int count = 0;
		boolean is_round = true;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || this._is_give_up[i]) {
				continue;
			}
			count++;
		}
		// 还没弃牌人数为1
		if (count <= 1) {
			int win_seat_index = GameConstants.INVALID_SEAT;
			this._current_player = GameConstants.INVALID_SEAT;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 0 || this._is_give_up[i]) {
					continue;
				}
				win_seat_index = i;
			}

			if (has_rule(GameConstants.GAME_RULE_DEH_PEI_SI_PI) && this.getPlayerCount() >= 3) {
				boolean is_pei_si_pi = true;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || !this._is_give_up[i]) {
						continue;
					}
					if (_jetton_score[i] > 0) {
						is_pei_si_pi = false;
						break;
					}
				}
				if (this._jetton_score[win_seat_index] == _int_round_score[win_seat_index] && is_pei_si_pi) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._player_ready[i] == 0 || !this._is_give_up[i]) {
							continue;
						}
						if (this._player_result.game_score[i] >= _jetton_score[win_seat_index]) {
							_jetton_score[i] = _jetton_score[win_seat_index];
						} else {
							_jetton_score[i] = (int) _player_result.game_score[i];
						}

					}
				}
			}
			if (this._end_index == 2) {
				_is_mang_guo = true;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || i == win_seat_index) {
						continue;
					}
					if (_jetton_score[i] > 1) {
						_is_mang_guo = false;
					}
				}
				_mang_guo_win_seat = win_seat_index;
			}

			Refresh_User_Operate(this._current_player);
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_seat_index, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			Operate_Result(seat_index, 1);
			return true;
		}

		count = 0;
		int cur_jetton_max = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (_cur_round_jetton_score[i] > cur_jetton_max) {
				cur_jetton_max = _cur_round_jetton_score[i];
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
				continue;
			}
			if (cur_jetton_max == this._cur_round_jetton_score[i] && _is_jetton[i]) {
				continue;
			}
			if (this._is_pass[i]) {
				continue;
			}
			count++;
		}
		// 本轮还可操作人数
		if (count == 0) {

			// 判断还有没休人数
			count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 0 || this._is_give_up[i]) {
					continue;
				}
				if (this._is_pass[i]) {
					continue;
				}
				count++;
			}
			if (count == 0) {
				_deh_game_staus = GAME_LIU_JU;
				refresh_game_status(true);
				this._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_DRAW), 3, TimeUnit.SECONDS);
				Operate_Result(seat_index, 1);
				return true;
			} else {
				count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || this._is_give_up[i]) {
						continue;
					}
					if (this._is_pass[i] || this._player_result.game_score[i] == 0) {
						continue;
					}
					count++;
				}
				if (count == 0) {

					Operate_Result(seat_index, 1);
					count = 0;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._player_ready[i] == 0 || this._is_give_up[i]) {
							continue;
						}
						if (this._is_pass[i]) {
							continue;
						}
						count++;
					}
					if (count == 1 && has_rule(GameConstants.GAME_RULE_DEH_PEI_SI_PI)) {
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (_is_pass[i]) {
								_is_give_up[i] = true;
							}
						}
						int win_seat_index = GameConstants.INVALID_SEAT;
						this._current_player = GameConstants.INVALID_SEAT;
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (this._player_ready[i] == 0 || this._is_give_up[i]) {
								continue;
							}
							if (_is_pass[i]) {
								continue;
							}
							win_seat_index = i;
						}
						Refresh_User_Operate(this._current_player);
						GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_seat_index, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
						return true;
					} else {
						if (count > 1 && has_rule(GameConstants.GAME_RULE_DEH_PEI_SI_PI)) {
							for (int i = 0; i < this.getTablePlayerNumber(); i++) {
								if (_is_pass[i]) {
									_is_give_up[i] = true;
								}
							}
							_end_index = 4;
							this.Send_card(this._end_index, _end_index, true);
							_deh_game_staus = GAME_CHE_PAI_WAIT;
							refresh_game_status(true);
							set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
							this._current_player = GameConstants.INVALID_SEAT;
							Refresh_User_Operate(this._current_player);
							return true;
						}
					}

				}
			}
			if (_end_index == 4) {
				_deh_game_staus = GAME_CHE_PAI_WAIT;
				refresh_game_status(true);
				Operate_Result(seat_index, 1);
				set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
				this._current_player = GameConstants.INVALID_SEAT;
				Refresh_User_Operate(this._current_player);
				return true;
			} else {
				this._jetton_current = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					_is_jetton[i] = false;
					_is_pass[i] = false;
				}
				// 判断下一轮还可操作人数
				count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || this._is_give_up[i]) {
						continue;
					}
					if (this._player_result.game_score[i] == 0) {
						continue;
					}
					count++;
				}
				// 下轮还可操作人数
				if (count <= 1) {
					if (deal_pei_si_pi(seat_index)) {
						return true;
					}
					_end_index = 4;
					Operate_Result(seat_index, 1);
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_is_pass[i]) {
							_is_give_up[i] = true;
						}
					}
					this.Send_card(this._end_index, _end_index, true);
					_deh_game_staus = GAME_CHE_PAI_WAIT;
					refresh_game_status(true);
					set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
					this._current_player = GameConstants.INVALID_SEAT;
					Refresh_User_Operate(this._current_player);
					return true;
				} else {
					this.Send_card(this._end_index, _end_index + 1, true);
					if (_end_index >= 2) {
						_cur_round_banker = this._cur_banker;
						int index = this._cur_banker;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							index = (_cur_banker + i) % getTablePlayerNumber();
							if (this._player_ready[index] == 1 && !this._is_give_up[index]) {
								if (this._is_give_up[_cur_round_banker]) {
									_cur_round_banker = index;
								} else {
									if (_logic.GetCardLogicValue(GRR._cards_data[index][_end_index]) > _logic
											.GetCardLogicValue(GRR._cards_data[_cur_round_banker][_end_index])) {
										_cur_round_banker = index;
									}
								}
							}
						}
						if (this._player_result.game_score[_cur_round_banker] == 0) {
							for (int i = 0; i < getTablePlayerNumber(); i++) {
								index = (_cur_round_banker + i) % getTablePlayerNumber();
								if (this._is_give_up[index] || this._player_result.game_score[index] == 0 || this._player_ready[index] == 0) {
									continue;
								}
								_cur_round_banker = index;
								break;
							}
						}
					}
					this._current_player = _cur_round_banker;
					if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
						this._jetton_current_add_min = this._jetton_score[_current_player];
					} else {
						_jetton_current_add_min = 1;
					}
					_end_index += 1;
				}
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_cur_round_jetton_score[i] = 0;
			}
		} else {
			// 判断还有没休人数
			count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 0 || this._is_give_up[i]) {
					continue;
				}
				if (this._is_pass[i]) {
					continue;
				}
				count++;
			}
			if (count == 0) {
				_deh_game_staus = GAME_LIU_JU;
				refresh_game_status(true);
				this._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_DRAW), 3, TimeUnit.SECONDS);
				Operate_Result(seat_index, 1);
				return true;
			}
		}

		Operate_Result(seat_index, 1);
		Refresh_User_Operate(this._current_player);
		set_timer(ID_TIMER_OPERATE, _operate_time);
		return true;
	}

	public boolean deal_pass(int seat_index) {
		if (seat_index != this._current_player) {
			return true;
		}
		if (_is_jetton[seat_index] || this._player_ready[seat_index] == 0 || _is_give_up[seat_index] || _is_pass[seat_index]) {
			return true;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._cur_round_jetton_score[i] != 0) {
				return true;
			}
		}
		// 庄家必管
		if (this._jetton_current == 0 && this._end_index == 2 && this.has_rule(GameConstants.GAME_RULE_DEH_BANKER_MUST_CALL)) {
			return true;
		}
		this.kill_timer();
		_is_pass[seat_index] = true;
		int next_player = (_current_player + 1) % this.getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[next_player] == 0 || _is_give_up[next_player] || this._player_result.game_score[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			} else {
				break;
			}
		}
		_current_player = next_player;
		// 操作
		Operate_Result(seat_index, 2);

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
				continue;
			}
			if (_is_pass[i]) {
				continue;
			}
			count++;
		}
		// 都休了
		if (count == 0) {
			// 没弃牌数量
			count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._player_ready[i] == 0 || _is_give_up[i]) {
					continue;
				}
				if (_is_pass[i]) {
					continue;
				}
				count++;
			}
			if (deal_pei_si_pi(seat_index)) {
				return true;
			}
			if (count > 0) {

				if (count == 1 && has_rule(GameConstants.GAME_RULE_DEH_PEI_SI_PI)) {
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (_is_pass[i]) {
							_is_give_up[i] = true;
						}
					}
					int win_seat_index = GameConstants.INVALID_SEAT;
					this._current_player = GameConstants.INVALID_SEAT;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._player_ready[i] == 0 || this._is_give_up[i]) {
							continue;
						}
						if (_is_pass[i]) {
							continue;
						}
						win_seat_index = i;
					}
					Refresh_User_Operate(this._current_player);
					GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_seat_index, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
				} else {
					count = 0;
					for (int i = 0; i < this.getTablePlayerNumber(); i++) {
						if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
							continue;
						}
						count++;
					}
					if (count == 1 || _end_index == 4) {
						_end_index = 4;

						this.Send_card(this._end_index, _end_index, true);
						_deh_game_staus = GAME_CHE_PAI_WAIT;
						refresh_game_status(true);
						set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
						this._current_player = GameConstants.INVALID_SEAT;
						Refresh_User_Operate(this._current_player);
						return true;
					} else {
						for (int i = 0; i < this.getTablePlayerNumber(); i++) {
							if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
								continue;
							}
							_is_pass[i] = false;
						}
						// 进入下一轮
						this.Send_card(this._end_index, _end_index + 1, true);
						if (_end_index >= 2) {
							_cur_round_banker = this._cur_banker;
							int index = this._cur_banker;
							for (int i = 0; i < getTablePlayerNumber(); i++) {
								index = (_cur_banker + i) % getTablePlayerNumber();
								if (this._player_ready[index] == 1 && !this._is_give_up[index]) {
									if (this._is_give_up[_cur_round_banker]) {
										_cur_round_banker = index;
									} else {
										if (_logic.GetCardLogicValue(GRR._cards_data[index][_end_index]) > _logic
												.GetCardLogicValue(GRR._cards_data[_cur_round_banker][_end_index])) {
											_cur_round_banker = index;
										}
									}
								}
							}
							if (this._player_result.game_score[_cur_round_banker] == 0) {
								for (int i = 0; i < getTablePlayerNumber(); i++) {
									index = (_cur_round_banker + i) % getTablePlayerNumber();
									if (this._is_give_up[index] || this._player_result.game_score[index] == 0 || this._player_ready[index] == 0) {
										continue;
									}
									_cur_round_banker = index;
									break;
								}
							}
						}
						this._current_player = _cur_round_banker;
						if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
							this._jetton_current_add_min = this._jetton_score[_current_player];
						} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_HAN_5)) {
							if (_jetton_current_add_min < 5) {
								_jetton_current_add_min = 5;
							}

						} else if (has_rule(GameConstants.GAME_RULE_DEH_MIN_HAN_10)) {
							if (_jetton_current_add_min < 10) {
								_jetton_current_add_min = 10;
							}
						} else {
							_jetton_current_add_min = 1;
						}
						_end_index += 1;
					}
				}

			} else {
				_deh_game_staus = GAME_LIU_JU;
				refresh_game_status(true);
				this._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_DRAW), 3, TimeUnit.SECONDS);
			}

		}
		Refresh_User_Operate(this._current_player);
		set_timer(ID_TIMER_OPERATE, _operate_time);
		return true;
	}

	public boolean deal_shuo_hand(int seat_index) {
		if (seat_index != this._current_player) {
			return true;
		}
		if (_is_give_up[seat_index] || this._player_ready[seat_index] == 0 || this._player_result.game_score[seat_index] <= 0) {
			return true;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		this.kill_timer();
		int jetton_score = (int) _player_result.game_score[seat_index];
		_jetton_score[seat_index] += jetton_score;
		_total_jetton_score += jetton_score;

		if (jetton_score + _cur_round_jetton_score[seat_index] > _jetton_current) {
			_jetton_current = jetton_score + _cur_round_jetton_score[seat_index];
		}
		_cur_round_jetton_score[seat_index] = jetton_score + _cur_round_jetton_score[seat_index];
		_player_result.game_score[seat_index] = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_is_pass[i] = false;
		}
		int next_player = (_current_player + 1) % this.getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[next_player] == 0 || _is_give_up[next_player] || this._player_result.game_score[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			} else {
				break;
			}
		}
		_current_player = next_player;

		Score_Result(seat_index, jetton_score, 1);

		// 操作
		Operate_Result(seat_index, 3);
		if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
			int max_jetton_score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_jetton_score[i] > max_jetton_score) {
					max_jetton_score = _jetton_score[i];
				}
			}
			this._jetton_current_add_min = max_jetton_score * 2 - _jetton_score[_current_player] + _cur_round_jetton_score[_current_player];
		} else {
			_jetton_current_add_min = _jetton_current + 1;
		}

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
				continue;
			}
			if (_jetton_current <= this._cur_round_jetton_score[i]) {
				continue;
			}
			count++;
		}
		// 该轮可继续操作的人数
		if (count == 0) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_cur_round_jetton_score[i] = 0;
			}
			if (_end_index == 4) {
				_deh_game_staus = GAME_CHE_PAI_WAIT;
				refresh_game_status(true);
				set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
				this._current_player = GameConstants.INVALID_SEAT;
				Refresh_User_Operate(this._current_player);
				return true;
			} else {
				int player_count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 1) {
						player_count++;
					}
				}

				_jetton_current = 0;

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					_is_jetton[i] = false;
					_is_pass[i] = false;
				}
				count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || this._is_give_up[i]) {
						continue;
					}
					if (this._player_result.game_score[i] == 0) {
						continue;
					}
					count++;
				}
				// 下轮还可操作人数
				if (count <= 1) {
					_end_index = 4;
					this.Send_card(this._end_index, _end_index, true);
					_deh_game_staus = GAME_CHE_PAI_WAIT;
					refresh_game_status(true);
					set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
					this._current_player = GameConstants.INVALID_SEAT;
					Refresh_User_Operate(this._current_player);
					return true;
				} else {
					this.Send_card(this._end_index, _end_index + 1, true);
					if (_end_index >= 2) {
						_cur_round_banker = this._cur_banker;
						int index = this._cur_banker;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							index = (_cur_banker + i) % getTablePlayerNumber();
							if (this._player_ready[index] == 1 && !this._is_give_up[index]) {
								if (this._is_give_up[_cur_round_banker]) {
									_cur_round_banker = index;
								} else {
									if (_logic.GetCardLogicValue(GRR._cards_data[index][_end_index]) > _logic
											.GetCardLogicValue(GRR._cards_data[_cur_round_banker][_end_index])) {
										_cur_round_banker = index;
									}
								}
							}
						}
						if (this._player_result.game_score[_cur_round_banker] == 0) {
							for (int i = 0; i < getTablePlayerNumber(); i++) {
								index = (_cur_round_banker + i) % getTablePlayerNumber();
								if (this._is_give_up[index] || this._player_result.game_score[index] == 0 || this._player_ready[index] == 0) {
									continue;
								}
								_cur_round_banker = index;
								break;
							}
						}
					}
					this._current_player = _cur_round_banker;
					if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
						this._jetton_current_add_min = this._jetton_score[_current_player];
					} else {
						_jetton_current_add_min = 1;
					}
					_end_index += 1;
				}

			}
		}
		Refresh_User_Operate(this._current_player);
		set_timer(ID_TIMER_OPERATE, _operate_time);
		return true;
	}

	public boolean deal_add_score(int seat_index, int jetton_score) {
		if (seat_index != this._current_player) {
			return true;
		}
		if (_jetton_current_add_min == 0) {
			if (_jetton_current_add_min >= jetton_score || this._player_ready[seat_index] == 0 || _is_give_up[seat_index]) {
				return true;
			}
		} else {
			if (_jetton_current_add_min > jetton_score || this._player_ready[seat_index] == 0 || _is_give_up[seat_index]) {
				return true;
			}
		}
		if (jetton_score > _player_result.game_score[seat_index] + _cur_round_jetton_score[seat_index]) {
			return true;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		this.kill_timer();
		_jetton_score[seat_index] += jetton_score - _cur_round_jetton_score[seat_index];
		_total_jetton_score += jetton_score - _cur_round_jetton_score[seat_index];
		_player_result.game_score[seat_index] -= jetton_score - _cur_round_jetton_score[seat_index];
		_cur_round_jetton_score[seat_index] = jetton_score;
		_is_jetton[seat_index] = true;
		_jetton_current = jetton_score;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_is_pass[i] = false;
		}

		int next_player = (_current_player + 1) % this.getTablePlayerNumber();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[next_player] == 0 || _is_give_up[next_player] || this._player_result.game_score[next_player] == 0) {
				next_player = (next_player + 1) % this.getTablePlayerNumber();
			} else {
				break;
			}
		}
		_current_player = next_player;

		if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
			int max_jetton_score = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_jetton_score[i] > max_jetton_score) {
					max_jetton_score = _jetton_score[i];
				}
			}
			this._jetton_current_add_min = max_jetton_score * 2 - _jetton_score[_current_player] + _cur_round_jetton_score[_current_player];
		} else {
			_jetton_current_add_min = _jetton_current + 1;
		}
		Score_Result(seat_index, jetton_score, 1);

		// 操作
		Operate_Result(seat_index, 4);

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0 || _is_give_up[i] || this._player_result.game_score[i] == 0) {
				continue;
			}
			if (_jetton_current <= this._cur_round_jetton_score[i]) {
				continue;
			}
			count++;
		}
		// 本轮还可操作人数
		if (count == 0) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				_cur_round_jetton_score[i] = 0;
			}
			if (_end_index == 4) {
				_deh_game_staus = GAME_CHE_PAI_WAIT;
				refresh_game_status(true);
				set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
				this._current_player = GameConstants.INVALID_SEAT;
				Refresh_User_Operate(this._current_player);
				return true;
			} else {

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					_is_jetton[i] = false;
					_is_pass[i] = false;
				}
				this._jetton_current = 4;
				count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || this._is_give_up[i]) {
						continue;
					}
					if (this._player_result.game_score[i] == 0) {
						continue;
					}
					count++;
				}
				// 下轮还可操作人数
				if (count <= 1) {
					_end_index = 4;
					this.Send_card(this._end_index, _end_index, true);
					_deh_game_staus = GAME_CHE_PAI_WAIT;
					refresh_game_status(true);
					set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
					this._current_player = GameConstants.INVALID_SEAT;
					Refresh_User_Operate(this._current_player);
					return true;
				} else {
					this.Send_card(this._end_index, _end_index + 1, true);
					if (_end_index >= 2) {
						_cur_round_banker = this._cur_banker;
						int index = this._cur_banker;
						for (int i = 0; i < getTablePlayerNumber(); i++) {
							index = (_cur_banker + i) % getTablePlayerNumber();
							if (this._player_ready[index] == 1 && !this._is_give_up[index]) {
								if (this._is_give_up[_cur_round_banker]) {
									_cur_round_banker = index;
								} else {
									if (_logic.GetCardLogicValue(GRR._cards_data[index][_end_index]) > _logic
											.GetCardLogicValue(GRR._cards_data[_cur_round_banker][_end_index])) {
										_cur_round_banker = index;
									}
								}
							}
						}
						if (this._player_result.game_score[_cur_round_banker] == 0) {
							for (int i = 0; i < getTablePlayerNumber(); i++) {
								index = (_cur_round_banker + i) % getTablePlayerNumber();
								if (this._is_give_up[index] || this._player_result.game_score[index] == 0 || this._player_ready[index] == 0) {
									continue;
								}
								_cur_round_banker = index;
								break;
							}
						}
					}
					this._current_player = _cur_round_banker;
					if (has_rule(GameConstants.GAME_RULE_DEH_HAN_QIAN_BI_GUN)) {
						this._jetton_current_add_min = this._jetton_score[_current_player];
					} else {
						_jetton_current_add_min = 1;
					}
					_end_index += 1;
				}
			}
		}
		Refresh_User_Operate(this._current_player);
		set_timer(ID_TIMER_OPERATE, _operate_time);
		return true;
	}

	public boolean deal_pei_si_pi(int seat_index) {
		// 赔死皮
		if (has_rule(GameConstants.GAME_RULE_DEH_PEI_SI_PI)) {
			boolean is_pei_si_pi = true;
			int win_si_pi_seat = GameConstants.INVALID_SEAT;
			;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (this._is_give_up[i] || this.get_players()[i] == null) {
					continue;
				}
				if (this._cur_round_jetton_score[i] > 0) {
					is_pei_si_pi = false;
					break;
				}
			}
			if (is_pei_si_pi) {

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._is_give_up[i] || this._player_ready[i] == 0 || this._player_result.game_score[i] == 0) {
						continue;
					}
					this._is_give_up[i] = true;
				}
				int count = 0;
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._is_give_up[i] || this._player_ready[i] == 0) {
						continue;
					}
					count++;
					win_si_pi_seat = i;
				}
				int win_seat_index = win_si_pi_seat;
				this._current_player = GameConstants.INVALID_SEAT;

				if (win_seat_index != GameConstants.INVALID_SEAT && count == 1) {
					Refresh_User_Operate(this._current_player);
					GameSchedule.put(new GameFinishRunnable(getRoom_id(), win_seat_index, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
					return true;
				} else if (win_seat_index == GameConstants.INVALID_SEAT) {
					return false;
				} else {
					_end_index = 4;
					this.Send_card(this._end_index, _end_index, true);
					_deh_game_staus = GAME_CHE_PAI_WAIT;
					refresh_game_status(true);
					set_timer(ID_TIMER_CHE_PAI_WAIT, 5);
					this._current_player = GameConstants.INVALID_SEAT;
					Refresh_User_Operate(this._current_player);
				}

				return true;
			}
		}
		return false;
	}

	public void refresh_bobo_score(int Opreate_index, int to_player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_BOBO_RESULT);
		roomResponse.setGameStatus(this._game_status);
		Make_BoBo_Result.Builder make_bobo_deh = Make_BoBo_Result.newBuilder();

		Player rplayer;
		int total_bobo = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null || this._player_ready[i] == 0)
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
			room_player.setScore(_bobo_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			make_bobo_deh.addPlayers(room_player);

			total_bobo += _bobo_score[i];
		}
		make_bobo_deh.setOpreatePlayer(Opreate_index);
		make_bobo_deh.setTotalBobo(total_bobo);
		roomResponse.setCommResponse(PBUtil.toByteString(make_bobo_deh));
		if (to_player == GameConstants.INVALID_SEAT) {
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}

	}

	public boolean deal_bobo_score(int seat_index, int jetton_score) {
		if (_is_bobo[seat_index] || jetton_score < _player_result.game_score[seat_index] || _deh_game_staus != GAME_BOBO) {
			return true;
		}
		if (this._zi_chan_score[seat_index] + _player_result.game_score[seat_index] < jetton_score) {
			return true;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		_zi_chan_score[seat_index] -= jetton_score - _player_result.game_score[seat_index];
		this._player_result.game_score[seat_index] = jetton_score;
		_bobo_score[seat_index] = jetton_score;
		_is_bobo[seat_index] = true;

		refresh_bobo_score(seat_index, GameConstants.INVALID_SEAT);

		this.operate_player_data();
		deal_bu_score_query(seat_index);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 0) {
				continue;
			}
			if (!_is_bobo[i]) {
				return true;
			}
		}
		_deh_game_staus = GAME_YIN_CANG;
		make_sure_banker();
		this.Send_card(_end_index, _end_index + 2, true);
		_end_index += 2;
		if (_is_mang_guo) {
			if (_xiu_mang_cur_round <= _xiu_mang_total_round) {
				_xiu_mang_cur_round++;
			}
			if (_xiu_mang_cur_round > _xiu_mang_total_round) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					this._player_result.game_score[i] += _user_mang_pi_score[i];
					_user_mang_pi_score[i] = 0;
				}
				_mang_chi_score = 0;
				_xiu_mang_cur_round = 0;
			} else {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (this._player_ready[i] == 0 || i == _mang_guo_win_seat) {
						continue;
					}
					_mang_chi_score += _mang_pi_score * _xiu_mang_cur_round;
					this._player_result.game_score[i] -= _mang_pi_score * _xiu_mang_cur_round;
					this.Score_Result(i, _mang_pi_score * _xiu_mang_cur_round, 6);
					_user_mang_pi_score[i] += _mang_pi_score * _xiu_mang_cur_round;
				}
			}
			_is_mang_guo = false;
		}
		refresh_table_message();
		Refresh_User_Operate(this._current_player);
		refresh_game_status(true);
		this.operate_player_data();
		return true;
	}

	public boolean deal_bu_score_query(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_ZI_CHAN_SCORE);
		roomResponse.setGameStatus(this._game_status);
		Zi_Chan_Score.Builder zi_chan_score = Zi_Chan_Score.newBuilder();
		zi_chan_score.setBuScoreMin(BU_FEN_MIN);
		zi_chan_score.setBuScoreUseGold(BU_FEN_NEED_GOLD);
		zi_chan_score.setZiChanScore(this._zi_chan_score[seat_index]);
		if (this._zi_chan_score[seat_index] + this._player_result.game_score[seat_index] <= BU_FEN_MIN && !_is_bu_zi_chan[seat_index]) {
			zi_chan_score.setIsBuScore(1);
		} else {
			zi_chan_score.setIsBuScore(0);
		}
		zi_chan_score.setGold(get_players()[seat_index].getGold());

		roomResponse.setCommResponse(PBUtil.toByteString(zi_chan_score));
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public boolean deal_bu_score_sure(int seat_index) {
		if (this._is_bu_zi_chan[seat_index]) {
			this.send_error_notify(seat_index, 2, "已经进行过一次补分");
			return false;
		}
		if (_zi_chan_score[seat_index] >= BU_FEN_MIN) {
			this.send_error_notify(seat_index, 2, "必须低于50分才能进行补分");
			return false;
		}
		if (!kou_dou_aa(this.get_players()[seat_index], 10)) {
			this.send_error_notify(seat_index, 2, SysParamServerDict.getInstance().replaceGoldTipsWord("闲逸豆不足"));
			return false;
		}
		this._zi_chan_score[seat_index] += 100;
		_is_bu_zi_chan[seat_index] = true;
		_init_user_score[seat_index] += 100;
		deal_bu_score_query(seat_index);
		if (this._game_status == GameConstants.GAME_STATUS_WAIT) {
			this.handler_player_ready(seat_index, false);
		}

		return true;
	}

	public void Score_Result(int seat_index, int jetton_score, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_DEH_JETTON_RESULT);
		roomResponse.setGameStatus(this._game_status);
		Score_Result.Builder score_result_deh = Score_Result.newBuilder();
		score_result_deh.setOpreatePlayer(seat_index);
		score_result_deh.setJettonScore(jetton_score);
		score_result_deh.setJettonTotalScore(_jetton_score[seat_index]);
		score_result_deh.setCurrentPlayer(_current_player);
		score_result_deh.setDisplayTime(20);
		score_result_deh.setJettonType(type);
		score_result_deh.setUserScore((int) this._player_result.game_score[seat_index]);

		roomResponse.setCommResponse(PBUtil.toByteString(score_result_deh));
		this.send_response_to_room(roomResponse);

		GRR.add_room_response(roomResponse);
	}

	public void Operate_Result(int seat_index, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_OPREATE_RESULT);
		roomResponse.setGameStatus(this._game_status);
		Opreate_Result.Builder opreate_result_deh = Opreate_Result.newBuilder();
		opreate_result_deh.setOpreatePlayer(seat_index);
		opreate_result_deh.setCurrentPlayer(_current_player);
		opreate_result_deh.setDisplayTime(20);
		opreate_result_deh.setOpreateType(type);

		roomResponse.setCommResponse(PBUtil.toByteString(opreate_result_deh));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
	}

	public void refresh_table_message() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_REFRESH_TABLE);
		roomResponse.setGameStatus(this._game_status);
		Refresh_Table_message.Builder refresh_table_message = Refresh_Table_message.newBuilder();

		int count = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1) {
				count++;
			}
		}
		refresh_table_message.setCurrentPlayer(count);
		refresh_table_message.setMangChiScore(_mang_chi_score);
		refresh_table_message.setMangPi(_mang_pi_score);
		refresh_table_message.setShouMangCurRound(1);
		refresh_table_message.setShouMangTotalRound(1);
		refresh_table_message.setXiuMangCurRound(_xiu_mang_cur_round);
		refresh_table_message.setXiuMangTotalRound(_xiu_mang_total_round);

		roomResponse.setCommResponse(PBUtil.toByteString(refresh_table_message));
		this.send_response_to_room(roomResponse);
		if (GRR != null) {
			GRR.add_room_response(roomResponse);
		}

	}

	public void refresh_player_status(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_REFRESH_PLAYER_STATUS);
		roomResponse.setGameStatus(this._game_status);
		RefreshPlayerStatus.Builder refresh_player_status = RefreshPlayerStatus.newBuilder();
		refresh_player_status.setPlayerIndex(seat_index);

		if (this.get_players()[seat_index] != null) {
			if (this.get_players()[seat_index].isOnline()) {
				if (GameConstants.GS_MJ_FREE == _game_status || GameConstants.GS_MJ_WAIT == _game_status) {
					if (this._player_ready[seat_index] == 1) {
						refresh_player_status.setStatus(1);
					} else {
						refresh_player_status.setStatus(0);
					}
				} else {
					if (this._player_ready[seat_index] == 1) {
						refresh_player_status.setStatus(2);
					} else {
						refresh_player_status.setStatus(3);
					}
				}
			} else {
				refresh_player_status.setStatus(4);
			}

		}
		roomResponse.setCommResponse(PBUtil.toByteString(refresh_player_status));
		this.send_response_to_room(roomResponse);
	}

	public void Refresh_User_Operate(int seat_index) {
		if (this._current_player != seat_index) {
			return;
		}

		if (_current_player != GameConstants.INVALID_SEAT && this.has_rule(GameConstants.GAME_RULE_DEH_TE_PAI)) {
			if (this._end_index >= 3 && _is_special_type[_current_player] != 1) {
				boolean hei_liu = false;
				boolean hong_liu = false;
				boolean dawang = false;
				boolean hei_shi = false;
				boolean hong_shi = false;
				boolean hong_J = false;

				for (int i = 0; i < _end_index; i++) {
					if (GRR._cards_data[_current_player][i] == 0x06 || GRR._cards_data[_current_player][i] == 0x26) {
						if (!hong_liu) {
							hong_liu = true;
						}
					}
					if (GRR._cards_data[_current_player][i] == 0x16 || GRR._cards_data[_current_player][i] == 0x36) {
						if (!hei_liu) {
							hei_liu = true;
						}
					}
					if (GRR._cards_data[_current_player][i] == 0x4F) {
						if (!dawang) {
							dawang = true;
						}
					}
					if (GRR._cards_data[_current_player][i] == 0x0A || GRR._cards_data[_current_player][i] == 0x2A) {
						if (!hong_shi) {
							hong_shi = true;
						}
					}
					if (GRR._cards_data[_current_player][i] == 0x1A || GRR._cards_data[_current_player][i] == 0x3A) {
						if (!hei_shi) {
							hei_shi = true;
						}
					}
					if (GRR._cards_data[_current_player][i] == 0x1B || GRR._cards_data[_current_player][i] == 0x3B) {
						if (!hong_J) {
							hong_J = true;
						}
					}
				}
				if ((hong_liu && dawang && hei_liu) || (hong_shi && hei_shi && hong_J)) {
					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					roomResponse.setType(MsgConstants.RESPONSE_DEH_SPECIAL_REMIN);
					roomResponse.setGameStatus(this._game_status);
					Special_Remin.Builder special_type = Special_Remin.newBuilder();
					special_type.setReminMessage("你有特殊牌型");

					roomResponse.setCommResponse(PBUtil.toByteString(special_type));
					this.send_response_to_player(_current_player, roomResponse);

					_is_special_type[_current_player] = 0;
				}
			}
		}
		if (_jetton_current_add_min < _mang_chi_score) {
			_jetton_current_add_min = _mang_chi_score;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_REFRESH_OPREATE);
		roomResponse.setGameStatus(this._game_status);
		User_Can_Opreate.Builder user_can_operate = User_Can_Opreate.newBuilder();
		if (_jetton_current == 0 && this._end_index == 2 && this.has_rule(GameConstants.GAME_RULE_DEH_BANKER_MUST_CALL)) {
			user_can_operate.setGiveUp(0);
		} else {
			user_can_operate.setGiveUp(1);
		}
		user_can_operate.setCurrentPlayer(_current_player);
		user_can_operate.setDisplaytime(20);
		if (seat_index != GameConstants.INVALID_SEAT) {
			user_can_operate.setShuoHand((int) _player_result.game_score[seat_index]);
			if (_jetton_current > _player_result.game_score[seat_index] + _cur_round_jetton_score[seat_index]) {
				user_can_operate.setGenZhu(0);
				user_can_operate.setAddOperate(0);
			} else {
				user_can_operate.setGenZhu(_jetton_current);

				if (_jetton_current_add_min > _player_result.game_score[seat_index] + _cur_round_jetton_score[seat_index]) {
					user_can_operate.setAddOperate(0);
				} else {
					user_can_operate.setAddOperate(1);
				}
				user_can_operate.setAddScoreMin(_jetton_current_add_min);
				user_can_operate.setAddScoreMax((int) _player_result.game_score[seat_index] + _cur_round_jetton_score[seat_index]);

			}

			boolean is_jetton = false;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_cur_round_jetton_score[i] != 0) {
					is_jetton = true;
					break;
				}
			}
			if (_jetton_current == 0 && this._end_index == 2 && this.has_rule(GameConstants.GAME_RULE_DEH_BANKER_MUST_CALL)) {
				user_can_operate.setPass(0);
			} else {
				if (!is_jetton) {
					user_can_operate.setPass(1);
				} else {
					user_can_operate.setPass(0);
				}
			}
		}
		roomResponse.setCommResponse(PBUtil.toByteString(user_can_operate));
		this.send_response_to_room(roomResponse);
	}

	public boolean handler_open_card(int get_seat_index, List<Integer> list, int card_count) {
		if (_deh_game_staus != GAME_CHE_PAI_BEGIN || _is_open_card[get_seat_index]) {
			return true;
		}
		if (this._player_ready[get_seat_index] == 0) {
			return true;
		}
		int card_date[] = new int[get_hand_card_count_max()];
		for (int i = 0; i < list.size(); i++) {
			card_date[i] = list.get(i);
		}
		if (!this._logic.check_data(GRR._cards_data[get_seat_index], GRR._card_count[get_seat_index], card_date, list.size())) {
			this.send_error_notify(get_seat_index, 2, "摆牌不正确！");
			return false;
		}
		logger.info("房间[" + this.getRoom_id() + "]" + "局数[" + this._cur_round + "]" + "_out_cards_data:" + Arrays.toString(card_date)
				+ "_out_card_count:" + card_count);
		for (int i = 0; i < list.size(); i++) {
			this.GRR._cards_data[get_seat_index][i] = list.get(i);
		}

		_is_open_card[get_seat_index] = true;
		int card_one[] = new int[this.get_hand_card_count_max()];
		int card_two[] = new int[this.get_hand_card_count_max()];
		int type_one = 0;
		int type_two = 0;
		int win_num = 0;
		for (int card_index = 0; card_index < 2; card_index++) {
			card_one[card_index] = this.GRR._cards_data[get_seat_index][card_index];
		}
		for (int card_index = 0; card_index < 2; card_index++) {
			card_two[card_index] = this.GRR._cards_data[get_seat_index][card_index + 2];
		}
		this._logic.SortCardList(card_one, 2);
		this._logic.SortCardList(card_two, 2);
		type_one = this._logic.GetCardType(card_one, 2);
		type_two = this._logic.GetCardType(card_two, 2);
		if (type_one < type_two) {
			for (int card_index = 0; card_index < 2; card_index++) {
				this.GRR._cards_data[get_seat_index][card_index] = card_two[card_index];
			}
			for (int card_index = 0; card_index < 2; card_index++) {
				this.GRR._cards_data[get_seat_index][card_index + 2] = card_one[card_index];
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DEH_OPEN_CARD_RESULT);
		roomResponse.setGameStatus(this._game_status);
		Open_Card_Result.Builder open_card_result = Open_Card_Result.newBuilder();
		open_card_result.setOpreatePlayer(get_seat_index);
		for (int i = 0; i < this.GRR._card_count[get_seat_index]; i++) {
			open_card_result.addCardsData(this.GRR._cards_data[get_seat_index][i]);
		}

		for (int card_index = 0; card_index < 2; card_index++) {
			card_one[card_index] = this.GRR._cards_data[get_seat_index][card_index];
		}
		for (int card_index = 0; card_index < 2; card_index++) {
			card_two[card_index] = this.GRR._cards_data[get_seat_index][card_index + 2];
		}
		this._logic.SortCardList(card_one, 2);
		this._logic.SortCardList(card_two, 2);
		type_one = this._logic.GetCardType(card_one, 2);
		type_two = this._logic.GetCardType(card_two, 2);
		open_card_result.addCardsType(type_one);
		open_card_result.addCardsType(type_two);
		roomResponse.setCommResponse(PBUtil.toByteString(open_card_result));
		this.send_response_to_room(roomResponse);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this._player_ready[i] == 1 && this._is_open_card[i] == false && !this._is_give_up[i]) {
				return true;
			}
		}
		_deh_game_staus = GAME_BI_WEI;
		this.refresh_game_status(true);

		// 开始比牌
		roomResponse.setType(MsgConstants.RESPONSE_DEH_CHEPAI_END);
		roomResponse.setGameStatus(this._game_status);
		Che_Pai_End.Builder che_pai_end = Che_Pai_End.newBuilder();
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			int type = 0;
			for (int j = 2; j < 4; j++) {
				if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
					cards_card.addItem(this.GRR._cards_data[i][j]);
				} else {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
			}
			int card_data[] = new int[this.get_hand_card_count_max()];
			for (int card_index = 0; card_index < 2; card_index++) {
				card_data[card_index] = this.GRR._cards_data[i][card_index + 2];
			}
			if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
				this._logic.SortCardList(card_data, 2);
				type = this._logic.GetCardType(card_data, 2);
			}
			che_pai_end.addType(type);
			che_pai_end.addCardsData(cards_card);
			che_pai_end.addCardCount(2);
		}

		che_pai_end.setOpreateType(2);
		roomResponse.setCommResponse(PBUtil.toByteString(che_pai_end));
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);
		set_timer(ID_TIMER_TOU_TO_WEI, 1);
		return true;
	}

	public boolean deal_special_type(int seat_index) {
		if (_is_special_type[seat_index] != 0) {
			return false;
		}
		if (this._player_ready[seat_index] == 0) {
			return true;
		}
		_is_special_type[seat_index] = 1;
		return true;
	}

	public boolean handler_requst_opreate(int seat_index, int opreate_type, int jetton_score) {

		switch (opreate_type) {
		case 1: {
			// 丢
			return deal_give_up(seat_index);
		}
		case 2: {
			// 休
			return deal_pass(seat_index);
		}
		case 3: {
			// 敲
			return deal_shuo_hand(seat_index);
		}
		case 4: {
			// 大
			return deal_add_score(seat_index, jetton_score);
		}
		case 5: {
			// 跟
			return deal_gen_jetton(seat_index);
		}
		case 6: {
			// 簸簸
			return deal_bobo_score(seat_index, jetton_score);
		}
		case 7: {
			deal_special_type(seat_index);
			return true;
		}
		case 8: {
			return deal_bu_score_sure(seat_index);
		}
		}
		return true;
	}

	@Override
	public boolean animation_timer(int timer_id) {
		_cur_game_timer = -1;
		switch (timer_id) {
		case ID_TIMER_TOU_TO_WEI: {
			_deh_game_staus = GAME_BI_TOU;
			this.refresh_game_status(true);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DEH_CHEPAI_END);
			roomResponse.setGameStatus(this._game_status);
			Che_Pai_End.Builder che_pai_end = Che_Pai_End.newBuilder();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				int type = 0;
				for (int j = 0; j < 2; j++) {
					if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
						cards_card.addItem(this.GRR._cards_data[i][j]);
					} else {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				int card_data[] = new int[this.get_hand_card_count_max()];
				for (int card_index = 0; card_index < 2; card_index++) {
					card_data[card_index] = this.GRR._cards_data[i][card_index];
				}
				this._logic.SortCardList(card_data, 2);
				if (this._player_ready[i] == 1 && !this._is_give_up[i]) {
					type = this._logic.GetCardType(card_data, 2);
				}
				che_pai_end.addType(type);
				che_pai_end.addCardsData(cards_card);
				che_pai_end.addCardCount(2);
			}

			che_pai_end.setOpreateType(1);
			roomResponse.setCommResponse(PBUtil.toByteString(che_pai_end));
			this.send_response_to_room(roomResponse);
			GRR.add_room_response(roomResponse);
			GameSchedule.put(new GameFinishRunnable(getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
			return true;
		}
		case ID_TIMER_CHE_PAI_WAIT: {
			set_timer(ID_TIMER_OPERATE, _operate_time * 10);
			che_pai_begin(GameConstants.INVALID_SEAT);
			return true;
		}
		case ID_TIMER_READY: {
			int cur_count = 0;
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_player_ready[i] == 1) {
					cur_count++;
				} else {
					continue;
				}
			}
			if (cur_count < 2) {
				return true;
			}

			if (cur_count >= 2) {
				handler_game_start();
			}
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				this.refresh_player_status(i);
			}

			return true;
		}
		case ID_TIMER_OPERATE: {
			if (this._deh_game_staus == GAME_CHE_PAI_BEGIN) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (!_is_open_card[i] && !_is_give_up[i] && this._player_ready[i] == 1) {
						List<Integer> list = new ArrayList<>();
						for (int j = 0; j < GRR._card_count[i]; j++) {
							list.add(GRR._cards_data[i][j]);
						}
						handler_open_card(i, list, GRR._card_count[i]);
					}
				}
			} else {
				this.deal_give_up(this._current_player);
			}
		}
		}
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_OPREATE_DEH) {
			Opreate_Request_DEH req = PBUtil.toObject(room_rq, Opreate_Request_DEH.class);
			return handler_requst_opreate(seat_index, req.getOpreateType(), req.getJettonScore());
		}
		if (type == MsgConstants.REQUST_OPEN_CARD_DEH) {
			Opreate_open_card req = PBUtil.toObject(room_rq, Opreate_open_card.class);
			return handler_open_card(seat_index, req.getCardsDataList(), req.getCardCount());
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
