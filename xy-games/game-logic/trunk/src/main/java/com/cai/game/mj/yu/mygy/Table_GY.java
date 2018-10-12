package com.cai.game.mj.yu.mygy;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.TimeUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;
import com.cai.util.Tuple;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
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

	private static final long serialVersionUID = -2750784307432844045L;

	public int chong_feng_ji_seat_yj; // 幺鸡冲锋鸡

	public int chong_feng_ji_seat_bt; // 八筒冲锋鸡

	public int[][] out_ji_pai; // 每个人打出的鸡牌数量
	public int[] out_ji_pai_count; // 每个人打出的鸡牌数量

	public int[] _ji_card_index; // 指定鸡牌

	public int _ji_card_count; // 指定 鸡牌数量

	public Tuple<Integer, Integer>[][] responsibility_ji; // 责任鸡
	public int responsibility_ji_count;

	public Tuple<Integer, Integer>[] show_responsibility_ji; // 责任鸡

	public boolean[] jin_ji; // 本局是否有金鸡

	public boolean[] player_mo_first;

	public MJHandlerOutCardBaoTing_GY _handler_out_card_bao_ting;

	public HandlerSelectMagic_GY handler_select_magic;
	public MJHandlerYingBao handler_ying_bao;
	public MJHandlerPiao_MYGY _handler_piao;

	public boolean[] shao; // 烧鸡烧杠

	public boolean show_continue_banker;

	public int[][] show_index_score;
	public int[][] show_index_card;
	public int[][] player_GangScore_type;

	public float[] hu_type_socre;

	public int[] player_ji_score;

	public int[] player_duan;

	public MJHandlerFinish_GY handler_finish;

	public GameRoundRecord game_end_GRR;

	public int[] zi_da;

	public float hu_base_fen;

	public RoomResponse.Builder roomResponse;

	public int old_banker;

	public int[] shang_xia_ji;
	public int fan_pao_zhuan_fen;

	public boolean[] player_ying_bao;
	public boolean[] player_ruan_bao;

	public int[] end_dou_score;
	public int[] end_ji_score;
	public int[] end_hu_score;
	public int[] end_mai_score;

	public int[] player_gu_mai;
	public int[] start_hu_right;
	public int[] start_hu_score;
	public int[] banker_continue_count;

	public int re_pao_gang_type;

	public Table_GY() {
		super(MJType.GAME_TYPE_GUI_YANG);
		_ji_card_count = 0;
		_ji_card_index = new int[GameConstants_MYGY.MAX_COUNT];
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
		if (card > GameConstants_MYGY.CARD_ESPECIAL_TYPE_TING && card < GameConstants_MYGY.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_MYGY.CARD_ESPECIAL_TYPE_TING;
			return card;
		}
		if (card > GameConstants_MYGY.CARD_ESPECIAL_TYPE_LIANG_ZHANG) {
			card -= GameConstants_MYGY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
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
		handler_ying_bao = new MJHandlerYingBao();
		_handler_piao = new MJHandlerPiao_MYGY();

		end_hu_score = new int[GameConstants_MYGY.GAME_PLAYER];
		end_ji_score = new int[GameConstants_MYGY.GAME_PLAYER];
		end_dou_score = new int[GameConstants_MYGY.GAME_PLAYER];
		end_mai_score = new int[GameConstants_MYGY.GAME_PLAYER];
		player_gu_mai = new int[getTablePlayerNumber()];
		banker_continue_count = new int[getTablePlayerNumber()];
	}

	public void cal_banker_contibue_banker(int old_banker, int new_banker) {
		int count = banker_continue_count[new_banker];
		Arrays.fill(banker_continue_count, 0);
		if (old_banker == new_banker) {
			banker_continue_count[new_banker] += count + 1;
		} else {
			banker_continue_count[new_banker] = 1;
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
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

	public boolean exe_ying_bao(int type, GangCardResult m_gangCardResult) {
		this.set_handler(this.handler_ying_bao);
		handler_ying_bao.reset(type, m_gangCardResult);
		_handler.exe(this);

		return true;
	}

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int seat_index, int send_card_data) {
		// 设置变量
		int cbActionMask = GameConstants_MYGY.WIK_NULL;
		if (_playerStatus[seat_index].is_bao_ting()) {
			return cbActionMask;
		}

		// 手上杠牌
		for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants_MYGY.WIK_GANG;

				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants_MYGY.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind != GameConstants_MYGY.WIK_PENG) {
					continue;
				}
				for (int j = 0; j < GameConstants_MYGY.MAX_INDEX; j++) {
					if (cards_index[j] != 1) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
						continue;
					}

					if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
						cbActionMask |= GameConstants_MYGY.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						if (WeaveItem[i].center_card == _send_card_data) {
							gangCardResult.type[index] = GameConstants_MYGY.GANG_TYPE_ADD_GANG;
						} else {
							gangCardResult.type[index] = GameConstants_MYGY.GAME_TYPE_TONG_GANG;
						}

						break;
					}
				}
			}
		}

		return cbActionMask;
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants_MYGY.GS_MJ_XIAOHU != _game_status && GameConstants_MYGY.GS_MJ_FREE != _game_status
				&& GameConstants_MYGY.GS_MJ_WAIT != _game_status) {
			return false;
		}

		if (is_cancel) {//
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

		_player_ready[seat_index] = 1;

		boolean nt = true;
		if (this.get_players()[seat_index].getAccount_id() == this.getRoom_owner_account_id()) {
			nt = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
		roomResponse.setOperatePlayer(seat_index);
		send_response_to_room(roomResponse);
		if (!is_mj_type(GameConstants_MYGY.GAME_TYPE_DT_MJ_HUNAN_CHEN_ZHOU) && !is_mj_type(GameConstants_MYGY.GAME_TYPE_MJ_JING_DE_ZHEN)
				&& !is_mj_type(GameConstants_MYGY.GAME_TYPE_MJ_NING_XIANG)) {
			// 跑分
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}
			// 闹庄
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				_player_result.nao[i] = 0;
				_player_result.ziba[i] = 0;
				_player_result.duanmen[i] = 0;
				_player_result.haspiao[i] = 0;
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

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (this.get_players()[i] == null) {
				_player_ready[i] = 0;
			}

			if (_player_ready[i] == 0) {
				this.refresh_room_redis_data(GameConstants_MYGY.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants_MYGY.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
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
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setJiaoDiZhu(player_gu_mai[i]);

			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
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

		int action = GameConstants_MYGY.WIK_NULL;

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
						GameConstants_MYGY.HU_CARD_TYPE_QIANG_GANG, i);

				if (player_duan[i] != -1) {
					for (int c = 0; c < GameConstants_MYGY.MAX_INDEX; c++) {
						if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]) {
							action = GameConstants_MYGY.WIK_NULL;
							break;
						}
					}
				}
				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants_MYGY.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
		}

		if (bAroseAction == true) {
			_provide_player = seat_index;// 谁打的牌
			_provide_card = card;// 打的什么牌
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants_MYGY.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
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

		int action = GameConstants_MYGY.WIK_NULL;

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
			for (int x = 0; x < GameConstants_MYGY.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0 && !_playerStatus[i].is_bao_ting()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				// int cards[] = new int[GameConstants_MYGY.MAX_COUNT];
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
						playerStatus.add_action(GameConstants_MYGY.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_MYGY.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_MYGY.HU_CARD_TYPE_JIE_PAO;
					if (type == GameConstants_MYGY.GANG_TYPE_AN_GANG || type == GameConstants_MYGY.GANG_TYPE_ADD_GANG
							|| type == GameConstants_MYGY.GANG_TYPE_JIE_GANG || type == GameConstants_MYGY.GAME_TYPE_TONG_GANG) {
						card_type = GameConstants_MYGY.HU_CARD_TYPE_RE_PAO;
					} else
					// 此处偷个懒，若出牌得人已报听牌，其他人没有通行证的情况下也是可以胡的
					if (_playerStatus[seat_index].is_bao_ting() || _playerStatus[i].is_bao_ting())
						card_type = GameConstants_MYGY.HU_CARD_TYPE_SHA_BAO;

					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (player_duan[i] != -1) {
						for (int c = 0; c < GameConstants_MYGY.MAX_INDEX; c++) {
							if (_logic.is_magic_index(c)) {
								continue;
							}
							if (GRR._cards_index[i][c] > 0 && _logic.get_card_color(_logic.switch_to_card_data(c)) == player_duan[i]) {
								action = GameConstants_MYGY.WIK_NULL;
								break;
							}
						}
					}
					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_MYGY.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;
			_current_player = GameConstants_MYGY.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_MYGY.GAMR_RULE_2_REN_JU)) {
			return 2;
		}
		if (has_rule(GameConstants_MYGY.GAMR_RULE_3_REN_JU)) {
			return 3;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) {
			return 2;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) {
			return 3;
		}
		return 4;
	}

	/**
	 * 结束调度
	 * 
	 * @return
	 */
	@Override
	public boolean exe_finish(int reason) {
		this._end_reason = reason;
		if (_end_reason == GameConstants_MYGY.Game_End_NORMAL || _end_reason == GameConstants_MYGY.Game_End_DRAW
				|| _end_reason == GameConstants_MYGY.Game_End_ROUND_OVER) {
			cost_dou = 0;
		}

		this.set_handler(this.handler_finish);
		this.handler_finish.exe(this);

		return true;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;
		if (reason == GameConstants_MYGY.Game_End_RELEASE_PLAY) {
			process_chi_hu_player_operate(GameConstants_MYGY.INVALID_SEAT, 0, true);
		}

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
				if (reason == GameConstants_MYGY.Game_End_NORMAL)
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < getTablePlayerNumber(); k++) {
							if (player_GangScore_type[i][j] == GameConstants_MYGY.GANG_TYPE_AN_GANG)
								if (i == k) {
									show_index_score[k][GameConstants_MYGY.INDEX_14_DOU_MEN_DOU] += GRR._gang_score[i].scores[j][k]
											/ (getTablePlayerNumber() - 1);
								}
							if (player_GangScore_type[i][j] == GameConstants_MYGY.GANG_TYPE_JIE_GANG)
								show_index_score[k][GameConstants_MYGY.INDEX_13_DOU_MING_DOU] += GRR._gang_score[i].scores[j][k];
							if (player_GangScore_type[i][j] == GameConstants_MYGY.GANG_TYPE_ADD_GANG) {
								if (i == k) {
									show_index_score[k][GameConstants_MYGY.INDEX_15_DOU_ZW_DOU] += GRR._gang_score[i].scores[j][k]
											/ (getTablePlayerNumber() - 1);
								}
							}
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 鏉犵墝锛屾瘡涓汉鐨勫垎鏁�
						}
					}

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}
			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants_MYGY.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < GameConstants_MYGY.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			for (int i = 0; i < GameConstants_MYGY.MAX_NIAO_CARD && i < GRR._count_niao_fei; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao_fei[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao + GRR._count_pick_niao_fei);// 中鸟个数

			for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
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
				for (int j = 0; j < GameConstants_MYGY.MAX_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					game_end.addHuCardData(GRR._chi_hu_card[i][h]);
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants_MYGY.MAX_RIGHT_COUNT];

			set_result_describe();

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				end_dou_score[i] += lGangScore[i];
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];

				Int32ArrayResponse.Builder items = Int32ArrayResponse.newBuilder();
				for (int s = 0; s < GameConstants_MYGY.INDEX_COUNT; s++) {
					items.addItem(show_index_score[i][s]);
				}
				for (int s = 0; s < GameConstants_MYGY.INDEX_COUNT; s++) {
					items.addItem(show_index_card[i][s]);
				}

				game_end.addLostFanShu(items);
				game_end.addGangScore(lGangScore[i]);
				game_end.addPao((int) hu_type_socre[i]);
				game_end.addJettonScore(player_ji_score[i]);

				end_hu_score[i] += hu_type_socre[i];
				end_ji_score[i] += player_ji_score[i];
			}

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
				game_end.addStartHuRight(start_hu_right[i]);
				game_end.addStartHuScore(start_hu_score[i]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addGangScore(lGangScore[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				// Int32ArrayResponse.Builder lfs =
				// Int32ArrayResponse.newBuilder();
				// for (int j = 0; j < getTablePlayerNumber(); j++) {
				// lfs.addItem(GRR._lost_fan_shu[i][j]);
				// }
				//
				// game_end.addLostFanShu(lfs);

			}

		}

		for (int player = 0; player < getTablePlayerNumber() && GRR != null; player++) {
			GRR._result_des[player] = "";
		}
		boolean end = false;
		if (reason == GameConstants_MYGY.Game_End_NORMAL || reason == GameConstants_MYGY.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants_MYGY.Game_End_RELEASE_PLAY || reason == GameConstants_MYGY.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants_MYGY.Game_End_RELEASE_RESULT || reason == GameConstants_MYGY.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants_MYGY.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants_MYGY.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants_MYGY.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);
		this.roomResponse = roomResponse;

		record_game_round(game_end);

		if (reason == GameConstants_MYGY.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants_MYGY.Game_End_RELEASE_WAIT_TIME_OUT) {
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
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < GameConstants_MYGY.GAME_PLAYER; j++) {
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

		for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
			player_result.addGameScore(_player_result.game_score[i]);
			player_result.addWinOrder(_player_result.win_order[i]);

			Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants_MYGY.GAME_PLAYER; j++) {
				lfs.addItem(_player_result.lost_fan_shu[i][j]);
			}

			player_result.addLostFanShu(lfs);

			// if(is_mj_type(MJGameConstants_MYGY.GAME_TYPE_ZZ) ||
			// is_mj_type(MJGameConstants_MYGY.GAME_TYPE_HZ)||
			// is_mj_type(MJGameConstants_MYGY.GAME_TYPE_SHUANGGUI)){
			player_result.addZiMoCount(_player_result.zi_mo_count[i]);
			player_result.addJiePaoCount(_player_result.jie_pao_count[i]);
			player_result.addDianPaoCount(_player_result.dian_pao_count[i]);
			player_result.addAnGangCount(_player_result.an_gang_count[i]);
			player_result.addMingGangCount(_player_result.ming_gang_count[i]);
			player_result.addMenQingCount(_player_result.men_qing[i]);
			player_result.addHaiDiCount(_player_result.hai_di[i]);
			// }else if(is_mj_type(MJGameConstants_MYGY.GAME_TYPE_CS)||
			// is_mj_type(MJGameConstants_MYGY.GAME_TYPE_ZHUZHOU)){
			player_result.addDaHuZiMo(end_dou_score[i]);
			player_result.addDaHuJiePao(end_ji_score[i]);
			player_result.addDaHuDianPao(end_hu_score[i]);
			player_result.addXiaoHuZiMo(end_mai_score[i]);
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

	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		// && GameConstants_MYGY.GS_MJ_WAIT != _game_statu
		if ((GameConstants_MYGY.GS_MJ_FREE != _game_status) && this.get_players()[seat_index] != null) {
			// this.send_play_data(seat_index);

			if (this._handler != null)
				this._handler.handler_player_be_in_room(this, seat_index);

			be_in_room_trustee_match(seat_index);
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
	public boolean reset_init_data() {
		super.reset_init_data();
		if (commonGameRuleProtos != null) {
			GRR._room_info.setNewRules(commonGameRuleProtos);
		}
		return true;
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

		_game_status = GameConstants_MYGY.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_FOUR_PLAYER) == 1) {
			_repertory_card = new int[GameConstants_MYGY.CARD_COUNT_HU_NAN];
			shuffle(_repertory_card, GameConstants_MYGY.CARD_DATA_WAN_TIAO_TONG);
		} else {
			if (has_rule(GameConstants_MYGY.GAME_RULE_ER_FANG_PAI)) {
				_repertory_card = new int[GameConstants_MYGY.CARD_DATA_TIAO_TONG.length];
				shuffle(_repertory_card, GameConstants_MYGY.CARD_DATA_TIAO_TONG);
			} else {
				_repertory_card = new int[GameConstants_MYGY.CARD_COUNT_HU_NAN];
				shuffle(_repertory_card, GameConstants_MYGY.CARD_DATA_WAN_TIAO_TONG);
			}
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

	public boolean on_game_start_real() {
		if (_cur_round == 1) {
			old_banker = -1;
		}
		cal_banker_contibue_banker(old_banker, _cur_banker);

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_MYGY.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_MYGY.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_MYGY.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_MYGY.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, _playerStatus[i]._hu_out_cards_fan[0], GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		old_banker = _current_player;
		exe_dispatch_card(_current_player, GameConstants_MYGY.DispatchCard_Type_Tian_Hu, GameConstants_MYGY.DELAY_SEND_CARD_DELAY);

		return true;
	}

	@Override
	protected boolean on_game_start() {
		_logic.clean_magic_cards();
		this.clean_ji_cards();
		this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_MYGY.YJ_CARD));
		if (has_rule(GameConstants_MYGY.GAME_RULE_WU_GU_JI))
			this.add_ji_card_index(_logic.switch_to_card_index(GameConstants_MYGY.BA_TONG_CARD));

		_game_status = GameConstants_MYGY.GS_MJ_PLAY;

		chong_feng_ji_seat_bt = -1;
		chong_feng_ji_seat_yj = -1;
		out_ji_pai = new int[getTablePlayerNumber()][];

		out_ji_pai_count = new int[getTablePlayerNumber()];
		jin_ji = new boolean[2]; // 只有两个默认的鸡产生金鸡
		responsibility_ji = new Tuple[getTablePlayerNumber()][2];
		responsibility_ji_count = 0;
		show_responsibility_ji = new Tuple[2];
		player_mo_first = new boolean[getTablePlayerNumber()];
		shao = new boolean[getTablePlayerNumber()];
		shang_xia_ji = new int[3];
		show_index_score = new int[getTablePlayerNumber()][GameConstants_MYGY.INDEX_COUNT];
		show_index_card = new int[getTablePlayerNumber()][GameConstants_MYGY.INDEX_COUNT];
		hu_type_socre = new float[getTablePlayerNumber()];
		player_ji_score = new int[getTablePlayerNumber()];
		player_duan = new int[getTablePlayerNumber()];
		zi_da = new int[getTablePlayerNumber()];
		player_GangScore_type = new int[getTablePlayerNumber()][4];
		hu_base_fen = 0;
		player_ying_bao = new boolean[getTablePlayerNumber()];
		player_ruan_bao = new boolean[getTablePlayerNumber()];
		fan_pao_zhuan_fen = 0;
		player_gu_mai = new int[getTablePlayerNumber()];
		start_hu_right = new int[getTablePlayerNumber()];
		start_hu_score = new int[getTablePlayerNumber()];
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			GRR._result_des[player] = "";
			out_ji_pai[player] = new int[28];
			player_mo_first[player] = true;
			player_duan[player] = -1;
			for (int i = 0; i < 2; i++) {
				responsibility_ji[player][i] = new Tuple<Integer, Integer>(-1, -1);
			}
		}
		if (has_rule(GameConstants_MYGY.GAME_RULE_LAI_ZI_JI)) {
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_MYGY.YI_TONG_CARD));
		}

		if (has_rule(GameConstants_MYGY.GAME_RULE_GU_MAI)) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		} else {
			return on_game_start_real();
		}
	}

	public boolean check_ying_bao() {
		boolean have_bao = false;
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == _current_player) {
				continue;
			}
			boolean _seat_index_hand_have_duan_card = false;
			for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++)
				if (GRR._cards_index[p][i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[p]) {
					_seat_index_hand_have_duan_card = true;
					break;
				}
			if (_seat_index_hand_have_duan_card) {
				continue;
			}
			if (_playerStatus[p]._hu_card_count > 0) {
				// 添加动作
				_playerStatus[p].add_action(GameConstants_MYGY.WIK_YING_BAO_XIAN);
				have_bao = true;
			}
		}
		return have_bao;
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

		// TODO 向其他人发送组合牌数据的时候，暗杠不能发center_card数据
		if (weave_count > 0) {
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				if (weaveitems[j].weave_kind == GameConstants_MYGY.WIK_GANG && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					weaveItem_item.setCenterCard(weaveitems[j].center_card);
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}
		send_response_to_other(seat_index, roomResponse);

		// TODO 向自己发送手牌数据，自己的数据里，暗杠是能看到的
		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			if (_logic.is_magic_card(cards[j])) {
				roomResponse.addCardData(cards[j] + GameConstants_MYGY.CARD_ESPECIAL_TYPE_LAI_ZI);
			} else {
				roomResponse.addCardData(cards[j]);
			}
		}

		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		int ting_count = _playerStatus[seat_index]._hu_out_card_count;

		roomResponse.setOutCardCount(ting_count);

		for (int i = 0; i < ting_count; i++) {
			int ting_card_cout = _playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			int out_card = _playerStatus[seat_index]._hu_out_card_ting[i];

			roomResponse.addOutCardTing(out_card + GameConstants_MYGY.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int card = _playerStatus[seat_index]._hu_out_cards[i][j];
				if (_logic.is_magic_card(card))
					card += GameConstants_MYGY.CARD_ESPECIAL_TYPE_LAI_ZI;
				int_array.addItem(card);
				roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		send_response_to_player(seat_index, roomResponse);

		GRR.add_room_response(roomResponse);

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
			for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
				if (_logic.is_magic_index(i)) {
					continue;
				}
				if (cards_index[i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[seat_index]) {
					add_flag = true;
					break;
				}
			}
		}

		// 转换扑克
		int cbPosition = 0;
		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			for (int i = 0; i < cards_index[_logic.get_all_magic_card_index()[m]]; i++) {
				if (add_flag && _logic.get_card_color(_logic.switch_to_card_data(_logic.get_all_magic_card_index()[m])) != player_duan[seat_index]) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(_logic.get_all_magic_card_index()[m])
							+ GameConstants_MYGY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
				} else {
					cards_data[cbPosition++] = _logic.switch_to_card_data(_logic.get_all_magic_card_index()[m])
							+ GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		}
		for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i)) {
				continue;
			}
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					if (add_flag && _logic.get_card_color(_logic.switch_to_card_data(i)) != player_duan[seat_index]) {
						cards_data[cbPosition++] = _logic.switch_to_card_data(i) + GameConstants_MYGY.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					} else {
						continue;
					}
				}
			}
		}
		for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i)) {
				continue;
			}
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					if (add_flag && _logic.get_card_color(_logic.switch_to_card_data(i)) != player_duan[seat_index]) {
						continue;
					} else {
						cards_data[cbPosition++] = _logic.switch_to_card_data(i);
					}
				}
			}
		}
		return cbPosition;
	}

	public int get_ting_card(int[] cards, int[] cards_hu_fan, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_MYGY.MAX_INDEX];
		for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		if (player_duan[seat_index] != -1) {
			for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
				if (_logic.is_magic_index(i)) {
					continue;
				}
				if (GRR._cards_index[seat_index][i] > 0 && _logic.get_card_color(_logic.switch_to_card_data(i)) == player_duan[seat_index])
					return count;
			}
		}

		int max_ting_count = GameConstants_MYGY.MAX_ZI;
		int real_max_ting_count = GameConstants_MYGY.MAX_ZI;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (getRuleValue(GameConstants_MYGY.GAME_RULE_ER_FANG_PAI) == 1 && _logic.get_card_color(cbCurrentCard) == 0
					&& !_logic.is_magic_index(i)) {
				continue;
			}
			if (player_duan[seat_index] != -1 && _logic.get_card_color(cbCurrentCard) == player_duan[seat_index] && !_logic.is_magic_index(i)) {
				continue;
			}

			if (GameConstants_MYGY.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_MYGY.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards_hu_fan[count] = get_pai_xin_fan(chr);
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
	public boolean operate_chi_hu_cards(int seat_index, int count, int cards[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_CHI_HU_CARDS);
		roomResponse.setTarget(seat_index);

		for (int i = 0; i < count; i++) {
			roomResponse.addChiHuCards(cards[i]);
			roomResponse.addDouliuzi(_playerStatus[seat_index]._hu_out_cards_fan[0][i]);
		}

		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public int get_pai_xin_fan(ChiHuRight chr) {
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_LONG_BEI).is_empty()) {
			return 30;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_QI_DUI).is_empty()) {
			return 20;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_LONG_QI_DUI).is_empty()) {
			return 20;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_SHUANG_QING).is_empty()) {
			return 20;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_DA_DUI).is_empty()) {
			return 15;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_XIAO_QI_DUI).is_empty()) {
			return 10;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_DAN_DIAO).is_empty()) {
			return 10;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_YI_SE).is_empty()) {
			return 10;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_DA_DUI_ZI).is_empty()) {
			return 5;
		}
		return 1;
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
		int cbCardIndexTemp[] = new int[GameConstants_MYGY.MAX_INDEX];
		for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		int cbCurrentCard;
		int max_ting_count = GameConstants_MYGY.MAX_ZI;

		List<com.cai.common.util.Tuple> list = Lists.newArrayList();
		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			ChiHuRight chr = new ChiHuRight();
			if (GameConstants_MYGY.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItems, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_MYGY.CHR_ZI_MO, seat_index)) {
				list.add(new com.cai.common.util.Tuple<>(cbCurrentCard, chr, 0));
			}
		}

		if (list.isEmpty())
			return null;

		// 设置各胡型权重
		list.forEach((tuple) -> {
			ChiHuRight chr = (ChiHuRight) tuple.getCenter();
			if (!chr.opr_and(GameConstants_MYGY.CHR_QING_LONG_BEI).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_QING_LONG_BEI);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_LONG_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_LONG_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_QING_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_QING_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_SHUANG_QING).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_SHUANG_QING);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_QING_DA_DUI).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_QING_DA_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_QING_YI_SE).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_QING_YI_SE);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_XIAO_QI_DUI).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_XIAO_QI_DUI);
				return;
			}
			if (!chr.opr_and(GameConstants_MYGY.CHR_DAN_DIAO).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_QING_YI_SE);
				return;
			} else if (!chr.opr_and(GameConstants_MYGY.CHR_DA_DUI_ZI).is_empty()) {
				tuple.setRight(GameConstants_MYGY.CHR_DA_DUI_ZI);
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

		if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
			if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
				lChiHuScore += banker_continue_count[seat_index];
				if (banker_continue_count[seat_index] > 0) {
					show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
				}
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			if (_playerStatus[i]._hu_card_count > 0)
				continue;

			float s = lChiHuScore;
			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				// 连庄玩法 加连庄数
				if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
					s += banker_continue_count[i];
					if (banker_continue_count[i] > 0) {
						show_index_score[i][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[i];
					}
				}
			}

			GRR._game_score[i] -= s;
			hu_type_socre[i] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}
	}

	// 七小对牌 七小对：胡牌时，手上任意七对牌。
	public int is_qi_xiao_dui(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, boolean zimo) {

		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants_MYGY.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;

		// 临时数据
		int cbCardIndexTemp[] = new int[GameConstants_MYGY.MAX_INDEX];
		for (int i = 0; i < GameConstants_MYGY.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入数据
		int cbCurrentIndex = _logic.switch_to_card_index(cur_card);
		cbCardIndexTemp[cbCurrentIndex]++;

		// 计算单牌

		boolean has_three = false;
		// 计算单牌
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			int cbCardCount = cbCardIndexTemp[i];

			if (_logic.get_magic_card_count() > 0) {
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m)) {
						continue;
					}

					// 单牌统计
					if (cbCardCount == 1 || cbCardCount == 3)
						cbReplaceCount++;

					if (cbCardCount == 3) {
						has_three = true;
					}
					if (cbCardCount == 4) {
						nGenCount++;
					}
				}
			} else {
				// 单牌统计
				if (cbCardCount == 1 || cbCardCount == 3)
					cbReplaceCount++;

				if (cbCardCount == 4) {
					nGenCount++;
				}
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			int count = 0;
			for (int m = 0; m < _logic.get_magic_card_count(); m++) {
				count += cbCardIndexTemp[_logic.get_magic_card_index(m)];
			}
			if (!zimo && _logic.is_magic_card(cur_card)) {
				count--;
			}
			if (cbReplaceCount > count) {
				return GameConstants.WIK_NULL;
			}
			count -= cbReplaceCount;
			if (count == 4 || has_three || count == 2) {
				nGenCount++;
			}
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			return GameConstants_MYGY.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_MYGY.CHR_XIAO_QI_DUI;
		}
	}

	private int get_hu_fen(ChiHuRight chr) {
		int fen = 0;
		if (has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
			fen = 3;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_LONG_BEI).is_empty()) {
			fen += 30;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_LONG_QI_DUI).is_empty()) {
			fen += 20;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_QI_DUI).is_empty()) {
			fen += 20;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_SHUANG_QING).is_empty()) {
			fen += 20;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_DA_DUI).is_empty()) {
			fen += 15;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_QING_YI_SE).is_empty()) {
			fen += 10;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_XIAO_QI_DUI).is_empty()) {
			fen += 10;
		}
		if (!chr.opr_and(GameConstants_MYGY.CHR_DAN_DIAO).is_empty()) {
			fen += 10;
		} else if (!chr.opr_and(GameConstants_MYGY.CHR_DA_DUI_ZI).is_empty()) {
			fen += 5;
		}
		return fen == 0 ? 1 : fen;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		if (player_duan[_seat_index] != -1 && _logic.get_card_color(cur_card) == player_duan[_seat_index] && !_logic.is_magic_card(cur_card)) {
			return GameConstants_MYGY.WIK_NULL;
		}
		boolean have_fan_hu = false;
		boolean add_qing_yi_se = true;

		boolean is_zimo = false;
		if (card_type == GameConstants_MYGY.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_MYGY.CHR_ZI_MO);
			is_zimo = true;
		} else if (card_type == GameConstants_MYGY.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_MYGY.CHR_QING_GANG_HU);// 抢杠胡
		} else if (card_type == GameConstants_MYGY.HU_CARD_TYPE_RE_PAO) {
			chiHuRight.opr_or(GameConstants_MYGY.CHR_RE_PAO);// 热炮
		} else if (card_type == GameConstants_MYGY.HU_CARD_TYPE_GANG_KAI_HUA) {
			is_zimo = true;
			chiHuRight.opr_or(GameConstants_MYGY.CHR_ZI_MO);
			chiHuRight.opr_or(GameConstants_MYGY.CHR_GNAG_KAI);
		} else if (card_type == GameConstants_MYGY.HU_CARD_TYPE_JIE_PAO || card_type == GameConstants_MYGY.HU_CARD_TYPE_SHA_BAO) {
			chiHuRight.opr_or(GameConstants_MYGY.CHR_SHU_FAN);
		}

		if (has_rule(GameConstants_MYGY.GAME_RULE_JIN_GANG_BU_DAO) && _logic.get_card_count_by_index(cards_index) == 13) {
			chiHuRight.opr_or(GameConstants_MYGY.CHR_JIN_GANG_BU_DAO);
		}
		int check_dui_zi_hu = is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card, is_zimo);
		if (GameConstants_MYGY.WIK_NULL != check_dui_zi_hu) {
			have_fan_hu = true;
			if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
				if (GameConstants_MYGY.CHR_XIAO_QI_DUI == check_dui_zi_hu) {
					chiHuRight.opr_or(GameConstants_MYGY.CHR_QING_QI_DUI);
				}
				if (GameConstants_MYGY.CHR_LONG_QI_DUI == check_dui_zi_hu) {
					chiHuRight.opr_or(GameConstants_MYGY.CHR_QING_LONG_BEI);
				}
				add_qing_yi_se = false;
			} else {
				chiHuRight.opr_or(check_dui_zi_hu);
			}

			return GameConstants_MYGY.WIK_CHI_HU;
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), is_zimo);

		if (!bValue && check_dui_zi_hu == GameConstants_MYGY.WIK_NULL) {
			chiHuRight.set_empty();
			return GameConstants_MYGY.WIK_NULL;
		}

		boolean pengpeng_hu = AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), is_zimo);
		if (pengpeng_hu) {
			have_fan_hu = true;
			if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
				if (_logic.get_card_count_by_index(cards_index) == 1) {
					chiHuRight.opr_or(GameConstants_MYGY.CHR_SHUANG_QING);
				} else {
					chiHuRight.opr_or(GameConstants_MYGY.CHR_QING_DA_DUI);
				}
				add_qing_yi_se = false;
			} else {
				if (_logic.get_card_count_by_index(cards_index) == 1) {
					chiHuRight.opr_or(GameConstants_MYGY.CHR_DAN_DIAO);
				} else {
					chiHuRight.opr_or(GameConstants_MYGY.CHR_DA_DUI_ZI);
				}
			}
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card) && add_qing_yi_se) {
			have_fan_hu = true;
			chiHuRight.opr_or(GameConstants_MYGY.CHR_QING_YI_SE);
		}

		if (card_type == GameConstants_MYGY.HU_CARD_TYPE_JIE_PAO) {
			int gang_count = 0;
			for (int w = 0; w < GRR._weave_count[_seat_index]; w++) {
				if (GRR._weave_items[_seat_index][w].weave_kind != GameConstants_MYGY.WIK_GANG) {
					continue;
				}
				gang_count++;
			}
			if (have_fan_hu)
				return GameConstants_MYGY.WIK_CHI_HU;
			if (gang_count > 0)
				return GameConstants_MYGY.WIK_CHI_HU;

			chiHuRight.set_empty();
			return GameConstants_MYGY.WIK_NULL;
		}
		return GameConstants_MYGY.WIK_CHI_HU;
	}

	public void process_ji_fen() {
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			// 吹风鸡
			if (has_rule(GameConstants_MYGY.GAME_RULE_CHUI_FENG_JI) && handler_select_magic._da_dian_card == GameConstants_MYGY.WU_TONG_CARD) {
				continue;
			}
			int s = 0;
			int ting_count = _playerStatus[player]._hu_card_count;

			// 冲锋鸡
			if (chong_feng_ji_seat_yj == player) {
				// s += jin_ji[0] ? 6 : 3; // 冲锋鸡也z要算金鸡分
				if (jin_ji[0]) {
					if (has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
						s += 6;
						show_index_score[player][GameConstants_MYGY.INDEX_17_JT_CFJJ] += 6;
						show_index_card[player][GameConstants_MYGY.INDEX_17_JT_CFJJ] = GameConstants_MYGY.YJ_CARD;
					} else {
						s += 4;
						show_index_score[player][GameConstants_MYGY.INDEX_17_JT_CFJJ] += 4;
						show_index_card[player][GameConstants_MYGY.INDEX_17_JT_CFJJ] = GameConstants_MYGY.YJ_CARD;
					}
				} else {
					if (has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
						s += 3;
						show_index_score[player][GameConstants_MYGY.INDEX_16_JT_CFJ] += 3;
						show_index_card[player][GameConstants_MYGY.INDEX_16_JT_CFJ] = GameConstants_MYGY.YJ_CARD;
					} else {
						s += 2;
						show_index_score[player][GameConstants_MYGY.INDEX_16_JT_CFJ] += 2;
						show_index_card[player][GameConstants_MYGY.INDEX_16_JT_CFJ] = GameConstants_MYGY.YJ_CARD;
					}
				}
			}
			if (chong_feng_ji_seat_bt == player) {
				if (jin_ji[1]) {
					s += 8;
					show_index_score[player][GameConstants_MYGY.INDEX_19_JT_CFJWGJ] += 8;
					show_index_card[player][GameConstants_MYGY.INDEX_19_JT_CFJWGJ] = GameConstants_MYGY.BA_TONG_CARD;
				} else {
					s += 4;
					show_index_score[player][GameConstants_MYGY.INDEX_18_JT_CFWGJ] += 4;
					show_index_card[player][GameConstants_MYGY.INDEX_18_JT_CFWGJ] = GameConstants_MYGY.BA_TONG_CARD;
				}
			}

			// 已打出的固定鸡牌
			for (int j = 0; j < out_ji_pai_count[player]; j++) {
				int out_card = out_ji_pai[player][j];

				if (!is_ji_card(out_card))
					continue;

				// 乌骨鸡
				if (out_card == GameConstants_MYGY.BA_TONG_CARD) {
					if (jin_ji[1]) {
						s += 4;
						show_index_score[player][GameConstants_MYGY.INDEX_23_JT_JWGJ] += 4;
						show_index_card[player][GameConstants_MYGY.INDEX_23_JT_JWGJ] = GameConstants_MYGY.BA_TONG_CARD;
					} else {
						s += 2;
						show_index_score[player][GameConstants_MYGY.INDEX_25_JT_WGJ] += 2;
						show_index_card[player][GameConstants_MYGY.INDEX_25_JT_WGJ] = GameConstants_MYGY.BA_TONG_CARD;
					}
					continue;
				}

				// 幺鸡
				if (out_card == GameConstants_MYGY.YJ_CARD) {
					if (jin_ji[0]) {
						s += 2;
						show_index_score[player][GameConstants_MYGY.INDEX_22_JT_JJ] += 2;
						show_index_card[player][GameConstants_MYGY.INDEX_22_JT_JJ] = GameConstants_MYGY.YJ_CARD;
					} else {
						s += 1;
						show_index_score[player][GameConstants_MYGY.INDEX_26_JT_YJ] += 1;
						show_index_card[player][GameConstants_MYGY.INDEX_26_JT_YJ] = GameConstants_MYGY.YJ_CARD;
					}
					continue;
				}

				s += 1;
				if (shang_xia_ji[0] == out_card) {
					show_index_score[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] += 1;
					show_index_card[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] = shang_xia_ji[0];
				}
				if (shang_xia_ji[1] == out_card) {
					show_index_score[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] += 1;
					show_index_card[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] = shang_xia_ji[1];
				}
				if (shang_xia_ji[2] == out_card) {
					show_index_score[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] += 1;
					show_index_card[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] = shang_xia_ji[2];
				}
			}

			if (ting_count > 0) {
				// 已被打出翻鸡牌
				for (int dis = 0; dis < GRR._discard_count[player]; dis++) {
					int discard = GRR._discard_cards[player][dis];
					// 星期鸡
					if (has_rule(GameConstants_MYGY.GAME_RULE_XING_QI_JI) && TimeUtil.isSameWeekDay(_logic.get_card_value(discard))) {
						int show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
						if (_logic.get_card_color(discard) == 0) {
							show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
						} else if (_logic.get_card_color(discard) == 1) {
							show_index = GameConstants_MYGY.INDEX_31_JT_XQJ_TWO;
						} else if (_logic.get_card_color(discard) == 2) {
							show_index = GameConstants_MYGY.INDEX_32_JT_XQJ_THREE;
						}
						show_index_score[player][show_index] += 1;
						show_index_card[player][show_index] = discard;
						s += 1;
					}

					if (discard == GameConstants_MYGY.YJ_CARD)
						continue;
					if (has_rule(GameConstants_MYGY.GAME_RULE_WU_GU_JI) && discard == GameConstants_MYGY.BA_TONG_CARD)
						continue;

					if (is_ji_card(discard)) {
						s += 1;
						if (shang_xia_ji[0] == discard) {
							show_index_score[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] += 1;
							show_index_card[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] = shang_xia_ji[0];
						}
						if (shang_xia_ji[1] == discard) {
							show_index_score[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] += 1;
							show_index_card[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] = shang_xia_ji[1];
						}
						if (shang_xia_ji[2] == discard) {
							show_index_score[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] += 1;
							show_index_card[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] = shang_xia_ji[2];
						}
					}
				}

				// 叫牌了，算手上的鸡牌了咯
				int hand_cards[] = new int[GameConstants_MYGY.MAX_COUNT];
				_logic.switch_to_cards_data(GRR._cards_index[player], hand_cards);
				for (int i = 0; i < GameConstants_MYGY.MAX_COUNT; i++) {
					int hand_card = hand_cards[i];

					// 星期鸡
					if (has_rule(GameConstants_MYGY.GAME_RULE_XING_QI_JI) && TimeUtil.isSameWeekDay(_logic.get_card_value(hand_card))) {
						int show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
						if (_logic.get_card_color(hand_card) == 0) {
							show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
						} else if (_logic.get_card_color(hand_card) == 1) {
							show_index = GameConstants_MYGY.INDEX_31_JT_XQJ_TWO;
						} else if (_logic.get_card_color(hand_card) == 2) {
							show_index = GameConstants_MYGY.INDEX_32_JT_XQJ_THREE;
						}
						show_index_score[player][show_index] += 1;
						show_index_card[player][show_index] = hand_card;
						s += 1;
					}

					if (!is_ji_card(hand_card))
						continue;

					// 乌骨鸡
					if (hand_card == GameConstants_MYGY.BA_TONG_CARD && has_rule(GameConstants_MYGY.GAME_RULE_WU_GU_JI)) {
						if (jin_ji[1]) {
							s += 4;
							show_index_score[player][GameConstants_MYGY.INDEX_23_JT_JWGJ] += 4;
							show_index_card[player][GameConstants_MYGY.INDEX_23_JT_JWGJ] = GameConstants_MYGY.BA_TONG_CARD;
						} else {
							s += 2;
							show_index_score[player][GameConstants_MYGY.INDEX_25_JT_WGJ] += 2;
							show_index_card[player][GameConstants_MYGY.INDEX_25_JT_WGJ] = GameConstants_MYGY.BA_TONG_CARD;
						}
						continue;
					}

					// 幺鸡
					if (hand_card == GameConstants_MYGY.YJ_CARD) {
						if (jin_ji[0]) {
							s += 2;
							show_index_score[player][GameConstants_MYGY.INDEX_22_JT_JJ] += 2;
							show_index_card[player][GameConstants_MYGY.INDEX_22_JT_JJ] = GameConstants_MYGY.YJ_CARD;
						} else {
							s += 1;
							show_index_score[player][GameConstants_MYGY.INDEX_26_JT_YJ] += 1;
							show_index_card[player][GameConstants_MYGY.INDEX_26_JT_YJ] = GameConstants_MYGY.YJ_CARD;
						}
						continue;
					}

					s += 1;
					if (shang_xia_ji[0] == hand_card) {
						show_index_score[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] += 1;
						show_index_card[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] = shang_xia_ji[0];
					}
					if (shang_xia_ji[1] == hand_card) {
						show_index_score[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] += 1;
						show_index_card[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] = shang_xia_ji[1];
					}
					if (shang_xia_ji[2] == hand_card) {
						show_index_score[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] += 1;
						show_index_card[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] = shang_xia_ji[2];
					}
				}

				for (int j = 0; j < GRR._weave_count[player]; j++) {
					// 星期鸡
					if (has_rule(GameConstants_MYGY.GAME_RULE_XING_QI_JI)
							&& TimeUtil.isSameWeekDay(_logic.get_card_value(GRR._weave_items[player][j].center_card))) {
						if (GameConstants_MYGY.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
							s += 3;
							int show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
							if (_logic.get_card_color(GRR._weave_items[player][j].center_card) == 0) {
								show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
							} else if (_logic.get_card_color(GRR._weave_items[player][j].center_card) == 1) {
								show_index = GameConstants_MYGY.INDEX_31_JT_XQJ_TWO;
							} else if (_logic.get_card_color(GRR._weave_items[player][j].center_card) == 2) {
								show_index = GameConstants_MYGY.INDEX_32_JT_XQJ_THREE;
							}
							show_index_score[player][show_index] += 3;
							show_index_card[player][show_index] = GRR._weave_items[player][j].center_card;
						} else {
							s += 4;
							int show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
							if (_logic.get_card_color(GRR._weave_items[player][j].center_card) == 0) {
								show_index = GameConstants_MYGY.INDEX_30_JT_XQJ_ONE;
							} else if (_logic.get_card_color(GRR._weave_items[player][j].center_card) == 1) {
								show_index = GameConstants_MYGY.INDEX_31_JT_XQJ_TWO;
							} else if (_logic.get_card_color(GRR._weave_items[player][j].center_card) == 2) {
								show_index = GameConstants_MYGY.INDEX_32_JT_XQJ_THREE;
							}
							show_index_score[player][show_index] += 4;
							show_index_card[player][show_index] = GRR._weave_items[player][j].center_card;
						}
					}

					if (!is_ji_card(GRR._weave_items[player][j].center_card))
						continue;

					// 乌骨鸡
					if (GRR._weave_items[player][j].center_card == GameConstants_MYGY.BA_TONG_CARD
							&& has_rule(GameConstants_MYGY.GAME_RULE_WU_GU_JI)) {
						continue;
					}
					// 幺鸡
					if (GRR._weave_items[player][j].center_card == GameConstants_MYGY.YJ_CARD) {
						continue;
					}

					if (GameConstants_MYGY.WIK_PENG == GRR._weave_items[player][j].weave_kind) {
						s += 3;
						if (shang_xia_ji[0] == GRR._weave_items[player][j].center_card) {
							show_index_score[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] += 3;
							show_index_card[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] = shang_xia_ji[0];
						}
						if (shang_xia_ji[1] == GRR._weave_items[player][j].center_card) {
							show_index_score[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] += 3;
							show_index_card[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] = shang_xia_ji[1];
						}
						if (shang_xia_ji[2] == GRR._weave_items[player][j].center_card) {
							show_index_score[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] += 3;
							show_index_card[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] = shang_xia_ji[2];
						}
					} else {
						s += 4;
						if (shang_xia_ji[0] == GRR._weave_items[player][j].center_card) {
							show_index_score[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] += 4;
							show_index_card[player][GameConstants_MYGY.INDEX_27_JT_FPJ_ONE] = shang_xia_ji[0];
						}
						if (shang_xia_ji[1] == GRR._weave_items[player][j].center_card) {
							show_index_score[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] += 4;
							show_index_card[player][GameConstants_MYGY.INDEX_28_JT_FPJ_TWO] = shang_xia_ji[1];
						}
						if (shang_xia_ji[2] == GRR._weave_items[player][j].center_card) {
							show_index_score[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] += 4;
							show_index_card[player][GameConstants_MYGY.INDEX_29_JT_FPJ_THREE] = shang_xia_ji[2];
						}
					}
				}
			}

			if (_playerStatus[player]._hu_card_count == 0) {
				s *= -1;
				for (int i = 0; i < GameConstants_MYGY.INDEX_COUNT; i++) {
					if (show_index_score[player][i] > 0) {
						show_index_score[player][i] *= -1;
					}
				}
			}

			if ((shao[player] && s > 0) || s == 0) {
				for (int i = 0; i < GameConstants_MYGY.INDEX_COUNT; i++) {
					if (show_index_score[player][i] > 0) {
						show_index_score[player][i] = 0;
					}
				}
				continue;
			}

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
		// 吹风鸡
		if (has_rule(GameConstants_MYGY.GAME_RULE_CHUI_FENG_JI) && handler_select_magic._da_dian_card == GameConstants_MYGY.WU_TONG_CARD) {
			return;
		}
		for (int c = 0; c < responsibility_ji_count; c++) {
			for (int count = 0; count < 2; count++) {
				Tuple<Integer, Integer> responsibility = responsibility_ji[c][count];

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
				if (check_ting.getRight() >= check_ting.getLeft()) {
					if (shao[responsibility_ji[c][count].getRight()]) {
						continue;
					}
					if (count == 0) {
						if (c == 0) {
							show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_20_JT_ZRJ] -= base;
						} else if (c == 1) {
							show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_20_JT_ZRJ_TWO] -= base;
						} else if (c == 2) {
							show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_20_JT_ZRJ_THREE] -= base;
						}
						show_index_card[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_20_JT_ZRJ] = GameConstants_MYGY.YJ_CARD;
						show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_20_JT_ZRJ] += base;
						show_index_card[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_20_JT_ZRJ] = GameConstants_MYGY.YJ_CARD;
					} else {
						if (c == 0) {
							show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] -= base;
						} else if (c == 1) {
							show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ_TWO] -= base;
						} else if (c == 2) {
							show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ_THREE] -= base;
						}
						show_index_card[responsibility_ji[c][count]
								.getLeft()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] = GameConstants_MYGY.BA_TONG_CARD;
						show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] += base;
						show_index_card[responsibility_ji[c][count]
								.getRight()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] = GameConstants_MYGY.BA_TONG_CARD;
					}
					GRR._game_score[responsibility_ji[c][count].getLeft()] -= base;
					GRR._game_score[responsibility_ji[c][count].getRight()] += base;
				} else {
					if (shao[responsibility_ji[c][count].getLeft()]) {
						continue;
					}

					if (count == 0) {
						if (c == 0) {
							show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_20_JT_ZRJ] -= base;
						} else if (c == 1) {
							show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_20_JT_ZRJ_TWO] -= base;
						} else if (c == 2) {
							show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_20_JT_ZRJ_THREE] -= base;
						}
						show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_20_JT_ZRJ] += base;
						show_index_card[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_20_JT_ZRJ] = GameConstants_MYGY.YJ_CARD;
						show_index_card[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_20_JT_ZRJ] = GameConstants_MYGY.YJ_CARD;
					} else {
						if (c == 0) {
							show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] -= base;
						} else if (c == 1) {
							show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ_TWO] -= base;
						} else if (c == 2) {
							show_index_score[responsibility_ji[c][count].getRight()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ_TWO] -= base;
						}
						show_index_score[responsibility_ji[c][count].getLeft()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] += base;
						show_index_card[responsibility_ji[c][count]
								.getLeft()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] = GameConstants_MYGY.BA_TONG_CARD;
						show_index_card[responsibility_ji[c][count]
								.getRight()][GameConstants_MYGY.INDEX_21_JT_ZRWGJ] = GameConstants_MYGY.BA_TONG_CARD;
					}
					GRR._game_score[responsibility_ji[c][count].getLeft()] += base;
					GRR._game_score[responsibility_ji[c][count].getRight()] -= base;
				}

				// 算了一次就请掉哈，不要重复计算撤
				responsibility.setLeft(-1);
				responsibility.setRight(-1);
			}
		}

		for (int p = 0; p < getTablePlayerNumber(); p++)
			player_ji_score[p] = (int) GRR._game_score[p];
	}

	public void process_chi_hu_player_score_two(int seat_index, int provide_index, int operate_card, boolean zimo) {
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
		start_hu_score[seat_index] = get_hu_fen(chr);

		if (!chr.opr_and(GameConstants_MYGY.CHR_JIN_GANG_BU_DAO).is_empty()) {
			lChiHuScore += 5;
			show_index_score[seat_index][GameConstants_MYGY.INDEX_7_HT_JIN_GANG_BU_DAO] += 5;
		}
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			chr.opr_or(GameConstants_MYGY.CHR_ZI_MO);
			hu_base_fen = lChiHuScore;

			// 天听软报额外收取10分
			if (_playerStatus[seat_index].is_bao_ting()) {
				if (player_ruan_bao[seat_index]) {
					lChiHuScore += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_4_HT_RUAN_BAO] += 10;
				} else if (player_ying_bao[seat_index]) {
					lChiHuScore += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_3_HT_YING_BAO] += 20;
				}
			}

			// 杠开多加一分
			if (!chr.opr_and(GameConstants_MYGY.CHR_GNAG_KAI).is_empty()) {
				lChiHuScore += 3;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_9_HT_GANG_SHNAG_HUA] += 3;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_9_HT_GANG_SHNAG_HUA] = operate_card;
			}

			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				lChiHuScore++;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_0_HT_ZI_MO] += 1;

				if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
					lChiHuScore += banker_continue_count[seat_index];
					if (banker_continue_count[seat_index] > 0) {
						show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
					}
				}
			}

			if (player_gu_mai[seat_index] == 2) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] += 2;
			} else if (player_gu_mai[seat_index] == 1) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] += 1;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
					// 连庄玩法 加连庄数
					if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
						s += banker_continue_count[i];
						if (banker_continue_count[i] > 0) {
							show_index_score[i][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[i];
						}
					}
				}

				// 杀报不和平胡叠加，所以加9
				if (_playerStatus[i].is_bao_ting()) {
					if (player_ruan_bao[i]) {
						s += 10;
						show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 10;
						show_index_score[i][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 10;
					} else if (player_ying_bao[i]) {
						s += 20;
						show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 20;
						show_index_score[i][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 20;
					}
				}

				if (player_gu_mai[i] == 2) {
					show_index_score[i][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] -= 2;
				} else if (player_gu_mai[i] == 1) {
					show_index_score[i][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] -= 1;
				}
				int gu_mai_score = player_gu_mai[seat_index] + player_gu_mai[i];
				end_mai_score[seat_index] += gu_mai_score;
				end_mai_score[i] -= gu_mai_score;

				s += gu_mai_score;
				// 添加动作)
				GRR._game_score[i] -= s;
				hu_type_socre[i] -= s;
				GRR._game_score[seat_index] += s;
				hu_type_socre[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			chr.opr_or(GameConstants_MYGY.CHR_SHU_FAN);
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_MYGY.CHR_FANG_PAO);

			float s = lChiHuScore;
			hu_base_fen = s;

			// 天听软报额外收取10分
			boolean flag = false;
			if (_playerStatus[seat_index].is_bao_ting()) {
				if (player_ruan_bao[seat_index]) {
					s += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_4_HT_RUAN_BAO] += 10;
				} else if (player_ying_bao[seat_index]) {
					s += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_3_HT_YING_BAO] += 20;
				}
				flag = true;
			}

			// 杀报不和平胡叠加，所以加9
			if (_playerStatus[provide_index].is_bao_ting()) {
				if (player_ruan_bao[provide_index]) {
					s += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 10;
					show_index_score[provide_index][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 10;
				} else if (player_ying_bao[provide_index]) {
					s += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 20;
					show_index_score[provide_index][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 20;
				}
			}

			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				s += banker_continue_count[seat_index] + banker_continue_count[provide_index];
				if (banker_continue_count[seat_index] > 0) {
					show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
				}
				if (banker_continue_count[provide_index] > 0 && show_index_score[provide_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] == 0) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[provide_index];
				}
			}

			// 热炮通三多出3分,其他热炮多加一分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_MYGY.CHR_RE_PAO).is_empty()) {
				int re_pao_score = 1;
				if (has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
					re_pao_score = 9;
					if (re_pao_gang_type == GameConstants_MYGY.GANG_TYPE_JIE_GANG) {
						re_pao_score = 6;
					}
				}
				s += re_pao_score;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_10_HT_RE_PAO] += re_pao_score;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_10_HT_RE_PAO] = operate_card;
			}

			// 抢杠胡多出9分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_MYGY.CHR_QING_GANG_HU).is_empty()) {
				s += 1;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_9_HT_QIANG_GANG_HU] += 1;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_9_HT_QIANG_GANG_HU] = operate_card;
			}

			if (player_gu_mai[seat_index] == 2) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] += 2;
			} else if (player_gu_mai[seat_index] == 1) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] += 1;
			}
			if (show_index_score[provide_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] == 0) {
				if (player_gu_mai[provide_index] == 2) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] -= 2;
				} else if (player_gu_mai[provide_index] == 1) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] -= 1;
				}
			}
			int gu_mai_score = player_gu_mai[seat_index] + player_gu_mai[provide_index];
			end_mai_score[seat_index] += gu_mai_score;
			end_mai_score[provide_index] -= gu_mai_score;

			s += gu_mai_score;

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

		change_player_status(seat_index, GameConstants_MYGY.INVALID_VALUE);
	}

	public void process_chi_hu_player_score_three(int seat_index, int provide_index, int operate_card, boolean zimo) {
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
		start_hu_score[seat_index] = get_hu_fen(chr);
		if (!chr.opr_and(GameConstants_MYGY.CHR_JIN_GANG_BU_DAO).is_empty()) {
			lChiHuScore += 5;
			show_index_score[seat_index][GameConstants_MYGY.INDEX_7_HT_JIN_GANG_BU_DAO] += 5;
		}
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			chr.opr_or(GameConstants_MYGY.CHR_ZI_MO);
			hu_base_fen = lChiHuScore;

			// 天听软报额外收取10分
			if (_playerStatus[seat_index].is_bao_ting()) {
				if (player_ruan_bao[seat_index]) {
					lChiHuScore += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_4_HT_RUAN_BAO] += 10;
				} else if (player_ying_bao[seat_index]) {
					lChiHuScore += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_3_HT_YING_BAO] += 20;
				}
			}

			// 杠开多加一分
			if (!chr.opr_and(GameConstants_MYGY.CHR_GNAG_KAI).is_empty()) {
				lChiHuScore += 3;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_9_HT_GANG_SHNAG_HUA] += 3;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_9_HT_GANG_SHNAG_HUA] = operate_card;
			}

			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				lChiHuScore++;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_0_HT_ZI_MO] += 1;

				if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
					lChiHuScore += banker_continue_count[seat_index];
					if (banker_continue_count[seat_index] > 0) {
						show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
					}
				}
			}

			if (player_gu_mai[seat_index] == 2) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] += 2;
			} else if (player_gu_mai[seat_index] == 1) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] += 1;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore;

				if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
					// 连庄玩法 加连庄数
					if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
						s += banker_continue_count[i];
						if (banker_continue_count[i] > 0) {
							show_index_score[i][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[i];
						}
					}
				}

				// 杀报不和平胡叠加，所以加9
				if (_playerStatus[i].is_bao_ting()) {
					if (player_ruan_bao[i]) {
						s += 10;
						show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 10;
						show_index_score[i][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 10;
					} else if (player_ying_bao[i]) {
						s += 20;
						show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 20;
						show_index_score[i][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 20;
					}
				}

				if (player_gu_mai[i] == 2) {
					show_index_score[i][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] -= 2;
				} else if (player_gu_mai[i] == 1) {
					show_index_score[i][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] -= 1;
				}
				int gu_mai_score = player_gu_mai[seat_index] + player_gu_mai[i];
				end_mai_score[seat_index] += gu_mai_score;
				end_mai_score[i] -= gu_mai_score;

				s += gu_mai_score;

				// 添加动作)
				GRR._game_score[i] -= s;
				hu_type_socre[i] -= s;
				GRR._game_score[seat_index] += s;
				hu_type_socre[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			chr.opr_or(GameConstants_MYGY.CHR_SHU_FAN);
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_MYGY.CHR_FANG_PAO);

			float s = lChiHuScore;
			hu_base_fen = s;

			// 天听软报额外收取10分
			boolean flag = false;
			if (_playerStatus[seat_index].is_bao_ting()) {
				if (player_ruan_bao[seat_index]) {
					s += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_4_HT_RUAN_BAO] += 10;
				} else if (player_ying_bao[seat_index]) {
					s += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_3_HT_YING_BAO] += 20;
				}
				flag = true;
			}

			// 杀报不和平胡叠加，所以加9
			if (player_ruan_bao[provide_index]) {
				s += 10;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 10;
				show_index_score[provide_index][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 10;
			} else if (player_ying_bao[provide_index]) {
				s += 20;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 20;
				show_index_score[provide_index][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 20;
			}

			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				s += banker_continue_count[seat_index] + banker_continue_count[provide_index];
				if (banker_continue_count[seat_index] > 0) {
					show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
				}
				if (banker_continue_count[provide_index] > 0 && show_index_score[provide_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] == 0) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[provide_index];
				}
			}

			// 热炮通三多出3分,其他热炮多加一分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_MYGY.CHR_RE_PAO).is_empty()) {
				int re_pao_score = 1;
				if (has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
					re_pao_score = 9;
					if (re_pao_gang_type == GameConstants_MYGY.GANG_TYPE_JIE_GANG) {
						re_pao_score = 6;
					}
				}
				s += re_pao_score;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_10_HT_RE_PAO] += re_pao_score;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_10_HT_RE_PAO] = operate_card;
			}

			// 抢杠胡多出9分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_MYGY.CHR_QING_GANG_HU).is_empty()) {
				s += 1;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_9_HT_QIANG_GANG_HU] += 1;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_9_HT_QIANG_GANG_HU] = operate_card;
			}

			if (player_gu_mai[seat_index] == 2) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] += 2;
			} else if (player_gu_mai[seat_index] == 1) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] += 1;
			}
			if (show_index_score[provide_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] == 0) {
				if (player_gu_mai[provide_index] == 2) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] -= 2;
				} else if (player_gu_mai[provide_index] == 1) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] -= 1;
				}
			}
			int gu_mai_score = player_gu_mai[seat_index] + player_gu_mai[provide_index];
			end_mai_score[seat_index] += gu_mai_score;
			end_mai_score[provide_index] -= gu_mai_score;

			s += gu_mai_score;

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

		change_player_status(seat_index, GameConstants_MYGY.INVALID_VALUE);
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_TWO_PLAYER) == 1) {
			process_chi_hu_player_score_two(seat_index, provide_index, operate_card, zimo);
			return;
		}
		if (getRuleValue(GameConstants.GAME_RULE_ALL_GAME_THREE_PLAYER) == 1) {
			process_chi_hu_player_score_three(seat_index, provide_index, operate_card, zimo);
			return;
		}
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
		start_hu_score[seat_index] = get_hu_fen(chr);
		if (!chr.opr_and(GameConstants_MYGY.CHR_JIN_GANG_BU_DAO).is_empty()) {
			lChiHuScore += 5;
			show_index_score[seat_index][GameConstants_MYGY.INDEX_7_HT_JIN_GANG_BU_DAO] += 5;
		}
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			chr.opr_or(GameConstants_MYGY.CHR_ZI_MO);

			hu_base_fen = lChiHuScore;

			if (_playerStatus[seat_index].is_bao_ting()) {
				if (player_ruan_bao[seat_index]) {
					lChiHuScore += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_4_HT_RUAN_BAO] += 10;
				} else if (player_ying_bao[seat_index]) {
					lChiHuScore += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_3_HT_YING_BAO] += 20;
				}
			}

			// 杠开多加一分
			if (!chr.opr_and(GameConstants_MYGY.CHR_GNAG_KAI).is_empty()) {
				lChiHuScore += 3;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_9_HT_GANG_SHNAG_HUA] += 3;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_9_HT_GANG_SHNAG_HUA] = operate_card;
			}

			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				lChiHuScore++;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_0_HT_ZI_MO] += 1;

				if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
					lChiHuScore += banker_continue_count[seat_index];
					if (banker_continue_count[seat_index] > 0) {
						show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
					}
				}
			}

			if (player_gu_mai[seat_index] == 2) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] += 2;
			} else if (player_gu_mai[seat_index] == 1) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] += 1;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = lChiHuScore;

				if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
					// 连庄玩法 加连庄数
					if (has_rule(GameConstants_MYGY.GAME_RULE_LIAN_ZHUANG)) {
						s += banker_continue_count[i];

						if (banker_continue_count[i] > 0) {
							show_index_score[i][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[i];
						}
					}
				}

				// 杀报不和平胡叠加，所以加9
				if (_playerStatus[i].is_bao_ting()) {
					if (player_ruan_bao[i]) {
						s += 10;
						show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 10;
						show_index_score[i][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 10;
					} else if (player_ying_bao[i]) {
						s += 20;
						show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 20;
						show_index_score[i][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 20;
					}
				}

				if (player_gu_mai[i] == 2) {
					show_index_score[i][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] -= 2;
				} else if (player_gu_mai[i] == 1) {
					show_index_score[i][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] -= 1;
				}
				int gu_mai_score = player_gu_mai[seat_index] + player_gu_mai[i];
				end_mai_score[seat_index] += gu_mai_score;
				end_mai_score[i] -= gu_mai_score;

				s += gu_mai_score;
				// 添加动作)
				GRR._game_score[i] -= s;
				hu_type_socre[i] -= s;
				GRR._game_score[seat_index] += s;
				hu_type_socre[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			chr.opr_or(GameConstants_MYGY.CHR_SHU_FAN);
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_MYGY.CHR_FANG_PAO);

			float s = lChiHuScore;
			hu_base_fen = s;

			// 天听软报额外收取10分
			boolean flag = false;
			if (_playerStatus[seat_index].is_bao_ting()) {
				if (player_ruan_bao[seat_index]) {
					s += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_4_HT_RUAN_BAO] += 10;
				} else if (player_ying_bao[seat_index]) {
					s += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_3_HT_YING_BAO] += 20;
				}
				flag = true;
			}
			// 杀报不和平胡叠加，所以加9
			if (_playerStatus[provide_index].is_bao_ting()) {
				if (player_ruan_bao[provide_index]) {
					s += 10;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 10;
					show_index_score[provide_index][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 10;
				} else if (player_ying_bao[provide_index]) {
					s += 20;
					show_index_score[seat_index][GameConstants_MYGY.INDEX_6_HT_SHA_BAO] += 20;
					show_index_score[provide_index][GameConstants_MYGY.INDEX_5_HT_BEI_SHA_BAO] -= 20;
				}
			}

			// 连庄玩法 加连庄数
			if (!has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
				s += banker_continue_count[seat_index] + banker_continue_count[provide_index];
				if (banker_continue_count[seat_index] > 0) {
					show_index_score[seat_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] += banker_continue_count[seat_index];
				}
				if (banker_continue_count[provide_index] > 0 && show_index_score[provide_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] == 0) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_2_HT_LIAN_ZHUAN] -= banker_continue_count[provide_index];
				}
			}

			// 热炮通三多出3分,其他热炮多加一分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_MYGY.CHR_RE_PAO).is_empty()) {
				int re_pao_score = 1;
				if (has_rule(GameConstants_MYGY.GAME_RULE_TONG_SAN)) {
					re_pao_score = 9;
					if (re_pao_gang_type == GameConstants_MYGY.GANG_TYPE_JIE_GANG) {
						re_pao_score = 6;
					}
				}
				s += re_pao_score;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_10_HT_RE_PAO] += re_pao_score;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_10_HT_RE_PAO] = operate_card;
			}

			// 抢杠胡多出9分
			if (!GRR._chi_hu_rights[seat_index].opr_and(GameConstants_MYGY.CHR_QING_GANG_HU).is_empty()) {
				s += 1;
				show_index_score[seat_index][GameConstants_MYGY.INDEX_9_HT_QIANG_GANG_HU] += 1;
				show_index_card[seat_index][GameConstants_MYGY.INDEX_9_HT_QIANG_GANG_HU] = operate_card;
			}

			if (player_gu_mai[seat_index] == 2) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] += 2;
			} else if (player_gu_mai[seat_index] == 1) {
				show_index_score[seat_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] += 1;
			}
			if (show_index_score[provide_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] == 0) {
				if (player_gu_mai[provide_index] == 2) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_12_HT_GU_MAI_TWO] -= 2;
				} else if (player_gu_mai[provide_index] == 1) {
					show_index_score[provide_index][GameConstants_MYGY.INDEX_11_HT_GU_MAI_ONE] -= 1;
				}
			}
			int gu_mai_score = player_gu_mai[seat_index] + player_gu_mai[provide_index];
			end_mai_score[seat_index] += gu_mai_score;
			end_mai_score[provide_index] -= gu_mai_score;

			s += gu_mai_score;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			hu_type_socre[provide_index] -= s;
			GRR._game_score[seat_index] += s;
			hu_type_socre[seat_index] += s;
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants_MYGY.INVALID_VALUE);
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
		// for (int i = 0; i < GameConstants_MYGY.MAX_NIAO_CARD; i++) {
		// GRR._cards_data_niao[i] = GameConstants_MYGY.INVALID_VALUE;
		// }
		//
		// for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
		// GRR._player_niao_count[i] = 0;
		// for (int j = 0; j < GameConstants_MYGY.MAX_NIAO_CARD; j++) {
		// GRR._player_niao_cards[i][j] = GameConstants_MYGY.INVALID_VALUE;
		// }
		// }
		//
		// GRR._show_bird_effect = show;
		// GRR._count_niao = getCsDingNiaoNum();
		//
		// if (GRR._count_niao > GameConstants_MYGY.ZHANIAO_0) {
		// if (card == GameConstants_MYGY.INVALID_VALUE) {
		// int cbCardIndexTemp[] = new int[GameConstants_MYGY.MAX_INDEX];
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
		// int[] player_niao_count = new int[GameConstants_MYGY.GAME_PLAYER];
		// int[][] player_niao_cards = new
		// int[GameConstants_MYGY.GAME_PLAYER][GameConstants_MYGY.MAX_NIAO_CARD];
		// for (int i = 0; i < GameConstants_MYGY.GAME_PLAYER; i++) {
		// player_niao_count[i] = 0;
		// for (int j = 0; j < GameConstants_MYGY.MAX_NIAO_CARD; j++) {
		// player_niao_cards[i][j] = GameConstants_MYGY.INVALID_VALUE;
		// }
		// }
		//
		// GRR._count_pick_niao = 0;
		// if (has_rule(GameConstants_MYGY.GAME_RULE_HU_JI_JIANG_JI)) {
		// player_niao_cards[seat_index][player_niao_count[seat_index]] =
		// this.set_ding_niao_valid(hu_card, true);
		// player_niao_count[seat_index]++;
		// if (hu_card == GameConstants_MYGY.HZ_MAGIC_CARD) {
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
		int nNum = GameConstants_MYGY.ZHANIAO_0;
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

			if (GRR._chi_hu_rights[player].is_valid()) {
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					if (p == player || GRR._chi_hu_rights[p].is_valid()) {
						continue;
					}
					if (_playerStatus[p].is_bao_ting()) {
						result.append(" 杀报");
						break;
					}
				}
			}
			chrTypes = GRR._chi_hu_rights[player].type_count;

			boolean ping_hu = true;
			boolean have_qing = false;
			boolean have_magic = false;

			int hu_base_fen = (int) this.hu_base_fen;
			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_MYGY.CHR_DA_DUI_ZI && !have_magic) {
						result.append(" 大对子");
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_LONG_QI_DUI && !have_magic) {
						result.append(" 龙七对");
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_XIAO_QI_DUI && !have_magic) {
						result.append(" 七小对");
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_QING_DA_DUI && !have_magic) {
						result.append(" 清大对");
						have_qing = true;
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_SHUANG_QING && !have_magic) {
						result.append(" 双清");
						have_qing = true;
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_DAN_DIAO && !have_magic) {
						result.append(" 把一");
						ping_hu = false;
					}
					if (type == GameConstants_MYGY.CHR_QING_QI_DUI && !have_magic) {
						result.append(" 清七对");
						have_qing = true;
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_QING_LONG_BEI && !have_magic) {
						result.append(" 青龙背");
						have_qing = true;
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_QING_YI_SE && !have_qing) {
						result.append(" 清一色");
						ping_hu = false;
						have_magic = true;
					}
					if (type == GameConstants_MYGY.CHR_QING_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_MYGY.CHR_ZI_MO) {
						result.append(" 自摸");
						start_hu_right[player] = GameConstants_MYGY.CHR_ZI_MO;
					}
					if (type == GameConstants_MYGY.CHR_SHU_FAN) {
						result.append(" 炮胡");
						start_hu_right[player] = GameConstants_MYGY.CHR_SHU_FAN;
					}
					if (type == GameConstants_MYGY.CHR_GNAG_KAI) {
						result.append(" 杠上花");
					}
					if (type == GameConstants_MYGY.CHR_RE_PAO) {
						result.append(" 热炮");
					}
				} else if (type == GameConstants_MYGY.CHR_FANG_PAO) {
					start_hu_right[player] = GameConstants_MYGY.CHR_FANG_PAO;
					if (qiang_gang_hu) {
						result.append(" 被抢杠");
					} else {
						result.append(" 放炮");
					}
				}
			}
			if (GRR._chi_hu_rights[player].is_valid()) {
				if (ping_hu) {
					result.append(" 平胡");
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
						if (GRR._weave_items[tmpPlayer][w].weave_kind != GameConstants_MYGY.WIK_GANG) {
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

			if (GRR._chi_hu_rights[player].is_valid()) {
				if (player_ying_bao[player]) {
					result.append(" 天听硬报");
				}
				if (player_ruan_bao[player]) {
					result.append(" 天听软报");
				}
			}

			// 传给客户端数据格式：平胡13 清一色13@鸡牌 +1_乌骨鸡 +3
			GRR._result_des[player] = result.toString() + " " + GRR._result_des[player];
		}
		if (_cur_banker != old_banker) {
			continue_banker_count = 1;
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (i == _cur_banker) {
				_player_result.qiang[_cur_banker] = continue_banker_count;
			} else {
				_player_result.qiang[i] = 0;
			}

			if (_playerStatus[i]._hu_card_count > 0) {
				if (!GRR._chi_hu_rights[i].is_valid()) {
					GRR._chi_hu_rights[i].set_empty();
					GRR._chi_hu_rights[i].opr_or(GameConstants_MYGY.CHR_JIAO_ZUI);
				} else {
					if (get_hu_fen(GRR._chi_hu_rights[i]) == 1) {
						GRR._chi_hu_rights[i].set_empty();
						GRR._chi_hu_rights[i].opr_or(GameConstants_MYGY.CHR_PING_HU);
					}
				}
			} else {
				GRR._chi_hu_rights[i].set_empty();
				GRR._chi_hu_rights[i].opr_or(GameConstants_MYGY.CHR_WEI_JIAO_ZUI);
			}
		}
	}

	/**
	 * 处理吃胡的玩家
	 * 
	 * @param seat_index
	 * @param operate_card
	 * @param rm
	 */
	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		if (seat_index != GameConstants_MYGY.INVALID_SEAT) {
			// 引用权位
			ChiHuRight chr = GRR._chi_hu_rights[seat_index];

			// filtrate_right(seat_index, chr);

			if (is_mj_type(GameConstants_MYGY.GAME_TYPE_ZZ) || is_mj_type(GameConstants_MYGY.GAME_TYPE_HZ)
					|| is_mj_type(GameConstants_MYGY.GAME_TYPE_FLS_HZ_LX) || is_mj_type(GameConstants_MYGY.GAME_TYPE_HENAN_ZHUAN_ZHUAN)) { // liuyan
				// 2017/7/10
				int effect_count = chr.type_count;
				long effect_indexs[] = new long[effect_count];
				for (int i = 0; i < effect_count; i++) {
					if (chr.type_list[i] == GameConstants_MYGY.CHR_SHU_FAN) {
						effect_indexs[i] = GameConstants_MYGY.CHR_HU;
					} else {
						effect_indexs[i] = chr.type_list[i];
					}

				}

				// 效果
				this.operate_effect_action(seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_HU, effect_count, effect_indexs, 1,
						GameConstants_MYGY.INVALID_SEAT);
			} else {
				// 效果
				this.operate_effect_action(seat_index, GameConstants_MYGY.EFFECT_ACTION_TYPE_HU, chr.type_count, chr.type_list, 1,
						GameConstants_MYGY.INVALID_SEAT);
			}

			// 手牌删掉
			this.operate_player_cards(seat_index, 0, null, 0, null);

			if (rm) {
				// 把摸的牌从手牌删掉,结算的时候不显示这张牌的
				GRR._cards_index[seat_index][_logic.switch_to_card_index(operate_card)]--;
			}
		}

		// 显示胡牌
		for (int p = 0; p < getTablePlayerNumber(); p++) {

			int cards[] = new int[GameConstants_MYGY.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[p], cards);
			if (seat_index == p) {
				cards[hand_card_count] = operate_card + GameConstants_MYGY.CARD_ESPECIAL_TYPE_HU;
				hand_card_count++;
			}
			this.operate_show_card(p, GameConstants_MYGY.Show_Card_HU, hand_card_count, cards, GameConstants_MYGY.INVALID_SEAT);
		}

		return;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x12, 0x12, 0x12, 0x14, 0x14, 0x14, 0x17, 0x17, 0x17, 0x29, 0x29, 0x29, 0x26 };
		int[] cards_of_player1 = new int[] { 0x12, 0x12, 0x12, 0x14, 0x14, 0x14, 0x17, 0x17, 0x17, 0x29, 0x29, 0x29, 0x26 };
		int[] cards_of_player2 = new int[] { 0x12, 0x12, 0x12, 0x14, 0x14, 0x14, 0x17, 0x17, 0x17, 0x29, 0x29, 0x29, 0x26 };
		int[] cards_of_player3 = new int[] { 0x11, 0x11, 0x11, 0x13, 0x13, 0x13, 0x14, 0x14, 0x15, 0x15, 0x16, 0x16, 0x18 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_MYGY.MAX_INDEX; j++) {
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
