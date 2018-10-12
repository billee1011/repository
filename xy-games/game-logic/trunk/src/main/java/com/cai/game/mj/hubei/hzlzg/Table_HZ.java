package com.cai.game.mj.hubei.hzlzg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuangZhou;
import com.cai.common.constant.game.GameConstants_HZLZG;
import com.cai.common.constant.game.GameConstants_NanNing;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJGameLogic.AnalyseItem;
import com.cai.game.mj.handler.MJHandlerGang;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

/**
 * 红中赖子杠
 * 
 * @author yu
 *
 */
public class Table_HZ extends AbstractMJTable {

	protected MJHandlerGang _handler_lai_gang;

	protected HandlerSelectMagic_HZ _handler_select_magic;

	public int pi_zi_card; // 痞子牌

	public boolean[] kai_kou;

	public int[][] special_gang_count; // 一位数组为每个人杠的数据，二维数组共3位，special_gang_count[][0] 赖子杠 [1] 痞子杠，[2]红中杠

	public int[] bao_pai; // 0:未报牌，1：报清， 2： 报将, 3： 过（不报）

	public int[] hu_fan; // 胡牌总番薯

	public int[] quan_bao;

	public int card_2_magic(int card) {
		if (_logic.is_magic_card(card)) {
			card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		if (card == pi_zi_card) {
			card += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
		}
		if (card == GameConstants.HZ_MAGIC_CARD) {
			card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
		}
		return card;
	}

	@Override
	public int get_real_card(int card) {
		if (card >= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_PI_ZI) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else if (card >= GameConstants.CARD_ESPECIAL_TYPE_PI_ZI && card < GameConstants.CARD_ESPECIAL_TYPE_HZ) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
		} else if (card >= GameConstants.CARD_ESPECIAL_TYPE_HZ) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_HZ;
		}
		return card;
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_HZ();
		_handler_dispath_card = new HandlerDispatchCard_HZ();
		_handler_gang = new HandlerGang_HZ();
		_handler_out_card_operate = new HandlerOutCardOperate_HZ();
		_handler_select_magic = new HandlerSelectMagic_HZ();
		_handler_lai_gang = new HandlerLaiGang_HZ();
	}

	public boolean is_gang_card(int outCard) {
		if (outCard == pi_zi_card)
			return true;
		if (outCard == GameConstants_HZLZG.HZ_MAGIC_CARD)
			return true;
		return false;
	}

	public int switch_to_cards_data(int cards_index[], int cards_data[]) {
		// 转换扑克
		int cbPosition = 0;

		for (int m = 0; m < _logic.get_magic_card_count(); m++) {
			for (int i = 0; i < cards_index[_logic.get_magic_card_index(m)]; i++) {
				cards_data[cbPosition++] = _logic.switch_to_card_data(_logic.get_magic_card_index(m));
			}
		}
		for (int i = 0; i < cards_index[_logic.switch_to_card_index(pi_zi_card)]; i++) {
			cards_data[cbPosition++] = pi_zi_card;
		}
		for (int i = 0; i < cards_index[_logic.switch_to_card_index(GameConstants_HZLZG.HZ_MAGIC_CARD)]; i++) {
			cards_data[cbPosition++] = GameConstants_HZLZG.HZ_MAGIC_CARD;
		}

		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (_logic.is_magic_index(i) || i == _logic.switch_to_card_index(GameConstants_HZLZG.HZ_MAGIC_CARD)
			        || i == _logic.switch_to_card_index(pi_zi_card))
				continue;
			if (cards_index[i] > 0) {
				for (int j = 0; j < cards_index[i]; j++) {
					cards_data[cbPosition++] = _logic.switch_to_card_data(i);
				}
			}
		}

		for (int j = 0; j < cbPosition; j++) {
			if (_logic.is_magic_card(cards_data[j])) {
				cards_data[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
			if (cards_data[j] == pi_zi_card) {
				cards_data[j] += GameConstants.CARD_ESPECIAL_TYPE_PI_ZI;
			}
			if (cards_data[j] == GameConstants.HZ_MAGIC_CARD) {
				cards_data[j] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
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
			if (playerStatus.is_chi_hu_round() && has_rule(GameConstants_HZLZG.RULE_TYPE_QIANG_GANG_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
				        GameConstants_HZLZG.HU_CARD_TYPE_QIANG_GANG, i);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
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

		int action = GameConstants_HZLZG.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}

			playerStatus = _playerStatus[i];

			if (GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_HZLZG.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_HZLZG.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data[x] == card) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_HZLZG.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
					        card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_HZLZG.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			}
		}

		int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
		// 长沙麻将吃操作 转转麻将不能吃
		// 这里可能有问题 应该是 |=
		action = _logic.check_chi(GRR._cards_index[chi_seat_index], card);
		if ((action & GameConstants.WIK_LEFT) != 0) {
			_playerStatus[chi_seat_index].add_action(GameConstants.WIK_LEFT);
			_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_LEFT, seat_index);
		}
		if ((action & GameConstants.WIK_CENTER) != 0) {
			_playerStatus[chi_seat_index].add_action(GameConstants.WIK_CENTER);
			_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_CENTER, seat_index);
		}
		if ((action & GameConstants.WIK_RIGHT) != 0) {
			_playerStatus[chi_seat_index].add_action(GameConstants.WIK_RIGHT);
			_playerStatus[chi_seat_index].add_chi(card, GameConstants.WIK_RIGHT, seat_index);
		}

		// 结果判断
		if (_playerStatus[chi_seat_index].has_action()) {
			bAroseAction = true;
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

	public int check_bao_qing(WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount < 2)
			return GameConstants_HZLZG.WIK_NULL;

		int cbCardColor = weaveItems[0].center_card & GameConstants.LOGIC_MASK_COLOR;
		// 组合判断
		for (int i = 0; i < weaveCount; i++) {
			int cbCenterCard = weaveItems[i].center_card;
			if ((cbCenterCard & GameConstants.LOGIC_MASK_COLOR) != cbCardColor)
				return GameConstants_HZLZG.WIK_NULL;
		}

		return GameConstants_HZLZG.WIK_BAO_QING;
	}

	public int check_bao_jiang(WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount < 2)
			return GameConstants_HZLZG.WIK_NULL;

		for (int i = 0; i < weaveCount; i++) {
			if (weaveItems[i].weave_kind == GameConstants.WIK_LEFT
			        || weaveItems[i].weave_kind == GameConstants.WIK_CENTER
			        || weaveItems[i].weave_kind == GameConstants.WIK_RIGHT)
				return GameConstants_HZLZG.WIK_NULL;

			int cbValue = _logic.get_card_value(weaveItems[i].center_card);
			// 单牌统计
			if ((cbValue != 2) && (cbValue != 5) && (cbValue != 8))
				return GameConstants_HZLZG.WIK_NULL;
		}

		return GameConstants_HZLZG.WIK_BAO_JIANG;
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
	public boolean exe_lai_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean self,
	        boolean d) {
		// 是否有抢杠胡
		this.set_handler(this._handler_lai_gang);
		this._handler_lai_gang.reset_status(seat_index, provide_player, center_card, action, type, self, d);
		this._handler.exe(this);

		return true;
	}

	public boolean exe_select_magic_card(int seat_index) {
		set_handler(_handler_select_magic);
		_handler_select_magic.reset_status(seat_index);
		_handler_select_magic.exe(this);
		return true;
	}

	private void initData() {
		special_gang_count = new int[getTablePlayerNumber()][3];
		kai_kou = new boolean[getTablePlayerNumber()];
		bao_pai = new int[getTablePlayerNumber()];
		hu_fan = new int[getTablePlayerNumber()];
		quan_bao = new int[getTablePlayerNumber()];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			quan_bao[i] = -1;
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
				// for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
				// for (int k = 0; k < getTablePlayerNumber(); k++) {
				// lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
				// }
				// }

				for (int j = 0; j < getTablePlayerNumber(); j++) {
					_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
				}
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
				game_end.addGangScore(getPersonFan(i) == 0 ? 0 : 1 << getPersonFan(i));
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
						cs.addItem(GRR._cards_data[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
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
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
		        || reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
		        || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
		        || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
		        || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
		        || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
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
	 * 刷新玩家的牌--手牌 和 牌堆上的牌
	 * 
	 * @param seat_index
	 * @param card_count
	 * @param cards
	 *            实际的牌数据(手上的牌)
	 * @param weave_count
	 * @param weaveitems
	 *            牌堆中的组合(落地的牌)
	 * @return
	 */
	@Override
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count,
	        WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setScoreType(getPersonFan(seat_index) == 0 ? 0 : 1 << getPersonFan(seat_index));
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);
		// 组合牌
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

		this.send_response_to_other(seat_index, roomResponse);

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	@Override
	protected boolean on_game_start() {
		_logic.clean_magic_cards();
		pi_zi_card = 0;

		initData();

		exe_select_magic_card(GRR._banker_player);

		_game_status = GameConstants_HZLZG.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_HZLZG.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_HZLZG.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
			        _current_player == GameConstants_HZLZG.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_HZLZG.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
			        GRR._weave_items[i], GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants_HZLZG.DispatchCard_Type_Tian_Hu,
		        GameConstants_HZLZG.DELAY_SEND_CARD_DELAY);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_HZLZG.MAX_INDEX];
		for (int i = 0; i < GameConstants_HZLZG.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_HZLZG.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants_HZLZG.MAX_ZI + 3;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_HZLZG.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount,
			        cbCurrentCard, chr, Constants_HuangZhou.HU_CARD_TYPE_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				if (_logic.is_magic_card(cbCurrentCard))
					cards[count] += GameConstants_HZLZG.CARD_ESPECIAL_TYPE_LAI_ZI;
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

		if (cbCardIndexTemp[_logic.switch_to_card_index(GameConstants_HZLZG.HZ_MAGIC_CARD)] != 0)
			return false;

		return true;
	}

	private int getPersonFan(int seat_index) {
		int fan = 0;
		if (kai_kou[seat_index])
			fan++;
		fan += special_gang_count[seat_index][0] * 2;
		fan += special_gang_count[seat_index][1];
		fan += special_gang_count[seat_index][2];
		fan += _player_result.ming_gang_count[seat_index];
		fan += _player_result.an_gang_count[seat_index];

		return fan;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
	        ChiHuRight chiHuRight, int card_type, int _seat_index) {
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		if (weave_count == 0)
			return GameConstants.WIK_NULL;
		if (cards_index[_logic.switch_to_card_index(pi_zi_card)] != 0)
			return GameConstants.WIK_NULL;
		if (cards_index[_logic.switch_to_card_index(GameConstants_HZLZG.HZ_MAGIC_CARD)] != 0)
			return GameConstants.WIK_NULL;
		if (cur_card == pi_zi_card)
			return GameConstants.WIK_NULL;
		if (cur_card == GameConstants_HZLZG.HZ_MAGIC_CARD)
			return GameConstants.WIK_NULL;

		boolean da_hu = false;
		int hu_typa_fan = 0;

		boolean is_qing_yi_se = _logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_qing_yi_se == true && bao_pai[_seat_index] != GameConstants_HZLZG.BAO_GUO
		        && bao_pai[_seat_index] != GameConstants_HZLZG.BAO_JIANG) {
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_QING_YI_SE);
			da_hu = true;
			hu_typa_fan += 1;
		}
		boolean is_jiang_yis_se = _logic.is_jiangjiang_hu(cards_index, weaveItems, weave_count, cur_card);
		if (is_jiang_yis_se == true && bao_pai[_seat_index] != GameConstants_HZLZG.BAO_GUO
		        && bao_pai[_seat_index] != GameConstants_HZLZG.BAO_QING) {
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_JIANG_YI_SE);
			da_hu = true;
			hu_typa_fan += 1;
		}

		boolean is_feng_yi_se = is_feng_is_se(cards_index, weaveItems, weave_count, cur_card);
		if (is_feng_yi_se == true) {
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_FENG_YI_SE);
			da_hu = true;
			hu_typa_fan += 1;
		}

		int[] cbCardIndexTemp = Arrays.copyOf(cards_index, cards_index.length);
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		boolean eyes_is_258 = false;
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
		        new int[] {}, 0);
		if (!bValue) {
			bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
			        _logic.get_all_magic_card_index(), _logic.get_magic_card_count());
		} else {
			// 赖子当正常牌使用
		}
		if (bValue) {
			for (AnalyseItem analyseItem : analyseItemArray) {
				if (_logic.is_pengpeng_hu(analyseItem)) {
					chiHuRight.opr_or(GameConstants_HZLZG.CHR_PENG_PENG_HU);
					da_hu = true;
					hu_typa_fan += 1;
				}
				int cbCardValue = _logic.get_card_value(analyseItem.cbCardEye);
				if (cbCardValue == 2 || cbCardValue == 5 || cbCardValue == 8)
					eyes_is_258 = true;
			}

			int card_count = _logic.get_card_count_by_index(cards_index);
			if (card_count == 1) {
				chiHuRight.opr_or(GameConstants_HZLZG.CHR_QUAN_QIU_REN);
			}
		}

		boolean hu = bValue | is_feng_yi_se | is_jiang_yis_se;
		if (!hu) {
			chiHuRight.set_empty();
			return GameConstants_HZLZG.WIK_NULL;
		}
		if (bao_pai[_seat_index] == GameConstants_HZLZG.BAO_QING
		        && chiHuRight.opr_and(GameConstants_HZLZG.CHR_QING_YI_SE).is_empty()) {
			chiHuRight.set_empty();
			return GameConstants_HZLZG.WIK_NULL;
		}
		if (bao_pai[_seat_index] == GameConstants_HZLZG.BAO_JIANG
		        && chiHuRight.opr_and(GameConstants_HZLZG.CHR_JIANG_YI_SE).is_empty()) {
			chiHuRight.set_empty();
			return GameConstants_HZLZG.WIK_NULL;
		}

		if (card_type == GameConstants_HZLZG.HU_CARD_TYPE_ZI_MO) { // 自摸
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_ZI_MO);
		} else if (card_type == GameConstants_HZLZG.HU_CARD_TYPE_QIANG_GANG) { // 抢杠
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_QIANG_GANG_HU);
			hu_typa_fan += 2;
			da_hu = true;
		} else if ((card_type & GameConstants_HZLZG.HU_CARD_TYPE_GANG_KAI_HUA) != 0) { // 杠上花
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_GANG_KAI_HUA);
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_ZI_MO); // 杠上花算自摸
			hu_typa_fan += 2;
			da_hu = true;
		} else if (card_type == GameConstants_HZLZG.HU_CARD_TYPE_JIE_PAO) { // 接炮
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_SHU_FAN);
		}
		if (GRR._left_card_count <= 14) {
			chiHuRight.opr_or(GameConstants_HZLZG.CHR_HAI_DI_HU);
			hu_typa_fan += 2;
			da_hu = true;
		}

		if (!da_hu) {
			if (_logic.get_magic_card_count() > 1 || !eyes_is_258) {
				chiHuRight.set_empty();
				return GameConstants_HZLZG.WIK_NULL;
			}

			chiHuRight.opr_or(GameConstants_NanNing.CHR_JI_BEN_HU); // 基本胡
		}

		hu_fan[_seat_index] = hu_typa_fan + getPersonFan(_seat_index);

		if (card_type != GameConstants_HZLZG.HU_CARD_TYPE_ZI_MO) {
			if (hu_fan[_seat_index] >= 3) {
				return GameConstants_HZLZG.WIK_CHI_HU;
			}
		} else {
			if (hu_fan[_seat_index] >= 3) {
				return GameConstants_HZLZG.WIK_CHI_HU;
			}

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				int fan = getPersonFan(i);
				if (fan >= 3)
					return GameConstants_HZLZG.WIK_CHI_HU;
			}
		}

		chiHuRight.set_empty();
		return GameConstants_HZLZG.WIK_NULL;
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;

		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		double di_fen = 0;
		int max_fen = 0;
		if (has_rule(GameConstants_HZLZG.RULE_TYPE_JIN_DING_10)) {
			di_fen = 0.25;
			max_fen = 10;
		}
		if (has_rule(GameConstants_HZLZG.RULE_TYPE_JIN_DING_20)) {
			di_fen = 0.5;
			max_fen = 20;
		}
		if (has_rule(GameConstants_HZLZG.RULE_TYPE_JIN_DING_50)) {
			di_fen = 1.25;
			max_fen = 50;
		}
		if (has_rule(GameConstants_HZLZG.RULE_TYPE_JIN_DING_100)) {
			di_fen = 2.5;
			max_fen = 100;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int all_fan = (hu_fan[seat_index] * getPersonFan(i));
				GRR._lost_fan_shu[i][seat_index] = all_fan;
			}
		} else {
			int all_fan = (hu_fan[seat_index] * getPersonFan(provide_index));
			GRR._lost_fan_shu[provide_index][seat_index] = all_fan;
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index)
					continue;

				int all_fan = (hu_fan[seat_index] * getPersonFan(i));
				if (all_fan > 5)
					GRR._chi_hu_rights[i].opr_or(GameConstants_HZLZG.CHR_JING_DING);

				int s = (int) di_fen * all_fan;
				if (s > max_fen)
					s = max_fen;
				if (quan_bao[seat_index] != -1) {
					GRR._game_score[quan_bao[seat_index]] -= s;
				} else {
					GRR._game_score[i] -= s;
				}
				GRR._game_score[seat_index] += s;
			}
		} else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants_HZLZG.CHR_FANG_PAO);

			int all_fan = (hu_fan[seat_index] * getPersonFan(provide_index));
			int s = (int) di_fen * all_fan;
			if (s > max_fen)
				s = max_fen;

			if (!chr.opr_and(GameConstants_HZLZG.CHR_QUAN_QIU_REN).is_empty()
			        && _playerStatus[provide_index]._hu_card_count > 0) {
				s *= 3;
			} else if (!chr.opr_and(GameConstants_HZLZG.CHR_QIANG_GANG_HU).is_empty()) {
				s *= 3;
			}

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		GRR._provider[seat_index] = provide_index;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);

		return;
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
					if (type == GameConstants_HZLZG.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == GameConstants_HZLZG.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == GameConstants_HZLZG.CHR_JIANG_YI_SE) {
						result.append(" 将一色");
					}
					if (type == GameConstants_HZLZG.CHR_FENG_YI_SE) {
						result.append(" 风一色");
					}
					if (type == GameConstants_HZLZG.CHR_GANG_KAI_HUA) {
						result.append(" 杠上开花");
					}
					if (type == GameConstants_HZLZG.CHR_HAI_DI_HU) {
						result.append(" 海底胡");
					}
					if (type == GameConstants_HZLZG.CHR_QIANG_GANG_HU) {
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_HZLZG.CHR_HUNAN_QI_XIAO_DUI) {
						result.append(" 七小对");
					}
					if (type == GameConstants_HZLZG.CHR_QUAN_QIU_REN) {
						result.append(" 全求人");
					}

				} else if (type == GameConstants_HZLZG.CHR_FANG_PAO) {
					result.append(" 放炮");
				} else if (type == GameConstants_HZLZG.CHR_JING_DING) {
					result.append(" 金顶");
				}
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
			if (kai_kou[player]) {
				result.append(" 开口");
			}
			if (special_gang_count[player][0] > 0) {
				result.append(" 赖子杠X" + special_gang_count[player][0]);
			}
			if (special_gang_count[player][1] > 0) {
				result.append(" 痞子杠X" + special_gang_count[player][1]);
			}
			if (special_gang_count[player][2] > 0) {
				result.append(" 红中杠X" + special_gang_count[player][2]);
			}

			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x35 };
		int[] cards_of_player1 = new int[] { 0x34, 0x34, 0x37, 0x31, 0x31, 0x36, 0x32, 0x32, 0x36, 0x33, 0x35, 0x35,
		        0x37 };
		int[] cards_of_player3 = new int[] { 0x34, 0x34, 0x37, 0x31, 0x31, 0x36, 0x32, 0x32, 0x36, 0x33, 0x35, 0x35,
		        0x37 };
		int[] cards_of_player2 = new int[] { 0x34, 0x34, 0x37, 0x31, 0x31, 0x36, 0x32, 0x32, 0x36, 0x33, 0x35, 0x35,
		        0x37 };

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
