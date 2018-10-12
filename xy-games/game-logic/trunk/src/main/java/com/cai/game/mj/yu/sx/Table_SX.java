package com.cai.game.mj.yu.sx;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJType;
import com.cai.game.util.AnalyseCardUtil;
import com.cai.service.MongoDBServiceImpl;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.GameEndResponse;
import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.PlayerResultResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomPlayerResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.shangQiu.ShangQiuRsp.OtherResponse;

public class Table_SX extends AbstractMJTable {

	private static final long serialVersionUID = 2373854457078205932L;

	public MJHandlerPiao_SX _handler_piao;

	public int[] player_mai_ma_count;
	public int[][] player_mai_ma_data;
	public int bu_hua_count;
	public int[] player_ma_get_score;
	public int[] player_ming_gang_all_count;
	public int[] player_jie_gang_all_count;

	public Table_SX() {
		super(MJType.GAME_TYPE_SHAN_WEI);
	}

	@Override
	protected void onInitTable() {
		_handler_chi_peng = new HandlerChiPeng_SX();
		_handler_dispath_card = new MJHandlerDispatchCard_SX();
		_handler_gang = new HandlerGang_SX();
		_handler_out_card_operate = new HandlerOutCardOperate_SX();
		_handler_piao = new MJHandlerPiao_SX();
		player_ming_gang_all_count = new int[4];
		player_jie_gang_all_count = new int[4];
	}

	@Override
	public void progress_banker_select() {
		if (_cur_round == 1) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % getTablePlayerNumber();//

			_shang_zhuang_player = GameConstants.INVALID_SEAT;
			_lian_zhuang_player = GameConstants.INVALID_SEAT;
		}
	}

	@Override
	public int getTablePlayerNumber() {
		if (has_rule(GameConstants_SX.GAME_RULE_PLAYER_2))
			return 2;
		if (has_rule(GameConstants_SX.GAME_RULE_PLAYER_3))
			return 3;
		return 4;
	}

	@Override
	public boolean reset_init_data() {
		super.reset_init_data();

		player_mai_ma_count = new int[getTablePlayerNumber()];
		player_mai_ma_data = new int[getTablePlayerNumber()][get_ma_num()];
		player_ma_get_score = new int[getTablePlayerNumber()];

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			_playerStatus[p].clear_cards_abandoned_gang();
		}
		return true;
	}

	@Override
	public PlayerResultResponse.Builder process_player_result(int result) {

		this.huan_dou(result);

		// 大赢家
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_result.win_order[i] = -1;
		}
		int win_idx = 0;
		float max_score = 0;
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int winner = -1;
			float s = -999999;
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			for (int j = 0; j < getTablePlayerNumber(); j++) {
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
			player_result.addMingGangCount(player_ming_gang_all_count[i]);
			player_result.addYingXiCount(player_jie_gang_all_count[i]);
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

	/**
	 * //执行发牌 是否延迟
	 * 
	 * @param seat_index
	 * @param delay
	 * @return
	 */
	public boolean exe_dispatch_card(final int seat_index, final int type, final int card, int delay) {
		if (delay > 0) {
			Table_SX mjTable_SX = this;
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					set_handler(_handler_dispath_card);
					_handler_dispath_card.reset_status(seat_index, type, card);
					_handler.exe(mjTable_SX);
				}
			}, delay, TimeUnit.MILLISECONDS);
		} else {
			// 发牌
			this.set_handler(this._handler_dispath_card);
			this._handler_dispath_card.reset_status(seat_index, type, card);
			this._handler.exe(this);
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

		_game_status = GameConstants.GS_MJ_PLAY;

		// 信阳麻将
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;
		banker_count[_current_player]++;

		if (has_rule(GameConstants_SX.GAME_RULE_HZ_LAI_ZI) || has_rule(GameConstants_SX.GAME_RULE_ZI_MO)) {
			_logic.clean_magic_cards();
			_logic.add_magic_card_index(_logic.switch_to_card_index(GameConstants_SX.HZ_MAGIC_CARD));
			GRR._especial_card_count = 1;
			GRR._especial_show_cards[0] = GameConstants_SX.HZ_MAGIC_CARD;
			_repertory_card = new int[GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI.length];
			shuffle(_repertory_card, GameConstants.CARD_DATA_HONG_ZHONG_LAI_ZI);
		} else {
			_repertory_card = new int[GameConstants.CARD_DATA_WAN_TIAO_TONG.length];
			shuffle(_repertory_card, GameConstants.CARD_DATA_WAN_TIAO_TONG);
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

		if (get_ma_num() > 0) {
			this.set_handler(this._handler_piao);
			this._handler_piao.exe(this);
			return true;
		} else {
			return on_game_start();
		}
	}

	public int get_ma_num() {
		if (has_rule(GameConstants_SX.GAME_RULE_MAI_MA_1)) {
			return 1;
		}
		if (has_rule(GameConstants_SX.GAME_RULE_MAI_MA_2)) {
			return 2;
		}
		if (has_rule(GameConstants_SX.GAME_RULE_MAI_MA_3)) {
			return 3;
		}
		return 0;
	}

	@Override
	protected boolean on_game_start() {
		_game_status = GameConstants_SX.GS_MJ_PLAY;

		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		gameStartResponse.setBankerPlayer(GRR._banker_player);
		gameStartResponse.setCurrentPlayer(_current_player);
		gameStartResponse.setLeftCardCount(GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants_SX.MAX_COUNT];
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {

			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants_SX.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					gameStartResponse.addCardData(hand_cards[i][j] + GameConstants_SX.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					gameStartResponse.addCardData(hand_cards[i][j]);
				}
			}

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			load_room_info_data(roomResponse);
			load_common_status(roomResponse);
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(_current_player == GameConstants_SX.INVALID_SEAT ? _resume_player : _current_player);
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

			for (int j = 0; j < GameConstants_SX.MAX_COUNT; j++) {
				if (_logic.is_magic_card(hand_cards[i][j])) {
					cards.addItem(hand_cards[i][j] + GameConstants_SX.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					cards.addItem(hand_cards[i][j]);
				}
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

		exe_dispatch_card(_current_player, GameConstants_SX.DispatchCard_Type_Tian_Hu, GameConstants_SX.DELAY_SEND_CARD_DELAY);

		return true;
	}

	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int seat_index) {
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
					GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
				cards[count] = cbCurrentCard;
				count++;
			}
		}

		if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card(cbCardIndexTemp, weaveItem, cbWeaveCount, GameConstants_SX.HZ_MAGIC_CARD, chr,
				GameConstants.HU_CARD_TYPE_ZIMO, seat_index)) {
			cards[count] = GameConstants_SX.HZ_MAGIC_CARD;
			count++;
		}

		if (count == 0) {
		} else if (count >= max_ting_count) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	public boolean operate_mai_ma_card(int to_player, boolean show_cards) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_SX_MAI_MA_CARDS);

		for (int p = 0; p < getTablePlayerNumber(); p++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			roomResponse.addOutCardTingCount(player_mai_ma_count[p]);
			for (int mc = 0; mc < player_mai_ma_count[p]; mc++) {
				if (show_cards) {
					cards.addItem(player_mai_ma_data[p][mc]);
				} else {
					cards.addItem(-1);
				}
			}
			roomResponse.addOutCardTingCards(cards);
		}

		if (to_player == GameConstants_SX.INVALID_SEAT) {
			GRR.add_room_response(roomResponse);
			return this.send_response_to_room(roomResponse);
		} else {
			return this.send_response_to_player(to_player, roomResponse);
		}
	}

	/**
	 * 刷新花牌和亮牌
	 */
	public boolean operate_show_card_other(int seat_index, int type) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		this.load_common_status(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_OTHER_CARD);
		roomResponse.setTarget(seat_index);

		OtherResponse.Builder otherBuilder = OtherResponse.newBuilder();
		// 亮牌
		if (type == 1 || type == 3) {
			Int32ArrayResponse.Builder liang_card = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < GRR.get_liang_card_count_show(seat_index); i++) {
				liang_card.addItem(GRR.get_player_liang_card_show(seat_index, i));
			}
			otherBuilder.setLiangZhang(liang_card);
		}

		// 花牌
		if (type == 2 || type == 3) {
			Int32ArrayResponse.Builder hua_cards = Int32ArrayResponse.newBuilder();
			for (int i = 0; i < GRR._hua_pai_card[seat_index].length; i++) {
				if (GRR._hua_pai_card[seat_index][i] == 0) {
					continue;
				}
				hua_cards.addItem(GRR._hua_pai_card[seat_index][i]);
			}
			otherBuilder.setHuaCard(hua_cards);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(otherBuilder));

		GRR.add_room_response(roomResponse);
		return this.send_response_to_room(roomResponse);
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
	public boolean operate_player_cards(int seat_index, int card_count, int cards[], int weave_count, WeaveItem weaveitems[], int send_card) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_REFRESH_PLAYER_CARDS);// 12更新玩家手牌数据
		roomResponse.setGameStatus(_game_status);
		roomResponse.setTarget(seat_index);
		roomResponse.setCardType(1);

		this.load_common_status(roomResponse);

		// 手牌数量
		roomResponse.setCardCount(card_count);
		roomResponse.setWeaveCount(weave_count);

		this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		if (weave_count > 0) {
			this.builderWeaveItemResponse(weave_count, weaveitems, roomResponse);
		}

		this.send_response_to_other(seat_index, roomResponse);

		int index_card[] = new int[] { GameConstants.INVALID_CARD, GameConstants.INVALID_CARD, GameConstants.INVALID_CARD,
				GameConstants.INVALID_CARD };
		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			// 特殊牌值处理
			int real_card = cards[i];
			for (int k = 0; k < GRR.get_liang_card_count(seat_index); k++) {
				if (index_card[k] == GameConstants.INVALID_CARD && real_card == GRR.get_player_liang_card(seat_index, k)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LIANG_ZHANG;
					index_card[k] = GRR.get_player_liang_card(seat_index, k);
				}
			}
			roomResponse.addCardData(real_card);
		}

		GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		this.send_response_to_player(seat_index, roomResponse);

		return true;
	}

	public boolean estimate_player_out_card_respond(int seat_index, int card, int type) {
		boolean bAroseAction = false;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (seat_index == i) {
				continue;
			}
			if (_playerStatus[i].is_bao_ting())
				continue;

			playerStatus = _playerStatus[i];

			boolean can_peng = true;
			int[] tmp_cards_data = _playerStatus[i].get_cards_abandoned_peng();
			for (int x = 0; x < GameConstants_SX.MAX_ABANDONED_CARDS_COUNT; x++) {
				if (tmp_cards_data[x] == card) {
					can_peng = false;
					break;
				}
			}
			if (can_peng && GRR._left_card_count > 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[i], cards);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}

				if (GRR._left_card_count > 0) {
					action = _logic.estimate_gang_card_out_card(GRR._cards_index[i], card);
					if (action != 0) {
						playerStatus.add_action(GameConstants_SX.WIK_GANG);
						playerStatus.add_gang(card, seat_index, 1); // 加上杠
						bAroseAction = true;
					}
				}
			}

			if (_playerStatus[i].is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_SX.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] != 0) {
						can_hu_this_card = false;
						break;
					}
				}
				if (can_hu_this_card) {
					ChiHuRight chr = GRR._chi_hu_rights[i];
					chr.set_empty();
					int cbWeaveCount = GRR._weave_count[i];

					int card_type = GameConstants_SX.HU_CARD_TYPE_JIE_PAO;
					action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr, card_type, i);

					if (action != 0) {
						_playerStatus[i].add_action(GameConstants_SX.WIK_CHI_HU);
						_playerStatus[i].add_chi_hu(card, seat_index);
						bAroseAction = true;
					}
				}
			}
		}

		if (bAroseAction) {
			_resume_player = _current_player;// 保存当前轮到的玩家
			_current_player = GameConstants.INVALID_SEAT;// 有需要操作的玩家。当前玩家为空
			_provide_player = seat_index;
		} else {
			return false;
		}

		return true;
	}

	// 杠牌分析 包括补杠
	public int analyse_gang_hong_zhong_all(int cards_index[], WeaveItem WeaveItem[], int cbWeaveCount, GangCardResult gangCardResult,
			boolean check_weave, int cards_abandoned_gang[], int seat_index, int send_card_data) {
		// 设置变量
		int cbActionMask = GameConstants_SX.WIK_NULL;

		// 手上杠牌
		for (int i = 0; i < GameConstants_SX.MAX_INDEX; i++) {
			if (cards_index[i] == 4) {
				cbActionMask |= GameConstants_SX.WIK_GANG;

				int index = gangCardResult.cbCardCount++;
				gangCardResult.cbCardData[index] = _logic.switch_to_card_data(i);
				gangCardResult.isPublic[index] = 0;// 安刚
				gangCardResult.type[index] = GameConstants_SX.GANG_TYPE_AN_GANG;
			}
		}

		if (check_weave) {
			// 组合杠牌，包括以前能杠，但是不杠，发牌之后而选择补杠的
			for (int i = 0; i < cbWeaveCount; i++) {
				if (WeaveItem[i].weave_kind != GameConstants_SX.WIK_PENG) {
					continue;
				}
				for (int j = 0; j < GameConstants_SX.MAX_INDEX; j++) {
					if (cards_index[j] != 1 || cards_abandoned_gang[j] != 0) { // 红中不能杠，能接杠但是不杠的牌也过滤掉
						continue;
					}

					if (WeaveItem[i].center_card == _logic.switch_to_card_data(j)) {
						cbActionMask |= GameConstants_SX.WIK_GANG;

						int index = gangCardResult.cbCardCount++;
						gangCardResult.cbCardData[index] = WeaveItem[i].center_card;
						gangCardResult.isPublic[index] = 1;// 明刚
						gangCardResult.type[index] = GameConstants_SX.GANG_TYPE_ADD_GANG;
						break;
					}
				}
			}
		}

		return cbActionMask;
	}

	/**
	 * 封装落地牌组合
	 * 
	 * @param weave_count
	 * @param weaveitems
	 * @param roomResponse
	 */
	public void builderWeaveItemResponse(int weave_count, WeaveItem weaveitems[], RoomResponse.Builder roomResponse) {
		for (int j = 0; j < weave_count; j++) {
			WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
			weaveItem_item.setProvidePlayer(weaveitems[j].provide_player);
			weaveItem_item.setPublicCard(weaveitems[j].public_card);
			weaveItem_item.setWeaveKind(weaveitems[j].weave_kind);
			weaveItem_item.setCenterCard(weaveitems[j].center_card);

			// 客户端特殊处理的牌值
			for (int i = 0; i < weaveitems[j].client_special_count; i++) {
				weaveItem_item.addClientSpecialCard(weaveitems[j].client_special_card[i]);
			}

			roomResponse.addWeaveItems(weaveItem_item);
		}
	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card, ChiHuRight chiHuRight, int card_type,
			int _seat_index) {

		if (has_rule(GameConstants_SX.GAME_RULE_ZI_MO) && card_type == GameConstants_SX.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.set_empty();
			return GameConstants_SX.WIK_NULL;
		}

		if (card_type == GameConstants_SX.HU_CARD_TYPE_ZI_MO) {
			chiHuRight.opr_or(GameConstants_SX.CHR_ZI_MO);
		} else if (card_type == GameConstants_SX.HU_CARD_TYPE_JIE_PAO) {
			chiHuRight.opr_or(GameConstants_SX.CHR_SHU_FAN);
		} else if (card_type == GameConstants_SX.HU_CARD_TYPE_GANG_KAI_HUA) {
			chiHuRight.opr_or(GameConstants_SX.CHR_GNAG_KAI);
		} else if (card_type == GameConstants_SX.HU_CARD_TYPE_QIANG_GANG) {
			chiHuRight.opr_or(GameConstants_SX.CHR_QING_GANG_HU);
		}

		boolean hz_orther = false; // 别人打的红中
		int lai_zi_count = 0;
		for (int i = 0; i < _logic.get_magic_card_count(); i++) {
			lai_zi_count += cards_index[_logic.get_all_magic_card_index()[i]];
		}
		if (_logic.is_magic_card(cur_card)) {
			if (card_type == GameConstants_SX.HU_CARD_TYPE_ZI_MO || card_type == GameConstants_SX.HU_CARD_TYPE_GANG_KAI_HUA) {
				lai_zi_count++;
			} else {
				hz_orther = true;
			}
		}

		boolean bValue = AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
				_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), lai_zi_count);

		// 自摸才有金~
		if (bValue && has_rule(GameConstants_SX.GAME_RULE_ZI_MO)) {
			for (int w = 0; w < weave_count; w++) {
				if (weaveItems[w].center_card != GameConstants_SX.HZ_MAGIC_CARD) {
					continue;
				}
				if (weaveItems[w].weave_kind == GameConstants_SX.WIK_GANG) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_GANG);
				}
				if (weaveItems[w].weave_kind == GameConstants_SX.WIK_PENG) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_LONG);
				}
			}

			if (lai_zi_count == 1 && hz_orther) {
				chiHuRight.opr_or(GameConstants_SX.CHR_JIN_QUE);
			} else if (lai_zi_count == 2) {
				if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
						_logic.get_magic_card_count(), 0)) {
					if (hz_orther) {
						chiHuRight.opr_or(GameConstants_SX.CHR_JIN_LONG);
					} else {
						chiHuRight.opr_or(GameConstants_SX.CHR_JIN_QUE);
					}
				} else if (hz_orther && AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), 1)) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_QUE);
				}
			} else if (lai_zi_count == 3) {
				if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
						_logic.get_magic_card_count(), 0)) {
					if (hz_orther) {
						chiHuRight.opr_or(GameConstants_SX.CHR_JIN_GANG);
					} else {
						chiHuRight.opr_or(GameConstants_SX.CHR_JIN_LONG);
					}
				} else if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), 1)) {
					if (hz_orther) {
						chiHuRight.opr_or(GameConstants_SX.CHR_JIN_LONG);
					} else {
						chiHuRight.opr_or(GameConstants_SX.CHR_JIN_QUE);
					}
				} else if (hz_orther && AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), 2)) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_QUE);
				}
			} else if (lai_zi_count == 4) {
				if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
						_logic.get_magic_card_count(), 0)) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_GANG);
				} else if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), 1)) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_LONG);
				} else if (AnalyseCardUtil.analyse_win_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card),
						_logic.get_all_magic_card_index(), _logic.get_magic_card_count(), 2)) {
					chiHuRight.opr_or(GameConstants_SX.CHR_JIN_QUE);
				}
			}
		}

		if (_logic.is_qing_yi_se(cards_index, weaveItems, weave_count, cur_card)) {
			chiHuRight.opr_or(GameConstants_SX.CHR_QING_YI_SE);
		}

		if (card_type == GameConstants_SX.HU_CARD_TYPE_QIANG_GANG || card_type == GameConstants_SX.HU_CARD_TYPE_JIE_PAO) {
			int code = is_qi_xiao_dui_jie_pao(cards_index, weaveItems, weave_count, cur_card);
			if (code != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(code);
				return GameConstants_SX.WIK_CHI_HU;
			}
		} else if (card_type == GameConstants_SX.HU_CARD_TYPE_ZI_MO || card_type == GameConstants_SX.HU_CARD_TYPE_GANG_KAI_HUA) {
			int code = is_qi_xiao_dui_jie_zi_mo(cards_index, weaveItems, weave_count, cur_card, chiHuRight);
			if (code != GameConstants.WIK_NULL) {
				chiHuRight.opr_or(code);
				return GameConstants_SX.WIK_CHI_HU;
			}
		}

		if (!bValue) {
			chiHuRight.set_empty();
			return GameConstants_SX.WIK_NULL;
		}

		if (AnalyseCardUtil.analyse_peng_hu_by_cards_index(cards_index, _logic.switch_to_card_index(cur_card), _logic.get_all_magic_card_index(),
				_logic.get_magic_card_count())) {
			chiHuRight.opr_or(GameConstants_SX.CHR_DA_PENG);
		}
		if (_logic.get_card_count_by_index(cards_index) == 1) {
			chiHuRight.opr_or(GameConstants_SX.CHR_DAN_DIAO);
		}

		return GameConstants_SX.WIK_CHI_HU;
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
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

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

		// 显示胡牌
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = _logic.switch_to_cards_data(GRR._cards_index[seat_index], cards);
		for (int h = 0; h < hand_card_count; h++) {
			if (_logic.is_magic_card(cards[h])) {
				cards[h] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
		if (_logic.is_magic_card(operate_card)) {
			operate_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		cards[hand_card_count] = operate_card + GameConstants.CARD_ESPECIAL_TYPE_HU;
		hand_card_count++;
		this.operate_show_card(seat_index, GameConstants.Show_Card_HU, hand_card_count, cards, GameConstants.INVALID_SEAT);

		return;
	}

	public int is_qi_xiao_dui_jie_pao(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card) {
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;
		int laiZi_count = 0;
		int three_num = 0;

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

			if (_logic.get_magic_card_count() > 0) {
				boolean flag = false;
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m)) {
						// 王牌数量统计
						laiZi_count += cbCardCount;
						flag = true;
						continue;
					}
				}
				if (cur_card == GameConstants_SX.HZ_MAGIC_CARD) {
					cbCardIndexTemp[i] = 1;
					flag = false;
					laiZi_count--;
				}
				if (flag) {
					continue;
				}
			}
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 3) {
				three_num++;
			}

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			if (cbReplaceCount > laiZi_count) {
				return GameConstants.WIK_NULL;
			}
			// 四张牌数量
			nGenCount += three_num;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			return GameConstants_SX.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_SX.CHR_XIAO_QI_DUI;
		}
	}

	public int is_qi_xiao_dui_jie_zi_mo(int cards_index[], WeaveItem weaveItem[], int cbWeaveCount, int cur_card, ChiHuRight chr) {
		// 组合判断
		if (cbWeaveCount != 0)
			return GameConstants.WIK_NULL;

		// 单牌数目
		int cbReplaceCount = 0;
		int nGenCount = 0;
		int laiZi_count = 0;
		int three_num = 0;

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

			if (_logic.get_magic_card_count() > 0) {
				boolean flag = false;
				for (int m = 0; m < _logic.get_magic_card_count(); m++) {
					// 王牌过滤
					if (i == _logic.get_magic_card_index(m)) {
						// 王牌数量统计
						laiZi_count += cbCardCount;
						flag = true;
						continue;
					}
				}
				if (flag) {
					continue;
				}
			}
			// 单牌统计
			if (cbCardCount == 1 || cbCardCount == 3)
				cbReplaceCount++;

			if (cbCardCount == 3) {
				three_num++;
			}

			if (cbCardCount == 4) {
				nGenCount++;
			}
		}

		// 王牌不够
		if (_logic.get_magic_card_count() > 0) {
			if (cbReplaceCount > laiZi_count) {
				return GameConstants.WIK_NULL;
			}
			int residue_hz = laiZi_count - cbReplaceCount;
			if (residue_hz == 2) {
				chr.opr_or(GameConstants_SX.CHR_JIN_QUE);
			} else if (residue_hz == 4) {
				chr.opr_or(GameConstants_SX.CHR_JIN_GANG);
			}
			// 四张牌数量
			nGenCount += three_num;
		} else {
			if (cbReplaceCount > 0)
				return GameConstants.WIK_NULL;
		}

		if (nGenCount > 0) {
			return GameConstants_SX.CHR_LONG_QI_DUI;
		} else {
			return GameConstants_SX.CHR_XIAO_QI_DUI;
		}
	}

	@Override
	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {
		GRR._chi_hu_card[seat_index][0] = operate_card;

		GRR._win_order[seat_index] = 1;
		// 引用权位
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];

		int wFanShu = get_hu_type_score(seat_index);
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

		float lChiHuScore = get_hu_type_score(seat_index);
		////////////////////////////////////////////////////// 自摸 算分
		if (zimo) {
			if (has_rule(GameConstants_SX.GAME_RULE_FAN_BEI_ZI_MO)) {
				lChiHuScore *= 2;
			}
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				float s = lChiHuScore + GRR._count_pick_niao; // 自摸加1分

				GRR._game_score[i] -= s;
				GRR._game_score[seat_index] += s;
			}
		}
		////////////////////////////////////////////////////// 点炮 算分
		else {
			GRR._chi_hu_rights[provide_index].opr_or(GameConstants.CHR_FANG_PAO);

			float s = lChiHuScore + GRR._count_pick_niao;

			// 胡牌分
			GRR._game_score[provide_index] -= s;
			GRR._game_score[seat_index] += s;
		}

		int[] ma_score = get_ma_score(seat_index, provide_index, zimo);
		for (int p = 0; p < getTablePlayerNumber(); p++) {
			GRR._game_score[p] += ma_score[p];
			player_ma_get_score[p] += ma_score[p];
		}

		// 设置变量
		GRR._provider[seat_index] = provide_index;
		_status_gang = false;
		_status_gang_hou_pao = false;

		change_player_status(seat_index, GameConstants.INVALID_VALUE);
	}

	public int get_hu_type_score(int seat_index) {
		ChiHuRight chr = GRR._chi_hu_rights[seat_index];
		int fan = 1;
		int max_chr = 0;
		if (!chr.opr_and(GameConstants_SX.CHR_DA_PENG).is_empty()) {
			fan = 2;
			max_chr = GameConstants_SX.CHR_DA_PENG;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_DAN_DIAO).is_empty()) {
			fan = 2;
			max_chr = GameConstants_SX.CHR_DAN_DIAO;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_QING_GANG_HU).is_empty()) {
			fan = 3;
			max_chr = GameConstants_SX.CHR_QING_GANG_HU;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_QING_YI_SE).is_empty()) {
			fan = 3;
			max_chr = GameConstants_SX.CHR_QING_YI_SE;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_XIAO_QI_DUI).is_empty()) {
			fan = 3;
			max_chr = GameConstants_SX.CHR_XIAO_QI_DUI;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_LONG_QI_DUI).is_empty()) {
			fan = 4;
			max_chr = GameConstants_SX.CHR_LONG_QI_DUI;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_DI_HU).is_empty()) {
			fan = 5;
			max_chr = GameConstants_SX.CHR_DI_HU;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_TIAN_HU).is_empty()) {
			fan = 5;
			max_chr = GameConstants_SX.CHR_TIAN_HU;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_JIN_QUE).is_empty()) {
			fan = 6;
			max_chr = GameConstants_SX.CHR_JIN_QUE;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_JIN_LONG).is_empty()) {
			fan = 10;
			max_chr = GameConstants_SX.CHR_JIN_LONG;
		}
		if (!chr.opr_and(GameConstants_SX.CHR_JIN_GANG).is_empty()) {
			fan = 20;
			max_chr = GameConstants_SX.CHR_JIN_GANG;
		}

		int hu_type = 0;
		if (!chr.opr_and(GameConstants_SX.CHR_ZI_MO).is_empty()) {
			hu_type = GameConstants_SX.CHR_ZI_MO;
		} else if (!chr.opr_and(GameConstants_SX.CHR_SHU_FAN).is_empty()) {
			hu_type = GameConstants_SX.CHR_SHU_FAN;
		} else if (!chr.opr_and(GameConstants_SX.CHR_GNAG_KAI).is_empty()) {
			hu_type = GameConstants_SX.CHR_GNAG_KAI;
		} else if (!chr.opr_and(GameConstants_SX.CHR_QING_GANG_HU).is_empty()) {
			hu_type = GameConstants_SX.CHR_QING_GANG_HU;
		}

		chr.set_empty();
		chr.opr_or(max_chr);
		chr.opr_or(hu_type);
		chr.set_valid(true);

		return fan;
	}

	private int[] get_ma_score(int seat_index, int provide_index, boolean zimo) {

		int[] ma_score = new int[getTablePlayerNumber()];

		int hu_type_score = get_hu_type_score(seat_index);
		if (zimo) {
			if (has_rule(GameConstants_SX.GAME_RULE_FAN_BEI_ZI_MO)) {
				hu_type_score *= 2;
			}
			// 每个人
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				// 每个人的每个码
				for (int j = 0; j < player_mai_ma_count[p]; j++) {
					if (player_mai_ma_data[p][j] == GameConstants_SX.HZ_MAGIC_CARD) {
						continue;
					}
					int card_value = _logic.get_card_value(player_mai_ma_data[p][j]);
					int seat = (GRR._banker_player + card_value - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
					if (seat == seat_index) {
						for (int pp = 0; pp < getTablePlayerNumber(); pp++) {
							if (pp == seat_index) {
								continue;
							}
							ma_score[p] += hu_type_score;
							ma_score[pp] -= hu_type_score;
						}
					} else {
						ma_score[p] -= hu_type_score;
						ma_score[seat_index] += hu_type_score;
					}
				}
			}
		} else {
			// 每个人
			for (int p = 0; p < getTablePlayerNumber(); p++) {
				// 每个人的每个码
				for (int j = 0; j < player_mai_ma_count[p]; j++) {
					if (player_mai_ma_data[p][j] == GameConstants_SX.HZ_MAGIC_CARD) {
						continue;
					}
					int card_value = _logic.get_card_value(player_mai_ma_data[p][j]);
					int seat = (GRR._banker_player + card_value - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
					if (seat == seat_index) {
						ma_score[p] += hu_type_score;
						ma_score[provide_index] -= hu_type_score;
					} else if (seat == provide_index) {
						ma_score[seat_index] += hu_type_score;
						ma_score[p] -= hu_type_score;
					}
				}
			}
		}

		return ma_score;
	}

	@Override
	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();
		// 查牌数据
		this.setGameEndBasicPrama(game_end);

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		RoomInfo.Builder room_info = getRoomInfo();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setGamePlayerNumber(this.getTablePlayerNumber());
		game_end.setEndTime(System.currentTimeMillis() / 1000L);
		if (GRR != null) {
			// operate_mai_ma_card(GameConstants_SX.INVALID_SEAT, true);

			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);
			}

			GRR._end_type = reason;

			// 流局不需要计算杠分。
			float lGangScore[] = new float[this.getTablePlayerNumber()];
			if (reason == GameConstants_SX.Game_End_NORMAL) {
				int[] ma_score = new int[getTablePlayerNumber()];

				// 每个人
				for (int p = 0; p < getTablePlayerNumber(); p++) {
					// 每个人的每个码
					for (int j = 0; j < player_mai_ma_count[p]; j++) {
						if (player_mai_ma_data[p][j] == GameConstants_SX.HZ_MAGIC_CARD) {
							continue;
						}
						int card_value = _logic.get_card_value(player_mai_ma_data[p][j]);
						int seat = (GRR._banker_player + card_value - 1 + getTablePlayerNumber()) % getTablePlayerNumber();
						for (int pp = 0; pp < getTablePlayerNumber(); pp++) {
							// 每个人的每个码对应人的每个杠
							for (int k = 0; k < GRR._gang_score[pp].gang_count; k++) {
								// 每个人的每个码对应人的每个杠的每个人出的分，，，哇！！！！！心好痛
								if (seat == pp) {
									for (int kk = 0; kk < this.getTablePlayerNumber(); kk++) {
										if (kk == pp) {
											continue;
										} else {
											ma_score[p] -= GRR._gang_score[pp].scores[k][kk];
											ma_score[kk] += GRR._gang_score[pp].scores[k][kk];
										}
									}
								} else {
									ma_score[pp] -= GRR._gang_score[pp].scores[k][seat];
									ma_score[p] += GRR._gang_score[pp].scores[k][seat];
								}
							}
						}
					}
				}
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					GRR._game_score[i] += ma_score[i];
					player_ma_get_score[i] += ma_score[i];
				}

				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					for (int j = 0; j < GRR._gang_score[i].gang_count; j++) {
						for (int k = 0; k < this.getTablePlayerNumber(); k++) {
							lGangScore[k] += GRR._gang_score[i].scores[j][k];// 杠牌，每个人的分数
						}
					}

					for (int j = 0; j < this.getTablePlayerNumber(); j++) {
						_player_result.lost_fan_shu[i][j] += GRR._lost_fan_shu[i][j];
					}
				}
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._game_score[i] += lGangScore[i];
				GRR._game_score[i] += GRR._start_hu_score[i];

				_player_result.game_score[i] += GRR._game_score[i];
			}

			this.load_player_info_data(roomResponse);

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

			for (int i = 0; i < getTablePlayerNumber(); i++) {
				// 鸟牌，必须按四人计算
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < player_mai_ma_count[i]; j++) {
					pnc.addItem(player_mai_ma_data[i][j]);
				}
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
			}

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				game_end.addHuResult(GRR._hu_result[i]);
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_COUNT; j++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][j])) {
						hc.addItem(GRR._chi_hu_card[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						hc.addItem(GRR._chi_hu_card[i][j]);
					}
				}

				for (int h = 0; h < GRR._chi_hu_card[i].length; h++) {
					if (_logic.is_magic_card(GRR._chi_hu_card[i][h])) {
						game_end.addHuCardData(GRR._chi_hu_card[i][h] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
					} else {
						game_end.addHuCardData(GRR._chi_hu_card[i][h]);
					}
				}

				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			this.set_result_describe();

			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);

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
					// 客户端特殊处理的牌值
					/*
					 * for(int k = 0; k <
					 * GRR._weave_items[i][j].client_special_count; k++){
					 * weaveItem_item.addClientSpecialCard(GRR._weave_items[i][j
					 * ].client_special_card[k]); }
					 */
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
				for (int j = 0; j < this.getTablePlayerNumber(); j++) {
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
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT || reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT || reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end);

		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT || reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys())) {
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}

		return false;
	}

	@Override
	protected void set_result_describe() {
		int chrTypes;
		long type = 0;
		boolean qiang_gang_hu = false;
		for (int player = 0; player < this.getTablePlayerNumber(); player++) {
			StringBuilder result = new StringBuilder("");

			chrTypes = GRR._chi_hu_rights[player].type_count;

			for (int typeIndex = 0; typeIndex < chrTypes; typeIndex++) {
				type = GRR._chi_hu_rights[player].type_list[typeIndex];

				if (GRR._chi_hu_rights[player].is_valid()) {
					if (type == GameConstants_SX.CHR_ZI_MO) {
						result.append(" 自摸");
					}
					if (type == GameConstants_SX.CHR_QING_GANG_HU) {
						qiang_gang_hu = true;
						result.append(" 抢杠胡");
					}
					if (type == GameConstants_SX.CHR_SHU_FAN) {
						result.append(" 接炮");
					}
					if (type == GameConstants_SX.CHR_GNAG_KAI) {
						result.append(" 自摸");
					}
					if (type == GameConstants_SX.CHR_LONG_QI_DUI) {
						result.append(" 龙七对");
					}
					if (type == GameConstants_SX.CHR_XIAO_QI_DUI) {
						result.append(" 七小对");
					}
					if (type == GameConstants_SX.CHR_QING_YI_SE) {
						result.append(" 清一色");
					}
					if (type == GameConstants_SX.CHR_DA_PENG) {
						result.append(" 大碰胡");
					}
					if (type == GameConstants_SX.CHR_DAN_DIAO) {
						result.append(" 单吊");
					}
					if (type == GameConstants_SX.CHR_TIAN_HU) {
						result.append(" 天胡");
					}
					if (type == GameConstants_SX.CHR_DI_HU) {
						result.append(" 地胡");
					}
					if (type == GameConstants_SX.CHR_JIN_QUE) {
						result.append(" 金雀");
					}
					if (type == GameConstants_SX.CHR_JIN_LONG) {
						result.append(" 金龙");
					}
					if (type == GameConstants_SX.CHR_JIN_GANG) {
						result.append(" 金杠");
					}
				} else if (type == GameConstants_SX.CHR_FANG_PAO) {
					if (qiang_gang_hu) {
						result.append("被抢杠");
					} else {
						result.append(" 放炮");
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
				result.append(" 补杠X" + ming_gang);
			}
			if (fang_gang > 0) {
				result.append(" 放杠X" + fang_gang);
			}
			if (jie_gang > 0) {
				result.append(" 接杠X" + jie_gang);
			}
			if (get_ma_num() > 0) {
				GRR._result_des[player] += "码分:" + player_ma_get_score[player];
			}

			GRR._result_des[player] += result.toString();
		}
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
		GRR._count_pick_niao = get_pick_niao_count(GRR._cards_data_niao, GRR._count_niao);
		for (int i = 0; i < GRR._count_niao; i++) {
			if (GRR._cards_data_niao[i] == GameConstants_SX.HZ_MAGIC_CARD) {
				GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);//
				GRR._player_niao_cards[seat_index][GRR._player_niao_count[seat_index]++] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);//
				continue;
			}
			int seat = (seat_index + (GRR._cards_data_niao[i] - 1) + 4) % 4;
			if (seat == seat_index) {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this.set_ding_niao_valid(GRR._cards_data_niao[i], true);//
			} else {
				GRR._player_niao_cards[seat][GRR._player_niao_count[seat]++] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);//
			}

			int nValue = _logic.get_card_value(GRR._cards_data_niao[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], true);//
			} else {
				GRR._cards_data_niao[i] = this.set_ding_niao_valid(GRR._cards_data_niao[i], false);
			}
		}

	}

	/**
	 * 抓鸟 1 5 9
	 * 
	 * @param cards_data
	 * @param card_num
	 * @return
	 */
	public int get_pick_niao_count(int cards_data[], int card_num) {
		// MAX_NIAO_CARD
		int cbPickNum = 0;
		for (int i = 0; i < card_num; i++) {
			if (!_logic.is_valid_card(cards_data[i])) {
				return 0;
			}

			if (cards_data[i] == 0x35)
				continue;

			int nValue = _logic.get_card_value(cards_data[i]);
			if (nValue == 1 || nValue == 5 || nValue == 9) {
				cbPickNum++;
			}

		}
		return cbPickNum;
	}

	@Override
	public int set_ding_niao_valid(int card_data, boolean val) {
		// 先把值还原
		if (val) {
			if (card_data > GameConstants.DING_NIAO_INVALID && card_data < GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_INVALID;
			} else if (card_data > GameConstants.DING_NIAO_VALID) {
				card_data -= GameConstants.DING_NIAO_VALID;
			}
		} else {
			if (card_data > GameConstants.DING_NIAO_INVALID) {
				return card_data;
			}
		}

		if (val == true) {
			// 生效
			return (card_data < GameConstants.DING_NIAO_VALID ? card_data + GameConstants.DING_NIAO_VALID : card_data);
		} else {
			return (card_data < GameConstants.DING_NIAO_INVALID ? card_data + GameConstants.DING_NIAO_INVALID : card_data);
		}
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		return _handler_piao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
	}

	/**
	 * 抓鸟 个数
	 * 
	 * @return
	 */
	public int getCsDingNiaoNum() {
		if (has_rule(GameConstants_SX.GAME_RULE_ZHUA_NIAO_2)) {
			return GameConstants.ZHANIAO_2;
		}
		if (has_rule(GameConstants_SX.GAME_RULE_ZHUA_NIAO_4)) {
			return GameConstants.ZHANIAO_4;
		}
		if (has_rule(GameConstants_SX.GAME_RULE_ZHUA_NIAO_6)) {
			return GameConstants.ZHANIAO_6;
		}

		return GameConstants.ZHANIAO_0;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public boolean estimate_gang_respond(int seat_index, int card) {

		if (has_rule(GameConstants_SX.GAME_RULE_ZI_MO)) {
			return false;
		}

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
			if (playerStatus.is_chi_hu_round()) {
				boolean can_hu_this_card = true;
				int[] tmp_cards_data_hu = _playerStatus[i].get_cards_abandoned_hu();
				for (int x = 0; x < GameConstants_SX.MAX_ABANDONED_CARDS_COUNT; x++) {
					if (tmp_cards_data_hu[x] != 0) {
						can_hu_this_card = false;
						break;
					}
				}
				if (!can_hu_this_card) {
					continue;
				}

				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants_SX.HU_CARD_TYPE_QIANG_GANG, i);

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

	@Override
	public void test_cards() {
		int[] cards_of_player0 = new int[] { 0x06, 0x06, 0x06, 0x07, 0x07, 0x07, 0x08, 0x35, 0x35, 0x35, 0x05, 0x08, 0x08 };
		int[] cards_of_player1 = new int[] { 0x06, 0x06, 0x06, 0x07, 0x07, 0x07, 0x08, 0x35, 0x35, 0x35, 0x05, 0x08, 0x08 };
		int[] cards_of_player2 = new int[] { 0x06, 0x06, 0x06, 0x07, 0x07, 0x07, 0x08, 0x35, 0x35, 0x35, 0x05, 0x08, 0x08 };
		int[] cards_of_player3 = new int[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x22, 0x35, 0x35, 0x35 };

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

}
