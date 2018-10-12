package com.cai.game.hh.handler.new_czphz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.Constants_New_ChenZhou;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.Player;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GangCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.hh.HHGameLogic.AnalyseItem;
import com.cai.game.hh.HHTable;
import com.cai.redis.service.RedisService;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.phz.ScoreRecord.Score_Record;

public class Table_New_ChenZhou extends HHTable {

	private static final long serialVersionUID = -6411443691444147984L;

	private HandlerPiao_ChenZhou _handler_piao;

	public int[] shoot_count = new int[getTablePlayerNumber()];

	public int time_for_animation = 800; // 发牌动画的时间(ms)
	public int time_for_organize = 100; // 理牌的时间(ms)
	public int time_for_operate_dragon = 450; // 发牌到执行提的延时(ms)
	public int time_for_add_discard = 450; // 加入废牌堆的延时(ms)
	public int time_for_dispatch_card = 950; // 发牌的延时(ms)
	public int time_for_deal_first_card = 450; // 处理第一张牌的延时(ms)
	public int time_for_force_win = 0; // 强制胡牌的延时(ms)
	public int time_for_display_win_border = 3; // 点了胡之后，到出现小结算的延时(s)

	public int[][] all_game_round_score = new int[getTablePlayerNumber()][32];
	public int[] total_score = new int[getTablePlayerNumber()];

	public Table_New_ChenZhou() {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean handler_player_be_in_room(int seat_index) {
		if ((GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) && get_players()[seat_index] != null) {
			if (_handler != null)
				_handler.handler_player_be_in_room(this, seat_index);
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

				send_response_to_player(seat_index, roomResponse);
			}
		}

		if (is_sys())
			return true;

		if (getRuleValue(GameConstants.GAME_RULE_CAN_LESS) != 0 || _cur_round > 0)
			return handler_player_ready(seat_index, false);

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean exe_gang(int seat_index, int provide_player, int center_card, int action, int type, boolean depatch, boolean self, boolean d,
			int delay) {
		if (delay > 0) {
			GameSchedule.put(new GangCardRunnable(getRoom_id(), seat_index, provide_player, center_card, action, type, depatch, self, d), delay,
					TimeUnit.MILLISECONDS);
		} else {
			_handler = _handler_gang;
			_handler_gang.reset_status(seat_index, provide_player, center_card, action, type, depatch, self, d);
			_handler.exe(this);
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
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	@Override
	public void process_chi_hu_player_operate(int seat_index, int operate_card, boolean rm) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_HU, 1, chr.type_list, 1, GameConstants.INVALID_SEAT);

		for (int i = 0; i < _hu_weave_count[seat_index]; i++) {
			switch (_hu_weave_items[seat_index][i].weave_kind) {
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_PAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_CHOU_WEI:
			case GameConstants.WIK_DUI_ZI:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_LEFT:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card + 2 == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_CENTER:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card + 1 == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_RIGHT:
				if (_hu_weave_items[seat_index][i].center_card == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card - 1 == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_hu_weave_items[seat_index][i].center_card - 2 == operate_card) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_EQS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(_hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 2) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 7) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_YWS:
				if (_logic.get_card_color(operate_card) != _logic.get_card_color(_hu_weave_items[seat_index][i].center_card)) {
					break;
				}
				if (_logic.get_card_value(operate_card) == 1) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 5) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				} else if (_logic.get_card_value(operate_card) == 10) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_XXD:
				if (_logic.get_card_value(operate_card) == _logic.get_card_value(_hu_weave_items[seat_index][i].center_card)) {
					GRR._count_pick_niao = _hu_weave_items[seat_index][i].weave_kind;
					break;
				}
				break;
			}
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);

			operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, GRR._weave_items[i], GRR._weave_count[i],
					GameConstants.INVALID_SEAT);
		}

		return;
	}

	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		// _logic.random_card_data(repertory_card, mj_cards);
		int xi_pai_count = 0;
		int rand = RandomUtil.generateRandomNumber(3, 6);
		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);
			xi_pai_count++;
		}

		int send_count = 20;
		int have_send_count = 0;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			if (count == 2) {
				send_count = GameConstants.MAX_HH_COUNT - 1;
			} else if (count == 3) {
				if (has_rule(Constants_New_ChenZhou.GAME_RULE_15_ZHANG))
					send_count = GameConstants.MAX_FPHZ_COUNT - 1;
				else
					send_count = GameConstants.MAX_HH_COUNT - 1;
			} else if (count == 4) {
				send_count = GameConstants.MAX_FPHZ_COUNT - 1;
			}

			GRR._left_card_count -= send_count;
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);
			have_send_count += send_count;
		}

		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[GameConstants.CARD_COUNT_HH_YX];
		shuffle(_repertory_card, GameConstants.CARD_DATA_HH_YX);


		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		return game_start_HH();
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUEST_SCORE_RECORD && _game_status != GameConstants.GS_MJ_FREE) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SCORE_RECORD);

			Score_Record.Builder sr = Score_Record.newBuilder();
			if (getTablePlayerNumber() == 2) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
			} else if (getTablePlayerNumber() == 3) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
					sr.addScorePlayer3(all_game_round_score[2][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
				sr.setScoreTotal3(total_score[2]);
			} else if (getTablePlayerNumber() == 4) {
				for (int j = 0; j < _cur_round; j++) {
					sr.addScorePlayer1(all_game_round_score[0][j]);
					sr.addScorePlayer2(all_game_round_score[1][j]);
					sr.addScorePlayer3(all_game_round_score[2][j]);
					sr.addScorePlayer4(all_game_round_score[3][j]);
				}
				sr.setScoreTotal1(total_score[0]);
				sr.setScoreTotal2(total_score[1]);
				sr.setScoreTotal3(total_score[2]);
				sr.setScoreTotal4(total_score[3]);
			}

			load_player_info_data(roomResponse);

			roomResponse.setCommResponse(PBUtil.toByteString(sr));

			send_response_to_player(seat_index, roomResponse);
		}

		return true;
	}

	private boolean game_start_HH() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
		}

		if (has_rule(Constants_New_ChenZhou.GAME_RULE_PIAO_123) || has_rule(Constants_New_ChenZhou.GAME_RULE_PIAO_235)) {
			_handler = _handler_piao;
			_handler_piao.exe(this);
		} else {
			on_game_start_real();
		}

		return true;
	}

	public boolean on_game_start_real() {
		int gameId = getGame_id() == 0 ? 275 : getGame_id();

		SysParamModel sysParamModel1105 = null;

		if (has_rule(Constants_New_ChenZhou.GAME_RULE_SPEED_FAST)) {
			sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1205);
		} else {
			sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1105);
		}

		if (sysParamModel1105 != null && sysParamModel1105.getVal1() > 0 && sysParamModel1105.getVal1() < 10000) {
			time_for_animation = sysParamModel1105.getVal1();
		}
		if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
			time_for_organize = sysParamModel1105.getVal2();
		}
		if (sysParamModel1105 != null && sysParamModel1105.getVal3() > 0 && sysParamModel1105.getVal3() < 10000) {
			time_for_operate_dragon = sysParamModel1105.getVal3();
		}
		if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
			time_for_add_discard = sysParamModel1105.getVal4();
		}
		if (sysParamModel1105 != null && sysParamModel1105.getVal5() > 0 && sysParamModel1105.getVal5() < 10000) {
			time_for_dispatch_card = sysParamModel1105.getVal5();
		}

		shoot_count = new int[getTablePlayerNumber()];

		_logic.clean_magic_cards();
		int playerCount = getPlayerCount();
		GRR._banker_player = _current_player = _cur_banker;
		// 游戏开始
		_game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][GameConstants.MAX_HH_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		@SuppressWarnings("unused")
		boolean can_ti = false;
		int ti_card_count[] = new int[getTablePlayerNumber()];
		int ti_card_index[][] = new int[getTablePlayerNumber()][5];

		for (int i = 0; i < playerCount; i++) {
			ti_card_count[i] = _logic.get_action_ti_Card(GRR._cards_index[i], ti_card_index[i]);
			if (ti_card_count[i] > 0)
				can_ti = true;
		}
		@SuppressWarnings("unused")
		int FlashTime = 4000;
		@SuppressWarnings("unused")
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);

			if (_cur_round == 1) {
				// shuffle_players();
				load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants.INVALID_SEAT ? _resume_player : _current_player);
			roomResponse.setLeftCardCount(GRR._left_card_count);
			roomResponse.setGameStatus(_game_status);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 设置发牌动画时间和停留时间
			roomResponse.setFlashTime(time_for_animation);
			roomResponse.setStandTime(time_for_organize);
			send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		load_room_info_data(roomResponse);
		load_common_status(roomResponse);
		load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < playerCount; i++) {
			if (i == GRR._banker_player)
				continue;

			// 起手2提以上，不做听牌
			int tmp_ti_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (GRR._cards_index[i][j] == 4) {
					tmp_ti_count++;
				}
			}

			if (tmp_ti_count > 1)
				continue;

			_playerStatus[i]._hu_card_count = get_hh_ting_card_twenty(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i, i);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}
		_handler = _handler_dispath_firstcards;
		exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, time_for_animation + time_for_organize + time_for_deal_first_card);

		return true;
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		_game_status = GameConstants.GS_MJ_WAIT;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return handler_game_finish_phz(seat_index, reason);
	}

	public boolean handler_game_finish_phz(int seat_index, int reason) {
		int real_reason = reason;

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
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
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间

		if (GRR != null) {
			// 胡的那张牌，是在哪一个组合里面
			game_end.setCountPickNiao(GRR._count_pick_niao);

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			// 利用game_end_response里的win_lzi_fen来表示胡牌的类型：1表示自摸，2表示胡，3表示点炮，4表示流局
			int tmp_win_index = -1;
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (GRR._win_order[i] == 1) {
					tmp_win_index = i;
					break;
				}
			}
			if (tmp_win_index == -1) {
				game_end.setWinLziFen(4);
			} else {
				ChiHuRight tmp_chr = GRR._chi_hu_rights[tmp_win_index];
				if (!tmp_chr.opr_and(Constants_New_ChenZhou.CHR_ZI_MO).is_empty()) {
					game_end.setWinLziFen(1);
				} else if (!tmp_chr.opr_and(Constants_New_ChenZhou.CHR_CHI_HU).is_empty()
						|| !tmp_chr.opr_and(Constants_New_ChenZhou.CHR_SPECAIL_TIAN_HU).is_empty()) {
					game_end.setWinLziFen(2);
				} else if (!tmp_chr.opr_and(Constants_New_ChenZhou.CHR_JEI_PAO_HU).is_empty()) {
					game_end.setWinLziFen(3);
				}
			}

			GRR._end_type = reason;

			load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);
			game_end.setLeftCardCount(GRR._left_card_count);
			int cards[] = new int[GRR._left_card_count];
			int k = 0;
			int left_card_count = GRR._left_card_count;
			for (int i = _all_card_len - GRR._left_card_count; i < _all_card_len; i++) {
				cards[k] = _repertory_card[_all_card_len - left_card_count];
				game_end.addCardsList(cards[k]);

				k++;
				left_card_count--;
			}
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);

				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			set_result_describe(seat_index);
			if (has_rule(GameConstants.GAME_RULE_DI_HUANG_FAN)) {
				if (reason == GameConstants.Game_End_DRAW) {
					_huang_zhang_count++;
				} else {
					_huang_zhang_count = 0;
				}
			}

			for (int i = 0; i < count; i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {

					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();

				if (_hu_weave_count[i] > 0 && GRR._win_order[i] == 1) {
					for (int j = 0; j < _hu_weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(_hu_weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(_hu_weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(_hu_weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(_hu_weave_items[i][j].hu_xi);
						weaveItem_item.setCenterCard(_hu_weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				} else {
					for (int j = 0; j < GRR._weave_count[i]; j++) {
						WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
						weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
						weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
						weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
						weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
						weaveItem_array.addWeaveItem(weaveItem_item);
					}
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
			}
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(process_player_result(reason));
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
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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

		return false;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new HandlerDispatchCard_ChenZhou();
		_handler_out_card_operate = new HandlerOutCardOperate_ChenZhou();
		_handler_gang = new HandlerGang_ChenZhou();
		_handler_chi_peng = new HandlerChiPeng_ChenZhou();
		_handler_chuli_firstcards = new HandlerLiangPai_ChenZhou();
		_handler_dispath_firstcards = new HandlerSanTiWuKan_ChenZhou();

		_handler_piao = new HandlerPiao_ChenZhou();

		all_game_round_score = new int[getTablePlayerNumber()][32];
		total_score = new int[getTablePlayerNumber()];
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		if (_is_xiang_gong[seat_index] == true) {
			return GameConstants.WIK_NULL;
		}

		// 起手提龙2提以上，需要知道进张打出相应的牌之后，才能胡牌
		if (_ti_mul_long[seat_index] > 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			int index = _logic.switch_to_card_index(cur_card);
			cbCardIndexTemp[index]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		_hu_xi[seat_index] = 0;
		int hu_xi[] = new int[1];
		hu_xi[0] = 0;
		boolean yws_type = false;
		boolean bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card, analyseItemArray,
				false, hu_xi, yws_type);

		if (cur_card != 0) {
			for (int i = 0; i < weaveCount; i++) {
				if ((cur_card == weaveItems[i].center_card) && ((weaveItems[i].weave_kind == GameConstants.WIK_PENG && dispatch == true)
						|| (weaveItems[i].weave_kind == GameConstants.WIK_WEI))) {

					int index = _logic.switch_to_card_index(cur_card);
					cbCardIndexTemp[index]--;
					int temp_index = analyseItemArray.size();

					boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
							analyseItemArray, false, hu_xi, yws_type);

					if (temp_index < analyseItemArray.size()) {
						bValue = temp_bValue;
						AnalyseItem analyseItem = new AnalyseItem();
						for (; temp_index < analyseItemArray.size(); temp_index++) {
							analyseItem = analyseItemArray.get(temp_index);
							hu_xi[0] = 0;
							for (int j = 0; j < 7; j++) {
								if ((cur_card == analyseItem.cbCenterCard[j])
										&& ((analyseItem.cbWeaveKind[j] == GameConstants.WIK_PENG && dispatch == true)
												|| analyseItem.cbWeaveKind[j] == GameConstants.WIK_WEI))
									analyseItem.cbWeaveKind[j] = GameConstants.WIK_PAO;
								analyseItem.hu_xi[j] = _logic.get_analyse_hu_xi(analyseItem.cbWeaveKind[j], analyseItem.cbCenterCard[j]);
								hu_xi[0] += analyseItem.hu_xi[j];

							}

						}
					}
					break;
				}
			}

			// 扫牌判断
			WeaveItem sao_WeaveItem = new WeaveItem();
			int cur_index = _logic.switch_to_card_index(cur_card);
			if (cards_index[cur_index] == 3) {
				cbCardIndexTemp[cur_index] = 1;
				sao_WeaveItem.weave_kind = GameConstants.WIK_KAN;
				sao_WeaveItem.center_card = cur_card;
				sao_WeaveItem.hu_xi = _logic.get_weave_hu_xi(sao_WeaveItem);

				int sao_index = analyseItemArray.size();
				boolean temp_bValue = _logic.analyse_card_phz(cbCardIndexTemp, weaveItems, weaveCount, seat_index, provider_index, cur_card,
						analyseItemArray, false, hu_xi, yws_type);
				if (sao_index < analyseItemArray.size()) {
					bValue = temp_bValue;
					for (; sao_index < analyseItemArray.size(); sao_index++) {
						AnalyseItem analyseItem = new AnalyseItem();
						analyseItem = analyseItemArray.get(sao_index);
						for (int j = 0; j < 7; j++) {
							if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL) {
								analyseItem.cbWeaveKind[j] = sao_WeaveItem.weave_kind;
								analyseItem.cbCenterCard[j] = sao_WeaveItem.center_card;
								analyseItem.hu_xi[j] = sao_WeaveItem.hu_xi;
								break;
							}
						}
					}
				}
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		AnalyseItem analyseItem = new AnalyseItem();
		int temp_hu_xi;
		int max_hu_xi = 0;
		int max_hu_index = 0;
		for (int i = 0; i < analyseItemArray.size(); i++) {
			temp_hu_xi = 0;
			analyseItem = analyseItemArray.get(i);

			for (int j = 0; j < 7; j++) {
				if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					break;
				WeaveItem weave_items = new WeaveItem();
				weave_items.center_card = analyseItem.cbCenterCard[j];
				weave_items.weave_kind = analyseItem.cbWeaveKind[j];
				temp_hu_xi += _logic.get_weave_hu_xi(weave_items);
			}
			if (temp_hu_xi > max_hu_xi) {
				max_hu_index = i;
				max_hu_xi = temp_hu_xi;
			}
		}

		if (max_hu_xi == 0) { // 毛胡
			if (has_rule(Constants_New_ChenZhou.GAME_RULE_MAO_HU)) {
				max_hu_xi = 15;
				chiHuRight.opr_or(Constants_New_ChenZhou.CHR_MAO_HU);
			} else {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		} else {
			int basic_hu_xi = get_basic_hu_xi();
			if (max_hu_xi < basic_hu_xi) {
				chiHuRight.set_empty();
				return GameConstants.WIK_NULL;
			}
		}

		hu_xi_hh[0] = max_hu_xi;

		analyseItem = analyseItemArray.get(max_hu_index);

		for (int j = 0; j < 7; j++) {
			if (analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
				break;
			_hu_weave_items[seat_index][j].weave_kind = analyseItem.cbWeaveKind[j];
			_hu_weave_items[seat_index][j].center_card = analyseItem.cbCenterCard[j];
			_hu_weave_items[seat_index][j].hu_xi = _logic.get_weave_hu_xi(_hu_weave_items[seat_index][j]);
			_hu_weave_count[seat_index] = j + 1;
		}

		if (analyseItem.curCardEye == true) {
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].weave_kind = GameConstants.WIK_DUI_ZI;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].center_card = analyseItem.cbCardEye;
			_hu_weave_items[seat_index][_hu_weave_count[seat_index]].hu_xi = 0;
			_hu_weave_count[seat_index]++;
		}

		if (has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN) || has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN_2_FAN)) {
			int hong_pai_count = _logic.calculate_hong_pai_count(analyseItem);

			if (hong_pai_count == 0) {
				chiHuRight.opr_or(Constants_New_ChenZhou.CHR_ALL_HEI);
			} else if (hong_pai_count == 1) {
				chiHuRight.opr_or(Constants_New_ChenZhou.CHR_ONE_HONG);
			} else if (hong_pai_count >= 10) {
				chiHuRight.opr_or(Constants_New_ChenZhou.CHR_TEN_HONG_PAI);
			}
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;
		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(Constants_New_ChenZhou.CHR_ZI_MO);
		} else if (card_type == GameConstants.HU_CARD_TYPE_PAOHU) {
			chiHuRight.opr_or(Constants_New_ChenZhou.CHR_JEI_PAO_HU);
		} else if (card_type == GameConstants.HU_CARD_TYPE_FAN_PAI) {
			chiHuRight.opr_or(Constants_New_ChenZhou.CHR_CHI_HU);
		}

		return cbChiHuKind;
	}

	public boolean check_first_out_card() {
		boolean is_first = false;
		if (has_rule(Constants_New_ChenZhou.GAME_RULE_PLAYER_2)) {
			if (GRR._left_card_count == 39 && _out_card_count == 1)
				is_first = true;
		} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_PLAYER_3)) {
			if (has_rule(Constants_New_ChenZhou.GAME_RULE_15_ZHANG)) {
				if (GRR._left_card_count == 37 && _out_card_count == 1)
					is_first = true;
			} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_21_ZHANG)) {
				if (GRR._left_card_count == 19 && _out_card_count == 1)
					is_first = true;
			}
		} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_PLAYER_4)) {
			if (GRR._left_card_count == 23 && _out_card_count == 1)
				is_first = true;
		}
		return is_first;
	}

	protected int get_hu_xi_mei_tun() {
		if (has_rule(Constants_New_ChenZhou.GAME_RULE_3_HU_XI_MEI_TUN))
			return 3;
		return 1;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int all_hu_xi = 0;
		for (int i = 0; i < _hu_weave_count[seat_index]; i++) {
			all_hu_xi += _hu_weave_items[seat_index][i].hu_xi;
		}

		if (all_hu_xi == 0) { // 毛胡
			if (has_rule(Constants_New_ChenZhou.GAME_RULE_MAO_HU)) {
				all_hu_xi = 15;
			}
		}

		_hu_xi[seat_index] = all_hu_xi;

		int wTunShu = 1;
		if (has_rule(Constants_New_ChenZhou.GAME_RULE_3_HU_XI_MEI_TUN)) {
			wTunShu = 1 + (all_hu_xi - get_basic_hu_xi()) / get_hu_xi_mei_tun();
		} else {
			if (has_rule(Constants_New_ChenZhou.GAME_RULE_SUAN_FEN_2)) {
				wTunShu = 1 + (all_hu_xi - get_basic_hu_xi()) / get_hu_xi_mei_tun();
			} else {
				wTunShu = all_hu_xi / get_hu_xi_mei_tun();
			}
		}

		int wFanShu = get_fan_shu(chr);

		int wBeiShu = get_bei_shu(chr);

		int lChiHuScore = wTunShu * wFanShu * wBeiShu;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				GRR._lost_fan_shu[i][seat_index] = lChiHuScore;
			}
		} else {
			GRR._lost_fan_shu[provide_index][seat_index] = lChiHuScore * (getTablePlayerNumber() - 1);
		}

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;

				if (has_rule(Constants_New_ChenZhou.GAME_RULE_PIAO_123) || has_rule(Constants_New_ChenZhou.GAME_RULE_PIAO_235))
					s += _player_result.pao[i] + _player_result.pao[seat_index];

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;

				all_game_round_score[i][_cur_round - 1] -= s;
				all_game_round_score[seat_index][_cur_round - 1] += s;
			}
		} else {
			int s = lChiHuScore * getTablePlayerNumber();

			if (has_rule(Constants_New_ChenZhou.GAME_RULE_PIAO_123) || has_rule(Constants_New_ChenZhou.GAME_RULE_PIAO_235))
				s += _player_result.pao[provide_index] + _player_result.pao[seat_index];

			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;

			all_game_round_score[provide_index][_cur_round - 1] -= s;
			all_game_round_score[seat_index][_cur_round - 1] += s;

			GRR._chi_hu_rights[provide_index].opr_or(Constants_New_ChenZhou.CHR_PHZ_FANG_PAO);
		}

		// process_xiang_gong();

		GRR._provider[seat_index] = provide_index;
	}

	protected void process_xiang_gong() {
		for (int i = 0; i < getTablePlayerNumber(); i++) { // 相公牌，罚囤，其他每个玩家没人一囤
			if (_is_xiang_gong[i] == true) {
				for (int j = 0; j < getTablePlayerNumber(); j++) {
					if (i == j)
						GRR._game_score[j] -= (getTablePlayerNumber() - 1);
					else
						GRR._game_score[j] += 1;
				}
			}
		}
	}

	protected int get_xiang_gong_tun_count(int[] cards_index, WeaveItem[] weave_items, int weave_count) {
		int tun = 0;

		int all_hu_xi = 0;
		for (int i = 0; i < weave_count; i++) {
			all_hu_xi += _logic.get_weave_hu_xi(weave_items[i]);
		}
		for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
			if (cards_index[j] == 4) {
				if (j < 10)
					all_hu_xi += 12;
				else
					all_hu_xi += 9;
			}
			if (cards_index[j] == 3) {
				if (j < 10)
					all_hu_xi += 6;
				else
					all_hu_xi += 3;
			}
		}

		tun = 1 + (all_hu_xi - get_basic_hu_xi()) / get_hu_xi_mei_tun();

		return tun;
	}

	protected int get_basic_hu_xi() {
		if (has_rule(Constants_New_ChenZhou.GAME_RULE_3_HU_QI_HU)) {
			return 3;
		} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_6_HU_QI_HU)) {
			return 6;
		} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_9_HU_QI_HU)) {
			return 9;
		}

		return 6;
	}

	protected int get_fan_shu(ChiHuRight chr) {
		int fan = 1;

		if (has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN_2_FAN)) {
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_TEN_HONG_PAI).is_empty() || !chr.opr_and(Constants_New_ChenZhou.CHR_ONE_HONG).is_empty()
					|| !chr.opr_and(Constants_New_ChenZhou.CHR_ALL_HEI).is_empty())
				fan = 2;
		} else if (has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN)) {
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_TEN_HONG_PAI).is_empty())
				fan = 2;
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_ONE_HONG).is_empty())
				fan = 3;
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_ALL_HEI).is_empty())
				fan = 5;
		}

		return fan;
	}

	protected int get_bei_shu(ChiHuRight chr) {
		int bei = 1;

		if (has_rule(Constants_New_ChenZhou.GAME_RULE_ZI_MO_DOUBLE)) {
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_ZI_MO).is_empty())
				bei *= 2;
		}

		if (has_rule(Constants_New_ChenZhou.GAME_RULE_TIAN_DI_HU)) {
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_TIAN_HU).is_empty())
				bei *= 2;
			if (!chr.opr_and(Constants_New_ChenZhou.CHR_DI_HU).is_empty())
				bei *= 2;
		}

		return bei;
	}

	@Override
	public void set_result_describe(int seat_index) {
		int chr_count;
		long chr_type = 0;
		for (int player = 0; player < getTablePlayerNumber(); player++) {
			StringBuilder gameDesc = new StringBuilder("");
			boolean hasFirst = false;

			chr_count = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chr_count; typeIndex++) {
				chr_type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (chr_type == Constants_New_ChenZhou.CHR_CHI_HU) {
						if (hasFirst) {
							gameDesc.append(" 胡");
						} else {
							gameDesc.append("胡");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_ZI_MO) {
						if (hasFirst) {
							gameDesc.append(" 自摸2倍");
						} else {
							gameDesc.append("自摸2倍");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_JEI_PAO_HU) {
						if (hasFirst) {
							gameDesc.append(" 接炮");
						} else {
							gameDesc.append("接炮");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_TIAN_HU) {
						if (hasFirst) {
							gameDesc.append(" 天胡2倍");
						} else {
							gameDesc.append("天胡2倍");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_DI_HU) {
						if (hasFirst) {
							gameDesc.append(" 地胡2倍");
						} else {
							gameDesc.append("地胡2倍");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_SPECAIL_TIAN_HU) {
						if (hasFirst) {
							gameDesc.append(" 特殊天胡");
						} else {
							gameDesc.append("特殊天胡");
							hasFirst = true;
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_TEN_HONG_PAI) {
						if (has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN_2_FAN)) {
							if (hasFirst) {
								gameDesc.append(" 红胡2番");
							} else {
								gameDesc.append("红胡2番");
								hasFirst = true;
							}
						} else {
							if (hasFirst) {
								gameDesc.append(" 红胡2番");
							} else {
								gameDesc.append("红胡2番");
								hasFirst = true;
							}
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_ONE_HONG) {
						if (has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN_2_FAN)) {
							if (hasFirst) {
								gameDesc.append(" 一点朱2番");
							} else {
								gameDesc.append("一点朱2番");
								hasFirst = true;
							}
						} else {
							if (hasFirst) {
								gameDesc.append(" 一点朱3番");
							} else {
								gameDesc.append("一点朱3番");
								hasFirst = true;
							}
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_ALL_HEI) {
						if (has_rule(Constants_New_ChenZhou.GAME_RULE_HONG_HEI_DIAN_2_FAN)) {
							if (hasFirst) {
								gameDesc.append(" 黑胡2番");
							} else {
								gameDesc.append("黑胡2番");
								hasFirst = true;
							}
						} else {
							if (hasFirst) {
								gameDesc.append(" 黑胡5番");
							} else {
								gameDesc.append("黑胡5番");
								hasFirst = true;
							}
						}
					}
					if (chr_type == Constants_New_ChenZhou.CHR_MAO_HU) {
						if (hasFirst) {
							gameDesc.append(" 毛胡");
						} else {
							gameDesc.append("毛胡");
							hasFirst = true;
						}
					}
				} else if (chr_type == Constants_New_ChenZhou.CHR_PHZ_FANG_PAO) {
					if (hasFirst) {
						gameDesc.append(" 放炮");
					} else {
						gameDesc.append("放炮");
						hasFirst = true;
					}
				}
			}

			GRR._result_des[player] = gameDesc.toString();
		}
	}

	protected int get_hong_pai_count(WeaveItem weaveItems[], int weaveCount, int cards_index[]) {
		int count = 0;

		for (int i = 0; i < weaveCount; i++) {
			switch (weaveItems[i].weave_kind) {
			case GameConstants.WIK_TI_LONG:
			case GameConstants.WIK_AN_LONG:
			case GameConstants.WIK_PAO:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 4;
				break;
			case GameConstants.WIK_SAO:
			case GameConstants.WIK_PENG:
			case GameConstants.WIK_CHOU_SAO:
			case GameConstants.WIK_KAN:
			case GameConstants.WIK_WEI:
			case GameConstants.WIK_XIAO:
			case GameConstants.WIK_CHOU_XIAO:
			case GameConstants.WIK_CHOU_WEI:
				if (_logic.color_hei(weaveItems[i].center_card) == false)
					count += 3;
				break;
			}
		}

		int hand_cards_data[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(cards_index, hand_cards_data);
		for (int i = 0; i < hand_card_count; i++) {
			if (_logic.color_hei(hand_cards_data[i]) == false)
				count++;
		}

		return count;
	}

	@Override
	public int get_real_card(int card) {
		if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT && card < GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
		} else if (card > GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT) {
			card -= GameConstants.CARD_ESPECIAL_TYPE_CAN_SHOOT;
		}

		return card;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x11, 0x11, 0x11, 0x12, 0x12, 0x12, 0x15, 0x15, 0x15, 0x17, 0x17,
				0x17, 0x19, 0x19 };
		int[] cards_of_player1 = new int[] { 0x07, 0x07, 0x07, 0x07, 0x0a, 0x0a, 0x0a, 0x0a, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x11, 0x12, 0x13,
				0x14, 0x15, 0x16 };
		int[] cards_of_player2 = new int[] { 0x07, 0x07, 0x07, 0x07, 0x0a, 0x0a, 0x0a, 0x0a, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x11, 0x12, 0x13,
				0x14, 0x15, 0x16 };
		int[] cards_of_player3 = new int[] { 0x07, 0x07, 0x07, 0x07, 0x0a, 0x0a, 0x0a, 0x0a, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x11, 0x12, 0x13,
				0x14, 0x15, 0x16 };

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (getTablePlayerNumber() == 3) {
			for (int j = 0; j < GameConstants.MAX_HH_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
			}
		} else {
			for (int j = 0; j < GameConstants.MAX_FPHZ_COUNT - 1; j++) {
				GRR._cards_index[0][_logic.switch_to_card_index(cards_of_player0[j])] += 1;
				GRR._cards_index[1][_logic.switch_to_card_index(cards_of_player1[j])] += 1;
				GRR._cards_index[2][_logic.switch_to_card_index(cards_of_player2[j])] += 1;
				GRR._cards_index[3][_logic.switch_to_card_index(cards_of_player3[j])] += 1;
			}
		}

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 20) {
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
	public void countChiHuTimes(int _seat_index, boolean zimo) {
		ChiHuRight chiHuRight = GRR._chi_hu_rights[_seat_index];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.game_score[i] += GRR._game_score[i];
			total_score[i] = (int) _player_result.game_score[i];
		}

		if (!(chiHuRight.opr_and(Constants_New_ChenZhou.CHR_TEN_HONG_PAI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_New_ChenZhou.CHR_ONE_HONG)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_New_ChenZhou.CHR_ALL_HEI)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_New_ChenZhou.CHR_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_New_ChenZhou.CHR_SPECAIL_TIAN_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
		if (!(chiHuRight.opr_and(Constants_New_ChenZhou.CHR_MAO_HU)).is_empty()) {
			_player_result.ming_tang_count[_seat_index]++;
		}
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int mj_count = GameConstants.MAX_HH_INDEX;

		for (int i = 0; i < mj_count; i++) {
			if (_logic.is_magic_index(i))
				continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int hu_xi[] = new int[1];
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, provate_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (count >= mj_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean is_card_has_wei(int card) {
		boolean bTmp = false;

		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_has_wei[i] != 0) {
				if (i == _logic.switch_to_card_index(card)) {
					bTmp = true;
					break;
				}
			}
		}

		return bTmp;
	}

	// 是否听牌
	public boolean is_ting_state(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();
		chr.set_empty();
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			int cbCurrentCard = _logic.switch_to_card_data(i);
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard, chr,
					GameConstants.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
				return true;
		}
		return false;
	}
}
