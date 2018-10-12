package com.cai.game.laopai.handler.xp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.RedisConstant;
import com.cai.common.define.ECardType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GameRoundRecord;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.common.util.SpringService;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DispatchLastCardRunnable;
import com.cai.game.RoomUtil;
import com.cai.game.laopai.AbstractLPTable;
import com.cai.game.laopai.LPGameLogic.AnalyseItem;
import com.cai.game.laopai.LPType;
import com.cai.redis.service.RedisService;
import com.cai.service.MongoDBServiceImpl;
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
import protobuf.clazz.laopai.LpRsp.LP_XU_JIPAIQI;

public class LPTable_XP extends AbstractLPTable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2698317717887322025L;

	public int _user_times[];
	public int _out_card_index[][];
	public int _can_not_pen_index[][];

	public int _end_score[];
	public int _pao_min;

	LPHandlerPao_XP _handler_pao;
	LPHandlerGU_XP _handler_gu;

	public LPTable_XP() {
		super(LPType.GAME_TYPE_LAOPAI_XUPU);
	}

	@Override
	protected void onInitTable() {
		_handler_dispath_card = new LPHandlerDispatchCard_XP();
		_handler_dispath_card_last = new LPHandlerDispatchCardLast_XP();
		_handler_out_card_operate = new LPHandlerOutCardOperate_XP();
		_handler_pao = new LPHandlerPao_XP();
		_handler_gu = new LPHandlerGU_XP();
		_handler_chi_peng = new LPHandlerChiPeng_XP();

		_user_times = new int[this.getTablePlayerNumber()];
		_end_score = new int[getTablePlayerNumber()];
		_out_card_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX_LAOPAI];
		_can_not_pen_index = new int[this.getTablePlayerNumber()][GameConstants.MAX_INDEX_LAOPAI];
		Arrays.fill(_user_times, 1);
		Arrays.fill(_end_score, 0);

		if (has_rule(GameConstants.GAME_RULE_XP_LP_DI_FEN_ONE)) {
			game_cell = 1;
		} else if (has_rule(GameConstants.GAME_RULE_XP_LP_DI_FEN_TWO)) {
			game_cell = 2;
		} else if (has_rule(GameConstants.GAME_RULE_XP_LP_DI_FEN_THREE)) {
			game_cell = 3;
		} else if (has_rule(GameConstants.GAME_RULE_XP_LP_DI_FEN_FOUR)) {
			game_cell = 4;
		}

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_out_card_index[i], 0);
			Arrays.fill(_can_not_pen_index[i], 0);
		}
	}

	public boolean reset_init_data() {

		if (_cur_round == 0) {
			record_game_room();
		}

		this._handler = null;

		GRR = new GameRoundRecord(this.getTablePlayerNumber(), GameConstants.MAX_WEAVE_LAOPAI,
				GameConstants.MAX_LAOPAI_COUNT, GameConstants.MAX_INDEX_LAOPAI);
		GRR._start_time = System.currentTimeMillis() / 1000L;
		GRR._game_type_index = _game_type_index;
		GRR._cur_round = _cur_round;

		_playerStatus = new PlayerStatus[this.getTablePlayerNumber()];
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			_playerStatus[i] = new PlayerStatus(GameConstants.MAX_LAOPAI_COUNT);
		}
		_cur_round++;

		// 设置变量
		_provide_card = GameConstants.INVALID_VALUE;
		_out_card_data = GameConstants.INVALID_VALUE;
		_send_card_data = GameConstants.INVALID_VALUE;

		_provide_player = GameConstants.INVALID_SEAT;
		_out_card_player = GameConstants.INVALID_SEAT;
		_current_player = GameConstants.INVALID_SEAT;

		_send_card_count = 0;
		_out_card_count = 0;

		istrustee = new boolean[4];
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
			room_player.setMoney(rplayer.getMoney());
			room_player.setGold(rplayer.getGold());
			if (rplayer.locationInfor != null) {
				room_player.setLocationInfor(rplayer.locationInfor);
			}
			GRR._video_recode.addPlayers(room_player);

		}

		return true;
	}

	@Override
	public boolean handler_player_ready(int seat_index, boolean is_cancel) {

		if (this.get_players()[seat_index] == null) {
			return false;
		}
		if (GameConstants.GS_MJ_FREE != _game_status && GameConstants.GS_MJ_WAIT != _game_status) {
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

		// 跑分
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			if (_player_result.pao[i] <= 0) {
				_player_result.pao[i] = 0;// 清掉 默认是-1
			}

		}
		// 闹庄
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_player_result.nao[i] = 0;
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
				this.refresh_room_redis_data(GameConstants.PROXY_ROOM_PLAYER, nt);
				return false;
			}
		}

		handler_game_start();

		this.refresh_room_redis_data(GameConstants.PROXY_ROOM_UPDATE, nt);

		return true;
	}

	@Override
	public void progress_banker_select() {
		if (_cur_banker == GameConstants.INVALID_SEAT) {
			_cur_banker = 0;// 创建者的玩家为专家
			_cur_banker = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % this.getTablePlayerNumber());
		}

		if (is_sys()) {// 金币场 随机庄家
			Random random = new Random();//
			int rand = random.nextInt(6) + 1 + random.nextInt(6) + 1;
			_cur_banker = rand % GameConstants.GAME_PLAYER;//
		}
	}

	// 游戏开始
	@Override
	protected boolean on_handler_game_start() {
		if (_cur_round == 2) {
			// real_kou_dou();// 记录真实扣豆
		}

		reset_init_data();
		Arrays.fill(_user_times, 1);
		Arrays.fill(_end_score, 0);
		Arrays.fill(_user_times, 1);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Arrays.fill(_out_card_index[i], 0);
			Arrays.fill(_can_not_pen_index[i], 0);
		}

		this.operate_jipaiqi();
		// 庄家选择
		this.progress_banker_select();
		// 信阳麻将
		if (this.DEBUG_CARDS_MODE) {
			_cur_banker = 3;
		}
		GRR._banker_player = _cur_banker;
		_current_player = GRR._banker_player;

		if (!this.has_rule(GameConstants.GAME_RULE_XP_LP_BU_DAI_HUA)) {
			_repertory_card = new int[GameConstants.CARD_COUNT_XUPU_DAIFENG];
			shuffle(_repertory_card, GameConstants.CARD_DATA_LP_XP_DAI_FENG);
		} else {
			_repertory_card = new int[GameConstants.CARD_COUNT_XUPU];
			shuffle(_repertory_card, GameConstants.CARD_DATA_LP_XP);
		}

		if (DEBUG_CARDS_MODE || BACK_DEBUG_CARDS_MODE)
			test_cards();

		getLocationTip();

		try {
			for (int i = 0; i < getTablePlayerNumber(); i++) {
				for (int j = 0; j < GRR._cards_index[i].length; j++) {
					if (GRR._cards_index[i][j] == 4) {
						MongoDBServiceImpl.getInstance().card_log(this.get_players()[i], ECardType.anLong, "",
								GRR._cards_index[i][j], 0l, this.getRoom_id());
					}
				}
			}
		} catch (Exception e) {

		}
		// 游戏开始时 初始化 未托管
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			istrustee[i] = false;
		}
		if (this.has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST)
				|| this.has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST_SELECT)) {
			if (has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST_SELECT_ONE)) {
				_pao_min = 1;
			} else if (has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST_SELECT_TWO)) {
				_pao_min = 2;
			} else if (has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST_SELECT_THREE)) {
				_pao_min = 3;
			} else {
				_pao_min = 4;
			}

		}

		if (has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST)
				|| this.has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST_SELECT)) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (_player_result.pao[i] < _pao_min) {
					_player_result.pao[i] = _pao_min;
				}
			}
			this.operate_player_data();
		}
		if (this.has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN)
				|| this.has_rule(GameConstants.GAME_RULE_XP_LP_CHONGFEN_MUST_SELECT)) {
			this.set_handler(_handler_pao);
			_handler_pao.exe(this);
		} else {
			on_game_start();
		}

		return true;
	}

	/// 洗牌
	@Override
	public void shuffle(int repertory_card[], int mj_cards[]) {
		_all_card_len = repertory_card.length;
		GRR._left_card_count = _all_card_len;

		int xi_pai_count = 0;
		int rand = (int) RandomUtil.generateRandomNumber(3, 6);

		while (xi_pai_count < 6 && xi_pai_count < rand) {
			if (xi_pai_count == 0)
				_logic.random_card_data(repertory_card, mj_cards);
			else
				_logic.random_card_data(repertory_card, repertory_card);

			xi_pai_count++;
		}

		int send_count;
		int have_send_count = 0;

		int count = this.getTablePlayerNumber();
		// 分发扑克
		for (int i = 0; i < count; i++) {
			// if(GRR._banker_player == i){
			// send_count = MJGameConstants.MAX_COUNT;
			// }else{
			//
			// send_count = (MJGameConstants.MAX_COUNT - 1);
			// }
			send_count = (GameConstants.MAX_LAOPAI_COUNT - 1);
			GRR._left_card_count -= send_count;

			// 一人13张牌,庄家多一张
			_logic.switch_to_cards_index(repertory_card, have_send_count, send_count, GRR._cards_index[i]);

			have_send_count += send_count;

		}
		// 记录初始牌型
		if (_recordRoomRecord != null) {
			_recordRoomRecord.setBeginArray(Arrays.toString(repertory_card));
		}
	}

	protected void test_cards() {

		int cards[] = new int[] { 9, 9, 49, 17, 23, 34, 36, 35, 22, 8, 34, 21, 38, 2, 21, 40, 1, 38, 36, 2, 22, 19, 18,
				6, 33, 51, 5, 40, 6, 49, 2, 9, 50, 39, 49, 17, 17, 19, 7, 8, 50, 4, 37, 3, 3, 18, 20, 35, 36, 24, 50,
				20, 2, 49, 8, 21, 1, 33, 41, 3, 39, 19, 41, 22, 4, 41, 35, 17, 1, 41, 20, 51, 24, 50, 51, 40, 1, 51, 8,
				7, 39, 20, 4, 9, 23, 24, 38, 5, 5, 38, 25, 37, 33, 40, 35, 3, 39, 7, 19, 6, 24, 21, 4, 22, 6, 25, 34,
				18, 18, 25, 36, 23, 23, 5, 25, 7, 34, 37, 33, 37 };
		int index = 0;
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < GameConstants.MAX_INDEX_LAOPAI; j++) {
				GRR._cards_index[i][j] = 0;
			}
		}
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			for (int j = 0; j < 16; j++) {
				GRR._cards_index[i][_logic.switch_to_card_index(cards[index++])] += 1;
			}
		}

		this._repertory_card = cards;

		/*************** 如果要测试线上实际牌 把下面代码取消注释 放入实际牌型即可 ***********************/

		/********
		 * 下面这个给测试用的 如果开发用 把上面的注释去掉 realyCards
		 **************************/

		if (BACK_DEBUG_CARDS_MODE) {
			if (debug_my_cards != null) {
				if (debug_my_cards.length > 16) {
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

	public int get_hand_card_count_max() {
		return 16;
	}

	@Override
	protected boolean on_game_start() {
		if (this._game_status == GameConstants.GS_MJ_PLAY || this._game_status == GameConstants.GS_MJ_NAO) {
			return false;
		}

		// 游戏开始
		this._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		_logic.clean_magic_cards();

		this.GRR._banker_player = this._current_player = this._cur_banker;
		// 游戏开始
		GameStartResponse.Builder gameStartResponse = GameStartResponse.newBuilder();
		// gameStartResponse.setSiceIndex(rand);
		gameStartResponse.setBankerPlayer(this.GRR._banker_player);
		gameStartResponse.setCurrentPlayer(this._current_player);
		gameStartResponse.setLeftCardCount(this.GRR._left_card_count);

		int hand_cards[][] = new int[getTablePlayerNumber()][GameConstants.MAX_LAOPAI_COUNT];
		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			int hand_card_count = this._logic.switch_to_cards_data(this.GRR._cards_index[i], hand_cards[i]);
			gameStartResponse.addCardsCount(hand_card_count);
		}

		// 发送数据
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			// 只发自己的牌
			gameStartResponse.clearCardData();
			for (int j = 0; j < GameConstants.MAX_LAOPAI_COUNT; j++) {
				gameStartResponse.addCardData(hand_cards[i][j]);
			}

			// 回放数据
			this.GRR._video_recode.addHandCards(cards);

			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			this.load_room_info_data(roomResponse);
			this.load_common_status(roomResponse);

			if (this._cur_round == 1) {
				// shuffle_players();
				this.load_player_info_data(roomResponse);
			}
			roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
			roomResponse.setGameStart(gameStartResponse);
			roomResponse.setCurrentPlayer(
					this._current_player == GameConstants.INVALID_SEAT ? this._resume_player : this._current_player);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			roomResponse.setGameStatus(this._game_status);
			roomResponse.setLeftCardCount(this.GRR._left_card_count);
			this.send_response_to_player(i, roomResponse);
		}
		////////////////////////////////////////////////// 回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_START);
		this.load_room_info_data(roomResponse);
		this.load_common_status(roomResponse);
		this.load_player_info_data(roomResponse);
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < GameConstants.MAX_LAOPAI_COUNT; j++) {
				cards.addItem(hand_cards[i][j]);
			}
			gameStartResponse.addCardsData(cards);
		}
		for (int i = 0; i < 2; i++) {
			gameStartResponse.addOtherCards(_repertory_card[_gang_mo_posion]);
			_gang_mo_cards[i] = _repertory_card[_gang_mo_posion++];
		}

		roomResponse.setGameStart(gameStartResponse);
		roomResponse.setLeftCardCount(this.GRR._left_card_count);
		this.GRR.add_room_response(roomResponse);

		// 检测听牌
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._hu_card_count = get_ting_card(_playerStatus[i]._hu_cards, GRR._cards_index[i],
					GRR._weave_items[i], GRR._weave_count[i]);
			if (_playerStatus[i]._hu_card_count > 0) {
				operate_chi_hu_cards(i, _playerStatus[i]._hu_card_count, _playerStatus[i]._hu_cards);
			}
		}

		exe_dispatch_card(_current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		return true;
	}

	/**
	 * 溆浦
	 * 
	 * @param cards_index
	 * @param weaveItems
	 * @param weaveCount
	 * @param cur_card
	 * @param chiHuRight
	 * @param card_type
	 * @return
	 */
	public int analyse_chi_hu_card_xp(int cards_index[], WeaveItem weaveItems[], int weaveCount, int cur_card,
			ChiHuRight chiHuRight, int card_type) {

		if (has_rule(GameConstants.GAME_RULE_XP_LP_ZI_MO_HU) && (card_type == GameConstants.HU_CARD_TYPE_PAOHU))// 是否选择了自摸胡
																												// !bSelfSendCard)
		{
			return GameConstants.WIK_NULL;
		}

		// cbCurrentCard一定不为0 !!!!!!!!!
		if (cur_card == 0) {
			return GameConstants.WIK_NULL;
		}

		// 变量定义
		int cbChiHuKind = GameConstants.WIK_NULL;

		if (chiHuRight.is_empty() == false) {

			return cbChiHuKind;
		}

		// 构造扑克
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX_LAOPAI];
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		// 插入扑克
		if (cur_card != GameConstants.INVALID_VALUE) {
			cbCardIndexTemp[_logic.switch_to_card_index(cur_card)]++;
		}

		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();

		// 分析扑克
		boolean bValue = _logic.analyse_card(cbCardIndexTemp, weaveItems, weaveCount, analyseItemArray,
				has_rule(GameConstants.GAME_RULE_XP_LP_DAI_HUA));
		if (!bValue) {

			chiHuRight.set_empty();
			return GameConstants.WIK_NULL;
		}

		cbChiHuKind = GameConstants.WIK_CHI_HU;

		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO) {
			// cbChiHuKind = MJGameConstants.WIK_CHI_HU;

			chiHuRight.opr_or(GameConstants.CHR_ZI_MO);
		} else {
			chiHuRight.opr_or(GameConstants.CHR_SHU_FAN);

		}

		return cbChiHuKind;
	}

	/**
	 * 三门峡麻将获取听牌
	 * 
	 * @param cards
	 * @param cards_index
	 * @param weaveItem
	 * @param cbWeaveCount
	 * @return
	 */
	public int get_ting_card(int[] cards, int cards_index[], WeaveItem weaveItem[], int cbWeaveCount) {

		// 复制数据
		int cbCardIndexTemp[] = new int[GameConstants.MAX_INDEX_LAOPAI];
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++) {
			cbCardIndexTemp[i] = cards_index[i];
		}

		ChiHuRight chr = new ChiHuRight();

		int count = 0;
		int cbCurrentCard;

		int del = 0;

		boolean isDaiFeng = true;
		int mj_count = GameConstants.MAX_ZI_LAOPAI;
		if (isDaiFeng) {
			mj_count = GameConstants.MAX_FENG_LAOPAI;
		} else {
			mj_count = GameConstants.MAX_ZI_LAOPAI;
		}

		for (int i = 0; i < mj_count; i++) {
			// if (this._logic.is_magic_index(i))
			// continue;
			cbCurrentCard = _logic.switch_to_card_data(i);
			chr.set_empty();
			if (GameConstants.WIK_CHI_HU == analyse_chi_hu_card_xp(cbCardIndexTemp, weaveItem, cbWeaveCount,
					cbCurrentCard, chr, GameConstants.HU_CARD_TYPE_ZIMO)) {

				cards[count] = cbCurrentCard;
				count++;
			}
		}

		// 有胡的牌。癞子肯定能胡

		int number = isDaiFeng ? 30 : 27;
		if (count >= number) {
			count = 1;
			cards[0] = -1;
		}

		return count;
	}

	/***
	 * 检查杠牌,有没有胡的 检查所有人
	 * 
	 * @param seat_index
	 * @param card
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
			// 可以胡的情况 判断
			if (playerStatus.is_chi_hu_round()) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_xp(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_QIANGGANG);

				// 结果判断
				if (action != 0) {
					chr.opr_or(GameConstants.CHR_HUNAN_QIANG_GANG_HU);// 抢杠胡
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

	// 麻将
	public boolean estimate_player_out_card_respond(int seat_index, int card) {
		// 变量定义
		boolean bAroseAction = false;// 出现(是否)有

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {

			_playerStatus[i].clean_action();
			_playerStatus[i].clean_weave();
		}

		// int llcard = get_niao_card_num(true, 0);
		int llcard = 0;
		if (has_rule(GameConstants.GAME_RULE_HENAN_DAI_HUN)) {// 带混玩法 剩余14张 结算
			llcard = GameConstants.CARD_COUNT_LEFT_HUANGZHUANG;
		}

		PlayerStatus playerStatus = null;

		int action = GameConstants.WIK_NULL;
		// 动作判断
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			// 用户过滤
			if (seat_index == i)
				continue;
			if (this._player_result.nao[i] > 0) {
				continue;
			}
			playerStatus = _playerStatus[i];

			int chi_seat_index = (seat_index + 1) % getTablePlayerNumber();
			if (_playerStatus[chi_seat_index].lock_huan_zhang() == false
					&& this._player_result.nao[chi_seat_index] < 1) {
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
			}
			//// 碰牌判断
			if (this._can_not_pen_index[i][this._logic.switch_to_card_index(card)] <= 0) {
				action = _logic.check_peng(GRR._cards_index[i], card);
				if (action != 0) {
					playerStatus.add_action(action);
					playerStatus.add_peng(card, seat_index);
					bAroseAction = true;
				}
			}

			// 可以胡的情况 判断
			if (_playerStatus[i].is_chi_hu_round() && !has_rule(GameConstants.GAME_RULE_XP_LP_ZI_MO_HU)) {
				// 吃胡判断
				ChiHuRight chr = GRR._chi_hu_rights[i];// playerStatus._chiHuRight;
				chr.set_empty();
				int cbWeaveCount = GRR._weave_count[i];
				action = analyse_chi_hu_card_xp(GRR._cards_index[i], GRR._weave_items[i], cbWeaveCount, card, chr,
						GameConstants.HU_CARD_TYPE_PAOHU);

				// 结果判断
				if (action != 0) {
					_playerStatus[i].add_action(GameConstants.WIK_CHI_HU);
					_playerStatus[i].add_chi_hu(card, seat_index);// 吃胡的组合
					bAroseAction = true;
				}
			}
			// }
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

	public void set_handler_out_card_operate() {
		this.set_handler(_handler_dispath_card);
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
	public boolean operate_jipaiqi() {
		//// 发送玩家出牌 201 =玩家出牌(CMD_OutCard: operate_player,operate_card)
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XU_LP_JPQ);
		LP_XU_JIPAIQI.Builder jipaiqi = LP_XU_JIPAIQI.newBuilder();
		int card_data_index[] = new int[GameConstants.MAX_INDEX_LAOPAI];
		for (int i = 0; i < GameConstants.MAX_INDEX_LAOPAI; i++) {
			for (int j = 0; j < this.getTablePlayerNumber(); j++) {
				card_data_index[i] += _out_card_index[j][i];
			}
			jipaiqi.addOutCardIndex(card_data_index[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(jipaiqi));
		GRR.add_room_response(roomResponse);
		return this.send_response_to_room(roomResponse);
	}

	// 游戏结束
	@Override
	public boolean on_room_game_finish(int seat_index, int reason) {
		if (reason == GameConstants.Game_End_RELEASE_PLAY || reason == GameConstants.Game_End_RELEASE_NO_BEGIN
				|| reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM) {
			if (_game_status == GameConstants.GS_MJ_PAO) {
				for (int i = 0; i < this.getTablePlayerNumber(); i++) {
					if (seat_index == i) {
						continue;
					}
					this._player_result.pao[i] = 0;
				}
			}
		}
		_game_status = GameConstants.GS_MJ_WAIT;
		boolean ret = false;

		if (_cur_round == 1 && (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW)) {
			RoomUtil.realkou_dou(this);
		}
		if (is_sys()) {
			clear_score_in_gold_room();
		}

		ret = this.on_handler_game_finish(seat_index, reason);
		return ret;
	}

	protected boolean on_handler_game_finish(int seat_index, int reason) {
		int real_reason = reason;

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			_player_ready[i] = 0;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GAME_END);
		GameEndResponse.Builder game_end = GameEndResponse.newBuilder();

		roomResponse.setLeftCardCount(0);

		this.load_common_status(roomResponse);
		this.load_room_info_data(roomResponse);

		// 这里记录了两次，先这样
		RoomInfo.Builder room_info = encodeRoomBase();
		game_end.setRoomInfo(room_info);
		game_end.setRunPlayerId(_run_player_id);
		game_end.setRoundOverType(0);
		game_end.setRoomOverType(0);
		game_end.setEndTime(System.currentTimeMillis() / 1000L);// 结束时间
		game_end.setGamePlayerNumber(getTablePlayerNumber());
		if (GRR != null) {// reason == MJGameConstants.Game_End_NORMAL || reason
							// == MJGameConstants.Game_End_DRAW ||
			// (reason ==MJGameConstants.Game_End_RELEASE_PLAY && GRR!=null)
			game_end.setRoundOverType(1);
			game_end.setStartTime(GRR._start_time);

			game_end.setGameTypeIndex(GRR._game_type_index);
			roomResponse.setLeftCardCount(GRR._left_card_count);

			// 特别显示的牌
			for (int i = 0; i < GRR._especial_card_count; i++) {
				game_end.addEspecialShowCards(GRR._especial_show_cards[i]);

			}

			GRR._end_type = reason;

			this.load_player_info_data(roomResponse);

			game_end.setGameRound(_game_round);
			game_end.setCurRound(_cur_round);

			game_end.setCellScore(GameConstants.CELL_SCORE);

			game_end.setBankerPlayer(GRR._banker_player);// 专家
			game_end.setLeftCardCount(GRR._left_card_count);// 剩余牌
			game_end.setShowBirdEffect(GRR._show_bird_effect == false ? 0 : 1);

			// 设置中鸟数据
			for (int i = 0; i < GameConstants.MAX_NIAO_CARD && i < GRR._count_niao; i++) {
				game_end.addCardsDataNiao(GRR._cards_data_niao[i]);
			}
			game_end.setCountPickNiao(GRR._count_pick_niao);// 中鸟个数

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder pnc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GRR._player_niao_count[i]; j++) {
					pnc.addItem(GRR._player_niao_cards[i][j]);
				}
				game_end.addPlayerNiaoCards(pnc);
				game_end.addHuResult(GRR._hu_result[i]);

				// 胡的牌
				Int32ArrayResponse.Builder hc = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_LAOPAI_COUNT; j++) {
					hc.addItem(GRR._chi_hu_card[i][j]);
				}

				game_end.addHuCardData(GRR._chi_hu_card[i][0]);
				game_end.addHuCardArray(hc);
			}

			// 现在权值只有一位
			long rv[] = new long[GameConstants.MAX_RIGHT_COUNT];

			// 设置胡牌描述
			this.set_result_describe();

			for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
				Int32ArrayResponse.Builder cs = Int32ArrayResponse.newBuilder();
				if (this._player_result.nao[i] > 0) {
					for (int j = 0; j < GameConstants.MAX_INDEX_LAOPAI; j++) {
						GRR._card_count[i] += GRR._cards_index[i][j];
					}
					int card_data[] = new int[GRR._card_count[i]];
					GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], card_data);
					for (int j = 0; j < GRR._card_count[i]; j++) {

						cs.addItem(card_data[j]);
					}
				} else {
					GRR._card_count[i] = _logic.switch_to_cards_data(GRR._cards_index[i], GRR._cards_data[i]);
					for (int j = 0; j < GRR._card_count[i]; j++) {

						cs.addItem(GRR._cards_data[i][j]);
					}
				}

				game_end.addCardsData(cs);// 牌

				// 组合
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE_LAOPAI; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setCenterCard(GRR._weave_items[i][j].center_card);
					weaveItem_item.setProvidePlayer(GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(GRR._weave_items[i][j].weave_kind);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				game_end.addWeaveItemArray(weaveItem_array);

				GRR._chi_hu_rights[i].get_right_data(rv);// 获取权位数值
				game_end.addChiHuRight(rv[0]);

				GRR._start_hu_right[i].get_right_data(rv);// 获取权位数值
				game_end.addStartHuRight(rv[0]);

				game_end.addProvidePlayer(GRR._provider[i]);
				game_end.addGameScore(_end_score[i]);// 放炮的人？
				game_end.addGangScore(0);// 杠牌得分
				game_end.addStartHuScore(GRR._start_hu_score[i]);
				game_end.addResultDes(GRR._result_des[i]);

				// 胡牌
				game_end.addWinOrder(GRR._win_order[i]);

				Int32ArrayResponse.Builder lfs = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
					lfs.addItem(GRR._lost_fan_shu[i][j]);
				}

				game_end.addLostFanShu(lfs);

			}

		}

		boolean end = false;
		if (reason == GameConstants.Game_End_NORMAL || reason == GameConstants.Game_End_DRAW) {
			if (_cur_round >= _game_round && (!is_sys())) {// 局数到了
				end = true;
				real_reason = GameConstants.Game_End_ROUND_OVER;
				game_end.setRoomOverType(1);
				game_end.setPlayerResult(this.process_player_result(reason));
			} else {
				// 确定下局庄家
				// 以后谁胡牌，下局谁做庄。
				// 流局,庄家下一个
				// 通炮,放炮的玩家

			}
		} else if ((!is_sys()) && (reason == GameConstants.Game_End_RELEASE_PLAY
				|| reason == GameConstants.Game_End_RELEASE_NO_BEGIN || reason == GameConstants.Game_End_RELEASE_RESULT
				|| reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_SYSTEM)) {
			end = true;
			real_reason = GameConstants.Game_End_DRAW;// 刘局
			game_end.setRoomOverType(1);
			game_end.setPlayerResult(this.process_player_result(reason));
		}
		game_end.setEndType(real_reason);

		////////////////////////////////////////////////////////////////////// 得分总的
		roomResponse.setGameEnd(game_end);

		this.send_response_to_room(roomResponse);

		record_game_round(game_end, real_reason);

		// 结束后刷新玩家
		// RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
		// roomResponse2.setGameStatus(_game_status);
		// roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
		// this.load_player_info_data(roomResponse2);
		// this.send_response_to_room(roomResponse2);

		// 超时解散
		if (reason == GameConstants.Game_End_RELEASE_PLAY_TIME_OUT
				|| reason == GameConstants.Game_End_RELEASE_WAIT_TIME_OUT) {
			for (int j = 0; j < GameConstants.GAME_PLAYER; j++) {
				Player player = this.get_players()[j];
				if (player == null)
					continue;
				send_error_notify(j, 1, "游戏解散成功!");

			}
		}

		if (end && (!is_sys()))// 删除
		{
			PlayerServiceImpl.getInstance().delRoomId(this.getRoom_id());
		}

		if (!is_sys()) {
			GRR = null;
		}

		if (is_sys()) {
			clear_score_in_gold_room();
		}
		for (int i = 0; i < getTablePlayerNumber(); i++) {
			_playerStatus[i]._is_nao_zhuang = false;
		}

		// 跑分重置
		for (int i = 0; i < this.getTablePlayerNumber(); i++) {
			if (i == seat_index) {
				continue;
			}
			_player_result.pao[i] = 0;
		}
		// 错误断言
		return false;
	}

	public void process_chi_hu_player_score(int seat_index, int provide_index, int operate_card, boolean zimo) {

	}

	/**
	 * 麻将算分
	 * 
	 * @param seat_index
	 * @param provide_index
	 * @param operate_card
	 * @param zimo
	 */
	public void process_chi_hu_player_score_xp(int seat_index, int provide_index, int operate_card, boolean zimo) {
		if (zimo) {
			for (int i = 0; i < this.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				if (has_rule(GameConstants.GAME_RULE_XP_LP_ZHUANG_XIAN)) {
					if (i == this.GRR._banker_player || seat_index == this.GRR._banker_player) {
						GRR._game_score[i] -= (_player_result.pao[i] + _player_result.pao[seat_index] + 1) * game_cell
								+ game_cell;
						GRR._game_score[seat_index] += (_player_result.pao[i] + _player_result.pao[seat_index] + 1)
								* game_cell + game_cell;

						_end_score[i] -= (_player_result.pao[i] + _player_result.pao[seat_index] + 1) * game_cell
								+ game_cell;
						_end_score[seat_index] += (_player_result.pao[i] + _player_result.pao[seat_index] + 1)
								* game_cell + game_cell;
					} else {
						GRR._game_score[i] -= (_player_result.pao[i] + _player_result.pao[seat_index]) * this.game_cell
								+ this.game_cell;
						GRR._game_score[seat_index] += (_player_result.pao[i] + _player_result.pao[seat_index])
								* this.game_cell + this.game_cell;

						_end_score[i] -= (_player_result.pao[i] + _player_result.pao[seat_index]) * this.game_cell
								+ this.game_cell;
						_end_score[seat_index] += (_player_result.pao[i] + _player_result.pao[seat_index]) * game_cell
								+ game_cell;
					}
				} else {
					GRR._game_score[i] -= (_player_result.pao[i] + _player_result.pao[seat_index]) * this.game_cell
							+ this.game_cell;
					GRR._game_score[seat_index] += (_player_result.pao[i] + _player_result.pao[seat_index])
							* this.game_cell + this.game_cell;

					_end_score[i] -= (_player_result.pao[i] + _player_result.pao[seat_index]) * this.game_cell
							+ this.game_cell;
					_end_score[seat_index] += (_player_result.pao[i] + _player_result.pao[seat_index]) * this.game_cell
							+ this.game_cell;
				}

			}
		} else {
			if (has_rule(GameConstants.GAME_RULE_XP_LP_ZHUANG_XIAN)) {
				if (provide_index == this.GRR._banker_player || seat_index == this.GRR._banker_player) {
					GRR._game_score[provide_index] -= (_player_result.pao[provide_index]
							+ _player_result.pao[seat_index] + 1) * game_cell + game_cell;
					GRR._game_score[seat_index] += (_player_result.pao[provide_index] + _player_result.pao[seat_index]
							+ 1) * game_cell + game_cell;

					_end_score[provide_index] -= (_player_result.pao[provide_index] + _player_result.pao[seat_index]
							+ 1) * game_cell + game_cell;
					_end_score[seat_index] += (_player_result.pao[provide_index] + _player_result.pao[seat_index] + 1)
							* game_cell + game_cell;
				} else {
					GRR._game_score[provide_index] -= (_player_result.pao[provide_index]
							+ _player_result.pao[seat_index]) * this.game_cell + this.game_cell;
					GRR._game_score[seat_index] += (_player_result.pao[provide_index] + _player_result.pao[seat_index])
							* this.game_cell + this.game_cell;

					_end_score[provide_index] -= (_player_result.pao[provide_index] + _player_result.pao[seat_index])
							* this.game_cell + this.game_cell;
					_end_score[seat_index] += (_player_result.pao[provide_index] + _player_result.pao[seat_index])
							* game_cell + game_cell;
				}
			} else {
				GRR._game_score[provide_index] -= (_player_result.pao[provide_index] + _player_result.pao[seat_index])
						* this.game_cell + this.game_cell;
				GRR._game_score[seat_index] += (_player_result.pao[provide_index] + _player_result.pao[seat_index])
						* this.game_cell + this.game_cell;

				_end_score[provide_index] -= (_player_result.pao[provide_index] + _player_result.pao[seat_index])
						* this.game_cell + this.game_cell;
				_end_score[seat_index] += (_player_result.pao[provide_index] + _player_result.pao[seat_index])
						* game_cell + game_cell;
			}
		}

		_cur_banker = seat_index;

		for (int i = 0; i < getTablePlayerNumber(); i++) {
			this._player_result.game_score[i] += _end_score[i];
		}

		this.operate_player_data();
	}

	@Override
	protected void set_result_describe() {
	}

	@Override
	public boolean handler_requst_pao_qiang(Player player, int pao, int qiang) {
		// TODO Auto-generated method stub
		if (_handler_pao != null) {
			_handler_pao.handler_pao_qiang(this, player.get_seat_index(), pao, qiang);
		}
		return false;
	}

	@Override
	public boolean handler_requst_nao_zhuang(Player player, int nao) {
		// TODO Auto-generated method stub
		if (_handler_gu != null) {
			return _handler_gu.handler_nao(this, player.get_seat_index(), nao);
		}
		return false;
	}

	@Override
	public boolean handler_requst_message_deal(Player player, int seat_index, RoomRequest room_rq, int type) {
		return true;
	}

	public boolean exe_dispatch_last_card(int seat_index, int type, int delay_time) {

		GameSchedule.put(new DispatchLastCardRunnable(this.getRoom_id(), seat_index, type, false), delay_time,
				TimeUnit.MILLISECONDS);// MJGameConstants.GANG_LAST_CARD_DELAY

		return true;
	}

	/**
	 * 显示中间出的牌
	 * 
	 * @param seat_index
	 */
	public void runnable_remove_hun_middle_cards(int seat_index) {

	}

	/**
	 * 取出实际牌数据
	 * 
	 * @param card
	 * @return
	 */
	public int get_real_card(int card) {

		return card;
	}

	/**
	 * 调度 发最后4张牌
	 * 
	 * @param cur_player
	 * @param type
	 * @param tail
	 * @return
	 */
	@Override
	public boolean runnable_dispatch_last_card_data(int cur_player, int type, boolean tail) {
		// 牌局未开 或者等待状态 调度不需要执行 add by zain 2017/6/1
		if ((_game_status == GameConstants.GS_MJ_FREE || _game_status == GameConstants.GS_MJ_WAIT) && is_sys())
			return false;

		return true;
	}

	public void rand_tuozi(int seat_index) {
		int num1 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;
		int num2 = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % 6) + 1;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_EFFECT_ACTION);
		roomResponse.setEffectType(GameConstants.Effect_Action_SHAI_ZI);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(num1);
		roomResponse.addEffectsIndex(num2);
		roomResponse.setEffectTime(1500);// anim time//摇骰子动画的时间
		roomResponse.setStandTime(500); // delay time//停留时间
		this.send_response_to_room(roomResponse);
		GRR.add_room_response(roomResponse);

	}

	@Override
	public int analyse_chi_hu_card(int[] cards_index, WeaveItem[] weaveItems, int weave_count, int cur_card,
			ChiHuRight chiHuRight, int card_type, int _seat_index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean trustee_timer(int operate_id, int seat_index) {
		// TODO Auto-generated method stub
		return false;
	}
}
