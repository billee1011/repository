package com.cai.game.mj.yu.dcwdh;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.constant.game.GameConstants_DCWDH;
import com.cai.common.constant.game.GameConstants_HanShouWang;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.type.PlayerRoomStatus;
import com.cai.common.util.SpringService;
import com.cai.common.util.Tuple;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.yu.dcwdh.handler.HandlerChiPeng_DCWDH;
import com.cai.game.mj.yu.dcwdh.handler.HandlerDispatchCard_DCWDH;
import com.cai.game.mj.yu.dcwdh.handler.HandlerGang_DCWDH;
import com.cai.game.mj.yu.dcwdh.handler.HandlerMaiMa_DCWDH;
import com.cai.game.mj.yu.dcwdh.handler.HandlerOutCardOperate_DCWDH;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.redis.service.RedisService;
import com.google.common.collect.Lists;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;

public class MJTable_DCWDH extends AbstractMJTable {

	private static final long serialVersionUID = 7594988584356233901L;

	private HandlerMaiMa_DCWDH _handler_mai_ma;

	public boolean banker_tian_hu;

	public int[] player_continue_gang_count;

	public int[] pao;

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_DCWDH();
		_handler_dispath_card = new HandlerDispatchCard_DCWDH();
		_handler_gang = new HandlerGang_DCWDH();
		_handler_out_card_operate = new HandlerOutCardOperate_DCWDH();
		_handler_mai_ma = new HandlerMaiMa_DCWDH();

		pao = new int[getTablePlayerNumber()];
	}

	public boolean exe_mai_ma() {
		set_handler(_handler_mai_ma);
		_handler_mai_ma.exe(this);
		return true;
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_mai_ma.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_DCWDH.GAME_RULE_PLAYER_2)) {
			return 2;
		}
		if (has_rule(GameConstants_DCWDH.GAME_RULE_PLAYER_3)) {
			return 3;
		}

		if (playerNumber > 0) {
			return playerNumber;
		}
		return 4;
	}

	@Override
	protected void init_shuffle() {
		_repertory_card = new int[GameConstants_DCWDH.CARD_DATA_DAI_FENG.length];
		shuffle(_repertory_card, GameConstants_DCWDH.CARD_DATA_DAI_FENG);
	};

	@Override
	public boolean reset_init_data() {
		super.reset_init_data();

		banker_tian_hu = false;
		player_continue_gang_count = new int[getTablePlayerNumber()];

		return true;
	}

	@Override
	protected boolean on_game_start() {
		if (has_rule(GameConstants_DCWDH.GAME_RULE_MAI_MA) && _cur_round == 1) {
			exe_mai_ma();
			return true;
		}
		return on_game_start_real();
	}

	/**
	 * 加载房间里的玩家信息
	 * 
	 * @param roomResponse
	 */
	@Override
	public void load_player_info_data(RoomResponse.Builder roomResponse) {
		Player rplayer;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			rplayer = this.get_players()[i];
			if (rplayer == null)
				continue;
			RoomPlayerResponse.Builder room_player = newPlayerBaseBuilder(rplayer);
			room_player.setAccountId(rplayer.getAccount_id());
			// room_player.setHeadImgUrl(rplayer.getAccount_icon());
			room_player.setIp(rplayer.getAccount_ip());
			// room_player.setUserName(rplayer.getNick_name());
			room_player.setSeatIndex(rplayer.get_seat_index());
			room_player.setOnline(rplayer.isOnline() ? 1 : 0);
			room_player.setIpAddr(rplayer.getAccount_ip_addr());
			room_player.setSex(rplayer.getSex());
			room_player.setScore(_player_result.game_score[i]);
			room_player.setReady(_player_ready[i]);
			room_player.setPao(pao[i]);
			room_player.setNao(_player_result.nao[i]);
			room_player.setQiang(_player_result.qiang[i]);
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			room_player.setHasPiao(_player_result.haspiao[i]);
			room_player.setBiaoyan(_player_result.biaoyan[i]);
			room_player.setZiBa(_player_result.ziba[i]);
			room_player.setDuanMen(_player_result.duanmen[i]);
			room_player.setOpenThree(_player_open_less[i] == 0 ? false : true);

			room_player.setGvoiceStatus(rplayer.getGvoiceStatus());

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

	public boolean on_game_start_real() {
		_game_status = GameConstants_DCWDH.GS_MJ_PLAY;

		_logic.clean_magic_cards();
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_DCWDH.MAX_COUNT];

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_DCWDH.MAX_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_DCWDH.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_DCWDH.MAX_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(GRR._left_card_count);
		GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card_bao_hu(_playerStatus[i]._hu_cards, GRR._cards_index[i], GRR._weave_items[i],
					GRR._weave_count[i], i);
			if (_playerStatus[i]._hu_card_count > 0) {
				if (_cur_banker == i) {
					banker_tian_hu = true;
				}
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants_DCWDH.DispatchCard_Type_Tian_Hu, GameConstants_DCWDH.DELAY_SEND_CARD_DELAY);
		return true;
	}

	public void check_banker_tian_hu(int[] cards_index, int seat_index, int cur_card) {
		int[] temp_cards_index = Arrays.copyOf(cards_index, cards_index.length);
		temp_cards_index[_logic.switch_to_card_index(cur_card)]++;

		for (int c = 0; c < temp_cards_index.length; c++) {
			if (temp_cards_index[c] == 0) {
				continue;
			}

			temp_cards_index[c]--;

			int hu_card_count = get_ting_card_bao_hu(new int[GameConstants_DCWDH.MAX_ZI_FENG], temp_cards_index, null, 0, seat_index);
			if (hu_card_count > 0) {
				banker_tian_hu = true;
			}
			temp_cards_index[c]++;
		}
	}

	// 玩家进入房间
	@Override
	public boolean handler_enter_room(Player player) {
		player.setStatus(PlayerRoomStatus.NORMAL);

		if (getRuleValue(GameConstants.GAME_RULE_IP) > 0) {
			for (Player tarplayer : get_players()) {
				if (tarplayer == null)
					continue;

				// logger.error("tarplayer
				// ip=="+tarplayer.getAccount_ip()+"player
				// ip=="+player.getAccount_ip());
				if (player.getAccount_id() == tarplayer.getAccount_id())
					continue;
				if ((!is_sys()) && StringUtils.isNotEmpty(tarplayer.getAccount_ip()) && StringUtils.isNotEmpty(player.getAccount_ip())
						&& player.getAccount_ip().equals(tarplayer.getAccount_ip())) {
					player.setRoom_id(0);// 把房间信息清除--
					send_error_notify(player, 1, "不允许相同ip进入");
					return false;
				}
			}
		}

		if (matchId > 0) {
			return false;
		}

		if (!canEnter(player)) {
			player.setRoom_id(0);// 把房间信息清除--
			if (GameConstants.CREATE_ROOM_NEW_COIN == getCreate_type()) {
				send_error_notify(player, 1, "房间不存在！");
			} else {
				send_error_notify(player, 1, "游戏中途不可进");
			}

			return false;
		}

		int seat_index = GameConstants.INVALID_SEAT;

		// if (playerNumber == 0) {// 未开始 才分配位置
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (get_players()[i] == null) {
				get_players()[i] = player;
				seat_index = i;
				break;
			}
		}
		// }

		if (seat_index == GameConstants.INVALID_SEAT && player.get_seat_index() != GameConstants.INVALID_SEAT
				&& player.get_seat_index() < getTablePlayerNumber()) {
			Player tarPlayer = get_players()[player.get_seat_index()];
			if (tarPlayer != null && tarPlayer.getAccount_id() == player.getAccount_id()) {
				seat_index = player.get_seat_index();
			}
		}

		if (seat_index == GameConstants.INVALID_SEAT) {
			player.setRoom_id(0);// 把房间信息清除--
			if (GameConstants.CREATE_ROOM_NEW_COIN == getCreate_type()) {
				send_error_notify(player, 1, "房间不存在");
			} else {
				send_error_notify(player, 1, "游戏已经开始");
			}

			return false;
		}

		player.set_seat_index(seat_index);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ENTER_ROOM);
		roomResponse.setGameStatus(_game_status);
		roomResponse.setIsGoldRoom(is_sys());
		roomResponse.setAppId(this.getGame_id());

		// WalkerGeek 新人加入清空之前少人的确认
		clear_open_less();

		this.load_room_info_data(roomResponse);
		this.load_player_info_data(roomResponse);
		//
		send_response_to_player(player.get_seat_index(), roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 进入房间
		send_response_to_room(roomResponse);
		godViewObservers().sendAll(roomResponse);
		// 同步数据

		// ========同步到中心========
		roomRedisModel.getPlayersIdSet().add(player.getAccount_id());
		roomRedisModel.getNames().add(player.getNick_name());
		int cur_player_num = 0;
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (this.get_players()[i] != null) {
				cur_player_num++;
			}
		}
		roomRedisModel.setCur_player_num(cur_player_num);
		roomRedisModel.setGame_round(this._game_round);
		// 写入redis
		SpringService.getBean(RedisService.class).hSet(RedisConstant.ROOM, getRoom_id() + "", roomRedisModel);

		onPlayerEnterUpdateRedis(player.getAccount_id());

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);

		if (player.getAccount_id() == getRoom_owner_account_id()) {
			this.getCreate_player().set_seat_index(player.get_seat_index());
		}

		return true;
	}

	/**
	 * 允许少人模式扩展
	 */
	@Override
	public boolean handler_requst_open_less(Player player, int playerNum) {
		if (GameConstants.GS_MJ_FREE != _game_status) {
			return false;
		}

		// 不允许一个人开启游戏，兼容客户端bug
		if (playerNum == 1) {
			return false;
		}

		// 判断规则是否存在
		if (getRuleValue(GameConstants_DCWDH.GAME_RULE_PLAYER_4) == 0) {
			return false;
		}
		// 已经开局 返回
		if (this._cur_round != 0) {
			return false;
		}

		int readys = 0;
		int openLess = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_ready[i] == 1) {
				readys++;
			}
			if (_player_open_less[i] == 1) {
				openLess++;
			}
		}
		// 变更少人模式数组
		int less = playerNum < getTablePlayerNumber() ? 1 : 0;
		if ((openLess + 1) == playerNum) {

			this.changePlayer();
			for (int j = 0; j < this.get_players().length; j++) {
				if (this.get_players()[j] != null) {
					_player_open_less[j] = less;
				} else {
					_player_open_less[j] = 0;
				}
			}

			playerNumber = playerNum;
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		} else if (playerNum == GameConstants.INVALID_SEAT) {// 取消勾选少人模式
			less = 0;
			playerNumber = playerNum;
			_player_open_less[player.get_seat_index()] = less;
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		} else {
			_player_open_less[player.get_seat_index()] = less;
		}
		// 通知客户端
		this.refresh_less_player();

		if ((openLess + 1) == readys && readys == playerNum) {
			this.changePlayer();
			playerNumber = playerNum;
			this.refresh_less_player();
			// 最小开局人数
			this.syncMinPlayerCountToClub(getTablePlayerNumber());

			handler_game_start();
			this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, true);
		}

		return false;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_DCWDH.MAX_INDEX];
		for (int i = 0; i < GameConstants_DCWDH.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_DCWDH.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants_DCWDH.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			if (GameConstants_DCWDH.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_DCWDH.CHR_ZI_MO, seat_index)) {
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

	public int get_ting_card_bao_hu(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_DCWDH.MAX_INDEX];
		for (int i = 0; i < GameConstants_DCWDH.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int max_ting_count = GameConstants_DCWDH.MAX_ZI_FENG;
		int real_max_ting_count = GameConstants_DCWDH.MAX_ZI_FENG;

		for (int i = 0; i < max_ting_count; i++) {
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();

			int[] check_feng = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);
			check_feng[_logic.switch_to_card_index(cbCurrentCard)]++;
			if (check_bao_hu(check_feng) && GameConstants_DCWDH.WIK_CHI_HU == analyse_bao_hu(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard,
					chr, GameConstants_DCWDH.CHR_ZI_MO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			} else if (GameConstants_DCWDH.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, cbCurrentCard, chr,
					GameConstants_DCWDH.CHR_ZI_MO, seat_index)) {
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

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int seat_index, int send_card_data) {
		// 设置变量
		int cbActionMask = GameConstants_DCWDH.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants_DCWDH.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants_DCWDH.WIK_GANG;

				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants_DCWDH.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind != GameConstants_DCWDH.WIK_PENG) {
					continue;
				}
				for (int j = 0; j < GameConstants_DCWDH.MAX_INDEX; j++) {
					if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
						continue;
					}

					if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
						cbActionMask |= GameConstants_DCWDH.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants_DCWDH.GANG_TYPE_ADD_GANG;
						break;
					}
				}
			}
		}

		return cbActionMask;
	}

	public boolean check_bao_hu(int[] cbCardIndexTemp) {
		if (zi_have_one(cbCardIndexTemp)) {
			return true;
		}

		// 计算单牌
		int[] cards_index = Arrays.copyOf(cbCardIndexTemp, cbCardIndexTemp.length);
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			if (cards_index[i] == 3) {
				cards_index[i] = 0;
			}
		}

		if (_logic.get_card_count_by_index(cards_index) > 5 && !zi_have_one(cards_index)) {
			return false;
		}
		return true;
	}

	public int analyse_bao_hu(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int seat_index) {
		int cbCardIndexTemp[] = new int[GameConstants_DCWDH.MAX_INDEX];
		for (int i = 0; i < GameConstants_DCWDH.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		if (cur_card != 0) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		if (card_type != GameConstants_DCWDH.HU_CARD_TYPE_GANG_KAI_HUA && _logic.get_card_count_by_index(cbCardIndexTemp) > 4
				&& !check_bao_hu(cbCardIndexTemp)) {
			return GameConstants_DCWDH.WIK_NULL;
		}

		chiHuRight.set_empty();
		if (card_type == GameConstants_DCWDH.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_ZI_MO);
		} else if (card_type == GameConstants_DCWDH.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_GNAG_KAI);
		} else {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_SHU_FAN);
		}

		List<Tuple<Integer, Integer, Integer>> list = Lists.newArrayList();
		for (int c = 0; c < GameConstants_DCWDH.MAX_ZI_FENG; c++) {
			if (cbCardIndexTemp[c] == 0) {
				continue;
			}
			cbCardIndexTemp[c]--;

			for (int i = 0; i < GameConstants_DCWDH.MAX_ZI_FENG; i++) {
				ChiHuRight chr = new ChiHuRight();
				if (GameConstants_DCWDH.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItems, weave_count, _logic.switch_to_card_data(i),
						chr, GameConstants_DCWDH.CHR_ZI_MO, seat_index)) {
					if (!chr.opr_and(GameConstants_DCWDH.CHR_DAN_DIAO_REAL).is_empty()) {
						list.add(new Tuple<Integer, Integer, Integer>(2, c, GameConstants_DCWDH.CHR_DAN_DIAO));
						break;
					}
					if (!chr.opr_and(GameConstants_DCWDH.CHR_PENG_PENG_HU_REAL).is_empty()) {
						if (card_type == GameConstants_DCWDH.HU_CARD_TYPE_GANG_KAI_HUA) {
							list.add(new Tuple<Integer, Integer, Integer>(2, c, GameConstants_DCWDH.CHR_PENG_PENG_HU));
						} else {
							list.add(new Tuple<Integer, Integer, Integer>(1, c, GameConstants_DCWDH.CHR_PENG_PENG_HU));
						}
						break;
					}
					if (!chr.opr_and(GameConstants_DCWDH.CHR_QI_XING_LAN_REAL).is_empty()) {
						list.add(new Tuple<Integer, Integer, Integer>(1, c, GameConstants_DCWDH.CHR_QI_XING_LAN));
						break;
					}
					if (card_type == GameConstants_DCWDH.HU_CARD_TYPE_GANG_KAI_HUA) {
						list.add(new Tuple<Integer, Integer, Integer>(1, c, GameConstants_DCWDH.CHR_PING_HU));
						break;
					}
				}
			}
			cbCardIndexTemp[c]++;
		}

		if (list.isEmpty()) {
			return GameConstants_DCWDH.WIK_NULL;
		}

		Collections.sort(list, new Comparator<Tuple<Integer, Integer, Integer>>() {
			@Override
			public int compare(Tuple<Integer, Integer, Integer> left, Tuple<Integer, Integer, Integer> right) {
				if (left.getLeft() == right.getLeft()) {
					return right.getRight() - left.getRight();
				} else {
					return right.getLeft() - left.getLeft();
				}
			}
		});
		chiHuRight.opr_or(list.get(0).getRight());
		return GameConstants_DCWDH.WIK_CHI_HU;
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {
		if (card_type == GameConstants_DCWDH.HU_CARD_TYPE_ZIMO) {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_ZI_MO);
		} else if (card_type == GameConstants_DCWDH.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_GNAG_KAI_REAL);
		} else {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_SHU_FAN);
		}

		int cbCardIndexTemp[] = new int[GameConstants_DCWDH.MAX_INDEX];
		for (int i = 0; i < GameConstants_DCWDH.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}
		cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;

		// 十三烂
		if (isShiSanLan(cbCardIndexTemp, weaveItems, weave_count)) {
			if (zi_have_one(cbCardIndexTemp)) {
				chiHuRight.opr_or(GameConstants_DCWDH.CHR_QI_XING_LAN_REAL);
			} else {
				chiHuRight.opr_or(GameConstants_DCWDH.CHR_LAN_HU);
			}
			return GameConstants_DCWDH.WIK_CHI_HU;
		}

		// 小七对
		if (GameConstants.WIK_NULL != _logic.is_qi_xiao_dui(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_XIAO_QI_DUI);
			return GameConstants_DCWDH.WIK_CHI_HU;
		}

		// 十三幺
		if (isShiSanYao(cbCardIndexTemp, weaveItems, weave_count)) {
			chiHuRight.opr_or(GameConstants_DCWDH.CHR_SHI_SAN_YAO);
			return GameConstants_DCWDH.WIK_CHI_HU;
		}

		// 碰碰胡
		if (AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
				_logic.get_magic_card_count())) {
			if (_logic.get_card_count_by_index(cards_index) == 1) {
				chiHuRight.opr_or(GameConstants_DCWDH.CHR_DAN_DIAO_REAL);
			} else {
				chiHuRight.opr_or(GameConstants_DCWDH.CHR_PENG_PENG_HU_REAL);
			}
			return GameConstants_DCWDH.WIK_CHI_HU;
		}

		// 平胡
		if (AnalyseCardUtil.analyse_feng_chi_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), null, 0)) {
			return GameConstants_DCWDH.WIK_CHI_HU;
		}

		chiHuRight.set_empty();
		return GameConstants_DCWDH.WIK_NULL;
	}

	private boolean isShiSanYao(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (j == i || j == i + 8) {
					continue;
				}
				if (cbCardIndexTemp[j] != 0) {
					return false;
				}
			}
		}

		for (int w = 0; w < weaveCount; w++) {
			if (_logic.switch_to_card_index(weaveItems[w].center_card) >= GameConstants.MAX_ZI) {
				continue;
			}

			int card_value = _logic.get_card_value(weaveItems[w].center_card);
			if (card_value != 1 && card_value != 9) {
				return false;
			}
		}
		return true;
	}

	private boolean isShiSanLan(int[] cards_index, WeaveItem[] weaveItems, int weaveCount) {
		if (weaveCount != 0) {
			return false;
		}

		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
		for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		for (int i = 0; i < 27; i += 9) {
			for (int j = i; j < i + 9; j++) {
				if (cbCardIndexTemp[j] == 0) {
					continue;
				}
				if (cbCardIndexTemp[j] > 1) {
					return false;
				}

				int limitIndex = i + 9;
				if (j + 1 < limitIndex && cbCardIndexTemp[j + 1] != 0) {
					return false;
				}
				if (j + 2 < limitIndex && cbCardIndexTemp[j + 2] != 0) {
					return false;
				}
			}
		}

		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				continue;
			}
			if (cbCardIndexTemp[i] > 1) {
				return false;
			}
		}

		return true;
	}

	// 七张字牌各一张
	private boolean zi_have_one(int cbCardIndexTemp[]) {
		for (int i = GameConstants.MAX_ZI; i < GameConstants.MAX_ZI_FENG; i++) {
			if (cbCardIndexTemp[i] == 0) {
				return false;
			}
		}
		return true;
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

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_DCWDH.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0 && !_playerStatus[i].is_bao_ting()) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_DCWDH.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
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
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = getBeiShu(chr, seat_index);

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
		} else {// 点炮
			GRR._lost_fan_shu[provide_index][seat_index] = wFanShu;
		}

		float lChiHuScore = getFanShu(chr);

		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}

				float s = lChiHuScore * wFanShu; // 自摸加1分
				s = s > getLimitFen() ? getLimitFen() : s;

				if (i == GRR._banker_player || seat_index == GRR._banker_player) {
					s *= 2;
				}

				_player_result.piao_lai_count[seat_index] += GRR._count_pick_niao;
				s += GRR._count_pick_niao;
				s += pao[seat_index] != -1 ? pao[seat_index] : 0;
				s += pao[i] != -1 ? pao[i] : 0;

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	private int getFanShu(ChiHuRight chr) {
		if (!chr.opr_and(GameConstants_DCWDH.CHR_DAN_DIAO_REAL).is_empty()) {
			return 4;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_QI_XING_LAN_REAL).is_empty()) {
			return 2;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_PENG_PENG_HU_REAL).is_empty()) {
			return 2;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_DAN_DIAO).is_empty()) {
			return 2;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_XIAO_QI_DUI).is_empty()) {
			return 4;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_TIAN_HU_REAL).is_empty()) {
			return 2;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_TIAN_HU).is_empty()) {
			return 1;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_PENG_PENG_HU).is_empty()) {
			return 1;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_SHI_SAN_YAO).is_empty()) {
			return 1;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_LAN_HU).is_empty()) {
			return 1;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_QI_XING_LAN).is_empty()) {
			return 1;
		}
		return 1;
	}

	private int getBeiShu(ChiHuRight chr, int seat_index) {
		int bei = 1;
		if (!chr.opr_and(GameConstants_DCWDH.CHR_GNAG_KAI_REAL).is_empty()) {
			bei *= 2;
		}
		if (!chr.opr_and(GameConstants_DCWDH.CHR_GNAG_KAI).is_empty()
				&& (!chr.opr_and(GameConstants_DCWDH.CHR_PENG_PENG_HU).is_empty() || !chr.opr_and(GameConstants_DCWDH.CHR_DAN_DIAO).is_empty())) {
			bei *= 2;
		}
		if (player_continue_gang_count[seat_index] > 1) {
			bei *= 4;
		}
		return bei;
	}

	private int getLimitFen() {
		return 4;
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
					if (type == GameConstants_DCWDH.CHR_TIAN_HU_REAL || type == GameConstants_DCWDH.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == GameConstants_DCWDH.CHR_PENG_PENG_HU_REAL || type == GameConstants_DCWDH.CHR_PENG_PENG_HU) {
						result.append(" 碰碰胡");
					}
					if (type == GameConstants_DCWDH.CHR_DAN_DIAO_REAL || type == GameConstants_DCWDH.CHR_DAN_DIAO) {
						result.append(" 大碰碰胡");
					}
					if (type == GameConstants_DCWDH.CHR_QI_XING_LAN_REAL || type == GameConstants_DCWDH.CHR_QI_XING_LAN) {
						result.append(" 七星烂胡");
					}
					if (type == GameConstants_DCWDH.CHR_GNAG_KAI || type == GameConstants_DCWDH.CHR_GNAG_KAI_REAL) {
						result.append(" 杠开");
					}
					if (type == GameConstants_DCWDH.CHR_LAN_HU) {
						result.append(" 十三烂");
					}
					if (type == GameConstants_DCWDH.CHR_SHI_SAN_YAO) {
						result.append(" 幺胡");
					}
					if (type == GameConstants_DCWDH.CHR_XIAO_QI_DUI) {
						result.append(" 小七对");
					}

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
			// if (fang_gang > 0) {
			// result.append(" 放杠X" + fang_gang);
			// }
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			if (player_continue_gang_count[player] > 1) {
				result.append(" 连杠");
			}

			GRR._result_des[player] = result.toString();
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
		cleanActionAfterHu();
		// filtrate_right(seat_index, chr);

		if (is_mj_type(GameConstants.GAME_TYPE_ZZ) || is_mj_type(GameConstants.GAME_TYPE_HZ) || is_mj_type(GameConstants.GAME_TYPE_FLS_HZ_LX)
				|| is_mj_type(GameConstants.GAME_TYPE_HENAN_ZHUAN_ZHUAN) || is_mj_type(GameConstants.GAME_TYPE_NEW_SAN_MEN_XIA)) { // liuyan
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

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			// 显示胡牌
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[p], cards);
			if (p == seat_index) {
				cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
				hand_card_count++;
			}
			this.operate_show_card(p, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);
		}
		return;
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
	public void set_niao_card(int seat_index, int card, boolean show, int add_niao) {

		for (int i = 0; i < GameConstants.MAX_NIAO_CARD; i++) {
			GRR._cards_data_niao[i] = GameConstants.INVALID_VALUE;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			GRR._player_niao_count[i] = 0;
			for (int j = 0; j < GameConstants.MAX_NIAO_CARD; j++) {
				GRR._player_niao_cards[i][j] = GameConstants.INVALID_VALUE;
			}
		}

		GRR._show_bird_effect = show;
		GRR._count_niao = getCsDingNiaoNum();
		GRR._count_niao = GRR._count_niao > GRR._left_card_count ? GRR._left_card_count : GRR._count_niao;
		if (GRR._count_niao == 0) {
			return;
		}

		if (GRR._count_niao > GameConstants.ZHANIAO_0) {
			if (card == GameConstants.INVALID_VALUE) {
				int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX];
				_logic.switch_to_cards_index(_repertory_card, _all_card_len - GRR._left_card_count, GRR._count_niao, cbCardIndexTemp);
				GRR._left_card_count -= GRR._count_niao;
				_logic.switch_to_cards_data(cbCardIndexTemp, GRR._cards_data_niao);
			} else {
				for (int i = 0; i < GRR._count_niao; i++) {
					GRR._cards_data_niao[i] = card;
				}
			}
		}

		// 中鸟个数z
		for (int i = 0; i < GRR._count_niao; i++) {
			int seat = -1;
			if (GRR._cards_data_niao[i] >= GameConstants.HZ_MAGIC_CARD) {
				if (getTablePlayerNumber() == 2) {
					seat = (GRR._banker_player + 1 + getTablePlayerNumber()) % getTablePlayerNumber();
				} else if (getTablePlayerNumber() == 3) {
					seat = (GRR._banker_player + (_logic.get_card_value(GRR._cards_data_niao[i]) - 5) + getTablePlayerNumber())
							% getTablePlayerNumber();
				} else {
					seat = (GRR._banker_player + (_logic.get_card_value(GRR._cards_data_niao[i])) + getTablePlayerNumber()) % getTablePlayerNumber();
				}
			} else {
				seat = (GRR._banker_player + (_logic.get_card_value(GRR._cards_data_niao[i]) - 1) + getTablePlayerNumber()) % getTablePlayerNumber();
			}
			if (seat == seat_index) {
				boolean flag = true;
				if (GRR._cards_data_niao[i] >= GameConstants.HZ_MAGIC_CARD - 4 && GRR._cards_data_niao[i] < GameConstants.HZ_MAGIC_CARD
						&& getTablePlayerNumber() != 4) {
					flag = false;
				}

				if (flag) {
					GRR._count_pick_niao++;
					GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this.set_ding_niao_valid(GRR._cards_data_niao[i], true);//
					GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], true);//
				} else {
					GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);//
					GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);
				}
			} else {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);//
				GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);
			}
		}
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		if (has_rule(GameConstants_DCWDH.GAME_RULE_MAI_MA)) {
			return GameConstants.ZHANIAO_4;
		}

		return GameConstants.ZHANIAO_0;
	}

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x1, 0x1, 0x1, 0x2, 0x2, 0x2, 0x11, 0x11, 0x11, 0x31, 0x31, 0x32, 0x33 };
		int[] cards_of_player1 = new int[] { 0x1, 0x1, 0x1, 0x2, 0x2, 0x2, 0x11, 0x11, 0x11, 0x31, 0x31, 0x32, 0x33 };
		int[] cards_of_player2 = new int[] { 0x15, 0x16, 0x18, 0x18, 0x17, 0x17, 0x17, 0x25, 0x25, 0x25, 0x29, 0x29, 0x29 };
		int[] cards_of_player3 = new int[] { 0x12, 0x12, 0x12, 0x14, 0x14, 0x14, 0x19, 0x19, 0x27, 0x29, 0x29, 0x29, 0x19 };

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			for (int j = 0; j < GameConstants_KWX.MAX_INDEX; j++) {
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
}
