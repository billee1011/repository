package com.cai.game.hongershi.pj;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_ChenZhou;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.hongershi.HandlerChiPeng;
import com.cai.game.hongershi.HandlerChuLiFirstCard;
import com.cai.game.hongershi.HandlerDispatchCard;
import com.cai.game.hongershi.HandlerDispatchFirstCard;
import com.cai.game.hongershi.HandlerGang;
import com.cai.game.hongershi.HandlerOutCardOperate;
import com.cai.game.hongershi.Handler_Bao_Ting;
import com.cai.game.hongershi.Handler_first_Operate;
import com.cai.game.hongershi.HongErShiGameLogic;
import com.cai.game.hongershi.HongErShiTable;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.ChiGroupCard;
import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomRequest;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.hes.HesRsp.PlayerMoCard;
import protobuf.clazz.hes.HesRsp.TableResponse_HES;
import protobuf.clazz.hongershi.HongErShiRsp.Operate_Card_HongErShi;

public class HongErShiTable_PJ extends HongErShiTable {

	private static final long serialVersionUID = 9143145499506743788L;

	// public HongErShiGameLogic logic;
	// public Handler_first_Operate handler_first_Operate;
	// public Handler_Bao_Ting handler_bao_ting;

	// public int[] cards;
	// public boolean[] touCards; // 玩家是否偷牌 或者起手流程中是否暗杠、暗碰
	// public boolean[] baoTing; // 玩家是否报听
	// public boolean isBegin;
	// public boolean[] anCards;
	// // public GangCardResult[] m_gangCardResult;
	// public HandlerChiPeng _handler_chi_peng;
	// public boolean[] has_king_tou;
	// public int[][] all_end;
	// public int[] cha_jiao_limit;
	// public String[] orther_des;
	//
	// public boolean is_mo_or_show; // 摸牌有两种,一种是摸到手上true, 一种在牌桌上

	public HongErShiTable_PJ() {
		super();
	}

	public void progress_banker_select() {
		if (_cur_banker == -1) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	protected boolean on_handler_game_start() {
		_game_status = GameConstants.GS_MJ_PLAY;

		reset_init_data();

		this.progress_banker_select();

		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		_repertory_card = new int[cards.length];
		shuffle(_repertory_card, cards);

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE) {
			test_cards();
		}
		// test_cards();
		return game_start();
	}

	public boolean reset_init_data() {

		Arrays.fill(touCards, false);
		Arrays.fill(anCards, false);
		Arrays.fill(baoTing, false);

		super.reset_init_data();

		GRR = new GameRoundRecord(GameConstants.MAX_WEAVE_HH, HongErShiConstants.MAX_HAND_CARD_COUNT, HongErShiConstants.MAX_CARD_INDEX);
		GRR.setRoom(this);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;
		isBegin = true;

		// 新建
		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		istrustee = new boolean[this.getTablePlayerNumber()];
		player_bao_ting = new boolean[getTablePlayerNumber()];
		has_king_tou = new boolean[getTablePlayerNumber()];
		cha_jiao_limit = new int[getTablePlayerNumber()];
		orther_des = new String[getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(HongErShiConstants.MAX_INDEX_COUNT);
			m_gangCardResult[i] = new GangCardResult();
			orther_des[i] = "";
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		GRR._video_recode.setBankerPlayer(this._cur_banker);

		return true;
	}

	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				logic.random_card_data(repertory_card, mj_cards);
			else
				logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;

		int count = getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			send_count = HongErShiConstants.HAND_CARD_COUNT;
			GRR._left_card_count -= send_count;
			for (int j = 0; j < send_count; j++) {
				this.GRR._cards_data[i][j] = repertory_card[send_count * i + j];
				GRR._card_count[i] = send_count;
			}
		}
		// 记录初始牌型
		_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
	}

	private boolean game_start() {
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.pao[i] = 0;
		}

		int playerCount = getPlayerCount();
		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[playerCount][HongErShiConstants.HAND_CARD_COUNT];
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			for (int j = 0; j < HongErShiConstants.HAND_CARD_COUNT; j++) {
				hand_cards[i][j] = this.GRR._cards_data[i][j];
			}
		}

		int flashTime = 4000;
		int standTime = 1000;
		// 发送数据
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < HongErShiConstants.HAND_CARD_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}
			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_player_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);

			int gameId = this.getGame_id() == 0 ? 8 : this.getGame_id();
			SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
			if (sysParamModel1104 != null && sysParamModel1104.getVal1() > 0 && sysParamModel1104.getVal1() < 10000) {
				flashTime = sysParamModel1104.getVal1();
			}
			if (sysParamModel1104 != null && sysParamModel1104.getVal2() > 0 && sysParamModel1104.getVal2() < 10000) {
				standTime = sysParamModel1104.getVal2();
			}
			roomResponse.setFlashTime(flashTime);
			roomResponse.setStandTime(standTime);
			this.send_response_to_player(i, roomResponse);
		}
		// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < playerCount; i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < HongErShiConstants.HAND_CARD_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}

		roomResponse.setGameStart(gameStartResponse);

		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		this._handler = this._handler_dispath_firstcards;
		this.exe_dispatch_first_card(_current_player, GameConstants.WIK_NULL, flashTime + standTime);

		return true;
	}

	public void change_player_status(int seat_index, int st) {
		PlayerStatus curPlayerStatus = _playerStatus[seat_index];
		curPlayerStatus.set_status(st);// 操作状态
	}

	/**
	 * 
	 * @return
	 */
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

			boolean can_hu_this_card = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_hu();
			for (int x = 0; x < GameConstants.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_hu_this_card = false;
					break;
				}
			}
			if (!can_hu_this_card) {
				continue;
			}

			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				action = analyse_chi_hu_card(i, seat_index, card, HongErShiConstants.WIK_QIANG_GANG, chr, true);
				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(HongErShiConstants.WIK_CHI_HU);
					_playerStatus[i].add_action(GameConstants.WIK_NULL);
					_playerStatus[i].add_tou(card, HongErShiConstants.WIK_CHI_HU, seat_index);
					chr.opr_or(HongErShiConstants.WIK_QIANG_GANG);// 抢杠
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

	/**
	 * 
	 * @param seat_index
	 * @param card
	 * @param bDisdatch
	 *            true：摸牌时,false：打牌时
	 * @return
	 */
	public boolean estimate_player_card_respond(int seat_index, int card, boolean bDisdatch) {

		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// _playerStatus[i].clean_action();
		// _playerStatus[i].clean_weave();
		// }

		boolean bAroseAction = false;

		// TODO: 持牌最多只有两个人吃
		for (int chi = 0; chi < 2; chi++) {
			int next_seat = (seat_index + chi + getTablePlayerNumber()) % getTablePlayerNumber();
			if (next_seat == seat_index && !bDisdatch) {
				continue;
			}
			// TODO：下家 在没有选择吃上家玩法时,无法吃牌
			if (chi == 1 && !has_rule(HongErShiConstants.RULE_NENG_CHI_SHANG_JIA)) {
				continue;
			}

			if (super.player_bao_ting[next_seat]) {
				continue;
			}

			int action = logic.check_chi(GRR._cards_data[next_seat], GRR._card_count[next_seat], card);
			if (GRR._card_count[next_seat] == 1) {
				action = GameConstants.WIK_NULL;
			}
			if (action != GameConstants.WIK_NULL) {
				int value = 14 - logic.get_card_value(card);
				for (int c = 0; c < GRR._card_count[next_seat]; c++) {
					if (value != logic.get_card_value(GRR._cards_data[next_seat][c])) {
						continue;
					}
					if (_playerStatus[next_seat].has_action_by_code(HongErShiConstants.WIK_CHI)) {
						_playerStatus[next_seat].add_chi_data(GRR._cards_data[next_seat][c], HongErShiConstants.WIK_CHI);
					} else {
						_playerStatus[next_seat].add_action(HongErShiConstants.WIK_CHI);
						_playerStatus[next_seat].add_action(GameConstants.WIK_NULL);
						_playerStatus[next_seat].add_chi(GRR._cards_data[next_seat][c], HongErShiConstants.WIK_CHI, seat_index); // 吃
						_playerStatus[next_seat].add_chi_data(GRR._cards_data[next_seat][c], HongErShiConstants.WIK_CHI);
					}
					bAroseAction = true;
				}
			}
		}

		// TODO: 碰
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == seat_index && !bDisdatch) {
				continue;
			}
			if (player_bao_ting[p]) {
				continue;
			}

			int action = logic.check_peng(GRR._cards_data[p], GRR._card_count[p], card);
			if (action != GameConstants.WIK_NULL) {
				if ((check_si_dui(p) && super.player_bao_ting[p]) || (super.player_bao_ting[p] && logic.get_card_value(card) != 7)) {
				} else {

					_playerStatus[p].add_action(HongErShiConstants.WIK_PENG);
					_playerStatus[p].add_action(GameConstants.WIK_NULL);
					// _playerStatus[p].add_peng(card, seat_index); // 碰
					_playerStatus[p].add_tou(card, HongErShiConstants.WIK_PENG, seat_index);
					bAroseAction = true;
				}
				// break;// 只有一个人能碰,跳出来
			}
		}

		// TODO: 杠
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == seat_index && !bDisdatch) {
				continue;
			}
			if (super.player_bao_ting[p]) {
				continue;
			}

			int[] cards_value_index = new int[14];
			logic.switch_to_cards_index_value(GRR._cards_data[p], 0, GRR._card_count[p], cards_value_index);
			if (cards_value_index[logic.get_card_value(card) - 1] == 3) {
				int index = m_gangCardResult[p].cbCardCount++;
				m_gangCardResult[p].cbCardData[index] = card;
				m_gangCardResult[p].isPublic[index] = 1;// 接杠
				m_gangCardResult[p].type[index] = GameConstants.GANG_TYPE_AN_GANG;

				_playerStatus[p].add_action(HongErShiConstants.WIK_GANG);
				_playerStatus[p].add_action(GameConstants.WIK_NULL);
				// _playerStatus[p].add_gang(card, seat_index, 1);
				_playerStatus[p].add_gang_with_suo_pai(card, seat_index, 1, HongErShiConstants.WIK_GANG);
				bAroseAction = true;
			}
			// if (bAroseAction) {
			// break;
			// }

			// TODO:只有从牌桌上摸牌才检测接杠,打出牌不检测
			for (int w = 0; w < GRR._weave_count[p]; w++) {
				if ((GRR._weave_items[p][w].weave_kind == HongErShiConstants.WIK_AN_PENG
						|| (p == seat_index && GRR._weave_items[p][w].weave_kind == HongErShiConstants.WIK_PENG))
						&& logic.get_card_value(GRR._weave_items[p][w].center_card) == logic.get_card_value(card)) {
					int index = m_gangCardResult[p].cbCardCount++;
					m_gangCardResult[p].cbCardData[index] = card;
					m_gangCardResult[p].isPublic[index] = 1;// 接杠
					if (GRR._weave_items[p][w].weave_kind == HongErShiConstants.WIK_AN_PENG) {
						m_gangCardResult[p].type[index] = GameConstants.GANG_TYPE_JIE_GANG;
					} else {
						m_gangCardResult[p].type[index] = GameConstants.GANG_TYPE_ADD_GANG;
					}

					_playerStatus[p].add_action(HongErShiConstants.WIK_GANG);
					_playerStatus[p].add_action(GameConstants.WIK_NULL);
					// _playerStatus[p].add_gang(card, seat_index, 1);
					_playerStatus[p].add_gang_with_suo_pai(card, seat_index, 1, HongErShiConstants.WIK_GANG);
					bAroseAction = true;
				}
			}
		}
		if (bAroseAction) {
			_resume_player = _current_player;
			// _current_player = GameConstants.INVALID_SEAT;
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	/**
	 * 玩家动作--通知玩家弹出/关闭操作
	 * 
	 * @param seat_index
	 * @param close
	 * @return
	 */
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
			weaveItem_item.setHuXi(curPlayerStatus._action_weaves[i].hu_xi);
			int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
					| GameConstants.WIK_EQS | GameConstants.WIK_YWS;
			// this.log_error("weave.kind" +
			// curPlayerStatus._action_weaves[i].weave_kind + "center_card"
			// + curPlayerStatus._action_weaves[i].center_card);
			if ((eat_type & curPlayerStatus._action_weaves[i].weave_kind) != 0) {
				for (int j = 0; j < curPlayerStatus._action_weaves[i].lou_qi_count; j++) {
					ChiGroupCard.Builder chi_group = ChiGroupCard.newBuilder();
					chi_group.setLouWeaveCount(j);
					for (int k = 0; k < 2; k++) {
						if (curPlayerStatus._action_weaves[i].lou_qi_weave[j][k] != GameConstants.WIK_NULL) {
							// this.log_error("lou_qi_weave.kind" +
							// curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);

							chi_group.addLouWeaveKind(curPlayerStatus._action_weaves[i].lou_qi_weave[j][k]);
						}
					}
					weaveItem_item.addChiGroupCard(chi_group);
				}
			}

			for (int w = 0; w < curPlayerStatus._action_weaves[i].weave_card.length; w++) {
				weaveItem_item.addWeaveCard(curPlayerStatus._action_weaves[i].weave_card[w]);
			}
			roomResponse.addWeaveItems(weaveItem_item);
		}
		GRR.add_room_response(roomResponse);
		this.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	public boolean estimate_player_an_gang(int seat_index) {
		boolean bAroseAction = false;

		int[] cards_value_index = new int[14];
		logic.switch_to_cards_index_value(GRR._cards_data[seat_index], 0, GRR._card_count[seat_index], cards_value_index);
		for (int v = 0; v < 14; v++) {
			if (cards_value_index[v] == 4) {
				int index = m_gangCardResult[seat_index].cbCardCount++;
				m_gangCardResult[seat_index].cbCardData[index] = cards_value_index[v];
				m_gangCardResult[seat_index].isPublic[index] = 0;// 暗杠
				m_gangCardResult[seat_index].type[index] = GameConstants.GANG_TYPE_AN_GANG;

				_playerStatus[seat_index].add_action(HongErShiConstants.WIK_GANG);
				_playerStatus[seat_index].add_gang(cards_value_index[v], seat_index, 1);
				bAroseAction = true;
			}
		}

		return bAroseAction;
	}

	public boolean estimate_player_bao_ting() {
		boolean bAroseAction = false;

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			if (p == _cur_banker) {
				continue;
			}

			_playerStatus[p]._hu_card_count = get_hh_ting_card_twenty_bao_ting(_playerStatus[p]._hu_cards, GRR._cards_index[p], GRR._weave_items[p],
					GRR._weave_count[p], p, p);

			if (_playerStatus[p]._hu_card_count != 0) {
				_playerStatus[p].add_action(HongErShiConstants.WIK_BAO_TING);
				_playerStatus[p].add_action(GameConstants.WIK_NULL);
				_playerStatus[p].add_pass(0, p);
				bAroseAction = true;
			}
		}
		return bAroseAction;
	}

	@Override
	public void on_init_table(int game_type_index, int game_rule_index, int game_round) {
		super.on_init_table(game_type_index, game_rule_index, game_round);

		_handler_dispath_card = new HandlerDispatchCard();
		_handler_out_card_operate = new HandlerOutCardOperate();
		_handler_gang = new HandlerGang();
		_handler_chi_peng = new HandlerChiPeng();
		_handler_chuli_firstcards = new HandlerChuLiFirstCard();
		_handler_dispath_firstcards = new HandlerDispatchFirstCard();

		if (has_rule(HongErShiConstants.RULE_KING_NUMBER_SIX)) {
			cards = HongErShiConstants.CARDS_SIX;
		} else if (has_rule(HongErShiConstants.RULE_KING_NUMBER_NINE)) {
			cards = HongErShiConstants.CARDS_NINE;
		} else if (has_rule(HongErShiConstants.RULE_KING_NUMBER_FIFTEEN)) {
			cards = HongErShiConstants.CARDS_FIFTEEN;
		} else if (has_rule(HongErShiConstants.RULE_KING_NUMBER_EIGHTEEN)) {
			cards = HongErShiConstants.CARDS_EIGHTEEN;
		} else if (has_rule(HongErShiConstants.RULE_KING_NUMBER_THREE)) {
			cards = HongErShiConstants.CARDS_THREE;
		} else {
			cards = HongErShiConstants.CARDS_TWELVE;
		}

		logic = new HongErShiGameLogic();
		handler_first_Operate = new Handler_first_Operate();
		touCards = new boolean[this.getTablePlayerNumber()];
		anCards = new boolean[this.getTablePlayerNumber()];
		baoTing = new boolean[this.getTablePlayerNumber()];
		handler_bao_ting = new Handler_Bao_Ting();
		m_gangCardResult = new GangCardResult[getTablePlayerNumber()];
		all_end = new int[9][getTablePlayerNumber() + 1];
	}

	public void exe_Handler_First_Operate(int seat_index) {
		_handler = handler_first_Operate;
		handler_first_Operate.reset(this, seat_index);
		handler_first_Operate.exe(this);
	}

	public void exe_Handler_bao_ting(boolean banker_gang) {

		_handler = handler_bao_ting;
		handler_bao_ting.reset(banker_gang);
		handler_bao_ting.exe(this);
	}

	@Override
	public int getTablePlayerNumber() {
		if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_PLAYER_THREE)) {
			return 3;
		}
		return 4;
	}

	/**
	 * 这里用叫庄协议表示偷牌
	 */
	@Override
	public boolean handler_call_banker(int seat_index, int call_banker) {
		if (this._current_player != seat_index) {
			log_error("红二十偷牌不是当前操作用户: " + seat_index);
			return false;
		}
		int number = logic.countKingNumber(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]);
		if (number <= 0) {
			log_error("红二十没有大王不能偷牌");
			return false;
		}
		if (!logic.remove_all_card_by_card(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index], number, 0x4F)) {
			return false;
		}
		this.touCard(seat_index, false);

		this.GRR._card_count[seat_index] -= number;
		this.operate_player_cards(seat_index, this.GRR._card_count[seat_index], cards, this.GRR._weave_count[seat_index],
				this.GRR._weave_items[seat_index]);

		int cards[] = new int[number];
		for (int i = 0; i < number; i++) {
			_send_card_count++;
			cards[i] = _repertory_card[_all_card_len - GRR._left_card_count];
			GRR._left_card_count--;
		}

		this.operate_player_get_card(seat_index, 1, cards, GameConstants.INVALID_SEAT, false);
		this.GRR._card_count[seat_index] += number;

		int wIndex = this.GRR._weave_count[seat_index];
		for (int i = 0; i < this.GRR._weave_count[seat_index]; i++) {
			if (this.GRR._weave_items[seat_index][i].center_card == 0x4F) {
				wIndex = i;
				for (int j = 0; j < 12; j++) {
					if (this.GRR._weave_items[seat_index][i].weave_card[j] == 0) {
						break;
					}
				}
				break;
			}
		}
		this.GRR._weave_items[seat_index][wIndex].hu_xi += number;
		if (wIndex == 0) {
			this.GRR._weave_count[seat_index]++;
		}
		this.operate_player_cards(seat_index, this.GRR._card_count[seat_index], cards, this.GRR._weave_count[seat_index],
				this.GRR._weave_items[seat_index]);

		if (logic.countKingNumber(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index]) > 0) {
			handler_call_banker(seat_index, 1);
		} else {
			int count = 0;
			do {
				if (logic.countKingNumber(GRR._cards_data[seat_index], GRR._card_count[seat_index]) > 0) {
					this.touCard(seat_index, true);
					_current_player = seat_index;
					return true;
				}
				this.touCards[seat_index] = true;
				seat_index = (seat_index + 1) % this.getTablePlayerNumber();
				count++;
			} while (count < this.getTablePlayerNumber() || this.touCards[count]);

			// 这个时候已经全部偷完牌了 开始检查暗碰、暗杠
			Arrays.fill(touCards, false);
			Arrays.fill(anCards, false);
			Arrays.fill(anCards, false);

			// 没有人要偷牌 则开始检查暗杠暗碰
			this._game_status = HongErShiConstants.STATUS_AN_CARDS;
			seat_index = this._cur_banker;
			do {
				int cardsFour[] = new int[2];
				int cardsThree[] = new int[2];
				int countNumber[] = new int[2];
				this.logic.checkLgThree(this.GRR._cards_data[seat_index], this.GRR._card_count[seat_index], cardsFour, cardsThree, countNumber);
				if (countNumber[0] > 0 || countNumber[1] > 0) {
					this.anCards(seat_index, cardsFour, cardsThree, countNumber);
					this._current_player = seat_index;
					return true;
				}
				this.touCards[seat_index] = true;
				seat_index = (seat_index + 1) % this.getTablePlayerNumber();
				count++;
			} while (count < this.getTablePlayerNumber());

			if (isBegin) {
				ChiHuRight chr = new ChiHuRight();
				int hu = this.analyse_chi_hu_card(this._cur_banker, this._cur_banker, 0, HongErShiConstants.WIK_TIAN_HU, chr, true);

				if (hu != GameConstants.WIK_NULL) {
					this._playerStatus[this._cur_banker].add_action(HongErShiConstants.WIK_ZI_MO);
					this._playerStatus[this._cur_banker].add_zi_mo(_send_card_data, this._cur_banker);
					this._playerStatus[this._cur_banker].add_action(GameConstants.WIK_NULL);
					this._playerStatus[this._cur_banker].add_pass(this._send_card_data, this._cur_banker);

					if (this._playerStatus[this._cur_banker].has_action()) {
						this._playerStatus[this._cur_banker].set_status(GameConstants.Player_Status_OPR_CARD);//
						// 操作状态
						this.operate_player_action(this._cur_banker, false);
					}
				} else {
					this._playerStatus[this._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					this.operate_player_status();
				}
			}
		}

		return true;
	}

	public int analyse_chi_hu_card(int seatIndex, int provider_index, int card, int type, ChiHuRight chr, boolean dispatch) {
		if (type == HongErShiConstants.WIK_TIAN_HU) {

		} else if (card == 0) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}

		int[] cardsData = new int[HongErShiConstants.MAX_HAND_CARD_COUNT];
		int cardCount = 0;
		for (int i = 0; i < GRR._card_count[seatIndex]; i++) {
			if (this.GRR._cards_data[seatIndex][i] != 0) {
				cardsData[cardCount++] = GRR._cards_data[seatIndex][i];
			}
		}

		if (card != 0) {
			cardsData[cardCount++] = card;
		}

		int red = 0, black = 0;
		for (int i = 0; i < cardCount; i++) {
			if ((logic.get_card_color(cardsData[i]) % 2) == 1) {
				red += logic.get_card_value(cardsData[i]) > 10 ? 1 : logic.get_card_value(cardsData[i]);
			} else {
				black += logic.get_card_value(cardsData[i]) > 10 ? 1 : logic.get_card_value(cardsData[i]);
			}
		}
		for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
			for (int c = 0; c < GRR._weave_items[seatIndex][w].weave_card.length; c++) {
				if (GRR._weave_items[seatIndex][w].center_card == HongErShiConstants.MAGIC_CARD_KING) {
					continue;
				}
				if ((logic.get_card_color(GRR._weave_items[seatIndex][w].weave_card[c]) % 2) == 1) {
					red += logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]) > 10 ? 1
							: logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]);
				} else {
					black += logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]) > 10 ? 1
							: logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]);
				}
			}
		}

		boolean check_magic_hu = false;
		if (red >= 50 && black >= 50) {
			chr.opr_or(HongErShiConstants.WIK_RED_BLACK_FIFTY);
			check_magic_hu = true;
		} else if (has_rule(HongErShiConstants.RULE_RED_FIFTY) && red >= 50) {
			chr.opr_or(HongErShiConstants.WIK_RED_FIFTY);
			check_magic_hu = true;
		} else if (has_rule(HongErShiConstants.RULE_BLACK_FIFTY) && black >= 50) {
			chr.opr_or(HongErShiConstants.WIK_BLACK_FIFTY);
			check_magic_hu = true;
		} else if (red >= 20 && black >= 20) {
			chr.opr_or(HongErShiConstants.WIK_BASE_HU);
		}

		if (red == 20 && has_rule(HongErShiConstants.RULE_JIN_ER_SHI)) {
			chr.opr_or(HongErShiConstants.WIK_JIN_ER_SHI);
		}

		int[] cardsIndex = new int[14];
		logic.switch_to_cards_index_value(cardsData, 0, cardsData.length, cardsIndex);

		boolean can_jie_pao = false;
		if (red == 0) { // 全红
			chr.opr_or(HongErShiConstants.WIK_ALL_BALCK);
			can_jie_pao = true;
			check_magic_hu = true;
		}
		if (black == 0) { // 全黑
			chr.opr_or(HongErShiConstants.WIK_ALL_RED);
			can_jie_pao = true;
			check_magic_hu = true;
		}
		boolean check_14_or_red = true;
		if (GRR._card_count[seatIndex] == 1) {
			boolean flag = true;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					flag = false;
					break;
				}
			}
			for (int i = 0; i < 13 && check_14_or_red; i++) {
				if (cardsIndex[i] > 0 && cardsIndex[i] != 2) {
					flag = false;
					break;
				}
			}
			if (flag && has_rule(HongErShiConstants.RULE_JIN_GOU_GOU)) {
				check_14_or_red = false;
				chr.opr_or(HongErShiConstants.WIK_JIN_GOU_GOU);
				can_jie_pao = true;
			}
		}

		if (type == HongErShiConstants.WIK_TIAN_HU) {
			chr.opr_or(HongErShiConstants.WIK_TIAN_HU);
		} else if (type == HongErShiConstants.WIK_DI_HU) {
			chr.opr_or(HongErShiConstants.WIK_DI_HU);
		} else if (type == HongErShiConstants.WIK_GANG_SHANG_HUA) {
			chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_HUA);
		} else if (type == HongErShiConstants.WIK_GANG_SHANG_PAO) {
			chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_PAO);
		} else if (type == HongErShiConstants.WIK_QIANG_GANG) {
			// 10.没有王或没碰牌,简单来说就是没有翻的情况下,必须要自摸才能胡,放炮不能胡
			int weave_count_chi = 0;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					weave_count_chi++;
				}
			}
			// 偷个懒，报听
			if (player_bao_ting[seatIndex] || check_magic_hu) {
				weave_count_chi = -1;
			}
			if (!can_jie_pao && is_si_dui_pj(cardsIndex) == GameConstants.WIK_NULL && GRR._weave_count[seatIndex] == weave_count_chi
					&& has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)) {
				chr.set_empty();
				return GameConstants.WIK_NULL;
			}
			chr.opr_or(HongErShiConstants.WIK_QIANG_GANG);
		} else if (dispatch && seatIndex == provider_index) {
			chr.opr_or(HongErShiConstants.WIK_ZI_MO);
		} else if (dispatch && seatIndex != provider_index) {

			// 10.没有王或没碰牌,简单来说就是没有翻的情况下,必须要自摸才能胡,放炮不能胡
			int weave_count_chi = 0;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					weave_count_chi++;
				}
			}
			// 偷个懒，报听
			if (player_bao_ting[seatIndex] || check_magic_hu) {
				weave_count_chi = -1;
			}
			if (!can_jie_pao && is_si_dui_pj(cardsIndex) == GameConstants.WIK_NULL && type != HongErShiConstants.WIK_QIANG_GANG
					&& GRR._weave_count[seatIndex] == weave_count_chi && has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)) {
				chr.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		if (seatIndex != this._cur_banker && this.baoTing[seatIndex]) {
			chr.opr_or(HongErShiConstants.WIK_BAO_TING);
		}

		int code = is_si_dui_pj(cardsIndex);
		if (code != GameConstants.WIK_NULL) {
			chr.opr_or(code);
			return HongErShiConstants.WIK_CHI_HU;
		}

		for (int i = 0; i < 13 && check_14_or_red; i++) {
			if (cardsIndex[i] > 0) {
				if (i == 6 && cardsIndex[i] % 2 != 0) {
					chr.set_empty();
					return GameConstants.WIK_NULL;
				}
				if (cardsIndex[12 - i] != cardsIndex[i]) {
					chr.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		}

		// if (has_rule(HongErShiConstants.RULE_NENG_HONG_DIAN_20) && card != 0)
		// {
		// int red_temp = red;
		// if ((logic.get_card_color(card) % 2) == 1) {
		// red_temp -= logic.get_card_value(card) > 10 ? 1 :
		// logic.get_card_value(card);
		// }
		// if (red_temp > 0 && red_temp < 20) {
		// chr.set_empty();
		// return GameConstants.WIK_NULL;
		// }
		// }
		if (has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)
				&& getFanShu(seatIndex, chr, GRR._weave_items[seatIndex], GRR._weave_count[seatIndex], true) == 0) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}
		if (red > 0 && red < 20 && has_rule(HongErShiConstants.RULE_HONG_DIAN) && check_14_or_red && !check_magic_hu) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}

		return HongErShiConstants.WIK_CHI_HU;
	}

	public int analyse_chi_hu_card_bao_ting(int seatIndex, int provider_index, int card, int type, ChiHuRight chr, boolean dispatch) {
		if (type == HongErShiConstants.WIK_TIAN_HU) {

		} else if (card == 0) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}

		int[] cardsData = new int[HongErShiConstants.MAX_HAND_CARD_COUNT];
		int cardCount = 0;
		for (int i = 0; i < GRR._card_count[seatIndex]; i++) {
			if (this.GRR._cards_data[seatIndex][i] != 0) {
				cardsData[cardCount++] = GRR._cards_data[seatIndex][i];
			}
		}

		int red = 0, black = 0;
		for (int i = 0; i < cardCount; i++) {
			if ((logic.get_card_color(cardsData[i]) % 2) == 1) {
				red += logic.get_card_value(cardsData[i]) > 10 ? 1 : logic.get_card_value(cardsData[i]);
			} else {
				black += logic.get_card_value(cardsData[i]) > 10 ? 1 : logic.get_card_value(cardsData[i]);
			}
		}
		for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
			for (int c = 0; c < GRR._weave_items[seatIndex][w].weave_card.length; c++) {
				if (GRR._weave_items[seatIndex][w].center_card == HongErShiConstants.MAGIC_CARD_KING) {
					continue;
				}
				if ((logic.get_card_color(GRR._weave_items[seatIndex][w].weave_card[c]) % 2) == 1) {
					red += logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]) > 10 ? 1
							: logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]);
				} else {
					black += logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]) > 10 ? 1
							: logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]);
				}
			}
		}

		if (card != 0) {
			cardsData[cardCount++] = card;
		}

		boolean check_magic_hu = false;
		if (red >= 50 && black >= 50) {
			chr.opr_or(HongErShiConstants.WIK_RED_BLACK_FIFTY);
			check_magic_hu = true;
		} else if (has_rule(HongErShiConstants.RULE_RED_FIFTY) && red >= 50) {
			chr.opr_or(HongErShiConstants.WIK_RED_FIFTY);
			check_magic_hu = true;
		} else if (has_rule(HongErShiConstants.RULE_BLACK_FIFTY) && black >= 50) {
			chr.opr_or(HongErShiConstants.WIK_BLACK_FIFTY);
			check_magic_hu = true;
		} else if (red >= 20 && black >= 20) {
			chr.opr_or(HongErShiConstants.WIK_BASE_HU);
		}

		if (red == 20 && has_rule(HongErShiConstants.RULE_JIN_ER_SHI)) {
			chr.opr_or(HongErShiConstants.WIK_JIN_ER_SHI);
		}

		if (red == 0) { // 全红
			chr.opr_or(HongErShiConstants.WIK_ALL_BALCK);
			check_magic_hu = true;
		}
		if (black == 0) { // 全黑
			chr.opr_or(HongErShiConstants.WIK_ALL_RED);
			check_magic_hu = true;
		}
		if (GRR._card_count[seatIndex] == 1) {
			boolean flag = true;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					flag = false;
					break;
				}
			}

			if (flag && has_rule(HongErShiConstants.RULE_JIN_GOU_GOU)) {
				chr.opr_or(HongErShiConstants.WIK_JIN_GOU_GOU);
			}
		}

		int[] cardsIndex = new int[14];
		logic.switch_to_cards_index_value(cardsData, 0, cardsData.length, cardsIndex);

		if (type == HongErShiConstants.WIK_TIAN_HU) {
			chr.opr_or(HongErShiConstants.WIK_TIAN_HU);
		} else if (type == HongErShiConstants.WIK_DI_HU) {
			chr.opr_or(HongErShiConstants.WIK_DI_HU);
		} else if (type == HongErShiConstants.WIK_GANG_SHANG_HUA) {
			chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_HUA);
		} else if (type == HongErShiConstants.WIK_GANG_SHANG_PAO) {
			chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_PAO);
		} else if (type == HongErShiConstants.WIK_QIANG_GANG) {
			chr.opr_or(HongErShiConstants.WIK_QIANG_GANG);
		} else if (dispatch && seatIndex == provider_index) {
			chr.opr_or(HongErShiConstants.WIK_ZI_MO);
		} else if (dispatch && seatIndex != provider_index) {

			// 10.没有王或没碰牌,简单来说就是没有翻的情况下,必须要自摸才能胡,放炮不能胡
			int weave_count_chi = 0;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					weave_count_chi++;
				}
			}
			// 偷个懒，报听
			if (player_bao_ting[seatIndex]) {
				weave_count_chi = -1;
			}
			if (is_si_dui_pj(cardsIndex) == GameConstants.WIK_NULL && type != HongErShiConstants.WIK_QIANG_GANG
					&& GRR._weave_count[seatIndex] == weave_count_chi && has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)) {
				chr.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		if (seatIndex != this._cur_banker && this.baoTing[seatIndex]) {
			chr.opr_or(HongErShiConstants.WIK_BAO_TING);
		}

		int code = is_si_dui_pj(cardsIndex);
		if (code != GameConstants.WIK_NULL) {
			chr.opr_or(code);
			return HongErShiConstants.WIK_CHI_HU;
		}

		for (int i = 0; i < 13; i++) {
			if (cardsIndex[i] > 0) {
				if (i == 6 && cardsIndex[i] % 2 != 0) {
					chr.set_empty();
					return GameConstants.WIK_NULL;
				}
				if (cardsIndex[12 - i] != cardsIndex[i]) {
					chr.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		}

		if (has_rule(HongErShiConstants.RULE_NENG_HONG_DIAN_20) && card != 0) {
			int red_temp = red;
			if ((logic.get_card_color(card) % 2) == 1) {
				red_temp -= logic.get_card_value(card) > 10 ? 1 : logic.get_card_value(card);
			}
			if (red_temp > 0 && red_temp < 20) {
				chr.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		if (has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)
				&& getFanShu(seatIndex, chr, GRR._weave_items[seatIndex], GRR._weave_count[seatIndex], true) == 0) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}
		if (red > 0 && red < 20 && has_rule(HongErShiConstants.RULE_HONG_DIAN) && !check_magic_hu) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}

		return HongErShiConstants.WIK_CHI_HU;
	}

	public int analyse_chi_hu_card_cha_jiao(int seatIndex, int provider_index, int card, int type, ChiHuRight chr, boolean dispatch) {
		if (type == HongErShiConstants.WIK_TIAN_HU) {

		} else if (card == 0) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}

		int[] cardsData = new int[HongErShiConstants.MAX_HAND_CARD_COUNT];
		int cardCount = 0;
		for (int i = 0; i < GRR._card_count[seatIndex]; i++) {
			if (this.GRR._cards_data[seatIndex][i] != 0) {
				cardsData[cardCount++] = GRR._cards_data[seatIndex][i];
			}
		}

		if (card != 0) {
			cardsData[cardCount++] = card;
		}

		int red = 0, black = 0;
		for (int i = 0; i < cardCount; i++) {
			if ((logic.get_card_color(cardsData[i]) % 2) == 1) {
				red += logic.get_card_value(cardsData[i]) > 10 ? 1 : logic.get_card_value(cardsData[i]);
			} else {
				black += logic.get_card_value(cardsData[i]) > 10 ? 1 : logic.get_card_value(cardsData[i]);
			}
		}
		for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
			for (int c = 0; c < GRR._weave_items[seatIndex][w].weave_card.length; c++) {
				if (GRR._weave_items[seatIndex][w].center_card == HongErShiConstants.MAGIC_CARD_KING) {
					continue;
				}
				if ((logic.get_card_color(GRR._weave_items[seatIndex][w].weave_card[c]) % 2) == 1) {
					red += logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]) > 10 ? 1
							: logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]);
				} else {
					black += logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]) > 10 ? 1
							: logic.get_card_value(GRR._weave_items[seatIndex][w].weave_card[c]);
				}
			}
		}

		if (red >= 50 && black >= 50) {
			chr.opr_or(HongErShiConstants.WIK_RED_BLACK_FIFTY);
		} else if (has_rule(HongErShiConstants.RULE_RED_FIFTY) && red >= 50) {
			chr.opr_or(HongErShiConstants.WIK_RED_FIFTY);
		} else if (has_rule(HongErShiConstants.RULE_BLACK_FIFTY) && black >= 50) {
			chr.opr_or(HongErShiConstants.WIK_BLACK_FIFTY);
		} else if (red >= 20 && black >= 20) {
			chr.opr_or(HongErShiConstants.WIK_BASE_HU);
		}

		if (red == 20 && has_rule(HongErShiConstants.RULE_JIN_ER_SHI)) {
			chr.opr_or(HongErShiConstants.WIK_JIN_ER_SHI);
		}

		if (red == 0) { // 全红
			chr.opr_or(HongErShiConstants.WIK_ALL_BALCK);
		}
		if (black == 0) { // 全黑
			chr.opr_or(HongErShiConstants.WIK_ALL_RED);
		}
		if (GRR._card_count[seatIndex] == 1) {
			boolean flag = true;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					flag = false;
					break;
				}
			}
			if (flag && has_rule(HongErShiConstants.RULE_JIN_GOU_GOU)) {
				chr.opr_or(HongErShiConstants.WIK_JIN_GOU_GOU);
			}
		}

		int[] cardsIndex = new int[14];
		logic.switch_to_cards_index_value(cardsData, 0, cardsData.length, cardsIndex);

		if (type == HongErShiConstants.WIK_TIAN_HU) {
			chr.opr_or(HongErShiConstants.WIK_TIAN_HU);
		} else if (type == HongErShiConstants.WIK_DI_HU) {
			chr.opr_or(HongErShiConstants.WIK_DI_HU);
		} else if (type == HongErShiConstants.WIK_GANG_SHANG_HUA) {
			chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_HUA);
		} else if (type == HongErShiConstants.WIK_GANG_SHANG_PAO) {
			chr.opr_or(HongErShiConstants.WIK_GANG_SHANG_PAO);
		} else if (type == HongErShiConstants.WIK_QIANG_GANG) {
			chr.opr_or(HongErShiConstants.WIK_QIANG_GANG);
		} else if (dispatch && seatIndex == provider_index) {
			// chr.opr_or(HongErShiConstants.WIK_ZI_MO);
		} else if (dispatch && seatIndex != provider_index) {

			// 10.没有王或没碰牌,简单来说就是没有翻的情况下,必须要自摸才能胡,放炮不能胡
			int weave_count_chi = 0;
			for (int w = 0; w < GRR._weave_count[seatIndex]; w++) {
				if (GRR._weave_items[seatIndex][w].weave_kind == HongErShiConstants.WIK_CHI) {
					weave_count_chi++;
				}
			}
			// 偷个懒，报听
			if (player_bao_ting[seatIndex]) {
				weave_count_chi = -1;
			}
			if (is_si_dui_pj(cardsIndex) == GameConstants.WIK_NULL && type != HongErShiConstants.WIK_QIANG_GANG
					&& GRR._weave_count[seatIndex] == weave_count_chi && has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)) {
				chr.set_empty();
				return GameConstants.WIK_NULL;
			}
		}
		if (seatIndex != this._cur_banker && this.baoTing[seatIndex]) {
			chr.opr_or(HongErShiConstants.WIK_BAO_TING);
		}

		int code = is_si_dui_pj(cardsIndex);
		if (code != GameConstants.WIK_NULL) {
			chr.opr_or(code);
			return HongErShiConstants.WIK_CHI_HU;
		}

		for (int i = 0; i < 13; i++) {
			if (cardsIndex[i] > 0) {
				if (i == 6 && cardsIndex[i] % 2 != 0) {
					chr.set_empty();
					return GameConstants.WIK_NULL;
				}
				if (cardsIndex[12 - i] != cardsIndex[i]) {
					chr.set_empty();
					return GameConstants.WIK_NULL;
				}
			}
		}

		if (has_rule(HongErShiConstants.RULE_YI_FAN_QI_HU)
				&& getFanShu(seatIndex, chr, GRR._weave_items[seatIndex], GRR._weave_count[seatIndex], true) == 0) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}
		if (red > 0 && red < 20 && has_rule(HongErShiConstants.RULE_HONG_DIAN)) {
			chr.set_empty();
			return GameConstants.WIK_NULL;
		}

		return HongErShiConstants.WIK_CHI_HU;
	}

	public int is_si_dui_pj(int[] cards_value_index) {
		if (!has_rule(HongErShiConstants.RULE_SI_DUI)) {
			return GameConstants.WIK_NULL;
		}

		int long_dui = 0;
		int count = 0;
		for (int i = 0; i < cards_value_index.length; i++) {
			if (cards_value_index[i] == 0) {
				continue;
			}

			if (cards_value_index[i] % 2 != 0) {
				return GameConstants.WIK_NULL;
			}

			count += cards_value_index[i] / 2;
			if (cards_value_index[i] == 4) {
				long_dui++;
			}
		}

		if (count == 4) {
			if (long_dui == 2) {
				if (has_rule(HongErShiConstants.RULE_TWO_LONG_SI_DUI)) {
					return HongErShiConstants.WIK_TWO_LONG_SI_DUI;
				}
			} else if (long_dui == 1) {
				if (has_rule(HongErShiConstants.RULE_LONG_SI_DUI)) {
					return HongErShiConstants.WIK_LONG_SI_DUI;
				}
			}
			return HongErShiConstants.WIK_SI_DUI;
		}

		return GameConstants.WIK_NULL;
	}

	@Override
	public int analyse_chi_hu_card(int cards_index[], WeaveItem weaveItems[], int weaveCount, int seat_index, int provider_index, int cur_card,
			ChiHuRight chiHuRight, int card_type, int[] hu_xi_hh, boolean dispatch) {
		return 0;
	}

	@Override
	public void process_chi_hu_player_score_phz(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;
		GRR._win_order[seat_index] = 1;
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		countCardType(chr, seat_index);

		int all_hu_xi = 0;
		for (int i = 0; i < this._hu_weave_count[seat_index]; i++) {
			all_hu_xi += this._hu_weave_items[seat_index][i].hu_xi;
		}

		if (all_hu_xi == 0) { // 毛胡
			if (has_rule(Constants_ChenZhou.GAME_RULE_MAO_HU)) {
				all_hu_xi = 15;
			}
		}

		this._hu_xi[seat_index] = all_hu_xi;

		int wFanShu = getFanShu(seat_index, chr, GRR._weave_items[seat_index], GRR._weave_count[seat_index], true);

		GRR._lost_fan_shu[seat_index][0] = wFanShu;
		// if (zimo) {
		// for (int i = 0; i < this.getTablePlayerNumber(); i++) {
		// if (i == seat_index) {
		// continue;
		// }
		// }
		// } else {
		// GRR._lost_fan_shu[provide_index][seat_index] = wFanShu *
		// this.getTablePlayerNumber();
		// }

		int lChiHuScore = 1;

		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				int tempfanShu = wFanShu;
				if (super.player_bao_ting[i]) {
					tempfanShu += 3;
					GRR._lost_fan_shu[i][0] -= 3;
				}
				if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_GNEDOU)) {
					if (tempfanShu > getFanLimit()) {
						int m = 1 << getFanLimit();
						int n = (tempfanShu - getFanLimit()) * 2;
						tempfanShu = m + n;
					} else {
						tempfanShu = 1 << tempfanShu;
					}
				}
				if (tempfanShu == 0) {
					tempfanShu = 1;
				}
				if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_GNEDOU)) {
					s = tempfanShu;
				} else {
					s += tempfanShu;
				}

				if (!chr.opr_and(HongErShiConstants.WIK_QIANG_GANG).is_empty()) {
					GRR._game_score[provide_index] -= s;
				} else {
					GRR._game_score[i] -= s;
				}
				GRR._game_score[seat_index] += s;
			}
		} else {
			// if (!chr.opr_and(HongErShiConstants.WIK_QIANG_GANG).is_empty()) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				int s = lChiHuScore;
				int tempfanShu = wFanShu;
				if (super.player_bao_ting[i]) {
					tempfanShu += 3;
					GRR._lost_fan_shu[i][0] -= 3;
				}
				if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_GNEDOU)) {
					if (tempfanShu > getFanLimit()) {
						int m = 1 << getFanLimit();
						int n = (tempfanShu - getFanLimit()) * 2;
						tempfanShu = m + n;
					} else {
						tempfanShu = 1 << tempfanShu;
					}
				}
				if (tempfanShu == 0) {
					tempfanShu = 1;
				}

				if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_GNEDOU)) {
					s = tempfanShu;
				} else {
					s += tempfanShu;
				}

				GRR._game_score[provide_index] -= s;
				GRR._game_score[seat_index] += s;
			}

			GRR._chi_hu_rights[provide_index].opr_or(HongErShiConstants.WIK_FANG_PAO);
			GRR._chi_hu_rights[seat_index].opr_or(HongErShiConstants.WIK_FANG_PAO);
			// } else {
			// int s = lChiHuScore;
			// int tempfanShu = wFanShu;
			// if (player_bao_ting[provide_index]) {
			// tempfanShu += 3;
			// GRR._lost_fan_shu[provide_index][0] = -tempfanShu;
			// }
			// if (has_rule(HongErShiConstants.RULE_FAN_GNEDOU)) {
			// tempfanShu = 1 << tempfanShu;
			// }
			// if (tempfanShu == 0) {
			// tempfanShu = 1;
			// }
			// s += tempfanShu;
			// GRR._game_score[provide_index] -= s;
			// GRR._game_score[seat_index] += s;
			//
			//
			// }
		}

		GRR._provider[seat_index] = provide_index;
	}

	public int getFanLimit() {
		if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_TITI)) {
			return Integer.MAX_VALUE;
		} else {
			if (has_rule(HongErShiConstants.RULE_FENG_DING_4)) {
				return 4;
			}
			if (has_rule(HongErShiConstants.RULE_FENG_DING_5)) {
				return 5;
			}
			if (has_rule(HongErShiConstants.RULE_FENG_DING_6)) {
				return 6;
			}
			return Integer.MAX_VALUE;
		}
	}

	public int getFanShu(int seat_index, ChiHuRight chr, WeaveItem weaveItem[], int cbWeaveCount, boolean dispatch) {
		int wfanShu = 0;

		if (dispatch) {
			if (!chr.opr_and(HongErShiConstants.WIK_QIANG_GANG).is_empty()) {
				wfanShu += 1;
			}
			if (!chr.opr_and(HongErShiConstants.WIK_GANG_SHANG_HUA).is_empty()) {
				wfanShu += 1;
			}
			if (!chr.opr_and(HongErShiConstants.WIK_GANG_SHANG_PAO).is_empty()) {
				wfanShu += 1;
			}
		}

		for (int w = 0; w < cbWeaveCount; w++) {
			if (weaveItem[w].center_card == HongErShiConstants.MAGIC_CARD_KING) {
				for (int c = 0; c < weaveItem[w].weave_card.length; c++) {
					if (weaveItem[w].weave_card[c] != HongErShiConstants.MAGIC_CARD_KING) {
						continue;
					}
					wfanShu += 1;
				}
			} else {
				switch (weaveItem[w].weave_kind) {
				case HongErShiConstants.WIK_GANG:
				case HongErShiConstants.WIK_AN_GANG:
				case GameConstants.GANG_TYPE_AN_GANG:
				case GameConstants.GANG_TYPE_ADD_GANG:
				case GameConstants.GANG_TYPE_JIE_GANG:

					wfanShu += 2;
					break;
				case HongErShiConstants.WIK_PENG:
				case HongErShiConstants.WIK_AN_PENG:

					wfanShu += 1;
					break;
				}
			}
		}

		if (!chr.opr_and(HongErShiConstants.WIK_TIAN_HU).is_empty()) {
			wfanShu += 3;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_DI_HU).is_empty()) {
			wfanShu += 3;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_JIN_GOU_GOU).is_empty()) {
			wfanShu += 3;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_SI_DUI).is_empty()) {
			wfanShu += 3;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_LONG_SI_DUI).is_empty()) {
			wfanShu += 5;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_TWO_LONG_SI_DUI).is_empty()) {
			wfanShu += 7;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_RED_BLACK_FIFTY).is_empty()) {
			wfanShu += 10;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_RED_FIFTY).is_empty()) {
			wfanShu += 5;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_BLACK_FIFTY).is_empty()) {
			wfanShu += 5;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_ALL_RED).is_empty()) {
			wfanShu += 3;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_ALL_BALCK).is_empty()) {
			wfanShu += 4;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_JIN_ER_SHI).is_empty()) {
			wfanShu += 1;
		}
		if (!chr.opr_and(HongErShiConstants.WIK_ZI_MO).is_empty() && has_rule(HongErShiConstants.RULE_ZI_MO_ADD_FAN)) {
			wfanShu += 1;
		}
		if (super.player_bao_ting[seat_index]) {
			wfanShu += 3;
		}

		return wfanShu;
	}

	@Override
	public void set_result_describe(int seat_index) {
		int chrTypes;
		long type = 0;
		boolean fang_pao = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == HongErShiConstants.WIK_TIAN_HU) {
						result.append("天胡 3番 \n");
					}
					if (type == HongErShiConstants.WIK_DI_HU) {
						result.append("地胡 3番 \n");
					}
					if (type == HongErShiConstants.WIK_JIN_GOU_GOU) {
						result.append("金钩钩 3番 \n");
					}
					if (type == HongErShiConstants.WIK_SI_DUI) {
						result.append("四对 3番 \n");
					}
					if (type == HongErShiConstants.WIK_TWO_LONG_SI_DUI) {
						result.append("双龙四对 7番 \n");
					}
					if (type == HongErShiConstants.WIK_LONG_SI_DUI) {
						result.append("龙四对 5番 \n");
					}
					if (type == HongErShiConstants.WIK_RED_FIFTY) {
						result.append("红五十 5番 \n");
					}
					if (type == HongErShiConstants.WIK_BLACK_FIFTY) {
						result.append("黑五十 5番 \n");
					}
					if (type == HongErShiConstants.WIK_RED_BLACK_FIFTY) {
						result.append("红黑五十 10番 \n");
					}
					if (type == HongErShiConstants.WIK_ALL_RED) {
						result.append("全红 3番 \n");
					}
					if (type == HongErShiConstants.WIK_ALL_BALCK) {
						result.append("全黑 4番 \n");
					}
					if (type == HongErShiConstants.WIK_GANG_SHANG_HUA) {
						result.append("杠上花 1番 \n");
					}
					if (type == HongErShiConstants.WIK_QIANG_GANG) {
						result.append("抢杠胡 \n");
					}
					if (type == HongErShiConstants.WIK_ZHUA_PAO) {
						result.append("抓炮 \n");
					}
					if (type == HongErShiConstants.WIK_GANG_SHANG_PAO) {
						result.append("杠上炮 \n");
					}
					if (type == HongErShiConstants.WIK_JIN_ER_SHI) {
						result.append("金二十 1番 \n");
					}
					if (type == HongErShiConstants.WIK_ZI_MO && GameConstants.Game_End_DRAW != GRR._end_type) {
						if (has_rule(HongErShiConstants.RULE_ZI_MO_ADD_FAN)) {
							result.append("自摸 1番 \n");
						} else {
							result.append("自摸 \n");
						}
					}
				} else if (type == HongErShiConstants.WIK_FANG_PAO) {
					result.append("点炮 \n");
					fang_pao = true;
				}
			}

			if (super.player_bao_ting[player] && GameConstants.Game_End_DRAW != GRR._end_type) {
				if (GRR._chi_hu_rights[player].is_valid()) {
					result.append("报听 3番\n");
				} else {
					result.append("报听反赔 3番 \n");
				}
			}
			GRR._result_des[player] = result.toString();
		}
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x0b, 0x1b, 0x09, 0x19, 0x04, 0x14, 0x0d };
		int[] cards_of_player1 = new int[] { 0x02, 0x0d, 0x18, 0x16, 0x1a, 0x4, 0xd };
		int[] cards_of_player2 = new int[] { 0x1d, 0x11, 0x12, 0x1c, 0x1b, 0x19, 0x15 };
		int[] cards_of_player3 = new int[] { 0x2a, 0x3a, 0x29, 0x39, 0x28, 0x38, 0x17 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}

		if (this.getTablePlayerNumber() == 3) {
			for (int i = 0; i < 7; i++) {
				this.GRR._cards_data[0][i] = cards_of_player0[i];
				this.GRR._cards_data[1][i] = cards_of_player1[i];
				this.GRR._cards_data[2][i] = cards_of_player2[i];
			}
		} else {
			for (int i = 0; i < 7; i++) {
				this.GRR._cards_data[0][i] = cards_of_player0[i];
				this.GRR._cards_data[1][i] = cards_of_player1[i];
				this.GRR._cards_data[2][i] = cards_of_player2[i];
				this.GRR._cards_data[3][i] = cards_of_player3[i];
			}
		}
		// int[] realyCards = new int[] {
		// 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
		// 0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
		// 0x01,0x02,0x03,0x04,0x05,0x06,0x07,
		// 0x01,0x02,0x03,0x04,0x05,0x06,0x07,
		//
		// };
		// testRealyCard(realyCards);
		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 7) {
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

	/**
	 * 模拟牌型--相同牌
	 */
	public void testSameCard(int[] cards) {
		_all_card_len = _repertory_card.length;
		GRR._left_card_count = _all_card_len;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < 7; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {

			for (int j = 0; j < 7; j++) {
				GRR._cards_data[i][j] = cards[j];
			}
			GRR._left_card_count -= 7;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
	}

	/**
	 * 测试线上 实际牌
	 */
	public void testRealyCard(int[] realyCards) {
		int count = realyCards.length / 7;
		_all_card_len = realyCards.length;
		GRR._left_card_count = _all_card_len;
		if (count > this.getTablePlayerNumber())
			count = this.getTablePlayerNumber();
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < 7; j++) {
				GRR._cards_data[i][j] = 0;
			}
		}
		int k = 0;
		while (k < realyCards.length) {
			int i = 0;

			for (; i < count; i++) {
				for (int j = 0; j < 7; j++) {
					GRR._cards_data[i][j] = realyCards[k++];

				}
				GRR._left_card_count -= 7;
			}
			if (i == count)
				break;
		}
		DEBUG_CARDS_MODE = false;// 把调试模式关闭
		BACK_DEBUG_CARDS_MODE = false;
		System.err.println("=========开始调试线上牌型 调试模式自动关闭*=========");

	}

	public boolean handler_player_be_in_room(HongErShiTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			// 组合
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GRR._weave_count[i]; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
				for (int wc = 0; wc < GRR._weave_items[i][j].weave_card.length; wc++) {
					if (GRR._weave_items[i][j].weave_kind == HongErShiConstants.WIK_TOU) {
						if (GRR._weave_items[i][j].weave_card[wc] != 0) {
							weaveItem_item.addWeaveCard(GRR._weave_items[i][j].weave_card[wc]);
						}
					} else {
						weaveItem_item.addWeaveCard(GRR._weave_items[i][j].weave_card[wc]);
					}
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}

			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table.GRR._card_count[i]);
		}

		for (int i = 0; i < GRR._card_count[seat_index]; i++) {
			tableResponse.addCardsData(GRR._cards_data[seat_index][i]);
		}

		roomResponse.setTable(tableResponse);

		if (_handler == _handler_out_card_operate) {
			table.operate_out_card(table._provide_player, 1, new int[] { table._out_card_data }, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		} else if (!table.is_mo_or_show && _handler != _handler_chi_peng) {
			table.operate_player_get_card(table._current_player, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);
		}
		if (_game_status == GameConstants.GS_MJ_PLAY) {
			table.refresh_game_status(false);
			roomResponse.setIsGoldRoom(false);
		} else {
			roomResponse.setIsGoldRoom(true);
		}
		table.send_response_to_player(seat_index, roomResponse);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		table.operate_player_status();
		return true;
	}

	@Override
	public void countChiHuTimes(int _seat_index, boolean zimo) {

	}

	public boolean check_si_dui(int seat_index) {

		int max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;

		for (int m = 0; m < max_ting_count; m++) {
			int cbCurrentCard = HongErShiConstants.CARDS_DEFAULT[m];
			int[] cardsData = new int[HongErShiConstants.MAX_HAND_CARD_COUNT];
			int cardCount = 0;
			for (int i = 0; i < GRR._card_count[seat_index]; i++) {
				if (this.GRR._cards_data[seat_index][i] != 0) {
					cardsData[cardCount++] = GRR._cards_data[seat_index][i];
				}
			}
			cardsData[cardCount++] = cbCurrentCard;
			int[] cardsIndex = new int[14];
			logic.switch_to_cards_index_value(cardsData, 0, cardsData.length, cardsIndex);

			if (is_si_dui_pj(cardsIndex) != GameConstants.WIK_NULL) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int get_hh_ting_card_twenty(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index, int provate_index) {

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;
		int real_max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = HongErShiConstants.CARDS_DEFAULT[i];
			chr.set_empty();

			if (HongErShiConstants.WIK_CHI_HU == analyse_chi_hu_card(seat_index, provate_index, cbCurrentCard, HongErShiConstants.WIK_ZI_MO, chr,
					true)) {
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

	public int get_hh_ting_card_twenty_bao_ting(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index,
			int provate_index) {

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;
		int real_max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = HongErShiConstants.CARDS_DEFAULT[i];
			chr.set_empty();

			if (HongErShiConstants.WIK_CHI_HU == analyse_chi_hu_card_bao_ting(seat_index, provate_index, cbCurrentCard, HongErShiConstants.WIK_ZI_MO,
					chr, true)) {
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

	public int get_hh_ting_card_twenty_cal_cha_jiao(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index,
			int provate_index) {

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;
		int real_max_ting_count = HongErShiConstants.CARDS_DEFAULT.length;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = HongErShiConstants.CARDS_DEFAULT[i];
			ChiHuRight chr = new ChiHuRight();
			chr.set_empty();

			if (HongErShiConstants.WIK_CHI_HU == analyse_chi_hu_card_cha_jiao(seat_index, provate_index, cbCurrentCard, HongErShiConstants.WIK_ZI_MO,
					chr, true)) {
				cards[count] = cbCurrentCard;
				count++;
				if (cha_jiao_limit[seat_index] < getFanShu(seat_index, chr, weaveItem, cbWeaveCount, false)) {
					cha_jiao_limit[seat_index] = getFanShu(seat_index, chr, weaveItem, cbWeaveCount, false);
					GRR._chi_hu_rights[seat_index] = chr;
					GRR._chi_hu_rights[seat_index].set_valid(true);
				}
			}
		}

		if (count == 0) {
			cha_jiao_limit[seat_index] = -1;
		} else if (count == real_max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
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
			int cbCurrentCard = logic.switch_to_card_data(i);
			if (HongErShiConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, seat_index, seat_index, cbCurrentCard,
					chr, GameConstants.HU_CARD_TYPE_ZIMO, hu_xi_chi, true))
				return true;
		}
		return false;
	}

	/**
	 * 给玩家发送偷牌按钮
	 * 
	 * @param seatIndex
	 */
	public void touCard(int seatIndex, boolean isAnNiu) {
		this._current_player = seatIndex;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		roomResponse.setType(HongErShiConstants.RESPONSE_TOU_CARDS_ACTIONS);
		if (isAnNiu) {
			roomResponse.setType(HongErShiConstants.RESPONSE_TOU_CARDS);
		}

		RoomUtil.send_response_to_player(this, seatIndex, roomResponse);
	}

	/**
	 * 给玩家发送暗杠、暗碰按钮
	 * 
	 * @param seatIndex
	 */
	public void anCards(int seatIndex, int cardsFour[], int cardsThree[], int countNumber[]) {
		this._current_player = seatIndex;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		roomResponse.setType(HongErShiConstants.RESPONSE_AN_CARDS);

		if (countNumber[0] > 0) { // 可以暗杠的玩家一定可以暗碰
			roomResponse.addEffectsIndex(GameConstants.WIK_AN_GANG);
			for (int i = 0; i < countNumber[0]; i++) {
				roomResponse.addDouliuzi(cardsFour[i]); // 暗杠
			}
			for (int i = 0; i < countNumber[0]; i++) {
				roomResponse.addActions(cardsFour[0]); // 暗碰牌
			}
		}
		if (countNumber[1] > 0) {
			roomResponse.addEffectsIndex(GameConstants.WIK_WEI);
			for (int i = 0; i < countNumber[1]; i++) {
				roomResponse.addActions(cardsThree[i]);
			}
		}

		this.operate_player_action(seatIndex, false);

		RoomUtil.send_response_to_player(this, seatIndex, roomResponse);
	}

	/**
	 * @param seat_index
	 * @param room_rq
	 * @param type
	 * @return
	 */
	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		if (type == MsgConstants.REQUST_OPERATE_CARD) { // 操作牌
			Operate_Card_HongErShi operate = PBUtil.toObject(room_rq, Operate_Card_HongErShi.class);
			if (operate.getType() == 0) {

			} else {
				if (operate.getDeleteCount() > 0 && !operate.getDeleteCardsList().isEmpty()
						&& operate.getDeleteCardsList().size() == operate.getDeleteCount()) {
					int cards[] = new int[operate.getDeleteCount()];
					for (int i = 0; i < operate.getDeleteCount(); i++) {
						cards[i] = operate.getDeleteCardsList().get(i);
					}
					handler_operate_card(seat_index, cards, operate.getDeleteCount(), operate.getOperateCard(), operate.getOperateCode());
				}
			}
		}
		return true;
	}

	public boolean handler_operate_card(int seat_index, int otherCards[], int cardCount, int operate_card, int operate_code) {
		PlayerStatus playerStatus = this._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			this.log_info("没有这个操作:" + operate_code);
			return false;
		}
		if (playerStatus.has_action() == false) {
			this.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			this.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		if (operate_card != this._send_card_data) {
			this.log_player_error(seat_index, "操作牌,与当前牌不一样");
			return true;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			this.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < this.getTablePlayerNumber(); p++) {
			int i = (seat_index + p) % this.getTablePlayerNumber();
			if (i == target_player) {
				target_p = this.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[this.getTablePlayerNumber()];
		for (int p = 0; p < this.getTablePlayerNumber(); p++) {

			int i = (seat_index + p) % this.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (this._playerStatus[i].has_action()) {
				if (this._playerStatus[i].is_respone()) {
					cbUserActionRank = this.logic.get_action_rank(this._playerStatus[i].get_perform()) + this.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbUserActionRank = this.logic.get_action_list_rank(this._playerStatus[i]._action_count, this._playerStatus[i]._action)
							+ this.getTablePlayerNumber() - p;
				}

				if (this._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = this.logic.get_action_rank(this._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbTargetActionRank = this.logic.get_action_list_rank(this._playerStatus[target_player]._action_count,
							this._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = this._playerStatus[i].get_perform();
					target_p = this.getTablePlayerNumber() - p;
				}
			}
		}

		if (this._playerStatus[target_player].is_respone() == false) {
			this.log_info("优先级最高的人还没操作");
			return true;
		}

		int target_card = this._playerStatus[target_player]._operate_card;

		int hand_card_count_cur = this.GRR._card_count[seat_index];
		int cards_cur[] = Arrays.copyOf(this.GRR._cards_data[seat_index], hand_card_count_cur);
		this.operate_player_cards(seat_index, hand_card_count_cur, cards_cur, this.GRR._weave_count[seat_index], this.GRR._weave_items[seat_index]);

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			this._playerStatus[seat_index].clean_action();
			this._playerStatus[seat_index].clean_status();

			if (this._playerStatus[seat_index].lock_huan_zhang()) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.MAX_HH_COUNT];
					int hand_card_count = this.logic.switch_to_cards_data(this.GRR._cards_index[i], cards);
					this.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, this.GRR._weave_items[i], this.GRR._weave_count[i],
							GameConstants.INVALID_SEAT);
				}

				GameSchedule.put(new OutCardRunnable(this.getRoom_id(), seat_index, this._send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					int pao_type[] = new int[1];
					int action = this.estimate_player_respond_phz_chd(i, seat_index, this._send_card_data, pao_type, true);
					if (action != GameConstants.WIK_NULL) {
						this.exe_gang(i, seat_index, this._send_card_data, action, pao_type[0], true, true, false, 1000);
						return true;
					}
				}

				this.operate_player_get_card(seat_index, 0, null, GameConstants.INVALID_SEAT, false);

				int pai_count = 0;
				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					if (this.GRR._cards_index[seat_index][i] < 3)
						pai_count += this.GRR._cards_index[seat_index][i];
				}

				if (pai_count == 0) {
					this._is_xiang_gong[seat_index] = true;
					this.operate_player_xiang_gong_flag(seat_index, this._is_xiang_gong[seat_index]);
					int next_player = (seat_index + this.getTablePlayerNumber() + 1) % this.getTablePlayerNumber();

					this._playerStatus[seat_index].clean_action();
					this._playerStatus[seat_index].clean_status();
					this._current_player = next_player;
					this._last_player = next_player;

					this.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					return true;
				}

				int ting_cards[] = this._playerStatus[seat_index]._hu_cards;
				int ting_count = this._playerStatus[seat_index]._hu_card_count;

				if (ting_count > 0) {
					this.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					this.operate_chi_hu_cards(seat_index, 1, ting_cards);
				}

				this.exe_add_discard(seat_index, 1, new int[] { this._send_card_data }, true, 0);

				int next_player = (seat_index + this.getTablePlayerNumber() + 1) % this.getTablePlayerNumber();

				this._current_player = next_player;
				seat_index = next_player;
				this._last_player = next_player;

				this.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 0);
				this._last_card = this._send_card_data;
			}
			return true;
		}
		case GameConstants.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!this.logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				this.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			this.exe_chi_peng(target_player, seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, -1);
			return true;
		}
		case HongErShiConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!this.logic.remove_cards_by_index(this.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				this.log_player_error(target_player, "碰牌删除出错");
				return false;
			}

			this.exe_chi_peng(target_player, seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, -1);
			return true;
		}
		case GameConstants.WIK_PAO: {
			int pao_type[] = new int[1];
			int action = this.estimate_player_respond_phz_chd(target_player, seat_index, this._send_card_data, pao_type, true);
			if (action != GameConstants.WIK_NULL) {
				this.exe_gang(target_player, seat_index, this._send_card_data, action, pao_type[0], true, true, false, 1000);
			}
			return true;
		}
		case HongErShiConstants.WIK_ZI_MO:
		case HongErShiConstants.WIK_CHI_HU: {
			this.GRR._chi_hu_rights[target_player].set_valid(true);

			this.GRR._chi_hu_card[target_player][0] = operate_card;

			this._cur_banker = target_player;

			this._shang_zhuang_player = target_player;

			this.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_action == HongErShiConstants.WIK_ZI_MO) {
				this._player_result.zi_mo_count[target_player]++;
			}

			this.process_chi_hu_player_score_phz(target_player, seat_index, operate_card, true);

			this.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (this.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += this.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(this.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	public void load_player_info_data_HES(TableResponse_HES.Builder roomResponse) {
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
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			roomResponse.addPlayers(room_player);
		}
	}

	public void clearHasPiao() {
		// int count = getTablePlayerNumber();
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.haspiao[i] = 0;
		}
	}

	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		// _game_status = GameConstants.GS_MJ_WAIT;
		if (is_sys()) {
			// 金币场 防重入
			if (_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT)
				return false;

			_game_status = GameConstants.GS_MJ_FREE;
		} else {
			_game_status = GameConstants.GS_MJ_WAIT;
		}
		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		clearHasPiao();

		int count = getTablePlayerNumber();
		if (count == 0) {
			count = this.getTablePlayerNumber();
		}
		for (int i = 0; i < count; i++) {
			_player_ready[i] = 0;
		}

		_game_status = GameConstants.GS_MJ_WAIT;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_HES_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		TableResponse_HES.Builder game_end_hes = TableResponse_HES.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end_hes.setRoomInfo(room_info);
		game_end.setGamePlayerNumber(count);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		SimpleDateFormat smf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		game_end_hes.setEndTime(smf.format(new Date()));// 结束时间
		this.load_player_info_data(roomResponse);
		load_player_info_data_HES(game_end_hes);

		if (GRR != null) {
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);
			game_end_hes.setStartTime(smf.format(new Date(GRR._start_time * 1000L)));
			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);
			game_end_hes.setGameRound(_game_round);
			game_end_hes.setCurRound(_cur_round);
			game_end.setGamePlayerNumber(getTablePlayerNumber());
			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end_hes.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌

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
				// Int32ArrayResponse.Builder pnc =
				// Int32ArrayResponse.newBuilder();
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				// Int32ArrayResponse.Builder hc =
				// Int32ArrayResponse.newBuilder();
				// for (int j = 0; j < GameConstants.MAX_HH_COUNT; j++) {
				// hc.addItem(GRR._chi_hu_card[i][j]);
				// }

				// game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				// game_end.addHuCardArray(hc);
				game_end_hes.addHuCard(GRR._chi_hu_card[i][0]);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe(seat_index);

			for (int i = 0; i < count; i++) {
				_player_result.game_score[i] += GRR._game_score[i];

				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._card_count[i]; j++) {
					cs.addItem(GRR._cards_data[i][j]);
				}
				game_end.addCardsData(cs);// 牌
				game_end_hes.addRestCard(cs); // 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GRR._weave_count[i]; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setHuXi(GRR._weave_items[i][j].hu_xi);
					for (int wc = 0; wc < GRR._weave_items[i][j].weave_card.length; wc++) {
						weaveItem_item.addWeaveCard(GRR._weave_items[i][j].weave_card[wc]);
					}
					if (j < GRR._weave_count[i]) {
						weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					} else {
						weaveItem_item.setWeaveKind(GameConstants.WIK_NULL);
					}

					weaveItem_array.addWeaveItem(weaveItem_item);
				}

				orther_des[i] += " \n" + GRR._result_des[i];

				game_end_hes.addWeaveDes(getWeaveDesc(GRR._weave_items[i], GRR._weave_count[i]));
				game_end_hes.addOrtherDes(orther_des[i]);
				game_end.addWeaveItemArray(weaveItem_array);
				game_end_hes.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(GRR._game_score[i]);// 放炮的人？
				// game_end.addGangScore(lGangScore[i]);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				game_end_hes.addGetSocre(GRR._game_score[i]);
				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);
				game_end_hes.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < count; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);
				game_end_hes.addFanShu(GRR._lost_fan_shu[i][0]);

			}

			cal_game_end_count(); // 综合计算
		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round) {// 局数到了
				reason = GameConstants.Game_End_ROUND_OVER;//
				end = true;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));

				for (int r = 0; r < all_end.length; r++) {
					Int32ArrayResponse.Builder en = Int32ArrayResponse.newBuilder();
					for (int c = 0; c < all_end[r].length; c++) {
						en.addItem(all_end[r][c]);
					}
					game_end_hes.addEndDes(en);
				}

				for (int p = 0; p < getTablePlayerNumber(); p++) {
					game_end_hes.addEndScore(_player_result.game_score[p]);
				}
			} else {
				// 确定下局庄家
				// 以后谁胡牌,下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			// 兼容
			reason = GameConstants.Game_End_RELEASE_PLAY;
			end = true;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));

			for (int r = 0; r < all_end.length; r++) {
				Int32ArrayResponse.Builder en = Int32ArrayResponse.newBuilder();
				for (int c = 0; c < all_end[r].length; c++) {
					en.addItem(all_end[r][c]);
				}
				game_end_hes.addEndDes(en);
			}

			for (int p = 0; p < getTablePlayerNumber(); p++) {
				game_end_hes.addEndScore(_player_result.game_score[p]);
			}
		}
		game_end.setEndType(reason);
		game_end_hes.setReason(reason);

		////////////////////////////////////////////////////////////////////// 得分总的

		game_end.setCommResponse(PBUtil.toByteString(game_end_hes));
		roomResponse.setGameEnd(game_end);
		roomResponse.setCommResponse(PBUtil.toByteString(game_end_hes));

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, reason);

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
		if (!is_sys()) {
			GRR = null;
		}

		// 错误断言
		return false;
	}

	public String getWeaveDesc(WeaveItem[] weaveItems, int weaveCount) {
		StringBuffer des = new StringBuffer();
		int[] count = new int[4]; // 吃碰杠王

		for (int w = 0; w < weaveCount; w++) {
			switch (weaveItems[w].weave_kind) {
			case HongErShiConstants.WIK_CHI: {

				count[0]++;
				break;
			}
			case HongErShiConstants.WIK_PENG:
			case HongErShiConstants.WIK_AN_PENG: {

				count[1]++;
				break;

			}
			case GameConstants.GANG_TYPE_AN_GANG:
			case GameConstants.GANG_TYPE_JIE_GANG:
			case GameConstants.GANG_TYPE_ADD_GANG:
			case HongErShiConstants.WIK_AN_GANG:
			case HongErShiConstants.WIK_GANG: {

				count[2]++;
				break;

			}

			case HongErShiConstants.WIK_TOU: {

				for (int c = 0; c < weaveItems[w].weave_card.length; c++) {
					if (weaveItems[w].weave_card[c] != HongErShiConstants.MAGIC_CARD_KING) {
						continue;
					}
					count[3]++;
				}
				break;

			}
			default:
				break;
			}
		}

		// if (count[0] > 0)
		// des.append("吃x").append(count[0]).append('\n');
		if (count[1] > 0)
			des.append("碰x").append(count[1]).append('\n');
		if (count[2] > 0)
			des.append("杠x").append(count[2]).append('\n');
		if (count[3] > 0)
			des.append("王x").append(count[3]).append('\n');

		return des.toString();
	}

	public void cal_cha_jiao() {

		if (!has_rule(HongErShiConstants.RULE_LIU_JU_CHA_JIAO)) {
			return;
		}

		System.out.println();
		int ting_player_count = 0;
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			int ting_count = get_hh_ting_card_twenty_cal_cha_jiao(_playerStatus[p]._hu_cards, GRR._cards_index[p], GRR._weave_items[p],
					GRR._weave_count[p], p, p);
			if (ting_count != 0) {
				ting_player_count++;
				orther_des[p] = "查叫";
			} else {
				orther_des[p] = "被查叫";
			}
		}

		if (ting_player_count == 0 || ting_player_count == getTablePlayerNumber()) {
			return;
		}
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			int wFanShu = cha_jiao_limit[p];

			if (wFanShu == -1) {
				continue;
			}

			GRR._lost_fan_shu[p][0] = wFanShu;

			int lChiHuScore = 1;

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				wFanShu = cha_jiao_limit[p];
				if (i == p || cha_jiao_limit[i] != -1) {
					continue;
				}

				int s = lChiHuScore;
				int tempfanShu = wFanShu;
				if (super.player_bao_ting[i]) {
					tempfanShu += 3;
					GRR._lost_fan_shu[i][0] -= 3;
				}
				if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_GNEDOU)) {
					if (tempfanShu > getFanLimit()) {
						int m = 1 << getFanLimit();
						int n = (tempfanShu - getFanLimit()) * 2;
						tempfanShu = m + n;
					} else {
						tempfanShu = 1 << tempfanShu;
					}
				}
				if (tempfanShu == 0) {
					tempfanShu = 1;
				}
				if (GameDescUtil.has_rule(gameRuleIndexEx, HongErShiConstants.RULE_FAN_GNEDOU)) {
					s = tempfanShu;
				} else {
					s += tempfanShu;
				}

				GRR._game_score[i] -= s;
				GRR._game_score[p] += s;
			}
		}
	}

	public void cal_game_end_count() {

		int[] check_chr = new int[] { HongErShiConstants.WIK_TIAN_HU, HongErShiConstants.WIK_DI_HU, HongErShiConstants.WIK_GANG_SHANG_HUA,
				HongErShiConstants.WIK_GANG_SHANG_PAO, HongErShiConstants.WIK_ALL_BALCK, HongErShiConstants.WIK_ALL_RED,
				HongErShiConstants.WIK_CHA_JIAO, HongErShiConstants.WIK_RED_FIFTY, HongErShiConstants.WIK_BLACK_FIFTY };

		for (int i = 0; i < check_chr.length; i++) {
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				ChiHuRight chr = GRR._chi_hu_rights[p];
				if (!chr.opr_and(check_chr[i]).is_empty()) {
					all_end[i][p] += 1;
				}
			}
		}

		for (int r = 0; r < all_end.length; r++) {
			int all = 0;
			for (int c = 0; c < all_end[r].length - 1; c++) {
				all += all_end[r][c];
			}

			all_end[r][all_end[r].length - 1] = all;
		}
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[]) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
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
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);

				if ((weaveitems[j].weave_kind == GameConstants.WIK_TI_LONG || weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG
						|| weaveitems[j].weave_kind == GameConstants.WIK_AN_LONG_LIANG) && weaveitems[j].public_card == 0) {
					weaveItem_item.setCenterCard(0);
				} else {
					if (this.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) && this._xt_display_an_long[seat_index] == true)
						weaveItem_item.setCenterCard(0);
					else {
						weaveItem_item.setCenterCard(weaveitems[j].center_card);
						for (int c = 0; c < weaveitems[j].weave_card.length; c++) {
							if (weaveitems[j].weave_kind == HongErShiConstants.WIK_TOU) {
								if (weaveitems[j].weave_card[c] != 0) {
									weaveItem_item.addWeaveCard(weaveitems[j].weave_card[c]);
								}
							} else {
								weaveItem_item.addWeaveCard(weaveitems[j].weave_card[c]);
							}
						}
					}
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		this.send_response_to_other(seat_index, roomResponse);

		if (weave_count > 0) {
			roomResponse.clearWeaveItems();
			for (int j = 0; j < weave_count; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
				weaveItem_item.setPublicCard(weaveitems[j].public_card);
				weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
				weaveItem_item.setCenterCard(weaveitems[j].center_card);
				weaveItem_item.setHuXi(weaveitems[j].hu_xi);
				for (int c = 0; c < weaveitems[j].weave_card.length; c++) {
					if (weaveitems[j].weave_kind == HongErShiConstants.WIK_TOU) {
						if (weaveitems[j].weave_card[c] != 0) {
							weaveItem_item.addWeaveCard(weaveitems[j].weave_card[c]);
						}
					} else {
						weaveItem_item.addWeaveCard(weaveitems[j].weave_card[c]);
					}
				}
				roomResponse.addWeaveItems(weaveItem_item);
			}
		}

		// 手牌--将自己的手牌数据发给自己
		for (int j = 0; j < card_count; j++) {
			roomResponse.addCardData(cards[j]);
		}
		roomResponse.setHuXiCount(this._hu_xi[seat_index]);
		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	/**
	 * 发牌
	 * 
	 * @param seat_index
	 * @param count
	 * @param cards
	 * @param to_player
	 * @return
	 */
	public boolean operate_player_mo_card(int seat_index, int count, int cards[], int hand_card_count, int to_player, boolean sao) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);
		roomResponse.setType(MsgConstants.RESPONSE_HES_BU_CARD);

		PlayerMoCard.Builder playerMoCard = PlayerMoCard.newBuilder();
		playerMoCard.setTarget(seat_index);
		playerMoCard.setCardCount(count);
		playerMoCard.setTargetHandCardCount(count + hand_card_count);

		if (to_player == GameConstants.INVALID_SEAT) {
			if (sao == true) {
				for (int i = 0; i < count; i++) {
					playerMoCard.addCardData(GameConstants.BLACK_CARD);// 给别人 牌背
				}
			} else {
				for (int i = 0; i < count; i++) {
					playerMoCard.addCardData(cards[i]);// 给别人 牌数据
				}
			}

			roomResponse.setCommResponse(PBUtil.toByteString(playerMoCard));
			this.send_response_to_other(seat_index, roomResponse);
			playerMoCard.clearCardData();
			for (int i = 0; i < count; i++) {
				playerMoCard.addCardData(cards[i]);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(playerMoCard));
			GRR.add_room_response(roomResponse);
			if (seat_index != -1)
				this.send_response_to_player(seat_index, roomResponse);
			return true;

		} else {
			if (seat_index == to_player) {
				for (int i = 0; i < count; i++) {
					playerMoCard.addCardData(cards[i]);
				}
				// GRR.add_room_response(roomResponse);
				roomResponse.setCommResponse(PBUtil.toByteString(playerMoCard));
				return this.send_response_to_player(seat_index, roomResponse);
			} else {
				if (sao == true) {
					for (int i = 0; i < count; i++) {
						playerMoCard.addCardData(GameConstants.BLACK_CARD);// 给别人
					}
				} else {
					for (int i = 0; i < count; i++) {
						playerMoCard.addCardData(cards[i]);// 给别人 牌数据
					}
				}
				roomResponse.setCommResponse(PBUtil.toByteString(playerMoCard));
				// GRR.add_room_response(roomResponse);
				return this.send_response_to_special(seat_index, to_player, roomResponse);
			}
		}

	}

	public boolean exe_chi_peng(int seat_index, int provider, int action, int card, int type, int[] weave_cards) {
		// 出牌
		_last_player = provider;
		this._handler = this._handler_chi_peng;
		this._handler_chi_peng.reset_status(seat_index, provider, action, card, type, weave_cards);
		this._handler.exe(this);

		return true;
	}
}
