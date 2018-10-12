package com.cai.game.mj.yu.gui_yang;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.GameConstants_GY;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.TimeUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.Tuple;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 贵阳抓鸡
 * 
 * @author yu
 *
 */
public class Table_GY extends AbstractMJTable {

	public int chong_feng_ji_seat_yj; // 幺鸡冲锋鸡

	public int chong_feng_ji_seat_bt; // 八筒冲锋鸡

	public int[][] out_ji_pai; // 每个人打出的鸡牌数量
	public int[] out_ji_pai_count; // 每个人打出的鸡牌数量

	public int[] _ji_card_index; // 指定鸡牌

	public int _ji_card_count; // 指定 鸡牌数量

	public Tuple<Integer, Integer>[] responsibility_ji; // 责任鸡

	public Tuple<Integer, Integer>[] show_responsibility_ji; // 责任鸡

	public boolean[] jin_ji; // 本局是否有金鸡

	public boolean[] player_mo_first;

	public MJHandlerOutCardBaoTing_GY _handler_out_card_bao_ting;

	public HandlerSelectMagic_GY handler_select_magic;

	public boolean[] shao; // 烧鸡烧杠

	public int[] player_all_ji_card;

	public boolean show_continue_banker;

	public int[][] player_11_detail;

	public int[][] player_GangScore_type;

	public float[] hu_type_socre;

	public int[] player_ji_score;

	public int[] player_duan;

	MJHandlerFinish_GY handler_finish;

	public GameRoundRecord game_end_GRR;

	public int[] zi_da;

	float hu_base_fen;

	public RoomResponse.Builder roomResponse;

	public int old_banker;

	public Table_GY() {
		super(MJType.GAME_TYPE_GUI_YANG);
		_ji_card_count = 0;
		_ji_card_index = new int[GameConstants.MAX_COUNT];
	}

	public void add_ji_card_index(int index) {
		_ji_card_index[_ji_card_count] = index;
		_ji_card_count++;
	}

	public boolean is_ji_card(int card) {
		for (int i = 0; i < _ji_card_count; i++) {
			if (_ji_card_index[i] == _logic.switch_to_card_index(card)) {
				return true;
			}
		}
		return false;
	}

	public boolean is_ji_index(int index) {
		for (int i = 0; i < _ji_card_count; i++) {
			if (_ji_card_index[i] == index) {
				return true;
			}
		}
		return false;
	}

	public void clean_ji_cards() {
		_ji_card_count = 0;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants_GY.CARD_ESPECIAL_TYPE_TING && card < GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_GY.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		if (card > GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
			return card;
		}
		return card;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_GY();
		_handler_dispath_card = new HandlerDispatchCard_GY();
		_handler_gang = new HandlerGang_GY();
		_handler_out_card_operate = new HandlerOutCardOperate_GY();
		_handler_out_card_bao_ting = new MJHandlerOutCardBaoTing_GY();
		handler_select_magic = new HandlerSelectMagic_GY();
		handler_finish = new MJHandlerFinish_GY();
	}

	@Override
	public boolean exe_out_card_bao_ting(int seat_index, int card, int type) {
		// 出牌
		this.set_handler(this._handler_out_card_bao_ting);
		this._handler_out_card_bao_ting.reset_status(seat_index, card, type);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_select_magic() {
		this.set_handler(this.handler_select_magic);
		handler_select_magic.reset_status(_cur_banker);
		_handler.exe(this);

		return true;
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
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
			room_player.setNao(_player_result.nao[i]);
			if (has_rule(GameConstants_GY.GAME_RULE_CONTINUE_BANKER)) {
				room_player.setQiang(_player_result.qiang[i]);
			}
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}

			if (_game_status == GameConstants.GS_MJ_PAO) {
				if (null != _playerStatus[i]) {
					// 叫地主的字段值，用来标示玩家是否点了跑呛
					if (_playerStatus[i]._is_pao_qiang) {
						room_player.setJiaoDiZhu(1);
					} else {
						room_player.setJiaoDiZhu(0);
					}
				}
			}

			roomResponse.addPlayers(room_player);
		}
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
			if (player_duan[i] != -1 && _logic.get_card_color(card) == player_duan[i])
				continue;

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_GY.HU_CARD_TYPE_QIANG_GANG, i);

				if (player_duan[i] != -1) {
					for (int c = 0; c < GameConstants.MAX_INDEX; c++) {
						if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]) {
							action = GameConstants_HZLZG.WIK_NULL;
							break;
						}
					}
				}
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
			// if (_playerStatus[i].is_bao_ting())
			// continue;
			if (player_duan[i] != -1 && _logic.get_card_color(card) == player_duan[i])
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_GY.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0 && !_playerStatus[i].is_bao_ting()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				// int cards[] = new int[GameConstants.MAX_COUNT];
				// int hand_card_count =
				// _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_GY.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_GY.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_GY.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants.GANG_TYPE_AN_GANG || type == GameConstants.GANG_TYPE_ADD_GANG
							|| type == GameConstants.GANG_TYPE_JIE_GANG) {
						card_type = GameConstants_GY.HU_CARD_TYPE_RE_PAO;
					}

					// 此处偷个懒，若出牌得人已报听牌，其他人没有通行证的情况下也是可以胡的
					if (_playerStatus[seat_index].is_bao_ting())
						card_type = GameConstants_GY.HU_CARD_TYPE_ZI_MO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (player_duan[i] != -1) {
						for (int c = 0; c < GameConstants.MAX_INDEX; c++) {
							if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]) {
								action = GameConstants_HZLZG.WIK_NULL;
								break;
							}
						}
					}
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_GY.WIK_CHI_HU);
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
		if (has_rule(GameConstants_GY.GAME_RULE_PLAYER_2))
			return 2;
		if (has_rule(GameConstants_GY.GAME_RULE_PLAYER_3))
			return 3;
		if (has_rule(GameConstants_GY.GAME_RULE_PLAYER_3_DING_GUAI))
			return 3;
		if (has_rule(GameConstants_GY.GAME_RULE_PLAYER_2_DING_GUAI))
			return 2;
		return 4;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	public boolean exe_finish(int reason) {
		this._end_reason = reason;
		if (_end_reason == GameConstants.Game_End_NORMAL || _end_reason == GameConstants.Game_End_DRAW
				|| _end_reason == GameConstants.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		this.set_handler(this.handler_finish);
		this.handler_finish.exe(this);

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

				if (shao[i] || _playerStatus[i]._hu_card_count == 0)
					continue;
				if (reason == GameConstants.Game_End_NORMAL)
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						if (player_GangScore_type[i][j] == GameConstants.GANG_TYPE_AN_GANG)
							GRR._result_des[i] = "闷逗            +3" + "_" + GRR._result_des[i];
						if (player_GangScore_type[i][j] == GameConstants.GANG_TYPE_ADD_GANG)
							GRR._result_des[i] = "转弯豆          +3" + "_" + GRR._result_des[i];
						if (player_GangScore_type[i][j] == GameConstants.GANG_TYPE_JIE_GANG)
							GRR._result_des[i] = "明豆            +3" + "_" + GRR._result_des[i];
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							if (player_GangScore_type[i][j] == GameConstants.GANG_TYPE_JIE_GANG) {
								if (GRR._gang_score[i].scores[j][k] < 0) {
									GRR._result_des[k] = "明豆            -3" + "_" + GRR._result_des[k];
								}
							}
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 鏉犵墝锛屾瘡涓汉鐨勫垎鏁�
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

				Int32ArrayResponse.Builder items = Int32ArrayResponse.newBuilder();
				// 角标 第十一位
				for (int c = 10; c < 11; c++) {
					items.addItem(player_11_detail[i][c]);
				}
				game_end.addLostFanShu(items);
				game_end.addGangScore(lGangScore[i]);
				game_end.addPao((int) hu_type_socre[i]);
				game_end.addJettonScore(player_ji_score[i]);
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

		for (int player = 0; player < getTablePlayerNumber() && GRR != null; player++) {
			GRR._result_des[player] = "";
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
		this.roomResponse = roomResponse;

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
			game_end_GRR = GRR;
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		// && GameConstants.GS_MJ_WAIT != _game_statu
		if ((GameConstants.GS_MJ_FREE != _game_status) && this.get_players()[seat_index] != null) {
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

		if (is_sys())
			return true;
		// return handler_player_ready(seat_index, false);
		return true;
	}

	@Override
	protected boolean on_game_start() {
		_logic.clean_magic_cards();
		this.clean_ji_cards();
		this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_GY.YJ_CARD));
		if (has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI))
			this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_GY.BA_TONG_CARD));

		_game_status = GameConstants_GY.GS_MJ_PLAY;

		chong_feng_ji_seat_bt = -1;
		chong_feng_ji_seat_yj = -1;
		out_ji_pai = new int[getTablePlayerNumber()][];

		out_ji_pai_count = new int[getTablePlayerNumber()];
		jin_ji = new boolean[2]; // 只有两个默认的鸡产生金鸡
		responsibility_ji = new Tuple[2];
		show_responsibility_ji = new Tuple[2];
		player_mo_first = new boolean[getTablePlayerNumber()];
		shao = new boolean[getTablePlayerNumber()];
		player_all_ji_card = new int[getTablePlayerNumber()];
		player_11_detail = new int[getTablePlayerNumber()][11];
		hu_type_socre = new float[getTablePlayerNumber()];
		player_ji_score = new int[getTablePlayerNumber()];
		player_duan = new int[getTablePlayerNumber()];
		zi_da = new int[getTablePlayerNumber()];
		player_GangScore_type = new int[getTablePlayerNumber()][4];
		hu_base_fen = 0;
		for (int i = 0; i < 2; i++) {
			responsibility_ji[i] = new Tuple<Integer, Integer>(-1, -1);
			show_responsibility_ji[i] = new Tuple<Integer, Integer>(-1, -1);
		}
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			GRR._result_des[player] = "";
			out_ji_pai[player] = new int[8];
			player_mo_first[player] = true;
			player_duan[player] = -1;
		}

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_HanShouWang.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
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

		old_banker = _current_player;
		exe_dispatch_card(_current_player, GameConstants_GY.DispatchCard_Type_Tian_Hu, GameConstants_GY.DELAY_SEND_CARD_DELAY);

		return true;
	}

	/**
	 * 扑克转换 将手中牌索引 转换为实际牌数据
	 * 
	 * @param cards_index
	 * @param cards_data
	 * @return
	 */
	public int switch_to_cards_data(int cards_index[], int cards_data[], int seat_index) {
		boolean add_flag = false;
		if (player_duan[seat_index] != -1) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
				if (cards_index[i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[seat_index]) {
					add_flag = true;
					break;
				}
			}
		}
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					if (add_flag && _logic.get_card_color(_logic.switch_to_card_data(i)) != player_duan[seat_index]) {
						cards_data[cbPosition++] = _logic.switch_to_card_data(i) + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					} else {
						cards_data[cbPosition++] = _logic.switch_to_card_data(i);
					}
				}
			}
		}
		return cbPosition;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_GY.MAX_INDEX];
		for (int i = 0; i < GameConstants_GY.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		if (player_duan[seat_index] != -1) {
			for (int i = 0; i < GameConstants.MAX_INDEX; i++)
				if (GRR._cards_index[seat_index][i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[seat_index])
					return count;
		}

		int max_ting_count = GameConstants_GY.MAX_ZI;
		int real_max_ting_count = GameConstants_GY.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_GY.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
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

	public void huan_zhuan() {
		int[] jiao_pai = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++)
			if (_playerStatus[i]._hu_card_count > 0)
				jiao_pai[i] = 1;

		int flag = jiao_pai[0];
		boolean equally = true;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (flag != jiao_pai[i]) {
				equally = false;
				break;
			}
		}

		if (equally)
			return;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ChiHuRight chr = huan_zhuang_cha_hu(GRR._cards_index[i], GRR._weave_items[i], GRR._weave_count[i], i);
			if (chr == null)
				continue;

			process_cha_hu_score(i, chr);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ChiHuRight huan_zhuang_cha_hu(int[] cards_index, WeaveItem[] weaveItems, int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_GY.MAX_INDEX];
		for (int i = 0; i < GameConstants_GY.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentCard;
		int max_ting_count = GameConstants_GY.MAX_ZI;

		List<com.cai.common.util.Tuple> list = Lists.newArrayList();
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			ChiHuRight chr = new ChiHuRight();
			if (GameConstants_GY.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					Constants_HuangZhou.CHR_ZI_MO, seat_index)) {
				list.add(new com.cai.common.util.Tuple<>(cbCurrentCard, chr, 0));
			}
		}

		if (list.isEmpty())
			return null;

		// 设置各胡型权重
		list.forEach((tuple) -> {
			ChiHuRight chr = (ChiHuRight) tuple.getCenter();
			if (!chr.opr_and(GameConstants_GY.CHR_QING_LONG_BEI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_LONG_BEI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_LONG_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_LONG_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_QING_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_SHUANG_QING).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_SHUANG_QING);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_QING_DA_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_DA_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_QING_YI_SE).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_YI_SE);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_XIAO_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_XIAO_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_GY.CHR_DAN_DIAO).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_QING_YI_SE);
				return;
			} else if (!chr.opr_and(GameConstants_GY.CHR_DA_DUI_ZI).is_empty()) {
				tuple.setRight(GameConstants_GY.CHR_DA_DUI_ZI);
				return;
			}

			tuple.setRight(1); // 权重最低
		});

		Collections.sort(list, new Comparator<com.cai.common.util.Tuple>() {
			@Override
			public int compare(com.cai.common.util.Tuple o1, com.cai.common.util.Tuple o2) {
				int r1 = (Integer) o1.getRight();
				int r2 = (Integer) o2.getRight();
				if (r1 == r2)
					return 0;

				return r1 > r2 ? -1 : 2;
			}
		});

		return (ChiHuRight) (list.get(0).getCenter());
	}

	public void process_cha_hu_score(int seat_index, ChiHuRight chr) {
		float lChiHuScore = get_hu_fen(chr);

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			if (_playerStatus[i]._hu_card_count > 0)
				continue;

			float s = lChiHuScore + (continue_banker_count > 0 ? continue_banker_count + 1 : 0);

			GRR._game_score[i] -= s;
			hu_type_socre[i] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}
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
			return GameConstants_GY.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_GY.CHR_XIAO_QI_DUI;
		}
	}

	private int get_hu_fen(ChiHuRight chr) {
		int fen = 0;
		if (!chr.opr_and(GameConstants_GY.CHR_QING_LONG_BEI).is_empty()) {
			fen = 30;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_LONG_QI_DUI).is_empty()) {
			fen = 20;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_QI_DUI).is_empty()) {
			fen = 20;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_SHUANG_QING).is_empty()) {
			fen = 20;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_DA_DUI).is_empty()) {
			fen = 15;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_QING_YI_SE).is_empty()) {
			fen = 10;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_XIAO_QI_DUI).is_empty()) {
			fen = 10;
			return fen;
		}
		if (!chr.opr_and(GameConstants_GY.CHR_DAN_DIAO).is_empty()) {
			fen = 10;
			return fen;
		} else if (!chr.opr_and(GameConstants_GY.CHR_DA_DUI_ZI).is_empty()) {
			fen = 5;
			return fen;
		}
		return fen;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		boolean have_fan_hu = false;
		int check_dui_zi_hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card);
		if (GameConstants.WIK_NULL != check_dui_zi_hu) {
			have_fan_hu = true;
			if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
				if (GameConstants_GY.CHR_XIAO_QI_DUI == check_dui_zi_hu) {
					chiHuRight.opr_or(GameConstants_GY.CHR_QING_QI_DUI);
				}
				if (GameConstants_GY.CHR_LONG_QI_DUI == check_dui_zi_hu) {
					chiHuRight.opr_or(GameConstants_GY.CHR_QING_LONG_BEI);
				}
			} else {
				chiHuRight.opr_or(check_dui_zi_hu);
			}
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count());

		if (!bValue && check_dui_zi_hu == GameConstants_GY.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants_HanShouWang.WIK_NULL;
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			have_fan_hu = true;
			chiHuRight.opr_or(GameConstants_GY.CHR_QING_YI_SE);
		}

		boolean pengpeng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), new int[] {}, 0);
		if (pengpeng_hu) {
			have_fan_hu = true;
			if (chiHuRight.opr_and(GameConstants_GY.CHR_QING_YI_SE).is_empty()) {
				if (_logic.get_card_count_by_index(cards_index) == 1) {
					chiHuRight.opr_or(GameConstants_GY.CHR_DAN_DIAO);
				}
				chiHuRight.opr_or(GameConstants_GY.CHR_DA_DUI_ZI);
			} else {
				if (_logic.get_card_count_by_index(cards_index) == 1) {
					chiHuRight.opr_or(GameConstants_GY.CHR_SHUANG_QING);
				} else {
					chiHuRight.opr_or(GameConstants_GY.CHR_QING_DA_DUI);
				}
			}
		}

		if (card_type == GameConstants_GY.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_GY.CHR_QING_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_GY.HU_CARD_TYPE_RE_PAO) {
			chiHuRight.opr_or(GameConstants_GY.CHR_RE_PAO);// 热炮
		} else if (card_type == GameConstants_GY.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_GY.CHR_GNAG_KAI);
		} else if (card_type == GameConstants_GY.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);
		}

		if (card_type == GameConstants_GY.HU_CARD_TYPE_JIE_PAO) {
			int gang_count = 0;
			for (int w = 0; w < GRR._weave_count[_seat_index]; w++) {
				if (GRR._weave_items[_seat_index][w].weave_kind != GameConstants.WIK_GANG) {
					continue;
				}
				gang_count++;
			}
			if (have_fan_hu)
				return GameConstants_GY.WIK_CHI_HU;
			if (gang_count > 0)
				return GameConstants_GY.WIK_CHI_HU;

			chiHuRight.set_empty();
			return GameConstants_GY.WIK_NULL;
		}
		return GameConstants_GY.WIK_CHI_HU;
	}

	public void process_ji_fen() {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			int s = 0;
			int ting_count = _playerStatus[player]._hu_card_count;

			// 吹风鸡
			if (has_rule(GameConstants_GY.GAME_RULE_BLOW_JI) && handler_select_magic._da_dian_card == GameConstants_GY.WU_TONG_CARD) {
				GRR._result_des[player] = "吹风鸡" + "_" + GRR._result_des[player];
				continue;
			}

			int base = 2;
			if (has_rule(GameConstants_GY.GAME_RULE_TONG_3))
				base = 3;
			// 冲锋鸡
			if (chong_feng_ji_seat_yj == player) {
				s += jin_ji[0] ? base * 2 : base; // 冲锋鸡也要算金鸡分

				if (jin_ji[0]) {
					String str = _playerStatus[player]._hu_card_count == 0 ? "金冲锋鸡      -" : "金冲锋鸡      +";
					GRR._result_des[player] = str + base * 2 + "_" + GRR._result_des[player];
				} else {
					String str = _playerStatus[player]._hu_card_count == 0 ? "冲锋鸡          -" : "冲锋鸡          +";
					GRR._result_des[player] = str + base + "_" + GRR._result_des[player];
				}
			}
			if (chong_feng_ji_seat_bt == player) {
				s += jin_ji[1] ? base * 4 : base * 2;

				if (jin_ji[1]) {
					String str = _playerStatus[player]._hu_card_count == 0 ? "金冲乌骨        -" : "金冲乌骨         +";
					GRR._result_des[player] = str + base * 4 + "_" + GRR._result_des[player];
				} else {
					String str = _playerStatus[player]._hu_card_count == 0 ? "冲锋乌骨        -" : "冲锋乌骨        +";
					GRR._result_des[player] = str + base * 2 + "_" + GRR._result_des[player];
				}
			}

			if (out_ji_pai_count[player] > 0)
				player_all_ji_card[player] = out_ji_pai_count[player];

			// 已打出的固定鸡牌
			for (int j = 0; j < out_ji_pai_count[player]; j++) {
				int out_card = out_ji_pai[player][j];
				int out_card_value = _logic.get_card_value(out_card);

				// 星期鸡
				if (ting_count > 0 && has_rule(GameConstants_GY.GAME_RULE_WEEK_JI) && TimeUtil.isSameWeekDay(_logic.get_card_value(out_card_value))) {
					s += 1;
					player_11_detail[player][0]++;
					player_all_ji_card[player]++;
				}

				if (!is_ji_card(out_card))
					continue;

				// 乌骨鸡
				if (out_card == GameConstants_GY.BA_TONG_CARD) {
					int wu_base = jin_ji[1] ? 4 : 2;
					s += wu_base;
					player_11_detail[player][2] += wu_base;
					continue;
				}

				// 幺鸡
				if (out_card == GameConstants_GY.YJ_CARD) {
					int wu_base = jin_ji[0] ? 2 : 1;
					s += wu_base;
					player_11_detail[player][1] += wu_base;
					continue;
				}

				s += 1;
				player_11_detail[player][3]++;
			}

			if (ting_count > 0) {
				// 已被打出翻鸡牌
				for (int dis = 0; dis < GRR._discard_count[player] && has_rule(GameConstants_GY.GAME_RULE_MAN_TANG_JI); dis++) {
					int discard = GRR._discard_cards[player][dis];
					// 星期鸡
					if (has_rule(GameConstants_GY.GAME_RULE_WEEK_JI) && TimeUtil.isSameWeekDay(_logic.get_card_value(discard))) {
						player_11_detail[player][0]++;
						s += 1;
						player_all_ji_card[player]++;
					}

					if (discard == GameConstants_GY.YJ_CARD)
						continue;
					if (has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI) && discard == GameConstants_GY.BA_TONG_CARD)
						continue;

					if (is_ji_card(discard)) {
						player_11_detail[player][3]++;
						s += 1;
						player_all_ji_card[player]++;
					}
				}

				// 叫牌了，算手上的鸡牌了咯
				int hand_cards[] = new int[GameConstants.MAX_COUNT];
				_logic.switch_to_cards_data(GRR._cards_index[player], hand_cards);
				for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
					int hand_card = hand_cards[i];

					// 星期鸡
					if (has_rule(GameConstants_GY.GAME_RULE_WEEK_JI) && TimeUtil.isSameWeekDay(_logic.get_card_value(hand_card))) {
						player_11_detail[player][0]++;
						s += 1;
						player_all_ji_card[player] += 1;
					}

					if (!is_ji_card(hand_card))
						continue;

					player_all_ji_card[player]++;

					// 乌骨鸡
					if (hand_card == GameConstants_GY.BA_TONG_CARD && has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI)) {
						int wu_base = jin_ji[1] ? 4 : 2;
						s += wu_base;
						player_11_detail[player][2] += wu_base;
						continue;
					}

					// 幺鸡
					if (hand_card == GameConstants_GY.YJ_CARD) {
						int wu_base = jin_ji[0] ? 2 : 1;
						s += wu_base;
						player_11_detail[player][1] += wu_base;
						continue;
					}

					s += 1;
					player_11_detail[player][3]++;
				}

				for (int j = 0; j < GRR._weave_count[player]; j++) {
					// 星期鸡
					if (has_rule(GameConstants_GY.GAME_RULE_WEEK_JI)
							&& TimeUtil.isSameWeekDay(_logic.get_card_value(GRR._weave_items[player][j].center_card))) {
						if (GameConstants_GY.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
							player_all_ji_card[player] += 3;
							s += 3;
							player_11_detail[player][0] += 3;
						} else {
							player_all_ji_card[player] += 4;
							s += 4;
							player_11_detail[player][0] += 4;
						}
					}

					if (!is_ji_card(GRR._weave_items[player][j].center_card))
						continue;

					// 乌骨鸡
					if (GRR._weave_items[player][j].center_card == GameConstants_GY.BA_TONG_CARD && has_rule(GameConstants_GY.GAME_RULE_WU_GU_JI)) {
						continue;
					}
					// 幺鸡
					if (GRR._weave_items[player][j].center_card == GameConstants_GY.YJ_CARD) {
						continue;
					}

					if (GameConstants_GY.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
						player_11_detail[player][3] += 3;
						s += 3;
						player_all_ji_card[player] += 3;
					} else {
						player_11_detail[player][3] += 4;
						s += 4;
						player_all_ji_card[player] += 4;
					}
				}
			}

			String sign = "+";
			if (_playerStatus[player]._hu_card_count == 0) {
				s *= -1;
				sign = "-";
			}

			if (player_11_detail[player][0] > 0)
				GRR._result_des[player] = "星期鸡          " + sign + player_11_detail[player][0] + "_" + GRR._result_des[player];
			if (player_11_detail[player][1] > 0)
				if (jin_ji[0]) {
					GRR._result_des[player] = "金鸡          " + sign + player_11_detail[player][1] + "_" + GRR._result_des[player];
				} else {
					GRR._result_des[player] = "普通鸡          " + sign + player_11_detail[player][1] + "_" + GRR._result_des[player];
				}
			if (player_11_detail[player][2] > 0)
				if (jin_ji[1]) {
					GRR._result_des[player] = "金乌骨          " + sign + player_11_detail[player][2] + "_" + GRR._result_des[player];
				} else {
					GRR._result_des[player] = "乌骨鸡          " + sign + player_11_detail[player][2] + "_" + GRR._result_des[player];
				}
			if (player_11_detail[player][3] > 0)
				GRR._result_des[player] = "翻鸡牌          " + sign + player_11_detail[player][3] + "_" + GRR._result_des[player];

			if ((shao[player] && s > 0) || s == 0)
				continue;

			for (int k = 0; k < getTablePlayerNumber(); k++) {
				if (player == k) {
					continue;
				}
				if (s < 0 && _playerStatus[k]._hu_card_count == 0)
					continue;

				GRR._game_score[k] -= s;
				GRR._game_score[player] += s;
			}
		}

		for (int p = 0; p < getTablePlayerNumber(); p++)
			player_ji_score[p] = (int) GRR._game_score[p];
	}

	/**
	 * 责任鸡算分
	 */
	public void process_reponsibility_ji_fen() {
		for (int count = 0; count < 2; count++) {
			Tuple<Integer, Integer> responsibility = responsibility_ji[count];

			if (responsibility.getLeft() == -1 || responsibility.getRight() == -1)
				continue;

			int ting_left = _playerStatus[responsibility.getLeft()]._hu_card_count;
			int ting_right = _playerStatus[responsibility.getRight()]._hu_card_count;

			Tuple<Integer, Integer> check_ting = new Tuple<>(0, 0);
			if (ting_left > 0)
				check_ting.setLeft(1);
			if (ting_right > 0)
				check_ting.setRight(1);

			if (check_ting.getLeft() == 0 && check_ting.getRight() == 0)
				continue;

			int base = count == 0 ? 1 : 2;
			String str = count == 0 ? "责任鸡          " : "责任乌骨        ";
			if (check_ting.getRight() >= check_ting.getLeft()) {
				GRR._game_score[responsibility_ji[count].getLeft()] -= base;
				GRR._result_des[responsibility_ji[count].getLeft()] = str + "-" + base + "_" + GRR._result_des[responsibility_ji[count].getLeft()];
				GRR._game_score[responsibility_ji[count].getRight()] += base;
				GRR._result_des[responsibility_ji[count].getRight()] = str + "+" + base + "_" + GRR._result_des[responsibility_ji[count].getRight()];
			} else {
				GRR._game_score[responsibility_ji[count].getLeft()] += base;
				GRR._result_des[responsibility_ji[count].getLeft()] = str + "+" + base + "_" + GRR._result_des[responsibility_ji[count].getLeft()];
				GRR._game_score[responsibility_ji[count].getRight()] -= base;
				GRR._result_des[responsibility_ji[count].getRight()] = str + "-" + base + "_" + GRR._result_des[responsibility_ji[count].getRight()];
			}

			show_responsibility_ji[count].setLeft(responsibility.getLeft());
			show_responsibility_ji[count].setRight(responsibility.getRight());
			// 算了一次就请掉哈，不要重复计算撤
			responsibility.setLeft(-1);
			responsibility.setRight(-1);
		}

		for (int p = 0; p < getTablePlayerNumber(); p++)
			player_ji_score[p] = (int) GRR._game_score[p];
	}

	/**
	 * 初始化plyaer_11_detail 的第十一位
	 */
	private void init_11_player_detail() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			ChiHuRight chr = GRR._chi_hu_rights[i];

			if (GRR._chi_hu_rights[i].is_valid()) {
				if (!chr.opr_and(GameConstants_GY.CHR_ZI_MO).is_empty())
					player_11_detail[i][10] = 1;
				if (!chr.opr_and(GameConstants_GY.CHR_SHU_FAN).is_empty())
					player_11_detail[i][10] = 2;
				if (!chr.opr_and(GameConstants_GY.CHR_QING_GANG_HU).is_empty())
					player_11_detail[i][10] = 3;
				if (!chr.opr_and(GameConstants_GY.CHR_GNAG_KAI).is_empty())
					player_11_detail[i][10] = 4;
				if (!chr.opr_and(GameConstants_GY.CHR_RE_PAO).is_empty())
					player_11_detail[i][10] = 6;
			} else {
				if (!chr.opr_and(GameConstants_GY.CHR_FANG_PAO).is_empty())
					player_11_detail[i][10] = 5;
			}
		}
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
			show_continue_banker = true;
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
			show_continue_banker = false;
		}

		float lChiHuScore = get_hu_fen(chr);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			chr.opr_or(GameConstants.CHR_ZI_MO);
			int add_fen = has_rule(GameConstants_GY.GAME_RULE_TONG_3) ? 3 : 1;
			if (lChiHuScore > 0 && add_fen == 1)
				add_fen = 0;
			float s = lChiHuScore + add_fen;
			hu_base_fen = s;

			// 特殊处理：一扣二加1分
			if (add_fen == 1 && has_rule(GameConstants_GY.GAME_RULE_1_KOU_2) && lChiHuScore == 0) {
				s += 1;
				GRR._result_des[seat_index] = "自摸            +1" + "_" + GRR._result_des[seat_index];
			}

			// 天听软报额外收取10分
			if (_playerStatus[seat_index].is_bao_ting()) {
				s += 10;
				GRR._result_des[seat_index] = "天听软报        +" + 10 + "_" + GRR._result_des[seat_index];
			}

			// 杠开多加一分
			if (!chr.opr_and(GameConstants_GY.CHR_GNAG_KAI).is_empty()) {
				s += 1;
				GRR._result_des[seat_index] = "杠上花          +1" + "_" + GRR._result_des[seat_index];
			}

			// 连庄玩法 加连庄数
			if (has_rule(GameConstants_GY.GAME_RULE_CONTINUE_BANKER)) {
				if (old_banker == _cur_banker) {
					s += continue_banker_count;
					if (continue_banker_count > 0) {
						GRR._result_des[seat_index] = "连庄            +" + continue_banker_count + "_" + GRR._result_des[seat_index];
					}
					continue_banker_count++;
				}
			}

			// 天胡算硬报，20分
			if (!chr.opr_and(GameConstants_GY.CHR_TIAN_HU).is_empty()) {
				s += 20;
				GRR._result_des[seat_index] = "天听硬报        +" + 20 + "_" + GRR._result_des[seat_index];
			} else if (player_mo_first[seat_index]) {
				chr.opr_or(GameConstants_GY.CHR_TIAN_HU);
				s += 20;
				GRR._result_des[seat_index] = "天听硬报        +" + 20 + "_" + GRR._result_des[seat_index];
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				// 添加动作)
				GRR._game_score[i] -= s;
				hu_type_socre[i] -= s;
				GRR._game_score[seat_index] += s;
				hu_type_socre[seat_index] += s;

				// 连庄玩法 加连庄数
				if (has_rule(GameConstants_GY.GAME_RULE_CONTINUE_BANKER)) {
					if (old_banker != _cur_banker && i == old_banker) {
						s += continue_banker_count;
						GRR._game_score[i] -= continue_banker_count;
						hu_type_socre[i] -= continue_banker_count;
						GRR._game_score[seat_index] += continue_banker_count;
						hu_type_socre[seat_index] += continue_banker_count;
						if (continue_banker_count > 0) {
							GRR._result_des[i] = "连庄            -" + continue_banker_count + "_" + GRR._result_des[i];
						}
						continue_banker_count = 1;
					}
				}
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			chr.opr_or(GameConstants.CHR_SHU_FAN);
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

			int add_fen = has_rule(GameConstants_GY.GAME_RULE_TONG_3) ? 3 : 1;
			if (lChiHuScore > 0 && add_fen == 1)
				add_fen = 0;

			float s = lChiHuScore + add_fen;
			hu_base_fen = s;

			// 天听软报额外收取10分
			if (_playerStatus[seat_index].is_bao_ting())
				s += 10;

			// 热炮的杠分, 一扣二玩法时不出
			// if
			// (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_RE_PAO).is_empty()
			// && !has_rule(GameConstants_GY.GAME_RULE_1_KOU_2)) {
			// if (_handler_gang.get_type() == GameConstants.GANG_TYPE_AN_GANG)
			// {
			// s += 9;
			// } else if (_handler_gang.get_type() ==
			// GameConstants.GANG_TYPE_JIE_GANG) {
			// s += 6;
			// } else if (_handler_gang.get_type() ==
			// GameConstants.GANG_TYPE_ADD_GANG) {
			// s += 9;
			// }
			// }

			// 连庄玩法 加连庄数
			if (has_rule(GameConstants_GY.GAME_RULE_CONTINUE_BANKER)) {
				if (seat_index == old_banker) {
					s += continue_banker_count;
					if (continue_banker_count > 0)
						GRR._result_des[seat_index] = "连庄            +" + continue_banker_count + "_" + GRR._result_des[seat_index];
					continue_banker_count++;
				}
				if (provide_index == old_banker) {
					s += continue_banker_count;
					if (continue_banker_count > 0)
						GRR._result_des[provide_index] = "连庄            -" + continue_banker_count + "_" + GRR._result_des[provide_index];
					continue_banker_count = 1;
				}
			}

			// 地胡 硬报加20分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_DI_HU).is_empty())
				s += 20;

			// 热炮通三多出3分,其他热炮多加一分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_RE_PAO).is_empty())
				s += has_rule(GameConstants_GY.GAME_RULE_TONG_3) ? 3 : 1;

			// 抢杠胡多出9分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_GY.CHR_QING_GANG_HU).is_empty()) {
				s += 9;
				chr.opr_or(GameConstants_GY.CHR_QING_GANG_HU);
				GRR._result_des[seat_index] = "抢杠胡          +9" + "_" + GRR._result_des[seat_index];
			}

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			hu_type_socre[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == _cur_banker) {
				_player_result.qiang[_cur_banker] = continue_banker_count;
			} else {
				_player_result.qiang[i] = 0;
			}
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		init_11_player_detail();
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

		if (true)
			return;
		// for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
		// GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		// }
		//
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		// GRR._player_niao_count[i] = 0;
		// for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
		// GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
		// }
		// }
		//
		// GRR._show_bird_effect = show;
		// GRR._count_niao = getCsDingNiaoNum();
		//
		// if (GRR._count_niao > GameConstants.ZHANIAO_0) {
		// if (card == GameConstants.INVALID_VALUE) {
		// int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		// _logic.switch_to_cards_index(_repertory_card, _all_card_len -
		// GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
		// GRR._left_card_count -= GRR._count_niao;
		// _logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
		// } else {
		// for (int i = 0; i < GRR._count_niao; i++) {
		// GRR._cards_data_niao[i] = card;
		// }
		// }
		// }
		// // 中鸟个数
		// GRR._count_pick_niao =
		// _logic.get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		//
		// for (int i = 0; i < GRR._count_niao; i++) {
		// int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
		// int seat = 0;
		// seat = (seat_index + (nValue - 1) % 4) % 4;
		// GRR._player_niao_cards[seat][GRR._player_niao_count[seat]] =
		// GRR._cards_data_niao[i];
		// GRR._player_niao_count[seat]++;
		// }
		//
		// // GRR._count_pick_niao = 0;
		// // for (int i = 0; i < getTablePlayerNumber(); i++) {
		// // for (int j = 0; j < GRR._player_niao_count[i]; j++) {
		// // if (seat_index == i) {
		// // GRR._count_pick_niao++;
		// // GRR._player_niao_cards[i][j] =
		// // this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
		// // 胡牌的鸟生效
		// // } else {
		// // GRR._player_niao_cards[i][j] =
		// // this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
		// // 胡牌的鸟生效
		// // }
		// // }
		// // }
		//
		// int[] player_niao_count = new int[GameConstants.GAME_PLAYER];
		// int[][] player_niao_cards = new
		// int[GameConstants.GAME_PLAYER][GameConstants.MAX_NIAO_CARD];
		// for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
		// player_niao_count[i] = 0;
		// for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
		// player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
		// }
		// }
		//
		// GRR._count_pick_niao = 0;
		// if (has_rule(GameConstants_HanShouWang.GAME_RULE_HU_JI_JIANG_JI)) {
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(hu_card, true);
		// player_niao_count[seat_index]++;
		// if (hu_card == GameConstants_HanShouWang.HZ_MAGIC_CARD) {
		// GRR._count_pick_niao = 10;
		// } else {
		// GRR._count_pick_niao = _logic.get_card_value(hu_card);
		// }
		// } else {
		// for (int i = 0; i < getTablePlayerNumber(); i++) {
		// for (int j = 0; j < GRR._player_niao_count[i]; j++) {
		// if (seat_index == i) {
		// GRR._count_pick_niao++;
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(GRR._player_niao_cards[i][j], true);//
		// 胡牌的鸟生效
		// } else {
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(GRR._player_niao_cards[i][j], false);//
		// 胡牌的鸟生效
		// }
		// player_niao_count[seat_index]++;
		// }
		// }
		// }
		// GRR._player_niao_cards = player_niao_cards;
		// GRR._player_niao_count = player_niao_count;
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
		if (GRR == null)
			GRR = game_end_GRR;

		int chrTypes;
		long type = 0;
		boolean qiang_gang_hu = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			if (_playerStatus[player]._hu_card_count > 0) {
				result.append("叫牌");
			} else {
				result.append("未叫牌");
			}
			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean ping_hu = true;
			boolean have_qing = false;

			int hu_base_fen = (int) this.hu_base_fen;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_GY.CHR_DA_DUI_ZI) {
						result.append(" 大对子" + hu_base_fen);
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_LONG_QI_DUI) {
						result.append(" 龙七对" + hu_base_fen);
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_XIAO_QI_DUI) {
						result.append(" 七小对" + hu_base_fen);
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_QING_DA_DUI) {
						result.append(" 清大对" + hu_base_fen);
						have_qing = true;
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_SHUANG_QING) {
						result.append(" 双清" + hu_base_fen);
						have_qing = true;
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_DAN_DIAO) {
						result.append(" 清一色" + hu_base_fen);
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_QING_QI_DUI) {
						result.append(" 清七对" + hu_base_fen);
						have_qing = true;
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_QING_LONG_BEI) {
						result.append(" 青龙背" + hu_base_fen);
						have_qing = true;
						ping_hu = false;
					}
					if (type == GameConstants_GY.CHR_QING_YI_SE && !have_qing) {
						result.append(" 清一色" + hu_base_fen);
						ping_hu = false;
					}
					if (ping_hu)
						result.append(" 平胡" + hu_base_fen);
				}
			}
			// if (GRR._chi_hu_rights[player].is_valid()) {
			//
			// if (show_continue_banker && continue_banker_count > 1)
			// result.append(" 连庄+").append(continue_banker_count);
			//
			// if (_playerStatus[player].is_bao_ting())
			// result.append(" 天听");
			//
			// }

			// if (chong_feng_ji_seat_yj == player)
			// result.append(" 幺鸡冲锋鸡");
			// if (chong_feng_ji_seat_bt == player)
			// result.append(" 八筒冲锋鸡");
			// if (player_all_ji_card[player] > 0)
			// result.append(" 鸡牌x").append(player_all_ji_card[player]);

			// for (int count = 0; count < 2; count++) {
			// Tuple<Integer, Integer> responsibility =
			// show_responsibility_ji[count];
			// if (responsibility.getRight() == player)
			// result.append(" 责任鸡 ");
			// if (responsibility.getLeft() == player)
			// result.append(" 责任鸡 ");
			// }
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

			// if (an_gang > 0) {
			// result.append(" 暗杠X" + an_gang);
			// }
			// if (ming_gang > 0) {
			// result.append(" 明杠X" + ming_gang);
			// }
			// if (fang_gang > 0) {
			// result.append(" 放杠X" + fang_gang);
			// }
			// if (jie_gang > 0) {
			// result.append(" 接杠X" + jie_gang);
			// }

			// GRR._result_des[player] = GRR._result_des[player].length() > 0
			// ? GRR._result_des[player].substring(0,
			// GRR._result_des[player].length() - 1)
			// : GRR._result_des[player];

			// 传给客户端数据格式：平胡13 清一色13@鸡牌 +1_乌骨鸡 +3
			GRR._result_des[player] = result.toString() + "@" + GRR._result_des[player];
		}
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN)) { // liuyan
																			// 2017/7/10
			int effect_count = chr.type_count;
			long effect_indexs[] = new long[effect_count];
			for (int i = 0; i < effect_count; i++) {
				if (chr.type_list[i] == GameConstants.CHR_SHU_FAN) {
					effect_indexs[i] = GameConstants.CHR_HU;
				} else {
					effect_indexs[i] = chr.type_list[i];
				}

			}

			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1, GameConstants.INVALID_SEAT);
		} else {
			// 效果
			this.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1, GameConstants.INVALID_SEAT);
		}

		// 手牌删掉
		this.operate_player_cards(seat_index, 0, null, 0, null);

		if (rm) {
			// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
			GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
		}

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05, 0x18 };
		int[] cards_of_player1 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x04, 0x04, 0x04, 0x06, 0x06, 0x07, 0x07 };
		int[] cards_of_player2 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x04, 0x04, 0x04, 0x06, 0x06, 0x07, 0x07 };
		int[] cards_of_player3 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x04, 0x04, 0x04, 0x06, 0x06, 0x07, 0x07 };

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
			} else if (this.getTablePlayerNumber() == 2) {
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

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

}
