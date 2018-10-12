package com.cai.game.dtz;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_DTZ;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.util.FvMask;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.AbstractRoom;
import com.cai.game.RoomUtil;
import com.cai.game.dtz.handler.DTZHandlerOutCardOperate;
import com.cai.game.dtz.runnable.AutoOutCardRunnable;
import com.cai.game.dtz.runnable.AutoPassRunnable;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dtz.DTZPro.CurrRoundScore;
import protobuf.clazz.dtz.DTZPro.GameEndDTZ;
import protobuf.clazz.dtz.DTZPro.GameStartDTZ;
import protobuf.clazz.dtz.DTZPro.OutCardDataDTZ;
import protobuf.clazz.dtz.DTZPro.PlayerScore;
import protobuf.clazz.dtz.DTZPro.RoomResponseDTZ;
import protobuf.clazz.dtz.DTZPro.ShowAutoOutCardTime;

public class Table_DTZ extends AbstractRoom {

	private static final long serialVersionUID = 984613541460485104L;

	public DTZGameLogic _logic;

	public ScheduledFuture _auto_out_card_scheduled;
	public DTZHandler<Table_DTZ> _handler;
	public DTZHandlerOutCardOperate _handler_out_card_operate;

	public int _current_player = GameConstants.INVALID_SEAT; // 当前用户
	public int _prev_player = GameConstants.INVALID_SEAT;
	public int _turn_out_player = GameConstants.INVALID_SEAT;// 上一出牌玩家
	public int _turn_out_card_count; // 上一次出牌数目
	public int _turn_out_card_data[]; // 上一次 出牌扑克
	public int _turn_out_card_type; // 上一次 出牌牌型
	public int _cur_out_card_data[][];
	public int _cur_out_card_count[];

	public int _turn_have_score; // 当局得分
	public int _get_score[];
	public int _history_score[];
	public int _magic_score[];

	public int _is_shou_chu;
	public int _banker_player;
	public int _chuwan_shunxu[];
	public int _curr_round_score[]; // 5,10,K的分

	public int[] score_detail_count; // 0-2:5,10,k 次数，3：地炸次数
										// 4；K筒子次数,5；A筒子次数,6；2筒子次数
	public int[] score_detail;
	public int[][] player_score_detail_count;
	public int[][] player_score_detail;
	public int[] player_curr_round_score;

	public int[][] game_end_player_score_detail_count;
	public int[][] game_end_player_score_detail;

	protected long _request_release_time;
	protected ScheduledFuture _release_scheduled;
	public int out_plane_count;

	public Table_DTZ() {
		super(RoomType.HH, GameConstants.GAME_PLAYER);

		_logic = new DTZGameLogic();
		_game_status = GameConstants.GS_MJ_FREE;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (_game_status == GameConstants.GS_MJ_PAO) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (seat_index == i) {
						continue;
					}
					this._player_result.pao[i] = 0;
				}
			}
		}

		if (_auto_out_card_scheduled != null) {
			_auto_out_card_scheduled.cancel(true);
			_auto_out_card_scheduled = null;
		}

		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		if (_chuwan_shunxu[0] != GameConstants.INVALID_SEAT) {
			_cur_banker = _chuwan_shunxu[0];
		}

		ret = this.on_handler_game_finish(seat_index, reason);
		return ret;
	}

	public int cal_win_order_score(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			return 0;
		}
		int score = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_chuwan_shunxu[i] == seat_index) {
				if (i == 0) {
					if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_2)) {
						score = 60;
					} else if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_3)) {
						score = 100;
					}
				} else if (i == 1) {
					if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_2)) {
						score = -60;
					} else if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_3)) {
						score = -40;
					}
				} else if (i == 2) {
					if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_2)) {
						score = 0;
					} else if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_3)) {
						score = -60;
					}
				}
				break;
			}
		}

		_player_result.game_score[seat_index] += score;
		game_end_player_score_detail[seat_index][7] += score;
		player_curr_round_score[seat_index] += score;
		_history_score[seat_index] += score;
		return score;
	}

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		GameEndDTZ.Builder gameEndBuilder = GameEndDTZ.newBuilder();
		game_end.setRoomInfo(getRoomInfo());
		load_player_info_data(gameEndBuilder);
		gameEndBuilder.setRoomInfo(getRoomInfo());
		gameEndBuilder.setCurrRound(_cur_round);
		gameEndBuilder.setBankerPlayer(_banker_player);

		// 流局计算筒子炸
		if (reason == GameConstants.Game_End_RELEASE_PLAY) {
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				if (GRR._card_count[p] < 3) {
					continue;
				}
				int count = _logic.have_tube_card(GRR._cards_data[p], GRR._card_count[p], 0x0D);
				if (count != 0) {
					player_score_detail_count[p][4] += count;
					player_score_detail[p][4] += 100 * count;
				}
				count = _logic.have_tube_card(GRR._cards_data[p], GRR._card_count[p], 0x01);
				if (count != 0) {
					player_score_detail_count[p][5] += count;
					player_score_detail[p][5] += 200 * count;
				}
				count = _logic.have_tube_card(GRR._cards_data[p], GRR._card_count[p], 0x02);
				if (count != 0) {
					player_score_detail_count[p][6] += count;
					player_score_detail[p][6] += 300 * count;
				}
				count = _logic.get_bom_di(GRR._cards_data[p], GRR._card_count[p]);
				if (count != 0) {
					player_score_detail_count[p][3] += count;
					player_score_detail[p][3] += 400 * count;
				}
			}
		}
		if (GRR != null) {
			game_end.setStartTime(GRR._start_time);

			for (int p = 0; p < getTablePlayerNumber(); p++) {
				_history_score[p] += player_curr_round_score[p];

				gameEndBuilder.addCardCount(GRR._card_count[p]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int pc = 0; pc < GRR._card_count[p]; pc++) {
					cards_card.addItem(GRR._cards_data[p][pc]);
				}
				gameEndBuilder.addCardsData(cards_card);
			}

			gameEndBuilder.setLeftCardCount(GRR._left_card_count);
			int n = GRR._left_card_count;
			for (int i = 0; i < n; i++) {
				gameEndBuilder.addLeftCardsData(_repertory_card[_all_card_len - GRR._left_card_count]);
				GRR._left_card_count--;
			}
		}
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			gameEndBuilder.addRound(_cur_round);
			gameEndBuilder.addWinOrder(_chuwan_shunxu[p]);

			Int32ArrayResponse.Builder sdc = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder sd = Int32ArrayResponse.newBuilder();
			for (int si = 0; si < 4; si++) {
				sdc.addItem(player_score_detail_count[p][si]);
				sd.addItem(player_score_detail[p][si]);
			}
			sdc.addItem(player_score_detail_count[p][4] + player_score_detail_count[p][5] + player_score_detail_count[p][6]);
			sd.addItem(player_score_detail[p][4] + player_score_detail[p][5] + player_score_detail[p][6]);
			gameEndBuilder.addScoreDetailCount(sdc);
			gameEndBuilder.addScoreDetail(sd);
			gameEndBuilder.addWinOrderScore(cal_win_order_score(p, reason));
			gameEndBuilder.addCurrRoundScore(player_curr_round_score[p]);
			gameEndBuilder.addIntegral(_history_score[p]);
			game_end.addGameScore(player_curr_round_score[p]);

			for (int si = 0; si < player_score_detail_count[p].length; si++) {
				game_end_player_score_detail_count[p][si] += player_score_detail_count[p][si];
				game_end_player_score_detail[p][si] += player_score_detail[p][si];
			}
		}

		// boolean have_equal = false;
		// for (int p = 0; p < getTablePlayerNumber(); p++) {
		// for (int pp = 0; pp < getTablePlayerNumber(); pp++) {
		// if (p == pp) {
		// continue;
		// }
		// if (_history_score[p] == _history_score[pp]) {
		// have_equal = true;
		// break;
		// }
		// }
		// }
		int max_history_score = 0;
		int max_history_score_index = -1;
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (max_history_score < _history_score[p]) {
				max_history_score = _history_score[p];
				max_history_score_index = p;
			}
		}

		int limit = 600;
		if (has_rule(GameConstants_DTZ.GAME_RULE_FEN_1000)) {
			limit = 1000;
		}
		int reward_score = 0;
		if (has_rule(GameConstants_DTZ.GAME_RULE_REWARD_NON) && max_history_score >= limit) {
			reward_score = 0;
		}
		if (has_rule(GameConstants_DTZ.GAME_RULE_REWARD_100) && max_history_score >= limit) {
			reward_score = 100;
		}
		if (has_rule(GameConstants_DTZ.GAME_RULE_REWARD_200) && max_history_score >= limit) {
			reward_score = 200;
		}
		if (has_rule(GameConstants_DTZ.GAME_RULE_REWARD_300) && max_history_score >= limit) {
			reward_score = 300;
		}
		if (has_rule(GameConstants_DTZ.GAME_RULE_REWARD_500) && max_history_score >= limit) {
			reward_score = 500;
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (max_history_score >= limit) {// 局数到了
				int[] score = new int[getTablePlayerNumber()];
				cal_win_lose_score(_history_score, score);
				if (max_history_score_index == -1) {
					for (int p = 0; p < getTablePlayerNumber(); p++) {
						if (max_history_score < _history_score[p]) {
							max_history_score = _history_score[p];
							max_history_score_index = p;
						}
					}
				}
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					Int32ArrayResponse.Builder mbc = Int32ArrayResponse.newBuilder();
					Int32ArrayResponse.Builder mb = Int32ArrayResponse.newBuilder();
					for (int si = 0; si < 3; si++) {
						mbc.addItem(game_end_player_score_detail_count[p][4 + si]);
						mb.addItem(game_end_player_score_detail[p][4 + si]);
					}
					gameEndBuilder.addMagicBombCount(mbc);
					gameEndBuilder.addMagicBombSocre(mb);
					gameEndBuilder.addDiBonmSocre(game_end_player_score_detail[p][3]);
					gameEndBuilder.addPaiScore(game_end_player_score_detail[p][7] + game_end_player_score_detail[p][0]
							+ game_end_player_score_detail[p][1] + game_end_player_score_detail[p][2]);
					gameEndBuilder.addAllScore(_history_score[p]);
					if (p == max_history_score_index) {
						gameEndBuilder.addWinLoseScore(reward_score);
						_player_result.game_score[p] = score[p] + reward_score * (getTablePlayerNumber() - 1);
						gameEndBuilder.addEndRewardScore((int) _player_result.game_score[p]);
					} else {
						gameEndBuilder.addWinLoseScore(0);
						_player_result.game_score[p] = score[p] - reward_score;
						gameEndBuilder.addEndRewardScore((int) _player_result.game_score[p]);
					}
				}
				end = true;
				real_reason = GameConstants.Game_End_ROUND_OVER;
				game_end.setPlayerResult(process_player_result(reason));
			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			int[] score = new int[getTablePlayerNumber()];
			cal_win_lose_score(_history_score, score);
			if (max_history_score_index == -1) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					if (max_history_score < _history_score[p]) {
						max_history_score = _history_score[p];
						max_history_score_index = p;
					}
				}
			}
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				Int32ArrayResponse.Builder mbc = Int32ArrayResponse.newBuilder();
				Int32ArrayResponse.Builder mb = Int32ArrayResponse.newBuilder();
				for (int si = 0; si < 3; si++) {
					mbc.addItem(game_end_player_score_detail_count[p][4 + si]);
					mb.addItem(game_end_player_score_detail[p][4 + si]);
				}
				gameEndBuilder.addMagicBombCount(mbc);
				gameEndBuilder.addMagicBombSocre(mb);
				gameEndBuilder.addDiBonmSocre(game_end_player_score_detail[p][3]);
				gameEndBuilder.addPaiScore(game_end_player_score_detail[p][7] + game_end_player_score_detail[p][0]
						+ game_end_player_score_detail[p][1] + game_end_player_score_detail[p][2]);
				gameEndBuilder.addAllScore(_history_score[p]);
				if (p == max_history_score_index) {
					gameEndBuilder.addWinLoseScore(reward_score);
					_player_result.game_score[p] = score[p] + reward_score * (getTablePlayerNumber() - 1);
					gameEndBuilder.addEndRewardScore((int) _player_result.game_score[p]);
				} else {
					gameEndBuilder.addWinLoseScore(0);
					_player_result.game_score[p] = score[p] - reward_score;
					gameEndBuilder.addEndRewardScore((int) _player_result.game_score[p]);
				}
			}
			end = true;
			game_end.setPlayerResult(process_player_result(reason));
			// real_reason = GameConstants.Game_End_RELEASE_PLAY;
		}
		if (GRR == null) {
			real_reason = GameConstants.Game_End_RELEASE_RESULT;
		}
		gameEndBuilder.setReason(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		game_end.setEndType(real_reason);
		game_end.setGameRound(_game_round);
		game_end.setCurRound(_cur_round);
		game_end.setCommResponse(PBUtil.toByteString(gameEndBuilder));
		game_end.setRoundOverType(1);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

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

		if (!is_sys()) {
			GRR = null;
		}
		if (end)// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}
		Arrays.fill(_get_score, 0);
		return true;
	}

	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
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

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// if(is_mj_type(MJGameConstants.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			// }else if(is_mj_type(MJGameConstants.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(_player_result.da_hu_zi_mo[i]);
			player_result.addDaHuJiePao(_player_result.da_hu_jie_pao[i]);
			player_result.addDaHuDianPao(_player_result.da_hu_dian_pao[i]);
			player_result.addXiaoHuZiMo(_player_result.xiao_hu_zi_mo[i]);
			player_result.addXiaoHuJiePao(_player_result.xiao_hu_jie_pao[i]);
			player_result.addXiaoHuDianPao(_player_result.xiao_hu_dian_pao[i]);

			player_result.addPiaoLaiCount(_player_result.piao_lai_count[i]);
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

		RoomPlayerResponse.Builder room_player = RoomPlayerResponse.newBuilder();
		room_player.setAccountId(this.getCreate_player().getAccount_id());
		room_player.setHeadImgUrl(this.getCreate_player().getAccount_icon());
		room_player.setIp(this.getCreate_player().getAccount_ip());
		room_player.setUserName(this.getCreate_player().getNick_name());
		room_player.setSeatIndex(this.getCreate_player().get_seat_index());
		room_player.setOnline(this.getCreate_player().isOnline() ? 1 : 0);
		room_player.setIpAddr(this.getCreate_player().getAccount_ip_addr());
		room_player.setSex(this.getCreate_player().getSex());
		room_player.setScore(0);
		room_player.setReady(0);
		room_player.setPao(0);
		room_player.setQiang(0);
		if (this.getCreate_player().locationInfor != null) {
			room_player.setLocationInfor(this.getCreate_player().locationInfor);
		}
		player_result.setCreatePlayer(room_player);
		return player_result;
	}

	private void cal_win_lose_score(int[] history_score, int[] score) {
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_history_score[p] += game_end_player_score_detail[p][3] + game_end_player_score_detail[p][4] + game_end_player_score_detail[p][5]
					+ game_end_player_score_detail[p][6];
		}
		int[] cal_histo = new int[getTablePlayerNumber()];
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			int m = history_score[p] / 100;
			int n = history_score[p] % 100;
			if (Math.abs(n) >= 50) {
				if (history_score[p] >= 0) {
					cal_histo[p] = (m + 1) * 100;
				} else {
					cal_histo[p] = (m - 1) * 100;
				}
			} else {
				cal_histo[p] = m * 100;
			}
		}

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			for (int p1 = 0; p1 < getTablePlayerNumber(); p1++) {
				if (p == p1) {
					continue;
				}
				score[p] += cal_histo[p] - cal_histo[p1];
			}
		}
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		_game_type_index = game_type_index;//
		_game_rule_index = game_rule_index;
		_game_round = game_round;

		_cur_round = 0;

		_player_result = new PlayerResult(this.getRoom_owner_account_id(), this.getRoom_id(), _game_type_index, _game_rule_index, _game_round,
				this.get_game_des());

		onInitTableHandler();
	}

	public void onInitTableHandler() {
		_handler_out_card_operate = new DTZHandlerOutCardOperate();

		_history_score = new int[getTablePlayerNumber()];
		_magic_score = new int[getTablePlayerNumber()];

		game_end_player_score_detail_count = new int[getTablePlayerNumber()][8];
		game_end_player_score_detail = new int[getTablePlayerNumber()][8];
	}

	@Override
	public boolean handler_operate_out_card_mul(int _seat_index, List<Integer> list, int card_count, int b_out_card, String desc) {

		if (this._handler_out_card_operate != null) {
			this._handler = this._handler_out_card_operate;
			int out_cards[] = new int[card_count];
			for (int i = 0; i < card_count; i++) {
				out_cards[i] = list.get(i);
			}

			this._handler_out_card_operate.reset_status(_seat_index, out_cards, card_count, b_out_card, desc);
			this._handler.exe(this);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

		}
		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				fixBugTemp(seat_index);
			}
		}, 1, TimeUnit.SECONDS);

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
		if (_release_scheduled == null) {
			int release_players[] = new int[getTablePlayerNumber()];
			Arrays.fill(release_players, 2);
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setReleaseTime(100);
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_RELEASE);
			roomResponse.setRequestPlayerSeat(seat_index);
			roomResponse.setOperateCode(1);
			roomResponse.setLeftTime(50);
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				roomResponse.addReleasePlayers(release_players[i]);
			}
			send_response_to_room(roomResponse);
		}
		if (is_sys())
			return true;
		// return handler_player_ready(seat_index, false);
		return true;
	}

	public void fixBugTemp(int seat_index) {
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
				// roomResponse.setLeftTime((_request_release_time -
				// System.currentTimeMillis()) / 1000);

				for (int i = 0; i < getTablePlayerNumber(); i++) {
					roomResponse.addReleasePlayers(_gameRoomRecord.release_players[i]);
				}
				this.send_response_to_player(seat_index, roomResponse);
			}
		}
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {
		if (is_cancel) {//
			if (this.get_players()[seat_index] == null) {
				return false;
			}
			if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status && _game_status != GameConstants.GS_MJ_XIAOHU) {
				return false;
			}
			_player_ready[seat_index] = 0;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			roomResponse.setIsCancelReady(is_cancel);
			send_response_to_room(roomResponse);
			if (this._cur_round > 0) {
				// 结束后刷新玩家
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(_game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				this.load_player_info_data(roomResponse2);
				this.send_response_to_player(seat_index, roomResponse2);
			}
			return false;
		}
		if (this.get_players()[seat_index] == null) {
			return false;
		}

		_player_ready[seat_index] = 1;

		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
			return false;
		}

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);

		if (this._cur_round > 0) {
			// 结束后刷新玩家
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setGameStatus(_game_status);
			roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
			this.load_player_info_data(roomResponse2);
			this.send_response_to_player(seat_index, roomResponse2);
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}
		if (playerNumber == 0) {
			playerNumber = this.getTablePlayerNumber();
		}
		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);
		return true;
	}

	@Override
	public boolean handler_player_offline(Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setGameStatus(_game_status);
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);

		send_response_to_other(player.get_seat_index(), roomResponse);
		return true;
	}

	public void progress_banker_select() {
		if (_cur_round == 1) {// 金币场 随机庄家
			if (has_rule(GameConstants_DTZ.GAME_RULE_RODAM)) {
				Random random = new Random();//
				int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
				_cur_banker = rand % getTablePlayerNumber();//
			} else {
				_cur_banker = 0;
			}
		}
	}

	public int get_hand_card_count_max() {
		return 50;
	}

	@Override
	public boolean reset_init_data() {

		if (_cur_round == 0) {
			record_game_room();
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		_prev_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		this._handler = null;
		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, get_hand_card_count_max(), GameConstants.MAX_HH_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		_cur_round++;
		GRR._cur_round = _cur_round;

		_chuwan_shunxu = new int[getTablePlayerNumber()];
		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(get_hand_card_count_max());
		}

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].reset();
		}

		// 牌局回放
		GRR._room_info.setRoomId(this.getRoom_id());
		GRR._room_info.setGameRuleIndex(_game_rule_index);
		GRR._room_info.setGameRuleDes(get_game_des());
		GRR._room_info.setGameTypeIndex(_game_type_index);
		GRR._room_info.setGameRound(_game_round);
		GRR._room_info.setCurRound(_cur_round);
		GRR._room_info.setGameStatus(_game_status);
		GRR._room_info.setCreatePlayerId(this.getRoom_owner_account_id());
		if (gameRuleIndexEx != null) {
			int[] ruleEx = new int[gameRuleIndexEx.length];
			for (int i = 0; i < gameRuleIndexEx.length; i++) {
				ruleEx[i] = gameRuleIndexEx[i];
				GRR._room_info.addGameRuleIndexEx(ruleEx[i]);
			}

		}
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

			_chuwan_shunxu[i] = GameConstants.INVALID_SEAT;
		}
		GRR._video_recode.setBankerPlayer(this._cur_banker);
		GRR._room_info = this.getRoomInfo();

		_curr_round_score = new int[6];
		_turn_out_card_data = new int[getHandCardCount()];
		_cur_out_card_data = new int[getTablePlayerNumber()][getHandCardCount()];
		_cur_out_card_count = new int[getTablePlayerNumber()];
		_get_score = new int[getTablePlayerNumber()];
		score_detail_count = new int[7];
		score_detail = new int[7];
		player_score_detail_count = new int[getTablePlayerNumber()][7];
		player_score_detail = new int[getTablePlayerNumber()][7];
		player_curr_round_score = new int[getTablePlayerNumber()];
		_is_shou_chu = 1;

		_turn_have_score = 0;
		_turn_out_card_count = 0;
		_turn_out_card_type = 0;
		Arrays.fill(_turn_out_card_data, 0);
		Arrays.fill(_curr_round_score, 0);
		Arrays.fill(GRR._cur_round_pass, 0);
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_cur_out_card_count[p] = 0;
			Arrays.fill(_cur_out_card_data[p], 0);
		}
		return true;
	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		// 庄家选择
		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_turn_out_player = GameConstants.INVALID_SEAT;
		_turn_out_card_count = 0;
		for (int i = 0; i < this.getHandCardCount(); i++) {
			_turn_out_card_data[i] = GameConstants.INVALID_CARD;
		}
		_turn_out_card_type = GameConstants.PDK_CT_ERROR;

		_repertory_card = new int[GameConstants_DTZ.CARD_DATA_THREE.length];
		shuffle(_repertory_card, GameConstants_DTZ.CARD_DATA_THREE);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return on_game_start();
	}

	protected boolean on_game_start() {

		_current_player = _cur_banker;
		_banker_player = _cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态

		int FlashTime = 4000;
		int standTime = 1000;

		for (int play_index = 0; play_index < this.getTablePlayerNumber(); play_index++) {

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_common_status(roomResponse);
			load_player_info_data(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_DTZ_GAME_START);
			roomResponse.setGameStatus(this._game_status);

			GameStartDTZ.Builder gameStartBuilder = GameStartDTZ.newBuilder();
			gameStartBuilder.setCurBanker(_cur_banker);
			gameStartBuilder.setRoomInfo(getRoomInfo());
			gameStartBuilder.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				gameStartBuilder.addCardCount(GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
				gameStartBuilder.addCardsData(cards_card);
			}
			// 手牌--将自己的手牌数据发给自己
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[play_index]; j++) {
				cards_card.addItem(GRR._cards_data[play_index][j]);
			}
			// gameStartBuilder.setCardCount(play_index,
			// GRR._card_count[play_index]);
			gameStartBuilder.setCardsData(play_index, cards_card);

			roomResponse.setCommResponse(PBUtil.toByteString(gameStartBuilder));

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

			// 自己才有牌数据
			this.send_response_to_player(play_index, roomResponse);
		}

		// -------------回放数据---------------------
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		load_common_status(roomResponse);
		load_player_info_data(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_DTZ_GAME_START);
		roomResponse.setGameStatus(this._game_status);

		GameStartDTZ.Builder gameStartBuilder = GameStartDTZ.newBuilder();
		gameStartBuilder.setCurBanker(_cur_banker);
		gameStartBuilder.setRoomInfo(getRoomInfo());
		gameStartBuilder.setLeftCardCount(GRR._left_card_count);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			gameStartBuilder.addCardCount(GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GRR._card_count[i]; j++) {
				cards_card.addItem(GRR._cards_data[i][j]);
			}
			gameStartBuilder.addCardsData(cards_card);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(gameStartBuilder));
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
		GRR.add_room_response(roomResponse);
		// -------------回放数据---------------------

		GameSchedule.put(new Runnable() {

			@Override
			public void run() {
				// 显示出牌
				operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants_DTZ.CT_PASS, GameConstants.INVALID_SEAT);
			}
		}, 1500, TimeUnit.MILLISECONDS);

		final Table_DTZ table = this;
		_auto_out_card_scheduled = GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				operate_show_auto_time(_current_player, GameConstants_DTZ.DISPLAY_TIME_AUTO_OUT);
				_auto_out_card_scheduled = GameSchedule.put(new AutoOutCardRunnable(getRoom_id(), _current_player, table),
						GameConstants_DTZ.DISPLAY_TIME_AUTO_OUT, TimeUnit.SECONDS);
			}
		}, GameConstants_DTZ.DISPLAY_TIME_15 * 1000 + 1500, TimeUnit.MILLISECONDS);

		PlayerStatus curPlayerStatus = _playerStatus[_current_player];
		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		this.operate_player_status();

		this._handler = this._handler_out_card_operate;

		refresh_user_get_score(GameConstants_DTZ.INVALID_SEAT, true);
		refresh_curr_round_score(GameConstants_DTZ.INVALID_SEAT);
		return true;
	}

	public void auto_out_card(int seat_index) {
		if (seat_index != _current_player) {
			return;
		}
		if (!has_rule(GameConstants_DTZ.GAME_RULE_TRUSTEE)) {
			return;
		}

		int[] cards_data = new int[get_hand_card_count_max()];
		int out_card_count = _logic.Ai_Out_Card(GRR._cards_data[_current_player], GRR._card_count[_current_player], _turn_out_card_data,
				_turn_out_card_count, cards_data, this);
		int is_out = 0;
		if (out_card_count != 0) {
			is_out = 1;
			_logic.sort_card_date_list(cards_data, out_card_count);
		}
		_handler = _handler_out_card_operate;
		_handler_out_card_operate.reset_status(_current_player, cards_data, out_card_count, is_out, "");
		_handler_out_card_operate.exe(this);
	}

	/**
	 * 加载基础状态 癞子 定鬼 牌型状态 玩家状态
	 * 
	 * @param roomResponse
	 */
	public void load_common_status(RoomResponse.Builder roomResponse) {
		roomResponse.setCurrentPlayer(_current_player);
		roomResponse.setGameStatus(_game_status);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			roomResponse.addCardStatus(_playerStatus[i]._card_status);
			roomResponse.addPlayerStatus(_playerStatus[i].get_status());
		}
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
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

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponseDTZ.Builder roomResponse) {
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

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(GameEndDTZ.Builder roomResponse) {
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

	protected int getHandCardCount() {
		if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_2)) {
			return 33;
		}
		if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_3)) {
			return 41;
		}
		return 41;
	}

	/// 洗牌
	private void shuffle(int repertory_card[], int mj_cards[]) {

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		GRR._left_card_count = repertory_card.length;
		_all_card_len = repertory_card.length;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < getHandCardCount(); j++) {
				GRR._cards_data[i][j] = repertory_card[i * getHandCardCount() + j];
			}
			GRR._card_count[i] = getHandCardCount();
			_logic.sort_card_date_list(GRR._cards_data[i], GRR._card_count[i]);
			GRR._left_card_count -= getHandCardCount();
		}
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private void test_cards() {
		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/
		// if (BACK_DEBUG_CARDS_MODE
		if (debug_my_cards != null) {
			if (debug_my_cards.length > getHandCardCount()) {
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

		// }
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < this.getHandCardCount(); j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		this._repertory_card = realyCards;

		// 分发扑克
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < this.getTablePlayerNumber(); i++) {
				if (this.get_players()[i] == null) {
					continue;
				}
				for (int j = 0; j < this.getHandCardCount(); j++) {
					GRR._cards_data[i][j] = realyCards[k++];
				}
				GRR._card_count[i] = getHandCardCount();
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
	public void testSameCard(int[] cards) {

		int count = cards.length > getHandCardCount() ? getHandCardCount() : cards.length;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = 0;
			}

		}
		// 分发扑克
		int k = 0;
		int i = 0;

		for (; i < this.getTablePlayerNumber(); i++) {
			k = 0;
			for (int j = 0; j < count; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
			GRR._card_count[i] = count;
		}

		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	@Override
	public boolean exe_finish(int reason) {
		return false;
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_2)) {
			return 2;
		}
		if (has_rule(GameConstants_DTZ.GMAE_RULE_PLAYER_3)) {
			return 3;
		}
		return 2;
	}

	@Override
	public boolean record_effect_action(int seat_index, int effect_type, int effect_count, long[] effect_indexs, int time) {
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

	public boolean operate_show_auto_time(int seat_index, int displayer_time) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DTZ_SHOW_AUTO_OUT_CARD_TIME);
		ShowAutoOutCardTime.Builder showTime = ShowAutoOutCardTime.newBuilder();
		showTime.setTargetPlayer(seat_index);
		showTime.setDisplayerTime(displayer_time);

		roomResponse.setCommResponse(PBUtil.toByteString(showTime));
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
	public boolean operate_out_card(int seat_index, int count, int cards_data[], int type, int to_player) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DTZ_OUT_CARD);
		roomResponse.setTarget(seat_index);

		OutCardDataDTZ.Builder outCardDataBuilder = OutCardDataDTZ.newBuilder();
		outCardDataBuilder.setOutCardPlayer(seat_index);
		outCardDataBuilder.setCardsCount(count);
		for (int c = 0; c < count; c++) {
			outCardDataBuilder.addCardsData(cards_data[c]);
		}

		outCardDataBuilder.setCardType(type);
		outCardDataBuilder.setCurPlayer(_current_player);
		outCardDataBuilder.setPrCardsCount(_turn_out_card_count);
		outCardDataBuilder.setPrOutCardType(_turn_out_card_type);
		for (int t = 0; t < _turn_out_card_count; t++) {
			outCardDataBuilder.addPrCardsData(_turn_out_card_data[t]);
		}
		if (_is_shou_chu == 1) {
			outCardDataBuilder.setIsFirstOut(true);
		} else {
			outCardDataBuilder.setIsFirstOut(false);
		}
		if (_turn_out_card_count == 0) {
			// 首出玩家一次能出完就全部弹起来
			int cbCardType = _logic.GetCardType(GRR._cards_data[_current_player], GRR._card_count[_current_player]);
			if (!has_rule(GameConstants_DTZ.GAME_RULE_CAN_DAI_CARD) && (cbCardType == GameConstants_DTZ.CT_THREE_TAKE_ONE
					|| cbCardType == GameConstants_DTZ.CT_THREE_TAKE_TWO || cbCardType == GameConstants_DTZ.CT_PLANE)) {
				cbCardType = GameConstants_DTZ.CT_ERROR;
			}
			if (cbCardType != GameConstants_DTZ.CT_ERROR) {
				outCardDataBuilder.setIsHaveNotCard(true);
			}
			outCardDataBuilder.setIsFirstOut(true);
		} else {
			outCardDataBuilder.setIsFirstOut(false);
		}
		if (_current_player != GameConstants_DTZ.INVALID_SEAT) {
			int[] user_can_out_data = new int[get_hand_card_count_max()];
			int can_out_card_count = _logic.Player_Can_out_card(GRR._cards_data[_current_player], GRR._card_count[_current_player],
					_turn_out_card_data, _turn_out_card_count, user_can_out_data);
			for (int i = 0; i < can_out_card_count; i++) {
				outCardDataBuilder.addUserCanOutData(user_can_out_data[i]);
			}
			outCardDataBuilder.setUserCanOutCount(can_out_card_count);

			if (can_out_card_count == 0 && has_rule(GameConstants_DTZ.GAME_RULE_HAVE_CARD_THAN_OUT)) {
				GameSchedule.put(new AutoPassRunnable(this.getRoom_id(), _current_player, this), 750, TimeUnit.MILLISECONDS);
			}
		}

		// if (_current_player != GameConstants_DTZ.INVALID_SEAT) {
		// if (this.GRR._card_count[_current_player] == 0) {
		// outCardDataBuilder.setIsHaveNotCard(true);
		// } else {
		// outCardDataBuilder.setIsHaveNotCard(false);
		// }
		// } else {
		// outCardDataBuilder.setIsHaveNotCard(true);
		// }

		if (has_rule(GameConstants_DTZ.GAME_RULE_HAVE_CARD_THAN_OUT)) {
			outCardDataBuilder.setHaveCardThanOut(true);
		} else {
			outCardDataBuilder.setHaveCardThanOut(false);
		}
		outCardDataBuilder.setDisplayTime(GameConstants_DTZ.DISPLAY_TIME_15);

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			outCardDataBuilder.addHandCardCount(this.GRR._card_count[i]);
			outCardDataBuilder.addWinOrder(this._chuwan_shunxu[i]);
		}
		if (_turn_out_card_type == GameConstants_DTZ.CT_PLANE_LOST || _turn_out_card_type == GameConstants_DTZ.CT_PLANE) {
			outCardDataBuilder.setPineCount(out_plane_count);
		}

		if (to_player == GameConstants.INVALID_SEAT) {

			for (int p = 0; p < getTablePlayerNumber(); p++) {
				outCardDataBuilder.clearHandCardData();
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cab = Int32ArrayResponse.newBuilder();
					for (int ic = 0; ic < GRR._card_count[p] && p == i; ic++) {
						cab.addItem(GRR._cards_data[p][ic]);
					}
					outCardDataBuilder.addHandCardData(cab);
				}
				roomResponse.setCommResponse(PBUtil.toByteString(outCardDataBuilder));
				this.send_response_to_player(p, roomResponse);
			}

			//
			outCardDataBuilder.clearHandCardData();
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cab = Int32ArrayResponse.newBuilder();
				for (int ic = 0; ic < GRR._card_count[i]; ic++) {
					cab.addItem(GRR._cards_data[i][ic]);
				}
				outCardDataBuilder.addHandCardData(cab);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outCardDataBuilder));
			GRR.add_room_response(roomResponse);

			// return this.send_response_to_room(roomResponse);
		} else {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cab = Int32ArrayResponse.newBuilder();
				for (int ic = 0; ic < GRR._card_count[to_player] && to_player == i; ic++) {
					cab.addItem(GRR._cards_data[to_player][ic]);
				}
				outCardDataBuilder.addHandCardData(cab);
			}
			roomResponse.setCommResponse(PBUtil.toByteString(outCardDataBuilder));
			return this.send_response_to_player(to_player, roomResponse);
		}

		return true;
	}

	public void refresh_curr_round_score(int to_player) {
		RoomResponse.Builder roomResponseBuilder = RoomResponse.newBuilder();
		roomResponseBuilder.setType(MsgConstants.RESPONSE_DTZ_REFRESH_CURR_ROUND_SCORE);
		CurrRoundScore.Builder currRoundScoreBuilder = CurrRoundScore.newBuilder();

		int cal_all_score = 0;
		for (int i = 0; i < 3; i++) {
			currRoundScoreBuilder.addCardsScoreDetail(_curr_round_score[i + 3]);
			cal_all_score += _curr_round_score[i];
		}
		currRoundScoreBuilder.setCurzScore(cal_all_score);

		roomResponseBuilder.setCommResponse(PBUtil.toByteString(currRoundScoreBuilder));

		if (to_player == GameConstants_DTZ.INVALID_SEAT) {
			GRR.add_room_response(roomResponseBuilder);
			this.send_response_to_room(roomResponseBuilder);
		} else {
			this.send_response_to_player(to_player, roomResponseBuilder);
		}
	}

	public void refresh_user_get_score(int to_player, boolean cal_his) {
		RoomResponse.Builder roomResponseBuilder = RoomResponse.newBuilder();
		roomResponseBuilder.setType(MsgConstants.RESPONSE_DTZ_REFRESH_USER_GET_SCORE);
		PlayerScore.Builder playerScoreBuilder = PlayerScore.newBuilder();
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			Player player = get_players()[p];
			if (player == null) {
				continue;
			}
			playerScoreBuilder.addHistoryScore(_history_score[p]);
			// if (cal_his) {
			// } else {
			// playerScoreBuilder.addHistoryScore(-10000); // 不实时刷新
			// }
			playerScoreBuilder.addCurScore(_get_score[p]);
			playerScoreBuilder.addMagicScore(_magic_score[p]);
		}

		roomResponseBuilder.setCommResponse(PBUtil.toByteString(playerScoreBuilder));

		operate_player_data();
		if (to_player == GameConstants_DTZ.INVALID_SEAT) {
			GRR.add_room_response(roomResponseBuilder);
			this.send_response_to_room(roomResponseBuilder);
		} else {
			this.send_response_to_player(to_player, roomResponseBuilder);
		}
	}

	/**
	 * 基础状态
	 * 
	 * @return
	 */
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
	public boolean operate_player_data() {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);
		this.load_player_info_data(roomResponse);

		GRR.add_room_response(roomResponse);

		return this.send_response_to_room(roomResponse);
	}

	@Override
	public boolean handler_refresh_player_data(int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		this.load_player_info_data(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	/**
	 * 效果 (通知玩家弹出 吃碰杆 胡牌==效果)
	 * 
	 * @param seat_index
	 * @param effect_type
	 * @param effect_count
	 * @param effect_indexs
	 * @param time
	 * @param to_player
	 * @return
	 */
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
			this.send_response_to_room(roomResponse);
		} else {
			this.send_response_to_player(to_player, roomResponse);
		}
		GRR.add_room_response(roomResponse);

		return true;
	}

	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

	@Override
	public void runnable_remove_out_cards(int seat_index, int type) {
	}

	@Override
	public void runnable_set_trustee(int _seat_index) {

	}

	@Override
	public void runnable_add_discard(int _seat_index, int _card_count, int[] _card_data, boolean _send_client) {

	}

	@Override
	public boolean handler_request_trustee(int get_seat_index, boolean isTrustee, int Trustee_type) {
		return true;
	}

	@Override
	public void clear_score_in_gold_room() {
	}

	@Override
	public boolean kickout_not_ready_player() {
		return true;
	}

	@Override
	public boolean open_card_timer() {
		return true;
	}

	@Override
	public boolean robot_banker_timer() {
		return true;
	}

	@Override
	public boolean ready_timer() {
		return true;
	}

	@Override
	public boolean add_jetton_timer() {
		return true;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		return true;
	}

	@Override
	public boolean handler_operate_card(int get_seat_index, int operateCode, int operateCard, int luoCode) {
		return true;
	}

	@Override
	public boolean handler_operate_button(int seat_index, int operate_code) {
		return true;
	}

	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		return true;
	}

	@Override
	public boolean handler_add_jetton(int seat_index, int jetton) {
		return true;
	}

	@Override
	public boolean handler_open_cards(int seat_index, boolean open_flag) {
		return true;
	}

	@Override
	public boolean handler_player_out_card(int get_seat_index, int operateCard) {
		return true;
	}

	@Override
	public boolean runnable_dispatch_last_card_data(int _seat_index, int _type, boolean _tail) {
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
	public boolean dispatch_card_data(int cur_player, int type, boolean tail) {
		return true;
	}

	@Override
	public boolean runnable_gang_card_data(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self,
			boolean d) {
		return true;
	}

	@Override
	public boolean handler_release_room(Player player, int operateCode) {
		if (is_sys()) {
			// return handler_release_room_in_gold(player, operateCode);
		}
		int seat_index = 0;
		if (player != null) {
			seat_index = player.get_seat_index();
		}

		SysParamModel sysParamModel3007 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(1).get(3007);
		int delay = 60;
		if (sysParamModel3007 != null) {
			delay = sysParamModel3007.getVal1();
		}

		switch (operateCode) {
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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (get_players()[i] == null)
					continue;
				if (_gameRoomRecord.release_players[i] != 1) {
					return false;// 有一个不同意
				}
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
			this.get_players()[seat_index] = null;
			_player_ready[seat_index] = 0;

			if (player != null) {
				PlayerServiceImpl.getInstance().quitRoomId(this.getRoom_id(), player.getAccount_id());
			}

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

			if (player.getAccount_id() == getRoom_owner_account_id()) {
				this.getCreate_player().set_seat_index(GameConstants.INVALID_SEAT);
			}
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
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return true;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		return true;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}
}
